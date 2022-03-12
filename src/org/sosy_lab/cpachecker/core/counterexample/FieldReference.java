// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.counterexample;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;

/**
 * Is used to represent a field reference without pointer dereferences.
 *
 * <p>E.g a.b.h. (is typically used with structs)
 */
public final class FieldReference extends LeftHandSide {

  private final List<String> fieldNames;

  public FieldReference(String pName, String pFunctionName, List<String> pFieldNames) {
    super(pName, pFunctionName);
    assert !pFieldNames.isEmpty();
    fieldNames = ImmutableList.copyOf(pFieldNames);
  }

  public FieldReference(String pName, List<String> pFieldNames) {
    super(pName);
    assert !pFieldNames.isEmpty();
    fieldNames = ImmutableList.copyOf(pFieldNames);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    FieldReference other = (FieldReference) obj;

    if (isGlobal()) {
      if (!other.isGlobal()) {
        return false;
      }
    } else if (!getFunctionName().equals(!other.isGlobal() ? other.getFunctionName() : null)) {
      return false;
    }

    return Objects.equals(getName(), other.getName()) && fieldNames.equals(other.fieldNames);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isGlobal() ? 0 : getFunctionName().hashCode());
    result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
    result = prime * result + ((fieldNames == null) ? 0 : fieldNames.hashCode());
    return result;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(super.toString());

    for (String fieldName : fieldNames) {
      result.append("$" + fieldName);
    }

    return result.toString();
  }
}
