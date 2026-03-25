package com.geiq.plugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("geiq")
public interface GeIqConfig extends Config
{
	@ConfigItem(
		keyName = "syncCode",
		name = "Sync Code",
		description = "Your 6-character GE IQ sync code. Find it in the Sync menu on ge-iq.vercel.app",
		position = 1
	)
	default String syncCode()
	{
		return "";
	}

	@ConfigItem(
		keyName = "syncEnabled",
		name = "Enable Sync",
		description = "Toggle automatic trade syncing on/off",
		position = 2
	)
	default boolean syncEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "apiUrl",
		name = "API URL",
		description = "GE IQ API endpoint (don't change unless you know what you're doing)",
		position = 3
	)
	default String apiUrl()
	{
		return "https://ge-iq.vercel.app/api/trade";
	}
}
