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
/**
 * 
 */
package cpa.common;

import java.util.Collection;
import java.util.Iterator;

import common.Pair;

import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;

/**
 * @author holzera
 *
 * We do not allow changes of the underlying collection through this wrapper.
 * TODO E.g., stop allows only reading the reached set, not manipulating it, so
 * the interface should be changed to allow only reading.
 */
public class ProjectionWrapper implements
                              Collection<AbstractElementWithLocation> {
  private class ProjectionIterator implements Iterator<AbstractElementWithLocation> {
    private Iterator<Pair<AbstractElementWithLocation, Precision>> mIterator;
    
    public ProjectionIterator() {
      mIterator = mCollection.iterator();
    }
    
    @Override
    public boolean hasNext() {
      return mIterator.hasNext();
    }

    @Override
    public AbstractElementWithLocation next() {
      return mIterator.next().getFirst();
    }

    @Override
    public void remove() {
      assert(false);
      
      throw new RuntimeException("This operation is not permitted in a projection!");
    }
    
  }
  
  private Collection<Pair<AbstractElementWithLocation, Precision>> mCollection;
  
  public ProjectionWrapper(Collection<Pair<AbstractElementWithLocation, Precision>> pCollection) {
    assert(pCollection != null);
    
    mCollection = pCollection;
  }
  
  @Override
  public boolean add(AbstractElementWithLocation pArg0) {
    assert(false);
    
    throw new RuntimeException("This operation is not permitted in a projection!");
  }

  @Override
  public boolean addAll(Collection<? extends AbstractElementWithLocation> pArg0) {
    assert(false);
    
    throw new RuntimeException("This operation is not permitted in a projection!");
  }

  @Override
  public void clear() {
    assert(false);
    
    throw new RuntimeException("This operation is not permitted in a projection!");
  }

  @Override
  public boolean contains(Object pArg0) {
    assert(pArg0 != null);
    
    for (Object lObject : this) {
      if (pArg0.equals(lObject)) {
        return true;
      }
    }
    
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> pArg0) {
    assert(pArg0 != null);
    
    for (Object lObject : pArg0) {
      if (!contains(lObject)) {
        return false;
      }
    }
    
    return true;
  }

  @Override
  public boolean isEmpty() {
    return mCollection.isEmpty();
  }

  @Override
  public Iterator<AbstractElementWithLocation> iterator() {
    return new ProjectionIterator();
  }

  @Override
  public boolean remove(Object pArg0) {
    assert(false);
    
    throw new RuntimeException("This operation is not permitted in a projection!");
 }

  @Override
  public boolean removeAll(Collection<?> pArg0) {
    assert(false);
    
    throw new RuntimeException("This operation is not permitted in a projection!");
  }

  @Override
  public boolean retainAll(Collection<?> pArg0) {
    assert(false);
    
    throw new RuntimeException("This operation is not permitted in a projection!");
  }

  @Override
  public int size() {
    return mCollection.size();
  }

  @Override
  public Object[] toArray() {
    // TODO implement
    assert(false);
    
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T> T[] toArray(T[] pArg0) {
    // TODO implement
    assert(false);
    
     // TODO Auto-generated method stub
    return null;
  }

}
