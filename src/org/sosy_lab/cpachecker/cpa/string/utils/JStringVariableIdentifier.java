// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.utils;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/*
 * Used to uniquely identify every string in the abstract state
 */
public class JStringVariableIdentifier {

  private final Type type;
  private final MemoryLocation memLoc;

  public JStringVariableIdentifier(Type pType, MemoryLocation pMemLoc) {
    memLoc = pMemLoc;
    type = pType;
  }

  public MemoryLocation getMemLoc() {
    return memLoc;
  }

  public Type getType() {
    return type;
  }

  public boolean isString() {
    return StringCpaUtilMethods.isString(type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(memLoc);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof JStringVariableIdentifier)) {
      return false;
    }
    JStringVariableIdentifier other = (JStringVariableIdentifier) obj;
    return type.equals(other.type) && memLoc.equals(other.memLoc);
  }

  @Override
  public String toString() {
    return "ID:" + memLoc.getIdentifier();
  }
}
