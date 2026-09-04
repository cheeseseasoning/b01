package kr.or.oti.b01.util;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3Uploader {
	private final AmazonS3Client amazonS3Client;
	
	@Value("${cloud.aws.s3.bucket}")
	public String bucket;
	
	public String upload(String filepath) throws RuntimeException {
		File targetFile = new File(filepath);
		
		//s3에 로컬 파일을 업로드한다.
		String uploadImageUrl = putS3(targetFile, targetFile.getName());
		
		//s3에 업로드 된 로컬 파일을 삭제한다.
		removeOriginalFile(targetFile);
		
		//s3에 업로드한 파일의 url 경로를 리턴한다.
		return uploadImageUrl;
	}
	
	private String putS3(File targetFile, String name) {
		amazonS3Client.putObject(new PutObjectRequest(bucket, name, targetFile)
				.withCannedAcl(CannedAccessControlList.PublicRead));
		return amazonS3Client.getUrl(bucket, name).toString();
	}

	// S3 객체 키를 브라우저에서 사용할 수 있는 전체 URL로 변환한다.
	public String getFileUrl(String fileName) {
		if (fileName == null || fileName.isBlank()) {
			return fileName;
		}
		if (fileName.startsWith("http://") || fileName.startsWith("https://")) {
			return fileName;
		}
		return amazonS3Client.getUrl(bucket, fileName).toString();
	}

	// 과거 데이터에 전체 URL이 저장된 경우에도 실제 S3 객체 키만 반환한다.
	public String getObjectKey(String fileName) {
		if (fileName == null || fileName.isBlank()) {
			return fileName;
		}
		return fileName.substring(fileName.lastIndexOf('/') + 1);
	}
	
    //S3에 업로드된 파일을 삭제한다
    public void removeS3File(String fileName) {
        //삭제 요청 객체 선언 
        amazonS3Client.deleteObject(new DeleteObjectRequest(bucket, fileName));
    }

	private void removeOriginalFile(File targetFile) {
		if (targetFile.exists() && targetFile.delete()) {
			log.info("잘 삭제되었습니다.");
			return;
		}
	}
}
