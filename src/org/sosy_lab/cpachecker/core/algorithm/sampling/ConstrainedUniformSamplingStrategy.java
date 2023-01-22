// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.sampling;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BasicProverEnvironment;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "sampling.strategy.constrainedUniform")
public class ConstrainedUniformSamplingStrategy implements SamplingStrategy {

  @Option(secure = true, description = "The minimum value for each variable in the sample.")
  private int lowerBound = 0;

  @Option(secure = true, description = "The maximum value for each variable in the sample.")
  private int upperBound = 10;

  private Random random = new Random();

  public ConstrainedUniformSamplingStrategy(Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  private long sampleUniform() {
    // Upper bound is exclusive
    // TODO: When drawing random values take the intersection of this interval and the known valid
    //       values for a variable (derived from formulas on solver stack)
    return random.nextLong(upperBound - lowerBound + 1) + lowerBound;
  }

  public Set<BooleanFormula> addVariableConstraints(
      FormulaManagerView fmgr, Iterable<Formula> variableFormulas, BasicProverEnvironment<?> prover)
      throws InterruptedException, SolverException {
    BitvectorFormulaManagerView bvmgr = fmgr.getBitvectorFormulaManager();
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();

    Set<BooleanFormula> constraints = new HashSet<>();
    for (Formula variableFormula : variableFormulas) {
      // TODO: We assume all variables are bitvectors for now
      BitvectorFormula variable = (BitvectorFormula) variableFormula;
      BooleanFormula constraint = bfmgr.makeTrue();
      boolean unsat = true;

      // TODO: Should break if no value is found so that formula stays SAT
      while (unsat) {
        // Create random variable assignment
        long lVal = sampleUniform();
        // TODO: How to create signed bitvector? Currently negative numbers are treated as
        //       unsigned, thus leading to bad samples
        BitvectorFormula value = bvmgr.makeBitvector(32, lVal);
        constraint = bvmgr.equal(variable, value);

        // Check if assignment is consistent with other formulas
        prover.push(constraint);
        prover.addConstraint(bfmgr.and(constraints));
        unsat = prover.isUnsat();
        prover.pop();
      }
      constraints.add(constraint);
    }
    return constraints;
  }

  @Override
  public List<ValueAssignment> getModel(
      FormulaManagerView fmgr, Iterable<Formula> variableFormulas, BasicProverEnvironment<?> prover)
      throws InterruptedException, SolverException {
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    List<ValueAssignment> model = null;

    Set<BooleanFormula> variableConstraints =
        addVariableConstraints(fmgr, variableFormulas, prover);
    prover.push(bfmgr.and(variableConstraints));
    if (!prover.isUnsat()) {
      model = prover.getModelAssignments();
    }
    prover.pop();
    return model;
  }
}
