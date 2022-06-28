// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.giageneration.GIAGenerator;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonVariable.AutomatonIntVariable;

public class GIAAutomatonParser {

  public static final String DISTANCE_TO_UNKNOWN = "__DISTANCE_TO_UNKNOWN";

  private final LogManager logger;

  public GIAAutomatonParser(LogManager pLogger) {
    this.logger = pLogger;
  }

  /**
   * Checks if one automaton present and if the automaton is a GIA
   *
   * @param pAutomata the list of automaton to check
   * @return true if the above conditions are matched, false otherwise
   */
  public static boolean isGIA(List<Automaton> pAutomata) {
    return pAutomata.size() == 1
        && pAutomata.get(0).getName().equals(GIAGenerator.ASSUMPTION_AUTOMATON_NAME);
  }

  public List<Automaton> postProcessGIA(List<Automaton> pAutomata) {

    if (pAutomata.size() != 1) {
      return pAutomata;
    }
    Automaton automaton = pAutomata.get(0);
    // Firstly, set the DISTANCE_TO_UNKNOWN for all states
    automaton = computeDistanceToUnknown(automaton);

    if (automaton.getStates().stream()
        .noneMatch(s -> s.getStateType().equals(AutomatonStateTypes.TARGET))) {
      return Lists.newArrayList(automaton);
    } else {

      // IMPORTANT: we assume at this point, that the GIA is a tree. If not, the heuristic might be
      // imprecise, but as it is only used for guiding, this is ok
      Map<AutomatonInternalState, Integer> distanceToViolation = new HashMap<>();
      List<AutomatonInternalState> parentsToProcess = new ArrayList<>();
      List<AutomatonInternalState> processed = new ArrayList<>();
      automaton.getStates().stream()
          .filter(s -> s.getStateType().equals(AutomatonStateTypes.TARGET))
          .forEach(
              s -> {
                distanceToViolation.put(s, 0);
                parentsToProcess.add(s);
                processed.add(s);
              });

      automaton.getStates().stream()
          .filter(s -> !s.getStateType().equals(AutomatonStateTypes.TARGET))
          .forEach(s -> distanceToViolation.put(s, Integer.MIN_VALUE));
      while (!parentsToProcess.isEmpty()) {
        AutomatonInternalState current = parentsToProcess.remove(0);
        for (AutomatonInternalState next :
            automaton.getStates().stream()
                .filter(s -> s.getSuccessorStates().contains(current) && !processed.contains(s))
                .collect(ImmutableList.toImmutableList())) {
          int newDistance = Integer.MIN_VALUE;
          Optional<Integer> optInt =
              next.getSuccessorStates().stream()
                  .map(s -> distanceToViolation.getOrDefault(s, Integer.MIN_VALUE))
                  .max(Integer::compareTo);
          if (optInt.isPresent() && optInt.orElseThrow() != Integer.MIN_VALUE) {
            newDistance = optInt.orElseThrow() - 1;
          }
          distanceToViolation.replace(next, newDistance);
          parentsToProcess.add(next);
          processed.add(next);
        }
      }

      logger.logf(
          Level.FINE, "Distances to Violation for GIA are %s", distanceToViolation.toString());

      return buildAutomaton(
          pAutomata, automaton, distanceToViolation, AutomatonGraphmlParser.DISTANCE_TO_VIOLATION);
    }
  }
  /**
   * Compute DISTANCE_TO_UNKNOWN for all nodes. If no node is marked as unkown, the distance is 0
   * for all nodes. If there is a node in unknown, the distance for that node is 0. For all other
   * nodes, the distance is the number of edges to reach the nearest node in PUnknown. If a node
   * cannot reach a node in pUnknown, the distance is MAX_INT
   *
   * @param pAutomata the initial automaton
   * @return the automaton with updated nodes
   */
  private Automaton computeDistanceToUnknown(Automaton pAutomata) {
    // Initialize all nodes to 0
    Map<AutomatonInternalState, Integer> distances = new HashMap<>();
    ImmutableList<AutomatonInternalState> unknownStates =
        pAutomata.getStates().stream()
            .filter(s -> s.getStateType().equals(AutomatonStateTypes.UNKNOWN))
            .collect(ImmutableList.toImmutableList());
    if (unknownStates.isEmpty()) {
      pAutomata.getStates().forEach(s -> distances.put(s, 0));
      return buildAutomaton(
              Lists.newArrayList(pAutomata), pAutomata, distances, DISTANCE_TO_UNKNOWN)
          .get(0);
    } else {
      // Init all to intMax, the states in unknown to 0.
      pAutomata.getStates().forEach(s -> distances.put(s, Integer.MAX_VALUE));
      unknownStates.forEach(s -> distances.replace(s, 0));

      Map<AutomatonInternalState, Set<AutomatonInternalState>> predesessors = new HashMap<>();
      pAutomata.getStates().forEach(s -> predesessors.put(s, new HashSet<>()));
      pAutomata
          .getStates()
          .forEach(s -> s.getSuccessorStates().forEach(succ -> predesessors.get(succ).add(s)));

      List<AutomatonInternalState> toProcess = new ArrayList<>(unknownStates);
      while (!toProcess.isEmpty()) {
        AutomatonInternalState s = toProcess.remove(0);
        int current_distance = distances.get(s);
        for (AutomatonInternalState succ : predesessors.get(s)) {
          if (distances.get(succ) == Integer.MAX_VALUE) {
            if (!toProcess.contains(succ)) {
              toProcess.add(succ);
            }
            distances.replace(succ, current_distance - 1);
          }
        }
      }
      logger.logf(Level.FINE, "The processed distances to unknown are %s", distances);
      return buildAutomaton(
              Lists.newArrayList(pAutomata), pAutomata, distances, DISTANCE_TO_UNKNOWN)
          .get(0);
    }
  }

  private List<Automaton> buildAutomaton(
      List<Automaton> pAutomata,
      Automaton automaton,
      Map<AutomatonInternalState, Integer> distanceToViolation,
      String pVariableName) {
    List<AutomatonInternalState> updatedStates = new ArrayList<>();
    Map<String, AutomatonVariable> variables = new HashMap<>(automaton.getInitialVariables());
    AutomatonIntVariable distanceVariable =
        (AutomatonIntVariable) AutomatonVariable.createAutomatonVariable("int", pVariableName);
    distanceVariable.setValue(
        distanceToViolation.getOrDefault(automaton.getInitialState(), Integer.MIN_VALUE));

    variables.put(pVariableName, distanceVariable);

    for (AutomatonInternalState state : automaton.getStates()) {
      List<AutomatonTransition> updatedTransitions = new ArrayList<>();

      for (AutomatonTransition transition : state.getTransitions()) {
        ImmutableList.Builder<AutomatonAction> actionBuilder = ImmutableList.builder();

        actionBuilder.add(
            new AutomatonAction.Assignment(
                pVariableName,
                new AutomatonIntExpr.Constant(
                    distanceToViolation.getOrDefault(
                        transition.getFollowState(), Integer.MIN_VALUE))));

        AutomatonTransition.Builder builder =
            new AutomatonTransition.Builder(transition, actionBuilder);
        updatedTransitions.add(builder.build());
      }
      updatedStates.add(
          new AutomatonInternalState(
              state.getName(),
              updatedTransitions,
              state.getStateType(),
              state.isNonDetState(),
              state.isNontrivialCycleStart(),
              state.getStateInvariants()));
    }

    try {
      return Lists.newArrayList(
          new Automaton(
              automaton.getName(),
              variables,
              updatedStates,
              automaton.getInitialState().getName()));
    } catch (InvalidAutomatonException pE) {
      logger.logf(
          Level.WARNING,
          "Failed to add the variable %s to the automaton due to %s.\n"
              + " Returning the original automaton",
          AutomatonGraphmlParser.DISTANCE_TO_VIOLATION,
          Throwables.getStackTraceAsString(pE));
      return pAutomata;
    }
  }
}
