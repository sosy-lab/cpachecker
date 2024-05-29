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
 * A correctness witness contains multiple entries in its content, this interface is used to
 * represent which entries can be exported inside a set of a correctness witness.
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

      // Assuming the invariant field is named "invariant" and "type" is inside it
      JsonNode invariantNode = root.get("invariant");
      if (invariantNode != null) {
        String invariantType = invariantNode.get("type").asText();

        // Use the type to determine the actual class to deserialize into
        Class<? extends AbstractInvariantEntry> targetClass = getClassForType(invariantType);
        return mapper.treeToValue(root, targetClass);
      }

      // Fallback if "invariant" or "type" is not found
      throw new IOException("An invariant should always have a type");
    }

    private Class<? extends AbstractInvariantEntry> getClassForType(String type) {
      switch (type) {
        case "function_contract" -> {
          return FunctionContractEntry.class;
        }
        case "loop_invariant", "location_invariant" -> {
          return InvariantEntry.class;
        }
        default -> throw new IllegalArgumentException("Unknown invariant type: " + type);
      }
    }
  }
}
