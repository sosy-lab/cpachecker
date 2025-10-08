// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.identifiers;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class GeneralLocalVariableIdentifier extends LocalVariableIdentifier
    implements AbstractIdentifier {

  public GeneralLocalVariableIdentifier(String pNm, int pDereference) {
    super(pNm, null, "", pDereference);
  }

  public GeneralLocalVariableIdentifier(
      String pNm, CType type, String pFunction, int pDereference) {
    super(pNm, type, pFunction, pDereference);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + getDereference();
    result = prime * result + Objects.hashCode(getName());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SingleIdentifier other = (SingleIdentifier) obj;
    return getDereference() == other.getDereference() && Objects.equals(getName(), other.getName());
  }

  @Override
  public GeneralLocalVariableIdentifier cloneWithDereference(int deref) {
    return new GeneralLocalVariableIdentifier(getName(), getType(), getFunction(), deref);
  }
}
