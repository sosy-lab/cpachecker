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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;


public class BooleanFormulaManagerView extends BaseManagerView implements BooleanFormulaManager {

  private final BooleanFormulaManager manager;
  private final UnsafeFormulaManager unsafe;

  public BooleanFormulaManagerView(FormulaWrappingHandler pWrappingHandler,
      BooleanFormulaManager pManager,
      UnsafeFormulaManager pUnsafe) {
    super(pWrappingHandler);
    this.manager = pManager;
    this.unsafe = pUnsafe;
  }

  public BooleanFormula makeVariable(String pVar, int pI) {
    return makeVariable(FormulaManagerView.makeName(pVar, pI));
  }

  @Override
  public BooleanFormula not(BooleanFormula pBits) {
    return manager.not(pBits);
  }

  @Override
  public BooleanFormula and(BooleanFormula pBits1, BooleanFormula pBits2) {
    return manager.and(pBits1, pBits2);
  }

  @Override
  public BooleanFormula and(List<BooleanFormula> pBits) {
    return manager.and(pBits);
  }

  @Override
  public BooleanFormula or(BooleanFormula pBits1, BooleanFormula pBits2) {
    return manager.or(pBits1, pBits2);
  }

  @Override
  public BooleanFormula or(List<BooleanFormula> pBits) {
    return manager.or(pBits);
  }

  @Override
  public BooleanFormula xor(BooleanFormula pBits1, BooleanFormula pBits2) {
    return manager.xor(pBits1, pBits2);
  }

  @Override
  public boolean isNot(BooleanFormula pBits) {
    return manager.isNot(pBits);
  }

  @Override
  public boolean isAnd(BooleanFormula pBits) {
    return manager.isAnd(pBits);
  }

  @Override
  public boolean isOr(BooleanFormula pBits) {
    return manager.isOr(pBits);
  }

  @Override
  public boolean isXor(BooleanFormula pBits) {
    return manager.isXor(pBits);
  }

  @Override
  public BooleanFormula applyTactic(BooleanFormula input, Tactic tactic) {
    return manager.applyTactic(input, tactic);
  }

  @Override
  public boolean isBoolean(Formula pF) {
    return pF instanceof BooleanFormula;
  }

  @Override
  public BooleanFormula makeBoolean(boolean pValue) {
    return manager.makeBoolean(pValue);
  }

  @Override
  public BooleanFormula makeVariable(String pVar) {
    return manager.makeVariable(pVar);
  }

  @Override
  public boolean isTrue(BooleanFormula pFormula) {
    return manager.isTrue(pFormula);
  }

  @Override
  public boolean isFalse(BooleanFormula pFormula) {
    return manager.isFalse(pFormula);
  }

  @Override
  public <T extends Formula> T ifThenElse(BooleanFormula pCond, T pF1, T pF2) {
    Formula f1 = unwrap(pF1);
    Formula f2 = unwrap(pF2);
    FormulaType<T> targetType = getFormulaType(pF1);

    return wrap(targetType, manager.ifThenElse(pCond, f1, f2));
  }

  @Override
  public <T extends Formula> boolean isIfThenElse(T pF) {
    return manager.isIfThenElse(unwrap(pF));
  }

  public <T extends Formula> Triple<BooleanFormula, T, T> splitIfThenElse(T pF) {
    checkArgument(isIfThenElse(pF));
    Formula f = unwrap(pF);

    assert unsafe.getArity(f) == 3;

    BooleanFormula cond = (BooleanFormula)unsafe.getArg(f, 0);
    FormulaType<Formula> innerType = getFormulaType(f);
    Formula thenBranch = unsafe.typeFormula(innerType, unsafe.getArg(f, 1));
    Formula elseBranch = unsafe.typeFormula(innerType, unsafe.getArg(f, 2));

    FormulaType<T> targetType = getFormulaType(pF);
    return Triple.of(cond, wrap(targetType, thenBranch), wrap(targetType, elseBranch));
  }

  @Override
  public boolean isEquivalence(BooleanFormula pFormula) {
    return manager.isEquivalence(pFormula);
  }

  public BooleanFormula implication(BooleanFormula p, BooleanFormula q) {
    return or(not(p), q);
  }

  @Override
  public boolean isImplication(BooleanFormula pFormula) {
    return manager.isImplication(pFormula);
  }

  @Override
  public BooleanFormula equivalence(BooleanFormula pFormula1, BooleanFormula pFormula2) {
    return manager.equivalence(pFormula1, pFormula2);
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
      unsafe = bfmgr.unsafe;
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

      if (unsafe.isAtom(f)) {
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

    private BooleanFormula getArg(BooleanFormula pF, int i) {
      Formula arg = unsafe.getArg(pF, i);
      assert fmgr.getFormulaType(arg).isBooleanType();
      return (BooleanFormula)arg;
    }

    private BooleanFormula[] getAllArgs(BooleanFormula pF) {
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

  /**
   * Base class for visitors for boolean formulas that traverse recursively
   * through the formula and somehow transform it (i.e., return a boolean formula).
   * This class ensures that each identical subtree of the formula
   * is visited only once to avoid the exponential explosion.
   * When a subclass wants to traverse into a subtree of the formula,
   * it needs to call {@link #visitIfNotSeen(BooleanFormula)} or
   * {@link #visitIfNotSeen(BooleanFormula...)} to ensure this.
   *
   * By default this class implements the identity function.
   *
   * No guarantee on iteration order is made.
   */
  public static abstract class BooleanFormulaTransformationVisitor extends BooleanFormulaManagerView.BooleanFormulaVisitor<BooleanFormula> {

    private final BooleanFormulaManagerView bfmgr;

    private final Map<BooleanFormula, BooleanFormula> cache;

    protected BooleanFormulaTransformationVisitor(FormulaManagerView pFmgr,
        Map<BooleanFormula, BooleanFormula> pCache) {
      super(pFmgr);
      bfmgr = pFmgr.getBooleanFormulaManager();
      cache = pCache;
    }

    protected final BooleanFormula visitIfNotSeen(BooleanFormula f) {
      BooleanFormula out = cache.get(f);
      if (out == null) {
        out = super.visit(f);
        cache.put(f, out);
      }
      return out;
    }

    protected final List<BooleanFormula> visitIfNotSeen(BooleanFormula... pOperands) {
      List<BooleanFormula> args = new ArrayList<>(pOperands.length);
      for (BooleanFormula arg : pOperands) {
        args.add(visitIfNotSeen(arg));
      }
      return args;
    }

    @Override
    protected BooleanFormula visitTrue() {
      return bfmgr.makeBoolean(true);
    }

    @Override
    protected BooleanFormula visitFalse() {
      return bfmgr.makeBoolean(false);
    }

    @Override
    protected BooleanFormula visitAtom(BooleanFormula pAtom) {
      return pAtom;
    }

    @Override
    protected BooleanFormula visitNot(BooleanFormula pOperand) {
      return bfmgr.not(visitIfNotSeen(pOperand));
    }

    @Override
    protected BooleanFormula visitAnd(BooleanFormula... pOperands) {
      return bfmgr.and(visitIfNotSeen(pOperands));
    }

    @Override
    protected BooleanFormula visitOr(BooleanFormula... pOperands) {
      return bfmgr.or(visitIfNotSeen(pOperands));
    }

    @Override
    protected BooleanFormula visitEquivalence(BooleanFormula pOperand1, BooleanFormula pOperand2) {
      return bfmgr.equivalence(visitIfNotSeen(pOperand1), visitIfNotSeen(pOperand2));
    }

    @Override
    protected BooleanFormula visitImplication(BooleanFormula pOperand1, BooleanFormula pOperand2) {
      return bfmgr.implication(visitIfNotSeen(pOperand1), visitIfNotSeen(pOperand2));
    }

    @Override
    protected BooleanFormula visitIfThenElse(BooleanFormula pCondition,
        BooleanFormula pThenFormula, BooleanFormula pElseFormula) {
      return bfmgr.ifThenElse(
          visitIfNotSeen(pCondition),
          visitIfNotSeen(pThenFormula),
          visitIfNotSeen(pElseFormula));
    }
  }
}
