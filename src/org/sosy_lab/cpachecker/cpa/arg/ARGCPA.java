// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.util.AbstractStates.getOutgoingEdges;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.Collection;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.NoOpReducer;
import org.sosy_lab.cpachecker.core.defaults.SimplePrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCoveringStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

@Options(prefix = "cpa.arg")
public class ARGCPA extends AbstractSingleWrapperCPA
    implements ConfigurableProgramAnalysisWithBAM, ProofChecker {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ARGCPA.class);
  }

  @Option(
      secure = true,
      description =
          "inform ARG CPA if it is run in an analysis with enabler CPA because then it must "
              + "behave differently during merge.")
  private boolean inCPAEnabledAnalysis = false;

  @Option(
      secure = true,
      description =
          "inform merge operator in CPA enabled analysis that it should delete the subgraph "
              + "of the merged node which is required to get at most one successor per CFA edge.")
  private boolean deleteInCPAEnabledAnalysis = false;

  @Option(
      secure = true,
      description =
          "whether to keep covered states in the reached set as addition to keeping them in the"
              + " ARG")
  private boolean keepCoveredStatesInReached = false;

  @Option(
      secure = true,
      description =
          "prevent the stop-operator from aborting the stop-check early when it crosses a target"
              + " state")
  private boolean coverTargetStates = false;

  @Option(
      secure = true,
      description =
          "Enable reduction for nested abstract states when entering or leaving a block abstraction"
              + " for BAM. The reduction can lead to a higher cache-hit-rate for BAM and a faster"
              + " sub-analysis for blocks.")
  private boolean enableStateReduction = true;

  private final LogManager logger;
  private final ARGMergeJoin.MergeOptions mergeOptions;
  private final ARGStatistics stats;

  private ARGCPA(
      ConfigurableProgramAnalysis cpa,
      Configuration config,
      LogManager logger,
      Specification pSpecification,
      CFA cfa)
      throws InvalidConfigurationException {
    super(cpa);
    config.inject(this);
    this.logger = logger;
    mergeOptions = new ARGMergeJoin.MergeOptions(config);
    stats = new ARGStatistics(config, logger, this, pSpecification, cfa);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return new FlatLatticeDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new ARGTransferRelation(getWrappedCpa().getTransferRelation());
  }

  @Override
  public MergeOperator getMergeOperator() {
    MergeOperator wrappedMergeOperator = getWrappedCpa().getMergeOperator();
    if (wrappedMergeOperator == MergeSepOperator.getInstance()) {
      return MergeSepOperator.getInstance();
    } else if (inCPAEnabledAnalysis) {
      return new ARGMergeJoinCPAEnabledAnalysis(wrappedMergeOperator, deleteInCPAEnabledAnalysis);
    } else {
      return new ARGMergeJoin(
          wrappedMergeOperator, getWrappedCpa().getAbstractDomain(), logger, mergeOptions);
    }
  }

  @Override
  public ForcedCoveringStopOperator getStopOperator() {
    return new ARGStopSep(
        getWrappedCpa().getStopOperator(),
        logger,
        inCPAEnabledAnalysis,
        keepCoveredStatesInReached,
        coverTargetStates);
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    PrecisionAdjustment wrappedPrec = getWrappedCpa().getPrecisionAdjustment();
    if (wrappedPrec instanceof SimplePrecisionAdjustment) {
      return new ARGSimplePrecisionAdjustment((SimplePrecisionAdjustment) wrappedPrec);
    } else {
      return new ARGPrecisionAdjustment(wrappedPrec, inCPAEnabledAnalysis, stats);
    }
  }

  @Override
  public Reducer getReducer() throws InvalidConfigurationException {
    ConfigurableProgramAnalysis cpa = getWrappedCpa();
    checkState(
        cpa instanceof ConfigurableProgramAnalysisWithBAM,
        "wrapped CPA does not support BAM: %s",
        cpa.getClass().getCanonicalName());
    Reducer nestedReducer =
        enableStateReduction
            ? ((ConfigurableProgramAnalysisWithBAM) cpa).getReducer()
            : NoOpReducer.getInstance();
    return new ARGReducer(nestedReducer);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    // TODO some code relies on the fact that this method is called only once and the result is the
    // root of the ARG
    return new ARGState(getWrappedCpa().getInitialState(pNode, pPartition), null);
  }

  public LogManager getLogger() {
    return logger;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (!Iterables.any(pStatsCollection, Predicates.instanceOf(ARGStatistics.class))) {
      // we do not want to add ARGStatistics twice, if a wrapping CPA also uses it.
      // This would result in overriding the output-files due to equal file names.
      // Info: this case is one of the reasons to first collect our own statistics
      // and afterwards call super.collectStatistics().
      pStatsCollection.add(stats);
    }
    super.collectStatistics(pStatsCollection);
  }

  public ARGStatistics getARGExporter() {
    return stats;
  }

  @Override
  public boolean areAbstractSuccessors(
      AbstractState pElement, CFAEdge pCfaEdge, Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    Preconditions.checkState(
        getWrappedCpa() instanceof ProofChecker,
        "Wrapped CPA has to implement ProofChecker interface");
    ProofChecker wrappedProofChecker = (ProofChecker) getWrappedCpa();
    ARGState element = (ARGState) pElement;

    assert Iterables.elementsEqual(element.getChildren(), pSuccessors);

    AbstractState wrappedState = element.getWrappedState();
    Multimap<CFAEdge, AbstractState> wrappedSuccessors = HashMultimap.create();
    for (AbstractState absElement : pSuccessors) {
      ARGState successorElem = (ARGState) absElement;
      wrappedSuccessors.put(element.getEdgeToChild(successorElem), successorElem.getWrappedState());
    }

    if (pCfaEdge != null) {
      return wrappedProofChecker.areAbstractSuccessors(
          wrappedState, pCfaEdge, wrappedSuccessors.get(pCfaEdge));
    }

    for (CFAEdge edge : getOutgoingEdges(element)) {
      if (!wrappedProofChecker.areAbstractSuccessors(
          wrappedState, edge, wrappedSuccessors.get(edge))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isCoveredBy(AbstractState pElement, AbstractState pOtherElement)
      throws CPAException, InterruptedException {
    Preconditions.checkState(
        getWrappedCpa() instanceof ProofChecker,
        "Wrapped CPA has to implement ProofChecker interface");
    ProofChecker wrappedProofChecker = (ProofChecker) getWrappedCpa();
    AbstractState wrappedState = ((ARGState) pElement).getWrappedState();
    AbstractState wrappedOtherElement = ((ARGState) pOtherElement).getWrappedState();
    return wrappedProofChecker.isCoveredBy(wrappedState, wrappedOtherElement);
  }

  @Override
  public void setPartitioning(BlockPartitioning partitioning) {
    ConfigurableProgramAnalysis cpa = getWrappedCpa();
    assert cpa instanceof ConfigurableProgramAnalysisWithBAM;
    ((ConfigurableProgramAnalysisWithBAM) cpa).setPartitioning(partitioning);
  }

  @Override
  public boolean isCoveredByRecursiveState(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    return ((ConfigurableProgramAnalysisWithBAM) getWrappedCpa())
        .isCoveredByRecursiveState(
            ((ARGState) state1).getWrappedState(), ((ARGState) state2).getWrappedState());
  }
}
