// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.templates.Template;
import org.sosy_lab.cpachecker.util.templates.Template.Kind;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

/** BAM reduction for LPI. */
class PolicyReducer implements Reducer {

  private final PolicyIterationManager policyIterationManager;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;
  private final StateFormulaConversionManager stateFormulaConversionManager;
  private final PathFormulaManager pfmgr;

  private static final int STARTING_SSA_IDX = 1;

  PolicyReducer(
      PolicyIterationManager pPolicyIterationManager,
      FormulaManagerView pFormulaManager,
      StateFormulaConversionManager pStateFormulaConversionManager,
      PathFormulaManager pPathFormulaManager) {
    policyIterationManager = pPolicyIterationManager;
    fmgr = pFormulaManager;
    stateFormulaConversionManager = pStateFormulaConversionManager;
    pfmgr = pPathFormulaManager;
    bfmgr = fmgr.getBooleanFormulaManager();
  }

  /**
   * Remove all information from the {@code expandedState} which is not relevant to {@code context}.
   */
  @Override
  public PolicyAbstractedState getVariableReducedState(
      AbstractState expandedState, Block context, CFANode callNode) {
    PolicyState pState = (PolicyState) expandedState;
    Preconditions.checkState(pState.isAbstract());
    PolicyAbstractedState aState = pState.asAbstracted();
    Set<String> blockVars = context.getVariables();

    Map<Template, PolicyBound> newAbstraction =
        Maps.filterEntries(
            Maps.filterKeys(
                aState.getAbstraction(),
                // Remove templates containing non-referenced variables.
                key -> blockVars.containsAll(key.getUsedVars())),
            // Remove templates setting specific upper bound which would enforce an equality:
            // we are performing explicit weakenings in order to generate more generic summarise.
            e -> !isUpperBoundOnEquality(e.getKey(), e.getValue(), aState.getAbstraction()));

    return PolicyAbstractedState.of(
        newAbstraction,
        aState.getNode(),
        policyIterationManager.getFreshLocationID(),
        stateFormulaConversionManager,
        SSAMap.emptySSAMap().withDefault(STARTING_SSA_IDX),
        aState.getPointerTargetSet(), // todo: might have to change pointer target set.
        bfmgr.makeTrue(),

        // During the summary generation we should not use elements from the previous recursion
        // level.
        Optional.empty(),
        Optional.empty());
  }

  /**
   * Return whether the {@code t} is bound to a single value (from above and below) in {@code
   * abstraction}, and current template is an upper bound.
   */
  private boolean isUpperBoundOnEquality(
      Template t, PolicyBound bound, Map<Template, PolicyBound> abstraction) {

    if (t.getKind() != Kind.UPPER_BOUND) {
      return false;
    }
    Template negated = Template.of(t.getLinearExpression().negate());
    PolicyBound negatedValue = abstraction.get(negated);
    return negatedValue != null && negatedValue.getBound().equals(bound.getBound());
  }

  @Override
  public PolicyState getVariableExpandedState(
      AbstractState entryState, Block reducedContext, AbstractState summaryState) {
    PolicyState pEntryState = (PolicyState) entryState;
    PolicyState pReturnState = (PolicyState) summaryState;

    if (!pReturnState.isAbstract()) {
      // We get an intermediate state for expansion if and only if
      // it is a target state.
      return pReturnState;
    }
    Preconditions.checkState(pEntryState.isAbstract());
    Preconditions.checkState(pReturnState.isAbstract());

    PolicyAbstractedState aEntryState = pEntryState.asAbstracted();
    PolicyAbstractedState aSummaryState = pReturnState.asAbstracted();

    ImmutableMap<Template, PolicyBound> summaryAbstraction = aSummaryState.getAbstraction();
    SSAMap summarySSA = aSummaryState.getSSA();
    CFANode node = aSummaryState.getNode();

    SSAMapBuilder builder = aEntryState.getSSA().builder();

    // Increment the SSA index for all variables modified in the summary
    // (we conveniently have calculated them already)
    // leave others the same.
    for (String var : builder.allVariables()) {
      if (aEntryState.getSSA().getIndex(var) > STARTING_SSA_IDX) {
        builder = builder.setIndex(var, builder.getType(var), builder.getFreshIndex(var));
      }
    }
    SSAMap ssa = builder.build();

    // Updated during precision adjustment.
    int locationID = aSummaryState.getLocationID();
    Optional<PolicyAbstractedState> sibling = Optional.empty();

    // TODO: update pointer target set.
    PointerTargetSet pointerTargetSet = aEntryState.getPointerTargetSet();

    BooleanFormula formula =
        bfmgr.and(
            stateFormulaConversionManager.abstractStateToConstraints(fmgr, aSummaryState, false));

    @SuppressWarnings("deprecation")
    // TODO Can we refactor and move part of the code into an appropriate high-level method in
    // PathFormulaManager?
    PathFormula pf = PathFormula.createManually(formula, ssa, pointerTargetSet, 1);

    // Fake that the control has come from the entry state.
    PolicyIntermediateState generator = PolicyIntermediateState.of(node, pf, aEntryState);

    Map<Template, PolicyBound> newAbstraction =
        updateAbstractionForExpanded(pf, aEntryState, summaryAbstraction, summarySSA);

    return PolicyAbstractedState.of(
        newAbstraction,
        node,
        locationID,
        stateFormulaConversionManager,
        ssa,
        pointerTargetSet,
        bfmgr.makeTrue(),
        Optional.of(generator),
        sibling);
  }

  /**
   * Update the meta-information for policies coming from the summary edge.
   *
   * @param inputPath Path formula, which contains {@link SSAMap} equivalent to the summary
   *     application, and the summary encoded as a formula.
   * @param pParent Previous abstract state, associated with the function call before the function
   *     application.
   * @param summaryAbstraction Abstraction associated with the summary state.
   * @param summarySSA {@link SSAMap} associated with the summary.
   */
  private Map<Template, PolicyBound> updateAbstractionForExpanded(
      PathFormula inputPath,
      PolicyAbstractedState pParent,
      Map<Template, PolicyBound> summaryAbstraction,
      SSAMap summarySSA) {

    ImmutableMap.Builder<Template, PolicyBound> newAbstraction = ImmutableMap.builder();
    Set<Template> allTemplates =
        Sets.union(pParent.getAbstraction().keySet(), summaryAbstraction.keySet());
    for (Template template : allTemplates) {
      PolicyBound pBound = summaryAbstraction.get(template);
      PolicyBound insertedBound = null;
      if (pBound != null) {

        // If bound for this template is present in the summary, add it after the abstraction.
        BooleanFormula policyFormula =
            stateFormulaConversionManager.templateToConstraint(
                template, pBound, pfmgr, fmgr, inputPath);
        PathFormula policy = inputPath.withFormula(policyFormula);
        insertedBound =
            PolicyBound.of(
                policy,
                pBound.getBound(),
                pParent,

                // TODO: filter the set of dependent templates, at least
                pParent.getAbstraction().keySet());
      } else if (template.getUsedVars().stream()
          .noneMatch(v -> summarySSA.getIndex(v) > STARTING_SSA_IDX)) {

        // Otherwise, use the bound from the parent state.
        insertedBound = pParent.getBound(template).orElseThrow();
      }

      if (insertedBound != null) {
        newAbstraction.put(template, insertedBound);
      }
    }

    return newAbstraction.buildOrThrow();
  }

  @Override
  public Precision getVariableReducedPrecision(Precision precision, Block context) {
    // Currently, precision is a singleton.
    return precision;
  }

  @Override
  public Precision getVariableExpandedPrecision(
      Precision rootPrecision, Block rootContext, Precision reducedPrecision) {
    // Currently, precision is a singleton.
    return rootPrecision;
  }

  @Override
  public Object getHashCodeForState(AbstractState stateKey, Precision precisionKey) {
    PolicyState pState = (PolicyState) stateKey;

    // Discard all the meta-information attached to the bounds.
    return ImmutableMap.copyOf(
        Maps.transformValues(pState.asAbstracted().getAbstraction(), PolicyBound::getBound));
  }

  /**
   * Take root state, remove all bounds associated with global variables, add all globals from the
   * expandedState, add assignment to return function value from expandedState.
   */
  @Override
  public PolicyAbstractedState rebuildStateAfterFunctionCall(
      AbstractState rootState,
      AbstractState entryState,
      AbstractState expandedState,
      FunctionExitNode exitLocation) {
    PolicyState pRootState = (PolicyState) rootState;
    PolicyState pExpandedState = (PolicyState) expandedState;
    PolicyState pEntryState = (PolicyState) entryState;
    Preconditions.checkState(pRootState.isAbstract());
    Preconditions.checkState(pEntryState.isAbstract());
    Preconditions.checkState(pExpandedState.isAbstract());

    throw new UnsupportedOperationException("Recursion is not supported by LPI+BAM yet.");
  }
}
