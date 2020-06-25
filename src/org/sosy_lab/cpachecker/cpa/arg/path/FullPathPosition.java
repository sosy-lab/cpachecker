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

class FullPathPosition extends PathPosition {

  private final int offset;

  FullPathPosition(ARGPath pPath, int pPosition, int pOffset) {
    super(pPath, pPosition);
    offset = pOffset;
  }

  /**
   * {@inheritDoc}
   * The position is exact, that means if the position is in the middle of an
   * ARG hole the iterator will start there.
   */
  @Override
  public PathIterator fullPathIterator() {
    return new DefaultFullPathIterator(path, pos, offset);
  }

  /**
   * {@inheritDoc}
   * The position is exact, that means if the position is in the middle of an
   * ARG hole the iterator will start there.
   */
  @Override
  public PathIterator reverseFullPathIterator() {
    return new ReverseFullPathIterator(path, pos, offset);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + path.hashCode();
    result = prime * result + pos;
    result = prime * result + offset;
    return result;
  }

  @Override
  public boolean equals(Object pObj) {
    if (!(pObj instanceof FullPathPosition)) {
      return false;
    }
    FullPathPosition other = (FullPathPosition) pObj;

    return super.equals(pObj) && other.offset == this.offset;
  }
}
