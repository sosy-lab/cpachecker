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

import static com.google.common.collect.FluentIterable.from;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMapMerger.MergeResult;
import org.sosy_lab.cpachecker.util.predicates.pathformula.arrays.CToFormulaConverterWithArrays;
import org.sosy_lab.cpachecker.util.predicates.pathformula.arrays.CtoFormulaTypeHandlerWithArrays;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.FormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Class implementing the FormulaManager interface,
 * providing some commonly used stuff which is independent from specific libraries.
 *
 * This class inherits from CtoFormulaConverter to import the stuff there.
 */
@Options(prefix="cpa.predicate")
public class PathFormulaManagerImpl implements PathFormulaManager {

  @Option(secure=true, description = "Handle aliasing of pointers. "
      + "This adds disjunctions to the formulas, so be careful when using cartesian abstraction.")
  private boolean handlePointerAliasing = true;

  @Option(secure=true, description = "Handle arrays using the theory of arrays.")
  private boolean handleArrays = false;

  @Option(secure=true, description="Call 'simplify' on generated formulas.")
  private boolean simplifyGeneratedPathFormulas = false;

  private static final String BRANCHING_PREDICATE_NAME = "__ART__";
  private static final Pattern BRANCHING_PREDICATE_NAME_PATTERN = Pattern.compile(
      "^.*" + BRANCHING_PREDICATE_NAME + "(?=\\d+$)");

  private static final String NONDET_VARIABLE = "__nondet__";
  static final String NONDET_FLAG_VARIABLE = NONDET_VARIABLE + "flag__";
  private static final CType NONDET_TYPE = CNumericTypes.SIGNED_INT;
  private final FormulaType<?> NONDET_FORMULA_TYPE;

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final CtoFormulaConverter converter;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  @Option(
    secure = true,
    description = "add special information to formulas about non-deterministic functions"
  )
  private boolean useNondetFlags = false;

  public PathFormulaManagerImpl(FormulaManagerView pFmgr,
      Configuration config, LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      CFA pCfa, AnalysisDirection pDirection)
          throws InvalidConfigurationException {

    this(pFmgr, config, pLogger, pShutdownNotifier, pCfa.getMachineModel(),
        pCfa.getVarClassification(), pDirection);
  }

  @VisibleForTesting
  PathFormulaManagerImpl(FormulaManagerView pFmgr,
      Configuration config, LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      MachineModel pMachineModel,
      Optional<VariableClassification> pVariableClassification, AnalysisDirection pDirection)
          throws InvalidConfigurationException {

    config.inject(this, PathFormulaManagerImpl.class);

    fmgr = pFmgr;
    bfmgr = fmgr.getBooleanFormulaManager();
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;

    if (handleArrays) {
      final FormulaEncodingOptions options = new FormulaEncodingOptions(config);
      CtoFormulaTypeHandler typeHandler =
          new CtoFormulaTypeHandlerWithArrays(pLogger, pMachineModel);
      converter = new CToFormulaConverterWithArrays(options, fmgr, pMachineModel,
          pVariableClassification, logger, shutdownNotifier, typeHandler, pDirection);

      logger.log(Level.WARNING,
          "Handling of pointer aliasing is disabled, analysis is unsound if aliased pointers exist.");

    } else if (handlePointerAliasing) {
      final FormulaEncodingWithPointerAliasingOptions options = new FormulaEncodingWithPointerAliasingOptions(config);
      if (options.useQuantifiersOnArrays()) {
        try {
          fmgr.getQuantifiedFormulaManager();
        } catch (UnsupportedOperationException e) {
          throw new InvalidConfigurationException("Cannot use quantifiers with current solver, either choose a different solver or disable quantifiers.");
        }
      }
      if (options.useArraysForHeap()) {
        try {
          fmgr.getArrayFormulaManager();
        } catch (UnsupportedOperationException e) {
          throw new InvalidConfigurationException(
              "Cannot use arrays with current solver, either choose a different solver or disable arrays.");
        }
      }

      TypeHandlerWithPointerAliasing aliasingTypeHandler = new TypeHandlerWithPointerAliasing(pLogger, pMachineModel, options);

      converter = new CToFormulaConverterWithPointerAliasing(options, fmgr,
          pMachineModel, pVariableClassification, logger, shutdownNotifier,
          aliasingTypeHandler, pDirection);

    } else {
      final FormulaEncodingOptions options = new FormulaEncodingOptions(config);
      CtoFormulaTypeHandler typeHandler = new CtoFormulaTypeHandler(pLogger, pMachineModel);
      converter = new CtoFormulaConverter(options, fmgr, pMachineModel,
          pVariableClassification, logger, shutdownNotifier, typeHandler, pDirection);

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
      throws UnrecognizedCCodeException, UnrecognizedCFAEdgeException, InterruptedException {
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
    if (simplifyGeneratedPathFormulas) {
      pf = pf.updateFormula(fmgr.simplify(pf.getFormula()));
    }
    return pf;
  }

  @Override
  public PathFormula makeAnd(PathFormula pPathFormula, CExpression pAssumption)
      throws CPATransferException, InterruptedException {
    CAssumeEdge fakeEdge =
        new CAssumeEdge(
            pAssumption.toASTString(),
            FileLocation.DUMMY,
            new CFANode("dummy"),
            new CFANode("dummy"),
            pAssumption,
            true);
    return converter.makeAnd(pPathFormula, fakeEdge, ErrorConditions.dummyInstance(bfmgr));
  }

  @Override
  public PathFormula makeAnd(PathFormula pOldFormula, CFAEdge pEdge)
      throws CPATransferException, InterruptedException {
    ErrorConditions errorConditions = ErrorConditions.dummyInstance(bfmgr);
    return makeAnd(pOldFormula, pEdge, errorConditions);
  }

  @Override
  public PathFormula makeEmptyPathFormula() {
    return new PathFormula(bfmgr.makeTrue(),
                           SSAMap.emptySSAMap(),
                           PointerTargetSet.emptyPointerTargetSet(),
                           0);
  }

  @Override
  public PathFormula makeEmptyPathFormula(PathFormula oldFormula) {
    return new PathFormula(bfmgr.makeTrue(),
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

    final SSAMapMerger merger =
        new SSAMapMerger(
            useNondetFlags,
            fmgr,
            converter,
            shutdownNotifier,
            NONDET_FORMULA_TYPE);
    final MergeResult<SSAMap> mergeSSAResult = merger.mergeSSAMaps(ssa1, pts1, ssa2, pts2);
    final SSAMapBuilder newSSA = mergeSSAResult.getResult().builder();

    final MergeResult<PointerTargetSet> mergePtsResult = converter.mergePointerTargetSets(pts1, pts2, newSSA);

    // (?) Do not swap these two lines, that makes a huge difference in performance (?) !
    final BooleanFormula newFormula1 = bfmgr.and(formula1,
        bfmgr.and(mergeSSAResult.getLeftConjunct(), mergePtsResult.getLeftConjunct()));
    final BooleanFormula newFormula2 = bfmgr.and(formula2,
        bfmgr.and(mergeSSAResult.getRightConjunct(), mergePtsResult.getRightConjunct()));
    final BooleanFormula newFormula = bfmgr.and(bfmgr.or(newFormula1, newFormula2),
        bfmgr.and(mergeSSAResult.getFinalConjunct(), mergePtsResult.getFinalConjunct()));
    final PointerTargetSet newPTS = mergePtsResult.getResult();
    final int newLength = Math.max(pathFormula1.getLength(), pathFormula2.getLength());

    PathFormula out = new PathFormula(newFormula, newSSA.build(), newPTS, newLength);
    if (simplifyGeneratedPathFormulas) {
      out = out.updateFormula(fmgr.simplify(out.getFormula()));
    }
    return out;
  }

  @Override
  public PathFormula makeAnd(PathFormula pPathFormula, BooleanFormula pOtherFormula) {
    SSAMap ssa = pPathFormula.getSsa();
    BooleanFormula otherFormula =  fmgr.instantiate(pOtherFormula, ssa);
    BooleanFormula resultFormula = bfmgr.and(pPathFormula.getFormula(), otherFormula);
    final PointerTargetSet pts = pPathFormula.getPointerTargetSet();
    return new PathFormula(resultFormula, ssa, pts, pPathFormula.getLength());
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
   * {@inheritDoc}
   */
  @Override
  public Formula makeFormulaForVariable(
      PathFormula pContext, String pVarName, CType pType, boolean forcePointerDereference) {
    return converter.makeFormulaForVariable(
        pContext.getSsa(),
        pContext.getPointerTargetSet(),
        pVarName,
        pType,
        forcePointerDereference);
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
  public BooleanFormula buildBranchingFormula(Set<ARGState> elementsOnPath)
      throws CPATransferException, InterruptedException {
    // build the branching formula that will help us find the real error path
    BooleanFormula branchingFormula = bfmgr.makeTrue();
    for (final ARGState pathElement : elementsOnPath) {
      Set<ARGState> children = Sets.newHashSet(pathElement.getChildren());
      Set<ARGState> childrenOnPath = Sets.intersection(children, elementsOnPath).immutableCopy();

      if (childrenOnPath.size() > 1) {
        if (childrenOnPath.size() > 2) {
          // can't create branching formula
          if (from(childrenOnPath).anyMatch(AbstractStates.IS_TARGET_STATE)) {
            // We expect this situation of one of the children is a target state created by PredicateCPA.
            continue;
          } else {
            logger.log(Level.WARNING, "ARG branching with more than two outgoing edges at ARG node " + pathElement.getStateId() + ".");
            return bfmgr.makeTrue();
          }
        }

        FluentIterable<CFAEdge> outgoingEdges =
            from(childrenOnPath).transform(pathElement::getEdgeToChild);
        if (!outgoingEdges.allMatch(Predicates.instanceOf(AssumeEdge.class))) {
          if (from(childrenOnPath).anyMatch(AbstractStates.IS_TARGET_STATE)) {
            // We expect this situation of one of the children is a target state created by PredicateCPA.
            continue;
          } else {
            logger.log(Level.WARNING, "ARG branching without AssumeEdge at ARG node " + pathElement.getStateId() + ".");
            return bfmgr.makeTrue();
          }
        }

        assert outgoingEdges.size() == 2;

        // We expect there to be exactly one positive and one negative edge
        AssumeEdge positiveEdge = null;
        AssumeEdge negativeEdge = null;
        for (AssumeEdge currentEdge : outgoingEdges.filter(AssumeEdge.class)) {
          if (currentEdge.getTruthAssumption()) {
            positiveEdge = currentEdge;
          } else {
            negativeEdge = currentEdge;
          }
        }
        if (positiveEdge == null || negativeEdge == null) {
          logger.log(Level.WARNING, "Ambiguous ARG branching at ARG node " + pathElement.getStateId() + ".");
          return bfmgr.makeTrue();
        }

        BooleanFormula pred = bfmgr.makeVariable(BRANCHING_PREDICATE_NAME + pathElement.getStateId());

        // create formula by edge, be sure to use the correct SSA indices!
        // TODO the class PathFormulaManagerImpl should not depend on PredicateAbstractState,
        // it is used without PredicateCPA as well.
        PathFormula pf;
        PredicateAbstractState pe = AbstractStates.extractStateByType(pathElement, PredicateAbstractState.class);
        if (pe == null) {
          logger.log(Level.WARNING, "Cannot find precise error path information without PredicateCPA");
          return bfmgr.makeTrue();
        } else {
          pf = pe.getPathFormula();
        }
        pf = this.makeEmptyPathFormula(pf); // reset everything except SSAMap
        pf = this.makeAnd(pf, positiveEdge);        // conjunct with edge

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
  public Map<Integer, Boolean> getBranchingPredicateValuesFromModel(Iterable<ValueAssignment> model) {
    // Do not use fmgr here, this fails if a separate solver is used for interpolation.
    if (!model.iterator().hasNext()) {
      logger.log(Level.WARNING, "No satisfying assignment given by solver!");
      return Collections.emptyMap();
    }

    Map<Integer, Boolean> preds = Maps.newHashMap();
    for (ValueAssignment entry : model) {
      String canonicalName = entry.getName();

      if (entry.getKey() instanceof BooleanFormula) {
        String name = BRANCHING_PREDICATE_NAME_PATTERN.matcher(canonicalName).replaceFirst("");
        if (!name.equals(canonicalName)) {
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

  @Override
  public Formula expressionToFormula(PathFormula pFormula,
      CIdExpression expr,
      CFAEdge edge) throws UnrecognizedCCodeException {
    return converter.buildTermFromPathFormula(pFormula, expr, edge);
  }

  @Override
  public BooleanFormula buildImplicationTestAsUnsat(PathFormula pF1, PathFormula pF2) throws InterruptedException {
    final SSAMapMerger merger =
        new SSAMapMerger(
            useNondetFlags,
            fmgr,
            converter,
            shutdownNotifier,
            NONDET_FORMULA_TYPE);
    BooleanFormula bF = pF2.getFormula();
    return bfmgr.and(
        merger.addMergeAssumptions(
            pF1.getFormula(), pF1.getSsa(), pF1.getPointerTargetSet(), pF2.getSsa()),
        bfmgr.not(bF));
  }

  @Override
  public void printStatistics(PrintStream out) {
    converter.printStatistics(out);
  }

}
