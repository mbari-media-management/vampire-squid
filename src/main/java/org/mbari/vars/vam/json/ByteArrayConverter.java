package org.mbari.vars.vam.json;

import com.google.gson.*;

import java.lang.reflect.Type;
import static org.mbari.vars.vam.dao.jpa.ByteArrayConverter.*;

/**
 * @author Brian Schlining
 * @since 2017-03-02T09:54:00
 */
public class ByteArrayConverter
        implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {

    @Override
    public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return decode(json.getAsString());
    }

    @Override
    public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(encode(src));
    }
}