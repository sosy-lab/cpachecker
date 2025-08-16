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
        K3ExecutionStatementVisitor<R, X>,
        K3TagAttributeVisitor<R, X>,
        K3TermVisitor<R, X> {
  R visit(K3VariableDeclaration pK3VariableDeclaration);

  R visit(K3ProcedureDeclaration pK3ProcedureDeclaration);

  R visit(K3ParameterDeclaration pK3ParameterDeclaration);

  R accept(K3SymbolApplicationTerm pK3SymbolApplicationTerm);

  R accept(K3IDTerm pK3IDTerm);
}
