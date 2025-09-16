// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.k3;

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
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Script;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.K3ToAstParser;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.K3ToAstParser.K3AstParseException;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.ParserException;

public class K3ToCfaParser implements Parser {

  private final LogManager logger;
  private final Configuration config;
  private final MachineModel machineModel;
  private final ShutdownNotifier shutdownNotifier;

  private final Timer parseTimer = new Timer();
  private final Timer cfaTimer = new Timer();

  private final K3CfaBuilder cfaBuilder;

  // This is only used by reflection, in order to unload the class
  // after its use and avoid keeping the large parsing classes in memory.
  @SuppressWarnings("unused")
  public K3ToCfaParser(
      LogManager pLogger,
      Configuration pConfig,
      MachineModel pMachineModel,
      ShutdownNotifier pShutdownNotifier) {
    logger = pLogger;
    config = pConfig;
    machineModel = pMachineModel;
    shutdownNotifier = pShutdownNotifier;

    cfaBuilder = new K3CfaBuilder(logger, config, machineModel, shutdownNotifier);
  }

  private ParseResult buildCfaFromScript(K3Script script)
      throws ParserException, InterruptedException {
    cfaTimer.start();

    ParseResult result = cfaBuilder.buildCfaFromScript(script);

    cfaTimer.stop();
    return result;
  }

  @Override
  public ParseResult parseFiles(List<String> filenames)
      throws ParserException, IOException, InterruptedException, InvalidConfigurationException {
    if (filenames.size() != 1) {
      throw new K3ParserException(
          "K3 parser expects exactly one file, but got " + filenames.size());
    }

    Path path = Path.of(filenames.getFirst());

    parseTimer.start();
    K3Script script;
    try {
      script = K3ToAstParser.parseScript(path);
    } catch (K3AstParseException e) {
      throw new K3ParserException("Failed converting the input file into AST objects", e);
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
    K3Script script;
    try {
      script = K3ToAstParser.parseScript(filename, code);
    } catch (K3AstParseException e) {
      throw new K3ParserException("Failed converting the input file into AST objects", e);
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
