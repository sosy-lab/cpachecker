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
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
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
import org.sosy_lab.cpachecker.exceptions.CFAGenerationRuntimeException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ForceStopCPAException;
import org.sosy_lab.cpachecker.util.CParser;
import org.sosy_lab.cpachecker.util.CParser.Dialect;

import com.google.common.base.Joiner;

public class CPAchecker {

  @Options
  private static class CPAcheckerOptions {

    @Option(name="parser.dialect")
    Dialect parserDialect = Dialect.GNUC;

    // algorithm options

    @Option(name="analysis.traversal")
    ReachedElements.TraversalMethod traversalMethod = ReachedElements.TraversalMethod.DFS;

    @Option(name="cpa.useSpecializedReachedSet")
    boolean locationMappedReachedSet = true;

    @Option(name="analysis.useAssumptionCollector")
    boolean useAssumptionCollector = false;

    @Option(name="analysis.useRefinement")
    boolean useRefinement = false;

    @Option(name="analysis.useCBMC")
    boolean useCBMC = false;

    @Option(name="analysis.stopAfterError")
    boolean stopAfterError = true;

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

  protected Configuration getConfiguration() {
    return config;
  }

  protected LogManager getLogger() {
    return logger;
  }
  
  public CPAcheckerResult run(String filename) {

    logger.log(Level.FINE, "Analysis Started");

    MainCPAStatistics stats = null;
    ReachedElements reached = null;
    Result result = Result.UNKNOWN;

    try {
      // parse code file
      IASTTranslationUnit ast = parse(filename);

      stats = new MainCPAStatistics(getConfiguration(), logger);

      // start measuring time
      stats.startProgramTimer();

      // create CFA
      CFACreator cfaCreator = new CFACreator(getConfiguration(), logger);
      Pair<Map<String, CFAFunctionDefinitionNode>, CFAFunctionDefinitionNode> cfa = cfaCreator.createCFA(ast);
      if (cfa == null) {
        // empty program, do nothing
        return new CPAcheckerResult(Result.UNKNOWN, null, null);
      }

      Map<String, CFAFunctionDefinitionNode> cfas = cfa.getFirst();
      CFAFunctionDefinitionNode mainFunction = cfa.getSecond();

      ConfigurableProgramAnalysis cpa = createCPA(stats);

      Algorithm algorithm = createAlgorithm(cfas, cpa, stats);

      Set<String> unusedProperties = getConfiguration().getUnusedProperties();
      if (!unusedProperties.isEmpty()) {
        logger.log(Level.WARNING, "The following configuration options were specified but are not used:\n",
            Joiner.on("\n ").join(unusedProperties), "\n");
      }

      if (!requireStopAsap) {
        reached = createInitialReachedSet(cpa, mainFunction);

        result = runAlgorithm(algorithm, reached, stats);
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
  protected IASTTranslationUnit parse(String filename) throws IOException, CoreException {
    logger.log(Level.FINE, "Starting parsing of file");
    IASTTranslationUnit ast = CParser.parseFile(filename, options.parserDialect);
    logger.log(Level.FINE, "Parser Finished");
    return ast;
  }

  private Result runAlgorithm(final Algorithm algorithm,
          final ReachedElements reached,
          final MainCPAStatistics stats) throws CPAException {

    logger.log(Level.INFO, "Starting analysis...");
    stats.startAnalysisTimer();

    do {
      algorithm.run(reached);
      
      // either run only once (if stopAfterError == true)
      // or until the waitlist is empty
    } while (!options.stopAfterError && reached.hasWaitingElement());

    stats.stopAnalysisTimer();
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

    CPABuilder builder = new CPABuilder(getConfiguration(), logger);
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
      algorithm = new CEGARAlgorithm(algorithm, getConfiguration(), logger);
    }

    if (options.useAssumptionCollector) {
      algorithm = new AssumptionCollectionAlgorithm(algorithm, getConfiguration(), logger);
    }

    if (options.useCBMC) {
      algorithm = new CBMCAlgorithm(cfas, algorithm, logger);
    }

    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(stats.getSubStatistics());
    }
    return algorithm;
  }


  private ReachedElements createInitialReachedSet(
      final ConfigurableProgramAnalysis cpa,
      final CFAFunctionDefinitionNode mainFunction) {
    logger.log(Level.FINE, "Creating initial reached set");

    AbstractElement initialElement = cpa.getInitialElement(mainFunction);
    Precision initialPrecision = cpa.getInitialPrecision(mainFunction);
    ReachedElements reached = new ReachedElements(options.traversalMethod, options.locationMappedReachedSet);
    reached.add(initialElement, initialPrecision);
    return reached;
  }
}
