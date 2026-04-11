package com.geiq.plugin;

public class TradePayload
{
	private final int itemId;
	private final String itemName;
	private final int quantity;
	private final int price;
	private final int pricePerItem;
	private final boolean isBuy;
	private final String status;
	private final int slot;
	private final long timestamp;

	public TradePayload(int itemId, String itemName, int quantity, int price,
						int pricePerItem, boolean isBuy, String status,
						int slot, long timestamp)
	{
		this.itemId = itemId;
		this.itemName = itemName;
		this.quantity = quantity;
		this.price = price;
		this.pricePerItem = pricePerItem;
		this.isBuy = isBuy;
		this.status = status;
		this.slot = slot;
		this.timestamp = timestamp;
	}

	public int getItemId()
	{
		return itemId;
	}

	public String getItemName()
	{
		return itemName;
	}

	public int getQuantity()
	{
		return quantity;
	}

	public int getPrice()
	{
		return price;
	}

	public int getPricePerItem()
	{
		return pricePerItem;
	}

	public boolean isBuy()
	{
		return isBuy;
	}

	public String getStatus()
	{
		return status;
	}

	public int getSlot()
	{
		return slot;
	}

	public long getTimestamp()
	{
		return timestamp;
	}
}
