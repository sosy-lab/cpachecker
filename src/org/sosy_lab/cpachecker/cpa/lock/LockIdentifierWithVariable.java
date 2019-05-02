/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.lock;

import java.util.Objects;

public class LockIdentifierWithVariable extends LockIdentifier {

  private final String varName;

  LockIdentifierWithVariable(String pName, String var, LockType pType) {
    super(pName, pType);
    assert !var.isEmpty();
    varName = var;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = super.hashCode();
    result = prime * result + Objects.hashCode(varName);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (!super.equals(obj)) {
      return false;
    }
    LockIdentifierWithVariable other = (LockIdentifierWithVariable) obj;
    return Objects.equals(varName, other.varName);
  }

  @Override
  public String toString() {
    return super.toString() + "(" + varName + ")";
  }

  @Override
  public int compareTo(LockIdentifier pO) {
    int result = super.compareTo(pO);
    if (result != 0) {
      return result;
    }
    if (pO instanceof LockIdentifierWithVariable) {
      return this.varName.compareTo(((LockIdentifierWithVariable) pO).varName);
    } else {
      return 1;
    }
  }
}
