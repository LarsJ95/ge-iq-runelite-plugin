package com.geiq.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Singleton
public class ApiClient
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private final OkHttpClient httpClient;
	private final Gson gson;

	@Inject
	public ApiClient(OkHttpClient httpClient, Gson gson)
	{
		this.httpClient = httpClient;
		this.gson = gson;
	}

	public void sendTrades(String apiUrl, String syncCode, List<TradePayload> trades) throws IOException
	{
		JsonObject body = new JsonObject();
		body.addProperty("syncCode", syncCode);
		body.add("trades", gson.toJsonTree(trades));

		Request request = new Request.Builder()
			.url(apiUrl)
			.header("User-Agent", "GE IQ RuneLite Plugin")
			.header("Content-Type", "application/json")
			.post(RequestBody.create(JSON, body.toString()))
			.build();

		try (Response response = httpClient.newCall(request).execute())
		{
			if (!response.isSuccessful())
			{
				throw new IOException("GE IQ API returned " + response.code());
			}
		}
	}

	/**
	 * GET /api/sync-pull?code={syncCode}. Returns null when the server has no synced
	 * data for this code (HTTP 404); throws on transport errors or other failures.
	 * Derives the sync-pull URL from the trade URL by replacing the trailing path
	 * segment so /api/trade → /api/sync-pull.
	 */
	public SyncState fetchSyncState(String apiUrl, String syncCode) throws IOException
	{
		String pullUrl = derivePullUrl(apiUrl) + "?code=" + URLEncoder.encode(syncCode, StandardCharsets.UTF_8.name());

		Request request = new Request.Builder()
			.url(pullUrl)
			.header("User-Agent", "GE IQ RuneLite Plugin")
			.get()
			.build();

		try (Response response = httpClient.newCall(request).execute())
		{
			if (response.code() == 404)
			{
				return null;
			}
			if (!response.isSuccessful())
			{
				throw new IOException("GE IQ sync-pull returned " + response.code());
			}
			ResponseBody body = response.body();
			if (body == null)
			{
				throw new IOException("GE IQ sync-pull returned empty body");
			}
			return gson.fromJson(body.charStream(), SyncState.class);
		}
	}

	/**
	 * Replace the last path segment of the trade URL with "sync-pull".
	 * Example: https://ge-iq.com/api/trade → https://ge-iq.com/api/sync-pull
	 */
	static String derivePullUrl(String apiUrl)
	{
		String trimmed = apiUrl;
		while (trimmed.endsWith("/"))
		{
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		int queryIdx = trimmed.indexOf('?');
		String base = queryIdx >= 0 ? trimmed.substring(0, queryIdx) : trimmed;
		int lastSlash = base.lastIndexOf('/');
		// Don't slice off the scheme's "//"
		if (lastSlash > "https://".length())
		{
			return base.substring(0, lastSlash) + "/sync-pull";
		}
		return base + "/sync-pull";
	}
}
