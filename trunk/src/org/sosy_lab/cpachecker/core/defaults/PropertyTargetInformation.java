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
import org.sosy_lab.cpachecker.core.specification.Property;

/** Instance of {@link TargetInformation} based on a {@link Property}. */
public final class PropertyTargetInformation implements TargetInformation {

  private final Property property;

  private PropertyTargetInformation(Property pProperty) {
    property = Preconditions.checkNotNull(pProperty);
  }

  public Property getProperty() {
    return property;
  }

  @Override
  public int hashCode() {
    return property.hashCode();
  }

  @Override
  public boolean equals(Object pOther) {
    if (!(pOther instanceof PropertyTargetInformation)) {
      return false;
    }
    return property.equals(((PropertyTargetInformation) pOther).property);
  }

  @Override
  public String toString() {
    return property.toString();
  }

  public static PropertyTargetInformation create(final Property pProperty) {
    return new PropertyTargetInformation(pProperty);
  }

  public static ImmutableSet<TargetInformation> singleton(final Property pProperty) {
    return ImmutableSet.of(PropertyTargetInformation.create(pProperty));
  }
}
