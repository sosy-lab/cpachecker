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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.location.LocationCPABackwards;

public class PostDominatorCPA implements ConfigurableProgramAnalysis {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PostDominatorCPA.class);
  }

  private org.sosy_lab.cpachecker.cpa.dominator.parametric.DominatorCPA parametricDominatorCPA;

  public PostDominatorCPA(CFA pCfa, Configuration config) throws InvalidConfigurationException {
    this.parametricDominatorCPA = new org.sosy_lab.cpachecker.cpa.dominator.parametric.DominatorCPA(new LocationCPABackwards(pCfa, config));
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return this.parametricDominatorCPA.getAbstractDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return this.parametricDominatorCPA.getTransferRelation();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return this.parametricDominatorCPA.getMergeOperator();
  }

  @Override
  public StopOperator getStopOperator() {
    return this.parametricDominatorCPA.getStopOperator();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return this.parametricDominatorCPA.getPrecisionAdjustment();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) throws InterruptedException {
    return this.parametricDominatorCPA.getInitialState(pNode, pPartition);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return this.parametricDominatorCPA.getInitialPrecision();
  }
}
