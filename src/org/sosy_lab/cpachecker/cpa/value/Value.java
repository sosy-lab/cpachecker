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
package org.sosy_lab.cpachecker.cpa.value;

import org.sosy_lab.cpachecker.cfa.types.c.CType;



/**
 * Base class for values that can be tracked by the ValueAnalysisCPA.
 *
 * Traditionally, ValueAnalysisCPA would only keep track of long type values.
 * For the future, floats, symbolic values, and SMG nodes should
 * also be supported.
 */
public interface Value {
  public boolean isNumericValue();

  public boolean isUnknown();

  /** Return the NumericValue if this is a numeric value, null otherwise. **/
  public NumericValue asNumericValue();

  /** Return the long value if this is a long value, null otherwise. **/
  public Long asLong(CType type);

  /** Singleton class used to signal that the value is unknown (could be anything). **/
  public static final class UnknownValue implements Value {

    private static final UnknownValue instance = new UnknownValue();

    @Override
    public String toString() {
      return "UNKNOWN";
    }

    public static UnknownValue getInstance() {
      return instance;
    }

    @Override
    public boolean isNumericValue() {
      return false;
    }

    @Override
    public NumericValue asNumericValue() {
      return null;
    }

    @Override
    public Long asLong(CType type) {
      return null;
    }

    @Override
    public boolean isUnknown() {
      return true;
    }

  }
}
