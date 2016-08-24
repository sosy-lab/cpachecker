/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.cfapath;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class CFAPathStandardState implements CFAPathState, Iterable<CFAEdge> {

  private static final CFAPathStandardState sEmptyPath = new CFAPathStandardState();

  public static CFAPathStandardState getEmptyPath() {
    return sEmptyPath;
  }

  private final CFAPathStandardState mPredecessor;
  private final CFAEdge mCFAEdge;
  private final int mLength;

  private static class CFAEdgeIterator implements Iterator<CFAEdge> {

    private CFAPathStandardState crrentState;

    public CFAEdgeIterator(CFAPathStandardState pLastElement) {
      crrentState = pLastElement;
    }

    @Override
    public boolean hasNext() {
      return (crrentState != sEmptyPath);
    }

    @Override
    public CFAEdge next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      CFAEdge lNextCFAEdge = crrentState.mCFAEdge;

      crrentState = crrentState.mPredecessor;

      return lNextCFAEdge;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

  private CFAPathStandardState() {
    mPredecessor = null;
    mCFAEdge = null;
    mLength = 0;
  }

  public CFAPathStandardState(CFAPathStandardState pPredecessor, CFAEdge pCFAEdge) {
    if (pPredecessor == null) {
      throw new IllegalArgumentException();
    }

    if (pCFAEdge == null) {
      throw new IllegalArgumentException();
    }

    mPredecessor = pPredecessor;
    mCFAEdge = pCFAEdge;
    mLength = pPredecessor.getLength() + 1;
  }

  public int getLength() {
    return mLength;
  }

  public CFAEdge get(int lIndex) {
    if (lIndex >= mLength || lIndex < 0) {
      throw new IllegalArgumentException();
    }

    if (lIndex + 1 == mLength) {
      return mCFAEdge;
    } else {
      return mPredecessor.get(lIndex);
    }
  }

  @Override
  /*
   * Traverses the cfa path backwards.
   */
  public Iterator<CFAEdge> iterator() {
    return new CFAEdgeIterator(this);
  }

  public CFAEdge[] toArray() {
    CFAEdge[] lPath = new CFAEdge[mLength];

    CFAPathStandardState lElement = this;

    for (int lIndex = mLength - 1; lIndex >= 0; lIndex--) {
      lPath[lIndex] = lElement.mCFAEdge;
      lElement = lElement.mPredecessor;
    }

    return lPath;
  }

  @Override
  public String toString() {
    if (getLength() == 0) {
      return "<>";
    } else {
      if (getLength() == 1) {
        return "< " + mCFAEdge.toString() + " >";
      } else {
        return "< ... " + mCFAEdge.toString() + " >";
      }
    }
  }

}
