#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""lab/db/run_sql.py — SQL 파일을 UTF-8로 직접 실행(pymysql).

mariadb.exe 클라이언트에 PowerShell 파이프로 넣으면 콘솔 코드페이지(CP949)를 거치며
한글 COMMENT·데이터가 깨진다(실측) — 파일 바이트를 그대로 UTF-8로 읽어 실행한다.

Usage: python run_sql.py <file.sql> [<file2.sql> ...] [--port 3307]
"""
import sys
import pymysql

args = []
port = 3307
argv = sys.argv[1:]
i = 0
while i < len(argv):
    if argv[i] == '--port' and i + 1 < len(argv):
        port = int(argv[i + 1])
        i += 2
        continue
    if not argv[i].startswith('--'):
        args.append(argv[i])
    i += 1

conn = pymysql.connect(host='127.0.0.1', port=port, user='root', password='',
                       charset='utf8mb4', autocommit=True)
cur = conn.cursor()
for path in args:
    sql = open(path, encoding='utf-8-sig').read()
    # 소박한 스테이트먼트 분리 — 랩 SQL은 프로시저/트리거가 없어 ';' 분리로 충분
    # 앞머리 주석 줄은 떼고 본문이 남으면 실행한다 — 종전엔 `--` 로 **시작하는** 문을 통째로 버려서,
    # 주석을 위에 단 CREATE TABLE CART_ITEMS(SR-202)가 조용히 빠졌다(RUN11 DB 재구축 실측).
    def _body(chunk):
        lines = chunk.strip().splitlines()
        while lines and (not lines[0].strip() or lines[0].strip().startswith('--')):
            lines.pop(0)
        return chr(10).join(lines).strip()
    stmts = [b for b in (_body(c) for c in sql.split(';')) if b]
    for s in stmts:
        cur.execute(s)
    print(f'[run_sql] {path}: {len(stmts)}문 실행')
cur.execute('SELECT COUNT(*) FROM sl_shop.ORDERS')
print('[run_sql] sl_shop.ORDERS =', cur.fetchone()[0])
conn.close()
