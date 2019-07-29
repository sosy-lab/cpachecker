/*

 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.ifcsecurity.combetoutputgenerator;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.combetoutputgenerator.lib.values.JsonArray;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.combetoutputgenerator.lib.values.JsonObj;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.combetoutputgenerator.lib.values.StringValue;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Pair;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;

public class FileInfoXML {
    String filefolder;
    String fileName;
    SortedSet<Pair<Variable, String>> variables;
    SortedSet<Pair<Variable, Pair<List<String>, String>>> methods;

    public FileInfoXML(String pFilefolder,String pFileName) {
      this.filefolder=pFilefolder;
      this.fileName=pFileName;
      this.variables=new TreeSet<>();
      this.methods=new TreeSet<>();
    }

    public void addVariable(Variable pVariable,String type){
    Pair<Variable, String> pair = new Pair<>(pVariable, type);
      if(!variables.contains(pair)){
        variables.add(pair);
      }
    }

    public void addMethod(Variable pVariable,List<String> pParams, String type){
      Pair<Variable, Pair<List<String>, String>> pair = new Pair<>(pVariable, new Pair<>(pParams,type));
      if(!methods.contains(pair)){
        methods.add(pair);
      }
    }

//    "FileInfo":
//    {
//      "FileName": "PATBC\llvm_patbc_results\llvm_patbc_results\example_project\real_example.c",
//      "Variables":["global","main","main::i","main::j","main::k","main::i","main::a","main::b","main::memory","main::y","main::y::a","main::y::b"],
//      "Methods":[["main","void->int"]]
//    }

    public String printFileInfo(int tabs){
      int curtabs=tabs;
      StringBuffer bf=new StringBuffer();
      //FileFolder
      tabbing(bf,curtabs);
      bf.append("File-Info: ");
      newLine(bf);

      bf.append("{");
      newLine(bf);
      curtabs++;
      //FileFolder
      bf.append(printFileFolderName(curtabs));
      bf.append(",");
      newLine(bf);
      //FileName
      bf.append(printFileName(curtabs));
      bf.append(",");
      newLine(bf);
      //Variables
      bf.append(printVariables(curtabs));
      bf.append(",");
      newLine(bf);
      //Methods

      newLine(bf);

      bf.append("}");
      newLine(bf);
      return bf.toString();
    }

    public String printFileFolderName(int tabs){
      int curtabs=tabs;
      StringBuffer bf=new StringBuffer();
      //FileFolder
      tabbing(bf,curtabs);
      bf.append("File-Folder: ");
      jsonString(bf,filefolder);
      newLine(bf);
      return bf.toString();
    }

    public String printFileName(int tabs){
      int curtabs=tabs;
      StringBuffer bf=new StringBuffer();
      //FileName
      tabbing(bf,curtabs);
      bf.append("File-Name: ");
      jsonString(bf,fileName);
      return bf.toString();
    }

    public String printVariables(int tabs){
      int curtabs=tabs;
      StringBuffer bf=new StringBuffer();
      //Variables
      tabbing(bf,curtabs);
      bf.append("Variables: ");
      bf.append("[");
      for(Pair<Variable, String> variable:variables){
        bf.append("[");
        jsonString(bf,variable.getFirst().toString());
        bf.append(",");
        jsonString(bf,variable.getSecond().toString());
        bf.append("]");
        bf.append(",");
      }
      bf.append("]");
      return bf.toString();
    }

    public String printMethods(int tabs){
      int curtabs=tabs;
      StringBuffer bf=new StringBuffer();
      //Variables
      tabbing(bf,curtabs);
      bf.append("Methods: ");
      bf.append("[");
      for(Pair<Variable, Pair<List<String>, String>> variable:methods){
        bf.append("[");
        jsonString(bf,variable.getFirst().toString());
        bf.append(",");
        bf.append("[");
        for(String s: variable.getSecond().getFirst()){
          jsonString(bf,s);
          bf.append(",");
        }
        bf.append("]");
        bf.append(",");
        jsonString(bf,variable.getSecond().getSecond().toString());
        bf.append("]");
        bf.append(",");
      }
      bf.append("]");
      return bf.toString();
    }

    private void tabbing(StringBuffer bf,int tabs){
      for(int i=0;i<tabs;i++){
        bf.append("\t");
      }
    }

    private void jsonString(StringBuffer bf,String text){
      bf.append("\""+text+"\"");
    }

    private void newLine(StringBuffer bf){
      bf.append("\n");
    }
//  "FileInfo":
//  {
//    "FileName": "PATBC\llvm_patbc_results\llvm_patbc_results\example_project\real_example.c",
//    "Variables":["global","main","main::i","main::j","main::k","main::i","main::a","main::b","main::memory","main::y","main::y::a","main::y::b"],
//    "Methods":[["main","void->int"]]
//  }
    public static void main(String args[]){
      JsonObj fileinfo=new JsonObj();
      JsonObj fileinfoR=new JsonObj();
      //FileName
      fileinfoR.add(new StringValue("File-Name"), new StringValue("PATBC\\llvm_patbc_results\\llvm_patbc_results\\example_project\\real_example.c"));
      //Variables
      JsonArray variablesR=new JsonArray();
      String[] variables ={"global","main","main::i","main::j","main::k","main::i","main::a","main::b","main::memory","main::y","main::y::a","main::y::b"};
      for(int i=0;i<variables.length;i++){
        variablesR.add(new StringValue(variables[i]));
      }
      fileinfoR.add(new StringValue("Variables"),variablesR);
      //Methods
      JsonArray methodsR=new JsonArray();
      String[] methods={"main"};
      for(int i=0;i<methods.length;i++){
        methodsR.add(new StringValue(methods[i]));
      }
      fileinfoR.add(new StringValue("Methods"),methodsR);
      fileinfo.add(new StringValue("FileInfo"),fileinfoR);
    }

}
