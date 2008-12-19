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
package common;

/*
 * NOTE: This class is not used presently, and is here as part of an un-submitted experiment
 *  to make the CPA algorithm completely language independent.  However, in the end I decided it
 *  better to save effort now, and to only create my object model if it is ever desired to support
 *  languages other than C for this CPA Plugin.
 */
public class TypeNames
{
    public static final String BoolStr = "bool";
    public static final String CharStr = "char";
    public static final String DoubleStr = "double";
    public static final String FloatStr = "float";
    public static final String IntStr = "int";
    public static final String VoidStr = "void";
    public static final String UnknownStr = "unknown";

    private TypeNames () {}
}
