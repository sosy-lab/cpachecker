/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.core.algorithm.legion;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

public class Utils {

    /**
     * Convert a value object (something implementing 
     * org.sosy_lab.cpachecker.cpa.value.type.Value) into it's concrete
     * implmenentation.
     */
    @SuppressWarnings("InstanceOfAndCastMatchWrongType")
    public static Value toValue(Object value){
        if (value instanceof Boolean) {
            return BooleanValue.valueOf((Boolean) value);
        } else if (value instanceof Integer) {
            return new NumericValue((Integer) value);
        } else if (value instanceof Character) {
            return new NumericValue((Integer) value);
        } else if (value instanceof Float) {
            return new NumericValue((Float) value);
        } else if (value instanceof Double) {
            return new NumericValue((Double) value);
        } else if (value instanceof BigInteger) {
            BigInteger v = (BigInteger) value;
            return new NumericValue(v);
        } else {
            throw new IllegalArgumentException(
                    String.format(
                            "Did not recognize value for loadedValues Map: %s.",
                            value.getClass()));
        }
    }
}
