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

import java.util.HashMap;
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
    UNFREED_MEMORY;
  }


  private final PathFormula pathFormula;
  private final Map<Formula, Formula> heap;

  private boolean isTarget;
  private SLStateError error = null;

  public SLState(PathFormula pPathFormula) {
    this(pPathFormula, new HashMap<>(), null);
  }

  public SLState(
      PathFormula pPathFormula,
      Map<Formula, Formula> pHeap,
      SLStateError pError) {
    pathFormula = pPathFormula;
    heap = pHeap;
    error = pError;
    isTarget = error != null;
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  @Override
  public String toString() {
    // String e = error == null ? "Nope." : error.name();
    return "Formula:  "
        + pathFormula.toString()
        + "\nHeap:     "
        + heap.toString()
        + "\nError: "
        + (error != null ? error.name() : "nope.");
  }

  public Map<Formula, Formula> getHeap() {
    return heap;
  }

  public boolean isOnHeap(Formula var) {
    return heap.values().contains(var);
  }

  @Override
  public boolean isTarget() {
    return isTarget;
  }

  public void setTarget(SLStateError pError) {
    error = pError;
    isTarget = pError != null;
  }

  @Override
  @Nonnull
  public Set<Property> getViolatedProperties() throws IllegalStateException {
    return NamedProperty.singleton(error.name());
  }
}
