// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ci.redundancyremover;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.sign.SIGN;
import org.sosy_lab.cpachecker.cpa.sign.SignState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.ci.redundancyremover.RedundantRequirementsRemover.RedundantRequirementsRemoverImplementation;

public class RedundantRequirementsRemoverSignStateImplementation
    extends RedundantRequirementsRemoverImplementation<SignState, SIGN> {

  private static final long serialVersionUID = 7689875020110766102L;

  @Override
  public int compare(SIGN pO1, SIGN pO2) {
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
      throw new NullPointerException(
          "At least one of the arguments " + pO1 + " or " + pO2 + " is null.");
    } else if (pO1.equals(pO2)) {
      return 0;
    } else if (pO2.covers(pO1)) {
      return -1;
    } else if (pO1.covers(pO2)) {
      return 1;
    } else if (pO1 == SIGN.MINUS && (pO2 == SIGN.ZERO || pO2 == SIGN.PLUS || pO2 == SIGN.PLUS0)) {
      return -1;
    } else if (pO1 == SIGN.ZERO && (pO2 == SIGN.PLUS || pO2 == SIGN.PLUSMINUS)) {
      return -1;
    } else if (pO1 == SIGN.PLUSMINUS && pO2 == SIGN.PLUS0) {
      return -1;
    } else if (pO1 == SIGN.MINUS0
        && (pO2 == SIGN.PLUS || pO2 == SIGN.PLUS0 || pO2 == SIGN.PLUSMINUS)) {
      return -1;
    }
    return 1;
  }

  @Override
  protected boolean covers(SIGN pCovering, SIGN pCovered) {
    // return pCovering.covers(pCovered)
    return pCovering.covers(pCovered);
  }

  @Override
  protected SIGN getAbstractValue(SignState pAbstractState, String pVarOrConst) {
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
