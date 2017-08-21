/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.interval;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

/** Visitor that get's the interval from an expression, */
class ExpressionValueVisitor extends DefaultCExpressionVisitor<NumberInterface, UnrecognizedCCodeException>
        implements CRightHandSideVisitor<NumberInterface, UnrecognizedCCodeException> {

    private final IntervalAnalysisState readableState;

    private final CFAEdge cfaEdge;

    public ExpressionValueVisitor(IntervalAnalysisState pState, CFAEdge edge) {
        readableState = pState;
        cfaEdge = edge;
    }

    @Override
    protected NumberInterface visitDefault(CExpression expression) {
        return new CreatorIntegerInterval().factoryMethod(null).UNBOUND();
    }

    @Override
    public NumberInterface visit(CBinaryExpression binaryExpression) throws UnrecognizedCCodeException {
        NumberInterface interval1 = binaryExpression.getOperand1().accept(this);
        NumberInterface interval2 = binaryExpression.getOperand2().accept(this);

        if (interval1 == null || interval2 == null) {
            return new CreatorIntegerInterval().factoryMethod(null).UNBOUND();
        }

        BinaryOperator operator = binaryExpression.getOperator();
        if (operator.isLogicalOperator()) {
            return getLogicInterval(operator, interval1, interval2);
        } else {
            return getArithmeticInterval(operator, interval1, interval2);
        }
    }

    private static NumberInterface getLogicInterval(BinaryOperator operator, NumberInterface interval1,
            NumberInterface interval2) {
        switch (operator) {
        case EQUALS:
            if (!interval1.intersects(interval2)) {
                return new CreatorIntegerInterval().factoryMethod(null).ZERO();
            } else if (interval1.getLow().equals(interval1.getHigh()) && interval1.equals(interval2)) {
                // singular interval, [5;5]==[5;5]
                return new CreatorIntegerInterval().factoryMethod(null).ONE();
            } else {
                return new CreatorIntegerInterval().factoryMethod(null).BOOLEAN_INTERVAL();
            }

        case NOT_EQUALS:
            if (!interval1.intersects(interval2)) {
                return new CreatorIntegerInterval().factoryMethod(null).ONE();
            } else if (interval1.getLow().equals(interval1.getHigh()) && interval1.equals(interval2)) {
                // singular interval, [5;5]!=[5;5]
                return new CreatorIntegerInterval().factoryMethod(null).ZERO();
            } else {
                return new CreatorIntegerInterval().factoryMethod(null).BOOLEAN_INTERVAL();
            }

        case GREATER_THAN:
            if (interval1.isGreaterThan(interval2)) {
                return new CreatorIntegerInterval().factoryMethod(null).ONE();
            } else if (interval2.isGreaterOrEqualThan(interval1)) {
                return new CreatorIntegerInterval().factoryMethod(null).ZERO();
            } else {
                return new CreatorIntegerInterval().factoryMethod(null).BOOLEAN_INTERVAL();
            }

        case GREATER_EQUAL: // a>=b == a+1>b, works only for integers
            return getLogicInterval(BinaryOperator.GREATER_THAN,
                    interval1.plus(new CreatorIntegerInterval().factoryMethod(null).ONE()), interval2);

        case LESS_THAN: // a<b == b>a
            return getLogicInterval(BinaryOperator.GREATER_THAN, interval2, interval1);

        case LESS_EQUAL: // a<=b == b+1>a, works only for integers
            return getLogicInterval(BinaryOperator.GREATER_THAN,
                    interval2.plus(new CreatorIntegerInterval().factoryMethod(null).ONE()), interval1);

        default:
            throw new AssertionError("unknown binary operator: " + operator);
        }
    }

    private static NumberInterface getArithmeticInterval(BinaryOperator operator, NumberInterface interval1,
            NumberInterface interval2) {

        switch (operator) {
        case PLUS:
            return interval1.plus(interval2);
        case MINUS:
            return interval1.minus(interval2);
        case MULTIPLY:
            return interval1.times(interval2);
        case DIVIDE:
            return interval1.divide(interval2);
        case SHIFT_LEFT:
            return interval1.shiftLeft(interval2);
        case SHIFT_RIGHT:
            return interval1.shiftRight(interval2);
        case MODULO:
            return interval1.modulo(interval2);
        case BINARY_AND:
        case BINARY_OR:
        case BINARY_XOR:
            return new CreatorIntegerInterval().factoryMethod(null).UNBOUND();
        default:
            throw new AssertionError("unknown binary operator: " + operator);
        }
    }

    @Override
    public NumberInterface visit(CCastExpression cast) throws UnrecognizedCCodeException {
        return cast.getOperand().accept(this);
    }

    @Override
    public NumberInterface visit(CFunctionCallExpression functionCall) {
        return new CreatorIntegerInterval().factoryMethod(null).UNBOUND();
    }

    @Override
    public NumberInterface visit(CCharLiteralExpression charLiteral) {
        return new CreatorIntegerInterval().factoryMethod((long) charLiteral.getCharacter());
    }

    @Override
    public NumberInterface visit(CImaginaryLiteralExpression exp) throws UnrecognizedCCodeException {
        return exp.getValue().accept(this);
    }

    @Override
    public NumberInterface visit(CIntegerLiteralExpression integerLiteral) {
        // TODO BigInteger
        return new CreatorIntegerInterval().factoryMethod(integerLiteral.asLong());
    }

    @Override
    public NumberInterface visit(CFloatLiteralExpression floatLiteral) {
        // TODO BigDecimal
        return new CreatorDoubleInterval().factoryMethod(floatLiteral.getValue().doubleValue());
    }

    @Override
    public NumberInterface visit(CIdExpression identifier) {
        if (identifier.getDeclaration() instanceof CEnumerator) {
            return new CreatorIntegerInterval().factoryMethod(((CEnumerator) identifier.getDeclaration()).getValue());
        }
        final String variableName = identifier.getDeclaration().getQualifiedName();
        if (readableState.contains(variableName)) {
            return readableState.getInterval(variableName);
        } else {
            return new CreatorIntegerInterval().factoryMethod(null).UNBOUND();
        }
    }

    @Override
    public NumberInterface visit(CUnaryExpression unaryExpression) throws UnrecognizedCCodeException {
        NumberInterface interval = unaryExpression.getOperand().accept(this);
        switch (unaryExpression.getOperator()) {
        case MINUS:
            return interval.negate();
        case AMPER:
        case TILDE:
            return new CreatorIntegerInterval().factoryMethod(null).UNBOUND(); // valid
                                                                               // expression,
                                                                               // but
                                                                               // it's
                                                                               // a
                                                                               // pointer
                                                                               // value
        default:
            throw new UnrecognizedCCodeException("unknown unary operator", cfaEdge, unaryExpression);
        }
    }
}