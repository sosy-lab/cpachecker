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
package cpa.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;

import cfa.objectmodel.CFANode;
import cpa.common.algorithm.CPAAlgorithm;
import cpa.common.interfaces.AbstractElementWithLocation;

/**
 * A class that can be used by CPAs to build specialized versions of
 * the "reached" set used by CPAAlgorithm.
 *
 * It keeps the AbstractElements grouped by program locations (CFANodes),
 * using a Map for the implementation
 *
 * @see CPAAlgorithm.createReachedSet
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
class LocationMappedReachedSet implements Set<AbstractElementWithLocation> {
    private final Map<CFANode, Set<AbstractElementWithLocation>> repr;
    private int numElems;

    private static class Iter implements Iterator<AbstractElementWithLocation> {
        private final Iterator<Map.Entry<CFANode, Set<AbstractElementWithLocation>>> outer;
        private Iterator<AbstractElementWithLocation> inner;

        private Iter(Iterator<Map.Entry<CFANode, Set<AbstractElementWithLocation>>> it) {
            outer = it;
            advanceInner();
        }

        private void advanceInner() {
            inner = null;
            while (inner == null && outer.hasNext()) {
                Set<AbstractElementWithLocation> s = outer.next().getValue();
                if (!s.isEmpty()) {
                    inner = s.iterator();
                }
            }
        }

        @Override
        public boolean hasNext() {
            if (inner == null) return false;
            if (inner.hasNext()) return true;
            advanceInner();
            return inner != null && inner.hasNext();
        }

        @Override
        public AbstractElementWithLocation next() {
            return inner.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove not supported!");
        }
    }

    public LocationMappedReachedSet() {
        repr = new HashMap<CFANode, Set<AbstractElementWithLocation>>();
        numElems = 0;
    }

    public Set<AbstractElementWithLocation> getReached(CFANode loc) {
      Set<AbstractElementWithLocation> result = repr.get(loc);
      if (result == null) {
        result = new HashSet<AbstractElementWithLocation>();
        repr.put(loc, result);
      }
      return result;
    }

    @Override
    public boolean add(AbstractElementWithLocation elem) {
      CFANode loc = elem.getLocationNode();
      Set<AbstractElementWithLocation> s = getReached(loc);
      boolean added = s.add(elem);
      if (added) {
        ++numElems;
      }
      return added;
    }

    @Override
    public boolean addAll(Collection<? extends AbstractElementWithLocation> elems) {
        boolean added = false;
        for (AbstractElementWithLocation e : elems) {
            added |= add(e);
        }
        return added;
    }

    @Override
    public void clear() {
        repr.clear();
        numElems = 0;
    }

    @Override
    public boolean contains(Object o) {
      Preconditions.checkNotNull(o);
      Preconditions.checkArgument(o instanceof AbstractElementWithLocation);
    
      AbstractElementWithLocation e = (AbstractElementWithLocation)o;
      CFANode loc = e.getLocationNode();
      return repr.containsKey(loc) && repr.get(loc).contains(e);
    }

    @Override
    public boolean containsAll(Collection<?> elems) {
        for (Object o : elems) {
            if (!contains(o)) return false;
        }
        return true;
    }

    @Override
    public boolean isEmpty() {
        if (repr.isEmpty()) return true;
        for (Set<AbstractElementWithLocation> s : repr.values()) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public Iterator<AbstractElementWithLocation> iterator() {
        return new Iter(repr.entrySet().iterator());
    }

    @Override
    public boolean remove(Object o) {
      Preconditions.checkNotNull(o);
      Preconditions.checkArgument(o instanceof AbstractElementWithLocation);
      
      AbstractElementWithLocation e = (AbstractElementWithLocation)o;
      CFANode loc = e.getLocationNode();
      if (!repr.containsKey(loc)) {
        return false;
      }
      Set<AbstractElementWithLocation> s = repr.get(loc);
      boolean ret = s.remove(o);
      if (ret) {
          --numElems;
      }
      return ret;
    }

    @Override
    public boolean removeAll(Collection<?> elems) {
        boolean changed = false;
        for (Object o : elems) {
            changed |= remove(o);
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> arg0) {
        throw new RuntimeException("retainAll not implemented!");
    }

    @Override
    public int size() {
        return numElems;
    }

    @Override
    public Object[] toArray() {
        throw new RuntimeException("toArray not implemented!");
    }

    @Override
    public <T> T[] toArray(T[] arg0) {
        throw new RuntimeException("toArray not implemented!");
    }
    
    @Override
    public String toString() {
    	return repr.toString();
    }

}
