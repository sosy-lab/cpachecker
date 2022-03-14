// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.alwaystop;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

enum AlwaysTopState implements AbstractState {
  INSTANCE;

  @Override
  public String toString() {
    return "TRUE";
  }
}
