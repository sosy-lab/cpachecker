// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock;

import com.google.common.base.CharMatcher;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class LockIdentifier implements Comparable<LockIdentifier> {

  public enum LockType {
    MUTEX,
    GLOBAL_LOCK,
    LOCAL_LOCK,
    SPINLOCK;

    public String toASTString() {
      return name().toLowerCase();
    }
  }

  private static Set<LockIdentifier> createdIds;

  private final String name;
  private final LockType type;

  LockIdentifier(String pName, LockType pType) {
    name = pName;
    type = pType;
  }

  public static LockIdentifier of(String name) {
    return LockIdentifier.of(name, "");
  }

  public static LockIdentifier of(String name, String var) {
    return LockIdentifier.of(name, var, LockType.GLOBAL_LOCK);
  }

  public static LockIdentifier of(String name, String var, LockType type) {
    if (createdIds == null) {
      createdIds = new HashSet<>();
    }
    LockIdentifier newId;
    if (var.isEmpty()) {
      newId = new LockIdentifier(name, type);
    } else {
      String varName = getCleanName(var);
      newId = new LockIdentifierWithVariable(name, varName, type);
    }

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

  private static String getCleanName(String originName) {
    if (originName != null) {
      String newName = CharMatcher.anyOf("()").removeFrom(originName);
      newName = newName.replaceAll("___\\d*", "");
      return newName;
    } else {
      return null;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type);
  }

  @Override
  // refactoring would be better, but currently safe for the existing subclass
  @SuppressWarnings("EqualsGetClass")
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    LockIdentifier other = (LockIdentifier) obj;
    return Objects.equals(name, other.name) && Objects.equals(type, other.type);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int compareTo(LockIdentifier pO) {
    return name.compareTo(pO.name);
  }
}
