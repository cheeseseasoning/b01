package kr.or.oti.b01.dto.upload;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.coobird.thumbnailator.Thumbnailator;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadResultDTO {
	private String uuid;
	private String filename;
	private String realFilename;
	private boolean img;
	public String getLink() {
		return img ? "s_" + realFilename : realFilename; 
	}
	
	public UploadResultDTO(String uploadPath, MultipartFile file) {
		this.uuid = UUID.randomUUID().toString();
		this.filename = file.getOriginalFilename();
		this.realFilename = this.uuid + "_" + this.filename; 
		this.img = false;
		
		Path path = Paths.get(uploadPath, realFilename);
		
		try {
			file.transferTo(path);
			
			if (file.getContentType().startsWith("image/")) {
				
				Path thumbFile = Paths.get(uploadPath, "s_" + realFilename);
				
				//섬네일 이미지 저장 
				Thumbnailator.createThumbnail(path.toFile(), thumbFile.toFile(), 200, 200);
				
				this.img = true;
			}
		} catch (Exception e) {
			
		}
	}
}
