/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value.type;


public final class NegativeNaN extends Number{
  private static final long serialVersionUID = 1L;

      public static final Number VALUE = new NegativeNaN();

      private NegativeNaN() {
      }

      @Override
      public double doubleValue() {
        return Double.NaN;
      }

      @Override
      public float floatValue() {
        return Float.NaN;
      }

      @Override
      public int intValue() {
        return (int) Double.NaN;
      }

      @Override
      public long longValue() {
        return (long) Double.NaN;
      }

      @Override
      public String toString() {
        return "-NaN";
      }

      @Override
      public boolean equals(Object pObj) {
        return pObj == this || pObj instanceof NegativeNaN;
      }

      @Override
      public int hashCode() {
        return -1;
      }

}
