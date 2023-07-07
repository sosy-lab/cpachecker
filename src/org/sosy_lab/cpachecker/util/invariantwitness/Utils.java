// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness;

import com.google.common.io.MoreFiles;
import java.nio.file.Path;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonYAMLParser;

public class Utils {

  public static boolean isXML(Path pPath)
      throws InvalidConfigurationException, InterruptedException {
    return AutomatonGraphmlParser.handlePotentiallyGZippedInput(
        MoreFiles.asByteSource(pPath),
        (x) -> {
          try {
            AutomatonGraphmlParser.parseXML(x);
            return true;
          } catch (Exception e) {
            return false;
          }
        },
        WitnessParseException::new);
  }

  public static boolean isYAML(Path pPath)
      throws InvalidConfigurationException, InterruptedException {
    return AutomatonGraphmlParser.handlePotentiallyGZippedInput(
        MoreFiles.asByteSource(pPath),
        (x) -> {
          try {
            AutomatonYAMLParser.parseYAML(x);
            return true;
          } catch (Exception e) {
            return false;
          }
        },
        WitnessParseException::new);
  }

  public static boolean isYAMLWitness(Path pPath)
      throws InvalidConfigurationException, InterruptedException {
    return isYAML(pPath);
  }
}