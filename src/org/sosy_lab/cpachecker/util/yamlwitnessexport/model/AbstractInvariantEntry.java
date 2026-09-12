// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInvariantEntry.AbstractInvariantEntryDelegateDeserializer;

/**
 * A correctness witness contains a list of `invariant_set`s. Each of them in turn contains multiple
 * `invariant` entries in their content. This interface is used to represent which entries can be
 * exported as an `invariant` inside the `contents` of an `invariant_set`.
 */
@JsonDeserialize(using = AbstractInvariantEntryDelegateDeserializer.class)
public abstract class AbstractInvariantEntry extends AbstractInformationRecord {
  public AbstractInvariantEntry(
      @JsonProperty("type") String pType,
      @JsonProperty("format") YAMLWitnessExpressionType pFormat) {
    super(pType, pFormat);
  }

  public static class AbstractInvariantEntryDelegateDeserializer
      extends JsonDeserializer<AbstractInvariantEntry> {

    @Override
    public AbstractInvariantEntry deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
      ObjectMapper mapper = (ObjectMapper) jp.getCodec();
      JsonNode root = mapper.readTree(jp);

      // An invariant is wrapped in "invariant" and a function contract in "contract", with the
      // "type" inside. Older witnesses also wrap contracts in "invariant".
      JsonNode contentNode = root.get(FunctionContractEntry.CONTRACT_KEY);
      if (contentNode == null) {
        contentNode = root.get(FunctionContractEntry.LEGACY_CONTRACT_KEY);
      }
      if (contentNode != null) {
        String type = contentNode.get("type").asText();

        // Use the type to determine the actual class to deserialize into
        Class<? extends AbstractInvariantEntry> targetClass = getClassForType(type);
        return mapper.treeToValue(root, targetClass);
      }

      // Fallback if neither wrapper nor "type" is found
      throw new IOException("An invariant should always have a type");
    }

    private Class<? extends AbstractInvariantEntry> getClassForType(String type) {
      return switch (type) {
        case "function_contract" -> FunctionContractEntry.class;
        case "loop_invariant",
            "location_invariant",
            "loop_transition_invariant",
            "location_transition_invariant" ->
            InvariantEntry.class;
        default -> throw new IllegalArgumentException("Unknown invariant type: " + type);
      };
    }
  }
}
