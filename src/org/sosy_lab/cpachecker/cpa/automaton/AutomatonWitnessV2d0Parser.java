// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.io.MoreFiles;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;

public class AutomatonWitnessV2d0Parser {

  private final LogManager logger;
  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;

  public AutomatonWitnessV2d0Parser(
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
    return AutomatonGraphmlParser.handlePotentiallyGZippedInput(
        MoreFiles.asByteSource(pInputFile), this::parseAutomatonFile, WitnessParseException::new);
  }

  /**
   * Parses a specification from an InputStream and returns the Automata found in the file.
   *
   * @param pInputStream the input stream to parse the witness from.
   * @throws InvalidConfigurationException if the configuration is invalid.
   * @throws IOException if there occurs an IOException while reading from the stream.
   * @return the automata representing the witnesses found in the stream.
   */
  private Automaton parseAutomatonFile(InputStream pInputStream)
      throws InvalidConfigurationException, IOException, InterruptedException {
    List<AbstractEntry> entries = AutomatonWitnessV2ParserUtils.parseYAML(pInputStream);
    if (AutomatonWitnessV2ParserUtils.getWitnessTypeIfYAML(entries)
        .orElseThrow()
        .equals(WitnessType.CORRECTNESS_WITNESS)) {
      AutomatonWitnessV2d0ParserCorrectness parser =
          switch (AutomatonWitnessV2ParserUtils.getWitnessVersion(entries).orElseThrow()) {
            case V2 ->
                new AutomatonWitnessV2d0ParserCorrectness(config, logger, shutdownNotifier, cfa);
            case V2d1 ->
                new AutomatonWitnessV2d1ParserCorrectness(config, logger, shutdownNotifier, cfa);
          };
      return parser.createCorrectnessAutomatonFromEntries(entries);
    } else {
      AutomatonWitnessViolationV2d0Parser parser =
          switch (AutomatonWitnessV2ParserUtils.getWitnessVersion(entries).orElseThrow()) {
            case V2 ->
                new AutomatonWitnessViolationV2d0Parser(config, logger, shutdownNotifier, cfa);
            case V2d1 ->
                new AutomatonWitnessViolationV2d1Parser(config, logger, shutdownNotifier, cfa);
          };
      return parser.createViolationAutomatonFromEntries(entries);
    }
  }
}
