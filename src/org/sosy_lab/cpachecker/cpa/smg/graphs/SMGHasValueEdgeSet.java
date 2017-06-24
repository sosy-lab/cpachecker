/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.smg.graphs;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.TreeMultimap;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue.SMGEdgeHasValueComparator;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject.SMGObjectComparator;

import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;


public class SMGHasValueEdgeSet implements Set<SMGEdgeHasValue>, SMGHasValueEdges {

  private TreeMultimap<SMGObject, SMGEdgeHasValue> map = TreeMultimap.create(
      new SMGObjectComparator(), new SMGEdgeHasValueComparator());

  @Override
  public SMGHasValueEdges copy() {
    SMGHasValueEdgeSet copy = new SMGHasValueEdgeSet();
    copy.addAll(this);
    return copy;
  }

  @Override
  public void removeAllEdgesOfObject(SMGObject pObj) {
    SMGEdgeHasValue pEdge = new SMGEdgeHasValue(0, 0, pObj, 0);
    NavigableSet<SMGEdgeHasValue> valueView = map.get(pEdge.getObject());
    Iterator<SMGEdgeHasValue> iterator = valueView.iterator();
    while (iterator.hasNext()) {
      iterator.next();
      iterator.remove();
    }
  }

  @Override
  public void addEdge(SMGEdgeHasValue pEdge) {
    NavigableSet<SMGEdgeHasValue> valueView = map.get(pEdge.getObject());
    if (!valueView.contains(pEdge)) {
      map.put(pEdge.getObject(), pEdge);
    }
  }

  @Override
  public void removeEdge(SMGEdgeHasValue pEdge) {
    map.remove(pEdge.getObject(), pEdge);
  }

  @Override
  public void replaceHvEdges(Set<SMGEdgeHasValue> pNewHV) {
    map.clear();
    for(SMGEdgeHasValue edge : pNewHV) {
      addEdge(edge);
    }
  }

  @Override
  public Set<SMGEdgeHasValue> getHvEdges() {
    return ImmutableSet.copyOf(this.map.values());
  }

  @Override
  public Set<SMGEdgeHasValue> filter(SMGEdgeHasValueFilter pFilter) {
    return pFilter.filterSet((SMGHasValueEdges)this);
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    if(o instanceof SMGEdgeHasValue) {
      SMGEdgeHasValue edge = (SMGEdgeHasValue)o;
      return map.containsEntry(edge.getObject(), edge);
    }
    return false;
  }

  @Override
  public Iterator<SMGEdgeHasValue> iterator() {
    return map.values().iterator();
  }

  @Override
  public Object[] toArray() {
    return map.values().toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return map.values().toArray(a);
  }

  @Override
  public boolean add(SMGEdgeHasValue pSMGEdgeHasValue) {
    return map.put(pSMGEdgeHasValue.getObject(), pSMGEdgeHasValue);
  }

  @Override
  public boolean remove(Object o) {
    if(o instanceof SMGEdgeHasValue) {
      SMGEdgeHasValue edge = (SMGEdgeHasValue) o;
      return map.remove(edge.getObject(), edge);
    }
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    boolean result = true;
    for (Object o: c) {
      if (o instanceof SMGEdgeHasValue) {
        SMGEdgeHasValue edge = (SMGEdgeHasValue) o;
        result = result && map.containsEntry(edge.getObject(), edge);
      } else {
        return false;
      }
    }
    return result;
  }

  @Override
  public boolean addAll(Collection<? extends SMGEdgeHasValue> c) {
    boolean result = false;
    for(SMGEdgeHasValue value: c) {
      result = map.put(value.getObject(), value) || result;
    }
    return result;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return map.values().retainAll(c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    boolean result = false;
    for (Object o: c) {
      if (o instanceof SMGEdgeHasValue) {
        SMGEdgeHasValue edge = (SMGEdgeHasValue) o;
        result = map.remove(edge.getObject(), edge) || result;
      }
    }
    return result;
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public Set<SMGEdgeHasValue> getEdgesForObject(SMGObject pObject) {
    return map.get(pObject);
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof SMGHasValueEdgeSet) {
      SMGHasValueEdgeSet other = (SMGHasValueEdgeSet) pObj;
      return map.equals(other.map);
    }
    return false;
  }
}