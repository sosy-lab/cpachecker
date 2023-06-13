// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.refiner;

import java.util.Objects;

public class SMGUseFieldEdge implements SMGUseGraphEdge<SMGUseFieldVertice> {

  private final SMGUseFieldVertice source;
  private final SMGUseFieldVertice target;

  public SMGUseFieldEdge(SMGUseFieldVertice pSource, SMGUseFieldVertice pTarget) {
    source = pSource;
    target = pTarget;
  }

  @Override
  public SMGUseFieldVertice getSource() {
    return source;
  }

  @Override
  public SMGUseFieldVertice getTarget() {
    return target;
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, target);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SMGUseFieldEdge)) {
      return false;
    }
    SMGUseFieldEdge other = (SMGUseFieldEdge) obj;
    return Objects.equals(source, other.source) && Objects.equals(target, other.target);
  }

  @Override
  public String toString() {
    return "SMGUseFieldEdge [source=" + source + ", target=" + target + "]";
  }
}
