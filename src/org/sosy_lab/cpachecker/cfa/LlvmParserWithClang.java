// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// Copyright (C) 2007-2017  Dirk Beyer
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.TempFile;
import org.sosy_lab.common.io.TempFile.DeleteOnCloseDir;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.parser.llvm.LlvmParser;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.CParserException;
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
  public ParseResult parseFiles(final List<String> pFilenames)
      throws ParserException, InterruptedException, InvalidConfigurationException {

    if (pFilenames.size() > 1) {
      throw new InvalidConfigurationException(
          "Multiple program files not supported when using LLVM frontend.");
    }
    Path filename = Path.of(pFilenames.get(0));

    if (preprocessor.getDumpDirectory() != null) {
      // Writing to the output directory is possible.
      return parse0(filename, preprocessor.getDumpDirectory());
    }

    try (DeleteOnCloseDir tempDir = TempFile.createDeleteOnCloseDir("clang-results")) {
      return parse0(filename, tempDir.toPath());
    } catch (IOException e) {
      throw new CParserException("Could not write clang output to file " + e.getMessage(), e);
    }
  }

  @Override
  public ParseResult parseString(final Path pFilename, final String pCode) {
    // TODO
    throw new UnsupportedOperationException();
  }

  private ParseResult parse0(final Path pFileName, final Path pDumpDirectory)
      throws ParserException, InterruptedException {
    Path dumpedFile = preprocessor.preprocessAndGetDumpedFile(pFileName, pDumpDirectory);
    return super.parseFile(dumpedFile);
  }

  static class Factory {
    public static LlvmParserWithClang getParser(
        ClangPreprocessor processor, LogManager logger, MachineModel machine) {
      return new LlvmParserWithClang(processor, logger, machine);
    }
  }

}
