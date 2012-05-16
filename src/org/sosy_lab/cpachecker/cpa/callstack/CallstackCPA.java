/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithABM;
import org.sosy_lab.cpachecker.core.interfaces.ProofChecker;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class CallstackCPA extends AbstractCPA implements ConfigurableProgramAnalysisWithABM, ProofChecker {

  private final Reducer reducer = new CallstackReducer();

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(CallstackCPA.class);
  }

  public CallstackCPA() {
    super("sep", "sep", new CallstackTransferRelation());
  }

  @Override
  public Reducer getReducer() {
    return reducer;
  }

  @Override
  public AbstractElement getInitialElement(CFANode pNode) {
    return new CallstackElement(null, pNode.getFunctionName(), pNode);
  }

  @Override
  public boolean areAbstractSuccessors(AbstractElement pElement, CFAEdge pCfaEdge, Collection<? extends AbstractElement> pSuccessors) throws CPATransferException, InterruptedException {
    return pSuccessors.equals(getTransferRelation().getAbstractSuccessors(pElement, null, pCfaEdge));
  }

  @Override
  public boolean isCoveredBy(AbstractElement pElement, AbstractElement pOtherElement) throws CPAException {
    return getAbstractDomain().isLessOrEqual(pElement, pOtherElement);
  }
}