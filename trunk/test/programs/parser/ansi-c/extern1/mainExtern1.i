# 1 "extern1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "extern1/main.c"
extern char a[];
char a[255];


unsigned char rtc_cmos_read(unsigned char addr)
{
}

extern __typeof__(rtc_cmos_read) rtc_cmos_read;

int main()
{
  return 0;
}
