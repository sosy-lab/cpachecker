/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cfa.objectmodel.c;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTExpression;

import cfa.objectmodel.AbstractCFAEdge;
import cfa.objectmodel.CFAEdgeType;


public class MultiStatementEdge extends AbstractCFAEdge
{
    private List<IASTExpression> expressions;
    private boolean jumpEdge;

    public MultiStatementEdge (String rawStatement,
                              List<IASTExpression> expressions)
    {
        super (rawStatement);
        this.jumpEdge = false;

        if (expressions == null)
            this.expressions = new ArrayList<IASTExpression> ();
        else
            this.expressions = expressions;
    }

    public void setIsJumpEdge (boolean jumpEdge)
    {
        this.jumpEdge = jumpEdge;
    }

    @Override
    public boolean isJumpEdge ()
    {
        return jumpEdge;
    }

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
