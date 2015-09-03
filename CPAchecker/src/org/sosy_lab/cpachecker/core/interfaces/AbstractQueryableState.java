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
package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

/**
 * An AbstractState that evaluates Properties (String-encoded) and
 * returns whether they are satisfied in concrete states represented by the AbstractState.
 */
public interface AbstractQueryableState extends AbstractState {

  public String getCPAName();

  /**
   * Checks whether this AbstractState satisfies the property.
   * Each CPA defines which properties can be evaluated.
   * @param property
   * @return if the property is satisfied
   * @throws InvalidQueryException if the property is not given in the (CPA-specific) syntax
   */
  public boolean checkProperty(String property) throws InvalidQueryException;

  public Object evaluateProperty(String property) throws InvalidQueryException;

  /**
   * Modifies the internal state of this AbstractState.
   * Each CPA defines a separate language for definition of modifications.
   * @param modification
   * @throws InvalidQueryException
   */
  public void modifyProperty(String modification) throws InvalidQueryException;

}
