/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
import org.sosy_lab.cpachecker.core.Specification;
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
          "Whether to use a refinable slicing precision that starts with an empty slice, or a statically computed, fixed slicing precision")
  private boolean useRefinableSlice = false;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final CFA cfa;
  private final Specification spec;

  private final Slicer slicer;

  private TransferRelation transferRelation;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private PrecisionAdjustment precisionAdjustment;


  /**
   * Returns the factory for creating this CPA.
   */
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
      throws InvalidConfigurationException {
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

    slicer = new SlicerFactory().create(logger, shutdownNotifier, config, pCfa);
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
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return super.getInitialState(node, partition);
  }

  @Override
  public Precision getInitialPrecision(
      CFANode pNode, StateSpacePartition pPartition) throws InterruptedException {
    Precision wrappedPrec = getWrappedCpa().getInitialPrecision(pNode, pPartition);

    ImmutableSet<CFAEdge> relevantEdges;
    if (useRefinableSlice) {
      relevantEdges = ImmutableSet.of();
    } else {
      relevantEdges = computeSlice(cfa, spec).getRelevantEdges();
    }

    return new SlicingPrecision(wrappedPrec, relevantEdges);
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
    if (slicer instanceof StatisticsProvider) {
      ((StatisticsProvider) slicer).collectStatistics(pStatsCollection);
    }

    super.collectStatistics(pStatsCollection);
  }
}
