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
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.EdgeType;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.Node;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.VisitResult;

/**
 * Exporter to export {@link SystemDependenceGraph} instances as dot files.
 *
 * @param <P> the procedure type of the SDG
 * @param <T> the statement type of the SDG
 * @param <V> the variable type of the SDG
 * @param <N> the node type of the SDG
 */
abstract class SdgDotExporter<P, T, V, N extends SystemDependenceGraph.Node<P, T, V>> {

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
    edgeLabels = edgeLabelBuilder.buildOrThrow();

    ImmutableMap.Builder<EdgeType, String> edgeStyleBuilder =
        ImmutableMap.builderWithExpectedSize(EdgeType.values().length);
    edgeStyleBuilder.put(EdgeType.FLOW_DEPENDENCY, "style=\"bold\",color=\"{color}\"");
    edgeStyleBuilder.put(EdgeType.CONTROL_DEPENDENCY, "style=\"bold,dashed\",color=\"{color}\"");
    edgeStyleBuilder.put(EdgeType.DECLARATION_EDGE, "color=\"{color}\"");
    edgeStyleBuilder.put(EdgeType.CALL_EDGE, "style=\"dashed\",color=\"{color}\"");
    edgeStyleBuilder.put(EdgeType.PARAMETER_EDGE, "style=\"bold,dotted\",color=\"{color}\"");
    edgeStyleBuilder.put(
        EdgeType.SUMMARY_EDGE, "style=\"bold\",peripheries=\"2\",color=\"{color}:invis:{color}\"");
    edgeStyles = edgeStyleBuilder.buildOrThrow();
  }

  /**
   * Returns the value of the dot file label for the specified procedure.
   *
   * <p>Quotation marks are automatically escaped.
   *
   * @param pProcedure the procedure to get the label for
   * @return the value of the dot file label for the specified procedure
   */
  protected abstract String getProcedureLabel(P pProcedure);

  /**
   * Returns style, shape, color, and possibly other attributes (except label) for the specified
   * node in a dot file compatible format.
   *
   * <p>The returned style string can be empty. All occurrences of the substring <code>"{color}"
   * </code> (quotation marks are not part of the substring) are automatically replaced with the
   * right color based on highlighting of the node.
   *
   * <p>Examples as java string literals: <or>
   * <li><code>""</code>
   * <li><code>"color=\"{color}\""</code>
   * <li><code>"style=\"bold,dashed\",shape=\"ellipse\",color=\"{color}\""</code>
   * </ol>
   *
   * @param pNode the node to get the style for
   * @return dot file compatible style, shape, color, and possibly other attributes (except label)
   *     for the node
   */
  protected abstract String getNodeStyle(Node<P, T, V> pNode);

  /**
   * Returns the value of the dot file label for the specified node.
   *
   * <p>Quotation marks are automatically escaped.
   *
   * @param pNode the node to get the label for
   * @return the value of the dot file label for the specified node
   */
  protected abstract String getNodeLabel(Node<P, T, V> pNode);

  /**
   * Returns whether the specified node is highlighted.
   *
   * @param pNode the node to check the highlighting for
   * @return {@code true} if the specified node is highlighted; otherwise, {@code false} is returned
   */
  protected abstract boolean isHighlighted(Node<P, T, V> pNode);

  /**
   * Returns whether the edge (specified by its type, predecessor and successor) is highlighted.
   *
   * @param pEdgeType the type of the edge
   * @param pPredecessor the predecessor of the edge
   * @param pSuccessor the successor of the edge
   * @return {@code true} if the specified edge is highlighted; otherwise, {@code false} is returned
   */
  protected abstract boolean isHighlighted(
      EdgeType pEdgeType, Node<P, T, V> pPredecessor, Node<P, T, V> pSuccessor);

  /** Returns the specified string with escaped quotation marks. */
  private String escape(String pLabel) {
    return pLabel.replace("\"", "\\\"");
  }

  /** Returns the identifier used in the dot file for the specified node. */
  private String nodeId(Map<Node<P, T, V>, Long> pVisitedNodes, Node<P, T, V> pNode) {
    return "n" + pVisitedNodes.get(pNode);
  }

  private static void writeLegend(Writer pWriter) throws IOException {

    pWriter.write("subgraph cluster_legend {\n");
    pWriter.write("label=\"Legend\\nY depends on X\";\n");

    pWriter.write("key1 [");
    pWriter.write("penwidth=\"0\",");
    pWriter.write("label=");
    pWriter.write("<<table ");
    pWriter.write("border=\"0\" ");
    pWriter.write("cellpadding=\"8\" ");
    pWriter.write("cellspacing=\"0\" ");
    pWriter.write("cellborder=\"0\">\n");

    int i = 1;
    for (String label : edgeLabels.values()) {
      pWriter.write("<tr>");
      pWriter.write("<td ");
      pWriter.write("align=\"right\" ");
      pWriter.write("port=\"i");
      pWriter.write(String.valueOf(i));
      pWriter.write("\">");
      pWriter.write(label);
      pWriter.write("      X ");
      pWriter.write("</td>");
      pWriter.write("</tr>\n");
      i++;
    }

    pWriter.write("</table>>]\n");

    pWriter.write("key2 [");
    pWriter.write("penwidth=\"0\",");
    pWriter.write("label=");
    pWriter.write("<<table ");
    pWriter.write("border=\"0\" ");
    pWriter.write("cellpadding=\"8\" ");
    pWriter.write("cellspacing=\"0\" ");
    pWriter.write("cellborder=\"0\">\n");

    for (i = 1; i <= edgeLabels.size(); i++) {
      pWriter.write("<tr>");
      pWriter.write("<td ");
      pWriter.write("port=\"i");
      pWriter.write(String.valueOf(i));
      pWriter.write("\">");
      pWriter.write(" Y");
      pWriter.write("</td>");
      pWriter.write("</tr>\n");
    }

    pWriter.write("</table>>]\n");

    i = 1;
    for (EdgeType edgeType : edgeLabels.keySet()) {
      String edgeStyle = edgeStyles.get(edgeType).replace("{color}", "black");
      pWriter.write("key1:i");
      pWriter.write(String.valueOf(i));
      pWriter.write(":e");
      pWriter.write(" -> ");
      pWriter.write("key2:i");
      pWriter.write(String.valueOf(i));
      pWriter.write(":w");
      pWriter.write(" [");
      pWriter.write(edgeStyle);
      pWriter.write("]\n");
      i++;
    }

    pWriter.write("}\n");
  }

  private void writeEdges(
      Writer pWriter, SystemDependenceGraph<V, N> pSdg, Map<Node<P, T, V>, Long> pVisitedNodes)
      throws IOException {

    StringBuilder sb = new StringBuilder();

    for (N node : pSdg.getNodes()) {

      sb.setLength(0);

      pSdg.traverse(
          ImmutableSet.of(node),
          new SystemDependenceGraph.ForwardsVisitor<N>() {

            @Override
            public VisitResult visitNode(N pNode) {
              return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitEdge(EdgeType pType, N pPredecessor, N pSuccessor) {

              sb.append(nodeId(pVisitedNodes, pPredecessor));
              sb.append(" -> ");
              sb.append(nodeId(pVisitedNodes, pSuccessor));
              sb.append(' ');

              String color = isHighlighted(pType, pPredecessor, pSuccessor) ? "red" : "black";
              String edgeStyle = edgeStyles.get(pType).replace("{color}", color);
              sb.append('[');
              sb.append(edgeStyle);
              sb.append("]\n");

              return VisitResult.SKIP;
            }
          });

      pWriter.write(sb.toString());
    }
  }

  private void write(Writer pWriter, SystemDependenceGraph<V, N> pSdg) throws IOException {

    pWriter.write("digraph SystemDependenceGraph {\n");
    pWriter.write("rankdir=LR;\n");

    writeLegend(pWriter);

    Map<Node<P, T, V>, Long> visitedNodes = new HashMap<>();
    Multimap<Optional<P>, Node<P, T, V>> procedureNodes = ArrayListMultimap.create();

    long counter = 0;
    for (Node<P, T, V> node : pSdg.getNodes()) {
      visitedNodes.put(node, counter);
      counter++;
      procedureNodes.put(node.getProcedure(), node);
    }

    for (Optional<P> procedure : procedureNodes.keySet()) {

      pWriter.write("subgraph cluster_f");
      pWriter.write(String.valueOf(counter));
      pWriter.write(" {\n");
      counter++;

      String procedureLabel =
          procedure.isPresent() ? getProcedureLabel(procedure.orElseThrow()) : "";
      pWriter.write("label=\"");
      pWriter.write(escape(procedureLabel));
      pWriter.write("\";\n");

      for (Node<P, T, V> node : procedureNodes.get(procedure)) {

        String color = isHighlighted(node) ? "red" : "black";
        String style = getNodeStyle(node).replace("{color}", color);

        pWriter.write(nodeId(visitedNodes, node));
        pWriter.write(" [");
        pWriter.write(style);
        pWriter.write(style.trim().isEmpty() ? "" : ",");
        pWriter.write("label=\"");
        pWriter.write(escape(getNodeLabel(node)));
        pWriter.write("\"]\n");
      }

      pWriter.write("}\n");
    }

    writeEdges(pWriter, pSdg, visitedNodes);

    pWriter.write("\n}\n");
  }

  /**
   * Exports the specified {@link SystemDependenceGraph} as a dot file.
   *
   * @param pSdg the system dependence graph to export
   * @param pPath the file the SDG is written to
   * @param pLogger the logger used for logging exceptions and other notable messages occurring
   *     during export of the SDG
   */
  void export(SystemDependenceGraph<V, N> pSdg, Path pPath, LogManager pLogger) {

    try (Writer writer = IO.openOutputFile(pPath, Charset.defaultCharset())) {
      write(writer, pSdg);
    } catch (IOException ex) {
      pLogger.logUserException(
          Level.WARNING, ex, "Failed writing system dependence graph to dot file");
    }
  }
}
