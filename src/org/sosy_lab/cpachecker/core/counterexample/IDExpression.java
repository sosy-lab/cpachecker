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


/**
 * This class is used to represent a identifier of a C
 * primary expression.
 *
 * It is usually used in the concrete state {@link ConcreteState}
 * to assign a value to a variable without needing to
 * calculate an address for it. It is also used to
 * define an address for a variable.
 *
 *
 */
public final class IDExpression extends LeftHandSide {

  /**
   * Constructs a IDExpression object with a given identifier
   * and a given scope. The primary expression this idExpresssion represents
   * has to be an lvalue.
   *
   * @param pName the name of the idExpression, which is the identifier of
   *        the primary expression in C.
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
    result = prime * result + ((isGlobal()) ? 0 : getFunctionName().hashCode());
    result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
    return result;
  }
}
