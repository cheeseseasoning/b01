package kr.or.oti.b01.service;

import kr.or.oti.b01.dto.AdminMemberModifyDTO;
import kr.or.oti.b01.dto.MemberJoinDTO;
import kr.or.oti.b01.dto.MemberInfoDTO;
import kr.or.oti.b01.dto.MemberModifyDTO;
import kr.or.oti.b01.dto.PageRequestDTO;
import kr.or.oti.b01.dto.PageResponseDTO;

public interface MemberService {
	
	static class MidExistException extends Exception {
		
	}
	
	void join(MemberJoinDTO memberJoinDTO) throws MidExistException;

	PageResponseDTO<MemberInfoDTO> getList(PageRequestDTO pageRequestDTO);

	MemberInfoDTO get(String mid);

	// 로그인 실패 횟수를 증가시키고 잠금 여부 반환
	boolean loginFailed(String mid);

	// 로그인 성공 시 실패 횟수와 잠금 상태 초기화
	void loginSucceeded(String mid);

	// 회원을 탈퇴 상태로 변경
	void withdraw(String mid);

	// 일반 회원의 비밀번호와 이메일 수정
	void modify(String mid, MemberModifyDTO memberModifyDTO);

	// 관리자가 회원 상태와 정보를 수정
	void adminModify(AdminMemberModifyDTO adminMemberModifyDTO);
}
