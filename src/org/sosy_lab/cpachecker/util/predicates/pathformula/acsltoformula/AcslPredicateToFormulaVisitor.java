// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula;

import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslExistsPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslForallPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateApplicationPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateVisitor;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslValidPredicate;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.java_smt.api.Formula;

@SuppressWarnings("unused")
public class AcslPredicateToFormulaVisitor implements AcslPredicateVisitor<Formula, NoException> {
  @Override
  public Formula visit(AcslBinaryPredicate pBinaryExpression) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslUnaryPredicate pAcslUnaryPredicate) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslIdPredicate pAcslIdPredicate) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslBinaryTermPredicate pAcslBinaryTermPredicate) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslOldPredicate pAcslOldPredicate) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslBooleanLiteralPredicate pAcslBooleanLiteralPredicate)
      throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslTernaryPredicate pAcslTernaryPredicate) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslValidPredicate pAcslValidPredicate) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslForallPredicate pForallPredicate) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslExistsPredicate pAcslExistsPredicate) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslPredicateApplicationPredicate pAcslPredicateApplicationPredicate)
      throws NoException {
    return null;
  }
}
