// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public interface AcslTermVisitor<R, X extends Exception> {
  R visit(AcslUnaryTerm pAcslUnaryTerm) throws X;

  R visit(AcslStringLiteralTerm pAcslStringLiteralTerm) throws X;

  R visit(AcslRealLiteralTerm pAcslRealLiteralTerm) throws X;

  R visit(AcslCharLiteralTerm pAcslCharLiteralTerm) throws X;

  R visit(AcslIntegerLiteralTerm pAcslIntegerLiteralTerm) throws X;

  R visit(AcslBooleanLiteralTerm pAcslBooleanLiteralTerm);

  R visit(AcslBinaryTerm pAcslBinaryTerm) throws X;

  R visit(AcslIdTerm pAcslBinaryTerm) throws X;

  R visit(AcslOldTerm pAcslOldTerm) throws X;

  R visit(AcslResultTerm pAcslResultTerm) throws X;
}
