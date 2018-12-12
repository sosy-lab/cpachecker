/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.slab;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.SymbolicLocationsUtility;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

public class SLABPredicateWrappingCPA extends AbstractSingleWrapperCPA {

  SLABPredicateTransferRelation transferRelation;
  PredicateAbstractState startState;
  private PredicateCPA predicateCPA;

  protected SLABPredicateWrappingCPA(Specification pSpecification, ConfigurableProgramAnalysis pCpa)
      throws CPAException {
    super(pCpa);
    // this will give a class-cast exception if a config tries to insert s.th. else than a
    // PredicateCPA, so no need for an extra warning:
    predicateCPA = (PredicateCPA) getWrappedCpa();
    transferRelation = new SLABPredicateTransferRelation(predicateCPA, pSpecification);

    // it is not allowed to throw InterruptedException if we want to use AutomaticCPAFactory:
    try {
      startState =
          new SymbolicLocationsUtility(predicateCPA, pSpecification)
              .makePredicateState(true, false);
    } catch (InterruptedException e) {
      throw new CPAException("Building of initial state was interrupted!", e);
    }
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SLABPredicateWrappingCPA.class).withOptions(BlockOperator.class);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return startState;
  }
}
