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
package org.sosy_lab.cpachecker.cpa.hybrid.value;

import com.google.errorprone.annotations.Immutable;

import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.ValueVisitor;

/**
 * This class represents a hybrid value 
 * that will never change over the complete program flow
 */
@Immutable
public final class ConstantValue extends HybridValue
{

    private static final long serialVersionUID = 1L;

    public ConstantValue(Value value)
    {
        super(value);
    }

    @Override
    public boolean isUnknown()
    {
        return false;
    }

    @Override
    public boolean isExplicitlyKnown() 
    {
        return true;
    }

    @Override
    public <T> T accept(ValueVisitor<T> pVisitor) {
        return pVisitor.visit(this);
    }

}