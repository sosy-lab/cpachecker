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
package org.sosy_lab.cpachecker.cpa.predicate;

import static org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter.PARAM_VARIABLE_NAME;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.FreshValueProvider;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;


public class BAMPredicateReducer implements Reducer {

  private final PathFormulaManager pmgr;
  private final PredicateAbstractionManager pamgr;
  private final BAMPredicateCPA cpa;
  private final LogManager logger;
  private final BooleanFormulaManager bfmgr;

  private final boolean reducePrecision;
  private final boolean reduceAbstraction;

  public BAMPredicateReducer(BooleanFormulaManager bfmgr, BAMPredicateCPA cpa) {
    this.pmgr = cpa.getPathFormulaManager();
    this.pamgr = cpa.getPredicateManager();
    this.bfmgr = bfmgr;
    this.logger = cpa.getLogger();
    this.cpa = cpa;
    this.reducePrecision = cpa.usePrecisionReduction();
    this.reduceAbstraction = cpa.useAbstractionReduction();
  }

  @Override
  public AbstractState getVariableReducedState(
      AbstractState pExpandedState, Block pContext,
      CFANode pLocation) throws InterruptedException {

    if (reduceAbstraction) {
      PredicateAbstractState predicateElement = (PredicateAbstractState) pExpandedState;
      Preconditions.checkState(predicateElement.isAbstractionState());

      AbstractionFormula oldAbstraction = predicateElement.getAbstractionFormula();

      Region oldRegion = oldAbstraction.asRegion();

      Collection<AbstractionPredicate> predicates = pamgr.extractPredicates(oldRegion);
      Collection<AbstractionPredicate> removePredicates =
          Sets.difference(
              new HashSet<>(predicates),
              new HashSet<>(
                  cpa.getRelevantPredicatesComputer().getRelevantPredicates(pContext, predicates)));

      PathFormula pathFormula = predicateElement.getPathFormula();

      assert bfmgr.isTrue(pathFormula.getFormula());

      AbstractionFormula newAbstraction = pamgr.reduce(oldAbstraction, removePredicates, pathFormula.getSsa());

      PersistentMap<CFANode, Integer> abstractionLocations = predicateElement.getAbstractionLocationsOnPath()
          .empty();

      return PredicateAbstractState.mkAbstractionState(pathFormula,
          newAbstraction, abstractionLocations);
    } else {
      return pExpandedState;
    }
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) throws InterruptedException {

    if (reduceAbstraction) {
      PredicateAbstractState rootState = (PredicateAbstractState) pRootState;
      PredicateAbstractState reducedState = (PredicateAbstractState) pReducedState;

      Preconditions.checkState(reducedState.isAbstractionState());
      Preconditions.checkState(rootState.isAbstractionState());

      //Note: BAM might introduce some additional abstraction if root region is not a cube
      AbstractionFormula rootAbstraction = rootState.getAbstractionFormula();
      AbstractionFormula reducedAbstraction = reducedState.getAbstractionFormula();

      Collection<AbstractionPredicate> rootPredicates = pamgr.extractPredicates(rootAbstraction.asRegion());
      Collection<AbstractionPredicate> relevantRootPredicates =
          cpa.getRelevantPredicatesComputer().getRelevantPredicates(pReducedContext, rootPredicates);
      //for each removed predicate, we have to lookup the old (expanded) value and insert it to the reducedStates region

      PathFormula oldPathFormula = reducedState.getPathFormula();
      assert bfmgr.isTrue(oldPathFormula.getFormula()) : "Formula should be TRUE, but formula is " + oldPathFormula.getFormula();
      SSAMap oldSSA = oldPathFormula.getSsa();

      //pathFormula.getSSa() might not contain index for the newly added variables in predicates; while the actual index is not really important at this point,
      //there still should be at least _some_ index for each variable of the abstraction formula.
      SSAMapBuilder builder = oldSSA.builder();
      SSAMap rootSSA = rootState.getPathFormula().getSsa();
      for (String var : rootSSA.allVariables()) {
        //if we do not have the index in the reduced map..
        if (!oldSSA.containsVariable(var)) {
          //add an index (with the value of rootSSA)
          builder.setIndex(var, rootSSA.getType(var), rootSSA.getIndex(var));
        }
      }
      SSAMap newSSA = builder.build();
      PathFormula newPathFormula = pmgr.makeNewPathFormula(oldPathFormula, newSSA);

      AbstractionFormula newAbstractionFormula =
          pamgr.expand(reducedAbstraction.asRegion(), rootAbstraction.asRegion(),
              relevantRootPredicates, newSSA, reducedAbstraction.getBlockFormula());

      PersistentMap<CFANode, Integer> abstractionLocations = reducedState.getAbstractionLocationsOnPath();

      return PredicateAbstractState.mkAbstractionState(newPathFormula,
          newAbstractionFormula, abstractionLocations);
    } else {
      return pReducedState;
    }
  }

  @Override
  public Object getHashCodeForState(AbstractState pElementKey, Precision pPrecisionKey) {

    PredicateAbstractState element = (PredicateAbstractState) pElementKey;
    PredicatePrecision precision = (PredicatePrecision) pPrecisionKey;

    /*
    Normally a getHashCodeForState is just the pair of state and precision.
    For PredicateAnalysis, we need to refine the RelevantPredicatesComputer (in some cases),
    thus the reduce-operator changes and we should clear the cache completely after such a refinement.
    Instead of doing this, we use the RelevantPredicatesComputer as (third) part of the cache-key,
    such that after refining the RelevantPredicatesComputer, each new access results in a cache-miss.
    The benefit is that we keep existing ARGs for statistics or other usage.
    The drawback is the memory-usage through some cached states,
    that will never be visited again, because the reduce-operator has changed.
    */
    // TODO is Object-equality for RelevantPredicatesComputer enough or should we implement RelevantPredicatesComputer.equals()?

    return Triple.of(element.getAbstractionFormula().asRegion(), precision, cpa.getRelevantPredicatesComputer());
  }

  @Override
  public Precision getVariableExpandedPrecision(Precision pRootPrecision, Block pRootContext,
      Precision pReducedPrecision) {

    if (reducePrecision) {
      PredicatePrecision rootPrecision = (PredicatePrecision) pRootPrecision;
      PredicatePrecision reducedPrecision = (PredicatePrecision) pReducedPrecision;

      if (rootPrecision instanceof ReducedPredicatePrecision) {
        rootPrecision = ((ReducedPredicatePrecision) rootPrecision).getRootPredicatePrecision();
      }
      if (reducedPrecision instanceof ReducedPredicatePrecision) {
        reducedPrecision = ((ReducedPredicatePrecision) reducedPrecision).getRootPredicatePrecision();
      }

      if (rootPrecision == reducedPrecision) { return pRootPrecision; }

      PredicatePrecision mergedPrecision = rootPrecision.mergeWith(reducedPrecision);

      return getVariableReducedPrecision(mergedPrecision, pRootContext);
    } else {
      return pReducedPrecision;
    }
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision, Block context) {
    PredicatePrecision expandedPredicatePrecision = (PredicatePrecision) pPrecision;

    if (reducePrecision) {
      assert expandedPredicatePrecision.getLocationInstancePredicates().isEmpty() :
        "TODO: need to handle location-instance-specific predicates in ReducedPredicatePrecision";
      /* LocationInstancePredicates is useless, because a block can be visited
       * several times along a error path and the index would always start from 0 again.
       * Thus we ignore LocationInstancePredicates and hope nobody is using them.
       * TODO can we assure this?
       */

      // create reduced precision

      // we only need global predicates with used variables
      final ImmutableSet<AbstractionPredicate> globalPredicates = ImmutableSet.copyOf(cpa.getRelevantPredicatesComputer().getRelevantPredicates(
          context, expandedPredicatePrecision.getGlobalPredicates()));

      // we only need function predicates with used variables
      final ImmutableSetMultimap.Builder<String, AbstractionPredicate> functionPredicatesBuilder = ImmutableSetMultimap.builder();
      for (String functionname : expandedPredicatePrecision.getFunctionPredicates().keySet()) {
        // TODO only add vars if functionname is used in block?
        functionPredicatesBuilder.putAll(functionname, cpa.getRelevantPredicatesComputer().getRelevantPredicates(
            context, expandedPredicatePrecision.getFunctionPredicates().get(functionname)));
      }
      final ImmutableSetMultimap<String, AbstractionPredicate> functionPredicates = functionPredicatesBuilder.build();

      // we only need local predicates with used variables and with nodes from the block
      final ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> localPredicatesBuilder = ImmutableSetMultimap.builder();
      for (CFANode node : expandedPredicatePrecision.getLocalPredicates().keySet()) {
        if (context.getNodes().contains(node)) {
          // TODO handle location-instance-specific predicates
          // Without support for them, we can just pass 0 as locInstance parameter
          localPredicatesBuilder.putAll(node, cpa.getRelevantPredicatesComputer().getRelevantPredicates(
              context, expandedPredicatePrecision.getPredicates(node, 0)));
        }
      }
      final ImmutableSetMultimap<CFANode, AbstractionPredicate> localPredicates = localPredicatesBuilder.build();

      PredicatePrecision rootPredicatePrecision = expandedPredicatePrecision;
      if (expandedPredicatePrecision instanceof ReducedPredicatePrecision) {
        rootPredicatePrecision = ((ReducedPredicatePrecision)expandedPredicatePrecision).getRootPredicatePrecision();
      }

      return new ReducedPredicatePrecision(
          rootPredicatePrecision,
          ImmutableSetMultimap.of(),
          localPredicates,
          functionPredicates,
          globalPredicates);
    } else {
      return pPrecision;
    }
  }

  static class ReducedPredicatePrecision extends PredicatePrecision {

    /* the top-level-precision of the main-block */
    private final PredicatePrecision rootPredicatePrecision;

    private ReducedPredicatePrecision(
        PredicatePrecision pRootPredicatePrecision,
        ImmutableSetMultimap<LocationInstance, AbstractionPredicate> pLocalInstPredicates,
        ImmutableSetMultimap<CFANode, AbstractionPredicate> pLocalPredicates,
        ImmutableSetMultimap<String, AbstractionPredicate> pFunctionPredicates,
        ImmutableSet<AbstractionPredicate> pGlobalPredicates) {
      super(pLocalInstPredicates, pLocalPredicates, pFunctionPredicates, pGlobalPredicates);
      assert !(pRootPredicatePrecision instanceof ReducedPredicatePrecision);
      this.rootPredicatePrecision = pRootPredicatePrecision;
    }

    PredicatePrecision getRootPredicatePrecision() {
      return rootPredicatePrecision;
    }

    @Override
    public boolean equals(Object pObj) {
      return super.equals(pObj)
          && pObj instanceof ReducedPredicatePrecision
          && rootPredicatePrecision.equals(((ReducedPredicatePrecision)pObj).getRootPredicatePrecision());
    }

    @Override
    public int hashCode() {
      return super.hashCode() + 17 * rootPredicatePrecision.hashCode();
    }
  }

  @Override
  public int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    PredicatePrecision precision = (PredicatePrecision) pPrecision;
    PredicatePrecision otherPrecision = (PredicatePrecision) pOtherPrecision;

    return precision.calculateDifferenceTo(otherPrecision);
  }

  @Override
  public AbstractState getVariableReducedStateForProofChecking(AbstractState pExpandedState, Block pContext,
      CFANode pCallNode) {
    return pExpandedState;
  }

  @Override
  public AbstractState getVariableExpandedStateForProofChecking(AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) throws InterruptedException {

    PredicateAbstractState rootState = (PredicateAbstractState) pRootState;
    PredicateAbstractState reducedState = (PredicateAbstractState) pReducedState;

    if (!reducedState.isAbstractionState()) { return reducedState; }

    AbstractionFormula rootAbstraction = rootState.getAbstractionFormula();
    AbstractionFormula reducedAbstraction = reducedState.getAbstractionFormula();

    // De-serialized AbstractionFormula are missing the Regions which we need for expand(),
    // so we re-create them here.
    rootAbstraction =
        pamgr.asAbstraction(rootAbstraction.asFormula(), rootAbstraction.getBlockFormula());
    reducedAbstraction =
        pamgr.asAbstraction(reducedAbstraction.asFormula(), reducedAbstraction.getBlockFormula());

    Collection<AbstractionPredicate> rootPredicates = pamgr.getPredicatesForAtomsOf(rootAbstraction.asInstantiatedFormula());
    Collection<AbstractionPredicate> relevantRootPredicates =
        cpa.getRelevantPredicatesComputer().getRelevantPredicates(pReducedContext, rootPredicates);
    //for each removed predicate, we have to lookup the old (expanded) value and insert it to the reducedStates region

    PathFormula oldPathFormula = reducedState.getPathFormula();
    SSAMap oldSSA = oldPathFormula.getSsa();

    //pathFormula.getSSa() might not contain index for the newly added variables in predicates; while the actual index is not really important at this point,
    //there still should be at least _some_ index for each variable of the abstraction formula.
    SSAMapBuilder builder = oldSSA.builder();
    SSAMap rootSSA = rootState.getPathFormula().getSsa();
    for (String var : rootSSA.allVariables()) {
      //if we do not have the index in the reduced map..
      if (!oldSSA.containsVariable(var)) {
        //add an index (with the value of rootSSA)
        builder.setIndex(var, rootSSA.getType(var), rootSSA.getIndex(var));
      }
    }
    SSAMap newSSA = builder.build();
    PathFormula newPathFormula = pmgr.makeNewPathFormula(pmgr.makeEmptyPathFormula(), newSSA);

    AbstractionFormula newAbstractionFormula =
        pamgr.expand(reducedAbstraction.asRegion(), rootAbstraction.asRegion(),
            relevantRootPredicates, newSSA, reducedAbstraction.getBlockFormula());

    PersistentMap<CFANode, Integer> abstractionLocations = rootState.getAbstractionLocationsOnPath();

    return PredicateAbstractState.mkAbstractionState(newPathFormula,
        newAbstractionFormula, abstractionLocations);
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(AbstractState pRootState, AbstractState pEntryState,
                                                     AbstractState pExpandedState,
                                                     FunctionExitNode exitLocation) {
    final PredicateAbstractState rootState = (PredicateAbstractState) pRootState;
    final PredicateAbstractState entryState = (PredicateAbstractState) pEntryState;
    final PredicateAbstractState expandedState = (PredicateAbstractState) pExpandedState;
    Preconditions.checkState(rootState.isAbstractionState());
    Preconditions.checkState(entryState.isAbstractionState());
    Preconditions.checkState(expandedState.isAbstractionState());

    final PersistentMap<CFANode, Integer> abstractionLocations = expandedState.getAbstractionLocationsOnPath();

    // we have:
    // - abstraction of rootState with ssa                --> use as it is
    // - callEdge-pathFormula with ssa (from rootState)   --> use as it is, with updated SSAMap
    // - abstraction of functioncall (expandedSSA)        --> instantiate, with updated SSAMap, so that:
    //           - only param and return-var overlap to callEdge
    //           - all other vars are distinct
    final String calledFunction = exitLocation.getFunctionName();
    final PathFormula functionCall = entryState.getAbstractionFormula().getBlockFormula();
    final SSAMap entrySsaWithRet = functionCall.getSsa();
    final SSAMapBuilder entrySsaWithRetBuilder = entrySsaWithRet.builder();
    final SSAMapBuilder summSsa = rootState.getAbstractionFormula().getBlockFormula().getSsa().builder();

    final SSAMap expandedSSA = expandedState.getAbstractionFormula().getBlockFormula().getSsa();
    for (String var : expandedSSA.allVariables()) {
      final CType type = expandedSSA.getType(var);
      if (var.startsWith(calledFunction + "::")
              && var.endsWith(PARAM_VARIABLE_NAME)) {
        int newIndex = entrySsaWithRet.getIndex(var);
        assert entrySsaWithRet.containsVariable(var) : "param for function is not used in functioncall";
        entrySsaWithRetBuilder.setIndex(var, type, newIndex);
        setFreshValueBasis(summSsa, var, newIndex);

      } else if (exitLocation.getEntryNode().getReturnVariable().isPresent() &&
          exitLocation.getEntryNode().getReturnVariable().get().getQualifiedName().equals(var)) {
        // var.startsWith(calledFunction + "::") && var.endsWith(RETURN_VARIABLE_NAME)
        final int newIndex = Math.max(expandedSSA.getIndex(var), entrySsaWithRetBuilder.getFreshIndex(var));
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

    // function-call needs have new retvars-indices.
    PathFormula functionCallWithSSA = new PathFormula(functionCall.getFormula(), newEntrySsaWithRet,
            functionCall.getPointerTargetSet(), functionCall.getLength());

    // concat function-call with function-summary,
    // function-summary will be instantiated with indices for params and retvars.
    PathFormula executedFunction = pmgr.makeAnd(functionCallWithSSA,
            expandedState.getAbstractionFormula().asFormula());

    // after function-execution we have to re-use the previous indices (fromouter scope),
    // thus lets change the SSAmap.
    PathFormula executedFunctionWithSSA = new PathFormula(executedFunction.getFormula(), newSummSsa,
            executedFunction.getPointerTargetSet(), executedFunction.getLength());

    // everything is prepared, so build a new AbstractionState.
    // we do this as 'future abstraction', because we do not have enough information
    // (necessary classes and managers) for the abstraction-process at this place.
    PredicateAbstractState rebuildState =
        PredicateAbstractState.mkNonAbstractionState(
            executedFunctionWithSSA, rootState.getAbstractionFormula(), abstractionLocations);

    logger.log(Level.ALL,
            "\noldAbs: ", rootState.getAbstractionFormula().asInstantiatedFormula(),
            "\ncall: ", functionCallWithSSA,
            "\nsumm: ", expandedState.getAbstractionFormula().asFormula(),
            "\nexe: ", executedFunction,
            "\nentrySsaRet", newEntrySsaWithRet,
            "\nsummSsaRet", newSummSsa
    );

    return rebuildState;
  }

  /**
   * rootSSA might not contain correct indices for the local variables of calling function-scope.
   * so lets build a new SSA from:
   * - local variables from rootSSA,                  -> update indices (their indices will have "holes")
   * - local variables from expandedSSA,              -> ignore indices (their indices are the "holes")
   * - global variables from expandedSSA,             -> update indices (we have to keep them)
   * - the local return variables from expandedState. -> update indices (we have to keep them,
   *       there can be several ret-vars from distinct functions, ignore them, they are created new, if needed)
   * we copy expandedState and override all local values.
   *
   * @param rootSSA SSA before function-call
   * @param expandedSSA SSA before function-return
   * @param functionExitNode the function-return-location
   * @return new SSAMap
   */
  static SSAMap updateIndices(final SSAMap rootSSA, final SSAMap expandedSSA,
      FunctionExitNode functionExitNode) {

    final SSAMapBuilder rootBuilder = rootSSA.builder();

    for (String var : expandedSSA.allVariables()) {

      // Depending on the scope of vars, set either only the lastUsedIndex or the default index.
      // var was used and maybe overridden inside the block
      final CType type = expandedSSA.getType(var);
      if (var.contains("::") && !isReturnVar(var, functionExitNode)) { // var is scoped -> not global

        if (!rootSSA.containsVariable(var)) {

          // Inner local variable, never seen before,
          // use fresh index as a basis for further assignments
          rootBuilder.setIndex(var, type, expandedSSA.builder().getFreshIndex(var));

        } else {

          // Outer variable or inner variable from previous function call
          setFreshValueBasis(rootBuilder, var,
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
   * Set a new index (7) for an old index (3),
   * so that getIndex() returns the old index (3) and getFreshIndex() returns a higher index (8).
   * Warning: do not use out of order!
   */
  private static void setFreshValueBasis(SSAMapBuilder ssa, String name, int idx) {
    Preconditions.checkArgument(idx > 0, "Indices need to be positive for this SSAMap implementation:", name, idx);
    int oldIdx = ssa.getIndex(name);
    Preconditions.checkArgument(idx >= oldIdx, "SSAMap updates need to be strictly monotone:", name, idx, "vs", oldIdx);

    if (idx > oldIdx) {
      FreshValueProvider bamfvp = new FreshValueProvider();
      bamfvp.put(name, idx);
      ssa.mergeFreshValueProviderWith(bamfvp);
    }
  }
}
