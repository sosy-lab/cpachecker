// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange;

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
import java.util.List;
import java.util.Objects;
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
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitness;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.LoopInvariantEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.InformationRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.LocationRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.MetadataRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.ProducerRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.TaskRecord;

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
    final String architecture;
    switch (pMachineModel) {
      case LINUX32:
        architecture = "ILP32";
        break;
      case LINUX64:
        architecture = "LP64";
        break;
      default:
        architecture = pMachineModel.toString();
        break;
    }
    return architecture;
  }

  /**
   * Exports the given invariant witness in the invariant-witness format through the configured
   * channel. The export is (in most cases) an IO operation and thus expensive and might be
   * blocking.
   *
   * @param invariantWitness Witness to export
   * @throws IOException If writing the witness is not possible
   * @throws IllegalArgumentException If the invariant witness is (semantically - according to the
   *     definition of the invariant-witness format) invalid.
   */
  public void exportInvariantWitness(InvariantWitness invariantWitness) throws IOException {
    LoopInvariantEntry entry = invariantWitnessToStoreEnty(invariantWitness);
    String entryYaml = mapper.writeValueAsString(ImmutableList.of(entry));

    Path outFile = outDir.resolve(entry.getMetadata().getUuid() + ".invariantwitness.yaml");
    logger.log(Level.INFO, "Exporting invariant", outFile);
    try (Writer writer = IO.openOutputFile(outFile, Charset.defaultCharset())) {
      writer.write(entryYaml);
    }
  }

  private LoopInvariantEntry invariantWitnessToStoreEnty(InvariantWitness invariantWitness) {
    ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);
    String creationTime = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    final MetadataRecord metadata =
        new MetadataRecord(
            "0.1",
            UUID.randomUUID().toString(),
            creationTime,
            producerDescription,
            taskDescription);

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
        new LoopInvariantEntry("loop_invariant", metadata, location, invariant);

    return entry;
  }
}
