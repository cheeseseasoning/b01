package kr.or.oti.b01.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.or.oti.b01.dto.AdminMemberModifyDTO;
import kr.or.oti.b01.dto.MemberJoinDTO;
import kr.or.oti.b01.dto.MemberInfoDTO;
import kr.or.oti.b01.dto.MemberModifyDTO;
import kr.or.oti.b01.dto.PageRequestDTO;
import kr.or.oti.b01.dto.PageResponseDTO;
import kr.or.oti.b01.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

	private final MemberService memberService;
	
	@GetMapping("login")
	public void login() {
		log.info("login()....");
	}

	// 회원가입 화면
	@GetMapping("join")
	public void joinGET() {
		log.info("joinGET()....");
	}

	// 회원가입 처리
	@PostMapping("join")
	public String joinPOST(
			MemberJoinDTO memberJoinDTO,
			RedirectAttributes redirectAttributes) {

		try {
			memberService.join(memberJoinDTO);
		} catch (MemberService.MidExistException e) {
			redirectAttributes.addFlashAttribute("error", "mid");

			return "redirect:/member/join";
		}

		return "redirect:/member/login?join=success";
	}

	// 관리자용 회원 목록
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("list")
	public void list(
			PageRequestDTO pageRequestDTO,
			Model model) {

		PageResponseDTO<MemberInfoDTO> result =
				memberService.getList(pageRequestDTO);

		model.addAttribute("pageResponseDTO", result);
	}

	// 관리자용 회원 상세보기
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("read")
	public void read(
			String mid,
			PageRequestDTO pageRequestDTO,
			Model model) {

		MemberInfoDTO memberInfoDTO = memberService.get(mid);

		model.addAttribute("dto", memberInfoDTO);
		model.addAttribute("pageRequestDTO", pageRequestDTO);
	}

	// 현재 로그인한 회원 탈퇴
	@PreAuthorize("isAuthenticated()")
	@PostMapping("remove")
	public String remove(
			Authentication authentication,
			HttpServletRequest request,
			HttpServletResponse response) {

		String mid = authentication.getName();
		memberService.withdraw(mid);

		// 자동 로그인 쿠키 삭제
		new CookieClearingLogoutHandler("remember-me")
				.logout(request, response, authentication);

		// 로그인 세션 삭제
		new SecurityContextLogoutHandler()
				.logout(request, response, authentication);

		return "redirect:/member/login?withdraw=success";
	}

	// 현재 로그인한 회원의 수정 화면
	@PreAuthorize("isAuthenticated()")
	@GetMapping("modify")
	public void modifyGET(
			Authentication authentication,
			Model model) {

		String mid = authentication.getName();
		MemberInfoDTO memberInfoDTO = memberService.get(mid);

		model.addAttribute("dto", memberInfoDTO);
	}

	// 현재 로그인한 회원 정보 수정
	@PreAuthorize("isAuthenticated()")
	@PostMapping("modify")
	public String modifyPOST(
			MemberModifyDTO memberModifyDTO,
			Authentication authentication) {

		String mid = authentication.getName();
		memberService.modify(mid, memberModifyDTO);

		return "redirect:/member/modify?success=true";
	}

	// 관리자용 회원 수정 화면
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("admin/modify")
	public String adminModifyGET(
			String mid,
			Model model) {

		MemberInfoDTO memberInfoDTO = memberService.get(mid);
		model.addAttribute("dto", memberInfoDTO);

		return "member/admin-modify";
	}

	// 관리자용 회원 수정 처리
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("admin/modify")
	public String adminModifyPOST(
			AdminMemberModifyDTO adminMemberModifyDTO,
			RedirectAttributes redirectAttributes) {

		memberService.adminModify(adminMemberModifyDTO);

		redirectAttributes.addAttribute(
				"mid", adminMemberModifyDTO.getMid());
		redirectAttributes.addAttribute("result", "modified");

		return "redirect:/member/read";
	}
}
