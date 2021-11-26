// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.utils;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.cpa.string.domains.AbstractStringDomain;
import org.sosy_lab.cpachecker.cpa.string.domains.DomainType;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect.UnknownAspect;

public class ValueAndAspects {

  // private String value;
  private List<Aspect<?>> aspects;

  public ValueAndAspects(List<Aspect<?>> pAspects) {
    // value = pValue;
    aspects = pAspects;
  }

  public ValueAndAspects newValue(List<Aspect<?>> pAspects) {
    aspects = pAspects;
    return this;
  }

  public int getAspectAmount() {
    return aspects.size();
  }

  public ValueAndAspects updateOneAspect(Aspect<?> as) {
    Aspect<?> temp = getAspectOfDomain(as.getDomain());
    if (temp instanceof UnknownAspect) {
      com.google.common.collect.ImmutableList.Builder<Aspect<?>> builder =
          new com.google.common.collect.ImmutableList.Builder<>();
      aspects = builder.addAll(aspects).add(as).build();

    } else {
      if (!temp.getValue().equals(as.getValue())) {
        com.google.common.collect.ImmutableList.Builder<Aspect<?>> builder =
            new com.google.common.collect.ImmutableList.Builder<>();
        for (Aspect<?> a : aspects) {
          if (!a.equals(temp)) {
            builder.add(a);
          } else {
            builder.add(as);
          }
        }
        aspects = builder.build();
      }
    }
    return this;
  }

  public Aspect<?> getAspectOfDomain(AbstractStringDomain<?> domain) {
    DomainType type = domain.getType();
    for (Aspect<?> a : aspects) {
      if (a.getDomainType().equals(type)) {
        return a;
      }
    }
    return UnknownAspect.getInstance();
  }
  public boolean isLessOrEqual(ValueAndAspects pOther) {
    List<Aspect<?>> otherList = pOther.aspects;
    if (aspects.size() < otherList.size()) {
      return false;
    }

    for (int i = 0; i < otherList.size(); i++) {
      Aspect<?> a = aspects.get(i);
      Aspect<?> other = otherList.get(i);

      // if not in same order or missing aspects.
      if (!other.getDomainType().equals(a.getDomainType())) {
        DomainType type = other.getDomainType();
        for (Aspect<?> temp : aspects) {
          if (temp.getDomainType().equals(type)) {
            a = temp;
            break;
          }
        }
      }
      if (!a.getDomain().isLessOrEqual(a, other)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ValueAndAspects)) {
      return false;
    }
    ValueAndAspects vaa = (ValueAndAspects) obj;
    if ((vaa.aspects.size() != this.aspects.size()) // || svaa.value != this.value
    ) {
      return false;
    }
    for (Aspect<?> a : this.aspects) {
      if (!vaa.aspects.contains(a)) {
        return false;
      }
    }
    return true;
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

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  public static class UnknownValueAndAspects extends ValueAndAspects {

    private final static UnknownValueAndAspects instance = new UnknownValueAndAspects();

    private UnknownValueAndAspects() {
      super(ImmutableList.of());
    }

    public static UnknownValueAndAspects getInstance() {
      return instance;
    }

  }
}
