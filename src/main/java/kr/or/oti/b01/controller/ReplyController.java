package kr.or.oti.b01.controller;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.validation.BindException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import io.swagger.annotations.ApiOperation;
import kr.or.oti.b01.dto.PageRequestDTO;
import kr.or.oti.b01.dto.PageResponseDTO;
import kr.or.oti.b01.dto.ReplyDTO;
import kr.or.oti.b01.service.ReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/replies")
@RequiredArgsConstructor
@Slf4j
public class ReplyController {

	private final ReplyService service;

	@ApiOperation(value = "Replies POST", notes = "POST 방식으로 댓글 등록")
	@PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Long>> register(@Valid @RequestBody ReplyDTO replyDTO,
			BindingResult bindingResult) throws BindException {
		log.info("replyDTO : " + replyDTO);
		if (bindingResult.hasErrors()) {
			throw new BindException(bindingResult);
		}
		replyDTO = service.register(replyDTO);
		return ResponseEntity.ok(Map.of("rno", replyDTO.getRno()));
	}

	@ApiOperation(value = "Replies of Board", notes = "get 방식으로 특정 게시물 댓글 목록 얻기")
	@GetMapping("/list/{bno}")
	public PageResponseDTO<ReplyDTO> list(@PathVariable("bno") Long bno, PageRequestDTO pageRequestDTO) {
		log.info("board list: {}", pageRequestDTO);
		return service.getList(bno, pageRequestDTO);
	}

	@ApiOperation(value = "Read reply", notes = "get 방식으로 특정 댓글 얻기")
	@GetMapping("/{rno}")
	public ReplyDTO read(@PathVariable("rno") long rno) {
		ReplyDTO dto = service.get(rno);
		log.info("reply read: {}", dto);
		return dto;
	}

	// ============ 신규 추가: 댓글 수정 ============
	@ApiOperation(value = "Modify reply", notes = "put 방식으로 댓글 수정")
	@PutMapping(value = "/{rno}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Long> modify(@PathVariable("rno") Long rno, @RequestBody ReplyDTO replyDTO) {
		replyDTO.setRno(rno); // ReplyDTO는 @Data라 setRno() 자동 생성됨
		log.info("modify reply: {}", replyDTO);
		service.modify(replyDTO);
		return Map.of("rno", rno);
	}

	// ============ 신규 추가: 댓글 삭제 ============
	@ApiOperation(value = "Remove reply", notes = "delete 방식으로 댓글 삭제")
	@DeleteMapping("/{rno}")
	public Map<String, Long> remove(@PathVariable("rno") long rno) {
		log.info("remove reply: rno={}", rno);
		service.remove(rno);
		return Map.of("rno", rno);
	}
}