int main() {
  __int128 i128;
  unsigned __int128 ui128;
  __int128_t i128_t;
  __uint128_t ui128_t;

  if (((__int128)2 << 100) <= 0) goto ERROR;

  __uint128_t j = (__uint128_t)0xFFFFFFFFFFFFFFFF + (__uint128_t)0x1000000000000000;
  if ((j >> 1) != 0x87FFFFFFFFFFFFFF) goto ERROR;

  __uint128_t op1 = 0xFFFFFFFFFFFFFFFF;
  __uint128_t op2 = 0x0000000011111111;
  
  //binary operations
  if((op1&op2) != op2) goto ERROR;
  if((op1|op2) != op1) goto ERROR;
  if((op1^op2) != 0xFFFFFFFFEEEEEEEE) goto ERROR;

  //arithmetic operations
  if((op1%(op2-1)) != 0xFF) goto ERROR;
  if(op1 - op2 != (op1^op2)) goto ERROR;
  //only true if op1%op2 == 0
  if((op1 / op2) * op2 != op1) goto ERROR;

  return;
ERROR:
  return 1;

EXIT:
  return 0;
}

