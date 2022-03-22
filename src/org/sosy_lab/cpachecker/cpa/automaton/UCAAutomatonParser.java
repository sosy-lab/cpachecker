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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.ucageneration.UCAGenerator;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonVariable.AutomatonIntVariable;

public class UCAAutomatonParser {
  private final LogManager logger;

  public UCAAutomatonParser(LogManager pLogger) {
    this.logger = pLogger;
  }

  public List<Automaton> postProcessUCA(List<Automaton> pAutomata) {

    if (pAutomata.size() != 1) return pAutomata;
    Automaton automaton = pAutomata.get(0);
    if (automaton.getStates().stream()
        .noneMatch(s -> s.getName().equals(UCAGenerator.NAME_OF_ERROR_STATE))) {
      return pAutomata;
    } else {

      // IMPORTANT: we assume at this point, that the UCA is a tree. If not, the heuristic might be
      // imprecises, but as it is only used for guiding, this is ok
      Map<AutomatonInternalState, Integer> distanceToViolation = new HashMap<>();
      List<AutomatonInternalState> parentsToProcess = new ArrayList<>();
      List<AutomatonInternalState> processed = new ArrayList<>();
      automaton.getStates().stream()
          .filter(s -> s.getName().equals(UCAGenerator.NAME_OF_ERROR_STATE))
          .forEach(
              s -> {
                distanceToViolation.put(s, 0);
                parentsToProcess.add(s);
                processed.add(s);
              });

      automaton.getStates().stream()
          .filter(s -> !s.getName().equals(UCAGenerator.NAME_OF_ERROR_STATE))
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

    logger.logf(Level.INFO, "Distances to Violation for UCA are %s", distanceToViolation.toString());

      List<AutomatonInternalState> updatedStates = new ArrayList<>();
      Map<String, AutomatonVariable> variables = new HashMap<>();
      AutomatonIntVariable distanceVariable =
          (AutomatonIntVariable)
              AutomatonVariable.createAutomatonVariable(
                  "int", AutomatonGraphmlParser.DISTANCE_TO_VIOLATION);
      distanceVariable.setValue(
          distanceToViolation.getOrDefault(automaton.getInitialState(), Integer.MIN_VALUE));

      variables.put(AutomatonGraphmlParser.DISTANCE_TO_VIOLATION, distanceVariable);

      for (AutomatonInternalState state : automaton.getStates()) {
        List<AutomatonTransition> updatedTransitions = new ArrayList<>();

        for (AutomatonTransition transition : state.getTransitions()) {
          ImmutableList.Builder<AutomatonAction> actionBuilder = ImmutableList.builder();

          actionBuilder.add(
              new AutomatonAction.Assignment(
                  AutomatonGraphmlParser.DISTANCE_TO_VIOLATION,
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
                state.isTarget(),
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
            "Failed to add the variable %s to the automaton due to %s.\n Returning the original automaton",
            AutomatonGraphmlParser.DISTANCE_TO_VIOLATION,
            Throwables.getStackTraceAsString(pE));
        return pAutomata;
      }
    }
  }
}
