// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
