-- Create the local demo database (run once on a new PC).
-- As postgres superuser:
--   sudo -u postgres psql -f scripts/create-db.sql
-- Or paste into pgAdmin Query Tool (skip lines that already succeeded).

CREATE USER codepulse WITH PASSWORD 'codepulse';
CREATE DATABASE codepulse OWNER codepulse;
GRANT ALL PRIVILEGES ON DATABASE codepulse TO codepulse;

\c codepulse
GRANT ALL ON SCHEMA public TO codepulse;
ALTER SCHEMA public OWNER TO codepulse;
