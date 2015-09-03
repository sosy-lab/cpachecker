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
package org.sosy_lab.cpachecker.core.defaults;

import java.util.Collection;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Standard stop-sep operator
 */
public class StopSepOperator implements StopOperator {

  private final AbstractDomain domain;

  /**
   * Creates a stop-sep operator based on the given
   * partial order
   */
  public StopSepOperator(AbstractDomain d) {
    domain = d;
  }

  @Override
  public boolean stop(AbstractState el, Collection<AbstractState> reached, Precision precision)
    throws CPAException, InterruptedException {

    for (AbstractState reachedState : reached) {
      if (domain.isLessOrEqual(el, reachedState)) {
        return true;
      }
    }
    return false;
  }
}
