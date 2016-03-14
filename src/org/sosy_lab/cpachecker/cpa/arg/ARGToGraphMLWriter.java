/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;

import org.sosy_lab.cpachecker.util.Pair;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides the possibility to export the ARG into a GraphML file
 */
public class ARGToGraphMLWriter {

  private final Appendable sb;

  ARGToGraphMLWriter(final Appendable pStringBuffer) throws IOException {
    sb = pStringBuffer;

    appendDocHeader();
  }

  /**
   * Create String with ARG in the GraphML format of yEd.
   *
   * @param pStringBuffer      Where to write the ARG into.
   * @param pRootState         The root element of the ARG.
   * @param pSuccessorFunction A function giving all successors of an {@code ARGState}. Only states
   *                           reachable from root by iteratively applying this function will be
   *                           dumped.
   * @param pDisplayedElements A predicate for selecting states that should be displayed. States
   *                           which are only reachable via non-displayed states are ignored, too.
   * @param pHighlightEdge     Which edges to highlight in the graph.
   * @throws IOException In case of an IO problem.
   */
  public static void write(
      final Appendable pStringBuffer,
      final ARGState pRootState,
      final Function<? super ARGState, ? extends Iterable<ARGState>> pSuccessorFunction,
      final Predicate<? super ARGState> pDisplayedElements,
      final Predicate<? super Pair<ARGState, ARGState>> pHighlightEdge)
      throws IOException {

    ARGToGraphMLWriter toGraphMLWriter = new ARGToGraphMLWriter(pStringBuffer);
    toGraphMLWriter.writeSubgraph(
        pRootState, pSuccessorFunction, pDisplayedElements, pHighlightEdge);
    toGraphMLWriter.finish();
  }

  /**
   * Create String with ARG in the GraphML format of yEd.
   *
   * @param pStringBuffer      Where to write the ARG into.
   * @param pRootStates        The root elements of the ARGs.
   * @param pConnections       Start- and end-points of edges between separate graphs.
   * @param pSuccessorFunction A function giving all successors of an {@code ARGState}. Only states
   *                           reachable from root by iteratively applying this function will be
   *                           dumped.
   * @param pDisplayedElements A predicate for selecting states that should be displayed. States
   *                           which are only reachable via non-displayed states are ignored, too.
   * @param pHighlightEdge     Which edges to highlight in the graph?
   * @throws IOException In case of an IO problem
   */
  public static void write(
      final Appendable pStringBuffer,
      final Set<ARGState> pRootStates,
      final Multimap<ARGState, ARGState> pConnections,
      final Function<? super ARGState, ? extends Iterable<ARGState>> pSuccessorFunction,
      final Predicate<? super ARGState> pDisplayedElements,
      final Predicate<? super Pair<ARGState, ARGState>> pHighlightEdge)
      throws IOException {

    ARGToGraphMLWriter toGraphMLWriter = new ARGToGraphMLWriter(pStringBuffer);
    for (ARGState rootState : pRootStates) {
      toGraphMLWriter.enterSubgraph(
          "cluster_" + rootState.getStateId(), "reachedset_" + rootState.getStateId());
      toGraphMLWriter.writeSubgraph(
          rootState, pSuccessorFunction, pDisplayedElements, pHighlightEdge);
      toGraphMLWriter.leaveSubgraph();
    }

    for (Map.Entry<ARGState, ARGState> connection : pConnections.entries()) {
      toGraphMLWriter.addConnection(
          connection.getKey().getStateId(), connection.getValue().getStateId());
    }

    toGraphMLWriter.finish();
  }

  void writeSubgraph(
      final ARGState pRootState,
      final Function<? super ARGState, ? extends Iterable<ARGState>> pSuccessorFunction,
      final Predicate<? super ARGState> pDisplayedElements,
      final Predicate<? super Pair<ARGState, ARGState>> pHightlightEdge)
      throws IOException {

    Deque<ARGState> workList = new ArrayDeque<>();
    Set<ARGState> processed = new HashSet<>();
    StringBuilder edges = new StringBuilder();

    workList.add(pRootState);

    while (!workList.isEmpty()) {
      ARGState currentElement = workList.removeLast();
      if (!pDisplayedElements.apply(currentElement)) {
        continue;
      }
      if (!processed.add(currentElement)) {
        continue;
      }

      createNodeForElement(currentElement);

      for (ARGState covered : currentElement.getCoveredByThis()) {
        // dashed line for covering
      }

      for (ARGState child : pSuccessorFunction.apply(currentElement)) {
        edges.append(determineEdge(pHightlightEdge, currentElement, child));
        workList.add(child);
      }
    }

    sb.append(edges);
  }

  void enterSubgraph(final String pClusterStateID, final String pReachedSetStateId)
      throws IOException {}

  void leaveSubgraph() throws IOException {}

  void addConnection(final int pStartStateID, final int pEndStateID) throws IOException {}

  private void appendDocHeader() throws IOException {
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
    sb.append(
        "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\""
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            + "xmlns:y=\"http://www.yworks.com/xml/graphml\""
            + "xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns "
            + "http://www.yworks.com/xml/schema/graphml/1.0/ygraphml.xsd\">\n");
    sb.append("  <key for=\"node\" id=\"d0\" yfiles.type=\"nodegraphics\"/>\n");
    sb.append("  <key attr.name=\"description\" attr.type=\"string\" for=\"node\" id=\"d1\"/>\n");
    sb.append("  <key for=\"edge\" id=\"d2\" yfiles.type=\"edgegraphics\"/>\n");
    sb.append("  <key attr.name=\"description\" attr.type=\"string\" for=\"edge\" id=\"d3\"/>\n");
    sb.append("  <key for=\"graphml\" id=\"d4\" yfiles.type=\"resources\"/>\n\n");
  }

  private void finish() throws IOException {
    sb.append("  </graph>\n");
    sb.append("  <data key=\"d4\">\n");
    sb.append("    <y:Resources/>\n");
    sb.append("  </data>\n");
    sb.append("</graphml>\n");
  }

  private void createNodeForElement(final ARGState pElement) {}

  private static String determineEdge(
      final Predicate<? super Pair<ARGState, ARGState>> pHightlightEdge,
      final ARGState pState,
      final ARGState pSuccessorState) {
    final StringBuilder builder = new StringBuilder();

    return builder.toString();
  }

  private static String determineColor(final ARGState pCurrentElement) {
    if (pCurrentElement.isCovered()) {
      return "#008000";
    }
    if (pCurrentElement.isTarget()) {
      return "#FF0000";
    }
    if (!pCurrentElement.wasExpanded()) {
      return "#FFA500";
    }
    if (pCurrentElement.shouldBeHighlighted()) {
      return "#6495ED";
    }

    return "#FFFFFF";
  }
}
