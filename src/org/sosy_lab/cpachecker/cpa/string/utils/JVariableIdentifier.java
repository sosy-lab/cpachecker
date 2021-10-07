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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

//rename to JSVarRepresenter?
public class JVariableIdentifier {


  private final Type type;
  private final String varIdentifier;
  private final @Nullable String functionName;
  private final boolean isGlobal;
  private final MemoryLocation memLoc;
  private List<Aspect> aspects;

  public JVariableIdentifier(Type pType, MemoryLocation pMemLoc) {
    memLoc = pMemLoc;
    type = pType;
    varIdentifier = memLoc.getIdentifier();
    functionName = memLoc.getFunctionName();
    isGlobal = functionName.isEmpty();
    aspects = ImmutableList.of();
  }

  public JVariableIdentifier addAspects(List<Aspect> pList) {
    aspects = ImmutableList.copyOf(pList);
    return this;
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
    return (type == other.type)
        && (varIdentifier.equals(other.varIdentifier))
        && (functionName.equals(other.functionName))
        && (isGlobal == other.isGlobal);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  // @Override
  // public int compareTo(AbstractIdentifier pO) {
  // if (pO instanceof JVariableIdentifier) {
  // JVariableIdentifier other = (JVariableIdentifier) pO;
  // int result = this.varIdentifier.compareTo(other.varIdentifier);
  // if (result != 0) {
  // return result;
  // }
  // if (this.type != null) {
  // if (other.type != null) {
  // result = this.type.toASTString("").compareTo(other.type.toASTString(""));
  // if (result != 0) {
  // return result;
  // }
  // } else {
  // return 1;
  // }
  // } else if (other.type != null) {
  // return -1;
  // }
  // }
  // return 1;
  // }

  @Override
  public String toString() {
    return "ID:" + varIdentifier // + "," + isGlobal
    ;
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
