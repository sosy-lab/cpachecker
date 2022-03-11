// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter.PARAM_VARIABLE_NAME;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.GenericReducer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.FreshValueProvider;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "cpa.predicate.bam")
public class BAMPredicateReducer
    extends GenericReducer<PredicateAbstractState, PredicatePrecision> {

  private final PathFormulaManager pmgr;
  private final PredicateAbstractionManager pamgr;
  private final LogManager logger;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;
  private final RegionManager rmgr;
  private final ShutdownNotifier shutdownNotifier;
  private final Set<String> addressedVariables;

  private final Map<BooleanFormula, Set<String>> variableCache = new HashMap<>();

  /**
   * A meaning of the following options is a number of problems in BAM: sometimes it is more
   * efficient not to reduce precision, than to have a RepeatedCounterexampleException. The results
   * without both of reductions are better (now). However, there are not only one possible
   * combination of the options, so, (at least now) there should not be used a single option for
   * switching to NoOpReducer.
   */
  @Option(description = "Enable/disable precision reduction at the BAM block entry", secure = true)
  private boolean usePrecisionReduction = true;

  @Option(
      description = "Enable/disable abstraction reduction at the BAM block entry",
      secure = true)
  private boolean useAbstractionReduction = true;

  public BAMPredicateReducer(BAMPredicateCPA cpa, Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    pmgr = cpa.getPathFormulaManager();
    pamgr = cpa.getPredicateManager();
    fmgr = cpa.getSolver().getFormulaManager();
    bfmgr = cpa.getSolver().getFormulaManager().getBooleanFormulaManager();
    rmgr = cpa.getAbstractionManager().getRegionCreator();
    logger = cpa.getLogger();
    shutdownNotifier = cpa.getShutdownNotifier();
    addressedVariables = cpa.getCfa().getVarClassification().orElseThrow().getAddressedVariables();
  }

  @Override
  public PredicateAbstractState getVariableReducedState0(
      PredicateAbstractState predicateElement, Block pContext, CFANode pLocation)
      throws InterruptedException {

    PathFormula pathFormula = predicateElement.getPathFormula();
    PersistentMap<CFANode, Integer> abstractionLocations =
        predicateElement.getAbstractionLocationsOnPath().empty();

    Preconditions.checkState(predicateElement.isAbstractionState());
    Preconditions.checkState(bfmgr.isTrue(pathFormula.getFormula()));

    AbstractionFormula abstraction = predicateElement.getAbstractionFormula();

    if (useAbstractionReduction) {
      Region reducedAbstraction =
          splitAbstractionForReduction(abstraction.asRegion(), pContext).getFirst();
      abstraction =
          pamgr.makeAbstractionFormula(
              reducedAbstraction, pathFormula.getSsa(), abstraction.getBlockFormula());
    }

    return PredicateAbstractState.mkAbstractionState(
        pathFormula, abstraction.copyOf(), abstractionLocations);
  }

  /**
   * Split an abstraction into a reduced abstraction and a conjunction of predicates, which were
   * removed from the abstraction.
   *
   * @return two regions are returned: (1) the reduced abstraction and (2) the removed predicates.
   */
  private Pair<Region, Region> splitAbstractionForReduction(Region abstraction, final Block context)
      throws InterruptedException {

    final Set<AbstractionPredicate> predicates = pamgr.extractPredicates(abstraction);
    final Set<AbstractionPredicate> irrelevantPredicates =
        Sets.difference(predicates, getRelevantPredicates(context, predicates));

    Region conjunction = rmgr.makeTrue();
    for (AbstractionPredicate predicate : irrelevantPredicates) {
      shutdownNotifier.shutdownIfNecessary();

      // check whether ABS=>PRED, because only then we can guarantee that
      // ( ( exists PRED: f(ABS,PRED) ) and PRED ) == f(ABS,PRED),
      // which is required for a valid (and precise) reduction and expansion afterwards
      boolean abstractionImpliesPredicate = false;
      try {
        abstractionImpliesPredicate = rmgr.entails(abstraction, predicate.getAbstractVariable());
      } catch (SolverException e) {
        logger.logException(
            Level.INFO, e, "cannot check implication for predicate, predicate is relevant");
      }
      if (abstractionImpliesPredicate) {
        conjunction = rmgr.makeAnd(conjunction, predicate.getAbstractVariable());
        abstraction = rmgr.makeExists(abstraction, predicate.getAbstractVariable());
      }
    }

    return Pair.of(abstraction, conjunction);
  }

  /**
   * Conservatively compute only the predicates that are relevant for the given context and fulfill
   * the following requirements:
   *
   * <ul>
   *   <li>contain variables never used outside of the block, also transitively.
   *   <li>do not encode pointers, addresses, return variables.
   * </ul>
   *
   * <p>We later may only remove those predicates that are implied by the abstraction (i.e. never
   * reduce internal parts of a boolean combination)
   */
  private Set<AbstractionPredicate> getRelevantPredicates(
      Block pContext, Collection<AbstractionPredicate> predicates) throws InterruptedException {

    final Set<AbstractionPredicate> relevantPredicates = new LinkedHashSet<>();
    Set<String> relevantVariables = new LinkedHashSet<>();
    Set<AbstractionPredicate> irrelevantPredicates = new LinkedHashSet<>();

    // get predicates that are directly relevant
    for (AbstractionPredicate predicate : predicates) {
      Set<String> variables = getVariables(predicate);
      if (isAnyVariableRelevant(pContext.getVariables(), variables)) {
        relevantPredicates.add(predicate);
        relevantVariables.addAll(variables);
      } else {
        irrelevantPredicates.add(predicate);
      }
    }

    // get transitive hull, i.e.,
    // predicates that are important because they contain variables used in relevant predicates.
    while (!relevantVariables.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();
      Set<String> newRelevantVariables = new LinkedHashSet<>();
      Set<AbstractionPredicate> newIrrelevantPredicates = new LinkedHashSet<>();
      for (AbstractionPredicate predicate : irrelevantPredicates) { // shrinking with each iteration
        Set<String> variables = getVariables(predicate);
        if (isAnyVariableRelevant(relevantVariables, variables)) {
          relevantPredicates.add(predicate);
          newRelevantVariables.addAll(Sets.difference(variables, relevantVariables));
        } else {
          newIrrelevantPredicates.add(predicate);
        }
      }
      relevantVariables = newRelevantVariables;
      irrelevantPredicates = newIrrelevantPredicates;
    }

    return relevantPredicates;
  }

  private Set<String> getVariables(AbstractionPredicate predicate) {
    BooleanFormula atom = predicate.getSymbolicAtom();
    Set<String> variables = variableCache.get(atom);
    if (variables == null) {
      variables = fmgr.extractVariableNames(atom);
      variableCache.put(atom, variables);
    }
    return variables;
  }

  /** return whether any new variable is relevant for the existing variables. */
  private boolean isAnyVariableRelevant(Set<String> relevantVariables, Set<String> newVariables) {
    for (String var : Sets.union(addressedVariables, relevantVariables)) {

      // short cut
      if (newVariables.contains(var)) {
        return true;
      }

      // here we have to handle some imprecise information,
      // because the encoding of variables in predicates and referencedVariables might differ.
      // Examples: "__ADDRESS_OF_xyz" vs "xyz", "ssl3_accept::s->state" vs "ssl3_accept::s"
      // This handling causes an over-approximation of the set of variables, because
      // a predicate-variable "f" is relevant, if "foo" is one of the referenced variables.
      for (String variable : newVariables) {
        if (variable.contains(var)) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public PredicateAbstractState getVariableExpandedState0(
      PredicateAbstractState rootState, Block pReducedContext, PredicateAbstractState reducedState)
      throws InterruptedException {

    Preconditions.checkState(reducedState.isAbstractionState());
    Preconditions.checkState(rootState.isAbstractionState());

    PathFormula pathFormula = reducedState.getPathFormula();
    Preconditions.checkState(
        bfmgr.isTrue(pathFormula.getFormula()),
        "Formula should be TRUE, but formula is %s",
        pathFormula.getFormula());

    SSAMap ssa = pathFormula.getSsa();
    AbstractionFormula abstractionFormula = reducedState.getAbstractionFormula();

    if (useAbstractionReduction) {
      ssa = copyMissingIndizes(rootState.getPathFormula().getSsa(), ssa);
      Region removedPredicates =
          splitAbstractionForReduction(
                  rootState.getAbstractionFormula().asRegion(), pReducedContext)
              .getSecond();
      Region expandedAbstraction = rmgr.makeAnd(abstractionFormula.asRegion(), removedPredicates);
      abstractionFormula =
          pamgr.makeAbstractionFormula(
              expandedAbstraction, ssa, abstractionFormula.getBlockFormula());
    }

    PointerTargetSet rootPts = rootState.getPathFormula().getPointerTargetSet();
    PointerTargetSet reducedPts = reducedState.getPathFormula().getPointerTargetSet();

    SSAMapBuilder ssaBuilder = ssa.builder();
    PointerTargetSet newPts = pmgr.mergePts(rootPts, reducedPts, ssaBuilder);
    ssa = ssaBuilder.build();

    pathFormula = pathFormula.withContext(ssa, newPts);

    return PredicateAbstractState.mkAbstractionState(
        pathFormula, abstractionFormula.copyOf(), reducedState.getAbstractionLocationsOnPath());
  }

  @Override
  public Object getHashCodeForState0(PredicateAbstractState state, PredicatePrecision precision) {
    return Pair.of(state.getAbstractionFormula().asRegion(), precision);
  }

  @Override
  public PredicatePrecision getVariableExpandedPrecision0(
      PredicatePrecision rootPrecision, Block pRootContext, PredicatePrecision reducedPrecision) {
    if (usePrecisionReduction) {
      return rootPrecision.mergeWith(reducedPrecision);
    } else {
      return reducedPrecision;
    }
  }

  @Override
  public Precision getVariableReducedPrecision0(PredicatePrecision pPrecision, Block context) {

    if (usePrecisionReduction) {

      assert pPrecision.getLocationInstancePredicates().isEmpty()
          : "TODO: need to handle location-instance-specific predicates in"
              + " ReducedPredicatePrecision";
      /* LocationInstancePredicates is useless, because a block can be visited
       * several times along a error path and the index would always start from 0 again.
       * Thus we ignore LocationInstancePredicates and hope nobody is using them.
       * TODO can we assure this?
       */

      // create reduced precision

      // we only need global predicates with used variables
      final Collection<AbstractionPredicate> globalPredicates = pPrecision.getGlobalPredicates();

      // we only need function predicates with used variables
      final ImmutableSetMultimap.Builder<String, AbstractionPredicate> functionPredicates =
          ImmutableSetMultimap.builder();
      for (String functionname : pPrecision.getFunctionPredicates().keySet()) {
        if (context.getFunctions().contains(functionname)) {
          functionPredicates.putAll(
              functionname, pPrecision.getFunctionPredicates().get(functionname));
        }
      }

      // we only need local predicates with used variables and with nodes from the block
      final ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> localPredicates =
          ImmutableSetMultimap.builder();
      for (CFANode node : pPrecision.getLocalPredicates().keySet()) {
        if (context.getNodes().contains(node)) {
          // TODO handle location-instance-specific predicates
          // Without support for them, we can just pass 0 as locInstance parameter
          localPredicates.putAll(node, pPrecision.getPredicates(node, 0));
        }
      }

      return new PredicatePrecision(
          ImmutableSetMultimap.of(),
          localPredicates.build(),
          functionPredicates.build(),
          globalPredicates);
    } else {
      return pPrecision;
    }
  }

  @Override
  public int measurePrecisionDifference0(
      PredicatePrecision pPrecision, PredicatePrecision pOtherPrecision) {
    return pPrecision.calculateDifferenceTo(pOtherPrecision);
  }

  @Override
  public AbstractState getVariableReducedStateForProofChecking(
      AbstractState pExpandedState, Block pContext, CFANode pCallNode) {
    return pExpandedState;
  }

  @Override
  public AbstractState getVariableExpandedStateForProofChecking(
      AbstractState pRootState, Block pReducedContext, AbstractState pReducedState)
      throws InterruptedException {

    PredicateAbstractState rootState = (PredicateAbstractState) pRootState;
    PredicateAbstractState reducedState = (PredicateAbstractState) pReducedState;

    if (!reducedState.isAbstractionState()) {
      return reducedState;
    }

    AbstractionFormula rootAbstraction = rootState.getAbstractionFormula();
    AbstractionFormula reducedAbstraction = reducedState.getAbstractionFormula();

    // De-serialized AbstractionFormula are missing the Regions which we need for expand(),
    // so we re-create them here.
    rootAbstraction =
        pamgr.asAbstraction(rootAbstraction.asFormula(), rootAbstraction.getBlockFormula());
    reducedAbstraction =
        pamgr.asAbstraction(reducedAbstraction.asFormula(), reducedAbstraction.getBlockFormula());

    // TODO why were predicates retrieved from instantiated formula in the past and not directly?
    // Set<AbstractionPredicate> rootPredicates =
    // pamgr.getPredicatesForAtomsOf(rootAbstraction.asInstantiatedFormula());

    PathFormula oldPathFormula = reducedState.getPathFormula();
    SSAMap oldSSA = oldPathFormula.getSsa();

    // pathFormula.getSSa() might not contain index for the newly added variables in predicates;
    // while the actual index is not really important at this point,
    // there still should be at least _some_ index for each variable of the abstraction formula.
    SSAMap newSSA = copyMissingIndizes(rootState.getPathFormula().getSsa(), oldSSA);
    // FIXME: seems buggy because it completely forgets the PointerTargetSet!
    PathFormula newPathFormula =
        pmgr.makeEmptyPathFormulaWithContext(newSSA, PointerTargetSet.emptyPointerTargetSet());
    Region removedPredicates =
        splitAbstractionForReduction(rootAbstraction.asRegion(), pReducedContext).getSecond();
    Region expandedAbstraction = rmgr.makeAnd(reducedAbstraction.asRegion(), removedPredicates);

    AbstractionFormula newAbstractionFormula =
        pamgr.makeAbstractionFormula(
            expandedAbstraction, newSSA, reducedAbstraction.getBlockFormula());
    PersistentMap<CFANode, Integer> abstractionLocations =
        rootState.getAbstractionLocationsOnPath();

    return PredicateAbstractState.mkAbstractionState(
        newPathFormula, newAbstractionFormula.copyOf(), abstractionLocations);
  }

  @Override
  public PredicateAbstractState rebuildStateAfterFunctionCall0(
      PredicateAbstractState rootState,
      PredicateAbstractState entryState,
      PredicateAbstractState expandedState,
      FunctionExitNode exitLocation) {
    Preconditions.checkState(rootState.isAbstractionState());
    Preconditions.checkState(entryState.isAbstractionState());
    Preconditions.checkState(expandedState.isAbstractionState());

    final PersistentMap<CFANode, Integer> abstractionLocations =
        expandedState.getAbstractionLocationsOnPath();

    // we have:
    // - abstraction of rootState with ssa                --> use as it is
    // - callEdge-pathFormula with ssa (from rootState)   --> use as it is, with updated SSAMap
    // - abstraction of functioncall (expandedSSA)        --> instantiate, with updated SSAMap, so
    // that:
    //           - only param and return-var overlap to callEdge
    //           - all other vars are distinct
    final String calledFunction = exitLocation.getFunctionName();
    final PathFormula functionCall = entryState.getAbstractionFormula().getBlockFormula();
    final SSAMap entrySsaWithRet = functionCall.getSsa();
    final SSAMapBuilder entrySsaWithRetBuilder = entrySsaWithRet.builder();
    final SSAMapBuilder summSsa =
        rootState.getAbstractionFormula().getBlockFormula().getSsa().builder();

    final SSAMap expandedSSA = expandedState.getAbstractionFormula().getBlockFormula().getSsa();
    for (String var : expandedSSA.allVariables()) {
      final CType type = expandedSSA.getType(var);
      if (var.startsWith(calledFunction + "::") && var.endsWith(PARAM_VARIABLE_NAME)) {
        int newIndex = entrySsaWithRet.getIndex(var);
        assert entrySsaWithRet.containsVariable(var)
            : "param for function is not used in functioncall";
        entrySsaWithRetBuilder.setIndex(var, type, newIndex);
        setFreshValueBasis(summSsa, var, newIndex);

      } else if (exitLocation.getEntryNode().getReturnVariable().isPresent()
          && exitLocation.getEntryNode().getReturnVariable().get().getQualifiedName().equals(var)) {
        // var.startsWith(calledFunction + "::") && var.endsWith(RETURN_VARIABLE_NAME)
        final int newIndex =
            Math.max(expandedSSA.getIndex(var), entrySsaWithRetBuilder.getFreshIndex(var));
        entrySsaWithRetBuilder.setIndex(var, type, newIndex);
        summSsa.setIndex(var, type, newIndex);

      } else if (!entrySsaWithRet.containsVariable(var)) {
        // non-existent index for variable only used in functioncall, just copy
        final int newIndex = expandedSSA.getIndex(var);
        entrySsaWithRetBuilder.setIndex(var, type, newIndex);
        summSsa.setIndex(var, type, newIndex);

      } else {
        final int newIndex = entrySsaWithRetBuilder.getFreshIndex(var);
        entrySsaWithRetBuilder.setIndex(var, type, newIndex);
        setFreshValueBasis(summSsa, var, newIndex);
      }
    }

    final SSAMap newEntrySsaWithRet = entrySsaWithRetBuilder.build();
    final SSAMap newSummSsa = summSsa.build();

    // TODO: This code updates only the SSAMaps of both path formulas, but not the PointerTargetSet!
    // This is likely buggy.

    // function-call needs have new retvars-indices.
    PathFormula functionCallWithSSA =
        functionCall.withContext(newEntrySsaWithRet, functionCall.getPointerTargetSet());

    // concat function-call with function-summary,
    // function-summary will be instantiated with indices for params and retvars.
    PathFormula executedFunction =
        pmgr.makeAnd(functionCallWithSSA, expandedState.getAbstractionFormula().asFormula());

    // after function-execution we have to re-use the previous indices (fromouter scope),
    // thus lets change the SSAmap.
    PathFormula executedFunctionWithSSA =
        executedFunction.withContext(newSummSsa, executedFunction.getPointerTargetSet());

    // everything is prepared, so build a new AbstractionState.
    // we do this as 'future abstraction', because we do not have enough information
    // (necessary classes and managers) for the abstraction-process at this place.
    PredicateAbstractState rebuildState =
        PredicateAbstractState.mkNonAbstractionState(
            executedFunctionWithSSA, rootState.getAbstractionFormula(), abstractionLocations);

    logger.log(
        Level.ALL,
        "\noldAbs: ",
        rootState.getAbstractionFormula().asInstantiatedFormula(),
        "\ncall: ",
        functionCallWithSSA,
        "\nsumm: ",
        expandedState.getAbstractionFormula().asFormula(),
        "\nexe: ",
        executedFunction,
        "\nentrySsaRet",
        newEntrySsaWithRet,
        "\nsummSsaRet",
        newSummSsa);

    return rebuildState;
  }

  /**
   * copy over all indices from FROM-SSA that are missing in TO-SSA.
   *
   * @return a new SSAMap with the merged indices
   */
  private static SSAMap copyMissingIndizes(SSAMap from, SSAMap to) {
    SSAMapBuilder builder = to.builder();
    for (String var : from.allVariables()) {
      // if we do not have the index in the TO map.
      if (!to.containsVariable(var)) {
        // add an index (with the value of FROM map)
        builder.setIndex(var, from.getType(var), from.getIndex(var));
      }
    }
    return builder.build();
  }

  /**
   * rootSSA might not contain correct indices for the local variables of calling function-scope. so
   * lets build a new SSA from: - local variables from rootSSA, -> update indices (their indices
   * will have "holes") - local variables from expandedSSA, -> ignore indices (their indices are the
   * "holes") - global variables from expandedSSA, -> update indices (we have to keep them) - the
   * local return variables from expandedState. -> update indices (we have to keep them, there can
   * be several ret-vars from distinct functions, ignore them, they are created new, if needed) we
   * copy expandedState and override all local values.
   *
   * @param rootSSA SSA before function-call
   * @param expandedSSA SSA before function-return
   * @param functionExitNode the function-return-location
   * @return new SSAMap
   */
  static SSAMap updateIndices(
      final SSAMap rootSSA, final SSAMap expandedSSA, FunctionExitNode functionExitNode) {

    final SSAMapBuilder rootBuilder = rootSSA.builder();

    for (String var : expandedSSA.allVariables()) {

      // Depending on the scope of vars, set either only the lastUsedIndex or the default index.
      // var was used and maybe overridden inside the block
      final CType type = expandedSSA.getType(var);
      if (var.contains("::")
          && !isReturnVar(var, functionExitNode)) { // var is scoped -> not global

        if (!rootSSA.containsVariable(var)) {

          // Inner local variable, never seen before,
          // use fresh index as a basis for further assignments
          rootBuilder.setIndex(var, type, expandedSSA.builder().getFreshIndex(var));

        } else {

          // Outer variable or inner variable from previous function call
          setFreshValueBasis(
              rootBuilder,
              var,
              Math.max(expandedSSA.builder().getFreshIndex(var), rootSSA.getIndex(var)));
        }

      } else {
        // global variable in rootSSA is outdated, the correct index is in expandedSSA.
        // return-variable in rootSSA is outdated, the correct index is in expandedSSA
        // (this is the return-variable of the current function-return).

        // small trick:
        // If MAX(expIndex, rootIndex) is not expIndex,
        // we are in the rebuilding-phase of the recursive BAM-algorithm and leave a cached block.
        // in this case the index is irrelevant and can be set to expIndex (TODO really?).
        // Otherwise (the important case, MAX == expIndex)
        // we are in the refinement step and build the CEX-path.
        rootBuilder.setIndex(var, type, expandedSSA.getIndex(var));
      }
    }

    return rootBuilder.build();
  }

  private static boolean isReturnVar(String var, FunctionExitNode functionExitNode) {
    return functionExitNode.getEntryNode().getReturnVariable().isPresent()
        && functionExitNode.getEntryNode().getReturnVariable().get().getQualifiedName().equals(var);
  }

  /**
   * Set a new index (7) for an old index (3), so that getIndex() returns the old index (3) and
   * getFreshIndex() returns a higher index (8). Warning: do not use out of order!
   */
  private static void setFreshValueBasis(SSAMapBuilder ssa, String name, int idx) {
    Preconditions.checkArgument(
        idx > 0, "Indices need to be positive for this SSAMap implementation:", name, idx);
    int oldIdx = ssa.getIndex(name);
    Preconditions.checkArgument(
        idx >= oldIdx, "SSAMap updates need to be strictly monotone:", name, idx, "vs", oldIdx);

    if (idx > oldIdx) {
      PersistentSortedMap<String, Integer> newMapping =
          PathCopyingPersistentTreeMap.<String, Integer>of().putAndCopy(name, idx);
      ssa.mergeFreshValueProviderWith(new FreshValueProvider(newMapping));
    }
  }

  @Override
  public boolean canBeUsedInCache0(PredicateAbstractState predicateState) {
    return !predicateState.getPathFormula().getPointerTargetSet().hasEmptyDeferredAllocationsSet();
  }
}
