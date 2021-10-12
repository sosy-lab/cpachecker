/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class PredicateAbstractEdge implements AbstractEdge {

  public static class FormulaDescription {
    private final CAssignment assignment;
    private final BooleanFormula formula;

    private final Map<String, CType> types;

    public FormulaDescription(
        CAssignment pAssignment,
        BooleanFormula pFormula,
        Map<String, CType> pInfo) {
      assignment = pAssignment;
      formula = pFormula;
      types = pInfo;
    }

    public BooleanFormula getFormula() {
      return formula;
    }

    public Map<String, CType> getInfo() {
      return types;
    }

    public CAssignment getAssignment() {
      return assignment;
    }

    @Override
    public int hashCode() {
      return Objects.hash(formula);
    }

    @Override
    public boolean equals(Object pOther) {
      if (pOther == this) {
        return true;
      }
      if (pOther instanceof FormulaDescription) {
        boolean res = Objects.equals(formula, ((FormulaDescription) pOther).formula);
        return res;
      } else {
        return false;
      }
    }

    @Override
    public String toString() {
      return assignment.toASTString();
    }
  }

  private final static PredicateAbstractEdge havocEdge =
      new PredicateAbstractEdge(ImmutableList.of());

  private final Collection<FormulaDescription> formula;

  PredicateAbstractEdge(Collection<FormulaDescription> pFormula) {
    formula = pFormula;
  }

  public Collection<FormulaDescription> getFormulas() {
    return formula;
  }

  public static PredicateAbstractEdge getHavocEdgeInstance() {
    return havocEdge;
  }

  @Override
  public String toString() {
    return formula.toString();
  }

}
