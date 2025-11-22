// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements;

public interface SvLibStatementVisitor<R, X extends Exception>
    extends SvLibControlFlowStatementVisitor<R, X> {
  R visit(SvLibAssignmentStatement pSvLibAssignmentStatement) throws X;

  R visit(SvLibHavocStatement pSvLibHavocStatement) throws X;

  R visit(SvLibProcedureCallStatement pSvLibProcedureCallStatement) throws X;
}
