// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.io.MoreFiles;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LemmaSetEntry;

public class Lemma {
  public static void parseLemmaFromYAML(Path pathToLemmaFile) throws IOException {
    List<AbstractEntry> entries =
        AutomatonWitnessV2ParserUtils.parseYAML(
            MoreFiles.asByteSource(pathToLemmaFile).openStream());
    for (AbstractEntry entry : entries) {
      if (entry instanceof LemmaSetEntry lemmaSetEntry) {
        System.out.println("Parse Lemma From Yaml: " + lemmaSetEntry.value);
      }
    }
  }
}
