/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormulaManager;

import com.google.common.base.Joiner;

public class InvariantsState implements AbstractState, FormulaReportingState {

  private final Map<String, SimpleInterval> vars;

  InvariantsState() {
    vars = new HashMap<>();
  }

  InvariantsState(Map<String, SimpleInterval> pVars) {
    vars = new HashMap<>(pVars);
  }

  public SimpleInterval get(String var) {
    return firstNonNull(vars.get(var), SimpleInterval.infinite());
  }

  public Map<String, SimpleInterval> getIntervals() {
    return Collections.unmodifiableMap(vars);
  }

  InvariantsState copyAndSet(String var, SimpleInterval value) {
    SimpleInterval oldValue = vars.get(var);

    if (value.equals(oldValue)) {
      return this;
    }

    if (!value.hasLowerBound() && !value.hasLowerBound()) {
      // new value is (-INF, INF)

      if (oldValue != null) {
        InvariantsState result = new InvariantsState(vars);
        result.vars.remove(var);
        return result;
      } else {
        return this;
      }
    }

    InvariantsState result = new InvariantsState(vars);
    result.vars.put(checkNotNull(var), checkNotNull(value));
    return result;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManager pManager) {
    BooleanFormulaManager bfmgr = pManager.getBooleanFormulaManager();
    RationalFormulaManager nfmgr = pManager.getRationalFormulaManager();
    BooleanFormula result = bfmgr.makeBoolean(true);

    for (Entry<String, SimpleInterval> entry : vars.entrySet()) {
      RationalFormula var = nfmgr.makeVariable(entry.getKey());
      SimpleInterval value = entry.getValue();

      if (value.hasLowerBound()) {
        RationalFormula bound = nfmgr.makeNumber(value.getLowerBound().longValue());
        BooleanFormula f = nfmgr.greaterOrEquals(var, bound);
        result = bfmgr.and(result, f);
      }
      if (value.hasUpperBound()) {
        RationalFormula bound = nfmgr.makeNumber(value.getUpperBound().longValue());
        BooleanFormula f = nfmgr.lessOrEquals(var, bound);
        result = bfmgr.and(result, f);
      }
    }
    return result;
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    } else if (!(pObj instanceof InvariantsState)) {
      return false;
    }

    return vars.equals(((InvariantsState)pObj).vars);
  }

  @Override
  public int hashCode() {
    return vars.hashCode();
  }

  @Override
  public String toString() {
    return Joiner.on(", ").withKeyValueSeparator("=").join(vars);
  }
}
