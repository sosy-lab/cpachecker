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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.interval.NumberInterface;

public enum SIGN implements NumberInterface, Serializable {
    // ALL = 111, PLUS = 100, MINUS = 010, ...
    EMPTY(0), PLUS(1), MINUS(2), ZERO(4), PLUSMINUS(3), PLUS0(5), MINUS0(6), ALL(7);

    private final int numVal;

    private static final ImmutableMap<Integer, SIGN> VALUE_MAP;

    static {
        Builder<Integer, SIGN> builder = ImmutableMap.builder();
        for (SIGN s : SIGN.values()) {
            builder.put(s.numVal, s);
        }
        VALUE_MAP = builder.build();
    }

    private SIGN(int numVal) {
        this.numVal = numVal;
    }

    public boolean isAll() {
        return this == ALL;
    }

    @Override
    public boolean isEmpty() {
        return this == EMPTY;
    }

    @Override
    public NumberInterface combineWith(NumberInterface sign) {
        SIGN tempSign = (SIGN) sign;
        // combine bit values
        return VALUE_MAP.get(Integer.valueOf(tempSign.numVal | numVal));
    }

    public boolean covers(SIGN sign) {
        if ((sign.numVal | this.numVal) == this.numVal) {
            return true;
        }
        return false;
    }

    public boolean intersects(SIGN sign) {
        if ((sign.numVal & this.numVal) != 0) {
            return true;
        }
        return false;
    }

    public static SIGN min(SIGN sign0, SIGN sign1) {
        if (sign0.isSubsetOf(sign1)) {
            return sign0;
        }
        return sign1;
    }
    @Override
    public boolean isSubsetOf(NumberInterface sign) {
        SIGN signTemp = (SIGN) sign;
        if (signTemp.isAll()) {
            return true;
        }
        // Check if this is a subset using atomic signs
        return sign.split().containsAll(this.split());
    }

    @Override
    public ImmutableSet<NumberInterface> split() { // TODO performance
        ImmutableSet.Builder<NumberInterface> builder = ImmutableSet.builder();
        for (SIGN s : ImmutableList.of(PLUS, MINUS, ZERO)) {
            if ((s.numVal & numVal) > 0) {
                builder.add(s);
            }
        }
        return builder.build();
    }

    @Override
    public Number getNumber() {
        return numVal;
    }

    @Override
    public NumberInterface evaluateNonCommutativePlusOperator(NumberInterface pRight) {
        SIGN pLeft = this;
        SIGN pRightTemp = (SIGN) pRight;
        if (pRightTemp == SIGN.ZERO) {
            return pLeft;
        }
        if (pLeft == SIGN.PLUS && pRightTemp == SIGN.MINUS) {
            return SIGN.ALL;
        }
        if (pLeft == SIGN.MINUS && pRightTemp == SIGN.MINUS) {
            return SIGN.MINUS;
        }
        if (pLeft == SIGN.PLUS && pRightTemp == SIGN.PLUS) {
            return SIGN.PLUS;
        }
        return SIGN.EMPTY;
    }

    @Override
    public NumberInterface evaluateMulOperator(NumberInterface pRight) {
        SIGN pLeft = this;
        NumberInterface leftToRightResult = evaluateNonCommutativeMulOperator(pRight);
        NumberInterface rightToLeftResult = evaluateNonCommutativeMulOperator(pLeft);
        return leftToRightResult.combineWith(rightToLeftResult);
    }

    @Override
    public NumberInterface evaluateNonCommutativeMulOperator(NumberInterface right) {
        SIGN left = this;
        SIGN rightTemp = (SIGN) right;
        if (right == SIGN.ZERO) {
            return SIGN.ZERO;
        }
        if (left == SIGN.PLUS && rightTemp == SIGN.MINUS) {
            return SIGN.MINUS;
        }
        if ((left == SIGN.PLUS && rightTemp == SIGN.PLUS) || (left == SIGN.MINUS && rightTemp == SIGN.MINUS)) {
            return SIGN.PLUS;
        }
        return SIGN.EMPTY;
    }

    @Override
    public NumberInterface evaluateDivideOperator(NumberInterface right) {
        SIGN rightTemp = (SIGN) right;
        // if (rightTemp == SIGN.ZERO) {
        // transferRel.logger.log(Level.WARNING, "Possibly dividing by zero",
        // edgeOfExpr);
        // return SIGN.ALL;
        // }
        return evaluateMulOperator(rightTemp);
    }

    @Override
    public NumberInterface evaluateModuloOperator(NumberInterface pRight) {
        SIGN pLeft = this;
        SIGN pRightTemp = (SIGN) pRight;
        if (pLeft == SIGN.ZERO) {
            return SIGN.ZERO;
        }
        if (pLeft == SIGN.PLUS && (pRightTemp == SIGN.PLUS || pRightTemp == SIGN.MINUS)) {
            return SIGN.PLUS0;
        }
        if (pLeft == SIGN.MINUS && (pRightTemp == SIGN.MINUS || pRightTemp == SIGN.PLUS)) {
            return SIGN.MINUS0;
        }
        return SIGN.ALL;
    }

    // assumes that indicator bit for negative numbers is 1
    @Override
    public NumberInterface evaluateAndOperator(NumberInterface right) {
        SIGN left = this;
        SIGN rightTemp = (SIGN) right;
        if (left == SIGN.ZERO || rightTemp == SIGN.ZERO) {
            return SIGN.ZERO;
        }
        if (left == SIGN.PLUS || rightTemp == SIGN.PLUS) {
            return SIGN.PLUS0;
        }
        if (left == SIGN.MINUS && rightTemp == SIGN.MINUS) {
            return SIGN.MINUS0;
        }
        return SIGN.EMPTY;
    }

    @Override
    public NumberInterface evaluateLessOperator(NumberInterface pRight) {
        SIGN pLeft = this;
        SIGN pRightTemp = (SIGN) pRight;
        if (pLeft == SIGN.EMPTY || pRightTemp == SIGN.EMPTY) {
            return SIGN.EMPTY;
        }
        switch (pLeft) {
        case PLUS:
            if (SIGN.MINUS0.covers(pRightTemp)) {
                return SIGN.ZERO;
            }
            break;
        case MINUS:
            if (SIGN.PLUS0.covers(pRightTemp)) {
                return SIGN.ZERO;
            }
            break;
        case ZERO:
            if (SIGN.MINUS0.covers(pRightTemp)) {
                return SIGN.ZERO;
            }
            if (pRightTemp == SIGN.ZERO) {
                return SIGN.PLUSMINUS;
            }
            break;
        case PLUS0:
            if (pRightTemp == SIGN.MINUS) {
                return SIGN.ZERO;
            }
            if (pRightTemp == SIGN.ZERO) {
                return SIGN.PLUSMINUS;
            }
            break;
        case MINUS0:
            if (pRightTemp == SIGN.PLUS) {
                return SIGN.PLUSMINUS;
            }
            break;
        default:
            break;
        }
        return SIGN.ALL;
    }

    @Override
    public NumberInterface evaluateLessEqualOperator(NumberInterface pRight) {
        SIGN pLeft = this;
        SIGN pRightTemp = (SIGN) pRight;
        if (pLeft == SIGN.EMPTY || pRightTemp == SIGN.EMPTY) {
            return SIGN.EMPTY;
        }
        switch (pLeft) {
        case PLUS:
            if (SIGN.MINUS0.covers(pRightTemp)) {
                return SIGN.ZERO;
            }
            break;
        case MINUS:
            if (SIGN.PLUS0.covers(pRightTemp)) {
                return SIGN.ZERO;
            }
            break;
        case ZERO:
            if (SIGN.PLUS0.covers(pRightTemp)) {
                return SIGN.PLUSMINUS;
            }
            if (pRight == SIGN.MINUS) {
                return SIGN.ZERO;
            }
            break;
        case PLUS0:
            if (pRight == SIGN.MINUS) {
                return SIGN.ZERO;
            }
            break;
        case MINUS0:
            if (pRight == SIGN.PLUS) {
                return SIGN.PLUSMINUS;
            }
            break;
        default:
            break;
        }
        return SIGN.ALL;
    }

    @Override
    public NumberInterface evaluateEqualOperator(NumberInterface pRight) {
        SIGN pLeft = this;
        SIGN pRightTemp = (SIGN) pRight;
        if (pLeft == SIGN.EMPTY || pRightTemp == SIGN.EMPTY) {
            return SIGN.EMPTY;
        }
        if (pLeft == SIGN.ZERO && pRightTemp == SIGN.ZERO) {
            return SIGN.PLUSMINUS;
        }
        return SIGN.ALL;
    }

    @Override
    public NumberInterface plus(NumberInterface pInterval) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface minus(NumberInterface pOther) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface times(NumberInterface pOther) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface divide(NumberInterface pOther) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface shiftLeft(NumberInterface pOffset) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface shiftRight(NumberInterface pOffset) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface unsignedDivide(NumberInterface pOther) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface unsignedModulo(NumberInterface pOther) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface unsignedShiftRight(NumberInterface pOther) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Long asLong(CType pType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface binaryAnd(NumberInterface pRNum) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface binaryOr(NumberInterface pRNum) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface binaryXor(NumberInterface pRNum) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean covers(NumberInterface pSign) {
        // TODO Auto-generated method stub
        return false;
    }
}