// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

public interface AStatementVisitor<R, X extends Exception> {

  R visit(AExpressionAssignmentStatement pAExpressionAssignmentStatement) throws X;

  R visit(AExpressionStatement pAExpressionStatement) throws X;

  R visit(AFunctionCallAssignmentStatement pAFunctionCallAssignmentStatement) throws X;

  R visit(AFunctionCallStatement pAFunctionCallStatement) throws X;
}
