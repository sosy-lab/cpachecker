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
package org.sosy_lab.cpachecker.util.predicates.interfaces.view;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;

import com.google.common.base.Function;
import com.google.common.collect.Lists;


public class BooleanFormulaManagerView extends BaseManagerView<BooleanFormula, BooleanFormula> implements BooleanFormulaManager {

  private final BooleanFormulaManager manager;

  public BooleanFormulaManagerView(BooleanFormulaManager pManager) {
    this.manager = pManager;
  }

  public BooleanFormula makeVariable(String pVar, int pI) {
    return makeVariable(FormulaManagerView.makeName(pVar, pI));
  }

  @Override
  public BooleanFormula not(BooleanFormula pBits) {
    return wrapInView(manager.not(extractFromView(pBits)));
  }

  @Override
  public BooleanFormula and(BooleanFormula pBits1, BooleanFormula pBits2) {
    return wrapInView(manager.and(extractFromView(pBits1), extractFromView(pBits2)));
  }

  @Override
  public BooleanFormula and(List<BooleanFormula> pBits) {
    BooleanFormula result = manager.and(Lists.transform(pBits,
        new Function<BooleanFormula, BooleanFormula>() {
          @Override
          public BooleanFormula apply(BooleanFormula pInput) {
            return extractFromView(pInput);
          }
        }));
    return wrapInView(result);
  }

  @Override
  public BooleanFormula or(BooleanFormula pBits1, BooleanFormula pBits2) {
    return wrapInView(manager.or(extractFromView(pBits1), extractFromView(pBits2)));
  }
  @Override
  public BooleanFormula xor(BooleanFormula pBits1, BooleanFormula pBits2) {
    return wrapInView(manager.xor(extractFromView(pBits1), extractFromView(pBits2)));
  }

  @Override
  public boolean isNot(BooleanFormula pBits) {
    return manager.isNot(extractFromView(pBits));
  }

  @Override
  public boolean isAnd(BooleanFormula pBits) {
    return manager.isAnd(extractFromView(pBits));
  }

  @Override
  public boolean isOr(BooleanFormula pBits) {
    return manager.isOr(extractFromView(pBits));
  }

  @Override
  public boolean isXor(BooleanFormula pBits) {
    return manager.isXor(extractFromView(pBits));
  }

  @Override
  public boolean isBoolean(Formula pF) {
    return pF instanceof BooleanFormula;
  }

  @Override
  public FormulaType<BooleanFormula> getFormulaType() {
    return manager.getFormulaType();
  }

  @Override
  public BooleanFormula makeBoolean(boolean pValue) {
    return wrapInView(manager.makeBoolean(pValue));
  }

  @Override
  public BooleanFormula makeVariable(String pVar) {
    return wrapInView(manager.makeVariable(pVar));
  }

  @Override
  public BooleanFormula equivalence(BooleanFormula pFormula1, BooleanFormula pFormula2) {
    return wrapInView(manager.equivalence(extractFromView(pFormula1), extractFromView(pFormula2)));
  }

  @Override
  public boolean isTrue(BooleanFormula pFormula) {
    return manager.isTrue(extractFromView(pFormula));
  }

  @Override
  public boolean isFalse(BooleanFormula pFormula) {
    return manager.isFalse(extractFromView(pFormula));
  }

  @Override
  public <T extends Formula> T ifThenElse(BooleanFormula pCond, T pF1, T pF2) {
    FormulaManagerView viewManager = getViewManager();
    return viewManager.wrapInView(manager.ifThenElse(extractFromView(pCond), viewManager.extractFromView(pF1), viewManager.extractFromView(pF2)));
  }

  @Override
  public <T extends Formula> boolean isIfThenElse(T pF) {
    FormulaManagerView viewManager = getViewManager();
    return manager.isIfThenElse(viewManager.extractFromView(pF));
  }

  public <T extends Formula> Triple<BooleanFormula, T, T> splitIfThenElse(T pF) {
    checkArgument(isIfThenElse(pF));

    FormulaManagerView fmgr = getViewManager();
    UnsafeFormulaManager unsafe = fmgr.getUnsafeFormulaManager();
    assert unsafe.getArity(pF) == 3;

    BooleanFormula cond = wrapInView(unsafe.typeFormula(FormulaType.BooleanType, unsafe.getArg(pF, 0)));
    T thenBranch = fmgr.wrapInView(unsafe.typeFormula(fmgr.getFormulaType(pF), unsafe.getArg(pF, 1)));
    T elseBranch = fmgr.wrapInView(unsafe.typeFormula(fmgr.getFormulaType(pF), unsafe.getArg(pF, 2)));

    return Triple.of(cond, thenBranch, elseBranch);
  }

  @Override
  public boolean isEquivalence(BooleanFormula pFormula) {
    return manager.isEquivalence(extractFromView(pFormula));
  }

  public BooleanFormula implication(BooleanFormula p, BooleanFormula q) {
    return or(not(p), q);
  }

  @Override
  public boolean isImplication(BooleanFormula pFormula) {
    return manager.isImplication(extractFromView(pFormula));
  }


  public BooleanFormula notEquivalence(BooleanFormula p, BooleanFormula q) {
    return not(equivalence(p, q));
  }

  public static abstract class BooleanFormulaVisitor<R> {

    private final FormulaManagerView fmgr;
    private final BooleanFormulaManagerView bfmgr;
    private final UnsafeFormulaManager unsafe;

    protected BooleanFormulaVisitor(FormulaManagerView pFmgr) {
      fmgr = pFmgr;
      bfmgr = fmgr.getBooleanFormulaManager();
      unsafe = fmgr.getUnsafeFormulaManager();
    }

    public final R visit(BooleanFormula f) {
      if (bfmgr.isTrue(f)) {
        assert unsafe.getArity(f) == 0;
        return visitTrue();
      }

      if (bfmgr.isFalse(f)) {
        assert unsafe.getArity(f) == 0;
        return visitFalse();
      }

      if (unsafe.isAtom(fmgr.extractFromView(f))) {
        return visitAtom(f);
      }

      if (bfmgr.isNot(f)) {
        assert unsafe.getArity(f) == 1;
        return visitNot(getArg(f, 0));
      }

      if (bfmgr.isAnd(f)) {
        assert unsafe.getArity(f) >= 2;
        return visitAnd(getAllArgs(f));
      }
      if (bfmgr.isOr(f)) {
        assert unsafe.getArity(f) >= 2;
        return visitOr(getAllArgs(f));
      }

      if (bfmgr.isEquivalence(f)) {
        assert unsafe.getArity(f) == 2;
        return visitEquivalence(getArg(f, 0), getArg(f, 1));
      }

      if (bfmgr.isImplication(f)) {
        assert unsafe.getArity(f) == 2;
        return visitImplication(getArg(f, 0), getArg(f, 1));
      }

      if (bfmgr.isIfThenElse(f)) {
        assert unsafe.getArity(f) == 3;
        return visitIfThenElse(getArg(f, 0), getArg(f, 1), getArg(f, 2));
      }

      throw new UnsupportedOperationException("Unknown boolean operator " + f);
    }

    private final BooleanFormula getArg(BooleanFormula pF, int i) {
      return unsafe.typeFormula(FormulaType.BooleanType, unsafe.getArg(pF, i));
    }

    private final BooleanFormula[] getAllArgs(BooleanFormula pF) {
      int arity = unsafe.getArity(pF);
      BooleanFormula[] args = new BooleanFormula[arity];
      for (int i = 0; i < arity; i++) {
        args[i] = getArg(pF, i);
      }
      return args;
    }

    protected abstract R visitTrue();
    protected abstract R visitFalse();
    protected abstract R visitAtom(BooleanFormula atom);
    protected abstract R visitNot(BooleanFormula operand);
    protected abstract R visitAnd(BooleanFormula... operands);
    protected abstract R visitOr(BooleanFormula... operand);
    protected abstract R visitEquivalence(BooleanFormula operand1, BooleanFormula operand2);
    protected abstract R visitImplication(BooleanFormula operand1, BooleanFormula operand2);
    protected abstract R visitIfThenElse(BooleanFormula condition, BooleanFormula thenFormula, BooleanFormula elseFormula);
  }

  public static abstract class DefaultBooleanFormulaVisitor<R> extends BooleanFormulaVisitor<R> {

    protected DefaultBooleanFormulaVisitor(FormulaManagerView pFmgr) {
      super(pFmgr);
    }

    @Override
    protected R visitTrue() {
      throw new UnsupportedOperationException();
    }

    @Override
    protected R visitFalse() {
      throw new UnsupportedOperationException();
    }

    @Override
    protected R visitAtom(BooleanFormula pAtom) {
      throw new UnsupportedOperationException();
    }

    @Override
    protected R visitNot(BooleanFormula pOperand) {
      throw new UnsupportedOperationException();
    }

    @Override
    protected R visitAnd(BooleanFormula... pOperands) {
      throw new UnsupportedOperationException();
    }

    @Override
    protected R visitOr(BooleanFormula... pOperands) {
      throw new UnsupportedOperationException();
    }

    @Override
    protected R visitEquivalence(BooleanFormula pOperand1, BooleanFormula pOperand2) {
      throw new UnsupportedOperationException();
    }

    @Override
    protected R visitImplication(BooleanFormula pOperand1, BooleanFormula pOperand2) {
      throw new UnsupportedOperationException();
    }

    @Override
    protected R visitIfThenElse(BooleanFormula pCondition, BooleanFormula pThenFormula, BooleanFormula pElseFormula) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Base class for visitors for boolean formulas that traverse recursively
   * through the full formula (at least the boolean part, not inside atoms).
   * This class ensures that each identical subtree of the formula
   * is visited only once to avoid the exponential explosion.
   *
   * Subclasses of this class should call super.visit...() to ensure recursive
   * traversal. If such a call is omitted, the respective part of the formula
   * is not visited.
   *
   * No guarantee on iteration order is made.
   */
  public static abstract class RecursiveBooleanFormulaVisitor extends BooleanFormulaVisitor<Void> {

    private final Set<BooleanFormula> seen = new HashSet<>();

    protected RecursiveBooleanFormulaVisitor(FormulaManagerView pFmgr) {
      super(pFmgr);
    }

    private Void visitIfNotSeen(BooleanFormula f) {
      if (seen.add(f)) {
        return visit(f);
      }
      return null;
    }

    private Void visitMulti(BooleanFormula... pOperands) {
      for (BooleanFormula operand : pOperands) {
        visitIfNotSeen(operand);
      }
      return null;
    }

    @Override
    protected Void visitNot(BooleanFormula pOperand) {
      return visitIfNotSeen(pOperand);
    }

    @Override
    protected Void visitAnd(BooleanFormula... pOperands) {
      return visitMulti(pOperands);
    }

    @Override
    protected Void visitOr(BooleanFormula... pOperands) {
      return visitMulti(pOperands);
    }

    @Override
    protected Void visitEquivalence(BooleanFormula pOperand1, BooleanFormula pOperand2) {
      visitIfNotSeen(pOperand1);
      visitIfNotSeen(pOperand2);
      return null;
    }

    @Override
    protected Void visitImplication(BooleanFormula pOperand1, BooleanFormula pOperand2) {
      visitIfNotSeen(pOperand1);
      visitIfNotSeen(pOperand2);
      return null;
    }

    @Override
    protected Void visitIfThenElse(BooleanFormula pCondition, BooleanFormula pThenFormula, BooleanFormula pElseFormula) {
      visitIfNotSeen(pCondition);
      visitIfNotSeen(pThenFormula);
      visitIfNotSeen(pElseFormula);
      return null;
    }


  }
}
