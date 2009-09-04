package cpa.pointeranalysis;

import java.util.Collection;

import cfa.objectmodel.CFAEdge;

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
     return "Mem" + (length > 0 ? "(" + length + ")[" : "[" ) + id + "]";
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
      if (offset == -1) {
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
      if (offset == -1) {
        return this;
      }
      return new MemoryAddress(region, true);
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
      return (offset != -1)
          && (this.region.equals(otherAddress.region))
          && (this.offset == otherAddress.offset);
    }
    
    @Override
    public String toString() {
      return region + (offset == -1 ? "[?]" : "[" + offset + "]");
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
      throw new InvalidPointerException("No pointer calculcations for simple variable " + this);
    }

    @Override
    public PointerTarget addUnknownOffset() throws InvalidPointerException {
      throw new InvalidPointerException("No pointer calculcations for simple variable" + this);
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
  public final static class LocalVariable extends Variable {
    
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
  public final static class GlobalVariable extends Variable {
    
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
  
  public static interface PointerLocation {
    
    public Pointer getPointer(Memory memory);
    
  }
  
  public void addNewGlobalPointer(String name, Pointer p);

  public void addNewLocalPointer(String name, Pointer p);

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