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
