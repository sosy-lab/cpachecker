/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
      return this.function.compareTo(((LocalVariableIdentifier) pO).function);
    } else if (pO instanceof GlobalVariableIdentifier) {
      return -1;
    } else {
      return 1;
    }
  }
}
