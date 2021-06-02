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
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitness;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitnessLocation;

public class InvariantWitnessWriter {
  private static final String FORMAT_VERSION = "0.1";

  private static class Entry {
    private final String entryType;
    private final EntryMetadata metadata;
    private final EntryLocation location;
    private final EntryLoopInvariant loopInvariant;

    public Entry(
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
      return this.entryType;
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

  private static class EntryMetadata {
    private final String formatVerison;

    public EntryMetadata(String formatVerison) {
      this.formatVerison = formatVerison;
    }

    public String getFormatVerison() {
      return this.formatVerison;
    }
  }

  private static class EntryLocation {
    private final String fileName;
    private final String fileHash;
    private final int line;
    private final int column;
    private final String function;

    public EntryLocation(String fileName, String fileHash, int line, int column, String function) {
      this.fileName = fileName;
      this.fileHash = fileHash;
      this.line = line;
      this.column = column;
      this.function = function;
    }

    public String getFileName() {
      return this.fileName;
    }

    public String getFileHash() {
      return this.fileHash;
    }

    public int getLine() {
      return this.line;
    }

    public int getColumn() {
      return this.column;
    }

    public String getFunction() {
      return this.function;
    }
  }

  private static class EntryLoopInvariant {
    private String string;
    private String type;
    private String format;

    public EntryLoopInvariant(String string, String type, String format) {
      this.string = string;
      this.type = type;
      this.format = format;
    }

    public String getString() {
      return this.string;
    }

    public String getType() {
      return this.type;
    }

    public String getFormat() {
      return this.format;
    }
  }

  private InvariantWitnessWriter() {}

  public static InvariantWitnessWriter getWriter() {
    return new InvariantWitnessWriter();
  }

  public String invariantWitnessToYamlEntry(InvariantWitness invariantWitness) {
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
