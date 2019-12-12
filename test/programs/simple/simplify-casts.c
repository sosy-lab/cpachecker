
int f(unsigned int cmd) {
  switch (cmd) {
  case (1UL | (unsigned long )4 ):
    ERROR: goto ERROR;
  break;
}
}

int main() {

  f(1UL | (unsigned long )3);
}
