extern void __VERIFIER_error() __attribute__ ((__noreturn__));
typedef signed char __s8;
typedef unsigned char __u8;
typedef short __s16;
typedef unsigned short __u16;
typedef int __s32;
typedef unsigned int __u32;
typedef long long __s64;
typedef unsigned long long __u64;
typedef unsigned char u8;
typedef short s16;
typedef unsigned short u16;
typedef int s32;
typedef unsigned int u32;
typedef long long s64;
typedef unsigned long long u64;
typedef long __kernel_long_t;
typedef unsigned long __kernel_ulong_t;
typedef int __kernel_pid_t;
typedef unsigned int __kernel_uid32_t;
typedef unsigned int __kernel_gid32_t;
typedef __kernel_ulong_t __kernel_size_t;
typedef __kernel_long_t __kernel_ssize_t;
typedef long long __kernel_loff_t;
typedef __kernel_long_t __kernel_time_t;
typedef __kernel_long_t __kernel_clock_t;
typedef int __kernel_timer_t;
typedef int __kernel_clockid_t;
typedef __u32 __kernel_dev_t;
typedef __kernel_dev_t dev_t;
typedef unsigned short umode_t;
typedef __kernel_pid_t pid_t;
typedef __kernel_clockid_t clockid_t;
typedef _Bool bool;
typedef __kernel_uid32_t uid_t;
typedef __kernel_gid32_t gid_t;
typedef __kernel_loff_t loff_t;
typedef __kernel_size_t size_t;
typedef __kernel_ssize_t ssize_t;
typedef __kernel_time_t time_t;
typedef __s32 int32_t;
typedef __u32 uint32_t;
typedef unsigned long sector_t;
typedef unsigned long blkcnt_t;
typedef unsigned int gfp_t;
typedef unsigned int fmode_t;
struct __anonstruct_atomic_t_6 {
   int counter ;
};
typedef struct __anonstruct_atomic_t_6 atomic_t;
struct __anonstruct_atomic64_t_7 {
   long counter ;
};
typedef struct __anonstruct_atomic64_t_7 atomic64_t;
struct list_head {
   struct list_head *next ;
   struct list_head *prev ;
};
struct hlist_node;
struct hlist_head {
   struct hlist_node *first ;
};
struct hlist_node {
   struct hlist_node *next ;
   struct hlist_node **pprev ;
};
struct callback_head {
   struct callback_head *next ;
   void (*func)(struct callback_head * ) ;
};
struct module;
typedef void (*ctor_fn_t)(void);
struct file_operations;
struct device;
struct completion;
struct pt_regs;
struct pid;
typedef u16 __ticket_t;
typedef u32 __ticketpair_t;
struct __raw_tickets {
   __ticket_t head ;
   __ticket_t tail ;
};
union __anonunion_ldv_2023_8 {
   __ticketpair_t head_tail ;
   struct __raw_tickets tickets ;
};
struct arch_spinlock {
   union __anonunion_ldv_2023_8 ldv_2023 ;
};
typedef struct arch_spinlock arch_spinlock_t;
struct __anonstruct_ldv_2030_10 {
   u32 read ;
   s32 write ;
};
union __anonunion_arch_rwlock_t_9 {
   s64 lock ;
   struct __anonstruct_ldv_2030_10 ldv_2030 ;
};
typedef union __anonunion_arch_rwlock_t_9 arch_rwlock_t;
struct task_struct;
struct lockdep_map;
struct mm_struct;
struct pt_regs {
   unsigned long r15 ;
   unsigned long r14 ;
   unsigned long r13 ;
   unsigned long r12 ;
   unsigned long bp ;
   unsigned long bx ;
   unsigned long r11 ;
   unsigned long r10 ;
   unsigned long r9 ;
   unsigned long r8 ;
   unsigned long ax ;
   unsigned long cx ;
   unsigned long dx ;
   unsigned long si ;
   unsigned long di ;
   unsigned long orig_ax ;
   unsigned long ip ;
   unsigned long cs ;
   unsigned long flags ;
   unsigned long sp ;
   unsigned long ss ;
};
struct __anonstruct_ldv_2147_12 {
   unsigned int a ;
   unsigned int b ;
};
struct __anonstruct_ldv_2162_13 {
   u16 limit0 ;
   u16 base0 ;
   unsigned char base1 ;
   unsigned char type : 4 ;
   unsigned char s : 1 ;
   unsigned char dpl : 2 ;
   unsigned char p : 1 ;
   unsigned char limit : 4 ;
   unsigned char avl : 1 ;
   unsigned char l : 1 ;
   unsigned char d : 1 ;
   unsigned char g : 1 ;
   unsigned char base2 ;
};
union __anonunion_ldv_2163_11 {
   struct __anonstruct_ldv_2147_12 ldv_2147 ;
   struct __anonstruct_ldv_2162_13 ldv_2162 ;
};
struct desc_struct {
   union __anonunion_ldv_2163_11 ldv_2163 ;
};
typedef unsigned long pgdval_t;
typedef unsigned long pgprotval_t;
struct pgprot {
   pgprotval_t pgprot ;
};
typedef struct pgprot pgprot_t;
struct __anonstruct_pgd_t_15 {
   pgdval_t pgd ;
};
typedef struct __anonstruct_pgd_t_15 pgd_t;
struct page;
typedef struct page *pgtable_t;
struct file;
struct seq_file;
struct thread_struct;
struct cpumask;
struct kernel_vm86_regs {
   struct pt_regs pt ;
   unsigned short es ;
   unsigned short __esh ;
   unsigned short ds ;
   unsigned short __dsh ;
   unsigned short fs ;
   unsigned short __fsh ;
   unsigned short gs ;
   unsigned short __gsh ;
};
union __anonunion_ldv_2766_18 {
   struct pt_regs *regs ;
   struct kernel_vm86_regs *vm86 ;
};
struct math_emu_info {
   long ___orig_eip ;
   union __anonunion_ldv_2766_18 ldv_2766 ;
};
struct bug_entry {
   int bug_addr_disp ;
   int file_disp ;
   unsigned short line ;
   unsigned short flags ;
};
struct cpumask {
   unsigned long bits[64U] ;
};
typedef struct cpumask cpumask_t;
typedef struct cpumask *cpumask_var_t;
struct static_key;
struct i387_fsave_struct {
   u32 cwd ;
   u32 swd ;
   u32 twd ;
   u32 fip ;
   u32 fcs ;
   u32 foo ;
   u32 fos ;
   u32 st_space[20U] ;
   u32 status ;
};
struct __anonstruct_ldv_5121_23 {
   u64 rip ;
   u64 rdp ;
};
struct __anonstruct_ldv_5127_24 {
   u32 fip ;
   u32 fcs ;
   u32 foo ;
   u32 fos ;
};
union __anonunion_ldv_5128_22 {
   struct __anonstruct_ldv_5121_23 ldv_5121 ;
   struct __anonstruct_ldv_5127_24 ldv_5127 ;
};
union __anonunion_ldv_5137_25 {
   u32 padding1[12U] ;
   u32 sw_reserved[12U] ;
};
struct i387_fxsave_struct {
   u16 cwd ;
   u16 swd ;
   u16 twd ;
   u16 fop ;
   union __anonunion_ldv_5128_22 ldv_5128 ;
   u32 mxcsr ;
   u32 mxcsr_mask ;
   u32 st_space[32U] ;
   u32 xmm_space[64U] ;
   u32 padding[12U] ;
   union __anonunion_ldv_5137_25 ldv_5137 ;
};
struct i387_soft_struct {
   u32 cwd ;
   u32 swd ;
   u32 twd ;
   u32 fip ;
   u32 fcs ;
   u32 foo ;
   u32 fos ;
   u32 st_space[20U] ;
   u8 ftop ;
   u8 changed ;
   u8 lookahead ;
   u8 no_update ;
   u8 rm ;
   u8 alimit ;
   struct math_emu_info *info ;
   u32 entry_eip ;
};
struct ymmh_struct {
   u32 ymmh_space[64U] ;
};
struct xsave_hdr_struct {
   u64 xstate_bv ;
   u64 reserved1[2U] ;
   u64 reserved2[5U] ;
};
struct xsave_struct {
   struct i387_fxsave_struct i387 ;
   struct xsave_hdr_struct xsave_hdr ;
   struct ymmh_struct ymmh ;
};
union thread_xstate {
   struct i387_fsave_struct fsave ;
   struct i387_fxsave_struct fxsave ;
   struct i387_soft_struct soft ;
   struct xsave_struct xsave ;
};
struct fpu {
   unsigned int last_cpu ;
   unsigned int has_fpu ;
   union thread_xstate *state ;
};
struct kmem_cache;
struct perf_event;
struct thread_struct {
   struct desc_struct tls_array[3U] ;
   unsigned long sp0 ;
   unsigned long sp ;
   unsigned long usersp ;
   unsigned short es ;
   unsigned short ds ;
   unsigned short fsindex ;
   unsigned short gsindex ;
   unsigned long fs ;
   unsigned long gs ;
   struct perf_event *ptrace_bps[4U] ;
   unsigned long debugreg6 ;
   unsigned long ptrace_dr7 ;
   unsigned long cr2 ;
   unsigned long trap_nr ;
   unsigned long error_code ;
   struct fpu fpu ;
   unsigned long *io_bitmap_ptr ;
   unsigned long iopl ;
   unsigned int io_bitmap_max ;
};
typedef atomic64_t atomic_long_t;
struct stack_trace {
   unsigned int nr_entries ;
   unsigned int max_entries ;
   unsigned long *entries ;
   int skip ;
};
struct lockdep_subclass_key {
   char __one_byte ;
} __attribute__((__packed__)) ;
struct lock_class_key {
   struct lockdep_subclass_key subkeys[8U] ;
};
struct lock_class {
   struct list_head hash_entry ;
   struct list_head lock_entry ;
   struct lockdep_subclass_key *key ;
   unsigned int subclass ;
   unsigned int dep_gen_id ;
   unsigned long usage_mask ;
   struct stack_trace usage_traces[13U] ;
   struct list_head locks_after ;
   struct list_head locks_before ;
   unsigned int version ;
   unsigned long ops ;
   char const *name ;
   int name_version ;
   unsigned long contention_point[4U] ;
   unsigned long contending_point[4U] ;
};
struct lockdep_map {
   struct lock_class_key *key ;
   struct lock_class *class_cache[2U] ;
   char const *name ;
   int cpu ;
   unsigned long ip ;
};
struct held_lock {
   u64 prev_chain_key ;
   unsigned long acquire_ip ;
   struct lockdep_map *instance ;
   struct lockdep_map *nest_lock ;
   u64 waittime_stamp ;
   u64 holdtime_stamp ;
   unsigned short class_idx : 13 ;
   unsigned char irq_context : 2 ;
   unsigned char trylock : 1 ;
   unsigned char read : 2 ;
   unsigned char check : 2 ;
   unsigned char hardirqs_off : 1 ;
   unsigned short references : 11 ;
};
struct raw_spinlock {
   arch_spinlock_t raw_lock ;
   unsigned int magic ;
   unsigned int owner_cpu ;
   void *owner ;
   struct lockdep_map dep_map ;
};
typedef struct raw_spinlock raw_spinlock_t;
struct __anonstruct_ldv_5956_29 {
   u8 __padding[24U] ;
   struct lockdep_map dep_map ;
};
union __anonunion_ldv_5957_28 {
   struct raw_spinlock rlock ;
   struct __anonstruct_ldv_5956_29 ldv_5956 ;
};
struct spinlock {
   union __anonunion_ldv_5957_28 ldv_5957 ;
};
typedef struct spinlock spinlock_t;
struct __anonstruct_rwlock_t_30 {
   arch_rwlock_t raw_lock ;
   unsigned int magic ;
   unsigned int owner_cpu ;
   void *owner ;
   struct lockdep_map dep_map ;
};
typedef struct __anonstruct_rwlock_t_30 rwlock_t;
struct mutex {
   atomic_t count ;
   spinlock_t wait_lock ;
   struct list_head wait_list ;
   struct task_struct *owner ;
   char const *name ;
   void *magic ;
   struct lockdep_map dep_map ;
};
struct mutex_waiter {
   struct list_head list ;
   struct task_struct *task ;
   void *magic ;
};
struct timespec;
struct seqcount {
   unsigned int sequence ;
};
typedef struct seqcount seqcount_t;
struct timespec {
   __kernel_time_t tv_sec ;
   long tv_nsec ;
};
struct user_namespace;
typedef uid_t kuid_t;
typedef gid_t kgid_t;
struct kstat {
   u64 ino ;
   dev_t dev ;
   umode_t mode ;
   unsigned int nlink ;
   kuid_t uid ;
   kgid_t gid ;
   dev_t rdev ;
   loff_t size ;
   struct timespec atime ;
   struct timespec mtime ;
   struct timespec ctime ;
   unsigned long blksize ;
   unsigned long long blocks ;
};
struct __wait_queue_head {
   spinlock_t lock ;
   struct list_head task_list ;
};
typedef struct __wait_queue_head wait_queue_head_t;
struct __anonstruct_nodemask_t_36 {
   unsigned long bits[16U] ;
};
typedef struct __anonstruct_nodemask_t_36 nodemask_t;
struct rw_semaphore;
struct rw_semaphore {
   long count ;
   raw_spinlock_t wait_lock ;
   struct list_head wait_list ;
   struct lockdep_map dep_map ;
};
struct completion {
   unsigned int done ;
   wait_queue_head_t wait ;
};
union ktime {
   s64 tv64 ;
};
typedef union ktime ktime_t;
struct tvec_base;
struct timer_list {
   struct list_head entry ;
   unsigned long expires ;
   struct tvec_base *base ;
   void (*function)(unsigned long ) ;
   unsigned long data ;
   int slack ;
   int start_pid ;
   void *start_site ;
   char start_comm[16U] ;
   struct lockdep_map lockdep_map ;
};
struct hrtimer;
enum hrtimer_restart;
struct work_struct;
struct work_struct {
   atomic_long_t data ;
   struct list_head entry ;
   void (*func)(struct work_struct * ) ;
   struct lockdep_map lockdep_map ;
};
struct delayed_work {
   struct work_struct work ;
   struct timer_list timer ;
   int cpu ;
};
struct pm_message {
   int event ;
};
typedef struct pm_message pm_message_t;
struct dev_pm_ops {
   int (*prepare)(struct device * ) ;
   void (*complete)(struct device * ) ;
   int (*suspend)(struct device * ) ;
   int (*resume)(struct device * ) ;
   int (*freeze)(struct device * ) ;
   int (*thaw)(struct device * ) ;
   int (*poweroff)(struct device * ) ;
   int (*restore)(struct device * ) ;
   int (*suspend_late)(struct device * ) ;
   int (*resume_early)(struct device * ) ;
   int (*freeze_late)(struct device * ) ;
   int (*thaw_early)(struct device * ) ;
   int (*poweroff_late)(struct device * ) ;
   int (*restore_early)(struct device * ) ;
   int (*suspend_noirq)(struct device * ) ;
   int (*resume_noirq)(struct device * ) ;
   int (*freeze_noirq)(struct device * ) ;
   int (*thaw_noirq)(struct device * ) ;
   int (*poweroff_noirq)(struct device * ) ;
   int (*restore_noirq)(struct device * ) ;
   int (*runtime_suspend)(struct device * ) ;
   int (*runtime_resume)(struct device * ) ;
   int (*runtime_idle)(struct device * ) ;
};
enum rpm_status {
    RPM_ACTIVE = 0,
    RPM_RESUMING = 1,
    RPM_SUSPENDED = 2,
    RPM_SUSPENDING = 3
} ;
enum rpm_request {
    RPM_REQ_NONE = 0,
    RPM_REQ_IDLE = 1,
    RPM_REQ_SUSPEND = 2,
    RPM_REQ_AUTOSUSPEND = 3,
    RPM_REQ_RESUME = 4
} ;
struct wakeup_source;
struct pm_subsys_data {
   spinlock_t lock ;
   unsigned int refcount ;
};
struct dev_pm_qos_request;
struct pm_qos_constraints;
struct dev_pm_info {
   pm_message_t power_state ;
   unsigned char can_wakeup : 1 ;
   unsigned char async_suspend : 1 ;
   bool is_prepared ;
   bool is_suspended ;
   bool ignore_children ;
   bool early_init ;
   spinlock_t lock ;
   struct list_head entry ;
   struct completion completion ;
   struct wakeup_source *wakeup ;
   bool wakeup_path ;
   bool syscore ;
   struct timer_list suspend_timer ;
   unsigned long timer_expires ;
   struct work_struct work ;
   wait_queue_head_t wait_queue ;
   atomic_t usage_count ;
   atomic_t child_count ;
   unsigned char disable_depth : 3 ;
   unsigned char idle_notification : 1 ;
   unsigned char request_pending : 1 ;
   unsigned char deferred_resume : 1 ;
   unsigned char run_wake : 1 ;
   unsigned char runtime_auto : 1 ;
   unsigned char no_callbacks : 1 ;
   unsigned char irq_safe : 1 ;
   unsigned char use_autosuspend : 1 ;
   unsigned char timer_autosuspends : 1 ;
   enum rpm_request request ;
   enum rpm_status runtime_status ;
   int runtime_error ;
   int autosuspend_delay ;
   unsigned long last_busy ;
   unsigned long active_jiffies ;
   unsigned long suspended_jiffies ;
   unsigned long accounting_timestamp ;
   struct dev_pm_qos_request *pq_req ;
   struct pm_subsys_data *subsys_data ;
   struct pm_qos_constraints *constraints ;
};
struct dev_pm_domain {
   struct dev_pm_ops ops ;
};
struct __anonstruct_mm_context_t_101 {
   void *ldt ;
   int size ;
   unsigned short ia32_compat ;
   struct mutex lock ;
   void *vdso ;
};
typedef struct __anonstruct_mm_context_t_101 mm_context_t;
struct vm_area_struct;
struct rb_node {
   unsigned long __rb_parent_color ;
   struct rb_node *rb_right ;
   struct rb_node *rb_left ;
};
struct rb_root {
   struct rb_node *rb_node ;
};
struct nsproxy;
struct cred;
typedef __u64 Elf64_Addr;
typedef __u16 Elf64_Half;
typedef __u32 Elf64_Word;
typedef __u64 Elf64_Xword;
struct elf64_sym {
   Elf64_Word st_name ;
   unsigned char st_info ;
   unsigned char st_other ;
   Elf64_Half st_shndx ;
   Elf64_Addr st_value ;
   Elf64_Xword st_size ;
};
typedef struct elf64_sym Elf64_Sym;
struct sock;
struct kobject;
enum kobj_ns_type {
    KOBJ_NS_TYPE_NONE = 0,
    KOBJ_NS_TYPE_NET = 1,
    KOBJ_NS_TYPES = 2
} ;
struct kobj_ns_type_operations {
   enum kobj_ns_type type ;
   void *(*grab_current_ns)(void) ;
   void const *(*netlink_ns)(struct sock * ) ;
   void const *(*initial_ns)(void) ;
   void (*drop_ns)(void * ) ;
};
struct attribute {
   char const *name ;
   umode_t mode ;
   bool ignore_lockdep ;
   struct lock_class_key *key ;
   struct lock_class_key skey ;
};
struct attribute_group {
   char const *name ;
   umode_t (*is_visible)(struct kobject * , struct attribute * , int ) ;
   struct attribute **attrs ;
};
struct bin_attribute {
   struct attribute attr ;
   size_t size ;
   void *private ;
   ssize_t (*read)(struct file * , struct kobject * , struct bin_attribute * , char * ,
                   loff_t , size_t ) ;
   ssize_t (*write)(struct file * , struct kobject * , struct bin_attribute * , char * ,
                    loff_t , size_t ) ;
   int (*mmap)(struct file * , struct kobject * , struct bin_attribute * , struct vm_area_struct * ) ;
};
struct sysfs_ops {
   ssize_t (*show)(struct kobject * , struct attribute * , char * ) ;
   ssize_t (*store)(struct kobject * , struct attribute * , char const * , size_t ) ;
   void const *(*namespace)(struct kobject * , struct attribute const * ) ;
};
struct sysfs_dirent;
struct kref {
   atomic_t refcount ;
};
struct kset;
struct kobj_type;
struct kobject {
   char const *name ;
   struct list_head entry ;
   struct kobject *parent ;
   struct kset *kset ;
   struct kobj_type *ktype ;
   struct sysfs_dirent *sd ;
   struct kref kref ;
   unsigned char state_initialized : 1 ;
   unsigned char state_in_sysfs : 1 ;
   unsigned char state_add_uevent_sent : 1 ;
   unsigned char state_remove_uevent_sent : 1 ;
   unsigned char uevent_suppress : 1 ;
};
struct kobj_type {
   void (*release)(struct kobject * ) ;
   struct sysfs_ops const *sysfs_ops ;
   struct attribute **default_attrs ;
   struct kobj_ns_type_operations const *(*child_ns_type)(struct kobject * ) ;
   void const *(*namespace)(struct kobject * ) ;
};
struct kobj_uevent_env {
   char *envp[32U] ;
   int envp_idx ;
   char buf[2048U] ;
   int buflen ;
};
struct kset_uevent_ops {
   int (* const filter)(struct kset * , struct kobject * ) ;
   char const *(* const name)(struct kset * , struct kobject * ) ;
   int (* const uevent)(struct kset * , struct kobject * , struct kobj_uevent_env * ) ;
};
struct kset {
   struct list_head list ;
   spinlock_t list_lock ;
   struct kobject kobj ;
   struct kset_uevent_ops const *uevent_ops ;
};
struct kernel_param;
struct kernel_param_ops {
   int (*set)(char const * , struct kernel_param const * ) ;
   int (*get)(char * , struct kernel_param const * ) ;
   void (*free)(void * ) ;
};
struct kparam_string;
struct kparam_array;
union __anonunion_ldv_13733_134 {
   void *arg ;
   struct kparam_string const *str ;
   struct kparam_array const *arr ;
};
struct kernel_param {
   char const *name ;
   struct kernel_param_ops const *ops ;
   u16 perm ;
   s16 level ;
   union __anonunion_ldv_13733_134 ldv_13733 ;
};
struct kparam_string {
   unsigned int maxlen ;
   char *string ;
};
struct kparam_array {
   unsigned int max ;
   unsigned int elemsize ;
   unsigned int *num ;
   struct kernel_param_ops const *ops ;
   void *elem ;
};
struct static_key {
   atomic_t enabled ;
};
struct tracepoint;
struct tracepoint_func {
   void *func ;
   void *data ;
};
struct tracepoint {
   char const *name ;
   struct static_key key ;
   void (*regfunc)(void) ;
   void (*unregfunc)(void) ;
   struct tracepoint_func *funcs ;
};
struct kernel_symbol {
   unsigned long value ;
   char const *name ;
};
struct mod_arch_specific {
};
struct module_param_attrs;
struct module_kobject {
   struct kobject kobj ;
   struct module *mod ;
   struct kobject *drivers_dir ;
   struct module_param_attrs *mp ;
};
struct module_attribute {
   struct attribute attr ;
   ssize_t (*show)(struct module_attribute * , struct module_kobject * , char * ) ;
   ssize_t (*store)(struct module_attribute * , struct module_kobject * , char const * ,
                    size_t ) ;
   void (*setup)(struct module * , char const * ) ;
   int (*test)(struct module * ) ;
   void (*free)(struct module * ) ;
};
struct exception_table_entry;
enum module_state {
    MODULE_STATE_LIVE = 0,
    MODULE_STATE_COMING = 1,
    MODULE_STATE_GOING = 2
} ;
struct module_ref {
   unsigned long incs ;
   unsigned long decs ;
};
struct module_sect_attrs;
struct module_notes_attrs;
struct ftrace_event_call;
struct module {
   enum module_state state ;
   struct list_head list ;
   char name[56U] ;
   struct module_kobject mkobj ;
   struct module_attribute *modinfo_attrs ;
   char const *version ;
   char const *srcversion ;
   struct kobject *holders_dir ;
   struct kernel_symbol const *syms ;
   unsigned long const *crcs ;
   unsigned int num_syms ;
   struct kernel_param *kp ;
   unsigned int num_kp ;
   unsigned int num_gpl_syms ;
   struct kernel_symbol const *gpl_syms ;
   unsigned long const *gpl_crcs ;
   struct kernel_symbol const *unused_syms ;
   unsigned long const *unused_crcs ;
   unsigned int num_unused_syms ;
   unsigned int num_unused_gpl_syms ;
   struct kernel_symbol const *unused_gpl_syms ;
   unsigned long const *unused_gpl_crcs ;
   struct kernel_symbol const *gpl_future_syms ;
   unsigned long const *gpl_future_crcs ;
   unsigned int num_gpl_future_syms ;
   unsigned int num_exentries ;
   struct exception_table_entry *extable ;
   int (*init)(void) ;
   void *module_init ;
   void *module_core ;
   unsigned int init_size ;
   unsigned int core_size ;
   unsigned int init_text_size ;
   unsigned int core_text_size ;
   unsigned int init_ro_size ;
   unsigned int core_ro_size ;
   struct mod_arch_specific arch ;
   unsigned int taints ;
   unsigned int num_bugs ;
   struct list_head bug_list ;
   struct bug_entry *bug_table ;
   Elf64_Sym *symtab ;
   Elf64_Sym *core_symtab ;
   unsigned int num_symtab ;
   unsigned int core_num_syms ;
   char *strtab ;
   char *core_strtab ;
   struct module_sect_attrs *sect_attrs ;
   struct module_notes_attrs *notes_attrs ;
   char *args ;
   void *percpu ;
   unsigned int percpu_size ;
   unsigned int num_tracepoints ;
   struct tracepoint * const *tracepoints_ptrs ;
   unsigned int num_trace_bprintk_fmt ;
   char const **trace_bprintk_fmt_start ;
   struct ftrace_event_call **trace_events ;
   unsigned int num_trace_events ;
   struct list_head source_list ;
   struct list_head target_list ;
   struct task_struct *waiter ;
   void (*exit)(void) ;
   struct module_ref *refptr ;
   ctor_fn_t (**ctors)(void) ;
   unsigned int num_ctors ;
};
struct kmem_cache_cpu {
   void **freelist ;
   unsigned long tid ;
   struct page *page ;
   struct page *partial ;
   unsigned int stat[26U] ;
};
struct kmem_cache_node {
   spinlock_t list_lock ;
   unsigned long nr_partial ;
   struct list_head partial ;
   atomic_long_t nr_slabs ;
   atomic_long_t total_objects ;
   struct list_head full ;
};
struct kmem_cache_order_objects {
   unsigned long x ;
};
struct kmem_cache {
   struct kmem_cache_cpu *cpu_slab ;
   unsigned long flags ;
   unsigned long min_partial ;
   int size ;
   int object_size ;
   int offset ;
   int cpu_partial ;
   struct kmem_cache_order_objects oo ;
   struct kmem_cache_order_objects max ;
   struct kmem_cache_order_objects min ;
   gfp_t allocflags ;
   int refcount ;
   void (*ctor)(void * ) ;
   int inuse ;
   int align ;
   int reserved ;
   char const *name ;
   struct list_head list ;
   struct kobject kobj ;
   int remote_node_defrag_ratio ;
   struct kmem_cache_node *node[1024U] ;
};
enum fe_type {
    FE_QPSK = 0,
    FE_QAM = 1,
    FE_OFDM = 2,
    FE_ATSC = 3
} ;
typedef enum fe_type fe_type_t;
enum fe_caps {
    FE_IS_STUPID = 0,
    FE_CAN_INVERSION_AUTO = 1,
    FE_CAN_FEC_1_2 = 2,
    FE_CAN_FEC_2_3 = 4,
    FE_CAN_FEC_3_4 = 8,
    FE_CAN_FEC_4_5 = 16,
    FE_CAN_FEC_5_6 = 32,
    FE_CAN_FEC_6_7 = 64,
    FE_CAN_FEC_7_8 = 128,
    FE_CAN_FEC_8_9 = 256,
    FE_CAN_FEC_AUTO = 512,
    FE_CAN_QPSK = 1024,
    FE_CAN_QAM_16 = 2048,
    FE_CAN_QAM_32 = 4096,
    FE_CAN_QAM_64 = 8192,
    FE_CAN_QAM_128 = 16384,
    FE_CAN_QAM_256 = 32768,
    FE_CAN_QAM_AUTO = 65536,
    FE_CAN_TRANSMISSION_MODE_AUTO = 131072,
    FE_CAN_BANDWIDTH_AUTO = 262144,
    FE_CAN_GUARD_INTERVAL_AUTO = 524288,
    FE_CAN_HIERARCHY_AUTO = 1048576,
    FE_CAN_8VSB = 2097152,
    FE_CAN_16VSB = 4194304,
    FE_HAS_EXTENDED_CAPS = 8388608,
    FE_CAN_MULTISTREAM = 67108864,
    FE_CAN_TURBO_FEC = 134217728,
    FE_CAN_2G_MODULATION = 268435456,
    FE_NEEDS_BENDING = 536870912,
    FE_CAN_RECOVER = 1073741824,
    FE_CAN_MUTE_TS = 2147483648L
} ;
typedef enum fe_caps fe_caps_t;
struct dvb_frontend_info {
   char name[128U] ;
   fe_type_t type ;
   __u32 frequency_min ;
   __u32 frequency_max ;
   __u32 frequency_stepsize ;
   __u32 frequency_tolerance ;
   __u32 symbol_rate_min ;
   __u32 symbol_rate_max ;
   __u32 symbol_rate_tolerance ;
   __u32 notifier_delay ;
   fe_caps_t caps ;
};
struct dvb_diseqc_master_cmd {
   __u8 msg[6U] ;
   __u8 msg_len ;
};
struct dvb_diseqc_slave_reply {
   __u8 msg[4U] ;
   __u8 msg_len ;
   int timeout ;
};
enum fe_sec_voltage {
    SEC_VOLTAGE_13 = 0,
    SEC_VOLTAGE_18 = 1,
    SEC_VOLTAGE_OFF = 2
} ;
typedef enum fe_sec_voltage fe_sec_voltage_t;
enum fe_sec_tone_mode {
    SEC_TONE_ON = 0,
    SEC_TONE_OFF = 1
} ;
typedef enum fe_sec_tone_mode fe_sec_tone_mode_t;
enum fe_sec_mini_cmd {
    SEC_MINI_A = 0,
    SEC_MINI_B = 1
} ;
typedef enum fe_sec_mini_cmd fe_sec_mini_cmd_t;
enum fe_status {
    FE_HAS_SIGNAL = 1,
    FE_HAS_CARRIER = 2,
    FE_HAS_VITERBI = 4,
    FE_HAS_SYNC = 8,
    FE_HAS_LOCK = 16,
    FE_TIMEDOUT = 32,
    FE_REINIT = 64
} ;
typedef enum fe_status fe_status_t;
enum fe_spectral_inversion {
    INVERSION_OFF = 0,
    INVERSION_ON = 1,
    INVERSION_AUTO = 2
} ;
typedef enum fe_spectral_inversion fe_spectral_inversion_t;
enum fe_code_rate {
    FEC_NONE = 0,
    FEC_1_2 = 1,
    FEC_2_3 = 2,
    FEC_3_4 = 3,
    FEC_4_5 = 4,
    FEC_5_6 = 5,
    FEC_6_7 = 6,
    FEC_7_8 = 7,
    FEC_8_9 = 8,
    FEC_AUTO = 9,
    FEC_3_5 = 10,
    FEC_9_10 = 11,
    FEC_2_5 = 12
} ;
typedef enum fe_code_rate fe_code_rate_t;
enum fe_modulation {
    QPSK = 0,
    QAM_16 = 1,
    QAM_32 = 2,
    QAM_64 = 3,
    QAM_128 = 4,
    QAM_256 = 5,
    QAM_AUTO = 6,
    VSB_8 = 7,
    VSB_16 = 8,
    PSK_8 = 9,
    APSK_16 = 10,
    APSK_32 = 11,
    DQPSK = 12,
    QAM_4_NR = 13
} ;
typedef enum fe_modulation fe_modulation_t;
enum fe_transmit_mode {
    TRANSMISSION_MODE_2K = 0,
    TRANSMISSION_MODE_8K = 1,
    TRANSMISSION_MODE_AUTO = 2,
    TRANSMISSION_MODE_4K = 3,
    TRANSMISSION_MODE_1K = 4,
    TRANSMISSION_MODE_16K = 5,
    TRANSMISSION_MODE_32K = 6,
    TRANSMISSION_MODE_C1 = 7,
    TRANSMISSION_MODE_C3780 = 8
} ;
typedef enum fe_transmit_mode fe_transmit_mode_t;
enum fe_guard_interval {
    GUARD_INTERVAL_1_32 = 0,
    GUARD_INTERVAL_1_16 = 1,
    GUARD_INTERVAL_1_8 = 2,
    GUARD_INTERVAL_1_4 = 3,
    GUARD_INTERVAL_AUTO = 4,
    GUARD_INTERVAL_1_128 = 5,
    GUARD_INTERVAL_19_128 = 6,
    GUARD_INTERVAL_19_256 = 7,
    GUARD_INTERVAL_PN420 = 8,
    GUARD_INTERVAL_PN595 = 9,
    GUARD_INTERVAL_PN945 = 10
} ;
typedef enum fe_guard_interval fe_guard_interval_t;
enum fe_hierarchy {
    HIERARCHY_NONE = 0,
    HIERARCHY_1 = 1,
    HIERARCHY_2 = 2,
    HIERARCHY_4 = 3,
    HIERARCHY_AUTO = 4
} ;
typedef enum fe_hierarchy fe_hierarchy_t;
enum fe_interleaving {
    INTERLEAVING_NONE = 0,
    INTERLEAVING_AUTO = 1,
    INTERLEAVING_240 = 2,
    INTERLEAVING_720 = 3
} ;
enum fe_pilot {
    PILOT_ON = 0,
    PILOT_OFF = 1,
    PILOT_AUTO = 2
} ;
typedef enum fe_pilot fe_pilot_t;
enum fe_rolloff {
    ROLLOFF_35 = 0,
    ROLLOFF_20 = 1,
    ROLLOFF_25 = 2,
    ROLLOFF_AUTO = 3
} ;
typedef enum fe_rolloff fe_rolloff_t;
enum fe_delivery_system {
    SYS_UNDEFINED = 0,
    SYS_DVBC_ANNEX_A = 1,
    SYS_DVBC_ANNEX_B = 2,
    SYS_DVBT = 3,
    SYS_DSS = 4,
    SYS_DVBS = 5,
    SYS_DVBS2 = 6,
    SYS_DVBH = 7,
    SYS_ISDBT = 8,
    SYS_ISDBS = 9,
    SYS_ISDBC = 10,
    SYS_ATSC = 11,
    SYS_ATSCMH = 12,
    SYS_DTMB = 13,
    SYS_CMMB = 14,
    SYS_DAB = 15,
    SYS_DVBT2 = 16,
    SYS_TURBO = 17,
    SYS_DVBC_ANNEX_C = 18
} ;
typedef enum fe_delivery_system fe_delivery_system_t;
struct __anonstruct_buffer_136 {
   __u8 data[32U] ;
   __u32 len ;
   __u32 reserved1[3U] ;
   void *reserved2 ;
};
union __anonunion_u_135 {
   __u32 data ;
   struct __anonstruct_buffer_136 buffer ;
};
struct dtv_property {
   __u32 cmd ;
   __u32 reserved[3U] ;
   union __anonunion_u_135 u ;
   int result ;
};
struct kernel_cap_struct {
   __u32 cap[2U] ;
};
typedef struct kernel_cap_struct kernel_cap_t;
struct inode;
struct dentry;
struct arch_uprobe_task {
   unsigned long saved_scratch_register ;
   unsigned int saved_trap_nr ;
   unsigned int saved_tf ;
};
enum uprobe_task_state {
    UTASK_RUNNING = 0,
    UTASK_SSTEP = 1,
    UTASK_SSTEP_ACK = 2,
    UTASK_SSTEP_TRAPPED = 3
} ;
struct uprobe;
struct uprobe_task {
   enum uprobe_task_state state ;
   struct arch_uprobe_task autask ;
   struct uprobe *active_uprobe ;
   unsigned long xol_vaddr ;
   unsigned long vaddr ;
};
struct xol_area {
   wait_queue_head_t wq ;
   atomic_t slot_count ;
   unsigned long *bitmap ;
   struct page *page ;
   unsigned long vaddr ;
};
struct uprobes_state {
   struct xol_area *xol_area ;
};
struct address_space;
union __anonunion_ldv_14925_139 {
   unsigned long index ;
   void *freelist ;
   bool pfmemalloc ;
};
struct __anonstruct_ldv_14935_143 {
   unsigned short inuse ;
   unsigned short objects : 15 ;
   unsigned char frozen : 1 ;
};
union __anonunion_ldv_14937_142 {
   atomic_t _mapcount ;
   struct __anonstruct_ldv_14935_143 ldv_14935 ;
   int units ;
};
struct __anonstruct_ldv_14939_141 {
   union __anonunion_ldv_14937_142 ldv_14937 ;
   atomic_t _count ;
};
union __anonunion_ldv_14940_140 {
   unsigned long counters ;
   struct __anonstruct_ldv_14939_141 ldv_14939 ;
};
struct __anonstruct_ldv_14941_138 {
   union __anonunion_ldv_14925_139 ldv_14925 ;
   union __anonunion_ldv_14940_140 ldv_14940 ;
};
struct __anonstruct_ldv_14948_145 {
   struct page *next ;
   int pages ;
   int pobjects ;
};
struct slab;
struct __anonstruct_ldv_14954_146 {
   struct kmem_cache *slab_cache ;
   struct slab *slab_page ;
};
union __anonunion_ldv_14955_144 {
   struct list_head lru ;
   struct __anonstruct_ldv_14948_145 ldv_14948 ;
   struct list_head list ;
   struct __anonstruct_ldv_14954_146 ldv_14954 ;
};
union __anonunion_ldv_14960_147 {
   unsigned long private ;
   struct kmem_cache *slab ;
   struct page *first_page ;
};
struct page {
   unsigned long flags ;
   struct address_space *mapping ;
   struct __anonstruct_ldv_14941_138 ldv_14941 ;
   union __anonunion_ldv_14955_144 ldv_14955 ;
   union __anonunion_ldv_14960_147 ldv_14960 ;
   unsigned long debug_flags ;
};
struct page_frag {
   struct page *page ;
   __u32 offset ;
   __u32 size ;
};
struct __anonstruct_linear_149 {
   struct rb_node rb ;
   unsigned long rb_subtree_last ;
};
union __anonunion_shared_148 {
   struct __anonstruct_linear_149 linear ;
   struct list_head nonlinear ;
};
struct anon_vma;
struct vm_operations_struct;
struct mempolicy;
struct vm_area_struct {
   struct mm_struct *vm_mm ;
   unsigned long vm_start ;
   unsigned long vm_end ;
   struct vm_area_struct *vm_next ;
   struct vm_area_struct *vm_prev ;
   pgprot_t vm_page_prot ;
   unsigned long vm_flags ;
   struct rb_node vm_rb ;
   union __anonunion_shared_148 shared ;
   struct list_head anon_vma_chain ;
   struct anon_vma *anon_vma ;
   struct vm_operations_struct const *vm_ops ;
   unsigned long vm_pgoff ;
   struct file *vm_file ;
   void *vm_private_data ;
   struct mempolicy *vm_policy ;
};
struct core_thread {
   struct task_struct *task ;
   struct core_thread *next ;
};
struct core_state {
   atomic_t nr_threads ;
   struct core_thread dumper ;
   struct completion startup ;
};
struct mm_rss_stat {
   atomic_long_t count[3U] ;
};
struct linux_binfmt;
struct mmu_notifier_mm;
struct mm_struct {
   struct vm_area_struct *mmap ;
   struct rb_root mm_rb ;
   struct vm_area_struct *mmap_cache ;
   unsigned long (*get_unmapped_area)(struct file * , unsigned long , unsigned long ,
                                      unsigned long , unsigned long ) ;
   void (*unmap_area)(struct mm_struct * , unsigned long ) ;
   unsigned long mmap_base ;
   unsigned long task_size ;
   unsigned long cached_hole_size ;
   unsigned long free_area_cache ;
   pgd_t *pgd ;
   atomic_t mm_users ;
   atomic_t mm_count ;
   int map_count ;
   spinlock_t page_table_lock ;
   struct rw_semaphore mmap_sem ;
   struct list_head mmlist ;
   unsigned long hiwater_rss ;
   unsigned long hiwater_vm ;
   unsigned long total_vm ;
   unsigned long locked_vm ;
   unsigned long pinned_vm ;
   unsigned long shared_vm ;
   unsigned long exec_vm ;
   unsigned long stack_vm ;
   unsigned long def_flags ;
   unsigned long nr_ptes ;
   unsigned long start_code ;
   unsigned long end_code ;
   unsigned long start_data ;
   unsigned long end_data ;
   unsigned long start_brk ;
   unsigned long brk ;
   unsigned long start_stack ;
   unsigned long arg_start ;
   unsigned long arg_end ;
   unsigned long env_start ;
   unsigned long env_end ;
   unsigned long saved_auxv[44U] ;
   struct mm_rss_stat rss_stat ;
   struct linux_binfmt *binfmt ;
   cpumask_var_t cpu_vm_mask_var ;
   mm_context_t context ;
   unsigned long flags ;
   struct core_state *core_state ;
   spinlock_t ioctx_lock ;
   struct hlist_head ioctx_list ;
   struct task_struct *owner ;
   struct file *exe_file ;
   struct mmu_notifier_mm *mmu_notifier_mm ;
   pgtable_t pmd_huge_pte ;
   struct cpumask cpumask_allocation ;
   struct uprobes_state uprobes_state ;
};
typedef unsigned long cputime_t;
struct sem_undo_list;
struct sysv_sem {
   struct sem_undo_list *undo_list ;
};
struct siginfo;
struct __anonstruct_sigset_t_150 {
   unsigned long sig[1U] ;
};
typedef struct __anonstruct_sigset_t_150 sigset_t;
typedef void __signalfn_t(int );
typedef __signalfn_t *__sighandler_t;
typedef void __restorefn_t(void);
typedef __restorefn_t *__sigrestore_t;
struct sigaction {
   __sighandler_t sa_handler ;
   unsigned long sa_flags ;
   __sigrestore_t sa_restorer ;
   sigset_t sa_mask ;
};
struct k_sigaction {
   struct sigaction sa ;
};
union sigval {
   int sival_int ;
   void *sival_ptr ;
};
typedef union sigval sigval_t;
struct __anonstruct__kill_152 {
   __kernel_pid_t _pid ;
   __kernel_uid32_t _uid ;
};
struct __anonstruct__timer_153 {
   __kernel_timer_t _tid ;
   int _overrun ;
   char _pad[0U] ;
   sigval_t _sigval ;
   int _sys_private ;
};
struct __anonstruct__rt_154 {
   __kernel_pid_t _pid ;
   __kernel_uid32_t _uid ;
   sigval_t _sigval ;
};
struct __anonstruct__sigchld_155 {
   __kernel_pid_t _pid ;
   __kernel_uid32_t _uid ;
   int _status ;
   __kernel_clock_t _utime ;
   __kernel_clock_t _stime ;
};
struct __anonstruct__sigfault_156 {
   void *_addr ;
   short _addr_lsb ;
};
struct __anonstruct__sigpoll_157 {
   long _band ;
   int _fd ;
};
struct __anonstruct__sigsys_158 {
   void *_call_addr ;
   int _syscall ;
   unsigned int _arch ;
};
union __anonunion__sifields_151 {
   int _pad[28U] ;
   struct __anonstruct__kill_152 _kill ;
   struct __anonstruct__timer_153 _timer ;
   struct __anonstruct__rt_154 _rt ;
   struct __anonstruct__sigchld_155 _sigchld ;
   struct __anonstruct__sigfault_156 _sigfault ;
   struct __anonstruct__sigpoll_157 _sigpoll ;
   struct __anonstruct__sigsys_158 _sigsys ;
};
struct siginfo {
   int si_signo ;
   int si_errno ;
   int si_code ;
   union __anonunion__sifields_151 _sifields ;
};
typedef struct siginfo siginfo_t;
struct user_struct;
struct sigpending {
   struct list_head list ;
   sigset_t signal ;
};
enum pid_type {
    PIDTYPE_PID = 0,
    PIDTYPE_PGID = 1,
    PIDTYPE_SID = 2,
    PIDTYPE_MAX = 3
} ;
struct pid_namespace;
struct upid {
   int nr ;
   struct pid_namespace *ns ;
   struct hlist_node pid_chain ;
};
struct pid {
   atomic_t count ;
   unsigned int level ;
   struct hlist_head tasks[3U] ;
   struct callback_head rcu ;
   struct upid numbers[1U] ;
};
struct pid_link {
   struct hlist_node node ;
   struct pid *pid ;
};
struct percpu_counter {
   raw_spinlock_t lock ;
   s64 count ;
   struct list_head list ;
   s32 *counters ;
};
struct seccomp_filter;
struct seccomp {
   int mode ;
   struct seccomp_filter *filter ;
};
struct plist_head {
   struct list_head node_list ;
};
struct plist_node {
   int prio ;
   struct list_head prio_list ;
   struct list_head node_list ;
};
struct rt_mutex {
   raw_spinlock_t wait_lock ;
   struct plist_head wait_list ;
   struct task_struct *owner ;
   int save_state ;
   char const *name ;
   char const *file ;
   int line ;
   void *magic ;
};
struct rt_mutex_waiter;
struct rlimit {
   unsigned long rlim_cur ;
   unsigned long rlim_max ;
};
struct timerqueue_node {
   struct rb_node node ;
   ktime_t expires ;
};
struct timerqueue_head {
   struct rb_root head ;
   struct timerqueue_node *next ;
};
struct hrtimer_clock_base;
struct hrtimer_cpu_base;
enum hrtimer_restart {
    HRTIMER_NORESTART = 0,
    HRTIMER_RESTART = 1
} ;
struct hrtimer {
   struct timerqueue_node node ;
   ktime_t _softexpires ;
   enum hrtimer_restart (*function)(struct hrtimer * ) ;
   struct hrtimer_clock_base *base ;
   unsigned long state ;
   int start_pid ;
   void *start_site ;
   char start_comm[16U] ;
};
struct hrtimer_clock_base {
   struct hrtimer_cpu_base *cpu_base ;
   int index ;
   clockid_t clockid ;
   struct timerqueue_head active ;
   ktime_t resolution ;
   ktime_t (*get_time)(void) ;
   ktime_t softirq_time ;
   ktime_t offset ;
};
struct hrtimer_cpu_base {
   raw_spinlock_t lock ;
   unsigned int active_bases ;
   unsigned int clock_was_set ;
   ktime_t expires_next ;
   int hres_active ;
   int hang_detected ;
   unsigned long nr_events ;
   unsigned long nr_retries ;
   unsigned long nr_hangs ;
   ktime_t max_hang_time ;
   struct hrtimer_clock_base clock_base[3U] ;
};
struct task_io_accounting {
   u64 rchar ;
   u64 wchar ;
   u64 syscr ;
   u64 syscw ;
   u64 read_bytes ;
   u64 write_bytes ;
   u64 cancelled_write_bytes ;
};
struct latency_record {
   unsigned long backtrace[12U] ;
   unsigned int count ;
   unsigned long time ;
   unsigned long max ;
};
typedef int32_t key_serial_t;
typedef uint32_t key_perm_t;
struct key;
struct signal_struct;
struct key_type;
struct keyring_list;
union __anonunion_ldv_16212_161 {
   struct list_head graveyard_link ;
   struct rb_node serial_node ;
};
struct key_user;
union __anonunion_ldv_16221_162 {
   time_t expiry ;
   time_t revoked_at ;
};
union __anonunion_type_data_163 {
   struct list_head link ;
   unsigned long x[2U] ;
   void *p[2U] ;
   int reject_error ;
};
union __anonunion_payload_164 {
   unsigned long value ;
   void *rcudata ;
   void *data ;
   struct keyring_list *subscriptions ;
};
struct key {
   atomic_t usage ;
   key_serial_t serial ;
   union __anonunion_ldv_16212_161 ldv_16212 ;
   struct key_type *type ;
   struct rw_semaphore sem ;
   struct key_user *user ;
   void *security ;
   union __anonunion_ldv_16221_162 ldv_16221 ;
   time_t last_used_at ;
   kuid_t uid ;
   kgid_t gid ;
   key_perm_t perm ;
   unsigned short quotalen ;
   unsigned short datalen ;
   unsigned long flags ;
   char *description ;
   union __anonunion_type_data_163 type_data ;
   union __anonunion_payload_164 payload ;
};
struct audit_context;
struct group_info {
   atomic_t usage ;
   int ngroups ;
   int nblocks ;
   kgid_t small_block[32U] ;
   kgid_t *blocks[0U] ;
};
struct thread_group_cred {
   atomic_t usage ;
   pid_t tgid ;
   spinlock_t lock ;
   struct key *session_keyring ;
   struct key *process_keyring ;
   struct callback_head rcu ;
};
struct cred {
   atomic_t usage ;
   atomic_t subscribers ;
   void *put_addr ;
   unsigned int magic ;
   kuid_t uid ;
   kgid_t gid ;
   kuid_t suid ;
   kgid_t sgid ;
   kuid_t euid ;
   kgid_t egid ;
   kuid_t fsuid ;
   kgid_t fsgid ;
   unsigned int securebits ;
   kernel_cap_t cap_inheritable ;
   kernel_cap_t cap_permitted ;
   kernel_cap_t cap_effective ;
   kernel_cap_t cap_bset ;
   unsigned char jit_keyring ;
   struct key *thread_keyring ;
   struct key *request_key_auth ;
   struct thread_group_cred *tgcred ;
   void *security ;
   struct user_struct *user ;
   struct user_namespace *user_ns ;
   struct group_info *group_info ;
   struct callback_head rcu ;
};
struct llist_node;
struct llist_node {
   struct llist_node *next ;
};
struct futex_pi_state;
struct robust_list_head;
struct bio_list;
struct fs_struct;
struct perf_event_context;
struct blk_plug;
struct cfs_rq;
struct task_group;
struct io_event {
   __u64 data ;
   __u64 obj ;
   __s64 res ;
   __s64 res2 ;
};
struct iovec {
   void *iov_base ;
   __kernel_size_t iov_len ;
};
struct kioctx;
union __anonunion_ki_obj_165 {
   void *user ;
   struct task_struct *tsk ;
};
struct eventfd_ctx;
struct kiocb {
   struct list_head ki_run_list ;
   unsigned long ki_flags ;
   int ki_users ;
   unsigned int ki_key ;
   struct file *ki_filp ;
   struct kioctx *ki_ctx ;
   int (*ki_cancel)(struct kiocb * , struct io_event * ) ;
   ssize_t (*ki_retry)(struct kiocb * ) ;
   void (*ki_dtor)(struct kiocb * ) ;
   union __anonunion_ki_obj_165 ki_obj ;
   __u64 ki_user_data ;
   loff_t ki_pos ;
   void *private ;
   unsigned short ki_opcode ;
   size_t ki_nbytes ;
   char *ki_buf ;
   size_t ki_left ;
   struct iovec ki_inline_vec ;
   struct iovec *ki_iovec ;
   unsigned long ki_nr_segs ;
   unsigned long ki_cur_seg ;
   struct list_head ki_list ;
   struct list_head ki_batch ;
   struct eventfd_ctx *ki_eventfd ;
};
struct aio_ring_info {
   unsigned long mmap_base ;
   unsigned long mmap_size ;
   struct page **ring_pages ;
   spinlock_t ring_lock ;
   long nr_pages ;
   unsigned int nr ;
   unsigned int tail ;
   struct page *internal_pages[8U] ;
};
struct kioctx {
   atomic_t users ;
   int dead ;
   struct mm_struct *mm ;
   unsigned long user_id ;
   struct hlist_node list ;
   wait_queue_head_t wait ;
   spinlock_t ctx_lock ;
   int reqs_active ;
   struct list_head active_reqs ;
   struct list_head run_list ;
   unsigned int max_reqs ;
   struct aio_ring_info ring_info ;
   struct delayed_work wq ;
   struct callback_head callback_head ;
};
struct sighand_struct {
   atomic_t count ;
   struct k_sigaction action[64U] ;
   spinlock_t siglock ;
   wait_queue_head_t signalfd_wqh ;
};
struct pacct_struct {
   int ac_flag ;
   long ac_exitcode ;
   unsigned long ac_mem ;
   cputime_t ac_utime ;
   cputime_t ac_stime ;
   unsigned long ac_minflt ;
   unsigned long ac_majflt ;
};
struct cpu_itimer {
   cputime_t expires ;
   cputime_t incr ;
   u32 error ;
   u32 incr_error ;
};
struct task_cputime {
   cputime_t utime ;
   cputime_t stime ;
   unsigned long long sum_exec_runtime ;
};
struct thread_group_cputimer {
   struct task_cputime cputime ;
   int running ;
   raw_spinlock_t lock ;
};
struct autogroup;
struct tty_struct;
struct taskstats;
struct tty_audit_buf;
struct signal_struct {
   atomic_t sigcnt ;
   atomic_t live ;
   int nr_threads ;
   wait_queue_head_t wait_chldexit ;
   struct task_struct *curr_target ;
   struct sigpending shared_pending ;
   int group_exit_code ;
   int notify_count ;
   struct task_struct *group_exit_task ;
   int group_stop_count ;
   unsigned int flags ;
   unsigned char is_child_subreaper : 1 ;
   unsigned char has_child_subreaper : 1 ;
   struct list_head posix_timers ;
   struct hrtimer real_timer ;
   struct pid *leader_pid ;
   ktime_t it_real_incr ;
   struct cpu_itimer it[2U] ;
   struct thread_group_cputimer cputimer ;
   struct task_cputime cputime_expires ;
   struct list_head cpu_timers[3U] ;
   struct pid *tty_old_pgrp ;
   int leader ;
   struct tty_struct *tty ;
   struct autogroup *autogroup ;
   cputime_t utime ;
   cputime_t stime ;
   cputime_t cutime ;
   cputime_t cstime ;
   cputime_t gtime ;
   cputime_t cgtime ;
   cputime_t prev_utime ;
   cputime_t prev_stime ;
   unsigned long nvcsw ;
   unsigned long nivcsw ;
   unsigned long cnvcsw ;
   unsigned long cnivcsw ;
   unsigned long min_flt ;
   unsigned long maj_flt ;
   unsigned long cmin_flt ;
   unsigned long cmaj_flt ;
   unsigned long inblock ;
   unsigned long oublock ;
   unsigned long cinblock ;
   unsigned long coublock ;
   unsigned long maxrss ;
   unsigned long cmaxrss ;
   struct task_io_accounting ioac ;
   unsigned long long sum_sched_runtime ;
   struct rlimit rlim[16U] ;
   struct pacct_struct pacct ;
   struct taskstats *stats ;
   unsigned int audit_tty ;
   struct tty_audit_buf *tty_audit_buf ;
   struct rw_semaphore group_rwsem ;
   int oom_score_adj ;
   int oom_score_adj_min ;
   struct mutex cred_guard_mutex ;
};
struct user_struct {
   atomic_t __count ;
   atomic_t processes ;
   atomic_t files ;
   atomic_t sigpending ;
   atomic_t inotify_watches ;
   atomic_t inotify_devs ;
   atomic_t fanotify_listeners ;
   atomic_long_t epoll_watches ;
   unsigned long mq_bytes ;
   unsigned long locked_shm ;
   struct key *uid_keyring ;
   struct key *session_keyring ;
   struct hlist_node uidhash_node ;
   kuid_t uid ;
   atomic_long_t locked_vm ;
};
struct backing_dev_info;
struct reclaim_state;
struct sched_info {
   unsigned long pcount ;
   unsigned long long run_delay ;
   unsigned long long last_arrival ;
   unsigned long long last_queued ;
};
struct task_delay_info {
   spinlock_t lock ;
   unsigned int flags ;
   struct timespec blkio_start ;
   struct timespec blkio_end ;
   u64 blkio_delay ;
   u64 swapin_delay ;
   u32 blkio_count ;
   u32 swapin_count ;
   struct timespec freepages_start ;
   struct timespec freepages_end ;
   u64 freepages_delay ;
   u32 freepages_count ;
};
struct io_context;
struct pipe_inode_info;
struct rq;
struct sched_class {
   struct sched_class const *next ;
   void (*enqueue_task)(struct rq * , struct task_struct * , int ) ;
   void (*dequeue_task)(struct rq * , struct task_struct * , int ) ;
   void (*yield_task)(struct rq * ) ;
   bool (*yield_to_task)(struct rq * , struct task_struct * , bool ) ;
   void (*check_preempt_curr)(struct rq * , struct task_struct * , int ) ;
   struct task_struct *(*pick_next_task)(struct rq * ) ;
   void (*put_prev_task)(struct rq * , struct task_struct * ) ;
   int (*select_task_rq)(struct task_struct * , int , int ) ;
   void (*pre_schedule)(struct rq * , struct task_struct * ) ;
   void (*post_schedule)(struct rq * ) ;
   void (*task_waking)(struct task_struct * ) ;
   void (*task_woken)(struct rq * , struct task_struct * ) ;
   void (*set_cpus_allowed)(struct task_struct * , struct cpumask const * ) ;
   void (*rq_online)(struct rq * ) ;
   void (*rq_offline)(struct rq * ) ;
   void (*set_curr_task)(struct rq * ) ;
   void (*task_tick)(struct rq * , struct task_struct * , int ) ;
   void (*task_fork)(struct task_struct * ) ;
   void (*switched_from)(struct rq * , struct task_struct * ) ;
   void (*switched_to)(struct rq * , struct task_struct * ) ;
   void (*prio_changed)(struct rq * , struct task_struct * , int ) ;
   unsigned int (*get_rr_interval)(struct rq * , struct task_struct * ) ;
   void (*task_move_group)(struct task_struct * , int ) ;
};
struct load_weight {
   unsigned long weight ;
   unsigned long inv_weight ;
};
struct sched_statistics {
   u64 wait_start ;
   u64 wait_max ;
   u64 wait_count ;
   u64 wait_sum ;
   u64 iowait_count ;
   u64 iowait_sum ;
   u64 sleep_start ;
   u64 sleep_max ;
   s64 sum_sleep_runtime ;
   u64 block_start ;
   u64 block_max ;
   u64 exec_max ;
   u64 slice_max ;
   u64 nr_migrations_cold ;
   u64 nr_failed_migrations_affine ;
   u64 nr_failed_migrations_running ;
   u64 nr_failed_migrations_hot ;
   u64 nr_forced_migrations ;
   u64 nr_wakeups ;
   u64 nr_wakeups_sync ;
   u64 nr_wakeups_migrate ;
   u64 nr_wakeups_local ;
   u64 nr_wakeups_remote ;
   u64 nr_wakeups_affine ;
   u64 nr_wakeups_affine_attempts ;
   u64 nr_wakeups_passive ;
   u64 nr_wakeups_idle ;
};
struct sched_entity {
   struct load_weight load ;
   struct rb_node run_node ;
   struct list_head group_node ;
   unsigned int on_rq ;
   u64 exec_start ;
   u64 sum_exec_runtime ;
   u64 vruntime ;
   u64 prev_sum_exec_runtime ;
   u64 nr_migrations ;
   struct sched_statistics statistics ;
   struct sched_entity *parent ;
   struct cfs_rq *cfs_rq ;
   struct cfs_rq *my_q ;
};
struct rt_rq;
struct sched_rt_entity {
   struct list_head run_list ;
   unsigned long timeout ;
   unsigned int time_slice ;
   struct sched_rt_entity *back ;
   struct sched_rt_entity *parent ;
   struct rt_rq *rt_rq ;
   struct rt_rq *my_q ;
};
struct mem_cgroup;
struct memcg_batch_info {
   int do_batch ;
   struct mem_cgroup *memcg ;
   unsigned long nr_pages ;
   unsigned long memsw_nr_pages ;
};
struct files_struct;
struct css_set;
struct compat_robust_list_head;
struct task_struct {
   long volatile state ;
   void *stack ;
   atomic_t usage ;
   unsigned int flags ;
   unsigned int ptrace ;
   struct llist_node wake_entry ;
   int on_cpu ;
   int on_rq ;
   int prio ;
   int static_prio ;
   int normal_prio ;
   unsigned int rt_priority ;
   struct sched_class const *sched_class ;
   struct sched_entity se ;
   struct sched_rt_entity rt ;
   struct task_group *sched_task_group ;
   struct hlist_head preempt_notifiers ;
   unsigned char fpu_counter ;
   unsigned int policy ;
   int nr_cpus_allowed ;
   cpumask_t cpus_allowed ;
   struct sched_info sched_info ;
   struct list_head tasks ;
   struct plist_node pushable_tasks ;
   struct mm_struct *mm ;
   struct mm_struct *active_mm ;
   unsigned char brk_randomized : 1 ;
   int exit_state ;
   int exit_code ;
   int exit_signal ;
   int pdeath_signal ;
   unsigned int jobctl ;
   unsigned int personality ;
   unsigned char did_exec : 1 ;
   unsigned char in_execve : 1 ;
   unsigned char in_iowait : 1 ;
   unsigned char no_new_privs : 1 ;
   unsigned char sched_reset_on_fork : 1 ;
   unsigned char sched_contributes_to_load : 1 ;
   pid_t pid ;
   pid_t tgid ;
   unsigned long stack_canary ;
   struct task_struct *real_parent ;
   struct task_struct *parent ;
   struct list_head children ;
   struct list_head sibling ;
   struct task_struct *group_leader ;
   struct list_head ptraced ;
   struct list_head ptrace_entry ;
   struct pid_link pids[3U] ;
   struct list_head thread_group ;
   struct completion *vfork_done ;
   int *set_child_tid ;
   int *clear_child_tid ;
   cputime_t utime ;
   cputime_t stime ;
   cputime_t utimescaled ;
   cputime_t stimescaled ;
   cputime_t gtime ;
   cputime_t prev_utime ;
   cputime_t prev_stime ;
   unsigned long nvcsw ;
   unsigned long nivcsw ;
   struct timespec start_time ;
   struct timespec real_start_time ;
   unsigned long min_flt ;
   unsigned long maj_flt ;
   struct task_cputime cputime_expires ;
   struct list_head cpu_timers[3U] ;
   struct cred const *real_cred ;
   struct cred const *cred ;
   char comm[16U] ;
   int link_count ;
   int total_link_count ;
   struct sysv_sem sysvsem ;
   unsigned long last_switch_count ;
   struct thread_struct thread ;
   struct fs_struct *fs ;
   struct files_struct *files ;
   struct nsproxy *nsproxy ;
   struct signal_struct *signal ;
   struct sighand_struct *sighand ;
   sigset_t blocked ;
   sigset_t real_blocked ;
   sigset_t saved_sigmask ;
   struct sigpending pending ;
   unsigned long sas_ss_sp ;
   size_t sas_ss_size ;
   int (*notifier)(void * ) ;
   void *notifier_data ;
   sigset_t *notifier_mask ;
   struct callback_head *task_works ;
   struct audit_context *audit_context ;
   kuid_t loginuid ;
   unsigned int sessionid ;
   struct seccomp seccomp ;
   u32 parent_exec_id ;
   u32 self_exec_id ;
   spinlock_t alloc_lock ;
   raw_spinlock_t pi_lock ;
   struct plist_head pi_waiters ;
   struct rt_mutex_waiter *pi_blocked_on ;
   struct mutex_waiter *blocked_on ;
   unsigned int irq_events ;
   unsigned long hardirq_enable_ip ;
   unsigned long hardirq_disable_ip ;
   unsigned int hardirq_enable_event ;
   unsigned int hardirq_disable_event ;
   int hardirqs_enabled ;
   int hardirq_context ;
   unsigned long softirq_disable_ip ;
   unsigned long softirq_enable_ip ;
   unsigned int softirq_disable_event ;
   unsigned int softirq_enable_event ;
   int softirqs_enabled ;
   int softirq_context ;
   u64 curr_chain_key ;
   int lockdep_depth ;
   unsigned int lockdep_recursion ;
   struct held_lock held_locks[48U] ;
   gfp_t lockdep_reclaim_gfp ;
   void *journal_info ;
   struct bio_list *bio_list ;
   struct blk_plug *plug ;
   struct reclaim_state *reclaim_state ;
   struct backing_dev_info *backing_dev_info ;
   struct io_context *io_context ;
   unsigned long ptrace_message ;
   siginfo_t *last_siginfo ;
   struct task_io_accounting ioac ;
   u64 acct_rss_mem1 ;
   u64 acct_vm_mem1 ;
   cputime_t acct_timexpd ;
   nodemask_t mems_allowed ;
   seqcount_t mems_allowed_seq ;
   int cpuset_mem_spread_rotor ;
   int cpuset_slab_spread_rotor ;
   struct css_set *cgroups ;
   struct list_head cg_list ;
   struct robust_list_head *robust_list ;
   struct compat_robust_list_head *compat_robust_list ;
   struct list_head pi_state_list ;
   struct futex_pi_state *pi_state_cache ;
   struct perf_event_context *perf_event_ctxp[2U] ;
   struct mutex perf_event_mutex ;
   struct list_head perf_event_list ;
   struct mempolicy *mempolicy ;
   short il_next ;
   short pref_node_fork ;
   struct callback_head rcu ;
   struct pipe_inode_info *splice_pipe ;
   struct page_frag task_frag ;
   struct task_delay_info *delays ;
   int make_it_fail ;
   int nr_dirtied ;
   int nr_dirtied_pause ;
   unsigned long dirty_paused_when ;
   int latency_record_count ;
   struct latency_record latency_record[32U] ;
   unsigned long timer_slack_ns ;
   unsigned long default_timer_slack_ns ;
   unsigned long trace ;
   unsigned long trace_recursion ;
   struct memcg_batch_info memcg_batch ;
   atomic_t ptrace_bp_refcnt ;
   struct uprobe_task *utask ;
};
struct of_device_id {
   char name[32U] ;
   char type[32U] ;
   char compatible[128U] ;
   void const *data ;
};
struct klist_node;
struct klist_node {
   void *n_klist ;
   struct list_head n_node ;
   struct kref n_ref ;
};
struct dma_map_ops;
struct dev_archdata {
   void *acpi_handle ;
   struct dma_map_ops *dma_ops ;
   void *iommu ;
};
struct device_private;
struct device_driver;
struct driver_private;
struct class;
struct subsys_private;
struct bus_type;
struct device_node;
struct iommu_ops;
struct iommu_group;
struct bus_attribute {
   struct attribute attr ;
   ssize_t (*show)(struct bus_type * , char * ) ;
   ssize_t (*store)(struct bus_type * , char const * , size_t ) ;
};
struct device_attribute;
struct driver_attribute;
struct bus_type {
   char const *name ;
   char const *dev_name ;
   struct device *dev_root ;
   struct bus_attribute *bus_attrs ;
   struct device_attribute *dev_attrs ;
   struct driver_attribute *drv_attrs ;
   int (*match)(struct device * , struct device_driver * ) ;
   int (*uevent)(struct device * , struct kobj_uevent_env * ) ;
   int (*probe)(struct device * ) ;
   int (*remove)(struct device * ) ;
   void (*shutdown)(struct device * ) ;
   int (*suspend)(struct device * , pm_message_t ) ;
   int (*resume)(struct device * ) ;
   struct dev_pm_ops const *pm ;
   struct iommu_ops *iommu_ops ;
   struct subsys_private *p ;
};
struct device_type;
struct device_driver {
   char const *name ;
   struct bus_type *bus ;
   struct module *owner ;
   char const *mod_name ;
   bool suppress_bind_attrs ;
   struct of_device_id const *of_match_table ;
   int (*probe)(struct device * ) ;
   int (*remove)(struct device * ) ;
   void (*shutdown)(struct device * ) ;
   int (*suspend)(struct device * , pm_message_t ) ;
   int (*resume)(struct device * ) ;
   struct attribute_group const **groups ;
   struct dev_pm_ops const *pm ;
   struct driver_private *p ;
};
struct driver_attribute {
   struct attribute attr ;
   ssize_t (*show)(struct device_driver * , char * ) ;
   ssize_t (*store)(struct device_driver * , char const * , size_t ) ;
};
struct class_attribute;
struct class {
   char const *name ;
   struct module *owner ;
   struct class_attribute *class_attrs ;
   struct device_attribute *dev_attrs ;
   struct bin_attribute *dev_bin_attrs ;
   struct kobject *dev_kobj ;
   int (*dev_uevent)(struct device * , struct kobj_uevent_env * ) ;
   char *(*devnode)(struct device * , umode_t * ) ;
   void (*class_release)(struct class * ) ;
   void (*dev_release)(struct device * ) ;
   int (*suspend)(struct device * , pm_message_t ) ;
   int (*resume)(struct device * ) ;
   struct kobj_ns_type_operations const *ns_type ;
   void const *(*namespace)(struct device * ) ;
   struct dev_pm_ops const *pm ;
   struct subsys_private *p ;
};
struct class_attribute {
   struct attribute attr ;
   ssize_t (*show)(struct class * , struct class_attribute * , char * ) ;
   ssize_t (*store)(struct class * , struct class_attribute * , char const * , size_t ) ;
   void const *(*namespace)(struct class * , struct class_attribute const * ) ;
};
struct device_type {
   char const *name ;
   struct attribute_group const **groups ;
   int (*uevent)(struct device * , struct kobj_uevent_env * ) ;
   char *(*devnode)(struct device * , umode_t * ) ;
   void (*release)(struct device * ) ;
   struct dev_pm_ops const *pm ;
};
struct device_attribute {
   struct attribute attr ;
   ssize_t (*show)(struct device * , struct device_attribute * , char * ) ;
   ssize_t (*store)(struct device * , struct device_attribute * , char const * ,
                    size_t ) ;
};
struct device_dma_parameters {
   unsigned int max_segment_size ;
   unsigned long segment_boundary_mask ;
};
struct dma_coherent_mem;
struct device {
   struct device *parent ;
   struct device_private *p ;
   struct kobject kobj ;
   char const *init_name ;
   struct device_type const *type ;
   struct mutex mutex ;
   struct bus_type *bus ;
   struct device_driver *driver ;
   void *platform_data ;
   struct dev_pm_info power ;
   struct dev_pm_domain *pm_domain ;
   int numa_node ;
   u64 *dma_mask ;
   u64 coherent_dma_mask ;
   struct device_dma_parameters *dma_parms ;
   struct list_head dma_pools ;
   struct dma_coherent_mem *dma_mem ;
   struct dev_archdata archdata ;
   struct device_node *of_node ;
   dev_t devt ;
   u32 id ;
   spinlock_t devres_lock ;
   struct list_head devres_head ;
   struct klist_node knode_class ;
   struct class *class ;
   struct attribute_group const **groups ;
   void (*release)(struct device * ) ;
   struct iommu_group *iommu_group ;
};
struct wakeup_source {
   char const *name ;
   struct list_head entry ;
   spinlock_t lock ;
   struct timer_list timer ;
   unsigned long timer_expires ;
   ktime_t total_time ;
   ktime_t max_time ;
   ktime_t last_time ;
   ktime_t start_prevent_time ;
   ktime_t prevent_sleep_time ;
   unsigned long event_count ;
   unsigned long active_count ;
   unsigned long relax_count ;
   unsigned long expire_count ;
   unsigned long wakeup_count ;
   bool active ;
   bool autosleep_enabled ;
};
typedef u32 phandle;
struct property {
   char *name ;
   int length ;
   void *value ;
   struct property *next ;
   unsigned long _flags ;
   unsigned int unique_id ;
};
struct proc_dir_entry;
struct device_node {
   char const *name ;
   char const *type ;
   phandle phandle ;
   char *full_name ;
   struct property *properties ;
   struct property *deadprops ;
   struct device_node *parent ;
   struct device_node *child ;
   struct device_node *sibling ;
   struct device_node *next ;
   struct device_node *allnext ;
   struct proc_dir_entry *pde ;
   struct kref kref ;
   unsigned long _flags ;
   void *data ;
};
struct i2c_msg {
   __u16 addr ;
   __u16 flags ;
   __u16 len ;
   __u8 *buf ;
};
union i2c_smbus_data {
   __u8 byte ;
   __u16 word ;
   __u8 block[34U] ;
};
struct i2c_algorithm;
struct i2c_adapter;
struct i2c_algorithm {
   int (*master_xfer)(struct i2c_adapter * , struct i2c_msg * , int ) ;
   int (*smbus_xfer)(struct i2c_adapter * , u16 , unsigned short , char , u8 ,
                     int , union i2c_smbus_data * ) ;
   u32 (*functionality)(struct i2c_adapter * ) ;
};
struct i2c_adapter {
   struct module *owner ;
   unsigned int class ;
   struct i2c_algorithm const *algo ;
   void *algo_data ;
   struct rt_mutex bus_lock ;
   int timeout ;
   int retries ;
   struct device dev ;
   int nr ;
   char name[48U] ;
   struct completion dev_released ;
   struct mutex userspace_clients_lock ;
   struct list_head userspace_clients ;
};
struct hlist_bl_node;
struct hlist_bl_head {
   struct hlist_bl_node *first ;
};
struct hlist_bl_node {
   struct hlist_bl_node *next ;
   struct hlist_bl_node **pprev ;
};
struct nameidata;
struct path;
struct vfsmount;
struct __anonstruct_ldv_19666_169 {
   u32 hash ;
   u32 len ;
};
union __anonunion_ldv_19668_168 {
   struct __anonstruct_ldv_19666_169 ldv_19666 ;
   u64 hash_len ;
};
struct qstr {
   union __anonunion_ldv_19668_168 ldv_19668 ;
   unsigned char const *name ;
};
struct dentry_operations;
struct super_block;
union __anonunion_d_u_170 {
   struct list_head d_child ;
   struct callback_head d_rcu ;
};
struct dentry {
   unsigned int d_flags ;
   seqcount_t d_seq ;
   struct hlist_bl_node d_hash ;
   struct dentry *d_parent ;
   struct qstr d_name ;
   struct inode *d_inode ;
   unsigned char d_iname[32U] ;
   unsigned int d_count ;
   spinlock_t d_lock ;
   struct dentry_operations const *d_op ;
   struct super_block *d_sb ;
   unsigned long d_time ;
   void *d_fsdata ;
   struct list_head d_lru ;
   union __anonunion_d_u_170 d_u ;
   struct list_head d_subdirs ;
   struct hlist_node d_alias ;
};
struct dentry_operations {
   int (*d_revalidate)(struct dentry * , unsigned int ) ;
   int (*d_hash)(struct dentry const * , struct inode const * , struct qstr * ) ;
   int (*d_compare)(struct dentry const * , struct inode const * , struct dentry const * ,
                    struct inode const * , unsigned int , char const * , struct qstr const * ) ;
   int (*d_delete)(struct dentry const * ) ;
   void (*d_release)(struct dentry * ) ;
   void (*d_prune)(struct dentry * ) ;
   void (*d_iput)(struct dentry * , struct inode * ) ;
   char *(*d_dname)(struct dentry * , char * , int ) ;
   struct vfsmount *(*d_automount)(struct path * ) ;
   int (*d_manage)(struct dentry * , bool ) ;
};
struct path {
   struct vfsmount *mnt ;
   struct dentry *dentry ;
};
struct radix_tree_node;
struct radix_tree_root {
   unsigned int height ;
   gfp_t gfp_mask ;
   struct radix_tree_node *rnode ;
};
struct fiemap_extent {
   __u64 fe_logical ;
   __u64 fe_physical ;
   __u64 fe_length ;
   __u64 fe_reserved64[2U] ;
   __u32 fe_flags ;
   __u32 fe_reserved[3U] ;
};
struct shrink_control {
   gfp_t gfp_mask ;
   unsigned long nr_to_scan ;
};
struct shrinker {
   int (*shrink)(struct shrinker * , struct shrink_control * ) ;
   int seeks ;
   long batch ;
   struct list_head list ;
   atomic_long_t nr_in_batch ;
};
enum migrate_mode {
    MIGRATE_ASYNC = 0,
    MIGRATE_SYNC_LIGHT = 1,
    MIGRATE_SYNC = 2
} ;
struct block_device;
struct export_operations;
struct poll_table_struct;
struct kstatfs;
struct swap_info_struct;
struct iattr {
   unsigned int ia_valid ;
   umode_t ia_mode ;
   kuid_t ia_uid ;
   kgid_t ia_gid ;
   loff_t ia_size ;
   struct timespec ia_atime ;
   struct timespec ia_mtime ;
   struct timespec ia_ctime ;
   struct file *ia_file ;
};
struct fs_disk_quota {
   __s8 d_version ;
   __s8 d_flags ;
   __u16 d_fieldmask ;
   __u32 d_id ;
   __u64 d_blk_hardlimit ;
   __u64 d_blk_softlimit ;
   __u64 d_ino_hardlimit ;
   __u64 d_ino_softlimit ;
   __u64 d_bcount ;
   __u64 d_icount ;
   __s32 d_itimer ;
   __s32 d_btimer ;
   __u16 d_iwarns ;
   __u16 d_bwarns ;
   __s32 d_padding2 ;
   __u64 d_rtb_hardlimit ;
   __u64 d_rtb_softlimit ;
   __u64 d_rtbcount ;
   __s32 d_rtbtimer ;
   __u16 d_rtbwarns ;
   __s16 d_padding3 ;
   char d_padding4[8U] ;
};
struct fs_qfilestat {
   __u64 qfs_ino ;
   __u64 qfs_nblks ;
   __u32 qfs_nextents ;
};
typedef struct fs_qfilestat fs_qfilestat_t;
struct fs_quota_stat {
   __s8 qs_version ;
   __u16 qs_flags ;
   __s8 qs_pad ;
   fs_qfilestat_t qs_uquota ;
   fs_qfilestat_t qs_gquota ;
   __u32 qs_incoredqs ;
   __s32 qs_btimelimit ;
   __s32 qs_itimelimit ;
   __s32 qs_rtbtimelimit ;
   __u16 qs_bwarnlimit ;
   __u16 qs_iwarnlimit ;
};
struct dquot;
typedef __kernel_uid32_t projid_t;
typedef projid_t kprojid_t;
struct if_dqinfo {
   __u64 dqi_bgrace ;
   __u64 dqi_igrace ;
   __u32 dqi_flags ;
   __u32 dqi_valid ;
};
enum quota_type {
    USRQUOTA = 0,
    GRPQUOTA = 1,
    PRJQUOTA = 2
} ;
typedef long long qsize_t;
union __anonunion_ldv_20573_171 {
   kuid_t uid ;
   kgid_t gid ;
   kprojid_t projid ;
};
struct kqid {
   union __anonunion_ldv_20573_171 ldv_20573 ;
   enum quota_type type ;
};
struct mem_dqblk {
   qsize_t dqb_bhardlimit ;
   qsize_t dqb_bsoftlimit ;
   qsize_t dqb_curspace ;
   qsize_t dqb_rsvspace ;
   qsize_t dqb_ihardlimit ;
   qsize_t dqb_isoftlimit ;
   qsize_t dqb_curinodes ;
   time_t dqb_btime ;
   time_t dqb_itime ;
};
struct quota_format_type;
struct mem_dqinfo {
   struct quota_format_type *dqi_format ;
   int dqi_fmt_id ;
   struct list_head dqi_dirty_list ;
   unsigned long dqi_flags ;
   unsigned int dqi_bgrace ;
   unsigned int dqi_igrace ;
   qsize_t dqi_maxblimit ;
   qsize_t dqi_maxilimit ;
   void *dqi_priv ;
};
struct dquot {
   struct hlist_node dq_hash ;
   struct list_head dq_inuse ;
   struct list_head dq_free ;
   struct list_head dq_dirty ;
   struct mutex dq_lock ;
   atomic_t dq_count ;
   wait_queue_head_t dq_wait_unused ;
   struct super_block *dq_sb ;
   struct kqid dq_id ;
   loff_t dq_off ;
   unsigned long dq_flags ;
   struct mem_dqblk dq_dqb ;
};
struct quota_format_ops {
   int (*check_quota_file)(struct super_block * , int ) ;
   int (*read_file_info)(struct super_block * , int ) ;
   int (*write_file_info)(struct super_block * , int ) ;
   int (*free_file_info)(struct super_block * , int ) ;
   int (*read_dqblk)(struct dquot * ) ;
   int (*commit_dqblk)(struct dquot * ) ;
   int (*release_dqblk)(struct dquot * ) ;
};
struct dquot_operations {
   int (*write_dquot)(struct dquot * ) ;
   struct dquot *(*alloc_dquot)(struct super_block * , int ) ;
   void (*destroy_dquot)(struct dquot * ) ;
   int (*acquire_dquot)(struct dquot * ) ;
   int (*release_dquot)(struct dquot * ) ;
   int (*mark_dirty)(struct dquot * ) ;
   int (*write_info)(struct super_block * , int ) ;
   qsize_t *(*get_reserved_space)(struct inode * ) ;
};
struct quotactl_ops {
   int (*quota_on)(struct super_block * , int , int , struct path * ) ;
   int (*quota_on_meta)(struct super_block * , int , int ) ;
   int (*quota_off)(struct super_block * , int ) ;
   int (*quota_sync)(struct super_block * , int ) ;
   int (*get_info)(struct super_block * , int , struct if_dqinfo * ) ;
   int (*set_info)(struct super_block * , int , struct if_dqinfo * ) ;
   int (*get_dqblk)(struct super_block * , struct kqid , struct fs_disk_quota * ) ;
   int (*set_dqblk)(struct super_block * , struct kqid , struct fs_disk_quota * ) ;
   int (*get_xstate)(struct super_block * , struct fs_quota_stat * ) ;
   int (*set_xstate)(struct super_block * , unsigned int , int ) ;
};
struct quota_format_type {
   int qf_fmt_id ;
   struct quota_format_ops const *qf_ops ;
   struct module *qf_owner ;
   struct quota_format_type *qf_next ;
};
struct quota_info {
   unsigned int flags ;
   struct mutex dqio_mutex ;
   struct mutex dqonoff_mutex ;
   struct rw_semaphore dqptr_sem ;
   struct inode *files[2U] ;
   struct mem_dqinfo info[2U] ;
   struct quota_format_ops const *ops[2U] ;
};
struct writeback_control;
union __anonunion_arg_173 {
   char *buf ;
   void *data ;
};
struct __anonstruct_read_descriptor_t_172 {
   size_t written ;
   size_t count ;
   union __anonunion_arg_173 arg ;
   int error ;
};
typedef struct __anonstruct_read_descriptor_t_172 read_descriptor_t;
struct address_space_operations {
   int (*writepage)(struct page * , struct writeback_control * ) ;
   int (*readpage)(struct file * , struct page * ) ;
   int (*writepages)(struct address_space * , struct writeback_control * ) ;
   int (*set_page_dirty)(struct page * ) ;
   int (*readpages)(struct file * , struct address_space * , struct list_head * ,
                    unsigned int ) ;
   int (*write_begin)(struct file * , struct address_space * , loff_t , unsigned int ,
                      unsigned int , struct page ** , void ** ) ;
   int (*write_end)(struct file * , struct address_space * , loff_t , unsigned int ,
                    unsigned int , struct page * , void * ) ;
   sector_t (*bmap)(struct address_space * , sector_t ) ;
   void (*invalidatepage)(struct page * , unsigned long ) ;
   int (*releasepage)(struct page * , gfp_t ) ;
   void (*freepage)(struct page * ) ;
   ssize_t (*direct_IO)(int , struct kiocb * , struct iovec const * , loff_t ,
                        unsigned long ) ;
   int (*get_xip_mem)(struct address_space * , unsigned long , int , void ** , unsigned long * ) ;
   int (*migratepage)(struct address_space * , struct page * , struct page * , enum migrate_mode ) ;
   int (*launder_page)(struct page * ) ;
   int (*is_partially_uptodate)(struct page * , read_descriptor_t * , unsigned long ) ;
   int (*error_remove_page)(struct address_space * , struct page * ) ;
   int (*swap_activate)(struct swap_info_struct * , struct file * , sector_t * ) ;
   void (*swap_deactivate)(struct file * ) ;
};
struct address_space {
   struct inode *host ;
   struct radix_tree_root page_tree ;
   spinlock_t tree_lock ;
   unsigned int i_mmap_writable ;
   struct rb_root i_mmap ;
   struct list_head i_mmap_nonlinear ;
   struct mutex i_mmap_mutex ;
   unsigned long nrpages ;
   unsigned long writeback_index ;
   struct address_space_operations const *a_ops ;
   unsigned long flags ;
   struct backing_dev_info *backing_dev_info ;
   spinlock_t private_lock ;
   struct list_head private_list ;
   struct address_space *assoc_mapping ;
};
struct request_queue;
struct hd_struct;
struct gendisk;
struct block_device {
   dev_t bd_dev ;
   int bd_openers ;
   struct inode *bd_inode ;
   struct super_block *bd_super ;
   struct mutex bd_mutex ;
   struct list_head bd_inodes ;
   void *bd_claiming ;
   void *bd_holder ;
   int bd_holders ;
   bool bd_write_holder ;
   struct list_head bd_holder_disks ;
   struct block_device *bd_contains ;
   unsigned int bd_block_size ;
   struct hd_struct *bd_part ;
   unsigned int bd_part_count ;
   int bd_invalidated ;
   struct gendisk *bd_disk ;
   struct request_queue *bd_queue ;
   struct list_head bd_list ;
   unsigned long bd_private ;
   int bd_fsfreeze_count ;
   struct mutex bd_fsfreeze_mutex ;
};
struct posix_acl;
struct inode_operations;
union __anonunion_ldv_21007_174 {
   unsigned int const i_nlink ;
   unsigned int __i_nlink ;
};
union __anonunion_ldv_21027_175 {
   struct hlist_head i_dentry ;
   struct callback_head i_rcu ;
};
struct file_lock;
struct cdev;
union __anonunion_ldv_21043_176 {
   struct pipe_inode_info *i_pipe ;
   struct block_device *i_bdev ;
   struct cdev *i_cdev ;
};
struct inode {
   umode_t i_mode ;
   unsigned short i_opflags ;
   kuid_t i_uid ;
   kgid_t i_gid ;
   unsigned int i_flags ;
   struct posix_acl *i_acl ;
   struct posix_acl *i_default_acl ;
   struct inode_operations const *i_op ;
   struct super_block *i_sb ;
   struct address_space *i_mapping ;
   void *i_security ;
   unsigned long i_ino ;
   union __anonunion_ldv_21007_174 ldv_21007 ;
   dev_t i_rdev ;
   loff_t i_size ;
   struct timespec i_atime ;
   struct timespec i_mtime ;
   struct timespec i_ctime ;
   spinlock_t i_lock ;
   unsigned short i_bytes ;
   unsigned int i_blkbits ;
   blkcnt_t i_blocks ;
   unsigned long i_state ;
   struct mutex i_mutex ;
   unsigned long dirtied_when ;
   struct hlist_node i_hash ;
   struct list_head i_wb_list ;
   struct list_head i_lru ;
   struct list_head i_sb_list ;
   union __anonunion_ldv_21027_175 ldv_21027 ;
   u64 i_version ;
   atomic_t i_count ;
   atomic_t i_dio_count ;
   atomic_t i_writecount ;
   struct file_operations const *i_fop ;
   struct file_lock *i_flock ;
   struct address_space i_data ;
   struct dquot *i_dquot[2U] ;
   struct list_head i_devices ;
   union __anonunion_ldv_21043_176 ldv_21043 ;
   __u32 i_generation ;
   __u32 i_fsnotify_mask ;
   struct hlist_head i_fsnotify_marks ;
   atomic_t i_readcount ;
   void *i_private ;
};
struct fown_struct {
   rwlock_t lock ;
   struct pid *pid ;
   enum pid_type pid_type ;
   kuid_t uid ;
   kuid_t euid ;
   int signum ;
};
struct file_ra_state {
   unsigned long start ;
   unsigned int size ;
   unsigned int async_size ;
   unsigned int ra_pages ;
   unsigned int mmap_miss ;
   loff_t prev_pos ;
};
union __anonunion_f_u_177 {
   struct list_head fu_list ;
   struct callback_head fu_rcuhead ;
};
struct file {
   union __anonunion_f_u_177 f_u ;
   struct path f_path ;
   struct file_operations const *f_op ;
   spinlock_t f_lock ;
   int f_sb_list_cpu ;
   atomic_long_t f_count ;
   unsigned int f_flags ;
   fmode_t f_mode ;
   loff_t f_pos ;
   struct fown_struct f_owner ;
   struct cred const *f_cred ;
   struct file_ra_state f_ra ;
   u64 f_version ;
   void *f_security ;
   void *private_data ;
   struct list_head f_ep_links ;
   struct list_head f_tfile_llink ;
   struct address_space *f_mapping ;
   unsigned long f_mnt_write_state ;
};
typedef struct files_struct *fl_owner_t;
struct file_lock_operations {
   void (*fl_copy_lock)(struct file_lock * , struct file_lock * ) ;
   void (*fl_release_private)(struct file_lock * ) ;
};
struct lock_manager_operations {
   int (*lm_compare_owner)(struct file_lock * , struct file_lock * ) ;
   void (*lm_notify)(struct file_lock * ) ;
   int (*lm_grant)(struct file_lock * , struct file_lock * , int ) ;
   void (*lm_break)(struct file_lock * ) ;
   int (*lm_change)(struct file_lock ** , int ) ;
};
struct nlm_lockowner;
struct nfs_lock_info {
   u32 state ;
   struct nlm_lockowner *owner ;
   struct list_head list ;
};
struct nfs4_lock_state;
struct nfs4_lock_info {
   struct nfs4_lock_state *owner ;
};
struct fasync_struct;
struct __anonstruct_afs_179 {
   struct list_head link ;
   int state ;
};
union __anonunion_fl_u_178 {
   struct nfs_lock_info nfs_fl ;
   struct nfs4_lock_info nfs4_fl ;
   struct __anonstruct_afs_179 afs ;
};
struct file_lock {
   struct file_lock *fl_next ;
   struct list_head fl_link ;
   struct list_head fl_block ;
   fl_owner_t fl_owner ;
   unsigned int fl_flags ;
   unsigned char fl_type ;
   unsigned int fl_pid ;
   struct pid *fl_nspid ;
   wait_queue_head_t fl_wait ;
   struct file *fl_file ;
   loff_t fl_start ;
   loff_t fl_end ;
   struct fasync_struct *fl_fasync ;
   unsigned long fl_break_time ;
   unsigned long fl_downgrade_time ;
   struct file_lock_operations const *fl_ops ;
   struct lock_manager_operations const *fl_lmops ;
   union __anonunion_fl_u_178 fl_u ;
};
struct fasync_struct {
   spinlock_t fa_lock ;
   int magic ;
   int fa_fd ;
   struct fasync_struct *fa_next ;
   struct file *fa_file ;
   struct callback_head fa_rcu ;
};
struct sb_writers {
   struct percpu_counter counter[3U] ;
   wait_queue_head_t wait ;
   int frozen ;
   wait_queue_head_t wait_unfrozen ;
   struct lockdep_map lock_map[3U] ;
};
struct file_system_type;
struct super_operations;
struct xattr_handler;
struct mtd_info;
struct super_block {
   struct list_head s_list ;
   dev_t s_dev ;
   unsigned char s_blocksize_bits ;
   unsigned long s_blocksize ;
   loff_t s_maxbytes ;
   struct file_system_type *s_type ;
   struct super_operations const *s_op ;
   struct dquot_operations const *dq_op ;
   struct quotactl_ops const *s_qcop ;
   struct export_operations const *s_export_op ;
   unsigned long s_flags ;
   unsigned long s_magic ;
   struct dentry *s_root ;
   struct rw_semaphore s_umount ;
   int s_count ;
   atomic_t s_active ;
   void *s_security ;
   struct xattr_handler const **s_xattr ;
   struct list_head s_inodes ;
   struct hlist_bl_head s_anon ;
   struct list_head *s_files ;
   struct list_head s_mounts ;
   struct list_head s_dentry_lru ;
   int s_nr_dentry_unused ;
   spinlock_t s_inode_lru_lock ;
   struct list_head s_inode_lru ;
   int s_nr_inodes_unused ;
   struct block_device *s_bdev ;
   struct backing_dev_info *s_bdi ;
   struct mtd_info *s_mtd ;
   struct hlist_node s_instances ;
   struct quota_info s_dquot ;
   struct sb_writers s_writers ;
   char s_id[32U] ;
   u8 s_uuid[16U] ;
   void *s_fs_info ;
   unsigned int s_max_links ;
   fmode_t s_mode ;
   u32 s_time_gran ;
   struct mutex s_vfs_rename_mutex ;
   char *s_subtype ;
   char *s_options ;
   struct dentry_operations const *s_d_op ;
   int cleancache_poolid ;
   struct shrinker s_shrink ;
   atomic_long_t s_remove_count ;
   int s_readonly_remount ;
};
struct fiemap_extent_info {
   unsigned int fi_flags ;
   unsigned int fi_extents_mapped ;
   unsigned int fi_extents_max ;
   struct fiemap_extent *fi_extents_start ;
};
struct file_operations {
   struct module *owner ;
   loff_t (*llseek)(struct file * , loff_t , int ) ;
   ssize_t (*read)(struct file * , char * , size_t , loff_t * ) ;
   ssize_t (*write)(struct file * , char const * , size_t , loff_t * ) ;
   ssize_t (*aio_read)(struct kiocb * , struct iovec const * , unsigned long ,
                       loff_t ) ;
   ssize_t (*aio_write)(struct kiocb * , struct iovec const * , unsigned long ,
                        loff_t ) ;
   int (*readdir)(struct file * , void * , int (*)(void * , char const * , int ,
                                                   loff_t , u64 , unsigned int ) ) ;
   unsigned int (*poll)(struct file * , struct poll_table_struct * ) ;
   long (*unlocked_ioctl)(struct file * , unsigned int , unsigned long ) ;
   long (*compat_ioctl)(struct file * , unsigned int , unsigned long ) ;
   int (*mmap)(struct file * , struct vm_area_struct * ) ;
   int (*open)(struct inode * , struct file * ) ;
   int (*flush)(struct file * , fl_owner_t ) ;
   int (*release)(struct inode * , struct file * ) ;
   int (*fsync)(struct file * , loff_t , loff_t , int ) ;
   int (*aio_fsync)(struct kiocb * , int ) ;
   int (*fasync)(int , struct file * , int ) ;
   int (*lock)(struct file * , int , struct file_lock * ) ;
   ssize_t (*sendpage)(struct file * , struct page * , int , size_t , loff_t * ,
                       int ) ;
   unsigned long (*get_unmapped_area)(struct file * , unsigned long , unsigned long ,
                                      unsigned long , unsigned long ) ;
   int (*check_flags)(int ) ;
   int (*flock)(struct file * , int , struct file_lock * ) ;
   ssize_t (*splice_write)(struct pipe_inode_info * , struct file * , loff_t * , size_t ,
                           unsigned int ) ;
   ssize_t (*splice_read)(struct file * , loff_t * , struct pipe_inode_info * , size_t ,
                          unsigned int ) ;
   int (*setlease)(struct file * , long , struct file_lock ** ) ;
   long (*fallocate)(struct file * , int , loff_t , loff_t ) ;
};
struct inode_operations {
   struct dentry *(*lookup)(struct inode * , struct dentry * , unsigned int ) ;
   void *(*follow_link)(struct dentry * , struct nameidata * ) ;
   int (*permission)(struct inode * , int ) ;
   struct posix_acl *(*get_acl)(struct inode * , int ) ;
   int (*readlink)(struct dentry * , char * , int ) ;
   void (*put_link)(struct dentry * , struct nameidata * , void * ) ;
   int (*create)(struct inode * , struct dentry * , umode_t , bool ) ;
   int (*link)(struct dentry * , struct inode * , struct dentry * ) ;
   int (*unlink)(struct inode * , struct dentry * ) ;
   int (*symlink)(struct inode * , struct dentry * , char const * ) ;
   int (*mkdir)(struct inode * , struct dentry * , umode_t ) ;
   int (*rmdir)(struct inode * , struct dentry * ) ;
   int (*mknod)(struct inode * , struct dentry * , umode_t , dev_t ) ;
   int (*rename)(struct inode * , struct dentry * , struct inode * , struct dentry * ) ;
   void (*truncate)(struct inode * ) ;
   int (*setattr)(struct dentry * , struct iattr * ) ;
   int (*getattr)(struct vfsmount * , struct dentry * , struct kstat * ) ;
   int (*setxattr)(struct dentry * , char const * , void const * , size_t , int ) ;
   ssize_t (*getxattr)(struct dentry * , char const * , void * , size_t ) ;
   ssize_t (*listxattr)(struct dentry * , char * , size_t ) ;
   int (*removexattr)(struct dentry * , char const * ) ;
   int (*fiemap)(struct inode * , struct fiemap_extent_info * , u64 , u64 ) ;
   int (*update_time)(struct inode * , struct timespec * , int ) ;
   int (*atomic_open)(struct inode * , struct dentry * , struct file * , unsigned int ,
                      umode_t , int * ) ;
};
struct super_operations {
   struct inode *(*alloc_inode)(struct super_block * ) ;
   void (*destroy_inode)(struct inode * ) ;
   void (*dirty_inode)(struct inode * , int ) ;
   int (*write_inode)(struct inode * , struct writeback_control * ) ;
   int (*drop_inode)(struct inode * ) ;
   void (*evict_inode)(struct inode * ) ;
   void (*put_super)(struct super_block * ) ;
   int (*sync_fs)(struct super_block * , int ) ;
   int (*freeze_fs)(struct super_block * ) ;
   int (*unfreeze_fs)(struct super_block * ) ;
   int (*statfs)(struct dentry * , struct kstatfs * ) ;
   int (*remount_fs)(struct super_block * , int * , char * ) ;
   void (*umount_begin)(struct super_block * ) ;
   int (*show_options)(struct seq_file * , struct dentry * ) ;
   int (*show_devname)(struct seq_file * , struct dentry * ) ;
   int (*show_path)(struct seq_file * , struct dentry * ) ;
   int (*show_stats)(struct seq_file * , struct dentry * ) ;
   ssize_t (*quota_read)(struct super_block * , int , char * , size_t , loff_t ) ;
   ssize_t (*quota_write)(struct super_block * , int , char const * , size_t ,
                          loff_t ) ;
   int (*bdev_try_to_free_page)(struct super_block * , struct page * , gfp_t ) ;
   int (*nr_cached_objects)(struct super_block * ) ;
   void (*free_cached_objects)(struct super_block * , int ) ;
};
struct file_system_type {
   char const *name ;
   int fs_flags ;
   struct dentry *(*mount)(struct file_system_type * , int , char const * , void * ) ;
   void (*kill_sb)(struct super_block * ) ;
   struct module *owner ;
   struct file_system_type *next ;
   struct hlist_head fs_supers ;
   struct lock_class_key s_lock_key ;
   struct lock_class_key s_umount_key ;
   struct lock_class_key s_vfs_rename_key ;
   struct lock_class_key s_writers_key[3U] ;
   struct lock_class_key i_lock_key ;
   struct lock_class_key i_mutex_key ;
   struct lock_class_key i_mutex_dir_key ;
};
struct exception_table_entry {
   int insn ;
   int fixup ;
};
struct poll_table_struct {
   void (*_qproc)(struct file * , wait_queue_head_t * , struct poll_table_struct * ) ;
   unsigned long _key ;
};
struct dvb_frontend;
struct dvb_device;
struct dvb_adapter {
   int num ;
   struct list_head list_head ;
   struct list_head device_list ;
   char const *name ;
   u8 proposed_mac[6U] ;
   void *priv ;
   struct device *device ;
   struct module *module ;
   int mfe_shared ;
   struct dvb_device *mfe_dvbdev ;
   struct mutex mfe_lock ;
};
struct dvb_device {
   struct list_head list_head ;
   struct file_operations const *fops ;
   struct dvb_adapter *adapter ;
   int type ;
   int minor ;
   u32 id ;
   int readers ;
   int writers ;
   int users ;
   wait_queue_head_t wait_queue ;
   int (*kernel_ioctl)(struct file * , unsigned int , void * ) ;
   void *priv ;
};
struct dvb_frontend_tune_settings {
   int min_delay_ms ;
   int step_size ;
   int max_drift ;
};
struct dvb_tuner_info {
   char name[128U] ;
   u32 frequency_min ;
   u32 frequency_max ;
   u32 frequency_step ;
   u32 bandwidth_min ;
   u32 bandwidth_max ;
   u32 bandwidth_step ;
};
struct analog_parameters {
   unsigned int frequency ;
   unsigned int mode ;
   unsigned int audmode ;
   u64 std ;
};
enum tuner_param {
    DVBFE_TUNER_FREQUENCY = 1,
    DVBFE_TUNER_TUNERSTEP = 2,
    DVBFE_TUNER_IFFREQ = 4,
    DVBFE_TUNER_BANDWIDTH = 8,
    DVBFE_TUNER_REFCLOCK = 16,
    DVBFE_TUNER_IQSENSE = 32,
    DVBFE_TUNER_DUMMY = (-0x7FFFFFFF-1)
} ;
enum dvbfe_algo {
    DVBFE_ALGO_HW = 1,
    DVBFE_ALGO_SW = 2,
    DVBFE_ALGO_CUSTOM = 4,
    DVBFE_ALGO_RECOVERY = (-0x7FFFFFFF-1)
} ;
struct tuner_state {
   u32 frequency ;
   u32 tunerstep ;
   u32 ifreq ;
   u32 bandwidth ;
   u32 iqsense ;
   u32 refclock ;
};
enum dvbfe_search {
    DVBFE_ALGO_SEARCH_SUCCESS = 1,
    DVBFE_ALGO_SEARCH_ASLEEP = 2,
    DVBFE_ALGO_SEARCH_FAILED = 4,
    DVBFE_ALGO_SEARCH_INVALID = 8,
    DVBFE_ALGO_SEARCH_AGAIN = 16,
    DVBFE_ALGO_SEARCH_ERROR = (-0x7FFFFFFF-1)
} ;
struct dvb_tuner_ops {
   struct dvb_tuner_info info ;
   int (*release)(struct dvb_frontend * ) ;
   int (*init)(struct dvb_frontend * ) ;
   int (*sleep)(struct dvb_frontend * ) ;
   int (*set_params)(struct dvb_frontend * ) ;
   int (*set_analog_params)(struct dvb_frontend * , struct analog_parameters * ) ;
   int (*calc_regs)(struct dvb_frontend * , u8 * , int ) ;
   int (*set_config)(struct dvb_frontend * , void * ) ;
   int (*get_frequency)(struct dvb_frontend * , u32 * ) ;
   int (*get_bandwidth)(struct dvb_frontend * , u32 * ) ;
   int (*get_if_frequency)(struct dvb_frontend * , u32 * ) ;
   int (*get_status)(struct dvb_frontend * , u32 * ) ;
   int (*get_rf_strength)(struct dvb_frontend * , u16 * ) ;
   int (*get_afc)(struct dvb_frontend * , s32 * ) ;
   int (*set_frequency)(struct dvb_frontend * , u32 ) ;
   int (*set_bandwidth)(struct dvb_frontend * , u32 ) ;
   int (*set_state)(struct dvb_frontend * , enum tuner_param , struct tuner_state * ) ;
   int (*get_state)(struct dvb_frontend * , enum tuner_param , struct tuner_state * ) ;
};
struct analog_demod_info {
   char *name ;
};
struct analog_demod_ops {
   struct analog_demod_info info ;
   void (*set_params)(struct dvb_frontend * , struct analog_parameters * ) ;
   int (*has_signal)(struct dvb_frontend * ) ;
   int (*get_afc)(struct dvb_frontend * ) ;
   void (*tuner_status)(struct dvb_frontend * ) ;
   void (*standby)(struct dvb_frontend * ) ;
   void (*release)(struct dvb_frontend * ) ;
   int (*i2c_gate_ctrl)(struct dvb_frontend * , int ) ;
   int (*set_config)(struct dvb_frontend * , void * ) ;
};
struct dtv_frontend_properties;
struct dvb_frontend_ops {
   struct dvb_frontend_info info ;
   u8 delsys[8U] ;
   void (*release)(struct dvb_frontend * ) ;
   void (*release_sec)(struct dvb_frontend * ) ;
   int (*init)(struct dvb_frontend * ) ;
   int (*sleep)(struct dvb_frontend * ) ;
   int (*write)(struct dvb_frontend * , u8 const * , int ) ;
   int (*tune)(struct dvb_frontend * , bool , unsigned int , unsigned int * , fe_status_t * ) ;
   enum dvbfe_algo (*get_frontend_algo)(struct dvb_frontend * ) ;
   int (*set_frontend)(struct dvb_frontend * ) ;
   int (*get_tune_settings)(struct dvb_frontend * , struct dvb_frontend_tune_settings * ) ;
   int (*get_frontend)(struct dvb_frontend * ) ;
   int (*read_status)(struct dvb_frontend * , fe_status_t * ) ;
   int (*read_ber)(struct dvb_frontend * , u32 * ) ;
   int (*read_signal_strength)(struct dvb_frontend * , u16 * ) ;
   int (*read_snr)(struct dvb_frontend * , u16 * ) ;
   int (*read_ucblocks)(struct dvb_frontend * , u32 * ) ;
   int (*diseqc_reset_overload)(struct dvb_frontend * ) ;
   int (*diseqc_send_master_cmd)(struct dvb_frontend * , struct dvb_diseqc_master_cmd * ) ;
   int (*diseqc_recv_slave_reply)(struct dvb_frontend * , struct dvb_diseqc_slave_reply * ) ;
   int (*diseqc_send_burst)(struct dvb_frontend * , fe_sec_mini_cmd_t ) ;
   int (*set_tone)(struct dvb_frontend * , fe_sec_tone_mode_t ) ;
   int (*set_voltage)(struct dvb_frontend * , fe_sec_voltage_t ) ;
   int (*enable_high_lnb_voltage)(struct dvb_frontend * , long ) ;
   int (*dishnetwork_send_legacy_command)(struct dvb_frontend * , unsigned long ) ;
   int (*i2c_gate_ctrl)(struct dvb_frontend * , int ) ;
   int (*ts_bus_ctrl)(struct dvb_frontend * , int ) ;
   int (*set_lna)(struct dvb_frontend * ) ;
   enum dvbfe_search (*search)(struct dvb_frontend * ) ;
   struct dvb_tuner_ops tuner_ops ;
   struct analog_demod_ops analog_ops ;
   int (*set_property)(struct dvb_frontend * , struct dtv_property * ) ;
   int (*get_property)(struct dvb_frontend * , struct dtv_property * ) ;
};
struct __anonstruct_layer_181 {
   u8 segment_count ;
   fe_code_rate_t fec ;
   fe_modulation_t modulation ;
   u8 interleaving ;
};
struct dtv_frontend_properties {
   u32 state ;
   u32 frequency ;
   fe_modulation_t modulation ;
   fe_sec_voltage_t voltage ;
   fe_sec_tone_mode_t sectone ;
   fe_spectral_inversion_t inversion ;
   fe_code_rate_t fec_inner ;
   fe_transmit_mode_t transmission_mode ;
   u32 bandwidth_hz ;
   fe_guard_interval_t guard_interval ;
   fe_hierarchy_t hierarchy ;
   u32 symbol_rate ;
   fe_code_rate_t code_rate_HP ;
   fe_code_rate_t code_rate_LP ;
   fe_pilot_t pilot ;
   fe_rolloff_t rolloff ;
   fe_delivery_system_t delivery_system ;
   enum fe_interleaving interleaving ;
   u8 isdbt_partial_reception ;
   u8 isdbt_sb_mode ;
   u8 isdbt_sb_subchannel ;
   u32 isdbt_sb_segment_idx ;
   u32 isdbt_sb_segment_count ;
   u8 isdbt_layer_enabled ;
   struct __anonstruct_layer_181 layer[3U] ;
   u32 stream_id ;
   u8 atscmh_fic_ver ;
   u8 atscmh_parade_id ;
   u8 atscmh_nog ;
   u8 atscmh_tnog ;
   u8 atscmh_sgn ;
   u8 atscmh_prc ;
   u8 atscmh_rs_frame_mode ;
   u8 atscmh_rs_frame_ensemble ;
   u8 atscmh_rs_code_mode_pri ;
   u8 atscmh_rs_code_mode_sec ;
   u8 atscmh_sccc_block_mode ;
   u8 atscmh_sccc_code_mode_a ;
   u8 atscmh_sccc_code_mode_b ;
   u8 atscmh_sccc_code_mode_c ;
   u8 atscmh_sccc_code_mode_d ;
   u32 lna ;
};
struct dvb_frontend {
   struct dvb_frontend_ops ops ;
   struct dvb_adapter *dvb ;
   void *demodulator_priv ;
   void *tuner_priv ;
   void *frontend_priv ;
   void *sec_priv ;
   void *analog_demod_priv ;
   struct dtv_frontend_properties dtv_property_cache ;
   int (*callback)(void * , int , int , int ) ;
   int id ;
};
enum tuner_mode {
    TUNER_SLEEP = 1,
    TUNER_WAKE = 2
} ;
enum stv090x_demodulator {
    STV090x_DEMODULATOR_0 = 1,
    STV090x_DEMODULATOR_1 = 2
} ;
enum stv090x_device {
    STV0903 = 0,
    STV0900 = 1
} ;
enum stv090x_mode {
    STV090x_DUAL = 0,
    STV090x_SINGLE = 1
} ;
enum stv090x_clkmode {
    STV090x_CLK_INT = 0,
    STV090x_CLK_EXT = 2
} ;
enum stv090x_i2crpt {
    STV090x_RPTLEVEL_256 = 0,
    STV090x_RPTLEVEL_128 = 1,
    STV090x_RPTLEVEL_64 = 2,
    STV090x_RPTLEVEL_32 = 3,
    STV090x_RPTLEVEL_16 = 4,
    STV090x_RPTLEVEL_8 = 5,
    STV090x_RPTLEVEL_4 = 6,
    STV090x_RPTLEVEL_2 = 7
} ;
enum stv090x_adc_range {
    STV090x_ADC_2Vpp = 0,
    STV090x_ADC_1Vpp = 1
} ;
struct stv090x_config {
   enum stv090x_device device ;
   enum stv090x_mode demod_mode ;
   enum stv090x_clkmode clk_mode ;
   u32 xtal ;
   u8 address ;
   u8 ts1_mode ;
   u8 ts2_mode ;
   u32 ts1_clk ;
   u32 ts2_clk ;
   unsigned char ts1_tei : 1 ;
   unsigned char ts2_tei : 1 ;
   enum stv090x_i2crpt repeater_level ;
   u8 tuner_bbgain ;
   enum stv090x_adc_range adc1_range ;
   enum stv090x_adc_range adc2_range ;
   bool diseqc_envelope_mode ;
   int (*tuner_init)(struct dvb_frontend * ) ;
   int (*tuner_sleep)(struct dvb_frontend * ) ;
   int (*tuner_set_mode)(struct dvb_frontend * , enum tuner_mode ) ;
   int (*tuner_set_frequency)(struct dvb_frontend * , u32 ) ;
   int (*tuner_get_frequency)(struct dvb_frontend * , u32 * ) ;
   int (*tuner_set_bandwidth)(struct dvb_frontend * , u32 ) ;
   int (*tuner_get_bandwidth)(struct dvb_frontend * , u32 * ) ;
   int (*tuner_set_bbgain)(struct dvb_frontend * , u32 ) ;
   int (*tuner_get_bbgain)(struct dvb_frontend * , u32 * ) ;
   int (*tuner_set_refclk)(struct dvb_frontend * , u32 ) ;
   int (*tuner_get_status)(struct dvb_frontend * , u32 * ) ;
   void (*tuner_i2c_lock)(struct dvb_frontend * , int ) ;
};
enum stv090x_signal_state {
    STV090x_NOAGC1 = 0,
    STV090x_NOCARRIER = 1,
    STV090x_NODATA = 2,
    STV090x_DATAOK = 3,
    STV090x_RANGEOK = 4,
    STV090x_OUTOFRANGE = 5
} ;
enum stv090x_fec {
    STV090x_PR12 = 0,
    STV090x_PR23 = 1,
    STV090x_PR34 = 2,
    STV090x_PR45 = 3,
    STV090x_PR56 = 4,
    STV090x_PR67 = 5,
    STV090x_PR78 = 6,
    STV090x_PR89 = 7,
    STV090x_PR910 = 8,
    STV090x_PRERR = 9
} ;
enum stv090x_modulation {
    STV090x_QPSK = 0,
    STV090x_8PSK = 1,
    STV090x_16APSK = 2,
    STV090x_32APSK = 3,
    STV090x_UNKNOWN = 4
} ;
enum stv090x_frame {
    STV090x_LONG_FRAME = 0,
    STV090x_SHORT_FRAME = 1
} ;
enum stv090x_pilot {
    STV090x_PILOTS_OFF = 0,
    STV090x_PILOTS_ON = 1
} ;
enum stv090x_rolloff {
    STV090x_RO_35 = 0,
    STV090x_RO_25 = 1,
    STV090x_RO_20 = 2
} ;
enum stv090x_inversion {
    STV090x_IQ_AUTO = 0,
    STV090x_IQ_NORMAL = 1,
    STV090x_IQ_SWAP = 2
} ;
enum stv090x_modcod {
    STV090x_DUMMY_PLF = 0,
    STV090x_QPSK_14 = 1,
    STV090x_QPSK_13 = 2,
    STV090x_QPSK_25 = 3,
    STV090x_QPSK_12 = 4,
    STV090x_QPSK_35 = 5,
    STV090x_QPSK_23 = 6,
    STV090x_QPSK_34 = 7,
    STV090x_QPSK_45 = 8,
    STV090x_QPSK_56 = 9,
    STV090x_QPSK_89 = 10,
    STV090x_QPSK_910 = 11,
    STV090x_8PSK_35 = 12,
    STV090x_8PSK_23 = 13,
    STV090x_8PSK_34 = 14,
    STV090x_8PSK_56 = 15,
    STV090x_8PSK_89 = 16,
    STV090x_8PSK_910 = 17,
    STV090x_16APSK_23 = 18,
    STV090x_16APSK_34 = 19,
    STV090x_16APSK_45 = 20,
    STV090x_16APSK_56 = 21,
    STV090x_16APSK_89 = 22,
    STV090x_16APSK_910 = 23,
    STV090x_32APSK_34 = 24,
    STV090x_32APSK_45 = 25,
    STV090x_32APSK_56 = 26,
    STV090x_32APSK_89 = 27,
    STV090x_32APSK_910 = 28,
    STV090x_MODCODE_UNKNOWN = 29
} ;
enum stv090x_search {
    STV090x_SEARCH_DSS = 0,
    STV090x_SEARCH_DVBS1 = 1,
    STV090x_SEARCH_DVBS2 = 2,
    STV090x_SEARCH_AUTO = 3
} ;
enum stv090x_algo {
    STV090x_BLIND_SEARCH = 0,
    STV090x_COLD_SEARCH = 1,
    STV090x_WARM_SEARCH = 2
} ;
enum stv090x_delsys {
    STV090x_ERROR = 0,
    STV090x_DVBS1 = 1,
    STV090x_DVBS2 = 2,
    STV090x_DSS = 3
} ;
struct stv090x_long_frame_crloop {
   enum stv090x_modcod modcod ;
   u8 crl_pilots_on_2 ;
   u8 crl_pilots_off_2 ;
   u8 crl_pilots_on_5 ;
   u8 crl_pilots_off_5 ;
   u8 crl_pilots_on_10 ;
   u8 crl_pilots_off_10 ;
   u8 crl_pilots_on_20 ;
   u8 crl_pilots_off_20 ;
   u8 crl_pilots_on_30 ;
   u8 crl_pilots_off_30 ;
};
struct stv090x_short_frame_crloop {
   enum stv090x_modulation modulation ;
   u8 crl_2 ;
   u8 crl_5 ;
   u8 crl_10 ;
   u8 crl_20 ;
   u8 crl_30 ;
};
struct stv090x_reg {
   u16 addr ;
   u8 data ;
};
struct stv090x_tab {
   s32 real ;
   s32 read ;
};
struct stv090x_internal {
   struct i2c_adapter *i2c_adap ;
   u8 i2c_addr ;
   struct mutex demod_lock ;
   struct mutex tuner_lock ;
   s32 mclk ;
   u32 dev_ver ;
   int num_used ;
};
struct stv090x_state {
   enum stv090x_device device ;
   enum stv090x_demodulator demod ;
   enum stv090x_mode demod_mode ;
   struct stv090x_internal *internal ;
   struct i2c_adapter *i2c ;
   struct stv090x_config const *config ;
   struct dvb_frontend frontend ;
   u32 *verbose ;
   enum stv090x_delsys delsys ;
   enum stv090x_fec fec ;
   enum stv090x_modulation modulation ;
   enum stv090x_modcod modcod ;
   enum stv090x_search search_mode ;
   enum stv090x_frame frame_len ;
   enum stv090x_pilot pilots ;
   enum stv090x_rolloff rolloff ;
   enum stv090x_inversion inversion ;
   enum stv090x_algo algo ;
   u32 frequency ;
   u32 srate ;
   s32 tuner_bw ;
   s32 search_range ;
   s32 DemodTimeout ;
   s32 FecTimeout ;
};
struct stv090x_dev {
   struct stv090x_internal *internal ;
   struct stv090x_dev *next_dev ;
};
typedef int ldv_func_ret_type___4;
long ldv__builtin_expect(long exp , long c ) ;
extern int printk(char const * , ...) ;
extern void __mutex_init(struct mutex * , char const * , struct lock_class_key * ) ;
extern int mutex_trylock(struct mutex * ) ;
int ldv_mutex_trylock_6(struct mutex *ldv_func_arg1 ) ;
extern void mutex_unlock(struct mutex * ) ;
void ldv_mutex_unlock_2(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_unlock_4(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_unlock_7(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_unlock_9(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_unlock_11(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_unlock_13(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_unlock_14(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_unlock_16(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_unlock_18(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_unlock_19(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_unlock_21(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_unlock_22(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_unlock_24(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_unlock_25(struct mutex *ldv_func_arg1 ) ;
extern void mutex_lock(struct mutex * ) ;
void ldv_mutex_lock_1(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_lock_3(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_lock_5(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_lock_8(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_lock_10(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_lock_12(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_lock_15(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_lock_17(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_lock_20(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_lock_23(struct mutex *ldv_func_arg1 ) ;
void ldv_mutex_lock_cred_guard_mutex(struct mutex *lock ) ;
void ldv_mutex_unlock_cred_guard_mutex(struct mutex *lock ) ;
void ldv_mutex_lock_demod_lock(struct mutex *lock ) ;
void ldv_mutex_unlock_demod_lock(struct mutex *lock ) ;
void ldv_mutex_lock_lock(struct mutex *lock ) ;
void ldv_mutex_unlock_lock(struct mutex *lock ) ;
void ldv_mutex_lock_mtx(struct mutex *lock ) ;
void ldv_mutex_unlock_mtx(struct mutex *lock ) ;
void ldv_mutex_lock_mutex(struct mutex *lock ) ;
int ldv_mutex_trylock_mutex(struct mutex *lock ) ;
void ldv_mutex_unlock_mutex(struct mutex *lock ) ;
void ldv_mutex_lock_tuner_lock(struct mutex *lock ) ;
void ldv_mutex_unlock_tuner_lock(struct mutex *lock ) ;
extern void kfree(void const * ) ;
extern int __VERIFIER_nondet_int(void);
extern void __VERIFIER_assume(int);
extern void *malloc(size_t size);
extern void *memcpy(void * , void const * , size_t ) ;
long ldv_is_err(const void *ptr)
{
  return ((unsigned long)ptr > ((unsigned long)-4095));
}
void *ldv_malloc(size_t size)
{
 if (__VERIFIER_nondet_int()) {
  void *res = malloc(size);
  __VERIFIER_assume(!ldv_is_err(res));
  return res;
 } else {
  return ((void *)0);
 }
}
void *__kmalloc(size_t size, gfp_t t)
{
 return ldv_malloc(size);
}
__inline static void *kmalloc(size_t size , gfp_t flags )
{
  void *tmp___2 ;
  {
  tmp___2 = __kmalloc(size, flags);
  return (tmp___2);
}
}
__inline static void *kzalloc(size_t size , gfp_t flags )
{
  void *tmp ;
  {
  tmp = kmalloc(size, flags | 32768U);
  return (tmp);
}
}
extern int i2c_transfer(struct i2c_adapter * , struct i2c_msg * , int ) ;
extern void msleep(unsigned int ) ;
struct dvb_frontend *stv090x_attach(struct stv090x_config const *config , struct i2c_adapter *i2c ,
                                    enum stv090x_demodulator demod ) ;
int stv090x_set_gpio(struct dvb_frontend *fe , u8 gpio , u8 dir , u8 value , u8 xor_value ) ;
static unsigned int verbose ;
static struct stv090x_dev *stv090x_first_dev ;
static struct stv090x_dev *find_dev(struct i2c_adapter *i2c_adap , u8 i2c_addr )
{
  struct stv090x_dev *temp_dev ;
  {
  temp_dev = stv090x_first_dev;
  goto ldv_23957;
  ldv_23956:
  temp_dev = temp_dev->next_dev;
  ldv_23957: ;
  if ((unsigned long )temp_dev != (unsigned long )((struct stv090x_dev *)0) && ((unsigned long )(temp_dev->internal)->i2c_adap != (unsigned long )i2c_adap || (int )(temp_dev->internal)->i2c_addr != (int )i2c_addr)) {
    goto ldv_23956;
  } else {
  }
  return (temp_dev);
}
}
static void remove_dev(struct stv090x_internal *internal )
{
  struct stv090x_dev *prev_dev ;
  struct stv090x_dev *del_dev ;
  struct stv090x_dev *tmp ;
  {
  prev_dev = stv090x_first_dev;
  tmp = find_dev(internal->i2c_adap, (int )internal->i2c_addr);
  del_dev = tmp;
  if ((unsigned long )del_dev != (unsigned long )((struct stv090x_dev *)0)) {
    if ((unsigned long )del_dev == (unsigned long )stv090x_first_dev) {
      stv090x_first_dev = del_dev->next_dev;
    } else {
      goto ldv_23965;
      ldv_23964:
      prev_dev = prev_dev->next_dev;
      ldv_23965: ;
      if ((unsigned long )prev_dev->next_dev != (unsigned long )del_dev) {
        goto ldv_23964;
      } else {
      }
      prev_dev->next_dev = del_dev->next_dev;
    }
    kfree((void const *)del_dev);
  } else {
  }
  return;
}
}
static struct stv090x_dev *append_internal(struct stv090x_internal *internal )
{
  struct stv090x_dev *new_dev ;
  struct stv090x_dev *temp_dev ;
  void *tmp ;
  {
  tmp = kmalloc(16UL, 208U);
  new_dev = (struct stv090x_dev *)tmp;
  if ((unsigned long )new_dev != (unsigned long )((struct stv090x_dev *)0)) {
    new_dev->internal = internal;
    new_dev->next_dev = 0;
    if ((unsigned long )stv090x_first_dev == (unsigned long )((struct stv090x_dev *)0)) {
      stv090x_first_dev = new_dev;
    } else {
      temp_dev = stv090x_first_dev;
      goto ldv_23973;
      ldv_23972:
      temp_dev = temp_dev->next_dev;
      ldv_23973: ;
      if ((unsigned long )temp_dev->next_dev != (unsigned long )((struct stv090x_dev *)0)) {
        goto ldv_23972;
      } else {
      }
      temp_dev->next_dev = new_dev;
    }
  } else {
  }
  return (new_dev);
}
}
static struct stv090x_tab const stv090x_s1cn_tab[52U] =
  { {0, 8917},
        {5, 8801},
        {10, 8667},
        {15, 8522},
        {20, 8355},
        {25, 8175},
        {30, 7979},
        {35, 7763},
        {40, 7530},
        {45, 7282},
        {50, 7026},
        {55, 6781},
        {60, 6514},
        {65, 6241},
        {70, 5965},
        {75, 5690},
        {80, 5424},
        {85, 5161},
        {90, 4902},
        {95, 4654},
        {100, 4417},
        {105, 4186},
        {110, 3968},
        {115, 3757},
        {120, 3558},
        {125, 3366},
        {130, 3185},
        {135, 3012},
        {140, 2850},
        {145, 2698},
        {150, 2550},
        {160, 2283},
        {170, 2042},
        {180, 1827},
        {190, 1636},
        {200, 1466},
        {210, 1315},
        {220, 1181},
        {230, 1064},
        {240, 960},
        {250, 869},
        {260, 792},
        {270, 724},
        {280, 665},
        {290, 616},
        {300, 573},
        {310, 537},
        {320, 507},
        {330, 483},
        {400, 398},
        {450, 381},
        {500, 377}};
static struct stv090x_tab const stv090x_s2cn_tab[55U] =
  { {-30, 13348},
        {-20, 12640},
        {-10, 11883},
        {0, 11101},
        {5, 10718},
        {10, 10339},
        {15, 9947},
        {20, 9552},
        {25, 9183},
        {30, 8799},
        {35, 8422},
        {40, 8062},
        {45, 7707},
        {50, 7353},
        {55, 7025},
        {60, 6684},
        {65, 6331},
        {70, 6036},
        {75, 5727},
        {80, 5437},
        {85, 5164},
        {90, 4902},
        {95, 4653},
        {100, 4408},
        {105, 4187},
        {110, 3961},
        {115, 3751},
        {120, 3558},
        {125, 3368},
        {130, 3191},
        {135, 3017},
        {140, 2862},
        {145, 2710},
        {150, 2565},
        {160, 2300},
        {170, 2058},
        {180, 1849},
        {190, 1663},
        {200, 1495},
        {210, 1349},
        {220, 1222},
        {230, 1110},
        {240, 1011},
        {250, 925},
        {260, 853},
        {270, 789},
        {280, 734},
        {290, 690},
        {300, 650},
        {310, 619},
        {320, 593},
        {330, 571},
        {400, 498},
        {450, 484},
        {500, 481}};
static struct stv090x_tab const stv090x_rf_tab[14U] =
  { {-5, 51873},
        {-10, 49705},
        {-15, 47880},
        {-20, 46268},
        {-25, 44378},
        {-30, 41624},
        {-35, 39080},
        {-40, 33673},
        {-45, 22974},
        {-50, 14868},
        {-55, 11537},
        {-60, 8461},
        {-65, 41295},
        {-70, 1962}};
static struct stv090x_reg stv0900_initval[161U] =
  { {61724U, 0U},
        {61725U, 255U},
        {61778U, 17U},
        {61782U, 19U},
        {63088U, 20U},
        {61921U, 33U},
        {61923U, 33U},
        {61840U, 34U},
        {61849U, 192U},
        {61850U, 192U},
        {61841U, 0U},
        {61972U, 249U},
        {61968U, 8U},
        {61982U, 196U},
        {62013U, 237U},
        {62015U, 208U},
        {62016U, 184U},
        {62032U, 210U},
        {62035U, 32U},
        {62544U, 210U},
        {62036U, 0U},
        {62368U, 136U},
        {62370U, 58U},
        {62376U, 0U},
        {62386U, 16U},
        {62360U, 53U},
        {62364U, 193U},
        {62017U, 248U},
        {61953U, 28U},
        {61975U, 32U},
        {61984U, 112U},
        {61985U, 136U},
        {61996U, 91U},
        {61997U, 56U},
        {62008U, 228U},
        {62009U, 26U},
        {62010U, 9U},
        {62014U, 8U},
        {62040U, 193U},
        {62037U, 240U},
        {62038U, 112U},
        {62041U, 88U},
        {62042U, 1U},
        {62096U, 38U},
        {62108U, 134U},
        {62109U, 134U},
        {62208U, 119U},
        {62209U, 133U},
        {62210U, 119U},
        {62324U, 32U},
        {61973U, 59U},
        {62128U, 255U},
        {62129U, 255U},
        {62130U, 255U},
        {62131U, 255U},
        {62132U, 255U},
        {62133U, 255U},
        {62134U, 255U},
        {62135U, 204U},
        {62136U, 204U},
        {62137U, 204U},
        {62138U, 204U},
        {62139U, 204U},
        {62140U, 204U},
        {62141U, 204U},
        {62142U, 204U},
        {62143U, 207U},
        {61856U, 34U},
        {61865U, 192U},
        {61866U, 192U},
        {61857U, 0U},
        {62484U, 249U},
        {62480U, 8U},
        {62494U, 196U},
        {62487U, 32U},
        {62525U, 237U},
        {62527U, 208U},
        {62528U, 184U},
        {62544U, 210U},
        {62547U, 32U},
        {62548U, 0U},
        {62549U, 240U},
        {62550U, 112U},
        {62836U, 32U},
        {62880U, 136U},
        {62882U, 58U},
        {62888U, 0U},
        {62898U, 16U},
        {62872U, 53U},
        {62876U, 193U},
        {62529U, 248U},
        {62465U, 28U},
        {62496U, 112U},
        {62497U, 136U},
        {62508U, 91U},
        {62509U, 56U},
        {62520U, 228U},
        {62521U, 26U},
        {62522U, 9U},
        {62526U, 8U},
        {62552U, 193U},
        {62553U, 88U},
        {62554U, 1U},
        {62608U, 38U},
        {62620U, 134U},
        {62621U, 134U},
        {62720U, 119U},
        {62721U, 133U},
        {62722U, 119U},
        {62485U, 59U},
        {62640U, 255U},
        {62641U, 255U},
        {62642U, 255U},
        {62643U, 255U},
        {62644U, 255U},
        {62645U, 255U},
        {62646U, 255U},
        {62647U, 204U},
        {62648U, 204U},
        {62649U, 204U},
        {62650U, 204U},
        {62651U, 204U},
        {62652U, 204U},
        {62653U, 204U},
        {62654U, 204U},
        {62655U, 207U},
        {64134U, 29U},
        {64003U, 55U},
        {64004U, 41U},
        {64005U, 55U},
        {64006U, 51U},
        {64007U, 49U},
        {64008U, 47U},
        {64009U, 57U},
        {64010U, 58U},
        {64011U, 41U},
        {64012U, 55U},
        {64013U, 51U},
        {64014U, 47U},
        {64015U, 57U},
        {64016U, 58U},
        {64063U, 4U},
        {64067U, 12U},
        {64068U, 15U},
        {64069U, 17U},
        {64070U, 20U},
        {64071U, 23U},
        {64072U, 25U},
        {64073U, 32U},
        {64074U, 33U},
        {64075U, 13U},
        {64076U, 15U},
        {64077U, 19U},
        {64078U, 26U},
        {64079U, 31U},
        {64080U, 33U},
        {62976U, 32U},
        {62771U, 1U},
        {62259U, 1U},
        {62780U, 47U},
        {62268U, 47U}};
static struct stv090x_reg stv0903_initval[99U] =
  { {61724U, 0U},
        {61778U, 17U},
        {61890U, 72U},
        {61891U, 20U},
        {61920U, 39U},
        {61921U, 33U},
        {61856U, 34U},
        {61865U, 192U},
        {61866U, 192U},
        {61857U, 0U},
        {62484U, 249U},
        {62480U, 8U},
        {62494U, 196U},
        {62525U, 237U},
        {62689U, 130U},
        {62527U, 208U},
        {62528U, 184U},
        {62544U, 210U},
        {62547U, 32U},
        {62548U, 0U},
        {62549U, 240U},
        {62550U, 112U},
        {62836U, 32U},
        {62880U, 136U},
        {62882U, 58U},
        {62888U, 0U},
        {62898U, 16U},
        {62872U, 53U},
        {62876U, 193U},
        {62529U, 248U},
        {62465U, 28U},
        {62487U, 32U},
        {62496U, 112U},
        {62497U, 136U},
        {62508U, 91U},
        {62509U, 56U},
        {62520U, 228U},
        {62521U, 26U},
        {62522U, 9U},
        {62526U, 8U},
        {62552U, 193U},
        {62553U, 88U},
        {62554U, 1U},
        {62608U, 38U},
        {62620U, 134U},
        {62621U, 134U},
        {62720U, 119U},
        {62721U, 133U},
        {62722U, 119U},
        {62485U, 59U},
        {62640U, 255U},
        {62641U, 255U},
        {62642U, 255U},
        {62643U, 255U},
        {62644U, 255U},
        {62645U, 255U},
        {62646U, 255U},
        {62647U, 204U},
        {62648U, 204U},
        {62649U, 204U},
        {62650U, 204U},
        {62651U, 204U},
        {62652U, 204U},
        {62653U, 204U},
        {62654U, 204U},
        {62655U, 207U},
        {64134U, 28U},
        {64003U, 55U},
        {64004U, 41U},
        {64005U, 55U},
        {64006U, 51U},
        {64007U, 49U},
        {64008U, 47U},
        {64009U, 57U},
        {64010U, 58U},
        {64011U, 41U},
        {64012U, 55U},
        {64013U, 51U},
        {64014U, 47U},
        {64015U, 57U},
        {64016U, 58U},
        {64063U, 4U},
        {64067U, 12U},
        {64068U, 15U},
        {64069U, 17U},
        {64070U, 20U},
        {64071U, 23U},
        {64072U, 25U},
        {64073U, 32U},
        {64074U, 33U},
        {64075U, 13U},
        {64076U, 15U},
        {64077U, 19U},
        {64078U, 26U},
        {64079U, 31U},
        {64080U, 33U},
        {62976U, 32U},
        {62771U, 1U},
        {62780U, 47U}};
static struct stv090x_reg stv0900_cut20_val[32U] =
  { {61982U, 232U},
        {61983U, 16U},
        {62013U, 56U},
        {62014U, 32U},
        {62040U, 90U},
        {62208U, 6U},
        {62209U, 0U},
        {62210U, 4U},
        {61953U, 12U},
        {62494U, 232U},
        {62495U, 16U},
        {62525U, 56U},
        {62526U, 32U},
        {62552U, 90U},
        {62720U, 6U},
        {62721U, 0U},
        {62722U, 4U},
        {62465U, 12U},
        {64067U, 33U},
        {64068U, 33U},
        {64069U, 32U},
        {64070U, 31U},
        {64071U, 30U},
        {64072U, 30U},
        {64073U, 29U},
        {64074U, 27U},
        {64075U, 32U},
        {64076U, 32U},
        {64077U, 32U},
        {64078U, 32U},
        {64079U, 32U},
        {64080U, 33U}};
static struct stv090x_reg stv0903_cut20_val[23U] =
  { {62494U, 232U},
        {62495U, 16U},
        {62525U, 56U},
        {62526U, 32U},
        {62552U, 90U},
        {62720U, 6U},
        {62721U, 0U},
        {62722U, 4U},
        {62465U, 12U},
        {64067U, 33U},
        {64068U, 33U},
        {64069U, 32U},
        {64070U, 31U},
        {64071U, 30U},
        {64072U, 30U},
        {64073U, 29U},
        {64074U, 27U},
        {64075U, 32U},
        {64076U, 32U},
        {64077U, 32U},
        {64078U, 32U},
        {64079U, 32U},
        {64080U, 33U}};
static struct stv090x_long_frame_crloop stv090x_s2_crl_cut20[14U] =
  { {STV090x_QPSK_12, 31U, 63U, 30U, 63U, 61U, 31U, 61U, 62U, 61U, 30U},
        {STV090x_QPSK_35, 47U, 63U, 46U, 47U, 61U, 15U, 14U, 46U, 61U, 14U},
        {STV090x_QPSK_23, 47U, 63U, 46U, 47U, 14U, 15U, 14U, 30U, 61U, 61U},
        {STV090x_QPSK_34, 63U, 63U, 62U, 31U, 14U, 62U, 14U, 30U, 61U, 61U},
        {STV090x_QPSK_45, 63U, 63U, 62U, 31U, 14U, 62U, 14U, 30U, 61U, 61U},
        {STV090x_QPSK_56, 63U, 63U, 62U, 31U, 14U, 62U, 14U, 30U, 61U, 61U},
        {STV090x_QPSK_89, 63U, 63U, 62U, 31U, 30U, 62U, 14U, 30U, 61U, 61U},
        {STV090x_QPSK_910, 63U, 63U, 62U, 31U, 30U, 62U, 14U, 30U, 61U, 61U},
        {STV090x_8PSK_35, 60U, 62U, 28U, 46U, 12U, 30U, 43U, 45U, 27U, 29U},
        {STV090x_8PSK_23, 29U, 62U, 60U, 46U, 44U, 30U, 12U, 45U, 43U, 29U},
        {STV090x_8PSK_34, 14U, 62U, 61U, 46U, 13U, 30U, 44U, 45U, 12U, 29U},
        {STV090x_8PSK_56, 46U, 62U, 30U, 46U, 45U, 30U, 60U, 45U, 44U, 29U},
        {STV090x_8PSK_89, 62U, 62U, 30U, 46U, 61U, 30U, 13U, 45U, 60U, 29U},
        {STV090x_8PSK_910, 62U, 62U, 30U, 46U, 61U, 30U, 29U, 45U, 13U, 29U}};
static struct stv090x_long_frame_crloop stv090x_s2_crl_cut30[14U] =
  { {STV090x_QPSK_12, 60U, 44U, 12U, 44U, 27U, 44U, 27U, 28U, 11U, 59U},
        {STV090x_QPSK_35, 13U, 13U, 12U, 13U, 27U, 60U, 27U, 28U, 11U, 59U},
        {STV090x_QPSK_23, 29U, 13U, 12U, 29U, 43U, 60U, 27U, 28U, 11U, 59U},
        {STV090x_QPSK_34, 29U, 29U, 12U, 29U, 43U, 60U, 27U, 28U, 11U, 59U},
        {STV090x_QPSK_45, 45U, 29U, 28U, 29U, 43U, 60U, 43U, 12U, 27U, 59U},
        {STV090x_QPSK_56, 45U, 29U, 28U, 29U, 43U, 60U, 43U, 12U, 27U, 59U},
        {STV090x_QPSK_89, 61U, 45U, 28U, 29U, 59U, 60U, 43U, 12U, 27U, 59U},
        {STV090x_QPSK_910, 61U, 45U, 28U, 29U, 59U, 60U, 43U, 12U, 27U, 59U},
        {STV090x_8PSK_35, 57U, 41U, 57U, 25U, 25U, 25U, 25U, 25U, 9U, 25U},
        {STV090x_8PSK_23, 42U, 57U, 26U, 10U, 57U, 10U, 41U, 57U, 41U, 10U},
        {STV090x_8PSK_34, 43U, 58U, 27U, 27U, 58U, 27U, 26U, 11U, 26U, 58U},
        {STV090x_8PSK_56, 12U, 27U, 59U, 59U, 27U, 59U, 58U, 59U, 58U, 27U},
        {STV090x_8PSK_89, 13U, 60U, 44U, 44U, 43U, 12U, 11U, 59U, 11U, 27U},
        {STV090x_8PSK_910, 13U, 13U, 44U, 60U, 59U, 28U, 11U, 59U, 11U, 27U}};
static struct stv090x_long_frame_crloop stv090x_s2_apsk_crl_cut20[11U] =
  { {STV090x_16APSK_23, 12U, 12U, 12U, 12U, 29U, 12U, 60U, 12U, 44U, 12U},
        {STV090x_16APSK_34, 12U, 12U, 12U, 12U, 14U, 12U, 45U, 12U, 29U, 12U},
        {STV090x_16APSK_45, 12U, 12U, 12U, 12U, 30U, 12U, 61U, 12U, 45U, 12U},
        {STV090x_16APSK_56, 12U, 12U, 12U, 12U, 30U, 12U, 61U, 12U, 45U, 12U},
        {STV090x_16APSK_89, 12U, 12U, 12U, 12U, 46U, 12U, 14U, 12U, 61U, 12U},
        {STV090x_16APSK_910, 12U, 12U, 12U, 12U, 46U, 12U, 14U, 12U, 61U, 12U},
        {STV090x_32APSK_34, 12U, 12U, 12U, 12U, 12U, 12U, 12U, 12U, 12U, 12U},
        {STV090x_32APSK_45, 12U, 12U, 12U, 12U, 12U, 12U, 12U, 12U, 12U, 12U},
        {STV090x_32APSK_56, 12U, 12U, 12U, 12U, 12U, 12U, 12U, 12U, 12U, 12U},
        {STV090x_32APSK_89, 12U, 12U, 12U, 12U, 12U, 12U, 12U, 12U, 12U, 12U},
        {STV090x_32APSK_910, 12U, 12U, 12U, 12U, 12U, 12U, 12U, 12U, 12U, 12U}};
static struct stv090x_long_frame_crloop stv090x_s2_apsk_crl_cut30[11U] =
  { {STV090x_16APSK_23, 10U, 10U, 10U, 10U, 26U, 10U, 58U, 10U, 42U, 10U},
        {STV090x_16APSK_34, 10U, 10U, 10U, 10U, 11U, 10U, 59U, 10U, 27U, 10U},
        {STV090x_16APSK_45, 10U, 10U, 10U, 10U, 27U, 10U, 59U, 10U, 43U, 10U},
        {STV090x_16APSK_56, 10U, 10U, 10U, 10U, 27U, 10U, 59U, 10U, 43U, 10U},
        {STV090x_16APSK_89, 10U, 10U, 10U, 10U, 43U, 10U, 12U, 10U, 59U, 10U},
        {STV090x_16APSK_910, 10U, 10U, 10U, 10U, 43U, 10U, 12U, 10U, 59U, 10U},
        {STV090x_32APSK_34, 10U, 10U, 10U, 10U, 10U, 10U, 10U, 10U, 10U, 10U},
        {STV090x_32APSK_45, 10U, 10U, 10U, 10U, 10U, 10U, 10U, 10U, 10U, 10U},
        {STV090x_32APSK_56, 10U, 10U, 10U, 10U, 10U, 10U, 10U, 10U, 10U, 10U},
        {STV090x_32APSK_89, 10U, 10U, 10U, 10U, 10U, 10U, 10U, 10U, 10U, 10U},
        {STV090x_32APSK_910, 10U, 10U, 10U, 10U, 10U, 10U, 10U, 10U, 10U, 10U}};
static struct stv090x_long_frame_crloop stv090x_s2_lowqpsk_crl_cut20[3U] = { {STV090x_QPSK_14, 15U, 63U, 14U, 63U, 45U, 47U, 45U, 31U, 61U, 62U},
        {STV090x_QPSK_13, 15U, 63U, 14U, 63U, 45U, 47U, 61U, 15U, 61U, 46U},
        {STV090x_QPSK_25, 31U, 63U, 30U, 63U, 61U, 31U, 61U, 62U, 61U, 46U}};
static struct stv090x_long_frame_crloop stv090x_s2_lowqpsk_crl_cut30[3U] = { {STV090x_QPSK_14, 12U, 60U, 11U, 60U, 42U, 44U, 42U, 28U, 58U, 59U},
        {STV090x_QPSK_13, 12U, 60U, 11U, 60U, 42U, 44U, 58U, 12U, 58U, 43U},
        {STV090x_QPSK_25, 28U, 60U, 27U, 60U, 58U, 28U, 58U, 59U, 58U, 43U}};
static struct stv090x_short_frame_crloop stv090x_s2_short_crl_cut20[4U] = { {STV090x_QPSK, 47U, 46U, 14U, 14U, 61U},
        {STV090x_8PSK, 62U, 14U, 45U, 13U, 60U},
        {STV090x_16APSK, 30U, 30U, 30U, 61U, 45U},
        {STV090x_32APSK, 30U, 30U, 30U, 61U, 45U}};
static struct stv090x_short_frame_crloop stv090x_s2_short_crl_cut30[4U] = { {STV090x_QPSK, 44U, 43U, 11U, 11U, 58U},
        {STV090x_8PSK, 59U, 11U, 42U, 10U, 57U},
        {STV090x_16APSK, 27U, 27U, 27U, 58U, 42U},
        {STV090x_32APSK, 27U, 27U, 27U, 58U, 42U}};
__inline static s32 comp2(s32 __x , s32 __width )
{
  {
  if (__width == 32) {
    return (__x);
  } else {
    return (1 << (__width + -1) <= __x ? __x - (1 << __width) : __x);
  }
}
}
static int stv090x_read_reg(struct stv090x_state *state , unsigned int reg )
{
  struct stv090x_config const *config ;
  int ret ;
  u8 b0[2U] ;
  u8 buf ;
  struct i2c_msg msg[2U] ;
  long tmp ;
  {
  config = state->config;
  b0[0] = (unsigned char )(reg >> 8);
  b0[1] = (unsigned char )reg;
  msg[0].addr = (unsigned short )config->address;
  msg[0].flags = 0U;
  msg[0].len = 2U;
  msg[0].buf = (__u8 *)(& b0);
  msg[1].addr = (unsigned short )config->address;
  msg[1].flags = 1U;
  msg[1].len = 1U;
  msg[1].buf = & buf;
  ret = i2c_transfer(state->i2c, (struct i2c_msg *)(& msg), 2);
  if (ret != 2) {
    if (ret != -512) {
      if (verbose != 0U && verbose != 0U) {
        printk("\v%s: Read error, Reg=[0x%02x], Status=%d\n", "stv090x_read_reg",
               reg, ret);
      } else
      if (verbose > 1U && verbose != 0U) {
        printk("\r%s: Read error, Reg=[0x%02x], Status=%d\n", "stv090x_read_reg",
               reg, ret);
      } else
      if (verbose > 2U && verbose != 0U) {
        printk("\016%s: Read error, Reg=[0x%02x], Status=%d\n", "stv090x_read_reg",
               reg, ret);
      } else
      if (verbose > 3U && verbose != 0U) {
        printk("\017%s: Read error, Reg=[0x%02x], Status=%d\n", "stv090x_read_reg",
               reg, ret);
      } else
      if (verbose != 0U) {
        printk("Read error, Reg=[0x%02x], Status=%d", reg, ret);
      } else {
      }
    } else {
    }
    return (ret < 0 ? ret : -121);
  } else {
  }
  tmp = ldv__builtin_expect(*(state->verbose) > 3U, 0L);
  if (tmp != 0L) {
    if (verbose != 0U && verbose != 0U) {
      printk("\v%s: Reg=[0x%02x], data=%02x\n", "stv090x_read_reg", reg, (int )buf);
    } else
    if (verbose > 1U && verbose != 0U) {
      printk("\r%s: Reg=[0x%02x], data=%02x\n", "stv090x_read_reg", reg, (int )buf);
    } else
    if (verbose > 2U && verbose != 0U) {
      printk("\016%s: Reg=[0x%02x], data=%02x\n", "stv090x_read_reg", reg, (int )buf);
    } else
    if (verbose > 3U && verbose != 0U) {
      printk("\017%s: Reg=[0x%02x], data=%02x\n", "stv090x_read_reg", reg, (int )buf);
    } else
    if (verbose != 0U) {
      printk("Reg=[0x%02x], data=%02x", reg, (int )buf);
    } else {
    }
  } else {
  }
  return ((int )buf);
}
}
static int stv090x_write_regs(struct stv090x_state *state , unsigned int reg , u8 *data ,
                              u32 count )
{
  struct stv090x_config const *config ;
  int ret ;
  u8 *buf ;
  unsigned long __lengthofbuf ;
  void *tmp ;
  struct i2c_msg i2c_msg ;
  size_t __len ;
  void *__ret ;
  int i ;
  long tmp___0 ;
  {
  config = state->config;
  __lengthofbuf = (unsigned long )((long )(count + 2U) + 0L);
  tmp = __builtin_alloca(sizeof(*buf) * __lengthofbuf);
  buf = (u8 *)tmp;
  i2c_msg.addr = (unsigned short )config->address;
  i2c_msg.flags = 0U;
  i2c_msg.len = (unsigned int )((unsigned short )count) + 2U;
  i2c_msg.buf = (__u8 *)(& buf);
  *(buf + 0) = (u8 )(reg >> 8);
  *(buf + 1) = (u8 )reg;
  __len = (size_t )count;
  __ret = memcpy((void *)(& buf) + 2U, (void const *)data, __len);
  tmp___0 = ldv__builtin_expect(*(state->verbose) > 3U, 0L);
  if (tmp___0 != 0L) {
    printk("\017%s [0x%04x]:", "stv090x_write_regs", reg);
    i = 0;
    goto ldv_24020;
    ldv_24019:
    printk(" %02x", (int )*(data + (unsigned long )i));
    i = i + 1;
    ldv_24020: ;
    if ((u32 )i < count) {
      goto ldv_24019;
    } else {
    }
    printk("\n");
  } else {
  }
  ret = i2c_transfer(state->i2c, & i2c_msg, 1);
  if (ret != 1) {
    if (ret != -512) {
      if (verbose != 0U && verbose != 0U) {
        printk("\v%s: Reg=[0x%04x], Data=[0x%02x ...], Count=%u, Status=%d\n", "stv090x_write_regs",
               reg, (int )*data, count, ret);
      } else
      if (verbose > 1U && verbose != 0U) {
        printk("\r%s: Reg=[0x%04x], Data=[0x%02x ...], Count=%u, Status=%d\n", "stv090x_write_regs",
               reg, (int )*data, count, ret);
      } else
      if (verbose > 2U && verbose != 0U) {
        printk("\016%s: Reg=[0x%04x], Data=[0x%02x ...], Count=%u, Status=%d\n", "stv090x_write_regs",
               reg, (int )*data, count, ret);
      } else
      if (verbose > 3U && verbose != 0U) {
        printk("\017%s: Reg=[0x%04x], Data=[0x%02x ...], Count=%u, Status=%d\n", "stv090x_write_regs",
               reg, (int )*data, count, ret);
      } else
      if (verbose != 0U) {
        printk("Reg=[0x%04x], Data=[0x%02x ...], Count=%u, Status=%d", reg, (int )*data,
               count, ret);
      } else {
      }
    } else {
    }
    return (ret < 0 ? ret : -121);
  } else {
  }
  return (0);
}
}
static int stv090x_write_reg(struct stv090x_state *state , unsigned int reg , u8 data )
{
  int tmp ;
  {
  tmp = stv090x_write_regs(state, reg, & data, 1U);
  return (tmp);
}
}
static int stv090x_i2c_gate_ctrl(struct stv090x_state *state , int enable )
{
  u32 reg ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  {
  if (enable != 0) {
    if ((unsigned long )(state->config)->tuner_i2c_lock != (unsigned long )((void (* )(struct dvb_frontend * ,
                                                                                                  int ))0)) {
      (*((state->config)->tuner_i2c_lock))(& state->frontend, 1);
    } else {
      ldv_mutex_lock_12(& (state->internal)->tuner_lock);
    }
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 61739U);
    tmp___1 = tmp;
  } else {
    tmp___0 = stv090x_read_reg(state, 61738U);
    tmp___1 = tmp___0;
  }
  reg = (u32 )tmp___1;
  if (enable != 0) {
    if (verbose != 0U && verbose > 3U) {
      printk("\v%s: Enable Gate\n", "stv090x_i2c_gate_ctrl");
    } else
    if (verbose > 1U && verbose > 3U) {
      printk("\r%s: Enable Gate\n", "stv090x_i2c_gate_ctrl");
    } else
    if (verbose > 2U && verbose > 3U) {
      printk("\016%s: Enable Gate\n", "stv090x_i2c_gate_ctrl");
    } else
    if (verbose > 3U && verbose > 3U) {
      printk("\017%s: Enable Gate\n", "stv090x_i2c_gate_ctrl");
    } else
    if (verbose > 3U) {
      printk("Enable Gate");
    } else {
    }
    reg = reg | 128U;
    if ((unsigned int )state->demod == 2U) {
      tmp___2 = stv090x_write_reg(state, 61739U, (int )((u8 )reg));
      tmp___4 = tmp___2 < 0;
    } else {
      tmp___3 = stv090x_write_reg(state, 61738U, (int )((u8 )reg));
      tmp___4 = tmp___3 < 0;
    }
    if (tmp___4) {
      goto err;
    } else {
    }
  } else {
    if (verbose != 0U && verbose > 3U) {
      printk("\v%s: Disable Gate\n", "stv090x_i2c_gate_ctrl");
    } else
    if (verbose > 1U && verbose > 3U) {
      printk("\r%s: Disable Gate\n", "stv090x_i2c_gate_ctrl");
    } else
    if (verbose > 2U && verbose > 3U) {
      printk("\016%s: Disable Gate\n", "stv090x_i2c_gate_ctrl");
    } else
    if (verbose > 3U && verbose > 3U) {
      printk("\017%s: Disable Gate\n", "stv090x_i2c_gate_ctrl");
    } else
    if (verbose > 3U) {
      printk("Disable Gate");
    } else {
    }
    reg = reg & 4294967167U;
    if ((unsigned int )state->demod == 2U) {
      tmp___5 = stv090x_write_reg(state, 61739U, (int )((u8 )reg));
      tmp___7 = tmp___5 < 0;
    } else {
      tmp___6 = stv090x_write_reg(state, 61738U, (int )((u8 )reg));
      tmp___7 = tmp___6 < 0;
    }
    if (tmp___7) {
      goto err;
    } else {
    }
  }
  if (enable == 0) {
    if ((unsigned long )(state->config)->tuner_i2c_lock != (unsigned long )((void (* )(struct dvb_frontend * ,
                                                                                                  int ))0)) {
      (*((state->config)->tuner_i2c_lock))(& state->frontend, 0);
    } else {
      ldv_mutex_unlock_13(& (state->internal)->tuner_lock);
    }
  } else {
  }
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_i2c_gate_ctrl");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_i2c_gate_ctrl");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_i2c_gate_ctrl");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_i2c_gate_ctrl");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  if ((unsigned long )(state->config)->tuner_i2c_lock != (unsigned long )((void (* )(struct dvb_frontend * ,
                                                                                                int ))0)) {
    (*((state->config)->tuner_i2c_lock))(& state->frontend, 0);
  } else {
    ldv_mutex_unlock_14(& (state->internal)->tuner_lock);
  }
  return (-1);
}
}
static void stv090x_get_lock_tmg(struct stv090x_state *state )
{
  {
  switch ((unsigned int )state->algo) {
  case 0U: ;
  if (verbose != 0U && verbose > 3U) {
    printk("\v%s: Blind Search\n", "stv090x_get_lock_tmg");
  } else
  if (verbose > 1U && verbose > 3U) {
    printk("\r%s: Blind Search\n", "stv090x_get_lock_tmg");
  } else
  if (verbose > 2U && verbose > 3U) {
    printk("\016%s: Blind Search\n", "stv090x_get_lock_tmg");
  } else
  if (verbose > 3U && verbose > 3U) {
    printk("\017%s: Blind Search\n", "stv090x_get_lock_tmg");
  } else
  if (verbose > 3U) {
    printk("Blind Search");
  } else {
  }
  if (state->srate <= 1500000U) {
    state->DemodTimeout = 1500;
    state->FecTimeout = 400;
  } else
  if (state->srate <= 5000000U) {
    state->DemodTimeout = 1000;
    state->FecTimeout = 300;
  } else {
    state->DemodTimeout = 700;
    state->FecTimeout = 100;
  }
  goto ldv_24039;
  case 1U: ;
  case 2U: ;
  default: ;
  if (verbose != 0U && verbose > 3U) {
    printk("\v%s: Normal Search\n", "stv090x_get_lock_tmg");
  } else
  if (verbose > 1U && verbose > 3U) {
    printk("\r%s: Normal Search\n", "stv090x_get_lock_tmg");
  } else
  if (verbose > 2U && verbose > 3U) {
    printk("\016%s: Normal Search\n", "stv090x_get_lock_tmg");
  } else
  if (verbose > 3U && verbose > 3U) {
    printk("\017%s: Normal Search\n", "stv090x_get_lock_tmg");
  } else
  if (verbose > 3U) {
    printk("Normal Search");
  } else {
  }
  if (state->srate <= 1000000U) {
    state->DemodTimeout = 4500;
    state->FecTimeout = 1700;
  } else
  if (state->srate <= 2000000U) {
    state->DemodTimeout = 2500;
    state->FecTimeout = 1100;
  } else
  if (state->srate <= 5000000U) {
    state->DemodTimeout = 1000;
    state->FecTimeout = 550;
  } else
  if (state->srate <= 10000000U) {
    state->DemodTimeout = 700;
    state->FecTimeout = 250;
  } else
  if (state->srate <= 20000000U) {
    state->DemodTimeout = 400;
    state->FecTimeout = 130;
  } else {
    state->DemodTimeout = 300;
    state->FecTimeout = 100;
  }
  goto ldv_24039;
  }
  ldv_24039: ;
  if ((unsigned int )state->algo == 2U) {
    state->DemodTimeout = state->DemodTimeout / 2;
  } else {
  }
  return;
}
}
static int stv090x_set_srate(struct stv090x_state *state , u32 srate )
{
  u32 sym ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  {
  if (srate > 60000000U) {
    sym = srate << 4;
    sym = sym / (u32 )((state->internal)->mclk >> 12);
  } else
  if (srate > 6000000U) {
    sym = srate << 6;
    sym = sym / (u32 )((state->internal)->mclk >> 10);
  } else {
    sym = srate << 9;
    sym = sym / (u32 )((state->internal)->mclk >> 7);
  }
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_write_reg(state, 62046U, (int )((u8 )(sym >> 8)) & 127);
    tmp___1 = tmp < 0;
  } else {
    tmp___0 = stv090x_write_reg(state, 62558U, (int )((u8 )(sym >> 8)) & 127);
    tmp___1 = tmp___0 < 0;
  }
  if (tmp___1) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_write_reg(state, 62047U, (int )((u8 )sym));
    tmp___4 = tmp___2 < 0;
  } else {
    tmp___3 = stv090x_write_reg(state, 62559U, (int )((u8 )sym));
    tmp___4 = tmp___3 < 0;
  }
  if (tmp___4) {
    goto err;
  } else {
  }
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_set_srate");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_set_srate");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_set_srate");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_set_srate");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_set_max_srate(struct stv090x_state *state , u32 clk , u32 srate )
{
  u32 sym ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  {
  srate = (srate / 100U) * 105U;
  if (srate > 60000000U) {
    sym = srate << 4;
    sym = sym / (u32 )((state->internal)->mclk >> 12);
  } else
  if (srate > 6000000U) {
    sym = srate << 6;
    sym = sym / (u32 )((state->internal)->mclk >> 10);
  } else {
    sym = srate << 9;
    sym = sym / (u32 )((state->internal)->mclk >> 7);
  }
  if (sym <= 32766U) {
    if ((unsigned int )state->demod == 2U) {
      tmp = stv090x_write_reg(state, 62048U, (int )((u8 )(sym >> 8)) & 127);
      tmp___1 = tmp < 0;
    } else {
      tmp___0 = stv090x_write_reg(state, 62560U, (int )((u8 )(sym >> 8)) & 127);
      tmp___1 = tmp___0 < 0;
    }
    if (tmp___1) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___2 = stv090x_write_reg(state, 62049U, (int )((u8 )sym));
      tmp___4 = tmp___2 < 0;
    } else {
      tmp___3 = stv090x_write_reg(state, 62561U, (int )((u8 )sym));
      tmp___4 = tmp___3 < 0;
    }
    if (tmp___4) {
      goto err;
    } else {
    }
  } else {
    if ((unsigned int )state->demod == 2U) {
      tmp___5 = stv090x_write_reg(state, 62048U, 127);
      tmp___7 = tmp___5 < 0;
    } else {
      tmp___6 = stv090x_write_reg(state, 62560U, 127);
      tmp___7 = tmp___6 < 0;
    }
    if (tmp___7) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___8 = stv090x_write_reg(state, 62049U, 255);
      tmp___10 = tmp___8 < 0;
    } else {
      tmp___9 = stv090x_write_reg(state, 62561U, 255);
      tmp___10 = tmp___9 < 0;
    }
    if (tmp___10) {
      goto err;
    } else {
    }
  }
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_set_max_srate");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_set_max_srate");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_set_max_srate");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_set_max_srate");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_set_min_srate(struct stv090x_state *state , u32 clk , u32 srate )
{
  u32 sym ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  {
  srate = (srate / 100U) * 95U;
  if (srate > 60000000U) {
    sym = srate << 4;
    sym = sym / (u32 )((state->internal)->mclk >> 12);
  } else
  if (srate > 6000000U) {
    sym = srate << 6;
    sym = sym / (u32 )((state->internal)->mclk >> 10);
  } else {
    sym = srate << 9;
    sym = sym / (u32 )((state->internal)->mclk >> 7);
  }
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_write_reg(state, 62050U, (int )((u8 )(sym >> 8)) & 127);
    tmp___1 = tmp < 0;
  } else {
    tmp___0 = stv090x_write_reg(state, 62562U, (int )((u8 )(sym >> 8)) & 127);
    tmp___1 = tmp___0 < 0;
  }
  if (tmp___1) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_write_reg(state, 62051U, (int )((u8 )sym));
    tmp___4 = tmp___2 < 0;
  } else {
    tmp___3 = stv090x_write_reg(state, 62563U, (int )((u8 )sym));
    tmp___4 = tmp___3 < 0;
  }
  if (tmp___4) {
    goto err;
  } else {
  }
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_set_min_srate");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_set_min_srate");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_set_min_srate");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_set_min_srate");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static u32 stv090x_car_width(u32 srate , enum stv090x_rolloff rolloff )
{
  u32 ro ;
  {
  switch ((unsigned int )rolloff) {
  case 2U:
  ro = 20U;
  goto ldv_24072;
  case 1U:
  ro = 25U;
  goto ldv_24072;
  case 0U: ;
  default:
  ro = 35U;
  goto ldv_24072;
  }
  ldv_24072: ;
  return ((srate * ro) / 100U + srate);
}
}
static int stv090x_set_vit_thacq(struct stv090x_state *state )
{
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  {
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_write_reg(state, 62260U, 150);
    tmp___1 = tmp < 0;
  } else {
    tmp___0 = stv090x_write_reg(state, 62772U, 150);
    tmp___1 = tmp___0 < 0;
  }
  if (tmp___1) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_write_reg(state, 62261U, 100);
    tmp___4 = tmp___2 < 0;
  } else {
    tmp___3 = stv090x_write_reg(state, 62773U, 100);
    tmp___4 = tmp___3 < 0;
  }
  if (tmp___4) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___5 = stv090x_write_reg(state, 62262U, 54);
    tmp___7 = tmp___5 < 0;
  } else {
    tmp___6 = stv090x_write_reg(state, 62774U, 54);
    tmp___7 = tmp___6 < 0;
  }
  if (tmp___7) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___8 = stv090x_write_reg(state, 62263U, 35);
    tmp___10 = tmp___8 < 0;
  } else {
    tmp___9 = stv090x_write_reg(state, 62775U, 35);
    tmp___10 = tmp___9 < 0;
  }
  if (tmp___10) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___11 = stv090x_write_reg(state, 62264U, 30);
    tmp___13 = tmp___11 < 0;
  } else {
    tmp___12 = stv090x_write_reg(state, 62776U, 30);
    tmp___13 = tmp___12 < 0;
  }
  if (tmp___13) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___14 = stv090x_write_reg(state, 62265U, 25);
    tmp___16 = tmp___14 < 0;
  } else {
    tmp___15 = stv090x_write_reg(state, 62777U, 25);
    tmp___16 = tmp___15 < 0;
  }
  if (tmp___16) {
    goto err;
  } else {
  }
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_set_vit_thacq");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_set_vit_thacq");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_set_vit_thacq");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_set_vit_thacq");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_set_vit_thtracq(struct stv090x_state *state )
{
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  {
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_write_reg(state, 62260U, 208);
    tmp___1 = tmp < 0;
  } else {
    tmp___0 = stv090x_write_reg(state, 62772U, 208);
    tmp___1 = tmp___0 < 0;
  }
  if (tmp___1) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_write_reg(state, 62261U, 125);
    tmp___4 = tmp___2 < 0;
  } else {
    tmp___3 = stv090x_write_reg(state, 62773U, 125);
    tmp___4 = tmp___3 < 0;
  }
  if (tmp___4) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___5 = stv090x_write_reg(state, 62262U, 83);
    tmp___7 = tmp___5 < 0;
  } else {
    tmp___6 = stv090x_write_reg(state, 62774U, 83);
    tmp___7 = tmp___6 < 0;
  }
  if (tmp___7) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___8 = stv090x_write_reg(state, 62263U, 47);
    tmp___10 = tmp___8 < 0;
  } else {
    tmp___9 = stv090x_write_reg(state, 62775U, 47);
    tmp___10 = tmp___9 < 0;
  }
  if (tmp___10) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___11 = stv090x_write_reg(state, 62264U, 36);
    tmp___13 = tmp___11 < 0;
  } else {
    tmp___12 = stv090x_write_reg(state, 62776U, 36);
    tmp___13 = tmp___12 < 0;
  }
  if (tmp___13) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___14 = stv090x_write_reg(state, 62265U, 31);
    tmp___16 = tmp___14 < 0;
  } else {
    tmp___15 = stv090x_write_reg(state, 62777U, 31);
    tmp___16 = tmp___15 < 0;
  }
  if (tmp___16) {
    goto err;
  } else {
  }
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_set_vit_thtracq");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_set_vit_thtracq");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_set_vit_thtracq");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_set_vit_thtracq");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_set_viterbi(struct stv090x_state *state )
{
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  int tmp___23 ;
  int tmp___24 ;
  int tmp___25 ;
  int tmp___26 ;
  int tmp___27 ;
  int tmp___28 ;
  int tmp___29 ;
  int tmp___30 ;
  int tmp___31 ;
  int tmp___32 ;
  int tmp___33 ;
  int tmp___34 ;
  int tmp___35 ;
  int tmp___36 ;
  int tmp___37 ;
  int tmp___38 ;
  int tmp___39 ;
  int tmp___40 ;
  {
  switch ((unsigned int )state->search_mode) {
  case 3U: ;
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_write_reg(state, 62259U, 16);
    tmp___1 = tmp < 0;
  } else {
    tmp___0 = stv090x_write_reg(state, 62771U, 16);
    tmp___1 = tmp___0 < 0;
  }
  if (tmp___1) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_write_reg(state, 62268U, 63);
    tmp___4 = tmp___2 < 0;
  } else {
    tmp___3 = stv090x_write_reg(state, 62780U, 63);
    tmp___4 = tmp___3 < 0;
  }
  if (tmp___4) {
    goto err;
  } else {
  }
  goto ldv_24091;
  case 1U: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___5 = stv090x_write_reg(state, 62259U, 0);
    tmp___7 = tmp___5 < 0;
  } else {
    tmp___6 = stv090x_write_reg(state, 62771U, 0);
    tmp___7 = tmp___6 < 0;
  }
  if (tmp___7) {
    goto err;
  } else {
  }
  switch ((unsigned int )state->fec) {
  case 0U: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___8 = stv090x_write_reg(state, 62268U, 1);
    tmp___10 = tmp___8 < 0;
  } else {
    tmp___9 = stv090x_write_reg(state, 62780U, 1);
    tmp___10 = tmp___9 < 0;
  }
  if (tmp___10) {
    goto err;
  } else {
  }
  goto ldv_24094;
  case 1U: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___11 = stv090x_write_reg(state, 62268U, 2);
    tmp___13 = tmp___11 < 0;
  } else {
    tmp___12 = stv090x_write_reg(state, 62780U, 2);
    tmp___13 = tmp___12 < 0;
  }
  if (tmp___13) {
    goto err;
  } else {
  }
  goto ldv_24094;
  case 2U: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___14 = stv090x_write_reg(state, 62268U, 4);
    tmp___16 = tmp___14 < 0;
  } else {
    tmp___15 = stv090x_write_reg(state, 62780U, 4);
    tmp___16 = tmp___15 < 0;
  }
  if (tmp___16) {
    goto err;
  } else {
  }
  goto ldv_24094;
  case 4U: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___17 = stv090x_write_reg(state, 62268U, 8);
    tmp___19 = tmp___17 < 0;
  } else {
    tmp___18 = stv090x_write_reg(state, 62780U, 8);
    tmp___19 = tmp___18 < 0;
  }
  if (tmp___19) {
    goto err;
  } else {
  }
  goto ldv_24094;
  case 6U: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___20 = stv090x_write_reg(state, 62268U, 32);
    tmp___22 = tmp___20 < 0;
  } else {
    tmp___21 = stv090x_write_reg(state, 62780U, 32);
    tmp___22 = tmp___21 < 0;
  }
  if (tmp___22) {
    goto err;
  } else {
  }
  goto ldv_24094;
  default: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___23 = stv090x_write_reg(state, 62268U, 47);
    tmp___25 = tmp___23 < 0;
  } else {
    tmp___24 = stv090x_write_reg(state, 62780U, 47);
    tmp___25 = tmp___24 < 0;
  }
  if (tmp___25) {
    goto err;
  } else {
  }
  goto ldv_24094;
  }
  ldv_24094: ;
  goto ldv_24091;
  case 0U: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___26 = stv090x_write_reg(state, 62259U, 128);
    tmp___28 = tmp___26 < 0;
  } else {
    tmp___27 = stv090x_write_reg(state, 62771U, 128);
    tmp___28 = tmp___27 < 0;
  }
  if (tmp___28) {
    goto err;
  } else {
  }
  switch ((unsigned int )state->fec) {
  case 0U: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___29 = stv090x_write_reg(state, 62268U, 1);
    tmp___31 = tmp___29 < 0;
  } else {
    tmp___30 = stv090x_write_reg(state, 62780U, 1);
    tmp___31 = tmp___30 < 0;
  }
  if (tmp___31) {
    goto err;
  } else {
  }
  goto ldv_24102;
  case 1U: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___32 = stv090x_write_reg(state, 62268U, 2);
    tmp___34 = tmp___32 < 0;
  } else {
    tmp___33 = stv090x_write_reg(state, 62780U, 2);
    tmp___34 = tmp___33 < 0;
  }
  if (tmp___34) {
    goto err;
  } else {
  }
  goto ldv_24102;
  case 5U: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___35 = stv090x_write_reg(state, 62268U, 16);
    tmp___37 = tmp___35 < 0;
  } else {
    tmp___36 = stv090x_write_reg(state, 62780U, 16);
    tmp___37 = tmp___36 < 0;
  }
  if (tmp___37) {
    goto err;
  } else {
  }
  goto ldv_24102;
  default: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___38 = stv090x_write_reg(state, 62268U, 19);
    tmp___40 = tmp___38 < 0;
  } else {
    tmp___39 = stv090x_write_reg(state, 62780U, 19);
    tmp___40 = tmp___39 < 0;
  }
  if (tmp___40) {
    goto err;
  } else {
  }
  goto ldv_24102;
  }
  ldv_24102: ;
  goto ldv_24091;
  default: ;
  goto ldv_24091;
  }
  ldv_24091: ;
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_set_viterbi");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_set_viterbi");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_set_viterbi");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_set_viterbi");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_stop_modcod(struct stv090x_state *state )
{
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  int tmp___23 ;
  int tmp___24 ;
  int tmp___25 ;
  int tmp___26 ;
  int tmp___27 ;
  int tmp___28 ;
  int tmp___29 ;
  int tmp___30 ;
  int tmp___31 ;
  int tmp___32 ;
  int tmp___33 ;
  int tmp___34 ;
  int tmp___35 ;
  int tmp___36 ;
  int tmp___37 ;
  int tmp___38 ;
  int tmp___39 ;
  int tmp___40 ;
  int tmp___41 ;
  int tmp___42 ;
  int tmp___43 ;
  int tmp___44 ;
  int tmp___45 ;
  int tmp___46 ;
  {
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_write_reg(state, 62128U, 255);
    tmp___1 = tmp < 0;
  } else {
    tmp___0 = stv090x_write_reg(state, 62640U, 255);
    tmp___1 = tmp___0 < 0;
  }
  if (tmp___1) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_write_reg(state, 62129U, 255);
    tmp___4 = tmp___2 < 0;
  } else {
    tmp___3 = stv090x_write_reg(state, 62641U, 255);
    tmp___4 = tmp___3 < 0;
  }
  if (tmp___4) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___5 = stv090x_write_reg(state, 62130U, 255);
    tmp___7 = tmp___5 < 0;
  } else {
    tmp___6 = stv090x_write_reg(state, 62642U, 255);
    tmp___7 = tmp___6 < 0;
  }
  if (tmp___7) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___8 = stv090x_write_reg(state, 62131U, 255);
    tmp___10 = tmp___8 < 0;
  } else {
    tmp___9 = stv090x_write_reg(state, 62643U, 255);
    tmp___10 = tmp___9 < 0;
  }
  if (tmp___10) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___11 = stv090x_write_reg(state, 62132U, 255);
    tmp___13 = tmp___11 < 0;
  } else {
    tmp___12 = stv090x_write_reg(state, 62644U, 255);
    tmp___13 = tmp___12 < 0;
  }
  if (tmp___13) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___14 = stv090x_write_reg(state, 62133U, 255);
    tmp___16 = tmp___14 < 0;
  } else {
    tmp___15 = stv090x_write_reg(state, 62645U, 255);
    tmp___16 = tmp___15 < 0;
  }
  if (tmp___16) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___17 = stv090x_write_reg(state, 62134U, 255);
    tmp___19 = tmp___17 < 0;
  } else {
    tmp___18 = stv090x_write_reg(state, 62646U, 255);
    tmp___19 = tmp___18 < 0;
  }
  if (tmp___19) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___20 = stv090x_write_reg(state, 62135U, 255);
    tmp___22 = tmp___20 < 0;
  } else {
    tmp___21 = stv090x_write_reg(state, 62647U, 255);
    tmp___22 = tmp___21 < 0;
  }
  if (tmp___22) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___23 = stv090x_write_reg(state, 62136U, 255);
    tmp___25 = tmp___23 < 0;
  } else {
    tmp___24 = stv090x_write_reg(state, 62648U, 255);
    tmp___25 = tmp___24 < 0;
  }
  if (tmp___25) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___26 = stv090x_write_reg(state, 62137U, 255);
    tmp___28 = tmp___26 < 0;
  } else {
    tmp___27 = stv090x_write_reg(state, 62649U, 255);
    tmp___28 = tmp___27 < 0;
  }
  if (tmp___28) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___29 = stv090x_write_reg(state, 62138U, 255);
    tmp___31 = tmp___29 < 0;
  } else {
    tmp___30 = stv090x_write_reg(state, 62650U, 255);
    tmp___31 = tmp___30 < 0;
  }
  if (tmp___31) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___32 = stv090x_write_reg(state, 62139U, 255);
    tmp___34 = tmp___32 < 0;
  } else {
    tmp___33 = stv090x_write_reg(state, 62651U, 255);
    tmp___34 = tmp___33 < 0;
  }
  if (tmp___34) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___35 = stv090x_write_reg(state, 62140U, 255);
    tmp___37 = tmp___35 < 0;
  } else {
    tmp___36 = stv090x_write_reg(state, 62652U, 255);
    tmp___37 = tmp___36 < 0;
  }
  if (tmp___37) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___38 = stv090x_write_reg(state, 62141U, 255);
    tmp___40 = tmp___38 < 0;
  } else {
    tmp___39 = stv090x_write_reg(state, 62653U, 255);
    tmp___40 = tmp___39 < 0;
  }
  if (tmp___40) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___41 = stv090x_write_reg(state, 62142U, 255);
    tmp___43 = tmp___41 < 0;
  } else {
    tmp___42 = stv090x_write_reg(state, 62654U, 255);
    tmp___43 = tmp___42 < 0;
  }
  if (tmp___43) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___44 = stv090x_write_reg(state, 62143U, 255);
    tmp___46 = tmp___44 < 0;
  } else {
    tmp___45 = stv090x_write_reg(state, 62655U, 255);
    tmp___46 = tmp___45 < 0;
  }
  if (tmp___46) {
    goto err;
  } else {
  }
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_stop_modcod");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_stop_modcod");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_stop_modcod");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_stop_modcod");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_activate_modcod(struct stv090x_state *state )
{
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  int tmp___23 ;
  int tmp___24 ;
  int tmp___25 ;
  int tmp___26 ;
  int tmp___27 ;
  int tmp___28 ;
  int tmp___29 ;
  int tmp___30 ;
  int tmp___31 ;
  int tmp___32 ;
  int tmp___33 ;
  int tmp___34 ;
  int tmp___35 ;
  int tmp___36 ;
  int tmp___37 ;
  int tmp___38 ;
  int tmp___39 ;
  int tmp___40 ;
  int tmp___41 ;
  int tmp___42 ;
  int tmp___43 ;
  int tmp___44 ;
  int tmp___45 ;
  int tmp___46 ;
  {
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_write_reg(state, 62128U, 255);
    tmp___1 = tmp < 0;
  } else {
    tmp___0 = stv090x_write_reg(state, 62640U, 255);
    tmp___1 = tmp___0 < 0;
  }
  if (tmp___1) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_write_reg(state, 62129U, 252);
    tmp___4 = tmp___2 < 0;
  } else {
    tmp___3 = stv090x_write_reg(state, 62641U, 252);
    tmp___4 = tmp___3 < 0;
  }
  if (tmp___4) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___5 = stv090x_write_reg(state, 62130U, 204);
    tmp___7 = tmp___5 < 0;
  } else {
    tmp___6 = stv090x_write_reg(state, 62642U, 204);
    tmp___7 = tmp___6 < 0;
  }
  if (tmp___7) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___8 = stv090x_write_reg(state, 62131U, 204);
    tmp___10 = tmp___8 < 0;
  } else {
    tmp___9 = stv090x_write_reg(state, 62643U, 204);
    tmp___10 = tmp___9 < 0;
  }
  if (tmp___10) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___11 = stv090x_write_reg(state, 62132U, 204);
    tmp___13 = tmp___11 < 0;
  } else {
    tmp___12 = stv090x_write_reg(state, 62644U, 204);
    tmp___13 = tmp___12 < 0;
  }
  if (tmp___13) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___14 = stv090x_write_reg(state, 62133U, 204);
    tmp___16 = tmp___14 < 0;
  } else {
    tmp___15 = stv090x_write_reg(state, 62645U, 204);
    tmp___16 = tmp___15 < 0;
  }
  if (tmp___16) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___17 = stv090x_write_reg(state, 62134U, 204);
    tmp___19 = tmp___17 < 0;
  } else {
    tmp___18 = stv090x_write_reg(state, 62646U, 204);
    tmp___19 = tmp___18 < 0;
  }
  if (tmp___19) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___20 = stv090x_write_reg(state, 62135U, 204);
    tmp___22 = tmp___20 < 0;
  } else {
    tmp___21 = stv090x_write_reg(state, 62647U, 204);
    tmp___22 = tmp___21 < 0;
  }
  if (tmp___22) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___23 = stv090x_write_reg(state, 62136U, 204);
    tmp___25 = tmp___23 < 0;
  } else {
    tmp___24 = stv090x_write_reg(state, 62648U, 204);
    tmp___25 = tmp___24 < 0;
  }
  if (tmp___25) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___26 = stv090x_write_reg(state, 62137U, 204);
    tmp___28 = tmp___26 < 0;
  } else {
    tmp___27 = stv090x_write_reg(state, 62649U, 204);
    tmp___28 = tmp___27 < 0;
  }
  if (tmp___28) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___29 = stv090x_write_reg(state, 62138U, 204);
    tmp___31 = tmp___29 < 0;
  } else {
    tmp___30 = stv090x_write_reg(state, 62650U, 204);
    tmp___31 = tmp___30 < 0;
  }
  if (tmp___31) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___32 = stv090x_write_reg(state, 62139U, 204);
    tmp___34 = tmp___32 < 0;
  } else {
    tmp___33 = stv090x_write_reg(state, 62651U, 204);
    tmp___34 = tmp___33 < 0;
  }
  if (tmp___34) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___35 = stv090x_write_reg(state, 62140U, 204);
    tmp___37 = tmp___35 < 0;
  } else {
    tmp___36 = stv090x_write_reg(state, 62652U, 204);
    tmp___37 = tmp___36 < 0;
  }
  if (tmp___37) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___38 = stv090x_write_reg(state, 62141U, 204);
    tmp___40 = tmp___38 < 0;
  } else {
    tmp___39 = stv090x_write_reg(state, 62653U, 204);
    tmp___40 = tmp___39 < 0;
  }
  if (tmp___40) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___41 = stv090x_write_reg(state, 62142U, 204);
    tmp___43 = tmp___41 < 0;
  } else {
    tmp___42 = stv090x_write_reg(state, 62654U, 204);
    tmp___43 = tmp___42 < 0;
  }
  if (tmp___43) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___44 = stv090x_write_reg(state, 62143U, 207);
    tmp___46 = tmp___44 < 0;
  } else {
    tmp___45 = stv090x_write_reg(state, 62655U, 207);
    tmp___46 = tmp___45 < 0;
  }
  if (tmp___46) {
    goto err;
  } else {
  }
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_activate_modcod");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_activate_modcod");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_activate_modcod");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_activate_modcod");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_activate_modcod_single(struct stv090x_state *state )
{
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  int tmp___23 ;
  int tmp___24 ;
  int tmp___25 ;
  int tmp___26 ;
  int tmp___27 ;
  int tmp___28 ;
  int tmp___29 ;
  int tmp___30 ;
  int tmp___31 ;
  int tmp___32 ;
  int tmp___33 ;
  int tmp___34 ;
  int tmp___35 ;
  int tmp___36 ;
  int tmp___37 ;
  int tmp___38 ;
  int tmp___39 ;
  int tmp___40 ;
  int tmp___41 ;
  int tmp___42 ;
  int tmp___43 ;
  int tmp___44 ;
  int tmp___45 ;
  int tmp___46 ;
  {
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_write_reg(state, 62128U, 255);
    tmp___1 = tmp < 0;
  } else {
    tmp___0 = stv090x_write_reg(state, 62640U, 255);
    tmp___1 = tmp___0 < 0;
  }
  if (tmp___1) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_write_reg(state, 62129U, 240);
    tmp___4 = tmp___2 < 0;
  } else {
    tmp___3 = stv090x_write_reg(state, 62641U, 240);
    tmp___4 = tmp___3 < 0;
  }
  if (tmp___4) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___5 = stv090x_write_reg(state, 62130U, 0);
    tmp___7 = tmp___5 < 0;
  } else {
    tmp___6 = stv090x_write_reg(state, 62642U, 0);
    tmp___7 = tmp___6 < 0;
  }
  if (tmp___7) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___8 = stv090x_write_reg(state, 62131U, 0);
    tmp___10 = tmp___8 < 0;
  } else {
    tmp___9 = stv090x_write_reg(state, 62643U, 0);
    tmp___10 = tmp___9 < 0;
  }
  if (tmp___10) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___11 = stv090x_write_reg(state, 62132U, 0);
    tmp___13 = tmp___11 < 0;
  } else {
    tmp___12 = stv090x_write_reg(state, 62644U, 0);
    tmp___13 = tmp___12 < 0;
  }
  if (tmp___13) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___14 = stv090x_write_reg(state, 62133U, 0);
    tmp___16 = tmp___14 < 0;
  } else {
    tmp___15 = stv090x_write_reg(state, 62645U, 0);
    tmp___16 = tmp___15 < 0;
  }
  if (tmp___16) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___17 = stv090x_write_reg(state, 62134U, 0);
    tmp___19 = tmp___17 < 0;
  } else {
    tmp___18 = stv090x_write_reg(state, 62646U, 0);
    tmp___19 = tmp___18 < 0;
  }
  if (tmp___19) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___20 = stv090x_write_reg(state, 62135U, 0);
    tmp___22 = tmp___20 < 0;
  } else {
    tmp___21 = stv090x_write_reg(state, 62647U, 0);
    tmp___22 = tmp___21 < 0;
  }
  if (tmp___22) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___23 = stv090x_write_reg(state, 62136U, 0);
    tmp___25 = tmp___23 < 0;
  } else {
    tmp___24 = stv090x_write_reg(state, 62648U, 0);
    tmp___25 = tmp___24 < 0;
  }
  if (tmp___25) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___26 = stv090x_write_reg(state, 62137U, 0);
    tmp___28 = tmp___26 < 0;
  } else {
    tmp___27 = stv090x_write_reg(state, 62649U, 0);
    tmp___28 = tmp___27 < 0;
  }
  if (tmp___28) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___29 = stv090x_write_reg(state, 62138U, 0);
    tmp___31 = tmp___29 < 0;
  } else {
    tmp___30 = stv090x_write_reg(state, 62650U, 0);
    tmp___31 = tmp___30 < 0;
  }
  if (tmp___31) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___32 = stv090x_write_reg(state, 62139U, 0);
    tmp___34 = tmp___32 < 0;
  } else {
    tmp___33 = stv090x_write_reg(state, 62651U, 0);
    tmp___34 = tmp___33 < 0;
  }
  if (tmp___34) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___35 = stv090x_write_reg(state, 62140U, 0);
    tmp___37 = tmp___35 < 0;
  } else {
    tmp___36 = stv090x_write_reg(state, 62652U, 0);
    tmp___37 = tmp___36 < 0;
  }
  if (tmp___37) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___38 = stv090x_write_reg(state, 62141U, 0);
    tmp___40 = tmp___38 < 0;
  } else {
    tmp___39 = stv090x_write_reg(state, 62653U, 0);
    tmp___40 = tmp___39 < 0;
  }
  if (tmp___40) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___41 = stv090x_write_reg(state, 62142U, 0);
    tmp___43 = tmp___41 < 0;
  } else {
    tmp___42 = stv090x_write_reg(state, 62654U, 0);
    tmp___43 = tmp___42 < 0;
  }
  if (tmp___43) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___44 = stv090x_write_reg(state, 62143U, 15);
    tmp___46 = tmp___44 < 0;
  } else {
    tmp___45 = stv090x_write_reg(state, 62655U, 15);
    tmp___46 = tmp___45 < 0;
  }
  if (tmp___46) {
    goto err;
  } else {
  }
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_activate_modcod_single");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_activate_modcod_single");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_activate_modcod_single");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_activate_modcod_single");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_vitclk_ctl(struct stv090x_state *state , int enable )
{
  u32 reg ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  {
  switch ((unsigned int )state->demod) {
  case 1U:
  ldv_mutex_lock_15(& (state->internal)->demod_lock);
  tmp = stv090x_read_reg(state, 61891U);
  reg = (u32 )tmp;
  reg = (reg & 4294967293U) | (u32 )(enable << 1);
  tmp___0 = stv090x_write_reg(state, 61891U, (int )((u8 )reg));
  if (tmp___0 < 0) {
    goto err;
  } else {
  }
  ldv_mutex_unlock_16(& (state->internal)->demod_lock);
  goto ldv_24130;
  case 2U:
  ldv_mutex_lock_17(& (state->internal)->demod_lock);
  tmp___1 = stv090x_read_reg(state, 61891U);
  reg = (u32 )tmp___1;
  reg = (reg & 4294967291U) | (u32 )(enable << 2);
  tmp___2 = stv090x_write_reg(state, 61891U, (int )((u8 )reg));
  if (tmp___2 < 0) {
    goto err;
  } else {
  }
  ldv_mutex_unlock_18(& (state->internal)->demod_lock);
  goto ldv_24130;
  default: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: Wrong demodulator!\n", "stv090x_vitclk_ctl");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: Wrong demodulator!\n", "stv090x_vitclk_ctl");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: Wrong demodulator!\n", "stv090x_vitclk_ctl");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: Wrong demodulator!\n", "stv090x_vitclk_ctl");
  } else
  if (verbose != 0U) {
    printk("Wrong demodulator!");
  } else {
  }
  goto ldv_24130;
  }
  ldv_24130: ;
  return (0);
  err:
  ldv_mutex_unlock_19(& (state->internal)->demod_lock);
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_vitclk_ctl");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_vitclk_ctl");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_vitclk_ctl");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_vitclk_ctl");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_dvbs_track_crl(struct stv090x_state *state )
{
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  {
  if ((state->internal)->dev_ver > 47U) {
    if (state->srate > 14999999U) {
      if ((unsigned int )state->demod == 2U) {
        tmp = stv090x_write_reg(state, 62009U, 43);
        tmp___1 = tmp < 0;
      } else {
        tmp___0 = stv090x_write_reg(state, 62521U, 43);
        tmp___1 = tmp___0 < 0;
      }
      if (tmp___1) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___2 = stv090x_write_reg(state, 62010U, 26);
        tmp___4 = tmp___2 < 0;
      } else {
        tmp___3 = stv090x_write_reg(state, 62522U, 26);
        tmp___4 = tmp___3 < 0;
      }
      if (tmp___4) {
        goto err;
      } else {
      }
    } else
    if (state->srate > 6999999U && state->srate <= 14999999U) {
      if ((unsigned int )state->demod == 2U) {
        tmp___5 = stv090x_write_reg(state, 62009U, 12);
        tmp___7 = tmp___5 < 0;
      } else {
        tmp___6 = stv090x_write_reg(state, 62521U, 12);
        tmp___7 = tmp___6 < 0;
      }
      if (tmp___7) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___8 = stv090x_write_reg(state, 62010U, 27);
        tmp___10 = tmp___8 < 0;
      } else {
        tmp___9 = stv090x_write_reg(state, 62522U, 27);
        tmp___10 = tmp___9 < 0;
      }
      if (tmp___10) {
        goto err;
      } else {
      }
    } else
    if (state->srate <= 6999999U) {
      if ((unsigned int )state->demod == 2U) {
        tmp___11 = stv090x_write_reg(state, 62009U, 44);
        tmp___13 = tmp___11 < 0;
      } else {
        tmp___12 = stv090x_write_reg(state, 62521U, 44);
        tmp___13 = tmp___12 < 0;
      }
      if (tmp___13) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___14 = stv090x_write_reg(state, 62010U, 28);
        tmp___16 = tmp___14 < 0;
      } else {
        tmp___15 = stv090x_write_reg(state, 62522U, 28);
        tmp___16 = tmp___15 < 0;
      }
      if (tmp___16) {
        goto err;
      } else {
      }
    } else {
      if ((unsigned int )state->demod == 2U) {
        tmp___17 = stv090x_write_reg(state, 62009U, 26);
        tmp___19 = tmp___17 < 0;
      } else {
        tmp___18 = stv090x_write_reg(state, 62521U, 26);
        tmp___19 = tmp___18 < 0;
      }
      if (tmp___19) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___20 = stv090x_write_reg(state, 62010U, 9);
        tmp___22 = tmp___20 < 0;
      } else {
        tmp___21 = stv090x_write_reg(state, 62522U, 9);
        tmp___22 = tmp___21 < 0;
      }
      if (tmp___22) {
        goto err;
      } else {
      }
    }
  } else {
  }
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_dvbs_track_crl");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_dvbs_track_crl");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_dvbs_track_crl");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_dvbs_track_crl");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_delivery_search(struct stv090x_state *state )
{
  u32 reg ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  int tmp___23 ;
  int tmp___24 ;
  int tmp___25 ;
  int tmp___26 ;
  int tmp___27 ;
  int tmp___28 ;
  int tmp___29 ;
  int tmp___30 ;
  int tmp___31 ;
  int tmp___32 ;
  int tmp___33 ;
  int tmp___34 ;
  int tmp___35 ;
  int tmp___36 ;
  int tmp___37 ;
  int tmp___38 ;
  int tmp___39 ;
  int tmp___40 ;
  int tmp___41 ;
  int tmp___42 ;
  int tmp___43 ;
  int tmp___44 ;
  int tmp___45 ;
  int tmp___46 ;
  int tmp___47 ;
  int tmp___48 ;
  int tmp___49 ;
  int tmp___50 ;
  int tmp___51 ;
  int tmp___52 ;
  int tmp___53 ;
  int tmp___54 ;
  int tmp___55 ;
  int tmp___56 ;
  int tmp___57 ;
  {
  switch ((unsigned int )state->search_mode) {
  case 1U: ;
  case 0U: ;
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 61972U);
    tmp___1 = tmp;
  } else {
    tmp___0 = stv090x_read_reg(state, 62484U);
    tmp___1 = tmp___0;
  }
  reg = (u32 )tmp___1;
  reg = reg | 64U;
  reg = reg & 4294967167U;
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_write_reg(state, 61972U, (int )((u8 )reg));
    tmp___4 = tmp___2 < 0;
  } else {
    tmp___3 = stv090x_write_reg(state, 62484U, (int )((u8 )reg));
    tmp___4 = tmp___3 < 0;
  }
  if (tmp___4) {
    goto err;
  } else {
  }
  tmp___5 = stv090x_vitclk_ctl(state, 0);
  if (tmp___5 < 0) {
    goto err;
  } else {
  }
  tmp___6 = stv090x_dvbs_track_crl(state);
  if (tmp___6 < 0) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___7 = stv090x_write_reg(state, 62096U, 34);
    tmp___9 = tmp___7 < 0;
  } else {
    tmp___8 = stv090x_write_reg(state, 62608U, 34);
    tmp___9 = tmp___8 < 0;
  }
  if (tmp___9) {
    goto err;
  } else {
  }
  tmp___10 = stv090x_set_vit_thacq(state);
  if (tmp___10 < 0) {
    goto err;
  } else {
  }
  tmp___11 = stv090x_set_viterbi(state);
  if (tmp___11 < 0) {
    goto err;
  } else {
  }
  goto ldv_24146;
  case 2U: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___12 = stv090x_read_reg(state, 61972U);
    tmp___14 = tmp___12;
  } else {
    tmp___13 = stv090x_read_reg(state, 62484U);
    tmp___14 = tmp___13;
  }
  reg = (u32 )tmp___14;
  reg = reg & 4294967231U;
  reg = reg & 4294967167U;
  if ((unsigned int )state->demod == 2U) {
    tmp___15 = stv090x_write_reg(state, 61972U, (int )((u8 )reg));
    tmp___17 = tmp___15 < 0;
  } else {
    tmp___16 = stv090x_write_reg(state, 62484U, (int )((u8 )reg));
    tmp___17 = tmp___16 < 0;
  }
  if (tmp___17) {
    goto err;
  } else {
  }
  reg = reg | 64U;
  reg = reg | 128U;
  if ((unsigned int )state->demod == 2U) {
    tmp___18 = stv090x_write_reg(state, 61972U, (int )((u8 )reg));
    tmp___20 = tmp___18 < 0;
  } else {
    tmp___19 = stv090x_write_reg(state, 62484U, (int )((u8 )reg));
    tmp___20 = tmp___19 < 0;
  }
  if (tmp___20) {
    goto err;
  } else {
  }
  tmp___21 = stv090x_vitclk_ctl(state, 1);
  if (tmp___21 < 0) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___22 = stv090x_write_reg(state, 62009U, 26);
    tmp___24 = tmp___22 < 0;
  } else {
    tmp___23 = stv090x_write_reg(state, 62521U, 26);
    tmp___24 = tmp___23 < 0;
  }
  if (tmp___24) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___25 = stv090x_write_reg(state, 62010U, 9);
    tmp___27 = tmp___25 < 0;
  } else {
    tmp___26 = stv090x_write_reg(state, 62522U, 9);
    tmp___27 = tmp___26 < 0;
  }
  if (tmp___27) {
    goto err;
  } else {
  }
  if ((state->internal)->dev_ver <= 32U) {
    if ((unsigned int )state->demod == 2U) {
      tmp___31 = stv090x_write_reg(state, 62096U, 38);
      tmp___33 = tmp___31 < 0;
    } else {
      tmp___32 = stv090x_write_reg(state, 62608U, 38);
      tmp___33 = tmp___32 < 0;
    }
    if (tmp___33) {
      goto err;
    } else {
      if ((unsigned int )state->demod == 2U) {
        tmp___28 = stv090x_write_reg(state, 62096U, 102);
        tmp___30 = tmp___28 < 0;
      } else {
        tmp___29 = stv090x_write_reg(state, 62608U, 102);
        tmp___30 = tmp___29 < 0;
      }
      if (tmp___30) {
        goto err;
      } else {
      }
    }
  } else {
  }
  if ((unsigned int )state->demod_mode != 1U) {
    tmp___35 = stv090x_activate_modcod(state);
    if (tmp___35 < 0) {
      goto err;
    } else {
      tmp___34 = stv090x_activate_modcod_single(state);
      if (tmp___34 < 0) {
        goto err;
      } else {
      }
    }
  } else {
  }
  tmp___36 = stv090x_set_vit_thtracq(state);
  if (tmp___36 < 0) {
    goto err;
  } else {
  }
  goto ldv_24146;
  case 3U: ;
  default: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___37 = stv090x_read_reg(state, 61972U);
    tmp___39 = tmp___37;
  } else {
    tmp___38 = stv090x_read_reg(state, 62484U);
    tmp___39 = tmp___38;
  }
  reg = (u32 )tmp___39;
  reg = reg & 4294967231U;
  reg = reg & 4294967167U;
  if ((unsigned int )state->demod == 2U) {
    tmp___40 = stv090x_write_reg(state, 61972U, (int )((u8 )reg));
    tmp___42 = tmp___40 < 0;
  } else {
    tmp___41 = stv090x_write_reg(state, 62484U, (int )((u8 )reg));
    tmp___42 = tmp___41 < 0;
  }
  if (tmp___42) {
    goto err;
  } else {
  }
  reg = reg | 64U;
  reg = reg | 128U;
  if ((unsigned int )state->demod == 2U) {
    tmp___43 = stv090x_write_reg(state, 61972U, (int )((u8 )reg));
    tmp___45 = tmp___43 < 0;
  } else {
    tmp___44 = stv090x_write_reg(state, 62484U, (int )((u8 )reg));
    tmp___45 = tmp___44 < 0;
  }
  if (tmp___45) {
    goto err;
  } else {
  }
  tmp___46 = stv090x_vitclk_ctl(state, 0);
  if (tmp___46 < 0) {
    goto err;
  } else {
  }
  tmp___47 = stv090x_dvbs_track_crl(state);
  if (tmp___47 < 0) {
    goto err;
  } else {
  }
  if ((state->internal)->dev_ver <= 32U) {
    if ((unsigned int )state->demod == 2U) {
      tmp___51 = stv090x_write_reg(state, 62096U, 38);
      tmp___53 = tmp___51 < 0;
    } else {
      tmp___52 = stv090x_write_reg(state, 62608U, 38);
      tmp___53 = tmp___52 < 0;
    }
    if (tmp___53) {
      goto err;
    } else {
      if ((unsigned int )state->demod == 2U) {
        tmp___48 = stv090x_write_reg(state, 62096U, 102);
        tmp___50 = tmp___48 < 0;
      } else {
        tmp___49 = stv090x_write_reg(state, 62608U, 102);
        tmp___50 = tmp___49 < 0;
      }
      if (tmp___50) {
        goto err;
      } else {
      }
    }
  } else {
  }
  if ((unsigned int )state->demod_mode != 1U) {
    tmp___55 = stv090x_activate_modcod(state);
    if (tmp___55 < 0) {
      goto err;
    } else {
      tmp___54 = stv090x_activate_modcod_single(state);
      if (tmp___54 < 0) {
        goto err;
      } else {
      }
    }
  } else {
  }
  tmp___56 = stv090x_set_vit_thacq(state);
  if (tmp___56 < 0) {
    goto err;
  } else {
  }
  tmp___57 = stv090x_set_viterbi(state);
  if (tmp___57 < 0) {
    goto err;
  } else {
  }
  goto ldv_24146;
  }
  ldv_24146: ;
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_delivery_search");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_delivery_search");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_delivery_search");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_delivery_search");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_start_search(struct stv090x_state *state )
{
  u32 reg ;
  u32 freq_abs ;
  s16 freq ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  int tmp___23 ;
  int tmp___24 ;
  int tmp___25 ;
  int tmp___26 ;
  int tmp___27 ;
  int tmp___28 ;
  int tmp___29 ;
  int tmp___30 ;
  int tmp___31 ;
  int tmp___32 ;
  int tmp___33 ;
  int tmp___34 ;
  int tmp___35 ;
  int tmp___36 ;
  int tmp___37 ;
  int tmp___38 ;
  int tmp___39 ;
  int tmp___40 ;
  int tmp___41 ;
  int tmp___42 ;
  int tmp___43 ;
  int tmp___44 ;
  int tmp___45 ;
  int tmp___46 ;
  int tmp___47 ;
  int tmp___48 ;
  int tmp___49 ;
  int tmp___50 ;
  int tmp___51 ;
  int tmp___52 ;
  int tmp___53 ;
  int tmp___54 ;
  int tmp___55 ;
  int tmp___56 ;
  int tmp___57 ;
  int tmp___58 ;
  int tmp___59 ;
  int tmp___60 ;
  int tmp___61 ;
  int tmp___62 ;
  int tmp___63 ;
  int tmp___64 ;
  int tmp___65 ;
  int tmp___66 ;
  int tmp___67 ;
  int tmp___68 ;
  int tmp___69 ;
  int tmp___70 ;
  int tmp___71 ;
  int tmp___72 ;
  int tmp___73 ;
  int tmp___74 ;
  int tmp___75 ;
  int tmp___76 ;
  int tmp___77 ;
  int tmp___78 ;
  int tmp___79 ;
  int tmp___80 ;
  int tmp___81 ;
  int tmp___82 ;
  int tmp___83 ;
  int tmp___84 ;
  int tmp___85 ;
  int tmp___86 ;
  int tmp___87 ;
  int tmp___88 ;
  int tmp___89 ;
  int tmp___90 ;
  int tmp___91 ;
  int tmp___92 ;
  int tmp___93 ;
  int tmp___94 ;
  int tmp___95 ;
  int tmp___96 ;
  int tmp___97 ;
  int tmp___98 ;
  int tmp___99 ;
  int tmp___100 ;
  int tmp___101 ;
  int tmp___102 ;
  int tmp___103 ;
  int tmp___104 ;
  int tmp___105 ;
  int tmp___106 ;
  int tmp___107 ;
  int tmp___108 ;
  int tmp___109 ;
  int tmp___110 ;
  int tmp___111 ;
  int tmp___112 ;
  int tmp___113 ;
  int tmp___114 ;
  int tmp___115 ;
  int tmp___116 ;
  int tmp___117 ;
  int tmp___118 ;
  int tmp___119 ;
  int tmp___120 ;
  int tmp___121 ;
  {
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 61974U);
    tmp___1 = tmp;
  } else {
    tmp___0 = stv090x_read_reg(state, 62486U);
    tmp___1 = tmp___0;
  }
  reg = (u32 )tmp___1;
  reg = reg | 31U;
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_write_reg(state, 61974U, (int )((u8 )reg));
    tmp___4 = tmp___2 < 0;
  } else {
    tmp___3 = stv090x_write_reg(state, 62486U, (int )((u8 )reg));
    tmp___4 = tmp___3 < 0;
  }
  if (tmp___4) {
    goto err;
  } else {
  }
  if ((state->internal)->dev_ver <= 32U) {
    if (state->srate <= 5000000U) {
      if ((unsigned int )state->demod == 2U) {
        tmp___5 = stv090x_write_reg(state, 62008U, 68);
        tmp___7 = tmp___5 < 0;
      } else {
        tmp___6 = stv090x_write_reg(state, 62520U, 68);
        tmp___7 = tmp___6 < 0;
      }
      if (tmp___7) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___8 = stv090x_write_reg(state, 62018U, 15);
        tmp___10 = tmp___8 < 0;
      } else {
        tmp___9 = stv090x_write_reg(state, 62530U, 15);
        tmp___10 = tmp___9 < 0;
      }
      if (tmp___10) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___11 = stv090x_write_reg(state, 62019U, 255);
        tmp___13 = tmp___11 < 0;
      } else {
        tmp___12 = stv090x_write_reg(state, 62531U, 255);
        tmp___13 = tmp___12 < 0;
      }
      if (tmp___13) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___14 = stv090x_write_reg(state, 62022U, 240);
        tmp___16 = tmp___14 < 0;
      } else {
        tmp___15 = stv090x_write_reg(state, 62534U, 240);
        tmp___16 = tmp___15 < 0;
      }
      if (tmp___16) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___17 = stv090x_write_reg(state, 62023U, 0);
        tmp___19 = tmp___17 < 0;
      } else {
        tmp___18 = stv090x_write_reg(state, 62535U, 0);
        tmp___19 = tmp___18 < 0;
      }
      if (tmp___19) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___20 = stv090x_write_reg(state, 62034U, 104);
        tmp___22 = tmp___20 < 0;
      } else {
        tmp___21 = stv090x_write_reg(state, 62546U, 104);
        tmp___22 = tmp___21 < 0;
      }
      if (tmp___22) {
        goto err;
      } else {
      }
    } else {
      if ((unsigned int )state->demod == 2U) {
        tmp___23 = stv090x_write_reg(state, 62008U, 196);
        tmp___25 = tmp___23 < 0;
      } else {
        tmp___24 = stv090x_write_reg(state, 62520U, 196);
        tmp___25 = tmp___24 < 0;
      }
      if (tmp___25) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___26 = stv090x_write_reg(state, 62034U, 68);
        tmp___28 = tmp___26 < 0;
      } else {
        tmp___27 = stv090x_write_reg(state, 62546U, 68);
        tmp___28 = tmp___27 < 0;
      }
      if (tmp___28) {
        goto err;
      } else {
      }
    }
  } else {
    if (state->srate <= 5000000U) {
      if ((unsigned int )state->demod == 2U) {
        stv090x_write_reg(state, 62034U, 104);
      } else {
        stv090x_write_reg(state, 62546U, 104);
      }
    } else
    if ((unsigned int )state->demod == 2U) {
      stv090x_write_reg(state, 62034U, 68);
    } else {
      stv090x_write_reg(state, 62546U, 68);
    }
    if ((unsigned int )state->demod == 2U) {
      stv090x_write_reg(state, 62008U, 70);
    } else {
      stv090x_write_reg(state, 62520U, 70);
    }
    if ((unsigned int )state->algo == 2U) {
      freq_abs = 65536000U;
      freq_abs = freq_abs / (u32 )((state->internal)->mclk / 1000);
      freq = (short )freq_abs;
    } else {
      freq_abs = (u32 )(state->search_range / 2000 + 600);
      freq_abs = freq_abs << 16;
      freq_abs = freq_abs / (u32 )((state->internal)->mclk / 1000);
      freq = (short )freq_abs;
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___29 = stv090x_write_reg(state, 62018U, (int )((u8 )((int )freq >> 8)));
      tmp___31 = tmp___29 < 0;
    } else {
      tmp___30 = stv090x_write_reg(state, 62530U, (int )((u8 )((int )freq >> 8)));
      tmp___31 = tmp___30 < 0;
    }
    if (tmp___31) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___32 = stv090x_write_reg(state, 62019U, (int )((u8 )freq));
      tmp___34 = tmp___32 < 0;
    } else {
      tmp___33 = stv090x_write_reg(state, 62531U, (int )((u8 )freq));
      tmp___34 = tmp___33 < 0;
    }
    if (tmp___34) {
      goto err;
    } else {
    }
    freq = (s16 )(- ((int )((unsigned short )freq)));
    if ((unsigned int )state->demod == 2U) {
      tmp___35 = stv090x_write_reg(state, 62022U, (int )((u8 )((int )freq >> 8)));
      tmp___37 = tmp___35 < 0;
    } else {
      tmp___36 = stv090x_write_reg(state, 62534U, (int )((u8 )((int )freq >> 8)));
      tmp___37 = tmp___36 < 0;
    }
    if (tmp___37) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___38 = stv090x_write_reg(state, 62023U, (int )((u8 )freq));
      tmp___40 = tmp___38 < 0;
    } else {
      tmp___39 = stv090x_write_reg(state, 62535U, (int )((u8 )freq));
      tmp___40 = tmp___39 < 0;
    }
    if (tmp___40) {
      goto err;
    } else {
    }
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___41 = stv090x_write_reg(state, 62024U, 0);
    tmp___43 = tmp___41 < 0;
  } else {
    tmp___42 = stv090x_write_reg(state, 62536U, 0);
    tmp___43 = tmp___42 < 0;
  }
  if (tmp___43) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___44 = stv090x_write_reg(state, 62025U, 0);
    tmp___46 = tmp___44 < 0;
  } else {
    tmp___45 = stv090x_write_reg(state, 62537U, 0);
    tmp___46 = tmp___45 < 0;
  }
  if (tmp___46) {
    goto err;
  } else {
  }
  if ((state->internal)->dev_ver > 31U) {
    if ((unsigned int )state->demod == 2U) {
      tmp___47 = stv090x_write_reg(state, 62063U, 65);
      tmp___49 = tmp___47 < 0;
    } else {
      tmp___48 = stv090x_write_reg(state, 62575U, 65);
      tmp___49 = tmp___48 < 0;
    }
    if (tmp___49) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___50 = stv090x_write_reg(state, 62168U, 65);
      tmp___52 = tmp___50 < 0;
    } else {
      tmp___51 = stv090x_write_reg(state, 62680U, 65);
      tmp___52 = tmp___51 < 0;
    }
    if (tmp___52) {
      goto err;
    } else {
    }
    if (((unsigned int )state->search_mode == 1U || (unsigned int )state->search_mode == 0U) || (unsigned int )state->search_mode == 3U) {
      if ((unsigned int )state->demod == 2U) {
        tmp___53 = stv090x_write_reg(state, 62258U, 130);
        tmp___55 = tmp___53 < 0;
      } else {
        tmp___54 = stv090x_write_reg(state, 62770U, 130);
        tmp___55 = tmp___54 < 0;
      }
      if (tmp___55) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___56 = stv090x_write_reg(state, 62269U, 0);
        tmp___58 = tmp___56 < 0;
      } else {
        tmp___57 = stv090x_write_reg(state, 62781U, 0);
        tmp___58 = tmp___57 < 0;
      }
      if (tmp___58) {
        goto err;
      } else {
      }
    } else {
    }
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___59 = stv090x_write_reg(state, 62041U, 0);
    tmp___61 = tmp___59 < 0;
  } else {
    tmp___60 = stv090x_write_reg(state, 62553U, 0);
    tmp___61 = tmp___60 < 0;
  }
  if (tmp___61) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___62 = stv090x_write_reg(state, 62035U, 224);
    tmp___64 = tmp___62 < 0;
  } else {
    tmp___63 = stv090x_write_reg(state, 62547U, 224);
    tmp___64 = tmp___63 < 0;
  }
  if (tmp___64) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___65 = stv090x_write_reg(state, 62036U, 192);
    tmp___67 = tmp___65 < 0;
  } else {
    tmp___66 = stv090x_write_reg(state, 62548U, 192);
    tmp___67 = tmp___66 < 0;
  }
  if (tmp___67) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___68 = stv090x_read_reg(state, 61972U);
    tmp___70 = tmp___68;
  } else {
    tmp___69 = stv090x_read_reg(state, 62484U);
    tmp___70 = tmp___69;
  }
  reg = (u32 )tmp___70;
  reg = reg & 4294967279U;
  reg = reg & 4294967287U;
  if ((unsigned int )state->demod == 2U) {
    tmp___71 = stv090x_write_reg(state, 61972U, (int )((u8 )reg));
    tmp___73 = tmp___71 < 0;
  } else {
    tmp___72 = stv090x_write_reg(state, 62484U, (int )((u8 )reg));
    tmp___73 = tmp___72 < 0;
  }
  if (tmp___73) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___74 = stv090x_read_reg(state, 61973U);
    tmp___76 = tmp___74;
  } else {
    tmp___75 = stv090x_read_reg(state, 62485U);
    tmp___76 = tmp___75;
  }
  reg = (u32 )tmp___76;
  reg = reg & 4294967231U;
  if ((unsigned int )state->demod == 2U) {
    tmp___77 = stv090x_write_reg(state, 61973U, (int )((u8 )reg));
    tmp___79 = tmp___77 < 0;
  } else {
    tmp___78 = stv090x_write_reg(state, 62485U, (int )((u8 )reg));
    tmp___79 = tmp___78 < 0;
  }
  if (tmp___79) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___80 = stv090x_write_reg(state, 62033U, 136);
    tmp___82 = tmp___80 < 0;
  } else {
    tmp___81 = stv090x_write_reg(state, 62545U, 136);
    tmp___82 = tmp___81 < 0;
  }
  if (tmp___82) {
    goto err;
  } else {
  }
  if ((state->internal)->dev_ver > 31U) {
    if (state->srate <= 1999999U) {
      if ((state->internal)->dev_ver <= 32U) {
        if ((unsigned int )state->demod == 2U) {
          tmp___86 = stv090x_write_reg(state, 62013U, 57);
          tmp___88 = tmp___86 < 0;
        } else {
          tmp___87 = stv090x_write_reg(state, 62525U, 57);
          tmp___88 = tmp___87 < 0;
        }
        if (tmp___88) {
          goto err;
        } else {
          if ((unsigned int )state->demod == 2U) {
            tmp___83 = stv090x_write_reg(state, 62013U, 137);
            tmp___85 = tmp___83 < 0;
          } else {
            tmp___84 = stv090x_write_reg(state, 62525U, 137);
            tmp___85 = tmp___84 < 0;
          }
          if (tmp___85) {
            goto err;
          } else {
          }
        }
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___89 = stv090x_write_reg(state, 62014U, 64);
        tmp___91 = tmp___89 < 0;
      } else {
        tmp___90 = stv090x_write_reg(state, 62526U, 64);
        tmp___91 = tmp___90 < 0;
      }
      if (tmp___91) {
        goto err;
      } else {
      }
    } else
    if (state->srate <= 9999999U) {
      if ((unsigned int )state->demod == 2U) {
        tmp___92 = stv090x_write_reg(state, 62013U, 76);
        tmp___94 = tmp___92 < 0;
      } else {
        tmp___93 = stv090x_write_reg(state, 62525U, 76);
        tmp___94 = tmp___93 < 0;
      }
      if (tmp___94) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___95 = stv090x_write_reg(state, 62014U, 32);
        tmp___97 = tmp___95 < 0;
      } else {
        tmp___96 = stv090x_write_reg(state, 62526U, 32);
        tmp___97 = tmp___96 < 0;
      }
      if (tmp___97) {
        goto err;
      } else {
      }
    } else {
      if ((unsigned int )state->demod == 2U) {
        tmp___98 = stv090x_write_reg(state, 62013U, 75);
        tmp___100 = tmp___98 < 0;
      } else {
        tmp___99 = stv090x_write_reg(state, 62525U, 75);
        tmp___100 = tmp___99 < 0;
      }
      if (tmp___100) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___101 = stv090x_write_reg(state, 62014U, 32);
        tmp___103 = tmp___101 < 0;
      } else {
        tmp___102 = stv090x_write_reg(state, 62526U, 32);
        tmp___103 = tmp___102 < 0;
      }
      if (tmp___103) {
        goto err;
      } else {
      }
    }
  } else
  if (state->srate <= 9999999U) {
    if ((unsigned int )state->demod == 2U) {
      tmp___107 = stv090x_write_reg(state, 62013U, 239);
      tmp___109 = tmp___107 < 0;
    } else {
      tmp___108 = stv090x_write_reg(state, 62525U, 239);
      tmp___109 = tmp___108 < 0;
    }
    if (tmp___109) {
      goto err;
    } else {
      if ((unsigned int )state->demod == 2U) {
        tmp___104 = stv090x_write_reg(state, 62013U, 237);
        tmp___106 = tmp___104 < 0;
      } else {
        tmp___105 = stv090x_write_reg(state, 62525U, 237);
        tmp___106 = tmp___105 < 0;
      }
      if (tmp___106) {
        goto err;
      } else {
      }
    }
  } else {
  }
  switch ((unsigned int )state->algo) {
  case 2U: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___110 = stv090x_write_reg(state, 61974U, 31);
    tmp___112 = tmp___110 < 0;
  } else {
    tmp___111 = stv090x_write_reg(state, 62486U, 31);
    tmp___112 = tmp___111 < 0;
  }
  if (tmp___112) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___113 = stv090x_write_reg(state, 61974U, 24);
    tmp___115 = tmp___113 < 0;
  } else {
    tmp___114 = stv090x_write_reg(state, 62486U, 24);
    tmp___115 = tmp___114 < 0;
  }
  if (tmp___115) {
    goto err;
  } else {
  }
  goto ldv_24159;
  case 1U: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___116 = stv090x_write_reg(state, 61974U, 31);
    tmp___118 = tmp___116 < 0;
  } else {
    tmp___117 = stv090x_write_reg(state, 62486U, 31);
    tmp___118 = tmp___117 < 0;
  }
  if (tmp___118) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___119 = stv090x_write_reg(state, 61974U, 21);
    tmp___121 = tmp___119 < 0;
  } else {
    tmp___120 = stv090x_write_reg(state, 62486U, 21);
    tmp___121 = tmp___120 < 0;
  }
  if (tmp___121) {
    goto err;
  } else {
  }
  goto ldv_24159;
  default: ;
  goto ldv_24159;
  }
  ldv_24159: ;
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_start_search");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_start_search");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_start_search");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_start_search");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_get_agc2_min_level(struct stv090x_state *state )
{
  u32 agc2_min ;
  u32 agc2 ;
  u32 freq_init ;
  u32 freq_step ;
  u32 reg ;
  s32 i ;
  s32 j ;
  s32 steps ;
  s32 dir ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  int tmp___23 ;
  int tmp___24 ;
  int tmp___25 ;
  int tmp___26 ;
  int tmp___27 ;
  int tmp___28 ;
  int tmp___29 ;
  int tmp___30 ;
  int tmp___31 ;
  int tmp___32 ;
  int tmp___33 ;
  int tmp___34 ;
  int tmp___35 ;
  int tmp___36 ;
  int tmp___37 ;
  int tmp___38 ;
  int tmp___39 ;
  int tmp___40 ;
  int tmp___41 ;
  {
  agc2_min = 65535U;
  agc2 = 0U;
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_write_reg(state, 61997U, 56);
    tmp___1 = tmp < 0;
  } else {
    tmp___0 = stv090x_write_reg(state, 62509U, 56);
    tmp___1 = tmp___0 < 0;
  }
  if (tmp___1) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_read_reg(state, 61972U);
    tmp___4 = tmp___2;
  } else {
    tmp___3 = stv090x_read_reg(state, 62484U);
    tmp___4 = tmp___3;
  }
  reg = (u32 )tmp___4;
  reg = reg & 4294967279U;
  reg = reg & 4294967287U;
  if ((unsigned int )state->demod == 2U) {
    tmp___5 = stv090x_write_reg(state, 61972U, (int )((u8 )reg));
    tmp___7 = tmp___5 < 0;
  } else {
    tmp___6 = stv090x_write_reg(state, 62484U, (int )((u8 )reg));
    tmp___7 = tmp___6 < 0;
  }
  if (tmp___7) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___8 = stv090x_write_reg(state, 62048U, 131);
    tmp___10 = tmp___8 < 0;
  } else {
    tmp___9 = stv090x_write_reg(state, 62560U, 131);
    tmp___10 = tmp___9 < 0;
  }
  if (tmp___10) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___11 = stv090x_write_reg(state, 62049U, 192);
    tmp___13 = tmp___11 < 0;
  } else {
    tmp___12 = stv090x_write_reg(state, 62561U, 192);
    tmp___13 = tmp___12 < 0;
  }
  if (tmp___13) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___14 = stv090x_write_reg(state, 62050U, 130);
    tmp___16 = tmp___14 < 0;
  } else {
    tmp___15 = stv090x_write_reg(state, 62562U, 130);
    tmp___16 = tmp___15 < 0;
  }
  if (tmp___16) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___17 = stv090x_write_reg(state, 62051U, 160);
    tmp___19 = tmp___17 < 0;
  } else {
    tmp___18 = stv090x_write_reg(state, 62563U, 160);
    tmp___19 = tmp___18 < 0;
  }
  if (tmp___19) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___20 = stv090x_write_reg(state, 61975U, 0);
    tmp___22 = tmp___20 < 0;
  } else {
    tmp___21 = stv090x_write_reg(state, 62487U, 0);
    tmp___22 = tmp___21 < 0;
  }
  if (tmp___22) {
    goto err;
  } else {
  }
  tmp___23 = stv090x_set_srate(state, 1000000U);
  if (tmp___23 < 0) {
    goto err;
  } else {
  }
  steps = state->search_range / 1000000;
  if (steps <= 0) {
    steps = 1;
  } else {
  }
  dir = 1;
  freq_step = (u32 )(256000000 / ((state->internal)->mclk / 256));
  freq_init = 0U;
  i = 0;
  goto ldv_24180;
  ldv_24179: ;
  if (dir > 0) {
    freq_init = freq_step * (u32 )i + freq_init;
  } else {
    freq_init = freq_init - freq_step * (u32 )i;
  }
  dir = - dir;
  if ((unsigned int )state->demod == 2U) {
    tmp___24 = stv090x_write_reg(state, 61974U, 92);
    tmp___26 = tmp___24 < 0;
  } else {
    tmp___25 = stv090x_write_reg(state, 62486U, 92);
    tmp___26 = tmp___25 < 0;
  }
  if (tmp___26) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___27 = stv090x_write_reg(state, 62024U, (int )((u8 )(freq_init >> 8)));
    tmp___29 = tmp___27 < 0;
  } else {
    tmp___28 = stv090x_write_reg(state, 62536U, (int )((u8 )(freq_init >> 8)));
    tmp___29 = tmp___28 < 0;
  }
  if (tmp___29) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___30 = stv090x_write_reg(state, 62025U, (int )((u8 )freq_init));
    tmp___32 = tmp___30 < 0;
  } else {
    tmp___31 = stv090x_write_reg(state, 62537U, (int )((u8 )freq_init));
    tmp___32 = tmp___31 < 0;
  }
  if (tmp___32) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___33 = stv090x_write_reg(state, 61974U, 88);
    tmp___35 = tmp___33 < 0;
  } else {
    tmp___34 = stv090x_write_reg(state, 62486U, 88);
    tmp___35 = tmp___34 < 0;
  }
  if (tmp___35) {
    goto err;
  } else {
  }
  msleep(10U);
  agc2 = 0U;
  j = 0;
  goto ldv_24177;
  ldv_24176: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___36 = stv090x_read_reg(state, 62006U);
    tmp___38 = tmp___36 << 8;
  } else {
    tmp___37 = stv090x_read_reg(state, 62518U);
    tmp___38 = tmp___37 << 8;
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___39 = stv090x_read_reg(state, 62007U);
    tmp___41 = tmp___39;
  } else {
    tmp___40 = stv090x_read_reg(state, 62519U);
    tmp___41 = tmp___40;
  }
  agc2 = (u32 )(tmp___38 | tmp___41) + agc2;
  j = j + 1;
  ldv_24177: ;
  if (j <= 9) {
    goto ldv_24176;
  } else {
  }
  agc2 = agc2 / 10U;
  if (agc2 < agc2_min) {
    agc2_min = agc2;
  } else {
  }
  i = i + 1;
  ldv_24180: ;
  if (i < steps) {
    goto ldv_24179;
  } else {
  }
  return ((int )agc2_min);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_get_agc2_min_level");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_get_agc2_min_level");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_get_agc2_min_level");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_get_agc2_min_level");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static u32 stv090x_get_srate(struct stv090x_state *state , u32 clk )
{
  u8 r3 ;
  u8 r2 ;
  u8 r1 ;
  u8 r0 ;
  s32 srate ;
  s32 int_1 ;
  s32 int_2 ;
  s32 tmp_1 ;
  s32 tmp_2 ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  {
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 62052U);
    r3 = (u8 )tmp;
  } else {
    tmp___0 = stv090x_read_reg(state, 62564U);
    r3 = (u8 )tmp___0;
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___1 = stv090x_read_reg(state, 62053U);
    r2 = (u8 )tmp___1;
  } else {
    tmp___2 = stv090x_read_reg(state, 62565U);
    r2 = (u8 )tmp___2;
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___3 = stv090x_read_reg(state, 62054U);
    r1 = (u8 )tmp___3;
  } else {
    tmp___4 = stv090x_read_reg(state, 62566U);
    r1 = (u8 )tmp___4;
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___5 = stv090x_read_reg(state, 62055U);
    r0 = (u8 )tmp___5;
  } else {
    tmp___6 = stv090x_read_reg(state, 62567U);
    r0 = (u8 )tmp___6;
  }
  srate = ((((int )r3 << 24) | ((int )r2 << 16)) | ((int )r1 << 8)) | (int )r0;
  int_1 = (s32 )(clk >> 16);
  int_2 = srate >> 16;
  tmp_1 = (s32 )clk & 65535;
  tmp_2 = srate % 65536;
  srate = (int_1 * int_2 + (int_1 * tmp_2 >> 16)) + (int_2 * tmp_1 >> 16);
  return ((u32 )srate);
}
}
static u32 stv090x_srate_srch_coarse(struct stv090x_state *state )
{
  struct dvb_frontend *fe ;
  int tmg_lock ;
  int i ;
  s32 tmg_cpt ;
  s32 dir ;
  s32 steps ;
  s32 cur_step ;
  s32 freq ;
  u32 srate_coarse ;
  u32 agc2 ;
  u32 car_step ;
  u32 reg ;
  u32 agc2th ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  int tmp___23 ;
  int tmp___24 ;
  int tmp___25 ;
  int tmp___26 ;
  int tmp___27 ;
  int tmp___28 ;
  int tmp___29 ;
  int tmp___30 ;
  int tmp___31 ;
  int tmp___32 ;
  int tmp___33 ;
  int tmp___34 ;
  int tmp___35 ;
  int tmp___36 ;
  int tmp___37 ;
  int tmp___38 ;
  int tmp___39 ;
  int tmp___40 ;
  int tmp___41 ;
  int tmp___42 ;
  int tmp___43 ;
  int tmp___44 ;
  int tmp___45 ;
  int tmp___46 ;
  int tmp___47 ;
  int tmp___48 ;
  int tmp___49 ;
  int tmp___50 ;
  int tmp___51 ;
  int tmp___52 ;
  int tmp___53 ;
  int tmp___54 ;
  int tmp___55 ;
  int tmp___56 ;
  int tmp___57 ;
  int tmp___58 ;
  int tmp___59 ;
  int tmp___60 ;
  int tmp___61 ;
  int tmp___62 ;
  int tmp___63 ;
  int tmp___64 ;
  int tmp___65 ;
  int tmp___66 ;
  int tmp___67 ;
  int tmp___68 ;
  int tmp___69 ;
  int tmp___70 ;
  int tmp___71 ;
  int tmp___72 ;
  int tmp___73 ;
  int tmp___74 ;
  int tmp___75 ;
  int tmp___76 ;
  int tmp___77 ;
  int tmp___78 ;
  int tmp___79 ;
  int tmp___80 ;
  int tmp___81 ;
  int tmp___82 ;
  int tmp___83 ;
  int tmp___84 ;
  int tmp___85 ;
  int tmp___86 ;
  {
  fe = & state->frontend;
  tmg_lock = 0;
  tmg_cpt = 0;
  dir = 1;
  cur_step = 0;
  srate_coarse = 0U;
  agc2 = 0U;
  car_step = 1200U;
  if ((state->internal)->dev_ver > 47U) {
    agc2th = 11776U;
  } else {
    agc2th = 7936U;
  }
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 61974U);
    tmp___1 = tmp;
  } else {
    tmp___0 = stv090x_read_reg(state, 62486U);
    tmp___1 = tmp___0;
  }
  reg = (u32 )tmp___1;
  reg = reg | 31U;
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_write_reg(state, 61974U, (int )((u8 )reg));
    tmp___4 = tmp___2 < 0;
  } else {
    tmp___3 = stv090x_write_reg(state, 62486U, (int )((u8 )reg));
    tmp___4 = tmp___3 < 0;
  }
  if (tmp___4) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___5 = stv090x_write_reg(state, 62032U, 18);
    tmp___7 = tmp___5 < 0;
  } else {
    tmp___6 = stv090x_write_reg(state, 62544U, 18);
    tmp___7 = tmp___6 < 0;
  }
  if (tmp___7) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___8 = stv090x_write_reg(state, 62042U, 192);
    tmp___10 = tmp___8 < 0;
  } else {
    tmp___9 = stv090x_write_reg(state, 62554U, 192);
    tmp___10 = tmp___9 < 0;
  }
  if (tmp___10) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___11 = stv090x_write_reg(state, 62035U, 240);
    tmp___13 = tmp___11 < 0;
  } else {
    tmp___12 = stv090x_write_reg(state, 62547U, 240);
    tmp___13 = tmp___12 < 0;
  }
  if (tmp___13) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___14 = stv090x_write_reg(state, 62036U, 224);
    tmp___16 = tmp___14 < 0;
  } else {
    tmp___15 = stv090x_write_reg(state, 62548U, 224);
    tmp___16 = tmp___15 < 0;
  }
  if (tmp___16) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___17 = stv090x_read_reg(state, 61972U);
    tmp___19 = tmp___17;
  } else {
    tmp___18 = stv090x_read_reg(state, 62484U);
    tmp___19 = tmp___18;
  }
  reg = (u32 )tmp___19;
  reg = reg | 16U;
  reg = reg & 4294967287U;
  if ((unsigned int )state->demod == 2U) {
    tmp___20 = stv090x_write_reg(state, 61972U, (int )((u8 )reg));
    tmp___22 = tmp___20 < 0;
  } else {
    tmp___21 = stv090x_write_reg(state, 62484U, (int )((u8 )reg));
    tmp___22 = tmp___21 < 0;
  }
  if (tmp___22) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___23 = stv090x_write_reg(state, 62048U, 131);
    tmp___25 = tmp___23 < 0;
  } else {
    tmp___24 = stv090x_write_reg(state, 62560U, 131);
    tmp___25 = tmp___24 < 0;
  }
  if (tmp___25) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___26 = stv090x_write_reg(state, 62049U, 192);
    tmp___28 = tmp___26 < 0;
  } else {
    tmp___27 = stv090x_write_reg(state, 62561U, 192);
    tmp___28 = tmp___27 < 0;
  }
  if (tmp___28) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___29 = stv090x_write_reg(state, 62050U, 130);
    tmp___31 = tmp___29 < 0;
  } else {
    tmp___30 = stv090x_write_reg(state, 62562U, 130);
    tmp___31 = tmp___30 < 0;
  }
  if (tmp___31) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___32 = stv090x_write_reg(state, 62051U, 160);
    tmp___34 = tmp___32 < 0;
  } else {
    tmp___33 = stv090x_write_reg(state, 62563U, 160);
    tmp___34 = tmp___33 < 0;
  }
  if (tmp___34) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___35 = stv090x_write_reg(state, 61975U, 0);
    tmp___37 = tmp___35 < 0;
  } else {
    tmp___36 = stv090x_write_reg(state, 62487U, 0);
    tmp___37 = tmp___36 < 0;
  }
  if (tmp___37) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___38 = stv090x_write_reg(state, 61997U, 80);
    tmp___40 = tmp___38 < 0;
  } else {
    tmp___39 = stv090x_write_reg(state, 62509U, 80);
    tmp___40 = tmp___39 < 0;
  }
  if (tmp___40) {
    goto err;
  } else {
  }
  if ((state->internal)->dev_ver > 47U) {
    if ((unsigned int )state->demod == 2U) {
      tmp___41 = stv090x_write_reg(state, 62013U, 153);
      tmp___43 = tmp___41 < 0;
    } else {
      tmp___42 = stv090x_write_reg(state, 62525U, 153);
      tmp___43 = tmp___42 < 0;
    }
    if (tmp___43) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___44 = stv090x_write_reg(state, 62041U, 152);
      tmp___46 = tmp___44 < 0;
    } else {
      tmp___45 = stv090x_write_reg(state, 62553U, 152);
      tmp___46 = tmp___45 < 0;
    }
    if (tmp___46) {
      goto err;
    } else {
    }
  } else
  if ((state->internal)->dev_ver > 31U) {
    if ((unsigned int )state->demod == 2U) {
      tmp___47 = stv090x_write_reg(state, 62013U, 106);
      tmp___49 = tmp___47 < 0;
    } else {
      tmp___48 = stv090x_write_reg(state, 62525U, 106);
      tmp___49 = tmp___48 < 0;
    }
    if (tmp___49) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___50 = stv090x_write_reg(state, 62041U, 149);
      tmp___52 = tmp___50 < 0;
    } else {
      tmp___51 = stv090x_write_reg(state, 62553U, 149);
      tmp___52 = tmp___51 < 0;
    }
    if (tmp___52) {
      goto err;
    } else {
    }
  } else {
  }
  if (state->srate <= 2000000U) {
    car_step = 1000U;
  } else
  if (state->srate <= 5000000U) {
    car_step = 2000U;
  } else
  if (state->srate <= 12000000U) {
    car_step = 3000U;
  } else {
    car_step = 5000U;
  }
  steps = (s32 )((u32 )(state->search_range / 1000) / car_step + 4294967295U);
  steps = steps / 2;
  steps = steps * 2 + 1;
  if (steps < 0) {
    steps = 1;
  } else
  if (steps > 10) {
    steps = 11;
    car_step = (u32 )(state->search_range / 10000);
  } else {
  }
  cur_step = 0;
  dir = 1;
  freq = (s32 )state->frequency;
  goto ldv_24219;
  ldv_24218: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___53 = stv090x_write_reg(state, 61974U, 95);
    tmp___55 = tmp___53 < 0;
  } else {
    tmp___54 = stv090x_write_reg(state, 62486U, 95);
    tmp___55 = tmp___54 < 0;
  }
  if (tmp___55) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___56 = stv090x_write_reg(state, 62024U, 0);
    tmp___58 = tmp___56 < 0;
  } else {
    tmp___57 = stv090x_write_reg(state, 62536U, 0);
    tmp___58 = tmp___57 < 0;
  }
  if (tmp___58) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___59 = stv090x_write_reg(state, 62025U, 0);
    tmp___61 = tmp___59 < 0;
  } else {
    tmp___60 = stv090x_write_reg(state, 62537U, 0);
    tmp___61 = tmp___60 < 0;
  }
  if (tmp___61) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___62 = stv090x_write_reg(state, 62046U, 0);
    tmp___64 = tmp___62 < 0;
  } else {
    tmp___63 = stv090x_write_reg(state, 62558U, 0);
    tmp___64 = tmp___63 < 0;
  }
  if (tmp___64) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___65 = stv090x_write_reg(state, 62047U, 0);
    tmp___67 = tmp___65 < 0;
  } else {
    tmp___66 = stv090x_write_reg(state, 62559U, 0);
    tmp___67 = tmp___66 < 0;
  }
  if (tmp___67) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___68 = stv090x_write_reg(state, 61974U, 64);
    tmp___70 = tmp___68 < 0;
  } else {
    tmp___69 = stv090x_write_reg(state, 62486U, 64);
    tmp___70 = tmp___69 < 0;
  }
  if (tmp___70) {
    goto err;
  } else {
  }
  msleep(50U);
  i = 0;
  goto ldv_24214;
  ldv_24213: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___71 = stv090x_read_reg(state, 61970U);
    tmp___73 = tmp___71;
  } else {
    tmp___72 = stv090x_read_reg(state, 62482U);
    tmp___73 = tmp___72;
  }
  reg = (u32 )tmp___73;
  if (((reg >> 5) & 3U) > 1U) {
    tmg_cpt = tmg_cpt + 1;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___74 = stv090x_read_reg(state, 62006U);
    tmp___76 = tmp___74 << 8;
  } else {
    tmp___75 = stv090x_read_reg(state, 62518U);
    tmp___76 = tmp___75 << 8;
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___77 = stv090x_read_reg(state, 62007U);
    tmp___79 = tmp___77;
  } else {
    tmp___78 = stv090x_read_reg(state, 62519U);
    tmp___79 = tmp___78;
  }
  agc2 = (u32 )(tmp___76 | tmp___79) + agc2;
  i = i + 1;
  ldv_24214: ;
  if (i <= 9) {
    goto ldv_24213;
  } else {
  }
  agc2 = agc2 / 10U;
  srate_coarse = stv090x_get_srate(state, (u32 )(state->internal)->mclk);
  cur_step = cur_step + 1;
  dir = - dir;
  if (((tmg_cpt > 4 && agc2 < agc2th) && srate_coarse <= 49999999U) && srate_coarse > 850000U) {
    tmg_lock = 1;
  } else
  if (cur_step < steps) {
    if (dir > 0) {
      freq = (s32 )((u32 )cur_step * car_step + (u32 )freq);
    } else {
      freq = (s32 )((u32 )freq - (u32 )cur_step * car_step);
    }
    tmp___80 = stv090x_i2c_gate_ctrl(state, 1);
    if (tmp___80 < 0) {
      goto err;
    } else {
    }
    if ((unsigned long )(state->config)->tuner_set_frequency != (unsigned long )((int (* )(struct dvb_frontend * ,
                                                                                                      u32 ))0)) {
      tmp___81 = (*((state->config)->tuner_set_frequency))(fe, (u32 )freq);
      if (tmp___81 < 0) {
        goto err_gateoff;
      } else {
      }
    } else {
    }
    if ((unsigned long )(state->config)->tuner_set_bandwidth != (unsigned long )((int (* )(struct dvb_frontend * ,
                                                                                                      u32 ))0)) {
      tmp___82 = (*((state->config)->tuner_set_bandwidth))(fe, (u32 )state->tuner_bw);
      if (tmp___82 < 0) {
        goto err_gateoff;
      } else {
      }
    } else {
    }
    tmp___83 = stv090x_i2c_gate_ctrl(state, 0);
    if (tmp___83 < 0) {
      goto err;
    } else {
    }
    msleep(50U);
    tmp___84 = stv090x_i2c_gate_ctrl(state, 1);
    if (tmp___84 < 0) {
      goto err;
    } else {
    }
    if ((unsigned long )(state->config)->tuner_get_status != (unsigned long )((int (* )(struct dvb_frontend * ,
                                                                                                   u32 * ))0)) {
      tmp___85 = (*((state->config)->tuner_get_status))(fe, & reg);
      if (tmp___85 < 0) {
        goto err_gateoff;
      } else {
      }
    } else {
    }
    if (reg != 0U) {
      if (verbose != 0U && verbose > 3U) {
        printk("\v%s: Tuner phase locked\n", "stv090x_srate_srch_coarse");
      } else
      if (verbose > 1U && verbose > 3U) {
        printk("\r%s: Tuner phase locked\n", "stv090x_srate_srch_coarse");
      } else
      if (verbose > 2U && verbose > 3U) {
        printk("\016%s: Tuner phase locked\n", "stv090x_srate_srch_coarse");
      } else
      if (verbose > 3U && verbose > 3U) {
        printk("\017%s: Tuner phase locked\n", "stv090x_srate_srch_coarse");
      } else
      if (verbose > 3U) {
        printk("Tuner phase locked");
      } else
      if (verbose != 0U && verbose > 3U) {
        printk("\v%s: Tuner unlocked\n", "stv090x_srate_srch_coarse");
      } else
      if (verbose > 1U && verbose > 3U) {
        printk("\r%s: Tuner unlocked\n", "stv090x_srate_srch_coarse");
      } else
      if (verbose > 2U && verbose > 3U) {
        printk("\016%s: Tuner unlocked\n", "stv090x_srate_srch_coarse");
      } else
      if (verbose > 3U && verbose > 3U) {
        printk("\017%s: Tuner unlocked\n", "stv090x_srate_srch_coarse");
      } else
      if (verbose > 3U) {
        printk("Tuner unlocked");
      } else {
      }
    } else {
    }
    tmp___86 = stv090x_i2c_gate_ctrl(state, 0);
    if (tmp___86 < 0) {
      goto err;
    } else {
    }
  } else {
  }
  ldv_24219: ;
  if (tmg_lock == 0 && cur_step < steps) {
    goto ldv_24218;
  } else {
  }
  if (tmg_lock == 0) {
    srate_coarse = 0U;
  } else {
    srate_coarse = stv090x_get_srate(state, (u32 )(state->internal)->mclk);
  }
  return (srate_coarse);
  err_gateoff:
  stv090x_i2c_gate_ctrl(state, 0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_srate_srch_coarse");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_srate_srch_coarse");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_srate_srch_coarse");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_srate_srch_coarse");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (4294967295U);
}
}
static u32 stv090x_srate_srch_fine(struct stv090x_state *state )
{
  u32 srate_coarse ;
  u32 freq_coarse ;
  u32 sym ;
  u32 reg ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  int tmp___23 ;
  int tmp___24 ;
  int tmp___25 ;
  int tmp___26 ;
  int tmp___27 ;
  int tmp___28 ;
  int tmp___29 ;
  int tmp___30 ;
  int tmp___31 ;
  int tmp___32 ;
  int tmp___33 ;
  int tmp___34 ;
  int tmp___35 ;
  int tmp___36 ;
  int tmp___37 ;
  int tmp___38 ;
  int tmp___39 ;
  int tmp___40 ;
  int tmp___41 ;
  int tmp___42 ;
  int tmp___43 ;
  int tmp___44 ;
  int tmp___45 ;
  int tmp___46 ;
  int tmp___47 ;
  int tmp___48 ;
  int tmp___49 ;
  int tmp___50 ;
  int tmp___51 ;
  int tmp___52 ;
  int tmp___53 ;
  int tmp___54 ;
  int tmp___55 ;
  int tmp___56 ;
  int tmp___57 ;
  int tmp___58 ;
  int tmp___59 ;
  int tmp___60 ;
  int tmp___61 ;
  int tmp___62 ;
  int tmp___63 ;
  int tmp___64 ;
  int tmp___65 ;
  int tmp___66 ;
  int tmp___67 ;
  int tmp___68 ;
  int tmp___69 ;
  int tmp___70 ;
  int tmp___71 ;
  int tmp___72 ;
  int tmp___73 ;
  int tmp___74 ;
  int tmp___75 ;
  int tmp___76 ;
  int tmp___77 ;
  int tmp___78 ;
  int tmp___79 ;
  int tmp___80 ;
  int tmp___81 ;
  int tmp___82 ;
  {
  srate_coarse = stv090x_get_srate(state, (u32 )(state->internal)->mclk);
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 62028U);
    tmp___1 = tmp << 8;
  } else {
    tmp___0 = stv090x_read_reg(state, 62540U);
    tmp___1 = tmp___0 << 8;
  }
  freq_coarse = (u32 )tmp___1;
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_read_reg(state, 62029U);
    tmp___4 = tmp___2;
  } else {
    tmp___3 = stv090x_read_reg(state, 62541U);
    tmp___4 = tmp___3;
  }
  freq_coarse = (u32 )tmp___4 | freq_coarse;
  sym = (srate_coarse / 10U) * 13U;
  if (state->srate > sym) {
    srate_coarse = 0U;
  } else {
    if ((unsigned int )state->demod == 2U) {
      tmp___5 = stv090x_write_reg(state, 61974U, 31);
      tmp___7 = tmp___5 < 0;
    } else {
      tmp___6 = stv090x_write_reg(state, 62486U, 31);
      tmp___7 = tmp___6 < 0;
    }
    if (tmp___7) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___8 = stv090x_write_reg(state, 62042U, 193);
      tmp___10 = tmp___8 < 0;
    } else {
      tmp___9 = stv090x_write_reg(state, 62554U, 193);
      tmp___10 = tmp___9 < 0;
    }
    if (tmp___10) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___11 = stv090x_write_reg(state, 62035U, 32);
      tmp___13 = tmp___11 < 0;
    } else {
      tmp___12 = stv090x_write_reg(state, 62547U, 32);
      tmp___13 = tmp___12 < 0;
    }
    if (tmp___13) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___14 = stv090x_write_reg(state, 62036U, 0);
      tmp___16 = tmp___14 < 0;
    } else {
      tmp___15 = stv090x_write_reg(state, 62548U, 0);
      tmp___16 = tmp___15 < 0;
    }
    if (tmp___16) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___17 = stv090x_write_reg(state, 62032U, 210);
      tmp___19 = tmp___17 < 0;
    } else {
      tmp___18 = stv090x_write_reg(state, 62544U, 210);
      tmp___19 = tmp___18 < 0;
    }
    if (tmp___19) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___20 = stv090x_read_reg(state, 61972U);
      tmp___22 = tmp___20;
    } else {
      tmp___21 = stv090x_read_reg(state, 62484U);
      tmp___22 = tmp___21;
    }
    reg = (u32 )tmp___22;
    reg = reg & 4294967287U;
    if ((unsigned int )state->demod == 2U) {
      tmp___23 = stv090x_write_reg(state, 61972U, (int )((u8 )reg));
      tmp___25 = tmp___23 < 0;
    } else {
      tmp___24 = stv090x_write_reg(state, 62484U, (int )((u8 )reg));
      tmp___25 = tmp___24 < 0;
    }
    if (tmp___25) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___26 = stv090x_write_reg(state, 61997U, 56);
      tmp___28 = tmp___26 < 0;
    } else {
      tmp___27 = stv090x_write_reg(state, 62509U, 56);
      tmp___28 = tmp___27 < 0;
    }
    if (tmp___28) {
      goto err;
    } else {
    }
    if ((state->internal)->dev_ver > 47U) {
      if ((unsigned int )state->demod == 2U) {
        tmp___32 = stv090x_write_reg(state, 62013U, 121);
        tmp___34 = tmp___32 < 0;
      } else {
        tmp___33 = stv090x_write_reg(state, 62525U, 121);
        tmp___34 = tmp___33 < 0;
      }
      if (tmp___34) {
        goto err;
      } else
      if ((state->internal)->dev_ver > 31U) {
        if ((unsigned int )state->demod == 2U) {
          tmp___29 = stv090x_write_reg(state, 62013U, 73);
          tmp___31 = tmp___29 < 0;
        } else {
          tmp___30 = stv090x_write_reg(state, 62525U, 73);
          tmp___31 = tmp___30 < 0;
        }
        if (tmp___31) {
          goto err;
        } else {
        }
      } else {
      }
    } else {
    }
    if (srate_coarse > 3000000U) {
      sym = (srate_coarse / 10U) * 13U;
      sym = (sym / 1000U) * 65536U;
      sym = sym / (u32 )((state->internal)->mclk / 1000);
      if ((unsigned int )state->demod == 2U) {
        tmp___35 = stv090x_write_reg(state, 62048U, (int )((u8 )(sym >> 8)) & 127);
        tmp___37 = tmp___35 < 0;
      } else {
        tmp___36 = stv090x_write_reg(state, 62560U, (int )((u8 )(sym >> 8)) & 127);
        tmp___37 = tmp___36 < 0;
      }
      if (tmp___37) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___38 = stv090x_write_reg(state, 62049U, (int )((u8 )sym));
        tmp___40 = tmp___38 < 0;
      } else {
        tmp___39 = stv090x_write_reg(state, 62561U, (int )((u8 )sym));
        tmp___40 = tmp___39 < 0;
      }
      if (tmp___40) {
        goto err;
      } else {
      }
      sym = (srate_coarse / 13U) * 10U;
      sym = (sym / 1000U) * 65536U;
      sym = sym / (u32 )((state->internal)->mclk / 1000);
      if ((unsigned int )state->demod == 2U) {
        tmp___41 = stv090x_write_reg(state, 62050U, (int )((u8 )(sym >> 8)) & 127);
        tmp___43 = tmp___41 < 0;
      } else {
        tmp___42 = stv090x_write_reg(state, 62562U, (int )((u8 )(sym >> 8)) & 127);
        tmp___43 = tmp___42 < 0;
      }
      if (tmp___43) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___44 = stv090x_write_reg(state, 62051U, (int )((u8 )sym));
        tmp___46 = tmp___44 < 0;
      } else {
        tmp___45 = stv090x_write_reg(state, 62563U, (int )((u8 )sym));
        tmp___46 = tmp___45 < 0;
      }
      if (tmp___46) {
        goto err;
      } else {
      }
      sym = (srate_coarse / 1000U) * 65536U;
      sym = sym / (u32 )((state->internal)->mclk / 1000);
      if ((unsigned int )state->demod == 2U) {
        tmp___47 = stv090x_write_reg(state, 62046U, (int )((u8 )(sym >> 8)));
        tmp___49 = tmp___47 < 0;
      } else {
        tmp___48 = stv090x_write_reg(state, 62558U, (int )((u8 )(sym >> 8)));
        tmp___49 = tmp___48 < 0;
      }
      if (tmp___49) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___50 = stv090x_write_reg(state, 62047U, (int )((u8 )sym));
        tmp___52 = tmp___50 < 0;
      } else {
        tmp___51 = stv090x_write_reg(state, 62559U, (int )((u8 )sym));
        tmp___52 = tmp___51 < 0;
      }
      if (tmp___52) {
        goto err;
      } else {
      }
    } else {
      sym = (srate_coarse / 10U) * 13U;
      sym = (sym / 100U) * 65536U;
      sym = sym / (u32 )((state->internal)->mclk / 100);
      if ((unsigned int )state->demod == 2U) {
        tmp___53 = stv090x_write_reg(state, 62048U, (int )((u8 )(sym >> 8)) & 127);
        tmp___55 = tmp___53 < 0;
      } else {
        tmp___54 = stv090x_write_reg(state, 62560U, (int )((u8 )(sym >> 8)) & 127);
        tmp___55 = tmp___54 < 0;
      }
      if (tmp___55) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___56 = stv090x_write_reg(state, 62049U, (int )((u8 )sym));
        tmp___58 = tmp___56 < 0;
      } else {
        tmp___57 = stv090x_write_reg(state, 62561U, (int )((u8 )sym));
        tmp___58 = tmp___57 < 0;
      }
      if (tmp___58) {
        goto err;
      } else {
      }
      sym = (srate_coarse / 14U) * 10U;
      sym = (sym / 100U) * 65536U;
      sym = sym / (u32 )((state->internal)->mclk / 100);
      if ((unsigned int )state->demod == 2U) {
        tmp___59 = stv090x_write_reg(state, 62050U, (int )((u8 )(sym >> 8)) & 127);
        tmp___61 = tmp___59 < 0;
      } else {
        tmp___60 = stv090x_write_reg(state, 62562U, (int )((u8 )(sym >> 8)) & 127);
        tmp___61 = tmp___60 < 0;
      }
      if (tmp___61) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___62 = stv090x_write_reg(state, 62051U, (int )((u8 )sym));
        tmp___64 = tmp___62 < 0;
      } else {
        tmp___63 = stv090x_write_reg(state, 62563U, (int )((u8 )sym));
        tmp___64 = tmp___63 < 0;
      }
      if (tmp___64) {
        goto err;
      } else {
      }
      sym = (srate_coarse / 100U) * 65536U;
      sym = sym / (u32 )((state->internal)->mclk / 100);
      if ((unsigned int )state->demod == 2U) {
        tmp___65 = stv090x_write_reg(state, 62046U, (int )((u8 )(sym >> 8)));
        tmp___67 = tmp___65 < 0;
      } else {
        tmp___66 = stv090x_write_reg(state, 62558U, (int )((u8 )(sym >> 8)));
        tmp___67 = tmp___66 < 0;
      }
      if (tmp___67) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___68 = stv090x_write_reg(state, 62047U, (int )((u8 )sym));
        tmp___70 = tmp___68 < 0;
      } else {
        tmp___69 = stv090x_write_reg(state, 62559U, (int )((u8 )sym));
        tmp___70 = tmp___69 < 0;
      }
      if (tmp___70) {
        goto err;
      } else {
      }
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___71 = stv090x_write_reg(state, 61975U, 32);
      tmp___73 = tmp___71 < 0;
    } else {
      tmp___72 = stv090x_write_reg(state, 62487U, 32);
      tmp___73 = tmp___72 < 0;
    }
    if (tmp___73) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___74 = stv090x_write_reg(state, 62024U, (int )((u8 )(freq_coarse >> 8)));
      tmp___76 = tmp___74 < 0;
    } else {
      tmp___75 = stv090x_write_reg(state, 62536U, (int )((u8 )(freq_coarse >> 8)));
      tmp___76 = tmp___75 < 0;
    }
    if (tmp___76) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___77 = stv090x_write_reg(state, 62025U, (int )((u8 )freq_coarse));
      tmp___79 = tmp___77 < 0;
    } else {
      tmp___78 = stv090x_write_reg(state, 62537U, (int )((u8 )freq_coarse));
      tmp___79 = tmp___78 < 0;
    }
    if (tmp___79) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___80 = stv090x_write_reg(state, 61974U, 21);
      tmp___82 = tmp___80 < 0;
    } else {
      tmp___81 = stv090x_write_reg(state, 62486U, 21);
      tmp___82 = tmp___81 < 0;
    }
    if (tmp___82) {
      goto err;
    } else {
    }
  }
  return (srate_coarse);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_srate_srch_fine");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_srate_srch_fine");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_srate_srch_fine");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_srate_srch_fine");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (4294967295U);
}
}
static int stv090x_get_dmdlock(struct stv090x_state *state , s32 timeout )
{
  s32 timer ;
  s32 lock ;
  u32 reg ;
  u8 stat ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  {
  timer = 0;
  lock = 0;
  goto ldv_24246;
  ldv_24245: ;
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 61979U);
    tmp___1 = tmp;
  } else {
    tmp___0 = stv090x_read_reg(state, 62491U);
    tmp___1 = tmp___0;
  }
  reg = (u32 )tmp___1;
  stat = (unsigned int )((u8 )(reg >> 5)) & 3U;
  switch ((int )stat) {
  case 0: ;
  case 1: ;
  default: ;
  if (verbose != 0U && verbose > 3U) {
    printk("\v%s: Demodulator searching ..\n", "stv090x_get_dmdlock");
  } else
  if (verbose > 1U && verbose > 3U) {
    printk("\r%s: Demodulator searching ..\n", "stv090x_get_dmdlock");
  } else
  if (verbose > 2U && verbose > 3U) {
    printk("\016%s: Demodulator searching ..\n", "stv090x_get_dmdlock");
  } else
  if (verbose > 3U && verbose > 3U) {
    printk("\017%s: Demodulator searching ..\n", "stv090x_get_dmdlock");
  } else
  if (verbose > 3U) {
    printk("Demodulator searching ..");
  } else {
  }
  lock = 0;
  goto ldv_24242;
  case 2: ;
  case 3: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_read_reg(state, 61970U);
    tmp___4 = tmp___2;
  } else {
    tmp___3 = stv090x_read_reg(state, 62482U);
    tmp___4 = tmp___3;
  }
  reg = (u32 )tmp___4;
  lock = (s32 )(reg >> 3) & 1;
  goto ldv_24242;
  }
  ldv_24242: ;
  if (lock == 0) {
    msleep(10U);
  } else
  if (verbose != 0U && verbose > 3U) {
    printk("\v%s: Demodulator acquired LOCK\n", "stv090x_get_dmdlock");
  } else
  if (verbose > 1U && verbose > 3U) {
    printk("\r%s: Demodulator acquired LOCK\n", "stv090x_get_dmdlock");
  } else
  if (verbose > 2U && verbose > 3U) {
    printk("\016%s: Demodulator acquired LOCK\n", "stv090x_get_dmdlock");
  } else
  if (verbose > 3U && verbose > 3U) {
    printk("\017%s: Demodulator acquired LOCK\n", "stv090x_get_dmdlock");
  } else
  if (verbose > 3U) {
    printk("Demodulator acquired LOCK");
  } else {
  }
  timer = timer + 10;
  ldv_24246: ;
  if (timer < timeout && lock == 0) {
    goto ldv_24245;
  } else {
  }
  return (lock);
}
}
static int stv090x_blind_search(struct stv090x_state *state )
{
  u32 agc2 ;
  u32 reg ;
  u32 srate_coarse ;
  s32 cpt_fail ;
  s32 agc2_ovflw ;
  s32 i ;
  u8 k_ref ;
  u8 k_max ;
  u8 k_min ;
  int coarse_fail ;
  int lock ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  int tmp___23 ;
  int tmp___24 ;
  int tmp___25 ;
  int tmp___26 ;
  int tmp___27 ;
  int tmp___28 ;
  int tmp___29 ;
  int tmp___30 ;
  int tmp___31 ;
  int tmp___32 ;
  u32 tmp___33 ;
  {
  coarse_fail = 0;
  k_max = 110U;
  k_min = 10U;
  tmp = stv090x_get_agc2_min_level(state);
  agc2 = (u32 )tmp;
  if (((state->internal)->dev_ver <= 32U ? 700U : 1400U) < agc2) {
    lock = 0;
  } else {
    if ((state->internal)->dev_ver <= 32U) {
      if ((unsigned int )state->demod == 2U) {
        tmp___3 = stv090x_write_reg(state, 62008U, 196);
        tmp___5 = tmp___3 < 0;
      } else {
        tmp___4 = stv090x_write_reg(state, 62520U, 196);
        tmp___5 = tmp___4 < 0;
      }
      if (tmp___5) {
        goto err;
      } else {
        if ((unsigned int )state->demod == 2U) {
          tmp___0 = stv090x_write_reg(state, 62008U, 6);
          tmp___2 = tmp___0 < 0;
        } else {
          tmp___1 = stv090x_write_reg(state, 62520U, 6);
          tmp___2 = tmp___1 < 0;
        }
        if (tmp___2) {
          goto err;
        } else {
        }
      }
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___6 = stv090x_write_reg(state, 62034U, 68);
      tmp___8 = tmp___6 < 0;
    } else {
      tmp___7 = stv090x_write_reg(state, 62546U, 68);
      tmp___8 = tmp___7 < 0;
    }
    if (tmp___8) {
      goto err;
    } else {
    }
    if ((state->internal)->dev_ver > 31U) {
      if ((unsigned int )state->demod == 2U) {
        tmp___9 = stv090x_write_reg(state, 62063U, 65);
        tmp___11 = tmp___9 < 0;
      } else {
        tmp___10 = stv090x_write_reg(state, 62575U, 65);
        tmp___11 = tmp___10 < 0;
      }
      if (tmp___11) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___12 = stv090x_write_reg(state, 62168U, 65);
        tmp___14 = tmp___12 < 0;
      } else {
        tmp___13 = stv090x_write_reg(state, 62680U, 65);
        tmp___14 = tmp___13 < 0;
      }
      if (tmp___14) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___15 = stv090x_write_reg(state, 62258U, 130);
        tmp___17 = tmp___15 < 0;
      } else {
        tmp___16 = stv090x_write_reg(state, 62770U, 130);
        tmp___17 = tmp___16 < 0;
      }
      if (tmp___17) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___18 = stv090x_write_reg(state, 62269U, 0);
        tmp___20 = tmp___18 < 0;
      } else {
        tmp___19 = stv090x_write_reg(state, 62781U, 0);
        tmp___20 = tmp___19 < 0;
      }
      if (tmp___20) {
        goto err;
      } else {
      }
    } else {
    }
    k_ref = k_max;
    ldv_24266: ;
    if ((unsigned int )state->demod == 2U) {
      tmp___21 = stv090x_write_reg(state, 62040U, (int )k_ref);
      tmp___23 = tmp___21 < 0;
    } else {
      tmp___22 = stv090x_write_reg(state, 62552U, (int )k_ref);
      tmp___23 = tmp___22 < 0;
    }
    if (tmp___23) {
      goto err;
    } else {
    }
    tmp___33 = stv090x_srate_srch_coarse(state);
    if (tmp___33 != 0U) {
      srate_coarse = stv090x_srate_srch_fine(state);
      if (srate_coarse != 0U) {
        stv090x_get_lock_tmg(state);
        lock = stv090x_get_dmdlock(state, state->DemodTimeout);
      } else {
        lock = 0;
      }
    } else {
      cpt_fail = 0;
      agc2_ovflw = 0;
      i = 0;
      goto ldv_24264;
      ldv_24263: ;
      if ((unsigned int )state->demod == 2U) {
        tmp___24 = stv090x_read_reg(state, 62006U);
        tmp___26 = tmp___24 << 8;
      } else {
        tmp___25 = stv090x_read_reg(state, 62518U);
        tmp___26 = tmp___25 << 8;
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___27 = stv090x_read_reg(state, 62007U);
        tmp___29 = tmp___27;
      } else {
        tmp___28 = stv090x_read_reg(state, 62519U);
        tmp___29 = tmp___28;
      }
      agc2 = (u32 )(tmp___26 | tmp___29) + agc2;
      if (agc2 > 65279U) {
        agc2_ovflw = agc2_ovflw + 1;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___30 = stv090x_read_reg(state, 61971U);
        tmp___32 = tmp___30;
      } else {
        tmp___31 = stv090x_read_reg(state, 62483U);
        tmp___32 = tmp___31;
      }
      reg = (u32 )tmp___32;
      if ((reg & 2U) != 0U && (reg & 128U) != 0U) {
        cpt_fail = cpt_fail + 1;
      } else {
      }
      i = i + 1;
      ldv_24264: ;
      if (i <= 9) {
        goto ldv_24263;
      } else {
      }
      if (cpt_fail > 7 || agc2_ovflw > 7) {
        coarse_fail = 1;
      } else {
      }
      lock = 0;
    }
    k_ref = (unsigned int )k_ref + 236U;
    if (((int )k_ref >= (int )k_min && lock == 0) && coarse_fail == 0) {
      goto ldv_24266;
    } else {
    }
  }
  return (lock);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_blind_search");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_blind_search");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_blind_search");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_blind_search");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_chk_tmg(struct stv090x_state *state )
{
  u32 reg ;
  s32 tmg_cpt ;
  s32 i ;
  u8 freq ;
  u8 tmg_thh ;
  u8 tmg_thl ;
  int tmg_lock ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  int tmp___23 ;
  int tmp___24 ;
  int tmp___25 ;
  int tmp___26 ;
  int tmp___27 ;
  int tmp___28 ;
  int tmp___29 ;
  int tmp___30 ;
  int tmp___31 ;
  int tmp___32 ;
  int tmp___33 ;
  int tmp___34 ;
  int tmp___35 ;
  int tmp___36 ;
  int tmp___37 ;
  int tmp___38 ;
  int tmp___39 ;
  int tmp___40 ;
  int tmp___41 ;
  int tmp___42 ;
  int tmp___43 ;
  int tmp___44 ;
  int tmp___45 ;
  int tmp___46 ;
  int tmp___47 ;
  int tmp___48 ;
  int tmp___49 ;
  int tmp___50 ;
  int tmp___51 ;
  int tmp___52 ;
  int tmp___53 ;
  int tmp___54 ;
  int tmp___55 ;
  int tmp___56 ;
  int tmp___57 ;
  int tmp___58 ;
  {
  tmg_cpt = 0;
  tmg_lock = 0;
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 62013U);
    freq = (u8 )tmp;
  } else {
    tmp___0 = stv090x_read_reg(state, 62525U);
    freq = (u8 )tmp___0;
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___1 = stv090x_read_reg(state, 62035U);
    tmg_thh = (u8 )tmp___1;
  } else {
    tmp___2 = stv090x_read_reg(state, 62547U);
    tmg_thh = (u8 )tmp___2;
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___3 = stv090x_read_reg(state, 62036U);
    tmg_thl = (u8 )tmp___3;
  } else {
    tmp___4 = stv090x_read_reg(state, 62548U);
    tmg_thl = (u8 )tmp___4;
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___5 = stv090x_write_reg(state, 62035U, 32);
    tmp___7 = tmp___5 < 0;
  } else {
    tmp___6 = stv090x_write_reg(state, 62547U, 32);
    tmp___7 = tmp___6 < 0;
  }
  if (tmp___7) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___8 = stv090x_write_reg(state, 62036U, 0);
    tmp___10 = tmp___8 < 0;
  } else {
    tmp___9 = stv090x_write_reg(state, 62548U, 0);
    tmp___10 = tmp___9 < 0;
  }
  if (tmp___10) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___11 = stv090x_read_reg(state, 61972U);
    tmp___13 = tmp___11;
  } else {
    tmp___12 = stv090x_read_reg(state, 62484U);
    tmp___13 = tmp___12;
  }
  reg = (u32 )tmp___13;
  reg = reg & 4294967287U;
  if ((unsigned int )state->demod == 2U) {
    tmp___14 = stv090x_write_reg(state, 61972U, (int )((u8 )reg));
    tmp___16 = tmp___14 < 0;
  } else {
    tmp___15 = stv090x_write_reg(state, 62484U, (int )((u8 )reg));
    tmp___16 = tmp___15 < 0;
  }
  if (tmp___16) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___17 = stv090x_write_reg(state, 62033U, 128);
    tmp___19 = tmp___17 < 0;
  } else {
    tmp___18 = stv090x_write_reg(state, 62545U, 128);
    tmp___19 = tmp___18 < 0;
  }
  if (tmp___19) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___20 = stv090x_write_reg(state, 62034U, 64);
    tmp___22 = tmp___20 < 0;
  } else {
    tmp___21 = stv090x_write_reg(state, 62546U, 64);
    tmp___22 = tmp___21 < 0;
  }
  if (tmp___22) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___23 = stv090x_write_reg(state, 62013U, 0);
    tmp___25 = tmp___23 < 0;
  } else {
    tmp___24 = stv090x_write_reg(state, 62525U, 0);
    tmp___25 = tmp___24 < 0;
  }
  if (tmp___25) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___26 = stv090x_write_reg(state, 62024U, 0);
    tmp___28 = tmp___26 < 0;
  } else {
    tmp___27 = stv090x_write_reg(state, 62536U, 0);
    tmp___28 = tmp___27 < 0;
  }
  if (tmp___28) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___29 = stv090x_write_reg(state, 62025U, 0);
    tmp___31 = tmp___29 < 0;
  } else {
    tmp___30 = stv090x_write_reg(state, 62537U, 0);
    tmp___31 = tmp___30 < 0;
  }
  if (tmp___31) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___32 = stv090x_write_reg(state, 61997U, 101);
    tmp___34 = tmp___32 < 0;
  } else {
    tmp___33 = stv090x_write_reg(state, 62509U, 101);
    tmp___34 = tmp___33 < 0;
  }
  if (tmp___34) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___35 = stv090x_write_reg(state, 61974U, 24);
    tmp___37 = tmp___35 < 0;
  } else {
    tmp___36 = stv090x_write_reg(state, 62486U, 24);
    tmp___37 = tmp___36 < 0;
  }
  if (tmp___37) {
    goto err;
  } else {
  }
  msleep(10U);
  i = 0;
  goto ldv_24281;
  ldv_24280: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___38 = stv090x_read_reg(state, 61970U);
    tmp___40 = tmp___38;
  } else {
    tmp___39 = stv090x_read_reg(state, 62482U);
    tmp___40 = tmp___39;
  }
  reg = (u32 )tmp___40;
  if (((reg >> 5) & 3U) > 1U) {
    tmg_cpt = tmg_cpt + 1;
  } else {
  }
  msleep(1U);
  i = i + 1;
  ldv_24281: ;
  if (i <= 9) {
    goto ldv_24280;
  } else {
  }
  if (tmg_cpt > 2) {
    tmg_lock = 1;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___41 = stv090x_write_reg(state, 61997U, 56);
    tmp___43 = tmp___41 < 0;
  } else {
    tmp___42 = stv090x_write_reg(state, 62509U, 56);
    tmp___43 = tmp___42 < 0;
  }
  if (tmp___43) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___44 = stv090x_write_reg(state, 62033U, 136);
    tmp___46 = tmp___44 < 0;
  } else {
    tmp___45 = stv090x_write_reg(state, 62545U, 136);
    tmp___46 = tmp___45 < 0;
  }
  if (tmp___46) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___47 = stv090x_write_reg(state, 62034U, 104);
    tmp___49 = tmp___47 < 0;
  } else {
    tmp___48 = stv090x_write_reg(state, 62546U, 104);
    tmp___49 = tmp___48 < 0;
  }
  if (tmp___49) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___50 = stv090x_write_reg(state, 62013U, (int )freq);
    tmp___52 = tmp___50 < 0;
  } else {
    tmp___51 = stv090x_write_reg(state, 62525U, (int )freq);
    tmp___52 = tmp___51 < 0;
  }
  if (tmp___52) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___53 = stv090x_write_reg(state, 62035U, (int )tmg_thh);
    tmp___55 = tmp___53 < 0;
  } else {
    tmp___54 = stv090x_write_reg(state, 62547U, (int )tmg_thh);
    tmp___55 = tmp___54 < 0;
  }
  if (tmp___55) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___56 = stv090x_write_reg(state, 62036U, (int )tmg_thl);
    tmp___58 = tmp___56 < 0;
  } else {
    tmp___57 = stv090x_write_reg(state, 62548U, (int )tmg_thl);
    tmp___58 = tmp___57 < 0;
  }
  if (tmp___58) {
    goto err;
  } else {
  }
  return (tmg_lock);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_chk_tmg");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_chk_tmg");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_chk_tmg");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_chk_tmg");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_get_coldlock(struct stv090x_state *state , s32 timeout_dmd )
{
  struct dvb_frontend *fe ;
  u32 reg ;
  s32 car_step ;
  s32 steps ;
  s32 cur_step ;
  s32 dir ;
  s32 freq ;
  s32 timeout_lock ;
  int lock ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  u32 tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  int tmp___23 ;
  int tmp___24 ;
  int tmp___25 ;
  {
  fe = & state->frontend;
  lock = 0;
  if (state->srate > 9999999U) {
    timeout_lock = timeout_dmd / 3;
  } else {
    timeout_lock = timeout_dmd / 2;
  }
  lock = stv090x_get_dmdlock(state, timeout_lock);
  if (lock == 0) {
    if (state->srate > 9999999U) {
      tmp___5 = stv090x_chk_tmg(state);
      if (tmp___5 != 0) {
        if ((unsigned int )state->demod == 2U) {
          tmp = stv090x_write_reg(state, 61974U, 31);
          tmp___1 = tmp < 0;
        } else {
          tmp___0 = stv090x_write_reg(state, 62486U, 31);
          tmp___1 = tmp___0 < 0;
        }
        if (tmp___1) {
          goto err;
        } else {
        }
        if ((unsigned int )state->demod == 2U) {
          tmp___2 = stv090x_write_reg(state, 61974U, 21);
          tmp___4 = tmp___2 < 0;
        } else {
          tmp___3 = stv090x_write_reg(state, 62486U, 21);
          tmp___4 = tmp___3 < 0;
        }
        if (tmp___4) {
          goto err;
        } else {
        }
        lock = stv090x_get_dmdlock(state, timeout_dmd);
      } else {
        lock = 0;
      }
    } else {
      if (state->srate <= 4000000U) {
        car_step = 1000;
      } else
      if (state->srate <= 7000000U) {
        car_step = 2000;
      } else
      if (state->srate <= 10000000U) {
        car_step = 3000;
      } else {
        car_step = 5000;
      }
      steps = (state->search_range / 1000) / car_step;
      steps = steps / 2;
      steps = (steps + 1) * 2;
      if (steps < 0) {
        steps = 2;
      } else
      if (steps > 12) {
        steps = 12;
      } else {
      }
      cur_step = 1;
      dir = 1;
      if (lock == 0) {
        freq = (s32 )state->frequency;
        tmp___6 = stv090x_car_width(state->srate, state->rolloff);
        state->tuner_bw = (s32 )(tmp___6 + state->srate);
        goto ldv_24301;
        ldv_24300: ;
        if (dir > 0) {
          freq = cur_step * car_step + freq;
        } else {
          freq = freq - cur_step * car_step;
        }
        tmp___7 = stv090x_i2c_gate_ctrl(state, 1);
        if (tmp___7 < 0) {
          goto err;
        } else {
        }
        if ((unsigned long )(state->config)->tuner_set_frequency != (unsigned long )((int (* )(struct dvb_frontend * ,
                                                                                                          u32 ))0)) {
          tmp___8 = (*((state->config)->tuner_set_frequency))(fe, (u32 )freq);
          if (tmp___8 < 0) {
            goto err_gateoff;
          } else {
          }
        } else {
        }
        if ((unsigned long )(state->config)->tuner_set_bandwidth != (unsigned long )((int (* )(struct dvb_frontend * ,
                                                                                                          u32 ))0)) {
          tmp___9 = (*((state->config)->tuner_set_bandwidth))(fe, (u32 )state->tuner_bw);
          if (tmp___9 < 0) {
            goto err_gateoff;
          } else {
          }
        } else {
        }
        tmp___10 = stv090x_i2c_gate_ctrl(state, 0);
        if (tmp___10 < 0) {
          goto err;
        } else {
        }
        msleep(50U);
        tmp___11 = stv090x_i2c_gate_ctrl(state, 1);
        if (tmp___11 < 0) {
          goto err;
        } else {
        }
        if ((unsigned long )(state->config)->tuner_get_status != (unsigned long )((int (* )(struct dvb_frontend * ,
                                                                                                       u32 * ))0)) {
          tmp___12 = (*((state->config)->tuner_get_status))(fe, & reg);
          if (tmp___12 < 0) {
            goto err_gateoff;
          } else {
          }
        } else {
        }
        if (reg != 0U) {
          if (verbose != 0U && verbose > 3U) {
            printk("\v%s: Tuner phase locked\n", "stv090x_get_coldlock");
          } else
          if (verbose > 1U && verbose > 3U) {
            printk("\r%s: Tuner phase locked\n", "stv090x_get_coldlock");
          } else
          if (verbose > 2U && verbose > 3U) {
            printk("\016%s: Tuner phase locked\n", "stv090x_get_coldlock");
          } else
          if (verbose > 3U && verbose > 3U) {
            printk("\017%s: Tuner phase locked\n", "stv090x_get_coldlock");
          } else
          if (verbose > 3U) {
            printk("Tuner phase locked");
          } else
          if (verbose != 0U && verbose > 3U) {
            printk("\v%s: Tuner unlocked\n", "stv090x_get_coldlock");
          } else
          if (verbose > 1U && verbose > 3U) {
            printk("\r%s: Tuner unlocked\n", "stv090x_get_coldlock");
          } else
          if (verbose > 2U && verbose > 3U) {
            printk("\016%s: Tuner unlocked\n", "stv090x_get_coldlock");
          } else
          if (verbose > 3U && verbose > 3U) {
            printk("\017%s: Tuner unlocked\n", "stv090x_get_coldlock");
          } else
          if (verbose > 3U) {
            printk("Tuner unlocked");
          } else {
          }
        } else {
        }
        tmp___13 = stv090x_i2c_gate_ctrl(state, 0);
        if (tmp___13 < 0) {
          goto err;
        } else {
        }
        if ((unsigned int )state->demod == 2U) {
          stv090x_write_reg(state, 61974U, 28);
        } else {
          stv090x_write_reg(state, 62486U, 28);
        }
        if ((unsigned int )state->demod == 2U) {
          tmp___14 = stv090x_write_reg(state, 62024U, 0);
          tmp___16 = tmp___14 < 0;
        } else {
          tmp___15 = stv090x_write_reg(state, 62536U, 0);
          tmp___16 = tmp___15 < 0;
        }
        if (tmp___16) {
          goto err;
        } else {
        }
        if ((unsigned int )state->demod == 2U) {
          tmp___17 = stv090x_write_reg(state, 62025U, 0);
          tmp___19 = tmp___17 < 0;
        } else {
          tmp___18 = stv090x_write_reg(state, 62537U, 0);
          tmp___19 = tmp___18 < 0;
        }
        if (tmp___19) {
          goto err;
        } else {
        }
        if ((unsigned int )state->demod == 2U) {
          tmp___20 = stv090x_write_reg(state, 61974U, 31);
          tmp___22 = tmp___20 < 0;
        } else {
          tmp___21 = stv090x_write_reg(state, 62486U, 31);
          tmp___22 = tmp___21 < 0;
        }
        if (tmp___22) {
          goto err;
        } else {
        }
        if ((unsigned int )state->demod == 2U) {
          tmp___23 = stv090x_write_reg(state, 61974U, 21);
          tmp___25 = tmp___23 < 0;
        } else {
          tmp___24 = stv090x_write_reg(state, 62486U, 21);
          tmp___25 = tmp___24 < 0;
        }
        if (tmp___25) {
          goto err;
        } else {
        }
        lock = stv090x_get_dmdlock(state, timeout_dmd / 3);
        dir = - dir;
        cur_step = cur_step + 1;
        ldv_24301: ;
        if (cur_step <= steps && lock == 0) {
          goto ldv_24300;
        } else {
        }
      } else {
      }
    }
  } else {
  }
  return (lock);
  err_gateoff:
  stv090x_i2c_gate_ctrl(state, 0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_get_coldlock");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_get_coldlock");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_get_coldlock");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_get_coldlock");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_get_loop_params(struct stv090x_state *state , s32 *freq_inc , s32 *timeout_sw ,
                                   s32 *steps )
{
  s32 timeout ;
  s32 inc ;
  s32 steps_max ;
  s32 srate ;
  s32 car_max ;
  {
  srate = (s32 )state->srate;
  car_max = state->search_range / 1000;
  car_max = car_max / 10 + car_max;
  car_max = (car_max / 2) * 65536;
  car_max = car_max / ((state->internal)->mclk / 1000);
  if (car_max > 16384) {
    car_max = 16384;
  } else {
  }
  inc = srate;
  inc = inc / ((state->internal)->mclk / 1000);
  inc = inc * 256;
  inc = inc * 256;
  inc = inc / 1000;
  switch ((unsigned int )state->search_mode) {
  case 1U: ;
  case 0U:
  inc = inc * 3;
  timeout = 20;
  goto ldv_24316;
  case 2U:
  inc = inc * 4;
  timeout = 25;
  goto ldv_24316;
  case 3U: ;
  default:
  inc = inc * 3;
  timeout = 25;
  goto ldv_24316;
  }
  ldv_24316:
  inc = inc / 100;
  if (inc > car_max || inc < 0) {
    inc = car_max / 2;
  } else {
  }
  timeout = timeout * 27500;
  if (srate > 0) {
    timeout = timeout / (srate / 1000);
  } else {
  }
  if (timeout > 100 || timeout < 0) {
    timeout = 100;
  } else {
  }
  steps_max = car_max / inc + 1;
  if (steps_max > 100 || steps_max < 0) {
    steps_max = 100;
    inc = car_max / steps_max;
  } else {
  }
  *freq_inc = inc;
  *timeout_sw = timeout;
  *steps = steps_max;
  return (0);
}
}
static int stv090x_chk_signal(struct stv090x_state *state )
{
  s32 offst_car ;
  s32 agc2 ;
  s32 car_max ;
  int no_signal ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  {
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 62028U);
    offst_car = tmp << 8;
  } else {
    tmp___0 = stv090x_read_reg(state, 62540U);
    offst_car = tmp___0 << 8;
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___1 = stv090x_read_reg(state, 62029U);
    tmp___3 = tmp___1;
  } else {
    tmp___2 = stv090x_read_reg(state, 62541U);
    tmp___3 = tmp___2;
  }
  offst_car = tmp___3 | offst_car;
  offst_car = comp2(offst_car, 16);
  if ((unsigned int )state->demod == 2U) {
    tmp___4 = stv090x_read_reg(state, 62006U);
    agc2 = tmp___4 << 8;
  } else {
    tmp___5 = stv090x_read_reg(state, 62518U);
    agc2 = tmp___5 << 8;
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___6 = stv090x_read_reg(state, 62007U);
    tmp___8 = tmp___6;
  } else {
    tmp___7 = stv090x_read_reg(state, 62519U);
    tmp___8 = tmp___7;
  }
  agc2 = tmp___8 | agc2;
  car_max = state->search_range / 1000;
  car_max = car_max / 10 + car_max;
  car_max = (car_max * 65536) / 2;
  car_max = car_max / ((state->internal)->mclk / 1000);
  if (car_max > 16384) {
    car_max = 16384;
  } else {
  }
  if ((agc2 > 8192 || car_max * 2 < offst_car) || car_max * -2 > offst_car) {
    no_signal = 1;
    if (verbose != 0U && verbose > 3U) {
      printk("\v%s: No Signal\n", "stv090x_chk_signal");
    } else
    if (verbose > 1U && verbose > 3U) {
      printk("\r%s: No Signal\n", "stv090x_chk_signal");
    } else
    if (verbose > 2U && verbose > 3U) {
      printk("\016%s: No Signal\n", "stv090x_chk_signal");
    } else
    if (verbose > 3U && verbose > 3U) {
      printk("\017%s: No Signal\n", "stv090x_chk_signal");
    } else
    if (verbose > 3U) {
      printk("No Signal");
    } else {
    }
  } else {
    no_signal = 0;
    if (verbose != 0U && verbose > 3U) {
      printk("\v%s: Found Signal\n", "stv090x_chk_signal");
    } else
    if (verbose > 1U && verbose > 3U) {
      printk("\r%s: Found Signal\n", "stv090x_chk_signal");
    } else
    if (verbose > 2U && verbose > 3U) {
      printk("\016%s: Found Signal\n", "stv090x_chk_signal");
    } else
    if (verbose > 3U && verbose > 3U) {
      printk("\017%s: Found Signal\n", "stv090x_chk_signal");
    } else
    if (verbose > 3U) {
      printk("Found Signal");
    } else {
    }
  }
  return (no_signal);
}
}
static int stv090x_search_car_loop(struct stv090x_state *state , s32 inc , s32 timeout ,
                                   int zigzag , s32 steps_max )
{
  int no_signal ;
  int lock ;
  s32 cpt_step ;
  s32 offst_freq ;
  s32 car_max ;
  u32 reg ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  {
  lock = 0;
  cpt_step = 0;
  car_max = state->search_range / 1000;
  car_max = car_max / 10 + car_max;
  car_max = (car_max * 65536) / 2;
  car_max = car_max / ((state->internal)->mclk / 1000);
  if (car_max > 16384) {
    car_max = 16384;
  } else {
  }
  if (zigzag != 0) {
    offst_freq = 0;
  } else {
    offst_freq = inc - car_max;
  }
  ldv_24342: ;
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_write_reg(state, 61974U, 28);
    tmp___1 = tmp < 0;
  } else {
    tmp___0 = stv090x_write_reg(state, 62486U, 28);
    tmp___1 = tmp___0 < 0;
  }
  if (tmp___1) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_write_reg(state, 62024U, (int )((u8 )(offst_freq / 256)));
    tmp___4 = tmp___2 < 0;
  } else {
    tmp___3 = stv090x_write_reg(state, 62536U, (int )((u8 )(offst_freq / 256)));
    tmp___4 = tmp___3 < 0;
  }
  if (tmp___4) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___5 = stv090x_write_reg(state, 62025U, (int )((u8 )offst_freq));
    tmp___7 = tmp___5 < 0;
  } else {
    tmp___6 = stv090x_write_reg(state, 62537U, (int )((u8 )offst_freq));
    tmp___7 = tmp___6 < 0;
  }
  if (tmp___7) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___8 = stv090x_write_reg(state, 61974U, 24);
    tmp___10 = tmp___8 < 0;
  } else {
    tmp___9 = stv090x_write_reg(state, 62486U, 24);
    tmp___10 = tmp___9 < 0;
  }
  if (tmp___10) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___11 = stv090x_read_reg(state, 62288U);
    tmp___13 = tmp___11;
  } else {
    tmp___12 = stv090x_read_reg(state, 62800U);
    tmp___13 = tmp___12;
  }
  reg = (u32 )tmp___13;
  reg = reg | 1U;
  if ((unsigned int )state->demod == 2U) {
    tmp___14 = stv090x_write_reg(state, 62288U, (int )((u8 )reg));
    tmp___16 = tmp___14 < 0;
  } else {
    tmp___15 = stv090x_write_reg(state, 62800U, (int )((u8 )reg));
    tmp___16 = tmp___15 < 0;
  }
  if (tmp___16) {
    goto err;
  } else {
  }
  if (zigzag != 0) {
    if (offst_freq >= 0) {
      offst_freq = inc * -2 - offst_freq;
    } else {
      offst_freq = - offst_freq;
    }
  } else {
    offst_freq = inc * 2 + offst_freq;
  }
  cpt_step = cpt_step + 1;
  lock = stv090x_get_dmdlock(state, timeout);
  no_signal = stv090x_chk_signal(state);
  if ((((lock == 0 && no_signal == 0) && offst_freq - inc < car_max) && offst_freq + inc > - car_max) && cpt_step < steps_max) {
    goto ldv_24342;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___17 = stv090x_read_reg(state, 62288U);
    tmp___19 = tmp___17;
  } else {
    tmp___18 = stv090x_read_reg(state, 62800U);
    tmp___19 = tmp___18;
  }
  reg = (u32 )tmp___19;
  reg = reg & 4294967294U;
  if ((unsigned int )state->demod == 2U) {
    tmp___20 = stv090x_write_reg(state, 62288U, (int )((u8 )reg));
    tmp___22 = tmp___20 < 0;
  } else {
    tmp___21 = stv090x_write_reg(state, 62800U, (int )((u8 )reg));
    tmp___22 = tmp___21 < 0;
  }
  if (tmp___22) {
    goto err;
  } else {
  }
  return (lock);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_search_car_loop");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_search_car_loop");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_search_car_loop");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_search_car_loop");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_sw_algo(struct stv090x_state *state )
{
  int no_signal ;
  int zigzag ;
  int lock ;
  u32 reg ;
  s32 dvbs2_fly_wheel ;
  s32 inc ;
  s32 timeout_step ;
  s32 trials ;
  s32 steps_max ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  int tmp___23 ;
  int tmp___24 ;
  int tmp___25 ;
  int tmp___26 ;
  int tmp___27 ;
  int tmp___28 ;
  int tmp___29 ;
  int tmp___30 ;
  int tmp___31 ;
  int tmp___32 ;
  int tmp___33 ;
  int tmp___34 ;
  int tmp___35 ;
  int tmp___36 ;
  int tmp___37 ;
  int tmp___38 ;
  int tmp___39 ;
  int tmp___40 ;
  {
  lock = 0;
  stv090x_get_loop_params(state, & inc, & timeout_step, & steps_max);
  switch ((unsigned int )state->search_mode) {
  case 1U: ;
  case 0U: ;
  if ((state->internal)->dev_ver > 31U) {
    if ((unsigned int )state->demod == 2U) {
      tmp = stv090x_write_reg(state, 62013U, 59);
      tmp___1 = tmp < 0;
    } else {
      tmp___0 = stv090x_write_reg(state, 62525U, 59);
      tmp___1 = tmp___0 < 0;
    }
    if (tmp___1) {
      goto err;
    } else {
    }
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_write_reg(state, 61972U, 73);
    tmp___4 = tmp___2 < 0;
  } else {
    tmp___3 = stv090x_write_reg(state, 62484U, 73);
    tmp___4 = tmp___3 < 0;
  }
  if (tmp___4) {
    goto err;
  } else {
  }
  zigzag = 0;
  goto ldv_24360;
  case 2U: ;
  if ((state->internal)->dev_ver > 31U) {
    if ((unsigned int )state->demod == 2U) {
      tmp___5 = stv090x_write_reg(state, 61985U, 121);
      tmp___7 = tmp___5 < 0;
    } else {
      tmp___6 = stv090x_write_reg(state, 62497U, 121);
      tmp___7 = tmp___6 < 0;
    }
    if (tmp___7) {
      goto err;
    } else {
    }
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___8 = stv090x_write_reg(state, 61972U, 137);
    tmp___10 = tmp___8 < 0;
  } else {
    tmp___9 = stv090x_write_reg(state, 62484U, 137);
    tmp___10 = tmp___9 < 0;
  }
  if (tmp___10) {
    goto err;
  } else {
  }
  zigzag = 1;
  goto ldv_24360;
  case 3U: ;
  default: ;
  if ((state->internal)->dev_ver > 31U) {
    if ((unsigned int )state->demod == 2U) {
      tmp___11 = stv090x_write_reg(state, 62013U, 59);
      tmp___13 = tmp___11 < 0;
    } else {
      tmp___12 = stv090x_write_reg(state, 62525U, 59);
      tmp___13 = tmp___12 < 0;
    }
    if (tmp___13) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___14 = stv090x_write_reg(state, 61985U, 121);
      tmp___16 = tmp___14 < 0;
    } else {
      tmp___15 = stv090x_write_reg(state, 62497U, 121);
      tmp___16 = tmp___15 < 0;
    }
    if (tmp___16) {
      goto err;
    } else {
    }
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___17 = stv090x_write_reg(state, 61972U, 201);
    tmp___19 = tmp___17 < 0;
  } else {
    tmp___18 = stv090x_write_reg(state, 62484U, 201);
    tmp___19 = tmp___18 < 0;
  }
  if (tmp___19) {
    goto err;
  } else {
  }
  zigzag = 0;
  goto ldv_24360;
  }
  ldv_24360:
  trials = 0;
  ldv_24364:
  lock = stv090x_search_car_loop(state, inc, timeout_step, zigzag, steps_max);
  no_signal = stv090x_chk_signal(state);
  trials = trials + 1;
  if ((lock != 0 || no_signal != 0) || trials == 2) {
    if ((state->internal)->dev_ver > 31U) {
      if ((unsigned int )state->demod == 2U) {
        tmp___20 = stv090x_write_reg(state, 62013U, 73);
        tmp___22 = tmp___20 < 0;
      } else {
        tmp___21 = stv090x_write_reg(state, 62525U, 73);
        tmp___22 = tmp___21 < 0;
      }
      if (tmp___22) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___23 = stv090x_write_reg(state, 61985U, 158);
        tmp___25 = tmp___23 < 0;
      } else {
        tmp___24 = stv090x_write_reg(state, 62497U, 158);
        tmp___25 = tmp___24 < 0;
      }
      if (tmp___25) {
        goto err;
      } else {
      }
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___26 = stv090x_read_reg(state, 61979U);
      tmp___28 = tmp___26;
    } else {
      tmp___27 = stv090x_read_reg(state, 62491U);
      tmp___28 = tmp___27;
    }
    reg = (u32 )tmp___28;
    if (lock != 0 && ((reg >> 5) & 3U) == 2U) {
      msleep((unsigned int )timeout_step);
      if ((unsigned int )state->demod == 2U) {
        tmp___29 = stv090x_read_reg(state, 61980U);
        tmp___31 = tmp___29;
      } else {
        tmp___30 = stv090x_read_reg(state, 62492U);
        tmp___31 = tmp___30;
      }
      reg = (u32 )tmp___31;
      dvbs2_fly_wheel = (s32 )reg & 15;
      if (dvbs2_fly_wheel <= 12) {
        msleep((unsigned int )timeout_step);
        if ((unsigned int )state->demod == 2U) {
          tmp___32 = stv090x_read_reg(state, 61980U);
          tmp___34 = tmp___32;
        } else {
          tmp___33 = stv090x_read_reg(state, 62492U);
          tmp___34 = tmp___33;
        }
        reg = (u32 )tmp___34;
        dvbs2_fly_wheel = (s32 )reg & 15;
      } else {
      }
      if (dvbs2_fly_wheel <= 12) {
        lock = 0;
        if (trials <= 1) {
          if ((state->internal)->dev_ver > 31U) {
            if ((unsigned int )state->demod == 2U) {
              tmp___35 = stv090x_write_reg(state, 61985U, 121);
              tmp___37 = tmp___35 < 0;
            } else {
              tmp___36 = stv090x_write_reg(state, 62497U, 121);
              tmp___37 = tmp___36 < 0;
            }
            if (tmp___37) {
              goto err;
            } else {
            }
          } else {
          }
          if ((unsigned int )state->demod == 2U) {
            tmp___38 = stv090x_write_reg(state, 61972U, 137);
            tmp___40 = tmp___38 < 0;
          } else {
            tmp___39 = stv090x_write_reg(state, 62484U, 137);
            tmp___40 = tmp___39 < 0;
          }
          if (tmp___40) {
            goto err;
          } else {
          }
        } else {
        }
      } else {
      }
    } else {
    }
  } else {
  }
  if ((lock == 0 && trials <= 1) && no_signal == 0) {
    goto ldv_24364;
  } else {
  }
  return (lock);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_sw_algo");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_sw_algo");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_sw_algo");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_sw_algo");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static enum stv090x_delsys stv090x_get_std(struct stv090x_state *state )
{
  u32 reg ;
  enum stv090x_delsys delsys ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  {
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 61979U);
    tmp___1 = tmp;
  } else {
    tmp___0 = stv090x_read_reg(state, 62491U);
    tmp___1 = tmp___0;
  }
  reg = (u32 )tmp___1;
  if (((reg >> 5) & 3U) == 2U) {
    delsys = STV090x_DVBS2;
  } else
  if (((reg >> 5) & 3U) == 3U) {
    if ((unsigned int )state->demod == 2U) {
      tmp___2 = stv090x_read_reg(state, 62259U);
      tmp___4 = tmp___2;
    } else {
      tmp___3 = stv090x_read_reg(state, 62771U);
      tmp___4 = tmp___3;
    }
    reg = (u32 )tmp___4;
    if ((reg & 128U) != 0U) {
      delsys = STV090x_DSS;
    } else {
      delsys = STV090x_DVBS1;
    }
  } else {
    delsys = STV090x_ERROR;
  }
  return (delsys);
}
}
static s32 stv090x_get_car_freq(struct stv090x_state *state , u32 mclk )
{
  s32 derot ;
  s32 int_1 ;
  s32 int_2 ;
  s32 tmp_1 ;
  s32 tmp_2 ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  {
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 62028U);
    derot = tmp << 16;
  } else {
    tmp___0 = stv090x_read_reg(state, 62540U);
    derot = tmp___0 << 16;
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___1 = stv090x_read_reg(state, 62029U);
    tmp___3 = tmp___1 << 8;
  } else {
    tmp___2 = stv090x_read_reg(state, 62541U);
    tmp___3 = tmp___2 << 8;
  }
  derot = tmp___3 | derot;
  if ((unsigned int )state->demod == 2U) {
    tmp___4 = stv090x_read_reg(state, 62030U);
    tmp___6 = tmp___4;
  } else {
    tmp___5 = stv090x_read_reg(state, 62542U);
    tmp___6 = tmp___5;
  }
  derot = tmp___6 | derot;
  derot = comp2(derot, 24);
  int_1 = (s32 )(mclk >> 12);
  int_2 = derot >> 12;
  tmp_1 = (s32 )mclk & 4095;
  tmp_2 = derot % 4096;
  derot = (int_1 * int_2 + (int_1 * tmp_2 >> 12)) + (int_2 * tmp_1 >> 12);
  return (derot);
}
}
static int stv090x_get_viterbi(struct stv090x_state *state )
{
  u32 reg ;
  u32 rate ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  {
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 62266U);
    tmp___1 = tmp;
  } else {
    tmp___0 = stv090x_read_reg(state, 62778U);
    tmp___1 = tmp___0;
  }
  reg = (u32 )tmp___1;
  rate = reg & 31U;
  switch (rate) {
  case (u32 )13:
  state->fec = STV090x_PR12;
  goto ldv_24387;
  case (u32 )18:
  state->fec = STV090x_PR23;
  goto ldv_24387;
  case (u32 )21:
  state->fec = STV090x_PR34;
  goto ldv_24387;
  case (u32 )24:
  state->fec = STV090x_PR56;
  goto ldv_24387;
  case (u32 )25:
  state->fec = STV090x_PR67;
  goto ldv_24387;
  case (u32 )26:
  state->fec = STV090x_PR78;
  goto ldv_24387;
  default:
  state->fec = STV090x_PRERR;
  goto ldv_24387;
  }
  ldv_24387: ;
  return (0);
}
}
static enum stv090x_signal_state stv090x_get_sig_params(struct stv090x_state *state )
{
  struct dvb_frontend *fe ;
  u8 tmg ;
  u32 reg ;
  s32 i ;
  s32 offst_freq ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  s32 tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  long ret ;
  int __x___0 ;
  u32 tmp___20 ;
  long ret___0 ;
  int __x___2 ;
  long ret___1 ;
  int __x___4 ;
  {
  fe = & state->frontend;
  i = 0;
  msleep(5U);
  if ((unsigned int )state->algo == 0U) {
    if ((unsigned int )state->demod == 2U) {
      tmp = stv090x_read_reg(state, 62056U);
      tmg = (u8 )tmp;
    } else {
      tmp___0 = stv090x_read_reg(state, 62568U);
      tmg = (u8 )tmp___0;
    }
    if ((unsigned int )state->demod == 2U) {
      stv090x_write_reg(state, 62041U, 92);
    } else {
      stv090x_write_reg(state, 62553U, 92);
    }
    goto ldv_24403;
    ldv_24402: ;
    if ((unsigned int )state->demod == 2U) {
      tmp___1 = stv090x_read_reg(state, 62056U);
      tmg = (u8 )tmp___1;
    } else {
      tmp___2 = stv090x_read_reg(state, 62568U);
      tmg = (u8 )tmp___2;
    }
    msleep(5U);
    i = i + 5;
    ldv_24403: ;
    if ((i <= 50 && (unsigned int )tmg != 0U) && (unsigned int )tmg != 255U) {
      goto ldv_24402;
    } else {
    }
  } else {
  }
  state->delsys = stv090x_get_std(state);
  tmp___3 = stv090x_i2c_gate_ctrl(state, 1);
  if (tmp___3 < 0) {
    goto err;
  } else {
  }
  if ((unsigned long )(state->config)->tuner_get_frequency != (unsigned long )((int (* )(struct dvb_frontend * ,
                                                                                                    u32 * ))0)) {
    tmp___4 = (*((state->config)->tuner_get_frequency))(fe, & state->frequency);
    if (tmp___4 < 0) {
      goto err_gateoff;
    } else {
    }
  } else {
  }
  tmp___5 = stv090x_i2c_gate_ctrl(state, 0);
  if (tmp___5 < 0) {
    goto err;
  } else {
  }
  tmp___6 = stv090x_get_car_freq(state, (u32 )(state->internal)->mclk);
  offst_freq = tmp___6 / 1000;
  state->frequency = state->frequency + (u32 )offst_freq;
  tmp___7 = stv090x_get_viterbi(state);
  if (tmp___7 < 0) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___8 = stv090x_read_reg(state, 61969U);
    tmp___10 = tmp___8;
  } else {
    tmp___9 = stv090x_read_reg(state, 62481U);
    tmp___10 = tmp___9;
  }
  reg = (u32 )tmp___10;
  state->modcod = (enum stv090x_modcod )((reg >> 2) & 31U);
  state->pilots = (enum stv090x_pilot )(reg & 1U);
  state->frame_len = (enum stv090x_frame )((reg & 3U) >> 1);
  if ((unsigned int )state->demod == 2U) {
    tmp___11 = stv090x_read_reg(state, 62061U);
    tmp___13 = tmp___11;
  } else {
    tmp___12 = stv090x_read_reg(state, 62573U);
    tmp___13 = tmp___12;
  }
  reg = (u32 )tmp___13;
  state->rolloff = (enum stv090x_rolloff )((reg >> 6) & 3U);
  if ((unsigned int )state->demod == 2U) {
    tmp___14 = stv090x_read_reg(state, 62259U);
    tmp___16 = tmp___14;
  } else {
    tmp___15 = stv090x_read_reg(state, 62771U);
    tmp___16 = tmp___15;
  }
  reg = (u32 )tmp___16;
  state->inversion = (enum stv090x_inversion )(reg & 1U);
  if ((unsigned int )state->algo == 0U || state->srate <= 9999999U) {
    tmp___17 = stv090x_i2c_gate_ctrl(state, 1);
    if (tmp___17 < 0) {
      goto err;
    } else {
    }
    if ((unsigned long )(state->config)->tuner_get_frequency != (unsigned long )((int (* )(struct dvb_frontend * ,
                                                                                                      u32 * ))0)) {
      tmp___18 = (*((state->config)->tuner_get_frequency))(fe, & state->frequency);
      if (tmp___18 < 0) {
        goto err_gateoff;
      } else {
      }
    } else {
    }
    tmp___19 = stv090x_i2c_gate_ctrl(state, 0);
    if (tmp___19 < 0) {
      goto err;
    } else {
    }
    __x___2 = offst_freq;
    ret___0 = (long )(__x___2 < 0 ? - __x___2 : __x___2);
    if (ret___0 <= (long )(state->search_range / 2000 + 500)) {
      return (STV090x_RANGEOK);
    } else {
      __x___0 = offst_freq;
      ret = (long )(__x___0 < 0 ? - __x___0 : __x___0);
      tmp___20 = stv090x_car_width(state->srate, state->rolloff);
      if (ret <= (long )(tmp___20 / 2000U)) {
        return (STV090x_RANGEOK);
      } else {
        return (STV090x_OUTOFRANGE);
      }
    }
  } else {
    __x___4 = offst_freq;
    ret___1 = (long )(__x___4 < 0 ? - __x___4 : __x___4);
    if (ret___1 <= (long )(state->search_range / 2000 + 500)) {
      return (STV090x_RANGEOK);
    } else {
      return (STV090x_OUTOFRANGE);
    }
  }
  return (STV090x_OUTOFRANGE);
  err_gateoff:
  stv090x_i2c_gate_ctrl(state, 0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_get_sig_params");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_get_sig_params");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_get_sig_params");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_get_sig_params");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (4294967295L);
}
}
static u32 stv090x_get_tmgoffst(struct stv090x_state *state , u32 srate )
{
  s32 offst_tmg ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  {
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 62056U);
    offst_tmg = tmp << 16;
  } else {
    tmp___0 = stv090x_read_reg(state, 62568U);
    offst_tmg = tmp___0 << 16;
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___1 = stv090x_read_reg(state, 62057U);
    tmp___3 = tmp___1 << 8;
  } else {
    tmp___2 = stv090x_read_reg(state, 62569U);
    tmp___3 = tmp___2 << 8;
  }
  offst_tmg = tmp___3 | offst_tmg;
  if ((unsigned int )state->demod == 2U) {
    tmp___4 = stv090x_read_reg(state, 62058U);
    tmp___6 = tmp___4;
  } else {
    tmp___5 = stv090x_read_reg(state, 62570U);
    tmp___6 = tmp___5;
  }
  offst_tmg = tmp___6 | offst_tmg;
  offst_tmg = comp2(offst_tmg, 24);
  if (offst_tmg == 0) {
    offst_tmg = 1;
  } else {
  }
  offst_tmg = ((int )srate * 10) / (16777216 / offst_tmg);
  offst_tmg = offst_tmg / 320;
  return ((u32 )offst_tmg);
}
}
static u8 stv090x_optimize_carloop(struct stv090x_state *state , enum stv090x_modcod modcod ,
                                   s32 pilots )
{
  u8 aclc ;
  s32 i ;
  struct stv090x_long_frame_crloop *car_loop ;
  struct stv090x_long_frame_crloop *car_loop_qpsk_low ;
  struct stv090x_long_frame_crloop *car_loop_apsk_low ;
  {
  aclc = 41U;
  if ((state->internal)->dev_ver == 32U) {
    car_loop = (struct stv090x_long_frame_crloop *)(& stv090x_s2_crl_cut20);
    car_loop_qpsk_low = (struct stv090x_long_frame_crloop *)(& stv090x_s2_lowqpsk_crl_cut20);
    car_loop_apsk_low = (struct stv090x_long_frame_crloop *)(& stv090x_s2_apsk_crl_cut20);
  } else {
    car_loop = (struct stv090x_long_frame_crloop *)(& stv090x_s2_crl_cut30);
    car_loop_qpsk_low = (struct stv090x_long_frame_crloop *)(& stv090x_s2_lowqpsk_crl_cut30);
    car_loop_apsk_low = (struct stv090x_long_frame_crloop *)(& stv090x_s2_apsk_crl_cut30);
  }
  if ((unsigned int )modcod <= 3U) {
    i = 0;
    goto ldv_24436;
    ldv_24435:
    i = i + 1;
    ldv_24436: ;
    if (i <= 2 && (unsigned int )(car_loop_qpsk_low + (unsigned long )i)->modcod != (unsigned int )modcod) {
      goto ldv_24435;
    } else {
    }
    if (i > 2) {
      i = 2;
    } else {
    }
  } else {
    i = 0;
    goto ldv_24439;
    ldv_24438:
    i = i + 1;
    ldv_24439: ;
    if (i <= 13 && (unsigned int )(car_loop + (unsigned long )i)->modcod != (unsigned int )modcod) {
      goto ldv_24438;
    } else {
    }
    if (i > 13) {
      i = 0;
      goto ldv_24442;
      ldv_24441:
      i = i + 1;
      ldv_24442: ;
      if (i <= 10 && (unsigned int )(car_loop_apsk_low + (unsigned long )i)->modcod != (unsigned int )modcod) {
        goto ldv_24441;
      } else {
      }
      if (i > 10) {
        i = 10;
      } else {
      }
    } else {
    }
  }
  if ((unsigned int )modcod <= 3U) {
    if (pilots != 0) {
      if (state->srate <= 3000000U) {
        aclc = (car_loop_qpsk_low + (unsigned long )i)->crl_pilots_on_2;
      } else
      if (state->srate <= 7000000U) {
        aclc = (car_loop_qpsk_low + (unsigned long )i)->crl_pilots_on_5;
      } else
      if (state->srate <= 15000000U) {
        aclc = (car_loop_qpsk_low + (unsigned long )i)->crl_pilots_on_10;
      } else
      if (state->srate <= 25000000U) {
        aclc = (car_loop_qpsk_low + (unsigned long )i)->crl_pilots_on_20;
      } else {
        aclc = (car_loop_qpsk_low + (unsigned long )i)->crl_pilots_on_30;
      }
    } else
    if (state->srate <= 3000000U) {
      aclc = (car_loop_qpsk_low + (unsigned long )i)->crl_pilots_off_2;
    } else
    if (state->srate <= 7000000U) {
      aclc = (car_loop_qpsk_low + (unsigned long )i)->crl_pilots_off_5;
    } else
    if (state->srate <= 15000000U) {
      aclc = (car_loop_qpsk_low + (unsigned long )i)->crl_pilots_off_10;
    } else
    if (state->srate <= 25000000U) {
      aclc = (car_loop_qpsk_low + (unsigned long )i)->crl_pilots_off_20;
    } else {
      aclc = (car_loop_qpsk_low + (unsigned long )i)->crl_pilots_off_30;
    }
  } else
  if ((unsigned int )modcod <= 17U) {
    if (pilots != 0) {
      if (state->srate <= 3000000U) {
        aclc = (car_loop + (unsigned long )i)->crl_pilots_on_2;
      } else
      if (state->srate <= 7000000U) {
        aclc = (car_loop + (unsigned long )i)->crl_pilots_on_5;
      } else
      if (state->srate <= 15000000U) {
        aclc = (car_loop + (unsigned long )i)->crl_pilots_on_10;
      } else
      if (state->srate <= 25000000U) {
        aclc = (car_loop + (unsigned long )i)->crl_pilots_on_20;
      } else {
        aclc = (car_loop + (unsigned long )i)->crl_pilots_on_30;
      }
    } else
    if (state->srate <= 3000000U) {
      aclc = (car_loop + (unsigned long )i)->crl_pilots_off_2;
    } else
    if (state->srate <= 7000000U) {
      aclc = (car_loop + (unsigned long )i)->crl_pilots_off_5;
    } else
    if (state->srate <= 15000000U) {
      aclc = (car_loop + (unsigned long )i)->crl_pilots_off_10;
    } else
    if (state->srate <= 25000000U) {
      aclc = (car_loop + (unsigned long )i)->crl_pilots_off_20;
    } else {
      aclc = (car_loop + (unsigned long )i)->crl_pilots_off_30;
    }
  } else
  if (state->srate <= 3000000U) {
    aclc = (car_loop_apsk_low + (unsigned long )i)->crl_pilots_on_2;
  } else
  if (state->srate <= 7000000U) {
    aclc = (car_loop_apsk_low + (unsigned long )i)->crl_pilots_on_5;
  } else
  if (state->srate <= 15000000U) {
    aclc = (car_loop_apsk_low + (unsigned long )i)->crl_pilots_on_10;
  } else
  if (state->srate <= 25000000U) {
    aclc = (car_loop_apsk_low + (unsigned long )i)->crl_pilots_on_20;
  } else {
    aclc = (car_loop_apsk_low + (unsigned long )i)->crl_pilots_on_30;
  }
  return (aclc);
}
}
static u8 stv090x_optimize_carloop_short(struct stv090x_state *state )
{
  struct stv090x_short_frame_crloop *short_crl ;
  s32 index ;
  u8 aclc ;
  {
  short_crl = 0;
  index = 0;
  aclc = 11U;
  switch ((unsigned int )state->modulation) {
  case 0U: ;
  default:
  index = 0;
  goto ldv_24452;
  case 1U:
  index = 1;
  goto ldv_24452;
  case 2U:
  index = 2;
  goto ldv_24452;
  case 3U:
  index = 3;
  goto ldv_24452;
  }
  ldv_24452: ;
  if ((state->internal)->dev_ver > 47U) {
    short_crl = (struct stv090x_short_frame_crloop *)(& stv090x_s2_short_crl_cut30);
  } else {
    short_crl = (struct stv090x_short_frame_crloop *)(& stv090x_s2_short_crl_cut20);
  }
  if (state->srate <= 3000000U) {
    aclc = (short_crl + (unsigned long )index)->crl_2;
  } else
  if (state->srate <= 7000000U) {
    aclc = (short_crl + (unsigned long )index)->crl_5;
  } else
  if (state->srate <= 15000000U) {
    aclc = (short_crl + (unsigned long )index)->crl_10;
  } else
  if (state->srate <= 25000000U) {
    aclc = (short_crl + (unsigned long )index)->crl_20;
  } else {
    aclc = (short_crl + (unsigned long )index)->crl_30;
  }
  return (aclc);
}
}
static int stv090x_optimize_track(struct stv090x_state *state )
{
  struct dvb_frontend *fe ;
  enum stv090x_modcod modcod ;
  s32 srate ;
  s32 pilots ;
  s32 aclc ;
  s32 f_1 ;
  s32 f_0 ;
  s32 i ;
  s32 blind_tune ;
  u32 reg ;
  u32 tmp ;
  u32 tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  int tmp___23 ;
  int tmp___24 ;
  int tmp___25 ;
  int tmp___26 ;
  int tmp___27 ;
  int tmp___28 ;
  int tmp___29 ;
  int tmp___30 ;
  int tmp___31 ;
  int tmp___32 ;
  int tmp___33 ;
  int tmp___34 ;
  int tmp___35 ;
  int tmp___36 ;
  int tmp___37 ;
  int tmp___38 ;
  int tmp___39 ;
  int tmp___40 ;
  int tmp___41 ;
  int tmp___42 ;
  int tmp___43 ;
  u8 tmp___44 ;
  int tmp___45 ;
  int tmp___46 ;
  int tmp___47 ;
  int tmp___48 ;
  int tmp___49 ;
  int tmp___50 ;
  int tmp___51 ;
  int tmp___52 ;
  int tmp___53 ;
  int tmp___54 ;
  int tmp___55 ;
  int tmp___56 ;
  int tmp___57 ;
  int tmp___58 ;
  int tmp___59 ;
  int tmp___60 ;
  int tmp___61 ;
  int tmp___62 ;
  u8 tmp___63 ;
  int tmp___64 ;
  int tmp___65 ;
  int tmp___66 ;
  int tmp___67 ;
  int tmp___68 ;
  int tmp___69 ;
  int tmp___70 ;
  int tmp___71 ;
  int tmp___72 ;
  int tmp___73 ;
  int tmp___74 ;
  int tmp___75 ;
  int tmp___76 ;
  int tmp___77 ;
  int tmp___78 ;
  int tmp___79 ;
  int tmp___80 ;
  int tmp___81 ;
  int tmp___82 ;
  int tmp___83 ;
  int tmp___84 ;
  int tmp___85 ;
  int tmp___86 ;
  int tmp___87 ;
  int tmp___88 ;
  int tmp___89 ;
  int tmp___90 ;
  int tmp___91 ;
  int tmp___92 ;
  int tmp___93 ;
  int tmp___94 ;
  int tmp___95 ;
  int tmp___96 ;
  int tmp___97 ;
  int tmp___98 ;
  int tmp___99 ;
  int tmp___100 ;
  int tmp___101 ;
  int tmp___102 ;
  int tmp___103 ;
  int tmp___104 ;
  int tmp___105 ;
  int tmp___106 ;
  int tmp___107 ;
  int tmp___108 ;
  int tmp___109 ;
  int tmp___110 ;
  int tmp___111 ;
  int tmp___112 ;
  int tmp___113 ;
  int tmp___114 ;
  int tmp___115 ;
  int tmp___116 ;
  int tmp___117 ;
  int tmp___118 ;
  int tmp___119 ;
  int tmp___120 ;
  int tmp___121 ;
  int tmp___122 ;
  int tmp___123 ;
  int tmp___124 ;
  int tmp___125 ;
  int tmp___126 ;
  int tmp___127 ;
  int tmp___128 ;
  int tmp___129 ;
  u32 tmp___130 ;
  int tmp___131 ;
  int tmp___132 ;
  int tmp___133 ;
  int tmp___134 ;
  int tmp___135 ;
  int tmp___136 ;
  int tmp___137 ;
  int tmp___138 ;
  int tmp___139 ;
  int tmp___140 ;
  int tmp___141 ;
  int tmp___142 ;
  int tmp___143 ;
  int tmp___144 ;
  int tmp___145 ;
  int tmp___146 ;
  int tmp___147 ;
  int tmp___148 ;
  int tmp___149 ;
  int tmp___150 ;
  int tmp___151 ;
  int tmp___152 ;
  int tmp___153 ;
  int tmp___154 ;
  int tmp___155 ;
  int tmp___156 ;
  int tmp___157 ;
  int tmp___158 ;
  int tmp___159 ;
  int tmp___160 ;
  int tmp___161 ;
  int tmp___162 ;
  {
  fe = & state->frontend;
  i = 0;
  blind_tune = 0;
  tmp = stv090x_get_srate(state, (u32 )(state->internal)->mclk);
  srate = (s32 )tmp;
  tmp___0 = stv090x_get_tmgoffst(state, (u32 )srate);
  srate = (s32 )(tmp___0 + (u32 )srate);
  switch ((unsigned int )state->delsys) {
  case 1U: ;
  case 3U: ;
  if ((unsigned int )state->search_mode == 3U) {
    if ((unsigned int )state->demod == 2U) {
      tmp___1 = stv090x_read_reg(state, 61972U);
      tmp___3 = tmp___1;
    } else {
      tmp___2 = stv090x_read_reg(state, 62484U);
      tmp___3 = tmp___2;
    }
    reg = (u32 )tmp___3;
    reg = reg | 64U;
    reg = reg & 4294967167U;
    if ((unsigned int )state->demod == 2U) {
      tmp___4 = stv090x_write_reg(state, 61972U, (int )((u8 )reg));
      tmp___6 = tmp___4 < 0;
    } else {
      tmp___5 = stv090x_write_reg(state, 62484U, (int )((u8 )reg));
      tmp___6 = tmp___5 < 0;
    }
    if (tmp___6) {
      goto err;
    } else {
    }
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___7 = stv090x_read_reg(state, 61968U);
    tmp___9 = tmp___7;
  } else {
    tmp___8 = stv090x_read_reg(state, 62480U);
    tmp___9 = tmp___8;
  }
  reg = (u32 )tmp___9;
  reg = (reg & 4294967292U) | (u32 )state->rolloff;
  reg = reg | 4U;
  if ((unsigned int )state->demod == 2U) {
    tmp___10 = stv090x_write_reg(state, 61968U, (int )((u8 )reg));
    tmp___12 = tmp___10 < 0;
  } else {
    tmp___11 = stv090x_write_reg(state, 62480U, (int )((u8 )reg));
    tmp___12 = tmp___11 < 0;
  }
  if (tmp___12) {
    goto err;
  } else {
  }
  if ((state->internal)->dev_ver > 47U) {
    tmp___13 = stv090x_get_viterbi(state);
    if (tmp___13 < 0) {
      goto err;
    } else {
    }
    if ((unsigned int )state->fec == 0U) {
      if ((unsigned int )state->demod == 2U) {
        tmp___14 = stv090x_write_reg(state, 62144U, 152);
        tmp___16 = tmp___14 < 0;
      } else {
        tmp___15 = stv090x_write_reg(state, 62656U, 152);
        tmp___16 = tmp___15 < 0;
      }
      if (tmp___16) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___17 = stv090x_write_reg(state, 62145U, 24);
        tmp___19 = tmp___17 < 0;
      } else {
        tmp___18 = stv090x_write_reg(state, 62657U, 24);
        tmp___19 = tmp___18 < 0;
      }
      if (tmp___19) {
        goto err;
      } else {
      }
    } else {
      if ((unsigned int )state->demod == 2U) {
        tmp___20 = stv090x_write_reg(state, 62144U, 24);
        tmp___22 = tmp___20 < 0;
      } else {
        tmp___21 = stv090x_write_reg(state, 62656U, 24);
        tmp___22 = tmp___21 < 0;
      }
      if (tmp___22) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___23 = stv090x_write_reg(state, 62145U, 24);
        tmp___25 = tmp___23 < 0;
      } else {
        tmp___24 = stv090x_write_reg(state, 62657U, 24);
        tmp___25 = tmp___24 < 0;
      }
      if (tmp___25) {
        goto err;
      } else {
      }
    }
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___26 = stv090x_write_reg(state, 62360U, 117);
    tmp___28 = tmp___26 < 0;
  } else {
    tmp___27 = stv090x_write_reg(state, 62872U, 117);
    tmp___28 = tmp___27 < 0;
  }
  if (tmp___28) {
    goto err;
  } else {
  }
  goto ldv_24472;
  case 2U: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___29 = stv090x_read_reg(state, 61972U);
    tmp___31 = tmp___29;
  } else {
    tmp___30 = stv090x_read_reg(state, 62484U);
    tmp___31 = tmp___30;
  }
  reg = (u32 )tmp___31;
  reg = reg & 4294967231U;
  reg = reg | 128U;
  if ((unsigned int )state->demod == 2U) {
    tmp___32 = stv090x_write_reg(state, 61972U, (int )((u8 )reg));
    tmp___34 = tmp___32 < 0;
  } else {
    tmp___33 = stv090x_write_reg(state, 62484U, (int )((u8 )reg));
    tmp___34 = tmp___33 < 0;
  }
  if (tmp___34) {
    goto err;
  } else {
  }
  if ((state->internal)->dev_ver > 47U) {
    if ((unsigned int )state->demod == 2U) {
      tmp___35 = stv090x_write_reg(state, 62009U, 0);
      tmp___37 = tmp___35 < 0;
    } else {
      tmp___36 = stv090x_write_reg(state, 62521U, 0);
      tmp___37 = tmp___36 < 0;
    }
    if (tmp___37) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___38 = stv090x_write_reg(state, 62010U, 0);
      tmp___40 = tmp___38 < 0;
    } else {
      tmp___39 = stv090x_write_reg(state, 62522U, 0);
      tmp___40 = tmp___39 < 0;
    }
    if (tmp___40) {
      goto err;
    } else {
    }
  } else {
  }
  if ((unsigned int )state->frame_len == 0U) {
    if ((unsigned int )state->demod == 2U) {
      tmp___41 = stv090x_read_reg(state, 61969U);
      tmp___43 = tmp___41;
    } else {
      tmp___42 = stv090x_read_reg(state, 62481U);
      tmp___43 = tmp___42;
    }
    reg = (u32 )tmp___43;
    modcod = (enum stv090x_modcod )((reg >> 2) & 31U);
    pilots = (s32 )reg & 1;
    tmp___44 = stv090x_optimize_carloop(state, modcod, pilots);
    aclc = (s32 )tmp___44;
    if ((unsigned int )modcod <= 11U) {
      if ((unsigned int )state->demod == 2U) {
        stv090x_write_reg(state, 62103U, (int )((u8 )aclc));
      } else {
        stv090x_write_reg(state, 62615U, (int )((u8 )aclc));
      }
    } else
    if ((unsigned int )modcod <= 17U) {
      if ((unsigned int )state->demod == 2U) {
        tmp___45 = stv090x_write_reg(state, 62103U, 42);
        tmp___47 = tmp___45 < 0;
      } else {
        tmp___46 = stv090x_write_reg(state, 62615U, 42);
        tmp___47 = tmp___46 < 0;
      }
      if (tmp___47) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___48 = stv090x_write_reg(state, 62104U, (int )((u8 )aclc));
        tmp___50 = tmp___48 < 0;
      } else {
        tmp___49 = stv090x_write_reg(state, 62616U, (int )((u8 )aclc));
        tmp___50 = tmp___49 < 0;
      }
      if (tmp___50) {
        goto err;
      } else {
      }
    } else {
    }
    if ((unsigned int )state->demod_mode == 1U && (unsigned int )modcod > 17U) {
      if ((unsigned int )modcod <= 23U) {
        if ((unsigned int )state->demod == 2U) {
          tmp___51 = stv090x_write_reg(state, 62103U, 42);
          tmp___53 = tmp___51 < 0;
        } else {
          tmp___52 = stv090x_write_reg(state, 62615U, 42);
          tmp___53 = tmp___52 < 0;
        }
        if (tmp___53) {
          goto err;
        } else {
        }
        if ((unsigned int )state->demod == 2U) {
          tmp___54 = stv090x_write_reg(state, 62105U, (int )((u8 )aclc));
          tmp___56 = tmp___54 < 0;
        } else {
          tmp___55 = stv090x_write_reg(state, 62617U, (int )((u8 )aclc));
          tmp___56 = tmp___55 < 0;
        }
        if (tmp___56) {
          goto err;
        } else {
        }
      } else {
        if ((unsigned int )state->demod == 2U) {
          tmp___57 = stv090x_write_reg(state, 62103U, 42);
          tmp___59 = tmp___57 < 0;
        } else {
          tmp___58 = stv090x_write_reg(state, 62615U, 42);
          tmp___59 = tmp___58 < 0;
        }
        if (tmp___59) {
          goto err;
        } else {
        }
        if ((unsigned int )state->demod == 2U) {
          tmp___60 = stv090x_write_reg(state, 62106U, (int )((u8 )aclc));
          tmp___62 = tmp___60 < 0;
        } else {
          tmp___61 = stv090x_write_reg(state, 62618U, (int )((u8 )aclc));
          tmp___62 = tmp___61 < 0;
        }
        if (tmp___62) {
          goto err;
        } else {
        }
      }
    } else {
    }
  } else {
    tmp___63 = stv090x_optimize_carloop_short(state);
    aclc = (s32 )tmp___63;
    if ((unsigned int )state->modulation == 0U) {
      if ((unsigned int )state->demod == 2U) {
        tmp___82 = stv090x_write_reg(state, 62103U, (int )((u8 )aclc));
        tmp___84 = tmp___82 < 0;
      } else {
        tmp___83 = stv090x_write_reg(state, 62615U, (int )((u8 )aclc));
        tmp___84 = tmp___83 < 0;
      }
      if (tmp___84) {
        goto err;
      } else
      if ((unsigned int )state->modulation == 1U) {
        if ((unsigned int )state->demod == 2U) {
          tmp___64 = stv090x_write_reg(state, 62103U, 42);
          tmp___66 = tmp___64 < 0;
        } else {
          tmp___65 = stv090x_write_reg(state, 62615U, 42);
          tmp___66 = tmp___65 < 0;
        }
        if (tmp___66) {
          goto err;
        } else {
        }
        if ((unsigned int )state->demod == 2U) {
          tmp___67 = stv090x_write_reg(state, 62104U, (int )((u8 )aclc));
          tmp___69 = tmp___67 < 0;
        } else {
          tmp___68 = stv090x_write_reg(state, 62616U, (int )((u8 )aclc));
          tmp___69 = tmp___68 < 0;
        }
        if (tmp___69) {
          goto err;
        } else {
        }
      } else
      if ((unsigned int )state->modulation == 2U) {
        if ((unsigned int )state->demod == 2U) {
          tmp___70 = stv090x_write_reg(state, 62103U, 42);
          tmp___72 = tmp___70 < 0;
        } else {
          tmp___71 = stv090x_write_reg(state, 62615U, 42);
          tmp___72 = tmp___71 < 0;
        }
        if (tmp___72) {
          goto err;
        } else {
        }
        if ((unsigned int )state->demod == 2U) {
          tmp___73 = stv090x_write_reg(state, 62105U, (int )((u8 )aclc));
          tmp___75 = tmp___73 < 0;
        } else {
          tmp___74 = stv090x_write_reg(state, 62617U, (int )((u8 )aclc));
          tmp___75 = tmp___74 < 0;
        }
        if (tmp___75) {
          goto err;
        } else {
        }
      } else
      if ((unsigned int )state->modulation == 3U) {
        if ((unsigned int )state->demod == 2U) {
          tmp___76 = stv090x_write_reg(state, 62103U, 42);
          tmp___78 = tmp___76 < 0;
        } else {
          tmp___77 = stv090x_write_reg(state, 62615U, 42);
          tmp___78 = tmp___77 < 0;
        }
        if (tmp___78) {
          goto err;
        } else {
        }
        if ((unsigned int )state->demod == 2U) {
          tmp___79 = stv090x_write_reg(state, 62106U, (int )((u8 )aclc));
          tmp___81 = tmp___79 < 0;
        } else {
          tmp___80 = stv090x_write_reg(state, 62618U, (int )((u8 )aclc));
          tmp___81 = tmp___80 < 0;
        }
        if (tmp___81) {
          goto err;
        } else {
        }
      } else {
      }
    } else {
    }
  }
  if ((unsigned int )state->demod == 2U) {
    stv090x_write_reg(state, 62360U, 103);
  } else {
    stv090x_write_reg(state, 62872U, 103);
  }
  goto ldv_24472;
  case 0U: ;
  default: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___85 = stv090x_read_reg(state, 61972U);
    tmp___87 = tmp___85;
  } else {
    tmp___86 = stv090x_read_reg(state, 62484U);
    tmp___87 = tmp___86;
  }
  reg = (u32 )tmp___87;
  reg = reg | 64U;
  reg = reg | 128U;
  if ((unsigned int )state->demod == 2U) {
    tmp___88 = stv090x_write_reg(state, 61972U, (int )((u8 )reg));
    tmp___90 = tmp___88 < 0;
  } else {
    tmp___89 = stv090x_write_reg(state, 62484U, (int )((u8 )reg));
    tmp___90 = tmp___89 < 0;
  }
  if (tmp___90) {
    goto err;
  } else {
  }
  goto ldv_24472;
  }
  ldv_24472: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___91 = stv090x_read_reg(state, 62028U);
    f_1 = tmp___91;
  } else {
    tmp___92 = stv090x_read_reg(state, 62540U);
    f_1 = tmp___92;
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___93 = stv090x_read_reg(state, 62029U);
    f_0 = tmp___93;
  } else {
    tmp___94 = stv090x_read_reg(state, 62541U);
    f_0 = tmp___94;
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___95 = stv090x_read_reg(state, 62061U);
    tmp___97 = tmp___95;
  } else {
    tmp___96 = stv090x_read_reg(state, 62573U);
    tmp___97 = tmp___96;
  }
  reg = (u32 )tmp___97;
  if ((unsigned int )state->algo == 0U) {
    if ((unsigned int )state->demod == 2U) {
      stv090x_write_reg(state, 62041U, 0);
    } else {
      stv090x_write_reg(state, 62553U, 0);
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___98 = stv090x_read_reg(state, 61972U);
      tmp___100 = tmp___98;
    } else {
      tmp___99 = stv090x_read_reg(state, 62484U);
      tmp___100 = tmp___99;
    }
    reg = (u32 )tmp___100;
    reg = reg & 4294967279U;
    reg = reg & 4294967287U;
    if ((unsigned int )state->demod == 2U) {
      tmp___101 = stv090x_write_reg(state, 61972U, (int )((u8 )reg));
      tmp___103 = tmp___101 < 0;
    } else {
      tmp___102 = stv090x_write_reg(state, 62484U, (int )((u8 )reg));
      tmp___103 = tmp___102 < 0;
    }
    if (tmp___103) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___104 = stv090x_write_reg(state, 62042U, 193);
      tmp___106 = tmp___104 < 0;
    } else {
      tmp___105 = stv090x_write_reg(state, 62554U, 193);
      tmp___106 = tmp___105 < 0;
    }
    if (tmp___106) {
      goto err;
    } else {
    }
    tmp___107 = stv090x_set_srate(state, (u32 )srate);
    if (tmp___107 < 0) {
      goto err;
    } else {
    }
    blind_tune = 1;
    tmp___108 = stv090x_dvbs_track_crl(state);
    if (tmp___108 < 0) {
      goto err;
    } else {
    }
  } else {
  }
  if ((state->internal)->dev_ver > 31U) {
    if (((unsigned int )state->search_mode == 1U || (unsigned int )state->search_mode == 0U) || (unsigned int )state->search_mode == 3U) {
      if ((unsigned int )state->demod == 2U) {
        tmp___109 = stv090x_write_reg(state, 62269U, 10);
        tmp___111 = tmp___109 < 0;
      } else {
        tmp___110 = stv090x_write_reg(state, 62781U, 10);
        tmp___111 = tmp___110 < 0;
      }
      if (tmp___111) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___112 = stv090x_write_reg(state, 62258U, 0);
        tmp___114 = tmp___112 < 0;
      } else {
        tmp___113 = stv090x_write_reg(state, 62770U, 0);
        tmp___114 = tmp___113 < 0;
      }
      if (tmp___114) {
        goto err;
      } else {
      }
    } else {
    }
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___115 = stv090x_write_reg(state, 61997U, 56);
    tmp___117 = tmp___115 < 0;
  } else {
    tmp___116 = stv090x_write_reg(state, 62509U, 56);
    tmp___117 = tmp___116 < 0;
  }
  if (tmp___117) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___118 = stv090x_write_reg(state, 62048U, 128);
    tmp___120 = tmp___118 < 0;
  } else {
    tmp___119 = stv090x_write_reg(state, 62560U, 128);
    tmp___120 = tmp___119 < 0;
  }
  if (tmp___120) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___121 = stv090x_write_reg(state, 62050U, 128);
    tmp___123 = tmp___121 < 0;
  } else {
    tmp___122 = stv090x_write_reg(state, 62562U, 128);
    tmp___123 = tmp___122 < 0;
  }
  if (tmp___123) {
    goto err;
  } else {
  }
  if (((state->internal)->dev_ver > 31U || blind_tune == 1) || state->srate <= 9999999U) {
    if ((unsigned int )state->demod == 2U) {
      tmp___124 = stv090x_write_reg(state, 62024U, (int )((u8 )f_1));
      tmp___126 = tmp___124 < 0;
    } else {
      tmp___125 = stv090x_write_reg(state, 62536U, (int )((u8 )f_1));
      tmp___126 = tmp___125 < 0;
    }
    if (tmp___126) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___127 = stv090x_write_reg(state, 62025U, (int )((u8 )f_0));
      tmp___129 = tmp___127 < 0;
    } else {
      tmp___128 = stv090x_write_reg(state, 62537U, (int )((u8 )f_0));
      tmp___129 = tmp___128 < 0;
    }
    if (tmp___129) {
      goto err;
    } else {
    }
    tmp___130 = stv090x_car_width((u32 )srate, state->rolloff);
    state->tuner_bw = (s32 )(tmp___130 + 10000000U);
    if ((state->internal)->dev_ver > 31U || blind_tune == 1) {
      if ((unsigned int )state->algo != 2U) {
        tmp___131 = stv090x_i2c_gate_ctrl(state, 1);
        if (tmp___131 < 0) {
          goto err;
        } else {
        }
        if ((unsigned long )(state->config)->tuner_set_bandwidth != (unsigned long )((int (* )(struct dvb_frontend * ,
                                                                                                          u32 ))0)) {
          tmp___132 = (*((state->config)->tuner_set_bandwidth))(fe, (u32 )state->tuner_bw);
          if (tmp___132 < 0) {
            goto err_gateoff;
          } else {
          }
        } else {
        }
        tmp___133 = stv090x_i2c_gate_ctrl(state, 0);
        if (tmp___133 < 0) {
          goto err;
        } else {
        }
      } else {
      }
    } else {
    }
    if ((unsigned int )state->algo == 0U || state->srate <= 9999999U) {
      msleep(50U);
    } else {
      msleep(5U);
    }
    stv090x_get_lock_tmg(state);
    tmp___159 = stv090x_get_dmdlock(state, state->DemodTimeout / 2);
    if (tmp___159 == 0) {
      if ((unsigned int )state->demod == 2U) {
        tmp___134 = stv090x_write_reg(state, 61974U, 31);
        tmp___136 = tmp___134 < 0;
      } else {
        tmp___135 = stv090x_write_reg(state, 62486U, 31);
        tmp___136 = tmp___135 < 0;
      }
      if (tmp___136) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___137 = stv090x_write_reg(state, 62024U, (int )((u8 )f_1));
        tmp___139 = tmp___137 < 0;
      } else {
        tmp___138 = stv090x_write_reg(state, 62536U, (int )((u8 )f_1));
        tmp___139 = tmp___138 < 0;
      }
      if (tmp___139) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___140 = stv090x_write_reg(state, 62025U, (int )((u8 )f_0));
        tmp___142 = tmp___140 < 0;
      } else {
        tmp___141 = stv090x_write_reg(state, 62537U, (int )((u8 )f_0));
        tmp___142 = tmp___141 < 0;
      }
      if (tmp___142) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___143 = stv090x_write_reg(state, 61974U, 24);
        tmp___145 = tmp___143 < 0;
      } else {
        tmp___144 = stv090x_write_reg(state, 62486U, 24);
        tmp___145 = tmp___144 < 0;
      }
      if (tmp___145) {
        goto err;
      } else {
      }
      i = 0;
      goto ldv_24478;
      ldv_24477: ;
      if ((unsigned int )state->demod == 2U) {
        tmp___146 = stv090x_write_reg(state, 61974U, 31);
        tmp___148 = tmp___146 < 0;
      } else {
        tmp___147 = stv090x_write_reg(state, 62486U, 31);
        tmp___148 = tmp___147 < 0;
      }
      if (tmp___148) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___149 = stv090x_write_reg(state, 62024U, (int )((u8 )f_1));
        tmp___151 = tmp___149 < 0;
      } else {
        tmp___150 = stv090x_write_reg(state, 62536U, (int )((u8 )f_1));
        tmp___151 = tmp___150 < 0;
      }
      if (tmp___151) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___152 = stv090x_write_reg(state, 62025U, (int )((u8 )f_0));
        tmp___154 = tmp___152 < 0;
      } else {
        tmp___153 = stv090x_write_reg(state, 62537U, (int )((u8 )f_0));
        tmp___154 = tmp___153 < 0;
      }
      if (tmp___154) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___155 = stv090x_write_reg(state, 61974U, 24);
        tmp___157 = tmp___155 < 0;
      } else {
        tmp___156 = stv090x_write_reg(state, 62486U, 24);
        tmp___157 = tmp___156 < 0;
      }
      if (tmp___157) {
        goto err;
      } else {
      }
      i = i + 1;
      ldv_24478:
      tmp___158 = stv090x_get_dmdlock(state, state->DemodTimeout / 2);
      if (tmp___158 == 0 && i <= 2) {
        goto ldv_24477;
      } else {
      }
    } else {
    }
  } else {
  }
  if ((state->internal)->dev_ver > 31U) {
    if ((unsigned int )state->demod == 2U) {
      tmp___160 = stv090x_write_reg(state, 62013U, 73);
      tmp___162 = tmp___160 < 0;
    } else {
      tmp___161 = stv090x_write_reg(state, 62525U, 73);
      tmp___162 = tmp___161 < 0;
    }
    if (tmp___162) {
      goto err;
    } else {
    }
  } else {
  }
  if ((unsigned int )state->delsys == 1U || (unsigned int )state->delsys == 3U) {
    stv090x_set_vit_thtracq(state);
  } else {
  }
  return (0);
  err_gateoff:
  stv090x_i2c_gate_ctrl(state, 0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_optimize_track");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_optimize_track");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_optimize_track");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_optimize_track");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_get_feclock(struct stv090x_state *state , s32 timeout )
{
  s32 timer ;
  s32 lock ;
  s32 stat ;
  u32 reg ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  {
  timer = 0;
  lock = 0;
  goto ldv_24496;
  ldv_24495: ;
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 61979U);
    tmp___1 = tmp;
  } else {
    tmp___0 = stv090x_read_reg(state, 62491U);
    tmp___1 = tmp___0;
  }
  reg = (u32 )tmp___1;
  stat = (s32 )(reg >> 5) & 3;
  switch (stat) {
  case 0: ;
  case 1: ;
  default:
  lock = 0;
  goto ldv_24492;
  case 2: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_read_reg(state, 62313U);
    tmp___4 = tmp___2;
  } else {
    tmp___3 = stv090x_read_reg(state, 62825U);
    tmp___4 = tmp___3;
  }
  reg = (u32 )tmp___4;
  lock = (s32 )(reg >> 1) & 1;
  goto ldv_24492;
  case 3: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___5 = stv090x_read_reg(state, 62270U);
    tmp___7 = tmp___5;
  } else {
    tmp___6 = stv090x_read_reg(state, 62782U);
    tmp___7 = tmp___6;
  }
  reg = (u32 )tmp___7;
  lock = (s32 )(reg >> 3) & 1;
  goto ldv_24492;
  }
  ldv_24492: ;
  if (lock == 0) {
    msleep(10U);
    timer = timer + 10;
  } else {
  }
  ldv_24496: ;
  if (timer < timeout && lock == 0) {
    goto ldv_24495;
  } else {
  }
  return (lock);
}
}
static int stv090x_get_lock(struct stv090x_state *state , s32 timeout_dmd , s32 timeout_fec )
{
  u32 reg ;
  s32 timer ;
  int lock ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  {
  timer = 0;
  lock = stv090x_get_dmdlock(state, timeout_dmd);
  if (lock != 0) {
    lock = stv090x_get_feclock(state, timeout_fec);
  } else {
  }
  if (lock != 0) {
    lock = 0;
    goto ldv_24507;
    ldv_24506: ;
    if ((unsigned int )state->demod == 2U) {
      tmp = stv090x_read_reg(state, 62337U);
      tmp___1 = tmp;
    } else {
      tmp___0 = stv090x_read_reg(state, 62849U);
      tmp___1 = tmp___0;
    }
    reg = (u32 )tmp___1;
    lock = (int )(reg >> 7) & 1;
    msleep(1U);
    timer = timer + 1;
    ldv_24507: ;
    if (timer < timeout_fec && lock == 0) {
      goto ldv_24506;
    } else {
    }
  } else {
  }
  return (lock);
}
}
static int stv090x_set_s2rolloff(struct stv090x_state *state )
{
  u32 reg ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  {
  if ((state->internal)->dev_ver <= 32U) {
    if ((unsigned int )state->demod == 2U) {
      tmp = stv090x_read_reg(state, 61968U);
      tmp___1 = tmp;
    } else {
      tmp___0 = stv090x_read_reg(state, 62480U);
      tmp___1 = tmp___0;
    }
    reg = (u32 )tmp___1;
    reg = reg & 4294967291U;
    if ((unsigned int )state->demod == 2U) {
      tmp___2 = stv090x_write_reg(state, 61968U, (int )((u8 )reg));
      tmp___4 = tmp___2 < 0;
    } else {
      tmp___3 = stv090x_write_reg(state, 62480U, (int )((u8 )reg));
      tmp___4 = tmp___3 < 0;
    }
    if (tmp___4) {
      goto err;
    } else {
    }
  } else {
    if ((unsigned int )state->demod == 2U) {
      tmp___5 = stv090x_read_reg(state, 61968U);
      tmp___7 = tmp___5;
    } else {
      tmp___6 = stv090x_read_reg(state, 62480U);
      tmp___7 = tmp___6;
    }
    reg = (u32 )tmp___7;
    reg = reg & 4294967167U;
    if ((unsigned int )state->demod == 2U) {
      tmp___8 = stv090x_write_reg(state, 61968U, (int )((u8 )reg));
      tmp___10 = tmp___8 < 0;
    } else {
      tmp___9 = stv090x_write_reg(state, 62480U, (int )((u8 )reg));
      tmp___10 = tmp___9 < 0;
    }
    if (tmp___10) {
      goto err;
    } else {
    }
  }
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_set_s2rolloff");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_set_s2rolloff");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_set_s2rolloff");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_set_s2rolloff");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static enum stv090x_signal_state stv090x_algo(struct stv090x_state *state )
{
  struct dvb_frontend *fe ;
  enum stv090x_signal_state signal_state ;
  u32 reg ;
  s32 agc1_power ;
  s32 power_iq ;
  s32 i ;
  int lock ;
  int low_sr ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  int tmp___23 ;
  int tmp___24 ;
  int tmp___25 ;
  int tmp___26 ;
  int tmp___27 ;
  int tmp___28 ;
  int tmp___29 ;
  int tmp___30 ;
  int tmp___31 ;
  int tmp___32 ;
  int tmp___33 ;
  int tmp___34 ;
  int tmp___35 ;
  int tmp___36 ;
  int tmp___37 ;
  int tmp___38 ;
  u32 tmp___39 ;
  u32 tmp___40 ;
  int tmp___41 ;
  int tmp___42 ;
  int tmp___43 ;
  int tmp___44 ;
  int tmp___45 ;
  int tmp___46 ;
  int tmp___47 ;
  int tmp___48 ;
  int tmp___49 ;
  int tmp___50 ;
  int tmp___51 ;
  int tmp___52 ;
  int tmp___53 ;
  int tmp___54 ;
  int tmp___55 ;
  int tmp___56 ;
  int tmp___57 ;
  int tmp___58 ;
  int tmp___59 ;
  int tmp___60 ;
  int tmp___61 ;
  int tmp___62 ;
  int tmp___63 ;
  int tmp___64 ;
  int tmp___65 ;
  int tmp___66 ;
  int tmp___67 ;
  int tmp___68 ;
  int tmp___69 ;
  int tmp___70 ;
  int tmp___71 ;
  int tmp___72 ;
  int tmp___73 ;
  int tmp___74 ;
  int tmp___75 ;
  int tmp___76 ;
  int tmp___77 ;
  int tmp___78 ;
  int tmp___79 ;
  int tmp___80 ;
  int tmp___81 ;
  int tmp___82 ;
  int tmp___83 ;
  int tmp___84 ;
  int tmp___85 ;
  int tmp___86 ;
  int tmp___87 ;
  int tmp___88 ;
  int tmp___89 ;
  int tmp___90 ;
  int tmp___91 ;
  int tmp___92 ;
  int tmp___93 ;
  int tmp___94 ;
  int tmp___95 ;
  int tmp___96 ;
  int tmp___97 ;
  int tmp___98 ;
  int tmp___99 ;
  int tmp___100 ;
  int tmp___101 ;
  int tmp___102 ;
  int tmp___103 ;
  int tmp___104 ;
  int tmp___105 ;
  int tmp___106 ;
  int tmp___107 ;
  int tmp___108 ;
  int tmp___109 ;
  int tmp___110 ;
  int tmp___111 ;
  {
  fe = & state->frontend;
  signal_state = STV090x_NOCARRIER;
  power_iq = 0;
  lock = 0;
  low_sr = 0;
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 62322U);
    tmp___1 = tmp;
  } else {
    tmp___0 = stv090x_read_reg(state, 62834U);
    tmp___1 = tmp___0;
  }
  reg = (u32 )tmp___1;
  reg = reg | 1U;
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_write_reg(state, 62322U, (int )((u8 )reg));
    tmp___4 = tmp___2 < 0;
  } else {
    tmp___3 = stv090x_write_reg(state, 62834U, (int )((u8 )reg));
    tmp___4 = tmp___3 < 0;
  }
  if (tmp___4) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___5 = stv090x_write_reg(state, 61974U, 92);
    tmp___7 = tmp___5 < 0;
  } else {
    tmp___6 = stv090x_write_reg(state, 62486U, 92);
    tmp___7 = tmp___6 < 0;
  }
  if (tmp___7) {
    goto err;
  } else {
  }
  if ((state->internal)->dev_ver > 31U) {
    if (state->srate > 5000000U) {
      if ((unsigned int )state->demod == 2U) {
        tmp___11 = stv090x_write_reg(state, 61985U, 158);
        tmp___13 = tmp___11 < 0;
      } else {
        tmp___12 = stv090x_write_reg(state, 62497U, 158);
        tmp___13 = tmp___12 < 0;
      }
      if (tmp___13) {
        goto err;
      } else {
        if ((unsigned int )state->demod == 2U) {
          tmp___8 = stv090x_write_reg(state, 61985U, 130);
          tmp___10 = tmp___8 < 0;
        } else {
          tmp___9 = stv090x_write_reg(state, 62497U, 130);
          tmp___10 = tmp___9 < 0;
        }
        if (tmp___10) {
          goto err;
        } else {
        }
      }
    } else {
    }
  } else {
  }
  stv090x_get_lock_tmg(state);
  if ((unsigned int )state->algo == 0U) {
    state->tuner_bw = 72000000;
    if ((unsigned int )state->demod == 2U) {
      tmp___14 = stv090x_write_reg(state, 62042U, 192);
      tmp___16 = tmp___14 < 0;
    } else {
      tmp___15 = stv090x_write_reg(state, 62554U, 192);
      tmp___16 = tmp___15 < 0;
    }
    if (tmp___16) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___17 = stv090x_write_reg(state, 61984U, 112);
      tmp___19 = tmp___17 < 0;
    } else {
      tmp___18 = stv090x_write_reg(state, 62496U, 112);
      tmp___19 = tmp___18 < 0;
    }
    if (tmp___19) {
      goto err;
    } else {
    }
    tmp___20 = stv090x_set_srate(state, 1000000U);
    if (tmp___20 < 0) {
      goto err;
    } else {
    }
  } else {
    if ((unsigned int )state->demod == 2U) {
      tmp___21 = stv090x_write_reg(state, 61975U, 32);
      tmp___23 = tmp___21 < 0;
    } else {
      tmp___22 = stv090x_write_reg(state, 62487U, 32);
      tmp___23 = tmp___22 < 0;
    }
    if (tmp___23) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___24 = stv090x_write_reg(state, 62032U, 210);
      tmp___26 = tmp___24 < 0;
    } else {
      tmp___25 = stv090x_write_reg(state, 62544U, 210);
      tmp___26 = tmp___25 < 0;
    }
    if (tmp___26) {
      goto err;
    } else {
    }
    if (state->srate <= 1999999U) {
      if ((unsigned int )state->demod == 2U) {
        tmp___30 = stv090x_write_reg(state, 61984U, 99);
        tmp___32 = tmp___30 < 0;
      } else {
        tmp___31 = stv090x_write_reg(state, 62496U, 99);
        tmp___32 = tmp___31 < 0;
      }
      if (tmp___32) {
        goto err;
      } else {
        if ((unsigned int )state->demod == 2U) {
          tmp___27 = stv090x_write_reg(state, 61984U, 112);
          tmp___29 = tmp___27 < 0;
        } else {
          tmp___28 = stv090x_write_reg(state, 62496U, 112);
          tmp___29 = tmp___28 < 0;
        }
        if (tmp___29) {
          goto err;
        } else {
        }
      }
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___33 = stv090x_write_reg(state, 61997U, 56);
      tmp___35 = tmp___33 < 0;
    } else {
      tmp___34 = stv090x_write_reg(state, 62509U, 56);
      tmp___35 = tmp___34 < 0;
    }
    if (tmp___35) {
      goto err;
    } else {
    }
    if ((state->internal)->dev_ver > 31U) {
      if ((unsigned int )state->demod == 2U) {
        tmp___36 = stv090x_write_reg(state, 62040U, 90);
        tmp___38 = tmp___36 < 0;
      } else {
        tmp___37 = stv090x_write_reg(state, 62552U, 90);
        tmp___38 = tmp___37 < 0;
      }
      if (tmp___38) {
        goto err;
      } else {
      }
      if ((unsigned int )state->algo == 1U) {
        tmp___39 = stv090x_car_width(state->srate, state->rolloff);
        state->tuner_bw = (s32 )((tmp___39 * 15U + 150000000U) / 10U);
      } else
      if ((unsigned int )state->algo == 2U) {
        tmp___40 = stv090x_car_width(state->srate, state->rolloff);
        state->tuner_bw = (s32 )(tmp___40 + 10000000U);
      } else {
      }
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___41 = stv090x_write_reg(state, 62042U, 193);
      tmp___43 = tmp___41 < 0;
    } else {
      tmp___42 = stv090x_write_reg(state, 62554U, 193);
      tmp___43 = tmp___42 < 0;
    }
    if (tmp___43) {
      goto err;
    } else {
    }
    tmp___44 = stv090x_set_srate(state, state->srate);
    if (tmp___44 < 0) {
      goto err;
    } else {
    }
    tmp___45 = stv090x_set_max_srate(state, (u32 )(state->internal)->mclk, state->srate);
    if (tmp___45 < 0) {
      goto err;
    } else {
    }
    tmp___46 = stv090x_set_min_srate(state, (u32 )(state->internal)->mclk, state->srate);
    if (tmp___46 < 0) {
      goto err;
    } else {
    }
    if (state->srate > 9999999U) {
      low_sr = 0;
    } else {
      low_sr = 1;
    }
  }
  tmp___47 = stv090x_i2c_gate_ctrl(state, 1);
  if (tmp___47 < 0) {
    goto err;
  } else {
  }
  if ((unsigned long )(state->config)->tuner_set_bbgain != (unsigned long )((int (* )(struct dvb_frontend * ,
                                                                                                 u32 ))0)) {
    reg = (u32 )(state->config)->tuner_bbgain;
    if (reg == 0U) {
      reg = 10U;
    } else {
    }
    tmp___48 = (*((state->config)->tuner_set_bbgain))(fe, reg);
    if (tmp___48 < 0) {
      goto err_gateoff;
    } else {
    }
  } else {
  }
  if ((unsigned long )(state->config)->tuner_set_frequency != (unsigned long )((int (* )(struct dvb_frontend * ,
                                                                                                    u32 ))0)) {
    tmp___49 = (*((state->config)->tuner_set_frequency))(fe, state->frequency);
    if (tmp___49 < 0) {
      goto err_gateoff;
    } else {
    }
  } else {
  }
  if ((unsigned long )(state->config)->tuner_set_bandwidth != (unsigned long )((int (* )(struct dvb_frontend * ,
                                                                                                    u32 ))0)) {
    tmp___50 = (*((state->config)->tuner_set_bandwidth))(fe, (u32 )state->tuner_bw);
    if (tmp___50 < 0) {
      goto err_gateoff;
    } else {
    }
  } else {
  }
  tmp___51 = stv090x_i2c_gate_ctrl(state, 0);
  if (tmp___51 < 0) {
    goto err;
  } else {
  }
  msleep(50U);
  if ((unsigned long )(state->config)->tuner_get_status != (unsigned long )((int (* )(struct dvb_frontend * ,
                                                                                                 u32 * ))0)) {
    tmp___52 = stv090x_i2c_gate_ctrl(state, 1);
    if (tmp___52 < 0) {
      goto err;
    } else {
    }
    tmp___53 = (*((state->config)->tuner_get_status))(fe, & reg);
    if (tmp___53 < 0) {
      goto err_gateoff;
    } else {
    }
    tmp___54 = stv090x_i2c_gate_ctrl(state, 0);
    if (tmp___54 < 0) {
      goto err;
    } else {
    }
    if (reg != 0U) {
      if (verbose != 0U && verbose > 3U) {
        printk("\v%s: Tuner phase locked\n", "stv090x_algo");
      } else
      if (verbose > 1U && verbose > 3U) {
        printk("\r%s: Tuner phase locked\n", "stv090x_algo");
      } else
      if (verbose > 2U && verbose > 3U) {
        printk("\016%s: Tuner phase locked\n", "stv090x_algo");
      } else
      if (verbose > 3U && verbose > 3U) {
        printk("\017%s: Tuner phase locked\n", "stv090x_algo");
      } else
      if (verbose > 3U) {
        printk("Tuner phase locked");
      } else {
        if (verbose != 0U && verbose > 3U) {
          printk("\v%s: Tuner unlocked\n", "stv090x_algo");
        } else
        if (verbose > 1U && verbose > 3U) {
          printk("\r%s: Tuner unlocked\n", "stv090x_algo");
        } else
        if (verbose > 2U && verbose > 3U) {
          printk("\016%s: Tuner unlocked\n", "stv090x_algo");
        } else
        if (verbose > 3U && verbose > 3U) {
          printk("\017%s: Tuner unlocked\n", "stv090x_algo");
        } else
        if (verbose > 3U) {
          printk("Tuner unlocked");
        } else {
        }
        return (STV090x_NOCARRIER);
      }
    } else {
    }
  } else {
  }
  msleep(10U);
  if ((unsigned int )state->demod == 2U) {
    tmp___55 = stv090x_read_reg(state, 61966U);
    tmp___57 = tmp___55 << 8;
  } else {
    tmp___56 = stv090x_read_reg(state, 62478U);
    tmp___57 = tmp___56 << 8;
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___58 = stv090x_read_reg(state, 61967U);
    tmp___60 = tmp___58;
  } else {
    tmp___59 = stv090x_read_reg(state, 62479U);
    tmp___60 = tmp___59;
  }
  agc1_power = tmp___57 | tmp___60;
  if (agc1_power == 0) {
    i = 0;
    goto ldv_24530;
    ldv_24529: ;
    if ((unsigned int )state->demod == 2U) {
      tmp___61 = stv090x_read_reg(state, 61962U);
      tmp___63 = tmp___61;
    } else {
      tmp___62 = stv090x_read_reg(state, 62474U);
      tmp___63 = tmp___62;
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___64 = stv090x_read_reg(state, 61963U);
      tmp___66 = tmp___64;
    } else {
      tmp___65 = stv090x_read_reg(state, 62475U);
      tmp___66 = tmp___65;
    }
    power_iq = ((tmp___63 + tmp___66) >> 1) + power_iq;
    i = i + 1;
    ldv_24530: ;
    if (i <= 4) {
      goto ldv_24529;
    } else {
    }
    power_iq = power_iq / 5;
  } else {
  }
  if (agc1_power == 0 && power_iq <= 29) {
    if (verbose != 0U && verbose != 0U) {
      printk("\v%s: No Signal: POWER_IQ=0x%02x\n", "stv090x_algo", power_iq);
    } else
    if (verbose > 1U && verbose != 0U) {
      printk("\r%s: No Signal: POWER_IQ=0x%02x\n", "stv090x_algo", power_iq);
    } else
    if (verbose > 2U && verbose != 0U) {
      printk("\016%s: No Signal: POWER_IQ=0x%02x\n", "stv090x_algo", power_iq);
    } else
    if (verbose > 3U && verbose != 0U) {
      printk("\017%s: No Signal: POWER_IQ=0x%02x\n", "stv090x_algo", power_iq);
    } else
    if (verbose != 0U) {
      printk("No Signal: POWER_IQ=0x%02x", power_iq);
    } else {
    }
    lock = 0;
    signal_state = STV090x_NOAGC1;
  } else {
    if ((unsigned int )state->demod == 2U) {
      tmp___67 = stv090x_read_reg(state, 61968U);
      tmp___69 = tmp___67;
    } else {
      tmp___68 = stv090x_read_reg(state, 62480U);
      tmp___69 = tmp___68;
    }
    reg = (u32 )tmp___69;
    reg = (reg & 4294967247U) | ((unsigned int )state->inversion << 4);
    if ((state->internal)->dev_ver <= 32U) {
      reg = reg | 4U;
    } else {
      reg = reg | 128U;
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___70 = stv090x_write_reg(state, 61968U, (int )((u8 )reg));
      tmp___72 = tmp___70 < 0;
    } else {
      tmp___71 = stv090x_write_reg(state, 62480U, (int )((u8 )reg));
      tmp___72 = tmp___71 < 0;
    }
    if (tmp___72) {
      goto err;
    } else {
    }
    tmp___73 = stv090x_delivery_search(state);
    if (tmp___73 < 0) {
      goto err;
    } else {
    }
    if ((unsigned int )state->algo != 0U) {
      tmp___74 = stv090x_start_search(state);
      if (tmp___74 < 0) {
        goto err;
      } else {
      }
    } else {
    }
  }
  if ((unsigned int )signal_state == 0U) {
    return (signal_state);
  } else {
  }
  if ((unsigned int )state->algo == 0U) {
    lock = stv090x_blind_search(state);
  } else
  if ((unsigned int )state->algo == 1U) {
    lock = stv090x_get_coldlock(state, state->DemodTimeout);
  } else
  if ((unsigned int )state->algo == 2U) {
    lock = stv090x_get_dmdlock(state, state->DemodTimeout);
  } else {
  }
  if (lock == 0 && (unsigned int )state->algo == 1U) {
    if (low_sr == 0) {
      tmp___75 = stv090x_chk_tmg(state);
      if (tmp___75 != 0) {
        lock = stv090x_sw_algo(state);
      } else {
      }
    } else {
    }
  } else {
  }
  if (lock != 0) {
    signal_state = stv090x_get_sig_params(state);
  } else {
  }
  if (lock != 0 && (unsigned int )signal_state == 4U) {
    stv090x_optimize_track(state);
    if ((state->internal)->dev_ver > 31U) {
      if ((unsigned int )state->demod == 2U) {
        tmp___76 = stv090x_read_reg(state, 62322U);
        tmp___78 = tmp___76;
      } else {
        tmp___77 = stv090x_read_reg(state, 62834U);
        tmp___78 = tmp___77;
      }
      reg = (u32 )tmp___78;
      reg = reg & 4294967294U;
      if ((unsigned int )state->demod == 2U) {
        tmp___79 = stv090x_write_reg(state, 62322U, (int )((u8 )reg));
        tmp___81 = tmp___79 < 0;
      } else {
        tmp___80 = stv090x_write_reg(state, 62834U, (int )((u8 )reg));
        tmp___81 = tmp___80 < 0;
      }
      if (tmp___81) {
        goto err;
      } else {
      }
      msleep(3U);
      reg = reg | 1U;
      if ((unsigned int )state->demod == 2U) {
        tmp___82 = stv090x_write_reg(state, 62322U, (int )((u8 )reg));
        tmp___84 = tmp___82 < 0;
      } else {
        tmp___83 = stv090x_write_reg(state, 62834U, (int )((u8 )reg));
        tmp___84 = tmp___83 < 0;
      }
      if (tmp___84) {
        goto err;
      } else {
      }
      reg = reg & 4294967294U;
      if ((unsigned int )state->demod == 2U) {
        tmp___85 = stv090x_write_reg(state, 62322U, (int )((u8 )reg));
        tmp___87 = tmp___85 < 0;
      } else {
        tmp___86 = stv090x_write_reg(state, 62834U, (int )((u8 )reg));
        tmp___87 = tmp___86 < 0;
      }
      if (tmp___87) {
        goto err;
      } else {
      }
    } else {
    }
    lock = stv090x_get_lock(state, state->FecTimeout, state->FecTimeout);
    if (lock != 0) {
      if ((unsigned int )state->delsys == 2U) {
        stv090x_set_s2rolloff(state);
        if ((unsigned int )state->demod == 2U) {
          tmp___88 = stv090x_read_reg(state, 62289U);
          tmp___90 = tmp___88;
        } else {
          tmp___89 = stv090x_read_reg(state, 62801U);
          tmp___90 = tmp___89;
        }
        reg = (u32 )tmp___90;
        reg = reg | 64U;
        if ((unsigned int )state->demod == 2U) {
          tmp___91 = stv090x_write_reg(state, 62289U, (int )((u8 )reg));
          tmp___93 = tmp___91 < 0;
        } else {
          tmp___92 = stv090x_write_reg(state, 62801U, (int )((u8 )reg));
          tmp___93 = tmp___92 < 0;
        }
        if (tmp___93) {
          goto err;
        } else {
        }
        if ((unsigned int )state->demod == 2U) {
          tmp___94 = stv090x_read_reg(state, 62289U);
          tmp___96 = tmp___94;
        } else {
          tmp___95 = stv090x_read_reg(state, 62801U);
          tmp___96 = tmp___95;
        }
        reg = (u32 )tmp___96;
        reg = reg & 4294967231U;
        if ((unsigned int )state->demod == 2U) {
          tmp___97 = stv090x_write_reg(state, 62289U, (int )((u8 )reg));
          tmp___99 = tmp___97 < 0;
        } else {
          tmp___98 = stv090x_write_reg(state, 62801U, (int )((u8 )reg));
          tmp___99 = tmp___98 < 0;
        }
        if (tmp___99) {
          goto err;
        } else {
        }
        if ((unsigned int )state->demod == 2U) {
          tmp___100 = stv090x_write_reg(state, 62360U, 103);
          tmp___102 = tmp___100 < 0;
        } else {
          tmp___101 = stv090x_write_reg(state, 62872U, 103);
          tmp___102 = tmp___101 < 0;
        }
        if (tmp___102) {
          goto err;
        } else {
        }
      } else {
        if ((unsigned int )state->demod == 2U) {
          tmp___103 = stv090x_write_reg(state, 62360U, 117);
          tmp___105 = tmp___103 < 0;
        } else {
          tmp___104 = stv090x_write_reg(state, 62872U, 117);
          tmp___105 = tmp___104 < 0;
        }
        if (tmp___105) {
          goto err;
        } else {
        }
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___106 = stv090x_write_reg(state, 62376U, 0);
        tmp___108 = tmp___106 < 0;
      } else {
        tmp___107 = stv090x_write_reg(state, 62888U, 0);
        tmp___108 = tmp___107 < 0;
      }
      if (tmp___108) {
        goto err;
      } else {
      }
      if ((unsigned int )state->demod == 2U) {
        tmp___109 = stv090x_write_reg(state, 62364U, 193);
        tmp___111 = tmp___109 < 0;
      } else {
        tmp___110 = stv090x_write_reg(state, 62876U, 193);
        tmp___111 = tmp___110 < 0;
      }
      if (tmp___111) {
        goto err;
      } else {
      }
    } else {
      signal_state = STV090x_NODATA;
      stv090x_chk_signal(state);
    }
  } else {
  }
  return (signal_state);
  err_gateoff:
  stv090x_i2c_gate_ctrl(state, 0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_algo");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_algo");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_algo");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_algo");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (4294967295L);
}
}
static int stv090x_set_mis(struct stv090x_state *state , int mis )
{
  u32 reg ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  {
  if (mis < 0 || mis > 255) {
    if (verbose != 0U && verbose > 3U) {
      printk("\v%s: Disable MIS filtering\n", "stv090x_set_mis");
    } else
    if (verbose > 1U && verbose > 3U) {
      printk("\r%s: Disable MIS filtering\n", "stv090x_set_mis");
    } else
    if (verbose > 2U && verbose > 3U) {
      printk("\016%s: Disable MIS filtering\n", "stv090x_set_mis");
    } else
    if (verbose > 3U && verbose > 3U) {
      printk("\017%s: Disable MIS filtering\n", "stv090x_set_mis");
    } else
    if (verbose > 3U) {
      printk("Disable MIS filtering");
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp = stv090x_read_reg(state, 62288U);
      tmp___1 = tmp;
    } else {
      tmp___0 = stv090x_read_reg(state, 62800U);
      tmp___1 = tmp___0;
    }
    reg = (u32 )tmp___1;
    reg = reg & 4294967263U;
    if ((unsigned int )state->demod == 2U) {
      tmp___2 = stv090x_write_reg(state, 62288U, (int )((u8 )reg));
      tmp___4 = tmp___2 < 0;
    } else {
      tmp___3 = stv090x_write_reg(state, 62800U, (int )((u8 )reg));
      tmp___4 = tmp___3 < 0;
    }
    if (tmp___4) {
      goto err;
    } else {
    }
  } else {
    if (verbose != 0U && verbose > 3U) {
      printk("\v%s: Enable MIS filtering - %d\n", "stv090x_set_mis", mis);
    } else
    if (verbose > 1U && verbose > 3U) {
      printk("\r%s: Enable MIS filtering - %d\n", "stv090x_set_mis", mis);
    } else
    if (verbose > 2U && verbose > 3U) {
      printk("\016%s: Enable MIS filtering - %d\n", "stv090x_set_mis", mis);
    } else
    if (verbose > 3U && verbose > 3U) {
      printk("\017%s: Enable MIS filtering - %d\n", "stv090x_set_mis", mis);
    } else
    if (verbose > 3U) {
      printk("Enable MIS filtering - %d", mis);
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___5 = stv090x_read_reg(state, 62288U);
      tmp___7 = tmp___5;
    } else {
      tmp___6 = stv090x_read_reg(state, 62800U);
      tmp___7 = tmp___6;
    }
    reg = (u32 )tmp___7;
    reg = reg | 32U;
    if ((unsigned int )state->demod == 2U) {
      tmp___8 = stv090x_write_reg(state, 62288U, (int )((u8 )reg));
      tmp___10 = tmp___8 < 0;
    } else {
      tmp___9 = stv090x_write_reg(state, 62800U, (int )((u8 )reg));
      tmp___10 = tmp___9 < 0;
    }
    if (tmp___10) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___11 = stv090x_write_reg(state, 62302U, (int )((u8 )mis));
      tmp___13 = tmp___11 < 0;
    } else {
      tmp___12 = stv090x_write_reg(state, 62814U, (int )((u8 )mis));
      tmp___13 = tmp___12 < 0;
    }
    if (tmp___13) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___14 = stv090x_write_reg(state, 62303U, 255);
      tmp___16 = tmp___14 < 0;
    } else {
      tmp___15 = stv090x_write_reg(state, 62815U, 255);
      tmp___16 = tmp___15 < 0;
    }
    if (tmp___16) {
      goto err;
    } else {
    }
  }
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_set_mis");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_set_mis");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_set_mis");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_set_mis");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static enum dvbfe_search stv090x_search(struct dvb_frontend *fe )
{
  struct stv090x_state *state ;
  struct dtv_frontend_properties *props ;
  enum stv090x_signal_state tmp ;
  {
  state = (struct stv090x_state *)fe->demodulator_priv;
  props = & fe->dtv_property_cache;
  if (props->frequency == 0U) {
    return (DVBFE_ALGO_SEARCH_INVALID);
  } else {
  }
  state->delsys = (enum stv090x_delsys )props->delivery_system;
  state->frequency = props->frequency;
  state->srate = props->symbol_rate;
  state->search_mode = STV090x_SEARCH_AUTO;
  state->algo = STV090x_COLD_SEARCH;
  state->fec = STV090x_PRERR;
  if (state->srate > 10000000U) {
    if (verbose != 0U && verbose > 3U) {
      printk("\v%s: Search range: 10 MHz\n", "stv090x_search");
    } else
    if (verbose > 1U && verbose > 3U) {
      printk("\r%s: Search range: 10 MHz\n", "stv090x_search");
    } else
    if (verbose > 2U && verbose > 3U) {
      printk("\016%s: Search range: 10 MHz\n", "stv090x_search");
    } else
    if (verbose > 3U && verbose > 3U) {
      printk("\017%s: Search range: 10 MHz\n", "stv090x_search");
    } else
    if (verbose > 3U) {
      printk("Search range: 10 MHz");
    } else {
    }
    state->search_range = 10000000;
  } else {
    if (verbose != 0U && verbose > 3U) {
      printk("\v%s: Search range: 5 MHz\n", "stv090x_search");
    } else
    if (verbose > 1U && verbose > 3U) {
      printk("\r%s: Search range: 5 MHz\n", "stv090x_search");
    } else
    if (verbose > 2U && verbose > 3U) {
      printk("\016%s: Search range: 5 MHz\n", "stv090x_search");
    } else
    if (verbose > 3U && verbose > 3U) {
      printk("\017%s: Search range: 5 MHz\n", "stv090x_search");
    } else
    if (verbose > 3U) {
      printk("Search range: 5 MHz");
    } else {
    }
    state->search_range = 5000000;
  }
  stv090x_set_mis(state, (int )props->stream_id);
  tmp = stv090x_algo(state);
  if ((unsigned int )tmp == 4U) {
    if (verbose != 0U && verbose > 3U) {
      printk("\v%s: Search success!\n", "stv090x_search");
    } else
    if (verbose > 1U && verbose > 3U) {
      printk("\r%s: Search success!\n", "stv090x_search");
    } else
    if (verbose > 2U && verbose > 3U) {
      printk("\016%s: Search success!\n", "stv090x_search");
    } else
    if (verbose > 3U && verbose > 3U) {
      printk("\017%s: Search success!\n", "stv090x_search");
    } else
    if (verbose > 3U) {
      printk("Search success!");
    } else {
    }
    return (DVBFE_ALGO_SEARCH_SUCCESS);
  } else {
    if (verbose != 0U && verbose > 3U) {
      printk("\v%s: Search failed!\n", "stv090x_search");
    } else
    if (verbose > 1U && verbose > 3U) {
      printk("\r%s: Search failed!\n", "stv090x_search");
    } else
    if (verbose > 2U && verbose > 3U) {
      printk("\016%s: Search failed!\n", "stv090x_search");
    } else
    if (verbose > 3U && verbose > 3U) {
      printk("\017%s: Search failed!\n", "stv090x_search");
    } else
    if (verbose > 3U) {
      printk("Search failed!");
    } else {
    }
    return (DVBFE_ALGO_SEARCH_FAILED);
  }
  return (DVBFE_ALGO_SEARCH_ERROR);
}
}
static int stv090x_read_status(struct dvb_frontend *fe , enum fe_status *status )
{
  struct stv090x_state *state ;
  u32 reg ;
  u32 dstatus ;
  u8 search_state ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  {
  state = (struct stv090x_state *)fe->demodulator_priv;
  *status = 0;
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 61970U);
    tmp___1 = tmp;
  } else {
    tmp___0 = stv090x_read_reg(state, 62482U);
    tmp___1 = tmp___0;
  }
  dstatus = (u32 )tmp___1;
  if ((dstatus & 128U) != 0U) {
    *status = (enum fe_status )((unsigned int )*status | 3U);
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_read_reg(state, 61979U);
    tmp___4 = tmp___2;
  } else {
    tmp___3 = stv090x_read_reg(state, 62491U);
    tmp___4 = tmp___3;
  }
  reg = (u32 )tmp___4;
  search_state = (unsigned int )((u8 )(reg >> 5)) & 3U;
  switch ((int )search_state) {
  case 0: ;
  case 1: ;
  default: ;
  if (verbose != 0U && verbose > 3U) {
    printk("\v%s: Status: Unlocked (Searching ..)\n", "stv090x_read_status");
  } else
  if (verbose > 1U && verbose > 3U) {
    printk("\r%s: Status: Unlocked (Searching ..)\n", "stv090x_read_status");
  } else
  if (verbose > 2U && verbose > 3U) {
    printk("\016%s: Status: Unlocked (Searching ..)\n", "stv090x_read_status");
  } else
  if (verbose > 3U && verbose > 3U) {
    printk("\017%s: Status: Unlocked (Searching ..)\n", "stv090x_read_status");
  } else
  if (verbose > 3U) {
    printk("Status: Unlocked (Searching ..)");
  } else {
  }
  goto ldv_24557;
  case 2: ;
  if (verbose != 0U && verbose > 3U) {
    printk("\v%s: Delivery system: DVB-S2\n", "stv090x_read_status");
  } else
  if (verbose > 1U && verbose > 3U) {
    printk("\r%s: Delivery system: DVB-S2\n", "stv090x_read_status");
  } else
  if (verbose > 2U && verbose > 3U) {
    printk("\016%s: Delivery system: DVB-S2\n", "stv090x_read_status");
  } else
  if (verbose > 3U && verbose > 3U) {
    printk("\017%s: Delivery system: DVB-S2\n", "stv090x_read_status");
  } else
  if (verbose > 3U) {
    printk("Delivery system: DVB-S2");
  } else {
  }
  if ((dstatus & 8U) != 0U) {
    if ((unsigned int )state->demod == 2U) {
      tmp___5 = stv090x_read_reg(state, 62313U);
      tmp___7 = tmp___5;
    } else {
      tmp___6 = stv090x_read_reg(state, 62825U);
      tmp___7 = tmp___6;
    }
    reg = (u32 )tmp___7;
    if ((reg & 2U) != 0U) {
      *status = (enum fe_status )((unsigned int )*status | 4U);
      if ((unsigned int )state->demod == 2U) {
        tmp___8 = stv090x_read_reg(state, 62337U);
        tmp___10 = tmp___8;
      } else {
        tmp___9 = stv090x_read_reg(state, 62849U);
        tmp___10 = tmp___9;
      }
      reg = (u32 )tmp___10;
      if ((reg & 128U) != 0U) {
        *status = (enum fe_status )((unsigned int )*status | 24U);
      } else {
      }
    } else {
    }
  } else {
  }
  goto ldv_24557;
  case 3: ;
  if (verbose != 0U && verbose > 3U) {
    printk("\v%s: Delivery system: DVB-S\n", "stv090x_read_status");
  } else
  if (verbose > 1U && verbose > 3U) {
    printk("\r%s: Delivery system: DVB-S\n", "stv090x_read_status");
  } else
  if (verbose > 2U && verbose > 3U) {
    printk("\016%s: Delivery system: DVB-S\n", "stv090x_read_status");
  } else
  if (verbose > 3U && verbose > 3U) {
    printk("\017%s: Delivery system: DVB-S\n", "stv090x_read_status");
  } else
  if (verbose > 3U) {
    printk("Delivery system: DVB-S");
  } else {
  }
  if ((dstatus & 8U) != 0U) {
    if ((unsigned int )state->demod == 2U) {
      tmp___11 = stv090x_read_reg(state, 62270U);
      tmp___13 = tmp___11;
    } else {
      tmp___12 = stv090x_read_reg(state, 62782U);
      tmp___13 = tmp___12;
    }
    reg = (u32 )tmp___13;
    if ((reg & 8U) != 0U) {
      *status = (enum fe_status )((unsigned int )*status | 4U);
      if ((unsigned int )state->demod == 2U) {
        tmp___14 = stv090x_read_reg(state, 62337U);
        tmp___16 = tmp___14;
      } else {
        tmp___15 = stv090x_read_reg(state, 62849U);
        tmp___16 = tmp___15;
      }
      reg = (u32 )tmp___16;
      if ((reg & 128U) != 0U) {
        *status = (enum fe_status )((unsigned int )*status | 24U);
      } else {
      }
    } else {
    }
  } else {
  }
  goto ldv_24557;
  }
  ldv_24557: ;
  return (0);
}
}
static int stv090x_read_per(struct dvb_frontend *fe , u32 *per )
{
  struct stv090x_state *state ;
  s32 count_4 ;
  s32 count_3 ;
  s32 count_2 ;
  s32 count_1 ;
  s32 count_0 ;
  s32 count ;
  u32 reg ;
  u32 h ;
  u32 m ;
  u32 l ;
  enum fe_status status ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  int tmp___23 ;
  {
  state = (struct stv090x_state *)fe->demodulator_priv;
  stv090x_read_status(fe, & status);
  if (((unsigned int )status & 16U) == 0U) {
    *per = 8388608U;
  } else {
    if ((unsigned int )state->demod == 2U) {
      tmp = stv090x_read_reg(state, 62365U);
      tmp___1 = tmp;
    } else {
      tmp___0 = stv090x_read_reg(state, 62877U);
      tmp___1 = tmp___0;
    }
    reg = (u32 )tmp___1;
    h = reg & 127U;
    if ((unsigned int )state->demod == 2U) {
      tmp___2 = stv090x_read_reg(state, 62366U);
      tmp___4 = tmp___2;
    } else {
      tmp___3 = stv090x_read_reg(state, 62878U);
      tmp___4 = tmp___3;
    }
    reg = (u32 )tmp___4;
    m = reg & 255U;
    if ((unsigned int )state->demod == 2U) {
      tmp___5 = stv090x_read_reg(state, 62367U);
      tmp___7 = tmp___5;
    } else {
      tmp___6 = stv090x_read_reg(state, 62879U);
      tmp___7 = tmp___6;
    }
    reg = (u32 )tmp___7;
    l = reg & 255U;
    *per = ((h << 16) | (m << 8)) | l;
    if ((unsigned int )state->demod == 2U) {
      tmp___8 = stv090x_read_reg(state, 62376U);
      count_4 = tmp___8;
    } else {
      tmp___9 = stv090x_read_reg(state, 62888U);
      count_4 = tmp___9;
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___10 = stv090x_read_reg(state, 62377U);
      count_3 = tmp___10;
    } else {
      tmp___11 = stv090x_read_reg(state, 62889U);
      count_3 = tmp___11;
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___12 = stv090x_read_reg(state, 62378U);
      count_2 = tmp___12;
    } else {
      tmp___13 = stv090x_read_reg(state, 62890U);
      count_2 = tmp___13;
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___14 = stv090x_read_reg(state, 62379U);
      count_1 = tmp___14;
    } else {
      tmp___15 = stv090x_read_reg(state, 62891U);
      count_1 = tmp___15;
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___16 = stv090x_read_reg(state, 62380U);
      count_0 = tmp___16;
    } else {
      tmp___17 = stv090x_read_reg(state, 62892U);
      count_0 = tmp___17;
    }
    if (count_4 == 0 && count_3 == 0) {
      count = (count_2 & 255) << 16;
      count = ((count_1 << 8) & 65535) | count;
      count = (count_0 & 255) | count;
    } else {
      count = 16777216;
    }
    if (count == 0) {
      *per = 1U;
    } else {
    }
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___18 = stv090x_write_reg(state, 62376U, 0);
    tmp___20 = tmp___18 < 0;
  } else {
    tmp___19 = stv090x_write_reg(state, 62888U, 0);
    tmp___20 = tmp___19 < 0;
  }
  if (tmp___20) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___21 = stv090x_write_reg(state, 62364U, 193);
    tmp___23 = tmp___21 < 0;
  } else {
    tmp___22 = stv090x_write_reg(state, 62876U, 193);
    tmp___23 = tmp___22 < 0;
  }
  if (tmp___23) {
    goto err;
  } else {
  }
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_read_per");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_read_per");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_read_per");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_read_per");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_table_lookup(struct stv090x_tab const *tab , int max , int val )
{
  int res ;
  int min ;
  int med ;
  {
  res = 0;
  min = 0;
  if (((int )(tab + (unsigned long )min)->read <= val && (int )(tab + (unsigned long )max)->read > val) || ((int )(tab + (unsigned long )max)->read <= val && (int )(tab + (unsigned long )min)->read > val)) {
    goto ldv_24587;
    ldv_24586:
    med = (max + min) / 2;
    if (((int )(tab + (unsigned long )min)->read <= val && (int )(tab + (unsigned long )med)->read > val) || ((int )(tab + (unsigned long )med)->read <= val && (int )(tab + (unsigned long )min)->read > val)) {
      max = med;
    } else {
      min = med;
    }
    ldv_24587: ;
    if (max - min > 1) {
      goto ldv_24586;
    } else {
    }
    res = ((val - (int )(tab + (unsigned long )min)->read) * ((int )(tab + (unsigned long )max)->real - (int )(tab + (unsigned long )min)->real)) / ((int )(tab + (unsigned long )max)->read - (int )(tab + (unsigned long )min)->read) + (int )(tab + (unsigned long )min)->real;
  } else
  if ((int )(tab + (unsigned long )min)->read < (int )(tab + (unsigned long )max)->read) {
    if ((int )(tab + (unsigned long )min)->read > val) {
      res = (tab + (unsigned long )min)->real;
    } else
    if ((int )(tab + (unsigned long )max)->read <= val) {
      res = (tab + (unsigned long )max)->real;
    } else
    if ((int )(tab + (unsigned long )min)->read <= val) {
      res = (tab + (unsigned long )min)->real;
    } else
    if ((int )(tab + (unsigned long )max)->read > val) {
      res = (tab + (unsigned long )max)->real;
    } else {
    }
  } else {
  }
  return (res);
}
}
static int stv090x_read_signal_strength(struct dvb_frontend *fe , u16 *strength )
{
  struct stv090x_state *state ;
  u32 reg ;
  s32 agc_0 ;
  s32 agc_1 ;
  s32 agc ;
  s32 str ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  {
  state = (struct stv090x_state *)fe->demodulator_priv;
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 61966U);
    tmp___1 = tmp;
  } else {
    tmp___0 = stv090x_read_reg(state, 62478U);
    tmp___1 = tmp___0;
  }
  reg = (u32 )tmp___1;
  agc_1 = (s32 )reg & 255;
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_read_reg(state, 61967U);
    tmp___4 = tmp___2;
  } else {
    tmp___3 = stv090x_read_reg(state, 62479U);
    tmp___4 = tmp___3;
  }
  reg = (u32 )tmp___4;
  agc_0 = (s32 )reg & 255;
  agc = (agc_1 << 8) | agc_0;
  str = stv090x_table_lookup((struct stv090x_tab const *)(& stv090x_rf_tab), 13,
                             agc);
  if ((int )stv090x_rf_tab[0].read < agc) {
    str = 0;
  } else
  if ((int )stv090x_rf_tab[13UL].read > agc) {
    str = -100;
  } else {
  }
  *strength = (u16 )((str * 65535 + 6553500) / 100);
  return (0);
}
}
static int stv090x_read_cnr(struct dvb_frontend *fe , u16 *cnr )
{
  struct stv090x_state *state ;
  u32 reg_0 ;
  u32 reg_1 ;
  u32 reg ;
  u32 i ;
  s32 val_0 ;
  s32 val_1 ;
  s32 val ;
  u8 lock_f ;
  s32 div ;
  u32 last ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  {
  state = (struct stv090x_state *)fe->demodulator_priv;
  val = 0;
  switch ((unsigned int )state->delsys) {
  case 2U: ;
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 61970U);
    tmp___1 = tmp;
  } else {
    tmp___0 = stv090x_read_reg(state, 62482U);
    tmp___1 = tmp___0;
  }
  reg = (u32 )tmp___1;
  lock_f = (unsigned int )((u8 )(reg >> 3)) & 1U;
  if ((unsigned int )lock_f != 0U) {
    msleep(5U);
    i = 0U;
    goto ldv_24620;
    ldv_24619: ;
    if ((unsigned int )state->demod == 2U) {
      tmp___2 = stv090x_read_reg(state, 62084U);
      tmp___4 = tmp___2;
    } else {
      tmp___3 = stv090x_read_reg(state, 62596U);
      tmp___4 = tmp___3;
    }
    reg_1 = (u32 )tmp___4;
    val_1 = (s32 )reg_1 & 255;
    if ((unsigned int )state->demod == 2U) {
      tmp___5 = stv090x_read_reg(state, 62085U);
      tmp___7 = tmp___5;
    } else {
      tmp___6 = stv090x_read_reg(state, 62597U);
      tmp___7 = tmp___6;
    }
    reg_0 = (u32 )tmp___7;
    val_0 = (s32 )reg_0 & 255;
    val = ((val_1 << 8) | val_0) + val;
    msleep(1U);
    i = i + (u32 )1;
    ldv_24620: ;
    if (i <= 15U) {
      goto ldv_24619;
    } else {
    }
    val = val / 16;
    last = 54U;
    div = (int )stv090x_s2cn_tab[0].read - (int )stv090x_s2cn_tab[last].read;
    *cnr = ~ ((int )((u16 )((val * 65535) / div)));
  } else {
  }
  goto ldv_24624;
  case 1U: ;
  case 3U: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___8 = stv090x_read_reg(state, 61970U);
    tmp___10 = tmp___8;
  } else {
    tmp___9 = stv090x_read_reg(state, 62482U);
    tmp___10 = tmp___9;
  }
  reg = (u32 )tmp___10;
  lock_f = (unsigned int )((u8 )(reg >> 3)) & 1U;
  if ((unsigned int )lock_f != 0U) {
    msleep(5U);
    i = 0U;
    goto ldv_24628;
    ldv_24627: ;
    if ((unsigned int )state->demod == 2U) {
      tmp___11 = stv090x_read_reg(state, 62088U);
      tmp___13 = tmp___11;
    } else {
      tmp___12 = stv090x_read_reg(state, 62600U);
      tmp___13 = tmp___12;
    }
    reg_1 = (u32 )tmp___13;
    val_1 = (s32 )reg_1 & 255;
    if ((unsigned int )state->demod == 2U) {
      tmp___14 = stv090x_read_reg(state, 62089U);
      tmp___16 = tmp___14;
    } else {
      tmp___15 = stv090x_read_reg(state, 62601U);
      tmp___16 = tmp___15;
    }
    reg_0 = (u32 )tmp___16;
    val_0 = (s32 )reg_0 & 255;
    val = ((val_1 << 8) | val_0) + val;
    msleep(1U);
    i = i + (u32 )1;
    ldv_24628: ;
    if (i <= 15U) {
      goto ldv_24627;
    } else {
    }
    val = val / 16;
    last = 51U;
    div = (int )stv090x_s1cn_tab[0].read - (int )stv090x_s1cn_tab[last].read;
    *cnr = ~ ((int )((u16 )((val * 65535) / div)));
  } else {
  }
  goto ldv_24624;
  default: ;
  goto ldv_24624;
  }
  ldv_24624: ;
  return (0);
}
}
static int stv090x_set_tone(struct dvb_frontend *fe , fe_sec_tone_mode_t tone )
{
  struct stv090x_state *state ;
  u32 reg ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  {
  state = (struct stv090x_state *)fe->demodulator_priv;
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 61840U);
    tmp___1 = tmp;
  } else {
    tmp___0 = stv090x_read_reg(state, 61856U);
    tmp___1 = tmp___0;
  }
  reg = (u32 )tmp___1;
  switch ((unsigned int )tone) {
  case 0U:
  reg = reg & 4294967288U;
  reg = reg | 64U;
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_write_reg(state, 61840U, (int )((u8 )reg));
    tmp___4 = tmp___2 < 0;
  } else {
    tmp___3 = stv090x_write_reg(state, 61856U, (int )((u8 )reg));
    tmp___4 = tmp___3 < 0;
  }
  if (tmp___4) {
    goto err;
  } else {
  }
  reg = reg & 4294967231U;
  if ((unsigned int )state->demod == 2U) {
    tmp___5 = stv090x_write_reg(state, 61840U, (int )((u8 )reg));
    tmp___7 = tmp___5 < 0;
  } else {
    tmp___6 = stv090x_write_reg(state, 61856U, (int )((u8 )reg));
    tmp___7 = tmp___6 < 0;
  }
  if (tmp___7) {
    goto err;
  } else {
  }
  goto ldv_24641;
  case 1U:
  reg = reg & 4294967288U;
  reg = reg | 64U;
  if ((unsigned int )state->demod == 2U) {
    tmp___8 = stv090x_write_reg(state, 61840U, (int )((u8 )reg));
    tmp___10 = tmp___8 < 0;
  } else {
    tmp___9 = stv090x_write_reg(state, 61856U, (int )((u8 )reg));
    tmp___10 = tmp___9 < 0;
  }
  if (tmp___10) {
    goto err;
  } else {
  }
  goto ldv_24641;
  default: ;
  return (-22);
  }
  ldv_24641: ;
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_set_tone");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_set_tone");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_set_tone");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_set_tone");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static enum dvbfe_algo stv090x_frontend_algo(struct dvb_frontend *fe )
{
  {
  return (DVBFE_ALGO_CUSTOM);
}
}
static int stv090x_send_diseqc_msg(struct dvb_frontend *fe , struct dvb_diseqc_master_cmd *cmd )
{
  struct stv090x_state *state ;
  u32 reg ;
  u32 idle ;
  u32 fifo_full ;
  int i ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  int tmp___23 ;
  int tmp___24 ;
  int tmp___25 ;
  {
  state = (struct stv090x_state *)fe->demodulator_priv;
  idle = 0U;
  fifo_full = 1U;
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 61840U);
    tmp___1 = tmp;
  } else {
    tmp___0 = stv090x_read_reg(state, 61856U);
    tmp___1 = tmp___0;
  }
  reg = (u32 )tmp___1;
  reg = (reg & 4294967288U) | ((int )(state->config)->diseqc_envelope_mode ? 4U : 2U);
  reg = reg | 64U;
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_write_reg(state, 61840U, (int )((u8 )reg));
    tmp___4 = tmp___2 < 0;
  } else {
    tmp___3 = stv090x_write_reg(state, 61856U, (int )((u8 )reg));
    tmp___4 = tmp___3 < 0;
  }
  if (tmp___4) {
    goto err;
  } else {
  }
  reg = reg & 4294967231U;
  if ((unsigned int )state->demod == 2U) {
    tmp___5 = stv090x_write_reg(state, 61840U, (int )((u8 )reg));
    tmp___7 = tmp___5 < 0;
  } else {
    tmp___6 = stv090x_write_reg(state, 61856U, (int )((u8 )reg));
    tmp___7 = tmp___6 < 0;
  }
  if (tmp___7) {
    goto err;
  } else {
  }
  reg = reg | 8U;
  if ((unsigned int )state->demod == 2U) {
    tmp___8 = stv090x_write_reg(state, 61840U, (int )((u8 )reg));
    tmp___10 = tmp___8 < 0;
  } else {
    tmp___9 = stv090x_write_reg(state, 61856U, (int )((u8 )reg));
    tmp___10 = tmp___9 < 0;
  }
  if (tmp___10) {
    goto err;
  } else {
  }
  i = 0;
  goto ldv_24662;
  ldv_24661: ;
  goto ldv_24659;
  ldv_24658: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___11 = stv090x_read_reg(state, 61848U);
    tmp___13 = tmp___11;
  } else {
    tmp___12 = stv090x_read_reg(state, 61864U);
    tmp___13 = tmp___12;
  }
  reg = (u32 )tmp___13;
  fifo_full = (reg >> 6) & 1U;
  ldv_24659: ;
  if (fifo_full != 0U) {
    goto ldv_24658;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___14 = stv090x_write_reg(state, 61847U, (int )cmd->msg[i]);
    tmp___16 = tmp___14 < 0;
  } else {
    tmp___15 = stv090x_write_reg(state, 61863U, (int )cmd->msg[i]);
    tmp___16 = tmp___15 < 0;
  }
  if (tmp___16) {
    goto err;
  } else {
  }
  i = i + 1;
  ldv_24662: ;
  if ((int )cmd->msg_len > i) {
    goto ldv_24661;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___17 = stv090x_read_reg(state, 61840U);
    tmp___19 = tmp___17;
  } else {
    tmp___18 = stv090x_read_reg(state, 61856U);
    tmp___19 = tmp___18;
  }
  reg = (u32 )tmp___19;
  reg = reg & 4294967287U;
  if ((unsigned int )state->demod == 2U) {
    tmp___20 = stv090x_write_reg(state, 61840U, (int )((u8 )reg));
    tmp___22 = tmp___20 < 0;
  } else {
    tmp___21 = stv090x_write_reg(state, 61856U, (int )((u8 )reg));
    tmp___22 = tmp___21 < 0;
  }
  if (tmp___22) {
    goto err;
  } else {
  }
  i = 0;
  goto ldv_24665;
  ldv_24664: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___23 = stv090x_read_reg(state, 61848U);
    tmp___25 = tmp___23;
  } else {
    tmp___24 = stv090x_read_reg(state, 61864U);
    tmp___25 = tmp___24;
  }
  reg = (u32 )tmp___25;
  idle = (reg >> 5) & 1U;
  msleep(10U);
  i = i + 1;
  ldv_24665: ;
  if (idle == 0U && i <= 9) {
    goto ldv_24664;
  } else {
  }
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_send_diseqc_msg");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_send_diseqc_msg");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_send_diseqc_msg");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_send_diseqc_msg");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_send_diseqc_burst(struct dvb_frontend *fe , fe_sec_mini_cmd_t burst )
{
  struct stv090x_state *state ;
  u32 reg ;
  u32 idle ;
  u32 fifo_full ;
  u8 mode ;
  u8 value ;
  int i ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  int tmp___23 ;
  int tmp___24 ;
  int tmp___25 ;
  {
  state = (struct stv090x_state *)fe->demodulator_priv;
  idle = 0U;
  fifo_full = 1U;
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 61840U);
    tmp___1 = tmp;
  } else {
    tmp___0 = stv090x_read_reg(state, 61856U);
    tmp___1 = tmp___0;
  }
  reg = (u32 )tmp___1;
  if ((unsigned int )burst == 0U) {
    mode = (int )(state->config)->diseqc_envelope_mode ? 5U : 3U;
    value = 0U;
  } else {
    mode = (int )(state->config)->diseqc_envelope_mode ? 4U : 2U;
    value = 255U;
  }
  reg = (reg & 4294967288U) | (u32 )mode;
  reg = reg | 64U;
  if ((unsigned int )state->demod == 2U) {
    tmp___2 = stv090x_write_reg(state, 61840U, (int )((u8 )reg));
    tmp___4 = tmp___2 < 0;
  } else {
    tmp___3 = stv090x_write_reg(state, 61856U, (int )((u8 )reg));
    tmp___4 = tmp___3 < 0;
  }
  if (tmp___4) {
    goto err;
  } else {
  }
  reg = reg & 4294967231U;
  if ((unsigned int )state->demod == 2U) {
    tmp___5 = stv090x_write_reg(state, 61840U, (int )((u8 )reg));
    tmp___7 = tmp___5 < 0;
  } else {
    tmp___6 = stv090x_write_reg(state, 61856U, (int )((u8 )reg));
    tmp___7 = tmp___6 < 0;
  }
  if (tmp___7) {
    goto err;
  } else {
  }
  reg = reg | 8U;
  if ((unsigned int )state->demod == 2U) {
    tmp___8 = stv090x_write_reg(state, 61840U, (int )((u8 )reg));
    tmp___10 = tmp___8 < 0;
  } else {
    tmp___9 = stv090x_write_reg(state, 61856U, (int )((u8 )reg));
    tmp___10 = tmp___9 < 0;
  }
  if (tmp___10) {
    goto err;
  } else {
  }
  goto ldv_24681;
  ldv_24680: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___11 = stv090x_read_reg(state, 61848U);
    tmp___13 = tmp___11;
  } else {
    tmp___12 = stv090x_read_reg(state, 61864U);
    tmp___13 = tmp___12;
  }
  reg = (u32 )tmp___13;
  fifo_full = (reg >> 6) & 1U;
  ldv_24681: ;
  if (fifo_full != 0U) {
    goto ldv_24680;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___14 = stv090x_write_reg(state, 61847U, (int )value);
    tmp___16 = tmp___14 < 0;
  } else {
    tmp___15 = stv090x_write_reg(state, 61863U, (int )value);
    tmp___16 = tmp___15 < 0;
  }
  if (tmp___16) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___17 = stv090x_read_reg(state, 61840U);
    tmp___19 = tmp___17;
  } else {
    tmp___18 = stv090x_read_reg(state, 61856U);
    tmp___19 = tmp___18;
  }
  reg = (u32 )tmp___19;
  reg = reg & 4294967287U;
  if ((unsigned int )state->demod == 2U) {
    tmp___20 = stv090x_write_reg(state, 61840U, (int )((u8 )reg));
    tmp___22 = tmp___20 < 0;
  } else {
    tmp___21 = stv090x_write_reg(state, 61856U, (int )((u8 )reg));
    tmp___22 = tmp___21 < 0;
  }
  if (tmp___22) {
    goto err;
  } else {
  }
  i = 0;
  goto ldv_24684;
  ldv_24683: ;
  if ((unsigned int )state->demod == 2U) {
    tmp___23 = stv090x_read_reg(state, 61848U);
    tmp___25 = tmp___23;
  } else {
    tmp___24 = stv090x_read_reg(state, 61864U);
    tmp___25 = tmp___24;
  }
  reg = (u32 )tmp___25;
  idle = (reg >> 5) & 1U;
  msleep(10U);
  i = i + 1;
  ldv_24684: ;
  if (idle == 0U && i <= 9) {
    goto ldv_24683;
  } else {
  }
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_send_diseqc_burst");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_send_diseqc_burst");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_send_diseqc_burst");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_send_diseqc_burst");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_recv_slave_reply(struct dvb_frontend *fe , struct dvb_diseqc_slave_reply *reply )
{
  struct stv090x_state *state ;
  u32 reg ;
  u32 i ;
  u32 rx_end ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  {
  state = (struct stv090x_state *)fe->demodulator_priv;
  reg = 0U;
  i = 0U;
  rx_end = 0U;
  goto ldv_24696;
  ldv_24695:
  msleep(10U);
  i = i + (u32 )1;
  if ((unsigned int )state->demod == 2U) {
    tmp = stv090x_read_reg(state, 61844U);
    tmp___1 = tmp;
  } else {
    tmp___0 = stv090x_read_reg(state, 61860U);
    tmp___1 = tmp___0;
  }
  reg = (u32 )tmp___1;
  rx_end = (reg >> 7) & 1U;
  ldv_24696: ;
  if (rx_end != 1U && i <= 9U) {
    goto ldv_24695;
  } else {
  }
  if (rx_end != 0U) {
    reply->msg_len = (unsigned int )((__u8 )reg) & 15U;
    i = 0U;
    goto ldv_24699;
    ldv_24698: ;
    if ((unsigned int )state->demod == 2U) {
      tmp___2 = stv090x_read_reg(state, 61846U);
      reply->msg[i] = (__u8 )tmp___2;
    } else {
      tmp___3 = stv090x_read_reg(state, 61862U);
      reply->msg[i] = (__u8 )tmp___3;
    }
    i = i + (u32 )1;
    ldv_24699: ;
    if ((u32 )reply->msg_len > i) {
      goto ldv_24698;
    } else {
    }
  } else {
  }
  return (0);
}
}
static int stv090x_sleep(struct dvb_frontend *fe )
{
  struct stv090x_state *state ;
  u32 reg ;
  u8 full_standby ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  {
  state = (struct stv090x_state *)fe->demodulator_priv;
  full_standby = 0U;
  tmp = stv090x_i2c_gate_ctrl(state, 1);
  if (tmp < 0) {
    goto err;
  } else {
  }
  if ((unsigned long )(state->config)->tuner_sleep != (unsigned long )((int (* )(struct dvb_frontend * ))0)) {
    tmp___0 = (*((state->config)->tuner_sleep))(fe);
    if (tmp___0 < 0) {
      goto err_gateoff;
    } else {
    }
  } else {
  }
  tmp___1 = stv090x_i2c_gate_ctrl(state, 0);
  if (tmp___1 < 0) {
    goto err;
  } else {
  }
  if (verbose != 0U && verbose > 3U) {
    printk("\v%s: Set %s(%d) to sleep\n", "stv090x_sleep", (unsigned int )state->device == 1U ? (char *)"STV0900" : (char *)"STV0903",
           (unsigned int )state->demod);
  } else
  if (verbose > 1U && verbose > 3U) {
    printk("\r%s: Set %s(%d) to sleep\n", "stv090x_sleep", (unsigned int )state->device == 1U ? (char *)"STV0900" : (char *)"STV0903",
           (unsigned int )state->demod);
  } else
  if (verbose > 2U && verbose > 3U) {
    printk("\016%s: Set %s(%d) to sleep\n", "stv090x_sleep", (unsigned int )state->device == 1U ? (char *)"STV0900" : (char *)"STV0903",
           (unsigned int )state->demod);
  } else
  if (verbose > 3U && verbose > 3U) {
    printk("\017%s: Set %s(%d) to sleep\n", "stv090x_sleep", (unsigned int )state->device == 1U ? (char *)"STV0900" : (char *)"STV0903",
           (unsigned int )state->demod);
  } else
  if (verbose > 3U) {
    printk("Set %s(%d) to sleep", (unsigned int )state->device == 1U ? (char *)"STV0900" : (char *)"STV0903",
           (unsigned int )state->demod);
  } else {
  }
  ldv_mutex_lock_20(& (state->internal)->demod_lock);
  switch ((unsigned int )state->demod) {
  case 1U:
  tmp___2 = stv090x_read_reg(state, 61920U);
  reg = (u32 )tmp___2;
  reg = reg & 4294967293U;
  tmp___3 = stv090x_write_reg(state, 61920U, (int )((u8 )reg));
  if (tmp___3 < 0) {
    goto err;
  } else {
  }
  tmp___4 = stv090x_read_reg(state, 61921U);
  reg = (u32 )tmp___4;
  reg = reg & 4294967263U;
  tmp___5 = stv090x_write_reg(state, 61921U, (int )((u8 )reg));
  if (tmp___5 < 0) {
    goto err;
  } else {
  }
  tmp___6 = stv090x_read_reg(state, 61922U);
  reg = (u32 )tmp___6;
  if ((reg & 2U) == 0U) {
    full_standby = 1U;
  } else {
  }
  tmp___7 = stv090x_read_reg(state, 61890U);
  reg = (u32 )tmp___7;
  reg = reg | 32U;
  reg = reg | 2U;
  if ((unsigned int )full_standby != 0U) {
    reg = reg | 16U;
  } else {
  }
  tmp___8 = stv090x_write_reg(state, 61890U, (int )((u8 )reg));
  if (tmp___8 < 0) {
    goto err;
  } else {
  }
  tmp___9 = stv090x_read_reg(state, 61891U);
  reg = (u32 )tmp___9;
  reg = reg | 8U;
  reg = reg | 2U;
  if ((unsigned int )full_standby != 0U) {
    reg = reg | 1U;
  } else {
  }
  tmp___10 = stv090x_write_reg(state, 61891U, (int )((u8 )reg));
  if (tmp___10 < 0) {
    goto err;
  } else {
  }
  goto ldv_24711;
  case 2U:
  tmp___11 = stv090x_read_reg(state, 61922U);
  reg = (u32 )tmp___11;
  reg = reg & 4294967293U;
  tmp___12 = stv090x_write_reg(state, 61922U, (int )((u8 )reg));
  if (tmp___12 < 0) {
    goto err;
  } else {
  }
  tmp___13 = stv090x_read_reg(state, 61923U);
  reg = (u32 )tmp___13;
  reg = reg & 4294967263U;
  tmp___14 = stv090x_write_reg(state, 61923U, (int )((u8 )reg));
  if (tmp___14 < 0) {
    goto err;
  } else {
  }
  tmp___15 = stv090x_read_reg(state, 61920U);
  reg = (u32 )tmp___15;
  if ((reg & 2U) == 0U) {
    full_standby = 1U;
  } else {
  }
  tmp___16 = stv090x_read_reg(state, 61890U);
  reg = (u32 )tmp___16;
  reg = reg | 64U;
  reg = reg | 8U;
  if ((unsigned int )full_standby != 0U) {
    reg = reg | 16U;
  } else {
  }
  tmp___17 = stv090x_write_reg(state, 61890U, (int )((u8 )reg));
  if (tmp___17 < 0) {
    goto err;
  } else {
  }
  tmp___18 = stv090x_read_reg(state, 61891U);
  reg = (u32 )tmp___18;
  reg = reg | 16U;
  reg = reg | 4U;
  if ((unsigned int )full_standby != 0U) {
    reg = reg | 1U;
  } else {
  }
  tmp___19 = stv090x_write_reg(state, 61891U, (int )((u8 )reg));
  if (tmp___19 < 0) {
    goto err;
  } else {
  }
  goto ldv_24711;
  default: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: Wrong demodulator!\n", "stv090x_sleep");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: Wrong demodulator!\n", "stv090x_sleep");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: Wrong demodulator!\n", "stv090x_sleep");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: Wrong demodulator!\n", "stv090x_sleep");
  } else
  if (verbose != 0U) {
    printk("Wrong demodulator!");
  } else {
  }
  goto ldv_24711;
  }
  ldv_24711: ;
  if ((unsigned int )full_standby != 0U) {
    tmp___20 = stv090x_read_reg(state, 61878U);
    reg = (u32 )tmp___20;
    reg = reg | 128U;
    tmp___21 = stv090x_write_reg(state, 61878U, (int )((u8 )reg));
    if (tmp___21 < 0) {
      goto err;
    } else {
    }
  } else {
  }
  ldv_mutex_unlock_21(& (state->internal)->demod_lock);
  return (0);
  err_gateoff:
  stv090x_i2c_gate_ctrl(state, 0);
  err:
  ldv_mutex_unlock_22(& (state->internal)->demod_lock);
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_sleep");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_sleep");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_sleep");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_sleep");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_wakeup(struct dvb_frontend *fe )
{
  struct stv090x_state *state ;
  u32 reg ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  {
  state = (struct stv090x_state *)fe->demodulator_priv;
  if (verbose != 0U && verbose > 3U) {
    printk("\v%s: Wake %s(%d) from standby\n", "stv090x_wakeup", (unsigned int )state->device == 1U ? (char *)"STV0900" : (char *)"STV0903",
           (unsigned int )state->demod);
  } else
  if (verbose > 1U && verbose > 3U) {
    printk("\r%s: Wake %s(%d) from standby\n", "stv090x_wakeup", (unsigned int )state->device == 1U ? (char *)"STV0900" : (char *)"STV0903",
           (unsigned int )state->demod);
  } else
  if (verbose > 2U && verbose > 3U) {
    printk("\016%s: Wake %s(%d) from standby\n", "stv090x_wakeup", (unsigned int )state->device == 1U ? (char *)"STV0900" : (char *)"STV0903",
           (unsigned int )state->demod);
  } else
  if (verbose > 3U && verbose > 3U) {
    printk("\017%s: Wake %s(%d) from standby\n", "stv090x_wakeup", (unsigned int )state->device == 1U ? (char *)"STV0900" : (char *)"STV0903",
           (unsigned int )state->demod);
  } else
  if (verbose > 3U) {
    printk("Wake %s(%d) from standby", (unsigned int )state->device == 1U ? (char *)"STV0900" : (char *)"STV0903",
           (unsigned int )state->demod);
  } else {
  }
  ldv_mutex_lock_23(& (state->internal)->demod_lock);
  tmp = stv090x_read_reg(state, 61878U);
  reg = (u32 )tmp;
  reg = reg & 4294967167U;
  tmp___0 = stv090x_write_reg(state, 61878U, (int )((u8 )reg));
  if (tmp___0 < 0) {
    goto err;
  } else {
  }
  switch ((unsigned int )state->demod) {
  case 1U:
  tmp___1 = stv090x_read_reg(state, 61920U);
  reg = (u32 )tmp___1;
  reg = reg | 2U;
  tmp___2 = stv090x_write_reg(state, 61920U, (int )((u8 )reg));
  if (tmp___2 < 0) {
    goto err;
  } else {
  }
  tmp___3 = stv090x_read_reg(state, 61921U);
  reg = (u32 )tmp___3;
  reg = reg | 32U;
  tmp___4 = stv090x_write_reg(state, 61921U, (int )((u8 )reg));
  if (tmp___4 < 0) {
    goto err;
  } else {
  }
  tmp___5 = stv090x_read_reg(state, 61890U);
  reg = (u32 )tmp___5;
  reg = reg & 4294967263U;
  reg = reg & 4294967293U;
  reg = reg & 4294967279U;
  tmp___6 = stv090x_write_reg(state, 61890U, (int )((u8 )reg));
  if (tmp___6 < 0) {
    goto err;
  } else {
  }
  tmp___7 = stv090x_read_reg(state, 61891U);
  reg = (u32 )tmp___7;
  reg = reg & 4294967287U;
  reg = reg & 4294967293U;
  reg = reg & 4294967294U;
  tmp___8 = stv090x_write_reg(state, 61891U, (int )((u8 )reg));
  if (tmp___8 < 0) {
    goto err;
  } else {
  }
  goto ldv_24722;
  case 2U:
  tmp___9 = stv090x_read_reg(state, 61922U);
  reg = (u32 )tmp___9;
  reg = reg | 2U;
  tmp___10 = stv090x_write_reg(state, 61922U, (int )((u8 )reg));
  if (tmp___10 < 0) {
    goto err;
  } else {
  }
  tmp___11 = stv090x_read_reg(state, 61923U);
  reg = (u32 )tmp___11;
  reg = reg | 32U;
  tmp___12 = stv090x_write_reg(state, 61923U, (int )((u8 )reg));
  if (tmp___12 < 0) {
    goto err;
  } else {
  }
  tmp___13 = stv090x_read_reg(state, 61890U);
  reg = (u32 )tmp___13;
  reg = reg & 4294967231U;
  reg = reg & 4294967287U;
  reg = reg & 4294967279U;
  tmp___14 = stv090x_write_reg(state, 61890U, (int )((u8 )reg));
  if (tmp___14 < 0) {
    goto err;
  } else {
  }
  tmp___15 = stv090x_read_reg(state, 61891U);
  reg = (u32 )tmp___15;
  reg = reg & 4294967279U;
  reg = reg & 4294967291U;
  reg = reg & 4294967294U;
  tmp___16 = stv090x_write_reg(state, 61891U, (int )((u8 )reg));
  if (tmp___16 < 0) {
    goto err;
  } else {
  }
  goto ldv_24722;
  default: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: Wrong demodulator!\n", "stv090x_wakeup");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: Wrong demodulator!\n", "stv090x_wakeup");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: Wrong demodulator!\n", "stv090x_wakeup");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: Wrong demodulator!\n", "stv090x_wakeup");
  } else
  if (verbose != 0U) {
    printk("Wrong demodulator!");
  } else {
  }
  goto ldv_24722;
  }
  ldv_24722:
  ldv_mutex_unlock_24(& (state->internal)->demod_lock);
  return (0);
  err:
  ldv_mutex_unlock_25(& (state->internal)->demod_lock);
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_wakeup");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_wakeup");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_wakeup");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_wakeup");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static void stv090x_release(struct dvb_frontend *fe )
{
  struct stv090x_state *state ;
  {
  state = (struct stv090x_state *)fe->demodulator_priv;
  (state->internal)->num_used = (state->internal)->num_used - 1;
  if ((state->internal)->num_used <= 0) {
    if (verbose != 0U && verbose != 0U) {
      printk("\v%s: Actually removing\n", "stv090x_release");
    } else
    if (verbose > 1U && verbose != 0U) {
      printk("\r%s: Actually removing\n", "stv090x_release");
    } else
    if (verbose > 2U && verbose != 0U) {
      printk("\016%s: Actually removing\n", "stv090x_release");
    } else
    if (verbose > 3U && verbose != 0U) {
      printk("\017%s: Actually removing\n", "stv090x_release");
    } else
    if (verbose != 0U) {
      printk("Actually removing");
    } else {
    }
    remove_dev(state->internal);
    kfree((void const *)state->internal);
  } else {
  }
  kfree((void const *)state);
  return;
}
}
static int stv090x_ldpc_mode(struct stv090x_state *state , enum stv090x_mode ldpc_mode )
{
  u32 reg ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  int tmp___23 ;
  int tmp___24 ;
  int tmp___25 ;
  int tmp___26 ;
  int tmp___27 ;
  int tmp___28 ;
  int tmp___29 ;
  int tmp___30 ;
  int tmp___31 ;
  int tmp___32 ;
  int tmp___33 ;
  int tmp___34 ;
  int tmp___35 ;
  int tmp___36 ;
  int tmp___37 ;
  int tmp___38 ;
  int tmp___39 ;
  int tmp___40 ;
  int tmp___41 ;
  int tmp___42 ;
  int tmp___43 ;
  int tmp___44 ;
  int tmp___45 ;
  int tmp___46 ;
  int tmp___47 ;
  int tmp___48 ;
  int tmp___49 ;
  int tmp___50 ;
  int tmp___51 ;
  int tmp___52 ;
  int tmp___53 ;
  int tmp___54 ;
  int tmp___55 ;
  int tmp___56 ;
  int tmp___57 ;
  int tmp___58 ;
  int tmp___59 ;
  int tmp___60 ;
  int tmp___61 ;
  int tmp___62 ;
  int tmp___63 ;
  int tmp___64 ;
  int tmp___65 ;
  int tmp___66 ;
  int tmp___67 ;
  {
  reg = 0U;
  tmp = stv090x_read_reg(state, 64134U);
  reg = (u32 )tmp;
  switch ((unsigned int )ldpc_mode) {
  case 0U: ;
  default: ;
  if ((unsigned int )state->demod_mode != 0U || (reg & 1U) == 0U) {
    tmp___0 = stv090x_write_reg(state, 64134U, 29);
    if (tmp___0 < 0) {
      goto err;
    } else {
    }
    state->demod_mode = STV090x_DUAL;
    tmp___1 = stv090x_read_reg(state, 65297U);
    reg = (u32 )tmp___1;
    reg = reg | 128U;
    tmp___2 = stv090x_write_reg(state, 65297U, (int )((u8 )reg));
    if (tmp___2 < 0) {
      goto err;
    } else {
    }
    reg = reg & 4294967167U;
    tmp___3 = stv090x_write_reg(state, 65297U, (int )((u8 )reg));
    if (tmp___3 < 0) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___4 = stv090x_write_reg(state, 62128U, 255);
      tmp___6 = tmp___4 < 0;
    } else {
      tmp___5 = stv090x_write_reg(state, 62640U, 255);
      tmp___6 = tmp___5 < 0;
    }
    if (tmp___6) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___7 = stv090x_write_reg(state, 62129U, 255);
      tmp___9 = tmp___7 < 0;
    } else {
      tmp___8 = stv090x_write_reg(state, 62641U, 255);
      tmp___9 = tmp___8 < 0;
    }
    if (tmp___9) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___10 = stv090x_write_reg(state, 62130U, 255);
      tmp___12 = tmp___10 < 0;
    } else {
      tmp___11 = stv090x_write_reg(state, 62642U, 255);
      tmp___12 = tmp___11 < 0;
    }
    if (tmp___12) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___13 = stv090x_write_reg(state, 62131U, 255);
      tmp___15 = tmp___13 < 0;
    } else {
      tmp___14 = stv090x_write_reg(state, 62643U, 255);
      tmp___15 = tmp___14 < 0;
    }
    if (tmp___15) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___16 = stv090x_write_reg(state, 62132U, 255);
      tmp___18 = tmp___16 < 0;
    } else {
      tmp___17 = stv090x_write_reg(state, 62644U, 255);
      tmp___18 = tmp___17 < 0;
    }
    if (tmp___18) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___19 = stv090x_write_reg(state, 62133U, 255);
      tmp___21 = tmp___19 < 0;
    } else {
      tmp___20 = stv090x_write_reg(state, 62645U, 255);
      tmp___21 = tmp___20 < 0;
    }
    if (tmp___21) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___22 = stv090x_write_reg(state, 62134U, 255);
      tmp___24 = tmp___22 < 0;
    } else {
      tmp___23 = stv090x_write_reg(state, 62646U, 255);
      tmp___24 = tmp___23 < 0;
    }
    if (tmp___24) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___25 = stv090x_write_reg(state, 62135U, 204);
      tmp___27 = tmp___25 < 0;
    } else {
      tmp___26 = stv090x_write_reg(state, 62647U, 204);
      tmp___27 = tmp___26 < 0;
    }
    if (tmp___27) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___28 = stv090x_write_reg(state, 62136U, 204);
      tmp___30 = tmp___28 < 0;
    } else {
      tmp___29 = stv090x_write_reg(state, 62648U, 204);
      tmp___30 = tmp___29 < 0;
    }
    if (tmp___30) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___31 = stv090x_write_reg(state, 62137U, 204);
      tmp___33 = tmp___31 < 0;
    } else {
      tmp___32 = stv090x_write_reg(state, 62649U, 204);
      tmp___33 = tmp___32 < 0;
    }
    if (tmp___33) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___34 = stv090x_write_reg(state, 62138U, 204);
      tmp___36 = tmp___34 < 0;
    } else {
      tmp___35 = stv090x_write_reg(state, 62650U, 204);
      tmp___36 = tmp___35 < 0;
    }
    if (tmp___36) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___37 = stv090x_write_reg(state, 62139U, 204);
      tmp___39 = tmp___37 < 0;
    } else {
      tmp___38 = stv090x_write_reg(state, 62651U, 204);
      tmp___39 = tmp___38 < 0;
    }
    if (tmp___39) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___40 = stv090x_write_reg(state, 62140U, 204);
      tmp___42 = tmp___40 < 0;
    } else {
      tmp___41 = stv090x_write_reg(state, 62652U, 204);
      tmp___42 = tmp___41 < 0;
    }
    if (tmp___42) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___43 = stv090x_write_reg(state, 62141U, 204);
      tmp___45 = tmp___43 < 0;
    } else {
      tmp___44 = stv090x_write_reg(state, 62653U, 204);
      tmp___45 = tmp___44 < 0;
    }
    if (tmp___45) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___46 = stv090x_write_reg(state, 62142U, 255);
      tmp___48 = tmp___46 < 0;
    } else {
      tmp___47 = stv090x_write_reg(state, 62654U, 255);
      tmp___48 = tmp___47 < 0;
    }
    if (tmp___48) {
      goto err;
    } else {
    }
    if ((unsigned int )state->demod == 2U) {
      tmp___49 = stv090x_write_reg(state, 62143U, 207);
      tmp___51 = tmp___49 < 0;
    } else {
      tmp___50 = stv090x_write_reg(state, 62655U, 207);
      tmp___51 = tmp___50 < 0;
    }
    if (tmp___51) {
      goto err;
    } else {
    }
  } else {
  }
  goto ldv_24738;
  case 1U:
  tmp___52 = stv090x_stop_modcod(state);
  if (tmp___52 < 0) {
    goto err;
  } else {
  }
  tmp___53 = stv090x_activate_modcod_single(state);
  if (tmp___53 < 0) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___55 = stv090x_write_reg(state, 64134U, 6);
    if (tmp___55 < 0) {
      goto err;
    } else {
      tmp___54 = stv090x_write_reg(state, 64134U, 4);
      if (tmp___54 < 0) {
        goto err;
      } else {
      }
    }
  } else {
  }
  tmp___56 = stv090x_read_reg(state, 65297U);
  reg = (u32 )tmp___56;
  reg = reg | 128U;
  tmp___57 = stv090x_write_reg(state, 65297U, (int )((u8 )reg));
  if (tmp___57 < 0) {
    goto err;
  } else {
  }
  reg = reg & 4294967167U;
  tmp___58 = stv090x_write_reg(state, 65297U, (int )((u8 )reg));
  if (tmp___58 < 0) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___59 = stv090x_read_reg(state, 62288U);
    tmp___61 = tmp___59;
  } else {
    tmp___60 = stv090x_read_reg(state, 62800U);
    tmp___61 = tmp___60;
  }
  reg = (u32 )tmp___61;
  reg = reg | 1U;
  if ((unsigned int )state->demod == 2U) {
    tmp___62 = stv090x_write_reg(state, 62288U, (int )((u8 )reg));
    tmp___64 = tmp___62 < 0;
  } else {
    tmp___63 = stv090x_write_reg(state, 62800U, (int )((u8 )reg));
    tmp___64 = tmp___63 < 0;
  }
  if (tmp___64) {
    goto err;
  } else {
  }
  reg = reg & 4294967294U;
  if ((unsigned int )state->demod == 2U) {
    tmp___65 = stv090x_write_reg(state, 62288U, (int )((u8 )reg));
    tmp___67 = tmp___65 < 0;
  } else {
    tmp___66 = stv090x_write_reg(state, 62800U, (int )((u8 )reg));
    tmp___67 = tmp___66 < 0;
  }
  if (tmp___67) {
    goto err;
  } else {
  }
  goto ldv_24738;
  }
  ldv_24738: ;
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_ldpc_mode");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_ldpc_mode");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_ldpc_mode");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_ldpc_mode");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static u32 stv090x_get_mclk(struct stv090x_state *state )
{
  struct stv090x_config const *config ;
  u32 div ;
  u32 reg ;
  u8 ratio ;
  int tmp ;
  int tmp___0 ;
  {
  config = state->config;
  tmp = stv090x_read_reg(state, 61875U);
  div = (u32 )tmp;
  tmp___0 = stv090x_read_reg(state, 61878U);
  reg = (u32 )tmp___0;
  ratio = (reg & 32U) != 0U ? 4U : 6U;
  return (((div + 1U) * (u32 )config->xtal) / (u32 )ratio);
}
}
static int stv090x_set_mclk(struct stv090x_state *state , u32 mclk , u32 clk )
{
  struct stv090x_config const *config ;
  u32 reg ;
  u32 div ;
  u32 clk_sel ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  u32 tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  {
  config = state->config;
  tmp = stv090x_read_reg(state, 61878U);
  reg = (u32 )tmp;
  clk_sel = (reg & 32U) != 0U ? 4U : 6U;
  div = (clk_sel * mclk) / (u32 )config->xtal - 1U;
  tmp___0 = stv090x_read_reg(state, 61875U);
  reg = (u32 )tmp___0;
  reg = (reg & 4294967040U) | div;
  tmp___1 = stv090x_write_reg(state, 61875U, (int )((u8 )reg));
  if (tmp___1 < 0) {
    goto err;
  } else {
  }
  tmp___2 = stv090x_get_mclk(state);
  (state->internal)->mclk = (s32 )tmp___2;
  div = (u32 )((state->internal)->mclk / 704000);
  if ((unsigned int )state->demod == 2U) {
    tmp___3 = stv090x_write_reg(state, 61849U, (int )((u8 )div));
    tmp___5 = tmp___3 < 0;
  } else {
    tmp___4 = stv090x_write_reg(state, 61865U, (int )((u8 )div));
    tmp___5 = tmp___4 < 0;
  }
  if (tmp___5) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___6 = stv090x_write_reg(state, 61850U, (int )((u8 )div));
    tmp___8 = tmp___6 < 0;
  } else {
    tmp___7 = stv090x_write_reg(state, 61866U, (int )((u8 )div));
    tmp___8 = tmp___7 < 0;
  }
  if (tmp___8) {
    goto err;
  } else {
  }
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_set_mclk");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_set_mclk");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_set_mclk");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_set_mclk");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_set_tspath(struct stv090x_state *state )
{
  u32 reg ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  int tmp___23 ;
  int tmp___24 ;
  int tmp___25 ;
  int tmp___26 ;
  int tmp___27 ;
  int tmp___28 ;
  int tmp___29 ;
  u32 speed ;
  int tmp___30 ;
  int tmp___31 ;
  int tmp___32 ;
  u32 speed___0 ;
  int tmp___33 ;
  int tmp___34 ;
  int tmp___35 ;
  int tmp___36 ;
  int tmp___37 ;
  int tmp___38 ;
  int tmp___39 ;
  int tmp___40 ;
  int tmp___41 ;
  {
  if ((state->internal)->dev_ver > 31U) {
    switch ((int )(state->config)->ts1_mode) {
    case 3: ;
    case 4: ;
    switch ((int )(state->config)->ts2_mode) {
    case 1: ;
    case 2: ;
    default:
    stv090x_write_reg(state, 63024U, 0);
    goto ldv_24768;
    case 3: ;
    case 4:
    tmp = stv090x_write_reg(state, 63024U, 6);
    if (tmp < 0) {
      goto err;
    } else {
    }
    tmp___0 = stv090x_read_reg(state, 62835U);
    reg = (u32 )tmp___0;
    reg = reg | 192U;
    tmp___1 = stv090x_write_reg(state, 62835U, (int )((u8 )reg));
    if (tmp___1 < 0) {
      goto err;
    } else {
    }
    tmp___2 = stv090x_read_reg(state, 62323U);
    reg = (u32 )tmp___2;
    reg = reg | 192U;
    tmp___3 = stv090x_write_reg(state, 62323U, (int )((u8 )reg));
    if (tmp___3 < 0) {
      goto err;
    } else {
    }
    tmp___4 = stv090x_write_reg(state, 62848U, 20);
    if (tmp___4 < 0) {
      goto err;
    } else {
    }
    tmp___5 = stv090x_write_reg(state, 62336U, 40);
    if (tmp___5 < 0) {
      goto err;
    } else {
    }
    goto ldv_24768;
    }
    ldv_24768: ;
    goto ldv_24772;
    case 1: ;
    case 2: ;
    default: ;
    switch ((int )(state->config)->ts2_mode) {
    case 1: ;
    case 2: ;
    default:
    tmp___6 = stv090x_write_reg(state, 63024U, 12);
    if (tmp___6 < 0) {
      goto err;
    } else {
    }
    goto ldv_24779;
    case 3: ;
    case 4:
    tmp___7 = stv090x_write_reg(state, 63024U, 10);
    if (tmp___7 < 0) {
      goto err;
    } else {
    }
    goto ldv_24779;
    }
    ldv_24779: ;
    goto ldv_24772;
    }
    ldv_24772: ;
  } else {
    switch ((int )(state->config)->ts1_mode) {
    case 3: ;
    case 4: ;
    switch ((int )(state->config)->ts2_mode) {
    case 1: ;
    case 2: ;
    default:
    stv090x_write_reg(state, 63088U, 16);
    goto ldv_24787;
    case 3: ;
    case 4:
    stv090x_write_reg(state, 63088U, 22);
    tmp___8 = stv090x_read_reg(state, 62835U);
    reg = (u32 )tmp___8;
    reg = reg | 192U;
    tmp___9 = stv090x_write_reg(state, 62835U, (int )((u8 )reg));
    if (tmp___9 < 0) {
      goto err;
    } else {
    }
    tmp___10 = stv090x_read_reg(state, 62835U);
    reg = (u32 )tmp___10;
    reg = reg & 4294967103U;
    tmp___11 = stv090x_write_reg(state, 62835U, (int )((u8 )reg));
    if (tmp___11 < 0) {
      goto err;
    } else {
    }
    tmp___12 = stv090x_write_reg(state, 62848U, 20);
    if (tmp___12 < 0) {
      goto err;
    } else {
    }
    tmp___13 = stv090x_write_reg(state, 62336U, 40);
    if (tmp___13 < 0) {
      goto err;
    } else {
    }
    goto ldv_24787;
    }
    ldv_24787: ;
    goto ldv_24790;
    case 1: ;
    case 2: ;
    default: ;
    switch ((int )(state->config)->ts2_mode) {
    case 1: ;
    case 2: ;
    default:
    stv090x_write_reg(state, 63088U, 20);
    goto ldv_24797;
    case 3: ;
    case 4:
    stv090x_write_reg(state, 63088U, 18);
    goto ldv_24797;
    }
    ldv_24797: ;
    goto ldv_24790;
    }
    ldv_24790: ;
  }
  switch ((int )(state->config)->ts1_mode) {
  case 3:
  tmp___14 = stv090x_read_reg(state, 62834U);
  reg = (u32 )tmp___14;
  reg = (reg & 4294967263U) | (u32 )((int )(state->config)->ts1_tei << 5);
  reg = reg & 4294967231U;
  reg = reg & 4294967167U;
  tmp___15 = stv090x_write_reg(state, 62834U, (int )((u8 )reg));
  if (tmp___15 < 0) {
    goto err;
  } else {
  }
  goto ldv_24801;
  case 4:
  tmp___16 = stv090x_read_reg(state, 62834U);
  reg = (u32 )tmp___16;
  reg = (reg & 4294967263U) | (u32 )((int )(state->config)->ts1_tei << 5);
  reg = reg & 4294967231U;
  reg = reg | 128U;
  tmp___17 = stv090x_write_reg(state, 62834U, (int )((u8 )reg));
  if (tmp___17 < 0) {
    goto err;
  } else {
  }
  goto ldv_24801;
  case 1:
  tmp___18 = stv090x_read_reg(state, 62834U);
  reg = (u32 )tmp___18;
  reg = (reg & 4294967263U) | (u32 )((int )(state->config)->ts1_tei << 5);
  reg = reg | 64U;
  reg = reg & 4294967167U;
  tmp___19 = stv090x_write_reg(state, 62834U, (int )((u8 )reg));
  if (tmp___19 < 0) {
    goto err;
  } else {
  }
  goto ldv_24801;
  case 2:
  tmp___20 = stv090x_read_reg(state, 62834U);
  reg = (u32 )tmp___20;
  reg = (reg & 4294967263U) | (u32 )((int )(state->config)->ts1_tei << 5);
  reg = reg | 64U;
  reg = reg | 128U;
  tmp___21 = stv090x_write_reg(state, 62834U, (int )((u8 )reg));
  if (tmp___21 < 0) {
    goto err;
  } else {
  }
  goto ldv_24801;
  default: ;
  goto ldv_24801;
  }
  ldv_24801: ;
  switch ((int )(state->config)->ts2_mode) {
  case 3:
  tmp___22 = stv090x_read_reg(state, 62322U);
  reg = (u32 )tmp___22;
  reg = (reg & 4294967263U) | (u32 )((int )(state->config)->ts2_tei << 5);
  reg = reg & 4294967231U;
  reg = reg & 4294967167U;
  tmp___23 = stv090x_write_reg(state, 62322U, (int )((u8 )reg));
  if (tmp___23 < 0) {
    goto err;
  } else {
  }
  goto ldv_24807;
  case 4:
  tmp___24 = stv090x_read_reg(state, 62322U);
  reg = (u32 )tmp___24;
  reg = (reg & 4294967263U) | (u32 )((int )(state->config)->ts2_tei << 5);
  reg = reg & 4294967231U;
  reg = reg | 128U;
  tmp___25 = stv090x_write_reg(state, 62322U, (int )((u8 )reg));
  if (tmp___25 < 0) {
    goto err;
  } else {
  }
  goto ldv_24807;
  case 1:
  tmp___26 = stv090x_read_reg(state, 62322U);
  reg = (u32 )tmp___26;
  reg = (reg & 4294967263U) | (u32 )((int )(state->config)->ts2_tei << 5);
  reg = reg | 64U;
  reg = reg & 4294967167U;
  tmp___27 = stv090x_write_reg(state, 62322U, (int )((u8 )reg));
  if (tmp___27 < 0) {
    goto err;
  } else {
  }
  goto ldv_24807;
  case 2:
  tmp___28 = stv090x_read_reg(state, 62322U);
  reg = (u32 )tmp___28;
  reg = (reg & 4294967263U) | (u32 )((int )(state->config)->ts2_tei << 5);
  reg = reg | 64U;
  reg = reg | 128U;
  tmp___29 = stv090x_write_reg(state, 62322U, (int )((u8 )reg));
  if (tmp___29 < 0) {
    goto err;
  } else {
  }
  goto ldv_24807;
  default: ;
  goto ldv_24807;
  }
  ldv_24807: ;
  if ((unsigned int )(state->config)->ts1_clk != 0U) {
    switch ((int )(state->config)->ts1_mode) {
    case 3: ;
    case 4: ;
    default:
    speed = (unsigned int )(state->internal)->mclk / ((unsigned int )(state->config)->ts1_clk / 4U);
    if (speed <= 7U) {
      speed = 8U;
    } else {
    }
    if (speed > 255U) {
      speed = 255U;
    } else {
    }
    goto ldv_24816;
    case 1: ;
    case 2:
    speed = (unsigned int )(state->internal)->mclk / ((unsigned int )(state->config)->ts1_clk / 32U);
    if (speed <= 31U) {
      speed = 32U;
    } else {
    }
    if (speed > 255U) {
      speed = 255U;
    } else {
    }
    goto ldv_24816;
    }
    ldv_24816:
    tmp___30 = stv090x_read_reg(state, 62835U);
    reg = (u32 )tmp___30;
    reg = reg | 192U;
    tmp___31 = stv090x_write_reg(state, 62835U, (int )((u8 )reg));
    if (tmp___31 < 0) {
      goto err;
    } else {
    }
    tmp___32 = stv090x_write_reg(state, 62848U, (int )((u8 )speed));
    if (tmp___32 < 0) {
      goto err;
    } else {
    }
  } else {
  }
  if ((unsigned int )(state->config)->ts2_clk != 0U) {
    switch ((int )(state->config)->ts2_mode) {
    case 3: ;
    case 4: ;
    default:
    speed___0 = (unsigned int )(state->internal)->mclk / ((unsigned int )(state->config)->ts2_clk / 4U);
    if (speed___0 <= 7U) {
      speed___0 = 8U;
    } else {
    }
    if (speed___0 > 255U) {
      speed___0 = 255U;
    } else {
    }
    goto ldv_24823;
    case 1: ;
    case 2:
    speed___0 = (unsigned int )(state->internal)->mclk / ((unsigned int )(state->config)->ts2_clk / 32U);
    if (speed___0 <= 31U) {
      speed___0 = 32U;
    } else {
    }
    if (speed___0 > 255U) {
      speed___0 = 255U;
    } else {
    }
    goto ldv_24823;
    }
    ldv_24823:
    tmp___33 = stv090x_read_reg(state, 62323U);
    reg = (u32 )tmp___33;
    reg = reg | 192U;
    tmp___34 = stv090x_write_reg(state, 62323U, (int )((u8 )reg));
    if (tmp___34 < 0) {
      goto err;
    } else {
    }
    tmp___35 = stv090x_write_reg(state, 62336U, (int )((u8 )speed___0));
    if (tmp___35 < 0) {
      goto err;
    } else {
    }
  } else {
  }
  tmp___36 = stv090x_read_reg(state, 62322U);
  reg = (u32 )tmp___36;
  reg = reg | 1U;
  tmp___37 = stv090x_write_reg(state, 62322U, (int )((u8 )reg));
  if (tmp___37 < 0) {
    goto err;
  } else {
  }
  reg = reg & 4294967294U;
  tmp___38 = stv090x_write_reg(state, 62322U, (int )((u8 )reg));
  if (tmp___38 < 0) {
    goto err;
  } else {
  }
  tmp___39 = stv090x_read_reg(state, 62834U);
  reg = (u32 )tmp___39;
  reg = reg | 1U;
  tmp___40 = stv090x_write_reg(state, 62834U, (int )((u8 )reg));
  if (tmp___40 < 0) {
    goto err;
  } else {
  }
  reg = reg & 4294967294U;
  tmp___41 = stv090x_write_reg(state, 62834U, (int )((u8 )reg));
  if (tmp___41 < 0) {
    goto err;
  } else {
  }
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_set_tspath");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_set_tspath");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_set_tspath");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_set_tspath");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_init(struct dvb_frontend *fe )
{
  struct stv090x_state *state ;
  struct stv090x_config const *config ;
  u32 reg ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  int tmp___19 ;
  int tmp___20 ;
  int tmp___21 ;
  {
  state = (struct stv090x_state *)fe->demodulator_priv;
  config = state->config;
  if ((state->internal)->mclk == 0) {
    tmp = stv090x_i2c_gate_ctrl(state, 1);
    if (tmp < 0) {
      goto err;
    } else {
    }
    if ((unsigned long )config->tuner_init != (unsigned long )((int (* )(struct dvb_frontend * ))0)) {
      tmp___0 = (*(config->tuner_init))(fe);
      if (tmp___0 < 0) {
        goto err_gateoff;
      } else {
      }
    } else {
    }
    tmp___1 = stv090x_i2c_gate_ctrl(state, 0);
    if (tmp___1 < 0) {
      goto err;
    } else {
    }
    stv090x_set_mclk(state, 135000000U, config->xtal);
    msleep(5U);
    tmp___2 = stv090x_write_reg(state, 61878U, (int )((unsigned int )((u8 )config->clk_mode) | 32U));
    if (tmp___2 < 0) {
      goto err;
    } else {
    }
    stv090x_get_mclk(state);
  } else {
  }
  tmp___3 = stv090x_wakeup(fe);
  if (tmp___3 < 0) {
    if (verbose != 0U && verbose != 0U) {
      printk("\v%s: Error waking device\n", "stv090x_init");
    } else
    if (verbose > 1U && verbose != 0U) {
      printk("\r%s: Error waking device\n", "stv090x_init");
    } else
    if (verbose > 2U && verbose != 0U) {
      printk("\016%s: Error waking device\n", "stv090x_init");
    } else
    if (verbose > 3U && verbose != 0U) {
      printk("\017%s: Error waking device\n", "stv090x_init");
    } else
    if (verbose != 0U) {
      printk("Error waking device");
    } else {
    }
    goto err;
  } else {
  }
  tmp___4 = stv090x_ldpc_mode(state, state->demod_mode);
  if (tmp___4 < 0) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___5 = stv090x_read_reg(state, 62177U);
    tmp___7 = tmp___5;
  } else {
    tmp___6 = stv090x_read_reg(state, 62689U);
    tmp___7 = tmp___6;
  }
  reg = (u32 )tmp___7;
  reg = (reg & 4294967167U) | ((unsigned int )state->inversion << 7);
  if ((unsigned int )state->demod == 2U) {
    tmp___8 = stv090x_write_reg(state, 62177U, (int )((u8 )reg));
    tmp___10 = tmp___8 < 0;
  } else {
    tmp___9 = stv090x_write_reg(state, 62689U, (int )((u8 )reg));
    tmp___10 = tmp___9 < 0;
  }
  if (tmp___10) {
    goto err;
  } else {
  }
  if ((unsigned int )state->demod == 2U) {
    tmp___11 = stv090x_read_reg(state, 61968U);
    tmp___13 = tmp___11;
  } else {
    tmp___12 = stv090x_read_reg(state, 62480U);
    tmp___13 = tmp___12;
  }
  reg = (u32 )tmp___13;
  reg = (reg & 4294967292U) | (u32 )state->rolloff;
  if ((unsigned int )state->demod == 2U) {
    tmp___14 = stv090x_write_reg(state, 61968U, (int )((u8 )reg));
    tmp___16 = tmp___14 < 0;
  } else {
    tmp___15 = stv090x_write_reg(state, 62480U, (int )((u8 )reg));
    tmp___16 = tmp___15 < 0;
  }
  if (tmp___16) {
    goto err;
  } else {
  }
  tmp___17 = stv090x_i2c_gate_ctrl(state, 1);
  if (tmp___17 < 0) {
    goto err;
  } else {
  }
  if ((unsigned long )config->tuner_set_mode != (unsigned long )((int (* )(struct dvb_frontend * ,
                                                                                      enum tuner_mode ))0)) {
    tmp___18 = (*(config->tuner_set_mode))(fe, TUNER_WAKE);
    if (tmp___18 < 0) {
      goto err_gateoff;
    } else {
    }
  } else {
  }
  if ((unsigned long )config->tuner_init != (unsigned long )((int (* )(struct dvb_frontend * ))0)) {
    tmp___19 = (*(config->tuner_init))(fe);
    if (tmp___19 < 0) {
      goto err_gateoff;
    } else {
    }
  } else {
  }
  tmp___20 = stv090x_i2c_gate_ctrl(state, 0);
  if (tmp___20 < 0) {
    goto err;
  } else {
  }
  tmp___21 = stv090x_set_tspath(state);
  if (tmp___21 < 0) {
    goto err;
  } else {
  }
  return (0);
  err_gateoff:
  stv090x_i2c_gate_ctrl(state, 0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_init");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_init");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_init");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_init");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
static int stv090x_setup(struct dvb_frontend *fe )
{
  struct stv090x_state *state ;
  struct stv090x_config const *config ;
  struct stv090x_reg const *stv090x_initval ;
  struct stv090x_reg const *stv090x_cut20_val ;
  unsigned long t1_size ;
  unsigned long t2_size ;
  u32 reg ;
  int i ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  {
  state = (struct stv090x_state *)fe->demodulator_priv;
  config = state->config;
  stv090x_initval = 0;
  stv090x_cut20_val = 0;
  t1_size = 0UL;
  t2_size = 0UL;
  reg = 0U;
  if ((unsigned int )state->device == 1U) {
    if (verbose != 0U && verbose > 3U) {
      printk("\v%s: Initializing STV0900\n", "stv090x_setup");
    } else
    if (verbose > 1U && verbose > 3U) {
      printk("\r%s: Initializing STV0900\n", "stv090x_setup");
    } else
    if (verbose > 2U && verbose > 3U) {
      printk("\016%s: Initializing STV0900\n", "stv090x_setup");
    } else
    if (verbose > 3U && verbose > 3U) {
      printk("\017%s: Initializing STV0900\n", "stv090x_setup");
    } else
    if (verbose > 3U) {
      printk("Initializing STV0900");
    } else {
    }
    stv090x_initval = (struct stv090x_reg const *)(& stv0900_initval);
    t1_size = 161UL;
    stv090x_cut20_val = (struct stv090x_reg const *)(& stv0900_cut20_val);
    t2_size = 32UL;
  } else
  if ((unsigned int )state->device == 0U) {
    if (verbose != 0U && verbose > 3U) {
      printk("\v%s: Initializing STV0903\n", "stv090x_setup");
    } else
    if (verbose > 1U && verbose > 3U) {
      printk("\r%s: Initializing STV0903\n", "stv090x_setup");
    } else
    if (verbose > 2U && verbose > 3U) {
      printk("\016%s: Initializing STV0903\n", "stv090x_setup");
    } else
    if (verbose > 3U && verbose > 3U) {
      printk("\017%s: Initializing STV0903\n", "stv090x_setup");
    } else
    if (verbose > 3U) {
      printk("Initializing STV0903");
    } else {
    }
    stv090x_initval = (struct stv090x_reg const *)(& stv0903_initval);
    t1_size = 99UL;
    stv090x_cut20_val = (struct stv090x_reg const *)(& stv0903_cut20_val);
    t2_size = 23UL;
  } else {
  }
  tmp = stv090x_write_reg(state, 62486U, 92);
  if (tmp < 0) {
    goto err;
  } else {
  }
  tmp___0 = stv090x_write_reg(state, 61974U, 92);
  if (tmp___0 < 0) {
    goto err;
  } else {
  }
  msleep(5U);
  tmp___1 = stv090x_write_reg(state, 62688U, 108);
  if (tmp___1 < 0) {
    goto err;
  } else {
  }
  tmp___2 = stv090x_write_reg(state, 62176U, 108);
  if (tmp___2 < 0) {
    goto err;
  } else {
  }
  reg = (reg & 4294967183U) | ((unsigned int )config->repeater_level << 4);
  tmp___3 = stv090x_write_reg(state, 61738U, (int )((u8 )reg));
  if (tmp___3 < 0) {
    goto err;
  } else {
  }
  tmp___4 = stv090x_write_reg(state, 61739U, (int )((u8 )reg));
  if (tmp___4 < 0) {
    goto err;
  } else {
  }
  tmp___5 = stv090x_write_reg(state, 61875U, 19);
  if (tmp___5 < 0) {
    goto err;
  } else {
  }
  msleep(5U);
  tmp___6 = stv090x_write_reg(state, 61737U, 8);
  if (tmp___6 < 0) {
    goto err;
  } else {
  }
  tmp___7 = stv090x_write_reg(state, 61878U, (int )((unsigned int )((u8 )config->clk_mode) | 32U));
  if (tmp___7 < 0) {
    goto err;
  } else {
  }
  msleep(5U);
  if (verbose != 0U && verbose > 3U) {
    printk("\v%s: Setting up initial values\n", "stv090x_setup");
  } else
  if (verbose > 1U && verbose > 3U) {
    printk("\r%s: Setting up initial values\n", "stv090x_setup");
  } else
  if (verbose > 2U && verbose > 3U) {
    printk("\016%s: Setting up initial values\n", "stv090x_setup");
  } else
  if (verbose > 3U && verbose > 3U) {
    printk("\017%s: Setting up initial values\n", "stv090x_setup");
  } else
  if (verbose > 3U) {
    printk("Setting up initial values");
  } else {
  }
  i = 0;
  goto ldv_24858;
  ldv_24857:
  tmp___8 = stv090x_write_reg(state, (unsigned int )(stv090x_initval + (unsigned long )i)->addr,
                              (int )(stv090x_initval + (unsigned long )i)->data);
  if (tmp___8 < 0) {
    goto err;
  } else {
  }
  i = i + 1;
  ldv_24858: ;
  if ((unsigned long )i < t1_size) {
    goto ldv_24857;
  } else {
  }
  tmp___9 = stv090x_read_reg(state, 61696U);
  (state->internal)->dev_ver = (u32 )tmp___9;
  if ((state->internal)->dev_ver > 31U) {
    tmp___10 = stv090x_write_reg(state, 63024U, 12);
    if (tmp___10 < 0) {
      goto err;
    } else {
    }
    if (verbose != 0U && verbose > 3U) {
      printk("\v%s: Setting up Cut 2.0 initial values\n", "stv090x_setup");
    } else
    if (verbose > 1U && verbose > 3U) {
      printk("\r%s: Setting up Cut 2.0 initial values\n", "stv090x_setup");
    } else
    if (verbose > 2U && verbose > 3U) {
      printk("\016%s: Setting up Cut 2.0 initial values\n", "stv090x_setup");
    } else
    if (verbose > 3U && verbose > 3U) {
      printk("\017%s: Setting up Cut 2.0 initial values\n", "stv090x_setup");
    } else
    if (verbose > 3U) {
      printk("Setting up Cut 2.0 initial values");
    } else {
    }
    i = 0;
    goto ldv_24861;
    ldv_24860:
    tmp___11 = stv090x_write_reg(state, (unsigned int )(stv090x_cut20_val + (unsigned long )i)->addr,
                                 (int )(stv090x_cut20_val + (unsigned long )i)->data);
    if (tmp___11 < 0) {
      goto err;
    } else {
    }
    i = i + 1;
    ldv_24861: ;
    if ((unsigned long )i < t2_size) {
      goto ldv_24860;
    } else {
    }
  } else
  if ((state->internal)->dev_ver <= 31U) {
    if (verbose != 0U && verbose != 0U) {
      printk("\v%s: NON_ERROR: Unsupported Cut: 0x%02x!\n", "stv090x_setup", (state->internal)->dev_ver);
    } else
    if (verbose > 1U && verbose != 0U) {
      printk("\r%s: NON_ERROR: Unsupported Cut: 0x%02x!\n", "stv090x_setup", (state->internal)->dev_ver);
    } else
    if (verbose > 2U && verbose != 0U) {
      printk("\016%s: NON_ERROR: Unsupported Cut: 0x%02x!\n", "stv090x_setup", (state->internal)->dev_ver);
    } else
    if (verbose > 3U && verbose != 0U) {
      printk("\017%s: NON_ERROR: Unsupported Cut: 0x%02x!\n", "stv090x_setup", (state->internal)->dev_ver);
    } else
    if (verbose != 0U) {
      printk("NON_ERROR: Unsupported Cut: 0x%02x!", (state->internal)->dev_ver);
    } else {
    }
    goto err;
  } else
  if ((state->internal)->dev_ver > 48U) {
    if (verbose != 0U && verbose != 0U) {
      printk("\v%s: INFO: Cut: 0x%02x probably incomplete support!\n", "stv090x_setup",
             (state->internal)->dev_ver);
    } else
    if (verbose > 1U && verbose != 0U) {
      printk("\r%s: INFO: Cut: 0x%02x probably incomplete support!\n", "stv090x_setup",
             (state->internal)->dev_ver);
    } else
    if (verbose > 2U && verbose != 0U) {
      printk("\016%s: INFO: Cut: 0x%02x probably incomplete support!\n", "stv090x_setup",
             (state->internal)->dev_ver);
    } else
    if (verbose > 3U && verbose != 0U) {
      printk("\017%s: INFO: Cut: 0x%02x probably incomplete support!\n", "stv090x_setup",
             (state->internal)->dev_ver);
    } else
    if (verbose != 0U) {
      printk("INFO: Cut: 0x%02x probably incomplete support!", (state->internal)->dev_ver);
    } else {
    }
  } else {
  }
  tmp___12 = stv090x_read_reg(state, 61920U);
  reg = (u32 )tmp___12;
  reg = (reg & 4294967294U) | (unsigned int )((unsigned int )config->adc1_range != 1U);
  tmp___13 = stv090x_write_reg(state, 61920U, (int )((u8 )reg));
  if (tmp___13 < 0) {
    goto err;
  } else {
  }
  tmp___14 = stv090x_read_reg(state, 61922U);
  reg = (u32 )tmp___14;
  reg = (reg & 4294967294U) | (unsigned int )((unsigned int )config->adc2_range != 1U);
  tmp___15 = stv090x_write_reg(state, 61922U, (int )((u8 )reg));
  if (tmp___15 < 0) {
    goto err;
  } else {
  }
  tmp___16 = stv090x_write_reg(state, 65297U, 128);
  if (tmp___16 < 0) {
    goto err;
  } else {
  }
  tmp___17 = stv090x_write_reg(state, 65297U, 0);
  if (tmp___17 < 0) {
    goto err;
  } else {
  }
  return (0);
  err: ;
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: I/O error\n", "stv090x_setup");
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: I/O error\n", "stv090x_setup");
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: I/O error\n", "stv090x_setup");
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: I/O error\n", "stv090x_setup");
  } else
  if (verbose != 0U) {
    printk("I/O error");
  } else {
  }
  return (-1);
}
}
int stv090x_set_gpio(struct dvb_frontend *fe , u8 gpio , u8 dir , u8 value , u8 xor_value )
{
  struct stv090x_state *state ;
  u8 reg ;
  int tmp ;
  {
  state = (struct stv090x_state *)fe->demodulator_priv;
  reg = 0U;
  reg = (u8 )(((int )((signed char )reg) & 127) | (int )((signed char )((int )dir << 7)));
  reg = (u8 )(((int )((signed char )reg) & -127) | (int )((signed char )((int )value << 1)));
  reg = (u8 )(((int )((signed char )reg) & -2) | (int )((signed char )xor_value));
  tmp = stv090x_write_reg(state, (unsigned int )((int )gpio + 61760), (int )reg);
  return (tmp);
}
}
static struct dvb_frontend_ops stv090x_ops =
     {{{'S', 'T', 'V', '0', '9', '0', 'x', ' ', 'M', 'u', 'l', 't', 'i', 's', 't', 'a',
      'n', 'd', 'a', 'r', 'd', '\000'}, 0, 950000U, 2150000U, 0U, 0U, 1000000U, 45000000U,
     0U, 0U, 268436993}, {5U, 6U, 4U}, & stv090x_release, 0, & stv090x_init, & stv090x_sleep,
    0, 0, & stv090x_frontend_algo, 0, 0, 0, & stv090x_read_status, & stv090x_read_per,
    & stv090x_read_signal_strength, & stv090x_read_cnr, 0, 0, & stv090x_send_diseqc_msg,
    & stv090x_recv_slave_reply, & stv090x_send_diseqc_burst, & stv090x_set_tone, 0,
    0, 0, 0, 0, 0, & stv090x_search, {{{(char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0, (char)0, (char)0,
                                        (char)0, (char)0, (char)0}, 0U, 0U, 0U, 0U,
                                       0U, 0U}, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                      0, 0, 0, 0, 0}, {{0}, 0, 0, 0, 0, 0, 0, 0, 0},
    0, 0};
struct dvb_frontend *stv090x_attach(struct stv090x_config const *config , struct i2c_adapter *i2c ,
                                    enum stv090x_demodulator demod )
{
  struct stv090x_state *state ;
  struct stv090x_dev *temp_int ;
  void *tmp ;
  void *tmp___0 ;
  struct lock_class_key __key ;
  struct lock_class_key __key___0 ;
  int tmp___1 ;
  {
  state = 0;
  tmp = kzalloc(1104UL, 208U);
  state = (struct stv090x_state *)tmp;
  if ((unsigned long )state == (unsigned long )((struct stv090x_state *)0)) {
    goto error;
  } else {
  }
  state->verbose = & verbose;
  state->config = config;
  state->i2c = i2c;
  state->frontend.ops = stv090x_ops;
  state->frontend.demodulator_priv = (void *)state;
  state->demod = demod;
  state->demod_mode = config->demod_mode;
  state->device = config->device;
  state->rolloff = STV090x_RO_35;
  temp_int = find_dev(state->i2c, (int )(state->config)->address);
  if ((unsigned long )temp_int != (unsigned long )((struct stv090x_dev *)0) && (unsigned int )state->demod_mode == 0U) {
    state->internal = temp_int->internal;
    (state->internal)->num_used = (state->internal)->num_used + 1;
    if (verbose != 0U && verbose > 2U) {
      printk("\v%s: Found Internal Structure!\n", "stv090x_attach");
    } else
    if (verbose > 1U && verbose > 2U) {
      printk("\r%s: Found Internal Structure!\n", "stv090x_attach");
    } else
    if (verbose > 2U && verbose > 2U) {
      printk("\016%s: Found Internal Structure!\n", "stv090x_attach");
    } else
    if (verbose > 3U && verbose > 2U) {
      printk("\017%s: Found Internal Structure!\n", "stv090x_attach");
    } else
    if (verbose > 2U) {
      printk("Found Internal Structure!");
    } else {
    }
  } else {
    tmp___0 = kmalloc(368UL, 208U);
    state->internal = (struct stv090x_internal *)tmp___0;
    if ((unsigned long )state->internal == (unsigned long )((struct stv090x_internal *)0)) {
      goto error;
    } else {
    }
    temp_int = append_internal(state->internal);
    if ((unsigned long )temp_int == (unsigned long )((struct stv090x_dev *)0)) {
      kfree((void const *)state->internal);
      goto error;
    } else {
    }
    (state->internal)->num_used = 1;
    (state->internal)->mclk = 0;
    (state->internal)->dev_ver = 0U;
    (state->internal)->i2c_adap = state->i2c;
    (state->internal)->i2c_addr = (state->config)->address;
    if (verbose != 0U && verbose > 2U) {
      printk("\v%s: Create New Internal Structure!\n", "stv090x_attach");
    } else
    if (verbose > 1U && verbose > 2U) {
      printk("\r%s: Create New Internal Structure!\n", "stv090x_attach");
    } else
    if (verbose > 2U && verbose > 2U) {
      printk("\016%s: Create New Internal Structure!\n", "stv090x_attach");
    } else
    if (verbose > 3U && verbose > 2U) {
      printk("\017%s: Create New Internal Structure!\n", "stv090x_attach");
    } else
    if (verbose > 2U) {
      printk("Create New Internal Structure!");
    } else {
    }
    __mutex_init(& (state->internal)->demod_lock, "&state->internal->demod_lock",
                 & __key);
    __mutex_init(& (state->internal)->tuner_lock, "&state->internal->tuner_lock",
                 & __key___0);
    tmp___1 = stv090x_setup(& state->frontend);
    if (tmp___1 < 0) {
      if (verbose != 0U && verbose != 0U) {
        printk("\v%s: Error setting up device\n", "stv090x_attach");
      } else
      if (verbose > 1U && verbose != 0U) {
        printk("\r%s: Error setting up device\n", "stv090x_attach");
      } else
      if (verbose > 2U && verbose != 0U) {
        printk("\016%s: Error setting up device\n", "stv090x_attach");
      } else
      if (verbose > 3U && verbose != 0U) {
        printk("\017%s: Error setting up device\n", "stv090x_attach");
      } else
      if (verbose != 0U) {
        printk("Error setting up device");
      } else {
      }
      goto err_remove;
    } else {
    }
  }
  if ((state->internal)->dev_ver > 47U) {
    state->frontend.ops.info.caps = (fe_caps_t )((unsigned int )state->frontend.ops.info.caps | 67108864U);
  } else {
  }
  if ((int )config->diseqc_envelope_mode) {
    stv090x_send_diseqc_burst(& state->frontend, SEC_MINI_A);
  } else {
  }
  if (verbose != 0U && verbose != 0U) {
    printk("\v%s: Attaching %s demodulator(%d) Cut=0x%02x\n", "stv090x_attach", (unsigned int )state->device == 1U ? (char *)"STV0900" : (char *)"STV0903",
           (unsigned int )demod, (state->internal)->dev_ver);
  } else
  if (verbose > 1U && verbose != 0U) {
    printk("\r%s: Attaching %s demodulator(%d) Cut=0x%02x\n", "stv090x_attach", (unsigned int )state->device == 1U ? (char *)"STV0900" : (char *)"STV0903",
           (unsigned int )demod, (state->internal)->dev_ver);
  } else
  if (verbose > 2U && verbose != 0U) {
    printk("\016%s: Attaching %s demodulator(%d) Cut=0x%02x\n", "stv090x_attach",
           (unsigned int )state->device == 1U ? (char *)"STV0900" : (char *)"STV0903",
           (unsigned int )demod, (state->internal)->dev_ver);
  } else
  if (verbose > 3U && verbose != 0U) {
    printk("\017%s: Attaching %s demodulator(%d) Cut=0x%02x\n", "stv090x_attach",
           (unsigned int )state->device == 1U ? (char *)"STV0900" : (char *)"STV0903",
           (unsigned int )demod, (state->internal)->dev_ver);
  } else
  if (verbose != 0U) {
    printk("Attaching %s demodulator(%d) Cut=0x%02x", (unsigned int )state->device == 1U ? (char *)"STV0900" : (char *)"STV0903",
           (unsigned int )demod, (state->internal)->dev_ver);
  } else {
  }
  return (& state->frontend);
  err_remove:
  remove_dev(state->internal);
  kfree((void const *)state->internal);
  error:
  kfree((void const *)state);
  return (0);
}
}
void ldv_check_final_state(void) ;
void ldv_initialize(void) ;
extern void ldv_handler_precall(void) ;
extern int __VERIFIER_nondet_int(void) ;
int LDV_IN_INTERRUPT ;
int main(void)
{
  struct dvb_frontend *var_group1 ;
  struct dvb_diseqc_master_cmd *var_group2 ;
  fe_sec_mini_cmd_t var_stv090x_send_diseqc_burst_57_p1 ;
  struct dvb_diseqc_slave_reply *var_group3 ;
  fe_sec_tone_mode_t var_stv090x_set_tone_54_p1 ;
  enum fe_status *var_stv090x_read_status_49_p1 ;
  u32 *var_stv090x_read_per_50_p1 ;
  u16 *var_stv090x_read_signal_strength_52_p1 ;
  u16 *var_stv090x_read_cnr_53_p1 ;
  int ldv_s_stv090x_ops_dvb_frontend_ops ;
  int tmp ;
  int tmp___0 ;
  {
  ldv_s_stv090x_ops_dvb_frontend_ops = 0;
  LDV_IN_INTERRUPT = 1;
  ldv_initialize();
  goto ldv_24949;
  ldv_24948:
  tmp = __VERIFIER_nondet_int();
  switch (tmp) {
  case 0: ;
  if (ldv_s_stv090x_ops_dvb_frontend_ops == 0) {
    ldv_handler_precall();
    stv090x_release(var_group1);
    ldv_s_stv090x_ops_dvb_frontend_ops = 0;
  } else {
  }
  goto ldv_24934;
  case 1:
  ldv_handler_precall();
  stv090x_init(var_group1);
  goto ldv_24934;
  case 2:
  ldv_handler_precall();
  stv090x_sleep(var_group1);
  goto ldv_24934;
  case 3:
  ldv_handler_precall();
  stv090x_frontend_algo(var_group1);
  goto ldv_24934;
  case 4:
  ldv_handler_precall();
  stv090x_send_diseqc_msg(var_group1, var_group2);
  goto ldv_24934;
  case 5:
  ldv_handler_precall();
  stv090x_send_diseqc_burst(var_group1, var_stv090x_send_diseqc_burst_57_p1);
  goto ldv_24934;
  case 6:
  ldv_handler_precall();
  stv090x_recv_slave_reply(var_group1, var_group3);
  goto ldv_24934;
  case 7:
  ldv_handler_precall();
  stv090x_set_tone(var_group1, var_stv090x_set_tone_54_p1);
  goto ldv_24934;
  case 8:
  ldv_handler_precall();
  stv090x_search(var_group1);
  goto ldv_24934;
  case 9:
  ldv_handler_precall();
  stv090x_read_status(var_group1, var_stv090x_read_status_49_p1);
  goto ldv_24934;
  case 10:
  ldv_handler_precall();
  stv090x_read_per(var_group1, var_stv090x_read_per_50_p1);
  goto ldv_24934;
  case 11:
  ldv_handler_precall();
  stv090x_read_signal_strength(var_group1, var_stv090x_read_signal_strength_52_p1);
  goto ldv_24934;
  case 12:
  ldv_handler_precall();
  stv090x_read_cnr(var_group1, var_stv090x_read_cnr_53_p1);
  goto ldv_24934;
  default: ;
  goto ldv_24934;
  }
  ldv_24934: ;
  ldv_24949:
  tmp___0 = __VERIFIER_nondet_int();
  if (tmp___0 != 0 || ldv_s_stv090x_ops_dvb_frontend_ops != 0) {
    goto ldv_24948;
  } else {
  }
  ldv_check_final_state();
  return 0;
}
}
void ldv_mutex_lock_1(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_lock_lock(ldv_func_arg1);
  mutex_lock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_unlock_2(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_unlock_lock(ldv_func_arg1);
  mutex_unlock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_lock_3(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_lock_cred_guard_mutex(ldv_func_arg1);
  mutex_lock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_unlock_4(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_unlock_cred_guard_mutex(ldv_func_arg1);
  mutex_unlock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_lock_5(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_lock_mutex(ldv_func_arg1);
  mutex_lock(ldv_func_arg1);
  return;
}
}
int ldv_mutex_trylock_6(struct mutex *ldv_func_arg1 )
{
  ldv_func_ret_type___4 ldv_func_res ;
  int tmp ;
  int tmp___0 ;
  {
  tmp = mutex_trylock(ldv_func_arg1);
  ldv_func_res = tmp;
  tmp___0 = ldv_mutex_trylock_mutex(ldv_func_arg1);
  return (tmp___0);
  return (ldv_func_res);
}
}
void ldv_mutex_unlock_7(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_unlock_mutex(ldv_func_arg1);
  mutex_unlock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_lock_8(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_lock_mtx(ldv_func_arg1);
  mutex_lock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_unlock_9(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_unlock_mtx(ldv_func_arg1);
  mutex_unlock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_lock_10(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_lock_mtx(ldv_func_arg1);
  mutex_lock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_unlock_11(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_unlock_mtx(ldv_func_arg1);
  mutex_unlock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_lock_12(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_lock_tuner_lock(ldv_func_arg1);
  mutex_lock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_unlock_13(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_unlock_tuner_lock(ldv_func_arg1);
  mutex_unlock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_unlock_14(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_unlock_tuner_lock(ldv_func_arg1);
  mutex_unlock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_lock_15(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_lock_demod_lock(ldv_func_arg1);
  mutex_lock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_unlock_16(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_unlock_demod_lock(ldv_func_arg1);
  mutex_unlock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_lock_17(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_lock_demod_lock(ldv_func_arg1);
  mutex_lock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_unlock_18(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_unlock_demod_lock(ldv_func_arg1);
  mutex_unlock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_unlock_19(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_unlock_demod_lock(ldv_func_arg1);
  mutex_unlock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_lock_20(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_lock_demod_lock(ldv_func_arg1);
  mutex_lock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_unlock_21(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_unlock_demod_lock(ldv_func_arg1);
  mutex_unlock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_unlock_22(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_unlock_demod_lock(ldv_func_arg1);
  mutex_unlock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_lock_23(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_lock_demod_lock(ldv_func_arg1);
  mutex_lock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_unlock_24(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_unlock_demod_lock(ldv_func_arg1);
  mutex_unlock(ldv_func_arg1);
  return;
}
}
void ldv_mutex_unlock_25(struct mutex *ldv_func_arg1 )
{
  {
  ldv_mutex_unlock_demod_lock(ldv_func_arg1);
  mutex_unlock(ldv_func_arg1);
  return;
}
}
__inline static void ldv_error(void) __attribute__((__no_instrument_function__)) ;
__inline static void ldv_error(void)
{
  {
  ERROR: __VERIFIER_error();
}
}
extern int __VERIFIER_nondet_int(void) ;
long ldv__builtin_expect(long exp , long c )
{
  {
  return (exp);
}
}
static int ldv_mutex_cred_guard_mutex ;
int ldv_mutex_lock_interruptible_cred_guard_mutex(struct mutex *lock )
{
  int nondetermined ;
  {
  if (ldv_mutex_cred_guard_mutex == 1) {
  } else {
    ldv_error();
  }
  nondetermined = __VERIFIER_nondet_int();
  if (nondetermined) {
    ldv_mutex_cred_guard_mutex = 2;
    return (0);
  } else {
    return (-4);
  }
}
}
int ldv_mutex_lock_killable_cred_guard_mutex(struct mutex *lock )
{
  int nondetermined ;
  {
  if (ldv_mutex_cred_guard_mutex == 1) {
  } else {
    ldv_error();
  }
  nondetermined = __VERIFIER_nondet_int();
  if (nondetermined) {
    ldv_mutex_cred_guard_mutex = 2;
    return (0);
  } else {
    return (-4);
  }
}
}
void ldv_mutex_lock_cred_guard_mutex(struct mutex *lock )
{
  {
  if (ldv_mutex_cred_guard_mutex == 1) {
  } else {
    ldv_error();
  }
  ldv_mutex_cred_guard_mutex = 2;
  return;
}
}
int ldv_mutex_trylock_cred_guard_mutex(struct mutex *lock )
{
  int is_mutex_held_by_another_thread ;
  {
  if (ldv_mutex_cred_guard_mutex == 1) {
  } else {
    ldv_error();
  }
  is_mutex_held_by_another_thread = __VERIFIER_nondet_int();
  if (is_mutex_held_by_another_thread) {
    return (0);
  } else {
    ldv_mutex_cred_guard_mutex = 2;
    return (1);
  }
}
}
int ldv_atomic_dec_and_mutex_lock_cred_guard_mutex(atomic_t *cnt , struct mutex *lock )
{
  int atomic_value_after_dec ;
  {
  if (ldv_mutex_cred_guard_mutex == 1) {
  } else {
    ldv_error();
  }
  atomic_value_after_dec = __VERIFIER_nondet_int();
  if (atomic_value_after_dec == 0) {
    ldv_mutex_cred_guard_mutex = 2;
    return (1);
  } else {
  }
  return (0);
}
}
int ldv_mutex_is_locked_cred_guard_mutex(struct mutex *lock )
{
  int nondetermined ;
  {
  if (ldv_mutex_cred_guard_mutex == 1) {
    nondetermined = __VERIFIER_nondet_int();
    if (nondetermined) {
      return (0);
    } else {
      return (1);
    }
  } else {
    return (1);
  }
}
}
void ldv_mutex_unlock_cred_guard_mutex(struct mutex *lock )
{
  {
  if (ldv_mutex_cred_guard_mutex == 2) {
  } else {
    ldv_error();
  }
  ldv_mutex_cred_guard_mutex = 1;
  return;
}
}
static int ldv_mutex_demod_lock ;
int ldv_mutex_lock_interruptible_demod_lock(struct mutex *lock )
{
  int nondetermined ;
  {
  if (ldv_mutex_demod_lock == 1) {
  } else {
    ldv_error();
  }
  nondetermined = __VERIFIER_nondet_int();
  if (nondetermined) {
    ldv_mutex_demod_lock = 2;
    return (0);
  } else {
    return (-4);
  }
}
}
int ldv_mutex_lock_killable_demod_lock(struct mutex *lock )
{
  int nondetermined ;
  {
  if (ldv_mutex_demod_lock == 1) {
  } else {
    ldv_error();
  }
  nondetermined = __VERIFIER_nondet_int();
  if (nondetermined) {
    ldv_mutex_demod_lock = 2;
    return (0);
  } else {
    return (-4);
  }
}
}
void ldv_mutex_lock_demod_lock(struct mutex *lock )
{
  {
  if (ldv_mutex_demod_lock == 1) {
  } else {
    ldv_error();
  }
  ldv_mutex_demod_lock = 2;
  return;
}
}
int ldv_mutex_trylock_demod_lock(struct mutex *lock )
{
  int is_mutex_held_by_another_thread ;
  {
  if (ldv_mutex_demod_lock == 1) {
  } else {
    ldv_error();
  }
  is_mutex_held_by_another_thread = __VERIFIER_nondet_int();
  if (is_mutex_held_by_another_thread) {
    return (0);
  } else {
    ldv_mutex_demod_lock = 2;
    return (1);
  }
}
}
int ldv_atomic_dec_and_mutex_lock_demod_lock(atomic_t *cnt , struct mutex *lock )
{
  int atomic_value_after_dec ;
  {
  if (ldv_mutex_demod_lock == 1) {
  } else {
    ldv_error();
  }
  atomic_value_after_dec = __VERIFIER_nondet_int();
  if (atomic_value_after_dec == 0) {
    ldv_mutex_demod_lock = 2;
    return (1);
  } else {
  }
  return (0);
}
}
int ldv_mutex_is_locked_demod_lock(struct mutex *lock )
{
  int nondetermined ;
  {
  if (ldv_mutex_demod_lock == 1) {
    nondetermined = __VERIFIER_nondet_int();
    if (nondetermined) {
      return (0);
    } else {
      return (1);
    }
  } else {
    return (1);
  }
}
}
void ldv_mutex_unlock_demod_lock(struct mutex *lock )
{
  {
  if (ldv_mutex_demod_lock == 2) {
  } else {
    ldv_error();
  }
  ldv_mutex_demod_lock = 1;
  return;
}
}
static int ldv_mutex_lock ;
int ldv_mutex_lock_interruptible_lock(struct mutex *lock )
{
  int nondetermined ;
  {
  if (ldv_mutex_lock == 1) {
  } else {
    ldv_error();
  }
  nondetermined = __VERIFIER_nondet_int();
  if (nondetermined) {
    ldv_mutex_lock = 2;
    return (0);
  } else {
    return (-4);
  }
}
}
int ldv_mutex_lock_killable_lock(struct mutex *lock )
{
  int nondetermined ;
  {
  if (ldv_mutex_lock == 1) {
  } else {
    ldv_error();
  }
  nondetermined = __VERIFIER_nondet_int();
  if (nondetermined) {
    ldv_mutex_lock = 2;
    return (0);
  } else {
    return (-4);
  }
}
}
void ldv_mutex_lock_lock(struct mutex *lock )
{
  {
  if (ldv_mutex_lock == 1) {
  } else {
    ldv_error();
  }
  ldv_mutex_lock = 2;
  return;
}
}
int ldv_mutex_trylock_lock(struct mutex *lock )
{
  int is_mutex_held_by_another_thread ;
  {
  if (ldv_mutex_lock == 1) {
  } else {
    ldv_error();
  }
  is_mutex_held_by_another_thread = __VERIFIER_nondet_int();
  if (is_mutex_held_by_another_thread) {
    return (0);
  } else {
    ldv_mutex_lock = 2;
    return (1);
  }
}
}
int ldv_atomic_dec_and_mutex_lock_lock(atomic_t *cnt , struct mutex *lock )
{
  int atomic_value_after_dec ;
  {
  if (ldv_mutex_lock == 1) {
  } else {
    ldv_error();
  }
  atomic_value_after_dec = __VERIFIER_nondet_int();
  if (atomic_value_after_dec == 0) {
    ldv_mutex_lock = 2;
    return (1);
  } else {
  }
  return (0);
}
}
int ldv_mutex_is_locked_lock(struct mutex *lock )
{
  int nondetermined ;
  {
  if (ldv_mutex_lock == 1) {
    nondetermined = __VERIFIER_nondet_int();
    if (nondetermined) {
      return (0);
    } else {
      return (1);
    }
  } else {
    return (1);
  }
}
}
void ldv_mutex_unlock_lock(struct mutex *lock )
{
  {
  if (ldv_mutex_lock == 2) {
  } else {
    ldv_error();
  }
  ldv_mutex_lock = 1;
  return;
}
}
static int ldv_mutex_mtx ;
int ldv_mutex_lock_interruptible_mtx(struct mutex *lock )
{
  int nondetermined ;
  {
  if (ldv_mutex_mtx == 1) {
  } else {
    ldv_error();
  }
  nondetermined = __VERIFIER_nondet_int();
  if (nondetermined) {
    ldv_mutex_mtx = 2;
    return (0);
  } else {
    return (-4);
  }
}
}
int ldv_mutex_lock_killable_mtx(struct mutex *lock )
{
  int nondetermined ;
  {
  if (ldv_mutex_mtx == 1) {
  } else {
    ldv_error();
  }
  nondetermined = __VERIFIER_nondet_int();
  if (nondetermined) {
    ldv_mutex_mtx = 2;
    return (0);
  } else {
    return (-4);
  }
}
}
void ldv_mutex_lock_mtx(struct mutex *lock )
{
  {
  if (ldv_mutex_mtx == 1) {
  } else {
    ldv_error();
  }
  ldv_mutex_mtx = 2;
  return;
}
}
int ldv_mutex_trylock_mtx(struct mutex *lock )
{
  int is_mutex_held_by_another_thread ;
  {
  if (ldv_mutex_mtx == 1) {
  } else {
    ldv_error();
  }
  is_mutex_held_by_another_thread = __VERIFIER_nondet_int();
  if (is_mutex_held_by_another_thread) {
    return (0);
  } else {
    ldv_mutex_mtx = 2;
    return (1);
  }
}
}
int ldv_atomic_dec_and_mutex_lock_mtx(atomic_t *cnt , struct mutex *lock )
{
  int atomic_value_after_dec ;
  {
  if (ldv_mutex_mtx == 1) {
  } else {
    ldv_error();
  }
  atomic_value_after_dec = __VERIFIER_nondet_int();
  if (atomic_value_after_dec == 0) {
    ldv_mutex_mtx = 2;
    return (1);
  } else {
  }
  return (0);
}
}
int ldv_mutex_is_locked_mtx(struct mutex *lock )
{
  int nondetermined ;
  {
  if (ldv_mutex_mtx == 1) {
    nondetermined = __VERIFIER_nondet_int();
    if (nondetermined) {
      return (0);
    } else {
      return (1);
    }
  } else {
    return (1);
  }
}
}
void ldv_mutex_unlock_mtx(struct mutex *lock )
{
  {
  if (ldv_mutex_mtx == 2) {
  } else {
    ldv_error();
  }
  ldv_mutex_mtx = 1;
  return;
}
}
static int ldv_mutex_mutex ;
int ldv_mutex_lock_interruptible_mutex(struct mutex *lock )
{
  int nondetermined ;
  {
  if (ldv_mutex_mutex == 1) {
  } else {
    ldv_error();
  }
  nondetermined = __VERIFIER_nondet_int();
  if (nondetermined) {
    ldv_mutex_mutex = 2;
    return (0);
  } else {
    return (-4);
  }
}
}
int ldv_mutex_lock_killable_mutex(struct mutex *lock )
{
  int nondetermined ;
  {
  if (ldv_mutex_mutex == 1) {
  } else {
    ldv_error();
  }
  nondetermined = __VERIFIER_nondet_int();
  if (nondetermined) {
    ldv_mutex_mutex = 2;
    return (0);
  } else {
    return (-4);
  }
}
}
void ldv_mutex_lock_mutex(struct mutex *lock )
{
  {
  if (ldv_mutex_mutex == 1) {
  } else {
    ldv_error();
  }
  ldv_mutex_mutex = 2;
  return;
}
}
int ldv_mutex_trylock_mutex(struct mutex *lock )
{
  int is_mutex_held_by_another_thread ;
  {
  if (ldv_mutex_mutex == 1) {
  } else {
    ldv_error();
  }
  is_mutex_held_by_another_thread = __VERIFIER_nondet_int();
  if (is_mutex_held_by_another_thread) {
    return (0);
  } else {
    ldv_mutex_mutex = 2;
    return (1);
  }
}
}
int ldv_atomic_dec_and_mutex_lock_mutex(atomic_t *cnt , struct mutex *lock )
{
  int atomic_value_after_dec ;
  {
  if (ldv_mutex_mutex == 1) {
  } else {
    ldv_error();
  }
  atomic_value_after_dec = __VERIFIER_nondet_int();
  if (atomic_value_after_dec == 0) {
    ldv_mutex_mutex = 2;
    return (1);
  } else {
  }
  return (0);
}
}
int ldv_mutex_is_locked_mutex(struct mutex *lock )
{
  int nondetermined ;
  {
  if (ldv_mutex_mutex == 1) {
    nondetermined = __VERIFIER_nondet_int();
    if (nondetermined) {
      return (0);
    } else {
      return (1);
    }
  } else {
    return (1);
  }
}
}
void ldv_mutex_unlock_mutex(struct mutex *lock )
{
  {
  if (ldv_mutex_mutex == 2) {
  } else {
    ldv_error();
  }
  ldv_mutex_mutex = 1;
  return;
}
}
static int ldv_mutex_tuner_lock ;
int ldv_mutex_lock_interruptible_tuner_lock(struct mutex *lock )
{
  int nondetermined ;
  {
  if (ldv_mutex_tuner_lock == 1) {
  } else {
    ldv_error();
  }
  nondetermined = __VERIFIER_nondet_int();
  if (nondetermined) {
    ldv_mutex_tuner_lock = 2;
    return (0);
  } else {
    return (-4);
  }
}
}
int ldv_mutex_lock_killable_tuner_lock(struct mutex *lock )
{
  int nondetermined ;
  {
  if (ldv_mutex_tuner_lock == 1) {
  } else {
    ldv_error();
  }
  nondetermined = __VERIFIER_nondet_int();
  if (nondetermined) {
    ldv_mutex_tuner_lock = 2;
    return (0);
  } else {
    return (-4);
  }
}
}
void ldv_mutex_lock_tuner_lock(struct mutex *lock )
{
  {
  if (ldv_mutex_tuner_lock == 1) {
  } else {
    ldv_error();
  }
  ldv_mutex_tuner_lock = 2;
  return;
}
}
int ldv_mutex_trylock_tuner_lock(struct mutex *lock )
{
  int is_mutex_held_by_another_thread ;
  {
  if (ldv_mutex_tuner_lock == 1) {
  } else {
    ldv_error();
  }
  is_mutex_held_by_another_thread = __VERIFIER_nondet_int();
  if (is_mutex_held_by_another_thread) {
    return (0);
  } else {
    ldv_mutex_tuner_lock = 2;
    return (1);
  }
}
}
int ldv_atomic_dec_and_mutex_lock_tuner_lock(atomic_t *cnt , struct mutex *lock )
{
  int atomic_value_after_dec ;
  {
  if (ldv_mutex_tuner_lock == 1) {
  } else {
    ldv_error();
  }
  atomic_value_after_dec = __VERIFIER_nondet_int();
  if (atomic_value_after_dec == 0) {
    ldv_mutex_tuner_lock = 2;
    return (1);
  } else {
  }
  return (0);
}
}
int ldv_mutex_is_locked_tuner_lock(struct mutex *lock )
{
  int nondetermined ;
  {
  if (ldv_mutex_tuner_lock == 1) {
    nondetermined = __VERIFIER_nondet_int();
    if (nondetermined) {
      return (0);
    } else {
      return (1);
    }
  } else {
    return (1);
  }
}
}
void ldv_mutex_unlock_tuner_lock(struct mutex *lock )
{
  {
  if (ldv_mutex_tuner_lock == 2) {
  } else {
    ldv_error();
  }
  ldv_mutex_tuner_lock = 1;
  return;
}
}
void ldv_initialize(void)
{
  {
  ldv_mutex_cred_guard_mutex = 1;
  ldv_mutex_demod_lock = 1;
  ldv_mutex_lock = 1;
  ldv_mutex_mtx = 1;
  ldv_mutex_mutex = 1;
  ldv_mutex_tuner_lock = 1;
  return;
}
}
void ldv_check_final_state(void)
{
  {
  if (ldv_mutex_cred_guard_mutex == 1) {
  } else {
    ldv_error();
  }
  if (ldv_mutex_demod_lock == 1) {
  } else {
    ldv_error();
  }
  if (ldv_mutex_lock == 1) {
  } else {
    ldv_error();
  }
  if (ldv_mutex_mtx == 1) {
  } else {
    ldv_error();
  }
  if (ldv_mutex_mutex == 1) {
  } else {
    ldv_error();
  }
  if (ldv_mutex_tuner_lock == 1) {
  } else {
    ldv_error();
  }
  return;
}
}
void __mutex_init(struct mutex *arg0, const char *arg1, struct lock_class_key *arg2) {
  return;
}
int __VERIFIER_nondet_int(void);
int i2c_transfer(struct i2c_adapter *arg0, struct i2c_msg *arg1, int arg2) {
  return __VERIFIER_nondet_int();
}
void ldv_handler_precall() {
  return;
}
void msleep(unsigned int arg0) {
  return;
}
void mutex_lock(struct mutex *arg0) {
  return;
}
int __VERIFIER_nondet_int(void);
int mutex_trylock(struct mutex *arg0) {
  return __VERIFIER_nondet_int();
}
void mutex_unlock(struct mutex *arg0) {
  return;
}
int __VERIFIER_nondet_int(void);
int printk(const char *arg0, ...) {
  return __VERIFIER_nondet_int();
}
void *__VERIFIER_nondet_pointer(void);
void *external_alloc(void) {
  return __VERIFIER_nondet_pointer();
}
void free(void *);
void kfree(void const *p) {
  free((void *)p);
}
