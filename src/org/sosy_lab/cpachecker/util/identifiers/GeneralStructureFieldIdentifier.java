/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

public class GeneralStructureFieldIdentifier extends StructureFieldIdentifier
    implements GeneralIdentifier {

  public GeneralStructureFieldIdentifier(String pNm, int pDereference) {
    super(pNm, /*"",*/ null, pDereference, null);
  }

  public GeneralStructureFieldIdentifier(
      String pNm, /*String fType,*/ CType type, int pDereference, AbstractIdentifier own) {
    super(pNm, /*fType,*/ type, pDereference, own);
    assert (type != null) : "Creating GeneralStructureFieldIdentifier with null field";
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
  public GeneralStructureFieldIdentifier cloneWithDereference(int deref) {
    return new GeneralStructureFieldIdentifier(name, /* fieldType,*/ type, deref, owner);
  }
}
