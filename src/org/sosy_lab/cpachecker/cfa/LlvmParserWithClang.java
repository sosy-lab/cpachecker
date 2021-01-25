// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// Copyright (C) 2007-2017  Dirk Beyer
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import java.nio.file.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.parser.llvm.LlvmParser;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/**
 * Parser for the LLVM intermediate language to a CFA. LLVM IR is a typed, assembler-like language
 * that uses the SSA form by default. Because of this, parsing is quite simple: there is no need for
 * scoping and expression trees are always flat.
 */
public class LlvmParserWithClang extends LlvmParser {

  private final ClangPreprocessor preprocessor;

  public LlvmParserWithClang(
      final ClangPreprocessor pPreprocessor,
      final LogManager pLogger,
      final MachineModel pMachineModel) {
    super(pLogger, pMachineModel);
    this.preprocessor = pPreprocessor;
  }

  @Override
  public ParseResult parseFile(final String pFilename)
      throws ParserException, InterruptedException {
    Path dumpedFile = preprocessor.preprocessAndGetDumpedFile(pFilename);
    return super.parseFile(dumpedFile.toString());
  }

  @Override
  public ParseResult parseString(final String pFilename, final String pCode) {
    // TODO
    throw new UnsupportedOperationException();
  }

  static class Factory {
    public static LlvmParserWithClang getParser(
        ClangPreprocessor processor, LogManager logger, MachineModel machine) {
      return new LlvmParserWithClang(processor, logger, machine);
    }
  }

}
