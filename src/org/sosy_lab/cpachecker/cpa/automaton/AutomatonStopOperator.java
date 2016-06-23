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
package org.sosy_lab.cpachecker.cpa.automaton;

import java.util.Collection;

import org.sosy_lab.cpachecker.core.algorithm.mpa.PropertyStats;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.Stats;
import org.sosy_lab.cpachecker.util.statistics.Stats.Contexts;

/**
 * Standard stop-sep operator
 */
public class AutomatonStopOperator implements StopOperator {

  private final AbstractDomain domain;

  /**
   * Creates a stop-sep operator based on the given
   * partial order
   */
  public AutomatonStopOperator(AbstractDomain d) {
    domain = d;
  }

  @Override
  public boolean stop(AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision)
    throws CPAException, InterruptedException {

    AutomatonState state = (AutomatonState) pState;
    Object[] props = state.getOwningAutomaton().getEncodedProperties().toArray(new Property[state.getOwningAutomaton().getEncodedProperties().size()]);

    try (Contexts stat = Stats.beginRootContext(props)) {

      for (AbstractState reachedState : pReached) {
        if (domain.isLessOrEqual(pState, reachedState)) {

          Stats.incCounter("Automaton State Coverage", 1);
          PropertyStats.INSTANCE.signalStopOperatorResult(
              state.getOwningAutomaton().getEncodedProperties(), true);

          return true;
        }
      }

      Stats.incCounter("No Automaton State Coverage", 1);
      PropertyStats.INSTANCE.signalStopOperatorResult(
          state.getOwningAutomaton().getEncodedProperties(), false);

      return false;
    }
  }
}
