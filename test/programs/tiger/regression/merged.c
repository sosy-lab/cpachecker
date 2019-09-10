# 1 "/home/sruland/git/c-case-study-preperation/MutationRevisionGenerator/output/revisions/linux-drivers-clk1__drivers---spi---spi-imx.ko.d8e172f3c0a5.cil_mx31_config/reemergingBugs/linux-drivers-clk1__drivers---spi---spi-imx.ko.d8e172f3c0a5.cil_mx31_config_revision_30.creemerging_20.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "/usr/include/stdc-predef.h" 1 3 4
# 1 "<command-line>" 2
# 1 "/home/sruland/git/c-case-study-preperation/MutationRevisionGenerator/output/revisions/linux-drivers-clk1__drivers---spi---spi-imx.ko.d8e172f3c0a5.cil_mx31_config/reemergingBugs/linux-drivers-clk1__drivers---spi---spi-imx.ko.d8e172f3c0a5.cil_mx31_config_revision_30.creemerging_20.c"
# 1 "output/prototypes/linux-drivers-clk1__drivers---spi---spi-imx.ko.d8e172f3c0a5.cil/linux-drivers-clk1__drivers---spi---spi-imx.ko.d8e172f3c0a5.cilproto.h" 1


struct device;

struct spi_master;

typedef unsigned char __u8;

typedef unsigned short __u16;

typedef int __s32;

typedef unsigned int __u32;

typedef unsigned long long __u64;

typedef unsigned char u8;

typedef short s16;

typedef unsigned short u16;

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

typedef u64 dma_addr_t;

typedef unsigned int gfp_t;

typedef unsigned int oom_flags_t;

typedef u64 phys_addr_t;

typedef phys_addr_t resource_size_t;

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

struct clk;

struct resource {
   resource_size_t start ;
   resource_size_t end ;
   char const *name ;
   unsigned long flags ;
   struct resource *parent ;
   struct resource *sibling ;
   struct resource *child ;
};

struct task_struct;

struct lockdep_map;

struct kernel_symbol {
   unsigned long value ;
   char const *name ;
};

struct module;

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

typedef void (*ctor_fn_t)(void);

struct __anonstruct____missing_field_name_9 {
   unsigned int a ;
   unsigned int b ;
};

struct __anonstruct____missing_field_name_10 {
   u16 limit0 ;
   u16 base0 ;
   unsigned int base1 : 8 ;
   unsigned int type : 4 ;
   unsigned int s : 1 ;
   unsigned int dpl : 2 ;
   unsigned int p : 1 ;
   unsigned int limit : 4 ;
   unsigned int avl : 1 ;
   unsigned int l : 1 ;
   unsigned int d : 1 ;
   unsigned int g : 1 ;
   unsigned int base2 : 8 ;
};

union __anonunion____missing_field_name_8 {
   struct __anonstruct____missing_field_name_9 __annonCompField4 ;
   struct __anonstruct____missing_field_name_10 __annonCompField5 ;
};

struct desc_struct {
   union __anonunion____missing_field_name_8 __annonCompField6 ;
};

typedef unsigned long pgdval_t;

typedef unsigned long pgprotval_t;

struct pgprot {
   pgprotval_t pgprot ;
};

typedef struct pgprot pgprot_t;

struct __anonstruct_pgd_t_12 {
   pgdval_t pgd ;
};

typedef struct __anonstruct_pgd_t_12 pgd_t;

struct page;

typedef struct page *pgtable_t;

struct file;

struct thread_struct;

struct mm_struct;

struct cpumask;

struct arch_spinlock;

typedef u16 __ticket_t;

typedef u32 __ticketpair_t;

struct __raw_tickets {
   __ticket_t head ;
   __ticket_t tail ;
};

union __anonunion____missing_field_name_15 {
   __ticketpair_t head_tail ;
   struct __raw_tickets tickets ;
};

struct arch_spinlock {
   union __anonunion____missing_field_name_15 __annonCompField7 ;
};

typedef struct arch_spinlock arch_spinlock_t;

struct _ddebug {
   char const *modname ;
   char const *function ;
   char const *filename ;
   char const *format ;
   unsigned int lineno : 18 ;
   unsigned int flags : 8 ;
};

struct completion;

struct pid;

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

union __anonunion____missing_field_name_18 {
   struct pt_regs *regs ;
   struct kernel_vm86_regs *vm86 ;
};

struct math_emu_info {
   long ___orig_eip ;
   union __anonunion____missing_field_name_18 __annonCompField9 ;
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

struct __anonstruct____missing_field_name_23 {
   u64 rip ;
   u64 rdp ;
};

struct __anonstruct____missing_field_name_24 {
   u32 fip ;
   u32 fcs ;
   u32 foo ;
   u32 fos ;
};

union __anonunion____missing_field_name_22 {
   struct __anonstruct____missing_field_name_23 __annonCompField13 ;
   struct __anonstruct____missing_field_name_24 __annonCompField14 ;
};

union __anonunion____missing_field_name_25 {
   u32 padding1[12U] ;
   u32 sw_reserved[12U] ;
};

struct i387_fxsave_struct {
   u16 cwd ;
   u16 swd ;
   u16 twd ;
   u16 fop ;
   union __anonunion____missing_field_name_22 __annonCompField15 ;
   u32 mxcsr ;
   u32 mxcsr_mask ;
   u32 st_space[32U] ;
   u32 xmm_space[64U] ;
   u32 padding[12U] ;
   union __anonunion____missing_field_name_25 __annonCompField16 ;
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
};

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
   unsigned int class_idx : 13 ;
   unsigned int irq_context : 2 ;
   unsigned int trylock : 1 ;
   unsigned int read : 2 ;
   unsigned int check : 2 ;
   unsigned int hardirqs_off : 1 ;
   unsigned int references : 11 ;
};

struct sock;

struct kobject;

enum kobj_ns_type {
    KOBJ_NS_TYPE_NONE = 0,
    KOBJ_NS_TYPE_NET = 1,
    KOBJ_NS_TYPES = 2
};

struct kobj_ns_type_operations {
   enum kobj_ns_type type ;
   bool (*current_may_mount)(void) ;
   void *(*grab_current_ns)(void) ;
   void const *(*netlink_ns)(struct sock * ) ;
   void const *(*initial_ns)(void) ;
   void (*drop_ns)(void * ) ;
};

struct timespec;

struct raw_spinlock {
   arch_spinlock_t raw_lock ;
   unsigned int magic ;
   unsigned int owner_cpu ;
   void *owner ;
   struct lockdep_map dep_map ;
};

typedef struct raw_spinlock raw_spinlock_t;

struct __anonstruct____missing_field_name_33 {
   u8 __padding[24U] ;
   struct lockdep_map dep_map ;
};

union __anonunion____missing_field_name_32 {
   struct raw_spinlock rlock ;
   struct __anonstruct____missing_field_name_33 __annonCompField19 ;
};

struct spinlock {
   union __anonunion____missing_field_name_32 __annonCompField20 ;
};

typedef struct spinlock spinlock_t;

struct jump_entry;

struct static_key_mod;

struct static_key {
   atomic_t enabled ;
   struct jump_entry *entries ;
   struct static_key_mod *next ;
};

typedef u64 jump_label_t;

struct jump_entry {
   jump_label_t code ;
   jump_label_t target ;
   jump_label_t key ;
};

struct seqcount {
   unsigned int sequence ;
};

typedef struct seqcount seqcount_t;

struct timespec {
   __kernel_time_t tv_sec ;
   long tv_nsec ;
};

struct user_namespace;

struct __anonstruct_kuid_t_36 {
   uid_t val ;
};

typedef struct __anonstruct_kuid_t_36 kuid_t;

struct __anonstruct_kgid_t_37 {
   gid_t val ;
};

typedef struct __anonstruct_kgid_t_37 kgid_t;

struct bin_attribute;

struct attribute {
   char const *name ;
   umode_t mode ;
   bool ignore_lockdep : 1 ;
   struct lock_class_key *key ;
   struct lock_class_key skey ;
};

struct attribute_group {
   char const *name ;
   umode_t (*is_visible)(struct kobject * , struct attribute * , int ) ;
   struct attribute **attrs ;
   struct bin_attribute **bin_attrs ;
};

struct vm_area_struct;

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

struct kref {
   atomic_t refcount ;
};

struct __wait_queue_head {
   spinlock_t lock ;
   struct list_head task_list ;
};

typedef struct __wait_queue_head wait_queue_head_t;

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

struct workqueue_struct;

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
   struct workqueue_struct *wq ;
   int cpu ;
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
   struct delayed_work release ;
   unsigned int state_initialized : 1 ;
   unsigned int state_in_sysfs : 1 ;
   unsigned int state_add_uevent_sent : 1 ;
   unsigned int state_remove_uevent_sent : 1 ;
   unsigned int uevent_suppress : 1 ;
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

struct klist_node;

struct klist_node {
   void *n_klist ;
   struct list_head n_node ;
   struct kref n_ref ;
};

struct __anonstruct_nodemask_t_38 {
   unsigned long bits[16U] ;
};

typedef struct __anonstruct_nodemask_t_38 nodemask_t;

struct pinctrl;

struct pinctrl_state;

struct dev_pin_info {
   struct pinctrl *p ;
   struct pinctrl_state *default_state ;
   struct pinctrl_state *sleep_state ;
   struct pinctrl_state *idle_state ;
};

struct completion {
   unsigned int done ;
   wait_queue_head_t wait ;
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
};

enum rpm_request {
    RPM_REQ_NONE = 0,
    RPM_REQ_IDLE = 1,
    RPM_REQ_SUSPEND = 2,
    RPM_REQ_AUTOSUSPEND = 3,
    RPM_REQ_RESUME = 4
};

struct wakeup_source;

struct pm_subsys_data {
   spinlock_t lock ;
   unsigned int refcount ;
   struct list_head clock_list ;
};

struct dev_pm_qos;

struct dev_pm_info {
   pm_message_t power_state ;
   unsigned int can_wakeup : 1 ;
   unsigned int async_suspend : 1 ;
   bool is_prepared : 1 ;
   bool is_suspended : 1 ;
   bool ignore_children : 1 ;
   bool early_init : 1 ;
   spinlock_t lock ;
   struct list_head entry ;
   struct completion completion ;
   struct wakeup_source *wakeup ;
   bool wakeup_path : 1 ;
   bool syscore : 1 ;
   struct timer_list suspend_timer ;
   unsigned long timer_expires ;
   struct work_struct work ;
   wait_queue_head_t wait_queue ;
   atomic_t usage_count ;
   atomic_t child_count ;
   unsigned int disable_depth : 3 ;
   unsigned int idle_notification : 1 ;
   unsigned int request_pending : 1 ;
   unsigned int deferred_resume : 1 ;
   unsigned int run_wake : 1 ;
   unsigned int runtime_auto : 1 ;
   unsigned int no_callbacks : 1 ;
   unsigned int irq_safe : 1 ;
   unsigned int use_autosuspend : 1 ;
   unsigned int timer_autosuspends : 1 ;
   unsigned int memalloc_noio : 1 ;
   enum rpm_request request ;
   enum rpm_status runtime_status ;
   int runtime_error ;
   int autosuspend_delay ;
   unsigned long last_busy ;
   unsigned long active_jiffies ;
   unsigned long suspended_jiffies ;
   unsigned long accounting_timestamp ;
   struct pm_subsys_data *subsys_data ;
   struct dev_pm_qos *qos ;
};

struct dev_pm_domain {
   struct dev_pm_ops ops ;
};

struct dma_map_ops;

struct dev_archdata {
   struct dma_map_ops *dma_ops ;
   void *iommu ;
};

struct pdev_archdata {

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
   struct attribute_group const **bus_groups ;
   struct attribute_group const **dev_groups ;
   struct attribute_group const **drv_groups ;
   int (*match)(struct device * , struct device_driver * ) ;
   int (*uevent)(struct device * , struct kobj_uevent_env * ) ;
   int (*probe)(struct device * ) ;
   int (*remove)(struct device * ) ;
   void (*shutdown)(struct device * ) ;
   int (*online)(struct device * ) ;
   int (*offline)(struct device * ) ;
   int (*suspend)(struct device * , pm_message_t ) ;
   int (*resume)(struct device * ) ;
   struct dev_pm_ops const *pm ;
   struct iommu_ops *iommu_ops ;
   struct subsys_private *p ;
   struct lock_class_key lock_key ;
};

struct device_type;

struct of_device_id;

struct acpi_device_id;

struct device_driver {
   char const *name ;
   struct bus_type *bus ;
   struct module *owner ;
   char const *mod_name ;
   bool suppress_bind_attrs ;
   struct of_device_id const *of_match_table ;
   struct acpi_device_id const *acpi_match_table ;
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
   struct attribute_group const **dev_groups ;
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
   char *(*devnode)(struct device * , umode_t * , kuid_t * , kgid_t * ) ;
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

struct acpi_dev_node {
   void *handle ;
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
   struct dev_pin_info *pins ;
   int numa_node ;
   u64 *dma_mask ;
   u64 coherent_dma_mask ;
   struct device_dma_parameters *dma_parms ;
   struct list_head dma_pools ;
   struct dma_coherent_mem *dma_mem ;
   struct dev_archdata archdata ;
   struct device_node *of_node ;
   struct acpi_dev_node acpi_node ;
   dev_t devt ;
   u32 id ;
   spinlock_t devres_lock ;
   struct list_head devres_head ;
   struct klist_node knode_class ;
   struct class *class ;
   struct attribute_group const **groups ;
   void (*release)(struct device * ) ;
   struct iommu_group *iommu_group ;
   bool offline_disabled : 1 ;
   bool offline : 1 ;
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
   bool active : 1 ;
   bool autosleep_enabled : 1 ;
};

typedef unsigned long kernel_ulong_t;

struct acpi_device_id {
   __u8 id[9U] ;
   kernel_ulong_t driver_data ;
};

struct of_device_id {
   char name[32U] ;
   char type[32U] ;
   char compatible[128U] ;
   void const *data ;
};

struct platform_device_id {
   char name[20U] ;
   kernel_ulong_t driver_data ;
};

struct mfd_cell;

struct platform_device {
   char const *name ;
   int id ;
   bool id_auto ;
   struct device dev ;
   u32 num_resources ;
   struct resource *resource ;
   struct platform_device_id const *id_entry ;
   struct mfd_cell *mfd_cell ;
   struct pdev_archdata archdata ;
};

struct platform_driver {
   int (*probe)(struct platform_device * ) ;
   int (*remove)(struct platform_device * ) ;
   void (*shutdown)(struct platform_device * ) ;
   int (*suspend)(struct platform_device * , pm_message_t ) ;
   int (*resume)(struct platform_device * ) ;
   struct device_driver driver ;
   struct platform_device_id const *id_table ;
};

struct rw_semaphore;

struct rw_semaphore {
   long count ;
   raw_spinlock_t wait_lock ;
   struct list_head wait_list ;
   struct lockdep_map dep_map ;
};

struct __anonstruct_mm_context_t_106 {
   void *ldt ;
   int size ;
   unsigned short ia32_compat ;
   struct mutex lock ;
   void *vdso ;
};

typedef struct __anonstruct_mm_context_t_106 mm_context_t;

struct rb_node {
   unsigned long __rb_parent_color ;
   struct rb_node *rb_right ;
   struct rb_node *rb_left ;
};

struct rb_root {
   struct rb_node *rb_node ;
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
   char const *full_name ;
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

enum irqreturn {
    IRQ_NONE = 0,
    IRQ_HANDLED = 1,
    IRQ_WAKE_THREAD = 2
};

typedef enum irqreturn irqreturn_t;

struct exception_table_entry {
   int insn ;
   int fixup ;
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
};

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
   struct hrtimer_clock_base clock_base[4U] ;
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

struct kernel_param;

struct kernel_param_ops {
   unsigned int flags ;
   int (*set)(char const * , struct kernel_param const * ) ;
   int (*get)(char * , struct kernel_param const * ) ;
   void (*free)(void * ) ;
};

struct kparam_string;

struct kparam_array;

union __anonunion____missing_field_name_141 {
   void *arg ;
   struct kparam_string const *str ;
   struct kparam_array const *arr ;
};

struct kernel_param {
   char const *name ;
   struct kernel_param_ops const *ops ;
   u16 perm ;
   s16 level ;
   union __anonunion____missing_field_name_141 __annonCompField34 ;
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

struct mod_arch_specific {

};

struct module_param_attrs;

struct module_kobject {
   struct kobject kobj ;
   struct module *mod ;
   struct kobject *drivers_dir ;
   struct module_param_attrs *mp ;
   struct completion *kobj_completion ;
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

enum module_state {
    MODULE_STATE_LIVE = 0,
    MODULE_STATE_COMING = 1,
    MODULE_STATE_GOING = 2,
    MODULE_STATE_UNFORMED = 3
};

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
   bool sig_ok ;
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
   struct jump_entry *jump_entries ;
   unsigned int num_jump_entries ;
   unsigned int num_trace_bprintk_fmt ;
   char const **trace_bprintk_fmt_start ;
   struct ftrace_event_call **trace_events ;
   unsigned int num_trace_events ;
   unsigned int num_ftrace_callsites ;
   unsigned long *ftrace_callsites ;
   struct list_head source_list ;
   struct list_head target_list ;
   struct task_struct *waiter ;
   void (*exit)(void) ;
   struct module_ref *refptr ;
   ctor_fn_t (**ctors)(void) ;
   unsigned int num_ctors ;
};

struct mem_cgroup;

struct kmem_cache_cpu {
   void **freelist ;
   unsigned long tid ;
   struct page *page ;
   struct page *partial ;
   unsigned int stat[26U] ;
};

struct kmem_cache_order_objects {
   unsigned long x ;
};

struct memcg_cache_params;

struct kmem_cache_node;

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
   struct memcg_cache_params *memcg_params ;
   int max_attr_size ;
   int remote_node_defrag_ratio ;
   struct kmem_cache_node *node[1024U] ;
};

struct __anonstruct____missing_field_name_143 {
   struct mem_cgroup *memcg ;
   struct list_head list ;
   struct kmem_cache *root_cache ;
   bool dead ;
   atomic_t nr_pages ;
   struct work_struct destroy ;
};

union __anonunion____missing_field_name_142 {
   struct kmem_cache *memcg_caches[0U] ;
   struct __anonstruct____missing_field_name_143 __annonCompField35 ;
};

struct memcg_cache_params {
   bool is_root_cache ;
   union __anonunion____missing_field_name_142 __annonCompField36 ;
};

struct kernel_cap_struct {
   __u32 cap[2U] ;
};

typedef struct kernel_cap_struct kernel_cap_t;

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
};

struct return_instance;

struct uprobe;

struct uprobe_task {
   enum uprobe_task_state state ;
   struct arch_uprobe_task autask ;
   struct return_instance *return_instances ;
   unsigned int depth ;
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

union __anonunion____missing_field_name_146 {
   unsigned long index ;
   void *freelist ;
   bool pfmemalloc ;
};

struct __anonstruct____missing_field_name_150 {
   unsigned int inuse : 16 ;
   unsigned int objects : 15 ;
   unsigned int frozen : 1 ;
};

union __anonunion____missing_field_name_149 {
   atomic_t _mapcount ;
   struct __anonstruct____missing_field_name_150 __annonCompField38 ;
   int units ;
};

struct __anonstruct____missing_field_name_148 {
   union __anonunion____missing_field_name_149 __annonCompField39 ;
   atomic_t _count ;
};

union __anonunion____missing_field_name_147 {
   unsigned long counters ;
   struct __anonstruct____missing_field_name_148 __annonCompField40 ;
};

struct __anonstruct____missing_field_name_145 {
   union __anonunion____missing_field_name_146 __annonCompField37 ;
   union __anonunion____missing_field_name_147 __annonCompField41 ;
};

struct __anonstruct____missing_field_name_152 {
   struct page *next ;
   int pages ;
   int pobjects ;
};

struct slab;

union __anonunion____missing_field_name_151 {
   struct list_head lru ;
   struct __anonstruct____missing_field_name_152 __annonCompField43 ;
   struct list_head list ;
   struct slab *slab_page ;
};

union __anonunion____missing_field_name_153 {
   unsigned long private ;
   struct kmem_cache *slab_cache ;
   struct page *first_page ;
};

struct page {
   unsigned long flags ;
   struct address_space *mapping ;
   struct __anonstruct____missing_field_name_145 __annonCompField42 ;
   union __anonunion____missing_field_name_151 __annonCompField44 ;
   union __anonunion____missing_field_name_153 __annonCompField45 ;
   unsigned long debug_flags ;
};

struct page_frag {
   struct page *page ;
   __u32 offset ;
   __u32 size ;
};

struct __anonstruct_linear_155 {
   struct rb_node rb ;
   unsigned long rb_subtree_last ;
};

union __anonunion_shared_154 {
   struct __anonstruct_linear_155 linear ;
   struct list_head nonlinear ;
};

struct anon_vma;

struct vm_operations_struct;

struct mempolicy;

struct vm_area_struct {
   unsigned long vm_start ;
   unsigned long vm_end ;
   struct vm_area_struct *vm_next ;
   struct vm_area_struct *vm_prev ;
   struct rb_node vm_rb ;
   unsigned long rb_subtree_gap ;
   struct mm_struct *vm_mm ;
   pgprot_t vm_page_prot ;
   unsigned long vm_flags ;
   union __anonunion_shared_154 shared ;
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

struct kioctx_table;

struct linux_binfmt;

struct mmu_notifier_mm;

struct mm_struct {
   struct vm_area_struct *mmap ;
   struct rb_root mm_rb ;
   struct vm_area_struct *mmap_cache ;
   unsigned long (*get_unmapped_area)(struct file * , unsigned long , unsigned long ,
                                      unsigned long , unsigned long ) ;
   unsigned long mmap_base ;
   unsigned long mmap_legacy_base ;
   unsigned long task_size ;
   unsigned long highest_vm_end ;
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
   unsigned long saved_auxv[46U] ;
   struct mm_rss_stat rss_stat ;
   struct linux_binfmt *binfmt ;
   cpumask_var_t cpu_vm_mask_var ;
   mm_context_t context ;
   unsigned long flags ;
   struct core_state *core_state ;
   spinlock_t ioctx_lock ;
   struct kioctx_table *ioctx_table ;
   struct task_struct *owner ;
   struct file *exe_file ;
   struct mmu_notifier_mm *mmu_notifier_mm ;
   pgtable_t pmd_huge_pte ;
   struct cpumask cpumask_allocation ;
   unsigned long numa_next_scan ;
   unsigned long numa_next_reset ;
   unsigned long numa_scan_offset ;
   int numa_scan_seq ;
   int first_nid ;
   bool tlb_flush_pending ;
   struct uprobes_state uprobes_state ;
};

typedef unsigned long cputime_t;

struct sem_undo_list;

struct sysv_sem {
   struct sem_undo_list *undo_list ;
};

struct __anonstruct_sigset_t_156 {
   unsigned long sig[1U] ;
};

typedef struct __anonstruct_sigset_t_156 sigset_t;

struct siginfo;

typedef void __signalfn_t(int );

typedef __signalfn_t *__sighandler_t;

typedef void __restorefn_t(void);

typedef __restorefn_t *__sigrestore_t;

union sigval {
   int sival_int ;
   void *sival_ptr ;
};

typedef union sigval sigval_t;

struct __anonstruct__kill_158 {
   __kernel_pid_t _pid ;
   __kernel_uid32_t _uid ;
};

struct __anonstruct__timer_159 {
   __kernel_timer_t _tid ;
   int _overrun ;
   char _pad[0U] ;
   sigval_t _sigval ;
   int _sys_private ;
};

struct __anonstruct__rt_160 {
   __kernel_pid_t _pid ;
   __kernel_uid32_t _uid ;
   sigval_t _sigval ;
};

struct __anonstruct__sigchld_161 {
   __kernel_pid_t _pid ;
   __kernel_uid32_t _uid ;
   int _status ;
   __kernel_clock_t _utime ;
   __kernel_clock_t _stime ;
};

struct __anonstruct__sigfault_162 {
   void *_addr ;
   short _addr_lsb ;
};

struct __anonstruct__sigpoll_163 {
   long _band ;
   int _fd ;
};

struct __anonstruct__sigsys_164 {
   void *_call_addr ;
   int _syscall ;
   unsigned int _arch ;
};

union __anonunion__sifields_157 {
   int _pad[28U] ;
   struct __anonstruct__kill_158 _kill ;
   struct __anonstruct__timer_159 _timer ;
   struct __anonstruct__rt_160 _rt ;
   struct __anonstruct__sigchld_161 _sigchld ;
   struct __anonstruct__sigfault_162 _sigfault ;
   struct __anonstruct__sigpoll_163 _sigpoll ;
   struct __anonstruct__sigsys_164 _sigsys ;
};

struct siginfo {
   int si_signo ;
   int si_errno ;
   int si_code ;
   union __anonunion__sifields_157 _sifields ;
};

typedef struct siginfo siginfo_t;

struct user_struct;

struct sigpending {
   struct list_head list ;
   sigset_t signal ;
};

struct sigaction {
   __sighandler_t sa_handler ;
   unsigned long sa_flags ;
   __sigrestore_t sa_restorer ;
   sigset_t sa_mask ;
};

struct k_sigaction {
   struct sigaction sa ;
};

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

struct rt_mutex_waiter;

struct rlimit {
   unsigned long rlim_cur ;
   unsigned long rlim_max ;
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

union __anonunion____missing_field_name_167 {
   struct list_head graveyard_link ;
   struct rb_node serial_node ;
};

struct key_user;

union __anonunion____missing_field_name_168 {
   time_t expiry ;
   time_t revoked_at ;
};

union __anonunion_type_data_169 {
   struct list_head link ;
   unsigned long x[2U] ;
   void *p[2U] ;
   int reject_error ;
};

union __anonunion_payload_170 {
   unsigned long value ;
   void *rcudata ;
   void *data ;
   struct keyring_list *subscriptions ;
};

struct key {
   atomic_t usage ;
   key_serial_t serial ;
   union __anonunion____missing_field_name_167 __annonCompField46 ;
   struct key_type *type ;
   struct rw_semaphore sem ;
   struct key_user *user ;
   void *security ;
   union __anonunion____missing_field_name_168 __annonCompField47 ;
   time_t last_used_at ;
   kuid_t uid ;
   kgid_t gid ;
   key_perm_t perm ;
   unsigned short quotalen ;
   unsigned short datalen ;
   unsigned long flags ;
   char *description ;
   union __anonunion_type_data_169 type_data ;
   union __anonunion_payload_170 payload ;
};

struct audit_context;

struct group_info {
   atomic_t usage ;
   int ngroups ;
   int nblocks ;
   kgid_t small_block[32U] ;
   kgid_t *blocks[0U] ;
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
   struct key *session_keyring ;
   struct key *process_keyring ;
   struct key *thread_keyring ;
   struct key *request_key_auth ;
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

struct cputime {
   cputime_t utime ;
   cputime_t stime ;
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
   unsigned int is_child_subreaper : 1 ;
   unsigned int has_child_subreaper : 1 ;
   int posix_timer_id ;
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
   struct cputime prev_cputime ;
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
   unsigned int audit_tty_log_passwd ;
   struct tty_audit_buf *tty_audit_buf ;
   struct rw_semaphore group_rwsem ;
   oom_flags_t oom_flags ;
   short oom_score_adj ;
   short oom_score_adj_min ;
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

struct load_weight {
   unsigned long weight ;
   unsigned long inv_weight ;
};

struct sched_avg {
   u32 runnable_avg_sum ;
   u32 runnable_avg_period ;
   u64 last_runnable_update ;
   s64 decay_count ;
   unsigned long load_avg_contrib ;
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
   struct sched_avg avg ;
};

struct rt_rq;

struct sched_rt_entity {
   struct list_head run_list ;
   unsigned long timeout ;
   unsigned long watchdog_stamp ;
   unsigned int time_slice ;
   struct sched_rt_entity *back ;
   struct sched_rt_entity *parent ;
   struct rt_rq *rt_rq ;
   struct rt_rq *my_q ;
};

struct memcg_batch_info {
   int do_batch ;
   struct mem_cgroup *memcg ;
   unsigned long nr_pages ;
   unsigned long memsw_nr_pages ;
};

struct memcg_oom_info {
   struct mem_cgroup *memcg ;
   gfp_t gfp_mask ;
   int order ;
   unsigned int may_oom : 1 ;
};

struct sched_class;

struct files_struct;

struct css_set;

struct compat_robust_list_head;

struct ftrace_ret_stack;

struct task_struct {
   long volatile state ;
   void *stack ;
   atomic_t usage ;
   unsigned int flags ;
   unsigned int ptrace ;
   struct llist_node wake_entry ;
   int on_cpu ;
   struct task_struct *last_wakee ;
   unsigned long wakee_flips ;
   unsigned long wakee_flip_decay_ts ;
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
   unsigned int btrace_seq ;
   unsigned int policy ;
   int nr_cpus_allowed ;
   cpumask_t cpus_allowed ;
   struct sched_info sched_info ;
   struct list_head tasks ;
   struct plist_node pushable_tasks ;
   struct mm_struct *mm ;
   struct mm_struct *active_mm ;
   unsigned int brk_randomized : 1 ;
   int exit_state ;
   int exit_code ;
   int exit_signal ;
   int pdeath_signal ;
   unsigned int jobctl ;
   unsigned int personality ;
   unsigned int did_exec : 1 ;
   unsigned int in_execve : 1 ;
   unsigned int in_iowait : 1 ;
   unsigned int no_new_privs : 1 ;
   unsigned int sched_reset_on_fork : 1 ;
   unsigned int sched_contributes_to_load : 1 ;
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
   struct cputime prev_cputime ;
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
   int numa_scan_seq ;
   int numa_migrate_seq ;
   unsigned int numa_scan_period ;
   u64 node_stamp ;
   struct callback_head numa_work ;
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
   int curr_ret_stack ;
   struct ftrace_ret_stack *ret_stack ;
   unsigned long long ftrace_timestamp ;
   atomic_t trace_overrun ;
   atomic_t tracing_graph_pause ;
   unsigned long trace ;
   unsigned long trace_recursion ;
   struct memcg_batch_info memcg_batch ;
   unsigned int memcg_kmem_skip_account ;
   struct memcg_oom_info memcg_oom ;
   struct uprobe_task *utask ;
   unsigned int sequential_io ;
   unsigned int sequential_io_avg ;
};

struct kthread_work;

struct kthread_worker {
   spinlock_t lock ;
   struct list_head work_list ;
   struct task_struct *task ;
   struct kthread_work *current_work ;
};

struct kthread_work {
   struct list_head node ;
   void (*func)(struct kthread_work * ) ;
   wait_queue_head_t done ;
   struct kthread_worker *worker ;
};

struct spi_device {
   struct device dev ;
   struct spi_master *master ;
   u32 max_speed_hz ;
   u8 chip_select ;
   u16 mode ;
   u8 bits_per_word ;
   int irq ;
   void *controller_state ;
   void *controller_data ;
   char modalias[32U] ;
   int cs_gpio ;
};

struct spi_message;

struct spi_master {
   struct device dev ;
   struct list_head list ;
   s16 bus_num ;
   u16 num_chipselect ;
   u16 dma_alignment ;
   u16 mode_bits ;
   u32 bits_per_word_mask ;
   u32 min_speed_hz ;
   u32 max_speed_hz ;
   u16 flags ;
   spinlock_t bus_lock_spinlock ;
   struct mutex bus_lock_mutex ;
   bool bus_lock_flag ;
   int (*setup)(struct spi_device * ) ;
   int (*transfer)(struct spi_device * , struct spi_message * ) ;
   void (*cleanup)(struct spi_device * ) ;
   bool queued ;
   struct kthread_worker kworker ;
   struct task_struct *kworker_task ;
   struct kthread_work pump_messages ;
   spinlock_t queue_lock ;
   struct list_head queue ;
   struct spi_message *cur_msg ;
   bool busy ;
   bool running ;
   bool rt ;
   bool auto_runtime_pm ;
   int (*prepare_transfer_hardware)(struct spi_master * ) ;
   int (*transfer_one_message)(struct spi_master * , struct spi_message * ) ;
   int (*unprepare_transfer_hardware)(struct spi_master * ) ;
   int *cs_gpios ;
};

struct spi_transfer {
   void const *tx_buf ;
   void *rx_buf ;
   unsigned int len ;
   dma_addr_t tx_dma ;
   dma_addr_t rx_dma ;
   unsigned int cs_change : 1 ;
   u8 tx_nbits ;
   u8 rx_nbits ;
   u8 bits_per_word ;
   u16 delay_usecs ;
   u32 speed_hz ;
   struct list_head transfer_list ;
};

struct spi_message {
   struct list_head transfers ;
   struct spi_device *spi ;
   unsigned int is_dma_mapped : 1 ;
   void (*complete)(void * ) ;
   void *context ;
   unsigned int frame_length ;
   unsigned int actual_length ;
   int status ;
   struct list_head queue ;
   void *state ;
};

struct spi_bitbang {
   spinlock_t lock ;
   u8 busy ;
   u8 use_dma ;
   u8 flags ;
   struct spi_master *master ;
   int (*setup_transfer)(struct spi_device * , struct spi_transfer * ) ;
   void (*chipselect)(struct spi_device * , int ) ;
   int (*txrx_bufs)(struct spi_device * , struct spi_transfer * ) ;
   u32 (*txrx_word[4U])(struct spi_device * , unsigned int , u32 , u8 ) ;
};

enum of_gpio_flags {
    OF_GPIO_ACTIVE_LOW = 1
};

struct spi_imx_master {
   int *chipselect ;
   int num_chipselect ;
};

struct spi_imx_config {
   unsigned int speed_hz ;
   unsigned int bpw ;
   unsigned int mode ;
   u8 cs ;
};

enum spi_imx_devtype {
    IMX1_CSPI = 0,
    IMX21_CSPI = 1,
    IMX27_CSPI = 2,
    IMX31_CSPI = 3,
    IMX35_CSPI = 4,
    IMX51_ECSPI = 5
};

struct spi_imx_data;

struct spi_imx_devtype_data {
   void (*intctrl)(struct spi_imx_data * , int ) ;
   int (*config)(struct spi_imx_data * , struct spi_imx_config * ) ;
   void (*trigger)(struct spi_imx_data * ) ;
   int (*rx_available)(struct spi_imx_data * ) ;
   void (*reset)(struct spi_imx_data * ) ;
   enum spi_imx_devtype devtype ;
};

struct spi_imx_data {
   struct spi_bitbang bitbang ;
   struct completion xfer_done ;
   void *base ;
   int irq ;
   struct clk *clk_per ;
   struct clk *clk_ipg ;
   unsigned long spi_clk ;
   unsigned int count ;
   void (*tx)(struct spi_imx_data * ) ;
   void (*rx)(struct spi_imx_data * ) ;
   void *rx_buf ;
   void const *tx_buf ;
   unsigned int txfifo ;
   struct spi_imx_devtype_data const *devtype_data ;
   int chipselect[0U] ;
};

struct ldv_struct_platform_instance_2 {
   struct platform_driver *arg0 ;
   int signal_pending ;
};

struct device_private {
   void *driver_data ;
};

enum hrtimer_restart;

extern long __builtin_expect(long exp, long c);
extern void *ldv_dev_get_drvdata(struct device const *dev);
extern int ldv_dev_set_drvdata(struct device *dev, void *data);
extern struct spi_master *ldv_spi_alloc_master(struct device *host, unsigned int size);
extern long ldv_is_err(void const *ptr);
extern long ldv_ptr_err(void const *ptr);
extern void ldv_clk_disable_clk_ipg_of_spi_imx_data(struct clk *clk);
extern int ldv_clk_enable_clk_ipg_of_spi_imx_data(void);
extern void ldv_clk_disable_clk_per_of_spi_imx_data(struct clk *clk);
extern int ldv_clk_enable_clk_per_of_spi_imx_data(void);
extern void ldv_initialize(void);
extern void ldv_check_final_state(void);
extern int ldv_post_init(int init_ret_val);
extern void ldv_pre_probe(void);
extern int ldv_post_probe(int probe_ret_val);
extern void __VERIFIER_assume(int);
extern int ldv_undef_int(void);
extern int ldv_undef_int_negative(void);
extern void ldv_free(void *s);
extern void *ldv_xmalloc(size_t size);
extern struct module __this_module;

__inline static int fls(int x);

extern int printk(char const *, ...);
extern int __dynamic_pr_debug(struct _ddebug *, char const *, ...);
extern int __dynamic_dev_dbg(struct _ddebug *, struct device const *, char const *, ...);
__inline static long PTR_ERR(void const *ptr);
__inline static long IS_ERR(void const *ptr);
extern void __init_waitqueue_head(wait_queue_head_t *, char const *, struct lock_class_key *);

__inline static void init_completion(struct completion *x);

extern void wait_for_completion(struct completion *);
extern void complete(struct completion *);
extern void *devm_ioremap_resource(struct device *, struct resource *);
static void *ldv_dev_get_drvdata_5(struct device const *dev);
static void *ldv_dev_get_drvdata_15(struct device const *dev);
static int ldv_dev_set_drvdata_6(struct device *dev, void *data);

__inline static void *dev_get_platdata(struct device const *dev);

extern struct device *get_device(struct device *);
extern void put_device(struct device *);
extern int dev_err(struct device const *, char const *, ...);
extern int _dev_info(struct device const *, char const *, ...);
extern struct resource *platform_get_resource(struct platform_device *, unsigned int, unsigned int);
extern int platform_get_irq(struct platform_device *, unsigned int);
static int ldv___platform_driver_register_24(struct platform_driver *ldv_func_arg1, struct module *ldv_func_arg2);
static void ldv_platform_driver_unregister_25(struct platform_driver *ldv_func_arg1);

__inline static void *platform_get_drvdata(struct platform_device const *pdev);
__inline static void platform_set_drvdata(struct platform_device *pdev, void *data);

extern struct clk *devm_clk_get(struct device *, char const *);
extern unsigned long clk_get_rate(struct clk *);
__inline static int ldv_clk_prepare_enable_18(struct clk *clk);
__inline static int ldv_clk_prepare_enable_19(struct clk *clk);
__inline static void ldv_clk_disable_unprepare_20(struct clk *clk);
__inline static void ldv_clk_disable_unprepare_21(struct clk *clk);

__inline static unsigned int readl(void const volatile *addr);
__inline static void writel(unsigned int val, void volatile *addr);
__inline static int of_property_read_u32_array(struct device_node const *np, char const *propname, u32 *out_values, size_t sz);
__inline static int of_property_read_u32(struct device_node const *np, char const *propname, u32 *out_value);
__inline static bool gpio_is_valid(int number);

extern int gpio_direction_output(unsigned int, int);
extern void __gpio_set_value(unsigned int, int);

__inline static void gpio_set_value(unsigned int gpio, int value);

extern int devm_gpio_request(struct device *, unsigned int, char const *);
extern int devm_request_threaded_irq(struct device *, unsigned int, irqreturn_t (*)(int, void *), irqreturn_t (*)(int, void *), unsigned long, char const *, void *);

__inline static int devm_request_irq(struct device *dev, unsigned int irq, irqreturn_t (*handler)(int, void *), unsigned long irqflags, char const *devname, void *dev_id);
__inline static void *spi_master_get_devdata(struct spi_master *master);
__inline static struct spi_master *spi_master_get(struct spi_master *master);
__inline static void spi_master_put(struct spi_master *master);

static struct spi_master *ldv_spi_alloc_master_17(struct device *host, unsigned int size);
extern int spi_bitbang_start(struct spi_bitbang *);
extern int spi_bitbang_stop(struct spi_bitbang *);

__inline static struct of_device_id const *of_match_device(struct of_device_id const *matches, struct device const *dev);
__inline static int of_get_named_gpio_flags(struct device_node *np, char const *list_name, int index, enum of_gpio_flags *flags);
__inline static int of_get_named_gpio(struct device_node *np, char const *propname, int index);
__inline static int is_imx27_cspi(struct spi_imx_data *d);
__inline static int is_imx35_cspi(struct spi_imx_data *d);
__inline static unsigned int spi_imx_get_fifosize(struct spi_imx_data *d);
static void spi_imx_buf_rx_u8(struct spi_imx_data *spi_imx);
static void spi_imx_buf_tx_u8(struct spi_imx_data *spi_imx);
static void spi_imx_buf_rx_u16(struct spi_imx_data *spi_imx);
static void spi_imx_buf_tx_u16(struct spi_imx_data *spi_imx);
static void spi_imx_buf_rx_u32(struct spi_imx_data *spi_imx);
static void spi_imx_buf_tx_u32(struct spi_imx_data *spi_imx);

static int mxc_clkdivs[19U];

static unsigned int spi_imx_clkdiv_1(unsigned int fin, unsigned int fspi, unsigned int max);
static unsigned int spi_imx_clkdiv_2(unsigned int fin, unsigned int fspi);
static unsigned int mx51_ecspi_clkdiv(unsigned int fin, unsigned int fspi);
static void mx51_ecspi_intctrl(struct spi_imx_data *spi_imx, int enable);
static void mx51_ecspi_trigger(struct spi_imx_data *spi_imx);
static int mx51_ecspi_config(struct spi_imx_data *spi_imx, struct spi_imx_config *config);
static int mx51_ecspi_rx_available(struct spi_imx_data *spi_imx);
static void mx51_ecspi_reset(struct spi_imx_data *spi_imx);
static void mx31_intctrl(struct spi_imx_data *spi_imx, int enable);
static void mx31_trigger(struct spi_imx_data *spi_imx);
static int mx31_config(struct spi_imx_data *spi_imx, struct spi_imx_config *config);
static int mx31_rx_available(struct spi_imx_data *spi_imx);
static void mx31_reset(struct spi_imx_data *spi_imx);
static void mx21_intctrl(struct spi_imx_data *spi_imx, int enable);
static void mx21_trigger(struct spi_imx_data *spi_imx);
static int mx21_config(struct spi_imx_data *spi_imx, struct spi_imx_config *config);
static int mx21_rx_available(struct spi_imx_data *spi_imx);
static void mx21_reset(struct spi_imx_data *spi_imx);
static void mx1_intctrl(struct spi_imx_data *spi_imx, int enable);
static void mx1_trigger(struct spi_imx_data *spi_imx);
static int mx1_config(struct spi_imx_data *spi_imx, struct spi_imx_config *config);
static int mx1_rx_available(struct spi_imx_data *spi_imx);
static void mx1_reset(struct spi_imx_data *spi_imx);

static struct spi_imx_devtype_data imx1_cspi_devtype_data;
static struct spi_imx_devtype_data imx21_cspi_devtype_data;
static struct spi_imx_devtype_data imx27_cspi_devtype_data;
static struct spi_imx_devtype_data imx31_cspi_devtype_data;
static struct spi_imx_devtype_data imx35_cspi_devtype_data;
static struct spi_imx_devtype_data imx51_ecspi_devtype_data;
static struct platform_device_id spi_imx_devtype[7U];
static struct of_device_id const spi_imx_dt_ids[7U];
extern struct of_device_id const __mod_of_device_table;

static void spi_imx_chipselect(struct spi_device *spi, int is_active);
static void spi_imx_push(struct spi_imx_data *spi_imx);
static irqreturn_t spi_imx_isr(int irq, void *dev_id);
static int spi_imx_setupxfer(struct spi_device *spi, struct spi_transfer *t);
static int spi_imx_transfer(struct spi_device *spi, struct spi_transfer *transfer);
static int spi_imx_setup(struct spi_device *spi);
static void spi_imx_cleanup(struct spi_device *spi);
static int spi_imx_probe(struct platform_device *pdev);
static int spi_imx_remove(struct platform_device *pdev);

static struct platform_driver spi_imx_driver;

static int spi_imx_driver_init(void);
static void spi_imx_driver_exit(void);

extern void ldv_dispatch_deregister_6_1(struct platform_driver *arg0);
extern void ldv_dispatch_insmod_deregister_7_1(void);
extern void ldv_dispatch_insmod_register_7_2(void);
extern void ldv_dispatch_pm_deregister_2_5(void);
extern void ldv_dispatch_pm_register_2_6(void);
extern void ldv_dispatch_register_5_3(struct platform_driver *arg0);
extern int ldv_emg___platform_driver_register(struct platform_driver *arg0, struct module *arg1);
extern void ldv_emg_platform_driver_unregister(struct platform_driver *arg0);
extern void ldv_insmod_4(void *arg0);
extern void ldv_insmod_spi_imx_driver_exit_4_2(void (*arg0)(void));
extern int ldv_insmod_spi_imx_driver_init_4_6(int (*arg0)(void));
extern void ldv_main_7(void *arg0);
extern void ldv_platform_instance_2(void *arg0);
extern int ldv_platform_instance_probe_2_14(int (*arg0)(struct platform_device *), struct platform_device *arg1);
extern void ldv_platform_instance_release_2_3(int (*arg0)(struct platform_device *), struct platform_device *arg1);
extern void ldv_pm_ops_scenario_3(void *arg0);
extern void ldv_pm_ops_scenario_complete_3_3(void (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_freeze_3_15(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_freeze_late_3_14(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_freeze_noirq_3_12(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_poweroff_3_9(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_poweroff_late_3_8(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_poweroff_noirq_3_6(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_prepare_3_22(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_restore_3_4(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_restore_early_3_7(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_restore_noirq_3_5(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_resume_3_16(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_resume_early_3_17(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_resume_noirq_3_19(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_runtime_idle_3_27(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_runtime_resume_3_24(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_runtime_suspend_3_25(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_suspend_3_21(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_suspend_late_3_18(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_suspend_noirq_3_20(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_thaw_3_10(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_thaw_early_3_13(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_thaw_noirq_3_11(int (*arg0)(struct device *), struct device *arg1);
extern int main(void);

extern void ldv_dispatch_deregister_6_1(struct platform_driver *arg0);
extern void ldv_dispatch_insmod_deregister_7_1(void);
extern void ldv_dispatch_insmod_register_7_2(void);
extern void ldv_dispatch_pm_deregister_2_5(void);
extern void ldv_dispatch_pm_register_2_6(void);
extern void ldv_dispatch_register_5_3(struct platform_driver *arg0);
extern int ldv_emg___platform_driver_register(struct platform_driver *arg0, struct module *arg1);
extern void ldv_emg_platform_driver_unregister(struct platform_driver *arg0);
extern void ldv_insmod_4(void *arg0);
extern void ldv_insmod_spi_imx_driver_exit_4_2(void (*arg0)(void));
extern int ldv_insmod_spi_imx_driver_init_4_6(int (*arg0)(void));
extern void ldv_main_7(void *arg0);
extern void ldv_platform_instance_2(void *arg0);
extern int ldv_platform_instance_probe_2_14(int (*arg0)(struct platform_device *), struct platform_device *arg1);
extern void ldv_platform_instance_release_2_3(int (*arg0)(struct platform_device *), struct platform_device *arg1);
extern void ldv_pm_ops_scenario_3(void *arg0);
extern void ldv_pm_ops_scenario_complete_3_3(void (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_freeze_3_15(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_freeze_late_3_14(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_freeze_noirq_3_12(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_poweroff_3_9(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_poweroff_late_3_8(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_poweroff_noirq_3_6(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_prepare_3_22(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_restore_3_4(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_restore_early_3_7(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_restore_noirq_3_5(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_resume_3_16(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_resume_early_3_17(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_resume_noirq_3_19(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_runtime_idle_3_27(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_runtime_resume_3_24(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_runtime_suspend_3_25(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_suspend_3_21(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_suspend_late_3_18(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_suspend_noirq_3_20(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_thaw_3_10(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_thaw_early_3_13(int (*arg0)(struct device *), struct device *arg1);
extern void ldv_pm_ops_scenario_thaw_noirq_3_11(int (*arg0)(struct device *), struct device *arg1);
extern int main(void);
__inline static long PTR_ERR(void const *ptr);
__inline static long IS_ERR(void const *ptr);
static void *ldv_dev_get_drvdata_5(struct device const *dev);
static int ldv_dev_set_drvdata_6(struct device *dev, void *data);
static void *ldv_dev_get_drvdata_15(struct device const *dev);
static struct spi_master *ldv_spi_alloc_master_17(struct device *host, unsigned int size);
__inline static int ldv_clk_prepare_enable_18(struct clk *clk);
__inline static int ldv_clk_prepare_enable_19(struct clk *clk);
__inline static void ldv_clk_disable_unprepare_20(struct clk *clk);
__inline static void ldv_clk_disable_unprepare_21(struct clk *clk);
static int ldv___platform_driver_register_24(struct platform_driver *ldv_func_arg1, struct module *ldv_func_arg2);
static void ldv_platform_driver_unregister_25(struct platform_driver *ldv_func_arg1);

extern void *ldv_xzalloc(size_t size);

extern void *ldv_dev_get_drvdata(struct device const *dev);
extern int ldv_dev_set_drvdata(struct device *dev, void *data);

extern void *ldv_zalloc(size_t size);

extern struct spi_master *ldv_spi_alloc_master(struct device *host, unsigned int size);

extern long ldv_is_err_or_null(void const *ptr);
extern void *ldv_err_ptr(long error);

extern long ldv_is_err(void const *ptr);
extern void *ldv_err_ptr(long error);
extern long ldv_ptr_err(void const *ptr);
extern long ldv_is_err_or_null(void const *ptr);

extern void ldv_switch_to_interrupt_context(void);
extern void ldv_switch_to_process_context(void);
extern bool ldv_in_interrupt_context(void);
extern int ldv_filter_err_code(int ret_val);
static bool __ldv_in_interrupt_context;

extern void ldv_switch_to_interrupt_context(void);
extern void ldv_switch_to_process_context(void);
extern bool ldv_in_interrupt_context(void);
static int ldv_filter_positive_int(int val);
extern int ldv_post_init(int init_ret_val);
extern int ldv_post_probe(int probe_ret_val);
extern int ldv_filter_err_code(int ret_val);

extern void *ldv_kzalloc(size_t size, gfp_t flags);
extern void *ldv_kmalloc(size_t size, gfp_t flags);
extern void *ldv_kcalloc(size_t n, size_t size, gfp_t flags);
extern void *ldv_kmalloc_array(size_t n, size_t size, gfp_t flags);
extern void ldv_check_alloc_flags(gfp_t);
extern void ldv_after_alloc(void *);
extern void *ldv_malloc(size_t size);
extern void *ldv_calloc(size_t nmemb, size_t size);

extern void *ldv_kmalloc(size_t size, gfp_t flags);
extern void *ldv_kcalloc(size_t n, size_t size, gfp_t flags);
extern void *ldv_kzalloc(size_t size, gfp_t flags);
extern void *ldv_kmalloc_array(size_t n, size_t size, gfp_t flags);

extern void __VERIFIER_error(void);

extern long __builtin_expect(long exp, long c);
extern void __builtin_trap(void);

extern void *external_allocated_data(void);
extern void *ldv_malloc_unknown_size(void);
extern void *ldv_calloc_unknown_size(void);
extern void *ldv_zalloc_unknown_size(void);
extern void *ldv_xmalloc_unknown_size(size_t size);
extern void *malloc(size_t);
extern void *calloc(size_t, size_t);
extern void free(void *);
extern void *memset(void *, int, size_t);

extern void *ldv_malloc(size_t size);
extern void *ldv_calloc(size_t nmemb, size_t size);
extern void *ldv_zalloc(size_t size);
extern void ldv_free(void *s);
extern void *ldv_xmalloc(size_t size);
extern void *ldv_xzalloc(size_t size);
extern void *ldv_malloc_unknown_size(void);
extern void *ldv_calloc_unknown_size(void);
extern void *ldv_zalloc_unknown_size(void);
extern void *ldv_xmalloc_unknown_size(size_t size);

extern int ldv_undef_long(void);
extern unsigned int ldv_undef_uint(void);
extern unsigned long ldv_undef_ulong(void);
extern unsigned long long ldv_undef_ulonglong(void);
extern void *ldv_undef_ptr(void);
extern int ldv_undef_int_positive(void);
extern int ldv_undef_int_nonpositive(void);
extern void *ldv_undef_ptr_non_null(void);
extern int __VERIFIER_nondet_int(void);
extern long __VERIFIER_nondet_long(void);
extern unsigned int __VERIFIER_nondet_uint(void);
extern unsigned long __VERIFIER_nondet_ulong(void);
extern unsigned long long __VERIFIER_nondet_ulonglong(void);
extern void *__VERIFIER_nondet_pointer(void);

extern int ldv_undef_int(void);
extern int ldv_undef_long(void);
extern unsigned int ldv_undef_uint(void);
extern void *ldv_undef_ptr(void);
extern unsigned long ldv_undef_ulong(void);
extern unsigned long long ldv_undef_ulonglong(void);
extern int ldv_undef_int_positive(void);
extern int ldv_undef_int_negative(void);
extern int ldv_undef_int_nonpositive(void);
extern void *ldv_undef_ptr_non_null(void);

extern void ldv_assert_linux_drivers_clk1__more_at_exit(int expr);
extern int ldv_counter_clk;

extern void ldv_clk_disable_clk(struct clk *clk);
extern int ldv_clk_enable_clk(void);

extern int ldv_counter_clk_ipg_of_spi_imx_data;

extern void ldv_clk_disable_clk_ipg_of_spi_imx_data(struct clk *clk);
extern int ldv_clk_enable_clk_ipg_of_spi_imx_data(void);

extern int ldv_counter_clk_per_of_spi_imx_data;

extern void ldv_clk_disable_clk_per_of_spi_imx_data(struct clk *clk);
extern int ldv_clk_enable_clk_per_of_spi_imx_data(void);
extern void ldv_check_final_state(void);
extern void ldv_assert_linux_drivers_clk1__more_at_exit(int expr);
# 2 "/home/sruland/git/c-case-study-preperation/MutationRevisionGenerator/output/revisions/linux-drivers-clk1__drivers---spi---spi-imx.ko.d8e172f3c0a5.cil_mx31_config/reemergingBugs/linux-drivers-clk1__drivers---spi---spi-imx.ko.d8e172f3c0a5.cil_mx31_config_revision_30.creemerging_20.c" 2
extern void __VERIFIER_error() __attribute__ ((__noreturn__));
static int mx31_config_rev_30 ( struct spi_imx_data * spi_imx , struct spi_imx_config * config )
{
unsigned int reg ;
int cs ;
unsigned int tmp ;
int tmp___0 ;
int tmp___1 ;
{
{
Revision_30:Revision_29:reg = - 2 ;
cs = spi_imx -> chipselect [ ( int ) config -> cs ] ;
tmp = spi_imx_clkdiv_2 ( ( unsigned int ) spi_imx -> spi_clk , config -> speed_hz ) ;
Revision_27:Revision_15:reg = reg | ( tmp << 16 ) ;
tmp___0 = is_imx35_cspi ( spi_imx ) ;
}
Revision_25:Revision_16:Revision_5:if ( - abs ( tmp___0 ) == 1 ) {
Revision_14:reg = reg | ( ( config -> bpw % 1U ) << 20 ) ;
Revision_24:Revision_19:Revision_6:Revision_4:reg = reg | - - 1 ;
}
else {
Revision_23:Revision_22:Revision_11:Revision_1:reg = reg | ( ( config -> bpw - - - 1 ) << 1 ) ;
}

if ( ( int ) config -> mode & 1 ) {
Revision_9:Revision_2:reg = reg | - 302 ;
}
else {
}

Revision_21:Revision_18:Revision_13:Revision_3:if ( ( config -> mode & 1 ) <= 1 ) {
reg = reg | 16U ;
}
else {
}

Revision_8:Revision_7:if ( ( config -> mode & 4 ) != 0U ) {
reg = reg | 128U ;
}
else {
}

Revision_28:Revision_12:if ( - abs ( - abs ( cs ) ) < 0 ) {
{
tmp___1 = is_imx35_cspi ( spi_imx ) ;
Revision_26:Revision_17:Revision_10:reg = reg | ( unsigned int ) ( ( cs + 31 ) << ( tmp___1 < 1 ? 12 : 24 ) ) ;
}
}
else {
}

{
writel ( reg , ( void volatile * ) ( spi_imx -> base + 8UL ) ) ;
}
return ( 0 ) ;
}
}
int main(){

struct spi_imx_data temp_spi_imx;


temp_spi_imx.bitbang.busy = __Verifier_nondet_uchar();

temp_spi_imx.bitbang.use_dma = __Verifier_nondet_uchar();

temp_spi_imx.bitbang.flags = __Verifier_nondet_uchar();




temp_spi_imx.xfer_done.done = __Verifier_nondet_uint();





temp_spi_imx.irq = __Verifier_nondet_int();

temp_spi_imx.spi_clk = __Verifier_nondet_ulong();

temp_spi_imx.count = __Verifier_nondet_uint();

temp_spi_imx.txfifo = __Verifier_nondet_uint();



struct spi_imx_data *spi_imx = &temp_spi_imx;

struct spi_imx_config temp_config;
temp_config.speed_hz = __Verifier_nondet_uint();

temp_config.bpw = __Verifier_nondet_uint();

temp_config.mode = __Verifier_nondet_uint();

temp_config.cs = __Verifier_nondet_uchar();


struct spi_imx_config *config = &temp_config;


int mx31_config_result = mx31_config_rev_30(spi_imx,config);
}
