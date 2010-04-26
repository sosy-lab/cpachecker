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
package org.sosy_lab.cpachecker.cfa.objectmodel.c;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;

import org.sosy_lab.cpachecker.cfa.objectmodel.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;


public class MultiDeclarationEdge extends AbstractCFAEdge
{
    private final List<IASTDeclarator[]> declarators;
    private final List<String> rawStatements;

    public MultiDeclarationEdge (String rawStatement, int lineNumber, CFANode predecessor, CFANode successor,
                              List<IASTDeclarator[]> declarators,
                              List<String> rawStatements)
    {
        super(rawStatement, lineNumber, predecessor, successor);

        if (declarators == null)
            this.declarators = new ArrayList<IASTDeclarator[]> ();
        else
            this.declarators = declarators;

        if (rawStatements == null)
            this.rawStatements = new ArrayList<String> ();
        else
            this.rawStatements = rawStatements;
    }

    @Override
    public CFAEdgeType getEdgeType ()
    {
        return CFAEdgeType.MultiDeclarationEdge;
    }

    public List<IASTDeclarator[]> getDeclarators ()
    {
        return declarators;
    }

    public List<String> getRawStatements ()
    {
        return rawStatements;
    }

    @Override
    public String getRawStatement ()
    {
        StringBuilder builder = new StringBuilder ();

        for (String sig : rawStatements)
        {
            builder.append (sig).append ("\\n");
        }

        return builder.toString ();
    }
}
