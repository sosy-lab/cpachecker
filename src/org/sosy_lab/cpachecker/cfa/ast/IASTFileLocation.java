package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTFileLocation implements
    org.eclipse.cdt.core.dom.ast.IASTFileLocation {

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

  @Override
  @Deprecated
  public IASTFileLocation asFileLocation() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getEndingLineNumber() {
    return endineLine;
  }

  @Override
  public String getFileName() {
    return fileName;
  }

  @Override
  public int getNodeLength() {
    return length;
  }

  @Override
  public int getNodeOffset() {
    return offset;
  }

  @Override
  public int getStartingLineNumber() {
    return startingLine;
  }

}
