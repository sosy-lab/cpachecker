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

import static com.google.common.collect.FluentIterable.from;

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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.Model.AssignableTerm;
import org.sosy_lab.cpachecker.util.predicates.Model.TermType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaList;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
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

  public PathFormulaManagerImpl(FormulaManagerView pFmgr,
      Configuration config, LogManager pLogger, MachineModel pMachineModel)
          throws InvalidConfigurationException {
    super(config, pFmgr, pMachineModel, pLogger);
  }

  @Override
  public PathFormula makeEmptyPathFormula() {
    return new PathFormula(bfmgr.makeBoolean(true), SSAMap.emptySSAMap(), 0);
  }

  @Override
  public PathFormula makeEmptyPathFormula(PathFormula oldFormula) {
    return new PathFormula(bfmgr.makeBoolean(true), oldFormula.getSsa(), 0);
  }

  @Override
  public PathFormula makeNewPathFormula(PathFormula oldFormula, SSAMap m) {
    return new PathFormula(oldFormula.getFormula(), m, oldFormula.getLength());
  }

  @Override
  public PathFormula makeOr(PathFormula pF1, PathFormula pF2) {
    BooleanFormula formula1 = pF1.getFormula();
    BooleanFormula formula2 = pF2.getFormula();
    SSAMap ssa1 = pF1.getSsa();
    SSAMap ssa2 = pF2.getSsa();

    Pair<Pair<BooleanFormula, BooleanFormula>,SSAMap> pm = mergeSSAMaps(ssa2, ssa1);

    // do not swap these two lines, that makes a huge difference in performance!
    BooleanFormula newFormula2 = bfmgr.and(formula2, pm.getFirst().getFirst());
    BooleanFormula newFormula1 = bfmgr.and(formula1, pm.getFirst().getSecond());

    BooleanFormula newFormula = bfmgr.or(newFormula1, newFormula2);
    SSAMap newSsa = pm.getSecond();

    int newLength = Math.max(pF1.getLength(), pF2.getLength());

    return new PathFormula(newFormula, newSsa, newLength);
  }

  @Override
  public PathFormula makeAnd(PathFormula pPathFormula, BooleanFormula pOtherFormula) {
    SSAMap ssa = pPathFormula.getSsa();
    BooleanFormula otherFormula =  fmgr.instantiate(pOtherFormula, ssa);
    BooleanFormula resultFormula = bfmgr.and(pPathFormula.getFormula(), otherFormula);
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
  private Pair<Pair<BooleanFormula, BooleanFormula>, SSAMap> mergeSSAMaps(
      SSAMap ssa1, SSAMap ssa2) {
    SSAMap result = SSAMap.merge(ssa1, ssa2);
    BooleanFormula mt1 = bfmgr.makeBoolean(true);
    BooleanFormula mt2 = bfmgr.makeBoolean(true);

    for (Variable<?> var : result.allVariables()) {
      if (var.equals(CtoFormulaConverter.NONDET_VARIABLE)) {
        continue; // do not add index adjustment terms for __nondet__
      }
      int i1 = ssa1.getIndex(var);
      int i2 = ssa2.getIndex(var);

      if (i1 > i2 && i1 > 1) {
        // i2:smaller, i1:bigger
        // => need correction term for i2
        BooleanFormula t;

        if (useNondetFlags && var.equals(CtoFormulaConverter.NONDET_FLAG_VARIABLE)) {
          t = makeNondetFlagMerger(Math.max(i2, 1), i1);
        }
        else {
          t = makeSSAMerger(var, Math.max(i2, 1), i1);
        }

        mt2 = bfmgr.and(mt2, t);

      } else if (i1 < i2 && i2 > 1) {
        // i1:smaller, i2:bigger
        // => need correction term for i1
        BooleanFormula t;

        if (useNondetFlags && var.equals(CtoFormulaConverter.NONDET_FLAG_VARIABLE)) {
          t = makeNondetFlagMerger(Math.max(i1, 1), i2);
        }
        else {
          t = makeSSAMerger(var, Math.max(i1, 1), i2);
        }

        mt1 = bfmgr.and(mt1, t);
      }
    }

    for (Pair<Variable<?>, FormulaList> f : result.allFunctions()) {
      Variable<?> name = f.getFirst();
      FormulaList args = f.getSecond();
      int i1 = ssa1.getIndex(f);
      int i2 = ssa2.getIndex(f);

      if (i1 > i2 && i1 > 1) {
        // i2:smaller, i1:bigger
        // => need correction term for i2
        BooleanFormula t = makeSSAMerger(name, args, Math.max(i2, 1), i1);
        mt2 = bfmgr.and(mt2, t);

      } else if (i1 < i2 && i2 > 1) {
        // i1:smaller, i2:bigger
        // => need correction term for i1
        BooleanFormula t = makeSSAMerger(name, args, Math.max(i1, 1), i2);
        mt1 = bfmgr.and(mt1, t);
      }
    }

    return Pair.of(Pair.of(mt1, mt2), result);
  }

  private BooleanFormula makeNondetFlagMerger(int iSmaller, int iBigger) {
    return makeMerger(CtoFormulaConverter.NONDET_FLAG_VARIABLE, iSmaller, iBigger, fmgr.makeNumber(getNondetType(), 0));
  }

  private BooleanFormula makeMerger(String var, int iSmaller, int iBigger, Formula pInitialValue) {
    assert iSmaller < iBigger;

    BooleanFormula lResult = bfmgr.makeBoolean(true);

    for (int i = iSmaller+1; i <= iBigger; ++i) {
      Formula currentVar = fmgr.makeVariable(fmgr.getFormulaType(pInitialValue), var, i);
      BooleanFormula e = fmgr.makeEqual(currentVar, pInitialValue);
      lResult = bfmgr.and(lResult, e);
    }

    return lResult;
  }

  // creates the mathsat terms
  // (var@iSmaller = var@iSmaller+1; ...; var@iSmaller = var@iBigger)
  private BooleanFormula makeSSAMerger(Variable<?> var, int iSmaller, int iBigger) {
    return makeMerger(var.getName(), iSmaller, iBigger, fmgr.makeVariable(var.getType(), var.getName(), iSmaller));
  }

  private BooleanFormula makeSSAMerger(Variable<?> var,
      FormulaList args, int iSmaller, int iBigger) {
    assert iSmaller < iBigger;

    FormulaType<?> t = var.getType();
    Formula initialFunc = ffmgr.createFuncAndCall(var.getName(), iSmaller, t, fromList(args));
    //BooleanFormula intialFunc = fmgr.makeUIF(name, args, iSmaller);
    BooleanFormula result = bfmgr.makeBoolean(true);

    for (int i = iSmaller+1; i <= iBigger; ++i) {
      //BooleanFormula currentFunc = fmgr.makeUIF(name, args, i);
      Formula currentFunc = ffmgr.createFuncAndCall(var.getName(), i, t, fromList(args));
      BooleanFormula e = fmgr.makeEqual(currentFunc, initialFunc);
      result = bfmgr.and(result, e);
    }
    return result;
  }

  private List<Formula> fromList(FormulaList pArgs) {
    return ImmutableList.copyOf(((AbstractFormulaList)pArgs).getTerms());
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
   * @param elementsOnPath The ARG states that should be considered.
   * @return A formula containing a predicate for each branching.
   * @throws CPATransferException
   */
  @Override
  public BooleanFormula buildBranchingFormula(Iterable<ARGState> elementsOnPath) throws CPATransferException {
    // build the branching formula that will help us find the real error path
    BooleanFormula branchingFormula = bfmgr.makeBoolean(true);
    for (final ARGState pathElement : elementsOnPath) {

      if (pathElement.getChildren().size() > 1) {
        if (pathElement.getChildren().size() > 2) {
          // can't create branching formula
          logger.log(Level.WARNING, "ARG branching with more than two outgoing edges");
          return bfmgr.makeBoolean(true);
        }

        FluentIterable<CFAEdge> outgoingEdges = from(pathElement.getChildren()).transform(
            new Function<ARGState, CFAEdge>() {
              @Override
              public CFAEdge apply(ARGState child) {
                return pathElement.getEdgeToChild(child);
              }
        });
        if (!outgoingEdges.allMatch(Predicates.instanceOf(CAssumeEdge.class))) {
          logger.log(Level.WARNING, "ARG branching without CAssumeEdge");
          return  bfmgr.makeBoolean(true);
        }

        CAssumeEdge edge = null;
        for (CFAEdge currentEdge : outgoingEdges) {
          if (((CAssumeEdge)currentEdge).getTruthAssumption()) {
            edge = (CAssumeEdge)currentEdge;
            break;
          }
        }
        assert edge != null;
        BooleanFormula pred = bfmgr.makeVariable(BRANCHING_PREDICATE_NAME + pathElement.getStateId(), 0);

        // create formula by edge, be sure to use the correct SSA indices!
        // TODO the class PathFormulaManagerImpl should not depend on PredicateAbstractState,
        // it is used without PredicateCPA as well.
        PathFormula pf;
        PredicateAbstractState pe = AbstractStates.extractStateByType(pathElement, PredicateAbstractState.class);
        if (pe == null) {
          logger.log(Level.WARNING, "Cannot find precise error path information without PredicateCPA");
          return bfmgr.makeBoolean(true);
        } else {
          pf = pe.getPathFormula();
        }
        pf = this.makeEmptyPathFormula(pf); // reset everything except SSAMap
        pf = this.makeAnd(pf, edge);        // conjunct with edge

        BooleanFormula equiv = bfmgr.equivalence(pred, pf.getFormula());
        branchingFormula = bfmgr.and(branchingFormula, equiv);
      }
    }
    return branchingFormula;
  }

  /**
   * Extract the information about the branching predicates created by
   * {@link #buildBranchingFormula(Set)} from a satisfying assignment.
   *
   * A map is created that stores for each ARGState (using its element id as
   * the map key) which edge was taken (the positive or the negated one).
   *
   * @param model A satisfying assignment that should contain values for branching predicates.
   * @return A map from ARG state id to a boolean value indicating direction.
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
