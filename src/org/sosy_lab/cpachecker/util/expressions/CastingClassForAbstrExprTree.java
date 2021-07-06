// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.expressions;

import com.google.common.collect.Sets;
import java.util.Set;

public class CastingClassForAbstrExprTree<LeafType> {

  public CastingClassForAbstrExprTree() {}

  @SuppressWarnings("unchecked")
  public ExpressionTree<LeafType> cast(ExpressionTree<Object> toCast) {

    if (toCast instanceof And) {
      Set<ExpressionTree<LeafType>> storedElemes = Sets.newHashSet();
      ((And<LeafType>) toCast).forEach(obj -> storedElemes.add(obj));
      return And.of(storedElemes);
    } else if (toCast instanceof Or) {
      Set<ExpressionTree<LeafType>> storedElemes = Sets.newHashSet();
      ((Or<LeafType>) toCast).forEach(obj -> storedElemes.add(obj));
      return Or.of(storedElemes);
    } else {
      return LeafExpression.of(((LeafExpression<LeafType>) toCast).getExpression());
    }
  }
}
