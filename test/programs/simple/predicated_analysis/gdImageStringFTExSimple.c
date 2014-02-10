#define BASE_SZ 2
#define INSZ BASE_SZ + 1
#define EOS 0
#define Tcl_UniChar int
#define gdFTEX_Unicode 0
#define gdFTEX_Shift_JIS 1
#define gdFTEX_Big5 2

extern __VERIFIER_nondet_int();

int flag = 0;
static int gdTcl_UtfToUniChar (char *str, Tcl_UniChar *chPtr)
{
  return 1;
}


/* Greatly, GREATLY simplified. There's a bunch of cruft that doesn't
 * have to do with the manipulation of "string". */
void gdImageStringFTEx (char *string) {
  int next;
  int encoding;
  int ch;
  int len;

  encoding = __VERIFIER_nondet_int();
  if (encoding > 2 || encoding < 0)
    return;

  next = 0;
  /* OK */
  while (next != INSZ-1)
    {
      /* grabbing a character and storing it in an int
       *
       * this'll fill the low-order byte, and keep more space free for
       * extra bytes for Unicode encoding, etc.
       */
      flag = next;
      ch = string[next];

      /* carriage returns */
      if (ch == '\r')
	{
	  next++;
	  continue;
	}
      /* newlines */
      if (ch == '\n')
	{
	  next++;
	  continue;
	}


      switch (encoding)
        {
        case gdFTEX_Unicode:
          {
            len = gdTcl_UtfToUniChar (string + next, &ch);
            next += len;
          }
          break;
        case gdFTEX_Shift_JIS:
          {
            unsigned char c;
            flag = next;
            c = (unsigned char) string[next];
            if (0xA1 <= c && c <= 0xFE)
              {
                next++;
              }
            if (next != INSZ-1)
              next++;
          }
          break;
        case gdFTEX_Big5:
          {
            flag = next;
            ch = (string[next]) & 0xFF;	/* don't extend sign */
            next++;
            if (ch >= 161	/* first code of JIS-8 pair */
                && next != INSZ-1)
              {
                flag = next;
                ch = (ch * 256) + ((string[next]) & 255);
                next++;
              }
          }
          break;
        }
    }
}

int main ()
{
  char in [INSZ];
  in [INSZ-1] = EOS;

  gdImageStringFTEx (in);

  return 0;
}


