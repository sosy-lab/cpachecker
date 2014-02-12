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
package org.sosy_lab.cpachecker.cfa.blocks;

import com.google.common.base.Objects;

/**
 * Represents a reference to a variable in the CFA.
 */
public class ReferencedVariable {
  private String ident;
  private boolean occursInCondition;
  private boolean occursOnLhs;
  private ReferencedVariable lhsVariable;

  public ReferencedVariable(String pIdent, boolean pOccursInCondition,
      boolean pOccursOnLhs, ReferencedVariable pLhsVariable) {
    super();
    ident = pIdent.replaceAll("[ \n\t]", ""); //mimic behavior of CtoFormulaConverter.exprToVarName
    occursInCondition = pOccursInCondition;
    occursOnLhs = pOccursOnLhs;
    lhsVariable = pLhsVariable;
  }

  public boolean occursInCondition() {
    return occursInCondition;
  }

  public String getName() {
    return ident;
  }

  public boolean occursOnLhs() {
    return occursOnLhs;
  }

  public ReferencedVariable getLhsVariable() {
    return lhsVariable;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ReferencedVariable)) {
      return false;
    }

    ReferencedVariable rhs = (ReferencedVariable)o;
    return ident.equals(rhs.ident) && occursInCondition == rhs.occursInCondition && occursOnLhs == rhs.occursOnLhs && Objects.equal(lhsVariable, rhs.lhsVariable);
  }

  @Override
  public int hashCode() {
    return ident.hashCode() + (occursInCondition?7:0) + (occursOnLhs?42:3) + (lhsVariable==null?0:lhsVariable.hashCode());
  }

  @Override
  public String toString() {
    return ident;
  }
}
