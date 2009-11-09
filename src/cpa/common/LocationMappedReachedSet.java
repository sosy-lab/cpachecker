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

import common.Pair;

import cfa.objectmodel.CFANode;
import cpa.common.algorithm.CPAAlgorithm;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;

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
class LocationMappedReachedSet implements Set<Pair<AbstractElementWithLocation,Precision>> {
    private final Map<CFANode, Set<Pair<AbstractElementWithLocation,Precision>>> repr;
    private int numElems;

    private class Iter implements Iterator<Pair<AbstractElementWithLocation,Precision>> {
        private final Iterator<Map.Entry<CFANode, Set<Pair<AbstractElementWithLocation,Precision>>>> outer;
        private Iterator<Pair<AbstractElementWithLocation,Precision>> inner;

        Iter(Iterator<Map.Entry<CFANode, Set<Pair<AbstractElementWithLocation,Precision>>>> it) {
            outer = it;
            advanceInner();
        }

        private void advanceInner() {
            inner = null;
            while (inner == null && outer.hasNext()) {
                Set<Pair<AbstractElementWithLocation,Precision>> s = outer.next().getValue();
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
        public Pair<AbstractElementWithLocation,Precision> next() {
            return inner.next();
        }

        @Override
        public void remove() {
            throw new RuntimeException("Remove not supported!");
        }
    }

    public LocationMappedReachedSet() {
        repr = new HashMap<CFANode, Set<Pair<AbstractElementWithLocation,Precision>>>();
        numElems = 0;
    }

    public Set<Pair<AbstractElementWithLocation,Precision>> getReached(CFANode loc) {
        if (repr.containsKey(loc)) {
            return repr.get(loc);
        } else {
            return null;
        }
    }

    @Override
    public boolean add(Pair<AbstractElementWithLocation,Precision> elem) {
        // AbstractElementWithLocation e = (AbstractElementWithLocation)elem;
        CFANode loc = elem.getFirst().getLocationNode();
        if (!repr.containsKey(loc)) {
            repr.put(loc, new HashSet<Pair<AbstractElementWithLocation,Precision>>());
        }
        Set<Pair<AbstractElementWithLocation,Precision>> s = repr.get(loc);
        boolean added = s.add(elem);
        if (added) {
            ++numElems;
        }
        return added;
    }

    @Override
    public boolean addAll(Collection<? extends Pair<AbstractElementWithLocation,Precision>> elems) {
        boolean added = false;
        for (Pair<AbstractElementWithLocation,Precision> e : elems) {
            added |= add(e);
        }
        return added;
    }

    @Override
    public void clear() {
        repr.clear();
        numElems = 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Pair)) return false;
        if (!(((Pair)o).getFirst() instanceof AbstractElementWithLocation)) return false;
        if (!(((Pair)o).getSecond() instanceof Precision)) return false;
      
        Pair<AbstractElementWithLocation,Precision> e = (Pair<AbstractElementWithLocation,Precision>)o;
        CFANode loc = e.getFirst().getLocationNode();
        if (!repr.containsKey(loc)) return false;
        return repr.get(loc).contains(o);
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
        for (Set<Pair<AbstractElementWithLocation,Precision>> s : repr.values()) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public Iterator<Pair<AbstractElementWithLocation,Precision>> iterator() {
        return new Iter(repr.entrySet().iterator());
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(Object o) {
      if (!(o instanceof Pair)) return false;
      if (!(((Pair)o).getFirst() instanceof AbstractElementWithLocation)) return false;
      if (!(((Pair)o).getSecond() instanceof Precision)) return false;
      Pair<AbstractElementWithLocation,Precision> e = (Pair<AbstractElementWithLocation,Precision>)o;
        CFANode loc = e.getFirst().getLocationNode();
        if (!repr.containsKey(loc)) return false;
        Set<Pair<AbstractElementWithLocation,Precision>> s = repr.get(loc);
        boolean ret = s.remove(o);
        if (ret) {
            --numElems;
        }
        if (s.isEmpty()) {
            repr.remove(loc);
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
