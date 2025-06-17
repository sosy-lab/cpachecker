// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessParserUtils.InvalidYAMLWitnessException;
import org.sosy_lab.cpachecker.util.ast.ASTElement;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ViolationSequenceEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord.WaypointAction;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord.WaypointType;

class AutomatonWitnessViolationV21Parser extends AutomatonWitnessViolationV2Parser {
  AutomatonWitnessViolationV21Parser(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCFA)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pCFA);
  }

  /**
   * Separate the entries into segments and check whether the witness is valid witness v2.1
   *
   * @param pEntries the entries to segmentize
   * @return the segmentized entries
   * @throws InvalidYAMLWitnessException if the YAML witness is not valid
   */
  @Override
  protected ImmutableList<PartitionedWaypoints> segmentizeAndCheck(List<AbstractEntry> pEntries)
      throws InvalidYAMLWitnessException {
    for (AbstractEntry entry : pEntries) {
      if (entry instanceof ViolationSequenceEntry violationEntry) {
        ImmutableList<PartitionedWaypoints> segmentizedEntries = segmentize(violationEntry);
        checkCycleOrTargetAtEnd(violationEntry);
        return segmentizedEntries;
      }
      break; // for now just take the first ViolationSequenceEntry in the witness V2
    }
    return ImmutableList.of();
  }

  @Override
  Automaton createViolationAutomatonFromEntries(List<AbstractEntry> pEntries)
      throws InterruptedException, InvalidYAMLWitnessException, WitnessParseException {
    List<PartitionedWaypoints> segments = segmentizeAndCheck(pEntries);
    // this needs to be called exactly WitnessAutomaton for the option
    // WitnessAutomaton.cpa.automaton.treatErrorsAsTargets to work
    final String automatonName = AutomatonGraphmlParser.WITNESS_AUTOMATON_NAME;

    // TODO: It may be worthwhile to refactor this into the CFA
    ImmutableListMultimap<Integer, @NonNull CFAEdge> startLineToCFAEdge =
        FluentIterable.from(cfa.edges())
            .index(edge -> edge.getFileLocation().getStartingLineNumber());

    int stateCounter = 0;
    final String initState = getStateName(stateCounter++);

    final ImmutableList.Builder<AutomatonInternalState> automatonStates =
        new ImmutableList.Builder<>();
    String currentStateId = initState;

    int distance = segments.size() - 1;
    String cycleHeadName = "";

    for (PartitionedWaypoints entry : segments) {
      ImmutableList.Builder<AutomatonTransition> transitions = new ImmutableList.Builder<>();
      // We call flow waypoint either cycle or follow waypoint as they ensure flow in the execution
      WaypointRecord flowWaypoint = entry.follow() != null ? entry.follow() : entry.cycle();
      List<WaypointRecord> avoids = entry.avoids();
      if (!avoids.isEmpty()) {
        logger.log(
            Level.WARNING, "Avoid waypoints in violation witnesses V2 are currently ignored!");
      }
      String nextStateId = getStateName(stateCounter++);
      int followLine = flowWaypoint.getLocation().getLine();
      int followColumn = flowWaypoint.getLocation().getColumn();

      if (flowWaypoint.getType().equals(WaypointType.TARGET)) {
        nextStateId = "X";
        transitions.add(handleTarget(nextStateId, followLine, followColumn, distance));
        if (stateCounter != segments.size() + 1) {
          logger.log(
              Level.INFO,
              "Target waypoint is not the last waypoint, following waypoints will be ignored!");
        }
        // Add the state directly, since we are exiting the loop afterwards
        automatonStates.add(
            new AutomatonInternalState(
                currentStateId,
                transitions.build(),
                /* pIsTarget= */ false,
                /* pAllTransitions= */ false,
                /* pIsCycleStart= */ false));
        currentStateId = nextStateId;
        break;
      } else if (flowWaypoint.getType().equals(WaypointType.ASSUMPTION)) {
        ASTElement element =
            cfa.getAstCfaRelation().getTightestStatementForStarting(followLine, followColumn);
        transitions.add(
            handleAssumption(
                nextStateId,
                element,
                followLine,
                flowWaypoint.getLocation().getFunction(),
                distance,
                flowWaypoint.getConstraint().getValue()));
      } else if (flowWaypoint.getType().equals(WaypointType.BRANCHING)) {
        AstCfaRelation astCFARelation = cfa.getAstCfaRelation();
        Verify.verifyNotNull(astCFARelation);

        Optional<List<AutomatonTransition>> ifStatementTransitions =
            handleFollowWaypointAtStatement(
                astCFARelation,
                nextStateId,
                followColumn,
                followLine,
                distance,
                Boolean.parseBoolean(flowWaypoint.getConstraint().getValue()));

        if (ifStatementTransitions.isEmpty()) {
          logger.log(Level.INFO, "Could not handle branching waypoint, skipping it");
          continue;
        }

        transitions.addAll(ifStatementTransitions.orElseThrow());
      } else if (flowWaypoint.getType().equals(WaypointType.FUNCTION_RETURN)) {
        transitions.add(
            handleFunctionReturn(
                nextStateId,
                followLine,
                followColumn,
                distance,
                flowWaypoint.getConstraint().getValue(),
                startLineToCFAEdge));

      } else {
        logger.log(Level.WARNING, "Unknown waypoint type: " + flowWaypoint.getType());
        continue;
      }

      if (flowWaypoint.getAction().equals(WaypointAction.CYCLE) && cycleHeadName.isEmpty()) {
        cycleHeadName = currentStateId;
      }

      automatonStates.add(
          new AutomatonInternalState(
              currentStateId,
              transitions.build(),
              /* pIsTarget= */ false,
              /* pAllTransitions= */ false,
              /* pIsCycleStart= */ currentStateId.equals(cycleHeadName)));

      distance--;
      currentStateId = nextStateId;
    }

    // If there is no cycle in the witness, it is a reachability witness
    if (cycleHeadName.isEmpty()) {
      // add last state and stutter in it:
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
    } else {
      // add last state and a transition to enclose the cycle
      automatonStates.add(
          new AutomatonInternalState(
              currentStateId,
              ImmutableList.of(
                  new AutomatonTransition.Builder(AutomatonBoolExpr.TRUE, cycleHeadName).build()),
              /* pIsTarget= */ false,
              /* pAllTransitions= */ false,
              /* pIsCycleStart= */ false));
    }

    ImmutableMap<String, AutomatonVariable> automatonVariables =
        ImmutableMap.of(
            AutomatonGraphmlParser.DISTANCE_TO_VIOLATION,
            AutomatonVariable.createAutomatonVariable(
                /* pType= */ "int",
                AutomatonGraphmlParser.DISTANCE_TO_VIOLATION,
                Integer.toString(segments.size())));

    Automaton automaton;
    try {
      automaton =
          new Automaton(automatonName, automatonVariables, automatonStates.build(), initState);
    } catch (InvalidAutomatonException e) {
      throw new WitnessParseException(
          "The witness automaton generated from the provided Witness V2 is invalid!", e);
    }

    automaton =
        getInvariantsSpecAutomaton().build(automaton, config, logger, shutdownNotifier, cfa);

    dumpAutomatonIfRequested(automaton);

    return automaton;
  }
}
