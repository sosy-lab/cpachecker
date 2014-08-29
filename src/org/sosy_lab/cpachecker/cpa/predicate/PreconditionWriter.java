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
package org.sosy_lab.cpachecker.cpa.predicate;

import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Preconditions;


/**
 * Export of preconditions computed by a predicate-based backwards analysis.
 */
@Options
public class PreconditionWriter {

  private final CFA cfa;
  private final LogManager logger;
  private final AbstractionManager absmgr;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final RegionManager rmgr;

  public PreconditionWriter(CFA pCfa, Configuration pConfig, LogManager pLogger,
      AbstractionManager pAbsmgr, FormulaManagerView pFormulaManager, RegionManager pRmgr)
          throws InvalidConfigurationException {

    pConfig.inject(this);

    cfa = pCfa;
    logger = pLogger;
    absmgr = pAbsmgr;
    fmgr = pFormulaManager;
    bfmgr = pFormulaManager.getBooleanFormulaManager();
    rmgr = pRmgr;
  }

  public void writePreconditionLog(@Nonnull Path pTarget, @Nonnull ReachedSet pReached) {
    try {
      writePrecondition(pTarget, pReached);
    } catch (IOException e) {
      logger.logf(Level.WARNING, "Writing the precondition failed due to an IO problem: %s", e.getMessage());
    }
  }

  public void writePrecondition(@Nonnull Path pTarget, @Nonnull ReachedSet pReached) throws IOException {
    Preconditions.checkNotNull(pTarget);
    Preconditions.checkNotNull(pReached);

    try (Writer w = Files.openOutputFile(pTarget)) {
      writePrecondition(w, pReached);
    }
  }

  public void writePrecondition(@Nonnull Appendable pTarget, @Nonnull ReachedSet pReached) throws IOException {
    Preconditions.checkNotNull(pTarget);
    Preconditions.checkNotNull(pReached);

    // Get the abstract states at the entry location
    CFANode entryLocation = cfa.getMainFunction();
    Collection<AbstractState> entryStates = pReached.getReached(entryLocation);

    // TODO: Extract the formulas of the states
    // and compute the disjunction of all paths that reach the entry location
    BooleanFormula pathsToError = bfmgr.makeBoolean(false);
    for (AbstractState e: entryStates) {
      PredicateAbstractState state = getPredicateState(e);
    }

    // Convert the formula to a weakest precondition
    // - Negation of the abstract paths to the error location
    BooleanFormula wp = fmgr.makeNegate(pathsToError);
    // - TODO: Reduce to predicates on either global variables or function parameters

    // Write the formula in the SMT-LIB2 format to the target stream
    pTarget.append(wp.toString());
  }

}
