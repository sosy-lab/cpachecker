// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.octagon;

public class NumArray {

  private final long array;

  NumArray(long l) {
    array = l;
  }

  long getArray() {
    return array;
  }

  @Override
  public String toString() {
    // TODO
    return super.toString();
  }

  @Override
  public boolean equals(Object pObj) {
    if (!(pObj instanceof NumArray)) {
      return false;
    }
    NumArray otherArr = (NumArray) pObj;
    return array == otherArr.array;
  }

  @Override
  public int hashCode() {
    return (int) array;
  }
}
