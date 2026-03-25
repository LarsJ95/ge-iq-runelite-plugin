package com.geiq.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Singleton
public class ApiClient
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private final OkHttpClient httpClient;
	private final Gson gson;

	@Inject
	public ApiClient(OkHttpClient httpClient)
	{
		this.httpClient = httpClient;
		this.gson = new Gson();
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
}
