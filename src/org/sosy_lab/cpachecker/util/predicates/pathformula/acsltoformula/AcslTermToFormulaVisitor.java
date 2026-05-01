// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslArraySubscriptTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslAtTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCharLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionCallTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslRealLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslResultTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslStringLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTermVisitor;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

@SuppressWarnings("unused")
public class AcslTermToFormulaVisitor implements AcslTermVisitor<Formula, NoException> {

  private final FormulaManagerView fmgr;
  private final AcslPredicateToFormulaVisitor predicateVisitor;

  public AcslTermToFormulaVisitor(FormulaManagerView pFmgr) {
    checkNotNull(pFmgr);
    this.fmgr = pFmgr;
    this.predicateVisitor = new AcslPredicateToFormulaVisitor(pFmgr);
  }

  @Override
  public Formula visit(AcslUnaryTerm pAcslUnaryTerm) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslStringLiteralTerm pAcslStringLiteralTerm) throws NoException {
    // return ctoFormulaConverter.makeString but this is not a public method?
    return null;
  }

  @Override
  public Formula visit(AcslRealLiteralTerm pAcslRealLiteralTerm) throws NoException {
    return fmgr.getRationalFormulaManager().makeNumber(pAcslRealLiteralTerm.getValue());
  }

  @Override
  public Formula visit(AcslCharLiteralTerm pAcslCharLiteralTerm) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslIntegerLiteralTerm pAcslIntegerLiteralTerm) throws NoException {
    return fmgr.getIntegerFormulaManager().makeNumber(pAcslIntegerLiteralTerm.getValue());
  }

  @Override
  public Formula visit(AcslBooleanLiteralTerm pAcslBooleanLiteralTerm) {
    return fmgr.getBooleanFormulaManager().makeBoolean(pAcslBooleanLiteralTerm.getValue());
  }

  @Override
  public Formula visit(AcslBinaryTerm pAcslBinaryTerm) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslIdTerm pAcslBinaryTerm) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslOldTerm pAcslOldTerm) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslResultTerm pAcslResultTerm) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslAtTerm pAcslAtTerm) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslTernaryTerm pAcslTernaryTerm) throws NoException {
    BooleanFormula conditionFormula = pAcslTernaryTerm.getCondition().accept(predicateVisitor);
    Formula ifTrueFormula = pAcslTernaryTerm.getResultIfTrue().accept(this);
    Formula ifFalseFormula = pAcslTernaryTerm.getResultIfFalse().accept(this);

    if (fmgr.getBooleanFormulaManager().isTrue(conditionFormula)) {
      return ifTrueFormula;
    }
    if (fmgr.getBooleanFormulaManager().isFalse(conditionFormula)) {
      return ifFalseFormula;
    }

    // TODO this seems wrong but how do I make if then else?
    return fmgr.makeOr(
        fmgr.makeAnd(conditionFormula, ifTrueFormula),
        fmgr.makeAnd(fmgr.getBooleanFormulaManager().not(conditionFormula), ifFalseFormula));
  }

  @Override
  public Formula visit(AcslFunctionCallTerm pAcslFunctionCallTerm) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslArraySubscriptTerm pAcslArraySubscriptTerm) throws NoException {
    return null;
  }
}
