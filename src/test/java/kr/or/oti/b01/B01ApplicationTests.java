package kr.or.oti.b01;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class B01ApplicationTests {

	@Test
	void contextLoads() {
		System.out.println("passwd(kosa1004) : " + new BCryptPasswordEncoder().encode("kosa1004"));
	}

}
