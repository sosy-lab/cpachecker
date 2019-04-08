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

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.dca.bfautomaton.BFAutomatonState;

public class DCAState implements AbstractQueryableState, Targetable {

  private final ImmutableSet<BFAutomatonState> states;
  private final ImmutableSet<Property> violatedProperties;

  public DCAState(Set<BFAutomatonState> pStates, Collection<DCAProperty> pProperties) {
    violatedProperties = ImmutableSet.copyOf(pProperties);
    states = ImmutableSet.copyOf(pStates);
  }

  @Override
  public boolean isTarget() {
    return states.stream().anyMatch(BFAutomatonState::isAcceptingState);
  }

  @Override
  public @NonNull Set<Property> getViolatedProperties() throws IllegalStateException {
    return violatedProperties;
  }

  @Override
  public String getCPAName() {
    return "DCAAnalysis";
  }

}
