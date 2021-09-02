// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.model;

public interface ExplicitTargetSet extends TargetSet {

  boolean contains(MemorySegment pSegment);

  MemorySegment.Identifier getLowerBound();

  MemorySegment.Identifier getUpperBound();

  int getSize();

  Iterable<MemorySegment> getSegments();

  @Override
  default boolean includes(TargetSet pOther) {

    if (pOther instanceof ExplicitTargetSet) {

      ExplicitTargetSet other = (ExplicitTargetSet) pOther;

      if (getLowerBound().compareTo(other.getLowerBound()) > 0) {
        return false;
      }

      if (getUpperBound().compareTo(other.getUpperBound()) < 0) {
        return false;
      }

      for (MemorySegment otherSegment : other.getSegments()) {
        if (!contains(otherSegment)) {
          return false;
        }
      }

      return true;

    } else {
      return pOther.equals(TargetSetBot.INSTANCE);
    }
  }
}
