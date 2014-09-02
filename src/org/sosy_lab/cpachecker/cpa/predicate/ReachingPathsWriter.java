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

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
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
 * Write all path formulas that reach a specified location
 * (here we consider one path formula as a conjunction of block formulas)
 *
 * The formulas get stored in the SMTLib2 format.
 *
 * The parameter analysis.stopAfterError should be "false" in order
 * to not exclude certain paths of programs that violate one or more properties.
 */
@SuppressWarnings("unused")
public class ReachingPathsWriter {

  private final CFA cfa;
  private final LogManager logger;
  private final AbstractionManager absmgr;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final RegionManager rmgr;

  public ReachingPathsWriter(CFA pCfa, Configuration pConfig, LogManager pLogger,
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

  private BooleanFormula getFormulaBetween(CFANode pStart, CFANode pDestination, ReachedSet pReached) {

    //  PredicateAbstractState state = getPredicateState(e);

    return null;
  }

  public void writeReachingPaths(@Nonnull Appendable pWriteTo, @Nonnull ReachedSet pReached, @Nonnull Set<CFANode> pReachingLocations) throws IOException {
    Preconditions.checkNotNull(pWriteTo);
    Preconditions.checkNotNull(pReached);
    Preconditions.checkNotNull(pReachingLocations);

    // Get the abstract states at the entry location
    CFANode entryLocation = cfa.getMainFunction();
    Collection<AbstractState> entryStates = pReached.getReached(entryLocation);

    // TODO: Extract the formulas of the states
    // and compute the disjunction of all paths that reach the entry location
    BooleanFormula pathsToError = bfmgr.makeBoolean(false);
    for (CFANode dest: pReachingLocations) {
      BooleanFormula pathsToDest = getFormulaBetween(entryLocation, dest, pReached);
    }



    // Convert the formula to a weakest precondition
    // - Negation of the abstract paths to the error location
    BooleanFormula wp = fmgr.makeNegate(pathsToError);
    // - TODO: Reduce to predicates on either global variables or function parameters

    // Write the formula in the SMT-LIB2 format to the target stream
    pWriteTo.append(wp.toString());
  }

  public void writeReachingPaths(Path pWriteTo, ReachedSet pReached, Set<CFANode> pReachingLocations) throws IOException {

    try (Writer w = Files.openOutputFile(pWriteTo)) {
      writeReachingPaths(w, pReached, pReachingLocations);
    }
  }

  public void writeReachingPaths(Path pWriteTo, @Nonnull LogManager pCatchExceptionsTo, ReachedSet pReached, Set<CFANode> pReachingLocations) {
    Preconditions.checkNotNull(pCatchExceptionsTo);

    try {
      writeReachingPaths(pWriteTo, pReached, pReachingLocations);

    } catch (IOException e) {
      pCatchExceptionsTo.logException(Level.WARNING, e, "Writing reaching paths failed!");
    }
  }

}
