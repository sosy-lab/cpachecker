// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public class FileLocationUtils {
  public static boolean entails(FileLocation a, FileLocation b) {
    return covers(b.getNodeOffset(), a) && covers(b.getNodeOffset() + b.getNodeLength(), a);
  }

  @SuppressWarnings("unused")
  public static boolean overlaps(FileLocation a, FileLocation b) {
    return covers(a.getNodeOffset(), b) || covers(a.getNodeOffset() + a.getNodeLength(), b);
  }

  private static boolean covers(int nodeOffset, FileLocation loc) {
    return nodeOffset <= loc.getNodeOffset() + loc.getNodeLength()
        && nodeOffset >= loc.getNodeOffset();
  }

  public static FileLocation merge(FileLocation a, FileLocation b) {
    if (a == null) {
      return b;
    }
    int startOffset = Math.min(a.getNodeOffset(), b.getNodeOffset());
    int endOffset =
        Math.max(a.getNodeOffset() + a.getNodeLength(), b.getNodeOffset() + b.getNodeLength());
    int length = endOffset - startOffset;
    int startLine = Math.min(a.getStartingLineNumber(), b.getStartingLineNumber());
    int endLine = Math.max(a.getEndingLineNumber(), b.getEndingLineNumber());
    int startColumn = Math.min(a.getNodeOffset(), b.getNodeOffset());
    return new FileLocation(a.getFileName(), startOffset, length, startLine, endLine, startColumn);
  }
}
