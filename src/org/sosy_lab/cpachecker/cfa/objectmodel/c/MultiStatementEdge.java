/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.objectmodel.c;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;

import org.sosy_lab.cpachecker.cfa.objectmodel.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;


public class MultiStatementEdge extends AbstractCFAEdge
{
    private final List<IASTExpression> expressions;

    public MultiStatementEdge (String rawStatement, int lineNumber, CFANode predecessor, CFANode successor,
                              List<IASTExpression> expressions)
    {
        super(rawStatement, lineNumber, predecessor, successor);

        if (expressions == null)
            this.expressions = new ArrayList<IASTExpression> ();
        else
            this.expressions = expressions;
    }

    @Override
    public CFAEdgeType getEdgeType ()
    {
        return CFAEdgeType.MultiStatementEdge;
    }

    public List<IASTExpression> getExpressions ()
    {
        return expressions;
    }

    @Override
    public String getRawStatement ()
    {
        StringBuilder builder = new StringBuilder ();

        for (IASTExpression expr : expressions)
        {
            builder.append (expr.getRawSignature ()).append ("\\n");
        }

        return builder.toString ();
    }
}
