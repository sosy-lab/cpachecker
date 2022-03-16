// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
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
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMapMerger.MergeResult;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoWpConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.FormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

/**
 * Class implementing the FormulaManager interface, providing some commonly used stuff which is
 * independent from specific libraries.
 *
 * <p>This class inherits from CtoFormulaConverter to import the stuff there.
 */
@Options(prefix = "cpa.predicate")
public class PathFormulaManagerImpl implements PathFormulaManager {

  @Option(
      secure = true,
      description =
          "Handle aliasing of pointers. This adds disjunctions to the formulas, so be careful when"
              + " using cartesian abstraction.")
  private boolean handlePointerAliasing = true;

  @Option(secure = true, description = "Call 'simplify' on generated formulas.")
  private boolean simplifyGeneratedPathFormulas = false;

  @Option(
      secure = true,
      description =
          "Which path-formula builder to use.Depending on this setting additional terms are added"
              + " to the path formulas,e.g. SYMBOLICLOCATIONS will add track the program counter"
              + " symbolically with a special variable %pc")
  private PathFormulaBuilderVariants pathFormulaBuilderVariant = PathFormulaBuilderVariants.DEFAULT;

  private enum PathFormulaBuilderVariants {
    DEFAULT,
    SYMBOLICLOCATIONS
  }

  private static final String BRANCHING_PREDICATE_NAME = "__ART__";
  private static final Pattern BRANCHING_PREDICATE_NAME_PATTERN =
      Pattern.compile("^.*" + BRANCHING_PREDICATE_NAME + "(?=\\d+$)");

  private static final String NONDET_VARIABLE = "__nondet__";
  static final String NONDET_FLAG_VARIABLE = NONDET_VARIABLE + "flag__";
  private static final CType NONDET_TYPE = CNumericTypes.SIGNED_INT;
  private final FormulaType<?> NONDET_FORMULA_TYPE;

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final CtoFormulaConverter converter;
  private final @Nullable CtoWpConverter wpConverter;
  private final PathFormulaBuilderFactory pfbFactory;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  @Option(
      secure = true,
      description = "add special information to formulas about non-deterministic functions")
  private boolean useNondetFlags = false;

  public PathFormulaManagerImpl(
      FormulaManagerView pFmgr,
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      AnalysisDirection pDirection)
      throws InvalidConfigurationException {

    this(
        pFmgr,
        config,
        pLogger,
        pShutdownNotifier,
        pCfa.getMachineModel(),
        pCfa.getVarClassification(),
        pDirection);
  }

  public PathFormulaManagerImpl(
      FormulaManagerView pFmgr,
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      MachineModel pMachineModel,
      Optional<VariableClassification> pVariableClassification,
      AnalysisDirection pDirection)
      throws InvalidConfigurationException {

    config.inject(this, PathFormulaManagerImpl.class);

    fmgr = pFmgr;
    bfmgr = fmgr.getBooleanFormulaManager();
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;

    if (handlePointerAliasing) {
      final FormulaEncodingWithPointerAliasingOptions options =
          new FormulaEncodingWithPointerAliasingOptions(config);
      if (options.useQuantifiersOnArrays()) {
        try {
          fmgr.getQuantifiedFormulaManager();
        } catch (UnsupportedOperationException e) {
          throw new InvalidConfigurationException(
              "Cannot use quantifiers with current solver, either choose a different solver or"
                  + " disable quantifiers.");
        }
      }
      if (options.useArraysForHeap()) {
        try {
          fmgr.getArrayFormulaManager();
        } catch (UnsupportedOperationException e) {
          throw new InvalidConfigurationException(
              "Cannot use arrays with current solver, either choose a different solver or disable"
                  + " arrays.");
        }
      }

      TypeHandlerWithPointerAliasing aliasingTypeHandler =
          new TypeHandlerWithPointerAliasing(pLogger, pMachineModel, options);

      converter =
          new CToFormulaConverterWithPointerAliasing(
              options,
              fmgr,
              pMachineModel,
              pVariableClassification,
              logger,
              shutdownNotifier,
              aliasingTypeHandler,
              pDirection);

      wpConverter = null;

    } else {
      final FormulaEncodingOptions options = new FormulaEncodingOptions(config);
      CtoFormulaTypeHandler typeHandler = new CtoFormulaTypeHandler(pLogger, pMachineModel);
      converter =
          new CtoFormulaConverter(
              options,
              fmgr,
              pMachineModel,
              pVariableClassification,
              logger,
              shutdownNotifier,
              typeHandler,
              pDirection);

      wpConverter =
          new CtoWpConverter(
              options,
              fmgr,
              pMachineModel,
              pVariableClassification,
              logger,
              shutdownNotifier,
              typeHandler,
              pDirection);

      logger.log(
          Level.WARNING,
          "Handling of pointer aliasing is disabled, analysis is unsound if aliased pointers"
              + " exist.");
    }

    switch (pathFormulaBuilderVariant) {
      case DEFAULT:
        pfbFactory = new DefaultPathFormulaBuilder.Factory();
        break;
      case SYMBOLICLOCATIONS:
        pfbFactory =
            new SymbolicLocationPathFormulaBuilder.Factory(
                new CBinaryExpressionBuilder(pMachineModel, pLogger));
        break;
      default:
        throw new InvalidConfigurationException("Invalid type of path formula builder specified!");
    }

    NONDET_FORMULA_TYPE = converter.getFormulaTypeFromCType(NONDET_TYPE);
  }

  @Override
  public Pair<PathFormula, ErrorConditions> makeAndWithErrorConditions(
      PathFormula pOldFormula, final CFAEdge pEdge)
      throws CPATransferException, InterruptedException {
    ErrorConditions errorConditions = new ErrorConditions(bfmgr);
    PathFormula pf = makeAnd(pOldFormula, pEdge, errorConditions);

    return Pair.of(pf, errorConditions);
  }

  private PathFormula makeAnd(
      PathFormula pOldFormula, final CFAEdge pEdge, ErrorConditions errorConditions)
      throws UnrecognizedCodeException, UnrecognizedCFAEdgeException, InterruptedException {
    PathFormula pf = converter.makeAnd(pOldFormula, pEdge, errorConditions);

    if (useNondetFlags) {
      SSAMapBuilder ssa = pf.getSsa().builder();

      int lNondetIndex = ssa.getIndex(NONDET_VARIABLE);
      int lFlagIndex = ssa.getIndex(NONDET_FLAG_VARIABLE);

      if (lNondetIndex != lFlagIndex) {
        if (lFlagIndex < 0) {
          lFlagIndex =
              1; // ssa indices start with 2, so next flag that is generated also uses index 2
        }

        BooleanFormula edgeFormula = pf.getFormula();

        for (int lIndex = lFlagIndex + 1; lIndex <= lNondetIndex; lIndex++) {
          Formula nondetVar = fmgr.makeVariable(NONDET_FORMULA_TYPE, NONDET_FLAG_VARIABLE, lIndex);
          BooleanFormula lAssignment =
              fmgr.assignment(nondetVar, fmgr.makeNumber(NONDET_FORMULA_TYPE, 1));
          edgeFormula = bfmgr.and(edgeFormula, lAssignment);
        }

        // update ssa index of nondet flag
        // setSsaIndex(ssa, Variable.create(NONDET_FLAG_VARIABLE, getNondetType()), lNondetIndex);
        ssa.setIndex(NONDET_FLAG_VARIABLE, NONDET_TYPE, lNondetIndex);

        pf = new PathFormula(edgeFormula, ssa.build(), pf.getPointerTargetSet(), pf.getLength());
      }
    }
    if (simplifyGeneratedPathFormulas) {
      pf = pf.withFormula(fmgr.simplify(pf.getFormula()));
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
            CFANode.newDummyCFANode(),
            CFANode.newDummyCFANode(),
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
  public PathFormula makeConjunction(List<PathFormula> pPathFormulas) {
    if (pPathFormulas.isEmpty()) {
      return makeEmptyPathFormula();
    }
    BooleanFormula conjunction = bfmgr.and(Lists.transform(pPathFormulas, PathFormula::getFormula));
    int lengthSum = pPathFormulas.stream().mapToInt(PathFormula::getLength).sum();
    PathFormula last = Iterables.getLast(pPathFormulas);
    return new PathFormula(conjunction, last.getSsa(), last.getPointerTargetSet(), lengthSum);
  }

  @Override
  public PathFormula makeEmptyPathFormula() {
    return new PathFormula(
        bfmgr.makeTrue(), SSAMap.emptySSAMap(), PointerTargetSet.emptyPointerTargetSet(), 0);
  }

  @Override
  public PathFormula makeEmptyPathFormulaWithContextFrom(PathFormula oldFormula) {
    return new PathFormula(
        bfmgr.makeTrue(), oldFormula.getSsa(), oldFormula.getPointerTargetSet(), 0);
  }

  @Override
  public PathFormula makeEmptyPathFormulaWithContext(SSAMap pSsaMap, PointerTargetSet pPts) {
    return new PathFormula(bfmgr.makeTrue(), pSsaMap, pPts, 0);
  }

  @Override
  public PathFormula makeOr(final PathFormula pathFormula1, final PathFormula pathFormula2)
      throws InterruptedException {

    final BooleanFormula formula1 = pathFormula1.getFormula();
    final BooleanFormula formula2 = pathFormula2.getFormula();
    final SSAMap ssa1 = pathFormula1.getSsa();
    final SSAMap ssa2 = pathFormula2.getSsa();

    final PointerTargetSet pts1 = pathFormula1.getPointerTargetSet();
    final PointerTargetSet pts2 = pathFormula2.getPointerTargetSet();

    final SSAMapMerger merger =
        new SSAMapMerger(useNondetFlags, fmgr, converter, shutdownNotifier, NONDET_FORMULA_TYPE);
    final MergeResult<SSAMap> mergeSSAResult = merger.mergeSSAMaps(ssa1, pts1, ssa2, pts2);
    final SSAMapBuilder newSSA = mergeSSAResult.getResult().builder();

    final MergeResult<PointerTargetSet> mergePtsResult =
        converter.mergePointerTargetSets(pts1, pts2, newSSA);

    // (?) Do not swap these two lines, that makes a huge difference in performance (?) !
    final BooleanFormula newFormula1 =
        bfmgr.and(
            formula1,
            bfmgr.and(mergeSSAResult.getLeftConjunct(), mergePtsResult.getLeftConjunct()));
    final BooleanFormula newFormula2 =
        bfmgr.and(
            formula2,
            bfmgr.and(mergeSSAResult.getRightConjunct(), mergePtsResult.getRightConjunct()));
    final BooleanFormula newFormula =
        bfmgr.and(
            bfmgr.or(newFormula1, newFormula2),
            bfmgr.and(mergeSSAResult.getFinalConjunct(), mergePtsResult.getFinalConjunct()));
    final PointerTargetSet newPTS = mergePtsResult.getResult();
    final int newLength = Math.max(pathFormula1.getLength(), pathFormula2.getLength());

    PathFormula out = new PathFormula(newFormula, newSSA.build(), newPTS, newLength);
    if (simplifyGeneratedPathFormulas) {
      out = out.withFormula(fmgr.simplify(out.getFormula()));
    }
    return out;
  }

  @Override
  public PointerTargetSet mergePts(
      PointerTargetSet pPts1, PointerTargetSet pPts2, SSAMapBuilder pSSA)
      throws InterruptedException {
    return converter.mergePointerTargetSets(pPts1, pPts2, pSSA).getResult();
  }

  @Override
  public PathFormula makeAnd(PathFormula pPathFormula, BooleanFormula pOtherFormula) {
    SSAMap ssa = pPathFormula.getSsa();
    BooleanFormula otherFormula = fmgr.instantiate(pOtherFormula, ssa);
    BooleanFormula resultFormula = bfmgr.and(pPathFormula.getFormula(), otherFormula);
    final PointerTargetSet pts = pPathFormula.getPointerTargetSet();
    return new PathFormula(resultFormula, ssa, pts, pPathFormula.getLength());
  }

  @Override
  public PathFormula makeFormulaForPath(List<CFAEdge> pPath)
      throws CPATransferException, InterruptedException {
    PathFormula pathFormula = makeEmptyPathFormula();
    for (CFAEdge edge : pPath) {
      pathFormula = makeAnd(pathFormula, edge);
    }
    return pathFormula;
  }

  /** {@inheritDoc} */
  @Override
  public Formula makeFormulaForVariable(PathFormula pContext, String pVarName, CType pType) {
    return converter.makeFormulaForVariable(
        pContext.getSsa(), pContext.getPointerTargetSet(), pVarName, pType);
  }

  /** {@inheritDoc} */
  @Override
  public Formula makeFormulaForUninstantiatedVariable(
      String pVarName, CType pType, PointerTargetSet pContextPTS, boolean forcePointerDereference) {
    return converter.makeFormulaForUninstantiatedVariable(
        pVarName, pType, pContextPTS, forcePointerDereference);
  }

  /**
   * Build a formula containing a predicate for all branching situations in the ARG. If a satisfying
   * assignment is created for this formula, it can be used to find out which paths in the ARG are
   * feasible.
   *
   * <p>This method may be called with an empty set, in which case it does nothing and returns the
   * formula "true".
   *
   * @param elementsOnPath The ARG states that should be considered.
   * @return A formula containing a predicate for each branching.
   */
  @Override
  public BooleanFormula buildBranchingFormula(Set<ARGState> elementsOnPath)
      throws CPATransferException, InterruptedException {
    return buildBranchingFormula(elementsOnPath, ImmutableMap.of());
  }

  /**
   * Build a formula containing a predicate for all branching situations in the ARG. If a satisfying
   * assignment is created for this formula, it can be used to find out which paths in the ARG are
   * feasible.
   *
   * <p>This method may be called with an empty set, in which case it does nothing and returns the
   * formula "true".
   *
   * @param elementsOnPath The ARG states that should be considered.
   * @param parentFormulasOnPath TODO.
   * @return A formula containing a predicate for each branching.
   */
  @Override
  public BooleanFormula buildBranchingFormula(
      Set<ARGState> elementsOnPath, Map<Pair<ARGState, CFAEdge>, PathFormula> parentFormulasOnPath)
      throws CPATransferException, InterruptedException {
    // build the branching formula that will help us find the real error path
    List<BooleanFormula> branchingFormula = new ArrayList<>();
    for (final ARGState pathElement : elementsOnPath) {
      final ImmutableSet<ARGState> childrenOnPath =
          from(pathElement.getChildren()).filter(elementsOnPath::contains).toSet();

      if (childrenOnPath.size() > 1) {
        if (childrenOnPath.size() > 2) {
          // can't create branching formula
          if (from(childrenOnPath).anyMatch(AbstractStates::isTargetState)) {
            // We expect this situation of one of the children is a target state created by
            // PredicateCPA.
            continue;
          } else {
            logger.log(
                Level.WARNING,
                "ARG branching with more than two outgoing edges at ARG node "
                    + pathElement.getStateId()
                    + ".");
            return bfmgr.makeTrue();
          }
        }

        FluentIterable<CFAEdge> outgoingEdges =
            from(childrenOnPath).transform(pathElement::getEdgeToChild);
        if (!outgoingEdges.allMatch(Predicates.instanceOf(AssumeEdge.class))) {
          if (from(childrenOnPath).anyMatch(AbstractStates::isTargetState)) {
            // We expect this situation of one of the children is a target state created by
            // PredicateCPA.
            continue;
          } else {
            logger.log(
                Level.WARNING,
                "ARG branching without AssumeEdge at ARG node " + pathElement.getStateId() + ".");
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
          logger.log(
              Level.WARNING,
              "Ambiguous ARG branching at ARG node " + pathElement.getStateId() + ".");
          return bfmgr.makeTrue();
        }

        BooleanFormula pred =
            bfmgr.makeVariable(BRANCHING_PREDICATE_NAME + pathElement.getStateId());

        Pair<ARGState, CFAEdge> key = Pair.of(pathElement, positiveEdge);
        PathFormula pf = parentFormulasOnPath.get(key);

        if (pf == null) {
          // create formula by edge, be sure to use the correct SSA indices!
          // TODO the class PathFormulaManagerImpl should not depend on PredicateAbstractState,
          // it is used without PredicateCPA as well.
          PredicateAbstractState pe =
              AbstractStates.extractStateByType(pathElement, PredicateAbstractState.class);
          if (pe == null) {
            logger.log(
                Level.WARNING, "Cannot find precise error path information without PredicateCPA");
            return bfmgr.makeTrue();
          } else {
            pf = pe.getPathFormula();
          }
          pf = makeEmptyPathFormulaWithContextFrom(pf); // reset everything except SSAMap
          pf = this.makeAnd(pf, positiveEdge); // conjunct with edge
        }
        BooleanFormula equiv = bfmgr.equivalence(pred, pf.getFormula());
        branchingFormula.add(equiv);
      }
    }
    return bfmgr.and(branchingFormula);
  }

  /**
   * Extract the information about the branching predicates created by {@link
   * #buildBranchingFormula(Set)} from a satisfying assignment.
   *
   * <p>A map is created that stores for each ARGState (using its element id as the map key) which
   * edge was taken (the positive or the negated one).
   *
   * @param model A satisfying assignment that should contain values for branching predicates.
   * @return A map from ARG state id to a boolean value indicating direction.
   */
  @Override
  public ImmutableMap<Integer, Boolean> getBranchingPredicateValuesFromModel(
      Iterable<ValueAssignment> model) {
    // Do not use fmgr here, this fails if a separate solver is used for interpolation.
    if (!model.iterator().hasNext()) {
      logger.log(Level.WARNING, "No satisfying assignment given by solver!");
      return ImmutableMap.of();
    }

    ImmutableMap.Builder<Integer, Boolean> preds = ImmutableMap.builder();
    for (ValueAssignment entry : model) {
      String canonicalName = entry.getName();

      if (entry.getKey() instanceof BooleanFormula) {
        String name = BRANCHING_PREDICATE_NAME_PATTERN.matcher(canonicalName).replaceFirst("");
        if (!name.equals(canonicalName)) {
          // pattern matched, so it's a variable with __ART__ in it

          // no NumberFormatException because of RegExp match earlier
          Integer nodeId = Integer.parseInt(name);
          preds.put(nodeId, (Boolean) entry.getValue()); // fails on duplicate key
        }
      }
    }
    return preds.buildOrThrow();
  }

  @Override
  public void clearCaches() {}

  @Override
  public Formula expressionToFormula(PathFormula pFormula, CIdExpression expr, CFAEdge edge)
      throws UnrecognizedCodeException {
    return converter.buildTermFromPathFormula(pFormula, expr, edge);
  }

  @Override
  public BooleanFormula buildImplicationTestAsUnsat(PathFormula pF1, PathFormula pF2)
      throws InterruptedException {
    final SSAMapMerger merger =
        new SSAMapMerger(useNondetFlags, fmgr, converter, shutdownNotifier, NONDET_FORMULA_TYPE);
    BooleanFormula bF = pF2.getFormula();
    return bfmgr.and(
        merger.addMergeAssumptions(
            pF1.getFormula(), pF1.getSsa(), pF1.getPointerTargetSet(), pF2.getSsa()),
        bfmgr.not(bF));
  }

  @Override
  public BooleanFormula addBitwiseAxiomsIfNeeded(
      final BooleanFormula pMainFormula, final BooleanFormula pExtractionFormula) {
    if (fmgr.useBitwiseAxioms()) {
      BooleanFormula bitwiseAxioms = fmgr.getBitwiseAxioms(pExtractionFormula);
      if (!fmgr.getBooleanFormulaManager().isTrue(bitwiseAxioms)) {
        logger.log(Level.ALL, "DEBUG_3", "ADDED BITWISE AXIOMS:", bitwiseAxioms);
        return fmgr.getBooleanFormulaManager().and(pMainFormula, bitwiseAxioms);
      }
    }

    return pMainFormula;
  }

  @Override
  public PathFormulaBuilder createNewPathFormulaBuilder() {
    return pfbFactory.create();
  }

  @Override
  public void printStatistics(PrintStream out) {
    converter.printStatistics(out);
  }

  @Override
  public BooleanFormula buildWeakestPrecondition(
      final CFAEdge pEdge, final BooleanFormula pPostcond)
      throws UnrecognizedCFAEdgeException, UnrecognizedCodeException, InterruptedException {

    // TODO: refactor as soon as there is a WP converter with pointer aliasing

    if (wpConverter != null) {
      return wpConverter.makePreconditionForEdge(pEdge, pPostcond);
    }

    throw new UnsupportedOperationException();
  }
}
