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
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
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

    config.inject(this);

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
  public PredicatePrecision extractPrecisionFromCfa(boolean atomicPredicates) throws CPATransferException {
    logger.log(Level.FINER, "Extracting precision from CFA...");

    Multimap<String, AbstractionPredicate> functionPredicates = ArrayListMultimap.create();
    Collection<AbstractionPredicate> globalPredicates         = Lists.newArrayList();

    Set<AssumeEdge> assumeEdges = new HashSet<>(getTargetLocationAssumes().values());

    for (AssumeEdge assume : assumeEdges) {
      BooleanFormula relevantAssumesFormula = pathFormulaManager.makeAnd(
          pathFormulaManager.makeEmptyPathFormula(),
          assume).getFormula();

      Collection<AbstractionPredicate> preds;
      if (atomicPredicates) {
        preds = predAbsManager.extractPredicates(relevantAssumesFormula);
      } else {
        preds = ImmutableList.of(predAbsManager.createPredicateFor(
            formulaManagerView.uninstantiate(relevantAssumesFormula)));
      }

      String function = assume.getPredecessor().getFunctionName();

      boolean applyGlobal = true;
      if (applyScoped) {
        for (String var : getQualifiedVariablesOfAssume(assume)) {
          logger.log(Level.FINER, "Checking scope of", function, var);
          if (isDeclaredInFunction(function, var)) {
            // Apply the predicate in function scope
            // as soon one of the variable the assumption talks about is local.
            applyGlobal = false;
            logger.log(Level.FINEST, "Local scoped variable mined", function, var);
            break;
          }
        }

        if (!applyGlobal) {
          functionPredicates.putAll(function, preds);
        }
      }

      if (applyGlobal) {
        logger.log(Level.FINEST, "Global predicates mined", preds);
        globalPredicates.addAll(preds);
      }

      logger.log(Level.FINER, "Extraction result", "Function:", function, "Predicates:", preds);
    }

    logger.log(Level.FINER, "Extracting finished.");

    return new PredicatePrecision(
        ImmutableSetMultimap.<Pair<CFANode,Integer>,
        AbstractionPredicate>of(),
        ArrayListMultimap.<CFANode, AbstractionPredicate>create(),
        functionPredicates,
        globalPredicates);
  }

  @Override
  @Deprecated
  public Precision extractPrecisionFromCfa() throws CPATransferException {
    throw new UnsupportedOperationException();
  }
}