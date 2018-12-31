function __VERIFIER_error() {}

function switchTest(value){
  var result = 0;

  switch(value) {
    case 1:
      result += 2;
      break;
    case 2:
      result += 4;
      break;
    default:
      result += 8;
  }
  
  return result;
}
        
if(!(switchTest(0) === 8)){
  __VERIFIER_error();
}

if(!(switchTest(1) === 2)){
  __VERIFIER_error();
}

if(!(switchTest(2) === 4)){
  __VERIFIER_error();
}

if(!(switchTest(3) === 8)){
  __VERIFIER_error();
}

if(!(switchTest(4) === 8)){
  __VERIFIER_error();
}

if(!(switchTest(true) === 8)){
  __VERIFIER_error();
}

if(!(switchTest(false) === 8)){
  __VERIFIER_error();
}

if(!(switchTest(null) === 8)){
  __VERIFIER_error();
}

if(!(switchTest(void 0) === 8)){
  __VERIFIER_error();
}

if(!(switchTest('0') === 8)){
  __VERIFIER_error();
}
