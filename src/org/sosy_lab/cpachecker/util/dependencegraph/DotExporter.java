// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.Direction;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.EdgeType;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.Node;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.NodeType;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.VisitResult;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.Visitor;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

final class DotExporter {

  private final SystemDependenceGraph<CFAEdge, MemoryLocation> sdg;

  private final StringBuilder stringBuilder;
  private final Map<Node<CFAEdge, MemoryLocation>, Long> visitedNodes;
  private long counter;

  private DotExporter(SystemDependenceGraph<CFAEdge, MemoryLocation> pSdg) {

    sdg = pSdg;

    stringBuilder = new StringBuilder();
    visitedNodes = new HashMap<>();
    counter = 0;
  }

  private String escape(String pLabel) {
    return pLabel.replaceAll("\\\"", "\\\\\"");
  }

  private String nodeId(Node<CFAEdge, MemoryLocation> pNode) {
    return "n" + visitedNodes.get(pNode);
  }

  private void appendEdges() {

    sdg.traverseOnce(
        Direction.FORWARDS,
        sdg.getNodes(),
        new Visitor<CFAEdge, MemoryLocation>() {

          @Override
          public VisitResult visitNode(Node<CFAEdge, MemoryLocation> pNode) {
            return VisitResult.CONTINUE;
          }

          @Override
          public VisitResult visitEdge(
              EdgeType pType,
              Node<CFAEdge, MemoryLocation> pPredecessor,
              Node<CFAEdge, MemoryLocation> pSuccessor) {

            stringBuilder.append(nodeId(pPredecessor));
            stringBuilder.append(" -> ");
            stringBuilder.append(nodeId(pSuccessor));

            if (pType == EdgeType.CONTROL_DEPENDENCY) {
              stringBuilder.append(" [style=dashed]");
            }

            stringBuilder.append('\n');

            return VisitResult.SKIP;
          }
        });
  }

  private String createString() {

    stringBuilder.append("digraph SystemDependenceGraph {\n");

    Multimap<AFunctionDeclaration, Node<CFAEdge, MemoryLocation>> functionNodes =
        ArrayListMultimap.create();

    sdg.traverseOnce(
        SystemDependenceGraph.Direction.FORWARDS,
        sdg.getNodes(),
        new SystemDependenceGraph.Visitor<CFAEdge, MemoryLocation>() {

          @Override
          public VisitResult visitNode(Node<CFAEdge, MemoryLocation> pNode) {

            visitedNodes.put(pNode, counter);
            counter++;

            CFAEdge cfaEdge = pNode.getStatement();
            CFANode cfaFunctionNode =
                cfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge
                    ? cfaEdge.getSuccessor()
                    : cfaEdge.getPredecessor();
            functionNodes.put(cfaFunctionNode.getFunction(), pNode);

            return VisitResult.SKIP;
          }

          @Override
          public VisitResult visitEdge(
              EdgeType pType,
              Node<CFAEdge, MemoryLocation> pPredecessor,
              Node<CFAEdge, MemoryLocation> pSuccessor) {
            return VisitResult.SKIP;
          }
        });

    for (AFunctionDeclaration function : functionNodes.keySet()) {

      stringBuilder.append("subgraph cluster_f");
      stringBuilder.append(counter);
      counter++;
      stringBuilder.append(" {\n");
      stringBuilder.append("label=\"");
      stringBuilder.append(escape(function.toString()));
      stringBuilder.append("\"\n");

      for (Node<CFAEdge, MemoryLocation> node : functionNodes.get(function)) {

        stringBuilder.append(nodeId(node));
        stringBuilder.append(" [");

        String style;
        switch (node.getStatement().getEdgeType()) {
          case AssumeEdge:
            style = "shape=\"diamond\"";
            break;
          case FunctionCallEdge:
            style = "shape=\"ellipse\", peripheries=\"2\"";
            break;
          case BlankEdge:
            style = "shape=\"box\"";
            break;
          default:
            style = "shape=\"ellipse\"";
        }

        stringBuilder.append(style);

        stringBuilder.append(",label=\"");

        if (node.getType() != NodeType.STATEMENT) {
          stringBuilder.append('[');
          stringBuilder.append(node.getType());
          stringBuilder.append(' ');
          stringBuilder.append(escape(String.valueOf(node.getVariable().orElse(null))));
          stringBuilder.append("] ");
        }

        stringBuilder.append(escape(node.getStatement().toString()));
        stringBuilder.append("\"]\n");
      }

      stringBuilder.append("}\n");
    }

    appendEdges();

    stringBuilder.append("\n}");

    return stringBuilder.toString();
  }

  public static void export(
      SystemDependenceGraph<CFAEdge, MemoryLocation> pSdg, Path pPath, LogManager pLogger) {
    try (Writer writer = IO.openOutputFile(pPath, Charset.defaultCharset())) {
      writer.append(new DotExporter(pSdg).createString());
    } catch (IOException ex) {
      pLogger.logUserException(
          Level.WARNING, ex, "Could not write system dependence graph to dot file");
    }
  }
}
