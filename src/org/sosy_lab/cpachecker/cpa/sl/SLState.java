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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class SLState implements AbstractState, AbstractQueryableState {

  private static final String HAS_INVALID_READS = "has-invalid-reads";
  private static final String HAS_INVALID_WRITES = "has-invalid-writes";
  private static final String HAS_LEAKS = "has-leaks";
  private static final String HAS_INVALID_FREES = "has-invalid-frees";

  public enum SLStateError {
    INVALID_READ,
    INVALID_WRITE,
    INVALID_FREE,
    MEMORY_LEAK,
    NONE;
  }


  private PathFormula pathFormula;
  private final Map<Formula, Formula> heap;
  private final Map<Formula, Formula> stack;
  private final List<Formula> heapKeys; // Insertion order
  private final List<Formula> stackKeys; // Insertion order

  private final Set<BooleanFormula> constraints;

  private final Map<Formula, BigInteger> allocationSizes;
  private final Map<String, Set<Formula>> allocas;

  private final Set<SLStateError> errors = new HashSet<>();


  public SLState(
      PathFormula pPathFormula,
      Map<Formula, Formula> pHeap,
      Map<Formula, Formula> pStack,
      List<Formula> pHeapKeys,
      List<Formula> pStackKeys,
      Set<BooleanFormula> pConstraints,
      Map<Formula, BigInteger> pAllocationSizes,
      Map<String, Set<Formula>> pAllocas,
      SLStateError pError) {
    pathFormula = pPathFormula;
    heap = pHeap;
    stack = pStack;
    heapKeys = pHeapKeys;
    stackKeys = pStackKeys;
    constraints = pConstraints;
    allocationSizes = pAllocationSizes;
    allocas = pAllocas;
    if (pError != null) {
      errors.add(pError);
    }

  }

  public SLState(PathFormula pStore) {
    this(
        pStore,
        new HashMap<>(),
        new HashMap<>(),
        new ArrayList<>(),
        new ArrayList<>(),
        new HashSet<>(),
        new HashMap<>(),
        new HashMap<>(),
        null);
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  @Override
  public String toString() {
    return "Errors:   "
        + errors;
    // + "\nFormula: "
    // + pathFormula
    // + "\nHeap: "
    // + heap
    // + "\nStack: "
    // + stack;
  }

  public Map<Formula, Formula> getHeap() {
    return heap;
  }

  public Map<Formula, Formula> getStack() {
    return stack;
  }

  public void putOn(boolean onHeap, Formula pKey, Formula pVal) {
    Map<Formula, Formula> memory = onHeap ? heap : stack;
    List<Formula> keys = onHeap ? heapKeys : stackKeys;
    if (!memory.containsKey(pKey)) {
      keys.add(pKey);
    }
    memory.put(pKey, pVal);
  }

  public Formula removeFrom(boolean fromHeap, Formula pKey) {
    Map<Formula, Formula> memory = fromHeap ? heap : stack;
    List<Formula> keys = fromHeap ? heapKeys : stackKeys;
    keys.remove(pKey);
    return memory.remove(pKey);
  }

  public Formula[] getSegment(boolean fromHeap, Formula pKey, int size) {
    Formula[] res = new Formula[size];
    List<Formula> keys = fromHeap ? heapKeys : stackKeys;
    int index = keys.indexOf(pKey);
    for (int i = 0; i < res.length; i++) {
      res[i] = keys.get(index + i);
    }
    return res;
  }

  public void addError(@Nonnull SLStateError pError) {
    errors.add(pError);
  }

  @Override
  public String getCPAName() {
    return "SLCPA";
  }

  @Override
  public boolean checkProperty(String property) throws InvalidQueryException {
    switch (property) {
      case HAS_LEAKS:
        return errors.contains(SLStateError.MEMORY_LEAK);

      case HAS_INVALID_READS:
        return errors.contains(SLStateError.INVALID_READ);

      case HAS_INVALID_WRITES:
        return errors.contains(SLStateError.INVALID_WRITE);

      case HAS_INVALID_FREES:
        return errors.contains(SLStateError.INVALID_FREE);

      default:
        throw new InvalidQueryException("Query '" + property + "' is invalid.");
    }
  }

  public @Nullable SLState copyWithoutErrors() {
    PathFormula newFormula =
        new PathFormula(
            pathFormula.getFormula(),
            pathFormula.getSsa(),
            pathFormula.getPointerTargetSet(),
            pathFormula.getLength());
    SLState s =
        new SLState(
        newFormula,
            new HashMap<>(heap),
            new HashMap<>(stack),
            new ArrayList<>(heapKeys),
            new ArrayList<>(stackKeys),
            new HashSet<>(constraints),
            new HashMap<>(allocationSizes),
            new HashMap<>(allocas),
        null);
    return s;
  }

  public Map<Formula, BigInteger> getAllocationSizes() {
    return allocationSizes;
  }

  public Map<String, Set<Formula>> getAllocas() {
    return allocas;
  }

  public void setPathFormula(PathFormula pFormula) {
    pathFormula = pFormula;
  }

  public void addAlloca(Formula pLoc, String pCaller) {
    if (!allocas.containsKey(pCaller)) {
      allocas.put(pCaller, new HashSet<>());
    }
    allocas.get(pCaller).add(pLoc);

  }

  public void addConstraint(BooleanFormula pConstraint) {
    constraints.add(pConstraint);
  }

  public Set<BooleanFormula> getConstraints() {
    return constraints;
  }

  public boolean heapIsEmpty() {
    return heap.isEmpty();
  }

}
