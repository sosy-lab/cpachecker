// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

/** Representation of a "return" statement, must including a return value. */
public interface AReturnStatement extends AAstNode {

  /** The return value */
  AExpression getReturnValue();

  /** The variable which the return value is assigned to */
  AAssignment asAssignment();
}
