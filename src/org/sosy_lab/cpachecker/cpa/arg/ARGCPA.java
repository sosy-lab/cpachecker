/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collection;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SimplePrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithABM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PostProcessor;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.base.Preconditions;

public class ARGCPA extends AbstractSingleWrapperCPA implements ConfigurableProgramAnalysisWithABM, ProofChecker, PostProcessor {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ARGCPA.class);
  }

  private final LogManager logger;

  private final AbstractDomain abstractDomain;
  private final ARGTransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final ARGStopSep stopOperator;
  private final PrecisionAdjustment precisionAdjustment;
  private final Reducer reducer;
  private final ARGStatistics stats;
  private final ProofChecker wrappedProofChecker;
  private final PostProcessor innerPostProcessor;
  private final Collection<PostProcessor> postProcessors;

  private CounterexampleInfo lastCounterexample = null;

  private ARGCPA(ConfigurableProgramAnalysis cpa, Configuration config, LogManager logger) throws InvalidConfigurationException {
    super(cpa);

    this.logger = logger;
    abstractDomain = new FlatLatticeDomain();
    transferRelation = new ARGTransferRelation(cpa.getTransferRelation());

    PrecisionAdjustment wrappedPrec = cpa.getPrecisionAdjustment();
    if (wrappedPrec instanceof SimplePrecisionAdjustment) {
      precisionAdjustment = new ARGSimplePrecisionAdjustment((SimplePrecisionAdjustment) wrappedPrec);
    } else {
      precisionAdjustment = new ARGPrecisionAdjustment(cpa.getPrecisionAdjustment());
    }

    if (cpa instanceof ConfigurableProgramAnalysisWithABM) {
      Reducer wrappedReducer = ((ConfigurableProgramAnalysisWithABM)cpa).getReducer();
      if (wrappedReducer != null) {
        reducer = new ARGReducer(wrappedReducer);
      } else {
        reducer = null;
      }
    } else {
      reducer = null;
    }

    if (cpa instanceof ProofChecker) {
      this.wrappedProofChecker = (ProofChecker)cpa;
    } else {
      this.wrappedProofChecker = null;
    }

    MergeOperator wrappedMerge = getWrappedCpa().getMergeOperator();
    if (wrappedMerge == MergeSepOperator.getInstance()) {
      mergeOperator = MergeSepOperator.getInstance();
    } else {
      mergeOperator = new ARGMergeJoin(wrappedMerge);
    }
    stopOperator = new ARGStopSep(getWrappedCpa().getStopOperator(), logger, config);
    stats = new ARGStatistics(config, this);

    if (cpa instanceof PostProcessor) {
      innerPostProcessor = (PostProcessor)cpa;
    } else {
      innerPostProcessor = null;
    }
    postProcessors = new ArrayList<PostProcessor>();
    postProcessors.add(new RVARGSimplifier(config, this));
    postProcessors.add(new ARGDumper(config, this));

  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
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
  public Reducer getReducer() {
    return reducer;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode) {
    // TODO some code relies on the fact that this method is called only one and the result is the root of the ARG
    return new ARGState(getWrappedCpa().getInitialState(pNode), null);
  }

  protected LogManager getLogger() {
    return logger;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    super.collectStatistics(pStatsCollection);
  }

  public CounterexampleInfo getLastCounterexample() {
    return lastCounterexample;
  }

  public void clearCounterexample() {
    lastCounterexample = null;
  }

  public void setCounterexample(CounterexampleInfo pCounterexample) {
    checkArgument(!pCounterexample.isSpurious());
    lastCounterexample = pCounterexample;
  }

  ARGToDotWriter getRefinementGraphWriter() {
    return stats.getRefinementGraphWriter();
  }

  @Override
  public boolean areAbstractSuccessors(AbstractState pElement, CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors) throws CPATransferException, InterruptedException {
    Preconditions.checkNotNull(wrappedProofChecker, "Wrapped CPA has to implement ProofChecker interface");
    return transferRelation.areAbstractSuccessors(pElement, pCfaEdge, pSuccessors, wrappedProofChecker);
  }

  @Override
  public boolean isCoveredBy(AbstractState pElement, AbstractState pOtherElement) throws CPAException {
    Preconditions.checkNotNull(wrappedProofChecker, "Wrapped CPA has to implement ProofChecker interface");
    return stopOperator.isCoveredBy(pElement, pOtherElement, wrappedProofChecker);
  }

  @Override
  public void postProcess(ReachedSet pReached) {
    if (innerPostProcessor != null) {
      innerPostProcessor.postProcess(pReached);
    }
    for (PostProcessor postProcessor : postProcessors) {
      postProcessor.postProcess(pReached);
    }
  }
}
