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
package cpa.dominator.parametric;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.defaults.MergeJoinOperator;
import cpa.common.defaults.StaticPrecisionAdjustment;
import cpa.common.defaults.StopSepOperator;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;

public class DominatorCPA {
  
  private ConfigurableProgramAnalysis cpa;
  
  private DominatorDomain abstractDomain;
  private TransferRelation transferRelation;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private PrecisionAdjustment precisionAdjustment;

	public DominatorCPA(ConfigurableProgramAnalysis cpa) throws CPAException {
	  this.cpa = cpa;
	  
		this.abstractDomain = new DominatorDomain(this.cpa);
    this.transferRelation = new DominatorTransferRelation(this.abstractDomain, this.cpa);
    this.mergeOperator = new MergeJoinOperator(abstractDomain.getJoinOperator());
		this.stopOperator = new StopSepOperator(abstractDomain.getPartialOrder());
		this.precisionAdjustment = StaticPrecisionAdjustment.getInstance();
	}

	public AbstractDomain getAbstractDomain() {
		return abstractDomain;
	}

  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

	public MergeOperator getMergeOperator() {
		return mergeOperator;
	}

	public StopOperator getStopOperator() {
		return stopOperator;
	}

  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }
	
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
    AbstractElement dominatedInitialElement_tmp = this.cpa.getInitialElement(node);

    AbstractElementWithLocation dominatedInitialElement = (AbstractElementWithLocation)dominatedInitialElement_tmp;

    DominatorElement initialElement = new DominatorElement(dominatedInitialElement);

    initialElement.update(dominatedInitialElement);

    return initialElement;
  }

  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return null;
  }
}
