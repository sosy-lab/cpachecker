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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.EdgeType;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.Node;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.VisitResult;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;

abstract class SdgDotExporter<P, T, V> {

  private static final ImmutableMap<EdgeType, String> edgeStyles;
  private static final ImmutableMap<EdgeType, String> edgeLabels;

  static {
    ImmutableSortedMap.Builder<EdgeType, String> edgeLabelBuilder =
        ImmutableSortedMap.naturalOrder();
    edgeLabelBuilder.put(EdgeType.FLOW_DEPENDENCY, "Flow Dependency");
    edgeLabelBuilder.put(EdgeType.CONTROL_DEPENDENCY, "Control Dependency");
    edgeLabelBuilder.put(EdgeType.DECLARATION_EDGE, "Declaration Edge");
    edgeLabelBuilder.put(EdgeType.CALL_EDGE, "Call Edge");
    edgeLabelBuilder.put(EdgeType.PARAMETER_EDGE, "Parameter Edge");
    edgeLabelBuilder.put(EdgeType.SUMMARY_EDGE, "Summary Edge");
    edgeLabels = edgeLabelBuilder.build();

    ImmutableMap.Builder<EdgeType, String> edgeStyleBuilder =
        ImmutableMap.builderWithExpectedSize(EdgeType.values().length);
    edgeStyleBuilder.put(EdgeType.FLOW_DEPENDENCY, "style=\"bold\",color=\"{color}\"");
    edgeStyleBuilder.put(EdgeType.CONTROL_DEPENDENCY, "style=\"bold,dashed\",color=\"{color}\"");
    edgeStyleBuilder.put(EdgeType.DECLARATION_EDGE, "color=\"{color}\"");
    edgeStyleBuilder.put(EdgeType.CALL_EDGE, "style=\"dashed\",color=\"{color}\"");
    edgeStyleBuilder.put(EdgeType.PARAMETER_EDGE, "style=\"bold,dotted\",color=\"{color}\"");
    edgeStyleBuilder.put(
        EdgeType.SUMMARY_EDGE, "style=\"bold\",peripheries=\"2\",color=\"{color}:invis:{color}\"");
    edgeStyles = edgeStyleBuilder.build();
  }

  protected abstract String getProcedureLabel(P pProcedure);

  protected abstract String getNodeStyle(Node<P, T, V> pNode);

  protected abstract String getNodeLabel(Node<P, T, V> pNode);

  protected abstract boolean isHighlighted(Node<P, T, V> pNode);

  protected abstract boolean isHighlighted(
      EdgeType pEdgeType, Node<P, T, V> pPredecessor, Node<P, T, V> pSuccessor);

  private String escape(String pLabel) {
    return pLabel.replaceAll("\\\"", "\\\\\"");
  }

  private String nodeId(Map<Node<P, T, V>, Long> pVisitedNodes, Node<P, T, V> pNode) {
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
      Writer pWriter, SystemDependenceGraph<P, T, V> pSdg, Map<Node<P, T, V>, Long> pVisitedNodes)
      throws IOException {

    StringBuilder sb = new StringBuilder();

    for (Node<P, T, V> node : pSdg.getNodes()) {

      sb.setLength(0);

      pSdg.traverse(
          ImmutableSet.of(node),
          new SystemDependenceGraph.ForwardsVisitor<P, T, V>() {

            @Override
            public VisitResult visitNode(Node<P, T, V> pNode) {
              return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitEdge(
                EdgeType pType, Node<P, T, V> pPredecessor, Node<P, T, V> pSuccessor) {

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
  }

  private void write(Writer pWriter, SystemDependenceGraph<P, T, V> pSdg) throws IOException {

    pWriter.write("digraph SystemDependenceGraph {\n");
    pWriter.write("rankdir=LR;\n");

    writeLegend(pWriter);

    Map<Node<P, T, V>, Long> visitedNodes = new HashMap<>();
    Multimap<Optional<P>, Node<P, T, V>> procedureNodes = ArrayListMultimap.create();
    StatCounter counter = new StatCounter("Node Counter");

    pSdg.traverse(
        pSdg.getNodes(),
        new SystemDependenceGraph.ForwardsVisitor<P, T, V>() {

          @Override
          public VisitResult visitNode(Node<P, T, V> pNode) {

            visitedNodes.put(pNode, counter.getValue());
            counter.inc();
            procedureNodes.put(pNode.getProcedure(), pNode);

            return VisitResult.SKIP;
          }

          @Override
          public VisitResult visitEdge(
              EdgeType pType, Node<P, T, V> pPredecessor, Node<P, T, V> pSuccessor) {
            return VisitResult.SKIP;
          }
        });

    for (Optional<P> procedure : procedureNodes.keySet()) {

      pWriter.write(String.format(Locale.ENGLISH, "subgraph cluster_f%d {%n", counter.getValue()));
      counter.inc();
      String procedureLabel =
          procedure.isPresent() ? getProcedureLabel(procedure.orElseThrow()) : "";
      pWriter.write(String.format(Locale.ENGLISH, "label=\"%s\";%n", escape(procedureLabel)));

      for (Node<P, T, V> node : procedureNodes.get(procedure)) {
        String color = isHighlighted(node) ? ",color=red" : "";
        String style = getNodeStyle(node);
        pWriter.write(
            String.format(
                Locale.ENGLISH,
                "%s [%s%slabel=\"%s\"%s]%n",
                nodeId(visitedNodes, node),
                style,
                style.trim().isEmpty() ? "" : ",",
                escape(getNodeLabel(node)),
                color));
      }

      pWriter.write("}\n");
    }

    writeEdges(pWriter, pSdg, visitedNodes);

    pWriter.write("\n}\n");
  }

  void export(SystemDependenceGraph<P, T, V> pSdg, Path pPath, LogManager pLogger) {

    try (Writer writer = IO.openOutputFile(pPath, Charset.defaultCharset())) {
      write(writer, pSdg);
    } catch (IOException ex) {
      pLogger.logUserException(
          Level.WARNING, ex, "Could not write system dependence graph to dot file");
    }
  }
}
