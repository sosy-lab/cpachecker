/**
 * use-after-free bug, found in Linux 2.6.14.3 in the infiniband drivers.
 * fixed with commit 2d6eac6c4fdaa69656d66c80754d267be233cc3f
 *
 * Simplified version.
 * Use cil with --dosimpleMem flag.
 */
#include <stdio.h>
#include <stdlib.h>

#define     ENOMEM          12      /* Out of memory */



struct ib_pd {
    int field;
};

typedef struct ib_mr {
    int device;
    struct ib_pd* pd;
} ib_mr;

typedef struct ib_mad_agent {
    struct ib_mr *mr;
} ib_mad_agent;

typedef struct ib_mad_agent_private {
	struct ib_mad_agent agent;
	unsigned long timeout;
} ib_mad_agent_private;

struct ib_mad_reg_req {
    int a;
};

void kfree(/*const*/ void * objp) {
    free(objp);
}

void* kmalloc(size_t size) {
    return malloc(size);
}


int ib_dereg_mr(struct ib_mr *mr)
{
	struct ib_pd *pd;
	int ret = 0;

	pd = mr->pd;

	return ret;
}

struct ib_mad_agent *ib_register_mad_agent(struct ib_mad_reg_req *mad_reg_req)
{
	int ret = 0;
	ib_mad_agent_private *mad_agent_priv;
	struct ib_mad_reg_req *reg_req = NULL;

	mad_agent_priv = kmalloc(sizeof (struct ib_mad_agent_private));
	if (!mad_agent_priv) {
		ret = -ENOMEM;
		goto err1;
	}

	if (mad_reg_req) {
		reg_req = kmalloc(sizeof *reg_req);
		if (!reg_req) {
			ret = -ENOMEM;
			goto err3;
		}
	}

	return &mad_agent_priv->agent;

err3:
	kfree(mad_agent_priv);
err2:
	ib_dereg_mr(mad_agent_priv->agent.mr); // error!
err1:
	return ret;
}


int main(void) {
    struct ib_mad_reg_req* reg_req = NULL;
    reg_req = kmalloc(sizeof *reg_req);

    ib_register_mad_agent(reg_req);

    return 0;
}

