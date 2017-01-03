/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.summary.summaryGeneration;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.sosy_lab.cpachecker.cfa.export.DOTBuilder;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Visualize the graph of summary computation requests.
 */
public class SummaryGenToDotWriter {
  private final Appendable sb;

  public static void write(
      Appendable sb,
      SummaryComputationState initial
  ) throws IOException {
    write(sb, initial, SummaryComputationState::getChildren, t -> true);
  }

  /**
   * Create String with ARG in the DOT format of Graphviz.
   * @param sb Where to write the ARG into.
   * @param rootState the root element of the ARG
   * @param successorFunction A function giving all successors of an SummaryComputationState.
   *                          Only states reachable from root by iteratively applying
   *                          this function will be dumped.
   * @param displayedElements A predicate for selecting states that should be displayed.
   *                          States which are only reachable via non-displayed states are ignored,
   *                          too.
   */
  public static void write(
      Appendable sb,
      final SummaryComputationState rootState,
      final Function<? super SummaryComputationState, ? extends Iterable<SummaryComputationState>> successorFunction,
      final Predicate<? super SummaryComputationState> displayedElements)
      throws IOException {

    SummaryGenToDotWriter toDotWriter = new SummaryGenToDotWriter(sb);
    toDotWriter.writeHeader();
    toDotWriter.writeSubgraph(rootState,
        successorFunction,
        displayedElements);
    toDotWriter.writeFooter();
  }

  private SummaryGenToDotWriter(Appendable pSb) {
    sb = pSb;
  }

  private void writeHeader() throws IOException {

    sb.append("digraph SummaryComputationRequests {\n");
    // default style for nodes
    sb.append("node [style=\"filled\" shape=\"box\" color=\"white\"]\n");
  }


  /**
   * Create String with summary computation requests. in the DOT format of Graphviz.
   * Only the states and edges are written, no surrounding graph definition.
   * @param rootState the root element of the ARG
   * @param successorFunction A function giving all successors of an SummaryComputationState.
   *                          Only states reachable from root by iteratively applying this
   *                          function will be dumped.
   * @param displayedElements A predicate for selecting states that should be displayed.
   *                          States which are only reachable via non-displayed states are ignored,
   *                          too.
   */
  private void writeSubgraph(
      SummaryComputationState rootState,
      Function<? super SummaryComputationState, ? extends Iterable<SummaryComputationState>> successorFunction,
      Predicate<? super SummaryComputationState> displayedElements)
      throws IOException {

    Deque<SummaryComputationState> worklist = new ArrayDeque<>();
    Set<SummaryComputationState> processed = new HashSet<>();
    StringBuilder edges = new StringBuilder();

    worklist.add(rootState);

    while (!worklist.isEmpty()) {
      SummaryComputationState currentElement = worklist.removeLast();
      if (!displayedElements.test(currentElement)) {
        continue;
      }
      if (!processed.add(currentElement)) {
        continue;
      }

      sb.append(determineNode(currentElement));
      sb.append(determineStateHint(currentElement));

      for (SummaryComputationState covered : currentElement.getCoveredByThis()) {
        edges.append(covered.getStateId());
        edges.append(" -> ");
        edges.append(currentElement.getStateId());
        edges.append(" [style=\"dashed\" weight=\"0\" label=\"covered by\"]\n");
      }

      for (SummaryComputationState child : successorFunction.apply(currentElement)) {
        edges.append(determineEdge(currentElement, child));
        worklist.add(child);
      }
    }
    sb.append(edges);
  }

  private String determineEdge(final SummaryComputationState state,
                                      final SummaryComputationState successorState) {
    final StringBuilder builder = new StringBuilder();
    builder.append(state.getStateId()).append(" -> ").append(successorState.getStateId());
    builder.append(" [");

    if (state.getChildren().contains(successorState)) {
      List<CFAEdge> edges = state.getEdgesToChild(successorState);

      // there is no direct edge between the nodes, use a dummy-edge
      if (edges.isEmpty()) {
        builder.append("style=\"bold\" color=\"blue\" label=\"dummy edge\"");

        // edge exists, use info from edge
      } else {
        builder.append("label=\"");
        if (edges.size() > 1) {

          builder
              .append("Lines ")
              .append(edges.get(0).getLineNumber())
              .append(" - ")
              .append(edges.get(edges.size() - 1).getLineNumber());
        } else {
          builder.append("Line ").append(edges.get(0).getLineNumber());
        }
        builder.append(": \\l");

        for (CFAEdge edge : edges) {
          builder.append(edge.getDescription().replaceAll("\n", " ").replace('"', '\''));
          builder.append("\\l");
        }

        builder.append("\"");
      }

      builder.append(" id=\"");
      builder.append(state.getStateId());
      builder.append(" -> ");
      builder.append(successorState.getStateId());
      builder.append("\"");
    }

    builder.append("]\n");
    return builder.toString();
  }

  private void writeFooter() throws IOException {
    sb.append("}\n");
  }

  private static String escapeLabelString(final String rawString) {
    return rawString;
  }

  private static String determineStateHint(final SummaryComputationState currentElement) {

    final String stateNodeId = Integer.toString(currentElement.getStateId());
    final String hintNodeId = stateNodeId + "hint";

    String hintLabel = "";
    final StringBuilder builder = new StringBuilder();

    if (!hintLabel.isEmpty()) {
      builder.append(" {");
      builder.append(" rank=same;\n");

      builder.append(" ");
      builder.append(stateNodeId);
      builder.append(";\n");

      builder.append(" \"");
      builder.append(hintNodeId);
      builder.append("\" [label=\"");
      builder.append(escapeLabelString(hintLabel));
      builder.append("\", shape=box, style=filled, fillcolor=gray];\n");

      builder.append(" ");
      builder.append(stateNodeId);
      builder.append(" -> ");
      builder.append("\"");
      builder.append(hintNodeId);
      builder.append("\"");
      builder.append(" [arrowhead=none, color=gray, style=solid]");
      builder.append(";\n");

      builder.append(" }\n");
    }

    return builder.toString();
  }

  private String determineNode(final SummaryComputationState currentElement) {
    final StringBuilder builder = new StringBuilder();
    builder.append(currentElement.getStateId());
    builder.append(" [");
    final String color = determineColor(currentElement);
    if (color != null) {
      builder.append("fillcolor=\"").append(color).append("\" ");
    }
    builder.append("label=\"").append(determineLabel(currentElement)).append("\" ");
    builder.append("id=\"").append(currentElement.getStateId()).append("\"]\n");
    return builder.toString();
  }

  private String determineLabel(SummaryComputationState currentElement) {
    StringBuilder builder = new StringBuilder();

    builder.append(currentElement.getStateId());

    for (CFANode loc : AbstractStates.extractLocations(currentElement)) {
      builder.append(" @ ");
      builder.append(loc.toString());
      builder.append("\\n");
      builder.append(loc.getFunctionName());
      if (loc instanceof FunctionEntryNode) {
        builder.append(" entry");
      } else if (loc instanceof FunctionExitNode) {
        builder.append(" exit");
      }
      builder.append("\\n");
    }

    builder.append(
        DOTBuilder.escapeGraphvizLabel(currentElement.toDOTLabel(), "\\\\n"));

    return builder.toString().trim();
  }

  private String determineColor(SummaryComputationState currentElement) {
    if (currentElement.isCovered()) {
      return "green";
    }
    if (currentElement.isTarget()) {
      return "red";
    }

    if (!currentElement.isFullyExplored()) {
      return "orange";
    }

    if (currentElement.shouldBeHighlighted()) {
      return "cornflowerblue";
    }

    return null;
  }
}
