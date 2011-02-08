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
package org.sosy_lab.cpachecker.cpa.art;

import java.util.LinkedList;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * Path contains a path through the ART that starts at the root node.
 * It is implemented as a list of pairs of an ARTElement and a CFAEdge,
 * where the edge of a pair is the outgoing edge of the element.
 * The first pair contains the root node of the ART.
 */
public class Path extends LinkedList<Pair<ARTElement, CFAEdge>> {

  private static final long serialVersionUID = -3223480082103314555L;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    for (CFAEdge edge : asEdgesList()) {
      sb.append("Line ");
      sb.append(edge.getLineNumber());
      sb.append(": ");
      sb.append(edge);
      sb.append("\n");
    }

    return sb.toString();
  }

  @SuppressWarnings("unchecked")
  public JSONArray toJSON() {
    JSONArray path = new JSONArray();
    for (Pair<ARTElement, CFAEdge> pair : this) {
      JSONObject elem = new JSONObject();
      ARTElement artelem = pair.getFirst();
      CFAEdge edge = pair.getSecond();
      elem.put("artelem", artelem.getElementId());
      elem.put("source", edge.getPredecessor().getNodeNumber());
      elem.put("target", edge.getSuccessor().getNodeNumber());
      elem.put("desc", edge.getRawStatement().replaceAll("\n", " "));
      elem.put("line", edge.getLineNumber());
      path.add(elem);
    }
    return path;
  }

  public List<CFAEdge> asEdgesList() {
    Function<Pair<?, ? extends CFAEdge>, CFAEdge> projectionToSecond = Pair.getProjectionToSecond();
    return Lists.transform(this, projectionToSecond);
  }
  
  /**
   * This method returns the path as C source code, intended to be used with CBMC.
   *
   * @return the path as C source code
   */
  public String toSourceCode()
  {
    StringBuilder sb = new StringBuilder();

    CFAEdgeType currentEdgeType;

    sb.append("int main()");
    sb.append("\n");
    sb.append("{");
    sb.append("\n");

    for(CFAEdge currentEdge : asEdgesList())
    {
      currentEdgeType = currentEdge.getEdgeType();

      switch (currentEdgeType)
      {
        case DeclarationEdge:
        case StatementEdge:
        case ReturnStatementEdge:
          sb.append(currentEdge.getRawStatement());
          sb.append("\n");

          break;

        case AssumeEdge:
          AssumeEdge assumeEdge = (AssumeEdge)currentEdge;
          sb.append("__CPROVER_assume(");

          if(assumeEdge.getTruthAssumption())
            sb.append(assumeEdge.getExpression().getRawSignature());

          else
          {
            sb.append("!(");
            sb.append(assumeEdge.getExpression().getRawSignature());
            sb.append(")");
          }

          sb.append(");");
          sb.append("\n");

          break;

        case FunctionCallEdge:
          FunctionCallEdge functionCallEdge = (FunctionCallEdge)currentEdge;

          List<IASTExpression> actualParams = functionCallEdge.getArguments();

          List<String> formalParameters = functionCallEdge.getSuccessor().getFunctionParameterNames();

          // define and declare a variable for each actual parameter of the called function that corresponds to its respective formal parameter
          for(int i = 0; i < formalParameters.size(); i++)
          {
            sb.append("int " + formalParameters.get(i) + ";\n");
            sb.append(formalParameters.get(i));
            sb.append(" = ");
            sb.append(actualParams.get(i).getRawSignature());
            sb.append(";\n");
          }

          break;

        case BlankEdge:
          if(currentEdge.isJumpEdge())
          {
            String statement = currentEdge.getRawStatement();
            if(isGotoErrorStateLabel(statement))
            {
              sb.append("goto ERROR;");
              sb.append("\n");

              sb.append("ERROR:");
              sb.append("\n");
            }
          }

          break;
      }
    }

    sb.append("}");

    return sb.toString();
  }

  private boolean isGotoErrorStateLabel(String statement)
  {
    return statement.contains("Goto: ERROR");
  }
}
