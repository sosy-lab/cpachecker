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

import org.eclipse.cdt.core.dom.CDOM;
import org.eclipse.cdt.core.dom.ICodeReaderFactory;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.core.parser.CodeReader;
import org.eclipse.cdt.core.parser.ICodeReaderCache;

import cmdline.stubs.StubCodeReaderFactory;

/**
 * This class is necessary in order to use others than version 4 of CDT. Do not
 * delete it or change because of errors referencing to a missing method
 * createCodeReaderForInclusion, this method exists only in CDT 4!
 */
public class StubCodeReaderFactory implements ICodeReaderFactory {

    private ICodeReaderCache cache = null;

    public static StubCodeReaderFactory getInstance()
    {
        return instance;
    }

    private static StubCodeReaderFactory instance = new StubCodeReaderFactory();

    private StubCodeReaderFactory()
    {
//        int size=0;
//        size = CodeReaderCache.DEFAULT_CACHE_SIZE_IN_MB;
//        cache = new CodeReaderCache(size);
    }
    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ICodeReaderFactory#getUniqueIdentifier()
     */
    public int getUniqueIdentifier() {
        return CDOM.PARSE_SAVED_RESOURCES;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ICodeReaderFactory#createCodeReaderForTranslationUnit(java.lang.String)
     */
    public CodeReader createCodeReaderForTranslationUnit(String path) {
//        return cache.get(path);
        try {
            return new CodeReader(path);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public CodeReader createCodeReaderForTranslationUnit(ITranslationUnit tu) {
        return new CodeReader(tu.getResource().getLocation().toOSString(), tu.getContents());
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ICodeReaderFactory#createCodeReaderForInclusion(java.lang.String)
     */
    public ICodeReaderCache getCodeReaderCache() {
        return cache;
    }
    public CodeReader createCodeReaderForInclusion(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }
}
