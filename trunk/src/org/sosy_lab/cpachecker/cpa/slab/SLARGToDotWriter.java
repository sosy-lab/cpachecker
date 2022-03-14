// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.slab;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSortedSet;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGToDotWriter;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.SlicingAbstractionsUtils;

public class SLARGToDotWriter {

  public static void write(Appendable sb, final Collection<SLARGState> states, String label)
      throws IOException {
    ARGToDotWriter toDotWriter = new ARGToDotWriter(sb);
    for (SLARGState state : states) {
      if (state.isDestroyed()) {
        continue;
      }
      sb.append(determineNode(state));
      // sb.append(determineStateHint(state));
      for (ARGState child : state.getChildren()) {
        sb.append(determineEdge(state, child));
      }
    }
    label = String.format("label=\"%s\";%nlabelloc=top;%nlabeljust=left;%n", label);
    sb.append(label);
    toDotWriter.finish();
  }

  private static CharSequence determineEdge(ARGState state, ARGState successorState) {
    final StringBuilder builder = new StringBuilder();
    builder.append(state.getStateId()).append(" -> ").append(successorState.getStateId());
    builder.append(" [");

    assert state instanceof SLARGState;
    EdgeSet edgeSet = ((SLARGState) state).getEdgeSetToChild(successorState);
    String label;
    if (edgeSet == null) {
      label = "-1";
    } else if (edgeSet.size() == 1) {
      label = edgeSet.choose().toString();
    } else {
      label = String.valueOf(edgeSet.size());
    }

    if (state.getChildren().contains(successorState)) {

      builder.append(String.format("style=\"bold\" color=\"blue\" label=\"%s\"", label));

      builder.append(" id=\"");
      builder.append(state.getStateId());
      builder.append(" -> ");
      builder.append(successorState.getStateId());
      builder.append("\"");
    }

    builder.append("]").append(System.lineSeparator());
    return builder.toString();
  }

  private static CharSequence determineNode(SLARGState pState) {
    final StringBuilder builder = new StringBuilder();
    builder.append(pState.getStateId());
    builder.append(" [");
    final String color = determineColor(pState);
    if (color != null) {
      builder.append("fillcolor=\"").append(color).append("\" ");
    }
    builder.append("label=\"").append(determineLabel(pState));

    Iterable<CFANode> locations = pState.getLocationNodes();
    Collection<Integer> locationNumbers =
        from(locations).transform(CFANode::getNodeNumber).toList();
    builder.append("@N");
    builder.append(generateLocationString(locationNumbers));
    builder.append("\" ");
    builder.append("id=\"").append(pState.getStateId());
    builder.append("\"]").append(System.lineSeparator());
    return builder.toString();
  }

  private static Object determineLabel(SLARGState pState) {
    return pState.getStateId();
  }

  private static String determineColor(SLARGState pState) {
    if (pState.isTarget()) {
      return "red";
    }
    if (pState.isInit()) {
      return "yellow";
    }
    if (!PredicateAbstractState.getPredicateState(pState).isAbstractionState()) {
      return "white";
    }
    if (pState.wasExpanded()) {
      return "cornflowerblue";
    }

    return "orange";
  }

  /**
   * Write out the ranked abstractions in Graphviz format.
   *
   * @param sb Where to write the ARG into
   * @param states States that should be written
   * @param label A text to be show in the top left of the graph
   * @throws IOException Writing to sb failed
   */
  public static void writeRankedAbstractions(
      Appendable sb, final Collection<SLARGState> states, String label) throws IOException {
    ARGState root = FluentIterable.from(states).filter(x -> x.isInit()).toList().get(0);
    int maxrank = 0;
    ARGToDotWriter toDotWriter = new ARGToDotWriter(sb);
    Deque<ARGState> waitlist = new ArrayDeque<>();
    Set<ARGState> reached = new HashSet<>();
    Map<ARGState, Integer> rank = new HashMap<>();
    waitlist.add(root);
    reached.add(root);
    rank.put(root, maxrank);
    while (!waitlist.isEmpty()) {
      ARGState currentState = waitlist.pop();
      if (currentState.isDestroyed()
          || !PredicateAbstractState.getPredicateState(currentState).isAbstractionState()) {
        continue;
      }
      sb.append(determineNode((SLARGState) currentState));
      for (ARGState child :
          SlicingAbstractionsUtils.calculateOutgoingSegments(currentState).keySet()) {
        if (!reached.contains(child)) {
          waitlist.add(child);
          reached.add(child);
        }
        sb.append(determineEdge(currentState, child));
        if (!rank.containsKey(child)) {
          int currentRank = rank.get(currentState) + 1;
          if (currentRank > maxrank) {
            maxrank = maxrank + 1;
          }
          rank.put(child, rank.get(currentState) + 1);
        }
      }
      sb.append(addrank(currentState, rank.get(currentState)));
    }
    for (ARGState state : FluentIterable.from(states).filter(x -> !reached.contains(x)).toList()) {
      if (state.isDestroyed()
          || !PredicateAbstractState.getPredicateState(state).isAbstractionState()) {
        continue;
      }
      sb.append(determineNode((SLARGState) state));
      for (ARGState child : SlicingAbstractionsUtils.calculateOutgoingSegments(state).keySet()) {
        sb.append(determineEdge(state, child));
      }
    }
    for (int i = 0; i < maxrank; i++) {
      sb.append(String.format("invisiblenode_%d->invisiblenode_%d[style=invis]%n", i, i + 1));
    }
    label = String.format("label=\"%s\";%nlabelloc=top;%nlabeljust=left;%n", label);
    sb.append(label);
    toDotWriter.finish();
  }

  private static String addrank(ARGState state, Integer pInteger) {
    return String.format(
        "invisiblenode_%d[style=invis label = \"\"];%n" + "{rank=same;invisiblenode_%d;%d};%n",
        pInteger, pInteger, state.getStateId());
  }

  /*
   * This method can be used to generate a compact String describing a set of integers.
   * Continous ranges will be abbreviated by a dash, e.g. 1-5, non-continous integers will be separated by commas.
   * Example: The integers {1,3,4,5,7} will be written as "1,3-5,7"
   */
  public static StringBuilder generateLocationString(Collection<Integer> pLocationNumbers) {
    ImmutableSortedSet<Integer> locationNumbers =
        from(pLocationNumbers).toSortedSet(Comparator.naturalOrder());
    StringBuilder builder = new StringBuilder();
    int state = 0;
    int lastNumber = -1;
    String separator = ",";
    for (Integer currentLocation : locationNumbers) {
      switch (state) {
        case 0:
          builder.append(currentLocation);
          state = 1;
          break;
        case 1:
          if (currentLocation != lastNumber + 1 || currentLocation.equals(locationNumbers.last())) {
            builder.append(",").append(currentLocation);
            // stay in state 1
          } else {
            state = 2;
          }
          break;
        case 2:
          if (currentLocation != lastNumber + 1) {
            builder.append(separator).append(lastNumber).append(",").append(currentLocation);
            separator = ",";
            state = 1;
          } else if (currentLocation.equals(locationNumbers.last())) {
            builder.append(separator).append(currentLocation);
            state = -1; // we should be finished, next transition would lead to exception
          } else {
            separator = "-";
            // stay in state 2
          }
          break;
        default:
          throw new RuntimeException("Unexpected state in location string generation automaton");
      }
      lastNumber = currentLocation;
    }
    return builder;
  }
}
