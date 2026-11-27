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
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionScope.PrecisionScopeDeserializer;

@JsonDeserialize(using = PrecisionScopeDeserializer.class)
public abstract sealed class PrecisionScope
    permits FunctionPrecisionScope,
        GlobalPrecisionScope,
        LocalLoopPrecisionScope,
        LocalPrecisionScope {

  @JsonProperty("type")
  protected final String entryType;

  public PrecisionScope(@JsonProperty("type") String pEntryType) {
    entryType = pEntryType;
  }

  public String getEntryType() {
    return entryType;
  }

  /**
   * Returns the nodes at which the precision of a loop holds, see {@link
   * LocalLoopPrecisionScope#loopHeadIn}.
   */
  public static ImmutableSet<CFANode> loopHeadsOf(CFA pCfa) {
    return pCfa.getLoopStructure()
        .map(pCfa.getAstCfaRelation()::getLoopInvariantNodes)
        .orElse(ImmutableSet.of());
  }

  /**
   * Returns the scope describing the program location of the given node, if there is one: the loop
   * whose head it is, or the statement that starts at it. This is the inverse of {@link
   * LocalLoopPrecisionScope#loopHeadIn} and {@link LocalPrecisionScope#startNodesIn}.
   *
   * @param pLoopHeads the nodes given by {@link #loopHeadsOf}
   */
  public static Optional<PrecisionScope> localPrecisionScopeFor(
      CFANode pNode, CFA pCfa, Set<CFANode> pLoopHeads) {
    AstCfaRelation astCfaRelation = pCfa.getAstCfaRelation();
    String functionName = pNode.getFunctionName();
    if (pLoopHeads.contains(pNode)) {
      Optional<FileLocation> loop =
          astCfaRelation
              .getTightestIterationStructureForNode(pNode)
              .filter(structure ->
                  pCfa.getLoopStructure()
                      .flatMap(structure::getLoopHead)
                      .equals(Optional.of(pNode)))
              .map(structure -> structure.getCompleteElement().location());
      if (loop.isPresent()) {
        return Optional.of(
            new LocalLoopPrecisionScope(
                LocationRecord.createLocationRecordAtStart(loop.orElseThrow(), functionName)));
      }
    }
    return astCfaRelation
        .getStatementStartingAtNode(pNode)
        .map(location ->
            new LocalPrecisionScope(
                LocationRecord.createLocationRecordAtStart(location, functionName)));
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
