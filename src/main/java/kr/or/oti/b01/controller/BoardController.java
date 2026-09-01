package kr.or.oti.b01.controller;

import java.security.Principal;
import java.util.List;

import javax.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.or.oti.b01.dto.BoardDTO;
import kr.or.oti.b01.dto.PageRequestDTO;
import kr.or.oti.b01.service.BoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/board")
@RequiredArgsConstructor
@Slf4j
public class BoardController {

    private final BoardService boardService;

    // 목록 조회
    @RequestMapping("/list")
    public void list(PageRequestDTO pageRequestDTO, Model model) {
        log.info("board list: {}", pageRequestDTO);
        
        // 일반 listWithReplyCount 대신 이미지까지 모두 가져오는 listWithAll 사용
        model.addAttribute("pageResponseDTO", boardService.listWithAll(pageRequestDTO));
    }

    // 등록 화면
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/register")
    public void register() {
    }

    // 등록 처리
    @PostMapping("/register")
    public String register(@Valid BoardDTO dto,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {

        log.info("board register post: {}", dto);

        if (bindingResult.hasErrors()) {
            log.info("errors: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("errors", bindingResult.getAllErrors());
            return "redirect:/board/register";
        }

        boardService.register(dto);
        redirectAttributes.addFlashAttribute("result", "registered");

        return "redirect:/board/list";
    }

    // 상세 조회 (tid -> bno로 수정)
    @PreAuthorize("isAuthenticated()")
    @RequestMapping("/read")
    public void read(long bno, PageRequestDTO pageRequestDTO, Model model) {
        BoardDTO boardDTO = boardService.get(bno);
        log.info("board read/modify: {}", boardDTO);
        model.addAttribute("dto", boardDTO);
    }
    
    // 수정 화면 
    @RequestMapping("/modify")
    public String modify(long bno, PageRequestDTO pageRequestDTO, Model model, RedirectAttributes redirectAttributes, Principal principal) {
        BoardDTO boardDTO = boardService.get(bno);
        
        //로그인한 사용자와 게시물 작성자가 같은 경우 
        if (principal.getName().equals(boardDTO.getWriter())) {
	        log.info("board read/modify: {}", boardDTO);
	        model.addAttribute("dto", boardDTO);
	        return "board/modify";
        } else {
        	//로그인한 사용자와 게시물 작성자가 다른 경우
        	redirectAttributes.addAttribute("error", "로그인한 사용자와 게시물 작성자가 달라 수정할 수 없습니다");
	        return "redirect:/board/read?bno=" + bno + "&" + pageRequestDTO.getLink();
        }
    }

    // 수정 처리
    @PreAuthorize("principal.username == #dto.writer")
    @PostMapping("/modify")
    public String modify(PageRequestDTO pageRequestDTO,
                         @Valid BoardDTO dto,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {

        log.info("board modify post: {}", dto);

        if (bindingResult.hasErrors()) {
            log.info("errors: {}", bindingResult.getAllErrors());
            String link = pageRequestDTO.getLink();
            redirectAttributes.addFlashAttribute("errors", bindingResult.getAllErrors());
            redirectAttributes.addAttribute("bno", dto.getBno());
            return "redirect:/board/modify?" + link;
        }

        boardService.modify(dto);
        redirectAttributes.addFlashAttribute("result", "modified");
        redirectAttributes.addAttribute("bno", dto.getBno());

        return "redirect:/board/read";
    }

    // 단건 삭제 (tid -> bno로 수정)
    @PreAuthorize("principal.username == #dto.writer")
    @PostMapping("/remove")
    public String remove(BoardDTO dto, PageRequestDTO pageRequestDTO, RedirectAttributes redirectAttributes) {
        log.info("board remove post: {}", dto.getBno());
        boardService.remove(dto.getBno());
        redirectAttributes.addFlashAttribute("result", "removed");

        return "redirect:/board/list?" + pageRequestDTO.getLink();
    }
    
    // 선택 삭제 처리 (POST /board/removeBatch)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/removeBatch")
    public String removeBatch(@RequestParam(value = "bnos", required = false) List<Long> bnos,
                              PageRequestDTO pageRequestDTO,
                              RedirectAttributes redirectAttributes) {

        log.info("removeBatch post: bnos={}, pageRequestDTO={}", bnos, pageRequestDTO);

        if (bnos != null && !bnos.isEmpty()) {
            boardService.removeBatch(bnos);
            redirectAttributes.addFlashAttribute("result", "removedBatch");
        }

        redirectAttributes.addFlashAttribute("result", "선택된 항목들을 삭제했습니다");
        // 삭제 후 기존 페이지 번호/검색 조건 유지하여 목록으로 이동
        return "redirect:/board/list?" + pageRequestDTO.getLink();
    }
}