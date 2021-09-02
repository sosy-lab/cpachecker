// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.utils;

import java.util.LinkedList;
import java.util.List;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ValueAndAspects {

  private String value;
  private List<Aspect> aspects;

  public ValueAndAspects(String pValue, List<Aspect> pAspects) {
    value = pValue;
    aspects = pAspects;
  }

  public ValueAndAspects(String pValue) {
    value = pValue;
    aspects = new LinkedList<>();
  }

  public void addAspect(Aspect pAdd) {
    if (!aspects.contains(pAdd)) {
      aspects.add(pAdd);
    }
  }

  public boolean emptyValue() {
    return value.isEmpty();
  }

  public int getAspectAmount() {
    return aspects.size();
  }

  public String getValue() {
    return value;
  }

  public ValueAndAspects newValue(String pUpdateValue) {
    return new ValueAndAspects(pUpdateValue);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ValueAndAspects)) {
      return false;
    }
    ValueAndAspects svaa = (ValueAndAspects) obj;
    if ((svaa.aspects.size() != this.aspects.size()) || svaa.value != this.value) {
      return false;
    }
    for (Aspect a : this.aspects) {
      for (Aspect b : svaa.aspects) {
        if (!a.equals(b)) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("(" + value// + ", Domains:"
    );
    // for (Aspect a : aspects) {
    // builder.append(a.toString() + ",");
    // }
    builder.append(")");
    return builder.toString();
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  public class UnknownValueAndAspects {
    private final UnknownValueAndAspects instance = new UnknownValueAndAspects();
    private UnknownValueAndAspects() {
      value = "";
      aspects = new LinkedList<>();
    }

    public UnknownValueAndAspects getInstance() {
      return instance;
    }

  }
}
