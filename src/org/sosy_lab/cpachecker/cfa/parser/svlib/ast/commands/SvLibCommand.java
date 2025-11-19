// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands;

import java.io.Serializable;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibAstNode;

public sealed interface SvLibCommand extends Serializable, SvLibAstNode
    permits SmtLibDefineFunCommand,
        SmtLibDefineFunRecCommand,
        SmtLibDefineFunsRecCommand,
        SvLibAnnotateTagCommand,
        SvLibAssertCommand,
        SvLibDeclareConstCommand,
        SvLibDeclareFunCommand,
        SvLibDeclareSortCommand,
        SvLibGetWitnessCommand,
        SvLibProcedureDefinitionCommand,
        SvLibSelectTraceCommand,
        SvLibSetInfoCommand,
        SvLibSetLogicCommand,
        SvLibSetOptionCommand,
        SvLibVariableDeclarationCommand,
        SvLibVerifyCallCommand {

  <R, X extends Exception> R accept(SvLibCommandVisitor<R, X> v) throws X;

  @Override
  // By default, all commands are already parenthesized in their AST string representation
  default String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }
}
