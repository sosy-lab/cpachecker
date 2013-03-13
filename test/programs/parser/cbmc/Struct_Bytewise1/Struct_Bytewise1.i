# 1 "Struct_Bytewise1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Struct_Bytewise1/main.c"
# 12 "Struct_Bytewise1/main.c"
typedef struct my_struct
{

  unsigned a;
  unsigned b;
} t_logAppl;

static t_logAppl logAppl;
static unsigned char arrayTmp[8];

void CopyBuffer(unsigned char *src) {
  int i;
  for(i=0;i<8;i++){
    arrayTmp[i] = src[i];
  }
}

int main()
{
  logAppl.a=1;
  logAppl.b=0x01000002;
  CopyBuffer((unsigned char *)&logAppl);
# 47 "Struct_Bytewise1/main.c"
  assert(arrayTmp[0]==1);
  assert(arrayTmp[1]==0);
  assert(arrayTmp[2]==0);
  assert(arrayTmp[3]==0);

  assert(arrayTmp[4]==2);
  assert(arrayTmp[5]==0);
  assert(arrayTmp[6]==0);
  assert(arrayTmp[7]==1);

}
