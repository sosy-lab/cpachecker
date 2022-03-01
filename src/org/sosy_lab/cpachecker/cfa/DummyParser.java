// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import java.nio.file.Path;
import java.util.List;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonParser;

/**
 * For all languages, where parsing of single or blocks of statements is not yet implemented,
 * use this dummy scope when parsing an automaton {@link AutomatonParser}.
 */
public class DummyParser implements CParser {

  private static final DummyParser DUMMYPARSER = new DummyParser();

  private DummyParser() {} // Private constructor to insure one instance.

  public static DummyParser getInstance() {
    return DUMMYPARSER;
  }

  @Override
  public ParseResult parseString(
      Path pFileName, String pCode, CSourceOriginMapping pSourceOriginMapping, Scope pScope) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Timer getParseTime() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Timer getCFAConstructionTime() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ParseResult parseFiles(List<String> pFilenames) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ParseResult parseString(
      List<FileContentToParse> pCode, CSourceOriginMapping pSourceOriginMapping) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CAstNode parseSingleStatement(String pCode, Scope pScope) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CAstNode> parseStatements(String pCode, Scope pScope) {
    throw new UnsupportedOperationException();
  }
}