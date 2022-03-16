// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.slicing;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.slicing.Slice;
import org.sosy_lab.cpachecker.util.slicing.Slicer;
import org.sosy_lab.cpachecker.util.slicing.SlicerFactory;

/**
 * CPA that performs program slicing during analysis. The Slicing CPA wraps another CPA. If a CFA
 * edge <code>g = (l, op, l')</code> is not relevant, the program operation <code>op</code> is not
 * considered - the wrapped CPA will handle the edge as if it was <code>(l, noop, l')</code>.
 *
 * <p>The set of relevant edges for the slicing criteria is stored in the {@link SlicingPrecision}.
 * This set can be iteratively created through the {@link
 * org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm CEGAR} approach. Initially, it is empty.
 */
@Options(prefix = "cpa.slicing")
public class SlicingCPA extends AbstractSingleWrapperCPA implements StatisticsProvider {

  @Option(
      secure = true,
      name = "refinableSlice",
      description =
          "Whether to use a refinable slicing precision that starts with an empty slice, or a"
              + " statically computed, fixed slicing precision")
  private boolean useRefinableSlice = false;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final CFA cfa;
  private final Specification spec;

  private final SlicerFactory slicerFactory;
  private final Slicer slicer;

  private TransferRelation transferRelation;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private PrecisionAdjustment precisionAdjustment;

  /** Returns the factory for creating this CPA. */
  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SlicingCPA.class);
  }

  public SlicingCPA(
      final ConfigurableProgramAnalysis pCpa,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final Configuration pConfig,
      final CFA pCfa,
      final Specification pSpec)
      throws CPAException, InvalidConfigurationException {
    super(pCpa);
    pConfig.inject(this);

    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    config = pConfig;
    cfa = pCfa;
    spec = pSpec;

    transferRelation = new SlicingTransferRelation(pCpa.getTransferRelation());
    mergeOperator = new PrecisionDelegatingMerge(pCpa.getMergeOperator());
    stopOperator = new PrecisionDelegatingStop(pCpa.getStopOperator());
    precisionAdjustment = new PrecisionDelegatingPrecisionAdjustment(pCpa.getPrecisionAdjustment());

    slicerFactory = new SlicerFactory();
    slicer = slicerFactory.create(logger, shutdownNotifier, config, pCfa);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return super.getInitialState(node, partition);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    Precision wrappedPrec = getWrappedCpa().getInitialPrecision(pNode, pPartition);

    ImmutableSet<CFAEdge> relevantEdges;
    if (useRefinableSlice) {
      relevantEdges = ImmutableSet.of();
    } else {
      relevantEdges = computeSlice(cfa, spec).getRelevantEdges();
    }

    return new SlicingPrecision(wrappedPrec, relevantEdges);
  }

  Slicer getSlicer() {
    return slicer;
  }

  private Slice computeSlice(CFA pCfa, Specification pSpec) throws InterruptedException {
    return slicer.getSlice(pCfa, pSpec);
  }

  public LogManager getLogger() {
    return logger;
  }

  public ShutdownNotifier getShutdownNotifier() {
    return shutdownNotifier;
  }

  public Configuration getConfig() {
    return config;
  }

  public CFA getCfa() {
    return cfa;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {

    slicerFactory.collectStatistics(pStatsCollection);

    if (slicer instanceof StatisticsProvider) {
      ((StatisticsProvider) slicer).collectStatistics(pStatsCollection);
    }

    super.collectStatistics(pStatsCollection);
  }
}
