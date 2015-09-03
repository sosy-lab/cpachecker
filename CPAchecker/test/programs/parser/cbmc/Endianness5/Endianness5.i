# 1 "Endianness5/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Endianness5/main.c"
unsigned char regb[100];
unsigned short *ptrUShort;
unsigned short shortTmp;

int main()
{
  ptrUShort = (unsigned short*)(&regb[12]);
  shortTmp= *ptrUShort;


  *ptrUShort = 1234;
}
