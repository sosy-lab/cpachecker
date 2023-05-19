// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAdditionalInfo;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopNeverOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithAdditionalInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithConcreteCex;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.AdditionalInfoConverter;
import org.sosy_lab.cpachecker.cpa.smg.SMGStatistics;
import org.sosy_lab.cpachecker.cpa.smg2.SMGPrecisionAdjustment.PrecAdjustmentOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGPrecisionAdjustment.PrecAdjustmentStatistics;
import org.sosy_lab.cpachecker.cpa.smg2.refiner.SMGConcreteErrorPathAllocator;
import org.sosy_lab.cpachecker.cpa.value.PredicateToValuePrecisionConverter;
import org.sosy_lab.cpachecker.cpa.value.symbolic.ConstraintsStrengthenOperator;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@Options(prefix = "cpa.smg2")
public class SMGCPA
    implements ConfigurableProgramAnalysis,
        ConfigurableProgramAnalysisWithConcreteCex,
        ConfigurableProgramAnalysisWithAdditionalInfo,
        StatisticsProvider {

  @Option(
      secure = true,
      name = "stop",
      toUppercase = true,
      values = {"SEP", "NEVER", "END_BLOCK"},
      description = "which stop operator to use for the SMGCPA")
  private String stopType = "SEP";

  @Option(
      secure = true,
      name = "merge",
      toUppercase = true,
      values = "SEP",
      description = "which merge operator to use for the SMGCPA")
  private String mergeType = "SEP";

  @Option(secure = true, description = "get an initial precision from file")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  @SuppressWarnings("unused")
  private Path initialPrecisionFile = null;

  @Option(secure = true, description = "get an initial precision from a predicate precision file")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  @SuppressWarnings("unused")
  private Path initialPredicatePrecisionFile = null;

  private final MachineModel machineModel;
  private final BlockOperator blockOperator;

  private final LogManager logger;
  private final Configuration config;
  private final CFA cfa;
  private final SMGOptions options;
  private final SMGCPAExportOptions exportOptions;
  private final PrecAdjustmentOptions precisionAdjustmentOptions;
  private final PrecAdjustmentStatistics precisionAdjustmentStatistics;
  private final ShutdownNotifier shutdownNotifier;

  private VariableTrackingPrecision precision;
  private boolean refineablePrecisionSet = false;

  private final SMGStatistics stats = new SMGStatistics();
  private final PredicateToValuePrecisionConverter predToValPrec;
  private final ConstraintsStrengthenOperator constraintsStrengthenOperator;

  private final SMGCPAStatistics statistics;

  private SMGCPA(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCfa)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    options = new SMGOptions(pConfig);

    config = pConfig;
    cfa = pCfa;
    machineModel = cfa.getMachineModel();
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    precision = initializePrecision(config, cfa);
    predToValPrec = new PredicateToValuePrecisionConverter(config, logger, pShutdownNotifier, cfa);
    constraintsStrengthenOperator = new ConstraintsStrengthenOperator(config, logger);

    statistics = new SMGCPAStatistics(this, config);
    precisionAdjustmentOptions = new PrecAdjustmentOptions(config, cfa);
    precisionAdjustmentStatistics = new PrecAdjustmentStatistics();

    blockOperator = new BlockOperator();
    pConfig.inject(blockOperator);
    blockOperator.setCFA(cfa);

    exportOptions =
        new SMGCPAExportOptions(options.getExportSMGFilePattern(), options.getExportSMGLevel());
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SMGCPA.class);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  @Override
  public AdditionalInfoConverter exportAdditionalInfoConverter() {
    return new SMGAdditionalInfoConverter();
  }

  @Override
  public CFAPathWithAdditionalInfo createExtendedInfo(ARGPath pPath) {
    return new AdditionalInfoExtractor().createExtendedInfo(pPath);
  }

  @Override
  public ConcreteStatePath createConcreteStatePath(ARGPath pPath) {
    try {
      return new SMGConcreteErrorPathAllocator(config, logger, machineModel)
          .allocateAssignmentsToPath(pPath);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return DelegateAbstractDomain.<SMGState>getInstance();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new SMGTransferRelation(
        logger, options, exportOptions, cfa, constraintsStrengthenOperator, statistics);
  }

  @Override
  public MergeOperator getMergeOperator() {
    switch (mergeType) {
      case "SEP":
        return MergeSepOperator.getInstance();
      case "JOIN":
        return new MergeJoinOperator(getAbstractDomain());
      default:
        throw new AssertionError("unknown mergetype for SMGCPA");
    }
  }

  @Override
  public StopOperator getStopOperator() {
    switch (stopType) {
        // TODO END_BLOCK
      case "NEVER":
        return StopNeverOperator.getInstance();
      case "SEP":
        return new StopSepOperator(getAbstractDomain());
      default:
        throw new AssertionError("unknown stoptype for SMGCPA");
    }
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    SMGState initState = SMGState.of(machineModel, logger, options, cfa);
    return initState;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return precision;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return new SMGPrecisionAdjustment(
        statistics, cfa, precisionAdjustmentOptions, precisionAdjustmentStatistics);
  }

  public LogManager getLogger() {
    return logger;
  }

  public void injectRefinablePrecision() {
    // replace the full precision with an empty, refinable precision
    if (initialPrecisionFile == null
        && initialPredicatePrecisionFile == null
        && !refineablePrecisionSet) {
      precision = new SMGPrecision(precision);
      refineablePrecisionSet = true;
    }
  }

  private VariableTrackingPrecision initializePrecision(Configuration pConfig, CFA pCfa)
      throws InvalidConfigurationException {
    if (initialPrecisionFile == null && initialPredicatePrecisionFile == null) {
      return VariableTrackingPrecision.createStaticPrecision(
          pConfig, pCfa.getVarClassification(), getClass());
    }

    // Initialize precision
    VariableTrackingPrecision initialPrecision =
        new SMGPrecision(
            VariableTrackingPrecision.createStaticPrecision(
                pConfig, pCfa.getVarClassification(), getClass()));

    if (initialPredicatePrecisionFile != null) {

      // convert the predicate precision to variable tracking precision and
      // refine precision with increment from the newly gained variable tracking precision
      // otherwise return empty precision if given predicate precision is empty

      initialPrecision =
          initialPrecision.withIncrement(
              predToValPrec.convertPredPrecToVariableTrackingPrec(initialPredicatePrecisionFile));
    }
    if (initialPrecisionFile != null) {
      // create precision with empty, refinable component precision
      // refine the refinable component precision with increment from file
      initialPrecision = initialPrecision.withIncrement(restoreMappingFromFile(pCfa));
    }

    return initialPrecision;
  }

  private Multimap<CFANode, MemoryLocation> restoreMappingFromFile(CFA pCfa) {
    Multimap<CFANode, MemoryLocation> mapping = HashMultimap.create();
    List<String> contents = null;
    try {
      contents = Files.readAllLines(initialPrecisionFile, Charset.defaultCharset());
    } catch (IOException e) {
      logger.logUserException(
          Level.WARNING, e, "Could not read precision from file named " + initialPrecisionFile);
      return mapping;
    }

    Map<Integer, CFANode> idToCfaNode = CFAUtils.getMappingFromNodeIDsToCFANodes(pCfa);

    CFANode location = getDefaultLocation(idToCfaNode);
    for (String currentLine : contents) {
      if (currentLine.trim().isEmpty()) {
        continue;

      } else if (currentLine.endsWith(":")) {
        String scopeSelectors = currentLine.substring(0, currentLine.indexOf(":"));
        Matcher matcher = CFAUtils.CFA_NODE_NAME_PATTERN.matcher(scopeSelectors);
        if (matcher.matches()) {
          location = idToCfaNode.get(Integer.parseInt(matcher.group(1)));
        }

      } else {
        mapping.put(location, MemoryLocation.parseExtendedQualifiedName(currentLine));
      }
    }

    return mapping;
  }

  private CFANode getDefaultLocation(Map<Integer, CFANode> idToCfaNode) {
    return idToCfaNode.values().iterator().next();
  }

  public Configuration getConfiguration() {
    return config;
  }

  public CFA getCFA() {
    return cfa;
  }

  public ShutdownNotifier getShutdownNotifier() {
    return shutdownNotifier;
  }
}
