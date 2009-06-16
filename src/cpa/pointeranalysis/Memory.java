package cpa.pointeranalysis;

import java.util.Collection;
import java.util.Map;

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
      throw new InvalidPointerException();
    }
    
    @Override
    public PointerTarget addUnknownOffset() throws InvalidPointerException {
      throw new InvalidPointerException();
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
      throw new InvalidPointerException();
    }
    
    @Override
    public PointerTarget addUnknownOffset() throws InvalidPointerException {
      throw new InvalidPointerException();
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
  public final static class MemoryAddress implements PointerTarget {
  
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
  abstract static class Variable implements PointerTarget {
    
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
    public LocalVariable(String name) {
      super(name);
    }
  }
  
  
  /**
   * A simple global variable.
   */
  public final static class GlobalVariable extends Variable {
    public GlobalVariable(String name) {
      super(name);
    }
  }
  
  public void addNewGlobalPointer(String name, Pointer p);

  public void addNewLocalPointer(String name, Pointer p);

  public Pointer getPointer(String name);

  public Pointer getHeapPointer(MemoryAddress memAddress);

  public void writeOnHeap(MemoryAddress memAddress, Pointer p);

  public Map<String, Pointer> getGlobalPointers();

  public Map<String, Pointer> getLocalPointers();

  public PointerTarget malloc(int length) throws InvalidPointerException;
  
  public PointerTarget malloc() throws InvalidPointerException;

  public void free(Pointer p) throws InvalidPointerException;
  
  public void free(MemoryAddress mem) throws InvalidPointerException;
  
  public void free(MemoryRegion mem) throws InvalidPointerException;

  public Collection<MemoryRegion> checkMemoryLeak();
}