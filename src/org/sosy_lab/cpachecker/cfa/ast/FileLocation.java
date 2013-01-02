/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

public final class FileLocation {

  private final int endineLine;
  private final String fileName;
  private final int length;
  private final int offset;
  private final int startingLine;

  public FileLocation(int pEndineLine, String pFileName, int pLength,
      int pOffset, int pStartingLine) {
    endineLine = pEndineLine;
    fileName = pFileName;
    length = pLength;
    offset = pOffset;
    startingLine = pStartingLine;
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
    int result = 1;
    result = prime * result + endineLine;
    result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
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
    if (this == obj) { return true; }
    if (obj == null) { return false; }
    if (!(obj instanceof FileLocation)) { return false; }
    FileLocation other = (FileLocation) obj;
    if (endineLine != other.endineLine) { return false; }
    if (fileName == null) {
      if (other.fileName != null) { return false; }
    } else if (!fileName.equals(other.fileName)) { return false; }
    if (length != other.length) { return false; }
    if (offset != other.offset) { return false; }
    if (startingLine != other.startingLine) { return false; }
    return true;
  }

}
