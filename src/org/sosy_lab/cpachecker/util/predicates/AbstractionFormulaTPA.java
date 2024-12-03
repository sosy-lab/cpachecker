// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.expressions.ExpressionTrees.FUNCTION_DELIMITER;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Set;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState.TranslationToExpressionTreeFailedException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * TODO: Add documentation
 */
public class AbstractionFormulaTPA extends AbstractionFormula implements Serializable {

  public AbstractionFormulaTPA(
      FormulaManagerView mgr,
      Region pRegion,
      BooleanFormula pFormula,
      BooleanFormula pInstantiatedFormula,
      PathFormula pBlockFormula,
      Set<Integer> pIdOfStoredAbstractionReused) {
    super(mgr, pRegion, pFormula, pInstantiatedFormula, pBlockFormula,
        pIdOfStoredAbstractionReused);
  }
}
