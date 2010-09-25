/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicateabstraction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.cpachecker.util.symbpredabstraction.bdd.BDDAbstractFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Predicate;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatInterpolatingProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatPredicateParser;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatTheoremProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.YicesTheoremProver;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;


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

    @Option(name="explicit.abstraction.solver", values = {"mathsat", "yices"})
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
        trans = new PredicateAbstractionTransferRelation(domain, logger);
        merge = MergeSepOperator.getInstance();
        stop = new PredicateAbstractionStopOperator(domain);
        precisionAdjustment = StaticPrecisionAdjustment.getInstance();
        abstractFormulaManager = new BDDAbstractFormulaManager(config);
        MathsatSymbolicFormulaManager mgr = new MathsatSymbolicFormulaManager(config, logger);
        TheoremProver prover;
        if (whichProver.equals("mathsat")) {
            prover = new MathsatTheoremProver(mgr);
        } else if (whichProver.equals("yices")) {
            prover = new YicesTheoremProver(mgr);
        } else {
          throw new InternalError("Update list of allowed solvers!");
        }
        InterpolatingTheoremProver<Integer> itpProver = new MathsatInterpolatingProver(mgr, true);
        amgr = new PredicateAbstractionFormulaManagerImpl<Integer>(abstractFormulaManager, mgr, prover, itpProver, config, logger);

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

    @Override
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

    @Override
    public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
      return SingletonPrecision.getInstance();
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

    LogManager getLogger() {
      return logger;
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
