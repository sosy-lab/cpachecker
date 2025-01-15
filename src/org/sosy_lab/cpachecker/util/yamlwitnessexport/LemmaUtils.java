// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LemmaEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LemmaSetEntry;

public class LemmaUtils {

  public static ImmutableSet<LemmaEntry> parseLemmasFromFile(Path lemmaFile, LogManager logger) {
    List<LemmaEntry> lemmaSet = new ArrayList<>();
    try {
      List<LemmaSetEntry> lemmaSetEntries = readLemmaFile(lemmaFile);
      for (LemmaSetEntry e : lemmaSetEntries) {
        List<LemmaEntry> lemmaEntries = e.getContent();
        lemmaSet.addAll(lemmaEntries);
      }
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not read lemmas from file");
    }
    return ImmutableSet.copyOf(lemmaSet);
  }

  public static List<LemmaSetEntry> readLemmaFile(Path lemmaFile) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.findAndRegisterModules();
    List<AbstractEntry> entries =
        Arrays.asList(mapper.readValue(lemmaFile.toFile(), AbstractEntry[].class));
    return entries.stream()
        .filter(entry -> entry instanceof LemmaSetEntry)
        .map(entry -> (LemmaSetEntry) entry)
        .toList();
  }
}
