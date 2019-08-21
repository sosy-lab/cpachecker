# 1 "asm1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "asm1/main.c"



char *strerror(int) __asm("_" "strerror" "$UNIX2003");
int global asm("rax");
asm ("nop");





asm void test();


void asm test()
{
  mov eax, eax
}


int main()
{

  __asm volatile("mov ax, dx");


  register unsigned my_var asm("eax")=1;


  __asm {
     mov al, 2
     mov dx, 0xD007
     out dx, al
  }
# 51 "asm1/main.c"
  return 0;
}
