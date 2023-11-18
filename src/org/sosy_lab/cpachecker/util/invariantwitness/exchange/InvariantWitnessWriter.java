// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange;

import static java.util.logging.Level.WARNING;
import static org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef.ASSUMPTION;
import static org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef.ASSUMPTIONSCOPE;
import static org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef.CONTROLCASE;
import static org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef.FUNCTIONENTRY;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Edge;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.TransitionCondition;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Witness;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeFlag;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitness;
import org.sosy_lab.cpachecker.util.invariantwitness.WitnessToYamlWitnessConverter;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.InvariantSetEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.LocationInvariantEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.LoopInvariantEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.ViolationSequenceEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.InformationRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.InvariantRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.InvariantRecord.InvariantRecordType;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.LocationRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.MetadataRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.ProducerRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.SegmentRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.TaskRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.WaypointRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.WaypointRecord.WaypointType;

/**
 * Class to export invariants in the invariant-witness format.
 *
 * <p>The class exports invariants by calling {@link #exportInvariantWitness(InvariantWitness)}. The
 * invariants are exported in the invariant-witness format. The export requires IO. Consider calling
 * it in a separate thread.
 */
@Options(prefix = "invariantStore.export")
public final class InvariantWitnessWriter {
  private final ListMultimap<String, Integer> lineOffsetsByFile;
  private final LogManager logger;
  private final ObjectMapper mapper;

  private final ProducerRecord producerDescription;
  private final TaskRecord taskDescription;

  @Option(secure = true, description = "The directory where the invariants are stored.")
  @FileOption(FileOption.Type.OUTPUT_DIRECTORY)
  private Path outDir = Path.of("invariantWitnesses");

  @Option(
      secure = true,
      description =
          "If enabled, this option will not just output loop invariants,"
              + "but also general location invariants at other locations.")
  private boolean writeLocationInvariants = false;

  @Option(
      secure = true,
      description =
          "If enabled, this option will output the invariants in the now deprecated YAML format.")
  private boolean outputDeprecatedYAMLFormat = false;

  private InvariantWitnessWriter(
      Configuration pConfig,
      LogManager pLogger,
      ListMultimap<String, Integer> pLineOffsetsByFile,
      ProducerRecord pProducerDescription,
      TaskRecord pTaskDescription)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    logger = Objects.requireNonNull(pLogger);
    lineOffsetsByFile = ArrayListMultimap.create(pLineOffsetsByFile);
    mapper =
        new ObjectMapper(
            YAMLFactory.builder()
                .disable(Feature.WRITE_DOC_START_MARKER, Feature.SPLIT_LINES)
                .build());
    mapper.setSerializationInclusion(Include.NON_NULL);

    producerDescription = pProducerDescription;
    taskDescription = pTaskDescription;
  }

  /**
   * Returns a new instance of this class. The instance is configured according to the given config.
   *
   * @param pConfig Configuration with which the instance shall be created
   * @param pCFA CFA representing the program of the invariants that the instance writes
   * @param pLogger Logger
   * @return Instance of this class
   * @throws InvalidConfigurationException if the configuration is (semantically) invalid
   * @throws IOException if the program files can not be accessed (access is required to translate
   *     the location mapping)
   */
  public static InvariantWitnessWriter getWriter(
      Configuration pConfig, CFA pCFA, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException, IOException {
    if (pSpecification.getProperties().size() != 1) {
      pLogger.log(WARNING, "Invariant export only supported for specific verification task");
    }
    return new InvariantWitnessWriter(
        pConfig,
        pLogger,
        InvariantStoreUtil.getLineOffsetsByFile(pCFA.getFileNames()),
        new ProducerRecord(
            "CPAchecker",
            CPAchecker.getPlainVersion(),
            CPAchecker.getApproachName(pConfig),
            null,
            null),
        getTaskDescription(pCFA, pSpecification));
  }

  private static TaskRecord getTaskDescription(CFA pCFA, Specification pSpecification)
      throws IOException {
    List<Path> inputFiles = pCFA.getFileNames();
    ImmutableMap.Builder<String, String> inputFileHashes = ImmutableMap.builder();
    for (Path inputFile : inputFiles) {
      inputFileHashes.put(inputFile.toString(), AutomatonGraphmlCommon.computeHash(inputFile));
    }

    String specification =
        pSpecification.getProperties().stream()
            .map(Property::toString)
            .collect(Collectors.joining(" && "));

    return new TaskRecord(
        inputFiles.stream().map(Path::toString).collect(ImmutableList.toImmutableList()),
        inputFileHashes.buildOrThrow(),
        specification,
        getArchitecture(pCFA.getMachineModel()),
        pCFA.getLanguage().toString());
  }

  private static String getArchitecture(MachineModel pMachineModel) {
    final String architecture =
        switch (pMachineModel) {
          case LINUX32 -> "ILP32";
          case LINUX64 -> "LP64";
          default -> pMachineModel.toString();
        };
    return architecture;
  }

  /**
   * Exports the given invariant witnesses in the invariant-witness format through the configured
   * channel. The export is (in most cases) an IO operation and thus expensive and might be
   * blocking.
   *
   * @param invariantWitnesses Collection of the {@link InvariantWitness}es to export
   * @throws IllegalArgumentException If the invariant witness is (semantically - according to the
   *     definition of the invariant-witness format) invalid.
   */
  public void exportInvariantWitnesses(
      Collection<InvariantWitness> invariantWitnesses, Path outFile) {
    logger.logf(
        Level.INFO, "Exporting %d invariant witnesses to %s", invariantWitnesses.size(), outFile);
    try (Writer writer = IO.openOutputFile(outFile, Charset.defaultCharset())) {
      if (outputDeprecatedYAMLFormat) {
        for (InvariantWitness invariantWitness : invariantWitnesses) {
          InvariantEntry entry = invariantWitnessToStoreEnty(invariantWitness);
          String entryYaml = mapper.writeValueAsString(ImmutableList.of(entry));
          writer.write(entryYaml);
        }
      } else {
        InvariantSetEntry invariantSet = invariantWitnessesToInvariantEntry(invariantWitnesses);
        String entryYaml = mapper.writeValueAsString(ImmutableList.of(invariantSet));
        writer.write(entryYaml);
      }

    } catch (IOException e) {
      logger.logfException(WARNING, e, "Invariant witness export to %s failed.", outFile);
    }
  }

  public void exportProofWitnessAsInvariantWitnesses(Witness witness, Path outFile) {
    WitnessToYamlWitnessConverter conv =
        new WitnessToYamlWitnessConverter(logger, writeLocationInvariants);
    Collection<InvariantWitness> invariantWitnesses = conv.convertProofWitness(witness);
    exportInvariantWitnesses(invariantWitnesses, outFile);
  }

  private LocationRecord createLocationRecord(FileLocation fLoc, String functionName) {
    final String fileName = fLoc.getFileName().toString();
    final int lineNumber = fLoc.getStartingLineInOrigin();
    final int lineOffset = lineOffsetsByFile.get(fileName).get(lineNumber - 1);
    final int offsetInLine = fLoc.getNodeOffset() - lineOffset + 1;

    LocationRecord location =
        new LocationRecord(fileName, "file_hash", lineNumber, offsetInLine, functionName);
    return location;
  }

  @SuppressWarnings("unused")
  public void exportErrorWitnessAsYamlWitness(CounterexampleInfo pCex, Appendable pApp) {
    CFAPathWithAssumptions cexPathWithAssignments = pCex.getCFAPathWithAssignments();
    List<SegmentRecord> segments = new ArrayList<>();

    for (CFAEdgeWithAssumptions edgeWithAssumptions : cexPathWithAssignments) {
      CFAEdge edge = edgeWithAssumptions.getCFAEdge();
      // See if the edge contains an assignment of a VerifierNondet call
      if (CFAUtils.assignsNondetFunctionCall(edge)) {
        List<WaypointRecord> waypoints = new ArrayList<>();
        for (AExpressionStatement statement : edgeWithAssumptions.getExpStmts()) {
          InformationRecord informationRecord =
              new InformationRecord(statement.toString(), null, "C");
          LocationRecord location =
              createLocationRecord(edge.getFileLocation(), edge.getPredecessor().getFunctionName());
          waypoints.add(
              new WaypointRecord(
                  WaypointRecord.WaypointType.ASSUMPTION,
                  WaypointRecord.WaypointAction.FOLLOW,
                  informationRecord,
                  location));
        }
        if (waypoints.size() > 0) {
          segments.add(new SegmentRecord(waypoints));
        }
      }
    }

    // Add target
    CFAEdge lastEdge = cexPathWithAssignments.get(cexPathWithAssignments.size() - 1).getCFAEdge();
    segments.add(
        SegmentRecord.ofOnlyElement(
            new WaypointRecord(
                WaypointRecord.WaypointType.TARGET,
                WaypointRecord.WaypointAction.FOLLOW,
                null,
                createLocationRecord(
                    lastEdge.getFileLocation(), lastEdge.getPredecessor().getFunctionName()))));

    ViolationSequenceEntry entry = new ViolationSequenceEntry(createMetadataRecord(), segments);
    try {
      pApp.append(mapper.writeValueAsString(ImmutableList.of(entry)));
    } catch (IOException e) {
      logger.logException(WARNING, e, "Failed to write yaml witness to file");
    }
  }

  public void exportErrorWitnessAsYamlWitness(Witness pWitness, Appendable pApp) {
    String startNode = pWitness.getEntryStateNodeId();
    List<SegmentRecord> segments = new ArrayList<>();
    GraphTraverser<String, YamlWitnessExportException> traverser =
        new ViolationWitnessToYamlWitnessTraverser(startNode, segments, pWitness);
    try {
      traverser.traverse();
      if (segments.isEmpty()) {
        throw new YamlWitnessExportException(
            "Empty waypoint sequence generated for yaml witness, cannot export");
      }
    } catch (YamlWitnessExportException e) {
      logger.logfException(
          WARNING, e, "Problem encountered during export of error witness into yaml format");
      return;
    }

    // Change the type of the last waypoint to TARGET, and remove constraints if any:
    SegmentRecord last = segments.remove(segments.size() - 1);
    segments.add(
        SegmentRecord.ofOnlyElement(
            last.getSegment().get(0).withType(WaypointType.TARGET).withConstraint(null)));

    // We only added FUNCTION_ENTER nodes for constructing the TARGET node above, so for now we
    // remove them again here:
    segments.removeIf(x -> x.getSegment().get(0).getType().equals(WaypointType.FUNCTION_ENTER));
    // TODO: the above only checks the first element in the segment which will become a problem
    // once we have segments with multiple waypoints inside

    ViolationSequenceEntry entry = new ViolationSequenceEntry(createMetadataRecord(), segments);
    try {
      pApp.append(mapper.writeValueAsString(ImmutableList.of(entry)));
    } catch (IOException e) {
      logger.logException(WARNING, e, "Failed to write yaml witness to file");
    }
  }

  /**
   * Exports the given invariant witness in the invariant-witness format through the configured
   * channel. The export is (in most cases) an IO operation and thus expensive and might be
   * blocking.
   *
   * @param invariantWitness Witness to export
   * @throws IllegalArgumentException If the invariant witness is (semantically - according to the
   *     definition of the invariant-witness format) invalid.
   */
  public void exportInvariantWitness(InvariantWitness invariantWitness) {
    InvariantEntry entry = invariantWitnessToStoreEnty(invariantWitness);
    Path outFile = outDir.resolve(entry.getMetadata().getUuid() + ".invariantwitness.yaml");
    exportInvariantWitnesses(ImmutableList.of(invariantWitness), outFile);
  }

  private InvariantEntry invariantWitnessToStoreEnty(InvariantWitness invariantWitness) {
    final MetadataRecord metadata = createMetadataRecord();

    final String fileName = invariantWitness.getLocation().getFileName().toString();
    final int lineNumber = invariantWitness.getLocation().getStartingLineInOrigin();
    final int lineOffset = lineOffsetsByFile.get(fileName).get(lineNumber - 1);
    final int offsetInLine = invariantWitness.getLocation().getNodeOffset() - lineOffset;

    LocationRecord location =
        new LocationRecord(
            fileName,
            "file_hash",
            lineNumber,
            offsetInLine,
            invariantWitness.getNode().getFunctionName());

    InformationRecord invariant =
        new InformationRecord(invariantWitness.getFormula().toString(), "assertion", "C");

    InvariantEntry entry;
    if (invariantWitness.getNode().isLoopStart()) {
      entry = new LoopInvariantEntry(metadata, location, invariant);
    } else {
      entry = new LocationInvariantEntry(metadata, location, invariant);
    }

    return entry;
  }

  private InvariantSetEntry invariantWitnessesToInvariantEntry(
      Collection<InvariantWitness> pInvariantWitnesses) {
    final MetadataRecord metadata = createMetadataRecord();

    ImmutableList.Builder<InvariantRecord> invariantRecordBuilder = ImmutableList.builder();
    for (InvariantWitness invariantWitness : pInvariantWitnesses) {
      final String fileName = invariantWitness.getLocation().getFileName().toString();
      final int lineNumber = invariantWitness.getLocation().getStartingLineInOrigin();
      final int lineOffset = lineOffsetsByFile.get(fileName).get(lineNumber - 1);
      final int offsetInLine = invariantWitness.getLocation().getNodeOffset() - lineOffset;

      LocationRecord location =
          new LocationRecord(
              fileName,
              "file_hash",
              lineNumber,
              offsetInLine,
              invariantWitness.getNode().getFunctionName());

      if (invariantWitness.getNode().isLoopStart()) {
        invariantRecordBuilder.add(
            new InvariantRecord(
                invariantWitness.getFormula().toString(),
                InvariantRecordType.LOOP_INVARIANT.getKeyword(),
                "c_expression",
                location));
      } else {
        invariantRecordBuilder.add(
            new InvariantRecord(
                invariantWitness.getFormula().toString(),
                InvariantRecordType.LOCATION_INVARIANT.getKeyword(),
                "c_expression",
                location));
      }
    }

    return new InvariantSetEntry(metadata, invariantRecordBuilder.build());
  }

  private MetadataRecord createMetadataRecord() {
    ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);
    String creationTime = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    final MetadataRecord metadata =
        new MetadataRecord(
            "0.1",
            UUID.randomUUID().toString(),
            creationTime,
            producerDescription,
            taskDescription);
    return metadata;
  }

  private final class ViolationWitnessToYamlWitnessTraverser
      extends GraphTraverser<String, YamlWitnessExportException> {
    private final List<SegmentRecord> segments;
    private final Witness witness;

    private ViolationWitnessToYamlWitnessTraverser(
        String pStartNode, List<SegmentRecord> pSegments, Witness pWitness) {
      super(pStartNode);
      segments = pSegments;
      witness = pWitness;
    }

    @Override
    protected String visit(String pSuccessor) {
      return pSuccessor;
    }

    @Override
    public Iterable<String> getSuccessors(String pCurrent) throws YamlWitnessExportException {
      Collection<Edge> outEdges = witness.getLeavingEdges().get(pCurrent);
      if (outEdges.size() > 2) {
        // we assume that violation witnesses only contain branchings at conditions,
        // and there should be only 2 successors in this case
        logger.logf(
            WARNING,
            "Expecting there to be at least two successors per node in a violation witness, but"
                + " found %d, which might indicate a branching condition with a disjunction or"
                + " conjunction",
            outEdges.size());
      }
      ImmutableList.Builder<String> builder = ImmutableList.builder();
      for (Edge e : outEdges) {
        TransitionCondition label = e.getLabel();
        Map<KeyDef, String> attrs = label.getMapping();
        String successor = e.getTarget();
        builder.add(successor);
        if (witness.getNodeFlags().get(successor).contains(NodeFlag.ISSINKNODE)) {
          continue; // we ignore sink nodes for now
        }
        Optional<LocationRecord> location = createLocationRecord(attrs, e);
        if (location.isEmpty()) {
          continue;
        }
        if (attrs.get(FUNCTIONENTRY) != null) {
          SegmentRecord segement = makeFunctionEntrySegment(attrs, location.orElseThrow());
          segments.add(segement);
        } else if (attrs.get(CONTROLCASE) != null) {
          Optional<SegmentRecord> segment = handleControlEdge(attrs, location.orElseThrow());
          segment.ifPresent(segments::add);
        } else if (attrs.get(ASSUMPTION) != null) {
          SegmentRecord waypoint = makeAssumptionSegment(attrs, location.orElseThrow());
          segments.add(waypoint);
        }
      }
      return builder.build();
    }

    private Optional<SegmentRecord> handleControlEdge(
        Map<KeyDef, String> attrs, LocationRecord location) {
      Preconditions.checkState(
          ImmutableSet.of("condition-true", "condition-false").contains(attrs.get(CONTROLCASE)));
      InformationRecord info =
          new InformationRecord(
              attrs.get(CONTROLCASE).equals("condition-true") ? "true" : "false", null, null);
      WaypointRecord waypoint =
          new WaypointRecord(
              WaypointRecord.WaypointType.BRANCHING,
              WaypointRecord.WaypointAction.FOLLOW,
              info,
              location);
      return Optional.of(new SegmentRecord(ImmutableList.of(waypoint)));
    }

    private SegmentRecord makeAssumptionSegment(
        Map<KeyDef, String> attrs, LocationRecord location) {
      InformationRecord inv = new InformationRecord(attrs.get(ASSUMPTION), null, "C");
      WaypointRecord waypoint =
          new WaypointRecord(
              WaypointRecord.WaypointType.ASSUMPTION,
              WaypointRecord.WaypointAction.FOLLOW,
              inv,
              location);
      return new SegmentRecord(ImmutableList.of(waypoint));
    }

    private SegmentRecord makeFunctionEntrySegment(
        @SuppressWarnings("unused") Map<KeyDef, String> attrs, LocationRecord location) {
      WaypointRecord waypoint =
          new WaypointRecord(
              WaypointRecord.WaypointType.FUNCTION_ENTER,
              WaypointRecord.WaypointAction.FOLLOW,
              null,
              location);
      return new SegmentRecord(ImmutableList.of(waypoint));
    }

    private Optional<LocationRecord> createLocationRecord(Map<KeyDef, String> attrs, Edge edge) {
      CFAEdge e = tryFindNextEdgeInWitness(edge);
      if (e == null) {
        return Optional.empty();
      }
      int offset = e.getFileLocation().getNodeOffset();
      int line = e.getFileLocation().getStartingLineInOrigin();
      if (line == 0) {
        return Optional.empty();
      }
      int lineoffset = lineOffsetsByFile.get(witness.getOriginFile()).get(line - 1);
      int column = offset - lineoffset;
      @Nullable String function = attrs.get(ASSUMPTIONSCOPE);
      LocationRecord location =
          new LocationRecord(witness.getOriginFile(), getProgramHash(), line, column, function);
      return Optional.of(location);
    }

    private @Nullable CFAEdge tryFindNextEdgeInWitness(Edge edge) {
      List<CFAEdge> cfaEdges = witness.getCFAEdgeFor(edge);
      if (cfaEdges.size() != 1) {
        return null;
      }
      CFAEdge cfaEdge = cfaEdges.get(0);
      Set<CFAEdge> linearReach = forwardLinearReach(cfaEdge);
      String target = edge.getTarget();
      for (Edge e : witness.getLeavingEdges().get(target)) {
        String successor = e.getTarget();
        if (witness.getNodeFlags().get(successor).contains(NodeFlag.ISSINKNODE)) {
          continue;
        }
        List<CFAEdge> currentlyRepresentedCFAEdges = witness.getCFAEdgeFor(e);
        Set<CFAEdge> intersection = new HashSet<>(currentlyRepresentedCFAEdges);
        intersection.retainAll(linearReach);
        if (!intersection.isEmpty()) {
          return intersection.iterator().next();
        }
      }

      // We only reach this place if the target node does not have any successors
      // If we are calling the reach error function, we want to export this as node
      // since this will likely be the target node
      if (cfaEdge.getRawStatement().equals("reach_error();")) {
        return cfaEdge;
      }

      return null;
    }

    private Set<CFAEdge> forwardLinearReach(CFAEdge edge) {
      CFAEdge current = edge;
      ImmutableSet.Builder<CFAEdge> builder = ImmutableSet.builder();
      while (CFAUtils.leavingEdges(current.getSuccessor()).size() == 1) {
        current = CFAUtils.leavingEdges(current.getSuccessor()).first().get();
        builder.add(current);
      }
      return builder.build();
    }

    private String getProgramHash() {
      String programHash;
      try {
        programHash = AutomatonGraphmlCommon.computeHash(witness.getCfa().getFileNames().get(0));
      } catch (IOException e1) {
        programHash = "";
        logger.logfException(WARNING, e1, "Could not compute program hash!");
      }
      return programHash;
    }
  }

  public static class YamlWitnessExportException extends Exception {
    private static final long serialVersionUID = -5647551194742587246L;

    public YamlWitnessExportException(String pReason) {
      super(pReason);
    }
  }

  abstract static class GraphTraverser<NodeType, E extends Throwable> {

    private List<NodeType> waitlist;
    private Set<NodeType> reached;

    public GraphTraverser(NodeType startNode) {
      waitlist = new ArrayList<>(ImmutableList.of(startNode));
      reached = new HashSet<>(waitlist);
    }

    protected List<NodeType> getWaitlist() {
      return waitlist;
    }

    protected Set<NodeType> getReached() {
      return reached;
    }

    public void traverse() throws E {
      while (!waitlist.isEmpty()) {
        NodeType current = waitlist.remove(0);
        for (NodeType successor : getSuccessors(current)) {
          successor = visit(successor);
          if (!stop(successor)) {
            reached.add(successor);
            waitlist.add(successor);
          }
        }
      }
    }

    /**
     * This method returns true whenever a node's successors shall not be explored any further. The
     * default behavior is to return true if the successor is already in the set of reached nodes.
     *
     * @throws E in an exceptional state needs to be communicated to the caller
     */
    protected boolean stop(NodeType successor) throws E {
      return reached.contains(successor);
    }

    protected abstract NodeType visit(NodeType pSuccessor) throws E;

    protected abstract Iterable<NodeType> getSuccessors(NodeType pCurrent) throws E;
  }
}
