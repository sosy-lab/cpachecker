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

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
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

  private final HashMap<String, Pointer> globalPointers; // tracks all global pointers
    // contains global non-pointer variables, too (mapped to null)
  
  private final Deque<Pair<String, HashMap<String, Pointer>>> localPointers;
  
  private final HashMap<MemoryAddress, Pointer> heap;
  
  private final HashSet<MemoryRegion> mallocs; // list of all malloc'ed memory regions

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
    callFunction("main()");
  }
  
  private PointerAnalysisElement(final Map<String, Pointer> globalPointers,
                                 final Deque<Pair<String, HashMap<String, Pointer>>> localPointers,
                                 final Map<MemoryAddress, Pointer> heap,
                                 final Set<MemoryRegion> mallocs) {
    this.globalPointers = new HashMap<String, Pointer>();
    this.localPointers = new LinkedList<Pair<String, HashMap<String, Pointer>>>();
    this.heap = new HashMap<MemoryAddress, Pointer>();
    this.mallocs = new HashSet<MemoryRegion>(mallocs);
    
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
  }
  
  @Override
  public void addNewGlobalPointer(String name, Pointer p) {
    globalPointers.put(name, p);
  }

  @Override
  public void addNewLocalPointer(String name, Pointer p) {
    localPointers.peekLast().getSecond().put(name, p);
  }
  
  @Override
  public Pointer getPointer(String name) {
    Pointer result = globalPointers.get(name);
    if (result == null) {
      result = localPointers.peekLast().getSecond().get(name);
    }
    return result;
  }
  
  @Override
  public Pointer getHeapPointer(MemoryAddress memAddress) {
    return heap.get(memAddress);
  }
  
  @Override
  public void writeOnHeap(MemoryAddress memAddress, Pointer p) {
    heap.put(memAddress, p);
  }
   
  @Override
  public PointerTarget malloc(int length) throws InvalidPointerException {
    if (length < 0) {
      throw new InvalidPointerException("Malloc with negative length");
    
    } else {
      MemoryRegion mem = new MemoryRegion();
      mem.setLength(length);
      mallocs.add(mem);
      return new MemoryAddress(mem, 0);
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
    for (PointerTarget target : p.getTargets()) {
      if (target instanceof MemoryAddress) {
        free((MemoryAddress)target);
      } else {
        throw new InvalidPointerException("Cannot free pointer to " + target.getClass().getSimpleName());
      }
    }
    p.setTarget(INVALID_POINTER);
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
        && localPointers.equals(otherElement.localPointers);
  }
  
  public void callFunction(String functionName) {
    localPointers.addLast(new Pair<String, HashMap<String, Pointer>>(functionName, new HashMap<String, Pointer>()));
  }
  
  public void returnFromFunction() {
    localPointers.pollLast();
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
    return new PointerAnalysisElement(globalPointers, localPointers, heap, mallocs); 
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