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
package org.sosy_lab.cpachecker.util.invariants.balancer;


public class Variable implements Comparable<Variable> {

  private final String name;

  public Variable(String x) {
    name = x;
  }

  public String getName() {
    return name;
  }

  public Variable copy() {
    return new Variable(name);
  }

  @Override
  public int compareTo(Variable v) {
    return name.compareTo(v.name);
  }

  @Override
  public boolean equals(Object o) {
    boolean ans = false;
    if (o instanceof Variable) {
      Variable v = (Variable) o;
      ans = this.name.equals(v.name);
    }
    return ans;
  }

  /**
   * HashSet only looks to the equals method if the hashCodes of the
   * two objects are the same.
   */
  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public String toString() {
    return name;
  }

}
