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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.UnmodifiableIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CParserWithLocationMapper;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.cwriter.FormulaToCExpressionConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class AutomatonBuilder {

  private static final String AUTOMATON_PRE_NAME = "ItpAut";

  private final LogManager logger;
  private MachineModel machineModel;
  private CProgramScope scope;
  private CParser parser;
  private FormulaManagerView formulaManagerView;
  private FormulaToCExpressionConverter formulaToCConverter;

  public AutomatonBuilder(
      FormulaManagerView pFormulaManagerView,
      CFA pCFA,
      Configuration pConfig,
      LogManager pLogger)
      throws InvalidConfigurationException {
    formulaManagerView = pFormulaManagerView;
    logger = pLogger;
    machineModel = pCFA.getMachineModel();
    scope = new CProgramScope(pCFA, pLogger);
    CParser cParser =
        CParser.Factory.getParser(pLogger, CParser.Factory.getOptions(pConfig), machineModel);
    parser = new CParserWithLocationMapper(pConfig, logger, cParser, false);
    formulaToCConverter = new FormulaToCExpressionConverter(formulaManagerView);
  }

  public Automaton buildInterpolantAutomaton(
      ARGPath pPath,
      List<BooleanFormula> pInvariants,
      Optional<BooleanFormula> pInterpolantOpt,
      int automatonIndex)
      throws InvalidAutomatonException, InterruptedException {
    checkArgument(pInterpolantOpt.isPresent());
    checkArgument(pPath.asStatesList().size() == pInvariants.size() + 1);

    ArrayList<BooleanFormula> invariants =
        new ArrayList<>(pInvariants.subList(1, pInvariants.size()));
    invariants.add(formulaManagerView.getBooleanFormulaManager().makeFalse());

    logger.log(
        Level.INFO,
        String.format("Building automaton with interpolant: %s", pInterpolantOpt.get()));

    String automatonName = AUTOMATON_PRE_NAME + automatonIndex;

    UnmodifiableIterator<ARGState> stateIterator = pPath.asStatesList().iterator();
    Iterator<BooleanFormula> itpIterator = invariants.iterator();
    ARGState parentState = stateIterator.next();

    BooleanFormula pInterpolant = pInterpolantOpt.get();
    String itpString = formulaToCConverter.formulaToCExpression(pInterpolant);
    String itpNegatedString =
        formulaToCConverter
            .formulaToCExpression(formulaManagerView.getBooleanFormulaManager().not(pInterpolant));
    StateManager stateMgr =
        new StateManager(
            formulaManagerView,
            parser,
            scope,
            parentState,
            automatonName,
            itpString,
            itpNegatedString);

    while (stateIterator.hasNext() && itpIterator.hasNext()) {
      ARGState currentState = stateIterator.next();
      BooleanFormula invariant = formulaManagerView.uninstantiate(itpIterator.next());
      stateMgr.addTransition(invariant, currentState);
    }

    return stateMgr.createAutomaton();
  }

}
