// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.counterexample;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;

/**
 * Implementations of this interface provide the concrete state with the name of the allocated
 * memory, which stores the value for the given address and expression.
 */
public interface MemoryName {

  /**
   * Returns the allocated memory name that stores the value of the given {@link CExpression} exp
   * with the given {@link Address} address.
   *
   * @param exp The value of this expression is requested.
   * @return The name of the memory that holds the value for the given expression at the given
   *     address.
   */
  String getMemoryName(CRightHandSide exp);
}
