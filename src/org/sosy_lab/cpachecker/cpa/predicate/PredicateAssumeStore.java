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
package org.sosy_lab.cpachecker.cpa.predicate;

import java.io.PrintStream;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;


public class PredicateAssumeStore implements Statistics {

  private final Map<CFANode, BooleanFormula> locationAssumes = Maps.newHashMap();
  private final FormulaManagerView fmv;

  public PredicateAssumeStore(FormulaManagerView pFmv) {
    Preconditions.checkNotNull(pFmv);

    this.fmv = pFmv;
  }

  public synchronized Optional<BooleanFormula> getAssumeOnLocation(final CFANode pLocation) {
    BooleanFormula result = locationAssumes.get(pLocation);

    if (result == null) {
      return Optional.absent();
    }

    return Optional.of(result);
  }

  public synchronized BooleanFormula conjunctAssumeToLocation(final CFANode pLocation, BooleanFormula pAssume) {
    BooleanFormula result = locationAssumes.get(pLocation);
    if (result == null) {
      result = fmv.getBooleanFormulaManager().makeBoolean(true);
    }

    result = fmv.simplify(fmv.makeAnd(result, pAssume));
    locationAssumes.put(pLocation, result);

    return result;
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
  }

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }
}
