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
import com.google.errorprone.annotations.Immutable;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.UUID;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitness;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitnessLocation;

@Options(prefix = "invariantStore.export")
public final class InvariantWitnessWriter {
  private static final String FORMAT_VERSION = "0.1";

  @Option(
      secure = true,
      required = true,
      description = "The directory where the invariants are stored.")
  @FileOption(FileOption.Type.OUTPUT_DIRECTORY)
  private Path outDir;

  @Immutable
  private static class Entry {
    public final String entryType;
    public final EntryMetadata metadata;
    public final EntryLocation location;
    public final EntryLoopInvariant loopInvariant;

    private Entry(
        String entryType,
        EntryMetadata metadata,
        EntryLocation location,
        EntryLoopInvariant loopInvariant) {
      this.entryType = entryType;
      this.metadata = metadata;
      this.location = location;
      this.loopInvariant = loopInvariant;
    }

    public String getEntryType() {
      return entryType;
    }

    public EntryMetadata getMetadata() {
      return metadata;
    }

    public EntryLocation getLocation() {
      return location;
    }

    public EntryLoopInvariant getLoopInvariant() {
      return loopInvariant;
    }
  }

  @Immutable
  private static class EntryMetadata {
    public final String formatVerison;

    private EntryMetadata(String formatVerison) {
      this.formatVerison = formatVerison;
    }

    public String getFormatVerison() {
      return formatVerison;
    }
  }

  @Immutable
  private static class EntryLocation {
    public final String fileName;
    public final String fileHash;
    public final int line;
    public final int column;
    public final String function;

    private EntryLocation(String fileName, String fileHash, int line, int column, String function) {
      this.fileName = fileName;
      this.fileHash = fileHash;
      this.line = line;
      this.column = column;
      this.function = function;
    }

    public String getFileName() {
      return fileName;
    }

    public String getFileHash() {
      return fileHash;
    }

    public int getLine() {
      return line;
    }

    public int getColumn() {
      return column;
    }

    public String getFunction() {
      return function;
    }
  }

  @Immutable
  private static class EntryLoopInvariant {
    public final String string;
    public final String type;
    public final String format;

    private EntryLoopInvariant(String string, String type, String format) {
      this.string = string;
      this.type = type;
      this.format = format;
    }

    public String getString() {
      return string;
    }

    public String getType() {
      return type;
    }

    public String getFormat() {
      return format;
    }
  }

  private InvariantWitnessWriter(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  public static InvariantWitnessWriter getWriter(Configuration pConfig)
      throws InvalidConfigurationException {
    return new InvariantWitnessWriter(pConfig);
  }

  public void exportInvariantWitness(InvariantWitness invariantWitness) throws IOException {
    UUID uuid = UUID.randomUUID();
    Path outFile = outDir.resolve(uuid + ".invariantwitness.yaml");

    String entry = invariantWitnessToYamlEntry(invariantWitness);
    try (Writer writer = IO.openOutputFile(outFile, Charset.defaultCharset())) {
      writer.write(entry);
    }
  }

  private String invariantWitnessToYamlEntry(InvariantWitness invariantWitness) {
    EntryMetadata metadata = new EntryMetadata(FORMAT_VERSION);

    InvariantWitnessLocation witnessLocation = invariantWitness.getLocation();
    EntryLocation location =
        new EntryLocation(
            witnessLocation.getFileName(),
            witnessLocation.getFileHash(),
            witnessLocation.getLine(),
            witnessLocation.getColumn(),
            witnessLocation.getFunctionName());

    EntryLoopInvariant invariant =
        new EntryLoopInvariant(invariantWitness.getFormula().toString(), "assertion", "C");

    Entry entry = new Entry("loop_invariant", metadata, location, invariant);

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    try {
      return mapper.writeValueAsString(entry);
    } catch (JsonProcessingException e) {
      throw new AssertionError(e);
    }
  }
}

