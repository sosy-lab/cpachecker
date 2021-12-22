// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.utils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.string.domains.AbstractStringDomain;
import org.sosy_lab.cpachecker.cpa.string.domains.DomainType;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect.UnknownAspect;

/*
 * Stores all known aspects of a string as a list.
 * Util-class to perform functions on the list.
 */
public class AspectSet {

  private ImmutableSortedSet<Aspect<?>> aspects;

  public AspectSet(Set<Aspect<?>> pAspects) {
    aspects = ImmutableSortedSet.copyOf(pAspects);
  }

  public AspectSet newValue(Set<Aspect<?>> pAspects) {
    aspects = ImmutableSortedSet.copyOf(pAspects);
    return this;
  }

  public ImmutableSortedSet<Aspect<?>> getAspects() {
    return aspects;
  }

  public int getAspectAmount() {
    return aspects.size();
  }

  public AspectSet updateOneAspect(Aspect<?> as) {

    Aspect<?> temp = getAspect(as.getDomain());

    if (temp instanceof UnknownAspect) {

      ImmutableSet.Builder<Aspect<?>> builder = new ImmutableSet.Builder<>();
      ImmutableSet<Aspect<?>> tempSet = builder.addAll(aspects).add(as).build();
      aspects = ImmutableSortedSet.copyOf(tempSet);
    } else {
      if (!temp.getValue().equals(as.getValue())) {
        ImmutableSet.Builder<Aspect<?>> builder = new ImmutableSet.Builder<>();
        for (Aspect<?> a : aspects) {
          if (!a.equals(temp)) {
            builder.add(a);
          } else {
            builder.add(as);
          }
        }
        ImmutableSet<Aspect<?>> tempSet = builder.build();
        aspects = ImmutableSortedSet.copyOf(tempSet);
      }
    }
    return this;
  }

  public Aspect<?> getAspect(AbstractStringDomain<?> domain) {
    if (aspects.isEmpty()) {
      return UnknownAspect.getInstance();
    }
    DomainType type = domain.getType();
    return getAspect(type);
  }

  public Aspect<?> getAspect(DomainType pDomainType) {

    if (aspects.isEmpty()) {
      return UnknownAspect.getInstance();
    }
    for (Aspect<?> a : aspects) {
      if (!(a instanceof UnknownAspect) && a.getDomainType().equals(pDomainType)) {
        return a;
      }
    }
    return UnknownAspect.getInstance();
  }

  public boolean isLessOrEqual(AspectSet pOther) {
    ImmutableSortedSet<Aspect<?>> otherSet = pOther.aspects;
    if (aspects.size() < otherSet.size()) {
      return false;
    }
    if (aspects.size() == 0 && otherSet.size() == 0) {
      return false;
    }
    List<Aspect<?>> aspectsAsList = aspects.asList();
    List<Aspect<?>> otherAspectsAsList = otherSet.asList();
    for (int i = 0; i < otherAspectsAsList.size(); i++) {
      Aspect<?> thisAspect = aspectsAsList.get(i);
      Aspect<?> other = otherAspectsAsList.get(i);
      if (thisAspect instanceof UnknownAspect || other instanceof UnknownAspect) {
        return false;
      }
      if (!thisAspect.getDomain().isLessOrEqual(thisAspect, other)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(aspects);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof AspectSet)) {
      return false;
    }
    AspectSet other = (AspectSet) obj;
    return Objects.equals(aspects, other.aspects);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("(Domains:");
    for (Aspect<?> a : aspects) {
      builder.append(a.toString() + ",");
    }
    builder.append(")");
    return builder.toString();
  }
}
