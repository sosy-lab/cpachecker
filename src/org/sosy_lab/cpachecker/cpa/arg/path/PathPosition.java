/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg.path;

import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * A marker for a specific position in an {@link ARGPath}. This class is independent of the
 * traversal order of the iterator that was used to create it.
 */
public class PathPosition {

  protected final int pos;
  protected final ARGPath path;

  PathPosition(ARGPath pPath, int pPosition) {
    this.path = pPath;
    this.pos = pPosition;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + path.hashCode();
    result = prime * result + pos;
    return result;
  }

  @Override
  public boolean equals(Object pObj) {
    if (!(pObj instanceof PathPosition)) {
      return false;
    }
    PathPosition other = (PathPosition) pObj;

    return ((this.pos == other.pos) && (this.path.equals(other.path)));
  }

  /**
   * Create a fresh {@link PathIterator} for this path, initialized at this position of the path,
   * and iterating forwards.
   */
  public PathIterator iterator() {
    return new DefaultPathIterator(path, pos);
  }

  /**
   * Create a fresh {@link PathIterator} for this path, initialized at this position of the path,
   * and iterating backwards.
   */
  public PathIterator reverseIterator() {
    return new ReversePathIterator(path, pos);
  }

  /**
   * Create a fresh {@link FullPathIterator} for this path, initialized at this position of the
   * path, and iterating forwards.
   *
   * <p>Note: if the {@link PathPosition} object was not created from a FullPathIterator the
   * iteration will always start at a position with abstract state, not inside and ARG hole.
   */
  public PathIterator fullPathIterator() {
    PathIterator it = new DefaultFullPathIterator(path);
    while (it.pos != pos) {
      it.advance();
    }
    assert it.pos == pos;

    return new DefaultFullPathIterator(path, pos, it.getIndex());
  }

  /**
   * Create a fresh {@link FullPathIterator} for this path, initialized at this position of the
   * path, and iterating backwards.
   *
   * <p>Note: if the {@link PathPosition} object was not created from a FullPathIterator the
   * iteration will always start at a position with abstract state, not inside and ARG hole.
   */
  public PathIterator reverseFullPathIterator() {
    PathIterator it = new ReverseFullPathIterator(path);
    // get to the correct abstract state location
    while (it.pos != pos) {
      it.advance();
    }
    // now move until the offset is also correct
    while (it.hasNext() && it.pos == pos) {
      it.advance();
    }

    if (pos == it.pos) {
      assert pos == 0;
      return new ReverseFullPathIterator(path, pos, pos);
    } else {
      assert pos == it.pos + 1;
      return new ReverseFullPathIterator(path, pos, it.getIndex() + 1);
    }
  }

  /** @see PathIterator#getLocation() */
  public CFANode getLocation() {
    return iterator().getLocation();
  }

  /** Return the {@link ARGPath} that this position belongs to. */
  public ARGPath getPath() {
    return path;
  }
}
