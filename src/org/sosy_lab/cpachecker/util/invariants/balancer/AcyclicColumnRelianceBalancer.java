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
package org.sosy_lab.cpachecker.util.invariants.balancer;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.invariants.Rational;
import org.sosy_lab.cpachecker.util.invariants.balancer.prh3.PivotRowHandler;
import org.sosy_lab.cpachecker.util.invariants.redlog.RedlogInterface;


public class AcyclicColumnRelianceBalancer extends AbstractBalancer {

  public AcyclicColumnRelianceBalancer(Configuration config, LogManager lm) {
    logger = lm;
    RLI = new RedlogInterface(config, logger);
  }

  @Override
  public boolean balance(TemplateNetwork tn) throws RefinementFailedException {
    logger.log(Level.FINEST, "Attempting to balance template network with RREF heuristic.");
    boolean succeed = false;
    tnet = tn;
    // Create Variables, and a map from parameter Strings to Variables.
    paramVars = makeParamVars();

    // Build all the matrices
    List<Matrix> mats = new Vector<>();
    for (Transition t : tnet.getTransitions()) {
      mats.addAll(getMatricesForTransition(t));
    }
    matrices = mats;
    logger.log(Level.ALL, "Transformed network transitions into matrices.");
    logMatrices();
    // Put them in RREF as far as possible without pivoting on any entries with variable numerator.
    innocuousRREF();
    logger.log(Level.ALL, "Put matrices in partial RREF, stopping when all potential pivots had variable numerator.");
    logMatrices();

    // Try to find a solution.
    Map<String, Rational> solution = solve();

    // Examine the results.
    if (solution == null) {
      logger.log(Level.FINEST, "MatrixBalancer could not find any solution.");
      succeed = false;
    } else {
      // Set parameters to zero for which Redlog specified no value.
      fillInZeros(solution, paramVars.keySet());
      // Now evaluate the tnet.
      succeed = tnet.evaluate(solution);
      if (!succeed) {
        logger.log(Level.FINEST, "Redlog appears to have completed, although not all parameters received values. Check for 'infinity' values.");
        logger.log(Level.ALL, "Templates after attempted evaluation:\n", tnet.dumpTemplates());
      }
    }
    return succeed;
  }

  private Map<String, Rational> solve() {
    // Declare the return value.
    Map<String, Rational> values = null;

    // Initialize an AssumptionManager.
    AssumptionManager amgr = new AssumptionManager(matrices, logger);

    boolean tryAgain = true;
    while (tryAgain) {

      // We try to get through all the matrices, without raising a bad assumptions exception.
      // If we make it all the way through, then we managed to find a sufficient set of assumptions
      // on the parameters, for all the matrices to have solutions in nonnegative numbers.
      // Finally, we check whether Redlog can find values for the parameters that satisfy these
      // assumptions.

      try {
        Matrix m = amgr.nextMatrix();
        while (m != null) {
          logger.log(Level.ALL, "Working on matrix:","\n"+m.toString());
          m.putInRREF(amgr, logger);
          PivotRowHandler prh = new PivotRowHandler(m, amgr, this, logger);
          prh.solve();
          m = amgr.nextMatrix();
        }

        // If we made it this far, then we managed to compute a sufficient set of conditions on
        // the parameters. So we retrieve it, and then ask Redlog to compute values for the parameters.

        // Retrieve the assumption set:
        AssumptionSet aset = amgr.getCurrentAssumptionSet();
        // Pass it to Redlog:
        values = tryAssumptionSet(aset);
        // Review the results:
        if (values == null) {
          // Redlog didn't find values for the parameters, so we will go back and try again.
          throw new BadAssumptionsException();
        } else {
          // We found an assumption set that works! We return it.
          break;
        }
      } catch (BadAssumptionsException e) {
        // We wind up here if anything went wrong, anywhere in the process.
        // It is now time to backtrack, i.e. to ask the assumption manager to take us back in time
        // to the last point at which we made a "possibly unnecessary" assumption. We'll carry on
        // from there. Or, if there are no options left, then 'tryAgain' will be false, and we will
        // exit the while loop. 'values' will still be null, indicating that we failed to find
        // values for the parameters.
        tryAgain = amgr.nextBranch();
      }

    }

    return values;
  }

}
