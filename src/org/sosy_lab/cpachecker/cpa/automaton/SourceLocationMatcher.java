// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

class SourceLocationMatcher {

  private abstract static class BaseFileNameMatcher implements Predicate<FileLocation> {

    private final Optional<String> originFileName;

    private BaseFileNameMatcher(Optional<String> originFileName) {
      this.originFileName = checkNotNull(originFileName);
    }

    @Override
    public boolean apply(FileLocation pFileLocation) {
      if (!originFileName.isPresent()) {
        return true;
      }
      String fileName = originFileName.orElseThrow();
      String fileLocationFileName = pFileLocation.getFileName().toString();
      fileName = getBaseName(fileName);
      fileLocationFileName = getBaseName(fileLocationFileName);
      return fileName.equals(fileLocationFileName);
    }

    private String getBaseName(String pOf) {
      int index = pOf.lastIndexOf('/');
      if (index == -1) {
        index = pOf.lastIndexOf('\\');
      }
      if (index == -1) {
        return pOf;
      } else {
        return pOf.substring(index + 1);
      }
    }

    @Override
    public String toString() {
      return originFileName.isPresent() ? "FILE " + originFileName : "TRUE";
    }

    protected Optional<String> getOriginFileName() {
      return originFileName;
    }
  }

  static class LineMatcher extends BaseFileNameMatcher {

    private final int startLineNumber;

    private final int endLineNumber;

    private final boolean origin;

    public LineMatcher(
        Optional<String> pFileName, int pStartLineNumber, int pEndLineNumber, boolean pOrigin) {
      super(pFileName);
      Preconditions.checkArgument(pStartLineNumber <= pEndLineNumber);
      startLineNumber = pStartLineNumber;
      endLineNumber = pEndLineNumber;
      origin = pOrigin;
    }

    public LineMatcher(Optional<String> pFileName, int pStartLineNumber, int pEndLineNumber) {
      this(pFileName, pStartLineNumber, pEndLineNumber, true);
    }

    @Override
    public int hashCode() {
      return Objects.hash(getOriginFileName(), startLineNumber, endLineNumber, origin);
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (!(pObj instanceof LineMatcher)) {
        return false;
      }
      LineMatcher other = (LineMatcher) pObj;
      return origin == other.origin
          && startLineNumber == other.startLineNumber
          && endLineNumber == other.endLineNumber
          && Objects.equals(getOriginFileName(), other.getOriginFileName());
    }

    @Override
    public boolean apply(FileLocation pFileLocation) {
      int compStartingLine =
          origin ? pFileLocation.getStartingLineInOrigin() : pFileLocation.getStartingLineNumber();
      int compEndingLine =
          origin ? pFileLocation.getEndingLineInOrigin() : pFileLocation.getEndingLineNumber();
      return super.apply(pFileLocation)
          && startLineNumber <= compEndingLine
          && compStartingLine <= endLineNumber;
    }

    @Override
    public String toString() {
      String prefix = "LINE ";
      if (origin) {
        prefix = "ORIGIN " + prefix;
      }
      if (startLineNumber == endLineNumber) {
        return prefix + startLineNumber;
      }
      return prefix + startLineNumber + "-" + endLineNumber;
    }
  }

  static class OffsetMatcher extends BaseFileNameMatcher {

    private final int startOffset;

    private final int endOffset;

    OffsetMatcher(Optional<String> pOriginFileName, int pStartOffset, int pEndOffset) {
      super(pOriginFileName);
      Preconditions.checkArgument(pStartOffset <= pEndOffset);
      startOffset = pStartOffset;
      endOffset = pEndOffset;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getOriginFileName(), startOffset, endOffset);
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (!(pObj instanceof OffsetMatcher)) {
        return false;
      }
      OffsetMatcher other = (OffsetMatcher) pObj;
      return Objects.equals(getOriginFileName(), other.getOriginFileName())
          && startOffset == other.startOffset
          && endOffset == other.endOffset;
    }

    @Override
    public boolean apply(FileLocation pFileLocation) {
      int locationEndOffset = pFileLocation.getNodeOffset() + pFileLocation.getNodeLength() - 1;
      return super.apply(pFileLocation)
          && pFileLocation.getNodeOffset() <= endOffset
          && startOffset <= locationEndOffset;
    }

    @Override
    public String toString() {
      if (startOffset == endOffset) {
        return "OFFSET " + startOffset;
      }
      return "OFFSET " + startOffset + "-" + endOffset;
    }
  }
}
