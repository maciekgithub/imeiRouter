package pl.oragne.imei.hub;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.MoreObjects;

public class JsonRawResponse {
    private final String json;

    public JsonRawResponse(String json) {
        this.json = json;
    }

    @JsonValue
    @JsonRawValue
    public String getJson() {
        return json;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("json", json).toString();
    }
}