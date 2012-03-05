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
package org.sosy_lab.cpachecker.cpa.predicate;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.ProofChecker;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.predicate.blocking.DefaultBlockOperator;
import org.sosy_lab.cpachecker.cpa.predicate.interfaces.BlockOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.SymbolicRegionManager;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

/**
 * CPA that defines symbolic predicate abstraction.
 */
@Options(prefix="cpa.predicate")
public class PredicateCPA implements ConfigurableProgramAnalysis, StatisticsProvider, ProofChecker {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PredicateCPA.class);
  }

  @Option(name="abstraction.type", toUppercase=true, values={"BDD", "FORMULA"},
      description="What to use for storing abstractions")
  private String abstractionType = "BDD";

  @Option(name="abstraction.initialPredicates",
      description="get an initial set of predicates from a file in MSAT format")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private File predicatesFile = null;

  @Option(description="always check satisfiability at end of block, even if precision is empty")
  private boolean checkBlockFeasibility = false;

  @Option(name="blk.useCache", description="use caching of path formulas")
  private boolean useCache = true;

  @Option(name="merge", values={"SEP", "ABE"}, toUppercase=true,
      description="which merge operator to use for predicate cpa (usually ABE should be used)")
  private String mergeType = "ABE";

  @Option(name="blockOperator", description="Type of the block-operator (any class that implements the interface BlockOperator).")
  @ClassOption(packagePrefix="org.sosy_lab.cpachecker.cpa.predicate.blocking")
  protected Class<? extends BlockOperator> blockOperatorClass = DefaultBlockOperator.class;

  protected final BlockOperator blockOperator;

  protected final Configuration config;
  protected final LogManager logger;
  protected final CFA cfa;

  private final PredicateAbstractDomain domain;
  private final PredicateTransferRelation transfer;
  private final MergeOperator merge;
  private final PredicatePrecisionAdjustment prec;
  private final StopOperator stop;
  private final PredicatePrecision initialPrecision;
  private final ExtendedFormulaManager formulaManager;
  private final FormulaManagerFactory formulaManagerFactory;
  private final PathFormulaManager pathFormulaManager;
  private final Solver solver;
  private final PredicateAbstractionManager predicateManager;
  private final PredicateCPAStatistics stats;
  private final PredicateAbstractElement topElement;

  protected PredicateCPA(Configuration config, LogManager logger, CFA cfa) throws InvalidConfigurationException {
    config.inject(this, PredicateCPA.class);

    this.config = config;
    this.logger = logger;
    this.cfa = cfa;

    this.blockOperator = createBlockOperator();

    formulaManagerFactory = new FormulaManagerFactory(config, logger);

    formulaManager = new ExtendedFormulaManager(formulaManagerFactory.getFormulaManager(), config, logger);
    String libraries = formulaManager.getVersion();

    PathFormulaManager pfMgr = new PathFormulaManagerImpl(formulaManager, config, logger);
    if (useCache) {
      pfMgr = new CachingPathFormulaManager(pfMgr);
    }
    pathFormulaManager = pfMgr;

    TheoremProver theoremProver = formulaManagerFactory.createTheoremProver();
    solver = new Solver(formulaManager, theoremProver);

    RegionManager regionManager;
    if (abstractionType.equals("FORMULA")) {
      regionManager = new SymbolicRegionManager(formulaManager, solver);
    } else {
      assert abstractionType.equals("BDD");
      regionManager = BDDRegionManager.getInstance();
      libraries += " and " + ((BDDRegionManager)regionManager).getVersion();
    }
    logger.log(Level.INFO, "Using predicate analysis with", libraries + ".");

    predicateManager = new PredicateAbstractionManager(regionManager, formulaManager, solver, config, logger);
    transfer = new PredicateTransferRelation(this, blockOperator);

    topElement = PredicateAbstractElement.abstractionElement(pathFormulaManager.makeEmptyPathFormula(), predicateManager.makeTrueAbstractionFormula(null));
    domain = new PredicateAbstractDomain(this);

    if (mergeType.equals("SEP")) {
      merge = MergeSepOperator.getInstance();
    } else if (mergeType.equals("ABE")) {
      merge = new PredicateMergeOperator(this);
    } else {
      throw new InternalError("Update list of allowed merge operators");
    }

    prec = new PredicatePrecisionAdjustment(this);
    stop = new StopSepOperator(domain);

    Collection<AbstractionPredicate> predicates = readPredicatesFromFile();

    if (checkBlockFeasibility) {
      AbstractionPredicate p = predicateManager.makeFalsePredicate();
      if (predicates == null) {
        predicates = ImmutableSet.of(p);
      } else {
        predicates.add(p);
      }
    }
    initialPrecision = new PredicatePrecision(predicates);

    stats = new PredicateCPAStatistics(this, blockOperator);

    GlobalInfo.getInstance().storeFormulaManager(formulaManager);
  }

  protected BlockOperator createBlockOperator() throws InvalidConfigurationException {
    Class<?>[] argumentTypes = { Configuration.class, LogManager.class, CFA.class };
    Object[] argumentValues = { config, logger, cfa };
    return Classes.createInstance(BlockOperator.class, blockOperatorClass, argumentTypes, argumentValues);
  }

  private Collection<AbstractionPredicate> readPredicatesFromFile() {
    if (predicatesFile != null) {
        try {
        String fileContent = Files.toString(predicatesFile, Charset.defaultCharset());
        Formula f = formulaManager.parse(fileContent);

        Collection<Formula> atoms = formulaManager.extractAtoms(f, false, false);

        Collection<AbstractionPredicate> predicates = new ArrayList<AbstractionPredicate>(atoms.size());

        for (Formula atom : atoms) {
          predicates.add(predicateManager.makePredicate(atom));
        }
        return predicates;

      } catch (IllegalArgumentException e) {
        logger.logUserException(Level.WARNING, e, "Could not read predicates from file " + predicatesFile);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not read predicates from file");
      }
    }

    return null;
  }

  @Override
  public PredicateAbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public PredicateTransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return merge;
  }

  @Override
  public StopOperator getStopOperator() {
    return stop;
  }

  public PredicateAbstractionManager getPredicateManager() {
    return predicateManager;
  }

  public ExtendedFormulaManager getFormulaManager() {
    return formulaManager;
  }

  public PathFormulaManager getPathFormulaManager() {
    return pathFormulaManager;
  }

  public Solver getSolver() {
    return solver;
  }

  Configuration getConfiguration() {
    return config;
  }

  LogManager getLogger() {
    return logger;
  }

  public FormulaManagerFactory getFormulaManagerFactory() {
    return formulaManagerFactory;
  }

  @Override
  public PredicateAbstractElement getInitialElement(CFANode node) {
    return topElement;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return initialPrecision;
  }

  @Override
  public PredicatePrecisionAdjustment getPrecisionAdjustment() {
    return prec;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  @Override
  public boolean areAbstractSuccessors(AbstractElement pElement, CFAEdge pCfaEdge, Collection<? extends AbstractElement> pSuccessors) throws CPATransferException, InterruptedException {
    return getTransferRelation().areAbstractSuccessors(pElement, pCfaEdge, pSuccessors);
  }

  @Override
  public boolean isCoveredBy(AbstractElement pElement, AbstractElement pOtherElement) throws CPAException {
    return getAbstractDomain().formulaBasedIsLessOrEqual(pElement, pOtherElement);
  }
}
