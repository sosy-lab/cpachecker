package cpa.pointeranalysis;

import static cpa.pointeranalysis.Memory.INVALID_POINTER;
import static cpa.pointeranalysis.Memory.NULL_POINTER;
import static cpa.pointeranalysis.Memory.UNKNOWN_POINTER;

import java.util.Collection;
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

  private void join(Pointer p) {
    assert p != null;
    // this adds all targets from p to this pointer
    targets.addAll(p.targets);
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
  
  private void addOffset(int shift, boolean keepOldTargets, Memory memory) {
    if (!hasSizeOfTarget()) {
      addUnknownOffset(keepOldTargets, memory);
      
    } else {
      Set<PointerTarget> newTargets = new HashSet<PointerTarget>();
      int byteShift = shift * sizeOfTarget;
      
      for (PointerTarget target : targets) {
        try {
          newTargets.add(target.addOffset(byteShift));
          
        } catch (InvalidPointerException e) {
          PointerAnalysisTransferRelation.addWarning(e.getMessage(),
              memory.getCurrentEdge(), target.toString());
          
          newTargets.add(INVALID_POINTER);
        }
      }
      
      if (keepOldTargets) {
        targets.addAll(newTargets);
      } else {
        targets = newTargets;
      }
    }
  }

  private void addUnknownOffset(boolean keepOldTargets, Memory memory) {
    Set<PointerTarget> newTargets = new HashSet<PointerTarget>();
    
    for (PointerTarget target : targets) {
      try {
        newTargets.add(target.addUnknownOffset());
      } catch (InvalidPointerException e) {
        PointerAnalysisTransferRelation.addWarning(e.getMessage(),
            memory.getCurrentEdge(), target.toString());
        
        newTargets.add(INVALID_POINTER);
      }
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

  public static interface PointerOperation {

    public void doOperation(Memory memory, Pointer pointer, boolean keepOldTargets);
    
  }
  
  public static class AddOffset implements PointerOperation {
    
    private final int shift;
    
    public AddOffset(int shift) {
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

  public static class Assign implements PointerOperation {
    
    private final PointerTarget assignValueTarget;
    private final Pointer assignValuePointer;
    
    public Assign(PointerTarget assignValue) {
      this.assignValueTarget  = assignValue;
      this.assignValuePointer = null;
    }
    
    public Assign(Pointer assignValue) {
      this.assignValueTarget  = null;
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
        
        if (!keepOldTargets) {
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
    private final int shift;

    public AddOffsetAndAssign(Pointer assignValue, int shift) {
      this.assignValue = assignValue;
      this.shift = shift;
    }
    
    @Override
    public void doOperation(Memory memory, Pointer pointer, boolean keepOldTargets) {
      if (!keepOldTargets) {
        pointer.targets.clear();
      }
      
      Pointer shiftedAssignValue = assignValue.clone();
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
    public void doOperation(Memory memory, Pointer pointer, boolean keepOldTargets) {
      if (!keepOldTargets) {
        pointer.targets.clear();
      }
      
      Pointer shiftedAssignValue = assignValue.clone();
      shiftedAssignValue.addUnknownOffset(false, memory);
      
      pointer.join(shiftedAssignValue);
    }
  }
    
  public static class DerefAndAssign implements PointerOperation {
    
    private final Pointer assignValue;
 
    public DerefAndAssign(Pointer assignValue) {
      this.assignValue = assignValue;
      
      if (!assignValue.isPointerToPointer()) {
        throw new IllegalArgumentException("Pointers which do not point to other pointers cannot be dereferenced in this analysis!");
      }
      
      if (!assignValue.isDereferencable()) {
        throw new IllegalArgumentException("Unsafe deref of pointer " + assignValue.getLocation() + " = " + assignValue);
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
    
    public AssumeInequality(PointerTarget removeTarget) {
      this.removeTarget = removeTarget;
    }
    
    /**
     * @param keepOldTargets ignored
     */
    @Override
    public void doOperation(Memory memory, Pointer pointer, boolean keepOldTargets) {
      
      pointer.targets.remove(removeTarget);
      if (pointer.getNumberOfTargets() == 0) {
        throw new IllegalStateException("Pointer without target must not exist!");
      }
    }
  }
}