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
package cpa.predicateabstraction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Vector;
import java.util.logging.Level;

import symbpredabstraction.FixedPredicateMap;
import symbpredabstraction.UpdateablePredicateMap;
import symbpredabstraction.bdd.BDDAbstractFormulaManager;
import symbpredabstraction.interfaces.AbstractFormulaManager;
import symbpredabstraction.interfaces.InterpolatingTheoremProver;
import symbpredabstraction.interfaces.Predicate;
import symbpredabstraction.interfaces.PredicateMap;
import symbpredabstraction.interfaces.TheoremProver;
import symbpredabstraction.mathsat.MathsatInterpolatingProver;
import symbpredabstraction.mathsat.MathsatPredicateParser;
import symbpredabstraction.mathsat.MathsatSymbolicFormulaManager;
import symbpredabstraction.mathsat.MathsatTheoremProver;
import symbpredabstraction.mathsat.SimplifyTheoremProver;
import symbpredabstraction.mathsat.YicesTheoremProver;
import cfa.objectmodel.CFAFunctionDefinitionNode;

import common.configuration.Configuration;
import common.configuration.Option;
import common.configuration.Options;

import cpa.common.LogManager;
import cpa.common.defaults.AbstractCPAFactory;
import cpa.common.defaults.MergeSepOperator;
import cpa.common.defaults.StaticPrecisionAdjustment;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.CPAFactory;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.Statistics;
import cpa.common.interfaces.StatisticsProvider;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.InvalidConfigurationException;


/**
 * CPA for Explicit-state lazy abstraction.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
@Options(prefix="cpas.symbpredabs")
public class PredicateAbstractionCPA implements ConfigurableProgramAnalysis, StatisticsProvider {

    private static class PredicateAbstractionCPAFactory extends AbstractCPAFactory {
      
      @Override
      public ConfigurableProgramAnalysis createInstance() throws InvalidConfigurationException {
        return new PredicateAbstractionCPA(getConfiguration(), getLogger());
      }
    }
    
    public static CPAFactory factory() {
      return new PredicateAbstractionCPAFactory();
    }
    
    @Option(name="explicit.abstraction.solver", values = {"mathsat", "simplify", "yices"})
    private String whichProver = "mathsat";
    
    @Option(name="abstraction.fixedPredMap")
    private String fixedPredMapFile = "";
    
    @Option(name="abstraction.norefinement")
    private boolean noRefinement = false;
    
    @Option(name="refinement.addPredicatesGlobally")
    private boolean addPredicatesGlobally;
  
    private final PredicateAbstractionAbstractDomain domain;
    private final PredicateAbstractionTransferRelation trans;
    private final MergeOperator merge;
    private final PredicateAbstractionStopOperator stop;
    private final PrecisionAdjustment precisionAdjustment;
    private final AbstractFormulaManager abstractFormulaManager;
    private final PredicateAbstractionFormulaManager amgr;
    private final PredicateMap pmap;
    private final PredicateAbstractionCPAStatistics stats;
    private final LogManager logger;

    private PredicateAbstractionCPA(Configuration config, LogManager logger) throws InvalidConfigurationException {
        this.logger = logger;
        domain = new PredicateAbstractionAbstractDomain(this);
        trans = new PredicateAbstractionTransferRelation(domain);
        merge = MergeSepOperator.getInstance();
        stop = new PredicateAbstractionStopOperator(domain);
        precisionAdjustment = StaticPrecisionAdjustment.getInstance();
        abstractFormulaManager = new BDDAbstractFormulaManager(config);
        MathsatSymbolicFormulaManager mgr = new MathsatSymbolicFormulaManager(config, logger);
        TheoremProver prover = null;
        if (whichProver.equals("mathsat")) {
            prover = new MathsatTheoremProver(mgr, false, config);
        } else if (whichProver.equals("simplify")) {
            prover = new SimplifyTheoremProver(mgr, config, logger);
        } else if (whichProver.equals("yices")) {
            prover = new YicesTheoremProver(mgr);
        } else {
          throw new InternalError("Update list of allowed solvers!");
        }
        InterpolatingTheoremProver<Integer> itpProver =
            new MathsatInterpolatingProver(mgr, true, config);
        amgr = new MathsatPredicateAbstractionFormulaManager<Integer>(abstractFormulaManager, mgr, prover, itpProver, config, logger);

//        covers = new HashMap<ExplicitAbstractElement,
//                             Set<ExplicitAbstractElement>>();
//        covered = new HashSet<PredicateAbstractionAbstractElement>();

        MathsatPredicateParser p = new MathsatPredicateParser(mgr, amgr);
        Collection<Predicate> preds;
        try {
            if (!fixedPredMapFile.isEmpty()) {
                File f = new File(fixedPredMapFile);
                InputStream in = new FileInputStream(f);
                preds = p.parsePredicates(in);
            } else {
                preds = null;
            }
        } catch (IOException e) {
          logger.logException(Level.WARNING, e, "");
            preds = new Vector<Predicate>();
        }
        if (noRefinement) {
            pmap = new FixedPredicateMap(preds);
        } else {
            pmap = new UpdateablePredicateMap(preds, addPredicatesGlobally);
        }

        stats = new PredicateAbstractionCPAStatistics(this, config);
    }

    @Override
    public AbstractDomain getAbstractDomain() {
        return domain;
    }

    @Override
    public TransferRelation getTransferRelation() {
        return trans;
    }

    public MergeOperator getMergeOperator() {
        return merge;
    }

    @Override
    public StopOperator getStopOperator() {
        return stop;
    }

    @Override
    public PrecisionAdjustment getPrecisionAdjustment() {
      return precisionAdjustment;
    }

    @Override
    public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
      logger.log(Level.FINEST, 
                       "Getting initial element from node: ", node);

        PredicateAbstractionAbstractElement e = new PredicateAbstractionAbstractElement(this);
        e.setAbstraction(abstractFormulaManager.makeTrue());
        return e;
    }
    
    public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
      return null;
    }

    public AbstractFormulaManager getAbstractFormulaManager() {
        return abstractFormulaManager;
    }

    public PredicateAbstractionFormulaManager getPredAbsFormulaManager() {
      return amgr;
    }
    
    public PredicateMap getPredicateMap() {
        return pmap;
    }

    @Override
    public void collectStatistics(Collection<Statistics> pStatsCollection) {
      pStatsCollection.add(stats);
    }
    
//    public void setCovered(PredicateAbstractionAbstractElement e1) {
//        covered.add(e1);        
//    }
//    
//    public Collection<PredicateAbstractionAbstractElement> getCovered() {
//        return covered;
//    }
//    
//    public void setUncovered(PredicateAbstractionAbstractElement e1) {
//        covered.remove(e1);
//    }

//    public Set<ExplicitAbstractElement> getCoveredBy(ExplicitAbstractElement e){
//        if (covers.containsKey(e)) {
//            return covers.get(e);
//        } else {
//            return Collections.emptySet();
//        }
//    }
//
//    public void setCoveredBy(ExplicitAbstractElement covered,
//                             ExplicitAbstractElement e) {
//        Set<ExplicitAbstractElement> s;
//        if (covers.containsKey(e)) {
//            s = covers.get(e);
//        } else {
//            s = new HashSet<ExplicitAbstractElement>();
//        }
//        s.add(covered);
//        covers.put(e, s);
//    }
//
//    public void uncoverAll(ExplicitAbstractElement e) {
//        if (covers.containsKey(e)) {
//            covers.remove(e);
//        }
//    }

}
