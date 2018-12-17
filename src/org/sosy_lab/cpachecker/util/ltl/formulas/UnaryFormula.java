/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ltl.formulas;

import static java.util.Objects.requireNonNull;

public abstract class UnaryFormula implements LtlFormula {

  public final LtlFormula operand;

  UnaryFormula(LtlFormula pOperand) {
    this.operand = requireNonNull(pOperand);
  }

  public LtlFormula getOperand() {
    return operand;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((operand == null) ? 0 : operand.hashCode());
    result = prime * result + ((getSymbol() == null) ? 0 : getSymbol().hashCode());
    return result;
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
    UnaryFormula other = (UnaryFormula) obj;
    if (operand == null) {
      if (other.operand != null) {
        return false;
      }
    } else if (!operand.equals(other.operand)) {
      return false;
    }
    if (getSymbol() == null) {
      if (other.getSymbol() != null) {
        return false;
      }
    } else if (!getSymbol().equals(other.getSymbol())) {
      return false;
    }
    return true;
  }

  public abstract String getSymbol();

  @Override
  public String toString() {
    return String.format("%s %s", getSymbol(), operand);
  }
}
