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

import java.util.Map;

import org.eclipse.cdt.core.dom.IParserConfiguration;
import org.eclipse.cdt.core.parser.IScannerInfo;


public class StubConfiguration implements IParserConfiguration {

    private static class StubScannerInfo implements IScannerInfo {

        @SuppressWarnings("unchecked")
        public Map getDefinedSymbols() {
            // the externally defined pre-processor macros  
            return null;
        }

        public String[] getIncludePaths() {
            return null;
        }
    }

    private final String parserDialect;
    
    public StubConfiguration(String parserDialect) {
      this.parserDialect = parserDialect;
    }
  
    public String getParserDialect() {
      return parserDialect;
    }

    public IScannerInfo getScannerInfo() {
        return new StubScannerInfo();
    }
}
