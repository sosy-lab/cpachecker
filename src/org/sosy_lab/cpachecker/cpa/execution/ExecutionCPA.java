// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.execution;

import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopNeverOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation.ValueTransferOptions;
import org.sosy_lab.cpachecker.util.CPAs;

/**
 * CPA that executes a program instead of abstracting it.
 *
 * <p>This CPA wraps another CPA, usually a {@link
 * org.sosy_lab.cpachecker.cpa.composite.CompositeCPA} consisting of {@link
 * org.sosy_lab.cpachecker.cpa.location.LocationCPA}, {@link
 * org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA}, {@link
 * org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA}, and the specification automata. It evaluates
 * the program edge by edge and requires that every state has exactly one or zero successors, i.e.,
 * that the program does not make any nondeterministic choice. If this does not hold, the analysis
 * aborts with an {@link org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException} and the result
 * is {@code UNKNOWN}.
 *
 * <p>The state tracks whether ignored external calls have made the execution unsound or imprecise.
 * A target state is reported only if the execution is still precise, and termination without a
 * target state proves safety only if the execution is still sound. Otherwise the analysis aborts
 * with an exception and reports {@code UNKNOWN}. For an exact execution:
 *
 * <ul>
 *   <li>If a target state is reached, the specification is really violated ({@code FALSE}).
 *   <li>If the analysis terminates without reaching a target state, no execution of the program
 *       violates the specification, because there is only one execution ({@code TRUE}). This holds
 *       for every specification that is expressed by target states, in particular for reachability
 *       and for the memory-safety properties.
 *   <li>Termination of the analysis implies termination of the program, because no abstract state
 *       is ever covered by another one (the stop operator is {@code stop-never}), so the analysis
 *       performs exactly as many steps as the program does.
 * </ul>
 */
@Options(prefix = "cpa.execution")
public class ExecutionCPA extends AbstractSingleWrapperCPA {

  @Option(
      secure = true,
      description =
          "Number of program steps that are executed within a single call of the transfer relation."
              + " Intermediate states are not added to the reached set, so the larger this value"
              + " is, the less memory the analysis needs. Use a value of -1 to execute the whole"
              + " program within a single call of the transfer relation (most memory efficient),"
              + " and a value of 1 to add every state to the reached set (needed for a complete"
              + " ARG, e.g., for exporting counterexamples).")
  private int stepsPerTransfer = -1;

  @Option(
      secure = true,
      description =
          "Restore the values of the local variables of a caller when a recursive function call"
              + " returns. A value analysis identifies local variables by the name of their"
              + " function, so all invocations of a function share the same memory locations and a"
              + " recursive call overwrites the variables of its caller. This option lets"
              + " ExecutionCPA remember and restore those values, such that recursive programs can"
              + " be executed precisely. The value analysis itself is not changed by this.")
  private boolean restoreCallerValuesOnRecursion = true;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ExecutionCPA.class);
  }

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final ExecutionStatistics stats = new ExecutionStatistics();
  private final ExecutionWitnessExporter witnessExporter;
  private final ExecutionSampler sampler;
  private final @Nullable ValueTransferOptions valueTransferOptions;

  private ExecutionCPA(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      Specification pSpecification)
      throws InvalidConfigurationException {
    super(pCpa);
    pConfig.inject(this);
    if (stepsPerTransfer == 0) {
      throw new InvalidConfigurationException(
          "cpa.execution.stepsPerTransfer needs to be at least 1 (or -1 for no limit),"
              + " otherwise the analysis would not make any progress");
    }
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    witnessExporter = new ExecutionWitnessExporter(pConfig, pCfa, pSpecification, pLogger);
    sampler = new ExecutionSampler(pConfig, pCfa, pLogger, stats);
    ValueAnalysisCPA valueAnalysis = CPAs.retrieveCPA(pCpa, ValueAnalysisCPA.class);
    valueTransferOptions = valueAnalysis == null ? null : valueAnalysis.getTransferOptions();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    // States are never merged or covered, so the domain is never used for anything but identity.
    return new FlatLatticeDomain();
  }

  @Override
  public ExecutionTransferRelation getTransferRelation() {
    return new ExecutionTransferRelation(
        getWrappedCpa().getTransferRelation(),
        shutdownNotifier,
        logger,
        stats,
        witnessExporter,
        sampler,
        valueTransferOptions,
        stepsPerTransfer,
        restoreCallerValuesOnRecursion);
  }

  /** There is only a single execution, so there is never anything to merge. */
  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  /**
   * States are never covered. This is essential for the claim that termination of the analysis
   * implies termination of the program: a coverage check would stop the analysis when the program
   * revisits a state, i.e., exactly when the program does not terminate.
   */
  @Override
  public StopOperator getStopOperator() {
    return StopNeverOperator.getInstance();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return new ExecutionPrecisionAdjustment(getWrappedCpa().getPrecisionAdjustment());
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return new ExecutionState(super.getInitialState(pNode, pPartition), null);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    super.collectStatistics(pStatsCollection);
    pStatsCollection.add(stats);
    pStatsCollection.add(witnessExporter);
  }
}
