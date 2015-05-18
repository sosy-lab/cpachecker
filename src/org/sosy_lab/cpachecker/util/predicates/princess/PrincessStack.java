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
package org.sosy_lab.cpachecker.util.predicates.princess;

import java.util.List;
import java.util.Set;

import scala.Option;
import ap.SimpleAPI;
import ap.parser.IFormula;

/** This is a Interface for the Wrapper around some parts of the PrincessAPI.
 * It allows to have a stack with operations like: push, pop, assert, checkSat, getInterpolants, getModel.
 * A stack is always connected with a PrincessEnvironment, because Variables are declared there.
 * One PrincessEnvironment can manage several stacks. */
public interface PrincessStack {

  PrincessEnvironment getEnv();

  void push(int levels);

  void pop(int levels);

  void assertTerm(IFormula booleanFormula);

  void assertTermInPartition(IFormula booleanFormula, int index);

  boolean checkSat();

  SimpleAPI.PartialModel getPartialModel();

  Option<Object> evalPartial(IFormula pFormula);

  List<IFormula> getInterpolants(List<Set<Integer>> partitions);

  void close();
}
