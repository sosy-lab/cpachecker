/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.pointeranalysis;

import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import common.Pair;

import cpa.common.interfaces.AbstractElement;

/**
 * This class is the abstraction of the memory of the program (global variables,
 * local variables, heap).
 * 
 * @author Philipp Wendler
 */
public class PointerAnalysisElement implements AbstractElement, Memory {

  private static final char FUNCTION_NAME_SEPARATOR = ':';

  private final HashMap<String, Pointer> globalPointers; // tracks all global pointers
    // contains global non-pointer variables, too (mapped to null)
  
  private final Deque<Pair<String, HashMap<String, Pointer>>> localPointers;
  
  private String currentFunctionName;
  
  private final HashMap<MemoryAddress, Pointer> heap;
  
  private final HashSet<MemoryRegion> mallocs; // list of all malloc'ed memory regions

  private final HashMap<PointerTarget, Set<PointerLocation>> reverseRelation; // reverse mapping from pointer targets to pointer locations
  
  private final HashMap<PointerLocation, Set<PointerLocation>> aliases; // mapping from pointer location to locations of all aliases
    
  // TODO: enforce only single reference to Pointer objects
  
  /*
   * Following possibilities exist:
   * Malloc and Pointer: a malloc with unknown length,
   *        ASTNode contains parameter to malloc
   * Pointer: a pointer variable with unknown type was declared,
   *        ASTNode contains type specifier
   * Pointer, PointerBackup, OffsetNegative: an unknown offset was added to a pointer,
   *        Pointer is the new one, PointerBackup is the old one (before shifting),
   *        and ASTNode is the shift operand   
   */
  
  public PointerAnalysisElement() {
    globalPointers = new HashMap<String, Pointer>();
    localPointers = new LinkedList<Pair<String, HashMap<String, Pointer>>>();
    heap = new HashMap<MemoryAddress, Pointer>();
    mallocs = new HashSet<MemoryRegion>();
    reverseRelation = new HashMap<PointerTarget, Set<PointerLocation>>();
    aliases = new HashMap<PointerLocation, Set<PointerLocation>>();
    callFunction("main()");
  }
  
  private PointerAnalysisElement(final Map<String, Pointer> globalPointers,
                                 final Deque<Pair<String, HashMap<String, Pointer>>> localPointers,
                                 final Map<MemoryAddress, Pointer> heap,
                                 final Set<MemoryRegion> mallocs,
                                 final Map<PointerTarget, Set<PointerLocation>> reverseRelation,
                                 final Map<PointerLocation, Set<PointerLocation>> aliases) {
    this.globalPointers = new HashMap<String, Pointer>();
    this.localPointers = new LinkedList<Pair<String, HashMap<String, Pointer>>>();
    this.heap = new HashMap<MemoryAddress, Pointer>();
    this.mallocs = new HashSet<MemoryRegion>(mallocs);
    this.reverseRelation = new HashMap<PointerTarget, Set<PointerLocation>>();
    this.aliases = new HashMap<PointerLocation, Set<PointerLocation>>();
    
    for (String name : globalPointers.keySet()) {
      Pointer p = globalPointers.get(name);
      if (p != null) {
        this.globalPointers.put(name, p.clone());
      } else {
        this.globalPointers.put(name, null);
      }
    }
    
    for (Pair<String, HashMap<String, Pointer>> stackframe : localPointers) {
      HashMap<String, Pointer> newLocalPointers = new HashMap<String, Pointer>();
      
      for (String name : stackframe.getSecond().keySet()) {
        newLocalPointers.put(name, stackframe.getSecond().get(name).clone());
      }
      
      this.localPointers.add(new Pair<String, HashMap<String, Pointer>>(stackframe.getFirst(), newLocalPointers));
    }
    
    for (MemoryAddress memAddress : heap.keySet()) {
      this.heap.put(memAddress, heap.get(memAddress).clone());
    }
    
    for (PointerTarget target : reverseRelation.keySet()) {
      this.reverseRelation.put(target, new HashSet<PointerLocation>(reverseRelation.get(target)));
    }
    
    for (Set<PointerLocation> aliasSet : aliases.values()) {
      Set<PointerLocation> newAliasSet = new HashSet<PointerLocation>(aliasSet);
      for (PointerLocation location : newAliasSet) {
        this.aliases.put(location, newAliasSet);
      }
    }
  }
  
  private void registerPointer(Pointer p, PointerLocation loc) {
    p.setLocation(loc);
    for (PointerTarget t : p.getTargets()) {
      addReverseRelation(t, loc);
    } 
  }
  
  @Override
  public void addNewGlobalPointer(String name, Pointer p) {
    globalPointers.put(name, p);
    registerPointer(p, new GlobalVariable(name));
  }

  @Override
  public void addNewLocalPointer(String name, Pointer p) {
    localPointers.peekLast().getSecond().put(name, p);
    registerPointer(p, new LocalVariable(getCurrentFunctionName(), name));
  }
  
  @Override
  public Pointer getPointer(String name) {
    Pointer result = globalPointers.get(name);
    if (result == null) {
      result = localPointers.peekLast().getSecond().get(name);
    }
    return result;
  }
  
  public Pointer getPointer(PointerLocation location) {
    if (location instanceof MemoryAddress) {
      return heap.get(location);
    
    } else if (location instanceof LocalVariable) {
      LocalVariable var = (LocalVariable)location;
      if (!var.getFunctionName().equals(getCurrentFunctionName())) {
        throw new IllegalArgumentException("Variabel out of scope!");
      }
      return localPointers.peekLast().getSecond().get(var.getVarName());
      
    } else if (location instanceof GlobalVariable) {
      return globalPointers.get(((GlobalVariable)location).getVarName());
    
    } else {
      throw new IllegalArgumentException("Unknown type of PointerLocation");
    }
  }
  
  @Override
  public Pointer getHeapPointer(MemoryAddress memAddress) {
    return heap.get(memAddress);
  }
  
  @Override
  public void writeOnHeap(MemoryAddress memAddress, Pointer p) {
    heap.put(memAddress, p);
    registerPointer(p, memAddress);
  }
   
  
  private void addReverseRelation(PointerTarget target, PointerLocation location) {
    if (target == INVALID_POINTER || target == UNKNOWN_POINTER) {
      return;
    }
    
    Set<PointerLocation> locs = reverseRelation.get(target);
    if (locs == null) {
      locs = new HashSet<PointerLocation>();
      reverseRelation.put(target, locs);
    }
    
    locs.add(location);
  }
  
  @Override
  public Set<PointerLocation> getReversePointers(PointerTarget target) {
    Set<PointerLocation> locs = reverseRelation.get(target);
    if (locs == null) {
      throw new IllegalStateException("Target " + target + " has no set of locations");
    }
    
    return locs;
  }
  
  private void removeReverseRelation(PointerTarget target, PointerLocation location) {
    if (target == INVALID_POINTER || target == UNKNOWN_POINTER) {
      return;
    }
    Set<PointerLocation> locs = reverseRelation.get(target);
    if (locs == null || !locs.contains(location)) {
      throw new IllegalStateException("Trying to remove location " + location + " from target " + target + ", but it's not there");
    }
    
    locs.remove(location);
  }
  
  private void removeAllReverseRelations(PointerLocation location) {
    for (PointerTarget target : getPointer(location).getTargets()) {
      removeReverseRelation(target, location);
    }
  }
  
  @Override
  public void makeAlias(PointerLocation firstPointer, PointerLocation secondPointer) {
    Set<PointerLocation> newAliases = aliases.get(firstPointer);
    if (newAliases == null) {
      newAliases = new HashSet<PointerLocation>();
      newAliases.add(firstPointer);
      aliases.put(firstPointer, newAliases);
    }
    
    Set<PointerLocation> oldAliases = aliases.get(secondPointer);
    if (oldAliases != null) {
      oldAliases.remove(secondPointer);
    }
    
    newAliases.add(secondPointer);
    aliases.put(secondPointer, newAliases);
  }
  
  /**
   * Merge the alias lists of both pointers. This does not change the actual list of targets.
   * 
   * @param firstPointer
   * @param secondPointer
   */
  private void mergeAliases(PointerLocation firstPointer, PointerLocation secondPointer) {
    Set<PointerLocation> firstAliases = aliases.get(firstPointer);
    
    if (firstAliases == null || firstAliases.size() == 1) {
      makeAlias(secondPointer, firstPointer);

    } else {
      Set<PointerLocation> secondAliases = aliases.get(secondPointer);
      
      if (firstAliases != secondAliases) {
        if (secondAliases == null || secondAliases.size() == 1) {
          makeAlias(firstPointer, secondPointer);
        
        } else {
          // both pointers have aliases
          for (PointerLocation p : secondAliases) {
            firstAliases.add(p);
            aliases.put(p, firstAliases);
          }
          secondAliases.clear(); // just for safety, there should be no references to this object anymore
        }
      }
    }
  }
  
  private void removeAllAliases(PointerLocation pointer) {
    Set<PointerLocation> pointerAliases = aliases.get(pointer);
    if (pointerAliases != null && pointerAliases.size() > 1) {
      pointerAliases.remove(pointer);
      aliases.remove(pointer);
    }
  }
  
  @Override
  public Set<PointerLocation> getAliases(PointerLocation pointer) {
    Set<PointerLocation> pointerAliases = aliases.get(pointer);

    if (pointerAliases == null) {
      pointerAliases = new HashSet<PointerLocation>();
      pointerAliases.add(pointer);
      aliases.put(pointer, pointerAliases);
    
    } else {
      assert pointerAliases.contains(pointer);
    } 

    if (getPointer(pointer).getNumberOfTargets() == 1) {
      // TODO: there could be other pointers with the same target -> they are aliases, too
    }
    
    return Collections.unmodifiableSet(pointerAliases);
  }
  
  @Override
  public Set<PointerLocation> getAliases(Pointer pointer) {
    return getAliases(pointer.getLocation());
  }
  
  @Override
  public boolean areAliases(Pointer p1, Pointer p2) {
    if (aliases.get(p1.getLocation()).contains(p2.getLocation())) {
      return true;
    }
    
    // check if both pointer have the same single target
    if (p1.getNumberOfTargets() == 1 && p2.getNumberOfTargets() == 1) {
      PointerTarget target = p1.getFirstTarget();
      
      if (target.equals(p2.getFirstTarget())
          && target != INVALID_POINTER && target != UNKNOWN_POINTER) {
        
        mergeAliases(p1.getLocation(), p2.getLocation());
        return true;
      }
    }
    
    return false;
  }
  
  private void findAndMergePossibleAliases(Pointer p) {
    if (p.getNumberOfTargets() != 1) {
      // not possible to decide whether another pointer is a true alias
      return;
    }
    
    PointerLocation loc = p.getLocation();
    PointerTarget target = p.getTargets().toArray(new PointerTarget[1])[0];
    if (target == INVALID_POINTER || target == UNKNOWN_POINTER) {
      // no tracking of aliases for these values
      return;
    }
    
    Set<PointerLocation> aliasesOfP = aliases.get(loc);
    
    for (PointerLocation candidateLoc : reverseRelation.get(target)) {
      if (!aliasesOfP.contains(candidateLoc)) {
        Pointer candidatePointer = getPointer(candidateLoc);
        
        if (candidatePointer.getNumberOfTargets() == 1) {
          assert candidatePointer.getTargets().equals(p.getTargets());
          
          mergeAliases(loc, candidateLoc);
          aliasesOfP = aliases.get(loc); // this may be a different object now
        }
      }
    }
  }
      
  @Override
  public MemoryAddress malloc() throws InvalidPointerException {
    MemoryRegion mem = new MemoryRegion();
    mallocs.add(mem);
    return new MemoryAddress(mem, 0);
  }
  
  @Override
  public void free(Pointer p) throws InvalidPointerException {
    // TODO: What if there are multiple targets?
    for (PointerTarget target : p.getTargets()) {
      if (target instanceof MemoryAddress) {
        free((MemoryAddress)target);
      } else {
        throw new InvalidPointerException("Cannot free pointer to " + target.getClass().getSimpleName());
      }
    }
    //p.assign(INVALID_POINTER);
  }
  
  @Override
  public void free(MemoryAddress mem) throws InvalidPointerException {
    if (mem.getOffset() != 0) {
      throw new InvalidPointerException("Cannot free pointer with offset != 0!");
    }
    free(mem.getRegion());
  }
  
  @Override
  public void free(MemoryRegion mem) throws InvalidPointerException {
    if (!mallocs.contains(mem)) {
      throw new InvalidPointerException("Double free!");
    }
    mallocs.remove(mem);
    // TODO: assign INVALID_POINTER to all pointers pointing to this region?
  }
  
  @Override
  public Pointer deref(PointerTarget target, int levelOfIndirection) throws InvalidPointerException {
    Pointer result;
    
    if (target instanceof Variable) {
      String varName = ((Variable)target).getVarName();
      result = getPointer(varName);
      if (result == null) {
        // type error
        // (pointers with levelOfIndirection > 1 should always point to other pointers)
        throw new InvalidPointerException("The target of this pointer is not a pointer, but this is a pointer of pointer");
      }
      
    } else if (target instanceof MemoryAddress) {
      result = getHeapPointer((MemoryAddress)target);
      if (result == null) {
      // assume, the heap is full of NULL_POINTERs where nothing has been
      // written
      // as this is a pointer of pointer, this is ok
      result = new Pointer(levelOfIndirection-1);
      writeOnHeap((MemoryAddress)target, result);
    }
    
    } else if (target == NULL_POINTER || target == INVALID_POINTER) {
      // warning is printed elsewhere
      result = null;
    
    } else if (target == UNKNOWN_POINTER) {
      result = null;
    
    } else {
      throw new InvalidPointerException("Pointer to " + target + " cannot be dereferenced");
    }
    return result;
  }
  
  public <E extends Exception> void pointerOp(PointerOperation<E> op, Pointer pointer) throws E {
    pointerOpNoDereference(op, pointer, false);
  }
  
  public <E extends Exception> void pointerOp(PointerOperation<E> op, Pointer pointer, boolean dereferenceFirst) throws E, InvalidPointerException {
    if (dereferenceFirst) {
      boolean keepOldTargets = (pointer.getNumberOfTargets() != 1);

      for (PointerTarget target : pointer.getTargets()) {
        Pointer actualPointer = deref(target, pointer.getLevelOfIndirection());
        
        if (actualPointer != null) {
          pointerOpNoDereference(op, actualPointer, keepOldTargets);
        } else {
          // was not able to dereference target, have to ignore operation
        }
      }
      
    } else {
      pointerOpNoDereference(op, pointer, false);
    }
  }

  private <E extends Exception> void pointerOpNoDereference(PointerOperation<E> op, Pointer pointer,
      boolean keepOldTargets) throws E {
    PointerLocation location = pointer.getLocation();
    
    removeAllAliases(location);
    removeAllReverseRelations(location);

    op.doOperation(this, pointer, keepOldTargets);
    
    for (PointerTarget target : pointer.getTargets()) {
      addReverseRelation(target, location);
    }
    findAndMergePossibleAliases(pointer);
  }
  
  /*public MemoryAddress pointerOpMalloc(Pointer targetPointer, boolean dereferenceFirst) throws InvalidPointerException {
    
    MemoryRegion mem = new MemoryRegion();
    mallocs.add(mem);
    MemoryAddress memAddress = new MemoryAddress(mem, 0);
    
    if (dereferenceFirst) {
      for (PointerTarget target : targetPointer.getTargets()) {
        Pointer actualTargetPointer = deref(target, targetPointer.getLevelOfIndirection());
        
        if (actualTargetPointer != null) {
          PointerLocation actualTargetLocation = actualTargetPointer.getLocation();
          removeAllAliases(actualTargetLocation);

          if (targetPointer.getNumberOfTargets() == 1) {
            // in this case, the actualTargetPointer is always overwritten
            removeAllReverseRelations(actualTargetLocation);
            actualTargetPointer.removeAllTargets();     
          }            

          actualTargetPointer.addTarget(NULL_POINTER);
          actualTargetPointer.addTarget(memAddress);

          addReverseRelation(NULL_POINTER, actualTargetLocation);
          addReverseRelation(memAddress, actualTargetLocation);

        } else {
          // p == null means the target was something like NULL or UNKNOWN
          // we cannot do more than ignore (warning will be printed elsewhere)
        }
      }
      
    } else {
      PointerLocation targetLoc = targetPointer.getLocation();
      removeAllAliases(targetLoc);
      removeAllReverseRelations(targetLoc);
      targetPointer.removeAllTargets();     
      
      targetPointer.addTarget(NULL_POINTER);
      targetPointer.addTarget(memAddress);

      addReverseRelation(NULL_POINTER, targetLoc);
      addReverseRelation(memAddress, targetLoc);
    }
    
    return memAddress;
  }*/
  
  /*public void pointerOpAddOffset(Pointer pointer, int shift, boolean dereferenceFirst) throws InvalidPointerException {
    if (dereferenceFirst) {
      boolean keepOldTargets = (pointer.getNumberOfTargets() != 1);

      for (PointerTarget target : pointer.getTargets()) {
        Pointer actualPointer = deref(target, pointer.getLevelOfIndirection());
        
        if (actualPointer != null) {
          pointerOpAddOffsetNoDereference(actualPointer, shift, keepOldTargets);
        }
      }
            
    } else {
      pointerOpAddOffsetNoDereference(pointer, shift, false);
    }
  }
  
  private void pointerOpAddOffsetNoDereference(Pointer pointer, int shift, boolean keepOldTargets) throws InvalidPointerException {
    PointerLocation location = pointer.getLocation();
    removeAllAliases(location);
    removeAllReverseRelations(location);
    
    pointer.addOffset(shift, keepOldTargets);
    
    for (PointerTarget target : pointer.getTargets()) {
      addReverseRelation(target, location);
    }
    findAndMergePossibleAliases(pointer);
  }
  
  public void pointerOpAddUnknownOffset(Pointer pointer, boolean dereferenceFirst) throws InvalidPointerException {
    pointerOpAddOffset(pointer, Integer.MAX_VALUE, dereferenceFirst);
  }*/
  
  /**
   * This method does everything necessary to handle the code
   * leftPointer = rightPointer;
   * where both variables are pointer.
   * 
   * @param leftPointer The target of the assignment.
   * @param rightPointer The source of the assignment.
   */
  /*public void pointerOpAssign(Pointer leftPointer, Pointer rightPointer) {
    PointerLocation leftLocation = leftPointer.getLocation();
    
    removeAllAliases(leftLocation);
    removeAllReverseRelations(leftLocation);
    leftPointer.removeAllTargets();
    
    leftPointer.assign(rightPointer);
    makeAlias(rightPointer.getLocation(), leftLocation);
    
    for (PointerTarget target : leftPointer.getTargets()) {
      addReverseRelation(target, leftLocation);
    }
  }
  
  public void pointerOpAssign(Pointer leftPointer, PointerTarget rightTarget) {
    removeAllAliases(leftPointer.getLocation());
    pointerOpAssignIgnoreAliases(leftPointer, rightTarget);
    findAndMergePossibleAliases(leftPointer);
  }*/
  
  private void pointerOpAssignIgnoreAliases(Pointer leftPointer, PointerTarget rightTarget) {
    PointerLocation leftLocation = leftPointer.getLocation();
    removeAllReverseRelations(leftLocation);
    
    leftPointer.assign(rightTarget); // this removes all other targets
    addReverseRelation(rightTarget, leftLocation);
  }
  
  private void pointerOpRemoveTargetFromAllAliases(Pointer pointer, PointerTarget target) {
    for (PointerLocation aliasLoc : getAliases(pointer)) {
      getPointer(aliasLoc).removeTarget(target);
      removeReverseRelation(target, aliasLoc);
    }
  }
  
  public void pointerOpAssumeEquality(Pointer firstPointer, Pointer secondPointer) {
    if (areAliases(firstPointer, secondPointer)) {
      return;
    }
    
    if (firstPointer.getTargets().equals(secondPointer.getTargets())) {
      mergeAliases(firstPointer.getLocation(), secondPointer.getLocation());
      return;
    }
    
    PointerTarget[] tempArray = new PointerTarget[firstPointer.getNumberOfTargets()];
    // remove impossible targets from firstPointer
    for (PointerTarget possibleTarget : firstPointer.getTargets().toArray(tempArray)) {
      if (!secondPointer.contains(possibleTarget)) {
        pointerOpRemoveTargetFromAllAliases(firstPointer, possibleTarget);
      }
    }
    // remove impossible targets from secondPointer    
    for (PointerTarget possibleTarget : secondPointer.getTargets().toArray(tempArray)) {
      if (!secondPointer.contains(possibleTarget)) {
        pointerOpRemoveTargetFromAllAliases(firstPointer, possibleTarget);
      }
    }
    // now first and second pointer have the same set of targets
    
    mergeAliases(firstPointer.getLocation(), secondPointer.getLocation());
  }
  
  public void pointerOpAssumeEquality(Pointer pointer, PointerTarget target) {
    if (pointer.getNumberOfTargets() > 1 || !pointer.contains(target)) {
      for (PointerLocation aliasLoc : getAliases(pointer)) {
        pointerOpAssignIgnoreAliases(getPointer(aliasLoc), target); // this removes all other targets
      }
    }
    findAndMergePossibleAliases(pointer);
  }
  
  public void pointerOpAssumeInequality(Pointer firstPointer, Pointer secondPointer) {
    if (firstPointer.getNumberOfTargets() == 1) {
      pointerOpAssumeInequality(secondPointer, firstPointer.getFirstTarget());
    
    } else if (secondPointer.getNumberOfTargets() == 1) {
      pointerOpAssumeInequality(firstPointer, secondPointer.getFirstTarget());
    
    } else {
      // can't do anything
    }
  }

  public void pointerOpAssumeInequality(Pointer pointer, PointerTarget target) {
    if (target != INVALID_POINTER && target != UNKNOWN_POINTER) { 
      
      if (pointer.contains(target)) {
        for (PointerLocation aliasLoc : getAliases(pointer)) {
          getPointer(aliasLoc).removeTarget(target);
          removeReverseRelation(target, aliasLoc);
        }
      }
      findAndMergePossibleAliases(pointer);
    }
  }
  
  @Override
  public Set<MemoryRegion> checkMemoryLeak() {
    Set<MemoryRegion> unmarkedRegions = new HashSet<MemoryRegion>(mallocs);
    
    for (Pointer p: globalPointers.values()) {
      if (p != null) {
        checkMemoryLeak(unmarkedRegions, p);
      }
    }
    
    for (Pair<String, HashMap<String, Pointer>> stackframe : localPointers) {
      for (Pointer p: stackframe.getSecond().values()) {
        checkMemoryLeak(unmarkedRegions, p);
      }
    }
    
    return unmarkedRegions;
  }
  
  private void checkMemoryLeak(Set<MemoryRegion> unmarkedRegions, Pointer p) {
    for (PointerTarget target : p.getTargets()) {
      if (target instanceof MemoryAddress) {
        MemoryRegion memRegion = ((MemoryAddress)target).getRegion();
        boolean unmarked = unmarkedRegions.contains(memRegion);
        if (unmarked) {
          unmarkedRegions.remove(memRegion);
          
          // recursively mark on heap
          for (MemoryAddress heapMem : heap.keySet()) {
            if (unmarkedRegions.contains(heapMem.getRegion())
                && heapMem.getRegion() == memRegion) {
              
              checkMemoryLeak(unmarkedRegions, heap.get(heapMem));
            }
          }
          
        }
      }
    }
  }
  
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof PointerAnalysisElement)) {
      return false;
    }
    
    PointerAnalysisElement otherElement = (PointerAnalysisElement)other;
    
    return globalPointers.equals(otherElement.globalPointers)
        && localPointers.equals(otherElement.localPointers)
        && reverseRelation.equals(otherElement.reverseRelation)
        && aliases.equals(otherElement.aliases);
  }
  
  public void callFunction(String functionName) {
    localPointers.addLast(new Pair<String, HashMap<String, Pointer>>(functionName, new HashMap<String, Pointer>()));
    currentFunctionName = currentFunctionName + FUNCTION_NAME_SEPARATOR + functionName;
  }
  
  public void returnFromFunction() {
    localPointers.pollLast();
    
    Iterator<PointerLocation> aliasIt = aliases.keySet().iterator();
    while (aliasIt.hasNext()) {
      PointerLocation loc = aliasIt.next();
      
      if (loc instanceof LocalVariable) {
      
        if (getCurrentFunctionName().equals(((LocalVariable)loc).getFunctionName())) {
          // a local pointer is aliased to another pointer
          aliases.get(loc).remove(loc);
          aliasIt.remove(); // instead of aliases.remove(loc);
        }
      }
    }
    
    Iterator<PointerTarget> reverseIt = reverseRelation.keySet().iterator();
    while (reverseIt.hasNext()) {
      PointerTarget target = reverseIt.next();
      
      Set<PointerLocation> locations = reverseRelation.get(target);
      for (PointerLocation loc : locations) {

        if (loc instanceof LocalVariable) {
          if (getCurrentFunctionName().equals(((LocalVariable)loc).getFunctionName())) {
            // a local pointer points to this target
            locations.remove(loc);
          }
        }
      }
      
      if (target instanceof LocalVariable) {
        if (getCurrentFunctionName().equals(((LocalVariable)target).getFunctionName())) {
          // this target is a local variable, there may be no remaining reference to this target!
          if (!locations.isEmpty()) {
            // all local locations have already been removed, there has to be a global one!
            // TODO report warning about this
            Pointer p = getPointer(((LocalVariable)target).getVarName());
            p.removeTarget(target);
            p.addTarget(INVALID_POINTER);
            // No need to handle aliases here, as there will be one iteration of the while loop 
            // for them as well
          }
          reverseIt.remove();
        }
      }
    }
    
    currentFunctionName = currentFunctionName.substring(0, currentFunctionName.lastIndexOf(FUNCTION_NAME_SEPARATOR));
  }
  
  public String getCurrentFunctionName() {
    return currentFunctionName;
  }
  
  @Override
  public Map<String, Pointer> getGlobalPointers() {
    return globalPointers;
  }
  
  @Override
  public Map<String, Pointer> getLocalPointers() {
    return localPointers.peek().getSecond();
  }
  
  @Override
  public int hashCode() {
    return localPointers.hashCode();
  }
  
  @Override
  public PointerAnalysisElement clone() {
    return new PointerAnalysisElement(globalPointers, localPointers, heap, mallocs, reverseRelation, aliases); 
  }
  
  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[<global:");
    for (String var : globalPointers.keySet()) {
      sb.append(" " + var + "=" + globalPointers.get(var) + " ");
    }
    for (Pair<String, HashMap<String, Pointer>> stackframe: localPointers) {
      sb.append("> <" + stackframe.getFirst() + ":");
      Map<String, Pointer> pointers = stackframe.getSecond();
      for (String var : pointers.keySet()) {
        sb.append(" " + var + "=" + pointers.get(var) + " ");
      }
    }
    sb.append("> <heap:");
    for (MemoryAddress memAddress : heap.keySet()) {
      sb.append(" " + memAddress + "=" + heap.get(memAddress));
    }
    sb.append(">]");
    return sb.toString();
  }
}