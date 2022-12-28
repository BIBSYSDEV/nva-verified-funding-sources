package no.sikt.nva.funding.verified.nfr.handlers;

import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.HTTPS;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Locale;
import java.util.stream.Collectors;
import no.sikt.nva.funding.verified.nfr.EnvironmentKeys;
import no.sikt.nva.funding.verified.nfr.client.NfrApiClient;
import no.sikt.nva.funding.verified.nfr.model.Funding;
import no.sikt.nva.funding.verified.nfr.model.PagedSearchResult;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class QueryNfrFundingsHandler extends ApiGatewayHandler<Void, PagedSearchResult<Funding>> {

    private static final URI CONTEXT_URI = URI.create("https://bibsysdev.github.io/src/funding-context.json");
    private static final String ILLEGAL_OFFSET = "Offset must be a zero or positive integer!";
    private static final String ILLEGAL_SIZE = "Size must be a positive integer!";
    public static final String NAME_QUERY_PARAM = "name";
    public static final String TERM_QUERY_PARAM = "term";
    public static final String SIZE_QUERY_PARAM = "size";
    public static final String OFFSET_QUERY_PARAM = "offset";
    public static final String DEFAULT_OFFSET = "0";
    public static final String DEFAULT_SIZE = "10";
    public static final int MINIMUM_SIZE = 1;
    private final transient NfrApiClient apiClient;

    @JacocoGenerated
    public QueryNfrFundingsHandler() {
        this(new Environment(), NfrApiClient.defaultClient());
    }

    public QueryNfrFundingsHandler(Environment environment, NfrApiClient apiClient) {
        super(Void.class, environment);
        this.apiClient = apiClient;
    }

    @Override
    protected PagedSearchResult<Funding> processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        var offset = validateAndGetOffsetFromRequest(requestInfo);
        var size = validateAndGetSizeFromRequest(requestInfo);

        var nameOpt = requestInfo.getQueryParameterOpt(NAME_QUERY_PARAM);

        var apiDomain = environment.readEnv(EnvironmentKeys.API_DOMAIN);
        var basePath = environment.readEnv(EnvironmentKeys.CUSTOM_DOMAIN_NAME_PATH);
        var cristinBasePath = environment.readEnv(EnvironmentKeys.CRISTIN_BASE_PATH);
        var cristinFundingSourcesPath = environment.readEnv(EnvironmentKeys.CRISTIN_FUNDING_SOURCES_PATH);

        PagedSearchResult<Funding> searchResult;
        if (nameOpt.isPresent()) {
            searchResult = queryByLeadName(nameOpt.get(), offset, size, apiDomain, basePath, cristinBasePath,
                                           cristinFundingSourcesPath);
        } else {
            var term = requestInfo.getQueryParameter(TERM_QUERY_PARAM);
            searchResult = queryByTerm(term, offset, size, apiDomain, basePath, cristinBasePath,
                                       cristinFundingSourcesPath);
        }
        return searchResult;
    }

    private int validateAndGetOffsetFromRequest(RequestInfo requestInfo) throws BadRequestException {
        var offsetAsString = requestInfo.getQueryParameterOpt(OFFSET_QUERY_PARAM).orElse(DEFAULT_OFFSET);

        var offset = attempt(() -> Integer.parseInt(offsetAsString))
                         .orElseThrow(failure -> new BadRequestException(ILLEGAL_OFFSET));

        if (offset < 0) {
            throw new BadRequestException(ILLEGAL_OFFSET);
        }

        return offset;
    }

    private int validateAndGetSizeFromRequest(RequestInfo requestInfo) throws BadRequestException {
        var sizeAsString = requestInfo.getQueryParameterOpt(SIZE_QUERY_PARAM).orElse(DEFAULT_SIZE);

        var size = attempt(() -> Integer.parseInt(sizeAsString))
                       .orElseThrow(failure -> new BadRequestException(ILLEGAL_SIZE));

        if (size < MINIMUM_SIZE) {
            throw new BadRequestException(ILLEGAL_SIZE);
        }

        return size;
    }

    private PagedSearchResult<Funding> queryByLeadName(String name,
                                                       int offset,
                                                       int size,
                                                       String apiDomain,
                                                       String basePath,
                                                       String cristinBasePath,
                                                       String cristinFundingSourcesPath)
        throws BadGatewayException {

        var searchResult = apiClient.query(name, offset, size);

        var hits = searchResult.getHits().stream()
                       .filter(funding -> funding.getLeadName().toLowerCase(Locale.ROOT).equals(
                           name.toLowerCase(Locale.ROOT)))
                       .map(funding -> funding.asFunding(apiDomain, basePath, cristinBasePath,
                                                         cristinFundingSourcesPath))
                       .collect(Collectors.toList());

        var baseUri = new UriWrapper(HTTPS, apiDomain).addChild(basePath, "nfr").getUri();
        return new PagedSearchResult<>(CONTEXT_URI,
                                       baseUri,
                                       offset, size, hits.size(), hits);
    }

    private PagedSearchResult<Funding> queryByTerm(String term,
                                                   int offset,
                                                   int size,
                                                   String apiDomain,
                                                   String basePath,
                                                   String cristinBasePath,
                                                   String cristinFundingSourcesPath)
        throws BadGatewayException {

        var searchResult = apiClient.query(term, offset, size);

        var hits = searchResult.getHits().stream()
                       .map(funding -> funding.asFunding(apiDomain, basePath, cristinBasePath,
                                                         cristinFundingSourcesPath))
                       .collect(Collectors.toList());

        var baseUri = new UriWrapper(HTTPS, apiDomain).addChild(basePath, "nfr").getUri();
        return new PagedSearchResult<>(CONTEXT_URI,
                                       baseUri,
                                       offset, size, searchResult.getTotalHits(), hits);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, PagedSearchResult<Funding> output) {
        return HttpURLConnection.HTTP_OK;
    }
}
