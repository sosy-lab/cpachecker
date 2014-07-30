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

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class EmptyECPEdgeSet implements ECPEdgeSet {

  private static class EmptyIterator implements Iterator<CFAEdge> {

    private static EmptyIterator ITERATOR_INSTANCE = new EmptyIterator();

    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public CFAEdge next() {
      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

  public static EmptyECPEdgeSet INSTANCE = new EmptyECPEdgeSet();

  private EmptyECPEdgeSet() {

  }

  @Override
  public boolean contains(CFAEdge pCFAEdge) {
    return false;
  }

  @Override
  public ECPEdgeSet startIn(ECPNodeSet pNodeSet) {
    return this;
  }

  @Override
  public ECPEdgeSet endIn(ECPNodeSet pNodeSet) {
    return this;
  }

  @Override
  public ECPEdgeSet intersect(ECPEdgeSet pOther) {
    return this;
  }

  @Override
  public ECPEdgeSet union(ECPEdgeSet pOther) {
    return pOther;
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public Iterator<CFAEdge> iterator() {
    return EmptyIterator.ITERATOR_INSTANCE;
  }

  @Override
  public <T> T accept(ECPVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

}
