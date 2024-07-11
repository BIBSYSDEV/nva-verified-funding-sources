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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import no.sikt.nva.funding.verified.nfr.client.model.NfrFunding;
import no.sikt.nva.funding.verified.nfr.client.model.NfrFundingSearchResult;
import nva.commons.core.paths.UriWrapper;

public class NfrApiStubber {

    private static final String SPACE = " ";
    private final Map<Integer, NfrFunding> exactMatchByProjectId = new ConcurrentHashMap<>();
    private final Map<String, List<NfrFunding>> exactMatchesByLeadName = new ConcurrentHashMap<>();

    public NfrApiStubber() {
        // no-op
    }

    public int byProjectIdSingleMatch(int totalHits) {
        var projectId = randomInteger();

        var responseObject = new NfrFundingSearchResult(totalHits, 0, 10,
                                                        generateSingleHitMultipleCandidates(projectId,
                                                                                            totalHits - 1));

        var url = "/search?query=" + projectId + "&from=0&size=10";

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

        var responseObject = new NfrFundingSearchResult(totalHits, 0, 10,
                                                        generateNoHitsMultipleCandidates(projectId,
                                                                                         totalHits));

        var url = "/search?query=" + projectId + "&from=0&size=10";

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

        var url = "/search?query=" + projectId + "&from=0&size=10";

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

    public List<NfrFunding> getMatchingEntriesByLeadName(String leadName) {
        return exactMatchesByLeadName.get(leadName);
    }

    public String withMatchingLeadNameAsMostRelevantHit(int numberOfMatchesByName, int numberOfAdditionalMatches,
                                                        int from, int size) {
        var leadName = randomLeadName();

        var matches = randomFundingsWithLeadName(leadName, numberOfMatchesByName);
        this.exactMatchesByLeadName.put(leadName, new ArrayList<>(matches));
        matches.addAll(randomFundingsWithDifferentLeadName(leadName, numberOfAdditionalMatches));

        var totalHits = numberOfMatchesByName + numberOfAdditionalMatches;
        var responseObject = new NfrFundingSearchResult(totalHits, from, size, matches);

        var url = "/search?query=" +  URLEncoder.encode(leadName, StandardCharsets.UTF_8)
                                         .replace("+", "%2B")
                                         .replace("&", "%26")
                                         .replace(",","%2C")
                  + "&from=" + from
                  + "&size=" + size;

        attempt(() -> stubFor(get(url)
                                  .willReturn(aResponse()
                                                  .withHeader("Content-Type",
                                                              "application/json;charset=utf-8")
                                                  .withBody(dtoObjectMapper.writeValueAsString(responseObject))
                                                  .withStatus(HttpURLConnection.HTTP_OK)))).orElseThrow();

        return leadName;
    }

    public List<NfrFunding> withRandomMatches(String term, int count, int from, int size) {
        var matches = new ArrayList<NfrFunding>(count);
        for (int counter = 0; counter < count; counter++) {
            matches.add(randomFunding(randomInteger(), randomString()));
        }

        var responseObject = new NfrFundingSearchResult(count, from, size, matches);

        var url = "/search?query=" + URLEncoder.encode(term, StandardCharsets.UTF_8)
                  + "&from=" + from
                  + "&size=" + size;

        attempt(() -> stubFor(get(url)
                                  .willReturn(aResponse()
                                                  .withHeader("Content-Type",
                                                              "application/json;charset=utf-8")
                                                  .withBody(dtoObjectMapper.writeValueAsString(responseObject))
                                                  .withStatus(HttpURLConnection.HTTP_OK)))).orElseThrow();

        return matches;
    }

    private List<NfrFunding> randomFundingsWithLeadName(String leadName, int numberOfFundings) {
        var fundings = new ArrayList<NfrFunding>(numberOfFundings);

        for (int counter = 0; counter < numberOfFundings; counter++) {
            fundings.add(randomFundingWithLeadName(leadName));
        }
        return fundings;
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
        return randomFunding(projectId, randomLeadName());
    }

    private String randomLeadName() {
        return String.join(SPACE, randomString(), randomString());
    }

    private NfrFunding randomFundingWithLeadName(String leadName) {
        return randomFunding(randomInteger(), leadName);
    }

    private NfrFunding randomFunding(int projectId, String leadName) {
        var activeFrom = randomInstant();
        var activeTo = randomInstant(activeFrom);

        return new NfrFunding(projectId,
                              activeFrom,
                              activeTo,
                              leadName,
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

    private List<NfrFunding> randomFundingsWithDifferentLeadName(String leadName, int numberOfFundings) {
        var fundings = new ArrayList<NfrFunding>(numberOfFundings);
        for (int counter = 0; counter < numberOfFundings; counter++) {
            fundings.add(randomFundingWithDifferentLeadName(leadName));
        }
        return fundings;
    }

    private NfrFunding randomFundingWithDifferentLeadName(String leadName) {
        String newLeadName;
        do {
            newLeadName = randomLeadName();
        } while (leadName.equals(newLeadName));

        return randomFundingWithLeadName(newLeadName);
    }
}
