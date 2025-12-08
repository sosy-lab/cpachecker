// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib;

import java.nio.file.Path;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser.SvLibAstParseException;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser.SvLibParsingResult;
import org.sosy_lab.cpachecker.exceptions.SvLibParserException;

public class SvLibWitnessParser {

  @SuppressWarnings("unused")
  public static void parseWitness(Path pFilename, String pCode) throws SvLibParserException {
    SvLibParsingResult script;
    try {
      script = SvLibToAstParser.parseScript(pFilename, pCode);
    } catch (SvLibAstParseException e) {
      throw new SvLibParserException(
          "Failed converting the input witness file into AST objects", e);
    }
  }
}
