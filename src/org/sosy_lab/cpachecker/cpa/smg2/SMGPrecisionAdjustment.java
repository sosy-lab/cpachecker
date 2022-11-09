// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintStream;
import java.util.Map.Entry;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
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
import org.sosy_lab.cpachecker.cpa.smg2.abstraction.SMGCPAAbstractionManager;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
import org.sosy_lab.cpachecker.cpa.smg2.util.ValueAndValueSize;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class SMGPrecisionAdjustment implements PrecisionAdjustment {

  @Options(prefix = "cpa.smg2.abstraction")
  public static class PrecAdjustmentOptions {

    @Option(secure = true, description = "restrict abstraction computations to branching points")
    private boolean alwaysAtBranch = false;

    @Option(secure = true, description = "restrict abstraction computations to join points")
    private boolean alwaysAtJoin = false;

    @Option(
        secure = true,
        description = "restrict abstraction computations to function calls/returns")
    private boolean alwaysAtFunction = false;

    @Option(
        secure = true,
        description =
            "If enabled, abstraction computations at loop-heads are enabled. List abstraction has"
                + " to be enabled for this.")
    private boolean alwaysAtLoop = false;

    @Option(secure = true, description = "toggle liveness abstraction")
    private boolean doLivenessAbstraction = false;

    @Option(
        secure = true,
        description =
            "restrict liveness abstractions to nodes with more than one entering and/or leaving"
                + " edge")
    private boolean onlyAtNonLinearCFA = false;

    @Option(
        secure = true,
        description =
            "skip abstraction computations until the given number of iterations are reached,"
                + " after that decision is based on then current level of determinism,"
                + " setting the option to -1 always performs abstraction computations")
    @IntegerOption(min = -1)
    private int iterationThreshold = -1;

    @Option(
        secure = true,
        description =
            "threshold for level of determinism, in percent, up-to which abstraction computations "
                + "are performed (and iteration threshold was reached)")
    @IntegerOption(min = 0, max = 100)
    @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "false alarm")
    private int determinismThreshold = 85;

    @Option(
        secure = true,
        name = "listAbstractionMinimumLengthThreshhold",
        description =
            "The minimum list segments directly following each other with the same value needed to"
                + " abstract them.Minimum is 2.")
    private int listAbstractionMinimumLengthThreshhold = 12;

    @Option(
        secure = true,
        name = "abstractHeapValues",
        description = "If heap values are to be abstracted based on CEGAR.")
    private boolean abstractHeapValues = false;

    @Option(
        secure = true,
        name = "abstractProgramVariables",
        description = "Abstraction of program variables via CEGAR.")
    private boolean abstractProgramVariables = false;

    @Option(
        secure = true,
        name = "abstractLinkedLists",
        description = "Abstraction of all detected linked lists at loop heads.")
    private boolean abstractLinkedLists = true;

    private final @Nullable ImmutableSet<CFANode> loopHeads;

    public PrecAdjustmentOptions(Configuration config, CFA pCfa)
        throws InvalidConfigurationException {
      config.inject(this);

      if (alwaysAtLoop && pCfa.getAllLoopHeads().isPresent()) {
        loopHeads = pCfa.getAllLoopHeads().orElseThrow();
      } else {
        loopHeads = null;
      }
    }

    public int getListAbstractionMinimumLengthThreshhold() {
      return listAbstractionMinimumLengthThreshhold;
    }

    /**
     * This method determines whether to abstract at each location.
     *
     * @return true, if an abstraction should be computed at each location, else false
     */
    private boolean abstractAtEachLocation() {
      return !alwaysAtBranch && !alwaysAtJoin && !alwaysAtFunction && !alwaysAtLoop;
    }

    private boolean abstractAtBranch(LocationState location) {
      return alwaysAtBranch && location.getLocationNode().getNumLeavingEdges() > 1;
    }

    private boolean abstractAtJoin(LocationState location) {
      return alwaysAtJoin && location.getLocationNode().getNumEnteringEdges() > 1;
    }

    private boolean abstractAtFunction(LocationState location) {
      return alwaysAtFunction
          && (location.getLocationNode() instanceof FunctionEntryNode
              || location.getLocationNode().getEnteringSummaryEdge() != null);
    }

    private boolean abstractAtLoop(LocationState location) {
      checkState(!alwaysAtLoop || loopHeads != null);
      return alwaysAtLoop && loopHeads.contains(location.getLocationNode());
    }
  }

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
  private final PrecAdjustmentOptions options;
  private final Optional<LiveVariables> liveVariables;
  private final Optional<ImmutableSet<CFANode>> maybeLoops;

  // for statistics
  private final StatCounter abstractions;
  private final StatTimer totalLiveness;
  private final StatTimer totalAbstraction;
  private final StatTimer totalEnforcePath;

  @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "false alarm")
  private boolean performPrecisionBasedAbstraction = false;

  public SMGPrecisionAdjustment(
      final SMGCPAStatistics pStats,
      final CFA pCfa,
      final PrecAdjustmentOptions pOptions,
      final PrecAdjustmentStatistics pStatistics) {

    options = pOptions;
    stats = pStats;
    liveVariables = pCfa.getLiveVariables();
    maybeLoops = pCfa.getAllLoopHeads();

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

    if (options.doLivenessAbstraction && liveVariables.isPresent()) {
      totalLiveness.start();
      if (options.abstractProgramVariables) {
        resultState = enforceLiveness(pState, location, resultState);
      }
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
      if (options.abstractProgramVariables) {
        resultState = enforcePathThreshold(resultState, assignments);
      }
      totalEnforcePath.stop();
    }

    if (options.abstractLinkedLists && checkAbstractListAt(location)) {
      // Abstract Lists at loop heads
      try {
        resultState =
            new SMGCPAAbstractionManager(
                    resultState, options.getListAbstractionMinimumLengthThreshhold())
                .findAndAbstractLists();
      } catch (SMG2Exception e) {
        // Do nothing. This should never happen anyway
      }
    }

    return Optional.of(PrecisionAdjustmentResult.create(resultState, pPrecision, Action.CONTINUE));
  }

  private boolean isLoopHead(LocationState location) {
    if (maybeLoops.isPresent() && maybeLoops.orElseThrow().contains(location.getLocationNode())) {
      return true;
    }
    return false;
  }

  private boolean checkAbstractListAt(LocationState location) {
    if (options.abstractAtFunction(location) || isLoopHead(location)) {
      return true;
    }
    return false;
  }

  /**
   * This method decides whether to perform abstraction computations. These are computed if the
   * iteration threshold is deactivated, or if the level of determinism ever gets below the
   * threshold for the level of determinism.
   *
   * @return true, if abstractions should be computed, else false
   */
  private boolean performPrecisionBasedAbstraction() {
    // always compute abstraction if option is disabled
    if (options.iterationThreshold == -1) {
      return true;
    }

    // else, delay abstraction computation as long as iteration threshold is not reached
    if (stats.getCurrentNumberOfIterations() < options.iterationThreshold) {
      return false;
    }

    // else, always compute abstraction if computed abstraction before
    if (performPrecisionBasedAbstraction) {
      return true;
    }

    // else, determine current setting and return that
    performPrecisionBasedAbstraction =
        stats.getCurrentLevelOfDeterminism() < options.determinismThreshold;

    return performPrecisionBasedAbstraction;
  }

  private SMGState enforceLiveness(
      final SMGState pState, LocationState location, SMGState initialState) {
    CFANode actNode = location.getLocationNode();
    SMGState currentState = initialState;
    boolean hasMoreThanOneEnteringLeavingEdge =
        actNode.getNumEnteringEdges() > 1 || actNode.getNumLeavingEdges() > 1;

    if (!options.onlyAtNonLinearCFA || hasMoreThanOneEnteringLeavingEdge) {
      boolean onlyBlankEdgesEntering = true;
      for (int i = 0; i < actNode.getNumEnteringEdges() && onlyBlankEdgesEntering; i++) {
        onlyBlankEdgesEntering = location.getLocationNode().getEnteringEdge(i) instanceof BlankEdge;
      }

      // when there are only blank edges that lead to this state, then we can
      // skip the abstraction, after a blank edge there cannot be a variable
      // less live
      if (!onlyBlankEdgesEntering) {
        for (MemoryLocation variable : pState.getTrackedMemoryLocations()) {
          if (!liveVariables
              .orElseThrow()
              .isVariableLive(variable.getExtendedQualifiedName(), location.getLocationNode())) {
            currentState = currentState.copyAndForget(variable).getState();
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
    if (options.abstractAtEachLocation()
        || options.abstractAtBranch(location)
        || options.abstractAtJoin(location)
        || options.abstractAtFunction(location)
        || options.abstractAtLoop(location)) {

      if (options.abstractProgramVariables) {
        for (MemoryLocation memoryLocation :
            currentState.getMemoryModel().getMemoryLocationsAndValuesForSPCWithoutHeap().keySet()) {
          CType type = currentState.getMemoryModel().getTypeOfVariable(memoryLocation);
          if (location != null
              && !precision.isTracking(memoryLocation, type, location.getLocationNode())) {
            currentState = currentState.copyAndForget(memoryLocation).getState();
          }
        }
      }
      if (precision instanceof SMGPrecision && options.abstractHeapValues) {
        SMGPrecision smgPrecision = (SMGPrecision) precision;
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
}
