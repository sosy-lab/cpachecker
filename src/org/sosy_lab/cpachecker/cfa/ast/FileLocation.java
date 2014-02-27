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
package org.sosy_lab.cpachecker.cfa.ast;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Objects;

import com.google.common.collect.Iterables;

public class FileLocation {

  private final int endineLine;
  private final String fileName;
  private final int length;
  private final int offset;
  private final int startingLine;
  private final int startingLineInOrigin;

  public FileLocation(int pEndineLine, String pFileName, int pLength,
      int pOffset, int pStartingLine) {
    this(pEndineLine, pFileName, pLength, pOffset, pStartingLine, pStartingLine);
  }

  public FileLocation(int pEndineLine, String pFileName, int pLength,
      int pOffset, int pStartingLine, int pStartingLineInOrigin) {
    endineLine = pEndineLine;
    fileName = pFileName;
    length = pLength;
    offset = pOffset;
    startingLine = pStartingLine;
    startingLineInOrigin = pStartingLineInOrigin;
  }

  public static final FileLocation DUMMY = new FileLocation(0, "<none>", 0, 0, 0) {
    @Override
    public String toString() {
      return "none";
    }
  };

  public static final FileLocation MULTIPLE_FILES = new FileLocation(0, "<multiple files>", 0, 0, 0) {
    @Override
    public String toString() {
      return getFileName();
    }
  };

  public static FileLocation merge(List<FileLocation> locations) {
    checkArgument(!Iterables.isEmpty(locations));

    String fileName = null;
    int startingLine = Integer.MAX_VALUE;
    int startingLineInOrigin = Integer.MAX_VALUE;
    int endingLine = Integer.MIN_VALUE;
    for (FileLocation loc : locations) {
      if (loc == DUMMY) {
        continue;
      }
      if (fileName == null) {
        fileName = loc.fileName;
      } else if (!fileName.equals(loc.fileName)) {
        return MULTIPLE_FILES;
      }

      startingLine = Math.min(startingLine, loc.getStartingLineNumber());
      startingLineInOrigin = Math.min(startingLineInOrigin, loc.getStartingLineInOrigin());
      endingLine = Math.max(endingLine, loc.getEndingLineNumber());
    }

    if (fileName == null) {
      // only DUMMY elements
      return DUMMY;
    }
    return new FileLocation(endingLine, fileName, 0, 0, startingLine, startingLineInOrigin);
  }

  public int getStartingLineInOrigin() {
    return startingLineInOrigin;
  }

  public int getEndingLineNumber() {
    return endineLine;
  }

  public String getFileName() {
    return fileName;
  }

  public int getNodeLength() {
    return length;
  }

  public int getNodeOffset() {
    return offset;
  }

  public int getStartingLineNumber() {
    return startingLine;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + endineLine;
    result = prime * result + Objects.hashCode(fileName);
    result = prime * result + length;
    result = prime * result + offset;
    result = prime * result + startingLine;
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof FileLocation)) {
      return false;
    }

    FileLocation other = (FileLocation) obj;

    return other.endineLine == endineLine
            && other.startingLine == startingLine
            && other.length == length
            && other.offset == offset
            && Objects.equals(other.fileName, fileName);
  }

  @Override
  public String toString() {
    if (startingLine == endineLine) {
      return "line " + startingLine;
    } else {
      return "lines " + startingLine + "-" + endineLine;
    }
  }
}
