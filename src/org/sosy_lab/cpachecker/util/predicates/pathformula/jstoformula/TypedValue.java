/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula;

import javax.annotation.Nonnull;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

/**
 * Data class that combines the formula encoding of a JavaScript value and a type.
 *
 * <p>JavaScript has no static type system. Values may be of mixed type. For example a value could
 * be a number or a boolean depending on conditions. Hence, the type of a value has to be formula
 * encoded, too.
 *
 * <p>Types are encoded as {@link TypeTags}. A {@link TypedValue} is usually created/managed by
 * {@link TypedValueManager}. Values of JavaScript variables are managed by {@link TypedValues}.
 *
 * @see TypeTags
 * @see TypedValues
 * @see TypedValueManager
 */
class TypedValue {
  @Nonnull private final Formula value;
  @Nonnull private final IntegerFormula type;

  TypedValue(@Nonnull final IntegerFormula pType, @Nonnull final Formula pValue) {
    value = pValue;
    type = pType;
  }

  /** @return Formula encoded value. */
  @Nonnull
  public Formula getValue() {
    return value;
  }

  /**
   * @return Formula encoded type (type tag) of the value.
   * @see TypeTags
   */
  @Nonnull
  public IntegerFormula getType() {
    return type;
  }
}
