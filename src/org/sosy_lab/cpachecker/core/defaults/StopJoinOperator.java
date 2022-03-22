// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import java.util.Collection;
import java.util.Iterator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Standard stop-join operator that first joins all states of the reached set into a single state,
 * and then checks the partial order relation.
 */
public class StopJoinOperator implements StopOperator {

  private final AbstractDomain domain;

  public StopJoinOperator(AbstractDomain domain) {
    this.domain = domain;
  }

  @Override
  public boolean stop(AbstractState state, Collection<AbstractState> reached, Precision precision)
      throws CPAException, InterruptedException {
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
