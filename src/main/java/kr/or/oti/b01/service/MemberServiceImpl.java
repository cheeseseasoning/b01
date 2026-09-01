package kr.or.oti.b01.service;

import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.or.oti.b01.domain.Member;
import kr.or.oti.b01.domain.MemberRole;
import kr.or.oti.b01.dto.AdminMemberModifyDTO;
import kr.or.oti.b01.dto.MemberJoinDTO;
import kr.or.oti.b01.dto.MemberInfoDTO;
import kr.or.oti.b01.dto.MemberModifyDTO;
import kr.or.oti.b01.dto.PageRequestDTO;
import kr.or.oti.b01.dto.PageResponseDTO;
import kr.or.oti.b01.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService{

    private final ModelMapper modelMapper;

    private final MemberRepository memberRepository;

    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void join(MemberJoinDTO memberJoinDTO) throws MidExistException {
    	
    	String mid = memberJoinDTO.getMid();
    	boolean exist = memberRepository.existsById(mid);
    	
    	if (exist) {
    		throw new MidExistException();
    	}
    	
    	Member member = modelMapper.map(memberJoinDTO, Member.class);
    	member.changePassword(passwordEncoder.encode(memberJoinDTO.getMpw()));
    	member.addRole(MemberRole.USER);
    	
    	log.info("==============================");
    	log.info(member.toString());
    	log.info(member.getRoleSet().toString());
    	
    	memberRepository.save(member);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<MemberInfoDTO> getList(PageRequestDTO pageRequestDTO) {
        Pageable pageable = PageRequest.of(
                pageRequestDTO.getPage() - 1,
                pageRequestDTO.getSize(),
                Sort.by("regDate").descending());

        Page<Member> result = memberRepository.findAll(pageable);

        return new PageResponseDTO<>(
                pageRequestDTO,
                result.getContent().stream()
                        .map(this::entityToDTO)
                        .collect(java.util.stream.Collectors.toList()),
                (int) result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public MemberInfoDTO get(String mid) {
        Member member = memberRepository.getWithRoles(mid)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 회원입니다: " + mid));

        return entityToDTO(member);
    }

    // 로그인 실패 횟수를 증가시키고 잠금 여부 반환
    @Override
    @Transactional
    public boolean loginFailed(String mid) {
        Optional<Member> result = memberRepository.findById(mid);

        if (result.isEmpty()) {
            return false;
        }

        Member member = result.get();
        member.increaseFailCount();

        return member.isAccountLocked();
    }

    // 로그인 성공 시 실패 횟수와 잠금 상태 초기화
    @Override
    @Transactional
    public void loginSucceeded(String mid) {
        Optional<Member> result = memberRepository.findById(mid);

        if (result.isEmpty()) {
            return;
        }

        Member member = result.get();
        member.resetLoginFailure();
    }

    // 회원을 탈퇴 상태로 변경
    @Override
    @Transactional
    public void withdraw(String mid) {
        Optional<Member> result = memberRepository.findById(mid);

        if (result.isEmpty()) {
            return;
        }

        Member member = result.get();
        member.changeDel(true);
        member.changeEnabled(false);
    }

    // 일반 회원의 비밀번호와 이메일 수정
    @Override
    @Transactional
    public void modify(String mid, MemberModifyDTO memberModifyDTO) {
        Optional<Member> result = memberRepository.findById(mid);

        if (result.isEmpty()) {
            return;
        }

        Member member = result.get();
        member.changeEmail(memberModifyDTO.getEmail());

        // 새 비밀번호가 입력된 경우에만 변경
        String newPassword = memberModifyDTO.getMpw();

        if (newPassword != null && !newPassword.isBlank()) {
            member.changePassword(passwordEncoder.encode(newPassword));
        }
    }

    // 관리자가 회원 상태와 정보를 수정
    @Override
    @Transactional
    public void adminModify(AdminMemberModifyDTO adminMemberModifyDTO) {
        Optional<Member> result =
                memberRepository.findById(adminMemberModifyDTO.getMid());

        if (result.isEmpty()) {
            return;
        }

        Member member = result.get();
        member.changeEmail(adminMemberModifyDTO.getEmail());

        // 새 비밀번호가 입력된 경우에만 변경
        String newPassword = adminMemberModifyDTO.getMpw();

        if (newPassword != null && !newPassword.isBlank()) {
            member.changePassword(passwordEncoder.encode(newPassword));
        }

        // 관리자가 요청한 경우 실패 횟수와 잠금 초기화
        if (adminMemberModifyDTO.isResetLoginFailure()) {
            member.resetLoginFailure();
        }

        member.changeDel(adminMemberModifyDTO.isDel());
        member.changeEnabled(adminMemberModifyDTO.isEnabled());
        member.changeExpiredDate(adminMemberModifyDTO.getExpiredDate());
        member.changeCredentialExpiredDate(
                adminMemberModifyDTO.getCredentialExpiredDate());
    }

    private MemberInfoDTO entityToDTO(Member member) {
        return MemberInfoDTO.builder()
                .mid(member.getMid())
                .email(member.getEmail())
                .del(member.isDel())
                .failCount(member.getFailCount())
                .accountLocked(member.isAccountLocked())
                .expiredDate(member.getExpiredDate())
                .credentialExpiredDate(member.getCredentialExpiredDate())
                .enabled(member.isEnabled())
                .roleSet(member.getRoleSet())
                .regDate(member.getRegDate())
                .modDate(member.getModDate())
                .build();
    }
	
}
