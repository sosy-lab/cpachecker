// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntryV3.InvariantRecordV3Deserializer;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntryV3.InvariantRecordV3Serializer;

@JsonDeserialize(using = InvariantRecordV3Deserializer.class)
@JsonSerialize(using = InvariantRecordV3Serializer.class)
public final class InvariantEntryV3 extends InvariantEntry {

  public InvariantEntryV3(String pString, String pType, String pFormat, LocationRecord pLocation) {
    super(pString, pType, pFormat, pLocation);
  }

  public static class InvariantRecordV3Deserializer extends JsonDeserializer<InvariantEntryV3> {
    @Override
    public InvariantEntryV3 deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
      ObjectMapper mapper = (ObjectMapper) jp.getCodec();
      JsonNode node = mapper.readTree(jp);

      // Delegate the actual object mapping back to Jackson:
      // WaypointRecord result = mapper.treeToValue(waypointNode, WaypointRecord.class);
      // CAVEAT: does not work, since this would use the custom deserializer.
      // Using the original deserializer is apparently very hard.
      // For now just manually construct this
      // (less elegant, but we probably never touch that code again, so it is fine):
      InvariantEntryV3 result =
          new InvariantEntryV3(
              mapper.treeToValue(node.get("value"), String.class),
              mapper.treeToValue(node.get("type"), String.class),
              mapper.treeToValue(node.get("format"), String.class),
              mapper.treeToValue(node.get("location"), LocationRecord.class));

      return result;
    }
  }

  public static class InvariantRecordV3Serializer extends JsonSerializer<InvariantEntryV3> {

    @Override
    public void serialize(InvariantEntryV3 value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {

      // start the actual InvariantEntry object
      gen.writeStartObject();
      gen.writeFieldName("type");
      serializers.defaultSerializeValue(value.getType(), gen);

      gen.writeFieldName("location");
      serializers.defaultSerializeValue(value.getLocation(), gen);

      gen.writeFieldName("value");
      serializers.defaultSerializeValue(value.getValue(), gen);

      gen.writeFieldName("format");
      serializers.defaultSerializeValue(value.getFormat(), gen);

      // End the wrapper object
      gen.writeEndObject();
    }
  }
}
