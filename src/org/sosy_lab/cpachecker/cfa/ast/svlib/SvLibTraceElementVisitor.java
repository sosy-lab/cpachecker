// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

public interface SvLibTraceElementVisitor<R, X extends Exception> {

  R accept(SvLibTraceSetGlobalVariable pSvLibTraceSetGlobalVariable) throws X;

  R accept(SvLibTraceEntryCall pSvLibTraceEntryCall) throws X;

  R accept(SvLibLocalVariablesStep pSvLibLocalVariablesStep) throws X;

  R accept(SvLibHavocVariablesStep pSvLibHavocVariablesStep) throws X;

  R accept(SvLibChoiceStep pSvLibChoiceStep) throws X;

  R accept(SvLibLeapStep pSvLibLeapStep) throws X;

  R accept(SvLibTrace pSvLibTrace) throws X;

  R accept(SvLibIncorrectTagProperty pSvLibIncorrectTagProperty) throws X;

  R accept(SvLibTraceSetTag pSvLibTraceSetTag) throws X;
}
