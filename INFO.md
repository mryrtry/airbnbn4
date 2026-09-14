Airbnb-BPM: сводка проекта для агента
Общая архитектура
Два Spring Boot модуля в Maven-мультимодуле (ru.itmo.blps:airbnb-bpm), плюс инфраструктура через docker-compose.

main-service (порт 8080):

Spring Boot 3.2.5, Camunda 7.22.0 (BPMN + DMN + Webapp), JWT, JPA/Postgres, STOMP-отправка в ActiveMQ.

REST /api/** (свой), Camunda Webapp (/camunda/**), Camunda REST (/engine-rest/**).

Схемы Postgres: business (свои entity), camunda (таблицы движка), notification (лог уведомлений).

notification-service (порт 8081):

JMS-consumer из ActiveMQ, пишет notification.notification_log, отправляет email через JavaMailSender (MailHog на localhost:1025).

Инфра: Postgres 16, ActiveMQ 5.18 (OpenWire 61616 + STOMP 61613), MailHog, pgAdmin (http://localhost:5050, логин admin@airbnb.com / change_me).

Точки подключения:

Postgres: localhost:5432, БД airbnb, юзер airbnb, пароль airbnb_secret.

pgAdmin: Host=postgres (не localhost).

MailHog UI: http://localhost:8025.

Бизнес-домены
Листинги (listing)
Entity Listing в business.listing: id, title, address, description, price, status, ownerId, createdAt, updatedAt, deletedAt, processInstanceId.

ListingStatus: AVAILABLE, BOOKED, LIVING, DELETED.

BPMN: listing-create, listing-update, listing-delete.

API: ListingController (/api/listings), permissions LISTING_READ/CREATE/UPDATE/DELETE.

Бронирования (booking)
Entity Booking в business.booking: id, listingId, guestId, ownerId, status, bookingStart, bookingEnd, ownerComment, processInstanceId, createdAt, updatedAt.

BookingStatus: PENDING_OWNER, APPROVED, REJECTED, CANCELLED, CHECKED_IN, CHECKED_OUT.

BPMN: booking-lifecycle.

API: BookingController (/api/bookings), permissions BOOKING_READ/CREATE/DECIDE/CHECK_IN/CHECK_OUT.

Резолюции (жалобы)
Без своей entity/таблицы. Всё состояние — в переменных процесса resolution-lifecycle.

BPMN: resolution-lifecycle.

API: ResolutionController (POST /api/resolutions), permission RESOLUTION_OPEN.

Роли и безопасность
Группы Camunda
USER — гость, может бронировать.

OWNER — владелец, может создавать/менять/удалять листинги.

ADMIN — админ (в т.ч. рассматривает жалобы).

BANNED — забаненный (REVOKE на всё).

camunda-admin — системная, доступ к Cockpit/Tasklist/Admin.

Создаются в GroupInitializer (@Order(1)).

DMN permissions
permissions.dmn — decision table permissions (hitPolicy=COLLECT), вход role, выход permission. Маппит группу → список permission-строк. Читается в PermissionService через Camunda DmnEngine.

PermissionService.hasPermission(...) вызывается через @PreAuthorize("@permissionService.hasPermission('X')") на REST-эндпоинтах.

JWT
JwtService выдаёт access (15 мин) и refresh (7 дней), секрет в application.yaml. JwtAuthenticationFilter:

парсит Bearer,

проверяет blacklist (TokenBlacklistService, in-memory),

ставит Spring Security аутентификацию + Camunda identityService.setAuthentication(username, groups),

в finally чистит identityService.clearAuthentication().

Важно: JWT — только для /api/**. Camunda Webapp (/camunda/**) и Camunda REST (/engine-rest/**) используют свою аутентификацию (Basic Auth / session cookie), независимо от JWT.

Camunda Authorization
CamundaAuthorizationInitializer (@Order(10)):

Удаляет legacy wildcard-авторизации.

Для USER/OWNER/ADMIN выдаёт GRANT на: TASK:TASK_ASSIGN, PROCESS_DEFINITION:<key>:READ+CREATE_INSTANCE, PROCESS_INSTANCE:*:CREATE+READ+UPDATE, APPLICATION:{cockpit,tasklist,admin,welcome}:ACCESS, GROUP:*:READ, GROUP_MEMBERSHIP:*:CREATE+DELETE.

Для BANNED — REVOKE на всё (PROCESS_DEFINITION:*, PROCESS_INSTANCE:*, TASK:*, APPLICATION:*). REVOKE перебивает GRANT в Camunda, поэтому даже если юзер остался в USER, он ничего не сможет.

Группы задач по людям
Все userTask используют candidateUsers, не candidateGroups:

Task_SelectListingDates — ${initiatorUserId}

Task_OwnerDecide — ${ownerId}

Task_CheckIn/Task_CheckOut — ${guestId}

Task_OwnerFileComplaint — ${ownerId}

Task_UserPay — ${guestId}

Task_AdminReview — candidateGroups="ADMIN" (единственное исключение).

BPMN booking-lifecycle — маршруты
Старт → Task_InitContext (${initUserContextDelegate}) → Gw_ListingProvided.

listingId задан → Task_CheckAvailable.

не задан → Task_SelectListingDates (candidate=${initiatorUserId}, форма select-listing) → Task_CheckAvailable.

Task_CheckAvailable (${checkBookingAvailableDelegate}) ставит checkError:

NOT_FOUND, NO_DATES, INVALID_DATES, DATE_IN_PAST, NOT_AVAILABLE, OWN_LISTING, HAS_ACTIVE.

На успехе ставит ownerId (и guestId для новых версий).

Gw_CheckAvailable роутит по checkError. Если ок → Task_CreateBooking (${createBookingDelegate}) → Task_PrepareNotifyOwner (${prepareBookingNotificationDelegate}, notificationType=BOOKING_CREATED, notificationRecipient=OWNER) → Task_SendNotifyOwner (${sendNotificationDelegate}) → Task_OwnerDecide (форма owner-decide).

Gw_Decision:

ownerDecision == 'APPROVE' → Task_ConfirmBooking (${confirmBookingDelegate}) → Task_PrepareNotifyApproved → Task_SendNotifyApproved → Task_CheckIn.

default (AUTO_REJECT или REJECT) → Task_RejectBooking (${rejectBookingDelegate}) → Task_PrepareNotifyRejected → Task_SendNotifyRejected → End_Rejected.

Task_CheckIn (candidate=${guestId}, форма check-in) → Task_RegisterCheckIn (${checkInDelegate}) → уведомления гостю и владельцу (по 2 пары prepare/send) → Task_CheckOut.

Task_CheckOut (candidate=${guestId}, форма check-out) → Task_RegisterCheckOut (${checkOutDelegate}) → Task_StartResolution (${startResolutionDelegate}) → уведомления гостю и владельцу о выезде → End_Completed.

Логика BookingService (ключевое)
BLOCKING_STATUSES = [APPROVED, CHECKED_IN] — только они блокируют новые брони.

PENDING_OWNER не блокирует — несколько pending на пересекающиеся даты допустимо.

hasBlockingBooking(listingId, start, end) — SQL-проверка пересечения: b.bookingStart < :end AND :start < b.bookingEnd (строгое неравенство → back-to-back разрешён).

approveBooking(bookingId, comment):

находит все пересекающиеся брони (PENDING_OWNER/APPROVED/CHECKED_IN),

для каждой завершает её Task_OwnerDecide через taskService.complete(taskId, {ownerDecision: "AUTO_REJECT", ownerComment}) — процесс сам идёт в reject-ветку,

ставит текущей APPROVED,

листингу BOOKED.

rejectBooking(bookingId, comment) — только текущей REJECTED.

checkIn(bookingId) — CHECKED_IN, листинг LIVING.

checkOut(bookingId) — CHECKED_OUT, листинг AVAILABLE.

BPMN resolution-lifecycle
Запускается startResolutionDelegate из booking-lifecycle на этапе check-out.

Переменные: bookingId, ownerId, guestId, bookingStart, bookingEnd, initiatorUserId=ownerId.
BusinessKey: resolution-booking-<bookingId> (используется для поиска окна по bookingId).

Маршрут:

text
Start → Task_CreateWindow (${createResolutionWindowDelegate})
      → Task_PrepareNotifyWindowOpened (RESOLUTION_WINDOW_OPENED / OWNER)
      → Task_SendNotifyWindowOpened
      → Task_OwnerFileComplaint (candidate=${ownerId}, форма file-complaint)
           ← boundary timer Timer_WindowExpire (PT1M, cancelActivity=true)
Ветка А (владелец подал жалобу) — Task_OwnerFileComplaint завершается (через Tasklist или REST):

text
→ Task_RegisterComplaint (${registerComplaintDelegate})
→ Task_PrepareNotifyComplaintFiled (RESOLUTION_FILED / GUEST)
→ Task_SendNotifyComplaintFiled
→ Task_AdminReview (candidateGroups=ADMIN, форма resolution-decide)
→ Gw_AdminDecision
   ├─ APPROVE → Task_ApproveComplaint (${approveComplaintDelegate})
   │          → уведомления RESOLUTION_APPROVED гостю и владельцу
   │          → Task_UserPay (candidate=${guestId}, форма resolution-pay)
   │          → Gw_UserDecision
   │              ├─ PAY → Task_RegisterPayment (${registerPaymentDelegate})
   │              │      → уведомления RESOLUTION_PAID обоим → End_Paid
   │              └─ DECLINE → Task_BanUser (${banUserDelegate})
   │                         → уведомления RESOLUTION_DECLINED обоим → End_Banned
   └─ REJECT/default → Task_RejectComplaint (${rejectComplaintDelegate})
                     → уведомления RESOLUTION_REJECTED обоим → End_ComplaintRejected
Ветка Б (таймер сработал):

text
→ Task_CloseWindowExpired (${closeResolutionWindowExpiredDelegate})
→ Task_PrepareNotifyExpired (RESOLUTION_WINDOW_EXPIRED / OWNER)
→ Task_SendNotifyExpired
→ End_WindowExpired
При срабатывании timer cancelActivity=true прерывает Task_OwnerFileComplaint — задача исчезает.

startResolutionDelegate
Перед стартом проверяет, что процесса resolution-lifecycle с businessKey resolution-booking-<bookingId> нет. Если есть — не стартует дубль. Иначе — runtimeService.startProcessInstanceByKey("resolution-lifecycle", businessKey, vars).

BanUserDelegate
Удаляет guestId из USER, OWNER, ADMIN.

Добавляет в BANNED.

Ставит переменные complaintStatus=DECLINED, windowStatus=CLOSED, userBanned=true.

REST POST /api/resolutions
Body: { bookingId, reason, ownerComment? }.

Находит ProcessInstance по businessKey resolution-booking-<bookingId> и processDefinitionKey=resolution-lifecycle.

Проверяет, что ownerId совпадает с Authentication.getName().

Находит Task_OwnerFileComplaint для этого процесса.

taskService.complete(taskId, {reason, ownerComment?}) — процесс идёт по ветке А.

Требует permission RESOLUTION_OPEN (у OWNER в DMN есть Rule_O9).

Если окна нет / процесс не на той стадии / не тот owner → 400/403.

ResolutionScheduler
@Scheduled(fixedRate = 30_000) — только для логирования открытых окон. Основной механизм закрытия — boundary timer в BPMN.

Формы (*.form JSON, schemaVersion 16)
create-listing.form, edit-listing.form — текстовые поля + цена.

select-listing.form — объединённая: listingId (обязательный), bookingStart/bookingEnd как textfield в ISO (YYYY-MM-DD), обязательность снята — чтобы годилась и для update/delete. Тип date не поддерживается — Camunda Forms в 7.x его не рендерит.

confirm-delete.form.

owner-decide.form — ownerDecision (select APPROVE/REJECT), ownerComment.

check-in.form, check-out.form — checkbox.

file-complaint.form — reason (textarea, обязательный), ownerComment.

resolution-decide.form — adminDecision (select APPROVE/REJECT), adminComment.

resolution-pay.form — userDecision (select PAY/DECLINE).

Все формы регистрируются в META-INF/processes.xml через <resource>имя.form</resource>. BPMN-задачи ссылаются через camunda:formRef + camunda:formRefBinding="latest".

Уведомления
NotificationMessage(type, recipientEmail, recipientName, subject, body) — record, реализует Serializable.
NotificationProducer → StompNotificationSender (сырой STOMP-фрейм поверх сокета localhost:61613) → ActiveMQ → notification-service NotificationConsumer (@JmsListener(destination="notifications.queue")) → NotificationDispatcher → запись в notification.notification_log + EmailService.send() через MailHog.

Делегаты-подготовки
PrepareBookingNotificationDelegate — типы BOOKING_CREATED/REJECTED/APPROVED/CHECKED_IN/CHECKED_OUT, использует notificationRecipient (GUEST/OWNER) + фолбэк по activityId.

PrepareResolutionNotificationDelegate — то же для RESOLUTION_*.

Тип уведомления передаётся через <camunda:inputParameter name="notificationType">ТИП</...> в BPMN. SendNotificationDelegate читает notificationType, notificationRecipientEmail, notificationSubject, notificationBody и вызывает notificationProducer.send(...).

Типы RESOLUTION_*
RESOLUTION_WINDOW_OPENED/EXPIRED (→OWNER), RESOLUTION_FILED/APPROVED/PAID/DECLINED/REJECTED (→GUEST и/или OWNER).

Известный нюанс: в notification_log поле type может быть NULL для старых процессов (деплой до добавления inputParameter). Для новых — заполняется.

Ключевые подводные камни, с которыми столкнулись
The deployment contains definitions with the same key — Camunda запрещает два <bpmn:process> с одинаковым id. Причина была в случайно вставленном resolution-lifecycle внутрь booking-lifecycle.bpmn. Лечится удалением дубля.

MalformedJsonException на деплое .form — любая опечатка в JSON валит весь деплой (Camunda парсит все формы, не только те, что используются). Формы без лишних запятых/скобок.

date в формах не поддерживается → использовать textfield в ISO.

Unknown property used in expression: ${guestId} — переменная не выставлялась до Task_CheckIn. Фикс: execution.setVariable("guestId", ...) в CheckBookingAvailableDelegate и CreateBookingDelegate.

Unknown property used in expression: ${XxxDelegate} — Spring-бин с таким именем не найден. Проверять @Component("имя") + пакет + пересборку (mvn clean package).

Cannot invoke "CharSequence.length()" при логине — у пользователя pwd_ = NULL. Camunda REST POST /user/create не сохраняет пароль (баг/особенность). Использовать POST /api/auth/register (свой) или camunda.bpm.admin-user — те пишут через IdentityService.saveUser.

REST Camunda PUT — членство пользователя в группе делается через PUT /group/{groupId}/members/{userId}, а не /user/{userId}/groups/{groupId} (там 404).

Старые версии BPMN в running instances — при правке .bpmn новый деплой создаёт новую версию, но существующие инстансы идут по старой. Надо удалять инстанс и стартовать новый.

/engine-rest/** и /camunda/** — permitAll в SecurityConfig. Это значит REST Camunda доступен без JWT. Забаненный может стартовать процесс через /engine-rest/process-definition/key/.../start, минуя все проверки. Закрывается через Camunda Authorization REVOKE для BANNED + отключение /engine-rest/** в SecurityConfig (опционально).

Разница JWT и Camunda-сессии — JWT для /api/**, Basic Auth для /camunda/**. Нельзя «зайти в Tasklist» с JWT — только Basic Auth (admin/admin или airadmin).

Удаление running instance без чистки бизнес-данных → бронь остаётся PENDING_OWNER и блокирует новые брони на те же даты. При удалении процессов чистить business.booking (UPDATE status='CANCELLED').

Boundary timer не срабатывал сам — потому что делегат closeResolutionWindowExpiredDelegate не существовал, job падал в retry. После создания класса и пересборки — работает.