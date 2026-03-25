package com.geiq.plugin;

import com.google.inject.Provides;
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
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "GE IQ Sync",
	description = "Syncs Grand Exchange trades with GE IQ",
	tags = {"grand exchange", "flip", "trade", "sync"}
)
@Slf4j
public class GeIqPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private GeIqConfig config;

	@Inject
	private ApiClient apiClient;

	private final List<TradePayload> pendingTrades = new ArrayList<>();
	private ScheduledExecutorService executor;

	@Override
	protected void startUp()
	{
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
		log.info("GE IQ Sync stopped");
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

	@Provides
	GeIqConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GeIqConfig.class);
	}
}
