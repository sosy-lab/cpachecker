/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.pathformula.withUF;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;

/**
 * This class is used to keep information about already performed memory allocations of unknown type, e.g.
 *
 *   <pre>
 *   void *tmp_0 = malloc(size); // tmp_0 is a pointer variable, allocation type is unknown
 *   ...
 *   void *tmp_2 = tmp_0; // Now tmp_2 is also a pointer variable corresponding to the same allocation
 *   struct s* ps = (struct s*)tmp_2; // Now the actual type of the allocation is revealed
 *   </pre>
 *
 * <p>
 * When the type of the allocation is revealed (by the context in which one of the pointer variables is used),
 * the actual allocation occurs (pointer targets are added to the set).
 * </p>
 * <p>
 * Several base variables can fall within the same pool in case of merging, e.g.:
 *
 *   <pre>
 *   void *tmp_0;
 *   if (condition) {
 *     tmp_0 = malloc(size1); // Corresponds to a fake base variable __VERIFIER_successfull_alloc0
 *   } else {
 *     tmp_0 = malloc(size2); // Corresponds to another fake base variable __VERIFIER_successfull_alloc1
 *   }
 *   ... (struct s*) tmp_0 // Both base variables are allocated here as (struct s)
 *                         // (but their addresses can be different!)
 *   </pre>
 */
 public class DeferredAllocationPool {

  private DeferredAllocationPool(final PersistentList<String> pointerVariables,
                                 final boolean isZeroing,
                                 final CIntegerLiteralExpression size,
                                 final PersistentList<String> baseVariables) {
    this.pointerVariables = pointerVariables;
    this.isZeroing = isZeroing;
    this.size = size;
    this.baseVariables = baseVariables;
  }

  DeferredAllocationPool(final String pointerVariable,
                         final boolean isZeroing,
                         final CIntegerLiteralExpression size,
                         final String baseVariable) {
    this(PersistentLinkedList.of(pointerVariable), isZeroing, size, PersistentLinkedList.of(baseVariable));
  }

  private DeferredAllocationPool(final DeferredAllocationPool predecessor,
                                 final PersistentList<String> pointerVariables) {
    this(pointerVariables,
         predecessor.isZeroing,
         predecessor.size,
         predecessor.baseVariables);
  }

  public PersistentList<String> getPointerVariables() {
    return pointerVariables;
  }

  public PersistentList<String> getBaseVariables() {
    return baseVariables;
  }

  public boolean wasAllocationZeroing() {
    return isZeroing;
  }

  public CIntegerLiteralExpression getSize() {
    return size;
  }

  DeferredAllocationPool addPointerVariable(final String pointerVariable) {
    return new DeferredAllocationPool(this, this.pointerVariables.with(pointerVariable));
  }

  DeferredAllocationPool removePointerVariable(final String pointerVariable) {
    return new DeferredAllocationPool(this, pointerVariables.without(pointerVariable));
  }

  DeferredAllocationPool mergeWith(final DeferredAllocationPool other) {
    return new DeferredAllocationPool(mergeLists(this.pointerVariables, other.pointerVariables),
                                      this.isZeroing && other.isZeroing,
                                      this.size != null && other.size != null ?
                                        this.size.getValue().equals(other.size.getValue()) ? this.size : null :
                                        this.size != null ? this.size : other.size,
                                      mergeLists(this.baseVariables, other.baseVariables));
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof DeferredAllocationPool)) {
      return false;
    }
    final DeferredAllocationPool otherPool = (DeferredAllocationPool) other;
    if (pointerVariables.containsAll(otherPool.pointerVariables) &&
        otherPool.pointerVariables.containsAll(pointerVariables)) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    int result = 0;
    for (final String s : pointerVariables) {
      result += s.hashCode();
    }
    return result;
  }

  private final PersistentList<String> pointerVariables;
  private final boolean isZeroing;
  private final CIntegerLiteralExpression size;
  private final PersistentList<String> baseVariables;


  static <T> PersistentList<T> mergeLists(final PersistentList<T> list1,
                                          final PersistentList<T> list2) {
    final int size1 = list1.size();
    final ArrayList<T> arrayList1 = new ArrayList<>(size1);
    for (final T element : list1) {
      arrayList1.add(element);
    }
    final int size2 = list2.size();
    final ArrayList<T> arrayList2 = new ArrayList<>(size2);
    for (final T element : list2) {
      arrayList2.add(element);
    }
    int sizeCommon = 0;
    for (int i = 0;
         i < arrayList1.size() && i < arrayList2.size() && arrayList1.get(i).equals(arrayList2.get(i));
         i++) {
      ++sizeCommon;
    }
    PersistentList<T> result;
    final ArrayList<T> biggerArrayList, smallerArrayList;
    final int biggerCommonStart, smallerCommonStart;
    if (size1 > size2) {
      result = list1;
      biggerArrayList = arrayList1;
      smallerArrayList = arrayList2;
      biggerCommonStart = size1 - sizeCommon;
      smallerCommonStart = size2 - sizeCommon;
    } else {
      result = list2;
      biggerArrayList = arrayList2;
      smallerArrayList = arrayList1;
      biggerCommonStart = size2 - sizeCommon;
      smallerCommonStart = size1 - sizeCommon;
    }
    final Set<T> fromBigger = new HashSet<>(2 * biggerCommonStart, 1.0f);
    for (int i = 0; i < biggerCommonStart; i++) {
      fromBigger.add(biggerArrayList.get(i));
    }
    for (int i = 0; i < smallerCommonStart; i++) {
      final T target = smallerArrayList.get(i);
      if (!fromBigger.contains(target)) {
        result = result.with(target);
      }
    }
    return result;
  }
}