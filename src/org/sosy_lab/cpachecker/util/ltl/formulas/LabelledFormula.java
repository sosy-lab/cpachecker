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
 */
package org.sosy_lab.cpachecker.util.ltl.formulas;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.util.Property;

public class LabelledFormula implements Property {

  public static LabelledFormula of(LtlFormula pFormula, List<Literal> pList) {
    return new LabelledFormula(pFormula, pList);
  }

  private final LtlFormula formula;
  private final ImmutableList<Literal> atomicPropositions;

  private LabelledFormula(LtlFormula pFormula, List<Literal> pList) {
    formula = checkNotNull(pFormula);
    atomicPropositions = ImmutableList.copyOf(checkNotNull(pList));
  }

  public LabelledFormula not() {
    return of(formula.not(), atomicPropositions);
  }

  public LtlFormula getFormula() {
    return formula;
  }

  public ImmutableList<Literal> getAPs() {
    return atomicPropositions;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((atomicPropositions == null) ? 0 : atomicPropositions.hashCode());
    result = prime * result + ((formula == null) ? 0 : formula.hashCode());
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
    LabelledFormula other = (LabelledFormula) obj;
    if (atomicPropositions == null) {
      if (other.atomicPropositions != null) {
        return false;
      }
    } else if (!atomicPropositions.equals(other.atomicPropositions)) {
      return false;
    }
    if (formula == null) {
      if (other.formula != null) {
        return false;
      }
    } else if (!formula.equals(other.formula)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return getFormula().toString();
  }
}
