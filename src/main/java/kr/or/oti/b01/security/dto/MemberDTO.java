package kr.or.oti.b01.security.dto;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import kr.or.oti.b01.domain.MemberRole;
import lombok.Data;

@Data
public class MemberDTO implements UserDetails, OAuth2User {

    private String mid;
    private String mpw;
    private String email;
    private boolean del;
    
    private int failCount;
    private boolean accountLocked;
    private LocalDate expiredDate;
    private LocalDate credentialExpiredDate;
    private boolean enabled;
    
    private boolean social;
    private Map<String, Object> props;	//소셜 로그인 정보

    private Set<MemberRole> roleSet;
    
    public MemberDTO(
            String mid,
            String mpw,
            String email,
            boolean del,
            int failCount,
            boolean accountLocked,
            LocalDate expiredDate,
            LocalDate credentialExpiredDate,
            boolean enabled,
            Set<MemberRole> roleSet) {

        this.mid = mid;
        this.mpw = mpw;
        this.email = email;
        this.del = del;
        this.failCount = failCount;
        this.accountLocked = accountLocked;
        this.expiredDate = expiredDate;
        this.credentialExpiredDate = credentialExpiredDate;
        this.enabled = enabled;
        this.roleSet = roleSet;
    }

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		if (this.roleSet != null) {
			return this.roleSet.stream()
				.map(memberRole -> new SimpleGrantedAuthority("ROLE_" + memberRole.name()))
				.collect(Collectors.toList());
		}
		return null;
	}

	@Override
	public String getPassword() {
		return mpw;
	}

	@Override
	public String getUsername() {
		return mid;
	}

	// 계정 사용기간 만료 여부 확인
	@Override
	public boolean isAccountNonExpired() {
		return expiredDate == null
				|| !expiredDate.isBefore(LocalDate.now());
	}

	// 계정 잠금 여부 확인
	@Override
	public boolean isAccountNonLocked() {
		return !accountLocked;
	}

	// 비밀번호 사용기간 만료 여부 확인
	@Override
	public boolean isCredentialsNonExpired() {
		return credentialExpiredDate == null
				|| !credentialExpiredDate.isBefore(LocalDate.now());
	}

	// 활성화 상태와 탈퇴 여부 확인
	@Override
	public boolean isEnabled() {
		return enabled && !del;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return getProps();
	}

	@Override
	public String getName() {
		return this.mid;
	}

}
