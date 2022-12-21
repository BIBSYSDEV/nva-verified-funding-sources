package no.sikt.nva.funding.verified.nfr.handlers;

import static no.sikt.nva.funding.verified.nfr.EnvironmentKeys.ALLOWED_ORIGIN;
import static no.sikt.nva.funding.verified.nfr.EnvironmentKeys.API_DOMAIN;
import static no.sikt.nva.funding.verified.nfr.EnvironmentKeys.CUSTOM_DOMAIN_NAME_PATH;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import no.sikt.nva.funding.verified.nfr.client.NfrApiClient;
import no.sikt.nva.funding.verified.nfr.model.Funding;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

@WireMockTest(httpsEnabled = true)
public class FetchNfrFundingByIdentifierHandlerTest {

    private final Context context = new FakeContext();
    private FetchNfrFundingByIdentifierHandler handlerUnderTest;
    private NfrApiStubber stubber;
    private ByteArrayOutputStream output;
    private Environment environment;

    public FetchNfrFundingByIdentifierHandlerTest() {
    }

    @BeforeEach
    public void setup(WireMockRuntimeInfo runtimeInfo) {
        this.environment = mock(Environment.class);

        when(environment.readEnv(API_DOMAIN)).thenReturn("localhost");
        when(environment.readEnv(ALLOWED_ORIGIN)).thenReturn("*");
        when(environment.readEnv(CUSTOM_DOMAIN_NAME_PATH)).thenReturn("verified-funding");

        var httpClient = WiremockHttpClient.create();
        var apiClient = new NfrApiClient(httpClient, URI.create(runtimeInfo.getHttpsBaseUrl()));
        handlerUnderTest = new FetchNfrFundingByIdentifierHandler(environment, apiClient);
        stubber = new NfrApiStubber();
        output = new ByteArrayOutputStream();
    }

    @Test
    public void shouldReturnOnlyOneMatchWhenQueryReturnsMultipleButOnlyOneHasExactMatchOnProjectId()
        throws IOException {
        var projectId = stubber.byProjectIdSingleMatch(2);

        var input = new HandlerRequestBuilder<Void>(dtoObjectMapper)
                        .withPathParameters(Map.of("identifier", Integer.toString(projectId)))
                        .build();

        handlerUnderTest.handleRequest(input, output, context);

        var response = GatewayResponse.fromOutputStream(output, Funding.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));

        var funding = response.getBodyObject(Funding.class);

        var expectedId = URI.create("https://localhost/verified-funding/nfr/" + projectId);
        assertThat(funding.getId(), is(equalTo(expectedId)));
        assertThat(funding.getIdentifier(), is(equalTo(Integer.toString(projectId))));

        var nfrFunding = stubber.getMatchingEntryFromNfr(projectId);
        assertThat(funding.getName().get("en"), is(equalTo(nfrFunding.getEnglishMetadata().get("title"))));
        assertThat(funding.getName().get("nb"), is(equalTo(nfrFunding.getNorwegianMetadata().get("title"))));
    }

    @Test
    public void shouldReturnNotFoundStatusCodeWhenNotFoundButCandidatesArePresent()
        throws IOException {
        var projectId = stubber.byProjectIdNoExactMatch(2);

        var input = new HandlerRequestBuilder<Void>(dtoObjectMapper)
                        .withPathParameters(Map.of("identifier", Integer.toString(projectId)))
                        .build();

        handlerUnderTest.handleRequest(input, output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));

        var problem = response.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), is(equalTo(String.format("Funding with identifier %d not found!", projectId))));
    }

    @Test
    public void shouldReturnNotFoundStatusCodeWhenNotFoundAndCandidatesAreNotPresent() throws IOException {
        var projectId = stubber.byProjectIdNoExactMatch(0);

        var input = new HandlerRequestBuilder<Void>(dtoObjectMapper)
                        .withPathParameters(Map.of("identifier", Integer.toString(projectId)))
                        .build();

        handlerUnderTest.handleRequest(input, output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));

        var problem = response.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), is(equalTo(String.format("Funding with identifier %d not found!", projectId))));
    }

    @Test
    public void shouldReturnBadGatewayOnNonSuccessStatusCodeFromNfrApi()
        throws IOException {
        var projectId = stubber.byProjectIdBadRequest();

        var input = new HandlerRequestBuilder<Void>(dtoObjectMapper)
                        .withPathParameters(Map.of("identifier", Integer.toString(projectId)))
                        .build();

        handlerUnderTest.handleRequest(input, output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));

        var problem = response.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), is(equalTo("Unexpected response: 400 - \"{}\"")));
    }

    @Test
    public void shouldReturnBadGatewayWhenUnableToConnectToNfrApi(WireMockRuntimeInfo runtimeInfo)
        throws IOException {

        var httpClient = WiremockHttpClient.create();
        var apiClient = new NfrApiClient(httpClient,
                                         URI.create("https://localhost:" + (runtimeInfo.getHttpsPort() - 1)));
        handlerUnderTest = new FetchNfrFundingByIdentifierHandler(environment, apiClient);

        var projectId = stubber.byProjectIdBadRequest();

        var input = new HandlerRequestBuilder<Void>(dtoObjectMapper)
                        .withPathParameters(Map.of("identifier", Integer.toString(projectId)))
                        .build();

        handlerUnderTest.handleRequest(input, output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));

        var problem = response.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), is(equalTo("Failed to communicate with NFR rest api!")));
    }
}
