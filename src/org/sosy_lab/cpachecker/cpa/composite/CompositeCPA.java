// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SimplePrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

@Options(prefix = "cpa.composite")
public final class CompositeCPA
    implements StatisticsProvider, WrapperCPA, ConfigurableProgramAnalysisWithBAM, ProofChecker {

  @Option(
      secure = true,
      toUppercase = true,
      values = {"PLAIN", "AGREE"},
      description =
          "which composite merge operator to use (plain or agree)\n"
              + "Both delegate to the component cpas, but agree only allows "
              + "merging if all cpas agree on this. This is probably what you want.")
  private String merge = "AGREE";

  @Option(
      secure = true,
      description =
          "inform Composite CPA if it is run in a CPA enabled analysis because then it must "
              + "behave differently during merge.")
  private boolean inCPAEnabledAnalysis = false;

  @Option(
      secure = true,
      description =
          "By enabling this option the CompositeTransferRelation will compute abstract successors"
              + " for as many edges as possible in one call. For any chain of edges in the CFA"
              + " which does not have more than one outgoing or leaving edge the components of the"
              + " CompositeCPA are called for each of the edges in this chain. Strengthening is"
              + " still computed after every edge. The main difference is that while this option is"
              + " enabled not every ARGState may have a single edge connecting to the child/parent"
              + " ARGState but it may instead be a list.")
  private boolean aggregateBasicBlocks = false;

  private static class CompositeCPAFactory extends AbstractCPAFactory {

    private CFA cfa = null;
    private ImmutableList<ConfigurableProgramAnalysis> cpas = null;

    @Override
    public ConfigurableProgramAnalysis createInstance() throws InvalidConfigurationException {
      Preconditions.checkState(cpas != null, "CompositeCPA needs wrapped CPAs!");
      Preconditions.checkState(cfa != null, "CompositeCPA needs CFA information!");
      return new CompositeCPA(getConfiguration(), cfa, cpas);
    }

    @Override
    public CPAFactory setChild(ConfigurableProgramAnalysis pChild)
        throws UnsupportedOperationException {
      throw new UnsupportedOperationException("Use CompositeCPA to wrap several CPAs!");
    }

    @Override
    public CPAFactory setChildren(List<ConfigurableProgramAnalysis> pChildren) {
      Preconditions.checkNotNull(pChildren);
      Preconditions.checkArgument(!pChildren.isEmpty());
      Preconditions.checkState(cpas == null);

      cpas = ImmutableList.copyOf(pChildren);
      return this;
    }

    @Override
    public <T> CPAFactory set(T pObject, Class<T> pClass) throws UnsupportedOperationException {
      if (pClass.equals(CFA.class)) {
        cfa = (CFA) pObject;
      }
      return super.set(pObject, pClass);
    }
  }

  public static CPAFactory factory() {
    return new CompositeCPAFactory();
  }

  private final ImmutableList<ConfigurableProgramAnalysis> cpas;
  private final CFA cfa;
  private final Supplier<MergeOperator> mergeSupplier;

  private CompositeCPA(
      Configuration config, CFA pCfa, ImmutableList<ConfigurableProgramAnalysis> cpas)
      throws InvalidConfigurationException {
    config.inject(this);
    cfa = pCfa;
    this.cpas = cpas;
    mergeSupplier = buildMergeOperatorSupplier();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return new CompositeDomain(
        transformedImmutableListCopy(cpas, ConfigurableProgramAnalysis::getAbstractDomain));
  }

  @Override
  public CompositeTransferRelation getTransferRelation() {
    return new CompositeTransferRelation(
        transformedImmutableListCopy(cpas, ConfigurableProgramAnalysis::getTransferRelation),
        cfa,
        aggregateBasicBlocks);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeSupplier.get();
  }

  /**
   * Build a function that lazily instantiates a merge operator with fresh wrapped merge operators
   * from the CPAs.
   */
  private Supplier<MergeOperator> buildMergeOperatorSupplier()
      throws InvalidConfigurationException {
    if (cpas.stream()
        .map(ConfigurableProgramAnalysis::getMergeOperator)
        .allMatch(mergeOp -> mergeOp == MergeSepOperator.getInstance())) {
      return () -> MergeSepOperator.getInstance();
    }

    switch (merge) {
      case "AGREE":
        if (inCPAEnabledAnalysis) {
          PredicateCPA predicateCPA =
              Collections3.filterByClass(cpas.stream(), PredicateCPA.class)
                  .findFirst()
                  .orElseThrow(
                      () ->
                          new InvalidConfigurationException(
                              "Option 'cpa.composite.inCPAEnabledAnalysis' needs PredicateCPA"));
          return () ->
              new CompositeMergeAgreeCPAEnabledAnalysisOperator(
                  getMergeOperators(), getStopOperators(), predicateCPA.getPredicateManager());
        } else {
          return () -> new CompositeMergeAgreeOperator(getMergeOperators(), getStopOperators());
        }

      case "PLAIN":
        if (inCPAEnabledAnalysis) {
          throw new InvalidConfigurationException(
              "Merge PLAIN is currently not supported for CompositeCPA in predicated analysis");
        } else {
          return () -> new CompositeMergePlainOperator(getMergeOperators());
        }

      default:
        throw new AssertionError();
    }
  }

  private ImmutableList<MergeOperator> getMergeOperators() {
    return transformedImmutableListCopy(cpas, ConfigurableProgramAnalysis::getMergeOperator);
  }

  private ImmutableList<StopOperator> getStopOperators() {
    return transformedImmutableListCopy(cpas, ConfigurableProgramAnalysis::getStopOperator);
  }

  @Override
  public CompositeStopOperator getStopOperator() {
    return new CompositeStopOperator(getStopOperators());
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    ImmutableList<PrecisionAdjustment> precisionAdjustments =
        transformedImmutableListCopy(cpas, ConfigurableProgramAnalysis::getPrecisionAdjustment);

    if (precisionAdjustments.stream().allMatch(prec -> prec instanceof SimplePrecisionAdjustment)) {
      @SuppressWarnings("unchecked") // cast is safe because we just checked this
      ImmutableList<SimplePrecisionAdjustment> simplePrecisionAdjustments =
          (ImmutableList<SimplePrecisionAdjustment>)
              (ImmutableList<? extends PrecisionAdjustment>) precisionAdjustments;
      return new CompositeSimplePrecisionAdjustment(simplePrecisionAdjustments);

    } else {
      return new CompositePrecisionAdjustment(precisionAdjustments);
    }
  }

  @Override
  public Reducer getReducer() throws InvalidConfigurationException {
    ImmutableList.Builder<Reducer> wrappedReducers = ImmutableList.builder();
    for (ConfigurableProgramAnalysis cpa : cpas) {
      checkState(
          cpa instanceof ConfigurableProgramAnalysisWithBAM,
          "wrapped CPA does not support BAM: %s",
          cpa.getClass().getCanonicalName());
      wrappedReducers.add(((ConfigurableProgramAnalysisWithBAM) cpa).getReducer());
    }
    return new CompositeReducer(wrappedReducers.build());
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    Preconditions.checkNotNull(pNode);

    ImmutableList.Builder<AbstractState> initialStates = ImmutableList.builder();
    for (ConfigurableProgramAnalysis sp : cpas) {
      initialStates.add(sp.getInitialState(pNode, pPartition));
    }

    return new CompositeState(initialStates.build());
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition partition)
      throws InterruptedException {
    Preconditions.checkNotNull(pNode);

    ImmutableList.Builder<Precision> initialPrecisions = ImmutableList.builder();
    for (ConfigurableProgramAnalysis sp : cpas) {
      initialPrecisions.add(sp.getInitialPrecision(pNode, partition));
    }
    return new CompositePrecision(initialPrecisions.build());
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    from(cpas)
        .filter(StatisticsProvider.class)
        .forEach(cpa -> cpa.collectStatistics(pStatsCollection));
  }

  @Override
  public <T extends ConfigurableProgramAnalysis> T retrieveWrappedCpa(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    }
    for (ConfigurableProgramAnalysis cpa : cpas) {
      if (pType.isAssignableFrom(cpa.getClass())) {
        return pType.cast(cpa);
      } else if (cpa instanceof WrapperCPA) {
        T result = ((WrapperCPA) cpa).retrieveWrappedCpa(pType);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  @Override
  public ImmutableList<ConfigurableProgramAnalysis> getWrappedCPAs() {
    return cpas;
  }

  @Override
  public boolean areAbstractSuccessors(
      AbstractState pElement, CFAEdge pCfaEdge, Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    return getTransferRelation().areAbstractSuccessors(pElement, pCfaEdge, pSuccessors, cpas);
  }

  @Override
  public boolean isCoveredBy(AbstractState pElement, AbstractState pOtherElement)
      throws CPAException, InterruptedException {
    return getStopOperator().isCoveredBy(pElement, pOtherElement, cpas);
  }

  @Override
  public void setPartitioning(BlockPartitioning partitioning) {
    cpas.forEach(
        cpa -> {
          checkState(
              cpa instanceof ConfigurableProgramAnalysisWithBAM,
              "wrapped CPA does not support BAM: %s",
              cpa.getClass().getCanonicalName());
          ((ConfigurableProgramAnalysisWithBAM) cpa).setPartitioning(partitioning);
        });
  }

  @Override
  public boolean isCoveredByRecursiveState(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    CompositeState state1 = (CompositeState) pState1;
    CompositeState state2 = (CompositeState) pState2;

    List<AbstractState> states1 = state1.getWrappedStates();
    List<AbstractState> states2 = state2.getWrappedStates();

    if (states1.size() != cpas.size()) {
      return false;
    }

    for (int idx = 0; idx < states1.size(); idx++) {
      if (!((ConfigurableProgramAnalysisWithBAM) cpas.get(idx))
          .isCoveredByRecursiveState(states1.get(idx), states2.get(idx))) {
        return false;
      }
    }

    return true;
  }
}
