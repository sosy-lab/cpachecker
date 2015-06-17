/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.interpolation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironmentWithAssumptions;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView.BooleanFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;


public class InterpolatingProverWithAssumptionsWrapper<T> implements InterpolatingProverEnvironmentWithAssumptions<T>{

  private final List<T> solverAssumptionsFromPush;
  private final List<BooleanFormula> solverAssumptionsAsFormula;
  private final InterpolatingProverEnvironment<T> delegate;
  private final FormulaManagerView formulaManagerView;

  public InterpolatingProverWithAssumptionsWrapper(InterpolatingProverEnvironment<T> pDelegate, FormulaManagerView pFmgr) {
    delegate = pDelegate;
    solverAssumptionsFromPush = new ArrayList<>();
    solverAssumptionsAsFormula = new ArrayList<>();
    formulaManagerView = pFmgr;
  }

  @Override
  public T push(BooleanFormula pF) {
    Preconditions.checkState(solverAssumptionsFromPush.isEmpty(),
                             "Pushing is not possible until the assumptions from"
                             + " isUnsatWithAssumptions are cleared");
    return delegate.push(pF);
  }

  @Override
  public BooleanFormula getInterpolant(List<T> pFormulasOfA) throws SolverException {
    List<T> completeListOfA = Lists.newArrayList(pFormulasOfA);
    completeListOfA.addAll(solverAssumptionsFromPush);
    BooleanFormula interpolant = delegate.getInterpolant(completeListOfA);

    // remove assumption variables from the rawInterpolant if necessary
    if (!solverAssumptionsAsFormula.isEmpty()) {
      interpolant = new RemoveAssumptionsFromFormulaVisitor().visit(interpolant);
    }

    return interpolant;
  }

  @Override
  public List<BooleanFormula> getSeqInterpolants(List<Set<T>> pPartitionedFormulas) {
    if (solverAssumptionsAsFormula.isEmpty()) {
      return delegate.getSeqInterpolants(pPartitionedFormulas);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public List<BooleanFormula> getTreeInterpolants(List<Set<T>> pPartitionedFormulas, int[] pStartOfSubTree) {
    if (solverAssumptionsAsFormula.isEmpty()) {
      return delegate.getTreeInterpolants(pPartitionedFormulas, pStartOfSubTree);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public void pop() {
    Preconditions.checkState(solverAssumptionsFromPush.isEmpty(),
                            "Popping is not possible until the assumptions from"
                            + " isUnsatWithAssumptions are cleared");
    delegate.pop();
  }

  @Override
  public boolean isUnsat() throws SolverException, InterruptedException {
    Preconditions.checkState(solverAssumptionsFromPush.isEmpty(),
                            "IsUnsat calls are not possible until the assumptions from"
                            + " isUnsatWithAssumptions are cleared");
    return delegate.isUnsat();
  }

  @Override
  public Model getModel() throws SolverException {
    return delegate.getModel();
  }

  @Override
  public void close() {
    delegate.close();
  }

  @Override
  public boolean isUnsatWithAssumptions(List<BooleanFormula> assumptions) throws SolverException, InterruptedException {
    Preconditions.checkState(solverAssumptionsFromPush.isEmpty(),
                            "IsUnsatWithAssumptions calls are not possible until"
                            + " the assumptions from isUnsatWithAssumptions are cleared");

    solverAssumptionsAsFormula.addAll(assumptions);
    for (BooleanFormula formula : assumptions) {
      solverAssumptionsFromPush.add(delegate.push(formula));
    }
    return delegate.isUnsat();
  }

  public void clearAssumptions() {
    for (int i = 0; i < solverAssumptionsAsFormula.size(); i++) {
      delegate.pop();
    }
    solverAssumptionsAsFormula.clear();
    solverAssumptionsFromPush.clear();
  }

  class RemoveAssumptionsFromFormulaVisitor extends BooleanFormulaVisitor<BooleanFormula> {

    private final Set<BooleanFormula> seen = new HashSet<>();
    private final BooleanFormulaManagerView bmgr;

    private RemoveAssumptionsFromFormulaVisitor() {
      super(formulaManagerView);
      bmgr = formulaManagerView.getBooleanFormulaManager();
    }

    private BooleanFormula visitIfNotSeen(BooleanFormula f) {
      if (seen.add(f)) {
        return visit(f);
      }
      return null;
    }

    @Override
    protected BooleanFormula visitNot(BooleanFormula pOperand) {
      BooleanFormula tmp = visitIfNotSeen(pOperand);
      if (tmp == null) {
        return null;
      }
      return bmgr.not(tmp);
    }

    @Override
    protected BooleanFormula visitAnd(BooleanFormula... pOperands) {
      List<BooleanFormula> necessaryParts = new ArrayList<>();
      for (BooleanFormula operand : pOperands) {
        BooleanFormula tmp = visitIfNotSeen(operand);
        if (tmp != null) {
          necessaryParts.add(tmp);
        }
      }
      return bmgr.and(necessaryParts);
    }

    @Override
    protected BooleanFormula visitOr(BooleanFormula... pOperands) {
      List<BooleanFormula> necessaryParts = new ArrayList<>();
      for (BooleanFormula operand : pOperands) {
        BooleanFormula tmp = visitIfNotSeen(operand);
        if (tmp != null) {
          necessaryParts.add(tmp);
        }
      }
      return bmgr.or(necessaryParts);
    }

    @Override
    protected BooleanFormula visitEquivalence(BooleanFormula pOperand1, BooleanFormula pOperand2) {
      BooleanFormula tmp1 = visitIfNotSeen(pOperand1);
      BooleanFormula tmp2 = visitIfNotSeen(pOperand2);
      return bmgr.equivalence(tmp1, tmp2);
    }

    @Override
    protected BooleanFormula visitImplication(BooleanFormula pOperand1, BooleanFormula pOperand2) {
      BooleanFormula tmp1 = visitIfNotSeen(pOperand1);
      BooleanFormula tmp2 = visitIfNotSeen(pOperand2);
      return bmgr.implication(tmp1, tmp2);
    }

    @Override
    protected BooleanFormula visitIfThenElse(BooleanFormula pCondition, BooleanFormula pThenFormula, BooleanFormula pElseFormula) {
      BooleanFormula cond = visitIfNotSeen(pCondition);
      BooleanFormula ifClause = visitIfNotSeen(pThenFormula);
      BooleanFormula thenClause = visitIfNotSeen(pElseFormula);
      return bmgr.ifThenElse(cond, ifClause, thenClause);
    }

    @Override
    protected BooleanFormula visitTrue() {
      return bmgr.makeBoolean(true);
    }

    @Override
    protected BooleanFormula visitFalse() {
      return bmgr.makeBoolean(false);
    }

    @Override
    protected BooleanFormula visitAtom(BooleanFormula pAtom) {
      if (solverAssumptionsContainEqualVariable(pAtom)) {
        return null;
      }
      return pAtom;
    }

    private boolean solverAssumptionsContainEqualVariable(BooleanFormula variable) {
      Set<String> variableName = formulaManagerView.extractVariableNames(variable);

      // this is no boolean variable atom, but there may be another formula inside
      if (variableName.size() > 1) {
        return false;
      }

      for (BooleanFormula solverVar : solverAssumptionsAsFormula) {
        if (variableName.containsAll(formulaManagerView.extractVariableNames(solverVar))) {
          return true;
        }
      }
      return false;
    }
  }
}

