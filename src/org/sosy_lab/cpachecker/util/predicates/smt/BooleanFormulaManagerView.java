// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collector;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.visitors.TraversalProcess;

public class BooleanFormulaManagerView extends BaseManagerView implements BooleanFormulaManager {

  private final BooleanFormulaManager manager;

  BooleanFormulaManagerView(
      FormulaWrappingHandler pWrappingHandler, BooleanFormulaManager pManager) {
    super(pWrappingHandler);
    manager = checkNotNull(pManager);
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
  public BooleanFormula and(Collection<BooleanFormula> pBits) {
    return manager.and(pBits);
  }

  @Override
  public BooleanFormula and(BooleanFormula... bits) {
    return manager.and(bits);
  }

  @Override
  public Collector<BooleanFormula, ?, BooleanFormula> toConjunction() {
    return manager.toConjunction();
  }

  @Override
  public BooleanFormula or(BooleanFormula pBits1, BooleanFormula pBits2) {
    return manager.or(pBits1, pBits2);
  }

  @Override
  public BooleanFormula or(Collection<BooleanFormula> pBits) {
    return manager.or(pBits);
  }

  @Override
  public BooleanFormula or(BooleanFormula... bits) {
    return manager.or(bits);
  }

  @Override
  public Collector<BooleanFormula, ?, BooleanFormula> toDisjunction() {
    return manager.toDisjunction();
  }

  @Override
  public BooleanFormula xor(BooleanFormula pBits1, BooleanFormula pBits2) {
    return manager.xor(pBits1, pBits2);
  }

  @Override
  public <R> R visit(
      BooleanFormula formula, org.sosy_lab.java_smt.api.visitors.BooleanFormulaVisitor<R> visitor) {
    return manager.visit(formula, visitor);
  }

  @Override
  public void visitRecursively(
      BooleanFormula f,
      org.sosy_lab.java_smt.api.visitors.BooleanFormulaVisitor<TraversalProcess> rFormulaVisitor) {
    manager.visitRecursively(f, rFormulaVisitor);
  }

  @Override
  public BooleanFormula transformRecursively(
      BooleanFormula f,
      org.sosy_lab.java_smt.api.visitors.BooleanFormulaTransformationVisitor pVisitor) {
    return manager.transformRecursively(f, pVisitor);
  }

  @Override
  public Set<BooleanFormula> toConjunctionArgs(BooleanFormula f, boolean flatten) {
    return manager.toConjunctionArgs(f, flatten);
  }

  @Override
  public Set<BooleanFormula> toDisjunctionArgs(BooleanFormula f, boolean flatten) {
    return manager.toDisjunctionArgs(f, flatten);
  }

  @Override
  public BooleanFormula makeBoolean(boolean pValue) {
    return manager.makeBoolean(pValue);
  }

  @Override
  public BooleanFormula makeTrue() {
    return manager.makeTrue();
  }

  @Override
  public BooleanFormula makeFalse() {
    return manager.makeFalse();
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
  public BooleanFormula equivalence(BooleanFormula pFormula1, BooleanFormula pFormula2) {
    return manager.equivalence(pFormula1, pFormula2);
  }

  @Override
  public BooleanFormula implication(BooleanFormula formula1, BooleanFormula formula2) {
    return manager.implication(formula1, formula2);
  }

  /**
   * Base class for visitors for boolean formulas that traverse recursively through the formula and
   * somehow transform it (i.e., return a boolean formula).
   *
   * <p>Should be called with {@link #transformRecursively}
   *
   * <p>This class ensures that each identical subtree of the formula is visited only once to avoid
   * the exponential explosion.
   *
   * <p>By default this class implements the identity function.
   *
   * <p>No guarantee on iteration order is made.
   */
  public abstract static class BooleanFormulaTransformationVisitor
      extends org.sosy_lab.java_smt.api.visitors.BooleanFormulaTransformationVisitor {

    protected BooleanFormulaTransformationVisitor(FormulaManagerView pFmgr) {
      super(pFmgr.getRawFormulaManager());
    }
  }
}
