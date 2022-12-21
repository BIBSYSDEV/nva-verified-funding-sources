package no.sikt.nva.funding.verified.nfr.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.function.Supplier;
import no.sikt.nva.funding.verified.nfr.EnvironmentKeys;
import no.sikt.nva.funding.verified.nfr.client.NfrApiClient;
import no.sikt.nva.funding.verified.nfr.model.Funding;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class FetchNfrFundingByIdentifierHandler extends ApiGatewayHandler<Void, Funding> {

    private static final String IDENTIFIER_PATH_PARAM_NAME = "identifier";

    private final transient NfrApiClient apiClient;

    @JacocoGenerated
    public FetchNfrFundingByIdentifierHandler() {
        this(new Environment(), NfrApiClient.defaultClient());
    }

    public FetchNfrFundingByIdentifierHandler(Environment environment, NfrApiClient apiClient) {
        super(Void.class, environment);
        this.apiClient = apiClient;
    }

    @Override
    protected Funding processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        var identifier = Integer.parseInt(requestInfo.getPathParameter(IDENTIFIER_PATH_PARAM_NAME));
        var funding = apiClient.fetchByProjectId(identifier);
        var apiDomain = environment.readEnv(EnvironmentKeys.API_DOMAIN);
        var basePath = environment.readEnv(EnvironmentKeys.CUSTOM_DOMAIN_NAME_PATH);
        return funding.orElseThrow(fundingNotFound(identifier)).asFunding(apiDomain, basePath);
    }

    private Supplier<NotFoundException> fundingNotFound(int identifier) {
        return () -> new NotFoundException("Funding with identifier " + identifier + " not "
                                           + "found!");
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Funding output) {
        return HttpURLConnection.HTTP_OK;
    }
}
