/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.multigoal;

import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class MultiGoalCPA extends AbstractCPA {

  private final MultiGoalPrecisionAdjustment precisionAdjustment;
  private final MutliGoalTransferRelation transferRelation;


  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(MultiGoalCPA.class);
  }

  public MultiGoalCPA() {
    super("sep", "sep", null);

    precisionAdjustment = new MultiGoalPrecisionAdjustment();
    transferRelation = new MutliGoalTransferRelation(null);
  }

  public void setTransferRelationTargets(Set<CFAEdgesGoal> pTargets) {
    transferRelation.setGoals(pTargets);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public AbstractState getInitialState(final CFANode pNode, final StateSpacePartition pPartition)
      throws InterruptedException {
    return MultiGoalState.NonTargetState();
  }

  @Override
  public MultiGoalPrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }
}
