package kr.jiasoft.hiteen.feature.user

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails


@Table(name = "tb_users")
data class UserEntity(
    @Id
    val id: Long? = null,
    val name: String?,
    val password: String?,
    val role: String?,
)

fun UserEntity.toUserDetails(): UserDetails {
    return User.builder()
        .username(name ?: "")
        .password(password ?:"")
        .roles(role ?: "USER")
        .build()
}



fun UserEntity.toClaims(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "role" to role,
//    "email" to email,
//    "provider" to provider,
//    "profileImage" to profileImage
)




/*
 * 사용자(User) 엔티티 클래스 - DB 'user' 테이블 매핑
ID	NAME	TYPE	NULL	DEFAULT	MEMO
id	회원번호	bigint	N
uid	회원UID	varchar(100)	Y
type	회원그룹	enum	N	User	User,Admin
phone	휴대폰번호	varchar(200)	N
teen_id	틴 아이디	varchar(50)	Y		사용할 수 없는 아이디 설정
password	비밀번호	varchar(255)	Y
name	회원명	varchar(100)	Y
birthday	생년월일	date	Y
gender	성별	enum	Y		M,F
school_year	학년도	smallint	Y
school_id	학교번호	int	Y
class_id	학급번호	bigint	Y
school_type	학교구분	tinyint	Y		초등학교/중학교/고등학교/기타
school_name	학교명	varchar(50)	Y
class_name	학급명	varchar(100)	Y		3학년 3반
grade	학년	varchar(50)	Y		3학년
auth_school	학교 인증여부	tinyint	N	0
auth_class	반친구 인증여부	tinyint	N	0
image	회원 이미지 파일 UID	varchar(100)	Y
mbti	MBTI	char(4)	Y
vote	받은 투표수	int	N	0
point	포인트	int	N	0
sido	시/도	varchar(20)	Y		광주
my_teens	보유 틴갯수	int	N	0
teens	받은 틴갯수	int	N	0
interests	관심사	varchar(255)	Y
invite_code	초대코드	varchar(20)	Y
invite_joins	초대 후 가입자수	int	N	0
poll_votes	받은 α-투표수	int	N	0
poll_comments	받은 α-투표 댓글수	int	N	0
status	가입상태	tinyint	N	0	대기/승인/차단/탈퇴
created_at	가입일시	datetime	Y
updated_at	수정일시	datetime	Y
deleted_at	삭제일시	datetime	Y
 */
//@Table(name = "tb_users")
//data class UserEntity(
//
//    // 회원번호 (PK)
//    @Id
//    val id: Long? = null,
//
//    // 회원UID (외부 시스템용 고유값)
//    val uid: String?,
//
//    // 회원그룹 (User / Admin)
//    val type: String,
//
//    // 휴대폰번호
//    val phone: String,
//
//    // 틴 아이디 (사용할 수 없는 아이디 방지용)
//    val teenId: String?,
//
//    // 비밀번호 (암호화 저장)
//    val password: String?,
//
//    // 회원명
//    val name: String?,
//
//    // 생년월일 (YYYY-MM-DD)
//    val birthday: String?,
//
//    // 성별 (M, F)
//    val gender: String?,
//
//    // 학년도
//    val schoolYear: Int?,
//
//    // 학교번호
//    val schoolId: Int?,
//
//    // 학급번호
//    val classId: Long?,
//
//    // 학교구분 (초등/중등/고등/기타)
//    val schoolType: Int?,
//
//    // 학교명
//    val schoolName: String?,
//
//    // 학급명 (예: 3학년 3반)
//    val className: String?,
//
//    // 학년 (예: 3학년)
//    val grade: String?,
//
//    // 학교 인증여부 (0/1)
//    val authSchool: Int,
//
//    // 반친구 인증여부 (0/1)
//    val authClass: Int,
//
//    // 회원 이미지 파일 UID
//    val image: String?,
//
//    // MBTI (예: INFP)
//    val mbti: String?,
//
//    // 받은 투표수
//    val vote: Int = 0,
//
//    // 포인트
//    val point: Int = 0,
//
//    // 시/도 (예: 광주)
//    val sido: String?,
//
//    // 보유 틴갯수
//    val myTeens: Int = 0,
//
//    // 받은 틴갯수
//    val teens: Int = 0,
//
//    // 관심사 (콤마 구분 등)
//    val interests: String?,
//
//    // 초대코드
//    val inviteCode: String?,
//
//    // 초대 후 가입자수
//    val inviteJoins: Int = 0,
//
//    // 받은 α-투표수
//    val pollVotes: Int = 0,
//
//    // 받은 α-투표 댓글수
//    val pollComments: Int = 0,
//
//    // 가입상태 (0: 대기, 1: 승인, 2: 차단, 3: 탈퇴 등)
//    val status: Int = 0,
//
//    // 가입일시 (생성일, ISO-8601 문자열 등)
//    val createdAt: String?,
//
//    // 수정일시
//    val updatedAt: String?,
//
//    // 삭제일시
//    val deletedAt: String?
//)
