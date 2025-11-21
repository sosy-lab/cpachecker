// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibHavocStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibProcedureCallStatement;

public interface SvLibCfaEdgeStatementVisitor<R, X extends Exception> {
  R visit(SvLibAssignmentStatement pSvLibAssignmentStatement) throws X;

  R visit(SvLibProcedureCallStatement pSvLibProcedureCallStatement) throws X;

  R visit(SvLibHavocStatement pSvLibHavocStatement) throws X;
}
