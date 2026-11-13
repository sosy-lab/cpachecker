// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PredicateDefinitionRecord.PredicateDefinitionRecordDeserializer;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PredicateDefinitionRecord.PredicateDefinitionRecordSerializer;

/**
 * One wrapped predicate definition of a memory-safety correctness witness in format version 2.4.
 * For example, {@code predicate_definition: {name: sll_seg, parameters: [{name: start, type:
 * "struct sll *"}, {name: end, type: "struct sll *"}], definition: "...", format: ext_c_expression,
 * locations: [{file_name: "./memsafety_sll-blank.c"}]}}.
 *
 * <p>The custom (de)serializers handle the single {@code predicate_definition} wrapper object in
 * each set-content element. Names, C types, and expressions are stored as strings after ordinary
 * YAML scalar decoding; they are not resolved or parsed as C or ACSL.
 *
 * @see <a
 *     href="https://gitlab.com/sosy-lab/benchmarking/sv-witnesses/-/blob/main/user-guide/Witness-Format.md#predicate-definitions-value-of-entry-type">Predicate
 *     definitions in the witness format</a>
 * @see <a
 *     href="https://gitlab.com/sosy-lab/benchmarking/sv-witnesses/-/blob/main/examples/memsafety_sll-blank.witness-2.4.yml">Example
 *     witness</a>
 * @see PredicateDefinitionSetEntry
 */
@JsonDeserialize(using = PredicateDefinitionRecordDeserializer.class)
@JsonSerialize(using = PredicateDefinitionRecordSerializer.class)
public class PredicateDefinitionRecord {

  private static final String PREDICATE_DEF_FIELD_NAME = "predicate_definition";

  /** The predicate's {@code name}. */
  @JsonProperty("name")
  private final String name;

  /**
   * The {@code definition} expression, for example {@code \canAccess(start, sizeof(struct sll))}.
   * Recursive calls, memory operations etc. are preserved without interpretation.
   */
  @JsonProperty("definition")
  private final String definition;

  /**
   * The ordered {@code parameters} array, for example {@code [{name: start, type: "struct sll *"},
   * {name: end, type: "struct sll *"}]}.
   */
  @JsonProperty("parameters")
  private final List<PredicateParameterRecord> parameters;

  @JsonProperty("format")
  private final YAMLWitnessExpressionType format;

  /**
   * The ordered {@code locations} array, for example {@code [{file_name:
   * "./memsafety_sll-blank.c"}]}. Each element is either file-only or a source location with a line
   * and optional column and function. The schema sets no minimum length, although the guide depicts
   * one or more locations; this model adds no count validation.
   */
  @JsonProperty("locations")
  private final List<LocationRecord> locations;

  public PredicateDefinitionRecord(
      @JsonProperty("name") String pName,
      @JsonProperty("definition") String pDefinition,
      @JsonProperty("parameters") List<PredicateParameterRecord> pParameters,
      @JsonProperty("format") YAMLWitnessExpressionType pFormat,
      @JsonProperty("locations") List<LocationRecord> pLocations) {
    name = pName;
    // An empty parameter list may be given as a key without value, which YAML decodes as null
    parameters = pParameters == null ? ImmutableList.of() : ImmutableList.copyOf(pParameters);
    definition = pDefinition;
    format = pFormat;
    locations = ImmutableList.copyOf(pLocations);
  }

  public String getName() {
    return name;
  }

  public List<PredicateParameterRecord> getParameters() {
    return parameters;
  }

  public String getDefinition() {
    return definition;
  }

  public YAMLWitnessExpressionType getFormat() {
    return format;
  }

  public List<LocationRecord> getLocations() {
    return locations;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof PredicateDefinitionRecord pOtherPredicateDefinitionRecord
        && definition.equals(pOtherPredicateDefinitionRecord.definition)
        && name.equals(pOtherPredicateDefinitionRecord.name)
        && locations.equals(pOtherPredicateDefinitionRecord.locations)
        && parameters.equals(pOtherPredicateDefinitionRecord.parameters)
        && format.equals(pOtherPredicateDefinitionRecord.format);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, parameters, definition, locations, format);
  }

  @Override
  public String toString() {
    return PREDICATE_DEF_FIELD_NAME
        + " [name="
        + name
        + ", definition="
        + definition
        + ", parameters=["
        + String.join(
            ", ",
            parameters.stream()
                .map(PredicateParameterRecord::toString)
                .collect(ImmutableList.toImmutableList()))
        + "]"
        + ", format="
        + format
        + ", locations="
        + locations
        + "]";
  }

  public static class PredicateDefinitionRecordDeserializer
      extends JsonDeserializer<PredicateDefinitionRecord> {

    @Override
    public PredicateDefinitionRecord deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
      ObjectMapper mapper = (ObjectMapper) jp.getCodec();
      JsonNode node = mapper.readTree(jp);

      // The node should now be the 'predicate_definition' node. Move one level deeper to its
      // children.
      JsonNode predicateDefinitionNode = node.get(PREDICATE_DEF_FIELD_NAME);
      checkNotNull(predicateDefinitionNode);

      // As for WaypointRecord, delegating back to Jackson would use this deserializer again, so we
      // construct the record manually.
      JavaType parameterListType =
          mapper
              .getTypeFactory()
              .constructCollectionType(List.class, PredicateParameterRecord.class);
      JavaType locationListType =
          mapper.getTypeFactory().constructCollectionType(List.class, LocationRecord.class);
      return new PredicateDefinitionRecord(
          mapper.treeToValue(predicateDefinitionNode.get("name"), String.class),
          mapper.treeToValue(predicateDefinitionNode.get("definition"), String.class),
          mapper.convertValue(predicateDefinitionNode.get("parameters"), parameterListType),
          mapper.treeToValue(
              predicateDefinitionNode.get("format"), YAMLWitnessExpressionType.class),
          mapper.convertValue(predicateDefinitionNode.get("locations"), locationListType));
    }
  }

  public static class PredicateDefinitionRecordSerializer
      extends JsonSerializer<PredicateDefinitionRecord> {

    public PredicateDefinitionRecordSerializer() {}

    @Override
    public void serialize(
        PredicateDefinitionRecord valueToSerialize,
        JsonGenerator gen,
        SerializerProvider serializers)
        throws IOException {

      // Start a wrapper object for "predicate_definition"
      gen.writeStartObject();
      gen.writeFieldName(PREDICATE_DEF_FIELD_NAME);

      // start the actual PredicateDefinitionRecord object
      gen.writeStartObject();
      gen.writeFieldName("name");
      serializers.defaultSerializeValue(valueToSerialize.getName(), gen);

      gen.writeFieldName("parameters");
      serializers.defaultSerializeValue(valueToSerialize.getParameters(), gen);

      gen.writeFieldName("definition");
      serializers.defaultSerializeValue(valueToSerialize.getDefinition(), gen);

      gen.writeFieldName("format");
      serializers.defaultSerializeValue(valueToSerialize.getFormat(), gen);

      gen.writeFieldName("locations");
      serializers.defaultSerializeValue(valueToSerialize.getLocations(), gen);

      // end the PredicateDefinitionRecord object
      gen.writeEndObject();

      // End the wrapper object
      gen.writeEndObject();
    }
  }
}
