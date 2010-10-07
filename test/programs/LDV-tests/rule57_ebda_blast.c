#include <assert.h>

#ifdef BLAST_AUTO_1
/* tmp_slot is freed before exit */
int VERDICT_SAFE;
int CURRENTLY_SAFE;
#else
int VERDICT_UNSAFE;
int CURRENTLY_UNSAFE;
#endif

struct hotplug_slot;

struct bus_info {
};

struct slot {
	int a;
	int b;
	struct hotplug_slot * hotplug_slot;
	struct bus_info *bus_on;
};

struct hotplug_slot {
	struct slot *private;
	int b;
};

#define MY_ENOMEM 2
#define MY_ENODEV 3
#define MY_GFP_KERNEL 1
#define NULL 0

struct slot *tmp_slot;
int used_tmp_slot = 0;
int freed_tmp_slot = 1;

extern void * kzalloc(int, int);

void kfree(void *p) {
	if(p!=NULL && p==tmp_slot)
		freed_tmp_slot = 1;
}

extern struct bus_info *ibmphp_find_same_bus_num();

extern int fillslotinfo(struct hotplug_slot *);
extern int ibmphp_init_devno(struct slot **);

int ebda_rsrc_controller() {
	struct hotplug_slot *hp_slot_ptr;
	//struct slot *tmp_slot;
	struct bus_info *bus_info_ptr1;
	int rc;

	hp_slot_ptr = kzalloc(sizeof(*hp_slot_ptr), MY_GFP_KERNEL);
	if(!hp_slot_ptr) {
		rc = -MY_ENOMEM;
		goto error_no_hp_slot;
	}
	hp_slot_ptr->b = 5;

	tmp_slot = kzalloc(sizeof(*tmp_slot), MY_GFP_KERNEL);

	if(!tmp_slot) {
		rc = -MY_ENOMEM;
		goto error_no_slot;
	}
	//change state
	used_tmp_slot = 0;
	freed_tmp_slot = 0;

	tmp_slot->a = 2;
	tmp_slot->b = 3;
	
	bus_info_ptr1 = ibmphp_find_same_bus_num();
	if(!bus_info_ptr1) {
		rc = -MY_ENODEV;
#ifdef BLAST_AUTO_1
		//BUG if not done
		kfree(tmp_slot);
		freed_tmp_slot = 1;
#endif
		goto error;
	}
	tmp_slot->bus_on = bus_info_ptr1;
	bus_info_ptr1 = NULL;

	tmp_slot->hotplug_slot = hp_slot_ptr;

	hp_slot_ptr->private = tmp_slot;
	used_tmp_slot = 1;

	rc = fillslotinfo(hp_slot_ptr);
	if(rc)
		goto error;

	rc = ibmphp_init_devno((struct slot **) &hp_slot_ptr->private);
	if(rc) 
		goto error;

	return 0;

error:
	kfree(hp_slot_ptr->private);
error_no_slot:
	//kfree(hp_slot_ptr->name);
error_no_hp_slot:
	//skip
	return rc;
}

void main() {
	ebda_rsrc_controller();
	if(!used_tmp_slot)
		assert(freed_tmp_slot);
}
