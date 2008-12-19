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

import java.util.List;
import java.util.Vector;

import cfa.objectmodel.AbstractCFAEdge;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;


public class BlockEdge extends AbstractCFAEdge {

    private List<CFAEdge> edges;

    public BlockEdge() {
        super("BLOCK");
        edges = new Vector<CFAEdge>();
    }

    public void addEdge(CFAEdge e) {
        edges.add(e);
    }

    public List<CFAEdge> getEdges() { return edges; }

    @Override
    public CFAEdgeType getEdgeType() {
        // TODO db: This does not make sense.
        //      This edge type should be called BlockEdge or so.
        // TODO Auto-generated method stub
        return CFAEdgeType.BlankEdge;
    }

    @Override
    public String getRawStatement() {
        StringBuffer buf = new StringBuffer();
        buf.append("BLOCK{");
        for (CFAEdge e : edges) {
            buf.append(e.getRawStatement());
            buf.append(" ");
        }
        buf.delete(buf.length()-1, buf.length());
        buf.append("}");
        return buf.toString();
    }
}
