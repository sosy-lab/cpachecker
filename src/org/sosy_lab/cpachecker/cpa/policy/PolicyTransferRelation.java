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
package org.sosy_lab.cpachecker.cpa.policy;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.*;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.rationals.ExtendedRational;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;
import org.sosy_lab.cpachecker.util.rationals.LinearConstraint;

import java.util.*;

/**
 * Transfer relation for policy iteration.
 *
 * NOTE to self: do not do any dependancy analysis yet.
 */
public class PolicyTransferRelation implements TransferRelation {

  private final PathFormulaManager pathFormulaManager;
  private final FormulaManagerFactory formulaManagerFactory;
  private final BooleanFormulaManagerView bfmgr;
  private final NumeralFormulaManager<NumeralFormula, NumeralFormula.RationalFormula> rfmgr;

  private final LinearConstraintManager lcmgr;

  /**
   * Class for dealing with fresh variables.
   */
  private static class FreshVariable {
    public static final String FRESH_VAR_PREFIX = "POLICY_ITERATION_FRESH_VAR_%d";

    // NOTE: hm what if it overflows?
    private static long fresh_var_counter = 0;

    final long no;
    final NumeralFormula variable;

    private FreshVariable(long no, NumeralFormula variable) {
      this.no = no;
      this.variable = variable;
    }

    String name() {
      return name(no);
    }

    static String name(long counter) {
      return String.format(FRESH_VAR_PREFIX, counter);
    }

    /**
     * @return Unique fresh variable created using a global counter.
     */
    private static FreshVariable createFreshVar(
        NumeralFormulaManager<NumeralFormula, NumeralFormula.RationalFormula> rfmgr
    ) {
      FreshVariable out = new FreshVariable(
          fresh_var_counter,
          rfmgr.makeVariable(FreshVariable.name(fresh_var_counter))
      );
      fresh_var_counter++;
      return out;
    }
  }


  public PolicyTransferRelation(
          Configuration config,
          FormulaManagerView formulaManager,
          FormulaManagerFactory formulaManagerFactory,
          PathFormulaManager pathFormulaManager)
      throws InvalidConfigurationException {

    // NOTE: apparently after this line we can start adding new options
    // to the class and they will be honoured by the runner.
    config.inject(this, PolicyTransferRelation.class);

    this.pathFormulaManager = pathFormulaManager;
    this.formulaManagerFactory = formulaManagerFactory;

    rfmgr = formulaManager.getRationalFormulaManager();
    bfmgr = formulaManager.getBooleanFormulaManager();

    lcmgr = new LinearConstraintManager(formulaManager);
  }

  // (abstract_state, edge) -> list abstract_state
  @Override
  public Collection<? extends AbstractState>
      getAbstractSuccessors(AbstractState pState, Precision precision, CFAEdge cfaEdge)
         throws CPATransferException, InterruptedException {

    PolicyAbstractState prevState = (PolicyAbstractState) pState;

    CFANode toNode = cfaEdge.getSuccessor();

    // Formula representing the edge.
    PathFormula pathFormula = pathFormulaManager.makeFormulaForPath(
        Collections.singletonList(cfaEdge));

    Set<LinearExpression> fromTemplates = prevState.getTemplates();

    // NOTE: we can do it much faster if we use a different datastructure to hash sets.
    // e.g. balanced binary tree.
    Set<LinearExpression> toTemplates = new HashSet<>();
    toTemplates.addAll(fromTemplates);

    if (cfaEdge.getEdgeType().equals(CFAEdgeType.DeclarationEdge)) {
      ADeclarationEdge declarationEdgeedge = (ADeclarationEdge) cfaEdge;
      String varName = declarationEdgeedge.getDeclaration().getName();

      // NOTE:
      // I am honestly not sure that this is a best way to propagate templates.
      // Though hey, I guess it is better than pre-processing.
      toTemplates.add(LinearExpression.ofVariable(varName));
      toTemplates.add(LinearExpression.ofVariable(varName).negate());
    }

    // Well okay so here is where it is gets interesting.
    // Do I have to interact through the [Solver] object?
    // Or should I just manually create the [OptEnvironment  as I see fit?]

    ImmutableMap.Builder<LinearExpression, PolicyTemplateBound>
        newStateData = ImmutableMap.builder();

    try (OptEnvironment solver = formulaManagerFactory.newOptEnvironment()) {

      // Constraints imposed by the previous state.
      solver.addConstraint(stateToFormula(prevState));

      // Constraints imposed by the edge.
      solver.addConstraint(pathFormula.getFormula());

      for (LinearExpression template : toTemplates) {

        ExtendedRational value = maximize(solver, template, pathFormula.getSsa());
        PolicyTemplateBound constraint = PolicyTemplateBound.of(
            cfaEdge,
            value
        );

        newStateData.put(template, constraint);
      }
    } catch (Exception e) {
      throw new CPATransferException("Failed the policy iteration step", e);
    }

    PolicyAbstractState newState = PolicyAbstractState.withState(
        newStateData.build(),
        toNode
    );

    return Collections.singleton(newState);
  }

  /**
   * @param prover Prover engine used
   * @param expression Expression to maximize
   * @return Returned value in the extended rational field.
   * @throws CPATransferException
   */
  private ExtendedRational maximize(
      OptEnvironment prover, LinearExpression expression, SSAMap pSSAMap
      ) throws CPATransferException, InterruptedException {

    // We can only maximize a single variable.
    // Create a new variable, make it equal to the template which we have.
    FreshVariable target = FreshVariable.createFreshVar(rfmgr);
    BooleanFormula constraint =
        rfmgr.equal(
            target.variable,
            lcmgr.linearExpressionToFormula(expression, SSAMap.emptySSAMap())
        );
    prover.addConstraint(constraint);
    prover.setObjective(target.variable);

    try {
      OptEnvironment.OptResult result = prover.maximize();

      switch (result) {
        case OPT:
          Model model = prover.getModel();
          return (ExtendedRational) model.get(
              new Model.Constant(target.name(), Model.TermType.Real)
          );
        case UNSAT:
          return ExtendedRational.NEG_INFTY;
        case UNBOUNDED:
          return ExtendedRational.INFTY;
        case UNDEF:
          throw new CPATransferException("Result undefiend: something is wrong");
        default:
          throw new RuntimeException("Internal Error, unaccounted case");
      }
    } catch (SolverException e) {
      throw new CPATransferException("Failed getting model", e);
    }
  }

  private BooleanFormula stateToFormula(PolicyAbstractState state) {
    List<BooleanFormula> constraints = new LinkedList<>();
    SSAMap freshMap = SSAMap.emptySSAMap();

    for (Map.Entry<LinearExpression, PolicyTemplateBound> item : state) {
      LinearExpression expr = item.getKey();

      LinearConstraint constraint = new LinearConstraint(expr, item.getValue().bound);
      constraints.add(lcmgr.linearConstraintToFormula(constraint, freshMap));
    }

    return bfmgr.and(constraints);
  }

  public Collection<? extends AbstractState> strengthen(
          AbstractState state,
          List<AbstractState> otherStates,
          CFAEdge cfaEdge,
          Precision precision) throws CPATransferException, InterruptedException {

    // AFAIR strengthening is used for communicating the analysis details between various CPAs.
    return null;
  }
}
