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
package org.sosy_lab.cpachecker.cpa.hybrid.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.automaton.CParserUtils;
import org.sosy_lab.cpachecker.cpa.automaton.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

/**
 * This class provides methods for parsing string encoded assumptions into CExpressions
 */
public class AssumptionParser {

    private final String delimiter;
    private final Scope scope;
    private final LogManager logger;
    private final ParserTools parserTools;
    private final CParser cParser;
    private final MachineModel machineModel;
    
    public AssumptionParser(
            final String pDelimiter,
                            final Scope pScope,
                            final Configuration pConfiguration,
                            final MachineModel pMachineModel,
                            final LogManager pLogger) throws InvalidConfigurationException{
        this.delimiter = pDelimiter;
        this.scope = pScope;
        this.logger = pLogger;
        this.parserTools = 
            ParserTools.create(ExpressionTrees.newCachingFactory(), pMachineModel, pLogger);

        cParser =
        CParser.Factory.getParser(
            LogManager.createNullLogManager(),
            CParser.Factory.getOptions(pConfiguration),
            pMachineModel);

        this.machineModel = pMachineModel;
    }

    /**
     * Currently the implementation relies on correctnes of the given configuration
     * 
     * @param assumptions The string containing all defined string encoded assumptions
     * @return A set of parsed CExpressions
     */
    public Set<CExpression> parseAssumptions(final String assumptions) throws InvalidAutomatonException {
        if(assumptions.isEmpty()) {
            return Collections.emptySet();
        }

        // build set of single statements
        Set<String> splitAssumptions = Sets.newHashSet(assumptions.split(delimiter));

        Collection<CStatement> statements = CParserUtils.parseStatements(splitAssumptions, Optional.empty(), cParser, scope, parserTools);  

        // convert statements to CExpressions
        Collection<CExpression> expressions = CollectionUtils.ofType(
            CParserUtils.convertStatementsToAssumptions(statements, machineModel, logger),
            CExpression.class);

        return ImmutableSet.copyOf(expressions);
    }
}