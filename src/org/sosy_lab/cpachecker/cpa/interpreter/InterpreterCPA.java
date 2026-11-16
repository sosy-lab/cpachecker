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
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation.ValueTransferOptions;
import org.sosy_lab.cpachecker.util.CPAs;

/**
 * CPA that interprets a program concretely instead of abstracting it, cf. the {@linkplain
 * org.sosy_lab.cpachecker.cpa.interpreter package documentation}.
 *
 * <p>It differs from the analysis that it wraps (usually a {@link
 * org.sosy_lab.cpachecker.cpa.composite.CompositeCPA} with a {@link
 * org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA}) in that it allows no nondeterministic
 * choice, i.e., a state with more than one successor aborts the analysis, and in that it never
 * merges or covers a state, so that the analysis takes exactly the steps that the program takes.
 * {@link InterpreterState} documents which verdict such an execution allows.
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
  private final @Nullable SMGOptions smgOptions;

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
    witnessExporter = new InterpreterWitnessExporter(pConfig, pCfa, pSpecification, logger);
    sampler =
        sampleNondeterministicValues ? new InterpreterSampler(pConfig, pCfa, logger, stats) : null;
    ValueAnalysisCPA valueAnalysis = CPAs.retrieveCPA(pCpa, ValueAnalysisCPA.class);
    valueTransferOptions = valueAnalysis == null ? null : valueAnalysis.getTransferOptions();
    SMGCPA smg = CPAs.retrieveCPA(pCpa, SMGCPA.class);
    smgOptions = smg == null ? null : smg.getSMGOptions();
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
        valueTransferOptions,
        smgOptions);
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
