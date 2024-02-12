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
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.AbstractEntry;

@Options(prefix = "witness")
public class AutomatonWitnessV2Parser {

  @Option(
      secure = true,
      name = "matchOffsetsWhenCreatingViolationAutomatonFromWitnessV2",
      description =
          "If true the offsets will be matched when creating an automaton to validate Violation"
              + " witnesses. If false only the lines will be matched.")
  private boolean matchOffsetsWhenCreatingViolationAutomatonFromWitnessV2 = false;

  private final LogManager logger;
  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;

  public AutomatonWitnessV2Parser(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCFA)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    cfa = pCFA;
    config = pConfig;
  }

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
      AutomatonWitnessV2ParserCorrectness parser =
          new AutomatonWitnessV2ParserCorrectness(config, logger, shutdownNotifier, cfa);
      return parser.createCorrectnessAutomatonFromEntries(entries);
    } else {
      return createViolationAutomatonFromEntries(entries);
    }
  }

  private Automaton createViolationAutomatonFromEntries(List<AbstractEntry> pEntries)
      throws InterruptedException, InvalidConfigurationException, IOException {
    if (matchOffsetsWhenCreatingViolationAutomatonFromWitnessV2) {
      AutomatonWitnessV2ParserWithOffsets parser =
          new AutomatonWitnessV2ParserWithOffsets(config, logger, shutdownNotifier, cfa);
      return parser.createViolationAutomatonFromEntriesMatchingOffsets(pEntries);
    } else {
      AutomatonWitnessV2ParserViolation parser =
          new AutomatonWitnessV2ParserViolation(config, logger, shutdownNotifier, cfa);
      return parser.createViolationAutomatonFromEntriesMatchingLines(pEntries);
    }
  }
}
