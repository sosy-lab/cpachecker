// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.model;

import com.google.common.collect.ImmutableSet;

final class CompoundTargetSet implements ExplicitTargetSet {

  private final MemorySegment.Identifier lowerBound;
  private final MemorySegment.Identifier upperBound;

  private final ExplicitTargetSet fstTargetSet;
  private final ExplicitTargetSet sndTargetSet;

  private final int size;

  private CompoundTargetSet(
      MemorySegment.Identifier pLowerBound,
      MemorySegment.Identifier pUpperBound,
      ExplicitTargetSet pFstTargetSet,
      ExplicitTargetSet pSndTargetSet) {

    lowerBound = pLowerBound;
    upperBound = pUpperBound;

    fstTargetSet = pFstTargetSet;
    sndTargetSet = pSndTargetSet;

    size = pFstTargetSet.getSize() + pSndTargetSet.getSize();
  }

  static ExplicitTargetSet of(ExplicitTargetSet pFstTargetSet, ExplicitTargetSet pSndTargetSet) {

    MemorySegment.Identifier fstLowerBound = pFstTargetSet.getLowerBound();
    MemorySegment.Identifier sndLowerBound = pSndTargetSet.getLowerBound();
    MemorySegment.Identifier lowerBound =
        fstLowerBound.compareTo(sndLowerBound) < 0 ? fstLowerBound : sndLowerBound;

    MemorySegment.Identifier fstUpperBound = pFstTargetSet.getUpperBound();
    MemorySegment.Identifier sndUpperBound = pSndTargetSet.getUpperBound();
    MemorySegment.Identifier upperBound =
        fstUpperBound.compareTo(sndUpperBound) > 0 ? fstUpperBound : sndUpperBound;

    return new CompoundTargetSet(lowerBound, upperBound, pFstTargetSet, pSndTargetSet);
  }

  @Override
  public AliasType alias(TargetSet pOther) {

    if (pOther instanceof ExplicitTargetSet) {

      ExplicitTargetSet other = (ExplicitTargetSet) pOther;

      if (upperBound.compareTo(other.getLowerBound()) < 0) {
        return AliasType.NOT;
      }

      if (lowerBound.compareTo(other.getUpperBound()) > 0) {
        return AliasType.NOT;
      }

      AliasType aliasType = fstTargetSet.alias(pOther);

      if (aliasType == AliasType.MUST || aliasType == AliasType.MAY) {
        return AliasType.MAY;
      }

      aliasType = sndTargetSet.alias(pOther);

      if (aliasType == AliasType.MUST || aliasType == AliasType.MAY) {
        return AliasType.MAY;
      }

      return AliasType.NOT;

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

    if (upperBound.compareTo(pSegment.getIdentifier()) < 0) {
      return false;
    }

    if (lowerBound.compareTo(pSegment.getIdentifier()) > 0) {
      return false;
    }

    return fstTargetSet.contains(pSegment) || sndTargetSet.contains(pSegment);
  }

  @Override
  public MemorySegment.Identifier getLowerBound() {
    return lowerBound;
  }

  @Override
  public MemorySegment.Identifier getUpperBound() {
    return upperBound;
  }

  @Override
  public int getSize() {
    return size;
  }

  @Override
  public ImmutableSet<MemorySegment> getSegments() {

    Iterable<MemorySegment> fstSegments = fstTargetSet.getSegments();
    Iterable<MemorySegment> sndSegments = sndTargetSet.getSegments();

    ImmutableSet.Builder<MemorySegment> builder = ImmutableSet.builder();
    builder.addAll(fstSegments);
    builder.addAll(sndSegments);

    return builder.build();
  }

  @Override
  public int hashCode() {
    return getSegments().hashCode();
  }

  @Override
  public boolean equals(Object obj) {

    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CompoundTargetSet)) {
      return false;
    }

    return getSegments().equals(((CompoundTargetSet) obj).getSegments());
  }

  @Override
  public String toString() {
    return getSegments().toString();
  }
}
