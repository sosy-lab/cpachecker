/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ParallelCFAS;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithABM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGLocationMapping;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGCFAEdge;

import com.google.common.collect.ImmutableMap;

public class ARTCPA extends AbstractSingleWrapperCPA implements ConfigurableProgramAnalysisWithABM {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ARTCPA.class);
  }

  private final LogManager logger;

  private final AbstractDomain abstractDomain;
  private final TransferRelation wrappedTranferRelation;
  private ARTTransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final PrecisionAdjustment precisionAdjustment;
  private final Reducer reducer;
  private final Statistics stats;

  // TODO check if needed
  private ParallelCFAS pcfa;
  private int tid = Integer.MAX_VALUE;

  private Path targetPath = null;



  private ARTCPA(ConfigurableProgramAnalysis cpa, Configuration config, LogManager logger) throws InvalidConfigurationException {
    super(cpa);

    this.logger = logger;
    abstractDomain = new FlatLatticeDomain();
    this.wrappedTranferRelation = cpa.getTransferRelation();
    precisionAdjustment = new ARTPrecisionAdjustment(cpa.getPrecisionAdjustment());
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

    MergeOperator wrappedMerge = getWrappedCpa().getMergeOperator();
    if (wrappedMerge == MergeSepOperator.getInstance()) {
      mergeOperator = MergeSepOperator.getInstance();
    } else {
      mergeOperator = new ARTMergeJoin(wrappedMerge);
    }
    stopOperator = new ARTStopSep(getWrappedCpa());
    stats = new ARTStatistics(config, this);
  }

  public void setData(ParallelCFAS pcfa, int tid){
    this.pcfa = pcfa;
    this.tid = tid;
    this.transferRelation = new ARTTransferRelation(wrappedTranferRelation, tid);
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
  public AbstractElement getInitialElement (CFANode pNode) {
    // TODO some code relies on the fact that this method is called only one and the result is the root of the ART
    Map<Integer, Integer> locMap = new HashMap<Integer, Integer>(pcfa.getThreadNo()-1);

    for (int i=0; i<pcfa.getThreadNo(); i++){

      if (i != tid){
        locMap.put(i, 1);
      }
    }

    ImmutableMap<Integer, Integer> locationClasses = ImmutableMap.copyOf(locMap);
    return new ARTElement(getWrappedCpa().getInitialElement(pNode), Collections.<ARTElement, CFAEdge> emptyMap(), Collections.<ARTElement, RGCFAEdge> emptyMap(), locationClasses, tid);
  }

  protected LogManager getLogger() {
    return logger;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    super.collectStatistics(pStatsCollection);
  }

  Path getTargetPath() {
    return targetPath;
  }

  void setTargetPath(Path pTargetPath) {
    targetPath = pTargetPath;
  }


  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    RGLocationMapping lm = RGLocationMapping.getEmpty(pcfa.getCfa(tid));
    return new ARTPrecision(lm, (CompositePrecision) super.getInitialPrecision(pNode));
  }

}