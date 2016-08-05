/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public class PredicatePropertyScopeUtil {

  private static final Pattern formulaVariablePattern =
      Pattern.compile("(?:(?<function>.+)::)?(?<variable>.+)@(?<ssaindex>[0-9]+)");

  public static class FormulaVariableResult {

    public final String function;
    public final String variable;
    public final int ssaIndex;
    public FormulaVariableResult(String pFunction, String pVariable, int pSsaIndex) {
      function = pFunction;
      variable = pVariable;
      ssaIndex = pSsaIndex;
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }
      FormulaVariableResult that = (FormulaVariableResult) pO;
      return ssaIndex == that.ssaIndex &&
          Objects.equals(function, that.function) &&
          Objects.equals(variable, that.variable);
    }

    @Override
    public int hashCode() {
      return Objects.hash(function, variable, ssaIndex);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("");
      if(function != null) {
        sb.append(function).append("::");
      }
      sb.append(variable).append("@").append(ssaIndex);

      return sb.toString();


    }

  }

  public static FormulaVariableResult splitFormulaVariable(String variable) {
    Matcher res = formulaVariablePattern.matcher(variable);
    res.matches();
    return new FormulaVariableResult(res.group("function"), res.group("variable"), Integer
        .parseInt(res.group("ssaindex")));

  };
  public static Stream<FormulaVariableResult> formulaVariableSplitStream(
      AbstractState ast, FormulaManagerView fmgr) {
    PredicateAbstractState past = extractStateByType(ast, PredicateAbstractState.class);
    return fmgr.extractVariableNames(past.getAbstractionFormula().asInstantiatedFormula()).stream()
        .map(PredicatePropertyScopeUtil::splitFormulaVariable);

  }

  static Optional<PredicateAbstractState> asNonTrueAbstractionState(AbstractState pState) {
    PredicateAbstractState predState = extractStateByType(pState, PredicateAbstractState.class);
    if (predState != null && predState.isAbstractionState() &&
        !predState.getAbstractionFormula().isTrue()) {
      return Optional.of(predState);
    }

    return Optional.empty();
  }

}
