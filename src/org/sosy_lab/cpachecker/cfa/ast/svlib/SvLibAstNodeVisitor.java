// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

public interface SvLibAstNodeVisitor<R, X extends Exception>
    extends SvLibControlFlowStatementVisitor<R, X>,
        SvLibCfaEdgeStatementVisitor<R, X>,
        SvLibTagAttributeVisitor<R, X>,
        SvLibTermVisitor<R, X>,
        SvLibCommandVisitor<R, X>,
        SvLibTraceElementVisitor<R, X> {
  R visit(SvLibVariableDeclaration pSvLibVariableDeclaration) throws X;

  R visit(SvLibProcedureDeclaration pSvLibProcedureDeclaration) throws X;

  R visit(SvLibParameterDeclaration pSvLibParameterDeclaration) throws X;

  R visit(SvLibSortDeclaration pSvLibSortDeclaration) throws X;

  R visit(SvLibFunctionDeclaration pSvLibFunctionDeclaration) throws X;
}
