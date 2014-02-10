#define MAX_STRING_LEN BASE_SZ + 2
#define BASE_SZ 2

#define NULL ((void *)0)
#define EOS 0
#define EOF -1
#define ERR -1

typedef int size_t;

extern int nondet_char ();
extern char * ap_cpystrn(char *dst, const char *src, size_t dst_size);
#define GET_CHAR(c,ret) {c = nondet_char();}

int flag=0;

char *get_tag(char *tag, int tagbuf_len)
{
  char *tag_val, c, term;
  int t;

  t = 0;

  --tagbuf_len;

    GET_CHAR(c, NULL);

  if (c == '-') {
    GET_CHAR(c, NULL);
    if (c == '-') {
        GET_CHAR(c, NULL);
      if (c == '>') {
        ap_cpystrn(tag, "done", tagbuf_len);
        return tag;
      }
    }
    return NULL;
  }

  while (1) {
    if (t == tagbuf_len) {
      flag = t;
      tag[t] = EOS;
      return NULL;
    }
    if (c == '=') {
      break;
    }
    flag = t;
    tag[t] = c;
    t++;
    GET_CHAR(c, NULL);
  }
  flag = t;
  tag[t] = EOS;
  t++;
  tag_val = tag + t;

  if (c != '=') {
    return NULL;
  }

    GET_CHAR(c, NULL);

  if (c != '"' && c != '\'') {
    return NULL;
  }
  term = c;
  while (1) {
    GET_CHAR(c, NULL);
    if (t == tagbuf_len) { /* Suppose t == tagbuf_len - 1 */
      flag = t;
      tag[t] = EOS;
      return NULL;
    }

    if (c == '\\') {
      GET_CHAR(c, NULL);
      if (c != term) {
        /* OK */
        flag = t;
        tag[t] = '\\';
        t++;
        if (t == tagbuf_len) {
          /* OK */
          flag = t;
          tag[t] = EOS;
          return NULL;
        }
      }
    }
    else if (c == term) {
      break;
    }

  }
  /* OK */
  flag = t;
  tag[t] = EOS;

  return tag;
}

int main ()
{
  char tag[MAX_STRING_LEN];

  /* The caller always passes in (tag, sizeof(tag)) */
  get_tag (tag, MAX_STRING_LEN);

  return 0;
}

