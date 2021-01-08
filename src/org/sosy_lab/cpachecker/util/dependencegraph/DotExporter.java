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
import java.io.Writer;
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
          "style=bold,color={color}",
          EdgeType.CONTROL_DEPENDENCY,
          "style=\"bold,dashed\",color={color}",
          EdgeType.PARAMETER_EDGE,
          "style=\"bold,dotted\",color={color}",
          EdgeType.SUMMARY_EDGE,
          "style=bold,peripheries=2,color=\"{color}:invis:{color}\"");
  private static final ImmutableMap<EdgeType, String> edgeLabels =
      ImmutableSortedMap.of(
          EdgeType.FLOW_DEPENDENCY,
          "Flow Dependency",
          EdgeType.CONTROL_DEPENDENCY,
          "Control Dependecy",
          EdgeType.PARAMETER_EDGE,
          "Parameter Edge",
          EdgeType.SUMMARY_EDGE,
          "Summary Edge");

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

  private static void writeLegend(Writer pWriter) throws IOException {

    pWriter.write("subgraph cluster_legend {\n");
    pWriter.write("label=\"Legend\\nY depends on X\";\n");

    pWriter.write(
        "key1 [penwidth=\"0\",label=<<table border=\"0\" cellpadding=\"8\" cellspacing=\"0\""
            + " cellborder=\"0\">\n");

    int i = 1;
    for (String label : edgeLabels.values()) {
      pWriter.write(
          String.format(
              Locale.ENGLISH,
              "<tr><td align=\"right\" port=\"i%d\">%s      X </td></tr>%n",
              i,
              label));
      i++;
    }

    pWriter.write("</table>>]\n");

    pWriter.write(
        "key2 [penwidth=\"0\",label=<<table border=\"0\" penwidth=\"0\" cellpadding=\"8\""
            + " cellspacing=\"0\" cellborder=\"0\">\n");

    for (i = 1; i <= edgeLabels.size(); i++) {
      pWriter.write(String.format(Locale.ENGLISH, "<tr><td port=\"i%d\"> Y</td></tr>%n", i));
    }

    pWriter.write("</table>>]\n");

    i = 1;
    for (EdgeType edgeType : edgeLabels.keySet()) {
      String edgeStyle = edgeStyles.get(edgeType).replace("{color}", "black");
      pWriter.write(
          String.format(Locale.ENGLISH, "key1:i%d:e -> key2:i%d:w [%s]%n", i, i, edgeStyle));
      i++;
    }

    pWriter.write("}\n");
  }

  private void writeEdges(
      Writer pWriter, SystemDependenceGraph<T, V> pSdg, Map<Node<T, V>, Long> pVisitedNodes)
      throws IOException {

    StringBuilder sb = new StringBuilder();

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

            sb.append(
                String.format(
                    Locale.ENGLISH,
                    "%s -> %s ",
                    nodeId(pVisitedNodes, pPredecessor),
                    nodeId(pVisitedNodes, pSuccessor)));

            String color = isHighlighted(pType, pPredecessor, pSuccessor) ? "red" : "black";
            String edgeStyle = edgeStyles.get(pType).replace("{color}", color);
            sb.append(String.format(Locale.ENGLISH, " [%s]%n", edgeStyle));

            return VisitResult.SKIP;
          }
        });

    pWriter.write(sb.toString());
  }

  private void write(Writer pWriter, SystemDependenceGraph<T, V> pSdg) throws IOException {

    pWriter.write("digraph SystemDependenceGraph {\n");
    pWriter.write("rankdir=LR;\n");

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

      pWriter.write(String.format(Locale.ENGLISH, "subgraph cluster_f%d {%n", counter.getValue()));
      counter.inc();
      pWriter.write(
          String.format(Locale.ENGLISH, "label=\"%s\";%n", escape(getContextLabel(cluster))));

      for (Node<T, V> node : contexts.get(cluster)) {
        String color = isHighlighted(node) ? ",color=red" : "";
        pWriter.write(
            String.format(
                Locale.ENGLISH,
                "%s [%s,label=\"%s\"%s]%n",
                nodeId(visitedNodes, node),
                getNodeStyle(node),
                escape(getNodeLabel(node)),
                color));
      }

      pWriter.write("}\n");
    }

    writeEdges(pWriter, pSdg, visitedNodes);

    pWriter.write("\n}\n");
  }

  void export(SystemDependenceGraph<T, V> pSdg, Path pPath, LogManager pLogger) {

    try (Writer writer = IO.openOutputFile(pPath, Charset.defaultCharset())) {
      write(writer, pSdg);
    } catch (IOException ex) {
      pLogger.logUserException(
          Level.WARNING, ex, "Could not write system dependence graph to dot file");
    }
  }
}
