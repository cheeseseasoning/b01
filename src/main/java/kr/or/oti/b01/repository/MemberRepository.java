package kr.or.oti.b01.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.or.oti.b01.domain.Member;

public interface MemberRepository extends JpaRepository<Member, String> {

    @EntityGraph(attributePaths = "roleSet")
    @Query("select m from Member m where m.mid = :mid")
    Optional<Member> getWithRoles(@Param("mid") String mid);

    @EntityGraph(attributePaths = "roleSet")
    Optional<Member> findByEmail(String email);    
}