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
