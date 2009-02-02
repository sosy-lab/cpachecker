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
package cpa.explicit;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;

public class ExplicitAnalysisCPA implements ConfigurableProgramAnalysis {

  private AbstractDomain abstractDomain;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private TransferRelation transferRelation;
  private PrecisionAdjustment precisionAdjustment;

  public ExplicitAnalysisCPA (String mergeType, String stopType) throws CPAException {
    ExplicitAnalysisDomain explicitAnalysisDomain = new ExplicitAnalysisDomain ();
    MergeOperator explicitAnalysisMergeOp = null;
    if(mergeType.equals("sep")){
      explicitAnalysisMergeOp = new ExplicitAnalysisMergeSep (explicitAnalysisDomain);
    }
    if(mergeType.equals("join")){
      explicitAnalysisMergeOp = new ExplicitAnalysisMergeJoin (explicitAnalysisDomain);
    }

    StopOperator explicitAnalysisStopOp = null;

    if(stopType.equals("sep")){
      explicitAnalysisStopOp = new ExplicitAnalysisStopSep (explicitAnalysisDomain);
    }
    if(stopType.equals("join")){
      explicitAnalysisStopOp = new ExplicitAnalysisStopJoin (explicitAnalysisDomain);
    }

    TransferRelation explicitAnalysisTransferRelation = new ExplicitAnalysisTransferRelation (explicitAnalysisDomain);

    this.abstractDomain = explicitAnalysisDomain;
    this.mergeOperator = explicitAnalysisMergeOp;
    this.stopOperator = explicitAnalysisStopOp;
    this.transferRelation = explicitAnalysisTransferRelation;
    this.precisionAdjustment = new ExplicitAnalysisPrecisionAdjustment();
  }

  public AbstractDomain getAbstractDomain ()
  {
    return abstractDomain;
  }

  public MergeOperator getMergeOperator ()
  {
    return mergeOperator;
  }

  public StopOperator getStopOperator ()
  {
    return stopOperator;
  }

  public TransferRelation getTransferRelation ()
  {
    return transferRelation;
  }

  public AbstractElement getInitialElement (CFAFunctionDefinitionNode node)
  {
    return new ExplicitAnalysisElement();
  }

  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return new ExplicitAnalysisPrecision();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

}
