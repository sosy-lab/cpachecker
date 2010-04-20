/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cmdline.stubs;

import java.io.IOException;

import org.eclipse.cdt.core.dom.ICodeReaderFactory;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.core.parser.CodeReader;
import org.eclipse.cdt.core.parser.ICodeReaderCache;

/**
 * This class is necessary in order to use others than version 4 of CDT. Do not
 * delete it or change because of errors referencing to a missing method
 * createCodeReaderForInclusion, this method exists only in CDT 4!
 */
public class StubCodeReaderFactory implements ICodeReaderFactory {

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

    public CodeReader createCodeReaderForTranslationUnit(ITranslationUnit tu) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ICodeReaderCache getCodeReaderCache() {
      throw new UnsupportedOperationException();
    }
    public CodeReader createCodeReaderForInclusion(String arg0) {
      throw new UnsupportedOperationException();
    }
}
