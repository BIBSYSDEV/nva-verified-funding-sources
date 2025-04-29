package no.sikt.nva.funding.verified.nfr.client;

import java.net.URI;
import java.net.http.HttpClient;
import nva.commons.apigateway.exceptions.BadGatewayException;

@SuppressWarnings({"PMD.CloseResource", "PMD.SystemPrintln"})
public class RunClient {

    public static void main(String[] args) throws BadGatewayException {
        var httpClient = HttpClient.newBuilder().build();
        var baseUri = URI.create("https://prosjektbanken.forskningsradet.no/prosjektbanken/rest/cristin");
        var client = new NfrApiClient(httpClient, baseUri);

        var searchResult = client.query("193391", 0, 10);

        System.out.println(searchResult);
    }
}
