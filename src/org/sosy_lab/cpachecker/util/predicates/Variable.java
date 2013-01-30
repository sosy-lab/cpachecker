/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates;


public class Variable<Type> {
  private final String name;
  private final Type type;

  private Variable(String pName, Type pType) {
    super();
    name = pName;
    type = pType;
  }

  public String getName(){
    return name;
  }

  public Type getType(){
    assert type != null;
    return type;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
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
    Variable<?> other = (Variable<?>) obj;
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    if (type instanceof org.sosy_lab.cpachecker.cfa.types.Type) {
      return ((org.sosy_lab.cpachecker.cfa.types.Type)type).toASTString(name);
    } else {
      return type + " " + name;
    }
  }

  public Variable<Type> withName(String newName) {
    return Variable.create(newName, type);
  }

  public Variable<Type> withType(Type pType) {
    return Variable.create(name, pType);
  }

  public static <Type> Variable<Type> create(String pName, Type pT) {
    return new Variable<>(pName, pT);
  }
}