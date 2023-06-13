// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.Immutable;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@Immutable
public class FileLocation implements Serializable, Comparable<FileLocation> {

  private static final long serialVersionUID = 6652099907084949014L;

  private final transient Path fileName;
  private final String niceFileName;

  private final int offset;
  private final int length;

  private final int startingLine;
  private final int endingLine;

  private final int startingLineInOrigin;
  private final int endingLineInOrigin;

  private final boolean offsetRelatedToOrigin;

  public FileLocation(
      Path pFileName, int pOffset, int pLength, int pStartingLine, int pEndingLine) {
    this(
        pFileName,
        pFileName.toString(),
        pOffset,
        pLength,
        pStartingLine,
        pEndingLine,
        pStartingLine,
        pEndingLine,
        true);
  }

  public FileLocation(
      Path pFileName,
      String pNiceFileName,
      int pOffset,
      int pLength,
      int pStartingLine,
      int pEndingLine,
      int pStartingLineInOrigin,
      int pEndingLineInOrigin,
      boolean pOffsetRelatedToOrigin) {
    fileName = checkNotNull(pFileName);
    checkArgument(!fileName.toString().isEmpty(), "Non-empty file name required");
    niceFileName = checkNotNull(pNiceFileName);
    offset = pOffset;
    length = pLength;
    startingLine = pStartingLine;
    endingLine = pEndingLine;
    startingLineInOrigin = pStartingLineInOrigin;
    endingLineInOrigin = pEndingLineInOrigin;
    offsetRelatedToOrigin = pOffsetRelatedToOrigin;
  }

  public static final FileLocation DUMMY =
      new FileLocation(Path.of("#none#"), 0, 0, 0, 0) {
        private static final long serialVersionUID = -3012034075570811723L;

        @Override
        public boolean isRealLocation() {
          return false;
        }

        @Override
        public String toString() {
          return "none";
        }
      };

  public static final FileLocation MULTIPLE_FILES =
      new FileLocation(Path.of("#multiple files#"), 0, 0, 0, 0) {
        private static final long serialVersionUID = -1725179775900132985L;

        @Override
        public boolean isRealLocation() {
          return false;
        }

        @Override
        public String toString() {
          return getFileName().toString();
        }
      };

  public static FileLocation merge(List<FileLocation> locations) {
    checkArgument(!Iterables.isEmpty(locations));

    Path fileName = null;
    String niceFileName = null;
    int startingLine = Integer.MAX_VALUE;
    int startingLineInOrigin = Integer.MAX_VALUE;
    int startOffset = Integer.MAX_VALUE;
    int endingLine = Integer.MIN_VALUE;
    int endingLineInOrigin = Integer.MIN_VALUE;
    int endOffset = Integer.MIN_VALUE;
    boolean offsetRelatedToOrigin = true;
    for (FileLocation loc : locations) {
      if (DUMMY.equals(loc)) {
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
      startOffset = Math.min(startOffset, loc.getNodeOffset());
      endingLine = Math.max(endingLine, loc.getEndingLineNumber());
      endingLineInOrigin = Math.max(endingLineInOrigin, loc.getEndingLineInOrigin());
      endOffset = Math.max(endOffset, loc.getNodeOffset() + loc.getNodeLength());
      offsetRelatedToOrigin &= loc.offsetRelatedToOrigin;
    }

    if (fileName == null) {
      // only DUMMY elements
      return DUMMY;
    }
    return new FileLocation(
        fileName,
        niceFileName,
        startOffset,
        endOffset - startOffset,
        startingLine,
        endingLine,
        startingLineInOrigin,
        endingLineInOrigin,
        offsetRelatedToOrigin);
  }

  /** Whether this is a real location or a dummy one like {@link FileLocation#DUMMY} */
  public boolean isRealLocation() {
    return true;
  }

  /** Return the non-null and non-empty file name. */
  public Path getFileName() {
    return fileName;
  }

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

  public boolean isOffsetRelatedToOrigin() {
    return offsetRelatedToOrigin;
  }

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
  public int compareTo(FileLocation pOther) {
    return ComparisonChain.start()
        .compare(fileName, pOther.fileName)
        .compare(offset, pOther.offset)
        .compare(length, pOther.length)
        .result();
  }

  @Override
  public String toString() {
    String prefix = niceFileName.isEmpty() ? "" : niceFileName + ", ";
    if (startingLineInOrigin == endingLineInOrigin) {
      return prefix + "line " + startingLineInOrigin;
    } else {
      return prefix + "lines " + startingLineInOrigin + "-" + endingLineInOrigin;
    }
  }

  protected Object writeReplace() {
    return new SerializationProxy(this);
  }

  @SuppressWarnings({"UnusedVariable", "unused"}) // parameter is required by API
  private void readObject(ObjectInputStream in) throws IOException {
    throw new InvalidObjectException("Proxy required");
  }

  private static class SerializationProxy implements Serializable {
    private static final long serialVersionUID = -3730421630343690695L;

    private final String fileName;
    private final String niceFileName;
    private final int offset;
    private final int length;
    private final int startingLine;
    private final int endingLine;
    private final int startingLineInOrigin;
    private final int endingLineInOrigin;
    private final boolean offsetRelatedToOrigin;

    SerializationProxy(FileLocation loc) {
      fileName = loc.fileName.toString();
      niceFileName = loc.niceFileName;
      offset = loc.offset;
      length = loc.length;
      startingLine = loc.startingLine;
      endingLine = loc.endingLine;
      startingLineInOrigin = loc.startingLineInOrigin;
      endingLineInOrigin = loc.endingLineInOrigin;
      offsetRelatedToOrigin = loc.offsetRelatedToOrigin;
    }

    private Object readResolve() {
      FileLocation result =
          new FileLocation(
              Path.of(fileName),
              niceFileName,
              offset,
              length,
              startingLine,
              endingLine,
              startingLineInOrigin,
              endingLineInOrigin,
              offsetRelatedToOrigin);

      if (result.equals(DUMMY)) {
        return DUMMY;
      } else if (result.equals(MULTIPLE_FILES)) {
        return MULTIPLE_FILES;
      }
      return result;
    }
  }
}
