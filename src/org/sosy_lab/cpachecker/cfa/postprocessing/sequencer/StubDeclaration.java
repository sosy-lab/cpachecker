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
package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer;

import static org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFAFunctionUtils.*;
import static org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.PThreadUtils.*;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFAEdgeUtils;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFASequenceBuilder;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.PThreadUtils;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

/**
 * This class handles and builds the declaration for the posix stubs.
 */
/*
 * The functionality of this class is not added to the POSIXStub class, because
 * the stub declaration are needed before the traversal. For the creation of the
 * stub bodies additional information is necessary which is only available after
 * the traversal. (e.g. controlVariables, threads)
 */
public class StubDeclaration {



  public final static CTypedefType THREAD_TYPE = new CTypedefType(false, false, "pthread_t", CNumericTypes.UNSIGNED_LONG_INT);

  private final CFunctionDeclaration pthreadCreateStubDeclaration = buildPthreadStubDeclaration();
  private final CFunctionDeclaration pthreadJoinDeclaration = buildPthreadJoinDeclaration();
  private CFunctionDeclaration pthreadMutexInitDeclaration = buildPthreadMutexInitDeclaraion();

  private LogManager logger;

  public StubDeclaration(LogManager logger) {
    this.logger = logger;
  }

  private CFunctionDeclaration buildPthreadMutexInitDeclaraion() {
    String parameterName = "__mutex"; //TODO check param name

    CParameterDeclaration firstParam = new CParameterDeclaration(FileLocation.DUMMY,
        THREAD_TYPE, parameterName);

    return buildFunctionWithParameter(PThreadUtils.PTHREAD_JOIN_NAME, CNumericTypes.INT, firstParam);
  }

  private static CFunctionDeclaration buildFunctionWithParameter(String functionName,
      CType returnType,
      CParameterDeclaration... param) {
    List<CParameterDeclaration> params = new ArrayList<>();
    for (CParameterDeclaration parameter : param) {
      params.add(parameter);
      parameter.setQualifiedName(functionName + "::" + parameter.getName());
    }

    CFunctionTypeWithNames functionType = new CFunctionTypeWithNames(false, false,
        returnType, params, false);
    functionType.setName(functionName);

    return new CFunctionDeclaration(FileLocation.DUMMY, functionType, functionName, params);
  }

  private static CFunctionDeclaration buildPthreadStubDeclaration() {
    final String parameterName1 = "__newthread";
    final String parameterName2 = "__arg";
    final String parameterName3 = "__threadNumber";

    CType threadTypePointer = new CPointerType(false, false, THREAD_TYPE);

    CParameterDeclaration firstParam =
        new CParameterDeclaration(FileLocation.DUMMY, threadTypePointer, parameterName1);
    CParameterDeclaration secondParam =
        new CParameterDeclaration(FileLocation.DUMMY,
            new CPointerType(false, false, CVoidType.VOID), parameterName2);
    CParameterDeclaration thirdParam =
        new CParameterDeclaration(FileLocation.DUMMY, CNumericTypes.LONG_INT, parameterName3);

    return buildFunctionWithParameter(PThreadUtils.PTHREAD_CREATE_NAME, CNumericTypes.INT,
        firstParam, secondParam, thirdParam);
  }

  private static CFunctionDeclaration buildPthreadJoinDeclaration() {
    String parameterName = "__th";
    String parameterName2 = "__thread_return";

    CParameterDeclaration firstParam = new CParameterDeclaration(FileLocation.DUMMY,
        THREAD_TYPE, parameterName);
    CParameterDeclaration secondParam = new CParameterDeclaration(FileLocation.DUMMY,
        new CPointerType(false, false, CVoidType.VOID), parameterName2);

    return buildFunctionWithParameter(PThreadUtils.PTHREAD_JOIN_NAME, CNumericTypes.INT,
        firstParam, secondParam);
  }

  public CFunctionDeclaration getPthreadCreateStubDeclaration() {
    return pthreadCreateStubDeclaration;
  }

  public CFunctionDeclaration getPthreadJoinDeclaration() {
    return pthreadJoinDeclaration;
  }

  public CFunctionDeclaration getPthreadMutexInitDeclaration() {
    return pthreadMutexInitDeclaration;
  }

  public void replaceDeclarationEdges(MutableCFA pCfa) {
    CFAVisitor declarationRepl = new DeclarationVisitor();
      CFATraversal.dfs().traverseOnce(pCfa.getMainFunction(), declarationRepl);
  }

  public class DeclarationVisitor extends CFATraversal.DefaultCFAVisitor {

    @Override
    public TraversalProcess visitEdge(CFAEdge pEdge) {
      assert isGlobalVar(pEdge) : "The declaration repacement is not done in the global var";

      if (IS_FUNCTION_START_EDGE.apply(pEdge)) {
        return TraversalProcess.ABORT;
      } else if (pEdge instanceof CDeclarationEdge) {
        repaceDeclaration((CDeclarationEdge) pEdge);
      }
      return TraversalProcess.CONTINUE;

    }

    private void repaceDeclaration(CDeclarationEdge pEdge) {
      CDeclaration declaration = pEdge.getDeclaration();
      if (declaration instanceof CFunctionDeclaration) {
        CFunctionDeclaration functionDec = (CFunctionDeclaration) declaration;
        CDeclarationEdge newDeclaration = null;
        switch (functionDec.getName()) {
          case PTHREAD_CREATE_NAME:
            newDeclaration = getDummyDeclarationEdge(pthreadCreateStubDeclaration);
            break;
          case PTHREAD_JOIN_NAME:
            newDeclaration = getDummyDeclarationEdge(pthreadJoinDeclaration);
            break;
          case PTHREAD_MUTEX_INIT_NAME:
            newDeclaration = getDummyDeclarationEdge(pthreadMutexInitDeclaration);
            break;
          case PTHREAD_MUTEX_LOCK_NAME:
          case PTHREAD_MUTEX_UNLOCK_NAME:
            // TODO impelment
            break;
          default:
            break;

        }
        if (newDeclaration != null) {
          CFAEdgeUtils.replaceCEdgeWith(pEdge, newDeclaration);
        }
      }
    }

  }

  private CDeclarationEdge getDummyDeclarationEdge(CDeclaration declaration) {
    return new CDeclarationEdge("", FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE,
        CFASequenceBuilder.DUMMY_NODE, declaration);
  }

  // assertion only
  private boolean globalVar = false;
  private boolean isGlobalVar(CFAEdge pEdge) {
    if(IS_INIT_GLOBAL_VARS.apply(pEdge)) {
      globalVar = true;
    }
    assert globalVar : "The declaration of global variables doesn't start at beginning";
    return true;
  }


}
