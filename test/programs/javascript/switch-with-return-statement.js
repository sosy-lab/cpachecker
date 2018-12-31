function __VERIFIER_error() {}

function switchTest(value){
  switch(value) {
    case 1:
      return 4;
    case 2:
      return 8;
  }
  return 2;
}
        
if(!(switchTest(0) === 2)){
  __VERIFIER_error();
}

if(!(switchTest(1) === 4)){
  __VERIFIER_error();
}

if(!(switchTest(2) === 8)){
  __VERIFIER_error();
}

if(!(switchTest(3) === 2)){
  __VERIFIER_error();
}

if(!(switchTest(4) === 2)){
  __VERIFIER_error();
}

if(!(switchTest(true) === 2)){
  __VERIFIER_error();
}

if(!(switchTest(false) === 2)){
  __VERIFIER_error();
}

if(!(switchTest(null) === 2)){
  __VERIFIER_error();
}

if(!(switchTest(void 0) === 2)){
  __VERIFIER_error();
}

if(!(switchTest('0') === 2)){
  __VERIFIER_error();
}
