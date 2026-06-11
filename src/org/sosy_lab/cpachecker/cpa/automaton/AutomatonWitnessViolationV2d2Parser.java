// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import static org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.THREAD_ID_VAR_NAME;
import static org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.getThreadIdAssignment;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckPassesThroughNodes;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils.InvalidYAMLWitnessException;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.SegmentRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ViolationSequenceEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord.WaypointAction;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord.WaypointType;

class AutomatonWitnessViolationV2d2Parser extends AutomatonWitnessViolationV2d0Parser {
  AutomatonWitnessViolationV2d2Parser(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCFA)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pCFA);
  }

  @Override
  protected void checkTargetIsAtEnd(WaypointRecord pLatest, int pNumTargetWaypoints)
      throws InvalidYAMLWitnessException {
    switch (pNumTargetWaypoints) {
      case 0 -> throw new InvalidYAMLWitnessException("No target waypoint in witness V2!");
      case 1, 2 -> {
        if (pLatest != null && !pLatest.getType().equals(WaypointType.TARGET)) {
          throw new InvalidYAMLWitnessException("Target waypoint is not at the end in witness V2!");
        }
      }
      default ->
          throw new InvalidYAMLWitnessException("More than one target waypoint in witness V2!");
    }
  }

  /**
   * Separate the entries into segments whose waypoints should be passed one after the other
   *
   * @param pViolationEntry the violation entry to segmentize
   * @return the segmentized entries
   */
  @Override
  ImmutableList<PartitionedWaypoints> segmentize(ViolationSequenceEntry pViolationEntry)
      throws InvalidYAMLWitnessException {
    ImmutableList.Builder<PartitionedWaypoints> segments = new ImmutableList.Builder<>();

    for (SegmentRecord segmentRecord : pViolationEntry.getContent()) {
      boolean containsCycle = false;
      boolean containsFollowOrCycle = false;
      ImmutableList.Builder<WaypointRecord> avoids = new ImmutableList.Builder<>();
      ImmutableList.Builder<WaypointRecord> follows = new ImmutableList.Builder<>();
      for (WaypointRecord waypoint : segmentRecord.getSegment()) {
        if (waypoint.getAction().equals(WaypointAction.AVOID)) {
          avoids.add(waypoint);
        } else {
          if (containsCycle) {
            throw new InvalidYAMLWitnessException(
                "Witnesses in version 2.2 can contain at most one follow or cycle waypoint per"
                    + " segment!");
          }
          containsFollowOrCycle = true;
          if (waypoint.getAction().equals(WaypointAction.FOLLOW)) {
            follows.add(waypoint);
          } else if (waypoint.getAction().equals(WaypointAction.CYCLE)) {
            containsCycle = true;
            // TODO: It is a bug to build it here, since there may be avoid
            //  waypoints which are not collected
            segments.add(new PartitionedWaypoints(avoids.build(), waypoint));
          }
        }
      }
      if (!containsFollowOrCycle) {
        throw new InvalidYAMLWitnessException(
            "Every segment in witness version 2.1 must contain follow or cycle waypoint!");
      }
      segments.add(new PartitionedWaypoints(follows.build(), avoids.build()));
    }
    return segments.build();
  }

  /**
   * Separate the entries into segments and check whether the witness is valid witness v2.1
   *
   * @param pEntries the entries to segmentize
   * @return the segmentized entries
   * @throws InvalidYAMLWitnessException if the YAML witness is not valid
   */
  protected ImmutableList<PartitionedWaypoints> segmentizeAndCheckV2d2(List<AbstractEntry> pEntries)
      throws InvalidYAMLWitnessException {
    ViolationSequenceEntry violationEntry = getViolationSequence(pEntries);
    ImmutableList<PartitionedWaypoints> segmentizedEntries = segmentize(violationEntry);
    checkCycleOrTargetAtEnd(violationEntry);
    return segmentizedEntries;
  }

  protected void handleMultiTargetWaypoints(
      List<WaypointRecord> pWaypointRecords,
      String nextStateId,
      String currentStateId,
      Integer pDistanceToViolation,
      ImmutableList.Builder<AutomatonInternalState> automatonStates)
      throws WitnessParseException, InterruptedException {
    // Formally there is no ordering between the waypoints at the same segment.
    // However, since CPAchecker is an interleaving based tool, all the interleavings
    // are explicitly set in the ARG. Therefore, we can just check the multi-follow
    // target waypoints, by first checking one, and then the other, so we need
    // to branch in the automaton for this.

    WaypointRecord firstWaypoint = pWaypointRecords.getFirst();
    WaypointRecord secondWaypoint = pWaypointRecords.get(1);

    ImmutableList.Builder<AutomatonTransition> currentStateTransitions =
        new ImmutableList.Builder<>();

    // First do waypoint1 then waypoint2
    followFirstWaypointWithSecondAsTarget(
        nextStateId,
        pDistanceToViolation,
        automatonStates,
        "SI1-" + nextStateId,
        firstWaypoint,
        secondWaypoint,
        currentStateTransitions);

    // then do waypoint2 then waypoint1
    followFirstWaypointWithSecondAsTarget(
        nextStateId,
        pDistanceToViolation,
        automatonStates,
        "SI2-" + nextStateId,
        secondWaypoint,
        firstWaypoint,
        currentStateTransitions);

    automatonStates.add(
        new AutomatonInternalState(
            currentStateId,
            currentStateTransitions.build(),
            /* pIsTarget= */ false,
            /* pAllTransitions= */ false,
            /* pIsCycleStart= */ false));
  }

  private void followFirstWaypointWithSecondAsTarget(
      String pNextStateId,
      Integer pDistanceToViolation,
      ImmutableList.Builder<AutomatonInternalState> automatonStates,
      String pIntermediateState,
      WaypointRecord firstWaypoint,
      WaypointRecord secondWaypoint,
      ImmutableList.Builder<AutomatonTransition> currentStateTransitions)
      throws InterruptedException, WitnessParseException {

    handleAssumption(
        pIntermediateState,
        cfa.getAstCfaRelation()
            .getTightestStatementForStarting(
                firstWaypoint.getLocation().getLine(), firstWaypoint.getLocation().getColumn())
            .orElseThrow(),
        firstWaypoint.getLocation().getLine(),
        firstWaypoint.getThread(),
        firstWaypoint.getLocation().getFunction(),
        pDistanceToViolation,
        "1",
        currentStateTransitions);

    handleTarget(
        pNextStateId,
        secondWaypoint.getLocation().getLine(),
        secondWaypoint.getLocation().getColumn(),
        secondWaypoint.getThread(),
        pDistanceToViolation - 1,
        pIntermediateState,
        new ImmutableList.Builder<>(),
        automatonStates);
  }

  @Override
  protected Integer handleFunctionEnter(
      String nextStateId,
      Integer followLine,
      OptionalInt followColumn,
      OptionalInt threadId,
      Integer pPthreadFunctionEnterWaypoint,
      Integer pDistanceToViolation,
      Multimap<Integer, CFAEdge> startLineToCFAEdge,
      ImmutableList.Builder<AutomatonTransition> transitions)
      throws WitnessParseException {
    // Find out the edge which corresponds to this statement, it can either be a CFunctionCallEgde
    // or a CStatementEdge
    // We sort the edges by their column, so we can take the first one which matches the given
    // column
    for (CFAEdge edge :
        FluentIterable.from(startLineToCFAEdge.get(followLine))
            .toSortedSet(
                Comparator.comparing(
                    pCFAEdge -> pCFAEdge.getFileLocation().getStartColumnInLine()))) {
      // Not a function call so we skip it
      if (!(edge instanceof AStatementEdge || edge instanceof FunctionCallEdge)) {
        continue;
      }

      // If the column does not match we continue by not matching this edge
      if (followColumn.isPresent()
          && followColumn.orElseThrow() != edge.getFileLocation().getStartColumnInLine()) {
        continue;
      }

      // If we are matching a `pthread_create` function call we need to handle this specially
      // to be able to correctly validate violation witnesses for concurrent programs,
      // since they need to have an action to set the thread id of the creating thread
      AutomatonBoolExpr expr =
          new CheckPassesThroughNodes(
              ImmutableSet.of(edge.getPredecessor()),
              ImmutableSet.of(edge.getSuccessor()),
              threadId);

      AutomatonTransition.Builder transitionBuilder =
          new AutomatonTransition.Builder(expr, nextStateId);
      transitionBuilder.withActions(
          ImmutableList.of(
              getThreadIdAssignment(pPthreadFunctionEnterWaypoint),
              distanceToViolationAction(pDistanceToViolation)));

      transitions.add(transitionBuilder.build());
      return pPthreadFunctionEnterWaypoint + 1;
    }

    throw new WitnessParseException(
        "No CFAEdge could be matched for the function enter waypoint passing line "
            + followLine
            + " and column "
            + followColumn);
  }

  @Override
  Automaton createViolationAutomatonFromEntries(List<AbstractEntry> pEntries)
      throws InterruptedException, InvalidYAMLWitnessException, WitnessParseException {
    List<PartitionedWaypoints> segments = segmentizeAndCheckV2d2(pEntries);
    // this needs to be called exactly WitnessAutomaton for the option
    // WitnessAutomaton.cpa.automaton.treatErrorsAsTargets to work
    final String automatonName = AutomatonGraphmlParser.WITNESS_AUTOMATON_NAME;

    int stateCounter = 0;
    final String initState = getStateName(stateCounter++);

    final ImmutableList.Builder<AutomatonInternalState> automatonStates =
        new ImmutableList.Builder<>();
    String currentStateId = initState;

    int distance = segments.size() - 1;
    Optional<String> cycleHeadName = Optional.empty();
    Integer pthreadFunctionEnterWaypoint = 1;

    ImmutableMap.Builder<String, AutomatonVariable> automatonVariablesBuilder =
        new ImmutableMap.Builder<>();
    automatonVariablesBuilder.put(
        AutomatonGraphmlParser.DISTANCE_TO_VIOLATION,
        AutomatonVariable.createAutomatonVariable(
            /* pType= */ "int",
            AutomatonGraphmlParser.DISTANCE_TO_VIOLATION,
            Integer.toString(segments.size())));

    for (PartitionedWaypoints entry : segments) {
      ImmutableList.Builder<AutomatonTransition> transitions = new ImmutableList.Builder<>();
      // We call flow waypoint either cycle or follow waypoint as they ensure flow in the execution
      List<WaypointRecord> followWaypoints =
          entry.follow().isPresent()
              ? entry.follow().orElseThrow()
              : ImmutableList.of(entry.cycle().orElseThrow());
      String nextStateId = getStateName(stateCounter++);

      if (followWaypoints.size() > 1) {
        if (!FluentIterable.from(followWaypoints)
            .transform(WaypointRecord::getType)
            .allMatch(type -> type.equals(WaypointType.TARGET))) {
          throw new WitnessParseException(
              "Currently we cannot handle multiple non-target follow waypoints in the same"
                  + " segment.");
        }

        handleMultiTargetWaypoints(followWaypoints, "X", currentStateId, distance, automatonStates);

        currentStateId = "X";
        break;
      } else {
        WaypointRecord followWaypoint = Iterables.getOnlyElement(followWaypoints);
        if (followWaypoint.getThread().isPresent()) {
          automatonVariablesBuilder.put(
              THREAD_ID_VAR_NAME,
              AutomatonVariable.createAutomatonVariable(
                  "int",
                  THREAD_ID_VAR_NAME,
                  // The initial thread always gets the identifier `0`
                  Integer.toString(0)));
        }

        pthreadFunctionEnterWaypoint =
            handleWaypointsV2d0(
                entry,
                followWaypoint,
                transitions,
                automatonStates,
                distance,
                pthreadFunctionEnterWaypoint,
                nextStateId,
                currentStateId);

        if (followWaypoint.getType().equals(WaypointType.TARGET)) {
          if (stateCounter != segments.size() + 1) {
            logger.log(
                Level.INFO,
                "Target waypoint is not the last waypoint, following waypoints will be ignored!");
          }
          currentStateId = "X";
          break;
        }

        if (followWaypoint.getAction().equals(WaypointAction.CYCLE) && cycleHeadName.isEmpty()) {
          cycleHeadName = Optional.of(currentStateId);
        }
      }

      automatonStates.add(
          new AutomatonInternalState(
              currentStateId,
              transitions.build(),
              /* pIsTarget= */ false,
              /* pAllTransitions= */ false,
              /* pIsCycleStart= */ currentStateId.equals(
                  cycleHeadName.isPresent() ? cycleHeadName.orElseThrow() : "")));

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
                  new AutomatonTransition.Builder(
                          AutomatonBoolExpr.TRUE, cycleHeadName.orElseThrow())
                      .build()),
              /* pIsTarget= */ false,
              /* pAllTransitions= */ false,
              /* pIsCycleStart= */ false));
    }

    Automaton automaton;
    try {
      automaton =
          new Automaton(
              automatonName,
              automatonVariablesBuilder.buildKeepingLast(),
              automatonStates.build(),
              initState);
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
