int main() {
  __int128 i128;
  unsigned __int128 ui128;
  __int128_t i128_t;
  __uint128_t ui128_t;
  __float128 f128;

  if (((__int128)2 << 100) <= 0) goto ERROR;

  __uint128_t j = (__uint128_t)0xFFFFFFFFFFFFFFFF + (__uint128_t)0x1000000000000000;
  if ((j >> 1) != 0x87FFFFFFFFFFFFFF) goto ERROR;
  
  return;
ERROR:
  return 1;

EXIT:
  return 0;
}

