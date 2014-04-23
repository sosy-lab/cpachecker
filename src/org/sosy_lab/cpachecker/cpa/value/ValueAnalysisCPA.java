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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StopNeverOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisStaticRefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.value")
public class ValueAnalysisCPA implements ConfigurableProgramAnalysisWithBAM, StatisticsProvider, ProofChecker {

  @Option(name="merge", toUppercase=true, values={"SEP", "JOIN"},
      description="which merge operator to use for ValueAnalysisCPA")
  private String mergeType = "SEP";

  @Option(name="stop", toUppercase=true, values={"SEP", "JOIN", "NEVER"},
      description="which stop operator to use for ValueAnalysisCPA")
  private String stopType = "SEP";

  @Option(name="variableBlacklist",
      description="blacklist regex for variables that won't be tracked by ValueAnalysisCPA")
  private String variableBlacklist = "";

  @Option(description="enables target checking for value-analysis, needed for predicate-analysis")
  private boolean doTargetCheck = false;

  @Option(name="inPredicatedAnalysis",
      description="enable if will be used in predicated analysis but all variables should be tracked, no refinement")
  private boolean useInPredicatedAnalysisWithoutRefinement = false;

  @Option(name="refiner.performInitialStaticRefinement",
      description="use heuristic to extract a precision from the CFA statically on first refinement")
  private boolean performInitialStaticRefinement = false;

  @Option(description="get an initial precison from file")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path initialPrecisionFile = null;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ValueAnalysisCPA.class);
  }

  private AbstractDomain abstractDomain;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private TransferRelation transferRelation;
  private ValueAnalysisPrecision precision;
  private PrecisionAdjustment precisionAdjustment;
  private final ValueAnalysisStaticRefiner staticRefiner;
  private final ValueAnalysisReducer reducer;
  private final ValueAnalysisCPAStatistics statistics;

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;

  private ValueAnalysisCPA(Configuration config, LogManager logger,
      ShutdownNotifier pShutdownNotifier, CFA cfa) throws InvalidConfigurationException {
    this.config           = config;
    this.logger           = logger;
    this.shutdownNotifier = pShutdownNotifier;
    this.cfa              = cfa;

    config.inject(this);

    abstractDomain      = new ValueAnalysisDomain();
    transferRelation    = new ValueAnalysisTransferRelation(config, logger, cfa);
    precision           = initializePrecision(config, cfa);
    mergeOperator       = initializeMergeOperator();
    stopOperator        = initializeStopOperator();
    staticRefiner       = initializeStaticRefiner(cfa);
    precisionAdjustment = StaticPrecisionAdjustment.getInstance();
    reducer             = new ValueAnalysisReducer();
    statistics          = new ValueAnalysisCPAStatistics(this, config);

    if (doTargetCheck) {
      ValueAnalysisState.initChecker(config);
    }
  }

  private MergeOperator initializeMergeOperator() {
    if (mergeType.equals("SEP")) {
      return MergeSepOperator.getInstance();
    }

    else if (mergeType.equals("JOIN")) {
      return new MergeJoinOperator(abstractDomain);
    }

    return null;
  }

  private StopOperator initializeStopOperator() {
    if (stopType.equals("SEP")) {
      return new StopSepOperator(abstractDomain);
    }

    else if (stopType.equals("JOIN")) {
      return new StopJoinOperator(abstractDomain);
    }

    else if (stopType.equals("NEVER")) {
      return new StopNeverOperator();
    }

    return null;
  }

  private ValueAnalysisStaticRefiner initializeStaticRefiner(CFA cfa) throws InvalidConfigurationException {
    if (performInitialStaticRefinement) {
      return new ValueAnalysisStaticRefiner(config, logger, precision);
    }

    return null;
  }

  private ValueAnalysisPrecision initializePrecision(Configuration config, CFA cfa) throws InvalidConfigurationException {
    // create default (empty) precision
    ValueAnalysisPrecision precision = new ValueAnalysisPrecision(variableBlacklist, config, cfa.getVarClassification());

    // refine it with precision from file
    return new ValueAnalysisPrecision(precision, restoreMappingFromFile(cfa));
  }

  private Multimap<CFANode, MemoryLocation> restoreMappingFromFile(CFA cfa) throws InvalidConfigurationException {
    Multimap<CFANode, MemoryLocation> mapping = HashMultimap.create();
    if (initialPrecisionFile == null) {
      return mapping;
    }

    List<String> contents = null;
    try {
      contents = initialPrecisionFile.asCharSource(Charset.defaultCharset()).readLines();
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
      }

      else if(currentLine.endsWith(":")) {
        String scopeSelectors = currentLine.substring(0, currentLine.indexOf(":"));
        Matcher matcher = CFA_NODE_PATTERN.matcher(scopeSelectors);
        if (matcher.matches()) {
          location = idToCfaNode.get(Integer.parseInt(matcher.group(1)));
        }
      }

      else {
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

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public AbstractState getInitialState(CFANode node) {
    return new ValueAnalysisState();
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return precision;
  }

  ValueAnalysisPrecision getPrecision() {
    return precision;
  }

  public ValueAnalysisStaticRefiner getStaticRefiner() {
    return staticRefiner;
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
    return reducer;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }

  public ValueAnalysisCPAStatistics getStats() {
    return statistics;
  }

  @Override
  public boolean areAbstractSuccessors(AbstractState pState, CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors) throws CPATransferException, InterruptedException {
    try {
      Collection<? extends AbstractState> computedSuccessors = transferRelation.getAbstractSuccessors(pState, null, pCfaEdge);
      boolean found;
      for(AbstractState comp:computedSuccessors){
        found = false;
        for(AbstractState e:pSuccessors){
          if(isCoveredBy(comp, e)){
            found = true;
            break;
          }
        }
        if(!found){
          return false;
        }
      }
    } catch (CPAException e) {
      e.printStackTrace();
    }
    return true;
  }

  @Override
  public boolean isCoveredBy(AbstractState pState, AbstractState pOtherState) throws CPAException, InterruptedException {
     return abstractDomain.isLessOrEqual(pState, pOtherState);
  }
}
