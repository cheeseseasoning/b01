package kr.or.oti.b01.service;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.or.oti.b01.domain.Board;
import kr.or.oti.b01.dto.BoardDTO;
import kr.or.oti.b01.dto.BoardListAllDTO;
import kr.or.oti.b01.dto.BoardListReplyCountDTO;
import kr.or.oti.b01.dto.PageRequestDTO;
import kr.or.oti.b01.dto.PageResponseDTO;
import kr.or.oti.b01.repository.BoardRepository;
import kr.or.oti.b01.util.S3Uploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardServiceImpl implements BoardService {
	private final BoardRepository boardRepository;
	private final ModelMapper mapper;
	
	private final S3Uploader s3Uploader;
	
	public void register(BoardDTO boardDTO) {
//		boardRepository.save(mapper.map(boardDTO, Board.class));
		Board board = dtoToEntity(boardDTO);
		boardRepository.save(board);
		
		System.out.println("DEBUG boardDTO ..." + boardDTO);
		System.out.println("DEBUG board ..." + board);
	}
	
	public PageResponseDTO<BoardDTO> getList(PageRequestDTO pageRequestDTO) {
		Pageable pageable = PageRequest.of(pageRequestDTO.getPage()-1, pageRequestDTO.getSize(), Sort.by("bno").descending());
		Page<Board> page = boardRepository.searchAll(pageRequestDTO.getTypes(), pageRequestDTO.getKeyword(), pageable);
		
		List<BoardDTO> dtoList = page.getContent().stream()
				.map(board -> mapper.map(board, BoardDTO.class))
				.collect(Collectors.toList());
		
		int total = (int) page.getTotalElements();
		
		log.info(dtoList.toString());
		log.info("total = " + total);
		
		PageResponseDTO<BoardDTO> result = new PageResponseDTO<>(pageRequestDTO, dtoList, total);
		
		return result;
	}
	
	public BoardDTO get(long bno) {
	    // Optional 처리: 데이터가 없을 경우 NoSuchElementException 발생
	    Board board = boardRepository.findByIdWithImages(bno)
	            .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다. bno=" + bno));
	    
	    BoardDTO boardDTO = entityToDto(board);
	    List<String> fileNames = boardDTO.getFileNames().stream()
	            .map(s3Uploader::getObjectKey)
	            .collect(Collectors.toList());
	    boardDTO.setFileNames(fileNames);
	    boardDTO.setFileUrls(fileNames.stream()
	            .map(s3Uploader::getFileUrl)
	            .collect(Collectors.toList()));
	    return boardDTO;
	}

	@Transactional
	public void remove(long bno) {
	    Board board = boardRepository.findByIdWithImages(bno)
	        .orElseThrow(() ->
	            new IllegalArgumentException(
	                "해당 게시글이 존재하지 않습니다. bno=" + bno
	            )
	        );

	    board.getImageSet().forEach(image -> {
	        String savedValue = image.getUuid() + "_" + image.getFilename();

	        // 전체 URL이 저장되어 있으면 마지막 '/' 뒤의 S3 객체 키만 추출
	        String s3Key = s3Uploader.getObjectKey(savedValue);

	        log.info("S3 삭제 대상 key: {}", s3Key);
	        s3Uploader.removeS3File(s3Key);
	    });

	    boardRepository.deleteById(bno);
	}

	public void modify(BoardDTO boardDTO) {
	    Board board = boardRepository.findByIdWithImages(boardDTO.getBno())
	            .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다. bno=" + boardDTO.getBno()));
	    
	    board.change(boardDTO.getTitle(), boardDTO.getContent());
	    
	    board.clearImages();
	    
		if (boardDTO.getFileNames() != null) {
			boardDTO.getFileNames().forEach(fileName -> {
				String objectKey = s3Uploader.getObjectKey(fileName);
				String[] arr = objectKey.split("_", 2);
				board.addImage(arr[0], arr[1]);
			});
		}	    
	    
		boardRepository.save(board);
	}
	
	@Transactional
	public void removeBatch(List<Long> bnos) {
	    log.info("removeBatch bnos: {}", bnos);

	    if (bnos == null || bnos.isEmpty()) {
	        return;
	    }

	    for (Long bno : bnos) {
	        remove(bno);
	    }
	}

	public PageResponseDTO<BoardListReplyCountDTO> listWithReplyCount(PageRequestDTO pageRequestDTO) {
		Pageable pageable = PageRequest.of(pageRequestDTO.getPage()-1, pageRequestDTO.getSize(), Sort.by("bno").descending());
		Page<BoardListReplyCountDTO> page = boardRepository.searchWithReplyCount(pageRequestDTO.getTypes(), pageRequestDTO.getKeyword(), pageable);
		
		return new PageResponseDTO<>(pageRequestDTO, page.getContent(), (int) page.getTotalElements());
	}
	
	public PageResponseDTO<BoardListAllDTO> listWithAll(PageRequestDTO pageRequestDTO) {
		Pageable pageable = PageRequest.of(pageRequestDTO.getPage()-1, pageRequestDTO.getSize(), Sort.by("bno").descending());
		Page<BoardListAllDTO> page = boardRepository.searchWithAll(pageRequestDTO.getTypes(), pageRequestDTO.getKeyword(), pageable);
		page.getContent().forEach(dto -> dto.getBoardImages().forEach(image -> {
			String savedValue = image.getUuid() + "_" + image.getFilename();
			String objectKey = s3Uploader.getObjectKey(savedValue);
			image.setUrl(s3Uploader.getFileUrl(objectKey));
		}));
		
		return new PageResponseDTO<>(pageRequestDTO, page.getContent(), (int) page.getTotalElements());
	}
}
