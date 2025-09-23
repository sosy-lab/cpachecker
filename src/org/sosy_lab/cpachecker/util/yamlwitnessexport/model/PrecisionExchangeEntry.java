// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.util.Arrays;
import java.util.List;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionExchangeEntry.PrecisionExchangeEntryDeserializer;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionExchangeEntry.PrecisionExchangeEntrySerializer;

@JsonDeserialize(using = PrecisionExchangeEntryDeserializer.class)
@JsonSerialize(using = PrecisionExchangeEntrySerializer.class)
public record PrecisionExchangeEntry(
    @JsonProperty("format") YAMLWitnessExpressionType format,
    @JsonProperty("scope") PrecisionScope scope,
    @JsonProperty("type") PrecisionType type,
    @JsonProperty("values") List<String> values) {

  public static class PrecisionExchangeEntryDeserializer
      extends JsonDeserializer<PrecisionExchangeEntry> {
    @Override
    public PrecisionExchangeEntry deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
      ObjectMapper mapper = (ObjectMapper) jp.getCodec();
      JsonNode node = mapper.readTree(jp);

      // The node should now be the 'invariant' node. Move one level deeper to its children.
      JsonNode invariantNode = node.get("precision");
      assert invariantNode != null;

      // Delegate the actual object mapping back to Jackson:
      // WaypointRecord result = mapper.treeToValue(waypointNode, WaypointRecord.class);
      // CAVEAT: does not work, since this would use the custom deserializer.
      // Using the original deserializer is apparently very hard.
      // For now just manually construct this
      // (less elegant, but we probably never touch that code again, so it is fine):
      PrecisionExchangeEntry result =
          new PrecisionExchangeEntry(
              mapper.treeToValue(invariantNode.get("format"), YAMLWitnessExpressionType.class),
              mapper.treeToValue(invariantNode.get("scope"), PrecisionScope.class),
              mapper.treeToValue(invariantNode.get("type"), PrecisionType.class),
              // We can't use generics for the types at runtime
              Arrays.asList(mapper.treeToValue(invariantNode.get("values"), String[].class)));

      return result;
    }
  }

  public static class PrecisionExchangeEntrySerializer
      extends JsonSerializer<PrecisionExchangeEntry> {

    @Override
    public void serialize(
        PrecisionExchangeEntry value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {

      // Start a wrapper object for "predicate"
      gen.writeStartObject();
      gen.writeFieldName("precision");

      // start the actual InvariantEntry object
      gen.writeStartObject();
      gen.writeFieldName("format");
      serializers.defaultSerializeValue(value.format(), gen);

      gen.writeFieldName("scope");
      serializers.defaultSerializeValue(value.scope(), gen);

      gen.writeFieldName("type");
      serializers.defaultSerializeValue(value.type(), gen);

      gen.writeFieldName("values");
      serializers.defaultSerializeValue(value.values(), gen);
      // end the InvariantEntry object
      gen.writeEndObject();

      // End the wrapper object
      gen.writeEndObject();
    }
  }
}
