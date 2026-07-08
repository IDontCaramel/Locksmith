package net.caramel.safesale;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SafeSaleCommandsTest {
	@Test
	void deleteRegionNameArgumentSuggestsSavedRegionNames() throws IOException {
		String source = Files.readString(Path.of("src/main/java/net/caramel/safesale/SafeSaleCommands.java"));

		assertTrue(Pattern.compile("literal\\(\"delete\"\\).*argument\\(\"region_name\", word\\(\\)\\).*suggests\\(REGION_NAME_SUGGESTIONS\\).*executeDeleteRegion", Pattern.DOTALL).matcher(source).find());
	}
}
