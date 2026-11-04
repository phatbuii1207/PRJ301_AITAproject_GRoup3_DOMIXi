"""Read configuration as data without executing .env or printing secrets."""
from pathlib import Path
import os
from urllib.parse import urlsplit, parse_qs, unquote

ROOT = Path(__file__).resolve().parent.parent

def read_env(path):
    values = {}
    path = Path(path)
    if not path.is_file():
        return values
    for raw in path.read_text(encoding='utf-8').splitlines():
        line = raw.strip()
        if line.startswith('export '):
            line = line[7:].strip()
        if not line or line.startswith('#') or '=' not in line:
            continue
        key, value = line.split('=', 1)
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
            value = value[1:-1]
        values[key.strip()] = value
    return values

def postgres_config():
    values = read_env(os.environ.get('AITA_ENV_FILE', ROOT / '.env'))
    def get(key, fallback=''):
        return os.environ.get(key) or values.get(key) or fallback
    url = get('DB_URL', 'jdbc:postgresql://' + get('DB_HOST', 'localhost') + ':' + get('DB_PORT', '5432') + '/' + get('DB_NAME', 'AST_CodeAnalysisDB'))
    if not url.startswith('jdbc:postgresql://'):
        raise SystemExit('DB_URL must use jdbc:postgresql://host:port/database')
    parsed = urlsplit(url[5:])
    if parsed.username or parsed.password:
        raise SystemExit('Use DB_USER / DB_PASSWORD separately from DB_URL')
    password = get('DB_PASSWORD')
    if not password:
        raise SystemExit('Set DB_PASSWORD in the environment or private .env')
    environment = os.environ.copy()
    environment['PGPASSWORD'] = password
    environment['PGCONNECT_TIMEOUT'] = '10'
    query = parse_qs(parsed.query)
    if 'sslmode' in query:
        environment['PGSSLMODE'] = query['sslmode'][0]
    command = ['psql', '-X', '--set', 'ON_ERROR_STOP=1', '--host', parsed.hostname or 'localhost',
               '--port', str(parsed.port or 5432), '--username', get('DB_USER', 'aita'),
               '--dbname', unquote(parsed.path.lstrip('/'))]
    return command, environment
