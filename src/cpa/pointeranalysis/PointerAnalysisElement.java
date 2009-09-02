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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import cfa.objectmodel.CFAEdge;

import common.Pair;

import cpa.common.interfaces.AbstractElement;
import cpa.pointeranalysis.Pointer.PointerOperation;
import cpa.pointeranalysis.PointerAnalysisDomain.IPointerAnalysisElement;

/**
 * This class is the abstraction of the memory of the program (global variables,
 * local variables, heap).
 * 
 * @author Philipp Wendler
 */
public class PointerAnalysisElement implements AbstractElement, Memory, IPointerAnalysisElement {

  private static final char FUNCTION_NAME_SEPARATOR = ':';

  private CFAEdge currentEdge = null;
  
  private final HashMap<String, Pointer> globalPointers; // tracks all global pointers
    // contains global non-pointer variables, too (mapped to null)
  
  private final Deque<Pair<String, HashMap<String, Pointer>>> localPointers;
  
  private final Map<String, Map<String, Pointer>> allLocalPointers;
  
  private String currentFunctionName = "";
  
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
    allLocalPointers = new HashMap<String, Map<String, Pointer>>();
    heap = new HashMap<MemoryAddress, Pointer>();
    mallocs = new HashSet<MemoryRegion>();
    reverseRelation = new HashMap<PointerTarget, Set<PointerLocation>>();
    aliases = new HashMap<PointerLocation, Set<PointerLocation>>();
    callFunction("main");
  }
  
  private PointerAnalysisElement(final Map<String, Pointer> globalPointers,
                                 final Deque<Pair<String, HashMap<String, Pointer>>> localPointers,
                                 final Map<MemoryAddress, Pointer> heap,
                                 final Set<MemoryRegion> mallocs,
                                 final Map<PointerTarget, Set<PointerLocation>> reverseRelation,
                                 final Map<PointerLocation, Set<PointerLocation>> aliases,
                                 final String currentFunctionName) {
    this.globalPointers = new HashMap<String, Pointer>();
    this.localPointers = new LinkedList<Pair<String, HashMap<String, Pointer>>>();
    this.allLocalPointers = new HashMap<String, Map<String, Pointer>>();
    this.heap = new HashMap<MemoryAddress, Pointer>();
    this.mallocs = new HashSet<MemoryRegion>(mallocs);
    this.reverseRelation = new HashMap<PointerTarget, Set<PointerLocation>>();
    this.aliases = new HashMap<PointerLocation, Set<PointerLocation>>();
    this.currentFunctionName = currentFunctionName;
    
    for (String name : globalPointers.keySet()) {
      Pointer p = globalPointers.get(name);
      if (p != null) {
        this.globalPointers.put(name, p.clone());
      } else {
        this.globalPointers.put(name, null);
      }
    }
    
    String function = "";
    for (Pair<String, HashMap<String, Pointer>> stackframe : localPointers) {
      HashMap<String, Pointer> newLocalPointers = new HashMap<String, Pointer>();
      
      for (String name : stackframe.getSecond().keySet()) {
        newLocalPointers.put(name, stackframe.getSecond().get(name).clone());
      }
      
      this.localPointers.add(new Pair<String, HashMap<String, Pointer>>(stackframe.getFirst(), newLocalPointers));
      
      if (function.equals("")) {
        function = stackframe.getFirst();
      } else {
        function = function + FUNCTION_NAME_SEPARATOR + stackframe.getFirst();
      }
      allLocalPointers.put(function, newLocalPointers);
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
    
    sanityCheck();
  }
  
  private void sanityCheck() throws IllegalStateException {
    for (Set<PointerLocation> aliasSet : aliases.values()) {
      for (PointerLocation loc : aliasSet) {
       if (aliases.get(loc) != aliasSet) {
         throw new IllegalStateException("Aliases wrong");
       }
      }
    }
    
    for (PointerTarget target : reverseRelation.keySet()) {
      for (PointerLocation loc : reverseRelation.get(target)) {
        if (!getPointer(loc).contains(target)) {
          throw new IllegalStateException("Reverse relation " + loc + " <- "
                                        + target + " without forward relation!");
        }
      }
    }
    
    for (Pointer p : globalPointers.values()) {
      for (PointerTarget target : p.getTargets()) {
        if (target != UNKNOWN_POINTER && target != INVALID_POINTER) {
          if (reverseRelation.get(target) == null) {
            throw new IllegalStateException("Target without reverse relations");
          }
          if (!reverseRelation.get(target).contains(p.getLocation())) {
            throw new IllegalStateException("Forward relation " + p.getLocation()
                            + " -> " + target + " without reverse relation!");
          }
        }
      }
      
      PointerLocation loc = p.getLocation();
      if (!(loc instanceof GlobalVariable) && !(getPointer(loc) == p) ) {
        throw new IllegalStateException("Pointer in invalid location");
      }
    }
    for (Map<String, Pointer> pointers : allLocalPointers.values()) {
      for (Pointer p : pointers.values()) {
        for (PointerTarget target : p.getTargets()) {
          if (target != UNKNOWN_POINTER && target != INVALID_POINTER) {
            if (reverseRelation.get(target) == null) {
              throw new IllegalStateException("Target " + target + " without reverse relations!");
            }
            if (!reverseRelation.get(target).contains(p.getLocation())) {
              throw new IllegalStateException("Forward relation " + p.getLocation()
                            + " -> " + target + " without reverse relation!");
            }
          }
        }
        PointerLocation loc = p.getLocation();
        if (!(loc instanceof LocalVariable) && !(getPointer(loc) == p) ) {
          throw new IllegalStateException("Pointer in invalid location!");
        }
      }
    }
    for (Pointer p : heap.values()) {
      for (PointerTarget target : p.getTargets()) {
        if (target != UNKNOWN_POINTER && target != INVALID_POINTER) {
          if (reverseRelation.get(target) == null) {
            throw new IllegalStateException("Target without reverse relations");
          }
          if (!reverseRelation.get(target).contains(p.getLocation())) {
            throw new IllegalStateException("Forward relation " + p.getLocation()
                            + " -> " + target + " without reverse relation!");
          }
        }
      }
      
      PointerLocation loc = p.getLocation();
      if (!(loc instanceof MemoryAddress) && !(getPointer(loc) == p) ) {
        throw new IllegalStateException("Pointer in invalid location!");
      }
    }
  }
  
  private void registerPointer(Pointer p, PointerLocation loc) {
    assert p != null && loc != null;
    p.setLocation(loc);
    addAllReverseRelations(p);
    findAndMergePossibleAliases(p);
  }
  
  @Override
  public void addNewGlobalPointer(String name, Pointer p) {
    assert name != null && p != null;
    globalPointers.put(name, p);
    registerPointer(p, new GlobalVariable(name));
  }

  @Override
  public void addNewLocalPointer(String name, Pointer p) {
    assert name != null && p != null;
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
  
  private Pointer getPointer(PointerLocation location) {
    assert location != null;
    if (location instanceof MemoryAddress) {
      return heap.get(location);
    
    } else if (location instanceof LocalVariable) {
      LocalVariable var = (LocalVariable)location;
      /*if (!var.getFunctionName().equals(getCurrentFunctionName())) {
        throw new IllegalArgumentException("Variable " + var + " out of scope (" + getCurrentFunctionName() + ")!");
      }
      return localPointers.peekLast().getSecond().get(var.getVarName());*/
      Map<String, Pointer> stackframe = allLocalPointers.get(var.getFunctionName());
      if (stackframe == null) {
        throw new IllegalStateException("No variables found for function context " + var.getFunctionName());
      }
      assert stackframe != null;
      return stackframe.get(var.getVarName());
      
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
    assert memAddress != null && p != null;
    heap.put(memAddress, p);
    registerPointer(p, memAddress);
  }

  
  private void addAllReverseRelations(Pointer pointer) {
    PointerLocation location = pointer.getLocation();
    
    for (PointerTarget target : pointer.getTargets()) {
      
      if (target != INVALID_POINTER && target != UNKNOWN_POINTER) {
        
        Set<PointerLocation> locs = reverseRelation.get(target);
        if (locs == null) {
          locs = new HashSet<PointerLocation>();
          reverseRelation.put(target, locs);
        }
        
        locs.add(location);
      }
    }
  }
  
  private void removeAllReverseRelations(Pointer pointer) {
    PointerLocation location = pointer.getLocation();

    for (PointerTarget target : pointer.getTargets()) {
      
      if (target != INVALID_POINTER && target != UNKNOWN_POINTER) {
        
        Set<PointerLocation> locs = reverseRelation.get(target);
        if (locs == null || !locs.contains(location)) {
          throw new IllegalStateException("Trying to remove reverse reference to location "
                  + location + " from target " + target + ", but it's not there");
        }
        
        locs.remove(location);
      }
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
    if (getAliases(p1).contains(p2.getLocation())) {
      return true;
    }
    
    // check if both pointer have the same single target
    if (p1.getNumberOfTargets() == 1 && p2.getNumberOfTargets() == 1) {
      PointerTarget target = p1.getFirstTarget();
      
      if (target.equals(p2.getFirstTarget())
          && target != INVALID_POINTER && target != UNKNOWN_POINTER) {
        
        mergeAliases(p1.getLocation(), p2.getLocation());
        System.out.println("INFO: Found pointer aliases which were not already aliased.");
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
    PointerTarget target = p.getFirstTarget();
    if (target == INVALID_POINTER || target == UNKNOWN_POINTER) {
      // no tracking of aliases for these values
      return;
    }
    
    Set<PointerLocation> aliasesOfP = getAliases(loc);
    if (!reverseRelation.containsKey(target)) {
      throw new IllegalStateException("Target " + target + " has no set of locations!");
    }
        
    for (PointerLocation candidateLoc : reverseRelation.get(target)) {
      if (!aliasesOfP.contains(candidateLoc)) {
        Pointer candidatePointer = getPointer(candidateLoc);
        
        if (candidatePointer.getNumberOfTargets() == 1) {
          assert candidatePointer.getTargets().equals(p.getTargets());
          
          mergeAliases(loc, candidateLoc);
          aliasesOfP = getAliases(loc); // this may be a different object now
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
    
    removeAllReverseRelations(pointer);
    removeAllAliases(location);

    op.doOperation(this, pointer, keepOldTargets);
    
    addAllReverseRelations(pointer);
    findAndMergePossibleAliases(pointer);
  }
  
  private <E extends Exception> void pointerOpForAllAliases(PointerOperation<E> op,
      Pointer pointer, boolean keepOldTargets) throws E {
    
    for (PointerLocation aliasLoc : getAliases(pointer)) {
      Pointer aliasPointer = getPointer(aliasLoc);
      removeAllReverseRelations(aliasPointer);
      
      op.doOperation(this, aliasPointer, keepOldTargets);
      
      addAllReverseRelations(aliasPointer);
    }
    // do not call findAndMergePossibleAliases here because this method should
    // leave the alias set untouched
  }
    
  public void pointerOpAssumeEquality(Pointer firstPointer, Pointer secondPointer) throws InvalidPointerException {
    if (areAliases(firstPointer, secondPointer)) {
      return;
    }
    
    ArrayList<PointerTarget> intersection = new ArrayList<PointerTarget>();
    Set<PointerTarget> firstTargets = firstPointer.getTargets();
    Set<PointerTarget> secondTargets = secondPointer.getTargets();

    for (PointerTarget target : firstTargets) {
      if (secondTargets.contains(target)) {
        intersection.add(target);
      }
    }
    
    if (intersection.size() != firstTargets.size()) {
      pointerOpForAllAliases(new Pointer.AssignListOfTargets(intersection), firstPointer, false);
    }
    
    if (intersection.size() != secondTargets.size()) {
      pointerOpForAllAliases(new Pointer.AssignListOfTargets(intersection), secondPointer, false);
    }
    
    // now first and second pointer have the same set of targets
    
    mergeAliases(firstPointer.getLocation(), secondPointer.getLocation());
  }
  
  public void pointerOpAssumeEquality(Pointer pointer, PointerTarget target) {
    if (!(pointer.getNumberOfTargets() == 1 && pointer.contains(target))) {
      
      pointerOpForAllAliases(new Pointer.Assign(target), pointer, false);
      
      findAndMergePossibleAliases(pointer);
    } else {
      // it is already equal like it should be 
    }
  }
  
  public void pointerOpAssumeInequality(Pointer firstPointer, Pointer secondPointer) throws InvalidPointerException {
    if (areAliases(firstPointer, secondPointer)) {
      throw new InvalidPointerException("Aliased pointers cannot be inequal.");
    }
    
    if (firstPointer.getNumberOfTargets() == 1) {
      pointerOpAssumeInequality(secondPointer, firstPointer.getFirstTarget());
    
    } else if (secondPointer.getNumberOfTargets() == 1) {
      pointerOpAssumeInequality(firstPointer, secondPointer.getFirstTarget());
      
    } else {
      // can't do anything
    }
  }

  public void pointerOpAssumeInequality(Pointer pointer, PointerTarget target) throws InvalidPointerException {
    if (target != INVALID_POINTER && target != UNKNOWN_POINTER) { 
      
      if (pointer.contains(target)) {
        pointerOpForAllAliases(new Pointer.AssumeInequality(target), pointer, true);

        findAndMergePossibleAliases(pointer);
      }
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
    HashMap<String, Pointer> newLocalPointers = new HashMap<String, Pointer>();
    localPointers.addLast(new Pair<String, HashMap<String, Pointer>>(functionName, newLocalPointers));
    
    if (currentFunctionName.equals("")) {
      currentFunctionName = functionName;
    } else {
      currentFunctionName = currentFunctionName + FUNCTION_NAME_SEPARATOR + functionName;
    }
    allLocalPointers.put(currentFunctionName, newLocalPointers);
  }
  
  public void returnFromFunction() {
    assert currentFunctionName != "" && currentFunctionName.contains(":")
        : "Cannot return from global context or main function!";
    localPointers.pollLast();
    allLocalPointers.remove(currentFunctionName);
    String oldFunctionName = currentFunctionName;
    currentFunctionName = currentFunctionName.substring(0, currentFunctionName.lastIndexOf(FUNCTION_NAME_SEPARATOR));

    Iterator<PointerLocation> aliasIt = aliases.keySet().iterator();
    while (aliasIt.hasNext()) {
      PointerLocation loc = aliasIt.next();
      
      if (loc instanceof LocalVariable) {
      
        if (oldFunctionName.equals(((LocalVariable)loc).getFunctionName())) {
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
      Iterator<PointerLocation> itLocations = locations.iterator();
      while (itLocations.hasNext()) {
        PointerLocation loc = itLocations.next();
        if (loc instanceof LocalVariable) {
          if (oldFunctionName.equals(((LocalVariable)loc).getFunctionName())) {
            // a local pointer points to this target
            itLocations.remove();
          }
        }
      }
      
      if (target instanceof LocalVariable) {
        if (oldFunctionName.equals(((LocalVariable)target).getFunctionName())) {
          // this target is a local variable, there may be no remaining reference to this target!
          if (!locations.isEmpty()) {
            // all local locations have already been removed, there has to be a global one!
            // TODO report warning about this
            Pointer p = getPointer(((LocalVariable)target).getVarName());
            pointerOp(new Pointer.Assign(INVALID_POINTER), p);
            // No need to handle aliases here, as there will be one iteration of the while loop 
            // for them as well
          }
          reverseIt.remove();
        }
      }
    }
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
  
  public void setCurrentEdge(CFAEdge currentEdge) {
    this.currentEdge = currentEdge;
  }

  public CFAEdge getCurrentEdge() {
    return currentEdge;
  }

  @Override
  public int hashCode() {
    return localPointers.hashCode();
  }
  
  @Override
  public PointerAnalysisElement clone() {
    return new PointerAnalysisElement(globalPointers, localPointers, heap,
                        mallocs, reverseRelation, aliases, currentFunctionName); 
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