// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib;

import java.nio.file.Path;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser.SvLibAstParseException;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibCorrectnessWitness;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibViolationWitness;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibWitness;
import org.sosy_lab.cpachecker.exceptions.SvLibParserException;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;

public class SvLibWitnessParser {

  // Just a wrapper to make the API cleaner
  public static SvLibWitness parseWitness(Path pFilename) throws SvLibParserException {
    try {
      return SvLibToAstParser.parseWitness(pFilename);
    } catch (SvLibAstParseException e) {
      throw new SvLibParserException("Parsing of SV-LIB witness file " + pFilename + " failed.", e);
    }
  }

  @SuppressWarnings("")
  public static Optional<WitnessType> getWitnessTypeIfSvLib(Path pFilename) {
    try {
      SvLibWitness witness = SvLibWitnessParser.parseWitness(pFilename);
      return switch (witness) {
        case SvLibCorrectnessWitness pCorrectnessWitness ->
            Optional.of(WitnessType.CORRECTNESS_WITNESS);
        case SvLibViolationWitness pViolationWitness -> Optional.of(WitnessType.VIOLATION_WITNESS);
      };
    } catch (SvLibParserException e) {
      return Optional.empty();
    }
  }
}
