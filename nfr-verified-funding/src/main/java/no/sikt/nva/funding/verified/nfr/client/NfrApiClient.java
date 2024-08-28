package no.sikt.nva.funding.verified.nfr.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import no.sikt.nva.funding.verified.nfr.EnvironmentKeys;
import no.sikt.nva.funding.verified.nfr.client.model.NfrFundingSearchResult;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NfrApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NfrApiClient.class);

    private final transient HttpClient httpClient;
    private final transient URI baseUri;

    @JacocoGenerated
    public static NfrApiClient defaultClient() {
        var baseUri = URI.create(new Environment().readEnv(EnvironmentKeys.NFR_API_BASE_URI));
        return new NfrApiClient(HttpClient.newBuilder().build(), baseUri);
    }

    public NfrApiClient(HttpClient httpClient, URI baseUri) {
        this.httpClient = httpClient;
        this.baseUri = baseUri;
    }

    public NfrFundingSearchResult query(String query, int offset, int size) throws BadGatewayException {
        URI requestUri = UriWrapper.fromUri(baseUri)
                             .addChild("search")
                             .addQueryParameter("query", query)
                             .addQueryParameter("from", Integer.toString(offset))
                             .addQueryParameter("size", Integer.toString(size))
                             .getUri();
        HttpRequest request = HttpRequest.newBuilder().GET().uri(requestUri).build();

        try {
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                return JsonUtils.dtoObjectMapper.readValue(response.body(), NfrFundingSearchResult.class);
            }

            throw new BadGatewayException(String.format("Unexpected response: %d - %s",
                                                        response.statusCode(), response.body()));
        } catch (IOException | InterruptedException e) {
            throw logAndCreateCustomException(e);
        }
    }

    @JacocoGenerated
    private BadGatewayException logAndCreateCustomException(Exception e) {
        var message = "Failed to communicate with NFR rest api!";
        LOGGER.error(message, e);

        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        return new BadGatewayException(message);
    }
}
