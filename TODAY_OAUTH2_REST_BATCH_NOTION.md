# 📚 Spring Boot 학습 정리 — Batch, REST API, HTTP, OAuth2 카카오 로그인

> 작성 기준: 2026-08-31 프로젝트의 실제 수정 코드  
> 학습 목표: 원본 데이터가 어디서 오며, 서버가 어떻게 읽고 처리해 로그인 사용자로 만드는지 전체 흐름을 이해한다.

---

## 0. 오늘 배운 내용 한눈에 보기 🧭

- **Spring Boot Batch**는 대량 데이터를 자동으로 나누어 안정적으로 처리하는 일괄 처리 프레임워크다.
- **REST API**는 URL, HTTP Method, 매개값, Header 등을 이용해 클라이언트와 서버가 자원을 주고받는 방식이다.
- **AJAX**는 페이지 전체를 다시 불러오지 않고 서버와 통신하는 방식이며 jQuery, `fetch`, Axios 등으로 구현한다.
- **OAuth2**는 사용자가 카카오 같은 외부 서비스에 로그인하고, 우리 서비스가 허가받은 사용자 정보에 접근하는 절차다.
- 카카오 로그인 성공 후 우리 서버는 카카오가 준 **인가 코드(authorization code)**를 토큰으로 교환한다.
- 그 토큰으로 카카오 사용자 정보 API를 호출하고, 응답 JSON의 `kakao_account.email`을 읽는다.
- 우리 DB에 같은 이메일이 없으면 소셜 회원을 만들고, 있으면 기존 회원을 로그인 사용자로 사용한다.
- `MemberDTO`는 일반 로그인용 `UserDetails`와 소셜 로그인용 `OAuth2User`를 함께 구현한다.

> ⭐ 오늘의 핵심: **원본 데이터의 구조를 정확히 확인한 뒤 원하는 값을 꺼내야 한다.**  
> 카카오 응답에서 이메일은 최상위가 아니라 `kakao_account → email` 안에 있다.

---

## 1. Spring Boot Batch란? ⚙️

Spring Boot Batch는 **대량의 데이터를 자동화하여 효율적으로 처리하는 일괄 처리(Batch) 프레임워크**다.

예를 들면 다음과 같은 작업에 적합하다.

- 매일 새벽 주문 100만 건을 정산한다.
- 휴면 대상 회원을 찾아 상태를 변경한다.
- CSV 파일의 데이터를 DB로 옮긴다.
- 대량 메일 발송 대상을 읽고 발송 결과를 저장한다.

일반적인 Spring Batch 흐름은 다음과 같다.

```text
Job
 └─ Step
     ├─ ItemReader    : 원본 데이터를 읽는다.
     ├─ ItemProcessor : 데이터를 가공한다.
     └─ ItemWriter    : 처리 결과를 저장한다.
```

### 원본 데이터를 읽는 방법이 중요한 이유

원본이 DB인지, CSV인지, JSON인지에 따라 Reader와 읽는 방법이 달라진다.

```text
DB  → 컬럼명과 자료형 확인
CSV → 열의 순서, 구분자, 헤더 확인
JSON → 중첩 객체와 배열 구조 확인
API → URL, Method, Header, 응답 JSON 구조 확인
```

카카오 로그인에서도 원리는 같다. 카카오가 제공한 원본 JSON 구조를 먼저 확인하고 `kakao_account.email`을 읽어야 한다.

### ⚠️ 현재 프로젝트의 `removeBatch`와 Spring Batch의 차이

게시판의 `removeBatch`는 체크된 게시물 번호 여러 개를 한 HTTP 요청으로 삭제하는 **다건 삭제 기능**이다.

```java
@PostMapping("/removeBatch")
public String removeBatch(@RequestParam List<Long> bnos, ...) {
    boardService.removeBatch(bnos);
    return "redirect:/board/list";
}
```

이름에 Batch가 있지만 Spring Batch의 `Job`, `Step`, `ItemReader`, `ItemWriter`를 사용하는 것은 아니다.

| 구분 | 게시판 `removeBatch` | Spring Batch |
|---|---|---|
| 목적 | 사용자가 선택한 게시물 여러 건 삭제 | 대량 데이터의 자동 일괄 처리 |
| 실행 계기 | HTTP 요청 | 스케줄, 명령, 운영 작업 등 |
| 처리 규모 | 비교적 소량 | 수천~수백만 건 가능 |
| 재시작·실패 관리 | 직접 구현 | 프레임워크가 지원 |

---

## 2. API와 함수 호출의 관계 🧩

카카오 Developers는 우리 서비스가 사용할 수 있는 로그인·사용자 정보 API를 제공한다. 개념적으로는 다른 개발자가 만든 함수를 규칙에 맞게 호출하는 것과 비슷하다.

```javascript
// 카카오가 함수의 기능을 선언하고 구현해 서비스를 제공한다고 비유할 수 있다.
function add(a, b) {
    return a + b;
}

// 나는 제공된 기능을 호출해 결과를 받는다.
let sum = add(10, 20);
```

실제 카카오 API는 같은 프로그램 내부의 JavaScript 함수가 아니므로 HTTP를 통해 호출한다.

```text
함수 호출: add(10, 20)
API 호출 : GET https://kapi.kakao.com/v2/user/me
           Authorization: Bearer 접근_토큰
```

API를 사용하려면 제공자가 정한 다음 규약을 지켜야 한다.

- 요청 URL
- HTTP Method
- 요청 매개값
- 인증 방법
- 요청·응답 Content-Type
- 응답 데이터 구조

---

## 3. REST API 기초 🌐

REST API는 URL로 자원을 표현하고 HTTP Method로 수행할 작업을 표현하는 방식이다.

| Method | 대표 의미 | 예시 |
|---|---|---|
| GET | 조회 | `GET /replies/10` |
| POST | 생성 | `POST /replies/` |
| PUT | 전체 수정 또는 수정 | `PUT /replies/10` |
| DELETE | 삭제 | `DELETE /replies/10` |

현재 프로젝트의 `ReplyController`가 좋은 예다.

```java
@RestController
@RequestMapping("/replies")
public class ReplyController {

    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> register(@RequestBody ReplyDTO replyDTO) { ... }

    @GetMapping("/list/{bno}")
    public PageResponseDTO<ReplyDTO> list(@PathVariable Long bno, ...) { ... }

    @GetMapping("/{rno}")
    public ReplyDTO read(@PathVariable long rno) { ... }

    @PutMapping("/{rno}")
    public Map<String, Long> modify(@PathVariable Long rno, @RequestBody ReplyDTO dto) { ... }

    @DeleteMapping("/{rno}")
    public Map<String, Long> remove(@PathVariable long rno) { ... }
}
```

### 매개값 전달 방법

```text
Query String : GET /board/read?bno=10
Path Variable: GET /replies/10
Form Data    : username=hong&password=1234
JSON Body    : {"replyText":"안녕하세요","writer":"hong"}
```

중요한 인증정보는 일반적으로 Header로 보낸다.

```http
Authorization: Bearer 접근_토큰
```

여기서 `Bearer`는 “이 토큰을 가진 사람에게 권한을 인정한다”는 인증 방식 이름이다.

> ⚠️ Header는 주소창에 보이지 않을 뿐 암호화 자체를 의미하지 않는다. 반드시 HTTPS를 사용해야 한다.

---

## 4. 클라이언트와 서버의 요청·응답 📮

```text
클라이언트 ───────── 요청 ─────────▶ 서버
           URL, Method, Header,
           Query/Form/JSON Body

클라이언트 ◀──────── 응답 ───────── 서버
           Status, Header, Body
```

브라우저에서 `http://서버주소`를 입력하는 것도 서버 기능을 호출하는 것이다. 클라이언트는 서버의 실행 결과를 응답으로 받는다.

서버 응답의 `Content-Type`은 본문 데이터의 종류를 알려준다.

| Content-Type | 의미 |
|---|---|
| `text/html` | HTML 문서 |
| `text/plain` | 일반 문자열 |
| `application/json` | JSON 데이터 |
| `image/jpeg` | JPEG 이진 이미지 |

프로젝트의 예시는 다음과 같다.

```java
@RestController
public class SampleJSONController {
    @GetMapping("/helloJson")
    public Map<String, Object> helloJson() {
        return Map.of("name", "hong", "age", 10);
    }
}
```

Java의 `Map`을 반환하면 Spring이 JSON으로 직렬화한다.

```json
{
  "name": "hong",
  "age": 10
}
```

### 서버가 클라이언트를 먼저 호출할 수 있는가?

일반적인 HTTP는 **클라이언트가 요청하고 서버가 응답하는 구조**이므로, 아무 요청도 없는 브라우저를 서버가 임의로 호출할 수는 없다.

실시간 알림이 필요하면 다음 기술을 별도로 사용한다.

- Polling: 클라이언트가 주기적으로 요청
- Server-Sent Events: 서버가 열린 연결로 단방향 전송
- WebSocket: 양방향 연결

Redirect 역시 서버가 브라우저를 직접 호출하는 것이 아니다. 서버가 `302` 응답과 이동할 주소를 주면 **브라우저가 새 GET 요청**을 보낸다.

---

## 5. AJAX — 화면을 유지하며 서버 호출하기 ⚡

AJAX는 페이지 전체를 새로고침하지 않고 JavaScript로 서버에 요청하는 방식이다.

대표 도구는 다음과 같다.

- jQuery의 `$.ajax()`
- 브라우저 기본 API인 `fetch()`
- 외부 라이브러리 Axios

### fetch 예시

```javascript
fetch('/replies/10', {
    method: 'GET',
    headers: {
        'Accept': 'application/json'
    }
})
.then(response => response.json())
.then(data => console.log(data));
```

JWT 방식의 API라면 다음처럼 Header에 넣는다.

```javascript
fetch('/api/members/me', {
    headers: {
        'Authorization': `Bearer ${accessToken}`
    }
});
```

현재 프로젝트의 일반 로그인은 JWT 방식이 아니라 기본적으로 **Spring Security 세션 로그인**이다. 카카오에서 받은 접근 토큰도 Spring이 OAuth2 처리 과정에서 사용하며, 프로젝트가 자체 JWT를 발급하는 코드는 현재 확인되지 않는다.

---

## 6. Redirect와 PRG 패턴 🔁

Redirect는 서버가 응답으로 다른 URL을 알려주고, 브라우저가 그 URL에 새 요청을 보내게 하는 방식이다.

```java
return "redirect:/board/list";
```

실제 흐름은 다음과 같다.

```text
브라우저 ── POST /board/register ──▶ 서버
브라우저 ◀─ 302 Location: /board/list ─ 서버
브라우저 ── GET /board/list ──────▶ 서버
브라우저 ◀─ 게시판 HTML ────────── 서버
```

이것이 **PRG(Post → Redirect → Get)** 패턴이다.

장점은 다음과 같다.

- 새로고침했을 때 POST가 중복 실행되는 것을 방지한다.
- 등록·수정·삭제 후 깔끔한 조회 URL로 이동한다.
- URL 매개값은 `RedirectAttributes.addAttribute()`로, 한 번만 쓸 메시지는 `addFlashAttribute()`로 전달할 수 있다.

---

## 7. OAuth2와 JWT를 구분하자 🔐

### OAuth2

OAuth2는 사용자의 비밀번호를 우리 서버에 주지 않고, 외부 서비스의 자원에 접근할 권한을 위임하는 절차다.

### JWT

JWT는 정보를 `header.payload.signature` 형태로 표현하는 토큰 형식이다. OAuth2의 토큰이 반드시 JWT인 것은 아니다.

따라서 다음 문장은 구분해서 이해해야 한다.

```text
부정확한 표현: 카카오와 인증하기 위한 JWT를 무조건 받는다.
정확한 표현  : 인가 코드를 카카오 접근 토큰으로 교환한다.
               그 토큰은 제공자 정책에 따라 JWT일 수도, 아닐 수도 있다.
```

우리 서버가 자체 JWT 인증을 사용한다면 API 요청에서 다음처럼 보낸다.

```http
Authorization: Bearer 자체_JWT
```

하지만 현재 프로젝트는 `SecurityFilterChain`, 세션, Remember-Me를 사용하는 구조이며 자체 JWT 발급·검증 코드는 없다.

---

## 8. 카카오 로그인 전체 흐름 🚕

등장인물은 세 명이다.

- 👤 클라이언트: 사용자의 브라우저
- 🏠 나의 서버: Spring Boot 프로젝트
- 💬 카카오 인증·자원 서버

### 전체 순서

```text
1. 브라우저가 우리 서버의 로그인 화면 요청
2. 사용자가 카카오 로그인 버튼 클릭
3. 브라우저가 카카오 인증 화면으로 이동
4. 사용자가 카카오 아이디·비밀번호로 로그인하고 정보 제공에 동의
5. 카카오가 Redirect URI로 브라우저를 돌려보내며 인가 코드 전달
6. 우리 서버가 인가 코드를 카카오 접근 토큰으로 교환
7. 우리 서버가 접근 토큰으로 카카오 사용자 정보 API 호출
8. 카카오가 사용자 정보 JSON 반환
9. 우리 서버가 kakao_account.email을 추출
10. DB에서 이메일로 회원 조회
11. 없으면 소셜 회원가입, 있으면 기존 회원 사용
12. MemberDTO를 인증 주체로 만들어 로그인 세션 완성
```

중요한 점은 5번의 Redirect다.

```text
카카오가 우리 서버를 직접 화면 전환시키는 것이 아니다.
카카오가 브라우저에 Redirect 응답을 주고,
브라우저가 우리 서버의 Redirect URI를 다시 요청한다.
```

---

## 9. 오늘 수정한 프로젝트 파일별 역할 🗂️

| 파일 | 역할 |
|---|---|
| `build.gradle` | OAuth2 Client 라이브러리 추가 |
| `application.properties` | 카카오 인증·토큰·사용자 정보 URL, Client ID, Redirect URI, Scope 설정 |
| `templates/member/login.html` | 카카오 로그인 버튼 및 시작 URL |
| `CustomSecurityConfig.java` | 폼 로그인, OAuth2 로그인, CSRF, Remember-Me 등 보안 설정 |
| `CustomOAuth2UserService.java` | 카카오 원본 사용자 정보 해석, 회원 조회·자동가입 |
| `Member.java` | 회원 DB 엔티티, `social`과 보안 상태 저장 |
| `MemberRepository.java` | 이메일로 회원과 권한을 함께 조회 |
| `MemberDTO.java` | 일반 로그인과 OAuth2 로그인에서 공통으로 사용할 인증 사용자 |
| `templates/board/list.html` | 로그인·권한 상태에 따른 메뉴 표시 및 선택 삭제 UI |

---

## 10. 설정 코드 이해하기 🔧

### 10.1 OAuth2 라이브러리

```gradle
implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
```

이 라이브러리가 다음 복잡한 일을 대신 처리한다.

- 카카오 인증 화면으로 이동
- Redirect URI에서 인가 코드 수신
- 인가 코드를 접근 토큰으로 교환
- 사용자 정보 API 호출
- OAuth2 인증 객체 생성

### 10.2 카카오 Provider 설정

```properties
spring.security.oauth2.client.provider.kakao.authorization-uri=https://kauth.kakao.com/oauth/authorize
spring.security.oauth2.client.provider.kakao.user-name-attribute=id
spring.security.oauth2.client.provider.kakao.token-uri=https://kauth.kakao.com/oauth/token
spring.security.oauth2.client.provider.kakao.user-info-uri=https://kapi.kakao.com/v2/user/me
```

- `authorization-uri`: 사용자 로그인·동의 화면
- `token-uri`: 인가 코드를 접근 토큰으로 교환
- `user-info-uri`: 접근 토큰으로 사용자 정보 조회
- `user-name-attribute=id`: 카카오 응답에서 사용자를 식별하는 대표 속성

### 10.3 Client 등록 설정

```properties
spring.security.oauth2.client.registration.kakao.client-name=kakao
spring.security.oauth2.client.registration.kakao.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.kakao.redirect-uri=http://localhost:8380/login/oauth2/code/kakao
spring.security.oauth2.client.registration.kakao.client-id=${KAKAO_CLIENT_ID}
spring.security.oauth2.client.registration.kakao.client-secret=${KAKAO_CLIENT_SECRET}
spring.security.oauth2.client.registration.kakao.client-authentication-method=POST
spring.security.oauth2.client.registration.kakao.scope=profile_nickname,profile_image,account_email
```

- `authorization_code`: 인가 코드 방식
- `redirect-uri`: 카카오 인증 후 브라우저가 돌아올 우리 서버 주소
- `scope`: 요청할 사용자 정보 범위

> 🚨 실제 프로젝트 파일에 Client Secret이 평문으로 노출되어 있다. 이미 노출된 키는 카카오 Developers에서 재발급하고 환경변수 또는 외부 비밀 설정으로 옮겨야 한다. Git 기록에 들어갔다면 파일만 수정해도 기존 기록에서는 사라지지 않는다.

---

## 11. 로그인 버튼에서 카카오로 이동하기 🟡

```html
<a href="/oauth2/authorization/kakao">
    <img src="/assets/kakao_login_medium_narrow.png" />
</a>
```

`/oauth2/authorization/kakao`는 직접 만든 Controller 주소가 아니다. Spring Security OAuth2 Client가 제공하는 로그인 시작 주소다.

```text
사용자 클릭
 → Spring Security가 kakao 등록 설정 확인
 → 카카오 authorization-uri와 필요한 매개값 생성
 → 카카오 로그인 화면으로 Redirect
```

---

## 12. `CustomOAuth2UserService` 코드 흐름 완전 이해하기 🧠

### 12.1 `loadUser()`는 언제 실행되는가?

```java
@Override
public OAuth2User loadUser(OAuth2UserRequest userRequest) {
    ClientRegistration clientRegistration = userRequest.getClientRegistration();
    String clientName = clientRegistration.getClientName();

    OAuth2User oAuth2User = super.loadUser(userRequest);
    Map<String, Object> paramMap = oAuth2User.getAttributes();

    String email = "";
    switch (clientName) {
        case "kakao":
            email = getKakaoEmail(paramMap);
            break;
    }

    return generateDTO(email, paramMap);
}
```

Spring이 인가 코드를 접근 토큰으로 교환한 다음 실행된다.

1. `userRequest`에서 로그인 제공자 정보를 얻는다.
2. `super.loadUser(userRequest)`가 접근 토큰으로 카카오 사용자 정보 API를 호출한다.
3. 반환된 `OAuth2User`에서 원본 속성 Map을 얻는다.
4. 카카오 응답 구조에 맞게 이메일을 꺼낸다.
5. 이메일을 이용해 우리 서비스의 로그인 사용자 DTO를 만든다.

### 12.2 원본 데이터 구조 읽기

카카오 응답은 개념적으로 다음과 같다.

```json
{
  "id": 123456789,
  "properties": {
    "nickname": "홍길동"
  },
  "kakao_account": {
    "profile_nickname_needs_agreement": false,
    "email_needs_agreement": false,
    "email": "hong@example.com"
  }
}
```

이메일은 최상위 속성이 아니므로 다음 코드는 틀리다.

```java
paramMap.get("email"); // null 가능
```

먼저 `kakao_account` Map을 꺼내고, 그 안에서 `email`을 읽어야 한다.

```java
private String getKakaoEmail(Map<String, Object> paramMap) {
    Object value = paramMap.get("kakao_account");

    if (value != null) {
        Map<String, Object> accountMap = (Map<String, Object>) value;
        return (String) accountMap.get("email");
    }

    return null;
}
```

> `LinkedHashMap`으로 고정 캐스팅하기보다 `Map<String, Object>` 인터페이스로 다루는 편이 구현체 변경에 덜 민감하다.

### 12.3 이메일로 회원 조회와 자동 회원가입

```java
Optional<Member> result = memberRepository.findByEmail(email);

if (result.isEmpty()) {
    member = Member.builder()
            .mid(email)
            .email(email)
            .social(true)
            .del(false)
            .failCount(0)
            .accountLocked(false)
            .enabled(true)
            .build();

    member.addRole(MemberRole.USER);
    memberRepository.save(member);
} else {
    member = result.get();
}
```

정책은 다음과 같다.

```text
카카오 이메일과 같은 회원이 없는가?
 ├─ 예  → social=true인 USER 회원으로 자동가입
 └─ 아니오 → 기존 Member 사용
```

`Optional<Member>`는 조회 결과가 없을 가능성을 명시적으로 표현한다.

### 12.4 Entity를 인증 DTO로 변환

```java
MemberDTO result = new MemberDTO(
    member.getMid(),
    member.getMpw(),
    member.getEmail(),
    member.isDel(),
    member.getFailCount(),
    member.isAccountLocked(),
    member.getExpiredDate(),
    member.getCredentialExpiredDate(),
    member.isEnabled(),
    member.getRoleSet()
);

result.setProps(paramMap);
return result;
```

DB의 회원 정보와 카카오가 제공한 원본 속성을 하나의 로그인 사용자 객체로 합친다.

```java
private Map<String, Object> props; // 소셜 로그인 원본 정보
```

이 `props` 덕분에 `MemberDTO.getAttributes()`가 카카오 원본 속성을 반환할 수 있다.

---

## 13. `MemberDTO`가 두 인터페이스를 구현하는 이유 👥

```java
public class MemberDTO implements UserDetails, OAuth2User
```

- `UserDetails`: 아이디·비밀번호 기반 일반 로그인 사용자를 표현
- `OAuth2User`: 카카오 같은 OAuth2 로그인 사용자를 표현

즉, 프로젝트는 로그인 방법이 달라도 최종 인증 사용자를 `MemberDTO`로 통일하려는 구조다.

### 주요 메서드

```java
getUsername()             // 회원 아이디
getPassword()             // 암호화된 비밀번호
getAuthorities()          // ROLE_USER, ROLE_ADMIN 등 권한
isAccountNonExpired()     // 계정 사용기간 확인
isAccountNonLocked()      // 계정 잠금 확인
isCredentialsNonExpired() // 비밀번호 사용기간 확인
isEnabled()               // 활성화 및 탈퇴 여부 확인
getAttributes()           // 카카오 원본 사용자 속성
getName()                 // OAuth2 사용자의 대표 이름
```

권한 변환 코드는 `USER`를 Spring Security가 이해하는 `ROLE_USER`로 바꾼다.

```java
new SimpleGrantedAuthority("ROLE_" + memberRole.name())
```

---

## 14. Repository에서 원본 회원을 읽는 방법 🗄️

```java
@EntityGraph(attributePaths = "roleSet")
Optional<Member> findByEmail(String email);
```

Spring Data JPA가 메서드 이름을 분석해 다음 의미의 쿼리를 만든다.

```sql
SELECT * FROM member WHERE email = ?;
```

`@EntityGraph(attributePaths = "roleSet")`은 지연 로딩인 권한도 회원 조회 시 함께 준비하게 한다. 인증 객체를 만드는 순간에는 권한이 필요하므로 중요하다.

---

## 15. `Member` 엔티티의 소셜·보안 상태 🛡️

```java
private boolean social;
private int failCount;
private boolean accountLocked;
private LocalDate expiredDate;
private LocalDate credentialExpiredDate;
private boolean enabled;
private boolean del;
```

| 필드 | 의미 |
|---|---|
| `social` | 소셜 로그인으로 가입한 회원인지 여부 |
| `failCount` | 일반 로그인 실패 횟수 |
| `accountLocked` | 계정 잠금 여부 |
| `expiredDate` | 계정 사용 만료일 |
| `credentialExpiredDate` | 비밀번호 만료일 |
| `enabled` | 관리자 기준 활성화 여부 |
| `del` | 회원 탈퇴 여부 |

일반 로그인은 5회 실패하면 잠기고, 성공하면 실패 횟수를 초기화한다.

```java
public void increaseFailCount() {
    failCount++;
    if (failCount >= 5) accountLocked = true;
}

public void resetLoginFailure() {
    failCount = 0;
    accountLocked = false;
}
```

---

## 16. `CustomSecurityConfig`에서 하는 일 🚦

현재 보안 설정은 다음 기능을 묶는다.

- BCrypt 비밀번호 암호화
- CSRF 토큰을 Cookie와 Header로 사용
- 일반 폼 로그인
- 로그인 실패 원인별 처리
- 성공 시 실패 횟수 초기화
- DB 기반 Remember-Me
- 403 처리
- 카카오 OAuth2 로그인

### 일반 로그인과 OAuth2 로그인 비교

```text
일반 로그인
로그인 Form → username/password → CustomUserDetailsService
→ DB 회원 조회 → 비밀번호 비교 → MemberDTO → 세션

카카오 로그인
카카오 버튼 → 카카오 인증 → 인가 코드 → 접근 토큰
→ CustomOAuth2UserService → 카카오 사용자 정보
→ 이메일로 DB 회원 조회/가입 → MemberDTO → 세션
```

### Custom OAuth2 서비스의 명시적 연결 권장

현재 코드는 다음 설정만 있다.

```java
http.oauth2Login().loginPage("/member/login");
```

작성한 `CustomOAuth2UserService`가 사용자 정보 처리에 확실히 사용되도록 다음과 같이 명시적으로 연결하는 구성이 이해하기 쉽고 안전하다.

```java
private final CustomOAuth2UserService customOAuth2UserService;

http.oauth2Login(oauth -> oauth
    .loginPage("/member/login")
    .userInfoEndpoint(userInfo ->
        userInfo.userService(customOAuth2UserService)
    )
);
```

사용 중인 Spring Security 버전에 맞는 DSL 문법을 적용해야 한다.

---

## 17. 가장 복잡한 결정: 소셜 회원 처리 정책 🤔

소셜 로그인 설정 자체보다 더 어려운 부분은 “받은 이메일을 우리 회원과 어떻게 연결할 것인가?”이다.

### 경우 1: 같은 이메일이 없음

- `social=true`로 신규 회원 생성
- 기본 권한 `USER` 부여
- 추가 정보가 필요하면 최초 로그인 후 프로필 완성 화면으로 이동

### 경우 2: 같은 이메일의 소셜 회원이 있음

- 기존 회원을 조회하여 로그인
- 카카오 프로필을 갱신할 것인지 정책 결정

### 경우 3: 같은 이메일의 일반 회원이 있음

현재 코드는 그대로 기존 회원으로 로그인시킨다. 하지만 이메일이 같다는 이유만으로 계정을 자동 연결하면 계정 탈취 가능성을 검토해야 한다.

가능한 정책은 다음과 같다.

1. 일반 로그인으로 본인 확인 후 카카오 계정을 연결한다.
2. 인증 메일을 보내 본인 확인 후 연결한다.
3. 자동 연결하지 않고 별도의 안내를 보여준다.

### 경우 4: 카카오가 이메일을 주지 않음

사용자가 이메일 제공에 동의하지 않았거나 카카오 계정에 이메일이 없을 수 있다. 현재 코드라면 `email=null`이 되어 조회나 저장 과정에서 오류가 발생할 수 있다.

필요한 방어 로직 예시:

```java
if (email == null || email.isBlank()) {
    throw new OAuth2AuthenticationException("카카오 이메일 제공 동의가 필요합니다.");
}
```

또는 카카오 고유 `id`를 `provider + providerId` 형태로 별도 저장하는 더 안정적인 회원 연결 모델을 사용할 수 있다.

---

## 18. 현재 코드에서 꼭 점검할 사항 ✅

### 긴급

- [ ] 노출된 카카오 Client Secret 재발급
- [ ] Client ID·Secret을 환경변수로 이동
- [ ] 카카오 Developers의 Redirect URI가 정확히 일치하는지 확인

### 기능

- [ ] `CustomOAuth2UserService`를 Security 설정에 명시적으로 연결
- [ ] 이메일 제공 거부·누락 처리
- [ ] 일반 회원과 같은 이메일일 때 계정 연결 정책 결정
- [ ] DB의 `email`에 유일성 제약 적용 검토
- [ ] 카카오 `id` 또는 provider 정보를 별도 저장할지 결정
- [ ] 소셜 회원의 `mpw=null`이 일반 로그인 경로에 들어가지 않도록 방지

### 코드 품질

- [ ] 사용하지 않는 `PasswordEncoder` 필드와 import 정리
- [ ] `LinkedHashMap` 대신 `Map` 사용 검토
- [ ] `switch`의 미지원 Provider 처리
- [ ] 로그에 토큰·Client Secret·전체 민감정보가 출력되지 않는지 확인
- [ ] `getAuthorities()`에서 권한이 없을 때 `null` 대신 빈 컬렉션 반환

### 게시판 UI

- [ ] 선택 삭제 버튼은 서버에서 ADMIN만 허용되므로 화면에서도 ADMIN에게만 표시
- [ ] 검색 Form이 POST이므로 CSRF hidden input이 실제로 포함되는지 확인

---

## 19. 개념을 하나의 예시로 연결하기 🎯

사용자 `hong@example.com`이 처음 카카오 로그인한다고 가정한다.

```text
① 로그인 페이지에서 카카오 버튼 클릭
② /oauth2/authorization/kakao 요청
③ 카카오 로그인·동의 화면으로 Redirect
④ 성공 후 /login/oauth2/code/kakao?code=abc... 요청
⑤ Spring이 code를 access token으로 교환
⑥ access token으로 /v2/user/me 요청
   Authorization: Bearer access_token
⑦ 카카오 JSON 응답 수신
⑧ attributes["kakao_account"]["email"] 추출
⑨ findByEmail("hong@example.com") 실행
⑩ 회원이 없으므로 Member 저장
⑪ USER 권한과 카카오 props를 담은 MemberDTO 생성
⑫ Spring Security 인증 세션 생성
⑬ 이후 Principal의 이름은 hong@example.com
```

두 번째 로그인에서는 ⑩의 회원가입을 하지 않고 기존 회원을 재사용한다.

---

## 20. 최종 암기표 📝

```text
REST API = URL + Method + Parameter/Body + Header + Response

AJAX = JavaScript로 화면 전체 새로고침 없이 HTTP 요청

Authorization: Bearer 토큰
= Header에 Bearer 인증 토큰 전달

Redirect
= 서버가 새 주소를 응답하고 브라우저가 그 주소로 다시 요청

PRG
= POST 처리 → Redirect 응답 → GET 조회

OAuth2
= 외부 서비스 접근 권한 위임 절차

JWT
= 토큰을 표현하는 한 가지 형식

카카오 로그인
= 인가 코드 → 접근 토큰 → 사용자 정보 → 이메일 추출
  → DB 회원 조회/가입 → MemberDTO → 로그인 세션

원본 데이터 읽기
= 원본 구조를 먼저 확인한 뒤 정확한 경로로 값을 꺼낸다.
```

---

## 21. 스스로 설명해 보는 복습 질문 🙋

1. REST API에서 URL과 HTTP Method는 각각 무엇을 나타내는가?
2. GET, POST, PUT, DELETE의 대표적인 용도는 무엇인가?
3. 인증 토큰을 Header에 보낼 때 어떤 형식을 사용하는가?
4. Header가 주소창에 보이지 않는다는 것이 암호화를 의미하는가?
5. AJAX를 구현하는 세 가지 도구는 무엇인가?
6. Redirect 때 서버가 브라우저를 직접 호출하는가?
7. PRG가 중복 등록을 방지하는 이유는 무엇인가?
8. OAuth2와 JWT의 차이는 무엇인가?
9. 카카오가 Redirect URI로 보내는 첫 번째 값은 접근 토큰인가, 인가 코드인가?
10. `super.loadUser(userRequest)`는 어떤 일을 하는가?
11. 카카오 응답에서 이메일을 왜 `paramMap.get("email")`로 읽을 수 없는가?
12. `findByEmail()`의 결과가 `Optional`인 이유는 무엇인가?
13. `MemberDTO`가 `UserDetails`와 `OAuth2User`를 모두 구현하는 이유는 무엇인가?
14. 같은 이메일의 일반 회원이 있을 때 자동 연결이 위험할 수 있는 이유는 무엇인가?
15. 게시판 `removeBatch`와 Spring Batch는 어떻게 다른가?

### 한 문장으로 설명할 수 있으면 성공 🎉

> 카카오 로그인은 브라우저가 카카오와 우리 서버 사이를 Redirect로 이동하고, 우리 서버가 인가 코드를 접근 토큰으로 교환한 뒤 사용자 정보 JSON에서 이메일을 읽어 우리 DB 회원과 연결하고 `MemberDTO`를 로그인 사용자로 만드는 과정이다.

