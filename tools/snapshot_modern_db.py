"""Read actual metadata via the configured, read-only MCP connection."""
import asyncio
import datetime
import json
import os
from pathlib import Path
import sys
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

ws = Path('D:/sl-shop')
plugin = Path('D:/speclinker-main')
out = Path('D:/sl-shop-runtime/setup')
sys.path.insert(0, str(plugin / 'scripts'))
import db_facts_io
import fk_snapshot


async def main():
    config = json.loads((ws / '.mcp.json').read_text(encoding='utf-8'))['mcpServers']['mariadb']
    params = StdioServerParameters(command=config['command'], args=config['args'], env={**os.environ, **config['env']})
    async def call(session, name, args):
        response = await session.call_tool(name, args)
        assert not response.isError, response
        return json.loads('\n'.join(x.text for x in response.content if x.type == 'text'))
    def lower(row):
        return {k.lower(): v for k, v in row.items()}
    with (out / 'mcp.log').open('w', encoding='utf-8') as log:
        async with stdio_client(params, errlog=log) as (read, write):
            async with ClientSession(read, write) as session:
                await session.initialize()
                tools = [t.name for t in (await session.list_tools()).tools]
                rows = await call(session, 'mdb_list_tables', {'schema': 'sl_shop_modern'})
                tables, fks, raw = {}, [], []
                for row in rows:
                    name = lower(row)['table_name']
                    data = await call(session, 'mdb_full_table_spec', {'schema': 'sl_shop_modern', 'table_name': name})
                    raw.append(data)
                    cols = [lower(x) for x in data['columns']]
                    indices = {}
                    for item in data['indexes']:
                        item = lower(item)
                        index = indices.setdefault(item['index_name'], {'name': item['index_name'], 'cols': [], 'unique': not item['non_unique']})
                        index['cols'].append(item['column_name'])
                    foreign = [lower(x) for x in data['foreign_keys']]
                    tables[name] = {'columns': [{'name': x['column_name'], 'type': x['column_type'],
                                                'nullable': x['is_nullable'] == 'YES', 'default': x['column_default'],
                                                'pk': x['is_pk'] == 'Y', 'comment': x['column_comment']} for x in cols],
                                    'indexes': list(indices.values()),
                                    'fks': [{'col': x['column_name'], 'ref_table': x['referenced_table_name'],
                                             'ref_col': x['referenced_column_name']} for x in foreign]}
                    fks += [{'table': name, 'column': x['column_name'], 'ref_table': x['referenced_table_name'],
                             'ref_column': x['referenced_column_name']} for x in foreign]
    data = {'source': 'Read-only MariaDB MCP 127.0.0.1:13328/sl_shop_modern',
            'generated': datetime.datetime.now().isoformat(timespec='seconds'), 'tables': tables}
    input_file = out / 'db-facts-current.json'
    input_file.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')
    # Archive and replace the captured catalogue. Canonical design MD stays historical.
    db_facts_io.merge(str(ws), data)
    fk_snapshot._save(str(ws), fks)
    (out / 'mcp-inventory.json').write_text(json.dumps({'tools': tools, 'tables': len(tables),
        'columns': sum(len(x['columns']) for x in tables.values()), 'foreign_keys': len(fks), 'raw': raw},
        ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps({'tables': len(tables), 'columns': sum(len(x['columns']) for x in tables.values()), 'foreign_keys': len(fks)}))


asyncio.run(main())
