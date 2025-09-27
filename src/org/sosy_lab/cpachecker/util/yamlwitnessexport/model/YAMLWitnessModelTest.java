// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import org.junit.Test;

public class YAMLWitnessModelTest {

  public static final String TEST_DIR_PATH = "test/witness/";

  @Test
  public void testParsingViolationWitness()
      throws JsonParseException, JsonMappingException, IOException {
    Queue<AbstractEntry> loadedEntries = testParsingFile("violation-witness.yml");
    for (AbstractEntry e : loadedEntries) {
      if (e instanceof ViolationSequenceEntry violationSequenceEntry) {
        List<SegmentRecord> sequence = violationSequenceEntry.getContent();
        assertThat(sequence).hasSize(5);
        assertThat(sequence.getFirst().getSegment().size()).isAtLeast(1);
        assertThat(sequence.getFirst().getSegment().getFirst().getConstraint().getValue())
            .isEqualTo("(x >= 1024U)");
      }
    }
  }

  @Test
  public void testRoundTripParsingOfViolationWitness()
      throws JsonParseException, JsonMappingException, IOException {
    Queue<AbstractEntry> entries1 = testParsingFile("violation-witness.yml");
    assertThat(entries1).hasSize(1);
    assertThat(entries1.element()).isInstanceOf(ViolationSequenceEntry.class);
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    String yamlString = mapper.writeValueAsString(ImmutableList.of(entries1.element()));
    ViolationSequenceEntry entry1 = (ViolationSequenceEntry) entries1.element();

    JavaType entryType =
        mapper.getTypeFactory().constructCollectionType(List.class, AbstractEntry.class);
    List<AbstractEntry> entries2 = mapper.readValue(yamlString, entryType);
    assertThat(entries2).hasSize(1);
    assertThat(entries2.getFirst()).isInstanceOf(ViolationSequenceEntry.class);
    ViolationSequenceEntry entry2 = (ViolationSequenceEntry) entries2.getFirst();

    // actual check of both representations
    assertThat(entry1.getMetadata()).isEqualTo(entry2.getMetadata());
    // TODO: check also the content for equality once we have decided on default values in case keys
    // are missing:
    // assertThat(entry1.getContent()).isEqualTo(entry2.getContent());
  }

  private Queue<AbstractEntry> testParsingFile(String filename)
      throws JsonParseException, JsonMappingException, IOException {
    File yamlWitness = Path.of(TEST_DIR_PATH, filename).toFile();
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    JavaType entryType =
        mapper.getTypeFactory().constructCollectionType(List.class, AbstractEntry.class);
    List<AbstractEntry> entries = mapper.readValue(yamlWitness, entryType);
    Queue<AbstractEntry> loadedEntries = new ArrayDeque<>(entries);
    return loadedEntries;
  }
}
