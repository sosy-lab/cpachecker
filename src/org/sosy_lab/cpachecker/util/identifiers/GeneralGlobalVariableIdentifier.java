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

public final class GeneralGlobalVariableIdentifier extends GlobalVariableIdentifier
    implements GeneralIdentifier {

  public GeneralGlobalVariableIdentifier(String pNm, CType type, int pDereference) {
    super(pNm, type, pDereference);
  }

  public GeneralGlobalVariableIdentifier(String pNm, int pDereference) {
    super(pNm, null, pDereference);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + dereference;
    result = prime * result + Objects.hashCode(name);
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
    return dereference == other.dereference && Objects.equals(name, other.name);
  }

  @Override
  public GeneralGlobalVariableIdentifier cloneWithDereference(int deref) {
    return new GeneralGlobalVariableIdentifier(name, type, deref);
  }
}
