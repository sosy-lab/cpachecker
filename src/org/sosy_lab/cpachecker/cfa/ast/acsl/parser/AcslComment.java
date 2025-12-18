// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

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
    Verify.verify(cfaNode == null);
    cfaNode = pCfaNode;
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

  public Boolean hasCfaNode() {
    return cfaNode != null;
  }

  public Optional<FunctionEntryNode> nextFunctionEntryNode(
      Collection<FunctionEntryNode> pFunctionEntryNodes) {
    ImmutableSortedSet<FunctionEntryNode> sortedFunctionEntryNodes =
        FluentIterable.from(pFunctionEntryNodes)
            .toSortedSet(Comparator.comparing(FunctionEntryNode::getFileLocation));
    for (FunctionEntryNode node : sortedFunctionEntryNodes) {
      if (fileLocation.getNodeOffset() + fileLocation.getNodeLength()
          < node.getFileLocation().getNodeOffset()) {
        return Optional.of(node);
      }
    }
    return Optional.empty();
  }
}
