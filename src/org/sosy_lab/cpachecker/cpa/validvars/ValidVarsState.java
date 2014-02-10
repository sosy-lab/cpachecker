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
package org.sosy_lab.cpachecker.cpa.validvars;

import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithTargetVariable;
import org.sosy_lab.cpachecker.core.interfaces.TargetableWithPredicatedAnalysis;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;


public class ValidVarsState extends AbstractSingleWrapperState implements TargetableWithPredicatedAnalysis{

  private static final long serialVersionUID = 9159663474411886276L;
  private final ValidVars validVariables;

  public ValidVarsState(AbstractState pWrappedState, ValidVars pValidVars) {
    super(pWrappedState);
    validVariables = pValidVars;
  }

  public ValidVars getValidVariables(){
    return validVariables;
  }

  @Override
  public boolean isTarget() {
    AbstractState wrappedState = getWrappedState();
    if ((wrappedState instanceof AbstractStateWithTargetVariable)
        && validVariables.containsVar(((AbstractStateWithTargetVariable) wrappedState).getTargetVariableName())) { return super
        .isTarget(); }
    return false;
  }

  @Override
  public BooleanFormula getErrorCondition(FormulaManagerView pFmgr) {
    AbstractState wrappedState = getWrappedState();
    if(wrappedState instanceof TargetableWithPredicatedAnalysis && isTarget()){
      return ((TargetableWithPredicatedAnalysis)wrappedState).getErrorCondition(pFmgr);
    }
    return pFmgr.getBooleanFormulaManager().makeBoolean(false);
  }

}
