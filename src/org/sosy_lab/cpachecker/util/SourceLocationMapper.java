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
package org.sosy_lab.cpachecker.util;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

import java.util.Objects;
import java.util.Optional;


public class SourceLocationMapper {

  public static interface LocationDescriptor {

    boolean matches(FileLocation pFileLocation);

  }

  private static abstract class FileNameDescriptor implements LocationDescriptor {

    private final Optional<String> originFileName;

    private final boolean matchBaseName;

    private FileNameDescriptor(String originFileName) {
      this(originFileName, true);
    }

    private FileNameDescriptor(String originFileName, boolean matchBaseName) {
      this.originFileName = Optional.of(originFileName);
      this.matchBaseName = matchBaseName;
    }

    private FileNameDescriptor(Optional<String> originFileName, boolean matchBaseName) {
      Preconditions.checkNotNull(originFileName);
      this.originFileName = originFileName;
      this.matchBaseName = matchBaseName;
    }

    @Override
    public boolean matches(FileLocation pFileLocation) {
      if (!originFileName.isPresent()) {
        return true;
      }
      String originFileName = this.originFileName.get();
      String fileLocationFileName = pFileLocation.getFileName();
      if (matchBaseName) {
        originFileName = getBaseName(originFileName);
        fileLocationFileName = getBaseName(fileLocationFileName);
      }
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

  public static class OriginLineDescriptor extends FileNameDescriptor implements LocationDescriptor {

    private final int originLineNumber;

    public OriginLineDescriptor(Optional<String> pOriginFileName, int pOriginLineNumber) {
      this(pOriginFileName, pOriginLineNumber, true);
    }

    private OriginLineDescriptor(
        Optional<String> pOriginFileName, int pOriginLineNumber, boolean pMatchBaseName) {
      super(pOriginFileName, pMatchBaseName);
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
      if (!(pObj instanceof OriginLineDescriptor)) {
        return false;
      }
      OriginLineDescriptor other = (OriginLineDescriptor) pObj;
      return Objects.equals(getOriginFileName(), other.getOriginFileName())
          && originLineNumber == other.originLineNumber;
    }

    @Override
    public boolean matches(FileLocation pFileLocation) {
      return super.matches(pFileLocation)
          && pFileLocation.getStartingLineInOrigin() == originLineNumber;
    }

    @Override
    public String toString() {
      return "ORIGIN STARTING LINE " + originLineNumber;
    }
  }

  public static class OffsetDescriptor extends FileNameDescriptor implements LocationDescriptor {

    private final int offset;

    public OffsetDescriptor(Optional<String> pOriginFileName, int pOffset) {
      this(pOriginFileName, pOffset, true);
    }

    private OffsetDescriptor(
        Optional<String> pOriginFileName, int pOffset, boolean pMatchBaseName) {
      super(pOriginFileName, pMatchBaseName);
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
      if (!(pObj instanceof OffsetDescriptor)) {
        return false;
      }
      OffsetDescriptor other = (OffsetDescriptor) pObj;
      return Objects.equals(getOriginFileName(), other.getOriginFileName())
          && offset == other.offset;
    }

    @Override
    public boolean matches(FileLocation pFileLocation) {
      return super.matches(pFileLocation)
          && pFileLocation.getNodeOffset() <= offset
          && pFileLocation.getNodeOffset() + pFileLocation.getNodeLength() > offset;
    }

    @Override
    public String toString() {
      return "OFFSET " + offset;
    }
  }
}
