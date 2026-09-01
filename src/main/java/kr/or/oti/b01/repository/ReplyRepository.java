package kr.or.oti.b01.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.or.oti.b01.domain.Board;
import kr.or.oti.b01.domain.Reply;
import kr.or.oti.b01.repository.search.BoardSearch;

public interface ReplyRepository extends JpaRepository<Reply, Long>{

	@Query("select r from Reply r where r.board.bno = :bno")
	Page<Reply> listOfBoard(@Param("bno") Long bno, Pageable pageable);

	//delete from reply where bno=:bno
	void deleteByBoard_Bno(@Param("bno")Long bno);
}
