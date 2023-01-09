package no.sikt.nva.funding.verified.nfr.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Instant;
import java.util.Map;

public class Funding {

    private static final String TYPE_FIELD = "type";
    private static final String SOURCE_FIELD = "source";
    private static final String ID_FIELD = "id";
    private static final String IDENTIFIER_FIELD = "identifier";
    private static final String NAME_FIELD = "name";
    private static final String LEAD_FIELD = "lead";
    private static final String ACTIVE_FROM_FIELD = "activeFrom";
    private static final String ACTIVE_TO_FIELD = "activeTo";

    public static final String LANGUAGE_EN = "en";
    public static final String LANGUAGE_NB = "nb";

    @JsonProperty(TYPE_FIELD)
    @SuppressWarnings("unused")
    private static final String type = "Funding";
    private final URI source;
    @JsonProperty(ID_FIELD)
    private final URI id;
    @JsonProperty(IDENTIFIER_FIELD)
    private final String identifier;
    @JsonProperty(NAME_FIELD)
    private final Map<String, String> name;
    @JsonProperty(LEAD_FIELD)
    private final String lead;
    @JsonProperty(ACTIVE_FROM_FIELD)
    private final Instant activeFrom;
    @JsonProperty(ACTIVE_TO_FIELD)
    private final Instant activeTo;

    @JsonCreator
    public Funding(@JsonProperty(SOURCE_FIELD) URI source,
                   @JsonProperty(ID_FIELD) URI id,
                   @JsonProperty(IDENTIFIER_FIELD) String identifier,
                   @JsonProperty(NAME_FIELD) Map<String, String> name,
                   @JsonProperty(LEAD_FIELD) String lead,
                   @JsonProperty(ACTIVE_FROM_FIELD) Instant activeFrom,
                   @JsonProperty(ACTIVE_TO_FIELD) Instant activeTo) {
        this.source = source;
        this.id = id;
        this.identifier = identifier;
        this.name = name;
        this.lead = lead;
        this.activeFrom = activeFrom;
        this.activeTo = activeTo;
    }

    public URI getSource() {
        return source;
    }

    public URI getId() {
        return id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Map<String, String> getName() {
        return name;
    }

    public String getLead() {
        return lead;
    }

    public Instant getActiveFrom() {
        return activeFrom;
    }

    public Instant getActiveTo() {
        return activeTo;
    }
}
