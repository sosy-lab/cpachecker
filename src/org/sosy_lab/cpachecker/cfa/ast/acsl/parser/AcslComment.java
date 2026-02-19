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
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class AcslComment {

  private final FileLocation fileLocation;
  private final String commentString;
  private @NonNull Optional<CFANode> cfaNode;

  public AcslComment(FileLocation pFileLocation, String pCommentString) {
    fileLocation = pFileLocation;
    commentString = pCommentString;
    cfaNode = Optional.empty();
  }

  public void updateCfaNode(@NonNull CFANode pCfaNode) {
    // Ensure the Cfa Node for an Acsl Comment can only be set once
    Verify.verify(cfaNode.isEmpty());
    cfaNode = Optional.of(pCfaNode);
  }

  public FileLocation getFileLocation() {
    return fileLocation;
  }

  public String getComment() {
    return commentString;
  }

  @Override
  public String toString() {
    return "'" + commentString + "'" + " at " + fileLocation;
  }

  @Nullable
  public Optional<CFANode> getCfaNode() {
    return cfaNode;
  }

  public Boolean hasCfaNode() {
    return cfaNode.isPresent();
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
