/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse;

import java.io.IOException;

import org.eclipse.cdt.core.dom.ICodeReaderFactory;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.parser.CodeReader;
import org.eclipse.cdt.core.parser.ICodeReaderCache;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.common.LogManager;

/**
 * Wrapper for Eclipse CDT 6 (internal version number 5.1.*)
 */
public class EclipseCParser extends AbstractEclipseCParser<CodeReader> {
    
  public EclipseCParser(LogManager pLogger, Dialect dialect) {
    super(pLogger, dialect);
  }
  
  @Override
  protected CodeReader wrapCode(String pCode) {
    return new CodeReader(pCode.toCharArray());
  }
  
  @Override
  protected CodeReader wrapFile(String pFilename) throws IOException {
    return new CodeReader(pFilename);
  }
  
  @Override
  protected IASTTranslationUnit getASTTranslationUnit(CodeReader codeReader) throws CFAGenerationRuntimeException, CoreException {
    return language.getASTTranslationUnit(codeReader,
                                          StubScannerInfo.instance,
                                          StubCodeReaderFactory.instance,
                                          null,
                                          PARSER_OPTIONS,
                                          parserLog);
  }
  
  /**
   * Private class that creates CodeReaders for files. Caching is not supported.
   * TODO: Errors are ignored currently.
   */
  private static class StubCodeReaderFactory implements ICodeReaderFactory {

    private static ICodeReaderFactory instance = new StubCodeReaderFactory();

    @Override
    public int getUniqueIdentifier() {
      throw new UnsupportedOperationException();
    }

    @Override
    public CodeReader createCodeReaderForTranslationUnit(String path) {
      try {
        return new CodeReader(path);
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }

    @Override
    public ICodeReaderCache getCodeReaderCache() {
      throw new UnsupportedOperationException();
    }
    @Override
    public CodeReader createCodeReaderForInclusion(String arg0) {
      throw new CFAGenerationRuntimeException("#include statements are not allowed in the source code.");
    }
  }
}