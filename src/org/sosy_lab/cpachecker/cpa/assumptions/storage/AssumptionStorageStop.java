// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.assumptions.storage;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;

/** Stop operator for the assumption storage CPA. Stops if the stop flag is true. */
public class AssumptionStorageStop implements StopOperator {

  @Override
  public boolean stop(
      AbstractState pElement, Collection<AbstractState> reached, Precision precision) {
    AssumptionStorageState element = (AssumptionStorageState) pElement;

    if (element.isStop()) {
      // normally we want to keep this element so that the assumption is not lost
      // but we may return true if the new assumption is implied by the old ones

      for (AbstractState ae : reached) {
        AssumptionStorageState reachedState = (AssumptionStorageState) ae;

        // implication check is costly, so we do a fast syntactical approximation
        if (reachedState.isStopFormulaFalse()
            || reachedState.getStopFormula().equals(element.getStopFormula())) {
          return true;
        }
      }
      return false;

    } else {
      // always true, because we never want to prevent the element from being covered
      return true;
    }
  }
}
