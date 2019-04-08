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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.dca.bfautomaton.BFAutomatonState;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class DCAState
    implements AbstractQueryableState, Targetable, FormulaReportingState, Graphable {

  static final DCAState EMPTY_STATE = new DCAState("_predefined_empty_");

  private final String stateName;
  private final ImmutableSet<BFAutomatonState> compositeStates;
  private final ImmutableSet<DCAProperty> violatedProperties;

  private final ImmutableList<BooleanFormula> assumptions;

  private DCAState(String pStateName) {
    this(pStateName, ImmutableSet.of(), ImmutableSet.of());
  }

  private DCAState(
      String pStateName,
      Set<BFAutomatonState> pCompositeStates,
      Collection<DCAProperty> pProperties) {
    this(pStateName, pCompositeStates, pProperties, ImmutableList.of());
  }

  public DCAState(
      String pStateName,
      Set<BFAutomatonState> pCompositeStates,
      Collection<DCAProperty> pProperties,
      List<BooleanFormula> pAssumptions) {
    stateName = Preconditions.checkNotNull(pStateName);
    violatedProperties = ImmutableSet.copyOf(pProperties);
    compositeStates = ImmutableSet.copyOf(pCompositeStates);
    assumptions = ImmutableList.copyOf(pAssumptions);
  }

  static DCAState
      createInitialState(
          Set<BFAutomatonState> pCompositeStates,
          Collection<DCAProperty> pProperties) {
    return new DCAState("Init", pCompositeStates, pProperties);
  }

  @Override
  public boolean isTarget() {
    return compositeStates.stream().anyMatch(BFAutomatonState::isAcceptingState);
  }

  @Override
  public @NonNull Set<Property> getViolatedProperties() throws IllegalStateException {
    return ImmutableSet.copyOf(violatedProperties);
  }

  @Override
  public String getCPAName() {
    return "DCAState";
  }

  ImmutableSet<BFAutomatonState> getCompositeStates() {
    return compositeStates;
  }

  public ImmutableList<BooleanFormula> getAssumptions() {
    return assumptions;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((assumptions == null) ? 0 : assumptions.hashCode());
    result = prime * result + ((compositeStates == null) ? 0 : compositeStates.hashCode());
    result = prime * result + ((stateName == null) ? 0 : stateName.hashCode());
    result = prime * result + ((violatedProperties == null) ? 0 : violatedProperties.hashCode());
    return result;
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
    if (assumptions == null) {
      if (other.assumptions != null) {
        return false;
      }
    } else if (!assumptions.equals(other.assumptions)) {
      return false;
    }
    if (compositeStates == null) {
      if (other.compositeStates != null) {
        return false;
      }
    } else if (!compositeStates.equals(other.compositeStates)) {
      return false;
    }
    if (stateName == null) {
      if (other.stateName != null) {
        return false;
      }
    } else if (!stateName.equals(other.stateName)) {
      return false;
    }
    if (violatedProperties == null) {
      if (other.violatedProperties != null) {
        return false;
      }
    } else if (!violatedProperties.equals(other.violatedProperties)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return stateName
        + " (States: "
        + Joiner.on("; ").join(Collections2.transform(compositeStates, BFAutomatonState::getName))
        + ")\n:DCA-Asmpts: "
        + Joiner.on("; ").join(assumptions);
  }

  @Override
  public String toDOTLabel() {
    if (compositeStates.isEmpty()) {
      return stateName;
    }

    return stateName + "\n:DCA-Asmpts: " + Joiner.on("; ").join(assumptions);
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView pManager) {
    BooleanFormula bf = pManager.getBooleanFormulaManager().makeTrue();
    assumptions.stream().forEach(x -> pManager.makeAnd(bf, x));
    return bf;
  }

}
