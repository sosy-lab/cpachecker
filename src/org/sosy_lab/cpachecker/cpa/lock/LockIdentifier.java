// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class LockIdentifier implements Comparable<LockIdentifier> {
  private final static Set<LockIdentifier> createdIds = new HashSet<>();

  private final String name;

  LockIdentifier(String pName) {
    name = pName;
  }

  public static LockIdentifier of(String name) {
    return LockIdentifier.of(name, "");
  }

  public static LockIdentifier of(String name, String var) {
    LockIdentifier newId;
    if (var.isEmpty()) {
      newId = new LockIdentifier(name);
    } else {
      newId = new LockIdentifierWithVariable(name, var);
    }

    // Equals after because of some updates of varName
    for (LockIdentifier id : createdIds) {
      if (id.equals(newId)) {
        return id;
      }
    }

    createdIds.add(newId);
    return newId;
  }

  public String getName() {
    return name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof LockIdentifier)) {
      return false;
    }
    LockIdentifier other = (LockIdentifier) obj;
    return Objects.equals(name, other.name);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int compareTo(LockIdentifier pO) {
    return this.name.compareTo(pO.name);
  }
}
