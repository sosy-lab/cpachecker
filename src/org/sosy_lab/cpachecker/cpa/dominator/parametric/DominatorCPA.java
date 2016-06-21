/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.dominator.parametric;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class DominatorCPA {

  private ConfigurableProgramAnalysis cpa;

  private DominatorDomain abstractDomain;
  private TransferRelation transferRelation;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private PrecisionAdjustment precisionAdjustment;

  public DominatorCPA(ConfigurableProgramAnalysis cpa) {
    this.cpa = cpa;

    this.abstractDomain = new DominatorDomain(this.cpa);
    this.transferRelation = new DominatorTransferRelation(this.cpa);
    this.mergeOperator = new MergeJoinOperator(abstractDomain);
    this.stopOperator = new StopSepOperator(abstractDomain);
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

  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) throws InterruptedException {
    AbstractState dominatedInitialState_tmp = this.cpa.getInitialState(pNode, pPartition);

    AbstractState dominatedInitialState = dominatedInitialState_tmp;

    DominatorState initialState = new DominatorState(dominatedInitialState);

    initialState.update(dominatedInitialState);

    return initialState;
  }

  public Precision getInitialPrecision() {
    return null;
  }
}
