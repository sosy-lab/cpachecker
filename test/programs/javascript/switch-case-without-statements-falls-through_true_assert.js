/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
function __VERIFIER_error() {}

function switchTest(value){
  var result = 0;

  switch(value) {
    case 0:
    case 1:
      result += 4;
      break;
    case 2:
      result += 8;
  }

  return result;
}
        
if(!(switchTest(0) === 4)){
  __VERIFIER_error();
}

if(!(switchTest(1) === 4)){
  __VERIFIER_error();
}

if(!(switchTest(2) === 8)){
  __VERIFIER_error();
}

if(!(switchTest(3) === 0)){
  __VERIFIER_error();
}

if(!(switchTest(4) === 0)){
  __VERIFIER_error();
}

if(!(switchTest(true) === 0)){
  __VERIFIER_error();
}

if(!(switchTest(false) === 0)){
  __VERIFIER_error();
}

if(!(switchTest(null) === 0)){
  __VERIFIER_error();
}

if(!(switchTest(void 0) === 0)){
  __VERIFIER_error();
}

if(!(switchTest('0') === 0)){
  __VERIFIER_error();
}
