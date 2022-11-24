// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.rankings.EdgeTypeScoring;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultExplanation;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfoExporter;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfoExporter.IntermediateFaults;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultScoring;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo.InfoType;
import org.sosy_lab.cpachecker.util.faultlocalization.explanation.InformationProvider;
import org.sosy_lab.cpachecker.util.faultlocalization.explanation.NoContextExplanation;
import org.sosy_lab.cpachecker.util.faultlocalization.explanation.SuspiciousCalculationExplanation;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.MaximalLineDistanceScoring;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.MinimalLineDistanceScoring;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.OverallOccurrenceScoring;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.SetSizeScoring;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.VariableCountScoring;

@Options(prefix = "faultLocalization.import")
public class FaultLocalizationByImport implements Algorithm {

  enum Scoring {
    VARIABLE_COUNT,
    EDGE_TYPE,
    MINIMAL_LINE_DISTANCE,
    MAXIMAL_LINE_DISTANCE,
    OVERALL_OCCURRENCE,
    SET_SIZE
  }

  enum Explanation {
    NO_CONTEXT,
    SUSPICIOUS_CALCULATION,
    INFORMATION_PROVIDER
  }

  private final Algorithm algorithm;
  private final FaultsConverter deserializer;
  private final LogManager logger;
  private final Configuration config;
  private final CFA cfa;

  @Option(secure = true, description = "Whether to run specified analysis")
  private boolean algorithmActivated = false;

  @Option(secure = true, description = "path to the input json file with faults")
  @FileOption(value = Type.REQUIRED_INPUT_FILE)
  private Path importFile;

  @Option(secure = true, description = "which explanations to use")
  private List<Explanation> explanations = ImmutableList.of();

  @Option(secure = true, description = "which scoring functions to use")
  private List<Scoring> scorings = ImmutableList.of();

  public FaultLocalizationByImport(
      Configuration pConfiguration, Algorithm pAlgorithm, CFA pCFA, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    logger = pLogger;
    algorithm = pAlgorithm;
    config = pConfiguration;
    cfa = pCFA;
    deserializer = new FaultsConverter(pCFA);
    if (algorithm == null && algorithmActivated) {
      throw new AssertionError("Cannot run an unavailable analysis");
    }
  }

  private FaultExplanation instantiateExplanations(
      Explanation pExplanation, List<CFAEdge> pEdgeList) {
    switch (pExplanation) {
      case NO_CONTEXT:
        return NoContextExplanation.getInstance();
      case SUSPICIOUS_CALCULATION:
        return new SuspiciousCalculationExplanation();
      case INFORMATION_PROVIDER:
        return new InformationProvider(pEdgeList);
      default:
        throw new IllegalStateException("Unexpected value: " + pExplanation);
    }
  }

  private FaultScoring instantiateScoring(Scoring pScoring, CFAEdge pErrorLocation) {
    switch (pScoring) {
      case VARIABLE_COUNT:
        return new VariableCountScoring();
      case EDGE_TYPE:
        return new EdgeTypeScoring();
      case MINIMAL_LINE_DISTANCE:
        if (pErrorLocation == null) {
          throw new IllegalArgumentException(
              "Tried to use "
                  + MinimalLineDistanceScoring.class
                  + " while not providing an error location in "
                  + importFile);
        }
        return new MinimalLineDistanceScoring(pErrorLocation);
      case MAXIMAL_LINE_DISTANCE:
        if (pErrorLocation == null) {
          throw new IllegalArgumentException(
              "Tried to use "
                  + MaximalLineDistanceScoring.class
                  + " while not providing an error location in "
                  + importFile);
        }
        return new MaximalLineDistanceScoring(pErrorLocation);
      case OVERALL_OCCURRENCE:
        return new OverallOccurrenceScoring();
      case SET_SIZE:
        return new SetSizeScoring();
      default:
        throw new IllegalStateException("Unexpected value: " + pScoring);
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    logger.log(Level.INFO, "Start parsing faults...");
    Collection<Fault> faults;
    CFAEdge errorEdge;
    CFANode errorLocation;
    try {
      IntermediateFaults intermediateFaults = deserializer.jsonToFaults(importFile);
      faults = intermediateFaults.getFaults();
      errorEdge = intermediateFaults.getErrorLocation();
      errorLocation = errorEdge == null ? null : errorEdge.getSuccessor();
    } catch (IOException pE) {
      throw new CPAException("Could not deserialize faults", pE);
    }
    logger.logf(Level.INFO, "Finished parsing %d faults.", faults.size());
    AlgorithmStatus status;
    if (algorithmActivated) {
      logger.log(Level.INFO, "Run configured analysis...");
      status = algorithm.run(reachedSet);
      FluentIterable<CounterexampleInfo> counterExamples =
          Optionals.presentInstances(
              from(reachedSet)
                  .filter(AbstractStates::isTargetState)
                  .filter(ARGState.class)
                  .transform(ARGState::getCounterexampleInformation));
      logger.log(Level.INFO, "Apply rankings/explanations...");
      for (CounterexampleInfo counterExample : counterExamples) {
        CFAPathWithAssumptions assumptions = counterExample.getCFAPathWithAssignments();
        final List<CFAEdge> edgeList =
            transformedImmutableListCopy(assumptions, assumption -> assumption.getCFAEdge());
        if (edgeList.isEmpty()) {
          continue;
        }
        FaultScoring[] scoringArray = new FaultScoring[scorings.size()];
        for (int i = 0; i < scorings.size(); i++) {
          scoringArray[i] = instantiateScoring(scorings.get(i), edgeList.get(edgeList.size() - 1));
        }
        FaultScoring finalScoring = FaultRankingUtils.concatHeuristics(scoringArray);

        FaultExplanation[] explanationsArray = new FaultExplanation[explanations.size()];
        for (int i = 0; i < explanations.size(); i++) {
          explanationsArray[i] = instantiateExplanations(explanations.get(i), edgeList);
        }
        FaultLocalizationInfo flInfo =
            new FaultLocalizationInfo(ImmutableSet.copyOf(faults), finalScoring, counterExample);
        flInfo.getRankedList().forEach(f -> FaultExplanation.explain(f, explanationsArray));
        flInfo.apply();
        logger.log(Level.INFO, "Finished exporting faults...");
      }
    } else {
      try {
        status = AlgorithmStatus.SOUND_AND_PRECISE;
        if (errorLocation == null) {
          status = algorithm.run(reachedSet);
          ARGState target =
              (ARGState)
                  FluentIterable.from(reachedSet)
                      .filter(AbstractStates::isTargetState)
                      .toList()
                      .get(0);
          errorLocation = AbstractStates.extractLocation(target);
        }
        reachedSet.clear();
        Set<CFAEdge> edges =
            FluentIterable.concat(faults).transform(f -> f.correspondingEdge()).toSet();
        Comparator<List<CFAEdge>> mostIntersections =
            Comparator.comparingInt(
                path -> Sets.intersection(ImmutableSet.copyOf(path), edges).size());
        List<CFAEdge> bestPath =
            Collections.max(findAllPaths(cfa.getMainFunction(), errorLocation), mostIntersections);
        errorEdge = bestPath.get(bestPath.size() - 1);
        ARGState currState = null;
        LocationStateFactory factory =
            new LocationStateFactory(cfa, AnalysisDirection.FORWARD, config);
        for (CFAEdge cfaEdge : bestPath) {
          ARGState next =
              new ARGState(
                  new CompositeState(
                      ImmutableList.of(
                          new ConfigurableTargetState(false),
                          factory.getState(cfaEdge.getPredecessor()))),
                  currState);
          reachedSet.addNoWaitlist(next, new Precision() {});
          currState = next;
        }
        ARGState target =
            new ARGState(
                new CompositeState(
                    ImmutableList.of(
                        new ConfigurableTargetState(true),
                        factory.getState(bestPath.get(bestPath.size() - 1).getSuccessor()))),
                currState);
        reachedSet.addNoWaitlist(target, new Precision() {});
        logger.log(Level.INFO, "Apply rankings/explanations right away...");
        FaultScoring[] scoringArray = new FaultScoring[scorings.size()];
        for (int i = 0; i < scorings.size(); i++) {
          scoringArray[i] = instantiateScoring(scorings.get(i), errorEdge);
        }
        FaultScoring finalScoring = FaultRankingUtils.concatHeuristics(scoringArray);
        FaultExplanation[] explanationsArray = new FaultExplanation[explanations.size()];
        for (int i = 0; i < explanations.size(); i++) {
          explanationsArray[i] = instantiateExplanations(explanations.get(i), ImmutableList.of());
        }
        faults = FaultRankingUtils.rank(finalScoring, ImmutableList.copyOf(faults));
        boolean intendedIndex = true;
        for (Fault f : faults) {
          if (f.getIntendedIndex() == -1) {
            intendedIndex = false;
            break;
          }
        }
        if (intendedIndex) {
          faults =
              ImmutableList.sortedCopyOf(
                  Comparator.comparingInt(f -> f.getIntendedIndex()), faults);
        }
        for (Fault fault : faults) {
          FaultExplanation.explain(fault, explanationsArray);
        }
        ARGPath errorPath = ARGUtils.getOnePathTo((ARGState) reachedSet.getLastState());
        FaultLocalizationInfo info =
            FaultLocalizationInfo.withoutCounterexampleInfo(
                ImmutableList.copyOf(faults),
                CFAPathWithAssumptions.from(
                    transformedImmutableListCopy(
                        errorPath.getInnerEdges(),
                        e -> new CFAEdgeWithAssumptions(e, ImmutableList.of(), ""))),
                errorPath);
        info.apply();
      } catch (IndexOutOfBoundsException pE) {
        throw new AssertionError("Could not find a path showing the violation.", pE);
      } catch (InvalidConfigurationException pE) {
        throw new AssertionError("Configuration for LocationStateFactory was invalid.", pE);
      }
      try {
        new FaultLocalizationInfoExporter(config).export(faults, errorEdge);
        logger.log(Level.INFO, "Finished exporting faults successfully!");
      } catch (IOException | InvalidConfigurationException pE) {
        logger.logUserException(Level.WARNING, pE, "Failed exporting faults...");
      }
      return status;
    }
    return status;
  }

  public static class FaultsConverter {

    private final ObjectMapper mapper;

    public FaultsConverter(CFA pCFA) {
      mapper = new ObjectMapper();
      SimpleModule deserializer =
          new SimpleModule("FaultsDeserializer", new Version(1, 0, 0, null, null, null));
      deserializer.addDeserializer(IntermediateFaults.class, new FaultsDeserializer(pCFA));
      mapper.registerModule(deserializer);
    }

    public IntermediateFaults jsonToFaults(Path inputFile) throws IOException {
      return mapper.readValue(inputFile.toFile(), IntermediateFaults.class);
    }
  }

  private static class FaultsDeserializer extends StdDeserializer<IntermediateFaults> {

    private static final long serialVersionUID = -9027432550465230262L;
    private final Set<CFAEdge> edges;

    public FaultsDeserializer(CFA pCFA) {
      super(IntermediateFaults.class);
      edges =
          FluentIterable.from(pCFA.getAllNodes())
              .transformAndConcat(CFAUtils::allLeavingEdges)
              .toSet();
    }

    @Override
    public IntermediateFaults deserialize(JsonParser parser, DeserializationContext deserializer)
        throws IOException {
      ObjectCodec codec = parser.getCodec();
      JsonNode node = codec.readTree(parser);
      ImmutableList.Builder<Fault> builder = ImmutableList.builder();
      if (node.get("faults").isArray()) {
        for (JsonNode jsonNode : node.get("faults")) {
          builder.add(restoreFault(jsonNode.get("fault")));
        }
      }
      CFAEdge error = null;
      if (node.has("error-location")) {
        error = restoreLocation(node.get("error-location"));
      }
      return new IntermediateFaults(builder.build(), error);
    }

    private Fault restoreFault(JsonNode pNode) throws IOException {
      Fault fault = new Fault();
      if (pNode.has("score")) {
        fault.setScore(pNode.get("score").asDouble());
      }
      if (pNode.has("intendedIndex")) {
        fault.setIntendedIndex(pNode.get("intendedIndex").asInt());
      }
      if (pNode.has("infos")) {
        JsonNode infos = pNode.get("infos");
        for (JsonNode info : infos) {
          fault.addInfo(restoreFaultInfo(info.get("fault-info")));
        }
        if (!infos.isArray()) {
          throw new AssertionError("Field 'infos' has to be an array");
        }
      }
      JsonNode contributions = pNode.get("contributions");
      if (!contributions.isArray()) {
        throw new AssertionError(
            "Faults are lists of contributions but cannot restore list of contributions.");
      }
      for (JsonNode contribution : contributions) {
        fault.add(restoreContribution(contribution.get("fault-contribution")));
      }
      if (fault.isEmpty()) {
        throw new AssertionError("Faults consist of at least one contribution but found none");
      }
      return fault;
    }

    private FaultContribution restoreContribution(JsonNode pContribution) throws IOException {
      List<FaultInfo> restoredInfos = new ArrayList<>();
      if (pContribution.has("infos")) {
        JsonNode infos = pContribution.get("infos");
        if (!infos.isArray()) {
          throw new AssertionError("Field 'infos' has to be an array");
        }
        for (JsonNode info : infos) {
          restoredInfos.add(restoreFaultInfo(info.get("fault-info")));
        }
      }
      FaultContribution fc = new FaultContribution(restoreLocation(pContribution.get("location")));
      if (pContribution.has("score")) {
        fc.setScore(pContribution.get("score").asDouble());
      }
      restoredInfos.forEach(fc::addInfo);
      return fc;
    }

    private CFAEdge restoreLocation(JsonNode pLocation) throws IOException {
      int startLine = pLocation.get("startLine").asInt();
      int endLine = pLocation.get("endLine").asInt();
      int startOffset = pLocation.get("startOffset").asInt();
      int length = pLocation.get("endOffset").asInt() - startOffset;

      Predicate<CFAEdge> matchCode = Predicates.alwaysTrue();
      if (pLocation.has("code")) {
        String code = pLocation.get("code").asText();
        matchCode = e -> e.getCode().equals(code);
      }
      Predicate<CFAEdge> matchFilename = Predicates.alwaysTrue();
      if (pLocation.has("filename")) {
        Path filename = Path.of(pLocation.get("filename").asText()).getFileName();
        matchFilename = e -> e.getFileLocation().getFileName().equals(filename);
      }

      Predicate<CFAEdge> matchStartLine =
          e -> e.getFileLocation().getStartingLineInOrigin() == startLine;
      Predicate<CFAEdge> matchEndLine = e -> e.getFileLocation().getEndingLineInOrigin() == endLine;
      Predicate<CFAEdge> matchOffset = e -> e.getFileLocation().getNodeOffset() == startOffset;
      Predicate<CFAEdge> matchLength = e -> e.getFileLocation().getNodeLength() == length;

      return filter(
          edges,
          ImmutableList.of(
                  matchStartLine, matchEndLine, matchOffset, matchLength, matchCode, matchFilename)
              .iterator());
    }

    private CFAEdge filter(Set<CFAEdge> pRemaining, Iterator<Predicate<CFAEdge>> pFilter)
        throws IOException {
      Predicate<CFAEdge> nextFilter;
      while (pRemaining.size() > 1) {
        checkArgument(pFilter.hasNext(), "Not enough filters...");
        nextFilter = pFilter.next();
        pRemaining = pRemaining.stream().filter(nextFilter).collect(ImmutableSet.toImmutableSet());
      }
      if (pRemaining.isEmpty()) {
        throw new IOException("Cannot parse given input file (cannot find matching CFAEdge)");
      }
      return Iterables.getOnlyElement(pRemaining);
    }

    private FaultInfo restoreFaultInfo(JsonNode pNode) {
      InfoType type = FaultInfo.InfoType.valueOf(pNode.get("type").asText());
      String description = pNode.get("description").asText();
      switch (type) {
        case REASON:
          return FaultInfo.justify(description);
        case FIX:
          return FaultInfo.fix(description);
        case RANK_INFO:
          return FaultInfo.rankInfo(
              description, pNode.has("score") ? pNode.get("score").asDouble() : .0);
        default:
          throw new AssertionError("Unknown " + InfoType.class + ": " + type);
      }
    }
  }

  private static List<List<CFAEdge>> findAllPaths(CFANode pStart, CFANode pEnd) {
    List<List<CFAEdge>> waitlist = new ArrayList<>();
    List<List<CFAEdge>> finished = new ArrayList<>();
    for (CFAEdge leavingEdge : CFAUtils.leavingEdges(pStart)) {
      waitlist.add(new ArrayList<>(ImmutableList.of(leavingEdge)));
    }
    while (!waitlist.isEmpty()) {
      List<CFAEdge> path = waitlist.remove(0);
      Set<CFAEdge> covered = ImmutableSet.copyOf(path);
      CFAEdge lastEdge = path.get(path.size() - 1);
      CFANode currentTail = lastEdge.getSuccessor();
      if (currentTail.equals(pEnd)) {
        finished.add(path);
      }
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(currentTail)) {
        if (covered.contains(leavingEdge)) {
          continue;
        }
        List<CFAEdge> copy = new ArrayList<>(path);
        copy.add(leavingEdge);
        waitlist.add(copy);
      }
    }
    return finished;
  }

  private static class ConfigurableTargetState implements AbstractState, Targetable {

    private final boolean isTarget;

    ConfigurableTargetState(boolean pIsTarget) {
      isTarget = pIsTarget;
    }

    @Override
    public boolean isTarget() {
      return isTarget;
    }

    @Override
    public @NonNull Set<TargetInformation> getTargetInformation() throws IllegalStateException {
      return ImmutableSet.of();
    }
  }
}
