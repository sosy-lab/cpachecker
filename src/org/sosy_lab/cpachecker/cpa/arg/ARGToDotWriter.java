/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg;

import static org.sosy_lab.common.Appenders.appendTo;
import static org.sosy_lab.cpachecker.util.AbstractStates.asIterable;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.rtt.RTTState;
import org.sosy_lab.cpachecker.cpa.seplogic.SeplogicState;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

class ARGToDotWriter {

  private final Appendable sb;

  ARGToDotWriter(Appendable pSb) throws IOException {
    sb = pSb;

    sb.append("digraph ARG {\n");
    // default style for nodes
    sb.append("node [style=\"filled\" shape=\"box\" color=\"white\"]\n");
  }

  /**
   * Create String with ARG in the DOT format of Graphviz.
   * @param sb Where to write the ARG into.
   * @param rootState the root element of the ARG
   * @param successorFunction A function giving all successors of an ARGState. Only states reachable from root by iteratively applying this function will be dumped.
   * @param displayedElements A predicate for selecting states that should be displayed. States which are only reachable via non-displayed states are ignored, too.
   * @param highlightEdge Which edges to highlight in the graph?
   * @throws IOException
   */
  static void write(Appendable sb,
      final ARGState rootState,
      final Function<? super ARGState, ? extends Iterable<ARGState>> successorFunction,
      final Predicate<? super ARGState> displayedElements,
      final Predicate<? super Pair<ARGState, ARGState>> highlightEdge)
      throws IOException {

    ARGToDotWriter toDotWriter = new ARGToDotWriter(sb);
    toDotWriter.writeSubgraph(rootState,
        successorFunction,
        displayedElements,
        highlightEdge);
    toDotWriter.finish();
  }

  /**
   * Create String with ARG in the DOT format of Graphviz.
   * Only the states and edges are written, no surrounding graph definition.
   * @param sb Where to write the ARG into.
   * @param rootState the root element of the ARG
   * @param successorFunction A function giving all successors of an ARGState. Only states reachable from root by iteratively applying this function will be dumped.
   * @param displayedElements A predicate for selecting states that should be displayed. States which are only reachable via non-displayed states are ignored, too.
   * @param highlightEdge Which edges to highlight in the graph?
   * @throws IOException
   */
  void writeSubgraph(final ARGState rootState,
      final Function<? super ARGState, ? extends Iterable<ARGState>> successorFunction,
      final Predicate<? super ARGState> displayedElements,
      final Predicate<? super Pair<ARGState, ARGState>> highlightEdge) throws IOException {

    Deque<ARGState> worklist = new ArrayDeque<>();
    Set<ARGState> processed = new HashSet<>();
    StringBuilder edges = new StringBuilder();

    worklist.add(rootState);

    while (!worklist.isEmpty()) {
      ARGState currentElement = worklist.removeLast();
      if (!displayedElements.apply(currentElement)) {
        continue;
      }
      if (!processed.add(currentElement)) {
        continue;
      }

      String label = determineLabel(currentElement);

      sb.append(""+currentElement.getStateId());
      sb.append(" [");
      String color = determineColor(currentElement);
      if (color != null) {
        sb.append("fillcolor=\"" + color + "\" ");
      }
      sb.append("label=\"" + label +"\" ");
      sb.append("id=\"" + currentElement.getStateId() + "\"");
      sb.append("]");
      sb.append("\n");

      for (ARGState covered : currentElement.getCoveredByThis()) {
        edges.append(covered.getStateId());
        edges.append(" -> ");
        edges.append(currentElement.getStateId());
        edges.append(" [style=\"dashed\" weight=\"0\" label=\"covered by\"]\n");
      }

      for (ARGState child : successorFunction.apply(currentElement)) {
        edges.append(currentElement.getStateId());
        edges.append(" -> ");
        edges.append(child.getStateId());
        edges.append(" [");

        boolean colored = highlightEdge.apply(Pair.of(currentElement, child));
        if (colored) {
          edges.append("color=\"red\"");
        }

        if (currentElement.getChildren().contains(child)) {
          CFAEdge edge = currentElement.getEdgeToChild(child);
          assert edge != null;
          if (colored) {
            edges.append(" ");
          }
          edges.append("label=\"");
          edges.append("Line ");
          edges.append(edge.getLineNumber());
          edges.append(": ");
          edges.append(edge.getDescription().replaceAll("\n", " ").replace('"', '\''));
          edges.append("\"");
          edges.append(" id=\"");
          edges.append(currentElement.getStateId());
          edges.append(" -> ");
          edges.append(child.getStateId());
          edges.append("\"");
        }

        edges.append("]\n");
        worklist.add(child);
      }
    }
    sb.append(edges);
  }

  void writeEdge(ARGState start, ARGState end) throws IOException {
    sb.append("" + start.getStateId());
    sb.append(" -> ");
    sb.append("" + end.getStateId());
    sb.append("\n");
  }

  void enterSubgraph(String name, String label) throws IOException {
    sb.append("subgraph ");
    sb.append(name);
    sb.append(" {\n");

    sb.append("label=\"");
    sb.append(label);
    sb.append("\"\n");
  }

  void leaveSubgraph() throws IOException {
    sb.append("}\n");
  }

  void finish() throws IOException {
    sb.append("}\n");
  }

  private static String determineLabel(ARGState currentElement) {
    StringBuilder builder = new StringBuilder();

    builder.append(currentElement.getStateId());

    CFANode loc = AbstractStates.extractLocation(currentElement);
    if (loc != null) {
      builder.append(" @ ");
      builder.append(loc.toString());
      builder.append("\\n");
      builder.append(loc.getFunctionName());
      if (loc instanceof FunctionEntryNode) {
        builder.append(" entry");
      } else if (loc instanceof FunctionExitNode) {
        builder.append(" exit");
      }
    }

    for (AutomatonState state : asIterable(currentElement).filter(AutomatonState.class)) {
      if (!state.getInternalStateName().equals("Init")) {
        builder.append("\\n");
        builder.append(state.getCPAName().replaceFirst("AutomatonAnalysis_", ""));
        builder.append(": ");
        builder.append(state.getInternalStateName());
      }
    }

    PredicateAbstractState abstraction = AbstractStates.extractStateByType(currentElement, PredicateAbstractState.class);
    if (abstraction != null && abstraction.isAbstractionState()) {
      builder.append("\\n");
      builder.append(abstraction.getAbstractionFormula());
    }

    ExplicitState explicit = AbstractStates.extractStateByType(currentElement, ExplicitState.class);
    if (explicit != null) {
      builder.append("\\n");
      builder.append(explicit.toCompactString());
    }

    RTTState rtt = AbstractStates.extractStateByType(currentElement, RTTState.class);
    if (rtt != null) {
      builder.append("\\n");
      appendTo(builder, rtt);
    }

    SeplogicState sls = AbstractStates.extractStateByType(currentElement, SeplogicState.class);
    if (sls != null) {
      builder.append("\\n");
      builder.append(sls.toString().replaceAll("\\*", "\\\\n*"));
    }

    return builder.toString();
  }

  private static String determineColor(ARGState currentElement) {
    if (currentElement.isCovered()) {
      return "green";
    }
    if (currentElement.isTarget()) {
      return "red";
    }

    if (!currentElement.wasExpanded()) {
      return "orange";
    }

    if (ARGUtils.IMPORTANT_FOR_ANALYSIS.apply(currentElement)) {
      return "cornflowerblue";
    }

    return null;
  }
}