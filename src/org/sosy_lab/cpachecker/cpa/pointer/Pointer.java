/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.pointer;

import static org.sosy_lab.cpachecker.cpa.pointer.Memory.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.pointer.Memory.InvalidPointerException;
import org.sosy_lab.cpachecker.cpa.pointer.Memory.MemoryAddress;
import org.sosy_lab.cpachecker.cpa.pointer.Memory.PointerLocation;
import org.sosy_lab.cpachecker.cpa.pointer.Memory.PointerTarget;
import org.sosy_lab.cpachecker.cpa.pointer.Memory.Variable;

/**
 * A pointer is a set of possible targets.
 */
public class Pointer implements Cloneable {

  private int                sizeOfTarget;

  private Set<PointerTarget> targets;

  private int                levelOfIndirection; // how many stars does this pointer have?

  private PointerLocation    location;

  private boolean            actualPointer;

  public Pointer() {
    this(0);
  }

  public Pointer(PointerTarget target) {
    this();
    if (target == null) {
      throw new IllegalArgumentException("Pointer must have a target!");
    }
    targets.clear();
    targets.add(target);
  }

  public Pointer(int levelOfIndirection) {
    this(-1, levelOfIndirection, new HashSet<PointerTarget>(), null);
    // if uninitialized, pointer is null
    targets.add(NULL_POINTER);
  }

  private Pointer(int sizeOfTarget, int levelOfIndirection,
        Set<PointerTarget> targets, PointerLocation location) {
    this.sizeOfTarget = sizeOfTarget;
    this.levelOfIndirection = levelOfIndirection;
    this.targets = new HashSet<>(targets);
    this.location = location;
  }

  private void join(Pointer p) {
    assert p != null;
    // this adds all targets from p to this pointer
    targets.addAll(p.targets);

    // update target size
    if (targets.size() <= 1) {
      sizeOfTarget = p.getSizeOfTarget();
    }
  }

  /*public boolean isUnsafe() {
    return targets.contains(NULL_POINTER) || targets.contains(INVALID_POINTER);
  }*/

  public boolean isDereferencable() {
    for (PointerTarget target : targets) {
      if (target == UNKNOWN_POINTER
          || target instanceof Variable
          || target instanceof MemoryAddress) {
        return true;
      }
    }
    return false;
  }

  public boolean isSafe() {
    return !(targets.contains(NULL_POINTER)
        || targets.contains(INVALID_POINTER) || targets
        .contains(UNKNOWN_POINTER)) || targets.contains(UNINITIALIZED_POINTER);

  }

  public boolean isSubsetOf(Pointer other) {
    assert other != null;
    return (this == other) || other.targets.containsAll(targets);
  }

  public boolean isDifferentFrom(Pointer other) {
    assert other != null;
    return !this.isSubsetOf(other)
        && !other.isSubsetOf(this)
        && !(targets.contains(INVALID_POINTER)
        && other.targets.contains(INVALID_POINTER))
        && !targets.contains(UNKNOWN_POINTER)
        && !other.targets.contains(UNKNOWN_POINTER)
        && !targets.contains(UNINITIALIZED_POINTER)
        && !other.targets.contains(UNINITIALIZED_POINTER);
  }

  public boolean contains(PointerTarget target) {
    assert target != null;
    return targets.contains(target);
  }

  private void addOffset(long shift, boolean keepOldTargets, Memory memory) {
    if (!hasSizeOfTarget()) {
      addUnknownOffset(keepOldTargets, memory);

    } else {
      Set<PointerTarget> newTargets = new HashSet<>();
      long byteShift = shift * sizeOfTarget;

      for (PointerTarget target : targets) {
        try {
          newTargets.add(target.addOffset(byteShift));

        } catch (InvalidPointerException e) {
          PointerTransferRelation.addWarning(e.getMessage(),
              memory.getCurrentEdge(), target.toString());

          newTargets.add(INVALID_POINTER);
        }
      }
      assert newTargets.size() >= 1;

      if (keepOldTargets) {
        targets.addAll(newTargets);
      } else {
        targets = newTargets;
      }
    }
  }

  private void addUnknownOffset(boolean keepOldTargets, Memory memory) {
    Set<PointerTarget> newTargets = new HashSet<>();

    for (PointerTarget target : targets) {
      try {
        newTargets.add(target.addUnknownOffset());
      } catch (InvalidPointerException e) {
        PointerTransferRelation.addWarning(e.getMessage(),
            memory.getCurrentEdge(), target.toString());

        newTargets.add(INVALID_POINTER);
      }
    }
    assert newTargets.size() >= 1;

    if (keepOldTargets) {
      targets.addAll(newTargets);
    } else {
      targets = newTargets;
    }
  }

  public int getNumberOfTargets() {
    return targets.size();
  }

  public Set<PointerTarget> getTargets() {
    return Collections.unmodifiableSet(targets);
  }

  public PointerTarget getFirstTarget() {
    if (getNumberOfTargets() >= 1) {
      return targets.iterator().next();
    } else {
      return null;
    }
  }

  /**
   * Checks if the size of the target of the pointer is known.
   */
  public boolean hasSizeOfTarget() {
    return sizeOfTarget != -1;
  }

  /**
   * Returns the size of the target of the pointer in bytes. The return value is
   * undefined, if the length is not known (i.e., if hasSizeOfTarget() returns false).
   */
  public int getSizeOfTarget() {
    return sizeOfTarget;
  }

  /**
   * Set the size of the target of the pointer, if it was unknown before.
   */
  public void setSizeOfTarget(int sizeOfTarget) {
    // allow setting this value only once
    if (hasSizeOfTarget() && this.sizeOfTarget != sizeOfTarget) {
      throw new IllegalArgumentException();
    }
    if (sizeOfTarget <= 0) {
      throw new IllegalArgumentException();
    }
    this.sizeOfTarget = sizeOfTarget;
  }

  public boolean isPointerToPointer() {
    return levelOfIndirection > 1;
  }

  public int getLevelOfIndirection() {
    return levelOfIndirection;
  }

  public PointerLocation getLocation() {
    return location;
  }

  public void setLocation(PointerLocation location) {
    if (this.location != null) {
      throw new IllegalStateException("May not overwrite pointer location!");
    }
    this.location = location;
  }

  public boolean isActualPointer() {
    return actualPointer;
  }

  public void setActualPointer(boolean actualPointer) {
    this.actualPointer = actualPointer;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof Pointer)) {
      return false;
    }
    return location.equals(((Pointer) other).location)
        && targets.equals(((Pointer) other).targets);
  }

  @Override
  public int hashCode() {
    return targets.hashCode();
  }

  @Override
  public Pointer clone() {
    return new Pointer(sizeOfTarget, levelOfIndirection, targets, location);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < levelOfIndirection; i++) {
      sb.append('*');
    }
    sb.append('(');
    for (PointerTarget target : targets) {
      sb.append(' ');
      if (target instanceof Variable) {
        sb.append('&');
      }
      sb.append(target);
      sb.append(' ');
    }
    sb.append(')');
    return sb.toString();
  }

  public static interface PointerOperation {

    public void doOperation(Memory memory, Pointer pointer, boolean keepOldTargets);

  }

  public static class AddOffset implements PointerOperation {

    private final long shift;

    public AddOffset(long shift) {
      this.shift = shift;
    }

    @Override
    public void doOperation(Memory memory, Pointer pointer, boolean keepOldTargets) {
      pointer.addOffset(shift, keepOldTargets, memory);
    }
  }

  public static class AddUnknownOffset implements PointerOperation {

    @Override
    public void doOperation(Memory memory, Pointer pointer, boolean keepOldTargets) {
      pointer.addUnknownOffset(keepOldTargets, memory);
    }
  }

  /**
   * Assigns either a Pointer or a PointerTarget to the given Pointer.
   */
  public static class Assign implements PointerOperation {

    private final PointerTarget assignValueTarget;
    private final Pointer       assignValuePointer;

    public Assign(PointerTarget assignValue) {
      this.assignValueTarget = assignValue;
      this.assignValuePointer = null;
    }

    public Assign(Pointer assignValue) {
      this.assignValueTarget = null;
      this.assignValuePointer = assignValue;
    }

    @Override
    public void doOperation(Memory memory, Pointer pointer, boolean keepOldTargets) {

      if (!keepOldTargets) {
        pointer.targets.clear();
      }

      if (assignValueTarget != null) {
        pointer.targets.add(assignValueTarget);

      } else {
        pointer.join(assignValuePointer);

        if (!keepOldTargets && assignValuePointer.getLocation() != null) {
          memory.makeAlias(assignValuePointer.getLocation(), pointer.getLocation());
        }
      }
    }
  }

  public static class AssignListOfTargets implements PointerOperation {

    private final Collection<PointerTarget> assignValues;

    public AssignListOfTargets(Collection<PointerTarget> assignValues) {
      if (assignValues.isEmpty()) {
        throw new IllegalArgumentException("No targets for assignment!");
      }
      this.assignValues = assignValues;
    }

    @Override
    public void doOperation(Memory memory, Pointer pointer, boolean keepOldTargets) {
      if (!keepOldTargets) {
        pointer.targets.clear();
      }

      pointer.targets.addAll(assignValues);
    }
  }

  public static class AddOffsetAndAssign implements PointerOperation {

    private final Pointer assignValue;
    private final long    shift;

    public AddOffsetAndAssign(Pointer assignValue, long shift) {
      this.assignValue = assignValue;
      this.shift = shift;
    }

    @Override
    public void doOperation(Memory memory, Pointer pointer, boolean keepOldTargets) {
      Pointer shiftedAssignValue = assignValue.clone(); // clone first!

      if (!keepOldTargets) {
        pointer.targets.clear();
      }

      shiftedAssignValue.addOffset(shift, false, memory);

      pointer.join(shiftedAssignValue);
    }
  }

  public static class AddUnknownOffsetAndAssign implements PointerOperation {

    private final Pointer assignValue;

    public AddUnknownOffsetAndAssign(Pointer assignValue) {
      this.assignValue = assignValue;
    }

    @Override
    public void doOperation(Memory memory, Pointer pointer,
        boolean keepOldTargets) {
      Pointer shiftedAssignValue = assignValue.clone(); // clone first!

      if (!keepOldTargets) {
        pointer.targets.clear();
      }

      shiftedAssignValue.addUnknownOffset(false, memory);

      pointer.join(shiftedAssignValue);
    }
  }

  public static class DerefAndAssign implements PointerOperation {

    private final Pointer assignValue;

    public DerefAndAssign(Pointer assignValue) {
      this.assignValue = assignValue;

      if (!assignValue.isPointerToPointer()) {
        throw new IllegalArgumentException("Pointers which do not point "
            + "to other pointers cannot be dereferenced in this analysis!");
      }

      if (!assignValue.isDereferencable()) {
        throw new IllegalArgumentException("Unsafe deref of pointer "
                      + assignValue.getLocation() + " = " + assignValue);
      }
    }

    @Override
    public void doOperation(Memory memory, Pointer pointer, boolean keepOldTargets) {
      if (!keepOldTargets) {
        pointer.targets.clear();
      }

      for (PointerTarget assignTarget : assignValue.getTargets()) {
        Pointer actualAssignValue = memory.deref(assignValue, assignTarget);

        if (actualAssignValue != null) {
          pointer.join(actualAssignValue);

          if (!keepOldTargets && assignValue.getNumberOfTargets() == 1) {
            memory.makeAlias(actualAssignValue.getLocation(), pointer.getLocation());
          }
        }
      }
    }
  }

  public static class MallocAndAssign implements PointerOperation {

    private MemoryAddress memAddress = null;

    @Override
    public void doOperation(Memory memory, Pointer pointer, boolean keepOldTargets) {
      if (!keepOldTargets) {
        pointer.targets.clear();
      }

      if (memAddress == null) {
        memAddress = memory.malloc();
      }

      pointer.targets.add(Memory.NULL_POINTER);
      pointer.targets.add(memAddress);


    }

    public MemoryAddress getMallocResult() {
      return memAddress;
    }
  }

  public static class AssumeInequality implements PointerOperation {

    private final PointerTarget removeTarget;

    public PointerTarget getRemoveTarget() {

      return removeTarget;

    }

    public AssumeInequality(PointerTarget removeTarget) {
      this.removeTarget = removeTarget;
    }

    /**
     * @param keepOldTargets ignored
     */
    @Override
    public void doOperation(Memory memory, Pointer pointer,
        boolean keepOldTargets) {


      pointer.targets.remove(removeTarget);

      if (pointer.getNumberOfTargets() == 0) {
        throw new IllegalStateException("Pointer without target must not exist!");
      }
    }
  }

}