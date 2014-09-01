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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.rationals.ExtendedRational;
import org.sosy_lab.cpachecker.util.rationals.LinearConstraint;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Transfer relation for policy iteration.
 */
@Options(prefix="cpa.policy")
public class PolicyTransferRelation implements TransferRelation {

  private final PathFormulaManager pfmgr;
  private final FormulaManagerFactory formulaManagerFactory;
  private final BooleanFormulaManagerView bfmgr;
  private final NumeralFormulaManager<NumeralFormula,
      NumeralFormula.RationalFormula> rfmgr;

  private final LinearConstraintManager lcmgr;
  private final LogManager logger;
  private final PolicyAbstractDomain abstractDomain;

  public PolicyTransferRelation(
          Configuration config,
          FormulaManagerView formulaManager,
          FormulaManagerFactory formulaManagerFactory,
          PathFormulaManager pfmgr,
          LogManager logger,
          PolicyAbstractDomain abstractDomain,
          LinearConstraintManager lcmgr)

      throws InvalidConfigurationException {

    config.inject(this, PolicyTransferRelation.class);

    this.pfmgr = pfmgr;
    this.formulaManagerFactory = formulaManagerFactory;

    rfmgr = formulaManager.getRationalFormulaManager();
    bfmgr = formulaManager.getBooleanFormulaManager();

    this.lcmgr = lcmgr;

    this.logger = logger;
    this.abstractDomain = abstractDomain;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pState,
      Precision precision,
      CFAEdge edge
      ) throws CPATransferException, InterruptedException {

    PolicyAbstractState prevState = (PolicyAbstractState) pState;

    logger.log(Level.FINE, ">>> Processing statement: " + edge.getCode()
     + " for to-node: " + edge.getSuccessor());

    CFANode toNode = edge.getSuccessor();

    // Formula representing the edge.
    PathFormula edgeFormula = pfmgr.makeFormulaForPath(
        Collections.singletonList(edge));

    ImmutableSet<LinearExpression> fromTemplates = prevState.getTemplates();

    // NOTE: we can do it much faster if we use a different datastructure to hash sets.
    // e.g. balanced binary tree.
    Set<LinearExpression> toTemplates = new HashSet<>();
    toTemplates.addAll(fromTemplates);

    /** Propagating templates */
    if (edge.getEdgeType().equals(CFAEdgeType.DeclarationEdge)) {

      ADeclarationEdge declarationEdge = (ADeclarationEdge) edge;

      // Do not process declarations for functions.
      if (!isFuncDeclaration(declarationEdge)) {
        String varName = declarationEdge.getDeclaration().getQualifiedName();

        // TODO: abstract template propagation to a separate class.

        // NOTE: A better way to propagate templates?
        // NOTE: Let's also check for liveness! [other property?
        // CPA communication FTW!!].
        // If the variable is no longer alive at a certain location
        // there is no point in tracking it (deeper analysis -> dependencies).
        toTemplates.add(LinearExpression.ofVariable(varName));

        // TODO: re-enable, disabled for ease of debugging.
//        toTemplates.add(LinearExpression.ofVariable(varName).negate());
      }
    }


    /** Propagate the invariants */
    ImmutableMap.Builder<LinearExpression, PolicyTemplateBound> newStateData;

    newStateData = ImmutableMap.builder();

    try (OptEnvironment solver = formulaManagerFactory.newOptEnvironment()) {

      // Constraints imposed by the previous state.
      solver.addConstraint(stateToFormula(prevState,
          SSAMap.emptySSAMap().withDefault(1)));

      // Constraints imposed by the edge.
      solver.addConstraint(edgeFormula.getFormula());

      for (LinearExpression template : toTemplates) {

        ExtendedRational value = lcmgr.maximize(
            solver, template, edgeFormula.getSsa());
        PolicyTemplateBound constraint = PolicyTemplateBound.of(edge, value);

        // If the state is not reachable, bail early.
        if (value == ExtendedRational.NEG_INFTY) {
          logger.log(Level.FINE, "Stopping, unfeasible branch.");
          return Collections.emptyList();
        }

        newStateData.put(template, constraint);
      }
    } catch (Exception e) {
      throw new CPATransferException("Failed solving", e);
    }

    /** Update the global policy object. */
    logger.log(Level.FINER,
        "For node = " + toNode + " setting policy = " + edge);
    for (LinearExpression template : toTemplates) {
      abstractDomain.setPolicyForTemplate(toNode, template, edge);
    }

    PolicyAbstractState newState = PolicyAbstractState.withState(
        newStateData.build(),
        toNode
    );

    return Collections.singleton(newState);
  }

  private BooleanFormula stateToFormula(PolicyAbstractState state, SSAMap ssaMap) {
    List<BooleanFormula> constraints = new LinkedList<>();

    for (Map.Entry<LinearExpression, PolicyTemplateBound> item : state) {
      LinearExpression expr = item.getKey();

      LinearConstraint constraint = new LinearConstraint(expr, item.getValue().bound);
      constraints.add(lcmgr.linearConstraintToFormula(constraint, ssaMap));
    }

    return bfmgr.and(constraints);
  }

  /**
   * Strengthening is used for communicating the analysis details between
   * various CPAs.
   */
  @Override
  public Collection<? extends AbstractState> strengthen(
          AbstractState state,
          List<AbstractState> otherStates,
          CFAEdge cfaEdge,
          Precision precision) throws CPATransferException, InterruptedException {

    return null;
  }

  /**
   * Test whether the declaration is a function.
   */
  private boolean isFuncDeclaration(ADeclarationEdge declarationEdge) {
    // NOTE: this is extremely hacky, but can't find a better way.
    Type t = declarationEdge.getDeclaration().getType();
    String type = t.toASTString(declarationEdge.getDeclaration().getName());
    return type.contains("(");
  }
}
