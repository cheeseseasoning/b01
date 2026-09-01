package kr.or.oti.b01.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import kr.or.oti.b01.domain.MemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberInfoDTO {

    private String mid;
    private String email;
    private boolean del;
    private int failCount;
    private boolean accountLocked;
    private LocalDate expiredDate;
    private LocalDate credentialExpiredDate;
    private boolean enabled;
    private Set<MemberRole> roleSet;
    private LocalDateTime regDate;
    private LocalDateTime modDate;
}
