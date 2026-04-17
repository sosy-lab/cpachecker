// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula;

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
import org.sosy_lab.java_smt.api.Formula;

@SuppressWarnings("unused")
public class AcslTermToFormulaVisitor implements AcslTermVisitor<Formula, NoException> {
  @Override
  public Formula visit(AcslUnaryTerm pAcslUnaryTerm) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslStringLiteralTerm pAcslStringLiteralTerm) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslRealLiteralTerm pAcslRealLiteralTerm) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslCharLiteralTerm pAcslCharLiteralTerm) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslIntegerLiteralTerm pAcslIntegerLiteralTerm) throws NoException {
    return null;
  }

  @Override
  public Formula visit(AcslBooleanLiteralTerm pAcslBooleanLiteralTerm) {
    return null;
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
    return null;
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
