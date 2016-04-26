/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.bnbmemorymodel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class ComplexTypeFieldStatistics {

  private HashMap<CType, HashMap<CType, HashSet<String>>> usedFields = new HashMap<>();
  private HashMap<CType, HashMap<CType, HashSet<String>>> refdFields = new HashMap<>();
  private final BnBStatementVisitor statementVisitor = new BnBStatementVisitor();
  private final BnBExpressionVisitor expressionVisitor = new BnBExpressionVisitor();
  private final BnBMapMerger merger = new BnBMapMerger();
  private final LogManager logger;

  public ComplexTypeFieldStatistics(LogManager logger) {
    this.logger = logger;
  }

  public void findFieldsInCFA(CFA cfa) throws BnBException {
    for (CFANode node : cfa.getAllNodes()){
      for (int i = 0; i < node.getNumEnteringEdges(); ++i){
        visitEdge(node.getEnteringEdge(i));
      }
    }
  }

  //Searching for address-taking and calling of the structure field
  private void visitEdge(CFAEdge edge) throws BnBException {
    CFAEdgeType edgeType;
    edgeType = edge.getEdgeType();
    Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> result;

    try {
      switch (edgeType){
        case StatementEdge:
          result = (((CStatementEdge) edge).getStatement()).accept(statementVisitor);

          if (result != null) {
            usedFields = merger.mergeMaps(usedFields, result.get(false));
            refdFields = merger.mergeMaps(refdFields, result.get(true));
          }
          break;

        case FunctionCallEdge:
          for (CExpression param : ((CFunctionCallEdge) edge).getArguments()) {
              result = param.accept(expressionVisitor);

              if (result != null) {
                usedFields = merger.mergeMaps(usedFields, result.get(false));
                refdFields = merger.mergeMaps(refdFields, result.get(true));
              }
          }
          break;

        case DeclarationEdge:
          CDeclaration decl = ((CDeclarationEdge) edge).getDeclaration();
          if (decl instanceof CVariableDeclaration) {
            CInitializer init = ((CVariableDeclaration) decl).getInitializer();
            if (init != null && init instanceof CInitializerExpression) {
                result = ((CInitializerExpression) init).getExpression().accept(expressionVisitor);

                if (result != null) {
                  usedFields = merger.mergeMaps(usedFields, result.get(false));
                  refdFields = merger.mergeMaps(refdFields, result.get(true));
                }
            }
          }
          break;
      }
    } catch (BnBException e) {
      logger.logException(Level.WARNING, e, "Exception while gathering information about struct type field usage");
      throw e;
    }
  }

  public void dumpStat(String filename){
    File dump = new File(filename);

    try {
      FileWriter writer = new FileWriter(dump);

      Map<CType, HashSet<String>> sub;
      String output = "";
      String sub_output;
      int used;

      output += "USED_FIELDS:\n";
      for (CType type : usedFields.keySet()){
        sub = usedFields.get(type);
        used = 0;
        sub_output = "";
        for (CType struct_name : sub.keySet()){
          sub_output += "\t\tSTRUCT: " + struct_name + '\n';
          used += sub.get(struct_name).size();
          for (String fieldName : sub.get(struct_name)){
            sub_output += "\t\t\tFIELD: " + fieldName + '\n';
          }
        }
        output += "\tFIELD_TYPE: " + type + "\n\tTIMES USED: " + used + '\n' + sub_output;
      }

      output += "\nREFERENCED_FIELDS:\n";
      for (CType type : refdFields.keySet()){
        sub = refdFields.get(type);
        used = 0;
        sub_output = "";
        for (CType struct_name : sub.keySet()){
          sub_output += "\t\tSTRUCT: " + struct_name + '\n';
          used += sub.get(struct_name).size();
          for (String fieldName : sub.get(struct_name)){
            sub_output += "\t\t\tFIELD: " + fieldName + '\n';
          }
        }
        output += "\tFIELD_TYPE: " + type + "\n\tTIMES USED: " + used + '\n' + sub_output;
      }

      writer.write(output);
      writer.close();

    } catch (IOException e) {
      logger.logException(Level.WARNING, e, "Exception while writing field usage information");
    }

  }

  public Map<CType, HashMap<CType, HashSet<String>>> getUsedFields() {
    return usedFields;
  }

  public Map<CType, HashMap<CType, HashSet<String>>> getRefdFields() {
    return refdFields;
  }
}