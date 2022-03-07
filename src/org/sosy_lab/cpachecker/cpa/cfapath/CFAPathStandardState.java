// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.cfapath;

import static com.google.common.base.Preconditions.checkArgument;

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
      return crrentState != sEmptyPath;
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
    checkArgument(pPredecessor != null);

    checkArgument(pCFAEdge != null);

    mPredecessor = pPredecessor;
    mCFAEdge = pCFAEdge;
    mLength = pPredecessor.getLength() + 1;
  }

  public int getLength() {
    return mLength;
  }

  public CFAEdge get(int lIndex) {
    checkArgument(lIndex < mLength && lIndex >= 0);

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
        return "< " + mCFAEdge + " >";
      } else {
        return "< ... " + mCFAEdge + " >";
      }
    }
  }
}
