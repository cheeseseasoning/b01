package kr.or.oti.b01.service;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import kr.or.oti.b01.domain.Board;
import kr.or.oti.b01.domain.Reply;
import kr.or.oti.b01.dto.PageRequestDTO;
import kr.or.oti.b01.dto.PageResponseDTO;
import kr.or.oti.b01.dto.ReplyDTO;
import kr.or.oti.b01.repository.ReplyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReplyServiceImpl implements ReplyService {
	private final ReplyRepository repository;
	private final ModelMapper mapper;
	
	public ReplyDTO register(ReplyDTO dto) {
		//ReplyDTO.bno -> Reply.board.bno 이것으로 전달이 되여야 함 
		Reply reply = Reply.builder()
			    .replyText(dto.getReplyText())
			    .replyer(dto.getReplyer())
			    .board(Board.builder().bno(dto.getBno()).build())
			    .build();

		reply = repository.save(reply);
		
		System.out.println("DEBUG..." + dto);
		
		return mapper.map(reply, ReplyDTO.class);
	}
	
	public PageResponseDTO<ReplyDTO> getList(long bno, PageRequestDTO pageRequestDTO) {
		Pageable pageable = PageRequest.of(pageRequestDTO.getPage()-1, pageRequestDTO.getSize(), Sort.by("rno").descending());
		Page<Reply> page = repository.listOfBoard(bno, pageable);
		
		List<ReplyDTO> dtoList = page.getContent().stream()
				.map(item -> mapper.map(item, ReplyDTO.class))
				.collect(Collectors.toList());
		
		int total = (int) page.getTotalElements();
		
		log.info(dtoList.toString());
		log.info("total = " + total);
		
		return new PageResponseDTO<>(pageRequestDTO, dtoList, total);
	}
	
	public ReplyDTO get(long rno) {
	    // Optional 처리: 데이터가 없을 경우 NoSuchElementException 발생
		Reply item = repository.findById(rno)
	            .orElseThrow(() -> new IllegalArgumentException("해당 댓글이 존재하지 않습니다. rno=" + rno));
	    
	    return mapper.map(item, ReplyDTO.class);
	}

	// 기존 register(), getList(), get() 등은 그대로 두고 아래 두 메서드만 교체

	@Override
	public void modify(ReplyDTO dto) {
	    // 1. 원본자료를 repository에서 얻는다
	    Reply reply = repository.findById(dto.getRno())
	            .orElseThrow(() -> new IllegalArgumentException("해당 댓글이 존재하지 않습니다. rno=" + dto.getRno()));

	    // 2. 얻은 자료를 사용자가 입력한 값으로 수정한다
	    reply.changeText(dto.getReplyText());

	    // 3. repository에 다시 저장한다 (영속 상태라 사실 save 생략해도 트랜잭션 커밋시 반영되지만, 명시적으로 호출)
	    repository.save(reply);
	}

	@Override
	public void remove(long rno) {
	    repository.deleteById(rno);
	}
}
