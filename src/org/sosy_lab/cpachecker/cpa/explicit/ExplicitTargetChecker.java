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
package org.sosy_lab.cpachecker.cpa.explicit;

import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState.MemoryLocation;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

@Options(prefix="cpa.explicit")
public class ExplicitTargetChecker {

  @Option(
      name = "varName",
      description = "Variable name of the variable for which should be checked that its value is globally a certain value ")
  private String errorVar = "";
  @Option(description = "Value which is allowed for the variable which is checked")
  private long allowedValue = 0;

  private BooleanFormula errorF;
  private MemoryLocation errorVarRep;

  public ExplicitTargetChecker(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
    errorVarRep = MemoryLocation.valueOf(errorVar);
  }

  public boolean isTarget(PersistentMap<MemoryLocation, ExplicitValueBase> pConstantsMap) {
    ExplicitValueBase value = pConstantsMap.get(errorVarRep);
    if (value == null || !value.equals(new ExplicitNumericValue(allowedValue))) { return true; }
    return false;
  }

  public BooleanFormula getErrorCondition(FormulaManagerView pFmgr) {
    if (errorF == null) {
      Formula f = pFmgr.makeVariable(FormulaType.RationalType, errorVar);
      errorF = pFmgr.makeEqual(f, pFmgr.makeNumber(FormulaType.RationalType, allowedValue));
      errorF = pFmgr.makeNot(errorF);
    }
    return errorF;
  }

  public String getTargetVariableName() {
    return errorVar;
  }
}
