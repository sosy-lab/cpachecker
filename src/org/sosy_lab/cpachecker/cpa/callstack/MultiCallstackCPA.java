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
package org.sosy_lab.cpachecker.cpa.callstack;

import java.util.Collection;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

// TODO diff this class with the original CallstackCPA to and merge them to provide both functionalities. The Real different functionalities might be in the states and transferrelation
@Options
public class MultiCallstackCPA extends AbstractCPA implements ConfigurableProgramAnalysisWithBAM, ProofChecker,
    ReachedSetAdjustingCPA {

  @Option(secure=false, description = "TODO")
  private boolean useMultithreadedCallstack = false;   // TODO

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(MultiCallstackCPA.class);
  }

  public MultiCallstackCPA(Configuration config, LogManager pLogger)
      throws InvalidConfigurationException {
    super("sep", "sep", new MultiCallstackTransferRelation(config, pLogger));
    config.inject(this);
  }

  @Override
  public Reducer getReducer() {
    return null;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return MultiCallstackState.initialState(pNode.getFunctionName(), pNode);
  }

  @Override
  public boolean areAbstractSuccessors(AbstractState pElement, CFAEdge pCfaEdge, Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    throw new UnsupportedOperationException("not impelemnted yet");
  }

  @Override
  public boolean isCoveredBy(AbstractState pElement, AbstractState pOtherElement) throws CPAException, InterruptedException {
    boolean is = getAbstractDomain().isLessOrEqual(pElement, pOtherElement);

    return getAbstractDomain().isLessOrEqual(pElement, pOtherElement);
  }

  @Override
  public boolean adjustPrecision() {
    throw new UnsupportedOperationException("not impelemnted yet");
    // TODO implement
//    CallstackTransferRelation ctr = (CallstackTransferRelation) getTransferRelation();
//    ++ctr.recursionBoundDepth;
//    return true;
  }

  @Override
  public void adjustReachedSet(ReachedSet pReachedSet) {
    // No action required
  }

}
