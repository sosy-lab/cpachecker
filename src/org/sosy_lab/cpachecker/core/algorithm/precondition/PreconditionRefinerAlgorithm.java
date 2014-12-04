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
package org.sosy_lab.cpachecker.core.algorithm.precondition;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.ReachedSetUtils;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.PredicatedAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Preconditions;

@Options(prefix="precondition")
public class PreconditionRefinerAlgorithm implements Algorithm {

  public static enum PreconditionExportType { NONE, SMTLIB }
  @Option(secure=true,
      name="export.type",
      description="(How) should the precondition be exported?")
  private PreconditionExportType exportPreciditionsAs = PreconditionExportType.NONE;

  @Option(secure=true,
      name="export.target",
      description="Where should the precondition be exported to?")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportPreciditionsTo = Paths.get("precondition.txt");

  private final ReachedSetFactory reachedSetFactory;
  private final Algorithm wrappedAlgorithm;
  private final FormulaManagerView mgrv;
  private final PredicateCPA predcpa;
  private final LogManager logger;

  public PreconditionRefinerAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa, CFA pCfa,
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
          throws InvalidConfigurationException {

    Preconditions.checkNotNull(pConfig).inject(this);

    logger = Preconditions.checkNotNull(pLogger);
    wrappedAlgorithm = Preconditions.checkNotNull(pAlgorithm);
    reachedSetFactory = new ReachedSetFactory(pConfig, pLogger);

    predcpa = Preconditions.checkNotNull(
        CPAs.retrieveCPA(pCpa, PredicateCPA.class),
        "The CPA must be composed of a predicate analysis in order to provide a precondition!");

    mgrv = predcpa.getFormulaManager();
  }

  private BooleanFormula getPreconditionForViolation(ReachedSet pReachedSet) {
    return PreconditionUtils.getPreconditionFromReached(
        mgrv, pReachedSet,
        PreconditionPartition.VIOLATING);
  }

  private BooleanFormula getPreconditionForValidity(ReachedSet pReachedSet) {
    return PreconditionUtils.getPreconditionFromReached(
        mgrv, pReachedSet,
        PreconditionPartition.VALID);
  }

  private boolean isDisjoint(BooleanFormula pP1, BooleanFormula pP2) {
    return false;
  }

  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException,
      PredicatedAnalysisPropertyViolationException {

    boolean result = true;

    final ReachedSet initialReachedSet = reachedSetFactory.create();
    ReachedSetUtils.addReachedStatesToOtherReached(pReachedSet, initialReachedSet);

    do {
      result &= wrappedAlgorithm.run(pReachedSet);

      // Use one set of reached states! Separate state space using a specification automaton!
      BooleanFormula pcViolation = getPreconditionForViolation(pReachedSet);
      BooleanFormula pcValid = getPreconditionForValidity(pReachedSet);

      if (isDisjoint(pcViolation, pcValid)) {
        // TODO: Provide the result somehow
        break;
      }

      // Get arbitrary traces...(without disjunctions)
      // ... one to the location that violates the specification
      PathFormula traceViolation;
      // ... and one to the location that represents the exit location
      PathFormula traceValid;

      // Check the disjointness of the WP for the two traces...

      // Refine the precision so that the
      // abstraction on the two traces is disjoint


      // Restart with the initial set of reached states
      // with the new precision!
      pReachedSet.clear();
      ReachedSetUtils.addReachedStatesToOtherReached(initialReachedSet, pReachedSet);

    } while (true);

    return result;
  }

}
