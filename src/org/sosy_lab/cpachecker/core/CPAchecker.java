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
package org.sosy_lab.cpachecker.core;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectionAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CBMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.CallstackSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.TopologicallySortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.exceptions.CFAGenerationRuntimeException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ForceStopCPAException;
import org.sosy_lab.cpachecker.util.CParser;
import org.sosy_lab.cpachecker.util.CParser.Dialect;

import com.google.common.base.Joiner;

public class CPAchecker {

  private static enum ReachedSetType {
    NORMAL, LOCATIONMAPPED, PARTITIONED
  }
  
  public static interface CPAcheckerMXBean {
    public int getReachedSetSize();
    
    public void stop();
  }
  
  private class CPAcheckerBean implements CPAcheckerMXBean {
    private final ReachedSet reached;
    
    public CPAcheckerBean(ReachedSet pReached) {
      reached = pReached;
    }

    @Override
    public int getReachedSetSize() {
      return reached.size();
    }
    
    @Override
    public void stop() {
      CPAchecker.requireStopAsap();
    }
    
  }
  
  @Options
  private static class CPAcheckerOptions {

    @Option(name="parser.dialect")
    Dialect parserDialect = Dialect.GNUC;

    // algorithm options

    @Option(name="analysis.traversal")
    Waitlist.TraversalMethod traversalMethod = Waitlist.TraversalMethod.DFS;

    @Option(name="analysis.traversal.useCallstack")
    boolean useCallstack = false;

    @Option(name="analysis.traversal.useTopsort")
    boolean useTopSort = false;

    @Option(name="analysis.reachedSet")
    ReachedSetType reachedSet = ReachedSetType.PARTITIONED;
    
    @Option(name="analysis.useAssumptionCollector")
    boolean useAssumptionCollector = false;

    @Option(name="analysis.useRefinement")
    boolean useRefinement = false;

    @Option(name="analysis.useCBMC")
    boolean useCBMC = false;

  }

  private final LogManager logger;
  private final Configuration config;
  private final CPAcheckerOptions options;

  private static volatile boolean requireStopAsap = false;

  /**
   * This method will throw an exception if the user has requested CPAchecker to
   * stop immediately. This exception should not be caught by the caller.
   */
  public static void stopIfNecessary() throws ForceStopCPAException {
    if (requireStopAsap) {
      throw new ForceStopCPAException();
    }
  }

  /**
   * This will request all running CPAchecker instances to stop as soon as possible.
   */
  public static void requireStopAsap() {
    requireStopAsap = true;
  }

  public CPAchecker(Configuration pConfiguration, LogManager pLogManager) throws InvalidConfigurationException {
    config = pConfiguration;
    logger = pLogManager;

    options = new CPAcheckerOptions();
    config.inject(options);
  }

  public CPAcheckerResult run(String filename) {

    logger.log(Level.INFO, "CPAchecker started");

    MainCPAStatistics stats = null;
    ReachedSet reached = null;
    Result result = Result.UNKNOWN;

    try {
      stats = new MainCPAStatistics(config, logger);

      // parse code file
      IASTTranslationUnit ast = parse(filename, stats);

      // create CFA
      stats.cfaCreationTime.start();
      CFACreator cfaCreator = new CFACreator(config, logger);
      cfaCreator.createCFA(ast);
      Map<String, CFAFunctionDefinitionNode> cfas = cfaCreator.getFunctions();
      CFAFunctionDefinitionNode mainFunction = cfaCreator.getMainFunction();
      stats.cfaCreationTime.stop();
      
      if (cfas.isEmpty()) {
        // empty program, do nothing
        return new CPAcheckerResult(Result.UNKNOWN, null, null);
      }

      stats.cpaCreationTime.start();
      ConfigurableProgramAnalysis cpa = createCPA(stats);

      Algorithm algorithm = createAlgorithm(cfas, cpa, stats);

      Set<String> unusedProperties = config.getUnusedProperties();
      if (!unusedProperties.isEmpty()) {
        logger.log(Level.WARNING, "The following configuration options were specified but are not used:\n",
            Joiner.on("\n ").join(unusedProperties), "\n");
      }

      if (!requireStopAsap) {
        reached = createInitialReachedSet(cpa, mainFunction);

        // register management interface for CPAchecker
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = null;
        try {
          name = new ObjectName("org.sosy_lab.cpachecker:type=CPAchecker");
          CPAcheckerMXBean mxbean = new CPAcheckerBean(reached);
          mbs.registerMBean(mxbean, name);
        } catch (JMException e) {
          logger.logException(Level.WARNING, e, "Error during registration of management interface");
        }

        stats.cpaCreationTime.stop();
        
        result = runAlgorithm(algorithm, reached, stats);
        
        // unregister management interface for CPAchecker
        if (name != null) {
          try {
            mbs.unregisterMBean(name);
          } catch (JMException e) {
            logger.logException(Level.WARNING, e, "Error during unregistration of management interface");
          }
        }
      }

    } catch (IOException e) {
      logger.log(Level.SEVERE, "Could not read file", filename,
          (e.getMessage() != null ? "(" + e.getMessage() + ")" : ""));

    } catch (CoreException e) {
      logger.logException(Level.SEVERE, e, "Exception thrown by Eclipse C parser");

    } catch (CFAGenerationRuntimeException e) {
      // only log message, not whole exception because this is a C problem,
      // not a CPAchecker problem
      logger.log(Level.SEVERE, e.getMessage());
      logger.log(Level.INFO, "Make sure that the code was preprocessed using Cil (HowTo.txt).\n"
          + "If the error still occurs, please send this error message together with the input file to cpachecker-users@sosy-lab.org.");

    } catch (InvalidConfigurationException e) {
      logger.log(Level.SEVERE, "Invalid configuration:", e.getMessage());

    } catch (ForceStopCPAException e) {
      // CPA must exit because it was asked to
      logger.log(Level.FINE, "ForceStopCPAException caught at top level: CPAchecker has stopped forcefully, but cleanly");
    } catch (CPAException e) {
      logger.logException(Level.SEVERE, e, null);
    }

    return new CPAcheckerResult(result, reached, stats);
  }

  /**
   * Parse the content of a file into an AST with the Eclipse CDT parser.
   *
   * @param fileName  The file to parse.
   * @return The AST.
   * @throws IOException If file cannot be read.
   * @throws CoreException If Eclipse C parser throws an exception.
   */
  private IASTTranslationUnit parse(String filename, final MainCPAStatistics stats) throws IOException, CoreException {
    logger.log(Level.FINE, "Starting parsing of file");
    stats.parseTime.start();

    IASTTranslationUnit ast = CParser.parseFile(filename, options.parserDialect);
    
    stats.parseTime.stop();
    logger.log(Level.FINE, "Parser Finished");
    return ast;
  }

  private Result runAlgorithm(final Algorithm algorithm,
          final ReachedSet reached,
          final MainCPAStatistics stats) throws CPAException {

    logger.log(Level.INFO, "Starting analysis...");
    stats.analysisTime.start();

    algorithm.run(reached);

    stats.analysisTime.stop();
    stats.programTime.stop();
    logger.log(Level.INFO, "Analysis finished.");

    for (AbstractElement reachedElement : reached) {
      if ((reachedElement instanceof Targetable)
          && ((Targetable)reachedElement).isTarget()) {
        return Result.UNSAFE;
      }
    }

    return Result.SAFE;
  }

  private ConfigurableProgramAnalysis createCPA(MainCPAStatistics stats) throws InvalidConfigurationException, CPAException {
    logger.log(Level.FINE, "Creating CPAs");

    CPABuilder builder = new CPABuilder(config, logger);
    ConfigurableProgramAnalysis cpa = builder.buildCPAs();

    if (cpa instanceof StatisticsProvider) {
      ((StatisticsProvider)cpa).collectStatistics(stats.getSubStatistics());
    }
    return cpa;
  }

  private Algorithm createAlgorithm(final Map<String, CFAFunctionDefinitionNode> cfas,
      final ConfigurableProgramAnalysis cpa, MainCPAStatistics stats) throws InvalidConfigurationException, CPAException {
    logger.log(Level.FINE, "Creating algorithms");

    Algorithm algorithm = new CPAAlgorithm(cpa, logger);

    if (options.useRefinement) {
      algorithm = new CEGARAlgorithm(algorithm, config, logger);
    }

    if (options.useAssumptionCollector) {
      algorithm = new AssumptionCollectionAlgorithm(algorithm, config, logger);
    }

    if (options.useCBMC) {
      algorithm = new CBMCAlgorithm(cfas, algorithm, logger);
    }

    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(stats.getSubStatistics());
    }
    return algorithm;
  }


  private ReachedSet createInitialReachedSet(
      final ConfigurableProgramAnalysis cpa,
      final CFAFunctionDefinitionNode mainFunction) {
    logger.log(Level.FINE, "Creating initial reached set");

    AbstractElement initialElement = cpa.getInitialElement(mainFunction);
    Precision initialPrecision = cpa.getInitialPrecision(mainFunction);
    
    WaitlistFactory waitlistFactory = options.traversalMethod;
    if (options.useTopSort) {
      waitlistFactory = TopologicallySortedWaitlist.factory(waitlistFactory);
    }
    if (options.useCallstack) {
      waitlistFactory = CallstackSortedWaitlist.factory(waitlistFactory);
    }
    
    ReachedSet reached;
    switch (options.reachedSet) {
    case PARTITIONED:
      reached = new PartitionedReachedSet(waitlistFactory);
      break;
    case LOCATIONMAPPED:
      reached = new LocationMappedReachedSet(waitlistFactory);
      break;
    case NORMAL:
    default:
      reached = new ReachedSet(waitlistFactory);
    }

    reached.add(initialElement, initialPrecision);
    return reached;
  }
}
