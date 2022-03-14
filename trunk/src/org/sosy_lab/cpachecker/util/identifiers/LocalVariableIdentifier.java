// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.identifiers;

import com.google.common.base.Strings;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

@SuppressWarnings("EqualsGetClass") // should be refactored
public class LocalVariableIdentifier extends VariableIdentifier {
  protected @NonNull String function; // function, where this variable was declared

  public LocalVariableIdentifier(String nm, CType t, String func, int dereference) {
    super(nm, t, dereference);
    function = Strings.nullToEmpty(func);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hashCode(function);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj) || getClass() != obj.getClass()) {
      return false;
    }
    LocalVariableIdentifier other = (LocalVariableIdentifier) obj;
    return Objects.equals(function, other.function);
  }

  @Override
  public LocalVariableIdentifier cloneWithDereference(int pDereference) {
    return new LocalVariableIdentifier(name, type, function, pDereference);
  }

  public String getFunction() {
    return function;
  }

  @Override
  public boolean isGlobal() {
    return false;
  }

  @Override
  public String toLog() {
    return "l;" + name + ";" + dereference;
  }

  @Override
  public GeneralIdentifier getGeneralId() {
    return new GeneralLocalVariableIdentifier(name, type, function, dereference);
  }

  @Override
  public int compareTo(AbstractIdentifier pO) {
    if (pO instanceof LocalVariableIdentifier) {
      int result = super.compareTo(pO);
      if (result != 0) {
        return result;
      }
      return function.compareTo(((LocalVariableIdentifier) pO).function);
    } else if (pO instanceof GlobalVariableIdentifier) {
      return -1;
    } else {
      return 1;
    }
  }
}
