// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintStream;
import java.util.Map.Entry;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
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
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

public class ValueAnalysisPrecisionAdjustment implements PrecisionAdjustment {

  @Options(prefix = "cpa.value.abstraction")
  public static class PrecAdjustmentOptions {

    @Option(secure = true, description = "restrict abstraction computations to branching points")
    private boolean alwaysAtBranch = false;

    @Option(secure = true, description = "restrict abstraction computations to join points")
    private boolean alwaysAtJoin = false;

    @Option(
        secure = true,
        description = "restrict abstraction computations to function calls/returns")
    private boolean alwaysAtFunction = false;

    @Option(secure = true, description = "restrict abstraction computations to loop heads")
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

    private final ImmutableSet<CFANode> loopHeads;

    public PrecAdjustmentOptions(Configuration config, CFA pCfa)
        throws InvalidConfigurationException {
      config.inject(this);

      if (alwaysAtLoop && pCfa.getAllLoopHeads().isPresent()) {
        loopHeads = pCfa.getAllLoopHeads().orElseThrow();
      } else {
        loopHeads = null;
      }
    }

    /**
     * This method determines whether or not to abstract at each location.
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

  @SuppressWarnings("deprecation") // remove ThreadSafeTimerContainer
  public static class PrecAdjustmentStatistics implements Statistics {

    final StatCounter abstractions = new StatCounter("Number of abstraction computations");
    private final ThreadSafeTimerContainer totalLivenessTimer =
        new ThreadSafeTimerContainer("Total time for liveness abstraction");
    private final ThreadSafeTimerContainer totalAbstractionTimer =
        new ThreadSafeTimerContainer("Total time for abstraction computation");
    private final ThreadSafeTimerContainer totalEnforcePathTimer =
        new ThreadSafeTimerContainer("Total time for path thresholds");

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);
      writer.put(abstractions);
      writer.put(totalLivenessTimer);
      writer.put(totalAbstractionTimer);
      writer.put(totalEnforcePathTimer);
    }

    @Override
    public String getName() {
      return "ValueAnalysisPrecisionAdjustment";
    }
  }

  private final ValueAnalysisCPAStatistics stats;
  private final PrecAdjustmentOptions options;
  private final Optional<LiveVariables> liveVariables;

  // for statistics
  private final StatCounter abstractions;
  private final TimerWrapper totalLiveness;
  private final TimerWrapper totalAbstraction;
  private final TimerWrapper totalEnforcePath;

  @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "false alarm")
  private boolean performPrecisionBasedAbstraction = false;

  public ValueAnalysisPrecisionAdjustment(
      final ValueAnalysisCPAStatistics pStats,
      final CFA pCfa,
      final PrecAdjustmentOptions pOptions,
      final PrecAdjustmentStatistics pStatistics) {

    options = pOptions;
    stats = pStats;
    liveVariables = pCfa.getLiveVariables();

    abstractions = pStatistics.abstractions;
    totalLiveness = pStatistics.totalLivenessTimer.getNewTimer();
    totalAbstraction = pStatistics.totalAbstractionTimer.getNewTimer();
    totalEnforcePath = pStatistics.totalEnforcePathTimer.getNewTimer();
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
        (ValueAnalysisState) pState,
        (VariableTrackingPrecision) pPrecision,
        AbstractStates.extractStateByType(fullState, LocationState.class),
        AbstractStates.extractStateByType(fullState, UniqueAssignmentsInPathConditionState.class));
  }

  private Optional<PrecisionAdjustmentResult> prec(
      ValueAnalysisState pState,
      VariableTrackingPrecision pPrecision,
      LocationState location,
      UniqueAssignmentsInPathConditionState assignments) {
    ValueAnalysisState resultState = ValueAnalysisState.copyOf(pState);

    if (options.doLivenessAbstraction && liveVariables.isPresent()) {
      totalLiveness.start();
      enforceLiveness(pState, location, resultState);
      totalLiveness.stop();
    }

    // compute the abstraction based on the value-analysis precision
    totalAbstraction.start();
    if (performPrecisionBasedAbstraction()) {
      enforcePrecision(resultState, location, pPrecision);
    }
    totalAbstraction.stop();

    // compute the abstraction for assignment thresholds
    if (assignments != null) {
      totalEnforcePath.start();
      enforcePathThreshold(resultState, assignments);
      totalEnforcePath.stop();
    }

    resultState = resultState.equals(pState) ? pState : resultState;

    return Optional.of(PrecisionAdjustmentResult.create(resultState, pPrecision, Action.CONTINUE));
  }

  /**
   * This method decides whether or not to perform abstraction computations. These are computed if
   * the iteration threshold is deactivated, or if the level of determinism ever gets below the
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

  private void enforceLiveness(
      ValueAnalysisState pState, LocationState location, ValueAnalysisState resultState) {
    CFANode actNode = location.getLocationNode();

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
            resultState.forget(variable);
          }
        }
      }
    }
  }

  /**
   * This method performs an abstraction computation on the current value-analysis state.
   *
   * @param location the current location
   * @param state the current state
   * @param precision the current precision
   */
  private void enforcePrecision(
      ValueAnalysisState state, LocationState location, VariableTrackingPrecision precision) {
    if (options.abstractAtEachLocation()
        || options.abstractAtBranch(location)
        || options.abstractAtJoin(location)
        || options.abstractAtFunction(location)
        || options.abstractAtLoop(location)) {

      for (Entry<MemoryLocation, ValueAndType> e : state.getConstants()) {
        MemoryLocation memoryLocation = e.getKey();
        if (location != null
            && !precision.isTracking(
                memoryLocation, e.getValue().getType(), location.getLocationNode())) {
          state.forget(memoryLocation);
        }
      }

      abstractions.inc();
    }
  }

  /**
   * This method abstracts variables that exceed the threshold of assignments along the current
   * path.
   *
   * @param state the state to abstract
   * @param assignments the assignment information
   */
  private void enforcePathThreshold(
      ValueAnalysisState state, UniqueAssignmentsInPathConditionState assignments) {

    // forget the value for all variables that exceed their threshold
    for (Entry<MemoryLocation, ValueAndType> e : state.getConstants()) {
      MemoryLocation memoryLocation = e.getKey();
      assignments.updateAssignmentInformation(memoryLocation, e.getValue().getValue());

      if (assignments.exceedsThreshold(memoryLocation)) {
        state.forget(memoryLocation);
      }
    }
  }
}
