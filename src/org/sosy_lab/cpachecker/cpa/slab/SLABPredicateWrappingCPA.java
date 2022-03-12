// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.slab;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
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
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

public class SLABPredicateWrappingCPA extends AbstractSingleWrapperCPA {

  private final SymbolicLocationsUtility symbolicLocationsUtility;
  private final SLABPredicateTransferRelation transferRelation;

  // To guarantee consistent initial state (it is mutable) TODO necessary?
  private PredicateAbstractState startState;

  protected SLABPredicateWrappingCPA(Specification pSpecification, ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    super(pCpa);
    if (!(pCpa instanceof PredicateCPA)) {
      throw new InvalidConfigurationException(
          "SLABPredicateWrappingCPA requires a PredicateCPA as child");
    }
    symbolicLocationsUtility =
        new SymbolicLocationsUtility((PredicateCPA) getWrappedCpa(), pSpecification);
    transferRelation = new SLABPredicateTransferRelation(symbolicLocationsUtility);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SLABPredicateWrappingCPA.class)
        .withOptions(BlockOperator.class);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    if (startState == null) {
      startState = symbolicLocationsUtility.makePredicateState(true, false);
    }
    return startState;
  }
}
