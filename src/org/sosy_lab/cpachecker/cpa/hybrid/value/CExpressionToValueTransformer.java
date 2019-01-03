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
package org.sosy_lab.cpachecker.cpa.hybrid.value;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cpa.hybrid.util.ExpressionUtils;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

import edu.umd.cs.findbugs.annotations.Nullable;

public class CExpressionToValueTransformer {

    @Nullable
    public static Value transform(CExpression pExpression) {

        if(pExpression instanceof CIntegerLiteralExpression) {
            return transformIntegerExpression((CIntegerLiteralExpression)pExpression);
        } 
        if(pExpression instanceof CFloatLiteralExpression) {
            return transformFloatExpression((CFloatLiteralExpression)pExpression);
        }
        if(pExpression instanceof CStringLiteralExpression) {
            return transformStringExpression((CStringLiteralExpression)pExpression);
        }
        if(pExpression instanceof CCharLiteralExpression) {
            CCharLiteralExpression charLiteral = (CCharLiteralExpression) pExpression;
            return transformIntegerExpression(ExpressionUtils.charToIntLiteral(charLiteral));
        }

        //TODO check for further expressions to cover

        return null;
    }

    private static Value transformStringExpression(CStringLiteralExpression pExpression) {
        return new StringValue(pExpression.getValue());
    }

    private static Value transformFloatExpression(CFloatLiteralExpression pExpression) {
        return new NumericValue(pExpression.getValue());
    }

    private static Value transformIntegerExpression(CIntegerLiteralExpression pExpression) {
        return new NumericValue(pExpression.getValue());
    }
}