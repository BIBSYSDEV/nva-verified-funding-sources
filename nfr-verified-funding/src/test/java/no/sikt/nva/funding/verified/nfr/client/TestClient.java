package no.sikt.nva.funding.verified.nfr.client;

import java.net.URI;
import java.net.http.HttpClient;
import nva.commons.apigateway.exceptions.BadGatewayException;

public class TestClient {

    public static void main(String[] args) throws BadGatewayException {
        var httpClient = HttpClient.newBuilder().build();
        var baseUri = URI.create("https://prosjektbanken.forskningsradet.no/prosjektbanken/rest/cristin");
        var client = new NfrApiClient(httpClient, baseUri);

        var searchResult = client.fetchByProjectId(193391);

        System.out.println(searchResult);
    }
}
