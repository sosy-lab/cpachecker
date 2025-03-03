// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils;

public class LemmaEntryTest {
  public static final String TEST_DIR_PATH = "test/lemma";

  @Test
  public void testParsingLemmaEntry() throws IOException {
    List<LemmaSetEntry> loadedEntries = testLemmaFile("witness.yml");
    for (AbstractEntry e : loadedEntries) {
      if (e instanceof LemmaSetEntry) {
        List<LemmaEntry> lemmaEntries = ((LemmaSetEntry) e).getContent();
        assertThat(lemmaEntries).hasSize(2);
        assertThat(lemmaEntries.get(0).getValue()).isEqualTo("ACSL(MaxArray(A,0)) == A[0]");
        assertThat(lemmaEntries.get(0).getFormat().toString()).isEqualTo("c_expression");
      }
    }
  }

  @Test
  public void testParseLemmas() throws IOException {
    Path lemmaFile = Path.of(TEST_DIR_PATH, "witness.yml");
    List<LemmaSetEntry> lemmaSetEntries = AutomatonWitnessV2ParserUtils.readLemmaFile(lemmaFile);
    ImmutableSet<LemmaEntry> lemmaSet =
        AutomatonWitnessV2ParserUtils.parseLemmasFromFile(lemmaSetEntries);
    assertThat(lemmaSet).hasSize(2);
    assertThat(lemmaSet.asList().get(0).getValue()).isEqualTo("ACSL(MaxArray(A,0)) == A[0]");
    assertThat(lemmaSet.asList().get(0).getFormat().toString()).isEqualTo("c_expression");
  }

  @Test
  public void testParseDeclarations() throws IOException {
    Path witnessFile = Path.of(TEST_DIR_PATH, "witness.yml");
    List<LemmaSetEntry> lemmaSetEntries = AutomatonWitnessV2ParserUtils.readLemmaFile(witnessFile);
    ImmutableSet<String> declarations =
        AutomatonWitnessV2ParserUtils.parseDeclarationsFromFile(lemmaSetEntries);
    assertThat(declarations).hasSize(3);
    assertThat(declarations.asList().get(0)).isEqualTo("int MaxArray(int* A, int I)");
    assertThat(declarations.asList().get(1)).isEqualTo("int* A");
  }

  private List<LemmaSetEntry> testLemmaFile(String filename) throws IOException {
    File lemmaFile = Path.of(TEST_DIR_PATH, filename).toFile();
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.findAndRegisterModules();
    List<AbstractEntry> entries = Arrays.asList(mapper.readValue(lemmaFile, AbstractEntry[].class));
    return entries.stream()
        .filter(entry -> entry instanceof LemmaSetEntry)
        .map(entry -> (LemmaSetEntry) entry)
        .toList();
  }
}
