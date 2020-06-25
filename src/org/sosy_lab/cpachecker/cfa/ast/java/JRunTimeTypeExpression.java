// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

/**
 * This interface represents expressions, that can be part of {@link JRunTimeTypeEqualsType}.
 * These expression evaluate to the run time type of the expression if they are part of
 * {@link JRunTimeTypeEqualsType}.
 *
 *
 */
public interface JRunTimeTypeExpression extends JExpression {

  boolean isThisReference();
  boolean isVariableReference();

}
