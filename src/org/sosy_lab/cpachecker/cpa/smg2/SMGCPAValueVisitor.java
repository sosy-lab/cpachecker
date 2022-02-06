// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAValueExpressionEvaluator;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * This visitor visits values mostly on the right hand side to get values (SMG or not) (but also on
 * the left hand side, for example concrete values used in array access) Important: we need to reuse
 * the state given back by other visitors and use the state in this class just to give it to the
 * next/innermost visitor! Read operations have side effects, hence why using the most up to date
 * state is important.
 */
public class SMGCPAValueVisitor
    extends DefaultCExpressionVisitor<List<SMGValueAndSMGState>, CPATransferException>
    implements CRightHandSideVisitor<List<SMGValueAndSMGState>, CPATransferException> {

  // TODO: remove CPAException and use more specific exceptions

  // The evaluator translates C expressions into the SMG counterparts and vice versa.
  private final SMGCPAValueExpressionEvaluator evaluator;

  private final SMGState state;

  /** This edge is only to be used for debugging/logging! */
  private final CFAEdge cfaEdge;

  public SMGCPAValueVisitor(
      SMGCPAValueExpressionEvaluator pEvaluator, SMGState currentState, CFAEdge edge) {
    evaluator = pEvaluator;
    state = currentState;
    cfaEdge = edge;
  }

  @Override
  protected List<SMGValueAndSMGState> visitDefault(CExpression pExp) throws CPATransferException {
    // Just get a default value
    return null;
  }

  @Override
  public List<SMGValueAndSMGState> visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws CPATransferException {
    // TODO: investigate whats possible here.
    return null;
  }

  @Override
  public List<SMGValueAndSMGState> visit(CArraySubscriptExpression e) throws CPATransferException {
    // Array subscript is default Java array usage. Example: array[5]
    // In C this can be translated to *(array + 5). Note: this is commutative!
    // TODO: how to handle *(array++) etc.? This case equals *(array + 1). Would the ++ case come
    // from an assignment edge?

    // Get the value from the array and return the value + state

    return visitDefault(e);
  }

  @Override
  public List<SMGValueAndSMGState> visit(CBinaryExpression e) throws CPATransferException {
    // TODO: remove from this class, move to a dedicated
    // From assumption edge
    // binary expression, examples: +, -, *, /, ==, !=, < ....
    // visit left and right, then use the expression and return it. This also means we need to
    // create new SMG values (symbolic value ranges) for them, but don't save them in the SMG right
    // away (save, not write!) as this is only done when write is used.

    return visitDefault(e);
  }

  @Override
  public List<SMGValueAndSMGState> visit(CCastExpression e) throws CPATransferException {
    // Casts are not trivial with SMGs as there might be type reinterpretation used inside the SMGs,
    // but this should be taken care of by the SMGCPAValueExpressionEvaluator.
    // Get the type and value from the nested expression (might be SMG) and cast the value
    return visitDefault(e);
  }

  @Override
  public List<SMGValueAndSMGState> visit(CComplexCastExpression e) throws CPATransferException {
    // TODO: do we need those?
    // Cast for complex numbers?
    return visitDefault(e);
  }

  @Override
  public List<SMGValueAndSMGState> visit(CFieldReference e) throws CPATransferException {
    // Get the object holding the field (should be struct/union)
    // I most likely need the CFAEdge for that
    // Read the value of the field from the object

    return visitDefault(e);
  }

  @Override
  public List<SMGValueAndSMGState> visit(CIdExpression e) throws CPATransferException {
    // essentially variables
    // Either CEnumerator, CVariableDeclaration, CParameterDeclaration
    // Could also be a type/function declaration, decide if we need those.

    // Get the var using the stack SMG, read and return
    return visitDefault(e);
  }

  @Override
  public List<SMGValueAndSMGState> visit(CCharLiteralExpression e) throws CPATransferException {
    // Simple character expression
    char value = e.getCharacter();

    // If the value is == 0 we return the zero value without checking as this one always exists.
    // Check if the value exists already, if it does, return that, else create a new one and return
    // that one.
    return visitDefault(e);
  }

  @Override
  public List<SMGValueAndSMGState> visit(CImaginaryLiteralExpression e)
      throws CPATransferException {
    // TODO: do we even need those?
    // Imaginary part for complex numbers
    return visitDefault(e);
  }

  @Override
  public List<SMGValueAndSMGState> visit(CFloatLiteralExpression e) throws CPATransferException {
    // Floating point value expression
    BigDecimal value = e.getValue();

    // If the value is == 0 we return the zero value without checking as this one always exists.
    // Check if the value exists already, if it does, return that, else create a new one and return
    // that one.

    return visitDefault(e);
  }

  @Override
  public List<SMGValueAndSMGState> visit(CIntegerLiteralExpression e) throws CPATransferException {
    // Simple int expression
    BigInteger value = e.getValue();

    // If the value is == 0 we return the zero value without checking as this one always exists.
    // Check if the value exists already, if it does, return that, else create a new one and return
    // that one.
    return visitDefault(e);
  }

  @Override
  public List<SMGValueAndSMGState> visit(CStringLiteralExpression e) throws CPATransferException {
    // Either split the String into chars or simply assign in as a single big value
    return visitDefault(e);
  }

  @Override
  public List<SMGValueAndSMGState> visit(CTypeIdExpression e) throws CPATransferException {
    // Operators:
    // sizeOf, typeOf and
    // _Alignof or alignof = the number of bytes between successive addresses, essentially a fancy
    // name for size

    // SMGs have type reinterpretation! Get the type of the SMG and translate it back to the C type.
    return visitDefault(e);
  }

  @Override
  public List<SMGValueAndSMGState> visit(CUnaryExpression e) throws CPATransferException {
    // Unary expression types like & (address of operator), sizeOf(), ++, - (unary minus), --, !
    // (not)
    // Split up into their operators, handle each. Most are not that difficult.
    // & needs the address of an object, so we need to get the mapping or create one to an SMG
    // object

    return visitDefault(e);
  }

  @Override
  public List<SMGValueAndSMGState> visit(CPointerExpression e) throws CPATransferException {
    // Pointers can be a multitude of things in C
    // Get the operand of the pointer, get the type of that and then split into the different cases
    // to handle them
    // Once we dereference a object with this, we return the objects value at the correct offset.
    // *(array + 2) for example

    return visitDefault(e);
  }

  @Override
  public List<SMGValueAndSMGState> visit(CAddressOfLabelExpression e) throws CPATransferException {
    // && expression
    // This is not in the C standard, just gcc
    // https://gcc.gnu.org/onlinedocs/gcc/Labels-as-Values.html
    // Returns a address to a function

    return visitDefault(e);
  }
}
