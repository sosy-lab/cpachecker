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
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibWitness;
import org.sosy_lab.cpachecker.exceptions.SvLibParserException;

public class SvLibWitnessParser {

  // Just a wrapper to make the API cleaner
  public static SvLibWitness parseWitness(Path pFilename) throws SvLibParserException {
    try {
      return SvLibToAstParser.parseWitness(pFilename);
    } catch (SvLibAstParseException e) {
      throw new SvLibParserException("Parsing of SV-LIB witness file " + pFilename + " failed.", e);
    }
  }
}
