package kr.or.oti.b01;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
public class DataSourceTests {
	@Autowired
	private DataSource dataSource;
	
	@Test
	public void testConnection() throws Exception {
		@Cleanup
		Connection conn = dataSource.getConnection();
		
		log.info("conn.toString() = " + conn);
		
		Assertions.assertNotNull(conn);
	}
	
}
