// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands;

public interface SvLibCommandVisitor<R, X extends Exception> {
  R visit(SvLibAnnotateTagCommand pSvLibAnnotateTagCommand) throws X;

  R visit(SvLibAssertCommand pSvLibAssertCommand) throws X;

  R visit(SvLibDeclareConstCommand pSvLibDeclareConstCommand) throws X;

  R visit(SvLibDeclareFunCommand pSvLibDeclareFunCommand) throws X;

  R visit(SvLibDeclareSortCommand pSvLibDeclareSortCommand) throws X;

  R visit(SvLibGetWitnessCommand pSvLibGetWitnessCommand) throws X;

  R visit(SvLibProcedureDefinitionCommand pSvLibProcedureDefinitionCommand) throws X;

  R visit(SvLibSetLogicCommand pSvLibSetLogicCommand) throws X;

  R visit(SvLibSetOptionCommand pSvLibSetOptionCommand) throws X;

  R visit(SvLibVariableDeclarationCommand pSvLibVariableDeclarationCommand) throws X;

  R visit(SvLibVerifyCallCommand pSvLibVerifyCallCommand) throws X;

  R visit(SvLibSelectTraceCommand pSvLibSelectTraceCommand) throws X;

  R visit(SvLibSetInfoCommand pSvLibSetInfoCommand) throws X;

  R accept(SmtLibDefineFunCommand pSmtLibDefineFunCommand) throws X;

  R accept(SmtLibDefineFunRecCommand pSmtLibDefineFunRecCommand) throws X;

  R accept(SmtLibDefineFunsRecCommand pSmtLibDefineFunsRecCommand) throws X;
}
