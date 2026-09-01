package kr.or.oti.b01.service;

import java.util.List;
import java.util.stream.Collectors;

import kr.or.oti.b01.domain.Board;
import kr.or.oti.b01.dto.BoardDTO;
import kr.or.oti.b01.dto.BoardListAllDTO;
import kr.or.oti.b01.dto.BoardListReplyCountDTO;
import kr.or.oti.b01.dto.PageRequestDTO;
import kr.or.oti.b01.dto.PageResponseDTO;

public interface BoardService {
	void register(BoardDTO boardDTO);
	PageResponseDTO<BoardDTO> getList(PageRequestDTO pageRequestDTO);
	BoardDTO get(long tid);
	void remove(long tid);
	void modify(BoardDTO boardDTO);
	void removeBatch(List<Long> bnos);
	
	PageResponseDTO<BoardListReplyCountDTO> listWithReplyCount(PageRequestDTO pageRequestDTO);
	PageResponseDTO<BoardListAllDTO> listWithAll(PageRequestDTO pageRequestDTO);
	
	default Board dtoToEntity(BoardDTO boardDTO) {
		Board board = Board.builder()
				.bno(boardDTO.getBno())
				.title(boardDTO.getTitle())
				.content(boardDTO.getContent())
				.writer(boardDTO.getWriter())
				.build();
		
		if (boardDTO.getFileNames() != null) {
			boardDTO.getFileNames().forEach(fileName -> {
				String[] arr = fileName.split("_");
				board.addImage(arr[0], arr[1]);
			});
		}
		return board;
	}
	
	default BoardDTO entityToDto(Board board) {
		BoardDTO boardDTO = BoardDTO.builder()
				.bno(board.getBno())
				.title(board.getTitle())
				.content(board.getContent())
				.writer(board.getWriter())
				.regDate(board.getRegDate())
				.modDate(board.getModDate())
				.build();

		boardDTO.setFileNames(board.getImageSet().stream().sorted().map(boardImage -> 
			boardImage.getUuid() + "_" + boardImage.getFilename())
			.collect(Collectors.toList())
		);
		
		return boardDTO;
	}
	
}
