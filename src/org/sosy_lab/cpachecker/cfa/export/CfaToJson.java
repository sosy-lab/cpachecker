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
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.CompositeCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.NodeCollectingCFAVisitor;

/**
 * Enables the export of all CFA {@link FunctionEntryNode}s, {@link CFANode}s, {@link CFAEdge}s and
 * relevant Metadata.
 *
 * <p>The export format is JSON.
 */
public class CfaToJson {
  private final Set<CFANode> nodes;
  private final List<CFAEdge> edges;

  public CfaToJson(CFA pCfa) {
    CFA cfa = checkNotNull(pCfa);

    /** Collect all nodes and edges by traversing the CFA. */
    NodeCollectingCFAVisitor nodeVisitor = new NodeCollectingCFAVisitor();
    EdgeCollectingCFAVisitor edgeVisitor = new EdgeCollectingCFAVisitor();

    CFAVisitor visitor =
        new NodeCollectingCFAVisitor(new CompositeCFAVisitor(nodeVisitor, edgeVisitor));

    for (FunctionEntryNode entryNode : cfa.entryNodes()) {
      CFATraversal.dfs().traverse(entryNode, visitor);
    }

    nodes = nodeVisitor.getVisitedNodes();
    edges = edgeVisitor.getVisitedEdges();
  }

  /**
   * Write the {@link CFA} to file.
   *
   * @param pOutdir Directory to which the JSON file is to be written.
   * @throws IOException If an error with {@link FileOutputStream} or {@link JsonGenerator} occurs.
   */
  public void write(Path pOutdir) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    /** Only map fields of objects. */
    objectMapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
    objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    /** Enable JSON serialization with indentation and newlines. */
    objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

    JsonFactory jsonFactory = new JsonFactory();

    Path jsonFilePath = pOutdir.resolve("cfa.json");
    MoreFiles.createParentDirectories(jsonFilePath);

    try (FileOutputStream fileOutputStream = new FileOutputStream(jsonFilePath.toFile());
        JsonGenerator jsonGenerator =
            jsonFactory.createGenerator(fileOutputStream, JsonEncoding.UTF8); ) {

      jsonGenerator.writeStartObject();

      /** Write all Nodes. */
      jsonGenerator.writeFieldName("nodes");
      jsonGenerator.writeStartArray();
      for (CFANode node : nodes) {
        objectMapper.writeValue(jsonGenerator, node);
      }
      jsonGenerator.writeEndArray();

      /** Write all Edges. */
      jsonGenerator.writeFieldName("edges");
      jsonGenerator.writeStartArray();
      for (CFAEdge edge : edges) {
        objectMapper.writeValue(jsonGenerator, edge);
      }
      jsonGenerator.writeEndArray();

      jsonGenerator.writeEndObject();
    }
  }
}
    // TODO for (FunctionEntryNode entryNode : cfa.entryNodes()) {
    // .resolve("functions").resolve(entryNode.getFunctionName() + ".json");
