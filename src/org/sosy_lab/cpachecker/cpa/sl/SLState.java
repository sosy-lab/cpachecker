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
package org.sosy_lab.cpachecker.cpa.sl;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.core.defaults.NamedProperty;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.Formula;

public class SLState implements AbstractState, Targetable {

  public enum SLStateError {
    INVALID_DEREF,
    MEMORY_LEAK;
  }


  private final PathFormula pathFormula;
  private final Map<Formula, Formula> heap;
  private final Map<Formula, Formula> stack;

  private final Set<SLStateError> errors = new HashSet<>();

  public SLState(PathFormula pPathFormula) {
    this(pPathFormula, new HashMap<>(), new HashMap<>(), null);
  }

  public SLState(
      PathFormula pPathFormula,
      Map<Formula, Formula> pHeap,
      Map<Formula, Formula> pStack,
      SLStateError pError) {
    pathFormula = pPathFormula;
    heap = pHeap;
    stack = pStack;
    if (pError != null) {
      errors.add(pError);
    }

  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  @Override
  public String toString() {
    return "Errors:   "
        + errors
        + "\nFormula:  "
        + pathFormula
        + "\nHeap:     "
        + heap
        + "\nStack:    "
        + stack;
  }

  public Map<Formula, Formula> getHeap() {
    return heap;
  }

  @Override
  public boolean isTarget() {
    return !errors.isEmpty();
  }

  public void setTarget(SLStateError pError) {
    errors.add(pError);
  }

  @Override
  @Nonnull
  public Set<Property> getViolatedProperties() throws IllegalStateException {
    Set<Property> res = new HashSet<>();
    errors.stream().forEach(e -> res.add(NamedProperty.create(e.name())));
    return ImmutableSet.copyOf(res);
  }
}
