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
package org.sosy_lab.cpachecker.cpa.lock;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Optional;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


public class LockIdentifier implements Comparable<LockIdentifier> {
  public static enum LockType {
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
  private final String variable;
  private final LockType type;

  private LockIdentifier(String pName, String pVariable, LockType pType) {
    name = pName;
    type = pType;
    variable = pVariable;
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
    String varName = getCleanName(var);

    Optional<LockIdentifier> oId = from(createdIds)
      .firstMatch(id -> id.name.equals(name) && id.type == type && id.variable.equals(varName));

    if (oId.isPresent()) {
      return oId.get();
    }

    LockIdentifier newId = new LockIdentifier(name, varName, type);
    createdIds.add(newId);
    return newId;
  }

  public boolean hasEqualNameAndVariable(String lockName, String variableName) {
    return (name.equals(lockName) && variable.equals(variableName));
  }

  public String getName() {
    return name;
  }

  private static String getCleanName(String originName) {
    if (originName != null) {
      String newName = originName.replaceAll("\\(", "");
      newName = newName.replaceAll("\\)", "");
      newName = newName.replaceAll("___\\d*", "");
      return newName;
    } else {
      return null;
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Objects.hashCode(name);
    result = prime * result + Objects.hashCode(type);
    result = prime * result + Objects.hashCode(variable);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null ||
        getClass() != obj.getClass()) {
      return false;
    }
    LockIdentifier other = (LockIdentifier) obj;
    return Objects.equals(name, other.name)
        && Objects.equals(type, other.type)
        && Objects.equals(variable, other.variable);
  }

  @Override
  public String toString() {
    return name + ( variable != "" ? ("(" + variable + ")") : "" );
  }

  @Override
  public int compareTo(LockIdentifier pO) {
    int result = this.name.compareTo(pO.name);
    if (result != 0) {
      return result;
    }
    return this.variable.compareTo(pO.variable);
  }

}
