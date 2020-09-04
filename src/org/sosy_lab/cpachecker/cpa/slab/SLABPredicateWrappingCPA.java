// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.slab;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.specification.Specification;
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
