/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.value.type.symbolic;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.SymbolicValue;

/**
 * Identifier for basic symbolic values.
 * Symbolic identifiers are used to track equality between
 * variables that have non-deterministic values.
 *
 * <p>Example:
 * <pre>
 *    int a = nondet_int();
 *    int b = a;
 *
 *    if (a != b) {
 * ERROR:
 *    return -1;
 *    }
 * </pre>
 * In the example above, <code>a</code> is assigned a symbolic identifier.
 * <code>b</code> is assigned the same symbolic identifier, so that the condition
 * <code>a != b</code> can be evaluated as <code>false</code>.
 * </p>
 */
public class SymbolicIdentifier implements SymbolicValue {

  // stores the next usable id
  private static long nextId = 0;

  // this objects unique id for identifying it
  private final long id;
  @SuppressWarnings("unused")
  private final Type type;

  protected SymbolicIdentifier(long pId, Type pType) {
    id = pId;
    type = pType;
  }

  /**
   * Returns a new instance of a <code>SymbolicIdentifier</code>.
   *
   * <p>Each call to this method returns a new, unique <code>SymbolicIdentifier</code>.</p>
   *
   * @return a new instance of a <code>SymbolicIdentifier</code>
   */
  static SymbolicIdentifier getInstance(Type pType) {
    return new SymbolicIdentifier(nextId++, pType);
  }

  @Override
  public int hashCode() {
    return (int) (id ^ (id >>> 32));
  }

  @Override
  public boolean equals(Object pOther) {
    return pOther instanceof SymbolicIdentifier && ((SymbolicIdentifier) pOther).id == id;
  }

  @Override
  public String toString() {
    return "SymbolicIdentifier[" + id + "]";
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
}
