package kr.or.oti.b01.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.or.oti.b01.domain.Board;
import kr.or.oti.b01.repository.search.BoardSearch;

public interface BoardRepository extends JpaRepository<Board, Long>, BoardSearch {
	@Query("select b from Board b where b.title like concat('%', :keyword, '%')")
    Page<Board> findKeyword(@Param("keyword") String keyword, Pageable pageable);

	@EntityGraph(attributePaths = {"imageSet"})
	@Query("select b from Board b where b.bno = :bno")
	Optional<Board> findByIdWithImages(@Param("bno") Long bno);
}
