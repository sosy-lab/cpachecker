// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class AcslComment {

  private final FileLocation fileLocation;
  private final String commentString;
  private CFANode cfaNode;

  public AcslComment(FileLocation pFileLocation, String pCommentString) {
    fileLocation = pFileLocation;
    commentString = pCommentString;
    cfaNode = CFANode.newDummyCFANode();
  }

  public void updateCfaNode(CFANode pCfaNode) {
    cfaNode = pCfaNode;
  }

  public FileLocation getFileLocation() {
    return fileLocation;
  }

  public String getComment() {
    return commentString;
  }

  public CFANode getCfaNode() {
    return cfaNode;
  }
}
