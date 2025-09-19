// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionScope.PrecisionScopeDeserializer;

@JsonDeserialize(using = PrecisionScopeDeserializer.class)
public abstract class PrecisionScope {

  @JsonProperty("type")
  protected final String entryType;

  public PrecisionScope(@JsonProperty("type") String pEntryType) {
    entryType = pEntryType;
  }

  public String getEntryType() {
    return entryType;
  }

  public static Optional<PrecisionScope> localPrecisionScopeFor(
      CFANode pNode, AstCfaRelation pAstCfaRelation) {
    String functionName = pNode.getFunctionName();
    if (pNode.isLoopStart()) {
      Optional<IterationElement> iterationStructure =
          pAstCfaRelation.getTightestIterationStructureForNode(pNode);

      if (iterationStructure.isEmpty()) {
        return Optional.empty();
      } else {
        FileLocation fileLocation =
            iterationStructure.orElseThrow().getCompleteElement().location();
        return Optional.of(
            new LocalLoopPrecisionScope(
                LocationRecord.createLocationRecordAtStart(fileLocation, functionName)));
      }

    } else {
      Optional<FileLocation> fileLocation = pAstCfaRelation.getStatementFileLocationForNode(pNode);
      if (fileLocation.isEmpty()) {
        return Optional.empty();
      } else {
        return Optional.of(
            new LocalPrecisionScope(
                LocationRecord.createLocationRecordAtStart(
                    fileLocation.orElseThrow(), functionName)));
      }
    }
  }

  public static class PrecisionScopeDeserializer extends JsonDeserializer<PrecisionScope> {

    @Override
    public PrecisionScope deserialize(
        JsonParser pJsonParser, DeserializationContext pDeserializationContext) throws IOException {

      ObjectMapper mapper = (ObjectMapper) pJsonParser.getCodec();
      ObjectNode root = mapper.readTree(pJsonParser);

      String type = root.get("type").asText();

      Class<? extends PrecisionScope> targetClass =
          switch (type) {
            case GlobalPrecisionScope.GLOBAL_TYPE_IDENTIFIER -> GlobalPrecisionScope.class;
            case FunctionPrecisionScope.FUNCTION_TYPE_IDENTIFIER -> FunctionPrecisionScope.class;
            case LocalPrecisionScope.LOCATION_TYPE_IDENTIFIER -> LocalPrecisionScope.class;
            case LocalLoopPrecisionScope.LOCATION_TYPE_IDENTIFIER -> LocalLoopPrecisionScope.class;
            default -> throw new IllegalArgumentException("Unknown type: " + type);
          };

      return mapper.treeToValue(root, targetClass);
    }
  }
}
