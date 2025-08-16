// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

public interface K3TermVisitor<R, X extends Exception> {

  R accept(K3OldTerm pK3OldTerm) throws X;

  R accept(K3SymbolApplicationTerm pK3SymbolApplicationTerm) throws X;

  R accept(K3IDTerm pK3IDTerm) throws X;
}
