// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.cpa.sl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class SLState implements AbstractState, AbstractQueryableState, Graphable {

  private static final String HAS_INVALID_READS = "has-invalid-reads";
  private static final String HAS_INVALID_WRITES = "has-invalid-writes";
  private static final String HAS_LEAKS = "has-leaks";
  private static final String HAS_INVALID_FREES = "has-invalid-frees";

  public enum SLStateError {
    INVALID_READ,
    INVALID_WRITE,
    INVALID_FREE,
    MEMORY_LEAK
  }

  /**
   * The SSAMap to construct formulas representing the symbolic locations of variables.
   */
  private final SSAMap ssa;

  /**
   * The map representing the memory state. Can be eventually converted to a SL formula in the form
   * key_1->value_1 * ... * key_n->value_n
   */
  private final ImmutableMap<Formula, Formula> heap;
  private final ImmutableMap<Formula, Formula> stack;

  /**
   * The pure part of the SL formula.
   */
  private final ImmutableSet<BooleanFormula> constraints;

  /**
   * Tracks the allocation size of each memory segment. The key represents the start address of the
   * respective segment.
   */
  private final ImmutableMap<Formula, BigInteger> allocationSizes;

  /**
   * The start address of all segments allocated by the function call alloca() mapped to their
   * associated function scope.
   */
  private final ImmutableMap<Formula, String> allocas;

  /**
   * All memory safety properties violated by the current state.
   */
  private final ImmutableSet<SLStateError> errors;

  public static class Builder {
    private SSAMap ssa;
    private ImmutableMap<Formula, String> allocas;
    private ImmutableMap<Formula, BigInteger> segmentSizes;
    private ImmutableMap<Formula, Formula> heap;
    private ImmutableMap<Formula, Formula> stack;
    private ImmutableSet<BooleanFormula> constraints;
    private ImmutableSet<SLStateError> errors;

    private Builder() {
      ssa = SSAMap.emptySSAMap();
      allocas = ImmutableMap.of();
      segmentSizes = ImmutableMap.of();
      heap = ImmutableMap.of();
      stack = ImmutableMap.of();
      constraints = ImmutableSet.of();
      errors = ImmutableSet.of();
    }

    public Builder(SLState pState, boolean includeErrors) {
      ssa = pState.getSsaMap();
      allocas = pState.getAllocas();
      segmentSizes = pState.getAllocationSizes();
      heap = pState.getHeap();
      stack = pState.getStack();
      constraints = pState.getConstraints();
      if(includeErrors) {
        errors = pState.getErrors();
      } else {
        errors = ImmutableSet.of();
      }
    }

    public Builder ssaMap(SSAMap pSsa) {
      ssa = pSsa;
      return this;
    }

    public Builder addSegmentSize(Formula pStartLoc, BigInteger pSize) {
      ImmutableMap.Builder<Formula, BigInteger> b = new ImmutableMap.Builder<>();
      segmentSizes = b.putAll(segmentSizes).put(pStartLoc, pSize).build();
      return this;
    }

    public Builder removeSegmentSize(Formula pStartLoc) {
      ImmutableMap.Builder<Formula, BigInteger> b = new ImmutableMap.Builder<>();
      segmentSizes.forEach((k, v) -> {
        if(!k.equals(pStartLoc)) {
          b.put(k, v);
        }
      });
      segmentSizes = b.build();
      return this;
    }

    public Builder addAlloca(Formula pLoc, String pFunctionScope) {
      ImmutableMap.Builder<Formula, String> b = new ImmutableMap.Builder<>();
      allocas = b.putAll(allocas).put(pLoc, pFunctionScope).build();
      return this;
    }

    public Builder addConstraint(BooleanFormula pConstraint) {
      ImmutableSet.Builder<BooleanFormula> b = new ImmutableSet.Builder<>();
      constraints = b.addAll(constraints).add(pConstraint).build();
      return this;
    }

    public Builder addError(SLStateError pError) {
      ImmutableSet.Builder<SLStateError> b = new ImmutableSet.Builder<>();
      errors = b.addAll(errors).add(pError).build();
      return this;
    }

    public Builder putOn(boolean onHeap, Formula pKey, Formula pVal) {
      ImmutableMap.Builder<Formula, Formula> mapBuilder = new ImmutableMap.Builder<>();
      ImmutableMap<Formula, Formula> memory = onHeap ? heap : stack;
      if (!memory.containsKey(pKey)) {
        mapBuilder.putAll(memory);
        mapBuilder.put(pKey, pVal);
      } else {
        memory.forEach((k, v) -> {
          if (k.equals(pKey)) {
            mapBuilder.put(pKey, pVal);
          } else {
            mapBuilder.put(k, v);
          }
        });
      }
      ImmutableMap<Formula, Formula> res = mapBuilder.build();
      if (onHeap) {
        heap = res;
      } else {
        stack = res;
      }
      return this;
    }

    public Builder removeFrom(boolean fromHeap, Formula pKey) {
      ImmutableMap.Builder<Formula, Formula> mapBuilder = new ImmutableMap.Builder<>();
      ImmutableMap<Formula, Formula> memory = fromHeap ? heap : stack;
      memory.forEach((k, v) -> {
        if (!k.equals(pKey)) {
          mapBuilder.put(k, v);
        }
      });
      ImmutableMap<Formula, Formula> res = mapBuilder.build();
      if (fromHeap) {
        heap = res;
      } else {
        stack = res;
      }
      return this;
    }

    public SLState build() {
      return new SLState(this);
    }
  }

  private SLState(Builder pBuilder) {
    ssa = pBuilder.ssa;
    allocas = pBuilder.allocas;
    allocationSizes = pBuilder.segmentSizes;
    heap = pBuilder.heap;
    stack = pBuilder.stack;
    constraints = pBuilder.constraints;
    errors = pBuilder.errors;
  }

  public static SLState copyWithoutErrors(SLState state) {
    return new SLState.Builder(state, false).build();
  }

  public static SLState empty() {
    return new Builder().build();
  }

  public SSAMap getSsaMap() {
    return ssa;
  }

  public ImmutableSet<SLStateError> getErrors() {
    return errors;
  }

  public ImmutableMap<Formula, Formula> getHeap() {
    return heap;
  }

  public ImmutableMap<Formula, Formula> getStack() {
    return stack;
  }

  public List<Formula> getSegment(boolean fromHeap, Formula pKey, int size) {
    List<Formula> res = new ArrayList<>(size);
    int counter = 0;
    boolean found = false;
    for (Entry<Formula, Formula> entry : fromHeap ? heap.entrySet() : stack.entrySet()) {
      found = found || entry.getKey().equals(pKey);
      if (found) {
        if (counter < size) {
          res.add(entry.getKey());
          counter++;
        } else {
          break;
        }
      }
    }
    return res;
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

  public ImmutableMap<Formula, BigInteger> getAllocationSizes() {
    return allocationSizes;
  }

  public ImmutableMap<Formula, String> getAllocas() {
    return allocas;
  }

  public ImmutableSet<BooleanFormula> getConstraints() {
    return constraints;
  }

  public boolean heapIsEmpty() {
    return heap.isEmpty();
  }

  @Override
  public String toDOTLabel() {
    return errors.isEmpty() ? "No error" : "Errors: " + errors.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return !errors.isEmpty();
  }
}
