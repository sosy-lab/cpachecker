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
package cpa.dominator;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpa.location.LocationCPA;
import exceptions.CPAException;

public class DominatorCPA implements ConfigurableProgramAnalysis {

	private cpa.dominator.parametric.DominatorCPA parametricDominatorCPA;

	public DominatorCPA(String mergeType, String stopType) throws CPAException {
		this.parametricDominatorCPA = new cpa.dominator.parametric.DominatorCPA(new LocationCPA(mergeType, stopType));
	}

	public AbstractDomain getAbstractDomain() {
		return this.parametricDominatorCPA.getAbstractDomain();
	}

  public TransferRelation getTransferRelation() {
    return this.parametricDominatorCPA.getTransferRelation();
  }

	public MergeOperator getMergeOperator() {
		return this.parametricDominatorCPA.getMergeOperator();
	}

	public StopOperator getStopOperator() {
		return this.parametricDominatorCPA.getStopOperator();
	}

  public PrecisionAdjustment getPrecisionAdjustment() {
    return this.parametricDominatorCPA.getPrecisionAdjustment();
  }

  public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
    return this.parametricDominatorCPA.getInitialElement(node);
  }

  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return this.parametricDominatorCPA.getInitialPrecision(pNode);
  }
}
