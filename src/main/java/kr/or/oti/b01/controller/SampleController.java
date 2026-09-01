package kr.or.oti.b01.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class SampleController {
	
	class SampleDTO {
		
		SampleDTO(String p1, String p2, String p3) {
			super();
			this.p1 = p1;
			this.p2 = p2;
			this.p3 = p3;
		}

		private String p1, p2, p3;

		public String getP1() {
			return p1;
		}

		public String getP2() {
			return p2;
		}

		public String getP3() {
			return p3;
		}
		
	}
	
	@GetMapping("/hello")
	public void hello(Model model) {
		
		log.info("Hello ...........");
		
		model.addAttribute("msg", "Hello World");
	}
	
	@GetMapping("/ex1")
	public void ex1(Model model) {
		
		log.info("ex1 ...........");
		
		model.addAttribute("list", Arrays.asList("AAA", "BBB", "CCC", "DDD"));
	}
	
	@GetMapping("/ex2")
	public void ex2(Model model) {
		
		log.info("ex2 ...........");
		
		List<String> strList = IntStream.range(1, 10)
				.mapToObj(i -> "Data" + i)
				.collect(Collectors.toList());
		model.addAttribute("list", strList);
		
		model.addAttribute("map", Map.of("A", "AAA"
									   , "B", "BBB"));
		
		
		model.addAttribute("dto", new SampleDTO(  "Value -- p1"
												, "Value -- p2"
												, "Value -- p3"));
	}
	
	@GetMapping("/ex3")
	public void ex3(Model model) {
		
		log.info("ex3 ...........");
		
		model.addAttribute("arr", Arrays.asList("AAA", "BBB", "CCC", "DDD"));
	}
}
