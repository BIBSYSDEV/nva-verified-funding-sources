package no.sikt.nva.funding.verified.nfr.handlers;

import static no.sikt.nva.funding.verified.nfr.EnvironmentKeys.ALLOWED_ORIGIN;
import static no.sikt.nva.funding.verified.nfr.EnvironmentKeys.API_DOMAIN;
import static no.sikt.nva.funding.verified.nfr.EnvironmentKeys.API_HOST;
import static no.sikt.nva.funding.verified.nfr.EnvironmentKeys.COGNITO_AUTHORIZER_URLS;
import static no.sikt.nva.funding.verified.nfr.EnvironmentKeys.CUSTOM_DOMAIN_NAME_PATH;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.sikt.nva.funding.verified.nfr.client.NfrApiClient;
import no.sikt.nva.funding.verified.nfr.client.model.NfrFunding;
import no.sikt.nva.funding.verified.nfr.model.Funding;
import no.sikt.nva.funding.verified.nfr.model.PagedSearchResult;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.zalando.problem.Problem;

@WireMockTest(httpsEnabled = true)
class QueryNfrFundingsHandlerTest {

    private final Context context = new FakeContext();
    private QueryNfrFundingsHandler handlerUnderTest;
    private NfrApiStubber stubber;
    private ByteArrayOutputStream output;
    private Environment environment;

    @BeforeEach
    public void setup(WireMockRuntimeInfo runtimeInfo) {
        this.environment = mock(Environment.class);

        when(environment.readEnv(API_DOMAIN)).thenReturn("localhost");
        when(environment.readEnv(ALLOWED_ORIGIN)).thenReturn("*");
        when(environment.readEnv(CUSTOM_DOMAIN_NAME_PATH)).thenReturn("verified-funding");
        when(environment.readEnv(API_HOST)).thenReturn("localhost");
        when(environment.readEnv(COGNITO_AUTHORIZER_URLS)).thenReturn("http://localhost:3000");

        var httpClient = WiremockHttpClient.create();
        var apiClient = new NfrApiClient(httpClient, URI.create(runtimeInfo.getHttpsBaseUrl()));
        handlerUnderTest = new QueryNfrFundingsHandler(environment, apiClient);
        stubber = new NfrApiStubber();
        output = new ByteArrayOutputStream();
    }

    @Test
    void shouldUseOnlyNameQueryParameterIfCombinedWithTermQueryParameter() throws IOException {
        var numberOfMatchesByName = 2;
        var numberOfAdditionalMatches = 2;
        var from = 0;
        var size = 10;

        var leadName = stubber.withMatchingLeadNameAsMostRelevantHit(numberOfMatchesByName,
                                                                     numberOfAdditionalMatches,
                                                                     from,
                                                                     size);

        var input = new HandlerRequestBuilder<Void>(dtoObjectMapper)
                        .withQueryParameters(Map.of(
                            "name", leadName,
                            "term", "abc",
                            "offset", Integer.toString(from),
                            "size", Integer.toString(size)))
                        .build();

        handlerUnderTest.handleRequest(input, output, context);

        var response = GatewayResponse.fromOutputStream(output, PagedSearchResult.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));

        @SuppressWarnings("unchecked")
        PagedSearchResult<Funding> searchResult = response.getBodyObject(PagedSearchResult.class);
        // need to "help" jackson due to type erasure:
        List<Funding> hits = dtoObjectMapper.convertValue(searchResult.getHits(), new TypeReference<>() {
        });

        var expectedProjectIds = stubber.getMatchingEntriesByLeadName(leadName).stream()
                                     .map(NfrFunding::getProjectId).toArray(Integer[]::new);
        var actualProjectIds = hits.stream()
                                   .map(funding -> Integer.parseInt(funding.getIdentifier()))
                                   .collect(Collectors.toList());

        assertThat(searchResult.getTotalSize(), is(equalTo(numberOfMatchesByName)));

        assertThat(hits, iterableWithSize(numberOfMatchesByName));
        assertThat(actualProjectIds, hasItems(expectedProjectIds));
    }

    @Test
    void shouldUseTermParameterIfPresent() throws IOException {
        var from = 0;
        var size = 10;
        var noTermMatches = 3;
        var term = randomString();
        var matches = stubber.withRandomMatches(term, noTermMatches, from, size);

        var input = new HandlerRequestBuilder<Void>(dtoObjectMapper)
                        .withQueryParameters(Map.of(
                            "term", term,
                            "offset", Integer.toString(from),
                            "size", Integer.toString(size)))
                        .build();

        handlerUnderTest.handleRequest(input, output, context);

        var response = GatewayResponse.fromOutputStream(output, PagedSearchResult.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));

        @SuppressWarnings("unchecked")
        PagedSearchResult<Funding> searchResult = response.getBodyObject(PagedSearchResult.class);
        // need to "help" jackson due to type erasure:
        List<Funding> hits = dtoObjectMapper.convertValue(searchResult.getHits(), new TypeReference<>() {
        });

        var expectedProjectIds = matches.stream()
                                     .map(NfrFunding::getProjectId).toArray(Integer[]::new);
        var actualProjectIds = hits.stream()
                                   .map(funding -> Integer.parseInt(funding.getIdentifier()))
                                   .collect(Collectors.toList());

        assertThat(searchResult.getTotalSize(), is(equalTo(noTermMatches)));

        assertThat(hits, iterableWithSize(noTermMatches));
        assertThat(actualProjectIds, hasItems(expectedProjectIds));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-10", "0"})
    void nonPositiveSizeShouldGiveBadRequest(String size) throws IOException {
        var input = new HandlerRequestBuilder<Void>(dtoObjectMapper)
                        .withQueryParameters(Map.of(
                            "term", randomString(),
                            "offset", "0",
                            "size", size))
                        .build();

        handlerUnderTest.handleRequest(input, output, context);

        var response = GatewayResponse.fromOutputStream(output, PagedSearchResult.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));

        var problem = dtoObjectMapper.readValue(response.getBody(), Problem.class);
        assertThat(problem.getDetail(), is(equalTo("Size must be a positive integer!")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"111111111111111111111111", "1a1b1c"})
    void nonIntegerSizeShouldGiveBadRequest(String size) throws IOException {
        var input = new HandlerRequestBuilder<Void>(dtoObjectMapper)
                        .withQueryParameters(Map.of(
                            "term", randomString(),
                            "offset", "0",
                            "size", size))
                        .build();

        handlerUnderTest.handleRequest(input, output, context);

        var response = GatewayResponse.fromOutputStream(output, PagedSearchResult.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));

        var problem = dtoObjectMapper.readValue(response.getBody(), Problem.class);
        assertThat(problem.getDetail(), is(equalTo("Size must be a positive integer!")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-10", "-1"})
    void nonZeroOrPositiveOffsetShouldGiveBadRequest(String offset) throws IOException {
        var input = new HandlerRequestBuilder<Void>(dtoObjectMapper)
                        .withQueryParameters(Map.of(
                            "term", randomString(),
                            "offset", offset,
                            "size", "10"))
                        .build();

        handlerUnderTest.handleRequest(input, output, context);

        var response = GatewayResponse.fromOutputStream(output, PagedSearchResult.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));

        var problem = dtoObjectMapper.readValue(response.getBody(), Problem.class);
        assertThat(problem.getDetail(), is(equalTo("Offset must be a zero or positive integer!")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"111111111111111111111111", "1a1b1c"})
    void nonIntegerOffsetShouldGiveBadRequest(String offset) throws IOException {
        var input = new HandlerRequestBuilder<Void>(dtoObjectMapper)
                        .withQueryParameters(Map.of(
                            "term", randomString(),
                            "offset", offset,
                            "size", "10"))
                        .build();

        handlerUnderTest.handleRequest(input, output, context);

        var response = GatewayResponse.fromOutputStream(output, PagedSearchResult.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));

        var problem = dtoObjectMapper.readValue(response.getBody(), Problem.class);
        assertThat(problem.getDetail(), is(equalTo("Offset must be a zero or positive integer!")));
    }

    @Test
    void missingBothNameAndTermQueryParamShouldGiveBadRequest() throws IOException {
        var input = new HandlerRequestBuilder<Void>(dtoObjectMapper)
                        .withQueryParameters(Map.of(
                            "offset", "0",
                            "size", "10"))
                        .build();

        handlerUnderTest.handleRequest(input, output, context);

        var response = GatewayResponse.fromOutputStream(output, PagedSearchResult.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));

        var problem = dtoObjectMapper.readValue(response.getBody(), Problem.class);
        assertThat(problem.getDetail(), is(equalTo("Missing from query parameters: term")));
    }

    @Test
    void shouldEncodeQueryParametersCorrectly() throws IOException, InterruptedException {
        var httpClient = mock(HttpClient.class);
        var apiClient = new NfrApiClient(httpClient, URI.create("https://example.org"));
        handlerUnderTest = new QueryNfrFundingsHandler(environment, apiClient);

        var paalUnencoded = "Pål";
        final var paalEncoded = "P%C3%A5l";

        var input = new HandlerRequestBuilder<Void>(dtoObjectMapper)
                        .withQueryParameters(Map.of("term", paalUnencoded,
                                                    "offset", "0",
                                                    "size", "10"))
                        .build();
        handlerUnderTest.handleRequest(input, output, context);
        var expectedRequest = HttpRequest.newBuilder()
                      .uri(URI.create("https://example.org/search?query=" + paalEncoded + "&from=0&size=10"))
                      .GET()
                      .build();
        verify(httpClient).send(expectedRequest, BodyHandlers.ofString());
    }
}
