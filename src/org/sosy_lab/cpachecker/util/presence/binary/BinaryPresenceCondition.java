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
package org.sosy_lab.cpachecker.util.presence.binary;

import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceCondition;


public class BinaryPresenceCondition implements PresenceCondition {

  private final boolean value;

  public BinaryPresenceCondition(boolean pValue) {
    value = pValue;
  }

  public boolean getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value ? "TRUE" : "FALSE";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (value ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (obj == null) { return false; }
    if (!(obj instanceof BinaryPresenceCondition)) { return false; }
    BinaryPresenceCondition other = (BinaryPresenceCondition) obj;
    if (value != other.value) { return false; }
    return true;
  }
}
