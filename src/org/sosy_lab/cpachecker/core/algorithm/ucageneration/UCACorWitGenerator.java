// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.ucageneration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ucageneration.UCAGenerator.UCAGeneratorOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

public class UCACorWitGenerator {

  private final LogManager logger;
  private UCAGeneratorOptions optinons;

  public UCACorWitGenerator(LogManager pLogger, UCAGeneratorOptions pOptions)
      throws InvalidConfigurationException {
    this.logger = pLogger;
    this.optinons = pOptions;
  }

  int produceUCA4CorrectnessWitness(Appendable output, UnmodifiableReachedSet reached)
      throws IOException {
    final AbstractState firstState = reached.getFirstState();
    if (!(firstState instanceof ARGState)
        || UCAGenerator.getWitnessAutomatonState(firstState).isEmpty()) {
      output.append("Cannot dump assumption as automaton if ARGCPA is not used.");
    }

    // check, if the uca that should be generated (e.g. for a violation witness)
    // matches the reached set (meaning that the reached set contians at least
    // location with a property violation according to the specification

    // We do not need to care about error states!

    // Goal: generate a set of the form (AutomatonState --EDGE--> AutomatonState)
    // and the root node

    final ARGState argRoot = (ARGState) reached.getFirstState();
    AutomatonState rootState = UCAGenerator.getWitnessAutomatonState(argRoot).orElseThrow();

    Set<UCAAutomatonStateEdge> edgesInAutomatonStates = new HashSet<>();
    Set<UCAARGStateEdge> edgesToAdd = new HashSet<>();

    // Next, filter the reached set fo all states, that have a different automaton
    // state compared to their predecessors, as these are the states that need to be stored in
    // the  uca

    for (AbstractState s : reached.asCollection()) {
      Optional<AutomatonState> automatonStateOpt = UCAGenerator.getWitnessAutomatonState(s);
      if (automatonStateOpt.isEmpty()) {
        logger.log(
            Level.WARNING,
            String.format("Cannot export state %s, as no AutomatonState is present", s));
        continue;
      }
      AutomatonState currentAutomatonState = automatonStateOpt.orElseThrow();
      @Nullable ARGState argState = AbstractStates.extractStateByType(s, ARGState.class);
      if (Objects.isNull(argState)) {
        logger.log(
            Level.WARNING, String.format("Cannot export state %s, as it is not an ARG State", s));
        continue;
      }

      Set<Pair<ARGState, AutomatonState>> parentsWithOtherAutomatonState =
          Sets.newConcurrentHashSet();

      for (ARGState parent : argState.getParents()) {
        Optional<AutomatonState> parentAutomatonState =
            UCAGenerator.getWitnessAutomatonState(parent);
        // If parent node has a automaton state and this is differnt to the one of the
        // child, add the child to statesWithNewAutomatonState
        if (parentAutomatonState.isPresent()
            && !parentAutomatonState.orElseThrow().equals(currentAutomatonState)
            && // automaton state is not already present in  parentsWithOtherAutomatonState
            parentsWithOtherAutomatonState.stream()
                .map(pair -> pair.getSecond())
                .noneMatch(state -> parentAutomatonState.orElseThrow().equals(state))) {
          parentsWithOtherAutomatonState.add(Pair.of(parent, parentAutomatonState.orElseThrow()));
        }
      }
      if (!parentsWithOtherAutomatonState.isEmpty()) {
        for (Pair<ARGState, AutomatonState> parentPair : parentsWithOtherAutomatonState) {
          // Create the edge
          CFAEdge edge = UCAGenerator.getEdge(parentPair, argState);
          edgesInAutomatonStates.add(
              new UCAAutomatonStateEdge(parentPair.getSecond(), currentAutomatonState, edge));
          edgesToAdd.add(new UCAARGStateEdge(parentPair.getFirst(), AbstractStates.extractStateByType(s, ARGState.class),edge ));

          // Check, if the parent node has any other outgoing edges, they have to be added aswell
          for (CFAEdge otherEdge :
              CFAUtils.leavingEdges(AbstractStates.extractLocation(parentPair.getFirst()))) {
            if (!otherEdge.equals(edge)) {
              edgesInAutomatonStates.add(new UCAAutomatonStateEdge(parentPair.getSecond(), otherEdge));
            }
          }
        }
      }
    }

    logger.log(
        Level.FINE, edgesInAutomatonStates.stream().map(e -> e.toString()).collect(Collectors.joining("\n")));

    return writeUCAForViolationWitness(
        output, argRoot, edgesToAdd, optinons.isAutomatonIgnoreAssumptions());
  }

  /**
   * Create an UCA for the given set of edges Beneth printing the edges, each node gets a self-loop
   * and a node to the temp-location
   *
   * @param sb the appendable to print to
   * @param rootState the root state of the automaton
   * @param edgesToAdd the edges between states to add
   * @throws IOException if the file cannot be accessed or does not exist
   */
  private int writeUCAForViolationWitness(
      Appendable sb, ARGState rootState, Set<UCAARGStateEdge> edgesToAdd, boolean ignoreAssumptions)
      throws IOException {
    int numProducedStates = 0;
    sb.append("OBSERVER AUTOMATON AssumptionAutomaton\n\n");

    String actionOnFinalEdges = "";

    UCAGenerator.storeInitialNode(sb, edgesToAdd.isEmpty(), UCAGenerator.getName(rootState));
    if (ignoreAssumptions) {
      sb.append(String.format("    TRUE -> GOTO %s;\n\n", UCAGenerator.NAME_OF_TEMP_STATE));
    } else {
      sb.append(
          String.format(
              "    TRUE -> ASSUME {false} GOTO %s;\n\n", UCAGenerator.NAME_OF_TEMP_STATE));
    }

    // Fill the map to be able to iterate over the nodes
    Map<ARGState, Set<UCAARGStateEdge>> nodesToEdges = new HashMap<>();
    edgesToAdd.forEach(
        e -> {
          if (nodesToEdges.containsKey(e.getSource())) {
            nodesToEdges.get(e.getSource()).add(e);
          } else {
            nodesToEdges.put(e.getSource(), Sets.newHashSet(e));
          }
        });

    for (final ARGState currentState :
        nodesToEdges.keySet().stream()
            .sorted(Comparator.comparing(UCAGenerator::getName))
            .collect(ImmutableList.toImmutableList())) {

      sb.append(String.format("STATE USEALL %s :\n", UCAGenerator.getName(currentState)));
      numProducedStates++;

      for (UCAARGStateEdge edge : nodesToEdges.get(currentState)) {

        sb.append("    MATCH \"");
        AssumptionCollectorAlgorithm.escape(UCAGenerator.getEdgeString(edge.getEdge()), sb);
        sb.append("\" -> ");
        sb.append(String.format("GOTO %s", edge.getTargetName()));
        sb.append(";\n");
      }
      // Add a edge to __TRUE, as all states are accepting
      sb.append("    TRUE -> " + "GOTO __TRUE;\n");
      if (!currentState.isTarget()) {
        sb.append(
            String.format(
                "    TRUE -> " + actionOnFinalEdges + "GOTO %s;\n",
                UCAGenerator.getName(currentState)));
      }
      @Nullable AssumptionStorageState assumptionState =
          AbstractStates.extractStateByType(currentState, AssumptionStorageState.class);
      if (Objects.nonNull(assumptionState) && !assumptionState.isAssumptionTrue()) {
        // Add indent to be able to use util mehtods
        // TODO: Refactor if CUP-grammer is refactored
        sb.append("    ");
        AssumptionCollectorAlgorithm.addAssumption(
            sb, assumptionState, false, AbstractStates.extractLocation(currentState));
        sb.append("\n");
      } sb.append("\n");
    }

    sb.append("END AUTOMATON\n");

    return numProducedStates;
  }
}
