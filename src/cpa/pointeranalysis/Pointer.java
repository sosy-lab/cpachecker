package cpa.pointeranalysis;

import static cpa.pointeranalysis.Memory.INVALID_POINTER;
import static cpa.pointeranalysis.Memory.NULL_POINTER;
import static cpa.pointeranalysis.Memory.UNKNOWN_POINTER;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cpa.pointeranalysis.Memory.InvalidPointerException;
import cpa.pointeranalysis.Memory.MemoryAddress;
import cpa.pointeranalysis.Memory.PointerTarget;
import cpa.pointeranalysis.Memory.Variable;

/**
 * A pointer is a set of possible targets.
 */
public class Pointer {
  
  private int sizeOfTarget = 4; // TODO: set to -1 as soon as we have a type CPA 
  
  private Set<PointerTarget> targets;
  
  private int levelOfIndirection; // how many stars does this pointer have?
  
  public Pointer() {
    this(0);
  }
  
  public Pointer(int levelOfIndirection) {
    this (levelOfIndirection, new HashSet<PointerTarget>());
    
    // if uninitialized, pointer is null
    targets.add(NULL_POINTER);
  }
  
  private Pointer(int levelOfIndirection, Set<PointerTarget> targets) {
    if (levelOfIndirection < 0 || targets == null) {
      throw new IllegalArgumentException();
    }
    
    this.levelOfIndirection = levelOfIndirection;
    this.targets = new HashSet<PointerTarget>(targets);
  }
  
  public void makeAlias(Pointer alias) {
    if (alias == null) {
      throw new IllegalArgumentException();
    }
    // this adds all possible targets from the other pointer to this pointer
    targets.clear();
    targets.addAll(alias.targets);
  }
  
  public void join(Pointer p) {
    if (p == null) {
      throw new IllegalArgumentException();
    }
    // this adds all targets from p to this pointer
    targets.addAll(p.targets);
  }
  
  public boolean isUnsafe() {
    return targets.contains(NULL_POINTER) || targets.contains(INVALID_POINTER);
  }
  
  public boolean isSafe() {
    return !(targets.contains(NULL_POINTER)
             || targets.contains(INVALID_POINTER)
             || targets.contains(UNKNOWN_POINTER));
  }
  
  public boolean isSubsetOf(Pointer other) {
    if (other == null) {
      throw new IllegalArgumentException();
    }
    return (this == other) || other.targets.containsAll(targets);
  }
  
  /**
   * This shifts the targets of the pointer.
   * The shift is given in elements, not in bytes (e.g. if this pointer is an
   * int*, shift==1 will shift 4 bytes). 
   */
  public void addOffset(int shift) throws InvalidPointerException {
    if (sizeOfTarget == 0) {
      addUnknownOffset();
    
    } else {
      Set<PointerTarget> newTargets = new HashSet<PointerTarget>();
      
      for (PointerTarget target : targets) {
        newTargets.add(target.addOffset(shift*sizeOfTarget));
      }
      targets = newTargets;
    }
  }
  
  public void addUnknownOffset() throws InvalidPointerException {
    Set<PointerTarget> newTargets = new HashSet<PointerTarget>();
    
    for (PointerTarget target : targets) {
      newTargets.add(target.addUnknownOffset());
    }
    targets = newTargets;
  }

  public Pointer deref(Memory memory) throws InvalidPointerException {
    if (memory == null) {
      throw new IllegalArgumentException();
    }
    if (levelOfIndirection == 1) {
      throw new InvalidPointerException("The target of this pointer is not a pointer");
    }
    assert targets.size() == 1;
    
    for (PointerTarget target : targets) {
      Pointer p;
      
      if (target instanceof Variable) {
        String varName = ((Variable)target).getVarName();
        p = memory.getPointer(varName);
        if (p == null) {
          // type error
          // (pointers with levelOfIndirection > 1 should always point to other pointers)
          throw new InvalidPointerException("The target of this pointer is not a pointer, but this is a pointer of pointer");
        }
        
      } else if (target instanceof MemoryAddress) {
        p = memory.getHeapPointer((MemoryAddress)target);
        if (p == null) {
          // assume, the heap is full of NULL_POINTERs where nothing has been
          // written
          // as this is a pointer of pointer, this is ok
          p = new Pointer(levelOfIndirection-1);
          memory.writeOnHeap((MemoryAddress)target, p);
        }
        
      } else {
        throw new InvalidPointerException("Pointer to " + target + " cannot be dereferenced");
      }
      
      assert levelOfIndirection - p.levelOfIndirection == 1;
      return p;
    }
    return null;
  }
  
  public void setTarget(PointerTarget target) {
    if (target == null) {
      throw new IllegalArgumentException();
    }
    targets.clear();
    targets.add(target);
  }
  
  /**
   * Checks if the size of the target of the pointer is known. 
   */
  public Set<PointerTarget> getTargets() {
    return Collections.unmodifiableSet(targets);
  }
  
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

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Pointer)) {
      return false;
    }
    return (other != null) && targets.equals(((Pointer)other).targets);
  }
  
  @Override
  public int hashCode() {
    return targets.hashCode();
  }
  
  @Override
  public Pointer clone() {
    return new Pointer(levelOfIndirection, targets);
  }
  
  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < levelOfIndirection; i++) {
      sb.append("*");
    }
    sb.append("(");
    for (PointerTarget target : targets) {
      sb.append(" " + target + " ");
    }
    sb.append(")");
    return sb.toString();
  }
}