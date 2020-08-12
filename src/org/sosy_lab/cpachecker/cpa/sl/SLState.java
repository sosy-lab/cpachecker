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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
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
  private final LinkedHashMap<Formula, Formula> heap;
  private final LinkedHashMap<Formula, Formula> stack;
  private final LinkedHashSet<BooleanFormula> constraints;

  private final Map<Formula, BigInteger> allocationSizes;
  private final Set<Formula> inScopePtrs;
  private final Set<CVariableDeclaration> declarations;
  private final Map<String, Set<Formula>> allocas;

  private final Set<SLStateError> errors = new HashSet<>();


  public SLState(
      PathFormula pPathFormula,
      LinkedHashMap<Formula, Formula> pHeap,
      LinkedHashMap<Formula, Formula> pStack,
      LinkedHashSet<BooleanFormula> pConstraints,
      Map<Formula, BigInteger> pAllocationSizes,
      Set<Formula> pInScopePtrs,
      Set<CVariableDeclaration> pDeclarations,
      Map<String, Set<Formula>> pAllocas,
      SLStateError pError) {
    pathFormula = pPathFormula;
    heap = pHeap;
    stack = pStack;
    constraints = pConstraints;
    allocationSizes = pAllocationSizes;
    inScopePtrs = pInScopePtrs;
    declarations = pDeclarations;
    allocas = pAllocas;
    if (pError != null) {
      errors.add(pError);
    }

  }

  public SLState(PathFormula pStore) {
    this(
        pStore,
        new LinkedHashMap<>(),
        new LinkedHashMap<>(),
        new LinkedHashSet<>(),
        new HashMap<>(),
        new HashSet<>(),
        new HashSet<>(),
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

  public LinkedHashMap<Formula, Formula> getHeap() {
    return heap;
  }

  public LinkedHashMap<Formula, Formula> getStack() {
    return stack;
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
            new LinkedHashMap<>(heap),
            new LinkedHashMap<>(stack),
            new LinkedHashSet<>(constraints),
            new HashMap<>(allocationSizes),
            new HashSet<>(inScopePtrs),
            new HashSet<>(declarations),
            new HashMap<>(allocas),
        null);
    return s;
  }

  public Map<Formula, BigInteger> getAllocationSizes() {
    return allocationSizes;
  }

  public Set<CVariableDeclaration> getDeclarations() {
    return declarations;
  }

  public Set<Formula> getInScopePtrs() {
    return inScopePtrs;
  }

  public Map<String, Set<Formula>> getAllocas() {
    return allocas;
  }

  public void addInScopePtr(Formula pPtr) {
    inScopePtrs.add(pPtr);
  }

  public void removeInScopePtr(Formula pPtr) {
    inScopePtrs.remove(pPtr);
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

  public LinkedHashSet<BooleanFormula> getConstraints() {
    return constraints;
  }
}
