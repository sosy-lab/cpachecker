/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement.ComputeAbstractionElement;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;

public class PredicatePrecisionAdjustment implements PrecisionAdjustment {

  // statistics
  final Timer totalPrecTime = new Timer();
  final Timer computingAbstractionTime = new Timer();

  int numAbstractions = 0;
  int numAbstractionsFalse = 0;
  int maxBlockSize = 0;
  int maxPredsPerAbstraction = 0;

  private final LogManager logger;
  private final PredicateAbstractionManager formulaManager;
  private final PathFormulaManager pathFormulaManager;
  private final MathsatFormulaManager mMathsatFormulaManager;

  public PredicatePrecisionAdjustment(PredicateCPA pCpa) {
    logger = pCpa.getLogger();
    formulaManager = pCpa.getPredicateManager();
    pathFormulaManager = pCpa.getPathFormulaManager();
    mMathsatFormulaManager = (MathsatFormulaManager)pCpa.getFormulaManager();
  }

  @Override
  public Triple<AbstractElement, Precision, Action> prec(
      AbstractElement pElement, Precision pPrecision,
      UnmodifiableReachedSet pElements) {

    totalPrecTime.start();

    if (pElement instanceof ComputeAbstractionElement) {
      //long lStartTime = System.currentTimeMillis();

      ComputeAbstractionElement element = (ComputeAbstractionElement)pElement;
      PredicatePrecision precision = (PredicatePrecision)pPrecision;

      //pElement = computeAbstraction(element, precision);

      Pair<AbstractElement, PredicatePrecision> lPair = computeAbstraction(element, precision);
      pElement = lPair.getFirst();
      pPrecision = lPair.getSecond();

      /*long lEndTime = System.currentTimeMillis();

      long lDuration = lEndTime - lStartTime;

      if (lDuration > 10000) {
        CFANode lLocation = element.getLocation();
        Collection<AbstractionPredicate> lPredicates = precision.getPredicates(lLocation);

        throw new RuntimeException("Elapsed time: " + (lDuration/1000.0) + "s (" + lPredicates.size() + ") @ " + lLocation.toString());
      }*/
    }

    totalPrecTime.stop();
    return new Triple<AbstractElement, Precision, Action>(
        pElement, pPrecision, Action.CONTINUE);
  }

  long mMaxDuration = -1;
  boolean mRemoveIrrelevantPredicates = true;
  int mPredsSizeThreshold = 0;
  HashMap<Formula, Collection<Formula>> mAtomCache = new HashMap<Formula, Collection<Formula>>();

  public int NUMBER_OF_ABSTRACTIONS;

  /**
   * Compute an abstraction.
   */
  private Pair<AbstractElement, PredicatePrecision> computeAbstraction(
      ComputeAbstractionElement element,
      PredicatePrecision precision) {

    NUMBER_OF_ABSTRACTIONS++;

    AbstractionFormula abstractionFormula = element.getAbstractionFormula();
    PathFormula pathFormula = element.getPathFormula();
    CFANode loc = element.getLocation();

    numAbstractions++;
    logger.log(Level.FINEST, "Computing abstraction on node", loc);

    Collection<AbstractionPredicate> preds = precision.getPredicates(loc);

    //int lPredsBefore = preds.size();

    /*boolean tmpCond = (loc.getNodeNumber() == 12) &&
      (loc.getNodeNumber() == 199) &&
      (loc.getNodeNumber() == 445) &&
      (loc.getNodeNumber() == 463) &&
      (loc.getNodeNumber() == 328) &&
      (loc.getNodeNumber() == 403) &&
      (loc.getNodeNumber() == 31) &&
      (loc.getNodeNumber() == 188) &&
      (loc.getNodeNumber() == 267) &&
      (loc.getNodeNumber() == 344) &&
      (loc.getNodeNumber() == 161) &&
      (loc.getNodeNumber() == 210) &&
      (loc.getNodeNumber() == 1) &&
      (loc.getNodeNumber() == 218) &&
      (loc.getNodeNumber() == 359) &&
      (loc.getNodeNumber() == 338) &&
      (loc.getNodeNumber() == 415) &&
      (loc.getNodeNumber() == 252) &&
      (loc.getNodeNumber() == 459) &&
      (loc.getNodeNumber() == 157);*/
    //boolean tmpCond = true;
    //boolean tmpCond = (loc.getNodeNumber() == 466);
    //mRemoveIrrelevantPredicates = false;
    //mPredsSizeThreshold = 0;

    if (mRemoveIrrelevantPredicates && preds.size() > mPredsSizeThreshold) {
      HashSet<String> lVariables = new HashSet<String>();
      lVariables.addAll(mMathsatFormulaManager.getVariables(abstractionFormula.asFormula()));
      lVariables.addAll(mMathsatFormulaManager.getVariables(pathFormula.getFormula()));

      // TODO remove
      /*HashSet<String> lConstants = new HashSet<String>();
      lConstants.addAll(mMathsatFormulaManager.getConstants(abstractionFormula.asFormula()));
      lConstants.addAll(mMathsatFormulaManager.getConstants(pathFormula.getFormula()));*/

      Collection<AbstractionPredicate> lRemainingPredicates = new HashSet<AbstractionPredicate>();

      for (AbstractionPredicate lPredicate : preds) {
        // TODO replace Strings with ids ?
        Collection<String> lPredicateVariables = mMathsatFormulaManager.getVariables2(lPredicate.getSymbolicAtom());

        boolean lFound = false;

        for (String lVariable : lPredicateVariables) {
          if (lVariables.contains(lVariable)) {
            lFound = true;
            break;
          }
        }

        // TODO
        /*if (mMathsatFormulaManager.isComparisonAgainstConstant(lPredicate.getSymbolicAtom())) {
          if (lFound) {
            Collection<String> lPredConstants = new HashSet<String>();

            lPredConstants.addAll(mMathsatFormulaManager.getConstants(lPredicate.getSymbolicAtom()));
            lPredConstants.retainAll(lConstants);

            if (lPredConstants.isEmpty()) {
              lFound = false;
            }
          }
        }*/

        // TODO think about it
        /*if (lFound && !lVariables.containsAll(lPredicateVariables)) {
          System.out.println(">>>>" + lPredicate.toString());
          lFound = false;
        }*/

        if (lFound || lPredicateVariables.isEmpty()) {
          lRemainingPredicates.add(lPredicate);
        }
      }

      if (preds.size() != lRemainingPredicates.size()) {
        preds = lRemainingPredicates;
      }
    }

    //System.out.println("Location: " + loc + " " + preds.size());

    /*if (lPredsBefore != preds.size()) {
      //System.out.println("Removed " + (lPredsBefore - preds.size()) + " predictates!");

      System.out.println("Location: " + loc + " (REDUCED)");
      //throw new RuntimeException();
    }
    else {
      System.out.println("Location: " + loc + " (NONREDUCED)");
    }*/

    maxBlockSize = Math.max(maxBlockSize, pathFormula.getLength());
    maxPredsPerAbstraction = Math.max(maxPredsPerAbstraction, preds.size());

    //long start = System.currentTimeMillis();

    computingAbstractionTime.start();

    // compute new abstraction
    AbstractionFormula newAbstractionFormula = computeAbstraction(
        abstractionFormula, pathFormula, preds, loc);

    computingAbstractionTime.stop();

    /*if (preds.size() > 40 && !newAbstractionFormula.isFalse()) {
      Formula lFormula = newAbstractionFormula.asFormula();
      Collection<Formula> lAtoms = mMathsatFormulaManager.extractAtoms(lFormula, false, false);

      if (lAtoms.size() != preds.size()) {
        System.out.println((preds.size() - lAtoms.size()) + " predicates vanished!");
      }
    }*/

    /*int lThreshold = 15;

    if (preds.size() > lThreshold) {
      Formula lFormula = newAbstractionFormula.asFormula();*/
      /*Collection<Formula> lAtoms = mAtomCache.get(lFormula);

      if (lAtoms == null) {
        lAtoms = mMathsatFormulaManager.extractAtoms(newAbstractionFormula.asFormula(), false, false);
        mAtomCache.put(lFormula, lAtoms);
      }*/
/*
      Collection<Formula> lAtoms = mMathsatFormulaManager.extractAtoms(lFormula, false, false);

      if (lAtoms.size() != preds.size()) {
        Collection<AbstractionPredicate> lNotVanished = new HashSet<AbstractionPredicate>();

        for (AbstractionPredicate lPredicate : preds) {
          Formula lSymbolicAtom = lPredicate.getSymbolicAtom();

          if (lAtoms.contains(lSymbolicAtom)) {
            lNotVanished.add(lPredicate);
          }
        }

        if (preds.size() - lNotVanished.size() >= lThreshold) {
          precision = precision.update(loc, lNotVanished);
        }
      }
    }*/



    /*long stop = System.currentTimeMillis();

    long duration = stop - start;

    if (duration > mMaxDuration) {
      mMaxDuration = duration;
      //System.out.println("PREDICATES:");
      //System.out.println(preds);
      //System.out.println(newAbstractionFormula);
      System.out.println("#preds: " + preds.size() + ", " + (newAbstractionFormula.isFalse()?"infeasible":"feasible"));

      int lComparisons = 0;

      for (AbstractionPredicate lPredicate : preds) {
        if (mMathsatFormulaManager.isComparisonAgainstConstant(lPredicate.getSymbolicAtom())) {
          lComparisons++;
        }
      }

      System.out.println("#comparisons: " + lComparisons);

      Collection<Formula> lAtoms = mMathsatFormulaManager.extractAtoms(newAbstractionFormula.asFormula(), false, false);

      //System.out.println(lAtoms);
      System.out.println("Remaining predicates in abstraction: " + lAtoms.size());
    }*/

    // if the abstraction is false, return bottom (represented by empty set)
    if (newAbstractionFormula.isFalse()) {
      numAbstractionsFalse++;
      logger.log(Level.FINEST, "Abstraction is false, node is not reachable");
    }

    // create new empty path formula
    PathFormula newPathFormula = pathFormulaManager.makeEmptyPathFormula(pathFormula);

    return Pair.<AbstractElement, PredicatePrecision>of(new PredicateAbstractElement.AbstractionElement(newPathFormula, newAbstractionFormula), precision);
  }

  protected AbstractionFormula computeAbstraction(AbstractionFormula pAbstractionFormula, PathFormula pPathFormula, Collection<AbstractionPredicate> pPreds, CFANode node) {
    return formulaManager.buildAbstraction(pAbstractionFormula, pPathFormula, pPreds);
  }

  protected LogManager getLogger() {
    return logger;
  }
}
