/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ltl.formulas;

public final class BooleanConstant implements LtlFormula {

  public static final BooleanConstant FALSE = new BooleanConstant(false);
  public static final BooleanConstant TRUE = new BooleanConstant(true);

  public final boolean value;

  public static BooleanConstant of(boolean value) {
    return value ? TRUE : FALSE;
  }

  private BooleanConstant(boolean value) {
    this.value = value;
  }

  @Override
  public BooleanConstant not() {
    return value ? FALSE : TRUE;
  }

  @Override
  public int hashCode() {
    return Boolean.hashCode(value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }
    BooleanConstant other = (BooleanConstant) obj;
    return value == other.value;
  }

  @Override
  public String toString() {
    return value ? "true" : "false";
  }
}
