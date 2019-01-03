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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProviderImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.SolverException;

public class SymbolicLocationsUtility {
  private CFA cfa;
  private Solver solver;
  private FormulaManagerView fmgr;
  private PredicateAbstractionManager pamgr;
  private PathFormulaManager pfmgr;
  private ImmutableSet<CFANode> targetNodes;

  public SymbolicLocationsUtility(PredicateCPA pPredicateCpa, Specification pSpecification) {
    cfa = pPredicateCpa.getCfa();
    solver = pPredicateCpa.getSolver();
    fmgr = solver.getFormulaManager();
    pamgr = pPredicateCpa.getPredicateManager();
    pfmgr = pPredicateCpa.getPathFormulaManager();
    TargetLocationProvider tlp = new TargetLocationProviderImpl(pPredicateCpa.getShutdownNotifier(), pPredicateCpa.getLogger(), cfa);
    targetNodes = tlp.tryGetAutomatonTargetLocations(cfa.getMainFunction(), pSpecification);
  }

  public PredicateAbstractState makePredicateState(boolean init, boolean error)
      throws InterruptedException {

    return PredicateAbstractState.mkAbstractionState(
        pfmgr.makeEmptyPathFormula(),
        pamgr.asAbstraction(makeStateFormula(init, error), pfmgr.makeEmptyPathFormula()),
        PathCopyingPersistentTreeMap.<CFANode, Integer>of());
  }

  public BooleanFormula makeStateFormula(boolean init, boolean error) {
    BooleanFormula formula = null;
    if (init) {
      formula = makeInit();
    } else {
      formula = fmgr.makeNot(makeInit());
    }
    if (error) {
      formula = fmgr.makeAnd(formula, makeError());
    } else {
      formula = fmgr.makeAnd(formula, solver.getFormulaManager().makeNot(makeError()));
    }
    return formula;
  }

  public BooleanFormula makeInit() {
    return makeProgramCounter(cfa.getMainFunction());
  }

  public BooleanFormula makeError() {
    // TODO: use TargetLocationProvider here!
    BooleanFormula formula = null;
    for (CFANode node : targetNodes) {
      if (formula == null) {
        formula = makeProgramCounter(node);
      } else {
        formula = fmgr.makeOr(formula, makeProgramCounter(node));
      }
    }
    if (formula == null) {
      return fmgr.getBooleanFormulaManager().makeFalse();
    }
    return formula;
  }

  public BooleanFormula makeProgramCounter(CFANode pNode) {
    BitvectorFormula pc = fmgr.makeVariable(FormulaType.getBitvectorTypeWithSize(32), "%pc");
    BitvectorFormula pc0 =
        fmgr.makeNumber(FormulaType.getBitvectorTypeWithSize(32), pNode.getNodeNumber());
    BooleanFormula formula = fmgr.assignment(pc, pc0);
    return formula;
  }

  public boolean isInit(AbstractState pAbsElement) throws SolverException, InterruptedException {
    PredicateAbstractState predicateState =
        checkNotNull(AbstractStates.extractStateByType(pAbsElement, PredicateAbstractState.class));
    return !solver.isUnsat(
        fmgr.makeAnd(makeInit(), predicateState.getAbstractionFormula().asFormula()));
  }

  public boolean isError(AbstractState pAbsElement) throws SolverException, InterruptedException {
    PredicateAbstractState predicateState =
        checkNotNull(AbstractStates.extractStateByType(pAbsElement, PredicateAbstractState.class));
    return !solver.isUnsat(
        fmgr.makeAnd(makeError(), predicateState.getAbstractionFormula().asFormula()));
  }
}
