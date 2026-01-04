package com.tallybot.backend.tallybot_back;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Test
@Disabled("Local/CI test environment does not provide AWS credentials for SecretsManager")
@SpringBootTest
class TallybotBackApplicationTests {

	@Test
	void contextLoads() {
	}

}
