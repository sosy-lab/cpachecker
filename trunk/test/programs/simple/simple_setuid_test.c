// testcase for simple_setuid.txt
main() {
  // some user input so CPAchecker must check all if branches
  i = someUserFunction();
  if (i == 0) {
    // this should be ok
    // setting the userid
    setuid(2);
    // systemcall
    i = system(20);
  } else if (i == 1) {
    // this should trigger an error
    //systemcall without setting the userid
    system(40);
  }
}
