// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements;

public interface SvLibControlFlowStatementVisitor<R, X extends Exception> {
  R visit(SvLibSequenceStatement pSvLibSequenceStatement) throws X;

  R visit(SvLibAssumeStatement pSvLibAssumeStatement) throws X;

  R visit(SvLibWhileStatement pSvLibWhileStatement) throws X;

  R visit(SvLibIfStatement pSvLibIfStatement) throws X;

  R visit(SvLibBreakStatement pSvLibBreakStatement) throws X;

  R visit(SvLibContinueStatement pSvLibContinueStatement) throws X;

  R visit(SvLibReturnStatement pSvLibReturnStatement) throws X;

  R visit(SvLibGotoStatement pSvLibGotoStatement) throws X;

  R visit(SvLibLabelStatement pSvLibLabelStatement) throws X;

  R visit(SvLibChoiceStatement pSvLibChoiceStatement) throws X;
}
