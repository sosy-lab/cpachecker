// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagProperty;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagReference;

public final class CFATaggedNode extends CFANode {
  private final List<K3TagProperty> tagAttributes;
  private final List<K3TagReference> tagReferences;

  public CFATaggedNode(
      AFunctionDeclaration pFunction,
      List<K3TagProperty> pTagAttributes,
      List<K3TagReference> pTagReferences) {
    super(pFunction);
    tagAttributes = pTagAttributes;
    tagReferences = pTagReferences;
  }

  public List<K3TagProperty> getTagAttributes() {
    return tagAttributes;
  }

  public List<K3TagReference> getTagReferences() {
    return tagReferences;
  }
}
