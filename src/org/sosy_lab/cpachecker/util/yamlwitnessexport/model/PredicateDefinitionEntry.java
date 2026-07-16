// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import static com.google.common.base.Preconditions.checkNotNull;

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
import com.google.common.base.Objects;
import com.google.errorprone.annotations.Immutable;
import java.io.IOException;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PredicateDefinitionEntry.PredicateEntryDeserializer;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PredicateDefinitionEntry.PredicateEntrySerializer;

/**
 * Predicates are defined in a predicate definitions set for a specific program location (due to C
 * allowing multiple type definitions in distinct locations with the same name as long as they don't
 * conflict) and can be used from e.g. in invariants in concrete locations. The predicates
 * themselves are defined using ACSL expressions (although the format in the witness format is
 * either c_expression or ext_c_expression) and are called using parameters defined in 'parameters'.
 * For example:
 *
 * <pre>{@code
 * - predicate_definition:
 *     name: "pred_sll"
 *     parameters:
 *       - name: start
 *         type: "struct sll *"
 *       - name: end
 *         type: "struct sll *"
 *     definition: "(start != end)"
 *     format: "ext_c_expression"
 *     locations:
 *       - file_name: "./example.c"
 *         line: 25
 *         column: 3
 *         function: "create"
 * }</pre>
 */
@Immutable
@JsonDeserialize(using = PredicateEntryDeserializer.class)
@JsonSerialize(using = PredicateEntrySerializer.class)
public class PredicateDefinitionEntry extends AbstractInvariantEntry {

  private static final String PREDICATE_DEF_FIELD_NAME = "predicate_definition";

  @JsonProperty("name")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final String name;

  @JsonProperty("parameters")
  private final String parameters;

  @JsonProperty("definition")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final String definition;

  @JsonProperty("definition")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final LocationRecord location;

  public PredicateDefinitionEntry(
      @JsonProperty("name") String pName,
      @JsonProperty("params") String pParameters,
      @JsonProperty("definition") String pDefinition,
      @JsonProperty("format") YAMLWitnessExpressionType pFormat,
      @JsonProperty("locations") LocationRecord pLocation) {
    super(PREDICATE_DEF_FIELD_NAME, pFormat);
    name = pName;
    parameters = pParameters;
    definition = pDefinition;
    location = pLocation;
  }

  public String getName() {
    return name;
  }

  public LocationRecord getLocation() {
    return location;
  }

  public String getParameters() {
    return parameters;
  }

  public String getDefinition() {
    return definition;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof PredicateDefinitionEntry pPredicateDefinitionEntry
        && definition.equals(pPredicateDefinitionEntry.definition)
        && name.equals(pPredicateDefinitionEntry.name)
        && location.equals(pPredicateDefinitionEntry.location)
        && parameters.equals(pPredicateDefinitionEntry.parameters)
        && super.equals(pPredicateDefinitionEntry);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        name, parameters, definition, location, super.getFormat(), super.getType());
  }

  @Override
  public String toString() {
    return PREDICATE_DEF_FIELD_NAME
        + " [name="
        + name
        + ", parameters="
        + parameters
        + ", definition="
        + definition
        + ", format="
        + format
        + ", location="
        + location
        + ", format="
        + getFormat()
        + "]";
  }

  public static class PredicateEntryDeserializer
      extends JsonDeserializer<PredicateDefinitionEntry> {
    @Override
    public PredicateDefinitionEntry deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
      ObjectMapper mapper = (ObjectMapper) jp.getCodec();
      JsonNode node = mapper.readTree(jp);

      // The node should now be the 'predicate_definition' node.
      JsonNode predicateNode = node.get(PREDICATE_DEF_FIELD_NAME);
      checkNotNull(predicateNode);
      // Move one level deeper to its children.

      return new PredicateDefinitionEntry(
          mapper.treeToValue(predicateNode.get("name"), String.class),
          mapper.treeToValue(predicateNode.get("parameters"), String.class),
          mapper.treeToValue(predicateNode.get("definition"), String.class),
          mapper.treeToValue(predicateNode.get("format"), YAMLWitnessExpressionType.class),
          mapper.treeToValue(predicateNode.get("locations"), LocationRecord.class));
    }
  }

  public static class PredicateEntrySerializer extends JsonSerializer<PredicateDefinitionEntry> {

    @Override
    public void serialize(
        PredicateDefinitionEntry valueToSerialize,
        JsonGenerator gen,
        SerializerProvider serializers)
        throws IOException {

      // Start '-predicate_definition' wrapper object
      gen.writeStartObject();
      gen.writeFieldName(PREDICATE_DEF_FIELD_NAME);

      // start the actual predicate_definition object
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
      serializers.defaultSerializeValue(valueToSerialize.getLocation(), gen);

      // end the object
      gen.writeEndObject();

      // End the wrapper object
      gen.writeEndObject();
    }
  }
}
