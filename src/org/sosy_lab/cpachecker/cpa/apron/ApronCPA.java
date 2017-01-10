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
package org.sosy_lab.cpachecker.cpa.apron;

import apron.ApronException;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.ApronManager;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@Options(prefix = "cpa.apron")
public final class ApronCPA
    implements ConfigurableProgramAnalysis, ProofCheckerCPA, StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ApronCPA.class);
  }

  @Option(secure=true, name="initialPrecisionType", toUppercase=true, values={"STATIC_FULL", "REFINEABLE_EMPTY"},
      description="this option determines which initial precision should be used")
  private String precisionType = "STATIC_FULL";

  @Option(secure=true, name="splitDisequalities",
      description="split disequalities considering integer operands into two states or use disequality provided by apron library ")
  private boolean splitDisequalities = true;

  @Option(secure=true, name="domain", toUppercase=true,
      description="Use this to change the underlying abstract domain in the APRON library")
  private ApronManager.AbstractDomain domainType = ApronManager.AbstractDomain.OCTAGON;

  @Option(secure=true, description="get an initial precision from file")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path initialPrecisionFile = null;

  @Option(secure=true, description="target file to hold the exported precision")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path precisionFile = null;

  private final AbstractDomain abstractDomain;
  private final TransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final LogManager logger;
  private final Precision precision;
  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;
  private final ApronManager apronManager;

  private ApronCPA(Configuration config, LogManager log,
                     ShutdownNotifier shutdownNotifier, CFA cfa)
                     throws InvalidConfigurationException, CPAException {
    if (!cfa.getLoopStructure().isPresent()) {
      throw new CPAException("ApronCPA cannot work without loop-structure information in CFA.");
    }
    config.inject(this);
    logger = log;
    ApronDomain apronDomain = new ApronDomain(logger);

    apronManager = new ApronManager(domainType);

    this.transferRelation = new ApronTransferRelation(logger, cfa.getLoopStructure().get(), splitDisequalities);

    MergeOperator apronMergeOp = ApronMergeOperator.getInstance(apronDomain, config);

    StopOperator apronStopOp = new StopSepOperator(apronDomain);

    this.abstractDomain = apronDomain;
    this.mergeOperator = apronMergeOp;
    this.stopOperator = apronStopOp;
    this.config = config;
    this.shutdownNotifier = shutdownNotifier;
    this.cfa = cfa;

    VariableTrackingPrecision tempPrecision;
    if (initialPrecisionFile != null || precisionType.equals("REFINEABLE_EMPTY")) {
      tempPrecision = VariableTrackingPrecision.createRefineablePrecision(config,
          VariableTrackingPrecision.createStaticPrecision(config, cfa.getVarClassification(), getClass()));
      if (initialPrecisionFile != null) {
        tempPrecision = tempPrecision.withIncrement(restoreMappingFromFile(cfa));
      }
      // static full precision is default
    } else {
      tempPrecision = VariableTrackingPrecision.createStaticPrecision(config, cfa.getVarClassification(), getClass());
    }
    precision = tempPrecision;
  }

  public ApronManager getManager() {
    return apronManager;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
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
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    try {
      return new ApronState(logger, apronManager);
    } catch (ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return precision;
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
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Statistics() {

      @Override
      public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
        if (precisionFile != null) {
          exportPrecision(pReached);
        }

      }

      @Override
      public @Nullable
      String getName() {
        return ApronCPA.this.getClass().getSimpleName();
      }
    });

  }

  /**
   * This method exports the precision to file.
   *
   * @param reached the set of reached states.
   */
  private void exportPrecision(UnmodifiableReachedSet reached) {
    VariableTrackingPrecision consolidatedPrecision =
        VariableTrackingPrecision.joinVariableTrackingPrecisionsInReachedSet(reached);
    try (Writer writer = MoreFiles.openOutputFile(precisionFile, Charset.defaultCharset())) {
      consolidatedPrecision.serialize(writer);
    } catch (IOException e) {
      getLogger().logUserException(Level.WARNING, e, "Could not write apron-analysis precision to file");
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


}
