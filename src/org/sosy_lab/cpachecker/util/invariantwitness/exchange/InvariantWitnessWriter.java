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
import static org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef.OFFSET;
import static org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef.STARTLINE;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Edge;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.TransitionCondition;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Witness;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeFlag;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitness;
import org.sosy_lab.cpachecker.util.invariantwitness.WitnessToYamlWitnessConverter;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.LoopInvariantEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.ViolationSequenceEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.InformationRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.LocationRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.MetadataRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.ProducerRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.TaskRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.WaypointRecord;

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
      throw new InvalidConfigurationException(
          "Invariant export only supported for specific verificaiton task");
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
    String specification = pSpecification.getProperties().iterator().next().toString();

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
      for (InvariantWitness invariantWitness : invariantWitnesses) {
        LoopInvariantEntry entry = invariantWitnessToStoreEnty(invariantWitness);
        String entryYaml = mapper.writeValueAsString(ImmutableList.of(entry));
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

  public void exportErrorWitnessAsYamlWitness(Witness pWitness, Appendable pApp) {
    String startNode = pWitness.getEntryStateNodeId();
    List<WaypointRecord> waypoints = new ArrayList<>();
    GraphTraverser<String, YamlWitnessExportException> traverser =
        new GraphTraverser<>(startNode) {

          @Override
          protected String visit(String pSuccessor) {
            return pSuccessor;
          }

          @Override
          public Iterable<String> getSuccessors(String pCurrent)
              throws YamlWitnessExportException {
            Collection<Edge> outEdges = pWitness.getLeavingEdges().get(pCurrent);
            if (outEdges.size() > 2) {
              // we assume that violation witnesses only contain branchings at conditions,
              // and there should be only 2 successors in this case
              throw new YamlWitnessExportException(
                  "Expecting there to be at least two successors per node in a violation witness");
            }
            ImmutableList.Builder<String> builder = ImmutableList.builder();
            for (Edge e : outEdges) {
              TransitionCondition label = e.getLabel();
              Map<KeyDef, String> attrs = label.getMapping();
              String successor = e.getTarget();
              if (pWitness.getNodeFlags().get(successor).contains(NodeFlag.ISSINKNODE)) {
                continue; // we ignore sink nodes for now
              }
              if (attrs.get(FUNCTIONENTRY) != null) {
                continue; // we do not support function entry nodes currently
              } else if (attrs.get(CONTROLCASE) != null) {
                WaypointRecord waypoint = makeBranchingWaypoint(pWitness, attrs);
              } else if (attrs.get(ASSUMPTION) != null) {
                WaypointRecord waypoint = makeAssumptionWaypoint(pWitness, attrs);
                waypoints.add(waypoint);
              }
              builder.add(successor);
            }
            return builder.build();
          }

          private WaypointRecord
              makeBranchingWaypoint(Witness pWitness, Map<KeyDef, String> pAttrs) {
            InformationRecord info = null;
            LocationRecord location = null;
            WaypointRecord waypoint =
                new WaypointRecord(
                    WaypointRecord.WaypointType.BRANCHING,
                    WaypointRecord.WaypointAction.FOLLOW,
                    info,
                    location);
            return waypoint;
          }

          private WaypointRecord makeAssumptionWaypoint(
              Witness pWitness, Map<KeyDef, String> attrs) {
            String function = attrs.get(ASSUMPTIONSCOPE);
            int startline = Integer.parseInt(attrs.get(STARTLINE));
            int lineoffset = lineOffsetsByFile.get(pWitness.getOriginFile()).get(startline - 1);
            int column = Integer.parseInt(attrs.get(OFFSET)) - lineoffset;
            LocationRecord location =
                new LocationRecord(
                    pWitness.getOriginFile(),
                    getProgramHash(pWitness),
                    startline,
                    column,
                    function);
            InformationRecord inv = new InformationRecord(attrs.get(ASSUMPTION), null, "C");
            WaypointRecord waypoint =
                new WaypointRecord(
                    WaypointRecord.WaypointType.ASSUMPTION,
                    WaypointRecord.WaypointAction.FOLLOW,
                    inv,
                    location);
            return waypoint;
          }

          private String getProgramHash(Witness pWitness) {
            String programHash;
            try {
              programHash =
                  AutomatonGraphmlCommon.computeHash(pWitness.getCfa().getFileNames().get(0));
            } catch (IOException e1) {
              programHash = "";
              logger.logfException(WARNING, e1, "Could not compute program hash!");
            }
            return programHash;
          }
        };
    try {
      traverser.traverse();
    } catch (YamlWitnessExportException e) {
      logger.logfException(
          WARNING, e, "Problem encountered during export of error witness into yaml format");
      return;
    }
    ViolationSequenceEntry entry = new ViolationSequenceEntry(createMetadataRecord(), waypoints);
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
    LoopInvariantEntry entry = invariantWitnessToStoreEnty(invariantWitness);
    Path outFile = outDir.resolve(entry.getMetadata().getUuid() + ".invariantwitness.yaml");
    exportInvariantWitnesses(ImmutableList.of(invariantWitness), outFile);
  }

  private LoopInvariantEntry invariantWitnessToStoreEnty(InvariantWitness invariantWitness) {
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

    LoopInvariantEntry entry =
        new LoopInvariantEntry(metadata, location, invariant);

    return entry;
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

  public class YamlWitnessExportException extends Exception {
    private static final long serialVersionUID = -5647551194742587246L;

    public YamlWitnessExportException(String pReason) {
      super(pReason);
    }
  }
  abstract static class GraphTraverser<NodeType, E extends Throwable> {

    private List<NodeType> waitlist;
    private Set<NodeType> reached;

    public GraphTraverser(NodeType startNode) {
      waitlist = new ArrayList<>(List.of(startNode));
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

    public boolean stop(NodeType successor) throws E {
      return reached.contains(successor);
    }

    protected abstract NodeType visit(NodeType pSuccessor) throws E;

    public abstract Iterable<NodeType> getSuccessors(NodeType pCurrent) throws E;
  }
}
