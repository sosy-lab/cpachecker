// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import com.google.common.base.Verify;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class AcslComment {

  private final FileLocation fileLocation;
  private final String commentString;
  private final ParserRuleContext commentContext;
  @Nullable private CFANode cfaNode;

  public AcslComment(
      FileLocation pFileLocation, String pCommentString, ParserRuleContext pCommentContext) {
    fileLocation = pFileLocation;
    commentString = pCommentString;
    commentContext = pCommentContext;
    cfaNode = null;
  }

  public void updateCfaNode(@Nullable CFANode pCfaNode) {
    // Ensure the Cfa Node for an Acsl Comment can only be set once
    Verify.verify(cfaNode == null);
    cfaNode = pCfaNode;
  }

  public FileLocation getFileLocation() {
    return fileLocation;
  }

  public String getComment() {
    return commentString;
  }

  public ParserRuleContext getCommentContext() {
    return commentContext;
  }

  @Nullable
  public CFANode getCfaNode() {
    return cfaNode;
  }

  public Boolean hasCfaNode() {
    return cfaNode != null;
  }

  public boolean noCommentInBetween(FileLocation nextStatement, List<AcslComment> otherComments) {
    for (AcslComment other : otherComments) {
      if (!other.equals(this)
          && other.getFileLocation().getNodeOffset()
              > this.getFileLocation().getNodeOffset() + this.getFileLocation().getNodeLength()
          && other.getFileLocation().getNodeOffset() + other.getFileLocation().getNodeLength()
              < nextStatement.getNodeOffset()) {
        // There is an annotation inbetween the comment and the statement
        return false;
      }
    }
    return true;
  }
}
