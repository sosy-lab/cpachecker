/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.automaton;

import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class AutomatonPrecision implements Precision {

  private final Automaton automaton;
  private boolean disable;

  public AutomatonPrecision(Automaton pAutomaton) {
    automaton = pAutomaton;
    disable = false;
  }

  public boolean isDisable() {
    return disable;
  }

  public void disable() {
    disable = true;
  }

  public void enable() {
    disable = false;
  }

  @Override
  public String toString() {
    String result = automaton.getName();
    if (disable) {
      result += " (disabled)";
    } else {
      result += " (enabled)";
    }
    return result;
  }

  public Automaton getAutomaton() {
    return automaton;
  }
}
