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

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.Pair;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;


public class CSourceOriginMapping {

  // Each RangeMap in this map contains the mapping for one input file,
  // from its lines to the tuple of (originalFile, lineDelta).
  // The full mapping is a map with those RangeMaps as values,
  // one for each input file.
  private final Map<String, RangeMap<Integer, Pair<String, Integer>>> mapping = new HashMap<>();

  void mapInputLineRangeToDelta(String inputFilename, String originFilename, int fromInputLineNumber, int toInputLineNumber, int deltaLinesToOrigin) {
    RangeMap<Integer, Pair<String, Integer>> fileMapping = mapping.get(inputFilename);
    if (fileMapping == null) {
      fileMapping = TreeRangeMap.create();
      mapping.put(inputFilename, fileMapping);
    }

    Range<Integer> lineRange = Range.openClosed(fromInputLineNumber-1, toInputLineNumber);
    fileMapping.put(lineRange, Pair.of(originFilename, deltaLinesToOrigin));
  }

  public Pair<String, Integer> getOriginLineFromAnalysisCodeLine(
      String analysisFile, int analysisCodeLine) {
    RangeMap<Integer, Pair<String, Integer>> fileMapping = mapping.get(analysisFile);

    if (fileMapping != null) {
      Pair<String, Integer> originFileAndLineDelta = fileMapping.get(analysisCodeLine);

      if (originFileAndLineDelta != null) {
        return Pair.of(originFileAndLineDelta.getFirst(),
            analysisCodeLine + originFileAndLineDelta.getSecond());
      }
    }
    return Pair.of(analysisFile, analysisCodeLine);
  }
}
