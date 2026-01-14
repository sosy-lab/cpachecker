// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.Parser;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser.SvLibAstParseException;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser.SvLibParsingResult;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.exceptions.SvLibParserException;

public class SvLibToCfaParser implements Parser {

  private final LogManager logger;
  private final Configuration config;
  private final MachineModel machineModel;
  private final ShutdownNotifier shutdownNotifier;

  private final Timer parseTimer = new Timer();
  private final Timer cfaTimer = new Timer();

  private final SvLibCfaBuilder cfaBuilder;

  // This is only used by reflection, in order to unload the class
  // after its use and avoid keeping the large parsing classes in memory.
  @SuppressWarnings("unused")
  public SvLibToCfaParser(
      LogManager pLogger,
      Configuration pConfig,
      MachineModel pMachineModel,
      ShutdownNotifier pShutdownNotifier) {
    logger = pLogger;
    config = pConfig;
    machineModel = pMachineModel;
    shutdownNotifier = pShutdownNotifier;

    cfaBuilder = new SvLibCfaBuilder(logger, config, machineModel, shutdownNotifier);
  }

  private ParseResult buildCfaFromScript(SvLibParsingResult script) throws ParserException {
    cfaTimer.start();

    ParseResult result = cfaBuilder.buildCfaFromScript(script);

    cfaTimer.stop();
    return result;
  }

  @Override
  public ParseResult parseFiles(List<String> filenames)
      throws ParserException, IOException, InterruptedException, InvalidConfigurationException {
    if (filenames.size() != 1) {
      throw new SvLibParserException(
          "SV-LIB parser expects exactly one file, but got " + filenames.size());
    }

    Path path = Path.of(filenames.getFirst());

    parseTimer.start();
    SvLibParsingResult script;
    try {
      script = SvLibToAstParser.parseScript(path);
    } catch (SvLibAstParseException e) {
      throw new SvLibParserException("Failed converting the input file into AST objects", e);
    } finally {
      parseTimer.stop();
    }

    ParseResult parseResult = buildCfaFromScript(script);
    parseResult = parseResult.withFileNames(ImmutableList.of(path));

    return parseResult;
  }

  @Override
  public ParseResult parseString(Path filename, String code)
      throws ParserException, InterruptedException {
    parseTimer.start();
    SvLibParsingResult script;
    try {
      script = SvLibToAstParser.parseScript(filename, code);
    } catch (SvLibAstParseException e) {
      throw new SvLibParserException("Failed converting the input file into AST objects", e);
    } finally {
      parseTimer.stop();
    }

    ParseResult parseResult = buildCfaFromScript(script);

    return parseResult;
  }

  @Override
  public Timer getParseTime() {
    return parseTimer;
  }

  @Override
  public Timer getCFAConstructionTime() {
    return cfaTimer;
  }
}
