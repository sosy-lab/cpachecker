// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

public interface CStatementVisitor<R, X extends Exception> {

  R visit(CExpressionStatement pIastExpressionStatement) throws X;

  /**
   * The left-hand side of an assignment statement might be a variable: v = ...; pointer: *v = ...;
   * array element: v[...] = ...; field reference: ...->v = ...;
   */
  R visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement) throws X;

  R visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement) throws X;

  R visit(CFunctionCallStatement pIastFunctionCallStatement) throws X;
}
