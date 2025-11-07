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
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTagReference;

public final class CFATaggedNode extends CFANode {
  private final List<SvLibTagProperty> tagAttributes;
  private final List<SvLibTagReference> tagReferences;

  public CFATaggedNode(
      AFunctionDeclaration pFunction,
      List<SvLibTagProperty> pTagAttributes,
      List<SvLibTagReference> pTagReferences) {
    super(pFunction);
    tagAttributes = pTagAttributes;
    tagReferences = pTagReferences;
  }

  public List<SvLibTagProperty> getTagAttributes() {
    return tagAttributes;
  }

  public List<SvLibTagReference> getTagReferences() {
    return tagReferences;
  }
}
