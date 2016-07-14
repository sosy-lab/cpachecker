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
package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Predicate;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

import java.util.Objects;
import java.util.Optional;

class SourceLocationMatcher {

  private static abstract class BaseFileNameMatcher implements Predicate<FileLocation> {

    private final Optional<String> originFileName;

    private BaseFileNameMatcher(Optional<String> originFileName) {
      this.originFileName = checkNotNull(originFileName);
    }

    @Override
    public boolean apply(FileLocation pFileLocation) {
      if (!originFileName.isPresent()) {
        return true;
      }
      String originFileName = this.originFileName.get();
      String fileLocationFileName = pFileLocation.getFileName();
      originFileName = getBaseName(originFileName);
      fileLocationFileName = getBaseName(fileLocationFileName);
      return originFileName.equals(fileLocationFileName);
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
    public int hashCode() {
      return originFileName.hashCode();
    }

    @Override
    public String toString() {
      return originFileName.isPresent() ? "FILE " + originFileName : "TRUE";
    }

    protected Optional<String> getOriginFileName() {
      return originFileName;
    }

  }

  static class OriginLineMatcher extends BaseFileNameMatcher {

    private final int originLineNumber;

    public OriginLineMatcher(Optional<String> pOriginFileName, int pOriginLineNumber) {
      super(pOriginFileName);
      this.originLineNumber = pOriginLineNumber;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getOriginFileName(), originLineNumber);
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (!(pObj instanceof OriginLineMatcher)) {
        return false;
      }
      OriginLineMatcher other = (OriginLineMatcher) pObj;
      return Objects.equals(getOriginFileName(), other.getOriginFileName())
          && originLineNumber == other.originLineNumber;
    }

    @Override
    public boolean apply(FileLocation pFileLocation) {
      return super.apply(pFileLocation)
          && pFileLocation.getStartingLineInOrigin() == originLineNumber;
    }

    @Override
    public String toString() {
      return "ORIGIN STARTING LINE " + originLineNumber;
    }
  }

  static class OffsetMatcher extends BaseFileNameMatcher {

    private final int offset;

    OffsetMatcher(Optional<String> pOriginFileName, int pOffset) {
      super(pOriginFileName);
      this.offset = pOffset;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getOriginFileName(), offset);
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
          && offset == other.offset;
    }

    @Override
    public boolean apply(FileLocation pFileLocation) {
      return super.apply(pFileLocation)
          && pFileLocation.getNodeOffset() <= offset
          && pFileLocation.getNodeOffset() + pFileLocation.getNodeLength() > offset;
    }

    @Override
    public String toString() {
      return "OFFSET " + offset;
    }
  }
}
