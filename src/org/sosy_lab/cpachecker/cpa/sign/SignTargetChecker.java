/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.sign;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

/**
 * To ignore variables which are not declared yet or not valid anymore because the corresponding function is not on the stack,
 * the SignCPA must be wrapped into ValidVariablesCPA
 */
@Options(prefix="cpa.sign")
public class SignTargetChecker {

  @Option(
      name = "varName",
      description = "Variable name of the variable for which should be checked that its value is globally in a certain bound ")
  private String errorVar = "";
  @Option(description = "Abstract value describing all values which are allowed for the variable which is checked")
  private SIGN allowedAbstractValue = SIGN.ALL;

  private BooleanFormula errorF;

  public SignTargetChecker(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  public boolean isTarget(SignState pSignState) {
    SIGN absValue = pSignState.getSignMap().getSignForVariable(errorVar);

    if(allowedAbstractValue.covers(absValue)){
      return false;
    }

    return true;
  }

  public BooleanFormula getErrorCondition(SignState pSignState, FormulaManagerView pFmgr) {
    if (isTarget(pSignState)) {
      if (errorF == null) {
        // build error condition
        Formula f = pFmgr.makeVariable(FormulaType.RationalType, errorVar);

        switch (allowedAbstractValue) {
        case EMPTY: {
          errorF = pFmgr.getBooleanFormulaManager().makeBoolean(true);
          break;
        }
        case PLUS: {
          errorF = pFmgr.makeLessOrEqual(f, pFmgr.makeNumber(FormulaType.RationalType, 0), true);
          break;
        }
        case MINUS: {
          errorF = pFmgr.makeGreaterOrEqual(f, pFmgr.makeNumber(FormulaType.RationalType, 0), true);
          break;
        }
        case ZERO: {
          errorF = pFmgr.makeEqual(f, pFmgr.makeNumber(FormulaType.RationalType, 0));
          errorF = pFmgr.makeNot(errorF);
          break;
        }
        case PLUSMINUS: {
          errorF = pFmgr.makeEqual(f, pFmgr.makeNumber(FormulaType.RationalType, 0));
          break;
        }
        case PLUS0: {
          errorF = pFmgr.makeLessThan(f, pFmgr.makeNumber(FormulaType.RationalType, 0), true);
          break;
        }
        case MINUS0: {
          errorF = pFmgr.makeGreaterThan(f, pFmgr.makeNumber(FormulaType.RationalType, 0), true);
          break;
        }
        default:
          errorF = pFmgr.getBooleanFormulaManager().makeBoolean(false);
        }
      }
      return errorF;
    }
    return pFmgr.getBooleanFormulaManager().makeBoolean(false);
  }

  public String getErrorVariableName(){
    return errorVar;
  }

}
