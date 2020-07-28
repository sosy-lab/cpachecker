// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class AutomatonPrecision implements Precision {

  private final Automaton automaton;
  private boolean enabled;

  public AutomatonPrecision(Automaton pAutomaton) {
    automaton = pAutomaton;
    enabled = true;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void disable() {
    enabled = false;
  }

  public void enable() {
    enabled = true;
  }

  @Override
  public String toString() {
    String result = automaton.getName();
    if (enabled) {
      result += " (enabled)";
    } else {
      result += " (disabled)";
    }
    return result;
  }

  public Automaton getAutomaton() {
    return automaton;
  }
}
