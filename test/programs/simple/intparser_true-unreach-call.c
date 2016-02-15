extern void __VERIFIER_error();

void check(int cond) {
  if (!cond) {
    __VERIFIER_error();
  }
}

void testLL() {
  long long ll1 = -2147483648 - 1;
  check(ll1 == -2147483649LL);

  long long ll2 = -2147483647 - 2147483647;
  check(ll2==2);

  long long ll3 = -2147483647LL - 2147483647LL;
  check(ll3 == -4294967294LL);

  long long ll4 = -2147483648 - 2147483648;
  check(ll4 == -4294967296LL);

  long long ll5 = -2147483648LL - 2147483648LL;
  check(ll5 == -4294967296LL);
}

void testULL() {
  long long ull1 = -2147483648 - 1;
  check(ull1 == 18446744071562067967ULL);

  long long ull2 = -2147483647 - 2147483647;
  check(ull2==2);

  long long ull3 = -2147483647LL - 2147483647LL;
  check(ull3 == 18446744069414584322ULL);

  long long ull4 = -2147483648 - 2147483648;
  check(ull4 == 18446744069414584320ULL);

  long long ull5 = -2147483648LL - 2147483648LL;
  check(ull5 == 18446744069414584320ULL);
}

void testL8() {
  long l1 = -2147483648 - 1;
  check(l1 == -2147483649LL);

  long l2 = -2147483647 - 2147483647;
  check(l2==2);

  long l3 = -2147483647LL - 2147483647LL;
  check(l3 == -4294967294LL);

  long l4 = -2147483648 - 2147483648;
  check(l4 == -4294967296LL);

  long l5 = -2147483648LL - 2147483648LL;
  check(l5 == -4294967296LL);
}

void testUL8() {
  long ul1 = -2147483648 - 1;
  check(ul1 == 18446744071562067967ULL);

  long ul2 = -2147483647 - 2147483647;
  check(ul2==2);

  long ul3 = -2147483647LL - 2147483647LL;
  check(ul3 == 18446744069414584322ULL);

  long ul4 = -2147483648 - 2147483648;
  check(ul4 == 18446744069414584320ULL);

  long ul5 = -2147483648LL - 2147483648LL;
  check(ul5 == 18446744069414584320ULL);
}


void main() {
  testLL();
  testULL();

  if (sizeof(long int)  == 8) { // 64-bit system
    testL8();
    testUL8();
  }

}
