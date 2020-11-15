/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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

  private final ClangProcessor preprocessor;

  public LlvmParserWithClang(final ClangProcessor pPreprocessor,final LogManager pLogger, final MachineModel pMachineModel) {
    super(pLogger, pMachineModel);
    this.preprocessor = pPreprocessor;
  }

  @Override
  public ParseResult parseFile(final String pFilename) throws ParserException {
    try {
      Path dumpedFile = preprocessor.preprocessAndGetDumpedFile(pFilename);
      return super.parseFile(dumpedFile.toString());
    } catch (InterruptedException ex) {
      // TODO;
    }
    return null;
  }

  @Override
  public ParseResult parseString(final String pFilename, final String pCode) {
    // TODO
    throw new UnsupportedOperationException();
  }

  static class Factory {
    public static LlvmParserWithClang getParser(
        ClangProcessor processor,
        LogManager logger,
        MachineModel machine) {
      return new LlvmParserWithClang(processor, logger, machine);
    }
  }

}
