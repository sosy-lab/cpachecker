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
package cpa.common.defaults;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;

/**
 * This is an abstract class for building CPAs. It uses the flat lattice domain
 * if no other domain is given, and the standard implementations for merge-(sep|join)
 * and stop-sep.
 */
public abstract class AbstractCPA implements ConfigurableProgramAnalysis {

  private final AbstractDomain abstractDomain;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final TransferRelation transferRelation;
  
  protected AbstractCPA(String mergeType, String stopType, TransferRelation transfer) {
    this(mergeType, stopType, new FlatLatticeDomain(), transfer);
  }
  
  protected AbstractCPA(String mergeType, String stopType, AbstractDomain domain, TransferRelation transfer) {
    this.abstractDomain = domain;
    
    if (mergeType.equals("join")) {
      mergeOperator = new MergeJoinOperator(abstractDomain.getJoinOperator());
    } else {
      assert mergeType.equals("sep");
      mergeOperator = MergeSepOperator.getInstance();
    }
    
    assert stopType.equals("sep");
    stopOperator = new StopSepOperator(abstractDomain.getPartialOrder());
    
    this.transferRelation = transfer;
  }
  
  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
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
    return StaticPrecisionAdjustment.getInstance();
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
