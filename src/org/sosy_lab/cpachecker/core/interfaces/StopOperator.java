// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import java.util.Collection;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public interface StopOperator {
  boolean stop(AbstractState state, Collection<AbstractState> reached, Precision precision)
      throws CPAException, InterruptedException;
}
