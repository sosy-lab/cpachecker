// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concolic;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class StatisticsState implements AbstractState {
  private int win, sel, sim;

  int uct() {
    return 0;
  }

}
