#!/usr/bin/env python3
"""Apply the PostgreSQL schema and optional demo seed using psql; never resets a database."""
import argparse
import subprocess
from db_config import ROOT, postgres_config

parser = argparse.ArgumentParser()
parser.add_argument('--schema-only', action='store_true', help='Create tables without seed, e.g. before copying SQL Server data')
args = parser.parse_args()
command, environment = postgres_config()
try:
    files = [ROOT / 'database/postgresql/schema.sql']
    if not args.schema_only:
        files.append(ROOT / 'database/postgresql/seed.sql')
    for path in files:
        # psql may echo an offending statement on error; keep sensitive database messages out of console output.
        result = subprocess.run(command + ['--file', str(path)], env=environment, capture_output=True, text=True)
        if result.returncode:
            raise SystemExit('PostgreSQL setup failed in ' + path.name + '. Check database access/schema; no reset was performed.')
        print('Applied:', path.name)
except FileNotFoundError:
    raise SystemExit('psql not found. Install PostgreSQL client tools or run AITA_Database.sql in pgAdmin/DBeaver.')
print('PostgreSQL schema ready; existing data was preserved.')
