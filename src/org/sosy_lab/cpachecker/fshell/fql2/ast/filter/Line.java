/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.fshell.fql2.ast.filter;

public class Line implements Filter {
  private int mLine;

  public Line(Integer pLine) {
    assert(pLine != null);

    mLine = pLine.intValue();
  }

  public int getLine() {
    return mLine;
  }

  @Override
  public String toString() {
    return "@LINE(" + mLine + ")";
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    if (pOther == null) {
      return false;
    }

    if (pOther.getClass() == getClass()) {
      Line mOtherLine = (Line)pOther;

      return (mOtherLine.getLine() == mLine);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return mLine;
  }

  @Override
  public <T> T accept(FilterVisitor<T> pVisitor) {
    assert(pVisitor != null);

    return pVisitor.visit(this);
  }

}
