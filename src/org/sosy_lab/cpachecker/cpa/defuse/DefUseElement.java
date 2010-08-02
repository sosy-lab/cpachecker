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
package org.sosy_lab.cpachecker.cpa.defuse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.defuse.DefUseDefinition;
import org.sosy_lab.cpachecker.cpa.defuse.DefUseElement;

public class DefUseElement implements AbstractElement
{
    private List<DefUseDefinition> definitions;

    public DefUseElement (List<DefUseDefinition> definitions)
    {
        this.definitions = definitions;

        if (this.definitions == null)
            this.definitions = new ArrayList<DefUseDefinition> ();
    }

    @Override
    public DefUseElement clone ()
    {
        DefUseElement newElement = new DefUseElement (null);
        for (DefUseDefinition def : definitions)
            newElement.definitions.add (def);

        return newElement;
    }

    public int getNumDefinitions ()
    {
        return definitions.size ();
    }

    public Iterator<DefUseDefinition> getIterator ()
    {
        return definitions.iterator ();
    }

    public DefUseDefinition getDefinition (int index)
    {
        return definitions.get (index);
    }

    public boolean containsDefinition (DefUseDefinition def)
    {
        return definitions.contains (def);
    }

    public void update (DefUseDefinition def)
    {
    	System.out.println("Update: " + def.getVariableName() + " + "+ def.getAssigningEdge().getPredecessor().getNodeNumber());
        String testVarName = def.getVariableName ();
        for (int defIdx = definitions.size () - 1; defIdx >= 0; defIdx--)
        {
            DefUseDefinition otherDef = definitions.get (defIdx);
            if (otherDef.getVariableName ().equals (testVarName))
                definitions.remove (defIdx);
        }

        definitions.add (def);
    }

    @Override
    public boolean equals (Object other)
    {
        if (this == other)
            return true;

        if (!(other instanceof DefUseElement))
            return false;

        DefUseElement otherDefUse = (DefUseElement) other;
        if (otherDefUse.definitions.size () != this.definitions.size ())
            return false;

        for (DefUseDefinition def : definitions)
        {
            if (!otherDefUse.definitions.contains (def))
                return false;
        }

        return true;
    }

    @Override
    public String toString ()
    {
        StringBuilder builder = new StringBuilder ();
        builder.append ('{');

        boolean hasAny = false;
        for (DefUseDefinition def : definitions)
        {
            CFAEdge assigningEdge = def.getAssigningEdge ();
            builder.append ('(').append (def.getVariableName ()).append(", ");

            if (assigningEdge != null)
                builder.append(assigningEdge.getPredecessor ().getNodeNumber ());
            else
                builder.append (0);

            builder.append (", ");

            if (assigningEdge != null)
                builder.append (assigningEdge.getSuccessor ().getNodeNumber ());
            else
                builder.append (0);

            builder.append("), ");
            hasAny = true;
        }

        if (hasAny)
            builder.replace (builder.length () - 2, builder.length (), "}");
        else
            builder.append ('}');

        return builder.toString ();
    }
}
