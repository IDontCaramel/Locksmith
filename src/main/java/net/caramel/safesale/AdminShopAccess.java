package net.caramel.safesale;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class AdminShopAccess {
	private static final Set<UUID> ENABLED_ADMINS = new HashSet<>();

	private AdminShopAccess() {
	}

	public static boolean toggle(UUID playerUuid) {
		if (ENABLED_ADMINS.remove(playerUuid)) {
			return false;
		}

		ENABLED_ADMINS.add(playerUuid);
		return true;
	}

	public static boolean isEnabled(UUID playerUuid) {
		return ENABLED_ADMINS.contains(playerUuid);
	}
}
