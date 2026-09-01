package kr.or.oti.b01;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import kr.or.oti.b01.domain.Board;
import kr.or.oti.b01.dto.BoardListAllDTO;
import kr.or.oti.b01.dto.BoardListReplyCountDTO;
import kr.or.oti.b01.repository.BoardRepository;
import kr.or.oti.b01.repository.ReplyRepository;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
public class BoardRepositoryTests {
	@Autowired
	private BoardRepository boardRepository;
	
	@Autowired
	private ReplyRepository replyRepository;

	@Test
	public void testInsert() {
		IntStream.range(1, 100).forEach(i -> {
			Board board = Board.builder().title("title..." + i).content("content..." + i).writer("user" + (i % 10))
					.build();

			Board result = boardRepository.save(board);
			log.info("BNO: " + result.getBno());
		});
	}

	@Test
	public void testSelect() {
		Long bno = 100L;

		Optional<Board> result = boardRepository.findById(bno);

		Board board = result.orElseThrow();

		log.info("board" + board);

	}

	@Test
	public void testUpdate() {

		Long bno = 100L;

		Optional<Board> result = boardRepository.findById(bno);

		Board board = result.orElseThrow();

		board.change("update..title 100", "update content 100");

		boardRepository.save(board);

	}

	@Test
	public void testDelete() {
		Long bno = 2L;

		boardRepository.deleteById(bno);
	}

	@Test
	public void testPaging() {
		Pageable pageable = PageRequest.of(0, 10, Sort.by("bno").descending());

		Page<Board> result = boardRepository.findAll(pageable);

		log.info("total count: " + result.getTotalElements());
		log.info("total pages: " + result.getTotalPages());
		log.info("page number: " + result.getNumber());
		log.info("page size: " + result.getSize());

		List<Board> todoList = result.getContent();

		todoList.forEach(board -> log.info("board: " + board));
	}

	@Test
	public void testPaging2() {
		Pageable pageable = PageRequest.of(0, 10, Sort.by("bno").descending());
		Page<Board> result = boardRepository.findKeyword("9", pageable);

		log.info("total count: " + result.getTotalElements());
		log.info("total pages:" + result.getTotalPages());
		log.info("page number: " + result.getNumber());
		log.info("page size: " + result.getSize());

		List<Board> todoList = result.getContent();

		todoList.forEach(board -> log.info(board.toString()));
	}
	@Test
	public void testPaging3() {
		Pageable pageable = PageRequest.of(0, 10, Sort.by("bno").descending());
		Page<Board> result = boardRepository.findKeyword("9", pageable);

		log.info("total count: " + result.getTotalElements());
		log.info("total pages:" + result.getTotalPages());
		log.info("page number: " + result.getNumber());
		log.info("page size: " + result.getSize());

		List<Board> todoList = result.getContent();

		todoList.forEach(board -> log.info(board.toString()));
	}
	@Test
	public void testPaging4() {
		Pageable pageable = PageRequest.of(0, 10, Sort.by("bno").descending());
		Page<Board> result = boardRepository.searchAll(new String[] {"w"}, "9", pageable);

		log.info("total count: " + result.getTotalElements());
		log.info("total pages:" + result.getTotalPages());
		log.info("page number: " + result.getNumber());
		log.info("page size: " + result.getSize());

		List<Board> todoList = result.getContent();

		todoList.forEach(board -> log.info(board.toString()));
	}
	
	@Test
	public void testSearchReplyCountPaging() {
		Pageable pageable = PageRequest.of(0, 10, Sort.by("bno").descending());
		Page<BoardListReplyCountDTO> result = boardRepository.searchWithReplyCount(new String[] {"w"}, "9", pageable);

		log.info("total count: " + result.getTotalElements());
		log.info("total pages:" + result.getTotalPages());
		log.info("page number: " + result.getNumber());
		log.info("page size: " + result.getSize());

		List<BoardListReplyCountDTO> todoList = result.getContent();

		todoList.forEach(board -> log.info(board.toString()));
	}
	
	@Test
	public void 이미지_첨부파일_테스트() {
		Board board = Board.builder()
				.title("Image Test")
				.content("첨부파일 테스트")
				.writer("홍길동")
				.build();
		
		for (int i=0;i<3;i++) {
			board.addImage(UUID.randomUUID().toString(), "고양이" + i + ".jpg");
		}
		
		boardRepository.save(board);
	}
	
	@Test
	public void 게시물_첨부파일_읽기_테스트() {
		Long bno = 205L;

		Optional<Board> result = boardRepository.findByIdWithImages(bno);

		Board board = result.orElseThrow();

		log.info("board" + board);
		log.info("imageSet" + board.getImageSet());

	}
	
	@Transactional
	@Commit
	@Test
	public void 게시물_수정_첨부파일_삭제_후_첨부파일_추가_테스트() {

		Long bno = 205L;

		Optional<Board> result = boardRepository.findByIdWithImages(bno);

		Board board = result.orElseThrow();

		board.change("update..title 100", "update content 100");
		
		//기존 이미지 삭제 
		board.clearImages();
		
		//새로운 첨부파일들 
		for (int i=0;i<2;i++) {
			board.addImage(UUID.randomUUID().toString(), "수정된_고양이" + i + ".jpg");
		}

		boardRepository.save(board);

	}

	@Test
	@Transactional
	@Commit
	public void 게시물과_댓글_첨부파일_삭제_테스트() {

		replyRepository.deleteByBoard_Bno(205L);		
		boardRepository.deleteById(205L);
	}

	@Test
	public void  게시물등록시_아이디가_5의_배수가_아니면_첨부파일_추가함_테스트() {
		IntStream.range(1, 100).forEach(i -> {
			Board board = Board.builder().title("title..." + i).content("content..." + i).writer("user" + (i % 10))
					.build();

			for (int j=0;j<3;j++) {
				if (i % 5 == 0) continue;
				board.addImage(UUID.randomUUID().toString(), i + "_고양이_" + j + ".jpg");
			}
			Board result = boardRepository.save(board);
			log.info("BNO: " + result.getBno());
		});
	}
	
	/*
	 * 게시물 번호 (1,2,3,4,5,6,7,8,9,10)
	 * select * from board b where bno between 1 and 10
	 * 
	 * 첨부파일 
	 *    select * from board_image where bno = 1
	 *    select * from board_image where bno = 2
	 *    select * from board_image where bno = 3
	 *    select * from board_image where bno = 4
	 *    select * from board_image where bno = 5
	 *    select * from board_image where bno = 6
	 *    select * from board_image where bno = 7
	 *    select * from board_image where bno = 8
	 *    select * from board_image where bno = 9
	 *    select * from board_image where bno = 10
	 * 
	 *    
	 * select * from board inner left join board_image on (board.bno = board_image.bno)    
	 *    
	 */

	@Test
	@Transactional
	public void 게시물_첨부파일_읽기_테스트2() {
		Pageable pageable = PageRequest.of(0, 10, Sort.by("bno").descending());

		Page<Board> result = boardRepository.findAll(pageable);

		log.info("total count: " + result.getTotalElements());
		log.info("total pages: " + result.getTotalPages());
		log.info("page number: " + result.getNumber());
		log.info("page size: " + result.getSize());

		List<Board> todoList = result.getContent();

		todoList.forEach(board -> {
			log.info("board: " + board);
			log.info("imageSet" + board.getImageSet());
		});
	}
	
	@Test
	@Transactional
	public void 게시물_첨부파일_읽기_테스트3() {
		Pageable pageable = PageRequest.of(0, 10, Sort.by("bno").descending());

		Page<BoardListAllDTO> result = boardRepository.searchWithAll(null, null, pageable);

		log.info("total count: " + result.getTotalElements());
		log.info("total pages: " + result.getTotalPages());
		log.info("page number: " + result.getNumber());
		log.info("page size: " + result.getSize());

		List<BoardListAllDTO> list = result.getContent();

		list.forEach(boardListAllDTO -> {
			log.info("board: " + boardListAllDTO);
			log.info("imageSet" + boardListAllDTO.getBoardImages());
		});
	}

}
