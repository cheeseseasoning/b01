# Spring Security: Remember-Me부터 회원가입까지 완전 정리

> **프로젝트 기준**: Spring Boot 2.7.3 · Spring Security 5 · Spring Data JPA · Thymeleaf · MariaDB  
> **학습 범위**: DB 기반 로그인, 권한, Remember-Me, 403 처리, 인증 정보의 화면 활용, 회원가입, 관리자용 회원 조회

---

## 0. 이 장의 핵심 한 문장

사용자가 회원가입할 때 비밀번호를 암호화해 DB에 저장하고, 로그인 시 Spring Security가 DB에서 회원과 권한을 조회해 인증 객체를 만든 뒤, 세션 또는 Remember-Me 쿠키로 로그인 상태를 유지하며, URL과 메서드마다 권한을 검사한다.

---

## 1. 전체 구조 한눈에 보기 🧭

### 주요 클래스의 역할

| 계층 | 파일 | 책임 |
|---|---|---|
| 보안 설정 | `CustomSecurityConfig` | 로그인 페이지, Remember-Me, 비밀번호 암호화기, 403 처리 등록 |
| 인증 조회 | `CustomUserDetailsService` | 로그인 아이디로 DB 회원과 권한 조회 |
| 인증 사용자 | `MemberDTO` | Spring Security가 사용하는 `UserDetails` 구현 |
| 회원 엔티티 | `Member` | 회원 아이디, 암호화된 비밀번호, 이메일, 탈퇴 상태, 권한 저장 |
| 권한 enum | `MemberRole` | 현재 `USER`, `ADMIN` 정의 |
| 저장소 | `MemberRepository` | 회원 및 권한 조회, 저장 |
| 가입 DTO | `MemberJoinDTO` | 가입 폼 데이터를 컨트롤러로 전달 |
| 조회 DTO | `MemberInfoDTO` | 비밀번호를 제외한 회원 정보를 화면에 전달 |
| 서비스 | `MemberServiceImpl` | 중복 검사, 비밀번호 암호화, 기본 권한 부여, 저장 |
| 컨트롤러 | `MemberController` | 로그인·가입 화면, 가입 처리, 관리자 회원 조회 |
| 예외 처리 | `Custom403Handler` | 로그인은 했지만 권한이 부족한 요청 처리 |

### 전체 인증 흐름

```text
[회원가입 폼]
     ↓ mid, mpw, email
[MemberController]
     ↓
[MemberServiceImpl]
     ├─ 아이디 중복 검사
     ├─ BCrypt 비밀번호 암호화
     ├─ ROLE_USER 부여
     └─ MemberRepository.save()
     ↓
[Member + Member_roleSet 테이블]

[로그인 폼]
     ↓ username, password
[Spring Security 인증 필터]
     ↓ username
[CustomUserDetailsService]
     ↓ getWithRoles(username)
[MemberRepository → DB]
     ↓ Member
[MemberDTO(UserDetails)]
     ├─ 저장된 암호화 비밀번호
     └─ ROLE_USER / ROLE_ADMIN
     ↓ BCrypt 비교
[인증 성공]
     ├─ 일반 로그인: 세션 유지
     └─ Remember-Me 선택: 쿠키 + persistent_logins 유지
```

---

## 2. 인증과 인가를 먼저 구분하자

| 개념 | 질문 | 이 프로젝트의 예 |
|---|---|---|
| **인증(Authentication)** | “누구인가?” | 아이디와 비밀번호로 `member1`임을 확인 |
| **인가(Authorization)** | “무엇을 할 수 있는가?” | `ADMIN`만 `/member/list` 접근 가능 |

> 💡 로그인 성공은 인증의 끝이지, 모든 기능을 사용할 수 있다는 뜻은 아니다. 로그인 후 요청마다 필요한 권한을 검사하는 과정이 인가이다.

### 역할(Role)과 권한(Authority)

Spring Security 내부에서는 최종적으로 문자열 권한을 사용한다.

```java
new SimpleGrantedAuthority("ROLE_" + memberRole.name())
```

따라서 `MemberRole.USER`는 `ROLE_USER`, `MemberRole.ADMIN`은 `ROLE_ADMIN`이 된다.

```java
@PreAuthorize("hasRole('ADMIN')")
```

`hasRole('ADMIN')`은 내부적으로 `ROLE_ADMIN`을 검사하므로 `ROLE_`를 직접 붙이지 않는다.

> 현재 프로젝트에 `MANAGER`는 정의되어 있지 않다. 필요하다면 `MemberRole`에 추가해야 한다.

---

## 3. 회원 정보가 DB에 저장되는 구조

### `Member` 엔티티

```java
@Entity
public class Member extends BaseEntity {
    @Id
    private String mid;
    private String mpw;
    private String email;
    private boolean del;

    @ElementCollection(fetch = FetchType.LAZY)
    private Set<MemberRole> roleSet = new HashSet<>();
}
```

- `mid`: 로그인 아이디이자 기본키
- `mpw`: 평문이 아닌 BCrypt 해시 저장
- `email`: 회원 이메일
- `del`: 논리적 탈퇴 여부
- `roleSet`: 한 회원이 가진 여러 역할
- `BaseEntity`: 가입일(`regDate`)과 수정일(`modDate`) 제공

`@ElementCollection` 때문에 권한은 별도의 컬렉션 테이블에 저장된다. 테이블명과 컬럼명은 JPA 네이밍 전략에 따라 생성된다.

### 권한을 함께 조회하는 이유

```java
@EntityGraph(attributePaths = "roleSet")
@Query("select m from Member m where m.mid = :mid")
Optional<Member> getWithRoles(@Param("mid") String mid);
```

`roleSet`은 `LAZY`이므로 평소에는 즉시 조회하지 않는다. 그러나 로그인 시에는 사용자뿐 아니라 권한도 반드시 필요하다. `@EntityGraph`는 회원과 권한을 한 조회 흐름에서 함께 가져오게 한다.

---

## 4. 회원가입 전체 흐름 ✍️

### 4.1 가입 화면 요청

```java
@GetMapping("/join")
public void joinGET() { }
```

반환형이 `void`이면 Spring MVC는 요청 경로를 기준으로 뷰 이름을 추론한다.

```text
GET /member/join → templates/member/join.html
```

### 4.2 HTML 폼 데이터 전송

```html
<form th:action="@{/member/join}" method="post">
    <input type="text" name="mid">
    <input type="password" name="mpw">
    <input type="email" name="email">
</form>
```

`name` 값이 DTO 필드명과 같기 때문에 Spring MVC가 자동 바인딩한다.

```text
name="mid"   → MemberJoinDTO.mid
name="mpw"   → MemberJoinDTO.mpw
name="email" → MemberJoinDTO.email
```

### 4.3 컨트롤러의 가입 처리

```java
@PostMapping("/join")
public String joinPOST(MemberJoinDTO dto,
                       RedirectAttributes redirectAttributes) {
    try {
        memberService.join(dto);
    } catch (MemberService.MidExistException e) {
        redirectAttributes.addFlashAttribute("error", "mid");
        return "redirect:/member/join";
    }

    redirectAttributes.addFlashAttribute("result", "success");
    return "redirect:/member/login";
}
```

- 성공: 로그인 화면으로 이동
- 아이디 중복: 가입 화면으로 다시 이동
- Flash Attribute: 다음 요청에서 한 번만 사용하는 메시지

가입 화면은 `error == 'mid'`일 때 중복 알림을 띄운다.

### 4.4 서비스의 핵심 비즈니스 로직

```java
String mid = memberJoinDTO.getMid();

if (memberRepository.existsById(mid)) {
    throw new MidExistException();
}

Member member = modelMapper.map(memberJoinDTO, Member.class);
member.changePassword(passwordEncoder.encode(memberJoinDTO.getMpw()));
member.addRole(MemberRole.USER);
memberRepository.save(member);
```

순서는 다음과 같다.

1. 같은 아이디가 있는지 검사한다.
2. 가입 DTO를 엔티티로 변환한다.
3. 비밀번호를 BCrypt로 암호화한다.
4. 신규 회원에게 기본 `USER` 역할을 부여한다.
5. 회원과 권한을 DB에 저장한다.

### 왜 비밀번호를 암호화해야 하는가?

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

BCrypt는 같은 비밀번호도 매번 다른 해시를 만들 수 있는 단방향 해시 방식이다. 로그인 시 복호화하지 않고 다음처럼 비교한다.

```text
입력한 평문 비밀번호 + 저장된 BCrypt 해시 → matches 결과
```

> ⚠️ 비밀번호를 평문으로 저장하거나 로그로 출력해서는 안 된다. DTO 전체를 로그에 남기면 현재 코드처럼 `mpw`가 노출될 수 있으므로 주의해야 한다.

---

## 5. DB 기반 로그인 흐름

### 5.1 로그인 폼의 약속된 이름

```html
<form action="/member/login" method="post">
    <input name="username">
    <input name="password" type="password">
    <input name="remember-me" type="checkbox">
</form>
```

기본 파라미터명은 반드시 `username`, `password`, `remember-me`여야 한다. 회원 엔티티 필드가 `mid`, `mpw`여도 로그인 필터가 받는 이름은 별개이다. 이름을 바꾸려면 Security 설정에서 명시해야 한다.

### 5.2 로그인 POST를 컨트롤러가 처리하지 않는 이유

컨트롤러에는 `GET /member/login`만 있고 POST 메서드가 없다. `POST /member/login`은 Spring Security의 인증 필터가 가로채서 처리한다.

```java
http.formLogin()
    .loginPage("/member/login");
```

### 5.3 DB 회원 조회

```java
public UserDetails loadUserByUsername(String username) {
    Member member = memberRepository.getWithRoles(username)
        .orElseThrow(() ->
            new UsernameNotFoundException(username + " 사용자가 존재하지 않습니다."));

    return entityToDTO(member);
}
```

Spring Security가 `CustomUserDetailsService`에 아이디를 전달하고, 서비스는 회원과 권한을 DB에서 읽어 `MemberDTO`로 반환한다.

### 5.4 `MemberDTO`가 필요한 이유

도메인 엔티티 `Member`는 우리 애플리케이션의 회원 모델이고, `UserDetails`는 Spring Security가 요구하는 인증 사용자 규격이다. `MemberDTO`가 둘 사이의 어댑터 역할을 한다.

| `UserDetails` 메서드 | 프로젝트에서 반환하는 값 |
|---|---|
| `getUsername()` | `mid` |
| `getPassword()` | 암호화된 `mpw` |
| `getAuthorities()` | `ROLE_USER`, `ROLE_ADMIN` 등 |
| `isAccountNonExpired()` | `true` |
| `isAccountNonLocked()` | `true` |
| `isCredentialsNonExpired()` | `true` |
| `isEnabled()` | 현재 무조건 `true` |

> ⚠️ `del` 필드가 있어도 현재 `isEnabled()`가 항상 `true`이므로 탈퇴 회원도 로그인할 수 있다. 논리적 탈퇴를 로그인 차단에 연결하려면 `return !del;`이 되어야 한다.

---

## 6. Remember-Me 완전 이해 🍪

### 정의

Remember-Me는 세션이 끝난 뒤에도 브라우저 쿠키를 이용해 사용자를 다시 인증하는 기능이다. 단순히 아이디만 기억하는 기능이 아니라 **인증 상태를 복원하는 자동 로그인 기능**이다.

### 현재 설정

```java
http.rememberMe()
    .key("*^kosa1004!$")
    .tokenRepository(persistentTokenRepository())
    .userDetailsService(customUserDetailsService)
    .tokenValiditySeconds(60 * 60 * 24 * 30);
```

| 설정 | 의미 |
|---|---|
| `key(...)` | Remember-Me 토큰 처리에 사용하는 애플리케이션 비밀값 |
| `tokenRepository(...)` | 토큰을 DB에 저장하는 저장소 |
| `userDetailsService(...)` | 자동 로그인 복원 시 최신 사용자·권한 조회 |
| `tokenValiditySeconds(...)` | 유효기간 30일 |

### DB 저장소 연결

```java
@Bean
public PersistentTokenRepository persistentTokenRepository() {
    JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
    repo.setDataSource(dataSource);
    return repo;
}
```

기본 테이블은 `persistent_logins`이다.

```sql
CREATE TABLE persistent_logins (
    username VARCHAR(64) NOT NULL,
    series VARCHAR(64) PRIMARY KEY,
    token VARCHAR(64) NOT NULL,
    last_used TIMESTAMP NOT NULL
);
```

| 컬럼 | 의미 |
|---|---|
| `username` | 로그인 사용자 아이디 |
| `series` | 브라우저/로그인 묶음을 식별하는 값 |
| `token` | 매 자동 인증 후 교체되는 토큰 |
| `last_used` | 마지막 사용 시각 |

### 동작 순서

1. 사용자가 로그인 폼에서 `remember-me`를 체크한다.
2. 아이디와 비밀번호 인증에 성공한다.
3. 서버가 임의의 `series`와 `token`을 DB에 저장한다.
4. 브라우저에는 Remember-Me 쿠키가 저장된다.
5. 세션이 없어도 다음 요청에서 쿠키가 전달된다.
6. 서버가 쿠키의 정보와 DB 토큰을 비교한다.
7. 일치하면 `UserDetailsService`로 회원을 다시 조회하고 인증 객체를 복원한다.
8. 사용한 토큰은 갱신되어 탈취 토큰 재사용 위험을 줄인다.

### 세션 로그인과 차이

| 구분 | 일반 로그인 | Remember-Me |
|---|---|---|
| 주 저장 위치 | 서버 세션 | 브라우저 쿠키 + DB 토큰 |
| 세션 종료 후 | 다시 로그인 | 쿠키가 유효하면 자동 인증 |
| 보안 수준 | 상대적으로 높음 | 쿠키 탈취 위험을 추가 고려 |

### 사용자 정의 테이블명

`remember_me` 같은 별도 테이블을 쓰려면 해당 프로젝트가 사용하는 Spring Security 버전에서 제공하는 SQL setter 이름을 확인하고 조회·삽입·수정·삭제 SQL을 모두 같은 테이블로 바꿔야 한다. 필기의 예시는 개념적으로 맞지만, 버전에 따라 `setSelectTokenSql`이 아니라 `setTokensBySeriesSql`, `setDeleteTokenSql`이 아니라 `setRemoveUserTokensSql`처럼 실제 메서드명이 다를 수 있다.

> ✅ 수업 프로젝트에서는 기본 `persistent_logins` 테이블을 사용하는 편이 가장 단순하고 안전하다.

### 운영 시 필수 주의사항

- `key`를 소스 코드에 고정하지 말고 환경 변수나 외부 설정으로 분리한다.
- HTTPS와 쿠키 보안 속성을 적용한다.
- 비밀번호 변경·탈퇴·강제 로그아웃 시 해당 사용자의 Remember-Me 토큰도 삭제한다.
- 공용 PC에서는 자동 로그인을 사용하지 않도록 안내한다.

---

## 7. 인증 사용자 정보를 Thymeleaf에서 사용하기

### 작성자를 현재 로그인 사용자로 표시

```html
<input type="text"
       name="writer"
       th:value="${#authentication.principal.username}"
       readonly>
```

`#authentication`은 현재 Spring Security 인증 객체이고, `principal`은 현재 로그인한 `MemberDTO`, `username`은 `MemberDTO#getUsername()`의 결과인 `mid`이다.

### 작성자에게만 수정 버튼 표시

```html
<div th:with="user=${#authentication.principal}">
    <button th:if="${user != null && user.username == dto.writer}"
            class="btn-modify">
        Modify
    </button>
</div>
```

이 코드는 사용자 경험을 개선하지만 보안 장치는 아니다. 공격자는 HTML을 수정하거나 서버에 직접 요청할 수 있다.

```text
화면 조건(th:if) = 버튼을 보여 줄지 결정
서버 조건(@PreAuthorize) = 실제 요청을 허용할지 결정
```

> ⭐ 중요한 원칙: 화면에서 숨기고 서버에서도 반드시 다시 검사한다.

---

## 8. 메서드 보안과 접근 제어

### 메서드 보안 활성화

```java
@EnableGlobalMethodSecurity(prePostEnabled = true)
```

이 설정이 있어야 `@PreAuthorize`가 동작한다.

### 현재 프로젝트의 대표 규칙

```java
@PreAuthorize("hasRole('USER')")
@GetMapping("/board/register")
```

```java
@PreAuthorize("isAuthenticated()")
@RequestMapping("/board/read")
```

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/member/list")
```

```java
@PreAuthorize("principal.username == #dto.writer")
@PostMapping("/board/modify")
```

마지막 표현식은 로그인 아이디와 요청 DTO의 작성자가 같은지 검사한다.

### 현재 코드에서 꼭 보완할 부분

- 게시글 등록 GET에는 `@PreAuthorize`가 있지만 POST에는 없다. 실제 데이터 변경 요청인 POST에도 권한 검사가 필요하다.
- 게시글 수정 화면 GET은 어노테이션 없이 `Principal`을 바로 사용하므로 비로그인 요청에서는 `NullPointerException` 위험이 있다.
- 요청 DTO의 `writer` 값만 신뢰하면 조작 가능하다. 더 안전한 방식은 게시글 번호로 원본 작성자를 DB에서 조회한 뒤 현재 사용자와 비교하는 것이다.
- URL 규칙을 `authorizeRequests()`로 함께 명시하면 공개 경로와 보호 경로가 더 분명해진다.

---

## 9. `AccessDeniedHandler`는 무엇인가? 🚫

### 401과 403

| 상황 | 의미 | 일반적인 처리 |
|---|---|---|
| 미인증 사용자가 보호 자원 요청 | 로그인하지 않음 | 로그인 페이지로 이동 |
| 인증됐지만 권한 부족 | `403 Forbidden` | `AccessDeniedHandler` 실행 |

등록 코드:

```java
http.exceptionHandling()
    .accessDeniedHandler(accessDeniedHandler());
```

```java
@Bean
public AccessDeniedHandler accessDeniedHandler() {
    return new Custom403Handler();
}
```

즉, `@PreAuthorize("hasRole('ADMIN')")` 조건을 통과하지 못하면 `Custom403Handler`가 후속 응답을 결정한다.

### 현재 핸들러의 의도

- 일반 HTML 요청: 로그인 화면으로 리다이렉트하면서 `error=ACCESS_DENIED` 전달
- JSON 요청: 상태 코드 403 반환

### 현재 구현의 문제

```java
if (!request.getContentType().startsWith("application/json")) {
```

GET 요청은 `Content-Type`이 없는 경우가 많아 `getContentType()`이 `null`일 수 있고, 이때 `startsWith()`에서 예외가 난다. 또한 요청의 `Content-Type`은 “보낸 본문의 형식”이지 “원하는 응답 형식”이므로 HTML/JSON 구분에는 `Accept` 헤더나 요청 경로 정책을 활용하는 편이 적절하다.

안전한 최소 형태는 다음과 같다.

```java
String contentType = request.getContentType();
boolean jsonRequest = contentType != null
        && contentType.startsWith("application/json");

if (jsonRequest) {
    response.sendError(HttpServletResponse.SC_FORBIDDEN);
} else {
    response.sendRedirect("/member/login?error=ACCESS_DENIED");
}
```

> 참고: 이미 로그인한 사용자를 권한 부족 때문에 다시 로그인 페이지로 보내면 이유가 혼동될 수 있다. 실제 서비스에서는 전용 `/error/403` 화면이 더 자연스럽다.

---

## 10. 관리자용 회원 목록과 상세 조회

### 회원 목록

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/list")
public void list(PageRequestDTO request, Model model) {
    model.addAttribute("pageResponseDTO", memberService.getList(request));
}
```

서비스에서는 페이지 번호를 Spring Data의 0부터 시작하는 인덱스로 변환하고 가입일 내림차순으로 조회한다.

```java
PageRequest.of(page - 1, size, Sort.by("regDate").descending())
```

`MemberInfoDTO`에는 비밀번호가 없다. 조회 화면에 엔티티나 가입 DTO를 그대로 전달하지 않고 필요한 정보만 담는 것은 중요한 보안 설계이다.

### 회원 상세

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/read")
public void read(String mid, PageRequestDTO request, Model model) {
    model.addAttribute("dto", memberService.get(mid));
}
```

회원과 권한을 조회해 아이디, 이메일, 상태, 역할, 가입일, 수정일을 보여 준다.

### 현재 구현 범위

| 기능 | 상태 |
|---|---|
| 회원가입 화면 GET | 구현 |
| 회원가입 처리 POST | 구현 |
| 아이디 중복 처리 | 구현 |
| BCrypt 암호화 | 구현 |
| 기본 USER 권한 | 구현 |
| DB 기반 로그인 | 구현 |
| Remember-Me | 구현 |
| 관리자 회원 목록 | 구현 |
| 관리자 회원 상세 | 구현 |
| 회원 정보 수정 | **미구현** |
| 회원 탈퇴 | **미구현** |

---

## 11. 현재 설정을 읽을 때의 중요 포인트

### CSRF 비활성화

```java
http.csrf().disable();
```

학습 단계에서는 폼 요청을 간단히 테스트할 수 있지만, 운영 서비스에서 CSRF를 끄면 로그인한 사용자의 권한을 악용한 위조 요청에 취약해질 수 있다. Thymeleaf와 Spring Security는 CSRF 토큰을 지원하므로 실제 서비스에서는 활성화가 원칙이다.

### 정적 자원 제외

```java
web.ignoring()
   .requestMatchers(PathRequest.toStaticResources().atCommonLocations());
```

CSS, JavaScript, 이미지 같은 일반 정적 자원은 보안 필터를 통과하지 않도록 한다.

### 요청 권한 정책

현재 `SecurityFilterChain`에는 `authorizeRequests()` 규칙이 없다. 따라서 세밀한 보호는 주로 컨트롤러의 `@PreAuthorize`에 의존한다. 공개 경로와 보호 경로를 명시하면 정책을 한눈에 이해하기 쉽다.

예시:

```java
http.authorizeRequests()
    .antMatchers("/member/login", "/member/join").permitAll()
    .antMatchers("/member/**").hasRole("ADMIN")
    .antMatchers("/board/register").hasRole("USER")
    .anyRequest().permitAll();
```

> URL 규칙과 `@PreAuthorize`를 함께 쓸 때는 서로 모순되지 않도록 설계해야 한다.

---

## 12. 자주 헷갈리는 질문

### Q1. `mid`인데 왜 로그인 폼은 `username`인가?

`username`은 Spring Security 로그인 필터의 기본 파라미터명이고, `mid`는 우리 DB 모델의 필드명이다. `MemberDTO#getUsername()`이 `mid`를 반환해 둘을 연결한다.

### Q2. `hasRole('USER')`와 `hasAuthority('ROLE_USER')`는 같은가?

현재 구조에서는 사실상 같은 권한을 검사한다. `hasRole`은 앞에 `ROLE_`를 자동으로 붙이고, `hasAuthority`는 정확한 문자열을 검사한다.

### Q3. `readonly`면 작성자를 조작할 수 없는가?

아니다. 브라우저 개발자 도구나 직접 HTTP 요청으로 값을 바꿀 수 있다. 서버가 현재 인증 사용자를 기준으로 작성자를 결정해야 안전하다.

### Q4. Remember-Me 쿠키에 비밀번호가 저장되는가?

아니다. Persistent Token 방식에서는 식별용 series/token이 사용되며 서버 DB의 토큰과 대조한다.

### Q5. 로그아웃하면 Remember-Me도 해제되는가?

정상적인 Spring Security 로그아웃 경로를 사용하면 관련 쿠키와 저장 토큰을 정리하는 흐름이 연동된다. 단순히 브라우저 창만 닫는 것은 로그아웃이 아니다.

### Q6. `th:if`로 버튼을 숨겼는데 `@PreAuthorize`가 또 필요한가?

반드시 필요하다. 화면 제어는 UX, 서버 검사는 보안이다.

---

## 13. 권장 보완 순서 ✅

1. `MemberJoinDTO`에 `@NotBlank`, `@Email`, 길이 제한을 추가하고 `@Valid`로 검증한다.
2. 가입 DTO 전체 로그를 제거해 평문 비밀번호 노출을 막는다.
3. DB의 아이디·이메일에 유니크 제약을 둬 동시 가입 경쟁 상황도 막는다.
4. `MemberDTO#isEnabled()`를 `!del`과 연결한다.
5. 게시글 등록 POST와 수정 GET에도 서버 권한 검사를 추가한다.
6. 작성자 값은 폼 값이 아니라 서버의 인증 객체에서 얻는다.
7. `Custom403Handler`의 null 처리 및 HTML/JSON 응답 구분을 수정한다.
8. CSRF를 다시 활성화하고 모든 변경 폼에 CSRF 토큰을 적용한다.
9. Remember-Me 비밀키와 DB 접속 비밀번호를 외부 설정으로 분리한다.
10. 회원 수정·탈퇴 시 권한 검사, 비밀번호 재확인, Remember-Me 토큰 삭제를 함께 설계한다.

---

## 14. 최종 암기 지도

```text
회원가입
DTO 바인딩 → 중복 확인 → BCrypt → USER 부여 → JPA 저장

로그인
Security Filter → UserDetailsService → Member + Roles 조회
→ MemberDTO(UserDetails) → 비밀번호 비교 → Authentication 생성

권한
MemberRole.USER → SimpleGrantedAuthority("ROLE_USER")
→ hasRole("USER")로 검사

Remember-Me
체크박스 → 로그인 성공 → 쿠키 + DB 토큰 저장
→ 세션 소멸 후 쿠키 검증 → UserDetailsService 재조회 → 인증 복원

화면
#authentication.principal.username으로 현재 아이디 출력
th:if는 버튼 표시용, @PreAuthorize는 실제 보안용

권한 부족
인증 전: 로그인 유도
인증 후 권한 부족: AccessDeniedHandler가 403 처리
```

---

## 15. 이번 범위의 결론

이 프로젝트는 임시 메모리 계정에서 벗어나 실제 `Member` 테이블을 인증 원천으로 사용한다. 회원가입으로 만들어진 BCrypt 비밀번호와 역할을 `CustomUserDetailsService`가 읽고, `MemberDTO`가 이를 Spring Security 형식으로 변환한다. 인증 이후에는 세션 또는 Remember-Me가 로그인 상태를 유지하며, `@PreAuthorize`가 역할과 소유권을 검사한다.

가장 중요한 세 가지 원칙은 다음과 같다.

1. **비밀번호는 반드시 단방향 암호화하여 저장한다.**
2. **화면에서 버튼을 숨기는 것과 서버에서 권한을 검사하는 것은 별개다.**
3. **자동 로그인은 편리하지만 장기 쿠키이므로 키·토큰·로그아웃·탈퇴 처리를 함께 설계해야 한다.**
