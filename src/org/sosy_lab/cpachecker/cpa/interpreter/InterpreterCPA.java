// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interpreter;

import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
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
import org.sosy_lab.cpachecker.cpa.interpreter.InterpreterState.StackFrame;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation.ValueTransferOptions;
import org.sosy_lab.cpachecker.util.CPAs;

/**
 * CPA that interprets a program concretely instead of abstracting it.
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
 * <p>For an exact execution (cf. {@link InterpreterState} for when an execution is exact):
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
 *
 * <p>The idea of this CPA is to simulate an abstract execution of the program. Everything else
 * follows from that idea, in particular the restriction to a single successor and the chain of
 * steps that one call of the transfer relation performs.
 */
@Options(prefix = "cpa.interpreter")
public class InterpreterCPA extends AbstractSingleWrapperCPA {

  @Option(
      secure = true,
      description =
          "Execute programs with inputs by asking the SMT solver for one assignment of the inputs"
              + " instead of aborting on the first nondeterministic choice. This needs symbolic"
              + " values (cpa.value.unknownValueHandling=INTRODUCE_SYMBOLIC) and the ConstraintsCPA"
              + " in the CPA configuration. Only one of the possible executions is explored, so"
              + " the analysis can then only report FALSE, never TRUE.")
  private boolean sampleNondeterministicValues = false;

  private final LogManagerWithoutDuplicates logger;
  private final ShutdownNotifier shutdownNotifier;
  private final InterpreterStatistics stats = new InterpreterStatistics();
  private final InterpreterWitnessExporter witnessExporter;
  private final @Nullable InterpreterSampler sampler;
  private final @Nullable ValueTransferOptions valueTransferOptions;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(InterpreterCPA.class);
  }

  private InterpreterCPA(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      Specification pSpecification)
      throws InvalidConfigurationException {
    super(pCpa);
    pConfig.inject(this);
    logger = new LogManagerWithoutDuplicates(pLogger);
    shutdownNotifier = pShutdownNotifier;
    witnessExporter = new InterpreterWitnessExporter(pConfig, pCfa, pSpecification, logger, pCpa);
    sampler =
        sampleNondeterministicValues ? new InterpreterSampler(pConfig, pCfa, logger, stats) : null;
    ValueAnalysisCPA valueAnalysis = CPAs.retrieveCPA(pCpa, ValueAnalysisCPA.class);
    valueTransferOptions = valueAnalysis == null ? null : valueAnalysis.getTransferOptions();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    // States are never merged or covered, so the domain is never used for anything but identity.
    return new FlatLatticeDomain();
  }

  @Override
  public InterpreterTransferRelation getTransferRelation() {
    return new InterpreterTransferRelation(
        getWrappedCpa().getTransferRelation(),
        shutdownNotifier,
        logger,
        stats,
        witnessExporter,
        sampler,
        valueTransferOptions);
  }

  @Override
  public MergeOperator getMergeOperator() {
    // There is only a single execution, so there is never anything to merge.
    return MergeSepOperator.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    // Stop-never is what lets termination of the analysis prove termination of the program: a
    // coverage check would stop the analysis when the program revisits a state, i.e., exactly
    // when the program does not terminate.
    return StopNeverOperator.getInstance();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return new InterpreterPrecisionAdjustment(getWrappedCpa().getPrecisionAdjustment());
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    // The execution starts inside the entry function, so its frame is already on the stack
    // (analogously to org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA#getInitialState).
    return new InterpreterState(
        super.getInitialState(pNode, pPartition), StackFrame.initial(pNode.getFunctionName()));
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    super.collectStatistics(pStatsCollection);
    pStatsCollection.add(stats);
    pStatsCollection.add(witnessExporter);
  }
}
