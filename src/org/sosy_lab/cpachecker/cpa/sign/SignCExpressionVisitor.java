/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.sign;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cpa.interval.Creator;
import org.sosy_lab.cpachecker.cpa.interval.NumberInterface;
import org.sosy_lab.cpachecker.cpa.interval.UnifyAnalysisState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SignCExpressionVisitor extends DefaultCExpressionVisitor<NumberInterface, UnrecognizedCodeException>
        implements CRightHandSideVisitor<NumberInterface, UnrecognizedCodeException> {

    private CFAEdge edgeOfExpr;

    private UnifyAnalysisState state;

    private SignTransferRelation transferRel;

    private static Creator creatorSign = new SIGNCreator();

    public SignCExpressionVisitor(CFAEdge pEdgeOfExpr, UnifyAnalysisState pState, SignTransferRelation pTransferRel) {
        edgeOfExpr = pEdgeOfExpr;
        state = pState;
        transferRel = pTransferRel;
    }

    @Override
    public NumberInterface visit(CFunctionCallExpression pIastFunctionCallExpression) throws UnrecognizedCodeException {
        // TODO possibly treat typedef types differently
        // e.g. x = non_det() where non_det is extern, unknown function allways assume
        // returns any value
        if (pIastFunctionCallExpression.getExpressionType() instanceof CSimpleType
                || pIastFunctionCallExpression.getExpressionType() instanceof CTypedefType
                || pIastFunctionCallExpression.getExpressionType() instanceof CPointerType) {
            return creatorSign.factoryMethod(SIGNCreator.ALL);
        }
        throw new UnrecognizedCodeException("unsupported code found", edgeOfExpr);
    }

    @Override
    protected NumberInterface visitDefault(CExpression pExp) throws UnrecognizedCodeException {
        throw new UnrecognizedCodeException("unsupported code found", edgeOfExpr);
    }

    @Override
    public NumberInterface visit(CCastExpression e) throws UnrecognizedCodeException {
        return e.getOperand().accept(this); // TODO correct?
    }

    @Override
    public NumberInterface visit(CFieldReference e) throws UnrecognizedCodeException {
        return state.getElement(MemoryLocation.valueOf(transferRel.getScopedVariableName(e)));
    }

    @Override
    public NumberInterface visit(CArraySubscriptExpression e) throws UnrecognizedCodeException {
        // TODO possibly may become preciser
        return creatorSign.factoryMethod(SIGNCreator.ALL);
    }

    @Override
    public NumberInterface visit(CPointerExpression e) throws UnrecognizedCodeException {
        // TODO possibly may become preciser
        return creatorSign.factoryMethod(SIGNCreator.ALL);
    }

    @Override
    public NumberInterface visit(CIdExpression pIastIdExpression) throws UnrecognizedCodeException {
        return state.getElement(MemoryLocation.valueOf(transferRel.getScopedVariableName(pIastIdExpression)));
    }

    @Override
    public NumberInterface visit(CBinaryExpression pIastBinaryExpression) throws UnrecognizedCodeException {
        NumberInterface left = pIastBinaryExpression.getOperand1().accept(this);
        NumberInterface right = pIastBinaryExpression.getOperand2().accept(this);
        Set<NumberInterface> leftAtomSigns = left.split();
        Set<NumberInterface> rightAtomSigns = right.split();
        NumberInterface result = creatorSign.factoryMethod(SIGNCreator.EMPTY);
        for (List<NumberInterface> signCombi : Sets.cartesianProduct(ImmutableList.of(leftAtomSigns, rightAtomSigns))) {
            result = result.union(evaluateExpression(signCombi.get(0), pIastBinaryExpression, signCombi.get(1)));
        }
        return result;
    }

    private NumberInterface evaluateExpression(NumberInterface pLeft, CBinaryExpression pExp, NumberInterface pRight)
            throws UnsupportedCCodeException {
        switch (pExp.getOperator()) {
        case PLUS:
            return evaluatePlusOperator(pLeft, pExp.getOperand1(), pRight, pExp.getOperand2());
        case MINUS:
            return evaluateMinusOperator(pLeft, pRight, pExp.getOperand2());
        case MULTIPLY:
            return pLeft.minus(pRight);
        case DIVIDE:
            if (pRight.getNumber().equals(SIGNCreator.ZERO)) {// == SIGN.ZERO) {
                transferRel.logger.log(Level.WARNING, "Possibly dividing by zero", edgeOfExpr);
                return creatorSign.factoryMethod(SIGNCreator.ALL);// SIGN.ALL;
            }
            return pLeft.divide(pRight);
        case MODULO:
            return pLeft.modulo(pRight);
        case BINARY_AND:
            return pLeft.binaryAnd(pRight);
        case LESS_EQUAL:
            return pLeft.evaluateLessEqualOperator(pRight);
        case GREATER_EQUAL:
            return pRight.evaluateLessEqualOperator(pLeft);
        case LESS_THAN:
            return pLeft.evaluateLessOperator(pRight);
        case GREATER_THAN:
            return pRight.evaluateLessOperator(pLeft);
        case EQUALS:
            return pLeft.evaluateEqualOperator(pRight);
        default:
            throw new UnsupportedCCodeException("Not supported", edgeOfExpr);
        }
    }

    @Override
    public NumberInterface visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws UnrecognizedCodeException {
        BigDecimal value = pIastFloatLiteralExpression.getValue();
        int cResult = value.compareTo(BigDecimal.ZERO);
        if (cResult == 1) {
            return creatorSign.factoryMethod(SIGNCreator.PLUS);// SIGN.PLUS;
        } else if (cResult == -1) {
            return creatorSign.factoryMethod(SIGNCreator.MINUS);// SIGN.MINUS;
        }
        return creatorSign.factoryMethod(SIGNCreator.ZERO);// SIGN.ZERO;
    }

    @Override
    public NumberInterface visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
            throws UnrecognizedCodeException {
        BigInteger value = pIastIntegerLiteralExpression.getValue();
        int cResult = value.compareTo(BigInteger.ZERO);
        if (cResult == 1) {
            return creatorSign.factoryMethod(SIGNCreator.PLUS);// SIGN.PLUS;
        } else if (cResult == -1) {
            return creatorSign.factoryMethod(SIGNCreator.MINUS);// SIGN.MINUS;
        }
        return creatorSign.factoryMethod(SIGNCreator.ZERO);// SIGN.ZERO;
    }

    @Override
    public NumberInterface visit(CStringLiteralExpression e) throws UnrecognizedCodeException {
        return creatorSign.factoryMethod(SIGNCreator.ALL);// SIGN.ALL;
    }

    @Override
    public NumberInterface visit(CCharLiteralExpression e) throws UnrecognizedCodeException {
        return creatorSign.factoryMethod(SIGNCreator.ALL);// SIGN.ALL;
    }

    @Override
    public NumberInterface visit(CUnaryExpression pIastUnaryExpression) throws UnrecognizedCodeException {
        switch (pIastUnaryExpression.getOperator()) {
        case MINUS:
            NumberInterface result = creatorSign.factoryMethod(SIGNCreator.EMPTY);// SIGN.EMPTY;
            NumberInterface operandSign = pIastUnaryExpression.getOperand().accept(this);
            for (NumberInterface atomSign : operandSign.split()) {
                result = result.union(evaluateUnaryExpression(pIastUnaryExpression.getOperator(), atomSign));
            }
            return result;
        default:
            throw new UnsupportedCCodeException("Not supported", edgeOfExpr, pIastUnaryExpression);
        }
    }

    private static NumberInterface evaluateUnaryExpression(UnaryOperator operator, NumberInterface operand) {
        if (operand.getNumber().equals(SIGNCreator.ZERO)) {// == SIGN.ZERO) {
            return creatorSign.factoryMethod(SIGNCreator.ZERO);// SIGN.ZERO;
        }
        if (operator == UnaryOperator.MINUS && operand.getNumber().equals(1)) {// == SIGN.PLUS) {
            return creatorSign.factoryMethod(SIGNCreator.MINUS);// SIGN.MINUS;
        }
        return creatorSign.factoryMethod(SIGNCreator.PLUS);// SIGN.MINUS;???
    }

    private NumberInterface evaluatePlusOperator(NumberInterface pLeft, CExpression pLeftExp, NumberInterface pRight,
            CExpression pRightExp) {
        // Special case: - + 1 => -0, 1 + - => -0
        if ((pLeft.getNumber().equals(SIGNCreator.MINUS)// == SIGN.MINUS
                && (pRightExp instanceof CIntegerLiteralExpression)
                && ((CIntegerLiteralExpression) pRightExp).getValue().equals(BigInteger.ONE))
                || ((pLeftExp instanceof CIntegerLiteralExpression)
                        && ((CIntegerLiteralExpression) pLeftExp).getValue().equals(BigInteger.ONE)
                        && pRight.getNumber().equals(SIGNCreator.MINUS))) {// == SIGN.MINUS)) {
            return creatorSign.factoryMethod(SIGNCreator.MINUS0);// SIGN.MINUS0;
        }
        // Special case: +0 + 1 => +, 1 + +0 => +
        if ((pLeft.getNumber().equals(5)// == SIGN.PLUS0
                && (pRightExp instanceof CIntegerLiteralExpression)
                && ((CIntegerLiteralExpression) pRightExp).getValue().equals(BigInteger.ONE))
                || ((pLeftExp instanceof CIntegerLiteralExpression)
                        && ((CIntegerLiteralExpression) pLeftExp).getValue().equals(BigInteger.ONE)
                        && pRight.getNumber().equals(SIGNCreator.PLUS0))) {// == SIGN.PLUS0)) {
            return creatorSign.factoryMethod(SIGNCreator.PLUS);// SIGN.PLUS;
        }
        NumberInterface leftToRightResult = pLeft.plus(pRight);
        NumberInterface rightToLeftResult = pRight.plus(pLeft);
        return leftToRightResult.union(rightToLeftResult);
    }

    private NumberInterface evaluateMinusOperator(NumberInterface pLeft, NumberInterface pRight,
            CExpression pRightExp) {
        // Special case: + - 1 => +0
        if (pLeft.getNumber().equals(1)// == SIGN.PLUS
                && (pRightExp instanceof CIntegerLiteralExpression)
                && ((CIntegerLiteralExpression) pRightExp).getValue().equals(BigInteger.ONE)) {
            return creatorSign.factoryMethod(SIGNCreator.PLUS0);// SIGN.PLUS0;
        }
        // Special case: -0 - 1 => -
        if (pLeft.getNumber().equals(6)// == SIGN.MINUS0
                && (pRightExp instanceof CIntegerLiteralExpression)
                && ((CIntegerLiteralExpression) pRightExp).getValue().equals(BigInteger.ONE)) {
            return creatorSign.factoryMethod(SIGNCreator.MINUS);// SIGN.MINUS;
        }
        if (pRight.getNumber().equals(4)) {// == SIGN.ZERO) {
            return pLeft;
        }
        if (pLeft.getNumber().equals(4)) {// == SIGN.ZERO) {
            switch (pRight.getNumber().intValue()) {
            case 1:// PLUS
                return creatorSign.factoryMethod(SIGNCreator.MINUS);// SIGN.MINUS;
            case 2:// MINUS
                return creatorSign.factoryMethod(SIGNCreator.PLUS);// SIGN.PLUS;
            case 5:// PLUS0
                return creatorSign.factoryMethod(SIGNCreator.MINUS0);// SIGN.MINUS0;
            case 6:// MINUS0
                return creatorSign.factoryMethod(SIGNCreator.PLUS0);// SIGN.PLUS0;
            default:
                return pRight;
            }
        }
        // if (pLeft == SIGN.PLUS && pRight == SIGN.MINUS) {
        // return SIGN.PLUS;
        // }
        if (pLeft.getNumber().equals(1) && pRight.getNumber().equals(SIGNCreator.MINUS)) {
            return creatorSign.factoryMethod(SIGNCreator.PLUS);
        }
        // if (pLeft == SIGN.MINUS && pRight == SIGN.PLUS) {
        // return SIGN.MINUS;
        // }
        if (pLeft.getNumber().equals(SIGNCreator.MINUS) && pRight.getNumber().equals(1)) {
            return creatorSign.factoryMethod(SIGNCreator.MINUS);
        }
        return creatorSign.factoryMethod(SIGNCreator.ALL);// SIGN.ALL;
    }
}
