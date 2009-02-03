/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.symbpredabs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Vector;

import logging.CustomLogLevel;
import logging.LazyLogger;

import cmdline.CPAMain;

import cfa.objectmodel.CFAFunctionDefinitionNode;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpa.symbpredabs.mathsat.BDDMathsatAbstractFormulaManager;
import cpa.symbpredabs.mathsat.MathsatPredicateParser;
import cpa.symbpredabs.mathsat.MathsatSymbolicFormulaManager;

/**
 * TODO. This is currently broken
 */
public class SymbPredAbstCPA implements ConfigurableProgramAnalysis {

    private AbstractDomain abstractDomain;
    private TransferRelation transferRelation;
    private MergeOperator mergeOperator;
    private StopOperator stopOperator;
    private PrecisionAdjustment precisionAdjustment;

    private PredicateMap predicateMap;
    private SymbolicFormulaManager formulaManager;
    private AbstractFormulaManager abstractManager;
    
    private SymbPredAbstCPA() {
        SymbPredAbstDomain domain = new SymbPredAbstDomain(this);
        abstractDomain = domain;
        transferRelation = new SymbPredAbstTransfer(domain);
        mergeOperator = new SymbPredAbstMerge(domain);
        stopOperator = new SymbPredAbstStop(domain);
        precisionAdjustment = new SymbPredAbstPrecisionAdjustment();
        Collection<Predicate> preds = null;
        formulaManager = new MathsatSymbolicFormulaManager();
        abstractManager = new BDDMathsatAbstractFormulaManager();
        MathsatPredicateParser p = new MathsatPredicateParser(
                (MathsatSymbolicFormulaManager)formulaManager,
                (BDDMathsatAbstractFormulaManager)abstractManager);
        try {
            String pth = CPAMain.cpaConfig.getProperty("predicates.path");
            File f = new File(pth, "predicates.msat");
            InputStream in = new FileInputStream(f);
            preds = p.parsePredicates(in);
        } catch (IOException e) {
            e.printStackTrace();
            preds = new Vector<Predicate>();
        }
        predicateMap = new FixedPredicateMap(preds);
    }

    public SymbPredAbstCPA(String s1, String s2) {
        this();
    }

    public PredicateMap getPredicateMap() { return predicateMap; }

    public SymbolicFormulaManager getFormulaManager() { return formulaManager; }
    public AbstractFormulaManager getAbstractFormulaManager() {
        return abstractManager;
    }

    public AbstractDomain getAbstractDomain() {
        return abstractDomain;
    }
    
    public TransferRelation getTransferRelation() {
        return transferRelation;
    }

    public MergeOperator getMergeOperator() {
        return mergeOperator;
    }

    public StopOperator getStopOperator() {
        return stopOperator;
    }

    public PrecisionAdjustment getPrecisionAdjustment() {
      return precisionAdjustment;
    }

    public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
        LazyLogger.log(CustomLogLevel.SpecificCPALevel,
                "Getting initial element from node: ", node.getNodeNumber());

        return new SymbPredAbstElement(node, formulaManager.makeTrue(), abstractManager.makeTrue(), this);
    }
    
    public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
      return new SymbPredAbstPrecision();
    }
}
