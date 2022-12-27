package no.sikt.nva.funding.verified.nfr.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Map;

public class Funding {

    private static final String TYPE_FIELD = "type";
    private static final String SOURCE_FIELD = "source";
    private static final String ID_FIELD = "id";
    private static final String IDENTIFIER_FIELD = "identifier";
    private static final String NAME_FIELD = "name";

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

    @JsonCreator
    public Funding(@JsonProperty(SOURCE_FIELD) URI source,
                   @JsonProperty(ID_FIELD) URI id,
                   @JsonProperty(IDENTIFIER_FIELD) String identifier,
                   @JsonProperty(NAME_FIELD) Map<String, String> name) {
        this.source = source;
        this.id = id;
        this.identifier = identifier;
        this.name = name;
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
}
