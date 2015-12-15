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

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.PThreadUtils;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

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

    return new CFunctionDeclaration(FileLocation.DUMMY, new CFunctionTypeWithNames(false, false,
        returnType, params, false), functionName, params);
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


}
