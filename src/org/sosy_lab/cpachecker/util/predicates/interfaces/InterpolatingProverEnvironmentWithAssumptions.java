/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.interfaces;

import java.util.List;

import org.sosy_lab.cpachecker.exceptions.SolverException;


public interface InterpolatingProverEnvironmentWithAssumptions<T> extends InterpolatingProverEnvironment<T> {

  /**
   * Check whether the conjunction of all formulas on the stack is unsatisfiable,
   * while regarding the assumptions from the parameter.
   * @throws InterruptedException
   * @throws SolverException
   */
  public boolean isUnsatWithAssumptions(List<BooleanFormula> assumptions) throws SolverException, InterruptedException;
}
