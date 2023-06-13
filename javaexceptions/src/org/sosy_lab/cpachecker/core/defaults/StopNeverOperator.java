// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/** Standard stop operator, which always return false */
public class StopNeverOperator implements StopOperator {

  @Override
  public boolean stop(AbstractState el, Collection<AbstractState> reached, Precision precision)
      throws CPAException {
    return false;
  }

  private static final StopOperator instance = new StopNeverOperator();

  public static StopOperator getInstance() {
    return instance;
  }
}
