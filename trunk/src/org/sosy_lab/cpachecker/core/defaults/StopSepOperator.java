// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/** Standard stop-sep operator */
public class StopSepOperator implements StopOperator {

  private final AbstractDomain domain;

  /** Creates a stop-sep operator based on the given partial order */
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
