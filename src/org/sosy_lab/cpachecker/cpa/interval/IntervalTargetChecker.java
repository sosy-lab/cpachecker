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
package org.sosy_lab.cpachecker.cpa.interval;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

@Options(prefix="cpa.interval")
public class IntervalTargetChecker {

  @Option(name = "varName",
      description = "Variable name of the variable for which should be checked that its value is globally in a certain bound ")
  private String errorVar = "";
  @Option(description = "Lowest allowed value of the variable")
  private long allowedLow = Long.MIN_VALUE;
  @Option(description = "Highest allowed value of the variable")
  private long allowedHigh = Long.MAX_VALUE;

  private BooleanFormula errorF;

  public IntervalTargetChecker(Configuration config) throws InvalidConfigurationException {
    config.inject(this);

    if (allowedLow > allowedHigh) { throw new InvalidConfigurationException(
        "Not a well formed specification of valid intervals for " + errorVar); }
  }

  public boolean isTarget(IntervalAnalysisState state) {
    Interval interval = state.getInterval(errorVar);

    if (interval == null || interval.isEmpty()) {// variable not defined or state is bottom state
      return false;
    }

    if (interval.getHigh() <= allowedHigh && interval.getLow() >= allowedLow) { return false; }
    return true;
  }

  public BooleanFormula getErrorCondition(IntervalAnalysisState state, FormulaManagerView pFmgr) {
    if (isTarget(state)) {
      if (errorF == null) {
        // build error condition
        Formula f = pFmgr.makeVariable(FormulaType.RationalType, errorVar);
        BooleanFormula bf1 = pFmgr.makeLessThan(f, pFmgr.makeNumber(FormulaType.RationalType, allowedLow), true);
        BooleanFormula bf2 = pFmgr.makeLessThan(pFmgr.makeNumber(FormulaType.RationalType, allowedHigh), f, true);
        errorF = pFmgr.makeOr(bf1, bf2);
      }
      return errorF;
    }
    return pFmgr.getBooleanFormulaManager().makeBoolean(false);
  }

}
