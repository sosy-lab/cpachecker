// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckReachesLine;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonVariable.AutomatonIntVariable;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.WaypointRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.WaypointRecord.WaypointType;

public class AutomatonYAMLParserViolation extends AutomatonYAMLParserCommon {

  AutomatonYAMLParserViolation(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCFA)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pCFA);
  }

  private Set<Integer> linesWithExactlyOneEdge() {
    Map<Integer, Integer> lineFrequencies = new HashMap<>();

    // we filter ADeclarationEdges because lines like int x = 5; are broken down into two CFA edges,
    // but we can savely ignore the declaration edge in these cases
    for (CFAEdge edge : CFAUtils.allEdges(cfa).filter(x -> !(x instanceof ADeclarationEdge))) {
      int line = edge.getLineNumber();
      if (lineFrequencies.containsKey(line)) {
        lineFrequencies.put(line, lineFrequencies.get(line) + 1);
      } else {
        int count = lineFrequencies.containsKey(line) ? lineFrequencies.get(line) : 0;
        lineFrequencies.put(line, count + 1);
      }
    }
    Set<Integer> allowedLines =
        lineFrequencies.entrySet().stream()
            .filter(entry -> entry.getValue().equals(1))
            .map(Map.Entry::getKey)
            .collect(ImmutableSet.toImmutableSet());
    return allowedLines;
  }

  Automaton createViolationAutomatonFromEntriesMatchingLines(List<AbstractEntry> pEntries)
      throws InterruptedException, InvalidConfigurationException {
    List<Pair<WaypointRecord, List<WaypointRecord>>> segments = segmentize(pEntries);
    // this needs to be called exactly WitnessAutomaton for the option
    // WitnessAutomaton.cpa.automaton.treatErrorsAsTargets to work m(
    final String automatonName = AutomatonGraphmlParser.WITNESS_AUTOMATON_NAME;

    Set<Integer> allowedLines = linesWithExactlyOneEdge();

    int counter = 0;
    final String initState = getStateName(counter++);

    final List<AutomatonInternalState> automatonStates = new ArrayList<>();
    String currentStateId = initState;
    WaypointRecord follow = null;

    int distance = segments.size();

    for (Pair<WaypointRecord, List<WaypointRecord>> entry : segments) {
      List<AutomatonTransition> transitions = new ArrayList<>();
      follow = entry.getFirst();
      List<WaypointRecord> avoids = entry.getSecond();
      if (!avoids.isEmpty()) {
        logger.log(
            Level.WARNING, "Avoid waypoints in yaml violation witnesses are currently ignored!");
      }
      String nextStateId = getStateName(counter++);
      if (follow.getType().equals(WaypointType.TARGET)) {
        nextStateId = "X";
      }
      int line = follow.getLocation().getLine();
      AutomatonBoolExpr expr = new CheckReachesLine(line);
      AutomatonTransition.Builder builder = new AutomatonTransition.Builder(expr, nextStateId);
      if (follow.getType().equals(WaypointType.ASSUMPTION) && allowedLines.contains(line)) {
        handleConstraint(
            follow.getConstraint().getValue(),
            Optional.ofNullable(follow.getLocation().getFunction()),
            line,
            builder);
      }

      ImmutableList.Builder<AutomatonAction> actionBuilder = ImmutableList.builder();
      actionBuilder.add(
          new AutomatonAction.Assignment(
              AutomatonGraphmlParser.DISTANCE_TO_VIOLATION,
              new AutomatonIntExpr.Constant(distance)));
      builder.withActions(actionBuilder.build());
      transitions.add(builder.build());
      automatonStates.add(
          new AutomatonInternalState(
              currentStateId,
              transitions,
              /* pIsTarget= */ false,
              /* pAllTransitions= */ false,
              /* pIsCycleStart= */ false));

      distance--;
      currentStateId = nextStateId;
    }

    // add last state and stutter in it:
    if (follow != null && follow.getType().equals(WaypointType.TARGET)) {
      automatonStates.add(
          new AutomatonInternalState(
              currentStateId,
              ImmutableList.of(
                  new AutomatonGraphmlParser.TargetInformationCopyingAutomatonTransition(
                      new AutomatonTransition.Builder(AutomatonBoolExpr.TRUE, currentStateId)
                          .withAssertion(createViolationAssertion()))),
              /* pIsTarget= */ false,
              /* pAllTransitions= */ false,
              /* pIsCycleStart= */ false));
    }

    Automaton automaton;
    Map<String, AutomatonVariable> automatonVariables = new HashMap<>();
    AutomatonIntVariable distanceVariable =
        (AutomatonIntVariable)
            AutomatonVariable.createAutomatonVariable(
                "int",
                AutomatonGraphmlParser.DISTANCE_TO_VIOLATION,
                Integer.toString(segments.size() + 1));
    automatonVariables.put(AutomatonGraphmlParser.DISTANCE_TO_VIOLATION, distanceVariable);

    // new AutomatonInternalState(entryStateId, transitions, false, false, true)
    try {
      automaton = new Automaton(automatonName, automatonVariables, automatonStates, initState);
    } catch (InvalidAutomatonException e) {
      throw new WitnessParseException(
          "The witness automaton generated from the provided YAML Witness is invalid!", e);
    }

    automaton = invariantsSpecAutomaton.build(automaton, config, logger, shutdownNotifier, cfa);

    dumpAutomatonIfRequested(automaton);

    return automaton;
  }
}
