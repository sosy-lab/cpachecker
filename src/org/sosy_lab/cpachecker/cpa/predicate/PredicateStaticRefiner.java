/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.StaticRefiner;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

@Options(prefix="staticRefiner")
public class PredicateStaticRefiner extends StaticRefiner {

  @Option(description="Apply mined predicates on the corresponding scope. false = add them to the global precision.")
  private boolean applyScoped = true;

  private final PathFormulaManager pathFormulaManager;
  private final FormulaManagerView formulaManagerView;
  private final PredicateAbstractionManager predAbsManager;

  public PredicateStaticRefiner(
      Configuration pConfig,
      LogManager pLogger,
      PathFormulaManager pPathFormulaManager,
      FormulaManagerView pFormulaManagerView,
      PredicateAbstractionManager pPredAbsManager,
      CFA pCfa) throws InvalidConfigurationException {
    super(pConfig, pLogger, pCfa);

    pConfig.inject(this);

    this.pathFormulaManager = pPathFormulaManager;
    this.formulaManagerView = pFormulaManagerView;
    this.predAbsManager = pPredAbsManager;
  }

  /**
   * This method extracts a precision based only on static information derived from the CFA.
   *
   * @return a precision for the predicate CPA
   * @throws CPATransferException
   */
  public PredicatePrecision extractPrecisionFromCfa(List<ARGState> pPath, boolean atomicPredicates) throws CPATransferException {
    logger.log(Level.FINER, "Extracting precision from CFA...");

    // Predicates that should be tracked on function scope
    Multimap<String, AbstractionPredicate> functionPredicates = ArrayListMultimap.create();

    // Predicates that should be tracked globally
    Collection<AbstractionPredicate> globalPredicates = Lists.newArrayList();

    // Determine the ERROR location of the path (last node)
    CFANode targetLocation = AbstractStates.extractLocation(pPath.get(pPath.size()-1));

    // Determine the assume edges that should be considered for predicate extraction
    Set<AssumeEdge> assumeEdges = new HashSet<>(getTargetLocationAssumes(Lists.newArrayList(targetLocation)).values());

    // Create predicates for the assume edges and add them to the precision
    for (AssumeEdge assume : assumeEdges) {
      // Create a boolean formula from the assume
      BooleanFormula relevantAssumesFormula = pathFormulaManager.makeAnd(
          pathFormulaManager.makeEmptyPathFormula(), assume).getFormula();

      Collection<AbstractionPredicate> preds;
      if (atomicPredicates) {
        preds = predAbsManager.extractPredicates(relevantAssumesFormula);
      } else {
        preds = ImmutableList.of(predAbsManager.createPredicateFor(
            formulaManagerView.uninstantiate(relevantAssumesFormula)));
      }

      // Check whether the predicate should be used global or only local
      boolean applyGlobal = true;
      if (applyScoped) {
        for (CIdExpression idExpr : getVariablesOfAssume(assume)) {
          CSimpleDeclaration decl = idExpr.getDeclaration();
          if (decl instanceof CVariableDeclaration) {
            if (!((CVariableDeclaration) decl).isGlobal()) {
              applyGlobal = false;
            }
          } else if (decl instanceof CParameterDeclaration) {
            applyGlobal = false;
          }
        }
      }

      // Add the predicate to the resulting precision
      if (applyGlobal) {
        logger.log(Level.FINEST, "Global predicates mined", preds);
        globalPredicates.addAll(preds);
      } else {
        logger.log(Level.FINEST, "Function predicates mined", preds);
        String function = assume.getPredecessor().getFunctionName();
        functionPredicates.putAll(function, preds);
      }
    }

    logger.log(Level.FINER, "Extracting finished.");

    return new PredicatePrecision(
        ImmutableSetMultimap.<Pair<CFANode,Integer>,
        AbstractionPredicate>of(),
        ArrayListMultimap.<CFANode, AbstractionPredicate>create(),
        functionPredicates,
        globalPredicates);
  }

}