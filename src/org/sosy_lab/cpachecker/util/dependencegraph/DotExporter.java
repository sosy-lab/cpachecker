// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.EdgeType;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.Node;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.VisitResult;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;

abstract class DotExporter<T, V, C> {

  private static final ImmutableMap<EdgeType, String> edgeStyles =
      ImmutableMap.of(
          EdgeType.FLOW_DEPENDENCY,
          "style=bold",
          EdgeType.CONTROL_DEPENDENCY,
          "style=\"bold,dashed\"",
          EdgeType.PARAMETER_EDGE,
          "style=\"bold,dotted\"");
  private static final ImmutableMap<EdgeType, String> edgeLabels =
      ImmutableSortedMap.of(
          EdgeType.FLOW_DEPENDENCY,
          "Flow Dependency",
          EdgeType.CONTROL_DEPENDENCY,
          "Control Dependecy",
          EdgeType.PARAMETER_EDGE,
          "Parameter Dependency");

  protected abstract C getContext(Node<T, V> pNode);

  protected abstract String getContextLabel(C pContext);

  protected abstract String getNodeStyle(Node<T, V> pNode);

  protected abstract String getNodeLabel(Node<T, V> pNode);

  protected abstract boolean isHighlighted(Node<T, V> pNode);

  protected abstract boolean isHighlighted(
      EdgeType pEdgeType, Node<T, V> pPredecessor, Node<T, V> pSuccessor);

  private String escape(String pLabel) {
    return pLabel.replaceAll("\\\"", "\\\\\"");
  }

  private String nodeId(Map<Node<T, V>, Long> pVisitedNodes, Node<T, V> pNode) {
    return "n" + pVisitedNodes.get(pNode);
  }

  private static void writeLegend(PrintWriter pWriter) {

    pWriter.println("subgraph cluster_legend {");
    pWriter.println("label=\"Legend\\nY depends on X\";");

    pWriter.println(
        "key1 [penwidth=\"0\",label=<<table border=\"0\" cellpadding=\"8\" cellspacing=\"0\""
            + " cellborder=\"0\">");

    int i = 1;
    for (String label : edgeLabels.values()) {
      pWriter.printf(
          Locale.ENGLISH, "<tr><td align=\"right\" port=\"i%d\">%s      X </td></tr>\n", i, label);
      i++;
    }

    pWriter.println("</table>>]");

    pWriter.println(
        "key2 [penwidth=\"0\",label=<<table border=\"0\" penwidth=\"0\" cellpadding=\"8\""
            + " cellspacing=\"0\" cellborder=\"0\">");

    for (i = 1; i <= edgeLabels.size(); i++) {
      pWriter.printf(Locale.ENGLISH, "<tr><td port=\"i%d\"> Y</td></tr>\n", i);
    }

    pWriter.println("</table>>]");

    i = 1;
    for (EdgeType edgeType : edgeLabels.keySet()) {
      pWriter.printf(
          Locale.ENGLISH, "key1:i%d:e -> key2:i%d:w [%s]\n", i, i, edgeStyles.get(edgeType));
      i++;
    }

    pWriter.println('}');
  }

  private void writeEdges(
      PrintWriter pWriter, SystemDependenceGraph<T, V> pSdg, Map<Node<T, V>, Long> pVisitedNodes) {

    pSdg.traverse(
        pSdg.getNodes(),
        new SystemDependenceGraph.ForwardsVisitor<T, V>() {

          @Override
          public VisitResult visitNode(Node<T, V> pNode) {
            return VisitResult.CONTINUE;
          }

          @Override
          public VisitResult visitEdge(
              EdgeType pType, Node<T, V> pPredecessor, Node<T, V> pSuccessor) {

            pWriter.printf(
                Locale.ENGLISH,
                "%s -> %s ",
                nodeId(pVisitedNodes, pPredecessor),
                nodeId(pVisitedNodes, pSuccessor));

            String color = isHighlighted(pType, pPredecessor, pSuccessor) ? ",color=red" : "";
            pWriter.printf(Locale.ENGLISH, " [%s%s]\n", edgeStyles.get(pType), color);

            return VisitResult.SKIP;
          }
        });
  }

  private void write(PrintWriter pWriter, SystemDependenceGraph<T, V> pSdg) {

    pWriter.println("digraph SystemDependenceGraph {");
    pWriter.println("rankdir=LR;");

    writeLegend(pWriter);

    Map<Node<T, V>, Long> visitedNodes = new HashMap<>();
    Multimap<C, Node<T, V>> contexts = ArrayListMultimap.create();
    StatCounter counter = new StatCounter("Node Counter");

    pSdg.traverse(
        pSdg.getNodes(),
        new SystemDependenceGraph.ForwardsVisitor<T, V>() {

          @Override
          public VisitResult visitNode(Node<T, V> pNode) {

            visitedNodes.put(pNode, counter.getValue());
            counter.inc();
            contexts.put(getContext(pNode), pNode);

            return VisitResult.SKIP;
          }

          @Override
          public VisitResult visitEdge(
              EdgeType pType, Node<T, V> pPredecessor, Node<T, V> pSuccessor) {
            return VisitResult.SKIP;
          }
        });

    for (C cluster : contexts.keySet()) {

      pWriter.printf(Locale.ENGLISH, "subgraph cluster_f%d {\n", counter.getValue());
      counter.inc();
      pWriter.printf(Locale.ENGLISH, "label=\"%s\";\n", escape(getContextLabel(cluster)));

      for (Node<T, V> node : contexts.get(cluster)) {
        String color = isHighlighted(node) ? ",color=red" : "";
        pWriter.printf(
            Locale.ENGLISH,
            "%s [%s,label=\"%s\"%s]\n",
            nodeId(visitedNodes, node),
            getNodeStyle(node),
            escape(getNodeLabel(node)),
            color);
      }

      pWriter.println('}');
    }

    writeEdges(pWriter, pSdg, visitedNodes);

    pWriter.println("\n}");
  }

  void export(SystemDependenceGraph<T, V> pSdg, Path pPath, LogManager pLogger) {

    try (PrintWriter writer = new PrintWriter(IO.openOutputFile(pPath, Charset.defaultCharset()))) {
      write(writer, pSdg);
    } catch (IOException ex) {
      pLogger.logUserException(
          Level.WARNING, ex, "Could not write system dependence graph to dot file");
    }
  }
}
