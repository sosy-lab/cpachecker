int global;

int f(unsigned int cmd) {
  switch (cmd) {
  case (1UL | (unsigned long )4 ): 
    global++;
  break;
}
}

int ldv_main() {

  f(1UL | (unsigned long )3);
}
