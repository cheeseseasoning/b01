package kr.or.oti.b01;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import kr.or.oti.b01.domain.Board;
import kr.or.oti.b01.domain.Reply;
import kr.or.oti.b01.repository.ReplyRepository;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
public class ReplyRepositoryTests {
	@Autowired
	private ReplyRepository repository;

	@Test
	public void testInsert() {
		Board board = Board.builder().bno(203L).build();
		
		IntStream.range(1, 100).forEach(i -> {
			Reply reply = Reply.builder()
							.board(board)
							.replyText("댓글 ..." + i)
							.replyer("writer...099")
							.build();
			
			Reply result = repository.save(reply);
			log.info("RNO: " + result.getRno());
		});
	}

//	@Test
//	public void testSelect() {
//		Long bno = 100L;
//
//		Optional<Board> result = boardRepository.findById(bno);
//
//		Board board = result.orElseThrow();
//
//		log.info("board" + board);
//
//	}
//
//	@Test
//	public void testUpdate() {
//
//		Long bno = 100L;
//
//		Optional<Board> result = boardRepository.findById(bno);
//
//		Board board = result.orElseThrow();
//
//		board.change("update..title 100", "update content 100");
//
//		boardRepository.save(board);
//
//	}
//
//	@Test
//	public void testDelete() {
//		Long bno = 2L;
//
//		boardRepository.deleteById(bno);
//	}
//
//	@Test
//	public void testPaging() {
//		Pageable pageable = PageRequest.of(0, 10, Sort.by("bno").descending());
//
//		Page<Board> result = boardRepository.findAll(pageable);
//
//		log.info("total count: " + result.getTotalElements());
//		log.info("total pages: " + result.getTotalPages());
//		log.info("page number: " + result.getNumber());
//		log.info("page size: " + result.getSize());
//
//		List<Board> todoList = result.getContent();
//
//		todoList.forEach(board -> log.info("board: " + board));
//	}
//
//	@Test
//	public void testPaging2() {
//		Pageable pageable = PageRequest.of(0, 10, Sort.by("bno").descending());
//		Page<Board> result = boardRepository.findKeyword("9", pageable);
//
//		log.info("total count: " + result.getTotalElements());
//		log.info("total pages:" + result.getTotalPages());
//		log.info("page number: " + result.getNumber());
//		log.info("page size: " + result.getSize());
//
//		List<Board> todoList = result.getContent();
//
//		todoList.forEach(board -> log.info(board.toString()));
//	}
//	@Test
//	public void testPaging3() {
//		Pageable pageable = PageRequest.of(0, 10, Sort.by("bno").descending());
//		Page<Board> result = boardRepository.findKeyword("9", pageable);
//
//		log.info("total count: " + result.getTotalElements());
//		log.info("total pages:" + result.getTotalPages());
//		log.info("page number: " + result.getNumber());
//		log.info("page size: " + result.getSize());
//
//		List<Board> todoList = result.getContent();
//
//		todoList.forEach(board -> log.info(board.toString()));
//	}
	@Test
	public void testBoardReplies() {
		Pageable pageable = PageRequest.of(0, 10, Sort.by("rno").descending());
		Page<Reply> result = repository.listOfBoard(203L, pageable);

		log.info("total count: " + result.getTotalElements());
		log.info("total pages:" + result.getTotalPages());
		log.info("page number: " + result.getNumber());
		log.info("page size: " + result.getSize());

//		List<Reply> todoList = result.getContent();
//
//		todoList.forEach(reply -> log.info(reply.toString()));
		result.getContent().forEach(reply -> log.info(reply.toString()));
	}
}
