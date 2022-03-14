// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.propertychecker;

import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class InIntervalChecker extends PerElementPropertyChecker {

  private final String label;
  private final String varName;
  private final Interval allowedValues;

  public InIntervalChecker(
      final String pVariableName,
      final String pLabel,
      // Necessary because called reflectively
      // TODO Better usability would be to pass Configuration object
      // and define all user-specified parameters as individual @Option fields in this class.
      @SuppressWarnings("unused") final String pMode,
      final String pMin,
      final String pMax) {
    label = pLabel;
    varName = pVariableName;
    allowedValues = new Interval(Long.parseLong(pMin), Long.parseLong(pMax));
  }

  public InIntervalChecker(
      final String pVariableName, final String pLabel, final String pMode, final String pValue) {
    this(
        pVariableName,
        pLabel,
        pMode,
        Integer.parseInt(pMode) == 0 ? pValue : Long.toString(Long.MIN_VALUE),
        Integer.parseInt(pMode) == 0 ? Long.toString(Long.MAX_VALUE) : pValue);
  }

  @Override
  public boolean satisfiesProperty(AbstractState pElemToCheck)
      throws UnsupportedOperationException {
    CFANode node = AbstractStates.extractLocation(pElemToCheck);
    if (node instanceof CFALabelNode && ((CFALabelNode) node).getLabel().equals(label)) {
      IntervalAnalysisState state =
          AbstractStates.extractStateByType(pElemToCheck, IntervalAnalysisState.class);
      if (state != null) {
        Interval interval = state.getInterval(varName);
        if (interval != null
            && interval.getHigh() <= allowedValues.getHigh()
            && interval.getLow() >= allowedValues.getLow()) {
          return true;
        }
      }
      return false;
    }
    return true;
  }
}
