// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
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

/*
 * Enables the export of CFA metadata and information about the main function entry node.
 * The export format is JSON and the target is a cfa.json file.
 */
public class CfaToJson {
  private final Set<CFANode> nodes;
  private final List<CFAEdge> edges;

  public CfaToJson(CFA pCfa) {
    CFA cfa = checkNotNull(pCfa);

    // Collect all nodes and edges by traversing the cfa
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

  /** output the CFA as JSON file */
  public void write(Path pOutdir) throws IOException {
    // ObjectMapper objectMapper = new ObjectMapper();
    // objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    // objectMapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);
    JsonFactory jsonFactory = new JsonFactory();

    // for (FunctionEntryNode entryNode : cfa.entryNodes()) {
    // .resolve("functions").resolve(entryNode.getFunctionName() + ".json");
    Path jsonFilePath = pOutdir.resolve("cfa.json");
    MoreFiles.createParentDirectories(jsonFilePath);

    try (FileOutputStream fileOutputStream = new FileOutputStream(jsonFilePath.toFile());
        JsonGenerator jsonGenerator =
            jsonFactory.createGenerator(fileOutputStream, JsonEncoding.UTF8); ) {

      // Nodes
      jsonGenerator.writeStartObject();
      jsonGenerator.writeNumberField("nodeCount", nodes.size());
      jsonGenerator.writeFieldName("nodes");
      jsonGenerator.writeStartArray();
      for (CFANode node : nodes) {
        jsonGenerator.writeNumber(node.getNodeNumber());
      }
      jsonGenerator.writeEndArray();
      jsonGenerator.writeEndObject();

      // Edges
      jsonGenerator.writeStartObject();
      jsonGenerator.writeNumberField("edgeCount", edges.size());
      jsonGenerator.writeFieldName("edges");
      jsonGenerator.writeStartArray();
      for (CFAEdge edge : edges) {
        jsonGenerator.writeNumber(edge.getLineNumber());
      }
      jsonGenerator.writeEndArray();
      jsonGenerator.writeEndObject();
    }
  }

  //  private static class CFAVisitorJSON extends DefaultCFAVisitor {
  //    private final Set<CFANode> nodes = new HashSet<>();
  //    private final Set<CFAEdge> edges = new HashSet<>();
  //
  //    @Override
  //    public TraversalProcess visitNode(CFANode node) {
  //      nodes.add(node);
  //
  //      return TraversalProcess.CONTINUE;
  //    }
  //
  //    @Override
  //    public TraversalProcess visitEdge(CFAEdge edge) {
  //      edges.add(edge);
  //
  //      return TraversalProcess.CONTINUE;
  //    }
  //
  //    Set<CFANode> getNodes() {
  //      return nodes;
  //    }
  //
  //    Set<CFAEdge> getEdges() {
  //      return edges;
  //    }
  //  }
}
