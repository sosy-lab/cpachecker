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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
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
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StopNeverOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
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
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisConcreteErrorPathAllocator;
import org.sosy_lab.cpachecker.util.StateToFormulaWriter;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@Options(prefix = "cpa.value")
public class ValueAnalysisCPA
    implements ConfigurableProgramAnalysisWithBAM, StatisticsProvider, ProofCheckerCPA,
        ConfigurableProgramAnalysisWithConcreteCex {

  @Option(secure=true, name="merge", toUppercase=true, values={"SEP", "JOIN"},
      description="which merge operator to use for ValueAnalysisCPA")
  private String mergeType = "SEP";

  @Option(secure=true, name="stop", toUppercase=true, values={"SEP", "JOIN", "NEVER"},
      description="which stop operator to use for ValueAnalysisCPA")
  private String stopType = "SEP";

  @Option(secure=true, description="get an initial precision from file")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path initialPrecisionFile = null;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ValueAnalysisCPA.class);
  }

  private final AbstractDomain abstractDomain =
      DelegateAbstractDomain.<ValueAnalysisState>getInstance();
  private ValueAnalysisTransferRelation transferRelation;
  private VariableTrackingPrecision precision;
  private ValueAnalysisPrecisionAdjustment precisionAdjustment;
  private final ValueAnalysisCPAStatistics statistics;
  private final StateToFormulaWriter writer;

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;

  private boolean refineablePrecisionSet = false;
  private ValueAnalysisConcreteErrorPathAllocator errorPathAllocator;

  private ValueAnalysisCPA(Configuration config, LogManager logger,
      ShutdownNotifier pShutdownNotifier, CFA cfa) throws InvalidConfigurationException {
    this.config           = config;
    this.logger           = logger;
    this.shutdownNotifier = pShutdownNotifier;
    this.cfa              = cfa;

    config.inject(this);

    transferRelation    = new ValueAnalysisTransferRelation(config, logger, cfa);
    precision           = initializePrecision(config, cfa);

    precisionAdjustment = new ValueAnalysisPrecisionAdjustment(config, transferRelation, cfa);

    statistics          = new ValueAnalysisCPAStatistics(this, config);
    writer = new StateToFormulaWriter(config, logger, shutdownNotifier, cfa);

    errorPathAllocator = new ValueAnalysisConcreteErrorPathAllocator(config, logger, cfa.getMachineModel());
  }

  private VariableTrackingPrecision initializePrecision(Configuration config, CFA cfa) throws InvalidConfigurationException {

    if (initialPrecisionFile == null) {
      return VariableTrackingPrecision.createStaticPrecision(config, cfa.getVarClassification(), getClass());

    } else {
      // create precision with empty, refinable component precision
      VariableTrackingPrecision precision = VariableTrackingPrecision.createRefineablePrecision(config,
                      VariableTrackingPrecision.createStaticPrecision(config, cfa.getVarClassification(), getClass()));
      // refine the refinable component precision with increment from file
      return precision.withIncrement(restoreMappingFromFile(cfa));
    }
  }

  private Multimap<CFANode, MemoryLocation> restoreMappingFromFile(CFA cfa) {
    Multimap<CFANode, MemoryLocation> mapping = HashMultimap.create();

    List<String> contents = null;
    try {
      contents = Files.readAllLines(initialPrecisionFile, Charset.defaultCharset());
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not read precision from file named " + initialPrecisionFile);
      return mapping;
    }

    Map<Integer, CFANode> idToCfaNode = createMappingForCFANodes(cfa);
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

  private Map<Integer, CFANode> createMappingForCFANodes(CFA cfa) {
    Map<Integer, CFANode> idToNodeMap = Maps.newHashMap();
    for (CFANode n : cfa.getAllNodes()) {
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
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public MergeOperator getMergeOperator() {
    switch (mergeType) {
      case "SEP":
        return MergeSepOperator.getInstance();

      case "JOIN":
        return new MergeJoinOperator(getAbstractDomain());

      default:
        throw new AssertionError("unknown merge operator");
    }
  }

  @Override
  public StopOperator getStopOperator() {
    switch (stopType) {
      case "SEP":
        return new StopSepOperator(getAbstractDomain());

      case "JOIN":
        return new StopJoinOperator(getAbstractDomain());

      case "NEVER":
        return new StopNeverOperator();

      default:
        throw new AssertionError("unknown stop operator");
    }
  }

  @Override
  public ValueAnalysisTransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new ValueAnalysisState(cfa.getMachineModel());
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return precision;
  }

  VariableTrackingPrecision getPrecision() {
    return precision;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
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
    writer.collectStatistics(pStatsCollection);
    precisionAdjustment.collectStatistics(pStatsCollection);
    transferRelation.collectStatistics(pStatsCollection);
  }

  public ValueAnalysisCPAStatistics getStats() {
    return statistics;
  }

  @Override
  public ConcreteStatePath createConcreteStatePath(ARGPath pPath) {
    return errorPathAllocator.allocateAssignmentsToPath(pPath);
  }
}