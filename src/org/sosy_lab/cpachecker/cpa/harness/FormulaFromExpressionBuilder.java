/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.harness;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.Formula;

public class FormulaFromExpressionBuilder {

  private CFA cfa;
  private LogManager logger;
  private ConfigurableProgramAnalysis wrappedCpa;
  private PredicateAbstractState predicateState;
  private Configuration config;
  private ShutdownNotifier shutdownNotifier;

  public FormulaFromExpressionBuilder(
      CFA pCFA,
      LogManager pLogger,
      ConfigurableProgramAnalysis pWrappedCpa,
      PredicateAbstractState pPredicateState,
      Configuration pConfig,
      ShutdownNotifier pShutdownNotifier) {
    cfa = pCFA;
    logger = pLogger;
    wrappedCpa = pWrappedCpa;
    predicateState = pPredicateState;
    config = pConfig;
    shutdownNotifier = pShutdownNotifier;
  }

  public List<Formula>
      buildFormulasFromExpressions(
          CFAEdge pEdge,
          List<CExpression> pFunctionParametersOfPointerType)
          throws InvalidConfigurationException {
    List<Formula> result = new LinkedList<>();

    PredicateCPA predicateCPA =
        CPAs.retrieveCPAOrFail(wrappedCpa, PredicateCPA.class, HarnessTransferRelation.class);

    Solver solver = predicateCPA.getSolver();
    CtoFormulaTypeHandler typeHandler = new CtoFormulaTypeHandler(logger, cfa.getMachineModel());
    CtoFormulaConverter converter =
        new CtoFormulaConverter(
            new FormulaEncodingOptions(config),
            solver.getFormulaManager(),
            cfa.getMachineModel(),
            Optional.empty(),
            logger,
            shutdownNotifier,
            typeHandler,
            AnalysisDirection.FORWARD);

    SSAMapBuilder ssaBuilder = predicateState.getPathFormula().getSsa().builder();
    PointerTargetSetBuilder pointerTargetSetBuilder =
        PointerTargetSetBuilder.DummyPointerTargetSetBuilder.INSTANCE;
    BooleanFormulaManagerView booleanManagerView =
        solver.getFormulaManager().getBooleanFormulaManager();
    Constraints constraints = new Constraints(booleanManagerView);
    ErrorConditions errorConditions = ErrorConditions.dummyInstance(booleanManagerView);

    for (CExpression pointerParameter : pFunctionParametersOfPointerType) {
      try {
        Formula term =
            converter.buildTerm(
                pointerParameter,
                pEdge,
                pEdge.getPredecessor().getFunctionName(),
                ssaBuilder,
                pointerTargetSetBuilder,
                constraints,
                errorConditions);
        result.add(term);
      } catch (UnrecognizedCodeException e) {
        logger.log(Level.WARNING, "Could not generate Formula from CExpression.");
      }
    }

    return result;
  }

}
