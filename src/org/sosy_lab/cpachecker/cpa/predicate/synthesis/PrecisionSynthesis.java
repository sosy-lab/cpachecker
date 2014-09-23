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
package org.sosy_lab.cpachecker.cpa.predicate.synthesis;

import java.util.Collection;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.ComputeAbstractionState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;


@Options
public class PrecisionSynthesis extends AbstractPrecisionSynthesis {

  private final RelationSynthesis relsynth;

  public PrecisionSynthesis(Configuration pConfig, LogManager pLogger, FormulaManagerView pFmgr,
      Optional<VariableClassification> pVariableClassification, FormulaManager pRawFmgr, AbstractionManager pAmgr,
      MachineModel pMachineModel, ShutdownNotifier pShutdownNotifier, CFA pCfa, RelationStore pRelStore) throws InvalidConfigurationException {
    super(pConfig, pLogger, pFmgr, pVariableClassification, pRawFmgr, pAmgr, pMachineModel, pShutdownNotifier, pCfa, pRelStore);

    this.relsynth = new RelationSynthesis(pLogger, pRelStore);
  }

  private ImmutableSet<CIdExpression> analysisInterfaceVariables;


  protected ImmutableSet<CIdExpression> getAnalysisInterfaceVariables() {
    if (analysisInterfaceVariables == null) {
      Set<CIdExpression> result = Sets.newHashSet();
      for (AParameterDeclaration param: cfa.getMainFunction().getFunctionParameters()) {
        CParameterDeclaration p = (CParameterDeclaration) param;
        CIdExpression id = new CIdExpression(p.getFileLocation(), p);
        result.add(id);
      }
      analysisInterfaceVariables = ImmutableSet.<CIdExpression>builder().addAll(result).build();
    }
    return analysisInterfaceVariables;
  }

  @Override
  public Collection<AbstractionPredicate> getSyntheticPredicates(ComputeAbstractionState pElement, CFANode pLoc,
      Integer pNewLocInstance) throws UnrecognizedCCodeException, InterruptedException {
    // TODO: Cache

    Collection<AbstractionPredicate> result = Lists.newArrayList();

    Set<CIdExpression> idExprs = getAnalysisInterfaceVariables();
    Collection<CBinaryExpression> predicateExprs = relsynth.getCombinedExpressionsOn(idExprs, pElement.getPathFormula().getSsa());

    for (CExpression e: predicateExprs) {
      BooleanFormula predFormula = converter.makePredicate(
          e,
          pLoc.getEnteringEdge(0),
          pLoc.getFunctionName(),
          pElement.getPathFormula().getSsa().builder());

      AbstractionPredicate abstPred = amgr.makePredicate(predFormula);

      result.add(abstPred);
    }

    return result;
  }



}
