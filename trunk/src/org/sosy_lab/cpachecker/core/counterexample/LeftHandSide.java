/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.counterexample;

import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

/**
 * This class is used to represent the left hand side of a C assignment.
 * This is used in the concrete state {@link ConcreteState} to assign values to
 * left hand sides without needing to calculate an address
 * for it. Every left hand side has to be distinct, thats
 * why only variables and field references without pointer
 * references are allowed to be represented by this class.
 *
 *
 */
public abstract class LeftHandSide {

  private final String name;
  private final String functionName;

  public LeftHandSide(String pName, String pFunctionName) {
    name = pName;
    functionName = pFunctionName;
  }

  public LeftHandSide(String pName) {
    name = pName;
    functionName = null;
  }

  /**
   * Returns the name of this left hand side.
   * This is usually the name of a variable.
   * It must be the name of a {@link CIdExpression} idExpression.
   *
   * @return returns the name of the left hand side.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the name of the function, which holds the scope
   * this left hand side is defined in. If the scope of this
   * left hand side is global, this function may not be called.
   *
   * @return Returns the name of the function, which has the scope
   * this left hand side is defined in.
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
  public abstract boolean equals(Object pObj);

  @Override
  public abstract int hashCode();

  @Override
  public String toString() {
    return functionName == null ? name : functionName + "::" + name;
  }
}