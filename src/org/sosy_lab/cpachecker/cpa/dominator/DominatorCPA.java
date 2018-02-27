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
package org.sosy_lab.cpachecker.cpa.dominator;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

/**
 * CPA that computes the dominators of CFA nodes. A CFA node `d` is a dominator of a CFA node `l` in
 * a CFA if it is part of all paths from the initial location to `l`.
 *
 * <p>This CPA can also be used for post-dominator computation, i.e., to compute all nodes that are
 * part of all paths from a given node to the program exit. To do so, run the CPA with {@link
 * org.sosy_lab.cpachecker.cpa.location.LocationCPABackwards LocationCPABackwards}.
 *
 * <p>Note: If run with {@link org.sosy_lab.cpachecker.cpa.location.LocationCPABackwards
 * LocationCPABackwards}, each node will be post-dominated by itself. This is not a problem and not
 * wrong, just don't be confused.
 */
public class DominatorCPA implements ConfigurableProgramAnalysis {

  private DominatorDomain abstractDomain;
  private DominatorState initialState;
  private TransferRelation transferRelation;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private PrecisionAdjustment precisionAdjustment;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(DominatorCPA.class);
  }

  public DominatorCPA() {
    abstractDomain = new DominatorDomain();
    initialState = new DominatorState();
    transferRelation = new DominatorTransferRelation();
    mergeOperator = new MergeJoinOperator(abstractDomain);
    stopOperator = new StopJoinOperator(abstractDomain);
    precisionAdjustment = StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) throws InterruptedException {
    return initialState;
  }
}
