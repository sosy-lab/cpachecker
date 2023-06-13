// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.counterexample;

import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

/**
 * This class is used to represent a identifier of a C primary expression.
 *
 * <p>It is usually used in the concrete state {@link ConcreteState} to assign a value to a variable
 * without needing to calculate an address for it. It is also used to define an address for a
 * variable.
 *
 * <p>TODO: How is it different to {@link CIdExpression}?
 */
public final class IDExpression extends LeftHandSide {

  /**
   * Constructs a IDExpression object with a given identifier and a given scope. The primary
   * expression this idExpresssion represents has to be an lvalue.
   *
   * @param pName the name of the idExpression, which is the identifier of the primary expression in
   *     C.
   * @param pFunctionName the name of the function, which holds the scope this lvalue is defined in.
   */
  public IDExpression(String pName, String pFunctionName) {
    super(pName, pFunctionName);
  }

  /**
   * Constructs a IDExpression object with a given identifier.
   *
   * @param pName the identifier of the primary expression.
   */
  public IDExpression(String pName) {
    super(pName);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    IDExpression other = (IDExpression) obj;

    if (isGlobal()) {
      if (!other.isGlobal()) {
        return false;
      }
    } else if (!getFunctionName().equals(!other.isGlobal() ? other.getFunctionName() : null)) {
      return false;
    }

    if (getName() == null) {
      if (other.getName() != null) {
        return false;
      }
    } else if (!getName().equals(other.getName())) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isGlobal() ? 0 : getFunctionName().hashCode());
    result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
    return result;
  }
}
