// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.google.common.collect.Table;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
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
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitness;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.InvariantStoreEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.InvariantStoreEntryLocation;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.InvariantStoreEntryLoopInvariant;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.InvariantStoreEntryMetadata;

@Options(prefix = "invariantStore.export")
public final class InvariantWitnessWriter {
  private final Table<String, Integer, Integer> lineOffsetsByFile;
  private final LogManager logger;

  @Option(
      secure = true,
      required = true,
      description = "The directory where the invariants are stored.")
  @FileOption(FileOption.Type.OUTPUT_DIRECTORY)
  private Path outDir;

  private InvariantWitnessWriter(Configuration pConfig, CFA pCFA, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    lineOffsetsByFile = InvariantStoreUtil.getLineOffsetsByFile(pCFA.getFileNames());
  }

  public static InvariantWitnessWriter getWriter(
      Configuration pConfig, CFA pCFA, LogManager pLogger) throws InvalidConfigurationException {
    return new InvariantWitnessWriter(pConfig, pCFA, pLogger);
  }

  public void exportInvariantWitness(InvariantWitness invariantWitness) throws IOException {
    UUID uuid = UUID.randomUUID();
    Path outFile = outDir.resolve(uuid + ".invariantwitness.yaml");

    logger.log(Level.INFO, "Exporting invariant " + uuid);
    String entry = invariantWitnessToYamlEntry(invariantWitness);
    try (Writer writer = IO.openOutputFile(outFile, Charset.defaultCharset())) {
      writer.write(entry);
    }
  }

  private String invariantWitnessToYamlEntry(InvariantWitness invariantWitness) {
    final InvariantStoreEntryMetadata metadata = new InvariantStoreEntryMetadata();

    final String fileName = invariantWitness.getLocation().getFileName();
    final int lineNumber = invariantWitness.getLocation().getStartingLineInOrigin();
    final int lineOffset = lineOffsetsByFile.get(fileName, lineNumber);
    final int offsetInLine = invariantWitness.getLocation().getNodeOffset() - lineOffset;

    InvariantStoreEntryLocation location =
        new InvariantStoreEntryLocation(
            fileName,
            "file_hash",
            lineNumber,
            offsetInLine,
            invariantWitness.getNode().getFunctionName());

    InvariantStoreEntryLoopInvariant invariant =
        new InvariantStoreEntryLoopInvariant(
            invariantWitness.getFormula().toString(), "assertion", "C");

    InvariantStoreEntry entry =
        new InvariantStoreEntry("loop_invariant", metadata, location, invariant);

    ObjectMapper mapper =
        new ObjectMapper(YAMLFactory.builder().disable(Feature.WRITE_DOC_START_MARKER).build());
    try {
      return mapper.writeValueAsString(entry);
    } catch (JsonProcessingException e) {
      throw new AssertionError(e);
    }
  }
}

