// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.utils;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/*
 * Used to uniquely identify every string in the abstract state
 */
public class JVariableIdentifier {

  private final Type type;

  private final String varIdentifier;
  private final @Nullable String functionName;

  private final boolean isGlobal;
  private final MemoryLocation memLoc;

  public JVariableIdentifier(Type pType, MemoryLocation pMemLoc) {

    memLoc = pMemLoc;
    type = pType;
    varIdentifier = memLoc.getIdentifier();
    functionName = memLoc.getFunctionName();
    isGlobal = functionName.isEmpty();

  }

  public MemoryLocation getMemLoc() {
    return memLoc;
  }

  public Type getType() {
    return type;
  }

  public String getIdentifier() {
    return varIdentifier;
  }

  public boolean isGlobal() {
    return isGlobal;
  }

  public boolean isString() {
    return HelperMethods.isString(type);
  }

  @Override
  public boolean equals(Object obj) {

    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JVariableIdentifier)) {
      return false;
    }

    JVariableIdentifier other = (JVariableIdentifier) obj;

    return type == other.type
        && varIdentifier.equals(other.varIdentifier)
        && functionName.equals(other.functionName)
        && isGlobal == other.isGlobal;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }


  @Override
  public String toString() {
    return "ID:" + varIdentifier;
  }

  public static class NotJSVar extends JVariableIdentifier {

    private final static NotJSVar instance = new NotJSVar();

    private NotJSVar() {
      super(null, null);
    }

    public static NotJSVar getInstance() {
      return instance;
    }
  }
}
