/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithConcreteCex;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisPrecisionAdjustment.PrecAdjustmentOptions;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisPrecisionAdjustment.PrecAdjustmentStatistics;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation.ValueTransferOptions;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisConcreteErrorPathAllocator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.ConstraintsStrengthenOperator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.SymbolicValueAnalysisPrecisionAdjustment;
import org.sosy_lab.cpachecker.cpa.value.symbolic.SymbolicValueAnalysisPrecisionAdjustment.SymbolicStatistics;
import org.sosy_lab.cpachecker.cpa.value.symbolic.SymbolicValueAssigner;
import org.sosy_lab.cpachecker.util.StateToFormulaWriter;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.states.MemoryLocationValueHandler;

@Options(prefix = "cpa.value")
public class ValueAnalysisCPA extends AbstractCPA
    implements ConfigurableProgramAnalysisWithBAM,
        StatisticsProvider,
        ProofCheckerCPA,
        ConfigurableProgramAnalysisWithConcreteCex {

  @Option(secure=true, name="merge", toUppercase=true, values={"SEP", "JOIN"},
      description="which merge operator to use for ValueAnalysisCPA")
  private String mergeType = "SEP";

  @Option(
      secure = true,
      name = "stop",
      toUppercase = true,
      values = {"SEP", "JOIN", "NEVER", "EQUALS"},
      description = "which stop operator to use for ValueAnalysisCPA")
  private String stopType = "SEP";

  @Option(secure=true, description="get an initial precision from file")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path initialPrecisionFile = null;

  @Option(secure=true,
      name="symbolic.useSymbolicValues",
      description="Use symbolic values. This allows tracking of non-deterministic values."
          + " Symbolic values should always be used in conjunction with ConstraintsCPA."
          + " Otherwise, symbolic values will be created, but not evaluated.")
  private boolean useSymbolicValues = false;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ValueAnalysisCPA.class);
  }

  private VariableTrackingPrecision precision;
  private final ValueAnalysisCPAStatistics statistics;
  private final StateToFormulaWriter writer;

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;

  private boolean refineablePrecisionSet = false;
  private ValueAnalysisConcreteErrorPathAllocator errorPathAllocator;

  private MemoryLocationValueHandler unknownValueHandler;
  private final ConstraintsStrengthenOperator constraintsStrengthenOperator;
  private final ValueTransferOptions transferOptions;
  private final PrecAdjustmentOptions precisionAdjustmentOptions;
  private final PrecAdjustmentStatistics precisionAdjustmentStatistics;
  private final SymbolicStatistics symbolicStats;

  private ValueAnalysisCPA(Configuration config, LogManager logger,
      ShutdownNotifier pShutdownNotifier, CFA cfa) throws InvalidConfigurationException {
    super(DelegateAbstractDomain.<ValueAnalysisState>getInstance(), null);
    this.config           = config;
    this.logger           = logger;
    this.shutdownNotifier = pShutdownNotifier;
    this.cfa              = cfa;

    config.inject(this, ValueAnalysisCPA.class);

    precision           = initializePrecision(config, cfa);
    statistics          = new ValueAnalysisCPAStatistics(this, config);
    writer = new StateToFormulaWriter(config, logger, shutdownNotifier, cfa);
    errorPathAllocator = new ValueAnalysisConcreteErrorPathAllocator(config, logger, cfa.getMachineModel());

    if (useSymbolicValues) {
      unknownValueHandler = new SymbolicValueAssigner(config);
      symbolicStats = new SymbolicStatistics();
    } else {
      unknownValueHandler = new UnknownValueAssigner();
      symbolicStats = null;
    }

    constraintsStrengthenOperator =
        new ConstraintsStrengthenOperator(config, logger);
    transferOptions = new ValueTransferOptions(config);
    precisionAdjustmentOptions = new PrecAdjustmentOptions(config, cfa);
    precisionAdjustmentStatistics = new PrecAdjustmentStatistics();
  }

  private VariableTrackingPrecision initializePrecision(Configuration pConfig, CFA pCfa) throws InvalidConfigurationException {

    if (initialPrecisionFile == null) {
      return VariableTrackingPrecision.createStaticPrecision(pConfig, pCfa.getVarClassification(), getClass());

    } else {
      // create precision with empty, refinable component precision
      VariableTrackingPrecision initialPrecision =
          VariableTrackingPrecision.createRefineablePrecision(
              pConfig,
              VariableTrackingPrecision.createStaticPrecision(
                  pConfig, pCfa.getVarClassification(), getClass()));
      // refine the refinable component precision with increment from file
      return initialPrecision.withIncrement(restoreMappingFromFile(pCfa));
    }
  }

  private Multimap<CFANode, MemoryLocation> restoreMappingFromFile(CFA pCfa) {
    Multimap<CFANode, MemoryLocation> mapping = HashMultimap.create();

    List<String> contents = null;
    try {
      contents = Files.readAllLines(initialPrecisionFile, Charset.defaultCharset());
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not read precision from file named " + initialPrecisionFile);
      return mapping;
    }

    Map<Integer, CFANode> idToCfaNode = createMappingForCFANodes(pCfa);
    final Pattern CFA_NODE_PATTERN = Pattern.compile("N([0-9][0-9]*)");

    CFANode location = getDefaultLocation(idToCfaNode);
    for (String currentLine : contents) {
      if (currentLine.trim().isEmpty()) {
        continue;

      } else if(currentLine.endsWith(":")) {
        String scopeSelectors = currentLine.substring(0, currentLine.indexOf(":"));
        Matcher matcher = CFA_NODE_PATTERN.matcher(scopeSelectors);
        if (matcher.matches()) {
          location = idToCfaNode.get(Integer.parseInt(matcher.group(1)));
        }

      } else {
        mapping.put(location, MemoryLocation.valueOf(currentLine));
      }
    }

    return mapping;
  }

  private CFANode getDefaultLocation(Map<Integer, CFANode> idToCfaNode) {
    return idToCfaNode.values().iterator().next();
  }

  private Map<Integer, CFANode> createMappingForCFANodes(CFA pCfa) {
    Map<Integer, CFANode> idToNodeMap = new HashMap<>();
    for (CFANode n : pCfa.getAllNodes()) {
      idToNodeMap.put(n.getNodeNumber(), n);
    }
    return idToNodeMap;
  }

  public void injectRefinablePrecision() throws InvalidConfigurationException {

    // replace the full precision with an empty, refinable precision
    if (initialPrecisionFile == null && !refineablePrecisionSet) {
      precision = VariableTrackingPrecision.createRefineablePrecision(config, precision);
      refineablePrecisionSet = true;
    }
  }

  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator(mergeType);
  }

  @Override
  public StopOperator getStopOperator() {
    return buildStopOperator(stopType);
  }

  @Override
  public ValueAnalysisTransferRelation getTransferRelation() {
    return new ValueAnalysisTransferRelation(
        logger,
        cfa,
        transferOptions,
        unknownValueHandler,
        constraintsStrengthenOperator,
        statistics);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new ValueAnalysisState(cfa.getMachineModel());
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return precision;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    if (useSymbolicValues) {
      return new SymbolicValueAnalysisPrecisionAdjustment(
          statistics,
          cfa,
          precisionAdjustmentOptions,
          precisionAdjustmentStatistics,
          Preconditions.checkNotNull(symbolicStats));
    } else {
      return new ValueAnalysisPrecisionAdjustment(
          statistics, cfa, precisionAdjustmentOptions, precisionAdjustmentStatistics);
    }
  }

  public Configuration getConfiguration() {
    return config;
  }

  public LogManager getLogger() {
    return logger;
  }

  public ShutdownNotifier getShutdownNotifier() {
    return shutdownNotifier;
  }

  public CFA getCFA() {
    return cfa;
  }

  @Override
  public Reducer getReducer() {
    return new ValueAnalysisReducer();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
    pStatsCollection.add(precisionAdjustmentStatistics);
    if (symbolicStats != null) {
      pStatsCollection.add(symbolicStats);
    }
    pStatsCollection.add(constraintsStrengthenOperator);
    writer.collectStatistics(pStatsCollection);
  }

  @Override
  public ConcreteStatePath createConcreteStatePath(ARGPath pPath) {
    return errorPathAllocator.allocateAssignmentsToPath(pPath);
  }
}