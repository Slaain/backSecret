package myavocat.legit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test") // ✅ Force le profil test (H2, pas de connexion à Postgres/Flyway)
class LegitApplicationTests {

	@Test
	void contextLoads() {
		// Vérifie simplement que le contexte Spring démarre
	}
}
