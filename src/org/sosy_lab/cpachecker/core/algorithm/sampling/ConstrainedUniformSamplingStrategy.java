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

  private Random random = new Random(141592653);

  public ConstrainedUniformSamplingStrategy(Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  private long sampleUniform() {
    // Upper bound is exclusive
    return random.nextLong(upperBound - lowerBound + 1) + lowerBound;
  }

  /**
   * Return a formula expressing that all the given variables are in the interval from which this
   * strategy draws samples.
   */
  private BooleanFormula makeIntervalConstraint(
      FormulaManagerView fmgr, Iterable<Formula> variableFormulas) {
    BitvectorFormulaManagerView bvmgr = fmgr.getBitvectorFormulaManager();
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();

    Set<BooleanFormula> constraints = new HashSet<>();
    for (Formula variableFormula : variableFormulas) {
      // TODO: We assume all variables are bitvectors for now
      BitvectorFormula variable = (BitvectorFormula) variableFormula;

      BitvectorFormula lower = bvmgr.makeBitvector(bvmgr.getLength(variable), lowerBound);
      BooleanFormula lowerConstraint = bvmgr.lessOrEquals(lower, variable, true);
      BitvectorFormula upper = bvmgr.makeBitvector(bvmgr.getLength(variable), upperBound);
      BooleanFormula upperConstraint = bvmgr.lessOrEquals(variable, upper, true);

      constraints.add(bfmgr.and(lowerConstraint, upperConstraint));
    }
    return bfmgr.and(constraints);
  }

  @Override
  public List<ValueAssignment> getModel(
      FormulaManagerView fmgr, Iterable<Formula> variableFormulas, BasicProverEnvironment<?> prover)
      throws InterruptedException, SolverException {
    BitvectorFormulaManagerView bvmgr = fmgr.getBitvectorFormulaManager();
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();

    prover.push(makeIntervalConstraint(fmgr, variableFormulas));

    Set<BooleanFormula> constraints = new HashSet<>();
    for (Formula variableFormula : variableFormulas) {
      // Check if a satisfying assignment even exists
      prover.push(bfmgr.and(constraints));
      if (prover.isUnsat()) {
        // Pop variable constraints added just now
        prover.pop();
        // Pop interval constraint
        prover.pop();
        return null;
      }

      // Try random values until we find an assignment that is consistent with other formulas on the
      // stack
      // TODO: We assume all variables are bitvectors for now
      BitvectorFormula variable = (BitvectorFormula) variableFormula;
      BooleanFormula constraint = bfmgr.makeTrue();
      boolean unsat = true;
      while (unsat) {
        // Create random variable assignment
        long lVal = sampleUniform();
        BitvectorFormula value = bvmgr.makeBitvector(bvmgr.getLength(variable), lVal);
        constraint = bvmgr.equal(variable, value);

        // Check if assignment is consistent with other formulas
        prover.push(constraint);
        unsat = prover.isUnsat();
        prover.pop();
      }
      constraints.add(constraint);

      // Pop previously found constraints
      prover.pop();
    }

    // Pop interval constraint - not needed for final SAT check as all variables are already
    // assigned a value from the interval
    prover.pop();

    prover.push(bfmgr.and(constraints));
    List<ValueAssignment> model = null;
    if (!prover.isUnsat()) {
      model = prover.getModelAssignments();
    }
    prover.pop();
    return model;
  }
}
