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
   * Returns the scope describing the program location of the given node, if there is one.
   *
   * <p>The location of a node is derived from its edges, and for the entry and the exit node of a
   * function these are the ones of the call site, i.e., inside the calling function. The resulting
   * record would name a function which does not contain its own location, so we only return a scope
   * for locations inside the function of the node.
   */
  public static Optional<PrecisionScope> localPrecisionScopeFor(
      CFANode pNode, AstCfaRelation pAstCfaRelation) {
    String functionName = pNode.getFunctionName();
    if (pNode.isLoopStart()) {
      return pAstCfaRelation
          .getTightestIterationStructureForNode(pNode)
          .map(structure -> structure.getCompleteElement().location())
          .filter(location -> isInsideFunctionOf(location, pNode))
          .map(
              location ->
                  new LocalLoopPrecisionScope(
                      LocationRecord.createLocationRecordAtStart(location, functionName)));
    } else {
      return pAstCfaRelation
          .getStatementFileLocationForNode(pNode)
          .filter(location -> isInsideFunctionOf(location, pNode))
          .map(
              location ->
                  new LocalPrecisionScope(
                      LocationRecord.createLocationRecordAtStart(location, functionName)));
    }
  }

  /**
   * Whether the given location lies inside the function the given node belongs to. The offsets are
   * compared and not the lines, since the lines of an origin file say nothing about a location of
   * another origin file which was preprocessed into the same one.
   */
  private static boolean isInsideFunctionOf(FileLocation pLocation, CFANode pNode) {
    FileLocation functionLocation = pNode.getFunction().getFileLocation();
    return pLocation.getFileName().equals(functionLocation.getFileName())
        && pLocation.getNodeOffset() >= functionLocation.getNodeOffset()
        && pLocation.getNodeOffset()
            <= functionLocation.getNodeOffset() + functionLocation.getNodeLength();
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
