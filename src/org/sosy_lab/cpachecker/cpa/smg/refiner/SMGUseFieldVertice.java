/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import java.util.Objects;

public class SMGUseFieldVertice implements SMGUseVertice {

  private final SMGMemoryPath field;
  private final int argPos;

  public SMGUseFieldVertice(SMGMemoryPath pField, int pArgPos) {
    field = pField;
    argPos = pArgPos;
  }

  @Override
  public int getPosition() {
    return argPos;
  }

  public SMGMemoryPath getField() {
    return field;
  }

  @Override
  public String toString() {
    return "SMGUseFieldVertice [field=" + field + ", argPos=" + argPos + "]";
  }

  @Override
  public int hashCode() {
    return Objects.hash(argPos, field);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SMGUseFieldVertice)) {
      return false;
    }
    SMGUseFieldVertice other = (SMGUseFieldVertice) obj;
    return argPos == other.argPos && Objects.equals(field, other.field);
  }
}