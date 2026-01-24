// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast;

import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTermVisitor;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagAttributeVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibCommandVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibStatementVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace.SvLibTrace;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace.SvLibTraceComponentVisitor;

public interface SvLibParsingAstNodeVisitor<R, X extends Exception>
    extends SvLibStatementVisitor<R, X>,
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

  R visit(SvLibParsingVariableDeclaration pSvLibParsingVariableDeclaration) throws X;

  R visit(SvLibParsingParameterDeclaration pSvLibParsingParameterDeclaration) throws X;

  R accept(SvLibSortDeclaration pSvLibSortDeclaration) throws X;

  R accept(SvLibScript pSvLibScript) throws X;
}
