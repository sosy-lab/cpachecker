function __VERIFIER_error() {}

function switchTest(value){
  var result = 0;

  switch(value) {
    case 0:
      result += 2;
      break;
    case 1:
      break;
    default:
      result += 4;
      break;
  }

  return result;
}
        
if(!(switchTest(0) === 2)){
  __VERIFIER_error();
}

if(!(switchTest(1) === 0)){
  __VERIFIER_error();
}

if(!(switchTest(2) === 4)){
  __VERIFIER_error();
}

if(!(switchTest(3) === 4)){
  __VERIFIER_error();
}

if(!(switchTest(4) === 4)){
  __VERIFIER_error();
}

if(!(switchTest(true) === 4)){
  __VERIFIER_error();
}

if(!(switchTest(false) === 4)){
  __VERIFIER_error();
}

if(!(switchTest(null) === 4)){
  __VERIFIER_error();
}

if(!(switchTest(void 0) === 4)){
  __VERIFIER_error();
}

if(!(switchTest('0') === 4)){
  __VERIFIER_error();
}
