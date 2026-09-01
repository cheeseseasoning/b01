package kr.or.oti.b01.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class AdminMemberModifyDTO {

    private String mid;
    private String mpw;
    private String email;

    private boolean resetLoginFailure;
    private boolean del;
    private boolean enabled;

    private LocalDate expiredDate;
    private LocalDate credentialExpiredDate;
}
