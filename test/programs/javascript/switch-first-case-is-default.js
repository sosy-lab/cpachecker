function __VERIFIER_error() {}

function switchTest(value){
  var result = 0;

  switch(value) {
    default:
      result += 2;
      break;
    case 1:
      result += 4;
      break;
    case 2:
      result += 8;
      break;
  }

  return result;
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
