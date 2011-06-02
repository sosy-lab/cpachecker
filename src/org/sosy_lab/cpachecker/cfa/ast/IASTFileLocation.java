/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

public final class IASTFileLocation {

  private final int endineLine;
  private final String fileName;
  private final int length;
  private final int offset;
  private final int startingLine;

  public IASTFileLocation(int pEndineLine, String pFileName, int pLength,
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
}
