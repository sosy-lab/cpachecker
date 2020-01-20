int main() {
    //Test casting
  __float128 cast_1 = 1445.3335556667777;
  if (((__int128)cast_1) != 1445) goto ERROR;

  //Test casting after arithmeticOperation
  __float128 cast_2 = 4500.9992222444441111100002222;
  if (((__int128) cast_1 * cast_2) <= (__int128)4500 * (__int128)1445) goto ERROR;

  //Testing simple operation
  __float128 op1 = 14533.34343354552525565677823234564789805;
  __float128 op2 = -1.42424242356767897890764244256347857690799686745;
  __float128 mult =  op1 * op2;
  __float128 add =  op1 + op2;
  __float128 subs =  op1 - op2;
  __float128 divide =  op1 / op2;

  if(add + subs != 2*op1) goto ERROR;
  if(mult > 0 || divide > 0) goto ERROR;  

  //Testing small numbers
  __float128 small = 0.0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001;
  __float128 product = small * (__float128)20;
  if(product != 0.000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002) goto ERROR;

  return;
ERROR:
  return 1;

EXIT:
  return 0;
}
