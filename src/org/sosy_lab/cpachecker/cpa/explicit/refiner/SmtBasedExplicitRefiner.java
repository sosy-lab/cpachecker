/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit.refiner;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitPrecision;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.PredicateMap;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.ReferencedVariablesCollector;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;

import com.google.common.collect.Multimap;

@Options(prefix="cpa.explict.refiner")
public class SmtBasedExplicitRefiner extends ExplicitRefiner {

  private final ExtendedFormulaManager formulaManager;

  // statistics
  private Timer timerSyntacticalPathAnalysis = new Timer();

  protected SmtBasedExplicitRefiner(
      Configuration config,
      PathFormulaManager pathFormulaManager,
      ExtendedFormulaManager formulaManager) throws InvalidConfigurationException {
    super(config, pathFormulaManager);
    config.inject(this);
    this.formulaManager = formulaManager;
  }

  @Override
  protected Multimap<CFANode, String> determinePrecisionIncrement(
      UnmodifiableReachedSet reachedSet,
      ExplicitPrecision oldPrecision) throws CPAException {
    // create the mapping of CFA nodes to predicates, based on the counter example trace info
    PredicateMap predicateMap = new PredicateMap(currentTraceInfo.getPredicatesForRefinement(), currentNodePath);

    // determine the precision increment
    Multimap<CFANode, String> precisionIncrement = predicateMap.determinePrecisionIncrement(formulaManager);

    // also add variables occurring on the error path and referenced by variables in precision increment
    precisionIncrement.putAll(determineReferencedVariablesInPath(oldPrecision, precisionIncrement));

    // create the new precision
    return precisionIncrement;
  }

  private Multimap<CFANode, String> determineReferencedVariablesInPath(
      ExplicitPrecision precision,
      Multimap<CFANode, String> precisionIncrement) {

    List<CFAEdge> cfaTrace = new ArrayList<CFAEdge>();
    for(int i = 0; i < currentEdgePath.size(); i++) {
      cfaTrace.add(currentEdgePath.get(i).getSecond());
    }

    // the referenced-variable-analysis has the done on basis of all variables in the precision plus the current increment
    Collection<String> referencingVariables = precision.getCegarPrecision().getVariablesInPrecision();
    referencingVariables.addAll(precisionIncrement.values());

    ReferencedVariablesCollector collector = new ReferencedVariablesCollector(referencingVariables);
    Multimap<CFANode, String> referencedVariables = collector.collectVariables(cfaTrace);

    return referencedVariables;
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    out.println(this.getClass().getSimpleName() + ":");
    out.println("  number of explicit refinements:            " + numberOfExplicitRefinements);
    out.println("  max. time for syntactical path analysis:   " + timerSyntacticalPathAnalysis.printMaxTime());
    out.println("  total time for syntactical path analysis:  " + timerSyntacticalPathAnalysis);
  }
}