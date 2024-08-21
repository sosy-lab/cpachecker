// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.path;

import static com.google.common.base.Preconditions.checkState;

/** The implementation of PathIterator that iterates in the reverse direction of the analysis. */
class ReversePathIterator extends PathIterator {

  ReversePathIterator(ARGPath pPath, int pPos) {
    super(pPath, pPos);
  }

  ReversePathIterator(ARGPath pPath) {
    super(pPath, pPath.size() - 1);
  }

  @Override
  public void advance() throws IllegalStateException {
    checkState(hasNext(), "No more states in PathIterator.");
    pos--;
  }

  @Override
  public void rewind() throws IllegalStateException {
    checkState(hasPrevious(), "No previous states in PathIterator.");
    pos++;
  }

  @Override
  public boolean hasNext() {
    return pos > 0;
  }

  @Override
  public boolean hasPrevious() {
    return pos < path.size() - 1;
  }
}
