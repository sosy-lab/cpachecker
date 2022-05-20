// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ci.redundancyremover;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.ci.redundancyremover.RedundantRequirementsRemover.RedundantRequirementsRemoverImplementation;


public class RedundantRequirementsRemoverIntervalStateImplementation extends
    RedundantRequirementsRemoverImplementation<IntervalAnalysisState, Interval> {

  private static final long serialVersionUID = 1323131138350817689L;

  @Override
  public int compare(Interval pO1, Interval pO2) {
    // one of arguments null -> NullPointerException
    // 0 if bounds the same for both
    // -1 if both bounds of p01 contained in bounds of p02
    // 1 if both bounds p02 contained in bounds of p01
    // -1 if p01 lower bound smaller than p02 bound
    // -1 if p01 lower bound equal to p02 lower bound and p01 higher bound smaller than p02 higher bound
    // otherwise 1

    if (pO1 == null || pO2 == null) {
      throw new NullPointerException("At least one of the arguments " + pO1 + " or " + pO2 + " is null.");
    } else if (pO1.getLow().equals(pO2.getLow()) && pO1.getHigh().equals(pO2.getHigh())) {
      return 0;
    } else if (pO2.contains(pO1)) {
      return -1;
    } else if (pO1.contains(pO2)) {
      return 1;
    } else if (pO1.getLow().compareTo(pO2.getLow()) < 0) {
      return -1;
    } else if (pO1.getLow().equals(pO2.getLow()) && pO1.getHigh().compareTo(pO2.getHigh()) < 0) {
      return -1;
    }

    return 1;
  }

  @Override
  protected boolean covers(Interval pCovering, Interval pCovered) {
    // return pCovering contains pCovered
    return pCovering.contains(pCovered);
  }

  @Override
  protected Interval getAbstractValue(IntervalAnalysisState pAbstractState, String pVarOrConst) {
    // if pVarOrConst number, return interval [pVarOrConst,pVarOrConst]
    // if state contains pVarOrConst return interval saved in state
    // otherwise unboundedInterval

    try {
      long constant = Long.parseLong(pVarOrConst);
      return new Interval(constant, constant);
    } catch (NumberFormatException e) {
      if (pAbstractState.contains(pVarOrConst)) {
        return pAbstractState.getInterval(pVarOrConst);
      }
    }

    return Interval.UNBOUND;
  }

  @Override
  protected Interval[] emptyArrayOfSize(int pSize) {
    return new Interval[Math.max(0, pSize)];
  }

  @Override
  protected Interval[][] emptyMatrixOfSize(int pSize) {
    return new Interval[Math.max(0, pSize)][];
  }

  @Override
  protected IntervalAnalysisState extractState(AbstractState pWrapperState) {
    // AbstractStates.extractState..
    return AbstractStates.extractStateByType(pWrapperState, IntervalAnalysisState.class); // TODO so?
  }

}
