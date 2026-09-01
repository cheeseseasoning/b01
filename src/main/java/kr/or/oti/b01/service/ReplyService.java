package kr.or.oti.b01.service;

import kr.or.oti.b01.dto.PageRequestDTO;
import kr.or.oti.b01.dto.PageResponseDTO;
import kr.or.oti.b01.dto.ReplyDTO;

public interface ReplyService {
	ReplyDTO register(ReplyDTO boardDTO);
	PageResponseDTO<ReplyDTO> getList(long bno, PageRequestDTO pageRequestDTO);
	ReplyDTO get(long rno);
	void remove(long rno);
	void modify(ReplyDTO replyDTO);
}
