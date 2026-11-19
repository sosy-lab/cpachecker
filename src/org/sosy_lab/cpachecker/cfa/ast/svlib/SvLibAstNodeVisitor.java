// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagAttributeVisitor;

public interface SvLibAstNodeVisitor<R, X extends Exception>
    extends SvLibCfaEdgeStatementVisitor<R, X>,
        SvLibTagAttributeVisitor<R, X>,
        SvLibExpressionVisitor<R, X> {
  R visit(SvLibVariableDeclaration pSvLibVariableDeclaration) throws X;

  R visit(SvLibParameterDeclaration pSvLibParameterDeclaration) throws X;

  R accept(SvLibFunctionCallExpression pSvLibFunctionCallExpression) throws X;

  R accept(SvLibFunctionDeclaration pSvLibFunctionDeclaration) throws X;

  R accept(SvLibParameterDeclaration pSvLibParameterDeclaration) throws X;

  R accept(SvLibVariableDeclarationTuple pSvLibVariableDeclarationTuple) throws X;
}
