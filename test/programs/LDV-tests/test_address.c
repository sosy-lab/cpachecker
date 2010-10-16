#include <malloc.h>
#include <assert.h>

int VERDICT_SAFE;
int CURRENTLY_UNSAFE;

struct path_info {
	int list;
};

void list_add(int *new) {
	assert(new!=NULL);
}

static void rr_fail_path(struct path_info *pi)
{
	list_add(&pi->list);
}


int main(void) {
	struct path_info pi;
	rr_fail_path(&pi);
}
