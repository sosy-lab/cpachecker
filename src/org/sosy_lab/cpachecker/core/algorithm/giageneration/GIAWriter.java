// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.giageneration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class GIAWriter<T extends AbstractState> {

  private final boolean stopAtUnknownStates;

  public GIAWriter(boolean pStopAtUnknownStates) {
    this.stopAtUnknownStates = pStopAtUnknownStates;
  }

  /**
   * Create an GIA for the given set of edges Beneth printing the edges, each node gets a self-loop
   * and a node to the temp-location
   *
   * @param sb the appendable to print to
   * @param rootState the root state of the automaton
   * @param edgesToAdd the edges between states to add
   * @param pTargetStates the target states
   * @param pNonTargetStates the non target states
   * @param pUnknownStates the unknwon states
   * @throws IOException if the file cannot be accessed or does not exist
   */
  public int writeGIA(
      Appendable sb,
      T rootState,
      Set<GIAARGStateEdge<T>> edgesToAdd,
      Set<T> pTargetStates,
      Set<T> pNonTargetStates,
      Set<T> pUnknownStates)
      throws IOException, InterruptedException {
    int numProducedStates = 0;
    sb.append(GIAGenerator.AUTOMATON_HEADER);

    String actionOnFinalEdges = "";

    GIAGenerator.storeInitialNode(sb, edgesToAdd.isEmpty(), GIAGenerator.getName(rootState));
    sb.append(String.format("    TRUE -> GOTO %s;\n\n", GIAGenerator.NAME_OF_TEMP_STATE));

    if (setIsReached(pTargetStates, edgesToAdd)) {
      sb.append(String.format("TARGET STATE %s :\n", GIAGenerator.NAME_OF_ERROR_STATE));
      sb.append(String.format("    TRUE -> GOTO %s;\n\n", GIAGenerator.NAME_OF_ERROR_STATE));
    }

    if (setIsReached(pNonTargetStates, edgesToAdd)) {
      sb.append(String.format("NON_TARGET STATE %s :\n", GIAGenerator.NAME_OF_FINAL_STATE));
      sb.append(String.format("    TRUE -> GOTO %s;\n\n", GIAGenerator.NAME_OF_FINAL_STATE));
    }

    if (setIsReached(pUnknownStates, edgesToAdd) && stopAtUnknownStates) {
      sb.append(String.format("UNKNOWN STATE %s :\n", GIAGenerator.NAME_OF_UNKNOWN_STATE));
      sb.append(String.format("    TRUE -> GOTO %s;\n\n", GIAGenerator.NAME_OF_UNKNOWN_STATE));
    }

    // Fill the map to be able to iterate over the nodes
    Map<T, List<GIAARGStateEdge<T>>> nodesToEdges = new HashMap<>();
    edgesToAdd.forEach(
        e -> {
          if (nodesToEdges.containsKey(e.getSource())) {
            nodesToEdges.get(e.getSource()).add(e);
          } else {
            nodesToEdges.put(e.getSource(), Lists.newArrayList(e));
          }
        });

    for (final T currentState :
        nodesToEdges.keySet().stream()
            .sorted(Comparator.comparing(GIAGenerator::getName))
            .collect(ImmutableList.toImmutableList())) {

      sb.append(
          GIAGenerator.getDefStringForState(
              currentState, pTargetStates, pNonTargetStates, pUnknownStates));
      numProducedStates++;
      boolean otherwiseEdgesNotAdded = true;
      // Only add a node if it is neither in F_N nor F_NT
      if (!pTargetStates.contains(currentState) && !pNonTargetStates.contains(currentState)) {
        List<GIAARGStateEdge<T>> edges = nodesToEdges.get(currentState);

        edges = sortEdges(edges, pTargetStates, pNonTargetStates, pUnknownStates);

        for (GIAARGStateEdge<T> edge : edges) {
          boolean thisEdgeIsOtherwise =
              edge.generateTransition(
                  sb, pTargetStates, pNonTargetStates, pUnknownStates, stopAtUnknownStates);
          otherwiseEdgesNotAdded = otherwiseEdgesNotAdded && !thisEdgeIsOtherwise;
        }
      }
      //     if (pTargetStates.contains(currentState) || pNonTargetStates.contains(currentState)
      //      || pUnknownStates.contains(currentState)){
      if (otherwiseEdgesNotAdded) {
        sb.append(
            String.format(
                "    MATCH OTHERWISE -> " + actionOnFinalEdges + "GOTO %s;\n",
                GIAGenerator.getName(currentState)));
      }
      //        sb.append("    TRUE -> " + actionOnFinalEdges + "GOTO __TRUE;\n\n");
      //      }
      sb.append("\n");
    }
    sb.append("END AUTOMATON\n");

    return numProducedStates;
  }

  private List<GIAARGStateEdge<T>> sortEdges(
      List<GIAARGStateEdge<T>> pEdges,
      Set<T> pTargetStates,
      Set<T> pNonTargetStates,
      Set<T> pUnknownStates) {
    // check if the edges can be clenaed up, because they contain multiple otherwise edges to the
    // same target
    if (cleanUp(pEdges, pTargetStates, pNonTargetStates, pUnknownStates)) {
      return pEdges.subList(0, 1);
    }

    List<GIAARGStateEdge<T>> notOtherwise = new ArrayList<>();
    List<GIAARGStateEdge<T>> otherwise = new ArrayList<>();
    for (GIAARGStateEdge<T> edge : pEdges) {
      StringBuilder sb = new StringBuilder();
      try {

        //noinspection ResultOfMethodCallIgnored
        edge.generateTransition(
            sb, pTargetStates, pNonTargetStates, pUnknownStates, stopAtUnknownStates);
        if (sb.toString().contains("MATCH OTHERWISE ->")) {
          otherwise.add(edge);
        } else {
          notOtherwise.add(edge);
        }
      } catch (IOException | InterruptedException pE) {
        notOtherwise.add(edge);
      }
    }
    notOtherwise.addAll(otherwise);
    return notOtherwise;
  }

  private boolean cleanUp(
      List<GIAARGStateEdge<T>> pEdges,
      Set<T> pTargetStates,
      Set<T> pNonTargetStates,
      Set<T> pUnknownStates) {
    if (pEdges.isEmpty()) return false;
    StringBuilder sb = new StringBuilder();
    try {
      //noinspection ResultOfMethodCallIgnored
      pEdges
          .get(0)
          .generateTransition(
              sb, pTargetStates, pNonTargetStates, pUnknownStates, stopAtUnknownStates);
      String resOfFirst = sb.toString();

      for (int i = 1; i < pEdges.size(); i++) {
        sb = new StringBuilder();
        //noinspection ResultOfMethodCallIgnored
        pEdges
            .get(i)
            .generateTransition(
                sb, pTargetStates, pNonTargetStates, pUnknownStates, stopAtUnknownStates);
        if (!resOfFirst.equals(sb.toString())) {
          return false;
        }
      }
    } catch (IOException | InterruptedException pE) {
      return false;
    }
    return true;
  }

  private boolean setIsReached(Set<T> pSet, Set<GIAARGStateEdge<T>> pEdgesToAdd) {
    return pEdgesToAdd.stream()
        .anyMatch(e -> e.getTarget().isPresent() && pSet.contains(e.getTarget().orElseThrow()));
  }
}
