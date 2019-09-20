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
import static com.google.common.base.Preconditions.checkNotNull;

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
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class DCAState implements AbstractQueryableState, Targetable, Graphable, Serializable,
    AbstractStateWithAssumptions {

  private static final long serialVersionUID = -3454798281550882095L;

  private final AutomatonState buechiState;
  private final ImmutableList<AutomatonState> productStates;

  private final ImmutableList<AutomatonState> compositeStates;
  private ImmutableList<AExpression> predecessorStateBuechiAssumptions;

  public DCAState(
      AutomatonState pBuechiState,
      List<AutomatonState> pCompositeStates,
      ImmutableList<AExpression> pPredecessorBuechiAssumptions) {
    buechiState = checkNotNull(pBuechiState);
    compositeStates = ImmutableList.copyOf(pCompositeStates);
    productStates =
        new ImmutableList.Builder<AutomatonState>().add(buechiState)
            .addAll(compositeStates)
            .build();
    predecessorStateBuechiAssumptions = pPredecessorBuechiAssumptions;
  }

  @Override
  public boolean isTarget() {
    return !productStates.isEmpty() && productStates.stream().allMatch(AutomatonState::isTarget);
  }

  @Override
  public @NonNull Set<Property> getViolatedProperties() throws IllegalStateException {
    checkArgument(isTarget());
    return productStates.stream()
        .flatMap(x -> x.getViolatedProperties().stream())
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public String getCPAName() {
    return "DCAState";
  }

  AutomatonState getBuechiState() {
    return buechiState;
  }

  List<AutomatonState> getCompositeStates() {
    return compositeStates;
  }

  @Override
  public ImmutableList<AExpression> getAssumptions() {
    return productStates.stream()
        .flatMap(x -> x.getAssumptions().stream())
        .distinct()
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((buechiState == null) ? 0 : buechiState.hashCode());
    result = prime * result + ((compositeStates == null) ? 0 : compositeStates.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DCAState)) {
      return false;
    }
    DCAState other = (DCAState) obj;
    if (buechiState == null) {
      if (other.buechiState != null) {
        return false;
      }
    } else if (!buechiState.equals(other.buechiState)) {
      return false;
    }
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
    if (productStates.isEmpty()) {
      return "_empty_state_";
    }

    return Joiner.on("; ").join(Collections2.transform(productStates, AutomatonState::toString));
  }

  @Override
  public String toDOTLabel() {
    if (productStates.isEmpty()) {
      return "_empty_state_";
    }

    return Joiner.on("\n").join(Collections2.transform(productStates, AutomatonState::toDOTLabel));
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    if (predecessorStateBuechiAssumptions.isEmpty()) {
      return true;
    }

    String predecessorStateBuechiExpressions =
        Joiner.on("; ")
            .join(Collections2.transform(buechiState.getAssumptions(), AExpression::toASTString));

    return predecessorStateBuechiExpressions.equals(pProperty);
  }

}
