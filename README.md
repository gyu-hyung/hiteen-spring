# TBMQ Minikube deployment scripts

This folder containing scripts and Kubernetes resources configurations to run TBMQ on Minikube cluster.

You can find the deployment guide by the [**link**](https://thingsboard.io/docs/mqtt-broker/install/cluster/minikube-cluster-setup/).




-- BOARD ------------------------------------------------------------------------------------
select * from boards;

select * from board_assets

select * from board_likes

select * from board_comments

select * from board_comment_likes

-- CHAT ------------------------------------------------------------------------------------
select * from users

select * from chat_rooms cr

select * from chat_users

select * from chat_messages


{"type":"send","content":"채팅!!!","clientMsgId":"msg-001","assetUids":[]}
{"type":"read","lastMessageUid":"6a74e624-ce7c-47e7-b0a2-61ed64240de3"}
{"type":"typing","isTyping":true}

wscat -c "ws://localhost:8080/ws/chat?token=Bearer%20<JWT>&since=0"

-- ================ CHAT1 ===================
--inbox
wscat -c "ws://localhost:8080/ws/inbox?token=Bearer%20eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjaGF0MSIsImlhdCI6MTc1NjgwMDIxMiwiZXhwIjoxNzU2ODg2NjEyfQ.Tv_TUyh2FUasCBdLlw15JdDi7pmWIv4Qq6l25vLBeNaflX0zpqhwCn84YM6dfHL0s40_SiMOHycloM3tX89yEg&since=0"

--chat
wscat -c "ws://localhost:8080/ws/chat?token=Bearer%20eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjaGF0MSIsImlhdCI6MTc1NjgwMDIxMiwiZXhwIjoxNzU2ODg2NjEyfQ.Tv_TUyh2FUasCBdLlw15JdDi7pmWIv4Qq6l25vLBeNaflX0zpqhwCn84YM6dfHL0s40_SiMOHycloM3tX89yEg&room=4bd422cc-c8ec-4152-a290-e136f02c8130"

--location
wscat -c "ws://localhost:8080/ws/loc?token=Bearer%20eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjaGF0MSIsImlhdCI6MTc1NjgwMDIxMiwiZXhwIjoxNzU2ODg2NjEyfQ.Tv_TUyh2FUasCBdLlw15JdDi7pmWIv4Qq6l25vLBeNaflX0zpqhwCn84YM6dfHL0s40_SiMOHycloM3tX89yEg&users=55ca3ea5-070a-440e-a1c9-af35aaa46733,203af176-d18a-41aa-b5fc-463085cc1ee7"
-- ================ CHAT1 ===================

-- ================ CHAT2 ===================
--inbox
wscat -c "ws://localhost:8080/ws/inbox?token=Bearer%20eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjaGF0MiIsImlhdCI6MTc1NjgwMDM4NCwiZXhwIjoxNzU2ODg2Nzg0fQ.JCwQ65YL9ta7rTGksYZ0Oasu5z7FYRDwy-znSd5dExGVvyeJsE_KWrananAhxi69p1tPXEAuJoQU0WnbbE__bw&since=0"

--chat
wscat -c "ws://localhost:8080/ws/chat?token=Bearer%20eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjaGF0MiIsImlhdCI6MTc1NjgwMDM4NCwiZXhwIjoxNzU2ODg2Nzg0fQ.JCwQ65YL9ta7rTGksYZ0Oasu5z7FYRDwy-znSd5dExGVvyeJsE_KWrananAhxi69p1tPXEAuJoQU0WnbbE__bw&room=4bd422cc-c8ec-4152-a290-e136f02c8130"

--location
wscat -c "ws://localhost:8080/ws/loc?token=Bearer%20eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjaGF0MiIsImlhdCI6MTc1NjgwMDM4NCwiZXhwIjoxNzU2ODg2Nzg0fQ.JCwQ65YL9ta7rTGksYZ0Oasu5z7FYRDwy-znSd5dExGVvyeJsE_KWrananAhxi69p1tPXEAuJoQU0WnbbE__bw&users=55ca3ea5-070a-440e-a1c9-af35aaa46733,203af176-d18a-41aa-b5fc-463085cc1ee7"
-- ================ CHAT2 ===================



--redis
redis-cli -h 192.168.49.2 -p 31864 -a xxxxxxxx
SUBSCRIBE chat:room:4bd422cc-c8ec-4152-a290-e136f02c8130
PUBLISH chat:room:4bd422cc-c8ec-4152-a290-e136f02c8130 '{"type":"test","msg":"hello redis"}'






