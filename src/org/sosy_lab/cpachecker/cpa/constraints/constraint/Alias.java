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
package org.sosy_lab.cpachecker.cpa.constraints.constraint;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

/**
 * Alias name for an identifier. Represents an identifier at a special point at runtime.
 * Stores whether the alias was newly created in the last transfer.
 */
public class Alias implements Value {
  private String identifierName;
  private long id;

  private Alias(String pIdentifierName, long pId) {
    identifierName = pIdentifierName;
    id = pId;
  }

  public static Alias createAlias(String pIdentifierName) {
    return new Alias(pIdentifierName, 0);
  }

  public static Alias createNextAlias(Alias pAlias) {
    return new Alias(pAlias.identifierName, pAlias.id + 1);
  }

  public long getAliasNumber() {
    return id;
  }

  public String getIdentifierName() {
    return identifierName;
  }

  @Override
  public String toString() {
    return identifierName + id;
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
