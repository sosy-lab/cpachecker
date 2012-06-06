/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.Model.AssignableTerm;
import org.sosy_lab.cpachecker.util.predicates.Model.TermType;
import org.sosy_lab.cpachecker.util.predicates.Model.Variable;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

/**
 * Class implementing the FormulaManager interface,
 * providing some commonly used stuff which is independent from specific libraries.
 *
 * This class inherits from CtoFormulaConverter to import the stuff there.
 */
public class PathFormulaManagerImpl extends CtoFormulaConverter implements PathFormulaManager {

  private static final String BRANCHING_PREDICATE_NAME = "__ART__";
  private static final Pattern BRANCHING_PREDICATE_NAME_PATTERN = Pattern.compile(
      "^.*" + BRANCHING_PREDICATE_NAME + "(?=\\d+$)");

  public PathFormulaManagerImpl(ExtendedFormulaManager pFmgr,
      Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    super(config, pFmgr, pLogger);
  }

  @Override
  public PathFormula makeEmptyPathFormula() {
    return new PathFormula(fmgr.makeTrue(), SSAMap.emptySSAMap(), 0);
  }

  @Override
  public PathFormula makeEmptyPathFormula(PathFormula oldFormula) {
    return new PathFormula(fmgr.makeTrue(), oldFormula.getSsa(), 0);
  }

  @Override
  public PathFormula makeNewPathFormula(PathFormula oldFormula, SSAMap m) {
    return new PathFormula(oldFormula.getFormula(), m, oldFormula.getLength());
  }

  @Override
  public PathFormula makeOr(PathFormula pF1, PathFormula pF2) {
    Formula formula1 = pF1.getFormula();
    Formula formula2 = pF2.getFormula();
    SSAMap ssa1 = pF1.getSsa();
    SSAMap ssa2 = pF2.getSsa();

    Pair<Pair<Formula, Formula>,SSAMap> pm = mergeSSAMaps(ssa2, ssa1);

    // do not swap these two lines, that makes a huge difference in performance!
    Formula newFormula2 = fmgr.makeAnd(formula2, pm.getFirst().getFirst());
    Formula newFormula1 = fmgr.makeAnd(formula1, pm.getFirst().getSecond());

    Formula newFormula = fmgr.makeOr(newFormula1, newFormula2);
    SSAMap newSsa = pm.getSecond();

    int newLength = Math.max(pF1.getLength(), pF2.getLength());

    return new PathFormula(newFormula, newSsa, newLength);
  }

  @Override
  public PathFormula makeAnd(PathFormula pPathFormula, Formula pOtherFormula) {
    SSAMap ssa = pPathFormula.getSsa();
    Formula otherFormula =  fmgr.instantiate(pOtherFormula, ssa);
    Formula resultFormula = fmgr.makeAnd(pPathFormula.getFormula(), otherFormula);
    return new PathFormula(resultFormula, ssa, pPathFormula.getLength());
  }

  /**
   * builds a formula that represents the necessary variable assignments
   * to "merge" the two ssa maps. That is, for every variable X that has two
   * different ssa indices i and j in the maps, creates a new formula
   * (X_k = X_i) | (X_k = X_j), where k is a fresh ssa index.
   * Returns the formula described above, plus a new SSAMap that is the merge
   * of the two.
   *
   * @param ssa1 an SSAMap
   * @param ssa2 an SSAMap
   * @return A pair (Formula, SSAMap)
   */
  private Pair<Pair<Formula, Formula>, SSAMap> mergeSSAMaps(
      SSAMap ssa1, SSAMap ssa2) {
    SSAMap result = SSAMap.merge(ssa1, ssa2);
    Formula mt1 = fmgr.makeTrue();
    Formula mt2 = fmgr.makeTrue();

    for (String var : result.allVariables()) {
      if (var.equals(CtoFormulaConverter.NONDET_VARIABLE)) {
        continue; // do not add index adjustment terms for __nondet__
      }
      int i1 = ssa1.getIndex(var);
      int i2 = ssa2.getIndex(var);

      if (i1 > i2 && i1 > 1) {
        // i2:smaller, i1:bigger
        // => need correction term for i2
        Formula t;

        if (useNondetFlags && var.equals(CtoFormulaConverter.NONDET_FLAG_VARIABLE)) {
          t = makeNondetFlagMerger(Math.max(i2, 1), i1);
        }
        else {
          t = makeSSAMerger(var, Math.max(i2, 1), i1);
        }

        mt2 = fmgr.makeAnd(mt2, t);

      } else if (i1 < i2 && i2 > 1) {
        // i1:smaller, i2:bigger
        // => need correction term for i1
        Formula t;

        if (useNondetFlags && var.equals(CtoFormulaConverter.NONDET_FLAG_VARIABLE)) {
          t = makeNondetFlagMerger(Math.max(i1, 1), i2);
        }
        else {
          t = makeSSAMerger(var, Math.max(i1, 1), i2);
        }

        mt1 = fmgr.makeAnd(mt1, t);
      }
    }

    for (Pair<String, FormulaList> f : result.allFunctions()) {
      String name = f.getFirst();
      FormulaList args = f.getSecond();
      int i1 = ssa1.getIndex(f);
      int i2 = ssa2.getIndex(f);

      if (i1 > i2 && i1 > 1) {
        // i2:smaller, i1:bigger
        // => need correction term for i2
        Formula t = makeSSAMerger(name, args, Math.max(i2, 1), i1);
        mt2 = fmgr.makeAnd(mt2, t);

      } else if (i1 < i2 && i2 > 1) {
        // i1:smaller, i2:bigger
        // => need correction term for i1
        Formula t = makeSSAMerger(name, args, Math.max(i1, 1), i2);
        mt1 = fmgr.makeAnd(mt1, t);
      }
    }

    return Pair.of(Pair.of(mt1, mt2), result);
  }

  private Formula makeNondetFlagMerger(int iSmaller, int iBigger) {
    return makeMerger(CtoFormulaConverter.NONDET_FLAG_VARIABLE, iSmaller, iBigger, fmgr.makeNumber(0));
  }

  private Formula makeMerger(String var, int iSmaller, int iBigger, Formula pInitialValue) {
    assert iSmaller < iBigger;

    Formula lResult = fmgr.makeTrue();

    for (int i = iSmaller+1; i <= iBigger; ++i) {
      Formula currentVar = fmgr.makeVariable(var, i);
      Formula e = fmgr.makeEqual(currentVar, pInitialValue);
      lResult = fmgr.makeAnd(lResult, e);
    }

    return lResult;
  }

  // creates the mathsat terms
  // (var@iSmaller = var@iSmaller+1; ...; var@iSmaller = var@iBigger)
  private Formula makeSSAMerger(String var, int iSmaller, int iBigger) {
    return makeMerger(var, iSmaller, iBigger, fmgr.makeVariable(var, iSmaller));
  }

  private Formula makeSSAMerger(String name,
      FormulaList args, int iSmaller, int iBigger) {
    assert iSmaller < iBigger;

    Formula intialFunc = fmgr.makeUIF(name, args, iSmaller);
    Formula result = fmgr.makeTrue();

    for (int i = iSmaller+1; i <= iBigger; ++i) {
      Formula currentFunc = fmgr.makeUIF(name, args, i);
      Formula e = fmgr.makeEqual(currentFunc, intialFunc);
      result = fmgr.makeAnd(result, e);
    }
    return result;
  }

  @Override
  public PathFormula makeFormulaForPath(List<CFAEdge> pPath) throws CPATransferException {
    PathFormula pathFormula = makeEmptyPathFormula();
    for (CFAEdge edge : pPath) {
      pathFormula = makeAnd(pathFormula, edge);
    }
    return pathFormula;
  }


  /**
   * Build a formula containing a predicate for all branching situations in the
   * ARG. If a satisfying assignment is created for this formula, it can be used
   * to find out which paths in the ARG are feasible.
   *
   * This method may be called with an empty set, in which case it does nothing
   * and returns the formula "true".
   *
   * @param elementsOnPath The ARG elements that should be considered.
   * @return A formula containing a predicate for each branching.
   * @throws CPATransferException
   */
  @Override
  public Formula buildBranchingFormula(Iterable<ARGElement> elementsOnPath) throws CPATransferException {
    // build the branching formula that will help us find the real error path
    Formula branchingFormula = fmgr.makeTrue();
    for (final ARGElement pathElement : elementsOnPath) {

      if (pathElement.getChildren().size() > 1) {
        if (pathElement.getChildren().size() > 2) {
          // can't create branching formula
          logger.log(Level.WARNING, "ARG branching with more than two outgoing edges");
          return fmgr.makeTrue();
        }

        Iterable<CFAEdge> outgoingEdges = Iterables.transform(pathElement.getChildren(),
            new Function<ARGElement, CFAEdge>() {
              @Override
              public CFAEdge apply(ARGElement child) {
                return pathElement.getEdgeToChild(child);
              }
        });
        if (!Iterables.all(outgoingEdges, Predicates.instanceOf(AssumeEdge.class))) {
          logger.log(Level.WARNING, "ARG branching without AssumeEdge");
          return fmgr.makeTrue();
        }

        AssumeEdge edge = null;
        for (CFAEdge currentEdge : outgoingEdges) {
          if (((AssumeEdge)currentEdge).getTruthAssumption()) {
            edge = (AssumeEdge)currentEdge;
            break;
          }
        }
        assert edge != null;

        Formula pred = fmgr.makePredicateVariable(BRANCHING_PREDICATE_NAME + pathElement.getElementId(), 0);

        // create formula by edge, be sure to use the correct SSA indices!
        // TODO the class PathFormulaManagerImpl should not depend on PredicateAbstractElement,
        // it is used without PredicateCPA as well.
        PathFormula pf;
        PredicateAbstractElement pe = AbstractStates.extractElementByType(pathElement, PredicateAbstractElement.class);
        if (pe == null) {
          logger.log(Level.WARNING, "Cannot find precise error path information without PredicateCPA");
          return fmgr.makeTrue();
        } else {
          pf = pe.getPathFormula();
        }
        pf = this.makeEmptyPathFormula(pf); // reset everything except SSAMap
        pf = this.makeAnd(pf, edge);        // conjunct with edge

        Formula equiv = fmgr.makeEquivalence(pred, pf.getFormula());
        branchingFormula = fmgr.makeAnd(branchingFormula, equiv);
      }
    }
    return branchingFormula;
  }

  /**
   * Extract the information about the branching predicates created by
   * {@link #buildBranchingFormula(Set)} from a satisfying assignment.
   *
   * A map is created that stores for each ARGElement (using its element id as
   * the map key) which edge was taken (the positive or the negated one).
   *
   * @param model A satisfying assignment that should contain values for branching predicates.
   * @return A map from ARG element id to a boolean value indicating direction.
   */
  @Override
  public Map<Integer, Boolean> getBranchingPredicateValuesFromModel(Model model) {
    if (model.isEmpty()) {
      logger.log(Level.WARNING, "No satisfying assignment given by solver!");
      return Collections.emptyMap();
    }

    Map<Integer, Boolean> preds = Maps.newHashMap();
    for (AssignableTerm a : model.keySet()) {
      if (a instanceof Variable && a.getType() == TermType.Boolean) {

        String name = BRANCHING_PREDICATE_NAME_PATTERN.matcher(a.getName()).replaceFirst("");
        if (!name.equals(a.getName())) {
          // pattern matched, so it's a variable with __ART__ in it

          // no NumberFormatException because of RegExp match earlier
          Integer nodeId = Integer.parseInt(name);

          assert !preds.containsKey(nodeId);


          Boolean value = (Boolean)model.get(a);
          preds.put(nodeId, value);
        }
      }
    }
    return preds;
  }
}
