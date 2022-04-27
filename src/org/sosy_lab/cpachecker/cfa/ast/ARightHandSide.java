// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import org.sosy_lab.cpachecker.cfa.types.Type;

/** Interfaces for all possible right-hand sides of an assignment. */
public interface ARightHandSide extends AAstNode {

  /**
   * This method returns the type of the expression. If the expression is evaluated, the result of
   * the evaluation has this type.
   *
   * <p>In some cases the parser can not determine the correct type (because of missing
   * information), then this method can return a ProblemType.
   */
  Type getExpressionType();
}
