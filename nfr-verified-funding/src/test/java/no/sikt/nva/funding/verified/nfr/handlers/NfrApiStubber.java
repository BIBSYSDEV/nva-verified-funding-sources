package no.sikt.nva.funding.verified.nfr.handlers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import no.sikt.nva.funding.verified.nfr.client.model.NfrFunding;
import no.sikt.nva.funding.verified.nfr.client.model.NfrFundingSearchResult;

public class NfrApiStubber {

    private final Map<Integer, NfrFunding> exactMatchByProjectId = new ConcurrentHashMap<>();

    public NfrApiStubber() {
        // no-op
    }

    public int byProjectIdSingleMatch(int totalHits) {
        var projectId = randomInteger();

        var responseObject = new NfrFundingSearchResult(totalHits, 0, 1,
                                                        generateSingleHitMultipleCandidates(projectId,
                                                                                            totalHits - 1));

        var url = "/search?query=" + projectId + "&from=0&size=1";

        attempt(() -> stubFor(get(url)
                                  .willReturn(aResponse()
                                                  .withHeader("Content-Type",
                                                              "application/json;charset=utf-8")
                                                  .withBody(dtoObjectMapper.writeValueAsString(responseObject))
                                                  .withStatus(HttpURLConnection.HTTP_OK)))).orElseThrow();
        return projectId;
    }

    public int byProjectIdNoExactMatch(int totalHits) {
        var projectId = randomInteger();

        var responseObject = new NfrFundingSearchResult(totalHits, 0, 1,
                                                        generateNoHitsMultipleCandidates(projectId,
                                                                                         totalHits));

        var url = "/search?query=" + projectId + "&from=0&size=1";

        attempt(() -> stubFor(get(url)
                                  .willReturn(aResponse()
                                                  .withHeader("Content-Type",
                                                              "application/json;charset=utf-8")
                                                  .withBody(dtoObjectMapper.writeValueAsString(responseObject))
                                                  .withStatus(HttpURLConnection.HTTP_OK)))).orElseThrow();
        return projectId;
    }

    public int byProjectIdBadRequest() {
        var projectId = randomInteger();

        var url = "/search?query=" + projectId + "&from=0&size=1";

        attempt(() -> stubFor(get(url)
                                  .willReturn(aResponse()
                                                  .withHeader("Content-Type",
                                                              "application/json;charset=utf-8")
                                                  .withBody(dtoObjectMapper.writeValueAsString("{}"))
                                                  .withStatus(HttpURLConnection.HTTP_BAD_REQUEST)))).orElseThrow();
        return projectId;
    }

    public NfrFunding getMatchingEntryFromNfr(int projectId) {
        return exactMatchByProjectId.get(projectId);
    }

    private List<NfrFunding> generateSingleHitMultipleCandidates(int projectId, int hitsCount) {
        List<NfrFunding> hits = new ArrayList<>();

        var match = randomFundingWithProjectId(projectId);
        exactMatchByProjectId.put(projectId, match);
        hits.add(match);
        for (int count = 0; count < hitsCount; count++) {
            hits.add(randomFundingWithDifferentProjectId(projectId));
        }
        return hits;
    }

    private List<NfrFunding> generateNoHitsMultipleCandidates(int projectId, int noAdditionalHits) {
        List<NfrFunding> hits = new ArrayList<>();
        for (int count = 0; count < noAdditionalHits; count++) {
            hits.add(randomFundingWithDifferentProjectId(projectId));
        }
        return hits;
    }

    private NfrFunding randomFundingWithProjectId(int projectId) {
        var activeFrom = randomInstant();
        var activeTo = randomInstant(activeFrom);

        return new NfrFunding(projectId,
                              activeFrom,
                              activeTo,
                              randomString(),
                              Map.of("title", randomString()),
                              Map.of("title", randomString()));
    }

    private NfrFunding randomFundingWithDifferentProjectId(int projectId) {
        int newProjectId;
        do {
            newProjectId = randomInteger();
        } while (projectId == newProjectId);

        return randomFundingWithProjectId(newProjectId);
    }
}
