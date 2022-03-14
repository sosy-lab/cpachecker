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
import org.sosy_lab.cpachecker.cpa.local.LocalTransferRelation;

@SuppressWarnings("EqualsGetClass") // should be refactored
public abstract class SingleIdentifier implements AbstractIdentifier {

  protected String name;
  protected CType type;
  protected int dereference;

  protected SingleIdentifier(String nm, CType tp, int deref) {
    name = nm;
    type = tp;
    dereference = deref;
  }

  @Override
  public int getDereference() {
    return dereference;
  }

  public CType getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean isPointer() {
    return (LocalTransferRelation.findDereference(type) > 0);
  }

  @Override
  public boolean isDereferenced() {
    return dereference > 0;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + dereference;
    result = prime * result + Objects.hashCode(name);
    result = prime * result + ((type == null) ? 0 : type.toASTString("").hashCode());
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
    return dereference == other.dereference
        && Objects.equals(name, other.name)
        && Objects.equals(type.toASTString(""), other.type.toASTString(""));
  }

  @Override
  public String toString() {
    String info = Identifiers.getCharsOf(dereference);
    info += name;
    return info;
  }

  public abstract String toLog();

  public abstract GeneralIdentifier getGeneralId();

  @Override
  public int compareTo(AbstractIdentifier pO) {
    if (!(pO instanceof SingleIdentifier)) {
      return 1;
    } else {
      SingleIdentifier other = (SingleIdentifier) pO;
      int result = name.compareTo(other.name);
      if (result != 0) {
        return result;
      }
      if (type != null) {
        if (other.type != null) {
          result = type.toASTString("").compareTo(other.type.toASTString(""));
          if (result != 0) {
            return result;
          }
        } else {
          return 1;
        }
      } else if (other.type != null) {
        return -1;
      }
      return dereference - other.dereference;
    }
  }
}
