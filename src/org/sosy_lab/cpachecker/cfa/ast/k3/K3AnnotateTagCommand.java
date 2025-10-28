// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3AnnotateTagCommand implements K3Command {
  @Serial private static final long serialVersionUID = 5333102692293273124L;

  private final String tagName;
  private final List<K3TagProperty> tags;
  private final FileLocation fileLocation;

  public K3AnnotateTagCommand(
      String pTagName, List<K3TagProperty> pTags, FileLocation pFileLocation) {
    tagName = pTagName;
    tags = pTags;
    fileLocation = pFileLocation;
  }

  public String getTagName() {
    return tagName;
  }

  public List<K3TagProperty> getTags() {
    return tags;
  }

  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + tagName.hashCode();
    result = prime * result + tags.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof K3AnnotateTagCommand other
        && tagName.equals(other.tagName)
        && tags.equals(other.tags);
  }
}
