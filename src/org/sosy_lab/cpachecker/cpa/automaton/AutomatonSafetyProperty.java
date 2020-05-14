/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.base.Preconditions;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.core.interfaces.Property;

public class AutomatonSafetyProperty implements Property {

  private final @NonNull Automaton automaton;
  private final @NonNull AutomatonTransition automatonTrans;
  private final @NonNull String propertyInstanceDescription;

  public AutomatonSafetyProperty(Automaton pAutomaton, AutomatonTransition pTransition, String pDesc) {
    this.automaton = Preconditions.checkNotNull(pAutomaton);
    this.automatonTrans = Preconditions.checkNotNull(pTransition);
    this.propertyInstanceDescription = Preconditions.checkNotNull(pDesc);
  }

  public AutomatonSafetyProperty(Automaton pAutomaton, AutomatonTransition pTransition) {
    this.automaton = Preconditions.checkNotNull(pAutomaton);
    this.automatonTrans = Preconditions.checkNotNull(pTransition);
    this.propertyInstanceDescription = "";
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
    if (!(obj instanceof AutomatonSafetyProperty)) {
      return false;
    }
    AutomatonSafetyProperty other = (AutomatonSafetyProperty) obj;
    return automatonTrans.equals(other.automatonTrans)
        && automaton.equals(other.automaton)
        && propertyInstanceDescription.equals(other.propertyInstanceDescription);
  }

}
