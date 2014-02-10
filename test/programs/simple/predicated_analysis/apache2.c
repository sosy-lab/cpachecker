#define TOKEN_SZ BASE_SZ + 1
#define BASE_SZ 2

#define NULL ((void *)0)
#define EOS 0
#define EOF -1
#define ERR -1
#define LDAP "ldap"
#define LDAP_SZ 4
#define URI_SZ LDAP_SZ + 1 + 1 + TOKEN_SZ + 2

extern int __VERIFIER_nondet_int();

int flag = 0;

void escape_absolute_uri (char *uri, int scheme)
{
  int cp;
  char *token[TOKEN_SZ];
  int c;
  int cond = __VERIFIER_nondet_int();

  if (cond) {
    return;
  }

  cp = scheme;

  flag = cp-1;
  if (uri[cp-1] == '/') {
    while (cp != URI_SZ-1) {
      flag = cp;
      if(uri[cp] != '/')
          break;
      ++cp;
    }
    
    if (cp== URI_SZ-1 || cp+1 == URI_SZ-1) return;
    ++cp;

    scheme = cp;
    cond = __VERIFIER_nondet_int();
    if (cond) {
      c = 0;
      token[0] = uri;
      while (cp != URI_SZ-1
             && c < TOKEN_SZ - 1) {
        flag = cp;
        if (uri[cp] == '?') {
          ++c;
          /* OK */
          token[c] = uri + cp + 1;
         flag =cp;
          uri[cp] = EOS;
        }
        ++cp;
      }
      return;
    }
  }

  return;
}

int main ()
{
  char uri [URI_SZ];
  int scheme;

  uri [URI_SZ-1] = EOS;
  scheme = LDAP_SZ + 2;

  escape_absolute_uri (uri, scheme);

  return 0;
}

