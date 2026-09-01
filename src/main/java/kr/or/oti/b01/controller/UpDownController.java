package kr.or.oti.b01.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.ApiOperation;
import kr.or.oti.b01.dto.upload.UploadFileDTO;
import kr.or.oti.b01.dto.upload.UploadResultDTO;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnailator;

@RestController
@Slf4j
public class UpDownController {
	
	
	@Value("${spring.servlet.multipart.location}")
	private String uploadPath;
	
	@ApiOperation(value = "remove 파일", notes = "DELETE 방식으로 파일 삭제")
	@org.springframework.web.bind.annotation.DeleteMapping(value = "/remove/{fileName}")
	public java.util.Map<String, Boolean> removeFile(@PathVariable("fileName") String fileName) {
		
		log.info("삭제 요청받은 파일 이름: " + fileName);
		log.info("설정된 uploadPath: " + uploadPath);
		
		// 실제 파일이 생성되는 경로를 콘솔로 정확히 확인하기 위해 출력
		File targetFile = new File(uploadPath + File.separator + fileName);
		log.info("최종 삭제 대상 파일 전체 경로: " + targetFile.getAbsolutePath());
		log.info("파일이 실제로 존재하는지 여부(exists): " + targetFile.exists());
		
		boolean result = false;
		
		try {
			// 원본 파일 삭제
			result = targetFile.delete();
			log.info("파일 삭제 결과(true/false): " + result);
			
		} catch (Exception e) {
			log.error("파일 삭제 중 예외 발생: " + e.getMessage());
		}
		
		java.util.Map<String, Boolean> resultMap = new java.util.HashMap<>();
		resultMap.put("result", result);
		
		return resultMap;
	}
	@ApiOperation(value = "Upload post", notes = "POST 방식으로 파일 등록")
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public List<UploadResultDTO> upload(UploadFileDTO uploadFileDTO) {
		
		log.info("uploadPath = " + uploadPath);
		log.info("upload = " + uploadFileDTO);
		
		if (uploadFileDTO.getFiles() != null) {
			final List<UploadResultDTO> list = new ArrayList<UploadResultDTO>();
			
			for(MultipartFile file : uploadFileDTO.getFiles()) {
				log.info("원본 파일명 : " + file.getOriginalFilename());
				log.info("파일 유형 : " + file.getContentType());
				log.info("파일 사이즈 : " + file.getSize());
			
				list.add(new UploadResultDTO(uploadPath, file));
			}
			return list;
		}
		return null;
	}
	
	@ApiOperation(value = "view 파일", notes = "Get 방식으로 파일 조회")
	@GetMapping(value = "/view/{filename}")
	public ResponseEntity<Resource> viewFileGet(@PathVariable("filename") String filename) {
		//"\\" 윈도우 경로 구분 문자 
		//"/"  mac / linux 경로 구분 문자
		Resource resource = new FileSystemResource(uploadPath + File.separator + filename);
		HttpHeaders headers = new HttpHeaders();
		String resourceName = resource.getFilename();
		
		try {
			headers.add("Content-Type", Files.probeContentType(resource.getFile().toPath()));
			headers.add("Content-Length", String.valueOf(resource.getFile().length()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return ResponseEntity.ok().headers(headers).body(resource);
	}
}
