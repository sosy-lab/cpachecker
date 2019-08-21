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
package org.sosy_lab.cpachecker.cpa.assumptions.storage;

import java.util.Collection;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;

/**
 * Stop operator for the assumption storage CPA. Stops if the stop flag is
 * true.
 */
public class AssumptionStorageStop implements StopOperator {

  @Override
  public boolean stop(AbstractState pElement, Collection<AbstractState> reached, Precision precision) {
    AssumptionStorageState element = (AssumptionStorageState) pElement;

    if (element.isStop()) {
      // normally we want to keep this element so that the assumption is not lost
      // but we may return true if the new assumption is implied by the old ones

      for (AbstractState ae : reached) {
        AssumptionStorageState reachedState = (AssumptionStorageState)ae;

        // implication check is costly, so we do a fast syntactical approximation
        if (   reachedState.isStopFormulaFalse()
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