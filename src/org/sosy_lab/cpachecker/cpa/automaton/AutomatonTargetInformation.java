// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.base.Preconditions;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.TargetInformation;

public class AutomatonTargetInformation implements TargetInformation {

  private final @NonNull Automaton automaton;
  private final @NonNull AutomatonTransition automatonTrans;
  private final @NonNull String propertyInstanceDescription;

  public AutomatonTargetInformation(
      Automaton pAutomaton, AutomatonTransition pTransition, String pDesc) {
    automaton = Preconditions.checkNotNull(pAutomaton);
    automatonTrans = Preconditions.checkNotNull(pTransition);
    propertyInstanceDescription = Preconditions.checkNotNull(pDesc);
  }

  public AutomatonTargetInformation(Automaton pAutomaton, AutomatonTransition pTransition) {
    automaton = Preconditions.checkNotNull(pAutomaton);
    automatonTrans = Preconditions.checkNotNull(pTransition);
    propertyInstanceDescription = "";
  }

  public AutomatonTransition getAutomatonTransition() {
    return automatonTrans;
  }

  @Override
  public String toString() {
    return !propertyInstanceDescription.isEmpty()
        ? propertyInstanceDescription
        : automaton.getName();
  }

  @Override
  public int hashCode() {
    return Objects.hash(automaton, automatonTrans, propertyInstanceDescription);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof AutomatonTargetInformation)) {
      return false;
    }
    AutomatonTargetInformation other = (AutomatonTargetInformation) obj;
    return automatonTrans.equals(other.automatonTrans)
        && automaton.equals(other.automaton)
        && propertyInstanceDescription.equals(other.propertyInstanceDescription);
  }
}
