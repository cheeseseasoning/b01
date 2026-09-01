package kr.or.oti.b01.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import kr.or.oti.b01.dto.BoardImageDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BoardImage implements Comparable<BoardImage> {
	@Id
	private String uuid;
	
	private String filename;
	
	private int ord;
	
	@ManyToOne
	private Board board;

	@Override
	public int compareTo(BoardImage o) {
		// TODO Auto-generated method stub
		return this.ord - o.ord;
	}
	
	public void changeBoard(Board board) {
		this.board = board;
	}
	
	public BoardImageDTO of() {
		return BoardImageDTO.builder()
				.uuid(uuid)
				.filename(filename)
				.ord(ord)
				.build();
	}
}
