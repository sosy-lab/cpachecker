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

import java.util.Set;

import org.sosy_lab.common.Pair;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeRangeMap;


public enum CSourceOriginMapping {
  INSTANCE;

  public class NoOriginMappingAvailableException extends Exception {
    private static final long serialVersionUID = -2094250312246030679L;

    public NoOriginMappingAvailableException(String message) {
      super(message);
    }
  }

  public class NoTokenizingAvailableException extends RuntimeException {
    private static final long serialVersionUID = 2376782857133795915L;

    public NoTokenizingAvailableException(String message) {
      super(message);
    }
  }

  private Boolean oneInputLinePerToken = null;
  private boolean frozen = false;

  public final RangeMap<Integer, String> lineToFilenameMapping = TreeRangeMap.create();
  public final RangeMap<Integer, String> tokenToFilenameMapping = TreeRangeMap.create();
  public final RangeMap<Integer, Integer> lineDeltaMapping = TreeRangeMap.create();
  public final RangeMap<Integer, Integer> tokenDeltaMapping = TreeRangeMap.create();
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

  public void mapAbsoluteTokenRangeToInputLine(int fromTokenNumber, int toTokenNumber, int inputLineNumber) {
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

  public void mapInputTokenRangeToDelta(String originFilename, int fromInputTokenNumber, int toInputTokenNumber, int deltaTokensToOrigin) {
    Preconditions.checkNotNull(oneInputLinePerToken);
    if (frozen) {
      return;
    }

    Range<Integer> tokenRange = Range.openClosed(fromInputTokenNumber-1, toInputTokenNumber);
    tokenToFilenameMapping.put(tokenRange, originFilename);
    tokenDeltaMapping.put(tokenRange, deltaTokensToOrigin);
  }

  public Pair<String, Integer> getOriginLineFromAnalysisCodeLine(int analysisCodeLine) throws NoOriginMappingAvailableException {
    Integer inputLine = analysisCodeLine;
    if ((oneInputLinePerToken != null) && oneInputLinePerToken) {
      inputLine = tokenToLineMapping.get(analysisCodeLine);

      if (inputLine == null) {
        throw new NoOriginMappingAvailableException("Mapping from token to line failed!");
      }
    }

    Integer lineDelta = lineDeltaMapping.get(inputLine);
    String originFileName = lineToFilenameMapping.get(inputLine);

    if (lineDelta == null || originFileName == null) {
      throw new NoOriginMappingAvailableException("Mapping failed! Delta or origin unknown!");
    }

    return Pair.of(originFileName, inputLine + lineDelta);
  }

  public Pair<String, Integer> getOriginTokenNumberFromAbsoluteTokenNumber(int absoluteTokenNumber) throws NoOriginMappingAvailableException {
    if ((oneInputLinePerToken == null) || !oneInputLinePerToken) {
      throw new NoTokenizingAvailableException("Tokenizing was not performed on the input program! Please enable the tokenizer!");
    }

    Integer tokenDelta = tokenDeltaMapping.get(absoluteTokenNumber);
    String originFileName = tokenToFilenameMapping.get(absoluteTokenNumber);

    if (tokenDelta == null || originFileName == null) {
      throw new NoOriginMappingAvailableException("Mapping source code line to its origin is not possible due to missing mappings!");
    }

    return Pair.of(originFileName, absoluteTokenNumber + tokenDelta);
  }

  public Pair<String, Set<Integer>> getRelativeTokensFromAbsolute(Set<Integer> absoluteTokens) throws NoOriginMappingAvailableException {
    Set<Integer> relative = Sets.newTreeSet();
    String originFilename = null;
    for (Integer abs: absoluteTokens) {
      Pair<String, Integer> rel = getOriginTokenNumberFromAbsoluteTokenNumber(abs);
      if (originFilename == null) {
        originFilename = rel.getFirst();
      }
      Preconditions.checkArgument(originFilename.equals(rel.getFirst()));
      relative.add(rel.getSecond());
    }
    return Pair.of(originFilename, relative);
  }


  public synchronized void freeze() {
    frozen = true;
  }
}
