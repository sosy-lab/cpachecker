/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import java.util.Collection;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

public interface Memory {

  public static class InvalidPointerException extends Exception {

    public InvalidPointerException(String msg)  {
      super(msg);
    }

    private static final long serialVersionUID = 5559627789061016553L;
  }

  /**
   * A contiguous block of heap memory returned by a single malloc
   */
  public final static class MemoryRegion {

    // global counter to have an unique id for each memory region
    private static int idCounter = 0;

    private final int id;
    private long length = -1; // length of region in bytes, -1 if unknown
    private boolean isValid; // denotes if this memory region is safe to access




    public MemoryRegion() {
      synchronized (this.getClass()) {
        id = idCounter++;
      }
    }

    /**
     * Checks if the length of this region is known.
     */
    public boolean hasLength() {
      return length != -1;
    }

    /**
     * Returns the length of this memory region in bytes. The return value is
     * undefined, if the length is not known (i.e., if hasLength() returns false).
     */
    public long getLength() {
      return length;
    }


    /**
     * Check if this memory region is still valid, i.e. save to access
     * @return true, if this memory region is valid - false otherwise
     */
    public boolean isValid() {
      return isValid;
    }

    /**
     * Set the validity of this memory region (e.g. memory is valid after malloc, but invalid after free)
     * @param pIsValid the validity of this memory region
     */
    public void setValid(boolean pIsValid) {
      isValid = pIsValid;
    }
    /**
     * Set the length of this memory region in bytes, if it was unknown before.
     * As objects of this type are considered constant, this method should be
     * called as soon as possible after the constructor.
     * @throws InvalidPointerException If length is negative.
     */
    public void setLength(long length) {
      // allow setting this value only once
      if (hasLength() && this.length != length) {
        throw new IllegalArgumentException("Trying to alter size of memory region " + this);
      }
      if (length < 0) {
        throw new IllegalArgumentException("Invalid size " + length + " for memory region " + this);
      }
      this.length = length;
    }

    @Override
    public String toString() {
     return "Mem"+ "[" + (isValid() ? "VALID" : "INVALID") + "]" + (length > 0 ? "(" + length + ")[" : "[" ) + id + "]";
    }
  }

  /**
   * An super interface for everything where a pointer may point to
   */
  public static interface PointerTarget {

    /**
     * Shift this pointer target. The offset is given in Bytes!
     */
    public PointerTarget addOffset(long shiftBytes) throws InvalidPointerException;

    /**
     * Shift this pointer target by an unknown offset.
     */
    public PointerTarget addUnknownOffset() throws InvalidPointerException;

    /**
     * If this pointer target is the null pointer.
     */
    public boolean isNull();
  }

  public static final PointerTarget NULL_POINTER = new PointerTarget() {
    @Override
    public PointerTarget addOffset(long shift) throws InvalidPointerException {
      if (shift == 0) {
        return this;
      } else {
        throw new InvalidPointerException("Pointer arithmetics on null pointer!");
      }
    }

    @Override
    public PointerTarget addUnknownOffset() throws InvalidPointerException {
      throw new InvalidPointerException("Pointer arithmetics on null pointer!");
    }

    @Override
    public boolean isNull() {
      return true;
    }

    @Override
    public String toString() {
      return "NULL";
    }
  };

  public static final PointerTarget INVALID_POINTER = new PointerTarget() {
    @Override
    public PointerTarget addOffset(long shift) throws InvalidPointerException {
      return this; // silently ignore
    }

    @Override
    public PointerTarget addUnknownOffset() throws InvalidPointerException {
      return this; // silently ignore
    }

    @Override
    public boolean isNull() {
      return false;
    }

    @Override
    public String toString() {
      return "INVALID";
    }
  };

  public static final PointerTarget UNINITIALIZED_POINTER = new PointerTarget() {
    @Override
    public PointerTarget addOffset(long shift) throws InvalidPointerException {
      if (shift == 0) {
        return this;
      } else {
        throw new InvalidPointerException("Pointer arithmetics on uninitialized pointer!");
      }
    }

    @Override
    public PointerTarget addUnknownOffset() throws InvalidPointerException {
      throw new InvalidPointerException("Pointer arithmetics on uninitialized pointer!");
    }

    @Override
    // thats the question ... isn't it
    public boolean isNull() {
      return false;
    }

    @Override
    public String toString() {
      return "UNINITIALIZED";
    }
  };

  public static final PointerTarget UNKNOWN_POINTER = new PointerTarget() {
    @Override
    public PointerTarget addOffset(long shift) throws InvalidPointerException {
      return this; // silently ignore
    }

    @Override
    public PointerTarget addUnknownOffset() throws InvalidPointerException {
      return this; // silently ignore
    }

    @Override
    public boolean isNull() {
      return false;
    }

    @Override
    public String toString() {
      return "UNKNOWN";
    }
  };

  /**
   * A heap address, consisting of a MemoryRegion and an offset
   */
  public final static class MemoryAddress implements PointerTarget, PointerLocation {

    private final MemoryRegion region;
    private final long offset; // offset in bytes from the start of the region
                               // -1 if unknown

    private MemoryAddress(MemoryRegion region, boolean unknownOffset) {
      assert region != null;
      this.region = region;

      if (unknownOffset) {
        this.offset = -1;
      } else {
        throw new IllegalArgumentException("Only use this constructor with unknownOffset = true");
      }
    }

    public MemoryAddress(MemoryRegion region) {
      assert region != null;
      this.region = region;
      this.offset = 0;
    }

    private MemoryAddress(MemoryRegion region, long offset) throws InvalidPointerException {
      if (region == null) {
        throw new IllegalArgumentException("MemoryAddress needs a MemoryRegion");
      }
      if ((offset < 0) || region.hasLength() && (offset >= region.getLength())) {
        throw new InvalidPointerException("Invalid offset " + offset
                                        + " for memory region " + region + "!");
      }
      this.region = region;
      this.offset = offset;
    }

    @Override
    public boolean isNull() {
      return true;
    }

    @Override
    public MemoryAddress addOffset(long shiftBytes) throws InvalidPointerException {
      if (!hasOffset()) {
        if (region.hasLength() && (Math.abs(shiftBytes) >= region.getLength())) {
          // current offset is unknown, but this shift is too large for sure
          throw new InvalidPointerException("Invalid shift " + shiftBytes
                                          + " for memory address " + this);
        }
        return this;
      }

      return (shiftBytes == 0) ? this : new MemoryAddress(region, offset+shiftBytes);
    }

    @Override
    public MemoryAddress addUnknownOffset() {
      return hasOffset() ? new MemoryAddress(region, true) : this;
    }

    public boolean hasOffset() {
      return (offset != -1);
    }

    @Override
    public Pointer getPointer(Memory memory) {
      return memory.getPointer(this);
    }

    @Override
    public boolean equals(Object other) {
      if ((other == null) || !(other instanceof MemoryAddress)) {
        return false;
      }

      MemoryAddress otherAddress = (MemoryAddress)other;

      // if offset is unknown, we do not know (return false)
      return (hasOffset())
          && (this.region.equals(otherAddress.region))
          && (this.offset == otherAddress.offset);
    }

    @Override
    public int hashCode() {
      return region.hashCode() + 17 * (int)offset;
    }

    @Override
    public String toString() {
      return region + (hasOffset() ? "[" + offset + "]" : "[?]" );
    }

    public MemoryRegion getRegion() {
      return region;
    }

    public long getOffset() {
      return offset;
    }
  }

  /**
   * A simple variable, local or global.
   */
  abstract static class Variable implements PointerTarget, PointerLocation {

    private final String name;

    public Variable(String name) {
      assert name != null;
      this.name = name;
    }

    @Override
    public PointerTarget addOffset(long shiftBytes) throws InvalidPointerException {
      if (shiftBytes != 0) {
        throw new InvalidPointerException("Pointer calculcations for simple variable " + this);
      } else {
        return this;
      }
    }

    @Override
    public PointerTarget addUnknownOffset() throws InvalidPointerException {
      throw new InvalidPointerException("Pointer calculcations for simple variable " + this);
    }

    @Override
    public boolean isNull() {
      return false;
    }

    public String getVarName() {
      return name;
    }

    @Override
    public boolean equals(Object other) {
      // no need to check whether both global/local
      // CIL renames variables so they have a unique name
      return (other != null)
          && (other instanceof Variable)
          && (getVarName().equals(((Variable)other).getVarName()));
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    @Override
    public String toString() {
      return name;
    }
  }

  /**
   * A simple local variable.
   */
  public static class LocalVariable extends Variable {

    private String function;

    public LocalVariable(String function, String name) {
      super(name);
      assert function != null;
      this.function = function;
    }

    @Override
    public Pointer getPointer(Memory memory) {
      return memory.getPointer(this);
    }

    public String getFunctionName() {
      return function;
    }

    @Override
    public String toString() {
      return function + ":" + super.toString();
    }

    @Override
    public boolean equals(Object other) {
      return (other instanceof LocalVariable)
              && super.equals(other)
              && function.equals(((LocalVariable)other).function);
    }
  }


  /**
   * A simple global variable.
   */
  public static class GlobalVariable extends Variable {

    public GlobalVariable(String name) {
      super(name);
    }

    @Override
    public Pointer getPointer(Memory memory) {
      return memory.getPointer(this);
    }

    @Override
    public boolean equals(Object other) {
      return (other instanceof GlobalVariable) && super.equals(other);
    }
  }

  public final static class StackArray {

    private final String name;
    private final long length;

    public StackArray(String name, long length) {
      if (name == null || length < 0) {
        throw new IllegalArgumentException();
      }
      this.name = name;
      this.length = length;
    }

    public long getLength() {
      return length;
    }

    @Override
    public String toString() {
      return name + "(" + length + ")";
    }
  }

  public static class StackArrayCell extends LocalVariable {

    private final StackArray array;
    private final long offset;

    private StackArrayCell(String function, StackArray array, boolean unknownOffset) {
      super(function, array.name);
      this.array = array;

      if (unknownOffset) {
        this.offset = -1;
      } else {
        throw new IllegalArgumentException("Only use this constructor with unknownOffset = true");
      }
    }

    public StackArrayCell(String function, StackArray array) {
      super(function, array.name);

      this.array = array;
      this.offset = 0;
    }

    private StackArrayCell(String function, StackArray array, long offset) throws InvalidPointerException {
      super(function, array.name);

      if ((offset < 0) || (offset >= array.getLength())) {
        throw new InvalidPointerException("Invalid offset " + offset
                                        + " for stack array " + array + "!");
      }
      this.array = array;
      this.offset = offset;
    }

    @Override
    public StackArrayCell addOffset(long shiftBytes) throws InvalidPointerException {
      if (offset == -1) {
        if (Math.abs(shiftBytes) >= array.getLength()) {
          // current offset is unknown, but this shift is too large for sure
          throw new InvalidPointerException("Invalid shift " + shiftBytes
              + " for stack array " + array + "!");
        }
        return this;
      }

      return (shiftBytes == 0) ? this : new StackArrayCell(getFunctionName(), array, offset+shiftBytes);
    }

    @Override
    public StackArrayCell addUnknownOffset() {
      if (offset == -1) {
        return this;
      }
      return new StackArrayCell(getFunctionName(), array, true);
    }

    @Override
    public Pointer getPointer(Memory memory) {
      if (offset != -1) {
        return null;
      } else {
        return super.getPointer(memory);
      }
    }

    @Override
    public String getVarName() {
      return "__cpa_stack_array__" + super.getVarName() + "__" + offset;
    }

    @Override
    public boolean equals(Object other) {
      if ((other == null) || !(other instanceof StackArrayCell)) {
        return false;
      }

      StackArrayCell otherCell = (StackArrayCell)other;

      // if offset is unknown, we do not know (return false)
      return (offset != -1)
          && (this.array.equals(otherCell.array))
          && (this.offset == otherCell.offset);
    }

    @Override
    public String toString() {
      return array + (offset == -1 ? "[?]" : "[" + offset + "]");
    }
  }

  public static interface PointerLocation {

    public Pointer getPointer(Memory memory);

  }

  public void addNewGlobalPointer(String name, Pointer p);

  public Pointer lookupPointer(String name);

  /**
   * Look up a variable name in the current context. If the variable does not
   * exist in the program, the result is undefined.
   *
   * @param name  The name of the variable.
   * @return An object of type Variable which represents the variable.
   */
  public Variable lookupVariable(String name);

  public void writeOnHeap(MemoryAddress memAddress, Pointer p);

  public Pointer getPointer(LocalVariable var);

  public void addNewLocalPointer(String name, Pointer p);

  public Pointer getPointer(GlobalVariable var);

  public Pointer getPointer(MemoryAddress memAddress);

  public boolean areAliases(Pointer p1, Pointer p2);

  /**
   * Register the fact, that secondPointer is now an alias of firstPointer.
   * This means that in all cases secondPointer points to the same target as
   * firstPointer. The current aliases of secondPointer are not touched.
   * This method does not change the actual list of targets of secondPointer.
   *
   * @param firstPointer  The location of the first pointer.
   * @param secondPointer The location of the second pointer.
   */
  public void makeAlias(PointerLocation firstPointer, PointerLocation secondPointer);

  public MemoryAddress malloc();

  public void free(MemoryRegion mem) throws InvalidPointerException;

  /**
   * Try to dereference a single target of a pointer and returns the referenced value.
   * If the target is NULL, UNKNOWN or INVALID, the method returns null.
   * This method may be only called for pointers to other pointers, otherwise the
   * behavior is undefined.
   *
   * @param pointer The pointer to which the target belongs.
   * @param target  The target to dereference.
   * @return  The reference value or null if the target could not be dereferenced (e.g. NULL or UNKNOWN pointer).
   */
  public Pointer deref(Pointer pointer, PointerTarget target);

  public Collection<MemoryRegion> checkMemoryLeak();

  public CFAEdge getCurrentEdge();
}