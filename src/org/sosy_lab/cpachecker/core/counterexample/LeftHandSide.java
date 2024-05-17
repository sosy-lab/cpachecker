// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.counterexample;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

/**
 * This class is used to represent the left hand side of a C assignment. This is used in the
 * concrete state {@link ConcreteState} to assign values to left hand sides without needing to
 * calculate an address for it. Every left hand side has to be distinct, thats why only variables
 * and field references without pointer references are allowed to be represented by this class.
 */
public abstract sealed class LeftHandSide permits FieldReference, IDExpression {

  private final String name;
  private final String functionName;

  protected LeftHandSide(String pName, String pFunctionName) {
    name = pName;
    functionName = pFunctionName;
  }

  protected LeftHandSide(String pName) {
    name = pName;
    functionName = null;
  }

  /**
   * Returns the name of this left hand side. This is usually the name of a variable. It must be the
   * name of a {@link CIdExpression} idExpression.
   *
   * @return returns the name of the left hand side.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the name of the function, which holds the scope this left hand side is defined in. If
   * the scope of this left hand side is global, this function may not be called.
   *
   * @return Returns the name of the function, which has the scope this left hand side is defined
   *     in.
   */
  public String getFunctionName() {
    assert functionName != null;
    return functionName;
  }

  /**
   * Checks if the left hand side is defined in a global scope.
   *
   * @return Returns true, if the address is defined globally in the program, false otherwise.
   */
  public boolean isGlobal() {
    return functionName == null;
  }

  @Override
  public boolean equals(Object pObj) {
    return pObj instanceof LeftHandSide other
        && Objects.equals(functionName, other.functionName)
        && Objects.equals(name, other.name);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Objects.hashCode(functionName);
    result = prime * result + Objects.hashCode(name);
    return result;
  }

  @Override
  public String toString() {
    return functionName == null ? name : functionName + "::" + name;
  }
}
