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
package cpa.location;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.defaults.MergeSepOperator;
import cpa.common.defaults.StaticPrecisionAdjustment;
import cpa.common.defaults.StopSepOperator;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.CPAFactory;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;

public class LocationCPA implements ConfigurableProgramAnalysis{

	private static final LocationDomain abstractDomain = new LocationDomain();
	private static final TransferRelation transferRelation = new LocationTransferRelation();
	private static final StopOperator stopOperator = new StopSepOperator(abstractDomain.getPartialOrder());

	public static CPAFactory factory() {
	  return new LocationCPAFactory(false);
	}
	
	public LocationCPA() {
  }
	
	public LocationCPA (String mergeType, String stopType) {
	}

	public AbstractDomain getAbstractDomain() {
	  return abstractDomain;
	}

  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

	public MergeOperator getMergeOperator() {
	  return MergeSepOperator.getInstance();
	}

	public StopOperator getStopOperator() {
	  return stopOperator;
	}

  public PrecisionAdjustment getPrecisionAdjustment () {
    return StaticPrecisionAdjustment.getInstance();
  }
  
  @Override
	public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
	  return new LocationElement (node);
	}

  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return null;
  }
}