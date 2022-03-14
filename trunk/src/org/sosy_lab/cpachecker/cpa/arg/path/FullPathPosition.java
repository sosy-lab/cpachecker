// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.path;

class FullPathPosition extends PathPosition {

  private final int offset;

  FullPathPosition(ARGPath pPath, int pPosition, int pOffset) {
    super(pPath, pPosition);
    offset = pOffset;
  }

  /**
   * {@inheritDoc} The position is exact, that means if the position is in the middle of an ARG hole
   * the iterator will start there.
   */
  @Override
  public PathIterator fullPathIterator() {
    return new DefaultFullPathIterator(path, pos, offset);
  }

  /**
   * {@inheritDoc} The position is exact, that means if the position is in the middle of an ARG hole
   * the iterator will start there.
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

    return super.equals(pObj) && other.offset == offset;
  }
}
