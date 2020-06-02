// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;


public interface JStatementVisitor<R, X extends Exception> {

  R visit(JExpressionAssignmentStatement pAExpressionAssignmentStatement) throws X;

  R visit(JExpressionStatement pAExpressionStatement) throws X;

  R visit(JMethodInvocationAssignmentStatement pAFunctionCallAssignmentStatement) throws X;

  R visit(JMethodInvocationStatement pAFunctionCallStatement) throws X;
}
