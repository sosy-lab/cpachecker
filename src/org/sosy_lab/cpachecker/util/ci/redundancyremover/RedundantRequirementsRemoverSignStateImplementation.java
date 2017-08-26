/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ci.redundancyremover;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.interval.NumberInterface;
import org.sosy_lab.cpachecker.cpa.sign.SIGN;
import org.sosy_lab.cpachecker.cpa.sign.SignState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.ci.redundancyremover.RedundantRequirementsRemover.RedundantRequirementsRemoverImplementation;


public class RedundantRequirementsRemoverSignStateImplementation extends
RedundantRequirementsRemoverImplementation<SignState, NumberInterface>{

  private static final long serialVersionUID = 7689875020110766102L;

  @Override
  public int compare(NumberInterface pO1, NumberInterface pO2) {
      SIGN pO1Temp = (SIGN)pO1;
      SIGN pO2Temp = (SIGN)pO2;
    // one of arguments null -> NullPointerException
    // 0 if sign values identical
    // -1 if p02 covers p01
    // 1 if p01 covers p02
    // -1 if p01=MINUS p02=ZERO,PLUS,PLUS0
    // -1 if p01=ZERO p02=PLUS,PLUSMINUS
    // -1 if p01=PLUSMINUS p02=PLUS0
   // -1 if p01=MINUS0 p02=PLUS,PLUS0,PLUSMINUS
    // otherwise 1

    if (pO1 == null || pO2 == null) {
      throw new NullPointerException("At least one of the arguments " + pO1 + " or " + pO2 + " is null.");
    } else if (pO1Temp.equals(pO2Temp)) {
      return 0;
    } else if (pO2Temp.covers(pO1Temp)) {
      return -1;
    } else if (pO1Temp.covers(pO2Temp)) {
      return 1;
    } else if (pO1Temp == SIGN.MINUS && (pO2Temp == SIGN.ZERO || pO2Temp == SIGN.PLUS || pO2Temp == SIGN.PLUS0)) {
      return -1;
    } else if (pO1Temp == SIGN.ZERO && (pO2Temp == SIGN.PLUS || pO2Temp == SIGN.PLUSMINUS)) {
      return -1;
    } else if (pO1Temp == SIGN.PLUSMINUS && pO2Temp == SIGN.PLUS0) {
      return -1;
    } else if (pO1Temp == SIGN.MINUS0 && (pO2Temp == SIGN.PLUS || pO2Temp == SIGN.PLUS0 || pO2Temp == SIGN.PLUSMINUS)) {
      return -1;
    }
    return 1;
  }

  @Override
  protected boolean covers(NumberInterface pCovering, NumberInterface pCovered) {
    // return pCovering.covers(pCovered)
    return ((SIGN)pCovering).covers((SIGN)pCovered);
  }

  @Override
  protected NumberInterface getAbstractValue(SignState pAbstractState, String pVarOrConst) {
    // if pVarOrConst number, number<0 MINUS, number=0 ZERO, number>0 PLUS
    // otherwise getSignForVariable

    double constant;
    try {
      constant = Double.parseDouble(pVarOrConst);
      if (constant < 0) {
        return SIGN.MINUS;
      } else if (constant == 0) {
        return SIGN.ZERO;
      } else if (constant > 0) {
        return SIGN.PLUS;
      }
    } catch (NumberFormatException e) {
      // pVarOrConst is var and handled in next return
    }

    return pAbstractState.getSignForVariable(pVarOrConst);
  }

  @Override
  protected SIGN[] emptyArrayOfSize(int pSize) {
    // similar to RedundantRequirementsRemoverIntervalStateImplementation
    return new SIGN[pSize];
  }

  @Override
  protected SIGN[][] emptyMatrixOfSize(int pSize) {
    // similar to RedundantRequirementsRemoverIntervalStateImplementation
    return new SIGN[pSize][];
  }

  @Override
  protected SignState extractState(AbstractState pWrapperState) {
    // AbstractStates.extractStateByType....
    return AbstractStates.extractStateByType(pWrapperState, SignState.class); // TODO
  }

}
