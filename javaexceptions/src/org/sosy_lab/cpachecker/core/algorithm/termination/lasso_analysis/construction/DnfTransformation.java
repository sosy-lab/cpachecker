// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

class DnfTransformation extends BooleanFormulaTransformationVisitor {

  private static final int MAX_CLAUSES = 1_000_000;

  private final ShutdownNotifier shutdownNotifier;

  private final BooleanFormulaManager fmgr;

  private final Supplier<ProverEnvironment> proverEnvironmentSupplier;

  private DnfTransformation(
      ShutdownNotifier pShutdownNotifier,
      FormulaManagerView pFmgr,
      Supplier<ProverEnvironment> pProverEnvironmentSupplier) {
    super(pFmgr);
    shutdownNotifier = checkNotNull(pShutdownNotifier);
    fmgr = pFmgr.getBooleanFormulaManager();
    proverEnvironmentSupplier = checkNotNull(pProverEnvironmentSupplier);
  }

  static BooleanFormula transformToDnf(
      BooleanFormula formula,
      FormulaManagerView fmgr,
      ShutdownNotifier shutdownNotifier,
      Supplier<ProverEnvironment> proverEnvironmentSupplier)
      throws InterruptedException, SolverException {
    shutdownNotifier.shutdownIfNecessary();
    final DnfTransformation visitor =
        new DnfTransformation(shutdownNotifier, fmgr, proverEnvironmentSupplier);
    try {
      return fmgr.getBooleanFormulaManager().transformRecursively(formula, visitor);
    } catch (DnfTransformationException e) {
      Throwables.throwIfInstanceOf(e.getCause(), InterruptedException.class);
      Throwables.throwIfInstanceOf(e.getCause(), SolverException.class);
      throw new AssertionError(e);
    }
  }

  @Override
  public BooleanFormula visitAnd(List<BooleanFormula> pProcessedOperands) {
    ImmutableList<BooleanFormula> clauses = ImmutableList.of(fmgr.makeTrue());

    ImmutableList<Set<BooleanFormula>> operands =
        pProcessedOperands.stream()
            .map(f -> fmgr.toDisjunctionArgs(f, false))
            .sorted(Comparator.comparingInt(Set::size))
            .collect(ImmutableList.toImmutableList());

    try (ProverEnvironment proverEnvironment = proverEnvironmentSupplier.get()) {

      for (Set<BooleanFormula> childOperands : operands) {
        shutdownNotifier.shutdownIfNecessary();

        List<BooleanFormula> tempList = new ArrayList<>();
        for (BooleanFormula clause : clauses) {
          List<BooleanFormula> list =
              childOperands.stream()
                  .map(co -> fmgr.and(clause, co))
                  .collect(Collectors.toCollection(ArrayList::new));
          for (BooleanFormula bf : list) {
            if (isSat(proverEnvironment, bf)) {
              tempList.add(bf);
            }
          }
        }
        clauses = ImmutableList.copyOf(tempList);

        // Give up and return original formula.
        if (clauses.size() > MAX_CLAUSES) {
          shutdownNotifier.shutdownIfNecessary();
          return fmgr.and(pProcessedOperands);
        }
      }
    } catch (InterruptedException | SolverException e) {
      // The exception can't be propagated here, because we're overwriting a method in which the
      // method signature cannot be changed. Thus, we have to throw an unchecked exceptions here.
      throw new DnfTransformationException(e.getMessage(), e);
    }

    return fmgr.or(clauses);
  }

  private boolean isSat(ProverEnvironment pProverEnvironment, BooleanFormula pFormula)
      throws InterruptedException, SolverException {
    shutdownNotifier.shutdownIfNecessary();

    try {
      pProverEnvironment.push(pFormula);
      return !pProverEnvironment.isUnsat();
    } finally {
      pProverEnvironment.pop();
    }
  }

  private static class DnfTransformationException extends RuntimeException {

    private static final long serialVersionUID = 8329172743374361993L;

    DnfTransformationException(String pMsg, Throwable pCause) {
      super(checkNotNull(pMsg), checkNotNull(pCause));
    }
  }
}
