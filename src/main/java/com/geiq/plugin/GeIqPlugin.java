package com.geiq.plugin;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
	name = "GE IQ Sync",
	description = "Syncs Grand Exchange trades with GE IQ",
	tags = {"grand exchange", "flip", "trade", "sync"}
)
@Slf4j
public class GeIqPlugin extends Plugin
{
	private static final String CONFIG_GROUP = "geiq";
	private static final String KEY_TOTAL_SYNCED = "totalSynced";
	private static final String KEY_LAST_SYNC_MS = "lastSyncMs";

	@Inject
	private Client client;

	@Inject
	private GeIqConfig config;

	@Inject
	private ApiClient apiClient;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	private final List<TradePayload> pendingTrades = new ArrayList<>();
	private ScheduledExecutorService executor;
	private NavigationButton navButton;
	private GeIqPluginPanel panel;

	@Override
	protected void startUp()
	{
		panel = new GeIqPluginPanel();
		refreshPanel();

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("GE IQ Sync")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(this::flushTrades, 5, 5, TimeUnit.SECONDS);
		log.info("GE IQ Sync started");
	}

	@Override
	protected void shutDown()
	{
		flushTrades();
		if (executor != null)
		{
			executor.shutdown();
		}
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}
		panel = null;
		log.info("GE IQ Sync stopped");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CONFIG_GROUP.equals(event.getGroup()))
		{
			refreshPanel();
		}
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
		if (!config.syncEnabled())
		{
			return;
		}

		String syncCode = config.syncCode().trim().toUpperCase();
		if (syncCode.length() != 6)
		{
			return;
		}

		GrandExchangeOffer offer = event.getOffer();
		if (offer == null)
		{
			return;
		}

		GrandExchangeOfferState state = offer.getState();
		if (state == GrandExchangeOfferState.EMPTY)
		{
			return;
		}

		String status;
		switch (state)
		{
			case BUYING:
				status = "buying";
				break;
			case SELLING:
				status = "selling";
				break;
			case BOUGHT:
				status = "completed";
				break;
			case SOLD:
				status = "completed";
				break;
			case CANCELLED_BUY:
				status = "cancelled";
				break;
			case CANCELLED_SELL:
				status = "cancelled";
				break;
			default:
				return;
		}

		boolean isBuy = (state == GrandExchangeOfferState.BUYING
			|| state == GrandExchangeOfferState.BOUGHT
			|| state == GrandExchangeOfferState.CANCELLED_BUY);

		int quantity = offer.getQuantitySold();
		int totalPrice = offer.getSpent();
		if (quantity <= 0)
		{
			return;
		}

		int itemId = offer.getItemId();
		String itemName = client.getItemDefinition(itemId).getName();

		TradePayload trade = new TradePayload(
			itemId, itemName, quantity, totalPrice,
			totalPrice / quantity, isBuy, status,
			event.getSlot(), System.currentTimeMillis()
		);

		synchronized (pendingTrades)
		{
			pendingTrades.add(trade);
		}

		log.debug("GE IQ: queued trade - {} {} x{} for {}gp",
			isBuy ? "BUY" : "SELL", itemName, quantity, totalPrice);
	}

	private void flushTrades()
	{
		List<TradePayload> toSend;
		synchronized (pendingTrades)
		{
			if (pendingTrades.isEmpty())
			{
				return;
			}
			toSend = new ArrayList<>(pendingTrades);
			pendingTrades.clear();
		}

		String syncCode = config.syncCode().trim().toUpperCase();
		if (syncCode.length() != 6)
		{
			return;
		}

		try
		{
			apiClient.sendTrades(config.apiUrl(), syncCode, toSend);
			int prevTotal = readInt(KEY_TOTAL_SYNCED, 0);
			long now = System.currentTimeMillis();
			configManager.setConfiguration(CONFIG_GROUP, KEY_TOTAL_SYNCED, prevTotal + toSend.size());
			configManager.setConfiguration(CONFIG_GROUP, KEY_LAST_SYNC_MS, now);
			refreshPanel();
			log.info("GE IQ: synced {} trades", toSend.size());
		}
		catch (Exception e)
		{
			log.warn("GE IQ: failed to sync trades", e);
			synchronized (pendingTrades)
			{
				pendingTrades.addAll(0, toSend);
				while (pendingTrades.size() > 100)
				{
					pendingTrades.remove(pendingTrades.size() - 1);
				}
			}
		}
	}

	private void refreshPanel()
	{
		if (panel == null)
		{
			return;
		}
		int total = readInt(KEY_TOTAL_SYNCED, 0);
		long lastSync = readLong(KEY_LAST_SYNC_MS, 0L);
		panel.update(config.syncCode(), config.syncEnabled(), total, lastSync);
	}

	private int readInt(String key, int fallback)
	{
		Integer v = configManager.getConfiguration(CONFIG_GROUP, key, Integer.class);
		return v == null ? fallback : v;
	}

	private long readLong(String key, long fallback)
	{
		Long v = configManager.getConfiguration(CONFIG_GROUP, key, Long.class);
		return v == null ? fallback : v;
	}

	@Provides
	GeIqConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GeIqConfig.class);
	}
}
