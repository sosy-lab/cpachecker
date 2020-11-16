// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAEdgeVisitor;

/** This Transfer-Relation forwards the method 'getAbstractSuccessors()'
 * to an edge-specific sub-method ('AssumeEdge', 'DeclarationEdge', ...).
 * It handles all casting of the edges and their information.
 * There is always an abstract method, that calls either the matching
 * C- or Java-Methods, depending on the type of the edge.
 * A developer should override the methods to get a valid analysis.
 *
 * The following structure shows the control-flow (work-flow) of this class.
 *
 * The tuple (C,J) represents the call of C- or Java-specific methods.
 * A user can either override the method itself, or the C- or Java-specific method.
 * If a C- or Java-specific method is called, but not overridden, it throws an assertion.
 *
 * 1. setInfo
 * 2. preCheck
 *
 * 3. getAbstractSuccessors:
 *   - handleAssumption -> C,J
 *   - handleFunctionCallEdge -> C,J
 *   - handleFunctionReturnEdge -> C,J
 *   - handleMultiEdge
 *   - handleSimpleEdge:
 *     -- handleDeclarationEdge -> C,J
 *     -- handleStatementEdge -> C,J
 *     -- handleReturnStatementEdge -> C,J
 *     -- handleBlankEdge
 *     -- handleFunctionSummaryEdge
 *
 * 4. postProcessing
 * 5. resetInfo
 *
 * Generics:
 *  - S type of intermediate result, should be equal to T or Collection<T>,
 *      should be converted/copied into an Object of type Collection<T> in method 'postProcessing'.
 *  - T type of State
 *  - P type of Precision
 */
public abstract class ForwardingTransferRelation2<S, T extends AbstractState, P extends Precision>
    extends SingleEdgeTransferRelation {



  /**
   * This is the main method that delegates the control-flow to the
   * corresponding edge-type-specific methods.
   * In most cases there is no need to override this method. */
  @Override
  public Collection<T> getAbstractSuccessorsForEdge(
      final AbstractState abstractState, final Precision abstractPrecision, final CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {

    @SuppressWarnings("unchecked")
    final Collection<T> preCheck = preCheck((T) abstractState, (P) abstractPrecision);
    if (preCheck != null) { return preCheck; }

    final S successor;

    successor = setInfo(abstractState, abstractPrecision, cfaEdge).visit(cfaEdge);

    final Collection<T> result = postProcessing(successor, cfaEdge);

    return result;
  }

  @SuppressWarnings("unchecked")
  protected CFAEdgeVisitor<S> setInfo(
      final AbstractState pAbstractState,
      final Precision abstractPrecision,
      final CFAEdge cfaEdge) {
    return new StatefulCFAEdgeVisitor<>((S) pAbstractState, abstractPrecision, cfaEdge);
  }

  /** This is a fast check, if the edge should be analyzed.
   * It returns NULL for further processing,
   * otherwise the return-value for skipping.  */
  @SuppressWarnings("unused")
  protected @Nullable Collection<T> preCheck(T pState, P pPrecision) {
    return null;
  }

  /** This method should convert/cast/copy the intermediate result into a Collection<T>.
   * This method can modify the successor, if needed. */
  @SuppressWarnings({"unchecked", "unused"})
  protected Collection<T> postProcessing(@Nullable S successor, CFAEdge edge) {
    if (successor == null) {
      return ImmutableSet.of();
    } else {
      return Collections.singleton((T)successor);
    }
  }
}
