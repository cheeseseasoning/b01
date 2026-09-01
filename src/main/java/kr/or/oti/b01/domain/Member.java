package kr.or.oti.b01.domain;

import lombok.*;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "roleSet")
public class Member extends BaseEntity{

    @Id
    private String mid;

    private String mpw;
    private String email;
    private boolean del;
    
    @Builder.Default
    private int failCount = 0;

    @Builder.Default
    private boolean accountLocked = false; //로그인 실패 횟수 5회가 되면 accountLocked = true, 성공하면 false

    private LocalDate expiredDate;
    private LocalDate credentialExpiredDate;

    @Builder.Default
    private boolean enabled = true;
    
    private boolean social;

    @ElementCollection(fetch = FetchType.LAZY)
    @Builder.Default
    private Set<MemberRole> roleSet = new HashSet<>();

    public void changePassword(String mpw){
        this.mpw = mpw;
    }

    public void changeEmail(String email){
        this.email = email;
    }

    public void changeDel(boolean del){
        this.del = del;
    }

    public void addRole(MemberRole memberRole){
        this.roleSet.add(memberRole);
    }

    public void clearRoles() {
        this.roleSet.clear();
    }

    public void increaseFailCount() {
        this.failCount++;

        if (this.failCount >= 5) {
            this.accountLocked = true;
        }
    }

    public void resetLoginFailure() {
        this.failCount = 0;
        this.accountLocked = false;
    }

    public void changeAccountLocked(boolean accountLocked) {
        this.accountLocked = accountLocked;

        if (!accountLocked) {
            this.failCount = 0;
        }
    }

    public void changeExpiredDate(LocalDate expiredDate) {
        this.expiredDate = expiredDate;
    }

    public void changeCredentialExpiredDate(LocalDate credentialExpiredDate) {
        this.credentialExpiredDate = credentialExpiredDate;
    }

    public void changeEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}

