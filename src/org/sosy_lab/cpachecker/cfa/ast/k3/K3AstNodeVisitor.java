// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

public interface K3AstNodeVisitor<R, X extends Exception>
    extends K3ControlFlowStatementVisitor<R, X>,
        K3CfaEdgeStatementVisitor<R, X>,
        K3TagAttributeVisitor<R, X>,
        K3TermVisitor<R, X>,
        K3CommandVisitor<R, X> {
  R visit(K3VariableDeclaration pK3VariableDeclaration) throws X;

  R visit(K3ProcedureDeclaration pK3ProcedureDeclaration) throws X;

  R visit(K3ParameterDeclaration pK3ParameterDeclaration) throws X;

  R visit(K3SortDeclaration pK3SortDeclaration) throws X;

  R visit(K3FunctionDeclaration pK3FunctionDeclaration) throws X;
}
