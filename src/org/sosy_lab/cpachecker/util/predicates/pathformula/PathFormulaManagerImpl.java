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
package org.sosy_lab.cpachecker.util.predicates.pathformula;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.FluentIterable.from;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.counterexample.Model.AssignableTerm;
import org.sosy_lab.cpachecker.core.counterexample.Model.TermType;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FunctionFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.FormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.pointerTarget.PointerTarget;

import com.google.common.base.Function;
import com.google.common.base.Optional;
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
@Options(prefix="cpa.predicate")
public class PathFormulaManagerImpl implements PathFormulaManager {

  @Option(description = "Handle aliasing of pointers. "
      + "This adds disjunctions to the formulas, so be careful when using cartesian abstraction.")
  private boolean handlePointerAliasing = true;

  private static final String BRANCHING_PREDICATE_NAME = "__ART__";
  private static final Pattern BRANCHING_PREDICATE_NAME_PATTERN = Pattern.compile(
      "^.*" + BRANCHING_PREDICATE_NAME + "(?=\\d+$)");

  private static final String NONDET_VARIABLE = "__nondet__";
  private static final String NONDET_FLAG_VARIABLE = NONDET_VARIABLE + "flag__";
  private static final CType NONDET_TYPE = CNumericTypes.INT;
  private final FormulaType<?> NONDET_FORMULA_TYPE;

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final FunctionFormulaManagerView ffmgr;
  private final CtoFormulaConverter converter;
  private final CtoFormulaTypeHandler typeHandler;
  private final @Nullable PointerTargetSetManager ptsManager;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  @Option(description="add special information to formulas about non-deterministic functions")
  private boolean useNondetFlags = false;

  @Deprecated
  public PathFormulaManagerImpl(FormulaManagerView pFmgr,
      Configuration config, LogManager pLogger, ShutdownNotifier pShutdownNotifier, MachineModel pMachineModel)
          throws InvalidConfigurationException {
    this(pFmgr, config, pLogger, pShutdownNotifier, pMachineModel, Optional.<VariableClassification>absent());
  }

  public PathFormulaManagerImpl(FormulaManagerView pFmgr,
      Configuration config, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCfa)
          throws InvalidConfigurationException {
    this(pFmgr, config, pLogger, pShutdownNotifier, pCfa.getMachineModel(), pCfa.getVarClassification());
  }

  public PathFormulaManagerImpl(FormulaManagerView pFmgr,
      Configuration config, LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      MachineModel pMachineModel,
      Optional<VariableClassification> pVariableClassification)
          throws InvalidConfigurationException {
    config.inject(this, PathFormulaManagerImpl.class);

    fmgr = pFmgr;
    bfmgr = fmgr.getBooleanFormulaManager();
    ffmgr = fmgr.getFunctionFormulaManager();
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;

    if (handlePointerAliasing) {
      final FormulaEncodingWithPointerAliasingOptions options = new FormulaEncodingWithPointerAliasingOptions(config);
      TypeHandlerWithPointerAliasing aliasingTypeHandler = new TypeHandlerWithPointerAliasing(pLogger, pMachineModel, pFmgr, options);
      typeHandler = aliasingTypeHandler;
      ptsManager = new PointerTargetSetManager(options, pFmgr, aliasingTypeHandler, shutdownNotifier);
      converter = new CToFormulaConverterWithPointerAliasing(options, pFmgr, pMachineModel, ptsManager, pVariableClassification, pLogger, shutdownNotifier, aliasingTypeHandler);

    } else {
      final FormulaEncodingOptions options = new FormulaEncodingOptions(config);
      typeHandler = new CtoFormulaTypeHandler(pLogger, pMachineModel, pFmgr);
      converter = new CtoFormulaConverter(options, pFmgr, pMachineModel, pVariableClassification, pLogger, pShutdownNotifier, typeHandler);
      ptsManager = null;

      logger.log(Level.WARNING, "Handling of pointer aliasing is disabled, analysis is unsound if aliased pointers exist.");
    }

    NONDET_FORMULA_TYPE = converter.getFormulaTypeFromCType(NONDET_TYPE);
  }

  @Override
  public Pair<PathFormula, ErrorConditions> makeAndWithErrorConditions(PathFormula pOldFormula,
                             final CFAEdge pEdge) throws CPATransferException, InterruptedException {
    ErrorConditions errorConditions = new ErrorConditions(bfmgr);
    PathFormula pf = makeAnd(pOldFormula, pEdge, errorConditions);

    return Pair.of(pf, errorConditions);
  }

  private PathFormula makeAnd(PathFormula pOldFormula, final CFAEdge pEdge, ErrorConditions errorConditions)
      throws CPATransferException, InterruptedException {
    PathFormula pf = converter.makeAnd(pOldFormula, pEdge, errorConditions);

    if (useNondetFlags) {
      SSAMapBuilder ssa = pf.getSsa().builder();

      int lNondetIndex = ssa.getIndex(NONDET_VARIABLE);
      int lFlagIndex = ssa.getIndex(NONDET_FLAG_VARIABLE);

      if (lNondetIndex != lFlagIndex) {
        if (lFlagIndex < 0) {
          lFlagIndex = 1; // ssa indices start with 2, so next flag that is generated also uses index 2
        }

        BooleanFormula edgeFormula = pf.getFormula();

        for (int lIndex = lFlagIndex + 1; lIndex <= lNondetIndex; lIndex++) {
          Formula nondetVar = fmgr.makeVariable(NONDET_FORMULA_TYPE, NONDET_FLAG_VARIABLE, lIndex);
          BooleanFormula lAssignment = fmgr.assignment(nondetVar, fmgr.makeNumber(NONDET_FORMULA_TYPE, 1));
          edgeFormula = bfmgr.and(edgeFormula, lAssignment);
        }

        // update ssa index of nondet flag
        //setSsaIndex(ssa, Variable.create(NONDET_FLAG_VARIABLE, getNondetType()), lNondetIndex);
        ssa.setIndex(NONDET_FLAG_VARIABLE, NONDET_TYPE, lNondetIndex);

        pf = new PathFormula(edgeFormula, ssa.build(), pf.getPointerTargetSet(), pf.getLength());
      }
    }
    return pf;
  }

  @Override
  public PathFormula makeAnd(PathFormula pOldFormula, CFAEdge pEdge) throws CPATransferException, InterruptedException {
    ErrorConditions errorConditions = ErrorConditions.dummyInstance(bfmgr);
    return makeAnd(pOldFormula, pEdge, errorConditions);
  }

  @Override
  public PathFormula makeEmptyPathFormula() {
    return new PathFormula(bfmgr.makeBoolean(true),
                           SSAMap.emptySSAMap(),
                           PointerTargetSet.emptyPointerTargetSet(),
                           0);
  }

  @Override
  public PathFormula makeEmptyPathFormula(PathFormula oldFormula) {
    return new PathFormula(bfmgr.makeBoolean(true),
                           oldFormula.getSsa(),
                           oldFormula.getPointerTargetSet(),
                           0);
  }

  @Override
  public PathFormula makeNewPathFormula(PathFormula oldFormula, SSAMap m) {
    return new PathFormula(oldFormula.getFormula(),
                           m,
                           oldFormula.getPointerTargetSet(),
                           oldFormula.getLength());
  }

  @Override
  public PathFormula makeOr(final PathFormula pathFormula1, final PathFormula pathFormula2) throws InterruptedException {

    final BooleanFormula formula1 = pathFormula1.getFormula();
    final BooleanFormula formula2 = pathFormula2.getFormula();
    final SSAMap ssa1 = pathFormula1.getSsa();
    final SSAMap ssa2 = pathFormula2.getSsa();

    final PointerTargetSet pts1 = pathFormula1.getPointerTargetSet();
    final PointerTargetSet pts2 = pathFormula2.getPointerTargetSet();

    final Pair<Pair<BooleanFormula, BooleanFormula>, SSAMap> mergeSSAResult = mergeSSAMaps(ssa1, pts1, ssa2, pts2);
    final SSAMap newSSA = mergeSSAResult.getSecond();

    final Pair<Triple<BooleanFormula, BooleanFormula, BooleanFormula>, PointerTargetSet> mergePtsResult;
    if (ptsManager != null) {
      mergePtsResult = ptsManager.mergePointerTargetSets(pts1, pts2, newSSA);
    } else {
      BooleanFormula trueFormula = bfmgr.makeBoolean(true);
      mergePtsResult = Pair.of(Triple.of(trueFormula, trueFormula, trueFormula), pts1);
    }

    // (?) Do not swap these two lines, that makes a huge difference in performance (?) !
    final BooleanFormula newFormula1 = bfmgr.and(formula1,
        bfmgr.and(mergeSSAResult.getFirst().getFirst(), mergePtsResult.getFirst().getFirst()));
    final BooleanFormula newFormula2 = bfmgr.and(formula2,
        bfmgr.and(mergeSSAResult.getFirst().getSecond(), mergePtsResult.getFirst().getSecond()));
    final BooleanFormula newFormula = bfmgr.and(bfmgr.or(newFormula1, newFormula2),
                                                         mergePtsResult.getFirst().getThird());
    final PointerTargetSet newPTS = mergePtsResult.getSecond();
    final int newLength = Math.max(pathFormula1.getLength(), pathFormula2.getLength());

    return new PathFormula(newFormula, newSSA, newPTS, newLength);
  }

  @Override
  public PathFormula makeAnd(PathFormula pPathFormula, BooleanFormula pOtherFormula) {
    SSAMap ssa = pPathFormula.getSsa();
    BooleanFormula otherFormula =  fmgr.instantiate(pOtherFormula, ssa);
    BooleanFormula resultFormula = bfmgr.and(pPathFormula.getFormula(), otherFormula);
    final PointerTargetSet pts = pPathFormula.getPointerTargetSet();
    return new PathFormula(resultFormula, ssa, pts, pPathFormula.getLength());
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
   * @param pts1 the PointerTargetSet for ssa1
   * @param ssa2 an SSAMap
   * @param pts2 the PointerTargetSet for ssa1
   * @return A pair (formulas, SSAMap) where the formulas need to be added to the path formulas before disjuncting them.
   */
  private Pair<Pair<BooleanFormula, BooleanFormula>, SSAMap> mergeSSAMaps(
                                     final SSAMap ssa1,
                                     final PointerTargetSet pts1,
                                     final SSAMap ssa2,
                                     final PointerTargetSet pts2) throws InterruptedException {
    final Pair<SSAMap, List<Triple<String, Integer, Integer>>> ssaMergeResult = SSAMap.merge(ssa1, ssa2);
    final SSAMap resultSSA = ssaMergeResult.getFirst();
    final List<Triple<String, Integer, Integer>> symbolDifferences = ssaMergeResult.getSecond();

    BooleanFormula mergeFormula1 = bfmgr.makeBoolean(true);
    BooleanFormula mergeFormula2 = bfmgr.makeBoolean(true);

    for (final Triple<String, Integer, Integer> symbolDifference : symbolDifferences) {
      shutdownNotifier.shutdownIfNecessary();
      final String symbolName = symbolDifference.getFirst();
      final int index1 = firstNonNull(symbolDifference.getSecond(), 1);
      final int index2 = firstNonNull(symbolDifference.getThird(), 1);

      assert symbolName != null;
      assert resultSSA != null;
      BooleanFormula mergeFormula;
      if (index1 > index2 && index1 > 1) {
        // i2:smaller, i1:bigger
        // => need correction term for i2

        assert index2 > 0;
        if (useNondetFlags && symbolName.equals(NONDET_FLAG_VARIABLE)) {
          mergeFormula = makeSsaNondetFlagMerger(index2, index1);

        } else if (CToFormulaConverterWithPointerAliasing.isUF(symbolName)) {
          assert symbolName.equals(CToFormulaConverterWithPointerAliasing.getUFName(resultSSA.getType(symbolName)));
          mergeFormula = makeSsaUFMerger(symbolName, resultSSA.getType(symbolName),
              index2, index1, pts2);

        } else {
          mergeFormula = makeSsaVariableMerger(symbolName,
              resultSSA.getType(symbolName),
              index2, index1);
        }

        mergeFormula2 = bfmgr.and(mergeFormula2, mergeFormula);

      } else if (index2 > 1) {
        assert index1 < index2;
        // i1:smaller, i2:bigger
        // => need correction term for i1

        assert index1 > 0;
        if (useNondetFlags && symbolName.equals(NONDET_FLAG_VARIABLE)) {
          mergeFormula = makeSsaNondetFlagMerger(index1, index2);

        } else if (CToFormulaConverterWithPointerAliasing.isUF(symbolName)) {
          assert symbolName.equals(CToFormulaConverterWithPointerAliasing.getUFName(resultSSA.getType(symbolName)));
          mergeFormula = makeSsaUFMerger(symbolName,
              resultSSA.getType(symbolName),
              index1, index2, pts1);

        } else {
          mergeFormula = makeSsaVariableMerger(symbolName,
              resultSSA.getType(symbolName),
              index1, index2);
        }

        mergeFormula1 = bfmgr.and(mergeFormula1, mergeFormula);
      }
    }

    return Pair.of(Pair.of(mergeFormula1, mergeFormula2), resultSSA);
  }

  private BooleanFormula makeSsaVariableMerger(final String variableName,
                                                        final CType variableType,
                                                        final int oldIndex,
                                                        final int newIndex) {
    assert oldIndex < newIndex;

    // TODO Previously we called makeMerger,
    // which creates the terms (var@oldIndex = var@oldIndex+1; ...; var@oldIndex = var@newIndex).
    // Now we only create a single term (var@oldIndex = var@newIndex).
    // This should not make a difference except maybe for the model,
    // but this could be investigated to be sure.

    final FormulaType<?> variableFormulaType = converter.getFormulaTypeFromCType(variableType);
    final Formula oldVariable = fmgr.makeVariable(variableFormulaType, variableName, oldIndex);
    final Formula newVariable = fmgr.makeVariable(variableFormulaType, variableName, newIndex);

    return fmgr.makeEqual(newVariable, oldVariable);
  }

  private BooleanFormula makeSsaUFMerger(final String functionName,
                                                  final CType returnType,
                                                  final int oldIndex,
                                                  final int newIndex,
                                                  final PointerTargetSet pts) throws InterruptedException {
    assert oldIndex < newIndex;

    final FormulaType<?> returnFormulaType =  converter.getFormulaTypeFromCType(returnType);
    BooleanFormula result = bfmgr.makeBoolean(true);
    for (final PointerTarget target : pts.getAllTargets(returnType)) {
      shutdownNotifier.shutdownIfNecessary();
      final Formula targetAddress = fmgr.makePlus(fmgr.makeVariable(typeHandler.getPointerType(), target.getBaseName()),
                                                  fmgr.makeNumber(typeHandler.getPointerType(), target.getOffset()));

      final BooleanFormula retention = fmgr.makeEqual(ffmgr.createFuncAndCall(functionName,
                                                                              newIndex,
                                                                              returnFormulaType,
                                                                              ImmutableList.of(targetAddress)),
                                                      ffmgr.createFuncAndCall(functionName,
                                                                              oldIndex,
                                                                              returnFormulaType,
                                                                              ImmutableList.of(targetAddress)));
      result = fmgr.makeAnd(result, retention);
    }

    return result;
  }

  private BooleanFormula makeSsaNondetFlagMerger(int iSmaller, int iBigger) {
    return makeMerger(NONDET_FLAG_VARIABLE, iSmaller, iBigger, fmgr.makeNumber(NONDET_FORMULA_TYPE, 0));
  }

  private BooleanFormula makeMerger(String var, int iSmaller, int iBigger, Formula pInitialValue) {
    assert iSmaller < iBigger;

    BooleanFormula lResult = bfmgr.makeBoolean(true);
    FormulaType<Formula> type = fmgr.getFormulaType(pInitialValue);

    for (int i = iSmaller+1; i <= iBigger; ++i) {
      Formula currentVar = fmgr.makeVariable(type, var, i);
      BooleanFormula e = fmgr.makeEqual(currentVar, pInitialValue);
      lResult = bfmgr.and(lResult, e);
    }

    return lResult;
  }

  @Override
  public PathFormula makeFormulaForPath(List<CFAEdge> pPath) throws CPATransferException, InterruptedException {
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
   */
  @Override
  public BooleanFormula buildBranchingFormula(Iterable<ARGState> elementsOnPath) throws CPATransferException, InterruptedException {
    // build the branching formula that will help us find the real error path
    BooleanFormula branchingFormula = bfmgr.makeBoolean(true);
    for (final ARGState pathElement : elementsOnPath) {

      if (pathElement.getChildren().size() > 1) {
        if (pathElement.getChildren().size() > 2) {
          // can't create branching formula
          if (from(pathElement.getChildren()).anyMatch(AbstractStates.IS_TARGET_STATE)) {
            // We expect this situation of one of the children is a target state created by PredicateCPA.
            continue;
          } else {
            logger.log(Level.WARNING, "ARG branching with more than two outgoing edges at ARG node " + pathElement.getStateId() + ".");
            return bfmgr.makeBoolean(true);
          }
        }

        FluentIterable<CFAEdge> outgoingEdges = from(pathElement.getChildren()).transform(
            new Function<ARGState, CFAEdge>() {
              @Override
              public CFAEdge apply(ARGState child) {
                return pathElement.getEdgeToChild(child);
              }
        });
        if (!outgoingEdges.allMatch(Predicates.instanceOf(AssumeEdge.class))) {
          if (from(pathElement.getChildren()).anyMatch(AbstractStates.IS_TARGET_STATE)) {
            // We expect this situation of one of the children is a target state created by PredicateCPA.
            continue;
          } else {
            logger.log(Level.WARNING, "ARG branching without AssumeEdge at ARG node " + pathElement.getStateId() + ".");
            return bfmgr.makeBoolean(true);
          }
        }

        AssumeEdge edge = null;
        for (CFAEdge currentEdge : outgoingEdges) {
          if (((AssumeEdge)currentEdge).getTruthAssumption()) {
            edge = (AssumeEdge)currentEdge;
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
    for (Map.Entry<AssignableTerm, Object> entry : model.entrySet()) {
      AssignableTerm a = entry.getKey();
      if (a instanceof Model.Variable && a.getType() == TermType.Boolean) {

        String name = BRANCHING_PREDICATE_NAME_PATTERN.matcher(a.getName()).replaceFirst("");
        if (!name.equals(a.getName())) {
          // pattern matched, so it's a variable with __ART__ in it

          // no NumberFormatException because of RegExp match earlier
          Integer nodeId = Integer.parseInt(name);

          assert !preds.containsKey(nodeId);

          preds.put(nodeId, (Boolean)entry.getValue());
        }
      }
    }
    return preds;
  }
}
