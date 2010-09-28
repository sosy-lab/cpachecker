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
package org.sosy_lab.cpachecker.cpa.symbpredabsCPA;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.util.symbpredabstraction.CSIsatInterpolatingProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Predicate;
import org.sosy_lab.cpachecker.util.symbpredabstraction.bdd.BDDAbstractFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatInterpolatingProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatPredicateParser;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatTheoremProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.YicesTheoremProver;

/**
 * CPA that defines symbolic predicate abstraction.
 * @author Erkan
 *
 */
@Options(prefix="cpas.symbpredabs")
public class SymbPredAbsCPA implements ConfigurableProgramAnalysis, StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SymbPredAbsCPA.class);
  }

  @Option(name="explicit.abstraction.solver", toUppercase=true, values={"MATHSAT", "YICES"})
  private String whichProver = "MATHSAT";

  @Option(name="interpolatingProver", toUppercase=true, values={"MATHSAT", "CSISAT"})
  private String whichItpProver = "MATHSAT";

  @Option
  private boolean symbolicCoverageCheck = false; 

  @Option(name="abstraction.initialPredicates", type=Option.Type.OPTIONAL_INPUT_FILE)
  private File predicatesFile = null;
  
  @Option(name="interpolation.changesolverontimeout")
  private boolean changeItpSolveOTF = false;
  
  
  private final Configuration config;
  private final LogManager logger;

  private final SymbPredAbsAbstractDomain domain;
  private final SymbPredAbsTransferRelation transfer;
  private final SymbPredAbsMergeOperator merge;
  private final StopOperator stop;
  private final SymbPredAbsPrecision initialPrecision;
  private final AbstractFormulaManager abstractFormulaManager;
  private final SymbPredAbsFormulaManagerImpl<?, ?> formulaManager;
  private final SymbPredAbsCPAStatistics stats;

  private SymbPredAbsCPA(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);

    this.config = config;
    this.logger = logger;

    abstractFormulaManager = new BDDAbstractFormulaManager(config);
    MathsatSymbolicFormulaManager symbolicFormulaManager = new MathsatSymbolicFormulaManager(config, logger);

    TheoremProver thmProver;
    if (whichProver.equals("MATHSAT")) {
      thmProver = new MathsatTheoremProver(symbolicFormulaManager);
    } else if (whichProver.equals("YICES")) {
      thmProver = new YicesTheoremProver(symbolicFormulaManager);
    } else {
      throw new InternalError("Update list of allowed solvers!");
    }

    InterpolatingTheoremProver<Integer> itpProver;
    InterpolatingTheoremProver<Integer> alternativeItpProver = null;
    if (whichItpProver.equals("MATHSAT")) {
      itpProver = new MathsatInterpolatingProver(symbolicFormulaManager, false);
      if(changeItpSolveOTF){
        alternativeItpProver =  new CSIsatInterpolatingProver(symbolicFormulaManager, logger);
      }
    } else if (whichItpProver.equals("CSISAT")) {
      itpProver = new CSIsatInterpolatingProver(symbolicFormulaManager, logger);
      if(changeItpSolveOTF){
        alternativeItpProver = new MathsatInterpolatingProver(symbolicFormulaManager, false);
      }
    } else {
      throw new InternalError("Update list of allowed solvers!");
    }
    formulaManager = new SymbPredAbsFormulaManagerImpl<Integer, Integer>(abstractFormulaManager, symbolicFormulaManager, thmProver, itpProver, alternativeItpProver, config, logger);
    domain = new SymbPredAbsAbstractDomain(abstractFormulaManager, formulaManager, symbolicCoverageCheck);
    transfer = new SymbPredAbsTransferRelation(this);
    merge = new SymbPredAbsMergeOperator(this);
    stop = new StopSepOperator(domain.getPartialOrder());
    
    Set<Predicate> predicates = null;
    if (predicatesFile != null) {
      try {
        String fileContent = Files.readFile(predicatesFile);
        
        MathsatPredicateParser p = new MathsatPredicateParser(symbolicFormulaManager, formulaManager);
        predicates = p.parsePredicates(fileContent);
      } catch (IOException e) {
        logger.log(Level.WARNING, "Could not read predicates from file", predicatesFile,
            "(" + e.getMessage() + ")");
      }
    }
    initialPrecision = new SymbPredAbsPrecision(predicates);

    stats = new SymbPredAbsCPAStatistics(this);
  }

  @Override
  public SymbPredAbsAbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public SymbPredAbsTransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public SymbPredAbsMergeOperator getMergeOperator() {
    return merge;
  }

  @Override
  public StopOperator getStopOperator() {
    return stop;
  }

  protected AbstractFormulaManager getAbstractFormulaManager() {
    return abstractFormulaManager;
  }

  protected SymbPredAbsFormulaManagerImpl<?, ?> getFormulaManager() {
    return formulaManager;
  }

  protected Configuration getConfiguration() {
    return config;
  }

  protected LogManager getLogger() {
    return logger;
  }

  @Override
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
    return new SymbPredAbsAbstractElement(true,
        formulaManager.makeEmptyPathFormula(),
        formulaManager.makeTrueAbstraction(null));
  }

  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return initialPrecision;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
