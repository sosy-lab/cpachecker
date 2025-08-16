// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureCallStatement;

public interface AStatementVisitor<R, X extends Exception> {

  R visit(AExpressionAssignmentStatement pAExpressionAssignmentStatement) throws X;

  R visit(AExpressionStatement pAExpressionStatement) throws X;

  R visit(AFunctionCallAssignmentStatement pAFunctionCallAssignmentStatement) throws X;

  R visit(AFunctionCallStatement pAFunctionCallStatement) throws X;

  R visit(K3AssignmentStatement pK3AssignmentStatement) throws X;

  R visit(K3ProcedureCallStatement pK3ProcedureCallStatement) throws X;
}
