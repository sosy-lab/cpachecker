// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

public interface K3ControlFlowStatementVisitor<R, X extends Exception> {
  R accept(K3SequenceStatement pK3SequenceStatement);

  R visit(K3AssumeStatement pK3AssumeStatement);
}
