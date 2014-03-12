/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import org.sosy_lab.common.Pair;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;


public class CSourceOriginMapping {

  private final RangeMap<Integer, String> lineToFilenameMapping = TreeRangeMap.create();
  private final RangeMap<Integer, Integer> lineDeltaMapping = TreeRangeMap.create();

  void mapInputLineRangeToDelta(String originFilename, int fromInputLineNumber, int toInputLineNumber, int deltaLinesToOrigin) {
    Range<Integer> lineRange = Range.openClosed(fromInputLineNumber-1, toInputLineNumber);
    lineToFilenameMapping.put(lineRange, originFilename);
    lineDeltaMapping.put(lineRange, deltaLinesToOrigin);
  }

  public Pair<String, Integer> getOriginLineFromAnalysisCodeLine(
      String analysisFile, int analysisCodeLine) {
    Integer lineDelta = lineDeltaMapping.get(analysisCodeLine);
    String originFileName = lineToFilenameMapping.get(analysisCodeLine);

    if (lineDelta == null || originFileName == null) {
      return Pair.of(analysisFile, analysisCodeLine);
    }

    return Pair.of(originFileName, analysisCodeLine + lineDelta);
  }
}
