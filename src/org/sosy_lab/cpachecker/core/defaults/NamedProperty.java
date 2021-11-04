// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.core.interfaces.Property;

public final class NamedProperty implements Property {

  private final String text;

  private NamedProperty(String pText) {
    this.text = Preconditions.checkNotNull(pText);
  }

  @Override
  public int hashCode() {
    return text.hashCode();
  }

  @Override
  public boolean equals(Object pOther) {
    if (!(pOther instanceof NamedProperty)) {
      return false;
    }
    return this.text.equals(pOther.toString());
  }

  @Override
  public String toString() {
    return text;
  }

  public static NamedProperty create(final String pText) {
    return new NamedProperty(pText);
  }

  public static ImmutableSet<Property> singleton(final String pText) {
    return ImmutableSet.of(NamedProperty.create(pText));
  }

}
