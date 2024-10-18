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
package org.sosy_lab.cpachecker.core;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.AbstractMBean;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.EmptyCFAException;
import org.sosy_lab.cpachecker.cfa.RGCFA;
import org.sosy_lab.cpachecker.cfa.RGCFACreator;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.BMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ConcurrentAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CounterexampleCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ExternalCBMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RGAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RGCEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RestartAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGCPA;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGVariables;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvironmentManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.AbstractElements;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;

public class CPAchecker {

  public static interface CPAcheckerMXBean {
    public int getReachedSetSize();

    public void stop();
  }

  private static class CPAcheckerBean extends AbstractMBean implements CPAcheckerMXBean {

    private final ReachedSet reached;
    private final Thread cpacheckerThread;

    public CPAcheckerBean(ReachedSet pReached, LogManager logger) {
      super("org.sosy_lab.cpachecker:type=CPAchecker", logger);
      reached = pReached;
      cpacheckerThread = Thread.currentThread();
      register();
    }

    @Override
    public int getReachedSetSize() {
      return reached.size();
    }

    @Override
    public void stop() {
      cpacheckerThread.interrupt();
    }

  }

  @Options
  private static class CPAcheckerOptions {

    @Option(name="analysis.useAssumptionCollector",
        description="use assumption collecting algorithm")
        boolean useAssumptionCollector = false;

    @Option(name = "analysis.useRefinement",
        description = "use CEGAR algorithm for lazy counter-example guided analysis"
          + "\nYou need to specify a refiner with the cegar.refiner option."
          + "\nCurrently all refiner require the use of the ARTCPA.")
          boolean useRefinement = false;

    @Option(name="analysis.useCBMC",
        description="use CBMC to double-check counter-examples")
        boolean useCBMC = false;

    @Option(name="analysis.useBMC",
        description="use a BMC like algorithm that checks for satisfiability "
          + "after the analysis has finished, works only with PredicateCPA")
          boolean useBMC = false;

    @Option(name="analysis.stopAfterError",
        description="stop after the first error has been found")
        boolean stopAfterError = true;

    @Option(name="analysis.restartAfterUnknown",
        description="restart the algorithm using a different CPA after unknown result")
        boolean useRestartingAlgorithm = false;

    @Option(name="analysis.externalCBMC",
        description="use CBMC as an external tool from CPAchecker")
        boolean runCBMCasExternalTool = false;

  }

  private final LogManager logger;
  private final Configuration config;
  private final CPAcheckerOptions options;
  private final ReachedSetFactory reachedSetFactory;

  /**
   * This method will throw an exception if the user has requested CPAchecker to
   * stop immediately. This exception should not be caught by the caller.
   */
  public static void stopIfNecessary() throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
  }

  public CPAchecker(Configuration pConfiguration, LogManager pLogManager) throws InvalidConfigurationException {
    config = pConfiguration;
    logger = pLogManager;

    options = new CPAcheckerOptions();
    config.inject(options);
    reachedSetFactory = new ReachedSetFactory(pConfiguration);
  }


  public CPAcheckerResult runAny(String filename) {
    CPAcheckerResult result=null;
    String[] concurrentOption = config.getPropertiesArray("analysis.concurrent");

    // check if multiple threads are expected
    if (concurrentOption.length>0  && concurrentOption[0].equals("true")){
      result = runRelyGuarantee(filename);
    }
    // run analysis for a single program
    else  {
      result = run(filename);
    }

      return result;
  }


  // analysis for multiple threads
  private CPAcheckerResult runRelyGuarantee(String filename) {

    ConfigurableProgramAnalysis[] cpas = null;
    ReachedSet initalReachedSets[] = null;
    MainCPAStatistics stats = null;
    Result result=null;

    try {
      stats = new MainCPAStatistics(config, logger);

      stats.creationTime.start();

      // get main functions and CFA
      Pair<CFAFunctionDefinitionNode[], RGCFA[]> tuple = getMainFunctionsAndCfas(filename);
      CFAFunctionDefinitionNode[] mainFunctions = tuple.getFirst();
      RGCFA[] cfas = tuple.getSecond();

      int threadNo = mainFunctions.length;

      cpas  = new ConfigurableProgramAnalysis[threadNo];

      // extract variables
      RGVariables vars = new RGVariables(cfas);

      RGEnvironmentManager environment = new RGEnvironmentManager(threadNo, vars, config, logger);

      // create a cpa for each thread
      for(int i=0; i<threadNo; i++){
        ConfigurableProgramAnalysis cpa = createCPA(stats);
        WrapperCPA wCPA = (WrapperCPA) cpa;
        RGCPA rgCPA = wCPA.retrieveWrappedCpa(RGCPA.class);
        rgCPA.setData(i, environment.getVariables(), cfas);
        //rgCPA.useHardcodedPredicates();
        cpas[i] = cpa;
      }

      // get the initial reached sets
      initalReachedSets = new ReachedSet[threadNo];
      for(int i=0; i<threadNo; i++){
        initalReachedSets[i] = createInitialReachedSet(cpas[i], mainFunctions[i]);
      }

      // TODO handle only with ConcurrentAlgorithm
      RGAlgorithm rgAlgorithm = new RGAlgorithm(cfas, mainFunctions, cpas, environment, vars, config, logger);
      ConcurrentAlgorithm algorithm;

      if (options.useRefinement) {
        algorithm = new RGCEGARAlgorithm(rgAlgorithm, config, logger);

      } else {
        algorithm = rgAlgorithm;
      }

      // add the main statistics of the algorithm
      stats.getSubStatistics().clear();
      algorithm.collectStatistics(stats.getSubStatistics());
      stats.creationTime.stop();


      Set<String> unusedProperties = config.getUnusedProperties();
      if (!unusedProperties.isEmpty()) {
        logger.log(Level.WARNING, "The following configuration options were specified but are not used:\n",
            Joiner.on("\n ").join(unusedProperties), "\n");
      }


      stopIfNecessary();

      // TODO change to multiple
      // register management interface for CPAchecker
      CPAcheckerBean mxbean = new CPAcheckerBean(initalReachedSets[0], logger);
      try {
        result = runRelyGuaranteeAlgorithm(algorithm, initalReachedSets, stats);
      } finally {
        // unregister management interface for CPAchecker
        mxbean.unregister();
      }

    } catch (IOException e) {
      logger.log(Level.SEVERE, "Could not read file",
          (e.getMessage() != null ? "(" + e.getMessage() + ")" : ""));

    } catch (ParserException e) {
      // only log message, not whole exception because this is a C problem,
      // not a CPAchecker problem
      logger.log(Level.SEVERE, Throwables.getRootCause(e).getMessage());
      logger.log(Level.INFO, "Make sure that the code was preprocessed using Cil (HowTo.txt).\n"
          + "If the error still occurs, please send this error message together with the input file to cpachecker-users@sosy-lab.org.");

    } catch (InvalidConfigurationException e) {
      logger.log(Level.SEVERE, "Invalid configuration:", e.getMessage());

    } catch (UnsatisfiedLinkError e) {
      if (e.getMessage().contains("libgmpxx.so.4")) {
        logger.log(Level.SEVERE, "Error: The GNU Multiprecision arithmetic library is required, but missing on this system!\n"
            + "Please install libgmpxx.so.4 and try again.\n"
            + "On Ubuntu you need to install the package 'libgmpxx4ldbl'.");
      } else {
        logger.logException(Level.SEVERE, e, null);
      }

    } catch (InterruptedException e) {
      // CPAchecker must exit because it was asked to
      // we return normally instead of propagating the exception
      // so we can return the partial result we have so far

    } catch (CPAException e) {
      logger.logException(Level.SEVERE, e, null);
    } catch (EmptyCFAException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return new CPAcheckerResult(result, initalReachedSets[0], stats);
  }





  // analysis for a single program
  public CPAcheckerResult run(String filename) {

    logger.log(Level.INFO, "CPAchecker started");

    MainCPAStatistics stats = null;
    ReachedSet reached = null;
    Result result = Result.UNKNOWN;

    if(options.useRestartingAlgorithm){
      logger.log(Level.INFO, "Using Restarting Algorithm");

      try {
        stats = new MainCPAStatistics(config, logger);

        // create parser, cpa, algorithm
        stats.creationTime.start();

        CFACreator cfaCreator = new CFACreator(config, logger);
        stats.setCFACreator(cfaCreator);

        // create CFA
        cfaCreator.parseFileAndCreateCFA(filename);

        if (cfaCreator.getFunctions().isEmpty()) {
          // empty program, do nothing
          return new CPAcheckerResult(Result.UNKNOWN, null, null);
        }

        Algorithm restartAlgorithm = createRestartAlgorithm(config, stats, cfaCreator, filename);

        Set<String> unusedProperties = config.getUnusedProperties();
        if (!unusedProperties.isEmpty()) {
          logger.log(Level.WARNING, "The following configuration options were specified but are not used:\n",
              Joiner.on("\n ").join(unusedProperties), "\n");
        }

        stats.creationTime.stop();

        stopIfNecessary();

        ((StatisticsProvider)restartAlgorithm).collectStatistics(stats.getSubStatistics());

        // register management interface for CPAchecker
        CPAcheckerBean mxbean = new CPAcheckerBean(reached, logger);
        try {
          result = runRestartAlgorithm((RestartAlgorithm)restartAlgorithm, stats);
          reached = ((RestartAlgorithm)restartAlgorithm).getUsedReachedSet();
        } finally {
          // unregister management interface for CPAchecker
          mxbean.unregister();
        }

      } catch (IOException e) {
        logger.log(Level.SEVERE, "Could not read file", filename,
            (e.getMessage() != null ? "(" + e.getMessage() + ")" : ""));

      } catch (ParserException e) {
        // only log message, not whole exception because this is a C problem,
        // not a CPAchecker problem
        logger.log(Level.SEVERE, Throwables.getRootCause(e).getMessage());
        logger.log(Level.INFO, "Make sure that the code was preprocessed using Cil (HowTo.txt).\n"
            + "If the error still occurs, please send this error message together with the input file to cpachecker-users@sosy-lab.org.");

      } catch (InvalidConfigurationException e) {
        logger.log(Level.SEVERE, "Invalid configuration:", e.getMessage());

      } catch (UnsatisfiedLinkError e) {
        if (e.getMessage().contains("libgmpxx.so.4")) {
          logger.log(Level.SEVERE, "Error: The GNU Multiprecision arithmetic library is required, but missing on this system!\n"
              + "Please install libgmpxx.so.4 and try again.\n"
              + "On Ubuntu you need to install the package 'libgmpxx4ldbl'.");
        } else {
          logger.logException(Level.SEVERE, e, null);
        }

      } catch (InterruptedException e) {
        // CPAchecker must exit because it was asked to
        // we return normally instead of propagating the exception
        // so we can return the partial result we have so far

      } catch (CPAException e) {
        logger.logException(Level.SEVERE, e, null);
      }

    }

    else{

      try {
        stats = new MainCPAStatistics(config, logger);

        // create parser, cpa, algorithm
        stats.creationTime.start();

        CFACreator cfaCreator = new CFACreator(config, logger);
        stats.setCFACreator(cfaCreator);

        if(options.runCBMCasExternalTool){
          Algorithm algorithm = createExternalCBMCAlgorithm(filename, config);
          reached = new ReachedSetFactory(config).create();
          result = runAlgorithm(algorithm, reached, stats);
          return new CPAcheckerResult(result, reached, stats);
        }
        ConfigurableProgramAnalysis cpa = createCPA(stats);
        Algorithm algorithm = createAlgorithm(cpa, stats);
        Set<String> unusedProperties = config.getUnusedProperties();
        if (!unusedProperties.isEmpty()) {
          logger.log(Level.WARNING, "The following configuration options were specified but are not used:\n",
              Joiner.on("\n ").join(unusedProperties), "\n");
        }

        stats.creationTime.stop();

        stopIfNecessary();

        // create CFA
        cfaCreator.parseFileAndCreateCFA(filename);

        if (cfaCreator.getFunctions().isEmpty()) {
          // empty program, do nothing
          return new CPAcheckerResult(Result.UNKNOWN, null, null);
        }

        reached = createInitialReachedSet(cpa, cfaCreator.getMainFunction());

        stopIfNecessary();

        // register management interface for CPAchecker
        CPAcheckerBean mxbean = new CPAcheckerBean(reached, logger);
        try {

          result = runAlgorithm(algorithm, reached, stats);

        } finally {
          // unregister management interface for CPAchecker
          mxbean.unregister();
        }

      } catch (IOException e) {
        logger.log(Level.SEVERE, "Could not read file", filename,
            (e.getMessage() != null ? "(" + e.getMessage() + ")" : ""));

      } catch (ParserException e) {
        // only log message, not whole exception because this is a C problem,
        // not a CPAchecker problem
        logger.log(Level.SEVERE, Throwables.getRootCause(e).getMessage());
        logger.log(Level.INFO, "Make sure that the code was preprocessed using Cil (HowTo.txt).\n"
            + "If the error still occurs, please send this error message together with the input file to cpachecker-users@sosy-lab.org.");

      } catch (InvalidConfigurationException e) {
        logger.log(Level.SEVERE, "Invalid configuration:", e.getMessage());

      } catch (UnsatisfiedLinkError e) {
        if (e.getMessage().contains("libgmpxx.so.4")) {
          logger.log(Level.SEVERE, "Error: The GNU Multiprecision arithmetic library is required, but missing on this system!\n"
              + "Please install libgmpxx.so.4 and try again.\n"
              + "On Ubuntu you need to install the package 'libgmpxx4ldbl'.");
        } else {
          logger.logException(Level.SEVERE, e, null);
        }

      } catch (InterruptedException e) {
        // CPAchecker must exit because it was asked to
        // we return normally instead of propagating the exception
        // so we can return the partial result we have so far

      } catch (CPAException e) {
        logger.logException(Level.SEVERE, e, null);
      }
    }
    return new CPAcheckerResult(result, reached, stats);
  }

  private Algorithm createExternalCBMCAlgorithm(String fileName, Configuration pConfig) {
    ExternalCBMCAlgorithm cbmcAlgorithm = null;
    try {
      cbmcAlgorithm = new ExternalCBMCAlgorithm(fileName, pConfig, logger);
    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    } catch (CPAException e) {
      e.printStackTrace();
    }
    return cbmcAlgorithm;
  }

  private Result runAlgorithm(final Algorithm algorithm,
      final ReachedSet reached,
      final MainCPAStatistics stats) throws CPAException, InterruptedException {

    logger.log(Level.INFO, "Starting analysis ...");
    stats.analysisTime.start();

    boolean sound = true;
    do {
      sound &= algorithm.run(reached);

      // either run only once (if stopAfterError == true)
      // or until the waitlist is empty
    } while (!options.stopAfterError && reached.hasWaitingElement());

    logger.log(Level.INFO, "Stopping analysis ...");
    stats.analysisTime.stop();
    stats.programTime.stop();

    if (Iterables.any(reached, AbstractElements.IS_TARGET_ELEMENT)) {
      return Result.UNSAFE;
    }

    if (reached.hasWaitingElement()) {
      logger.log(Level.WARNING, "Analysis not completed: there are still elements to be processed.");
      return Result.UNKNOWN;
    }

    if (!sound) {
      logger.log(Level.WARNING, "Analysis incomplete: no errors found, but not everything could be checked.");
      return Result.UNKNOWN;
    }

    return Result.SAFE;
  }

  /**
   * Runs concurrent analysis starting from thread 0.
   * @param pAlgorithm
   * @param reached
   * @param stats
   * @return
   * @throws CPAException
   * @throws InterruptedException
   */
  private Result runRelyGuaranteeAlgorithm(final ConcurrentAlgorithm pAlgorithm, final ReachedSet[] reached, final MainCPAStatistics stats) throws CPAException, InterruptedException {

    logger.log(Level.INFO, "Starting analysis ...");
    stats.analysisTime.start();

    int errorThr = pAlgorithm.run(reached, 0);

    logger.log(Level.INFO, "Stopping analysis ...");
    stats.analysisTime.stop();
    stats.programTime.stop();

    if (errorThr == -1){
      return Result.SAFE;
    } else {
      return Result.UNSAFE;
    }
  }

  private Result runRestartAlgorithm(final RestartAlgorithm restartAlgorithm,
      final MainCPAStatistics stats) throws CPAException, InterruptedException {

    logger.log(Level.INFO, "Starting analysis ...");
    stats.analysisTime.start();

    boolean sound = true;
    do {
      sound &= restartAlgorithm.run(null);

      // either run only once (if stopAfterError == true)
    } while (!options.stopAfterError);

    logger.log(Level.INFO, "Stopping analysis ...");
    stats.analysisTime.stop();
    stats.programTime.stop();

    return restartAlgorithm.getResult();
  }

  private ConfigurableProgramAnalysis createCPA(MainCPAStatistics stats) throws InvalidConfigurationException, CPAException {
    logger.log(Level.FINE, "Creating CPAs");

    CPABuilder builder = new CPABuilder(config, logger, reachedSetFactory);
    ConfigurableProgramAnalysis cpa = builder.buildCPAs();

    if (cpa instanceof StatisticsProvider) {
      ((StatisticsProvider)cpa).collectStatistics(stats.getSubStatistics());
    }
    return cpa;
  }



  private Algorithm createAlgorithm(
      final ConfigurableProgramAnalysis cpa, final MainCPAStatistics stats)
  throws InvalidConfigurationException, CPAException {
    logger.log(Level.FINE, "Creating algorithms");

    Algorithm algorithm = new CPAAlgorithm(cpa, logger);
    if (options.useRefinement) {
      algorithm = new CEGARAlgorithm(algorithm, config, logger);
    }
    if (options.useBMC) {
      algorithm = new BMCAlgorithm(algorithm, config, logger, reachedSetFactory);
    }
    if (options.useCBMC) {
      algorithm = new CounterexampleCheckAlgorithm(algorithm, config, logger);
    }

    if (options.useAssumptionCollector) {
      algorithm = new AssumptionCollectorAlgorithm(algorithm, config, logger);
    }

    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(stats.getSubStatistics());
    }
    return algorithm;
  }

  private Algorithm createRestartAlgorithm(Configuration config, MainCPAStatistics stats, CFACreator cfaCreator, String filename) {
    Algorithm restartAlgorithm = null;

    try {
      restartAlgorithm = new RestartAlgorithm(config, logger, cfaCreator, filename);
    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    } catch (CPAException e) {
      e.printStackTrace();
    }

    Preconditions.checkNotNull(restartAlgorithm);
    return restartAlgorithm;
  }


  private ReachedSet createInitialReachedSet(
      final ConfigurableProgramAnalysis cpa,
      final CFAFunctionDefinitionNode mainFunction) {
    logger.log(Level.FINE, "Creating initial reached set");

    AbstractElement initialElement = cpa.getInitialElement(mainFunction);
    Precision initialPrecision = cpa.getInitialPrecision(mainFunction);

    ReachedSet reached = reachedSetFactory.create();
    reached.add(initialElement, initialPrecision);
    return reached;
  }

  // returns main functions and CFA from the given file names
  private Pair<CFAFunctionDefinitionNode[], RGCFA[]> getMainFunctionsAndCfas(String filename) throws InvalidConfigurationException, IOException, ParserException, InterruptedException, EmptyCFAException, UnrecognizedCFAEdgeException{

    RGCFACreator creator = new RGCFACreator(config, logger);
    creator.parseFileAndCreateCFA(filename);

    List<CFAFunctionDefinitionNode> funs = creator.getMainFunctions();
    CFAFunctionDefinitionNode[] funsArr = funs.toArray(new CFAFunctionDefinitionNode[funs.size()]);
    List<CFA> cfas = creator.getCfas();
    List<CFANode> startNodes = creator.getStartNodes();
    RGCFA[] rgCfas = new RGCFA[cfas.size()];
    for (int i=0; i<cfas.size(); i++){
      RGCFA rgCfa = new RGCFA(cfas.get(i),startNodes.get(i),funs.get(i), i);
      rgCfas[i] = rgCfa;
    }

    return Pair.of(funsArr, rgCfas);
  }

  // returns main functions and CFA from the given file names
  /*private Pair<CFAFunctionDefinitionNode[], CFA[]> getMainFunctionsAndCFAS(String[] pFilenames) throws InvalidConfigurationException, IOException, ParserException, InterruptedException, EmptyCFAException{
    int threadNo = pFilenames.length;
    CFACreator[] cfaCreators = new CFACreator[threadNo];
    CFA cfas[] = new CFA[threadNo];
    CFAFunctionDefinitionNode mainFunctions[] = new CFAFunctionDefinitionNode[threadNo];

    CFACreator creator;
    for(int i=0; i<threadNo; i++){
      String filename = pFilenames[i];
      creator = new CFACreator(config, logger);
      creator.parseFileAndCreateCFA(filename);
      if (creator.getFunctions().isEmpty()) {
        // empty program, do nothing
        throw new EmptyCFAException();
      }
      mainFunctions[i] = creator.getMainFunction();
      cfas[i] = creator.getCFA();
    }

    return new Pair<CFAFunctionDefinitionNode[], CFA[]>(mainFunctions, cfas);
  }*/


}
