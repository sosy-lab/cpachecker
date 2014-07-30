/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class StandardECPEdgeSet implements ECPEdgeSet {

  public static ECPEdgeSet create(CFAEdge pCFAEdge) {
    return new SingletonECPEdgeSet(pCFAEdge);
  }

  public static ECPEdgeSet create(Collection<CFAEdge> pCFAEdges) {
    if (pCFAEdges.isEmpty()) {
      return EmptyECPEdgeSet.INSTANCE;
    }

    if (pCFAEdges.size() == 1) {
      CFAEdge lCFAEdge = pCFAEdges.iterator().next();

      return create(lCFAEdge);
    }

    return new StandardECPEdgeSet(pCFAEdges);
  }

  private Set<CFAEdge> mCFAEdges = new HashSet<>();

  private StandardECPEdgeSet(Collection<CFAEdge> pCFAEdges) {
    mCFAEdges.addAll(pCFAEdges);
  }

  /** copy constructor */
  public StandardECPEdgeSet(StandardECPEdgeSet pEdgeSet) {
    this(pEdgeSet.mCFAEdges);
  }

  @Override
  public boolean contains(CFAEdge pCFAEdge) {
    return mCFAEdges.contains(pCFAEdge);
  }

  @Override
  public ECPEdgeSet startIn(ECPNodeSet pNodeSet) {
    HashSet<CFAEdge> lResult = new HashSet<>();

    for (CFAEdge lEdge : mCFAEdges) {
      if (pNodeSet.contains(lEdge.getPredecessor())) {
        lResult.add(lEdge);
      }
    }

    return new StandardECPEdgeSet(lResult);
  }

  @Override
  public ECPEdgeSet endIn(ECPNodeSet pNodeSet) {
    HashSet<CFAEdge> lResult = new HashSet<>();

    for (CFAEdge lEdge : mCFAEdges) {
      if (pNodeSet.contains(lEdge.getSuccessor())) {
        lResult.add(lEdge);
      }
    }

    return new StandardECPEdgeSet(lResult);
  }

  @Override
  public ECPEdgeSet intersect(ECPEdgeSet pOther) {
    if (pOther instanceof EmptyECPEdgeSet) {
      return pOther;
    }

    if (pOther instanceof SingletonECPEdgeSet) {
      SingletonECPEdgeSet lOther = (SingletonECPEdgeSet)pOther;

      if (mCFAEdges.contains(lOther.getCFAEdge())) {
        return lOther;
      }
      else {
        return EmptyECPEdgeSet.INSTANCE;
      }
    }

    StandardECPEdgeSet lOther = (StandardECPEdgeSet)pOther;

    HashSet<CFAEdge> lIntersection = new HashSet<>();
    lIntersection.addAll(mCFAEdges);
    lIntersection.retainAll(lOther.mCFAEdges);

    return create(lIntersection);
  }

  @Override
  public ECPEdgeSet union(ECPEdgeSet pOther) {
    if (pOther instanceof EmptyECPEdgeSet) {
      return this;
    }

    HashSet<CFAEdge> lUnion = new HashSet<>();

    if (pOther instanceof SingletonECPEdgeSet) {
      SingletonECPEdgeSet lOther = (SingletonECPEdgeSet)pOther;
      lUnion.add(lOther.getCFAEdge());
    }
    else {
      StandardECPEdgeSet lOther = (StandardECPEdgeSet)pOther;
      lUnion.addAll(lOther.mCFAEdges);
    }

    lUnion.addAll(mCFAEdges);

    return new StandardECPEdgeSet(lUnion);
  }

  @Override
  public int hashCode() {
    return mCFAEdges.hashCode();
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    if (pOther == null) {
      return false;
    }

    if (pOther.getClass().equals(getClass())) {
      StandardECPEdgeSet lOther = (StandardECPEdgeSet)pOther;

      return mCFAEdges.equals(lOther.mCFAEdges);
    }

    return false;
  }

  @Override
  public int size() {
    return mCFAEdges.size();
  }

  @Override
  public boolean isEmpty() {
    // Per construction a standard ECP edge set should be never empty!
    return false;
  }

  @Override
  public String toString() {
    return mCFAEdges.toString();
  }

  @Override
  public Iterator<CFAEdge> iterator() {
    return mCFAEdges.iterator();
  }

  @Override
  public <T> T accept(ECPVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

}
