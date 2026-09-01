package kr.or.oti.b01.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class SampleJSONController {
	
	@GetMapping("/helloArr")
	public String[] hello() {
		
		log.info("HelloArr ...........");
		
		return new String[] {"aaa","bbb", "ccc"};
	}
	
	@GetMapping("/helloJson")
	public Map<String, Object> hellJson() {
		
		log.info("hellJson ...........");
		
		return Map.of("name", "hong",
				"age", 10);
	}
}
