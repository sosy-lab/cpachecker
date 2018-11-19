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
package org.sosy_lab.cpachecker.core.algorithm.mpv.property;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonPrecision;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;

/** The property is represented by one or several specification automata. */
public final class AutomataSingleProperty extends AbstractSingleProperty {

  private final List<Automaton> automata;

  public AutomataSingleProperty(String pName, List<Automaton> pAutomata) {
    super(pName);
    automata = pAutomata;
  }

  @Override
  public void disable(Precision precision) {
    for (AutomatonPrecision automatonPrecision : getAutomatonPrecision(precision)) {
      automatonPrecision.disable();
    }
  }

  @Override
  public void enable(Precision precision) {
    for (AutomatonPrecision automatonPrecision : getAutomatonPrecision(precision)) {
      automatonPrecision.enable();
    }
  }

  /**
   * Get all AutomatonPrecision, which correspond to the given property. Note, that
   * AutomatonPrecision must present in a given precision.
   */
  private Set<AutomatonPrecision> getAutomatonPrecision(Precision precision) {
    ImmutableSet.Builder<AutomatonPrecision> builder = ImmutableSet.builder();
    if (precision instanceof WrapperPrecision) {
      for (Precision wrappedPrecision : ((WrapperPrecision) precision).getWrappedPrecisions()) {
        builder.addAll(getAutomatonPrecision(wrappedPrecision));
      }
    } else if (precision instanceof AutomatonPrecision) {
      AutomatonPrecision automatonPrecision = (AutomatonPrecision) precision;
      if (automata.contains(automatonPrecision.getAutomaton())) {
        builder.add(automatonPrecision);
      }
    }
    return builder.build();
  }

  @Override
  public boolean isTarget(AutomatonState pState) {
    if (automata.contains(pState.getOwningAutomaton())) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void determineRelevancy(CFA cfa) {
    for (Automaton automaton : automata) {
      if (automaton.isRelevantForCFA(cfa)) {
        setRelevant();
        break;
      }
    }
  }
}
