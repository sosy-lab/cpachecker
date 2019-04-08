/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.dca;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;

public class DCAState implements AbstractQueryableState, Targetable, Graphable, Serializable,
    AbstractStateWithAssumptions {

  private static final long serialVersionUID = -3454798281550882095L;

  private final ImmutableList<AutomatonState> compositeStates;

  public DCAState(List<AutomatonState> pCompositeStates) {
    compositeStates = ImmutableList.copyOf(pCompositeStates);
    checkArgument(!compositeStates.isEmpty());
  }

  @Override
  public boolean isTarget() {
    return !compositeStates.isEmpty()
        && compositeStates.stream().allMatch(AutomatonState::isTarget);
  }

  @Override
  public @NonNull Set<Property> getViolatedProperties() throws IllegalStateException {
    checkArgument(isTarget());
    return compositeStates.stream()
        .flatMap(x -> x.getViolatedProperties().stream())
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public String getCPAName() {
    return "DCAState";
  }

  ImmutableList<AutomatonState> getCompositeStates() {
    return compositeStates;
  }

  @Override
  public ImmutableList<AExpression> getAssumptions() {
    return compositeStates.stream()
        .flatMap(x -> x.getAssumptions().stream())
        .distinct()
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public int hashCode() {
    return compositeStates.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    DCAState other = (DCAState) obj;
    if (compositeStates == null) {
      if (other.compositeStates != null) {
        return false;
      }
    } else if (!compositeStates.equals(other.compositeStates)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    if (compositeStates.isEmpty()) {
      return "_empty_state_";
    }

    return Joiner.on("; ").join(Collections2.transform(compositeStates, AutomatonState::toString));
  }

  @Override
  public String toDOTLabel() {
    if (compositeStates.isEmpty()) {
      return "_empty_state_";
    }

    return Joiner.on("\n")
        .join(Collections2.transform(compositeStates, AutomatonState::toDOTLabel));
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

}
