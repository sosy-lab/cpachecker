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
import org.sosy_lab.cpachecker.core.interfaces.Targetable.TargetInformation;

/** Instance of {@link TargetInformation} with just a human-readable string. */
public final class SimpleTargetInformation implements TargetInformation {

  private final String text;

  private SimpleTargetInformation(String pText) {
    text = Preconditions.checkNotNull(pText);
  }

  @Override
  public int hashCode() {
    return text.hashCode();
  }

  @Override
  public boolean equals(Object pOther) {
    if (!(pOther instanceof SimpleTargetInformation)) {
      return false;
    }
    return text.equals(pOther.toString());
  }

  @Override
  public String toString() {
    return text;
  }

  public static SimpleTargetInformation create(final String pText) {
    return new SimpleTargetInformation(pText);
  }

  public static ImmutableSet<TargetInformation> singleton(final String pText) {
    return ImmutableSet.of(SimpleTargetInformation.create(pText));
  }
}
