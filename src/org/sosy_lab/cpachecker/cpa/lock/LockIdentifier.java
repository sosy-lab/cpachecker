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

import java.util.HashSet;
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
    for (LockIdentifier id : createdIds) {
      if ( id.name.equals(name) && id.type == type && id.variable.equals(varName)) {
        return id;
      }
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
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    result = prime * result + ((variable == null) ? 0 : variable.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    LockIdentifier other = (LockIdentifier) obj;
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    if (type != other.type) {
      return false;
    }
    if (variable == null) {
      if (other.variable != null) {
        return false;
      }
    } else if (!variable.equals(other.variable)) {
      return false;
    }
    return true;
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
    result = this.variable.compareTo(pO.variable);
    return result;
  }

}
