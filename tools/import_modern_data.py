"""Clone the preserved database into the independent local demonstration instance."""
import hashlib
import json
from pathlib import Path
import subprocess
import time
import bcrypt
import pymysql

runtime = Path('D:/sl-shop-runtime')
proof = runtime / 'setup'
schema = 'sl_shop_modern'
connection = dict(host='127.0.0.1', port=13328, user='root', charset='utf8mb4', autocommit=True)
for attempt in range(30):
    try:
        c = pymysql.connect(**connection)
        break
    except pymysql.Error:
        time.sleep(1)
else:
    raise SystemExit('Independent MariaDB not ready')
with c.cursor() as q:
    q.execute('SHOW DATABASES LIKE %s', (schema,))
    if q.fetchone():
        raise SystemExit('Refusing to overwrite existing database')
    q.execute('CREATE DATABASE `' + schema + '` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci')
bin_dir = Path('D:/Shopping_mall/.runtime/mariadb-11.4.5-winx64/bin')
dump = proof / 'source-sl-shop.sql'
with dump.open('wb') as output:
    subprocess.run([str(bin_dir / 'mariadb-dump.exe'), '-h127.0.0.1', '-P13318', '-uroot',
                    '--single-transaction', '--skip-lock-tables', '--hex-blob', '--default-character-set=utf8mb4',
                    'sl_shop'], stdout=output, check=True)
with dump.open('rb') as inp:
    subprocess.run([str(bin_dir / 'mariadb.exe'), '-h127.0.0.1', '-P13328', '-uroot',
                    '--default-character-set=utf8mb4', schema], stdin=inp, check=True)
c.select_db(schema)

def counts(conn):
    with conn.cursor() as q:
        q.execute('SHOW TABLES')
        tables = [row[0] for row in q.fetchall()]
        result = {}
        for table in tables:
            q.execute('SELECT COUNT(*) FROM `' + table + '`')
            result[table] = q.fetchone()[0]
        return result

origin = pymysql.connect(host='127.0.0.1', port=13318, user='root', database='sl_shop')
original_counts = counts(origin)
assert counts(c) == original_counts
origin.close()
password = 'Shop-demo-local-20261018!'
fixtures = []
with c.cursor() as q:
    for member, name, email in [('M-9001', '체험 구매자', 'buyer@speclinker.local'),
                                 ('M-9002', '소유권 확인용 회원', 'other@speclinker.local')]:
        q.execute('INSERT INTO MEMBERS(member_id,member_name,grade,created_at,del_yn,email,password_hash,marketing_opt_in) '
                  'VALUES (%s,%s,%s,NOW(),%s,%s,%s,0)',
                  (member, name, 'BRONZE', 'N', email, bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()))
        fixtures.append({'member': member, 'email': email, 'password': password})
    products = [('SKU-DEMO-SHIP', '배송비 체험 상품', 20000, 100, 'Y'),
                ('SKU-DEMO-FREE', '고가 체험 상품', 60000, 100, 'Y'),
                ('SKU-DEMO-SOLDOUT', '품절 상태 체험 상품', 15000, 0, 'Y'),
                ('SKU-DEMO-HIDDEN', '판매중지 검증 상품', 10000, 10, 'N')]
    for row in products:
        q.execute('INSERT INTO PRODUCTS(sku,product_name,price,stock_qty,sale_yn) VALUES (%s,%s,%s,%s,%s)', row)
    q.execute("CREATE USER IF NOT EXISTS 'speclens_modern_ro'@'localhost' IDENTIFIED BY ''")
    q.execute("GRANT SELECT, SHOW VIEW ON sl_shop_modern.* TO 'speclens_modern_ro'@'localhost'")
report = {'schema': schema, 'port': 13328, 'source_port': 13318, 'source_schema': 'sl_shop',
          'source_row_counts': original_counts, 'copy_counts_match_before_fixtures': True,
          'dump_sha256': hashlib.sha256(dump.read_bytes()).hexdigest(),
          'final_row_counts': counts(c), 'accounts': fixtures,
          'products': [p[0] for p in products], 'mcp_readonly': True}
(proof / 'database.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
c.close()
test_schema = 'sl_shop_modern_test'
test_connection = pymysql.connect(**connection)
with test_connection.cursor() as q:
    q.execute('SHOW DATABASES LIKE %s', (test_schema,))
    if q.fetchone():
        raise SystemExit('Refusing to overwrite existing test database')
    q.execute('CREATE DATABASE `' + test_schema + '` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci')
# Unit/integration tests assume the original product catalogue. Demo-only products
# belong exclusively to the interactive database, not this regression fixture.
with dump.open('rb') as inp:
    subprocess.run([str(bin_dir / 'mariadb.exe'), '-h127.0.0.1', '-P13328', '-uroot', test_schema], stdin=inp, check=True)
report['test_schema'] = test_schema
report['test_database_isolated_from_interactive_demo'] = True
(proof / 'database.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
test_connection.close()
print(json.dumps({'tables': len(original_counts), 'original_rows': sum(original_counts.values()),
                  'new_members': 2, 'new_products': 4, 'port': 13328}))
