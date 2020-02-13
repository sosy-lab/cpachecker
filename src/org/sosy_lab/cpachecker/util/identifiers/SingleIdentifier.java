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

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.local.LocalTransferRelation;

@SuppressWarnings("EqualsGetClass") // should be refactored
public abstract class SingleIdentifier implements AbstractIdentifier {

  protected String name;
  protected CType type;
  protected int dereference;

  public SingleIdentifier(String nm, CType tp, int deref) {
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
      int result = this.name.compareTo(other.name);
      if (result != 0) {
        return result;
      }
      if (this.type != null) {
        if (other.type != null) {
          result = this.type.toASTString("").compareTo(other.type.toASTString(""));
          if (result != 0) {
            return result;
          }
        } else {
          return 1;
        }
      } else if (other.type != null) {
        return -1;
      }
      return this.dereference - other.dereference;
    }
  }
}
