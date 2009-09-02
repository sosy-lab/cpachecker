package cpa.pointeranalysis;

import static cpa.pointeranalysis.Memory.INVALID_POINTER;
import static cpa.pointeranalysis.Memory.NULL_POINTER;
import static cpa.pointeranalysis.Memory.UNKNOWN_POINTER;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cpa.pointeranalysis.Memory.InvalidPointerException;
import cpa.pointeranalysis.Memory.MemoryAddress;
import cpa.pointeranalysis.Memory.PointerLocation;
import cpa.pointeranalysis.Memory.PointerTarget;
import cpa.pointeranalysis.Memory.Variable;

/**
 * A pointer is a set of possible targets.
 */
public class Pointer {
  
  private int sizeOfTarget; 
  
  private Set<PointerTarget> targets;
  
  private int levelOfIndirection; // how many stars does this pointer have?
  
  private PointerLocation location;
  
  public Pointer() {
    this(0);
  }

  public Pointer(PointerTarget target) {
    this();
    assign(target);
  }
  
  public Pointer(int levelOfIndirection) {
    this (-1, levelOfIndirection, new HashSet<PointerTarget>(), null);
    
    // if uninitialized, pointer is null
    targets.add(NULL_POINTER);
  }
  
  private Pointer(int sizeOfTarget, int levelOfIndirection, Set<PointerTarget> targets, PointerLocation location) {
    this.sizeOfTarget = sizeOfTarget;
    this.levelOfIndirection = levelOfIndirection;
    this.targets = new HashSet<PointerTarget>(targets);
    this.location = location;
  }
  
  public void assign(Pointer rightHandSide) {
    assert rightHandSide != null;

    // this adds all possible targets from the other pointer to this pointer
    targets.clear();
    targets.addAll(rightHandSide.targets);
  }

  /**
   * Assign a single target to the pointer and remove all others. This method
   * does not change the list of aliases of this pointer, the caller has to
   * ensure that this list is still correct.
   * 
   * @param target
   */
  public void assign(PointerTarget target) {
    assert target != null;
    targets.clear();
    targets.add(target);
  }
  

  public void join(Pointer p) {
    assert p != null;
    // this adds all targets from p to this pointer
    targets.addAll(p.targets);
  }
  
  public void addTarget(PointerTarget target) {
    assert target != null;
    targets.add(target);
  }
  
  
  public void removeTarget(PointerTarget target) {
    assert target != null;
    targets.remove(target);
  }
  
  public void removeAllTargets(Pointer other) {
    assert other != null;
    targets.removeAll(other.targets);
  }
  
  public void removeAllTargets() {
    targets.clear();
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
             || targets.contains(INVALID_POINTER)
             || targets.contains(UNKNOWN_POINTER));
  }
  
  public boolean isSubsetOf(Pointer other) {
    assert other != null;
    return (this == other) || other.targets.containsAll(targets);
  }

  public boolean isDifferentFrom(Pointer other) {
    assert other != null;
    return !this.isSubsetOf(other)
        && !other.isSubsetOf(this)
        && !targets.contains(INVALID_POINTER)
        && !targets.contains(UNKNOWN_POINTER)
        && !other.targets.contains(INVALID_POINTER)
        && !other.targets.contains(UNKNOWN_POINTER);
  }
  
  public boolean contains(PointerTarget target) {
    assert target != null;
    return targets.contains(target);
  }
  
  public void addOffset(int shift, boolean keepOldTargets) throws InvalidPointerException {
    if (!hasSizeOfTarget()) {
      addUnknownOffset(keepOldTargets);
      
    } else {
      Set<PointerTarget> newTargets = new HashSet<PointerTarget>();
      
      for (PointerTarget target : targets) {
        newTargets.add(target.addOffset(shift*sizeOfTarget));
      }
      
      if (keepOldTargets) {
        targets.addAll(newTargets);
      } else {
        targets = newTargets;
      }
    }
  }

  public void addUnknownOffset(boolean keepOldTargets) throws InvalidPointerException {
    Set<PointerTarget> newTargets = new HashSet<PointerTarget>();
    
    for (PointerTarget target : targets) {
      newTargets.add(target.addUnknownOffset());
    }
    
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

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Pointer)) {
      return false;
    }
    return (other != null)
        && location.equals(((Pointer)other).location)
        && targets.equals(((Pointer)other).targets);
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
    StringBuffer sb = new StringBuffer();
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

}