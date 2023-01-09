package no.sikt.nva.funding.verified.nfr.client.model;

import static no.sikt.nva.funding.verified.nfr.model.Funding.LANGUAGE_EN;
import static no.sikt.nva.funding.verified.nfr.model.Funding.LANGUAGE_NB;
import static nva.commons.core.paths.UriWrapper.HTTPS;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import no.sikt.nva.funding.verified.nfr.model.Funding;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public final class NfrFunding {

    private static final String TITLE_METADATA_KEY = "title";
    private static final String NFR_SOURCE = "NFR";

    @JsonProperty("projectId")
    private final int projectId;
    @JsonProperty("activeFrom")
    private final Instant activeFrom;
    @JsonProperty("activeTo")
    private final Instant activeTo;
    @JsonProperty("leadName")
    private final String leadName;
    @JsonProperty("english")
    private final Map<String, String> englishMetadata;
    @JsonProperty("norwegian")
    private final Map<String, String> norwegianMetadata;

    @JsonCreator
    public NfrFunding(@JsonProperty("projectId") int projectId,
                      @JsonProperty("activeFrom") Instant activeFrom,
                      @JsonProperty("activeTo") Instant activeTo,
                      @JsonProperty("leadName") String leadName,
                      @JsonProperty("english") Map<String, String> englishMetadata,
                      @JsonProperty("norwegian") Map<String, String> norwegianMetadata) {
        this.projectId = projectId;
        this.activeFrom = activeFrom;
        this.activeTo = activeTo;
        this.leadName = leadName;
        this.englishMetadata = Collections.unmodifiableMap(englishMetadata);
        this.norwegianMetadata = Collections.unmodifiableMap(norwegianMetadata);
    }

    public int getProjectId() {
        return projectId;
    }

    public Instant getActiveFrom() {
        return activeFrom;
    }

    public Instant getActiveTo() {
        return activeTo;
    }

    public String getLeadName() {
        return leadName;
    }

    public Map<String, String> getEnglishMetadata() {
        return englishMetadata;
    }

    public Map<String, String> getNorwegianMetadata() {
        return norwegianMetadata;
    }

    public Funding asFunding(final String apiDomain, final String basePath, final String cristinBasePath,
                             final String cristinFundingSourcesPath) {
        var identifier = Integer.toString(projectId);
        var id = new UriWrapper(HTTPS, apiDomain).addChild(basePath, "nfr", identifier).getUri();
        var name = new ConcurrentHashMap<String, String>();
        if (englishMetadata.containsKey(TITLE_METADATA_KEY)) {
            name.put(LANGUAGE_EN, englishMetadata.get(TITLE_METADATA_KEY));
        }
        if (norwegianMetadata.containsKey(TITLE_METADATA_KEY)) {
            name.put(LANGUAGE_NB, norwegianMetadata.get(TITLE_METADATA_KEY));
        }
        var source = new UriWrapper(HTTPS, apiDomain)
                         .addChild(cristinBasePath, cristinFundingSourcesPath, NFR_SOURCE)
                         .getUri();

        return new Funding(source,
                           id,
                           Integer.toString(projectId),
                           name,
                           getLeadName(),
                           getActiveFrom(),
                           getActiveTo());
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return "Funding{"
               + "projectId=" + projectId
               + ", activeFrom=" + activeFrom
               + ", activeTo=" + activeTo
               + ", leadName='" + leadName + '\''
               + ", englishMetadata=" + englishMetadata
               + ", norwegianMetadata=" + norwegianMetadata
               + '}';
    }
}
