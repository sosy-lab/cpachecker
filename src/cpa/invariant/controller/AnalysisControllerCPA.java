/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.invariant.controller;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.defaults.MergeSepOperator;
import cpa.common.defaults.StopNeverOperator;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;

/**
 * @author g.theoduloz
 *
 */
public class AnalysisControllerCPA implements ConfigurableProgramAnalysis {

  private final AnalysisControllerDomain abstractDomain;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final TransferRelation transferRelation;
  private final PrecisionAdjustment precisionAdjustment;
  
  private final List<StopHeuristics<? extends StopHeuristicsData>> enabledHeuristics;
  
  /** Return the immutable list of enables heuristics */
  public List<StopHeuristics<? extends StopHeuristicsData>> getEnabledHeuristics()
  {
    return enabledHeuristics;
  }
  
  public AnalysisControllerCPA(String mergeOp, String stopOp)
  {
    // TODO make this parametric
    LinkedList<StopHeuristics<? extends StopHeuristicsData>> heuristics = new LinkedList<StopHeuristics<? extends StopHeuristicsData>>();
    heuristics.add(new EdgeCountHeuristics());
    enabledHeuristics = Collections.unmodifiableList(heuristics);
    
    abstractDomain = new AnalysisControllerDomain(this);
    mergeOperator = new MergeSepOperator();
    stopOperator = new StopNeverOperator();
    transferRelation = new AnalysisControllerTransferRelation(abstractDomain);
    precisionAdjustment = new AnalysisControllerPrecisionAdjustment();
  }
  
  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <AE extends AbstractElement> AE getInitialElement(CFAFunctionDefinitionNode node) {
    return (AE) AnalysisControllerElement.getInitial(this, node);
  }

  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return null;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

}
