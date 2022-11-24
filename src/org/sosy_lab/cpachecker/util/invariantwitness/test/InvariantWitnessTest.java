// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.test;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import org.junit.Test;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.LoopInvariantCertificateEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.LoopInvariantEntry;

public class InvariantWitnessTest {

  public static final String TEST_DIR_PATH = "test/witness/";

  @Test
  public void testParsingEntries() throws JsonParseException, JsonMappingException, IOException {
    File yamlWitness = Path.of(TEST_DIR_PATH, "loop_invariant_and_certificate.yml").toFile();
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    JavaType entryType =
        mapper.getTypeFactory().constructCollectionType(List.class, AbstractEntry.class);
    List<AbstractEntry> entries = mapper.readValue(yamlWitness, entryType);
    Queue<AbstractEntry> loadedEntries = new ArrayDeque<>();
    loadedEntries.addAll(entries);
    for (AbstractEntry e : loadedEntries) {
      if (e instanceof LoopInvariantEntry) {
        LoopInvariantEntry invEntry = (LoopInvariantEntry) e;
        assertThat(invEntry.getLocation().getFileName()).isEqualTo("multivar_1-1.c");
      } else if (e instanceof LoopInvariantCertificateEntry) {
        LoopInvariantCertificateEntry invCertEntry = (LoopInvariantCertificateEntry) e;
        assertThat(invCertEntry.getCertification().getString()).isEqualTo("confirmed");
      }
    }
  }
}
