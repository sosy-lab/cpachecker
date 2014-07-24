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
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption.AssumptionType;
import org.sosy_lab.cpachecker.util.invariants.balancer.MatrixSolvingFailedException.Reason;
import org.sosy_lab.cpachecker.util.invariants.balancer.prh12.PivotRowHandler;


public class MatrixSolver {

  private LogManager logger;
  private Matrix matrix;

  public MatrixSolver(Matrix mx, LogManager lm) {
    logger = lm;
    matrix = mx;
  }

  public Set<AssumptionSet> solve() throws MatrixSolvingFailedException {
    return solve(new AssumptionSet());
  }

  public Set<AssumptionSet> solve(AssumptionSet init) throws MatrixSolvingFailedException {
    // Declare/Initialize sets of assumptions:

    // Polynomials we are assuming to be zero, since assuming they were nonzero turned out
    // to make the matrix unsolvable (initially empty):
    ZeroPolynomialManager zeroPolyMan = new ZeroPolynomialManager();
    AssumptionSet zeros = zeroPolyMan.next();

    // Polynomials assumed to be nonzero during an RREF process:
    AssumptionSet rrefnonzeros;
    // RationalFunctiosn assumed to be zero since they wound up in an "almost zero row"
    // of the matrix after it was put in RREF:
    AssumptionSet azrZeros;

    // Inequalities assumed on the pivot rows:
    Set<AssumptionSet> pivotAssumptionSets = null;

    while (true) {
      // Make a copy of the matrix.
      Matrix mat = matrix.copy();
      logger.log(Level.ALL, "Basic matrix is:","\n"+mat.toString());
      // First apply any substitutions available in the passed assumption set 'init'.
      zeroSubs(mat, init);
      // Make substitutions based on zero assumptions.
      zeroSubs(mat, zeros);
      logger.log(Level.ALL, "Using assumptions",zeros.toString()+",", "get matrix:","\n"+mat.toString());
      // Put in reduced row-echelon form, obtaining the set of quantities that were
      // assumed to be nonzero during the process, and the set of quantities that must
      // be zero (because they are at the end of an almost zero row), in order for the
      // matrix to have a solution.
      rrefnonzeros = mat.putInRREF(logger);
      logger.log(Level.ALL, "RREF:","\n"+mat.toString());
      logger.log(Level.ALL, "Assumptions made during RREF process:","\n"+rrefnonzeros.toString());
      azrZeros = mat.getAlmostZeroRowAssumptions();
      logger.log(Level.ALL, "Assumptions from almost-zero rows:","\n"+azrZeros.toString());
      // Create a PivotRowHandler.
      PivotRowHandler prh = new PivotRowHandler(mat, logger);
      // Try to handle the pivot rows.
      try {
        // We try to get assumptions on the pivot rows which, together with all other assumptions,
        // will be sufficient for this matrix to have a solution in nonnegative numbers.
        pivotAssumptionSets = prh.handlePivotRows();
        // If no exception was raised, then we were successful.
        break;
      } catch (MatrixSolvingFailedException e) {
        logger.log(Level.ALL, e.toString());
        if (e.getReason() == Reason.BadNonzeroAssumptions) {
          // In this case, the matrix was found conclusively to be unsolvable, and
          // this must be either because the template can work but we made a nonzero assumption
          // which prevents it from working, or else because the template simply can't work at all.
          // We attempt to revise our nonzero assumptions, and try again.
          logger.log(Level.ALL, "Nonzero assumptions were:","\n"+rrefnonzeros.toString());
          zeroPolyMan.extend(rrefnonzeros);
          logger.log(Level.ALL, zeroPolyMan);
          zeros = zeroPolyMan.next();
          logger.log(Level.ALL, "New set of zeros:\n",zeros);
          if (zeros.size() == 0) {
            // In this case we've run out of nonzero assumptions to try reversing. We have to give up
            // on this template altogether.
            throw new MatrixSolvingFailedException(Reason.BadTemplate);
          }
          logger.log(Level.ALL, "We therefore try again, assuming:","\n"+zeros.toString());
        } else {
          // ...
          // Can the pivot row handler fail for any other reason?
        }
      }
    }
    // At this point, we have managed to compute one or more sets of assumptions under any of which
    // the pivot rows would be satisfiable. These data are stored in the Set<Set<Assumption>>
    // called pivotAssumptionSets. In addition, we have the collections of assumptions called
    // rrefnonzeros, azrZeros, and zeros.
    // We unite those last collections, and add them to each element of pivotAssumptionSets. This is
    // our return value. It is left to a higher-level control to decide how to use this.

    AssumptionSet base = new AssumptionSet();
    base.addAll(azrZeros);
    base.addAll(rrefnonzeros);
    base.addAll(zeros);
    for (AssumptionSet aset : pivotAssumptionSets) {
      aset.addAll(base);
    }
    return pivotAssumptionSets;
  }

  /*
   * Make substitutions in the matrix on the basis of polynomials assumed to be zero.
   */
  private void zeroSubs(Matrix mat, AssumptionSet zeros) {
    // Compute substitutions based on the assumptions.
    List<Substitution> subs = new Vector<>();
    for (Assumption a : zeros) {
      // We can only use assumptions of type ZERO.
      if (a.getAssumptionType() != AssumptionType.ZERO) {
        continue;
      }
      Polynomial num = a.getNumerator();
      Substitution s = num.linearIsolateFirst();
      if (s != null) {
        subs.add(s);
      }
    }
    // If we didn't get any, then quit.
    if (subs.size() == 0) {
      return;
    }
    // So we got one or more linear substitutions.
    // We must apply them to the matrix intelligently, so that we get the maximum
    // possible simplification. (In particular, we should eliminate as many variables
    // as possible.)
    SubstitutionManager sman = new SubstitutionManager(subs, logger);
    sman.applyAll(mat); // (The matrix is modified in-place.)
  }

  private class ZeroPolynomialManager {

    private final Vector<List<Polynomial>> stack;
    private final Vector<Integer> pointers;

    public ZeroPolynomialManager() {
      stack = new Vector<>();
      pointers = new Vector<>();
    }

    @Override
    public String toString() {
      return "ZeroPolynomialManager:\n  Stack:\n  "+stack.toString()+"\n  Pointers:\n  "+pointers.toString();
    }

    public void extend(AssumptionSet aset) {
      // If it's the empty list, don't do anything.
      if (aset.size() == 0) {
        return;
      }
      // Extract list of polynomials from the list of assumptions.
      List<Polynomial> polys = new Vector<>(aset.size());
      for (Assumption a : aset) {
        polys.add(a.getNumerator());
      }
      // Add list to stack, and initialize pointer to point just past the end of the list.
      stack.add(polys);
      pointers.add(Integer.valueOf(polys.size()));
    }

    /*
     * Say whether we have another set of polynomials.
     */
    public boolean hasNext() {
      return stack.size() > 0;
    }

    /*
     * Get the next assumption set (empty if there isn't another one).
     */
    public AssumptionSet next() {
      AssumptionSet aset = new AssumptionSet();
      if (!hasNext()) {
        return aset;
      }
      // Advance the pointers.
      advancePointers();
      // Build the next set.
      for (int i = 0; i < stack.size(); i++) {
        List<Polynomial> polys = stack.get(i);
        Integer ptr = pointers.get(i);
        Polynomial p = polys.get(ptr);
        aset.add(new Assumption(p, AssumptionType.ZERO));
      }
      return aset;
    }

    private void advancePointers() {
      int n = pointers.size();
      if (n == 0) {
        return;
      }
      int topPtr = pointers.get(n-1).intValue();
      if (topPtr > 0) {
        pointers.set(n-1, Integer.valueOf(topPtr-1));
      } else {
        stack.remove(n-1);
        pointers.remove(n-1);
        advancePointers();
      }
    }

  }

}
