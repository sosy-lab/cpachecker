// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagAttributeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTrace;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTraceComponentVisitor;

public interface SvLibAstNodeVisitor<R, X extends Exception>
    extends SvLibCfaEdgeStatementVisitor<R, X>,
        SvLibTagAttributeVisitor<R, X>,
        SvLibTermVisitor<R, X>,
        SvLibTraceComponentVisitor<R, X> {
  R visit(SvLibVariableDeclaration pSvLibVariableDeclaration) throws X;

  R visit(SvLibProcedureDeclaration pSvLibProcedureDeclaration) throws X;

  R visit(SvLibParameterDeclaration pSvLibParameterDeclaration) throws X;

  R accept(SvLibTrace pSvLibTrace) throws X;
}
