// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonInclude;
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
import com.google.errorprone.annotations.Immutable;
import java.io.IOException;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.FunctionContractEntry.FunctionContractRecordDeserializer;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.FunctionContractEntry.FunctionContractRecordSerializer;

@Immutable
@JsonDeserialize(using = FunctionContractRecordDeserializer.class)
@JsonSerialize(using = FunctionContractRecordSerializer.class)
public non-sealed class FunctionContractEntry implements CorrectnessWitnessSetElementEntry {

  @SuppressWarnings("unused")
  private static final String FUNCTION_CONTRACT_IDENTIFIER = "function_contract";

  @JsonProperty("location")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final LocationRecord location;

  @JsonProperty("ensures")
  private final String ensures;

  @JsonProperty("requires")
  private final String requires;

  @JsonProperty("format")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final YAMLWitnessExpressionType format;

  public FunctionContractEntry(
      @JsonProperty("ensures") String pEnsures,
      @JsonProperty("requires") String pRequires,
      @JsonProperty("format") YAMLWitnessExpressionType pFormat,
      @JsonProperty("location") LocationRecord pLocation) {
    location = pLocation;
    ensures = pEnsures;
    requires = pRequires;
    format = pFormat;
  }

  public LocationRecord getLocation() {
    return location;
  }

  public String getEnsures() {
    return ensures;
  }

  public String getRequires() {
    return requires;
  }

  public YAMLWitnessExpressionType getFormat() {
    return format;
  }

  public static class FunctionContractRecordDeserializer
      extends JsonDeserializer<FunctionContractEntry> {
    @Override
    public FunctionContractEntry deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
      ObjectMapper mapper = (ObjectMapper) jp.getCodec();
      JsonNode node = mapper.readTree(jp);

      // The node should now be the 'invariant' node. Move one level deeper to its children.
      JsonNode invariantNode = node.get("invariant");
      assert invariantNode != null;

      // Delegate the actual object mapping back to Jackson:
      // WaypointRecord result = mapper.treeToValue(waypointNode, WaypointRecord.class);
      // CAVEAT: does not work, since this would use the custom deserializer.
      // Using the original deserializer is apparently very hard.
      // For now just manually construct this
      // (less elegant, but we probably never touch that code again, so it is fine):
      FunctionContractEntry result =
          new FunctionContractEntry(
              mapper.treeToValue(node.get("ensures"), String.class),
              mapper.treeToValue(node.get("requires"), String.class),
              mapper.treeToValue(node.get("format"), YAMLWitnessExpressionType.class),
              mapper.treeToValue(node.get("location"), LocationRecord.class));

      return result;
    }
  }

  public static class FunctionContractRecordSerializer
      extends JsonSerializer<FunctionContractEntry> {

    @Override
    public void serialize(
        FunctionContractEntry value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {

      // Start a wrapper object for "function_contract"
      gen.writeStartObject();
      gen.writeFieldName("invariant");

      // start the actual InvariantEntry object
      gen.writeStartObject();
      gen.writeFieldName("type");
      serializers.defaultSerializeValue(FUNCTION_CONTRACT_IDENTIFIER, gen);

      gen.writeFieldName("location");
      serializers.defaultSerializeValue(value.location, gen);

      gen.writeFieldName("requires");
      serializers.defaultSerializeValue(value.getRequires(), gen);

      gen.writeFieldName("ensures");
      serializers.defaultSerializeValue(value.getEnsures(), gen);

      gen.writeFieldName("format");
      serializers.defaultSerializeValue(value.getFormat(), gen);

      // end the object
      gen.writeEndObject();

      // End the wrapper object
      gen.writeEndObject();
    }
  }
}
