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
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.Immutable;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Immutable
public class FileLocation implements Serializable {

  private static final long serialVersionUID = 6652099907084949014L;

  private final String fileName;
  private final String niceFileName;

  private final int offset;
  private final int length;

  private final int startingLine;
  private final int endingLine;

  private final int startingLineInOrigin;
  private final int endingLineInOrigin;

  public FileLocation(
      String pFileName, int pOffset, int pLength, int pStartingLine, int pEndingLine) {
    this(
        pFileName,
        pFileName,
        pOffset,
        pLength,
        pStartingLine,
        pEndingLine,
        pStartingLine,
        pEndingLine);
  }

  public FileLocation(
      String pFileName,
      String pNiceFileName,
      int pOffset,
      int pLength,
      int pStartingLine,
      int pEndingLine,
      int pStartingLineInOrigin,
      int pEndingLineInOrigin) {
    fileName = checkNotNull(pFileName);
    niceFileName = checkNotNull(pNiceFileName);
    offset = pOffset;
    length = pLength;
    startingLine = pStartingLine;
    endingLine = pEndingLine;
    startingLineInOrigin = pStartingLineInOrigin;
    endingLineInOrigin = pEndingLineInOrigin;
  }

  public static final FileLocation DUMMY =
      new FileLocation("<none>", 0, 0, 0, 0) {
        private static final long serialVersionUID = -3012034075570811723L;

        @Override
        public String toString() {
          return "none";
        }
      };

  public static final FileLocation MULTIPLE_FILES =
      new FileLocation("<multiple files>", 0, 0, 0, 0) {
        private static final long serialVersionUID = -1725179775900132985L;

        @Override
        public String toString() {
          return getFileName();
        }
      };

  public static FileLocation merge(List<FileLocation> locations) {
    checkArgument(!Iterables.isEmpty(locations));

    String fileName = null;
    String niceFileName = null;
    int startingLine = Integer.MAX_VALUE;
    int startingLineInOrigin = Integer.MAX_VALUE;
    int endingLine = Integer.MIN_VALUE;
    int endingLineInOrigin = Integer.MIN_VALUE;
    for (FileLocation loc : locations) {
      if (loc == DUMMY) {
        continue;
      }
      if (fileName == null) {
        fileName = loc.fileName;
        niceFileName = loc.niceFileName;
      } else if (!fileName.equals(loc.fileName)) {
        return MULTIPLE_FILES;
      }

      startingLine = Math.min(startingLine, loc.getStartingLineNumber());
      startingLineInOrigin = Math.min(startingLineInOrigin, loc.getStartingLineInOrigin());
      endingLine = Math.max(endingLine, loc.getEndingLineNumber());
      endingLineInOrigin = Math.max(endingLineInOrigin, loc.getEndingLineInOrigin());
    }

    if (fileName == null) {
      // only DUMMY elements
      return DUMMY;
    }
    return new FileLocation(
        fileName,
        niceFileName,
        0,
        0,
        startingLine,
        endingLine,
        startingLineInOrigin,
        endingLineInOrigin);
  }

  public String getFileName() {
    return fileName;
  }

  @VisibleForTesting
  public String getNiceFileName() {
    return niceFileName;
  }

  public int getNodeOffset() {
    return offset;
  }

  public int getNodeLength() {
    return length;
  }

  public int getStartingLineNumber() {
    return startingLine;
  }

  public int getEndingLineNumber() {
    return endingLine;
  }

  public int getStartingLineInOrigin() {
    return startingLineInOrigin;
  }

  public int getEndingLineInOrigin() {
    return endingLineInOrigin;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(fileName);
    result = prime * result + offset;
    result = prime * result + length;
    result = prime * result + startingLine;
    result = prime * result + endingLine;
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

    return other.offset == offset
        && other.length == length
        && other.startingLine == startingLine
        && other.endingLine == endingLine
        && Objects.equals(other.fileName, fileName);
  }

  @Override
  public String toString() {
    String prefix = niceFileName.isEmpty()
        ? ""
        : niceFileName + ", ";
    if (startingLineInOrigin == endingLineInOrigin) {
      return prefix + "line " + startingLineInOrigin;
    } else {
      return prefix + "lines " + startingLineInOrigin + "-" + endingLineInOrigin;
    }
  }
}
