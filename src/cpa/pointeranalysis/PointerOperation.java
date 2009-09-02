package cpa.pointeranalysis;

import cpa.pointeranalysis.Memory.InvalidPointerException;
import cpa.pointeranalysis.Memory.MemoryAddress;
import cpa.pointeranalysis.Memory.PointerTarget;

interface PointerOperation<E extends Throwable> {

  public void doOperation(Memory memory, Pointer pointer, boolean keepOldTargets) throws E;
  
  public static class AddOffset implements PointerOperation<InvalidPointerException> {
    
    private final int shift;
    
    public AddOffset(int shift) {
      this.shift = shift;
    }
    
    @Override
    public void doOperation(Memory memory, Pointer pointer, boolean keepOldTargets) throws InvalidPointerException {
      pointer.addOffset(shift, keepOldTargets);      
    }
  }
  
  public static class AddUnknownOffset implements PointerOperation<InvalidPointerException> {

    @Override
    public void doOperation(Memory memory, Pointer pointer, boolean keepOldTargets) throws InvalidPointerException {
      pointer.addUnknownOffset(keepOldTargets);      
    }
  }

  public static class Assign implements PointerOperation<RuntimeException> {
    
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
    public void doOperation(Memory memory, Pointer pointer,
        boolean keepOldTargets) {
      
      if (!keepOldTargets) {
        pointer.removeAllTargets();
      }
      
      if (assignValueTarget != null) {
        pointer.addTarget(assignValueTarget);
      } else {
        pointer.join(assignValuePointer);
        
        if (!keepOldTargets) {
          memory.makeAlias(assignValuePointer.getLocation(), pointer.getLocation());
        }
      }
    }
  }
  
  public static class AddOffsetAndAssign implements PointerOperation<InvalidPointerException> {

    private final Pointer assignValue;
    private final int shift;

    public AddOffsetAndAssign(Pointer assignValue, int shift) {
      this.assignValue = assignValue;
      this.shift = shift;
    }
    
    @Override
    public void doOperation(Memory memory, Pointer pointer, boolean keepOldTargets) throws InvalidPointerException {
      if (!keepOldTargets) {
        pointer.removeAllTargets();
      }
      
      Pointer shiftedAssignValue = assignValue.clone();
      shiftedAssignValue.addOffset(shift, false);
      
      pointer.join(shiftedAssignValue);
    }
  }
    
  public static class AddUnknownOffsetAndAssign implements PointerOperation<InvalidPointerException> {

    private final Pointer assignValue;

    public AddUnknownOffsetAndAssign(Pointer assignValue) {
      this.assignValue = assignValue;
    }
    
    @Override
    public void doOperation(Memory memory, Pointer pointer, boolean keepOldTargets) throws InvalidPointerException {
      if (!keepOldTargets) {
        pointer.removeAllTargets();
      }
      
      Pointer shiftedAssignValue = assignValue.clone();
      shiftedAssignValue.addUnknownOffset(false);
      
      pointer.join(shiftedAssignValue);
    }
  }
    
  public static class DerefAndAssign implements PointerOperation<InvalidPointerException> {
    
    private final Pointer assignValue;

    public DerefAndAssign(Pointer assignValue) {
      this.assignValue = assignValue;
    }
    
    @Override
    public void doOperation(Memory memory, Pointer pointer,
        boolean keepOldTargets) throws InvalidPointerException {
      if (!keepOldTargets) {
        pointer.removeAllTargets();
      }
      
      for (PointerTarget target : assignValue.getTargets()) {
        Pointer actualAssignValue = memory.deref(target, assignValue.getLevelOfIndirection());
        
        if (actualAssignValue != null) {
          pointer.join(actualAssignValue);
          
          if (!keepOldTargets && assignValue.getNumberOfTargets() == 1) {
            memory.makeAlias(actualAssignValue.getLocation(), pointer.getLocation());
          }
        }
      }
    }
  }
  
  public static class MallocAndAssign implements PointerOperation<InvalidPointerException> {

    private MemoryAddress memAddress = null;
    
    @Override
    public void doOperation(Memory memory, Pointer pointer, boolean keepOldTargets) throws InvalidPointerException {
      if (memAddress == null) {
        memAddress = memory.malloc();
      }
      
      if (!keepOldTargets) {
        pointer.removeAllTargets();
      }
      pointer.addTarget(Memory.NULL_POINTER);
      pointer.addTarget(memAddress);
    }
    
    public MemoryAddress getMallocResult() {
      return memAddress;
    }
  }
  
}