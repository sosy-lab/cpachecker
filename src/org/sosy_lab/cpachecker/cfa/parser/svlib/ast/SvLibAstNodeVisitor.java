// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast;

import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibCfaEdgeStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSmtFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSortDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTermVisitor;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibCommandVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibControlFlowStatementVisitor;
import org.sosy_lab.cpachecker.core.specification.svlib.ast.SvLibTagAttributeVisitor;
import org.sosy_lab.cpachecker.core.specification.svlib.ast.SvLibTrace;
import org.sosy_lab.cpachecker.core.specification.svlib.ast.SvLibTraceComponentVisitor;

public interface SvLibAstNodeVisitor<R, X extends Exception>
    extends SvLibControlFlowStatementVisitor<R, X>,
        SvLibCfaEdgeStatementVisitor<R, X>,
        SvLibTagAttributeVisitor<R, X>,
        SvLibTermVisitor<R, X>,
        SvLibCommandVisitor<R, X>,
        SvLibTraceComponentVisitor<R, X> {
  R visit(SvLibVariableDeclaration pSvLibVariableDeclaration) throws X;

  R visit(SvLibProcedureDeclaration pSvLibProcedureDeclaration) throws X;

  R visit(SvLibParameterDeclaration pSvLibParameterDeclaration) throws X;

  R visit(SvLibSortDeclaration pSvLibSortDeclaration) throws X;

  R visit(SvLibSmtFunctionDeclaration pSvLibSmtFunctionDeclaration) throws X;

  R accept(SvLibTrace pSvLibTrace) throws X;
}
