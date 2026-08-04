-- run as postgres superuser in pgAdmin Query Tool, or:
--   sudo -u postgres psql -f scripts/create-db.sql

CREATE USER codepulse WITH PASSWORD 'codepulse';
CREATE DATABASE codepulse OWNER codepulse;
GRANT ALL PRIVILEGES ON DATABASE codepulse TO codepulse;

\c codepulse
GRANT ALL ON SCHEMA public TO codepulse;
