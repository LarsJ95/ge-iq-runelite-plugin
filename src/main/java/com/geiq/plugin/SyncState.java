package com.geiq.plugin;

import java.util.Collections;
import java.util.List;
import lombok.Data;

/**
 * Snapshot of the user's GE IQ state pulled from /api/sync-pull.
 * All collections may be null when deserialized from JSON; use the {@code *OrEmpty}
 * helpers in {@link GeIqPlugin} rather than reading the fields directly.
 */
@Data
public class SyncState
{
	private List<Integer> favorites;
	private List<PortfolioItem> portfolio;
	private List<Investment> investments;
	private String updatedAt;

	public List<Integer> favoritesOrEmpty()
	{
		return favorites != null ? favorites : Collections.emptyList();
	}

	public List<PortfolioItem> portfolioOrEmpty()
	{
		return portfolio != null ? portfolio : Collections.emptyList();
	}

	public List<Investment> investmentsOrEmpty()
	{
		return investments != null ? investments : Collections.emptyList();
	}

	@Data
	public static class PortfolioItem
	{
		private int itemId;
		private Integer buyPrice;
		private Integer sellPrice;
		private Integer soldPrice;
		private Integer quantity;

		public boolean isActive()
		{
			return soldPrice == null;
		}
	}

	@Data
	public static class Investment
	{
		private int itemId;
		private String itemName;
		private int buyPrice;
		private int quantity;
		private Integer targetSellPrice;
		private Integer soldPrice;

		public boolean isActive()
		{
			return soldPrice == null;
		}
	}
}
