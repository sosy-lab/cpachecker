package cpa.pointeranalysis;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface Memory {

  public static class InvalidPointerException extends Exception {
    
    public InvalidPointerException() { }
    
    public InvalidPointerException(String msg)  {
      super(msg);
    }
    
    private static final long serialVersionUID = 5559627789061016553L;
  }
  
  /**
   * A contigous block of heap memory returned by a single malloc
   */
  public final static class MemoryRegion {
    
    // global counter to have an unique id for each memory region
    private static int idCounter = 0;
  
    private final int id;
    private int length = -1; // length of region in bytes, -1 if unknown
        
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
    public int getLength() {
      return length;
    }
    
    /**
     * Set the length of this memory region in bytes, if it was unknown before.
     * As objects of this type are considered constant, this method should be
     * called as soon as possible after the constructor.
     */
    public void setLength(int length) {
      // allow setting this value only once
      if (hasLength() && this.length != length) {
        throw new IllegalArgumentException();
      }
      if (length < 0) {
        throw new IllegalArgumentException();
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
    public PointerTarget addOffset(int shiftBytes) throws InvalidPointerException;    
    
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
    public PointerTarget addOffset(int shift) throws InvalidPointerException {
      return (shift == 0) ? this : INVALID_POINTER;
    }
    
    @Override
    public PointerTarget addUnknownOffset() throws InvalidPointerException {
      return INVALID_POINTER;
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
    public PointerTarget addOffset(int shift) throws InvalidPointerException {
      return this;
    }
    
    @Override
    public PointerTarget addUnknownOffset() throws InvalidPointerException {
      return this;
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
    public PointerTarget addOffset(int shift) throws InvalidPointerException {
      // ignore
      return this;
    }
    
    @Override
    public PointerTarget addUnknownOffset() throws InvalidPointerException {
      // ignore
      return this;
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
    private final int offset; // offset in bytes from the start of the region
                              // -1 if unknown
    
    public MemoryAddress(MemoryRegion region) {
      if (region == null) {
        throw new IllegalArgumentException("MemoryAddress needs a MemoryRegion");
      }
      this.region = region;
      this.offset = -1;
    }
    
    MemoryAddress(MemoryRegion region, int offset) throws InvalidPointerException {
      if (region == null) {
        throw new IllegalArgumentException("MemoryAddress needs a MemoryRegion");
      }
      if (offset < 0) {
        throw new IllegalArgumentException("Negative offset");
      }
      if (region.hasLength() && (offset >= region.getLength())) {
        throw new InvalidPointerException("Invalid offset " + offset
            + " (length " + region.getLength() + ")");
      }
      this.region = region;
      this.offset = offset;
    }
    
    @Override
    public boolean isNull() {
      return true;
    }
    
    @Override
    public MemoryAddress addOffset(int shiftBytes) throws InvalidPointerException {
      if (offset == -1 || shiftBytes == 0) {
        return this;
      }
      return new MemoryAddress(region, offset+shiftBytes);
    }
    
    @Override
    public MemoryAddress addUnknownOffset() throws InvalidPointerException {
      if (offset == -1) {
        return this;
      }
      return new MemoryAddress(region);
    }
    
    @Override
    public Pointer getPointer(Memory memory) {
      return memory.getHeapPointer(this);
    }
    
    @Override
    public boolean equals(Object other) {
      if ((other == null) || !(other instanceof MemoryAddress)) {
        return false;
      }
      
      MemoryAddress otherAddress = (MemoryAddress)other;
      
      // if offset is unknown, we do not know (return false)
      return (offset != -1)
          && (this.region == otherAddress.region)
          && (this.offset == otherAddress.offset);
    }
    
    @Override
    public String toString() {
      return region + (offset == -1 ? "[?]" : "[" + offset + "]");
    }

    public MemoryRegion getRegion() {
      return region;
    }

    public int getOffset() {
      return offset;
    }
  }

  /**
   * A simple variable, local or global.
   */
  abstract static class Variable implements PointerTarget, PointerLocation {
    
    private final String name;
    
    public Variable(String name) {
      this.name = name;
    }
    
    @Override
    public PointerTarget addOffset(int shiftBytes) throws InvalidPointerException {
      throw new InvalidPointerException("No pointer calculcations for simple variables");
    }

    @Override
    public PointerTarget addUnknownOffset() throws InvalidPointerException {
      throw new InvalidPointerException("No pointer calculcations for simple variables");
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
      return "&" + name;
    }
  }
   
  /**
   * A simple local variable.
   */
  public final static class LocalVariable extends Variable {
    
    private String function;
    
    public LocalVariable(String function, String name) {
      super(name);
      this.function = function;
    }
    
    @Override
    public Pointer getPointer(Memory memory) {
      return memory.getLocalPointers().get(getVarName());
    }
    
    public String getFunctionName() {
      return function;
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
      return memory.getGlobalPointers().get(getVarName());
    }
  }
  
  public static interface PointerLocation {
    
    public Pointer getPointer(Memory memory);
    
  }
  
  public void addNewGlobalPointer(String name, Pointer p);

  public void addNewLocalPointer(String name, Pointer p);

  public Pointer getPointer(String name);

  public Pointer getHeapPointer(MemoryAddress memAddress);

  public void writeOnHeap(MemoryAddress memAddress, Pointer p);

  public Map<String, Pointer> getGlobalPointers();

  public Map<String, Pointer> getLocalPointers();
  
  public Set<PointerLocation> getReversePointers(PointerTarget target);

  //public void addReverseRelation(PointerTarget target, PointerLocation location);
  
  //public void removeAllReverseRelations(PointerLocation location);
  
  /**
   * Get all aliases of a pointer. An alias of a pointer is another pointer which points
   * to the same target in all cases.
   * 
   * @param pointer The pointer for which the aliases should be returned.
   * @return  An unmodifiable set with all aliases including the original pointer. Is never null.
   */
  public Set<PointerLocation> getAliases(PointerLocation pointer);
  
  public Set<PointerLocation> getAliases(Pointer pointer);
  
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
  
  //public void findAndMergePossibleAliases(Pointer p);
  
  //public void removeAllAliases(PointerLocation pointer);
  
  public MemoryAddress malloc() throws InvalidPointerException;
  
  public void free(Pointer p) throws InvalidPointerException;
  
  public void free(MemoryAddress mem) throws InvalidPointerException;
  
  public void free(MemoryRegion mem) throws InvalidPointerException;

  /**
   * Tries to dereference a pointer target and returns the referenced value.
   * 
   * @param target  The target to dereference.
   * @param levelOfIndirection The level of indirection of the pointer the target belongs to.
   * @return  The reference value or null if the target could not be dereferenced (e.g. NULL or UNKNOWN pointer).
   * @throws InvalidPointerException  If the target points to a non-pointer value.
   */
  public Pointer deref(PointerTarget target, int levelOfIndirection) throws InvalidPointerException;
  
  public Collection<MemoryRegion> checkMemoryLeak();

}