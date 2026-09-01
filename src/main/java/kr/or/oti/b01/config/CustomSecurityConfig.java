package kr.or.oti.b01.config;

import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import kr.or.oti.b01.domain.Member;
import kr.or.oti.b01.repository.MemberRepository;
import kr.or.oti.b01.security.handler.Custom403Handler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class CustomSecurityConfig {
	
	private final DataSource dataSource;
	private final UserDetailsService customUserDetailsService;
	private final MemberRepository memberRepository;
	
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		
		log.info("filterChain()... -------------------------------");
		
//		http.csrf().disable();
		CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();

        csrfRepository.setCookieName("XSRF-TOKEN");
        csrfRepository.setHeaderName("X-XSRF-TOKEN");

        http.csrf(csrf -> csrf.csrfTokenRepository(csrfRepository));
        
        http.formLogin()
        .loginPage("/member/login")
        .failureHandler((request, response, exception) -> {
            String mid = request.getParameter("username");
            String error = "login";

            // 비밀번호 오류일 때 실패 횟수 증가
            if (exception instanceof BadCredentialsException) {
                Optional<Member> result = memberRepository.findById(mid);

                if (result.isPresent()) {
                    Member member = result.get();
                    member.increaseFailCount();
                    memberRepository.save(member);

                    if (member.isAccountLocked()) {
                        error = "locked";
                    } else {
                        error = "badCredentials";
                    }
                } else {
                    error = "badCredentials";
                }
            } else if (exception instanceof LockedException) {
                error = "locked";
            } else if (exception instanceof AccountExpiredException) {
                error = "accountExpired";
            } else if (exception instanceof CredentialsExpiredException) {
                error = "credentialsExpired";
            } else if (exception instanceof DisabledException) {
                error = "disabled";
            }

            response.sendRedirect("/member/login?error=" + error);
        })
        .successHandler((request, response, authentication) -> {
            String mid = authentication.getName();
            Optional<Member> result = memberRepository.findById(mid);

            // 로그인 성공 시 실패 횟수 초기화
            if (result.isPresent()) {
                Member member = result.get();
                member.resetLoginFailure();
                memberRepository.save(member);
            }

            response.sendRedirect("/");
        });
		
		http.rememberMe()
			.key("*^kosa1004!$")
			.tokenRepository(persistentTokenRepository())
			.userDetailsService(customUserDetailsService)
			.tokenValiditySeconds(60 * 60 * 24 * 30);
		
		
		http.exceptionHandling().accessDeniedHandler(accessDeniedHandler());
		
		http.oauth2Login().loginPage("/member/login");
		
		return http.build();
	}
	
	@Bean 
	public AccessDeniedHandler accessDeniedHandler() {
		return new Custom403Handler();
	}
	
	
	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() throws Exception {
		log.info("webSecurityCustomizer()... -------------------------------");
		
		return (web) -> web.ignoring().requestMatchers(PathRequest.toStaticResources().atCommonLocations());
	}
	
	@Bean
	public PersistentTokenRepository persistentTokenRepository() {
		JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
		repo.setDataSource(dataSource);
		return repo;
	}
}

