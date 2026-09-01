package kr.or.oti.b01.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import kr.or.oti.b01.domain.Member;
import kr.or.oti.b01.repository.MemberRepository;
import kr.or.oti.b01.security.dto.MemberDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

	private final MemberRepository memberRepository; 	
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
		log.info("loadUserByUsername : " + username);
		
//		UserDetails user = User.builder()
//				.username(username)
//				.password("$2a$10$xJXaXxx5vFwB7ZJFT1ghKOe3wq1kQttbnu0yB5ppk0VkrWIhP96Gq")
//				.authorities("ROLE_USER")
//				.build();
		Member member = memberRepository.getWithRoles(username)
						.orElseThrow(() -> new UsernameNotFoundException(username + " 사용자가 존재하지 않습니다"));
		
		return entityToDTO(member);
	}

	// Member 엔티티를 로그인 사용자 정보로 변환
	private MemberDTO entityToDTO(Member member) {
		return new MemberDTO(
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
	}
}
