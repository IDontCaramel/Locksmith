package net.caramel.safesale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BuyScreenHandlerTest {
	@Test
	void interceptsOnlyShopDisplaySlots() {
		assertTrue(BuyScreenHandler.isShopDisplaySlot(0));
		assertTrue(BuyScreenHandler.isShopDisplaySlot(26));
		assertFalse(BuyScreenHandler.isShopDisplaySlot(27));
		assertFalse(BuyScreenHandler.isShopDisplaySlot(-999));
	}
}
