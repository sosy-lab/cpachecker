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
package org.sosy_lab.cpachecker.cpa.hybrid.util;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;

/**
 * This class provides CExpression related functionality
 */
public final class ExpressionUtils {

    // utility class only contains static members
    private ExpressionUtils() {}

    /**
     * Calculate the Expression including the truthAssumption
     * @param cfaEdge The respective AssumptionEdge of the cfa
     * @param expression The already casted expression contained withing the edge
     * @return the (possibly inverted Expression), if the Expression provided by the edge is of type CBinaryExpression,
     *         else an empty Optional
     */
    public static CBinaryExpression getASTWithTruthAssumption(AssumeEdge cfaEdge, CBinaryExpression expression) {

        if(!cfaEdge.getTruthAssumption()) {

            // operator inversion is needed
            BinaryOperator newOperator = expression.getOperator().getOppositLogicalOperator();

            expression = new CBinaryExpression(
                expression.getFileLocation(), 
                expression.getExpressionType(), 
                expression.getCalculationType(),
                expression.getOperand1(),
                expression.getOperand2(),
                newOperator);
        }

        return expression;
    }

    /**
     * Inverts the given operator (logical)
     * @param operator The respective operator
     * @return The inverted operator, if a logical operator is given, else null
     */
    @Deprecated
    public static @Nullable BinaryOperator invertOperator(BinaryOperator operator) {

        switch(operator) {
            case LESS_THAN     : return BinaryOperator.GREATER_EQUAL;
            case GREATER_THAN  : return BinaryOperator.LESS_EQUAL;
            case LESS_EQUAL    : return BinaryOperator.GREATER_THAN;
            case GREATER_EQUAL : return BinaryOperator.LESS_THAN;
            case EQUALS        : return BinaryOperator.NOT_EQUALS;
            case NOT_EQUALS    : return BinaryOperator.EQUALS;
            // arithmetic operators cannot be inverted
            default            : return null;
        }
    }

    /**
     * Determines wether a BinaryOperator is a logical operator
     * @param operator The respective operator
     * @return true if the operator is a logical operator, else false
     */
    @Deprecated
    public static boolean isLogicalOperator(BinaryOperator operator) {
        return operator == BinaryOperator.GREATER_EQUAL
            || operator == BinaryOperator.LESS_EQUAL
            || operator == BinaryOperator.GREATER_THAN
            || operator == BinaryOperator.LESS_THAN
            || operator == BinaryOperator.NOT_EQUALS
            || operator == BinaryOperator.EQUALS;
    }
} 