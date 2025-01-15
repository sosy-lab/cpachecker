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
import org.sosy_lab.cpachecker.util.yamlwitnessexport.LemmaUtils;

public class LemmaEntryTest {
  public static final String TEST_DIR_PATH = "test/lemma";

  @Test
  public void testParsingLemmaEntry() throws IOException {
    List<LemmaSetEntry> loadedEntries = testLemmaFile("witness.yml");
    for (AbstractEntry e : loadedEntries) {
      if (e instanceof LemmaSetEntry) {
        List<LemmaEntry> lemmaEntries = ((LemmaSetEntry) e).getContent();
        assertThat(lemmaEntries).hasSize(2);
        assertThat(lemmaEntries.get(0).getValue()).isEqualTo("MaxArray(A,0) = A[0]");
        assertThat(lemmaEntries.get(0).getFormat().toString()).isEqualTo("c_expression");
      }
    }
  }

  @Test
  public void testParseLemmas() {
    Path lemmaFile = Path.of(TEST_DIR_PATH, "witness.yml");
    ImmutableSet<LemmaEntry> lemmaSet = LemmaUtils.parseLemmasFromFile(lemmaFile, null);
    assertThat(lemmaSet).hasSize(2);
    assertThat(lemmaSet.asList().get(0).getValue()).isEqualTo("MaxArray(A,0) = A[0]");
    assertThat(lemmaSet.asList().get(0).getFormat().toString()).isEqualTo("c_expression");
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
