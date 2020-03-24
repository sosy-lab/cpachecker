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
package org.sosy_lab.cpachecker.core.algorithm.tiger.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CParser.FileToParse;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;


public class WrapperUtil {

  public static final String CPAtiger_MAIN = "__CPAtiger__main";
  public static final String CPAtiger_INPUT = "input";

  @SuppressWarnings("null")
  public static FileToParse getWrapperCFunction(CFunctionEntryNode pMainFunction, LogManager logger)
      throws IOException {

    StringWriter lWrapperFunction = new StringWriter();
    PrintWriter lWriter = new PrintWriter(lWrapperFunction);

    // TODO interpreter is not capable of handling initialization of global declarations
    if (pMainFunction == null) {
      logger.log(Level.SEVERE, "No Entry function given");
      throw new NullPointerException("No Entry function given");
    }

    lWriter.println(pMainFunction.getFunctionDefinition().toASTString());
    lWriter.println();
    lWriter.println("extern int __VERIFIER_nondet_int();");
    lWriter.println();
    lWriter.println("int " +  CPAtiger_INPUT + "() {");
    lWriter.println("  return __VERIFIER_nondet_int();");
    lWriter.println("}");
    lWriter.println();
    lWriter.println("void " + CPAtiger_MAIN + "()");
    lWriter.println("{");

    for (CParameterDeclaration lDeclaration : pMainFunction.getFunctionParameters()) {
      lWriter.println("  " + lDeclaration.toASTString() + ";");
    }

    for (CParameterDeclaration lDeclaration : pMainFunction.getFunctionParameters()) {
      // TODO do we need to handle lDeclaration more specifically?
      lWriter.println("  " + lDeclaration.getName() + " = " +  CPAtiger_INPUT + "();");
    }

    lWriter.println();
    lWriter.print("  " + pMainFunction.getFunctionName() + "(");

    boolean isFirst = true;

    for (CParameterDeclaration lDeclaration : pMainFunction.getFunctionParameters()) {
      if (isFirst) {
        isFirst = false;
      }
      else {
        lWriter.print(", ");
      }

      lWriter.print(lDeclaration.getName());
    }

    lWriter.println(");");
    lWriter.println("  return;");
    lWriter.println("}");
    lWriter.println();

    File f = File.createTempFile(CPAtiger_MAIN, ".c", null);
    f.deleteOnExit();

    @SuppressWarnings("resource")
    Writer writer = null;

    try {
        writer = new BufferedWriter(new OutputStreamWriter(
              new FileOutputStream(f), "utf-8"));
        writer.write(lWrapperFunction.toString());
    } catch (IOException ex) {
      throw ex;
    } finally {
      try {
        writer.close();
      } catch (Exception ex) {
        throw ex;
      }
    }

    //return new FileToParse(f.getAbsolutePath(), CPAtiger_MAIN + "__");
    return new FileToParse(f.getAbsolutePath());
  }

}
