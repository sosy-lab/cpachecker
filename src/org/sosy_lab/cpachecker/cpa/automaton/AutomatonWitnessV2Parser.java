// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessVersion;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;

/**
 * Parser for witnesses in one of the YAML witness format versions 2.0, 2.1 or 2.2.
 *
 * <p>This class only dispatches: it reads the entries, determines whether they describe a
 * correctness or a violation witness and which format version they use, and hands them to {@link
 * AutomatonWitnessCorrectnessV2Parser} or {@link AutomatonWitnessViolationV2Parser} respectively.
 */
public class AutomatonWitnessV2Parser {

  private final LogManager logger;
  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;

  public AutomatonWitnessV2Parser(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCFA) {
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    cfa = pCFA;
    config = pConfig;
  }

  /**
   * Parses a specification from a file and returns the Automata found in the file. This handles
   * potentially GZipped files.
   *
   * @param pInputFile the file to parse the witness from.
   * @return the automata representing the witnesses found in the file.
   * @throws InvalidConfigurationException if the configuration is invalid.
   * @throws InterruptedException if the parsing is interrupted.
   */
  public Automaton parseAutomatonFile(Path pInputFile)
      throws InvalidConfigurationException, InterruptedException {
    List<AbstractEntry> entries;
    try {
      entries = AutomatonWitnessV2ParserUtils.parseYAML(pInputFile);
    } catch (IOException e) {
      throw new WitnessParseException(e);
    }
    YAMLWitnessVersion witnessVersion =
        AutomatonWitnessV2ParserUtils.getWitnessVersion(entries).orElseThrow();
    if (AutomatonWitnessV2ParserUtils.getWitnessTypeIfYAML(entries)
        .orElseThrow()
        .equals(WitnessType.CORRECTNESS_WITNESS)) {
      return new AutomatonWitnessCorrectnessV2Parser(
              config, logger, shutdownNotifier, cfa, witnessVersion)
          .createCorrectnessAutomatonFromEntries(entries);
    } else {
      return new AutomatonWitnessViolationV2Parser(
              config, logger, shutdownNotifier, cfa, witnessVersion)
          .createViolationAutomatonFromEntries(entries);
    }
  }
}
