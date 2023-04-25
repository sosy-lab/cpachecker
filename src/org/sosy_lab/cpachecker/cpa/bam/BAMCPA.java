// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmFactory;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm.CEGARAlgorithmFactory;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm.CPAAlgorithmFactory;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMCache;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMCacheAggressiveImpl;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMCacheImpl;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMDataManager;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMDataManagerImpl;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisSummaryCache;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisSummary;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@Options(prefix = "cpa.bam")
public class BAMCPA extends AbstractBAMCPA implements StatisticsProvider, ProofChecker {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(BAMCPA.class);
  }

  private final BAMTransferRelation transfer;
  private final ProofChecker wrappedProofChecker;
  private final BAMDataManager data;
  private final BAMPCCManager bamPccManager;

  @Option(
      name = "handleRecursiveProcedures",
      secure = true,
      description =
          "BAM allows to analyse recursive procedures. This strongly depends on the underlying CPA."
              + " The current support includes only ValueAnalysis and PredicateAnalysis (with tree"
              + " interpolation enabled).")
  private boolean handleRecursiveProcedures = false;

  @Option(
      secure = true,
      description =
          "If enabled, cache queries also consider blocks with non-matching precision for reuse.")
  private boolean aggressiveCaching = true;

  @Option(
      secure = true,
      description = "Should the nested CPA-algorithm be wrapped with CEGAR within BAM?")
  private boolean useCEGAR = false;

  @Option(
      secure = true,
      description = "get initial summaries from file")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path initialSummariesFile = null;

  @Option(
      secure = true,
      description = "get the last revision of the program, for which the summaries were generated")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path lastRevision = null;

  private BAMCPA(
      ConfigurableProgramAnalysis pCpa,
      Configuration config,
      LogManager pLogger,
      ReachedSetFactory pReachedSetFactory,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException, CPAException {
    super(pCpa, config, pLogger, pShutdownNotifier, pSpecification, pCfa);
    config.inject(this);

    if (pCpa instanceof ProofChecker) {
      wrappedProofChecker = (ProofChecker) pCpa;
    } else {
      wrappedProofChecker = null;
    }

    final BAMCache cache;
    if (aggressiveCaching) {
      cache = new BAMCacheAggressiveImpl(config, getReducer(), logger);
    } else {
      cache = new BAMCacheImpl(config, getReducer(), logger);
    }
    data = new BAMDataManagerImpl(this, cache, pReachedSetFactory, pLogger);

    bamPccManager =
        new BAMPCCManager(wrappedProofChecker, config, blockPartitioning, getReducer(), this, data);

    AlgorithmFactory factory = new CPAAlgorithmFactory(this, logger, config, pShutdownNotifier);
    if (useCEGAR) {
      // We will use this single instance of CEGARAlgFactory for the whole analysis.
      // There will be exactly one Refiner within all nestings of BAM (and one from the surrounding
      // CEGAR loop), because it is part of the factory.
      factory = new CEGARAlgorithmFactory(factory, this, logger, config, pShutdownNotifier);
    }

    ValueAnalysisSummaryCache.initialize(blockPartitioning);

    if (handleRecursiveProcedures) {
      transfer =
          new BAMTransferRelationWithFixPointForRecursion(
              config, this, pShutdownNotifier, factory, bamPccManager, searchTargetStatesOnExit());
    } else if (initialSummariesFile != null && lastRevision != null) {
      transfer =
          new BAMTransferRelationWithSummaryReuse(
              this, pShutdownNotifier, factory, bamPccManager, searchTargetStatesOnExit());
      readSummaries(config, pCfa);
    } else {
      transfer =
          new BAMTransferRelation(
              this, pShutdownNotifier, factory, bamPccManager, searchTargetStatesOnExit());
    }
  }

  @Override
  public BAMMergeOperator getMergeOperator() {
    return super.getMergeOperator().withBAMPCCManager(bamPccManager);
  }

  @Override
  public StopOperator getStopOperator() {
    return handleRecursiveProcedures
        ? new BAMStopOperatorForRecursion(getWrappedCpa().getStopOperator())
        : getWrappedCpa().getStopOperator();
  }

  @Override
  public BAMPrecisionAdjustment getPrecisionAdjustment() {
    return new BAMPrecisionAdjustment(
        getWrappedCpa().getPrecisionAdjustment(), data, bamPccManager, logger, blockPartitioning);
  }

  @Override
  public BAMTransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public BAMDataManager getData() {
    Preconditions.checkNotNull(data);
    return data;
  }

  public BAMPCCManager getBamPccManager() {
    return bamPccManager;
  }

  @Override
  public boolean areAbstractSuccessors(
      AbstractState pState, CFAEdge pCfaEdge, Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    Preconditions.checkNotNull(
        wrappedProofChecker, "Wrapped CPA has to implement ProofChecker interface");
    return bamPccManager.areAbstractSuccessors(pState, pCfaEdge, pSuccessors);
  }

  @Override
  public boolean isCoveredBy(AbstractState pState, AbstractState pOtherState)
      throws CPAException, InterruptedException {
    Preconditions.checkNotNull(
        wrappedProofChecker, "Wrapped CPA has to implement ProofChecker interface");
    return wrappedProofChecker.isCoveredBy(pState, pOtherState);
  }

  public BAMMultipleCEXSubgraphComputer createBAMMultipleSubgraphComputer(
      Function<ARGState, Integer> pIdExtractor) {
    return new BAMMultipleCEXSubgraphComputer(this, pIdExtractor);
  }

  private void readSummaries(Configuration pConfig, CFA pCfa) throws InvalidConfigurationException {
    if (readSummariesFromFile(pConfig, pCfa))
      selectSummaries(pConfig, pCfa);
  }

  private boolean readSummariesFromFile(Configuration pConfig, CFA pCfa)
      throws InvalidConfigurationException {
    ValueAnalysisSummaryCache cache = ValueAnalysisSummaryCache.getInstance();
    List<String> contents;
    try {
      contents = Files.readAllLines(initialSummariesFile, Charset.defaultCharset());
    } catch (IOException e) {
      logger.logUserException(
          Level.WARNING, e, "Could not read summaries from file named " + initialSummariesFile);
      return false;
    }

    String location = null;
    VariableTrackingPrecision precision = null;
    ValueAnalysisState entryState = null, exitState;
    for (String currentLine : contents) {
      if (currentLine.trim().isEmpty()) {
        continue;
      }

      if (location == null) {
        location = currentLine;
        continue;
      }

      if (precision == null) {
        var variables = Splitter.on(", ").trimResults(CharMatcher.anyOf("[]")).splitToList(currentLine);
        Multimap<CFANode, MemoryLocation> mapping = HashMultimap.create();
        var locations = variables.stream().map(name -> MemoryLocation.fromQualifiedName(name)).toList();
        mapping.putAll(pCfa.getMainFunction(), locations);

        precision = VariableTrackingPrecision.createRefineablePrecision(
            pConfig,
            VariableTrackingPrecision.createStaticPrecision(
                pConfig, pCfa.getVarClassification(), getClass()));

        precision = precision.withIncrement(mapping);
        continue;
      }

      if (entryState == null) {
        entryState = readState(currentLine, pCfa);
        continue;
      }

      exitState = readState(currentLine, pCfa);

      var summary = new ValueAnalysisSummary(entryState, exitState, precision);
      cache.add(location, summary);

      location = null;
      entryState = null;
      precision = null;
    }
    return true;
  }

  private ValueAnalysisState readState(String line, CFA pCfa) {
    // line format: [varName=NumericValue[number=value] (dataType), ...]
    ValueAnalysisState state = new ValueAnalysisState(pCfa.getMachineModel());


    var values = Splitter.on(", ")
        .trimResults(CharMatcher.anyOf("[]"))
        .omitEmptyStrings()
        .splitToList(line);

    for (String value : values) {
      // value format: varName=NumericValue[number=value] (dataType)
      var split = Splitter.on(CharMatcher.anyOf("=]")).splitToList(value);
      var name = split.get(0);
      var number = split.get(2);

      var memLoc = MemoryLocation.fromQualifiedName(name);

      state.assignConstant(memLoc, new NumericValue(Long.parseLong(number)), null);
    }

    return state;
  }

  private void selectSummaries(Configuration pConfig, CFA pCfa) throws InvalidConfigurationException {
    if (lastRevision == null) {
      return;
    }

    CFACreator cfaCreator = new CFACreator(pConfig, logger, shutdownNotifier);
    try {
      var previousCfa =
          cfaCreator.parseFileAndCreateCFA(ImmutableList.of(lastRevision.toString()));

      ValueAnalysisSummaryCache.getInstance().removeSummariesForChangedBlocks(pCfa, previousCfa);

    } catch (ParserException pE) {
      throw new InvalidConfigurationException("Parser error for lastRevision", pE);
    } catch (InterruptedException | IOException pE) {
      throw new AssertionError(pE);
    }
  }
}
