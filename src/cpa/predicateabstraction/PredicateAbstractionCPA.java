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
package cpa.predicateabstraction;

import cmdline.CPAMain;
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

/**
 * @author erkan
 *
 *  BROKEN
 */
public class PredicateAbstractionCPA implements ConfigurableProgramAnalysis {

  private AbstractDomain      abstractDomain;
  private TransferRelation    transferRelation;
  private MergeOperator       mergeOperator;
  private StopOperator        stopOperator;
  private PrecisionAdjustment precisionAdjustment;

  public PredicateAbstractionCPA(String mergeType, String stopType)
                                                                   throws CPAException {
    PredicateAbstractionDomain predicateAbstractionDomain =
                                                            new PredicateAbstractionDomain();

    this.transferRelation =
                            new PredicateAbstractionTransferRelation(
                                predicateAbstractionDomain);

    MergeOperator predicateAbstractionMergeOp = null;
    if (mergeType.equals("sep")) {
      predicateAbstractionMergeOp =
                                    new PredicateAbstractionMergeSep(
                                        predicateAbstractionDomain);
    } else if (mergeType.equals("join")) {
      predicateAbstractionMergeOp =
                                    new PredicateAbstractionMergeJoin(
                                        predicateAbstractionDomain);
    }

    StopOperator predicateAbstractionStopOp = null;

    if (stopType.equals("sep")) {
      predicateAbstractionStopOp =
                                   new PredicateAbstractionStopSep(
                                       predicateAbstractionDomain);
    } else if (stopType.equals("join")) {
      predicateAbstractionStopOp =
                                   new PredicateAbstractionStopJoin(
                                       predicateAbstractionDomain);
    }

    this.precisionAdjustment = new PredicateAbstractionPrecisionAdjustment();

    this.abstractDomain = predicateAbstractionDomain;
    this.mergeOperator = predicateAbstractionMergeOp;
    this.stopOperator = predicateAbstractionStopOp;
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
    return new PredicateAbstractionElement(CPAMain.cpaConfig
        .getProperty("analysis.entryFunction"));
  }

  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return new PredicateAbstractionPrecision();
  }
}
