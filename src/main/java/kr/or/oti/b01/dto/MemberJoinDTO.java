package kr.or.oti.b01.dto;

import lombok.Data;

@Data
public class MemberJoinDTO {

    private String mid;
    private String mpw;
    private String email;
    private boolean del;
}
