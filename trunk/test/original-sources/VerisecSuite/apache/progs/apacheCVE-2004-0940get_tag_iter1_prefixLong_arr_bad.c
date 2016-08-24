#include "../apache.h"
#include <assert.h>

char *get_tag(char *tag, int tagbuf_len)
{
  char *tag_val, c, term;
  int t;

  t = 0;

  --tagbuf_len;

  do {
    GET_CHAR(c, NULL);
  } while (ap_isspace(c));

  if (c == '-') {
    GET_CHAR(c, NULL);
    if (c == '-') {
      do {
        GET_CHAR(c, NULL);
      } while (ap_isspace(c));
      if (c == '>') {
        ap_cpystrn(tag, "done", tagbuf_len);
        return tag;
      }
    }
    return NULL;
  }

  while (1) {
    if (t == tagbuf_len) {
      tag[t] = EOS;
      return NULL;
    }
    if (c == '=' || ap_isspace(c)) {
      break;
    }
    tag[t] = ap_tolower(c);
    t++;
    GET_CHAR(c, NULL);
  }

  tag[t] = EOS;
  t++;
  tag_val = tag + t;

  while (ap_isspace(c)) {
    GET_CHAR(c, NULL);
  }
  if (c != '=') {
    return NULL;
  }

  do {
    GET_CHAR(c, NULL);
  } while (ap_isspace(c));

  if (c != '"' && c != '\'') {
    return NULL;
  }
  term = c;
  while (1) {
    GET_CHAR(c, NULL);
    if (t == tagbuf_len) { /* Suppose t == tagbuf_len - 1 */
      tag[t] = EOS;
      return NULL;
    }

    if (c == '\\') {
      /*von mir eingefügt*/
      assert(t + 1 < tagbuf_len);
      /* BAD */
      tag[t] = c;
      t++;               /* Now t == tagbuf_len */
      GET_CHAR(c, NULL);
    }
    else if (c == term) {
      break;
    }

    /*von mir eingefügt*/
    assert(t + 2 < tagbuf_len);
    /* BAD */
    tag[t] = c;    
    t++;                /* Now t == tagbuf_len + 1 
                         * So the bounds check (t == tagbuf_len) will fail */
  }
  /* BAD */
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
