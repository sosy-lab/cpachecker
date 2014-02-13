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

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;


public enum CSourceOriginMapping {
  INSTANCE;

  public class NoOriginMappingAvailable extends Exception {
    public NoOriginMappingAvailable(String message) {
      super(message);
    }
  }

  private Boolean oneInputLinePerToken = null;
  private boolean frozen = false;

  public final RangeMap<Integer, String> lineToFilenameMapping = TreeRangeMap.create();
  public final RangeMap<Integer, Integer> lineDeltaMapping = TreeRangeMap.create();
  public final RangeMap<Integer, Integer> tokenToLineMapping = TreeRangeMap.create();

  public boolean getHasOneInputLinePerToken() {
    Preconditions.checkNotNull(oneInputLinePerToken);
    return oneInputLinePerToken;
  }

  public void setHasOneInputLinePerToken(boolean pOneInputLinePerToken) {
    if (frozen) {
      return;
    }

    oneInputLinePerToken = pOneInputLinePerToken;
  }

  public void mapTokenRangeToInputLine(int fromTokenNumber, int toTokenNumber, int inputLineNumber) {
    Preconditions.checkNotNull(oneInputLinePerToken);
    if (frozen) {
      return;
    }

    Range<Integer> tokenRange = Range.openClosed(fromTokenNumber-1, toTokenNumber);
    tokenToLineMapping.put(tokenRange, inputLineNumber);
  }

  public void mapInputLineRangeToDelta(String originFilename, int fromInputLineNumber, int toInputLineNumber, int deltaLinesToOrigin) {
    Preconditions.checkNotNull(oneInputLinePerToken);
    if (frozen) {
      return;
    }

    Range<Integer> lineRange = Range.openClosed(fromInputLineNumber-1, toInputLineNumber);
    lineToFilenameMapping.put(lineRange, originFilename);
    lineDeltaMapping.put(lineRange, deltaLinesToOrigin);
  }

  public Pair<String, Integer> getOriginLineFromAnalysisCodeLine(int analysisCodeLine) throws NoOriginMappingAvailable {
    Integer inputLine = analysisCodeLine;
    if ((oneInputLinePerToken != null) && oneInputLinePerToken) {
      inputLine = tokenToLineMapping.get(analysisCodeLine);
    }

    Integer lineDelta = lineDeltaMapping.get(inputLine);
    String originFileName = lineToFilenameMapping.get(analysisCodeLine);

    if (inputLine == null || lineDelta == null || originFileName == null) {
      throw new NoOriginMappingAvailable("Mapping source code line to its origin is not possible due to missing mappings!");
    }

    return Pair.of(originFileName, inputLine + lineDelta);
  }

  public synchronized void freeze() {
    frozen = true;
  }
}
