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

/** Stop operator, which returns whether an identical abstract state was already found. */
public class StopEqualsOperator implements StopOperator {

  @Override
  public boolean stop(AbstractState state, Collection<AbstractState> reached, Precision precision) {
    return reached.contains(state);
  }

  private static final StopOperator instance = new StopEqualsOperator();

  public static StopOperator getInstance() {
    return instance;
  }
}
