// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAValueExpressionEvaluator;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * TODO: rename. I am bad with names. This class visits objects (SMG or otherwise) that will be
 * assigned to. (LeftHandSide; array[1] = 0;) Returns the proper (SMG)Object + correct offset to be
 * written to. This should not change the state!
 */
public class SMGCPAGetObjectVisitor
    extends DefaultCExpressionVisitor<List<SMGObjectAndOffset>, CPATransferException> {

  // TODO: remove CPAException and use more specific exceptions

  // The evaluator translates C expressions into the SMG counterparts and vice versa.
  private final SMGCPAValueExpressionEvaluator evaluator;

  private final SMGState state;

  /** This edge is only to be used for debugging/logging! */
  private final CFAEdge cfaEdge;

  public SMGCPAGetObjectVisitor(
      SMGCPAValueExpressionEvaluator pEvaluator, SMGState currentState, CFAEdge edge) {
    evaluator = pEvaluator;
    state = currentState;
    cfaEdge = edge;
  }

  @Override
  protected List<SMGObjectAndOffset> visitDefault(CExpression pExp) throws CPATransferException {
    // I currently don't know what to return here. If nothing goes wrong this should be either
    // nothing or write to 0.
    return null;
  }

  @Override
  public List<SMGObjectAndOffset> visit(CArraySubscriptExpression e) throws CPATransferException {
    // Array subscript is default Java array usage. Example: array[5]
    // In C this can be translated to *(array + 5). Note: this is commutative!
    // TODO: how to handle *(array++) etc.? This case equals *(array + 1)

    // Just get the array object + offset and return it
    // The offset is a value, so use the value visitor for that

    return visitDefault(e);
  }

  @Override
  public List<SMGObjectAndOffset> visit(CFieldReference e) throws CPATransferException {
    // Get the object holding the field (should be struct/union)
    // I most likely need the CFAEdge for that
    // Get the SMGObject and the offset

    return visitDefault(e);
  }

  @Override
  public List<SMGObjectAndOffset> visit(CIdExpression e) throws CPATransferException {
    // essentially variables
    // Either CEnumerator, CVariableDeclaration, CParameterDeclaration
    // Could also be a type/function declaration, decide if we need that

    // Get the SMGObject used to save the var and return it. Offset should be 0.

    return visitDefault(e);
  }

  @Override
  public List<SMGObjectAndOffset> visit(CStringLiteralExpression e) throws CPATransferException {
    // TODO: Investigate if we need this here
    return visitDefault(e);
  }

  @Override
  public List<SMGObjectAndOffset> visit(CUnaryExpression e) throws CPATransferException {
    // Unary expression types like & (address of operator), sizeOf(), ++, - (unary minus), --, !
    // (not)
    // Split up into their operators, handle each. Most are not that difficult.
    // & needs the address of an object, so we need to get the mapping or create one to an SMG
    // object

    // ++ -- are both cases in which we have to get the current value and the object holding it and
    // write the value ++ or -- to the same object at the same position!
    // & gives back the address (pointer) of an expression. In our case thats the value that points
    // to the object + the offset, so we return that.

    return visitDefault(e);
  }

  @Override
  public List<SMGObjectAndOffset> visit(CPointerExpression e) throws CPATransferException {
    // Pointers can be a multitude of things in C
    // Get the operand of the pointer, get the type of that and then split into the different cases
    // to handle them
    // On the left hand side (this visitor) this expression dereferences the pointer and returns the
    // underlying object + offset = 0.
    // TODO: can this expression be smth like that: *(array + 2) = ...?

    return visitDefault(e);
  }

  @Override
  public List<SMGObjectAndOffset> visit(CAddressOfLabelExpression e) throws CPATransferException {
    // && expression
    // This is not in the C standard, just gcc
    // https://gcc.gnu.org/onlinedocs/gcc/Labels-as-Values.html

    // This could be leftHandSide but only in goto i think.
    // TODO: do we need that?
    return visitDefault(e);
  }
}
