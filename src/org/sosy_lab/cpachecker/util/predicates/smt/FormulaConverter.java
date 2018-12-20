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
/**
 * Extensions of the pure {@link org.sosy_lab.java_smt.api.FormulaManager}
 * interface and its related interfaces
 * that make it easier to use by client code.
 * This package can be used regardless of which SMT solver is the backend.
 *
 * The most important feature of this package is to replace an SMT theory
 * with another one, simulating the semantics of the replaced theory
 * with other theories.
 * This can be used to allow working with {@link org.sosy_lab.java_smt.api.BitvectorFormula}
 * even if the solver does not support the theory of bitvectors.
 * Bitvectors will then be approximated with rationals or integers.
 */
package org.sosy_lab.cpachecker.util.predicates.smt;

import java.util.Collection;
import java.util.Optional;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.automaton.CParserUtils;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.cpa.automaton.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.cpa.hybrid.util.CollectionUtils;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class FormulaConverter {

    private final FormulaManagerView formulaManagerView;
    private final FormulaToCVisitor toCVisitor;
    private final CParser parser;
    private final Scope scope;
    private final ParserTools parserTools;
    private final MachineModel machineModel;
    private final LogManager logger;

    public FormulaConverter(
        FormulaManagerView pFormulaManagerView,
        Scope pScope,
        LogManager pLogger,
        MachineModel pMachineModel,
        Configuration configuration)
            throws InvalidConfigurationException {

        this.formulaManagerView = pFormulaManagerView;
        this.toCVisitor = new FormulaToCVisitor(pFormulaManagerView);

        this.parser = CParser.Factory.getParser(
            LogManager.createNullLogManager(),
            CParser.Factory.getOptions(configuration),
            pMachineModel);

        this.scope = pScope;
        this.logger = pLogger;

        this.parserTools = 
            ParserTools.create(ExpressionTrees.newCachingFactory(), pMachineModel, pLogger);
        this.machineModel = pMachineModel;
    }

    public Collection<CBinaryExpression> convertFormulaToCBinaryExpressions(BooleanFormula formula)
                    throws InvalidAutomatonException {

        // convert Formula to C-String
        Boolean isValid = formulaManagerView.visit(formula, toCVisitor);

        // if the formula is invalid, the resulting string is useless
        if(!isValid) {
            throw new InvalidAutomatonException(String.format("The boolean formula %s could not be parsed", formula));
        }

        final String cCodeString = toCVisitor.getString();

        // parse c code to expression
        Collection<CStatement> statements = CParserUtils.parseAsCStatements(
            cCodeString, 
            Optional.empty(), 
            parser, 
            scope, 
            parserTools);

        return CollectionUtils.ofType( 
            CParserUtils.convertStatementsToAssumptions(statements, machineModel, logger),
            CBinaryExpression.class);
    }
}
