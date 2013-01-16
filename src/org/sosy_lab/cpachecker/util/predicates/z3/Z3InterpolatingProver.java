/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.z3;

import java.util.List;

import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;


public class Z3InterpolatingProver implements InterpolatingTheoremProver<Integer> {

  @Override
  public void init() {
    // TODO Auto-generated method stub

  }

  @Override
  public void reset() {
    // TODO Auto-generated method stub

  }

  @Override
  public Integer addFormula(BooleanFormula pF) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void popFormula() {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean isUnsat() throws InterruptedException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public BooleanFormula getInterpolant(List<Integer> pFormulasOfA) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Model getModel() throws SolverException {
    // TODO Auto-generated method stub
    return null;
  }

}
