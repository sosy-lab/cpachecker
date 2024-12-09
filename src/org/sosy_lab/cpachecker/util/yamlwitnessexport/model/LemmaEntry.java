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
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LemmaEntry.LemmaRecordDeserializer;

@JsonDeserialize(using = LemmaRecordDeserializer.class)
public class LemmaEntry extends AbstractInformationRecord {

  @JsonProperty("value")
  protected final String value;

  public LemmaEntry(
      @JsonProperty("value") String pString,
      @JsonProperty("format") YAMLWitnessExpressionType pFormat) {
    super("lemma", pFormat);
    value = pString;
  }

  public String getValue() {
    return value;
  }

  public static class LemmaRecordDeserializer extends JsonDeserializer<LemmaEntry> {
    @Override
    public LemmaEntry deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
      ObjectMapper mapper = (ObjectMapper) jp.getCodec();
      JsonNode node = mapper.readTree(jp);

      // The node should now be the 'lemma' node. Move one level deeper to its children.
      JsonNode lemmaNode = node.get("lemma");
      assert lemmaNode != null;

      // Delegate the actual object mapping back to Jackson:
      // WaypointRecord result = mapper.treeToValue(waypointNode, WaypointRecord.class);
      // CAVEAT: does not work, since this would use the custom deserializer.
      // Using the original deserializer is apparently very hard.
      // For now just manually construct this
      // (less elegant, but we probably never touch that code again, so it is fine):
      return new LemmaEntry(
          mapper.treeToValue(lemmaNode.get("value"), String.class),
          mapper.treeToValue(lemmaNode.get("format"), YAMLWitnessExpressionType.class));
    }
  }
}
