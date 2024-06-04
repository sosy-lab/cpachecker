// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.io.MoreFiles;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.CompositeCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.NodeCollectingCFAVisitor;

/**
 * Enables to export all {@link CFA} {@link FunctionEntryNode}s, {@link CFANode}s, {@link CFAEdge}s
 * and relevant Metadata.
 *
 * <p>The export format is JSON.
 */
public final class CfaToJson {
  private final CfaJsonExport cfaJsonExport;

  public CfaToJson(CFA pCfa) {
    CFA cfa = checkNotNull(pCfa);

    /* Collect all nodes and edges by traversing the CFA. */

    NodeCollectingCFAVisitor nodeVisitor = new NodeCollectingCFAVisitor();
    EdgeCollectingCFAVisitor edgeVisitor = new EdgeCollectingCFAVisitor();

    CFAVisitor visitor =
        new NodeCollectingCFAVisitor(new CompositeCFAVisitor(nodeVisitor, edgeVisitor));

    for (FunctionEntryNode entryNode : cfa.entryNodes()) {
      CFATraversal.dfs().traverse(entryNode, visitor);
    }

    this.cfaJsonExport =
        new CfaJsonExport(nodeVisitor.getVisitedNodes(), edgeVisitor.getVisitedEdges());
  }

  /**
   * This record represents the {@link CFA} data to be exported to JSON.
   *
   * <p>It contains all {@link CFANode}s and {@link CFAEdge}s.
   */
  private record CfaJsonExport(Collection<CFANode> nodes, Collection<CFAEdge> edges) {}

  /**
   * Writes the {@link CfaJsonExport} data.
   *
   * @param pOutdir Directory to which the JSON files are to be written.
   * @throws IOException If an error with {@link FileOutputStream} or {@link JsonGenerator} occurs.
   */
  public void write(Path pOutdir) throws IOException {
    /* Define the file path and create any required directories. */
    Path jsonFilePath = pOutdir.resolve("cfa.json");
    MoreFiles.createParentDirectories(jsonFilePath);

    /* Write the CFA data. */
    try (FileOutputStream fileOutputStream = new FileOutputStream(jsonFilePath.toFile());
        JsonGenerator jsonGenerator =
            new JsonFactory().createGenerator(fileOutputStream, JsonEncoding.UTF8); ) {

      provideConfiguredCfaObjectMapper().writeValue(jsonGenerator, this.cfaJsonExport);
    }
  }

  /**
   * Configures and provides an instance of {@link ObjectMapper} for CFA serialization.
   *
   * @return The configured {@link ObjectMapper} instance which only maps fields and uses
   *     indentation and newlines.
   */
  private static ObjectMapper provideConfiguredCfaObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();

    /* Only map fields of objects. */
    objectMapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
    objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

    /* Enable serialization with indentation and newlines. */
    objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

    /* Add mixins. */
    objectMapper.addMixIn(FunctionEntryNode.class, FunctionEntryNodeMixin.class);
    objectMapper.addMixIn(FunctionExitNode.class, FunctionExitNodeMixin.class);
    objectMapper.addMixIn(CFANode.class, CfaNodeMixin.class);

    return objectMapper;
  }

  /**
   * This class is a mixin for {@link FunctionEntryNode}.
   *
   * <p>It serializes its {@link FunctionExitNode} field as number.
   */
  private abstract static class FunctionEntryNodeMixin {

    @JsonIdentityReference(alwaysAsId = true)
    private FunctionExitNode exitNode;
  }

  /**
   * This class is a mixin for {@link FunctionExitNode}.
   *
   * <p>It serializes its {@link FunctionEntryNode} field as number.
   */
  private abstract static class FunctionExitNodeMixin {

    @JsonIdentityReference(alwaysAsId = true)
    private FunctionEntryNode entryNode;
  }

  /**
   * This class is a mixin for {@link CFANode}.
   *
   * <p>It prevents cyclic references by serializing the {@link CFANode} as number if the same node
   * has already been fully serialized once.
   */
  @JsonIdentityInfo(
      generator = ObjectIdGenerators.PropertyGenerator.class,
      scope = CFANode.class,
      property = "nodeNumber")
  private abstract static class CfaNodeMixin {}
}
    // TODO for (FunctionEntryNode entryNode : cfa.entryNodes()) {
    // .resolve("functions").resolve(entryNode.getFunctionName() + ".json");
