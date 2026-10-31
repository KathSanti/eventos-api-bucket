package com.duoc.eventos_api_bucket;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Usa src/test/resources/application-test.properties (secretos de prueba, BD H2 propia).
@SpringBootTest
@ActiveProfiles("test")
class EventosApiBucketApplicationTests {

	@Test
	void contextLoads() {
	}

}
