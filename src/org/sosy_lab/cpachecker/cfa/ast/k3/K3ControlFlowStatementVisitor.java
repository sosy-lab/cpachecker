// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

public interface K3ControlFlowStatementVisitor<R, X extends Exception> {
  R visit(K3SequenceStatement pK3SequenceStatement) throws X;

  R visit(K3AssumeStatement pK3AssumeStatement) throws X;

  R visit(K3WhileStatement pK3WhileStatement) throws X;

  R visit(K3IfStatement pK3IfStatement) throws X;

  R visit(K3BreakStatement pK3BreakStatement) throws X;

  R visit(K3ContinueStatement pK3ContinueStatement) throws X;

  R visit(K3ReturnStatement pK3ReturnStatement) throws X;

  R visit(K3GotoStatement pK3GotoStatement) throws X;

  R visit(K3LabelStatement pK3LabelStatement) throws X;

  R visit(K3ChoiceStatement pK3ChoiceStatement) throws X;
}
