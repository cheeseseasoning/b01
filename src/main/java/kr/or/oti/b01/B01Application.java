package kr.or.oti.b01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class B01Application {

	public static void main(String[] args) {
		SpringApplication.run(B01Application.class, args);
	}
	
	//소유자가 로그인 기능 구현함

}
