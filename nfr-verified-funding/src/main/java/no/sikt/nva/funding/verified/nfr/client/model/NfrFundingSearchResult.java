package no.sikt.nva.funding.verified.nfr.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import nva.commons.core.JacocoGenerated;

public final class NfrFundingSearchResult {
    @JsonProperty("totalHits")
    private final int totalHits;
    @JsonProperty("from")
    private final int from;
    @JsonProperty("size")
    private final int size;
    @JsonProperty("hits")
    private final List<NfrFunding> hits;

    @JsonCreator
    public NfrFundingSearchResult(@JsonProperty("totalHits") int totalHits,
                                  @JsonProperty("from") int from,
                                  @JsonProperty("size") int size,
                                  @JsonProperty("hits") List<NfrFunding> hits) {
        this.totalHits = totalHits;
        this.from = from;
        this.size = size;
        this.hits = Collections.unmodifiableList(hits);
    }

    public int getTotalHits() {
        return totalHits;
    }

    public int getFrom() {
        return from;
    }

    public int getSize() {
        return size;
    }

    public List<NfrFunding> getHits() {
        return hits;
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return "NfrFundingSearchResult{"
               + "totalHits=" + totalHits
               + ", from=" + from
               + ", size=" + size
               + ", hits=" + hits
               + '}';
    }
}
