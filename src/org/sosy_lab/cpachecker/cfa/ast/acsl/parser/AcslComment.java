// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class AcslComment {

  private final FileLocation fileLocation;
  private final String commentString;
  @Nullable private CFANode cfaNode;

  public AcslComment(FileLocation pFileLocation, String pCommentString) {
    fileLocation = pFileLocation;
    commentString = pCommentString;
    cfaNode = null;
  }

  public void updateCfaNode(@Nullable CFANode pCfaNode) {
    // Ensure the Cfa Node for an Acsl Comment can only be set once
    if (cfaNode == null) {
      cfaNode = pCfaNode;
    }
  }

  public FileLocation getFileLocation() {
    return fileLocation;
  }

  public String getComment() {
    return commentString;
  }

  @Nullable
  public CFANode getCfaNode() {
    return cfaNode;
  }
}
