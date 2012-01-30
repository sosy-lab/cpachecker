/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.relyguarantee;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefiner;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.RGRefinementManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CachingPathFormulaManager;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.relyguarantee.predmap")
public class  RGCPAStatistics implements Statistics {

    private final RGCPA cpa;
    private PredicateRefiner refiner = null;

    public RGCPAStatistics(RGCPA cpa) throws InvalidConfigurationException {
      this.cpa = cpa;
      cpa.getConfiguration().inject(this, RGCPAStatistics.class);
    }

    void addRefiner(PredicateRefiner ref) {
      refiner = ref;
    }

    @Override
    public String getName() {
      return "GCPA";
    }

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      RGRefinementManager<?, ?> amgr = cpa.refManager;

      Multimap<CFANode, AbstractionPredicate> predicates = HashMultimap.create();

      for (Precision precision : reached.getPrecisions()) {
        if (precision instanceof WrapperPrecision) {
          PredicatePrecision preds = ((WrapperPrecision)precision).retrieveWrappedPrecision(PredicatePrecision.class);
          predicates.putAll(preds.getPredicateMap());
        }
      }

      int maxPredsPerLocation = 0;
      for (Collection<AbstractionPredicate> p : predicates.asMap().values()) {
        maxPredsPerLocation = Math.max(maxPredsPerLocation, p.size());
      }

      int allLocs = predicates.keySet().size();
      int totPredsUsed = predicates.size();
      int avgPredsPerLocation = allLocs > 0 ? totPredsUsed/allLocs : 0;
      int allDistinctPreds = (new HashSet<AbstractionPredicate>(predicates.values())).size();

      //RGRefinementManager.PredStats as = amgr.stats;
      //RGRefinementManager.RefStats bs = amgr.refStats;
      RGAbstractDomain domain = cpa.domain;
      RGTransferRelation trans = cpa.transfer;
      RGPrecisionAdjustment prec = cpa.prec;

      CachingPathFormulaManager pfMgr = null;
      if (cpa.pfManager instanceof CachingPathFormulaManager) {
        pfMgr = (CachingPathFormulaManager)cpa.pfManager;
      }


      out.println("Number of coverage checks:         " + domain.coverageCheckTimer.getNumberOfIntervals());
      out.println("  BDD entailment checks:           " + domain.bddCoverageCheckTimer.getNumberOfIntervals());
      out.println("  Symbolic coverage check:         " + domain.symbolicCoverageCheckTimer.getNumberOfIntervals());
      out.println();
      out.println("Max ABE block size:                       " + prec.maxBlockSize);
      out.println("Number of predicates discovered:          " + allDistinctPreds);
      out.println("Number of abstraction locations:          " + allLocs);
      out.println("Max number of predicates per location:    " + maxPredsPerLocation);
      out.println("Avg number of predicates per location:    " + avgPredsPerLocation);
      out.println("Max number of predicates per abstraction: " + prec.maxPredsPerAbstraction);
      out.println();

      out.println("Time for prec operator:             " + prec.totalPrecTime);
      out.println("  Time for abstraction:              " + prec.computingAbstractionTime + " (Max: " + prec.computingAbstractionTime.printMaxTime() + ", Count: " + prec.computingAbstractionTime.getNumberOfIntervals() + ")");
      out.println("Time for merge operator:             " + cpa.merge.totalMergeTime);
      out.println("Time for coverage check:             " + domain.coverageCheckTimer);
      if (domain.bddCoverageCheckTimer.getNumberOfIntervals() > 0) {
        out.println("  Time for BDD entailment checks:    " + domain.bddCoverageCheckTimer);
      }
      if (domain.symbolicCoverageCheckTimer.getNumberOfIntervals() > 0) {
        out.println("  Time for symbolic coverage checks: " + domain.bddCoverageCheckTimer);
      }
    }

    private String toPercent(double val, double full) {
      return String.format("%1.0f", val/full*100) + "%";
    }
}