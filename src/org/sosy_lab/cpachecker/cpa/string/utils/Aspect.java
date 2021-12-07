// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.utils;

import org.sosy_lab.cpachecker.cpa.string.domains.AbstractStringDomain;
import org.sosy_lab.cpachecker.cpa.string.domains.DomainType;

/*
 * Represents an aspect or fact about a string.
 * The type of the aspect is dependent on the domain that constructs the aspect.
 */
public class Aspect<T> {

  private AbstractStringDomain<?> domain;

  private T t;
  // t as a string
  private String value;

  public Aspect(AbstractStringDomain<T> pDomain, T pT) {

    domain = pDomain;
    t = pT;
    value = pT.toString();

  }

  public String getValueAsString() {
    return t.toString();
  }

  public AbstractStringDomain<?> getDomain() {
    return domain;
  }

  public T getValue() {
    return t;
  }

  public DomainType getDomainType() {
    return domain.getType();
  }

  @Override
  public String toString() {
    return "(" + getDomainType() + "," + value + ")";
  }

  public static class UnknownAspect extends Aspect<Void> {

    private final static UnknownAspect instance = new UnknownAspect();

    private UnknownAspect() {
      super(null, null);
    }

    public static UnknownAspect getInstance() {
      return instance;
    }
  }

}
