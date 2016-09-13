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
package org.sosy_lab.cpachecker.util.predicates.interpolation;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * This is a class that allows to use a different SMT solver for interpolation
 * than for the rest.
 * Whenever it is used, it copies the formulas to the interpolation SMT solver
 * and back accordingly.
 */
public class SeparateInterpolatingProverEnvironment<T>
    implements InterpolatingProverEnvironment<T> {

  private final FormulaManager mainFmgr;
  private final FormulaManager itpFmgr;
  private final InterpolatingProverEnvironment<T> itpEnv;

  public SeparateInterpolatingProverEnvironment(
      FormulaManager pMainFmgr,
      FormulaManager pItpFmgr,
      InterpolatingProverEnvironment<T> pItpEnv) {
    mainFmgr = checkNotNull(pMainFmgr);
    itpFmgr = checkNotNull(pItpFmgr);
    itpEnv = checkNotNull(pItpEnv);
  }

  @Override
  public T push(BooleanFormula mainF) {
    BooleanFormula itpF = itpFmgr.parse(mainFmgr.dumpFormula(mainF).toString());
    return itpEnv.push(itpF);
  }

  @Override
  public void pop() {
    itpEnv.pop();
  }

  @Override
  public T addConstraint(BooleanFormula constraint) {
    return itpEnv.addConstraint(convertToItp(constraint));
  }

  @Override
  public void push() {
    itpEnv.push();
  }

  @Override
  public boolean isUnsat() throws InterruptedException, SolverException {
    return itpEnv.isUnsat();
  }

  @Override
  public boolean isUnsatWithAssumptions(Collection<BooleanFormula> assumptions)
      throws SolverException, InterruptedException {
    return itpEnv.isUnsatWithAssumptions(assumptions);
  }

  @Override
  public void close() {
    itpEnv.close();
  }

  @Override
  public BooleanFormula getInterpolant(List<T> pFormulasOfA)
      throws SolverException, InterruptedException {
    BooleanFormula itpF = itpEnv.getInterpolant(pFormulasOfA);
    return convertToMain(itpF);
  }

  @Override
  public List<BooleanFormula> getSeqInterpolants(List<Set<T>> partitionedFormulas)
      throws SolverException, InterruptedException {
    final List<BooleanFormula> itps = itpEnv.getSeqInterpolants(partitionedFormulas);
    final List<BooleanFormula> result = new ArrayList<>();
    for (BooleanFormula itp : itps) {
      result.add(convertToMain(itp));
    }
    return result;
  }

  @Override
  public List<BooleanFormula> getTreeInterpolants(List<Set<T>> partitionedFormulas, int[] startOfSubTree)
      throws SolverException, InterruptedException {
    final List<BooleanFormula> itps = itpEnv.getTreeInterpolants(partitionedFormulas, startOfSubTree);
    final List<BooleanFormula> result = new ArrayList<>();
    for (BooleanFormula itp : itps) {
      result.add(convertToMain(itp));
    }
    return result;
  }

  private BooleanFormula convertToItp(BooleanFormula f) {
    return itpFmgr.parse(mainFmgr.dumpFormula(f).toString());
  }

  private BooleanFormula convertToMain(BooleanFormula f) {
    return mainFmgr.parse(itpFmgr.dumpFormula(f).toString());
  }

  @Override
  public Model getModel() throws SolverException {
    return itpEnv.getModel();
  }

  @Override
  public ImmutableList<ValueAssignment> getModelAssignments() throws SolverException {
    return itpEnv.getModelAssignments();
  }
}
