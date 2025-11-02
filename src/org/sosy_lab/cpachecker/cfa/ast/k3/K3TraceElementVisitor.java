// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

public interface K3TraceElementVisitor<R, X extends Exception> {

  R accept(K3TraceSetGlobalVariable pK3TraceSetGlobalVariable) throws X;

  R accept(K3TraceEntryCall pK3TraceEntryCall) throws X;

  R accept(K3LocalVariablesStep pK3LocalVariablesStep) throws X;

  R accept(K3HavocVariablesStep pK3HavocVariablesStep) throws X;

  R accept(K3ChoiceStep pK3ChoiceStep) throws X;

  R accept(K3LeapStep pK3LeapStep) throws X;

  R accept(K3Trace pK3Trace) throws X;

  R accept(K3IncorrectTagProperty pK3IncorrectTagProperty) throws X;
}
