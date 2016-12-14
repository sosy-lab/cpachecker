/*
 ============================================================================
 *  Author      : Vadim Mutilin
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 ============================================================================
*/

#include <stdlib.h>

void kfree(void*);

int __VERIFIER_nondet_int(void);
void *__VERIFIER_nondet_pointer(void);

int ldv_nonpositive(void) {
	int r = __VERIFIER_nondet_int();
	if(r<0) return r;
	else return 0;
} 

int ldv_positive(void) {
	int r = __VERIFIER_nondet_int();
	if(r>0) return r;
	else return 1;
} 

void *memcpy(void*, const void *, size_t);
void *memset(void*, int, size_t);

void *ldv_malloc(size_t size) {
	if(__VERIFIER_nondet_int()) {
		return malloc(size);
	} else {
		return 0;
	}
};

void ldv_assume(int expression)
{
	if (!expression)
	{
		/* Cut this path */
		ldv_assume_label:
		goto ldv_assume_label;
	}
}

void *ldv_calloc(size_t nmemb, size_t size)
{
	if (__VERIFIER_nondet_int()) {
		void *res = calloc(nmemb, size);
		ldv_assume(res != 0);
		return res;
	}
	else {
		return 0;
	}
}

void *ldv_zalloc(size_t size) {
	return ldv_calloc(1, size);
}

#define ENOMEM -3
//--------------------------------------------------------------------------------
//---------------- LDV LIST IMPLEMENTATION --------------------------------------
//--------------------------------------------------------------------------------
struct ldv_list_head {
	struct ldv_list_head *next, *prev;
};

#define LDV_LIST_HEAD_INIT(name) { &(name), &(name) }

#define LDV_LIST_HEAD(name) \
	struct ldv_list_head name = LDV_LIST_HEAD_INIT(name)

static inline void LDV_INIT_LIST_HEAD(struct ldv_list_head *list)
{
	list->next = list;
	list->prev = list;
}

static inline void __ldv_list_add(struct ldv_list_head *new,
                            struct ldv_list_head *prev,
                            struct ldv_list_head *next)
{
	next->prev = new;
	new->next = next;
	new->prev = prev;
	prev->next = new;
}

static inline void __ldv_list_del(struct ldv_list_head * prev, struct ldv_list_head * next)
{
	next->prev = prev;
	prev->next = next;
}

static inline void ldv_list_add(struct ldv_list_head *new, struct ldv_list_head *head)
{
	__ldv_list_add(new, head, head->next);
}

static inline void ldv_list_add_tail(struct ldv_list_head *new, struct ldv_list_head *head)
{
	__ldv_list_add(new, head->prev, head);
}

static inline void ldv_list_del(struct ldv_list_head *entry)
{
	__ldv_list_del(entry->prev, entry->next);
	//entry->next = LIST_POISON1;
	//entry->prev = LIST_POISON2;
}

#define ldv_offsetof(TYPE, MEMBER) ((size_t) &((TYPE *)0)->MEMBER)
/**
 * container_of - cast a member of a structure out to the containing structure
 * @ptr:        the pointer to the member.
 * @type:       the type of the container struct this is embedded in.
 * @member:     the name of the member within the struct.
 *
 */
#define ldv_container_of(ptr, type, member) ({                      \
	const typeof( ((type *)0)->member ) *__mptr = (ptr);    \
	(type *)( (char *)__mptr - ldv_offsetof(type,member) );})

/**
 * list_for_each        -       iterate over a list
 * @pos:        the &struct list_head to use as a loop cursor.
 * @head:       the head for your list.
 */
#define ldv_list_for_each(pos, head) \
	for (pos = (head)->next; pos != (head); pos = pos->next)

#define ldv_list_entry(ptr, type, member) \
	ldv_container_of(ptr, type, member)

#define ldv_list_first_entry(ptr, type, member) \
	ldv_list_entry((ptr)->next, type, member)

/**
 * list_next_entry - get the next element in list
 * @pos:        the type * to cursor
 * @member:     the name of the list_struct within the struct.
 */
#define ldv_list_next_entry(pos, member) \
	ldv_list_entry((pos)->member.next, typeof(*(pos)), member)

/**
 * list_for_each_entry  -       iterate over list of given type
 * @pos:        the type * to use as a loop cursor.
 * @head:       the head for your list.
 * @member:     the name of the list_struct within the struct.
 */
#define ldv_list_for_each_entry(pos, head, member)                          \
	for (pos = ldv_list_first_entry(head, typeof(*pos), member);        \
		&pos->member != (head);                                    \
		pos = ldv_list_next_entry(pos, member))

/**
 * list_for_each_entry_safe - iterate over list of given type safe against removal of list entry
 * @pos:	the type * to use as a loop cursor.
 * @n:		another type * to use as temporary storage
 * @head:	the head for your list.
 * @member:	the name of the list_head within the struct.
 */
#define ldv_list_for_each_entry_safe(pos, n, head, member)		\
	for (pos = ldv_list_first_entry(head, typeof(*pos), member),	\
		n = ldv_list_next_entry(pos, member);			\
	     &pos->member != (head); 					\
	     pos = n, n = ldv_list_next_entry(n, member))


//--------------------------------------------------------------------------------
//---------------- LDV MSGS IMPLEMENTATION --------------------------------------
//--------------------------------------------------------------------------------
LDV_LIST_HEAD(ldv_global_msg_list);

struct ldv_msg {
	void *data;
	struct ldv_list_head list;
};

struct ldv_msg *ldv_msg_alloc() {
	struct ldv_msg *msg;
	msg = (struct ldv_msg*)ldv_malloc(sizeof(struct ldv_msg));
	if(msg) {
		msg->data=0;
		LDV_INIT_LIST_HEAD(&msg->list);
	}
	return msg;
}

int ldv_msg_fill(struct ldv_msg *msg, void *buf, int len) {
	void *data;
	data = ldv_malloc(len);
	if(!data) return -ENOMEM;
	memcpy(data, buf, len);
	msg->data = data;
	return 0;
}

void ldv_msg_free(struct ldv_msg *msg) {
	if(msg) {
		free(msg->data);
		free(msg);
	}
}

int ldv_submit_msg(struct ldv_msg *msg) {
		if(__VERIFIER_nondet_int()) {
			ldv_list_add(&msg->list, &ldv_global_msg_list);
			return 0;
		}
		return -1;
}

void ldv_destroy_msgs(void) {
	struct ldv_msg *msg;
	struct ldv_msg *n;
	ldv_list_for_each_entry_safe(msg, n, &ldv_global_msg_list, list) {
		ldv_list_del(&msg->list);
		//ldv_free_msg(msg); alexeybor:
		ldv_msg_free(msg);
	}
}

//--------------------------------------------------------------------------------
//---------------- END OF MSGS --------------------------------------------------
//--------------------------------------------------------------------------------

//--------------------------------------------------------------------------------
//---------------- DRV DATA -----------------------------------------------------
//--------------------------------------------------------------------------------

struct ldv_device {
	void *platform_data;
	void *driver_data;
	struct ldv_device *parent;
};

static inline void *ldv_dev_get_drvdata(const struct ldv_device *dev)
{
	return dev->driver_data;
}

static inline void ldv_dev_set_drvdata(struct ldv_device *dev, void *data)
{
	dev->driver_data = data;
}

//--------------------------------------------------------------------------------
//---------------- USB INTERFACE ------------------------------------------------
//--------------------------------------------------------------------------------

#define USB_INTERFACE_PROTOCOL_KEYBOARD 7
struct ldv_usb_interface_descriptor {
	char bLength;
	char bDescriptorType;
	char bInterfaceNumber;
	char bAlternateSetting;
	char bNumEndpoints;
	char bInterfaceClass;
	char bInterfaceSubClass;
	char bInterfaceProtocol;
	char iInterface;
} __attribute__ ((packed));

struct ldv_usb_host_interface {
	struct ldv_usb_interface_descriptor desc;
};

struct ldv_usb_interface {
	/* array of alternate settings for this interface,
	 * stored in no particular order */
	struct ldv_usb_host_interface *altsetting;
	struct ldv_usb_host_interface *cur_altsetting;
	struct ldv_device dev;
};

#define ldv_to_usb_interface(d) ldv_container_of(d, struct ldv_usb_interface, dev)

//--------------------------------------------------------------------------------
//---------------- KREF IMPLEMETATION -------------------------------------------
//--------------------------------------------------------------------------------
typedef struct {
        int counter;
} ldv_atomic_t;

struct ldv_kref {
        ldv_atomic_t refcount;
};

struct ldv_kobject {
        char              *name;
        struct ldv_list_head    entry;
        //struct ldv_kobject    *parent;
        //struct ldv_kset       *kset;
        //struct kobj_type      *ktype;
        //struct kernfs_node    *sd;
        struct ldv_kref         kref;
};

static inline int ldv_atomic_add_return(int i, ldv_atomic_t *v)
{
        int temp;
        temp = v->counter;
        temp += i;
        v->counter = temp;
        return temp;
}

static inline int ldv_atomic_sub_return(int i, ldv_atomic_t *v)
{
        int temp;
        temp = v->counter;
        temp -= i;
        v->counter = temp;
        return temp;
}

//asm instruction in the kernel code
#define ldv_atomic_sub_and_test(i, v) (ldv_atomic_sub_return((i), (v)) == 0)
#define ldv_atomic_inc_return(v) ldv_atomic_add_return(1, (v))
#define ldv_atomic_set(v, i) (((v)->counter) = (i))

static inline int ldv_kref_sub(struct ldv_kref *kref, unsigned int count,
            void (*release)(struct ldv_kref *kref))
{
        //WARN_ON(release == NULL);

        if (ldv_atomic_sub_and_test((int) count, &kref->refcount)) {
                release(kref);
                return 1;
        }
        return 0;
}

/**
 * kref_init - initialize object.
 * @kref: object in question.
 */
static inline void ldv_kref_init(struct ldv_kref *kref)
{
        ldv_atomic_set(&kref->refcount, 1);
}

/**
 * kref_get - increment refcount for object.
 * @kref: object.
 */
static inline void ldv_kref_get(struct ldv_kref *kref)
{
        /* If refcount was 0 before incrementing then we have a race
         * condition when this kref is freeing by some other thread right now.
         * In this case one should use kref_get_unless_zero()
         */
        //WARN_ON_ONCE(atomic_inc_return(&kref->refcount) < 2);
        ldv_atomic_inc_return(&kref->refcount);
}

static inline int ldv_kref_put(struct ldv_kref *kref, void (*release)(struct ldv_kref *kref))
{
        return ldv_kref_sub(kref, 1, release);
}


/**
 * kobject_del - unlink kobject from hierarchy.
 * @kobj: object.
 */
void ldv_kobject_del(struct ldv_kobject *kobj)
{
//558         struct kernfs_node *sd;
//559 
        if (!kobj)
                return;
//563         sd = kobj->sd;
//564         sysfs_remove_dir(kobj);
//565         sysfs_put(sd);
//        kobj->state_in_sysfs = 0;
//568         kobj_kset_leave(kobj);
//        kobject_put(kobj->parent);
//        kobj->parent = NULL;
}

static void ldv_kobject_cleanup(struct ldv_kobject *kobj)
{
        //struct kobj_type *t = get_ktype(kobj);
        char *name = kobj->name;
        //if (t && !t->release)
                //pr_debug("kobject: '%s' (%p): does not have a release() "
                //         "function, it is broken and must be fixed.\n",
                //         kobject_name(kobj), kobj);

        //if (t && t->release) {
        //        pr_debug("kobject: '%s' (%p): calling ktype release\n",
        //                 kobject_name(kobj), kobj);
        //        t->release(kobj);
        //}
	//instead of static void dynamic_kobj_release(struct kobject *kobj)
        free(kobj);

        /* free name if we allocated it */
        if (name) {
                free(name);
        }
}

static void ldv_kobject_release(struct ldv_kref *kref) {
	struct ldv_kobject *kobj = ldv_container_of(kref, struct ldv_kobject, kref);
        ldv_kobject_cleanup(kobj);
}

/*
 * kobject_put - decrement refcount for object.
 * @kobj: object.
 *
 * Decrement the refcount, and if 0, call kobject_cleanup().
 */
void ldv_kobject_put(struct ldv_kobject *kobj)
{
        if (kobj) {
                //if (!kobj->state_initialized)
                ldv_kref_put(&kobj->kref, ldv_kobject_release);
        }
}

/**
 * kobject_get - increment refcount for object.
 * @kobj: object.
 */
struct ldv_kobject *ldv_kobject_get(struct ldv_kobject *kobj)
{
        if (kobj)
                ldv_kref_get(&kobj->kref);
        return kobj;
}

static void ldv_kobject_init_internal(struct ldv_kobject *kobj)
{
        if (!kobj)
                return;
        ldv_kref_init(&kobj->kref);
        LDV_INIT_LIST_HEAD(&kobj->entry);
        //kobj->state_in_sysfs = 0;
        //kobj->state_add_uevent_sent = 0;
        //kobj->state_remove_uevent_sent = 0;
        //kobj->state_initialized = 1;
}

void ldv_kobject_init(struct ldv_kobject *kobj)//, struct ldv_kobj_type *ktype)
{
        //char *err_str;

        if (!kobj) {
		//"invalid kobject pointer!";
                goto error;
        }
        //if (!ktype) {
                //"must have a ktype to be initialized properly!\n";
                //goto error;
        //}
        //if (kobj->state_initialized) {
                /* do not error out as sometimes we can recover */
                //printk(KERN_ERR "kobject (%p): tried to init an initialized "
                //       "object, something is seriously wrong.\n", kobj);
        //}

        ldv_kobject_init_internal(kobj);
        //kobj->ktype = ktype;
        return;
error:
	return;
}

/**
 * kobject_create - create a struct kobject dynamically
 *
 * This function creates a kobject structure dynamically and sets it up
 * to be a "dynamic" kobject with a default release function set up.
 *
 * If the kobject was not able to be created, NULL will be returned.
 * The kobject structure returned from here must be cleaned up with a
 * call to kobject_put() and not kfree(), as kobject_init() has
 * already been called on this structure.
 */
struct ldv_kobject *ldv_kobject_create(void)
{
        struct ldv_kobject *kobj;

        kobj = ldv_malloc(sizeof(*kobj));
        if (!kobj)
                return 0;
	memset(kobj, 0, sizeof(*kobj));

        ldv_kobject_init(kobj);//, &ldv_dynamic_kobj_ktype);
        return kobj;
}

//--------------------------------------------------------------------------------
//---------------- --------------------------------------------------------------
//--------------------------------------------------------------------------------

struct A {
	void *p;
};

int f(void) {
	return __VERIFIER_nondet_int();
}

int g(void) {
	return __VERIFIER_nondet_int();
}

