package kr.or.oti.b01.repository.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import kr.or.oti.b01.domain.Board;
import kr.or.oti.b01.dto.BoardListAllDTO;
import kr.or.oti.b01.dto.BoardListReplyCountDTO;

public interface BoardSearch {
	Page<Board> search1(Pageable pageable);

    Page<Board> searchAll(String[] types, String keyword, Pageable pageable);
    
    Page<BoardListReplyCountDTO> searchWithReplyCount(String[] types, String keyword, Pageable pageable);
    Page<BoardListAllDTO> searchWithAll(String[] types, String keyword, Pageable pageable);
}
