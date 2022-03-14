// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Splitter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;

/** abstract algorithm for executing other nested algorithms. */
public abstract class NestingAlgorithm implements Algorithm, StatisticsProvider {

  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;
  protected final Configuration globalConfig;
  protected final Specification specification;

  protected NestingAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification) {
    shutdownNotifier = Objects.requireNonNull(pShutdownNotifier);
    globalConfig = Objects.requireNonNull(pConfig);
    specification = Objects.requireNonNull(pSpecification);
    logger = Objects.requireNonNull(pLogger);
  }

  protected Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> createAlgorithm(
      Path singleConfigFileName,
      CFANode initialNode,
      CFA pCfa,
      ShutdownManager singleShutdownManager,
      AggregatedReachedSets aggregateReached,
      Collection<String> ignoreOptions,
      Collection<Statistics> stats)
      throws InvalidConfigurationException, CPAException, IOException, InterruptedException {

    Configuration singleConfig = buildSubConfig(singleConfigFileName, ignoreOptions);
    LogManager singleLogger = logger.withComponentName("Analysis " + singleConfigFileName);

    ResourceLimitChecker singleLimits =
        ResourceLimitChecker.fromConfiguration(singleConfig, singleLogger, singleShutdownManager);
    singleLimits.start();

    CoreComponentsFactory coreComponents =
        new CoreComponentsFactory(
            singleConfig, singleLogger, singleShutdownManager.getNotifier(), aggregateReached);
    ConfigurableProgramAnalysis cpa = coreComponents.createCPA(pCfa, specification);
    GlobalInfo.getInstance().setUpInfoFromCPA(cpa);
    Algorithm algorithm = coreComponents.createAlgorithm(cpa, pCfa, specification);
    ReachedSet reached = createInitialReachedSet(cpa, initialNode, coreComponents, singleLogger);

    if (cpa instanceof StatisticsProvider) {
      ((StatisticsProvider) cpa).collectStatistics(stats);
    }
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) algorithm).collectStatistics(stats);
    }

    return Triple.of(algorithm, cpa, reached);
  }

  private Configuration buildSubConfig(Path singleConfigFileName, Collection<String> ignoreOptions)
      throws IOException, InvalidConfigurationException {

    ConfigurationBuilder singleConfigBuilder = Configuration.builder();
    singleConfigBuilder.copyFrom(globalConfig);
    for (String ignore : ignoreOptions) {
      singleConfigBuilder.clearOption(ignore);
    }

    // TODO next line overrides existing options with options loaded from file.
    // Perhaps we want to keep some global options like 'specification'?
    singleConfigBuilder.loadFromFile(singleConfigFileName);

    Configuration singleConfig = singleConfigBuilder.build();
    checkConfigs(globalConfig, singleConfig, singleConfigFileName, logger);
    return singleConfig;
  }

  private static ReachedSet createInitialReachedSet(
      ConfigurableProgramAnalysis cpa,
      CFANode mainFunction,
      CoreComponentsFactory pFactory,
      LogManager singleLogger)
      throws InterruptedException {
    singleLogger.log(Level.FINE, "Creating initial reached set");

    AbstractState initialState =
        cpa.getInitialState(mainFunction, StateSpacePartition.getDefaultPartition());
    Precision initialPrecision =
        cpa.getInitialPrecision(mainFunction, StateSpacePartition.getDefaultPartition());

    ReachedSet reached = pFactory.createReachedSet(cpa);
    reached.add(initialState, initialPrecision);
    return reached;
  }

  static void checkConfigs(
      Configuration pGlobalConfig,
      Configuration pSingleConfig,
      Path pSingleConfigFileName,
      LogManager pLogger)
      throws InvalidConfigurationException {
    Map<String, String> global = configToMap(pGlobalConfig);
    Map<String, String> single = configToMap(pSingleConfig);
    for (Entry<String, String> entry : global.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (single.containsKey(key) && !value.equals(single.get(key))) {
        pLogger.logf(
            Level.INFO,
            "Mismatch of configuration options when loading from '%s': '%s' has two values '%s' and"
                + " '%s'. Using '%s'.",
            pSingleConfigFileName,
            key,
            value,
            single.get(key),
            single.get(key));
      }
    }

    // "cfa.*"-options of a subconfig are effectively ignored because the CFA gets only generated
    // once for the NestingAlgorithm, so we check whether all "cfa.*"-options that are set in the
    // subconfig are also present and with the same value in the global config:
    for (Entry<String, String> entry : single.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (key.startsWith("cfa.") && !(global.containsKey(key) && value.equals(global.get(key)))) {
        throw new InvalidConfigurationException(
            "CFA option of a nested sub-configuration must also be present in the outer"
                + " configuration!\n"
                + String.format(
                    "inner config: \"%s = %s\" ; outer config: \"%s = %s\" ",
                    key, value, key, global.get(key)));
      }
    }
  }

  /** get an iterable data structure from configuration options. Sadly there is no nicer way. */
  private static Map<String, String> configToMap(Configuration config) {
    Map<String, String> mp = new LinkedHashMap<>();
    for (String option : Splitter.on("\n").omitEmptyStrings().split(config.asPropertiesString())) {
      List<String> split = Splitter.on(" = ").splitToList(option);
      checkArgument(split.size() == 2, "unexpected option format: %s", option);
      mp.put(split.get(0), split.get(1));
    }
    return mp;
  }
}
