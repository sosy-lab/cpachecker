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
package cpa.concrete;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.defaults.AbstractCPAFactory;
import cpa.common.defaults.MergeSepOperator;
import cpa.common.defaults.SingletonPrecision;
import cpa.common.defaults.StaticPrecisionAdjustment;
import cpa.common.defaults.StopSepOperator;
import cpa.common.interfaces.CPAFactory;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

public class ConcreteAnalysisCPA implements cpa.common.interfaces.ConfigurableProgramAnalysis {

  private static class ConcreteAnalysisCPAFactory extends AbstractCPAFactory {
    
    @Override
    public ConfigurableProgramAnalysis createInstance() throws CPAException {
      return new ConcreteAnalysisCPA();
    }
  }
  
  public static CPAFactory factory() {
    return new ConcreteAnalysisCPAFactory();
  }
  
  private ConcreteAnalysisDomain mAbstractDomain;
  private MergeOperator mMergeOperator;
  private StopOperator mStopOperator;
  private ConcreteAnalysisTransferRelation mTransferRelation;
  private PrecisionAdjustment mPrecisionAdjustment;

  public ConcreteAnalysisCPA(String pMergeType, String pStopType) {
    this();
  }
  
  public ConcreteAnalysisCPA() {
    
    this.mAbstractDomain = ConcreteAnalysisDomain.getInstance();
    
    this.mTransferRelation = new ConcreteAnalysisTransferRelation(this.mAbstractDomain);
    
    this.mMergeOperator = MergeSepOperator.getInstance();
    this.mStopOperator = new StopSepOperator(this.mAbstractDomain.getPartialOrder());
    this.mPrecisionAdjustment = StaticPrecisionAdjustment.getInstance();
  }

  public ConcreteAnalysisDomain getAbstractDomain()
  {
    return mAbstractDomain;
  }

  public MergeOperator getMergeOperator()
  {
    return this.mMergeOperator;
  }

  public StopOperator getStopOperator()
  {
    return mStopOperator;
  }

  public ConcreteAnalysisTransferRelation getTransferRelation()
  {
    return mTransferRelation;
  }

  @Override
  public ConcreteAnalysisElement getInitialElement(CFAFunctionDefinitionNode node)
  {
    return new ConcreteAnalysisElement();
  }

  @Override
  public SingletonPrecision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return mPrecisionAdjustment;
  }

}
