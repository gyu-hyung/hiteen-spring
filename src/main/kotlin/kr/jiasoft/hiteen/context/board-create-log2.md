✅✅ ✅✅ ✅✅ ✅✅ ✅✅ ✅✅ ✅✅ ✅✅ ✅✅ ✅✅
2026-01-29T04:50:38.075Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [INSERT INTO assets (uid, origin_file_name, store_file_name, file_path, type, size, width, height, ext, download_count, created_id, created_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)]
2026-01-29T04:50:38.076Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 62bcb49f-d23d-4582-b127-edfffff33ba9
2026-01-29T04:50:38.076Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: image_picker_99D788E7-E284-45E8-88B4-CA3A64E63695-90209-00003509402E21B9.webp
2026-01-29T04:50:38.078Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: 6b55ae0185e0493cac0ae113109ee0e5.webp
2026-01-29T04:50:38.078Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [3] to: post/2026/01/29/
2026-01-29T04:50:38.079Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [4] to: image/webp
2026-01-29T04:50:38.080Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [5] to: 1387412
2026-01-29T04:50:38.080Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [6] to: 1920
2026-01-29T04:50:38.081Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [7] to: 2560
2026-01-29T04:50:38.081Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [8] to: webp
2026-01-29T04:50:38.081Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [9] to: 0
2026-01-29T04:50:38.081Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [10] to: 41
2026-01-29T04:50:38.081Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [11] to: 2026-01-29T04:50:38.072630394Z
2026-01-29T04:50:38.086Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [SELECT assets.id, assets.uid, assets.origin_file_name, assets.store_file_name, assets.file_path, assets.type, assets.size, assets.width, assets.height, assets.origin_id, assets.ext, assets.download_count, assets.created_id, assets.created_at, assets.updated_id, assets.updated_at, assets.deleted_id, assets.deleted_at FROM assets WHERE assets.uid = $1]
2026-01-29T04:50:38.086Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 62bcb49f-d23d-4582-b127-edfffff33ba9
2026-01-29T04:50:38.090Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [
SELECT * FROM assets
WHERE origin_id = $1
AND width = $2
AND height = $3
AND deleted_at IS NULL
LIMIT 1
]
2026-01-29T04:50:38.090Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 2512
2026-01-29T04:50:38.091Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: 800
2026-01-29T04:50:38.091Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: 800
2026-01-29T04:50:38.115Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [INSERT INTO assets (uid, origin_file_name, store_file_name, file_path, type, size, width, height, ext, download_count, created_id, created_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)]
2026-01-29T04:50:38.116Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 2af0a9c3-107c-42df-a571-b633a917023b
2026-01-29T04:50:38.116Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: image_picker_A5AF5574-400A-49E4-A657-649F3D8A158A-90209-000035094354E5E2.webp
2026-01-29T04:50:38.116Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: ccdfbc7b6f834dd9af0835517e0b9f9c.webp
2026-01-29T04:50:38.116Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [3] to: post/2026/01/29/
2026-01-29T04:50:38.116Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [4] to: image/webp
2026-01-29T04:50:38.116Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [5] to: 1387412
2026-01-29T04:50:38.116Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [6] to: 1920
2026-01-29T04:50:38.117Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [7] to: 2560
2026-01-29T04:50:38.117Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [8] to: webp
2026-01-29T04:50:38.117Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [9] to: 0
2026-01-29T04:50:38.118Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [10] to: 41
2026-01-29T04:50:38.118Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [11] to: 2026-01-29T04:50:38.113871412Z
2026-01-29T04:50:38.123Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [SELECT assets.id, assets.uid, assets.origin_file_name, assets.store_file_name, assets.file_path, assets.type, assets.size, assets.width, assets.height, assets.origin_id, assets.ext, assets.download_count, assets.created_id, assets.created_at, assets.updated_id, assets.updated_at, assets.deleted_id, assets.deleted_at FROM assets WHERE assets.uid = $1]
2026-01-29T04:50:38.123Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 2af0a9c3-107c-42df-a571-b633a917023b
2026-01-29T04:50:38.127Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [
SELECT * FROM assets
WHERE origin_id = $1
AND width = $2
AND height = $3
AND deleted_at IS NULL
LIMIT 1
]
2026-01-29T04:50:38.128Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 2513
2026-01-29T04:50:38.128Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: 800
2026-01-29T04:50:38.128Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: 800
2026-01-29T04:50:38.177Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [INSERT INTO assets (uid, origin_file_name, store_file_name, file_path, type, size, width, height, ext, download_count, created_id, created_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)]
2026-01-29T04:50:38.178Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 71d9c032-f2c6-4795-8c06-14ea5cf382b1
2026-01-29T04:50:38.178Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: image_picker_95E83B70-C650-4ED1-9582-85526F95E232-90209-0000350946776901.webp
2026-01-29T04:50:38.178Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: 6dc31cb1bfbd401392480be43420fb8a.webp
2026-01-29T04:50:38.178Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [3] to: post/2026/01/29/
2026-01-29T04:50:38.178Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [4] to: image/webp
2026-01-29T04:50:38.178Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [5] to: 1387412
2026-01-29T04:50:38.179Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [6] to: 1920
2026-01-29T04:50:38.179Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [7] to: 2560
2026-01-29T04:50:38.179Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [8] to: webp
2026-01-29T04:50:38.179Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [9] to: 0
2026-01-29T04:50:38.179Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [10] to: 41
2026-01-29T04:50:38.179Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [11] to: 2026-01-29T04:50:38.175207941Z
2026-01-29T04:50:38.185Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [SELECT assets.id, assets.uid, assets.origin_file_name, assets.store_file_name, assets.file_path, assets.type, assets.size, assets.width, assets.height, assets.origin_id, assets.ext, assets.download_count, assets.created_id, assets.created_at, assets.updated_id, assets.updated_at, assets.deleted_id, assets.deleted_at FROM assets WHERE assets.uid = $1]
2026-01-29T04:50:38.185Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 71d9c032-f2c6-4795-8c06-14ea5cf382b1
2026-01-29T04:50:38.313Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [
SELECT * FROM assets
WHERE origin_id = $1
AND width = $2
AND height = $3
AND deleted_at IS NULL
LIMIT 1
]
2026-01-29T04:50:38.313Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 2514
2026-01-29T04:50:38.314Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: 800
2026-01-29T04:50:38.317Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: 800
2026-01-29T04:50:38.389Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [INSERT INTO assets (uid, origin_file_name, store_file_name, file_path, type, size, width, height, ext, download_count, created_id, created_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)]
2026-01-29T04:50:38.390Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 29bba0d0-8dc0-4ae1-9003-03261f363d28
2026-01-29T04:50:38.390Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: image_picker_77FFEEF5-6E5E-43F3-AEE3-CBDCB1960D99-90209-000035094991E114.webp
2026-01-29T04:50:38.391Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: ccd02061625545989f136d5d13549f2e.webp
2026-01-29T04:50:38.391Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [3] to: post/2026/01/29/
2026-01-29T04:50:38.392Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [4] to: image/webp
2026-01-29T04:50:38.392Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [5] to: 1387412
2026-01-29T04:50:38.393Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [6] to: 1920
2026-01-29T04:50:38.393Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [7] to: 2560
2026-01-29T04:50:38.394Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [8] to: webp
2026-01-29T04:50:38.394Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [9] to: 0
2026-01-29T04:50:38.394Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [10] to: 41
2026-01-29T04:50:38.394Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [11] to: 2026-01-29T04:50:38.387801899Z
2026-01-29T04:50:38.402Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [SELECT assets.id, assets.uid, assets.origin_file_name, assets.store_file_name, assets.file_path, assets.type, assets.size, assets.width, assets.height, assets.origin_id, assets.ext, assets.download_count, assets.created_id, assets.created_at, assets.updated_id, assets.updated_at, assets.deleted_id, assets.deleted_at FROM assets WHERE assets.uid = $1]
2026-01-29T04:50:38.402Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 29bba0d0-8dc0-4ae1-9003-03261f363d28
2026-01-29T04:50:38.409Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [
SELECT * FROM assets
WHERE origin_id = $1
AND width = $2
AND height = $3
AND deleted_at IS NULL
LIMIT 1
]
2026-01-29T04:50:38.409Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 2515
2026-01-29T04:50:38.409Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: 800
2026-01-29T04:50:38.412Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: 800
2026-01-29T04:50:38.427Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [INSERT INTO assets (uid, origin_file_name, store_file_name, file_path, type, size, width, height, ext, download_count, created_id, created_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)]
2026-01-29T04:50:38.428Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 9f08d075-4db2-4897-bad4-99eb2deaca9b
2026-01-29T04:50:38.429Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: image_picker_F9DA64D7-C4B3-4A0E-A750-1EF016C4B559-90209-000035094DD8B094.webp
2026-01-29T04:50:38.429Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: 7ceb0bf1005a4df68fb19c9472c64780.webp
2026-01-29T04:50:38.429Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [3] to: post/2026/01/29/
2026-01-29T04:50:38.430Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [4] to: image/webp
2026-01-29T04:50:38.430Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [5] to: 1387412
2026-01-29T04:50:38.431Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [6] to: 1920
2026-01-29T04:50:38.431Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [7] to: 2560
2026-01-29T04:50:38.431Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [8] to: webp
2026-01-29T04:50:38.432Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [9] to: 0
2026-01-29T04:50:38.432Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [10] to: 41
2026-01-29T04:50:38.432Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [11] to: 2026-01-29T04:50:38.422403507Z
2026-01-29T04:50:38.437Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [SELECT assets.id, assets.uid, assets.origin_file_name, assets.store_file_name, assets.file_path, assets.type, assets.size, assets.width, assets.height, assets.origin_id, assets.ext, assets.download_count, assets.created_id, assets.created_at, assets.updated_id, assets.updated_at, assets.deleted_id, assets.deleted_at FROM assets WHERE assets.uid = $1]
2026-01-29T04:50:38.438Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 9f08d075-4db2-4897-bad4-99eb2deaca9b
2026-01-29T04:50:38.441Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [
SELECT * FROM assets
WHERE origin_id = $1
AND width = $2
AND height = $3
AND deleted_at IS NULL
LIMIT 1
]
2026-01-29T04:50:38.442Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 2516
2026-01-29T04:50:38.442Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: 800
2026-01-29T04:50:38.442Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: 800
2026-01-29T04:50:38.446Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [INSERT INTO boards (uid, category, content, ip, hits, asset_uid, report_count, status, created_id, created_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)]
2026-01-29T04:50:38.446Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 165ba595-2c4e-4559-a5ac-23bd42751b17
2026-01-29T04:50:38.573Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: POST
2026-01-29T04:50:38.573Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: .
2026-01-29T04:50:38.573Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [3] to: 10.8.1.218
2026-01-29T04:50:38.573Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [4] to: 0
2026-01-29T04:50:38.573Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [5] to: 62bcb49f-d23d-4582-b127-edfffff33ba9
2026-01-29T04:50:38.573Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [6] to: 0
2026-01-29T04:50:38.573Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [7] to: ACTIVE
2026-01-29T04:50:38.573Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [8] to: 41
2026-01-29T04:50:38.573Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [9] to: 2026-01-29T04:50:38.440822008Z
2026-01-29T04:50:39.089Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [INSERT INTO board_assets (board_id, uid) VALUES ($1, $2)]
2026-01-29T04:50:39.089Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 144
2026-01-29T04:50:39.094Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: 62bcb49f-d23d-4582-b127-edfffff33ba9
2026-01-29T04:50:39.099Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [INSERT INTO board_assets (board_id, uid) VALUES ($1, $2)]
2026-01-29T04:50:39.108Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 144
2026-01-29T04:50:39.110Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: 2af0a9c3-107c-42df-a571-b633a917023b
2026-01-29T04:50:39.126Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [INSERT INTO board_assets (board_id, uid) VALUES ($1, $2)]
2026-01-29T04:50:39.127Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 144
2026-01-29T04:50:39.128Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: 71d9c032-f2c6-4795-8c06-14ea5cf382b1
2026-01-29T04:50:39.172Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [INSERT INTO board_assets (board_id, uid) VALUES ($1, $2)]
2026-01-29T04:50:39.172Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 144
2026-01-29T04:50:39.172Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: 29bba0d0-8dc0-4ae1-9003-03261f363d28
2026-01-29T04:50:39.178Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [INSERT INTO board_assets (board_id, uid) VALUES ($1, $2)]
2026-01-29T04:50:39.178Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 144
2026-01-29T04:50:39.178Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: 9f08d075-4db2-4897-bad4-99eb2deaca9b
2026-01-29T04:50:39.192Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [
SELECT *
FROM exp_actions
WHERE action_code = $1
LIMIT 1
]
2026-01-29T04:50:39.206Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: CREATE_BOARD
2026-01-29T04:50:39.209Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [
SELECT COUNT(*)
FROM user_exp_history
WHERE user_id = $1
AND action_code = $2
AND DATE(created_at) = $3
]
2026-01-29T04:50:39.221Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 41
2026-01-29T04:50:39.221Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: CREATE_BOARD
2026-01-29T04:50:39.221Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: 2026-01-29
2026-01-29T04:50:39.225Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [
SELECT *
FROM point_rules
WHERE action_code = $1
AND deleted_at IS NULL
ORDER BY id
LIMIT 1
]
2026-01-29T04:50:39.273Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: STORY_POST
2026-01-29T04:50:39.371Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [
SELECT COUNT(*)
FROM points
WHERE user_id = $1
AND pointable_type = $2
AND DATE(created_at) = $3
]
2026-01-29T04:50:39.371Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 41
2026-01-29T04:50:39.371Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: STORY_POST
2026-01-29T04:50:39.371Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: 2026-01-29
오늘은 더 이상 'STORY_POST' 포인트를 받을 수 없습니다. (일일 제한: 3)
2026-01-29T04:50:39.374Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [
SELECT user_id
FROM follows
WHERE follow_id = $1
AND status = 'ACCEPTED'
]
2026-01-29T04:50:39.374Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-2] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 41
2026-01-29T04:50:39.378Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [INSERT INTO push (type, code, title, message, total, success, failure, created_id, created_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)]
2026-01-29T04:50:39.379Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: notification
2026-01-29T04:50:39.379Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: NEW_POST
2026-01-29T04:50:39.379Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: 새 글 등록 알림 🔔
2026-01-29T04:50:39.379Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [3] to: 방금 새로운 글이 올라왔어 🔔
2026-01-29T04:50:39.379Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [4] to: 1
2026-01-29T04:50:39.379Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [5] to: 0
2026-01-29T04:50:39.380Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [6] to: 0
2026-01-29T04:50:39.380Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [7] to: 41
2026-01-29T04:50:39.380Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [8] to: 2026-01-29T04:50:39.375540789Z
2026-01-29T04:50:39.385Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [
SELECT u.id as user_id, u.phone, d.device_os, d.device_token, d.push_items, d.push_service
FROM users u
JOIN user_details d ON u.id = d.user_id
WHERE u.id IN ($1)
]
2026-01-29T04:50:39.387Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 1
2026-01-29T04:50:40.483Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [INSERT INTO push_detail (push_id, user_id, device_os, device_token, phone, message_id, success, created_at, updated_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)]
2026-01-29T04:50:40.483Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 33568
2026-01-29T04:50:40.483Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: 1
2026-01-29T04:50:40.483Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: ios
2026-01-29T04:50:40.483Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [3] to: cLdkzm6t20gas1uvopkDDw:APA91bF8TUXQ1c5USWDCNoJjMnALgmK0dym7TuV7dErqCMKkcxj3CXkN4RSgXebrN8cdtTInRCsGaCUTfF0OwVYcCcHeaBdzxiEgQyNwgP8f7g0g2s0LEhM
2026-01-29T04:50:40.483Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [4] to: 01095393637
2026-01-29T04:50:40.483Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [5] to: projects/hi-teen-6fa22/messages/1769662240081671
2026-01-29T04:50:40.484Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [6] to: 1
2026-01-29T04:50:40.484Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [7] to: 2026-01-29T04:50:40.480633158Z
2026-01-29T04:50:40.484Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [8] to: 2026-01-29T04:50:40.480649360Z
🔥 Firebase sendEachForMulticast success=1, failure=0
2026-01-29T04:50:40.491Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [UPDATE push SET type = $1, code = $2, title = $3, message = $4, total = $5, success = $6, failure = $7, multicast_id = $8, canonical_ids = $9, created_id = $10, created_at = $11, updated_at = $12, deleted_at = $13 WHERE push.id = $14]
2026-01-29T04:50:40.492Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: notification
2026-01-29T04:50:40.492Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: NEW_POST
2026-01-29T04:50:40.492Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: 새 글 등록 알림 🔔
2026-01-29T04:50:40.492Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [3] to: 방금 새로운 글이 올라왔어 🔔
2026-01-29T04:50:40.492Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [4] to: 1
2026-01-29T04:50:40.492Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [5] to: 1
2026-01-29T04:50:40.494Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [6] to: 0
2026-01-29T04:50:40.494Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [7] to null, type: java.lang.String
2026-01-29T04:50:40.495Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [8] to null, type: java.lang.String
2026-01-29T04:50:40.496Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [9] to: 41
2026-01-29T04:50:40.497Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [10] to: 2026-01-29T04:50:39.375540789Z
2026-01-29T04:50:40.497Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [11] to: 2026-01-29T04:50:40.489674307Z
2026-01-29T04:50:40.498Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [12] to null, type: java.time.OffsetDateTime
2026-01-29T04:50:40.498Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [13] to: 33568
✅ [PushService] pushId=33568, success=1, failure=0
2026-01-29T04:50:45.489Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [INSERT INTO assets (uid, origin_file_name, store_file_name, file_path, type, size, width, height, origin_id, ext, download_count, created_id, created_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)]
2026-01-29T04:50:45.490Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: b326fac6-7c1a-42ec-bc23-ceb8960373ae
2026-01-29T04:50:45.490Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: image_picker_F9DA64D7-C4B3-4A0E-A750-1EF016C4B559-90209-000035094DD8B094_800x800.webp
2026-01-29T04:50:45.490Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: c933f01d-5dc2-426f-a7e4-af2864596ff6.webp
2026-01-29T04:50:45.490Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [3] to: thumb/cover/800x800/2026/01/29/
2026-01-29T04:50:45.490Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [4] to: image/webp
2026-01-29T04:50:45.490Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [5] to: 141254
2026-01-29T04:50:45.490Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [6] to: 800
2026-01-29T04:50:45.490Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [7] to: 800
2026-01-29T04:50:45.490Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [8] to: 2516
2026-01-29T04:50:45.490Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [9] to: webp
2026-01-29T04:50:45.490Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [10] to: 0
2026-01-29T04:50:45.490Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [11] to: 41
2026-01-29T04:50:45.490Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [12] to: 2026-01-29T04:50:45.488103742Z
2026-01-29T04:50:45.772Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [INSERT INTO assets (uid, origin_file_name, store_file_name, file_path, type, size, width, height, origin_id, ext, download_count, created_id, created_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)]
2026-01-29T04:50:45.772Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: c6509c2c-561c-40d7-8b0c-296dc8e0ca8a
2026-01-29T04:50:45.772Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: image_picker_95E83B70-C650-4ED1-9582-85526F95E232-90209-0000350946776901_800x800.webp
2026-01-29T04:50:45.772Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: f4fc2950-edcb-410b-a611-ceb5c305d81c.webp
2026-01-29T04:50:45.772Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [3] to: thumb/cover/800x800/2026/01/29/
2026-01-29T04:50:45.772Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [4] to: image/webp
2026-01-29T04:50:45.772Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [5] to: 141254
2026-01-29T04:50:45.772Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [6] to: 800
2026-01-29T04:50:45.772Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [7] to: 800
2026-01-29T04:50:45.772Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [8] to: 2514
2026-01-29T04:50:45.772Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [9] to: webp
2026-01-29T04:50:45.772Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [10] to: 0
2026-01-29T04:50:45.772Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [11] to: 41
2026-01-29T04:50:45.772Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [12] to: 2026-01-29T04:50:45.739743565Z
2026-01-29T04:50:45.799Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [INSERT INTO assets (uid, origin_file_name, store_file_name, file_path, type, size, width, height, origin_id, ext, download_count, created_id, created_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)]
2026-01-29T04:50:45.800Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 4e4d69a9-bc95-434a-8121-284ddb814060
2026-01-29T04:50:45.800Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: image_picker_A5AF5574-400A-49E4-A657-649F3D8A158A-90209-000035094354E5E2_800x800.webp
2026-01-29T04:50:45.800Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: 232f13c6-e184-4e83-90a3-b3b870590d55.webp
2026-01-29T04:50:45.800Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [3] to: thumb/cover/800x800/2026/01/29/
2026-01-29T04:50:45.800Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [4] to: image/webp
2026-01-29T04:50:45.800Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [5] to: 141254
2026-01-29T04:50:45.800Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [6] to: 800
2026-01-29T04:50:45.800Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [7] to: 800
2026-01-29T04:50:45.800Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [8] to: 2513
2026-01-29T04:50:45.800Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [9] to: webp
2026-01-29T04:50:45.800Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [10] to: 0
2026-01-29T04:50:45.800Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [11] to: 41
2026-01-29T04:50:45.800Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [12] to: 2026-01-29T04:50:45.798493166Z
2026-01-29T04:50:45.873Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [INSERT INTO assets (uid, origin_file_name, store_file_name, file_path, type, size, width, height, origin_id, ext, download_count, created_id, created_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)]
2026-01-29T04:50:45.873Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 9dd55cb4-a330-4067-8aaa-dc2ca833f890
2026-01-29T04:50:45.874Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: image_picker_99D788E7-E284-45E8-88B4-CA3A64E63695-90209-00003509402E21B9_800x800.webp
2026-01-29T04:50:45.874Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: 126b562f-2cb8-466d-b370-e41db70bbb62.webp
2026-01-29T04:50:45.874Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [3] to: thumb/cover/800x800/2026/01/29/
2026-01-29T04:50:45.874Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [4] to: image/webp
2026-01-29T04:50:45.874Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [5] to: 141254
2026-01-29T04:50:45.874Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [6] to: 800
2026-01-29T04:50:45.874Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [7] to: 800
2026-01-29T04:50:45.874Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [8] to: 2512
2026-01-29T04:50:45.874Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [9] to: webp
2026-01-29T04:50:45.874Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [10] to: 0
2026-01-29T04:50:45.874Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [11] to: 41
2026-01-29T04:50:45.875Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [12] to: 2026-01-29T04:50:45.832447987Z
2026-01-29T04:50:45.994Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [INSERT INTO assets (uid, origin_file_name, store_file_name, file_path, type, size, width, height, origin_id, ext, download_count, created_id, created_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)]
2026-01-29T04:50:45.994Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 1da81ed0-b524-4dbe-8136-788aab94d842
2026-01-29T04:50:45.994Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [1] to: image_picker_77FFEEF5-6E5E-43F3-AEE3-CBDCB1960D99-90209-000035094991E114_800x800.webp
2026-01-29T04:50:45.994Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [2] to: dce9025c-94cb-4b40-be39-f788c93feabe.webp
2026-01-29T04:50:45.994Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [3] to: thumb/cover/800x800/2026/01/29/
2026-01-29T04:50:45.994Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [4] to: image/webp
2026-01-29T04:50:45.994Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [5] to: 141254
2026-01-29T04:50:45.994Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [6] to: 800
2026-01-29T04:50:45.996Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [7] to: 800
2026-01-29T04:50:45.996Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [8] to: 2515
2026-01-29T04:50:45.997Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [9] to: webp
2026-01-29T04:50:45.997Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [10] to: 0
2026-01-29T04:50:45.997Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [11] to: 41
2026-01-29T04:50:45.997Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [12] to: 2026-01-29T04:50:45.992256173Z
2026-01-29T04:50:46.252Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [SELECT users.id, users.uid, users.username, users.email, users.nickname, users.password, users.role, users.address, users.detail_address, users.phone, users.mood, users.mood_emoji, users.mbti, users.exp_points, users.tier_id, users.asset_uid, users.school_id, users.school_updated_at, users.class_id, users.grade, users.gender, users.birthday, users.profile_decoration_code, users.invite_code, users.invite_joins, users.location_mode, users.year, users.created_id, users.created_at, users.updated_id, users.updated_at, users.deleted_id, users.deleted_at FROM users WHERE users.username = $1]
2026-01-29T04:50:46.253Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 01021635328
2026-01-29T04:50:47.629Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [SELECT users.id, users.uid, users.username, users.email, users.nickname, users.password, users.role, users.address, users.detail_address, users.phone, users.mood, users.mood_emoji, users.mbti, users.exp_points, users.tier_id, users.asset_uid, users.school_id, users.school_updated_at, users.class_id, users.grade, users.gender, users.birthday, users.profile_decoration_code, users.invite_code, users.invite_joins, users.location_mode, users.year, users.created_id, users.created_at, users.updated_id, users.updated_at, users.deleted_id, users.deleted_at FROM users WHERE users.username = $1]
2026-01-29T04:50:47.629Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 01021635328
2026-01-29T04:50:48.591Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [SELECT users.id, users.uid, users.username, users.email, users.nickname, users.password, users.role, users.address, users.detail_address, users.phone, users.mood, users.mood_emoji, users.mbti, users.exp_points, users.tier_id, users.asset_uid, users.school_id, users.school_updated_at, users.class_id, users.grade, users.gender, users.birthday, users.profile_decoration_code, users.invite_code, users.invite_joins, users.location_mode, users.year, users.created_id, users.created_at, users.updated_id, users.updated_at, users.deleted_id, users.deleted_at FROM users WHERE users.username = $1]
2026-01-29T04:50:48.592Z DEBUG 1 --- [hiteen] [tor-tcp-epoll-4] io.r2dbc.postgresql.PARAM                : Bind parameter [0] to: 01053044102
^C[root@hiteen-api-2 ~]# ^C
[root@hiteen-api-2 ~]#