struct tty_struct;

void ldv_initialize(void) {}
void ldv_handler_precall(void) {}
void ldv_got_tty(struct tty_struct *tty) {}
void ldv_check_tty(struct tty_struct *tty) {}

void main(void)
{
  struct tty_struct *test_tty;
  ldv_initialize();
  ldv_handler_precall();
  if (test_tty)
  {
    ldv_got_tty(test_tty);
    ldv_check_tty(test_tty);
  }
  else // test_tty == NULL; 
  {
    struct tty_struct *test_tty_2 = ((struct tty_struct *)1);
    ldv_got_tty(test_tty);
    ldv_check_tty(test_tty);
  }
}
