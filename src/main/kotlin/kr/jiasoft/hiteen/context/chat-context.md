채팅 도메인 관련 요구사항:  

1. chat_rooms 에 room_name(채팅방 이름), invite_mode(채팅방 친구 초대 권한 [OWNER| ALL_MEMBERS] ), asset_uid(채팅방 썸네일) 정보 추가
2. ChatService.getRoomByUid 에 chat_rooms정보와 참여중인 유저정보(user_id, user_uid, chat_user_id, asset_uid, nickname) 함께 반환하도록 수정
3. 회원 채팅방 초대 기능 추가 (invite_mode가 OWNER인 경우 방장만 초대 가능, ALL_MEMBERS인 경우 참여중인 회원 모두 초대 가능)


## 채팅 관련 테이블

```cookie
CREATE TABLE public.chat_rooms (
	id bigserial NOT NULL,
	uid uuid DEFAULT gen_random_uuid() NULL,
	last_user_id int8 NULL,
	last_message_id int8 NULL,
	created_id int8 NULL,
	created_at timestamptz DEFAULT now() NULL,
	updated_id int8 NULL,
	updated_at timestamptz NULL,
	deleted_id int8 NULL,
	deleted_at timestamptz NULL,
	CONSTRAINT chat_rooms_pkey PRIMARY KEY (id),
	CONSTRAINT chat_rooms_last_msg_fk FOREIGN KEY (last_message_id) REFERENCES public.chat_messages(id) ON DELETE SET NULL
);

CREATE TABLE public.chat_users (
	id bigserial NOT NULL,
	chat_room_id int8 NOT NULL,
	user_id int8 NOT NULL,
	last_read_message_id int8 NULL,
	last_read_at timestamptz NULL,
	status int2 NULL,
	push bool NULL,
	push_at timestamptz NULL,
	joining_at timestamptz NULL,
	leaving_at timestamptz NULL,
	deleted_at timestamptz NULL,
	CONSTRAINT chat_users_chat_room_id_user_id_key UNIQUE (chat_room_id, user_id),
	CONSTRAINT chat_users_pkey PRIMARY KEY (id),
	CONSTRAINT chat_users_chat_room_id_fkey FOREIGN KEY (chat_room_id) REFERENCES public.chat_rooms(id) ON DELETE CASCADE,
	CONSTRAINT chat_users_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);
CREATE INDEX idx_chat_users_room_user_active ON public.chat_users USING btree (chat_room_id, user_id) WHERE (deleted_at IS NULL);



CREATE TABLE public.chat_messages (
	id bigserial NOT NULL,
	chat_room_id int8 NOT NULL,
	user_id int8 NOT NULL,
	uid uuid DEFAULT gen_random_uuid() NOT NULL,
	"content" text NULL,
	kind int2 DEFAULT 0 NOT NULL,
	emoji_code varchar(64) NULL,
	emoji_count int2 NULL,
	created_at timestamptz DEFAULT now() NULL,
	updated_at timestamptz NULL,
	deleted_at timestamptz NULL,
	CONSTRAINT chat_messages_pkey PRIMARY KEY (id),
	CONSTRAINT chat_messages_chat_room_id_fkey FOREIGN KEY (chat_room_id) REFERENCES public.chat_rooms(id) ON DELETE CASCADE,
	CONSTRAINT chat_messages_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);


CREATE TABLE public.chat_messages_assets (
	id bigserial NOT NULL,
	uid uuid NOT NULL,
	message_id int8 NOT NULL,
	width int4 NULL,
	height int4 NULL,
	CONSTRAINT chat_messages_assets_pkey PRIMARY KEY (id),
	CONSTRAINT chat_messages_assets_message_id_fkey FOREIGN KEY (message_id) REFERENCES public.chat_messages(id),
	CONSTRAINT chat_messages_assets_uid_fkey FOREIGN KEY (uid) REFERENCES public.assets(uid)
);


```