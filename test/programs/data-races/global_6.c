//Test work of InterruptFilter

int intNoCnct;
int intNoSrc;

void qbusIntr() 
{ 
  register unsigned int intSrc ;
  register unsigned int intEna ;
  register unsigned int iv ;

  iv = 0;
  while (1) {
    if (iv < 13U) {

    } else {
      break;
    }
    if (! (intSrc & (unsigned int )(1 << iv))) {
      goto __Cont;
    } 
    if (intEna) {
      intNoCnct = intNoCnct + 1U;
      goto __Cont;
    } 
    __Cont: /* CIL Label */ 
    iv = iv + 1U;
  }
  if (! (intSrc & 8191U)) {
    intNoSrc = intNoSrc + 1U;
  } 
  return;
}

int ldv_main() {
  qbusIntr();
}
