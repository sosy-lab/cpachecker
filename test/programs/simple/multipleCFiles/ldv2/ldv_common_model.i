# 1 "/home/alpha/work/work/current--X--drivers/media/platform/soc_camera/sh_mobile_ceu_camera.ko--X--defaultlinux-3.15.6.tar.xz--X--104_1a--X--cpachecker/linux-3.15.6.tar.xz/csd_deg_dscv/12/dscv_tempdir/rule-instrumentor/104_1a/common-model/ldv_common_model.c"
# 1 "<command-line>"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/include/linux/kconfig.h" 1



# 1 "include/generated/autoconf.h" 1
# 5 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/include/linux/kconfig.h" 2
# 1 "<command-line>" 2
# 1 "/home/alpha/git/ldv-tools-newdeg/kernel-rules/models/config-tracers.h" 1
# 1 "<command-line>" 2
# 1 "/home/alpha/work/work/current--X--drivers/media/platform/soc_camera/sh_mobile_ceu_camera.ko--X--defaultlinux-3.15.6.tar.xz--X--104_1a--X--cpachecker/linux-3.15.6.tar.xz/csd_deg_dscv/12/dscv_tempdir/rule-instrumentor/104_1a/common-model/ldv_common_model.c"


# 1 "/home/alpha/git/ldv-tools-newdeg/kernel-rules/verifier/rcv.h" 1
# 10 "/home/alpha/git/ldv-tools-newdeg/kernel-rules/verifier/rcv.h"
static inline void ldv_error(void)
{
  LDV_ERROR: goto LDV_ERROR;
}






static inline void ldv_stop(void) {
  LDV_STOP: goto LDV_STOP;
}


int ldv_undef_int(void);
void *ldv_undef_ptr(void);
unsigned long ldv_undef_ulong(void);

static inline int ldv_undef_int_negative(void)
{
  int ret = ldv_undef_int();

  ((ret < 0) ? 0 : ldv_stop());

  return ret;
}

static inline int ldv_undef_int_nonpositive(void)
{
  int ret = ldv_undef_int();

  ((ret <= 0) ? 0 : ldv_stop());

  return ret;
}



long __builtin_expect(long exp, long c)
{
  return exp;
}






void __builtin_trap(void)
{
  ((0) ? 0 : ldv_error());
}
# 4 "/home/alpha/work/work/current--X--drivers/media/platform/soc_camera/sh_mobile_ceu_camera.ko--X--defaultlinux-3.15.6.tar.xz--X--104_1a--X--cpachecker/linux-3.15.6.tar.xz/csd_deg_dscv/12/dscv_tempdir/rule-instrumentor/104_1a/common-model/ldv_common_model.c" 2
# 1 "include/linux/device.h" 1
# 16 "include/linux/device.h"
# 1 "include/linux/ioport.h" 1
# 12 "include/linux/ioport.h"
# 1 "include/linux/compiler.h" 1
# 54 "include/linux/compiler.h"
# 1 "include/linux/compiler-gcc.h" 1
# 106 "include/linux/compiler-gcc.h"
# 1 "include/linux/compiler-gcc4.h" 1
# 107 "include/linux/compiler-gcc.h" 2
# 55 "include/linux/compiler.h" 2
# 79 "include/linux/compiler.h"
struct ftrace_branch_data {
 const char *func;
 const char *file;
 unsigned line;
 union {
  struct {
   unsigned long correct;
   unsigned long incorrect;
  };
  struct {
   unsigned long miss;
   unsigned long hit;
  };
  unsigned long miss_hit[2];
 };
};
# 13 "include/linux/ioport.h" 2
# 1 "include/linux/types.h" 1




# 1 "include/uapi/linux/types.h" 1



# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/types.h" 1



# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/include/uapi/asm-generic/types.h" 1





# 1 "include/asm-generic/int-ll64.h" 1
# 10 "include/asm-generic/int-ll64.h"
# 1 "include/uapi/asm-generic/int-ll64.h" 1
# 11 "include/uapi/asm-generic/int-ll64.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/bitsperlong.h" 1
# 10 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/bitsperlong.h"
# 1 "include/asm-generic/bitsperlong.h" 1



# 1 "include/uapi/asm-generic/bitsperlong.h" 1
# 5 "include/asm-generic/bitsperlong.h" 2
# 11 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/bitsperlong.h" 2
# 12 "include/uapi/asm-generic/int-ll64.h" 2







typedef __signed__ char __s8;
typedef unsigned char __u8;

typedef __signed__ short __s16;
typedef unsigned short __u16;

typedef __signed__ int __s32;
typedef unsigned int __u32;


__extension__ typedef __signed__ long long __s64;
__extension__ typedef unsigned long long __u64;
# 11 "include/asm-generic/int-ll64.h" 2




typedef signed char s8;
typedef unsigned char u8;

typedef signed short s16;
typedef unsigned short u16;

typedef signed int s32;
typedef unsigned int u32;

typedef signed long long s64;
typedef unsigned long long u64;
# 7 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/include/uapi/asm-generic/types.h" 2
# 5 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/types.h" 2
# 5 "include/uapi/linux/types.h" 2
# 13 "include/uapi/linux/types.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/include/uapi/linux/posix_types.h" 1



# 1 "include/linux/stddef.h" 1



# 1 "include/uapi/linux/stddef.h" 1
# 5 "include/linux/stddef.h" 2





enum {
 false = 0,
 true = 1
};
# 5 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/include/uapi/linux/posix_types.h" 2
# 24 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/include/uapi/linux/posix_types.h"
typedef struct {
 unsigned long fds_bits[1024 / (8 * sizeof(long))];
} __kernel_fd_set;


typedef void (*__kernel_sighandler_t)(int);


typedef int __kernel_key_t;
typedef int __kernel_mqd_t;

# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/posix_types.h" 1



# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/posix_types_64.h" 1
# 10 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/posix_types_64.h"
typedef unsigned short __kernel_old_uid_t;
typedef unsigned short __kernel_old_gid_t;


typedef unsigned long __kernel_old_dev_t;


# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/include/uapi/asm-generic/posix_types.h" 1
# 14 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/include/uapi/asm-generic/posix_types.h"
typedef long __kernel_long_t;
typedef unsigned long __kernel_ulong_t;



typedef __kernel_ulong_t __kernel_ino_t;



typedef unsigned int __kernel_mode_t;



typedef int __kernel_pid_t;



typedef int __kernel_ipc_pid_t;



typedef unsigned int __kernel_uid_t;
typedef unsigned int __kernel_gid_t;



typedef __kernel_long_t __kernel_suseconds_t;



typedef int __kernel_daddr_t;



typedef unsigned int __kernel_uid32_t;
typedef unsigned int __kernel_gid32_t;
# 71 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/include/uapi/asm-generic/posix_types.h"
typedef __kernel_ulong_t __kernel_size_t;
typedef __kernel_long_t __kernel_ssize_t;
typedef __kernel_long_t __kernel_ptrdiff_t;




typedef struct {
 int val[2];
} __kernel_fsid_t;





typedef __kernel_long_t __kernel_off_t;
typedef long long __kernel_loff_t;
typedef __kernel_long_t __kernel_time_t;
typedef __kernel_long_t __kernel_clock_t;
typedef int __kernel_timer_t;
typedef int __kernel_clockid_t;
typedef char * __kernel_caddr_t;
typedef unsigned short __kernel_uid16_t;
typedef unsigned short __kernel_gid16_t;
# 18 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/posix_types_64.h" 2
# 5 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/posix_types.h" 2
# 36 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/include/uapi/linux/posix_types.h" 2
# 14 "include/uapi/linux/types.h" 2
# 32 "include/uapi/linux/types.h"
typedef __u16 __le16;
typedef __u16 __be16;
typedef __u32 __le32;
typedef __u32 __be32;
typedef __u64 __le64;
typedef __u64 __be64;

typedef __u16 __sum16;
typedef __u32 __wsum;
# 6 "include/linux/types.h" 2






typedef __u32 __kernel_dev_t;

typedef __kernel_fd_set fd_set;
typedef __kernel_dev_t dev_t;
typedef __kernel_ino_t ino_t;
typedef __kernel_mode_t mode_t;
typedef unsigned short umode_t;
typedef __u32 nlink_t;
typedef __kernel_off_t off_t;
typedef __kernel_pid_t pid_t;
typedef __kernel_daddr_t daddr_t;
typedef __kernel_key_t key_t;
typedef __kernel_suseconds_t suseconds_t;
typedef __kernel_timer_t timer_t;
typedef __kernel_clockid_t clockid_t;
typedef __kernel_mqd_t mqd_t;

typedef _Bool bool;

typedef __kernel_uid32_t uid_t;
typedef __kernel_gid32_t gid_t;
typedef __kernel_uid16_t uid16_t;
typedef __kernel_gid16_t gid16_t;

typedef unsigned long uintptr_t;



typedef __kernel_old_uid_t old_uid_t;
typedef __kernel_old_gid_t old_gid_t;



typedef __kernel_loff_t loff_t;
# 54 "include/linux/types.h"
typedef __kernel_size_t size_t;




typedef __kernel_ssize_t ssize_t;




typedef __kernel_ptrdiff_t ptrdiff_t;




typedef __kernel_time_t time_t;




typedef __kernel_clock_t clock_t;




typedef __kernel_caddr_t caddr_t;



typedef unsigned char u_char;
typedef unsigned short u_short;
typedef unsigned int u_int;
typedef unsigned long u_long;


typedef unsigned char unchar;
typedef unsigned short ushort;
typedef unsigned int uint;
typedef unsigned long ulong;




typedef __u8 u_int8_t;
typedef __s8 int8_t;
typedef __u16 u_int16_t;
typedef __s16 int16_t;
typedef __u32 u_int32_t;
typedef __s32 int32_t;



typedef __u8 uint8_t;
typedef __u16 uint16_t;
typedef __u32 uint32_t;


typedef __u64 uint64_t;
typedef __u64 u_int64_t;
typedef __s64 int64_t;
# 133 "include/linux/types.h"
typedef unsigned long sector_t;
typedef unsigned long blkcnt_t;
# 146 "include/linux/types.h"
typedef u64 dma_addr_t;
# 157 "include/linux/types.h"
typedef unsigned gfp_t;
typedef unsigned fmode_t;
typedef unsigned oom_flags_t;


typedef u64 phys_addr_t;




typedef phys_addr_t resource_size_t;





typedef unsigned long irq_hw_number_t;

typedef struct {
 int counter;
} atomic_t;


typedef struct {
 long counter;
} atomic64_t;


struct list_head {
 struct list_head *next, *prev;
};

struct hlist_head {
 struct hlist_node *first;
};

struct hlist_node {
 struct hlist_node *next, **pprev;
};

struct ustat {
 __kernel_daddr_t f_tfree;
 __kernel_ino_t f_tinode;
 char f_fname[6];
 char f_fpack[6];
};






struct callback_head {
 struct callback_head *next;
 void (*func)(struct callback_head *head);
};
# 14 "include/linux/ioport.h" 2




struct resource {
 resource_size_t start;
 resource_size_t end;
 const char *name;
 unsigned long flags;
 struct resource *parent, *sibling, *child;
};
# 138 "include/linux/ioport.h"
extern struct resource ioport_resource;
extern struct resource iomem_resource;

extern struct resource *request_resource_conflict(struct resource *root, struct resource *new);
extern int request_resource(struct resource *root, struct resource *new);
extern int release_resource(struct resource *new);
void release_child_resources(struct resource *new);
extern void reserve_region_with_split(struct resource *root,
        resource_size_t start, resource_size_t end,
        const char *name);
extern struct resource *insert_resource_conflict(struct resource *parent, struct resource *new);
extern int insert_resource(struct resource *parent, struct resource *new);
extern void insert_resource_expand_to_fit(struct resource *root, struct resource *new);
extern void arch_remove_reservations(struct resource *avail);
extern int allocate_resource(struct resource *root, struct resource *new,
        resource_size_t size, resource_size_t min,
        resource_size_t max, resource_size_t align,
        resource_size_t (*alignf)(void *,
             const struct resource *,
             resource_size_t,
             resource_size_t),
        void *alignf_data);
struct resource *lookup_resource(struct resource *root, resource_size_t start);
int adjust_resource(struct resource *res, resource_size_t start,
      resource_size_t size);
resource_size_t resource_alignment(struct resource *res);
static inline __attribute__((no_instrument_function)) resource_size_t resource_size(const struct resource *res)
{
 return res->end - res->start + 1;
}
static inline __attribute__((no_instrument_function)) unsigned long resource_type(const struct resource *res)
{
 return res->flags & 0x00001f00;
}

static inline __attribute__((no_instrument_function)) bool resource_contains(struct resource *r1, struct resource *r2)
{
 if (resource_type(r1) != resource_type(r2))
  return false;
 if (r1->flags & 0x20000000 || r2->flags & 0x20000000)
  return false;
 return r1->start <= r2->start && r1->end >= r2->end;
}
# 192 "include/linux/ioport.h"
extern struct resource * __request_region(struct resource *,
     resource_size_t start,
     resource_size_t n,
     const char *name, int flags);






extern int __check_region(struct resource *, resource_size_t, resource_size_t);
extern void __release_region(struct resource *, resource_size_t,
    resource_size_t);

extern int release_mem_region_adjustable(struct resource *, resource_size_t,
    resource_size_t);


static inline __attribute__((no_instrument_function)) int __attribute__((deprecated)) check_region(resource_size_t s,
      resource_size_t n)
{
 return __check_region(&ioport_resource, s, n);
}


struct device;





extern struct resource * __devm_request_region(struct device *dev,
    struct resource *parent, resource_size_t start,
    resource_size_t n, const char *name);






extern void __devm_release_region(struct device *dev, struct resource *parent,
      resource_size_t start, resource_size_t n);
extern int iomem_map_sanity_check(resource_size_t addr, unsigned long size);
extern int iomem_is_exclusive(u64 addr);

extern int
walk_system_ram_range(unsigned long start_pfn, unsigned long nr_pages,
  void *arg, int (*func)(unsigned long, unsigned long, void *));


static inline __attribute__((no_instrument_function)) bool resource_overlaps(struct resource *r1, struct resource *r2)
{
       return (r1->start <= r2->end && r1->end >= r2->start);
}
# 17 "include/linux/device.h" 2
# 1 "include/linux/kobject.h" 1
# 20 "include/linux/kobject.h"
# 1 "include/linux/list.h" 1





# 1 "include/linux/poison.h" 1
# 7 "include/linux/list.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/include/uapi/linux/const.h" 1
# 8 "include/linux/list.h" 2
# 24 "include/linux/list.h"
static inline __attribute__((no_instrument_function)) void INIT_LIST_HEAD(struct list_head *list)
{
 list->next = list;
 list->prev = list;
}
# 47 "include/linux/list.h"
extern void __list_add(struct list_head *new,
         struct list_head *prev,
         struct list_head *next);
# 60 "include/linux/list.h"
static inline __attribute__((no_instrument_function)) void list_add(struct list_head *new, struct list_head *head)
{
 __list_add(new, head, head->next);
}
# 74 "include/linux/list.h"
static inline __attribute__((no_instrument_function)) void list_add_tail(struct list_head *new, struct list_head *head)
{
 __list_add(new, head->prev, head);
}
# 86 "include/linux/list.h"
static inline __attribute__((no_instrument_function)) void __list_del(struct list_head * prev, struct list_head * next)
{
 next->prev = prev;
 prev->next = next;
}
# 111 "include/linux/list.h"
extern void __list_del_entry(struct list_head *entry);
extern void list_del(struct list_head *entry);
# 122 "include/linux/list.h"
static inline __attribute__((no_instrument_function)) void list_replace(struct list_head *old,
    struct list_head *new)
{
 new->next = old->next;
 new->next->prev = new;
 new->prev = old->prev;
 new->prev->next = new;
}

static inline __attribute__((no_instrument_function)) void list_replace_init(struct list_head *old,
     struct list_head *new)
{
 list_replace(old, new);
 INIT_LIST_HEAD(old);
}





static inline __attribute__((no_instrument_function)) void list_del_init(struct list_head *entry)
{
 __list_del_entry(entry);
 INIT_LIST_HEAD(entry);
}






static inline __attribute__((no_instrument_function)) void list_move(struct list_head *list, struct list_head *head)
{
 __list_del_entry(list);
 list_add(list, head);
}






static inline __attribute__((no_instrument_function)) void list_move_tail(struct list_head *list,
      struct list_head *head)
{
 __list_del_entry(list);
 list_add_tail(list, head);
}






static inline __attribute__((no_instrument_function)) int list_is_last(const struct list_head *list,
    const struct list_head *head)
{
 return list->next == head;
}





static inline __attribute__((no_instrument_function)) int list_empty(const struct list_head *head)
{
 return head->next == head;
}
# 204 "include/linux/list.h"
static inline __attribute__((no_instrument_function)) int list_empty_careful(const struct list_head *head)
{
 struct list_head *next = head->next;
 return (next == head) && (next == head->prev);
}





static inline __attribute__((no_instrument_function)) void list_rotate_left(struct list_head *head)
{
 struct list_head *first;

 if (!list_empty(head)) {
  first = head->next;
  list_move_tail(first, head);
 }
}





static inline __attribute__((no_instrument_function)) int list_is_singular(const struct list_head *head)
{
 return !list_empty(head) && (head->next == head->prev);
}

static inline __attribute__((no_instrument_function)) void __list_cut_position(struct list_head *list,
  struct list_head *head, struct list_head *entry)
{
 struct list_head *new_first = entry->next;
 list->next = head->next;
 list->next->prev = list;
 list->prev = entry;
 entry->next = list;
 head->next = new_first;
 new_first->prev = head;
}
# 259 "include/linux/list.h"
static inline __attribute__((no_instrument_function)) void list_cut_position(struct list_head *list,
  struct list_head *head, struct list_head *entry)
{
 if (list_empty(head))
  return;
 if (list_is_singular(head) &&
  (head->next != entry && head != entry))
  return;
 if (entry == head)
  INIT_LIST_HEAD(list);
 else
  __list_cut_position(list, head, entry);
}

static inline __attribute__((no_instrument_function)) void __list_splice(const struct list_head *list,
     struct list_head *prev,
     struct list_head *next)
{
 struct list_head *first = list->next;
 struct list_head *last = list->prev;

 first->prev = prev;
 prev->next = first;

 last->next = next;
 next->prev = last;
}






static inline __attribute__((no_instrument_function)) void list_splice(const struct list_head *list,
    struct list_head *head)
{
 if (!list_empty(list))
  __list_splice(list, head, head->next);
}






static inline __attribute__((no_instrument_function)) void list_splice_tail(struct list_head *list,
    struct list_head *head)
{
 if (!list_empty(list))
  __list_splice(list, head->prev, head);
}
# 318 "include/linux/list.h"
static inline __attribute__((no_instrument_function)) void list_splice_init(struct list_head *list,
        struct list_head *head)
{
 if (!list_empty(list)) {
  __list_splice(list, head, head->next);
  INIT_LIST_HEAD(list);
 }
}
# 335 "include/linux/list.h"
static inline __attribute__((no_instrument_function)) void list_splice_tail_init(struct list_head *list,
      struct list_head *head)
{
 if (!list_empty(list)) {
  __list_splice(list, head->prev, head);
  INIT_LIST_HEAD(list);
 }
}
# 597 "include/linux/list.h"
static inline __attribute__((no_instrument_function)) void INIT_HLIST_NODE(struct hlist_node *h)
{
 h->next = ((void *)0);
 h->pprev = ((void *)0);
}

static inline __attribute__((no_instrument_function)) int hlist_unhashed(const struct hlist_node *h)
{
 return !h->pprev;
}

static inline __attribute__((no_instrument_function)) int hlist_empty(const struct hlist_head *h)
{
 return !h->first;
}

static inline __attribute__((no_instrument_function)) void __hlist_del(struct hlist_node *n)
{
 struct hlist_node *next = n->next;
 struct hlist_node **pprev = n->pprev;
 *pprev = next;
 if (next)
  next->pprev = pprev;
}

static inline __attribute__((no_instrument_function)) void hlist_del(struct hlist_node *n)
{
 __hlist_del(n);
 n->next = ((void *) 0x00100100 + (0xdead000000000000UL));
 n->pprev = ((void *) 0x00200200 + (0xdead000000000000UL));
}

static inline __attribute__((no_instrument_function)) void hlist_del_init(struct hlist_node *n)
{
 if (!hlist_unhashed(n)) {
  __hlist_del(n);
  INIT_HLIST_NODE(n);
 }
}

static inline __attribute__((no_instrument_function)) void hlist_add_head(struct hlist_node *n, struct hlist_head *h)
{
 struct hlist_node *first = h->first;
 n->next = first;
 if (first)
  first->pprev = &n->next;
 h->first = n;
 n->pprev = &h->first;
}


static inline __attribute__((no_instrument_function)) void hlist_add_before(struct hlist_node *n,
     struct hlist_node *next)
{
 n->pprev = next->pprev;
 n->next = next;
 next->pprev = &n->next;
 *(n->pprev) = n;
}

static inline __attribute__((no_instrument_function)) void hlist_add_after(struct hlist_node *n,
     struct hlist_node *next)
{
 next->next = n->next;
 n->next = next;
 next->pprev = &n->next;

 if(next->next)
  next->next->pprev = &next->next;
}


static inline __attribute__((no_instrument_function)) void hlist_add_fake(struct hlist_node *n)
{
 n->pprev = &n->next;
}





static inline __attribute__((no_instrument_function)) void hlist_move_list(struct hlist_head *old,
       struct hlist_head *new)
{
 new->first = old->first;
 if (new->first)
  new->first->pprev = &new->first;
 old->first = ((void *)0);
}
# 21 "include/linux/kobject.h" 2
# 1 "include/linux/sysfs.h" 1
# 15 "include/linux/sysfs.h"
# 1 "include/linux/kernfs.h" 1
# 10 "include/linux/kernfs.h"
# 1 "include/linux/kernel.h" 1




# 1 "/usr/lib/gcc/x86_64-redhat-linux/4.7.2/include/stdarg.h" 1 3 4
# 40 "/usr/lib/gcc/x86_64-redhat-linux/4.7.2/include/stdarg.h" 3 4
typedef __builtin_va_list __gnuc_va_list;
# 102 "/usr/lib/gcc/x86_64-redhat-linux/4.7.2/include/stdarg.h" 3 4
typedef __gnuc_va_list va_list;
# 6 "include/linux/kernel.h" 2
# 1 "include/linux/linkage.h" 1




# 1 "include/linux/stringify.h" 1
# 6 "include/linux/linkage.h" 2
# 1 "include/linux/export.h" 1
# 26 "include/linux/export.h"
struct kernel_symbol
{
 unsigned long value;
 const char *name;
};


extern struct module __this_module;
# 7 "include/linux/linkage.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/linkage.h" 1
# 8 "include/linux/linkage.h" 2
# 7 "include/linux/kernel.h" 2



# 1 "include/linux/bitops.h" 1
# 24 "include/linux/bitops.h"
extern unsigned int __sw_hweight8(unsigned int w);
extern unsigned int __sw_hweight16(unsigned int w);
extern unsigned int __sw_hweight32(unsigned int w);
extern unsigned long __sw_hweight64(__u64 w);





# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h" 1
# 16 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/alternative.h" 1






# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/asm.h" 1
# 8 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/alternative.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/ptrace.h" 1



# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/segment.h" 1
# 148 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/segment.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/cache.h" 1
# 149 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/segment.h" 2
# 216 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/segment.h"
extern const char early_idt_handlers[32][2+2+5];
# 267 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/segment.h"
static inline __attribute__((no_instrument_function)) unsigned long get_limit(unsigned long segment)
{
 unsigned long __limit;
 asm("lsll %1,%0" : "=r" (__limit) : "r" (segment));
 return __limit + 1;
}
# 5 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/ptrace.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/page_types.h" 1
# 42 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/page_types.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/page_64_types.h" 1
# 43 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/page_types.h" 2






extern int devmem_is_allowed(unsigned long pagenr);

extern unsigned long max_low_pfn_mapped;
extern unsigned long max_pfn_mapped;

static inline __attribute__((no_instrument_function)) phys_addr_t get_max_mapped(void)
{
 return (phys_addr_t)max_pfn_mapped << 12;
}

bool pfn_range_is_mapped(unsigned long start_pfn, unsigned long end_pfn);

extern unsigned long init_memory_mapping(unsigned long start,
      unsigned long end);

extern void initmem_init(void);
# 6 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/ptrace.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/ptrace.h" 1




# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/ptrace-abi.h" 1
# 6 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/ptrace.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor-flags.h" 1



# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/processor-flags.h" 1
# 5 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor-flags.h" 2
# 7 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/ptrace.h" 2
# 7 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/ptrace.h" 2
# 33 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/ptrace.h"
struct pt_regs {
 unsigned long r15;
 unsigned long r14;
 unsigned long r13;
 unsigned long r12;
 unsigned long bp;
 unsigned long bx;

 unsigned long r11;
 unsigned long r10;
 unsigned long r9;
 unsigned long r8;
 unsigned long ax;
 unsigned long cx;
 unsigned long dx;
 unsigned long si;
 unsigned long di;
 unsigned long orig_ax;


 unsigned long ip;
 unsigned long cs;
 unsigned long flags;
 unsigned long sp;
 unsigned long ss;

};




# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt_types.h" 1
# 42 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt_types.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/desc_defs.h" 1
# 22 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/desc_defs.h"
struct desc_struct {
 union {
  struct {
   unsigned int a;
   unsigned int b;
  };
  struct {
   u16 limit0;
   u16 base0;
   unsigned base1: 8, type: 4, s: 1, dpl: 2, p: 1;
   unsigned limit: 4, avl: 1, l: 1, d: 1, g: 1, base2: 8;
  };
 };
} __attribute__((packed));







enum {
 GATE_INTERRUPT = 0xE,
 GATE_TRAP = 0xF,
 GATE_CALL = 0xC,
 GATE_TASK = 0x5,
};


struct gate_struct64 {
 u16 offset_low;
 u16 segment;
 unsigned ist : 3, zero0 : 5, type : 5, dpl : 2, p : 1;
 u16 offset_middle;
 u32 offset_high;
 u32 zero1;
} __attribute__((packed));





enum {
 DESC_TSS = 0x9,
 DESC_LDT = 0x2,
 DESCTYPE_S = 0x10,
};


struct ldttss_desc64 {
 u16 limit0;
 u16 base0;
 unsigned base1 : 8, type : 5, dpl : 2, p : 1;
 unsigned limit1 : 4, zero0 : 3, g : 1, base2 : 8;
 u32 base3;
 u32 zero1;
} __attribute__((packed));


typedef struct gate_struct64 gate_desc;
typedef struct ldttss_desc64 ldt_desc;
typedef struct ldttss_desc64 tss_desc;
# 94 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/desc_defs.h"
struct desc_ptr {
 unsigned short size;
 unsigned long address;
} __attribute__((packed)) ;
# 43 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt_types.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/kmap_types.h" 1







# 1 "include/asm-generic/kmap_types.h" 1
# 9 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/kmap_types.h" 2
# 44 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt_types.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/pgtable_types.h" 1
# 225 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/pgtable_types.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/pgtable_64_types.h" 1



# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/sparsemem.h" 1
# 5 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/pgtable_64_types.h" 2







typedef unsigned long pteval_t;
typedef unsigned long pmdval_t;
typedef unsigned long pudval_t;
typedef unsigned long pgdval_t;
typedef unsigned long pgprotval_t;

typedef struct { pteval_t pte; } pte_t;
# 226 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/pgtable_types.h" 2
# 238 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/pgtable_types.h"
typedef struct pgprot { pgprotval_t pgprot; } pgprot_t;

typedef struct { pgdval_t pgd; } pgd_t;

static inline __attribute__((no_instrument_function)) pgd_t native_make_pgd(pgdval_t val)
{
 return (pgd_t) { val };
}

static inline __attribute__((no_instrument_function)) pgdval_t native_pgd_val(pgd_t pgd)
{
 return pgd.pgd;
}

static inline __attribute__((no_instrument_function)) pgdval_t pgd_flags(pgd_t pgd)
{
 return native_pgd_val(pgd) & (~((pteval_t)(((signed long)(~(((1UL) << 12)-1))) & ((phys_addr_t)((1ULL << 46) - 1)))));
}


typedef struct { pudval_t pud; } pud_t;

static inline __attribute__((no_instrument_function)) pud_t native_make_pud(pmdval_t val)
{
 return (pud_t) { val };
}

static inline __attribute__((no_instrument_function)) pudval_t native_pud_val(pud_t pud)
{
 return pud.pud;
}
# 279 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/pgtable_types.h"
typedef struct { pmdval_t pmd; } pmd_t;

static inline __attribute__((no_instrument_function)) pmd_t native_make_pmd(pmdval_t val)
{
 return (pmd_t) { val };
}

static inline __attribute__((no_instrument_function)) pmdval_t native_pmd_val(pmd_t pmd)
{
 return pmd.pmd;
}
# 299 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/pgtable_types.h"
static inline __attribute__((no_instrument_function)) pudval_t pud_flags(pud_t pud)
{
 return native_pud_val(pud) & (~((pteval_t)(((signed long)(~(((1UL) << 12)-1))) & ((phys_addr_t)((1ULL << 46) - 1)))));
}

static inline __attribute__((no_instrument_function)) pmdval_t pmd_flags(pmd_t pmd)
{
 return native_pmd_val(pmd) & (~((pteval_t)(((signed long)(~(((1UL) << 12)-1))) & ((phys_addr_t)((1ULL << 46) - 1)))));
}

static inline __attribute__((no_instrument_function)) pte_t native_make_pte(pteval_t val)
{
 return (pte_t) { .pte = val };
}

static inline __attribute__((no_instrument_function)) pteval_t native_pte_val(pte_t pte)
{
 return pte.pte;
}

static inline __attribute__((no_instrument_function)) pteval_t pte_flags(pte_t pte)
{
 return native_pte_val(pte) & (~((pteval_t)(((signed long)(~(((1UL) << 12)-1))) & ((phys_addr_t)((1ULL << 46) - 1)))));
}





typedef struct page *pgtable_t;

extern pteval_t __supported_pte_mask;
extern void set_nx(void);
extern int nx_enabled;


extern pgprot_t pgprot_writecombine(pgprot_t prot);





struct file;
pgprot_t phys_mem_access_prot(struct file *file, unsigned long pfn,
                              unsigned long size, pgprot_t vma_prot);
int phys_mem_access_prot_allowed(struct file *file, unsigned long pfn,
                              unsigned long size, pgprot_t *vma_prot);


void set_pte_vaddr(unsigned long vaddr, pte_t pte);







struct seq_file;
extern void arch_report_meminfo(struct seq_file *m);

enum pg_level {
 PG_LEVEL_NONE,
 PG_LEVEL_4K,
 PG_LEVEL_2M,
 PG_LEVEL_1G,
 PG_LEVEL_NUM
};


extern void update_page_count(int level, unsigned long pages);
# 379 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/pgtable_types.h"
extern pte_t *lookup_address(unsigned long address, unsigned int *level);
extern pte_t *lookup_address_in_pgd(pgd_t *pgd, unsigned long address,
        unsigned int *level);
extern phys_addr_t slow_virt_to_phys(void *__address);
extern int kernel_map_pages_in_pgd(pgd_t *pgd, u64 pfn, unsigned long address,
       unsigned numpages, unsigned long page_flags);
void kernel_unmap_pages_in_pgd(pgd_t *root, unsigned long address,
          unsigned numpages);
# 45 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt_types.h" 2

struct page;
struct thread_struct;
struct desc_ptr;
struct tss_struct;
struct mm_struct;
struct desc_struct;
struct task_struct;
struct cpumask;





struct paravirt_callee_save {
 void *func;
};


struct pv_info {
 unsigned int kernel_rpl;
 int shared_kernel_pmd;


 u16 extra_user_64bit_cs;


 int paravirt_enabled;
 const char *name;
};

struct pv_init_ops {
# 85 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt_types.h"
 unsigned (*patch)(u8 type, u16 clobber, void *insnbuf,
     unsigned long addr, unsigned len);
};


struct pv_lazy_ops {

 void (*enter)(void);
 void (*leave)(void);
 void (*flush)(void);
};

struct pv_time_ops {
 unsigned long long (*sched_clock)(void);
 unsigned long long (*steal_clock)(int cpu);
 unsigned long (*get_tsc_khz)(void);
};

struct pv_cpu_ops {

 unsigned long (*get_debugreg)(int regno);
 void (*set_debugreg)(int regno, unsigned long value);

 void (*clts)(void);

 unsigned long (*read_cr0)(void);
 void (*write_cr0)(unsigned long);

 unsigned long (*read_cr4_safe)(void);
 unsigned long (*read_cr4)(void);
 void (*write_cr4)(unsigned long);


 unsigned long (*read_cr8)(void);
 void (*write_cr8)(unsigned long);



 void (*load_tr_desc)(void);
 void (*load_gdt)(const struct desc_ptr *);
 void (*load_idt)(const struct desc_ptr *);

 void (*store_idt)(struct desc_ptr *);
 void (*set_ldt)(const void *desc, unsigned entries);
 unsigned long (*store_tr)(void);
 void (*load_tls)(struct thread_struct *t, unsigned int cpu);

 void (*load_gs_index)(unsigned int idx);

 void (*write_ldt_entry)(struct desc_struct *ldt, int entrynum,
    const void *desc);
 void (*write_gdt_entry)(struct desc_struct *,
    int entrynum, const void *desc, int size);
 void (*write_idt_entry)(gate_desc *,
    int entrynum, const gate_desc *gate);
 void (*alloc_ldt)(struct desc_struct *ldt, unsigned entries);
 void (*free_ldt)(struct desc_struct *ldt, unsigned entries);

 void (*load_sp0)(struct tss_struct *tss, struct thread_struct *t);

 void (*set_iopl_mask)(unsigned mask);

 void (*wbinvd)(void);
 void (*io_delay)(void);


 void (*cpuid)(unsigned int *eax, unsigned int *ebx,
        unsigned int *ecx, unsigned int *edx);



 u64 (*read_msr)(unsigned int msr, int *err);
 int (*write_msr)(unsigned int msr, unsigned low, unsigned high);

 u64 (*read_tsc)(void);
 u64 (*read_pmc)(int counter);
 unsigned long long (*read_tscp)(unsigned int *aux);







 void (*irq_enable_sysexit)(void);







 void (*usergs_sysret64)(void);







 void (*usergs_sysret32)(void);



 void (*iret)(void);

 void (*swapgs)(void);

 void (*start_context_switch)(struct task_struct *prev);
 void (*end_context_switch)(struct task_struct *next);
};

struct pv_irq_ops {
# 207 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt_types.h"
 struct paravirt_callee_save save_fl;
 struct paravirt_callee_save restore_fl;
 struct paravirt_callee_save irq_disable;
 struct paravirt_callee_save irq_enable;

 void (*safe_halt)(void);
 void (*halt)(void);


 void (*adjust_exception_frame)(void);

};

struct pv_apic_ops {

 void (*startup_ipi_hook)(int phys_apicid,
     unsigned long start_eip,
     unsigned long start_esp);

};

struct pv_mmu_ops {
 unsigned long (*read_cr2)(void);
 void (*write_cr2)(unsigned long);

 unsigned long (*read_cr3)(void);
 void (*write_cr3)(unsigned long);





 void (*activate_mm)(struct mm_struct *prev,
       struct mm_struct *next);
 void (*dup_mmap)(struct mm_struct *oldmm,
    struct mm_struct *mm);
 void (*exit_mmap)(struct mm_struct *mm);



 void (*flush_tlb_user)(void);
 void (*flush_tlb_kernel)(void);
 void (*flush_tlb_single)(unsigned long addr);
 void (*flush_tlb_others)(const struct cpumask *cpus,
     struct mm_struct *mm,
     unsigned long start,
     unsigned long end);


 int (*pgd_alloc)(struct mm_struct *mm);
 void (*pgd_free)(struct mm_struct *mm, pgd_t *pgd);





 void (*alloc_pte)(struct mm_struct *mm, unsigned long pfn);
 void (*alloc_pmd)(struct mm_struct *mm, unsigned long pfn);
 void (*alloc_pud)(struct mm_struct *mm, unsigned long pfn);
 void (*release_pte)(unsigned long pfn);
 void (*release_pmd)(unsigned long pfn);
 void (*release_pud)(unsigned long pfn);


 void (*set_pte)(pte_t *ptep, pte_t pteval);
 void (*set_pte_at)(struct mm_struct *mm, unsigned long addr,
      pte_t *ptep, pte_t pteval);
 void (*set_pmd)(pmd_t *pmdp, pmd_t pmdval);
 void (*set_pmd_at)(struct mm_struct *mm, unsigned long addr,
      pmd_t *pmdp, pmd_t pmdval);
 void (*pte_update)(struct mm_struct *mm, unsigned long addr,
      pte_t *ptep);
 void (*pte_update_defer)(struct mm_struct *mm,
     unsigned long addr, pte_t *ptep);
 void (*pmd_update)(struct mm_struct *mm, unsigned long addr,
      pmd_t *pmdp);
 void (*pmd_update_defer)(struct mm_struct *mm,
     unsigned long addr, pmd_t *pmdp);

 pte_t (*ptep_modify_prot_start)(struct mm_struct *mm, unsigned long addr,
     pte_t *ptep);
 void (*ptep_modify_prot_commit)(struct mm_struct *mm, unsigned long addr,
     pte_t *ptep, pte_t pte);

 struct paravirt_callee_save pte_val;
 struct paravirt_callee_save make_pte;

 struct paravirt_callee_save pgd_val;
 struct paravirt_callee_save make_pgd;
# 306 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt_types.h"
 void (*set_pud)(pud_t *pudp, pud_t pudval);

 struct paravirt_callee_save pmd_val;
 struct paravirt_callee_save make_pmd;


 struct paravirt_callee_save pud_val;
 struct paravirt_callee_save make_pud;

 void (*set_pgd)(pgd_t *pudp, pgd_t pgdval);



 struct pv_lazy_ops lazy_mode;





 void (*set_fixmap)(unsigned idx,
      phys_addr_t phys, pgprot_t flags);
};

struct arch_spinlock;

# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/spinlock_types.h" 1
# 18 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/spinlock_types.h"
typedef u16 __ticket_t;
typedef u32 __ticketpair_t;






typedef struct arch_spinlock {
 union {
  __ticketpair_t head_tail;
  struct __raw_tickets {
   __ticket_t head, tail;
  } tickets;
 };
} arch_spinlock_t;



# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/rwlock.h" 1
# 27 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/rwlock.h"
typedef union {
 s64 lock;
 struct {
  u32 read;
  s32 write;
 };
} arch_rwlock_t;
# 38 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/spinlock_types.h" 2
# 332 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt_types.h" 2




struct pv_lock_ops {
 struct paravirt_callee_save lock_spinning;
 void (*unlock_kick)(struct arch_spinlock *lock, __ticket_t ticket);
};




struct paravirt_patch_template {
 struct pv_init_ops pv_init_ops;
 struct pv_time_ops pv_time_ops;
 struct pv_cpu_ops pv_cpu_ops;
 struct pv_irq_ops pv_irq_ops;
 struct pv_apic_ops pv_apic_ops;
 struct pv_mmu_ops pv_mmu_ops;
 struct pv_lock_ops pv_lock_ops;
};

extern struct pv_info pv_info;
extern struct pv_init_ops pv_init_ops;
extern struct pv_time_ops pv_time_ops;
extern struct pv_cpu_ops pv_cpu_ops;
extern struct pv_irq_ops pv_irq_ops;
extern struct pv_apic_ops pv_apic_ops;
extern struct pv_mmu_ops pv_mmu_ops;
extern struct pv_lock_ops pv_lock_ops;
# 397 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt_types.h"
unsigned paravirt_patch_nop(void);
unsigned paravirt_patch_ident_32(void *insnbuf, unsigned len);
unsigned paravirt_patch_ident_64(void *insnbuf, unsigned len);
unsigned paravirt_patch_ignore(unsigned len);
unsigned paravirt_patch_call(void *insnbuf,
        const void *target, u16 tgt_clobbers,
        unsigned long addr, u16 site_clobbers,
        unsigned len);
unsigned paravirt_patch_jmp(void *insnbuf, const void *target,
       unsigned long addr, unsigned len);
unsigned paravirt_patch_default(u8 type, u16 clobbers, void *insnbuf,
    unsigned long addr, unsigned len);

unsigned paravirt_patch_insns(void *insnbuf, unsigned len,
         const char *start, const char *end);

unsigned native_patch(u8 type, u16 clobbers, void *ibuf,
        unsigned long addr, unsigned len);

int paravirt_disable_iospace(void);
# 675 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt_types.h"
enum paravirt_lazy_mode {
 PARAVIRT_LAZY_NONE,
 PARAVIRT_LAZY_MMU,
 PARAVIRT_LAZY_CPU,
};

enum paravirt_lazy_mode paravirt_get_lazy_mode(void);
void paravirt_start_context_switch(struct task_struct *prev);
void paravirt_end_context_switch(struct task_struct *next);

void paravirt_enter_lazy_mmu(void);
void paravirt_leave_lazy_mmu(void);
void paravirt_flush_lazy_mmu(void);

void _paravirt_nop(void);
u32 _paravirt_ident_32(u32);
u64 _paravirt_ident_64(u64);




struct paravirt_patch_site {
 u8 *instr;
 u8 instrtype;
 u8 len;
 u16 clobbers;
};

extern struct paravirt_patch_site __parainstructions[],
 __parainstructions_end[];
# 65 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/ptrace.h" 2


struct cpuinfo_x86;
struct task_struct;

extern unsigned long profile_pc(struct pt_regs *regs);


extern unsigned long
convert_ip_to_linear(struct task_struct *child, struct pt_regs *regs);
extern void send_sigtrap(struct task_struct *tsk, struct pt_regs *regs,
    int error_code, int si_code);

extern long syscall_trace_enter(struct pt_regs *);
extern void syscall_trace_leave(struct pt_regs *);

static inline __attribute__((no_instrument_function)) unsigned long regs_return_value(struct pt_regs *regs)
{
 return regs->ax;
}
# 93 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/ptrace.h"
static inline __attribute__((no_instrument_function)) int user_mode(struct pt_regs *regs)
{



 return !!(regs->cs & 3);

}

static inline __attribute__((no_instrument_function)) int user_mode_vm(struct pt_regs *regs)
{




 return user_mode(regs);

}

static inline __attribute__((no_instrument_function)) int v8086_mode(struct pt_regs *regs)
{



 return 0;

}


static inline __attribute__((no_instrument_function)) bool user_64bit_mode(struct pt_regs *regs)
{
# 132 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/ptrace.h"
 return regs->cs == (6*8+3) || regs->cs == pv_info.extra_user_64bit_cs;

}
# 147 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/ptrace.h"
static inline __attribute__((no_instrument_function)) unsigned long kernel_stack_pointer(struct pt_regs *regs)
{
 return regs->sp;
}






# 1 "include/asm-generic/ptrace.h" 1
# 22 "include/asm-generic/ptrace.h"
static inline __attribute__((no_instrument_function)) unsigned long instruction_pointer(struct pt_regs *regs)
{
 return ((regs)->ip);
}
static inline __attribute__((no_instrument_function)) void instruction_pointer_set(struct pt_regs *regs,
                                           unsigned long val)
{
 (((regs)->ip) = (val));
}
# 44 "include/asm-generic/ptrace.h"
static inline __attribute__((no_instrument_function)) unsigned long user_stack_pointer(struct pt_regs *regs)
{
 return ((regs)->sp);
}
static inline __attribute__((no_instrument_function)) void user_stack_pointer_set(struct pt_regs *regs,
                                          unsigned long val)
{
 (((regs)->sp) = (val));
}
# 62 "include/asm-generic/ptrace.h"
static inline __attribute__((no_instrument_function)) unsigned long frame_pointer(struct pt_regs *regs)
{
 return ((regs)->bp);
}
static inline __attribute__((no_instrument_function)) void frame_pointer_set(struct pt_regs *regs,
                                     unsigned long val)
{
 (((regs)->bp) = (val));
}
# 158 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/ptrace.h" 2


extern int regs_query_register_offset(const char *name);
extern const char *regs_query_register_name(unsigned int offset);
# 173 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/ptrace.h"
static inline __attribute__((no_instrument_function)) unsigned long regs_get_register(struct pt_regs *regs,
           unsigned int offset)
{
 if (__builtin_expect(!!(offset > (__builtin_offsetof(struct pt_regs,ss))), 0))
  return 0;
# 187 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/ptrace.h"
 return *(unsigned long *)((unsigned long)regs + offset);
}
# 198 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/ptrace.h"
static inline __attribute__((no_instrument_function)) int regs_within_kernel_stack(struct pt_regs *regs,
        unsigned long addr)
{
 return ((addr & ~((((1UL) << 12) << 2) - 1)) ==
  (kernel_stack_pointer(regs) & ~((((1UL) << 12) << 2) - 1)));
}
# 214 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/ptrace.h"
static inline __attribute__((no_instrument_function)) unsigned long regs_get_kernel_stack_nth(struct pt_regs *regs,
            unsigned int n)
{
 unsigned long *addr = (unsigned long *)kernel_stack_pointer(regs);
 addr += n;
 if (regs_within_kernel_stack(regs, (unsigned long)addr))
  return *addr;
 else
  return 0;
}
# 250 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/ptrace.h"
struct user_desc;
extern int do_get_thread_area(struct task_struct *p, int idx,
         struct user_desc *info);
extern int do_set_thread_area(struct task_struct *p, int idx,
         struct user_desc *info, int can_allocate);
# 9 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/alternative.h" 2
# 46 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/alternative.h"
struct alt_instr {
 s32 instr_offset;
 s32 repl_offset;
 u16 cpuid;
 u8 instrlen;
 u8 replacementlen;
};

extern void alternative_instructions(void);
extern void apply_alternatives(struct alt_instr *start, struct alt_instr *end);

struct module;


extern void alternatives_smp_module_add(struct module *mod, char *name,
     void *locks, void *locks_end,
     void *text, void *text_end);
extern void alternatives_smp_module_del(struct module *mod);
extern void alternatives_enable_smp(void);
extern int alternatives_text_reserved(void *start, void *end);
extern bool skip_smp_alternatives;
# 132 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/alternative.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/cpufeature.h" 1







# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/required-features.h" 1
# 9 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/cpufeature.h" 2
# 243 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/cpufeature.h"
# 1 "include/linux/bitops.h" 1
# 244 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/cpufeature.h" 2

extern const char * const x86_cap_flags[10*32];
extern const char * const x86_power_flags[32];
# 365 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/cpufeature.h"
extern void warn_pre_alternatives(void);
extern bool __static_cpu_has_safe(u16 bit);






static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) __attribute__((pure)) bool __static_cpu_has(u16 bit)
{
# 420 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/cpufeature.h"
  u8 flag;

  asm volatile("1: movb $0,%0\n"
        "2:\n"
        ".section .altinstructions,\"a\"\n"
        " .long 1b - .\n"
        " .long 3f - .\n"
        " .word %P1\n"
        " .byte 2b - 1b\n"
        " .byte 4f - 3f\n"
        ".previous\n"
        ".section .discard,\"aw\",@progbits\n"
        " .byte 0xff + (4f-3f) - (2b-1b)\n"
        ".previous\n"
        ".section .altinstr_replacement,\"ax\"\n"
        "3: movb $1,%0\n"
        "4:\n"
        ".previous\n"
        : "=qm" (flag) : "i" (bit));
  return flag;


}
# 453 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/cpufeature.h"
static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) __attribute__((pure)) bool _static_cpu_has_safe(u16 bit)
{
# 490 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/cpufeature.h"
  u8 flag;

  asm volatile("1: movb $2,%0\n"
        "2:\n"
        ".section .altinstructions,\"a\"\n"
        " .long 1b - .\n"
        " .long 3f - .\n"
        " .word %P2\n"
        " .byte 2b - 1b\n"
        " .byte 4f - 3f\n"
        ".previous\n"
        ".section .discard,\"aw\",@progbits\n"
        " .byte 0xff + (4f-3f) - (2b-1b)\n"
        ".previous\n"
        ".section .altinstr_replacement,\"ax\"\n"
        "3: movb $0,%0\n"
        "4:\n"
        ".previous\n"
        ".section .altinstructions,\"a\"\n"
        " .long 1b - .\n"
        " .long 5f - .\n"
        " .word %P1\n"
        " .byte 4b - 3b\n"
        " .byte 6f - 5f\n"
        ".previous\n"
        ".section .discard,\"aw\",@progbits\n"
        " .byte 0xff + (6f-5f) - (4b-3b)\n"
        ".previous\n"
        ".section .altinstr_replacement,\"ax\"\n"
        "5: movb $1,%0\n"
        "6:\n"
        ".previous\n"
        : "=qm" (flag)
        : "i" (bit), "i" ((3*32+21)));
  return (flag == 2 ? __static_cpu_has_safe(bit) : flag);

}
# 133 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/alternative.h" 2
# 199 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/alternative.h"
struct paravirt_patch_site;

void apply_paravirt(struct paravirt_patch_site *start,
      struct paravirt_patch_site *end);
# 211 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/alternative.h"
extern void *text_poke_early(void *addr, const void *opcode, size_t len);
# 227 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/alternative.h"
extern void *text_poke(void *addr, const void *opcode, size_t len);
extern int poke_int3_handler(struct pt_regs *regs);
extern void *text_poke_bp(void *addr, const void *opcode, size_t len, void *handler);
# 17 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/rmwcc.h" 1
# 18 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h" 2
# 70 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) void
set_bit(long nr, volatile unsigned long *addr)
{
 if ((__builtin_constant_p(nr))) {
  asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "orb %1,%0"
   : "+m" (*(volatile long *) ((void *)(addr) + ((nr)>>3)))
   : "iq" ((u8)(1 << ((nr) & 7)))
   : "memory");
 } else {
  asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "bts %1,%0"
   : "+m" (*(volatile long *) (addr)) : "Ir" (nr) : "memory");
 }
}
# 93 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
static inline __attribute__((no_instrument_function)) void __set_bit(long nr, volatile unsigned long *addr)
{
 asm volatile("bts %1,%0" : "+m" (*(volatile long *) (addr)) : "Ir" (nr) : "memory");
}
# 108 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) void
clear_bit(long nr, volatile unsigned long *addr)
{
 if ((__builtin_constant_p(nr))) {
  asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "andb %1,%0"
   : "+m" (*(volatile long *) ((void *)(addr) + ((nr)>>3)))
   : "iq" ((u8)~(1 << ((nr) & 7))));
 } else {
  asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "btr %1,%0"
   : "+m" (*(volatile long *) (addr))
   : "Ir" (nr));
 }
}
# 130 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
static inline __attribute__((no_instrument_function)) void clear_bit_unlock(long nr, volatile unsigned long *addr)
{
 __asm__ __volatile__("": : :"memory");
 clear_bit(nr, addr);
}

static inline __attribute__((no_instrument_function)) void __clear_bit(long nr, volatile unsigned long *addr)
{
 asm volatile("btr %1,%0" : "+m" (*(volatile long *) (addr)) : "Ir" (nr));
}
# 153 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
static inline __attribute__((no_instrument_function)) void __clear_bit_unlock(long nr, volatile unsigned long *addr)
{
 __asm__ __volatile__("": : :"memory");
 __clear_bit(nr, addr);
}
# 171 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
static inline __attribute__((no_instrument_function)) void __change_bit(long nr, volatile unsigned long *addr)
{
 asm volatile("btc %1,%0" : "+m" (*(volatile long *) (addr)) : "Ir" (nr));
}
# 185 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
static inline __attribute__((no_instrument_function)) void change_bit(long nr, volatile unsigned long *addr)
{
 if ((__builtin_constant_p(nr))) {
  asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "xorb %1,%0"
   : "+m" (*(volatile long *) ((void *)(addr) + ((nr)>>3)))
   : "iq" ((u8)(1 << ((nr) & 7))));
 } else {
  asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "btc %1,%0"
   : "+m" (*(volatile long *) (addr))
   : "Ir" (nr));
 }
}
# 206 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
static inline __attribute__((no_instrument_function)) int test_and_set_bit(long nr, volatile unsigned long *addr)
{
 do { char c; asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "bts" " %2, " "%0" "; set" "c" " %1" : "+m" (*addr), "=qm" (c) : "Ir" (nr) : "memory"); return c != 0; } while (0);
}
# 218 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) int
test_and_set_bit_lock(long nr, volatile unsigned long *addr)
{
 return test_and_set_bit(nr, addr);
}
# 233 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
static inline __attribute__((no_instrument_function)) int __test_and_set_bit(long nr, volatile unsigned long *addr)
{
 int oldbit;

 asm("bts %2,%1\n\t"
     "sbb %0,%0"
     : "=r" (oldbit), "+m" (*(volatile long *) (addr))
     : "Ir" (nr));
 return oldbit;
}
# 252 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
static inline __attribute__((no_instrument_function)) int test_and_clear_bit(long nr, volatile unsigned long *addr)
{
 do { char c; asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "btr" " %2, " "%0" "; set" "c" " %1" : "+m" (*addr), "=qm" (c) : "Ir" (nr) : "memory"); return c != 0; } while (0);
}
# 273 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
static inline __attribute__((no_instrument_function)) int __test_and_clear_bit(long nr, volatile unsigned long *addr)
{
 int oldbit;

 asm volatile("btr %2,%1\n\t"
       "sbb %0,%0"
       : "=r" (oldbit), "+m" (*(volatile long *) (addr))
       : "Ir" (nr));
 return oldbit;
}


static inline __attribute__((no_instrument_function)) int __test_and_change_bit(long nr, volatile unsigned long *addr)
{
 int oldbit;

 asm volatile("btc %2,%1\n\t"
       "sbb %0,%0"
       : "=r" (oldbit), "+m" (*(volatile long *) (addr))
       : "Ir" (nr) : "memory");

 return oldbit;
}
# 305 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
static inline __attribute__((no_instrument_function)) int test_and_change_bit(long nr, volatile unsigned long *addr)
{
 do { char c; asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "btc" " %2, " "%0" "; set" "c" " %1" : "+m" (*addr), "=qm" (c) : "Ir" (nr) : "memory"); return c != 0; } while (0);
}

static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) int constant_test_bit(long nr, const volatile unsigned long *addr)
{
 return ((1UL << (nr & (64 -1))) &
  (addr[nr >> 6])) != 0;
}

static inline __attribute__((no_instrument_function)) int variable_test_bit(long nr, volatile const unsigned long *addr)
{
 int oldbit;

 asm volatile("bt %2,%1\n\t"
       "sbb %0,%0"
       : "=r" (oldbit)
       : "m" (*(unsigned long *)addr), "Ir" (nr));

 return oldbit;
}
# 348 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
static inline __attribute__((no_instrument_function)) unsigned long __ffs(unsigned long word)
{
 asm("rep; bsf %1,%0"
  : "=r" (word)
  : "rm" (word));
 return word;
}







static inline __attribute__((no_instrument_function)) unsigned long ffz(unsigned long word)
{
 asm("rep; bsf %1,%0"
  : "=r" (word)
  : "r" (~word));
 return word;
}







static inline __attribute__((no_instrument_function)) unsigned long __fls(unsigned long word)
{
 asm("bsr %1,%0"
     : "=r" (word)
     : "rm" (word));
 return word;
}
# 398 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
static inline __attribute__((no_instrument_function)) int ffs(int x)
{
 int r;
# 412 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
 asm("bsfl %1,%0"
     : "=r" (r)
     : "rm" (x), "0" (-1));
# 425 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
 return r + 1;
}
# 439 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
static inline __attribute__((no_instrument_function)) int fls(int x)
{
 int r;
# 453 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
 asm("bsrl %1,%0"
     : "=r" (r)
     : "rm" (x), "0" (-1));
# 466 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
 return r + 1;
}
# 481 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h"
static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) int fls64(__u64 x)
{
 int bitpos = -1;





 asm("bsrq %1,%q0"
     : "+r" (bitpos)
     : "rm" (x));
 return bitpos + 1;
}




# 1 "include/asm-generic/bitops/find.h" 1
# 14 "include/asm-generic/bitops/find.h"
extern unsigned long find_next_bit(const unsigned long *addr, unsigned long
  size, unsigned long offset);
# 28 "include/asm-generic/bitops/find.h"
extern unsigned long find_next_zero_bit(const unsigned long *addr, unsigned
  long size, unsigned long offset);
# 42 "include/asm-generic/bitops/find.h"
extern unsigned long find_first_bit(const unsigned long *addr,
        unsigned long size);
# 53 "include/asm-generic/bitops/find.h"
extern unsigned long find_first_zero_bit(const unsigned long *addr,
      unsigned long size);
# 499 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h" 2

# 1 "include/asm-generic/bitops/sched.h" 1
# 12 "include/asm-generic/bitops/sched.h"
static inline __attribute__((no_instrument_function)) int sched_find_first_bit(const unsigned long *b)
{

 if (b[0])
  return __ffs(b[0]);
 return __ffs(b[1]) + 64;
# 29 "include/asm-generic/bitops/sched.h"
}
# 501 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h" 2



# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/arch_hweight.h" 1
# 24 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/arch_hweight.h"
static inline __attribute__((no_instrument_function)) unsigned int __arch_hweight32(unsigned int w)
{
 unsigned int res = 0;

 asm ("661:\n\t" "call __sw_hweight32" "\n662:\n" ".pushsection .altinstructions,\"a\"\n" " .long 661b - .\n" " .long " "663""1""f - .\n" " .word " "(4*32+23)" "\n" " .byte " "662b-661b" "\n" " .byte " "664""1""f-""663""1""f" "\n" ".popsection\n" ".pushsection .discard,\"aw\",@progbits\n" " .byte 0xff + (" "664""1""f-""663""1""f" ") - (" "662b-661b" ")\n" ".popsection\n" ".pushsection .altinstr_replacement, \"ax\"\n" "663""1"":\n\t" ".byte 0xf3,0x40,0x0f,0xb8,0xc7" "\n" "664""1" ":\n\t" ".popsection"
       : "=""a" (res)
       : "D" (w));

 return res;
}

static inline __attribute__((no_instrument_function)) unsigned int __arch_hweight16(unsigned int w)
{
 return __arch_hweight32(w & 0xffff);
}

static inline __attribute__((no_instrument_function)) unsigned int __arch_hweight8(unsigned int w)
{
 return __arch_hweight32(w & 0xff);
}

static inline __attribute__((no_instrument_function)) unsigned long __arch_hweight64(__u64 w)
{
 unsigned long res = 0;





 asm ("661:\n\t" "call __sw_hweight64" "\n662:\n" ".pushsection .altinstructions,\"a\"\n" " .long 661b - .\n" " .long " "663""1""f - .\n" " .word " "(4*32+23)" "\n" " .byte " "662b-661b" "\n" " .byte " "664""1""f-""663""1""f" "\n" ".popsection\n" ".pushsection .discard,\"aw\",@progbits\n" " .byte 0xff + (" "664""1""f-""663""1""f" ") - (" "662b-661b" ")\n" ".popsection\n" ".pushsection .altinstr_replacement, \"ax\"\n" "663""1"":\n\t" ".byte 0xf3,0x48,0x0f,0xb8,0xc7" "\n" "664""1" ":\n\t" ".popsection"
       : "=""a" (res)
       : "D" (w));


 return res;
}
# 505 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h" 2

# 1 "include/asm-generic/bitops/const_hweight.h" 1
# 507 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h" 2

# 1 "include/asm-generic/bitops/le.h" 1




# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/byteorder.h" 1



# 1 "include/linux/byteorder/little_endian.h" 1



# 1 "include/uapi/linux/byteorder/little_endian.h" 1
# 12 "include/uapi/linux/byteorder/little_endian.h"
# 1 "include/linux/swab.h" 1



# 1 "include/uapi/linux/swab.h" 1





# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/swab.h" 1






static inline __attribute__((no_instrument_function)) __attribute__((__const__)) __u32 __arch_swab32(__u32 val)
{
 asm("bswapl %0" : "=r" (val) : "0" (val));
 return val;
}


static inline __attribute__((no_instrument_function)) __attribute__((__const__)) __u64 __arch_swab64(__u64 val)
{
# 30 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/swab.h"
 asm("bswapq %0" : "=r" (val) : "0" (val));
 return val;

}
# 7 "include/uapi/linux/swab.h" 2
# 46 "include/uapi/linux/swab.h"
static inline __attribute__((no_instrument_function)) __attribute__((__const__)) __u16 __fswab16(__u16 val)
{





 return ((__u16)( (((__u16)(val) & (__u16)0x00ffU) << 8) | (((__u16)(val) & (__u16)0xff00U) >> 8)));

}

static inline __attribute__((no_instrument_function)) __attribute__((__const__)) __u32 __fswab32(__u32 val)
{



 return __arch_swab32(val);



}

static inline __attribute__((no_instrument_function)) __attribute__((__const__)) __u64 __fswab64(__u64 val)
{



 return __arch_swab64(val);







}

static inline __attribute__((no_instrument_function)) __attribute__((__const__)) __u32 __fswahw32(__u32 val)
{



 return ((__u32)( (((__u32)(val) & (__u32)0x0000ffffUL) << 16) | (((__u32)(val) & (__u32)0xffff0000UL) >> 16)));

}

static inline __attribute__((no_instrument_function)) __attribute__((__const__)) __u32 __fswahb32(__u32 val)
{



 return ((__u32)( (((__u32)(val) & (__u32)0x00ff00ffUL) << 8) | (((__u32)(val) & (__u32)0xff00ff00UL) >> 8)));

}
# 154 "include/uapi/linux/swab.h"
static inline __attribute__((no_instrument_function)) __u16 __swab16p(const __u16 *p)
{



 return (__builtin_constant_p((__u16)(*p)) ? ((__u16)( (((__u16)(*p) & (__u16)0x00ffU) << 8) | (((__u16)(*p) & (__u16)0xff00U) >> 8))) : __fswab16(*p));

}





static inline __attribute__((no_instrument_function)) __u32 __swab32p(const __u32 *p)
{



 return (__builtin_constant_p((__u32)(*p)) ? ((__u32)( (((__u32)(*p) & (__u32)0x000000ffUL) << 24) | (((__u32)(*p) & (__u32)0x0000ff00UL) << 8) | (((__u32)(*p) & (__u32)0x00ff0000UL) >> 8) | (((__u32)(*p) & (__u32)0xff000000UL) >> 24))) : __fswab32(*p));

}





static inline __attribute__((no_instrument_function)) __u64 __swab64p(const __u64 *p)
{



 return (__builtin_constant_p((__u64)(*p)) ? ((__u64)( (((__u64)(*p) & (__u64)0x00000000000000ffULL) << 56) | (((__u64)(*p) & (__u64)0x000000000000ff00ULL) << 40) | (((__u64)(*p) & (__u64)0x0000000000ff0000ULL) << 24) | (((__u64)(*p) & (__u64)0x00000000ff000000ULL) << 8) | (((__u64)(*p) & (__u64)0x000000ff00000000ULL) >> 8) | (((__u64)(*p) & (__u64)0x0000ff0000000000ULL) >> 24) | (((__u64)(*p) & (__u64)0x00ff000000000000ULL) >> 40) | (((__u64)(*p) & (__u64)0xff00000000000000ULL) >> 56))) : __fswab64(*p));

}







static inline __attribute__((no_instrument_function)) __u32 __swahw32p(const __u32 *p)
{



 return (__builtin_constant_p((__u32)(*p)) ? ((__u32)( (((__u32)(*p) & (__u32)0x0000ffffUL) << 16) | (((__u32)(*p) & (__u32)0xffff0000UL) >> 16))) : __fswahw32(*p));

}







static inline __attribute__((no_instrument_function)) __u32 __swahb32p(const __u32 *p)
{



 return (__builtin_constant_p((__u32)(*p)) ? ((__u32)( (((__u32)(*p) & (__u32)0x00ff00ffUL) << 8) | (((__u32)(*p) & (__u32)0xff00ff00UL) >> 8))) : __fswahb32(*p));

}





static inline __attribute__((no_instrument_function)) void __swab16s(__u16 *p)
{



 *p = __swab16p(p);

}




static inline __attribute__((no_instrument_function)) void __swab32s(__u32 *p)
{



 *p = __swab32p(p);

}





static inline __attribute__((no_instrument_function)) void __swab64s(__u64 *p)
{



 *p = __swab64p(p);

}







static inline __attribute__((no_instrument_function)) void __swahw32s(__u32 *p)
{



 *p = __swahw32p(p);

}







static inline __attribute__((no_instrument_function)) void __swahb32s(__u32 *p)
{



 *p = __swahb32p(p);

}
# 5 "include/linux/swab.h" 2
# 13 "include/uapi/linux/byteorder/little_endian.h" 2
# 43 "include/uapi/linux/byteorder/little_endian.h"
static inline __attribute__((no_instrument_function)) __le64 __cpu_to_le64p(const __u64 *p)
{
 return ( __le64)*p;
}
static inline __attribute__((no_instrument_function)) __u64 __le64_to_cpup(const __le64 *p)
{
 return ( __u64)*p;
}
static inline __attribute__((no_instrument_function)) __le32 __cpu_to_le32p(const __u32 *p)
{
 return ( __le32)*p;
}
static inline __attribute__((no_instrument_function)) __u32 __le32_to_cpup(const __le32 *p)
{
 return ( __u32)*p;
}
static inline __attribute__((no_instrument_function)) __le16 __cpu_to_le16p(const __u16 *p)
{
 return ( __le16)*p;
}
static inline __attribute__((no_instrument_function)) __u16 __le16_to_cpup(const __le16 *p)
{
 return ( __u16)*p;
}
static inline __attribute__((no_instrument_function)) __be64 __cpu_to_be64p(const __u64 *p)
{
 return ( __be64)__swab64p(p);
}
static inline __attribute__((no_instrument_function)) __u64 __be64_to_cpup(const __be64 *p)
{
 return __swab64p((__u64 *)p);
}
static inline __attribute__((no_instrument_function)) __be32 __cpu_to_be32p(const __u32 *p)
{
 return ( __be32)__swab32p(p);
}
static inline __attribute__((no_instrument_function)) __u32 __be32_to_cpup(const __be32 *p)
{
 return __swab32p((__u32 *)p);
}
static inline __attribute__((no_instrument_function)) __be16 __cpu_to_be16p(const __u16 *p)
{
 return ( __be16)__swab16p(p);
}
static inline __attribute__((no_instrument_function)) __u16 __be16_to_cpup(const __be16 *p)
{
 return __swab16p((__u16 *)p);
}
# 5 "include/linux/byteorder/little_endian.h" 2

# 1 "include/linux/byteorder/generic.h" 1
# 143 "include/linux/byteorder/generic.h"
static inline __attribute__((no_instrument_function)) void le16_add_cpu(__le16 *var, u16 val)
{
 *var = (( __le16)(__u16)((( __u16)(__le16)(*var)) + val));
}

static inline __attribute__((no_instrument_function)) void le32_add_cpu(__le32 *var, u32 val)
{
 *var = (( __le32)(__u32)((( __u32)(__le32)(*var)) + val));
}

static inline __attribute__((no_instrument_function)) void le64_add_cpu(__le64 *var, u64 val)
{
 *var = (( __le64)(__u64)((( __u64)(__le64)(*var)) + val));
}

static inline __attribute__((no_instrument_function)) void be16_add_cpu(__be16 *var, u16 val)
{
 *var = (( __be16)(__builtin_constant_p((__u16)(((__builtin_constant_p((__u16)(( __u16)(__be16)(*var))) ? ((__u16)( (((__u16)(( __u16)(__be16)(*var)) & (__u16)0x00ffU) << 8) | (((__u16)(( __u16)(__be16)(*var)) & (__u16)0xff00U) >> 8))) : __fswab16(( __u16)(__be16)(*var))) + val))) ? ((__u16)( (((__u16)(((__builtin_constant_p((__u16)(( __u16)(__be16)(*var))) ? ((__u16)( (((__u16)(( __u16)(__be16)(*var)) & (__u16)0x00ffU) << 8) | (((__u16)(( __u16)(__be16)(*var)) & (__u16)0xff00U) >> 8))) : __fswab16(( __u16)(__be16)(*var))) + val)) & (__u16)0x00ffU) << 8) | (((__u16)(((__builtin_constant_p((__u16)(( __u16)(__be16)(*var))) ? ((__u16)( (((__u16)(( __u16)(__be16)(*var)) & (__u16)0x00ffU) << 8) | (((__u16)(( __u16)(__be16)(*var)) & (__u16)0xff00U) >> 8))) : __fswab16(( __u16)(__be16)(*var))) + val)) & (__u16)0xff00U) >> 8))) : __fswab16(((__builtin_constant_p((__u16)(( __u16)(__be16)(*var))) ? ((__u16)( (((__u16)(( __u16)(__be16)(*var)) & (__u16)0x00ffU) << 8) | (((__u16)(( __u16)(__be16)(*var)) & (__u16)0xff00U) >> 8))) : __fswab16(( __u16)(__be16)(*var))) + val))));
}

static inline __attribute__((no_instrument_function)) void be32_add_cpu(__be32 *var, u32 val)
{
 *var = (( __be32)(__builtin_constant_p((__u32)(((__builtin_constant_p((__u32)(( __u32)(__be32)(*var))) ? ((__u32)( (((__u32)(( __u32)(__be32)(*var)) & (__u32)0x000000ffUL) << 24) | (((__u32)(( __u32)(__be32)(*var)) & (__u32)0x0000ff00UL) << 8) | (((__u32)(( __u32)(__be32)(*var)) & (__u32)0x00ff0000UL) >> 8) | (((__u32)(( __u32)(__be32)(*var)) & (__u32)0xff000000UL) >> 24))) : __fswab32(( __u32)(__be32)(*var))) + val))) ? ((__u32)( (((__u32)(((__builtin_constant_p((__u32)(( __u32)(__be32)(*var))) ? ((__u32)( (((__u32)(( __u32)(__be32)(*var)) & (__u32)0x000000ffUL) << 24) | (((__u32)(( __u32)(__be32)(*var)) & (__u32)0x0000ff00UL) << 8) | (((__u32)(( __u32)(__be32)(*var)) & (__u32)0x00ff0000UL) >> 8) | (((__u32)(( __u32)(__be32)(*var)) & (__u32)0xff000000UL) >> 24))) : __fswab32(( __u32)(__be32)(*var))) + val)) & (__u32)0x000000ffUL) << 24) | (((__u32)(((__builtin_constant_p((__u32)(( __u32)(__be32)(*var))) ? ((__u32)( (((__u32)(( __u32)(__be32)(*var)) & (__u32)0x000000ffUL) << 24) | (((__u32)(( __u32)(__be32)(*var)) & (__u32)0x0000ff00UL) << 8) | (((__u32)(( __u32)(__be32)(*var)) & (__u32)0x00ff0000UL) >> 8) | (((__u32)(( __u32)(__be32)(*var)) & (__u32)0xff000000UL) >> 24))) : __fswab32(( __u32)(__be32)(*var))) + val)) & (__u32)0x0000ff00UL) << 8) | (((__u32)(((__builtin_constant_p((__u32)(( __u32)(__be32)(*var))) ? ((__u32)( (((__u32)(( __u32)(__be32)(*var)) & (__u32)0x000000ffUL) << 24) | (((__u32)(( __u32)(__be32)(*var)) & (__u32)0x0000ff00UL) << 8) | (((__u32)(( __u32)(__be32)(*var)) & (__u32)0x00ff0000UL) >> 8) | (((__u32)(( __u32)(__be32)(*var)) & (__u32)0xff000000UL) >> 24))) : __fswab32(( __u32)(__be32)(*var))) + val)) & (__u32)0x00ff0000UL) >> 8) | (((__u32)(((__builtin_constant_p((__u32)(( __u32)(__be32)(*var))) ? ((__u32)( (((__u32)(( __u32)(__be32)(*var)) & (__u32)0x000000ffUL) << 24) | (((__u32)(( __u32)(__be32)(*var)) & (__u32)0x0000ff00UL) << 8) | (((__u32)(( __u32)(__be32)(*var)) & (__u32)0x00ff0000UL) >> 8) | (((__u32)(( __u32)(__be32)(*var)) & (__u32)0xff000000UL) >> 24))) : __fswab32(( __u32)(__be32)(*var))) + val)) & (__u32)0xff000000UL) >> 24))) : __fswab32(((__builtin_constant_p((__u32)(( __u32)(__be32)(*var))) ? ((__u32)( (((__u32)(( __u32)(__be32)(*var)) & (__u32)0x000000ffUL) << 24) | (((__u32)(( __u32)(__be32)(*var)) & (__u32)0x0000ff00UL) << 8) | (((__u32)(( __u32)(__be32)(*var)) & (__u32)0x00ff0000UL) >> 8) | (((__u32)(( __u32)(__be32)(*var)) & (__u32)0xff000000UL) >> 24))) : __fswab32(( __u32)(__be32)(*var))) + val))));
}

static inline __attribute__((no_instrument_function)) void be64_add_cpu(__be64 *var, u64 val)
{
 *var = (( __be64)(__builtin_constant_p((__u64)(((__builtin_constant_p((__u64)(( __u64)(__be64)(*var))) ? ((__u64)( (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00000000000000ffULL) << 56) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x000000000000ff00ULL) << 40) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x0000000000ff0000ULL) << 24) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00000000ff000000ULL) << 8) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x000000ff00000000ULL) >> 8) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x0000ff0000000000ULL) >> 24) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00ff000000000000ULL) >> 40) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0xff00000000000000ULL) >> 56))) : __fswab64(( __u64)(__be64)(*var))) + val))) ? ((__u64)( (((__u64)(((__builtin_constant_p((__u64)(( __u64)(__be64)(*var))) ? ((__u64)( (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00000000000000ffULL) << 56) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x000000000000ff00ULL) << 40) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x0000000000ff0000ULL) << 24) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00000000ff000000ULL) << 8) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x000000ff00000000ULL) >> 8) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x0000ff0000000000ULL) >> 24) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00ff000000000000ULL) >> 40) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0xff00000000000000ULL) >> 56))) : __fswab64(( __u64)(__be64)(*var))) + val)) & (__u64)0x00000000000000ffULL) << 56) | (((__u64)(((__builtin_constant_p((__u64)(( __u64)(__be64)(*var))) ? ((__u64)( (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00000000000000ffULL) << 56) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x000000000000ff00ULL) << 40) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x0000000000ff0000ULL) << 24) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00000000ff000000ULL) << 8) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x000000ff00000000ULL) >> 8) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x0000ff0000000000ULL) >> 24) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00ff000000000000ULL) >> 40) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0xff00000000000000ULL) >> 56))) : __fswab64(( __u64)(__be64)(*var))) + val)) & (__u64)0x000000000000ff00ULL) << 40) | (((__u64)(((__builtin_constant_p((__u64)(( __u64)(__be64)(*var))) ? ((__u64)( (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00000000000000ffULL) << 56) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x000000000000ff00ULL) << 40) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x0000000000ff0000ULL) << 24) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00000000ff000000ULL) << 8) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x000000ff00000000ULL) >> 8) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x0000ff0000000000ULL) >> 24) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00ff000000000000ULL) >> 40) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0xff00000000000000ULL) >> 56))) : __fswab64(( __u64)(__be64)(*var))) + val)) & (__u64)0x0000000000ff0000ULL) << 24) | (((__u64)(((__builtin_constant_p((__u64)(( __u64)(__be64)(*var))) ? ((__u64)( (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00000000000000ffULL) << 56) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x000000000000ff00ULL) << 40) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x0000000000ff0000ULL) << 24) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00000000ff000000ULL) << 8) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x000000ff00000000ULL) >> 8) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x0000ff0000000000ULL) >> 24) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00ff000000000000ULL) >> 40) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0xff00000000000000ULL) >> 56))) : __fswab64(( __u64)(__be64)(*var))) + val)) & (__u64)0x00000000ff000000ULL) << 8) | (((__u64)(((__builtin_constant_p((__u64)(( __u64)(__be64)(*var))) ? ((__u64)( (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00000000000000ffULL) << 56) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x000000000000ff00ULL) << 40) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x0000000000ff0000ULL) << 24) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00000000ff000000ULL) << 8) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x000000ff00000000ULL) >> 8) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x0000ff0000000000ULL) >> 24) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00ff000000000000ULL) >> 40) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0xff00000000000000ULL) >> 56))) : __fswab64(( __u64)(__be64)(*var))) + val)) & (__u64)0x000000ff00000000ULL) >> 8) | (((__u64)(((__builtin_constant_p((__u64)(( __u64)(__be64)(*var))) ? ((__u64)( (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00000000000000ffULL) << 56) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x000000000000ff00ULL) << 40) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x0000000000ff0000ULL) << 24) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00000000ff000000ULL) << 8) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x000000ff00000000ULL) >> 8) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x0000ff0000000000ULL) >> 24) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00ff000000000000ULL) >> 40) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0xff00000000000000ULL) >> 56))) : __fswab64(( __u64)(__be64)(*var))) + val)) & (__u64)0x0000ff0000000000ULL) >> 24) | (((__u64)(((__builtin_constant_p((__u64)(( __u64)(__be64)(*var))) ? ((__u64)( (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00000000000000ffULL) << 56) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x000000000000ff00ULL) << 40) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x0000000000ff0000ULL) << 24) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00000000ff000000ULL) << 8) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x000000ff00000000ULL) >> 8) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x0000ff0000000000ULL) >> 24) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00ff000000000000ULL) >> 40) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0xff00000000000000ULL) >> 56))) : __fswab64(( __u64)(__be64)(*var))) + val)) & (__u64)0x00ff000000000000ULL) >> 40) | (((__u64)(((__builtin_constant_p((__u64)(( __u64)(__be64)(*var))) ? ((__u64)( (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00000000000000ffULL) << 56) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x000000000000ff00ULL) << 40) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x0000000000ff0000ULL) << 24) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00000000ff000000ULL) << 8) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x000000ff00000000ULL) >> 8) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x0000ff0000000000ULL) >> 24) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00ff000000000000ULL) >> 40) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0xff00000000000000ULL) >> 56))) : __fswab64(( __u64)(__be64)(*var))) + val)) & (__u64)0xff00000000000000ULL) >> 56))) : __fswab64(((__builtin_constant_p((__u64)(( __u64)(__be64)(*var))) ? ((__u64)( (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00000000000000ffULL) << 56) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x000000000000ff00ULL) << 40) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x0000000000ff0000ULL) << 24) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00000000ff000000ULL) << 8) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x000000ff00000000ULL) >> 8) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x0000ff0000000000ULL) >> 24) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0x00ff000000000000ULL) >> 40) | (((__u64)(( __u64)(__be64)(*var)) & (__u64)0xff00000000000000ULL) >> 56))) : __fswab64(( __u64)(__be64)(*var))) + val))));
}
# 7 "include/linux/byteorder/little_endian.h" 2
# 5 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/byteorder.h" 2
# 6 "include/asm-generic/bitops/le.h" 2





static inline __attribute__((no_instrument_function)) unsigned long find_next_zero_bit_le(const void *addr,
  unsigned long size, unsigned long offset)
{
 return find_next_zero_bit(addr, size, offset);
}

static inline __attribute__((no_instrument_function)) unsigned long find_next_bit_le(const void *addr,
  unsigned long size, unsigned long offset)
{
 return find_next_bit(addr, size, offset);
}

static inline __attribute__((no_instrument_function)) unsigned long find_first_zero_bit_le(const void *addr,
  unsigned long size)
{
 return find_first_zero_bit(addr, size);
}
# 52 "include/asm-generic/bitops/le.h"
static inline __attribute__((no_instrument_function)) int test_bit_le(int nr, const void *addr)
{
 return (__builtin_constant_p((nr ^ 0)) ? constant_test_bit((nr ^ 0), (addr)) : variable_test_bit((nr ^ 0), (addr)));
}

static inline __attribute__((no_instrument_function)) void set_bit_le(int nr, void *addr)
{
 set_bit(nr ^ 0, addr);
}

static inline __attribute__((no_instrument_function)) void clear_bit_le(int nr, void *addr)
{
 clear_bit(nr ^ 0, addr);
}

static inline __attribute__((no_instrument_function)) void __set_bit_le(int nr, void *addr)
{
 __set_bit(nr ^ 0, addr);
}

static inline __attribute__((no_instrument_function)) void __clear_bit_le(int nr, void *addr)
{
 __clear_bit(nr ^ 0, addr);
}

static inline __attribute__((no_instrument_function)) int test_and_set_bit_le(int nr, void *addr)
{
 return test_and_set_bit(nr ^ 0, addr);
}

static inline __attribute__((no_instrument_function)) int test_and_clear_bit_le(int nr, void *addr)
{
 return test_and_clear_bit(nr ^ 0, addr);
}

static inline __attribute__((no_instrument_function)) int __test_and_set_bit_le(int nr, void *addr)
{
 return __test_and_set_bit(nr ^ 0, addr);
}

static inline __attribute__((no_instrument_function)) int __test_and_clear_bit_le(int nr, void *addr)
{
 return __test_and_clear_bit(nr ^ 0, addr);
}
# 509 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h" 2

# 1 "include/asm-generic/bitops/ext2-atomic-setbit.h" 1
# 511 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bitops.h" 2
# 34 "include/linux/bitops.h" 2
# 57 "include/linux/bitops.h"
static __inline__ __attribute__((no_instrument_function)) int get_bitmask_order(unsigned int count)
{
 int order;

 order = fls(count);
 return order;
}

static __inline__ __attribute__((no_instrument_function)) int get_count_order(unsigned int count)
{
 int order;

 order = fls(count) - 1;
 if (count & (count - 1))
  order++;
 return order;
}

static inline __attribute__((no_instrument_function)) unsigned long hweight_long(unsigned long w)
{
 return sizeof(w) == 4 ? (__builtin_constant_p(w) ? ((((unsigned int) ((!!((w) & (1ULL << 0))) + (!!((w) & (1ULL << 1))) + (!!((w) & (1ULL << 2))) + (!!((w) & (1ULL << 3))) + (!!((w) & (1ULL << 4))) + (!!((w) & (1ULL << 5))) + (!!((w) & (1ULL << 6))) + (!!((w) & (1ULL << 7))))) + ((unsigned int) ((!!(((w) >> 8) & (1ULL << 0))) + (!!(((w) >> 8) & (1ULL << 1))) + (!!(((w) >> 8) & (1ULL << 2))) + (!!(((w) >> 8) & (1ULL << 3))) + (!!(((w) >> 8) & (1ULL << 4))) + (!!(((w) >> 8) & (1ULL << 5))) + (!!(((w) >> 8) & (1ULL << 6))) + (!!(((w) >> 8) & (1ULL << 7)))))) + (((unsigned int) ((!!(((w) >> 16) & (1ULL << 0))) + (!!(((w) >> 16) & (1ULL << 1))) + (!!(((w) >> 16) & (1ULL << 2))) + (!!(((w) >> 16) & (1ULL << 3))) + (!!(((w) >> 16) & (1ULL << 4))) + (!!(((w) >> 16) & (1ULL << 5))) + (!!(((w) >> 16) & (1ULL << 6))) + (!!(((w) >> 16) & (1ULL << 7))))) + ((unsigned int) ((!!((((w) >> 16) >> 8) & (1ULL << 0))) + (!!((((w) >> 16) >> 8) & (1ULL << 1))) + (!!((((w) >> 16) >> 8) & (1ULL << 2))) + (!!((((w) >> 16) >> 8) & (1ULL << 3))) + (!!((((w) >> 16) >> 8) & (1ULL << 4))) + (!!((((w) >> 16) >> 8) & (1ULL << 5))) + (!!((((w) >> 16) >> 8) & (1ULL << 6))) + (!!((((w) >> 16) >> 8) & (1ULL << 7))))))) : __arch_hweight32(w)) : (__builtin_constant_p(w) ? (((((unsigned int) ((!!((w) & (1ULL << 0))) + (!!((w) & (1ULL << 1))) + (!!((w) & (1ULL << 2))) + (!!((w) & (1ULL << 3))) + (!!((w) & (1ULL << 4))) + (!!((w) & (1ULL << 5))) + (!!((w) & (1ULL << 6))) + (!!((w) & (1ULL << 7))))) + ((unsigned int) ((!!(((w) >> 8) & (1ULL << 0))) + (!!(((w) >> 8) & (1ULL << 1))) + (!!(((w) >> 8) & (1ULL << 2))) + (!!(((w) >> 8) & (1ULL << 3))) + (!!(((w) >> 8) & (1ULL << 4))) + (!!(((w) >> 8) & (1ULL << 5))) + (!!(((w) >> 8) & (1ULL << 6))) + (!!(((w) >> 8) & (1ULL << 7)))))) + (((unsigned int) ((!!(((w) >> 16) & (1ULL << 0))) + (!!(((w) >> 16) & (1ULL << 1))) + (!!(((w) >> 16) & (1ULL << 2))) + (!!(((w) >> 16) & (1ULL << 3))) + (!!(((w) >> 16) & (1ULL << 4))) + (!!(((w) >> 16) & (1ULL << 5))) + (!!(((w) >> 16) & (1ULL << 6))) + (!!(((w) >> 16) & (1ULL << 7))))) + ((unsigned int) ((!!((((w) >> 16) >> 8) & (1ULL << 0))) + (!!((((w) >> 16) >> 8) & (1ULL << 1))) + (!!((((w) >> 16) >> 8) & (1ULL << 2))) + (!!((((w) >> 16) >> 8) & (1ULL << 3))) + (!!((((w) >> 16) >> 8) & (1ULL << 4))) + (!!((((w) >> 16) >> 8) & (1ULL << 5))) + (!!((((w) >> 16) >> 8) & (1ULL << 6))) + (!!((((w) >> 16) >> 8) & (1ULL << 7))))))) + ((((unsigned int) ((!!(((w) >> 32) & (1ULL << 0))) + (!!(((w) >> 32) & (1ULL << 1))) + (!!(((w) >> 32) & (1ULL << 2))) + (!!(((w) >> 32) & (1ULL << 3))) + (!!(((w) >> 32) & (1ULL << 4))) + (!!(((w) >> 32) & (1ULL << 5))) + (!!(((w) >> 32) & (1ULL << 6))) + (!!(((w) >> 32) & (1ULL << 7))))) + ((unsigned int) ((!!((((w) >> 32) >> 8) & (1ULL << 0))) + (!!((((w) >> 32) >> 8) & (1ULL << 1))) + (!!((((w) >> 32) >> 8) & (1ULL << 2))) + (!!((((w) >> 32) >> 8) & (1ULL << 3))) + (!!((((w) >> 32) >> 8) & (1ULL << 4))) + (!!((((w) >> 32) >> 8) & (1ULL << 5))) + (!!((((w) >> 32) >> 8) & (1ULL << 6))) + (!!((((w) >> 32) >> 8) & (1ULL << 7)))))) + (((unsigned int) ((!!((((w) >> 32) >> 16) & (1ULL << 0))) + (!!((((w) >> 32) >> 16) & (1ULL << 1))) + (!!((((w) >> 32) >> 16) & (1ULL << 2))) + (!!((((w) >> 32) >> 16) & (1ULL << 3))) + (!!((((w) >> 32) >> 16) & (1ULL << 4))) + (!!((((w) >> 32) >> 16) & (1ULL << 5))) + (!!((((w) >> 32) >> 16) & (1ULL << 6))) + (!!((((w) >> 32) >> 16) & (1ULL << 7))))) + ((unsigned int) ((!!(((((w) >> 32) >> 16) >> 8) & (1ULL << 0))) + (!!(((((w) >> 32) >> 16) >> 8) & (1ULL << 1))) + (!!(((((w) >> 32) >> 16) >> 8) & (1ULL << 2))) + (!!(((((w) >> 32) >> 16) >> 8) & (1ULL << 3))) + (!!(((((w) >> 32) >> 16) >> 8) & (1ULL << 4))) + (!!(((((w) >> 32) >> 16) >> 8) & (1ULL << 5))) + (!!(((((w) >> 32) >> 16) >> 8) & (1ULL << 6))) + (!!(((((w) >> 32) >> 16) >> 8) & (1ULL << 7)))))))) : __arch_hweight64(w));
}






static inline __attribute__((no_instrument_function)) __u64 rol64(__u64 word, unsigned int shift)
{
 return (word << shift) | (word >> (64 - shift));
}






static inline __attribute__((no_instrument_function)) __u64 ror64(__u64 word, unsigned int shift)
{
 return (word >> shift) | (word << (64 - shift));
}






static inline __attribute__((no_instrument_function)) __u32 rol32(__u32 word, unsigned int shift)
{
 return (word << shift) | (word >> (32 - shift));
}






static inline __attribute__((no_instrument_function)) __u32 ror32(__u32 word, unsigned int shift)
{
 return (word >> shift) | (word << (32 - shift));
}






static inline __attribute__((no_instrument_function)) __u16 rol16(__u16 word, unsigned int shift)
{
 return (word << shift) | (word >> (16 - shift));
}






static inline __attribute__((no_instrument_function)) __u16 ror16(__u16 word, unsigned int shift)
{
 return (word >> shift) | (word << (16 - shift));
}






static inline __attribute__((no_instrument_function)) __u8 rol8(__u8 word, unsigned int shift)
{
 return (word << shift) | (word >> (8 - shift));
}






static inline __attribute__((no_instrument_function)) __u8 ror8(__u8 word, unsigned int shift)
{
 return (word >> shift) | (word << (8 - shift));
}






static inline __attribute__((no_instrument_function)) __s32 sign_extend32(__u32 value, int index)
{
 __u8 shift = 31 - index;
 return (__s32)(value << shift) >> shift;
}

static inline __attribute__((no_instrument_function)) unsigned fls_long(unsigned long l)
{
 if (sizeof(l) == 4)
  return fls(l);
 return fls64(l);
}
# 186 "include/linux/bitops.h"
static inline __attribute__((no_instrument_function)) unsigned long __ffs64(u64 word)
{






 return __ffs((unsigned long)word);
}
# 222 "include/linux/bitops.h"
extern unsigned long find_last_bit(const unsigned long *addr,
       unsigned long size);
# 11 "include/linux/kernel.h" 2
# 1 "include/linux/log2.h" 1
# 21 "include/linux/log2.h"
extern __attribute__((const, noreturn))
int ____ilog2_NaN(void);
# 31 "include/linux/log2.h"
static inline __attribute__((no_instrument_function)) __attribute__((const))
int __ilog2_u32(u32 n)
{
 return fls(n) - 1;
}



static inline __attribute__((no_instrument_function)) __attribute__((const))
int __ilog2_u64(u64 n)
{
 return fls64(n) - 1;
}







static inline __attribute__((no_instrument_function)) __attribute__((const))
bool is_power_of_2(unsigned long n)
{
 return (n != 0 && ((n & (n - 1)) == 0));
}




static inline __attribute__((no_instrument_function)) __attribute__((const))
unsigned long __roundup_pow_of_two(unsigned long n)
{
 return 1UL << fls_long(n - 1);
}




static inline __attribute__((no_instrument_function)) __attribute__((const))
unsigned long __rounddown_pow_of_two(unsigned long n)
{
 return 1UL << (fls_long(n) - 1);
}
# 12 "include/linux/kernel.h" 2
# 1 "include/linux/typecheck.h" 1
# 13 "include/linux/kernel.h" 2
# 1 "include/linux/printk.h" 1




# 1 "include/linux/init.h" 1
# 135 "include/linux/init.h"
typedef int (*initcall_t)(void);
typedef void (*exitcall_t)(void);

extern initcall_t __con_initcall_start[], __con_initcall_end[];
extern initcall_t __security_initcall_start[], __security_initcall_end[];


typedef void (*ctor_fn_t)(void);


extern int do_one_initcall(initcall_t fn);
extern char __attribute__ ((__section__(".init.data"))) boot_command_line[];
extern char *saved_command_line;
extern unsigned int reset_devices;


void setup_arch(char **);
void prepare_namespace(void);
void __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) load_default_modules(void);
int __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) init_rootfs(void);

extern void (*late_time_init)(void);

extern bool initcall_debug;
# 6 "include/linux/printk.h" 2
# 1 "include/linux/kern_levels.h" 1
# 7 "include/linux/printk.h" 2

# 1 "include/linux/cache.h" 1



# 1 "include/uapi/linux/kernel.h" 1



# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/include/uapi/linux/sysinfo.h" 1






struct sysinfo {
 __kernel_long_t uptime;
 __kernel_ulong_t loads[3];
 __kernel_ulong_t totalram;
 __kernel_ulong_t freeram;
 __kernel_ulong_t sharedram;
 __kernel_ulong_t bufferram;
 __kernel_ulong_t totalswap;
 __kernel_ulong_t freeswap;
 __u16 procs;
 __u16 pad;
 __kernel_ulong_t totalhigh;
 __kernel_ulong_t freehigh;
 __u32 mem_unit;
 char _f[20-2*sizeof(__kernel_ulong_t)-sizeof(__u32)];
};
# 5 "include/uapi/linux/kernel.h" 2
# 5 "include/linux/cache.h" 2
# 9 "include/linux/printk.h" 2

extern const char linux_banner[];
extern const char linux_proc_banner[];

static inline __attribute__((no_instrument_function)) int printk_get_level(const char *buffer)
{
 if (buffer[0] == '\001' && buffer[1]) {
  switch (buffer[1]) {
  case '0' ... '7':
  case 'd':
   return buffer[1];
  }
 }
 return 0;
}

static inline __attribute__((no_instrument_function)) const char *printk_skip_level(const char *buffer)
{
 if (printk_get_level(buffer))
  return buffer + 2;

 return buffer;
}

extern int console_printk[];






static inline __attribute__((no_instrument_function)) void console_silent(void)
{
 (console_printk[0]) = 0;
}

static inline __attribute__((no_instrument_function)) void console_verbose(void)
{
 if ((console_printk[0]))
  (console_printk[0]) = 15;
}

struct va_format {
 const char *fmt;
 va_list *va;
};
# 98 "include/linux/printk.h"
static inline __attribute__((no_instrument_function)) __attribute__((format(printf, 1, 2)))
int no_printk(const char *fmt, ...)
{
 return 0;
}


extern __attribute__((format(printf, 1, 2)))
void early_printk(const char *fmt, ...);
void early_vprintk(const char *fmt, va_list ap);






 __attribute__((format(printf, 5, 0)))
int vprintk_emit(int facility, int level,
   const char *dict, size_t dictlen,
   const char *fmt, va_list args);

 __attribute__((format(printf, 1, 0)))
int vprintk(const char *fmt, va_list args);

 __attribute__((format(printf, 5, 6)))
int printk_emit(int facility, int level,
  const char *dict, size_t dictlen,
  const char *fmt, ...);

 __attribute__((format(printf, 1, 2)))
int printk(const char *fmt, ...);




__attribute__((format(printf, 1, 2))) int printk_sched(const char *fmt, ...);






extern int __printk_ratelimit(const char *func);

extern bool printk_timed_ratelimit(unsigned long *caller_jiffies,
       unsigned int interval_msec);

extern int printk_delay_msec;
extern int dmesg_restrict;
extern int kptr_restrict;

extern void wake_up_klogd(void);

void log_buf_kexec_setup(void);
void __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) setup_log_buf(int early);
void dump_stack_set_arch_desc(const char *fmt, ...);
void dump_stack_print_info(const char *log_lvl);
void show_regs_print_info(const char *log_lvl);
# 207 "include/linux/printk.h"
extern void dump_stack(void) ;
# 240 "include/linux/printk.h"
# 1 "include/linux/dynamic_debug.h" 1
# 9 "include/linux/dynamic_debug.h"
struct _ddebug {




 const char *modname;
 const char *function;
 const char *filename;
 const char *format;
 unsigned int lineno:18;
# 35 "include/linux/dynamic_debug.h"
 unsigned int flags:8;
} __attribute__((aligned(8)));


int ddebug_add_module(struct _ddebug *tab, unsigned int n,
    const char *modname);


extern int ddebug_remove_module(const char *mod_name);
extern __attribute__((format(printf, 2, 3)))
int __dynamic_pr_debug(struct _ddebug *descriptor, const char *fmt, ...);

extern int ddebug_dyndbg_module_param_cb(char *param, char *val,
     const char *modname);

struct device;

extern __attribute__((format(printf, 3, 4)))
int __dynamic_dev_dbg(struct _ddebug *descriptor, const struct device *dev,
        const char *fmt, ...);

struct net_device;

extern __attribute__((format(printf, 3, 4)))
int __dynamic_netdev_dbg(struct _ddebug *descriptor,
    const struct net_device *dev,
    const char *fmt, ...);
# 241 "include/linux/printk.h" 2
# 372 "include/linux/printk.h"
extern const struct file_operations kmsg_fops;

enum {
 DUMP_PREFIX_NONE,
 DUMP_PREFIX_ADDRESS,
 DUMP_PREFIX_OFFSET
};
extern void hex_dump_to_buffer(const void *buf, size_t len,
          int rowsize, int groupsize,
          char *linebuf, size_t linebuflen, bool ascii);

extern void print_hex_dump(const char *level, const char *prefix_str,
      int prefix_type, int rowsize, int groupsize,
      const void *buf, size_t len, bool ascii);
# 14 "include/linux/kernel.h" 2
# 153 "include/linux/kernel.h"
struct completion;
struct pt_regs;
struct user;
# 165 "include/linux/kernel.h"
  void __might_sleep(const char *file, int line, int preempt_offset);
# 223 "include/linux/kernel.h"
static inline __attribute__((no_instrument_function)) u32 reciprocal_scale(u32 val, u32 ep_ro)
{
 return (u32)(((u64) val * ep_ro) >> 32);
}



void might_fault(void);




extern struct atomic_notifier_head panic_notifier_list;
extern long (*panic_blink)(int state);
__attribute__((format(printf, 1, 2)))
void panic(const char *fmt, ...)
 __attribute__((noreturn)) ;
extern void oops_enter(void);
extern void oops_exit(void);
void print_oops_end_marker(void);
extern int oops_may_print(void);
void do_exit(long error_code)
 __attribute__((noreturn));
void complete_and_exit(struct completion *, long)
 __attribute__((noreturn));


int __attribute__((warn_unused_result)) _kstrtoul(const char *s, unsigned int base, unsigned long *res);
int __attribute__((warn_unused_result)) _kstrtol(const char *s, unsigned int base, long *res);

int __attribute__((warn_unused_result)) kstrtoull(const char *s, unsigned int base, unsigned long long *res);
int __attribute__((warn_unused_result)) kstrtoll(const char *s, unsigned int base, long long *res);
# 272 "include/linux/kernel.h"
static inline __attribute__((no_instrument_function)) int __attribute__((warn_unused_result)) kstrtoul(const char *s, unsigned int base, unsigned long *res)
{




 if (sizeof(unsigned long) == sizeof(unsigned long long) &&
     __alignof__(unsigned long) == __alignof__(unsigned long long))
  return kstrtoull(s, base, (unsigned long long *)res);
 else
  return _kstrtoul(s, base, res);
}
# 301 "include/linux/kernel.h"
static inline __attribute__((no_instrument_function)) int __attribute__((warn_unused_result)) kstrtol(const char *s, unsigned int base, long *res)
{




 if (sizeof(long) == sizeof(long long) &&
     __alignof__(long) == __alignof__(long long))
  return kstrtoll(s, base, (long long *)res);
 else
  return _kstrtol(s, base, res);
}

int __attribute__((warn_unused_result)) kstrtouint(const char *s, unsigned int base, unsigned int *res);
int __attribute__((warn_unused_result)) kstrtoint(const char *s, unsigned int base, int *res);

static inline __attribute__((no_instrument_function)) int __attribute__((warn_unused_result)) kstrtou64(const char *s, unsigned int base, u64 *res)
{
 return kstrtoull(s, base, res);
}

static inline __attribute__((no_instrument_function)) int __attribute__((warn_unused_result)) kstrtos64(const char *s, unsigned int base, s64 *res)
{
 return kstrtoll(s, base, res);
}

static inline __attribute__((no_instrument_function)) int __attribute__((warn_unused_result)) kstrtou32(const char *s, unsigned int base, u32 *res)
{
 return kstrtouint(s, base, res);
}

static inline __attribute__((no_instrument_function)) int __attribute__((warn_unused_result)) kstrtos32(const char *s, unsigned int base, s32 *res)
{
 return kstrtoint(s, base, res);
}

int __attribute__((warn_unused_result)) kstrtou16(const char *s, unsigned int base, u16 *res);
int __attribute__((warn_unused_result)) kstrtos16(const char *s, unsigned int base, s16 *res);
int __attribute__((warn_unused_result)) kstrtou8(const char *s, unsigned int base, u8 *res);
int __attribute__((warn_unused_result)) kstrtos8(const char *s, unsigned int base, s8 *res);

int __attribute__((warn_unused_result)) kstrtoull_from_user(const char *s, size_t count, unsigned int base, unsigned long long *res);
int __attribute__((warn_unused_result)) kstrtoll_from_user(const char *s, size_t count, unsigned int base, long long *res);
int __attribute__((warn_unused_result)) kstrtoul_from_user(const char *s, size_t count, unsigned int base, unsigned long *res);
int __attribute__((warn_unused_result)) kstrtol_from_user(const char *s, size_t count, unsigned int base, long *res);
int __attribute__((warn_unused_result)) kstrtouint_from_user(const char *s, size_t count, unsigned int base, unsigned int *res);
int __attribute__((warn_unused_result)) kstrtoint_from_user(const char *s, size_t count, unsigned int base, int *res);
int __attribute__((warn_unused_result)) kstrtou16_from_user(const char *s, size_t count, unsigned int base, u16 *res);
int __attribute__((warn_unused_result)) kstrtos16_from_user(const char *s, size_t count, unsigned int base, s16 *res);
int __attribute__((warn_unused_result)) kstrtou8_from_user(const char *s, size_t count, unsigned int base, u8 *res);
int __attribute__((warn_unused_result)) kstrtos8_from_user(const char *s, size_t count, unsigned int base, s8 *res);

static inline __attribute__((no_instrument_function)) int __attribute__((warn_unused_result)) kstrtou64_from_user(const char *s, size_t count, unsigned int base, u64 *res)
{
 return kstrtoull_from_user(s, count, base, res);
}

static inline __attribute__((no_instrument_function)) int __attribute__((warn_unused_result)) kstrtos64_from_user(const char *s, size_t count, unsigned int base, s64 *res)
{
 return kstrtoll_from_user(s, count, base, res);
}

static inline __attribute__((no_instrument_function)) int __attribute__((warn_unused_result)) kstrtou32_from_user(const char *s, size_t count, unsigned int base, u32 *res)
{
 return kstrtouint_from_user(s, count, base, res);
}

static inline __attribute__((no_instrument_function)) int __attribute__((warn_unused_result)) kstrtos32_from_user(const char *s, size_t count, unsigned int base, s32 *res)
{
 return kstrtoint_from_user(s, count, base, res);
}



extern unsigned long simple_strtoul(const char *,char **,unsigned int);
extern long simple_strtol(const char *,char **,unsigned int);
extern unsigned long long simple_strtoull(const char *,char **,unsigned int);
extern long long simple_strtoll(const char *,char **,unsigned int);





extern int num_to_str(char *buf, int size, unsigned long long num);



extern __attribute__((format(printf, 2, 3))) int sprintf(char *buf, const char * fmt, ...);
extern __attribute__((format(printf, 2, 0))) int vsprintf(char *buf, const char *, va_list);
extern __attribute__((format(printf, 3, 4)))
int snprintf(char *buf, size_t size, const char *fmt, ...);
extern __attribute__((format(printf, 3, 0)))
int vsnprintf(char *buf, size_t size, const char *fmt, va_list args);
extern __attribute__((format(printf, 3, 4)))
int scnprintf(char *buf, size_t size, const char *fmt, ...);
extern __attribute__((format(printf, 3, 0)))
int vscnprintf(char *buf, size_t size, const char *fmt, va_list args);
extern __attribute__((format(printf, 2, 3)))
char *kasprintf(gfp_t gfp, const char *fmt, ...);
extern char *kvasprintf(gfp_t gfp, const char *fmt, va_list args);

extern __attribute__((format(scanf, 2, 3)))
int sscanf(const char *, const char *, ...);
extern __attribute__((format(scanf, 2, 0)))
int vsscanf(const char *, const char *, va_list);

extern int get_option(char **str, int *pint);
extern char *get_options(const char *str, int nints, int *ints);
extern unsigned long long memparse(const char *ptr, char **retptr);

extern int core_kernel_text(unsigned long addr);
extern int core_kernel_data(unsigned long addr);
extern int __kernel_text_address(unsigned long addr);
extern int kernel_text_address(unsigned long addr);
extern int func_ptr_is_kernel_text(void *ptr);

struct pid;
extern struct pid *session_of_pgrp(struct pid *pgrp);

unsigned long int_sqrt(unsigned long);

extern void bust_spinlocks(int yes);
extern int oops_in_progress;
extern int panic_timeout;
extern int panic_on_oops;
extern int panic_on_unrecovered_nmi;
extern int panic_on_io_nmi;
extern int sysctl_panic_on_stackoverflow;




static inline __attribute__((no_instrument_function)) void set_arch_panic_timeout(int timeout, int arch_default_timeout)
{
 if (panic_timeout == arch_default_timeout)
  panic_timeout = timeout;
}
extern const char *print_tainted(void);
enum lockdep_ok {
 LOCKDEP_STILL_OK,
 LOCKDEP_NOW_UNRELIABLE
};
extern void add_taint(unsigned flag, enum lockdep_ok);
extern int test_taint(unsigned flag);
extern unsigned long get_taint(void);
extern int root_mountflags;

extern bool early_boot_irqs_disabled;


extern enum system_states {
 SYSTEM_BOOTING,
 SYSTEM_RUNNING,
 SYSTEM_HALT,
 SYSTEM_POWER_OFF,
 SYSTEM_RESTART,
} system_state;
# 474 "include/linux/kernel.h"
extern const char hex_asc[];



static inline __attribute__((no_instrument_function)) char *hex_byte_pack(char *buf, u8 byte)
{
 *buf++ = hex_asc[((byte) & 0xf0) >> 4];
 *buf++ = hex_asc[((byte) & 0x0f)];
 return buf;
}

extern const char hex_asc_upper[];



static inline __attribute__((no_instrument_function)) char *hex_byte_pack_upper(char *buf, u8 byte)
{
 *buf++ = hex_asc_upper[((byte) & 0xf0) >> 4];
 *buf++ = hex_asc_upper[((byte) & 0x0f)];
 return buf;
}

static inline __attribute__((no_instrument_function)) char * __attribute__((deprecated)) pack_hex_byte(char *buf, u8 byte)
{
 return hex_byte_pack(buf, byte);
}

extern int hex_to_bin(char ch);
extern int __attribute__((warn_unused_result)) hex2bin(u8 *dst, const char *src, size_t count);

int mac_pton(const char *s, u8 *mac);
# 527 "include/linux/kernel.h"
void tracing_off_permanent(void);




enum ftrace_dump_mode {
 DUMP_NONE,
 DUMP_ALL,
 DUMP_ORIG,
};


void tracing_on(void);
void tracing_off(void);
int tracing_is_on(void);
void tracing_snapshot(void);
void tracing_snapshot_alloc(void);

extern void tracing_start(void);
extern void tracing_stop(void);

static inline __attribute__((no_instrument_function)) __attribute__((format(printf, 1, 2)))
void ____trace_printk_check_format(const char *fmt, ...)
{
}
# 611 "include/linux/kernel.h"
extern __attribute__((format(printf, 2, 3)))
int __trace_bprintk(unsigned long ip, const char *fmt, ...);

extern __attribute__((format(printf, 2, 3)))
int __trace_printk(unsigned long ip, const char *fmt, ...);
# 652 "include/linux/kernel.h"
extern int __trace_bputs(unsigned long ip, const char *str);
extern int __trace_puts(unsigned long ip, const char *str, int size);

extern void trace_dump_stack(int skip);
# 674 "include/linux/kernel.h"
extern int
__ftrace_vbprintk(unsigned long ip, const char *fmt, va_list ap);

extern int
__ftrace_vprintk(unsigned long ip, const char *fmt, va_list ap);

extern void ftrace_dump(enum ftrace_dump_mode oops_dump_mode);
# 11 "include/linux/kernfs.h" 2
# 1 "include/linux/err.h" 1






# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/errno.h" 1
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/include/uapi/asm-generic/errno.h" 1



# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/include/uapi/asm-generic/errno-base.h" 1
# 5 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/include/uapi/asm-generic/errno.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/errno.h" 2
# 8 "include/linux/err.h" 2
# 23 "include/linux/err.h"
static inline __attribute__((no_instrument_function)) void * __attribute__((warn_unused_result)) ERR_PTR(long error)
{
 return (void *) error;
}

static inline __attribute__((no_instrument_function)) long __attribute__((warn_unused_result)) PTR_ERR( const void *ptr)
{
 return (long) ptr;
}

static inline __attribute__((no_instrument_function)) bool __attribute__((warn_unused_result)) IS_ERR( const void *ptr)
{
 return __builtin_expect(!!(((unsigned long)ptr) >= (unsigned long)-4095), 0);
}

static inline __attribute__((no_instrument_function)) bool __attribute__((warn_unused_result)) IS_ERR_OR_NULL( const void *ptr)
{
 return !ptr || __builtin_expect(!!(((unsigned long)ptr) >= (unsigned long)-4095), 0);
}
# 50 "include/linux/err.h"
static inline __attribute__((no_instrument_function)) void * __attribute__((warn_unused_result)) ERR_CAST( const void *ptr)
{

 return (void *) ptr;
}

static inline __attribute__((no_instrument_function)) int __attribute__((warn_unused_result)) PTR_ERR_OR_ZERO( const void *ptr)
{
 if (IS_ERR(ptr))
  return PTR_ERR(ptr);
 else
  return 0;
}
# 12 "include/linux/kernfs.h" 2

# 1 "include/linux/mutex.h" 1
# 13 "include/linux/mutex.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/current.h" 1




# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/percpu.h" 1
# 88 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/percpu.h"
extern void __bad_percpu_size(void);
# 492 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/percpu.h"
static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) int x86_this_cpu_constant_test_bit(unsigned int nr,
                        const unsigned long *addr)
{
 unsigned long *a = (unsigned long *)addr + nr / 64;


 return ((1UL << (nr % 64)) & ({ typeof((*a)) pfo_ret__; switch (sizeof((*a))) { case 1: asm("mov" "b ""%%""gs"":" "%P" "1"",%0" : "=q" (pfo_ret__) : "m"(*a)); break; case 2: asm("mov" "w ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"(*a)); break; case 4: asm("mov" "l ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"(*a)); break; case 8: asm("mov" "q ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"(*a)); break; default: __bad_percpu_size(); } pfo_ret__; })) != 0;



}

static inline __attribute__((no_instrument_function)) int x86_this_cpu_variable_test_bit(int nr,
                        const unsigned long *addr)
{
 int oldbit;

 asm volatile("bt ""%%""gs"":" "%P" "2"",%1\n\t"
   "sbb %0,%0"
   : "=r" (oldbit)
   : "m" (*(unsigned long *)addr), "Ir" (nr));

 return oldbit;
}







# 1 "include/asm-generic/percpu.h" 1




# 1 "include/linux/threads.h" 1
# 6 "include/asm-generic/percpu.h" 2
# 1 "include/linux/percpu-defs.h" 1
# 7 "include/asm-generic/percpu.h" 2
# 18 "include/asm-generic/percpu.h"
extern unsigned long __per_cpu_offset[8192];
# 72 "include/asm-generic/percpu.h"
extern void setup_per_cpu_areas(void);
# 524 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/percpu.h" 2


extern __attribute__((section(".discard"), unused)) char __pcpu_scope_this_cpu_off; extern __attribute__((section(".data..percpu" ""))) __typeof__(unsigned long) this_cpu_off;
# 6 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/current.h" 2


struct task_struct;

extern __attribute__((section(".discard"), unused)) char __pcpu_scope_current_task; extern __attribute__((section(".data..percpu" ""))) __typeof__(struct task_struct *) current_task;

static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) struct task_struct *get_current(void)
{
 return ({ typeof(current_task) pfo_ret__; switch (sizeof(current_task)) { case 1: asm("mov" "b ""%%""gs"":" "%P" "1"",%0" : "=q" (pfo_ret__) : "p" (&(current_task))); break; case 2: asm("mov" "w ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "p" (&(current_task))); break; case 4: asm("mov" "l ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "p" (&(current_task))); break; case 8: asm("mov" "q ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "p" (&(current_task))); break; default: __bad_percpu_size(); } pfo_ret__; });
}
# 14 "include/linux/mutex.h" 2

# 1 "include/linux/spinlock_types.h" 1
# 18 "include/linux/spinlock_types.h"
# 1 "include/linux/lockdep.h" 1
# 12 "include/linux/lockdep.h"
struct task_struct;
struct lockdep_map;


extern int prove_locking;
extern int lock_stat;





# 1 "include/linux/debug_locks.h" 1




# 1 "include/linux/atomic.h" 1



# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic.h" 1





# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h" 1






struct task_struct;
struct mm_struct;

# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/vm86.h" 1





# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/vm86.h" 1
# 62 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/vm86.h"
struct vm86_regs {



 long ebx;
 long ecx;
 long edx;
 long esi;
 long edi;
 long ebp;
 long eax;
 long __null_ds;
 long __null_es;
 long __null_fs;
 long __null_gs;
 long orig_eax;
 long eip;
 unsigned short cs, __csh;
 long eflags;
 long esp;
 unsigned short ss, __ssh;



 unsigned short es, __esh;
 unsigned short ds, __dsh;
 unsigned short fs, __fsh;
 unsigned short gs, __gsh;
};

struct revectored_struct {
 unsigned long __map[8];
};

struct vm86_struct {
 struct vm86_regs regs;
 unsigned long flags;
 unsigned long screen_bitmap;
 unsigned long cpu_type;
 struct revectored_struct int_revectored;
 struct revectored_struct int21_revectored;
};






struct vm86plus_info_struct {
 unsigned long force_return_for_pic:1;
 unsigned long vm86dbg_active:1;
 unsigned long vm86dbg_TFpendig:1;
 unsigned long unused:28;
 unsigned long is_vm86pus:1;
 unsigned char vm86dbg_intxxtab[32];
};
struct vm86plus_struct {
 struct vm86_regs regs;
 unsigned long flags;
 unsigned long screen_bitmap;
 unsigned long cpu_type;
 struct revectored_struct int_revectored;
 struct revectored_struct int21_revectored;
 struct vm86plus_info_struct vm86plus;
};
# 7 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/vm86.h" 2
# 17 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/vm86.h"
struct kernel_vm86_regs {



 struct pt_regs pt;



 unsigned short es, __esh;
 unsigned short ds, __dsh;
 unsigned short fs, __fsh;
 unsigned short gs, __gsh;
};

struct kernel_vm86_struct {
 struct kernel_vm86_regs regs;
# 42 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/vm86.h"
 unsigned long flags;
 unsigned long screen_bitmap;
 unsigned long cpu_type;
 struct revectored_struct int_revectored;
 struct revectored_struct int21_revectored;
 struct vm86plus_info_struct vm86plus;
 struct pt_regs *regs32;
# 59 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/vm86.h"
};
# 75 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/vm86.h"
static inline __attribute__((no_instrument_function)) int handle_vm86_trap(struct kernel_vm86_regs *a, long b, int c)
{
 return 0;
}
# 11 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/math_emu.h" 1
# 11 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/math_emu.h"
struct math_emu_info {
 long ___orig_eip;
 union {
  struct pt_regs *regs;
  struct kernel_vm86_regs *vm86;
 };
};
# 12 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h" 2


# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/sigcontext.h" 1



# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/sigcontext.h" 1
# 23 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/sigcontext.h"
struct _fpx_sw_bytes {
 __u32 magic1;
 __u32 extended_size;


 __u64 xstate_bv;




 __u32 xstate_size;




 __u32 padding[7];
};
# 136 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/sigcontext.h"
struct _fpstate {
 __u16 cwd;
 __u16 swd;
 __u16 twd;

 __u16 fop;
 __u64 rip;
 __u64 rdp;
 __u32 mxcsr;
 __u32 mxcsr_mask;
 __u32 st_space[32];
 __u32 xmm_space[64];
 __u32 reserved2[12];
 union {
  __u32 reserved3[12];
  struct _fpx_sw_bytes sw_reserved;

 };
};
# 197 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/sigcontext.h"
struct _xsave_hdr {
 __u64 xstate_bv;
 __u64 reserved1[2];
 __u64 reserved2[5];
};

struct _ymmh_state {

 __u32 ymmh_space[64];
};







struct _xstate {
 struct _fpstate fpstate;
 struct _xsave_hdr xstate_hdr;
 struct _ymmh_state ymmh;

};
# 5 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/sigcontext.h" 2
# 40 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/sigcontext.h"
struct sigcontext {
 unsigned long r8;
 unsigned long r9;
 unsigned long r10;
 unsigned long r11;
 unsigned long r12;
 unsigned long r13;
 unsigned long r14;
 unsigned long r15;
 unsigned long di;
 unsigned long si;
 unsigned long bp;
 unsigned long bx;
 unsigned long dx;
 unsigned long ax;
 unsigned long cx;
 unsigned long sp;
 unsigned long ip;
 unsigned long flags;
 unsigned short cs;
 unsigned short gs;
 unsigned short fs;
 unsigned short __pad0;
 unsigned long err;
 unsigned long trapno;
 unsigned long oldmask;
 unsigned long cr2;
# 75 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/sigcontext.h"
 void *fpstate;
 unsigned long reserved1[8];
};
# 15 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h" 2


# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/page.h" 1
# 11 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/page.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/page_64.h" 1
# 9 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/page_64.h"
extern unsigned long max_pfn;
extern unsigned long phys_base;

static inline __attribute__((no_instrument_function)) unsigned long __phys_addr_nodebug(unsigned long x)
{
 unsigned long y = x - (0xffffffff80000000UL);


 x = y + ((x > y) ? phys_base : ((0xffffffff80000000UL) - ((unsigned long)(0xffff880000000000UL))));

 return x;
}


extern unsigned long __phys_addr(unsigned long);
extern unsigned long __phys_addr_symbol(unsigned long);
# 37 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/page_64.h"
void clear_page(void *page);
void copy_page(void *to, void *from);
# 12 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/page.h" 2






struct page;

# 1 "include/linux/range.h" 1



struct range {
 u64 start;
 u64 end;
};

int add_range(struct range *range, int az, int nr_range,
  u64 start, u64 end);


int add_range_with_merge(struct range *range, int az, int nr_range,
    u64 start, u64 end);

void subtract_range(struct range *range, int az, u64 start, u64 end);

int clean_sort_range(struct range *range, int az);

void sort_range(struct range *range, int nr_range);


static inline __attribute__((no_instrument_function)) resource_size_t cap_resource(u64 val)
{
 if (val > ((resource_size_t)~0))
  return ((resource_size_t)~0);

 return val;
}
# 21 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/page.h" 2
extern struct range pfn_mapped[];
extern int nr_pfn_mapped;

static inline __attribute__((no_instrument_function)) void clear_user_page(void *page, unsigned long vaddr,
       struct page *pg)
{
 clear_page(page);
}

static inline __attribute__((no_instrument_function)) void copy_user_page(void *to, void *from, unsigned long vaddr,
      struct page *topage)
{
 copy_page(to, from);
}
# 65 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/page.h"
extern bool __virt_addr_valid(unsigned long kaddr);




# 1 "include/asm-generic/memory_model.h" 1
# 71 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/page.h" 2
# 1 "include/asm-generic/getorder.h" 1
# 12 "include/asm-generic/getorder.h"
static inline __attribute__((no_instrument_function)) __attribute__((__const__))
int __get_order(unsigned long size)
{
 int order;

 size--;
 size >>= 12;



 order = fls64(size);

 return order;
}
# 72 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/page.h" 2
# 18 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h" 2


# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/msr.h" 1



# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/msr.h" 1



# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/msr-index.h" 1
# 5 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/msr.h" 2




# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/include/uapi/linux/ioctl.h" 1



# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/ioctl.h" 1
# 1 "include/asm-generic/ioctl.h" 1



# 1 "include/uapi/asm-generic/ioctl.h" 1
# 5 "include/asm-generic/ioctl.h" 2


extern unsigned int __invalid_size_argument_for_IOC;
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/ioctl.h" 2
# 5 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/include/uapi/linux/ioctl.h" 2
# 10 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/msr.h" 2
# 5 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/msr.h" 2




# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/errno.h" 1
# 10 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/msr.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/cpumask.h" 1



# 1 "include/linux/cpumask.h" 1
# 11 "include/linux/cpumask.h"
# 1 "include/linux/bitmap.h" 1







# 1 "include/linux/string.h" 1
# 9 "include/linux/string.h"
# 1 "include/uapi/linux/string.h" 1
# 10 "include/linux/string.h" 2

extern char *strndup_user(const char *, long);
extern void *memdup_user(const void *, size_t);




# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/string.h" 1



# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/string_64.h" 1
# 9 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/string_64.h"
static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) void *__inline_memcpy(void *to, const void *from, size_t n)
{
 unsigned long d0, d1, d2;
 asm volatile("rep ; movsl\n\t"
       "testb $2,%b4\n\t"
       "je 1f\n\t"
       "movsw\n"
       "1:\ttestb $1,%b4\n\t"
       "je 2f\n\t"
       "movsb\n"
       "2:"
       : "=&c" (d0), "=&D" (d1), "=&S" (d2)
       : "0" (n / 4), "q" (n), "1" ((long)to), "2" ((long)from)
       : "memory");
 return to;
}
# 34 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/string_64.h"
extern void *__memcpy(void *to, const void *from, size_t len);
# 55 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/string_64.h"
void *memset(void *s, int c, size_t n);


void *memmove(void *dest, const void *src, size_t count);

int memcmp(const void *cs, const void *ct, size_t count);
size_t strlen(const char *s);
char *strcpy(char *dest, const char *src);
char *strcat(char *dest, const char *src);
int strcmp(const char *cs, const char *ct);
# 5 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/string.h" 2
# 18 "include/linux/string.h" 2


extern char * strcpy(char *,const char *);


extern char * strncpy(char *,const char *, __kernel_size_t);


size_t strlcpy(char *, const char *, size_t);


extern char * strcat(char *, const char *);


extern char * strncat(char *, const char *, __kernel_size_t);


extern size_t strlcat(char *, const char *, __kernel_size_t);


extern int strcmp(const char *,const char *);


extern int strncmp(const char *,const char *,__kernel_size_t);


extern int strnicmp(const char *, const char *, __kernel_size_t);


extern int strcasecmp(const char *s1, const char *s2);


extern int strncasecmp(const char *s1, const char *s2, size_t n);


extern char * strchr(const char *,int);


extern char * strnchr(const char *, size_t, int);


extern char * strrchr(const char *,int);

extern char * __attribute__((warn_unused_result)) skip_spaces(const char *);

extern char *strim(char *);

static inline __attribute__((no_instrument_function)) __attribute__((warn_unused_result)) char *strstrip(char *str)
{
 return strim(str);
}


extern char * strstr(const char *, const char *);


extern char * strnstr(const char *, const char *, size_t);


extern __kernel_size_t strlen(const char *);


extern __kernel_size_t strnlen(const char *,__kernel_size_t);


extern char * strpbrk(const char *,const char *);


extern char * strsep(char **,const char *);


extern __kernel_size_t strspn(const char *,const char *);


extern __kernel_size_t strcspn(const char *,const char *);
# 105 "include/linux/string.h"
extern void * memscan(void *,int,__kernel_size_t);


extern int memcmp(const void *,const void *,__kernel_size_t);


extern void * memchr(const void *,int,__kernel_size_t);

void *memchr_inv(const void *s, int c, size_t n);

extern char *kstrdup(const char *s, gfp_t gfp);
extern char *kstrndup(const char *s, size_t len, gfp_t gfp);
extern void *kmemdup(const void *src, size_t len, gfp_t gfp);

extern char **argv_split(gfp_t gfp, const char *str, int *argcp);
extern void argv_free(char **argv);

extern bool sysfs_streq(const char *s1, const char *s2);
extern int strtobool(const char *s, bool *res);


int vbin_printf(u32 *bin_buf, size_t size, const char *fmt, va_list args);
int bstr_printf(char *buf, size_t size, const char *fmt, const u32 *bin_buf);
int bprintf(u32 *bin_buf, size_t size, const char *fmt, ...) __attribute__((format(printf, 3, 4)));


extern ssize_t memory_read_from_buffer(void *to, size_t count, loff_t *ppos,
   const void *from, size_t available);






static inline __attribute__((no_instrument_function)) bool strstarts(const char *str, const char *prefix)
{
 return strncmp(str, prefix, strlen(prefix)) == 0;
}

extern size_t memweight(const void *ptr, size_t bytes);






static inline __attribute__((no_instrument_function)) const char *kbasename(const char *path)
{
 const char *tail = strrchr(path, '/');
 return tail ? tail + 1 : path;
}
# 9 "include/linux/bitmap.h" 2
# 91 "include/linux/bitmap.h"
extern int __bitmap_empty(const unsigned long *bitmap, int bits);
extern int __bitmap_full(const unsigned long *bitmap, int bits);
extern int __bitmap_equal(const unsigned long *bitmap1,
                 const unsigned long *bitmap2, int bits);
extern void __bitmap_complement(unsigned long *dst, const unsigned long *src,
   int bits);
extern void __bitmap_shift_right(unsigned long *dst,
                        const unsigned long *src, int shift, int bits);
extern void __bitmap_shift_left(unsigned long *dst,
                        const unsigned long *src, int shift, int bits);
extern int __bitmap_and(unsigned long *dst, const unsigned long *bitmap1,
   const unsigned long *bitmap2, int bits);
extern void __bitmap_or(unsigned long *dst, const unsigned long *bitmap1,
   const unsigned long *bitmap2, int bits);
extern void __bitmap_xor(unsigned long *dst, const unsigned long *bitmap1,
   const unsigned long *bitmap2, int bits);
extern int __bitmap_andnot(unsigned long *dst, const unsigned long *bitmap1,
   const unsigned long *bitmap2, int bits);
extern int __bitmap_intersects(const unsigned long *bitmap1,
   const unsigned long *bitmap2, int bits);
extern int __bitmap_subset(const unsigned long *bitmap1,
   const unsigned long *bitmap2, int bits);
extern int __bitmap_weight(const unsigned long *bitmap, int bits);

extern void bitmap_set(unsigned long *map, int i, int len);
extern void bitmap_clear(unsigned long *map, int start, int nr);
extern unsigned long bitmap_find_next_zero_area(unsigned long *map,
      unsigned long size,
      unsigned long start,
      unsigned int nr,
      unsigned long align_mask);

extern int bitmap_scnprintf(char *buf, unsigned int len,
   const unsigned long *src, int nbits);
extern int __bitmap_parse(const char *buf, unsigned int buflen, int is_user,
   unsigned long *dst, int nbits);
extern int bitmap_parse_user(const char *ubuf, unsigned int ulen,
   unsigned long *dst, int nbits);
extern int bitmap_scnlistprintf(char *buf, unsigned int len,
   const unsigned long *src, int nbits);
extern int bitmap_parselist(const char *buf, unsigned long *maskp,
   int nmaskbits);
extern int bitmap_parselist_user(const char *ubuf, unsigned int ulen,
   unsigned long *dst, int nbits);
extern void bitmap_remap(unsigned long *dst, const unsigned long *src,
  const unsigned long *old, const unsigned long *new, int bits);
extern int bitmap_bitremap(int oldbit,
  const unsigned long *old, const unsigned long *new, int bits);
extern void bitmap_onto(unsigned long *dst, const unsigned long *orig,
  const unsigned long *relmap, int bits);
extern void bitmap_fold(unsigned long *dst, const unsigned long *orig,
  int sz, int bits);
extern int bitmap_find_free_region(unsigned long *bitmap, int bits, int order);
extern void bitmap_release_region(unsigned long *bitmap, int pos, int order);
extern int bitmap_allocate_region(unsigned long *bitmap, int pos, int order);
extern void bitmap_copy_le(void *dst, const unsigned long *src, int nbits);
extern int bitmap_ord_to_pos(const unsigned long *bitmap, int n, int bits);
# 159 "include/linux/bitmap.h"
static inline __attribute__((no_instrument_function)) void bitmap_zero(unsigned long *dst, int nbits)
{
 if ((__builtin_constant_p(nbits) && (nbits) <= 64))
  *dst = 0UL;
 else {
  int len = (((nbits) + (8 * sizeof(long)) - 1) / (8 * sizeof(long))) * sizeof(unsigned long);
  memset(dst, 0, len);
 }
}

static inline __attribute__((no_instrument_function)) void bitmap_fill(unsigned long *dst, int nbits)
{
 size_t nlongs = (((nbits) + (8 * sizeof(long)) - 1) / (8 * sizeof(long)));
 if (!(__builtin_constant_p(nbits) && (nbits) <= 64)) {
  int len = (nlongs - 1) * sizeof(unsigned long);
  memset(dst, 0xff, len);
 }
 dst[nlongs - 1] = ( ((nbits) % 64) ? (1UL<<((nbits) % 64))-1 : ~0UL );
}

static inline __attribute__((no_instrument_function)) void bitmap_copy(unsigned long *dst, const unsigned long *src,
   int nbits)
{
 if ((__builtin_constant_p(nbits) && (nbits) <= 64))
  *dst = *src;
 else {
  int len = (((nbits) + (8 * sizeof(long)) - 1) / (8 * sizeof(long))) * sizeof(unsigned long);
  ({ size_t __len = (len); void *__ret; if (__builtin_constant_p(len) && __len >= 64) __ret = __memcpy((dst), (src), __len); else __ret = __builtin_memcpy((dst), (src), __len); __ret; });
 }
}

static inline __attribute__((no_instrument_function)) int bitmap_and(unsigned long *dst, const unsigned long *src1,
   const unsigned long *src2, int nbits)
{
 if ((__builtin_constant_p(nbits) && (nbits) <= 64))
  return (*dst = *src1 & *src2) != 0;
 return __bitmap_and(dst, src1, src2, nbits);
}

static inline __attribute__((no_instrument_function)) void bitmap_or(unsigned long *dst, const unsigned long *src1,
   const unsigned long *src2, int nbits)
{
 if ((__builtin_constant_p(nbits) && (nbits) <= 64))
  *dst = *src1 | *src2;
 else
  __bitmap_or(dst, src1, src2, nbits);
}

static inline __attribute__((no_instrument_function)) void bitmap_xor(unsigned long *dst, const unsigned long *src1,
   const unsigned long *src2, int nbits)
{
 if ((__builtin_constant_p(nbits) && (nbits) <= 64))
  *dst = *src1 ^ *src2;
 else
  __bitmap_xor(dst, src1, src2, nbits);
}

static inline __attribute__((no_instrument_function)) int bitmap_andnot(unsigned long *dst, const unsigned long *src1,
   const unsigned long *src2, int nbits)
{
 if ((__builtin_constant_p(nbits) && (nbits) <= 64))
  return (*dst = *src1 & ~(*src2)) != 0;
 return __bitmap_andnot(dst, src1, src2, nbits);
}

static inline __attribute__((no_instrument_function)) void bitmap_complement(unsigned long *dst, const unsigned long *src,
   int nbits)
{
 if ((__builtin_constant_p(nbits) && (nbits) <= 64))
  *dst = ~(*src) & ( ((nbits) % 64) ? (1UL<<((nbits) % 64))-1 : ~0UL );
 else
  __bitmap_complement(dst, src, nbits);
}

static inline __attribute__((no_instrument_function)) int bitmap_equal(const unsigned long *src1,
   const unsigned long *src2, int nbits)
{
 if ((__builtin_constant_p(nbits) && (nbits) <= 64))
  return ! ((*src1 ^ *src2) & ( ((nbits) % 64) ? (1UL<<((nbits) % 64))-1 : ~0UL ));
 else
  return __bitmap_equal(src1, src2, nbits);
}

static inline __attribute__((no_instrument_function)) int bitmap_intersects(const unsigned long *src1,
   const unsigned long *src2, int nbits)
{
 if ((__builtin_constant_p(nbits) && (nbits) <= 64))
  return ((*src1 & *src2) & ( ((nbits) % 64) ? (1UL<<((nbits) % 64))-1 : ~0UL )) != 0;
 else
  return __bitmap_intersects(src1, src2, nbits);
}

static inline __attribute__((no_instrument_function)) int bitmap_subset(const unsigned long *src1,
   const unsigned long *src2, int nbits)
{
 if ((__builtin_constant_p(nbits) && (nbits) <= 64))
  return ! ((*src1 & ~(*src2)) & ( ((nbits) % 64) ? (1UL<<((nbits) % 64))-1 : ~0UL ));
 else
  return __bitmap_subset(src1, src2, nbits);
}

static inline __attribute__((no_instrument_function)) int bitmap_empty(const unsigned long *src, int nbits)
{
 if ((__builtin_constant_p(nbits) && (nbits) <= 64))
  return ! (*src & ( ((nbits) % 64) ? (1UL<<((nbits) % 64))-1 : ~0UL ));
 else
  return __bitmap_empty(src, nbits);
}

static inline __attribute__((no_instrument_function)) int bitmap_full(const unsigned long *src, int nbits)
{
 if ((__builtin_constant_p(nbits) && (nbits) <= 64))
  return ! (~(*src) & ( ((nbits) % 64) ? (1UL<<((nbits) % 64))-1 : ~0UL ));
 else
  return __bitmap_full(src, nbits);
}

static inline __attribute__((no_instrument_function)) int bitmap_weight(const unsigned long *src, int nbits)
{
 if ((__builtin_constant_p(nbits) && (nbits) <= 64))
  return hweight_long(*src & ( ((nbits) % 64) ? (1UL<<((nbits) % 64))-1 : ~0UL ));
 return __bitmap_weight(src, nbits);
}

static inline __attribute__((no_instrument_function)) void bitmap_shift_right(unsigned long *dst,
   const unsigned long *src, int n, int nbits)
{
 if ((__builtin_constant_p(nbits) && (nbits) <= 64))
  *dst = *src >> n;
 else
  __bitmap_shift_right(dst, src, n, nbits);
}

static inline __attribute__((no_instrument_function)) void bitmap_shift_left(unsigned long *dst,
   const unsigned long *src, int n, int nbits)
{
 if ((__builtin_constant_p(nbits) && (nbits) <= 64))
  *dst = (*src << n) & ( ((nbits) % 64) ? (1UL<<((nbits) % 64))-1 : ~0UL );
 else
  __bitmap_shift_left(dst, src, n, nbits);
}

static inline __attribute__((no_instrument_function)) int bitmap_parse(const char *buf, unsigned int buflen,
   unsigned long *maskp, int nmaskbits)
{
 return __bitmap_parse(buf, buflen, 0, maskp, nmaskbits);
}
# 12 "include/linux/cpumask.h" 2
# 1 "include/linux/bug.h" 1



# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bug.h" 1
# 35 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bug.h"
# 1 "include/asm-generic/bug.h" 1
# 18 "include/asm-generic/bug.h"
struct bug_entry {



 signed int bug_addr_disp;





 signed int file_disp;

 unsigned short line;

 unsigned short flags;
};
# 65 "include/asm-generic/bug.h"
extern __attribute__((format(printf, 3, 4)))
void warn_slowpath_fmt(const char *file, const int line,
         const char *fmt, ...);
extern __attribute__((format(printf, 4, 5)))
void warn_slowpath_fmt_taint(const char *file, const int line, unsigned taint,
        const char *fmt, ...);
extern void warn_slowpath_null(const char *file, const int line);
# 36 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/bug.h" 2
# 5 "include/linux/bug.h" 2


enum bug_trap_type {
 BUG_TRAP_TYPE_NONE = 0,
 BUG_TRAP_TYPE_WARN = 1,
 BUG_TRAP_TYPE_BUG = 2,
};

struct pt_regs;
# 91 "include/linux/bug.h"
static inline __attribute__((no_instrument_function)) int is_warning_bug(const struct bug_entry *bug)
{
 return bug->flags & (1 << 0);
}

const struct bug_entry *find_bug(unsigned long bugaddr);

enum bug_trap_type report_bug(unsigned long bug_addr, struct pt_regs *regs);


int is_valid_bugaddr(unsigned long addr);
# 13 "include/linux/cpumask.h" 2

typedef struct cpumask { unsigned long bits[(((8192) + (8 * sizeof(long)) - 1) / (8 * sizeof(long)))]; } cpumask_t;
# 28 "include/linux/cpumask.h"
extern int nr_cpu_ids;
# 79 "include/linux/cpumask.h"
extern const struct cpumask *const cpu_possible_mask;
extern const struct cpumask *const cpu_online_mask;
extern const struct cpumask *const cpu_present_mask;
extern const struct cpumask *const cpu_active_mask;
# 105 "include/linux/cpumask.h"
static inline __attribute__((no_instrument_function)) unsigned int cpumask_check(unsigned int cpu)
{

 ({ static bool __attribute__ ((__section__(".data.unlikely"))) __warned; int __ret_warn_once = !!(cpu >= nr_cpu_ids); if (__builtin_expect(!!(__ret_warn_once), 0)) if (({ int __ret_warn_on = !!(!__warned); if (__builtin_expect(!!(__ret_warn_on), 0)) warn_slowpath_null("include/linux/cpumask.h", 108); __builtin_expect(!!(__ret_warn_on), 0); })) __warned = true; __builtin_expect(!!(__ret_warn_once), 0); });

 return cpu;
}
# 158 "include/linux/cpumask.h"
static inline __attribute__((no_instrument_function)) unsigned int cpumask_first(const struct cpumask *srcp)
{
 return find_first_bit(((srcp)->bits), nr_cpu_ids);
}
# 170 "include/linux/cpumask.h"
static inline __attribute__((no_instrument_function)) unsigned int cpumask_next(int n, const struct cpumask *srcp)
{

 if (n != -1)
  cpumask_check(n);
 return find_next_bit(((srcp)->bits), nr_cpu_ids, n+1);
}
# 185 "include/linux/cpumask.h"
static inline __attribute__((no_instrument_function)) unsigned int cpumask_next_zero(int n, const struct cpumask *srcp)
{

 if (n != -1)
  cpumask_check(n);
 return find_next_zero_bit(((srcp)->bits), nr_cpu_ids, n+1);
}

int cpumask_next_and(int n, const struct cpumask *, const struct cpumask *);
int cpumask_any_but(const struct cpumask *mask, unsigned int cpu);
# 255 "include/linux/cpumask.h"
static inline __attribute__((no_instrument_function)) void cpumask_set_cpu(unsigned int cpu, struct cpumask *dstp)
{
 set_bit(cpumask_check(cpu), ((dstp)->bits));
}






static inline __attribute__((no_instrument_function)) void cpumask_clear_cpu(int cpu, struct cpumask *dstp)
{
 clear_bit(cpumask_check(cpu), ((dstp)->bits));
}
# 291 "include/linux/cpumask.h"
static inline __attribute__((no_instrument_function)) int cpumask_test_and_set_cpu(int cpu, struct cpumask *cpumask)
{
 return test_and_set_bit(cpumask_check(cpu), ((cpumask)->bits));
}
# 305 "include/linux/cpumask.h"
static inline __attribute__((no_instrument_function)) int cpumask_test_and_clear_cpu(int cpu, struct cpumask *cpumask)
{
 return test_and_clear_bit(cpumask_check(cpu), ((cpumask)->bits));
}





static inline __attribute__((no_instrument_function)) void cpumask_setall(struct cpumask *dstp)
{
 bitmap_fill(((dstp)->bits), nr_cpu_ids);
}





static inline __attribute__((no_instrument_function)) void cpumask_clear(struct cpumask *dstp)
{
 bitmap_zero(((dstp)->bits), nr_cpu_ids);
}
# 336 "include/linux/cpumask.h"
static inline __attribute__((no_instrument_function)) int cpumask_and(struct cpumask *dstp,
          const struct cpumask *src1p,
          const struct cpumask *src2p)
{
 return bitmap_and(((dstp)->bits), ((src1p)->bits),
           ((src2p)->bits), nr_cpu_ids);
}







static inline __attribute__((no_instrument_function)) void cpumask_or(struct cpumask *dstp, const struct cpumask *src1p,
         const struct cpumask *src2p)
{
 bitmap_or(((dstp)->bits), ((src1p)->bits),
          ((src2p)->bits), nr_cpu_ids);
}







static inline __attribute__((no_instrument_function)) void cpumask_xor(struct cpumask *dstp,
          const struct cpumask *src1p,
          const struct cpumask *src2p)
{
 bitmap_xor(((dstp)->bits), ((src1p)->bits),
           ((src2p)->bits), nr_cpu_ids);
}
# 379 "include/linux/cpumask.h"
static inline __attribute__((no_instrument_function)) int cpumask_andnot(struct cpumask *dstp,
      const struct cpumask *src1p,
      const struct cpumask *src2p)
{
 return bitmap_andnot(((dstp)->bits), ((src1p)->bits),
       ((src2p)->bits), nr_cpu_ids);
}






static inline __attribute__((no_instrument_function)) void cpumask_complement(struct cpumask *dstp,
          const struct cpumask *srcp)
{
 bitmap_complement(((dstp)->bits), ((srcp)->bits),
           nr_cpu_ids);
}






static inline __attribute__((no_instrument_function)) bool cpumask_equal(const struct cpumask *src1p,
    const struct cpumask *src2p)
{
 return bitmap_equal(((src1p)->bits), ((src2p)->bits),
       nr_cpu_ids);
}






static inline __attribute__((no_instrument_function)) bool cpumask_intersects(const struct cpumask *src1p,
         const struct cpumask *src2p)
{
 return bitmap_intersects(((src1p)->bits), ((src2p)->bits),
            nr_cpu_ids);
}
# 430 "include/linux/cpumask.h"
static inline __attribute__((no_instrument_function)) int cpumask_subset(const struct cpumask *src1p,
     const struct cpumask *src2p)
{
 return bitmap_subset(((src1p)->bits), ((src2p)->bits),
        nr_cpu_ids);
}





static inline __attribute__((no_instrument_function)) bool cpumask_empty(const struct cpumask *srcp)
{
 return bitmap_empty(((srcp)->bits), nr_cpu_ids);
}





static inline __attribute__((no_instrument_function)) bool cpumask_full(const struct cpumask *srcp)
{
 return bitmap_full(((srcp)->bits), nr_cpu_ids);
}





static inline __attribute__((no_instrument_function)) unsigned int cpumask_weight(const struct cpumask *srcp)
{
 return bitmap_weight(((srcp)->bits), nr_cpu_ids);
}







static inline __attribute__((no_instrument_function)) void cpumask_shift_right(struct cpumask *dstp,
           const struct cpumask *srcp, int n)
{
 bitmap_shift_right(((dstp)->bits), ((srcp)->bits), n,
            nr_cpu_ids);
}







static inline __attribute__((no_instrument_function)) void cpumask_shift_left(struct cpumask *dstp,
          const struct cpumask *srcp, int n)
{
 bitmap_shift_left(((dstp)->bits), ((srcp)->bits), n,
           nr_cpu_ids);
}






static inline __attribute__((no_instrument_function)) void cpumask_copy(struct cpumask *dstp,
    const struct cpumask *srcp)
{
 bitmap_copy(((dstp)->bits), ((srcp)->bits), nr_cpu_ids);
}
# 542 "include/linux/cpumask.h"
static inline __attribute__((no_instrument_function)) int cpumask_scnprintf(char *buf, int len,
        const struct cpumask *srcp)
{
 return bitmap_scnprintf(buf, len, ((srcp)->bits), nr_cpu_ids);
}
# 556 "include/linux/cpumask.h"
static inline __attribute__((no_instrument_function)) int cpumask_parse_user(const char *buf, int len,
         struct cpumask *dstp)
{
 return bitmap_parse_user(buf, len, ((dstp)->bits), nr_cpu_ids);
}
# 570 "include/linux/cpumask.h"
static inline __attribute__((no_instrument_function)) int cpumask_parselist_user(const char *buf, int len,
         struct cpumask *dstp)
{
 return bitmap_parselist_user(buf, len, ((dstp)->bits),
       nr_cpu_ids);
}
# 586 "include/linux/cpumask.h"
static inline __attribute__((no_instrument_function)) int cpulist_scnprintf(char *buf, int len,
        const struct cpumask *srcp)
{
 return bitmap_scnlistprintf(buf, len, ((srcp)->bits),
        nr_cpu_ids);
}
# 600 "include/linux/cpumask.h"
static inline __attribute__((no_instrument_function)) int cpumask_parse(const char *buf, struct cpumask *dstp)
{
 char *nl = strchr(buf, '\n');
 int len = nl ? nl - buf : strlen(buf);

 return bitmap_parse(buf, len, ((dstp)->bits), nr_cpu_ids);
}
# 615 "include/linux/cpumask.h"
static inline __attribute__((no_instrument_function)) int cpulist_parse(const char *buf, struct cpumask *dstp)
{
 return bitmap_parselist(buf, ((dstp)->bits), nr_cpu_ids);
}






static inline __attribute__((no_instrument_function)) size_t cpumask_size(void)
{


 return (((8192) + (8 * sizeof(long)) - 1) / (8 * sizeof(long))) * sizeof(long);
}
# 663 "include/linux/cpumask.h"
typedef struct cpumask *cpumask_var_t;

bool alloc_cpumask_var_node(cpumask_var_t *mask, gfp_t flags, int node);
bool alloc_cpumask_var(cpumask_var_t *mask, gfp_t flags);
bool zalloc_cpumask_var_node(cpumask_var_t *mask, gfp_t flags, int node);
bool zalloc_cpumask_var(cpumask_var_t *mask, gfp_t flags);
void alloc_bootmem_cpumask_var(cpumask_var_t *mask);
void free_cpumask_var(cpumask_var_t mask);
void free_bootmem_cpumask_var(cpumask_var_t mask);
# 715 "include/linux/cpumask.h"
extern const unsigned long cpu_all_bits[(((8192) + (8 * sizeof(long)) - 1) / (8 * sizeof(long)))];
# 726 "include/linux/cpumask.h"
void set_cpu_possible(unsigned int cpu, bool possible);
void set_cpu_present(unsigned int cpu, bool present);
void set_cpu_online(unsigned int cpu, bool online);
void set_cpu_active(unsigned int cpu, bool active);
void init_cpu_present(const struct cpumask *src);
void init_cpu_possible(const struct cpumask *src);
void init_cpu_online(const struct cpumask *src);
# 748 "include/linux/cpumask.h"
static inline __attribute__((no_instrument_function)) int __check_is_bitmap(const unsigned long *bitmap)
{
 return 1;
}
# 760 "include/linux/cpumask.h"
extern const unsigned long
 cpu_bit_bitmap[64 +1][(((8192) + (8 * sizeof(long)) - 1) / (8 * sizeof(long)))];

static inline __attribute__((no_instrument_function)) const struct cpumask *get_cpu_mask(unsigned int cpu)
{
 const unsigned long *p = cpu_bit_bitmap[1 + cpu % 64];
 p -= cpu / 64;
 return ((struct cpumask *)(1 ? (p) : (void *)sizeof(__check_is_bitmap(p))));
}
# 831 "include/linux/cpumask.h"
int __first_cpu(const cpumask_t *srcp);
int __next_cpu(int n, const cpumask_t *srcp);
# 849 "include/linux/cpumask.h"
int __next_cpu_nr(int n, const cpumask_t *srcp);
# 860 "include/linux/cpumask.h"
static inline __attribute__((no_instrument_function)) void __cpu_set(int cpu, volatile cpumask_t *dstp)
{
 set_bit(cpu, dstp->bits);
}


static inline __attribute__((no_instrument_function)) void __cpu_clear(int cpu, volatile cpumask_t *dstp)
{
 clear_bit(cpu, dstp->bits);
}


static inline __attribute__((no_instrument_function)) void __cpus_setall(cpumask_t *dstp, int nbits)
{
 bitmap_fill(dstp->bits, nbits);
}


static inline __attribute__((no_instrument_function)) void __cpus_clear(cpumask_t *dstp, int nbits)
{
 bitmap_zero(dstp->bits, nbits);
}





static inline __attribute__((no_instrument_function)) int __cpu_test_and_set(int cpu, cpumask_t *addr)
{
 return test_and_set_bit(cpu, addr->bits);
}


static inline __attribute__((no_instrument_function)) int __cpus_and(cpumask_t *dstp, const cpumask_t *src1p,
     const cpumask_t *src2p, int nbits)
{
 return bitmap_and(dstp->bits, src1p->bits, src2p->bits, nbits);
}


static inline __attribute__((no_instrument_function)) void __cpus_or(cpumask_t *dstp, const cpumask_t *src1p,
     const cpumask_t *src2p, int nbits)
{
 bitmap_or(dstp->bits, src1p->bits, src2p->bits, nbits);
}


static inline __attribute__((no_instrument_function)) void __cpus_xor(cpumask_t *dstp, const cpumask_t *src1p,
     const cpumask_t *src2p, int nbits)
{
 bitmap_xor(dstp->bits, src1p->bits, src2p->bits, nbits);
}



static inline __attribute__((no_instrument_function)) int __cpus_andnot(cpumask_t *dstp, const cpumask_t *src1p,
     const cpumask_t *src2p, int nbits)
{
 return bitmap_andnot(dstp->bits, src1p->bits, src2p->bits, nbits);
}


static inline __attribute__((no_instrument_function)) int __cpus_equal(const cpumask_t *src1p,
     const cpumask_t *src2p, int nbits)
{
 return bitmap_equal(src1p->bits, src2p->bits, nbits);
}


static inline __attribute__((no_instrument_function)) int __cpus_intersects(const cpumask_t *src1p,
     const cpumask_t *src2p, int nbits)
{
 return bitmap_intersects(src1p->bits, src2p->bits, nbits);
}


static inline __attribute__((no_instrument_function)) int __cpus_subset(const cpumask_t *src1p,
     const cpumask_t *src2p, int nbits)
{
 return bitmap_subset(src1p->bits, src2p->bits, nbits);
}


static inline __attribute__((no_instrument_function)) int __cpus_empty(const cpumask_t *srcp, int nbits)
{
 return bitmap_empty(srcp->bits, nbits);
}


static inline __attribute__((no_instrument_function)) int __cpus_weight(const cpumask_t *srcp, int nbits)
{
 return bitmap_weight(srcp->bits, nbits);
}



static inline __attribute__((no_instrument_function)) void __cpus_shift_left(cpumask_t *dstp,
     const cpumask_t *srcp, int n, int nbits)
{
 bitmap_shift_left(dstp->bits, srcp->bits, n, nbits);
}
# 5 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/cpumask.h" 2

extern cpumask_var_t cpu_callin_mask;
extern cpumask_var_t cpu_callout_mask;
extern cpumask_var_t cpu_initialized_mask;
extern cpumask_var_t cpu_sibling_setup_mask;

extern void setup_cpu_local_masks(void);
# 11 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/msr.h" 2

struct msr {
 union {
  struct {
   u32 l;
   u32 h;
  };
  u64 q;
 };
};

struct msr_info {
 u32 msr_no;
 struct msr reg;
 struct msr *msrs;
 int err;
};

struct msr_regs_info {
 u32 *regs;
 int err;
};

static inline __attribute__((no_instrument_function)) unsigned long long native_read_tscp(unsigned int *aux)
{
 unsigned long low, high;
 asm volatile(".byte 0x0f,0x01,0xf9"
       : "=a" (low), "=d" (high), "=c" (*aux));
 return low | ((u64)high << 32);
}
# 60 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/msr.h"
static inline __attribute__((no_instrument_function)) unsigned long long native_read_msr(unsigned int msr)
{
 unsigned low, high;

 asm volatile("rdmsr" : "=a" (low), "=d" (high) : "c" (msr));
 return ((low) | ((u64)(high) << 32));
}

static inline __attribute__((no_instrument_function)) unsigned long long native_read_msr_safe(unsigned int msr,
            int *err)
{
 unsigned low, high;

 asm volatile("2: rdmsr ; xor %[err],%[err]\n"
       "1:\n\t"
       ".section .fixup,\"ax\"\n\t"
       "3:  mov %[fault],%[err] ; jmp 1b\n\t"
       ".previous\n\t"
       " .pushsection \"__ex_table\",\"a\"\n" " .balign 8\n" " .long (" "2b" ") - .\n" " .long (" "3b" ") - .\n" " .popsection\n"
       : [err] "=r" (*err), "=a" (low), "=d" (high)
       : "c" (msr), [fault] "i" (-5));
 return ((low) | ((u64)(high) << 32));
}

static inline __attribute__((no_instrument_function)) void native_write_msr(unsigned int msr,
        unsigned low, unsigned high)
{
 asm volatile("wrmsr" : : "c" (msr), "a"(low), "d" (high) : "memory");
}


__attribute__((no_instrument_function)) static inline __attribute__((no_instrument_function)) int native_write_msr_safe(unsigned int msr,
     unsigned low, unsigned high)
{
 int err;
 asm volatile("2: wrmsr ; xor %[err],%[err]\n"
       "1:\n\t"
       ".section .fixup,\"ax\"\n\t"
       "3:  mov %[fault],%[err] ; jmp 1b\n\t"
       ".previous\n\t"
       " .pushsection \"__ex_table\",\"a\"\n" " .balign 8\n" " .long (" "2b" ") - .\n" " .long (" "3b" ") - .\n" " .popsection\n"
       : [err] "=a" (err)
       : "c" (msr), "0" (low), "d" (high),
         [fault] "i" (-5)
       : "memory");
 return err;
}

extern unsigned long long native_read_tsc(void);

extern int rdmsr_safe_regs(u32 regs[8]);
extern int wrmsr_safe_regs(u32 regs[8]);

static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) unsigned long long __native_read_tsc(void)
{
 unsigned low, high;

 asm volatile("rdtsc" : "=a" (low), "=d" (high));

 return ((low) | ((u64)(high) << 32));
}

static inline __attribute__((no_instrument_function)) unsigned long long native_read_pmc(int counter)
{
 unsigned low, high;

 asm volatile("rdpmc" : "=a" (low), "=d" (high) : "c" (counter));
 return ((low) | ((u64)(high) << 32));
}


# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h" 1
# 17 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
static inline __attribute__((no_instrument_function)) int paravirt_enabled(void)
{
 return pv_info.paravirt_enabled;
}

static inline __attribute__((no_instrument_function)) void load_sp0(struct tss_struct *tss,
        struct thread_struct *thread)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.load_sp0 == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (25), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.load_sp0) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.load_sp0)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(tss)), "S" ((unsigned long)(thread)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}


static inline __attribute__((no_instrument_function)) void __cpuid(unsigned int *eax, unsigned int *ebx,
      unsigned int *ecx, unsigned int *edx)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.cpuid == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (32), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.cpuid) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.cpuid)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(eax)), "S" ((unsigned long)(ebx)), "d" ((unsigned long)(ecx)), "c" ((unsigned long)(edx)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}




static inline __attribute__((no_instrument_function)) unsigned long paravirt_get_debugreg(int reg)
{
 return ({ unsigned long __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.get_debugreg == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (40), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(unsigned long) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.get_debugreg) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.get_debugreg)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(reg)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (unsigned long)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.get_debugreg) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.get_debugreg)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(reg)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (unsigned long)__eax; } __ret; });
}

static inline __attribute__((no_instrument_function)) void set_debugreg(unsigned long val, int reg)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.set_debugreg == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (45), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.set_debugreg) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.set_debugreg)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(reg)), "S" ((unsigned long)(val)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void clts(void)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.clts == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (50), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.clts) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.clts)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) unsigned long read_cr0(void)
{
 return ({ unsigned long __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.read_cr0 == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (55), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(unsigned long) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.read_cr0) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.read_cr0)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (unsigned long)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.read_cr0) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.read_cr0)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (unsigned long)__eax; } __ret; });
}

static inline __attribute__((no_instrument_function)) void write_cr0(unsigned long x)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.write_cr0 == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (60), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.write_cr0) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.write_cr0)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(x)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) unsigned long read_cr2(void)
{
 return ({ unsigned long __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.read_cr2 == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (65), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(unsigned long) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.read_cr2) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.read_cr2)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (unsigned long)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.read_cr2) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.read_cr2)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (unsigned long)__eax; } __ret; });
}

static inline __attribute__((no_instrument_function)) void write_cr2(unsigned long x)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.write_cr2 == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (70), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.write_cr2) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.write_cr2)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(x)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) unsigned long read_cr3(void)
{
 return ({ unsigned long __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.read_cr3 == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (75), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(unsigned long) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.read_cr3) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.read_cr3)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (unsigned long)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.read_cr3) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.read_cr3)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (unsigned long)__eax; } __ret; });
}

static inline __attribute__((no_instrument_function)) void write_cr3(unsigned long x)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.write_cr3 == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (80), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.write_cr3) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.write_cr3)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(x)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) unsigned long read_cr4(void)
{
 return ({ unsigned long __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.read_cr4 == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (85), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(unsigned long) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.read_cr4) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.read_cr4)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (unsigned long)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.read_cr4) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.read_cr4)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (unsigned long)__eax; } __ret; });
}
static inline __attribute__((no_instrument_function)) unsigned long read_cr4_safe(void)
{
 return ({ unsigned long __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.read_cr4_safe == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (89), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(unsigned long) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.read_cr4_safe) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.read_cr4_safe)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (unsigned long)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.read_cr4_safe) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.read_cr4_safe)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (unsigned long)__eax; } __ret; });
}

static inline __attribute__((no_instrument_function)) void write_cr4(unsigned long x)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.write_cr4 == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (94), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.write_cr4) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.write_cr4)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(x)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}


static inline __attribute__((no_instrument_function)) unsigned long read_cr8(void)
{
 return ({ unsigned long __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.read_cr8 == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (100), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(unsigned long) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.read_cr8) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.read_cr8)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (unsigned long)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.read_cr8) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.read_cr8)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (unsigned long)__eax; } __ret; });
}

static inline __attribute__((no_instrument_function)) void write_cr8(unsigned long x)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.write_cr8 == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (105), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.write_cr8) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.write_cr8)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(x)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}


static inline __attribute__((no_instrument_function)) void arch_safe_halt(void)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_irq_ops.safe_halt == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (111), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_irq_ops.safe_halt) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_irq_ops.safe_halt)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void halt(void)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_irq_ops.halt == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (116), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_irq_ops.halt) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_irq_ops.halt)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void wbinvd(void)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.wbinvd == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (121), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.wbinvd) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.wbinvd)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}



static inline __attribute__((no_instrument_function)) u64 paravirt_read_msr(unsigned msr, int *err)
{
 return ({ u64 __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.read_msr == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (128), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(u64) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.read_msr) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.read_msr)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(msr)), "S" ((unsigned long)(err)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (u64)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.read_msr) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.read_msr)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(msr)), "S" ((unsigned long)(err)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (u64)__eax; } __ret; });
}

static inline __attribute__((no_instrument_function)) int paravirt_write_msr(unsigned msr, unsigned low, unsigned high)
{
 return ({ int __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.write_msr == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (133), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(int) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.write_msr) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.write_msr)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(msr)), "S" ((unsigned long)(low)), "d" ((unsigned long)(high)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (int)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.write_msr) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.write_msr)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(msr)), "S" ((unsigned long)(low)), "d" ((unsigned long)(high)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (int)__eax; } __ret; });
}
# 169 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
static inline __attribute__((no_instrument_function)) int rdmsrl_safe(unsigned msr, unsigned long long *p)
{
 int err;

 *p = paravirt_read_msr(msr, &err);
 return err;
}

static inline __attribute__((no_instrument_function)) u64 paravirt_read_tsc(void)
{
 return ({ u64 __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.read_tsc == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (179), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(u64) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.read_tsc) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.read_tsc)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (u64)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.read_tsc) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.read_tsc)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (u64)__eax; } __ret; });
}
# 190 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
static inline __attribute__((no_instrument_function)) unsigned long long paravirt_sched_clock(void)
{
 return ({ unsigned long long __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_time_ops.sched_clock == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (192), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(unsigned long long) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_time_ops.sched_clock) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_time_ops.sched_clock)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (unsigned long long)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_time_ops.sched_clock) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_time_ops.sched_clock)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (unsigned long long)__eax; } __ret; });
}

struct static_key;
extern struct static_key paravirt_steal_enabled;
extern struct static_key paravirt_steal_rq_enabled;

static inline __attribute__((no_instrument_function)) u64 paravirt_steal_clock(int cpu)
{
 return ({ u64 __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_time_ops.steal_clock == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (201), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(u64) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_time_ops.steal_clock) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_time_ops.steal_clock)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(cpu)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (u64)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_time_ops.steal_clock) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_time_ops.steal_clock)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(cpu)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (u64)__eax; } __ret; });
}

static inline __attribute__((no_instrument_function)) unsigned long long paravirt_read_pmc(int counter)
{
 return ({ u64 __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.read_pmc == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (206), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(u64) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.read_pmc) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.read_pmc)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(counter)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (u64)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.read_pmc) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.read_pmc)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(counter)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (u64)__eax; } __ret; });
}
# 218 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
static inline __attribute__((no_instrument_function)) unsigned long long paravirt_rdtscp(unsigned int *aux)
{
 return ({ u64 __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.read_tscp == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (220), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(u64) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.read_tscp) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.read_tscp)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(aux)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (u64)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.read_tscp) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.read_tscp)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(aux)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (u64)__eax; } __ret; });
}
# 239 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
static inline __attribute__((no_instrument_function)) void paravirt_alloc_ldt(struct desc_struct *ldt, unsigned entries)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.alloc_ldt == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (241), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.alloc_ldt) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.alloc_ldt)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(ldt)), "S" ((unsigned long)(entries)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void paravirt_free_ldt(struct desc_struct *ldt, unsigned entries)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.free_ldt == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (246), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.free_ldt) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.free_ldt)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(ldt)), "S" ((unsigned long)(entries)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void load_TR_desc(void)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.load_tr_desc == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (251), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.load_tr_desc) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.load_tr_desc)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}
static inline __attribute__((no_instrument_function)) void load_gdt(const struct desc_ptr *dtr)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.load_gdt == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (255), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.load_gdt) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.load_gdt)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(dtr)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}
static inline __attribute__((no_instrument_function)) void load_idt(const struct desc_ptr *dtr)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.load_idt == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (259), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.load_idt) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.load_idt)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(dtr)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}
static inline __attribute__((no_instrument_function)) void set_ldt(const void *addr, unsigned entries)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.set_ldt == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (263), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.set_ldt) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.set_ldt)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(addr)), "S" ((unsigned long)(entries)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}
static inline __attribute__((no_instrument_function)) void store_idt(struct desc_ptr *dtr)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.store_idt == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (267), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.store_idt) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.store_idt)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(dtr)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}
static inline __attribute__((no_instrument_function)) unsigned long paravirt_store_tr(void)
{
 return ({ unsigned long __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.store_tr == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (271), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(unsigned long) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.store_tr) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.store_tr)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (unsigned long)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.store_tr) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.store_tr)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (unsigned long)__eax; } __ret; });
}

static inline __attribute__((no_instrument_function)) void load_TLS(struct thread_struct *t, unsigned cpu)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.load_tls == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (276), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.load_tls) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.load_tls)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(t)), "S" ((unsigned long)(cpu)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}


static inline __attribute__((no_instrument_function)) void load_gs_index(unsigned int gs)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.load_gs_index == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (282), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.load_gs_index) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.load_gs_index)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(gs)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}


static inline __attribute__((no_instrument_function)) void write_ldt_entry(struct desc_struct *dt, int entry,
       const void *desc)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.write_ldt_entry == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (289), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.write_ldt_entry) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.write_ldt_entry)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(dt)), "S" ((unsigned long)(entry)), "d" ((unsigned long)(desc)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void write_gdt_entry(struct desc_struct *dt, int entry,
       void *desc, int type)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.write_gdt_entry == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (295), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.write_gdt_entry) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.write_gdt_entry)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(dt)), "S" ((unsigned long)(entry)), "d" ((unsigned long)(desc)), "c" ((unsigned long)(type)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void write_idt_entry(gate_desc *dt, int entry, const gate_desc *g)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.write_idt_entry == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (300), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.write_idt_entry) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.write_idt_entry)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(dt)), "S" ((unsigned long)(entry)), "d" ((unsigned long)(g)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}
static inline __attribute__((no_instrument_function)) void set_iopl_mask(unsigned mask)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.set_iopl_mask == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (304), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.set_iopl_mask) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.set_iopl_mask)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(mask)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}


static inline __attribute__((no_instrument_function)) void slow_down_io(void)
{
 pv_cpu_ops.io_delay();





}


static inline __attribute__((no_instrument_function)) void startup_ipi_hook(int phys_apicid, unsigned long start_eip,
        unsigned long start_esp)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_apic_ops.startup_ipi_hook == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 322 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
 ), "i" (
 323
# 322 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
 ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_apic_ops.startup_ipi_hook) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_apic_ops.startup_ipi_hook)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(phys_apicid)), "S" ((unsigned long)(start_eip)), "d" ((unsigned long)(start_esp)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); })
                                        ;
}


static inline __attribute__((no_instrument_function)) void paravirt_activate_mm(struct mm_struct *prev,
     struct mm_struct *next)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.activate_mm == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (330), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.activate_mm) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.activate_mm)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(prev)), "S" ((unsigned long)(next)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void arch_dup_mmap(struct mm_struct *oldmm,
     struct mm_struct *mm)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.dup_mmap == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (336), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.dup_mmap) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.dup_mmap)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(oldmm)), "S" ((unsigned long)(mm)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void arch_exit_mmap(struct mm_struct *mm)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.exit_mmap == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (341), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.exit_mmap) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.exit_mmap)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(mm)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void __flush_tlb(void)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.flush_tlb_user == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (346), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.flush_tlb_user) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.flush_tlb_user)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}
static inline __attribute__((no_instrument_function)) void __flush_tlb_global(void)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.flush_tlb_kernel == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (350), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.flush_tlb_kernel) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.flush_tlb_kernel)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}
static inline __attribute__((no_instrument_function)) void __flush_tlb_single(unsigned long addr)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.flush_tlb_single == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (354), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.flush_tlb_single) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.flush_tlb_single)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(addr)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void flush_tlb_others(const struct cpumask *cpumask,
        struct mm_struct *mm,
        unsigned long start,
        unsigned long end)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.flush_tlb_others == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (362), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.flush_tlb_others) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.flush_tlb_others)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(cpumask)), "S" ((unsigned long)(mm)), "d" ((unsigned long)(start)), "c" ((unsigned long)(end)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) int paravirt_pgd_alloc(struct mm_struct *mm)
{
 return ({ int __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.pgd_alloc == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (367), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(int) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pgd_alloc) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pgd_alloc)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(mm)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (int)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pgd_alloc) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pgd_alloc)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(mm)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (int)__eax; } __ret; });
}

static inline __attribute__((no_instrument_function)) void paravirt_pgd_free(struct mm_struct *mm, pgd_t *pgd)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.pgd_free == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (372), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pgd_free) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pgd_free)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(mm)), "S" ((unsigned long)(pgd)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void paravirt_alloc_pte(struct mm_struct *mm, unsigned long pfn)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.alloc_pte == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (377), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.alloc_pte) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.alloc_pte)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(mm)), "S" ((unsigned long)(pfn)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}
static inline __attribute__((no_instrument_function)) void paravirt_release_pte(unsigned long pfn)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.release_pte == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (381), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.release_pte) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.release_pte)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(pfn)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void paravirt_alloc_pmd(struct mm_struct *mm, unsigned long pfn)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.alloc_pmd == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (386), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.alloc_pmd) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.alloc_pmd)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(mm)), "S" ((unsigned long)(pfn)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void paravirt_release_pmd(unsigned long pfn)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.release_pmd == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (391), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.release_pmd) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.release_pmd)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(pfn)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void paravirt_alloc_pud(struct mm_struct *mm, unsigned long pfn)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.alloc_pud == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (396), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.alloc_pud) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.alloc_pud)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(mm)), "S" ((unsigned long)(pfn)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}
static inline __attribute__((no_instrument_function)) void paravirt_release_pud(unsigned long pfn)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.release_pud == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (400), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.release_pud) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.release_pud)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(pfn)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void pte_update(struct mm_struct *mm, unsigned long addr,
         pte_t *ptep)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.pte_update == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (406), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pte_update) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pte_update)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(mm)), "S" ((unsigned long)(addr)), "d" ((unsigned long)(ptep)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}
static inline __attribute__((no_instrument_function)) void pmd_update(struct mm_struct *mm, unsigned long addr,
         pmd_t *pmdp)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.pmd_update == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (411), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pmd_update) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pmd_update)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(mm)), "S" ((unsigned long)(addr)), "d" ((unsigned long)(pmdp)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void pte_update_defer(struct mm_struct *mm, unsigned long addr,
        pte_t *ptep)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.pte_update_defer == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (417), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pte_update_defer) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pte_update_defer)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(mm)), "S" ((unsigned long)(addr)), "d" ((unsigned long)(ptep)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void pmd_update_defer(struct mm_struct *mm, unsigned long addr,
        pmd_t *pmdp)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.pmd_update_defer == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (423), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pmd_update_defer) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pmd_update_defer)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(mm)), "S" ((unsigned long)(addr)), "d" ((unsigned long)(pmdp)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) pte_t __pte(pteval_t val)
{
 pteval_t ret;

 if (sizeof(pteval_t) > sizeof(long))
  ret = ({ pteval_t __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.make_pte.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (

 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 431 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
        ), "i" (

 433
# 431 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
        ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(pteval_t) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.make_pte.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.make_pte.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(val)), "S" ((unsigned long)((u64)val >> 32)) : "memory", "cc" ); __ret = (pteval_t)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.make_pte.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.make_pte.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(val)), "S" ((unsigned long)((u64)val >> 32)) : "memory", "cc" ); __ret = (pteval_t)__eax; } __ret; })

                           ;
 else
  ret = ({ pteval_t __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.make_pte.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (

 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 435 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
        ), "i" (

 437
# 435 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
        ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(pteval_t) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.make_pte.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.make_pte.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(val)) : "memory", "cc" ); __ret = (pteval_t)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.make_pte.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.make_pte.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(val)) : "memory", "cc" ); __ret = (pteval_t)__eax; } __ret; })

           ;

 return (pte_t) { .pte = ret };
}

static inline __attribute__((no_instrument_function)) pteval_t pte_val(pte_t pte)
{
 pteval_t ret;

 if (sizeof(pteval_t) > sizeof(long))
  ret = ({ pteval_t __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.pte_val.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 447 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
        ), "i" (
 448
# 447 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
        ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(pteval_t) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pte_val.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pte_val.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(pte.pte)), "S" ((unsigned long)((u64)pte.pte >> 32)) : "memory", "cc" ); __ret = (pteval_t)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pte_val.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pte_val.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(pte.pte)), "S" ((unsigned long)((u64)pte.pte >> 32)) : "memory", "cc" ); __ret = (pteval_t)__eax; } __ret; })
                                   ;
 else
  ret = ({ pteval_t __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.pte_val.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 450 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
        ), "i" (
 451
# 450 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
        ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(pteval_t) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pte_val.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pte_val.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(pte.pte)) : "memory", "cc" ); __ret = (pteval_t)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pte_val.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pte_val.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(pte.pte)) : "memory", "cc" ); __ret = (pteval_t)__eax; } __ret; })
               ;

 return ret;
}

static inline __attribute__((no_instrument_function)) pgd_t __pgd(pgdval_t val)
{
 pgdval_t ret;

 if (sizeof(pgdval_t) > sizeof(long))
  ret = ({ pgdval_t __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.make_pgd.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 461 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
        ), "i" (
 462
# 461 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
        ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(pgdval_t) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.make_pgd.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.make_pgd.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(val)), "S" ((unsigned long)((u64)val >> 32)) : "memory", "cc" ); __ret = (pgdval_t)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.make_pgd.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.make_pgd.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(val)), "S" ((unsigned long)((u64)val >> 32)) : "memory", "cc" ); __ret = (pgdval_t)__eax; } __ret; })
                           ;
 else
  ret = ({ pgdval_t __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.make_pgd.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 464 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
        ), "i" (
 465
# 464 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
        ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(pgdval_t) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.make_pgd.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.make_pgd.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(val)) : "memory", "cc" ); __ret = (pgdval_t)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.make_pgd.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.make_pgd.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(val)) : "memory", "cc" ); __ret = (pgdval_t)__eax; } __ret; })
           ;

 return (pgd_t) { ret };
}

static inline __attribute__((no_instrument_function)) pgdval_t pgd_val(pgd_t pgd)
{
 pgdval_t ret;

 if (sizeof(pgdval_t) > sizeof(long))
  ret = ({ pgdval_t __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.pgd_val.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 475 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
         ), "i" (
 476
# 475 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
         ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(pgdval_t) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pgd_val.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pgd_val.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(pgd.pgd)), "S" ((unsigned long)((u64)pgd.pgd >> 32)) : "memory", "cc" ); __ret = (pgdval_t)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pgd_val.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pgd_val.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(pgd.pgd)), "S" ((unsigned long)((u64)pgd.pgd >> 32)) : "memory", "cc" ); __ret = (pgdval_t)__eax; } __ret; })
                                    ;
 else
  ret = ({ pgdval_t __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.pgd_val.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 478 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
         ), "i" (
 479
# 478 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
         ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(pgdval_t) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pgd_val.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pgd_val.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(pgd.pgd)) : "memory", "cc" ); __ret = (pgdval_t)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pgd_val.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pgd_val.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(pgd.pgd)) : "memory", "cc" ); __ret = (pgdval_t)__eax; } __ret; })
                ;

 return ret;
}


static inline __attribute__((no_instrument_function)) pte_t ptep_modify_prot_start(struct mm_struct *mm, unsigned long addr,
        pte_t *ptep)
{
 pteval_t ret;

 ret = ({ pteval_t __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.ptep_modify_prot_start == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 490 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
       ), "i" (
 491
# 490 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
       ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(pteval_t) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.ptep_modify_prot_start) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.ptep_modify_prot_start)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(mm)), "S" ((unsigned long)(addr)), "d" ((unsigned long)(ptep)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (pteval_t)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx), "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.ptep_modify_prot_start) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.ptep_modify_prot_start)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(mm)), "S" ((unsigned long)(addr)), "d" ((unsigned long)(ptep)) : "memory", "cc" , "r8", "r9", "r10", "r11"); __ret = (pteval_t)__eax; } __ret; })
                   ;

 return (pte_t) { .pte = ret };
}

static inline __attribute__((no_instrument_function)) void ptep_modify_prot_commit(struct mm_struct *mm, unsigned long addr,
        pte_t *ptep, pte_t pte)
{
 if (sizeof(pteval_t) > sizeof(long))

  pv_mmu_ops.ptep_modify_prot_commit(mm, addr, ptep, pte);
 else
  ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.ptep_modify_prot_commit == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 503 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
  ), "i" (
 504
# 503 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
  ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.ptep_modify_prot_commit) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.ptep_modify_prot_commit)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(mm)), "S" ((unsigned long)(addr)), "d" ((unsigned long)(ptep)), "c" ((unsigned long)(pte.pte)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); })
                               ;
}

static inline __attribute__((no_instrument_function)) void set_pte(pte_t *ptep, pte_t pte)
{
 if (sizeof(pteval_t) > sizeof(long))
  ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.set_pte == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 510 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
  ), "i" (
 511
# 510 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
  ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.set_pte) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.set_pte)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(ptep)), "S" ((unsigned long)(pte.pte)), "d" ((unsigned long)((u64)pte.pte >> 32)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); })
                                   ;
 else
  ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.set_pte == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 513 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
  ), "i" (
 514
# 513 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
  ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.set_pte) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.set_pte)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(ptep)), "S" ((unsigned long)(pte.pte)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); })
               ;
}

static inline __attribute__((no_instrument_function)) void set_pte_at(struct mm_struct *mm, unsigned long addr,
         pte_t *ptep, pte_t pte)
{
 if (sizeof(pteval_t) > sizeof(long))

  pv_mmu_ops.set_pte_at(mm, addr, ptep, pte);
 else
  ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.set_pte_at == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (524), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.set_pte_at) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.set_pte_at)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(mm)), "S" ((unsigned long)(addr)), "d" ((unsigned long)(ptep)), "c" ((unsigned long)(pte.pte)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void set_pmd_at(struct mm_struct *mm, unsigned long addr,
         pmd_t *pmdp, pmd_t pmd)
{
 if (sizeof(pmdval_t) > sizeof(long))

  pv_mmu_ops.set_pmd_at(mm, addr, pmdp, pmd);
 else
  ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.set_pmd_at == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 534 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
  ), "i" (
 535
# 534 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
  ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.set_pmd_at) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.set_pmd_at)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(mm)), "S" ((unsigned long)(addr)), "d" ((unsigned long)(pmdp)), "c" ((unsigned long)(native_pmd_val(pmd))) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); })
                           ;
}

static inline __attribute__((no_instrument_function)) void set_pmd(pmd_t *pmdp, pmd_t pmd)
{
 pmdval_t val = native_pmd_val(pmd);

 if (sizeof(pmdval_t) > sizeof(long))
  ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.set_pmd == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (543), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.set_pmd) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.set_pmd)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(pmdp)), "S" ((unsigned long)(val)), "d" ((unsigned long)((u64)val >> 32)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
 else
  ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.set_pmd == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (545), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.set_pmd) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.set_pmd)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(pmdp)), "S" ((unsigned long)(val)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}


static inline __attribute__((no_instrument_function)) pmd_t __pmd(pmdval_t val)
{
 pmdval_t ret;

 if (sizeof(pmdval_t) > sizeof(long))
  ret = ({ pmdval_t __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.make_pmd.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 554 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
        ), "i" (
 555
# 554 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
        ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(pmdval_t) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.make_pmd.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.make_pmd.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(val)), "S" ((unsigned long)((u64)val >> 32)) : "memory", "cc" ); __ret = (pmdval_t)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.make_pmd.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.make_pmd.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(val)), "S" ((unsigned long)((u64)val >> 32)) : "memory", "cc" ); __ret = (pmdval_t)__eax; } __ret; })
                           ;
 else
  ret = ({ pmdval_t __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.make_pmd.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 557 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
        ), "i" (
 558
# 557 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
        ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(pmdval_t) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.make_pmd.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.make_pmd.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(val)) : "memory", "cc" ); __ret = (pmdval_t)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.make_pmd.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.make_pmd.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(val)) : "memory", "cc" ); __ret = (pmdval_t)__eax; } __ret; })
           ;

 return (pmd_t) { ret };
}

static inline __attribute__((no_instrument_function)) pmdval_t pmd_val(pmd_t pmd)
{
 pmdval_t ret;

 if (sizeof(pmdval_t) > sizeof(long))
  ret = ({ pmdval_t __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.pmd_val.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 568 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
         ), "i" (
 569
# 568 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
         ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(pmdval_t) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pmd_val.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pmd_val.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(pmd.pmd)), "S" ((unsigned long)((u64)pmd.pmd >> 32)) : "memory", "cc" ); __ret = (pmdval_t)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pmd_val.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pmd_val.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(pmd.pmd)), "S" ((unsigned long)((u64)pmd.pmd >> 32)) : "memory", "cc" ); __ret = (pmdval_t)__eax; } __ret; })
                                    ;
 else
  ret = ({ pmdval_t __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.pmd_val.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 571 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
         ), "i" (
 572
# 571 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
         ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(pmdval_t) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pmd_val.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pmd_val.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(pmd.pmd)) : "memory", "cc" ); __ret = (pmdval_t)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pmd_val.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pmd_val.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(pmd.pmd)) : "memory", "cc" ); __ret = (pmdval_t)__eax; } __ret; })
                ;

 return ret;
}

static inline __attribute__((no_instrument_function)) void set_pud(pud_t *pudp, pud_t pud)
{
 pudval_t val = native_pud_val(pud);

 if (sizeof(pudval_t) > sizeof(long))
  ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.set_pud == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 582 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
  ), "i" (
 583
# 582 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
  ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.set_pud) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.set_pud)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(pudp)), "S" ((unsigned long)(val)), "d" ((unsigned long)((u64)val >> 32)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); })
                           ;
 else
  ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.set_pud == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 585 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
  ), "i" (
 586
# 585 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
  ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.set_pud) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.set_pud)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(pudp)), "S" ((unsigned long)(val)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); })
           ;
}

static inline __attribute__((no_instrument_function)) pud_t __pud(pudval_t val)
{
 pudval_t ret;

 if (sizeof(pudval_t) > sizeof(long))
  ret = ({ pudval_t __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.make_pud.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 594 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
        ), "i" (
 595
# 594 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
        ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(pudval_t) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.make_pud.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.make_pud.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(val)), "S" ((unsigned long)((u64)val >> 32)) : "memory", "cc" ); __ret = (pudval_t)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.make_pud.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.make_pud.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(val)), "S" ((unsigned long)((u64)val >> 32)) : "memory", "cc" ); __ret = (pudval_t)__eax; } __ret; })
                           ;
 else
  ret = ({ pudval_t __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.make_pud.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 597 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
        ), "i" (
 598
# 597 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
        ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(pudval_t) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.make_pud.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.make_pud.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(val)) : "memory", "cc" ); __ret = (pudval_t)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.make_pud.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.make_pud.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(val)) : "memory", "cc" ); __ret = (pudval_t)__eax; } __ret; })
           ;

 return (pud_t) { ret };
}

static inline __attribute__((no_instrument_function)) pudval_t pud_val(pud_t pud)
{
 pudval_t ret;

 if (sizeof(pudval_t) > sizeof(long))
  ret = ({ pudval_t __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.pud_val.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 608 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
         ), "i" (
 609
# 608 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
         ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(pudval_t) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pud_val.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pud_val.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(pud.pud)), "S" ((unsigned long)((u64)pud.pud >> 32)) : "memory", "cc" ); __ret = (pudval_t)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pud_val.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pud_val.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(pud.pud)), "S" ((unsigned long)((u64)pud.pud >> 32)) : "memory", "cc" ); __ret = (pudval_t)__eax; } __ret; })
                                    ;
 else
  ret = ({ pudval_t __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.pud_val.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 611 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
         ), "i" (
 612
# 611 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
         ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(pudval_t) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pud_val.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pud_val.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(pud.pud)) : "memory", "cc" ); __ret = (pudval_t)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.pud_val.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.pud_val.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(pud.pud)) : "memory", "cc" ); __ret = (pudval_t)__eax; } __ret; })
                ;

 return ret;
}

static inline __attribute__((no_instrument_function)) void set_pgd(pgd_t *pgdp, pgd_t pgd)
{
 pgdval_t val = native_pgd_val(pgd);

 if (sizeof(pgdval_t) > sizeof(long))
  ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.set_pgd == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 622 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
  ), "i" (
 623
# 622 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
  ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.set_pgd) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.set_pgd)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(pgdp)), "S" ((unsigned long)(val)), "d" ((unsigned long)((u64)val >> 32)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); })
                           ;
 else
  ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.set_pgd == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" (
 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
# 625 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
  ), "i" (
 626
# 625 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
  ), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.set_pgd) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.set_pgd)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(pgdp)), "S" ((unsigned long)(val)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); })
           ;
}

static inline __attribute__((no_instrument_function)) void pgd_clear(pgd_t *pgdp)
{
 set_pgd(pgdp, __pgd(0));
}

static inline __attribute__((no_instrument_function)) void pud_clear(pud_t *pudp)
{
 set_pud(pudp, __pud(0));
}
# 663 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
static inline __attribute__((no_instrument_function)) void set_pte_atomic(pte_t *ptep, pte_t pte)
{
 set_pte(ptep, pte);
}

static inline __attribute__((no_instrument_function)) void pte_clear(struct mm_struct *mm, unsigned long addr,
        pte_t *ptep)
{
 set_pte_at(mm, addr, ptep, __pte(0));
}

static inline __attribute__((no_instrument_function)) void pmd_clear(pmd_t *pmdp)
{
 set_pmd(pmdp, __pmd(0));
}



static inline __attribute__((no_instrument_function)) void arch_start_context_switch(struct task_struct *prev)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.start_context_switch == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (683), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.start_context_switch) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.start_context_switch)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(prev)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void arch_end_context_switch(struct task_struct *next)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_cpu_ops.end_context_switch == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (688), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_cpu_ops.end_context_switch) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_cpu_ops.end_context_switch)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(next)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}


static inline __attribute__((no_instrument_function)) void arch_enter_lazy_mmu_mode(void)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.lazy_mode.enter == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (694), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.lazy_mode.enter) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.lazy_mode.enter)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void arch_leave_lazy_mmu_mode(void)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.lazy_mode.leave == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (699), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.lazy_mode.leave) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.lazy_mode.leave)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void arch_flush_lazy_mmu_mode(void)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_mmu_ops.lazy_mode.flush == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (704), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_mmu_ops.lazy_mode.flush) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_mmu_ops.lazy_mode.flush)), [paravirt_clobber] "i" (((1 << 9) - 1)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}

static inline __attribute__((no_instrument_function)) void __set_fixmap(unsigned idx,
    phys_addr_t phys, pgprot_t flags)
{
 pv_mmu_ops.set_fixmap(idx, phys, flags);
}



static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) void __ticket_lock_spinning(struct arch_spinlock *lock,
       __ticket_t ticket)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_lock_ops.lock_spinning.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (718), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_lock_ops.lock_spinning.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_lock_ops.lock_spinning.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(lock)), "S" ((unsigned long)(ticket)) : "memory", "cc" ); });
}

static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) void __ticket_unlock_kick(struct arch_spinlock *lock,
       __ticket_t ticket)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_lock_ops.unlock_kick == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (724), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=D" (__edi), "=S" (__esi), "=d" (__edx), "=c" (__ecx) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_lock_ops.unlock_kick) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_lock_ops.unlock_kick)), [paravirt_clobber] "i" (((1 << 9) - 1)), "D" ((unsigned long)(lock)), "S" ((unsigned long)(ticket)) : "memory", "cc" , "rax", "r8", "r9", "r10", "r11"); });
}
# 802 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
static inline __attribute__((no_instrument_function)) __attribute__((no_instrument_function)) unsigned long arch_local_save_flags(void)
{
 return ({ unsigned long __ret; unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_irq_ops.save_fl.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (804), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); if (sizeof(unsigned long) > sizeof(unsigned long)) { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_irq_ops.save_fl.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_irq_ops.save_fl.func)), [paravirt_clobber] "i" (((1 << 0))) : "memory", "cc" ); __ret = (unsigned long)((((u64)__edx) << 32) | __eax); } else { asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_irq_ops.save_fl.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_irq_ops.save_fl.func)), [paravirt_clobber] "i" (((1 << 0))) : "memory", "cc" ); __ret = (unsigned long)__eax; } __ret; });
}

static inline __attribute__((no_instrument_function)) __attribute__((no_instrument_function)) void arch_local_irq_restore(unsigned long f)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_irq_ops.restore_fl.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (809), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_irq_ops.restore_fl.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_irq_ops.restore_fl.func)), [paravirt_clobber] "i" (((1 << 0))), "D" ((unsigned long)(f)) : "memory", "cc" ); });
}

static inline __attribute__((no_instrument_function)) __attribute__((no_instrument_function)) void arch_local_irq_disable(void)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_irq_ops.irq_disable.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (814), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_irq_ops.irq_disable.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_irq_ops.irq_disable.func)), [paravirt_clobber] "i" (((1 << 0))) : "memory", "cc" ); });
}

static inline __attribute__((no_instrument_function)) __attribute__((no_instrument_function)) void arch_local_irq_enable(void)
{
 ({ unsigned long __edi = __edi, __esi = __esi, __edx = __edx, __ecx = __ecx, __eax = __eax; do { if (__builtin_expect(!!(pv_irq_ops.irq_enable.func == ((void *)0)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"), "i" (819), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0); asm volatile("" "771:\n\t" "call *%c[paravirt_opptr];" "\n" "772:\n" ".pushsection .parainstructions,\"a\"\n" " " ".balign 8" " " "\n" " " ".quad" " " " 771b\n" "  .byte " "%c[paravirt_typenum]" "\n" "  .byte 772b-771b\n" "  .short " "%c[paravirt_clobber]" "\n" ".popsection\n" "" : "=a" (__eax) : [paravirt_typenum] "i" ((__builtin_offsetof(struct paravirt_patch_template,pv_irq_ops.irq_enable.func) / sizeof(void *))), [paravirt_opptr] "i" (&(pv_irq_ops.irq_enable.func)), [paravirt_clobber] "i" (((1 << 0))) : "memory", "cc" ); });
}

static inline __attribute__((no_instrument_function)) __attribute__((no_instrument_function)) unsigned long arch_local_irq_save(void)
{
 unsigned long f;

 f = arch_local_save_flags();
 arch_local_irq_disable();
 return f;
}
# 847 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/paravirt.h"
extern void default_banner(void);
# 132 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/msr.h" 2
# 215 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/msr.h"
struct msr *msrs_alloc(void);
void msrs_free(struct msr *msrs);
int msr_set_bit(u32 msr, u8 bit);
int msr_clear_bit(u32 msr, u8 bit);


int rdmsr_on_cpu(unsigned int cpu, u32 msr_no, u32 *l, u32 *h);
int wrmsr_on_cpu(unsigned int cpu, u32 msr_no, u32 l, u32 h);
int rdmsrl_on_cpu(unsigned int cpu, u32 msr_no, u64 *q);
int wrmsrl_on_cpu(unsigned int cpu, u32 msr_no, u64 q);
void rdmsr_on_cpus(const struct cpumask *mask, u32 msr_no, struct msr *msrs);
void wrmsr_on_cpus(const struct cpumask *mask, u32 msr_no, struct msr *msrs);
int rdmsr_safe_on_cpu(unsigned int cpu, u32 msr_no, u32 *l, u32 *h);
int wrmsr_safe_on_cpu(unsigned int cpu, u32 msr_no, u32 l, u32 h);
int rdmsrl_safe_on_cpu(unsigned int cpu, u32 msr_no, u64 *q);
int wrmsrl_safe_on_cpu(unsigned int cpu, u32 msr_no, u64 q);
int rdmsr_safe_regs_on_cpu(unsigned int cpu, u32 regs[8]);
int wrmsr_safe_regs_on_cpu(unsigned int cpu, u32 regs[8]);
# 21 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h" 2

# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/nops.h" 1
# 142 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/nops.h"
extern const unsigned char * const *ideal_nops;
extern void arch_init_ideal_nops(void);
# 23 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/special_insns.h" 1






static inline __attribute__((no_instrument_function)) void native_clts(void)
{
 asm volatile("clts");
}
# 19 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/special_insns.h"
extern unsigned long __force_order;

static inline __attribute__((no_instrument_function)) unsigned long native_read_cr0(void)
{
 unsigned long val;
 asm volatile("mov %%cr0,%0\n\t" : "=r" (val), "=m" (__force_order));
 return val;
}

static inline __attribute__((no_instrument_function)) void native_write_cr0(unsigned long val)
{
 asm volatile("mov %0,%%cr0": : "r" (val), "m" (__force_order));
}

static inline __attribute__((no_instrument_function)) unsigned long native_read_cr2(void)
{
 unsigned long val;
 asm volatile("mov %%cr2,%0\n\t" : "=r" (val), "=m" (__force_order));
 return val;
}

static inline __attribute__((no_instrument_function)) void native_write_cr2(unsigned long val)
{
 asm volatile("mov %0,%%cr2": : "r" (val), "m" (__force_order));
}

static inline __attribute__((no_instrument_function)) unsigned long native_read_cr3(void)
{
 unsigned long val;
 asm volatile("mov %%cr3,%0\n\t" : "=r" (val), "=m" (__force_order));
 return val;
}

static inline __attribute__((no_instrument_function)) void native_write_cr3(unsigned long val)
{
 asm volatile("mov %0,%%cr3": : "r" (val), "m" (__force_order));
}

static inline __attribute__((no_instrument_function)) unsigned long native_read_cr4(void)
{
 unsigned long val;
 asm volatile("mov %%cr4,%0\n\t" : "=r" (val), "=m" (__force_order));
 return val;
}

static inline __attribute__((no_instrument_function)) unsigned long native_read_cr4_safe(void)
{
 unsigned long val;
# 75 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/special_insns.h"
 val = native_read_cr4();

 return val;
}

static inline __attribute__((no_instrument_function)) void native_write_cr4(unsigned long val)
{
 asm volatile("mov %0,%%cr4": : "r" (val), "m" (__force_order));
}


static inline __attribute__((no_instrument_function)) unsigned long native_read_cr8(void)
{
 unsigned long cr8;
 asm volatile("movq %%cr8,%0" : "=r" (cr8));
 return cr8;
}

static inline __attribute__((no_instrument_function)) void native_write_cr8(unsigned long val)
{
 asm volatile("movq %0,%%cr8" :: "r" (val) : "memory");
}


static inline __attribute__((no_instrument_function)) void native_wbinvd(void)
{
 asm volatile("wbinvd": : :"memory");
}

extern void native_load_gs_index(unsigned);
# 189 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/special_insns.h"
static inline __attribute__((no_instrument_function)) void clflush(volatile void *__p)
{
 asm volatile("clflush %0" : "+m" (*(volatile char *)__p));
}

static inline __attribute__((no_instrument_function)) void clflushopt(volatile void *__p)
{
 asm volatile ("661:\n\t" ".byte " "0x3e" "; clflush %P0" "\n662:\n" ".pushsection .altinstructions,\"a\"\n" " .long 661b - .\n" " .long " "663""1""f - .\n" " .word " "(9*32+23)" "\n" " .byte " "662b-661b" "\n" " .byte " "664""1""f-""663""1""f" "\n" ".popsection\n" ".pushsection .discard,\"aw\",@progbits\n" " .byte 0xff + (" "664""1""f-""663""1""f" ") - (" "662b-661b" ")\n" ".popsection\n" ".pushsection .altinstr_replacement, \"ax\"\n" "663""1"":\n\t" ".byte 0x66; clflush %P0" "\n" "664""1" ":\n\t" ".popsection" : "+m" (*(volatile char *)__p) : "i" (0))


                                              ;
}
# 24 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h" 2

# 1 "include/linux/personality.h" 1



# 1 "include/uapi/linux/personality.h" 1
# 10 "include/uapi/linux/personality.h"
enum {
 UNAME26 = 0x0020000,
 ADDR_NO_RANDOMIZE = 0x0040000,
 FDPIC_FUNCPTRS = 0x0080000,


 MMAP_PAGE_ZERO = 0x0100000,
 ADDR_COMPAT_LAYOUT = 0x0200000,
 READ_IMPLIES_EXEC = 0x0400000,
 ADDR_LIMIT_32BIT = 0x0800000,
 SHORT_INODE = 0x1000000,
 WHOLE_SECONDS = 0x2000000,
 STICKY_TIMEOUTS = 0x4000000,
 ADDR_LIMIT_3GB = 0x8000000,
};
# 41 "include/uapi/linux/personality.h"
enum {
 PER_LINUX = 0x0000,
 PER_LINUX_32BIT = 0x0000 | ADDR_LIMIT_32BIT,
 PER_LINUX_FDPIC = 0x0000 | FDPIC_FUNCPTRS,
 PER_SVR4 = 0x0001 | STICKY_TIMEOUTS | MMAP_PAGE_ZERO,
 PER_SVR3 = 0x0002 | STICKY_TIMEOUTS | SHORT_INODE,
 PER_SCOSVR3 = 0x0003 | STICKY_TIMEOUTS |
      WHOLE_SECONDS | SHORT_INODE,
 PER_OSR5 = 0x0003 | STICKY_TIMEOUTS | WHOLE_SECONDS,
 PER_WYSEV386 = 0x0004 | STICKY_TIMEOUTS | SHORT_INODE,
 PER_ISCR4 = 0x0005 | STICKY_TIMEOUTS,
 PER_BSD = 0x0006,
 PER_SUNOS = 0x0006 | STICKY_TIMEOUTS,
 PER_XENIX = 0x0007 | STICKY_TIMEOUTS | SHORT_INODE,
 PER_LINUX32 = 0x0008,
 PER_LINUX32_3GB = 0x0008 | ADDR_LIMIT_3GB,
 PER_IRIX32 = 0x0009 | STICKY_TIMEOUTS,
 PER_IRIXN32 = 0x000a | STICKY_TIMEOUTS,
 PER_IRIX64 = 0x000b | STICKY_TIMEOUTS,
 PER_RISCOS = 0x000c,
 PER_SOLARIS = 0x000d | STICKY_TIMEOUTS,
 PER_UW7 = 0x000e | STICKY_TIMEOUTS | MMAP_PAGE_ZERO,
 PER_OSF4 = 0x000f,
 PER_HPUX = 0x0010,
 PER_MASK = 0x00ff,
};
# 5 "include/linux/personality.h" 2






struct exec_domain;
struct pt_regs;

extern int register_exec_domain(struct exec_domain *);
extern int unregister_exec_domain(struct exec_domain *);
extern int __set_personality(unsigned int);
# 25 "include/linux/personality.h"
typedef void (*handler_t)(int, struct pt_regs *);

struct exec_domain {
 const char *name;
 handler_t handler;
 unsigned char pers_low;
 unsigned char pers_high;
 unsigned long *signal_map;
 unsigned long *signal_invmap;
 struct map_segment *err_map;
 struct map_segment *socktype_map;
 struct map_segment *sockopt_map;
 struct map_segment *af_map;
 struct module *module;
 struct exec_domain *next;
};
# 26 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h" 2



# 1 "include/linux/math64.h" 1




# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/div64.h" 1
# 63 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/div64.h"
# 1 "include/asm-generic/div64.h" 1
# 64 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/div64.h" 2
# 6 "include/linux/math64.h" 2
# 18 "include/linux/math64.h"
static inline __attribute__((no_instrument_function)) u64 div_u64_rem(u64 dividend, u32 divisor, u32 *remainder)
{
 *remainder = dividend % divisor;
 return dividend / divisor;
}




static inline __attribute__((no_instrument_function)) s64 div_s64_rem(s64 dividend, s32 divisor, s32 *remainder)
{
 *remainder = dividend % divisor;
 return dividend / divisor;
}




static inline __attribute__((no_instrument_function)) u64 div64_u64_rem(u64 dividend, u64 divisor, u64 *remainder)
{
 *remainder = dividend % divisor;
 return dividend / divisor;
}




static inline __attribute__((no_instrument_function)) u64 div64_u64(u64 dividend, u64 divisor)
{
 return dividend / divisor;
}




static inline __attribute__((no_instrument_function)) s64 div64_s64(s64 dividend, s64 divisor)
{
 return dividend / divisor;
}
# 97 "include/linux/math64.h"
static inline __attribute__((no_instrument_function)) u64 div_u64(u64 dividend, u32 divisor)
{
 u32 remainder;
 return div_u64_rem(dividend, divisor, &remainder);
}






static inline __attribute__((no_instrument_function)) s64 div_s64(s64 dividend, s32 divisor)
{
 s32 remainder;
 return div_s64_rem(dividend, divisor, &remainder);
}


u32 iter_div_u64_rem(u64 dividend, u32 divisor, u64 *remainder);

static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) u32
__iter_div_u64_rem(u64 dividend, u32 divisor, u64 *remainder)
{
 u32 ret = 0;

 while (dividend >= divisor) {


  asm("" : "+rm"(dividend));

  dividend -= divisor;
  ret++;
 }

 *remainder = dividend;

 return ret;
}




static inline __attribute__((no_instrument_function)) u64 mul_u64_u32_shr(u64 a, u32 mul, unsigned int shift)
{
 return (u64)(((unsigned int)a * mul) >> shift);
}
# 30 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h" 2

# 1 "include/linux/irqflags.h" 1
# 15 "include/linux/irqflags.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/irqflags.h" 1
# 11 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/irqflags.h"
static inline __attribute__((no_instrument_function)) unsigned long native_save_fl(void)
{
 unsigned long flags;






 asm volatile("# __raw_save_flags\n\t"
       "pushf ; pop %0"
       : "=rm" (flags)
       :
       : "memory");

 return flags;
}

static inline __attribute__((no_instrument_function)) void native_restore_fl(unsigned long flags)
{
 asm volatile("push %0 ; popf"
       :
       :"g" (flags)
       :"memory", "cc");
}

static inline __attribute__((no_instrument_function)) void native_irq_disable(void)
{
 asm volatile("cli": : :"memory");
}

static inline __attribute__((no_instrument_function)) void native_irq_enable(void)
{
 asm volatile("sti": : :"memory");
}

static inline __attribute__((no_instrument_function)) void native_safe_halt(void)
{
 asm volatile("sti; hlt": : :"memory");
}

static inline __attribute__((no_instrument_function)) void native_halt(void)
{
 asm volatile("hlt": : :"memory");
}
# 155 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/irqflags.h"
static inline __attribute__((no_instrument_function)) int arch_irqs_disabled_flags(unsigned long flags)
{
 return !(flags & ((1UL) << (9)));
}

static inline __attribute__((no_instrument_function)) int arch_irqs_disabled(void)
{
 unsigned long flags = arch_local_save_flags();

 return arch_irqs_disabled_flags(flags);
}
# 16 "include/linux/irqflags.h" 2


  extern void trace_softirqs_on(unsigned long ip);
  extern void trace_softirqs_off(unsigned long ip);
  extern void trace_hardirqs_on(void);
  extern void trace_hardirqs_off(void);
# 32 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h" 2
# 46 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h"
static inline __attribute__((no_instrument_function)) void *current_text_addr(void)
{
 void *pc;

 asm volatile("mov $1f, %0; 1:":"=r" (pc));

 return pc;
}
# 63 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h"
enum tlb_infos {
 ENTRIES,
 NR_INFO
};

extern u16 __attribute__((__section__(".data..read_mostly"))) tlb_lli_4k[NR_INFO];
extern u16 __attribute__((__section__(".data..read_mostly"))) tlb_lli_2m[NR_INFO];
extern u16 __attribute__((__section__(".data..read_mostly"))) tlb_lli_4m[NR_INFO];
extern u16 __attribute__((__section__(".data..read_mostly"))) tlb_lld_4k[NR_INFO];
extern u16 __attribute__((__section__(".data..read_mostly"))) tlb_lld_2m[NR_INFO];
extern u16 __attribute__((__section__(".data..read_mostly"))) tlb_lld_4m[NR_INFO];
extern u16 __attribute__((__section__(".data..read_mostly"))) tlb_lld_1g[NR_INFO];
extern s8 __attribute__((__section__(".data..read_mostly"))) tlb_flushall_shift;







struct cpuinfo_x86 {
 __u8 x86;
 __u8 x86_vendor;
 __u8 x86_model;
 __u8 x86_mask;
# 97 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h"
 int x86_tlbsize;

 __u8 x86_virt_bits;
 __u8 x86_phys_bits;

 __u8 x86_coreid_bits;

 __u32 extended_cpuid_level;

 int cpuid_level;
 __u32 x86_capability[10 + 1];
 char x86_vendor_id[16];
 char x86_model_id[64];

 int x86_cache_size;
 int x86_cache_alignment;
 int x86_power;
 unsigned long loops_per_jiffy;

 u16 x86_max_cores;
 u16 apicid;
 u16 initial_apicid;
 u16 x86_clflush_size;

 u16 booted_cores;

 u16 phys_proc_id;

 u16 cpu_core_id;

 u8 compute_unit_id;

 u16 cpu_index;
 u32 microcode;
} __attribute__((__aligned__((1 << (6)))));
# 147 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h"
extern struct cpuinfo_x86 boot_cpu_data;
extern struct cpuinfo_x86 new_cpu_data;

extern struct tss_struct doublefault_tss;
extern __u32 cpu_caps_cleared[10];
extern __u32 cpu_caps_set[10];


extern __attribute__((section(".discard"), unused)) char __pcpu_scope_cpu_info; extern __attribute__((section(".data..percpu" ""))) __typeof__(struct cpuinfo_x86) cpu_info __attribute__((__aligned__((1 << (6)))));






extern const struct seq_operations cpuinfo_op;



extern void cpu_detect(struct cpuinfo_x86 *c);
extern void fpu_detect(struct cpuinfo_x86 *c);

extern void early_cpu_init(void);
extern void identify_boot_cpu(void);
extern void identify_secondary_cpu(struct cpuinfo_x86 *);
extern void print_cpu_info(struct cpuinfo_x86 *);
void print_cpu_msr(struct cpuinfo_x86 *);
extern void init_scattered_cpuid_features(struct cpuinfo_x86 *c);
extern unsigned int init_intel_cacheinfo(struct cpuinfo_x86 *c);
extern void init_amd_cacheinfo(struct cpuinfo_x86 *c);

extern void detect_extended_topology(struct cpuinfo_x86 *c);
extern void detect_ht(struct cpuinfo_x86 *c);




static inline __attribute__((no_instrument_function)) int have_cpuid_p(void)
{
 return 1;
}

static inline __attribute__((no_instrument_function)) void native_cpuid(unsigned int *eax, unsigned int *ebx,
    unsigned int *ecx, unsigned int *edx)
{

 asm volatile("cpuid"
     : "=a" (*eax),
       "=b" (*ebx),
       "=c" (*ecx),
       "=d" (*edx)
     : "0" (*eax), "2" (*ecx)
     : "memory");
}

static inline __attribute__((no_instrument_function)) void load_cr3(pgd_t *pgdir)
{
 write_cr3(__phys_addr((unsigned long)(pgdir)));
}
# 241 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h"
struct x86_hw_tss {
 u32 reserved1;
 u64 sp0;
 u64 sp1;
 u64 sp2;
 u64 reserved2;
 u64 ist[7];
 u32 reserved3;
 u32 reserved4;
 u16 reserved5;
 u16 io_bitmap_base;

} __attribute__((packed)) __attribute__((__aligned__((1 << (6)))));
# 265 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h"
struct tss_struct {



 struct x86_hw_tss x86_tss;







 unsigned long io_bitmap[((65536/8)/sizeof(long)) + 1];




 unsigned long stack[64];

} __attribute__((__aligned__((1 << (6)))));

extern __attribute__((section(".discard"), unused)) char __pcpu_scope_init_tss; extern __attribute__((section(".data..percpu" ""))) __typeof__(struct tss_struct) init_tss __attribute__((__aligned__((1 << (6)))));




struct orig_ist {
 unsigned long ist[7];
};



struct i387_fsave_struct {
 u32 cwd;
 u32 swd;
 u32 twd;
 u32 fip;
 u32 fcs;
 u32 foo;
 u32 fos;


 u32 st_space[20];


 u32 status;
};

struct i387_fxsave_struct {
 u16 cwd;
 u16 swd;
 u16 twd;
 u16 fop;
 union {
  struct {
   u64 rip;
   u64 rdp;
  };
  struct {
   u32 fip;
   u32 fcs;
   u32 foo;
   u32 fos;
  };
 };
 u32 mxcsr;
 u32 mxcsr_mask;


 u32 st_space[32];


 u32 xmm_space[64];

 u32 padding[12];

 union {
  u32 padding1[12];
  u32 sw_reserved[12];
 };

} __attribute__((aligned(16)));

struct i387_soft_struct {
 u32 cwd;
 u32 swd;
 u32 twd;
 u32 fip;
 u32 fcs;
 u32 foo;
 u32 fos;

 u32 st_space[20];
 u8 ftop;
 u8 changed;
 u8 lookahead;
 u8 no_update;
 u8 rm;
 u8 alimit;
 struct math_emu_info *info;
 u32 entry_eip;
};

struct ymmh_struct {

 u32 ymmh_space[64];
};


struct lwp_struct {
 u8 reserved[128];
};

struct bndregs_struct {
 u64 bndregs[8];
} __attribute__((packed));

struct bndcsr_struct {
 u64 cfg_reg_u;
 u64 status_reg;
} __attribute__((packed));

struct xsave_hdr_struct {
 u64 xstate_bv;
 u64 reserved1[2];
 u64 reserved2[5];
} __attribute__((packed));

struct xsave_struct {
 struct i387_fxsave_struct i387;
 struct xsave_hdr_struct xsave_hdr;
 struct ymmh_struct ymmh;
 struct lwp_struct lwp;
 struct bndregs_struct bndregs;
 struct bndcsr_struct bndcsr;

} __attribute__ ((packed, aligned (64)));

union thread_xstate {
 struct i387_fsave_struct fsave;
 struct i387_fxsave_struct fxsave;
 struct i387_soft_struct soft;
 struct xsave_struct xsave;
};

struct fpu {
 unsigned int last_cpu;
 unsigned int has_fpu;
 union thread_xstate *state;
};


extern __attribute__((section(".discard"), unused)) char __pcpu_scope_orig_ist; extern __attribute__((section(".data..percpu" ""))) __typeof__(struct orig_ist) orig_ist;

union irq_stack_union {
 char irq_stack[(((1UL) << 12) << 2)];





 struct {
  char gs_base[40];
  unsigned long stack_canary;
 };
};

extern __attribute__((section(".discard"), unused)) char __pcpu_scope_irq_stack_union; extern __attribute__((section(".data..percpu" "..first"))) __typeof__(union irq_stack_union) irq_stack_union ;
extern typeof(irq_stack_union) init_per_cpu__irq_stack_union;

extern __attribute__((section(".discard"), unused)) char __pcpu_scope_irq_stack_ptr; extern __attribute__((section(".data..percpu" ""))) __typeof__(char *) irq_stack_ptr;
extern __attribute__((section(".discard"), unused)) char __pcpu_scope_irq_count; extern __attribute__((section(".data..percpu" ""))) __typeof__(unsigned int) irq_count;
extern void ignore_sysret(void);
# 463 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h"
extern unsigned int xstate_size;
extern void free_thread_xstate(struct task_struct *);
extern struct kmem_cache *task_xstate_cachep;

struct perf_event;

struct thread_struct {

 struct desc_struct tls_array[3];
 unsigned long sp0;
 unsigned long sp;



 unsigned long usersp;
 unsigned short es;
 unsigned short ds;
 unsigned short fsindex;
 unsigned short gsindex;





 unsigned long fs;

 unsigned long gs;

 struct perf_event *ptrace_bps[4];

 unsigned long debugreg6;

 unsigned long ptrace_dr7;

 unsigned long cr2;
 unsigned long trap_nr;
 unsigned long error_code;

 struct fpu fpu;
# 513 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h"
 unsigned long *io_bitmap_ptr;
 unsigned long iopl;

 unsigned io_bitmap_max;
# 525 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h"
 unsigned char fpu_counter;
};




static inline __attribute__((no_instrument_function)) void native_set_iopl_mask(unsigned mask)
{
# 545 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h"
}

static inline __attribute__((no_instrument_function)) void
native_load_sp0(struct tss_struct *tss, struct thread_struct *thread)
{
 tss->x86_tss.sp0 = thread->sp0;







}

static inline __attribute__((no_instrument_function)) void native_swapgs(void)
{

 asm volatile("swapgs" ::: "memory");

}
# 588 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h"
extern unsigned long mmu_cr4_features;
extern u32 *trampoline_cr4_features;

static inline __attribute__((no_instrument_function)) void set_in_cr4(unsigned long mask)
{
 unsigned long cr4;

 mmu_cr4_features |= mask;
 if (trampoline_cr4_features)
  *trampoline_cr4_features = mmu_cr4_features;
 cr4 = read_cr4();
 cr4 |= mask;
 write_cr4(cr4);
}

static inline __attribute__((no_instrument_function)) void clear_in_cr4(unsigned long mask)
{
 unsigned long cr4;

 mmu_cr4_features &= ~mask;
 if (trampoline_cr4_features)
  *trampoline_cr4_features = mmu_cr4_features;
 cr4 = read_cr4();
 cr4 &= ~mask;
 write_cr4(cr4);
}

typedef struct {
 unsigned long seg;
} mm_segment_t;



extern void release_thread(struct task_struct *);

unsigned long get_wchan(struct task_struct *p);






static inline __attribute__((no_instrument_function)) void cpuid(unsigned int op,
    unsigned int *eax, unsigned int *ebx,
    unsigned int *ecx, unsigned int *edx)
{
 *eax = op;
 *ecx = 0;
 __cpuid(eax, ebx, ecx, edx);
}


static inline __attribute__((no_instrument_function)) void cpuid_count(unsigned int op, int count,
          unsigned int *eax, unsigned int *ebx,
          unsigned int *ecx, unsigned int *edx)
{
 *eax = op;
 *ecx = count;
 __cpuid(eax, ebx, ecx, edx);
}




static inline __attribute__((no_instrument_function)) unsigned int cpuid_eax(unsigned int op)
{
 unsigned int eax, ebx, ecx, edx;

 cpuid(op, &eax, &ebx, &ecx, &edx);

 return eax;
}

static inline __attribute__((no_instrument_function)) unsigned int cpuid_ebx(unsigned int op)
{
 unsigned int eax, ebx, ecx, edx;

 cpuid(op, &eax, &ebx, &ecx, &edx);

 return ebx;
}

static inline __attribute__((no_instrument_function)) unsigned int cpuid_ecx(unsigned int op)
{
 unsigned int eax, ebx, ecx, edx;

 cpuid(op, &eax, &ebx, &ecx, &edx);

 return ecx;
}

static inline __attribute__((no_instrument_function)) unsigned int cpuid_edx(unsigned int op)
{
 unsigned int eax, ebx, ecx, edx;

 cpuid(op, &eax, &ebx, &ecx, &edx);

 return edx;
}


static inline __attribute__((no_instrument_function)) void rep_nop(void)
{
 asm volatile("rep; nop" ::: "memory");
}

static inline __attribute__((no_instrument_function)) void cpu_relax(void)
{
 rep_nop();
}


static inline __attribute__((no_instrument_function)) void sync_core(void)
{
 int tmp;
# 722 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h"
 asm volatile("cpuid"
       : "=a" (tmp)
       : "0" (1)
       : "ebx", "ecx", "edx", "memory");

}

extern void select_idle_routine(const struct cpuinfo_x86 *c);
extern void init_amd_e400_c1e_mask(void);

extern unsigned long boot_option_idle_override;
extern bool amd_e400_c1e_detected;

enum idle_boot_override {IDLE_NO_OVERRIDE=0, IDLE_HALT, IDLE_NOMWAIT,
    IDLE_POLL};

extern void enable_sep_cpu(void);
extern int sysenter_setup(void);

extern void early_trap_init(void);
void early_trap_pf_init(void);


extern struct desc_ptr early_gdt_descr;

extern void cpu_set_gdt(int);
extern void switch_to_new_gdt(int);
extern void load_percpu_segment(int);
extern void cpu_init(void);

static inline __attribute__((no_instrument_function)) unsigned long get_debugctlmsr(void)
{
 unsigned long debugctlmsr = 0;





 do { int _err; debugctlmsr = paravirt_read_msr(0x000001d9, &_err); } while (0);

 return debugctlmsr;
}

static inline __attribute__((no_instrument_function)) void update_debugctlmsr(unsigned long debugctlmsr)
{




 do { paravirt_write_msr(0x000001d9, (u32)((u64)(debugctlmsr)), ((u64)(debugctlmsr))>>32); } while (0);
}

extern void set_task_blockstep(struct task_struct *task, bool on);





extern unsigned int machine_id;
extern unsigned int machine_submodel_id;
extern unsigned int BIOS_revision;


extern int bootloader_type;
extern int bootloader_version;

extern char ignore_fpu_irq;
# 807 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h"
static inline __attribute__((no_instrument_function)) void prefetch(const void *x)
{
 asm volatile ("661:\n\t" "prefetcht0 (%1)" "\n662:\n" ".pushsection .altinstructions,\"a\"\n" " .long 661b - .\n" " .long " "663""1""f - .\n" " .word " "(0*32+25)" "\n" " .byte " "662b-661b" "\n" " .byte " "664""1""f-""663""1""f" "\n" ".popsection\n" ".pushsection .discard,\"aw\",@progbits\n" " .byte 0xff + (" "664""1""f-""663""1""f" ") - (" "662b-661b" ")\n" ".popsection\n" ".pushsection .altinstr_replacement, \"ax\"\n" "663""1"":\n\t" "prefetchnta (%1)" "\n" "664""1" ":\n\t" ".popsection" : : "i" (0), "r" (x))


             ;
}






static inline __attribute__((no_instrument_function)) void prefetchw(const void *x)
{
 asm volatile ("661:\n\t" "prefetcht0 (%1)" "\n662:\n" ".pushsection .altinstructions,\"a\"\n" " .long 661b - .\n" " .long " "663""1""f - .\n" " .word " "(1*32+31)" "\n" " .byte " "662b-661b" "\n" " .byte " "664""1""f-""663""1""f" "\n" ".popsection\n" ".pushsection .discard,\"aw\",@progbits\n" " .byte 0xff + (" "664""1""f-""663""1""f" ") - (" "662b-661b" ")\n" ".popsection\n" ".pushsection .altinstr_replacement, \"ax\"\n" "663""1"":\n\t" "prefetchw (%1)" "\n" "664""1" ":\n\t" ".popsection" : : "i" (0), "r" (x))


             ;
}

static inline __attribute__((no_instrument_function)) void spin_lock_prefetch(const void *x)
{
 prefetchw(x);
}
# 928 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h"
extern unsigned long KSTK_ESP(struct task_struct *task);




extern __attribute__((section(".discard"), unused)) char __pcpu_scope_old_rsp; extern __attribute__((section(".data..percpu" ""))) __typeof__(unsigned long) old_rsp;



extern void start_thread(struct pt_regs *regs, unsigned long new_ip,
            unsigned long new_sp);
# 952 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/processor.h"
extern int get_tsc_mode(unsigned long adr);
extern int set_tsc_mode(unsigned int val);

extern u16 amd_get_nb_id(int cpu);

static inline __attribute__((no_instrument_function)) uint32_t hypervisor_cpuid_base(const char *sig, uint32_t leaves)
{
 uint32_t base, eax, signature[3];

 for (base = 0x40000000; base < 0x40010000; base += 0x100) {
  cpuid(base, &eax, &signature[0], &signature[1], &signature[2]);

  if (!memcmp(sig, signature, 12) &&
      (leaves == 0 || ((eax - base) >= leaves)))
   return base;
 }

 return 0;
}

extern unsigned long arch_align_stack(unsigned long sp);
extern void free_init_pages(char *what, unsigned long begin, unsigned long end);

void default_idle(void);

bool xen_set_default_idle(void);




void stop_this_cpu(void *dummy);
void df_debug(struct pt_regs *regs, long error_code);
# 7 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic.h" 2

# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/cmpxchg.h" 1
# 11 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/cmpxchg.h"
extern void __xchg_wrong_size(void)
 ;
extern void __cmpxchg_wrong_size(void)
 ;
extern void __xadd_wrong_size(void)
 ;
extern void __add_wrong_size(void)
 ;
# 143 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/cmpxchg.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/cmpxchg_64.h" 1



static inline __attribute__((no_instrument_function)) void set_64bit(volatile u64 *ptr, u64 val)
{
 *ptr = val;
}
# 144 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/cmpxchg.h" 2
# 9 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic.h" 2
# 24 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic.h"
static inline __attribute__((no_instrument_function)) int atomic_read(const atomic_t *v)
{
 return (*(volatile int *)&(v)->counter);
}
# 36 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic.h"
static inline __attribute__((no_instrument_function)) void atomic_set(atomic_t *v, int i)
{
 v->counter = i;
}
# 48 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic.h"
static inline __attribute__((no_instrument_function)) void atomic_add(int i, atomic_t *v)
{
 asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "addl %1,%0"
       : "+m" (v->counter)
       : "ir" (i));
}
# 62 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic.h"
static inline __attribute__((no_instrument_function)) void atomic_sub(int i, atomic_t *v)
{
 asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "subl %1,%0"
       : "+m" (v->counter)
       : "ir" (i));
}
# 78 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic.h"
static inline __attribute__((no_instrument_function)) int atomic_sub_and_test(int i, atomic_t *v)
{
 do { char c; asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "subl" " %2, " "%0" "; set" "e" " %1" : "+m" (v->counter), "=qm" (c) : "er" (i) : "memory"); return c != 0; } while (0);
}







static inline __attribute__((no_instrument_function)) void atomic_inc(atomic_t *v)
{
 asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "incl %0"
       : "+m" (v->counter));
}







static inline __attribute__((no_instrument_function)) void atomic_dec(atomic_t *v)
{
 asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "decl %0"
       : "+m" (v->counter));
}
# 115 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic.h"
static inline __attribute__((no_instrument_function)) int atomic_dec_and_test(atomic_t *v)
{
 do { char c; asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "decl" " " "%0" "; set" "e" " %1" : "+m" (v->counter), "=qm" (c) : : "memory"); return c != 0; } while (0);
}
# 128 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic.h"
static inline __attribute__((no_instrument_function)) int atomic_inc_and_test(atomic_t *v)
{
 do { char c; asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "incl" " " "%0" "; set" "e" " %1" : "+m" (v->counter), "=qm" (c) : : "memory"); return c != 0; } while (0);
}
# 142 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic.h"
static inline __attribute__((no_instrument_function)) int atomic_add_negative(int i, atomic_t *v)
{
 do { char c; asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "addl" " %2, " "%0" "; set" "s" " %1" : "+m" (v->counter), "=qm" (c) : "er" (i) : "memory"); return c != 0; } while (0);
}
# 154 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic.h"
static inline __attribute__((no_instrument_function)) int atomic_add_return(int i, atomic_t *v)
{
 return i + ({ __typeof__ (*(((&v->counter)))) __ret = (((i))); switch (sizeof(*(((&v->counter))))) { case 1: asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "xadd" "b %b0, %1\n" : "+q" (__ret), "+m" (*(((&v->counter)))) : : "memory", "cc"); break; case 2: asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "xadd" "w %w0, %1\n" : "+r" (__ret), "+m" (*(((&v->counter)))) : : "memory", "cc"); break; case 4: asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "xadd" "l %0, %1\n" : "+r" (__ret), "+m" (*(((&v->counter)))) : : "memory", "cc"); break; case 8: asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "xadd" "q %q0, %1\n" : "+r" (__ret), "+m" (*(((&v->counter)))) : : "memory", "cc"); break; default: __xadd_wrong_size(); } __ret; });
}
# 166 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic.h"
static inline __attribute__((no_instrument_function)) int atomic_sub_return(int i, atomic_t *v)
{
 return atomic_add_return(-i, v);
}




static inline __attribute__((no_instrument_function)) int atomic_cmpxchg(atomic_t *v, int old, int new)
{
 return ({ __typeof__(*((&v->counter))) __ret; __typeof__(*((&v->counter))) __old = ((old)); __typeof__(*((&v->counter))) __new = ((new)); switch ((sizeof(*(&v->counter)))) { case 1: { volatile u8 *__ptr = (volatile u8 *)((&v->counter)); asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "cmpxchgb %2,%1" : "=a" (__ret), "+m" (*__ptr) : "q" (__new), "0" (__old) : "memory"); break; } case 2: { volatile u16 *__ptr = (volatile u16 *)((&v->counter)); asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "cmpxchgw %2,%1" : "=a" (__ret), "+m" (*__ptr) : "r" (__new), "0" (__old) : "memory"); break; } case 4: { volatile u32 *__ptr = (volatile u32 *)((&v->counter)); asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "cmpxchgl %2,%1" : "=a" (__ret), "+m" (*__ptr) : "r" (__new), "0" (__old) : "memory"); break; } case 8: { volatile u64 *__ptr = (volatile u64 *)((&v->counter)); asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "cmpxchgq %2,%1" : "=a" (__ret), "+m" (*__ptr) : "r" (__new), "0" (__old) : "memory"); break; } default: __cmpxchg_wrong_size(); } __ret; });
}

static inline __attribute__((no_instrument_function)) int atomic_xchg(atomic_t *v, int new)
{
 return ({ __typeof__ (*((&v->counter))) __ret = ((new)); switch (sizeof(*((&v->counter)))) { case 1: asm volatile ("" "xchg" "b %b0, %1\n" : "+q" (__ret), "+m" (*((&v->counter))) : : "memory", "cc"); break; case 2: asm volatile ("" "xchg" "w %w0, %1\n" : "+r" (__ret), "+m" (*((&v->counter))) : : "memory", "cc"); break; case 4: asm volatile ("" "xchg" "l %0, %1\n" : "+r" (__ret), "+m" (*((&v->counter))) : : "memory", "cc"); break; case 8: asm volatile ("" "xchg" "q %q0, %1\n" : "+r" (__ret), "+m" (*((&v->counter))) : : "memory", "cc"); break; default: __xchg_wrong_size(); } __ret; });
}
# 193 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic.h"
static inline __attribute__((no_instrument_function)) int __atomic_add_unless(atomic_t *v, int a, int u)
{
 int c, old;
 c = atomic_read(v);
 for (;;) {
  if (__builtin_expect(!!(c == (u)), 0))
   break;
  old = atomic_cmpxchg((v), c, c + (a));
  if (__builtin_expect(!!(old == c), 1))
   break;
  c = old;
 }
 return c;
}
# 215 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic.h"
static inline __attribute__((no_instrument_function)) short int atomic_inc_short(short int *v)
{
 asm(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "addw $1, %0" : "+m" (*v));
 return *v;
}
# 230 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic.h"
static inline __attribute__((no_instrument_function)) void atomic_or_long(unsigned long *v1, unsigned long v2)
{
 asm(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "orq %1, %0" : "+m" (*v1) : "r" (v2));
}
# 255 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic64_64.h" 1
# 19 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic64_64.h"
static inline __attribute__((no_instrument_function)) long atomic64_read(const atomic64_t *v)
{
 return (*(volatile long *)&(v)->counter);
}
# 31 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic64_64.h"
static inline __attribute__((no_instrument_function)) void atomic64_set(atomic64_t *v, long i)
{
 v->counter = i;
}
# 43 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic64_64.h"
static inline __attribute__((no_instrument_function)) void atomic64_add(long i, atomic64_t *v)
{
 asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "addq %1,%0"
       : "=m" (v->counter)
       : "er" (i), "m" (v->counter));
}
# 57 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic64_64.h"
static inline __attribute__((no_instrument_function)) void atomic64_sub(long i, atomic64_t *v)
{
 asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "subq %1,%0"
       : "=m" (v->counter)
       : "er" (i), "m" (v->counter));
}
# 73 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic64_64.h"
static inline __attribute__((no_instrument_function)) int atomic64_sub_and_test(long i, atomic64_t *v)
{
 do { char c; asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "subq" " %2, " "%0" "; set" "e" " %1" : "+m" (v->counter), "=qm" (c) : "er" (i) : "memory"); return c != 0; } while (0);
}







static inline __attribute__((no_instrument_function)) void atomic64_inc(atomic64_t *v)
{
 asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "incq %0"
       : "=m" (v->counter)
       : "m" (v->counter));
}







static inline __attribute__((no_instrument_function)) void atomic64_dec(atomic64_t *v)
{
 asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "decq %0"
       : "=m" (v->counter)
       : "m" (v->counter));
}
# 112 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic64_64.h"
static inline __attribute__((no_instrument_function)) int atomic64_dec_and_test(atomic64_t *v)
{
 do { char c; asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "decq" " " "%0" "; set" "e" " %1" : "+m" (v->counter), "=qm" (c) : : "memory"); return c != 0; } while (0);
}
# 125 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic64_64.h"
static inline __attribute__((no_instrument_function)) int atomic64_inc_and_test(atomic64_t *v)
{
 do { char c; asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "incq" " " "%0" "; set" "e" " %1" : "+m" (v->counter), "=qm" (c) : : "memory"); return c != 0; } while (0);
}
# 139 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic64_64.h"
static inline __attribute__((no_instrument_function)) int atomic64_add_negative(long i, atomic64_t *v)
{
 do { char c; asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "addq" " %2, " "%0" "; set" "s" " %1" : "+m" (v->counter), "=qm" (c) : "er" (i) : "memory"); return c != 0; } while (0);
}
# 151 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic64_64.h"
static inline __attribute__((no_instrument_function)) long atomic64_add_return(long i, atomic64_t *v)
{
 return i + ({ __typeof__ (*(((&v->counter)))) __ret = (((i))); switch (sizeof(*(((&v->counter))))) { case 1: asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "xadd" "b %b0, %1\n" : "+q" (__ret), "+m" (*(((&v->counter)))) : : "memory", "cc"); break; case 2: asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "xadd" "w %w0, %1\n" : "+r" (__ret), "+m" (*(((&v->counter)))) : : "memory", "cc"); break; case 4: asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "xadd" "l %0, %1\n" : "+r" (__ret), "+m" (*(((&v->counter)))) : : "memory", "cc"); break; case 8: asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "xadd" "q %q0, %1\n" : "+r" (__ret), "+m" (*(((&v->counter)))) : : "memory", "cc"); break; default: __xadd_wrong_size(); } __ret; });
}

static inline __attribute__((no_instrument_function)) long atomic64_sub_return(long i, atomic64_t *v)
{
 return atomic64_add_return(-i, v);
}




static inline __attribute__((no_instrument_function)) long atomic64_cmpxchg(atomic64_t *v, long old, long new)
{
 return ({ __typeof__(*((&v->counter))) __ret; __typeof__(*((&v->counter))) __old = ((old)); __typeof__(*((&v->counter))) __new = ((new)); switch ((sizeof(*(&v->counter)))) { case 1: { volatile u8 *__ptr = (volatile u8 *)((&v->counter)); asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "cmpxchgb %2,%1" : "=a" (__ret), "+m" (*__ptr) : "q" (__new), "0" (__old) : "memory"); break; } case 2: { volatile u16 *__ptr = (volatile u16 *)((&v->counter)); asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "cmpxchgw %2,%1" : "=a" (__ret), "+m" (*__ptr) : "r" (__new), "0" (__old) : "memory"); break; } case 4: { volatile u32 *__ptr = (volatile u32 *)((&v->counter)); asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "cmpxchgl %2,%1" : "=a" (__ret), "+m" (*__ptr) : "r" (__new), "0" (__old) : "memory"); break; } case 8: { volatile u64 *__ptr = (volatile u64 *)((&v->counter)); asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "cmpxchgq %2,%1" : "=a" (__ret), "+m" (*__ptr) : "r" (__new), "0" (__old) : "memory"); break; } default: __cmpxchg_wrong_size(); } __ret; });
}

static inline __attribute__((no_instrument_function)) long atomic64_xchg(atomic64_t *v, long new)
{
 return ({ __typeof__ (*((&v->counter))) __ret = ((new)); switch (sizeof(*((&v->counter)))) { case 1: asm volatile ("" "xchg" "b %b0, %1\n" : "+q" (__ret), "+m" (*((&v->counter))) : : "memory", "cc"); break; case 2: asm volatile ("" "xchg" "w %w0, %1\n" : "+r" (__ret), "+m" (*((&v->counter))) : : "memory", "cc"); break; case 4: asm volatile ("" "xchg" "l %0, %1\n" : "+r" (__ret), "+m" (*((&v->counter))) : : "memory", "cc"); break; case 8: asm volatile ("" "xchg" "q %q0, %1\n" : "+r" (__ret), "+m" (*((&v->counter))) : : "memory", "cc"); break; default: __xchg_wrong_size(); } __ret; });
}
# 183 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic64_64.h"
static inline __attribute__((no_instrument_function)) int atomic64_add_unless(atomic64_t *v, long a, long u)
{
 long c, old;
 c = atomic64_read(v);
 for (;;) {
  if (__builtin_expect(!!(c == (u)), 0))
   break;
  old = atomic64_cmpxchg((v), c, c + (a));
  if (__builtin_expect(!!(old == c), 1))
   break;
  c = old;
 }
 return c != (u);
}
# 207 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic64_64.h"
static inline __attribute__((no_instrument_function)) long atomic64_dec_if_positive(atomic64_t *v)
{
 long c, old, dec;
 c = atomic64_read(v);
 for (;;) {
  dec = c - 1;
  if (__builtin_expect(!!(dec < 0), 0))
   break;
  old = atomic64_cmpxchg((v), c, dec);
  if (__builtin_expect(!!(old == c), 1))
   break;
  c = old;
 }
 return dec;
}
# 256 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/atomic.h" 2
# 5 "include/linux/atomic.h" 2
# 15 "include/linux/atomic.h"
static inline __attribute__((no_instrument_function)) int atomic_add_unless(atomic_t *v, int a, int u)
{
 return __atomic_add_unless(v, a, u) != u;
}
# 44 "include/linux/atomic.h"
static inline __attribute__((no_instrument_function)) int atomic_inc_not_zero_hint(atomic_t *v, int hint)
{
 int val, c = hint;


 if (!hint)
  return atomic_add_unless((v), 1, 0);

 do {
  val = atomic_cmpxchg(v, c, c + 1);
  if (val == c)
   return 1;
  c = val;
 } while (c);

 return 0;
}



static inline __attribute__((no_instrument_function)) int atomic_inc_unless_negative(atomic_t *p)
{
 int v, v1;
 for (v = 0; v >= 0; v = v1) {
  v1 = atomic_cmpxchg(p, v, v + 1);
  if (__builtin_expect(!!(v1 == v), 1))
   return 1;
 }
 return 0;
}



static inline __attribute__((no_instrument_function)) int atomic_dec_unless_positive(atomic_t *p)
{
 int v, v1;
 for (v = 0; v <= 0; v = v1) {
  v1 = atomic_cmpxchg(p, v, v - 1);
  if (__builtin_expect(!!(v1 == v), 1))
   return 1;
 }
 return 0;
}
# 97 "include/linux/atomic.h"
static inline __attribute__((no_instrument_function)) int atomic_dec_if_positive(atomic_t *v)
{
 int c, old, dec;
 c = atomic_read(v);
 for (;;) {
  dec = c - 1;
  if (__builtin_expect(!!(dec < 0), 0))
   break;
  old = atomic_cmpxchg((v), c, dec);
  if (__builtin_expect(!!(old == c), 1))
   break;
  c = old;
 }
 return dec;
}



static inline __attribute__((no_instrument_function)) void atomic_or(int i, atomic_t *v)
{
 int old;
 int new;

 do {
  old = atomic_read(v);
  new = old | i;
 } while (atomic_cmpxchg(v, old, new) != old);
}


# 1 "include/asm-generic/atomic-long.h" 1
# 23 "include/asm-generic/atomic-long.h"
typedef atomic64_t atomic_long_t;



static inline __attribute__((no_instrument_function)) long atomic_long_read(atomic_long_t *l)
{
 atomic64_t *v = (atomic64_t *)l;

 return (long)atomic64_read(v);
}

static inline __attribute__((no_instrument_function)) void atomic_long_set(atomic_long_t *l, long i)
{
 atomic64_t *v = (atomic64_t *)l;

 atomic64_set(v, i);
}

static inline __attribute__((no_instrument_function)) void atomic_long_inc(atomic_long_t *l)
{
 atomic64_t *v = (atomic64_t *)l;

 atomic64_inc(v);
}

static inline __attribute__((no_instrument_function)) void atomic_long_dec(atomic_long_t *l)
{
 atomic64_t *v = (atomic64_t *)l;

 atomic64_dec(v);
}

static inline __attribute__((no_instrument_function)) void atomic_long_add(long i, atomic_long_t *l)
{
 atomic64_t *v = (atomic64_t *)l;

 atomic64_add(i, v);
}

static inline __attribute__((no_instrument_function)) void atomic_long_sub(long i, atomic_long_t *l)
{
 atomic64_t *v = (atomic64_t *)l;

 atomic64_sub(i, v);
}

static inline __attribute__((no_instrument_function)) int atomic_long_sub_and_test(long i, atomic_long_t *l)
{
 atomic64_t *v = (atomic64_t *)l;

 return atomic64_sub_and_test(i, v);
}

static inline __attribute__((no_instrument_function)) int atomic_long_dec_and_test(atomic_long_t *l)
{
 atomic64_t *v = (atomic64_t *)l;

 return atomic64_dec_and_test(v);
}

static inline __attribute__((no_instrument_function)) int atomic_long_inc_and_test(atomic_long_t *l)
{
 atomic64_t *v = (atomic64_t *)l;

 return atomic64_inc_and_test(v);
}

static inline __attribute__((no_instrument_function)) int atomic_long_add_negative(long i, atomic_long_t *l)
{
 atomic64_t *v = (atomic64_t *)l;

 return atomic64_add_negative(i, v);
}

static inline __attribute__((no_instrument_function)) long atomic_long_add_return(long i, atomic_long_t *l)
{
 atomic64_t *v = (atomic64_t *)l;

 return (long)atomic64_add_return(i, v);
}

static inline __attribute__((no_instrument_function)) long atomic_long_sub_return(long i, atomic_long_t *l)
{
 atomic64_t *v = (atomic64_t *)l;

 return (long)atomic64_sub_return(i, v);
}

static inline __attribute__((no_instrument_function)) long atomic_long_inc_return(atomic_long_t *l)
{
 atomic64_t *v = (atomic64_t *)l;

 return (long)(atomic64_add_return(1, (v)));
}

static inline __attribute__((no_instrument_function)) long atomic_long_dec_return(atomic_long_t *l)
{
 atomic64_t *v = (atomic64_t *)l;

 return (long)(atomic64_sub_return(1, (v)));
}

static inline __attribute__((no_instrument_function)) long atomic_long_add_unless(atomic_long_t *l, long a, long u)
{
 atomic64_t *v = (atomic64_t *)l;

 return (long)atomic64_add_unless(v, a, u);
}
# 128 "include/linux/atomic.h" 2
# 6 "include/linux/debug_locks.h" 2


struct task_struct;

extern int debug_locks;
extern int debug_locks_silent;


static inline __attribute__((no_instrument_function)) int __debug_locks_off(void)
{
 return ({ __typeof__ (*((&debug_locks))) __ret = ((0)); switch (sizeof(*((&debug_locks)))) { case 1: asm volatile ("" "xchg" "b %b0, %1\n" : "+q" (__ret), "+m" (*((&debug_locks))) : : "memory", "cc"); break; case 2: asm volatile ("" "xchg" "w %w0, %1\n" : "+r" (__ret), "+m" (*((&debug_locks))) : : "memory", "cc"); break; case 4: asm volatile ("" "xchg" "l %0, %1\n" : "+r" (__ret), "+m" (*((&debug_locks))) : : "memory", "cc"); break; case 8: asm volatile ("" "xchg" "q %q0, %1\n" : "+r" (__ret), "+m" (*((&debug_locks))) : : "memory", "cc"); break; default: __xchg_wrong_size(); } __ret; });
}




extern int debug_locks_off(void);
# 43 "include/linux/debug_locks.h"
  extern void locking_selftest(void);




struct task_struct;


extern void debug_show_all_locks(void);
extern void debug_show_held_locks(struct task_struct *task);
extern void debug_check_no_locks_freed(const void *from, unsigned long len);
extern void debug_check_no_locks_held(void);
# 24 "include/linux/lockdep.h" 2
# 1 "include/linux/stacktrace.h" 1



struct task_struct;
struct pt_regs;


struct task_struct;

struct stack_trace {
 unsigned int nr_entries, max_entries;
 unsigned long *entries;
 int skip;
};

extern void save_stack_trace(struct stack_trace *trace);
extern void save_stack_trace_regs(struct pt_regs *regs,
      struct stack_trace *trace);
extern void save_stack_trace_tsk(struct task_struct *tsk,
    struct stack_trace *trace);

extern void print_stack_trace(struct stack_trace *trace, int spaces);


extern void save_stack_trace_user(struct stack_trace *trace);
# 25 "include/linux/lockdep.h" 2
# 50 "include/linux/lockdep.h"
struct lockdep_subclass_key {
 char __one_byte;
} __attribute__ ((__packed__));

struct lock_class_key {
 struct lockdep_subclass_key subkeys[8UL];
};

extern struct lock_class_key __lockdep_no_validate__;






struct lock_class {



 struct list_head hash_entry;




 struct list_head lock_entry;

 struct lockdep_subclass_key *key;
 unsigned int subclass;
 unsigned int dep_gen_id;




 unsigned long usage_mask;
 struct stack_trace usage_traces[(1+3*4)];






 struct list_head locks_after, locks_before;





 unsigned int version;




 unsigned long ops;

 const char *name;
 int name_version;


 unsigned long contention_point[4];
 unsigned long contending_point[4];

};


struct lock_time {
 s64 min;
 s64 max;
 s64 total;
 unsigned long nr;
};

enum bounce_type {
 bounce_acquired_write,
 bounce_acquired_read,
 bounce_contended_write,
 bounce_contended_read,
 nr_bounce_types,

 bounce_acquired = bounce_acquired_write,
 bounce_contended = bounce_contended_write,
};

struct lock_class_stats {
 unsigned long contention_point[4];
 unsigned long contending_point[4];
 struct lock_time read_waittime;
 struct lock_time write_waittime;
 struct lock_time read_holdtime;
 struct lock_time write_holdtime;
 unsigned long bounces[nr_bounce_types];
};

struct lock_class_stats lock_stats(struct lock_class *class);
void clear_lock_stats(struct lock_class *class);






struct lockdep_map {
 struct lock_class_key *key;
 struct lock_class *class_cache[2];
 const char *name;

 int cpu;
 unsigned long ip;

};

static inline __attribute__((no_instrument_function)) void lockdep_copy_map(struct lockdep_map *to,
        struct lockdep_map *from)
{
 int i;

 *to = *from;
# 174 "include/linux/lockdep.h"
 for (i = 0; i < 2; i++)
  to->class_cache[i] = ((void *)0);
}





struct lock_list {
 struct list_head entry;
 struct lock_class *class;
 struct stack_trace trace;
 int distance;





 struct lock_list *parent;
};




struct lock_chain {
 u8 irq_context;
 u8 depth;
 u16 base;
 struct list_head entry;
 u64 chain_key;
};
# 214 "include/linux/lockdep.h"
struct held_lock {
# 229 "include/linux/lockdep.h"
 u64 prev_chain_key;
 unsigned long acquire_ip;
 struct lockdep_map *instance;
 struct lockdep_map *nest_lock;

 u64 waittime_stamp;
 u64 holdtime_stamp;

 unsigned int class_idx:13;
# 251 "include/linux/lockdep.h"
 unsigned int irq_context:2;
 unsigned int trylock:1;

 unsigned int read:2;
 unsigned int check:1;
 unsigned int hardirqs_off:1;
 unsigned int references:12;
};




extern void lockdep_init(void);
extern void lockdep_info(void);
extern void lockdep_reset(void);
extern void lockdep_reset_lock(struct lockdep_map *lock);
extern void lockdep_free_key_range(void *start, unsigned long size);
extern void lockdep_sys_exit(void);

extern void lockdep_off(void);
extern void lockdep_on(void);







extern void lockdep_init_map(struct lockdep_map *lock, const char *name,
        struct lock_class_key *key, int subclass);
# 312 "include/linux/lockdep.h"
static inline __attribute__((no_instrument_function)) int lockdep_match_key(struct lockdep_map *lock,
        struct lock_class_key *key)
{
 return lock->key == key;
}
# 332 "include/linux/lockdep.h"
extern void lock_acquire(struct lockdep_map *lock, unsigned int subclass,
    int trylock, int read, int check,
    struct lockdep_map *nest_lock, unsigned long ip);

extern void lock_release(struct lockdep_map *lock, int nested,
    unsigned long ip);



extern int lock_is_held(struct lockdep_map *lock);

extern void lock_set_class(struct lockdep_map *lock, const char *name,
      struct lock_class_key *key, unsigned int subclass,
      unsigned long ip);

static inline __attribute__((no_instrument_function)) void lock_set_subclass(struct lockdep_map *lock,
  unsigned int subclass, unsigned long ip)
{
 lock_set_class(lock, lock->name, lock->key, subclass, ip);
}

extern void lockdep_set_current_reclaim_state(gfp_t gfp_mask);
extern void lockdep_clear_current_reclaim_state(void);
extern void lockdep_trace_alloc(gfp_t mask);
# 422 "include/linux/lockdep.h"
extern void lock_contended(struct lockdep_map *lock, unsigned long ip);
extern void lock_acquired(struct lockdep_map *lock, unsigned long ip);
# 462 "include/linux/lockdep.h"
extern void print_irqtrace_events(struct task_struct *curr);
# 529 "include/linux/lockdep.h"
void lockdep_rcu_suspicious(const char *file, const int line, const char *s);
# 19 "include/linux/spinlock_types.h" 2

typedef struct raw_spinlock {
 arch_spinlock_t raw_lock;




 unsigned int magic, owner_cpu;
 void *owner;


 struct lockdep_map dep_map;

} raw_spinlock_t;
# 64 "include/linux/spinlock_types.h"
typedef struct spinlock {
 union {
  struct raw_spinlock rlock;



  struct {
   u8 __padding[(__builtin_offsetof(struct raw_spinlock,dep_map))];
   struct lockdep_map dep_map;
  };

 };
} spinlock_t;
# 86 "include/linux/spinlock_types.h"
# 1 "include/linux/rwlock_types.h" 1
# 11 "include/linux/rwlock_types.h"
typedef struct {
 arch_rwlock_t raw_lock;




 unsigned int magic, owner_cpu;
 void *owner;


 struct lockdep_map dep_map;

} rwlock_t;
# 87 "include/linux/spinlock_types.h" 2
# 16 "include/linux/mutex.h" 2
# 49 "include/linux/mutex.h"
struct optimistic_spin_queue;
struct mutex {

 atomic_t count;
 spinlock_t wait_lock;
 struct list_head wait_list;

 struct task_struct *owner;





 const char *name;
 void *magic;


 struct lockdep_map dep_map;

};





struct mutex_waiter {
 struct list_head list;
 struct task_struct *task;

 void *magic;

};


# 1 "include/linux/mutex-debug.h" 1
# 22 "include/linux/mutex-debug.h"
extern void mutex_destroy(struct mutex *lock);
# 84 "include/linux/mutex.h" 2
# 120 "include/linux/mutex.h"
extern void __mutex_init(struct mutex *lock, const char *name,
    struct lock_class_key *key);







static inline __attribute__((no_instrument_function)) int mutex_is_locked(struct mutex *lock)
{
 return atomic_read(&lock->count) != 1;
}






extern void mutex_lock_nested(struct mutex *lock, unsigned int subclass);
extern void _mutex_lock_nest_lock(struct mutex *lock, struct lockdep_map *nest_lock);

extern int __attribute__((warn_unused_result)) mutex_lock_interruptible_nested(struct mutex *lock,
     unsigned int subclass);
extern int __attribute__((warn_unused_result)) mutex_lock_killable_nested(struct mutex *lock,
     unsigned int subclass);
# 174 "include/linux/mutex.h"
extern int mutex_trylock(struct mutex *lock);
extern void mutex_unlock(struct mutex *lock);

extern int atomic_dec_and_mutex_lock(atomic_t *cnt, struct mutex *lock);
# 14 "include/linux/kernfs.h" 2
# 1 "include/linux/idr.h" 1
# 18 "include/linux/idr.h"
# 1 "include/linux/rcupdate.h" 1
# 38 "include/linux/rcupdate.h"
# 1 "include/linux/spinlock.h" 1
# 50 "include/linux/spinlock.h"
# 1 "include/linux/preempt.h" 1
# 18 "include/linux/preempt.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/preempt.h" 1





# 1 "include/linux/thread_info.h" 1
# 13 "include/linux/thread_info.h"
struct timespec;
struct compat_timespec;




struct restart_block {
 long (*fn)(struct restart_block *);
 union {

  struct {
   u32 *uaddr;
   u32 val;
   u32 flags;
   u32 bitset;
   u64 time;
   u32 *uaddr2;
  } futex;

  struct {
   clockid_t clockid;
   struct timespec *rmtp;

   struct compat_timespec *compat_rmtp;

   u64 expires;
  } nanosleep;

  struct {
   struct pollfd *ufds;
   int nfds;
   int has_timeout;
   unsigned long tv_sec;
   unsigned long tv_nsec;
  } poll;
 };
};

extern long do_no_restart_syscall(struct restart_block *parm);


# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/thread_info.h" 1
# 21 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/thread_info.h"
struct task_struct;
struct exec_domain;



struct thread_info {
 struct task_struct *task;
 struct exec_domain *exec_domain;
 __u32 flags;
 __u32 status;
 __u32 cpu;
 int saved_preempt_count;
 mm_segment_t addr_limit;
 struct restart_block restart_block;
 void *sysenter_return;
 unsigned int sig_on_uaccess_error:1;
 unsigned int uaccess_err:1;
};
# 161 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/thread_info.h"
extern __attribute__((section(".discard"), unused)) char __pcpu_scope_kernel_stack; extern __attribute__((section(".data..percpu" ""))) __typeof__(unsigned long) kernel_stack;

static inline __attribute__((no_instrument_function)) struct thread_info *current_thread_info(void)
{
 struct thread_info *ti;
 ti = (void *)(({ typeof(kernel_stack) pfo_ret__; switch (sizeof(kernel_stack)) { case 1: asm("mov" "b ""%%""gs"":" "%P" "1"",%0" : "=q" (pfo_ret__) : "p" (&(kernel_stack))); break; case 2: asm("mov" "w ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "p" (&(kernel_stack))); break; case 4: asm("mov" "l ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "p" (&(kernel_stack))); break; case 8: asm("mov" "q ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "p" (&(kernel_stack))); break; default: __bad_percpu_size(); } pfo_ret__; }) +
        (5*(64/8)) - (((1UL) << 12) << 2));
 return ti;
}
# 200 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/thread_info.h"
static inline __attribute__((no_instrument_function)) void set_restore_sigmask(void)
{
 struct thread_info *ti = current_thread_info();
 ti->status |= 0x0008;
 ({ int __ret_warn_on = !!(!(__builtin_constant_p((2)) ? constant_test_bit((2), ((unsigned long *)&ti->flags)) : variable_test_bit((2), ((unsigned long *)&ti->flags)))); if (__builtin_expect(!!(__ret_warn_on), 0)) warn_slowpath_null("/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/thread_info.h", 204); __builtin_expect(!!(__ret_warn_on), 0); });
}
static inline __attribute__((no_instrument_function)) void clear_restore_sigmask(void)
{
 current_thread_info()->status &= ~0x0008;
}
static inline __attribute__((no_instrument_function)) bool test_restore_sigmask(void)
{
 return current_thread_info()->status & 0x0008;
}
static inline __attribute__((no_instrument_function)) bool test_and_clear_restore_sigmask(void)
{
 struct thread_info *ti = current_thread_info();
 if (!(ti->status & 0x0008))
  return false;
 ti->status &= ~0x0008;
 return true;
}

static inline __attribute__((no_instrument_function)) bool is_ia32_task(void)
{




 if (current_thread_info()->status & 0x0002)
  return true;

 return false;
}



extern void arch_task_cache_init(void);
extern int arch_dup_task_struct(struct task_struct *dst, struct task_struct *src);
extern void arch_release_task_struct(struct task_struct *tsk);
# 55 "include/linux/thread_info.h" 2
# 71 "include/linux/thread_info.h"
static inline __attribute__((no_instrument_function)) void set_ti_thread_flag(struct thread_info *ti, int flag)
{
 set_bit(flag, (unsigned long *)&ti->flags);
}

static inline __attribute__((no_instrument_function)) void clear_ti_thread_flag(struct thread_info *ti, int flag)
{
 clear_bit(flag, (unsigned long *)&ti->flags);
}

static inline __attribute__((no_instrument_function)) int test_and_set_ti_thread_flag(struct thread_info *ti, int flag)
{
 return test_and_set_bit(flag, (unsigned long *)&ti->flags);
}

static inline __attribute__((no_instrument_function)) int test_and_clear_ti_thread_flag(struct thread_info *ti, int flag)
{
 return test_and_clear_bit(flag, (unsigned long *)&ti->flags);
}

static inline __attribute__((no_instrument_function)) int test_ti_thread_flag(struct thread_info *ti, int flag)
{
 return (__builtin_constant_p((flag)) ? constant_test_bit((flag), ((unsigned long *)&ti->flags)) : variable_test_bit((flag), ((unsigned long *)&ti->flags)));
}
# 107 "include/linux/thread_info.h"
static inline __attribute__((no_instrument_function)) __attribute__((deprecated)) void set_need_resched(void)
{
# 119 "include/linux/thread_info.h"
}
# 7 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/preempt.h" 2

extern __attribute__((section(".discard"), unused)) char __pcpu_scope___preempt_count; extern __attribute__((section(".data..percpu" ""))) __typeof__(int) __preempt_count;
# 20 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/preempt.h"
static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) int preempt_count(void)
{
 return ({ typeof((__preempt_count)) pfo_ret__; switch (sizeof((__preempt_count))) { case 1: asm("mov" "b ""%%""gs"":" "%P" "1"",%0" : "=q" (pfo_ret__) : "m"(__preempt_count)); break; case 2: asm("mov" "w ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"(__preempt_count)); break; case 4: asm("mov" "l ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"(__preempt_count)); break; case 8: asm("mov" "q ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"(__preempt_count)); break; default: __bad_percpu_size(); } pfo_ret__; }) & ~0x80000000;
}

static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) void preempt_count_set(int pc)
{
 do { typedef typeof((__preempt_count)) pto_T__; if (0) { pto_T__ pto_tmp__; pto_tmp__ = (pc); (void)pto_tmp__; } switch (sizeof((__preempt_count))) { case 1: asm("mov" "b %1,""%%""gs"":" "%P" "0" : "+m" ((__preempt_count)) : "qi" ((pto_T__)(pc))); break; case 2: asm("mov" "w %1,""%%""gs"":" "%P" "0" : "+m" ((__preempt_count)) : "ri" ((pto_T__)(pc))); break; case 4: asm("mov" "l %1,""%%""gs"":" "%P" "0" : "+m" ((__preempt_count)) : "ri" ((pto_T__)(pc))); break; case 8: asm("mov" "q %1,""%%""gs"":" "%P" "0" : "+m" ((__preempt_count)) : "re" ((pto_T__)(pc))); break; default: __bad_percpu_size(); } } while (0);
}
# 54 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/preempt.h"
static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) void set_preempt_need_resched(void)
{
 do { typedef typeof((__preempt_count)) pto_T__; if (0) { pto_T__ pto_tmp__; pto_tmp__ = (~0x80000000); (void)pto_tmp__; } switch (sizeof((__preempt_count))) { case 1: asm("and" "b %1,""%%""gs"":" "%P" "0" : "+m" ((__preempt_count)) : "qi" ((pto_T__)(~0x80000000))); break; case 2: asm("and" "w %1,""%%""gs"":" "%P" "0" : "+m" ((__preempt_count)) : "ri" ((pto_T__)(~0x80000000))); break; case 4: asm("and" "l %1,""%%""gs"":" "%P" "0" : "+m" ((__preempt_count)) : "ri" ((pto_T__)(~0x80000000))); break; case 8: asm("and" "q %1,""%%""gs"":" "%P" "0" : "+m" ((__preempt_count)) : "re" ((pto_T__)(~0x80000000))); break; default: __bad_percpu_size(); } } while (0);
}

static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) void clear_preempt_need_resched(void)
{
 do { typedef typeof((__preempt_count)) pto_T__; if (0) { pto_T__ pto_tmp__; pto_tmp__ = (0x80000000); (void)pto_tmp__; } switch (sizeof((__preempt_count))) { case 1: asm("or" "b %1,""%%""gs"":" "%P" "0" : "+m" ((__preempt_count)) : "qi" ((pto_T__)(0x80000000))); break; case 2: asm("or" "w %1,""%%""gs"":" "%P" "0" : "+m" ((__preempt_count)) : "ri" ((pto_T__)(0x80000000))); break; case 4: asm("or" "l %1,""%%""gs"":" "%P" "0" : "+m" ((__preempt_count)) : "ri" ((pto_T__)(0x80000000))); break; case 8: asm("or" "q %1,""%%""gs"":" "%P" "0" : "+m" ((__preempt_count)) : "re" ((pto_T__)(0x80000000))); break; default: __bad_percpu_size(); } } while (0);
}

static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) bool test_preempt_need_resched(void)
{
 return !(({ typeof((__preempt_count)) pfo_ret__; switch (sizeof((__preempt_count))) { case 1: asm("mov" "b ""%%""gs"":" "%P" "1"",%0" : "=q" (pfo_ret__) : "m"(__preempt_count)); break; case 2: asm("mov" "w ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"(__preempt_count)); break; case 4: asm("mov" "l ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"(__preempt_count)); break; case 8: asm("mov" "q ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"(__preempt_count)); break; default: __bad_percpu_size(); } pfo_ret__; }) & 0x80000000);
}





static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) void __preempt_count_add(int val)
{
 do { typedef typeof((__preempt_count)) pao_T__; const int pao_ID__ = (__builtin_constant_p(val) && ((val) == 1 || (val) == -1)) ? (int)(val) : 0; if (0) { pao_T__ pao_tmp__; pao_tmp__ = (val); (void)pao_tmp__; } switch (sizeof((__preempt_count))) { case 1: if (pao_ID__ == 1) asm("incb ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count))); else if (pao_ID__ == -1) asm("decb ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count))); else asm("addb %1, ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count)) : "qi" ((pao_T__)(val))); break; case 2: if (pao_ID__ == 1) asm("incw ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count))); else if (pao_ID__ == -1) asm("decw ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count))); else asm("addw %1, ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count)) : "ri" ((pao_T__)(val))); break; case 4: if (pao_ID__ == 1) asm("incl ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count))); else if (pao_ID__ == -1) asm("decl ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count))); else asm("addl %1, ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count)) : "ri" ((pao_T__)(val))); break; case 8: if (pao_ID__ == 1) asm("incq ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count))); else if (pao_ID__ == -1) asm("decq ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count))); else asm("addq %1, ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count)) : "re" ((pao_T__)(val))); break; default: __bad_percpu_size(); } } while (0);
}

static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) void __preempt_count_sub(int val)
{
 do { typedef typeof((__preempt_count)) pao_T__; const int pao_ID__ = (__builtin_constant_p(-val) && ((-val) == 1 || (-val) == -1)) ? (int)(-val) : 0; if (0) { pao_T__ pao_tmp__; pao_tmp__ = (-val); (void)pao_tmp__; } switch (sizeof((__preempt_count))) { case 1: if (pao_ID__ == 1) asm("incb ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count))); else if (pao_ID__ == -1) asm("decb ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count))); else asm("addb %1, ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count)) : "qi" ((pao_T__)(-val))); break; case 2: if (pao_ID__ == 1) asm("incw ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count))); else if (pao_ID__ == -1) asm("decw ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count))); else asm("addw %1, ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count)) : "ri" ((pao_T__)(-val))); break; case 4: if (pao_ID__ == 1) asm("incl ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count))); else if (pao_ID__ == -1) asm("decl ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count))); else asm("addl %1, ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count)) : "ri" ((pao_T__)(-val))); break; case 8: if (pao_ID__ == 1) asm("incq ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count))); else if (pao_ID__ == -1) asm("decq ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count))); else asm("addq %1, ""%%""gs"":" "%P" "0" : "+m" ((__preempt_count)) : "re" ((pao_T__)(-val))); break; default: __bad_percpu_size(); } } while (0);
}






static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) bool __preempt_count_dec_and_test(void)
{
 do { char c; asm volatile ("decl" " " "%%""gs"":" "%P" "0" "; set" "e" " %1" : "+m" (__preempt_count), "=qm" (c) : : "memory"); return c != 0; } while (0);
}




static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) bool should_resched(void)
{
 return __builtin_expect(!!(!({ typeof((__preempt_count)) pfo_ret__; switch (sizeof((__preempt_count))) { case 1: asm("mov" "b ""%%""gs"":" "%P" "1"",%0" : "=q" (pfo_ret__) : "m"(__preempt_count)); break; case 2: asm("mov" "w ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"(__preempt_count)); break; case 4: asm("mov" "l ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"(__preempt_count)); break; case 8: asm("mov" "q ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"(__preempt_count)); break; default: __bad_percpu_size(); } pfo_ret__; })), 0);
}
# 19 "include/linux/preempt.h" 2
# 149 "include/linux/preempt.h"
struct preempt_notifier;
# 165 "include/linux/preempt.h"
struct preempt_ops {
 void (*sched_in)(struct preempt_notifier *notifier, int cpu);
 void (*sched_out)(struct preempt_notifier *notifier,
     struct task_struct *next);
};
# 178 "include/linux/preempt.h"
struct preempt_notifier {
 struct hlist_node link;
 struct preempt_ops *ops;
};

void preempt_notifier_register(struct preempt_notifier *notifier);
void preempt_notifier_unregister(struct preempt_notifier *notifier);

static inline __attribute__((no_instrument_function)) void preempt_notifier_init(struct preempt_notifier *notifier,
         struct preempt_ops *ops)
{
 INIT_HLIST_NODE(&notifier->link);
 notifier->ops = ops;
}
# 51 "include/linux/spinlock.h" 2






# 1 "include/linux/bottom_half.h" 1




# 1 "include/linux/preempt_mask.h" 1
# 6 "include/linux/bottom_half.h" 2


extern void __local_bh_disable_ip(unsigned long ip, unsigned int cnt);
# 17 "include/linux/bottom_half.h"
static inline __attribute__((no_instrument_function)) void local_bh_disable(void)
{
 __local_bh_disable_ip(0, (2 * (1UL << (0 + 8))));
}

extern void _local_bh_enable(void);
extern void __local_bh_enable_ip(unsigned long ip, unsigned int cnt);

static inline __attribute__((no_instrument_function)) void local_bh_enable_ip(unsigned long ip)
{
 __local_bh_enable_ip(ip, (2 * (1UL << (0 + 8))));
}

static inline __attribute__((no_instrument_function)) void local_bh_enable(void)
{
 __local_bh_enable_ip(0, (2 * (1UL << (0 + 8))));
}
# 58 "include/linux/spinlock.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/barrier.h" 1
# 147 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/barrier.h"
static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) void rdtsc_barrier(void)
{
 asm volatile ("661:\n\t" ".byte " "0x66,0x66,0x90" "\n" "\n662:\n" ".pushsection .altinstructions,\"a\"\n" " .long 661b - .\n" " .long " "663""1""f - .\n" " .word " "(3*32+17)" "\n" " .byte " "662b-661b" "\n" " .byte " "664""1""f-""663""1""f" "\n" ".popsection\n" ".pushsection .discard,\"aw\",@progbits\n" " .byte 0xff + (" "664""1""f-""663""1""f" ") - (" "662b-661b" ")\n" ".popsection\n" ".pushsection .altinstr_replacement, \"ax\"\n" "663""1"":\n\t" "mfence" "\n" "664""1" ":\n\t" ".popsection" : : : "memory");
 asm volatile ("661:\n\t" ".byte " "0x66,0x66,0x90" "\n" "\n662:\n" ".pushsection .altinstructions,\"a\"\n" " .long 661b - .\n" " .long " "663""1""f - .\n" " .word " "(3*32+18)" "\n" " .byte " "662b-661b" "\n" " .byte " "664""1""f-""663""1""f" "\n" ".popsection\n" ".pushsection .discard,\"aw\",@progbits\n" " .byte 0xff + (" "664""1""f-""663""1""f" ") - (" "662b-661b" ")\n" ".popsection\n" ".pushsection .altinstr_replacement, \"ax\"\n" "663""1"":\n\t" "lfence" "\n" "664""1" ":\n\t" ".popsection" : : : "memory");
}
# 59 "include/linux/spinlock.h" 2
# 87 "include/linux/spinlock.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/spinlock.h" 1



# 1 "include/linux/jump_label.h" 1
# 53 "include/linux/jump_label.h"
extern bool static_key_initialized;
# 74 "include/linux/jump_label.h"
enum jump_label_type {
 JUMP_LABEL_DISABLE = 0,
 JUMP_LABEL_ENABLE,
};

struct module;
# 137 "include/linux/jump_label.h"
struct static_key {
 atomic_t enabled;
};

static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) void jump_label_init(void)
{
 static_key_initialized = true;
}

static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) bool static_key_false(struct static_key *key)
{
 if (__builtin_expect(!!(atomic_read(&key->enabled) > 0), 0))
  return true;
 return false;
}

static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) bool static_key_true(struct static_key *key)
{
 if (__builtin_expect(!!(atomic_read(&key->enabled) > 0), 1))
  return true;
 return false;
}

static inline __attribute__((no_instrument_function)) void static_key_slow_inc(struct static_key *key)
{
 ({ int __ret_warn_on = !!(!static_key_initialized); if (__builtin_expect(!!(__ret_warn_on), 0)) warn_slowpath_fmt("include/linux/jump_label.h", 162, "%s used before call to jump_label_init", __func__); __builtin_expect(!!(__ret_warn_on), 0); });
 atomic_inc(&key->enabled);
}

static inline __attribute__((no_instrument_function)) void static_key_slow_dec(struct static_key *key)
{
 ({ int __ret_warn_on = !!(!static_key_initialized); if (__builtin_expect(!!(__ret_warn_on), 0)) warn_slowpath_fmt("include/linux/jump_label.h", 168, "%s used before call to jump_label_init", __func__); __builtin_expect(!!(__ret_warn_on), 0); });
 atomic_dec(&key->enabled);
}

static inline __attribute__((no_instrument_function)) int jump_label_text_reserved(void *start, void *end)
{
 return 0;
}

static inline __attribute__((no_instrument_function)) void jump_label_lock(void) {}
static inline __attribute__((no_instrument_function)) void jump_label_unlock(void) {}

static inline __attribute__((no_instrument_function)) int jump_label_apply_nops(struct module *mod)
{
 return 0;
}
# 195 "include/linux/jump_label.h"
static inline __attribute__((no_instrument_function)) bool static_key_enabled(struct static_key *key)
{
 return (atomic_read(&key->enabled) > 0);
}
# 5 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/spinlock.h" 2
# 42 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/spinlock.h"
extern struct static_key paravirt_ticketlocks_enabled;
static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) bool static_key_false(struct static_key *key);



static inline __attribute__((no_instrument_function)) void __ticket_enter_slowpath(arch_spinlock_t *lock)
{
 set_bit(0, (volatile unsigned long *)&lock->tickets.tail);
}
# 64 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/spinlock.h"
static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) int arch_spin_value_unlocked(arch_spinlock_t lock)
{
 return lock.tickets.head == lock.tickets.tail;
}
# 82 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/spinlock.h"
static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) void arch_spin_lock(arch_spinlock_t *lock)
{
 register struct __raw_tickets inc = { .tail = ((__ticket_t)2) };

 inc = ({ __typeof__ (*(((&lock->tickets)))) __ret = (((inc))); switch (sizeof(*(((&lock->tickets))))) { case 1: asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "xadd" "b %b0, %1\n" : "+q" (__ret), "+m" (*(((&lock->tickets)))) : : "memory", "cc"); break; case 2: asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "xadd" "w %w0, %1\n" : "+r" (__ret), "+m" (*(((&lock->tickets)))) : : "memory", "cc"); break; case 4: asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "xadd" "l %0, %1\n" : "+r" (__ret), "+m" (*(((&lock->tickets)))) : : "memory", "cc"); break; case 8: asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "xadd" "q %q0, %1\n" : "+r" (__ret), "+m" (*(((&lock->tickets)))) : : "memory", "cc"); break; default: __xadd_wrong_size(); } __ret; });
 if (__builtin_expect(!!(inc.head == inc.tail), 1))
  goto out;

 inc.tail &= ~((__ticket_t)1);
 for (;;) {
  unsigned count = (1 << 15);

  do {
   if ((*(volatile typeof(lock->tickets.head) *)&(lock->tickets.head)) == inc.tail)
    goto out;
   cpu_relax();
  } while (--count);
  __ticket_lock_spinning(lock, inc.tail);
 }
out: __asm__ __volatile__("": : :"memory");
}

static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) int arch_spin_trylock(arch_spinlock_t *lock)
{
 arch_spinlock_t old, new;

 old.tickets = (*(volatile typeof(lock->tickets) *)&(lock->tickets));
 if (old.tickets.head != (old.tickets.tail & ~((__ticket_t)1)))
  return 0;

 new.head_tail = old.head_tail + (((__ticket_t)2) << (sizeof(__ticket_t) * 8));


 return ({ __typeof__(*((&lock->head_tail))) __ret; __typeof__(*((&lock->head_tail))) __old = ((old.head_tail)); __typeof__(*((&lock->head_tail))) __new = ((new.head_tail)); switch ((sizeof(*(&lock->head_tail)))) { case 1: { volatile u8 *__ptr = (volatile u8 *)((&lock->head_tail)); asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "cmpxchgb %2,%1" : "=a" (__ret), "+m" (*__ptr) : "q" (__new), "0" (__old) : "memory"); break; } case 2: { volatile u16 *__ptr = (volatile u16 *)((&lock->head_tail)); asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "cmpxchgw %2,%1" : "=a" (__ret), "+m" (*__ptr) : "r" (__new), "0" (__old) : "memory"); break; } case 4: { volatile u32 *__ptr = (volatile u32 *)((&lock->head_tail)); asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "cmpxchgl %2,%1" : "=a" (__ret), "+m" (*__ptr) : "r" (__new), "0" (__old) : "memory"); break; } case 8: { volatile u64 *__ptr = (volatile u64 *)((&lock->head_tail)); asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "cmpxchgq %2,%1" : "=a" (__ret), "+m" (*__ptr) : "r" (__new), "0" (__old) : "memory"); break; } default: __cmpxchg_wrong_size(); } __ret; }) == old.head_tail;
}

static inline __attribute__((no_instrument_function)) void __ticket_unlock_slowpath(arch_spinlock_t *lock,
         arch_spinlock_t old)
{
 arch_spinlock_t new;

 ;


 old.tickets.head += ((__ticket_t)2);


 new.head_tail = old.head_tail & ~(((__ticket_t)1) << (sizeof(__ticket_t) * 8));





 if (new.tickets.head != new.tickets.tail ||
     ({ __typeof__(*((&lock->head_tail))) __ret; __typeof__(*((&lock->head_tail))) __old = ((old.head_tail)); __typeof__(*((&lock->head_tail))) __new = ((new.head_tail)); switch ((sizeof(*(&lock->head_tail)))) { case 1: { volatile u8 *__ptr = (volatile u8 *)((&lock->head_tail)); asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "cmpxchgb %2,%1" : "=a" (__ret), "+m" (*__ptr) : "q" (__new), "0" (__old) : "memory"); break; } case 2: { volatile u16 *__ptr = (volatile u16 *)((&lock->head_tail)); asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "cmpxchgw %2,%1" : "=a" (__ret), "+m" (*__ptr) : "r" (__new), "0" (__old) : "memory"); break; } case 4: { volatile u32 *__ptr = (volatile u32 *)((&lock->head_tail)); asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "cmpxchgl %2,%1" : "=a" (__ret), "+m" (*__ptr) : "r" (__new), "0" (__old) : "memory"); break; } case 8: { volatile u64 *__ptr = (volatile u64 *)((&lock->head_tail)); asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "cmpxchgq %2,%1" : "=a" (__ret), "+m" (*__ptr) : "r" (__new), "0" (__old) : "memory"); break; } default: __cmpxchg_wrong_size(); } __ret; })
                    != old.head_tail) {




  __ticket_unlock_kick(lock, old.tickets.head);
 }
}

static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) void arch_spin_unlock(arch_spinlock_t *lock)
{
 if (((__ticket_t)1) &&
     static_key_false(&paravirt_ticketlocks_enabled)) {
  arch_spinlock_t prev;

  prev = *lock;
  ({ __typeof__ (*((&lock->tickets.head))) __ret = ((((__ticket_t)2))); switch (sizeof(*((&lock->tickets.head)))) { case 1: asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "addb %b1, %0\n" : "+m" (*((&lock->tickets.head))) : "qi" ((((__ticket_t)2))) : "memory", "cc"); break; case 2: asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "addw %w1, %0\n" : "+m" (*((&lock->tickets.head))) : "ri" ((((__ticket_t)2))) : "memory", "cc"); break; case 4: asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "addl %1, %0\n" : "+m" (*((&lock->tickets.head))) : "ri" ((((__ticket_t)2))) : "memory", "cc"); break; case 8: asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "addq %1, %0\n" : "+m" (*((&lock->tickets.head))) : "ri" ((((__ticket_t)2))) : "memory", "cc"); break; default: __add_wrong_size(); } __ret; });



  if (__builtin_expect(!!(lock->tickets.tail & ((__ticket_t)1)), 0))
   __ticket_unlock_slowpath(lock, prev);
 } else
  ({ __typeof__ (*(&lock->tickets.head)) __ret = (((__ticket_t)2)); switch (sizeof(*(&lock->tickets.head))) { case 1: asm volatile ( "addb %b1, %0\n" : "+m" (*(&lock->tickets.head)) : "qi" (((__ticket_t)2)) : "memory", "cc"); break; case 2: asm volatile ( "addw %w1, %0\n" : "+m" (*(&lock->tickets.head)) : "ri" (((__ticket_t)2)) : "memory", "cc"); break; case 4: asm volatile ( "addl %1, %0\n" : "+m" (*(&lock->tickets.head)) : "ri" (((__ticket_t)2)) : "memory", "cc"); break; case 8: asm volatile ( "addq %1, %0\n" : "+m" (*(&lock->tickets.head)) : "ri" (((__ticket_t)2)) : "memory", "cc"); break; default: __add_wrong_size(); } __ret; });
}

static inline __attribute__((no_instrument_function)) int arch_spin_is_locked(arch_spinlock_t *lock)
{
 struct __raw_tickets tmp = (*(volatile typeof(lock->tickets) *)&(lock->tickets));

 return tmp.tail != tmp.head;
}

static inline __attribute__((no_instrument_function)) int arch_spin_is_contended(arch_spinlock_t *lock)
{
 struct __raw_tickets tmp = (*(volatile typeof(lock->tickets) *)&(lock->tickets));

 return (__ticket_t)(tmp.tail - tmp.head) > ((__ticket_t)2);
}


static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) void arch_spin_lock_flags(arch_spinlock_t *lock,
        unsigned long flags)
{
 arch_spin_lock(lock);
}

static inline __attribute__((no_instrument_function)) void arch_spin_unlock_wait(arch_spinlock_t *lock)
{
 while (arch_spin_is_locked(lock))
  cpu_relax();
}
# 208 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/spinlock.h"
static inline __attribute__((no_instrument_function)) int arch_read_can_lock(arch_rwlock_t *lock)
{
 return lock->lock > 0;
}





static inline __attribute__((no_instrument_function)) int arch_write_can_lock(arch_rwlock_t *lock)
{
 return lock->write == 1;
}

static inline __attribute__((no_instrument_function)) void arch_read_lock(arch_rwlock_t *rw)
{
 asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " " " "decq" " " " (%0)\n\t"
       "jns 1f\n"
       "call __read_lock_failed\n\t"
       "1:\n"
       ::"D" (rw) : "memory");
}

static inline __attribute__((no_instrument_function)) void arch_write_lock(arch_rwlock_t *rw)
{
 asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " " " "decl" " " "(%0)\n\t"
       "jz 1f\n"
       "call __write_lock_failed\n\t"
       "1:\n"
       ::"D" (&rw->write), "i" (((1L) << 32))
       : "memory");
}

static inline __attribute__((no_instrument_function)) int arch_read_trylock(arch_rwlock_t *lock)
{
 atomic64_t *count = (atomic64_t *)lock;

 if ((atomic64_sub_return(1, (count))) >= 0)
  return 1;
 atomic64_inc(count);
 return 0;
}

static inline __attribute__((no_instrument_function)) int arch_write_trylock(arch_rwlock_t *lock)
{
 atomic_t *count = (atomic_t *)&lock->write;

 if (atomic_sub_and_test(1, count))
  return 1;
 atomic_add(1, count);
 return 0;
}

static inline __attribute__((no_instrument_function)) void arch_read_unlock(arch_rwlock_t *rw)
{
 asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " " " "incq" " " " %0"
       :"+m" (rw->lock) : : "memory");
}

static inline __attribute__((no_instrument_function)) void arch_write_unlock(arch_rwlock_t *rw)
{
 asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " " " "incl" " " "%0"
       : "+m" (rw->write) : "i" (((1L) << 32)) : "memory");
}
# 88 "include/linux/spinlock.h" 2





  extern void __raw_spin_lock_init(raw_spinlock_t *lock, const char *name,
       struct lock_class_key *key);
# 150 "include/linux/spinlock.h"
 extern void do_raw_spin_lock(raw_spinlock_t *lock) ;

 extern int do_raw_spin_trylock(raw_spinlock_t *lock);
 extern void do_raw_spin_unlock(raw_spinlock_t *lock) ;
# 275 "include/linux/spinlock.h"
# 1 "include/linux/rwlock.h" 1
# 18 "include/linux/rwlock.h"
  extern void __rwlock_init(rwlock_t *lock, const char *name,
       struct lock_class_key *key);
# 32 "include/linux/rwlock.h"
 extern void do_raw_read_lock(rwlock_t *lock) ;

 extern int do_raw_read_trylock(rwlock_t *lock);
 extern void do_raw_read_unlock(rwlock_t *lock) ;
 extern void do_raw_write_lock(rwlock_t *lock) ;

 extern int do_raw_write_trylock(rwlock_t *lock);
 extern void do_raw_write_unlock(rwlock_t *lock) ;
# 276 "include/linux/spinlock.h" 2





# 1 "include/linux/spinlock_api_smp.h" 1
# 18 "include/linux/spinlock_api_smp.h"
int in_lock_functions(unsigned long addr);



void __attribute__((section(".spinlock.text"))) _raw_spin_lock(raw_spinlock_t *lock) ;
void __attribute__((section(".spinlock.text"))) _raw_spin_lock_nested(raw_spinlock_t *lock, int subclass)
        ;
void __attribute__((section(".spinlock.text")))
_raw_spin_lock_nest_lock(raw_spinlock_t *lock, struct lockdep_map *map)
        ;
void __attribute__((section(".spinlock.text"))) _raw_spin_lock_bh(raw_spinlock_t *lock) ;
void __attribute__((section(".spinlock.text"))) _raw_spin_lock_irq(raw_spinlock_t *lock)
        ;

unsigned long __attribute__((section(".spinlock.text"))) _raw_spin_lock_irqsave(raw_spinlock_t *lock)
        ;
unsigned long __attribute__((section(".spinlock.text")))
_raw_spin_lock_irqsave_nested(raw_spinlock_t *lock, int subclass)
        ;
int __attribute__((section(".spinlock.text"))) _raw_spin_trylock(raw_spinlock_t *lock);
int __attribute__((section(".spinlock.text"))) _raw_spin_trylock_bh(raw_spinlock_t *lock);
void __attribute__((section(".spinlock.text"))) _raw_spin_unlock(raw_spinlock_t *lock) ;
void __attribute__((section(".spinlock.text"))) _raw_spin_unlock_bh(raw_spinlock_t *lock) ;
void __attribute__((section(".spinlock.text"))) _raw_spin_unlock_irq(raw_spinlock_t *lock) ;
void __attribute__((section(".spinlock.text")))
_raw_spin_unlock_irqrestore(raw_spinlock_t *lock, unsigned long flags)
        ;
# 86 "include/linux/spinlock_api_smp.h"
static inline __attribute__((no_instrument_function)) int __raw_spin_trylock(raw_spinlock_t *lock)
{
 do { __preempt_count_add(1); __asm__ __volatile__("": : :"memory"); } while (0);
 if (do_raw_spin_trylock(lock)) {
  lock_acquire(&lock->dep_map, 0, 1, 0, 1, ((void *)0), (unsigned long)__builtin_return_address(0));
  return 1;
 }
 do { __asm__ __volatile__("": : :"memory"); __preempt_count_sub(1); } while (0);
 return 0;
}
# 104 "include/linux/spinlock_api_smp.h"
static inline __attribute__((no_instrument_function)) unsigned long __raw_spin_lock_irqsave(raw_spinlock_t *lock)
{
 unsigned long flags;

 do { do { ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); flags = arch_local_irq_save(); } while (0); trace_hardirqs_off(); } while (0);
 do { __preempt_count_add(1); __asm__ __volatile__("": : :"memory"); } while (0);
 lock_acquire(&lock->dep_map, 0, 0, 0, 1, ((void *)0), (unsigned long)__builtin_return_address(0));






 do { if (!do_raw_spin_trylock(lock)) { lock_contended(&(lock)->dep_map, (unsigned long)__builtin_return_address(0)); do_raw_spin_lock(lock); } lock_acquired(&(lock)->dep_map, (unsigned long)__builtin_return_address(0)); } while (0);



 return flags;
}

static inline __attribute__((no_instrument_function)) void __raw_spin_lock_irq(raw_spinlock_t *lock)
{
 do { arch_local_irq_disable(); trace_hardirqs_off(); } while (0);
 do { __preempt_count_add(1); __asm__ __volatile__("": : :"memory"); } while (0);
 lock_acquire(&lock->dep_map, 0, 0, 0, 1, ((void *)0), (unsigned long)__builtin_return_address(0));
 do { if (!do_raw_spin_trylock(lock)) { lock_contended(&(lock)->dep_map, (unsigned long)__builtin_return_address(0)); do_raw_spin_lock(lock); } lock_acquired(&(lock)->dep_map, (unsigned long)__builtin_return_address(0)); } while (0);
}

static inline __attribute__((no_instrument_function)) void __raw_spin_lock_bh(raw_spinlock_t *lock)
{
 __local_bh_disable_ip((unsigned long)__builtin_return_address(0), ((2 * (1UL << (0 + 8))) + 1));
 lock_acquire(&lock->dep_map, 0, 0, 0, 1, ((void *)0), (unsigned long)__builtin_return_address(0));
 do { if (!do_raw_spin_trylock(lock)) { lock_contended(&(lock)->dep_map, (unsigned long)__builtin_return_address(0)); do_raw_spin_lock(lock); } lock_acquired(&(lock)->dep_map, (unsigned long)__builtin_return_address(0)); } while (0);
}

static inline __attribute__((no_instrument_function)) void __raw_spin_lock(raw_spinlock_t *lock)
{
 do { __preempt_count_add(1); __asm__ __volatile__("": : :"memory"); } while (0);
 lock_acquire(&lock->dep_map, 0, 0, 0, 1, ((void *)0), (unsigned long)__builtin_return_address(0));
 do { if (!do_raw_spin_trylock(lock)) { lock_contended(&(lock)->dep_map, (unsigned long)__builtin_return_address(0)); do_raw_spin_lock(lock); } lock_acquired(&(lock)->dep_map, (unsigned long)__builtin_return_address(0)); } while (0);
}



static inline __attribute__((no_instrument_function)) void __raw_spin_unlock(raw_spinlock_t *lock)
{
 lock_release(&lock->dep_map, 1, (unsigned long)__builtin_return_address(0));
 do_raw_spin_unlock(lock);
 do { __asm__ __volatile__("": : :"memory"); __preempt_count_sub(1); } while (0);
}

static inline __attribute__((no_instrument_function)) void __raw_spin_unlock_irqrestore(raw_spinlock_t *lock,
         unsigned long flags)
{
 lock_release(&lock->dep_map, 1, (unsigned long)__builtin_return_address(0));
 do_raw_spin_unlock(lock);
 do { if (({ ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); arch_irqs_disabled_flags(flags); })) { do { ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); arch_local_irq_restore(flags); } while (0); trace_hardirqs_off(); } else { trace_hardirqs_on(); do { ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); arch_local_irq_restore(flags); } while (0); } } while (0);
 do { __asm__ __volatile__("": : :"memory"); __preempt_count_sub(1); } while (0);
}

static inline __attribute__((no_instrument_function)) void __raw_spin_unlock_irq(raw_spinlock_t *lock)
{
 lock_release(&lock->dep_map, 1, (unsigned long)__builtin_return_address(0));
 do_raw_spin_unlock(lock);
 do { trace_hardirqs_on(); arch_local_irq_enable(); } while (0);
 do { __asm__ __volatile__("": : :"memory"); __preempt_count_sub(1); } while (0);
}

static inline __attribute__((no_instrument_function)) void __raw_spin_unlock_bh(raw_spinlock_t *lock)
{
 lock_release(&lock->dep_map, 1, (unsigned long)__builtin_return_address(0));
 do_raw_spin_unlock(lock);
 __local_bh_enable_ip((unsigned long)__builtin_return_address(0), ((2 * (1UL << (0 + 8))) + 1));
}

static inline __attribute__((no_instrument_function)) int __raw_spin_trylock_bh(raw_spinlock_t *lock)
{
 __local_bh_disable_ip((unsigned long)__builtin_return_address(0), ((2 * (1UL << (0 + 8))) + 1));
 if (do_raw_spin_trylock(lock)) {
  lock_acquire(&lock->dep_map, 0, 1, 0, 1, ((void *)0), (unsigned long)__builtin_return_address(0));
  return 1;
 }
 __local_bh_enable_ip((unsigned long)__builtin_return_address(0), ((2 * (1UL << (0 + 8))) + 1));
 return 0;
}

# 1 "include/linux/rwlock_api_smp.h" 1
# 18 "include/linux/rwlock_api_smp.h"
void __attribute__((section(".spinlock.text"))) _raw_read_lock(rwlock_t *lock) ;
void __attribute__((section(".spinlock.text"))) _raw_write_lock(rwlock_t *lock) ;
void __attribute__((section(".spinlock.text"))) _raw_read_lock_bh(rwlock_t *lock) ;
void __attribute__((section(".spinlock.text"))) _raw_write_lock_bh(rwlock_t *lock) ;
void __attribute__((section(".spinlock.text"))) _raw_read_lock_irq(rwlock_t *lock) ;
void __attribute__((section(".spinlock.text"))) _raw_write_lock_irq(rwlock_t *lock) ;
unsigned long __attribute__((section(".spinlock.text"))) _raw_read_lock_irqsave(rwlock_t *lock)
       ;
unsigned long __attribute__((section(".spinlock.text"))) _raw_write_lock_irqsave(rwlock_t *lock)
       ;
int __attribute__((section(".spinlock.text"))) _raw_read_trylock(rwlock_t *lock);
int __attribute__((section(".spinlock.text"))) _raw_write_trylock(rwlock_t *lock);
void __attribute__((section(".spinlock.text"))) _raw_read_unlock(rwlock_t *lock) ;
void __attribute__((section(".spinlock.text"))) _raw_write_unlock(rwlock_t *lock) ;
void __attribute__((section(".spinlock.text"))) _raw_read_unlock_bh(rwlock_t *lock) ;
void __attribute__((section(".spinlock.text"))) _raw_write_unlock_bh(rwlock_t *lock) ;
void __attribute__((section(".spinlock.text"))) _raw_read_unlock_irq(rwlock_t *lock) ;
void __attribute__((section(".spinlock.text"))) _raw_write_unlock_irq(rwlock_t *lock) ;
void __attribute__((section(".spinlock.text")))
_raw_read_unlock_irqrestore(rwlock_t *lock, unsigned long flags)
       ;
void __attribute__((section(".spinlock.text")))
_raw_write_unlock_irqrestore(rwlock_t *lock, unsigned long flags)
       ;
# 117 "include/linux/rwlock_api_smp.h"
static inline __attribute__((no_instrument_function)) int __raw_read_trylock(rwlock_t *lock)
{
 do { __preempt_count_add(1); __asm__ __volatile__("": : :"memory"); } while (0);
 if (do_raw_read_trylock(lock)) {
  lock_acquire(&lock->dep_map, 0, 1, 2, 1, ((void *)0), (unsigned long)__builtin_return_address(0));
  return 1;
 }
 do { __asm__ __volatile__("": : :"memory"); __preempt_count_sub(1); } while (0);
 return 0;
}

static inline __attribute__((no_instrument_function)) int __raw_write_trylock(rwlock_t *lock)
{
 do { __preempt_count_add(1); __asm__ __volatile__("": : :"memory"); } while (0);
 if (do_raw_write_trylock(lock)) {
  lock_acquire(&lock->dep_map, 0, 1, 0, 1, ((void *)0), (unsigned long)__builtin_return_address(0));
  return 1;
 }
 do { __asm__ __volatile__("": : :"memory"); __preempt_count_sub(1); } while (0);
 return 0;
}
# 146 "include/linux/rwlock_api_smp.h"
static inline __attribute__((no_instrument_function)) void __raw_read_lock(rwlock_t *lock)
{
 do { __preempt_count_add(1); __asm__ __volatile__("": : :"memory"); } while (0);
 lock_acquire(&lock->dep_map, 0, 0, 2, 1, ((void *)0), (unsigned long)__builtin_return_address(0));
 do { if (!do_raw_read_trylock(lock)) { lock_contended(&(lock)->dep_map, (unsigned long)__builtin_return_address(0)); do_raw_read_lock(lock); } lock_acquired(&(lock)->dep_map, (unsigned long)__builtin_return_address(0)); } while (0);
}

static inline __attribute__((no_instrument_function)) unsigned long __raw_read_lock_irqsave(rwlock_t *lock)
{
 unsigned long flags;

 do { do { ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); flags = arch_local_irq_save(); } while (0); trace_hardirqs_off(); } while (0);
 do { __preempt_count_add(1); __asm__ __volatile__("": : :"memory"); } while (0);
 lock_acquire(&lock->dep_map, 0, 0, 2, 1, ((void *)0), (unsigned long)__builtin_return_address(0));
 do { if (!(do_raw_read_trylock)((lock))) { lock_contended(&((lock))->dep_map, (unsigned long)__builtin_return_address(0)); (do_raw_read_lock)((lock)); } lock_acquired(&((lock))->dep_map, (unsigned long)__builtin_return_address(0)); } while (0)
                                       ;
 return flags;
}

static inline __attribute__((no_instrument_function)) void __raw_read_lock_irq(rwlock_t *lock)
{
 do { arch_local_irq_disable(); trace_hardirqs_off(); } while (0);
 do { __preempt_count_add(1); __asm__ __volatile__("": : :"memory"); } while (0);
 lock_acquire(&lock->dep_map, 0, 0, 2, 1, ((void *)0), (unsigned long)__builtin_return_address(0));
 do { if (!do_raw_read_trylock(lock)) { lock_contended(&(lock)->dep_map, (unsigned long)__builtin_return_address(0)); do_raw_read_lock(lock); } lock_acquired(&(lock)->dep_map, (unsigned long)__builtin_return_address(0)); } while (0);
}

static inline __attribute__((no_instrument_function)) void __raw_read_lock_bh(rwlock_t *lock)
{
 __local_bh_disable_ip((unsigned long)__builtin_return_address(0), ((2 * (1UL << (0 + 8))) + 1));
 lock_acquire(&lock->dep_map, 0, 0, 2, 1, ((void *)0), (unsigned long)__builtin_return_address(0));
 do { if (!do_raw_read_trylock(lock)) { lock_contended(&(lock)->dep_map, (unsigned long)__builtin_return_address(0)); do_raw_read_lock(lock); } lock_acquired(&(lock)->dep_map, (unsigned long)__builtin_return_address(0)); } while (0);
}

static inline __attribute__((no_instrument_function)) unsigned long __raw_write_lock_irqsave(rwlock_t *lock)
{
 unsigned long flags;

 do { do { ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); flags = arch_local_irq_save(); } while (0); trace_hardirqs_off(); } while (0);
 do { __preempt_count_add(1); __asm__ __volatile__("": : :"memory"); } while (0);
 lock_acquire(&lock->dep_map, 0, 0, 0, 1, ((void *)0), (unsigned long)__builtin_return_address(0));
 do { if (!(do_raw_write_trylock)((lock))) { lock_contended(&((lock))->dep_map, (unsigned long)__builtin_return_address(0)); (do_raw_write_lock)((lock)); } lock_acquired(&((lock))->dep_map, (unsigned long)__builtin_return_address(0)); } while (0)
                                        ;
 return flags;
}

static inline __attribute__((no_instrument_function)) void __raw_write_lock_irq(rwlock_t *lock)
{
 do { arch_local_irq_disable(); trace_hardirqs_off(); } while (0);
 do { __preempt_count_add(1); __asm__ __volatile__("": : :"memory"); } while (0);
 lock_acquire(&lock->dep_map, 0, 0, 0, 1, ((void *)0), (unsigned long)__builtin_return_address(0));
 do { if (!do_raw_write_trylock(lock)) { lock_contended(&(lock)->dep_map, (unsigned long)__builtin_return_address(0)); do_raw_write_lock(lock); } lock_acquired(&(lock)->dep_map, (unsigned long)__builtin_return_address(0)); } while (0);
}

static inline __attribute__((no_instrument_function)) void __raw_write_lock_bh(rwlock_t *lock)
{
 __local_bh_disable_ip((unsigned long)__builtin_return_address(0), ((2 * (1UL << (0 + 8))) + 1));
 lock_acquire(&lock->dep_map, 0, 0, 0, 1, ((void *)0), (unsigned long)__builtin_return_address(0));
 do { if (!do_raw_write_trylock(lock)) { lock_contended(&(lock)->dep_map, (unsigned long)__builtin_return_address(0)); do_raw_write_lock(lock); } lock_acquired(&(lock)->dep_map, (unsigned long)__builtin_return_address(0)); } while (0);
}

static inline __attribute__((no_instrument_function)) void __raw_write_lock(rwlock_t *lock)
{
 do { __preempt_count_add(1); __asm__ __volatile__("": : :"memory"); } while (0);
 lock_acquire(&lock->dep_map, 0, 0, 0, 1, ((void *)0), (unsigned long)__builtin_return_address(0));
 do { if (!do_raw_write_trylock(lock)) { lock_contended(&(lock)->dep_map, (unsigned long)__builtin_return_address(0)); do_raw_write_lock(lock); } lock_acquired(&(lock)->dep_map, (unsigned long)__builtin_return_address(0)); } while (0);
}



static inline __attribute__((no_instrument_function)) void __raw_write_unlock(rwlock_t *lock)
{
 lock_release(&lock->dep_map, 1, (unsigned long)__builtin_return_address(0));
 do_raw_write_unlock(lock);
 do { __asm__ __volatile__("": : :"memory"); __preempt_count_sub(1); } while (0);
}

static inline __attribute__((no_instrument_function)) void __raw_read_unlock(rwlock_t *lock)
{
 lock_release(&lock->dep_map, 1, (unsigned long)__builtin_return_address(0));
 do_raw_read_unlock(lock);
 do { __asm__ __volatile__("": : :"memory"); __preempt_count_sub(1); } while (0);
}

static inline __attribute__((no_instrument_function)) void
__raw_read_unlock_irqrestore(rwlock_t *lock, unsigned long flags)
{
 lock_release(&lock->dep_map, 1, (unsigned long)__builtin_return_address(0));
 do_raw_read_unlock(lock);
 do { if (({ ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); arch_irqs_disabled_flags(flags); })) { do { ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); arch_local_irq_restore(flags); } while (0); trace_hardirqs_off(); } else { trace_hardirqs_on(); do { ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); arch_local_irq_restore(flags); } while (0); } } while (0);
 do { __asm__ __volatile__("": : :"memory"); __preempt_count_sub(1); } while (0);
}

static inline __attribute__((no_instrument_function)) void __raw_read_unlock_irq(rwlock_t *lock)
{
 lock_release(&lock->dep_map, 1, (unsigned long)__builtin_return_address(0));
 do_raw_read_unlock(lock);
 do { trace_hardirqs_on(); arch_local_irq_enable(); } while (0);
 do { __asm__ __volatile__("": : :"memory"); __preempt_count_sub(1); } while (0);
}

static inline __attribute__((no_instrument_function)) void __raw_read_unlock_bh(rwlock_t *lock)
{
 lock_release(&lock->dep_map, 1, (unsigned long)__builtin_return_address(0));
 do_raw_read_unlock(lock);
 __local_bh_enable_ip((unsigned long)__builtin_return_address(0), ((2 * (1UL << (0 + 8))) + 1));
}

static inline __attribute__((no_instrument_function)) void __raw_write_unlock_irqrestore(rwlock_t *lock,
          unsigned long flags)
{
 lock_release(&lock->dep_map, 1, (unsigned long)__builtin_return_address(0));
 do_raw_write_unlock(lock);
 do { if (({ ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); arch_irqs_disabled_flags(flags); })) { do { ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); arch_local_irq_restore(flags); } while (0); trace_hardirqs_off(); } else { trace_hardirqs_on(); do { ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); arch_local_irq_restore(flags); } while (0); } } while (0);
 do { __asm__ __volatile__("": : :"memory"); __preempt_count_sub(1); } while (0);
}

static inline __attribute__((no_instrument_function)) void __raw_write_unlock_irq(rwlock_t *lock)
{
 lock_release(&lock->dep_map, 1, (unsigned long)__builtin_return_address(0));
 do_raw_write_unlock(lock);
 do { trace_hardirqs_on(); arch_local_irq_enable(); } while (0);
 do { __asm__ __volatile__("": : :"memory"); __preempt_count_sub(1); } while (0);
}

static inline __attribute__((no_instrument_function)) void __raw_write_unlock_bh(rwlock_t *lock)
{
 lock_release(&lock->dep_map, 1, (unsigned long)__builtin_return_address(0));
 do_raw_write_unlock(lock);
 __local_bh_enable_ip((unsigned long)__builtin_return_address(0), ((2 * (1UL << (0 + 8))) + 1));
}
# 191 "include/linux/spinlock_api_smp.h" 2
# 282 "include/linux/spinlock.h" 2
# 290 "include/linux/spinlock.h"
static inline __attribute__((no_instrument_function)) raw_spinlock_t *spinlock_check(spinlock_t *lock)
{
 return &lock->rlock;
}







static inline __attribute__((no_instrument_function)) void spin_lock(spinlock_t *lock)
{
 _raw_spin_lock(&lock->rlock);
}

static inline __attribute__((no_instrument_function)) void spin_lock_bh(spinlock_t *lock)
{
 _raw_spin_lock_bh(&lock->rlock);
}

static inline __attribute__((no_instrument_function)) int spin_trylock(spinlock_t *lock)
{
 return (_raw_spin_trylock(&lock->rlock));
}
# 326 "include/linux/spinlock.h"
static inline __attribute__((no_instrument_function)) void spin_lock_irq(spinlock_t *lock)
{
 _raw_spin_lock_irq(&lock->rlock);
}
# 341 "include/linux/spinlock.h"
static inline __attribute__((no_instrument_function)) void spin_unlock(spinlock_t *lock)
{
 _raw_spin_unlock(&lock->rlock);
}

static inline __attribute__((no_instrument_function)) void spin_unlock_bh(spinlock_t *lock)
{
 _raw_spin_unlock_bh(&lock->rlock);
}

static inline __attribute__((no_instrument_function)) void spin_unlock_irq(spinlock_t *lock)
{
 _raw_spin_unlock_irq(&lock->rlock);
}

static inline __attribute__((no_instrument_function)) void spin_unlock_irqrestore(spinlock_t *lock, unsigned long flags)
{
 do { ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); _raw_spin_unlock_irqrestore(&lock->rlock, flags); } while (0);
}

static inline __attribute__((no_instrument_function)) int spin_trylock_bh(spinlock_t *lock)
{
 return (_raw_spin_trylock_bh(&lock->rlock));
}

static inline __attribute__((no_instrument_function)) int spin_trylock_irq(spinlock_t *lock)
{
 return ({ do { arch_local_irq_disable(); trace_hardirqs_off(); } while (0); (_raw_spin_trylock(&lock->rlock)) ? 1 : ({ do { trace_hardirqs_on(); arch_local_irq_enable(); } while (0); 0; }); });
}






static inline __attribute__((no_instrument_function)) void spin_unlock_wait(spinlock_t *lock)
{
 arch_spin_unlock_wait(&(&lock->rlock)->raw_lock);
}

static inline __attribute__((no_instrument_function)) int spin_is_locked(spinlock_t *lock)
{
 return arch_spin_is_locked(&(&lock->rlock)->raw_lock);
}

static inline __attribute__((no_instrument_function)) int spin_is_contended(spinlock_t *lock)
{
 return arch_spin_is_contended(&(&lock->rlock)->raw_lock);
}

static inline __attribute__((no_instrument_function)) int spin_can_lock(spinlock_t *lock)
{
 return (!arch_spin_is_locked(&(&lock->rlock)->raw_lock));
}
# 411 "include/linux/spinlock.h"
extern int _atomic_dec_and_lock(atomic_t *atomic, spinlock_t *lock);
# 39 "include/linux/rcupdate.h" 2


# 1 "include/linux/seqlock.h" 1
# 46 "include/linux/seqlock.h"
typedef struct seqcount {
 unsigned sequence;

 struct lockdep_map dep_map;

} seqcount_t;

static inline __attribute__((no_instrument_function)) void __seqcount_init(seqcount_t *s, const char *name,
       struct lock_class_key *key)
{



 lockdep_init_map(&s->dep_map, name, key, 0);
 s->sequence = 0;
}
# 73 "include/linux/seqlock.h"
static inline __attribute__((no_instrument_function)) void seqcount_lockdep_reader_access(const seqcount_t *s)
{
 seqcount_t *l = (seqcount_t *)s;
 unsigned long flags;

 do { do { ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); flags = arch_local_irq_save(); } while (0); trace_hardirqs_off(); } while (0);
 lock_acquire(&l->dep_map, 0, 0, 2, 1, ((void *)0), (unsigned long)__builtin_return_address(0));
 lock_release(&l->dep_map, 1, (unsigned long)__builtin_return_address(0));
 do { if (({ ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); arch_irqs_disabled_flags(flags); })) { do { ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); arch_local_irq_restore(flags); } while (0); trace_hardirqs_off(); } else { trace_hardirqs_on(); do { ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); arch_local_irq_restore(flags); } while (0); } } while (0);
}
# 106 "include/linux/seqlock.h"
static inline __attribute__((no_instrument_function)) unsigned __read_seqcount_begin(const seqcount_t *s)
{
 unsigned ret;

repeat:
 ret = (*(volatile typeof(s->sequence) *)&(s->sequence));
 if (__builtin_expect(!!(ret & 1), 0)) {
  cpu_relax();
  goto repeat;
 }
 return ret;
}
# 128 "include/linux/seqlock.h"
static inline __attribute__((no_instrument_function)) unsigned raw_read_seqcount_begin(const seqcount_t *s)
{
 unsigned ret = __read_seqcount_begin(s);
 __asm__ __volatile__("": : :"memory");
 return ret;
}
# 144 "include/linux/seqlock.h"
static inline __attribute__((no_instrument_function)) unsigned read_seqcount_begin(const seqcount_t *s)
{
 seqcount_lockdep_reader_access(s);
 return raw_read_seqcount_begin(s);
}
# 164 "include/linux/seqlock.h"
static inline __attribute__((no_instrument_function)) unsigned raw_seqcount_begin(const seqcount_t *s)
{
 unsigned ret = (*(volatile typeof(s->sequence) *)&(s->sequence));

 seqcount_lockdep_reader_access(s);
 __asm__ __volatile__("": : :"memory");
 return ret & ~1;
}
# 187 "include/linux/seqlock.h"
static inline __attribute__((no_instrument_function)) int __read_seqcount_retry(const seqcount_t *s, unsigned start)
{
 return __builtin_expect(!!(s->sequence != start), 0);
}
# 202 "include/linux/seqlock.h"
static inline __attribute__((no_instrument_function)) int read_seqcount_retry(const seqcount_t *s, unsigned start)
{
 __asm__ __volatile__("": : :"memory");
 return __read_seqcount_retry(s, start);
}



static inline __attribute__((no_instrument_function)) void raw_write_seqcount_begin(seqcount_t *s)
{
 s->sequence++;
 __asm__ __volatile__("": : :"memory");
}

static inline __attribute__((no_instrument_function)) void raw_write_seqcount_end(seqcount_t *s)
{
 __asm__ __volatile__("": : :"memory");
 s->sequence++;
}





static inline __attribute__((no_instrument_function)) void write_seqcount_begin_nested(seqcount_t *s, int subclass)
{
 raw_write_seqcount_begin(s);
 lock_acquire(&s->dep_map, subclass, 0, 0, 1, ((void *)0), (unsigned long)__builtin_return_address(0));
}

static inline __attribute__((no_instrument_function)) void write_seqcount_begin(seqcount_t *s)
{
 write_seqcount_begin_nested(s, 0);
}

static inline __attribute__((no_instrument_function)) void write_seqcount_end(seqcount_t *s)
{
 lock_release(&s->dep_map, 1, (unsigned long)__builtin_return_address(0));
 raw_write_seqcount_end(s);
}
# 250 "include/linux/seqlock.h"
static inline __attribute__((no_instrument_function)) void write_seqcount_barrier(seqcount_t *s)
{
 __asm__ __volatile__("": : :"memory");
 s->sequence+=2;
}

typedef struct {
 struct seqcount seqcount;
 spinlock_t lock;
} seqlock_t;
# 283 "include/linux/seqlock.h"
static inline __attribute__((no_instrument_function)) unsigned read_seqbegin(const seqlock_t *sl)
{
 return read_seqcount_begin(&sl->seqcount);
}

static inline __attribute__((no_instrument_function)) unsigned read_seqretry(const seqlock_t *sl, unsigned start)
{
 return read_seqcount_retry(&sl->seqcount, start);
}






static inline __attribute__((no_instrument_function)) void write_seqlock(seqlock_t *sl)
{
 spin_lock(&sl->lock);
 write_seqcount_begin(&sl->seqcount);
}

static inline __attribute__((no_instrument_function)) void write_sequnlock(seqlock_t *sl)
{
 write_seqcount_end(&sl->seqcount);
 spin_unlock(&sl->lock);
}

static inline __attribute__((no_instrument_function)) void write_seqlock_bh(seqlock_t *sl)
{
 spin_lock_bh(&sl->lock);
 write_seqcount_begin(&sl->seqcount);
}

static inline __attribute__((no_instrument_function)) void write_sequnlock_bh(seqlock_t *sl)
{
 write_seqcount_end(&sl->seqcount);
 spin_unlock_bh(&sl->lock);
}

static inline __attribute__((no_instrument_function)) void write_seqlock_irq(seqlock_t *sl)
{
 spin_lock_irq(&sl->lock);
 write_seqcount_begin(&sl->seqcount);
}

static inline __attribute__((no_instrument_function)) void write_sequnlock_irq(seqlock_t *sl)
{
 write_seqcount_end(&sl->seqcount);
 spin_unlock_irq(&sl->lock);
}

static inline __attribute__((no_instrument_function)) unsigned long __write_seqlock_irqsave(seqlock_t *sl)
{
 unsigned long flags;

 do { do { ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); flags = _raw_spin_lock_irqsave(spinlock_check(&sl->lock)); } while (0); } while (0);
 write_seqcount_begin(&sl->seqcount);
 return flags;
}




static inline __attribute__((no_instrument_function)) void
write_sequnlock_irqrestore(seqlock_t *sl, unsigned long flags)
{
 write_seqcount_end(&sl->seqcount);
 spin_unlock_irqrestore(&sl->lock, flags);
}






static inline __attribute__((no_instrument_function)) void read_seqlock_excl(seqlock_t *sl)
{
 spin_lock(&sl->lock);
}

static inline __attribute__((no_instrument_function)) void read_sequnlock_excl(seqlock_t *sl)
{
 spin_unlock(&sl->lock);
}
# 378 "include/linux/seqlock.h"
static inline __attribute__((no_instrument_function)) void read_seqbegin_or_lock(seqlock_t *lock, int *seq)
{
 if (!(*seq & 1))
  *seq = read_seqbegin(lock);
 else
  read_seqlock_excl(lock);
}

static inline __attribute__((no_instrument_function)) int need_seqretry(seqlock_t *lock, int seq)
{
 return !(seq & 1) && read_seqretry(lock, seq);
}

static inline __attribute__((no_instrument_function)) void done_seqretry(seqlock_t *lock, int seq)
{
 if (seq & 1)
  read_sequnlock_excl(lock);
}

static inline __attribute__((no_instrument_function)) void read_seqlock_excl_bh(seqlock_t *sl)
{
 spin_lock_bh(&sl->lock);
}

static inline __attribute__((no_instrument_function)) void read_sequnlock_excl_bh(seqlock_t *sl)
{
 spin_unlock_bh(&sl->lock);
}

static inline __attribute__((no_instrument_function)) void read_seqlock_excl_irq(seqlock_t *sl)
{
 spin_lock_irq(&sl->lock);
}

static inline __attribute__((no_instrument_function)) void read_sequnlock_excl_irq(seqlock_t *sl)
{
 spin_unlock_irq(&sl->lock);
}

static inline __attribute__((no_instrument_function)) unsigned long __read_seqlock_excl_irqsave(seqlock_t *sl)
{
 unsigned long flags;

 do { do { ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); flags = _raw_spin_lock_irqsave(spinlock_check(&sl->lock)); } while (0); } while (0);
 return flags;
}




static inline __attribute__((no_instrument_function)) void
read_sequnlock_excl_irqrestore(seqlock_t *sl, unsigned long flags)
{
 spin_unlock_irqrestore(&sl->lock, flags);
}
# 42 "include/linux/rcupdate.h" 2

# 1 "include/linux/completion.h" 1
# 11 "include/linux/completion.h"
# 1 "include/linux/wait.h" 1
# 10 "include/linux/wait.h"
# 1 "include/uapi/linux/wait.h" 1
# 11 "include/linux/wait.h" 2

typedef struct __wait_queue wait_queue_t;
typedef int (*wait_queue_func_t)(wait_queue_t *wait, unsigned mode, int flags, void *key);
int default_wake_function(wait_queue_t *wait, unsigned mode, int flags, void *key);

struct __wait_queue {
 unsigned int flags;

 void *private;
 wait_queue_func_t func;
 struct list_head task_list;
};

struct wait_bit_key {
 void *flags;
 int bit_nr;

};

struct wait_bit_queue {
 struct wait_bit_key key;
 wait_queue_t wait;
};

struct __wait_queue_head {
 spinlock_t lock;
 struct list_head task_list;
};
typedef struct __wait_queue_head wait_queue_head_t;

struct task_struct;
# 68 "include/linux/wait.h"
extern void __init_waitqueue_head(wait_queue_head_t *q, const char *name, struct lock_class_key *);
# 86 "include/linux/wait.h"
static inline __attribute__((no_instrument_function)) void init_waitqueue_entry(wait_queue_t *q, struct task_struct *p)
{
 q->flags = 0;
 q->private = p;
 q->func = default_wake_function;
}

static inline __attribute__((no_instrument_function)) void
init_waitqueue_func_entry(wait_queue_t *q, wait_queue_func_t func)
{
 q->flags = 0;
 q->private = ((void *)0);
 q->func = func;
}

static inline __attribute__((no_instrument_function)) int waitqueue_active(wait_queue_head_t *q)
{
 return !list_empty(&q->task_list);
}

extern void add_wait_queue(wait_queue_head_t *q, wait_queue_t *wait);
extern void add_wait_queue_exclusive(wait_queue_head_t *q, wait_queue_t *wait);
extern void remove_wait_queue(wait_queue_head_t *q, wait_queue_t *wait);

static inline __attribute__((no_instrument_function)) void __add_wait_queue(wait_queue_head_t *head, wait_queue_t *new)
{
 list_add(&new->task_list, &head->task_list);
}




static inline __attribute__((no_instrument_function)) void
__add_wait_queue_exclusive(wait_queue_head_t *q, wait_queue_t *wait)
{
 wait->flags |= 0x01;
 __add_wait_queue(q, wait);
}

static inline __attribute__((no_instrument_function)) void __add_wait_queue_tail(wait_queue_head_t *head,
      wait_queue_t *new)
{
 list_add_tail(&new->task_list, &head->task_list);
}

static inline __attribute__((no_instrument_function)) void
__add_wait_queue_tail_exclusive(wait_queue_head_t *q, wait_queue_t *wait)
{
 wait->flags |= 0x01;
 __add_wait_queue_tail(q, wait);
}

static inline __attribute__((no_instrument_function)) void
__remove_wait_queue(wait_queue_head_t *head, wait_queue_t *old)
{
 list_del(&old->task_list);
}

void __wake_up(wait_queue_head_t *q, unsigned int mode, int nr, void *key);
void __wake_up_locked_key(wait_queue_head_t *q, unsigned int mode, void *key);
void __wake_up_sync_key(wait_queue_head_t *q, unsigned int mode, int nr, void *key);
void __wake_up_locked(wait_queue_head_t *q, unsigned int mode, int nr);
void __wake_up_sync(wait_queue_head_t *q, unsigned int mode, int nr);
void __wake_up_bit(wait_queue_head_t *, void *, int);
int __wait_on_bit(wait_queue_head_t *, struct wait_bit_queue *, int (*)(void *), unsigned);
int __wait_on_bit_lock(wait_queue_head_t *, struct wait_bit_queue *, int (*)(void *), unsigned);
void wake_up_bit(void *, int);
void wake_up_atomic_t(atomic_t *);
int out_of_line_wait_on_bit(void *, int, int (*)(void *), unsigned);
int out_of_line_wait_on_bit_lock(void *, int, int (*)(void *), unsigned);
int out_of_line_wait_on_atomic_t(atomic_t *, int (*)(atomic_t *), unsigned);
wait_queue_head_t *bit_waitqueue(void *, int);
# 821 "include/linux/wait.h"
void prepare_to_wait(wait_queue_head_t *q, wait_queue_t *wait, int state);
void prepare_to_wait_exclusive(wait_queue_head_t *q, wait_queue_t *wait, int state);
long prepare_to_wait_event(wait_queue_head_t *q, wait_queue_t *wait, int state);
void finish_wait(wait_queue_head_t *q, wait_queue_t *wait);
void abort_exclusive_wait(wait_queue_head_t *q, wait_queue_t *wait, unsigned int mode, void *key);
int autoremove_wake_function(wait_queue_t *wait, unsigned mode, int sync, void *key);
int wake_bit_function(wait_queue_t *wait, unsigned mode, int sync, void *key);
# 871 "include/linux/wait.h"
static inline __attribute__((no_instrument_function)) int
wait_on_bit(void *word, int bit, int (*action)(void *), unsigned mode)
{
 if (!(__builtin_constant_p((bit)) ? constant_test_bit((bit), (word)) : variable_test_bit((bit), (word))))
  return 0;
 return out_of_line_wait_on_bit(word, bit, action, mode);
}
# 895 "include/linux/wait.h"
static inline __attribute__((no_instrument_function)) int
wait_on_bit_lock(void *word, int bit, int (*action)(void *), unsigned mode)
{
 if (!test_and_set_bit(bit, word))
  return 0;
 return out_of_line_wait_on_bit_lock(word, bit, action, mode);
}
# 913 "include/linux/wait.h"
static inline __attribute__((no_instrument_function))
int wait_on_atomic_t(atomic_t *val, int (*action)(atomic_t *), unsigned mode)
{
 if (atomic_read(val) == 0)
  return 0;
 return out_of_line_wait_on_atomic_t(val, action, mode);
}
# 12 "include/linux/completion.h" 2
# 25 "include/linux/completion.h"
struct completion {
 unsigned int done;
 wait_queue_head_t wait;
};
# 73 "include/linux/completion.h"
static inline __attribute__((no_instrument_function)) void init_completion(struct completion *x)
{
 x->done = 0;
 do { static struct lock_class_key __key; __init_waitqueue_head((&x->wait), "&x->wait", &__key); } while (0);
}
# 86 "include/linux/completion.h"
static inline __attribute__((no_instrument_function)) void reinit_completion(struct completion *x)
{
 x->done = 0;
}

extern void wait_for_completion(struct completion *);
extern void wait_for_completion_io(struct completion *);
extern int wait_for_completion_interruptible(struct completion *x);
extern int wait_for_completion_killable(struct completion *x);
extern unsigned long wait_for_completion_timeout(struct completion *x,
         unsigned long timeout);
extern unsigned long wait_for_completion_io_timeout(struct completion *x,
          unsigned long timeout);
extern long wait_for_completion_interruptible_timeout(
 struct completion *x, unsigned long timeout);
extern long wait_for_completion_killable_timeout(
 struct completion *x, unsigned long timeout);
extern bool try_wait_for_completion(struct completion *x);
extern bool completion_done(struct completion *x);

extern void complete(struct completion *);
extern void complete_all(struct completion *);
# 44 "include/linux/rcupdate.h" 2
# 1 "include/linux/debugobjects.h" 1






enum debug_obj_state {
 ODEBUG_STATE_NONE,
 ODEBUG_STATE_INIT,
 ODEBUG_STATE_INACTIVE,
 ODEBUG_STATE_ACTIVE,
 ODEBUG_STATE_DESTROYED,
 ODEBUG_STATE_NOTAVAILABLE,
 ODEBUG_STATE_MAX,
};

struct debug_obj_descr;
# 27 "include/linux/debugobjects.h"
struct debug_obj {
 struct hlist_node node;
 enum debug_obj_state state;
 unsigned int astate;
 void *object;
 struct debug_obj_descr *descr;
};
# 52 "include/linux/debugobjects.h"
struct debug_obj_descr {
 const char *name;
 void *(*debug_hint) (void *addr);
 int (*fixup_init) (void *addr, enum debug_obj_state state);
 int (*fixup_activate) (void *addr, enum debug_obj_state state);
 int (*fixup_destroy) (void *addr, enum debug_obj_state state);
 int (*fixup_free) (void *addr, enum debug_obj_state state);
 int (*fixup_assert_init)(void *addr, enum debug_obj_state state);
};


extern void debug_object_init (void *addr, struct debug_obj_descr *descr);
extern void
debug_object_init_on_stack(void *addr, struct debug_obj_descr *descr);
extern int debug_object_activate (void *addr, struct debug_obj_descr *descr);
extern void debug_object_deactivate(void *addr, struct debug_obj_descr *descr);
extern void debug_object_destroy (void *addr, struct debug_obj_descr *descr);
extern void debug_object_free (void *addr, struct debug_obj_descr *descr);
extern void debug_object_assert_init(void *addr, struct debug_obj_descr *descr);






extern void
debug_object_active_state(void *addr, struct debug_obj_descr *descr,
     unsigned int expect, unsigned int next);

extern void debug_objects_early_init(void);
extern void debug_objects_mem_init(void);
# 104 "include/linux/debugobjects.h"
extern void debug_check_no_obj_freed(const void *address, unsigned long size);
# 45 "include/linux/rcupdate.h" 2




extern int rcu_expedited;





void rcutorture_record_test_transition(void);
void rcutorture_record_progress(unsigned long vernum);
void do_trace_rcu_torture_read(const char *rcutorturename,
          struct callback_head *rhp,
          unsigned long secs,
          unsigned long c_old,
          unsigned long c);
# 154 "include/linux/rcupdate.h"
void call_rcu_bh(struct callback_head *head,
   void (*func)(struct callback_head *head));
# 176 "include/linux/rcupdate.h"
void call_rcu_sched(struct callback_head *head,
      void (*func)(struct callback_head *rcu));

void synchronize_sched(void);
# 198 "include/linux/rcupdate.h"
static inline __attribute__((no_instrument_function)) void __rcu_read_lock(void)
{
 do { __preempt_count_add(1); __asm__ __volatile__("": : :"memory"); } while (0);
}

static inline __attribute__((no_instrument_function)) void __rcu_read_unlock(void)
{
 do { __asm__ __volatile__("": : :"memory"); __preempt_count_sub(1); } while (0);
}

static inline __attribute__((no_instrument_function)) void synchronize_rcu(void)
{
 synchronize_sched();
}

static inline __attribute__((no_instrument_function)) int rcu_preempt_depth(void)
{
 return 0;
}




void rcu_init(void);
void rcu_sched_qs(int cpu);
void rcu_bh_qs(int cpu);
void rcu_check_callbacks(int cpu, int user);
struct notifier_block;
void rcu_idle_enter(void);
void rcu_idle_exit(void);
void rcu_irq_enter(void);
void rcu_irq_exit(void);


void rcu_user_enter(void);
void rcu_user_exit(void);
# 267 "include/linux/rcupdate.h"
bool __rcu_is_watching(void);







typedef void call_rcu_func_t(struct callback_head *head,
        void (*func)(struct callback_head *head));
void wait_rcu_gp(call_rcu_func_t crf);


# 1 "include/linux/rcutree.h" 1
# 33 "include/linux/rcutree.h"
void rcu_note_context_switch(int cpu);

int rcu_needs_cpu(int cpu, unsigned long *delta_jiffies);

void rcu_cpu_stall_reset(void);






static inline __attribute__((no_instrument_function)) void rcu_virt_note_context_switch(int cpu)
{
 rcu_note_context_switch(cpu);
}

void synchronize_rcu_bh(void);
void synchronize_sched_expedited(void);
void synchronize_rcu_expedited(void);

void kfree_call_rcu(struct callback_head *head, void (*func)(struct callback_head *rcu));
# 71 "include/linux/rcutree.h"
static inline __attribute__((no_instrument_function)) void synchronize_rcu_bh_expedited(void)
{
 synchronize_sched_expedited();
}

void rcu_barrier(void);
void rcu_barrier_bh(void);
void rcu_barrier_sched(void);
unsigned long get_state_synchronize_rcu(void);
void cond_synchronize_rcu(unsigned long oldstate);

extern unsigned long rcutorture_testseq;
extern unsigned long rcutorture_vernum;
long rcu_batches_completed(void);
long rcu_batches_completed_bh(void);
long rcu_batches_completed_sched(void);

void rcu_force_quiescent_state(void);
void rcu_bh_force_quiescent_state(void);
void rcu_sched_force_quiescent_state(void);

void exit_rcu(void);

void rcu_scheduler_starting(void);
extern int rcu_scheduler_active __attribute__((__section__(".data..read_mostly")));

bool rcu_is_watching(void);
# 281 "include/linux/rcupdate.h" 2
# 294 "include/linux/rcupdate.h"
void init_rcu_head_on_stack(struct callback_head *head);
void destroy_rcu_head_on_stack(struct callback_head *head);
# 307 "include/linux/rcupdate.h"
bool rcu_lockdep_current_cpu_online(void);
# 317 "include/linux/rcupdate.h"
static inline __attribute__((no_instrument_function)) void rcu_lock_acquire(struct lockdep_map *map)
{
 lock_acquire(map, 0, 0, 2, 0, ((void *)0), (0));
}

static inline __attribute__((no_instrument_function)) void rcu_lock_release(struct lockdep_map *map)
{
 lock_release(map, 1, 0);
}

extern struct lockdep_map rcu_lock_map;
extern struct lockdep_map rcu_bh_lock_map;
extern struct lockdep_map rcu_sched_lock_map;
extern struct lockdep_map rcu_callback_map;
extern int debug_lockdep_rcu_enabled(void);
# 353 "include/linux/rcupdate.h"
static inline __attribute__((no_instrument_function)) int rcu_read_lock_held(void)
{
 if (!debug_lockdep_rcu_enabled())
  return 1;
 if (!rcu_is_watching())
  return 0;
 if (!rcu_lockdep_current_cpu_online())
  return 0;
 return lock_is_held(&rcu_lock_map);
}





int rcu_read_lock_bh_held(void);
# 402 "include/linux/rcupdate.h"
static inline __attribute__((no_instrument_function)) int rcu_read_lock_sched_held(void)
{
 int lockdep_opinion = 0;

 if (!debug_lockdep_rcu_enabled())
  return 1;
 if (!rcu_is_watching())
  return 0;
 if (!rcu_lockdep_current_cpu_online())
  return 0;
 if (debug_locks)
  lockdep_opinion = lock_is_held(&rcu_sched_lock_map);
 return lockdep_opinion || preempt_count() != 0 || ({ unsigned long _flags; do { ({ unsigned long __dummy; typeof(_flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); _flags = arch_local_save_flags(); } while (0); ({ ({ unsigned long __dummy; typeof(_flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); arch_irqs_disabled_flags(_flags); }); });
}
# 469 "include/linux/rcupdate.h"
static inline __attribute__((no_instrument_function)) void rcu_preempt_sleep_check(void)
{
 do { static bool __attribute__ ((__section__(".data.unlikely"))) __warned; if (debug_lockdep_rcu_enabled() && !__warned && !(!lock_is_held(&rcu_lock_map))) { __warned = true; lockdep_rcu_suspicious(
 "include/linux/rcupdate.h"
# 471 "include/linux/rcupdate.h"
 ,
 472
# 471 "include/linux/rcupdate.h"
 , "Illegal context switch in RCU read-side critical section"); } } while (0)
                                                                 ;
}
# 798 "include/linux/rcupdate.h"
static inline __attribute__((no_instrument_function)) void rcu_read_lock(void)
{
 __rcu_read_lock();
 (void)0;
 rcu_lock_acquire(&rcu_lock_map);
 do { static bool __attribute__ ((__section__(".data.unlikely"))) __warned; if (debug_lockdep_rcu_enabled() && !__warned && !(rcu_is_watching())) { __warned = true; lockdep_rcu_suspicious(
 "include/linux/rcupdate.h"
# 803 "include/linux/rcupdate.h"
 ,
 804
# 803 "include/linux/rcupdate.h"
 , "rcu_read_lock() used illegally while idle"); } } while (0)
                                                  ;
}
# 822 "include/linux/rcupdate.h"
static inline __attribute__((no_instrument_function)) void rcu_read_unlock(void)
{
 do { static bool __attribute__ ((__section__(".data.unlikely"))) __warned; if (debug_lockdep_rcu_enabled() && !__warned && !(rcu_is_watching())) { __warned = true; lockdep_rcu_suspicious(
 "include/linux/rcupdate.h"
# 824 "include/linux/rcupdate.h"
 ,
 825
# 824 "include/linux/rcupdate.h"
 , "rcu_read_unlock() used illegally while idle"); } } while (0)
                                                    ;
 rcu_lock_release(&rcu_lock_map);
 (void)0;
 __rcu_read_unlock();
}
# 848 "include/linux/rcupdate.h"
static inline __attribute__((no_instrument_function)) void rcu_read_lock_bh(void)
{
 local_bh_disable();
 (void)0;
 rcu_lock_acquire(&rcu_bh_lock_map);
 do { static bool __attribute__ ((__section__(".data.unlikely"))) __warned; if (debug_lockdep_rcu_enabled() && !__warned && !(rcu_is_watching())) { __warned = true; lockdep_rcu_suspicious(
 "include/linux/rcupdate.h"
# 853 "include/linux/rcupdate.h"
 ,
 854
# 853 "include/linux/rcupdate.h"
 , "rcu_read_lock_bh() used illegally while idle"); } } while (0)
                                                     ;
}






static inline __attribute__((no_instrument_function)) void rcu_read_unlock_bh(void)
{
 do { static bool __attribute__ ((__section__(".data.unlikely"))) __warned; if (debug_lockdep_rcu_enabled() && !__warned && !(rcu_is_watching())) { __warned = true; lockdep_rcu_suspicious(
 "include/linux/rcupdate.h"
# 864 "include/linux/rcupdate.h"
 ,
 865
# 864 "include/linux/rcupdate.h"
 , "rcu_read_unlock_bh() used illegally while idle"); } } while (0)
                                                       ;
 rcu_lock_release(&rcu_bh_lock_map);
 (void)0;
 local_bh_enable();
}
# 884 "include/linux/rcupdate.h"
static inline __attribute__((no_instrument_function)) void rcu_read_lock_sched(void)
{
 do { __preempt_count_add(1); __asm__ __volatile__("": : :"memory"); } while (0);
 (void)0;
 rcu_lock_acquire(&rcu_sched_lock_map);
 do { static bool __attribute__ ((__section__(".data.unlikely"))) __warned; if (debug_lockdep_rcu_enabled() && !__warned && !(rcu_is_watching())) { __warned = true; lockdep_rcu_suspicious(
 "include/linux/rcupdate.h"
# 889 "include/linux/rcupdate.h"
 ,
 890
# 889 "include/linux/rcupdate.h"
 , "rcu_read_lock_sched() used illegally while idle"); } } while (0)
                                                        ;
}


static inline __attribute__((no_instrument_function)) __attribute__((no_instrument_function)) void rcu_read_lock_sched_notrace(void)
{
 do { __preempt_count_add(1); __asm__ __volatile__("": : :"memory"); } while (0);
 (void)0;
}






static inline __attribute__((no_instrument_function)) void rcu_read_unlock_sched(void)
{
 do { static bool __attribute__ ((__section__(".data.unlikely"))) __warned; if (debug_lockdep_rcu_enabled() && !__warned && !(rcu_is_watching())) { __warned = true; lockdep_rcu_suspicious(
 "include/linux/rcupdate.h"
# 907 "include/linux/rcupdate.h"
 ,
 908
# 907 "include/linux/rcupdate.h"
 , "rcu_read_unlock_sched() used illegally while idle"); } } while (0)
                                                          ;
 rcu_lock_release(&rcu_sched_lock_map);
 (void)0;
 do { __asm__ __volatile__("": : :"memory"); __preempt_count_sub(1); } while (0);
}


static inline __attribute__((no_instrument_function)) __attribute__((no_instrument_function)) void rcu_read_unlock_sched_notrace(void)
{
 (void)0;
 do { __asm__ __volatile__("": : :"memory"); __preempt_count_sub(1); } while (0);
}
# 1021 "include/linux/rcupdate.h"
bool rcu_is_nocb_cpu(int cpu);
# 1033 "include/linux/rcupdate.h"
static inline __attribute__((no_instrument_function)) bool rcu_sys_is_idle(void)
{
 return false;
}

static inline __attribute__((no_instrument_function)) void rcu_sysidle_force_exit(void)
{
}
# 19 "include/linux/idr.h" 2
# 30 "include/linux/idr.h"
struct idr_layer {
 int prefix;
 unsigned long bitmap[((((1 << 8)) + (8 * sizeof(long)) - 1) / (8 * sizeof(long)))];
 struct idr_layer *ary[1<<8];
 int count;
 int layer;
 struct callback_head callback_head;
};

struct idr {
 struct idr_layer *hint;
 struct idr_layer *top;
 struct idr_layer *id_free;
 int layers;
 int id_free_cnt;
 int cur;
 spinlock_t lock;
};
# 76 "include/linux/idr.h"
void *idr_find_slowpath(struct idr *idp, int id);
void idr_preload(gfp_t gfp_mask);
int idr_alloc(struct idr *idp, void *ptr, int start, int end, gfp_t gfp_mask);
int idr_alloc_cyclic(struct idr *idr, void *ptr, int start, int end, gfp_t gfp_mask);
int idr_for_each(struct idr *idp,
   int (*fn)(int id, void *p, void *data), void *data);
void *idr_get_next(struct idr *idp, int *nextid);
void *idr_replace(struct idr *idp, void *ptr, int id);
void idr_remove(struct idr *idp, int id);
void idr_destroy(struct idr *idp);
void idr_init(struct idr *idp);
bool idr_is_empty(struct idr *idp);







static inline __attribute__((no_instrument_function)) void idr_preload_end(void)
{
 do { __asm__ __volatile__("": : :"memory"); __preempt_count_sub(1); } while (0);
}
# 112 "include/linux/idr.h"
static inline __attribute__((no_instrument_function)) void *idr_find(struct idr *idr, int id)
{
 struct idr_layer *hint = ({ typeof(*(idr->hint)) *_________p1 = (typeof(*(idr->hint)) *)(*(volatile typeof((idr->hint)) *)&((idr->hint))); do { static bool __attribute__ ((__section__(".data.unlikely"))) __warned; if (debug_lockdep_rcu_enabled() && !__warned && !(rcu_read_lock_held() || (1))) { __warned = true; lockdep_rcu_suspicious("include/linux/idr.h", 114, "suspicious rcu_dereference_check() usage"); } } while (0); ; do { } while (0); ((typeof(*(idr->hint)) *)(_________p1)); });

 if (hint && (id & ~((1 << 8)-1)) == hint->prefix)
  return ({ typeof(*(hint->ary[id & ((1 << 8)-1)])) *_________p1 = (typeof(*(hint->ary[id & ((1 << 8)-1)])) *)(*(volatile typeof((hint->ary[id & ((1 << 8)-1)])) *)&((hint->ary[id & ((1 << 8)-1)]))); do { static bool __attribute__ ((__section__(".data.unlikely"))) __warned; if (debug_lockdep_rcu_enabled() && !__warned && !(rcu_read_lock_held() || (1))) { __warned = true; lockdep_rcu_suspicious("include/linux/idr.h", 117, "suspicious rcu_dereference_check() usage"); } } while (0); ; do { } while (0); ((typeof(*(hint->ary[id & ((1 << 8)-1)])) *)(_________p1)); });

 return idr_find_slowpath(idr, id);
}
# 146 "include/linux/idr.h"
struct ida_bitmap {
 long nr_busy;
 unsigned long bitmap[(128 / sizeof(long) - 1)];
};

struct ida {
 struct idr idr;
 struct ida_bitmap *free_bitmap;
};




int ida_pre_get(struct ida *ida, gfp_t gfp_mask);
int ida_get_new_above(struct ida *ida, int starting_id, int *p_id);
void ida_remove(struct ida *ida, int id);
void ida_destroy(struct ida *ida);
void ida_init(struct ida *ida);

int ida_simple_get(struct ida *ida, unsigned int start, unsigned int end,
     gfp_t gfp_mask);
void ida_simple_remove(struct ida *ida, unsigned int id);
# 176 "include/linux/idr.h"
static inline __attribute__((no_instrument_function)) int ida_get_new(struct ida *ida, int *p_id)
{
 return ida_get_new_above(ida, 0, p_id);
}

void __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) idr_init_cache(void);
# 15 "include/linux/kernfs.h" 2

# 1 "include/linux/rbtree.h" 1
# 35 "include/linux/rbtree.h"
struct rb_node {
 unsigned long __rb_parent_color;
 struct rb_node *rb_right;
 struct rb_node *rb_left;
} __attribute__((aligned(sizeof(long))));


struct rb_root {
 struct rb_node *rb_node;
};
# 61 "include/linux/rbtree.h"
extern void rb_insert_color(struct rb_node *, struct rb_root *);
extern void rb_erase(struct rb_node *, struct rb_root *);



extern struct rb_node *rb_next(const struct rb_node *);
extern struct rb_node *rb_prev(const struct rb_node *);
extern struct rb_node *rb_first(const struct rb_root *);
extern struct rb_node *rb_last(const struct rb_root *);


extern struct rb_node *rb_first_postorder(const struct rb_root *);
extern struct rb_node *rb_next_postorder(const struct rb_node *);


extern void rb_replace_node(struct rb_node *victim, struct rb_node *new,
       struct rb_root *root);

static inline __attribute__((no_instrument_function)) void rb_link_node(struct rb_node * node, struct rb_node * parent,
    struct rb_node ** rb_link)
{
 node->__rb_parent_color = (unsigned long)parent;
 node->rb_left = node->rb_right = ((void *)0);

 *rb_link = node;
}
# 17 "include/linux/kernfs.h" 2



struct file;
struct dentry;
struct iattr;
struct seq_file;
struct vm_area_struct;
struct super_block;
struct file_system_type;

struct kernfs_open_node;
struct kernfs_iattrs;

enum kernfs_node_type {
 KERNFS_DIR = 0x0001,
 KERNFS_FILE = 0x0002,
 KERNFS_LINK = 0x0004,
};




enum kernfs_node_flag {
 KERNFS_ACTIVATED = 0x0010,
 KERNFS_NS = 0x0020,
 KERNFS_HAS_SEQ_SHOW = 0x0040,
 KERNFS_HAS_MMAP = 0x0080,
 KERNFS_LOCKDEP = 0x0100,
 KERNFS_STATIC_NAME = 0x0200,
 KERNFS_SUICIDAL = 0x0400,
 KERNFS_SUICIDED = 0x0800,
};


enum kernfs_root_flag {






 KERNFS_ROOT_CREATE_DEACTIVATED = 0x0001,
# 70 "include/linux/kernfs.h"
 KERNFS_ROOT_EXTRA_OPEN_PERM_CHECK = 0x0002,
};


struct kernfs_elem_dir {
 unsigned long subdirs;

 struct rb_root children;





 struct kernfs_root *root;
};

struct kernfs_elem_symlink {
 struct kernfs_node *target_kn;
};

struct kernfs_elem_attr {
 const struct kernfs_ops *ops;
 struct kernfs_open_node *open;
 loff_t size;
};
# 105 "include/linux/kernfs.h"
struct kernfs_node {
 atomic_t count;
 atomic_t active;

 struct lockdep_map dep_map;







 struct kernfs_node *parent;
 const char *name;

 struct rb_node rb;

 const void *ns;
 unsigned int hash;
 union {
  struct kernfs_elem_dir dir;
  struct kernfs_elem_symlink symlink;
  struct kernfs_elem_attr attr;
 };

 void *priv;

 unsigned short flags;
 umode_t mode;
 unsigned int ino;
 struct kernfs_iattrs *iattr;
};
# 145 "include/linux/kernfs.h"
struct kernfs_syscall_ops {
 int (*remount_fs)(struct kernfs_root *root, int *flags, char *data);
 int (*show_options)(struct seq_file *sf, struct kernfs_root *root);

 int (*mkdir)(struct kernfs_node *parent, const char *name,
       umode_t mode);
 int (*rmdir)(struct kernfs_node *kn);
 int (*rename)(struct kernfs_node *kn, struct kernfs_node *new_parent,
        const char *new_name);
};

struct kernfs_root {

 struct kernfs_node *kn;
 unsigned int flags;


 struct ida ino_ida;
 struct kernfs_syscall_ops *syscall_ops;


 struct list_head supers;

 wait_queue_head_t deactivate_waitq;
};

struct kernfs_open_file {

 struct kernfs_node *kn;
 struct file *file;
 void *priv;


 struct mutex mutex;
 int event;
 struct list_head list;

 size_t atomic_write_len;
 bool mmapped;
 const struct vm_operations_struct *vm_ops;
};

struct kernfs_ops {
# 199 "include/linux/kernfs.h"
 int (*seq_show)(struct seq_file *sf, void *v);

 void *(*seq_start)(struct seq_file *sf, loff_t *ppos);
 void *(*seq_next)(struct seq_file *sf, void *v, loff_t *ppos);
 void (*seq_stop)(struct seq_file *sf, void *v);

 ssize_t (*read)(struct kernfs_open_file *of, char *buf, size_t bytes,
   loff_t off);
# 215 "include/linux/kernfs.h"
 size_t atomic_write_len;
 ssize_t (*write)(struct kernfs_open_file *of, char *buf, size_t bytes,
    loff_t off);

 int (*mmap)(struct kernfs_open_file *of, struct vm_area_struct *vma);


 struct lock_class_key lockdep_key;

};



static inline __attribute__((no_instrument_function)) enum kernfs_node_type kernfs_type(struct kernfs_node *kn)
{
 return kn->flags & 0x000f;
}
# 241 "include/linux/kernfs.h"
static inline __attribute__((no_instrument_function)) void kernfs_enable_ns(struct kernfs_node *kn)
{
 ({ static bool __attribute__ ((__section__(".data.unlikely"))) __warned; int __ret_warn_once = !!(kernfs_type(kn) != KERNFS_DIR); if (__builtin_expect(!!(__ret_warn_once), 0)) if (({ int __ret_warn_on = !!(!__warned); if (__builtin_expect(!!(__ret_warn_on), 0)) warn_slowpath_null("include/linux/kernfs.h", 243); __builtin_expect(!!(__ret_warn_on), 0); })) __warned = true; __builtin_expect(!!(__ret_warn_once), 0); });
 ({ static bool __attribute__ ((__section__(".data.unlikely"))) __warned; int __ret_warn_once = !!(!((&kn->dir.children)->rb_node == ((void *)0))); if (__builtin_expect(!!(__ret_warn_once), 0)) if (({ int __ret_warn_on = !!(!__warned); if (__builtin_expect(!!(__ret_warn_on), 0)) warn_slowpath_null("include/linux/kernfs.h", 244); __builtin_expect(!!(__ret_warn_on), 0); })) __warned = true; __builtin_expect(!!(__ret_warn_once), 0); });
 kn->flags |= KERNFS_NS;
}







static inline __attribute__((no_instrument_function)) bool kernfs_ns_enabled(struct kernfs_node *kn)
{
 return kn->flags & KERNFS_NS;
}

int kernfs_name(struct kernfs_node *kn, char *buf, size_t buflen);
char * __attribute__((warn_unused_result)) kernfs_path(struct kernfs_node *kn, char *buf,
    size_t buflen);
void pr_cont_kernfs_name(struct kernfs_node *kn);
void pr_cont_kernfs_path(struct kernfs_node *kn);
struct kernfs_node *kernfs_get_parent(struct kernfs_node *kn);
struct kernfs_node *kernfs_find_and_get_ns(struct kernfs_node *parent,
        const char *name, const void *ns);
void kernfs_get(struct kernfs_node *kn);
void kernfs_put(struct kernfs_node *kn);

struct kernfs_node *kernfs_node_from_dentry(struct dentry *dentry);
struct kernfs_root *kernfs_root_from_sb(struct super_block *sb);

struct kernfs_root *kernfs_create_root(struct kernfs_syscall_ops *scops,
           unsigned int flags, void *priv);
void kernfs_destroy_root(struct kernfs_root *root);

struct kernfs_node *kernfs_create_dir_ns(struct kernfs_node *parent,
      const char *name, umode_t mode,
      void *priv, const void *ns);
struct kernfs_node *__kernfs_create_file(struct kernfs_node *parent,
      const char *name,
      umode_t mode, loff_t size,
      const struct kernfs_ops *ops,
      void *priv, const void *ns,
      bool name_is_static,
      struct lock_class_key *key);
struct kernfs_node *kernfs_create_link(struct kernfs_node *parent,
           const char *name,
           struct kernfs_node *target);
void kernfs_activate(struct kernfs_node *kn);
void kernfs_remove(struct kernfs_node *kn);
void kernfs_break_active_protection(struct kernfs_node *kn);
void kernfs_unbreak_active_protection(struct kernfs_node *kn);
bool kernfs_remove_self(struct kernfs_node *kn);
int kernfs_remove_by_name_ns(struct kernfs_node *parent, const char *name,
        const void *ns);
int kernfs_rename_ns(struct kernfs_node *kn, struct kernfs_node *new_parent,
       const char *new_name, const void *new_ns);
int kernfs_setattr(struct kernfs_node *kn, const struct iattr *iattr);
void kernfs_notify(struct kernfs_node *kn);

const void *kernfs_super_ns(struct super_block *sb);
struct dentry *kernfs_mount_ns(struct file_system_type *fs_type, int flags,
          struct kernfs_root *root, unsigned long magic,
          bool *new_sb_created, const void *ns);
void kernfs_kill_sb(struct super_block *sb);
struct super_block *kernfs_pin_sb(struct kernfs_root *root, const void *ns);

void kernfs_init(void);
# 409 "include/linux/kernfs.h"
static inline __attribute__((no_instrument_function)) struct kernfs_node *
kernfs_find_and_get(struct kernfs_node *kn, const char *name)
{
 return kernfs_find_and_get_ns(kn, name, ((void *)0));
}

static inline __attribute__((no_instrument_function)) struct kernfs_node *
kernfs_create_dir(struct kernfs_node *parent, const char *name, umode_t mode,
    void *priv)
{
 return kernfs_create_dir_ns(parent, name, mode, priv, ((void *)0));
}

static inline __attribute__((no_instrument_function)) struct kernfs_node *
kernfs_create_file_ns(struct kernfs_node *parent, const char *name,
        umode_t mode, loff_t size, const struct kernfs_ops *ops,
        void *priv, const void *ns)
{
 struct lock_class_key *key = ((void *)0);


 key = (struct lock_class_key *)&ops->lockdep_key;

 return __kernfs_create_file(parent, name, mode, size, ops, priv, ns,
        false, key);
}

static inline __attribute__((no_instrument_function)) struct kernfs_node *
kernfs_create_file(struct kernfs_node *parent, const char *name, umode_t mode,
     loff_t size, const struct kernfs_ops *ops, void *priv)
{
 return kernfs_create_file_ns(parent, name, mode, size, ops, priv, ((void *)0));
}

static inline __attribute__((no_instrument_function)) int kernfs_remove_by_name(struct kernfs_node *parent,
     const char *name)
{
 return kernfs_remove_by_name_ns(parent, name, ((void *)0));
}

static inline __attribute__((no_instrument_function)) int kernfs_rename(struct kernfs_node *kn,
    struct kernfs_node *new_parent,
    const char *new_name)
{
 return kernfs_rename_ns(kn, new_parent, new_name, ((void *)0));
}

static inline __attribute__((no_instrument_function)) struct dentry *
kernfs_mount(struct file_system_type *fs_type, int flags,
  struct kernfs_root *root, unsigned long magic,
  bool *new_sb_created)
{
 return kernfs_mount_ns(fs_type, flags, root,
    magic, new_sb_created, ((void *)0));
}
# 16 "include/linux/sysfs.h" 2

# 1 "include/linux/errno.h" 1



# 1 "include/uapi/linux/errno.h" 1
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/errno.h" 1
# 1 "include/uapi/linux/errno.h" 2
# 5 "include/linux/errno.h" 2
# 18 "include/linux/sysfs.h" 2


# 1 "include/linux/kobject_ns.h" 1
# 20 "include/linux/kobject_ns.h"
struct sock;
struct kobject;





enum kobj_ns_type {
 KOBJ_NS_TYPE_NONE = 0,
 KOBJ_NS_TYPE_NET,
 KOBJ_NS_TYPES
};
# 40 "include/linux/kobject_ns.h"
struct kobj_ns_type_operations {
 enum kobj_ns_type type;
 bool (*current_may_mount)(void);
 void *(*grab_current_ns)(void);
 const void *(*netlink_ns)(struct sock *sk);
 const void *(*initial_ns)(void);
 void (*drop_ns)(void *);
};

int kobj_ns_type_register(const struct kobj_ns_type_operations *ops);
int kobj_ns_type_registered(enum kobj_ns_type type);
const struct kobj_ns_type_operations *kobj_child_ns_ops(struct kobject *parent);
const struct kobj_ns_type_operations *kobj_ns_ops(struct kobject *kobj);

bool kobj_ns_current_may_mount(enum kobj_ns_type type);
void *kobj_ns_grab_current(enum kobj_ns_type type);
const void *kobj_ns_netlink(enum kobj_ns_type type, struct sock *sk);
const void *kobj_ns_initial(enum kobj_ns_type type);
void kobj_ns_drop(enum kobj_ns_type type, void *ns);
# 21 "include/linux/sysfs.h" 2
# 1 "include/linux/stat.h" 1




# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/stat.h" 1



# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/posix_types.h" 1
# 5 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/stat.h" 2
# 82 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/stat.h"
struct stat {
 __kernel_ulong_t st_dev;
 __kernel_ulong_t st_ino;
 __kernel_ulong_t st_nlink;

 unsigned int st_mode;
 unsigned int st_uid;
 unsigned int st_gid;
 unsigned int __pad0;
 __kernel_ulong_t st_rdev;
 __kernel_long_t st_size;
 __kernel_long_t st_blksize;
 __kernel_long_t st_blocks;

 __kernel_ulong_t st_atime;
 __kernel_ulong_t st_atime_nsec;
 __kernel_ulong_t st_mtime;
 __kernel_ulong_t st_mtime_nsec;
 __kernel_ulong_t st_ctime;
 __kernel_ulong_t st_ctime_nsec;
 __kernel_long_t __unused[3];
};
# 116 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/stat.h"
struct __old_kernel_stat {
 unsigned short st_dev;
 unsigned short st_ino;
 unsigned short st_mode;
 unsigned short st_nlink;
 unsigned short st_uid;
 unsigned short st_gid;
 unsigned short st_rdev;






 unsigned int st_size;
 unsigned int st_atime;
 unsigned int st_mtime;
 unsigned int st_ctime;

};
# 6 "include/linux/stat.h" 2
# 1 "include/uapi/linux/stat.h" 1
# 7 "include/linux/stat.h" 2
# 18 "include/linux/stat.h"
# 1 "include/linux/time.h" 1






# 1 "include/uapi/linux/time.h" 1
# 9 "include/uapi/linux/time.h"
struct timespec {
 __kernel_time_t tv_sec;
 long tv_nsec;
};


struct timeval {
 __kernel_time_t tv_sec;
 __kernel_suseconds_t tv_usec;
};

struct timezone {
 int tz_minuteswest;
 int tz_dsttime;
};
# 34 "include/uapi/linux/time.h"
struct itimerspec {
 struct timespec it_interval;
 struct timespec it_value;
};

struct itimerval {
 struct timeval it_interval;
 struct timeval it_value;
};
# 8 "include/linux/time.h" 2

extern struct timezone sys_tz;
# 22 "include/linux/time.h"
static inline __attribute__((no_instrument_function)) int timespec_equal(const struct timespec *a,
                                 const struct timespec *b)
{
 return (a->tv_sec == b->tv_sec) && (a->tv_nsec == b->tv_nsec);
}






static inline __attribute__((no_instrument_function)) int timespec_compare(const struct timespec *lhs, const struct timespec *rhs)
{
 if (lhs->tv_sec < rhs->tv_sec)
  return -1;
 if (lhs->tv_sec > rhs->tv_sec)
  return 1;
 return lhs->tv_nsec - rhs->tv_nsec;
}

static inline __attribute__((no_instrument_function)) int timeval_compare(const struct timeval *lhs, const struct timeval *rhs)
{
 if (lhs->tv_sec < rhs->tv_sec)
  return -1;
 if (lhs->tv_sec > rhs->tv_sec)
  return 1;
 return lhs->tv_usec - rhs->tv_usec;
}

extern unsigned long mktime(const unsigned int year, const unsigned int mon,
       const unsigned int day, const unsigned int hour,
       const unsigned int min, const unsigned int sec);

extern void set_normalized_timespec(struct timespec *ts, time_t sec, s64 nsec);






extern struct timespec timespec_add_safe(const struct timespec lhs,
      const struct timespec rhs);


static inline __attribute__((no_instrument_function)) struct timespec timespec_add(struct timespec lhs,
      struct timespec rhs)
{
 struct timespec ts_delta;
 set_normalized_timespec(&ts_delta, lhs.tv_sec + rhs.tv_sec,
    lhs.tv_nsec + rhs.tv_nsec);
 return ts_delta;
}




static inline __attribute__((no_instrument_function)) struct timespec timespec_sub(struct timespec lhs,
      struct timespec rhs)
{
 struct timespec ts_delta;
 set_normalized_timespec(&ts_delta, lhs.tv_sec - rhs.tv_sec,
    lhs.tv_nsec - rhs.tv_nsec);
 return ts_delta;
}
# 97 "include/linux/time.h"
static inline __attribute__((no_instrument_function)) bool timespec_valid(const struct timespec *ts)
{

 if (ts->tv_sec < 0)
  return false;

 if ((unsigned long)ts->tv_nsec >= 1000000000L)
  return false;
 return true;
}

static inline __attribute__((no_instrument_function)) bool timespec_valid_strict(const struct timespec *ts)
{
 if (!timespec_valid(ts))
  return false;

 if ((unsigned long long)ts->tv_sec >= (((s64)~((u64)1 << 63)) / 1000000000L))
  return false;
 return true;
}

extern bool persistent_clock_exist;

static inline __attribute__((no_instrument_function)) bool has_persistent_clock(void)
{
 return persistent_clock_exist;
}

extern void read_persistent_clock(struct timespec *ts);
extern void read_boot_clock(struct timespec *ts);
extern int persistent_clock_is_local;
extern int update_persistent_clock(struct timespec now);
void timekeeping_init(void);
extern int timekeeping_suspended;

unsigned long get_seconds(void);
struct timespec current_kernel_time(void);
struct timespec __current_kernel_time(void);
struct timespec get_monotonic_coarse(void);
void get_xtime_and_monotonic_and_sleep_offset(struct timespec *xtim,
    struct timespec *wtom, struct timespec *sleep);
void timekeeping_inject_sleeptime(struct timespec *delta);
# 156 "include/linux/time.h"
extern void do_gettimeofday(struct timeval *tv);
extern int do_settimeofday(const struct timespec *tv);
extern int do_sys_settimeofday(const struct timespec *tv,
          const struct timezone *tz);

extern long do_utimes(int dfd, const char *filename, struct timespec *times, int flags);
struct itimerval;
extern int do_setitimer(int which, struct itimerval *value,
   struct itimerval *ovalue);
extern unsigned int alarm_setitimer(unsigned int seconds);
extern int do_getitimer(int which, struct itimerval *value);
extern int __getnstimeofday(struct timespec *tv);
extern void getnstimeofday(struct timespec *tv);
extern void getrawmonotonic(struct timespec *ts);
extern void getnstime_raw_and_real(struct timespec *ts_raw,
  struct timespec *ts_real);
extern void getboottime(struct timespec *ts);
extern void monotonic_to_bootbased(struct timespec *ts);
extern void get_monotonic_boottime(struct timespec *ts);

extern struct timespec timespec_trunc(struct timespec t, unsigned gran);
extern int timekeeping_valid_for_hres(void);
extern u64 timekeeping_max_deferment(void);
extern int timekeeping_inject_offset(struct timespec *ts);
extern s32 timekeeping_get_tai_offset(void);
extern void timekeeping_set_tai_offset(s32 tai_offset);
extern void timekeeping_clocktai(struct timespec *ts);

struct tms;
extern void do_sys_times(struct tms *);





struct tm {




 int tm_sec;

 int tm_min;

 int tm_hour;

 int tm_mday;

 int tm_mon;

 long tm_year;

 int tm_wday;

 int tm_yday;
};

void time_to_tm(time_t totalsecs, int offset, struct tm *result);
# 222 "include/linux/time.h"
static inline __attribute__((no_instrument_function)) s64 timespec_to_ns(const struct timespec *ts)
{
 return ((s64) ts->tv_sec * 1000000000L) + ts->tv_nsec;
}
# 234 "include/linux/time.h"
static inline __attribute__((no_instrument_function)) s64 timeval_to_ns(const struct timeval *tv)
{
 return ((s64) tv->tv_sec * 1000000000L) +
  tv->tv_usec * 1000L;
}







extern struct timespec ns_to_timespec(const s64 nsec);







extern struct timeval ns_to_timeval(const s64 nsec);
# 264 "include/linux/time.h"
static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) void timespec_add_ns(struct timespec *a, u64 ns)
{
 a->tv_sec += __iter_div_u64_rem(a->tv_nsec + ns, 1000000000L, &ns);
 a->tv_nsec = ns;
}
# 19 "include/linux/stat.h" 2
# 1 "include/linux/uidgid.h" 1
# 15 "include/linux/uidgid.h"
# 1 "include/linux/highuid.h" 1
# 34 "include/linux/highuid.h"
extern int overflowuid;
extern int overflowgid;

extern void __bad_uid(void);
extern void __bad_gid(void);
# 81 "include/linux/highuid.h"
extern int fs_overflowuid;
extern int fs_overflowgid;
# 16 "include/linux/uidgid.h" 2

struct user_namespace;
extern struct user_namespace init_user_ns;

typedef struct {
 uid_t val;
} kuid_t;


typedef struct {
 gid_t val;
} kgid_t;




static inline __attribute__((no_instrument_function)) uid_t __kuid_val(kuid_t uid)
{
 return uid.val;
}

static inline __attribute__((no_instrument_function)) gid_t __kgid_val(kgid_t gid)
{
 return gid.val;
}







static inline __attribute__((no_instrument_function)) bool uid_eq(kuid_t left, kuid_t right)
{
 return __kuid_val(left) == __kuid_val(right);
}

static inline __attribute__((no_instrument_function)) bool gid_eq(kgid_t left, kgid_t right)
{
 return __kgid_val(left) == __kgid_val(right);
}

static inline __attribute__((no_instrument_function)) bool uid_gt(kuid_t left, kuid_t right)
{
 return __kuid_val(left) > __kuid_val(right);
}

static inline __attribute__((no_instrument_function)) bool gid_gt(kgid_t left, kgid_t right)
{
 return __kgid_val(left) > __kgid_val(right);
}

static inline __attribute__((no_instrument_function)) bool uid_gte(kuid_t left, kuid_t right)
{
 return __kuid_val(left) >= __kuid_val(right);
}

static inline __attribute__((no_instrument_function)) bool gid_gte(kgid_t left, kgid_t right)
{
 return __kgid_val(left) >= __kgid_val(right);
}

static inline __attribute__((no_instrument_function)) bool uid_lt(kuid_t left, kuid_t right)
{
 return __kuid_val(left) < __kuid_val(right);
}

static inline __attribute__((no_instrument_function)) bool gid_lt(kgid_t left, kgid_t right)
{
 return __kgid_val(left) < __kgid_val(right);
}

static inline __attribute__((no_instrument_function)) bool uid_lte(kuid_t left, kuid_t right)
{
 return __kuid_val(left) <= __kuid_val(right);
}

static inline __attribute__((no_instrument_function)) bool gid_lte(kgid_t left, kgid_t right)
{
 return __kgid_val(left) <= __kgid_val(right);
}

static inline __attribute__((no_instrument_function)) bool uid_valid(kuid_t uid)
{
 return !uid_eq(uid, (kuid_t){ -1 });
}

static inline __attribute__((no_instrument_function)) bool gid_valid(kgid_t gid)
{
 return !gid_eq(gid, (kgid_t){ -1 });
}



extern kuid_t make_kuid(struct user_namespace *from, uid_t uid);
extern kgid_t make_kgid(struct user_namespace *from, gid_t gid);

extern uid_t from_kuid(struct user_namespace *to, kuid_t uid);
extern gid_t from_kgid(struct user_namespace *to, kgid_t gid);
extern uid_t from_kuid_munged(struct user_namespace *to, kuid_t uid);
extern gid_t from_kgid_munged(struct user_namespace *to, kgid_t gid);

static inline __attribute__((no_instrument_function)) bool kuid_has_mapping(struct user_namespace *ns, kuid_t uid)
{
 return from_kuid(ns, uid) != (uid_t) -1;
}

static inline __attribute__((no_instrument_function)) bool kgid_has_mapping(struct user_namespace *ns, kgid_t gid)
{
 return from_kgid(ns, gid) != (gid_t) -1;
}
# 20 "include/linux/stat.h" 2

struct kstat {
 u64 ino;
 dev_t dev;
 umode_t mode;
 unsigned int nlink;
 kuid_t uid;
 kgid_t gid;
 dev_t rdev;
 loff_t size;
 struct timespec atime;
 struct timespec mtime;
 struct timespec ctime;
 unsigned long blksize;
 unsigned long long blocks;
};
# 22 "include/linux/sysfs.h" 2


struct kobject;
struct module;
struct bin_attribute;
enum kobj_ns_type;

struct attribute {
 const char *name;
 umode_t mode;

 bool ignore_lockdep:1;
 struct lock_class_key *key;
 struct lock_class_key skey;

};
# 60 "include/linux/sysfs.h"
struct attribute_group {
 const char *name;
 umode_t (*is_visible)(struct kobject *,
           struct attribute *, int);
 struct attribute **attrs;
 struct bin_attribute **bin_attrs;
};
# 118 "include/linux/sysfs.h"
struct file;
struct vm_area_struct;

struct bin_attribute {
 struct attribute attr;
 size_t size;
 void *private;
 ssize_t (*read)(struct file *, struct kobject *, struct bin_attribute *,
   char *, loff_t, size_t);
 ssize_t (*write)(struct file *, struct kobject *, struct bin_attribute *,
    char *, loff_t, size_t);
 int (*mmap)(struct file *, struct kobject *, struct bin_attribute *attr,
      struct vm_area_struct *vma);
};
# 175 "include/linux/sysfs.h"
struct sysfs_ops {
 ssize_t (*show)(struct kobject *, struct attribute *, char *);
 ssize_t (*store)(struct kobject *, struct attribute *, const char *, size_t);
};



int __attribute__((warn_unused_result)) sysfs_create_dir_ns(struct kobject *kobj, const void *ns);
void sysfs_remove_dir(struct kobject *kobj);
int __attribute__((warn_unused_result)) sysfs_rename_dir_ns(struct kobject *kobj, const char *new_name,
         const void *new_ns);
int __attribute__((warn_unused_result)) sysfs_move_dir_ns(struct kobject *kobj,
       struct kobject *new_parent_kobj,
       const void *new_ns);

int __attribute__((warn_unused_result)) sysfs_create_file_ns(struct kobject *kobj,
          const struct attribute *attr,
          const void *ns);
int __attribute__((warn_unused_result)) sysfs_create_files(struct kobject *kobj,
       const struct attribute **attr);
int __attribute__((warn_unused_result)) sysfs_chmod_file(struct kobject *kobj,
      const struct attribute *attr, umode_t mode);
void sysfs_remove_file_ns(struct kobject *kobj, const struct attribute *attr,
     const void *ns);
bool sysfs_remove_file_self(struct kobject *kobj, const struct attribute *attr);
void sysfs_remove_files(struct kobject *kobj, const struct attribute **attr);

int __attribute__((warn_unused_result)) sysfs_create_bin_file(struct kobject *kobj,
           const struct bin_attribute *attr);
void sysfs_remove_bin_file(struct kobject *kobj,
      const struct bin_attribute *attr);

int __attribute__((warn_unused_result)) sysfs_create_link(struct kobject *kobj, struct kobject *target,
       const char *name);
int __attribute__((warn_unused_result)) sysfs_create_link_nowarn(struct kobject *kobj,
       struct kobject *target,
       const char *name);
void sysfs_remove_link(struct kobject *kobj, const char *name);

int sysfs_rename_link_ns(struct kobject *kobj, struct kobject *target,
    const char *old_name, const char *new_name,
    const void *new_ns);

void sysfs_delete_link(struct kobject *dir, struct kobject *targ,
   const char *name);

int __attribute__((warn_unused_result)) sysfs_create_group(struct kobject *kobj,
        const struct attribute_group *grp);
int __attribute__((warn_unused_result)) sysfs_create_groups(struct kobject *kobj,
         const struct attribute_group **groups);
int sysfs_update_group(struct kobject *kobj,
         const struct attribute_group *grp);
void sysfs_remove_group(struct kobject *kobj,
   const struct attribute_group *grp);
void sysfs_remove_groups(struct kobject *kobj,
    const struct attribute_group **groups);
int sysfs_add_file_to_group(struct kobject *kobj,
   const struct attribute *attr, const char *group);
void sysfs_remove_file_from_group(struct kobject *kobj,
   const struct attribute *attr, const char *group);
int sysfs_merge_group(struct kobject *kobj,
         const struct attribute_group *grp);
void sysfs_unmerge_group(struct kobject *kobj,
         const struct attribute_group *grp);
int sysfs_add_link_to_group(struct kobject *kobj, const char *group_name,
       struct kobject *target, const char *link_name);
void sysfs_remove_link_from_group(struct kobject *kobj, const char *group_name,
      const char *link_name);

void sysfs_notify(struct kobject *kobj, const char *dir, const char *attr);

int __attribute__((warn_unused_result)) sysfs_init(void);

static inline __attribute__((no_instrument_function)) void sysfs_enable_ns(struct kernfs_node *kn)
{
 kernfs_enable_ns(kn);
}
# 431 "include/linux/sysfs.h"
static inline __attribute__((no_instrument_function)) int __attribute__((warn_unused_result)) sysfs_create_file(struct kobject *kobj,
       const struct attribute *attr)
{
 return sysfs_create_file_ns(kobj, attr, ((void *)0));
}

static inline __attribute__((no_instrument_function)) void sysfs_remove_file(struct kobject *kobj,
         const struct attribute *attr)
{
 return sysfs_remove_file_ns(kobj, attr, ((void *)0));
}

static inline __attribute__((no_instrument_function)) int sysfs_rename_link(struct kobject *kobj, struct kobject *target,
        const char *old_name, const char *new_name)
{
 return sysfs_rename_link_ns(kobj, target, old_name, new_name, ((void *)0));
}

static inline __attribute__((no_instrument_function)) void sysfs_notify_dirent(struct kernfs_node *kn)
{
 kernfs_notify(kn);
}

static inline __attribute__((no_instrument_function)) struct kernfs_node *sysfs_get_dirent(struct kernfs_node *parent,
         const unsigned char *name)
{
 return kernfs_find_and_get(parent, name);
}

static inline __attribute__((no_instrument_function)) struct kernfs_node *sysfs_get(struct kernfs_node *kn)
{
 kernfs_get(kn);
 return kn;
}

static inline __attribute__((no_instrument_function)) void sysfs_put(struct kernfs_node *kn)
{
 kernfs_put(kn);
}
# 22 "include/linux/kobject.h" 2


# 1 "include/linux/kref.h" 1
# 24 "include/linux/kref.h"
struct kref {
 atomic_t refcount;
};





static inline __attribute__((no_instrument_function)) void kref_init(struct kref *kref)
{
 atomic_set(&kref->refcount, 1);
}





static inline __attribute__((no_instrument_function)) void kref_get(struct kref *kref)
{




 ({ static bool __attribute__ ((__section__(".data.unlikely"))) __warned; int __ret_warn_once = !!((atomic_add_return(1, &kref->refcount)) < 2); if (__builtin_expect(!!(__ret_warn_once), 0)) if (({ int __ret_warn_on = !!(!__warned); if (__builtin_expect(!!(__ret_warn_on), 0)) warn_slowpath_null("include/linux/kref.h", 47); __builtin_expect(!!(__ret_warn_on), 0); })) __warned = true; __builtin_expect(!!(__ret_warn_once), 0); });
}
# 68 "include/linux/kref.h"
static inline __attribute__((no_instrument_function)) int kref_sub(struct kref *kref, unsigned int count,
      void (*release)(struct kref *kref))
{
 ({ int __ret_warn_on = !!(release == ((void *)0)); if (__builtin_expect(!!(__ret_warn_on), 0)) warn_slowpath_null("include/linux/kref.h", 71); __builtin_expect(!!(__ret_warn_on), 0); });

 if (atomic_sub_and_test((int) count, &kref->refcount)) {
  release(kref);
  return 1;
 }
 return 0;
}
# 97 "include/linux/kref.h"
static inline __attribute__((no_instrument_function)) int kref_put(struct kref *kref, void (*release)(struct kref *kref))
{
 return kref_sub(kref, 1, release);
}
# 115 "include/linux/kref.h"
static inline __attribute__((no_instrument_function)) int kref_put_spinlock_irqsave(struct kref *kref,
  void (*release)(struct kref *kref),
  spinlock_t *lock)
{
 unsigned long flags;

 ({ int __ret_warn_on = !!(release == ((void *)0)); if (__builtin_expect(!!(__ret_warn_on), 0)) warn_slowpath_null("include/linux/kref.h", 121); __builtin_expect(!!(__ret_warn_on), 0); });
 if (atomic_add_unless(&kref->refcount, -1, 1))
  return 0;
 do { do { ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); flags = _raw_spin_lock_irqsave(spinlock_check(lock)); } while (0); } while (0);
 if (atomic_dec_and_test(&kref->refcount)) {
  release(kref);
  do { if (({ ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); arch_irqs_disabled_flags(flags); })) { do { ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); arch_local_irq_restore(flags); } while (0); trace_hardirqs_off(); } else { trace_hardirqs_on(); do { ({ unsigned long __dummy; typeof(flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); arch_local_irq_restore(flags); } while (0); } } while (0);
  return 1;
 }
 spin_unlock_irqrestore(lock, flags);
 return 0;
}

static inline __attribute__((no_instrument_function)) int kref_put_mutex(struct kref *kref,
     void (*release)(struct kref *kref),
     struct mutex *lock)
{
 ({ int __ret_warn_on = !!(release == ((void *)0)); if (__builtin_expect(!!(__ret_warn_on), 0)) warn_slowpath_null("include/linux/kref.h", 138); __builtin_expect(!!(__ret_warn_on), 0); });
 if (__builtin_expect(!!(!atomic_add_unless(&kref->refcount, -1, 1)), 0)) {
  mutex_lock_nested(lock, 0);
  if (__builtin_expect(!!(!atomic_dec_and_test(&kref->refcount)), 0)) {
   mutex_unlock(lock);
   return 0;
  }
  release(kref);
  return 1;
 }
 return 0;
}
# 167 "include/linux/kref.h"
static inline __attribute__((no_instrument_function)) int __attribute__((warn_unused_result)) kref_get_unless_zero(struct kref *kref)
{
 return atomic_add_unless(&kref->refcount, 1, 0);
}
# 25 "include/linux/kobject.h" 2




# 1 "include/linux/workqueue.h" 1







# 1 "include/linux/timer.h" 1




# 1 "include/linux/ktime.h" 1
# 25 "include/linux/ktime.h"
# 1 "include/linux/jiffies.h" 1







# 1 "include/linux/timex.h" 1
# 56 "include/linux/timex.h"
# 1 "include/uapi/linux/timex.h" 1
# 64 "include/uapi/linux/timex.h"
struct timex {
 unsigned int modes;
 __kernel_long_t offset;
 __kernel_long_t freq;
 __kernel_long_t maxerror;
 __kernel_long_t esterror;
 int status;
 __kernel_long_t constant;
 __kernel_long_t precision;
 __kernel_long_t tolerance;


 struct timeval time;
 __kernel_long_t tick;

 __kernel_long_t ppsfreq;
 __kernel_long_t jitter;
 int shift;
 __kernel_long_t stabil;
 __kernel_long_t jitcnt;
 __kernel_long_t calcnt;
 __kernel_long_t errcnt;
 __kernel_long_t stbcnt;

 int tai;

 int :32; int :32; int :32; int :32;
 int :32; int :32; int :32; int :32;
 int :32; int :32; int :32;
};
# 57 "include/linux/timex.h" 2






# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/include/uapi/linux/param.h" 1



# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/param.h" 1
# 1 "include/asm-generic/param.h" 1



# 1 "include/uapi/asm-generic/param.h" 1
# 5 "include/asm-generic/param.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/param.h" 2
# 5 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/include/uapi/linux/param.h" 2
# 64 "include/linux/timex.h" 2

# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/timex.h" 1




# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/tsc.h" 1
# 15 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/tsc.h"
typedef unsigned long long cycles_t;

extern unsigned int cpu_khz;
extern unsigned int tsc_khz;

extern void disable_TSC(void);

static inline __attribute__((no_instrument_function)) cycles_t get_cycles(void)
{
 unsigned long long ret = 0;





 (ret = paravirt_read_tsc());

 return ret;
}

static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) cycles_t vget_cycles(void)
{
# 45 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/tsc.h"
 return (cycles_t)__native_read_tsc();
}

extern void tsc_init(void);
extern void mark_tsc_unstable(char *reason);
extern int unsynchronized_tsc(void);
extern int check_tsc_unstable(void);
extern int check_tsc_disabled(void);
extern unsigned long native_calibrate_tsc(void);

extern int tsc_clocksource_reliable;





extern void check_tsc_sync_source(int cpu);
extern void check_tsc_sync_target(void);

extern int notsc_setup(char *);
extern void tsc_save_sched_clock_state(void);
extern void tsc_restore_sched_clock_state(void);


unsigned long try_msr_calibrate_tsc(void);
# 6 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/timex.h" 2
# 66 "include/linux/timex.h" 2
# 139 "include/linux/timex.h"
extern unsigned long tick_usec;
extern unsigned long tick_nsec;
# 154 "include/linux/timex.h"
extern int do_adjtimex(struct timex *);
extern void hardpps(const struct timespec *, const struct timespec *);

int read_current_timer(unsigned long *timer_val);
void ntp_notify_cmos_timer(void);
# 9 "include/linux/jiffies.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/param.h" 1
# 10 "include/linux/jiffies.h" 2
# 57 "include/linux/jiffies.h"
extern int register_refined_jiffies(long clock_tick_rate);
# 76 "include/linux/jiffies.h"
extern u64 __attribute__((section(".data"))) jiffies_64;
extern unsigned long volatile __attribute__((section(".data"))) jiffies;




static inline __attribute__((no_instrument_function)) u64 get_jiffies_64(void)
{
 return (u64)jiffies;
}
# 182 "include/linux/jiffies.h"
extern unsigned long preset_lpj;
# 295 "include/linux/jiffies.h"
extern unsigned int jiffies_to_msecs(const unsigned long j);
extern unsigned int jiffies_to_usecs(const unsigned long j);

static inline __attribute__((no_instrument_function)) u64 jiffies_to_nsecs(const unsigned long j)
{
 return (u64)jiffies_to_usecs(j) * 1000L;
}

extern unsigned long msecs_to_jiffies(const unsigned int m);
extern unsigned long usecs_to_jiffies(const unsigned int u);
extern unsigned long timespec_to_jiffies(const struct timespec *value);
extern void jiffies_to_timespec(const unsigned long jiffies,
    struct timespec *value);
extern unsigned long timeval_to_jiffies(const struct timeval *value);
extern void jiffies_to_timeval(const unsigned long jiffies,
          struct timeval *value);

extern clock_t jiffies_to_clock_t(unsigned long x);
static inline __attribute__((no_instrument_function)) clock_t jiffies_delta_to_clock_t(long delta)
{
 return jiffies_to_clock_t(({ typeof(0L) _max1 = (0L); typeof(delta) _max2 = (delta); (void) (&_max1 == &_max2); _max1 > _max2 ? _max1 : _max2; }));
}

extern unsigned long clock_t_to_jiffies(unsigned long x);
extern u64 jiffies_64_to_clock_t(u64 x);
extern u64 nsec_to_clock_t(u64 x);
extern u64 nsecs_to_jiffies64(u64 n);
extern unsigned long nsecs_to_jiffies(u64 n);
# 26 "include/linux/ktime.h" 2
# 46 "include/linux/ktime.h"
union ktime {
 s64 tv64;
# 57 "include/linux/ktime.h"
};

typedef union ktime ktime_t;
# 74 "include/linux/ktime.h"
static inline __attribute__((no_instrument_function)) ktime_t ktime_set(const long secs, const unsigned long nsecs)
{

 if (__builtin_expect(!!(secs >= (((s64)~((u64)1 << 63)) / 1000000000L)), 0))
  return (ktime_t){ .tv64 = ((s64)~((u64)1 << 63)) };

 return (ktime_t) { .tv64 = (s64)secs * 1000000000L + (s64)nsecs };
}
# 106 "include/linux/ktime.h"
static inline __attribute__((no_instrument_function)) ktime_t timespec_to_ktime(struct timespec ts)
{
 return ktime_set(ts.tv_sec, ts.tv_nsec);
}


static inline __attribute__((no_instrument_function)) ktime_t timeval_to_ktime(struct timeval tv)
{
 return ktime_set(tv.tv_sec, tv.tv_usec * 1000L);
}
# 283 "include/linux/ktime.h"
static inline __attribute__((no_instrument_function)) int ktime_equal(const ktime_t cmp1, const ktime_t cmp2)
{
 return cmp1.tv64 == cmp2.tv64;
}
# 298 "include/linux/ktime.h"
static inline __attribute__((no_instrument_function)) int ktime_compare(const ktime_t cmp1, const ktime_t cmp2)
{
 if (cmp1.tv64 < cmp2.tv64)
  return -1;
 if (cmp1.tv64 > cmp2.tv64)
  return 1;
 return 0;
}

static inline __attribute__((no_instrument_function)) s64 ktime_to_us(const ktime_t kt)
{
 struct timeval tv = ns_to_timeval((kt).tv64);
 return (s64) tv.tv_sec * 1000000L + tv.tv_usec;
}

static inline __attribute__((no_instrument_function)) s64 ktime_to_ms(const ktime_t kt)
{
 struct timeval tv = ns_to_timeval((kt).tv64);
 return (s64) tv.tv_sec * 1000L + tv.tv_usec / 1000L;
}

static inline __attribute__((no_instrument_function)) s64 ktime_us_delta(const ktime_t later, const ktime_t earlier)
{
       return ktime_to_us(({ (ktime_t){ .tv64 = (later).tv64 - (earlier).tv64 }; }));
}

static inline __attribute__((no_instrument_function)) ktime_t ktime_add_us(const ktime_t kt, const u64 usec)
{
 return ({ (ktime_t){ .tv64 = (kt).tv64 + (usec * 1000L) }; });
}

static inline __attribute__((no_instrument_function)) ktime_t ktime_add_ms(const ktime_t kt, const u64 msec)
{
 return ({ (ktime_t){ .tv64 = (kt).tv64 + (msec * 1000000L) }; });
}

static inline __attribute__((no_instrument_function)) ktime_t ktime_sub_us(const ktime_t kt, const u64 usec)
{
 return ({ (ktime_t){ .tv64 = (kt).tv64 - (usec * 1000L) }; });
}

extern ktime_t ktime_add_safe(const ktime_t lhs, const ktime_t rhs);
# 349 "include/linux/ktime.h"
static inline __attribute__((no_instrument_function)) __attribute__((warn_unused_result)) bool ktime_to_timespec_cond(const ktime_t kt,
             struct timespec *ts)
{
 if (kt.tv64) {
  *ts = ns_to_timespec((kt).tv64);
  return true;
 } else {
  return false;
 }
}
# 370 "include/linux/ktime.h"
extern void ktime_get_ts(struct timespec *ts);




static inline __attribute__((no_instrument_function)) ktime_t ns_to_ktime(u64 ns)
{
 static const ktime_t ktime_zero = { .tv64 = 0 };

 return ({ (ktime_t){ .tv64 = (ktime_zero).tv64 + (ns) }; });
}

static inline __attribute__((no_instrument_function)) ktime_t ms_to_ktime(u64 ms)
{
 static const ktime_t ktime_zero = { .tv64 = 0 };

 return ktime_add_ms(ktime_zero, ms);
}
# 6 "include/linux/timer.h" 2




struct tvec_base;

struct timer_list {




 struct list_head entry;
 unsigned long expires;
 struct tvec_base *base;

 void (*function)(unsigned long);
 unsigned long data;

 int slack;


 int start_pid;
 void *start_site;
 char start_comm[16];


 struct lockdep_map lockdep_map;

};

extern struct tvec_base boot_tvec_bases;
# 94 "include/linux/timer.h"
void init_timer_key(struct timer_list *timer, unsigned int flags,
      const char *name, struct lock_class_key *key);


extern void init_timer_on_stack_key(struct timer_list *timer,
        unsigned int flags, const char *name,
        struct lock_class_key *key);
extern void destroy_timer_on_stack(struct timer_list *timer);
# 169 "include/linux/timer.h"
static inline __attribute__((no_instrument_function)) int timer_pending(const struct timer_list * timer)
{
 return timer->entry.next != ((void *)0);
}

extern void add_timer_on(struct timer_list *timer, int cpu);
extern int del_timer(struct timer_list * timer);
extern int mod_timer(struct timer_list *timer, unsigned long expires);
extern int mod_timer_pending(struct timer_list *timer, unsigned long expires);
extern int mod_timer_pinned(struct timer_list *timer, unsigned long expires);

extern void set_timer_slack(struct timer_list *time, int slack_hz);
# 195 "include/linux/timer.h"
extern unsigned long get_next_timer_interrupt(unsigned long now);






extern int timer_stats_active;



extern void init_timer_stats(void);

extern void timer_stats_update_stats(void *timer, pid_t pid, void *startf,
         void *timerf, char *comm,
         unsigned int timer_flag);

extern void __timer_stats_timer_set_start_info(struct timer_list *timer,
            void *addr);

static inline __attribute__((no_instrument_function)) void timer_stats_timer_set_start_info(struct timer_list *timer)
{
 if (__builtin_expect(!!(!timer_stats_active), 1))
  return;
 __timer_stats_timer_set_start_info(timer, __builtin_return_address(0));
}

static inline __attribute__((no_instrument_function)) void timer_stats_timer_clear_start_info(struct timer_list *timer)
{
 timer->start_site = ((void *)0);
}
# 240 "include/linux/timer.h"
extern void add_timer(struct timer_list *timer);

extern int try_to_del_timer_sync(struct timer_list *timer);


  extern int del_timer_sync(struct timer_list *timer);






extern void init_timers(void);
extern void run_local_timers(void);
struct hrtimer;
extern enum hrtimer_restart it_real_fn(struct hrtimer *);

unsigned long __round_jiffies(unsigned long j, int cpu);
unsigned long __round_jiffies_relative(unsigned long j, int cpu);
unsigned long round_jiffies(unsigned long j);
unsigned long round_jiffies_relative(unsigned long j);

unsigned long __round_jiffies_up(unsigned long j, int cpu);
unsigned long __round_jiffies_up_relative(unsigned long j, int cpu);
unsigned long round_jiffies_up(unsigned long j);
unsigned long round_jiffies_up_relative(unsigned long j);
# 9 "include/linux/workqueue.h" 2







struct workqueue_struct;

struct work_struct;
typedef void (*work_func_t)(struct work_struct *work);
void delayed_work_timer_fn(unsigned long __data);







enum {
 WORK_STRUCT_PENDING_BIT = 0,
 WORK_STRUCT_DELAYED_BIT = 1,
 WORK_STRUCT_PWQ_BIT = 2,
 WORK_STRUCT_LINKED_BIT = 3,

 WORK_STRUCT_STATIC_BIT = 4,
 WORK_STRUCT_COLOR_SHIFT = 5,




 WORK_STRUCT_COLOR_BITS = 4,

 WORK_STRUCT_PENDING = 1 << WORK_STRUCT_PENDING_BIT,
 WORK_STRUCT_DELAYED = 1 << WORK_STRUCT_DELAYED_BIT,
 WORK_STRUCT_PWQ = 1 << WORK_STRUCT_PWQ_BIT,
 WORK_STRUCT_LINKED = 1 << WORK_STRUCT_LINKED_BIT,

 WORK_STRUCT_STATIC = 1 << WORK_STRUCT_STATIC_BIT,
# 56 "include/linux/workqueue.h"
 WORK_NR_COLORS = (1 << WORK_STRUCT_COLOR_BITS) - 1,
 WORK_NO_COLOR = WORK_NR_COLORS,


 WORK_CPU_UNBOUND = 8192,
 WORK_CPU_END = 8192 + 1,






 WORK_STRUCT_FLAG_BITS = WORK_STRUCT_COLOR_SHIFT +
      WORK_STRUCT_COLOR_BITS,


 WORK_OFFQ_FLAG_BASE = WORK_STRUCT_COLOR_SHIFT,

 WORK_OFFQ_CANCELING = (1 << WORK_OFFQ_FLAG_BASE),






 WORK_OFFQ_FLAG_BITS = 1,
 WORK_OFFQ_POOL_SHIFT = WORK_OFFQ_FLAG_BASE + WORK_OFFQ_FLAG_BITS,
 WORK_OFFQ_LEFT = 64 - WORK_OFFQ_POOL_SHIFT,
 WORK_OFFQ_POOL_BITS = WORK_OFFQ_LEFT <= 31 ? WORK_OFFQ_LEFT : 31,
 WORK_OFFQ_POOL_NONE = (1UL << WORK_OFFQ_POOL_BITS) - 1,


 WORK_STRUCT_FLAG_MASK = (1UL << WORK_STRUCT_FLAG_BITS) - 1,
 WORK_STRUCT_WQ_DATA_MASK = ~WORK_STRUCT_FLAG_MASK,
 WORK_STRUCT_NO_POOL = (unsigned long)WORK_OFFQ_POOL_NONE << WORK_OFFQ_POOL_SHIFT,


 WORK_BUSY_PENDING = 1 << 0,
 WORK_BUSY_RUNNING = 1 << 1,


 WORKER_DESC_LEN = 24,
};

struct work_struct {
 atomic_long_t data;
 struct list_head entry;
 work_func_t func;

 struct lockdep_map lockdep_map;

};





struct delayed_work {
 struct work_struct work;
 struct timer_list timer;


 struct workqueue_struct *wq;
 int cpu;
};
# 130 "include/linux/workqueue.h"
struct workqueue_attrs {
 int nice;
 cpumask_var_t cpumask;
 bool no_numa;
};

static inline __attribute__((no_instrument_function)) struct delayed_work *to_delayed_work(struct work_struct *work)
{
 return ({ const typeof( ((struct delayed_work *)0)->work ) *__mptr = (work); (struct delayed_work *)( (char *)__mptr - __builtin_offsetof(struct delayed_work,work) );});
}

struct execute_work {
 struct work_struct work;
};
# 181 "include/linux/workqueue.h"
extern void __init_work(struct work_struct *work, int onstack);
extern void destroy_work_on_stack(struct work_struct *work);
extern void destroy_delayed_work_on_stack(struct delayed_work *work);
static inline __attribute__((no_instrument_function)) unsigned int work_static(struct work_struct *work)
{
 return *((unsigned long *)(&(work)->data)) & WORK_STRUCT_STATIC;
}
# 288 "include/linux/workqueue.h"
enum {
 WQ_UNBOUND = 1 << 1,
 WQ_FREEZABLE = 1 << 2,
 WQ_MEM_RECLAIM = 1 << 3,
 WQ_HIGHPRI = 1 << 4,
 WQ_CPU_INTENSIVE = 1 << 5,
 WQ_SYSFS = 1 << 6,
# 321 "include/linux/workqueue.h"
 WQ_POWER_EFFICIENT = 1 << 7,

 __WQ_DRAINING = 1 << 16,
 __WQ_ORDERED = 1 << 17,

 WQ_MAX_ACTIVE = 512,
 WQ_MAX_UNBOUND_PER_CPU = 4,
 WQ_DFL_ACTIVE = WQ_MAX_ACTIVE / 2,
};
# 360 "include/linux/workqueue.h"
extern struct workqueue_struct *system_wq;
extern struct workqueue_struct *system_long_wq;
extern struct workqueue_struct *system_unbound_wq;
extern struct workqueue_struct *system_freezable_wq;
extern struct workqueue_struct *system_power_efficient_wq;
extern struct workqueue_struct *system_freezable_power_efficient_wq;

static inline __attribute__((no_instrument_function)) struct workqueue_struct * __attribute__((deprecated)) __system_nrt_wq(void)
{
 return system_wq;
}

static inline __attribute__((no_instrument_function)) struct workqueue_struct * __attribute__((deprecated)) __system_nrt_freezable_wq(void)
{
 return system_freezable_wq;
}





extern struct workqueue_struct *
__alloc_workqueue_key(const char *fmt, unsigned int flags, int max_active,
 struct lock_class_key *key, const char *lock_name, ...) __attribute__((format(printf, 1, 6)));
# 442 "include/linux/workqueue.h"
extern void destroy_workqueue(struct workqueue_struct *wq);

struct workqueue_attrs *alloc_workqueue_attrs(gfp_t gfp_mask);
void free_workqueue_attrs(struct workqueue_attrs *attrs);
int apply_workqueue_attrs(struct workqueue_struct *wq,
     const struct workqueue_attrs *attrs);

extern bool queue_work_on(int cpu, struct workqueue_struct *wq,
   struct work_struct *work);
extern bool queue_delayed_work_on(int cpu, struct workqueue_struct *wq,
   struct delayed_work *work, unsigned long delay);
extern bool mod_delayed_work_on(int cpu, struct workqueue_struct *wq,
   struct delayed_work *dwork, unsigned long delay);

extern void flush_workqueue(struct workqueue_struct *wq);
extern void drain_workqueue(struct workqueue_struct *wq);
extern void flush_scheduled_work(void);

extern int schedule_on_each_cpu(work_func_t func);

int execute_in_process_context(work_func_t fn, struct execute_work *);

extern bool flush_work(struct work_struct *work);
extern bool cancel_work_sync(struct work_struct *work);

extern bool flush_delayed_work(struct delayed_work *dwork);
extern bool cancel_delayed_work(struct delayed_work *dwork);
extern bool cancel_delayed_work_sync(struct delayed_work *dwork);

extern void workqueue_set_max_active(struct workqueue_struct *wq,
         int max_active);
extern bool current_is_workqueue_rescuer(void);
extern bool workqueue_congested(int cpu, struct workqueue_struct *wq);
extern unsigned int work_busy(struct work_struct *work);
extern __attribute__((format(printf, 1, 2))) void set_worker_desc(const char *fmt, ...);
extern void print_worker_info(const char *log_lvl, struct task_struct *task);
# 489 "include/linux/workqueue.h"
static inline __attribute__((no_instrument_function)) bool queue_work(struct workqueue_struct *wq,
         struct work_struct *work)
{
 return queue_work_on(WORK_CPU_UNBOUND, wq, work);
}
# 503 "include/linux/workqueue.h"
static inline __attribute__((no_instrument_function)) bool queue_delayed_work(struct workqueue_struct *wq,
          struct delayed_work *dwork,
          unsigned long delay)
{
 return queue_delayed_work_on(WORK_CPU_UNBOUND, wq, dwork, delay);
}
# 518 "include/linux/workqueue.h"
static inline __attribute__((no_instrument_function)) bool mod_delayed_work(struct workqueue_struct *wq,
        struct delayed_work *dwork,
        unsigned long delay)
{
 return mod_delayed_work_on(WORK_CPU_UNBOUND, wq, dwork, delay);
}
# 532 "include/linux/workqueue.h"
static inline __attribute__((no_instrument_function)) bool schedule_work_on(int cpu, struct work_struct *work)
{
 return queue_work_on(cpu, system_wq, work);
}
# 548 "include/linux/workqueue.h"
static inline __attribute__((no_instrument_function)) bool schedule_work(struct work_struct *work)
{
 return queue_work(system_wq, work);
}
# 562 "include/linux/workqueue.h"
static inline __attribute__((no_instrument_function)) bool schedule_delayed_work_on(int cpu, struct delayed_work *dwork,
         unsigned long delay)
{
 return queue_delayed_work_on(cpu, system_wq, dwork, delay);
}
# 576 "include/linux/workqueue.h"
static inline __attribute__((no_instrument_function)) bool schedule_delayed_work(struct delayed_work *dwork,
      unsigned long delay)
{
 return queue_delayed_work(system_wq, dwork, delay);
}




static inline __attribute__((no_instrument_function)) bool keventd_up(void)
{
 return system_wq != ((void *)0);
}


static inline __attribute__((no_instrument_function)) bool __attribute__((deprecated)) flush_work_sync(struct work_struct *work)
{
 return flush_work(work);
}


static inline __attribute__((no_instrument_function)) bool __attribute__((deprecated)) flush_delayed_work_sync(struct delayed_work *dwork)
{
 return flush_delayed_work(dwork);
}







long work_on_cpu(int cpu, long (*fn)(void *), void *arg);



extern void freeze_workqueues_begin(void);
extern bool freeze_workqueues_busy(void);
extern void thaw_workqueues(void);



int workqueue_sysfs_register(struct workqueue_struct *wq);
# 30 "include/linux/kobject.h" 2






extern char uevent_helper[];


extern u64 uevent_seqnum;
# 51 "include/linux/kobject.h"
enum kobject_action {
 KOBJ_ADD,
 KOBJ_REMOVE,
 KOBJ_CHANGE,
 KOBJ_MOVE,
 KOBJ_ONLINE,
 KOBJ_OFFLINE,
 KOBJ_MAX
};

struct kobject {
 const char *name;
 struct list_head entry;
 struct kobject *parent;
 struct kset *kset;
 struct kobj_type *ktype;
 struct kernfs_node *sd;
 struct kref kref;

 struct delayed_work release;

 unsigned int state_initialized:1;
 unsigned int state_in_sysfs:1;
 unsigned int state_add_uevent_sent:1;
 unsigned int state_remove_uevent_sent:1;
 unsigned int uevent_suppress:1;
};

extern __attribute__((format(printf, 2, 3)))
int kobject_set_name(struct kobject *kobj, const char *name, ...);
extern int kobject_set_name_vargs(struct kobject *kobj, const char *fmt,
      va_list vargs);

static inline __attribute__((no_instrument_function)) const char *kobject_name(const struct kobject *kobj)
{
 return kobj->name;
}

extern void kobject_init(struct kobject *kobj, struct kobj_type *ktype);
extern __attribute__((format(printf, 3, 4))) __attribute__((warn_unused_result))
int kobject_add(struct kobject *kobj, struct kobject *parent,
  const char *fmt, ...);
extern __attribute__((format(printf, 4, 5))) __attribute__((warn_unused_result))
int kobject_init_and_add(struct kobject *kobj,
    struct kobj_type *ktype, struct kobject *parent,
    const char *fmt, ...);

extern void kobject_del(struct kobject *kobj);

extern struct kobject * __attribute__((warn_unused_result)) kobject_create(void);
extern struct kobject * __attribute__((warn_unused_result)) kobject_create_and_add(const char *name,
      struct kobject *parent);

extern int __attribute__((warn_unused_result)) kobject_rename(struct kobject *, const char *new_name);
extern int __attribute__((warn_unused_result)) kobject_move(struct kobject *, struct kobject *);

extern struct kobject *kobject_get(struct kobject *kobj);
extern void kobject_put(struct kobject *kobj);

extern const void *kobject_namespace(struct kobject *kobj);
extern char *kobject_get_path(struct kobject *kobj, gfp_t flag);

struct kobj_type {
 void (*release)(struct kobject *kobj);
 const struct sysfs_ops *sysfs_ops;
 struct attribute **default_attrs;
 const struct kobj_ns_type_operations *(*child_ns_type)(struct kobject *kobj);
 const void *(*namespace)(struct kobject *kobj);
};

struct kobj_uevent_env {
 char *argv[3];
 char *envp[32];
 int envp_idx;
 char buf[2048];
 int buflen;
};

struct kset_uevent_ops {
 int (* const filter)(struct kset *kset, struct kobject *kobj);
 const char *(* const name)(struct kset *kset, struct kobject *kobj);
 int (* const uevent)(struct kset *kset, struct kobject *kobj,
        struct kobj_uevent_env *env);
};

struct kobj_attribute {
 struct attribute attr;
 ssize_t (*show)(struct kobject *kobj, struct kobj_attribute *attr,
   char *buf);
 ssize_t (*store)(struct kobject *kobj, struct kobj_attribute *attr,
    const char *buf, size_t count);
};

extern const struct sysfs_ops kobj_sysfs_ops;

struct sock;
# 165 "include/linux/kobject.h"
struct kset {
 struct list_head list;
 spinlock_t list_lock;
 struct kobject kobj;
 const struct kset_uevent_ops *uevent_ops;
};

extern void kset_init(struct kset *kset);
extern int __attribute__((warn_unused_result)) kset_register(struct kset *kset);
extern void kset_unregister(struct kset *kset);
extern struct kset * __attribute__((warn_unused_result)) kset_create_and_add(const char *name,
      const struct kset_uevent_ops *u,
      struct kobject *parent_kobj);

static inline __attribute__((no_instrument_function)) struct kset *to_kset(struct kobject *kobj)
{
 return kobj ? ({ const typeof( ((struct kset *)0)->kobj ) *__mptr = (kobj); (struct kset *)( (char *)__mptr - __builtin_offsetof(struct kset,kobj) );}) : ((void *)0);
}

static inline __attribute__((no_instrument_function)) struct kset *kset_get(struct kset *k)
{
 return k ? to_kset(kobject_get(&k->kobj)) : ((void *)0);
}

static inline __attribute__((no_instrument_function)) void kset_put(struct kset *k)
{
 kobject_put(&k->kobj);
}

static inline __attribute__((no_instrument_function)) struct kobj_type *get_ktype(struct kobject *kobj)
{
 return kobj->ktype;
}

extern struct kobject *kset_find_obj(struct kset *, const char *);


extern struct kobject *kernel_kobj;

extern struct kobject *mm_kobj;

extern struct kobject *hypervisor_kobj;

extern struct kobject *power_kobj;

extern struct kobject *firmware_kobj;

int kobject_uevent(struct kobject *kobj, enum kobject_action action);
int kobject_uevent_env(struct kobject *kobj, enum kobject_action action,
   char *envp[]);

__attribute__((format(printf, 2, 3)))
int add_uevent_var(struct kobj_uevent_env *env, const char *format, ...);

int kobject_action_type(const char *buf, size_t count,
   enum kobject_action *type);
# 18 "include/linux/device.h" 2
# 1 "include/linux/klist.h" 1
# 19 "include/linux/klist.h"
struct klist_node;
struct klist {
 spinlock_t k_lock;
 struct list_head k_list;
 void (*get)(struct klist_node *);
 void (*put)(struct klist_node *);
} __attribute__ ((aligned (sizeof(void *))));
# 36 "include/linux/klist.h"
extern void klist_init(struct klist *k, void (*get)(struct klist_node *),
         void (*put)(struct klist_node *));

struct klist_node {
 void *n_klist;
 struct list_head n_node;
 struct kref n_ref;
};

extern void klist_add_tail(struct klist_node *n, struct klist *k);
extern void klist_add_head(struct klist_node *n, struct klist *k);
extern void klist_add_after(struct klist_node *n, struct klist_node *pos);
extern void klist_add_before(struct klist_node *n, struct klist_node *pos);

extern void klist_del(struct klist_node *n);
extern void klist_remove(struct klist_node *n);

extern int klist_node_attached(struct klist_node *n);


struct klist_iter {
 struct klist *i_klist;
 struct klist_node *i_cur;
};


extern void klist_iter_init(struct klist *k, struct klist_iter *i);
extern void klist_iter_init_node(struct klist *k, struct klist_iter *i,
     struct klist_node *n);
extern void klist_iter_exit(struct klist_iter *i);
extern struct klist_node *klist_next(struct klist_iter *i);
# 19 "include/linux/device.h" 2





# 1 "include/linux/pinctrl/devinfo.h" 1
# 21 "include/linux/pinctrl/devinfo.h"
# 1 "include/linux/pinctrl/consumer.h" 1
# 17 "include/linux/pinctrl/consumer.h"
# 1 "include/linux/seq_file.h" 1
# 9 "include/linux/seq_file.h"
# 1 "include/linux/nodemask.h" 1
# 96 "include/linux/nodemask.h"
# 1 "include/linux/numa.h" 1
# 97 "include/linux/nodemask.h" 2

typedef struct { unsigned long bits[((((1 << 10)) + (8 * sizeof(long)) - 1) / (8 * sizeof(long)))]; } nodemask_t;
extern nodemask_t _unused_nodemask_arg_;
# 111 "include/linux/nodemask.h"
static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) void __node_set(int node, volatile nodemask_t *dstp)
{
 set_bit(node, dstp->bits);
}


static inline __attribute__((no_instrument_function)) void __node_clear(int node, volatile nodemask_t *dstp)
{
 clear_bit(node, dstp->bits);
}


static inline __attribute__((no_instrument_function)) void __nodes_setall(nodemask_t *dstp, int nbits)
{
 bitmap_fill(dstp->bits, nbits);
}


static inline __attribute__((no_instrument_function)) void __nodes_clear(nodemask_t *dstp, int nbits)
{
 bitmap_zero(dstp->bits, nbits);
}






static inline __attribute__((no_instrument_function)) int __node_test_and_set(int node, nodemask_t *addr)
{
 return test_and_set_bit(node, addr->bits);
}



static inline __attribute__((no_instrument_function)) void __nodes_and(nodemask_t *dstp, const nodemask_t *src1p,
     const nodemask_t *src2p, int nbits)
{
 bitmap_and(dstp->bits, src1p->bits, src2p->bits, nbits);
}



static inline __attribute__((no_instrument_function)) void __nodes_or(nodemask_t *dstp, const nodemask_t *src1p,
     const nodemask_t *src2p, int nbits)
{
 bitmap_or(dstp->bits, src1p->bits, src2p->bits, nbits);
}



static inline __attribute__((no_instrument_function)) void __nodes_xor(nodemask_t *dstp, const nodemask_t *src1p,
     const nodemask_t *src2p, int nbits)
{
 bitmap_xor(dstp->bits, src1p->bits, src2p->bits, nbits);
}



static inline __attribute__((no_instrument_function)) void __nodes_andnot(nodemask_t *dstp, const nodemask_t *src1p,
     const nodemask_t *src2p, int nbits)
{
 bitmap_andnot(dstp->bits, src1p->bits, src2p->bits, nbits);
}



static inline __attribute__((no_instrument_function)) void __nodes_complement(nodemask_t *dstp,
     const nodemask_t *srcp, int nbits)
{
 bitmap_complement(dstp->bits, srcp->bits, nbits);
}



static inline __attribute__((no_instrument_function)) int __nodes_equal(const nodemask_t *src1p,
     const nodemask_t *src2p, int nbits)
{
 return bitmap_equal(src1p->bits, src2p->bits, nbits);
}



static inline __attribute__((no_instrument_function)) int __nodes_intersects(const nodemask_t *src1p,
     const nodemask_t *src2p, int nbits)
{
 return bitmap_intersects(src1p->bits, src2p->bits, nbits);
}



static inline __attribute__((no_instrument_function)) int __nodes_subset(const nodemask_t *src1p,
     const nodemask_t *src2p, int nbits)
{
 return bitmap_subset(src1p->bits, src2p->bits, nbits);
}


static inline __attribute__((no_instrument_function)) int __nodes_empty(const nodemask_t *srcp, int nbits)
{
 return bitmap_empty(srcp->bits, nbits);
}


static inline __attribute__((no_instrument_function)) int __nodes_full(const nodemask_t *srcp, int nbits)
{
 return bitmap_full(srcp->bits, nbits);
}


static inline __attribute__((no_instrument_function)) int __nodes_weight(const nodemask_t *srcp, int nbits)
{
 return bitmap_weight(srcp->bits, nbits);
}



static inline __attribute__((no_instrument_function)) void __nodes_shift_right(nodemask_t *dstp,
     const nodemask_t *srcp, int n, int nbits)
{
 bitmap_shift_right(dstp->bits, srcp->bits, n, nbits);
}



static inline __attribute__((no_instrument_function)) void __nodes_shift_left(nodemask_t *dstp,
     const nodemask_t *srcp, int n, int nbits)
{
 bitmap_shift_left(dstp->bits, srcp->bits, n, nbits);
}





static inline __attribute__((no_instrument_function)) int __first_node(const nodemask_t *srcp)
{
 return ({ int __min1 = ((1 << 10)); int __min2 = (find_first_bit(srcp->bits, (1 << 10))); __min1 < __min2 ? __min1: __min2; });
}


static inline __attribute__((no_instrument_function)) int __next_node(int n, const nodemask_t *srcp)
{
 return ({ int __min1 = ((1 << 10)); int __min2 = (find_next_bit(srcp->bits, (1 << 10), n+1)); __min1 < __min2 ? __min1: __min2; });
}

static inline __attribute__((no_instrument_function)) void init_nodemask_of_node(nodemask_t *mask, int node)
{
 __nodes_clear(&(*mask), (1 << 10));
 __node_set((node), &(*mask));
}
# 275 "include/linux/nodemask.h"
static inline __attribute__((no_instrument_function)) int __first_unset_node(const nodemask_t *maskp)
{
 return ({ int __min1 = ((1 << 10)); int __min2 = (find_first_zero_bit(maskp->bits, (1 << 10))); __min1 < __min2 ? __min1: __min2; })
                                                  ;
}
# 309 "include/linux/nodemask.h"
static inline __attribute__((no_instrument_function)) int __nodemask_scnprintf(char *buf, int len,
     const nodemask_t *srcp, int nbits)
{
 return bitmap_scnprintf(buf, len, srcp->bits, nbits);
}



static inline __attribute__((no_instrument_function)) int __nodemask_parse_user(const char *buf, int len,
     nodemask_t *dstp, int nbits)
{
 return bitmap_parse_user(buf, len, dstp->bits, nbits);
}



static inline __attribute__((no_instrument_function)) int __nodelist_scnprintf(char *buf, int len,
     const nodemask_t *srcp, int nbits)
{
 return bitmap_scnlistprintf(buf, len, srcp->bits, nbits);
}


static inline __attribute__((no_instrument_function)) int __nodelist_parse(const char *buf, nodemask_t *dstp, int nbits)
{
 return bitmap_parselist(buf, dstp->bits, nbits);
}



static inline __attribute__((no_instrument_function)) int __node_remap(int oldbit,
  const nodemask_t *oldp, const nodemask_t *newp, int nbits)
{
 return bitmap_bitremap(oldbit, oldp->bits, newp->bits, nbits);
}



static inline __attribute__((no_instrument_function)) void __nodes_remap(nodemask_t *dstp, const nodemask_t *srcp,
  const nodemask_t *oldp, const nodemask_t *newp, int nbits)
{
 bitmap_remap(dstp->bits, srcp->bits, oldp->bits, newp->bits, nbits);
}



static inline __attribute__((no_instrument_function)) void __nodes_onto(nodemask_t *dstp, const nodemask_t *origp,
  const nodemask_t *relmapp, int nbits)
{
 bitmap_onto(dstp->bits, origp->bits, relmapp->bits, nbits);
}



static inline __attribute__((no_instrument_function)) void __nodes_fold(nodemask_t *dstp, const nodemask_t *origp,
  int sz, int nbits)
{
 bitmap_fold(dstp->bits, origp->bits, sz, nbits);
}
# 383 "include/linux/nodemask.h"
enum node_states {
 N_POSSIBLE,
 N_ONLINE,
 N_NORMAL_MEMORY,



 N_HIGH_MEMORY = N_NORMAL_MEMORY,


 N_MEMORY,



 N_CPU,
 NR_NODE_STATES
};






extern nodemask_t node_states[NR_NODE_STATES];


static inline __attribute__((no_instrument_function)) int node_state(int node, enum node_states state)
{
 return (__builtin_constant_p(((node))) ? constant_test_bit(((node)), ((node_states[state]).bits)) : variable_test_bit(((node)), ((node_states[state]).bits)));
}

static inline __attribute__((no_instrument_function)) void node_set_state(int node, enum node_states state)
{
 __node_set(node, &node_states[state]);
}

static inline __attribute__((no_instrument_function)) void node_clear_state(int node, enum node_states state)
{
 __node_clear(node, &node_states[state]);
}

static inline __attribute__((no_instrument_function)) int num_node_state(enum node_states state)
{
 return __nodes_weight(&(node_states[state]), (1 << 10));
}







extern int nr_node_ids;
extern int nr_online_nodes;

static inline __attribute__((no_instrument_function)) void node_set_online(int nid)
{
 node_set_state(nid, N_ONLINE);
 nr_online_nodes = num_node_state(N_ONLINE);
}

static inline __attribute__((no_instrument_function)) void node_set_offline(int nid)
{
 node_clear_state(nid, N_ONLINE);
 nr_online_nodes = num_node_state(N_ONLINE);
}
# 484 "include/linux/nodemask.h"
extern int node_random(const nodemask_t *maskp);
# 518 "include/linux/nodemask.h"
struct nodemask_scratch {
 nodemask_t mask1;
 nodemask_t mask2;
};
# 10 "include/linux/seq_file.h" 2

struct seq_operations;
struct file;
struct path;
struct inode;
struct dentry;
struct user_namespace;

struct seq_file {
 char *buf;
 size_t size;
 size_t from;
 size_t count;
 size_t pad_until;
 loff_t index;
 loff_t read_pos;
 u64 version;
 struct mutex lock;
 const struct seq_operations *op;
 int poll_event;

 struct user_namespace *user_ns;

 void *private;
};

struct seq_operations {
 void * (*start) (struct seq_file *m, loff_t *pos);
 void (*stop) (struct seq_file *m, void *v);
 void * (*next) (struct seq_file *m, void *v, loff_t *pos);
 int (*show) (struct seq_file *m, void *v);
};
# 53 "include/linux/seq_file.h"
static inline __attribute__((no_instrument_function)) size_t seq_get_buf(struct seq_file *m, char **bufp)
{
 do { if (__builtin_expect(!!(m->count > m->size), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("include/linux/seq_file.h"), "i" (55), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0);
 if (m->count < m->size)
  *bufp = m->buf + m->count;
 else
  *bufp = ((void *)0);

 return m->size - m->count;
}
# 73 "include/linux/seq_file.h"
static inline __attribute__((no_instrument_function)) void seq_commit(struct seq_file *m, int num)
{
 if (num < 0) {
  m->count = m->size;
 } else {
  do { if (__builtin_expect(!!(m->count + num > m->size), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("include/linux/seq_file.h"), "i" (78), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0);
  m->count += num;
 }
}
# 91 "include/linux/seq_file.h"
static inline __attribute__((no_instrument_function)) void seq_setwidth(struct seq_file *m, size_t size)
{
 m->pad_until = m->count + size;
}
void seq_pad(struct seq_file *m, char c);

char *mangle_path(char *s, const char *p, const char *esc);
int seq_open(struct file *, const struct seq_operations *);
ssize_t seq_read(struct file *, char *, size_t, loff_t *);
loff_t seq_lseek(struct file *, loff_t, int);
int seq_release(struct inode *, struct file *);
int seq_escape(struct seq_file *, const char *, const char *);
int seq_putc(struct seq_file *m, char c);
int seq_puts(struct seq_file *m, const char *s);
int seq_write(struct seq_file *seq, const void *data, size_t len);

__attribute__((format(printf, 2, 3))) int seq_printf(struct seq_file *, const char *, ...);
__attribute__((format(printf, 2, 0))) int seq_vprintf(struct seq_file *, const char *, va_list args);

int seq_path(struct seq_file *, const struct path *, const char *);
int seq_dentry(struct seq_file *, struct dentry *, const char *);
int seq_path_root(struct seq_file *m, const struct path *path,
    const struct path *root, const char *esc);
int seq_bitmap(struct seq_file *m, const unsigned long *bits,
       unsigned int nr_bits);
static inline __attribute__((no_instrument_function)) int seq_cpumask(struct seq_file *m, const struct cpumask *mask)
{
 return seq_bitmap(m, ((mask)->bits), nr_cpu_ids);
}

static inline __attribute__((no_instrument_function)) int seq_nodemask(struct seq_file *m, nodemask_t *mask)
{
 return seq_bitmap(m, mask->bits, (1 << 10));
}

int seq_bitmap_list(struct seq_file *m, const unsigned long *bits,
  unsigned int nr_bits);

static inline __attribute__((no_instrument_function)) int seq_cpumask_list(struct seq_file *m,
       const struct cpumask *mask)
{
 return seq_bitmap_list(m, ((mask)->bits), nr_cpu_ids);
}

static inline __attribute__((no_instrument_function)) int seq_nodemask_list(struct seq_file *m, nodemask_t *mask)
{
 return seq_bitmap_list(m, mask->bits, (1 << 10));
}

int single_open(struct file *, int (*)(struct seq_file *, void *), void *);
int single_open_size(struct file *, int (*)(struct seq_file *, void *), void *, size_t);
int single_release(struct inode *, struct file *);
void *__seq_open_private(struct file *, const struct seq_operations *, int);
int seq_open_private(struct file *, const struct seq_operations *, int);
int seq_release_private(struct inode *, struct file *);
int seq_put_decimal_ull(struct seq_file *m, char delimiter,
   unsigned long long num);
int seq_put_decimal_ll(struct seq_file *m, char delimiter,
   long long num);

static inline __attribute__((no_instrument_function)) struct user_namespace *seq_user_ns(struct seq_file *seq)
{

 return seq->user_ns;




}






extern struct list_head *seq_list_start(struct list_head *head,
  loff_t pos);
extern struct list_head *seq_list_start_head(struct list_head *head,
  loff_t pos);
extern struct list_head *seq_list_next(void *v, struct list_head *head,
  loff_t *ppos);





extern struct hlist_node *seq_hlist_start(struct hlist_head *head,
       loff_t pos);
extern struct hlist_node *seq_hlist_start_head(struct hlist_head *head,
            loff_t pos);
extern struct hlist_node *seq_hlist_next(void *v, struct hlist_head *head,
      loff_t *ppos);

extern struct hlist_node *seq_hlist_start_rcu(struct hlist_head *head,
           loff_t pos);
extern struct hlist_node *seq_hlist_start_head_rcu(struct hlist_head *head,
         loff_t pos);
extern struct hlist_node *seq_hlist_next_rcu(void *v,
         struct hlist_head *head,
         loff_t *ppos);


extern struct hlist_node *seq_hlist_start_percpu(struct hlist_head *head, int *cpu, loff_t pos);

extern struct hlist_node *seq_hlist_next_percpu(void *v, struct hlist_head *head, int *cpu, loff_t *pos);
# 18 "include/linux/pinctrl/consumer.h" 2
# 1 "include/linux/pinctrl/pinctrl-state.h" 1
# 19 "include/linux/pinctrl/consumer.h" 2


struct pinctrl;
struct pinctrl_state;
struct device;




extern int pinctrl_request_gpio(unsigned gpio);
extern void pinctrl_free_gpio(unsigned gpio);
extern int pinctrl_gpio_direction_input(unsigned gpio);
extern int pinctrl_gpio_direction_output(unsigned gpio);

extern struct pinctrl * __attribute__((warn_unused_result)) pinctrl_get(struct device *dev);
extern void pinctrl_put(struct pinctrl *p);
extern struct pinctrl_state * __attribute__((warn_unused_result)) pinctrl_lookup_state(
       struct pinctrl *p,
       const char *name);
extern int pinctrl_select_state(struct pinctrl *p, struct pinctrl_state *s);

extern struct pinctrl * __attribute__((warn_unused_result)) devm_pinctrl_get(struct device *dev);
extern void devm_pinctrl_put(struct pinctrl *p);


extern int pinctrl_pm_select_default_state(struct device *dev);
extern int pinctrl_pm_select_sleep_state(struct device *dev);
extern int pinctrl_pm_select_idle_state(struct device *dev);
# 131 "include/linux/pinctrl/consumer.h"
static inline __attribute__((no_instrument_function)) struct pinctrl * __attribute__((warn_unused_result)) pinctrl_get_select(
     struct device *dev, const char *name)
{
 struct pinctrl *p;
 struct pinctrl_state *s;
 int ret;

 p = pinctrl_get(dev);
 if (IS_ERR(p))
  return p;

 s = pinctrl_lookup_state(p, name);
 if (IS_ERR(s)) {
  pinctrl_put(p);
  return ERR_PTR(PTR_ERR(s));
 }

 ret = pinctrl_select_state(p, s);
 if (ret < 0) {
  pinctrl_put(p);
  return ERR_PTR(ret);
 }

 return p;
}

static inline __attribute__((no_instrument_function)) struct pinctrl * __attribute__((warn_unused_result)) pinctrl_get_select_default(
     struct device *dev)
{
 return pinctrl_get_select(dev, "default");
}

static inline __attribute__((no_instrument_function)) struct pinctrl * __attribute__((warn_unused_result)) devm_pinctrl_get_select(
     struct device *dev, const char *name)
{
 struct pinctrl *p;
 struct pinctrl_state *s;
 int ret;

 p = devm_pinctrl_get(dev);
 if (IS_ERR(p))
  return p;

 s = pinctrl_lookup_state(p, name);
 if (IS_ERR(s)) {
  devm_pinctrl_put(p);
  return ERR_CAST(s);
 }

 ret = pinctrl_select_state(p, s);
 if (ret < 0) {
  devm_pinctrl_put(p);
  return ERR_PTR(ret);
 }

 return p;
}

static inline __attribute__((no_instrument_function)) struct pinctrl * __attribute__((warn_unused_result)) devm_pinctrl_get_select_default(
     struct device *dev)
{
 return devm_pinctrl_get_select(dev, "default");
}
# 22 "include/linux/pinctrl/devinfo.h" 2






struct dev_pin_info {
 struct pinctrl *p;
 struct pinctrl_state *default_state;

 struct pinctrl_state *sleep_state;
 struct pinctrl_state *idle_state;

};

extern int pinctrl_bind_pins(struct device *dev);
# 25 "include/linux/device.h" 2
# 1 "include/linux/pm.h" 1
# 34 "include/linux/pm.h"
extern void (*pm_power_off)(void);
extern void (*pm_power_off_prepare)(void);

struct device;

extern void pm_vt_switch_required(struct device *dev, bool required);
extern void pm_vt_switch_unregister(struct device *dev);
# 54 "include/linux/pm.h"
struct device;


extern const char power_group_name[];




typedef struct pm_message {
 int event;
} pm_message_t;
# 276 "include/linux/pm.h"
struct dev_pm_ops {
 int (*prepare)(struct device *dev);
 void (*complete)(struct device *dev);
 int (*suspend)(struct device *dev);
 int (*resume)(struct device *dev);
 int (*freeze)(struct device *dev);
 int (*thaw)(struct device *dev);
 int (*poweroff)(struct device *dev);
 int (*restore)(struct device *dev);
 int (*suspend_late)(struct device *dev);
 int (*resume_early)(struct device *dev);
 int (*freeze_late)(struct device *dev);
 int (*thaw_early)(struct device *dev);
 int (*poweroff_late)(struct device *dev);
 int (*restore_early)(struct device *dev);
 int (*suspend_noirq)(struct device *dev);
 int (*resume_noirq)(struct device *dev);
 int (*freeze_noirq)(struct device *dev);
 int (*thaw_noirq)(struct device *dev);
 int (*poweroff_noirq)(struct device *dev);
 int (*restore_noirq)(struct device *dev);
 int (*runtime_suspend)(struct device *dev);
 int (*runtime_resume)(struct device *dev);
 int (*runtime_idle)(struct device *dev);
};
# 491 "include/linux/pm.h"
enum rpm_status {
 RPM_ACTIVE = 0,
 RPM_RESUMING,
 RPM_SUSPENDED,
 RPM_SUSPENDING,
};
# 513 "include/linux/pm.h"
enum rpm_request {
 RPM_REQ_NONE = 0,
 RPM_REQ_IDLE,
 RPM_REQ_SUSPEND,
 RPM_REQ_AUTOSUSPEND,
 RPM_REQ_RESUME,
};

struct wakeup_source;

struct pm_domain_data {
 struct list_head list_node;
 struct device *dev;
};

struct pm_subsys_data {
 spinlock_t lock;
 unsigned int refcount;

 struct list_head clock_list;




};

struct dev_pm_info {
 pm_message_t power_state;
 unsigned int can_wakeup:1;
 unsigned int async_suspend:1;
 bool is_prepared:1;
 bool is_suspended:1;
 bool is_noirq_suspended:1;
 bool is_late_suspended:1;
 bool ignore_children:1;
 bool early_init:1;
 spinlock_t lock;

 struct list_head entry;
 struct completion completion;
 struct wakeup_source *wakeup;
 bool wakeup_path:1;
 bool syscore:1;




 struct timer_list suspend_timer;
 unsigned long timer_expires;
 struct work_struct work;
 wait_queue_head_t wait_queue;
 atomic_t usage_count;
 atomic_t child_count;
 unsigned int disable_depth:3;
 unsigned int idle_notification:1;
 unsigned int request_pending:1;
 unsigned int deferred_resume:1;
 unsigned int run_wake:1;
 unsigned int runtime_auto:1;
 unsigned int no_callbacks:1;
 unsigned int irq_safe:1;
 unsigned int use_autosuspend:1;
 unsigned int timer_autosuspends:1;
 unsigned int memalloc_noio:1;
 enum rpm_request request;
 enum rpm_status runtime_status;
 int runtime_error;
 int autosuspend_delay;
 unsigned long last_busy;
 unsigned long active_jiffies;
 unsigned long suspended_jiffies;
 unsigned long accounting_timestamp;

 struct pm_subsys_data *subsys_data;
 void (*set_latency_tolerance)(struct device *, s32);
 struct dev_pm_qos *qos;
};

extern void update_pm_runtime_accounting(struct device *dev);
extern int dev_pm_get_subsys_data(struct device *dev);
extern int dev_pm_put_subsys_data(struct device *dev);






struct dev_pm_domain {
 struct dev_pm_ops ops;
};
# 659 "include/linux/pm.h"
extern void device_pm_lock(void);
extern void dpm_resume_start(pm_message_t state);
extern void dpm_resume_end(pm_message_t state);
extern void dpm_resume(pm_message_t state);
extern void dpm_complete(pm_message_t state);

extern void device_pm_unlock(void);
extern int dpm_suspend_end(pm_message_t state);
extern int dpm_suspend_start(pm_message_t state);
extern int dpm_suspend(pm_message_t state);
extern int dpm_prepare(pm_message_t state);

extern void __suspend_report_result(const char *function, void *fn, int ret);






extern int device_pm_wait_for_dev(struct device *sub, struct device *dev);
extern void dpm_for_each_dev(void *data, void (*fn)(struct device *, void *));

extern int pm_generic_prepare(struct device *dev);
extern int pm_generic_suspend_late(struct device *dev);
extern int pm_generic_suspend_noirq(struct device *dev);
extern int pm_generic_suspend(struct device *dev);
extern int pm_generic_resume_early(struct device *dev);
extern int pm_generic_resume_noirq(struct device *dev);
extern int pm_generic_resume(struct device *dev);
extern int pm_generic_freeze_noirq(struct device *dev);
extern int pm_generic_freeze_late(struct device *dev);
extern int pm_generic_freeze(struct device *dev);
extern int pm_generic_thaw_noirq(struct device *dev);
extern int pm_generic_thaw_early(struct device *dev);
extern int pm_generic_thaw(struct device *dev);
extern int pm_generic_restore_noirq(struct device *dev);
extern int pm_generic_restore_early(struct device *dev);
extern int pm_generic_restore(struct device *dev);
extern int pm_generic_poweroff_noirq(struct device *dev);
extern int pm_generic_poweroff_late(struct device *dev);
extern int pm_generic_poweroff(struct device *dev);
extern void pm_generic_complete(struct device *dev);
# 746 "include/linux/pm.h"
enum dpm_order {
 DPM_ORDER_NONE,
 DPM_ORDER_DEV_AFTER_PARENT,
 DPM_ORDER_PARENT_BEFORE_DEV,
 DPM_ORDER_DEV_LAST,
};
# 26 "include/linux/device.h" 2

# 1 "include/linux/ratelimit.h" 1
# 10 "include/linux/ratelimit.h"
struct ratelimit_state {
 raw_spinlock_t lock;

 int interval;
 int burst;
 int printed;
 int missed;
 unsigned long begin;
};
# 28 "include/linux/ratelimit.h"
static inline __attribute__((no_instrument_function)) void ratelimit_state_init(struct ratelimit_state *rs,
     int interval, int burst)
{
 do { static struct lock_class_key __key; __raw_spin_lock_init((&rs->lock), "&rs->lock", &__key); } while (0);
 rs->interval = interval;
 rs->burst = burst;
 rs->printed = 0;
 rs->missed = 0;
 rs->begin = 0;
}

extern struct ratelimit_state printk_ratelimit_state;

extern int ___ratelimit(struct ratelimit_state *rs, const char *func);
# 28 "include/linux/device.h" 2

# 1 "include/linux/gfp.h" 1



# 1 "include/linux/mmdebug.h" 1



struct page;

extern void dump_page(struct page *page, const char *reason);
extern void dump_page_badflags(struct page *page, const char *reason,
          unsigned long badflags);
# 5 "include/linux/gfp.h" 2
# 1 "include/linux/mmzone.h" 1
# 17 "include/linux/mmzone.h"
# 1 "include/linux/pageblock-flags.h" 1
# 29 "include/linux/pageblock-flags.h"
enum pageblock_bits {
 PB_migrate,
 PB_migrate_end = PB_migrate + 3 - 1,

 PB_migrate_skip,





 NR_PAGEBLOCK_BITS
};
# 66 "include/linux/pageblock-flags.h"
struct page;

unsigned long get_pageblock_flags_mask(struct page *page,
    unsigned long end_bitidx,
    unsigned long mask);
void set_pageblock_flags_mask(struct page *page,
    unsigned long flags,
    unsigned long end_bitidx,
    unsigned long mask);


static inline __attribute__((no_instrument_function)) unsigned long get_pageblock_flags_group(struct page *page,
     int start_bitidx, int end_bitidx)
{
 unsigned long nr_flag_bits = end_bitidx - start_bitidx + 1;
 unsigned long mask = (1 << nr_flag_bits) - 1;

 return get_pageblock_flags_mask(page, end_bitidx, mask);
}

static inline __attribute__((no_instrument_function)) void set_pageblock_flags_group(struct page *page,
     unsigned long flags,
     int start_bitidx, int end_bitidx)
{
 unsigned long nr_flag_bits = end_bitidx - start_bitidx + 1;
 unsigned long mask = (1 << nr_flag_bits) - 1;

 set_pageblock_flags_mask(page, flags, end_bitidx, mask);
}
# 18 "include/linux/mmzone.h" 2
# 1 "include/linux/page-flags-layout.h" 1




# 1 "include/generated/bounds.h" 1
# 6 "include/linux/page-flags-layout.h" 2
# 19 "include/linux/mmzone.h" 2
# 38 "include/linux/mmzone.h"
enum {
 MIGRATE_UNMOVABLE,
 MIGRATE_RECLAIMABLE,
 MIGRATE_MOVABLE,
 MIGRATE_PCPTYPES,
 MIGRATE_RESERVE = MIGRATE_PCPTYPES,
# 58 "include/linux/mmzone.h"
 MIGRATE_CMA,


 MIGRATE_ISOLATE,

 MIGRATE_TYPES
};
# 76 "include/linux/mmzone.h"
extern int page_group_by_mobility_disabled;




static inline __attribute__((no_instrument_function)) int get_pageblock_migratetype(struct page *page)
{
 ;
 return get_pageblock_flags_mask(page, PB_migrate_end, ((1UL << (PB_migrate_end - PB_migrate + 1)) - 1));
}

struct free_area {
 struct list_head free_list[MIGRATE_TYPES];
 unsigned long nr_free;
};

struct pglist_data;
# 101 "include/linux/mmzone.h"
struct zone_padding {
 char x[0];
} __attribute__((__aligned__(1 << (12))));





enum zone_stat_item {

 NR_FREE_PAGES,
 NR_ALLOC_BATCH,
 NR_LRU_BASE,
 NR_INACTIVE_ANON = NR_LRU_BASE,
 NR_ACTIVE_ANON,
 NR_INACTIVE_FILE,
 NR_ACTIVE_FILE,
 NR_UNEVICTABLE,
 NR_MLOCK,
 NR_ANON_PAGES,
 NR_FILE_MAPPED,

 NR_FILE_PAGES,
 NR_FILE_DIRTY,
 NR_WRITEBACK,
 NR_SLAB_RECLAIMABLE,
 NR_SLAB_UNRECLAIMABLE,
 NR_PAGETABLE,
 NR_KERNEL_STACK,

 NR_UNSTABLE_NFS,
 NR_BOUNCE,
 NR_VMSCAN_WRITE,
 NR_VMSCAN_IMMEDIATE,
 NR_WRITEBACK_TEMP,
 NR_ISOLATED_ANON,
 NR_ISOLATED_FILE,
 NR_SHMEM,
 NR_DIRTIED,
 NR_WRITTEN,

 NUMA_HIT,
 NUMA_MISS,
 NUMA_FOREIGN,
 NUMA_INTERLEAVE_HIT,
 NUMA_LOCAL,
 NUMA_OTHER,

 WORKINGSET_REFAULT,
 WORKINGSET_ACTIVATE,
 WORKINGSET_NODERECLAIM,
 NR_ANON_TRANSPARENT_HUGEPAGES,
 NR_FREE_CMA_PAGES,
 NR_VM_ZONE_STAT_ITEMS };
# 169 "include/linux/mmzone.h"
enum lru_list {
 LRU_INACTIVE_ANON = 0,
 LRU_ACTIVE_ANON = 0 + 1,
 LRU_INACTIVE_FILE = 0 + 2,
 LRU_ACTIVE_FILE = 0 + 2 + 1,
 LRU_UNEVICTABLE,
 NR_LRU_LISTS
};





static inline __attribute__((no_instrument_function)) int is_file_lru(enum lru_list lru)
{
 return (lru == LRU_INACTIVE_FILE || lru == LRU_ACTIVE_FILE);
}

static inline __attribute__((no_instrument_function)) int is_active_lru(enum lru_list lru)
{
 return (lru == LRU_ACTIVE_ANON || lru == LRU_ACTIVE_FILE);
}

static inline __attribute__((no_instrument_function)) int is_unevictable_lru(enum lru_list lru)
{
 return (lru == LRU_UNEVICTABLE);
}

struct zone_reclaim_stat {
# 206 "include/linux/mmzone.h"
 unsigned long recent_rotated[2];
 unsigned long recent_scanned[2];
};

struct lruvec {
 struct list_head lists[NR_LRU_LISTS];
 struct zone_reclaim_stat reclaim_stat;

 struct zone *zone;

};
# 233 "include/linux/mmzone.h"
typedef unsigned isolate_mode_t;

enum zone_watermarks {
 WMARK_MIN,
 WMARK_LOW,
 WMARK_HIGH,
 NR_WMARK
};





struct per_cpu_pages {
 int count;
 int high;
 int batch;


 struct list_head lists[MIGRATE_PCPTYPES];
};

struct per_cpu_pageset {
 struct per_cpu_pages pcp;

 s8 expire;


 s8 stat_threshold;
 s8 vm_stat_diff[NR_VM_ZONE_STAT_ITEMS];

};



enum zone_type {
# 288 "include/linux/mmzone.h"
 ZONE_DMA,







 ZONE_DMA32,






 ZONE_NORMAL,
# 315 "include/linux/mmzone.h"
 ZONE_MOVABLE,
 __MAX_NR_ZONES
};



struct zone {



 unsigned long watermark[NR_WMARK];






 unsigned long percpu_drift_mark;
# 342 "include/linux/mmzone.h"
 unsigned long lowmem_reserve[4];





 unsigned long dirty_balance_reserve;


 int node;



 unsigned long min_unmapped_pages;
 unsigned long min_slab_pages;

 struct per_cpu_pageset *pageset;



 spinlock_t lock;


 bool compact_blockskip_flush;


 unsigned long compact_cached_free_pfn;
 unsigned long compact_cached_migrate_pfn;



 seqlock_t span_seqlock;

 struct free_area free_area[11];
# 391 "include/linux/mmzone.h"
 unsigned int compact_considered;
 unsigned int compact_defer_shift;
 int compact_order_failed;


 struct zone_padding _pad1_;


 spinlock_t lru_lock;
 struct lruvec lruvec;


 atomic_long_t inactive_age;

 unsigned long pages_scanned;
 unsigned long flags;


 atomic_long_t vm_stat[NR_VM_ZONE_STAT_ITEMS];





 unsigned int inactive_ratio;


 struct zone_padding _pad2_;
# 445 "include/linux/mmzone.h"
 wait_queue_head_t * wait_table;
 unsigned long wait_table_hash_nr_entries;
 unsigned long wait_table_bits;




 struct pglist_data *zone_pgdat;

 unsigned long zone_start_pfn;
# 498 "include/linux/mmzone.h"
 unsigned long spanned_pages;
 unsigned long present_pages;
 unsigned long managed_pages;





 int nr_migrate_reserve_block;




 const char *name;
} __attribute__((__aligned__(1 << (12))));

typedef enum {
 ZONE_RECLAIM_LOCKED,
 ZONE_OOM_LOCKED,
 ZONE_CONGESTED,


 ZONE_TAIL_LRU_DIRTY,



 ZONE_WRITEBACK,


} zone_flags_t;

static inline __attribute__((no_instrument_function)) void zone_set_flag(struct zone *zone, zone_flags_t flag)
{
 set_bit(flag, &zone->flags);
}

static inline __attribute__((no_instrument_function)) int zone_test_and_set_flag(struct zone *zone, zone_flags_t flag)
{
 return test_and_set_bit(flag, &zone->flags);
}

static inline __attribute__((no_instrument_function)) void zone_clear_flag(struct zone *zone, zone_flags_t flag)
{
 clear_bit(flag, &zone->flags);
}

static inline __attribute__((no_instrument_function)) int zone_is_reclaim_congested(const struct zone *zone)
{
 return (__builtin_constant_p((ZONE_CONGESTED)) ? constant_test_bit((ZONE_CONGESTED), (&zone->flags)) : variable_test_bit((ZONE_CONGESTED), (&zone->flags)));
}

static inline __attribute__((no_instrument_function)) int zone_is_reclaim_dirty(const struct zone *zone)
{
 return (__builtin_constant_p((ZONE_TAIL_LRU_DIRTY)) ? constant_test_bit((ZONE_TAIL_LRU_DIRTY), (&zone->flags)) : variable_test_bit((ZONE_TAIL_LRU_DIRTY), (&zone->flags)));
}

static inline __attribute__((no_instrument_function)) int zone_is_reclaim_writeback(const struct zone *zone)
{
 return (__builtin_constant_p((ZONE_WRITEBACK)) ? constant_test_bit((ZONE_WRITEBACK), (&zone->flags)) : variable_test_bit((ZONE_WRITEBACK), (&zone->flags)));
}

static inline __attribute__((no_instrument_function)) int zone_is_reclaim_locked(const struct zone *zone)
{
 return (__builtin_constant_p((ZONE_RECLAIM_LOCKED)) ? constant_test_bit((ZONE_RECLAIM_LOCKED), (&zone->flags)) : variable_test_bit((ZONE_RECLAIM_LOCKED), (&zone->flags)));
}

static inline __attribute__((no_instrument_function)) int zone_is_oom_locked(const struct zone *zone)
{
 return (__builtin_constant_p((ZONE_OOM_LOCKED)) ? constant_test_bit((ZONE_OOM_LOCKED), (&zone->flags)) : variable_test_bit((ZONE_OOM_LOCKED), (&zone->flags)));
}

static inline __attribute__((no_instrument_function)) unsigned long zone_end_pfn(const struct zone *zone)
{
 return zone->zone_start_pfn + zone->spanned_pages;
}

static inline __attribute__((no_instrument_function)) bool zone_spans_pfn(const struct zone *zone, unsigned long pfn)
{
 return zone->zone_start_pfn <= pfn && pfn < zone_end_pfn(zone);
}

static inline __attribute__((no_instrument_function)) bool zone_is_initialized(struct zone *zone)
{
 return !!zone->wait_table;
}

static inline __attribute__((no_instrument_function)) bool zone_is_empty(struct zone *zone)
{
 return zone->spanned_pages == 0;
}
# 670 "include/linux/mmzone.h"
struct zonelist_cache {
 unsigned short z_to_n[((1 << 10) * 4)];
 unsigned long fullzones[(((((1 << 10) * 4)) + (8 * sizeof(long)) - 1) / (8 * sizeof(long)))];
 unsigned long last_full_zap;
};
# 684 "include/linux/mmzone.h"
struct zoneref {
 struct zone *zone;
 int zone_idx;
};
# 706 "include/linux/mmzone.h"
struct zonelist {
 struct zonelist_cache *zlcache_ptr;
 struct zoneref _zonerefs[((1 << 10) * 4) + 1];

 struct zonelist_cache zlcache;

};


struct node_active_region {
 unsigned long start_pfn;
 unsigned long end_pfn;
 int nid;
};




extern struct page *mem_map;
# 738 "include/linux/mmzone.h"
struct bootmem_data;
typedef struct pglist_data {
 struct zone node_zones[4];
 struct zonelist node_zonelists[2];
 int nr_zones;
# 763 "include/linux/mmzone.h"
 spinlock_t node_size_lock;

 unsigned long node_start_pfn;
 unsigned long node_present_pages;
 unsigned long node_spanned_pages;

 int node_id;
 nodemask_t reclaim_nodes;
 wait_queue_head_t kswapd_wait;
 wait_queue_head_t pfmemalloc_wait;
 struct task_struct *kswapd;
 int kswapd_max_order;
 enum zone_type classzone_idx;


 spinlock_t numabalancing_migrate_lock;


 unsigned long numabalancing_migrate_next_window;


 unsigned long numabalancing_migrate_nr_pages;

} pg_data_t;
# 800 "include/linux/mmzone.h"
static inline __attribute__((no_instrument_function)) unsigned long pgdat_end_pfn(pg_data_t *pgdat)
{
 return pgdat->node_start_pfn + pgdat->node_spanned_pages;
}

static inline __attribute__((no_instrument_function)) bool pgdat_is_empty(pg_data_t *pgdat)
{
 return !pgdat->node_start_pfn && !pgdat->node_spanned_pages;
}

# 1 "include/linux/memory_hotplug.h" 1



# 1 "include/linux/mmzone.h" 1
# 5 "include/linux/memory_hotplug.h" 2

# 1 "include/linux/notifier.h" 1
# 14 "include/linux/notifier.h"
# 1 "include/linux/rwsem.h" 1
# 19 "include/linux/rwsem.h"
struct rw_semaphore;





struct rw_semaphore {
 long count;
 raw_spinlock_t wait_lock;
 struct list_head wait_list;

 struct lockdep_map dep_map;

};

extern struct rw_semaphore *rwsem_down_read_failed(struct rw_semaphore *sem);
extern struct rw_semaphore *rwsem_down_write_failed(struct rw_semaphore *sem);
extern struct rw_semaphore *rwsem_wake(struct rw_semaphore *);
extern struct rw_semaphore *rwsem_downgrade_wake(struct rw_semaphore *sem);


# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/rwsem.h" 1
# 63 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/rwsem.h"
static inline __attribute__((no_instrument_function)) void __down_read(struct rw_semaphore *sem)
{
 asm volatile("# beginning down_read\n\t"
       ".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " " " "incq" " " "(%1)\n\t"

       "  jns        1f\n"
       "  call call_rwsem_down_read_failed\n"
       "1:\n\t"
       "# ending down_read\n\t"
       : "+m" (sem->count)
       : "a" (sem)
       : "memory", "cc");
}




static inline __attribute__((no_instrument_function)) int __down_read_trylock(struct rw_semaphore *sem)
{
 long result, tmp;
 asm volatile("# beginning __down_read_trylock\n\t"
       "  mov          %0,%1\n\t"
       "1:\n\t"
       "  mov          %1,%2\n\t"
       "  add          %3,%2\n\t"
       "  jle	     2f\n\t"
       ".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "  cmpxchg  %2,%0\n\t"
       "  jnz	     1b\n\t"
       "2:\n\t"
       "# ending __down_read_trylock\n\t"
       : "+m" (sem->count), "=&a" (result), "=&r" (tmp)
       : "i" (0x00000001L)
       : "memory", "cc");
 return result >= 0 ? 1 : 0;
}




static inline __attribute__((no_instrument_function)) void __down_write_nested(struct rw_semaphore *sem, int subclass)
{
 long tmp;
 asm volatile("# beginning down_write\n\t"
       ".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "  xadd      %1,(%2)\n\t"

       "  test " " " "%k1" " " "," " " "%k1" " " "\n\t"

       "  jz        1f\n"
       "  call call_rwsem_down_write_failed\n"
       "1:\n"
       "# ending down_write"
       : "+m" (sem->count), "=d" (tmp)
       : "a" (sem), "1" (((-0xffffffffL -1) + 0x00000001L))
       : "memory", "cc");
}

static inline __attribute__((no_instrument_function)) void __down_write(struct rw_semaphore *sem)
{
 __down_write_nested(sem, 0);
}




static inline __attribute__((no_instrument_function)) int __down_write_trylock(struct rw_semaphore *sem)
{
 long result, tmp;
 asm volatile("# beginning __down_write_trylock\n\t"
       "  mov          %0,%1\n\t"
       "1:\n\t"
       "  test " " " "%k1" " " "," " " "%k1" " " "\n\t"

       "  jnz          2f\n\t"
       "  mov          %1,%2\n\t"
       "  add          %3,%2\n\t"
       ".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "  cmpxchg  %2,%0\n\t"
       "  jnz	     1b\n\t"
       "2:\n\t"
       "  sete         %b1\n\t"
       "  movzbl       %b1, %k1\n\t"
       "# ending __down_write_trylock\n\t"
       : "+m" (sem->count), "=&a" (result), "=&r" (tmp)
       : "er" (((-0xffffffffL -1) + 0x00000001L))
       : "memory", "cc");
 return result;
}




static inline __attribute__((no_instrument_function)) void __up_read(struct rw_semaphore *sem)
{
 long tmp;
 asm volatile("# beginning __up_read\n\t"
       ".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "  xadd      %1,(%2)\n\t"

       "  jns        1f\n\t"
       "  call call_rwsem_wake\n"
       "1:\n"
       "# ending __up_read\n"
       : "+m" (sem->count), "=d" (tmp)
       : "a" (sem), "1" (-0x00000001L)
       : "memory", "cc");
}




static inline __attribute__((no_instrument_function)) void __up_write(struct rw_semaphore *sem)
{
 long tmp;
 asm volatile("# beginning __up_write\n\t"
       ".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "  xadd      %1,(%2)\n\t"

       "  jns        1f\n\t"
       "  call call_rwsem_wake\n"
       "1:\n\t"
       "# ending __up_write\n"
       : "+m" (sem->count), "=d" (tmp)
       : "a" (sem), "1" (-((-0xffffffffL -1) + 0x00000001L))
       : "memory", "cc");
}




static inline __attribute__((no_instrument_function)) void __downgrade_write(struct rw_semaphore *sem)
{
 asm volatile("# beginning __downgrade_write\n\t"
       ".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " " " "addq" " " "%2,(%1)\n\t"




       "  jns       1f\n\t"
       "  call call_rwsem_downgrade_wake\n"
       "1:\n\t"
       "# ending __downgrade_write\n"
       : "+m" (sem->count)
       : "a" (sem), "er" (-(-0xffffffffL -1))
       : "memory", "cc");
}




static inline __attribute__((no_instrument_function)) void rwsem_atomic_add(long delta, struct rw_semaphore *sem)
{
 asm volatile(".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " " " "addq" " " "%1,%0"
       : "+m" (sem->count)
       : "er" (delta));
}




static inline __attribute__((no_instrument_function)) long rwsem_atomic_update(long delta, struct rw_semaphore *sem)
{
 return delta + ({ __typeof__ (*(((&sem->count)))) __ret = (((delta))); switch (sizeof(*(((&sem->count))))) { case 1: asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "xadd" "b %b0, %1\n" : "+q" (__ret), "+m" (*(((&sem->count)))) : : "memory", "cc"); break; case 2: asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "xadd" "w %w0, %1\n" : "+r" (__ret), "+m" (*(((&sem->count)))) : : "memory", "cc"); break; case 4: asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "xadd" "l %0, %1\n" : "+r" (__ret), "+m" (*(((&sem->count)))) : : "memory", "cc"); break; case 8: asm volatile (".pushsection .smp_locks,\"a\"\n" ".balign 4\n" ".long 671f - .\n" ".popsection\n" "671:" "\n\tlock; " "xadd" "q %q0, %1\n" : "+r" (__ret), "+m" (*(((&sem->count)))) : : "memory", "cc"); break; default: __xadd_wrong_size(); } __ret; });
}
# 41 "include/linux/rwsem.h" 2


static inline __attribute__((no_instrument_function)) int rwsem_is_locked(struct rw_semaphore *sem)
{
 return sem->count != 0;
}
# 67 "include/linux/rwsem.h"
extern void __init_rwsem(struct rw_semaphore *sem, const char *name,
    struct lock_class_key *key);
# 83 "include/linux/rwsem.h"
static inline __attribute__((no_instrument_function)) int rwsem_is_contended(struct rw_semaphore *sem)
{
 return !list_empty(&sem->wait_list);
}




extern void down_read(struct rw_semaphore *sem);




extern int down_read_trylock(struct rw_semaphore *sem);




extern void down_write(struct rw_semaphore *sem);




extern int down_write_trylock(struct rw_semaphore *sem);




extern void up_read(struct rw_semaphore *sem);




extern void up_write(struct rw_semaphore *sem);




extern void downgrade_write(struct rw_semaphore *sem);
# 137 "include/linux/rwsem.h"
extern void down_read_nested(struct rw_semaphore *sem, int subclass);
extern void down_write_nested(struct rw_semaphore *sem, int subclass);
extern void _down_write_nest_lock(struct rw_semaphore *sem, struct lockdep_map *nest_lock);
# 153 "include/linux/rwsem.h"
extern void down_read_non_owner(struct rw_semaphore *sem);
extern void up_read_non_owner(struct rw_semaphore *sem);
# 15 "include/linux/notifier.h" 2
# 1 "include/linux/srcu.h" 1
# 36 "include/linux/srcu.h"
struct srcu_struct_array {
 unsigned long c[2];
 unsigned long seq[2];
};

struct rcu_batch {
 struct callback_head *head, **tail;
};



struct srcu_struct {
 unsigned completed;
 struct srcu_struct_array *per_cpu_ref;
 spinlock_t queue_lock;
 bool running;

 struct rcu_batch batch_queue;

 struct rcu_batch batch_check0;

 struct rcu_batch batch_check1;
 struct rcu_batch batch_done;
 struct delayed_work work;

 struct lockdep_map dep_map;

};



int __init_srcu_struct(struct srcu_struct *sp, const char *name,
         struct lock_class_key *key);
# 85 "include/linux/srcu.h"
void process_srcu(struct work_struct *work);
# 130 "include/linux/srcu.h"
void call_srcu(struct srcu_struct *sp, struct callback_head *head,
  void (*func)(struct callback_head *head));

void cleanup_srcu_struct(struct srcu_struct *sp);
int __srcu_read_lock(struct srcu_struct *sp) ;
void __srcu_read_unlock(struct srcu_struct *sp, int idx) ;
void synchronize_srcu(struct srcu_struct *sp);
void synchronize_srcu_expedited(struct srcu_struct *sp);
long srcu_batches_completed(struct srcu_struct *sp);
void srcu_barrier(struct srcu_struct *sp);
# 158 "include/linux/srcu.h"
static inline __attribute__((no_instrument_function)) int srcu_read_lock_held(struct srcu_struct *sp)
{
 if (!debug_lockdep_rcu_enabled())
  return 1;
 return lock_is_held(&sp->dep_map);
}
# 218 "include/linux/srcu.h"
static inline __attribute__((no_instrument_function)) int srcu_read_lock(struct srcu_struct *sp)
{
 int retval = __srcu_read_lock(sp);

 rcu_lock_acquire(&(sp)->dep_map);
 return retval;
}
# 233 "include/linux/srcu.h"
static inline __attribute__((no_instrument_function)) void srcu_read_unlock(struct srcu_struct *sp, int idx)

{
 rcu_lock_release(&(sp)->dep_map);
 __srcu_read_unlock(sp, idx);
}
# 249 "include/linux/srcu.h"
static inline __attribute__((no_instrument_function)) void smp_mb__after_srcu_read_unlock(void)
{

}
# 16 "include/linux/notifier.h" 2
# 50 "include/linux/notifier.h"
typedef int (*notifier_fn_t)(struct notifier_block *nb,
   unsigned long action, void *data);

struct notifier_block {
 notifier_fn_t notifier_call;
 struct notifier_block *next;
 int priority;
};

struct atomic_notifier_head {
 spinlock_t lock;
 struct notifier_block *head;
};

struct blocking_notifier_head {
 struct rw_semaphore rwsem;
 struct notifier_block *head;
};

struct raw_notifier_head {
 struct notifier_block *head;
};

struct srcu_notifier_head {
 struct mutex mutex;
 struct srcu_struct srcu;
 struct notifier_block *head;
};
# 92 "include/linux/notifier.h"
extern void srcu_init_notifier_head(struct srcu_notifier_head *nh);
# 118 "include/linux/notifier.h"
extern int atomic_notifier_chain_register(struct atomic_notifier_head *nh,
  struct notifier_block *nb);
extern int blocking_notifier_chain_register(struct blocking_notifier_head *nh,
  struct notifier_block *nb);
extern int raw_notifier_chain_register(struct raw_notifier_head *nh,
  struct notifier_block *nb);
extern int srcu_notifier_chain_register(struct srcu_notifier_head *nh,
  struct notifier_block *nb);

extern int blocking_notifier_chain_cond_register(
  struct blocking_notifier_head *nh,
  struct notifier_block *nb);

extern int atomic_notifier_chain_unregister(struct atomic_notifier_head *nh,
  struct notifier_block *nb);
extern int blocking_notifier_chain_unregister(struct blocking_notifier_head *nh,
  struct notifier_block *nb);
extern int raw_notifier_chain_unregister(struct raw_notifier_head *nh,
  struct notifier_block *nb);
extern int srcu_notifier_chain_unregister(struct srcu_notifier_head *nh,
  struct notifier_block *nb);

extern int atomic_notifier_call_chain(struct atomic_notifier_head *nh,
  unsigned long val, void *v);
extern int __atomic_notifier_call_chain(struct atomic_notifier_head *nh,
 unsigned long val, void *v, int nr_to_call, int *nr_calls);
extern int blocking_notifier_call_chain(struct blocking_notifier_head *nh,
  unsigned long val, void *v);
extern int __blocking_notifier_call_chain(struct blocking_notifier_head *nh,
 unsigned long val, void *v, int nr_to_call, int *nr_calls);
extern int raw_notifier_call_chain(struct raw_notifier_head *nh,
  unsigned long val, void *v);
extern int __raw_notifier_call_chain(struct raw_notifier_head *nh,
 unsigned long val, void *v, int nr_to_call, int *nr_calls);
extern int srcu_notifier_call_chain(struct srcu_notifier_head *nh,
  unsigned long val, void *v);
extern int __srcu_notifier_call_chain(struct srcu_notifier_head *nh,
 unsigned long val, void *v, int nr_to_call, int *nr_calls);
# 168 "include/linux/notifier.h"
static inline __attribute__((no_instrument_function)) int notifier_from_errno(int err)
{
 if (err)
  return 0x8000 | (0x0001 - err);

 return 0x0001;
}


static inline __attribute__((no_instrument_function)) int notifier_to_errno(int ret)
{
 ret &= ~0x8000;
 return ret > 0x0001 ? 0x0001 - ret : 0;
}
# 212 "include/linux/notifier.h"
extern struct blocking_notifier_head reboot_notifier_list;
# 7 "include/linux/memory_hotplug.h" 2


struct page;
struct zone;
struct pglist_data;
struct mem_section;
struct memory_block;







enum {
 MEMORY_HOTPLUG_MIN_BOOTMEM_TYPE = 12,
 SECTION_INFO = MEMORY_HOTPLUG_MIN_BOOTMEM_TYPE,
 MIX_SECTION_INFO,
 NODE_INFO,
 MEMORY_HOTPLUG_MAX_BOOTMEM_TYPE = NODE_INFO,
};


enum {
 ONLINE_KEEP,
 ONLINE_KERNEL,
 ONLINE_MOVABLE,
};




static inline __attribute__((no_instrument_function))
void pgdat_resize_lock(struct pglist_data *pgdat, unsigned long *flags)
{
 do { do { ({ unsigned long __dummy; typeof(*flags) __dummy2; (void)(&__dummy == &__dummy2); 1; }); *flags = _raw_spin_lock_irqsave(spinlock_check(&pgdat->node_size_lock)); } while (0); } while (0);
}
static inline __attribute__((no_instrument_function))
void pgdat_resize_unlock(struct pglist_data *pgdat, unsigned long *flags)
{
 spin_unlock_irqrestore(&pgdat->node_size_lock, *flags);
}
static inline __attribute__((no_instrument_function))
void pgdat_resize_init(struct pglist_data *pgdat)
{
 do { spinlock_check(&pgdat->node_size_lock); do { static struct lock_class_key __key; __raw_spin_lock_init((&(&pgdat->node_size_lock)->rlock), "&(&pgdat->node_size_lock)->rlock", &__key); } while (0); } while (0);
}







static inline __attribute__((no_instrument_function)) unsigned zone_span_seqbegin(struct zone *zone)
{
 return read_seqbegin(&zone->span_seqlock);
}
static inline __attribute__((no_instrument_function)) int zone_span_seqretry(struct zone *zone, unsigned iv)
{
 return read_seqretry(&zone->span_seqlock, iv);
}
static inline __attribute__((no_instrument_function)) void zone_span_writelock(struct zone *zone)
{
 write_seqlock(&zone->span_seqlock);
}
static inline __attribute__((no_instrument_function)) void zone_span_writeunlock(struct zone *zone)
{
 write_sequnlock(&zone->span_seqlock);
}
static inline __attribute__((no_instrument_function)) void zone_seqlock_init(struct zone *zone)
{
 do { do { static struct lock_class_key __key; __seqcount_init((&(&zone->span_seqlock)->seqcount), "&(&zone->span_seqlock)->seqcount", &__key); } while (0); do { spinlock_check(&(&zone->span_seqlock)->lock); do { static struct lock_class_key __key; __raw_spin_lock_init((&(&(&zone->span_seqlock)->lock)->rlock), "&(&(&zone->span_seqlock)->lock)->rlock", &__key); } while (0); } while (0); } while (0);
}
extern int zone_grow_free_lists(struct zone *zone, unsigned long new_nr_pages);
extern int zone_grow_waitqueues(struct zone *zone, unsigned long nr_pages);
extern int add_one_highpage(struct page *page, int pfn, int bad_ppro);

extern int online_pages(unsigned long, unsigned long, int);
extern void __offline_isolated_pages(unsigned long, unsigned long);

typedef void (*online_page_callback_t)(struct page *page);

extern int set_online_page_callback(online_page_callback_t callback);
extern int restore_online_page_callback(online_page_callback_t callback);

extern void __online_page_set_limits(struct page *page);
extern void __online_page_increment_counters(struct page *page);
extern void __online_page_free(struct page *page);

extern int try_online_node(int nid);


extern bool is_pageblock_removable_nolock(struct page *page);
extern int arch_remove_memory(u64 start, u64 size);
extern int __remove_pages(struct zone *zone, unsigned long start_pfn,
 unsigned long nr_pages);



extern int __add_pages(int nid, struct zone *zone, unsigned long start_pfn,
 unsigned long nr_pages);


extern int memory_add_physaddr_to_nid(u64 start);
# 156 "include/linux/memory_hotplug.h"
extern pg_data_t *node_data[];
static inline __attribute__((no_instrument_function)) void arch_refresh_nodedata(int nid, pg_data_t *pgdat)
{
 node_data[nid] = pgdat;
}
# 180 "include/linux/memory_hotplug.h"
extern void register_page_bootmem_info_node(struct pglist_data *pgdat);





extern void put_page_bootmem(struct page *page);
extern void get_page_bootmem(unsigned long ingo, struct page *page,
        unsigned long type);







void lock_memory_hotplug(void);
void unlock_memory_hotplug(void);
# 242 "include/linux/memory_hotplug.h"
extern int is_mem_section_removable(unsigned long pfn, unsigned long nr_pages);
extern void try_offline_node(int nid);
extern int offline_pages(unsigned long start_pfn, unsigned long nr_pages);
extern void remove_memory(int nid, u64 start, u64 size);
# 264 "include/linux/memory_hotplug.h"
extern int walk_memory_range(unsigned long start_pfn, unsigned long end_pfn,
  void *arg, int (*func)(struct memory_block *, void *));
extern int add_memory(int nid, u64 start, u64 size);
extern int arch_add_memory(int nid, u64 start, u64 size);
extern int offline_pages(unsigned long start_pfn, unsigned long nr_pages);
extern bool is_memblock_offlined(struct memory_block *mem);
extern void remove_memory(int nid, u64 start, u64 size);
extern int sparse_add_one_section(struct zone *zone, unsigned long start_pfn);
extern void sparse_remove_one_section(struct zone *zone, struct mem_section *ms);
extern struct page *sparse_decode_mem_map(unsigned long coded_mem_map,
       unsigned long pnum);
# 811 "include/linux/mmzone.h" 2

extern struct mutex zonelists_mutex;
void build_all_zonelists(pg_data_t *pgdat, struct zone *zone);
void wakeup_kswapd(struct zone *zone, int order, enum zone_type classzone_idx);
bool zone_watermark_ok(struct zone *z, int order, unsigned long mark,
  int classzone_idx, int alloc_flags);
bool zone_watermark_ok_safe(struct zone *z, int order, unsigned long mark,
  int classzone_idx, int alloc_flags);
enum memmap_context {
 MEMMAP_EARLY,
 MEMMAP_HOTPLUG,
};
extern int init_currently_empty_zone(struct zone *zone, unsigned long start_pfn,
         unsigned long size,
         enum memmap_context context);

extern void lruvec_init(struct lruvec *lruvec);

static inline __attribute__((no_instrument_function)) struct zone *lruvec_zone(struct lruvec *lruvec)
{

 return lruvec->zone;



}


void memory_present(int nid, unsigned long start, unsigned long end);







static inline __attribute__((no_instrument_function)) int local_memory_node(int node_id) { return node_id; };
# 859 "include/linux/mmzone.h"
static inline __attribute__((no_instrument_function)) int populated_zone(struct zone *zone)
{
 return (!!zone->present_pages);
}

extern int movable_zone;

static inline __attribute__((no_instrument_function)) int zone_movable_is_highmem(void)
{



 return 0;

}

static inline __attribute__((no_instrument_function)) int is_highmem_idx(enum zone_type idx)
{




 return 0;

}







static inline __attribute__((no_instrument_function)) int is_highmem(struct zone *zone)
{






 return 0;

}


struct ctl_table;
int min_free_kbytes_sysctl_handler(struct ctl_table *, int,
     void *, size_t *, loff_t *);
extern int sysctl_lowmem_reserve_ratio[4 -1];
int lowmem_reserve_ratio_sysctl_handler(struct ctl_table *, int,
     void *, size_t *, loff_t *);
int percpu_pagelist_fraction_sysctl_handler(struct ctl_table *, int,
     void *, size_t *, loff_t *);
int sysctl_min_unmapped_ratio_sysctl_handler(struct ctl_table *, int,
   void *, size_t *, loff_t *);
int sysctl_min_slab_ratio_sysctl_handler(struct ctl_table *, int,
   void *, size_t *, loff_t *);

extern int numa_zonelist_order_handler(struct ctl_table *, int,
   void *, size_t *, loff_t *);
extern char numa_zonelist_order[];
# 930 "include/linux/mmzone.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/mmzone.h" 1



# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/mmzone_64.h" 1
# 10 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/mmzone_64.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/smp.h" 1
# 11 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/smp.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/mpspec.h" 1




# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/mpspec_def.h" 1
# 21 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/mpspec_def.h"
struct mpf_intel {
 char signature[4];
 unsigned int physptr;
 unsigned char length;
 unsigned char specification;
 unsigned char checksum;
 unsigned char feature1;
 unsigned char feature2;
 unsigned char feature3;
 unsigned char feature4;
 unsigned char feature5;
};



struct mpc_table {
 char signature[4];
 unsigned short length;
 char spec;
 char checksum;
 char oem[8];
 char productid[12];
 unsigned int oemptr;
 unsigned short oemsize;
 unsigned short oemcount;
 unsigned int lapic;
 unsigned int reserved;
};
# 67 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/mpspec_def.h"
struct mpc_cpu {
 unsigned char type;
 unsigned char apicid;
 unsigned char apicver;
 unsigned char cpuflag;
 unsigned int cpufeature;
 unsigned int featureflag;
 unsigned int reserved[2];
};

struct mpc_bus {
 unsigned char type;
 unsigned char busid;
 unsigned char bustype[6];
};
# 105 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/mpspec_def.h"
struct mpc_ioapic {
 unsigned char type;
 unsigned char apicid;
 unsigned char apicver;
 unsigned char flags;
 unsigned int apicaddr;
};

struct mpc_intsrc {
 unsigned char type;
 unsigned char irqtype;
 unsigned short irqflag;
 unsigned char srcbus;
 unsigned char srcbusirq;
 unsigned char dstapic;
 unsigned char dstirq;
};

enum mp_irq_source_types {
 mp_INT = 0,
 mp_NMI = 1,
 mp_SMI = 2,
 mp_ExtINT = 3
};







struct mpc_lintsrc {
 unsigned char type;
 unsigned char irqtype;
 unsigned short irqflag;
 unsigned char srcbusid;
 unsigned char srcbusirq;
 unsigned char destapic;
 unsigned char destapiclint;
};



struct mpc_oemtable {
 char signature[4];
 unsigned short length;
 char rev;
 char checksum;
 char mpc[8];
};
# 168 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/mpspec_def.h"
enum mp_bustype {
 MP_BUS_ISA = 1,
 MP_BUS_EISA,
 MP_BUS_PCI,
};
# 6 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/mpspec.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/x86_init.h" 1




# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/bootparam.h" 1
# 32 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/bootparam.h"
# 1 "include/linux/screen_info.h" 1



# 1 "include/uapi/linux/screen_info.h" 1
# 10 "include/uapi/linux/screen_info.h"
struct screen_info {
 __u8 orig_x;
 __u8 orig_y;
 __u16 ext_mem_k;
 __u16 orig_video_page;
 __u8 orig_video_mode;
 __u8 orig_video_cols;
 __u8 flags;
 __u8 unused2;
 __u16 orig_video_ega_bx;
 __u16 unused3;
 __u8 orig_video_lines;
 __u8 orig_video_isVGA;
 __u16 orig_video_points;


 __u16 lfb_width;
 __u16 lfb_height;
 __u16 lfb_depth;
 __u32 lfb_base;
 __u32 lfb_size;
 __u16 cl_magic, cl_offset;
 __u16 lfb_linelength;
 __u8 red_size;
 __u8 red_pos;
 __u8 green_size;
 __u8 green_pos;
 __u8 blue_size;
 __u8 blue_pos;
 __u8 rsvd_size;
 __u8 rsvd_pos;
 __u16 vesapm_seg;
 __u16 vesapm_off;
 __u16 pages;
 __u16 vesa_attributes;
 __u32 capabilities;
 __u8 _reserved[6];
} __attribute__((packed));
# 5 "include/linux/screen_info.h" 2

extern struct screen_info screen_info;
# 33 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/bootparam.h" 2
# 1 "include/linux/apm_bios.h" 1
# 18 "include/linux/apm_bios.h"
# 1 "include/uapi/linux/apm_bios.h" 1
# 21 "include/uapi/linux/apm_bios.h"
typedef unsigned short apm_event_t;
typedef unsigned short apm_eventinfo_t;

struct apm_bios_info {
 __u16 version;
 __u16 cseg;
 __u32 offset;
 __u16 cseg_16;
 __u16 dseg;
 __u16 flags;
 __u16 cseg_len;
 __u16 cseg_16_len;
 __u16 dseg_len;
};
# 19 "include/linux/apm_bios.h" 2
# 35 "include/linux/apm_bios.h"
struct apm_info {
 struct apm_bios_info bios;
 unsigned short connection_version;
 int get_power_status_broken;
 int get_power_status_swabinminutes;
 int allow_ints;
 int forbid_idle;
 int realmode_power_off;
 int disabled;
};
# 94 "include/linux/apm_bios.h"
extern struct apm_info apm_info;
# 34 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/bootparam.h" 2
# 1 "include/linux/edd.h" 1
# 33 "include/linux/edd.h"
# 1 "include/uapi/linux/edd.h" 1
# 71 "include/uapi/linux/edd.h"
struct edd_device_params {
 __u16 length;
 __u16 info_flags;
 __u32 num_default_cylinders;
 __u32 num_default_heads;
 __u32 sectors_per_track;
 __u64 number_of_sectors;
 __u16 bytes_per_sector;
 __u32 dpte_ptr;
 __u16 key;
 __u8 device_path_info_length;
 __u8 reserved2;
 __u16 reserved3;
 __u8 host_bus_type[4];
 __u8 interface_type[8];
 union {
  struct {
   __u16 base_address;
   __u16 reserved1;
   __u32 reserved2;
  } __attribute__ ((packed)) isa;
  struct {
   __u8 bus;
   __u8 slot;
   __u8 function;
   __u8 channel;
   __u32 reserved;
  } __attribute__ ((packed)) pci;

  struct {
   __u64 reserved;
  } __attribute__ ((packed)) ibnd;
  struct {
   __u64 reserved;
  } __attribute__ ((packed)) xprs;
  struct {
   __u64 reserved;
  } __attribute__ ((packed)) htpt;
  struct {
   __u64 reserved;
  } __attribute__ ((packed)) unknown;
 } interface_path;
 union {
  struct {
   __u8 device;
   __u8 reserved1;
   __u16 reserved2;
   __u32 reserved3;
   __u64 reserved4;
  } __attribute__ ((packed)) ata;
  struct {
   __u8 device;
   __u8 lun;
   __u8 reserved1;
   __u8 reserved2;
   __u32 reserved3;
   __u64 reserved4;
  } __attribute__ ((packed)) atapi;
  struct {
   __u16 id;
   __u64 lun;
   __u16 reserved1;
   __u32 reserved2;
  } __attribute__ ((packed)) scsi;
  struct {
   __u64 serial_number;
   __u64 reserved;
  } __attribute__ ((packed)) usb;
  struct {
   __u64 eui;
   __u64 reserved;
  } __attribute__ ((packed)) i1394;
  struct {
   __u64 wwid;
   __u64 lun;
  } __attribute__ ((packed)) fibre;
  struct {
   __u64 identity_tag;
   __u64 reserved;
  } __attribute__ ((packed)) i2o;
  struct {
   __u32 array_number;
   __u32 reserved1;
   __u64 reserved2;
  } __attribute__ ((packed)) raid;
  struct {
   __u8 device;
   __u8 reserved1;
   __u16 reserved2;
   __u32 reserved3;
   __u64 reserved4;
  } __attribute__ ((packed)) sata;
  struct {
   __u64 reserved1;
   __u64 reserved2;
  } __attribute__ ((packed)) unknown;
 } device_path;
 __u8 reserved4;
 __u8 checksum;
} __attribute__ ((packed));

struct edd_info {
 __u8 device;
 __u8 version;
 __u16 interface_support;
 __u16 legacy_max_cylinder;
 __u8 legacy_max_head;
 __u8 legacy_sectors_per_track;
 struct edd_device_params params;
} __attribute__ ((packed));

struct edd {
 unsigned int mbr_signature[16];
 struct edd_info edd_info[6];
 unsigned char mbr_signature_nr;
 unsigned char edd_info_nr;
};
# 34 "include/linux/edd.h" 2


extern struct edd edd;
# 35 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/bootparam.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/e820.h" 1
# 10 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/e820.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/e820.h" 1
# 52 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/e820.h"
struct e820entry {
 __u64 addr;
 __u64 size;
 __u32 type;
} __attribute__((packed));

struct e820map {
 __u32 nr_map;
 struct e820entry map[(128 + 3 * (1 << 10))];
};
# 11 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/e820.h" 2


extern struct e820map e820;
extern struct e820map e820_saved;

extern unsigned long pci_mem_start;
extern int e820_any_mapped(u64 start, u64 end, unsigned type);
extern int e820_all_mapped(u64 start, u64 end, unsigned type);
extern void e820_add_region(u64 start, u64 size, int type);
extern void e820_print_map(char *who);
extern int
sanitize_e820_map(struct e820entry *biosmap, int max_nr_map, u32 *pnr_map);
extern u64 e820_update_range(u64 start, u64 size, unsigned old_type,
          unsigned new_type);
extern u64 e820_remove_range(u64 start, u64 size, unsigned old_type,
        int checktype);
extern void update_e820(void);
extern void e820_setup_gap(void);
extern int e820_search_gap(unsigned long *gapstart, unsigned long *gapsize,
   unsigned long start_addr, unsigned long long end_addr);
struct setup_data;
extern void parse_e820_ext(u64 phys_addr, u32 data_len);



extern void e820_mark_nosave_regions(unsigned long limit_pfn);







extern void early_memtest(unsigned long start, unsigned long end);






extern unsigned long e820_end_of_ram_pfn(void);
extern unsigned long e820_end_of_low_ram_pfn(void);
extern u64 early_reserve_e820(u64 sizet, u64 align);

void memblock_x86_fill(void);
void memblock_find_dma_reserve(void);

extern void finish_e820_parsing(void);
extern void e820_reserve_resources(void);
extern void e820_reserve_resources_late(void);
extern void setup_memory_map(void);
extern char *default_machine_specific_memory_setup(void);





static inline __attribute__((no_instrument_function)) bool is_ISA_range(u64 s, u64 e)
{
 return s >= 0xa0000 && e <= 0x100000;
}
# 36 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/bootparam.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/ist.h" 1
# 18 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/ist.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/ist.h" 1
# 22 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/ist.h"
struct ist_info {
 __u32 signature;
 __u32 command;
 __u32 event;
 __u32 perf_level;
};
# 19 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/ist.h" 2


extern struct ist_info ist_info;
# 37 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/bootparam.h" 2
# 1 "include/video/edid.h" 1



# 1 "include/uapi/video/edid.h" 1



struct edid_info {
 unsigned char dummy[128];
};
# 5 "include/video/edid.h" 2


extern struct edid_info edid_info;
# 38 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/bootparam.h" 2


struct setup_data {
 __u64 next;
 __u32 type;
 __u32 len;
 __u8 data[0];
};

struct setup_header {
 __u8 setup_sects;
 __u16 root_flags;
 __u32 syssize;
 __u16 ram_size;
 __u16 vid_mode;
 __u16 root_dev;
 __u16 boot_flag;
 __u16 jump;
 __u32 header;
 __u16 version;
 __u32 realmode_swtch;
 __u16 start_sys;
 __u16 kernel_version;
 __u8 type_of_loader;
 __u8 loadflags;
 __u16 setup_move_size;
 __u32 code32_start;
 __u32 ramdisk_image;
 __u32 ramdisk_size;
 __u32 bootsect_kludge;
 __u16 heap_end_ptr;
 __u8 ext_loader_ver;
 __u8 ext_loader_type;
 __u32 cmd_line_ptr;
 __u32 initrd_addr_max;
 __u32 kernel_alignment;
 __u8 relocatable_kernel;
 __u8 min_alignment;
 __u16 xloadflags;
 __u32 cmdline_size;
 __u32 hardware_subarch;
 __u64 hardware_subarch_data;
 __u32 payload_offset;
 __u32 payload_length;
 __u64 setup_data;
 __u64 pref_address;
 __u32 init_size;
 __u32 handover_offset;
} __attribute__((packed));

struct sys_desc_table {
 __u16 length;
 __u8 table[14];
};


struct olpc_ofw_header {
 __u32 ofw_magic;
 __u32 ofw_version;
 __u32 cif_handler;
 __u32 irq_desc_table;
} __attribute__((packed));

struct efi_info {
 __u32 efi_loader_signature;
 __u32 efi_systab;
 __u32 efi_memdesc_size;
 __u32 efi_memdesc_version;
 __u32 efi_memmap;
 __u32 efi_memmap_size;
 __u32 efi_systab_hi;
 __u32 efi_memmap_hi;
};


struct boot_params {
 struct screen_info screen_info;
 struct apm_bios_info apm_bios_info;
 __u8 _pad2[4];
 __u64 tboot_addr;
 struct ist_info ist_info;
 __u8 _pad3[16];
 __u8 hd0_info[16];
 __u8 hd1_info[16];
 struct sys_desc_table sys_desc_table;
 struct olpc_ofw_header olpc_ofw_header;
 __u32 ext_ramdisk_image;
 __u32 ext_ramdisk_size;
 __u32 ext_cmd_line_ptr;
 __u8 _pad4[116];
 struct edid_info edid_info;
 struct efi_info efi_info;
 __u32 alt_mem_k;
 __u32 scratch;
 __u8 e820_entries;
 __u8 eddbuf_entries;
 __u8 edd_mbr_sig_buf_entries;
 __u8 kbd_status;
 __u8 _pad5[3];
# 148 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/bootparam.h"
 __u8 sentinel;
 __u8 _pad6[1];
 struct setup_header hdr;
 __u8 _pad7[0x290-0x1f1-sizeof(struct setup_header)];
 __u32 edd_mbr_sig_buffer[16];
 struct e820entry e820_map[128];
 __u8 _pad8[48];
 struct edd_info eddbuf[6];
 __u8 _pad9[276];
} __attribute__((packed));

enum {
 X86_SUBARCH_PC = 0,
 X86_SUBARCH_LGUEST,
 X86_SUBARCH_XEN,
 X86_SUBARCH_INTEL_MID,
 X86_SUBARCH_CE4100,
 X86_NR_SUBARCHS,
};
# 6 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/x86_init.h" 2

struct mpc_bus;
struct mpc_cpu;
struct mpc_table;
struct cpuinfo_x86;
# 23 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/x86_init.h"
struct x86_init_mpparse {
 void (*mpc_record)(unsigned int mode);
 void (*setup_ioapic_ids)(void);
 int (*mpc_apic_id)(struct mpc_cpu *m);
 void (*smp_read_mpc_oem)(struct mpc_table *mpc);
 void (*mpc_oem_pci_bus)(struct mpc_bus *m);
 void (*mpc_oem_bus_info)(struct mpc_bus *m, char *name);
 void (*find_smp_config)(void);
 void (*get_smp_config)(unsigned int early);
};
# 42 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/x86_init.h"
struct x86_init_resources {
 void (*probe_roms)(void);
 void (*reserve_resources)(void);
 char *(*memory_setup)(void);
};
# 55 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/x86_init.h"
struct x86_init_irqs {
 void (*pre_vector_init)(void);
 void (*intr_init)(void);
 void (*trap_init)(void);
};






struct x86_init_oem {
 void (*arch_setup)(void);
 void (*banner)(void);
};
# 78 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/x86_init.h"
struct x86_init_paging {
 void (*pagetable_init)(void);
};
# 90 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/x86_init.h"
struct x86_init_timers {
 void (*setup_percpu_clockev)(void);
 void (*tsc_pre_init)(void);
 void (*timer_init)(void);
 void (*wallclock_init)(void);
};





struct x86_init_iommu {
 int (*iommu_init)(void);
};
# 112 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/x86_init.h"
struct x86_init_pci {
 int (*arch_init)(void);
 int (*init)(void);
 void (*init_irq)(void);
 void (*fixup_irqs)(void);
};





struct x86_init_ops {
 struct x86_init_resources resources;
 struct x86_init_mpparse mpparse;
 struct x86_init_irqs irqs;
 struct x86_init_oem oem;
 struct x86_init_paging paging;
 struct x86_init_timers timers;
 struct x86_init_iommu iommu;
 struct x86_init_pci pci;
};






struct x86_cpuinit_ops {
 void (*setup_percpu_clockev)(void);
 void (*early_percpu_clock_init)(void);
 void (*fixup_cpu_id)(struct cpuinfo_x86 *c, int node);
};

struct timespec;
# 159 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/x86_init.h"
struct x86_platform_ops {
 unsigned long (*calibrate_tsc)(void);
 void (*get_wallclock)(struct timespec *ts);
 int (*set_wallclock)(const struct timespec *ts);
 void (*iommu_shutdown)(void);
 bool (*is_untracked_pat_range)(u64 start, u64 end);
 void (*nmi_init)(void);
 unsigned char (*get_nmi_reason)(void);
 int (*i8042_detect)(void);
 void (*save_sched_clock_state)(void);
 void (*restore_sched_clock_state)(void);
 void (*apic_post_init)(void);
};

struct pci_dev;
struct msi_msg;
struct msi_desc;

struct x86_msi_ops {
 int (*setup_msi_irqs)(struct pci_dev *dev, int nvec, int type);
 void (*compose_msi_msg)(struct pci_dev *dev, unsigned int irq,
    unsigned int dest, struct msi_msg *msg,
          u8 hpet_id);
 void (*teardown_msi_irq)(unsigned int irq);
 void (*teardown_msi_irqs)(struct pci_dev *dev);
 void (*restore_msi_irqs)(struct pci_dev *dev);
 int (*setup_hpet_msi)(unsigned int irq, unsigned int id);
 u32 (*msi_mask_irq)(struct msi_desc *desc, u32 mask, u32 flag);
 u32 (*msix_mask_irq)(struct msi_desc *desc, u32 flag);
};

struct IO_APIC_route_entry;
struct io_apic_irq_attr;
struct irq_data;
struct cpumask;

struct x86_io_apic_ops {
 void (*init) (void);
 unsigned int (*read) (unsigned int apic, unsigned int reg);
 void (*write) (unsigned int apic, unsigned int reg, unsigned int value);
 void (*modify) (unsigned int apic, unsigned int reg, unsigned int value);
 void (*disable)(void);
 void (*print_entries)(unsigned int apic, unsigned int nr_entries);
 int (*set_affinity)(struct irq_data *data,
     const struct cpumask *mask,
     bool force);
 int (*setup_entry)(int irq, struct IO_APIC_route_entry *entry,
           unsigned int destination, int vector,
           struct io_apic_irq_attr *attr);
 void (*eoi_ioapic_pin)(int apic, int pin, int vector);
};

extern struct x86_init_ops x86_init;
extern struct x86_cpuinit_ops x86_cpuinit;
extern struct x86_platform_ops x86_platform;
extern struct x86_msi_ops x86_msi;
extern struct x86_io_apic_ops x86_io_apic_ops;
extern void x86_init_noop(void);
extern void x86_init_uint_noop(unsigned int unused);
# 7 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/mpspec.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/apicdef.h" 1
# 178 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/apicdef.h"
struct local_apic {

        struct { unsigned int __reserved[4]; } __reserved_01;

        struct { unsigned int __reserved[4]; } __reserved_02;

        struct {
  unsigned int __reserved_1 : 24,
   phys_apic_id : 4,
   __reserved_2 : 4;
  unsigned int __reserved[3];
 } id;

        const
 struct {
  unsigned int version : 8,
   __reserved_1 : 8,
   max_lvt : 8,
   __reserved_2 : 8;
  unsigned int __reserved[3];
 } version;

        struct { unsigned int __reserved[4]; } __reserved_03;

        struct { unsigned int __reserved[4]; } __reserved_04;

        struct { unsigned int __reserved[4]; } __reserved_05;

        struct { unsigned int __reserved[4]; } __reserved_06;

        struct {
  unsigned int priority : 8,
   __reserved_1 : 24;
  unsigned int __reserved_2[3];
 } tpr;

        const
 struct {
  unsigned int priority : 8,
   __reserved_1 : 24;
  unsigned int __reserved_2[3];
 } apr;

        const
 struct {
  unsigned int priority : 8,
   __reserved_1 : 24;
  unsigned int __reserved_2[3];
 } ppr;

        struct {
  unsigned int eoi;
  unsigned int __reserved[3];
 } eoi;

        struct { unsigned int __reserved[4]; } __reserved_07;

        struct {
  unsigned int __reserved_1 : 24,
   logical_dest : 8;
  unsigned int __reserved_2[3];
 } ldr;

        struct {
  unsigned int __reserved_1 : 28,
   model : 4;
  unsigned int __reserved_2[3];
 } dfr;

        struct {
  unsigned int spurious_vector : 8,
   apic_enabled : 1,
   focus_cpu : 1,
   __reserved_2 : 22;
  unsigned int __reserved_3[3];
 } svr;

        struct {
         unsigned int bitfield;
  unsigned int __reserved[3];
 } isr [8];

        struct {
         unsigned int bitfield;
  unsigned int __reserved[3];
 } tmr [8];

        struct {
         unsigned int bitfield;
  unsigned int __reserved[3];
 } irr [8];

        union {
  struct {
   unsigned int send_cs_error : 1,
    receive_cs_error : 1,
    send_accept_error : 1,
    receive_accept_error : 1,
    __reserved_1 : 1,
    send_illegal_vector : 1,
    receive_illegal_vector : 1,
    illegal_register_address : 1,
    __reserved_2 : 24;
   unsigned int __reserved_3[3];
  } error_bits;
  struct {
   unsigned int errors;
   unsigned int __reserved_3[3];
  } all_errors;
 } esr;

        struct { unsigned int __reserved[4]; } __reserved_08;

        struct { unsigned int __reserved[4]; } __reserved_09;

        struct { unsigned int __reserved[4]; } __reserved_10;

        struct { unsigned int __reserved[4]; } __reserved_11;

        struct { unsigned int __reserved[4]; } __reserved_12;

        struct { unsigned int __reserved[4]; } __reserved_13;

        struct { unsigned int __reserved[4]; } __reserved_14;

        struct {
  unsigned int vector : 8,
   delivery_mode : 3,
   destination_mode : 1,
   delivery_status : 1,
   __reserved_1 : 1,
   level : 1,
   trigger : 1,
   __reserved_2 : 2,
   shorthand : 2,
   __reserved_3 : 12;
  unsigned int __reserved_4[3];
 } icr1;

        struct {
  union {
   unsigned int __reserved_1 : 24,
    phys_dest : 4,
    __reserved_2 : 4;
   unsigned int __reserved_3 : 24,
    logical_dest : 8;
  } dest;
  unsigned int __reserved_4[3];
 } icr2;

        struct {
  unsigned int vector : 8,
   __reserved_1 : 4,
   delivery_status : 1,
   __reserved_2 : 3,
   mask : 1,
   timer_mode : 1,
   __reserved_3 : 14;
  unsigned int __reserved_4[3];
 } lvt_timer;

        struct {
  unsigned int vector : 8,
   delivery_mode : 3,
   __reserved_1 : 1,
   delivery_status : 1,
   __reserved_2 : 3,
   mask : 1,
   __reserved_3 : 15;
  unsigned int __reserved_4[3];
 } lvt_thermal;

        struct {
  unsigned int vector : 8,
   delivery_mode : 3,
   __reserved_1 : 1,
   delivery_status : 1,
   __reserved_2 : 3,
   mask : 1,
   __reserved_3 : 15;
  unsigned int __reserved_4[3];
 } lvt_pc;

        struct {
  unsigned int vector : 8,
   delivery_mode : 3,
   __reserved_1 : 1,
   delivery_status : 1,
   polarity : 1,
   remote_irr : 1,
   trigger : 1,
   mask : 1,
   __reserved_2 : 15;
  unsigned int __reserved_3[3];
 } lvt_lint0;

        struct {
  unsigned int vector : 8,
   delivery_mode : 3,
   __reserved_1 : 1,
   delivery_status : 1,
   polarity : 1,
   remote_irr : 1,
   trigger : 1,
   mask : 1,
   __reserved_2 : 15;
  unsigned int __reserved_3[3];
 } lvt_lint1;

        struct {
  unsigned int vector : 8,
   __reserved_1 : 4,
   delivery_status : 1,
   __reserved_2 : 3,
   mask : 1,
   __reserved_3 : 15;
  unsigned int __reserved_4[3];
 } lvt_error;

        struct {
  unsigned int initial_count;
  unsigned int __reserved_2[3];
 } timer_icr;

        const
 struct {
  unsigned int curr_count;
  unsigned int __reserved_2[3];
 } timer_ccr;

        struct { unsigned int __reserved[4]; } __reserved_16;

        struct { unsigned int __reserved[4]; } __reserved_17;

        struct { unsigned int __reserved[4]; } __reserved_18;

        struct { unsigned int __reserved[4]; } __reserved_19;

        struct {
  unsigned int divisor : 4,
   __reserved_1 : 28;
  unsigned int __reserved_2[3];
 } timer_dcr;

        struct { unsigned int __reserved[4]; } __reserved_20;

} __attribute__ ((packed));
# 434 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/apicdef.h"
enum ioapic_irq_destination_types {
 dest_Fixed = 0,
 dest_LowestPrio = 1,
 dest_SMI = 2,
 dest__reserved_1 = 3,
 dest_NMI = 4,
 dest_INIT = 5,
 dest__reserved_2 = 6,
 dest_ExtINT = 7
};
# 8 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/mpspec.h" 2

extern int apic_version[];
extern int pic_mode;
# 40 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/mpspec.h"
extern unsigned long mp_bus_not_pci[(((256) + (8 * sizeof(long)) - 1) / (8 * sizeof(long)))];

extern unsigned int boot_cpu_physical_apicid;
extern unsigned int max_physical_apicid;
extern int mpc_default_type;
extern unsigned long mp_lapic_addr;


extern int smp_found_config;




static inline __attribute__((no_instrument_function)) void get_smp_config(void)
{
 x86_init.mpparse.get_smp_config(0);
}

static inline __attribute__((no_instrument_function)) void early_get_smp_config(void)
{
 x86_init.mpparse.get_smp_config(1);
}

static inline __attribute__((no_instrument_function)) void find_smp_config(void)
{
 x86_init.mpparse.find_smp_config();
}


extern void early_reserve_e820_mpc_new(void);
extern int enable_update_mptable;
extern int default_mpc_apic_id(struct mpc_cpu *m);
extern void default_smp_read_mpc_oem(struct mpc_table *mpc);

extern void default_mpc_oem_bus_info(struct mpc_bus *m, char *str);



extern void default_find_smp_config(void);
extern void default_get_smp_config(unsigned int early);
# 90 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/mpspec.h"
int generic_processor_info(int apicid, int version);

extern void mp_register_ioapic(int id, u32 address, u32 gsi_base);
extern void mp_override_legacy_irq(u8 bus_irq, u8 polarity, u8 trigger,
       u32 gsi);
extern void mp_config_acpi_legacy_irqs(void);
struct device;
extern int mp_register_gsi(struct device *dev, u32 gsi, int edge_level,
     int active_high_low);




struct physid_mask {
 unsigned long mask[(((32768) + (8 * sizeof(long)) - 1) / (8 * sizeof(long)))];
};

typedef struct physid_mask physid_mask_t;
# 142 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/mpspec.h"
static inline __attribute__((no_instrument_function)) unsigned long physids_coerce(physid_mask_t *map)
{
 return map->mask[0];
}

static inline __attribute__((no_instrument_function)) void physids_promote(unsigned long physids, physid_mask_t *map)
{
 bitmap_zero((*map).mask, 32768);
 map->mask[0] = physids;
}

static inline __attribute__((no_instrument_function)) void physid_set_mask_of_physid(int physid, physid_mask_t *map)
{
 bitmap_zero((*map).mask, 32768);
 set_bit(physid, (*map).mask);
}




extern physid_mask_t phys_cpu_present_map;

extern int generic_mps_oem_check(struct mpc_table *, char *, char *);

extern int default_acpi_madt_oem_check(char *, char *);
# 12 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/smp.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/apic.h" 1
# 12 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/apic.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/fixmap.h" 1
# 19 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/fixmap.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/acpi.h" 1
# 26 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/acpi.h"
# 1 "include/acpi/pdc_intel.h" 1
# 27 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/acpi.h" 2

# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/numa.h" 1





# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/topology.h" 1
# 51 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/topology.h"
extern __attribute__((section(".discard"), unused)) char __pcpu_scope_x86_cpu_to_node_map; extern __attribute__((section(".data..percpu" ""))) __typeof__(int) x86_cpu_to_node_map; extern __typeof__(int) *x86_cpu_to_node_map_early_ptr; extern __typeof__(int) x86_cpu_to_node_map_early_map[];





extern int __cpu_to_node(int cpu);


extern int early_cpu_to_node(int cpu);
# 73 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/topology.h"
extern cpumask_var_t node_to_cpumask_map[(1 << 10)];


extern const struct cpumask *cpumask_of_node(int node);
# 85 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/topology.h"
extern void setup_node_to_cpumask_map(void);
# 95 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/topology.h"
extern int __node_distance(int, int);
# 118 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/topology.h"
# 1 "include/asm-generic/topology.h" 1
# 119 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/topology.h" 2

extern const struct cpumask *cpu_coregroup_mask(int cpu);
# 130 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/topology.h"
static inline __attribute__((no_instrument_function)) void arch_fix_phys_package_id(int num, u32 slot)
{
}

struct pci_bus;
int x86_pci_root_bus_node(int bus);
void x86_pci_root_bus_resources(int bus, struct list_head *resources);
# 7 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/numa.h" 2
# 21 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/numa.h"
extern int numa_off;
# 31 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/numa.h"
extern s16 __apicid_to_node[32768];
extern nodemask_t numa_nodes_parsed __attribute__ ((__section__(".init.data")));

extern int __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) numa_add_memblk(int nodeid, u64 start, u64 end);
extern void __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) numa_set_distance(int from, int to, int distance);

static inline __attribute__((no_instrument_function)) void set_apicid_to_node(int apicid, s16 node)
{
 __apicid_to_node[apicid] = node;
}

extern int numa_cpu_node(int cpu);
# 60 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/numa.h"
extern void numa_set_node(int cpu, int node);
extern void numa_clear_node(int cpu);
extern void __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) init_cpu_to_node(void);
extern void numa_add_cpu(int cpu);
extern void numa_remove_cpu(int cpu);
# 74 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/numa.h"
void debug_cpumask_set_cpu(int cpu, int node, bool enable);





void numa_emu_cmdline(char *);
# 29 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/acpi.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/fixmap.h" 1
# 30 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/acpi.h" 2

# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/mmu.h" 1
# 11 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/mmu.h"
typedef struct {
 void *ldt;
 int size;



 unsigned short ia32_compat;


 struct mutex lock;
 void *vdso;
} mm_context_t;


void leave_mm(int cpu);
# 32 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/acpi.h" 2

# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/realmode.h" 1




# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/io.h" 1
# 42 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/io.h"
# 1 "arch/x86/include/generated/asm/early_ioremap.h" 1
# 1 "include/asm-generic/early_ioremap.h" 1
# 10 "include/asm-generic/early_ioremap.h"
extern void *early_ioremap(resource_size_t phys_addr,
       unsigned long size);
extern void *early_memremap(resource_size_t phys_addr,
       unsigned long size);
extern void early_iounmap(void *addr, unsigned long size);
extern void early_memunmap(void *addr, unsigned long size);





extern void early_ioremap_shutdown(void);



extern void early_ioremap_init(void);


extern void early_ioremap_setup(void);





extern void early_ioremap_reset(void);
# 1 "arch/x86/include/generated/asm/early_ioremap.h" 2
# 43 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/io.h" 2
# 54 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/io.h"
static inline __attribute__((no_instrument_function)) unsigned char readb(const volatile void *addr) { unsigned char ret; asm volatile("mov" "b" " %1,%0":"=q" (ret) :"m" (*(volatile unsigned char *)addr) :"memory"); return ret; }
static inline __attribute__((no_instrument_function)) unsigned short readw(const volatile void *addr) { unsigned short ret; asm volatile("mov" "w" " %1,%0":"=r" (ret) :"m" (*(volatile unsigned short *)addr) :"memory"); return ret; }
static inline __attribute__((no_instrument_function)) unsigned int readl(const volatile void *addr) { unsigned int ret; asm volatile("mov" "l" " %1,%0":"=r" (ret) :"m" (*(volatile unsigned int *)addr) :"memory"); return ret; }

static inline __attribute__((no_instrument_function)) unsigned char __readb(const volatile void *addr) { unsigned char ret; asm volatile("mov" "b" " %1,%0":"=q" (ret) :"m" (*(volatile unsigned char *)addr) ); return ret; }
static inline __attribute__((no_instrument_function)) unsigned short __readw(const volatile void *addr) { unsigned short ret; asm volatile("mov" "w" " %1,%0":"=r" (ret) :"m" (*(volatile unsigned short *)addr) ); return ret; }
static inline __attribute__((no_instrument_function)) unsigned int __readl(const volatile void *addr) { unsigned int ret; asm volatile("mov" "l" " %1,%0":"=r" (ret) :"m" (*(volatile unsigned int *)addr) ); return ret; }

static inline __attribute__((no_instrument_function)) void writeb(unsigned char val, volatile void *addr) { asm volatile("mov" "b" " %0,%1": :"q" (val), "m" (*(volatile unsigned char *)addr) :"memory"); }
static inline __attribute__((no_instrument_function)) void writew(unsigned short val, volatile void *addr) { asm volatile("mov" "w" " %0,%1": :"r" (val), "m" (*(volatile unsigned short *)addr) :"memory"); }
static inline __attribute__((no_instrument_function)) void writel(unsigned int val, volatile void *addr) { asm volatile("mov" "l" " %0,%1": :"r" (val), "m" (*(volatile unsigned int *)addr) :"memory"); }

static inline __attribute__((no_instrument_function)) void __writeb(unsigned char val, volatile void *addr) { asm volatile("mov" "b" " %0,%1": :"q" (val), "m" (*(volatile unsigned char *)addr) ); }
static inline __attribute__((no_instrument_function)) void __writew(unsigned short val, volatile void *addr) { asm volatile("mov" "w" " %0,%1": :"r" (val), "m" (*(volatile unsigned short *)addr) ); }
static inline __attribute__((no_instrument_function)) void __writel(unsigned int val, volatile void *addr) { asm volatile("mov" "l" " %0,%1": :"r" (val), "m" (*(volatile unsigned int *)addr) ); }
# 85 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/io.h"
static inline __attribute__((no_instrument_function)) unsigned long readq(const volatile void *addr) { unsigned long ret; asm volatile("mov" "q" " %1,%0":"=r" (ret) :"m" (*(volatile unsigned long *)addr) :"memory"); return ret; }
static inline __attribute__((no_instrument_function)) void writeq(unsigned long val, volatile void *addr) { asm volatile("mov" "q" " %0,%1": :"r" (val), "m" (*(volatile unsigned long *)addr) :"memory"); }
# 112 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/io.h"
static inline __attribute__((no_instrument_function)) phys_addr_t virt_to_phys(volatile void *address)
{
 return __phys_addr((unsigned long)(address));
}
# 130 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/io.h"
static inline __attribute__((no_instrument_function)) void *phys_to_virt(phys_addr_t address)
{
 return ((void *)((unsigned long)(address)+((unsigned long)(0xffff880000000000UL))));
}
# 145 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/io.h"
static inline __attribute__((no_instrument_function)) unsigned int isa_virt_to_bus(volatile void *address)
{
 return (unsigned int)virt_to_phys(address);
}
# 175 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/io.h"
extern void *ioremap_nocache(resource_size_t offset, unsigned long size);
extern void *ioremap_cache(resource_size_t offset, unsigned long size);
extern void *ioremap_prot(resource_size_t offset, unsigned long size,
    unsigned long prot_val);




static inline __attribute__((no_instrument_function)) void *ioremap(resource_size_t offset, unsigned long size)
{
 return ioremap_nocache(offset, size);
}

extern void iounmap(volatile void *addr);

extern void set_iounmap_nonlazy(void);



# 1 "include/asm-generic/iomap.h" 1
# 28 "include/asm-generic/iomap.h"
extern unsigned int ioread8(void *);
extern unsigned int ioread16(void *);
extern unsigned int ioread16be(void *);
extern unsigned int ioread32(void *);
extern unsigned int ioread32be(void *);

extern void iowrite8(u8, void *);
extern void iowrite16(u16, void *);
extern void iowrite16be(u16, void *);
extern void iowrite32(u32, void *);
extern void iowrite32be(u32, void *);
# 51 "include/asm-generic/iomap.h"
extern void ioread8_rep(void *port, void *buf, unsigned long count);
extern void ioread16_rep(void *port, void *buf, unsigned long count);
extern void ioread32_rep(void *port, void *buf, unsigned long count);

extern void iowrite8_rep(void *port, const void *buf, unsigned long count);
extern void iowrite16_rep(void *port, const void *buf, unsigned long count);
extern void iowrite32_rep(void *port, const void *buf, unsigned long count);



extern void *ioport_map(unsigned long port, unsigned int nr);
extern void ioport_unmap(void *);
# 71 "include/asm-generic/iomap.h"
struct pci_dev;
extern void pci_iounmap(struct pci_dev *dev, void *);






# 1 "include/asm-generic/pci_iomap.h" 1
# 14 "include/asm-generic/pci_iomap.h"
struct pci_dev;


extern void *pci_iomap(struct pci_dev *dev, int bar, unsigned long max);
# 80 "include/asm-generic/iomap.h" 2
# 195 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/io.h" 2

# 1 "include/linux/vmalloc.h" 1
# 10 "include/linux/vmalloc.h"
struct vm_area_struct;
# 29 "include/linux/vmalloc.h"
struct vm_struct {
 struct vm_struct *next;
 void *addr;
 unsigned long size;
 unsigned long flags;
 struct page **pages;
 unsigned int nr_pages;
 phys_addr_t phys_addr;
 const void *caller;
};

struct vmap_area {
 unsigned long va_start;
 unsigned long va_end;
 unsigned long flags;
 struct rb_node rb_node;
 struct list_head list;
 struct list_head purge_list;
 struct vm_struct *vm;
 struct callback_head callback_head;
};




extern void vm_unmap_ram(const void *mem, unsigned int count);
extern void *vm_map_ram(struct page **pages, unsigned int count,
    int node, pgprot_t prot);
extern void vm_unmap_aliases(void);


extern void __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) vmalloc_init(void);






extern void *vmalloc(unsigned long size);
extern void *vzalloc(unsigned long size);
extern void *vmalloc_user(unsigned long size);
extern void *vmalloc_node(unsigned long size, int node);
extern void *vzalloc_node(unsigned long size, int node);
extern void *vmalloc_exec(unsigned long size);
extern void *vmalloc_32(unsigned long size);
extern void *vmalloc_32_user(unsigned long size);
extern void *__vmalloc(unsigned long size, gfp_t gfp_mask, pgprot_t prot);
extern void *__vmalloc_node_range(unsigned long size, unsigned long align,
   unsigned long start, unsigned long end, gfp_t gfp_mask,
   pgprot_t prot, int node, const void *caller);
extern void vfree(const void *addr);

extern void *vmap(struct page **pages, unsigned int count,
   unsigned long flags, pgprot_t prot);
extern void vunmap(const void *addr);

extern int remap_vmalloc_range_partial(struct vm_area_struct *vma,
           unsigned long uaddr, void *kaddr,
           unsigned long size);

extern int remap_vmalloc_range(struct vm_area_struct *vma, void *addr,
       unsigned long pgoff);
void vmalloc_sync_all(void);





static inline __attribute__((no_instrument_function)) size_t get_vm_area_size(const struct vm_struct *area)
{

 return area->size - ((1UL) << 12);
}

extern struct vm_struct *get_vm_area(unsigned long size, unsigned long flags);
extern struct vm_struct *get_vm_area_caller(unsigned long size,
     unsigned long flags, const void *caller);
extern struct vm_struct *__get_vm_area(unsigned long size, unsigned long flags,
     unsigned long start, unsigned long end);
extern struct vm_struct *__get_vm_area_caller(unsigned long size,
     unsigned long flags,
     unsigned long start, unsigned long end,
     const void *caller);
extern struct vm_struct *remove_vm_area(const void *addr);
extern struct vm_struct *find_vm_area(const void *addr);

extern int map_vm_area(struct vm_struct *area, pgprot_t prot,
   struct page ***pages);

extern int map_kernel_range_noflush(unsigned long start, unsigned long size,
        pgprot_t prot, struct page **pages);
extern void unmap_kernel_range_noflush(unsigned long addr, unsigned long size);
extern void unmap_kernel_range(unsigned long addr, unsigned long size);
# 140 "include/linux/vmalloc.h"
extern struct vm_struct *alloc_vm_area(size_t size, pte_t **ptes);
extern void free_vm_area(struct vm_struct *area);


extern long vread(char *buf, char *addr, unsigned long count);
extern long vwrite(char *buf, char *addr, unsigned long count);




extern struct list_head vmap_area_list;
extern __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) void vm_area_add_early(struct vm_struct *vm);
extern __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) void vm_area_register_early(struct vm_struct *vm, size_t align);



struct vm_struct **pcpu_get_vm_areas(const unsigned long *offsets,
         const size_t *sizes, int nr_vms,
         size_t align);

void pcpu_free_vm_areas(struct vm_struct **vms, int nr_vms);
# 177 "include/linux/vmalloc.h"
struct vmalloc_info {
 unsigned long used;
 unsigned long largest_chunk;
};



extern void get_vmalloc_info(struct vmalloc_info *vmi);
# 197 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/io.h" 2






static inline __attribute__((no_instrument_function)) void
memset_io(volatile void *addr, unsigned char val, size_t count)
{
 memset((void *)addr, val, count);
}

static inline __attribute__((no_instrument_function)) void
memcpy_fromio(void *dst, const volatile void *src, size_t count)
{
 ({ size_t __len = (count); void *__ret; if (__builtin_constant_p(count) && __len >= 64) __ret = __memcpy((dst), ((const void *)src), __len); else __ret = __builtin_memcpy((dst), ((const void *)src), __len); __ret; });
}

static inline __attribute__((no_instrument_function)) void
memcpy_toio(volatile void *dst, const void *src, size_t count)
{
 ({ size_t __len = (count); void *__ret; if (__builtin_constant_p(count) && __len >= 64) __ret = __memcpy(((void *)dst), (src), __len); else __ret = __builtin_memcpy(((void *)dst), (src), __len); __ret; });
}
# 239 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/io.h"
static inline __attribute__((no_instrument_function)) void flush_write_buffers(void)
{



}



extern void native_io_delay(void);

extern int io_delay_type;
extern void io_delay_init(void);
# 309 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/io.h"
static inline __attribute__((no_instrument_function)) void outb(unsigned char value, int port) { asm volatile("out" "b" " %" "b" "0, %w1" : : "a"(value), "Nd"(port)); } static inline __attribute__((no_instrument_function)) unsigned char inb(int port) { unsigned char value; asm volatile("in" "b" " %w1, %" "b" "0" : "=a"(value) : "Nd"(port)); return value; } static inline __attribute__((no_instrument_function)) void outb_p(unsigned char value, int port) { outb(value, port); slow_down_io(); } static inline __attribute__((no_instrument_function)) unsigned char inb_p(int port) { unsigned char value = inb(port); slow_down_io(); return value; } static inline __attribute__((no_instrument_function)) void outsb(int port, const void *addr, unsigned long count) { asm volatile("rep; outs" "b" : "+S"(addr), "+c"(count) : "d"(port)); } static inline __attribute__((no_instrument_function)) void insb(int port, void *addr, unsigned long count) { asm volatile("rep; ins" "b" : "+D"(addr), "+c"(count) : "d"(port)); }
static inline __attribute__((no_instrument_function)) void outw(unsigned short value, int port) { asm volatile("out" "w" " %" "w" "0, %w1" : : "a"(value), "Nd"(port)); } static inline __attribute__((no_instrument_function)) unsigned short inw(int port) { unsigned short value; asm volatile("in" "w" " %w1, %" "w" "0" : "=a"(value) : "Nd"(port)); return value; } static inline __attribute__((no_instrument_function)) void outw_p(unsigned short value, int port) { outw(value, port); slow_down_io(); } static inline __attribute__((no_instrument_function)) unsigned short inw_p(int port) { unsigned short value = inw(port); slow_down_io(); return value; } static inline __attribute__((no_instrument_function)) void outsw(int port, const void *addr, unsigned long count) { asm volatile("rep; outs" "w" : "+S"(addr), "+c"(count) : "d"(port)); } static inline __attribute__((no_instrument_function)) void insw(int port, void *addr, unsigned long count) { asm volatile("rep; ins" "w" : "+D"(addr), "+c"(count) : "d"(port)); }
static inline __attribute__((no_instrument_function)) void outl(unsigned int value, int port) { asm volatile("out" "l" " %" "" "0, %w1" : : "a"(value), "Nd"(port)); } static inline __attribute__((no_instrument_function)) unsigned int inl(int port) { unsigned int value; asm volatile("in" "l" " %w1, %" "" "0" : "=a"(value) : "Nd"(port)); return value; } static inline __attribute__((no_instrument_function)) void outl_p(unsigned int value, int port) { outl(value, port); slow_down_io(); } static inline __attribute__((no_instrument_function)) unsigned int inl_p(int port) { unsigned int value = inl(port); slow_down_io(); return value; } static inline __attribute__((no_instrument_function)) void outsl(int port, const void *addr, unsigned long count) { asm volatile("rep; outs" "l" : "+S"(addr), "+c"(count) : "d"(port)); } static inline __attribute__((no_instrument_function)) void insl(int port, void *addr, unsigned long count) { asm volatile("rep; ins" "l" : "+D"(addr), "+c"(count) : "d"(port)); }

extern void *xlate_dev_mem_ptr(unsigned long phys);
extern void unxlate_dev_mem_ptr(unsigned long phys, void *addr);

extern int ioremap_change_attr(unsigned long vaddr, unsigned long size,
    unsigned long prot_val);
extern void *ioremap_wc(resource_size_t offset, unsigned long size);

extern bool is_early_ioremap_ptep(pte_t *ptep);


# 1 "include/xen/xen.h" 1



enum xen_domain_type {
 XEN_NATIVE,
 XEN_PV_DOMAIN,
 XEN_HVM_DOMAIN,
};


extern enum xen_domain_type xen_domain_type;
# 23 "include/xen/xen.h"
# 1 "include/xen/interface/xen.h" 1
# 12 "include/xen/interface/xen.h"
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/xen/interface.h" 1
# 53 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/xen/interface.h"
typedef unsigned long xen_pfn_t;

typedef unsigned long xen_ulong_t;


typedef unsigned char * __guest_handle_uchar;
typedef unsigned int * __guest_handle_uint;
typedef char * __guest_handle_char;
typedef int * __guest_handle_int;
typedef void * __guest_handle_void;
typedef uint64_t * __guest_handle_uint64_t;
typedef uint32_t * __guest_handle_uint32_t;
typedef xen_pfn_t * __guest_handle_xen_pfn_t;
typedef xen_ulong_t * __guest_handle_xen_ulong_t;
# 109 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/xen/interface.h"
struct trap_info {
    uint8_t vector;
    uint8_t flags;
    uint16_t cs;
    unsigned long address;
};
typedef struct trap_info * __guest_handle_trap_info;

struct arch_shared_info {
    unsigned long max_pfn;

    unsigned long pfn_to_mfn_frame_list_list;
    unsigned long nmi_reason;
};





# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/xen/interface_64.h" 1
# 81 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/xen/interface_64.h"
struct iret_context {

    uint64_t rax, r11, rcx, flags, rip, cs, rflags, rsp, ss;

};
# 98 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/xen/interface_64.h"
struct cpu_user_regs {
    uint64_t r15;
    uint64_t r14;
    uint64_t r13;
    uint64_t r12;
    union { uint64_t rbp, ebp; uint32_t _ebp; };
    union { uint64_t rbx, ebx; uint32_t _ebx; };
    uint64_t r11;
    uint64_t r10;
    uint64_t r9;
    uint64_t r8;
    union { uint64_t rax, eax; uint32_t _eax; };
    union { uint64_t rcx, ecx; uint32_t _ecx; };
    union { uint64_t rdx, edx; uint32_t _edx; };
    union { uint64_t rsi, esi; uint32_t _esi; };
    union { uint64_t rdi, edi; uint32_t _edi; };
    uint32_t error_code;
    uint32_t entry_vector;
    union { uint64_t rip, eip; uint32_t _eip; };
    uint16_t cs, _pad0[1];
    uint8_t saved_upcall_mask;
    uint8_t _pad1[3];
    union { uint64_t rflags, eflags; uint32_t _eflags; };
    union { uint64_t rsp, esp; uint32_t _esp; };
    uint16_t ss, _pad2[3];
    uint16_t es, _pad3[3];
    uint16_t ds, _pad4[3];
    uint16_t fs, _pad5[3];
    uint16_t gs, _pad6[3];
};
typedef struct cpu_user_regs * __guest_handle_cpu_user_regs;






struct arch_vcpu_info {
    unsigned long cr2;
    unsigned long pad;
};

typedef unsigned long xen_callback_t;
# 129 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/xen/interface.h" 2


# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/pvclock-abi.h" 1
# 25 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/pvclock-abi.h"
struct pvclock_vcpu_time_info {
 u32 version;
 u32 pad0;
 u64 tsc_timestamp;
 u64 system_time;
 u32 tsc_to_system_mul;
 s8 tsc_shift;
 u8 flags;
 u8 pad[2];
} __attribute__((__packed__));

struct pvclock_wall_clock {
 u32 version;
 u32 sec;
 u32 nsec;
} __attribute__((__packed__));
# 132 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/xen/interface.h" 2






struct vcpu_guest_context {

    struct { char x[512]; } fpu_ctxt;



    unsigned long flags;
    struct cpu_user_regs user_regs;
    struct trap_info trap_ctxt[256];
    unsigned long ldt_base, ldt_ents;
    unsigned long gdt_frames[16], gdt_ents;
    unsigned long kernel_ss, kernel_sp;

    unsigned long ctrlreg[8];
    unsigned long debugreg[8];






    unsigned long event_callback_eip;
    unsigned long failsafe_callback_eip;
    unsigned long syscall_callback_eip;

    unsigned long vm_assist;


    uint64_t fs_base;
    uint64_t gs_base_kernel;
    uint64_t gs_base_user;

};
typedef struct vcpu_guest_context * __guest_handle_vcpu_guest_context;
# 13 "include/xen/interface/xen.h" 2
# 188 "include/xen/interface/xen.h"
struct mmuext_op {
 unsigned int cmd;
 union {

  xen_pfn_t mfn;

  unsigned long linear_addr;
 } arg1;
 union {

  unsigned int nr_ents;

  void *vcpumask;
 } arg2;
};
typedef struct mmuext_op * __guest_handle_mmuext_op;
# 236 "include/xen/interface/xen.h"
typedef uint16_t domid_t;
# 267 "include/xen/interface/xen.h"
struct mmu_update {
    uint64_t ptr;
    uint64_t val;
};
typedef struct mmu_update * __guest_handle_mmu_update;





struct multicall_entry {
    unsigned long op;
    long result;
    unsigned long args[6];
};
typedef struct multicall_entry * __guest_handle_multicall_entry;

struct vcpu_time_info {
# 295 "include/xen/interface/xen.h"
 uint32_t version;
 uint32_t pad0;
 uint64_t tsc_timestamp;
 uint64_t system_time;






 uint32_t tsc_to_system_mul;
 int8_t tsc_shift;
 int8_t pad1[3];
};

struct vcpu_info {
# 336 "include/xen/interface/xen.h"
 uint8_t evtchn_upcall_pending;
 uint8_t evtchn_upcall_mask;
 xen_ulong_t evtchn_pending_sel;
 struct arch_vcpu_info arch;
 struct pvclock_vcpu_time_info time;
};





struct shared_info {
 struct vcpu_info vcpu_info[32];
# 381 "include/xen/interface/xen.h"
 xen_ulong_t evtchn_pending[sizeof(xen_ulong_t) * 8];
 xen_ulong_t evtchn_mask[sizeof(xen_ulong_t) * 8];





 struct pvclock_wall_clock wc;

 struct arch_shared_info arch;

};
# 420 "include/xen/interface/xen.h"
struct start_info {

 char magic[32];
 unsigned long nr_pages;
 unsigned long shared_info;
 uint32_t flags;
 xen_pfn_t store_mfn;
 uint32_t store_evtchn;
 union {
  struct {
   xen_pfn_t mfn;
   uint32_t evtchn;
  } domU;
  struct {
   uint32_t info_off;
   uint32_t info_size;
  } dom0;
 } console;

 unsigned long pt_base;
 unsigned long nr_pt_frames;
 unsigned long mfn_list;
 unsigned long mod_start;
 unsigned long mod_len;
 int8_t cmd_line[1024];
};

struct dom0_vga_console_info {
 uint8_t video_type;




 union {
  struct {

   uint16_t font_height;

   uint16_t cursor_x, cursor_y;

   uint16_t rows, columns;
  } text_mode_3;

  struct {

   uint16_t width, height;

   uint16_t bytes_per_line;

   uint16_t bits_per_pixel;

   uint32_t lfb_base;
   uint32_t lfb_size;

   uint8_t red_pos, red_size;
   uint8_t green_pos, green_size;
   uint8_t blue_pos, blue_size;
   uint8_t rsvd_pos, rsvd_size;


   uint32_t gbl_caps;

   uint16_t mode_attrs;
  } vesa_lfb;
 } u;
};






typedef uint64_t cpumap_t;

typedef uint8_t xen_domain_handle_t[16];







struct tmem_op {
 uint32_t cmd;
 int32_t pool_id;
 union {
  struct {
   uint64_t uuid[2];
   uint32_t flags;
  } new;
  struct {
   uint64_t oid[3];
   uint32_t index;
   uint32_t tmem_offset;
   uint32_t pfn_offset;
   uint32_t len;
   __guest_handle_void gmfn;
  } gen;
 } u;
};

typedef u64 * __guest_handle_u64;
# 24 "include/xen/xen.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/xen/hypervisor.h" 1
# 36 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/xen/hypervisor.h"
extern struct shared_info *HYPERVISOR_shared_info;
extern struct start_info *xen_start_info;



static inline __attribute__((no_instrument_function)) uint32_t xen_cpuid_base(void)
{
 return hypervisor_cpuid_base("XenVMMXenVMM", 2);
}


extern bool xen_hvm_need_lapic(void);

static inline __attribute__((no_instrument_function)) bool xen_x2apic_para_available(void)
{
 return xen_hvm_need_lapic();
}
# 25 "include/xen/xen.h" 2
# 39 "include/xen/xen.h"
# 1 "include/xen/features.h" 1
# 12 "include/xen/features.h"
# 1 "include/xen/interface/features.h" 1
# 13 "include/xen/features.h" 2

void xen_setup_features(void);

extern u8 xen_features[1 * 32];

static inline __attribute__((no_instrument_function)) int xen_feature(int flag)
{
 return xen_features[flag];
}
# 40 "include/xen/xen.h" 2
# 324 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/io.h" 2
struct bio_vec;

extern bool xen_biovec_phys_mergeable(const struct bio_vec *vec1,
          const struct bio_vec *vec2);
# 337 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/io.h"
extern int __attribute__((warn_unused_result)) arch_phys_wc_add(unsigned long base,
      unsigned long size);
extern void arch_phys_wc_del(int handle);
# 6 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/realmode.h" 2


struct real_mode_header {
 u32 text_start;
 u32 ro_end;

 u32 trampoline_start;
 u32 trampoline_status;
 u32 trampoline_header;

 u32 trampoline_pgd;



 u32 wakeup_start;
 u32 wakeup_header;


 u32 machine_real_restart_asm;

 u32 machine_real_restart_seg;

};


struct trampoline_header {






 u64 start;
 u64 efer;
 u32 cr4;

};

extern struct real_mode_header *real_mode_header;
extern unsigned char real_mode_blob_end[];

extern unsigned long init_rsp;
extern unsigned long initial_code;
extern unsigned long initial_gs;

extern unsigned char real_mode_blob[];
extern unsigned char real_mode_relocs[];





extern unsigned char secondary_startup_64[];


void reserve_real_mode(void);
void setup_real_mode(void);
# 34 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/acpi.h" 2
# 55 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/acpi.h"
int __acpi_acquire_global_lock(unsigned int *lock);
int __acpi_release_global_lock(unsigned int *lock);
# 81 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/acpi.h"
extern int acpi_lapic;
extern int acpi_ioapic;
extern int acpi_noirq;
extern int acpi_strict;
extern int acpi_disabled;
extern int acpi_pci_disabled;
extern int acpi_skip_timer_override;
extern int acpi_use_timer_override;
extern int acpi_fix_pin2_polarity;
extern int acpi_disable_cmcff;

extern u8 acpi_sci_flags;
extern int acpi_sci_override_gsi;
void acpi_pic_sci_set_trigger(unsigned int, u16);

extern int (*__acpi_register_gsi)(struct device *dev, u32 gsi,
      int trigger, int polarity);

static inline __attribute__((no_instrument_function)) void disable_acpi(void)
{
 acpi_disabled = 1;
 acpi_pci_disabled = 1;
 acpi_noirq = 1;
}

extern int acpi_gsi_to_irq(u32 gsi, unsigned int *irq);

static inline __attribute__((no_instrument_function)) void acpi_noirq_set(void) { acpi_noirq = 1; }
static inline __attribute__((no_instrument_function)) void acpi_disable_pci(void)
{
 acpi_pci_disabled = 1;
 acpi_noirq_set();
}


extern int (*acpi_suspend_lowlevel)(void);







static inline __attribute__((no_instrument_function)) unsigned int acpi_processor_cstate_check(unsigned int max_cstate)
{






 if (boot_cpu_data.x86 == 0x0F &&
     boot_cpu_data.x86_vendor == 2 &&
     boot_cpu_data.x86_model <= 0x05 &&
     boot_cpu_data.x86_mask < 0x0A)
  return 1;
 else if (amd_e400_c1e_detected)
  return 1;
 else
  return max_cstate;
}

static inline __attribute__((no_instrument_function)) bool arch_has_acpi_pdc(void)
{
 struct cpuinfo_x86 *c = &(*({ do { const void *__vpp_verify = (typeof(((&(cpu_info))) + 0))((void *)0); (void)__vpp_verify; } while (0); ({ unsigned long __ptr; __asm__ ("" : "=r"(__ptr) : "0"((typeof(*(&(cpu_info))) *)(&(cpu_info)))); (typeof((typeof(*(&(cpu_info))) *)(&(cpu_info)))) (__ptr + (((__per_cpu_offset[0])))); }); }));
 return (c->x86_vendor == 0 ||
  c->x86_vendor == 5);
}

static inline __attribute__((no_instrument_function)) void arch_acpi_set_pdc_bits(u32 *buf)
{
 struct cpuinfo_x86 *c = &(*({ do { const void *__vpp_verify = (typeof(((&(cpu_info))) + 0))((void *)0); (void)__vpp_verify; } while (0); ({ unsigned long __ptr; __asm__ ("" : "=r"(__ptr) : "0"((typeof(*(&(cpu_info))) *)(&(cpu_info)))); (typeof((typeof(*(&(cpu_info))) *)(&(cpu_info)))) (__ptr + (((__per_cpu_offset[0])))); }); }));

 buf[2] |= ((0x0010) | (0x0008) | (0x0002) | (0x0100) | (0x0200));

 if ((__builtin_constant_p((4*32+ 7)) && ( ((((4*32+ 7))>>5)==0 && (1UL<<(((4*32+ 7))&31) & ((1<<((0*32+ 0) & 31))|0|(1<<((0*32+ 5) & 31))|(1<<((0*32+ 6) & 31))| (1<<((0*32+ 8) & 31))|0|(1<<((0*32+24) & 31))|(1<<((0*32+15) & 31))| (1<<((0*32+25) & 31))|(1<<((0*32+26) & 31))))) || ((((4*32+ 7))>>5)==1 && (1UL<<(((4*32+ 7))&31) & ((1<<((1*32+29) & 31))|0))) || ((((4*32+ 7))>>5)==2 && (1UL<<(((4*32+ 7))&31) & 0)) || ((((4*32+ 7))>>5)==3 && (1UL<<(((4*32+ 7))&31) & ((1<<((3*32+20) & 31))))) || ((((4*32+ 7))>>5)==4 && (1UL<<(((4*32+ 7))&31) & (0))) || ((((4*32+ 7))>>5)==5 && (1UL<<(((4*32+ 7))&31) & 0)) || ((((4*32+ 7))>>5)==6 && (1UL<<(((4*32+ 7))&31) & 0)) || ((((4*32+ 7))>>5)==7 && (1UL<<(((4*32+ 7))&31) & 0)) || ((((4*32+ 7))>>5)==8 && (1UL<<(((4*32+ 7))&31) & 0)) || ((((4*32+ 7))>>5)==9 && (1UL<<(((4*32+ 7))&31) & 0)) ) ? 1 : (__builtin_constant_p(((4*32+ 7))) ? constant_test_bit(((4*32+ 7)), ((unsigned long *)((c)->x86_capability))) : variable_test_bit(((4*32+ 7)), ((unsigned long *)((c)->x86_capability))))))
  buf[2] |= ((0x0008) | (0x0002) | (0x0020) | (0x0800) | (0x0001));

 if ((__builtin_constant_p((0*32+22)) && ( ((((0*32+22))>>5)==0 && (1UL<<(((0*32+22))&31) & ((1<<((0*32+ 0) & 31))|0|(1<<((0*32+ 5) & 31))|(1<<((0*32+ 6) & 31))| (1<<((0*32+ 8) & 31))|0|(1<<((0*32+24) & 31))|(1<<((0*32+15) & 31))| (1<<((0*32+25) & 31))|(1<<((0*32+26) & 31))))) || ((((0*32+22))>>5)==1 && (1UL<<(((0*32+22))&31) & ((1<<((1*32+29) & 31))|0))) || ((((0*32+22))>>5)==2 && (1UL<<(((0*32+22))&31) & 0)) || ((((0*32+22))>>5)==3 && (1UL<<(((0*32+22))&31) & ((1<<((3*32+20) & 31))))) || ((((0*32+22))>>5)==4 && (1UL<<(((0*32+22))&31) & (0))) || ((((0*32+22))>>5)==5 && (1UL<<(((0*32+22))&31) & 0)) || ((((0*32+22))>>5)==6 && (1UL<<(((0*32+22))&31) & 0)) || ((((0*32+22))>>5)==7 && (1UL<<(((0*32+22))&31) & 0)) || ((((0*32+22))>>5)==8 && (1UL<<(((0*32+22))&31) & 0)) || ((((0*32+22))>>5)==9 && (1UL<<(((0*32+22))&31) & 0)) ) ? 1 : (__builtin_constant_p(((0*32+22))) ? constant_test_bit(((0*32+22)), ((unsigned long *)((c)->x86_capability))) : variable_test_bit(((0*32+22)), ((unsigned long *)((c)->x86_capability))))))
  buf[2] |= (0x0004);




 if (!(__builtin_constant_p((4*32+ 3)) && ( ((((4*32+ 3))>>5)==0 && (1UL<<(((4*32+ 3))&31) & ((1<<((0*32+ 0) & 31))|0|(1<<((0*32+ 5) & 31))|(1<<((0*32+ 6) & 31))| (1<<((0*32+ 8) & 31))|0|(1<<((0*32+24) & 31))|(1<<((0*32+15) & 31))| (1<<((0*32+25) & 31))|(1<<((0*32+26) & 31))))) || ((((4*32+ 3))>>5)==1 && (1UL<<(((4*32+ 3))&31) & ((1<<((1*32+29) & 31))|0))) || ((((4*32+ 3))>>5)==2 && (1UL<<(((4*32+ 3))&31) & 0)) || ((((4*32+ 3))>>5)==3 && (1UL<<(((4*32+ 3))&31) & ((1<<((3*32+20) & 31))))) || ((((4*32+ 3))>>5)==4 && (1UL<<(((4*32+ 3))&31) & (0))) || ((((4*32+ 3))>>5)==5 && (1UL<<(((4*32+ 3))&31) & 0)) || ((((4*32+ 3))>>5)==6 && (1UL<<(((4*32+ 3))&31) & 0)) || ((((4*32+ 3))>>5)==7 && (1UL<<(((4*32+ 3))&31) & 0)) || ((((4*32+ 3))>>5)==8 && (1UL<<(((4*32+ 3))&31) & 0)) || ((((4*32+ 3))>>5)==9 && (1UL<<(((4*32+ 3))&31) & 0)) ) ? 1 : (__builtin_constant_p(((4*32+ 3))) ? constant_test_bit(((4*32+ 3)), ((unsigned long *)((c)->x86_capability))) : variable_test_bit(((4*32+ 3)), ((unsigned long *)((c)->x86_capability))))))
  buf[2] &= ~((0x0200));
}
# 183 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/acpi.h"
extern int acpi_numa;
extern int x86_acpi_numa_init(void);
# 20 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/fixmap.h" 2


# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/pvclock.h" 1



# 1 "include/linux/clocksource.h" 1
# 22 "include/linux/clocksource.h"
typedef u64 cycle_t;
struct clocksource;
struct module;


# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/clocksource.h" 1
# 11 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/clocksource.h"
struct arch_clocksource_data {
 int vclock_mode;
};
# 28 "include/linux/clocksource.h" 2
# 44 "include/linux/clocksource.h"
struct cyclecounter {
 cycle_t (*read)(const struct cyclecounter *cc);
 cycle_t mask;
 u32 mult;
 u32 shift;
};
# 67 "include/linux/clocksource.h"
struct timecounter {
 const struct cyclecounter *cc;
 cycle_t cycle_last;
 u64 nsec;
};
# 81 "include/linux/clocksource.h"
static inline __attribute__((no_instrument_function)) u64 cyclecounter_cyc2ns(const struct cyclecounter *cc,
          cycle_t cycles)
{
 u64 ret = (u64)cycles;
 ret = (ret * cc->mult) >> cc->shift;
 return ret;
}
# 99 "include/linux/clocksource.h"
extern void timecounter_init(struct timecounter *tc,
        const struct cyclecounter *cc,
        u64 start_tstamp);
# 111 "include/linux/clocksource.h"
extern u64 timecounter_read(struct timecounter *tc);
# 127 "include/linux/clocksource.h"
extern u64 timecounter_cyc2time(struct timecounter *tc,
    cycle_t cycle_tstamp);
# 168 "include/linux/clocksource.h"
struct clocksource {




 cycle_t (*read)(struct clocksource *cs);
 cycle_t cycle_last;
 cycle_t mask;
 u32 mult;
 u32 shift;
 u64 max_idle_ns;
 u32 maxadj;

 struct arch_clocksource_data archdata;


 const char *name;
 struct list_head list;
 int rating;
 int (*enable)(struct clocksource *cs);
 void (*disable)(struct clocksource *cs);
 unsigned long flags;
 void (*suspend)(struct clocksource *cs);
 void (*resume)(struct clocksource *cs);




 struct list_head wd_list;
 cycle_t cs_last;
 cycle_t wd_last;

 struct module *owner;
} __attribute__((__aligned__((1 << (6)))));
# 226 "include/linux/clocksource.h"
static inline __attribute__((no_instrument_function)) u32 clocksource_khz2mult(u32 khz, u32 shift_constant)
{







 u64 tmp = ((u64)1000000) << shift_constant;

 tmp += khz/2;
 ({ uint32_t __base = (khz); uint32_t __rem; __rem = ((uint64_t)(tmp)) % __base; (tmp) = ((uint64_t)(tmp)) / __base; __rem; });

 return (u32)tmp;
}
# 252 "include/linux/clocksource.h"
static inline __attribute__((no_instrument_function)) u32 clocksource_hz2mult(u32 hz, u32 shift_constant)
{







 u64 tmp = ((u64)1000000000) << shift_constant;

 tmp += hz/2;
 ({ uint32_t __base = (hz); uint32_t __rem; __rem = ((uint64_t)(tmp)) % __base; (tmp) = ((uint64_t)(tmp)) / __base; __rem; });

 return (u32)tmp;
}
# 279 "include/linux/clocksource.h"
static inline __attribute__((no_instrument_function)) s64 clocksource_cyc2ns(cycle_t cycles, u32 mult, u32 shift)
{
 return ((u64) cycles * mult) >> shift;
}


extern int clocksource_register(struct clocksource*);
extern int clocksource_unregister(struct clocksource*);
extern void clocksource_touch_watchdog(void);
extern struct clocksource* clocksource_get_next(void);
extern void clocksource_change_rating(struct clocksource *cs, int rating);
extern void clocksource_suspend(void);
extern void clocksource_resume(void);
extern struct clocksource * __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) __attribute__((weak)) clocksource_default_clock(void);
extern void clocksource_mark_unstable(struct clocksource *cs);

extern u64
clocks_calc_max_nsecs(u32 mult, u32 shift, u32 maxadj, u64 mask);
extern void
clocks_calc_mult_shift(u32 *mult, u32 *shift, u32 from, u32 to, u32 minsec);





extern int
__clocksource_register_scale(struct clocksource *cs, u32 scale, u32 freq);
extern void
__clocksource_updatefreq_scale(struct clocksource *cs, u32 scale, u32 freq);

static inline __attribute__((no_instrument_function)) int clocksource_register_hz(struct clocksource *cs, u32 hz)
{
 return __clocksource_register_scale(cs, 1, hz);
}

static inline __attribute__((no_instrument_function)) int clocksource_register_khz(struct clocksource *cs, u32 khz)
{
 return __clocksource_register_scale(cs, 1000, khz);
}

static inline __attribute__((no_instrument_function)) void __clocksource_updatefreq_hz(struct clocksource *cs, u32 hz)
{
 __clocksource_updatefreq_scale(cs, 1, hz);
}

static inline __attribute__((no_instrument_function)) void __clocksource_updatefreq_khz(struct clocksource *cs, u32 khz)
{
 __clocksource_updatefreq_scale(cs, 1000, khz);
}


extern int timekeeping_notify(struct clocksource *clock);

extern cycle_t clocksource_mmio_readl_up(struct clocksource *);
extern cycle_t clocksource_mmio_readl_down(struct clocksource *);
extern cycle_t clocksource_mmio_readw_up(struct clocksource *);
extern cycle_t clocksource_mmio_readw_down(struct clocksource *);

extern int clocksource_mmio_init(void *, const char *,
 unsigned long, int, unsigned, cycle_t (*)(struct clocksource *));

extern int clocksource_i8253_init(void);

struct device_node;
typedef void(*clocksource_of_init_fn)(struct device_node *);
# 353 "include/linux/clocksource.h"
static inline __attribute__((no_instrument_function)) void clocksource_of_init(void) {}
# 5 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/pvclock.h" 2



cycle_t pvclock_clocksource_read(struct pvclock_vcpu_time_info *src);
u8 pvclock_read_flags(struct pvclock_vcpu_time_info *src);
void pvclock_set_flags(u8 flags);
unsigned long pvclock_tsc_khz(struct pvclock_vcpu_time_info *src);
void pvclock_read_wallclock(struct pvclock_wall_clock *wall,
       struct pvclock_vcpu_time_info *vcpu,
       struct timespec *ts);
void pvclock_resume(void);

void pvclock_touch_watchdogs(void);





static inline __attribute__((no_instrument_function)) u64 pvclock_scale_delta(u64 delta, u32 mul_frac, int shift)
{
 u64 product;



 ulong tmp;


 if (shift < 0)
  delta >>= -shift;
 else
  delta <<= shift;
# 49 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/pvclock.h"
 __asm__ (
  "mulq %[mul_frac] ; shrd $32, %[hi], %[lo]"
  : [lo]"=a"(product),
    [hi]"=d"(tmp)
  : "0"(delta),
    [mul_frac]"rm"((u64)mul_frac));




 return product;
}

static inline __attribute__((no_instrument_function)) __attribute__((always_inline))
u64 pvclock_get_nsec_offset(const struct pvclock_vcpu_time_info *src)
{
 u64 delta = __native_read_tsc() - src->tsc_timestamp;
 return pvclock_scale_delta(delta, src->tsc_to_system_mul,
       src->tsc_shift);
}

static inline __attribute__((no_instrument_function)) __attribute__((always_inline))
unsigned __pvclock_read_cycles(const struct pvclock_vcpu_time_info *src,
          cycle_t *cycles, u8 *flags)
{
 unsigned version;
 cycle_t ret, offset;
 u8 ret_flags;

 version = src->version;






 rdtsc_barrier();
 offset = pvclock_get_nsec_offset(src);
 ret = src->system_time + offset;
 ret_flags = src->flags;
 rdtsc_barrier();

 *cycles = ret;
 *flags = ret_flags;
 return version;
}

struct pvclock_vsyscall_time_info {
 struct pvclock_vcpu_time_info pvti;
} __attribute__((__aligned__((1 << (6)))));




int __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) pvclock_init_vsyscall(struct pvclock_vsyscall_time_info *i,
     int size);
struct pvclock_vcpu_time_info *pvclock_get_vsyscall_time_info(int cpu);
# 23 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/fixmap.h" 2




# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/vsyscall.h" 1




# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/uapi/asm/vsyscall.h" 1



enum vsyscall_num {
 __NR_vgettimeofday,
 __NR_vtime,
 __NR_vgetcpu,
};
# 6 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/vsyscall.h" 2





extern int vgetcpu_mode;
extern struct timezone sys_tz;

# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/vvar.h" 1
# 41 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/vvar.h"
extern char __vvar_page;
# 65 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/vvar.h"
static volatile unsigned long const * const vvaraddr_jiffies = (void *)((-10*1024*1024 - 4096) + (0));
static int const * const vvaraddr_vgetcpu_mode = (void *)((-10*1024*1024 - 4096) + (16));
static struct vsyscall_gtod_data const * const vvaraddr_vsyscall_gtod_data = (void *)((-10*1024*1024 - 4096) + (128));
# 15 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/vsyscall.h" 2

extern void map_vsyscall(void);





extern bool emulate_vsyscall(struct pt_regs *regs, unsigned long address);





static inline __attribute__((no_instrument_function)) unsigned int __getcpu(void)
{
 unsigned int p;

 if ((*vvaraddr_vgetcpu_mode) == 1) {

  native_read_tscp(&p);
 } else {

  asm("lsl %1,%0" : "=r" (p) : "r" ((15 * 8 + 3)));
 }

 return p;
}
# 28 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/fixmap.h" 2
# 67 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/fixmap.h"
enum fixed_addresses {



 VSYSCALL_LAST_PAGE,
 VSYSCALL_FIRST_PAGE = VSYSCALL_LAST_PAGE
       + (((-2UL << 20)-(-10UL << 20)) >> 12) - 1,
 VVAR_PAGE,
 VSYSCALL_HPET,

 PVCLOCK_FIXMAP_BEGIN,
 PVCLOCK_FIXMAP_END = PVCLOCK_FIXMAP_BEGIN+(((8192 -1)/(((1UL) << 12)/sizeof(struct pvclock_vsyscall_time_info)))+1)-1,


 FIX_DBGP_BASE,
 FIX_EARLYCON_MEM_BASE,

 FIX_OHCI1394_BASE,


 FIX_APIC_BASE,


 FIX_IO_APIC_BASE_0,
 FIX_IO_APIC_BASE_END = FIX_IO_APIC_BASE_0 + 128 - 1,

 FIX_RO_IDT,
# 102 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/fixmap.h"
 FIX_PARAVIRT_BOOTMAP,

 FIX_TEXT_POKE1,
 FIX_TEXT_POKE0,



 __end_of_permanent_fixed_addresses,
# 121 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/fixmap.h"
 FIX_BTMAP_END =
  (__end_of_permanent_fixed_addresses ^
   (__end_of_permanent_fixed_addresses + (64 * 4) - 1)) &
  -512
  ? __end_of_permanent_fixed_addresses + (64 * 4) -
    (__end_of_permanent_fixed_addresses & ((64 * 4) - 1))
  : __end_of_permanent_fixed_addresses,
 FIX_BTMAP_BEGIN = FIX_BTMAP_END + (64 * 4) - 1,




 FIX_TBOOT_BASE,

 __end_of_fixed_addresses
};


extern void reserve_top_address(unsigned long reserve);






extern int fixmaps_set;

extern pte_t *kmap_pte;
extern pgprot_t kmap_prot;
extern pte_t *pkmap_page_table;

void __native_set_fixmap(enum fixed_addresses idx, pte_t pte);
void native_set_fixmap(enum fixed_addresses idx,
         phys_addr_t phys, pgprot_t flags);
# 164 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/fixmap.h"
# 1 "include/asm-generic/fixmap.h" 1
# 29 "include/asm-generic/fixmap.h"
static inline __attribute__((no_instrument_function)) __attribute__((always_inline)) unsigned long fix_to_virt(const unsigned int idx)
{
 ;
 return (((-2UL << 20)-((1UL) << 12)) - ((idx) << 12));
}

static inline __attribute__((no_instrument_function)) unsigned long virt_to_fix(const unsigned long vaddr)
{
 do { if (__builtin_expect(!!(vaddr >= ((-2UL << 20)-((1UL) << 12)) || vaddr < (((-2UL << 20)-((1UL) << 12)) - (__end_of_permanent_fixed_addresses << 12))), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("include/asm-generic/fixmap.h"), "i" (37), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0);
 return ((((-2UL << 20)-((1UL) << 12)) - ((vaddr)&(~(((1UL) << 12)-1)))) >> 12);
}
# 165 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/fixmap.h" 2




void __early_set_fixmap(enum fixed_addresses idx,
   phys_addr_t phys, pgprot_t flags);
# 13 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/apic.h" 2


# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/idle.h" 1






struct notifier_block;
void idle_notifier_register(struct notifier_block *n);
void idle_notifier_unregister(struct notifier_block *n);


void enter_idle(void);
void exit_idle(void);






void amd_e400_remove_cpu(int cpu);
# 16 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/apic.h" 2
# 41 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/apic.h"
static inline __attribute__((no_instrument_function)) void generic_apic_probe(void)
{
}




extern unsigned int apic_verbosity;
extern int local_apic_timer_c2_ok;

extern int disable_apic;
extern unsigned int lapic_timer_frequency;


extern void __inquire_remote_apic(int apicid);






static inline __attribute__((no_instrument_function)) void default_inquire_remote_apic(int apicid)
{
 if (apic_verbosity >= 2)
  __inquire_remote_apic(apicid);
}
# 76 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/apic.h"
static inline __attribute__((no_instrument_function)) bool apic_from_smp_config(void)
{
 return smp_found_config && !disable_apic;
}
# 89 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/apic.h"
extern int is_vsmp_box(void);






extern int setup_profiling_timer(unsigned int);

static inline __attribute__((no_instrument_function)) void native_apic_mem_write(u32 reg, u32 v)
{
 volatile u32 *addr = (volatile u32 *)((fix_to_virt(FIX_APIC_BASE)) + reg);

 asm volatile ("661:\n\t" "movl %0, %1" "\n662:\n" ".pushsection .altinstructions,\"a\"\n" " .long 661b - .\n" " .long " "663""1""f - .\n" " .word " "(3*32+19)" "\n" " .byte " "662b-661b" "\n" " .byte " "664""1""f-""663""1""f" "\n" ".popsection\n" ".pushsection .discard,\"aw\",@progbits\n" " .byte 0xff + (" "664""1""f-""663""1""f" ") - (" "662b-661b" ")\n" ".popsection\n" ".pushsection .altinstr_replacement, \"ax\"\n" "663""1"":\n\t" "xchgl %0, %1" "\n" "664""1" ":\n\t" ".popsection" : "=r" (v), "=m" (*addr) : "i" (0), "0" (v), "m" (*addr))

                                           ;
}

static inline __attribute__((no_instrument_function)) u32 native_apic_mem_read(u32 reg)
{
 return *((volatile u32 *)((fix_to_virt(FIX_APIC_BASE)) + reg));
}

extern void native_apic_wait_icr_idle(void);
extern u32 native_safe_apic_wait_icr_idle(void);
extern void native_apic_icr_write(u32 low, u32 id);
extern u64 native_apic_icr_read(void);

extern int x2apic_mode;







static inline __attribute__((no_instrument_function)) void x2apic_wrmsr_fence(void)
{
 asm volatile("mfence" : : : "memory");
}

static inline __attribute__((no_instrument_function)) void native_apic_msr_write(u32 reg, u32 v)
{
 if (reg == 0xE0 || reg == 0x20 || reg == 0xD0 ||
     reg == 0x30)
  return;

 do { paravirt_write_msr(0x800 + (reg >> 4), v, 0); } while (0);
}

static inline __attribute__((no_instrument_function)) void native_apic_msr_eoi_write(u32 reg, u32 v)
{
 do { paravirt_write_msr(0x800 + (0xB0 >> 4), 0x0, 0); } while (0);
}

static inline __attribute__((no_instrument_function)) u32 native_apic_msr_read(u32 reg)
{
 u64 msr;

 if (reg == 0xE0)
  return -1;

 do { int _err; msr = paravirt_read_msr(0x800 + (reg >> 4), &_err); } while (0);
 return (u32)msr;
}

static inline __attribute__((no_instrument_function)) void native_x2apic_wait_icr_idle(void)
{

 return;
}

static inline __attribute__((no_instrument_function)) u32 native_safe_x2apic_wait_icr_idle(void)
{

 return 0;
}

static inline __attribute__((no_instrument_function)) void native_x2apic_icr_write(u32 low, u32 id)
{
 do { paravirt_write_msr(0x800 + (0x300 >> 4), (u32)((u64)(((__u64) id) << 32 | low)), ((u64)(((__u64) id) << 32 | low))>>32); } while (0);
}

static inline __attribute__((no_instrument_function)) u64 native_x2apic_icr_read(void)
{
 unsigned long val;

 do { int _err; val = paravirt_read_msr(0x800 + (0x300 >> 4), &_err); } while (0);
 return val;
}

extern int x2apic_phys;
extern int x2apic_preenabled;
extern void check_x2apic(void);
extern void enable_x2apic(void);
static inline __attribute__((no_instrument_function)) int x2apic_enabled(void)
{
 u64 msr;

 if (!(__builtin_constant_p((4*32+21)) && ( ((((4*32+21))>>5)==0 && (1UL<<(((4*32+21))&31) & ((1<<((0*32+ 0) & 31))|0|(1<<((0*32+ 5) & 31))|(1<<((0*32+ 6) & 31))| (1<<((0*32+ 8) & 31))|0|(1<<((0*32+24) & 31))|(1<<((0*32+15) & 31))| (1<<((0*32+25) & 31))|(1<<((0*32+26) & 31))))) || ((((4*32+21))>>5)==1 && (1UL<<(((4*32+21))&31) & ((1<<((1*32+29) & 31))|0))) || ((((4*32+21))>>5)==2 && (1UL<<(((4*32+21))&31) & 0)) || ((((4*32+21))>>5)==3 && (1UL<<(((4*32+21))&31) & ((1<<((3*32+20) & 31))))) || ((((4*32+21))>>5)==4 && (1UL<<(((4*32+21))&31) & (0))) || ((((4*32+21))>>5)==5 && (1UL<<(((4*32+21))&31) & 0)) || ((((4*32+21))>>5)==6 && (1UL<<(((4*32+21))&31) & 0)) || ((((4*32+21))>>5)==7 && (1UL<<(((4*32+21))&31) & 0)) || ((((4*32+21))>>5)==8 && (1UL<<(((4*32+21))&31) & 0)) || ((((4*32+21))>>5)==9 && (1UL<<(((4*32+21))&31) & 0)) ) ? 1 : (__builtin_constant_p(((4*32+21))) ? constant_test_bit(((4*32+21)), ((unsigned long *)((&boot_cpu_data)->x86_capability))) : variable_test_bit(((4*32+21)), ((unsigned long *)((&boot_cpu_data)->x86_capability))))))
  return 0;

 do { int _err; msr = paravirt_read_msr(0x0000001b, &_err); } while (0);
 if (msr & (1UL << 10))
  return 1;
 return 0;
}


static inline __attribute__((no_instrument_function)) void x2apic_force_phys(void)
{
 x2apic_phys = 1;
}
# 224 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/apic.h"
extern void enable_IR_x2apic(void);

extern int get_physical_broadcast(void);

extern int lapic_get_maxlvt(void);
extern void clear_local_APIC(void);
extern void connect_bsp_APIC(void);
extern void disconnect_bsp_APIC(int virt_wire_setup);
extern void disable_local_APIC(void);
extern void lapic_shutdown(void);
extern int verify_local_APIC(void);
extern void sync_Arb_IDs(void);
extern void init_bsp_APIC(void);
extern void setup_local_APIC(void);
extern void end_local_APIC_setup(void);
extern void bsp_end_local_APIC_setup(void);
extern void init_apic_mappings(void);
void register_lapic_address(unsigned long address);
extern void setup_boot_APIC_clock(void);
extern void setup_secondary_APIC_clock(void);
extern int APIC_init_uniprocessor(void);
extern int apic_force_enable(unsigned long addr);





extern int apic_is_clustered_box(void);







extern int setup_APIC_eilvt(u8 lvt_off, u8 vector, u8 msg_type, u8 mask);
# 286 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/apic.h"
struct apic {
 char *name;

 int (*probe)(void);
 int (*acpi_madt_oem_check)(char *oem_id, char *oem_table_id);
 int (*apic_id_valid)(int apicid);
 int (*apic_id_registered)(void);

 u32 irq_delivery_mode;
 u32 irq_dest_mode;

 const struct cpumask *(*target_cpus)(void);

 int disable_esr;

 int dest_logical;
 unsigned long (*check_apicid_used)(physid_mask_t *map, int apicid);
 unsigned long (*check_apicid_present)(int apicid);

 void (*vector_allocation_domain)(int cpu, struct cpumask *retmask,
      const struct cpumask *mask);
 void (*init_apic_ldr)(void);

 void (*ioapic_phys_id_map)(physid_mask_t *phys_map, physid_mask_t *retmap);

 void (*setup_apic_routing)(void);
 int (*multi_timer_check)(int apic, int irq);
 int (*cpu_present_to_apicid)(int mps_cpu);
 void (*apicid_to_cpu_present)(int phys_apicid, physid_mask_t *retmap);
 void (*setup_portio_remap)(void);
 int (*check_phys_apicid_present)(int phys_apicid);
 void (*enable_apic_mode)(void);
 int (*phys_pkg_id)(int cpuid_apic, int index_msb);






 int (*mps_oem_check)(struct mpc_table *mpc, char *oem, char *productid);

 unsigned int (*get_apic_id)(unsigned long x);
 unsigned long (*set_apic_id)(unsigned int id);
 unsigned long apic_id_mask;

 int (*cpu_mask_to_apicid_and)(const struct cpumask *cpumask,
          const struct cpumask *andmask,
          unsigned int *apicid);


 void (*send_IPI_mask)(const struct cpumask *mask, int vector);
 void (*send_IPI_mask_allbutself)(const struct cpumask *mask,
      int vector);
 void (*send_IPI_allbutself)(int vector);
 void (*send_IPI_all)(int vector);
 void (*send_IPI_self)(int vector);


 int (*wakeup_secondary_cpu)(int apicid, unsigned long start_eip);

 int trampoline_phys_low;
 int trampoline_phys_high;

 bool wait_for_init_deassert;
 void (*smp_callin_clear_local_apic)(void);
 void (*inquire_remote_apic)(int apicid);


 u32 (*read)(u32 reg);
 void (*write)(u32 reg, u32 v);







 void (*eoi_write)(u32 reg, u32 v);
 u64 (*icr_read)(void);
 void (*icr_write)(u32 low, u32 high);
 void (*wait_icr_idle)(void);
 u32 (*safe_wait_icr_idle)(void);
# 390 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/apic.h"
};






extern struct apic *apic;
# 417 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/apic.h"
extern struct apic *__apicdrivers[], *__apicdrivers_end[];





extern atomic_t init_deasserted;
extern int wakeup_secondary_cpu_via_nmi(int apicid, unsigned long start_eip);




static inline __attribute__((no_instrument_function)) u32 apic_read(u32 reg)
{
 return apic->read(reg);
}

static inline __attribute__((no_instrument_function)) void apic_write(u32 reg, u32 val)
{
 apic->write(reg, val);
}

static inline __attribute__((no_instrument_function)) void apic_eoi(void)
{
 apic->eoi_write(0xB0, 0x0);
}

static inline __attribute__((no_instrument_function)) u64 apic_icr_read(void)
{
 return apic->icr_read();
}

static inline __attribute__((no_instrument_function)) void apic_icr_write(u32 low, u32 high)
{
 apic->icr_write(low, high);
}

static inline __attribute__((no_instrument_function)) void apic_wait_icr_idle(void)
{
 apic->wait_icr_idle();
}

static inline __attribute__((no_instrument_function)) u32 safe_apic_wait_icr_idle(void)
{
 return apic->safe_wait_icr_idle();
}

extern void __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) apic_set_eoi_write(void (*eoi_write)(u32 reg, u32 v));
# 479 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/apic.h"
static inline __attribute__((no_instrument_function)) void ack_APIC_irq(void)
{




 apic_eoi();
}

static inline __attribute__((no_instrument_function)) unsigned default_get_apic_id(unsigned long x)
{
 unsigned int ver = ((apic_read(0x30)) & 0xFFu);

 if (((ver) >= 0x14) || (__builtin_constant_p((3*32+26)) && ( ((((3*32+26))>>5)==0 && (1UL<<(((3*32+26))&31) & ((1<<((0*32+ 0) & 31))|0|(1<<((0*32+ 5) & 31))|(1<<((0*32+ 6) & 31))| (1<<((0*32+ 8) & 31))|0|(1<<((0*32+24) & 31))|(1<<((0*32+15) & 31))| (1<<((0*32+25) & 31))|(1<<((0*32+26) & 31))))) || ((((3*32+26))>>5)==1 && (1UL<<(((3*32+26))&31) & ((1<<((1*32+29) & 31))|0))) || ((((3*32+26))>>5)==2 && (1UL<<(((3*32+26))&31) & 0)) || ((((3*32+26))>>5)==3 && (1UL<<(((3*32+26))&31) & ((1<<((3*32+20) & 31))))) || ((((3*32+26))>>5)==4 && (1UL<<(((3*32+26))&31) & (0))) || ((((3*32+26))>>5)==5 && (1UL<<(((3*32+26))&31) & 0)) || ((((3*32+26))>>5)==6 && (1UL<<(((3*32+26))&31) & 0)) || ((((3*32+26))>>5)==7 && (1UL<<(((3*32+26))&31) & 0)) || ((((3*32+26))>>5)==8 && (1UL<<(((3*32+26))&31) & 0)) || ((((3*32+26))>>5)==9 && (1UL<<(((3*32+26))&31) & 0)) ) ? 1 : (__builtin_constant_p(((3*32+26))) ? constant_test_bit(((3*32+26)), ((unsigned long *)((&boot_cpu_data)->x86_capability))) : variable_test_bit(((3*32+26)), ((unsigned long *)((&boot_cpu_data)->x86_capability))))))
  return (x >> 24) & 0xFF;
 else
  return (x >> 24) & 0x0F;
}
# 505 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/apic.h"
extern int default_acpi_madt_oem_check(char *, char *);

extern void apic_send_IPI_self(int vector);

extern __attribute__((section(".discard"), unused)) char __pcpu_scope_x2apic_extra_bits; extern __attribute__((section(".data..percpu" ""))) __typeof__(int) x2apic_extra_bits;

extern int default_cpu_present_to_apicid(int mps_cpu);
extern int default_check_phys_apicid_present(int phys_apicid);


extern void generic_bigsmp_probe(void);




# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/smp.h" 1
# 521 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/apic.h" 2



static inline __attribute__((no_instrument_function)) const struct cpumask *default_target_cpus(void)
{

 return cpu_online_mask;



}

static inline __attribute__((no_instrument_function)) const struct cpumask *online_target_cpus(void)
{
 return cpu_online_mask;
}

extern __attribute__((section(".discard"), unused)) char __pcpu_scope_x86_bios_cpu_apicid; extern __attribute__((section(".data..percpu" "..readmostly"))) __typeof__(u16) x86_bios_cpu_apicid; extern __typeof__(u16) *x86_bios_cpu_apicid_early_ptr; extern __typeof__(u16) x86_bios_cpu_apicid_early_map[];


static inline __attribute__((no_instrument_function)) unsigned int read_apic_id(void)
{
 unsigned int reg;

 reg = apic_read(0x20);

 return apic->get_apic_id(reg);
}

static inline __attribute__((no_instrument_function)) int default_apic_id_valid(int apicid)
{
 return (apicid < 255);
}

extern void default_setup_apic_routing(void);

extern struct apic apic_noop;
# 587 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/apic.h"
static inline __attribute__((no_instrument_function)) int
flat_cpu_mask_to_apicid_and(const struct cpumask *cpumask,
       const struct cpumask *andmask,
       unsigned int *apicid)
{
 unsigned long cpu_mask = ((cpumask)->bits)[0] &
     ((andmask)->bits)[0] &
     ((cpu_online_mask)->bits)[0] &
     0xFFu;

 if (__builtin_expect(!!(cpu_mask), 1)) {
  *apicid = (unsigned int)cpu_mask;
  return 0;
 } else {
  return -22;
 }
}

extern int
default_cpu_mask_to_apicid_and(const struct cpumask *cpumask,
          const struct cpumask *andmask,
          unsigned int *apicid);

static inline __attribute__((no_instrument_function)) void
flat_vector_allocation_domain(int cpu, struct cpumask *retmask,
         const struct cpumask *mask)
{
# 622 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/apic.h"
 cpumask_clear(retmask);
 ((retmask)->bits)[0] = 0xFFu;
}

static inline __attribute__((no_instrument_function)) void
default_vector_allocation_domain(int cpu, struct cpumask *retmask,
     const struct cpumask *mask)
{
 cpumask_copy(retmask, (get_cpu_mask(cpu)));
}

static inline __attribute__((no_instrument_function)) unsigned long default_check_apicid_used(physid_mask_t *map, int apicid)
{
 return (__builtin_constant_p((apicid)) ? constant_test_bit((apicid), ((*map).mask)) : variable_test_bit((apicid), ((*map).mask)));
}

static inline __attribute__((no_instrument_function)) unsigned long default_check_apicid_present(int bit)
{
 return (__builtin_constant_p((bit)) ? constant_test_bit((bit), ((phys_cpu_present_map).mask)) : variable_test_bit((bit), ((phys_cpu_present_map).mask)));
}

static inline __attribute__((no_instrument_function)) void default_ioapic_phys_id_map(physid_mask_t *phys_map, physid_mask_t *retmap)
{
 *retmap = *phys_map;
}

static inline __attribute__((no_instrument_function)) int __default_cpu_present_to_apicid(int mps_cpu)
{
 if (mps_cpu < nr_cpu_ids && (__builtin_constant_p((cpumask_check((mps_cpu)))) ? constant_test_bit((cpumask_check((mps_cpu))), ((((cpu_present_mask))->bits))) : variable_test_bit((cpumask_check((mps_cpu))), ((((cpu_present_mask))->bits)))))
  return (int)(*({ do { const void *__vpp_verify = (typeof(((&(x86_bios_cpu_apicid))) + 0))((void *)0); (void)__vpp_verify; } while (0); ({ unsigned long __ptr; __asm__ ("" : "=r"(__ptr) : "0"((typeof(*(&(x86_bios_cpu_apicid))) *)(&(x86_bios_cpu_apicid)))); (typeof((typeof(*(&(x86_bios_cpu_apicid))) *)(&(x86_bios_cpu_apicid)))) (__ptr + (((__per_cpu_offset[mps_cpu])))); }); }));
 else
  return 0xFFFFu;
}

static inline __attribute__((no_instrument_function)) int
__default_check_phys_apicid_present(int phys_apicid)
{
 return (__builtin_constant_p((phys_apicid)) ? constant_test_bit((phys_apicid), ((phys_cpu_present_map).mask)) : variable_test_bit((phys_apicid), ((phys_cpu_present_map).mask)));
}
# 674 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/apic.h"
extern int default_cpu_present_to_apicid(int mps_cpu);
extern int default_check_phys_apicid_present(int phys_apicid);



extern void irq_enter(void);
extern void irq_exit(void);

static inline __attribute__((no_instrument_function)) void entering_irq(void)
{
 irq_enter();
 exit_idle();
}

static inline __attribute__((no_instrument_function)) void entering_ack_irq(void)
{
 ack_APIC_irq();
 entering_irq();
}

static inline __attribute__((no_instrument_function)) void exiting_irq(void)
{
 irq_exit();
}

static inline __attribute__((no_instrument_function)) void exiting_ack_irq(void)
{
 irq_exit();

 ack_APIC_irq();
}

extern void ioapic_zap_locks(void);
# 13 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/smp.h" 2

# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/io_apic.h" 1






# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/irq_vectors.h" 1
# 135 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/irq_vectors.h"
static inline __attribute__((no_instrument_function)) int invalid_vm86_irq(int irq)
{
 return irq < 3 || irq > 15;
}
# 8 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/io_apic.h" 2
# 27 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/io_apic.h"
union IO_APIC_reg_00 {
 u32 raw;
 struct {
  u32 __reserved_2 : 14,
   LTS : 1,
   delivery_type : 1,
   __reserved_1 : 8,
   ID : 8;
 } __attribute__ ((packed)) bits;
};

union IO_APIC_reg_01 {
 u32 raw;
 struct {
  u32 version : 8,
   __reserved_2 : 7,
   PRQ : 1,
   entries : 8,
   __reserved_1 : 8;
 } __attribute__ ((packed)) bits;
};

union IO_APIC_reg_02 {
 u32 raw;
 struct {
  u32 __reserved_2 : 24,
   arbitration : 4,
   __reserved_1 : 4;
 } __attribute__ ((packed)) bits;
};

union IO_APIC_reg_03 {
 u32 raw;
 struct {
  u32 boot_DT : 1,
   __reserved_1 : 31;
 } __attribute__ ((packed)) bits;
};

struct IO_APIC_route_entry {
 __u32 vector : 8,
  delivery_mode : 3,



  dest_mode : 1,
  delivery_status : 1,
  polarity : 1,
  irr : 1,
  trigger : 1,
  mask : 1,
  __reserved_2 : 15;

 __u32 __reserved_3 : 24,
  dest : 8;
} __attribute__ ((packed));

struct IR_IO_APIC_route_entry {
 __u64 vector : 8,
  zero : 3,
  index2 : 1,
  delivery_status : 1,
  polarity : 1,
  irr : 1,
  trigger : 1,
  mask : 1,
  reserved : 31,
  format : 1,
  index : 15;
} __attribute__ ((packed));
# 107 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/io_apic.h"
extern int nr_ioapics;

extern int mpc_ioapic_id(int ioapic);
extern unsigned int mpc_ioapic_addr(int ioapic);
extern struct mp_ioapic_gsi *mp_ioapic_gsi_routing(int ioapic);




extern int mp_irq_entries;


extern struct mpc_intsrc mp_irqs[(256 * 4)];


extern int mpc_default_type;


extern int sis_apic_bug;


extern int skip_ioapic_setup;


extern int noioapicquirk;


extern int noioapicreroute;


extern int timer_through_8259;
# 146 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/io_apic.h"
struct io_apic_irq_attr;
struct irq_cfg;
extern int io_apic_set_pci_routing(struct device *dev, int irq,
   struct io_apic_irq_attr *irq_attr);
void setup_IO_APIC_irq_extra(u32 gsi);
extern void ioapic_insert_resources(void);

extern int native_setup_ioapic_entry(int, struct IO_APIC_route_entry *,
         unsigned int, int,
         struct io_apic_irq_attr *);
extern int native_setup_ioapic_entry(int, struct IO_APIC_route_entry *,
         unsigned int, int,
         struct io_apic_irq_attr *);
extern void eoi_ioapic_irq(unsigned int irq, struct irq_cfg *cfg);

extern void native_compose_msi_msg(struct pci_dev *pdev,
       unsigned int irq, unsigned int dest,
       struct msi_msg *msg, u8 hpet_id);
extern void native_eoi_ioapic_pin(int apic, int pin, int vector);
int io_apic_setup_irq_pin_once(unsigned int irq, int node, struct io_apic_irq_attr *attr);

extern int save_ioapic_entries(void);
extern void mask_ioapic_entries(void);
extern int restore_ioapic_entries(void);

extern int get_nr_irqs_gsi(void);

extern void setup_ioapic_ids_from_mpc(void);
extern void setup_ioapic_ids_from_mpc_nocheck(void);

struct mp_ioapic_gsi{
 u32 gsi_base;
 u32 gsi_end;
};
extern struct mp_ioapic_gsi mp_gsi_routing[];
extern u32 gsi_top;
int mp_find_ioapic(u32 gsi);
int mp_find_ioapic_pin(int ioapic, u32 gsi);
void __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) mp_register_ioapic(int id, u32 address, u32 gsi_base);
extern void __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) pre_init_apic_IRQ0(void);

extern void mp_save_irq(struct mpc_intsrc *m);

extern void disable_ioapic_support(void);

extern void __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) native_io_apic_init_mappings(void);
extern unsigned int native_io_apic_read(unsigned int apic, unsigned int reg);
extern void native_io_apic_write(unsigned int apic, unsigned int reg, unsigned int val);
extern void native_io_apic_modify(unsigned int apic, unsigned int reg, unsigned int val);
extern void native_disable_io_apic(void);
extern void native_io_apic_print_entries(unsigned int apic, unsigned int nr_entries);
extern void intel_ir_io_apic_print_entries(unsigned int apic, unsigned int nr_entries);
extern int native_ioapic_set_affinity(struct irq_data *,
          const struct cpumask *,
          bool);

static inline __attribute__((no_instrument_function)) unsigned int io_apic_read(unsigned int apic, unsigned int reg)
{
 return x86_io_apic_ops.read(apic, reg);
}

static inline __attribute__((no_instrument_function)) void io_apic_write(unsigned int apic, unsigned int reg, unsigned int value)
{
 x86_io_apic_ops.write(apic, reg, value);
}
static inline __attribute__((no_instrument_function)) void io_apic_modify(unsigned int apic, unsigned int reg, unsigned int value)
{
 x86_io_apic_ops.modify(apic, reg, value);
}

extern void io_apic_eoi(unsigned int apic, unsigned int vector);
# 15 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/smp.h" 2






extern int smp_num_siblings;
extern unsigned int num_processors;

static inline __attribute__((no_instrument_function)) bool cpu_has_ht_siblings(void)
{
 bool has_siblings = false;

 has_siblings = (__builtin_constant_p((0*32+28)) && ( ((((0*32+28))>>5)==0 && (1UL<<(((0*32+28))&31) & ((1<<((0*32+ 0) & 31))|0|(1<<((0*32+ 5) & 31))|(1<<((0*32+ 6) & 31))| (1<<((0*32+ 8) & 31))|0|(1<<((0*32+24) & 31))|(1<<((0*32+15) & 31))| (1<<((0*32+25) & 31))|(1<<((0*32+26) & 31))))) || ((((0*32+28))>>5)==1 && (1UL<<(((0*32+28))&31) & ((1<<((1*32+29) & 31))|0))) || ((((0*32+28))>>5)==2 && (1UL<<(((0*32+28))&31) & 0)) || ((((0*32+28))>>5)==3 && (1UL<<(((0*32+28))&31) & ((1<<((3*32+20) & 31))))) || ((((0*32+28))>>5)==4 && (1UL<<(((0*32+28))&31) & (0))) || ((((0*32+28))>>5)==5 && (1UL<<(((0*32+28))&31) & 0)) || ((((0*32+28))>>5)==6 && (1UL<<(((0*32+28))&31) & 0)) || ((((0*32+28))>>5)==7 && (1UL<<(((0*32+28))&31) & 0)) || ((((0*32+28))>>5)==8 && (1UL<<(((0*32+28))&31) & 0)) || ((((0*32+28))>>5)==9 && (1UL<<(((0*32+28))&31) & 0)) ) ? 1 : (__builtin_constant_p(((0*32+28))) ? constant_test_bit(((0*32+28)), ((unsigned long *)((&boot_cpu_data)->x86_capability))) : variable_test_bit(((0*32+28)), ((unsigned long *)((&boot_cpu_data)->x86_capability))))) && smp_num_siblings > 1;

 return has_siblings;
}

extern __attribute__((section(".discard"), unused)) char __pcpu_scope_cpu_sibling_map; extern __attribute__((section(".data..percpu" "..readmostly"))) __typeof__(cpumask_var_t) cpu_sibling_map;
extern __attribute__((section(".discard"), unused)) char __pcpu_scope_cpu_core_map; extern __attribute__((section(".data..percpu" "..readmostly"))) __typeof__(cpumask_var_t) cpu_core_map;

extern __attribute__((section(".discard"), unused)) char __pcpu_scope_cpu_llc_shared_map; extern __attribute__((section(".data..percpu" "..readmostly"))) __typeof__(cpumask_var_t) cpu_llc_shared_map;
extern __attribute__((section(".discard"), unused)) char __pcpu_scope_cpu_llc_id; extern __attribute__((section(".data..percpu" "..readmostly"))) __typeof__(u16) cpu_llc_id;
extern __attribute__((section(".discard"), unused)) char __pcpu_scope_cpu_number; extern __attribute__((section(".data..percpu" "..readmostly"))) __typeof__(int) cpu_number;

static inline __attribute__((no_instrument_function)) struct cpumask *cpu_sibling_mask(int cpu)
{
 return (*({ do { const void *__vpp_verify = (typeof(((&(cpu_sibling_map))) + 0))((void *)0); (void)__vpp_verify; } while (0); ({ unsigned long __ptr; __asm__ ("" : "=r"(__ptr) : "0"((typeof(*(&(cpu_sibling_map))) *)(&(cpu_sibling_map)))); (typeof((typeof(*(&(cpu_sibling_map))) *)(&(cpu_sibling_map)))) (__ptr + (((__per_cpu_offset[cpu])))); }); }));
}

static inline __attribute__((no_instrument_function)) struct cpumask *cpu_core_mask(int cpu)
{
 return (*({ do { const void *__vpp_verify = (typeof(((&(cpu_core_map))) + 0))((void *)0); (void)__vpp_verify; } while (0); ({ unsigned long __ptr; __asm__ ("" : "=r"(__ptr) : "0"((typeof(*(&(cpu_core_map))) *)(&(cpu_core_map)))); (typeof((typeof(*(&(cpu_core_map))) *)(&(cpu_core_map)))) (__ptr + (((__per_cpu_offset[cpu])))); }); }));
}

static inline __attribute__((no_instrument_function)) struct cpumask *cpu_llc_shared_mask(int cpu)
{
 return (*({ do { const void *__vpp_verify = (typeof(((&(cpu_llc_shared_map))) + 0))((void *)0); (void)__vpp_verify; } while (0); ({ unsigned long __ptr; __asm__ ("" : "=r"(__ptr) : "0"((typeof(*(&(cpu_llc_shared_map))) *)(&(cpu_llc_shared_map)))); (typeof((typeof(*(&(cpu_llc_shared_map))) *)(&(cpu_llc_shared_map)))) (__ptr + (((__per_cpu_offset[cpu])))); }); }));
}

extern __attribute__((section(".discard"), unused)) char __pcpu_scope_x86_cpu_to_apicid; extern __attribute__((section(".data..percpu" "..readmostly"))) __typeof__(u16) x86_cpu_to_apicid; extern __typeof__(u16) *x86_cpu_to_apicid_early_ptr; extern __typeof__(u16) x86_cpu_to_apicid_early_map[];
extern __attribute__((section(".discard"), unused)) char __pcpu_scope_x86_bios_cpu_apicid; extern __attribute__((section(".data..percpu" "..readmostly"))) __typeof__(u16) x86_bios_cpu_apicid; extern __typeof__(u16) *x86_bios_cpu_apicid_early_ptr; extern __typeof__(u16) x86_bios_cpu_apicid_early_map[];





extern unsigned long stack_start;

struct task_struct;

struct smp_ops {
 void (*smp_prepare_boot_cpu)(void);
 void (*smp_prepare_cpus)(unsigned max_cpus);
 void (*smp_cpus_done)(unsigned max_cpus);

 void (*stop_other_cpus)(int wait);
 void (*smp_send_reschedule)(int cpu);

 int (*cpu_up)(unsigned cpu, struct task_struct *tidle);
 int (*cpu_disable)(void);
 void (*cpu_die)(unsigned int cpu);
 void (*play_dead)(void);

 void (*send_call_func_ipi)(const struct cpumask *mask);
 void (*send_call_func_single_ipi)(int cpu);
};


extern void set_cpu_sibling_map(int cpu);





extern struct smp_ops smp_ops;

static inline __attribute__((no_instrument_function)) void smp_send_stop(void)
{
 smp_ops.stop_other_cpus(0);
}

static inline __attribute__((no_instrument_function)) void stop_other_cpus(void)
{
 smp_ops.stop_other_cpus(1);
}

static inline __attribute__((no_instrument_function)) void smp_prepare_boot_cpu(void)
{
 smp_ops.smp_prepare_boot_cpu();
}

static inline __attribute__((no_instrument_function)) void smp_prepare_cpus(unsigned int max_cpus)
{
 smp_ops.smp_prepare_cpus(max_cpus);
}

static inline __attribute__((no_instrument_function)) void smp_cpus_done(unsigned int max_cpus)
{
 smp_ops.smp_cpus_done(max_cpus);
}

static inline __attribute__((no_instrument_function)) int __cpu_up(unsigned int cpu, struct task_struct *tidle)
{
 return smp_ops.cpu_up(cpu, tidle);
}

static inline __attribute__((no_instrument_function)) int __cpu_disable(void)
{
 return smp_ops.cpu_disable();
}

static inline __attribute__((no_instrument_function)) void __cpu_die(unsigned int cpu)
{
 smp_ops.cpu_die(cpu);
}

static inline __attribute__((no_instrument_function)) void play_dead(void)
{
 smp_ops.play_dead();
}

static inline __attribute__((no_instrument_function)) void smp_send_reschedule(int cpu)
{
 smp_ops.smp_send_reschedule(cpu);
}

static inline __attribute__((no_instrument_function)) void arch_send_call_function_single_ipi(int cpu)
{
 smp_ops.send_call_func_single_ipi(cpu);
}

static inline __attribute__((no_instrument_function)) void arch_send_call_function_ipi_mask(const struct cpumask *mask)
{
 smp_ops.send_call_func_ipi(mask);
}

void cpu_disable_common(void);
void native_smp_prepare_boot_cpu(void);
void native_smp_prepare_cpus(unsigned int max_cpus);
void native_smp_cpus_done(unsigned int max_cpus);
int native_cpu_up(unsigned int cpunum, struct task_struct *tidle);
int native_cpu_disable(void);
void native_cpu_die(unsigned int cpu);
void native_play_dead(void);
void play_dead_common(void);
void wbinvd_on_cpu(int cpu);
int wbinvd_on_all_cpus(void);

void native_send_call_func_ipi(const struct cpumask *mask);
void native_send_call_func_single_ipi(int cpu);
void x86_idle_thread_init(unsigned int cpu, struct task_struct *idle);

void smp_store_boot_cpu_info(void);
void smp_store_cpu_info(int id);
# 181 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/smp.h"
extern unsigned disabled_cpus;
# 216 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/smp.h"
extern int hard_smp_processor_id(void);
# 227 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/smp.h"
extern void nmi_selftest(void);
# 11 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/mmzone_64.h" 2

extern struct pglist_data *node_data[];
# 5 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/mmzone.h" 2
# 931 "include/linux/mmzone.h" 2



extern struct pglist_data *first_online_pgdat(void);
extern struct pglist_data *next_online_pgdat(struct pglist_data *pgdat);
extern struct zone *next_zone(struct zone *zone);
# 966 "include/linux/mmzone.h"
static inline __attribute__((no_instrument_function)) struct zone *zonelist_zone(struct zoneref *zoneref)
{
 return zoneref->zone;
}

static inline __attribute__((no_instrument_function)) int zonelist_zone_idx(struct zoneref *zoneref)
{
 return zoneref->zone_idx;
}

static inline __attribute__((no_instrument_function)) int zonelist_node_idx(struct zoneref *zoneref)
{


 return zoneref->zone->node;



}
# 999 "include/linux/mmzone.h"
struct zoneref *next_zones_zonelist(struct zoneref *z,
     enum zone_type highest_zoneidx,
     nodemask_t *nodes,
     struct zone **zone);
# 1016 "include/linux/mmzone.h"
static inline __attribute__((no_instrument_function)) struct zoneref *first_zones_zonelist(struct zonelist *zonelist,
     enum zone_type highest_zoneidx,
     nodemask_t *nodes,
     struct zone **zone)
{
 return next_zones_zonelist(zonelist->_zonerefs, highest_zoneidx, nodes,
        zone);
}
# 1098 "include/linux/mmzone.h"
struct page;
struct page_cgroup;
struct mem_section {
# 1113 "include/linux/mmzone.h"
 unsigned long section_mem_map;


 unsigned long *pageblock_flags;





 struct page_cgroup *page_cgroup;
 unsigned long pad;





};
# 1142 "include/linux/mmzone.h"
extern struct mem_section *mem_section[((((1UL << (46 - 27))) + ((((1UL) << 12) / sizeof (struct mem_section))) - 1) / ((((1UL) << 12) / sizeof (struct mem_section))))];




static inline __attribute__((no_instrument_function)) struct mem_section *__nr_to_section(unsigned long nr)
{
 if (!mem_section[((nr) / (((1UL) << 12) / sizeof (struct mem_section)))])
  return ((void *)0);
 return &mem_section[((nr) / (((1UL) << 12) / sizeof (struct mem_section)))][nr & ((((1UL) << 12) / sizeof (struct mem_section)) - 1)];
}
extern int __section_nr(struct mem_section* ms);
extern unsigned long usemap_size(void);
# 1167 "include/linux/mmzone.h"
static inline __attribute__((no_instrument_function)) struct page *__section_mem_map_addr(struct mem_section *section)
{
 unsigned long map = section->section_mem_map;
 map &= (~((1UL<<2)-1));
 return (struct page *)map;
}

static inline __attribute__((no_instrument_function)) int present_section(struct mem_section *section)
{
 return (section && (section->section_mem_map & (1UL<<0)));
}

static inline __attribute__((no_instrument_function)) int present_section_nr(unsigned long nr)
{
 return present_section(__nr_to_section(nr));
}

static inline __attribute__((no_instrument_function)) int valid_section(struct mem_section *section)
{
 return (section && (section->section_mem_map & (1UL<<1)));
}

static inline __attribute__((no_instrument_function)) int valid_section_nr(unsigned long nr)
{
 return valid_section(__nr_to_section(nr));
}

static inline __attribute__((no_instrument_function)) struct mem_section *__pfn_to_section(unsigned long pfn)
{
 return __nr_to_section(((pfn) >> (27 - 12)));
}


static inline __attribute__((no_instrument_function)) int pfn_valid(unsigned long pfn)
{
 if (((pfn) >> (27 - 12)) >= (1UL << (46 - 27)))
  return 0;
 return valid_section(__nr_to_section(((pfn) >> (27 - 12))));
}


static inline __attribute__((no_instrument_function)) int pfn_present(unsigned long pfn)
{
 if (((pfn) >> (27 - 12)) >= (1UL << (46 - 27)))
  return 0;
 return present_section(__nr_to_section(((pfn) >> (27 - 12))));
}
# 1231 "include/linux/mmzone.h"
void sparse_init(void);






bool early_pfn_in_nid(unsigned long pfn, int nid);
# 1247 "include/linux/mmzone.h"
void memory_present(int nid, unsigned long start, unsigned long end);
unsigned long __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) node_memmap_size_bytes(int, unsigned long, unsigned long);
# 1281 "include/linux/mmzone.h"
static inline __attribute__((no_instrument_function)) int memmap_valid_within(unsigned long pfn,
     struct page *page, struct zone *zone)
{
 return 1;
}
# 6 "include/linux/gfp.h" 2


# 1 "include/linux/topology.h" 1
# 33 "include/linux/topology.h"
# 1 "include/linux/smp.h" 1
# 14 "include/linux/smp.h"
# 1 "include/linux/llist.h" 1
# 61 "include/linux/llist.h"
struct llist_head {
 struct llist_node *first;
};

struct llist_node {
 struct llist_node *next;
};
# 76 "include/linux/llist.h"
static inline __attribute__((no_instrument_function)) void init_llist_head(struct llist_head *list)
{
 list->first = ((void *)0);
}
# 158 "include/linux/llist.h"
static inline __attribute__((no_instrument_function)) bool llist_empty(const struct llist_head *head)
{
 return (*(volatile typeof(head->first) *)&(head->first)) == ((void *)0);
}

static inline __attribute__((no_instrument_function)) struct llist_node *llist_next(struct llist_node *node)
{
 return node->next;
}

extern bool llist_add_batch(struct llist_node *new_first,
       struct llist_node *new_last,
       struct llist_head *head);







static inline __attribute__((no_instrument_function)) bool llist_add(struct llist_node *new, struct llist_head *head)
{
 return llist_add_batch(new, new, head);
}
# 191 "include/linux/llist.h"
static inline __attribute__((no_instrument_function)) struct llist_node *llist_del_all(struct llist_head *head)
{
 return ({ __typeof__ (*((&head->first))) __ret = ((((void *)0))); switch (sizeof(*((&head->first)))) { case 1: asm volatile ("" "xchg" "b %b0, %1\n" : "+q" (__ret), "+m" (*((&head->first))) : : "memory", "cc"); break; case 2: asm volatile ("" "xchg" "w %w0, %1\n" : "+r" (__ret), "+m" (*((&head->first))) : : "memory", "cc"); break; case 4: asm volatile ("" "xchg" "l %0, %1\n" : "+r" (__ret), "+m" (*((&head->first))) : : "memory", "cc"); break; case 8: asm volatile ("" "xchg" "q %q0, %1\n" : "+r" (__ret), "+m" (*((&head->first))) : : "memory", "cc"); break; default: __xchg_wrong_size(); } __ret; });
}

extern struct llist_node *llist_del_first(struct llist_head *head);

struct llist_node *llist_reverse_order(struct llist_node *head);
# 15 "include/linux/smp.h" 2

extern void cpu_idle(void);

typedef void (*smp_call_func_t)(void *info);
struct call_single_data {
 struct llist_node llist;
 smp_call_func_t func;
 void *info;
 u16 flags;
};


extern unsigned int total_cpus;

int smp_call_function_single(int cpuid, smp_call_func_t func, void *info,
        int wait);




int on_each_cpu(smp_call_func_t func, void *info, int wait);





void on_each_cpu_mask(const struct cpumask *mask, smp_call_func_t func,
  void *info, bool wait);






void on_each_cpu_cond(bool (*cond_func)(int cpu, void *info),
  smp_call_func_t func, void *info, bool wait,
  gfp_t gfp_flags);

int smp_call_function_single_async(int cpu, struct call_single_data *csd);
# 71 "include/linux/smp.h"
extern void smp_send_stop(void);




extern void smp_send_reschedule(int cpu);





extern void smp_prepare_cpus(unsigned int max_cpus);




extern int __cpu_up(unsigned int cpunum, struct task_struct *tidle);




extern void smp_cpus_done(unsigned int max_cpus);




int smp_call_function(smp_call_func_t func, void *info, int wait);
void smp_call_function_many(const struct cpumask *mask,
       smp_call_func_t func, void *info, bool wait);

int smp_call_function_any(const struct cpumask *mask,
     smp_call_func_t func, void *info, int wait);

void kick_all_cpus_sync(void);




void __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) call_function_init(void);
void generic_smp_call_function_single_interrupt(void);







void smp_prepare_boot_cpu(void);

extern unsigned int setup_max_cpus;
extern void __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) setup_nr_cpu_ids(void);
extern void __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) smp_init(void);
# 185 "include/linux/smp.h"
extern void arch_disable_smp_support(void);

extern void arch_enable_nonboot_cpus_begin(void);
extern void arch_enable_nonboot_cpus_end(void);

void smp_setup_processor_id(void);
# 34 "include/linux/topology.h" 2
# 1 "include/linux/percpu.h" 1







# 1 "include/linux/pfn.h" 1
# 9 "include/linux/percpu.h" 2
# 82 "include/linux/percpu.h"
extern void *pcpu_base_addr;
extern const unsigned long *pcpu_unit_offsets;

struct pcpu_group_info {
 int nr_units;
 unsigned long base_offset;
 unsigned int *cpu_map;

};

struct pcpu_alloc_info {
 size_t static_size;
 size_t reserved_size;
 size_t dyn_size;
 size_t unit_size;
 size_t atom_size;
 size_t alloc_size;
 size_t __ai_size;
 int nr_groups;
 struct pcpu_group_info groups[];
};

enum pcpu_fc {
 PCPU_FC_AUTO,
 PCPU_FC_EMBED,
 PCPU_FC_PAGE,

 PCPU_FC_NR,
};
extern const char * const pcpu_fc_names[PCPU_FC_NR];

extern enum pcpu_fc pcpu_chosen_fc;

typedef void * (*pcpu_fc_alloc_fn_t)(unsigned int cpu, size_t size,
         size_t align);
typedef void (*pcpu_fc_free_fn_t)(void *ptr, size_t size);
typedef void (*pcpu_fc_populate_pte_fn_t)(unsigned long addr);
typedef int (pcpu_fc_cpu_distance_fn_t)(unsigned int from, unsigned int to);

extern struct pcpu_alloc_info * __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) pcpu_alloc_alloc_info(int nr_groups,
            int nr_units);
extern void __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) pcpu_free_alloc_info(struct pcpu_alloc_info *ai);

extern int __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) pcpu_setup_first_chunk(const struct pcpu_alloc_info *ai,
      void *base_addr);


extern int __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) pcpu_embed_first_chunk(size_t reserved_size, size_t dyn_size,
    size_t atom_size,
    pcpu_fc_cpu_distance_fn_t cpu_distance_fn,
    pcpu_fc_alloc_fn_t alloc_fn,
    pcpu_fc_free_fn_t free_fn);



extern int __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) pcpu_page_first_chunk(size_t reserved_size,
    pcpu_fc_alloc_fn_t alloc_fn,
    pcpu_fc_free_fn_t free_fn,
    pcpu_fc_populate_pte_fn_t populate_pte_fn);
# 154 "include/linux/percpu.h"
extern void *__alloc_reserved_percpu(size_t size, size_t align);
extern bool is_kernel_percpu_address(unsigned long addr);




extern void __attribute__ ((__section__(".init.text"))) __attribute__((no_instrument_function)) percpu_init_late(void);

extern void *__alloc_percpu(size_t size, size_t align);
extern void free_percpu(void *__pdata);
extern phys_addr_t per_cpu_ptr_to_phys(void *addr);
# 174 "include/linux/percpu.h"
extern void __bad_size_call_parameter(void);




static inline __attribute__((no_instrument_function)) void __this_cpu_preempt_check(const char *op) { }
# 35 "include/linux/topology.h" 2
# 49 "include/linux/topology.h"
int arch_update_cpu_topology(void);
# 185 "include/linux/topology.h"
extern __attribute__((section(".discard"), unused)) char __pcpu_scope_numa_node; extern __attribute__((section(".data..percpu" ""))) __typeof__(int) numa_node;



static inline __attribute__((no_instrument_function)) int numa_node_id(void)
{
 return ({ typeof((numa_node)) pscr_ret__; do { const void *__vpp_verify = (typeof((&((numa_node))) + 0))((void *)0); (void)__vpp_verify; } while (0); switch(sizeof((numa_node))) { case 1: pscr_ret__ = ({ typeof(((numa_node))) pfo_ret__; switch (sizeof(((numa_node)))) { case 1: asm("mov" "b ""%%""gs"":" "%P" "1"",%0" : "=q" (pfo_ret__) : "m"((numa_node))); break; case 2: asm("mov" "w ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"((numa_node))); break; case 4: asm("mov" "l ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"((numa_node))); break; case 8: asm("mov" "q ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"((numa_node))); break; default: __bad_percpu_size(); } pfo_ret__; });break; case 2: pscr_ret__ = ({ typeof(((numa_node))) pfo_ret__; switch (sizeof(((numa_node)))) { case 1: asm("mov" "b ""%%""gs"":" "%P" "1"",%0" : "=q" (pfo_ret__) : "m"((numa_node))); break; case 2: asm("mov" "w ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"((numa_node))); break; case 4: asm("mov" "l ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"((numa_node))); break; case 8: asm("mov" "q ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"((numa_node))); break; default: __bad_percpu_size(); } pfo_ret__; });break; case 4: pscr_ret__ = ({ typeof(((numa_node))) pfo_ret__; switch (sizeof(((numa_node)))) { case 1: asm("mov" "b ""%%""gs"":" "%P" "1"",%0" : "=q" (pfo_ret__) : "m"((numa_node))); break; case 2: asm("mov" "w ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"((numa_node))); break; case 4: asm("mov" "l ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"((numa_node))); break; case 8: asm("mov" "q ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"((numa_node))); break; default: __bad_percpu_size(); } pfo_ret__; });break; case 8: pscr_ret__ = ({ typeof(((numa_node))) pfo_ret__; switch (sizeof(((numa_node)))) { case 1: asm("mov" "b ""%%""gs"":" "%P" "1"",%0" : "=q" (pfo_ret__) : "m"((numa_node))); break; case 2: asm("mov" "w ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"((numa_node))); break; case 4: asm("mov" "l ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"((numa_node))); break; case 8: asm("mov" "q ""%%""gs"":" "%P" "1"",%0" : "=r" (pfo_ret__) : "m"((numa_node))); break; default: __bad_percpu_size(); } pfo_ret__; });break; default: __bad_size_call_parameter();break; } pscr_ret__; });
}
# 203 "include/linux/topology.h"
static inline __attribute__((no_instrument_function)) void set_numa_node(int node)
{
 do { do { const void *__vpp_verify = (typeof((&((numa_node))) + 0))((void *)0); (void)__vpp_verify; } while (0); switch(sizeof((numa_node))) { case 1: do { typedef typeof(((numa_node))) pto_T__; if (0) { pto_T__ pto_tmp__; pto_tmp__ = ((node)); (void)pto_tmp__; } switch (sizeof(((numa_node)))) { case 1: asm("mov" "b %1,""%%""gs"":" "%P" "0" : "+m" (((numa_node))) : "qi" ((pto_T__)((node)))); break; case 2: asm("mov" "w %1,""%%""gs"":" "%P" "0" : "+m" (((numa_node))) : "ri" ((pto_T__)((node)))); break; case 4: asm("mov" "l %1,""%%""gs"":" "%P" "0" : "+m" (((numa_node))) : "ri" ((pto_T__)((node)))); break; case 8: asm("mov" "q %1,""%%""gs"":" "%P" "0" : "+m" (((numa_node))) : "re" ((pto_T__)((node)))); break; default: __bad_percpu_size(); } } while (0);break; case 2: do { typedef typeof(((numa_node))) pto_T__; if (0) { pto_T__ pto_tmp__; pto_tmp__ = ((node)); (void)pto_tmp__; } switch (sizeof(((numa_node)))) { case 1: asm("mov" "b %1,""%%""gs"":" "%P" "0" : "+m" (((numa_node))) : "qi" ((pto_T__)((node)))); break; case 2: asm("mov" "w %1,""%%""gs"":" "%P" "0" : "+m" (((numa_node))) : "ri" ((pto_T__)((node)))); break; case 4: asm("mov" "l %1,""%%""gs"":" "%P" "0" : "+m" (((numa_node))) : "ri" ((pto_T__)((node)))); break; case 8: asm("mov" "q %1,""%%""gs"":" "%P" "0" : "+m" (((numa_node))) : "re" ((pto_T__)((node)))); break; default: __bad_percpu_size(); } } while (0);break; case 4: do { typedef typeof(((numa_node))) pto_T__; if (0) { pto_T__ pto_tmp__; pto_tmp__ = ((node)); (void)pto_tmp__; } switch (sizeof(((numa_node)))) { case 1: asm("mov" "b %1,""%%""gs"":" "%P" "0" : "+m" (((numa_node))) : "qi" ((pto_T__)((node)))); break; case 2: asm("mov" "w %1,""%%""gs"":" "%P" "0" : "+m" (((numa_node))) : "ri" ((pto_T__)((node)))); break; case 4: asm("mov" "l %1,""%%""gs"":" "%P" "0" : "+m" (((numa_node))) : "ri" ((pto_T__)((node)))); break; case 8: asm("mov" "q %1,""%%""gs"":" "%P" "0" : "+m" (((numa_node))) : "re" ((pto_T__)((node)))); break; default: __bad_percpu_size(); } } while (0);break; case 8: do { typedef typeof(((numa_node))) pto_T__; if (0) { pto_T__ pto_tmp__; pto_tmp__ = ((node)); (void)pto_tmp__; } switch (sizeof(((numa_node)))) { case 1: asm("mov" "b %1,""%%""gs"":" "%P" "0" : "+m" (((numa_node))) : "qi" ((pto_T__)((node)))); break; case 2: asm("mov" "w %1,""%%""gs"":" "%P" "0" : "+m" (((numa_node))) : "ri" ((pto_T__)((node)))); break; case 4: asm("mov" "l %1,""%%""gs"":" "%P" "0" : "+m" (((numa_node))) : "ri" ((pto_T__)((node)))); break; case 8: asm("mov" "q %1,""%%""gs"":" "%P" "0" : "+m" (((numa_node))) : "re" ((pto_T__)((node)))); break; default: __bad_percpu_size(); } } while (0);break; default: __bad_size_call_parameter();break; } } while (0);
}



static inline __attribute__((no_instrument_function)) void set_cpu_numa_node(int cpu, int node)
{
 (*({ do { const void *__vpp_verify = (typeof(((&(numa_node))) + 0))((void *)0); (void)__vpp_verify; } while (0); ({ unsigned long __ptr; __asm__ ("" : "=r"(__ptr) : "0"((typeof(*(&(numa_node))) *)(&(numa_node)))); (typeof((typeof(*(&(numa_node))) *)(&(numa_node)))) (__ptr + (((__per_cpu_offset[cpu])))); }); })) = node;
}
# 270 "include/linux/topology.h"
static inline __attribute__((no_instrument_function)) int numa_mem_id(void)
{
 return numa_node_id();
}



static inline __attribute__((no_instrument_function)) int cpu_to_mem(int cpu)
{
 return __cpu_to_node(cpu);
}
# 9 "include/linux/gfp.h" 2


struct vm_area_struct;
# 162 "include/linux/gfp.h"
static inline __attribute__((no_instrument_function)) int allocflags_to_migratetype(gfp_t gfp_flags)
{
 ({ int __ret_warn_on = !!((gfp_flags & ((( gfp_t)0x80000u)|(( gfp_t)0x08u))) == ((( gfp_t)0x80000u)|(( gfp_t)0x08u))); if (__builtin_expect(!!(__ret_warn_on), 0)) warn_slowpath_null("include/linux/gfp.h", 164); __builtin_expect(!!(__ret_warn_on), 0); });

 if (__builtin_expect(!!(page_group_by_mobility_disabled), 0))
  return MIGRATE_UNMOVABLE;


 return (((gfp_flags & (( gfp_t)0x08u)) != 0) << 1) |
  ((gfp_flags & (( gfp_t)0x80000u)) != 0);
}
# 257 "include/linux/gfp.h"
static inline __attribute__((no_instrument_function)) enum zone_type gfp_zone(gfp_t flags)
{
 enum zone_type z;
 int bit = ( int) (flags & ((( gfp_t)0x01u)|(( gfp_t)0x02u)|(( gfp_t)0x04u)|(( gfp_t)0x08u)));

 z = (( (ZONE_NORMAL << 0 * 2) | (ZONE_DMA << 0x01u * 2) | (ZONE_NORMAL << 0x02u * 2) | (ZONE_DMA32 << 0x04u * 2) | (ZONE_NORMAL << 0x08u * 2) | (ZONE_DMA << (0x08u | 0x01u) * 2) | (ZONE_MOVABLE << (0x08u | 0x02u) * 2) | (ZONE_DMA32 << (0x08u | 0x04u) * 2) ) >> (bit * 2)) &
      ((1 << 2) - 1);
 do { if (__builtin_expect(!!((( 1 << (0x01u | 0x02u) | 1 << (0x01u | 0x04u) | 1 << (0x04u | 0x02u) | 1 << (0x01u | 0x04u | 0x02u) | 1 << (0x08u | 0x02u | 0x01u) | 1 << (0x08u | 0x04u | 0x01u) | 1 << (0x08u | 0x04u | 0x02u) | 1 << (0x08u | 0x04u | 0x01u | 0x02u) ) >> bit) & 1), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("include/linux/gfp.h"), "i" (264), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0);
 return z;
}
# 275 "include/linux/gfp.h"
static inline __attribute__((no_instrument_function)) int gfp_zonelist(gfp_t flags)
{
 if ((1 || 0) && __builtin_expect(!!(flags & (( gfp_t)0x40000u)), 0))
  return 1;

 return 0;
}
# 292 "include/linux/gfp.h"
static inline __attribute__((no_instrument_function)) struct zonelist *node_zonelist(int nid, gfp_t flags)
{
 return (node_data[nid])->node_zonelists + gfp_zonelist(flags);
}


static inline __attribute__((no_instrument_function)) void arch_free_page(struct page *page, int order) { }


static inline __attribute__((no_instrument_function)) void arch_alloc_page(struct page *page, int order) { }


struct page *
__alloc_pages_nodemask(gfp_t gfp_mask, unsigned int order,
         struct zonelist *zonelist, nodemask_t *nodemask);

static inline __attribute__((no_instrument_function)) struct page *
__alloc_pages(gfp_t gfp_mask, unsigned int order,
  struct zonelist *zonelist)
{
 return __alloc_pages_nodemask(gfp_mask, order, zonelist, ((void *)0));
}

static inline __attribute__((no_instrument_function)) struct page *alloc_pages_node(int nid, gfp_t gfp_mask,
      unsigned int order)
{

 if (nid < 0)
  nid = numa_node_id();

 return __alloc_pages(gfp_mask, order, node_zonelist(nid, gfp_mask));
}

static inline __attribute__((no_instrument_function)) struct page *alloc_pages_exact_node(int nid, gfp_t gfp_mask,
      unsigned int order)
{
 do { if (__builtin_expect(!!(nid < 0 || nid >= (1 << 10) || !node_state((nid), N_ONLINE)), 0)) do { asm volatile("1:\tud2\n" ".pushsection __bug_table,\"a\"\n" "2:\t.long 1b - 2b, %c0 - 2b\n" "\t.word %c1, 0\n" "\t.org 2b+%c2\n" ".popsection" : : "i" ("include/linux/gfp.h"), "i" (328), "i" (sizeof(struct bug_entry))); do { } while (1); } while (0); } while (0);

 return __alloc_pages(gfp_mask, order, node_zonelist(nid, gfp_mask));
}


extern struct page *alloc_pages_current(gfp_t gfp_mask, unsigned order);

static inline __attribute__((no_instrument_function)) struct page *
alloc_pages(gfp_t gfp_mask, unsigned int order)
{
 return alloc_pages_current(gfp_mask, order);
}
extern struct page *alloc_pages_vma(gfp_t gfp_mask, int order,
   struct vm_area_struct *vma, unsigned long addr,
   int node);
# 356 "include/linux/gfp.h"
extern unsigned long __get_free_pages(gfp_t gfp_mask, unsigned int order);
extern unsigned long get_zeroed_page(gfp_t gfp_mask);

void *alloc_pages_exact(size_t size, gfp_t gfp_mask);
void free_pages_exact(void *virt, size_t size);

void *alloc_pages_exact_nid(int nid, size_t size, gfp_t gfp_mask);







extern void __free_pages(struct page *page, unsigned int order);
extern void free_pages(unsigned long addr, unsigned int order);
extern void free_hot_cold_page(struct page *page, int cold);
extern void free_hot_cold_page_list(struct list_head *list, int cold);

extern void __free_memcg_kmem_pages(struct page *page, unsigned int order);
extern void free_memcg_kmem_pages(unsigned long addr, unsigned int order);




void page_alloc_init(void);
void drain_zone_pages(struct zone *zone, struct per_cpu_pages *pcp);
void drain_all_pages(void);
void drain_local_pages(void *dummy);
# 393 "include/linux/gfp.h"
extern gfp_t gfp_allowed_mask;


bool gfp_pfmemalloc_allowed(gfp_t gfp_mask);

extern void pm_restrict_gfp_mask(void);
extern void pm_restore_gfp_mask(void);


extern bool pm_suspended_storage(void);
# 413 "include/linux/gfp.h"
extern int alloc_contig_range(unsigned long start, unsigned long end,
         unsigned migratetype);
extern void free_contig_range(unsigned long pfn, unsigned nr_pages);


extern void init_cma_reserved_pageblock(struct page *page);
# 30 "include/linux/device.h" 2
# 1 "/home/alpha/work/inst/current/envs/linux-3.15.6.tar.xz/linux-3.15.6/arch/x86/include/asm/device.h" 1



struct dev_archdata {

 struct dma_map_ops *dma_ops;


 void *iommu;

};

struct pdev_archdata {
};
# 31 "include/linux/device.h" 2

struct device;
struct device_private;
struct device_driver;
struct driver_private;
struct module;
struct class;
struct subsys_private;
struct bus_type;
struct device_node;
struct iommu_ops;
struct iommu_group;

struct bus_attribute {
 struct attribute attr;
 ssize_t (*show)(struct bus_type *bus, char *buf);
 ssize_t (*store)(struct bus_type *bus, const char *buf, size_t count);
};
# 57 "include/linux/device.h"
extern int __attribute__((warn_unused_result)) bus_create_file(struct bus_type *,
     struct bus_attribute *);
extern void bus_remove_file(struct bus_type *, struct bus_attribute *);
# 104 "include/linux/device.h"
struct bus_type {
 const char *name;
 const char *dev_name;
 struct device *dev_root;
 struct device_attribute *dev_attrs;
 const struct attribute_group **bus_groups;
 const struct attribute_group **dev_groups;
 const struct attribute_group **drv_groups;

 int (*match)(struct device *dev, struct device_driver *drv);
 int (*uevent)(struct device *dev, struct kobj_uevent_env *env);
 int (*probe)(struct device *dev);
 int (*remove)(struct device *dev);
 void (*shutdown)(struct device *dev);

 int (*online)(struct device *dev);
 int (*offline)(struct device *dev);

 int (*suspend)(struct device *dev, pm_message_t state);
 int (*resume)(struct device *dev);

 const struct dev_pm_ops *pm;

 struct iommu_ops *iommu_ops;

 struct subsys_private *p;
 struct lock_class_key lock_key;
};

extern int __attribute__((warn_unused_result)) bus_register(struct bus_type *bus);

extern void bus_unregister(struct bus_type *bus);

extern int __attribute__((warn_unused_result)) bus_rescan_devices(struct bus_type *bus);


struct subsys_dev_iter {
 struct klist_iter ki;
 const struct device_type *type;
};
void subsys_dev_iter_init(struct subsys_dev_iter *iter,
    struct bus_type *subsys,
    struct device *start,
    const struct device_type *type);
struct device *subsys_dev_iter_next(struct subsys_dev_iter *iter);
void subsys_dev_iter_exit(struct subsys_dev_iter *iter);

int bus_for_each_dev(struct bus_type *bus, struct device *start, void *data,
       int (*fn)(struct device *dev, void *data));
struct device *bus_find_device(struct bus_type *bus, struct device *start,
          void *data,
          int (*match)(struct device *dev, void *data));
struct device *bus_find_device_by_name(struct bus_type *bus,
           struct device *start,
           const char *name);
struct device *subsys_find_device_by_id(struct bus_type *bus, unsigned int id,
     struct device *hint);
int bus_for_each_drv(struct bus_type *bus, struct device_driver *start,
       void *data, int (*fn)(struct device_driver *, void *));
void bus_sort_breadthfirst(struct bus_type *bus,
      int (*compare)(const struct device *a,
       const struct device *b));






struct notifier_block;

extern int bus_register_notifier(struct bus_type *bus,
     struct notifier_block *nb);
extern int bus_unregister_notifier(struct bus_type *bus,
       struct notifier_block *nb);
# 193 "include/linux/device.h"
extern struct kset *bus_get_kset(struct bus_type *bus);
extern struct klist *bus_get_device_klist(struct bus_type *bus);
# 228 "include/linux/device.h"
struct device_driver {
 const char *name;
 struct bus_type *bus;

 struct module *owner;
 const char *mod_name;

 bool suppress_bind_attrs;

 const struct of_device_id *of_match_table;
 const struct acpi_device_id *acpi_match_table;

 int (*probe) (struct device *dev);
 int (*remove) (struct device *dev);
 void (*shutdown) (struct device *dev);
 int (*suspend) (struct device *dev, pm_message_t state);
 int (*resume) (struct device *dev);
 const struct attribute_group **groups;

 const struct dev_pm_ops *pm;

 struct driver_private *p;
};


extern int __attribute__((warn_unused_result)) driver_register(struct device_driver *drv);
extern void driver_unregister(struct device_driver *drv);

extern struct device_driver *driver_find(const char *name,
      struct bus_type *bus);
extern int driver_probe_done(void);
extern void wait_for_device_probe(void);




struct driver_attribute {
 struct attribute attr;
 ssize_t (*show)(struct device_driver *driver, char *buf);
 ssize_t (*store)(struct device_driver *driver, const char *buf,
    size_t count);
};
# 280 "include/linux/device.h"
extern int __attribute__((warn_unused_result)) driver_create_file(struct device_driver *driver,
     const struct driver_attribute *attr);
extern void driver_remove_file(struct device_driver *driver,
          const struct driver_attribute *attr);

extern int __attribute__((warn_unused_result)) driver_for_each_device(struct device_driver *drv,
            struct device *start,
            void *data,
            int (*fn)(struct device *dev,
        void *));
struct device *driver_find_device(struct device_driver *drv,
      struct device *start, void *data,
      int (*match)(struct device *dev, void *data));
# 307 "include/linux/device.h"
struct subsys_interface {
 const char *name;
 struct bus_type *subsys;
 struct list_head node;
 int (*add_dev)(struct device *dev, struct subsys_interface *sif);
 int (*remove_dev)(struct device *dev, struct subsys_interface *sif);
};

int subsys_interface_register(struct subsys_interface *sif);
void subsys_interface_unregister(struct subsys_interface *sif);

int subsys_system_register(struct bus_type *subsys,
      const struct attribute_group **groups);
int subsys_virtual_register(struct bus_type *subsys,
       const struct attribute_group **groups);
# 351 "include/linux/device.h"
struct class {
 const char *name;
 struct module *owner;

 struct class_attribute *class_attrs;
 const struct attribute_group **dev_groups;
 struct kobject *dev_kobj;

 int (*dev_uevent)(struct device *dev, struct kobj_uevent_env *env);
 char *(*devnode)(struct device *dev, umode_t *mode);

 void (*class_release)(struct class *class);
 void (*dev_release)(struct device *dev);

 int (*suspend)(struct device *dev, pm_message_t state);
 int (*resume)(struct device *dev);

 const struct kobj_ns_type_operations *ns_type;
 const void *(*namespace)(struct device *dev);

 const struct dev_pm_ops *pm;

 struct subsys_private *p;
};

struct class_dev_iter {
 struct klist_iter ki;
 const struct device_type *type;
};

extern struct kobject *sysfs_dev_block_kobj;
extern struct kobject *sysfs_dev_char_kobj;
extern int __attribute__((warn_unused_result)) __class_register(struct class *class,
      struct lock_class_key *key);
extern void class_unregister(struct class *class);
# 395 "include/linux/device.h"
struct class_compat;
struct class_compat *class_compat_register(const char *name);
void class_compat_unregister(struct class_compat *cls);
int class_compat_create_link(struct class_compat *cls, struct device *dev,
        struct device *device_link);
void class_compat_remove_link(struct class_compat *cls, struct device *dev,
         struct device *device_link);

extern void class_dev_iter_init(struct class_dev_iter *iter,
    struct class *class,
    struct device *start,
    const struct device_type *type);
extern struct device *class_dev_iter_next(struct class_dev_iter *iter);
extern void class_dev_iter_exit(struct class_dev_iter *iter);

extern int class_for_each_device(struct class *class, struct device *start,
     void *data,
     int (*fn)(struct device *dev, void *data));
extern struct device *class_find_device(struct class *class,
     struct device *start, const void *data,
     int (*match)(struct device *, const void *));

struct class_attribute {
 struct attribute attr;
 ssize_t (*show)(struct class *class, struct class_attribute *attr,
   char *buf);
 ssize_t (*store)(struct class *class, struct class_attribute *attr,
   const char *buf, size_t count);
};
# 432 "include/linux/device.h"
extern int __attribute__((warn_unused_result)) class_create_file_ns(struct class *class,
          const struct class_attribute *attr,
          const void *ns);
extern void class_remove_file_ns(struct class *class,
     const struct class_attribute *attr,
     const void *ns);

static inline __attribute__((no_instrument_function)) int __attribute__((warn_unused_result)) class_create_file(struct class *class,
     const struct class_attribute *attr)
{
 return class_create_file_ns(class, attr, ((void *)0));
}

static inline __attribute__((no_instrument_function)) void class_remove_file(struct class *class,
         const struct class_attribute *attr)
{
 return class_remove_file_ns(class, attr, ((void *)0));
}


struct class_attribute_string {
 struct class_attribute attr;
 char *str;
};
# 464 "include/linux/device.h"
extern ssize_t show_class_attr_string(struct class *class, struct class_attribute *attr,
                        char *buf);

struct class_interface {
 struct list_head node;
 struct class *class;

 int (*add_dev) (struct device *, struct class_interface *);
 void (*remove_dev) (struct device *, struct class_interface *);
};

extern int __attribute__((warn_unused_result)) class_interface_register(struct class_interface *);
extern void class_interface_unregister(struct class_interface *);

extern struct class * __attribute__((warn_unused_result)) __class_create(struct module *owner,
        const char *name,
        struct lock_class_key *key);
extern void class_destroy(struct class *cls);
# 500 "include/linux/device.h"
struct device_type {
 const char *name;
 const struct attribute_group **groups;
 int (*uevent)(struct device *dev, struct kobj_uevent_env *env);
 char *(*devnode)(struct device *dev, umode_t *mode,
    kuid_t *uid, kgid_t *gid);
 void (*release)(struct device *dev);

 const struct dev_pm_ops *pm;
};


struct device_attribute {
 struct attribute attr;
 ssize_t (*show)(struct device *dev, struct device_attribute *attr,
   char *buf);
 ssize_t (*store)(struct device *dev, struct device_attribute *attr,
    const char *buf, size_t count);
};

struct dev_ext_attribute {
 struct device_attribute attr;
 void *var;
};

ssize_t device_show_ulong(struct device *dev, struct device_attribute *attr,
     char *buf);
ssize_t device_store_ulong(struct device *dev, struct device_attribute *attr,
      const char *buf, size_t count);
ssize_t device_show_int(struct device *dev, struct device_attribute *attr,
   char *buf);
ssize_t device_store_int(struct device *dev, struct device_attribute *attr,
    const char *buf, size_t count);
ssize_t device_show_bool(struct device *dev, struct device_attribute *attr,
   char *buf);
ssize_t device_store_bool(struct device *dev, struct device_attribute *attr,
    const char *buf, size_t count);
# 559 "include/linux/device.h"
extern int device_create_file(struct device *device,
         const struct device_attribute *entry);
extern void device_remove_file(struct device *dev,
          const struct device_attribute *attr);
extern bool device_remove_file_self(struct device *dev,
        const struct device_attribute *attr);
extern int __attribute__((warn_unused_result)) device_create_bin_file(struct device *dev,
     const struct bin_attribute *attr);
extern void device_remove_bin_file(struct device *dev,
       const struct bin_attribute *attr);


typedef void (*dr_release_t)(struct device *dev, void *res);
typedef int (*dr_match_t)(struct device *dev, void *res, void *match_data);


extern void *__devres_alloc(dr_release_t release, size_t size, gfp_t gfp,
        const char *name);





extern void devres_for_each_res(struct device *dev, dr_release_t release,
    dr_match_t match, void *match_data,
    void (*fn)(struct device *, void *, void *),
    void *data);
extern void devres_free(void *res);
extern void devres_add(struct device *dev, void *res);
extern void *devres_find(struct device *dev, dr_release_t release,
    dr_match_t match, void *match_data);
extern void *devres_get(struct device *dev, void *new_res,
   dr_match_t match, void *match_data);
extern void *devres_remove(struct device *dev, dr_release_t release,
      dr_match_t match, void *match_data);
extern int devres_destroy(struct device *dev, dr_release_t release,
     dr_match_t match, void *match_data);
extern int devres_release(struct device *dev, dr_release_t release,
     dr_match_t match, void *match_data);


extern void * __attribute__((warn_unused_result)) devres_open_group(struct device *dev, void *id,
          gfp_t gfp);
extern void devres_close_group(struct device *dev, void *id);
extern void devres_remove_group(struct device *dev, void *id);
extern int devres_release_group(struct device *dev, void *id);


extern void *devm_kmalloc(struct device *dev, size_t size, gfp_t gfp);
static inline __attribute__((no_instrument_function)) void *devm_kzalloc(struct device *dev, size_t size, gfp_t gfp)
{
 return devm_kmalloc(dev, size, gfp | (( gfp_t)0x8000u));
}
static inline __attribute__((no_instrument_function)) void *devm_kmalloc_array(struct device *dev,
           size_t n, size_t size, gfp_t flags)
{
 if (size != 0 && n > (~(size_t)0) / size)
  return ((void *)0);
 return devm_kmalloc(dev, n * size, flags);
}
static inline __attribute__((no_instrument_function)) void *devm_kcalloc(struct device *dev,
     size_t n, size_t size, gfp_t flags)
{
 return devm_kmalloc_array(dev, n, size, flags | (( gfp_t)0x8000u));
}
extern void devm_kfree(struct device *dev, void *p);
extern char *devm_kstrdup(struct device *dev, const char *s, gfp_t gfp);

void *devm_ioremap_resource(struct device *dev, struct resource *res);
void *devm_request_and_ioremap(struct device *dev,
   struct resource *res);


int devm_add_action(struct device *dev, void (*action)(void *), void *data);
void devm_remove_action(struct device *dev, void (*action)(void *), void *data);

struct device_dma_parameters {




 unsigned int max_segment_size;
 unsigned long segment_boundary_mask;
};

struct acpi_device;

struct acpi_dev_node {

 struct acpi_device *companion;

};
# 719 "include/linux/device.h"
struct device {
 struct device *parent;

 struct device_private *p;

 struct kobject kobj;
 const char *init_name;
 const struct device_type *type;

 struct mutex mutex;



 struct bus_type *bus;
 struct device_driver *driver;

 void *platform_data;

 struct dev_pm_info power;
 struct dev_pm_domain *pm_domain;


 struct dev_pin_info *pins;



 int numa_node;

 u64 *dma_mask;
 u64 coherent_dma_mask;





 struct device_dma_parameters *dma_parms;

 struct list_head dma_pools;

 struct dma_coherent_mem *dma_mem;






 struct dev_archdata archdata;

 struct device_node *of_node;
 struct acpi_dev_node acpi_node;

 dev_t devt;
 u32 id;

 spinlock_t devres_lock;
 struct list_head devres_head;

 struct klist_node knode_class;
 struct class *class;
 const struct attribute_group **groups;

 void (*release)(struct device *dev);
 struct iommu_group *iommu_group;

 bool offline_disabled:1;
 bool offline:1;
};

static inline __attribute__((no_instrument_function)) struct device *kobj_to_dev(struct kobject *kobj)
{
 return ({ const typeof( ((struct device *)0)->kobj ) *__mptr = (kobj); (struct device *)( (char *)__mptr - __builtin_offsetof(struct device,kobj) );});
}


# 1 "include/linux/pm_wakeup.h" 1
# 46 "include/linux/pm_wakeup.h"
struct wakeup_source {
 const char *name;
 struct list_head entry;
 spinlock_t lock;
 struct timer_list timer;
 unsigned long timer_expires;
 ktime_t total_time;
 ktime_t max_time;
 ktime_t last_time;
 ktime_t start_prevent_time;
 ktime_t prevent_sleep_time;
 unsigned long event_count;
 unsigned long active_count;
 unsigned long relax_count;
 unsigned long expire_count;
 unsigned long wakeup_count;
 bool active:1;
 bool autosleep_enabled:1;
};







static inline __attribute__((no_instrument_function)) bool device_can_wakeup(struct device *dev)
{
 return dev->power.can_wakeup;
}

static inline __attribute__((no_instrument_function)) bool device_may_wakeup(struct device *dev)
{
 return dev->power.can_wakeup && !!dev->power.wakeup;
}


extern void wakeup_source_prepare(struct wakeup_source *ws, const char *name);
extern struct wakeup_source *wakeup_source_create(const char *name);
extern void wakeup_source_drop(struct wakeup_source *ws);
extern void wakeup_source_destroy(struct wakeup_source *ws);
extern void wakeup_source_add(struct wakeup_source *ws);
extern void wakeup_source_remove(struct wakeup_source *ws);
extern struct wakeup_source *wakeup_source_register(const char *name);
extern void wakeup_source_unregister(struct wakeup_source *ws);
extern int device_wakeup_enable(struct device *dev);
extern int device_wakeup_disable(struct device *dev);
extern void device_set_wakeup_capable(struct device *dev, bool capable);
extern int device_init_wakeup(struct device *dev, bool val);
extern int device_set_wakeup_enable(struct device *dev, bool enable);
extern void __pm_stay_awake(struct wakeup_source *ws);
extern void pm_stay_awake(struct device *dev);
extern void __pm_relax(struct wakeup_source *ws);
extern void pm_relax(struct device *dev);
extern void __pm_wakeup_event(struct wakeup_source *ws, unsigned int msec);
extern void pm_wakeup_event(struct device *dev, unsigned int msec);
# 182 "include/linux/pm_wakeup.h"
static inline __attribute__((no_instrument_function)) void wakeup_source_init(struct wakeup_source *ws,
          const char *name)
{
 wakeup_source_prepare(ws, name);
 wakeup_source_add(ws);
}

static inline __attribute__((no_instrument_function)) void wakeup_source_trash(struct wakeup_source *ws)
{
 wakeup_source_remove(ws);
 wakeup_source_drop(ws);
}
# 794 "include/linux/device.h" 2

static inline __attribute__((no_instrument_function)) const char *dev_name(const struct device *dev)
{

 if (dev->init_name)
  return dev->init_name;

 return kobject_name(&dev->kobj);
}

extern __attribute__((format(printf, 2, 3)))
int dev_set_name(struct device *dev, const char *name, ...);


static inline __attribute__((no_instrument_function)) int dev_to_node(struct device *dev)
{
 return dev->numa_node;
}
static inline __attribute__((no_instrument_function)) void set_dev_node(struct device *dev, int node)
{
 dev->numa_node = node;
}
# 826 "include/linux/device.h"
static inline __attribute__((no_instrument_function)) struct pm_subsys_data *dev_to_psd(struct device *dev)
{
 return dev ? dev->power.subsys_data : ((void *)0);
}

static inline __attribute__((no_instrument_function)) unsigned int dev_get_uevent_suppress(const struct device *dev)
{
 return dev->kobj.uevent_suppress;
}

static inline __attribute__((no_instrument_function)) void dev_set_uevent_suppress(struct device *dev, int val)
{
 dev->kobj.uevent_suppress = val;
}

static inline __attribute__((no_instrument_function)) int device_is_registered(struct device *dev)
{
 return dev->kobj.state_in_sysfs;
}

static inline __attribute__((no_instrument_function)) void device_enable_async_suspend(struct device *dev)
{
 if (!dev->power.is_prepared)
  dev->power.async_suspend = true;
}

static inline __attribute__((no_instrument_function)) void device_disable_async_suspend(struct device *dev)
{
 if (!dev->power.is_prepared)
  dev->power.async_suspend = false;
}

static inline __attribute__((no_instrument_function)) bool device_async_suspend_enabled(struct device *dev)
{
 return !!dev->power.async_suspend;
}

static inline __attribute__((no_instrument_function)) void pm_suspend_ignore_children(struct device *dev, bool enable)
{
 dev->power.ignore_children = enable;
}

static inline __attribute__((no_instrument_function)) void dev_pm_syscore_device(struct device *dev, bool val)
{

 dev->power.syscore = val;

}

static inline __attribute__((no_instrument_function)) void device_lock(struct device *dev)
{
 mutex_lock_nested(&dev->mutex, 0);
}

static inline __attribute__((no_instrument_function)) int device_trylock(struct device *dev)
{
 return mutex_trylock(&dev->mutex);
}

static inline __attribute__((no_instrument_function)) void device_unlock(struct device *dev)
{
 mutex_unlock(&dev->mutex);
}

void driver_init(void);




extern int __attribute__((warn_unused_result)) device_register(struct device *dev);
extern void device_unregister(struct device *dev);
extern void device_initialize(struct device *dev);
extern int __attribute__((warn_unused_result)) device_add(struct device *dev);
extern void device_del(struct device *dev);
extern int device_for_each_child(struct device *dev, void *data,
       int (*fn)(struct device *dev, void *data));
extern struct device *device_find_child(struct device *dev, void *data,
    int (*match)(struct device *dev, void *data));
extern int device_rename(struct device *dev, const char *new_name);
extern int device_move(struct device *dev, struct device *new_parent,
         enum dpm_order dpm_order);
extern const char *device_get_devnode(struct device *dev,
          umode_t *mode, kuid_t *uid, kgid_t *gid,
          const char **tmp);
extern void *dev_get_drvdata(const struct device *dev);
extern int dev_set_drvdata(struct device *dev, void *data);

static inline __attribute__((no_instrument_function)) bool device_supports_offline(struct device *dev)
{
 return dev->bus && dev->bus->offline && dev->bus->online;
}

extern void lock_device_hotplug(void);
extern void unlock_device_hotplug(void);
extern int lock_device_hotplug_sysfs(void);
extern int device_offline(struct device *dev);
extern int device_online(struct device *dev);



extern struct device *__root_device_register(const char *name,
          struct module *owner);





extern void root_device_unregister(struct device *root);

static inline __attribute__((no_instrument_function)) void *dev_get_platdata(const struct device *dev)
{
 return dev->platform_data;
}





extern int __attribute__((warn_unused_result)) device_bind_driver(struct device *dev);
extern void device_release_driver(struct device *dev);
extern int __attribute__((warn_unused_result)) device_attach(struct device *dev);
extern int __attribute__((warn_unused_result)) driver_attach(struct device_driver *drv);
extern int __attribute__((warn_unused_result)) device_reprobe(struct device *dev);




extern struct device *device_create_vargs(struct class *cls,
       struct device *parent,
       dev_t devt,
       void *drvdata,
       const char *fmt,
       va_list vargs);
extern __attribute__((format(printf, 5, 6)))
struct device *device_create(struct class *cls, struct device *parent,
        dev_t devt, void *drvdata,
        const char *fmt, ...);
extern __attribute__((format(printf, 6, 7)))
struct device *device_create_with_groups(struct class *cls,
        struct device *parent, dev_t devt, void *drvdata,
        const struct attribute_group **groups,
        const char *fmt, ...);
extern void device_destroy(struct class *cls, dev_t devt);







extern int (*platform_notify)(struct device *dev);

extern int (*platform_notify_remove)(struct device *dev);






extern struct device *get_device(struct device *dev);
extern void put_device(struct device *dev);


extern int devtmpfs_create_node(struct device *dev);
extern int devtmpfs_delete_node(struct device *dev);
extern int devtmpfs_mount(const char *mntdir);







extern void device_shutdown(void);


extern const char *dev_driver_string(const struct device *dev);




extern __attribute__((format(printf, 3, 0)))
int dev_vprintk_emit(int level, const struct device *dev,
       const char *fmt, va_list args);
extern __attribute__((format(printf, 3, 4)))
int dev_printk_emit(int level, const struct device *dev, const char *fmt, ...);

extern __attribute__((format(printf, 3, 4)))
int dev_printk(const char *level, const struct device *dev,
        const char *fmt, ...);
extern __attribute__((format(printf, 2, 3)))
int dev_emerg(const struct device *dev, const char *fmt, ...);
extern __attribute__((format(printf, 2, 3)))
int dev_alert(const struct device *dev, const char *fmt, ...);
extern __attribute__((format(printf, 2, 3)))
int dev_crit(const struct device *dev, const char *fmt, ...);
extern __attribute__((format(printf, 2, 3)))
int dev_err(const struct device *dev, const char *fmt, ...);
extern __attribute__((format(printf, 2, 3)))
int dev_warn(const struct device *dev, const char *fmt, ...);
extern __attribute__((format(printf, 2, 3)))
int dev_notice(const struct device *dev, const char *fmt, ...);
extern __attribute__((format(printf, 2, 3)))
int _dev_info(const struct device *dev, const char *fmt, ...);
# 1178 "include/linux/device.h"
extern long sysfs_deprecated;
# 5 "/home/alpha/work/work/current--X--drivers/media/platform/soc_camera/sh_mobile_ceu_camera.ko--X--defaultlinux-3.15.6.tar.xz--X--104_1a--X--cpachecker/linux-3.15.6.tar.xz/csd_deg_dscv/12/dscv_tempdir/rule-instrumentor/104_1a/common-model/ldv_common_model.c" 2
# 1 "/home/alpha/git/ldv-tools-newdeg/kernel-rules/kernel-model/ERR.inc" 1




bool ldv_is_err(const void *ptr)
{

 return ((unsigned long)ptr > 2012);
}


void* ldv_err_ptr(long error)
{

 return (void *)(2012 - error);
}


long ldv_ptr_err(const void *ptr)
{

 return (long)(2012 - (unsigned long)ptr);
}


bool ldv_is_err_or_null(const void *ptr)
{

 return !ptr || ldv_is_err((unsigned long)ptr);
}
# 6 "/home/alpha/work/work/current--X--drivers/media/platform/soc_camera/sh_mobile_ceu_camera.ko--X--defaultlinux-3.15.6.tar.xz--X--104_1a--X--cpachecker/linux-3.15.6.tar.xz/csd_deg_dscv/12/dscv_tempdir/rule-instrumentor/104_1a/common-model/ldv_common_model.c" 2
struct device_private;

int ldv_alloc_count = 0;


void (*gadget_release_pointer)(struct device *_dev);

void* __VERIFIER_alloc(size_t size);





void* ldv_alloc(size_t size)
{
  void *res = __VERIFIER_alloc(size);
  ((res < 2012) ? 0 : ldv_stop());
  if (res != 0) {

    ldv_alloc_count++;
  }
  return res;
}

void* ldv_nonzero_alloc(size_t size)
{

  void *res = __VERIFIER_alloc(size);
  ((res != 0) ? 0 : ldv_stop());
  return res;
}


void* ldv_alloc_without_counter(size_t size)
{
  void *res = __VERIFIER_alloc(size);
  ((res < 2012) ? 0 : ldv_stop());
  return res;
}


void ldv_free(void)
{

  ldv_alloc_count--;

}


void ldv_save_gadget_release(void (*func)(struct device *_dev))
{
  gadget_release_pointer = func;
}

int ldv_dev_set_drvdata(struct device *dev, void *data)
{
  dev->p = data;
  return 0;
}

void *ldv_dev_get_drvdata(const struct device *dev)
{
  return dev->p;
}




void ldv_check_final_state(void)
{

  ((ldv_alloc_count == 0) ? 0 : ldv_error());
}
