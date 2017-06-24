/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.logging.Level.SEVERE;

import com.google.common.collect.Lists;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.ProverEnvironment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class DnfTransformation extends BooleanFormulaTransformationVisitor {

  private final static int MAX_CLAUSES = 1_000_000;

  private final LogManager logger;

  private ShutdownNotifier shutdownNotifier;

  private final BooleanFormulaManager fmgr;

  private final Supplier<ProverEnvironment> proverEnvironmentSupplier;

  DnfTransformation(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      FormulaManagerView pFmgr,
      Supplier<ProverEnvironment> pProverEnvironmentSupplier) {
    super(pFmgr);
    logger = checkNotNull(pLogger);
    shutdownNotifier = checkNotNull(pShutdownNotifier);
    fmgr = pFmgr.getBooleanFormulaManager();
    proverEnvironmentSupplier = checkNotNull(pProverEnvironmentSupplier);
  }

  @Override
  public BooleanFormula visitAnd(List<BooleanFormula> pProcessedOperands) {
    Collection<BooleanFormula> clauses = Lists.newArrayList(fmgr.makeTrue());

    List<Set<BooleanFormula>> operands =
        pProcessedOperands
            .stream()
            .map(f -> fmgr.toDisjunctionArgs(f, false))
            .sorted(Comparator.comparingInt(Set::size))
            .collect(Collectors.toList());

    try (ProverEnvironment proverEnvironment = proverEnvironmentSupplier.get()) {

      for (Set<BooleanFormula> childOperands : operands) {
        clauses =
            clauses
                .stream()
                .flatMap(c -> childOperands.stream().map(co -> fmgr.and(c, co)))
                .filter(f -> isSat(proverEnvironment, f))
                .collect(Collectors.toCollection(ArrayList::new));

        // Give up and return original formula.
        if (clauses.size() > MAX_CLAUSES || shutdownNotifier.shouldShutdown()) {
          return fmgr.and(pProcessedOperands);
        }
      }
    }

    return fmgr.or(clauses);
  }

  private boolean isSat(ProverEnvironment pProverEnvironment, BooleanFormula pFormula) {
    if (shutdownNotifier.shouldShutdown()) {
      return false;
    }

    pProverEnvironment.push(pFormula);
    boolean isSat;
    try {
      isSat = !pProverEnvironment.isUnsat();

    } catch (SolverException e) {
      logger.logException(SEVERE, e, null);
      return true;

    } catch (InterruptedException e) {
      return false;

    } finally {
      pProverEnvironment.pop();
    }
    return isSat;
  }
}
