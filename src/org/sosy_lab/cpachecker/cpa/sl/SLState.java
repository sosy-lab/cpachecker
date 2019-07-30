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
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.Formula;

public class SLState implements AbstractState, Targetable {

  private final PathFormula pathFormula;
  private final Map<Formula, Formula> heap;
  private final Map<Formula, Formula> stack;

  private final boolean isTarget;

  public SLState(PathFormula pPathFormula) {
    this(pPathFormula, new HashMap<>(), new HashMap<>(), false);
  }

  public SLState(
      PathFormula pPathFormula,
      Map<Formula, Formula> pHeap,
      Map<Formula, Formula> pStack,
      boolean pIsTarget) {
    pathFormula = pPathFormula;
    heap = pHeap;
    stack = pStack;
    isTarget = pIsTarget;
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  @Override
  public String toString() {
    return "Formula:  "
        + pathFormula.toString()
        + "\nStack:   "
        + stack.toString()
        + "\nHeap:     "
        + heap.toString()
        + "\nisTarget: "
        + isTarget;
  }

  public Map<Formula, Formula> getHeap() {
    return heap;
  }

  public Map<Formula, Formula> getStack() {
    return stack;
  }

  public boolean isOnHeap(Formula var) {
    return heap.values().contains(var);
  }

  @Override
  public boolean isTarget() {
    return isTarget;
  }

  @Override
  @Nonnull
  public Set<Property> getViolatedProperties() throws IllegalStateException {
    return ImmutableSet.of(new Property() {
      @Override
      public String toString() {
        return "INVALID DEREF";
      }
    });
  }
}
