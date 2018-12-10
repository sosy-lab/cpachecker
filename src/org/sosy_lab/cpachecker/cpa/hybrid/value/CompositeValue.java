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
package org.sosy_lab.cpachecker.cpa.hybrid.value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.ValueVisitor;

public class CompositeValue implements Value {

    private static final long serialVersionUID = 254736985412L;

    // list instead of set, because the same value could occur several times 
    private final List<Value> values;

    public CompositeValue(Collection<Value> pValues) {
        this.values = new ArrayList<>(pValues);
    }

    @Override
    public boolean isNumericValue() {
        return false;
    }

    @Override
    public boolean isUnknown() {
        return false;
    }

    @Override
    public boolean isExplicitlyKnown() {
        return true;
    }

    @Override
    public NumericValue asNumericValue() {
        throw new AssertionError("Composite value cannot be represented by a numeric value.");
    }

    @Override
    public Long asLong(CType pType) {
        throw new AssertionError("Composite value cannot be represented by a numeric value.");
    }

  @Override
  public <T> T accept(ValueVisitor<T> pVisitor) {
    return null;
  }

  /**
   * Creates a returns an immutable copy of the internal values
   * @return The values held by this composite value
   */
  public Collection<Value> getValues() {
      return ImmutableList.copyOf(values);
  }

  @Override
  public String toString() {
      return values.toString(); // output like [1,3,4]
  }

}