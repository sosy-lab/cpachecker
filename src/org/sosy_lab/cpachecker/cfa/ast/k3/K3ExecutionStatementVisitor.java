// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

public interface K3ExecutionStatementVisitor<R, X extends Exception> {
  R visit(K3AssignmentStatement pK3AssignmentStatement) throws X;

  R visit(K3ProcedureCallStatement pK3ProcedureCallStatement) throws X;
}
