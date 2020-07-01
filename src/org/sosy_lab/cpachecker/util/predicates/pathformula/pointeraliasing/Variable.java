/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.types.c.CType;

final class Variable {
  private final String name;
  private final CType type;

  private Variable(String pName, CType pType) {
    name = pName;
    type = pType;
  }

  String getName() {
    return name;
  }

  CType getType() {
    return type;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Variable)) {
      return false;
    }
    Variable other = (Variable) obj;
    return name.equals(other.name);
  }

  @Override
  public String toString() {
    return type.toASTString(name);
  }

  static Variable create(String pName, CType pT) {
    CTypeUtils.checkIsSimplified(pT);
    return new Variable(checkNotNull(pName), checkNotNull(pT));
  }
}
