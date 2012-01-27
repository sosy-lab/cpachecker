#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "clp.h"
#include "clpintf.h"
#include "pdl_gen.h"
#include "csm.h"
#include "eliproto.h"

#define TYPE_MASK      0x001F
#define BOOL           0x0001
#define INTVAL         0x0002
#define STRVAL         0x0004
#define USAGE          0x0008
#define TERM           0x0010

#define JOINED         0x0100
#define SPACED         0x0200
#define PLURAL         0x0400

typedef struct opt opt;
struct opt {
    CONST char *str;
    int flags;
    int *count;
    DefTableKey *key;
    DefTableKeyList *list;
};

DefTableKey CLP_InputFile = NoKey;
DefTableKey InputFile = NoKey;
static int error_count = 0;

#define NUMOPTS 0

static opt opts[] = {
    { "dummy", 0, NULL, NULL, NULL }
};

static void
#ifdef PROTO_OK
clp_error (const char *str, const char *msg)
#else
clp_error (str, msg)
char *str, *msg;
#endif
{
  if (str == (char *) NULL)
    fprintf (stderr, "%s\n", msg);
  else
    fprintf (stderr, "%s %s\n", str, msg);
  error_count++;
}

static int
#ifdef PROTO_OK
clp_atos (char *str)
#else
clp_atos (str)
char *str;
#endif
{
  return stostr (str, strlen(str));
}

void
#ifdef PROTO_OK
clp_usage (char *prog)
#else
clp_usage (prog)
  char *prog;
#endif
{
  fprintf (stderr, "Usage: %s [options] parameters\n", prog);
  fprintf (stderr, "  Items marked with * may be repeated.\n");
  fprintf (stderr, "  Options:\n");
  fprintf (stderr, "  Parameters:\n");
  fprintf (stderr, "    InputFile File to be processed\n");
    exit (1);
}

void
#ifdef PROTO_OK
clp_init (int argc, char *argv[])
#else
clp_init (argc, argv)
int argc;
char *argv[];
#endif
{
  int i, j, p, t;
  DefTableKey k;
  opt *o;
  char *s;

  SetFileErr ("%p: cannot open %f for reading", 0);
  for (i = 1; i < argc; i++) {
    for (j = 0, o = opts; j < NUMOPTS; j++, o++)
      if (strncmp (o->str, argv[i], strlen (o->str)) == 0)
        break;
    if (j == NUMOPTS)
      break;
    s = argv[i] + strlen (o->str);
    if (o->flags & USAGE) {
      if (*s != '\0')
        clp_error (o->str, "must not have a value");
      clp_usage (argv[0]);
    } else if (o->flags & TERM) {
      if (*s != '\0')
        clp_error (o->str, "must not have a value");
      i++;
      break;
    } else {
      if (o->count != (int *) NULL)
        (*o->count)++;
      p = o->flags & PLURAL;
      if ((!p) && (*o->count > 1))
        clp_error (o->str, "can only be given once");
      t = o->flags & TYPE_MASK;
      switch (t) {
        case BOOL:
          if (*s != '\0')
            clp_error (o->str, "must not have a value");
          break;
        case INTVAL:
        case STRVAL: {
          if (o->flags & JOINED) {
            if (*s == '\0') {
              if (o->flags & SPACED) {
                if (i == argc - 1) {
                  clp_error (o->str, "must have a value");
                  continue;
                } else {
                  i++;
                  s = argv[i];
                }
              } else {
                clp_error (o->str, "must have a joined value");
                continue;
              }
            }
          } else if (o->flags & SPACED) {
            if (*s != '\0') {
              clp_error (o->str, "must not have a joined value");
              continue;
            }
            if (i == argc - 1) {
              clp_error (o->str, "must have a value");
              continue;
            } else {
              i++;
              s = argv[i];
            }
          }
          if (p) {
            DefTableKey k = NewKey ();
            switch (t) {
              case INTVAL:
                SetClpValue (k, atoi (s), 0);
                break;
              case STRVAL:
                SetClpValue (k, clp_atos (s), 0);
                break;
            }
            *o->list = AppElDefTableKeyList (*o->list, k);
          } else {
            *o->key = NewKey ();
            switch (t) {
              case INTVAL:
                SetClpValue (*o->key, atoi (s), 0);
                break;
              case STRVAL:
                SetClpValue (*o->key, clp_atos (s), 0);
                break;
            }
          }
        }
      }
    }
  }
  if (i < argc) {
    InputFile = NewKey ();
    SetClpValue (InputFile, clp_string (argv[i++]), 0);
    CLP_InputFile = InputFile;
  }
  if ((i < argc) || (error_count > 0))
    clp_usage (argv[0]);
}
