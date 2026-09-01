package kr.or.oti.b01.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardListAllDTO {
	private Long bno;
	private String title;
	private String writer;
	private LocalDateTime regDate;
	private Long replyCount;

	private List<BoardImageDTO> boardImages;
}
