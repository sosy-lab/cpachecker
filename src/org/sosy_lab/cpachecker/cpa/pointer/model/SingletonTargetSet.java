// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.model;

import com.google.common.collect.ImmutableSet;

final class SingletonTargetSet implements ExplicitTargetSet {

  private final MemorySegment segment;

  private SingletonTargetSet(MemorySegment pSegment) {
    segment = pSegment;
  }

  static ExplicitTargetSet of(MemorySegment pSegment) {

    return new SingletonTargetSet(pSegment);
  }

  @Override
  public AliasType alias(TargetSet pOther) {

    if (pOther instanceof SingletonTargetSet) {

      MemorySegment otherSegment = ((SingletonTargetSet) pOther).segment;

      if (!segment.getIdentifier().equals(otherSegment.getIdentifier())) {
        return AliasType.NOT;
      }

      if (segment.getOffset() + segment.getLength() <= otherSegment.getOffset()) {
        return AliasType.NOT;
      }

      if (otherSegment.getOffset() + otherSegment.getLength() <= segment.getOffset()) {
        return AliasType.NOT;
      }

      return AliasType.MUST;

    } else {
      return pOther.alias(this);
    }
  }

  @Override
  public TargetSet union(TargetSet pOther) {

    if (pOther instanceof ExplicitTargetSet) {
      return CompoundTargetSet.of(this, (ExplicitTargetSet) pOther);
    } else {
      return pOther.union(this);
    }
  }

  @Override
  public boolean contains(MemorySegment pSegment) {
    return segment.contains(pSegment);
  }

  @Override
  public MemorySegment.Identifier getLowerBound() {
    return segment.getIdentifier();
  }

  @Override
  public MemorySegment.Identifier getUpperBound() {
    return segment.getIdentifier();
  }

  @Override
  public int getSize() {
    return 1;
  }

  @Override
  public Iterable<MemorySegment> getSegments() {
    return ImmutableSet.of(segment);
  }

  @Override
  public int hashCode() {
    return segment.hashCode();
  }

  @Override
  public boolean equals(Object obj) {

    if (this == obj) {
      return true;
    }

    if (!(obj instanceof SingletonTargetSet)) {
      return false;
    }

    return segment.equals(((SingletonTargetSet) obj).segment);
  }

  @Override
  public String toString() {
    return getSegments().toString();
  }
}
