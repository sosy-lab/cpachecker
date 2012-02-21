/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.art;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SimplePrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithABM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.ProofChecker;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.base.Preconditions;

public class ARTCPA extends AbstractSingleWrapperCPA implements ConfigurableProgramAnalysisWithABM, ProofChecker {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ARTCPA.class);
  }

  private final LogManager logger;

  private final AbstractDomain abstractDomain;
  private final ARTTransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final ARTStopSep stopOperator;
  private final PrecisionAdjustment precisionAdjustment;
  private final Reducer reducer;
  private final Statistics stats;
  private final ProofChecker wrappedProofChecker;

  private CounterexampleInfo lastCounterexample = null;

  private ARTCPA(ConfigurableProgramAnalysis cpa, Configuration config, LogManager logger) throws InvalidConfigurationException {
    super(cpa);

    this.logger = logger;
    abstractDomain = new FlatLatticeDomain();
    transferRelation = new ARTTransferRelation(cpa.getTransferRelation());

    PrecisionAdjustment wrappedPrec = cpa.getPrecisionAdjustment();
    if (wrappedPrec instanceof SimplePrecisionAdjustment) {
      precisionAdjustment = new ARTSimplePrecisionAdjustment((SimplePrecisionAdjustment) wrappedPrec);
    } else {
      precisionAdjustment = new ARTPrecisionAdjustment(cpa.getPrecisionAdjustment());
    }

    if (cpa instanceof ConfigurableProgramAnalysisWithABM) {
      Reducer wrappedReducer = ((ConfigurableProgramAnalysisWithABM)cpa).getReducer();
      if (wrappedReducer != null) {
        reducer = new ARTReducer(wrappedReducer);
      } else {
        reducer = null;
      }
    } else {
      reducer = null;
    }

    if(cpa instanceof ProofChecker) {
      this.wrappedProofChecker = (ProofChecker)cpa;
    } else {
      this.wrappedProofChecker = null;
    }

    MergeOperator wrappedMerge = getWrappedCpa().getMergeOperator();
    if (wrappedMerge == MergeSepOperator.getInstance()) {
      mergeOperator = MergeSepOperator.getInstance();
    } else {
      mergeOperator = new ARTMergeJoin(wrappedMerge);
    }
    stopOperator = new ARTStopSep(getWrappedCpa().getStopOperator(), logger);
    stats = new ARTStatistics(config, this);
  }

  @Override
  public AbstractDomain getAbstractDomain ()
  {
    return abstractDomain;
  }

  @Override
  public TransferRelation getTransferRelation ()
  {
    return transferRelation;
  }

  @Override
  public MergeOperator getMergeOperator ()
  {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator ()
  {
    return stopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment () {
    return precisionAdjustment;
  }

  @Override
  public Reducer getReducer() {
    return reducer;
  }

  @Override
  public ARTElement getInitialElement (CFANode pNode) {
    // TODO some code relies on the fact that this method is called only one and the result is the root of the ART
    return new ARTElement(getWrappedCpa().getInitialElement(pNode), null);
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

  @Override
  public boolean areAbstractSuccessors(AbstractElement pElement, CFAEdge pCfaEdge,
      Collection<? extends AbstractElement> pSuccessors) throws CPATransferException, InterruptedException {
    Preconditions.checkNotNull(wrappedProofChecker, "Wrapped CPA has to implement ProofChecker interface");
    return transferRelation.areAbstractSuccessors(pElement, pCfaEdge, pSuccessors, wrappedProofChecker);
  }

  @Override
  public boolean isCoveredBy(AbstractElement pElement, AbstractElement pOtherElement) throws CPAException {
    Preconditions.checkNotNull(wrappedProofChecker, "Wrapped CPA has to implement ProofChecker interface");
    return stopOperator.isCoveredBy(pElement, pOtherElement, wrappedProofChecker);
  }
}