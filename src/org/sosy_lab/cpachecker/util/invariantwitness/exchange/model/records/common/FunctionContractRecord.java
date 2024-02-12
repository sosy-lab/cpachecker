// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common;

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
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.FunctionContractRecord.FunctionContractRecordDeserializer;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.FunctionContractRecord.FunctionContractRecordSerializer;
import org.sosy_lab.cpachecker.util.witnessv2export.WitnessesV2AndUpDataTypes.ExpressionType;

@Immutable
@JsonDeserialize(using = FunctionContractRecordDeserializer.class)
@JsonSerialize(using = FunctionContractRecordSerializer.class)
public class FunctionContractRecord implements SetElementRecord {

  @SuppressWarnings("unused")
  private static final String FUNCTION_CONTRACT_IDENTIFIER = "function_contract";

  @JsonProperty("location")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final LocationRecord location;

  @JsonProperty("ensures")
  private final EnsuresRecord ensures;

  @JsonProperty("requires")
  private final RequiresRecord requires;

  @JsonProperty("format")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final ExpressionType format;

  public FunctionContractRecord(
      @JsonProperty("ensures") EnsuresRecord pEnsures,
      @JsonProperty("requires") RequiresRecord pRequires,
      @JsonProperty("format") ExpressionType pFormat,
      @JsonProperty("location") LocationRecord pLocation) {
    location = pLocation;
    ensures = pEnsures;
    requires = pRequires;
    format = pFormat;
  }

  public LocationRecord getLocation() {
    return location;
  }

  public EnsuresRecord getEnsures() {
    return ensures;
  }

  public RequiresRecord getRequires() {
    return requires;
  }

  public ExpressionType getFormat() {
    return format;
  }

  public static class FunctionContractRecordDeserializer
      extends JsonDeserializer<FunctionContractRecord> {
    @Override
    public FunctionContractRecord deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
      ObjectMapper mapper = (ObjectMapper) jp.getCodec();
      JsonNode node = mapper.readTree(jp);

      // Delegate the actual object mapping back to Jackson:
      // WaypointRecord result = mapper.treeToValue(waypointNode, WaypointRecord.class);
      // CAVEAT: does not work, since this would use the custom deserializer.
      // Using the original deserializer is apparently very hard.
      // For now just manually construct this
      // (less elegant, but we probably never touch that code again, so it is fine):
      FunctionContractRecord result =
          new FunctionContractRecord(
              mapper.treeToValue(node.get("ensures"), EnsuresRecord.class),
              mapper.treeToValue(node.get("requires"), RequiresRecord.class),
              mapper.treeToValue(node.get("format"), ExpressionType.class),
              mapper.treeToValue(node.get("location"), LocationRecord.class));

      return result;
    }
  }

  public static class FunctionContractRecordSerializer
      extends JsonSerializer<FunctionContractRecord> {

    @Override
    public void serialize(
        FunctionContractRecord value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {

      // start the actual InvariantRecord object
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
    }
  }
}
