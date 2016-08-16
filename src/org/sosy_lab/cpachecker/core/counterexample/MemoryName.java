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
package org.sosy_lab.cpachecker.core.counterexample;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;

import javax.annotation.Nullable;

/**
 * Implementations of this interface provide the concrete state
 * with the name of the allocated memory, which stores the value for
 * the given address and expression.
 */
public interface MemoryName {

  /**
   * Returns the allocated memory name that stores the value
   * of the given {@link CExpression} exp with the given {@link Address} address.
   *
   * @param exp The value of this expression is requested.
   * @param address The requested value is expected to be at this address.
   * @return The name of the memory that holds the value for the given expression at the given address.
   */
  public String getMemoryName(@Nullable CRightHandSide exp, @Nullable Address address);

}