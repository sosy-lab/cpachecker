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
package org.sosy_lab.cpachecker.cpa.automaton;

import java.io.Serializable;


/** Represents a local variable of the automaton. So far only integer variables are supported. */
public class AutomatonVariable implements Cloneable, Serializable {
  private static final long serialVersionUID = -6765794863680244559L;
  private int value;
  private String name;
  public AutomatonVariable(String type, String name) {
    if (type.toLowerCase().equals("int") || (type.toLowerCase().equals("integer"))) {
      value = 0;
      this.name = name;
    } else {
      throw new IllegalArgumentException("Only Type int supported");
    }
  }

  public String getName() {
    return name;
  }

  public int getValue() {
    return value;
  }
  public void setValue(int v) {
    value = v;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @Override
  public AutomatonVariable clone() {
    try {
      return (AutomatonVariable)super.clone();
    } catch (CloneNotSupportedException e) {
      throw new InternalError();
    }
  }
  @Override
  public boolean equals(Object pObj) {
    if (super.equals(pObj)) {
      return true;
    }
    if (!(pObj instanceof AutomatonVariable)) {
      return false;
    }
    AutomatonVariable otherVar = (AutomatonVariable) pObj;
    return (this.value == otherVar.value) && this.name.equals(otherVar.name);
  }
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   *
   * I don't use the hashcode, but it should be redefined every time equals is overwritten.
   */
  @Override
  public int hashCode() {
    return this.value + this.name.hashCode();
  }
}
