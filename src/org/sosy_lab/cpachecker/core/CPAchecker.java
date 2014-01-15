/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.core.ShutdownNotifier.interruptCurrentThreadOnShutdown;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.AbstractMBean;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.ExternalCBMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.impact.ImpactAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.ViolatedProperty;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.io.Resources;

@Options(prefix="analysis")
public class CPAchecker {

  public static interface CPAcheckerMXBean {
    public int getReachedSetSize();

    public void stop();
  }

  private static class CPAcheckerBean extends AbstractMBean implements CPAcheckerMXBean {

    private final ReachedSet reached;
    private final ShutdownNotifier shutdownNotifier;

    public CPAcheckerBean(ReachedSet pReached, LogManager logger, ShutdownNotifier pShutdownNotifier) {
      super("org.sosy_lab.cpachecker:type=CPAchecker", logger);
      reached = pReached;
      shutdownNotifier = pShutdownNotifier;
      register();
    }

    @Override
    public int getReachedSetSize() {
      return reached.size();
    }

    @Override
    public void stop() {
      shutdownNotifier.requestShutdown("A stop request was received via the JMX interface.");
    }

  }

  @Option(description="stop after the first error has been found")
  private boolean stopAfterError = true;

  @Option(name="disable",
      description="stop CPAchecker after startup (internal option, not intended for users)")
  private boolean disableAnalysis = false;

  @Option(name="algorithm.CBMC",
      description="use CBMC as an external tool from CPAchecker")
  private boolean runCBMCasExternalTool = false;

  @Option(description="Do not report unknown if analysis terminated, report true (UNSOUND!).")
  private boolean unknownAsTrue = false;

  private final LogManager logger;
  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private final CoreComponentsFactory factory;

  // The content of this String is read from a file that is created by the
  // ant task "init".
  // To change the version, update the property in build.xml.
  private static final String version;
  static {
    String v = "(unknown version)";
    try {
      URL url = CPAchecker.class.getClassLoader().getResource("org/sosy_lab/cpachecker/VERSION.txt");
      if (url != null) {
        String content = Resources.toString(url, Charsets.US_ASCII).trim();
        if (content.matches("[a-zA-Z0-9 ._+:-]+")) {
          v = content;
        }
      }
    } catch (IOException e) {
      // Ignore exception, no better idea what to do here.
    }
    version = v;
  }

  public static String getVersion() {
    return version;
  }

  public CPAchecker(Configuration pConfiguration, LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    config = pConfiguration;
    logger = pLogManager;
    shutdownNotifier = pShutdownNotifier;

    config.inject(this);
    factory = new CoreComponentsFactory(pConfiguration, pLogManager, shutdownNotifier);
  }

  public CPAcheckerResult run(String programDenotation) {

    logger.log(Level.INFO, "CPAchecker", getVersion(), "started");

    MainCPAStatistics stats = null;
    ReachedSet reached = null;
    Result result = Result.NOT_YET_STARTED;

    final ShutdownRequestListener interruptThreadOnShutdown = interruptCurrentThreadOnShutdown();
    shutdownNotifier.register(interruptThreadOnShutdown);

    try {
      stats = new MainCPAStatistics(config, logger, programDenotation);

      // create reached set, cpa, algorithm
      stats.creationTime.start();
      reached = factory.createReachedSet();

      Algorithm algorithm;

      if (runCBMCasExternalTool) {

        checkIfOneValidFile(programDenotation);
        algorithm = new ExternalCBMCAlgorithm(programDenotation, config, logger);

      } else {
        CFA cfa = parse(programDenotation, stats);
        GlobalInfo.getInstance().storeCFA(cfa);
        shutdownNotifier.shutdownIfNecessary();

        ConfigurableProgramAnalysis cpa = factory.createCPA(cfa, stats);

        algorithm = factory.createAlgorithm(cpa, programDenotation, cfa, stats);

        if (algorithm instanceof ImpactAlgorithm) {
          ImpactAlgorithm mcmillan = (ImpactAlgorithm)algorithm;
          reached.add(mcmillan.getInitialState(cfa.getMainFunction()), mcmillan.getInitialPrecision(cfa.getMainFunction()));
        } else {
          initializeReachedSet(reached, cpa, cfa.getMainFunction());
        }
      }

      printConfigurationWarnings();

      stats.creationTime.stop();
      shutdownNotifier.shutdownIfNecessary();
      // now everything necessary has been instantiated

      if (disableAnalysis) {
        return new CPAcheckerResult(Result.NOT_YET_STARTED, null, stats);
      }

      // run analysis
      result = Result.UNKNOWN; // set to unknown so that the result is correct in case of exception

      boolean isComplete = runAlgorithm(algorithm, reached, stats);

      result = analyzeResult(reached, isComplete);
      if (unknownAsTrue && result == Result.UNKNOWN) {
        result = Result.TRUE;
      }

    } catch (IOException e) {
      logger.logUserException(Level.SEVERE, e, "Could not read file");

    } catch (ParserException e) {
      logger.logUserException(Level.SEVERE, e, "Parsing failed");
      StringBuilder msg = new StringBuilder();
      msg.append("Please make sure that the code can be compiled by a compiler.\n");
      if (e.getLanguage() == Language.C) {
        msg.append("If the code was not preprocessed, please use a C preprocessor\nor specify the -preprocess command-line argument.\n");
      }
      msg.append("If the error still occurs, please send this error message\ntogether with the input file to cpachecker-users@sosy-lab.org.\n");
      logger.log(Level.INFO, msg);

    } catch (InvalidConfigurationException e) {
      logger.logUserException(Level.SEVERE, e, "Invalid configuration");

    } catch (InterruptedException e) {
      // CPAchecker must exit because it was asked to
      // we return normally instead of propagating the exception
      // so we can return the partial result we have so far
      if (!Strings.isNullOrEmpty(e.getMessage())) {
        logger.logUserException(Level.WARNING, e, "Analysis stopped");
      }

    } catch (CPAException e) {
      logger.logUserException(Level.SEVERE, e, null);

    } finally {
      shutdownNotifier.unregister(interruptThreadOnShutdown);
    }
    return new CPAcheckerResult(result, reached, stats);
  }

  private void checkIfOneValidFile(String fileDenotation) throws InvalidConfigurationException {
    if (!denotesOneFile(fileDenotation)) {
      throw new InvalidConfigurationException(
        "Exactly one code file has to be given.");
    }

    Path file = Paths.get(fileDenotation);

    try {
      Files.checkReadableFile(file);
    } catch (FileNotFoundException e) {
      throw new InvalidConfigurationException(e.getMessage());
    }
  }

  private boolean denotesOneFile(String programDenotation) {
    return !programDenotation.contains(",");
  }

  private CFA parse(String fileNamesCommaSeparated, MainCPAStatistics stats) throws InvalidConfigurationException, IOException,
      ParserException, InterruptedException {
    // parse file and create CFA
    CFACreator cfaCreator = new CFACreator(config, logger, shutdownNotifier);
    stats.setCFACreator(cfaCreator);

    Splitter commaSplitter = Splitter.on(',').omitEmptyStrings().trimResults();
    CFA cfa = cfaCreator.parseFileAndCreateCFA(commaSplitter.splitToList(fileNamesCommaSeparated));
    stats.setCFA(cfa);
    return cfa;
  }

  private void printConfigurationWarnings() {
    Set<String> unusedProperties = config.getUnusedProperties();
    if (!unusedProperties.isEmpty()) {
      logger.log(Level.WARNING, "The following configuration options were specified but are not used:\n",
          Joiner.on("\n ").join(unusedProperties), "\n");
    }
    Set<String> deprecatedProperties = config.getDeprecatedProperties();
    if (!deprecatedProperties.isEmpty()) {
      logger.log(Level.WARNING, "The following options are deprecated and will be removed in the future:\n",
          Joiner.on("\n ").join(deprecatedProperties), "\n");
    }
  }

  private boolean runAlgorithm(final Algorithm algorithm,
      final ReachedSet reached,
      final MainCPAStatistics stats) throws CPAException, InterruptedException {

    logger.log(Level.INFO, "Starting analysis ...");

    boolean isComplete = true;

    // register management interface for CPAchecker
    CPAcheckerBean mxbean = new CPAcheckerBean(reached, logger, shutdownNotifier);

    stats.startAnalysisTimer();
    try {

      do {
        isComplete &= algorithm.run(reached);

        // either run only once (if stopAfterError == true)
        // or until the waitlist is empty
      } while (!stopAfterError && reached.hasWaitingState());

      logger.log(Level.INFO, "Stopping analysis ...");
      return isComplete;

    } finally {
      stats.stopAnalysisTimer();

      // unregister management interface for CPAchecker
      mxbean.unregister();
    }
  }

  private Result analyzeResult(final ReachedSet reached, boolean isComplete) {
    for (AbstractState s : from(reached).filter(IS_TARGET_STATE)) {
      ViolatedProperty property = ((Targetable)s).getViolatedProperty();
      if (property != ViolatedProperty.OTHER) {
        logger.log(Level.WARNING, "Found violation of property", property);
      }
      return Result.FALSE;
    }

    if (reached.hasWaitingState()) {
      logger.log(Level.WARNING, "Analysis not completed: there are still states to be processed.");
      return Result.UNKNOWN;
    }

    if (!isComplete) {
      logger.log(Level.WARNING, "Analysis incomplete: no errors found, but not everything could be checked.");
      return Result.UNKNOWN;
    }

    return Result.TRUE;
  }

  private void initializeReachedSet(
      final ReachedSet reached,
      final ConfigurableProgramAnalysis cpa,
      final FunctionEntryNode mainFunction) {
    logger.log(Level.FINE, "Creating initial reached set");

    AbstractState initialState = cpa.getInitialState(mainFunction);
    Precision initialPrecision = cpa.getInitialPrecision(mainFunction);

    reached.add(initialState, initialPrecision);
  }
}
