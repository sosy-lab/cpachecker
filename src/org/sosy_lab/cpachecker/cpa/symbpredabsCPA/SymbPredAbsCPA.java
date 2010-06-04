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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
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
import org.sosy_lab.cpachecker.util.symbpredabstraction.PathFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.SSAMap;
import org.sosy_lab.cpachecker.util.symbpredabstraction.bdd.BDDAbstractFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Predicate;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatInterpolatingProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatPredicateParser;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatTheoremProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.SimplifyTheoremProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.YicesTheoremProver;

import com.google.common.collect.ImmutableList;

/**
 * CPA that defines symbolic predicate abstraction.
 * @author Erkan
 *
 */
@Options(prefix="cpas.symbpredabs")
public class SymbPredAbsCPA implements ConfigurableProgramAnalysis, StatisticsProvider {

  private static class SymbPredAbsCPAFactory extends AbstractCPAFactory {
    @Override
    public ConfigurableProgramAnalysis createInstance() throws InvalidConfigurationException {
      return new SymbPredAbsCPA(getConfiguration(), getLogger());
    }
  }

  public static CPAFactory factory() {
    return new SymbPredAbsCPAFactory();
  }

  @Option(name="explicit.abstraction.solver", toUppercase=true, values={"MATHSAT", "SIMPLIFY", "YICES"})
  private String whichProver = "MATHSAT";

  @Option(name="interpolatingProver", toUppercase=true, values={"MATHSAT", "CSISAT"})
  private String whichItpProver = "MATHSAT";

  @Option
  private boolean symbolicCoverageCheck = false; 

  @Option(name="abstraction.initialPredicates")
  private String predicatesFile = "";
  
  private final Configuration config;
  private final LogManager logger;

  private final SymbPredAbsAbstractDomain domain;
  private final SymbPredAbsTransferRelation transfer;
  private final SymbPredAbsMergeOperator merge;
  private final StopOperator stop;
  private final SymbPredAbsPrecision initialPrecision;
  private final AbstractFormulaManager abstractFormulaManager;
  private final MathsatSymbolicFormulaManager symbolicFormulaManager;
  private final SymbPredAbsFormulaManager formulaManager;
  private final SymbPredAbsCPAStatistics stats;

  private SymbPredAbsCPA(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);

    this.config = config;
    this.logger = logger;

    abstractFormulaManager = new BDDAbstractFormulaManager(config);
    MathsatSymbolicFormulaManager mmgr = new MathsatSymbolicFormulaManager(config, logger);
    symbolicFormulaManager = mmgr; 

    TheoremProver thmProver;
    if (whichProver.equals("MATHSAT")) {
      thmProver = new MathsatTheoremProver(symbolicFormulaManager, false, config);
    } else if (whichProver.equals("SIMPLIFY")) {
      thmProver = new SimplifyTheoremProver(symbolicFormulaManager, config, logger);
    } else if (whichProver.equals("YICES")) {
      thmProver = new YicesTheoremProver(symbolicFormulaManager);
    } else {
      throw new InternalError("Update list of allowed solvers!");
    }

    InterpolatingTheoremProver<Integer> itpProver;
    if (whichItpProver.equals("MATHSAT")) {
      itpProver = new MathsatInterpolatingProver(symbolicFormulaManager, false, config);
    } else if (whichItpProver.equals("CSISAT")) {
      itpProver = new CSIsatInterpolatingProver(symbolicFormulaManager, logger);
    } else {
      throw new InternalError("Update list of allowed solvers!");
    }
    formulaManager = new SymbPredAbsFormulaManagerImpl<Integer>(abstractFormulaManager, symbolicFormulaManager, thmProver, itpProver, config, logger);
    domain = new SymbPredAbsAbstractDomain(abstractFormulaManager, formulaManager, symbolicCoverageCheck);
    transfer = new SymbPredAbsTransferRelation(this);
    merge = new SymbPredAbsMergeOperator(this);
    stop = new StopSepOperator(domain.getPartialOrder());
    
    Set<Predicate> predicates = null;
    if (!predicatesFile.isEmpty()) {
      MathsatPredicateParser p = new MathsatPredicateParser(mmgr, formulaManager);
      try {
        InputStream file = new FileInputStream(predicatesFile);
        predicates = p.parsePredicates(file);
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

  public SymbPredAbsTransferRelation getTransferRelation() {
    return transfer;
  }

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

  protected SymbPredAbsFormulaManager getFormulaManager() {
    return formulaManager;
  }

  protected SymbolicFormulaManager getSymbolicFormulaManager() {
    return symbolicFormulaManager;
  }

  protected Configuration getConfiguration() {
    return config;
  }

  protected LogManager getLogger() {
    return logger;
  }

  public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
    ImmutableList<CFANode> oldAbstractionPath = ImmutableList.of();
    PathFormula pf = new PathFormula(symbolicFormulaManager.makeTrue(), new SSAMap());
    AbstractFormula initAbstraction = abstractFormulaManager.makeTrue();

    return new SymbPredAbsAbstractElement(node,
        pf, pf, initAbstraction, oldAbstractionPath);
  }

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
