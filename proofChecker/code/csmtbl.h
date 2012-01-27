	/* This is an initialized obstack */

static struct {char *l; void *p; char c[1];} csm_data = {
	&(csm_data.c[1]),
	0,
""};

struct obstack csm_obstack =
	obstack_known_chunk(&csm_data,
	&(csm_data.c[1]), &(csm_data.c[1]), &(csm_data.c[1]),
	4096, 1);

	/* This is an initialized obstack */

static struct {char **l; void *p; char *c[1];} csm_indx = {
	&(csm_indx.c[1]),
	0,{
	&(csm_data.c[0])}};

struct csmalign {char ___x; char *___d;};
static struct obstack csm_indx_obstk =
	obstack_known_chunk(&csm_indx, &(csm_indx.c[0]),
	&(csm_indx.c[1]), &(csm_indx.c[1]), 4096,
	((PTR_INT_TYPE) ((char *) &((struct csmalign *) 0)->___d - (char *) 0)));

char **strng = csm_indx.c;
int numstr = 1;

