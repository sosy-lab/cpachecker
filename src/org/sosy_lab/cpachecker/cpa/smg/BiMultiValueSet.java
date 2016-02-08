/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Created by druidos on 08.02.16.
 */
public class BiMultiValueSet<V> implements Set<V> {
  HashSet<V> hashSet;
  private Multimap<Object, V> oppositeMap;
  private ReturnOppositeValueOperator<V> oppositeValueOperator;


  public BiMultiValueSet(ReturnOppositeValueOperator<V> oper) {
    hashSet = new HashSet<>();
    oppositeMap = ArrayListMultimap.create();
    oppositeValueOperator = oper;
  }

  public BiMultiValueSet() {
    this(new ReturnOppositeSameOperator());
  }

  @Override
  public boolean add(V pV) {
    oppositeMap.put(oppositeValueOperator.getOpposite(pV), pV);
    return hashSet.add(pV);
  }

  @Override
  public boolean addAll(Collection<? extends V> c) {
    for (V elem : c) {
      oppositeMap.put(oppositeValueOperator.getOpposite(elem), elem);
    }
    return hashSet.addAll(c);
  }

  @Override
  public boolean remove(Object o) {
    if (hashSet.remove(o)) {
      oppositeMap.remove(oppositeValueOperator.getOpposite((V)o), o);
      return true;
    }
    return false;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    for (Object elem : c) {
      if (hashSet.contains(elem)) {
        oppositeMap.remove(oppositeValueOperator.getOpposite((V)elem), elem);
      }
    }
    return hashSet.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    for (Object elem : c) {
      if (!hashSet.contains(elem)) {
        oppositeMap.remove(oppositeValueOperator.getOpposite((V)elem), elem);
      }
    }
    return hashSet.retainAll(c);
  }

  @Override
  public int size() {
    return hashSet.size();
  }

  @Override
  public boolean isEmpty() {
    return hashSet.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return hashSet.contains(o);
  }

  @Override
  public Iterator<V> iterator() {
    return hashSet.iterator();
  }

  @Override
  public Object[] toArray() {
    return hashSet.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return hashSet.toArray(a);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return hashSet.containsAll(c);
  }

  @Override
  public void clear() {
    hashSet.clear();
    oppositeMap.clear();
  }

  public Set<V> getSet() {
    return hashSet;
  }

  public void removeOppositValue(Object pObj) {
    for (V elem: oppositeMap.removeAll(pObj)) {
      hashSet.remove(elem);
    }
  }

  public void removeValue(V pObj) {
    for (V elem: oppositeMap.removeAll(oppositeValueOperator.getOpposite(pObj))) {
      hashSet.remove(elem);
    }
  }
}
