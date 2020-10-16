// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

public interface JAstNodeVisitor<R, X extends Exception>
    extends JRightHandSideVisitor<R, X>, JStatementVisitor<R, X> {

  R visit(JInitializerExpression pJInitializerExpression) throws X;

  R visit(JMethodDeclaration pJMethodDeclaration) throws X;

  R visit(JParameterDeclaration pJParameterDeclaration) throws X;

  R visit(JReturnStatement pJReturnStatement) throws X;

  R visit(JVariableDeclaration pJVariableDeclaration) throws X;
}
