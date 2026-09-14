CREATE SCHEMA IF NOT EXISTS business;
CREATE SCHEMA IF NOT EXISTS camunda;
CREATE SCHEMA IF NOT EXISTS notification;

GRANT ALL ON SCHEMA business TO airbnb;
GRANT ALL ON SCHEMA camunda TO airbnb;
GRANT ALL ON SCHEMA notification TO airbnb;

CREATE UNIQUE INDEX IF NOT EXISTS uq_active_booking_per_listing
ON business.booking (listing_id)
WHERE status IN ('PENDING_OWNER','APPROVED','CHECKED_IN');

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";