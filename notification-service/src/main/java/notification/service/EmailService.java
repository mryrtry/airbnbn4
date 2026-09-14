package notification.service;

import notification.dto.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(NotificationMessage message) {
        if (message.recipientEmail() == null || message.recipientEmail().isBlank()) {
            log.warn("No recipient email, skipping send. type={}", message.type());
            return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(message.recipientEmail());
        mail.setSubject(message.subject() != null ? message.subject() : "Notification");
        mail.setText(message.body() != null ? message.body() : "");

        mailSender.send(mail);
        log.info("Email sent to {}: subject={}", message.recipientEmail(), mail.getSubject());
    }
}