// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;

/**
 * Interface that represents the union of a method and a constructor
 * Invocation.
 */
public interface JMethodOrConstructorInvocation extends AFunctionCall, JStatement {

  //TODO Investigate interface and the classes it implements, seems wrong

  @Override
  JMethodInvocationExpression getFunctionCallExpression();
}
