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

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.Collection;
import java.util.Iterator;

/**
 * Standard stop-join operator that first joins all states
 * of the reached set into a single state, and then checks the
 * partial order relation.
 */
public class StopJoinOperator implements StopOperator {

 private final AbstractDomain domain;

  public StopJoinOperator(AbstractDomain domain) {
    this.domain = domain;
  }

  @Override
  public boolean stop(AbstractState state, Collection<AbstractState> reached,
                      Precision precision) throws CPAException, InterruptedException {
    if (reached.isEmpty()) {
      return false;
    }
    Iterator<AbstractState> it = reached.iterator();
    AbstractState joinedState = it.next();
    while (it.hasNext()) {
      joinedState = domain.join(it.next(), joinedState);
    }

    return domain.isLessOrEqual(state, joinedState);
  }
}
