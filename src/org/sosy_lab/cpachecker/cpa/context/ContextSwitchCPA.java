/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.context;

import java.util.Collection;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.AThreadContainer;
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

public class ContextSwitchCPA extends AbstractCPA implements ConfigurableProgramAnalysisWithBAM, ProofChecker, ReachedSetAdjustingCPA {

  private AThreadContainer initialThreads;

  protected ContextSwitchCPA(Configuration config, LogManager pLogger, CFA cfa) throws InvalidConfigurationException {
    super("sep", "sep", new ContextSwitchTransferRelation(config, pLogger, cfa));
    assert cfa.getThreads().isPresent();
    this.initialThreads = cfa.getThreads().get();
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ContextSwitchCPA.class);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition) {
    return ThreadState.getInitialState(initialThreads);
  }

  @Override
  public boolean adjustPrecision() {
    //TODO implement!!
    return true;
//    ContextSwitchTransferRelation cs = (ContextSwitchTransferRelation) getTransferRelation();
//    cs.contextSwitchBound++;
//    return true;
  }

  @Override
  public void adjustReachedSet(ReachedSet pReachedSet) {
    // No action required
  }

  @Override
  public boolean areAbstractSuccessors(AbstractState state, CFAEdge cfaEdge, Collection<? extends AbstractState> successors)
      throws CPATransferException, InterruptedException {
      //TODO implement
    throw new UnsupportedOperationException("not impelemnted yet");
  }

  @Override
  public boolean isCoveredBy(AbstractState state, AbstractState otherState) throws CPAException, InterruptedException {
    return (getAbstractDomain().isLessOrEqual(state, otherState));
  }

  @Override
  public Reducer getReducer() {
    // TODO implement
//    throw new UnsupportedOperationException("not implemented yet!");
    return null;
  }

}
