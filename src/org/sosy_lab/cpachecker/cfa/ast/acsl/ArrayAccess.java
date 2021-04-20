// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public class ArrayAccess implements ACSLTerm {

  private final ACSLTerm array;
  private final ACSLTerm index;

  public ArrayAccess(ACSLTerm pArray, ACSLTerm pIndex) {
    array = pArray;
    index = pIndex;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ArrayAccess) {
      ArrayAccess other = (ArrayAccess) o;
      return array.equals(other.array) && index.equals(other.index);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 3 * array.hashCode() + index.hashCode();
  }

  @Override
  public String toString() {
    return array.toString() + "[" + index.toString() + "]";
  }

  public ACSLTerm getArray() {
    return array;
  }

  public ACSLTerm getIndex() {
    return index;
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    return array.isAllowedIn(clauseType) && index.isAllowedIn(clauseType);
  }

  @Override
  public <R, X extends Exception> R accept(ACSLTermVisitor<R, X> visitor) throws X {
    return visitor.visit(this);
  }
}
