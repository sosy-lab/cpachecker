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

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.invariants.Rational;
import org.sosy_lab.cpachecker.util.invariants.redlog.RedlogInterface;


public class WeispfenningBalancer extends AbstractBalancer {

  private List<WeispfenningSystem> wsystems;

  public WeispfenningBalancer(Configuration config, LogManager lm) {
    logger = lm;
    RLI = new RedlogInterface(config, logger);
  }

  @Override
  public boolean balance(TemplateNetwork tn) throws RefinementFailedException {
    logger.log(Level.FINEST, "Attempting to balance template network with "+
        "Weispfenning's elimination-by-substitution-of-test-points method.");
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

    // Build Weispfenning Systems
    buildWeispfenningSystems();

    // Try to find a solution.
    Map<String, Rational> solution = solve();

    // Examine the results.
    if (solution == null) {
      logger.log(Level.FINEST, "WeispfenningBalancer could not find any solution.");
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
    int n = wsystems.size();
    AssumptionSet[] asets = new AssumptionSet[n];
    AssumptionSet aset;
    Map<String, Rational> values = null;
    int j = 0;
    while (true) {
      //diag:
      System.out.println(Integer.toString(j)+"\n");
      //
      aset = wsystems.get(j).solve();
      if (aset == null) {
        if (j == 0) {
          return null;
        } else {
          j--;
        }
      } else {
        if (j == 0) {
          asets[j] = aset;
        } else {
          asets[j] = asets[j-1].union(aset);
        }
        if (j < n - 1) {
          j++;
          wsystems.get(j).reinit();
        } else {
          values = tryAssumptionSet(asets[n-1]);
          if (values != null) {
            return values;
          }
        }
      }
    }

    /*
    System.out.println("--------------------------------------------");
    for (WeispfenningSystem w : wsystems) {
      System.out.println(w.toString()+"\n\n");
    }
    */

    /*
    System.out.println("============================================");
    WeispfenningSystem w = wsystems.get(3);
    System.out.println(w.toString()+"\n\n");
    w = w.eliminateRow(1, 1);
    System.out.println(w.toString()+"\n\n");

    System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
    w = wsystems.get(3);
    System.out.println(w.toString()+"\n\n");
    try {
      w = w.eliminateEquations();
      System.out.println(w.toString()+"\n"+w.getAssumptionSet().toString()+"\n\n");
    } catch (BadAssumptionsException e) {
      System.out.println("There was a bad assumption.\n\n");
    }
    */

    /*
    System.out.println("============================================");
    WeispfenningSystem w = wsystems.get(3);
    System.out.println(w.toString()+"\n\n");
    for (int i = 0; i < 10; i++) {
      AssumptionSet aset = w.solve();
      if (aset == null) { break; }
      System.out.println(aset+"\n");
    }
    System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
    */
  }

  private void buildWeispfenningSystems() {
    wsystems = new Vector<>();
    for (Matrix a : matrices) {
      int m = a.getRowNum();
      int n = a.getColNum();
      int ac = a.getNumAugCols();
      int lc = n - ac; // number of "left columns"
      RationalFunction f;
      WeispfenningSystem w;
      for (int k = 0; k < ac; k++) {
        Matrix b = new Matrix(m, lc+1);
        for (int i = 0; i < m; i++) {
          for (int j = 0; j < lc; j++) {
            f = a.getEntry(i, j);
            b.setEntry(i, j, f);
          }
          f = a.getEntry(i, lc+k);
          f = RationalFunction.makeNegative(f);
          b.setEntry(i, lc, f);
        }
        w = new WeispfenningSystem(b);
        wsystems.add(w);
      }
    }
  }

}













