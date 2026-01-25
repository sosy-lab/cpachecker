// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace;

public interface SvLibTraceComponentVisitor<R, X extends Exception> {

  R accept(SvLibTraceSetGlobalVariable pSvLibTraceSetGlobalVariable) throws X;

  R accept(SvLibTraceEntryProcedure pSvLibTraceEntryProcedure) throws X;

  R accept(SvLibInitProcVariablesStep pSvLibInitProcVariablesStep) throws X;

  R accept(SvLibHavocVariablesStep pSvLibHavocVariablesStep) throws X;

  R accept(SvLibChoiceStep pSvLibChoiceStep) throws X;

  R accept(SvLibLeapStep pSvLibLeapStep) throws X;

  R accept(SvLibIncorrectTagProperty pSvLibIncorrectTagProperty) throws X;

  R accept(SvLibTraceUsingAnnotation pSvLibTraceUsingAnnotation) throws X;

  R accept(SmtLibModel pSmtLibModel) throws X;
}
