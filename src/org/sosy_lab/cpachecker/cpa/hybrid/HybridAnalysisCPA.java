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
package org.sosy_lab.cpachecker.cpa.hybrid;

import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StopNeverOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.cpa.hybrid.util.AssumptionParser;
import org.sosy_lab.cpachecker.cpa.hybrid.util.CollectionUtils;
import org.sosy_lab.cpachecker.cpa.hybrid.util.OperatorType;
import org.sosy_lab.cpachecker.util.LiveVariables;

@Options(prefix = "cpa.hybrid")
public class HybridAnalysisCPA implements ConfigurableProgramAnalysis {
    @Option(secure = true, 
            name = "initialAssumptions",
            description = "The initial assumptions for a given program.")
    private String initialAssumptionsStringEncoded = "";

    @Option(secure = true,
            name = "delimiter",
            description = "The delimiter for different assumptions in 'initialAssumptions' (e.g. x = 10.0; y = -5).")
    private String delimiter = ";";

    @Option(secure = true,
            name = "mergeOperator",
            description = "The type of merge operator to use.")
    private OperatorType mergeOperatorType = OperatorType.SEP;

    @Option(secure = true,
            name = "stopOperator",
            description = "The type of stop operator to use.")
    private OperatorType stopOperatorType = OperatorType.SEP;

    public static CPAFactory factory() {
        return AutomaticCPAFactory.forType(HybridAnalysisCPA.class);
    }
    
    private static final String cfaErrorMessage = "CFA must be present for HybridAnalysis";

    private final AbstractDomain abstractDomain;
    private final CFA cfa;
    private final LogManager logger;
    private final @Nullable AssumptionParser assumptionParser;
    private final CProgramScope scope;

    protected HybridAnalysisCPA( 
        CFA cfa, 
        LogManager logger,
        Configuration configuration) throws InvalidConfigurationException {
            
        // HybridAnalysisState implements LatticeAbstractState
        this.abstractDomain = DelegateAbstractDomain.<HybridAnalysisState>getInstance();
        this.cfa = Preconditions.checkNotNull(cfa, cfaErrorMessage);
        this.logger = logger;
        this.scope = new CProgramScope(cfa, logger);
        this.assumptionParser = 
            new AssumptionParser(delimiter, scope, configuration, cfa.getMachineModel(), logger);
        configuration.inject(this);
    }

    @Override
    public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
            throws InterruptedException {

        // there have not been any anitial live variables
        if(assumptionParser == null)
        {
            return new HybridAnalysisState();
        }

        try {
            return new HybridAnalysisState(
                    assumptionParser.parseAssumptions(initialAssumptionsStringEncoded));
        } catch (InvalidAutomatonException e) {
            logger.logException(Level.INFO, e, "Assumption parsing failed.");
        }

        return new HybridAnalysisState();
    }

    @Override
    public TransferRelation getTransferRelation() {
        return new HybridAnalysisTransferRelation();
    }

    @Override
    public AbstractDomain getAbstractDomain() {
        return abstractDomain;
    }

    @Override
    public MergeOperator getMergeOperator() {
        switch (mergeOperatorType) {
            case SEP:
              return MergeSepOperator.getInstance();
      
            case JOIN:
              return new MergeJoinOperator(getAbstractDomain());
      
            default:
              throw new AssertionError("Unknown merge operator");
        }
    }

    @Override
    public StopOperator getStopOperator() {
        switch (stopOperatorType) {
            case SEP:
              return new StopSepOperator(getAbstractDomain());
      
            case JOIN:
              return new StopJoinOperator(getAbstractDomain());
      
            case NEVER:
              return new StopNeverOperator();
      
            default:
              throw new AssertionError("Unknown stop operator");
        }
    }
}