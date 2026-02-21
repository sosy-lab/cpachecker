// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import com.google.common.base.Equivalence.Wrapper;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.UniqueAssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions.SMGAbstractionOptions;
import org.sosy_lab.cpachecker.cpa.smg2.abstraction.SMGCPAAbstractionManager;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.ValueAndValueSize;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.smg.datastructures.PersistentSet;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class SMGPrecisionAdjustment implements PrecisionAdjustment {
  public static class PrecAdjustmentStatistics implements Statistics {

    final StatCounter abstractions = new StatCounter("Number of abstraction computations");
    private StatTimer totalLivenessTimer;
    private StatTimer totalAbstractionTimer;
    private StatTimer totalEnforcePathTimer;

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      renewTimers();
      StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);
      writer.put(abstractions);
      writer.put(totalLivenessTimer);
      writer.put(totalAbstractionTimer);
      writer.put(totalEnforcePathTimer);
    }

    @Override
    public String getName() {
      return "SMGPrecisionAdjustment";
    }

    public void renewTimers() {
      totalLivenessTimer = new StatTimer("Total time for liveness abstraction");
      totalAbstractionTimer = new StatTimer("Total time for abstraction computation");
      totalEnforcePathTimer = new StatTimer("Total time for path thresholds");
    }
  }

  private final SMGCPAStatistics stats;
  private final SMGOptions options;
  private final SMGAbstractionOptions abstractionOptions;
  private final Optional<LiveVariables> liveVariables;
  private final Optional<ImmutableSet<CFANode>> maybeLoops;
  private Optional<Set<CFANode>> maybeLoopLeavers;

  // for statistics
  private final StatCounter abstractions;
  private final StatTimer totalLiveness;
  private final StatTimer totalAbstraction;
  private final StatTimer totalEnforcePath;

  private boolean performPrecisionBasedAbstraction = false;

  public SMGPrecisionAdjustment(
      final SMGCPAStatistics pStats,
      final CFA pCfa,
      final SMGOptions pOptions,
      final PrecAdjustmentStatistics pStatistics) {

    options = pOptions;
    abstractionOptions = options.getAbstractionOptions();
    stats = pStats;
    liveVariables = pCfa.getLiveVariables();
    maybeLoops = pCfa.getAllLoopHeads();
    maybeLoopLeavers = Optional.empty();

    abstractions = pStatistics.abstractions;
    pStatistics.renewTimers();
    totalLiveness = pStatistics.totalLivenessTimer;
    totalAbstraction = pStatistics.totalAbstractionTimer;
    totalEnforcePath = pStatistics.totalEnforcePathTimer;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState)
      throws CPAException, InterruptedException {

    return prec(
        (SMGState) pState,
        (VariableTrackingPrecision) pPrecision,
        AbstractStates.extractStateByType(fullState, LocationState.class),
        AbstractStates.extractStateByType(fullState, UniqueAssignmentsInPathConditionState.class));
  }

  private Optional<PrecisionAdjustmentResult> prec(
      final SMGState pState,
      VariableTrackingPrecision pPrecision,
      LocationState location,
      UniqueAssignmentsInPathConditionState assignments) {
    SMGState resultState = pState;

    if ((abstractionOptions.doLivenessAbstraction()
            || abstractionOptions.abstractProgramVariables())
        && liveVariables.isPresent()) {
      totalLiveness.start();
      resultState = enforceLiveness(pState, location, resultState);
      totalLiveness.stop();
    }

    // compute the abstraction based on the value-analysis precision
    totalAbstraction.start();
    if (performPrecisionBasedAbstraction()) {
      resultState = enforcePrecision(resultState, location, pPrecision);
    }
    totalAbstraction.stop();

    // compute the abstraction for assignment thresholds
    if (assignments != null) {
      totalEnforcePath.start();
      if (abstractionOptions.abstractProgramVariables()) {
        resultState = enforcePathThreshold(resultState, assignments);
      }
      totalEnforcePath.stop();
    }

    if (abstractionOptions.getAbstractConcreteValuesAboveThreshold() >= 0) {
      resultState =
          enforceConcreteValueThreshold(
              resultState, abstractionOptions.getAbstractConcreteValuesAboveThreshold());
    }

    if (abstractionOptions.abstractLinkedLists() && checkAbstractListAt(location)) {
      // Abstract Lists at loop heads
      try {
        resultState =
            new SMGCPAAbstractionManager(
                    resultState,
                    abstractionOptions.getListAbstractionMinimumLengthThreshold(),
                    stats)
                .findAndAbstractLists();
      } catch (SMGException e) {
        // Do nothing. This should never happen anyway
        throw new RuntimeException(e);
      }
    }

    if (abstractionOptions.getCleanUpUnusedConstraints()) {
      resultState = resultState.removeOldConstraints();
    }

    if (checkAbstractListAt(location)) {
      resultState = resultState.withBlockEnd(location.getLocationNode());
    }

    return Optional.of(new PrecisionAdjustmentResult(resultState, pPrecision, Action.CONTINUE));
  }

  private boolean isLoopHead(LocationState location) {
    return maybeLoops.isPresent() && maybeLoops.orElseThrow().contains(location.getLocationNode());
  }

  @SuppressWarnings("unused")
  private boolean isLoopLeaving(LocationState location) {
    if (maybeLoopLeavers.isEmpty()) {
      ImmutableSet.Builder<CFANode> builder = ImmutableSet.builder();
      for (Set<CFANode> bla : getAllLoopHeadNodesToLeavingNodes().orElseThrow().values()) {
        builder.addAll(bla);
      }
      // TODO: clean this up once it is sufficiently debugged
      maybeLoopLeavers = Optional.ofNullable(builder.build());
    }
    // Just left a loop.
    // We detect this by checking if the current location is a node that is following a loop head
    // Abstracting here allows us to subsume most lists into the first abstracted that left the loop
    return maybeLoopLeavers.isPresent()
        && maybeLoopLeavers.orElseThrow().contains(location.getLocationNode());
  }

  private Optional<Map<CFANode, Set<CFANode>>> getAllLoopHeadNodesToLeavingNodes() {
    if (maybeLoops.isEmpty()) {
      return Optional.empty();
    }
    Map<CFANode, Set<CFANode>> loopHeadToLoopLeavers = new HashMap<>();
    for (CFANode loopHeadNode : maybeLoops.orElseThrow()) {
      Set<CFANode> cache = new HashSet<>();
      for (int i = 0; i < loopHeadNode.getNumLeavingEdges(); i++) {
        CFAEdge leavingEdge = loopHeadNode.getLeavingEdge(i);
        int loopBeginLine = leavingEdge.getFileLocation().getStartingLineInOrigin();
        int loopEndLine = leavingEdge.getFileLocation().getEndingLineInOrigin();
        // The first CFAEdge that exceeds loopEndLine is a loop exiting location
        Optional<CFANode> maybeLoopLeavingNode =
            getLeavingNode(leavingEdge.getSuccessor(), loopBeginLine, loopEndLine, cache);
        if (maybeLoopLeavingNode.isPresent()) {
          CFANode loopLeavingNode = maybeLoopLeavingNode.orElseThrow();
          if (loopHeadToLoopLeavers.containsKey(loopHeadNode)) {
            loopHeadToLoopLeavers.get(loopHeadNode).add(loopLeavingNode);
          } else {
            Set<CFANode> newSet = new HashSet<>();
            newSet.add(loopLeavingNode);
            loopHeadToLoopLeavers.put(loopHeadNode, newSet);
          }
        }
      }
      Preconditions.checkArgument(loopHeadToLoopLeavers.containsKey(loopHeadNode));
    }
    return Optional.of(loopHeadToLoopLeavers);
  }

  private Optional<CFANode> getLeavingNode(
      CFANode node, int loopBeginLine, int loopEndLine, Set<CFANode> visited) {
    if (visited.contains(node)) {
      return Optional.empty();
    }
    visited.add(node);
    for (int i = 0; i < node.getNumLeavingEdges(); i++) {
      CFAEdge leavingEdge = node.getLeavingEdge(i);
      if (leavingEdge.getFileLocation().getStartingLineInOrigin() > loopEndLine) {
        // Loop leaving edge
        return Optional.of(node);
      }
      if (leavingEdge.getFileLocation().getStartingLineInOrigin() < loopBeginLine) {
        // Loop leaving edge (e.g. a goTo)
        return Optional.of(node);
      }
      Optional<CFANode> recNode =
          getLeavingNode(leavingEdge.getSuccessor(), loopBeginLine, loopEndLine, visited);
      if (recNode.isPresent()) {
        return recNode;
      }
    }
    return Optional.empty();
  }

  private boolean checkAbstractListAt(LocationState location) {
    return abstractionOptions.abstractAtFunction(location) || isLoopHead(location);
  }

  /**
   * This method decides whether to perform abstraction computations. These are computed if the
   * iteration threshold is deactivated, or if the level of determinism ever gets below the
   * threshold for the level of determinism.
   *
   * @return whether abstractions should be computed
   */
  private boolean performPrecisionBasedAbstraction() {
    // always compute abstraction if option is disabled
    if (abstractionOptions.getIterationThreshold() == -1) {
      return true;
    }

    // else, delay abstraction computation as long as iteration threshold is not reached
    if (stats.getCurrentNumberOfIterations() < abstractionOptions.getIterationThreshold()) {
      return false;
    }

    // else, always compute abstraction if computed abstraction before
    if (performPrecisionBasedAbstraction) {
      return true;
    }

    // else, determine current setting and return that
    performPrecisionBasedAbstraction =
        stats.getCurrentLevelOfDeterminism() < abstractionOptions.getDeterminismThreshold();

    return performPrecisionBasedAbstraction;
  }

  private SMGState enforceLiveness(
      final SMGState pState, LocationState location, SMGState initialState) {
    CFANode actNode = location.getLocationNode();
    SMGState currentState = initialState;
    boolean hasMoreThanOneEnteringLeavingEdge =
        actNode.getNumEnteringEdges() > 1 || actNode.getNumLeavingEdges() > 1;

    if (!abstractionOptions.onlyAtNonLinearCFA() || hasMoreThanOneEnteringLeavingEdge) {
      boolean onlyBlankEdgesEntering = true;
      for (int i = 0; i < actNode.getNumEnteringEdges() && onlyBlankEdgesEntering; i++) {
        onlyBlankEdgesEntering = location.getLocationNode().getEnteringEdge(i) instanceof BlankEdge;
      }

      // when there are only blank edges that lead to this state, then we can
      // skip the abstraction, after a blank edge there cannot be a variable
      // less live
      if (!onlyBlankEdgesEntering) {
        for (MemoryLocation variable : pState.getTrackedMemoryLocations()) {
          String qualifiedVarName = variable.getQualifiedName();
          if (!liveVariables
              .orElseThrow()
              .isVariableLive(qualifiedVarName, location.getLocationNode())) {
            if (!abstractionOptions.isEnforcePointerSensitiveLiveness()) {
              // TODO: LiveVariablesCPA fails to track stack based memory correctly and invalidates
              //  e.g. arrays to early. Hence isEnforcePointerInsensitiveLiveness = true is unsound!
              currentState = currentState.invalidateVariable(variable, true);

            } else {
              // Don't invalidate memory that may have valid outside pointers to it that may keep it
              // alive!
              Optional<SMGObject> maybeVarObj =
                  currentState.getMemoryModel().getObjectForVariable(qualifiedVarName);
              if (maybeVarObj.isPresent()) {
                // Does not contain itself
                Set<SMGObject> allObjsPointingTowards =
                    currentState
                        .getMemoryModel()
                        .getSmg()
                        .getAllSourcesForPointersPointingTowards(maybeVarObj.orElseThrow())
                        .stream()
                        .filter(o -> !o.equals(maybeVarObj.orElseThrow()))
                        .collect(ImmutableSet.toImmutableSet());
                if (allObjsPointingTowards.isEmpty()) {
                  currentState = currentState.invalidateVariable(variable, true);
                }
              }
            }
          }
        }
      }
    }
    return currentState;
  }

  /**
   * This method performs an abstraction computation on the current state.
   *
   * @param location the current location
   * @param state the current state
   * @param precision the current precision
   */
  private SMGState enforcePrecision(
      final SMGState state, LocationState location, VariableTrackingPrecision precision) {

    SMGState currentState = state;
    if (abstractionOptions.abstractAtEachLocation()
        || abstractionOptions.abstractAtBranch(location)
        || abstractionOptions.abstractAtJoin(location)
        || abstractionOptions.abstractAtFunction(location)
        || abstractionOptions.abstractAtLoop(location)) {

      if (abstractionOptions.abstractProgramVariables()) {
        for (MemoryLocation memoryLocation :
            currentState.getMemoryModel().getMemoryLocationsAndValuesForSPCWithoutHeap().keySet()) {
          CType type = currentState.getMemoryModel().getTypeOfVariable(memoryLocation);
          if (location != null
              && !precision.isTracking(memoryLocation, type, location.getLocationNode())) {
            currentState = currentState.copyAndForget(memoryLocation).getState();
          }
        }
      }
      if (precision instanceof SMGPrecision smgPrecision
          && abstractionOptions.abstractHeapValues()) {
        currentState = currentState.enforceHeapValuePrecision(smgPrecision.getTrackedHeapValues());
      }

      abstractions.inc();
    }
    return currentState;
  }

  /**
   * This method abstracts variables that exceed the threshold of assignments along the current
   * path.
   *
   * @param state the state to abstract
   * @param assignments the assignment information
   */
  private SMGState enforcePathThreshold(
      final SMGState state, UniqueAssignmentsInPathConditionState assignments) {

    SMGState currentState = state;
    // forget the value for all variables that exceed their threshold
    for (Entry<MemoryLocation, ValueAndValueSize> e :
        currentState.getMemoryModel().getMemoryLocationsAndValuesForSPCWithoutHeap().entrySet()) {
      MemoryLocation memoryLocation = e.getKey();
      assignments.updateAssignmentInformation(memoryLocation, e.getValue().getValue());

      if (assignments.exceedsThreshold(memoryLocation)) {
        currentState = currentState.copyAndForget(memoryLocation).getState();
      }
    }
    return currentState;
  }

  @SuppressWarnings("unused")
  private SMGState enforceConcreteValueThreshold(
      final SMGState state, int numOfConcreteValuesAllowed) {
    // TODO: add tracking of concrete value order
    // TODO: try to remove 0 only for a non pointer type
    SMGState currentState = state;
    // Gather all concrete values first and filter out all above the threshold
    ImmutableBiMap<SMGValue, Wrapper<Value>> mapping =
        currentState.getMemoryModel().getValueToSMGValueMapping().inverse();
    // remove all concrete values above the threshold (will be replaced by symbolics by reading)
    for (Entry<SMGObject, PersistentSet<SMGHasValueEdge>> objAndHVEs :
        currentState.getMemoryModel().getSmg().getSMGObjectsWithSMGHasValueEdges().entrySet()) {
      SMGObject object = objAndHVEs.getKey();
      Set<SMGHasValueEdge> edgesToRemove = new HashSet<>();
      for (SMGHasValueEdge hve : objAndHVEs.getValue()) {
        SMGValue smgValue = hve.hasValue();
        Wrapper<Value> wValue = mapping.get(smgValue);
        if (wValue == null || wValue.get() instanceof NumericValue) {
          edgesToRemove.add(hve);
        }
      }
      currentState =
          currentState.copyAndReplaceMemoryModel(
              currentState
                  .getMemoryModel()
                  .copyWithNewSMG(
                      currentState
                          .getMemoryModel()
                          .getSmg()
                          .copyAndRemoveHVEdges(edgesToRemove, object)));
    }
    return currentState;
  }
}
