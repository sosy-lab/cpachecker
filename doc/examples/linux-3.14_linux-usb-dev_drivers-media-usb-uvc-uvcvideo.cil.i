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
typedef __kernel_long_t __kernel_suseconds_t;
typedef unsigned int __kernel_uid32_t;
typedef unsigned int __kernel_gid32_t;
typedef __kernel_ulong_t __kernel_size_t;
typedef __kernel_long_t __kernel_ssize_t;
typedef long long __kernel_loff_t;
typedef __kernel_long_t __kernel_time_t;
typedef __kernel_long_t __kernel_clock_t;
typedef int __kernel_timer_t;
typedef int __kernel_clockid_t;
typedef __u16 __le16;
typedef __u32 __le32;
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
typedef u64 dma_addr_t;
typedef unsigned int gfp_t;
typedef unsigned int fmode_t;
typedef unsigned int oom_flags_t;
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
struct device;
struct usb_device;
struct ldv_thread;
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
struct seq_file;
struct thread_struct;
struct mm_struct;
struct task_struct;
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
struct __anonstruct____missing_field_name_17 {
   u32 read ;
   s32 write ;
};
union __anonunion_arch_rwlock_t_16 {
   s64 lock ;
   struct __anonstruct____missing_field_name_17 __annonCompField8 ;
};
typedef union __anonunion_arch_rwlock_t_16 arch_rwlock_t;
typedef void (*ctor_fn_t)(void);
struct file_operations;
struct completion;
struct pid;
struct lockdep_map;
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
   unsigned long bits[128U] ;
};
typedef struct cpumask cpumask_t;
typedef struct cpumask *cpumask_var_t;
struct static_key;
struct seq_operations;
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
struct lwp_struct {
   u8 reserved[128U] ;
};
struct bndregs_struct {
   u64 bndregs[8U] ;
};
struct bndcsr_struct {
   u64 cfg_reg_u ;
   u64 status_reg ;
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
   struct lwp_struct lwp ;
   struct bndregs_struct bndregs ;
   struct bndcsr_struct bndcsr ;
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
   unsigned char fpu_counter ;
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
struct raw_spinlock {
   arch_spinlock_t raw_lock ;
   unsigned int magic ;
   unsigned int owner_cpu ;
   void *owner ;
   struct lockdep_map dep_map ;
};
typedef struct raw_spinlock raw_spinlock_t;
struct __anonstruct____missing_field_name_29 {
   u8 __padding[24U] ;
   struct lockdep_map dep_map ;
};
union __anonunion____missing_field_name_28 {
   struct raw_spinlock rlock ;
   struct __anonstruct____missing_field_name_29 __annonCompField18 ;
};
struct spinlock {
   union __anonunion____missing_field_name_28 __annonCompField19 ;
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
   struct lockdep_map dep_map ;
};
typedef struct seqcount seqcount_t;
struct __wait_queue_head {
   spinlock_t lock ;
   struct list_head task_list ;
};
typedef struct __wait_queue_head wait_queue_head_t;
struct completion {
   unsigned int done ;
   wait_queue_head_t wait ;
};
struct idr_layer {
   int prefix ;
   unsigned long bitmap[4U] ;
   struct idr_layer *ary[256U] ;
   int count ;
   int layer ;
   struct callback_head callback_head ;
};
struct idr {
   struct idr_layer *hint ;
   struct idr_layer *top ;
   struct idr_layer *id_free ;
   int layers ;
   int id_free_cnt ;
   int cur ;
   spinlock_t lock ;
};
struct ida_bitmap {
   long nr_busy ;
   unsigned long bitmap[15U] ;
};
struct ida {
   struct idr idr ;
   struct ida_bitmap *free_bitmap ;
};
struct rb_node {
   unsigned long __rb_parent_color ;
   struct rb_node *rb_right ;
   struct rb_node *rb_left ;
};
struct rb_root {
   struct rb_node *rb_node ;
};
struct dentry;
struct iattr;
struct vm_area_struct;
struct super_block;
struct file_system_type;
struct kernfs_open_node;
struct kernfs_iattrs;
struct kernfs_root;
struct kernfs_elem_dir {
   unsigned long subdirs ;
   struct rb_root children ;
   struct kernfs_root *root ;
};
struct kernfs_node;
struct kernfs_elem_symlink {
   struct kernfs_node *target_kn ;
};
struct kernfs_ops;
struct kernfs_elem_attr {
   struct kernfs_ops const *ops ;
   struct kernfs_open_node *open ;
   loff_t size ;
};
union __anonunion_u_36 {
   struct completion *completion ;
   struct kernfs_node *removed_list ;
};
union __anonunion____missing_field_name_37 {
   struct kernfs_elem_dir dir ;
   struct kernfs_elem_symlink symlink ;
   struct kernfs_elem_attr attr ;
};
struct kernfs_node {
   atomic_t count ;
   atomic_t active ;
   struct lockdep_map dep_map ;
   struct kernfs_node *parent ;
   char const *name ;
   struct rb_node rb ;
   union __anonunion_u_36 u ;
   void const *ns ;
   unsigned int hash ;
   union __anonunion____missing_field_name_37 __annonCompField21 ;
   void *priv ;
   unsigned short flags ;
   umode_t mode ;
   unsigned int ino ;
   struct kernfs_iattrs *iattr ;
};
struct kernfs_dir_ops {
   int (*mkdir)(struct kernfs_node * , char const * , umode_t ) ;
   int (*rmdir)(struct kernfs_node * ) ;
   int (*rename)(struct kernfs_node * , struct kernfs_node * , char const * ) ;
};
struct kernfs_root {
   struct kernfs_node *kn ;
   struct ida ino_ida ;
   struct kernfs_dir_ops *dir_ops ;
};
struct vm_operations_struct;
struct kernfs_open_file {
   struct kernfs_node *kn ;
   struct file *file ;
   struct mutex mutex ;
   int event ;
   struct list_head list ;
   bool mmapped ;
   struct vm_operations_struct const *vm_ops ;
};
struct kernfs_ops {
   int (*seq_show)(struct seq_file * , void * ) ;
   void *(*seq_start)(struct seq_file * , loff_t * ) ;
   void *(*seq_next)(struct seq_file * , void * , loff_t * ) ;
   void (*seq_stop)(struct seq_file * , void * ) ;
   ssize_t (*read)(struct kernfs_open_file * , char * , size_t , loff_t ) ;
   ssize_t (*write)(struct kernfs_open_file * , char * , size_t , loff_t ) ;
   int (*mmap)(struct kernfs_open_file * , struct vm_area_struct * ) ;
   struct lock_class_key lockdep_key ;
};
struct sock;
struct kobject;
enum kobj_ns_type {
    KOBJ_NS_TYPE_NONE = 0,
    KOBJ_NS_TYPE_NET = 1,
    KOBJ_NS_TYPES = 2
} ;
struct kobj_ns_type_operations {
   enum kobj_ns_type type ;
   bool (*current_may_mount)(void) ;
   void *(*grab_current_ns)(void) ;
   void const *(*netlink_ns)(struct sock * ) ;
   void const *(*initial_ns)(void) ;
   void (*drop_ns)(void * ) ;
};
struct timespec {
   __kernel_time_t tv_sec ;
   long tv_nsec ;
};
struct timeval {
   __kernel_time_t tv_sec ;
   __kernel_suseconds_t tv_usec ;
};
struct user_namespace;
struct __anonstruct_kuid_t_38 {
   uid_t val ;
};
typedef struct __anonstruct_kuid_t_38 kuid_t;
struct __anonstruct_kgid_t_39 {
   gid_t val ;
};
typedef struct __anonstruct_kgid_t_39 kgid_t;
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
};
struct kref {
   atomic_t refcount ;
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
   struct kernfs_node *sd ;
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
struct inode;
struct cdev {
   struct kobject kobj ;
   struct module *owner ;
   struct file_operations const *ops ;
   struct list_head list ;
   dev_t dev ;
   unsigned int count ;
};
struct backing_dev_info;
typedef unsigned long kernel_ulong_t;
struct usb_device_id {
   __u16 match_flags ;
   __u16 idVendor ;
   __u16 idProduct ;
   __u16 bcdDevice_lo ;
   __u16 bcdDevice_hi ;
   __u8 bDeviceClass ;
   __u8 bDeviceSubClass ;
   __u8 bDeviceProtocol ;
   __u8 bInterfaceClass ;
   __u8 bInterfaceSubClass ;
   __u8 bInterfaceProtocol ;
   __u8 bInterfaceNumber ;
   kernel_ulong_t driver_info ;
};
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
struct usb_device_descriptor {
   __u8 bLength ;
   __u8 bDescriptorType ;
   __le16 bcdUSB ;
   __u8 bDeviceClass ;
   __u8 bDeviceSubClass ;
   __u8 bDeviceProtocol ;
   __u8 bMaxPacketSize0 ;
   __le16 idVendor ;
   __le16 idProduct ;
   __le16 bcdDevice ;
   __u8 iManufacturer ;
   __u8 iProduct ;
   __u8 iSerialNumber ;
   __u8 bNumConfigurations ;
};
struct usb_config_descriptor {
   __u8 bLength ;
   __u8 bDescriptorType ;
   __le16 wTotalLength ;
   __u8 bNumInterfaces ;
   __u8 bConfigurationValue ;
   __u8 iConfiguration ;
   __u8 bmAttributes ;
   __u8 bMaxPower ;
};
struct usb_interface_descriptor {
   __u8 bLength ;
   __u8 bDescriptorType ;
   __u8 bInterfaceNumber ;
   __u8 bAlternateSetting ;
   __u8 bNumEndpoints ;
   __u8 bInterfaceClass ;
   __u8 bInterfaceSubClass ;
   __u8 bInterfaceProtocol ;
   __u8 iInterface ;
};
struct usb_endpoint_descriptor {
   __u8 bLength ;
   __u8 bDescriptorType ;
   __u8 bEndpointAddress ;
   __u8 bmAttributes ;
   __le16 wMaxPacketSize ;
   __u8 bInterval ;
   __u8 bRefresh ;
   __u8 bSynchAddress ;
};
struct usb_ss_ep_comp_descriptor {
   __u8 bLength ;
   __u8 bDescriptorType ;
   __u8 bMaxBurst ;
   __u8 bmAttributes ;
   __le16 wBytesPerInterval ;
};
struct usb_interface_assoc_descriptor {
   __u8 bLength ;
   __u8 bDescriptorType ;
   __u8 bFirstInterface ;
   __u8 bInterfaceCount ;
   __u8 bFunctionClass ;
   __u8 bFunctionSubClass ;
   __u8 bFunctionProtocol ;
   __u8 iFunction ;
};
struct usb_bos_descriptor {
   __u8 bLength ;
   __u8 bDescriptorType ;
   __le16 wTotalLength ;
   __u8 bNumDeviceCaps ;
};
struct usb_ext_cap_descriptor {
   __u8 bLength ;
   __u8 bDescriptorType ;
   __u8 bDevCapabilityType ;
   __le32 bmAttributes ;
};
struct usb_ss_cap_descriptor {
   __u8 bLength ;
   __u8 bDescriptorType ;
   __u8 bDevCapabilityType ;
   __u8 bmAttributes ;
   __le16 wSpeedSupported ;
   __u8 bFunctionalitySupport ;
   __u8 bU1devExitLat ;
   __le16 bU2DevExitLat ;
};
struct usb_ss_container_id_descriptor {
   __u8 bLength ;
   __u8 bDescriptorType ;
   __u8 bDevCapabilityType ;
   __u8 bReserved ;
   __u8 ContainerID[16U] ;
};
enum usb_device_speed {
    USB_SPEED_UNKNOWN = 0,
    USB_SPEED_LOW = 1,
    USB_SPEED_FULL = 2,
    USB_SPEED_HIGH = 3,
    USB_SPEED_WIRELESS = 4,
    USB_SPEED_SUPER = 5
} ;
enum usb_device_state {
    USB_STATE_NOTATTACHED = 0,
    USB_STATE_ATTACHED = 1,
    USB_STATE_POWERED = 2,
    USB_STATE_RECONNECTING = 3,
    USB_STATE_UNAUTHENTICATED = 4,
    USB_STATE_DEFAULT = 5,
    USB_STATE_ADDRESS = 6,
    USB_STATE_CONFIGURED = 7,
    USB_STATE_SUSPENDED = 8
} ;
struct llist_node;
struct llist_node {
   struct llist_node *next ;
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
struct __anonstruct_nodemask_t_108 {
   unsigned long bits[16U] ;
};
typedef struct __anonstruct_nodemask_t_108 nodemask_t;
struct __anonstruct_mm_context_t_109 {
   void *ldt ;
   int size ;
   unsigned short ia32_compat ;
   struct mutex lock ;
   void *vdso ;
};
typedef struct __anonstruct_mm_context_t_109 mm_context_t;
struct device_node;
struct rw_semaphore;
struct rw_semaphore {
   long count ;
   raw_spinlock_t wait_lock ;
   struct list_head wait_list ;
   struct lockdep_map dep_map ;
};
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
   struct hrtimer_clock_base clock_base[4U] ;
};
struct klist_node;
struct klist_node {
   void *n_klist ;
   struct list_head n_node ;
   struct kref n_ref ;
};
struct path;
struct seq_file {
   char *buf ;
   size_t size ;
   size_t from ;
   size_t count ;
   size_t pad_until ;
   loff_t index ;
   loff_t read_pos ;
   u64 version ;
   struct mutex lock ;
   struct seq_operations const *op ;
   int poll_event ;
   struct user_namespace *user_ns ;
   void *private ;
};
struct seq_operations {
   void *(*start)(struct seq_file * , loff_t * ) ;
   void (*stop)(struct seq_file * , void * ) ;
   void *(*next)(struct seq_file * , void * , loff_t * ) ;
   int (*show)(struct seq_file * , void * ) ;
};
struct pinctrl;
struct pinctrl_state;
struct dev_pin_info {
   struct pinctrl *p ;
   struct pinctrl_state *default_state ;
   struct pinctrl_state *sleep_state ;
   struct pinctrl_state *idle_state ;
};
struct dma_map_ops;
struct dev_archdata {
   struct dma_map_ops *dma_ops ;
   void *iommu ;
};
struct device_private;
struct device_driver;
struct driver_private;
struct class;
struct subsys_private;
struct bus_type;
struct iommu_ops;
struct iommu_group;
struct device_attribute;
struct bus_type {
   char const *name ;
   char const *dev_name ;
   struct device *dev_root ;
   struct device_attribute *dev_attrs ;
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
struct class_attribute;
struct class {
   char const *name ;
   struct module *owner ;
   struct class_attribute *class_attrs ;
   struct attribute_group const **dev_groups ;
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
struct acpi_device;
struct acpi_dev_node {
   struct acpi_device *companion ;
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
struct hlist_bl_node;
struct hlist_bl_head {
   struct hlist_bl_node *first ;
};
struct hlist_bl_node {
   struct hlist_bl_node *next ;
   struct hlist_bl_node **pprev ;
};
struct __anonstruct____missing_field_name_139 {
   spinlock_t lock ;
   unsigned int count ;
};
union __anonunion____missing_field_name_138 {
   struct __anonstruct____missing_field_name_139 __annonCompField34 ;
};
struct lockref {
   union __anonunion____missing_field_name_138 __annonCompField35 ;
};
struct nameidata;
struct vfsmount;
struct __anonstruct____missing_field_name_141 {
   u32 hash ;
   u32 len ;
};
union __anonunion____missing_field_name_140 {
   struct __anonstruct____missing_field_name_141 __annonCompField36 ;
   u64 hash_len ;
};
struct qstr {
   union __anonunion____missing_field_name_140 __annonCompField37 ;
   unsigned char const *name ;
};
struct dentry_operations;
union __anonunion_d_u_142 {
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
   struct lockref d_lockref ;
   struct dentry_operations const *d_op ;
   struct super_block *d_sb ;
   unsigned long d_time ;
   void *d_fsdata ;
   struct list_head d_lru ;
   union __anonunion_d_u_142 d_u ;
   struct list_head d_subdirs ;
   struct hlist_node d_alias ;
};
struct dentry_operations {
   int (*d_revalidate)(struct dentry * , unsigned int ) ;
   int (*d_weak_revalidate)(struct dentry * , unsigned int ) ;
   int (*d_hash)(struct dentry const * , struct qstr * ) ;
   int (*d_compare)(struct dentry const * , struct dentry const * , unsigned int ,
                    char const * , struct qstr const * ) ;
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
struct list_lru_node {
   spinlock_t lock ;
   struct list_head list ;
   long nr_items ;
};
struct list_lru {
   struct list_lru_node *node ;
   nodemask_t active_nodes ;
};
struct radix_tree_node;
struct radix_tree_root {
   unsigned int height ;
   gfp_t gfp_mask ;
   struct radix_tree_node *rnode ;
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
struct kernel_cap_struct {
   __u32 cap[2U] ;
};
typedef struct kernel_cap_struct kernel_cap_t;
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
   nodemask_t nodes_to_scan ;
   int nid ;
};
struct shrinker {
   unsigned long (*count_objects)(struct shrinker * , struct shrink_control * ) ;
   unsigned long (*scan_objects)(struct shrinker * , struct shrink_control * ) ;
   int seeks ;
   long batch ;
   unsigned long flags ;
   struct list_head list ;
   atomic_long_t *nr_deferred ;
};
enum migrate_mode {
    MIGRATE_ASYNC = 0,
    MIGRATE_SYNC_LIGHT = 1,
    MIGRATE_SYNC = 2
} ;
struct block_device;
struct io_context;
struct export_operations;
struct iovec;
struct kiocb;
struct pipe_inode_info;
struct poll_table_struct;
struct kstatfs;
struct cred;
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
struct percpu_counter {
   raw_spinlock_t lock ;
   s64 count ;
   struct list_head list ;
   s32 *counters ;
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
struct fs_qfilestatv {
   __u64 qfs_ino ;
   __u64 qfs_nblks ;
   __u32 qfs_nextents ;
   __u32 qfs_pad ;
};
struct fs_quota_statv {
   __s8 qs_version ;
   __u8 qs_pad1 ;
   __u16 qs_flags ;
   __u32 qs_incoredqs ;
   struct fs_qfilestatv qs_uquota ;
   struct fs_qfilestatv qs_gquota ;
   struct fs_qfilestatv qs_pquota ;
   __s32 qs_btimelimit ;
   __s32 qs_itimelimit ;
   __s32 qs_rtbtimelimit ;
   __u16 qs_bwarnlimit ;
   __u16 qs_iwarnlimit ;
   __u64 qs_pad2[8U] ;
};
struct dquot;
typedef __kernel_uid32_t projid_t;
struct __anonstruct_kprojid_t_144 {
   projid_t val ;
};
typedef struct __anonstruct_kprojid_t_144 kprojid_t;
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
union __anonunion____missing_field_name_145 {
   kuid_t uid ;
   kgid_t gid ;
   kprojid_t projid ;
};
struct kqid {
   union __anonunion____missing_field_name_145 __annonCompField38 ;
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
   int (*get_xstatev)(struct super_block * , struct fs_quota_statv * ) ;
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
struct address_space;
struct writeback_control;
union __anonunion_arg_147 {
   char *buf ;
   void *data ;
};
struct __anonstruct_read_descriptor_t_146 {
   size_t written ;
   size_t count ;
   union __anonunion_arg_147 arg ;
   int error ;
};
typedef struct __anonstruct_read_descriptor_t_146 read_descriptor_t;
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
   void (*invalidatepage)(struct page * , unsigned int , unsigned int ) ;
   int (*releasepage)(struct page * , gfp_t ) ;
   void (*freepage)(struct page * ) ;
   ssize_t (*direct_IO)(int , struct kiocb * , struct iovec const * , loff_t ,
                        unsigned long ) ;
   int (*get_xip_mem)(struct address_space * , unsigned long , int , void ** , unsigned long * ) ;
   int (*migratepage)(struct address_space * , struct page * , struct page * , enum migrate_mode ) ;
   int (*launder_page)(struct page * ) ;
   int (*is_partially_uptodate)(struct page * , read_descriptor_t * , unsigned long ) ;
   void (*is_dirty_writeback)(struct page * , bool * , bool * ) ;
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
   void *private_data ;
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
union __anonunion____missing_field_name_148 {
   unsigned int const i_nlink ;
   unsigned int __i_nlink ;
};
union __anonunion____missing_field_name_149 {
   struct hlist_head i_dentry ;
   struct callback_head i_rcu ;
};
struct file_lock;
union __anonunion____missing_field_name_150 {
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
   union __anonunion____missing_field_name_148 __annonCompField39 ;
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
   union __anonunion____missing_field_name_149 __annonCompField40 ;
   u64 i_version ;
   atomic_t i_count ;
   atomic_t i_dio_count ;
   atomic_t i_writecount ;
   struct file_operations const *i_fop ;
   struct file_lock *i_flock ;
   struct address_space i_data ;
   struct dquot *i_dquot[2U] ;
   struct list_head i_devices ;
   union __anonunion____missing_field_name_150 __annonCompField41 ;
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
union __anonunion_f_u_151 {
   struct llist_node fu_llist ;
   struct callback_head fu_rcuhead ;
};
struct file {
   union __anonunion_f_u_151 f_u ;
   struct path f_path ;
   struct inode *f_inode ;
   struct file_operations const *f_op ;
   spinlock_t f_lock ;
   atomic_long_t f_count ;
   unsigned int f_flags ;
   fmode_t f_mode ;
   struct mutex f_pos_lock ;
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
struct files_struct;
typedef struct files_struct *fl_owner_t;
struct file_lock_operations {
   void (*fl_copy_lock)(struct file_lock * , struct file_lock * ) ;
   void (*fl_release_private)(struct file_lock * ) ;
};
struct lock_manager_operations {
   int (*lm_compare_owner)(struct file_lock * , struct file_lock * ) ;
   unsigned long (*lm_owner_key)(struct file_lock * ) ;
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
struct __anonstruct_afs_153 {
   struct list_head link ;
   int state ;
};
union __anonunion_fl_u_152 {
   struct nfs_lock_info nfs_fl ;
   struct nfs4_lock_info nfs4_fl ;
   struct __anonstruct_afs_153 afs ;
};
struct file_lock {
   struct file_lock *fl_next ;
   struct hlist_node fl_link ;
   struct list_head fl_block ;
   fl_owner_t fl_owner ;
   unsigned int fl_flags ;
   unsigned char fl_type ;
   unsigned int fl_pid ;
   int fl_link_cpu ;
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
   union __anonunion_fl_u_152 fl_u ;
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
   struct list_head s_mounts ;
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
   struct workqueue_struct *s_dio_done_wq ;
   struct list_lru s_dentry_lru ;
   struct list_lru s_inode_lru ;
   struct callback_head rcu ;
};
struct fiemap_extent_info {
   unsigned int fi_flags ;
   unsigned int fi_extents_mapped ;
   unsigned int fi_extents_max ;
   struct fiemap_extent *fi_extents_start ;
};
struct dir_context {
   int (*actor)(void * , char const * , int , loff_t , u64 , unsigned int ) ;
   loff_t pos ;
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
   int (*iterate)(struct file * , struct dir_context * ) ;
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
   int (*show_fdinfo)(struct seq_file * , struct file * ) ;
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
   int (*tmpfile)(struct inode * , struct dentry * , umode_t ) ;
   int (*set_acl)(struct inode * , struct posix_acl * , int ) ;
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
   long (*nr_cached_objects)(struct super_block * , int ) ;
   long (*free_cached_objects)(struct super_block * , long , int ) ;
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
struct plist_node {
   int prio ;
   struct list_head prio_list ;
   struct list_head node_list ;
};
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
struct __anonstruct____missing_field_name_156 {
   struct arch_uprobe_task autask ;
   unsigned long vaddr ;
};
struct __anonstruct____missing_field_name_157 {
   struct callback_head dup_xol_work ;
   unsigned long dup_xol_addr ;
};
union __anonunion____missing_field_name_155 {
   struct __anonstruct____missing_field_name_156 __annonCompField43 ;
   struct __anonstruct____missing_field_name_157 __annonCompField44 ;
};
struct uprobe;
struct return_instance;
struct uprobe_task {
   enum uprobe_task_state state ;
   union __anonunion____missing_field_name_155 __annonCompField45 ;
   struct uprobe *active_uprobe ;
   unsigned long xol_vaddr ;
   struct return_instance *return_instances ;
   unsigned int depth ;
};
struct xol_area;
struct uprobes_state {
   struct xol_area *xol_area ;
};
union __anonunion____missing_field_name_158 {
   struct address_space *mapping ;
   void *s_mem ;
};
union __anonunion____missing_field_name_160 {
   unsigned long index ;
   void *freelist ;
   bool pfmemalloc ;
};
struct __anonstruct____missing_field_name_164 {
   unsigned int inuse : 16 ;
   unsigned int objects : 15 ;
   unsigned int frozen : 1 ;
};
union __anonunion____missing_field_name_163 {
   atomic_t _mapcount ;
   struct __anonstruct____missing_field_name_164 __annonCompField48 ;
   int units ;
};
struct __anonstruct____missing_field_name_162 {
   union __anonunion____missing_field_name_163 __annonCompField49 ;
   atomic_t _count ;
};
union __anonunion____missing_field_name_161 {
   unsigned long counters ;
   struct __anonstruct____missing_field_name_162 __annonCompField50 ;
   unsigned int active ;
};
struct __anonstruct____missing_field_name_159 {
   union __anonunion____missing_field_name_160 __annonCompField47 ;
   union __anonunion____missing_field_name_161 __annonCompField51 ;
};
struct __anonstruct____missing_field_name_166 {
   struct page *next ;
   int pages ;
   int pobjects ;
};
struct slab;
union __anonunion____missing_field_name_165 {
   struct list_head lru ;
   struct __anonstruct____missing_field_name_166 __annonCompField53 ;
   struct list_head list ;
   struct slab *slab_page ;
   struct callback_head callback_head ;
   pgtable_t pmd_huge_pte ;
};
union __anonunion____missing_field_name_167 {
   unsigned long private ;
   spinlock_t *ptl ;
   struct kmem_cache *slab_cache ;
   struct page *first_page ;
};
struct page {
   unsigned long flags ;
   union __anonunion____missing_field_name_158 __annonCompField46 ;
   struct __anonstruct____missing_field_name_159 __annonCompField52 ;
   union __anonunion____missing_field_name_165 __annonCompField54 ;
   union __anonunion____missing_field_name_167 __annonCompField55 ;
   unsigned long debug_flags ;
};
struct page_frag {
   struct page *page ;
   __u32 offset ;
   __u32 size ;
};
struct __anonstruct_linear_169 {
   struct rb_node rb ;
   unsigned long rb_subtree_last ;
};
union __anonunion_shared_168 {
   struct __anonstruct_linear_169 linear ;
   struct list_head nonlinear ;
};
struct anon_vma;
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
   union __anonunion_shared_168 shared ;
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
struct task_rss_stat {
   int events ;
   int count[3U] ;
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
   atomic_long_t nr_ptes ;
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
   struct cpumask cpumask_allocation ;
   unsigned long numa_next_scan ;
   unsigned long numa_scan_offset ;
   int numa_scan_seq ;
   bool tlb_flush_pending ;
   struct uprobes_state uprobes_state ;
};
typedef unsigned long cputime_t;
struct sem_undo_list;
struct sysv_sem {
   struct sem_undo_list *undo_list ;
};
struct __anonstruct_sigset_t_170 {
   unsigned long sig[1U] ;
};
typedef struct __anonstruct_sigset_t_170 sigset_t;
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
struct __anonstruct__kill_172 {
   __kernel_pid_t _pid ;
   __kernel_uid32_t _uid ;
};
struct __anonstruct__timer_173 {
   __kernel_timer_t _tid ;
   int _overrun ;
   char _pad[0U] ;
   sigval_t _sigval ;
   int _sys_private ;
};
struct __anonstruct__rt_174 {
   __kernel_pid_t _pid ;
   __kernel_uid32_t _uid ;
   sigval_t _sigval ;
};
struct __anonstruct__sigchld_175 {
   __kernel_pid_t _pid ;
   __kernel_uid32_t _uid ;
   int _status ;
   __kernel_clock_t _utime ;
   __kernel_clock_t _stime ;
};
struct __anonstruct__sigfault_176 {
   void *_addr ;
   short _addr_lsb ;
};
struct __anonstruct__sigpoll_177 {
   long _band ;
   int _fd ;
};
struct __anonstruct__sigsys_178 {
   void *_call_addr ;
   int _syscall ;
   unsigned int _arch ;
};
union __anonunion__sifields_171 {
   int _pad[28U] ;
   struct __anonstruct__kill_172 _kill ;
   struct __anonstruct__timer_173 _timer ;
   struct __anonstruct__rt_174 _rt ;
   struct __anonstruct__sigchld_175 _sigchld ;
   struct __anonstruct__sigfault_176 _sigfault ;
   struct __anonstruct__sigpoll_177 _sigpoll ;
   struct __anonstruct__sigsys_178 _sigsys ;
};
struct siginfo {
   int si_signo ;
   int si_errno ;
   int si_code ;
   union __anonunion__sifields_171 _sifields ;
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
struct seccomp_filter;
struct seccomp {
   int mode ;
   struct seccomp_filter *filter ;
};
struct rt_mutex_waiter;
struct rlimit {
   __kernel_ulong_t rlim_cur ;
   __kernel_ulong_t rlim_max ;
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
struct nsproxy;
struct assoc_array_ptr;
struct assoc_array {
   struct assoc_array_ptr *root ;
   unsigned long nr_leaves_on_tree ;
};
typedef int32_t key_serial_t;
typedef uint32_t key_perm_t;
struct key;
struct signal_struct;
struct key_type;
struct keyring_index_key {
   struct key_type *type ;
   char const *description ;
   size_t desc_len ;
};
union __anonunion____missing_field_name_183 {
   struct list_head graveyard_link ;
   struct rb_node serial_node ;
};
struct key_user;
union __anonunion____missing_field_name_184 {
   time_t expiry ;
   time_t revoked_at ;
};
struct __anonstruct____missing_field_name_186 {
   struct key_type *type ;
   char *description ;
};
union __anonunion____missing_field_name_185 {
   struct keyring_index_key index_key ;
   struct __anonstruct____missing_field_name_186 __annonCompField60 ;
};
union __anonunion_type_data_187 {
   struct list_head link ;
   unsigned long x[2U] ;
   void *p[2U] ;
   int reject_error ;
};
union __anonunion_payload_189 {
   unsigned long value ;
   void *rcudata ;
   void *data ;
   void *data2[2U] ;
};
union __anonunion____missing_field_name_188 {
   union __anonunion_payload_189 payload ;
   struct assoc_array keys ;
};
struct key {
   atomic_t usage ;
   key_serial_t serial ;
   union __anonunion____missing_field_name_183 __annonCompField58 ;
   struct rw_semaphore sem ;
   struct key_user *user ;
   void *security ;
   union __anonunion____missing_field_name_184 __annonCompField59 ;
   time_t last_used_at ;
   kuid_t uid ;
   kgid_t gid ;
   key_perm_t perm ;
   unsigned short quotalen ;
   unsigned short datalen ;
   unsigned long flags ;
   union __anonunion____missing_field_name_185 __annonCompField61 ;
   union __anonunion_type_data_187 type_data ;
   union __anonunion____missing_field_name_188 __annonCompField62 ;
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
   struct list_head thread_head ;
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
struct load_weight {
   unsigned long weight ;
   u32 inv_weight ;
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
struct sched_dl_entity {
   struct rb_node rb_node ;
   u64 dl_runtime ;
   u64 dl_deadline ;
   u64 dl_period ;
   u64 dl_bw ;
   s64 runtime ;
   u64 deadline ;
   unsigned int flags ;
   int dl_throttled ;
   int dl_new ;
   int dl_boosted ;
   struct hrtimer dl_timer ;
};
struct mem_cgroup;
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
struct css_set;
struct compat_robust_list_head;
struct numa_group;
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
   int wake_cpu ;
   int on_rq ;
   int prio ;
   int static_prio ;
   int normal_prio ;
   unsigned int rt_priority ;
   struct sched_class const *sched_class ;
   struct sched_entity se ;
   struct sched_rt_entity rt ;
   struct task_group *sched_task_group ;
   struct sched_dl_entity dl ;
   struct hlist_head preempt_notifiers ;
   unsigned int btrace_seq ;
   unsigned int policy ;
   int nr_cpus_allowed ;
   cpumask_t cpus_allowed ;
   struct sched_info sched_info ;
   struct list_head tasks ;
   struct plist_node pushable_tasks ;
   struct rb_node pushable_dl_tasks ;
   struct mm_struct *mm ;
   struct mm_struct *active_mm ;
   unsigned int brk_randomized : 1 ;
   struct task_rss_stat rss_stat ;
   int exit_state ;
   int exit_code ;
   int exit_signal ;
   int pdeath_signal ;
   unsigned int jobctl ;
   unsigned int personality ;
   unsigned int in_execve : 1 ;
   unsigned int in_iowait : 1 ;
   unsigned int no_new_privs : 1 ;
   unsigned int sched_reset_on_fork : 1 ;
   unsigned int sched_contributes_to_load : 1 ;
   pid_t pid ;
   pid_t tgid ;
   struct task_struct *real_parent ;
   struct task_struct *parent ;
   struct list_head children ;
   struct list_head sibling ;
   struct task_struct *group_leader ;
   struct list_head ptraced ;
   struct list_head ptrace_entry ;
   struct pid_link pids[3U] ;
   struct list_head thread_group ;
   struct list_head thread_node ;
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
   struct rb_root pi_waiters ;
   struct rb_node *pi_waiters_leftmost ;
   struct rt_mutex_waiter *pi_blocked_on ;
   struct task_struct *pi_top_task ;
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
   unsigned int numa_scan_period ;
   unsigned int numa_scan_period_max ;
   int numa_preferred_nid ;
   int numa_migrate_deferred ;
   unsigned long numa_migrate_retry ;
   u64 node_stamp ;
   struct callback_head numa_work ;
   struct list_head numa_entry ;
   struct numa_group *numa_group ;
   unsigned long *numa_faults ;
   unsigned long total_numa_faults ;
   unsigned long *numa_faults_buffer ;
   unsigned long numa_faults_locality[2U] ;
   unsigned long numa_pages_migrated ;
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
struct usb_driver;
struct wusb_dev;
struct ep_device;
struct usb_host_endpoint {
   struct usb_endpoint_descriptor desc ;
   struct usb_ss_ep_comp_descriptor ss_ep_comp ;
   struct list_head urb_list ;
   void *hcpriv ;
   struct ep_device *ep_dev ;
   unsigned char *extra ;
   int extralen ;
   int enabled ;
};
struct usb_host_interface {
   struct usb_interface_descriptor desc ;
   int extralen ;
   unsigned char *extra ;
   struct usb_host_endpoint *endpoint ;
   char *string ;
};
enum usb_interface_condition {
    USB_INTERFACE_UNBOUND = 0,
    USB_INTERFACE_BINDING = 1,
    USB_INTERFACE_BOUND = 2,
    USB_INTERFACE_UNBINDING = 3
} ;
struct usb_interface {
   struct usb_host_interface *altsetting ;
   struct usb_host_interface *cur_altsetting ;
   unsigned int num_altsetting ;
   struct usb_interface_assoc_descriptor *intf_assoc ;
   int minor ;
   enum usb_interface_condition condition ;
   unsigned int sysfs_files_created : 1 ;
   unsigned int ep_devs_created : 1 ;
   unsigned int unregistering : 1 ;
   unsigned int needs_remote_wakeup : 1 ;
   unsigned int needs_altsetting0 : 1 ;
   unsigned int needs_binding : 1 ;
   unsigned int reset_running : 1 ;
   unsigned int resetting_device : 1 ;
   struct device dev ;
   struct device *usb_dev ;
   atomic_t pm_usage_cnt ;
   struct work_struct reset_ws ;
};
struct usb_interface_cache {
   unsigned int num_altsetting ;
   struct kref ref ;
   struct usb_host_interface altsetting[0U] ;
};
struct usb_host_config {
   struct usb_config_descriptor desc ;
   char *string ;
   struct usb_interface_assoc_descriptor *intf_assoc[16U] ;
   struct usb_interface *interface[32U] ;
   struct usb_interface_cache *intf_cache[32U] ;
   unsigned char *extra ;
   int extralen ;
};
struct usb_host_bos {
   struct usb_bos_descriptor *desc ;
   struct usb_ext_cap_descriptor *ext_cap ;
   struct usb_ss_cap_descriptor *ss_cap ;
   struct usb_ss_container_id_descriptor *ss_id ;
};
struct usb_devmap {
   unsigned long devicemap[2U] ;
};
struct mon_bus;
struct usb_bus {
   struct device *controller ;
   int busnum ;
   char const *bus_name ;
   u8 uses_dma ;
   u8 uses_pio_for_control ;
   u8 otg_port ;
   unsigned int is_b_host : 1 ;
   unsigned int b_hnp_enable : 1 ;
   unsigned int no_stop_on_short : 1 ;
   unsigned int no_sg_constraint : 1 ;
   unsigned int sg_tablesize ;
   int devnum_next ;
   struct usb_devmap devmap ;
   struct usb_device *root_hub ;
   struct usb_bus *hs_companion ;
   struct list_head bus_list ;
   int bandwidth_allocated ;
   int bandwidth_int_reqs ;
   int bandwidth_isoc_reqs ;
   unsigned int resuming_ports ;
   struct mon_bus *mon_bus ;
   int monitored ;
};
struct usb_tt;
enum usb_device_removable {
    USB_DEVICE_REMOVABLE_UNKNOWN = 0,
    USB_DEVICE_REMOVABLE = 1,
    USB_DEVICE_FIXED = 2
} ;
struct usb2_lpm_parameters {
   unsigned int besl ;
   int timeout ;
};
struct usb3_lpm_parameters {
   unsigned int mel ;
   unsigned int pel ;
   unsigned int sel ;
   int timeout ;
};
struct usb_device {
   int devnum ;
   char devpath[16U] ;
   u32 route ;
   enum usb_device_state state ;
   enum usb_device_speed speed ;
   struct usb_tt *tt ;
   int ttport ;
   unsigned int toggle[2U] ;
   struct usb_device *parent ;
   struct usb_bus *bus ;
   struct usb_host_endpoint ep0 ;
   struct device dev ;
   struct usb_device_descriptor descriptor ;
   struct usb_host_bos *bos ;
   struct usb_host_config *config ;
   struct usb_host_config *actconfig ;
   struct usb_host_endpoint *ep_in[16U] ;
   struct usb_host_endpoint *ep_out[16U] ;
   char **rawdescriptors ;
   unsigned short bus_mA ;
   u8 portnum ;
   u8 level ;
   unsigned int can_submit : 1 ;
   unsigned int persist_enabled : 1 ;
   unsigned int have_langid : 1 ;
   unsigned int authorized : 1 ;
   unsigned int authenticated : 1 ;
   unsigned int wusb : 1 ;
   unsigned int lpm_capable : 1 ;
   unsigned int usb2_hw_lpm_capable : 1 ;
   unsigned int usb2_hw_lpm_besl_capable : 1 ;
   unsigned int usb2_hw_lpm_enabled : 1 ;
   unsigned int usb2_hw_lpm_allowed : 1 ;
   unsigned int usb3_lpm_enabled : 1 ;
   int string_langid ;
   char *product ;
   char *manufacturer ;
   char *serial ;
   struct list_head filelist ;
   int maxchild ;
   u32 quirks ;
   atomic_t urbnum ;
   unsigned long active_duration ;
   unsigned long connect_time ;
   unsigned int do_remote_wakeup : 1 ;
   unsigned int reset_resume : 1 ;
   unsigned int port_is_suspended : 1 ;
   struct wusb_dev *wusb_dev ;
   int slot_id ;
   enum usb_device_removable removable ;
   struct usb2_lpm_parameters l1_params ;
   struct usb3_lpm_parameters u1_params ;
   struct usb3_lpm_parameters u2_params ;
   unsigned int lpm_disable_count ;
};
struct usb_dynids {
   spinlock_t lock ;
   struct list_head list ;
};
struct usbdrv_wrap {
   struct device_driver driver ;
   int for_devices ;
};
struct usb_driver {
   char const *name ;
   int (*probe)(struct usb_interface * , struct usb_device_id const * ) ;
   void (*disconnect)(struct usb_interface * ) ;
   int (*unlocked_ioctl)(struct usb_interface * , unsigned int , void * ) ;
   int (*suspend)(struct usb_interface * , pm_message_t ) ;
   int (*resume)(struct usb_interface * ) ;
   int (*reset_resume)(struct usb_interface * ) ;
   int (*pre_reset)(struct usb_interface * ) ;
   int (*post_reset)(struct usb_interface * ) ;
   struct usb_device_id const *id_table ;
   struct usb_dynids dynids ;
   struct usbdrv_wrap drvwrap ;
   unsigned int no_dynamic_id : 1 ;
   unsigned int supports_autosuspend : 1 ;
   unsigned int disable_hub_initiated_lpm : 1 ;
   unsigned int soft_unbind : 1 ;
};
struct usb_iso_packet_descriptor {
   unsigned int offset ;
   unsigned int length ;
   unsigned int actual_length ;
   int status ;
};
struct urb;
struct usb_anchor {
   struct list_head urb_list ;
   wait_queue_head_t wait ;
   spinlock_t lock ;
   atomic_t suspend_wakeups ;
   unsigned int poisoned : 1 ;
};
struct scatterlist;
struct urb {
   struct kref kref ;
   void *hcpriv ;
   atomic_t use_count ;
   atomic_t reject ;
   int unlinked ;
   struct list_head urb_list ;
   struct list_head anchor_list ;
   struct usb_anchor *anchor ;
   struct usb_device *dev ;
   struct usb_host_endpoint *ep ;
   unsigned int pipe ;
   unsigned int stream_id ;
   int status ;
   unsigned int transfer_flags ;
   void *transfer_buffer ;
   dma_addr_t transfer_dma ;
   struct scatterlist *sg ;
   int num_mapped_sgs ;
   int num_sgs ;
   u32 transfer_buffer_length ;
   u32 actual_length ;
   unsigned char *setup_packet ;
   dma_addr_t setup_dma ;
   int start_frame ;
   int number_of_packets ;
   int interval ;
   int error_count ;
   void *context ;
   void (*complete)(struct urb * ) ;
   struct usb_iso_packet_descriptor iso_frame_desc[0U] ;
};
struct vm_fault {
   unsigned int flags ;
   unsigned long pgoff ;
   void *virtual_address ;
   struct page *page ;
};
struct vm_operations_struct {
   void (*open)(struct vm_area_struct * ) ;
   void (*close)(struct vm_area_struct * ) ;
   int (*fault)(struct vm_area_struct * , struct vm_fault * ) ;
   int (*page_mkwrite)(struct vm_area_struct * , struct vm_fault * ) ;
   int (*access)(struct vm_area_struct * , unsigned long , void * , int , int ) ;
   int (*set_policy)(struct vm_area_struct * , struct mempolicy * ) ;
   struct mempolicy *(*get_policy)(struct vm_area_struct * , unsigned long ) ;
   int (*migrate)(struct vm_area_struct * , nodemask_t const * , nodemask_t const * ,
                  unsigned long ) ;
   int (*remap_pages)(struct vm_area_struct * , unsigned long , unsigned long ,
                      unsigned long ) ;
};
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
union __anonunion____missing_field_name_195 {
   void *arg ;
   struct kparam_string const *str ;
   struct kparam_array const *arr ;
};
struct kernel_param {
   char const *name ;
   struct kernel_param_ops const *ops ;
   u16 perm ;
   s16 level ;
   union __anonunion____missing_field_name_195 __annonCompField64 ;
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
struct __anonstruct____missing_field_name_197 {
   struct callback_head callback_head ;
   struct kmem_cache *memcg_caches[0U] ;
};
struct __anonstruct____missing_field_name_198 {
   struct mem_cgroup *memcg ;
   struct list_head list ;
   struct kmem_cache *root_cache ;
   bool dead ;
   atomic_t nr_pages ;
   struct work_struct destroy ;
};
union __anonunion____missing_field_name_196 {
   struct __anonstruct____missing_field_name_197 __annonCompField65 ;
   struct __anonstruct____missing_field_name_198 __annonCompField66 ;
};
struct memcg_cache_params {
   bool is_root_cache ;
   union __anonunion____missing_field_name_196 __annonCompField67 ;
};
enum v4l2_buf_type {
    V4L2_BUF_TYPE_VIDEO_CAPTURE = 1,
    V4L2_BUF_TYPE_VIDEO_OUTPUT = 2,
    V4L2_BUF_TYPE_VIDEO_OVERLAY = 3,
    V4L2_BUF_TYPE_VBI_CAPTURE = 4,
    V4L2_BUF_TYPE_VBI_OUTPUT = 5,
    V4L2_BUF_TYPE_SLICED_VBI_CAPTURE = 6,
    V4L2_BUF_TYPE_SLICED_VBI_OUTPUT = 7,
    V4L2_BUF_TYPE_VIDEO_OUTPUT_OVERLAY = 8,
    V4L2_BUF_TYPE_VIDEO_CAPTURE_MPLANE = 9,
    V4L2_BUF_TYPE_VIDEO_OUTPUT_MPLANE = 10,
    V4L2_BUF_TYPE_PRIVATE = 128
} ;
enum v4l2_memory {
    V4L2_MEMORY_MMAP = 1,
    V4L2_MEMORY_USERPTR = 2,
    V4L2_MEMORY_OVERLAY = 3,
    V4L2_MEMORY_DMABUF = 4
} ;
enum v4l2_priority {
    V4L2_PRIORITY_UNSET = 0,
    V4L2_PRIORITY_BACKGROUND = 1,
    V4L2_PRIORITY_INTERACTIVE = 2,
    V4L2_PRIORITY_RECORD = 3,
    V4L2_PRIORITY_DEFAULT = 2
} ;
struct v4l2_rect {
   __s32 left ;
   __s32 top ;
   __u32 width ;
   __u32 height ;
};
struct v4l2_fract {
   __u32 numerator ;
   __u32 denominator ;
};
struct v4l2_pix_format {
   __u32 width ;
   __u32 height ;
   __u32 pixelformat ;
   __u32 field ;
   __u32 bytesperline ;
   __u32 sizeimage ;
   __u32 colorspace ;
   __u32 priv ;
};
struct v4l2_frmsize_discrete {
   __u32 width ;
   __u32 height ;
};
struct v4l2_frmsize_stepwise {
   __u32 min_width ;
   __u32 max_width ;
   __u32 step_width ;
   __u32 min_height ;
   __u32 max_height ;
   __u32 step_height ;
};
union __anonunion____missing_field_name_199 {
   struct v4l2_frmsize_discrete discrete ;
   struct v4l2_frmsize_stepwise stepwise ;
};
struct v4l2_frmsizeenum {
   __u32 index ;
   __u32 pixel_format ;
   __u32 type ;
   union __anonunion____missing_field_name_199 __annonCompField68 ;
   __u32 reserved[2U] ;
};
struct v4l2_frmival_stepwise {
   struct v4l2_fract min ;
   struct v4l2_fract max ;
   struct v4l2_fract step ;
};
union __anonunion____missing_field_name_200 {
   struct v4l2_fract discrete ;
   struct v4l2_frmival_stepwise stepwise ;
};
struct v4l2_frmivalenum {
   __u32 index ;
   __u32 pixel_format ;
   __u32 width ;
   __u32 height ;
   __u32 type ;
   union __anonunion____missing_field_name_200 __annonCompField69 ;
   __u32 reserved[2U] ;
};
struct v4l2_timecode {
   __u32 type ;
   __u32 flags ;
   __u8 frames ;
   __u8 seconds ;
   __u8 minutes ;
   __u8 hours ;
   __u8 userbits[4U] ;
};
union __anonunion_m_201 {
   __u32 mem_offset ;
   unsigned long userptr ;
   __s32 fd ;
};
struct v4l2_plane {
   __u32 bytesused ;
   __u32 length ;
   union __anonunion_m_201 m ;
   __u32 data_offset ;
   __u32 reserved[11U] ;
};
union __anonunion_m_202 {
   __u32 offset ;
   unsigned long userptr ;
   struct v4l2_plane *planes ;
   __s32 fd ;
};
struct v4l2_buffer {
   __u32 index ;
   __u32 type ;
   __u32 bytesused ;
   __u32 flags ;
   __u32 field ;
   struct timeval timestamp ;
   struct v4l2_timecode timecode ;
   __u32 sequence ;
   __u32 memory ;
   union __anonunion_m_202 m ;
   __u32 length ;
   __u32 reserved2 ;
   __u32 reserved ;
};
struct v4l2_clip {
   struct v4l2_rect c ;
   struct v4l2_clip *next ;
};
struct v4l2_window {
   struct v4l2_rect w ;
   __u32 field ;
   __u32 chromakey ;
   struct v4l2_clip *clips ;
   __u32 clipcount ;
   void *bitmap ;
   __u8 global_alpha ;
};
struct v4l2_captureparm {
   __u32 capability ;
   __u32 capturemode ;
   struct v4l2_fract timeperframe ;
   __u32 extendedmode ;
   __u32 readbuffers ;
   __u32 reserved[4U] ;
};
struct v4l2_outputparm {
   __u32 capability ;
   __u32 outputmode ;
   struct v4l2_fract timeperframe ;
   __u32 extendedmode ;
   __u32 writebuffers ;
   __u32 reserved[4U] ;
};
struct v4l2_cropcap {
   __u32 type ;
   struct v4l2_rect bounds ;
   struct v4l2_rect defrect ;
   struct v4l2_fract pixelaspect ;
};
struct v4l2_crop {
   __u32 type ;
   struct v4l2_rect c ;
};
typedef __u64 v4l2_std_id;
struct v4l2_bt_timings {
   __u32 width ;
   __u32 height ;
   __u32 interlaced ;
   __u32 polarities ;
   __u64 pixelclock ;
   __u32 hfrontporch ;
   __u32 hsync ;
   __u32 hbackporch ;
   __u32 vfrontporch ;
   __u32 vsync ;
   __u32 vbackporch ;
   __u32 il_vfrontporch ;
   __u32 il_vsync ;
   __u32 il_vbackporch ;
   __u32 standards ;
   __u32 flags ;
   __u32 reserved[14U] ;
};
union __anonunion____missing_field_name_203 {
   struct v4l2_bt_timings bt ;
   __u32 reserved[32U] ;
};
struct v4l2_dv_timings {
   __u32 type ;
   union __anonunion____missing_field_name_203 __annonCompField70 ;
};
struct v4l2_enum_dv_timings {
   __u32 index ;
   __u32 reserved[3U] ;
   struct v4l2_dv_timings timings ;
};
struct v4l2_bt_timings_cap {
   __u32 min_width ;
   __u32 max_width ;
   __u32 min_height ;
   __u32 max_height ;
   __u64 min_pixelclock ;
   __u64 max_pixelclock ;
   __u32 standards ;
   __u32 capabilities ;
   __u32 reserved[16U] ;
};
union __anonunion____missing_field_name_204 {
   struct v4l2_bt_timings_cap bt ;
   __u32 raw_data[32U] ;
};
struct v4l2_dv_timings_cap {
   __u32 type ;
   __u32 reserved[3U] ;
   union __anonunion____missing_field_name_204 __annonCompField71 ;
};
struct v4l2_control {
   __u32 id ;
   __s32 value ;
};
union __anonunion____missing_field_name_205 {
   __s32 value ;
   __s64 value64 ;
   char *string ;
};
struct v4l2_ext_control {
   __u32 id ;
   __u32 size ;
   __u32 reserved2[1U] ;
   union __anonunion____missing_field_name_205 __annonCompField72 ;
};
struct v4l2_ext_controls {
   __u32 ctrl_class ;
   __u32 count ;
   __u32 error_idx ;
   __u32 reserved[2U] ;
   struct v4l2_ext_control *controls ;
};
enum v4l2_ctrl_type {
    V4L2_CTRL_TYPE_INTEGER = 1,
    V4L2_CTRL_TYPE_BOOLEAN = 2,
    V4L2_CTRL_TYPE_MENU = 3,
    V4L2_CTRL_TYPE_BUTTON = 4,
    V4L2_CTRL_TYPE_INTEGER64 = 5,
    V4L2_CTRL_TYPE_CTRL_CLASS = 6,
    V4L2_CTRL_TYPE_STRING = 7,
    V4L2_CTRL_TYPE_BITMASK = 8,
    V4L2_CTRL_TYPE_INTEGER_MENU = 9
} ;
struct v4l2_queryctrl {
   __u32 id ;
   __u32 type ;
   __u8 name[32U] ;
   __s32 minimum ;
   __s32 maximum ;
   __s32 step ;
   __s32 default_value ;
   __u32 flags ;
   __u32 reserved[2U] ;
};
union __anonunion____missing_field_name_206 {
   __u8 name[32U] ;
   __s64 value ;
};
struct v4l2_querymenu {
   __u32 id ;
   __u32 index ;
   union __anonunion____missing_field_name_206 __annonCompField73 ;
   __u32 reserved ;
};
struct v4l2_tuner {
   __u32 index ;
   __u8 name[32U] ;
   __u32 type ;
   __u32 capability ;
   __u32 rangelow ;
   __u32 rangehigh ;
   __u32 rxsubchans ;
   __u32 audmode ;
   __s32 signal ;
   __s32 afc ;
   __u32 reserved[4U] ;
};
struct v4l2_modulator {
   __u32 index ;
   __u8 name[32U] ;
   __u32 capability ;
   __u32 rangelow ;
   __u32 rangehigh ;
   __u32 txsubchans ;
   __u32 reserved[4U] ;
};
struct v4l2_frequency {
   __u32 tuner ;
   __u32 type ;
   __u32 frequency ;
   __u32 reserved[8U] ;
};
struct v4l2_vbi_format {
   __u32 sampling_rate ;
   __u32 offset ;
   __u32 samples_per_line ;
   __u32 sample_format ;
   __s32 start[2U] ;
   __u32 count[2U] ;
   __u32 flags ;
   __u32 reserved[2U] ;
};
struct v4l2_sliced_vbi_format {
   __u16 service_set ;
   __u16 service_lines[2U][24U] ;
   __u32 io_size ;
   __u32 reserved[2U] ;
};
struct v4l2_sliced_vbi_cap {
   __u16 service_set ;
   __u16 service_lines[2U][24U] ;
   __u32 type ;
   __u32 reserved[3U] ;
};
struct v4l2_sliced_vbi_data {
   __u32 id ;
   __u32 field ;
   __u32 line ;
   __u32 reserved ;
   __u8 data[48U] ;
};
struct v4l2_plane_pix_format {
   __u32 sizeimage ;
   __u16 bytesperline ;
   __u16 reserved[7U] ;
};
struct v4l2_pix_format_mplane {
   __u32 width ;
   __u32 height ;
   __u32 pixelformat ;
   __u32 field ;
   __u32 colorspace ;
   struct v4l2_plane_pix_format plane_fmt[8U] ;
   __u8 num_planes ;
   __u8 reserved[11U] ;
};
union __anonunion_fmt_214 {
   struct v4l2_pix_format pix ;
   struct v4l2_pix_format_mplane pix_mp ;
   struct v4l2_window win ;
   struct v4l2_vbi_format vbi ;
   struct v4l2_sliced_vbi_format sliced ;
   __u8 raw_data[200U] ;
};
struct v4l2_format {
   __u32 type ;
   union __anonunion_fmt_214 fmt ;
};
union __anonunion_parm_215 {
   struct v4l2_captureparm capture ;
   struct v4l2_outputparm output ;
   __u8 raw_data[200U] ;
};
struct v4l2_streamparm {
   __u32 type ;
   union __anonunion_parm_215 parm ;
};
struct v4l2_event_vsync {
   __u8 field ;
};
union __anonunion____missing_field_name_216 {
   __s32 value ;
   __s64 value64 ;
};
struct v4l2_event_ctrl {
   __u32 changes ;
   __u32 type ;
   union __anonunion____missing_field_name_216 __annonCompField77 ;
   __u32 flags ;
   __s32 minimum ;
   __s32 maximum ;
   __s32 step ;
   __s32 default_value ;
};
struct v4l2_event_frame_sync {
   __u32 frame_sequence ;
};
union __anonunion_u_217 {
   struct v4l2_event_vsync vsync ;
   struct v4l2_event_ctrl ctrl ;
   struct v4l2_event_frame_sync frame_sync ;
   __u8 data[64U] ;
};
struct v4l2_event {
   __u32 type ;
   union __anonunion_u_217 u ;
   __u32 pending ;
   __u32 sequence ;
   struct timespec timestamp ;
   __u32 id ;
   __u32 reserved[8U] ;
};
struct v4l2_event_subscription {
   __u32 type ;
   __u32 id ;
   __u32 flags ;
   __u32 reserved[5U] ;
};
union __anonunion____missing_field_name_218 {
   __u32 addr ;
   char name[32U] ;
};
struct v4l2_dbg_match {
   __u32 type ;
   union __anonunion____missing_field_name_218 __annonCompField78 ;
};
struct v4l2_dbg_register {
   struct v4l2_dbg_match match ;
   __u32 size ;
   __u64 reg ;
   __u64 val ;
};
struct poll_table_struct {
   void (*_qproc)(struct file * , wait_queue_head_t * , struct poll_table_struct * ) ;
   unsigned long _key ;
};
struct media_pipeline {
};
struct media_pad;
struct media_link {
   struct media_pad *source ;
   struct media_pad *sink ;
   struct media_link *reverse ;
   unsigned long flags ;
};
struct media_entity;
struct media_pad {
   struct media_entity *entity ;
   u16 index ;
   unsigned long flags ;
};
struct media_entity_operations {
   int (*link_setup)(struct media_entity * , struct media_pad const * , struct media_pad const * ,
                     u32 ) ;
   int (*link_validate)(struct media_link * ) ;
};
struct media_device;
struct __anonstruct_v4l_225 {
   u32 major ;
   u32 minor ;
};
struct __anonstruct_fb_226 {
   u32 major ;
   u32 minor ;
};
struct __anonstruct_alsa_227 {
   u32 card ;
   u32 device ;
   u32 subdevice ;
};
union __anonunion_info_224 {
   struct __anonstruct_v4l_225 v4l ;
   struct __anonstruct_fb_226 fb ;
   struct __anonstruct_alsa_227 alsa ;
   int dvb ;
};
struct media_entity {
   struct list_head list ;
   struct media_device *parent ;
   u32 id ;
   char const *name ;
   u32 type ;
   u32 revision ;
   unsigned long flags ;
   u32 group_id ;
   u16 num_pads ;
   u16 num_links ;
   u16 num_backlinks ;
   u16 max_links ;
   struct media_pad *pads ;
   struct media_link *links ;
   struct media_entity_operations const *ops ;
   int stream_count ;
   int use_count ;
   struct media_pipeline *pipe ;
   union __anonunion_info_224 info ;
};
struct video_device;
struct v4l2_device;
struct v4l2_ctrl_handler;
struct v4l2_prio_state {
   atomic_t prios[4U] ;
};
struct v4l2_file_operations {
   struct module *owner ;
   ssize_t (*read)(struct file * , char * , size_t , loff_t * ) ;
   ssize_t (*write)(struct file * , char const * , size_t , loff_t * ) ;
   unsigned int (*poll)(struct file * , struct poll_table_struct * ) ;
   long (*ioctl)(struct file * , unsigned int , unsigned long ) ;
   long (*unlocked_ioctl)(struct file * , unsigned int , unsigned long ) ;
   long (*compat_ioctl32)(struct file * , unsigned int , unsigned long ) ;
   unsigned long (*get_unmapped_area)(struct file * , unsigned long , unsigned long ,
                                      unsigned long , unsigned long ) ;
   int (*mmap)(struct file * , struct vm_area_struct * ) ;
   int (*open)(struct file * ) ;
   int (*release)(struct file * ) ;
};
struct vb2_queue;
struct v4l2_ioctl_ops;
struct video_device {
   struct media_entity entity ;
   struct v4l2_file_operations const *fops ;
   struct device dev ;
   struct cdev *cdev ;
   struct v4l2_device *v4l2_dev ;
   struct device *dev_parent ;
   struct v4l2_ctrl_handler *ctrl_handler ;
   struct vb2_queue *queue ;
   struct v4l2_prio_state *prio ;
   char name[32U] ;
   int vfl_type ;
   int vfl_dir ;
   int minor ;
   u16 num ;
   unsigned long flags ;
   int index ;
   spinlock_t fh_lock ;
   struct list_head fh_list ;
   int debug ;
   v4l2_std_id tvnorms ;
   void (*release)(struct video_device * ) ;
   struct v4l2_ioctl_ops const *ioctl_ops ;
   unsigned long valid_ioctls[3U] ;
   unsigned long disable_locking[3U] ;
   struct mutex *lock ;
};
struct v4l2_subdev;
struct v4l2_subdev_ops;
struct v4l2_priv_tun_config {
   int tuner ;
   void *priv ;
};
struct uvc_streaming_control {
   __u16 bmHint ;
   __u8 bFormatIndex ;
   __u8 bFrameIndex ;
   __u32 dwFrameInterval ;
   __u16 wKeyFrameRate ;
   __u16 wPFrameRate ;
   __u16 wCompQuality ;
   __u16 wCompWindowSize ;
   __u16 wDelay ;
   __u32 dwMaxVideoFrameSize ;
   __u32 dwMaxPayloadTransferSize ;
   __u32 dwClockFrequency ;
   __u8 bmFramingInfo ;
   __u8 bPreferedVersion ;
   __u8 bMinVersion ;
   __u8 bMaxVersion ;
};
struct uvc_menu_info {
   __u32 value ;
   __u8 name[32U] ;
};
struct media_file_operations {
   struct module *owner ;
   ssize_t (*read)(struct file * , char * , size_t , loff_t * ) ;
   ssize_t (*write)(struct file * , char const * , size_t , loff_t * ) ;
   unsigned int (*poll)(struct file * , struct poll_table_struct * ) ;
   long (*ioctl)(struct file * , unsigned int , unsigned long ) ;
   long (*compat_ioctl)(struct file * , unsigned int , unsigned long ) ;
   int (*open)(struct file * ) ;
   int (*release)(struct file * ) ;
};
struct media_devnode {
   struct media_file_operations const *fops ;
   struct device dev ;
   struct cdev cdev ;
   struct device *parent ;
   int minor ;
   unsigned long flags ;
   void (*release)(struct media_devnode * ) ;
};
struct media_device {
   struct device *dev ;
   struct media_devnode devnode ;
   char model[32U] ;
   char serial[40U] ;
   char bus_info[32U] ;
   u32 hw_revision ;
   u32 driver_version ;
   u32 entity_id ;
   struct list_head entities ;
   spinlock_t lock ;
   struct mutex graph_mutex ;
   int (*link_notify)(struct media_link * , u32 , unsigned int ) ;
};
enum v4l2_mbus_pixelcode {
    V4L2_MBUS_FMT_FIXED = 1,
    V4L2_MBUS_FMT_RGB444_2X8_PADHI_BE = 4097,
    V4L2_MBUS_FMT_RGB444_2X8_PADHI_LE = 4098,
    V4L2_MBUS_FMT_RGB555_2X8_PADHI_BE = 4099,
    V4L2_MBUS_FMT_RGB555_2X8_PADHI_LE = 4100,
    V4L2_MBUS_FMT_BGR565_2X8_BE = 4101,
    V4L2_MBUS_FMT_BGR565_2X8_LE = 4102,
    V4L2_MBUS_FMT_RGB565_2X8_BE = 4103,
    V4L2_MBUS_FMT_RGB565_2X8_LE = 4104,
    V4L2_MBUS_FMT_RGB666_1X18 = 4105,
    V4L2_MBUS_FMT_RGB888_1X24 = 4106,
    V4L2_MBUS_FMT_RGB888_2X12_BE = 4107,
    V4L2_MBUS_FMT_RGB888_2X12_LE = 4108,
    V4L2_MBUS_FMT_ARGB8888_1X32 = 4109,
    V4L2_MBUS_FMT_Y8_1X8 = 8193,
    V4L2_MBUS_FMT_UV8_1X8 = 8213,
    V4L2_MBUS_FMT_UYVY8_1_5X8 = 8194,
    V4L2_MBUS_FMT_VYUY8_1_5X8 = 8195,
    V4L2_MBUS_FMT_YUYV8_1_5X8 = 8196,
    V4L2_MBUS_FMT_YVYU8_1_5X8 = 8197,
    V4L2_MBUS_FMT_UYVY8_2X8 = 8198,
    V4L2_MBUS_FMT_VYUY8_2X8 = 8199,
    V4L2_MBUS_FMT_YUYV8_2X8 = 8200,
    V4L2_MBUS_FMT_YVYU8_2X8 = 8201,
    V4L2_MBUS_FMT_Y10_1X10 = 8202,
    V4L2_MBUS_FMT_YUYV10_2X10 = 8203,
    V4L2_MBUS_FMT_YVYU10_2X10 = 8204,
    V4L2_MBUS_FMT_Y12_1X12 = 8211,
    V4L2_MBUS_FMT_UYVY8_1X16 = 8207,
    V4L2_MBUS_FMT_VYUY8_1X16 = 8208,
    V4L2_MBUS_FMT_YUYV8_1X16 = 8209,
    V4L2_MBUS_FMT_YVYU8_1X16 = 8210,
    V4L2_MBUS_FMT_YDYUYDYV8_1X16 = 8212,
    V4L2_MBUS_FMT_YUYV10_1X20 = 8205,
    V4L2_MBUS_FMT_YVYU10_1X20 = 8206,
    V4L2_MBUS_FMT_YUV10_1X30 = 8214,
    V4L2_MBUS_FMT_AYUV8_1X32 = 8215,
    V4L2_MBUS_FMT_SBGGR8_1X8 = 12289,
    V4L2_MBUS_FMT_SGBRG8_1X8 = 12307,
    V4L2_MBUS_FMT_SGRBG8_1X8 = 12290,
    V4L2_MBUS_FMT_SRGGB8_1X8 = 12308,
    V4L2_MBUS_FMT_SBGGR10_ALAW8_1X8 = 12309,
    V4L2_MBUS_FMT_SGBRG10_ALAW8_1X8 = 12310,
    V4L2_MBUS_FMT_SGRBG10_ALAW8_1X8 = 12311,
    V4L2_MBUS_FMT_SRGGB10_ALAW8_1X8 = 12312,
    V4L2_MBUS_FMT_SBGGR10_DPCM8_1X8 = 12299,
    V4L2_MBUS_FMT_SGBRG10_DPCM8_1X8 = 12300,
    V4L2_MBUS_FMT_SGRBG10_DPCM8_1X8 = 12297,
    V4L2_MBUS_FMT_SRGGB10_DPCM8_1X8 = 12301,
    V4L2_MBUS_FMT_SBGGR10_2X8_PADHI_BE = 12291,
    V4L2_MBUS_FMT_SBGGR10_2X8_PADHI_LE = 12292,
    V4L2_MBUS_FMT_SBGGR10_2X8_PADLO_BE = 12293,
    V4L2_MBUS_FMT_SBGGR10_2X8_PADLO_LE = 12294,
    V4L2_MBUS_FMT_SBGGR10_1X10 = 12295,
    V4L2_MBUS_FMT_SGBRG10_1X10 = 12302,
    V4L2_MBUS_FMT_SGRBG10_1X10 = 12298,
    V4L2_MBUS_FMT_SRGGB10_1X10 = 12303,
    V4L2_MBUS_FMT_SBGGR12_1X12 = 12296,
    V4L2_MBUS_FMT_SGBRG12_1X12 = 12304,
    V4L2_MBUS_FMT_SGRBG12_1X12 = 12305,
    V4L2_MBUS_FMT_SRGGB12_1X12 = 12306,
    V4L2_MBUS_FMT_JPEG_1X8 = 16385,
    V4L2_MBUS_FMT_S5C_UYVY_JPEG_1X8 = 20481,
    V4L2_MBUS_FMT_AHSV8888_1X32 = 24577
} ;
struct v4l2_mbus_framefmt {
   __u32 width ;
   __u32 height ;
   __u32 code ;
   __u32 field ;
   __u32 colorspace ;
   __u32 reserved[7U] ;
};
struct v4l2_subdev_format {
   __u32 which ;
   __u32 pad ;
   struct v4l2_mbus_framefmt format ;
   __u32 reserved[8U] ;
};
struct v4l2_subdev_crop {
   __u32 which ;
   __u32 pad ;
   struct v4l2_rect rect ;
   __u32 reserved[8U] ;
};
struct v4l2_subdev_mbus_code_enum {
   __u32 pad ;
   __u32 index ;
   __u32 code ;
   __u32 reserved[9U] ;
};
struct v4l2_subdev_frame_size_enum {
   __u32 index ;
   __u32 pad ;
   __u32 code ;
   __u32 min_width ;
   __u32 max_width ;
   __u32 min_height ;
   __u32 max_height ;
   __u32 reserved[9U] ;
};
struct v4l2_subdev_frame_interval {
   __u32 pad ;
   struct v4l2_fract interval ;
   __u32 reserved[9U] ;
};
struct v4l2_subdev_frame_interval_enum {
   __u32 index ;
   __u32 pad ;
   __u32 code ;
   __u32 width ;
   __u32 height ;
   struct v4l2_fract interval ;
   __u32 reserved[9U] ;
};
struct v4l2_subdev_selection {
   __u32 which ;
   __u32 pad ;
   __u32 target ;
   __u32 flags ;
   struct v4l2_rect r ;
   __u32 reserved[8U] ;
};
struct v4l2_subdev_edid {
   __u32 pad ;
   __u32 start_block ;
   __u32 blocks ;
   __u32 reserved[5U] ;
   __u8 *edid ;
};
struct v4l2_async_notifier;
enum v4l2_async_match_type {
    V4L2_ASYNC_MATCH_CUSTOM = 0,
    V4L2_ASYNC_MATCH_DEVNAME = 1,
    V4L2_ASYNC_MATCH_I2C = 2,
    V4L2_ASYNC_MATCH_OF = 3
} ;
struct __anonstruct_of_230 {
   struct device_node const *node ;
};
struct __anonstruct_device_name_231 {
   char const *name ;
};
struct __anonstruct_i2c_232 {
   int adapter_id ;
   unsigned short address ;
};
struct __anonstruct_custom_233 {
   bool (*match)(struct device * , struct v4l2_async_subdev * ) ;
   void *priv ;
};
union __anonunion_match_229 {
   struct __anonstruct_of_230 of ;
   struct __anonstruct_device_name_231 device_name ;
   struct __anonstruct_i2c_232 i2c ;
   struct __anonstruct_custom_233 custom ;
};
struct v4l2_async_subdev {
   enum v4l2_async_match_type match_type ;
   union __anonunion_match_229 match ;
   struct list_head list ;
};
struct v4l2_async_notifier {
   unsigned int num_subdevs ;
   struct v4l2_async_subdev **subdevs ;
   struct v4l2_device *v4l2_dev ;
   struct list_head waiting ;
   struct list_head done ;
   struct list_head list ;
   int (*bound)(struct v4l2_async_notifier * , struct v4l2_subdev * , struct v4l2_async_subdev * ) ;
   int (*complete)(struct v4l2_async_notifier * ) ;
   void (*unbind)(struct v4l2_async_notifier * , struct v4l2_subdev * , struct v4l2_async_subdev * ) ;
};
struct v4l2_m2m_ctx;
struct v4l2_fh {
   struct list_head list ;
   struct video_device *vdev ;
   struct v4l2_ctrl_handler *ctrl_handler ;
   enum v4l2_priority prio ;
   wait_queue_head_t wait ;
   struct list_head subscribed ;
   struct list_head available ;
   unsigned int navailable ;
   u32 sequence ;
   struct v4l2_m2m_ctx *m2m_ctx ;
};
enum v4l2_mbus_type {
    V4L2_MBUS_PARALLEL = 0,
    V4L2_MBUS_BT656 = 1,
    V4L2_MBUS_CSI2 = 2
} ;
struct v4l2_mbus_config {
   enum v4l2_mbus_type type ;
   unsigned int flags ;
};
struct v4l2_subdev_fh;
struct tuner_setup;
struct v4l2_mbus_frame_desc;
struct v4l2_decode_vbi_line {
   u32 is_second_field ;
   u8 *p ;
   u32 line ;
   u32 type ;
};
struct v4l2_subdev_io_pin_config {
   u32 flags ;
   u8 pin ;
   u8 function ;
   u8 value ;
   u8 strength ;
};
struct v4l2_subdev_core_ops {
   int (*log_status)(struct v4l2_subdev * ) ;
   int (*s_io_pin_config)(struct v4l2_subdev * , size_t , struct v4l2_subdev_io_pin_config * ) ;
   int (*init)(struct v4l2_subdev * , u32 ) ;
   int (*load_fw)(struct v4l2_subdev * ) ;
   int (*reset)(struct v4l2_subdev * , u32 ) ;
   int (*s_gpio)(struct v4l2_subdev * , u32 ) ;
   int (*queryctrl)(struct v4l2_subdev * , struct v4l2_queryctrl * ) ;
   int (*g_ctrl)(struct v4l2_subdev * , struct v4l2_control * ) ;
   int (*s_ctrl)(struct v4l2_subdev * , struct v4l2_control * ) ;
   int (*g_ext_ctrls)(struct v4l2_subdev * , struct v4l2_ext_controls * ) ;
   int (*s_ext_ctrls)(struct v4l2_subdev * , struct v4l2_ext_controls * ) ;
   int (*try_ext_ctrls)(struct v4l2_subdev * , struct v4l2_ext_controls * ) ;
   int (*querymenu)(struct v4l2_subdev * , struct v4l2_querymenu * ) ;
   int (*g_std)(struct v4l2_subdev * , v4l2_std_id * ) ;
   int (*s_std)(struct v4l2_subdev * , v4l2_std_id ) ;
   long (*ioctl)(struct v4l2_subdev * , unsigned int , void * ) ;
   int (*g_register)(struct v4l2_subdev * , struct v4l2_dbg_register * ) ;
   int (*s_register)(struct v4l2_subdev * , struct v4l2_dbg_register const * ) ;
   int (*s_power)(struct v4l2_subdev * , int ) ;
   int (*interrupt_service_routine)(struct v4l2_subdev * , u32 , bool * ) ;
   int (*subscribe_event)(struct v4l2_subdev * , struct v4l2_fh * , struct v4l2_event_subscription * ) ;
   int (*unsubscribe_event)(struct v4l2_subdev * , struct v4l2_fh * , struct v4l2_event_subscription * ) ;
};
struct v4l2_subdev_tuner_ops {
   int (*s_radio)(struct v4l2_subdev * ) ;
   int (*s_frequency)(struct v4l2_subdev * , struct v4l2_frequency const * ) ;
   int (*g_frequency)(struct v4l2_subdev * , struct v4l2_frequency * ) ;
   int (*g_tuner)(struct v4l2_subdev * , struct v4l2_tuner * ) ;
   int (*s_tuner)(struct v4l2_subdev * , struct v4l2_tuner const * ) ;
   int (*g_modulator)(struct v4l2_subdev * , struct v4l2_modulator * ) ;
   int (*s_modulator)(struct v4l2_subdev * , struct v4l2_modulator const * ) ;
   int (*s_type_addr)(struct v4l2_subdev * , struct tuner_setup * ) ;
   int (*s_config)(struct v4l2_subdev * , struct v4l2_priv_tun_config const * ) ;
};
struct v4l2_subdev_audio_ops {
   int (*s_clock_freq)(struct v4l2_subdev * , u32 ) ;
   int (*s_i2s_clock_freq)(struct v4l2_subdev * , u32 ) ;
   int (*s_routing)(struct v4l2_subdev * , u32 , u32 , u32 ) ;
   int (*s_stream)(struct v4l2_subdev * , int ) ;
};
struct v4l2_mbus_frame_desc_entry {
   u16 flags ;
   u32 pixelcode ;
   u32 length ;
};
struct v4l2_mbus_frame_desc {
   struct v4l2_mbus_frame_desc_entry entry[4U] ;
   unsigned short num_entries ;
};
struct v4l2_subdev_video_ops {
   int (*s_routing)(struct v4l2_subdev * , u32 , u32 , u32 ) ;
   int (*s_crystal_freq)(struct v4l2_subdev * , u32 , u32 ) ;
   int (*s_std_output)(struct v4l2_subdev * , v4l2_std_id ) ;
   int (*g_std_output)(struct v4l2_subdev * , v4l2_std_id * ) ;
   int (*querystd)(struct v4l2_subdev * , v4l2_std_id * ) ;
   int (*g_tvnorms_output)(struct v4l2_subdev * , v4l2_std_id * ) ;
   int (*g_input_status)(struct v4l2_subdev * , u32 * ) ;
   int (*s_stream)(struct v4l2_subdev * , int ) ;
   int (*cropcap)(struct v4l2_subdev * , struct v4l2_cropcap * ) ;
   int (*g_crop)(struct v4l2_subdev * , struct v4l2_crop * ) ;
   int (*s_crop)(struct v4l2_subdev * , struct v4l2_crop const * ) ;
   int (*g_parm)(struct v4l2_subdev * , struct v4l2_streamparm * ) ;
   int (*s_parm)(struct v4l2_subdev * , struct v4l2_streamparm * ) ;
   int (*g_frame_interval)(struct v4l2_subdev * , struct v4l2_subdev_frame_interval * ) ;
   int (*s_frame_interval)(struct v4l2_subdev * , struct v4l2_subdev_frame_interval * ) ;
   int (*enum_framesizes)(struct v4l2_subdev * , struct v4l2_frmsizeenum * ) ;
   int (*enum_frameintervals)(struct v4l2_subdev * , struct v4l2_frmivalenum * ) ;
   int (*s_dv_timings)(struct v4l2_subdev * , struct v4l2_dv_timings * ) ;
   int (*g_dv_timings)(struct v4l2_subdev * , struct v4l2_dv_timings * ) ;
   int (*enum_dv_timings)(struct v4l2_subdev * , struct v4l2_enum_dv_timings * ) ;
   int (*query_dv_timings)(struct v4l2_subdev * , struct v4l2_dv_timings * ) ;
   int (*dv_timings_cap)(struct v4l2_subdev * , struct v4l2_dv_timings_cap * ) ;
   int (*enum_mbus_fmt)(struct v4l2_subdev * , unsigned int , enum v4l2_mbus_pixelcode * ) ;
   int (*enum_mbus_fsizes)(struct v4l2_subdev * , struct v4l2_frmsizeenum * ) ;
   int (*g_mbus_fmt)(struct v4l2_subdev * , struct v4l2_mbus_framefmt * ) ;
   int (*try_mbus_fmt)(struct v4l2_subdev * , struct v4l2_mbus_framefmt * ) ;
   int (*s_mbus_fmt)(struct v4l2_subdev * , struct v4l2_mbus_framefmt * ) ;
   int (*g_mbus_config)(struct v4l2_subdev * , struct v4l2_mbus_config * ) ;
   int (*s_mbus_config)(struct v4l2_subdev * , struct v4l2_mbus_config const * ) ;
   int (*s_rx_buffer)(struct v4l2_subdev * , void * , unsigned int * ) ;
};
struct v4l2_subdev_vbi_ops {
   int (*decode_vbi_line)(struct v4l2_subdev * , struct v4l2_decode_vbi_line * ) ;
   int (*s_vbi_data)(struct v4l2_subdev * , struct v4l2_sliced_vbi_data const * ) ;
   int (*g_vbi_data)(struct v4l2_subdev * , struct v4l2_sliced_vbi_data * ) ;
   int (*g_sliced_vbi_cap)(struct v4l2_subdev * , struct v4l2_sliced_vbi_cap * ) ;
   int (*s_raw_fmt)(struct v4l2_subdev * , struct v4l2_vbi_format * ) ;
   int (*g_sliced_fmt)(struct v4l2_subdev * , struct v4l2_sliced_vbi_format * ) ;
   int (*s_sliced_fmt)(struct v4l2_subdev * , struct v4l2_sliced_vbi_format * ) ;
};
struct v4l2_subdev_sensor_ops {
   int (*g_skip_top_lines)(struct v4l2_subdev * , u32 * ) ;
   int (*g_skip_frames)(struct v4l2_subdev * , u32 * ) ;
};
enum v4l2_subdev_ir_mode {
    V4L2_SUBDEV_IR_MODE_PULSE_WIDTH = 0
} ;
struct v4l2_subdev_ir_parameters {
   unsigned int bytes_per_data_element ;
   enum v4l2_subdev_ir_mode mode ;
   bool enable ;
   bool interrupt_enable ;
   bool shutdown ;
   bool modulation ;
   u32 max_pulse_width ;
   unsigned int carrier_freq ;
   unsigned int duty_cycle ;
   bool invert_level ;
   bool invert_carrier_sense ;
   u32 noise_filter_min_width ;
   unsigned int carrier_range_lower ;
   unsigned int carrier_range_upper ;
   u32 resolution ;
};
struct v4l2_subdev_ir_ops {
   int (*rx_read)(struct v4l2_subdev * , u8 * , size_t , ssize_t * ) ;
   int (*rx_g_parameters)(struct v4l2_subdev * , struct v4l2_subdev_ir_parameters * ) ;
   int (*rx_s_parameters)(struct v4l2_subdev * , struct v4l2_subdev_ir_parameters * ) ;
   int (*tx_write)(struct v4l2_subdev * , u8 * , size_t , ssize_t * ) ;
   int (*tx_g_parameters)(struct v4l2_subdev * , struct v4l2_subdev_ir_parameters * ) ;
   int (*tx_s_parameters)(struct v4l2_subdev * , struct v4l2_subdev_ir_parameters * ) ;
};
struct v4l2_subdev_pad_ops {
   int (*enum_mbus_code)(struct v4l2_subdev * , struct v4l2_subdev_fh * , struct v4l2_subdev_mbus_code_enum * ) ;
   int (*enum_frame_size)(struct v4l2_subdev * , struct v4l2_subdev_fh * , struct v4l2_subdev_frame_size_enum * ) ;
   int (*enum_frame_interval)(struct v4l2_subdev * , struct v4l2_subdev_fh * , struct v4l2_subdev_frame_interval_enum * ) ;
   int (*get_fmt)(struct v4l2_subdev * , struct v4l2_subdev_fh * , struct v4l2_subdev_format * ) ;
   int (*set_fmt)(struct v4l2_subdev * , struct v4l2_subdev_fh * , struct v4l2_subdev_format * ) ;
   int (*set_crop)(struct v4l2_subdev * , struct v4l2_subdev_fh * , struct v4l2_subdev_crop * ) ;
   int (*get_crop)(struct v4l2_subdev * , struct v4l2_subdev_fh * , struct v4l2_subdev_crop * ) ;
   int (*get_selection)(struct v4l2_subdev * , struct v4l2_subdev_fh * , struct v4l2_subdev_selection * ) ;
   int (*set_selection)(struct v4l2_subdev * , struct v4l2_subdev_fh * , struct v4l2_subdev_selection * ) ;
   int (*get_edid)(struct v4l2_subdev * , struct v4l2_subdev_edid * ) ;
   int (*set_edid)(struct v4l2_subdev * , struct v4l2_subdev_edid * ) ;
   int (*link_validate)(struct v4l2_subdev * , struct media_link * , struct v4l2_subdev_format * ,
                        struct v4l2_subdev_format * ) ;
   int (*get_frame_desc)(struct v4l2_subdev * , unsigned int , struct v4l2_mbus_frame_desc * ) ;
   int (*set_frame_desc)(struct v4l2_subdev * , unsigned int , struct v4l2_mbus_frame_desc * ) ;
};
struct v4l2_subdev_ops {
   struct v4l2_subdev_core_ops const *core ;
   struct v4l2_subdev_tuner_ops const *tuner ;
   struct v4l2_subdev_audio_ops const *audio ;
   struct v4l2_subdev_video_ops const *video ;
   struct v4l2_subdev_vbi_ops const *vbi ;
   struct v4l2_subdev_ir_ops const *ir ;
   struct v4l2_subdev_sensor_ops const *sensor ;
   struct v4l2_subdev_pad_ops const *pad ;
};
struct v4l2_subdev_internal_ops {
   int (*registered)(struct v4l2_subdev * ) ;
   void (*unregistered)(struct v4l2_subdev * ) ;
   int (*open)(struct v4l2_subdev * , struct v4l2_subdev_fh * ) ;
   int (*close)(struct v4l2_subdev * , struct v4l2_subdev_fh * ) ;
};
struct regulator_bulk_data;
struct v4l2_subdev_platform_data {
   struct regulator_bulk_data *regulators ;
   int num_regulators ;
   void *host_priv ;
};
struct v4l2_subdev {
   struct media_entity entity ;
   struct list_head list ;
   struct module *owner ;
   u32 flags ;
   struct v4l2_device *v4l2_dev ;
   struct v4l2_subdev_ops const *ops ;
   struct v4l2_subdev_internal_ops const *internal_ops ;
   struct v4l2_ctrl_handler *ctrl_handler ;
   char name[32U] ;
   u32 grp_id ;
   void *dev_priv ;
   void *host_priv ;
   struct video_device *devnode ;
   struct device *dev ;
   struct list_head async_list ;
   struct v4l2_async_subdev *asd ;
   struct v4l2_async_notifier *notifier ;
   struct v4l2_subdev_platform_data *pdata ;
};
struct __anonstruct_pad_234 {
   struct v4l2_mbus_framefmt try_fmt ;
   struct v4l2_rect try_crop ;
   struct v4l2_rect try_compose ;
};
struct v4l2_subdev_fh {
   struct v4l2_fh vfh ;
   struct __anonstruct_pad_234 *pad ;
};
struct v4l2_device {
   struct device *dev ;
   struct media_device *mdev ;
   struct list_head subdevs ;
   spinlock_t lock ;
   char name[36U] ;
   void (*notify)(struct v4l2_subdev * , unsigned int , void * ) ;
   struct v4l2_ctrl_handler *ctrl_handler ;
   struct v4l2_prio_state prio ;
   struct mutex ioctl_lock ;
   struct kref ref ;
   void (*release)(struct v4l2_device * ) ;
};
struct v4l2_subscribed_event;
struct v4l2_kevent {
   struct list_head list ;
   struct v4l2_subscribed_event *sev ;
   struct v4l2_event event ;
};
struct v4l2_subscribed_event_ops {
   int (*add)(struct v4l2_subscribed_event * , unsigned int ) ;
   void (*del)(struct v4l2_subscribed_event * ) ;
   void (*replace)(struct v4l2_event * , struct v4l2_event const * ) ;
   void (*merge)(struct v4l2_event const * , struct v4l2_event * ) ;
};
struct v4l2_subscribed_event {
   struct list_head list ;
   u32 type ;
   u32 id ;
   u32 flags ;
   struct v4l2_fh *fh ;
   struct list_head node ;
   struct v4l2_subscribed_event_ops const *ops ;
   unsigned int elems ;
   unsigned int first ;
   unsigned int in_use ;
   struct v4l2_kevent events[] ;
};
struct scatterlist {
   unsigned long sg_magic ;
   unsigned long page_link ;
   unsigned int offset ;
   unsigned int length ;
   dma_addr_t dma_address ;
   unsigned int dma_length ;
};
struct sg_table {
   struct scatterlist *sgl ;
   unsigned int nents ;
   unsigned int orig_nents ;
};
struct dma_attrs {
   unsigned long flags[1U] ;
};
enum dma_data_direction {
    DMA_BIDIRECTIONAL = 0,
    DMA_TO_DEVICE = 1,
    DMA_FROM_DEVICE = 2,
    DMA_NONE = 3
} ;
struct dma_map_ops {
   void *(*alloc)(struct device * , size_t , dma_addr_t * , gfp_t , struct dma_attrs * ) ;
   void (*free)(struct device * , size_t , void * , dma_addr_t , struct dma_attrs * ) ;
   int (*mmap)(struct device * , struct vm_area_struct * , void * , dma_addr_t ,
               size_t , struct dma_attrs * ) ;
   int (*get_sgtable)(struct device * , struct sg_table * , void * , dma_addr_t ,
                      size_t , struct dma_attrs * ) ;
   dma_addr_t (*map_page)(struct device * , struct page * , unsigned long , size_t ,
                          enum dma_data_direction , struct dma_attrs * ) ;
   void (*unmap_page)(struct device * , dma_addr_t , size_t , enum dma_data_direction ,
                      struct dma_attrs * ) ;
   int (*map_sg)(struct device * , struct scatterlist * , int , enum dma_data_direction ,
                 struct dma_attrs * ) ;
   void (*unmap_sg)(struct device * , struct scatterlist * , int , enum dma_data_direction ,
                    struct dma_attrs * ) ;
   void (*sync_single_for_cpu)(struct device * , dma_addr_t , size_t , enum dma_data_direction ) ;
   void (*sync_single_for_device)(struct device * , dma_addr_t , size_t , enum dma_data_direction ) ;
   void (*sync_sg_for_cpu)(struct device * , struct scatterlist * , int , enum dma_data_direction ) ;
   void (*sync_sg_for_device)(struct device * , struct scatterlist * , int , enum dma_data_direction ) ;
   int (*mapping_error)(struct device * , dma_addr_t ) ;
   int (*dma_supported)(struct device * , u64 ) ;
   int (*set_dma_mask)(struct device * , u64 ) ;
   int is_phys ;
};
struct dma_buf;
struct dma_buf_attachment;
struct dma_buf_ops {
   int (*attach)(struct dma_buf * , struct device * , struct dma_buf_attachment * ) ;
   void (*detach)(struct dma_buf * , struct dma_buf_attachment * ) ;
   struct sg_table *(*map_dma_buf)(struct dma_buf_attachment * , enum dma_data_direction ) ;
   void (*unmap_dma_buf)(struct dma_buf_attachment * , struct sg_table * , enum dma_data_direction ) ;
   void (*release)(struct dma_buf * ) ;
   int (*begin_cpu_access)(struct dma_buf * , size_t , size_t , enum dma_data_direction ) ;
   void (*end_cpu_access)(struct dma_buf * , size_t , size_t , enum dma_data_direction ) ;
   void *(*kmap_atomic)(struct dma_buf * , unsigned long ) ;
   void (*kunmap_atomic)(struct dma_buf * , unsigned long , void * ) ;
   void *(*kmap)(struct dma_buf * , unsigned long ) ;
   void (*kunmap)(struct dma_buf * , unsigned long , void * ) ;
   int (*mmap)(struct dma_buf * , struct vm_area_struct * ) ;
   void *(*vmap)(struct dma_buf * ) ;
   void (*vunmap)(struct dma_buf * , void * ) ;
};
struct dma_buf {
   size_t size ;
   struct file *file ;
   struct list_head attachments ;
   struct dma_buf_ops const *ops ;
   struct mutex lock ;
   unsigned int vmapping_counter ;
   void *vmap_ptr ;
   char const *exp_name ;
   struct list_head list_node ;
   void *priv ;
};
struct dma_buf_attachment {
   struct dma_buf *dmabuf ;
   struct device *dev ;
   struct list_head node ;
   void *priv ;
};
struct vb2_fileio_data;
struct vb2_mem_ops {
   void *(*alloc)(void * , unsigned long , gfp_t ) ;
   void (*put)(void * ) ;
   struct dma_buf *(*get_dmabuf)(void * , unsigned long ) ;
   void *(*get_userptr)(void * , unsigned long , unsigned long , int ) ;
   void (*put_userptr)(void * ) ;
   void (*prepare)(void * ) ;
   void (*finish)(void * ) ;
   void *(*attach_dmabuf)(void * , struct dma_buf * , unsigned long , int ) ;
   void (*detach_dmabuf)(void * ) ;
   int (*map_dmabuf)(void * ) ;
   void (*unmap_dmabuf)(void * ) ;
   void *(*vaddr)(void * ) ;
   void *(*cookie)(void * ) ;
   unsigned int (*num_users)(void * ) ;
   int (*mmap)(void * , struct vm_area_struct * ) ;
};
struct vb2_plane {
   void *mem_priv ;
   struct dma_buf *dbuf ;
   unsigned int dbuf_mapped ;
};
enum vb2_buffer_state {
    VB2_BUF_STATE_DEQUEUED = 0,
    VB2_BUF_STATE_PREPARING = 1,
    VB2_BUF_STATE_PREPARED = 2,
    VB2_BUF_STATE_QUEUED = 3,
    VB2_BUF_STATE_ACTIVE = 4,
    VB2_BUF_STATE_DONE = 5,
    VB2_BUF_STATE_ERROR = 6
} ;
struct vb2_buffer {
   struct v4l2_buffer v4l2_buf ;
   struct v4l2_plane v4l2_planes[8U] ;
   struct vb2_queue *vb2_queue ;
   unsigned int num_planes ;
   enum vb2_buffer_state state ;
   struct list_head queued_entry ;
   struct list_head done_entry ;
   struct vb2_plane planes[8U] ;
};
struct vb2_ops {
   int (*queue_setup)(struct vb2_queue * , struct v4l2_format const * , unsigned int * ,
                      unsigned int * , unsigned int * , void ** ) ;
   void (*wait_prepare)(struct vb2_queue * ) ;
   void (*wait_finish)(struct vb2_queue * ) ;
   int (*buf_init)(struct vb2_buffer * ) ;
   int (*buf_prepare)(struct vb2_buffer * ) ;
   int (*buf_finish)(struct vb2_buffer * ) ;
   void (*buf_cleanup)(struct vb2_buffer * ) ;
   int (*start_streaming)(struct vb2_queue * , unsigned int ) ;
   int (*stop_streaming)(struct vb2_queue * ) ;
   void (*buf_queue)(struct vb2_buffer * ) ;
};
struct vb2_queue {
   enum v4l2_buf_type type ;
   unsigned int io_modes ;
   unsigned int io_flags ;
   struct mutex *lock ;
   struct v4l2_fh *owner ;
   struct vb2_ops const *ops ;
   struct vb2_mem_ops const *mem_ops ;
   void *drv_priv ;
   unsigned int buf_struct_size ;
   u32 timestamp_type ;
   gfp_t gfp_flags ;
   enum v4l2_memory memory ;
   struct vb2_buffer *bufs[32U] ;
   unsigned int num_buffers ;
   struct list_head queued_list ;
   atomic_t queued_count ;
   struct list_head done_list ;
   spinlock_t done_lock ;
   wait_queue_head_t done_wq ;
   void *alloc_ctx[8U] ;
   unsigned int plane_sizes[8U] ;
   unsigned int streaming : 1 ;
   unsigned int retry_start_streaming : 1 ;
   struct vb2_fileio_data *fileio ;
};
struct uvc_device;
struct uvc_control_info {
   struct list_head mappings ;
   __u8 entity[16U] ;
   __u8 index ;
   __u8 selector ;
   __u16 size ;
   __u32 flags ;
};
struct uvc_control_mapping {
   struct list_head list ;
   struct list_head ev_subs ;
   __u32 id ;
   __u8 name[32U] ;
   __u8 entity[16U] ;
   __u8 selector ;
   __u8 size ;
   __u8 offset ;
   enum v4l2_ctrl_type v4l2_type ;
   __u32 data_type ;
   struct uvc_menu_info *menu_info ;
   __u32 menu_count ;
   __u32 master_id ;
   __s32 master_manual ;
   __u32 slave_ids[2U] ;
   __s32 (*get)(struct uvc_control_mapping * , __u8 , __u8 const * ) ;
   void (*set)(struct uvc_control_mapping * , __s32 , __u8 * ) ;
};
struct uvc_entity;
struct uvc_control {
   struct uvc_entity *entity ;
   struct uvc_control_info info ;
   __u8 index ;
   __u8 dirty : 1 ;
   __u8 loaded : 1 ;
   __u8 modified : 1 ;
   __u8 cached : 1 ;
   __u8 initialized : 1 ;
   __u8 *uvc_data ;
};
struct uvc_format_desc {
   char *name ;
   __u8 guid[16U] ;
   __u32 fcc ;
};
struct __anonstruct_camera_236 {
   __u16 wObjectiveFocalLengthMin ;
   __u16 wObjectiveFocalLengthMax ;
   __u16 wOcularFocalLength ;
   __u8 bControlSize ;
   __u8 *bmControls ;
};
struct __anonstruct_media_237 {
   __u8 bControlSize ;
   __u8 *bmControls ;
   __u8 bTransportModeSize ;
   __u8 *bmTransportModes ;
};
struct __anonstruct_output_238 {
};
struct __anonstruct_processing_239 {
   __u16 wMaxMultiplier ;
   __u8 bControlSize ;
   __u8 *bmControls ;
   __u8 bmVideoStandards ;
};
struct __anonstruct_selector_240 {
};
struct __anonstruct_extension_241 {
   __u8 guidExtensionCode[16U] ;
   __u8 bNumControls ;
   __u8 bControlSize ;
   __u8 *bmControls ;
   __u8 *bmControlsType ;
};
union __anonunion____missing_field_name_235 {
   struct __anonstruct_camera_236 camera ;
   struct __anonstruct_media_237 media ;
   struct __anonstruct_output_238 output ;
   struct __anonstruct_processing_239 processing ;
   struct __anonstruct_selector_240 selector ;
   struct __anonstruct_extension_241 extension ;
};
struct uvc_entity {
   struct list_head list ;
   struct list_head chain ;
   unsigned int flags ;
   __u8 id ;
   __u16 type ;
   char name[64U] ;
   struct video_device *vdev ;
   struct v4l2_subdev subdev ;
   unsigned int num_pads ;
   unsigned int num_links ;
   struct media_pad *pads ;
   union __anonunion____missing_field_name_235 __annonCompField80 ;
   __u8 bNrInPins ;
   __u8 *baSourceID ;
   unsigned int ncontrols ;
   struct uvc_control *controls ;
};
struct uvc_frame {
   __u8 bFrameIndex ;
   __u8 bmCapabilities ;
   __u16 wWidth ;
   __u16 wHeight ;
   __u32 dwMinBitRate ;
   __u32 dwMaxBitRate ;
   __u32 dwMaxVideoFrameBufferSize ;
   __u8 bFrameIntervalType ;
   __u32 dwDefaultFrameInterval ;
   __u32 *dwFrameInterval ;
};
struct uvc_format {
   __u8 type ;
   __u8 index ;
   __u8 bpp ;
   __u8 colorspace ;
   __u32 fcc ;
   __u32 flags ;
   char name[32U] ;
   unsigned int nframes ;
   struct uvc_frame *frame ;
};
struct uvc_streaming_header {
   __u8 bNumFormats ;
   __u8 bEndpointAddress ;
   __u8 bTerminalLink ;
   __u8 bControlSize ;
   __u8 *bmaControls ;
   __u8 bmInfo ;
   __u8 bStillCaptureMethod ;
   __u8 bTriggerSupport ;
   __u8 bTriggerUsage ;
};
enum uvc_buffer_state {
    UVC_BUF_STATE_IDLE = 0,
    UVC_BUF_STATE_QUEUED = 1,
    UVC_BUF_STATE_ACTIVE = 2,
    UVC_BUF_STATE_READY = 3,
    UVC_BUF_STATE_DONE = 4,
    UVC_BUF_STATE_ERROR = 5
} ;
struct uvc_buffer {
   struct vb2_buffer buf ;
   struct list_head queue ;
   enum uvc_buffer_state state ;
   unsigned int error ;
   void *mem ;
   unsigned int length ;
   unsigned int bytesused ;
   u32 pts ;
};
struct uvc_video_queue {
   struct vb2_queue queue ;
   struct mutex mutex ;
   unsigned int flags ;
   unsigned int buf_used ;
   spinlock_t irqlock ;
   struct list_head irqqueue ;
};
struct uvc_video_chain {
   struct uvc_device *dev ;
   struct list_head list ;
   struct list_head entities ;
   struct uvc_entity *processing ;
   struct uvc_entity *selector ;
   struct mutex ctrl_mutex ;
   struct v4l2_prio_state prio ;
   u32 caps ;
};
struct uvc_stats_frame {
   unsigned int size ;
   unsigned int first_data ;
   unsigned int nb_packets ;
   unsigned int nb_empty ;
   unsigned int nb_invalid ;
   unsigned int nb_errors ;
   unsigned int nb_pts ;
   unsigned int nb_pts_diffs ;
   unsigned int last_pts_diff ;
   bool has_initial_pts ;
   bool has_early_pts ;
   u32 pts ;
   unsigned int nb_scr ;
   unsigned int nb_scr_diffs ;
   u16 scr_sof ;
   u32 scr_stc ;
};
struct uvc_stats_stream {
   struct timespec start_ts ;
   struct timespec stop_ts ;
   unsigned int nb_frames ;
   unsigned int nb_packets ;
   unsigned int nb_empty ;
   unsigned int nb_invalid ;
   unsigned int nb_errors ;
   unsigned int nb_pts_constant ;
   unsigned int nb_pts_early ;
   unsigned int nb_pts_initial ;
   unsigned int nb_scr_count_ok ;
   unsigned int nb_scr_diffs_ok ;
   unsigned int scr_sof_count ;
   unsigned int scr_sof ;
   unsigned int min_sof ;
   unsigned int max_sof ;
};
struct uvc_clock_sample {
   u32 dev_stc ;
   u16 dev_sof ;
   struct timespec host_ts ;
   u16 host_sof ;
};
struct uvc_clock {
   struct uvc_clock_sample *samples ;
   unsigned int head ;
   unsigned int count ;
   unsigned int size ;
   u16 last_sof ;
   u16 sof_offset ;
   spinlock_t lock ;
};
struct __anonstruct_bulk_242 {
   __u8 header[256U] ;
   unsigned int header_size ;
   int skip_payload ;
   __u32 payload_size ;
   __u32 max_payload_size ;
};
struct __anonstruct_stats_243 {
   struct uvc_stats_frame frame ;
   struct uvc_stats_stream stream ;
};
struct uvc_streaming {
   struct list_head list ;
   struct uvc_device *dev ;
   struct video_device *vdev ;
   struct uvc_video_chain *chain ;
   atomic_t active ;
   struct usb_interface *intf ;
   int intfnum ;
   __u16 maxpsize ;
   struct uvc_streaming_header header ;
   enum v4l2_buf_type type ;
   unsigned int nformats ;
   struct uvc_format *format ;
   struct uvc_streaming_control ctrl ;
   struct uvc_format *def_format ;
   struct uvc_format *cur_format ;
   struct uvc_frame *cur_frame ;
   struct mutex mutex ;
   unsigned int frozen : 1 ;
   struct uvc_video_queue queue ;
   void (*decode)(struct urb * , struct uvc_streaming * , struct uvc_buffer * ) ;
   struct __anonstruct_bulk_242 bulk ;
   struct urb *urb[5U] ;
   char *urb_buffer[5U] ;
   dma_addr_t urb_dma[5U] ;
   unsigned int urb_size ;
   __u32 sequence ;
   __u8 last_fid ;
   struct dentry *debugfs_dir ;
   struct __anonstruct_stats_243 stats ;
   struct uvc_clock clock ;
};
enum uvc_device_state {
    UVC_DEV_DISCONNECTED = 1
} ;
struct input_dev;
struct uvc_device {
   struct usb_device *udev ;
   struct usb_interface *intf ;
   unsigned long warnings ;
   __u32 quirks ;
   int intfnum ;
   char name[32U] ;
   enum uvc_device_state state ;
   struct mutex lock ;
   unsigned int users ;
   atomic_t nmappings ;
   struct media_device mdev ;
   struct v4l2_device vdev ;
   __u16 uvc_version ;
   __u32 clock_frequency ;
   struct list_head entities ;
   struct list_head chains ;
   struct list_head streams ;
   atomic_t nstreams ;
   struct usb_host_endpoint *int_ep ;
   struct urb *int_urb ;
   __u8 *status ;
   struct input_dev *input ;
   char input_phys[64U] ;
};
struct uvc_driver {
   struct usb_driver driver ;
};
struct ldv_struct_EMGentry_10 {
   int signal_pending ;
};
struct ldv_struct_usb_instance_5 {
   struct usb_driver *arg0 ;
   int signal_pending ;
};
typedef int ldv_func_ret_type;
enum hrtimer_restart;
struct v4l2_requestbuffers {
   __u32 count ;
   __u32 type ;
   __u32 memory ;
   __u32 reserved[2U] ;
};
typedef struct poll_table_struct poll_table;
struct exec_domain;
struct map_segment;
struct exec_domain {
   char const *name ;
   void (*handler)(int , struct pt_regs * ) ;
   unsigned char pers_low ;
   unsigned char pers_high ;
   unsigned long *signal_map ;
   unsigned long *signal_invmap ;
   struct map_segment *err_map ;
   struct map_segment *socktype_map ;
   struct map_segment *sockopt_map ;
   struct map_segment *af_map ;
   struct module *module ;
   struct exec_domain *next ;
};
struct __anonstruct_mm_segment_t_27 {
   unsigned long seg ;
};
typedef struct __anonstruct_mm_segment_t_27 mm_segment_t;
struct compat_timespec;
struct __anonstruct_futex_32 {
   u32 *uaddr ;
   u32 val ;
   u32 flags ;
   u32 bitset ;
   u64 time ;
   u32 *uaddr2 ;
};
struct __anonstruct_nanosleep_33 {
   clockid_t clockid ;
   struct timespec *rmtp ;
   struct compat_timespec *compat_rmtp ;
   u64 expires ;
};
struct pollfd;
struct __anonstruct_poll_34 {
   struct pollfd *ufds ;
   int nfds ;
   int has_timeout ;
   unsigned long tv_sec ;
   unsigned long tv_nsec ;
};
union __anonunion____missing_field_name_31 {
   struct __anonstruct_futex_32 futex ;
   struct __anonstruct_nanosleep_33 nanosleep ;
   struct __anonstruct_poll_34 poll ;
};
struct restart_block {
   long (*fn)(struct restart_block * ) ;
   union __anonunion____missing_field_name_31 __annonCompField20 ;
};
struct thread_info {
   struct task_struct *task ;
   struct exec_domain *exec_domain ;
   __u32 flags ;
   __u32 status ;
   __u32 cpu ;
   int saved_preempt_count ;
   mm_segment_t addr_limit ;
   struct restart_block restart_block ;
   void *sysenter_return ;
   unsigned int sig_on_uaccess_error : 1 ;
   unsigned int uaccess_err : 1 ;
};
enum hrtimer_restart;
struct __large_struct {
   unsigned long buf[100U] ;
};
struct iovec {
   void *iov_base ;
   __kernel_size_t iov_len ;
};
typedef s32 compat_time_t;
typedef u32 compat_caddr_t;
typedef s32 compat_long_t;
typedef u32 compat_uptr_t;
struct compat_timespec {
   compat_time_t tv_sec ;
   s32 tv_nsec ;
};
struct compat_robust_list {
   compat_uptr_t next ;
};
struct compat_robust_list_head {
   struct compat_robust_list list ;
   compat_long_t futex_offset ;
   compat_uptr_t list_op_pending ;
};
struct v4l2_capability {
   __u8 driver[16U] ;
   __u8 card[32U] ;
   __u8 bus_info[32U] ;
   __u32 version ;
   __u32 capabilities ;
   __u32 device_caps ;
   __u32 reserved[3U] ;
};
struct v4l2_fmtdesc {
   __u32 index ;
   __u32 type ;
   __u32 flags ;
   __u8 description[32U] ;
   __u32 pixelformat ;
   __u32 reserved[4U] ;
};
struct v4l2_jpegcompression {
   int quality ;
   int APPn ;
   int APP_len ;
   char APP_data[60U] ;
   int COM_len ;
   char COM_data[60U] ;
   __u32 jpeg_markers ;
};
struct v4l2_exportbuffer {
   __u32 type ;
   __u32 index ;
   __u32 plane ;
   __u32 flags ;
   __s32 fd ;
   __u32 reserved[11U] ;
};
struct v4l2_framebuffer {
   __u32 capability ;
   __u32 flags ;
   void *base ;
   struct v4l2_pix_format fmt ;
};
struct v4l2_selection {
   __u32 type ;
   __u32 target ;
   __u32 flags ;
   struct v4l2_rect r ;
   __u32 reserved[9U] ;
};
struct v4l2_input {
   __u32 index ;
   __u8 name[32U] ;
   __u32 type ;
   __u32 audioset ;
   __u32 tuner ;
   v4l2_std_id std ;
   __u32 status ;
   __u32 capabilities ;
   __u32 reserved[3U] ;
};
struct v4l2_output {
   __u32 index ;
   __u8 name[32U] ;
   __u32 type ;
   __u32 audioset ;
   __u32 modulator ;
   v4l2_std_id std ;
   __u32 capabilities ;
   __u32 reserved[3U] ;
};
struct v4l2_frequency_band {
   __u32 tuner ;
   __u32 type ;
   __u32 index ;
   __u32 capability ;
   __u32 rangelow ;
   __u32 rangehigh ;
   __u32 modulation ;
   __u32 reserved[9U] ;
};
struct v4l2_hw_freq_seek {
   __u32 tuner ;
   __u32 type ;
   __u32 seek_upward ;
   __u32 wrap_around ;
   __u32 spacing ;
   __u32 rangelow ;
   __u32 rangehigh ;
   __u32 reserved[5U] ;
};
struct v4l2_audio {
   __u32 index ;
   __u8 name[32U] ;
   __u32 capability ;
   __u32 mode ;
   __u32 reserved[2U] ;
};
struct v4l2_audioout {
   __u32 index ;
   __u8 name[32U] ;
   __u32 capability ;
   __u32 mode ;
   __u32 reserved[2U] ;
};
struct v4l2_enc_idx_entry {
   __u64 offset ;
   __u64 pts ;
   __u32 length ;
   __u32 flags ;
   __u32 reserved[2U] ;
};
struct v4l2_enc_idx {
   __u32 entries ;
   __u32 entries_cap ;
   __u32 reserved[4U] ;
   struct v4l2_enc_idx_entry entry[64U] ;
};
struct __anonstruct_raw_233 {
   __u32 data[8U] ;
};
union __anonunion____missing_field_name_232 {
   struct __anonstruct_raw_233 raw ;
};
struct v4l2_encoder_cmd {
   __u32 cmd ;
   __u32 flags ;
   union __anonunion____missing_field_name_232 __annonCompField74 ;
};
struct __anonstruct_stop_235 {
   __u64 pts ;
};
struct __anonstruct_start_236 {
   __s32 speed ;
   __u32 format ;
};
struct __anonstruct_raw_237 {
   __u32 data[16U] ;
};
union __anonunion____missing_field_name_234 {
   struct __anonstruct_stop_235 stop ;
   struct __anonstruct_start_236 start ;
   struct __anonstruct_raw_237 raw ;
};
struct v4l2_decoder_cmd {
   __u32 cmd ;
   __u32 flags ;
   union __anonunion____missing_field_name_234 __annonCompField75 ;
};
struct v4l2_dbg_chip_info {
   struct v4l2_dbg_match match ;
   char name[32U] ;
   __u32 flags ;
   __u32 reserved[32U] ;
};
struct v4l2_create_buffers {
   __u32 index ;
   __u32 count ;
   __u32 memory ;
   struct v4l2_format format ;
   __u32 reserved[8U] ;
};
struct pollfd {
   int fd ;
   short events ;
   short revents ;
};
struct v4l2_ctrl_helper;
struct v4l2_ctrl;
struct v4l2_ctrl_ops {
   int (*g_volatile_ctrl)(struct v4l2_ctrl * ) ;
   int (*try_ctrl)(struct v4l2_ctrl * ) ;
   int (*s_ctrl)(struct v4l2_ctrl * ) ;
};
union __anonunion____missing_field_name_254 {
   u32 step ;
   u32 menu_skip_mask ;
};
union __anonunion____missing_field_name_255 {
   char const * const *qmenu ;
   s64 const *qmenu_int ;
};
union __anonunion_cur_256 {
   s32 val ;
   s64 val64 ;
   char *string ;
};
union __anonunion____missing_field_name_257 {
   s32 val ;
   s64 val64 ;
   char *string ;
};
struct v4l2_ctrl {
   struct list_head node ;
   struct list_head ev_subs ;
   struct v4l2_ctrl_handler *handler ;
   struct v4l2_ctrl **cluster ;
   unsigned int ncontrols ;
   unsigned int done : 1 ;
   unsigned int is_new : 1 ;
   unsigned int is_private : 1 ;
   unsigned int is_auto : 1 ;
   unsigned int has_volatiles : 1 ;
   unsigned int call_notify : 1 ;
   unsigned int manual_mode_value : 8 ;
   struct v4l2_ctrl_ops const *ops ;
   u32 id ;
   char const *name ;
   enum v4l2_ctrl_type type ;
   s32 minimum ;
   s32 maximum ;
   s32 default_value ;
   union __anonunion____missing_field_name_254 __annonCompField80 ;
   union __anonunion____missing_field_name_255 __annonCompField81 ;
   unsigned long flags ;
   union __anonunion_cur_256 cur ;
   union __anonunion____missing_field_name_257 __annonCompField82 ;
   void *priv ;
};
struct v4l2_ctrl_ref {
   struct list_head node ;
   struct v4l2_ctrl_ref *next ;
   struct v4l2_ctrl *ctrl ;
   struct v4l2_ctrl_helper *helper ;
};
struct v4l2_ctrl_handler {
   struct mutex _lock ;
   struct mutex *lock ;
   struct list_head ctrls ;
   struct list_head ctrl_refs ;
   struct v4l2_ctrl_ref *cached ;
   struct v4l2_ctrl_ref **buckets ;
   void (*notify)(struct v4l2_ctrl * , void * ) ;
   void *notify_priv ;
   u16 nr_of_buckets ;
   int error ;
};
struct v4l2_ioctl_ops {
   int (*vidioc_querycap)(struct file * , void * , struct v4l2_capability * ) ;
   int (*vidioc_g_priority)(struct file * , void * , enum v4l2_priority * ) ;
   int (*vidioc_s_priority)(struct file * , void * , enum v4l2_priority ) ;
   int (*vidioc_enum_fmt_vid_cap)(struct file * , void * , struct v4l2_fmtdesc * ) ;
   int (*vidioc_enum_fmt_vid_overlay)(struct file * , void * , struct v4l2_fmtdesc * ) ;
   int (*vidioc_enum_fmt_vid_out)(struct file * , void * , struct v4l2_fmtdesc * ) ;
   int (*vidioc_enum_fmt_vid_cap_mplane)(struct file * , void * , struct v4l2_fmtdesc * ) ;
   int (*vidioc_enum_fmt_vid_out_mplane)(struct file * , void * , struct v4l2_fmtdesc * ) ;
   int (*vidioc_g_fmt_vid_cap)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_g_fmt_vid_overlay)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_g_fmt_vid_out)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_g_fmt_vid_out_overlay)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_g_fmt_vbi_cap)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_g_fmt_vbi_out)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_g_fmt_sliced_vbi_cap)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_g_fmt_sliced_vbi_out)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_g_fmt_vid_cap_mplane)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_g_fmt_vid_out_mplane)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_s_fmt_vid_cap)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_s_fmt_vid_overlay)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_s_fmt_vid_out)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_s_fmt_vid_out_overlay)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_s_fmt_vbi_cap)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_s_fmt_vbi_out)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_s_fmt_sliced_vbi_cap)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_s_fmt_sliced_vbi_out)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_s_fmt_vid_cap_mplane)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_s_fmt_vid_out_mplane)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_try_fmt_vid_cap)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_try_fmt_vid_overlay)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_try_fmt_vid_out)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_try_fmt_vid_out_overlay)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_try_fmt_vbi_cap)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_try_fmt_vbi_out)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_try_fmt_sliced_vbi_cap)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_try_fmt_sliced_vbi_out)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_try_fmt_vid_cap_mplane)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_try_fmt_vid_out_mplane)(struct file * , void * , struct v4l2_format * ) ;
   int (*vidioc_reqbufs)(struct file * , void * , struct v4l2_requestbuffers * ) ;
   int (*vidioc_querybuf)(struct file * , void * , struct v4l2_buffer * ) ;
   int (*vidioc_qbuf)(struct file * , void * , struct v4l2_buffer * ) ;
   int (*vidioc_expbuf)(struct file * , void * , struct v4l2_exportbuffer * ) ;
   int (*vidioc_dqbuf)(struct file * , void * , struct v4l2_buffer * ) ;
   int (*vidioc_create_bufs)(struct file * , void * , struct v4l2_create_buffers * ) ;
   int (*vidioc_prepare_buf)(struct file * , void * , struct v4l2_buffer * ) ;
   int (*vidioc_overlay)(struct file * , void * , unsigned int ) ;
   int (*vidioc_g_fbuf)(struct file * , void * , struct v4l2_framebuffer * ) ;
   int (*vidioc_s_fbuf)(struct file * , void * , struct v4l2_framebuffer const * ) ;
   int (*vidioc_streamon)(struct file * , void * , enum v4l2_buf_type ) ;
   int (*vidioc_streamoff)(struct file * , void * , enum v4l2_buf_type ) ;
   int (*vidioc_g_std)(struct file * , void * , v4l2_std_id * ) ;
   int (*vidioc_s_std)(struct file * , void * , v4l2_std_id ) ;
   int (*vidioc_querystd)(struct file * , void * , v4l2_std_id * ) ;
   int (*vidioc_enum_input)(struct file * , void * , struct v4l2_input * ) ;
   int (*vidioc_g_input)(struct file * , void * , unsigned int * ) ;
   int (*vidioc_s_input)(struct file * , void * , unsigned int ) ;
   int (*vidioc_enum_output)(struct file * , void * , struct v4l2_output * ) ;
   int (*vidioc_g_output)(struct file * , void * , unsigned int * ) ;
   int (*vidioc_s_output)(struct file * , void * , unsigned int ) ;
   int (*vidioc_queryctrl)(struct file * , void * , struct v4l2_queryctrl * ) ;
   int (*vidioc_g_ctrl)(struct file * , void * , struct v4l2_control * ) ;
   int (*vidioc_s_ctrl)(struct file * , void * , struct v4l2_control * ) ;
   int (*vidioc_g_ext_ctrls)(struct file * , void * , struct v4l2_ext_controls * ) ;
   int (*vidioc_s_ext_ctrls)(struct file * , void * , struct v4l2_ext_controls * ) ;
   int (*vidioc_try_ext_ctrls)(struct file * , void * , struct v4l2_ext_controls * ) ;
   int (*vidioc_querymenu)(struct file * , void * , struct v4l2_querymenu * ) ;
   int (*vidioc_enumaudio)(struct file * , void * , struct v4l2_audio * ) ;
   int (*vidioc_g_audio)(struct file * , void * , struct v4l2_audio * ) ;
   int (*vidioc_s_audio)(struct file * , void * , struct v4l2_audio const * ) ;
   int (*vidioc_enumaudout)(struct file * , void * , struct v4l2_audioout * ) ;
   int (*vidioc_g_audout)(struct file * , void * , struct v4l2_audioout * ) ;
   int (*vidioc_s_audout)(struct file * , void * , struct v4l2_audioout const * ) ;
   int (*vidioc_g_modulator)(struct file * , void * , struct v4l2_modulator * ) ;
   int (*vidioc_s_modulator)(struct file * , void * , struct v4l2_modulator const * ) ;
   int (*vidioc_cropcap)(struct file * , void * , struct v4l2_cropcap * ) ;
   int (*vidioc_g_crop)(struct file * , void * , struct v4l2_crop * ) ;
   int (*vidioc_s_crop)(struct file * , void * , struct v4l2_crop const * ) ;
   int (*vidioc_g_selection)(struct file * , void * , struct v4l2_selection * ) ;
   int (*vidioc_s_selection)(struct file * , void * , struct v4l2_selection * ) ;
   int (*vidioc_g_jpegcomp)(struct file * , void * , struct v4l2_jpegcompression * ) ;
   int (*vidioc_s_jpegcomp)(struct file * , void * , struct v4l2_jpegcompression const * ) ;
   int (*vidioc_g_enc_index)(struct file * , void * , struct v4l2_enc_idx * ) ;
   int (*vidioc_encoder_cmd)(struct file * , void * , struct v4l2_encoder_cmd * ) ;
   int (*vidioc_try_encoder_cmd)(struct file * , void * , struct v4l2_encoder_cmd * ) ;
   int (*vidioc_decoder_cmd)(struct file * , void * , struct v4l2_decoder_cmd * ) ;
   int (*vidioc_try_decoder_cmd)(struct file * , void * , struct v4l2_decoder_cmd * ) ;
   int (*vidioc_g_parm)(struct file * , void * , struct v4l2_streamparm * ) ;
   int (*vidioc_s_parm)(struct file * , void * , struct v4l2_streamparm * ) ;
   int (*vidioc_g_tuner)(struct file * , void * , struct v4l2_tuner * ) ;
   int (*vidioc_s_tuner)(struct file * , void * , struct v4l2_tuner const * ) ;
   int (*vidioc_g_frequency)(struct file * , void * , struct v4l2_frequency * ) ;
   int (*vidioc_s_frequency)(struct file * , void * , struct v4l2_frequency const * ) ;
   int (*vidioc_enum_freq_bands)(struct file * , void * , struct v4l2_frequency_band * ) ;
   int (*vidioc_g_sliced_vbi_cap)(struct file * , void * , struct v4l2_sliced_vbi_cap * ) ;
   int (*vidioc_log_status)(struct file * , void * ) ;
   int (*vidioc_s_hw_freq_seek)(struct file * , void * , struct v4l2_hw_freq_seek const * ) ;
   int (*vidioc_g_register)(struct file * , void * , struct v4l2_dbg_register * ) ;
   int (*vidioc_s_register)(struct file * , void * , struct v4l2_dbg_register const * ) ;
   int (*vidioc_g_chip_info)(struct file * , void * , struct v4l2_dbg_chip_info * ) ;
   int (*vidioc_enum_framesizes)(struct file * , void * , struct v4l2_frmsizeenum * ) ;
   int (*vidioc_enum_frameintervals)(struct file * , void * , struct v4l2_frmivalenum * ) ;
   int (*vidioc_s_dv_timings)(struct file * , void * , struct v4l2_dv_timings * ) ;
   int (*vidioc_g_dv_timings)(struct file * , void * , struct v4l2_dv_timings * ) ;
   int (*vidioc_query_dv_timings)(struct file * , void * , struct v4l2_dv_timings * ) ;
   int (*vidioc_enum_dv_timings)(struct file * , void * , struct v4l2_enum_dv_timings * ) ;
   int (*vidioc_dv_timings_cap)(struct file * , void * , struct v4l2_dv_timings_cap * ) ;
   int (*vidioc_subscribe_event)(struct v4l2_fh * , struct v4l2_event_subscription const * ) ;
   int (*vidioc_unsubscribe_event)(struct v4l2_fh * , struct v4l2_event_subscription const * ) ;
   long (*vidioc_default)(struct file * , void * , bool , unsigned int , void * ) ;
};
struct uvc_xu_control_mapping {
   __u32 id ;
   __u8 name[32U] ;
   __u8 entity[16U] ;
   __u8 selector ;
   __u8 size ;
   __u8 offset ;
   __u32 v4l2_type ;
   __u32 data_type ;
   struct uvc_menu_info *menu_info ;
   __u32 menu_count ;
   __u32 reserved[4U] ;
};
struct uvc_xu_control_query {
   __u8 unit ;
   __u8 selector ;
   __u8 query ;
   __u16 size ;
   __u8 *data ;
};
enum uvc_handle_state {
    UVC_HANDLE_PASSIVE = 0,
    UVC_HANDLE_ACTIVE = 1
} ;
struct uvc_fh {
   struct v4l2_fh vfh ;
   struct uvc_video_chain *chain ;
   struct uvc_streaming *stream ;
   enum uvc_handle_state state ;
};
struct uvc_xu_control_mapping32 {
   __u32 id ;
   __u8 name[32U] ;
   __u8 entity[16U] ;
   __u8 selector ;
   __u8 size ;
   __u8 offset ;
   __u32 v4l2_type ;
   __u32 data_type ;
   compat_caddr_t menu_info ;
   __u32 menu_count ;
   __u32 reserved[4U] ;
};
struct uvc_xu_control_query32 {
   __u8 unit ;
   __u8 selector ;
   __u8 query ;
   __u16 size ;
   compat_caddr_t data ;
};
union __anonunion_karg_274 {
   struct uvc_xu_control_mapping xmap ;
   struct uvc_xu_control_query xqry ;
};
typedef signed char s8;
enum hrtimer_restart;
enum hrtimer_restart;
struct uvc_ctrl_fixup {
   struct usb_device_id id ;
   u8 entity ;
   u8 selector ;
   u8 flags ;
};
struct uvc_ctrl_blacklist {
   struct usb_device_id id ;
   u8 index ;
};
enum hrtimer_restart;
struct input_device_id {
   kernel_ulong_t flags ;
   __u16 bustype ;
   __u16 vendor ;
   __u16 product ;
   __u16 version ;
   kernel_ulong_t evbit[1U] ;
   kernel_ulong_t keybit[12U] ;
   kernel_ulong_t relbit[1U] ;
   kernel_ulong_t absbit[1U] ;
   kernel_ulong_t mscbit[1U] ;
   kernel_ulong_t ledbit[1U] ;
   kernel_ulong_t sndbit[1U] ;
   kernel_ulong_t ffbit[2U] ;
   kernel_ulong_t swbit[1U] ;
   kernel_ulong_t driver_info ;
};
struct input_id {
   __u16 bustype ;
   __u16 vendor ;
   __u16 product ;
   __u16 version ;
};
struct input_absinfo {
   __s32 value ;
   __s32 minimum ;
   __s32 maximum ;
   __s32 fuzz ;
   __s32 flat ;
   __s32 resolution ;
};
struct input_keymap_entry {
   __u8 flags ;
   __u8 len ;
   __u16 index ;
   __u32 keycode ;
   __u8 scancode[32U] ;
};
struct ff_replay {
   __u16 length ;
   __u16 delay ;
};
struct ff_trigger {
   __u16 button ;
   __u16 interval ;
};
struct ff_envelope {
   __u16 attack_length ;
   __u16 attack_level ;
   __u16 fade_length ;
   __u16 fade_level ;
};
struct ff_constant_effect {
   __s16 level ;
   struct ff_envelope envelope ;
};
struct ff_ramp_effect {
   __s16 start_level ;
   __s16 end_level ;
   struct ff_envelope envelope ;
};
struct ff_condition_effect {
   __u16 right_saturation ;
   __u16 left_saturation ;
   __s16 right_coeff ;
   __s16 left_coeff ;
   __u16 deadband ;
   __s16 center ;
};
struct ff_periodic_effect {
   __u16 waveform ;
   __u16 period ;
   __s16 magnitude ;
   __s16 offset ;
   __u16 phase ;
   struct ff_envelope envelope ;
   __u32 custom_len ;
   __s16 *custom_data ;
};
struct ff_rumble_effect {
   __u16 strong_magnitude ;
   __u16 weak_magnitude ;
};
union __anonunion_u_191 {
   struct ff_constant_effect constant ;
   struct ff_ramp_effect ramp ;
   struct ff_periodic_effect periodic ;
   struct ff_condition_effect condition[2U] ;
   struct ff_rumble_effect rumble ;
};
struct ff_effect {
   __u16 type ;
   __s16 id ;
   __u16 direction ;
   struct ff_trigger trigger ;
   struct ff_replay replay ;
   union __anonunion_u_191 u ;
};
struct input_value {
   __u16 type ;
   __u16 code ;
   __s32 value ;
};
struct ff_device;
struct input_mt;
struct input_handle;
struct input_dev {
   char const *name ;
   char const *phys ;
   char const *uniq ;
   struct input_id id ;
   unsigned long propbit[1U] ;
   unsigned long evbit[1U] ;
   unsigned long keybit[12U] ;
   unsigned long relbit[1U] ;
   unsigned long absbit[1U] ;
   unsigned long mscbit[1U] ;
   unsigned long ledbit[1U] ;
   unsigned long sndbit[1U] ;
   unsigned long ffbit[2U] ;
   unsigned long swbit[1U] ;
   unsigned int hint_events_per_packet ;
   unsigned int keycodemax ;
   unsigned int keycodesize ;
   void *keycode ;
   int (*setkeycode)(struct input_dev * , struct input_keymap_entry const * , unsigned int * ) ;
   int (*getkeycode)(struct input_dev * , struct input_keymap_entry * ) ;
   struct ff_device *ff ;
   unsigned int repeat_key ;
   struct timer_list timer ;
   int rep[2U] ;
   struct input_mt *mt ;
   struct input_absinfo *absinfo ;
   unsigned long key[12U] ;
   unsigned long led[1U] ;
   unsigned long snd[1U] ;
   unsigned long sw[1U] ;
   int (*open)(struct input_dev * ) ;
   void (*close)(struct input_dev * ) ;
   int (*flush)(struct input_dev * , struct file * ) ;
   int (*event)(struct input_dev * , unsigned int , unsigned int , int ) ;
   struct input_handle *grab ;
   spinlock_t event_lock ;
   struct mutex mutex ;
   unsigned int users ;
   bool going_away ;
   struct device dev ;
   struct list_head h_list ;
   struct list_head node ;
   unsigned int num_vals ;
   unsigned int max_vals ;
   struct input_value *vals ;
   bool devres_managed ;
};
struct input_handler {
   void *private ;
   void (*event)(struct input_handle * , unsigned int , unsigned int , int ) ;
   void (*events)(struct input_handle * , struct input_value const * , unsigned int ) ;
   bool (*filter)(struct input_handle * , unsigned int , unsigned int , int ) ;
   bool (*match)(struct input_handler * , struct input_dev * ) ;
   int (*connect)(struct input_handler * , struct input_dev * , struct input_device_id const * ) ;
   void (*disconnect)(struct input_handle * ) ;
   void (*start)(struct input_handle * ) ;
   bool legacy_minors ;
   int minor ;
   char const *name ;
   struct input_device_id const *id_table ;
   struct list_head h_list ;
   struct list_head node ;
};
struct input_handle {
   void *private ;
   int open ;
   char const *name ;
   struct input_dev *dev ;
   struct input_handler *handler ;
   struct list_head d_node ;
   struct list_head h_node ;
};
struct ff_device {
   int (*upload)(struct input_dev * , struct ff_effect * , struct ff_effect * ) ;
   int (*erase)(struct input_dev * , int ) ;
   int (*playback)(struct input_dev * , int , int ) ;
   void (*set_gain)(struct input_dev * , u16 ) ;
   void (*set_autocenter)(struct input_dev * , u16 ) ;
   void (*destroy)(struct ff_device * ) ;
   void *private ;
   unsigned long ffbit[2U] ;
   struct mutex mutex ;
   int max_effects ;
   struct ff_effect *effects ;
   struct file *effect_owners[] ;
};
enum hrtimer_restart;
enum hrtimer_restart;
struct uvc_debugfs_buffer {
   size_t count ;
   char data[1024U] ;
};
enum hrtimer_restart;
struct device_private {
   void *driver_data ;
};
enum hrtimer_restart;
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
struct spi_master;
struct spi_device {
   struct device dev ;
   struct spi_master *master ;
   u32 max_speed_hz ;
   u8 chip_select ;
   u8 bits_per_word ;
   u16 mode ;
   int irq ;
   void *controller_state ;
   void *controller_data ;
   char modalias[32U] ;
   int cs_gpio ;
};
struct spi_message;
struct spi_transfer;
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
   bool cur_msg_prepared ;
   struct completion xfer_completion ;
   int (*prepare_transfer_hardware)(struct spi_master * ) ;
   int (*transfer_one_message)(struct spi_master * , struct spi_message * ) ;
   int (*unprepare_transfer_hardware)(struct spi_master * ) ;
   int (*prepare_message)(struct spi_master * , struct spi_message * ) ;
   int (*unprepare_message)(struct spi_master * , struct spi_message * ) ;
   void (*set_cs)(struct spi_device * , bool ) ;
   int (*transfer_one)(struct spi_master * , struct spi_device * , struct spi_transfer * ) ;
   int *cs_gpios ;
};
struct spi_transfer {
   void const *tx_buf ;
   void *rx_buf ;
   unsigned int len ;
   dma_addr_t tx_dma ;
   dma_addr_t rx_dma ;
   unsigned int cs_change : 1 ;
   unsigned int tx_nbits : 3 ;
   unsigned int rx_nbits : 3 ;
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
typedef int ldv_map;
struct ldv_thread_set {
   int number ;
   struct ldv_thread **threads ;
};
struct ldv_thread {
   int identifier ;
   void (*function)(void * ) ;
};
void ldv_atomic_inc(atomic_t *v ) ;
int ldv_atomic_dec_and_test(atomic_t *v ) ;
void *ldv_dev_get_drvdata(struct device const *dev ) ;
int ldv_dev_set_drvdata(struct device *dev , void *data ) ;
void *ldv_kzalloc(size_t size , gfp_t flags ) ;
void ldv_assume(int expression ) ;
void ldv_stop(void) ;
struct usb_device *ldv_usb_get_dev(struct usb_device *dev ) ;
void ldv_usb_put_dev(struct usb_device *dev ) ;
void ldv_check_return_value_probe(int retval ) ;
void ldv_initialize(void) ;
int ldv_post_init(int init_ret_val ) ;
extern void ldv_pre_probe(void) ;
int ldv_post_probe(int probe_ret_val ) ;
static int ldv_ldv_post_probe_28(int ldv_func_arg1 ) ;
extern int ldv_pre_usb_register_driver(void) ;
void ldv_check_final_state(void) ;
int ldv_undef_int(void) ;
void ldv_free(void *s ) ;
void *ldv_xmalloc(size_t size ) ;
__inline static void INIT_LIST_HEAD(struct list_head *list )
{
  {
  list->next = list;
  list->prev = list;
  return;
}
}
extern void __list_add(struct list_head * , struct list_head * , struct list_head * ) ;
__inline static void list_add_tail(struct list_head *new , struct list_head *head )
{
  {
  {
  __list_add(new, head->prev, head);
  }
  return;
}
}
__inline static int list_empty(struct list_head const *head )
{
  {
  return ((unsigned long )((struct list_head const *)head->next) == (unsigned long )head);
}
}
extern struct module __this_module ;
__inline static void set_bit(long nr , unsigned long volatile *addr )
{
  {
  __asm__ volatile (".pushsection .smp_locks,\"a\"\n.balign 4\n.long 671f - .\n.popsection\n671:\n\tlock; bts %1,%0": "+m" (*((long volatile *)addr)): "Ir" (nr): "memory");
  return;
}
}
__inline static int constant_test_bit(long nr , unsigned long const volatile *addr )
{
  {
  return ((int )((unsigned long )*(addr + (unsigned long )(nr >> 6)) >> ((int )nr & 63)) & 1);
}
}
__inline static __u32 __le32_to_cpup(__le32 const *p )
{
  {
  return ((__u32 )*p);
}
}
__inline static __u16 __le16_to_cpup(__le16 const *p )
{
  {
  return ((__u16 )*p);
}
}
extern int printk(char const * , ...) ;
extern int sprintf(char * , char const * , ...) ;
extern int snprintf(char * , size_t , char const * , ...) ;
extern void *memcpy(void * , void const * , size_t ) ;
extern void *memset(void * , int , size_t ) ;
extern int memcmp(void const * , void const * , size_t ) ;
extern size_t strlen(char const * ) ;
extern char *strcpy(char * , char const * ) ;
extern size_t strlcpy(char * , char const * , size_t ) ;
extern size_t strlcat(char * , char const * , __kernel_size_t ) ;
extern int strcasecmp(char const * , char const * ) ;
extern int strncasecmp(char const * , char const * , size_t ) ;
extern void *kmemdup(void const * , size_t , gfp_t ) ;
__inline static void atomic_set(atomic_t *v , int i )
{
  {
  v->counter = i;
  return;
}
}
__inline static void atomic_inc(atomic_t *v ) ;
__inline static int atomic_dec_and_test(atomic_t *v ) ;
extern void __mutex_init(struct mutex * , char const * , struct lock_class_key * ) ;
extern void mutex_lock_nested(struct mutex * , unsigned int ) ;
extern void mutex_unlock(struct mutex * ) ;
__inline static int usb_endpoint_dir_in(struct usb_endpoint_descriptor const *epd )
{
  {
  return ((int )((signed char )epd->bEndpointAddress) < 0);
}
}
__inline static int usb_endpoint_xfer_int(struct usb_endpoint_descriptor const *epd )
{
  {
  return (((int )epd->bmAttributes & 3) == 3);
}
}
__inline static int usb_endpoint_is_int_in(struct usb_endpoint_descriptor const *epd )
{
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  {
  {
  tmp = usb_endpoint_xfer_int(epd);
  }
  if (tmp != 0) {
    {
    tmp___0 = usb_endpoint_dir_in(epd);
    }
    if (tmp___0 != 0) {
      tmp___1 = 1;
    } else {
      tmp___1 = 0;
    }
  } else {
    tmp___1 = 0;
  }
  return (tmp___1);
}
}
static void *ldv_dev_get_drvdata_15(struct device const *dev ) ;
static void *ldv_dev_get_drvdata_18(struct device const *dev ) ;
static int ldv_dev_set_drvdata_16(struct device *dev , void *data ) ;
static int ldv_dev_set_drvdata_19(struct device *dev , void *data ) ;
__inline static void *usb_get_intfdata(struct usb_interface *intf )
{
  void *tmp ;
  {
  {
  tmp = ldv_dev_get_drvdata_15((struct device const *)(& intf->dev));
  }
  return (tmp);
}
}
__inline static void usb_set_intfdata(struct usb_interface *intf , void *data )
{
  {
  {
  ldv_dev_set_drvdata_16(& intf->dev, data);
  }
  return;
}
}
extern struct usb_interface *usb_get_intf(struct usb_interface * ) ;
extern void usb_put_intf(struct usb_interface * ) ;
__inline static struct usb_device *interface_to_usbdev(struct usb_interface *intf )
{
  struct device const *__mptr ;
  {
  __mptr = (struct device const *)intf->dev.parent;
  return ((struct usb_device *)__mptr + 0xffffffffffffff78UL);
}
}
static struct usb_device *ldv_usb_get_dev_25(struct usb_device *ldv_func_arg1 ) ;
static void ldv_usb_put_dev_24(struct usb_device *ldv_func_arg1 ) ;
extern void usb_enable_autosuspend(struct usb_device * ) ;
extern int usb_driver_claim_interface(struct usb_driver * , struct usb_interface * ,
                                      void * ) ;
extern void usb_driver_release_interface(struct usb_driver * , struct usb_interface * ) ;
extern struct usb_interface *usb_ifnum_to_if(struct usb_device const * , unsigned int ) ;
extern int usb_register_driver(struct usb_driver * , struct module * , char const * ) ;
static int ldv_usb_register_driver_26(struct usb_driver *ldv_func_arg1 , struct module *ldv_func_arg2 ,
                                      char const *ldv_func_arg3 ) ;
extern void usb_deregister(struct usb_driver * ) ;
static void ldv_usb_deregister_27(struct usb_driver *ldv_func_arg1 ) ;
extern int usb_string(struct usb_device * , int , char * , size_t ) ;
extern void kfree(void const * ) ;
extern void *ldv_malloc(size_t);
void *__kmalloc(size_t size, gfp_t t)
{
 return ldv_malloc(size);
}
void *ldv_malloc(size_t size ) ;
__inline static void *kmalloc(size_t size , gfp_t flags )
{
  void *tmp___2 ;
  {
  {
  tmp___2 = __kmalloc(size, flags);
  }
  return (tmp___2);
}
}
__inline static void *kzalloc(size_t size , gfp_t flags ) ;
__inline static u16 get_unaligned_le16(void const *p )
{
  __u16 tmp ;
  {
  {
  tmp = __le16_to_cpup((__le16 const *)p);
  }
  return (tmp);
}
}
__inline static u32 get_unaligned_le32(void const *p )
{
  __u32 tmp ;
  {
  {
  tmp = __le32_to_cpup((__le32 const *)p);
  }
  return (tmp);
}
}
extern void v4l2_prio_init(struct v4l2_prio_state * ) ;
extern int __video_register_device(struct video_device * , int , int , int , struct module * ) ;
__inline static int video_register_device(struct video_device *vdev , int type , int nr )
{
  int tmp ;
  {
  {
  tmp = __video_register_device(vdev, type, nr, 1, (vdev->fops)->owner);
  }
  return (tmp);
}
}
extern void video_unregister_device(struct video_device * ) ;
extern struct video_device *video_device_alloc(void) ;
extern void video_device_release(struct video_device * ) ;
__inline static void *video_get_drvdata(struct video_device *vdev )
{
  void *tmp ;
  {
  {
  tmp = ldv_dev_get_drvdata_18((struct device const *)(& vdev->dev));
  }
  return (tmp);
}
}
__inline static void video_set_drvdata(struct video_device *vdev , void *data )
{
  {
  {
  ldv_dev_set_drvdata_19(& vdev->dev, data);
  }
  return;
}
}
__inline static int media_devnode_is_registered(struct media_devnode *mdev )
{
  int tmp ;
  {
  {
  tmp = constant_test_bit(0L, (unsigned long const volatile *)(& mdev->flags));
  }
  return (tmp);
}
}
extern int media_device_register(struct media_device * ) ;
extern void media_device_unregister(struct media_device * ) ;
extern int v4l2_device_register(struct device * , struct v4l2_device * ) ;
extern void v4l2_device_unregister(struct v4l2_device * ) ;
unsigned int uvc_clock_param ;
unsigned int uvc_no_drop_param ;
unsigned int uvc_trace_param ;
unsigned int uvc_timeout_param ;
struct uvc_driver uvc_driver ;
struct uvc_entity *uvc_entity_by_id(struct uvc_device *dev , int id ) ;
struct v4l2_file_operations const uvc_fops ;
int uvc_mc_register_entities(struct uvc_video_chain *chain ) ;
void uvc_mc_cleanup_entity(struct uvc_entity *entity ) ;
int uvc_video_init(struct uvc_streaming *stream ) ;
int uvc_video_suspend(struct uvc_streaming *stream ) ;
int uvc_video_resume(struct uvc_streaming *stream , int reset ) ;
int uvc_status_init(struct uvc_device *dev ) ;
void uvc_status_cleanup(struct uvc_device *dev ) ;
int uvc_status_start(struct uvc_device *dev , gfp_t flags ) ;
void uvc_status_stop(struct uvc_device *dev ) ;
int uvc_ctrl_init_device(struct uvc_device *dev ) ;
void uvc_ctrl_cleanup_device(struct uvc_device *dev ) ;
int uvc_ctrl_resume_device(struct uvc_device *dev ) ;
void uvc_simplify_fraction(uint32_t *numerator , uint32_t *denominator , unsigned int n_terms ,
                           unsigned int threshold ) ;
uint32_t uvc_fraction_to_interval(uint32_t numerator , uint32_t denominator ) ;
struct usb_host_endpoint *uvc_find_endpoint(struct usb_host_interface *alts , __u8 epaddr ) ;
int uvc_debugfs_init(void) ;
void uvc_debugfs_cleanup(void) ;
int uvc_debugfs_init_stream(struct uvc_streaming *stream ) ;
void uvc_debugfs_cleanup_stream(struct uvc_streaming *stream ) ;
unsigned int uvc_clock_param = 1U;
static unsigned int uvc_quirks_param = 4294967295U;
unsigned int uvc_timeout_param = 5000U;
static struct uvc_format_desc uvc_fmts[16U] =
  { {(char *)"YUV 4:2:2 (YUYV)", {89U, 85U, 89U, 50U, 0U, 0U, 16U, 0U, 128U, 0U,
                                   0U, 170U, 0U, 56U, 155U, 113U}, 1448695129U},
        {(char *)"YUV 4:2:2 (YUYV)", {89U, 85U, 89U, 50U, 0U, 0U, 16U, 0U, 128U, 0U,
                                   0U, 0U, 0U, 56U, 155U, 113U}, 1448695129U},
        {(char *)"YUV 4:2:0 (NV12)", {78U, 86U, 49U, 50U, 0U, 0U, 16U, 0U, 128U, 0U,
                                   0U, 170U, 0U, 56U, 155U, 113U}, 842094158U},
        {(char *)"MJPEG", {77U, 74U, 80U, 71U, 0U, 0U, 16U, 0U, 128U, 0U, 0U, 170U, 0U,
                        56U, 155U, 113U}, 1196444237U},
        {(char *)"YVU 4:2:0 (YV12)", {89U, 86U, 49U, 50U, 0U, 0U, 16U, 0U, 128U, 0U,
                                   0U, 170U, 0U, 56U, 155U, 113U}, 842094169U},
        {(char *)"YUV 4:2:0 (I420)", {73U, 52U, 50U, 48U, 0U, 0U, 16U, 0U, 128U, 0U,
                                   0U, 170U, 0U, 56U, 155U, 113U}, 842093913U},
        {(char *)"YUV 4:2:0 (M420)", {77U, 52U, 50U, 48U, 0U, 0U, 16U, 0U, 128U, 0U,
                                   0U, 170U, 0U, 56U, 155U, 113U}, 808596557U},
        {(char *)"YUV 4:2:2 (UYVY)", {85U, 89U, 86U, 89U, 0U, 0U, 16U, 0U, 128U, 0U,
                                   0U, 170U, 0U, 56U, 155U, 113U}, 1498831189U},
        {(char *)"Greyscale 8-bit (Y800)", {89U, 56U, 48U, 48U, 0U, 0U, 16U, 0U, 128U,
                                         0U, 0U, 170U, 0U, 56U, 155U, 113U}, 1497715271U},
        {(char *)"Greyscale 8-bit (Y8  )",
      {89U, 56U, 32U, 32U, 0U, 0U, 16U, 0U, 128U, 0U, 0U, 170U, 0U, 56U, 155U, 113U},
      1497715271U},
        {(char *)"Greyscale 10-bit (Y10 )", {89U, 49U, 48U, 32U, 0U, 0U, 16U, 0U, 128U,
                                          0U, 0U, 170U, 0U, 56U, 155U, 113U}, 540029273U},
        {(char *)"Greyscale 12-bit (Y12 )",
      {89U, 49U, 50U, 32U, 0U, 0U, 16U, 0U, 128U, 0U, 0U, 170U, 0U, 56U, 155U, 113U},
      540160345U},
        {(char *)"Greyscale 16-bit (Y16 )", {89U, 49U, 54U, 32U, 0U, 0U, 16U, 0U, 128U,
                                          0U, 0U, 170U, 0U, 56U, 155U, 113U}, 540422489U},
        {(char *)"RGB Bayer",
      {66U, 89U, 56U, 32U, 0U, 0U, 16U, 0U, 128U, 0U, 0U, 170U, 0U, 56U, 155U, 113U},
      825770306U},
        {(char *)"RGB565", {82U, 71U, 66U, 80U, 0U, 0U, 16U, 0U, 128U, 0U, 0U, 170U,
                         0U, 56U, 155U, 113U}, 1346520914U},
        {(char *)"H.264", {72U, 50U, 54U, 52U, 0U, 0U, 16U, 0U, 128U, 0U, 0U, 170U, 0U,
                        56U, 155U, 113U}, 875967048U}};
struct usb_host_endpoint *uvc_find_endpoint(struct usb_host_interface *alts , __u8 epaddr )
{
  struct usb_host_endpoint *ep ;
  unsigned int i ;
  {
  i = 0U;
  goto ldv_34596;
  ldv_34595:
  ep = alts->endpoint + (unsigned long )i;
  if ((int )ep->desc.bEndpointAddress == (int )epaddr) {
    return (ep);
  } else {
  }
  i = i + 1U;
  ldv_34596: ;
  if (i < (unsigned int )alts->desc.bNumEndpoints) {
    goto ldv_34595;
  } else {
  }
  return ((struct usb_host_endpoint *)0);
}
}
static struct uvc_format_desc *uvc_format_by_guid(__u8 const *guid )
{
  unsigned int len ;
  unsigned int i ;
  int tmp ;
  {
  len = 16U;
  i = 0U;
  goto ldv_34606;
  ldv_34605:
  {
  tmp = memcmp((void const *)guid, (void const *)(& uvc_fmts[i].guid), 16UL);
  }
  if (tmp == 0) {
    return ((struct uvc_format_desc *)(& uvc_fmts) + (unsigned long )i);
  } else {
  }
  i = i + 1U;
  ldv_34606: ;
  if (i < len) {
    goto ldv_34605;
  } else {
  }
  return ((struct uvc_format_desc *)0);
}
}
static __u32 uvc_colorspace(__u8 const primaries )
{
  __u8 colorprimaries[6U] ;
  {
  colorprimaries[0] = 0U;
  colorprimaries[1] = 8U;
  colorprimaries[2] = 5U;
  colorprimaries[3] = 6U;
  colorprimaries[4] = 1U;
  colorprimaries[5] = 2U;
  if ((unsigned int )((unsigned char )primaries) <= 5U) {
    return ((__u32 )colorprimaries[(int )primaries]);
  } else {
  }
  return (0U);
}
}
void uvc_simplify_fraction(uint32_t *numerator , uint32_t *denominator , unsigned int n_terms ,
                           unsigned int threshold )
{
  uint32_t *an ;
  uint32_t x ;
  uint32_t y ;
  uint32_t r ;
  unsigned int i ;
  unsigned int n ;
  void *tmp ;
  {
  {
  tmp = kmalloc((unsigned long )n_terms * 4UL, 208U);
  an = (uint32_t *)tmp;
  }
  if ((unsigned long )an == (unsigned long )((uint32_t *)0U)) {
    return;
  } else {
  }
  x = *numerator;
  y = *denominator;
  n = 0U;
  goto ldv_34628;
  ldv_34627:
  *(an + (unsigned long )n) = x / y;
  if (*(an + (unsigned long )n) >= threshold) {
    if (n <= 1U) {
      n = n + 1U;
    } else {
    }
    goto ldv_34626;
  } else {
  }
  r = x - *(an + (unsigned long )n) * y;
  x = y;
  y = r;
  n = n + 1U;
  ldv_34628: ;
  if (n < n_terms && y != 0U) {
    goto ldv_34627;
  } else {
  }
  ldv_34626:
  x = 0U;
  y = 1U;
  i = n;
  goto ldv_34630;
  ldv_34629:
  r = y;
  y = *(an + (unsigned long )(i - 1U)) * y + x;
  x = r;
  i = i - 1U;
  ldv_34630: ;
  if (i != 0U) {
    goto ldv_34629;
  } else {
  }
  {
  *numerator = y;
  *denominator = x;
  kfree((void const *)an);
  }
  return;
}
}
uint32_t uvc_fraction_to_interval(uint32_t numerator , uint32_t denominator )
{
  uint32_t multiplier ;
  {
  if (denominator == 0U || numerator / denominator > 428U) {
    return (4294967295U);
  } else {
  }
  multiplier = 10000000U;
  goto ldv_34638;
  ldv_34637:
  multiplier = multiplier / 2U;
  denominator = denominator / 2U;
  ldv_34638: ;
  if (numerator > 4294967295U / multiplier) {
    goto ldv_34637;
  } else {
  }
  return (denominator != 0U ? (numerator * multiplier) / denominator : 0U);
}
}
struct uvc_entity *uvc_entity_by_id(struct uvc_device *dev , int id )
{
  struct uvc_entity *entity ;
  struct list_head const *__mptr ;
  struct list_head const *__mptr___0 ;
  {
  __mptr = (struct list_head const *)dev->entities.next;
  entity = (struct uvc_entity *)__mptr;
  goto ldv_34650;
  ldv_34649: ;
  if ((int )entity->id == id) {
    return (entity);
  } else {
  }
  __mptr___0 = (struct list_head const *)entity->list.next;
  entity = (struct uvc_entity *)__mptr___0;
  ldv_34650: ;
  if ((unsigned long )(& entity->list) != (unsigned long )(& dev->entities)) {
    goto ldv_34649;
  } else {
  }
  return ((struct uvc_entity *)0);
}
}
static struct uvc_entity *uvc_entity_by_reference(struct uvc_device *dev , int id ,
                                                  struct uvc_entity *entity )
{
  unsigned int i ;
  struct list_head const *__mptr ;
  struct list_head const *__mptr___0 ;
  struct list_head const *__mptr___1 ;
  {
  if ((unsigned long )entity == (unsigned long )((struct uvc_entity *)0)) {
    __mptr = (struct list_head const *)(& dev->entities);
    entity = (struct uvc_entity *)__mptr;
  } else {
  }
  __mptr___0 = (struct list_head const *)entity->list.next;
  entity = (struct uvc_entity *)__mptr___0;
  goto ldv_34668;
  ldv_34667:
  i = 0U;
  goto ldv_34665;
  ldv_34664: ;
  if ((int )*(entity->baSourceID + (unsigned long )i) == id) {
    return (entity);
  } else {
  }
  i = i + 1U;
  ldv_34665: ;
  if (i < (unsigned int )entity->bNrInPins) {
    goto ldv_34664;
  } else {
  }
  __mptr___1 = (struct list_head const *)entity->list.next;
  entity = (struct uvc_entity *)__mptr___1;
  ldv_34668: ;
  if ((unsigned long )(& entity->list) != (unsigned long )(& dev->entities)) {
    goto ldv_34667;
  } else {
  }
  return ((struct uvc_entity *)0);
}
}
static struct uvc_streaming *uvc_stream_by_id(struct uvc_device *dev , int id )
{
  struct uvc_streaming *stream ;
  struct list_head const *__mptr ;
  struct list_head const *__mptr___0 ;
  {
  __mptr = (struct list_head const *)dev->streams.next;
  stream = (struct uvc_streaming *)__mptr;
  goto ldv_34680;
  ldv_34679: ;
  if ((int )stream->header.bTerminalLink == id) {
    return (stream);
  } else {
  }
  __mptr___0 = (struct list_head const *)stream->list.next;
  stream = (struct uvc_streaming *)__mptr___0;
  ldv_34680: ;
  if ((unsigned long )(& stream->list) != (unsigned long )(& dev->streams)) {
    goto ldv_34679;
  } else {
  }
  return ((struct uvc_streaming *)0);
}
}
static int uvc_parse_format(struct uvc_device *dev , struct uvc_streaming *streaming ,
                            struct uvc_format *format , __u32 **intervals , unsigned char *buffer ,
                            int buflen )
{
  struct usb_interface *intf ;
  struct usb_host_interface *alts ;
  struct uvc_format_desc *fmtdesc ;
  struct uvc_frame *frame ;
  unsigned char const *start ;
  unsigned int interval ;
  unsigned int i ;
  unsigned int n ;
  __u8 ftype ;
  __u32 *tmp ;
  __u32 *tmp___0 ;
  __u32 _min1 ;
  __u32 _min2 ;
  __u32 _max1 ;
  __u32 _max2 ;
  __u32 tmp___1 ;
  {
  intf = streaming->intf;
  alts = intf->cur_altsetting;
  start = (unsigned char const *)buffer;
  format->type = *(buffer + 2UL);
  format->index = *(buffer + 3UL);
  {
  if ((int )*(buffer + 2UL) == 4) {
    goto case_4;
  } else {
  }
  if ((int )*(buffer + 2UL) == 16) {
    goto case_16;
  } else {
  }
  if ((int )*(buffer + 2UL) == 6) {
    goto case_6;
  } else {
  }
  if ((int )*(buffer + 2UL) == 12) {
    goto case_12;
  } else {
  }
  if ((int )*(buffer + 2UL) == 10) {
    goto case_10;
  } else {
  }
  if ((int )*(buffer + 2UL) == 18) {
    goto case_18;
  } else {
  }
  goto switch_default___0;
  case_4: ;
  case_16:
  n = (unsigned int )*(buffer + 2UL) == 4U ? 27U : 28U;
  if ((unsigned int )buflen < n) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: device %d videostreaming interface %d FORMAT error\n",
             (dev->udev)->devnum, (int )alts->desc.bInterfaceNumber);
      }
    } else {
    }
    return (-22);
  } else {
  }
  {
  fmtdesc = uvc_format_by_guid((__u8 const *)buffer + 5U);
  }
  if ((unsigned long )fmtdesc != (unsigned long )((struct uvc_format_desc *)0)) {
    {
    strlcpy((char *)(& format->name), (char const *)fmtdesc->name, 32UL);
    format->fcc = fmtdesc->fcc;
    }
  } else {
    {
    printk("\016uvcvideo: Unknown video format %pUl\n", buffer + 5UL);
    snprintf((char *)(& format->name), 32UL, "%pUl\n", buffer + 5UL);
    format->fcc = 0U;
    }
  }
  format->bpp = *(buffer + 21UL);
  if ((unsigned int )*(buffer + 2UL) == 4U) {
    ftype = 5U;
  } else {
    ftype = 17U;
    if ((unsigned int )*(buffer + 27UL) != 0U) {
      format->flags = 1U;
    } else {
    }
  }
  goto ldv_34701;
  case_6: ;
  if (buflen <= 10) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: device %d videostreaming interface %d FORMAT error\n",
             (dev->udev)->devnum, (int )alts->desc.bInterfaceNumber);
      }
    } else {
    }
    return (-22);
  } else {
  }
  {
  strlcpy((char *)(& format->name), "MJPEG", 32UL);
  format->fcc = 1196444237U;
  format->flags = 1U;
  format->bpp = 0U;
  ftype = 7U;
  }
  goto ldv_34701;
  case_12: ;
  if (buflen <= 8) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: device %d videostreaming interface %d FORMAT error\n",
             (dev->udev)->devnum, (int )alts->desc.bInterfaceNumber);
      }
    } else {
    }
    return (-22);
  } else {
  }
  {
  if (((int )*(buffer + 8UL) & 127) == 0) {
    goto case_0;
  } else {
  }
  if (((int )*(buffer + 8UL) & 127) == 1) {
    goto case_1;
  } else {
  }
  if (((int )*(buffer + 8UL) & 127) == 2) {
    goto case_2;
  } else {
  }
  goto switch_default;
  case_0:
  {
  strlcpy((char *)(& format->name), "SD-DV", 32UL);
  }
  goto ldv_34705;
  case_1:
  {
  strlcpy((char *)(& format->name), "SDL-DV", 32UL);
  }
  goto ldv_34705;
  case_2:
  {
  strlcpy((char *)(& format->name), "HD-DV", 32UL);
  }
  goto ldv_34705;
  switch_default: ;
  if ((uvc_trace_param & 2U) != 0U) {
    {
    printk("\017uvcvideo: device %d videostreaming interface %d: unknown DV format %u\n",
           (dev->udev)->devnum, (int )alts->desc.bInterfaceNumber, (int )*(buffer + 8UL));
    }
  } else {
  }
  return (-22);
  switch_break___0: ;
  }
  ldv_34705:
  {
  strlcat((char *)(& format->name), (int )((signed char )*(buffer + 8UL)) < 0 ? " 60Hz" : " 50Hz",
          32UL);
  format->fcc = 1685288548U;
  format->flags = 3U;
  format->bpp = 0U;
  ftype = 0U;
  frame = format->frame;
  memset((void *)format->frame, 0, 40UL);
  frame->bFrameIntervalType = 1U;
  frame->dwDefaultFrameInterval = 1U;
  frame->dwFrameInterval = *intervals;
  tmp = *intervals;
  *intervals = *intervals + 1;
  *tmp = 1U;
  format->nframes = 1U;
  }
  goto ldv_34701;
  case_10: ;
  case_18: ;
  switch_default___0: ;
  if ((uvc_trace_param & 2U) != 0U) {
    {
    printk("\017uvcvideo: device %d videostreaming interface %d unsupported format %u\n",
           (dev->udev)->devnum, (int )alts->desc.bInterfaceNumber, (int )*(buffer + 2UL));
    }
  } else {
  }
  return (-22);
  switch_break: ;
  }
  ldv_34701: ;
  if ((uvc_trace_param & 2U) != 0U) {
    {
    printk("\017uvcvideo: Found format %s.\n", (char *)(& format->name));
    }
  } else {
  }
  buflen = buflen - (int )*buffer;
  buffer = buffer + (unsigned long )*buffer;
  goto ldv_34725;
  ldv_34724:
  frame = format->frame + (unsigned long )format->nframes;
  if ((unsigned int )ftype != 17U) {
    n = buflen > 25 ? (unsigned int )*(buffer + 25UL) : 0U;
  } else {
    n = buflen > 21 ? (unsigned int )*(buffer + 21UL) : 0U;
  }
  n = n != 0U ? n : 3U;
  if ((unsigned int )buflen < n * 4U + 26U) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: device %d videostreaming interface %d FRAME error\n",
             (dev->udev)->devnum, (int )alts->desc.bInterfaceNumber);
      }
    } else {
    }
    return (-22);
  } else {
  }
  {
  frame->bFrameIndex = *(buffer + 3UL);
  frame->bmCapabilities = *(buffer + 4UL);
  frame->wWidth = get_unaligned_le16((void const *)buffer + 5U);
  frame->wHeight = get_unaligned_le16((void const *)buffer + 7U);
  frame->dwMinBitRate = get_unaligned_le32((void const *)buffer + 9U);
  frame->dwMaxBitRate = get_unaligned_le32((void const *)buffer + 13U);
  }
  if ((unsigned int )ftype != 17U) {
    {
    frame->dwMaxVideoFrameBufferSize = get_unaligned_le32((void const *)buffer + 17U);
    frame->dwDefaultFrameInterval = get_unaligned_le32((void const *)buffer + 21U);
    frame->bFrameIntervalType = *(buffer + 25UL);
    }
  } else {
    {
    frame->dwMaxVideoFrameBufferSize = 0U;
    frame->dwDefaultFrameInterval = get_unaligned_le32((void const *)buffer + 17U);
    frame->bFrameIntervalType = *(buffer + 21UL);
    }
  }
  frame->dwFrameInterval = *intervals;
  if ((format->flags & 1U) == 0U) {
    frame->dwMaxVideoFrameBufferSize = (__u32 )((((int )format->bpp * (int )frame->wWidth) * (int )frame->wHeight) / 8);
  } else {
  }
  i = 0U;
  goto ldv_34713;
  ldv_34712:
  {
  interval = get_unaligned_le32((void const *)buffer + (unsigned long )(i * 4U + 26U));
  tmp___0 = *intervals;
  *intervals = *intervals + 1;
  *tmp___0 = interval != 0U ? interval : 1U;
  i = i + 1U;
  }
  ldv_34713: ;
  if (i < n) {
    goto ldv_34712;
  } else {
  }
  n = n - ((unsigned int )frame->bFrameIntervalType != 0U ? 1U : 2U);
  _min1 = *(frame->dwFrameInterval + (unsigned long )n);
  _max1 = *(frame->dwFrameInterval);
  _max2 = frame->dwDefaultFrameInterval;
  _min2 = _max1 > _max2 ? _max1 : _max2;
  frame->dwDefaultFrameInterval = _min1 < _min2 ? _min1 : _min2;
  if ((dev->quirks & 512U) != 0U) {
    frame->bFrameIntervalType = 1U;
    *(frame->dwFrameInterval) = frame->dwDefaultFrameInterval;
  } else {
  }
  if ((uvc_trace_param & 2U) != 0U) {
    {
    printk("\017uvcvideo: - %ux%u (%u.%u fps)\n", (int )frame->wWidth, (int )frame->wHeight,
           10000000U / frame->dwDefaultFrameInterval, (100000000U / frame->dwDefaultFrameInterval) % 10U);
    }
  } else {
  }
  format->nframes = format->nframes + 1U;
  buflen = buflen - (int )*buffer;
  buffer = buffer + (unsigned long )*buffer;
  ldv_34725: ;
  if ((buflen > 2 && (unsigned int )*(buffer + 1UL) == 36U) && (int )*(buffer + 2UL) == (int )ftype) {
    goto ldv_34724;
  } else {
  }
  if ((buflen > 2 && (unsigned int )*(buffer + 1UL) == 36U) && (unsigned int )*(buffer + 2UL) == 3U) {
    buflen = buflen - (int )*buffer;
    buffer = buffer + (unsigned long )*buffer;
  } else {
  }
  if ((buflen > 2 && (unsigned int )*(buffer + 1UL) == 36U) && (unsigned int )*(buffer + 2UL) == 13U) {
    if (buflen <= 5) {
      if ((uvc_trace_param & 2U) != 0U) {
        {
        printk("\017uvcvideo: device %d videostreaming interface %d COLORFORMAT error\n",
               (dev->udev)->devnum, (int )alts->desc.bInterfaceNumber);
        }
      } else {
      }
      return (-22);
    } else {
    }
    {
    tmp___1 = uvc_colorspace((int )*(buffer + 3UL));
    format->colorspace = (__u8 )tmp___1;
    buflen = buflen - (int )*buffer;
    buffer = buffer + (unsigned long )*buffer;
    }
  } else {
  }
  return ((int )((unsigned int )((long )buffer) - (unsigned int )((long )start)));
}
}
static int uvc_parse_streaming(struct uvc_device *dev , struct usb_interface *intf )
{
  struct uvc_streaming *streaming ;
  struct uvc_format *format ;
  struct uvc_frame *frame ;
  struct usb_host_interface *alts ;
  unsigned char *_buffer ;
  unsigned char *buffer ;
  int _buflen ;
  int buflen ;
  unsigned int nformats ;
  unsigned int nframes ;
  unsigned int nintervals ;
  unsigned int size ;
  unsigned int i ;
  unsigned int n ;
  unsigned int p ;
  __u32 *interval ;
  __u16 psize ;
  int ret ;
  int tmp ;
  void *tmp___0 ;
  struct lock_class_key __key ;
  struct usb_host_endpoint *ep ;
  void *tmp___1 ;
  void *tmp___2 ;
  struct usb_host_endpoint *ep___0 ;
  {
  streaming = (struct uvc_streaming *)0;
  alts = intf->altsetting;
  buffer = alts->extra;
  buflen = alts->extralen;
  nformats = 0U;
  nframes = 0U;
  nintervals = 0U;
  ret = -22;
  if ((unsigned int )(intf->cur_altsetting)->desc.bInterfaceSubClass != 2U) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: device %d interface %d isn\'t a video streaming interface\n",
             (dev->udev)->devnum, (int )(intf->altsetting)->desc.bInterfaceNumber);
      }
    } else {
    }
    return (-22);
  } else {
  }
  {
  tmp = usb_driver_claim_interface(& uvc_driver.driver, intf, (void *)dev);
  }
  if (tmp != 0) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: device %d interface %d is already claimed\n", (dev->udev)->devnum,
             (int )(intf->altsetting)->desc.bInterfaceNumber);
      }
    } else {
    }
    return (-22);
  } else {
  }
  {
  tmp___0 = kzalloc(1928UL, 208U);
  streaming = (struct uvc_streaming *)tmp___0;
  }
  if ((unsigned long )streaming == (unsigned long )((struct uvc_streaming *)0)) {
    {
    usb_driver_release_interface(& uvc_driver.driver, intf);
    }
    return (-22);
  } else {
  }
  {
  __mutex_init(& streaming->mutex, "&streaming->mutex", & __key);
  streaming->dev = dev;
  streaming->intf = usb_get_intf(intf);
  streaming->intfnum = (int )(intf->cur_altsetting)->desc.bInterfaceNumber;
  }
  if (buflen == 0) {
    i = 0U;
    goto ldv_34754;
    ldv_34753:
    ep = alts->endpoint + (unsigned long )i;
    if (ep->extralen == 0) {
      goto ldv_34751;
    } else {
    }
    if (ep->extralen > 2 && (unsigned int )*(ep->extra + 1UL) == 36U) {
      if ((uvc_trace_param & 2U) != 0U) {
        {
        printk("\017uvcvideo: trying extra data from endpoint %u.\n", i);
        }
      } else {
      }
      buffer = (alts->endpoint + (unsigned long )i)->extra;
      buflen = (alts->endpoint + (unsigned long )i)->extralen;
      goto ldv_34752;
    } else {
    }
    ldv_34751:
    i = i + 1U;
    ldv_34754: ;
    if (i < (unsigned int )alts->desc.bNumEndpoints) {
      goto ldv_34753;
    } else {
    }
    ldv_34752: ;
  } else {
  }
  goto ldv_34756;
  ldv_34755:
  buflen = buflen - (int )*buffer;
  buffer = buffer + (unsigned long )*buffer;
  ldv_34756: ;
  if (buflen > 2 && (unsigned int )*(buffer + 1UL) != 36U) {
    goto ldv_34755;
  } else {
  }
  if (buflen <= 2) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: no class-specific streaming interface descriptors found.\n");
      }
    } else {
    }
    goto error;
  } else {
  }
  {
  if ((int )*(buffer + 2UL) == 2) {
    goto case_2;
  } else {
  }
  if ((int )*(buffer + 2UL) == 1) {
    goto case_1;
  } else {
  }
  goto switch_default;
  case_2:
  streaming->type = 2;
  size = 9U;
  goto ldv_34760;
  case_1:
  streaming->type = 1;
  size = 13U;
  goto ldv_34760;
  switch_default: ;
  if ((uvc_trace_param & 2U) != 0U) {
    {
    printk("\017uvcvideo: device %d videostreaming interface %d HEADER descriptor not found.\n",
           (dev->udev)->devnum, (int )alts->desc.bInterfaceNumber);
    }
  } else {
  }
  goto error;
  switch_break: ;
  }
  ldv_34760:
  p = buflen > 3 ? (unsigned int )*(buffer + 3UL) : 0U;
  n = (unsigned int )buflen >= size ? (unsigned int )*(buffer + (unsigned long )(size - 1U)) : 0U;
  if ((unsigned int )buflen < size + p * n) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: device %d videostreaming interface %d HEADER descriptor is invalid.\n",
             (dev->udev)->devnum, (int )alts->desc.bInterfaceNumber);
      }
    } else {
    }
    goto error;
  } else {
  }
  streaming->header.bNumFormats = (__u8 )p;
  streaming->header.bEndpointAddress = *(buffer + 6UL);
  if ((unsigned int )*(buffer + 2UL) == 1U) {
    streaming->header.bmInfo = *(buffer + 7UL);
    streaming->header.bTerminalLink = *(buffer + 8UL);
    streaming->header.bStillCaptureMethod = *(buffer + 9UL);
    streaming->header.bTriggerSupport = *(buffer + 10UL);
    streaming->header.bTriggerUsage = *(buffer + 11UL);
  } else {
    streaming->header.bTerminalLink = *(buffer + 7UL);
  }
  {
  streaming->header.bControlSize = (__u8 )n;
  tmp___1 = kmemdup((void const *)buffer + (unsigned long )size, (size_t )(p * n),
                    208U);
  streaming->header.bmaControls = (__u8 *)tmp___1;
  }
  if ((unsigned long )streaming->header.bmaControls == (unsigned long )((__u8 *)0U)) {
    ret = -12;
    goto error;
  } else {
  }
  buflen = buflen - (int )*buffer;
  buffer = buffer + (unsigned long )*buffer;
  _buffer = buffer;
  _buflen = buflen;
  goto ldv_34774;
  ldv_34773: ;
  {
  if ((int )*(_buffer + 2UL) == 4) {
    goto case_4;
  } else {
  }
  if ((int )*(_buffer + 2UL) == 6) {
    goto case_6;
  } else {
  }
  if ((int )*(_buffer + 2UL) == 16) {
    goto case_16;
  } else {
  }
  if ((int )*(_buffer + 2UL) == 12) {
    goto case_12;
  } else {
  }
  if ((int )*(_buffer + 2UL) == 10) {
    goto case_10;
  } else {
  }
  if ((int )*(_buffer + 2UL) == 18) {
    goto case_18;
  } else {
  }
  if ((int )*(_buffer + 2UL) == 5) {
    goto case_5;
  } else {
  }
  if ((int )*(_buffer + 2UL) == 7) {
    goto case_7;
  } else {
  }
  if ((int )*(_buffer + 2UL) == 17) {
    goto case_17;
  } else {
  }
  goto switch_break___0;
  case_4: ;
  case_6: ;
  case_16:
  nformats = nformats + 1U;
  goto ldv_34766;
  case_12:
  nformats = nformats + 1U;
  nframes = nframes + 1U;
  nintervals = nintervals + 1U;
  goto ldv_34766;
  case_10: ;
  case_18: ;
  if ((uvc_trace_param & 2U) != 0U) {
    {
    printk("\017uvcvideo: device %d videostreaming interface %d FORMAT %u is not supported.\n",
           (dev->udev)->devnum, (int )alts->desc.bInterfaceNumber, (int )*(_buffer + 2UL));
    }
  } else {
  }
  goto ldv_34766;
  case_5: ;
  case_7:
  nframes = nframes + 1U;
  if (_buflen > 25) {
    nintervals = nintervals + ((unsigned int )*(_buffer + 25UL) != 0U ? (unsigned int )*(_buffer + 25UL) : 3U);
  } else {
  }
  goto ldv_34766;
  case_17:
  nframes = nframes + 1U;
  if (_buflen > 21) {
    nintervals = nintervals + ((unsigned int )*(_buffer + 21UL) != 0U ? (unsigned int )*(_buffer + 21UL) : 3U);
  } else {
  }
  goto ldv_34766;
  switch_break___0: ;
  }
  ldv_34766:
  _buflen = _buflen - (int )*_buffer;
  _buffer = _buffer + (unsigned long )*_buffer;
  ldv_34774: ;
  if (_buflen > 2 && (unsigned int )*(_buffer + 1UL) == 36U) {
    goto ldv_34773;
  } else {
  }
  if (nformats == 0U) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: device %d videostreaming interface %d has no supported formats defined.\n",
             (dev->udev)->devnum, (int )alts->desc.bInterfaceNumber);
      }
    } else {
    }
    goto error;
  } else {
  }
  {
  size = (nformats * 56U + nframes * 40U) + nintervals * 4U;
  tmp___2 = kzalloc((size_t )size, 208U);
  format = (struct uvc_format *)tmp___2;
  }
  if ((unsigned long )format == (unsigned long )((struct uvc_format *)0)) {
    ret = -12;
    goto error;
  } else {
  }
  frame = (struct uvc_frame *)format + (unsigned long )nformats;
  interval = (__u32 *)frame + (unsigned long )nframes;
  streaming->format = format;
  streaming->nformats = nformats;
  goto ldv_34780;
  ldv_34783: ;
  {
  if ((int )*(buffer + 2UL) == 4) {
    goto case_4___0;
  } else {
  }
  if ((int )*(buffer + 2UL) == 6) {
    goto case_6___0;
  } else {
  }
  if ((int )*(buffer + 2UL) == 12) {
    goto case_12___0;
  } else {
  }
  if ((int )*(buffer + 2UL) == 16) {
    goto case_16___0;
  } else {
  }
  goto switch_default___0;
  case_4___0: ;
  case_6___0: ;
  case_12___0: ;
  case_16___0:
  {
  format->frame = frame;
  ret = uvc_parse_format(dev, streaming, format, & interval, buffer, buflen);
  }
  if (ret < 0) {
    goto error;
  } else {
  }
  frame = frame + (unsigned long )format->nframes;
  format = format + 1;
  buflen = buflen - ret;
  buffer = buffer + (unsigned long )ret;
  goto ldv_34780;
  switch_default___0: ;
  goto ldv_34782;
  switch_break___1: ;
  }
  ldv_34782:
  buflen = buflen - (int )*buffer;
  buffer = buffer + (unsigned long )*buffer;
  ldv_34780: ;
  if (buflen > 2 && (unsigned int )*(buffer + 1UL) == 36U) {
    goto ldv_34783;
  } else {
  }
  if (buflen != 0) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: device %d videostreaming interface %d has %u bytes of trailing descriptor garbage.\n",
             (dev->udev)->devnum, (int )alts->desc.bInterfaceNumber, buflen);
      }
    } else {
    }
  } else {
  }
  i = 0U;
  goto ldv_34788;
  ldv_34787:
  {
  alts = intf->altsetting + (unsigned long )i;
  ep___0 = uvc_find_endpoint(alts, (int )streaming->header.bEndpointAddress);
  }
  if ((unsigned long )ep___0 == (unsigned long )((struct usb_host_endpoint *)0)) {
    goto ldv_34786;
  } else {
  }
  psize = ep___0->desc.wMaxPacketSize;
  psize = ((unsigned int )psize & 2047U) * (unsigned int )((__u16 )((((int )psize >> 11) & 3) + 1));
  if ((int )psize > (int )streaming->maxpsize) {
    streaming->maxpsize = psize;
  } else {
  }
  ldv_34786:
  i = i + 1U;
  ldv_34788: ;
  if (i < intf->num_altsetting) {
    goto ldv_34787;
  } else {
  }
  {
  list_add_tail(& streaming->list, & dev->streams);
  }
  return (0);
  error:
  {
  usb_driver_release_interface(& uvc_driver.driver, intf);
  usb_put_intf(intf);
  kfree((void const *)streaming->format);
  kfree((void const *)streaming->header.bmaControls);
  kfree((void const *)streaming);
  }
  return (ret);
}
}
static struct uvc_entity *uvc_alloc_entity(u16 type , u8 id , unsigned int num_pads ,
                                           unsigned int extra_size )
{
  struct uvc_entity *entity ;
  unsigned int num_inputs ;
  unsigned int size ;
  unsigned int i ;
  void *tmp ;
  {
  {
  extra_size = (extra_size + 23U) & 4294967272U;
  num_inputs = (int )((short )type) < 0 ? num_pads : num_pads - 1U;
  size = ((extra_size + num_pads * 24U) + num_inputs) + 504U;
  tmp = kzalloc((size_t )size, 208U);
  entity = (struct uvc_entity *)tmp;
  }
  if ((unsigned long )entity == (unsigned long )((struct uvc_entity *)0)) {
    return ((struct uvc_entity *)0);
  } else {
  }
  entity->id = id;
  entity->type = type;
  entity->num_links = 0U;
  entity->num_pads = num_pads;
  entity->pads = (struct media_pad *)entity + ((unsigned long )extra_size + 1UL);
  i = 0U;
  goto ldv_34801;
  ldv_34800:
  (entity->pads + (unsigned long )i)->flags = 1UL;
  i = i + 1U;
  ldv_34801: ;
  if (i < num_inputs) {
    goto ldv_34800;
  } else {
  }
  if (((int )entity->type & 65280) == 0 || (int )((short )entity->type) >= 0) {
    (entity->pads + (unsigned long )(num_pads - 1U))->flags = 2UL;
  } else {
  }
  entity->bNrInPins = (__u8 )num_inputs;
  entity->baSourceID = (__u8 *)entity->pads + (unsigned long )num_pads;
  return (entity);
}
}
static int uvc_parse_vendor_control(struct uvc_device *dev , unsigned char const *buffer ,
                                    int buflen )
{
  struct usb_device *udev ;
  struct usb_host_interface *alts ;
  struct uvc_entity *unit ;
  unsigned int n ;
  unsigned int p ;
  int handled ;
  {
  udev = dev->udev;
  alts = (dev->intf)->cur_altsetting;
  handled = 0;
  {
  if ((int )(dev->udev)->descriptor.idVendor == 1133) {
    goto case_1133;
  } else {
  }
  goto switch_break;
  case_1133: ;
  if ((unsigned int )((unsigned char )*(buffer + 1UL)) != 65U || (unsigned int )((unsigned char )*(buffer + 2UL)) != 1U) {
    goto ldv_34815;
  } else {
  }
  p = buflen > 21 ? (unsigned int )*(buffer + 21UL) : 0U;
  n = (unsigned int )buflen >= p + 25U ? (unsigned int )*(buffer + (unsigned long )(p + 22U)) : 0U;
  if ((unsigned int )buflen < (p + n * 2U) + 25U) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: device %d videocontrol interface %d EXTENSION_UNIT error\n",
             udev->devnum, (int )alts->desc.bInterfaceNumber);
      }
    } else {
    }
    goto ldv_34815;
  } else {
  }
  {
  unit = uvc_alloc_entity(6, (int )*(buffer + 3UL), p + 1U, n * 2U);
  }
  if ((unsigned long )unit == (unsigned long )((struct uvc_entity *)0)) {
    return (-12);
  } else {
  }
  {
  memcpy((void *)(& unit->__annonCompField80.extension.guidExtensionCode), (void const *)buffer + 4U,
         16UL);
  unit->__annonCompField80.extension.bNumControls = *(buffer + 20UL);
  memcpy((void *)unit->baSourceID, (void const *)buffer + 22U, (size_t )p);
  unit->__annonCompField80.extension.bControlSize = *(buffer + (unsigned long )(p + 22U));
  unit->__annonCompField80.extension.bmControls = (__u8 *)unit + 504UL;
  unit->__annonCompField80.extension.bmControlsType = (__u8 *)unit + ((unsigned long )n + 504UL);
  memcpy((void *)unit->__annonCompField80.extension.bmControls, (void const *)buffer + (unsigned long )(p + 23U),
         (size_t )(n * 2U));
  }
  if ((unsigned int )((unsigned char )*(buffer + (unsigned long )((p + n * 2U) + 24U))) != 0U) {
    {
    usb_string(udev, (int )*(buffer + (unsigned long )((p + n * 2U) + 24U)), (char *)(& unit->name),
               64UL);
    }
  } else {
    {
    sprintf((char *)(& unit->name), "Extension %u", (int )*(buffer + 3UL));
    }
  }
  {
  list_add_tail(& unit->list, & dev->entities);
  handled = 1;
  }
  goto ldv_34815;
  switch_break: ;
  }
  ldv_34815: ;
  return (handled);
}
}
static int uvc_parse_standard_control(struct uvc_device *dev , unsigned char const *buffer ,
                                      int buflen )
{
  struct usb_device *udev ;
  struct uvc_entity *unit ;
  struct uvc_entity *term ;
  struct usb_interface *intf ;
  struct usb_host_interface *alts ;
  unsigned int i ;
  unsigned int n ;
  unsigned int p ;
  unsigned int len ;
  __u16 type ;
  {
  udev = dev->udev;
  alts = (dev->intf)->cur_altsetting;
  {
  if ((int )*(buffer + 2UL) == 1) {
    goto case_1;
  } else {
  }
  if ((int )*(buffer + 2UL) == 2) {
    goto case_2;
  } else {
  }
  if ((int )*(buffer + 2UL) == 3) {
    goto case_3;
  } else {
  }
  if ((int )*(buffer + 2UL) == 4) {
    goto case_4;
  } else {
  }
  if ((int )*(buffer + 2UL) == 5) {
    goto case_5;
  } else {
  }
  if ((int )*(buffer + 2UL) == 6) {
    goto case_6;
  } else {
  }
  goto switch_default;
  case_1:
  n = buflen > 11 ? (unsigned int )*(buffer + 11UL) : 0U;
  if (buflen <= 11 || (unsigned int )buflen < n + 12U) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: device %d videocontrol interface %d HEADER error\n", udev->devnum,
             (int )alts->desc.bInterfaceNumber);
      }
    } else {
    }
    return (-22);
  } else {
  }
  {
  dev->uvc_version = get_unaligned_le16((void const *)buffer + 3U);
  dev->clock_frequency = get_unaligned_le32((void const *)buffer + 7U);
  i = 0U;
  }
  goto ldv_34834;
  ldv_34833:
  {
  intf = usb_ifnum_to_if((struct usb_device const *)udev, (unsigned int )*(buffer + (unsigned long )(i + 12U)));
  }
  if ((unsigned long )intf == (unsigned long )((struct usb_interface *)0)) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: device %d interface %d doesn\'t exists\n", udev->devnum,
             i);
      }
    } else {
    }
    goto ldv_34832;
  } else {
  }
  {
  uvc_parse_streaming(dev, intf);
  }
  ldv_34832:
  i = i + 1U;
  ldv_34834: ;
  if (i < n) {
    goto ldv_34833;
  } else {
  }
  goto ldv_34836;
  case_2: ;
  if (buflen <= 7) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: device %d videocontrol interface %d INPUT_TERMINAL error\n",
             udev->devnum, (int )alts->desc.bInterfaceNumber);
      }
    } else {
    }
    return (-22);
  } else {
  }
  {
  type = get_unaligned_le16((void const *)buffer + 4U);
  }
  if (((int )type & 65280) == 0) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: device %d videocontrol interface %d INPUT_TERMINAL %d has invalid type 0x%04x, skipping\n",
             udev->devnum, (int )alts->desc.bInterfaceNumber, (int )*(buffer + 3UL),
             (int )type);
      }
    } else {
    }
    return (0);
  } else {
  }
  n = 0U;
  p = 0U;
  len = 8U;
  if ((unsigned int )type == 513U) {
    n = buflen > 14 ? (unsigned int )*(buffer + 14UL) : 0U;
    len = 15U;
  } else
  if ((unsigned int )type == 514U) {
    n = buflen > 8 ? (unsigned int )*(buffer + 8UL) : 0U;
    p = (unsigned int )buflen >= n + 10U ? (unsigned int )*(buffer + (unsigned long )(n + 9U)) : 0U;
    len = 10U;
  } else {
  }
  if ((unsigned int )buflen < (len + n) + p) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: device %d videocontrol interface %d INPUT_TERMINAL error\n",
             udev->devnum, (int )alts->desc.bInterfaceNumber);
      }
    } else {
    }
    return (-22);
  } else {
  }
  {
  term = uvc_alloc_entity((int )type, (int )*(buffer + 3UL), 1U, n + p);
  }
  if ((unsigned long )term == (unsigned long )((struct uvc_entity *)0)) {
    return (-12);
  } else {
  }
  if (((int )term->type & 32767) == 513) {
    {
    term->__annonCompField80.camera.bControlSize = (__u8 )n;
    term->__annonCompField80.camera.bmControls = (__u8 *)term + 504UL;
    term->__annonCompField80.camera.wObjectiveFocalLengthMin = get_unaligned_le16((void const *)buffer + 8U);
    term->__annonCompField80.camera.wObjectiveFocalLengthMax = get_unaligned_le16((void const *)buffer + 10U);
    term->__annonCompField80.camera.wOcularFocalLength = get_unaligned_le16((void const *)buffer + 12U);
    memcpy((void *)term->__annonCompField80.camera.bmControls, (void const *)buffer + 15U,
           (size_t )n);
    }
  } else
  if (((int )term->type & 32767) == 514) {
    {
    term->__annonCompField80.media.bControlSize = (__u8 )n;
    term->__annonCompField80.media.bmControls = (__u8 *)term + 504UL;
    term->__annonCompField80.media.bTransportModeSize = (__u8 )p;
    term->__annonCompField80.media.bmTransportModes = (__u8 *)term + ((unsigned long )n + 504UL);
    memcpy((void *)term->__annonCompField80.media.bmControls, (void const *)buffer + 9U,
           (size_t )n);
    memcpy((void *)term->__annonCompField80.media.bmTransportModes, (void const *)buffer + (unsigned long )(n + 10U),
           (size_t )p);
    }
  } else {
  }
  if ((unsigned int )((unsigned char )*(buffer + 7UL)) != 0U) {
    {
    usb_string(udev, (int )*(buffer + 7UL), (char *)(& term->name), 64UL);
    }
  } else
  if (((int )term->type & 32767) == 513) {
    {
    sprintf((char *)(& term->name), "Camera %u", (int )*(buffer + 3UL));
    }
  } else
  if (((int )term->type & 32767) == 514) {
    {
    sprintf((char *)(& term->name), "Media %u", (int )*(buffer + 3UL));
    }
  } else {
    {
    sprintf((char *)(& term->name), "Input %u", (int )*(buffer + 3UL));
    }
  }
  {
  list_add_tail(& term->list, & dev->entities);
  }
  goto ldv_34836;
  case_3: ;
  if (buflen <= 8) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: device %d videocontrol interface %d OUTPUT_TERMINAL error\n",
             udev->devnum, (int )alts->desc.bInterfaceNumber);
      }
    } else {
    }
    return (-22);
  } else {
  }
  {
  type = get_unaligned_le16((void const *)buffer + 4U);
  }
  if (((int )type & 65280) == 0) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: device %d videocontrol interface %d OUTPUT_TERMINAL %d has invalid type 0x%04x, skipping\n",
             udev->devnum, (int )alts->desc.bInterfaceNumber, (int )*(buffer + 3UL),
             (int )type);
      }
    } else {
    }
    return (0);
  } else {
  }
  {
  term = uvc_alloc_entity((int )((unsigned int )type | 32768U), (int )*(buffer + 3UL),
                          1U, 0U);
  }
  if ((unsigned long )term == (unsigned long )((struct uvc_entity *)0)) {
    return (-12);
  } else {
  }
  {
  memcpy((void *)term->baSourceID, (void const *)buffer + 7U, 1UL);
  }
  if ((unsigned int )((unsigned char )*(buffer + 8UL)) != 0U) {
    {
    usb_string(udev, (int )*(buffer + 8UL), (char *)(& term->name), 64UL);
    }
  } else {
    {
    sprintf((char *)(& term->name), "Output %u", (int )*(buffer + 3UL));
    }
  }
  {
  list_add_tail(& term->list, & dev->entities);
  }
  goto ldv_34836;
  case_4:
  p = buflen > 4 ? (unsigned int )*(buffer + 4UL) : 0U;
  if (buflen <= 4 || (unsigned int )buflen < p + 6U) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: device %d videocontrol interface %d SELECTOR_UNIT error\n",
             udev->devnum, (int )alts->desc.bInterfaceNumber);
      }
    } else {
    }
    return (-22);
  } else {
  }
  {
  unit = uvc_alloc_entity((int )*(buffer + 2UL), (int )*(buffer + 3UL), p + 1U, 0U);
  }
  if ((unsigned long )unit == (unsigned long )((struct uvc_entity *)0)) {
    return (-12);
  } else {
  }
  {
  memcpy((void *)unit->baSourceID, (void const *)buffer + 5U, (size_t )p);
  }
  if ((unsigned int )((unsigned char )*(buffer + (unsigned long )(p + 5U))) != 0U) {
    {
    usb_string(udev, (int )*(buffer + (unsigned long )(p + 5U)), (char *)(& unit->name),
               64UL);
    }
  } else {
    {
    sprintf((char *)(& unit->name), "Selector %u", (int )*(buffer + 3UL));
    }
  }
  {
  list_add_tail(& unit->list, & dev->entities);
  }
  goto ldv_34836;
  case_5:
  n = buflen > 7 ? (unsigned int )*(buffer + 7UL) : 0U;
  p = (unsigned int )dev->uvc_version > 271U ? 10U : 9U;
  if ((unsigned int )buflen < p + n) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: device %d videocontrol interface %d PROCESSING_UNIT error\n",
             udev->devnum, (int )alts->desc.bInterfaceNumber);
      }
    } else {
    }
    return (-22);
  } else {
  }
  {
  unit = uvc_alloc_entity((int )*(buffer + 2UL), (int )*(buffer + 3UL), 2U, n);
  }
  if ((unsigned long )unit == (unsigned long )((struct uvc_entity *)0)) {
    return (-12);
  } else {
  }
  {
  memcpy((void *)unit->baSourceID, (void const *)buffer + 4U, 1UL);
  unit->__annonCompField80.processing.wMaxMultiplier = get_unaligned_le16((void const *)buffer + 5U);
  unit->__annonCompField80.processing.bControlSize = *(buffer + 7UL);
  unit->__annonCompField80.processing.bmControls = (__u8 *)unit + 504UL;
  memcpy((void *)unit->__annonCompField80.processing.bmControls, (void const *)buffer + 8U,
         (size_t )n);
  }
  if ((unsigned int )dev->uvc_version > 271U) {
    unit->__annonCompField80.processing.bmVideoStandards = *(buffer + (unsigned long )(n + 9U));
  } else {
  }
  if ((unsigned int )((unsigned char )*(buffer + (unsigned long )(n + 8U))) != 0U) {
    {
    usb_string(udev, (int )*(buffer + (unsigned long )(n + 8U)), (char *)(& unit->name),
               64UL);
    }
  } else {
    {
    sprintf((char *)(& unit->name), "Processing %u", (int )*(buffer + 3UL));
    }
  }
  {
  list_add_tail(& unit->list, & dev->entities);
  }
  goto ldv_34836;
  case_6:
  p = buflen > 21 ? (unsigned int )*(buffer + 21UL) : 0U;
  n = (unsigned int )buflen >= p + 24U ? (unsigned int )*(buffer + (unsigned long )(p + 22U)) : 0U;
  if ((unsigned int )buflen < (p + n) + 24U) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: device %d videocontrol interface %d EXTENSION_UNIT error\n",
             udev->devnum, (int )alts->desc.bInterfaceNumber);
      }
    } else {
    }
    return (-22);
  } else {
  }
  {
  unit = uvc_alloc_entity((int )*(buffer + 2UL), (int )*(buffer + 3UL), p + 1U, n);
  }
  if ((unsigned long )unit == (unsigned long )((struct uvc_entity *)0)) {
    return (-12);
  } else {
  }
  {
  memcpy((void *)(& unit->__annonCompField80.extension.guidExtensionCode), (void const *)buffer + 4U,
         16UL);
  unit->__annonCompField80.extension.bNumControls = *(buffer + 20UL);
  memcpy((void *)unit->baSourceID, (void const *)buffer + 22U, (size_t )p);
  unit->__annonCompField80.extension.bControlSize = *(buffer + (unsigned long )(p + 22U));
  unit->__annonCompField80.extension.bmControls = (__u8 *)unit + 504UL;
  memcpy((void *)unit->__annonCompField80.extension.bmControls, (void const *)buffer + (unsigned long )(p + 23U),
         (size_t )n);
  }
  if ((unsigned int )((unsigned char )*(buffer + (unsigned long )((p + n) + 23U))) != 0U) {
    {
    usb_string(udev, (int )*(buffer + (unsigned long )((p + n) + 23U)), (char *)(& unit->name),
               64UL);
    }
  } else {
    {
    sprintf((char *)(& unit->name), "Extension %u", (int )*(buffer + 3UL));
    }
  }
  {
  list_add_tail(& unit->list, & dev->entities);
  }
  goto ldv_34836;
  switch_default: ;
  if ((uvc_trace_param & 2U) != 0U) {
    {
    printk("\017uvcvideo: Found an unknown CS_INTERFACE descriptor (%u)\n", (int )*(buffer + 2UL));
    }
  } else {
  }
  goto ldv_34836;
  switch_break: ;
  }
  ldv_34836: ;
  return (0);
}
}
static int uvc_parse_control(struct uvc_device *dev )
{
  struct usb_host_interface *alts ;
  unsigned char *buffer ;
  int buflen ;
  int ret ;
  int tmp ;
  struct usb_host_endpoint *ep ;
  struct usb_endpoint_descriptor *desc ;
  int tmp___0 ;
  {
  alts = (dev->intf)->cur_altsetting;
  buffer = alts->extra;
  buflen = alts->extralen;
  goto ldv_34852;
  ldv_34851:
  {
  tmp = uvc_parse_vendor_control(dev, (unsigned char const *)buffer, buflen);
  }
  if (tmp != 0 || (unsigned int )*(buffer + 1UL) != 36U) {
    goto next_descriptor;
  } else {
  }
  {
  ret = uvc_parse_standard_control(dev, (unsigned char const *)buffer, buflen);
  }
  if (ret < 0) {
    return (ret);
  } else {
  }
  next_descriptor:
  buflen = buflen - (int )*buffer;
  buffer = buffer + (unsigned long )*buffer;
  ldv_34852: ;
  if (buflen > 2) {
    goto ldv_34851;
  } else {
  }
  if ((unsigned int )alts->desc.bNumEndpoints == 1U && (dev->quirks & 8U) == 0U) {
    {
    ep = alts->endpoint;
    desc = & ep->desc;
    tmp___0 = usb_endpoint_is_int_in((struct usb_endpoint_descriptor const *)desc);
    }
    if ((tmp___0 != 0 && (unsigned int )desc->wMaxPacketSize > 7U) && (unsigned int )desc->bInterval != 0U) {
      if ((uvc_trace_param & 2U) != 0U) {
        {
        printk("\017uvcvideo: Found a Status endpoint (addr %02x).\n", (int )desc->bEndpointAddress);
        }
      } else {
      }
      dev->int_ep = ep;
    } else {
    }
  } else {
  }
  return (0);
}
}
static int uvc_scan_chain_entity(struct uvc_video_chain *chain , struct uvc_entity *entity )
{
  {
  {
  if (((int )entity->type & 32767) == 6) {
    goto case_6;
  } else {
  }
  if (((int )entity->type & 32767) == 5) {
    goto case_5;
  } else {
  }
  if (((int )entity->type & 32767) == 4) {
    goto case_4;
  } else {
  }
  if (((int )entity->type & 32767) == 512) {
    goto case_512;
  } else {
  }
  if (((int )entity->type & 32767) == 513) {
    goto case_513;
  } else {
  }
  if (((int )entity->type & 32767) == 514) {
    goto case_514;
  } else {
  }
  if (((int )entity->type & 32767) == 768) {
    goto case_768;
  } else {
  }
  if (((int )entity->type & 32767) == 769) {
    goto case_769;
  } else {
  }
  if (((int )entity->type & 32767) == 770) {
    goto case_770;
  } else {
  }
  if (((int )entity->type & 32767) == 257) {
    goto case_257;
  } else {
  }
  goto switch_default;
  case_6: ;
  if ((int )uvc_trace_param & 1) {
    {
    printk(" <- XU %d", (int )entity->id);
    }
  } else {
  }
  if ((unsigned int )entity->bNrInPins != 1U) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: Extension unit %d has more than 1 input pin.\n", (int )entity->id);
      }
    } else {
    }
    return (-1);
  } else {
  }
  goto ldv_34861;
  case_5: ;
  if ((int )uvc_trace_param & 1) {
    {
    printk(" <- PU %d", (int )entity->id);
    }
  } else {
  }
  if ((unsigned long )chain->processing != (unsigned long )((struct uvc_entity *)0)) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: Found multiple Processing Units in chain.\n");
      }
    } else {
    }
    return (-1);
  } else {
  }
  chain->processing = entity;
  goto ldv_34861;
  case_4: ;
  if ((int )uvc_trace_param & 1) {
    {
    printk(" <- SU %d", (int )entity->id);
    }
  } else {
  }
  if ((unsigned int )entity->bNrInPins == 1U) {
    goto ldv_34861;
  } else {
  }
  if ((unsigned long )chain->selector != (unsigned long )((struct uvc_entity *)0)) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: Found multiple Selector Units in chain.\n");
      }
    } else {
    }
    return (-1);
  } else {
  }
  chain->selector = entity;
  goto ldv_34861;
  case_512: ;
  case_513: ;
  case_514: ;
  if ((int )uvc_trace_param & 1) {
    {
    printk(" <- IT %d\n", (int )entity->id);
    }
  } else {
  }
  goto ldv_34861;
  case_768: ;
  case_769: ;
  case_770: ;
  if ((int )uvc_trace_param & 1) {
    {
    printk(" OT %d", (int )entity->id);
    }
  } else {
  }
  goto ldv_34861;
  case_257: ;
  if (((int )entity->type & 65280) != 0 && (int )((short )entity->type) >= 0) {
    if ((int )uvc_trace_param & 1) {
      {
      printk(" <- IT %d\n", (int )entity->id);
      }
    } else {
    }
  } else
  if ((int )uvc_trace_param & 1) {
    {
    printk(" OT %d", (int )entity->id);
    }
  } else {
  }
  goto ldv_34861;
  switch_default: ;
  if ((uvc_trace_param & 2U) != 0U) {
    {
    printk("\017uvcvideo: Unsupported entity type 0x%04x found in chain.\n", (int )entity->type & 32767);
    }
  } else {
  }
  return (-1);
  switch_break: ;
  }
  ldv_34861:
  {
  list_add_tail(& entity->chain, & chain->entities);
  }
  return (0);
}
}
static int uvc_scan_chain_forward(struct uvc_video_chain *chain , struct uvc_entity *entity ,
                                  struct uvc_entity *prev )
{
  struct uvc_entity *forward ;
  int found ;
  {
  forward = (struct uvc_entity *)0;
  found = 0;
  ldv_34887:
  {
  forward = uvc_entity_by_reference(chain->dev, (int )entity->id, forward);
  }
  if ((unsigned long )forward == (unsigned long )((struct uvc_entity *)0)) {
    goto ldv_34879;
  } else {
  }
  if ((unsigned long )forward == (unsigned long )prev) {
    goto ldv_34880;
  } else {
  }
  {
  if (((int )forward->type & 32767) == 6) {
    goto case_6;
  } else {
  }
  if (((int )forward->type & 32767) == 768) {
    goto case_768;
  } else {
  }
  if (((int )forward->type & 32767) == 769) {
    goto case_769;
  } else {
  }
  if (((int )forward->type & 32767) == 770) {
    goto case_770;
  } else {
  }
  if (((int )forward->type & 32767) == 257) {
    goto case_257;
  } else {
  }
  goto switch_break;
  case_6: ;
  if ((unsigned int )forward->bNrInPins != 1U) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: Extension unit %d has more than 1 input pin.\n", (int )entity->id);
      }
    } else {
    }
    return (-22);
  } else {
  }
  {
  list_add_tail(& forward->chain, & chain->entities);
  }
  if ((int )uvc_trace_param & 1) {
    if (found == 0) {
      {
      printk(" (->");
      }
    } else {
    }
    {
    printk(" XU %d", (int )forward->id);
    found = 1;
    }
  } else {
  }
  goto ldv_34882;
  case_768: ;
  case_769: ;
  case_770: ;
  case_257: ;
  if (((int )forward->type & 65280) != 0 && (int )((short )forward->type) >= 0) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: Unsupported input terminal %u.\n", (int )forward->id);
      }
    } else {
    }
    return (-22);
  } else {
  }
  {
  list_add_tail(& forward->chain, & chain->entities);
  }
  if ((int )uvc_trace_param & 1) {
    if (found == 0) {
      {
      printk(" (->");
      }
    } else {
    }
    {
    printk(" OT %d", (int )forward->id);
    found = 1;
    }
  } else {
  }
  goto ldv_34882;
  switch_break: ;
  }
  ldv_34882: ;
  ldv_34880: ;
  goto ldv_34887;
  ldv_34879: ;
  if (found != 0) {
    {
    printk(")");
    }
  } else {
  }
  return (0);
}
}
static int uvc_scan_chain_backward(struct uvc_video_chain *chain , struct uvc_entity **_entity )
{
  struct uvc_entity *entity ;
  struct uvc_entity *term ;
  int id ;
  int i ;
  {
  entity = *_entity;
  id = -22;
  {
  if (((int )entity->type & 32767) == 6) {
    goto case_6;
  } else {
  }
  if (((int )entity->type & 32767) == 5) {
    goto case_5;
  } else {
  }
  if (((int )entity->type & 32767) == 4) {
    goto case_4;
  } else {
  }
  if (((int )entity->type & 32767) == 512) {
    goto case_512;
  } else {
  }
  if (((int )entity->type & 32767) == 513) {
    goto case_513;
  } else {
  }
  if (((int )entity->type & 32767) == 514) {
    goto case_514;
  } else {
  }
  if (((int )entity->type & 32767) == 768) {
    goto case_768;
  } else {
  }
  if (((int )entity->type & 32767) == 769) {
    goto case_769;
  } else {
  }
  if (((int )entity->type & 32767) == 770) {
    goto case_770;
  } else {
  }
  if (((int )entity->type & 32767) == 257) {
    goto case_257;
  } else {
  }
  goto switch_break;
  case_6: ;
  case_5:
  id = (int )*(entity->baSourceID);
  goto ldv_34898;
  case_4: ;
  if ((unsigned int )entity->bNrInPins == 1U) {
    id = (int )*(entity->baSourceID);
    goto ldv_34898;
  } else {
  }
  if ((int )uvc_trace_param & 1) {
    {
    printk(" <- IT");
    }
  } else {
  }
  chain->selector = entity;
  i = 0;
  goto ldv_34901;
  ldv_34900:
  {
  id = (int )*(entity->baSourceID + (unsigned long )i);
  term = uvc_entity_by_id(chain->dev, id);
  }
  if ((unsigned long )term == (unsigned long )((struct uvc_entity *)0) || (((int )term->type & 65280) == 0 || (int )((short )term->type) < 0)) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: Selector unit %d input %d isn\'t connected to an input terminal\n",
             (int )entity->id, i);
      }
    } else {
    }
    return (-1);
  } else {
  }
  if ((int )uvc_trace_param & 1) {
    {
    printk(" %d", (int )term->id);
    }
  } else {
  }
  {
  list_add_tail(& term->chain, & chain->entities);
  uvc_scan_chain_forward(chain, term, entity);
  i = i + 1;
  }
  ldv_34901: ;
  if (i < (int )entity->bNrInPins) {
    goto ldv_34900;
  } else {
  }
  if ((int )uvc_trace_param & 1) {
    {
    printk("\n");
    }
  } else {
  }
  id = 0;
  goto ldv_34898;
  case_512: ;
  case_513: ;
  case_514: ;
  case_768: ;
  case_769: ;
  case_770: ;
  case_257:
  id = ((int )entity->type & 65280) != 0 && (int )((short )entity->type) < 0 ? (int )*(entity->baSourceID) : 0;
  goto ldv_34898;
  switch_break: ;
  }
  ldv_34898: ;
  if (id <= 0) {
    *_entity = (struct uvc_entity *)0;
    return (id);
  } else {
  }
  {
  entity = uvc_entity_by_id(chain->dev, id);
  }
  if ((unsigned long )entity == (unsigned long )((struct uvc_entity *)0)) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: Found reference to unknown entity %d.\n", id);
      }
    } else {
    }
    return (-22);
  } else {
  }
  *_entity = entity;
  return (0);
}
}
static int uvc_scan_chain(struct uvc_video_chain *chain , struct uvc_entity *term )
{
  struct uvc_entity *entity ;
  struct uvc_entity *prev ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  {
  if ((int )uvc_trace_param & 1) {
    {
    printk("\017uvcvideo: Scanning UVC chain:");
    }
  } else {
  }
  entity = term;
  prev = (struct uvc_entity *)0;
  goto ldv_34917;
  ldv_34916: ;
  if ((unsigned long )entity->chain.next != (unsigned long )((struct list_head *)0) || (unsigned long )entity->chain.prev != (unsigned long )((struct list_head *)0)) {
    if ((uvc_trace_param & 2U) != 0U) {
      {
      printk("\017uvcvideo: Found reference to entity %d already in chain.\n", (int )entity->id);
      }
    } else {
    }
    return (-22);
  } else {
  }
  {
  tmp = uvc_scan_chain_entity(chain, entity);
  }
  if (tmp < 0) {
    return (-22);
  } else {
  }
  {
  tmp___0 = uvc_scan_chain_forward(chain, entity, prev);
  }
  if (tmp___0 < 0) {
    return (-22);
  } else {
  }
  {
  prev = entity;
  tmp___1 = uvc_scan_chain_backward(chain, & entity);
  }
  if (tmp___1 < 0) {
    return (-22);
  } else {
  }
  ldv_34917: ;
  if ((unsigned long )entity != (unsigned long )((struct uvc_entity *)0)) {
    goto ldv_34916;
  } else {
  }
  return (0);
}
}
static unsigned int uvc_print_terms(struct list_head *terms , u16 dir , char *buffer )
{
  struct uvc_entity *term ;
  unsigned int nterms ;
  char *p ;
  struct list_head const *__mptr ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  struct list_head const *__mptr___0 ;
  {
  nterms = 0U;
  p = buffer;
  __mptr = (struct list_head const *)terms->next;
  term = (struct uvc_entity *)__mptr + 0xfffffffffffffff0UL;
  goto ldv_34934;
  ldv_34933: ;
  if (((int )term->type & 65280) == 0 || ((int )term->type & 32768) != (int )dir) {
    goto ldv_34931;
  } else {
  }
  if (nterms != 0U) {
    {
    tmp = sprintf(p, ",");
    p = p + (unsigned long )tmp;
    }
  } else {
  }
  nterms = nterms + 1U;
  if (nterms > 3U) {
    {
    tmp___0 = sprintf(p, "...");
    p = p + (unsigned long )tmp___0;
    }
    goto ldv_34932;
  } else {
  }
  {
  tmp___1 = sprintf(p, "%u", (int )term->id);
  p = p + (unsigned long )tmp___1;
  }
  ldv_34931:
  __mptr___0 = (struct list_head const *)term->chain.next;
  term = (struct uvc_entity *)__mptr___0 + 0xfffffffffffffff0UL;
  ldv_34934: ;
  if ((unsigned long )(& term->chain) != (unsigned long )terms) {
    goto ldv_34933;
  } else {
  }
  ldv_34932: ;
  return ((unsigned int )((long )p) - (unsigned int )((long )buffer));
}
}
static char const *uvc_print_chain(struct uvc_video_chain *chain )
{
  char buffer[43U] ;
  char *p ;
  unsigned int tmp ;
  int tmp___0 ;
  {
  {
  p = (char *)(& buffer);
  tmp = uvc_print_terms(& chain->entities, 0, p);
  p = p + (unsigned long )tmp;
  tmp___0 = sprintf(p, " -> ");
  p = p + (unsigned long )tmp___0;
  uvc_print_terms(& chain->entities, 32768, p);
  }
  return ((char const *)(& buffer));
}
}
static int uvc_scan_device(struct uvc_device *dev )
{
  struct uvc_video_chain *chain ;
  struct uvc_entity *term ;
  struct list_head const *__mptr ;
  void *tmp ;
  struct lock_class_key __key ;
  int tmp___0 ;
  char const *tmp___1 ;
  struct list_head const *__mptr___0 ;
  int tmp___2 ;
  {
  __mptr = (struct list_head const *)dev->entities.next;
  term = (struct uvc_entity *)__mptr;
  goto ldv_34952;
  ldv_34951: ;
  if (((int )term->type & 65280) == 0 || (int )((short )term->type) >= 0) {
    goto ldv_34949;
  } else {
  }
  if ((unsigned long )term->chain.next != (unsigned long )((struct list_head *)0) || (unsigned long )term->chain.prev != (unsigned long )((struct list_head *)0)) {
    goto ldv_34949;
  } else {
  }
  {
  tmp = kzalloc(248UL, 208U);
  chain = (struct uvc_video_chain *)tmp;
  }
  if ((unsigned long )chain == (unsigned long )((struct uvc_video_chain *)0)) {
    return (-12);
  } else {
  }
  {
  INIT_LIST_HEAD(& chain->entities);
  __mutex_init(& chain->ctrl_mutex, "&chain->ctrl_mutex", & __key);
  chain->dev = dev;
  v4l2_prio_init(& chain->prio);
  term->flags = term->flags | 1U;
  tmp___0 = uvc_scan_chain(chain, term);
  }
  if (tmp___0 < 0) {
    {
    kfree((void const *)chain);
    }
    goto ldv_34949;
  } else {
  }
  if ((int )uvc_trace_param & 1) {
    {
    tmp___1 = uvc_print_chain(chain);
    printk("\017uvcvideo: Found a valid video chain (%s).\n", tmp___1);
    }
  } else {
  }
  {
  list_add_tail(& chain->list, & dev->chains);
  }
  ldv_34949:
  __mptr___0 = (struct list_head const *)term->list.next;
  term = (struct uvc_entity *)__mptr___0;
  ldv_34952: ;
  if ((unsigned long )(& term->list) != (unsigned long )(& dev->entities)) {
    goto ldv_34951;
  } else {
  }
  {
  tmp___2 = list_empty((struct list_head const *)(& dev->chains));
  }
  if (tmp___2 != 0) {
    {
    printk("\016uvcvideo: No valid video chain found.\n");
    }
    return (-1);
  } else {
  }
  return (0);
}
}
static void uvc_delete(struct uvc_device *dev )
{
  struct list_head *p ;
  struct list_head *n ;
  int tmp ;
  struct uvc_video_chain *chain ;
  struct list_head const *__mptr ;
  struct uvc_entity *entity ;
  struct list_head const *__mptr___0 ;
  struct uvc_streaming *streaming ;
  struct list_head const *__mptr___1 ;
  {
  {
  usb_put_intf(dev->intf);
  ldv_usb_put_dev_24(dev->udev);
  uvc_status_cleanup(dev);
  uvc_ctrl_cleanup_device(dev);
  }
  if ((unsigned long )dev->vdev.dev != (unsigned long )((struct device *)0)) {
    {
    v4l2_device_unregister(& dev->vdev);
    }
  } else {
  }
  {
  tmp = media_devnode_is_registered(& dev->mdev.devnode);
  }
  if (tmp != 0) {
    {
    media_device_unregister(& dev->mdev);
    }
  } else {
  }
  p = dev->chains.next;
  n = p->next;
  goto ldv_34963;
  ldv_34962:
  {
  __mptr = (struct list_head const *)p;
  chain = (struct uvc_video_chain *)__mptr + 0xfffffffffffffff8UL;
  kfree((void const *)chain);
  p = n;
  n = p->next;
  }
  ldv_34963: ;
  if ((unsigned long )p != (unsigned long )(& dev->chains)) {
    goto ldv_34962;
  } else {
  }
  p = dev->entities.next;
  n = p->next;
  goto ldv_34969;
  ldv_34968:
  {
  __mptr___0 = (struct list_head const *)p;
  entity = (struct uvc_entity *)__mptr___0;
  uvc_mc_cleanup_entity(entity);
  }
  if ((unsigned long )entity->vdev != (unsigned long )((struct video_device *)0)) {
    {
    video_device_release(entity->vdev);
    entity->vdev = (struct video_device *)0;
    }
  } else {
  }
  {
  kfree((void const *)entity);
  p = n;
  n = p->next;
  }
  ldv_34969: ;
  if ((unsigned long )p != (unsigned long )(& dev->entities)) {
    goto ldv_34968;
  } else {
  }
  p = dev->streams.next;
  n = p->next;
  goto ldv_34975;
  ldv_34974:
  {
  __mptr___1 = (struct list_head const *)p;
  streaming = (struct uvc_streaming *)__mptr___1;
  usb_driver_release_interface(& uvc_driver.driver, streaming->intf);
  usb_put_intf(streaming->intf);
  kfree((void const *)streaming->format);
  kfree((void const *)streaming->header.bmaControls);
  kfree((void const *)streaming);
  p = n;
  n = p->next;
  }
  ldv_34975: ;
  if ((unsigned long )p != (unsigned long )(& dev->streams)) {
    goto ldv_34974;
  } else {
  }
  {
  kfree((void const *)dev);
  }
  return;
}
}
static void uvc_release(struct video_device *vdev )
{
  struct uvc_streaming *stream ;
  void *tmp ;
  struct uvc_device *dev ;
  int tmp___0 ;
  {
  {
  tmp = video_get_drvdata(vdev);
  stream = (struct uvc_streaming *)tmp;
  dev = stream->dev;
  tmp___0 = atomic_dec_and_test(& dev->nstreams);
  }
  if (tmp___0 != 0) {
    {
    uvc_delete(dev);
    }
  } else {
  }
  return;
}
}
static void uvc_unregister_video(struct uvc_device *dev )
{
  struct uvc_streaming *stream ;
  struct list_head const *__mptr ;
  struct list_head const *__mptr___0 ;
  int tmp ;
  {
  {
  atomic_inc(& dev->nstreams);
  __mptr = (struct list_head const *)dev->streams.next;
  stream = (struct uvc_streaming *)__mptr;
  }
  goto ldv_34992;
  ldv_34991: ;
  if ((unsigned long )stream->vdev == (unsigned long )((struct video_device *)0)) {
    goto ldv_34990;
  } else {
  }
  {
  video_unregister_device(stream->vdev);
  stream->vdev = (struct video_device *)0;
  uvc_debugfs_cleanup_stream(stream);
  }
  ldv_34990:
  __mptr___0 = (struct list_head const *)stream->list.next;
  stream = (struct uvc_streaming *)__mptr___0;
  ldv_34992: ;
  if ((unsigned long )(& stream->list) != (unsigned long )(& dev->streams)) {
    goto ldv_34991;
  } else {
  }
  {
  tmp = atomic_dec_and_test(& dev->nstreams);
  }
  if (tmp != 0) {
    {
    uvc_delete(dev);
    }
  } else {
  }
  return;
}
}
static int uvc_register_video(struct uvc_device *dev , struct uvc_streaming *stream )
{
  struct video_device *vdev ;
  int ret ;
  {
  {
  ret = uvc_video_init(stream);
  }
  if (ret < 0) {
    {
    printk("\vuvcvideo: Failed to initialize the device (%d).\n", ret);
    }
    return (ret);
  } else {
  }
  {
  uvc_debugfs_init_stream(stream);
  vdev = video_device_alloc();
  }
  if ((unsigned long )vdev == (unsigned long )((struct video_device *)0)) {
    {
    printk("\vuvcvideo: Failed to allocate video device (%d).\n", ret);
    }
    return (-12);
  } else {
  }
  {
  vdev->v4l2_dev = & dev->vdev;
  vdev->fops = & uvc_fops;
  vdev->release = & uvc_release;
  vdev->prio = & (stream->chain)->prio;
  set_bit(2L, (unsigned long volatile *)(& vdev->flags));
  }
  if ((unsigned int )stream->type == 2U) {
    vdev->vfl_dir = 1;
  } else {
  }
  {
  strlcpy((char *)(& vdev->name), (char const *)(& dev->name), 32UL);
  stream->vdev = vdev;
  video_set_drvdata(vdev, (void *)stream);
  ret = video_register_device(vdev, 0, -1);
  }
  if (ret < 0) {
    {
    printk("\vuvcvideo: Failed to register video device (%d).\n", ret);
    stream->vdev = (struct video_device *)0;
    video_device_release(vdev);
    }
    return (ret);
  } else {
  }
  if ((unsigned int )stream->type == 1U) {
    (stream->chain)->caps = (stream->chain)->caps | 1U;
  } else {
    (stream->chain)->caps = (stream->chain)->caps | 2U;
  }
  {
  atomic_inc(& dev->nstreams);
  }
  return (0);
}
}
static int uvc_register_terms(struct uvc_device *dev , struct uvc_video_chain *chain )
{
  struct uvc_streaming *stream ;
  struct uvc_entity *term ;
  int ret ;
  struct list_head const *__mptr ;
  struct list_head const *__mptr___0 ;
  {
  __mptr = (struct list_head const *)chain->entities.next;
  term = (struct uvc_entity *)__mptr + 0xfffffffffffffff0UL;
  goto ldv_35013;
  ldv_35012: ;
  if (((int )term->type & 32767) != 257) {
    goto ldv_35011;
  } else {
  }
  {
  stream = uvc_stream_by_id(dev, (int )term->id);
  }
  if ((unsigned long )stream == (unsigned long )((struct uvc_streaming *)0)) {
    {
    printk("\016uvcvideo: No streaming interface found for terminal %u.", (int )term->id);
    }
    goto ldv_35011;
  } else {
  }
  {
  stream->chain = chain;
  ret = uvc_register_video(dev, stream);
  }
  if (ret < 0) {
    return (ret);
  } else {
  }
  term->vdev = stream->vdev;
  ldv_35011:
  __mptr___0 = (struct list_head const *)term->chain.next;
  term = (struct uvc_entity *)__mptr___0 + 0xfffffffffffffff0UL;
  ldv_35013: ;
  if ((unsigned long )(& term->chain) != (unsigned long )(& chain->entities)) {
    goto ldv_35012;
  } else {
  }
  return (0);
}
}
static int uvc_register_chains(struct uvc_device *dev )
{
  struct uvc_video_chain *chain ;
  int ret ;
  struct list_head const *__mptr ;
  struct list_head const *__mptr___0 ;
  {
  __mptr = (struct list_head const *)dev->chains.next;
  chain = (struct uvc_video_chain *)__mptr + 0xfffffffffffffff8UL;
  goto ldv_35025;
  ldv_35024:
  {
  ret = uvc_register_terms(dev, chain);
  }
  if (ret < 0) {
    return (ret);
  } else {
  }
  {
  ret = uvc_mc_register_entities(chain);
  }
  if (ret < 0) {
    {
    printk("\016uvcvideo: Failed to register entites (%d).\n", ret);
    }
  } else {
  }
  __mptr___0 = (struct list_head const *)chain->list.next;
  chain = (struct uvc_video_chain *)__mptr___0 + 0xfffffffffffffff8UL;
  ldv_35025: ;
  if ((unsigned long )(& chain->list) != (unsigned long )(& dev->chains)) {
    goto ldv_35024;
  } else {
  }
  return (0);
}
}
static int uvc_probe(struct usb_interface *intf , struct usb_device_id const *id )
{
  struct usb_device *udev ;
  struct usb_device *tmp ;
  struct uvc_device *dev ;
  int ret ;
  void *tmp___0 ;
  struct lock_class_key __key ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  {
  {
  tmp = interface_to_usbdev(intf);
  udev = tmp;
  }
  if ((unsigned int )((unsigned short )id->idVendor) != 0U && (unsigned int )((unsigned short )id->idProduct) != 0U) {
    if ((int )uvc_trace_param & 1) {
      {
      printk("\017uvcvideo: Probing known UVC device %s (%04x:%04x)\n", (char *)(& udev->devpath),
             (int )id->idVendor, (int )id->idProduct);
      }
    } else {
    }
  } else
  if ((int )uvc_trace_param & 1) {
    {
    printk("\017uvcvideo: Probing generic UVC device %s\n", (char *)(& udev->devpath));
    }
  } else {
  }
  {
  tmp___0 = kzalloc(2920UL, 208U);
  dev = (struct uvc_device *)tmp___0;
  }
  if ((unsigned long )dev == (unsigned long )((struct uvc_device *)0)) {
    return (-12);
  } else {
  }
  {
  INIT_LIST_HEAD(& dev->entities);
  INIT_LIST_HEAD(& dev->chains);
  INIT_LIST_HEAD(& dev->streams);
  atomic_set(& dev->nstreams, 0);
  atomic_set(& dev->nmappings, 0);
  __mutex_init(& dev->lock, "&dev->lock", & __key);
  dev->udev = ldv_usb_get_dev_25(udev);
  dev->intf = usb_get_intf(intf);
  dev->intfnum = (int )(intf->cur_altsetting)->desc.bInterfaceNumber;
  dev->quirks = uvc_quirks_param == 4294967295U ? (__u32 )id->driver_info : uvc_quirks_param;
  }
  if ((unsigned long )udev->product != (unsigned long )((char *)0)) {
    {
    strlcpy((char *)(& dev->name), (char const *)udev->product, 32UL);
    }
  } else {
    {
    snprintf((char *)(& dev->name), 32UL, "UVC Camera (%04x:%04x)", (int )udev->descriptor.idVendor,
             (int )udev->descriptor.idProduct);
    }
  }
  {
  tmp___1 = uvc_parse_control(dev);
  }
  if (tmp___1 < 0) {
    if ((int )uvc_trace_param & 1) {
      {
      printk("\017uvcvideo: Unable to parse UVC descriptors.\n");
      }
    } else {
    }
    goto error;
  } else {
  }
  {
  printk("\016uvcvideo: Found UVC %u.%02x device %s (%04x:%04x)\n", (int )dev->uvc_version >> 8,
         (int )dev->uvc_version & 255, (unsigned long )udev->product != (unsigned long )((char *)0) ? udev->product : (char *)"<unnamed>",
         (int )udev->descriptor.idVendor, (int )udev->descriptor.idProduct);
  }
  if ((unsigned long )dev->quirks != (unsigned long )id->driver_info) {
    {
    printk("\016uvcvideo: Forcing device quirks to 0x%x by module parameter for testing purpose.\n",
           dev->quirks);
    printk("\016uvcvideo: Please report required quirks to the linux-uvc-devel mailing list.\n");
    }
  } else {
  }
  {
  dev->mdev.dev = & intf->dev;
  strlcpy((char *)(& dev->mdev.model), (char const *)(& dev->name), 32UL);
  }
  if ((unsigned long )udev->serial != (unsigned long )((char *)0)) {
    {
    strlcpy((char *)(& dev->mdev.serial), (char const *)udev->serial, 40UL);
    }
  } else {
  }
  {
  strcpy((char *)(& dev->mdev.bus_info), (char const *)(& udev->devpath));
  dev->mdev.hw_revision = (u32 )udev->descriptor.bcdDevice;
  dev->mdev.driver_version = 200192U;
  tmp___2 = media_device_register(& dev->mdev);
  }
  if (tmp___2 < 0) {
    goto error;
  } else {
  }
  {
  dev->vdev.mdev = & dev->mdev;
  tmp___3 = v4l2_device_register(& intf->dev, & dev->vdev);
  }
  if (tmp___3 < 0) {
    goto error;
  } else {
  }
  {
  tmp___4 = uvc_ctrl_init_device(dev);
  }
  if (tmp___4 < 0) {
    goto error;
  } else {
  }
  {
  tmp___5 = uvc_scan_device(dev);
  }
  if (tmp___5 < 0) {
    goto error;
  } else {
  }
  {
  tmp___6 = uvc_register_chains(dev);
  }
  if (tmp___6 < 0) {
    goto error;
  } else {
  }
  {
  usb_set_intfdata(intf, (void *)dev);
  ret = uvc_status_init(dev);
  }
  if (ret < 0) {
    {
    printk("\016uvcvideo: Unable to initialize the status endpoint (%d), status interrupt will not be supported.\n",
           ret);
    }
  } else {
  }
  if ((int )uvc_trace_param & 1) {
    {
    printk("\017uvcvideo: UVC device initialized.\n");
    }
  } else {
  }
  {
  usb_enable_autosuspend(udev);
  }
  return (0);
  error:
  {
  uvc_unregister_video(dev);
  }
  return (-19);
}
}
static void uvc_disconnect(struct usb_interface *intf )
{
  struct uvc_device *dev ;
  void *tmp ;
  {
  {
  tmp = usb_get_intfdata(intf);
  dev = (struct uvc_device *)tmp;
  usb_set_intfdata(intf, (void *)0);
  }
  if ((unsigned int )(intf->cur_altsetting)->desc.bInterfaceSubClass == 2U) {
    return;
  } else {
  }
  {
  dev->state = (enum uvc_device_state )((unsigned int )dev->state | 1U);
  uvc_unregister_video(dev);
  }
  return;
}
}
static int uvc_suspend(struct usb_interface *intf , pm_message_t message )
{
  struct uvc_device *dev ;
  void *tmp ;
  struct uvc_streaming *stream ;
  struct list_head const *__mptr ;
  int tmp___0 ;
  struct list_head const *__mptr___0 ;
  {
  {
  tmp = usb_get_intfdata(intf);
  dev = (struct uvc_device *)tmp;
  }
  if ((uvc_trace_param & 256U) != 0U) {
    {
    printk("\017uvcvideo: Suspending interface %u\n", (int )(intf->cur_altsetting)->desc.bInterfaceNumber);
    }
  } else {
  }
  if ((unsigned int )(intf->cur_altsetting)->desc.bInterfaceSubClass == 1U) {
    {
    mutex_lock_nested(& dev->lock, 0U);
    }
    if (dev->users != 0U) {
      {
      uvc_status_stop(dev);
      }
    } else {
    }
    {
    mutex_unlock(& dev->lock);
    }
    return (0);
  } else {
  }
  __mptr = (struct list_head const *)dev->streams.next;
  stream = (struct uvc_streaming *)__mptr;
  goto ldv_35051;
  ldv_35050: ;
  if ((unsigned long )stream->intf == (unsigned long )intf) {
    {
    tmp___0 = uvc_video_suspend(stream);
    }
    return (tmp___0);
  } else {
  }
  __mptr___0 = (struct list_head const *)stream->list.next;
  stream = (struct uvc_streaming *)__mptr___0;
  ldv_35051: ;
  if ((unsigned long )(& stream->list) != (unsigned long )(& dev->streams)) {
    goto ldv_35050;
  } else {
  }
  if ((uvc_trace_param & 256U) != 0U) {
    {
    printk("\017uvcvideo: Suspend: video streaming USB interface mismatch.\n");
    }
  } else {
  }
  return (-22);
}
}
static int __uvc_resume(struct usb_interface *intf , int reset )
{
  struct uvc_device *dev ;
  void *tmp ;
  struct uvc_streaming *stream ;
  int ret ;
  struct list_head const *__mptr ;
  int tmp___0 ;
  struct list_head const *__mptr___0 ;
  {
  {
  tmp = usb_get_intfdata(intf);
  dev = (struct uvc_device *)tmp;
  }
  if ((uvc_trace_param & 256U) != 0U) {
    {
    printk("\017uvcvideo: Resuming interface %u\n", (int )(intf->cur_altsetting)->desc.bInterfaceNumber);
    }
  } else {
  }
  if ((unsigned int )(intf->cur_altsetting)->desc.bInterfaceSubClass == 1U) {
    ret = 0;
    if (reset != 0) {
      {
      ret = uvc_ctrl_resume_device(dev);
      }
      if (ret < 0) {
        return (ret);
      } else {
      }
    } else {
    }
    {
    mutex_lock_nested(& dev->lock, 0U);
    }
    if (dev->users != 0U) {
      {
      ret = uvc_status_start(dev, 16U);
      }
    } else {
    }
    {
    mutex_unlock(& dev->lock);
    }
    return (ret);
  } else {
  }
  __mptr = (struct list_head const *)dev->streams.next;
  stream = (struct uvc_streaming *)__mptr;
  goto ldv_35065;
  ldv_35064: ;
  if ((unsigned long )stream->intf == (unsigned long )intf) {
    {
    tmp___0 = uvc_video_resume(stream, reset);
    }
    return (tmp___0);
  } else {
  }
  __mptr___0 = (struct list_head const *)stream->list.next;
  stream = (struct uvc_streaming *)__mptr___0;
  ldv_35065: ;
  if ((unsigned long )(& stream->list) != (unsigned long )(& dev->streams)) {
    goto ldv_35064;
  } else {
  }
  if ((uvc_trace_param & 256U) != 0U) {
    {
    printk("\017uvcvideo: Resume: video streaming USB interface mismatch.\n");
    }
  } else {
  }
  return (-22);
}
}
static int uvc_resume(struct usb_interface *intf )
{
  int tmp ;
  {
  {
  tmp = __uvc_resume(intf, 0);
  }
  return (tmp);
}
}
static int uvc_reset_resume(struct usb_interface *intf )
{
  int tmp ;
  {
  {
  tmp = __uvc_resume(intf, 1);
  }
  return (tmp);
}
}
static int uvc_clock_param_get(char *buffer , struct kernel_param *kp )
{
  int tmp ;
  int tmp___0 ;
  {
  if (uvc_clock_param == 1U) {
    {
    tmp = sprintf(buffer, "CLOCK_MONOTONIC");
    }
    return (tmp);
  } else {
    {
    tmp___0 = sprintf(buffer, "CLOCK_REALTIME");
    }
    return (tmp___0);
  }
}
}
static int uvc_clock_param_set(char const *val , struct kernel_param *kp )
{
  size_t tmp ;
  size_t tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  {
  {
  tmp___0 = strlen("clock_");
  tmp___1 = strncasecmp(val, "clock_", tmp___0);
  }
  if (tmp___1 == 0) {
    {
    tmp = strlen("clock_");
    val = val + tmp;
    }
  } else {
  }
  {
  tmp___3 = strcasecmp(val, "monotonic");
  }
  if (tmp___3 == 0) {
    uvc_clock_param = 1U;
  } else {
    {
    tmp___2 = strcasecmp(val, "realtime");
    }
    if (tmp___2 == 0) {
      uvc_clock_param = 0U;
    } else {
      return (-22);
    }
  }
  return (0);
}
}
static struct usb_device_id uvc_ids[47U] =
  { {899U, 1046U, 43290U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 2UL},
        {899U, 1112U, 28782U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 2UL},
        {899U, 1118U, 248U, (unsigned short)0, (unsigned short)0, (unsigned char)0, (unsigned char)0,
      (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 2UL},
        {899U, 1118U, 1825U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 256UL},
        {899U, 1118U, 1827U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 2UL},
        {899U, 1133U, 2241U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 255U, 1U, 0U, (unsigned char)0, 0UL},
        {899U, 1133U, 2242U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 255U, 1U, 0U, (unsigned char)0, 0UL},
        {899U, 1133U, 2243U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 255U, 1U, 0U, (unsigned char)0, 0UL},
        {899U, 1133U, 2245U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 255U, 1U, 0U, (unsigned char)0, 0UL},
        {899U, 1133U, 2246U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 255U, 1U, 0U, (unsigned char)0, 0UL},
        {899U, 1133U, 2247U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 255U, 1U, 0U, (unsigned char)0, 0UL},
        {899U, 1266U, 45169U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 512UL},
        {899U, 1423U, 14368U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 2UL},
        {899U, 1449U, 9792U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 256UL},
        {899U, 1449U, 9793U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 256UL},
        {899U, 1449U, 9795U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 256UL},
        {899U, 1449U, 9802U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 256UL},
        {899U, 1452U, 34049U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 10UL},
        {899U, 1480U, 1027U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 128UL},
        {899U, 1507U, 1285U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 16UL},
        {899U, 1784U, 12300U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 128UL},
        {899U, 2760U, 13101U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 128UL},
        {899U, 2760U, 13328U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 128UL},
        {899U, 2760U, 13344U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 128UL},
        {899U, 3027U, 1365U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 2UL},
        {899U, 3725U, 4U, (unsigned short)0, (unsigned short)0, (unsigned char)0, (unsigned char)0,
      (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 258UL},
        {899U, 5075U, 20739U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 16UL},
        {899U, 5421U, 784U, (unsigned short)0, (unsigned short)0, (unsigned char)0, (unsigned char)0,
      (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 2UL},
        {899U, 5967U, 21010U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 16UL},
        {899U, 5967U, 22833U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 16UL},
        {899U, 5967U, 35346U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 16UL},
        {899U, 5967U, 35377U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 16UL},
        {899U, 5967U, 35379U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 16UL},
        {899U, 5967U, 35380U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 16UL},
        {899U, 6108U, 514U, (unsigned short)0, (unsigned short)0, (unsigned char)0, (unsigned char)0,
      (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 16UL},
        {899U, 6127U, 18443U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 16UL},
        {899U, 6257U, 774U, (unsigned short)0, (unsigned short)0, (unsigned char)0, (unsigned char)0,
      (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 6UL},
        {899U, 6349U, 51966U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 4UL},
        {899U, 6380U, 12680U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 2UL},
        {899U, 6380U, 12936U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 2UL},
        {899U, 6380U, 12944U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 256UL},
        {899U, 6558U, 33026U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 255U, 1U, 0U, (unsigned char)0, 0UL},
        {907U, 6571U, 4096U, (unsigned short)0, 294U, (unsigned char)0, (unsigned char)0,
      (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 1UL},
        {899U, 6971U, 10577U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 2UL},
        {899U, 7247U, 12288U, (unsigned short)0, (unsigned short)0, (unsigned char)0,
      (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0, 34UL},
        {896U, (unsigned short)0, (unsigned short)0, (unsigned short)0, (unsigned short)0,
      (unsigned char)0, (unsigned char)0, (unsigned char)0, 14U, 1U, 0U, (unsigned char)0,
      0UL}};
struct usb_device_id const __mod_usb_device_table ;
struct uvc_driver uvc_driver = {{"uvcvideo", & uvc_probe, & uvc_disconnect, 0, & uvc_suspend, & uvc_resume, & uvc_reset_resume,
     0, 0, (struct usb_device_id const *)(& uvc_ids), {{{{{{0U}}, 0U, 0U, 0, {0,
                                                                                {0,
                                                                                 0},
                                                                                0,
                                                                                0,
                                                                                0UL}}}},
                                                         {0, 0}}, {{0, 0, 0, 0, (_Bool)0,
                                                                    0, 0, 0, 0, 0,
                                                                    0, 0, 0, 0, 0},
                                                                   0}, 0U, 1U, 0U,
     0U}};
static int uvc_init(void)
{
  int ret ;
  {
  {
  uvc_debugfs_init();
  ret = ldv_usb_register_driver_26(& uvc_driver.driver, & __this_module, "uvcvideo");
  }
  if (ret < 0) {
    {
    uvc_debugfs_cleanup();
    }
    return (ret);
  } else {
  }
  {
  printk("\016USB Video Class driver (1.1.1)\n");
  }
  return (0);
}
}
static void uvc_cleanup(void)
{
  {
  {
  ldv_usb_deregister_27(& uvc_driver.driver);
  uvc_debugfs_cleanup();
  }
  return;
}
}
void ldv_EMGentry_exit_uvc_cleanup_10_2(void (*arg0)(void) ) ;
int ldv_EMGentry_init_uvc_init_10_19(int (*arg0)(void) ) ;
void ldv_dispatch_deregister_8_1(struct usb_driver *arg0 ) ;
void ldv_dispatch_deregister_dummy_resourceless_instance_4_10_4(void) ;
void ldv_dispatch_deregister_dummy_resourceless_instance_5_10_5(void) ;
void ldv_dispatch_deregister_dummy_resourceless_instance_6_10_6(void) ;
void ldv_dispatch_deregister_dummy_resourceless_instance_7_10_7(void) ;
void ldv_dispatch_deregister_file_operations_instance_3_10_8(void) ;
void ldv_dispatch_deregister_io_instance_10_10_9(void) ;
void ldv_dispatch_instance_deregister_6_2(struct usb_driver *arg0 ) ;
void ldv_dispatch_instance_register_6_3(struct usb_driver *arg0 ) ;
void ldv_dispatch_register_9_2(struct usb_driver *arg0 ) ;
void ldv_dispatch_register_dummy_resourceless_instance_4_10_10(void) ;
void ldv_dispatch_register_dummy_resourceless_instance_5_10_11(void) ;
void ldv_dispatch_register_dummy_resourceless_instance_6_10_12(void) ;
void ldv_dispatch_register_dummy_resourceless_instance_7_10_13(void) ;
void ldv_dispatch_register_file_operations_instance_3_10_14(void) ;
void ldv_dispatch_register_io_instance_10_10_15(void) ;
void ldv_dummy_resourceless_instance_callback_1_3(int (*arg0)(char * , struct kernel_param * ) ,
                                                  char *arg1 , struct kernel_param *arg2 ) ;
void ldv_dummy_resourceless_instance_callback_1_9(int (*arg0)(char * , struct kernel_param * ) ,
                                                  char *arg1 , struct kernel_param *arg2 ) ;
void ldv_dummy_resourceless_instance_callback_2_3(int (*arg0)(struct uvc_control_mapping * ,
                                                              unsigned char , unsigned char * ) ,
                                                  struct uvc_control_mapping *arg1 ,
                                                  unsigned char arg2 , unsigned char *arg3 ) ;
void ldv_dummy_resourceless_instance_callback_2_9(void (*arg0)(struct uvc_control_mapping * ,
                                                               int , unsigned char * ) ,
                                                  struct uvc_control_mapping *arg1 ,
                                                  int arg2 , unsigned char *arg3 ) ;
void ldv_dummy_resourceless_instance_callback_3_10(void (*arg0)(struct v4l2_event * ,
                                                                struct v4l2_event * ) ,
                                                   struct v4l2_event *arg1 , struct v4l2_event *arg2 ) ;
void ldv_dummy_resourceless_instance_callback_3_13(void (*arg0)(struct v4l2_event * ,
                                                                struct v4l2_event * ) ,
                                                   struct v4l2_event *arg1 , struct v4l2_event *arg2 ) ;
void ldv_dummy_resourceless_instance_callback_3_3(int (*arg0)(struct v4l2_subscribed_event * ,
                                                              unsigned int ) , struct v4l2_subscribed_event *arg1 ,
                                                  unsigned int arg2 ) ;
void ldv_dummy_resourceless_instance_callback_3_9(void (*arg0)(struct v4l2_subscribed_event * ) ,
                                                  struct v4l2_subscribed_event *arg1 ) ;
void ldv_dummy_resourceless_instance_callback_4_12(void (*arg0)(struct vb2_queue * ) ,
                                                   struct vb2_queue *arg1 ) ;
void ldv_dummy_resourceless_instance_callback_4_13(void (*arg0)(struct vb2_queue * ) ,
                                                   struct vb2_queue *arg1 ) ;
void ldv_dummy_resourceless_instance_callback_4_3(int (*arg0)(struct vb2_buffer * ) ,
                                                  struct vb2_buffer *arg1 ) ;
void ldv_dummy_resourceless_instance_callback_4_7(int (*arg0)(struct vb2_buffer * ) ,
                                                  struct vb2_buffer *arg1 ) ;
void ldv_dummy_resourceless_instance_callback_4_8(void (*arg0)(struct vb2_buffer * ) ,
                                                  struct vb2_buffer *arg1 ) ;
void ldv_dummy_resourceless_instance_callback_4_9(int (*arg0)(struct vb2_queue * ,
                                                              struct v4l2_format * ,
                                                              unsigned int * , unsigned int * ,
                                                              unsigned int * , void ** ) ,
                                                  struct vb2_queue *arg1 , struct v4l2_format *arg2 ,
                                                  unsigned int *arg3 , unsigned int *arg4 ,
                                                  unsigned int *arg5 , void **arg6 ) ;
void ldv_entry_EMGentry_10(void *arg0 ) ;
int main(void) ;
void ldv_file_operations_file_operations_instance_0(void *arg0 ) ;
void ldv_file_operations_instance_write_0_4(long (*arg0)(struct file * , char * ,
                                                         unsigned long , long long * ) ,
                                            struct file *arg1 , char *arg2 , unsigned long arg3 ,
                                            long long *arg4 ) ;
void ldv_struct_kernel_param_ops_dummy_resourceless_instance_1(void *arg0 ) ;
void ldv_struct_uvc_menu_info_dummy_resourceless_instance_2(void *arg0 ) ;
void ldv_struct_v4l2_subscribed_event_ops_dummy_resourceless_instance_3(void *arg0 ) ;
void ldv_struct_vb2_ops_dummy_resourceless_instance_4(void *arg0 ) ;
void ldv_usb_deregister(void *arg0 , struct usb_driver *arg1 ) ;
void ldv_usb_dummy_factory_6(void *arg0 ) ;
void ldv_usb_instance_callback_5_6(int (*arg0)(struct usb_interface * ) , struct usb_interface *arg1 ) ;
void ldv_usb_instance_post_5_9(int (*arg0)(struct usb_interface * ) , struct usb_interface *arg1 ) ;
void ldv_usb_instance_pre_5_10(int (*arg0)(struct usb_interface * ) , struct usb_interface *arg1 ) ;
int ldv_usb_instance_probe_5_13(int (*arg0)(struct usb_interface * , struct usb_device_id * ) ,
                                struct usb_interface *arg1 , struct usb_device_id *arg2 ) ;
void ldv_usb_instance_release_5_4(void (*arg0)(struct usb_interface * ) , struct usb_interface *arg1 ) ;
void ldv_usb_instance_resume_5_7(int (*arg0)(struct usb_interface * ) , struct usb_interface *arg1 ) ;
void ldv_usb_instance_suspend_5_8(int (*arg0)(struct usb_interface * , struct pm_message ) ,
                                  struct usb_interface *arg1 , struct pm_message *arg2 ) ;
int ldv_usb_register_driver(int arg0 , struct usb_driver *arg1 , struct module *arg2 ,
                            char *arg3 ) ;
void ldv_usb_usb_instance_5(void *arg0 ) ;
void ldv_v4l2_file_operations_io_instance_7(void *arg0 ) ;
struct ldv_thread ldv_thread_1 ;
struct ldv_thread ldv_thread_10 ;
struct ldv_thread ldv_thread_2 ;
struct ldv_thread ldv_thread_3 ;
struct ldv_thread ldv_thread_4 ;
struct ldv_thread ldv_thread_5 ;
struct ldv_thread ldv_thread_6 ;
void ldv_EMGentry_exit_uvc_cleanup_10_2(void (*arg0)(void) )
{
  {
  {
  uvc_cleanup();
  }
  return;
}
}
int ldv_EMGentry_init_uvc_init_10_19(int (*arg0)(void) )
{
  int tmp ;
  {
  {
  tmp = uvc_init();
  }
  return (tmp);
}
}
void ldv_dispatch_deregister_8_1(struct usb_driver *arg0 )
{
  {
  return;
}
}
void ldv_dispatch_deregister_dummy_resourceless_instance_4_10_4(void)
{
  {
  return;
}
}
void ldv_dispatch_deregister_dummy_resourceless_instance_5_10_5(void)
{
  {
  return;
}
}
void ldv_dispatch_deregister_dummy_resourceless_instance_6_10_6(void)
{
  {
  return;
}
}
void ldv_dispatch_deregister_dummy_resourceless_instance_7_10_7(void)
{
  {
  return;
}
}
void ldv_dispatch_deregister_file_operations_instance_3_10_8(void)
{
  {
  return;
}
}
void ldv_dispatch_deregister_io_instance_10_10_9(void)
{
  {
  return;
}
}
void ldv_dispatch_instance_deregister_6_2(struct usb_driver *arg0 )
{
  {
  return;
}
}
void ldv_dispatch_instance_register_6_3(struct usb_driver *arg0 )
{
  struct ldv_struct_usb_instance_5 *cf_arg_5 ;
  void *tmp ;
  {
  {
  tmp = ldv_xmalloc(16UL);
  cf_arg_5 = (struct ldv_struct_usb_instance_5 *)tmp;
  cf_arg_5->arg0 = arg0;
  ldv_usb_usb_instance_5((void *)cf_arg_5);
  }
  return;
}
}
void ldv_dispatch_register_9_2(struct usb_driver *arg0 )
{
  struct ldv_struct_usb_instance_5 *cf_arg_6 ;
  void *tmp ;
  {
  {
  tmp = ldv_xmalloc(16UL);
  cf_arg_6 = (struct ldv_struct_usb_instance_5 *)tmp;
  cf_arg_6->arg0 = arg0;
  ldv_usb_dummy_factory_6((void *)cf_arg_6);
  }
  return;
}
}
void ldv_dispatch_register_dummy_resourceless_instance_4_10_10(void)
{
  struct ldv_struct_EMGentry_10 *cf_arg_1 ;
  void *tmp ;
  {
  {
  tmp = ldv_xmalloc(4UL);
  cf_arg_1 = (struct ldv_struct_EMGentry_10 *)tmp;
  ldv_struct_kernel_param_ops_dummy_resourceless_instance_1((void *)cf_arg_1);
  }
  return;
}
}
void ldv_dispatch_register_dummy_resourceless_instance_5_10_11(void)
{
  struct ldv_struct_EMGentry_10 *cf_arg_2 ;
  void *tmp ;
  {
  {
  tmp = ldv_xmalloc(4UL);
  cf_arg_2 = (struct ldv_struct_EMGentry_10 *)tmp;
  ldv_struct_uvc_menu_info_dummy_resourceless_instance_2((void *)cf_arg_2);
  }
  return;
}
}
void ldv_dispatch_register_dummy_resourceless_instance_6_10_12(void)
{
  struct ldv_struct_EMGentry_10 *cf_arg_3 ;
  void *tmp ;
  {
  {
  tmp = ldv_xmalloc(4UL);
  cf_arg_3 = (struct ldv_struct_EMGentry_10 *)tmp;
  ldv_struct_v4l2_subscribed_event_ops_dummy_resourceless_instance_3((void *)cf_arg_3);
  }
  return;
}
}
void ldv_dispatch_register_dummy_resourceless_instance_7_10_13(void)
{
  struct ldv_struct_EMGentry_10 *cf_arg_4 ;
  void *tmp ;
  {
  {
  tmp = ldv_xmalloc(4UL);
  cf_arg_4 = (struct ldv_struct_EMGentry_10 *)tmp;
  ldv_struct_vb2_ops_dummy_resourceless_instance_4((void *)cf_arg_4);
  }
  return;
}
}
void ldv_dispatch_register_file_operations_instance_3_10_14(void)
{
  struct ldv_struct_EMGentry_10 *cf_arg_0 ;
  void *tmp ;
  {
  {
  tmp = ldv_xmalloc(4UL);
  cf_arg_0 = (struct ldv_struct_EMGentry_10 *)tmp;
  ldv_file_operations_file_operations_instance_0((void *)cf_arg_0);
  }
  return;
}
}
void ldv_dispatch_register_io_instance_10_10_15(void)
{
  struct ldv_struct_EMGentry_10 *cf_arg_7 ;
  void *tmp ;
  {
  {
  tmp = ldv_xmalloc(4UL);
  cf_arg_7 = (struct ldv_struct_EMGentry_10 *)tmp;
  ldv_v4l2_file_operations_io_instance_7((void *)cf_arg_7);
  }
  return;
}
}
void ldv_dummy_resourceless_instance_callback_1_3(int (*arg0)(char * , struct kernel_param * ) ,
                                                  char *arg1 , struct kernel_param *arg2 )
{
  {
  {
  uvc_clock_param_get(arg1, arg2);
  }
  return;
}
}
void ldv_dummy_resourceless_instance_callback_1_9(int (*arg0)(char * , struct kernel_param * ) ,
                                                  char *arg1 , struct kernel_param *arg2 )
{
  {
  {
  uvc_clock_param_set((char const *)arg1, arg2);
  }
  return;
}
}
void ldv_entry_EMGentry_10(void *arg0 )
{
  void (*ldv_10_exit_uvc_cleanup_default)(void) ;
  int (*ldv_10_init_uvc_init_default)(void) ;
  int ldv_10_ret_default ;
  int tmp ;
  int tmp___0 ;
  {
  {
  ldv_10_ret_default = ldv_EMGentry_init_uvc_init_10_19(ldv_10_init_uvc_init_default);
  ldv_10_ret_default = ldv_post_init(ldv_10_ret_default);
  tmp___0 = ldv_undef_int();
  }
  if (tmp___0 != 0) {
    {
    ldv_assume(ldv_10_ret_default != 0);
    ldv_check_final_state();
    ldv_stop();
    }
    return;
  } else {
    {
    ldv_assume(ldv_10_ret_default == 0);
    tmp = ldv_undef_int();
    }
    if (tmp != 0) {
      {
      ldv_dispatch_register_io_instance_10_10_15();
      ldv_dispatch_register_file_operations_instance_3_10_14();
      ldv_dispatch_register_dummy_resourceless_instance_7_10_13();
      ldv_dispatch_register_dummy_resourceless_instance_6_10_12();
      ldv_dispatch_register_dummy_resourceless_instance_5_10_11();
      ldv_dispatch_register_dummy_resourceless_instance_4_10_10();
      ldv_dispatch_deregister_io_instance_10_10_9();
      ldv_dispatch_deregister_file_operations_instance_3_10_8();
      ldv_dispatch_deregister_dummy_resourceless_instance_7_10_7();
      ldv_dispatch_deregister_dummy_resourceless_instance_6_10_6();
      ldv_dispatch_deregister_dummy_resourceless_instance_5_10_5();
      ldv_dispatch_deregister_dummy_resourceless_instance_4_10_4();
      }
    } else {
    }
    {
    ldv_EMGentry_exit_uvc_cleanup_10_2(ldv_10_exit_uvc_cleanup_default);
    ldv_check_final_state();
    ldv_stop();
    }
    return;
  }
  return;
}
}
int main(void)
{
  {
  {
  ldv_initialize();
  ldv_entry_EMGentry_10((void *)0);
  }
return 0;
}
}
void ldv_file_operations_instance_write_0_4(long (*arg0)(struct file * , char * ,
                                                         unsigned long , long long * ) ,
                                            struct file *arg1 , char *arg2 , unsigned long arg3 ,
                                            long long *arg4 )
{
  {
  {
  (*arg0)(arg1, arg2, arg3, arg4);
  }
  return;
}
}
void ldv_struct_kernel_param_ops_dummy_resourceless_instance_1(void *arg0 )
{
  int (*ldv_1_callback_get)(char * , struct kernel_param * ) ;
  int (*ldv_1_callback_set)(char * , struct kernel_param * ) ;
  struct kernel_param *ldv_1_container_struct_kernel_param ;
  char *ldv_1_ldv_param_3_0_default ;
  char *ldv_1_ldv_param_9_0_default ;
  void *tmp ;
  void *tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  {
  goto ldv_call_1;
  return;
  ldv_call_1:
  {
  tmp___2 = ldv_undef_int();
  }
  if (tmp___2 != 0) {
    {
    tmp = ldv_xmalloc(1UL);
    ldv_1_ldv_param_3_0_default = (char *)tmp;
    tmp___1 = ldv_undef_int();
    }
    if (tmp___1 != 0) {
      {
      tmp___0 = ldv_xmalloc(1UL);
      ldv_1_ldv_param_9_0_default = (char *)tmp___0;
      ldv_dummy_resourceless_instance_callback_1_9(ldv_1_callback_set, ldv_1_ldv_param_9_0_default,
                                                   ldv_1_container_struct_kernel_param);
      ldv_free((void *)ldv_1_ldv_param_9_0_default);
      }
    } else {
      {
      ldv_dummy_resourceless_instance_callback_1_3(ldv_1_callback_get, ldv_1_ldv_param_3_0_default,
                                                   ldv_1_container_struct_kernel_param);
      }
    }
    {
    ldv_free((void *)ldv_1_ldv_param_3_0_default);
    }
    goto ldv_call_1;
  } else {
    return;
  }
  return;
}
}
void ldv_struct_uvc_menu_info_dummy_resourceless_instance_2(void *arg0 )
{
  int (*ldv_2_callback_get)(struct uvc_control_mapping * , unsigned char , unsigned char * ) ;
  void (*ldv_2_callback_set)(struct uvc_control_mapping * , int , unsigned char * ) ;
  struct uvc_control_mapping *ldv_2_container_struct_uvc_control_mapping ;
  unsigned char ldv_2_ldv_param_3_1_default ;
  unsigned char *ldv_2_ldv_param_3_2_default ;
  int ldv_2_ldv_param_9_1_default ;
  unsigned char *ldv_2_ldv_param_9_2_default ;
  void *tmp ;
  void *tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  {
  goto ldv_call_2;
  return;
  ldv_call_2:
  {
  tmp___2 = ldv_undef_int();
  }
  if (tmp___2 != 0) {
    {
    tmp = ldv_xmalloc(1UL);
    ldv_2_ldv_param_3_2_default = (unsigned char *)tmp;
    tmp___1 = ldv_undef_int();
    }
    if (tmp___1 != 0) {
      {
      tmp___0 = ldv_xmalloc(1UL);
      ldv_2_ldv_param_9_2_default = (unsigned char *)tmp___0;
      ldv_dummy_resourceless_instance_callback_2_9(ldv_2_callback_set, ldv_2_container_struct_uvc_control_mapping,
                                                   ldv_2_ldv_param_9_1_default, ldv_2_ldv_param_9_2_default);
      ldv_free((void *)ldv_2_ldv_param_9_2_default);
      }
    } else {
      {
      ldv_dummy_resourceless_instance_callback_2_3(ldv_2_callback_get, ldv_2_container_struct_uvc_control_mapping,
                                                   (int )ldv_2_ldv_param_3_1_default,
                                                   ldv_2_ldv_param_3_2_default);
      }
    }
    {
    ldv_free((void *)ldv_2_ldv_param_3_2_default);
    }
    goto ldv_call_2;
  } else {
    return;
  }
  return;
}
}
void ldv_struct_v4l2_subscribed_event_ops_dummy_resourceless_instance_3(void *arg0 )
{
  int (*ldv_3_callback_add)(struct v4l2_subscribed_event * , unsigned int ) ;
  void (*ldv_3_callback_del)(struct v4l2_subscribed_event * ) ;
  void (*ldv_3_callback_merge)(struct v4l2_event * , struct v4l2_event * ) ;
  void (*ldv_3_callback_replace)(struct v4l2_event * , struct v4l2_event * ) ;
  struct v4l2_event *ldv_3_container_struct_v4l2_event_ptr ;
  struct v4l2_subscribed_event *ldv_3_container_struct_v4l2_subscribed_event_ptr ;
  struct v4l2_event *ldv_3_ldv_param_10_1_default ;
  struct v4l2_event *ldv_3_ldv_param_13_1_default ;
  unsigned int ldv_3_ldv_param_3_1_default ;
  int tmp ;
  void *tmp___0 ;
  void *tmp___1 ;
  int tmp___2 ;
  {
  goto ldv_call_3;
  return;
  ldv_call_3:
  {
  tmp___2 = ldv_undef_int();
  }
  if (tmp___2 != 0) {
    {
    tmp = ldv_undef_int();
    }
    {
    if (tmp == 1) {
      goto case_1;
    } else {
    }
    if (tmp == 2) {
      goto case_2;
    } else {
    }
    if (tmp == 3) {
      goto case_3;
    } else {
    }
    if (tmp == 4) {
      goto case_4;
    } else {
    }
    goto switch_default;
    case_1:
    {
    tmp___0 = ldv_xmalloc(136UL);
    ldv_3_ldv_param_13_1_default = (struct v4l2_event *)tmp___0;
    ldv_dummy_resourceless_instance_callback_3_13(ldv_3_callback_replace, ldv_3_container_struct_v4l2_event_ptr,
                                                  ldv_3_ldv_param_13_1_default);
    ldv_free((void *)ldv_3_ldv_param_13_1_default);
    }
    goto ldv_35528;
    case_2:
    {
    tmp___1 = ldv_xmalloc(136UL);
    ldv_3_ldv_param_10_1_default = (struct v4l2_event *)tmp___1;
    ldv_dummy_resourceless_instance_callback_3_10(ldv_3_callback_merge, ldv_3_container_struct_v4l2_event_ptr,
                                                  ldv_3_ldv_param_10_1_default);
    ldv_free((void *)ldv_3_ldv_param_10_1_default);
    }
    goto ldv_35528;
    case_3:
    {
    ldv_dummy_resourceless_instance_callback_3_9(ldv_3_callback_del, ldv_3_container_struct_v4l2_subscribed_event_ptr);
    }
    goto ldv_35528;
    case_4:
    {
    ldv_dummy_resourceless_instance_callback_3_3(ldv_3_callback_add, ldv_3_container_struct_v4l2_subscribed_event_ptr,
                                                 ldv_3_ldv_param_3_1_default);
    }
    goto ldv_35528;
    switch_default:
    {
    ldv_stop();
    }
    switch_break: ;
    }
    ldv_35528: ;
    goto ldv_call_3;
  } else {
    return;
  }
  return;
}
}
void ldv_struct_vb2_ops_dummy_resourceless_instance_4(void *arg0 )
{
  int (*ldv_4_callback_buf_finish)(struct vb2_buffer * ) ;
  int (*ldv_4_callback_buf_prepare)(struct vb2_buffer * ) ;
  void (*ldv_4_callback_buf_queue)(struct vb2_buffer * ) ;
  int (*ldv_4_callback_queue_setup)(struct vb2_queue * , struct v4l2_format * , unsigned int * ,
                                    unsigned int * , unsigned int * , void ** ) ;
  void (*ldv_4_callback_wait_finish)(struct vb2_queue * ) ;
  void (*ldv_4_callback_wait_prepare)(struct vb2_queue * ) ;
  struct v4l2_format *ldv_4_container_struct_v4l2_format_ptr ;
  struct vb2_buffer *ldv_4_container_struct_vb2_buffer_ptr ;
  struct vb2_queue *ldv_4_container_struct_vb2_queue_ptr ;
  void **ldv_4_container_void_ptr_ptr ;
  unsigned int *ldv_4_ldv_param_9_3_default ;
  unsigned int *ldv_4_ldv_param_9_4_default ;
  int tmp ;
  void *tmp___0 ;
  void *tmp___1 ;
  {
  goto ldv_call_4;
  return;
  ldv_call_4:
  {
  tmp = ldv_undef_int();
  }
  {
  if (tmp == 1) {
    goto case_1;
  } else {
  }
  if (tmp == 2) {
    goto case_2;
  } else {
  }
  if (tmp == 3) {
    goto case_3;
  } else {
  }
  if (tmp == 4) {
    goto case_4;
  } else {
  }
  if (tmp == 5) {
    goto case_5;
  } else {
  }
  if (tmp == 6) {
    goto case_6;
  } else {
  }
  if (tmp == 7) {
    goto case_7;
  } else {
  }
  goto switch_default;
  case_1:
  {
  ldv_dummy_resourceless_instance_callback_4_13(ldv_4_callback_wait_prepare, ldv_4_container_struct_vb2_queue_ptr);
  }
  goto ldv_call_4;
  case_2:
  {
  ldv_dummy_resourceless_instance_callback_4_12(ldv_4_callback_wait_finish, ldv_4_container_struct_vb2_queue_ptr);
  }
  goto ldv_call_4;
  goto ldv_call_4;
  case_3:
  {
  tmp___0 = ldv_xmalloc(4UL);
  ldv_4_ldv_param_9_3_default = (unsigned int *)tmp___0;
  tmp___1 = ldv_xmalloc(4UL);
  ldv_4_ldv_param_9_4_default = (unsigned int *)tmp___1;
  ldv_dummy_resourceless_instance_callback_4_9(ldv_4_callback_queue_setup, ldv_4_container_struct_vb2_queue_ptr,
                                               ldv_4_container_struct_v4l2_format_ptr,
                                               (unsigned int *)ldv_4_container_void_ptr_ptr,
                                               ldv_4_ldv_param_9_3_default, ldv_4_ldv_param_9_4_default,
                                               (void **)ldv_4_container_struct_vb2_buffer_ptr);
  ldv_free((void *)ldv_4_ldv_param_9_3_default);
  ldv_free((void *)ldv_4_ldv_param_9_4_default);
  }
  goto ldv_call_4;
  goto ldv_call_4;
  goto ldv_call_4;
  case_4:
  {
  ldv_dummy_resourceless_instance_callback_4_8(ldv_4_callback_buf_queue, ldv_4_container_struct_vb2_buffer_ptr);
  }
  goto ldv_call_4;
  goto ldv_call_4;
  goto ldv_call_4;
  goto ldv_call_4;
  case_5:
  {
  ldv_dummy_resourceless_instance_callback_4_7(ldv_4_callback_buf_prepare, ldv_4_container_struct_vb2_buffer_ptr);
  }
  goto ldv_call_4;
  goto ldv_call_4;
  goto ldv_call_4;
  goto ldv_call_4;
  goto ldv_call_4;
  case_6:
  {
  ldv_dummy_resourceless_instance_callback_4_3(ldv_4_callback_buf_finish, ldv_4_container_struct_vb2_buffer_ptr);
  }
  goto ldv_call_4;
  goto ldv_call_4;
  goto ldv_call_4;
  goto ldv_call_4;
  goto ldv_call_4;
  goto ldv_call_4;
  case_7: ;
  return;
  switch_default:
  {
  ldv_stop();
  }
  switch_break: ;
  }
  return;
}
}
void ldv_usb_deregister(void *arg0 , struct usb_driver *arg1 )
{
  struct usb_driver *ldv_8_usb_driver_usb_driver ;
  {
  {
  ldv_8_usb_driver_usb_driver = arg1;
  ldv_dispatch_deregister_8_1(ldv_8_usb_driver_usb_driver);
  }
  return;
  return;
}
}
void ldv_usb_dummy_factory_6(void *arg0 )
{
  struct usb_driver *ldv_6_container_usb_driver ;
  struct ldv_struct_usb_instance_5 *data ;
  {
  data = (struct ldv_struct_usb_instance_5 *)arg0;
  if ((unsigned long )data != (unsigned long )((struct ldv_struct_usb_instance_5 *)0)) {
    {
    ldv_6_container_usb_driver = data->arg0;
    ldv_free((void *)data);
    }
  } else {
  }
  {
  ldv_dispatch_instance_register_6_3(ldv_6_container_usb_driver);
  ldv_dispatch_instance_deregister_6_2(ldv_6_container_usb_driver);
  }
  return;
  return;
}
}
void ldv_usb_instance_callback_5_6(int (*arg0)(struct usb_interface * ) , struct usb_interface *arg1 )
{
  {
  {
  uvc_reset_resume(arg1);
  }
  return;
}
}
void ldv_usb_instance_post_5_9(int (*arg0)(struct usb_interface * ) , struct usb_interface *arg1 )
{
  {
  {
  (*arg0)(arg1);
  }
  return;
}
}
void ldv_usb_instance_pre_5_10(int (*arg0)(struct usb_interface * ) , struct usb_interface *arg1 )
{
  {
  {
  (*arg0)(arg1);
  }
  return;
}
}
int ldv_usb_instance_probe_5_13(int (*arg0)(struct usb_interface * , struct usb_device_id * ) ,
                                struct usb_interface *arg1 , struct usb_device_id *arg2 )
{
  int tmp ;
  {
  {
  tmp = uvc_probe(arg1, (struct usb_device_id const *)arg2);
  }
  return (tmp);
}
}
void ldv_usb_instance_release_5_4(void (*arg0)(struct usb_interface * ) , struct usb_interface *arg1 )
{
  {
  {
  uvc_disconnect(arg1);
  }
  return;
}
}
void ldv_usb_instance_resume_5_7(int (*arg0)(struct usb_interface * ) , struct usb_interface *arg1 )
{
  {
  {
  uvc_resume(arg1);
  }
  return;
}
}
void ldv_usb_instance_suspend_5_8(int (*arg0)(struct usb_interface * , struct pm_message ) ,
                                  struct usb_interface *arg1 , struct pm_message *arg2 )
{
  {
  {
  uvc_suspend(arg1, *arg2);
  }
  return;
}
}
int ldv_usb_register_driver(int arg0 , struct usb_driver *arg1 , struct module *arg2 ,
                            char *arg3 )
{
  struct usb_driver *ldv_9_usb_driver_usb_driver ;
  int tmp ;
  {
  {
  arg0 = ldv_pre_usb_register_driver();
  tmp = ldv_undef_int();
  }
  if (tmp != 0) {
    {
    ldv_assume(arg0 == 0);
    ldv_9_usb_driver_usb_driver = arg1;
    ldv_dispatch_register_9_2(ldv_9_usb_driver_usb_driver);
    }
    return (arg0);
  } else {
    {
    ldv_assume(arg0 != 0);
    }
    return (arg0);
  }
  return (arg0);
}
}
void ldv_usb_usb_instance_5(void *arg0 )
{
  int (*ldv_5_callback_reset_resume)(struct usb_interface * ) ;
  struct usb_driver *ldv_5_container_usb_driver ;
  struct usb_device_id *ldv_5_ldv_param_13_1_default ;
  struct pm_message *ldv_5_ldv_param_8_1_default ;
  int ldv_5_probe_retval_default ;
  _Bool ldv_5_reset_flag_default ;
  struct usb_interface *ldv_5_resource_usb_interface ;
  struct usb_device *ldv_5_usb_device_usb_device ;
  struct ldv_struct_usb_instance_5 *data ;
  void *tmp ;
  void *tmp___0 ;
  void *tmp___1 ;
  int tmp___2 ;
  void *tmp___3 ;
  int tmp___4 ;
  {
  data = (struct ldv_struct_usb_instance_5 *)arg0;
  ldv_5_reset_flag_default = 0;
  if ((unsigned long )data != (unsigned long )((struct ldv_struct_usb_instance_5 *)0)) {
    {
    ldv_5_container_usb_driver = data->arg0;
    ldv_free((void *)data);
    }
  } else {
  }
  {
  tmp = ldv_xmalloc(1528UL);
  ldv_5_resource_usb_interface = (struct usb_interface *)tmp;
  tmp___0 = ldv_xmalloc(1992UL);
  ldv_5_usb_device_usb_device = (struct usb_device *)tmp___0;
  ldv_5_resource_usb_interface->dev.parent = & ldv_5_usb_device_usb_device->dev;
  tmp___1 = ldv_xmalloc(32UL);
  ldv_5_ldv_param_13_1_default = (struct usb_device_id *)tmp___1;
  ldv_pre_probe();
  ldv_5_probe_retval_default = ldv_usb_instance_probe_5_13((int (*)(struct usb_interface * ,
                                                                    struct usb_device_id * ))ldv_5_container_usb_driver->probe,
                                                           ldv_5_resource_usb_interface,
                                                           ldv_5_ldv_param_13_1_default);
  ldv_5_probe_retval_default = ldv_ldv_post_probe_28(ldv_5_probe_retval_default);
  ldv_free((void *)ldv_5_ldv_param_13_1_default);
  tmp___4 = ldv_undef_int();
  }
  if (tmp___4 != 0) {
    {
    ldv_assume(ldv_5_probe_retval_default == 0);
    tmp___2 = ldv_undef_int();
    }
    {
    if (tmp___2 == 1) {
      goto case_1;
    } else {
    }
    if (tmp___2 == 2) {
      goto case_2;
    } else {
    }
    if (tmp___2 == 3) {
      goto case_3;
    } else {
    }
    if (tmp___2 == 4) {
      goto case_4;
    } else {
    }
    goto switch_default;
    case_1:
    {
    tmp___3 = ldv_xmalloc(4UL);
    ldv_5_ldv_param_8_1_default = (struct pm_message *)tmp___3;
    ldv_usb_instance_suspend_5_8(ldv_5_container_usb_driver->suspend, ldv_5_resource_usb_interface,
                                 ldv_5_ldv_param_8_1_default);
    ldv_free((void *)ldv_5_ldv_param_8_1_default);
    ldv_usb_instance_resume_5_7(ldv_5_container_usb_driver->resume, ldv_5_resource_usb_interface);
    }
    goto ldv_35638;
    case_2: ;
    if ((unsigned long )ldv_5_container_usb_driver->pre_reset != (unsigned long )((int (*)(struct usb_interface * ))0)) {
      {
      ldv_usb_instance_pre_5_10(ldv_5_container_usb_driver->pre_reset, ldv_5_resource_usb_interface);
      }
    } else {
    }
    if ((unsigned long )ldv_5_container_usb_driver->post_reset != (unsigned long )((int (*)(struct usb_interface * ))0)) {
      {
      ldv_usb_instance_post_5_9(ldv_5_container_usb_driver->post_reset, ldv_5_resource_usb_interface);
      }
    } else {
    }
    goto ldv_35638;
    case_3:
    {
    ldv_usb_instance_callback_5_6(ldv_5_callback_reset_resume, ldv_5_resource_usb_interface);
    }
    goto ldv_35638;
    case_4: ;
    goto ldv_35638;
    switch_default:
    {
    ldv_stop();
    }
    switch_break: ;
    }
    ldv_35638:
    {
    ldv_usb_instance_release_5_4(ldv_5_container_usb_driver->disconnect, ldv_5_resource_usb_interface);
    }
  } else {
    {
    ldv_assume(ldv_5_probe_retval_default != 0);
    }
  }
  {
  ldv_free((void *)ldv_5_resource_usb_interface);
  ldv_free((void *)ldv_5_usb_device_usb_device);
  }
  return;
  return;
}
}
__inline static void atomic_inc(atomic_t *v )
{
  {
  {
  ldv_atomic_inc(v);
  }
  return;
}
}
__inline static int atomic_dec_and_test(atomic_t *v )
{
  int tmp ;
  {
  {
  tmp = ldv_atomic_dec_and_test(v);
  }
  return (tmp);
}
}
static void *ldv_dev_get_drvdata_15(struct device const *dev )
{
  void *tmp ;
  {
  {
  tmp = ldv_dev_get_drvdata(dev);
  }
  return (tmp);
}
}
static int ldv_dev_set_drvdata_16(struct device *dev , void *data )
{
  int tmp ;
  {
  {
  tmp = ldv_dev_set_drvdata(dev, data);
  }
  return (tmp);
}
}
void *ldv_zalloc(size_t size ) ;
__inline static void *kzalloc(size_t size , gfp_t flags )
{
  void *tmp ;
  {
  {
  tmp = ldv_kzalloc(size, flags);
  }
  return (tmp);
}
}
static void *ldv_dev_get_drvdata_18(struct device const *dev )
{
  void *tmp ;
  {
  {
  tmp = ldv_dev_get_drvdata(dev);
  }
  return (tmp);
}
}
static int ldv_dev_set_drvdata_19(struct device *dev , void *data )
{
  int tmp ;
  {
  {
  tmp = ldv_dev_set_drvdata(dev, data);
  }
  return (tmp);
}
}
static void ldv_usb_put_dev_24(struct usb_device *ldv_func_arg1 )
{
  {
  {
  ldv_usb_put_dev(ldv_func_arg1);
  }
  return;
}
}
static struct usb_device *ldv_usb_get_dev_25(struct usb_device *ldv_func_arg1 )
{
  struct usb_device *tmp ;
  {
  {
  tmp = ldv_usb_get_dev(ldv_func_arg1);
  }
  return (tmp);
}
}
static int ldv_usb_register_driver_26(struct usb_driver *ldv_func_arg1 , struct module *ldv_func_arg2 ,
                                      char const *ldv_func_arg3 )
{
  ldv_func_ret_type ldv_func_res ;
  int tmp ;
  int tmp___0 ;
  {
  {
  tmp = usb_register_driver(ldv_func_arg1, ldv_func_arg2, ldv_func_arg3);
  ldv_func_res = tmp;
  tmp___0 = ldv_usb_register_driver(ldv_func_res, ldv_func_arg1, ldv_func_arg2, (char *)ldv_func_arg3);
  }
  return (tmp___0);
  return (ldv_func_res);
}
}
static void ldv_usb_deregister_27(struct usb_driver *ldv_func_arg1 )
{
  {
  {
  usb_deregister(ldv_func_arg1);
  ldv_usb_deregister((void *)0, ldv_func_arg1);
  }
  return;
}
}
static int ldv_ldv_post_probe_28(int ldv_func_arg1 )
{
  int tmp ;
  {
  {
  ldv_check_return_value_probe(ldv_func_arg1);
  tmp = ldv_post_probe(ldv_func_arg1);
  }
  return (tmp);
}
}
long ldv__builtin_expect(long exp , long c ) ;
extern void list_del(struct list_head * ) ;
extern void __raw_spin_lock_init(raw_spinlock_t * , char const * , struct lock_class_key * ) ;
extern unsigned long _raw_spin_lock_irqsave(raw_spinlock_t * ) ;
extern void _raw_spin_unlock_irqrestore(raw_spinlock_t * , unsigned long ) ;
__inline static raw_spinlock_t *spinlock_check(spinlock_t *lock )
{
  {
  return (& lock->__annonCompField19.rlock);
}
}
__inline static void spin_unlock_irqrestore(spinlock_t *lock , unsigned long flags )
{
  {
  {
  _raw_spin_unlock_irqrestore(& lock->__annonCompField19.rlock, flags);
  }
  return;
}
}
extern void *vb2_plane_vaddr(struct vb2_buffer * , unsigned int ) ;
extern void vb2_buffer_done(struct vb2_buffer * , enum vb2_buffer_state ) ;
extern int vb2_querybuf(struct vb2_queue * , struct v4l2_buffer * ) ;
extern int vb2_reqbufs(struct vb2_queue * , struct v4l2_requestbuffers * ) ;
extern int vb2_queue_init(struct vb2_queue * ) ;
extern void vb2_queue_release(struct vb2_queue * ) ;
extern int vb2_qbuf(struct vb2_queue * , struct v4l2_buffer * ) ;
extern int vb2_dqbuf(struct vb2_queue * , struct v4l2_buffer * , bool ) ;
extern int vb2_streamon(struct vb2_queue * , enum v4l2_buf_type ) ;
extern int vb2_streamoff(struct vb2_queue * , enum v4l2_buf_type ) ;
extern int vb2_mmap(struct vb2_queue * , struct vm_area_struct * ) ;
extern unsigned int vb2_poll(struct vb2_queue * , struct file * , poll_table * ) ;
__inline static bool vb2_is_busy(struct vb2_queue *q )
{
  {
  return (q->num_buffers != 0U);
}
}
__inline static void *vb2_get_drv_priv(struct vb2_queue *q )
{
  {
  return (q->drv_priv);
}
}
__inline static void vb2_set_plane_payload(struct vb2_buffer *vb , unsigned int plane_no ,
                                           unsigned long size )
{
  {
  if (plane_no < vb->num_planes) {
    vb->v4l2_planes[plane_no].bytesused = (__u32 )size;
  } else {
  }
  return;
}
}
__inline static unsigned long vb2_get_plane_payload(struct vb2_buffer *vb , unsigned int plane_no )
{
  {
  if (plane_no < vb->num_planes) {
    return ((unsigned long )vb->v4l2_planes[plane_no].bytesused);
  } else {
  }
  return (0UL);
}
}
__inline static unsigned long vb2_plane_size(struct vb2_buffer *vb , unsigned int plane_no )
{
  {
  if (plane_no < vb->num_planes) {
    return ((unsigned long )vb->v4l2_planes[plane_no].length);
  } else {
  }
  return (0UL);
}
}
extern struct vb2_mem_ops const vb2_vmalloc_memops ;
int uvc_queue_init(struct uvc_video_queue *queue , enum v4l2_buf_type type , int drop_corrupted ) ;
int uvc_alloc_buffers(struct uvc_video_queue *queue , struct v4l2_requestbuffers *rb ) ;
void uvc_free_buffers(struct uvc_video_queue *queue ) ;
int uvc_query_buffer(struct uvc_video_queue *queue , struct v4l2_buffer *buf ) ;
int uvc_queue_buffer(struct uvc_video_queue *queue , struct v4l2_buffer *buf ) ;
int uvc_dequeue_buffer(struct uvc_video_queue *queue , struct v4l2_buffer *buf , int nonblocking ) ;
int uvc_queue_enable(struct uvc_video_queue *queue , int enable ) ;
void uvc_queue_cancel(struct uvc_video_queue *queue , int disconnect ) ;
struct uvc_buffer *uvc_queue_next_buffer(struct uvc_video_queue *queue , struct uvc_buffer *buf ) ;
int uvc_queue_mmap(struct uvc_video_queue *queue , struct vm_area_struct *vma ) ;
unsigned int uvc_queue_poll(struct uvc_video_queue *queue , struct file *file , poll_table *wait ) ;
int uvc_queue_allocated(struct uvc_video_queue *queue ) ;
void uvc_video_clock_update(struct uvc_streaming *stream , struct v4l2_buffer *v4l2_buf ,
                            struct uvc_buffer *buf ) ;
static int uvc_queue_setup(struct vb2_queue *vq , struct v4l2_format const *fmt ,
                           unsigned int *nbuffers , unsigned int *nplanes , unsigned int *sizes ,
                           void **alloc_ctxs )
{
  struct uvc_video_queue *queue ;
  void *tmp ;
  struct uvc_streaming *stream ;
  struct uvc_video_queue const *__mptr ;
  {
  {
  tmp = vb2_get_drv_priv(vq);
  queue = (struct uvc_video_queue *)tmp;
  __mptr = (struct uvc_video_queue const *)queue;
  stream = (struct uvc_streaming *)__mptr + 0xfffffffffffffea8UL;
  }
  if (*nbuffers > 32U) {
    *nbuffers = 32U;
  } else {
  }
  *nplanes = 1U;
  *sizes = stream->ctrl.dwMaxVideoFrameSize;
  return (0);
}
}
static int uvc_buffer_prepare(struct vb2_buffer *vb )
{
  struct uvc_video_queue *queue ;
  void *tmp ;
  struct uvc_buffer *buf ;
  struct vb2_buffer const *__mptr ;
  unsigned long tmp___0 ;
  unsigned long tmp___1 ;
  long tmp___2 ;
  unsigned long tmp___3 ;
  unsigned long tmp___4 ;
  {
  {
  tmp = vb2_get_drv_priv(vb->vb2_queue);
  queue = (struct uvc_video_queue *)tmp;
  __mptr = (struct vb2_buffer const *)vb;
  buf = (struct uvc_buffer *)__mptr;
  }
  if (vb->v4l2_buf.type == 2U) {
    {
    tmp___0 = vb2_get_plane_payload(vb, 0U);
    tmp___1 = vb2_plane_size(vb, 0U);
    }
    if (tmp___0 > tmp___1) {
      if ((uvc_trace_param & 16U) != 0U) {
        {
        printk("\017uvcvideo: [E] Bytes used out of bounds.\n");
        }
      } else {
      }
      return (-22);
    } else {
    }
  } else {
  }
  {
  tmp___2 = ldv__builtin_expect((long )((int )queue->flags) & 1L, 0L);
  }
  if (tmp___2 != 0L) {
    return (-19);
  } else {
  }
  {
  buf->state = 1;
  buf->error = 0U;
  buf->mem = vb2_plane_vaddr(vb, 0U);
  tmp___3 = vb2_plane_size(vb, 0U);
  buf->length = (unsigned int )tmp___3;
  }
  if (vb->v4l2_buf.type == 1U) {
    buf->bytesused = 0U;
  } else {
    {
    tmp___4 = vb2_get_plane_payload(vb, 0U);
    buf->bytesused = (unsigned int )tmp___4;
    }
  }
  return (0);
}
}
static void uvc_buffer_queue(struct vb2_buffer *vb )
{
  struct uvc_video_queue *queue ;
  void *tmp ;
  struct uvc_buffer *buf ;
  struct vb2_buffer const *__mptr ;
  unsigned long flags ;
  raw_spinlock_t *tmp___0 ;
  long tmp___1 ;
  {
  {
  tmp = vb2_get_drv_priv(vb->vb2_queue);
  queue = (struct uvc_video_queue *)tmp;
  __mptr = (struct vb2_buffer const *)vb;
  buf = (struct uvc_buffer *)__mptr;
  tmp___0 = spinlock_check(& queue->irqlock);
  flags = _raw_spin_lock_irqsave(tmp___0);
  tmp___1 = ldv__builtin_expect((queue->flags & 1U) == 0U, 1L);
  }
  if (tmp___1 != 0L) {
    {
    list_add_tail(& buf->queue, & queue->irqqueue);
    }
  } else {
    {
    buf->state = 5;
    vb2_buffer_done(& buf->buf, 6);
    }
  }
  {
  spin_unlock_irqrestore(& queue->irqlock, flags);
  }
  return;
}
}
static int uvc_buffer_finish(struct vb2_buffer *vb )
{
  struct uvc_video_queue *queue ;
  void *tmp ;
  struct uvc_streaming *stream ;
  struct uvc_video_queue const *__mptr ;
  struct uvc_buffer *buf ;
  struct vb2_buffer const *__mptr___0 ;
  {
  {
  tmp = vb2_get_drv_priv(vb->vb2_queue);
  queue = (struct uvc_video_queue *)tmp;
  __mptr = (struct uvc_video_queue const *)queue;
  stream = (struct uvc_streaming *)__mptr + 0xfffffffffffffea8UL;
  __mptr___0 = (struct vb2_buffer const *)vb;
  buf = (struct uvc_buffer *)__mptr___0;
  uvc_video_clock_update(stream, & vb->v4l2_buf, buf);
  }
  return (0);
}
}
static void uvc_wait_prepare(struct vb2_queue *vq )
{
  struct uvc_video_queue *queue ;
  void *tmp ;
  {
  {
  tmp = vb2_get_drv_priv(vq);
  queue = (struct uvc_video_queue *)tmp;
  mutex_unlock(& queue->mutex);
  }
  return;
}
}
static void uvc_wait_finish(struct vb2_queue *vq )
{
  struct uvc_video_queue *queue ;
  void *tmp ;
  {
  {
  tmp = vb2_get_drv_priv(vq);
  queue = (struct uvc_video_queue *)tmp;
  mutex_lock_nested(& queue->mutex, 0U);
  }
  return;
}
}
static struct vb2_ops uvc_queue_qops =
     {& uvc_queue_setup, & uvc_wait_prepare, & uvc_wait_finish, 0, & uvc_buffer_prepare,
    & uvc_buffer_finish, 0, 0, 0, & uvc_buffer_queue};
int uvc_queue_init(struct uvc_video_queue *queue , enum v4l2_buf_type type , int drop_corrupted )
{
  int ret ;
  struct lock_class_key __key ;
  struct lock_class_key __key___0 ;
  {
  {
  queue->queue.type = type;
  queue->queue.io_modes = 19U;
  queue->queue.drv_priv = (void *)queue;
  queue->queue.buf_struct_size = 888U;
  queue->queue.ops = (struct vb2_ops const *)(& uvc_queue_qops);
  queue->queue.mem_ops = & vb2_vmalloc_memops;
  queue->queue.timestamp_type = 8192U;
  ret = vb2_queue_init(& queue->queue);
  }
  if (ret != 0) {
    return (ret);
  } else {
  }
  {
  __mutex_init(& queue->mutex, "&queue->mutex", & __key);
  spinlock_check(& queue->irqlock);
  __raw_spin_lock_init(& queue->irqlock.__annonCompField19.rlock, "&(&queue->irqlock)->rlock",
                       & __key___0);
  INIT_LIST_HEAD(& queue->irqqueue);
  queue->flags = drop_corrupted != 0 ? 2U : 0U;
  }
  return (0);
}
}
int uvc_alloc_buffers(struct uvc_video_queue *queue , struct v4l2_requestbuffers *rb )
{
  int ret ;
  {
  {
  mutex_lock_nested(& queue->mutex, 0U);
  ret = vb2_reqbufs(& queue->queue, rb);
  mutex_unlock(& queue->mutex);
  }
  return (ret != 0 ? ret : (int )rb->count);
}
}
void uvc_free_buffers(struct uvc_video_queue *queue )
{
  {
  {
  mutex_lock_nested(& queue->mutex, 0U);
  vb2_queue_release(& queue->queue);
  mutex_unlock(& queue->mutex);
  }
  return;
}
}
int uvc_query_buffer(struct uvc_video_queue *queue , struct v4l2_buffer *buf )
{
  int ret ;
  {
  {
  mutex_lock_nested(& queue->mutex, 0U);
  ret = vb2_querybuf(& queue->queue, buf);
  mutex_unlock(& queue->mutex);
  }
  return (ret);
}
}
int uvc_queue_buffer(struct uvc_video_queue *queue , struct v4l2_buffer *buf )
{
  int ret ;
  {
  {
  mutex_lock_nested(& queue->mutex, 0U);
  ret = vb2_qbuf(& queue->queue, buf);
  mutex_unlock(& queue->mutex);
  }
  return (ret);
}
}
int uvc_dequeue_buffer(struct uvc_video_queue *queue , struct v4l2_buffer *buf , int nonblocking )
{
  int ret ;
  {
  {
  mutex_lock_nested(& queue->mutex, 0U);
  ret = vb2_dqbuf(& queue->queue, buf, nonblocking != 0);
  mutex_unlock(& queue->mutex);
  }
  return (ret);
}
}
int uvc_queue_mmap(struct uvc_video_queue *queue , struct vm_area_struct *vma )
{
  int ret ;
  {
  {
  mutex_lock_nested(& queue->mutex, 0U);
  ret = vb2_mmap(& queue->queue, vma);
  mutex_unlock(& queue->mutex);
  }
  return (ret);
}
}
unsigned int uvc_queue_poll(struct uvc_video_queue *queue , struct file *file , poll_table *wait )
{
  unsigned int ret ;
  {
  {
  mutex_lock_nested(& queue->mutex, 0U);
  ret = vb2_poll(& queue->queue, file, wait);
  mutex_unlock(& queue->mutex);
  }
  return (ret);
}
}
int uvc_queue_allocated(struct uvc_video_queue *queue )
{
  int allocated ;
  bool tmp ;
  {
  {
  mutex_lock_nested(& queue->mutex, 0U);
  tmp = vb2_is_busy(& queue->queue);
  allocated = (int )tmp;
  mutex_unlock(& queue->mutex);
  }
  return (allocated);
}
}
int uvc_queue_enable(struct uvc_video_queue *queue , int enable )
{
  unsigned long flags ;
  int ret ;
  raw_spinlock_t *tmp ;
  {
  {
  mutex_lock_nested(& queue->mutex, 0U);
  }
  if (enable != 0) {
    {
    ret = vb2_streamon(& queue->queue, queue->queue.type);
    }
    if (ret < 0) {
      goto done;
    } else {
    }
    queue->buf_used = 0U;
  } else {
    {
    ret = vb2_streamoff(& queue->queue, queue->queue.type);
    }
    if (ret < 0) {
      goto done;
    } else {
    }
    {
    tmp = spinlock_check(& queue->irqlock);
    flags = _raw_spin_lock_irqsave(tmp);
    INIT_LIST_HEAD(& queue->irqqueue);
    spin_unlock_irqrestore(& queue->irqlock, flags);
    }
  }
  done:
  {
  mutex_unlock(& queue->mutex);
  }
  return (ret);
}
}
void uvc_queue_cancel(struct uvc_video_queue *queue , int disconnect )
{
  struct uvc_buffer *buf ;
  unsigned long flags ;
  raw_spinlock_t *tmp ;
  struct list_head const *__mptr ;
  int tmp___0 ;
  {
  {
  tmp = spinlock_check(& queue->irqlock);
  flags = _raw_spin_lock_irqsave(tmp);
  }
  goto ldv_34634;
  ldv_34633:
  {
  __mptr = (struct list_head const *)queue->irqqueue.next;
  buf = (struct uvc_buffer *)__mptr + 0xfffffffffffffcb8UL;
  list_del(& buf->queue);
  buf->state = 5;
  vb2_buffer_done(& buf->buf, 6);
  }
  ldv_34634:
  {
  tmp___0 = list_empty((struct list_head const *)(& queue->irqqueue));
  }
  if (tmp___0 == 0) {
    goto ldv_34633;
  } else {
  }
  if (disconnect != 0) {
    queue->flags = queue->flags | 1U;
  } else {
  }
  {
  spin_unlock_irqrestore(& queue->irqlock, flags);
  }
  return;
}
}
struct uvc_buffer *uvc_queue_next_buffer(struct uvc_video_queue *queue , struct uvc_buffer *buf )
{
  struct uvc_buffer *nextbuf ;
  unsigned long flags ;
  raw_spinlock_t *tmp ;
  struct list_head const *__mptr ;
  int tmp___0 ;
  {
  if ((queue->flags & 2U) != 0U && buf->error != 0U) {
    {
    buf->error = 0U;
    buf->state = 1;
    buf->bytesused = 0U;
    vb2_set_plane_payload(& buf->buf, 0U, 0UL);
    }
    return (buf);
  } else {
  }
  {
  tmp = spinlock_check(& queue->irqlock);
  flags = _raw_spin_lock_irqsave(tmp);
  list_del(& buf->queue);
  tmp___0 = list_empty((struct list_head const *)(& queue->irqqueue));
  }
  if (tmp___0 == 0) {
    __mptr = (struct list_head const *)queue->irqqueue.next;
    nextbuf = (struct uvc_buffer *)__mptr + 0xfffffffffffffcb8UL;
  } else {
    nextbuf = (struct uvc_buffer *)0;
  }
  {
  spin_unlock_irqrestore(& queue->irqlock, flags);
  buf->state = buf->error != 0U ? 6 : 4;
  vb2_set_plane_payload(& buf->buf, 0U, (unsigned long )buf->bytesused);
  vb2_buffer_done(& buf->buf, 5);
  }
  return (nextbuf);
}
}
void ldv_dummy_resourceless_instance_callback_4_12(void (*arg0)(struct vb2_queue * ) ,
                                                   struct vb2_queue *arg1 )
{
  {
  {
  uvc_wait_finish(arg1);
  }
  return;
}
}
void ldv_dummy_resourceless_instance_callback_4_13(void (*arg0)(struct vb2_queue * ) ,
                                                   struct vb2_queue *arg1 )
{
  {
  {
  uvc_wait_prepare(arg1);
  }
  return;
}
}
void ldv_dummy_resourceless_instance_callback_4_3(int (*arg0)(struct vb2_buffer * ) ,
                                                  struct vb2_buffer *arg1 )
{
  {
  {
  uvc_buffer_finish(arg1);
  }
  return;
}
}
void ldv_dummy_resourceless_instance_callback_4_7(int (*arg0)(struct vb2_buffer * ) ,
                                                  struct vb2_buffer *arg1 )
{
  {
  {
  uvc_buffer_prepare(arg1);
  }
  return;
}
}
void ldv_dummy_resourceless_instance_callback_4_8(void (*arg0)(struct vb2_buffer * ) ,
                                                  struct vb2_buffer *arg1 )
{
  {
  {
  uvc_buffer_queue(arg1);
  }
  return;
}
}
void ldv_dummy_resourceless_instance_callback_4_9(int (*arg0)(struct vb2_queue * ,
                                                              struct v4l2_format * ,
                                                              unsigned int * , unsigned int * ,
                                                              unsigned int * , void ** ) ,
                                                  struct vb2_queue *arg1 , struct v4l2_format *arg2 ,
                                                  unsigned int *arg3 , unsigned int *arg4 ,
                                                  unsigned int *arg5 , void **arg6 )
{
  {
  {
  uvc_queue_setup(arg1, (struct v4l2_format const *)arg2, arg3, arg4, arg5, arg6);
  }
  return;
}
}
void ldv_atomic_dec(atomic_t *v ) ;
int ldv_atomic_add_return(int i , atomic_t *v ) ;
int ldv_filter_err_code(int ret_val ) ;
extern void might_fault(void) ;
extern void __bad_percpu_size(void) ;
__inline static void atomic_dec(atomic_t *v ) ;
__inline static int atomic_add_return(int i , atomic_t *v ) ;
extern unsigned long kernel_stack ;
__inline static struct thread_info *current_thread_info(void)
{
  struct thread_info *ti ;
  unsigned long pfo_ret__ ;
  {
  {
  if (8UL == 1UL) {
    goto case_1;
  } else {
  }
  if (8UL == 2UL) {
    goto case_2;
  } else {
  }
  if (8UL == 4UL) {
    goto case_4;
  } else {
  }
  if (8UL == 8UL) {
    goto case_8;
  } else {
  }
  goto switch_default;
  case_1:
  __asm__ ("movb %%gs:%P1,%0": "=q" (pfo_ret__): "p" (& kernel_stack));
  goto ldv_6240;
  case_2:
  __asm__ ("movw %%gs:%P1,%0": "=r" (pfo_ret__): "p" (& kernel_stack));
  goto ldv_6240;
  case_4:
  __asm__ ("movl %%gs:%P1,%0": "=r" (pfo_ret__): "p" (& kernel_stack));
  goto ldv_6240;
  case_8:
  __asm__ ("movq %%gs:%P1,%0": "=r" (pfo_ret__): "p" (& kernel_stack));
  goto ldv_6240;
  switch_default:
  {
  __bad_percpu_size();
  }
  switch_break: ;
  }
  ldv_6240:
  ti = (struct thread_info *)(pfo_ret__ - 8152UL);
  return (ti);
}
}
__inline static bool __chk_range_not_ok(unsigned long addr , unsigned long size ,
                                        unsigned long limit )
{
  {
  addr = addr + size;
  if (addr < size) {
    return (1);
  } else {
  }
  return (addr > limit);
}
}
extern int __get_user_bad(void) ;
extern void __put_user_bad(void) ;
extern unsigned long __clear_user(void * , unsigned long ) ;
extern unsigned long copy_user_enhanced_fast_string(void * , void const * , unsigned int ) ;
extern unsigned long copy_user_generic_string(void * , void const * , unsigned int ) ;
extern unsigned long copy_user_generic_unrolled(void * , void const * , unsigned int ) ;
__inline static unsigned long copy_user_generic(void *to , void const *from , unsigned int len )
{
  unsigned int ret ;
  {
  __asm__ volatile ("661:\n\tcall %P4\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (3*32+16)\n .byte 662b-661b\n .byte 6641f-6631f\n .long 661b - .\n .long 6632f - .\n .word (9*32+ 9)\n .byte 662b-661b\n .byte 6642f-6632f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n .byte 0xff + (6642f-6632f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\tcall %P5\n6641:\n\t6632:\n\tcall %P6\n6642:\n\t.popsection": "=a" (ret),
                       "=D" (to), "=S" (from), "=d" (len): [old] "i" (& copy_user_generic_unrolled),
                       [new1] "i" (& copy_user_generic_string), [new2] "i" (& copy_user_enhanced_fast_string),
                       "1" (to), "2" (from), "3" (len): "memory", "rcx", "r8", "r9",
                       "r10", "r11");
  return ((unsigned long )ret);
}
}
extern unsigned long copy_in_user(void * , void const * , unsigned int ) ;
__inline static int __copy_from_user_nocheck(void *dst , void const *src , unsigned int size )
{
  int ret ;
  unsigned long tmp ;
  long tmp___0 ;
  long tmp___1 ;
  unsigned long tmp___2 ;
  {
  {
  ret = 0;
  tmp = copy_user_generic(dst, src, size);
  }
  return ((int )tmp);
  {
  if (size == 1U) {
    goto case_1;
  } else {
  }
  if (size == 2U) {
    goto case_2;
  } else {
  }
  if (size == 4U) {
    goto case_4;
  } else {
  }
  if (size == 8U) {
    goto case_8;
  } else {
  }
  if (size == 10U) {
    goto case_10;
  } else {
  }
  if (size == 16U) {
    goto case_16;
  } else {
  }
  goto switch_default;
  case_1:
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovb %2,%b1\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\txorb %b1,%b1\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (ret),
                       "=q" (*((u8 *)dst)): "m" (*((struct __large_struct *)src)),
                       "i" (1), "0" (ret));
  return (ret);
  case_2:
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovw %2,%w1\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\txorw %w1,%w1\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (ret),
                       "=r" (*((u16 *)dst)): "m" (*((struct __large_struct *)src)),
                       "i" (2), "0" (ret));
  return (ret);
  case_4:
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovl %2,%k1\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\txorl %k1,%k1\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (ret),
                       "=r" (*((u32 *)dst)): "m" (*((struct __large_struct *)src)),
                       "i" (4), "0" (ret));
  return (ret);
  case_8:
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovq %2,%1\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\txorq %1,%1\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (ret),
                       "=r" (*((u64 *)dst)): "m" (*((struct __large_struct *)src)),
                       "i" (8), "0" (ret));
  return (ret);
  case_10:
  {
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovq %2,%1\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\txorq %1,%1\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (ret),
                       "=r" (*((u64 *)dst)): "m" (*((struct __large_struct *)src)),
                       "i" (10), "0" (ret));
  tmp___0 = ldv__builtin_expect(ret != 0, 0L);
  }
  if (tmp___0 != 0L) {
    return (ret);
  } else {
  }
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovw %2,%w1\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\txorw %w1,%w1\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (ret),
                       "=r" (*((u16 *)dst + 8U)): "m" (*((struct __large_struct *)src + 8U)),
                       "i" (2), "0" (ret));
  return (ret);
  case_16:
  {
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovq %2,%1\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\txorq %1,%1\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (ret),
                       "=r" (*((u64 *)dst)): "m" (*((struct __large_struct *)src)),
                       "i" (16), "0" (ret));
  tmp___1 = ldv__builtin_expect(ret != 0, 0L);
  }
  if (tmp___1 != 0L) {
    return (ret);
  } else {
  }
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovq %2,%1\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\txorq %1,%1\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (ret),
                       "=r" (*((u64 *)dst + 8U)): "m" (*((struct __large_struct *)src + 8U)),
                       "i" (8), "0" (ret));
  return (ret);
  switch_default:
  {
  tmp___2 = copy_user_generic(dst, src, size);
  }
  return ((int )tmp___2);
  switch_break: ;
  }
}
}
__inline static int __copy_from_user(void *dst , void const *src , unsigned int size )
{
  int tmp ;
  {
  {
  might_fault();
  tmp = __copy_from_user_nocheck(dst, src, size);
  }
  return (tmp);
}
}
__inline static int __copy_to_user_nocheck(void *dst , void const *src , unsigned int size )
{
  int ret ;
  unsigned long tmp ;
  long tmp___0 ;
  long tmp___1 ;
  unsigned long tmp___2 ;
  {
  {
  ret = 0;
  tmp = copy_user_generic(dst, src, size);
  }
  return ((int )tmp);
  {
  if (size == 1U) {
    goto case_1;
  } else {
  }
  if (size == 2U) {
    goto case_2;
  } else {
  }
  if (size == 4U) {
    goto case_4;
  } else {
  }
  if (size == 8U) {
    goto case_8;
  } else {
  }
  if (size == 10U) {
    goto case_10;
  } else {
  }
  if (size == 16U) {
    goto case_16;
  } else {
  }
  goto switch_default;
  case_1:
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovb %b1,%2\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (ret): "iq" (*((u8 *)src)),
                       "m" (*((struct __large_struct *)dst)), "i" (1), "0" (ret));
  return (ret);
  case_2:
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovw %w1,%2\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (ret): "ir" (*((u16 *)src)),
                       "m" (*((struct __large_struct *)dst)), "i" (2), "0" (ret));
  return (ret);
  case_4:
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovl %k1,%2\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (ret): "ir" (*((u32 *)src)),
                       "m" (*((struct __large_struct *)dst)), "i" (4), "0" (ret));
  return (ret);
  case_8:
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovq %1,%2\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (ret): "er" (*((u64 *)src)),
                       "m" (*((struct __large_struct *)dst)), "i" (8), "0" (ret));
  return (ret);
  case_10:
  {
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovq %1,%2\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (ret): "er" (*((u64 *)src)),
                       "m" (*((struct __large_struct *)dst)), "i" (10), "0" (ret));
  tmp___0 = ldv__builtin_expect(ret != 0, 0L);
  }
  if (tmp___0 != 0L) {
    return (ret);
  } else {
  }
  __asm__ volatile ("": : : "memory");
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovw %w1,%2\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (ret): "ir" (*((u16 *)src + 4UL)),
                       "m" (*((struct __large_struct *)dst + 4U)), "i" (2), "0" (ret));
  return (ret);
  case_16:
  {
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovq %1,%2\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (ret): "er" (*((u64 *)src)),
                       "m" (*((struct __large_struct *)dst)), "i" (16), "0" (ret));
  tmp___1 = ldv__builtin_expect(ret != 0, 0L);
  }
  if (tmp___1 != 0L) {
    return (ret);
  } else {
  }
  __asm__ volatile ("": : : "memory");
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovq %1,%2\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (ret): "er" (*((u64 *)src + 1UL)),
                       "m" (*((struct __large_struct *)dst + 1U)), "i" (8), "0" (ret));
  return (ret);
  switch_default:
  {
  tmp___2 = copy_user_generic(dst, src, size);
  }
  return ((int )tmp___2);
  switch_break: ;
  }
}
}
__inline static int __copy_to_user(void *dst , void const *src , unsigned int size )
{
  int tmp ;
  {
  {
  might_fault();
  tmp = __copy_to_user_nocheck(dst, src, size);
  }
  return (tmp);
}
}
extern unsigned long _copy_from_user(void * , void const * , unsigned int ) ;
extern void __copy_from_user_overflow(void) ;
__inline static unsigned long copy_from_user(void *to , void const *from , unsigned long n )
{
  int sz ;
  long tmp ;
  long tmp___0 ;
  {
  {
  sz = -1;
  might_fault();
  tmp = ldv__builtin_expect(sz < 0, 1L);
  }
  if (tmp != 0L) {
    {
    n = _copy_from_user(to, from, (unsigned int )n);
    }
  } else {
    {
    tmp___0 = ldv__builtin_expect((unsigned long )sz >= n, 1L);
    }
    if (tmp___0 != 0L) {
      {
      n = _copy_from_user(to, from, (unsigned int )n);
      }
    } else {
      {
      __copy_from_user_overflow();
      }
    }
  }
  return (n);
}
}
static void *ldv_dev_get_drvdata_18___0(struct device const *dev ) ;
extern int usb_autopm_get_interface(struct usb_interface * ) ;
extern void usb_autopm_put_interface(struct usb_interface * ) ;
__inline static int usb_make_path(struct usb_device *dev , char *buf , size_t size )
{
  int actual ;
  {
  {
  actual = snprintf(buf, size, "usb-%s-%s", (dev->bus)->bus_name, (char *)(& dev->devpath));
  }
  return (actual < (int )size ? actual : -1);
}
}
__inline static void *compat_ptr(compat_uptr_t uptr )
{
  {
  return ((void *)((unsigned long )uptr));
}
}
extern void *compat_alloc_user_space(unsigned long ) ;
__inline static void *kzalloc(size_t size , gfp_t flags ) ;
extern int v4l2_prio_change(struct v4l2_prio_state * , enum v4l2_priority * , enum v4l2_priority ) ;
extern enum v4l2_priority v4l2_prio_max(struct v4l2_prio_state * ) ;
extern int v4l2_prio_check(struct v4l2_prio_state * , enum v4l2_priority ) ;
__inline static void *video_get_drvdata___0(struct video_device *vdev )
{
  void *tmp ;
  {
  {
  tmp = ldv_dev_get_drvdata_18___0((struct device const *)(& vdev->dev));
  }
  return (tmp);
}
}
extern struct video_device *video_devdata(struct file * ) ;
__inline static void *video_drvdata(struct file *file )
{
  struct video_device *tmp ;
  void *tmp___0 ;
  {
  {
  tmp = video_devdata(file);
  tmp___0 = video_get_drvdata___0(tmp);
  }
  return (tmp___0);
}
}
extern int v4l2_event_dequeue(struct v4l2_fh * , struct v4l2_event * , int ) ;
extern int v4l2_event_subscribe(struct v4l2_fh * , struct v4l2_event_subscription const * ,
                                unsigned int , struct v4l2_subscribed_event_ops const * ) ;
extern int v4l2_event_unsubscribe(struct v4l2_fh * , struct v4l2_event_subscription const * ) ;
extern void v4l_printk_ioctl(char const * , unsigned int ) ;
extern long video_usercopy(struct file * , unsigned int , unsigned long , long (*)(struct file * ,
                                                                                     unsigned int ,
                                                                                     void * ) ) ;
extern void v4l2_fh_init(struct v4l2_fh * , struct video_device * ) ;
extern void v4l2_fh_add(struct v4l2_fh * ) ;
extern void v4l2_fh_del(struct v4l2_fh * ) ;
extern void v4l2_fh_exit(struct v4l2_fh * ) ;
__inline static bool vb2_is_streaming(struct vb2_queue *q )
{
  {
  return ((int )q->streaming != 0);
}
}
__inline static int uvc_queue_streaming(struct uvc_video_queue *queue )
{
  bool tmp ;
  {
  {
  tmp = vb2_is_streaming(& queue->queue);
  }
  return ((int )tmp);
}
}
int uvc_video_enable(struct uvc_streaming *stream , int enable ) ;
int uvc_probe_video(struct uvc_streaming *stream , struct uvc_streaming_control *probe ) ;
int uvc_query_ctrl(struct uvc_device *dev , __u8 query , __u8 unit , __u8 intfnum ,
                   __u8 cs , void *data , __u16 size ) ;
struct v4l2_subscribed_event_ops const uvc_ctrl_sub_ev_ops ;
int uvc_query_v4l2_ctrl(struct uvc_video_chain *chain , struct v4l2_queryctrl *v4l2_ctrl ) ;
int uvc_query_v4l2_menu(struct uvc_video_chain *chain , struct v4l2_querymenu *query_menu ) ;
int uvc_ctrl_add_mapping(struct uvc_video_chain *chain , struct uvc_control_mapping const *mapping ) ;
int uvc_ctrl_begin(struct uvc_video_chain *chain ) ;
int __uvc_ctrl_commit(struct uvc_fh *handle , int rollback , struct v4l2_ext_control const *xctrls ,
                      unsigned int xctrls_count ) ;
__inline static int uvc_ctrl_commit(struct uvc_fh *handle , struct v4l2_ext_control const *xctrls ,
                                    unsigned int xctrls_count )
{
  int tmp ;
  {
  {
  tmp = __uvc_ctrl_commit(handle, 0, xctrls, xctrls_count);
  }
  return (tmp);
}
}
__inline static int uvc_ctrl_rollback(struct uvc_fh *handle )
{
  int tmp ;
  {
  {
  tmp = __uvc_ctrl_commit(handle, 1, (struct v4l2_ext_control const *)0, 0U);
  }
  return (tmp);
}
}
int uvc_ctrl_get(struct uvc_video_chain *chain , struct v4l2_ext_control *xctrl ) ;
int uvc_ctrl_set(struct uvc_video_chain *chain , struct v4l2_ext_control *xctrl ) ;
int uvc_xu_ctrl_query(struct uvc_video_chain *chain , struct uvc_xu_control_query *xqry ) ;
static int uvc_ioctl_ctrl_map(struct uvc_video_chain *chain , struct uvc_xu_control_mapping *xmap )
{
  struct uvc_control_mapping *map ;
  unsigned int size ;
  int ret ;
  void *tmp ;
  void *tmp___0 ;
  unsigned long tmp___1 ;
  {
  {
  tmp = kzalloc(144UL, 208U);
  map = (struct uvc_control_mapping *)tmp;
  }
  if ((unsigned long )map == (unsigned long )((struct uvc_control_mapping *)0)) {
    return (-12);
  } else {
  }
  {
  map->id = xmap->id;
  memcpy((void *)(& map->name), (void const *)(& xmap->name), 32UL);
  memcpy((void *)(& map->entity), (void const *)(& xmap->entity), 16UL);
  map->selector = xmap->selector;
  map->size = xmap->size;
  map->offset = xmap->offset;
  map->v4l2_type = (enum v4l2_ctrl_type )xmap->v4l2_type;
  map->data_type = xmap->data_type;
  }
  {
  if (xmap->v4l2_type == 1U) {
    goto case_1;
  } else {
  }
  if (xmap->v4l2_type == 2U) {
    goto case_2;
  } else {
  }
  if (xmap->v4l2_type == 4U) {
    goto case_4;
  } else {
  }
  if (xmap->v4l2_type == 3U) {
    goto case_3;
  } else {
  }
  goto switch_default;
  case_1: ;
  case_2: ;
  case_4: ;
  goto ldv_36563;
  case_3: ;
  if (xmap->menu_count - 1U > 31U) {
    ret = -22;
    goto done;
  } else {
  }
  {
  size = xmap->menu_count * 36U;
  tmp___0 = kmalloc((size_t )size, 208U);
  map->menu_info = (struct uvc_menu_info *)tmp___0;
  }
  if ((unsigned long )map->menu_info == (unsigned long )((struct uvc_menu_info *)0)) {
    ret = -12;
    goto done;
  } else {
  }
  {
  tmp___1 = copy_from_user((void *)map->menu_info, (void const *)xmap->menu_info,
                           (unsigned long )size);
  }
  if (tmp___1 != 0UL) {
    ret = -14;
    goto done;
  } else {
  }
  map->menu_count = xmap->menu_count;
  goto ldv_36563;
  switch_default: ;
  if ((uvc_trace_param & 4U) != 0U) {
    {
    printk("\017uvcvideo: Unsupported V4L2 control type %u.\n", xmap->v4l2_type);
    }
  } else {
  }
  ret = -25;
  goto done;
  switch_break: ;
  }
  ldv_36563:
  {
  ret = uvc_ctrl_add_mapping(chain, (struct uvc_control_mapping const *)map);
  }
  done:
  {
  kfree((void const *)map->menu_info);
  kfree((void const *)map);
  }
  return (ret);
}
}
static __u32 uvc_try_frame_interval(struct uvc_frame *frame , __u32 interval )
{
  unsigned int i ;
  __u32 best ;
  __u32 dist ;
  __u32 min ;
  __u32 max ;
  __u32 step ;
  {
  if ((unsigned int )frame->bFrameIntervalType != 0U) {
    best = 4294967295U;
    i = 0U;
    goto ldv_36576;
    ldv_36575:
    dist = interval > *(frame->dwFrameInterval + (unsigned long )i) ? interval - *(frame->dwFrameInterval + (unsigned long )i) : *(frame->dwFrameInterval + (unsigned long )i) - interval;
    if (dist > best) {
      goto ldv_36574;
    } else {
    }
    best = dist;
    i = i + 1U;
    ldv_36576: ;
    if (i < (unsigned int )frame->bFrameIntervalType) {
      goto ldv_36575;
    } else {
    }
    ldv_36574:
    interval = *(frame->dwFrameInterval + (unsigned long )(i - 1U));
  } else {
    min = *(frame->dwFrameInterval);
    max = *(frame->dwFrameInterval + 1UL);
    step = *(frame->dwFrameInterval + 2UL);
    interval = min + (((interval - min) + step / 2U) / step) * step;
    if (interval > max) {
      interval = max;
    } else {
    }
  }
  return (interval);
}
}
static int uvc_v4l2_try_format(struct uvc_streaming *stream , struct v4l2_format *fmt ,
                               struct uvc_streaming_control *probe , struct uvc_format **uvc_format ,
                               struct uvc_frame **uvc_frame )
{
  struct uvc_format *format ;
  struct uvc_frame *frame ;
  __u16 rw ;
  __u16 rh ;
  unsigned int d ;
  unsigned int maxd ;
  unsigned int i ;
  __u32 interval ;
  int ret ;
  __u8 *fcc ;
  __u16 w ;
  __u16 h ;
  __u16 _min1 ;
  __u16 _min2 ;
  __u16 _min1___0 ;
  __u16 _min2___0 ;
  {
  format = (struct uvc_format *)0;
  frame = (struct uvc_frame *)0;
  ret = 0;
  if (fmt->type != (__u32 )stream->type) {
    return (-22);
  } else {
  }
  fcc = (__u8 *)(& fmt->fmt.pix.pixelformat);
  if ((uvc_trace_param & 8U) != 0U) {
    {
    printk("\017uvcvideo: Trying format 0x%08x (%c%c%c%c): %ux%u.\n", fmt->fmt.pix.pixelformat,
           (int )*fcc, (int )*(fcc + 1UL), (int )*(fcc + 2UL), (int )*(fcc + 3UL),
           fmt->fmt.pix.width, fmt->fmt.pix.height);
    }
  } else {
  }
  i = 0U;
  goto ldv_36599;
  ldv_36598:
  format = stream->format + (unsigned long )i;
  if (format->fcc == fmt->fmt.pix.pixelformat) {
    goto ldv_36597;
  } else {
  }
  i = i + 1U;
  ldv_36599: ;
  if (i < stream->nformats) {
    goto ldv_36598;
  } else {
  }
  ldv_36597: ;
  if (i == stream->nformats) {
    format = stream->def_format;
    fmt->fmt.pix.pixelformat = format->fcc;
  } else {
  }
  rw = (__u16 )fmt->fmt.pix.width;
  rh = (__u16 )fmt->fmt.pix.height;
  maxd = 4294967295U;
  i = 0U;
  goto ldv_36610;
  ldv_36609:
  w = (format->frame + (unsigned long )i)->wWidth;
  h = (format->frame + (unsigned long )i)->wHeight;
  _min1 = w;
  _min2 = rw;
  _min1___0 = h;
  _min2___0 = rh;
  d = (unsigned int )(((int )_min1 < (int )_min2 ? _min1 : _min2) * ((int )_min1___0 < (int )_min2___0 ? _min1___0 : _min2___0));
  d = (unsigned int )((int )w * (int )h + (int )rw * (int )rh) - d * 2U;
  if (d < maxd) {
    maxd = d;
    frame = format->frame + (unsigned long )i;
  } else {
  }
  if (maxd == 0U) {
    goto ldv_36608;
  } else {
  }
  i = i + 1U;
  ldv_36610: ;
  if (i < format->nframes) {
    goto ldv_36609;
  } else {
  }
  ldv_36608: ;
  if ((unsigned long )frame == (unsigned long )((struct uvc_frame *)0)) {
    if ((uvc_trace_param & 8U) != 0U) {
      {
      printk("\017uvcvideo: Unsupported size %ux%u.\n", fmt->fmt.pix.width, fmt->fmt.pix.height);
      }
    } else {
    }
    return (-22);
  } else {
  }
  interval = frame->dwDefaultFrameInterval;
  if ((uvc_trace_param & 8U) != 0U) {
    {
    printk("\017uvcvideo: Using default frame interval %u.%u us (%u.%u fps).\n", interval / 10U,
           interval % 10U, 10000000U / interval, (100000000U / interval) % 10U);
    }
  } else {
  }
  {
  memset((void *)probe, 0, 34UL);
  probe->bmHint = 1U;
  probe->bFormatIndex = format->index;
  probe->bFrameIndex = frame->bFrameIndex;
  probe->dwFrameInterval = uvc_try_frame_interval(frame, interval);
  mutex_lock_nested(& stream->mutex, 0U);
  }
  if (((stream->dev)->quirks & 4U) != 0U) {
    probe->dwMaxVideoFrameSize = stream->ctrl.dwMaxVideoFrameSize;
  } else {
  }
  {
  ret = uvc_probe_video(stream, probe);
  mutex_unlock(& stream->mutex);
  }
  if (ret < 0) {
    goto done;
  } else {
  }
  fmt->fmt.pix.width = (__u32 )frame->wWidth;
  fmt->fmt.pix.height = (__u32 )frame->wHeight;
  fmt->fmt.pix.field = 1U;
  fmt->fmt.pix.bytesperline = (__u32 )(((int )format->bpp * (int )frame->wWidth) / 8);
  fmt->fmt.pix.sizeimage = probe->dwMaxVideoFrameSize;
  fmt->fmt.pix.colorspace = (__u32 )format->colorspace;
  fmt->fmt.pix.priv = 0U;
  if ((unsigned long )uvc_format != (unsigned long )((struct uvc_format **)0)) {
    *uvc_format = format;
  } else {
  }
  if ((unsigned long )uvc_frame != (unsigned long )((struct uvc_frame **)0)) {
    *uvc_frame = frame;
  } else {
  }
  done: ;
  return (ret);
}
}
static int uvc_v4l2_get_format(struct uvc_streaming *stream , struct v4l2_format *fmt )
{
  struct uvc_format *format ;
  struct uvc_frame *frame ;
  int ret ;
  {
  ret = 0;
  if (fmt->type != (__u32 )stream->type) {
    return (-22);
  } else {
  }
  {
  mutex_lock_nested(& stream->mutex, 0U);
  format = stream->cur_format;
  frame = stream->cur_frame;
  }
  if ((unsigned long )format == (unsigned long )((struct uvc_format *)0) || (unsigned long )frame == (unsigned long )((struct uvc_frame *)0)) {
    ret = -22;
    goto done;
  } else {
  }
  fmt->fmt.pix.pixelformat = format->fcc;
  fmt->fmt.pix.width = (__u32 )frame->wWidth;
  fmt->fmt.pix.height = (__u32 )frame->wHeight;
  fmt->fmt.pix.field = 1U;
  fmt->fmt.pix.bytesperline = (__u32 )(((int )format->bpp * (int )frame->wWidth) / 8);
  fmt->fmt.pix.sizeimage = stream->ctrl.dwMaxVideoFrameSize;
  fmt->fmt.pix.colorspace = (__u32 )format->colorspace;
  fmt->fmt.pix.priv = 0U;
  done:
  {
  mutex_unlock(& stream->mutex);
  }
  return (ret);
}
}
static int uvc_v4l2_set_format(struct uvc_streaming *stream , struct v4l2_format *fmt )
{
  struct uvc_streaming_control probe ;
  struct uvc_format *format ;
  struct uvc_frame *frame ;
  int ret ;
  int tmp ;
  {
  if (fmt->type != (__u32 )stream->type) {
    return (-22);
  } else {
  }
  {
  ret = uvc_v4l2_try_format(stream, fmt, & probe, & format, & frame);
  }
  if (ret < 0) {
    return (ret);
  } else {
  }
  {
  mutex_lock_nested(& stream->mutex, 0U);
  tmp = uvc_queue_allocated(& stream->queue);
  }
  if (tmp != 0) {
    ret = -16;
    goto done;
  } else {
  }
  stream->ctrl = probe;
  stream->cur_format = format;
  stream->cur_frame = frame;
  done:
  {
  mutex_unlock(& stream->mutex);
  }
  return (ret);
}
}
static int uvc_v4l2_get_streamparm(struct uvc_streaming *stream , struct v4l2_streamparm *parm )
{
  uint32_t numerator ;
  uint32_t denominator ;
  {
  if (parm->type != (__u32 )stream->type) {
    return (-22);
  } else {
  }
  {
  mutex_lock_nested(& stream->mutex, 0U);
  numerator = stream->ctrl.dwFrameInterval;
  mutex_unlock(& stream->mutex);
  denominator = 10000000U;
  uvc_simplify_fraction(& numerator, & denominator, 8U, 333U);
  memset((void *)parm, 0, 204UL);
  parm->type = (__u32 )stream->type;
  }
  if ((unsigned int )stream->type == 1U) {
    parm->parm.capture.capability = 4096U;
    parm->parm.capture.capturemode = 0U;
    parm->parm.capture.timeperframe.numerator = numerator;
    parm->parm.capture.timeperframe.denominator = denominator;
    parm->parm.capture.extendedmode = 0U;
    parm->parm.capture.readbuffers = 0U;
  } else {
    parm->parm.output.capability = 4096U;
    parm->parm.output.outputmode = 0U;
    parm->parm.output.timeperframe.numerator = numerator;
    parm->parm.output.timeperframe.denominator = denominator;
  }
  return (0);
}
}
static int uvc_v4l2_set_streamparm(struct uvc_streaming *stream , struct v4l2_streamparm *parm )
{
  struct uvc_streaming_control probe ;
  struct v4l2_fract timeperframe ;
  uint32_t interval ;
  int ret ;
  int tmp ;
  {
  if (parm->type != (__u32 )stream->type) {
    return (-22);
  } else {
  }
  if (parm->type == 1U) {
    timeperframe = parm->parm.capture.timeperframe;
  } else {
    timeperframe = parm->parm.output.timeperframe;
  }
  {
  interval = uvc_fraction_to_interval(timeperframe.numerator, timeperframe.denominator);
  }
  if ((uvc_trace_param & 8U) != 0U) {
    {
    printk("\017uvcvideo: Setting frame interval to %u/%u (%u).\n", timeperframe.numerator,
           timeperframe.denominator, interval);
    }
  } else {
  }
  {
  mutex_lock_nested(& stream->mutex, 0U);
  tmp = uvc_queue_streaming(& stream->queue);
  }
  if (tmp != 0) {
    {
    mutex_unlock(& stream->mutex);
    }
    return (-16);
  } else {
  }
  {
  probe = stream->ctrl;
  probe.dwFrameInterval = uvc_try_frame_interval(stream->cur_frame, interval);
  ret = uvc_probe_video(stream, & probe);
  }
  if (ret < 0) {
    {
    mutex_unlock(& stream->mutex);
    }
    return (ret);
  } else {
  }
  {
  stream->ctrl = probe;
  mutex_unlock(& stream->mutex);
  timeperframe.numerator = probe.dwFrameInterval;
  timeperframe.denominator = 10000000U;
  uvc_simplify_fraction(& timeperframe.numerator, & timeperframe.denominator, 8U,
                        333U);
  }
  if (parm->type == 1U) {
    parm->parm.capture.timeperframe = timeperframe;
  } else {
    parm->parm.output.timeperframe = timeperframe;
  }
  return (0);
}
}
static int uvc_acquire_privileges(struct uvc_fh *handle )
{
  int tmp ;
  {
  if ((unsigned int )handle->state == 1U) {
    return (0);
  } else {
  }
  {
  tmp = atomic_add_return(1, & (handle->stream)->active);
  }
  if (tmp != 1) {
    {
    atomic_dec(& (handle->stream)->active);
    }
    return (-16);
  } else {
  }
  handle->state = 1;
  return (0);
}
}
static void uvc_dismiss_privileges(struct uvc_fh *handle )
{
  {
  if ((unsigned int )handle->state == 1U) {
    {
    atomic_dec(& (handle->stream)->active);
    }
  } else {
  }
  handle->state = 0;
  return;
}
}
static int uvc_has_privileges(struct uvc_fh *handle )
{
  {
  return ((unsigned int )handle->state == 1U);
}
}
static int uvc_v4l2_open(struct file *file )
{
  struct uvc_streaming *stream ;
  struct uvc_fh *handle ;
  int ret ;
  void *tmp ;
  void *tmp___0 ;
  {
  ret = 0;
  if ((uvc_trace_param & 32U) != 0U) {
    {
    printk("\017uvcvideo: uvc_v4l2_open\n");
    }
  } else {
  }
  {
  tmp = video_drvdata(file);
  stream = (struct uvc_streaming *)tmp;
  }
  if ((int )(stream->dev)->state & 1) {
    return (-19);
  } else {
  }
  {
  ret = usb_autopm_get_interface((stream->dev)->intf);
  }
  if (ret < 0) {
    return (ret);
  } else {
  }
  {
  tmp___0 = kzalloc(200UL, 208U);
  handle = (struct uvc_fh *)tmp___0;
  }
  if ((unsigned long )handle == (unsigned long )((struct uvc_fh *)0)) {
    {
    usb_autopm_put_interface((stream->dev)->intf);
    }
    return (-12);
  } else {
  }
  {
  mutex_lock_nested(& (stream->dev)->lock, 0U);
  }
  if ((stream->dev)->users == 0U) {
    {
    ret = uvc_status_start(stream->dev, 208U);
    }
    if (ret < 0) {
      {
      mutex_unlock(& (stream->dev)->lock);
      usb_autopm_put_interface((stream->dev)->intf);
      kfree((void const *)handle);
      }
      return (ret);
    } else {
    }
  } else {
  }
  {
  (stream->dev)->users = (stream->dev)->users + 1U;
  mutex_unlock(& (stream->dev)->lock);
  v4l2_fh_init(& handle->vfh, stream->vdev);
  v4l2_fh_add(& handle->vfh);
  handle->chain = stream->chain;
  handle->stream = stream;
  handle->state = 0;
  file->private_data = (void *)handle;
  }
  return (0);
}
}
static int uvc_v4l2_release(struct file *file )
{
  struct uvc_fh *handle ;
  struct uvc_streaming *stream ;
  int tmp ;
  {
  handle = (struct uvc_fh *)file->private_data;
  stream = handle->stream;
  if ((uvc_trace_param & 32U) != 0U) {
    {
    printk("\017uvcvideo: uvc_v4l2_release\n");
    }
  } else {
  }
  {
  tmp = uvc_has_privileges(handle);
  }
  if (tmp != 0) {
    {
    uvc_video_enable(stream, 0);
    uvc_free_buffers(& stream->queue);
    }
  } else {
  }
  {
  uvc_dismiss_privileges(handle);
  v4l2_fh_del(& handle->vfh);
  v4l2_fh_exit(& handle->vfh);
  kfree((void const *)handle);
  file->private_data = (void *)0;
  mutex_lock_nested(& (stream->dev)->lock, 0U);
  (stream->dev)->users = (stream->dev)->users - 1U;
  }
  if ((stream->dev)->users == 0U) {
    {
    uvc_status_stop(stream->dev);
    }
  } else {
  }
  {
  mutex_unlock(& (stream->dev)->lock);
  usb_autopm_put_interface((stream->dev)->intf);
  }
  return (0);
}
}
static long uvc_v4l2_do_ioctl(struct file *file , unsigned int cmd , void *arg )
{
  struct video_device *vdev ;
  struct video_device *tmp ;
  struct uvc_fh *handle ;
  struct uvc_video_chain *chain ;
  struct uvc_streaming *stream ;
  long ret ;
  struct v4l2_capability *cap ;
  enum v4l2_priority tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  struct v4l2_control *ctrl ;
  struct v4l2_ext_control xctrl ;
  int tmp___4 ;
  int tmp___5 ;
  struct v4l2_control *ctrl___0 ;
  struct v4l2_ext_control xctrl___0 ;
  int tmp___6 ;
  int tmp___7 ;
  int tmp___8 ;
  int tmp___9 ;
  int tmp___10 ;
  struct v4l2_ext_controls *ctrls ;
  struct v4l2_ext_control *ctrl___1 ;
  unsigned int i ;
  int tmp___11 ;
  int tmp___12 ;
  int tmp___13 ;
  int tmp___14 ;
  struct v4l2_ext_controls *ctrls___0 ;
  struct v4l2_ext_control *ctrl___2 ;
  unsigned int i___0 ;
  int tmp___15 ;
  int tmp___16 ;
  int tmp___17 ;
  int tmp___18 ;
  struct uvc_entity const *selector ;
  struct v4l2_input *input ;
  struct uvc_entity *iterm ;
  u32 index ;
  int pin ;
  struct list_head const *__mptr ;
  struct list_head const *__mptr___0 ;
  struct list_head const *__mptr___1 ;
  struct list_head const *__mptr___2 ;
  u8 input___0 ;
  int tmp___19 ;
  u32 input___1 ;
  int tmp___20 ;
  int tmp___21 ;
  int tmp___22 ;
  struct v4l2_fmtdesc *fmt ;
  struct uvc_format *format ;
  enum v4l2_buf_type type ;
  __u32 index___0 ;
  struct uvc_streaming_control probe ;
  int tmp___23 ;
  int tmp___24 ;
  int tmp___25 ;
  int tmp___26 ;
  int tmp___27 ;
  struct v4l2_frmsizeenum *fsize ;
  struct uvc_format *format___0 ;
  struct uvc_frame *frame ;
  int i___1 ;
  struct v4l2_frmivalenum *fival ;
  struct uvc_format *format___1 ;
  struct uvc_frame *frame___0 ;
  int i___2 ;
  int tmp___28 ;
  int tmp___29 ;
  int tmp___30 ;
  int tmp___31 ;
  struct v4l2_cropcap *ccap ;
  int tmp___32 ;
  int tmp___33 ;
  int tmp___34 ;
  struct v4l2_buffer *buf ;
  int tmp___35 ;
  int tmp___36 ;
  int tmp___37 ;
  int tmp___38 ;
  int tmp___39 ;
  int tmp___40 ;
  int *type___0 ;
  int tmp___41 ;
  int tmp___42 ;
  int tmp___43 ;
  int *type___1 ;
  int tmp___44 ;
  int tmp___45 ;
  int tmp___46 ;
  struct v4l2_event_subscription *sub ;
  int tmp___47 ;
  int tmp___48 ;
  int tmp___49 ;
  int tmp___50 ;
  int tmp___51 ;
  {
  {
  tmp = video_devdata(file);
  vdev = tmp;
  handle = (struct uvc_fh *)file->private_data;
  chain = handle->chain;
  stream = handle->stream;
  ret = 0L;
  }
  {
  if (cmd == 2154321408U) {
    goto case_2154321408;
  } else {
  }
  if (cmd == 2147767875U) {
    goto case_2147767875;
  } else {
  }
  if (cmd == 1074026052U) {
    goto case_1074026052;
  } else {
  }
  if (cmd == 3225703972U) {
    goto case_3225703972;
  } else {
  }
  if (cmd == 3221771803U) {
    goto case_3221771803;
  } else {
  }
  if (cmd == 3221771804U) {
    goto case_3221771804;
  } else {
  }
  if (cmd == 3224131109U) {
    goto case_3224131109;
  } else {
  }
  if (cmd == 3223344711U) {
    goto case_3223344711;
  } else {
  }
  if (cmd == 3223344712U) {
    goto case_3223344712;
  } else {
  }
  if (cmd == 3223344713U) {
    goto case_3223344713;
  } else {
  }
  if (cmd == 3226490394U) {
    goto case_3226490394;
  } else {
  }
  if (cmd == 2147767846U) {
    goto case_2147767846;
  } else {
  }
  if (cmd == 3221509671U) {
    goto case_3221509671;
  } else {
  }
  if (cmd == 3225441794U) {
    goto case_3225441794;
  } else {
  }
  if (cmd == 3234879040U) {
    goto case_3234879040;
  } else {
  }
  if (cmd == 3234878981U) {
    goto case_3234878981;
  } else {
  }
  if (cmd == 3234878980U) {
    goto case_3234878980;
  } else {
  }
  if (cmd == 3224131146U) {
    goto case_3224131146;
  } else {
  }
  if (cmd == 3224655435U) {
    goto case_3224655435;
  } else {
  }
  if (cmd == 3234616853U) {
    goto case_3234616853;
  } else {
  }
  if (cmd == 3234616854U) {
    goto case_3234616854;
  } else {
  }
  if (cmd == 3224131130U) {
    goto case_3224131130;
  } else {
  }
  if (cmd == 3222558267U) {
    goto case_3222558267;
  } else {
  }
  if (cmd == 1075074620U) {
    goto case_1075074620;
  } else {
  }
  if (cmd == 3222558216U) {
    goto case_3222558216;
  } else {
  }
  if (cmd == 3227014665U) {
    goto case_3227014665;
  } else {
  }
  if (cmd == 3227014671U) {
    goto case_3227014671;
  } else {
  }
  if (cmd == 3227014673U) {
    goto case_3227014673;
  } else {
  }
  if (cmd == 1074026002U) {
    goto case_1074026002;
  } else {
  }
  if (cmd == 1074026003U) {
    goto case_1074026003;
  } else {
  }
  if (cmd == 1075861082U) {
    goto case_1075861082;
  } else {
  }
  if (cmd == 1075861083U) {
    goto case_1075861083;
  } else {
  }
  if (cmd == 2156418649U) {
    goto case_2156418649;
  } else {
  }
  if (cmd == 3225966105U) {
    goto case_3225966105;
  } else {
  }
  if (cmd == 2148030015U) {
    goto case_2148030015;
  } else {
  }
  if (cmd == 2148029975U) {
    goto case_2148029975;
  } else {
  }
  if (cmd == 1074288152U) {
    goto case_1074288152;
  } else {
  }
  if (cmd == 1074025998U) {
    goto case_1074025998;
  } else {
  }
  if (cmd == 3224655425U) {
    goto case_3224655425;
  } else {
  }
  if (cmd == 3224655426U) {
    goto case_3224655426;
  } else {
  }
  if (cmd == 3225966128U) {
    goto case_3225966128;
  } else {
  }
  if (cmd == 3227546912U) {
    goto case_3227546912;
  } else {
  }
  if (cmd == 3222304033U) {
    goto case_3222304033;
  } else {
  }
  goto switch_default___0;
  case_2154321408:
  {
  cap = (struct v4l2_capability *)arg;
  memset((void *)cap, 0, 104UL);
  strlcpy((char *)(& cap->driver), "uvcvideo", 16UL);
  strlcpy((char *)(& cap->card), (char const *)(& vdev->name), 32UL);
  usb_make_path((stream->dev)->udev, (char *)(& cap->bus_info), 32UL);
  cap->version = 200192U;
  cap->capabilities = chain->caps | 2214592512U;
  }
  if ((unsigned int )stream->type == 1U) {
    cap->device_caps = 67108865U;
  } else {
    cap->device_caps = 67108866U;
  }
  goto ldv_36675;
  case_2147767875:
  {
  tmp___0 = v4l2_prio_max(vdev->prio);
  *((u32 *)arg) = (u32 )tmp___0;
  }
  goto ldv_36675;
  case_1074026052:
  {
  tmp___1 = v4l2_prio_check(vdev->prio, handle->vfh.prio);
  ret = (long )tmp___1;
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  {
  tmp___2 = v4l2_prio_change(vdev->prio, & handle->vfh.prio, (enum v4l2_priority )*((u32 *)arg));
  }
  return ((long )tmp___2);
  case_3225703972:
  {
  tmp___3 = uvc_query_v4l2_ctrl(chain, (struct v4l2_queryctrl *)arg);
  }
  return ((long )tmp___3);
  case_3221771803:
  {
  ctrl = (struct v4l2_control *)arg;
  memset((void *)(& xctrl), 0, 20UL);
  xctrl.id = ctrl->id;
  tmp___4 = uvc_ctrl_begin(chain);
  ret = (long )tmp___4;
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  {
  tmp___5 = uvc_ctrl_get(chain, & xctrl);
  ret = (long )tmp___5;
  uvc_ctrl_rollback(handle);
  }
  if (ret >= 0L) {
    ctrl->value = xctrl.__annonCompField72.value;
  } else {
  }
  goto ldv_36675;
  case_3221771804:
  {
  ctrl___0 = (struct v4l2_control *)arg;
  tmp___6 = v4l2_prio_check(vdev->prio, handle->vfh.prio);
  ret = (long )tmp___6;
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  {
  memset((void *)(& xctrl___0), 0, 20UL);
  xctrl___0.id = ctrl___0->id;
  xctrl___0.__annonCompField72.value = ctrl___0->value;
  tmp___7 = uvc_ctrl_begin(chain);
  ret = (long )tmp___7;
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  {
  tmp___8 = uvc_ctrl_set(chain, & xctrl___0);
  ret = (long )tmp___8;
  }
  if (ret < 0L) {
    {
    uvc_ctrl_rollback(handle);
    }
    return (ret);
  } else {
  }
  {
  tmp___9 = uvc_ctrl_commit(handle, (struct v4l2_ext_control const *)(& xctrl___0),
                            1U);
  ret = (long )tmp___9;
  }
  if (ret == 0L) {
    ctrl___0->value = xctrl___0.__annonCompField72.value;
  } else {
  }
  goto ldv_36675;
  case_3224131109:
  {
  tmp___10 = uvc_query_v4l2_menu(chain, (struct v4l2_querymenu *)arg);
  }
  return ((long )tmp___10);
  case_3223344711:
  {
  ctrls = (struct v4l2_ext_controls *)arg;
  ctrl___1 = ctrls->controls;
  tmp___11 = uvc_ctrl_begin(chain);
  ret = (long )tmp___11;
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  i = 0U;
  goto ldv_36691;
  ldv_36690:
  {
  tmp___12 = uvc_ctrl_get(chain, ctrl___1);
  ret = (long )tmp___12;
  }
  if (ret < 0L) {
    {
    uvc_ctrl_rollback(handle);
    ctrls->error_idx = i;
    }
    return (ret);
  } else {
  }
  ctrl___1 = ctrl___1 + 1;
  i = i + 1U;
  ldv_36691: ;
  if (i < ctrls->count) {
    goto ldv_36690;
  } else {
  }
  {
  ctrls->error_idx = 0U;
  tmp___13 = uvc_ctrl_rollback(handle);
  ret = (long )tmp___13;
  }
  goto ldv_36675;
  case_3223344712:
  {
  tmp___14 = v4l2_prio_check(vdev->prio, handle->vfh.prio);
  ret = (long )tmp___14;
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  case_3223344713:
  {
  ctrls___0 = (struct v4l2_ext_controls *)arg;
  ctrl___2 = ctrls___0->controls;
  tmp___15 = uvc_ctrl_begin(chain);
  ret = (long )tmp___15;
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  i___0 = 0U;
  goto ldv_36699;
  ldv_36698:
  {
  tmp___16 = uvc_ctrl_set(chain, ctrl___2);
  ret = (long )tmp___16;
  }
  if (ret < 0L) {
    {
    uvc_ctrl_rollback(handle);
    ctrls___0->error_idx = cmd == 3223344712U ? ctrls___0->count : i___0;
    }
    return (ret);
  } else {
  }
  ctrl___2 = ctrl___2 + 1;
  i___0 = i___0 + 1U;
  ldv_36699: ;
  if (i___0 < ctrls___0->count) {
    goto ldv_36698;
  } else {
  }
  ctrls___0->error_idx = 0U;
  if (cmd == 3223344712U) {
    {
    tmp___17 = uvc_ctrl_commit(handle, (struct v4l2_ext_control const *)ctrls___0->controls,
                               ctrls___0->count);
    ret = (long )tmp___17;
    }
  } else {
    {
    tmp___18 = uvc_ctrl_rollback(handle);
    ret = (long )tmp___18;
    }
  }
  goto ldv_36675;
  case_3226490394:
  selector = (struct uvc_entity const *)chain->selector;
  input = (struct v4l2_input *)arg;
  iterm = (struct uvc_entity *)0;
  index = input->index;
  pin = 0;
  if ((unsigned long )selector == (unsigned long )((struct uvc_entity const *)0) || ((chain->dev)->quirks & 32U) != 0U) {
    if (index != 0U) {
      return (-22L);
    } else {
    }
    __mptr = (struct list_head const *)chain->entities.next;
    iterm = (struct uvc_entity *)__mptr + 0xfffffffffffffff0UL;
    goto ldv_36713;
    ldv_36712: ;
    if (((int )iterm->type & 65280) != 0 && (int )((short )iterm->type) >= 0) {
      goto ldv_36711;
    } else {
    }
    __mptr___0 = (struct list_head const *)iterm->chain.next;
    iterm = (struct uvc_entity *)__mptr___0 + 0xfffffffffffffff0UL;
    ldv_36713: ;
    if ((unsigned long )(& iterm->chain) != (unsigned long )(& chain->entities)) {
      goto ldv_36712;
    } else {
    }
    ldv_36711:
    pin = (int )iterm->id;
  } else
  if (index < (u32 )selector->bNrInPins) {
    pin = (int )*(selector->baSourceID + (unsigned long )index);
    __mptr___1 = (struct list_head const *)chain->entities.next;
    iterm = (struct uvc_entity *)__mptr___1 + 0xfffffffffffffff0UL;
    goto ldv_36721;
    ldv_36720: ;
    if (((int )iterm->type & 65280) == 0 || (int )((short )iterm->type) < 0) {
      goto ldv_36718;
    } else {
    }
    if ((int )iterm->id == pin) {
      goto ldv_36719;
    } else {
    }
    ldv_36718:
    __mptr___2 = (struct list_head const *)iterm->chain.next;
    iterm = (struct uvc_entity *)__mptr___2 + 0xfffffffffffffff0UL;
    ldv_36721: ;
    if ((unsigned long )(& iterm->chain) != (unsigned long )(& chain->entities)) {
      goto ldv_36720;
    } else {
    }
    ldv_36719: ;
  } else {
  }
  if ((unsigned long )iterm == (unsigned long )((struct uvc_entity *)0) || (int )iterm->id != pin) {
    return (-22L);
  } else {
  }
  {
  memset((void *)input, 0, 80UL);
  input->index = index;
  strlcpy((char *)(& input->name), (char const *)(& iterm->name), 32UL);
  }
  if (((int )iterm->type & 32767) == 513) {
    input->type = 2U;
  } else {
  }
  goto ldv_36675;
  case_2147767846: ;
  if ((unsigned long )chain->selector == (unsigned long )((struct uvc_entity *)0) || ((chain->dev)->quirks & 32U) != 0U) {
    *((int *)arg) = 0;
    goto ldv_36675;
  } else {
  }
  {
  tmp___19 = uvc_query_ctrl(chain->dev, 129, (int )(chain->selector)->id, (int )((__u8 )(chain->dev)->intfnum),
                            1, (void *)(& input___0), 1);
  ret = (long )tmp___19;
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  *((int *)arg) = (int )input___0 + -1;
  goto ldv_36675;
  case_3221509671:
  {
  input___1 = *((u32 *)arg) + 1U;
  tmp___20 = v4l2_prio_check(vdev->prio, handle->vfh.prio);
  ret = (long )tmp___20;
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  {
  tmp___21 = uvc_acquire_privileges(handle);
  ret = (long )tmp___21;
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  if ((unsigned long )chain->selector == (unsigned long )((struct uvc_entity *)0) || ((chain->dev)->quirks & 32U) != 0U) {
    if (input___1 != 1U) {
      return (-22L);
    } else {
    }
    goto ldv_36675;
  } else {
  }
  if (input___1 == 0U || input___1 > (u32 )(chain->selector)->bNrInPins) {
    return (-22L);
  } else {
  }
  {
  tmp___22 = uvc_query_ctrl(chain->dev, 1, (int )(chain->selector)->id, (int )((__u8 )(chain->dev)->intfnum),
                            1, (void *)(& input___1), 1);
  }
  return ((long )tmp___22);
  case_3225441794:
  fmt = (struct v4l2_fmtdesc *)arg;
  type = fmt->type;
  index___0 = fmt->index;
  if (fmt->type != (__u32 )stream->type || fmt->index >= stream->nformats) {
    return (-22L);
  } else {
  }
  {
  memset((void *)fmt, 0, 64UL);
  fmt->index = index___0;
  fmt->type = (__u32 )type;
  format = stream->format + (unsigned long )fmt->index;
  fmt->flags = 0U;
  }
  if ((int )format->flags & 1) {
    fmt->flags = fmt->flags | 1U;
  } else {
  }
  {
  strlcpy((char *)(& fmt->description), (char const *)(& format->name), 32UL);
  fmt->description[31UL] = 0U;
  fmt->pixelformat = format->fcc;
  }
  goto ldv_36675;
  case_3234879040:
  {
  tmp___23 = uvc_v4l2_try_format(stream, (struct v4l2_format *)arg, & probe, (struct uvc_format **)0,
                                 (struct uvc_frame **)0);
  }
  return ((long )tmp___23);
  case_3234878981:
  {
  tmp___24 = v4l2_prio_check(vdev->prio, handle->vfh.prio);
  ret = (long )tmp___24;
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  {
  tmp___25 = uvc_acquire_privileges(handle);
  ret = (long )tmp___25;
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  {
  tmp___26 = uvc_v4l2_set_format(stream, (struct v4l2_format *)arg);
  }
  return ((long )tmp___26);
  case_3234878980:
  {
  tmp___27 = uvc_v4l2_get_format(stream, (struct v4l2_format *)arg);
  }
  return ((long )tmp___27);
  case_3224131146:
  fsize = (struct v4l2_frmsizeenum *)arg;
  format___0 = (struct uvc_format *)0;
  i___1 = 0;
  goto ldv_36742;
  ldv_36741: ;
  if ((stream->format + (unsigned long )i___1)->fcc == fsize->pixel_format) {
    format___0 = stream->format + (unsigned long )i___1;
    goto ldv_36740;
  } else {
  }
  i___1 = i___1 + 1;
  ldv_36742: ;
  if ((unsigned int )i___1 < stream->nformats) {
    goto ldv_36741;
  } else {
  }
  ldv_36740: ;
  if ((unsigned long )format___0 == (unsigned long )((struct uvc_format *)0)) {
    return (-22L);
  } else {
  }
  if (fsize->index >= format___0->nframes) {
    return (-22L);
  } else {
  }
  frame = format___0->frame + (unsigned long )fsize->index;
  fsize->type = 1U;
  fsize->__annonCompField68.discrete.width = (__u32 )frame->wWidth;
  fsize->__annonCompField68.discrete.height = (__u32 )frame->wHeight;
  goto ldv_36675;
  case_3224655435:
  fival = (struct v4l2_frmivalenum *)arg;
  format___1 = (struct uvc_format *)0;
  frame___0 = (struct uvc_frame *)0;
  i___2 = 0;
  goto ldv_36750;
  ldv_36749: ;
  if ((stream->format + (unsigned long )i___2)->fcc == fival->pixel_format) {
    format___1 = stream->format + (unsigned long )i___2;
    goto ldv_36748;
  } else {
  }
  i___2 = i___2 + 1;
  ldv_36750: ;
  if ((unsigned int )i___2 < stream->nformats) {
    goto ldv_36749;
  } else {
  }
  ldv_36748: ;
  if ((unsigned long )format___1 == (unsigned long )((struct uvc_format *)0)) {
    return (-22L);
  } else {
  }
  i___2 = 0;
  goto ldv_36753;
  ldv_36752: ;
  if ((__u32 )(format___1->frame + (unsigned long )i___2)->wWidth == fival->width && (__u32 )(format___1->frame + (unsigned long )i___2)->wHeight == fival->height) {
    frame___0 = format___1->frame + (unsigned long )i___2;
    goto ldv_36751;
  } else {
  }
  i___2 = i___2 + 1;
  ldv_36753: ;
  if ((unsigned int )i___2 < format___1->nframes) {
    goto ldv_36752;
  } else {
  }
  ldv_36751: ;
  if ((unsigned long )frame___0 == (unsigned long )((struct uvc_frame *)0)) {
    return (-22L);
  } else {
  }
  if ((unsigned int )frame___0->bFrameIntervalType != 0U) {
    if (fival->index >= (__u32 )frame___0->bFrameIntervalType) {
      return (-22L);
    } else {
    }
    {
    fival->type = 1U;
    fival->__annonCompField69.discrete.numerator = *(frame___0->dwFrameInterval + (unsigned long )fival->index);
    fival->__annonCompField69.discrete.denominator = 10000000U;
    uvc_simplify_fraction(& fival->__annonCompField69.discrete.numerator, & fival->__annonCompField69.discrete.denominator,
                          8U, 333U);
    }
  } else {
    {
    fival->type = 3U;
    fival->__annonCompField69.stepwise.min.numerator = *(frame___0->dwFrameInterval);
    fival->__annonCompField69.stepwise.min.denominator = 10000000U;
    fival->__annonCompField69.stepwise.max.numerator = *(frame___0->dwFrameInterval + 1UL);
    fival->__annonCompField69.stepwise.max.denominator = 10000000U;
    fival->__annonCompField69.stepwise.step.numerator = *(frame___0->dwFrameInterval + 2UL);
    fival->__annonCompField69.stepwise.step.denominator = 10000000U;
    uvc_simplify_fraction(& fival->__annonCompField69.stepwise.min.numerator, & fival->__annonCompField69.stepwise.min.denominator,
                          8U, 333U);
    uvc_simplify_fraction(& fival->__annonCompField69.stepwise.max.numerator, & fival->__annonCompField69.stepwise.max.denominator,
                          8U, 333U);
    uvc_simplify_fraction(& fival->__annonCompField69.stepwise.step.numerator, & fival->__annonCompField69.stepwise.step.denominator,
                          8U, 333U);
    }
  }
  goto ldv_36675;
  case_3234616853:
  {
  tmp___28 = uvc_v4l2_get_streamparm(stream, (struct v4l2_streamparm *)arg);
  }
  return ((long )tmp___28);
  case_3234616854:
  {
  tmp___29 = v4l2_prio_check(vdev->prio, handle->vfh.prio);
  ret = (long )tmp___29;
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  {
  tmp___30 = uvc_acquire_privileges(handle);
  ret = (long )tmp___30;
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  {
  tmp___31 = uvc_v4l2_set_streamparm(stream, (struct v4l2_streamparm *)arg);
  }
  return ((long )tmp___31);
  case_3224131130:
  ccap = (struct v4l2_cropcap *)arg;
  if (ccap->type != (__u32 )stream->type) {
    return (-22L);
  } else {
  }
  {
  ccap->bounds.left = 0;
  ccap->bounds.top = 0;
  mutex_lock_nested(& stream->mutex, 0U);
  ccap->bounds.width = (__u32 )(stream->cur_frame)->wWidth;
  ccap->bounds.height = (__u32 )(stream->cur_frame)->wHeight;
  mutex_unlock(& stream->mutex);
  ccap->defrect = ccap->bounds;
  ccap->pixelaspect.numerator = 1U;
  ccap->pixelaspect.denominator = 1U;
  }
  goto ldv_36675;
  case_3222558267: ;
  case_1075074620: ;
  return (-25L);
  case_3222558216:
  {
  tmp___32 = v4l2_prio_check(vdev->prio, handle->vfh.prio);
  ret = (long )tmp___32;
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  {
  tmp___33 = uvc_acquire_privileges(handle);
  ret = (long )tmp___33;
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  {
  mutex_lock_nested(& stream->mutex, 0U);
  tmp___34 = uvc_alloc_buffers(& stream->queue, (struct v4l2_requestbuffers *)arg);
  ret = (long )tmp___34;
  mutex_unlock(& stream->mutex);
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  if (ret == 0L) {
    {
    uvc_dismiss_privileges(handle);
    }
  } else {
  }
  ret = 0L;
  goto ldv_36675;
  case_3227014665:
  {
  buf = (struct v4l2_buffer *)arg;
  tmp___35 = uvc_has_privileges(handle);
  }
  if (tmp___35 == 0) {
    return (-16L);
  } else {
  }
  {
  tmp___36 = uvc_query_buffer(& stream->queue, buf);
  }
  return ((long )tmp___36);
  case_3227014671:
  {
  tmp___37 = uvc_has_privileges(handle);
  }
  if (tmp___37 == 0) {
    return (-16L);
  } else {
  }
  {
  tmp___38 = uvc_queue_buffer(& stream->queue, (struct v4l2_buffer *)arg);
  }
  return ((long )tmp___38);
  case_3227014673:
  {
  tmp___39 = uvc_has_privileges(handle);
  }
  if (tmp___39 == 0) {
    return (-16L);
  } else {
  }
  {
  tmp___40 = uvc_dequeue_buffer(& stream->queue, (struct v4l2_buffer *)arg, (int )file->f_flags & 2048);
  }
  return ((long )tmp___40);
  case_1074026002:
  type___0 = (int *)arg;
  if ((unsigned int )*type___0 != (unsigned int )stream->type) {
    return (-22L);
  } else {
  }
  {
  tmp___41 = v4l2_prio_check(vdev->prio, handle->vfh.prio);
  ret = (long )tmp___41;
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  {
  tmp___42 = uvc_has_privileges(handle);
  }
  if (tmp___42 == 0) {
    return (-16L);
  } else {
  }
  {
  mutex_lock_nested(& stream->mutex, 0U);
  tmp___43 = uvc_video_enable(stream, 1);
  ret = (long )tmp___43;
  mutex_unlock(& stream->mutex);
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  goto ldv_36675;
  case_1074026003:
  type___1 = (int *)arg;
  if ((unsigned int )*type___1 != (unsigned int )stream->type) {
    return (-22L);
  } else {
  }
  {
  tmp___44 = v4l2_prio_check(vdev->prio, handle->vfh.prio);
  ret = (long )tmp___44;
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  {
  tmp___45 = uvc_has_privileges(handle);
  }
  if (tmp___45 == 0) {
    return (-16L);
  } else {
  }
  {
  tmp___46 = uvc_video_enable(stream, 0);
  }
  return ((long )tmp___46);
  case_1075861082:
  sub = (struct v4l2_event_subscription *)arg;
  {
  if (sub->type == 3U) {
    goto case_3;
  } else {
  }
  goto switch_default;
  case_3:
  {
  tmp___47 = v4l2_event_subscribe(& handle->vfh, (struct v4l2_event_subscription const *)sub,
                                  0U, & uvc_ctrl_sub_ev_ops);
  }
  return ((long )tmp___47);
  switch_default: ;
  return (-22L);
  switch_break___0: ;
  }
  case_1075861083:
  {
  tmp___48 = v4l2_event_unsubscribe(& handle->vfh, (struct v4l2_event_subscription const *)arg);
  }
  return ((long )tmp___48);
  case_2156418649:
  {
  tmp___49 = v4l2_event_dequeue(& handle->vfh, (struct v4l2_event *)arg, (int )file->f_flags & 2048);
  }
  return ((long )tmp___49);
  case_3225966105: ;
  case_2148030015: ;
  case_2148029975: ;
  case_1074288152: ;
  case_1074025998: ;
  case_3224655425: ;
  case_3224655426: ;
  case_3225966128: ;
  if ((uvc_trace_param & 64U) != 0U) {
    {
    printk("\017uvcvideo: Unsupported ioctl 0x%08x\n", cmd);
    }
  } else {
  }
  return (-25L);
  case_3227546912:
  {
  tmp___50 = uvc_ioctl_ctrl_map(chain, (struct uvc_xu_control_mapping *)arg);
  }
  return ((long )tmp___50);
  case_3222304033:
  {
  tmp___51 = uvc_xu_ctrl_query(chain, (struct uvc_xu_control_query *)arg);
  }
  return ((long )tmp___51);
  switch_default___0: ;
  if ((uvc_trace_param & 64U) != 0U) {
    {
    printk("\017uvcvideo: Unknown ioctl 0x%08x\n", cmd);
    }
  } else {
  }
  return (-25L);
  switch_break: ;
  }
  ldv_36675: ;
  return (ret);
}
}
static long uvc_v4l2_ioctl(struct file *file , unsigned int cmd , unsigned long arg )
{
  long tmp ;
  {
  if ((uvc_trace_param & 64U) != 0U) {
    {
    printk("\017uvcvideo: uvc_v4l2_ioctl(");
    v4l_printk_ioctl((char const *)0, cmd);
    printk(")\n");
    }
  } else {
  }
  {
  tmp = video_usercopy(file, cmd, arg, & uvc_v4l2_do_ioctl);
  }
  return (tmp);
}
}
static int uvc_v4l2_get_xu_mapping(struct uvc_xu_control_mapping *kp , struct uvc_xu_control_mapping32 const *up___0 )
{
  struct uvc_menu_info *umenus ;
  struct uvc_menu_info *kmenus ;
  compat_caddr_t p ;
  struct thread_info *tmp ;
  bool tmp___0 ;
  int tmp___1 ;
  long tmp___2 ;
  int tmp___3 ;
  int __gu_err ;
  unsigned long __gu_val ;
  int tmp___4 ;
  int __gu_err___0 ;
  unsigned long __gu_val___0 ;
  int tmp___5 ;
  void *tmp___6 ;
  struct thread_info *tmp___7 ;
  bool tmp___8 ;
  int tmp___9 ;
  long tmp___10 ;
  void *tmp___11 ;
  unsigned long tmp___12 ;
  {
  {
  tmp = current_thread_info();
  tmp___0 = __chk_range_not_ok((unsigned long )up___0, 88UL, tmp->addr_limit.seg);
  }
  if (tmp___0) {
    tmp___1 = 0;
  } else {
    tmp___1 = 1;
  }
  {
  tmp___2 = ldv__builtin_expect((long )tmp___1, 1L);
  }
  if (tmp___2 == 0L) {
    return (-14);
  } else {
    {
    tmp___3 = __copy_from_user((void *)kp, (void const *)up___0, 64U);
    }
    if (tmp___3 != 0) {
      return (-14);
    } else {
      __gu_err = 0;
      {
      if (4UL == 1UL) {
        goto case_1;
      } else {
      }
      if (4UL == 2UL) {
        goto case_2;
      } else {
      }
      if (4UL == 4UL) {
        goto case_4;
      } else {
      }
      if (4UL == 8UL) {
        goto case_8;
      } else {
      }
      goto switch_default;
      case_1:
      __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovb %2,%b1\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\txorb %b1,%b1\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (__gu_err),
                           "=q" (__gu_val): "m" (*((struct __large_struct *)(& up___0->menu_count))),
                           "i" (-14), "0" (__gu_err));
      goto ldv_36814;
      case_2:
      __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovw %2,%w1\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\txorw %w1,%w1\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (__gu_err),
                           "=r" (__gu_val): "m" (*((struct __large_struct *)(& up___0->menu_count))),
                           "i" (-14), "0" (__gu_err));
      goto ldv_36814;
      case_4:
      __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovl %2,%k1\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\txorl %k1,%k1\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (__gu_err),
                           "=r" (__gu_val): "m" (*((struct __large_struct *)(& up___0->menu_count))),
                           "i" (-14), "0" (__gu_err));
      goto ldv_36814;
      case_8:
      __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovq %2,%1\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\txorq %1,%1\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (__gu_err),
                           "=r" (__gu_val): "m" (*((struct __large_struct *)(& up___0->menu_count))),
                           "i" (-14), "0" (__gu_err));
      goto ldv_36814;
      switch_default:
      {
      tmp___4 = __get_user_bad();
      __gu_val = (unsigned long )tmp___4;
      }
      switch_break: ;
      }
      ldv_36814:
      kp->menu_count = (unsigned int )__gu_val;
      if (__gu_err != 0) {
        return (-14);
      } else {
      }
    }
  }
  {
  memset((void *)(& kp->reserved), 0, 16UL);
  }
  if (kp->menu_count == 0U) {
    kp->menu_info = (struct uvc_menu_info *)0;
    return (0);
  } else {
  }
  __gu_err___0 = 0;
  {
  if (4UL == 1UL) {
    goto case_1___0;
  } else {
  }
  if (4UL == 2UL) {
    goto case_2___0;
  } else {
  }
  if (4UL == 4UL) {
    goto case_4___0;
  } else {
  }
  if (4UL == 8UL) {
    goto case_8___0;
  } else {
  }
  goto switch_default___0;
  case_1___0:
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovb %2,%b1\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\txorb %b1,%b1\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (__gu_err___0),
                       "=q" (__gu_val___0): "m" (*((struct __large_struct *)(& up___0->menu_info))),
                       "i" (-14), "0" (__gu_err___0));
  goto ldv_36823;
  case_2___0:
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovw %2,%w1\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\txorw %w1,%w1\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (__gu_err___0),
                       "=r" (__gu_val___0): "m" (*((struct __large_struct *)(& up___0->menu_info))),
                       "i" (-14), "0" (__gu_err___0));
  goto ldv_36823;
  case_4___0:
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovl %2,%k1\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\txorl %k1,%k1\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (__gu_err___0),
                       "=r" (__gu_val___0): "m" (*((struct __large_struct *)(& up___0->menu_info))),
                       "i" (-14), "0" (__gu_err___0));
  goto ldv_36823;
  case_8___0:
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovq %2,%1\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\txorq %1,%1\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (__gu_err___0),
                       "=r" (__gu_val___0): "m" (*((struct __large_struct *)(& up___0->menu_info))),
                       "i" (-14), "0" (__gu_err___0));
  goto ldv_36823;
  switch_default___0:
  {
  tmp___5 = __get_user_bad();
  __gu_val___0 = (unsigned long )tmp___5;
  }
  switch_break___0: ;
  }
  ldv_36823:
  p = (unsigned int )__gu_val___0;
  if (__gu_err___0 != 0) {
    return (-14);
  } else {
  }
  {
  tmp___6 = compat_ptr(p);
  umenus = (struct uvc_menu_info *)tmp___6;
  tmp___7 = current_thread_info();
  tmp___8 = __chk_range_not_ok((unsigned long )umenus, (unsigned long )kp->menu_count * 36UL,
                               tmp___7->addr_limit.seg);
  }
  if (tmp___8) {
    tmp___9 = 0;
  } else {
    tmp___9 = 1;
  }
  {
  tmp___10 = ldv__builtin_expect((long )tmp___9, 1L);
  }
  if (tmp___10 == 0L) {
    return (-14);
  } else {
  }
  {
  tmp___11 = compat_alloc_user_space((unsigned long )kp->menu_count * 36UL);
  kmenus = (struct uvc_menu_info *)tmp___11;
  }
  if ((unsigned long )kmenus == (unsigned long )((struct uvc_menu_info *)0)) {
    return (-14);
  } else {
  }
  {
  kp->menu_info = kmenus;
  tmp___12 = copy_in_user((void *)kmenus, (void const *)umenus, kp->menu_count * 36U);
  }
  if (tmp___12 != 0UL) {
    return (-14);
  } else {
  }
  return (0);
}
}
static int uvc_v4l2_put_xu_mapping(struct uvc_xu_control_mapping const *kp , struct uvc_xu_control_mapping32 *up___0 )
{
  struct uvc_menu_info *umenus ;
  struct uvc_menu_info *kmenus ;
  compat_caddr_t p ;
  struct thread_info *tmp ;
  bool tmp___0 ;
  int tmp___1 ;
  long tmp___2 ;
  int tmp___3 ;
  int __pu_err ;
  unsigned long tmp___4 ;
  int __ret_gu ;
  register unsigned long __val_gu ;
  void *tmp___5 ;
  unsigned long tmp___6 ;
  {
  {
  kmenus = kp->menu_info;
  tmp = current_thread_info();
  tmp___0 = __chk_range_not_ok((unsigned long )up___0, 88UL, tmp->addr_limit.seg);
  }
  if (tmp___0) {
    tmp___1 = 0;
  } else {
    tmp___1 = 1;
  }
  {
  tmp___2 = ldv__builtin_expect((long )tmp___1, 1L);
  }
  if (tmp___2 == 0L) {
    return (-14);
  } else {
    {
    tmp___3 = __copy_to_user((void *)up___0, (void const *)kp, 64U);
    }
    if (tmp___3 != 0) {
      return (-14);
    } else {
      __pu_err = 0;
      {
      if (4UL == 1UL) {
        goto case_1;
      } else {
      }
      if (4UL == 2UL) {
        goto case_2;
      } else {
      }
      if (4UL == 4UL) {
        goto case_4;
      } else {
      }
      if (4UL == 8UL) {
        goto case_8;
      } else {
      }
      goto switch_default;
      case_1:
      __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovb %b1,%2\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (__pu_err): "iq" (kp->menu_count),
                           "m" (*((struct __large_struct *)(& up___0->menu_count))),
                           "i" (-14), "0" (__pu_err));
      goto ldv_36840;
      case_2:
      __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovw %w1,%2\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (__pu_err): "ir" (kp->menu_count),
                           "m" (*((struct __large_struct *)(& up___0->menu_count))),
                           "i" (-14), "0" (__pu_err));
      goto ldv_36840;
      case_4:
      __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovl %k1,%2\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (__pu_err): "ir" (kp->menu_count),
                           "m" (*((struct __large_struct *)(& up___0->menu_count))),
                           "i" (-14), "0" (__pu_err));
      goto ldv_36840;
      case_8:
      __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovq %1,%2\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (__pu_err): "er" (kp->menu_count),
                           "m" (*((struct __large_struct *)(& up___0->menu_count))),
                           "i" (-14), "0" (__pu_err));
      goto ldv_36840;
      switch_default:
      {
      __put_user_bad();
      }
      switch_break: ;
      }
      ldv_36840: ;
      if (__pu_err != 0) {
        return (-14);
      } else {
      }
    }
  }
  {
  tmp___4 = __clear_user((void *)(& up___0->reserved), 16UL);
  }
  if (tmp___4 != 0UL) {
    return (-14);
  } else {
  }
  if ((unsigned int )kp->menu_count == 0U) {
    return (0);
  } else {
  }
  {
  might_fault();
  __asm__ volatile ("call __get_user_%P3": "=a" (__ret_gu), "=r" (__val_gu): "0" (& up___0->menu_info),
                       "i" (4UL));
  p = (unsigned int )__val_gu;
  }
  if (__ret_gu != 0) {
    return (-14);
  } else {
  }
  {
  tmp___5 = compat_ptr(p);
  umenus = (struct uvc_menu_info *)tmp___5;
  tmp___6 = copy_in_user((void *)umenus, (void const *)kmenus, (unsigned int )kp->menu_count * 36U);
  }
  if (tmp___6 != 0UL) {
    return (-14);
  } else {
  }
  return (0);
}
}
static int uvc_v4l2_get_xu_query(struct uvc_xu_control_query *kp , struct uvc_xu_control_query32 const *up___0 )
{
  u8 *udata ;
  u8 *kdata ;
  compat_caddr_t p ;
  struct thread_info *tmp ;
  bool tmp___0 ;
  int tmp___1 ;
  long tmp___2 ;
  int tmp___3 ;
  int __gu_err ;
  unsigned long __gu_val ;
  int tmp___4 ;
  void *tmp___5 ;
  struct thread_info *tmp___6 ;
  bool tmp___7 ;
  int tmp___8 ;
  long tmp___9 ;
  void *tmp___10 ;
  unsigned long tmp___11 ;
  {
  {
  tmp = current_thread_info();
  tmp___0 = __chk_range_not_ok((unsigned long )up___0, 12UL, tmp->addr_limit.seg);
  }
  if (tmp___0) {
    tmp___1 = 0;
  } else {
    tmp___1 = 1;
  }
  {
  tmp___2 = ldv__builtin_expect((long )tmp___1, 1L);
  }
  if (tmp___2 == 0L) {
    return (-14);
  } else {
    {
    tmp___3 = __copy_from_user((void *)kp, (void const *)up___0, 8U);
    }
    if (tmp___3 != 0) {
      return (-14);
    } else {
    }
  }
  if ((unsigned int )kp->size == 0U) {
    kp->data = (__u8 *)0U;
    return (0);
  } else {
  }
  __gu_err = 0;
  {
  if (4UL == 1UL) {
    goto case_1;
  } else {
  }
  if (4UL == 2UL) {
    goto case_2;
  } else {
  }
  if (4UL == 4UL) {
    goto case_4;
  } else {
  }
  if (4UL == 8UL) {
    goto case_8;
  } else {
  }
  goto switch_default;
  case_1:
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovb %2,%b1\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\txorb %b1,%b1\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (__gu_err),
                       "=q" (__gu_val): "m" (*((struct __large_struct *)(& up___0->data))),
                       "i" (-14), "0" (__gu_err));
  goto ldv_36866;
  case_2:
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovw %2,%w1\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\txorw %w1,%w1\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (__gu_err),
                       "=r" (__gu_val): "m" (*((struct __large_struct *)(& up___0->data))),
                       "i" (-14), "0" (__gu_err));
  goto ldv_36866;
  case_4:
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovl %2,%k1\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\txorl %k1,%k1\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (__gu_err),
                       "=r" (__gu_val): "m" (*((struct __large_struct *)(& up___0->data))),
                       "i" (-14), "0" (__gu_err));
  goto ldv_36866;
  case_8:
  __asm__ volatile ("661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xcb\n6641:\n\t.popsection\n1:\tmovq %2,%1\n2: 661:\n\t.byte 0x66,0x66,0x90\n\n662:\n.pushsection .altinstructions,\"a\"\n .long 661b - .\n .long 6631f - .\n .word (9*32+20)\n .byte 662b-661b\n .byte 6641f-6631f\n.popsection\n.pushsection .discard,\"aw\",@progbits\n .byte 0xff + (6641f-6631f) - (662b-661b)\n.popsection\n.pushsection .altinstr_replacement, \"ax\"\n6631:\n\t.byte 0x0f,0x01,0xca\n6641:\n\t.popsection\n.section .fixup,\"ax\"\n3:\tmov %3,%0\n\txorq %1,%1\n\tjmp 2b\n.previous\n .pushsection \"__ex_table\",\"a\"\n .balign 8\n .long (1b) - .\n .long (3b) - .\n .popsection\n": "=r" (__gu_err),
                       "=r" (__gu_val): "m" (*((struct __large_struct *)(& up___0->data))),
                       "i" (-14), "0" (__gu_err));
  goto ldv_36866;
  switch_default:
  {
  tmp___4 = __get_user_bad();
  __gu_val = (unsigned long )tmp___4;
  }
  switch_break: ;
  }
  ldv_36866:
  p = (unsigned int )__gu_val;
  if (__gu_err != 0) {
    return (-14);
  } else {
  }
  {
  tmp___5 = compat_ptr(p);
  udata = (u8 *)tmp___5;
  tmp___6 = current_thread_info();
  tmp___7 = __chk_range_not_ok((unsigned long )udata, (unsigned long )kp->size, tmp___6->addr_limit.seg);
  }
  if (tmp___7) {
    tmp___8 = 0;
  } else {
    tmp___8 = 1;
  }
  {
  tmp___9 = ldv__builtin_expect((long )tmp___8, 1L);
  }
  if (tmp___9 == 0L) {
    return (-14);
  } else {
  }
  {
  tmp___10 = compat_alloc_user_space((unsigned long )kp->size);
  kdata = (u8 *)tmp___10;
  }
  if ((unsigned long )kdata == (unsigned long )((u8 *)0U)) {
    return (-14);
  } else {
  }
  {
  kp->data = kdata;
  tmp___11 = copy_in_user((void *)kdata, (void const *)udata, (unsigned int )kp->size);
  }
  if (tmp___11 != 0UL) {
    return (-14);
  } else {
  }
  return (0);
}
}
static int uvc_v4l2_put_xu_query(struct uvc_xu_control_query const *kp , struct uvc_xu_control_query32 *up___0 )
{
  u8 *udata ;
  u8 *kdata ;
  compat_caddr_t p ;
  struct thread_info *tmp ;
  bool tmp___0 ;
  int tmp___1 ;
  long tmp___2 ;
  int tmp___3 ;
  int __ret_gu ;
  register unsigned long __val_gu ;
  void *tmp___4 ;
  struct thread_info *tmp___5 ;
  bool tmp___6 ;
  int tmp___7 ;
  long tmp___8 ;
  unsigned long tmp___9 ;
  {
  {
  kdata = (u8 *)kp->data;
  tmp = current_thread_info();
  tmp___0 = __chk_range_not_ok((unsigned long )up___0, 12UL, tmp->addr_limit.seg);
  }
  if (tmp___0) {
    tmp___1 = 0;
  } else {
    tmp___1 = 1;
  }
  {
  tmp___2 = ldv__builtin_expect((long )tmp___1, 1L);
  }
  if (tmp___2 == 0L) {
    return (-14);
  } else {
    {
    tmp___3 = __copy_to_user((void *)up___0, (void const *)kp, 8U);
    }
    if (tmp___3 != 0) {
      return (-14);
    } else {
    }
  }
  if ((unsigned int )((unsigned short )kp->size) == 0U) {
    return (0);
  } else {
  }
  {
  might_fault();
  __asm__ volatile ("call __get_user_%P3": "=a" (__ret_gu), "=r" (__val_gu): "0" (& up___0->data),
                       "i" (4UL));
  p = (unsigned int )__val_gu;
  }
  if (__ret_gu != 0) {
    return (-14);
  } else {
  }
  {
  tmp___4 = compat_ptr(p);
  udata = (u8 *)tmp___4;
  tmp___5 = current_thread_info();
  tmp___6 = __chk_range_not_ok((unsigned long )udata, (unsigned long )kp->size, tmp___5->addr_limit.seg);
  }
  if (tmp___6) {
    tmp___7 = 0;
  } else {
    tmp___7 = 1;
  }
  {
  tmp___8 = ldv__builtin_expect((long )tmp___7, 1L);
  }
  if (tmp___8 == 0L) {
    return (-14);
  } else {
  }
  {
  tmp___9 = copy_in_user((void *)udata, (void const *)kdata, (unsigned int )kp->size);
  }
  if (tmp___9 != 0UL) {
    return (-14);
  } else {
  }
  return (0);
}
}
static long uvc_v4l2_compat_ioctl32(struct file *file , unsigned int cmd , unsigned long arg )
{
  union __anonunion_karg_274 karg ;
  void *up___0 ;
  void *tmp ;
  mm_segment_t old_fs ;
  long ret ;
  int tmp___0 ;
  int tmp___1 ;
  struct thread_info *tmp___2 ;
  struct thread_info *tmp___3 ;
  mm_segment_t __constr_expr_0 ;
  struct thread_info *tmp___4 ;
  int tmp___5 ;
  int tmp___6 ;
  {
  {
  tmp = compat_ptr((compat_uptr_t )arg);
  up___0 = tmp;
  }
  {
  if (cmd == 3227022624U) {
    goto case_3227022624;
  } else {
  }
  if (cmd == 3222041889U) {
    goto case_3222041889;
  } else {
  }
  goto switch_default;
  case_3227022624:
  {
  cmd = 3227546912U;
  tmp___0 = uvc_v4l2_get_xu_mapping(& karg.xmap, (struct uvc_xu_control_mapping32 const *)up___0);
  ret = (long )tmp___0;
  }
  goto ldv_36898;
  case_3222041889:
  {
  cmd = 3222304033U;
  tmp___1 = uvc_v4l2_get_xu_query(& karg.xqry, (struct uvc_xu_control_query32 const *)up___0);
  ret = (long )tmp___1;
  }
  goto ldv_36898;
  switch_default: ;
  return (-515L);
  switch_break: ;
  }
  ldv_36898:
  {
  tmp___2 = current_thread_info();
  old_fs = tmp___2->addr_limit;
  tmp___3 = current_thread_info();
  __constr_expr_0.seg = 0xffffffffffffffffUL;
  tmp___3->addr_limit = __constr_expr_0;
  ret = uvc_v4l2_ioctl(file, cmd, (unsigned long )(& karg));
  tmp___4 = current_thread_info();
  tmp___4->addr_limit = old_fs;
  }
  if (ret < 0L) {
    return (ret);
  } else {
  }
  {
  if (cmd == 3227546912U) {
    goto case_3227546912;
  } else {
  }
  if (cmd == 3222304033U) {
    goto case_3222304033;
  } else {
  }
  goto switch_break___0;
  case_3227546912:
  {
  tmp___5 = uvc_v4l2_put_xu_mapping((struct uvc_xu_control_mapping const *)(& karg.xmap),
                                    (struct uvc_xu_control_mapping32 *)up___0);
  ret = (long )tmp___5;
  }
  goto ldv_36903;
  case_3222304033:
  {
  tmp___6 = uvc_v4l2_put_xu_query((struct uvc_xu_control_query const *)(& karg.xqry),
                                  (struct uvc_xu_control_query32 *)up___0);
  ret = (long )tmp___6;
  }
  goto ldv_36903;
  switch_break___0: ;
  }
  ldv_36903: ;
  return (ret);
}
}
static ssize_t uvc_v4l2_read(struct file *file , char *data , size_t count , loff_t *ppos )
{
  {
  if ((uvc_trace_param & 32U) != 0U) {
    {
    printk("\017uvcvideo: uvc_v4l2_read: not implemented.\n");
    }
  } else {
  }
  return (-22L);
}
}
static int uvc_v4l2_mmap(struct file *file , struct vm_area_struct *vma )
{
  struct uvc_fh *handle ;
  struct uvc_streaming *stream ;
  int tmp ;
  {
  handle = (struct uvc_fh *)file->private_data;
  stream = handle->stream;
  if ((uvc_trace_param & 32U) != 0U) {
    {
    printk("\017uvcvideo: uvc_v4l2_mmap\n");
    }
  } else {
  }
  {
  tmp = uvc_queue_mmap(& stream->queue, vma);
  }
  return (tmp);
}
}
static unsigned int uvc_v4l2_poll(struct file *file , poll_table *wait )
{
  struct uvc_fh *handle ;
  struct uvc_streaming *stream ;
  unsigned int tmp ;
  {
  handle = (struct uvc_fh *)file->private_data;
  stream = handle->stream;
  if ((uvc_trace_param & 32U) != 0U) {
    {
    printk("\017uvcvideo: uvc_v4l2_poll\n");
    }
  } else {
  }
  {
  tmp = uvc_queue_poll(& stream->queue, file, wait);
  }
  return (tmp);
}
}
struct v4l2_file_operations const uvc_fops =
     {& __this_module, & uvc_v4l2_read, 0, & uvc_v4l2_poll, 0, & uvc_v4l2_ioctl, & uvc_v4l2_compat_ioctl32,
    0, & uvc_v4l2_mmap, & uvc_v4l2_open, & uvc_v4l2_release};
void ldv_io_instance_callback_7_19(long long (*arg0)(struct file * , long long ,
                                                     int ) , struct file *arg1 ,
                                   long long arg2 , int arg3 ) ;
void ldv_io_instance_callback_7_22(int (*arg0)(struct file * , struct vm_area_struct * ) ,
                                   struct file *arg1 , struct vm_area_struct *arg2 ) ;
void ldv_io_instance_callback_7_23(unsigned int (*arg0)(struct file * , struct poll_table_struct * ) ,
                                   struct file *arg1 , struct poll_table_struct *arg2 ) ;
void ldv_io_instance_callback_7_24(long (*arg0)(struct file * , char * , unsigned long ,
                                                long long * ) , struct file *arg1 ,
                                   char *arg2 , unsigned long arg3 , long long *arg4 ) ;
void ldv_io_instance_callback_7_27(long (*arg0)(struct file * , unsigned int , unsigned long ) ,
                                   struct file *arg1 , unsigned int arg2 , unsigned long arg3 ) ;
void ldv_io_instance_callback_7_4(long (*arg0)(struct file * , unsigned int , unsigned long ) ,
                                  struct file *arg1 , unsigned int arg2 , unsigned long arg3 ) ;
int ldv_io_instance_probe_7_11(int (*arg0)(struct file * ) , struct file *arg1 ) ;
void ldv_io_instance_release_7_2(int (*arg0)(struct file * ) , struct file *arg1 ) ;
struct ldv_thread ldv_thread_7 ;
void ldv_io_instance_callback_7_22(int (*arg0)(struct file * , struct vm_area_struct * ) ,
                                   struct file *arg1 , struct vm_area_struct *arg2 )
{
  {
  {
  uvc_v4l2_mmap(arg1, arg2);
  }
  return;
}
}
void ldv_io_instance_callback_7_23(unsigned int (*arg0)(struct file * , struct poll_table_struct * ) ,
                                   struct file *arg1 , struct poll_table_struct *arg2 )
{
  {
  {
  uvc_v4l2_poll(arg1, arg2);
  }
  return;
}
}
void ldv_io_instance_callback_7_24(long (*arg0)(struct file * , char * , unsigned long ,
                                                long long * ) , struct file *arg1 ,
                                   char *arg2 , unsigned long arg3 , long long *arg4 )
{
  {
  {
  uvc_v4l2_read(arg1, arg2, arg3, arg4);
  }
  return;
}
}
void ldv_io_instance_callback_7_27(long (*arg0)(struct file * , unsigned int , unsigned long ) ,
                                   struct file *arg1 , unsigned int arg2 , unsigned long arg3 )
{
  {
  {
  uvc_v4l2_ioctl(arg1, arg2, arg3);
  }
  return;
}
}
void ldv_io_instance_callback_7_4(long (*arg0)(struct file * , unsigned int , unsigned long ) ,
                                  struct file *arg1 , unsigned int arg2 , unsigned long arg3 )
{
  {
  {
  uvc_v4l2_compat_ioctl32(arg1, arg2, arg3);
  }
  return;
}
}
int ldv_io_instance_probe_7_11(int (*arg0)(struct file * ) , struct file *arg1 )
{
  int tmp ;
  {
  {
  tmp = uvc_v4l2_open(arg1);
  }
  return (tmp);
}
}
void ldv_io_instance_release_7_2(int (*arg0)(struct file * ) , struct file *arg1 )
{
  {
  {
  uvc_v4l2_release(arg1);
  }
  return;
}
}
void ldv_v4l2_file_operations_io_instance_7(void *arg0 )
{
  long (*ldv_7_callback_compat_ioctl32)(struct file * , unsigned int , unsigned long ) ;
  long long (*ldv_7_callback_llseek)(struct file * , long long , int ) ;
  int (*ldv_7_callback_mmap)(struct file * , struct vm_area_struct * ) ;
  unsigned int (*ldv_7_callback_poll)(struct file * , struct poll_table_struct * ) ;
  long (*ldv_7_callback_read)(struct file * , char * , unsigned long , long long * ) ;
  long (*ldv_7_callback_unlocked_ioctl)(struct file * , unsigned int , unsigned long ) ;
  struct v4l2_file_operations *ldv_7_container_v4l2_file_operations ;
  long long ldv_7_ldv_param_19_1_default ;
  int ldv_7_ldv_param_19_2_default ;
  char *ldv_7_ldv_param_24_1_default ;
  unsigned long ldv_7_ldv_param_24_2_default ;
  long long *ldv_7_ldv_param_24_3_default ;
  unsigned int ldv_7_ldv_param_27_1_default ;
  unsigned long ldv_7_ldv_param_27_2_default ;
  unsigned int ldv_7_ldv_param_4_1_default ;
  unsigned long ldv_7_ldv_param_4_2_default ;
  struct file *ldv_7_resource_file ;
  struct poll_table_struct *ldv_7_resource_struct_poll_table_struct_ptr ;
  struct vm_area_struct *ldv_7_resource_struct_vm_area_struct_ptr ;
  int ldv_7_ret_default ;
  void *tmp ;
  void *tmp___0 ;
  void *tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  void *tmp___5 ;
  void *tmp___6 ;
  int tmp___7 ;
  {
  {
  ldv_7_ret_default = 1;
  tmp = ldv_xmalloc(520UL);
  ldv_7_resource_file = (struct file *)tmp;
  tmp___0 = ldv_xmalloc(16UL);
  ldv_7_resource_struct_poll_table_struct_ptr = (struct poll_table_struct *)tmp___0;
  tmp___1 = ldv_xmalloc(184UL);
  ldv_7_resource_struct_vm_area_struct_ptr = (struct vm_area_struct *)tmp___1;
  }
  goto ldv_main_7;
  return;
  ldv_main_7:
  {
  tmp___3 = ldv_undef_int();
  }
  if (tmp___3 != 0) {
    {
    ldv_7_ret_default = ldv_io_instance_probe_7_11(ldv_7_container_v4l2_file_operations->open,
                                                   ldv_7_resource_file);
    ldv_7_ret_default = ldv_filter_err_code(ldv_7_ret_default);
    tmp___2 = ldv_undef_int();
    }
    if (tmp___2 != 0) {
      {
      ldv_assume(ldv_7_ret_default == 0);
      }
      goto ldv_call_7;
    } else {
      {
      ldv_assume(ldv_7_ret_default != 0);
      }
      goto ldv_main_7;
    }
  } else {
    {
    ldv_free((void *)ldv_7_resource_file);
    ldv_free((void *)ldv_7_resource_struct_poll_table_struct_ptr);
    ldv_free((void *)ldv_7_resource_struct_vm_area_struct_ptr);
    }
    return;
  }
  return;
  ldv_call_7:
  {
  tmp___7 = ldv_undef_int();
  }
  if (tmp___7 != 0) {
    {
    ldv_io_instance_release_7_2(ldv_7_container_v4l2_file_operations->release, ldv_7_resource_file);
    }
    goto ldv_main_7;
  } else {
    {
    tmp___4 = ldv_undef_int();
    }
    {
    if (tmp___4 == 1) {
      goto case_1;
    } else {
    }
    if (tmp___4 == 2) {
      goto case_2;
    } else {
    }
    if (tmp___4 == 3) {
      goto case_3;
    } else {
    }
    if (tmp___4 == 4) {
      goto case_4;
    } else {
    }
    if (tmp___4 == 5) {
      goto case_5;
    } else {
    }
    if (tmp___4 == 6) {
      goto case_6;
    } else {
    }
    goto switch_default;
    case_1:
    {
    ldv_io_instance_callback_7_27(ldv_7_callback_unlocked_ioctl, ldv_7_resource_file,
                                  ldv_7_ldv_param_27_1_default, ldv_7_ldv_param_27_2_default);
    }
    goto ldv_37082;
    case_2:
    {
    tmp___5 = ldv_xmalloc(1UL);
    ldv_7_ldv_param_24_1_default = (char *)tmp___5;
    tmp___6 = ldv_xmalloc(8UL);
    ldv_7_ldv_param_24_3_default = (long long *)tmp___6;
    ldv_io_instance_callback_7_24(ldv_7_callback_read, ldv_7_resource_file, ldv_7_ldv_param_24_1_default,
                                  ldv_7_ldv_param_24_2_default, ldv_7_ldv_param_24_3_default);
    ldv_free((void *)ldv_7_ldv_param_24_1_default);
    ldv_free((void *)ldv_7_ldv_param_24_3_default);
    }
    goto ldv_37082;
    case_3:
    {
    ldv_io_instance_callback_7_23(ldv_7_callback_poll, ldv_7_resource_file, ldv_7_resource_struct_poll_table_struct_ptr);
    }
    goto ldv_37082;
    case_4:
    {
    ldv_io_instance_callback_7_22(ldv_7_callback_mmap, ldv_7_resource_file, ldv_7_resource_struct_vm_area_struct_ptr);
    }
    goto ldv_37082;
    case_5:
    {
    ldv_io_instance_callback_7_19(ldv_7_callback_llseek, ldv_7_resource_file, ldv_7_ldv_param_19_1_default,
                                  ldv_7_ldv_param_19_2_default);
    }
    goto ldv_37082;
    case_6:
    {
    ldv_io_instance_callback_7_4(ldv_7_callback_compat_ioctl32, ldv_7_resource_file,
                                 ldv_7_ldv_param_4_1_default, ldv_7_ldv_param_4_2_default);
    }
    goto ldv_37082;
    switch_default:
    {
    ldv_stop();
    }
    switch_break: ;
    }
    ldv_37082: ;
  }
  goto ldv_call_7;
  return;
}
}
__inline static void atomic_dec(atomic_t *v )
{
  {
  {
  ldv_atomic_dec(v);
  }
  return;
}
}
__inline static int atomic_add_return(int i , atomic_t *v )
{
  int tmp ;
  {
  {
  tmp = ldv_atomic_add_return(i, v);
  }
  return (tmp);
}
}
static void *ldv_dev_get_drvdata_18___0(struct device const *dev )
{
  void *tmp ;
  {
  {
  tmp = ldv_dev_get_drvdata(dev);
  }
  return (tmp);
}
}
__inline static int test_and_set_bit(long nr , unsigned long volatile *addr )
{
  {
  __asm__ volatile ("":);
  return (0);
  return (1);
}
}
extern int scnprintf(char * , size_t , char const * , ...) ;
__inline static u64 div_u64_rem(u64 dividend , u32 divisor , u32 *remainder )
{
  {
  *remainder = (u32 )(dividend % (u64 )divisor);
  return (dividend / (u64 )divisor);
}
}
__inline static u64 div_u64(u64 dividend , u32 divisor )
{
  u32 remainder ;
  u64 tmp ;
  {
  {
  tmp = div_u64_rem(dividend, divisor, & remainder);
  }
  return (tmp);
}
}
extern void set_normalized_timespec(struct timespec * , time_t , s64 ) ;
__inline static struct timespec timespec_sub(struct timespec lhs , struct timespec rhs )
{
  struct timespec ts_delta ;
  {
  {
  set_normalized_timespec(& ts_delta, lhs.tv_sec - rhs.tv_sec, (s64 )(lhs.tv_nsec - rhs.tv_nsec));
  }
  return (ts_delta);
}
}
extern void getnstimeofday(struct timespec * ) ;
extern void ktime_get_ts(struct timespec * ) ;
__inline static int usb_endpoint_maxp(struct usb_endpoint_descriptor const *epd )
{
  {
  return ((int )epd->wMaxPacketSize);
}
}
extern int usb_get_current_frame_number(struct usb_device * ) ;
__inline static void usb_fill_bulk_urb(struct urb *urb , struct usb_device *dev ,
                                       unsigned int pipe , void *transfer_buffer ,
                                       int buffer_length , void (*complete_fn)(struct urb * ) ,
                                       void *context )
{
  {
  urb->dev = dev;
  urb->pipe = pipe;
  urb->transfer_buffer = transfer_buffer;
  urb->transfer_buffer_length = (u32 )buffer_length;
  urb->complete = complete_fn;
  urb->context = context;
  return;
}
}
extern struct urb *usb_alloc_urb(int , gfp_t ) ;
extern void usb_free_urb(struct urb * ) ;
extern int usb_submit_urb(struct urb * , gfp_t ) ;
extern void usb_kill_urb(struct urb * ) ;
extern void *usb_alloc_coherent(struct usb_device * , size_t , gfp_t , dma_addr_t * ) ;
extern void usb_free_coherent(struct usb_device * , size_t , void * , dma_addr_t ) ;
extern int usb_control_msg(struct usb_device * , unsigned int , __u8 , __u8 , __u16 ,
                           __u16 , void * , __u16 , int ) ;
extern int usb_set_interface(struct usb_device * , int , int ) ;
__inline static unsigned int __create_pipe(struct usb_device *dev , unsigned int endpoint )
{
  {
  return ((unsigned int )(dev->devnum << 8) | (endpoint << 15));
}
}
__inline static void *kzalloc(size_t size , gfp_t flags ) ;
__inline static void put_unaligned_le32(u32 val , void *p )
{
  {
  *((__le32 *)p) = val;
  return;
}
}
void uvc_video_decode_isight(struct urb *urb , struct uvc_streaming *stream , struct uvc_buffer *buf ) ;
size_t uvc_video_stats_dump(struct uvc_streaming *stream , char *buf , size_t size ) ;
static int __uvc_query_ctrl(struct uvc_device *dev , __u8 query , __u8 unit , __u8 intfnum ,
                            __u8 cs , void *data , __u16 size , int timeout )
{
  __u8 type ;
  unsigned int pipe ;
  unsigned int tmp ;
  unsigned int tmp___0 ;
  int tmp___1 ;
  {
  type = 33U;
  if ((int )((signed char )query) < 0) {
    {
    tmp = __create_pipe(dev->udev, 0U);
    pipe = tmp | 2147483776U;
    }
  } else {
    {
    tmp___0 = __create_pipe(dev->udev, 0U);
    pipe = tmp___0 | 2147483648U;
    }
  }
  {
  type = (__u8 )((int )((signed char )type) | ((int )((signed char )query) & -128));
  tmp___1 = usb_control_msg(dev->udev, pipe, (int )query, (int )type, (int )((__u16 )cs) << 8U,
                            (int )((__u16 )((int )((short )((int )unit << 8)) | (int )((short )intfnum))),
                            data, (int )size, timeout);
  }
  return (tmp___1);
}
}
static char const *uvc_query_name(__u8 query )
{
  {
  {
  if ((int )query == 1) {
    goto case_1;
  } else {
  }
  if ((int )query == 129) {
    goto case_129;
  } else {
  }
  if ((int )query == 130) {
    goto case_130;
  } else {
  }
  if ((int )query == 131) {
    goto case_131;
  } else {
  }
  if ((int )query == 132) {
    goto case_132;
  } else {
  }
  if ((int )query == 133) {
    goto case_133;
  } else {
  }
  if ((int )query == 134) {
    goto case_134;
  } else {
  }
  if ((int )query == 135) {
    goto case_135;
  } else {
  }
  goto switch_default;
  case_1: ;
  return ("SET_CUR");
  case_129: ;
  return ("GET_CUR");
  case_130: ;
  return ("GET_MIN");
  case_131: ;
  return ("GET_MAX");
  case_132: ;
  return ("GET_RES");
  case_133: ;
  return ("GET_LEN");
  case_134: ;
  return ("GET_INFO");
  case_135: ;
  return ("GET_DEF");
  switch_default: ;
  return ("<invalid>");
  switch_break: ;
  }
}
}
int uvc_query_ctrl(struct uvc_device *dev , __u8 query , __u8 unit , __u8 intfnum ,
                   __u8 cs , void *data , __u16 size )
{
  int ret ;
  char const *tmp ;
  {
  {
  ret = __uvc_query_ctrl(dev, (int )query, (int )unit, (int )intfnum, (int )cs, data,
                         (int )size, 300);
  }
  if (ret != (int )size) {
    {
    tmp = uvc_query_name((int )query);
    printk("\vuvcvideo: Failed to query (%s) UVC control %u on unit %u: %d (exp. %u).\n",
           tmp, (int )cs, (int )unit, ret, (int )size);
    }
    return (-5);
  } else {
  }
  return (0);
}
}
static void uvc_fixup_video_ctrl(struct uvc_streaming *stream , struct uvc_streaming_control *ctrl )
{
  struct uvc_format *format ;
  struct uvc_frame *frame ;
  unsigned int i ;
  u32 interval ;
  u32 bandwidth ;
  u32 __max1 ;
  u32 __max2 ;
  {
  format = (struct uvc_format *)0;
  frame = (struct uvc_frame *)0;
  i = 0U;
  goto ldv_34602;
  ldv_34601: ;
  if ((int )(stream->format + (unsigned long )i)->index == (int )ctrl->bFormatIndex) {
    format = stream->format + (unsigned long )i;
    goto ldv_34600;
  } else {
  }
  i = i + 1U;
  ldv_34602: ;
  if (i < stream->nformats) {
    goto ldv_34601;
  } else {
  }
  ldv_34600: ;
  if ((unsigned long )format == (unsigned long )((struct uvc_format *)0)) {
    return;
  } else {
  }
  i = 0U;
  goto ldv_34605;
  ldv_34604: ;
  if ((int )(format->frame + (unsigned long )i)->bFrameIndex == (int )ctrl->bFrameIndex) {
    frame = format->frame + (unsigned long )i;
    goto ldv_34603;
  } else {
  }
  i = i + 1U;
  ldv_34605: ;
  if (i < format->nframes) {
    goto ldv_34604;
  } else {
  }
  ldv_34603: ;
  if ((unsigned long )frame == (unsigned long )((struct uvc_frame *)0)) {
    return;
  } else {
  }
  if ((format->flags & 1U) == 0U || (ctrl->dwMaxVideoFrameSize == 0U && (unsigned int )(stream->dev)->uvc_version <= 271U)) {
    ctrl->dwMaxVideoFrameSize = frame->dwMaxVideoFrameBufferSize;
  } else {
  }
  if (((format->flags & 1U) == 0U && ((stream->dev)->quirks & 128U) != 0U) && (stream->intf)->num_altsetting > 1U) {
    interval = ctrl->dwFrameInterval > 100000U ? ctrl->dwFrameInterval : *(frame->dwFrameInterval);
    bandwidth = (u32 )((((int )frame->wWidth * (int )frame->wHeight) / 8) * (int )format->bpp);
    bandwidth = bandwidth * (10000000U / interval + 1U);
    bandwidth = bandwidth / 1000U;
    if ((unsigned int )((stream->dev)->udev)->speed == 3U) {
      bandwidth = bandwidth / 8U;
    } else {
    }
    bandwidth = bandwidth + 12U;
    __max1 = bandwidth;
    __max2 = 1024U;
    bandwidth = __max1 > __max2 ? __max1 : __max2;
    ctrl->dwMaxPayloadTransferSize = bandwidth;
  } else {
  }
  return;
}
}
static int uvc_get_video_ctrl(struct uvc_streaming *stream , struct uvc_streaming_control *ctrl ,
                              int probe , __u8 query )
{
  __u8 *data ;
  __u16 size ;
  int ret ;
  void *tmp ;
  int tmp___0 ;
  int tmp___1 ;
  {
  size = (unsigned int )(stream->dev)->uvc_version > 271U ? 34U : 26U;
  if (((stream->dev)->quirks & 256U) != 0U && (unsigned int )query == 135U) {
    return (-5);
  } else {
  }
  {
  tmp = kmalloc((size_t )size, 208U);
  data = (__u8 *)tmp;
  }
  if ((unsigned long )data == (unsigned long )((__u8 *)0U)) {
    return (-12);
  } else {
  }
  {
  ret = __uvc_query_ctrl(stream->dev, (int )query, 0, (int )((__u8 )stream->intfnum),
                         probe != 0 ? 1 : 2, (void *)data, (int )size, (int )uvc_timeout_param);
  }
  if ((unsigned int )query - 130U <= 1U && ret == 2) {
    {
    tmp___0 = test_and_set_bit(0L, (unsigned long volatile *)(& (stream->dev)->warnings));
    }
    if (tmp___0 == 0) {
      {
      printk("\016uvcvideo: UVC non compliance - GET_MIN/MAX(PROBE) incorrectly supported. Enabling workaround.\n");
      }
    } else {
    }
    {
    memset((void *)ctrl, 0, 34UL);
    ctrl->wCompQuality = __le16_to_cpup((__le16 const *)data);
    ret = 0;
    }
    goto out;
  } else
  if (((unsigned int )query == 135U && probe == 1) && ret != (int )size) {
    {
    tmp___1 = test_and_set_bit(1L, (unsigned long volatile *)(& (stream->dev)->warnings));
    }
    if (tmp___1 == 0) {
      {
      printk("\016uvcvideo: UVC non compliance - GET_DEF(PROBE) not supported. Enabling workaround.\n");
      }
    } else {
    }
    ret = -5;
    goto out;
  } else
  if (ret != (int )size) {
    {
    printk("\vuvcvideo: Failed to query (%u) UVC %s control : %d (exp. %u).\n", (int )query,
           probe != 0 ? (char *)"probe" : (char *)"commit", ret, (int )size);
    ret = -5;
    }
    goto out;
  } else {
  }
  {
  ctrl->bmHint = __le16_to_cpup((__le16 const *)data);
  ctrl->bFormatIndex = *(data + 2UL);
  ctrl->bFrameIndex = *(data + 3UL);
  ctrl->dwFrameInterval = __le32_to_cpup((__le32 const *)data + 4U);
  ctrl->wKeyFrameRate = __le16_to_cpup((__le16 const *)data + 8U);
  ctrl->wPFrameRate = __le16_to_cpup((__le16 const *)data + 10U);
  ctrl->wCompQuality = __le16_to_cpup((__le16 const *)data + 12U);
  ctrl->wCompWindowSize = __le16_to_cpup((__le16 const *)data + 14U);
  ctrl->wDelay = __le16_to_cpup((__le16 const *)data + 16U);
  ctrl->dwMaxVideoFrameSize = get_unaligned_le32((void const *)data + 18U);
  ctrl->dwMaxPayloadTransferSize = get_unaligned_le32((void const *)data + 22U);
  }
  if ((unsigned int )size == 34U) {
    {
    ctrl->dwClockFrequency = get_unaligned_le32((void const *)data + 26U);
    ctrl->bmFramingInfo = *(data + 30UL);
    ctrl->bPreferedVersion = *(data + 31UL);
    ctrl->bMinVersion = *(data + 32UL);
    ctrl->bMaxVersion = *(data + 33UL);
    }
  } else {
    ctrl->dwClockFrequency = (stream->dev)->clock_frequency;
    ctrl->bmFramingInfo = 0U;
    ctrl->bPreferedVersion = 0U;
    ctrl->bMinVersion = 0U;
    ctrl->bMaxVersion = 0U;
  }
  {
  uvc_fixup_video_ctrl(stream, ctrl);
  ret = 0;
  }
  out:
  {
  kfree((void const *)data);
  }
  return (ret);
}
}
static int uvc_set_video_ctrl(struct uvc_streaming *stream , struct uvc_streaming_control *ctrl ,
                              int probe )
{
  __u8 *data ;
  __u16 size ;
  int ret ;
  void *tmp ;
  {
  {
  size = (unsigned int )(stream->dev)->uvc_version > 271U ? 34U : 26U;
  tmp = kzalloc((size_t )size, 208U);
  data = (__u8 *)tmp;
  }
  if ((unsigned long )data == (unsigned long )((__u8 *)0U)) {
    return (-12);
  } else {
  }
  {
  *((__le16 *)data) = ctrl->bmHint;
  *(data + 2UL) = ctrl->bFormatIndex;
  *(data + 3UL) = ctrl->bFrameIndex;
  *((__le32 *)data + 4U) = ctrl->dwFrameInterval;
  *((__le16 *)data + 8U) = ctrl->wKeyFrameRate;
  *((__le16 *)data + 10U) = ctrl->wPFrameRate;
  *((__le16 *)data + 12U) = ctrl->wCompQuality;
  *((__le16 *)data + 14U) = ctrl->wCompWindowSize;
  *((__le16 *)data + 16U) = ctrl->wDelay;
  put_unaligned_le32(ctrl->dwMaxVideoFrameSize, (void *)data + 18U);
  put_unaligned_le32(ctrl->dwMaxPayloadTransferSize, (void *)data + 22U);
  }
  if ((unsigned int )size == 34U) {
    {
    put_unaligned_le32(ctrl->dwClockFrequency, (void *)data + 26U);
    *(data + 30UL) = ctrl->bmFramingInfo;
    *(data + 31UL) = ctrl->bPreferedVersion;
    *(data + 32UL) = ctrl->bMinVersion;
    *(data + 33UL) = ctrl->bMaxVersion;
    }
  } else {
  }
  {
  ret = __uvc_query_ctrl(stream->dev, 1, 0, (int )((__u8 )stream->intfnum), probe != 0 ? 1 : 2,
                         (void *)data, (int )size, (int )uvc_timeout_param);
  }
  if (ret != (int )size) {
    {
    printk("\vuvcvideo: Failed to set UVC %s control : %d (exp. %u).\n", probe != 0 ? (char *)"probe" : (char *)"commit",
           ret, (int )size);
    ret = -5;
    }
  } else {
  }
  {
  kfree((void const *)data);
  }
  return (ret);
}
}
int uvc_probe_video(struct uvc_streaming *stream , struct uvc_streaming_control *probe )
{
  struct uvc_streaming_control probe_min ;
  struct uvc_streaming_control probe_max ;
  __u16 bandwidth ;
  unsigned int i ;
  int ret ;
  {
  {
  ret = uvc_set_video_ctrl(stream, probe, 1);
  }
  if (ret < 0) {
    goto done;
  } else {
  }
  if (((stream->dev)->quirks & 2U) == 0U) {
    {
    ret = uvc_get_video_ctrl(stream, & probe_min, 1, 130);
    }
    if (ret < 0) {
      goto done;
    } else {
    }
    {
    ret = uvc_get_video_ctrl(stream, & probe_max, 1, 131);
    }
    if (ret < 0) {
      goto done;
    } else {
    }
    probe->wCompQuality = probe_max.wCompQuality;
  } else {
  }
  i = 0U;
  goto ldv_34641;
  ldv_34640:
  {
  ret = uvc_set_video_ctrl(stream, probe, 1);
  }
  if (ret < 0) {
    goto done;
  } else {
  }
  {
  ret = uvc_get_video_ctrl(stream, probe, 1, 129);
  }
  if (ret < 0) {
    goto done;
  } else {
  }
  if ((stream->intf)->num_altsetting == 1U) {
    goto ldv_34639;
  } else {
  }
  bandwidth = (__u16 )probe->dwMaxPayloadTransferSize;
  if ((int )bandwidth <= (int )stream->maxpsize) {
    goto ldv_34639;
  } else {
  }
  if (((stream->dev)->quirks & 2U) != 0U) {
    ret = -28;
    goto done;
  } else {
  }
  probe->wKeyFrameRate = probe_min.wKeyFrameRate;
  probe->wPFrameRate = probe_min.wPFrameRate;
  probe->wCompQuality = probe_max.wCompQuality;
  probe->wCompWindowSize = probe_min.wCompWindowSize;
  i = i + 1U;
  ldv_34641: ;
  if (i <= 1U) {
    goto ldv_34640;
  } else {
  }
  ldv_34639: ;
  done: ;
  return (ret);
}
}
static int uvc_commit_video(struct uvc_streaming *stream , struct uvc_streaming_control *probe )
{
  int tmp ;
  {
  {
  tmp = uvc_set_video_ctrl(stream, probe, 0);
  }
  return (tmp);
}
}
static void uvc_video_clock_decode(struct uvc_streaming *stream , struct uvc_buffer *buf ,
                                   __u8 const *data , int len )
{
  struct uvc_clock_sample *sample ;
  unsigned int header_size ;
  bool has_pts ;
  bool has_scr ;
  unsigned long flags ;
  struct timespec ts ;
  u16 host_sof ;
  u16 dev_sof ;
  int tmp ;
  u16 delta_sof ;
  raw_spinlock_t *tmp___0 ;
  {
  has_pts = 0;
  has_scr = 0;
  {
  if (((int )*(data + 1UL) & 12) == 12) {
    goto case_12;
  } else {
  }
  if (((int )*(data + 1UL) & 12) == 4) {
    goto case_4;
  } else {
  }
  if (((int )*(data + 1UL) & 12) == 8) {
    goto case_8;
  } else {
  }
  goto switch_default;
  case_12:
  header_size = 12U;
  has_pts = 1;
  has_scr = 1;
  goto ldv_34661;
  case_4:
  header_size = 6U;
  has_pts = 1;
  goto ldv_34661;
  case_8:
  header_size = 8U;
  has_scr = 1;
  goto ldv_34661;
  switch_default:
  header_size = 2U;
  goto ldv_34661;
  switch_break: ;
  }
  ldv_34661: ;
  if ((unsigned int )len < header_size) {
    return;
  } else {
  }
  if ((int )has_pts && (unsigned long )buf != (unsigned long )((struct uvc_buffer *)0)) {
    {
    buf->pts = get_unaligned_le32((void const *)data + 2U);
    }
  } else {
  }
  if (! has_scr) {
    return;
  } else {
  }
  {
  dev_sof = get_unaligned_le16((void const *)data + (unsigned long )(header_size - 2U));
  }
  if ((int )dev_sof == (int )stream->clock.last_sof) {
    return;
  } else {
  }
  {
  stream->clock.last_sof = dev_sof;
  tmp = usb_get_current_frame_number((stream->dev)->udev);
  host_sof = (u16 )tmp;
  ktime_get_ts(& ts);
  }
  if ((unsigned int )stream->clock.sof_offset == 65535U) {
    delta_sof = (unsigned int )((u16 )((int )host_sof - (int )dev_sof)) & 255U;
    if ((unsigned int )delta_sof > 9U) {
      stream->clock.sof_offset = delta_sof;
    } else {
      stream->clock.sof_offset = 0U;
    }
  } else {
  }
  {
  dev_sof = (unsigned int )((u16 )((int )dev_sof + (int )stream->clock.sof_offset)) & 2047U;
  tmp___0 = spinlock_check(& stream->clock.lock);
  flags = _raw_spin_lock_irqsave(tmp___0);
  sample = stream->clock.samples + (unsigned long )stream->clock.head;
  sample->dev_stc = get_unaligned_le32((void const *)data + (unsigned long )(header_size - 6U));
  sample->dev_sof = dev_sof;
  sample->host_sof = host_sof;
  sample->host_ts = ts;
  stream->clock.head = (stream->clock.head + 1U) % stream->clock.size;
  }
  if (stream->clock.count < stream->clock.size) {
    stream->clock.count = stream->clock.count + 1U;
  } else {
  }
  {
  spin_unlock_irqrestore(& stream->clock.lock, flags);
  }
  return;
}
}
static void uvc_video_clock_reset(struct uvc_streaming *stream )
{
  struct uvc_clock *clock ;
  {
  clock = & stream->clock;
  clock->head = 0U;
  clock->count = 0U;
  clock->last_sof = 65535U;
  clock->sof_offset = 65535U;
  return;
}
}
static int uvc_video_clock_init(struct uvc_streaming *stream )
{
  struct uvc_clock *clock ;
  struct lock_class_key __key ;
  void *tmp ;
  {
  {
  clock = & stream->clock;
  spinlock_check(& clock->lock);
  __raw_spin_lock_init(& clock->lock.__annonCompField19.rlock, "&(&clock->lock)->rlock",
                       & __key);
  clock->size = 32U;
  tmp = kmalloc((unsigned long )clock->size * 32UL, 208U);
  clock->samples = (struct uvc_clock_sample *)tmp;
  }
  if ((unsigned long )clock->samples == (unsigned long )((struct uvc_clock_sample *)0)) {
    return (-12);
  } else {
  }
  {
  uvc_video_clock_reset(stream);
  }
  return (0);
}
}
static void uvc_video_clock_cleanup(struct uvc_streaming *stream )
{
  {
  {
  kfree((void const *)stream->clock.samples);
  stream->clock.samples = (struct uvc_clock_sample *)0;
  }
  return;
}
}
static u16 uvc_video_clock_host_sof(struct uvc_clock_sample const *sample )
{
  s8 delta_sof ;
  {
  delta_sof = (s8 )((int )((unsigned char )sample->host_sof) - (int )((unsigned char )sample->dev_sof));
  return ((unsigned int )((u16 )((int )((unsigned short )sample->dev_sof) + (int )((unsigned short )delta_sof))) & 2047U);
}
}
void uvc_video_clock_update(struct uvc_streaming *stream , struct v4l2_buffer *v4l2_buf ,
                            struct uvc_buffer *buf )
{
  struct uvc_clock *clock ;
  struct uvc_clock_sample *first ;
  struct uvc_clock_sample *last ;
  unsigned long flags ;
  struct timespec ts ;
  u32 delta_stc ;
  u32 y1 ;
  u32 y2 ;
  u32 x1 ;
  u32 x2 ;
  u32 mean ;
  u32 sof ;
  u32 div ;
  u32 rem ;
  u64 y ;
  raw_spinlock_t *tmp ;
  u64 tmp___0 ;
  u64 tmp___1 ;
  u16 tmp___2 ;
  u16 tmp___3 ;
  u64 tmp___4 ;
  u64 tmp___5 ;
  {
  {
  clock = & stream->clock;
  tmp = spinlock_check(& clock->lock);
  flags = _raw_spin_lock_irqsave(tmp);
  }
  if (clock->count < clock->size) {
    goto done;
  } else {
  }
  first = clock->samples + (unsigned long )clock->head;
  last = clock->samples + (unsigned long )((clock->head - 1U) % clock->size);
  delta_stc = buf->pts - 2147483648U;
  x1 = first->dev_stc - delta_stc;
  x2 = last->dev_stc - delta_stc;
  if (x1 == x2) {
    goto done;
  } else {
  }
  y1 = (u32 )(((int )first->dev_sof + 2048) << 16);
  y2 = (u32 )(((int )last->dev_sof + 2048) << 16);
  if (y2 < y1) {
    y2 = y2 + 134217728U;
  } else {
  }
  {
  y = ((unsigned long long )(y2 - y1) * 2147483648ULL + (unsigned long long )y1 * (unsigned long long )x2) - (unsigned long long )y2 * (unsigned long long )x1;
  y = div_u64(y, x2 - x1);
  sof = (u32 )y;
  }
  if ((uvc_trace_param & 4096U) != 0U) {
    {
    tmp___0 = div_u64(((unsigned long long )sof & 65535ULL) * 1000000ULL, 65536U);
    tmp___1 = div_u64((y & 65535ULL) * 1000000ULL, 65536U);
    printk("\017uvcvideo: %s: PTS %u y %llu.%06llu SOF %u.%06llu (x1 %u x2 %u y1 %u y2 %u SOF offset %u)\n",
           (char *)(& (stream->dev)->name), buf->pts, y >> 16, tmp___1, sof >> 16,
           tmp___0, x1, x2, y1, y2, (int )clock->sof_offset);
    }
  } else {
  }
  {
  tmp___2 = uvc_video_clock_host_sof((struct uvc_clock_sample const *)first);
  x1 = (u32 )(((int )tmp___2 + 2048) << 16);
  tmp___3 = uvc_video_clock_host_sof((struct uvc_clock_sample const *)last);
  x2 = (u32 )(((int )tmp___3 + 2048) << 16);
  }
  if (x2 < x1) {
    x2 = x2 + 134217728U;
  } else {
  }
  if (x1 == x2) {
    goto done;
  } else {
  }
  {
  ts = timespec_sub(last->host_ts, first->host_ts);
  y1 = 1000000000U;
  y2 = (u32 )(ts.tv_sec + 1L) * 1000000000U + (u32 )ts.tv_nsec;
  mean = (x1 + x2) / 2U;
  }
  if (mean - 67108864U > sof) {
    sof = sof + 134217728U;
  } else
  if (sof > mean + 67108864U) {
    sof = sof - 134217728U;
  } else {
  }
  {
  y = ((unsigned long long )(y2 - y1) * (unsigned long long )sof + (unsigned long long )y1 * (unsigned long long )x2) - (unsigned long long )y2 * (unsigned long long )x1;
  y = div_u64(y, x2 - x1);
  tmp___4 = div_u64_rem(y, 1000000000U, & rem);
  div = (u32 )tmp___4;
  ts.tv_sec = (first->host_ts.tv_sec + -1L) + (__kernel_time_t )div;
  ts.tv_nsec = first->host_ts.tv_nsec + (long )rem;
  }
  if (ts.tv_nsec > 999999999L) {
    ts.tv_sec = ts.tv_sec + 1L;
    ts.tv_nsec = ts.tv_nsec + -1000000000L;
  } else {
  }
  if ((uvc_trace_param & 4096U) != 0U) {
    {
    tmp___5 = div_u64(((unsigned long long )sof & 65535ULL) * 1000000ULL, 65536U);
    printk("\017uvcvideo: %s: SOF %u.%06llu y %llu ts %lu.%06lu buf ts %lu.%06lu (x1 %u/%u/%u x2 %u/%u/%u y1 %u y2 %u)\n",
           (char *)(& (stream->dev)->name), sof >> 16, tmp___5, y, ts.tv_sec, ts.tv_nsec / 1000L,
           v4l2_buf->timestamp.tv_sec, (unsigned long )v4l2_buf->timestamp.tv_usec,
           x1, (int )first->host_sof, (int )first->dev_sof, x2, (int )last->host_sof,
           (int )last->dev_sof, y1, y2);
    }
  } else {
  }
  v4l2_buf->timestamp.tv_sec = ts.tv_sec;
  v4l2_buf->timestamp.tv_usec = ts.tv_nsec / 1000L;
  done:
  {
  spin_unlock_irqrestore(& stream->clock.lock, flags);
  }
  return;
}
}
static void uvc_video_stats_decode(struct uvc_streaming *stream , __u8 const *data ,
                                   int len )
{
  unsigned int header_size ;
  bool has_pts ;
  bool has_scr ;
  u16 scr_sof ;
  u32 scr_stc ;
  u32 pts ;
  {
  has_pts = 0;
  has_scr = 0;
  scr_sof = scr_sof;
  scr_stc = scr_stc;
  pts = pts;
  if (stream->stats.stream.nb_frames == 0U && stream->stats.frame.nb_packets == 0U) {
    {
    ktime_get_ts(& stream->stats.stream.start_ts);
    }
  } else {
  }
  {
  if (((int )*(data + 1UL) & 12) == 12) {
    goto case_12;
  } else {
  }
  if (((int )*(data + 1UL) & 12) == 4) {
    goto case_4;
  } else {
  }
  if (((int )*(data + 1UL) & 12) == 8) {
    goto case_8;
  } else {
  }
  goto switch_default;
  case_12:
  header_size = 12U;
  has_pts = 1;
  has_scr = 1;
  goto ldv_34721;
  case_4:
  header_size = 6U;
  has_pts = 1;
  goto ldv_34721;
  case_8:
  header_size = 8U;
  has_scr = 1;
  goto ldv_34721;
  switch_default:
  header_size = 2U;
  goto ldv_34721;
  switch_break: ;
  }
  ldv_34721: ;
  if ((unsigned int )len < header_size || (unsigned int )*data < header_size) {
    stream->stats.frame.nb_invalid = stream->stats.frame.nb_invalid + 1U;
    return;
  } else {
  }
  if ((int )has_pts) {
    {
    pts = get_unaligned_le32((void const *)data + 2U);
    }
  } else {
  }
  if ((int )has_scr) {
    {
    scr_stc = get_unaligned_le32((void const *)data + (unsigned long )(header_size - 6U));
    scr_sof = get_unaligned_le16((void const *)data + (unsigned long )(header_size - 2U));
    }
  } else {
  }
  if ((int )has_pts && stream->stats.frame.nb_pts != 0U) {
    if (stream->stats.frame.pts != pts) {
      stream->stats.frame.nb_pts_diffs = stream->stats.frame.nb_pts_diffs + 1U;
      stream->stats.frame.last_pts_diff = stream->stats.frame.nb_packets;
    } else {
    }
  } else {
  }
  if ((int )has_pts) {
    stream->stats.frame.nb_pts = stream->stats.frame.nb_pts + 1U;
    stream->stats.frame.pts = pts;
  } else {
  }
  if (stream->stats.frame.size == 0U) {
    if ((unsigned int )len > header_size) {
      stream->stats.frame.has_initial_pts = has_pts;
    } else {
    }
    if ((unsigned int )len == header_size && (int )has_pts) {
      stream->stats.frame.has_early_pts = 1;
    } else {
    }
  } else {
  }
  if ((int )has_scr && stream->stats.frame.nb_scr != 0U) {
    if (stream->stats.frame.scr_stc != scr_stc) {
      stream->stats.frame.nb_scr_diffs = stream->stats.frame.nb_scr_diffs + 1U;
    } else {
    }
  } else {
  }
  if ((int )has_scr) {
    if (stream->stats.stream.nb_frames != 0U || stream->stats.frame.nb_scr != 0U) {
      stream->stats.stream.scr_sof_count = stream->stats.stream.scr_sof_count + (((unsigned int )scr_sof - stream->stats.stream.scr_sof) & 2047U);
    } else {
    }
    stream->stats.stream.scr_sof = (unsigned int )scr_sof;
    stream->stats.frame.nb_scr = stream->stats.frame.nb_scr + 1U;
    stream->stats.frame.scr_stc = scr_stc;
    stream->stats.frame.scr_sof = scr_sof;
    if ((unsigned int )scr_sof < stream->stats.stream.min_sof) {
      stream->stats.stream.min_sof = (unsigned int )scr_sof;
    } else {
    }
    if ((unsigned int )scr_sof > stream->stats.stream.max_sof) {
      stream->stats.stream.max_sof = (unsigned int )scr_sof;
    } else {
    }
  } else {
  }
  if (stream->stats.frame.size == 0U && (unsigned int )len > header_size) {
    stream->stats.frame.first_data = stream->stats.frame.nb_packets;
  } else {
  }
  stream->stats.frame.size = stream->stats.frame.size + ((unsigned int )len - header_size);
  stream->stats.frame.nb_packets = stream->stats.frame.nb_packets + 1U;
  if ((unsigned int )len > header_size) {
    stream->stats.frame.nb_empty = stream->stats.frame.nb_empty + 1U;
  } else {
  }
  if (((int )*(data + 1UL) & 64) != 0) {
    stream->stats.frame.nb_errors = stream->stats.frame.nb_errors + 1U;
  } else {
  }
  return;
}
}
static void uvc_video_stats_update(struct uvc_streaming *stream )
{
  struct uvc_stats_frame *frame ;
  {
  frame = & stream->stats.frame;
  if ((uvc_trace_param & 2048U) != 0U) {
    {
    printk("\017uvcvideo: frame %u stats: %u/%u/%u packets, %u/%u/%u pts (%searly %sinitial), %u/%u scr, last pts/stc/sof %u/%u/%u\n",
           stream->sequence, frame->first_data, frame->nb_packets - frame->nb_empty,
           frame->nb_packets, frame->nb_pts_diffs, frame->last_pts_diff, frame->nb_pts,
           (int )frame->has_early_pts ? (char *)"" : (char *)"!", (int )frame->has_initial_pts ? (char *)"" : (char *)"!",
           frame->nb_scr_diffs, frame->nb_scr, frame->pts, frame->scr_stc, (int )frame->scr_sof);
    }
  } else {
  }
  stream->stats.stream.nb_frames = stream->stats.stream.nb_frames + 1U;
  stream->stats.stream.nb_packets = stream->stats.stream.nb_packets + stream->stats.frame.nb_packets;
  stream->stats.stream.nb_empty = stream->stats.stream.nb_empty + stream->stats.frame.nb_empty;
  stream->stats.stream.nb_errors = stream->stats.stream.nb_errors + stream->stats.frame.nb_errors;
  stream->stats.stream.nb_invalid = stream->stats.stream.nb_invalid + stream->stats.frame.nb_invalid;
  if ((int )frame->has_early_pts) {
    stream->stats.stream.nb_pts_early = stream->stats.stream.nb_pts_early + 1U;
  } else {
  }
  if ((int )frame->has_initial_pts) {
    stream->stats.stream.nb_pts_initial = stream->stats.stream.nb_pts_initial + 1U;
  } else {
  }
  if (frame->last_pts_diff <= frame->first_data) {
    stream->stats.stream.nb_pts_constant = stream->stats.stream.nb_pts_constant + 1U;
  } else {
  }
  if (frame->nb_scr >= frame->nb_packets - frame->nb_empty) {
    stream->stats.stream.nb_scr_count_ok = stream->stats.stream.nb_scr_count_ok + 1U;
  } else {
  }
  if (frame->nb_scr_diffs + 1U == frame->nb_scr) {
    stream->stats.stream.nb_scr_diffs_ok = stream->stats.stream.nb_scr_diffs_ok + 1U;
  } else {
  }
  {
  memset((void *)(& stream->stats.frame), 0, 60UL);
  }
  return;
}
}
size_t uvc_video_stats_dump(struct uvc_streaming *stream , char *buf , size_t size )
{
  unsigned int scr_sof_freq ;
  unsigned int duration ;
  struct timespec ts ;
  size_t count ;
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  {
  count = 0UL;
  ts.tv_sec = stream->stats.stream.stop_ts.tv_sec - stream->stats.stream.start_ts.tv_sec;
  ts.tv_nsec = stream->stats.stream.stop_ts.tv_nsec - stream->stats.stream.start_ts.tv_nsec;
  if (ts.tv_nsec < 0L) {
    ts.tv_sec = ts.tv_sec - 1L;
    ts.tv_nsec = ts.tv_nsec + 1000000000L;
  } else {
  }
  duration = (unsigned int )ts.tv_sec * 1000U + (unsigned int )(ts.tv_nsec / 1000000L);
  if (duration != 0U) {
    scr_sof_freq = (stream->stats.stream.scr_sof_count * 1000U) / duration;
  } else {
    scr_sof_freq = 0U;
  }
  {
  tmp = scnprintf(buf + count, size - count, "frames:  %u\npackets: %u\nempty:   %u\nerrors:  %u\ninvalid: %u\n",
                  stream->stats.stream.nb_frames, stream->stats.stream.nb_packets,
                  stream->stats.stream.nb_empty, stream->stats.stream.nb_errors, stream->stats.stream.nb_invalid);
  count = count + (size_t )tmp;
  tmp___0 = scnprintf(buf + count, size - count, "pts: %u early, %u initial, %u ok\n",
                      stream->stats.stream.nb_pts_early, stream->stats.stream.nb_pts_initial,
                      stream->stats.stream.nb_pts_constant);
  count = count + (size_t )tmp___0;
  tmp___1 = scnprintf(buf + count, size - count, "scr: %u count ok, %u diff ok\n",
                      stream->stats.stream.nb_scr_count_ok, stream->stats.stream.nb_scr_diffs_ok);
  count = count + (size_t )tmp___1;
  tmp___2 = scnprintf(buf + count, size - count, "sof: %u <= sof <= %u, freq %u.%03u kHz\n",
                      stream->stats.stream.min_sof, stream->stats.stream.max_sof,
                      scr_sof_freq / 1000U, scr_sof_freq % 1000U);
  count = count + (size_t )tmp___2;
  }
  return (count);
}
}
static void uvc_video_stats_start(struct uvc_streaming *stream )
{
  {
  {
  memset((void *)(& stream->stats), 0, 152UL);
  stream->stats.stream.min_sof = 2048U;
  }
  return;
}
}
static void uvc_video_stats_stop(struct uvc_streaming *stream )
{
  {
  {
  ktime_get_ts(& stream->stats.stream.stop_ts);
  }
  return;
}
}
static int uvc_video_decode_start(struct uvc_streaming *stream , struct uvc_buffer *buf ,
                                  __u8 const *data , int len )
{
  __u8 fid ;
  struct timespec ts ;
  {
  if ((len <= 1 || (unsigned int )((unsigned char )*data) <= 1U) || (int )*data > len) {
    stream->stats.frame.nb_invalid = stream->stats.frame.nb_invalid + 1U;
    return (-22);
  } else {
  }
  fid = (unsigned int )((__u8 )*(data + 1UL)) & 1U;
  if ((int )stream->last_fid != (int )fid) {
    stream->sequence = stream->sequence + 1U;
    if (stream->sequence != 0U) {
      {
      uvc_video_stats_update(stream);
      }
    } else {
    }
  } else {
  }
  {
  uvc_video_clock_decode(stream, buf, data, len);
  uvc_video_stats_decode(stream, data, len);
  }
  if ((unsigned long )buf == (unsigned long )((struct uvc_buffer *)0)) {
    stream->last_fid = fid;
    return (-61);
  } else {
  }
  if (((int )*(data + 1UL) & 64) != 0) {
    if ((uvc_trace_param & 128U) != 0U) {
      {
      printk("\017uvcvideo: Marking buffer as bad (error bit set).\n");
      }
    } else {
    }
    buf->error = 1U;
  } else {
  }
  if ((unsigned int )buf->state != 2U) {
    if ((int )fid == (int )stream->last_fid) {
      if ((uvc_trace_param & 128U) != 0U) {
        {
        printk("\017uvcvideo: Dropping payload (out of sync).\n");
        }
      } else {
      }
      if (((stream->dev)->quirks & 16U) != 0U && ((int )*(data + 1UL) & 2) != 0) {
        stream->last_fid = (__u8 )((unsigned int )stream->last_fid ^ 1U);
      } else {
      }
      return (-61);
    } else {
    }
    if (uvc_clock_param == 1U) {
      {
      ktime_get_ts(& ts);
      }
    } else {
      {
      getnstimeofday(& ts);
      }
    }
    buf->buf.v4l2_buf.sequence = stream->sequence;
    buf->buf.v4l2_buf.timestamp.tv_sec = ts.tv_sec;
    buf->buf.v4l2_buf.timestamp.tv_usec = ts.tv_nsec / 1000L;
    buf->state = 2;
  } else {
  }
  if ((int )fid != (int )stream->last_fid && buf->bytesused != 0U) {
    if ((uvc_trace_param & 128U) != 0U) {
      {
      printk("\017uvcvideo: Frame complete (FID bit toggled).\n");
      }
    } else {
    }
    buf->state = 3;
    return (-11);
  } else {
  }
  stream->last_fid = fid;
  return ((int )*data);
}
}
static void uvc_video_decode_data(struct uvc_streaming *stream , struct uvc_buffer *buf ,
                                  __u8 const *data , int len )
{
  unsigned int maxlen ;
  unsigned int nbytes ;
  void *mem ;
  unsigned int _min1 ;
  unsigned int _min2 ;
  {
  if (len <= 0) {
    return;
  } else {
  }
  {
  maxlen = buf->length - buf->bytesused;
  mem = buf->mem + (unsigned long )buf->bytesused;
  _min1 = (unsigned int )len;
  _min2 = maxlen;
  nbytes = _min1 < _min2 ? _min1 : _min2;
  memcpy(mem, (void const *)data, (size_t )nbytes);
  buf->bytesused = buf->bytesused + nbytes;
  }
  if ((unsigned int )len > maxlen) {
    if ((uvc_trace_param & 128U) != 0U) {
      {
      printk("\017uvcvideo: Frame complete (overflow).\n");
      }
    } else {
    }
    buf->state = 3;
  } else {
  }
  return;
}
}
static void uvc_video_decode_end(struct uvc_streaming *stream , struct uvc_buffer *buf ,
                                 __u8 const *data , int len )
{
  {
  if (((int )*(data + 1UL) & 2) != 0 && buf->bytesused != 0U) {
    if ((uvc_trace_param & 128U) != 0U) {
      {
      printk("\017uvcvideo: Frame complete (EOF found).\n");
      }
    } else {
    }
    if ((int )*data == len) {
      if ((uvc_trace_param & 128U) != 0U) {
        {
        printk("\017uvcvideo: EOF in empty payload.\n");
        }
      } else {
      }
    } else {
    }
    buf->state = 3;
    if (((stream->dev)->quirks & 16U) != 0U) {
      stream->last_fid = (__u8 )((unsigned int )stream->last_fid ^ 1U);
    } else {
    }
  } else {
  }
  return;
}
}
static int uvc_video_encode_header(struct uvc_streaming *stream , struct uvc_buffer *buf ,
                                   __u8 *data , int len )
{
  {
  *data = 2U;
  *(data + 1UL) = (__u8 )(((int )((signed char )stream->last_fid) & 1) | -126);
  return (2);
}
}
static int uvc_video_encode_data(struct uvc_streaming *stream , struct uvc_buffer *buf ,
                                 __u8 *data , int len )
{
  struct uvc_video_queue *queue ;
  unsigned int nbytes ;
  void *mem ;
  unsigned int _min1 ;
  unsigned int _min2 ;
  __u32 _min1___0 ;
  unsigned int _min2___0 ;
  {
  {
  queue = & stream->queue;
  mem = buf->mem + (unsigned long )queue->buf_used;
  _min1 = (unsigned int )len;
  _min2 = buf->bytesused - queue->buf_used;
  nbytes = _min1 < _min2 ? _min1 : _min2;
  _min1___0 = stream->bulk.max_payload_size - stream->bulk.payload_size;
  _min2___0 = nbytes;
  nbytes = _min1___0 < _min2___0 ? _min1___0 : _min2___0;
  memcpy((void *)data, (void const *)mem, (size_t )nbytes);
  queue->buf_used = queue->buf_used + nbytes;
  }
  return ((int )nbytes);
}
}
static void uvc_video_decode_isoc(struct urb *urb , struct uvc_streaming *stream ,
                                  struct uvc_buffer *buf )
{
  u8 *mem ;
  int ret ;
  int i ;
  {
  i = 0;
  goto ldv_34803;
  ldv_34802: ;
  if (urb->iso_frame_desc[i].status < 0) {
    if ((uvc_trace_param & 128U) != 0U) {
      {
      printk("\017uvcvideo: USB isochronous frame lost (%d).\n", urb->iso_frame_desc[i].status);
      }
    } else {
    }
    if ((unsigned long )buf != (unsigned long )((struct uvc_buffer *)0)) {
      buf->error = 1U;
    } else {
    }
    goto ldv_34799;
  } else {
  }
  mem = (u8 *)urb->transfer_buffer + (unsigned long )urb->iso_frame_desc[i].offset;
  ldv_34800:
  {
  ret = uvc_video_decode_start(stream, buf, (__u8 const *)mem, (int )urb->iso_frame_desc[i].actual_length);
  }
  if (ret == -11) {
    {
    buf = uvc_queue_next_buffer(& stream->queue, buf);
    }
  } else {
  }
  if (ret == -11) {
    goto ldv_34800;
  } else {
  }
  if (ret < 0) {
    goto ldv_34799;
  } else {
  }
  {
  uvc_video_decode_data(stream, buf, (__u8 const *)mem + (unsigned long )ret, (int )(urb->iso_frame_desc[i].actual_length - (unsigned int )ret));
  uvc_video_decode_end(stream, buf, (__u8 const *)mem, (int )urb->iso_frame_desc[i].actual_length);
  }
  if ((unsigned int )buf->state == 3U) {
    if (buf->length != buf->bytesused && ((stream->cur_format)->flags & 1U) == 0U) {
      buf->error = 1U;
    } else {
    }
    {
    buf = uvc_queue_next_buffer(& stream->queue, buf);
    }
  } else {
  }
  ldv_34799:
  i = i + 1;
  ldv_34803: ;
  if (i < urb->number_of_packets) {
    goto ldv_34802;
  } else {
  }
  return;
}
}
static void uvc_video_decode_bulk(struct urb *urb , struct uvc_streaming *stream ,
                                  struct uvc_buffer *buf )
{
  u8 *mem ;
  int len ;
  int ret ;
  {
  if (urb->actual_length == 0U && stream->bulk.header_size == 0U) {
    return;
  } else {
  }
  mem = (u8 *)urb->transfer_buffer;
  len = (int )urb->actual_length;
  stream->bulk.payload_size = stream->bulk.payload_size + (__u32 )len;
  if (*((unsigned long *)stream + 190UL) == 0UL) {
    ldv_34813:
    {
    ret = uvc_video_decode_start(stream, buf, (__u8 const *)mem, len);
    }
    if (ret == -11) {
      {
      buf = uvc_queue_next_buffer(& stream->queue, buf);
      }
    } else {
    }
    if (ret == -11) {
      goto ldv_34813;
    } else {
    }
    if (ret < 0 || (unsigned long )buf == (unsigned long )((struct uvc_buffer *)0)) {
      stream->bulk.skip_payload = 1;
    } else {
      {
      memcpy((void *)(& stream->bulk.header), (void const *)mem, (size_t )ret);
      stream->bulk.header_size = (unsigned int )ret;
      mem = mem + (unsigned long )ret;
      len = len - ret;
      }
    }
  } else {
  }
  if (stream->bulk.skip_payload == 0 && (unsigned long )buf != (unsigned long )((struct uvc_buffer *)0)) {
    {
    uvc_video_decode_data(stream, buf, (__u8 const *)mem, len);
    }
  } else {
  }
  if (urb->actual_length < urb->transfer_buffer_length || stream->bulk.payload_size >= stream->bulk.max_payload_size) {
    if (stream->bulk.skip_payload == 0 && (unsigned long )buf != (unsigned long )((struct uvc_buffer *)0)) {
      {
      uvc_video_decode_end(stream, buf, (__u8 const *)(& stream->bulk.header), (int )stream->bulk.payload_size);
      }
      if ((unsigned int )buf->state == 3U) {
        {
        buf = uvc_queue_next_buffer(& stream->queue, buf);
        }
      } else {
      }
    } else {
    }
    stream->bulk.header_size = 0U;
    stream->bulk.skip_payload = 0;
    stream->bulk.payload_size = 0U;
  } else {
  }
  return;
}
}
static void uvc_video_encode_bulk(struct urb *urb , struct uvc_streaming *stream ,
                                  struct uvc_buffer *buf )
{
  u8 *mem ;
  int len ;
  int ret ;
  {
  mem = (u8 *)urb->transfer_buffer;
  len = (int )stream->urb_size;
  if ((unsigned long )buf == (unsigned long )((struct uvc_buffer *)0)) {
    urb->transfer_buffer_length = 0U;
    return;
  } else {
  }
  if (stream->bulk.header_size == 0U) {
    {
    ret = uvc_video_encode_header(stream, buf, mem, len);
    stream->bulk.header_size = (unsigned int )ret;
    stream->bulk.payload_size = stream->bulk.payload_size + (__u32 )ret;
    mem = mem + (unsigned long )ret;
    len = len - ret;
    }
  } else {
  }
  {
  ret = uvc_video_encode_data(stream, buf, mem, len);
  stream->bulk.payload_size = stream->bulk.payload_size + (__u32 )ret;
  len = len - ret;
  }
  if (buf->bytesused == stream->queue.buf_used || stream->bulk.payload_size == stream->bulk.max_payload_size) {
    if (buf->bytesused == stream->queue.buf_used) {
      {
      stream->queue.buf_used = 0U;
      buf->state = 3;
      stream->sequence = stream->sequence + 1U;
      buf->buf.v4l2_buf.sequence = stream->sequence;
      uvc_queue_next_buffer(& stream->queue, buf);
      stream->last_fid = (__u8 )((unsigned int )stream->last_fid ^ 1U);
      }
    } else {
    }
    stream->bulk.header_size = 0U;
    stream->bulk.payload_size = 0U;
  } else {
  }
  urb->transfer_buffer_length = stream->urb_size - (unsigned int )len;
  return;
}
}
static void uvc_video_complete(struct urb *urb )
{
  struct uvc_streaming *stream ;
  struct uvc_video_queue *queue ;
  struct uvc_buffer *buf ;
  unsigned long flags ;
  int ret ;
  raw_spinlock_t *tmp ;
  struct list_head const *__mptr ;
  int tmp___0 ;
  {
  stream = (struct uvc_streaming *)urb->context;
  queue = & stream->queue;
  buf = (struct uvc_buffer *)0;
  {
  if (urb->status == 0) {
    goto case_0;
  } else {
  }
  if (urb->status == -2) {
    goto case_neg_2;
  } else {
  }
  if (urb->status == -104) {
    goto case_neg_104;
  } else {
  }
  if (urb->status == -108) {
    goto case_neg_108;
  } else {
  }
  goto switch_default;
  case_0: ;
  goto ldv_34832;
  switch_default:
  {
  printk("\fuvcvideo: Non-zero status (%d) in video completion handler.\n", urb->status);
  }
  case_neg_2: ;
  if ((unsigned int )*((unsigned char *)stream + 336UL) != 0U) {
    return;
  } else {
  }
  case_neg_104: ;
  case_neg_108:
  {
  uvc_queue_cancel(queue, urb->status == -108);
  }
  return;
  switch_break: ;
  }
  ldv_34832:
  {
  tmp = spinlock_check(& queue->irqlock);
  flags = _raw_spin_lock_irqsave(tmp);
  tmp___0 = list_empty((struct list_head const *)(& queue->irqqueue));
  }
  if (tmp___0 == 0) {
    __mptr = (struct list_head const *)queue->irqqueue.next;
    buf = (struct uvc_buffer *)__mptr + 0xfffffffffffffcb8UL;
  } else {
  }
  {
  spin_unlock_irqrestore(& queue->irqlock, flags);
  (*(stream->decode))(urb, stream, buf);
  ret = usb_submit_urb(urb, 32U);
  }
  if (ret < 0) {
    {
    printk("\vuvcvideo: Failed to resubmit video URB (%d).\n", ret);
    }
  } else {
  }
  return;
}
}
static void uvc_free_urb_buffers(struct uvc_streaming *stream )
{
  unsigned int i ;
  {
  i = 0U;
  goto ldv_34847;
  ldv_34846: ;
  if ((unsigned long )stream->urb_buffer[i] != (unsigned long )((char *)0)) {
    {
    usb_free_coherent((stream->dev)->udev, (size_t )stream->urb_size, (void *)stream->urb_buffer[i],
                      stream->urb_dma[i]);
    stream->urb_buffer[i] = (char *)0;
    }
  } else {
  }
  i = i + 1U;
  ldv_34847: ;
  if (i <= 4U) {
    goto ldv_34846;
  } else {
  }
  stream->urb_size = 0U;
  return;
}
}
static int uvc_alloc_urb_buffers(struct uvc_streaming *stream , unsigned int size ,
                                 unsigned int psize , gfp_t gfp_flags )
{
  unsigned int npackets ;
  unsigned int i ;
  void *tmp ;
  {
  if (stream->urb_size != 0U) {
    return ((int )(stream->urb_size / psize));
  } else {
  }
  npackets = ((size + psize) - 1U) / psize;
  if (npackets > 32U) {
    npackets = 32U;
  } else {
  }
  goto ldv_34861;
  ldv_34860:
  i = 0U;
  goto ldv_34859;
  ldv_34858:
  {
  stream->urb_size = psize * npackets;
  tmp = usb_alloc_coherent((stream->dev)->udev, (size_t )stream->urb_size, gfp_flags | 512U,
                           (dma_addr_t *)(& stream->urb_dma) + (unsigned long )i);
  stream->urb_buffer[i] = (char *)tmp;
  }
  if ((unsigned long )stream->urb_buffer[i] == (unsigned long )((char *)0)) {
    {
    uvc_free_urb_buffers(stream);
    }
    goto ldv_34857;
  } else {
  }
  i = i + 1U;
  ldv_34859: ;
  if (i <= 4U) {
    goto ldv_34858;
  } else {
  }
  ldv_34857: ;
  if (i == 5U) {
    if ((uvc_trace_param & 1024U) != 0U) {
      {
      printk("\017uvcvideo: Allocated %u URB buffers of %ux%u bytes each.\n", 5, npackets,
             psize);
      }
    } else {
    }
    return ((int )npackets);
  } else {
  }
  npackets = npackets / 2U;
  ldv_34861: ;
  if (npackets > 1U) {
    goto ldv_34860;
  } else {
  }
  if ((uvc_trace_param & 1024U) != 0U) {
    {
    printk("\017uvcvideo: Failed to allocate URB buffers (%u bytes per packet).\n",
           psize);
    }
  } else {
  }
  return (0);
}
}
static void uvc_uninit_video(struct uvc_streaming *stream , int free_buffers )
{
  struct urb *urb ;
  unsigned int i ;
  {
  {
  uvc_video_stats_stop(stream);
  i = 0U;
  }
  goto ldv_34871;
  ldv_34870:
  urb = stream->urb[i];
  if ((unsigned long )urb == (unsigned long )((struct urb *)0)) {
    goto ldv_34869;
  } else {
  }
  {
  usb_kill_urb(urb);
  usb_free_urb(urb);
  stream->urb[i] = (struct urb *)0;
  }
  ldv_34869:
  i = i + 1U;
  ldv_34871: ;
  if (i <= 4U) {
    goto ldv_34870;
  } else {
  }
  if (free_buffers != 0) {
    {
    uvc_free_urb_buffers(stream);
    }
  } else {
  }
  return;
}
}
static unsigned int uvc_endpoint_max_bpi(struct usb_device *dev , struct usb_host_endpoint *ep )
{
  u16 psize ;
  int tmp ;
  int tmp___0 ;
  {
  {
  if ((unsigned int )dev->speed == 5U) {
    goto case_5;
  } else {
  }
  if ((unsigned int )dev->speed == 3U) {
    goto case_3;
  } else {
  }
  goto switch_default;
  case_5: ;
  return ((unsigned int )ep->ss_ep_comp.wBytesPerInterval);
  case_3:
  {
  tmp = usb_endpoint_maxp((struct usb_endpoint_descriptor const *)(& ep->desc));
  psize = (u16 )tmp;
  }
  return ((unsigned int )(((int )psize & 2047) * ((((int )psize >> 11) & 3) + 1)));
  switch_default:
  {
  tmp___0 = usb_endpoint_maxp((struct usb_endpoint_descriptor const *)(& ep->desc));
  psize = (u16 )tmp___0;
  }
  return ((unsigned int )psize & 2047U);
  switch_break: ;
  }
}
}
static int uvc_init_video_isoc(struct uvc_streaming *stream , struct usb_host_endpoint *ep ,
                               gfp_t gfp_flags )
{
  struct urb *urb ;
  unsigned int npackets ;
  unsigned int i ;
  unsigned int j ;
  u16 psize ;
  u32 size ;
  unsigned int tmp ;
  int tmp___0 ;
  unsigned int tmp___1 ;
  {
  {
  tmp = uvc_endpoint_max_bpi((stream->dev)->udev, ep);
  psize = (u16 )tmp;
  size = stream->ctrl.dwMaxVideoFrameSize;
  tmp___0 = uvc_alloc_urb_buffers(stream, size, (unsigned int )psize, gfp_flags);
  npackets = (unsigned int )tmp___0;
  }
  if (npackets == 0U) {
    return (-12);
  } else {
  }
  size = npackets * (unsigned int )psize;
  i = 0U;
  goto ldv_34896;
  ldv_34895:
  {
  urb = usb_alloc_urb((int )npackets, gfp_flags);
  }
  if ((unsigned long )urb == (unsigned long )((struct urb *)0)) {
    {
    uvc_uninit_video(stream, 1);
    }
    return (-12);
  } else {
  }
  {
  urb->dev = (stream->dev)->udev;
  urb->context = (void *)stream;
  tmp___1 = __create_pipe((stream->dev)->udev, (unsigned int )ep->desc.bEndpointAddress);
  urb->pipe = tmp___1 | 128U;
  urb->transfer_flags = 6U;
  urb->transfer_dma = stream->urb_dma[i];
  urb->interval = (int )ep->desc.bInterval;
  urb->transfer_buffer = (void *)stream->urb_buffer[i];
  urb->complete = & uvc_video_complete;
  urb->number_of_packets = (int )npackets;
  urb->transfer_buffer_length = size;
  j = 0U;
  }
  goto ldv_34893;
  ldv_34892:
  urb->iso_frame_desc[j].offset = j * (unsigned int )psize;
  urb->iso_frame_desc[j].length = (unsigned int )psize;
  j = j + 1U;
  ldv_34893: ;
  if (j < npackets) {
    goto ldv_34892;
  } else {
  }
  stream->urb[i] = urb;
  i = i + 1U;
  ldv_34896: ;
  if (i <= 4U) {
    goto ldv_34895;
  } else {
  }
  return (0);
}
}
static int uvc_init_video_bulk(struct uvc_streaming *stream , struct usb_host_endpoint *ep ,
                               gfp_t gfp_flags )
{
  struct urb *urb ;
  unsigned int npackets ;
  unsigned int pipe ;
  unsigned int i ;
  u16 psize ;
  u32 size ;
  int tmp ;
  int tmp___0 ;
  unsigned int tmp___1 ;
  unsigned int tmp___2 ;
  int tmp___3 ;
  {
  {
  tmp = usb_endpoint_maxp((struct usb_endpoint_descriptor const *)(& ep->desc));
  psize = (unsigned int )((u16 )tmp) & 2047U;
  size = stream->ctrl.dwMaxPayloadTransferSize;
  stream->bulk.max_payload_size = size;
  tmp___0 = uvc_alloc_urb_buffers(stream, size, (unsigned int )psize, gfp_flags);
  npackets = (unsigned int )tmp___0;
  }
  if (npackets == 0U) {
    return (-12);
  } else {
  }
  {
  size = npackets * (unsigned int )psize;
  tmp___3 = usb_endpoint_dir_in((struct usb_endpoint_descriptor const *)(& ep->desc));
  }
  if (tmp___3 != 0) {
    {
    tmp___1 = __create_pipe((stream->dev)->udev, (unsigned int )ep->desc.bEndpointAddress);
    pipe = tmp___1 | 3221225600U;
    }
  } else {
    {
    tmp___2 = __create_pipe((stream->dev)->udev, (unsigned int )ep->desc.bEndpointAddress);
    pipe = tmp___2 | 3221225472U;
    }
  }
  if ((unsigned int )stream->type == 2U) {
    size = 0U;
  } else {
  }
  i = 0U;
  goto ldv_34910;
  ldv_34909:
  {
  urb = usb_alloc_urb(0, gfp_flags);
  }
  if ((unsigned long )urb == (unsigned long )((struct urb *)0)) {
    {
    uvc_uninit_video(stream, 1);
    }
    return (-12);
  } else {
  }
  {
  usb_fill_bulk_urb(urb, (stream->dev)->udev, pipe, (void *)stream->urb_buffer[i],
                    (int )size, & uvc_video_complete, (void *)stream);
  urb->transfer_flags = 4U;
  urb->transfer_dma = stream->urb_dma[i];
  stream->urb[i] = urb;
  i = i + 1U;
  }
  ldv_34910: ;
  if (i <= 4U) {
    goto ldv_34909;
  } else {
  }
  return (0);
}
}
static int uvc_init_video(struct uvc_streaming *stream , gfp_t gfp_flags )
{
  struct usb_interface *intf ;
  struct usb_host_endpoint *ep ;
  unsigned int i ;
  int ret ;
  struct usb_host_endpoint *best_ep ;
  unsigned int best_psize ;
  unsigned int bandwidth ;
  unsigned int altsetting ;
  int intfnum ;
  struct usb_host_interface *alts ;
  unsigned int psize ;
  {
  {
  intf = stream->intf;
  stream->sequence = 4294967295U;
  stream->last_fid = 255U;
  stream->bulk.header_size = 0U;
  stream->bulk.skip_payload = 0;
  stream->bulk.payload_size = 0U;
  uvc_video_stats_start(stream);
  }
  if (intf->num_altsetting > 1U) {
    best_ep = (struct usb_host_endpoint *)0;
    best_psize = 4294967295U;
    altsetting = altsetting;
    intfnum = stream->intfnum;
    bandwidth = stream->ctrl.dwMaxPayloadTransferSize;
    if (bandwidth == 0U) {
      if ((uvc_trace_param & 1024U) != 0U) {
        {
        printk("\017uvcvideo: Device requested null bandwidth, defaulting to lowest.\n");
        }
      } else {
      }
      bandwidth = 1U;
    } else
    if ((uvc_trace_param & 1024U) != 0U) {
      {
      printk("\017uvcvideo: Device requested %u B/frame bandwidth.\n", bandwidth);
      }
    } else {
    }
    i = 0U;
    goto ldv_34929;
    ldv_34928:
    {
    alts = intf->altsetting + (unsigned long )i;
    ep = uvc_find_endpoint(alts, (int )stream->header.bEndpointAddress);
    }
    if ((unsigned long )ep == (unsigned long )((struct usb_host_endpoint *)0)) {
      goto ldv_34927;
    } else {
    }
    {
    psize = uvc_endpoint_max_bpi((stream->dev)->udev, ep);
    }
    if (psize >= bandwidth && psize <= best_psize) {
      altsetting = (unsigned int )alts->desc.bAlternateSetting;
      best_psize = psize;
      best_ep = ep;
    } else {
    }
    ldv_34927:
    i = i + 1U;
    ldv_34929: ;
    if (i < intf->num_altsetting) {
      goto ldv_34928;
    } else {
    }
    if ((unsigned long )best_ep == (unsigned long )((struct usb_host_endpoint *)0)) {
      if ((uvc_trace_param & 1024U) != 0U) {
        {
        printk("\017uvcvideo: No fast enough alt setting for requested bandwidth.\n");
        }
      } else {
      }
      return (-5);
    } else {
    }
    if ((uvc_trace_param & 1024U) != 0U) {
      {
      printk("\017uvcvideo: Selecting alternate setting %u (%u B/frame bandwidth).\n",
             altsetting, best_psize);
      }
    } else {
    }
    {
    ret = usb_set_interface((stream->dev)->udev, intfnum, (int )altsetting);
    }
    if (ret < 0) {
      return (ret);
    } else {
    }
    {
    ret = uvc_init_video_isoc(stream, best_ep, gfp_flags);
    }
  } else {
    {
    ep = uvc_find_endpoint(intf->altsetting, (int )stream->header.bEndpointAddress);
    }
    if ((unsigned long )ep == (unsigned long )((struct usb_host_endpoint *)0)) {
      return (-5);
    } else {
    }
    {
    ret = uvc_init_video_bulk(stream, ep, gfp_flags);
    }
  }
  if (ret < 0) {
    return (ret);
  } else {
  }
  i = 0U;
  goto ldv_34932;
  ldv_34931:
  {
  ret = usb_submit_urb(stream->urb[i], gfp_flags);
  }
  if (ret < 0) {
    {
    printk("\vuvcvideo: Failed to submit URB %u (%d).\n", i, ret);
    uvc_uninit_video(stream, 1);
    }
    return (ret);
  } else {
  }
  i = i + 1U;
  ldv_34932: ;
  if (i <= 4U) {
    goto ldv_34931;
  } else {
  }
  return (0);
}
}
int uvc_video_suspend(struct uvc_streaming *stream )
{
  int tmp ;
  {
  {
  tmp = uvc_queue_streaming(& stream->queue);
  }
  if (tmp == 0) {
    return (0);
  } else {
  }
  {
  stream->frozen = 1U;
  uvc_uninit_video(stream, 0);
  usb_set_interface((stream->dev)->udev, stream->intfnum, 0);
  }
  return (0);
}
}
int uvc_video_resume(struct uvc_streaming *stream , int reset )
{
  int ret ;
  int tmp ;
  {
  if (reset != 0) {
    {
    usb_set_interface((stream->dev)->udev, stream->intfnum, 0);
    }
  } else {
  }
  {
  stream->frozen = 0U;
  uvc_video_clock_reset(stream);
  ret = uvc_commit_video(stream, & stream->ctrl);
  }
  if (ret < 0) {
    {
    uvc_queue_enable(& stream->queue, 0);
    }
    return (ret);
  } else {
  }
  {
  tmp = uvc_queue_streaming(& stream->queue);
  }
  if (tmp == 0) {
    return (0);
  } else {
  }
  {
  ret = uvc_init_video(stream, 16U);
  }
  if (ret < 0) {
    {
    uvc_queue_enable(& stream->queue, 0);
    }
  } else {
  }
  return (ret);
}
}
int uvc_video_init(struct uvc_streaming *stream )
{
  struct uvc_streaming_control *probe ;
  struct uvc_format *format ;
  struct uvc_frame *frame ;
  unsigned int i ;
  int ret ;
  int tmp ;
  {
  probe = & stream->ctrl;
  format = (struct uvc_format *)0;
  frame = (struct uvc_frame *)0;
  if (stream->nformats == 0U) {
    {
    printk("\016uvcvideo: No supported video formats found.\n");
    }
    return (-22);
  } else {
  }
  {
  atomic_set(& stream->active, 0);
  ret = uvc_queue_init(& stream->queue, stream->type, uvc_no_drop_param == 0U);
  }
  if (ret != 0) {
    return (ret);
  } else {
  }
  {
  usb_set_interface((stream->dev)->udev, stream->intfnum, 0);
  tmp = uvc_get_video_ctrl(stream, probe, 1, 135);
  }
  if (tmp == 0) {
    {
    uvc_set_video_ctrl(stream, probe, 1);
    }
  } else {
  }
  {
  ret = uvc_get_video_ctrl(stream, probe, 1, 129);
  }
  if (ret < 0) {
    return (ret);
  } else {
  }
  i = stream->nformats;
  goto ldv_34952;
  ldv_34951:
  format = stream->format + (unsigned long )(i - 1U);
  if ((int )format->index == (int )probe->bFormatIndex) {
    goto ldv_34950;
  } else {
  }
  i = i - 1U;
  ldv_34952: ;
  if (i != 0U) {
    goto ldv_34951;
  } else {
  }
  ldv_34950: ;
  if (format->nframes == 0U) {
    {
    printk("\016uvcvideo: No frame descriptor found for the default format.\n");
    }
    return (-22);
  } else {
  }
  i = format->nframes;
  goto ldv_34955;
  ldv_34954:
  frame = format->frame + (unsigned long )(i - 1U);
  if ((int )frame->bFrameIndex == (int )probe->bFrameIndex) {
    goto ldv_34953;
  } else {
  }
  i = i - 1U;
  ldv_34955: ;
  if (i != 0U) {
    goto ldv_34954;
  } else {
  }
  ldv_34953:
  probe->bFormatIndex = format->index;
  probe->bFrameIndex = frame->bFrameIndex;
  stream->def_format = format;
  stream->cur_format = format;
  stream->cur_frame = frame;
  if ((unsigned int )stream->type == 1U) {
    if (((stream->dev)->quirks & 8U) != 0U) {
      stream->decode = & uvc_video_decode_isight;
    } else
    if ((stream->intf)->num_altsetting > 1U) {
      stream->decode = & uvc_video_decode_isoc;
    } else {
      stream->decode = & uvc_video_decode_bulk;
    }
  } else
  if ((stream->intf)->num_altsetting == 1U) {
    stream->decode = & uvc_video_encode_bulk;
  } else {
    {
    printk("\016uvcvideo: Isochronous endpoints are not supported for video output devices.\n");
    }
    return (-22);
  }
  return (0);
}
}
int uvc_video_enable(struct uvc_streaming *stream , int enable )
{
  int ret ;
  {
  if (enable == 0) {
    {
    uvc_uninit_video(stream, 1);
    usb_set_interface((stream->dev)->udev, stream->intfnum, 0);
    uvc_queue_enable(& stream->queue, 0);
    uvc_video_clock_cleanup(stream);
    }
    return (0);
  } else {
  }
  {
  ret = uvc_video_clock_init(stream);
  }
  if (ret < 0) {
    return (ret);
  } else {
  }
  {
  ret = uvc_queue_enable(& stream->queue, 1);
  }
  if (ret < 0) {
    goto error_queue;
  } else {
  }
  {
  ret = uvc_commit_video(stream, & stream->ctrl);
  }
  if (ret < 0) {
    goto error_commit;
  } else {
  }
  {
  ret = uvc_init_video(stream, 208U);
  }
  if (ret < 0) {
    goto error_video;
  } else {
  }
  return (0);
  error_video:
  {
  usb_set_interface((stream->dev)->udev, stream->intfnum, 0);
  }
  error_commit:
  {
  uvc_queue_enable(& stream->queue, 0);
  }
  error_queue:
  {
  uvc_video_clock_cleanup(stream);
  }
  return (ret);
}
}
extern size_t memweight(void const * , size_t ) ;
__inline static void atomic_dec(atomic_t *v ) ;
__inline static int atomic_add_return(int i , atomic_t *v ) ;
extern int mutex_lock_interruptible_nested(struct mutex * , unsigned int ) ;
extern unsigned long _copy_to_user(void * , void const * , unsigned int ) ;
extern void __copy_to_user_overflow(void) ;
__inline static unsigned long copy_to_user(void *to , void const *from , unsigned long n )
{
  int sz ;
  long tmp ;
  long tmp___0 ;
  {
  {
  sz = -1;
  might_fault();
  tmp = ldv__builtin_expect(sz < 0, 1L);
  }
  if (tmp != 0L) {
    {
    n = _copy_to_user(to, from, (unsigned int )n);
    }
  } else {
    {
    tmp___0 = ldv__builtin_expect((unsigned long )sz >= n, 1L);
    }
    if (tmp___0 != 0L) {
      {
      n = _copy_to_user(to, from, (unsigned int )n);
      }
    } else {
      {
      __copy_to_user_overflow();
      }
    }
  }
  return (n);
}
}
extern int usb_match_one_id(struct usb_interface * , struct usb_device_id const * ) ;
__inline static void *kmalloc_array(size_t n , size_t size , gfp_t flags )
{
  void *tmp ;
  {
  if (size != 0UL && n > 0xffffffffffffffffUL / size) {
    return ((void *)0);
  } else {
  }
  {
  tmp = __kmalloc(n * size, flags);
  }
  return (tmp);
}
}
void *ldv_calloc(size_t nmemb , size_t size ) ;
__inline static void *kcalloc(size_t n , size_t size , gfp_t flags )
{
  void *tmp ;
  {
  {
  tmp = kmalloc_array(n, size, flags | 32768U);
  }
  return (tmp);
}
}
__inline static void *kzalloc(size_t size , gfp_t flags ) ;
extern void v4l2_ctrl_replace(struct v4l2_event * , struct v4l2_event const * ) ;
extern void v4l2_ctrl_merge(struct v4l2_event const * , struct v4l2_event * ) ;
extern void v4l2_event_queue_fh(struct v4l2_fh * , struct v4l2_event const * ) ;
static struct uvc_control_info uvc_ctrls[35U] =
  { {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 0U,
      2U, 2U, 127U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 1U,
      3U, 2U, 127U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 2U,
      6U, 2U, 255U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 3U,
      7U, 2U, 127U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 4U,
      8U, 2U, 127U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 5U,
      9U, 2U, 127U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 6U,
      10U, 2U, 255U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 7U,
      12U, 4U, 255U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 8U,
      1U, 2U, 127U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 9U,
      4U, 2U, 127U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 10U,
      5U, 1U, 99U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 11U,
      16U, 1U, 99U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 12U,
      11U, 1U, 99U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 13U,
      13U, 1U, 99U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 14U,
      14U, 2U, 127U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 15U,
      15U, 2U, 127U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 16U,
      17U, 1U, 2U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 17U,
      18U, 1U, 2U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U}, 0U,
      1U, 1U, 67U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U}, 1U,
      2U, 1U, 115U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U}, 2U,
      3U, 1U, 67U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U}, 3U,
      4U, 4U, 127U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U}, 4U,
      5U, 1U, 65U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U}, 5U,
      6U, 2U, 255U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U}, 6U,
      7U, 2U, 189U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U}, 7U,
      9U, 2U, 255U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U}, 8U,
      10U, 1U, 129U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U}, 9U,
      11U, 2U, 255U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U}, 10U,
      12U, 3U, 189U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U}, 11U,
      13U, 8U, 255U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U}, 12U,
      14U, 4U, 189U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U}, 13U,
      15U, 2U, 255U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U}, 14U,
      16U, 2U, 189U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U}, 17U,
      8U, 1U, 99U},
        {{0, 0}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U}, 18U,
      17U, 1U, 195U}};
static struct uvc_menu_info power_line_frequency_controls[3U] = { {0U, {'D', 'i', 's', 'a', 'b', 'l', 'e', 'd', '\000'}},
        {1U, {'5', '0', ' ', 'H', 'z', '\000'}},
        {2U, {'6', '0', ' ', 'H', 'z', '\000'}}};
static struct uvc_menu_info exposure_auto_controls[4U] = { {2U, {'A', 'u', 't', 'o', ' ', 'M', 'o', 'd', 'e', '\000'}},
        {1U, {'M', 'a', 'n', 'u', 'a', 'l', ' ', 'M', 'o', 'd', 'e', '\000'}},
        {4U, {'S', 'h', 'u', 't', 't', 'e', 'r', ' ', 'P', 'r', 'i', 'o', 'r', 'i', 't',
           'y', ' ', 'M', 'o', 'd', 'e', '\000'}},
        {8U, {'A', 'p', 'e', 'r', 't', 'u', 'r', 'e', ' ', 'P', 'r', 'i', 'o', 'r', 'i',
           't', 'y', ' ', 'M', 'o', 'd', 'e', '\000'}}};
static __s32 uvc_ctrl_get_zoom(struct uvc_control_mapping *mapping , __u8 query ,
                               __u8 const *data )
{
  __s8 zoom ;
  {
  zoom = (signed char )*data;
  {
  if ((int )query == 129) {
    goto case_129;
  } else {
  }
  if ((int )query == 130) {
    goto case_130;
  } else {
  }
  if ((int )query == 131) {
    goto case_131;
  } else {
  }
  if ((int )query == 132) {
    goto case_132;
  } else {
  }
  if ((int )query == 135) {
    goto case_135;
  } else {
  }
  goto switch_default;
  case_129: ;
  return ((int )zoom != 0 ? ((int )zoom > 0 ? (__s32 )*(data + 2UL) : - ((int )*(data + 2UL))) : 0);
  case_130: ;
  case_131: ;
  case_132: ;
  case_135: ;
  switch_default: ;
  return ((__s32 )*(data + 2UL));
  switch_break: ;
  }
}
}
static void uvc_ctrl_set_zoom(struct uvc_control_mapping *mapping , __s32 value ,
                              __u8 *data )
{
  int _min1 ;
  long ret ;
  int __x___0 ;
  int _min2 ;
  {
  *data = value != 0 ? (value > 0 ? 1U : 255U) : 0U;
  __x___0 = value;
  ret = (long )(__x___0 < 0 ? - __x___0 : __x___0);
  _min1 = (int )ret;
  _min2 = 255;
  *(data + 2UL) = (__u8 )(_min1 < _min2 ? _min1 : _min2);
  return;
}
}
static struct uvc_control_mapping uvc_ctrl_mappings[27U] =
  { {{0, 0}, {0, 0}, 9963776U, {'B', 'r', 'i', 'g', 'h', 't', 'n', 'e', 's', 's',
                                 '\000'}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U,
                                           0U, 0U, 0U, 0U, 1U, 1U}, 2U, 16U, 0U, 1,
      1U, 0, 0U, 0U, 0, {0U, 0U}, 0, 0},
        {{0, 0}, {0, 0}, 9963777U, {'C', 'o', 'n', 't', 'r', 'a', 's', 't', '\000'},
      {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 3U, 16U, 0U,
      1, 2U, 0, 0U, 0U, 0, {0U, 0U}, 0, 0},
        {{0, 0}, {0, 0}, 9963779U, {'H', 'u', 'e', '\000'}, {0U, 0U, 0U, 0U, 0U, 0U,
                                                          0U, 0U, 0U, 0U, 0U, 0U,
                                                          0U, 0U, 1U, 1U}, 6U, 16U,
      0U, 1, 1U, 0, 0U, 9963801U, 0, {0U, 0U}, 0, 0},
        {{0, 0}, {0, 0}, 9963778U, {'S', 'a', 't', 'u', 'r', 'a', 't', 'i', 'o', 'n',
                                 '\000'}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U,
                                           0U, 0U, 0U, 0U, 1U, 1U}, 7U, 16U, 0U, 1,
      2U, 0, 0U, 0U, 0, {0U, 0U}, 0, 0},
        {{0, 0}, {0, 0}, 9963803U, {'S', 'h', 'a', 'r', 'p', 'n', 'e', 's', 's', '\000'},
      {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 8U, 16U, 0U,
      1, 2U, 0, 0U, 0U, 0, {0U, 0U}, 0, 0},
        {{0, 0}, {0, 0}, 9963792U, {'G', 'a', 'm', 'm', 'a', '\000'}, {0U, 0U, 0U, 0U,
                                                                    0U, 0U, 0U, 0U,
                                                                    0U, 0U, 0U, 0U,
                                                                    0U, 0U, 1U, 1U},
      9U, 16U, 0U, 1, 2U, 0, 0U, 0U, 0, {0U, 0U}, 0, 0},
        {{0, 0}, {0, 0}, 9963804U, {'B', 'a', 'c', 'k', 'l', 'i', 'g', 'h', 't', ' ',
                                 'C', 'o', 'm', 'p', 'e', 'n', 's', 'a', 't', 'i',
                                 'o', 'n', '\000'}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U,
                                                     0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U},
      1U, 16U, 0U, 1, 2U, 0, 0U, 0U, 0, {0U, 0U}, 0, 0},
        {{0, 0}, {0, 0}, 9963795U, {'G', 'a', 'i', 'n', '\000'}, {0U, 0U, 0U, 0U, 0U,
                                                               0U, 0U, 0U, 0U, 0U,
                                                               0U, 0U, 0U, 0U, 1U,
                                                               1U}, 4U, 16U, 0U, 1,
      2U, 0, 0U, 0U, 0, {0U, 0U}, 0, 0},
        {{0, 0}, {0, 0}, 9963800U, {'P', 'o', 'w', 'e', 'r', ' ', 'L', 'i', 'n', 'e',
                                 ' ', 'F', 'r', 'e', 'q', 'u', 'e', 'n', 'c', 'y',
                                 '\000'}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U,
                                           0U, 0U, 0U, 0U, 1U, 1U}, 5U, 2U, 0U, 3,
      4U, (struct uvc_menu_info *)(& power_line_frequency_controls), 3U, 0U, 0, {0U,
                                                                                 0U},
      0, 0},
        {{0, 0}, {0, 0}, 9963801U, {'H', 'u', 'e', ',', ' ', 'A', 'u', 't', 'o', '\000'},
      {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 16U, 1U, 0U,
      2, 3U, 0, 0U, 0U, 0, {9963779U}, 0, 0},
        {{0, 0}, {0, 0}, 10094849U, {'E', 'x', 'p', 'o', 's', 'u', 'r', 'e', ',', ' ',
                                  'A', 'u', 't', 'o', '\000'}, {0U, 0U, 0U, 0U, 0U,
                                                                0U, 0U, 0U, 0U, 0U,
                                                                0U, 0U, 0U, 0U, 0U,
                                                                1U}, 2U, 4U, 0U, 3,
      5U, (struct uvc_menu_info *)(& exposure_auto_controls), 4U, 0U, 0, {10094850U},
      0, 0},
        {{0, 0}, {0, 0}, 10094851U, {'E', 'x', 'p', 'o', 's', 'u', 'r', 'e', ',', ' ',
                                  'A', 'u', 't', 'o', ' ', 'P', 'r', 'i', 'o', 'r',
                                  'i', 't', 'y', '\000'}, {0U, 0U, 0U, 0U, 0U, 0U,
                                                           0U, 0U, 0U, 0U, 0U, 0U,
                                                           0U, 0U, 0U, 1U}, 3U, 1U,
      0U, 2, 3U, 0, 0U, 0U, 0, {0U, 0U}, 0, 0},
        {{0, 0}, {0, 0}, 10094850U, {'E', 'x', 'p', 'o', 's', 'u', 'r', 'e', ' ', '(',
                                  'A', 'b', 's', 'o', 'l', 'u', 't', 'e', ')', '\000'},
      {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U}, 4U, 32U, 0U,
      1, 2U, 0, 0U, 10094849U, 1, {0U, 0U}, 0, 0},
        {{0, 0}, {0, 0}, 9963788U, {'W', 'h', 'i', 't', 'e', ' ', 'B', 'a', 'l', 'a',
                                 'n', 'c', 'e', ' ', 'T', 'e', 'm', 'p', 'e', 'r',
                                 'a', 't', 'u', 'r', 'e', ',', ' ', 'A', 'u', 't',
                                 'o', '\000'}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U,
                                                0U, 0U, 0U, 0U, 0U, 1U, 1U}, 11U,
      1U, 0U, 2, 3U, 0, 0U, 0U, 0, {9963802U}, 0, 0},
        {{0, 0}, {0, 0}, 9963802U, {'W', 'h', 'i', 't', 'e', ' ', 'B', 'a', 'l', 'a',
                                 'n', 'c', 'e', ' ', 'T', 'e', 'm', 'p', 'e', 'r',
                                 'a', 't', 'u', 'r', 'e', '\000'}, {0U, 0U, 0U, 0U,
                                                                    0U, 0U, 0U, 0U,
                                                                    0U, 0U, 0U, 0U,
                                                                    0U, 0U, 1U, 1U},
      10U, 16U, 0U, 1, 2U, 0, 0U, 9963788U, 0, {0U, 0U}, 0, 0},
        {{0, 0}, {0, 0}, 9963788U, {'W', 'h', 'i', 't', 'e', ' ', 'B', 'a', 'l', 'a',
                                 'n', 'c', 'e', ' ', 'C', 'o', 'm', 'p', 'o', 'n',
                                 'e', 'n', 't', ',', ' ', 'A', 'u', 't', 'o', '\000'},
      {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 13U, 1U, 0U,
      2, 3U, 0, 0U, 0U, 0, {9963791U, 9963790U}, 0, 0},
        {{0, 0}, {0, 0}, 9963791U, {'W', 'h', 'i', 't', 'e', ' ', 'B', 'a', 'l', 'a',
                                 'n', 'c', 'e', ' ', 'B', 'l', 'u', 'e', ' ', 'C',
                                 'o', 'm', 'p', 'o', 'n', 'e', 'n', 't', '\000'},
      {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 1U, 1U}, 12U, 16U,
      0U, 1, 1U, 0, 0U, 9963788U, 0, {0U, 0U}, 0, 0},
        {{0, 0}, {0, 0}, 9963790U, {'W', 'h', 'i', 't', 'e', ' ', 'B', 'a', 'l', 'a',
                                 'n', 'c', 'e', ' ', 'R', 'e', 'd', ' ', 'C', 'o',
                                 'm', 'p', 'o', 'n', 'e', 'n', 't', '\000'}, {0U,
                                                                              0U,
                                                                              0U,
                                                                              0U,
                                                                              0U,
                                                                              0U,
                                                                              0U,
                                                                              0U,
                                                                              0U,
                                                                              0U,
                                                                              0U,
                                                                              0U,
                                                                              0U,
                                                                              0U,
                                                                              1U,
                                                                              1U},
      12U, 16U, 16U, 1, 1U, 0, 0U, 9963788U, 0, {0U, 0U}, 0, 0},
        {{0, 0}, {0, 0}, 10094858U, {'F', 'o', 'c', 'u', 's', ' ', '(', 'a', 'b', 's',
                                  'o', 'l', 'u', 't', 'e', ')', '\000'}, {0U, 0U,
                                                                          0U, 0U,
                                                                          0U, 0U,
                                                                          0U, 0U,
                                                                          0U, 0U,
                                                                          0U, 0U,
                                                                          0U, 0U,
                                                                          0U, 1U},
      6U, 16U, 0U, 1, 2U, 0, 0U, 10094860U, 0, {0U, 0U}, 0, 0},
        {{0, 0}, {0, 0}, 10094860U, {'F', 'o', 'c', 'u', 's', ',', ' ', 'A', 'u', 't',
                                  'o', '\000'}, {0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U,
                                                 0U, 0U, 0U, 0U, 0U, 0U, 1U}, 8U,
      1U, 0U, 2, 3U, 0, 0U, 0U, 0, {10094858U}, 0, 0},
        {{0, 0}, {0, 0}, 10094865U, {'I', 'r', 'i', 's', ',', ' ', 'A', 'b', 's', 'o',
                                  'l', 'u', 't', 'e', '\000'}, {0U, 0U, 0U, 0U, 0U,
                                                                0U, 0U, 0U, 0U, 0U,
                                                                0U, 0U, 0U, 0U, 0U,
                                                                1U}, 9U, 16U, 0U,
      1, 2U, 0, 0U, 0U, 0, {0U, 0U}, 0, 0},
        {{0, 0}, {0, 0}, 10094866U, {'I', 'r', 'i', 's', ',', ' ', 'R', 'e', 'l', 'a',
                                  't', 'i', 'v', 'e', '\000'}, {0U, 0U, 0U, 0U, 0U,
                                                                0U, 0U, 0U, 0U, 0U,
                                                                0U, 0U, 0U, 0U, 0U,
                                                                1U}, 10U, 8U, 0U,
      1, 1U, 0, 0U, 0U, 0, {0U, 0U}, 0, 0},
        {{0, 0}, {0, 0}, 10094861U, {'Z', 'o', 'o', 'm', ',', ' ', 'A', 'b', 's', 'o',
                                  'l', 'u', 't', 'e', '\000'}, {0U, 0U, 0U, 0U, 0U,
                                                                0U, 0U, 0U, 0U, 0U,
                                                                0U, 0U, 0U, 0U, 0U,
                                                                1U}, 11U, 16U, 0U,
      1, 2U, 0, 0U, 0U, 0, {0U, 0U}, 0, 0},
        {{0, 0}, {0, 0}, 10094863U, {'Z', 'o', 'o', 'm', ',', ' ', 'C', 'o', 'n', 't',
                                  'i', 'n', 'u', 'o', 'u', 's', '\000'}, {0U, 0U,
                                                                          0U, 0U,
                                                                          0U, 0U,
                                                                          0U, 0U,
                                                                          0U, 0U,
                                                                          0U, 0U,
                                                                          0U, 0U,
                                                                          0U, 1U},
      12U, 0U, 0U, 1, 1U, 0, 0U, 0U, 0, {0U, 0U}, & uvc_ctrl_get_zoom, & uvc_ctrl_set_zoom},
        {{0,
       0}, {0, 0}, 10094856U, {'P', 'a', 'n', ' ', '(', 'A', 'b', 's', 'o', 'l', 'u',
                               't', 'e', ')', '\000'}, {0U, 0U, 0U, 0U, 0U, 0U, 0U,
                                                        0U, 0U, 0U, 0U, 0U, 0U, 0U,
                                                        0U, 1U}, 13U, 32U, 0U, 1,
      1U, 0, 0U, 0U, 0, {0U, 0U}, 0, 0},
        {{0, 0}, {0, 0}, 10094857U, {'T', 'i', 'l', 't', ' ', '(', 'A', 'b', 's', 'o',
                                  'l', 'u', 't', 'e', ')', '\000'}, {0U, 0U, 0U, 0U,
                                                                     0U, 0U, 0U, 0U,
                                                                     0U, 0U, 0U, 0U,
                                                                     0U, 0U, 0U, 1U},
      13U, 32U, 32U, 1, 1U, 0, 0U, 0U, 0, {0U, 0U}, 0, 0},
        {{0, 0}, {0, 0}, 10094864U, {'P', 'r', 'i', 'v', 'a', 'c', 'y', '\000'}, {0U,
                                                                               0U,
                                                                               0U,
                                                                               0U,
                                                                               0U,
                                                                               0U,
                                                                               0U,
                                                                               0U,
                                                                               0U,
                                                                               0U,
                                                                               0U,
                                                                               0U,
                                                                               0U,
                                                                               0U,
                                                                               0U,
                                                                               1U},
      17U, 1U, 0U, 2, 3U, 0, 0U, 0U, 0, {0U, 0U}, 0, 0}};
__inline static __u8 *uvc_ctrl_data(struct uvc_control *ctrl , int id )
{
  {
  return (ctrl->uvc_data + (unsigned long )(id * (int )ctrl->info.size));
}
}
__inline static int uvc_test_bit(__u8 const *data , int bit )
{
  {
  return (((int )*(data + (unsigned long )(bit >> 3)) >> (bit & 7)) & 1);
}
}
__inline static void uvc_clear_bit(__u8 *data , int bit )
{
  {
  *(data + (unsigned long )(bit >> 3)) = (__u8 )((int )((signed char )*(data + (unsigned long )(bit >> 3))) & ~ ((int )((signed char )(1 << (bit & 7)))));
  return;
}
}
static __s32 uvc_get_le_value(struct uvc_control_mapping *mapping , __u8 query , __u8 const *data )
{
  int bits ;
  int offset ;
  __s32 value ;
  __u8 mask ;
  __u8 byte ;
  {
  bits = (int )mapping->size;
  offset = (int )mapping->offset;
  value = 0;
  data = data + (unsigned long )(offset / 8);
  offset = offset & 7;
  mask = (__u8 )(((1LL << bits) + -1LL) << offset);
  goto ldv_34828;
  ldv_34827:
  byte = (__u8 )((int )((unsigned char )*data) & (int )mask);
  value = value | (offset > 0 ? (int )byte >> offset : (int )byte << - offset);
  bits = bits + ((0 > offset ? 0 : offset) + -8);
  offset = offset + -8;
  mask = (unsigned int )((__u8 )(1 << bits)) + 255U;
  data = data + 1;
  ldv_34828: ;
  if (bits > 0) {
    goto ldv_34827;
  } else {
  }
  if (mapping->data_type == 1U) {
    value = value | - (value & (1 << ((int )mapping->size + -1)));
  } else {
  }
  return (value);
}
}
static void uvc_set_le_value(struct uvc_control_mapping *mapping , __s32 value , __u8 *data )
{
  int bits ;
  int offset ;
  __u8 mask ;
  {
  bits = (int )mapping->size;
  offset = (int )mapping->offset;
  if ((unsigned int )mapping->v4l2_type == 4U) {
    value = -1;
  } else {
  }
  data = data + (unsigned long )(offset / 8);
  offset = offset & 7;
  goto ldv_34839;
  ldv_34838:
  mask = (__u8 )(((1LL << bits) + -1LL) << offset);
  *data = (__u8 )(((int )((signed char )*data) & ~ ((int )((signed char )mask))) | ((int )((signed char )(value << offset)) & (int )((signed char )mask)));
  value = value >> (offset != 0 ? offset : 8);
  bits = bits + (offset + -8);
  offset = 0;
  data = data + 1;
  ldv_34839: ;
  if (bits > 0) {
    goto ldv_34838;
  } else {
  }
  return;
}
}
static __u8 const uvc_processing_guid[16U] =
  { 0U, 0U, 0U, 0U,
        0U, 0U, 0U, 0U,
        0U, 0U, 0U, 0U,
        0U, 0U, 1U, 1U};
static __u8 const uvc_camera_guid[16U] =
  { 0U, 0U, 0U, 0U,
        0U, 0U, 0U, 0U,
        0U, 0U, 0U, 0U,
        0U, 0U, 0U, 1U};
static __u8 const uvc_media_transport_input_guid[16U] =
  { 0U, 0U, 0U, 0U,
        0U, 0U, 0U, 0U,
        0U, 0U, 0U, 0U,
        0U, 0U, 0U, 3U};
static int uvc_entity_match_guid(struct uvc_entity const *entity , __u8 const *guid )
{
  int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  {
  {
  if (((int )entity->type & 32767) == 513) {
    goto case_513;
  } else {
  }
  if (((int )entity->type & 32767) == 514) {
    goto case_514;
  } else {
  }
  if (((int )entity->type & 32767) == 5) {
    goto case_5;
  } else {
  }
  if (((int )entity->type & 32767) == 6) {
    goto case_6;
  } else {
  }
  goto switch_default;
  case_513:
  {
  tmp = memcmp((void const *)(& uvc_camera_guid), (void const *)guid, 16UL);
  }
  return (tmp == 0);
  case_514:
  {
  tmp___0 = memcmp((void const *)(& uvc_media_transport_input_guid), (void const *)guid,
                   16UL);
  }
  return (tmp___0 == 0);
  case_5:
  {
  tmp___1 = memcmp((void const *)(& uvc_processing_guid), (void const *)guid,
                   16UL);
  }
  return (tmp___1 == 0);
  case_6:
  {
  tmp___2 = memcmp((void const *)(& entity->__annonCompField80.extension.guidExtensionCode),
                   (void const *)guid, 16UL);
  }
  return (tmp___2 == 0);
  switch_default: ;
  return (0);
  switch_break: ;
  }
}
}
static void __uvc_find_control(struct uvc_entity *entity , __u32 v4l2_id , struct uvc_control_mapping **mapping ,
                               struct uvc_control **control , int next )
{
  struct uvc_control *ctrl ;
  struct uvc_control_mapping *map ;
  unsigned int i ;
  struct list_head const *__mptr ;
  struct list_head const *__mptr___0 ;
  {
  if ((unsigned long )entity == (unsigned long )((struct uvc_entity *)0)) {
    return;
  } else {
  }
  i = 0U;
  goto ldv_34872;
  ldv_34871:
  ctrl = entity->controls + (unsigned long )i;
  if ((unsigned int )*((unsigned char *)ctrl + 49UL) == 0U) {
    goto ldv_34863;
  } else {
  }
  __mptr = (struct list_head const *)ctrl->info.mappings.next;
  map = (struct uvc_control_mapping *)__mptr;
  goto ldv_34869;
  ldv_34868: ;
  if (map->id == v4l2_id && next == 0) {
    *control = ctrl;
    *mapping = map;
    return;
  } else {
  }
  if (((unsigned long )*mapping == (unsigned long )((struct uvc_control_mapping *)0) || (*mapping)->id > map->id) && (map->id > v4l2_id && next != 0)) {
    *control = ctrl;
    *mapping = map;
  } else {
  }
  __mptr___0 = (struct list_head const *)map->list.next;
  map = (struct uvc_control_mapping *)__mptr___0;
  ldv_34869: ;
  if ((unsigned long )(& map->list) != (unsigned long )(& ctrl->info.mappings)) {
    goto ldv_34868;
  } else {
  }
  ldv_34863:
  i = i + 1U;
  ldv_34872: ;
  if (i < entity->ncontrols) {
    goto ldv_34871;
  } else {
  }
  return;
}
}
static struct uvc_control *uvc_find_control(struct uvc_video_chain *chain , __u32 v4l2_id ,
                                            struct uvc_control_mapping **mapping )
{
  struct uvc_control *ctrl ;
  struct uvc_entity *entity ;
  int next ;
  struct list_head const *__mptr ;
  struct list_head const *__mptr___0 ;
  {
  ctrl = (struct uvc_control *)0;
  next = (long )((int )v4l2_id) & (-0x7FFFFFFF-1);
  *mapping = (struct uvc_control_mapping *)0;
  v4l2_id = v4l2_id & 268435455U;
  __mptr = (struct list_head const *)chain->entities.next;
  entity = (struct uvc_entity *)__mptr + 0xfffffffffffffff0UL;
  goto ldv_34887;
  ldv_34886:
  {
  __uvc_find_control(entity, v4l2_id, mapping, & ctrl, next);
  }
  if ((unsigned long )ctrl != (unsigned long )((struct uvc_control *)0) && next == 0) {
    return (ctrl);
  } else {
  }
  __mptr___0 = (struct list_head const *)entity->chain.next;
  entity = (struct uvc_entity *)__mptr___0 + 0xfffffffffffffff0UL;
  ldv_34887: ;
  if ((unsigned long )(& entity->chain) != (unsigned long )(& chain->entities)) {
    goto ldv_34886;
  } else {
  }
  if ((unsigned long )ctrl == (unsigned long )((struct uvc_control *)0) && next == 0) {
    if ((uvc_trace_param & 4U) != 0U) {
      {
      printk("\017uvcvideo: Control 0x%08x not found.\n", v4l2_id);
      }
    } else {
    }
  } else {
  }
  return (ctrl);
}
}
static int uvc_ctrl_populate_cache(struct uvc_video_chain *chain , struct uvc_control *ctrl )
{
  int ret ;
  __u8 *tmp ;
  __u8 *tmp___0 ;
  __u8 *tmp___1 ;
  __u8 *tmp___2 ;
  int tmp___3 ;
  __u8 *tmp___4 ;
  {
  if ((ctrl->info.flags & 32U) != 0U) {
    {
    tmp = uvc_ctrl_data(ctrl, 5);
    ret = uvc_query_ctrl(chain->dev, 135, (int )(ctrl->entity)->id, (int )((__u8 )(chain->dev)->intfnum),
                         (int )ctrl->info.selector, (void *)tmp, (int )ctrl->info.size);
    }
    if (ret < 0) {
      return (ret);
    } else {
    }
  } else {
  }
  if ((ctrl->info.flags & 4U) != 0U) {
    {
    tmp___0 = uvc_ctrl_data(ctrl, 2);
    ret = uvc_query_ctrl(chain->dev, 130, (int )(ctrl->entity)->id, (int )((__u8 )(chain->dev)->intfnum),
                         (int )ctrl->info.selector, (void *)tmp___0, (int )ctrl->info.size);
    }
    if (ret < 0) {
      return (ret);
    } else {
    }
  } else {
  }
  if ((ctrl->info.flags & 8U) != 0U) {
    {
    tmp___1 = uvc_ctrl_data(ctrl, 3);
    ret = uvc_query_ctrl(chain->dev, 131, (int )(ctrl->entity)->id, (int )((__u8 )(chain->dev)->intfnum),
                         (int )ctrl->info.selector, (void *)tmp___1, (int )ctrl->info.size);
    }
    if (ret < 0) {
      return (ret);
    } else {
    }
  } else {
  }
  if ((ctrl->info.flags & 16U) != 0U) {
    {
    tmp___2 = uvc_ctrl_data(ctrl, 4);
    ret = uvc_query_ctrl(chain->dev, 132, (int )(ctrl->entity)->id, (int )((__u8 )(chain->dev)->intfnum),
                         (int )ctrl->info.selector, (void *)tmp___2, (int )ctrl->info.size);
    }
    if (ret < 0) {
      if (((int )(ctrl->entity)->type & 32767) != 6) {
        return (ret);
      } else {
      }
      {
      tmp___3 = test_and_set_bit(2L, (unsigned long volatile *)(& (chain->dev)->warnings));
      }
      if (tmp___3 == 0) {
        {
        printk("\016uvcvideo: UVC non compliance - GET_RES failed on an XU control. Enabling workaround.\n");
        }
      } else {
      }
      {
      tmp___4 = uvc_ctrl_data(ctrl, 4);
      memset((void *)tmp___4, 0, (size_t )ctrl->info.size);
      }
    } else {
    }
  } else {
  }
  ctrl->cached = 1U;
  return (0);
}
}
static int __uvc_ctrl_get(struct uvc_video_chain *chain , struct uvc_control *ctrl ,
                          struct uvc_control_mapping *mapping , s32 *value )
{
  struct uvc_menu_info *menu ;
  unsigned int i ;
  int ret ;
  __u8 *tmp ;
  __u8 *tmp___0 ;
  {
  if ((ctrl->info.flags & 2U) == 0U) {
    return (-13);
  } else {
  }
  if ((unsigned int )*((unsigned char *)ctrl + 49UL) == 0U) {
    {
    tmp = uvc_ctrl_data(ctrl, 0);
    ret = uvc_query_ctrl(chain->dev, 129, (int )(ctrl->entity)->id, (int )((__u8 )(chain->dev)->intfnum),
                         (int )ctrl->info.selector, (void *)tmp, (int )ctrl->info.size);
    }
    if (ret < 0) {
      return (ret);
    } else {
    }
    ctrl->loaded = 1U;
  } else {
  }
  {
  tmp___0 = uvc_ctrl_data(ctrl, 0);
  *value = (*(mapping->get))(mapping, 129, (__u8 const *)tmp___0);
  }
  if ((unsigned int )mapping->v4l2_type == 3U) {
    menu = mapping->menu_info;
    i = 0U;
    goto ldv_34905;
    ldv_34904: ;
    if (menu->value == (__u32 )*value) {
      *value = (s32 )i;
      goto ldv_34903;
    } else {
    }
    i = i + 1U;
    menu = menu + 1;
    ldv_34905: ;
    if (i < mapping->menu_count) {
      goto ldv_34904;
    } else {
    }
    ldv_34903: ;
  } else {
  }
  return (0);
}
}
static int __uvc_query_v4l2_ctrl(struct uvc_video_chain *chain , struct uvc_control *ctrl ,
                                 struct uvc_control_mapping *mapping , struct v4l2_queryctrl *v4l2_ctrl )
{
  struct uvc_control_mapping *master_map ;
  struct uvc_control *master_ctrl ;
  struct uvc_menu_info *menu ;
  unsigned int i ;
  s32 val ;
  int ret ;
  int tmp ;
  int ret___0 ;
  int tmp___0 ;
  __u8 *tmp___1 ;
  __u8 *tmp___2 ;
  __u8 *tmp___3 ;
  __u8 *tmp___4 ;
  {
  {
  master_map = (struct uvc_control_mapping *)0;
  master_ctrl = (struct uvc_control *)0;
  memset((void *)v4l2_ctrl, 0, 68UL);
  v4l2_ctrl->id = mapping->id;
  v4l2_ctrl->type = (__u32 )mapping->v4l2_type;
  strlcpy((char *)(& v4l2_ctrl->name), (char const *)(& mapping->name), 32UL);
  v4l2_ctrl->flags = 0U;
  }
  if ((ctrl->info.flags & 2U) == 0U) {
    v4l2_ctrl->flags = v4l2_ctrl->flags | 64U;
  } else {
  }
  if ((ctrl->info.flags & 1U) == 0U) {
    v4l2_ctrl->flags = v4l2_ctrl->flags | 4U;
  } else {
  }
  if (mapping->master_id != 0U) {
    {
    __uvc_find_control(ctrl->entity, mapping->master_id, & master_map, & master_ctrl,
                       0);
    }
  } else {
  }
  if ((unsigned long )master_ctrl != (unsigned long )((struct uvc_control *)0) && (master_ctrl->info.flags & 2U) != 0U) {
    {
    tmp = __uvc_ctrl_get(chain, master_ctrl, master_map, & val);
    ret = tmp;
    }
    if (ret < 0) {
      return (ret);
    } else {
    }
    if (val != mapping->master_manual) {
      v4l2_ctrl->flags = v4l2_ctrl->flags | 16U;
    } else {
    }
  } else {
  }
  if ((unsigned int )*((unsigned char *)ctrl + 49UL) == 0U) {
    {
    tmp___0 = uvc_ctrl_populate_cache(chain, ctrl);
    ret___0 = tmp___0;
    }
    if (ret___0 < 0) {
      return (ret___0);
    } else {
    }
  } else {
  }
  if ((ctrl->info.flags & 32U) != 0U) {
    {
    tmp___1 = uvc_ctrl_data(ctrl, 5);
    v4l2_ctrl->default_value = (*(mapping->get))(mapping, 135, (__u8 const *)tmp___1);
    }
  } else {
  }
  {
  if ((unsigned int )mapping->v4l2_type == 3U) {
    goto case_3;
  } else {
  }
  if ((unsigned int )mapping->v4l2_type == 2U) {
    goto case_2;
  } else {
  }
  if ((unsigned int )mapping->v4l2_type == 4U) {
    goto case_4;
  } else {
  }
  goto switch_default;
  case_3:
  v4l2_ctrl->minimum = 0;
  v4l2_ctrl->maximum = (__s32 )(mapping->menu_count - 1U);
  v4l2_ctrl->step = 1;
  menu = mapping->menu_info;
  i = 0U;
  goto ldv_34922;
  ldv_34921: ;
  if (menu->value == (__u32 )v4l2_ctrl->default_value) {
    v4l2_ctrl->default_value = (__s32 )i;
    goto ldv_34920;
  } else {
  }
  i = i + 1U;
  menu = menu + 1;
  ldv_34922: ;
  if (i < mapping->menu_count) {
    goto ldv_34921;
  } else {
  }
  ldv_34920: ;
  return (0);
  case_2:
  v4l2_ctrl->minimum = 0;
  v4l2_ctrl->maximum = 1;
  v4l2_ctrl->step = 1;
  return (0);
  case_4:
  v4l2_ctrl->minimum = 0;
  v4l2_ctrl->maximum = 0;
  v4l2_ctrl->step = 0;
  return (0);
  switch_default: ;
  goto ldv_34926;
  switch_break: ;
  }
  ldv_34926: ;
  if ((ctrl->info.flags & 4U) != 0U) {
    {
    tmp___2 = uvc_ctrl_data(ctrl, 2);
    v4l2_ctrl->minimum = (*(mapping->get))(mapping, 130, (__u8 const *)tmp___2);
    }
  } else {
  }
  if ((ctrl->info.flags & 8U) != 0U) {
    {
    tmp___3 = uvc_ctrl_data(ctrl, 3);
    v4l2_ctrl->maximum = (*(mapping->get))(mapping, 131, (__u8 const *)tmp___3);
    }
  } else {
  }
  if ((ctrl->info.flags & 16U) != 0U) {
    {
    tmp___4 = uvc_ctrl_data(ctrl, 4);
    v4l2_ctrl->step = (*(mapping->get))(mapping, 132, (__u8 const *)tmp___4);
    }
  } else {
  }
  return (0);
}
}
int uvc_query_v4l2_ctrl(struct uvc_video_chain *chain , struct v4l2_queryctrl *v4l2_ctrl )
{
  struct uvc_control *ctrl ;
  struct uvc_control_mapping *mapping ;
  int ret ;
  {
  {
  ret = mutex_lock_interruptible_nested(& chain->ctrl_mutex, 0U);
  }
  if (ret < 0) {
    return (-512);
  } else {
  }
  {
  ctrl = uvc_find_control(chain, v4l2_ctrl->id, & mapping);
  }
  if ((unsigned long )ctrl == (unsigned long )((struct uvc_control *)0)) {
    ret = -22;
    goto done;
  } else {
  }
  {
  ret = __uvc_query_v4l2_ctrl(chain, ctrl, mapping, v4l2_ctrl);
  }
  done:
  {
  mutex_unlock(& chain->ctrl_mutex);
  }
  return (ret);
}
}
int uvc_query_v4l2_menu(struct uvc_video_chain *chain , struct v4l2_querymenu *query_menu )
{
  struct uvc_menu_info *menu_info ;
  struct uvc_control_mapping *mapping ;
  struct uvc_control *ctrl ;
  u32 index ;
  u32 id ;
  int ret ;
  s32 bitmap ;
  __u8 *tmp ;
  {
  {
  index = query_menu->index;
  id = query_menu->id;
  memset((void *)query_menu, 0, 44UL);
  query_menu->id = id;
  query_menu->index = index;
  ret = mutex_lock_interruptible_nested(& chain->ctrl_mutex, 0U);
  }
  if (ret < 0) {
    return (-512);
  } else {
  }
  {
  ctrl = uvc_find_control(chain, query_menu->id, & mapping);
  }
  if ((unsigned long )ctrl == (unsigned long )((struct uvc_control *)0) || (unsigned int )mapping->v4l2_type != 3U) {
    ret = -22;
    goto done;
  } else {
  }
  if (query_menu->index >= mapping->menu_count) {
    ret = -22;
    goto done;
  } else {
  }
  menu_info = mapping->menu_info + (unsigned long )query_menu->index;
  if (mapping->data_type == 5U && (ctrl->info.flags & 16U) != 0U) {
    if ((unsigned int )*((unsigned char *)ctrl + 49UL) == 0U) {
      {
      ret = uvc_ctrl_populate_cache(chain, ctrl);
      }
      if (ret < 0) {
        goto done;
      } else {
      }
    } else {
    }
    {
    tmp = uvc_ctrl_data(ctrl, 4);
    bitmap = (*(mapping->get))(mapping, 132, (__u8 const *)tmp);
    }
    if (((__u32 )bitmap & menu_info->value) == 0U) {
      ret = -22;
      goto done;
    } else {
    }
  } else {
  }
  {
  strlcpy((char *)(& query_menu->__annonCompField73.name), (char const *)(& menu_info->name),
          32UL);
  }
  done:
  {
  mutex_unlock(& chain->ctrl_mutex);
  }
  return (ret);
}
}
static void uvc_ctrl_fill_event(struct uvc_video_chain *chain , struct v4l2_event *ev ,
                                struct uvc_control *ctrl , struct uvc_control_mapping *mapping ,
                                s32 value , u32 changes )
{
  struct v4l2_queryctrl v4l2_ctrl ;
  {
  {
  __uvc_query_v4l2_ctrl(chain, ctrl, mapping, & v4l2_ctrl);
  memset((void *)(& ev->reserved), 0, 32UL);
  ev->type = 3U;
  ev->id = v4l2_ctrl.id;
  ev->u.ctrl.__annonCompField77.value = value;
  ev->u.ctrl.changes = changes;
  ev->u.ctrl.type = v4l2_ctrl.type;
  ev->u.ctrl.flags = v4l2_ctrl.flags;
  ev->u.ctrl.minimum = v4l2_ctrl.minimum;
  ev->u.ctrl.maximum = v4l2_ctrl.maximum;
  ev->u.ctrl.step = v4l2_ctrl.step;
  ev->u.ctrl.default_value = v4l2_ctrl.default_value;
  }
  return;
}
}
static void uvc_ctrl_send_event(struct uvc_fh *handle , struct uvc_control *ctrl ,
                                struct uvc_control_mapping *mapping , s32 value ,
                                u32 changes )
{
  struct v4l2_subscribed_event *sev ;
  struct v4l2_event ev ;
  int tmp ;
  struct list_head const *__mptr ;
  struct list_head const *__mptr___0 ;
  {
  {
  tmp = list_empty((struct list_head const *)(& mapping->ev_subs));
  }
  if (tmp != 0) {
    return;
  } else {
  }
  {
  uvc_ctrl_fill_event(handle->chain, & ev, ctrl, mapping, value, changes);
  __mptr = (struct list_head const *)mapping->ev_subs.next;
  sev = (struct v4l2_subscribed_event *)__mptr + 0xffffffffffffffd8UL;
  }
  goto ldv_34970;
  ldv_34969: ;
  if ((unsigned long )sev->fh != (unsigned long )((struct v4l2_fh *)0) && (((unsigned long )sev->fh != (unsigned long )(& handle->vfh) || (sev->flags & 2U) != 0U) || (changes & 2U) != 0U)) {
    {
    v4l2_event_queue_fh(sev->fh, (struct v4l2_event const *)(& ev));
    }
  } else {
  }
  __mptr___0 = (struct list_head const *)sev->node.next;
  sev = (struct v4l2_subscribed_event *)__mptr___0 + 0xffffffffffffffd8UL;
  ldv_34970: ;
  if ((unsigned long )(& sev->node) != (unsigned long )(& mapping->ev_subs)) {
    goto ldv_34969;
  } else {
  }
  return;
}
}
static void uvc_ctrl_send_slave_event(struct uvc_fh *handle , struct uvc_control *master ,
                                      u32 slave_id , struct v4l2_ext_control const *xctrls ,
                                      unsigned int xctrls_count )
{
  struct uvc_control_mapping *mapping ;
  struct uvc_control *ctrl ;
  u32 changes ;
  unsigned int i ;
  s32 val ;
  int tmp ;
  {
  mapping = (struct uvc_control_mapping *)0;
  ctrl = (struct uvc_control *)0;
  changes = 2U;
  val = 0;
  i = 0U;
  goto ldv_34985;
  ldv_34984: ;
  if ((unsigned int )(xctrls + (unsigned long )i)->id == slave_id) {
    return;
  } else {
  }
  i = i + 1U;
  ldv_34985: ;
  if (i < xctrls_count) {
    goto ldv_34984;
  } else {
  }
  {
  __uvc_find_control(master->entity, slave_id, & mapping, & ctrl, 0);
  }
  if ((unsigned long )ctrl == (unsigned long )((struct uvc_control *)0)) {
    return;
  } else {
  }
  {
  tmp = __uvc_ctrl_get(handle->chain, ctrl, mapping, & val);
  }
  if (tmp == 0) {
    changes = changes | 1U;
  } else {
  }
  {
  uvc_ctrl_send_event(handle, ctrl, mapping, val, changes);
  }
  return;
}
}
static void uvc_ctrl_send_events(struct uvc_fh *handle , struct v4l2_ext_control const *xctrls ,
                                 unsigned int xctrls_count )
{
  struct uvc_control_mapping *mapping ;
  struct uvc_control *ctrl ;
  u32 changes ;
  unsigned int i ;
  unsigned int j ;
  {
  changes = 1U;
  i = 0U;
  goto ldv_35006;
  ldv_35005:
  {
  ctrl = uvc_find_control(handle->chain, (xctrls + (unsigned long )i)->id, & mapping);
  j = 0U;
  }
  goto ldv_35001;
  ldv_35000: ;
  if (mapping->slave_ids[j] == 0U) {
    goto ldv_34999;
  } else {
  }
  {
  uvc_ctrl_send_slave_event(handle, ctrl, mapping->slave_ids[j], xctrls, xctrls_count);
  j = j + 1U;
  }
  ldv_35001: ;
  if (j <= 1U) {
    goto ldv_35000;
  } else {
  }
  ldv_34999: ;
  if (mapping->master_id != 0U) {
    j = 0U;
    goto ldv_35004;
    ldv_35003: ;
    if ((unsigned int )(xctrls + (unsigned long )j)->id == mapping->master_id) {
      changes = changes | 2U;
      goto ldv_35002;
    } else {
    }
    j = j + 1U;
    ldv_35004: ;
    if (j < xctrls_count) {
      goto ldv_35003;
    } else {
    }
    ldv_35002: ;
  } else {
  }
  {
  uvc_ctrl_send_event(handle, ctrl, mapping, (xctrls + (unsigned long )i)->__annonCompField72.value,
                      changes);
  i = i + 1U;
  }
  ldv_35006: ;
  if (i < xctrls_count) {
    goto ldv_35005;
  } else {
  }
  return;
}
}
static int uvc_ctrl_add_event(struct v4l2_subscribed_event *sev , unsigned int elems )
{
  struct uvc_fh *handle ;
  struct v4l2_fh const *__mptr ;
  struct uvc_control_mapping *mapping ;
  struct uvc_control *ctrl ;
  int ret ;
  struct v4l2_event ev ;
  u32 changes ;
  s32 val ;
  int tmp ;
  {
  {
  __mptr = (struct v4l2_fh const *)sev->fh;
  handle = (struct uvc_fh *)__mptr;
  ret = mutex_lock_interruptible_nested(& (handle->chain)->ctrl_mutex, 0U);
  }
  if (ret < 0) {
    return (-512);
  } else {
  }
  {
  ctrl = uvc_find_control(handle->chain, sev->id, & mapping);
  }
  if ((unsigned long )ctrl == (unsigned long )((struct uvc_control *)0)) {
    ret = -22;
    goto done;
  } else {
  }
  {
  list_add_tail(& sev->node, & mapping->ev_subs);
  }
  if ((int )sev->flags & 1) {
    {
    changes = 2U;
    val = 0;
    tmp = __uvc_ctrl_get(handle->chain, ctrl, mapping, & val);
    }
    if (tmp == 0) {
      changes = changes | 1U;
    } else {
    }
    {
    uvc_ctrl_fill_event(handle->chain, & ev, ctrl, mapping, val, changes);
    sev->elems = elems;
    v4l2_event_queue_fh(sev->fh, (struct v4l2_event const *)(& ev));
    }
  } else {
  }
  done:
  {
  mutex_unlock(& (handle->chain)->ctrl_mutex);
  }
  return (ret);
}
}
static void uvc_ctrl_del_event(struct v4l2_subscribed_event *sev )
{
  struct uvc_fh *handle ;
  struct v4l2_fh const *__mptr ;
  {
  {
  __mptr = (struct v4l2_fh const *)sev->fh;
  handle = (struct uvc_fh *)__mptr;
  mutex_lock_nested(& (handle->chain)->ctrl_mutex, 0U);
  list_del(& sev->node);
  mutex_unlock(& (handle->chain)->ctrl_mutex);
  }
  return;
}
}
struct v4l2_subscribed_event_ops const uvc_ctrl_sub_ev_ops = {& uvc_ctrl_add_event, & uvc_ctrl_del_event, & v4l2_ctrl_replace, & v4l2_ctrl_merge};
int uvc_ctrl_begin(struct uvc_video_chain *chain )
{
  int tmp ;
  {
  {
  tmp = mutex_lock_interruptible_nested(& chain->ctrl_mutex, 0U);
  }
  return (tmp != 0 ? -512 : 0);
}
}
static int uvc_ctrl_commit_entity(struct uvc_device *dev , struct uvc_entity *entity ,
                                  int rollback )
{
  struct uvc_control *ctrl ;
  unsigned int i ;
  int ret ;
  __u8 *tmp ;
  __u8 *tmp___0 ;
  __u8 *tmp___1 ;
  {
  if ((unsigned long )entity == (unsigned long )((struct uvc_entity *)0)) {
    return (0);
  } else {
  }
  i = 0U;
  goto ldv_35042;
  ldv_35041:
  ctrl = entity->controls + (unsigned long )i;
  if ((unsigned int )*((unsigned char *)ctrl + 49UL) == 0U) {
    goto ldv_35040;
  } else {
  }
  if (*((unsigned int *)ctrl + 11UL) != 2U) {
    ctrl->loaded = 0U;
  } else {
  }
  if ((unsigned int )*((unsigned char *)ctrl + 49UL) == 0U) {
    goto ldv_35040;
  } else {
  }
  if (rollback == 0) {
    {
    tmp = uvc_ctrl_data(ctrl, 0);
    ret = uvc_query_ctrl(dev, 1, (int )(ctrl->entity)->id, (int )((__u8 )dev->intfnum),
                         (int )ctrl->info.selector, (void *)tmp, (int )ctrl->info.size);
    }
  } else {
    ret = 0;
  }
  if (rollback != 0 || ret < 0) {
    {
    tmp___0 = uvc_ctrl_data(ctrl, 1);
    tmp___1 = uvc_ctrl_data(ctrl, 0);
    memcpy((void *)tmp___1, (void const *)tmp___0, (size_t )ctrl->info.size);
    }
  } else {
  }
  ctrl->dirty = 0U;
  if (ret < 0) {
    return (ret);
  } else {
  }
  ldv_35040:
  i = i + 1U;
  ldv_35042: ;
  if (i < entity->ncontrols) {
    goto ldv_35041;
  } else {
  }
  return (0);
}
}
int __uvc_ctrl_commit(struct uvc_fh *handle , int rollback , struct v4l2_ext_control const *xctrls ,
                      unsigned int xctrls_count )
{
  struct uvc_video_chain *chain ;
  struct uvc_entity *entity ;
  int ret ;
  struct list_head const *__mptr ;
  struct list_head const *__mptr___0 ;
  {
  chain = handle->chain;
  ret = 0;
  __mptr = (struct list_head const *)chain->entities.next;
  entity = (struct uvc_entity *)__mptr + 0xfffffffffffffff0UL;
  goto ldv_35059;
  ldv_35058:
  {
  ret = uvc_ctrl_commit_entity(chain->dev, entity, rollback);
  }
  if (ret < 0) {
    goto done;
  } else {
  }
  __mptr___0 = (struct list_head const *)entity->chain.next;
  entity = (struct uvc_entity *)__mptr___0 + 0xfffffffffffffff0UL;
  ldv_35059: ;
  if ((unsigned long )(& entity->chain) != (unsigned long )(& chain->entities)) {
    goto ldv_35058;
  } else {
  }
  if (rollback == 0) {
    {
    uvc_ctrl_send_events(handle, xctrls, xctrls_count);
    }
  } else {
  }
  done:
  {
  mutex_unlock(& chain->ctrl_mutex);
  }
  return (ret);
}
}
int uvc_ctrl_get(struct uvc_video_chain *chain , struct v4l2_ext_control *xctrl )
{
  struct uvc_control *ctrl ;
  struct uvc_control_mapping *mapping ;
  int tmp ;
  {
  {
  ctrl = uvc_find_control(chain, xctrl->id, & mapping);
  }
  if ((unsigned long )ctrl == (unsigned long )((struct uvc_control *)0)) {
    return (-22);
  } else {
  }
  {
  tmp = __uvc_ctrl_get(chain, ctrl, mapping, & xctrl->__annonCompField72.value);
  }
  return (tmp);
}
}
int uvc_ctrl_set(struct uvc_video_chain *chain , struct v4l2_ext_control *xctrl )
{
  struct uvc_control *ctrl ;
  struct uvc_control_mapping *mapping ;
  s32 value ;
  u32 step ;
  s32 min ;
  s32 max ;
  int ret ;
  __u8 *tmp ;
  __u8 *tmp___0 ;
  __u8 *tmp___1 ;
  __s32 tmp___2 ;
  __s32 __val ;
  s32 __min ;
  s32 __max ;
  u32 __val___0 ;
  u32 __min___0 ;
  u32 __max___0 ;
  __s32 __val___1 ;
  int __min___1 ;
  int __max___1 ;
  __u8 *tmp___3 ;
  __s32 tmp___4 ;
  __u8 *tmp___5 ;
  __u8 *tmp___6 ;
  __u8 *tmp___7 ;
  __u8 *tmp___8 ;
  __u8 *tmp___9 ;
  {
  {
  ctrl = uvc_find_control(chain, xctrl->id, & mapping);
  }
  if ((unsigned long )ctrl == (unsigned long )((struct uvc_control *)0)) {
    return (-22);
  } else {
  }
  if ((ctrl->info.flags & 1U) == 0U) {
    return (-13);
  } else {
  }
  {
  if ((unsigned int )mapping->v4l2_type == 1U) {
    goto case_1;
  } else {
  }
  if ((unsigned int )mapping->v4l2_type == 2U) {
    goto case_2;
  } else {
  }
  if ((unsigned int )mapping->v4l2_type == 3U) {
    goto case_3;
  } else {
  }
  goto switch_default;
  case_1: ;
  if ((unsigned int )*((unsigned char *)ctrl + 49UL) == 0U) {
    {
    ret = uvc_ctrl_populate_cache(chain, ctrl);
    }
    if (ret < 0) {
      return (ret);
    } else {
    }
  } else {
  }
  {
  tmp = uvc_ctrl_data(ctrl, 2);
  min = (*(mapping->get))(mapping, 130, (__u8 const *)tmp);
  tmp___0 = uvc_ctrl_data(ctrl, 3);
  max = (*(mapping->get))(mapping, 131, (__u8 const *)tmp___0);
  tmp___1 = uvc_ctrl_data(ctrl, 4);
  tmp___2 = (*(mapping->get))(mapping, 132, (__u8 const *)tmp___1);
  step = (u32 )tmp___2;
  }
  if (step == 0U) {
    step = 1U;
  } else {
  }
  xctrl->__annonCompField72.value = (__s32 )((unsigned int )min + (((unsigned int )(xctrl->__annonCompField72.value - min) + step / 2U) / step) * step);
  if (mapping->data_type == 1U) {
    __val = xctrl->__annonCompField72.value;
    __min = min;
    __max = max;
    __val = __min > __val ? __min : __val;
    xctrl->__annonCompField72.value = __max < __val ? __max : __val;
  } else {
    __val___0 = (u32 )xctrl->__annonCompField72.value;
    __min___0 = (u32 )min;
    __max___0 = (u32 )max;
    __val___0 = __min___0 > __val___0 ? __min___0 : __val___0;
    xctrl->__annonCompField72.value = (__s32 )(__max___0 < __val___0 ? __max___0 : __val___0);
  }
  value = xctrl->__annonCompField72.value;
  goto ldv_35087;
  case_2:
  __val___1 = xctrl->__annonCompField72.value;
  __min___1 = 0;
  __max___1 = 1;
  __val___1 = __min___1 > __val___1 ? __min___1 : __val___1;
  xctrl->__annonCompField72.value = __max___1 < __val___1 ? __max___1 : __val___1;
  value = xctrl->__annonCompField72.value;
  goto ldv_35087;
  case_3: ;
  if (xctrl->__annonCompField72.value < 0 || (__u32 )xctrl->__annonCompField72.value >= mapping->menu_count) {
    return (-34);
  } else {
  }
  value = (s32 )(mapping->menu_info + (unsigned long )xctrl->__annonCompField72.value)->value;
  if (mapping->data_type == 5U && (ctrl->info.flags & 16U) != 0U) {
    if ((unsigned int )*((unsigned char *)ctrl + 49UL) == 0U) {
      {
      ret = uvc_ctrl_populate_cache(chain, ctrl);
      }
      if (ret < 0) {
        return (ret);
      } else {
      }
    } else {
    }
    {
    tmp___3 = uvc_ctrl_data(ctrl, 4);
    tmp___4 = (*(mapping->get))(mapping, 132, (__u8 const *)tmp___3);
    step = (u32 )tmp___4;
    }
    if ((step & (u32 )value) == 0U) {
      return (-22);
    } else {
    }
  } else {
  }
  goto ldv_35087;
  switch_default:
  value = xctrl->__annonCompField72.value;
  goto ldv_35087;
  switch_break: ;
  }
  ldv_35087: ;
  if ((unsigned int )*((unsigned char *)ctrl + 49UL) == 0U && (int )ctrl->info.size * 8 != (int )mapping->size) {
    if ((ctrl->info.flags & 2U) == 0U) {
      {
      tmp___5 = uvc_ctrl_data(ctrl, 0);
      memset((void *)tmp___5, 0, (size_t )ctrl->info.size);
      }
    } else {
      {
      tmp___6 = uvc_ctrl_data(ctrl, 0);
      ret = uvc_query_ctrl(chain->dev, 129, (int )(ctrl->entity)->id, (int )((__u8 )(chain->dev)->intfnum),
                           (int )ctrl->info.selector, (void *)tmp___6, (int )ctrl->info.size);
      }
      if (ret < 0) {
        return (ret);
      } else {
      }
    }
    ctrl->loaded = 1U;
  } else {
  }
  if ((unsigned int )*((unsigned char *)ctrl + 49UL) == 0U) {
    {
    tmp___7 = uvc_ctrl_data(ctrl, 0);
    tmp___8 = uvc_ctrl_data(ctrl, 1);
    memcpy((void *)tmp___8, (void const *)tmp___7, (size_t )ctrl->info.size);
    }
  } else {
  }
  {
  tmp___9 = uvc_ctrl_data(ctrl, 0);
  (*(mapping->set))(mapping, value, tmp___9);
  ctrl->dirty = 1U;
  ctrl->modified = 1U;
  }
  return (0);
}
}
static void uvc_ctrl_fixup_xu_info(struct uvc_device *dev , struct uvc_control const *ctrl ,
                                   struct uvc_control_info *info )
{
  struct uvc_ctrl_fixup fixups[3U] ;
  unsigned int i ;
  int tmp ;
  {
  fixups[0].id.match_flags = 3U;
  fixups[0].id.idVendor = 1133U;
  fixups[0].id.idProduct = 2242U;
  fixups[0].id.bcdDevice_lo = (unsigned short)0;
  fixups[0].id.bcdDevice_hi = (unsigned short)0;
  fixups[0].id.bDeviceClass = (unsigned char)0;
  fixups[0].id.bDeviceSubClass = (unsigned char)0;
  fixups[0].id.bDeviceProtocol = (unsigned char)0;
  fixups[0].id.bInterfaceClass = (unsigned char)0;
  fixups[0].id.bInterfaceSubClass = (unsigned char)0;
  fixups[0].id.bInterfaceProtocol = (unsigned char)0;
  fixups[0].id.bInterfaceNumber = (unsigned char)0;
  fixups[0].id.driver_info = 0UL;
  fixups[0].entity = 9U;
  fixups[0].selector = 1U;
  fixups[0].flags = 173U;
  fixups[1].id.match_flags = 3U;
  fixups[1].id.idVendor = 1133U;
  fixups[1].id.idProduct = 2252U;
  fixups[1].id.bcdDevice_lo = (unsigned short)0;
  fixups[1].id.bcdDevice_hi = (unsigned short)0;
  fixups[1].id.bDeviceClass = (unsigned char)0;
  fixups[1].id.bDeviceSubClass = (unsigned char)0;
  fixups[1].id.bDeviceProtocol = (unsigned char)0;
  fixups[1].id.bInterfaceClass = (unsigned char)0;
  fixups[1].id.bInterfaceSubClass = (unsigned char)0;
  fixups[1].id.bInterfaceProtocol = (unsigned char)0;
  fixups[1].id.bInterfaceNumber = (unsigned char)0;
  fixups[1].id.driver_info = 0UL;
  fixups[1].entity = 9U;
  fixups[1].selector = 1U;
  fixups[1].flags = 173U;
  fixups[2].id.match_flags = 3U;
  fixups[2].id.idVendor = 1133U;
  fixups[2].id.idProduct = 2452U;
  fixups[2].id.bcdDevice_lo = (unsigned short)0;
  fixups[2].id.bcdDevice_hi = (unsigned short)0;
  fixups[2].id.bDeviceClass = (unsigned char)0;
  fixups[2].id.bDeviceSubClass = (unsigned char)0;
  fixups[2].id.bDeviceProtocol = (unsigned char)0;
  fixups[2].id.bInterfaceClass = (unsigned char)0;
  fixups[2].id.bInterfaceSubClass = (unsigned char)0;
  fixups[2].id.bInterfaceProtocol = (unsigned char)0;
  fixups[2].id.bInterfaceNumber = (unsigned char)0;
  fixups[2].id.driver_info = 0UL;
  fixups[2].entity = 9U;
  fixups[2].selector = 1U;
  fixups[2].flags = 173U;
  i = 0U;
  goto ldv_35111;
  ldv_35110:
  {
  tmp = usb_match_one_id(dev->intf, & fixups[i].id);
  }
  if (tmp == 0) {
    goto ldv_35109;
  } else {
  }
  if ((int )fixups[i].entity == (int )(ctrl->entity)->id && (int )fixups[i].selector == (int )info->selector) {
    info->flags = (__u32 )fixups[i].flags;
    return;
  } else {
  }
  ldv_35109:
  i = i + 1U;
  ldv_35111: ;
  if (i <= 2U) {
    goto ldv_35110;
  } else {
  }
  return;
}
}
static int uvc_ctrl_fill_xu_info(struct uvc_device *dev , struct uvc_control const *ctrl ,
                                 struct uvc_control_info *info )
{
  u8 *data ;
  int ret ;
  void *tmp ;
  {
  {
  tmp = kmalloc(2UL, 208U);
  data = (u8 *)tmp;
  }
  if ((unsigned long )data == (unsigned long )((u8 *)0U)) {
    return (-12);
  } else {
  }
  {
  memcpy((void *)(& info->entity), (void const *)(& (ctrl->entity)->__annonCompField80.extension.guidExtensionCode),
         16UL);
  info->index = ctrl->index;
  info->selector = (unsigned int )((__u8 )ctrl->index) + 1U;
  ret = uvc_query_ctrl(dev, 133, (int )(ctrl->entity)->id, (int )((__u8 )dev->intfnum),
                       (int )info->selector, (void *)data, 2);
  }
  if (ret < 0) {
    if ((uvc_trace_param & 4U) != 0U) {
      {
      printk("\017uvcvideo: GET_LEN failed on control %pUl/%u (%d).\n", (__u8 *)(& info->entity),
             (int )info->selector, ret);
      }
    } else {
    }
    goto done;
  } else {
  }
  {
  info->size = __le16_to_cpup((__le16 const *)data);
  ret = uvc_query_ctrl(dev, 134, (int )(ctrl->entity)->id, (int )((__u8 )dev->intfnum),
                       (int )info->selector, (void *)data, 1);
  }
  if (ret < 0) {
    if ((uvc_trace_param & 4U) != 0U) {
      {
      printk("\017uvcvideo: GET_INFO failed on control %pUl/%u (%d).\n", (__u8 *)(& info->entity),
             (int )info->selector, ret);
      }
    } else {
    }
    goto done;
  } else {
  }
  {
  info->flags = (__u32 )((((int )*data & 1 ? 62 : 60) | (((int )*data & 2) != 0)) | (((int )*data & 8) != 0 ? 128 : 0));
  uvc_ctrl_fixup_xu_info(dev, ctrl, info);
  }
  if ((uvc_trace_param & 4U) != 0U) {
    {
    printk("\017uvcvideo: XU control %pUl/%u queried: len %u, flags { get %u set %u auto %u }.\n",
           (__u8 *)(& info->entity), (int )info->selector, (int )info->size, (info->flags & 2U) != 0U,
           (int )info->flags & 1, (info->flags & 128U) != 0U);
    }
  } else {
  }
  done:
  {
  kfree((void const *)data);
  }
  return (ret);
}
}
static int uvc_ctrl_add_info(struct uvc_device *dev , struct uvc_control *ctrl , struct uvc_control_info const *info ) ;
static int uvc_ctrl_init_xu_ctrl(struct uvc_device *dev , struct uvc_control *ctrl )
{
  struct uvc_control_info info ;
  int ret ;
  {
  if ((unsigned int )*((unsigned char *)ctrl + 49UL) != 0U) {
    return (0);
  } else {
  }
  {
  ret = uvc_ctrl_fill_xu_info(dev, (struct uvc_control const *)ctrl, & info);
  }
  if (ret < 0) {
    return (ret);
  } else {
  }
  {
  ret = uvc_ctrl_add_info(dev, ctrl, (struct uvc_control_info const *)(& info));
  }
  if (ret < 0) {
    if ((uvc_trace_param & 4U) != 0U) {
      {
      printk("\017uvcvideo: Failed to initialize control %pUl/%u on device %s entity %u\n",
             (__u8 *)(& info.entity), (int )info.selector, (char *)(& (dev->udev)->devpath),
             (int )(ctrl->entity)->id);
      }
    } else {
    }
  } else {
  }
  return (ret);
}
}
int uvc_xu_ctrl_query(struct uvc_video_chain *chain , struct uvc_xu_control_query *xqry )
{
  struct uvc_entity *entity ;
  struct uvc_control *ctrl ;
  unsigned int i ;
  unsigned int found ;
  __u32 reqflags ;
  __u16 size ;
  __u8 *data ;
  int ret ;
  struct list_head const *__mptr ;
  struct list_head const *__mptr___0 ;
  int tmp ;
  void *tmp___0 ;
  unsigned long tmp___1 ;
  unsigned long tmp___2 ;
  {
  found = 0U;
  data = (__u8 *)0U;
  __mptr = (struct list_head const *)chain->entities.next;
  entity = (struct uvc_entity *)__mptr + 0xfffffffffffffff0UL;
  goto ldv_35149;
  ldv_35148: ;
  if (((int )entity->type & 32767) == 6 && (int )entity->id == (int )xqry->unit) {
    goto ldv_35147;
  } else {
  }
  __mptr___0 = (struct list_head const *)entity->chain.next;
  entity = (struct uvc_entity *)__mptr___0 + 0xfffffffffffffff0UL;
  ldv_35149: ;
  if ((unsigned long )(& entity->chain) != (unsigned long )(& chain->entities)) {
    goto ldv_35148;
  } else {
  }
  ldv_35147: ;
  if ((int )entity->id != (int )xqry->unit) {
    if ((uvc_trace_param & 4U) != 0U) {
      {
      printk("\017uvcvideo: Extension unit %u not found.\n", (int )xqry->unit);
      }
    } else {
    }
    return (-2);
  } else {
  }
  i = 0U;
  goto ldv_35152;
  ldv_35151:
  ctrl = entity->controls + (unsigned long )i;
  if ((int )ctrl->index == (int )xqry->selector + -1) {
    found = 1U;
    goto ldv_35150;
  } else {
  }
  i = i + 1U;
  ldv_35152: ;
  if (i < entity->ncontrols) {
    goto ldv_35151;
  } else {
  }
  ldv_35150: ;
  if (found == 0U) {
    if ((uvc_trace_param & 4U) != 0U) {
      {
      printk("\017uvcvideo: Control %pUl/%u not found.\n", (__u8 *)(& entity->__annonCompField80.extension.guidExtensionCode),
             (int )xqry->selector);
      }
    } else {
    }
    return (-2);
  } else {
  }
  {
  tmp = mutex_lock_interruptible_nested(& chain->ctrl_mutex, 0U);
  }
  if (tmp != 0) {
    return (-512);
  } else {
  }
  {
  ret = uvc_ctrl_init_xu_ctrl(chain->dev, ctrl);
  }
  if (ret < 0) {
    ret = -2;
    goto done;
  } else {
  }
  reqflags = 0U;
  size = ctrl->info.size;
  {
  if ((int )xqry->query == 129) {
    goto case_129;
  } else {
  }
  if ((int )xqry->query == 130) {
    goto case_130;
  } else {
  }
  if ((int )xqry->query == 131) {
    goto case_131;
  } else {
  }
  if ((int )xqry->query == 135) {
    goto case_135;
  } else {
  }
  if ((int )xqry->query == 132) {
    goto case_132;
  } else {
  }
  if ((int )xqry->query == 1) {
    goto case_1;
  } else {
  }
  if ((int )xqry->query == 133) {
    goto case_133;
  } else {
  }
  if ((int )xqry->query == 134) {
    goto case_134;
  } else {
  }
  goto switch_default;
  case_129:
  reqflags = 2U;
  goto ldv_35155;
  case_130:
  reqflags = 4U;
  goto ldv_35155;
  case_131:
  reqflags = 8U;
  goto ldv_35155;
  case_135:
  reqflags = 32U;
  goto ldv_35155;
  case_132:
  reqflags = 16U;
  goto ldv_35155;
  case_1:
  reqflags = 1U;
  goto ldv_35155;
  case_133:
  size = 2U;
  goto ldv_35155;
  case_134:
  size = 1U;
  goto ldv_35155;
  switch_default:
  ret = -22;
  goto done;
  switch_break: ;
  }
  ldv_35155: ;
  if ((int )size != (int )xqry->size) {
    ret = -105;
    goto done;
  } else {
  }
  if (reqflags != 0U && (ctrl->info.flags & reqflags) == 0U) {
    ret = -56;
    goto done;
  } else {
  }
  {
  tmp___0 = kmalloc((size_t )size, 208U);
  data = (__u8 *)tmp___0;
  }
  if ((unsigned long )data == (unsigned long )((__u8 *)0U)) {
    ret = -12;
    goto done;
  } else {
  }
  if ((unsigned int )xqry->query == 1U) {
    {
    tmp___1 = copy_from_user((void *)data, (void const *)xqry->data, (unsigned long )size);
    }
    if (tmp___1 != 0UL) {
      ret = -14;
      goto done;
    } else {
    }
  } else {
  }
  {
  ret = uvc_query_ctrl(chain->dev, (int )xqry->query, (int )xqry->unit, (int )((__u8 )(chain->dev)->intfnum),
                       (int )xqry->selector, (void *)data, (int )size);
  }
  if (ret < 0) {
    goto done;
  } else {
  }
  if ((unsigned int )xqry->query != 1U) {
    {
    tmp___2 = copy_to_user((void *)xqry->data, (void const *)data, (unsigned long )size);
    }
    if (tmp___2 != 0UL) {
      ret = -14;
    } else {
    }
  } else {
  }
  done:
  {
  kfree((void const *)data);
  mutex_unlock(& chain->ctrl_mutex);
  }
  return (ret);
}
}
int uvc_ctrl_resume_device(struct uvc_device *dev )
{
  struct uvc_control *ctrl ;
  struct uvc_entity *entity ;
  unsigned int i ;
  int ret ;
  struct list_head const *__mptr ;
  struct list_head const *__mptr___0 ;
  {
  __mptr = (struct list_head const *)dev->entities.next;
  entity = (struct uvc_entity *)__mptr;
  goto ldv_35180;
  ldv_35179:
  i = 0U;
  goto ldv_35177;
  ldv_35176:
  ctrl = entity->controls + (unsigned long )i;
  if ((unsigned int )*((unsigned char *)ctrl + 49UL) != 20U || (ctrl->info.flags & 64U) == 0U) {
    goto ldv_35175;
  } else {
  }
  {
  printk("\016restoring control %pUl/%u/%u\n", (__u8 *)(& ctrl->info.entity), (int )ctrl->info.index,
         (int )ctrl->info.selector);
  ctrl->dirty = 1U;
  }
  ldv_35175:
  i = i + 1U;
  ldv_35177: ;
  if (i < entity->ncontrols) {
    goto ldv_35176;
  } else {
  }
  {
  ret = uvc_ctrl_commit_entity(dev, entity, 0);
  }
  if (ret < 0) {
    return (ret);
  } else {
  }
  __mptr___0 = (struct list_head const *)entity->list.next;
  entity = (struct uvc_entity *)__mptr___0;
  ldv_35180: ;
  if ((unsigned long )(& entity->list) != (unsigned long )(& dev->entities)) {
    goto ldv_35179;
  } else {
  }
  return (0);
}
}
static int uvc_ctrl_add_info(struct uvc_device *dev , struct uvc_control *ctrl , struct uvc_control_info const *info )
{
  int ret ;
  void *tmp ;
  {
  {
  ret = 0;
  ctrl->info = *info;
  INIT_LIST_HEAD(& ctrl->info.mappings);
  tmp = kzalloc((size_t )((int )ctrl->info.size * 6 + 1), 208U);
  ctrl->uvc_data = (__u8 *)tmp;
  }
  if ((unsigned long )ctrl->uvc_data == (unsigned long )((__u8 *)0U)) {
    ret = -12;
    goto done;
  } else {
  }
  ctrl->initialized = 1U;
  if ((uvc_trace_param & 4U) != 0U) {
    {
    printk("\017uvcvideo: Added control %pUl/%u to device %s entity %u\n", (__u8 *)(& ctrl->info.entity),
           (int )ctrl->info.selector, (char *)(& (dev->udev)->devpath), (int )(ctrl->entity)->id);
    }
  } else {
  }
  done: ;
  if (ret < 0) {
    {
    kfree((void const *)ctrl->uvc_data);
    }
  } else {
  }
  return (ret);
}
}
static int __uvc_ctrl_add_mapping(struct uvc_device *dev , struct uvc_control *ctrl ,
                                  struct uvc_control_mapping const *mapping )
{
  struct uvc_control_mapping *map ;
  unsigned int size ;
  void *tmp ;
  void *tmp___0 ;
  {
  {
  tmp = kmemdup((void const *)mapping, 144UL, 208U);
  map = (struct uvc_control_mapping *)tmp;
  }
  if ((unsigned long )map == (unsigned long )((struct uvc_control_mapping *)0)) {
    return (-12);
  } else {
  }
  {
  INIT_LIST_HEAD(& map->ev_subs);
  size = (unsigned int )mapping->menu_count * 36U;
  tmp___0 = kmemdup((void const *)mapping->menu_info, (size_t )size, 208U);
  map->menu_info = (struct uvc_menu_info *)tmp___0;
  }
  if ((unsigned long )map->menu_info == (unsigned long )((struct uvc_menu_info *)0)) {
    {
    kfree((void const *)map);
    }
    return (-12);
  } else {
  }
  if ((unsigned long )map->get == (unsigned long )((__s32 (*)(struct uvc_control_mapping * ,
                                                              __u8 , __u8 const * ))0)) {
    map->get = & uvc_get_le_value;
  } else {
  }
  if ((unsigned long )map->set == (unsigned long )((void (*)(struct uvc_control_mapping * ,
                                                             __s32 , __u8 * ))0)) {
    map->set = & uvc_set_le_value;
  } else {
  }
  {
  list_add_tail(& map->list, & ctrl->info.mappings);
  }
  if ((uvc_trace_param & 4U) != 0U) {
    {
    printk("\017uvcvideo: Adding mapping \'%s\' to control %pUl/%u.\n", (__u8 *)(& map->name),
           (__u8 *)(& ctrl->info.entity), (int )ctrl->info.selector);
    }
  } else {
  }
  return (0);
}
}
int uvc_ctrl_add_mapping(struct uvc_video_chain *chain , struct uvc_control_mapping const *mapping )
{
  struct uvc_device *dev ;
  struct uvc_control_mapping *map ;
  struct uvc_entity *entity ;
  struct uvc_control *ctrl ;
  int found ;
  int ret ;
  struct list_head const *__mptr ;
  unsigned int i ;
  int tmp ;
  struct list_head const *__mptr___0 ;
  int tmp___0 ;
  struct list_head const *__mptr___1 ;
  struct list_head const *__mptr___2 ;
  int tmp___1 ;
  {
  dev = chain->dev;
  found = 0;
  if (((unsigned int )mapping->id & 4026531840U) != 0U) {
    if ((uvc_trace_param & 4U) != 0U) {
      {
      printk("\017uvcvideo: Can\'t add mapping \'%s\', control id 0x%08x is invalid.\n",
             (__u8 const *)(& mapping->name), mapping->id);
      }
    } else {
    }
    return (-22);
  } else {
  }
  __mptr = (struct list_head const *)chain->entities.next;
  entity = (struct uvc_entity *)__mptr + 0xfffffffffffffff0UL;
  goto ldv_35217;
  ldv_35216: ;
  if (((int )entity->type & 32767) != 6) {
    goto ldv_35211;
  } else {
    {
    tmp = uvc_entity_match_guid((struct uvc_entity const *)entity, (__u8 const *)(& mapping->entity));
    }
    if (tmp == 0) {
      goto ldv_35211;
    } else {
    }
  }
  i = 0U;
  goto ldv_35214;
  ldv_35213:
  ctrl = entity->controls + (unsigned long )i;
  if ((int )ctrl->index == (int )mapping->selector + -1) {
    found = 1;
    goto ldv_35212;
  } else {
  }
  i = i + 1U;
  ldv_35214: ;
  if (i < entity->ncontrols) {
    goto ldv_35213;
  } else {
  }
  ldv_35212: ;
  if (found != 0) {
    goto ldv_35215;
  } else {
  }
  ldv_35211:
  __mptr___0 = (struct list_head const *)entity->chain.next;
  entity = (struct uvc_entity *)__mptr___0 + 0xfffffffffffffff0UL;
  ldv_35217: ;
  if ((unsigned long )(& entity->chain) != (unsigned long )(& chain->entities)) {
    goto ldv_35216;
  } else {
  }
  ldv_35215: ;
  if (found == 0) {
    return (-2);
  } else {
  }
  {
  tmp___0 = mutex_lock_interruptible_nested(& chain->ctrl_mutex, 0U);
  }
  if (tmp___0 != 0) {
    return (-512);
  } else {
  }
  {
  ret = uvc_ctrl_init_xu_ctrl(dev, ctrl);
  }
  if (ret < 0) {
    ret = -2;
    goto done;
  } else {
  }
  __mptr___1 = (struct list_head const *)ctrl->info.mappings.next;
  map = (struct uvc_control_mapping *)__mptr___1;
  goto ldv_35224;
  ldv_35223: ;
  if ((unsigned int )mapping->id == map->id) {
    if ((uvc_trace_param & 4U) != 0U) {
      {
      printk("\017uvcvideo: Can\'t add mapping \'%s\', control id 0x%08x already exists.\n",
             (__u8 const *)(& mapping->name), mapping->id);
      }
    } else {
    }
    ret = -17;
    goto done;
  } else {
  }
  __mptr___2 = (struct list_head const *)map->list.next;
  map = (struct uvc_control_mapping *)__mptr___2;
  ldv_35224: ;
  if ((unsigned long )(& map->list) != (unsigned long )(& ctrl->info.mappings)) {
    goto ldv_35223;
  } else {
  }
  {
  tmp___1 = atomic_add_return(1, & dev->nmappings);
  }
  if (tmp___1 > 1024) {
    {
    atomic_dec(& dev->nmappings);
    }
    if ((uvc_trace_param & 4U) != 0U) {
      {
      printk("\017uvcvideo: Can\'t add mapping \'%s\', maximum mappings count (%u) exceeded.\n",
             (__u8 const *)(& mapping->name), 1024);
      }
    } else {
    }
    ret = -12;
    goto done;
  } else {
  }
  {
  ret = __uvc_ctrl_add_mapping(dev, ctrl, mapping);
  }
  if (ret < 0) {
    {
    atomic_dec(& dev->nmappings);
    }
  } else {
  }
  done:
  {
  mutex_unlock(& chain->ctrl_mutex);
  }
  return (ret);
}
}
static void uvc_ctrl_prune_entity(struct uvc_device *dev , struct uvc_entity *entity )
{
  struct uvc_ctrl_blacklist processing_blacklist[3U] ;
  struct uvc_ctrl_blacklist camera_blacklist[1U] ;
  struct uvc_ctrl_blacklist const *blacklist ;
  unsigned int size ;
  unsigned int count ;
  unsigned int i ;
  u8 *controls ;
  int tmp ;
  int tmp___0 ;
  {
  processing_blacklist[0].id.match_flags = 3U;
  processing_blacklist[0].id.idVendor = 5075U;
  processing_blacklist[0].id.idProduct = 20635U;
  processing_blacklist[0].id.bcdDevice_lo = (unsigned short)0;
  processing_blacklist[0].id.bcdDevice_hi = (unsigned short)0;
  processing_blacklist[0].id.bDeviceClass = (unsigned char)0;
  processing_blacklist[0].id.bDeviceSubClass = (unsigned char)0;
  processing_blacklist[0].id.bDeviceProtocol = (unsigned char)0;
  processing_blacklist[0].id.bInterfaceClass = (unsigned char)0;
  processing_blacklist[0].id.bInterfaceSubClass = (unsigned char)0;
  processing_blacklist[0].id.bInterfaceProtocol = (unsigned char)0;
  processing_blacklist[0].id.bInterfaceNumber = (unsigned char)0;
  processing_blacklist[0].id.driver_info = 0UL;
  processing_blacklist[0].index = 9U;
  processing_blacklist[1].id.match_flags = 3U;
  processing_blacklist[1].id.idVendor = 7247U;
  processing_blacklist[1].id.idProduct = 12288U;
  processing_blacklist[1].id.bcdDevice_lo = (unsigned short)0;
  processing_blacklist[1].id.bcdDevice_hi = (unsigned short)0;
  processing_blacklist[1].id.bDeviceClass = (unsigned char)0;
  processing_blacklist[1].id.bDeviceSubClass = (unsigned char)0;
  processing_blacklist[1].id.bDeviceProtocol = (unsigned char)0;
  processing_blacklist[1].id.bInterfaceClass = (unsigned char)0;
  processing_blacklist[1].id.bInterfaceSubClass = (unsigned char)0;
  processing_blacklist[1].id.bInterfaceProtocol = (unsigned char)0;
  processing_blacklist[1].id.bInterfaceNumber = (unsigned char)0;
  processing_blacklist[1].id.driver_info = 0UL;
  processing_blacklist[1].index = 6U;
  processing_blacklist[2].id.match_flags = 3U;
  processing_blacklist[2].id.idVendor = 22918U;
  processing_blacklist[2].id.idProduct = 577U;
  processing_blacklist[2].id.bcdDevice_lo = (unsigned short)0;
  processing_blacklist[2].id.bcdDevice_hi = (unsigned short)0;
  processing_blacklist[2].id.bDeviceClass = (unsigned char)0;
  processing_blacklist[2].id.bDeviceSubClass = (unsigned char)0;
  processing_blacklist[2].id.bDeviceProtocol = (unsigned char)0;
  processing_blacklist[2].id.bInterfaceClass = (unsigned char)0;
  processing_blacklist[2].id.bInterfaceSubClass = (unsigned char)0;
  processing_blacklist[2].id.bInterfaceProtocol = (unsigned char)0;
  processing_blacklist[2].id.bInterfaceNumber = (unsigned char)0;
  processing_blacklist[2].id.driver_info = 0UL;
  processing_blacklist[2].index = 2U;
  camera_blacklist[0].id.match_flags = 3U;
  camera_blacklist[0].id.idVendor = 1784U;
  camera_blacklist[0].id.idProduct = 12293U;
  camera_blacklist[0].id.bcdDevice_lo = (unsigned short)0;
  camera_blacklist[0].id.bcdDevice_hi = (unsigned short)0;
  camera_blacklist[0].id.bDeviceClass = (unsigned char)0;
  camera_blacklist[0].id.bDeviceSubClass = (unsigned char)0;
  camera_blacklist[0].id.bDeviceProtocol = (unsigned char)0;
  camera_blacklist[0].id.bInterfaceClass = (unsigned char)0;
  camera_blacklist[0].id.bInterfaceSubClass = (unsigned char)0;
  camera_blacklist[0].id.bInterfaceProtocol = (unsigned char)0;
  camera_blacklist[0].id.bInterfaceNumber = (unsigned char)0;
  camera_blacklist[0].id.driver_info = 0UL;
  camera_blacklist[0].index = 9U;
  {
  if (((int )entity->type & 32767) == 5) {
    goto case_5;
  } else {
  }
  if (((int )entity->type & 32767) == 513) {
    goto case_513;
  } else {
  }
  goto switch_default;
  case_5:
  blacklist = (struct uvc_ctrl_blacklist const *)(& processing_blacklist);
  count = 3U;
  controls = entity->__annonCompField80.processing.bmControls;
  size = (unsigned int )entity->__annonCompField80.processing.bControlSize;
  goto ldv_35243;
  case_513:
  blacklist = (struct uvc_ctrl_blacklist const *)(& camera_blacklist);
  count = 1U;
  controls = entity->__annonCompField80.camera.bmControls;
  size = (unsigned int )entity->__annonCompField80.camera.bControlSize;
  goto ldv_35243;
  switch_default: ;
  return;
  switch_break: ;
  }
  ldv_35243:
  i = 0U;
  goto ldv_35250;
  ldv_35249:
  {
  tmp = usb_match_one_id(dev->intf, & (blacklist + (unsigned long )i)->id);
  }
  if (tmp == 0) {
    goto ldv_35248;
  } else {
  }
  if ((unsigned int )(blacklist + (unsigned long )i)->index >= size * 8U) {
    goto ldv_35248;
  } else {
    {
    tmp___0 = uvc_test_bit((__u8 const *)controls, (int )(blacklist + (unsigned long )i)->index);
    }
    if (tmp___0 == 0) {
      goto ldv_35248;
    } else {
    }
  }
  if ((uvc_trace_param & 4U) != 0U) {
    {
    printk("\017uvcvideo: %u/%u control is black listed, removing it.\n", (int )entity->id,
           (int )(blacklist + (unsigned long )i)->index);
    }
  } else {
  }
  {
  uvc_clear_bit(controls, (int )(blacklist + (unsigned long )i)->index);
  }
  ldv_35248:
  i = i + 1U;
  ldv_35250: ;
  if (i < count) {
    goto ldv_35249;
  } else {
  }
  return;
}
}
static void uvc_ctrl_init_ctrl(struct uvc_device *dev , struct uvc_control *ctrl )
{
  struct uvc_control_info const *info ;
  struct uvc_control_info const *iend ;
  struct uvc_control_mapping const *mapping ;
  struct uvc_control_mapping const *mend ;
  int tmp ;
  int tmp___0 ;
  {
  info = (struct uvc_control_info const *)(& uvc_ctrls);
  iend = info + 35UL;
  mapping = (struct uvc_control_mapping const *)(& uvc_ctrl_mappings);
  mend = mapping + 27UL;
  if (((int )(ctrl->entity)->type & 32767) == 6) {
    return;
  } else {
  }
  goto ldv_35266;
  ldv_35265:
  {
  tmp = uvc_entity_match_guid((struct uvc_entity const *)ctrl->entity, (__u8 const *)(& info->entity));
  }
  if (tmp != 0 && (int )ctrl->index == (int )((unsigned char )info->index)) {
    {
    uvc_ctrl_add_info(dev, ctrl, info);
    }
    goto ldv_35264;
  } else {
  }
  info = info + 1;
  ldv_35266: ;
  if ((unsigned long )info < (unsigned long )iend) {
    goto ldv_35265;
  } else {
  }
  ldv_35264: ;
  if ((unsigned int )*((unsigned char *)ctrl + 49UL) == 0U) {
    return;
  } else {
  }
  goto ldv_35268;
  ldv_35267:
  {
  tmp___0 = uvc_entity_match_guid((struct uvc_entity const *)ctrl->entity, (__u8 const *)(& mapping->entity));
  }
  if (tmp___0 != 0 && (int )ctrl->info.selector == (int )((unsigned char )mapping->selector)) {
    {
    __uvc_ctrl_add_mapping(dev, ctrl, mapping);
    }
  } else {
  }
  mapping = mapping + 1;
  ldv_35268: ;
  if ((unsigned long )mapping < (unsigned long )mend) {
    goto ldv_35267;
  } else {
  }
  return;
}
}
int uvc_ctrl_init_device(struct uvc_device *dev )
{
  struct uvc_entity *entity ;
  unsigned int i ;
  struct list_head const *__mptr ;
  struct uvc_control *ctrl ;
  unsigned int bControlSize ;
  unsigned int ncontrols ;
  __u8 *bmControls ;
  size_t tmp ;
  void *tmp___0 ;
  int tmp___1 ;
  struct list_head const *__mptr___0 ;
  {
  __mptr = (struct list_head const *)dev->entities.next;
  entity = (struct uvc_entity *)__mptr;
  goto ldv_35289;
  ldv_35288:
  bControlSize = 0U;
  bmControls = (__u8 *)0U;
  if (((int )entity->type & 32767) == 6) {
    bmControls = entity->__annonCompField80.extension.bmControls;
    bControlSize = (unsigned int )entity->__annonCompField80.extension.bControlSize;
  } else
  if (((int )entity->type & 32767) == 5) {
    bmControls = entity->__annonCompField80.processing.bmControls;
    bControlSize = (unsigned int )entity->__annonCompField80.processing.bControlSize;
  } else
  if (((int )entity->type & 32767) == 513) {
    bmControls = entity->__annonCompField80.camera.bmControls;
    bControlSize = (unsigned int )entity->__annonCompField80.camera.bControlSize;
  } else {
  }
  {
  uvc_ctrl_prune_entity(dev, entity);
  tmp = memweight((void const *)bmControls, (size_t )bControlSize);
  ncontrols = (unsigned int )tmp;
  }
  if (ncontrols == 0U) {
    goto ldv_35283;
  } else {
  }
  {
  tmp___0 = kcalloc((size_t )ncontrols, 64UL, 208U);
  entity->controls = (struct uvc_control *)tmp___0;
  }
  if ((unsigned long )entity->controls == (unsigned long )((struct uvc_control *)0)) {
    return (-12);
  } else {
  }
  entity->ncontrols = ncontrols;
  ctrl = entity->controls;
  i = 0U;
  goto ldv_35286;
  ldv_35285:
  {
  tmp___1 = uvc_test_bit((__u8 const *)bmControls, (int )i);
  }
  if (tmp___1 == 0) {
    goto ldv_35284;
  } else {
  }
  {
  ctrl->entity = entity;
  ctrl->index = (__u8 )i;
  uvc_ctrl_init_ctrl(dev, ctrl);
  ctrl = ctrl + 1;
  }
  ldv_35284:
  i = i + 1U;
  ldv_35286: ;
  if (i < bControlSize * 8U) {
    goto ldv_35285;
  } else {
  }
  ldv_35283:
  __mptr___0 = (struct list_head const *)entity->list.next;
  entity = (struct uvc_entity *)__mptr___0;
  ldv_35289: ;
  if ((unsigned long )(& entity->list) != (unsigned long )(& dev->entities)) {
    goto ldv_35288;
  } else {
  }
  return (0);
}
}
static void uvc_ctrl_cleanup_mappings(struct uvc_device *dev , struct uvc_control *ctrl )
{
  struct uvc_control_mapping *mapping ;
  struct uvc_control_mapping *nm ;
  struct list_head const *__mptr ;
  struct list_head const *__mptr___0 ;
  struct list_head const *__mptr___1 ;
  {
  __mptr = (struct list_head const *)ctrl->info.mappings.next;
  mapping = (struct uvc_control_mapping *)__mptr;
  __mptr___0 = (struct list_head const *)mapping->list.next;
  nm = (struct uvc_control_mapping *)__mptr___0;
  goto ldv_35304;
  ldv_35303:
  {
  list_del(& mapping->list);
  kfree((void const *)mapping->menu_info);
  kfree((void const *)mapping);
  mapping = nm;
  __mptr___1 = (struct list_head const *)nm->list.next;
  nm = (struct uvc_control_mapping *)__mptr___1;
  }
  ldv_35304: ;
  if ((unsigned long )(& mapping->list) != (unsigned long )(& ctrl->info.mappings)) {
    goto ldv_35303;
  } else {
  }
  return;
}
}
void uvc_ctrl_cleanup_device(struct uvc_device *dev )
{
  struct uvc_entity *entity ;
  unsigned int i ;
  struct list_head const *__mptr ;
  struct uvc_control *ctrl ;
  struct list_head const *__mptr___0 ;
  {
  __mptr = (struct list_head const *)dev->entities.next;
  entity = (struct uvc_entity *)__mptr;
  goto ldv_35321;
  ldv_35320:
  i = 0U;
  goto ldv_35318;
  ldv_35317:
  ctrl = entity->controls + (unsigned long )i;
  if ((unsigned int )*((unsigned char *)ctrl + 49UL) == 0U) {
    goto ldv_35316;
  } else {
  }
  {
  uvc_ctrl_cleanup_mappings(dev, ctrl);
  kfree((void const *)ctrl->uvc_data);
  }
  ldv_35316:
  i = i + 1U;
  ldv_35318: ;
  if (i < entity->ncontrols) {
    goto ldv_35317;
  } else {
  }
  {
  kfree((void const *)entity->controls);
  __mptr___0 = (struct list_head const *)entity->list.next;
  entity = (struct uvc_entity *)__mptr___0;
  }
  ldv_35321: ;
  if ((unsigned long )(& entity->list) != (unsigned long )(& dev->entities)) {
    goto ldv_35320;
  } else {
  }
  return;
}
}
void ldv_dummy_resourceless_instance_callback_2_3(int (*arg0)(struct uvc_control_mapping * ,
                                                              unsigned char , unsigned char * ) ,
                                                  struct uvc_control_mapping *arg1 ,
                                                  unsigned char arg2 , unsigned char *arg3 )
{
  {
  {
  uvc_ctrl_get_zoom(arg1, (int )arg2, (__u8 const *)arg3);
  }
  return;
}
}
void ldv_dummy_resourceless_instance_callback_2_9(void (*arg0)(struct uvc_control_mapping * ,
                                                               int , unsigned char * ) ,
                                                  struct uvc_control_mapping *arg1 ,
                                                  int arg2 , unsigned char *arg3 )
{
  {
  {
  uvc_ctrl_set_zoom(arg1, arg2, arg3);
  }
  return;
}
}
void ldv_dummy_resourceless_instance_callback_3_10(void (*arg0)(struct v4l2_event * ,
                                                                struct v4l2_event * ) ,
                                                   struct v4l2_event *arg1 , struct v4l2_event *arg2 )
{
  {
  {
  v4l2_ctrl_merge((struct v4l2_event const *)arg1, arg2);
  }
  return;
}
}
void ldv_dummy_resourceless_instance_callback_3_13(void (*arg0)(struct v4l2_event * ,
                                                                struct v4l2_event * ) ,
                                                   struct v4l2_event *arg1 , struct v4l2_event *arg2 )
{
  {
  {
  v4l2_ctrl_replace(arg1, (struct v4l2_event const *)arg2);
  }
  return;
}
}
void ldv_dummy_resourceless_instance_callback_3_3(int (*arg0)(struct v4l2_subscribed_event * ,
                                                              unsigned int ) , struct v4l2_subscribed_event *arg1 ,
                                                  unsigned int arg2 )
{
  {
  {
  uvc_ctrl_add_event(arg1, arg2);
  }
  return;
}
}
void ldv_dummy_resourceless_instance_callback_3_9(void (*arg0)(struct v4l2_subscribed_event * ) ,
                                                  struct v4l2_subscribed_event *arg1 )
{
  {
  {
  uvc_ctrl_del_event(arg1);
  }
  return;
}
}
__inline static void __set_bit(long nr , unsigned long volatile *addr )
{
  {
  __asm__ volatile ("bts %1,%0": "+m" (*((long volatile *)addr)): "Ir" (nr): "memory");
  return;
}
}
__inline static int fls(int x )
{
  int r ;
  {
  __asm__ ("bsrl %1,%0": "=r" (r): "rm" (x), "0" (-1));
  return (r + 1);
}
}
__inline static void usb_fill_int_urb(struct urb *urb , struct usb_device *dev , unsigned int pipe ,
                                      void *transfer_buffer , int buffer_length ,
                                      void (*complete_fn)(struct urb * ) , void *context ,
                                      int interval )
{
  int __val ;
  int __min ;
  int __max ;
  {
  urb->dev = dev;
  urb->pipe = pipe;
  urb->transfer_buffer = transfer_buffer;
  urb->transfer_buffer_length = (u32 )buffer_length;
  urb->complete = complete_fn;
  urb->context = context;
  if ((unsigned int )dev->speed == 3U || (unsigned int )dev->speed == 5U) {
    __val = interval;
    __min = 1;
    __max = 16;
    __val = __min > __val ? __min : __val;
    interval = __max < __val ? __max : __val;
    urb->interval = 1 << (interval + -1);
  } else {
    urb->interval = interval;
  }
  urb->start_frame = -1;
  return;
}
}
struct input_dev *input_allocate_device(void) {
       return kzalloc(sizeof(struct input_dev), 0x10u | 0x40u | 0x80u);
}
extern void input_free_device(struct input_dev * ) ;
extern int input_register_device(struct input_dev * ) ;
extern void input_unregister_device(struct input_dev * ) ;
extern void input_event(struct input_dev * , unsigned int , unsigned int , int ) ;
__inline static void input_report_key(struct input_dev *dev , unsigned int code ,
                                      int value )
{
  {
  {
  input_event(dev, 1U, code, value != 0);
  }
  return;
}
}
__inline static void input_sync(struct input_dev *dev )
{
  {
  {
  input_event(dev, 0U, 0U, 0);
  }
  return;
}
}
__inline static void *kzalloc(size_t size , gfp_t flags ) ;
__inline static void usb_to_input_id(struct usb_device const *dev , struct input_id *id )
{
  {
  id->bustype = 3U;
  id->vendor = dev->descriptor.idVendor;
  id->product = dev->descriptor.idProduct;
  id->version = dev->descriptor.bcdDevice;
  return;
}
}
static int uvc_input_init(struct uvc_device *dev )
{
  struct input_dev *input ;
  int ret ;
  {
  {
  input = input_allocate_device();
  }
  if ((unsigned long )input == (unsigned long )((struct input_dev *)0)) {
    return (-12);
  } else {
  }
  {
  usb_make_path(dev->udev, (char *)(& dev->input_phys), 64UL);
  strlcat((char *)(& dev->input_phys), "/button", 64UL);
  input->name = (char const *)(& dev->name);
  input->phys = (char const *)(& dev->input_phys);
  usb_to_input_id((struct usb_device const *)dev->udev, & input->id);
  input->dev.parent = & (dev->intf)->dev;
  __set_bit(1L, (unsigned long volatile *)(& input->evbit));
  __set_bit(212L, (unsigned long volatile *)(& input->keybit));
  ret = input_register_device(input);
  }
  if (ret < 0) {
    goto error;
  } else {
  }
  dev->input = input;
  return (0);
  error:
  {
  input_free_device(input);
  }
  return (ret);
}
}
static void uvc_input_cleanup(struct uvc_device *dev )
{
  {
  if ((unsigned long )dev->input != (unsigned long )((struct input_dev *)0)) {
    {
    input_unregister_device(dev->input);
    }
  } else {
  }
  return;
}
}
static void uvc_input_report_key(struct uvc_device *dev , unsigned int code , int value )
{
  {
  if ((unsigned long )dev->input != (unsigned long )((struct input_dev *)0)) {
    {
    input_report_key(dev->input, code, value);
    input_sync(dev->input);
    }
  } else {
  }
  return;
}
}
static void uvc_event_streaming(struct uvc_device *dev , __u8 *data , int len )
{
  {
  if (len <= 2) {
    if ((uvc_trace_param & 512U) != 0U) {
      {
      printk("\017uvcvideo: Invalid streaming status event received.\n");
      }
    } else {
    }
    return;
  } else {
  }
  if ((unsigned int )*(data + 2UL) == 0U) {
    if (len <= 3) {
      return;
    } else {
    }
    if ((uvc_trace_param & 512U) != 0U) {
      {
      printk("\017uvcvideo: Button (intf %u) %s len %d\n", (int )*(data + 1UL), (unsigned int )*(data + 3UL) != 0U ? (char *)"pressed" : (char *)"released",
             len);
      }
    } else {
    }
    {
    uvc_input_report_key(dev, 212U, (int )*(data + 3UL));
    }
  } else
  if ((uvc_trace_param & 512U) != 0U) {
    {
    printk("\017uvcvideo: Stream %u error event %02x %02x len %d.\n", (int )*(data + 1UL),
           (int )*(data + 2UL), (int )*(data + 3UL), len);
    }
  } else {
  }
  return;
}
}
static void uvc_event_control(struct uvc_device *dev , __u8 *data , int len )
{
  char *attrs[3U] ;
  {
  attrs[0] = (char *)"value";
  attrs[1] = (char *)"info";
  attrs[2] = (char *)"failure";
  if ((len <= 5 || (unsigned int )*(data + 2UL) != 0U) || (unsigned int )*(data + 4UL) > 2U) {
    if ((uvc_trace_param & 512U) != 0U) {
      {
      printk("\017uvcvideo: Invalid control status event received.\n");
      }
    } else {
    }
    return;
  } else {
  }
  if ((uvc_trace_param & 512U) != 0U) {
    {
    printk("\017uvcvideo: Control %u/%u %s change len %d.\n", (int )*(data + 1UL),
           (int )*(data + 3UL), attrs[(int )*(data + 4UL)], len);
    }
  } else {
  }
  return;
}
}
static void uvc_status_complete(struct urb *urb )
{
  struct uvc_device *dev ;
  int len ;
  int ret ;
  {
  dev = (struct uvc_device *)urb->context;
  {
  if (urb->status == 0) {
    goto case_0;
  } else {
  }
  if (urb->status == -2) {
    goto case_neg_2;
  } else {
  }
  if (urb->status == -104) {
    goto case_neg_104;
  } else {
  }
  if (urb->status == -108) {
    goto case_neg_108;
  } else {
  }
  if (urb->status == -71) {
    goto case_neg_71;
  } else {
  }
  goto switch_default;
  case_0: ;
  goto ldv_34170;
  case_neg_2: ;
  case_neg_104: ;
  case_neg_108: ;
  case_neg_71: ;
  return;
  switch_default:
  {
  printk("\fuvcvideo: Non-zero status (%d) in status completion handler.\n", urb->status);
  }
  return;
  switch_break: ;
  }
  ldv_34170:
  len = (int )urb->actual_length;
  if (len > 0) {
    {
    if (((int )*(dev->status) & 15) == 1) {
      goto case_1;
    } else {
    }
    if (((int )*(dev->status) & 15) == 2) {
      goto case_2;
    } else {
    }
    goto switch_default___0;
    case_1:
    {
    uvc_event_control(dev, dev->status, len);
    }
    goto ldv_34177;
    case_2:
    {
    uvc_event_streaming(dev, dev->status, len);
    }
    goto ldv_34177;
    switch_default___0: ;
    if ((uvc_trace_param & 512U) != 0U) {
      {
      printk("\017uvcvideo: Unknown status event type %u.\n", (int )*(dev->status));
      }
    } else {
    }
    goto ldv_34177;
    switch_break___0: ;
    }
    ldv_34177: ;
  } else {
  }
  {
  urb->interval = (int )(dev->int_ep)->desc.bInterval;
  ret = usb_submit_urb(urb, 32U);
  }
  if (ret < 0) {
    {
    printk("\vuvcvideo: Failed to resubmit status URB (%d).\n", ret);
    }
  } else {
  }
  return;
}
}
int uvc_status_init(struct uvc_device *dev )
{
  struct usb_host_endpoint *ep ;
  unsigned int pipe ;
  int interval ;
  void *tmp ;
  unsigned int tmp___0 ;
  int tmp___1 ;
  {
  ep = dev->int_ep;
  if ((unsigned long )ep == (unsigned long )((struct usb_host_endpoint *)0)) {
    return (0);
  } else {
  }
  {
  uvc_input_init(dev);
  tmp = kzalloc(16UL, 208U);
  dev->status = (__u8 *)tmp;
  }
  if ((unsigned long )dev->status == (unsigned long )((__u8 *)0U)) {
    return (-12);
  } else {
  }
  {
  dev->int_urb = usb_alloc_urb(0, 208U);
  }
  if ((unsigned long )dev->int_urb == (unsigned long )((struct urb *)0)) {
    {
    kfree((void const *)dev->status);
    }
    return (-12);
  } else {
  }
  {
  tmp___0 = __create_pipe(dev->udev, (unsigned int )ep->desc.bEndpointAddress);
  pipe = tmp___0 | 1073741952U;
  interval = (int )ep->desc.bInterval;
  }
  if ((interval > 16 && (unsigned int )(dev->udev)->speed == 3U) && (int )dev->quirks & 1) {
    {
    tmp___1 = fls(interval);
    interval = tmp___1 + -1;
    }
  } else {
  }
  {
  usb_fill_int_urb(dev->int_urb, dev->udev, pipe, (void *)dev->status, 16, & uvc_status_complete,
                   (void *)dev, interval);
  }
  return (0);
}
}
void uvc_status_cleanup(struct uvc_device *dev )
{
  {
  {
  usb_kill_urb(dev->int_urb);
  usb_free_urb(dev->int_urb);
  kfree((void const *)dev->status);
  uvc_input_cleanup(dev);
  }
  return;
}
}
int uvc_status_start(struct uvc_device *dev , gfp_t flags )
{
  int tmp ;
  {
  if ((unsigned long )dev->int_urb == (unsigned long )((struct urb *)0)) {
    return (0);
  } else {
  }
  {
  tmp = usb_submit_urb(dev->int_urb, flags);
  }
  return (tmp);
}
}
void uvc_status_stop(struct uvc_device *dev )
{
  {
  {
  usb_kill_urb(dev->int_urb);
  }
  return;
}
}
static int isight_decode(struct uvc_video_queue *queue , struct uvc_buffer *buf ,
                         __u8 const *data , unsigned int len )
{
  __u8 hdr[12U] ;
  unsigned int maxlen ;
  unsigned int nbytes ;
  __u8 *mem ;
  int is_header ;
  int tmp ;
  int tmp___0 ;
  unsigned int _min1 ;
  unsigned int _min2 ;
  {
  hdr[0] = 17U;
  hdr[1] = 34U;
  hdr[2] = 51U;
  hdr[3] = 68U;
  hdr[4] = 222U;
  hdr[5] = 173U;
  hdr[6] = 190U;
  hdr[7] = 239U;
  hdr[8] = 222U;
  hdr[9] = 173U;
  hdr[10] = 250U;
  hdr[11] = 206U;
  is_header = 0;
  if ((unsigned long )buf == (unsigned long )((struct uvc_buffer *)0)) {
    return (0);
  } else {
  }
  if (len > 13U) {
    {
    tmp = memcmp((void const *)data + 2U, (void const *)(& hdr), 12UL);
    }
    if (tmp == 0) {
      goto _L;
    } else {
      goto _L___0;
    }
  } else
  _L___0:
  if (len > 14U) {
    {
    tmp___0 = memcmp((void const *)data + 3U, (void const *)(& hdr), 12UL);
    }
    if (tmp___0 == 0) {
      _L:
      if ((uvc_trace_param & 128U) != 0U) {
        {
        printk("\017uvcvideo: iSight header found\n");
        }
      } else {
      }
      is_header = 1;
    } else {
    }
  } else {
  }
  if ((unsigned int )buf->state != 2U) {
    if (is_header == 0) {
      if ((uvc_trace_param & 128U) != 0U) {
        {
        printk("\017uvcvideo: Dropping packet (out of sync).\n");
        }
      } else {
      }
      return (0);
    } else {
    }
    buf->state = 2;
  } else {
  }
  if (is_header != 0 && buf->bytesused != 0U) {
    buf->state = 4;
    return (-11);
  } else {
  }
  if (is_header == 0) {
    {
    maxlen = buf->length - buf->bytesused;
    mem = (__u8 *)buf->mem + (unsigned long )buf->bytesused;
    _min1 = len;
    _min2 = maxlen;
    nbytes = _min1 < _min2 ? _min1 : _min2;
    memcpy((void *)mem, (void const *)data, (size_t )nbytes);
    buf->bytesused = buf->bytesused + nbytes;
    }
    if (len > maxlen || buf->bytesused == buf->length) {
      if ((uvc_trace_param & 128U) != 0U) {
        {
        printk("\017uvcvideo: Frame complete (overflow).\n");
        }
      } else {
      }
      buf->state = 4;
    } else {
    }
  } else {
  }
  return (0);
}
}
void uvc_video_decode_isight(struct urb *urb , struct uvc_streaming *stream , struct uvc_buffer *buf )
{
  int ret ;
  int i ;
  {
  i = 0;
  goto ldv_33739;
  ldv_33738: ;
  if (urb->iso_frame_desc[i].status < 0) {
    if ((uvc_trace_param & 128U) != 0U) {
      {
      printk("\017uvcvideo: USB isochronous frame lost (%d).\n", urb->iso_frame_desc[i].status);
      }
    } else {
    }
  } else {
  }
  ldv_33737:
  {
  ret = isight_decode(& stream->queue, buf, (__u8 const *)urb->transfer_buffer + (unsigned long )urb->iso_frame_desc[i].offset,
                      urb->iso_frame_desc[i].actual_length);
  }
  if ((unsigned long )buf == (unsigned long )((struct uvc_buffer *)0)) {
    goto ldv_33736;
  } else {
  }
  if ((unsigned int )buf->state - 4U <= 1U) {
    {
    buf = uvc_queue_next_buffer(& stream->queue, buf);
    }
  } else {
  }
  if (ret == -11) {
    goto ldv_33737;
  } else {
  }
  ldv_33736:
  i = i + 1;
  ldv_33739: ;
  if (i < urb->number_of_packets) {
    goto ldv_33738;
  } else {
  }
  return;
}
}
long ldv_is_err_or_null(void const *ptr ) ;
__inline static long IS_ERR_OR_NULL(void const *ptr ) ;
extern loff_t no_llseek(struct file * , loff_t , int ) ;
extern ssize_t simple_read_from_buffer(void * , size_t , loff_t * , void const * ,
                                       size_t ) ;
extern struct dentry *usb_debug_root ;
extern struct dentry *debugfs_create_file(char const * , umode_t , struct dentry * ,
                                          void * , struct file_operations const * ) ;
extern struct dentry *debugfs_create_dir(char const * , struct dentry * ) ;
extern void debugfs_remove_recursive(struct dentry * ) ;
static int uvc_debugfs_stats_open(struct inode *inode , struct file *file )
{
  struct uvc_streaming *stream ;
  struct uvc_debugfs_buffer *buf ;
  void *tmp ;
  {
  {
  stream = (struct uvc_streaming *)inode->i_private;
  tmp = kmalloc(1032UL, 208U);
  buf = (struct uvc_debugfs_buffer *)tmp;
  }
  if ((unsigned long )buf == (unsigned long )((struct uvc_debugfs_buffer *)0)) {
    return (-12);
  } else {
  }
  {
  buf->count = uvc_video_stats_dump(stream, (char *)(& buf->data), 1024UL);
  file->private_data = (void *)buf;
  }
  return (0);
}
}
static ssize_t uvc_debugfs_stats_read(struct file *file , char *user_buf , size_t nbytes ,
                                      loff_t *ppos )
{
  struct uvc_debugfs_buffer *buf ;
  ssize_t tmp ;
  {
  {
  buf = (struct uvc_debugfs_buffer *)file->private_data;
  tmp = simple_read_from_buffer((void *)user_buf, nbytes, ppos, (void const *)(& buf->data),
                                buf->count);
  }
  return (tmp);
}
}
static int uvc_debugfs_stats_release(struct inode *inode , struct file *file )
{
  {
  {
  kfree((void const *)file->private_data);
  file->private_data = (void *)0;
  }
  return (0);
}
}
static struct file_operations const uvc_debugfs_stats_fops =
     {& __this_module, & no_llseek, & uvc_debugfs_stats_read, 0, 0, 0, 0, 0, 0, 0, 0,
    & uvc_debugfs_stats_open, 0, & uvc_debugfs_stats_release, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0};
static struct dentry *uvc_debugfs_root_dir ;
int uvc_debugfs_init_stream(struct uvc_streaming *stream )
{
  struct usb_device *udev ;
  struct dentry *dent ;
  char dir_name[32U] ;
  long tmp ;
  long tmp___0 ;
  {
  udev = (stream->dev)->udev;
  if ((unsigned long )uvc_debugfs_root_dir == (unsigned long )((struct dentry *)0)) {
    return (-19);
  } else {
  }
  {
  sprintf((char *)(& dir_name), "%u-%u", (udev->bus)->busnum, udev->devnum);
  dent = debugfs_create_dir((char const *)(& dir_name), uvc_debugfs_root_dir);
  tmp = IS_ERR_OR_NULL((void const *)dent);
  }
  if (tmp != 0L) {
    {
    printk("\016uvcvideo: Unable to create debugfs %s directory.\n", (char *)(& dir_name));
    }
    return (-19);
  } else {
  }
  {
  stream->debugfs_dir = dent;
  dent = debugfs_create_file("stats", 292, stream->debugfs_dir, (void *)stream, & uvc_debugfs_stats_fops);
  tmp___0 = IS_ERR_OR_NULL((void const *)dent);
  }
  if (tmp___0 != 0L) {
    {
    printk("\016uvcvideo: Unable to create debugfs stats file.\n");
    uvc_debugfs_cleanup_stream(stream);
    }
    return (-19);
  } else {
  }
  return (0);
}
}
void uvc_debugfs_cleanup_stream(struct uvc_streaming *stream )
{
  {
  if ((unsigned long )stream->debugfs_dir == (unsigned long )((struct dentry *)0)) {
    return;
  } else {
  }
  {
  debugfs_remove_recursive(stream->debugfs_dir);
  stream->debugfs_dir = (struct dentry *)0;
  }
  return;
}
}
int uvc_debugfs_init(void)
{
  struct dentry *dir ;
  long tmp ;
  {
  {
  dir = debugfs_create_dir("uvcvideo", usb_debug_root);
  tmp = IS_ERR_OR_NULL((void const *)dir);
  }
  if (tmp != 0L) {
    {
    printk("\016uvcvideo: Unable to create debugfs directory\n");
    }
    return (-61);
  } else {
  }
  uvc_debugfs_root_dir = dir;
  return (0);
}
}
void uvc_debugfs_cleanup(void)
{
  {
  if ((unsigned long )uvc_debugfs_root_dir != (unsigned long )((struct dentry *)0)) {
    {
    debugfs_remove_recursive(uvc_debugfs_root_dir);
    }
  } else {
  }
  return;
}
}
int ldv_file_operations_instance_probe_0_12(int (*arg0)(struct inode * , struct file * ) ,
                                            struct inode *arg1 , struct file *arg2 ) ;
struct ldv_thread ldv_thread_0 ;
void ldv_file_operations_file_operations_instance_0(void *arg0 )
{
  struct file_operations *ldv_0_container_file_operations ;
  char *ldv_0_ldv_param_4_1_default ;
  long long *ldv_0_ldv_param_4_3_default ;
  struct file *ldv_0_resource_file ;
  struct inode *ldv_0_resource_inode ;
  int ldv_0_ret_default ;
  unsigned long ldv_0_size_cnt_write_size ;
  void *tmp ;
  void *tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___3 ;
  int tmp___4 ;
  void *tmp___5 ;
  void *tmp___6 ;
  {
  {
  ldv_0_ret_default = 1;
  tmp = ldv_xmalloc(520UL);
  ldv_0_resource_file = (struct file *)tmp;
  tmp___0 = ldv_xmalloc(1032UL);
  ldv_0_resource_inode = (struct inode *)tmp___0;
  tmp___1 = ldv_undef_int();
  ldv_0_size_cnt_write_size = (unsigned long )tmp___1;
  }
  goto ldv_main_0;
  return;
  ldv_main_0:
  {
  tmp___3 = ldv_undef_int();
  }
  if (tmp___3 != 0) {
    {
    ldv_0_ret_default = ldv_file_operations_instance_probe_0_12(ldv_0_container_file_operations->open,
                                                                ldv_0_resource_inode,
                                                                ldv_0_resource_file);
    ldv_0_ret_default = ldv_filter_err_code(ldv_0_ret_default);
    tmp___2 = ldv_undef_int();
    }
    if (tmp___2 != 0) {
      {
      ldv_assume(ldv_0_ret_default == 0);
      }
      goto ldv_call_0;
    } else {
      {
      ldv_assume(ldv_0_ret_default != 0);
      }
      goto ldv_main_0;
    }
  } else {
    {
    ldv_free((void *)ldv_0_resource_file);
    ldv_free((void *)ldv_0_resource_inode);
    }
    return;
  }
  return;
  ldv_call_0:
  {
  tmp___4 = ldv_undef_int();
  }
  {
  if (tmp___4 == 1) {
    goto case_1;
  } else {
  }
  if (tmp___4 == 2) {
    goto case_2;
  } else {
  }
  if (tmp___4 == 3) {
    goto case_3;
  } else {
  }
  goto switch_default;
  case_1:
  {
  tmp___5 = ldv_xmalloc(1UL);
  ldv_0_ldv_param_4_1_default = (char *)tmp___5;
  tmp___6 = ldv_xmalloc(8UL);
  ldv_0_ldv_param_4_3_default = (long long *)tmp___6;
  ldv_assume(ldv_0_size_cnt_write_size <= 2147479552UL);
  }
  if ((unsigned long )ldv_0_container_file_operations->write != (unsigned long )((ssize_t (*)(struct file * ,
                                                                                              char const * ,
                                                                                              size_t ,
                                                                                              loff_t * ))0)) {
    {
    ldv_file_operations_instance_write_0_4((long (*)(struct file * , char * , unsigned long ,
                                                     long long * ))ldv_0_container_file_operations->write,
                                           ldv_0_resource_file, ldv_0_ldv_param_4_1_default,
                                           ldv_0_size_cnt_write_size, ldv_0_ldv_param_4_3_default);
    }
  } else {
  }
  {
  ldv_free((void *)ldv_0_ldv_param_4_1_default);
  ldv_free((void *)ldv_0_ldv_param_4_3_default);
  }
  goto ldv_call_0;
  case_2: ;
  goto ldv_call_0;
  goto ldv_call_0;
  case_3: ;
  goto ldv_main_0;
  switch_default:
  {
  ldv_stop();
  }
  switch_break: ;
  }
  return;
}
}
int ldv_file_operations_instance_probe_0_12(int (*arg0)(struct inode * , struct file * ) ,
                                            struct inode *arg1 , struct file *arg2 )
{
  int tmp ;
  {
  {
  tmp = uvc_debugfs_stats_open(arg1, arg2);
  }
  return (tmp);
}
}
void ldv_io_instance_callback_7_19(long long (*arg0)(struct file * , long long ,
                                                     int ) , struct file *arg1 ,
                                   long long arg2 , int arg3 )
{
  {
  {
  no_llseek(arg1, arg2, arg3);
  }
  return;
}
}
__inline static long IS_ERR_OR_NULL(void const *ptr )
{
  long tmp ;
  {
  {
  tmp = ldv_is_err_or_null(ptr);
  }
  return (tmp);
}
}
extern int media_entity_init(struct media_entity * , u16 , struct media_pad * , u16 ) ;
extern void media_entity_cleanup(struct media_entity * ) ;
extern int media_entity_create_link(struct media_entity * , u16 , struct media_entity * ,
                                    u16 , u32 ) ;
extern void v4l2_subdev_init(struct v4l2_subdev * , struct v4l2_subdev_ops const * ) ;
extern int v4l2_device_register_subdev(struct v4l2_device * , struct v4l2_subdev * ) ;
static int uvc_mc_register_entity(struct uvc_video_chain *chain , struct uvc_entity *entity )
{
  u32 flags ;
  struct media_entity *sink ;
  unsigned int i ;
  int ret ;
  struct media_entity *source ;
  struct uvc_entity *remote ;
  u8 remote_pad ;
  int tmp ;
  {
  flags = 3U;
  sink = ((int )entity->type & 32767) == 257 ? ((unsigned long )entity->vdev != (unsigned long )((struct video_device *)0) ? & (entity->vdev)->entity : (struct media_entity *)0) : & entity->subdev.entity;
  if ((unsigned long )sink == (unsigned long )((struct media_entity *)0)) {
    return (0);
  } else {
  }
  i = 0U;
  goto ldv_33728;
  ldv_33727: ;
  if (((entity->pads + (unsigned long )i)->flags & 1UL) == 0UL) {
    goto ldv_33726;
  } else {
  }
  {
  remote = uvc_entity_by_id(chain->dev, (int )*(entity->baSourceID + (unsigned long )i));
  }
  if ((unsigned long )remote == (unsigned long )((struct uvc_entity *)0)) {
    return (-22);
  } else {
  }
  source = ((int )remote->type & 32767) == 257 ? ((unsigned long )remote->vdev != (unsigned long )((struct video_device *)0) ? & (remote->vdev)->entity : (struct media_entity *)0) : & remote->subdev.entity;
  if ((unsigned long )source == (unsigned long )((struct media_entity *)0)) {
    goto ldv_33726;
  } else {
  }
  {
  remote_pad = (unsigned int )((u8 )remote->num_pads) - 1U;
  ret = media_entity_create_link(source, (int )remote_pad, sink, (int )((u16 )i),
                                 flags);
  }
  if (ret < 0) {
    return (ret);
  } else {
  }
  ldv_33726:
  i = i + 1U;
  ldv_33728: ;
  if (i < entity->num_pads) {
    goto ldv_33727;
  } else {
  }
  if (((int )entity->type & 32767) == 257) {
    return (0);
  } else {
  }
  {
  tmp = v4l2_device_register_subdev(& (chain->dev)->vdev, & entity->subdev);
  }
  return (tmp);
}
}
static struct v4l2_subdev_ops uvc_subdev_ops =
     {0, 0, 0, 0, 0, 0, 0, 0};
void uvc_mc_cleanup_entity(struct uvc_entity *entity )
{
  {
  if (((int )entity->type & 32767) != 257) {
    {
    media_entity_cleanup(& entity->subdev.entity);
    }
  } else
  if ((unsigned long )entity->vdev != (unsigned long )((struct video_device *)0)) {
    {
    media_entity_cleanup(& (entity->vdev)->entity);
    }
  } else {
  }
  return;
}
}
static int uvc_mc_init_entity(struct uvc_entity *entity )
{
  int ret ;
  {
  if (((int )entity->type & 32767) != 257) {
    {
    v4l2_subdev_init(& entity->subdev, (struct v4l2_subdev_ops const *)(& uvc_subdev_ops));
    strlcpy((char *)(& entity->subdev.name), (char const *)(& entity->name), 32UL);
    ret = media_entity_init(& entity->subdev.entity, (int )((u16 )entity->num_pads),
                            entity->pads, 0);
    }
  } else
  if ((unsigned long )entity->vdev != (unsigned long )((struct video_device *)0)) {
    {
    ret = media_entity_init(& (entity->vdev)->entity, (int )((u16 )entity->num_pads),
                            entity->pads, 0);
    }
    if ((int )entity->flags & 1) {
      (entity->vdev)->entity.flags = (entity->vdev)->entity.flags | 1UL;
    } else {
    }
  } else {
    ret = 0;
  }
  return (ret);
}
}
int uvc_mc_register_entities(struct uvc_video_chain *chain )
{
  struct uvc_entity *entity ;
  int ret ;
  struct list_head const *__mptr ;
  struct list_head const *__mptr___0 ;
  struct list_head const *__mptr___1 ;
  struct list_head const *__mptr___2 ;
  {
  __mptr = (struct list_head const *)chain->entities.next;
  entity = (struct uvc_entity *)__mptr + 0xfffffffffffffff0UL;
  goto ldv_33748;
  ldv_33747:
  {
  ret = uvc_mc_init_entity(entity);
  }
  if (ret < 0) {
    {
    printk("\016uvcvideo: Failed to initialize entity for entity %u\n", (int )entity->id);
    }
    return (ret);
  } else {
  }
  __mptr___0 = (struct list_head const *)entity->chain.next;
  entity = (struct uvc_entity *)__mptr___0 + 0xfffffffffffffff0UL;
  ldv_33748: ;
  if ((unsigned long )(& entity->chain) != (unsigned long )(& chain->entities)) {
    goto ldv_33747;
  } else {
  }
  __mptr___1 = (struct list_head const *)chain->entities.next;
  entity = (struct uvc_entity *)__mptr___1 + 0xfffffffffffffff0UL;
  goto ldv_33755;
  ldv_33754:
  {
  ret = uvc_mc_register_entity(chain, entity);
  }
  if (ret < 0) {
    {
    printk("\016uvcvideo: Failed to register entity for entity %u\n", (int )entity->id);
    }
    return (ret);
  } else {
  }
  __mptr___2 = (struct list_head const *)entity->chain.next;
  entity = (struct uvc_entity *)__mptr___2 + 0xfffffffffffffff0UL;
  ldv_33755: ;
  if ((unsigned long )(& entity->chain) != (unsigned long )(& chain->entities)) {
    goto ldv_33754;
  } else {
  }
  return (0);
}
}
void ldv_atomic_add(int i , atomic_t *v )
{
  {
  v->counter = v->counter + i;
  return;
}
}
void ldv_atomic_sub(int i , atomic_t *v )
{
  {
  v->counter = v->counter - i;
  return;
}
}
int ldv_atomic_sub_and_test(int i , atomic_t *v )
{
  {
  v->counter = v->counter - i;
  if (v->counter != 0) {
    return (0);
  } else {
  }
  return (1);
}
}
void ldv_atomic_inc(atomic_t *v )
{
  {
  v->counter = v->counter + 1;
  return;
}
}
void ldv_atomic_dec(atomic_t *v )
{
  {
  v->counter = v->counter - 1;
  return;
}
}
int ldv_atomic_dec_and_test(atomic_t *v )
{
  {
  v->counter = v->counter - 1;
  if (v->counter != 0) {
    return (0);
  } else {
  }
  return (1);
}
}
int ldv_atomic_inc_and_test(atomic_t *v )
{
  {
  v->counter = v->counter + 1;
  if (v->counter != 0) {
    return (0);
  } else {
  }
  return (1);
}
}
int ldv_atomic_add_return(int i , atomic_t *v )
{
  {
  v->counter = v->counter + i;
  return (v->counter);
}
}
int ldv_atomic_add_negative(int i , atomic_t *v )
{
  {
  v->counter = v->counter + i;
  return (v->counter < 0);
}
}
int ldv_atomic_inc_short(short *v )
{
  {
  *v = (short )((unsigned int )((unsigned short )*v) + 1U);
  return ((int )*v);
}
}
void *ldv_xzalloc(size_t size ) ;
void *ldv_dev_get_drvdata(struct device const *dev )
{
  {
  if ((unsigned long )dev != (unsigned long )((struct device const *)0) && (unsigned long )dev->p != (unsigned long )((struct device_private * )0)) {
    return ((dev->p)->driver_data);
  } else {
  }
  return ((void *)0);
}
}
int ldv_dev_set_drvdata(struct device *dev , void *data )
{
  void *tmp ;
  {
  {
  tmp = ldv_xzalloc(8UL);
  dev->p = (struct device_private *)tmp;
  (dev->p)->driver_data = data;
  }
  return (0);
}
}
struct spi_master *ldv_spi_alloc_master(struct device *host , unsigned int size )
{
  struct spi_master *master ;
  void *tmp ;
  {
  {
  tmp = ldv_zalloc((unsigned long )size + 2200UL);
  master = (struct spi_master *)tmp;
  }
  if ((unsigned long )master == (unsigned long )((struct spi_master *)0)) {
    return ((struct spi_master *)0);
  } else {
  }
  {
  ldv_dev_set_drvdata(& master->dev, (void *)master + 1U);
  }
  return (master);
}
}
long ldv_is_err(void const *ptr )
{
  {
  return ((unsigned long )ptr > 4294967295UL);
}
}
void *ldv_err_ptr(long error )
{
  {
  return ((void *)(4294967295L - error));
}
}
long ldv_ptr_err(void const *ptr )
{
  {
  return ((long )(4294967295UL - (unsigned long )ptr));
}
}
long ldv_is_err_or_null(void const *ptr )
{
  long tmp ;
  int tmp___0 ;
  {
  if ((unsigned long )ptr == (unsigned long )((void const *)0)) {
    tmp___0 = 1;
  } else {
    {
    tmp = ldv_is_err(ptr);
    }
    if (tmp != 0L) {
      tmp___0 = 1;
    } else {
      tmp___0 = 0;
    }
  }
  return ((long )tmp___0);
}
}
static int ldv_filter_positive_int(int val )
{
  {
  {
  ldv_assume(val <= 0);
  }
  return (val);
}
}
int ldv_post_init(int init_ret_val )
{
  int tmp ;
  {
  {
  tmp = ldv_filter_positive_int(init_ret_val);
  }
  return (tmp);
}
}
int ldv_post_probe(int probe_ret_val )
{
  int tmp ;
  {
  {
  tmp = ldv_filter_positive_int(probe_ret_val);
  }
  return (tmp);
}
}
int ldv_filter_err_code(int ret_val )
{
  int tmp ;
  {
  {
  tmp = ldv_filter_positive_int(ret_val);
  }
  return (tmp);
}
}
extern void ldv_check_alloc_flags(gfp_t ) ;
extern void ldv_after_alloc(void * ) ;
void *ldv_kzalloc(size_t size , gfp_t flags )
{
  void *res ;
  {
  {
  ldv_check_alloc_flags(flags);
  res = ldv_zalloc(size);
  ldv_after_alloc(res);
  }
  return (res);
}
}
void ldv_assert_linux_usb_dev__less_initial_decrement(int expr ) ;
void ldv_assert_linux_usb_dev__more_initial_at_exit(int expr ) ;
void ldv_assert_linux_usb_dev__probe_failed(int expr ) ;
void ldv_assert_linux_usb_dev__unincremented_counter_decrement(int expr ) ;
ldv_map LDV_USB_DEV_REF_COUNTS ;
struct usb_device *ldv_usb_get_dev(struct usb_device *dev )
{
  {
  if ((unsigned long )dev != (unsigned long )((struct usb_device *)0)) {
    LDV_USB_DEV_REF_COUNTS = LDV_USB_DEV_REF_COUNTS != 0 ? LDV_USB_DEV_REF_COUNTS + 1 : 1;
  } else {
  }
  return (dev);
}
}
void ldv_usb_put_dev(struct usb_device *dev )
{
  {
  if ((unsigned long )dev != (unsigned long )((struct usb_device *)0)) {
    {
    ldv_assert_linux_usb_dev__unincremented_counter_decrement(LDV_USB_DEV_REF_COUNTS != 0);
    ldv_assert_linux_usb_dev__less_initial_decrement(LDV_USB_DEV_REF_COUNTS > 0);
    }
    if (LDV_USB_DEV_REF_COUNTS > 1) {
      LDV_USB_DEV_REF_COUNTS = LDV_USB_DEV_REF_COUNTS + -1;
    } else {
      LDV_USB_DEV_REF_COUNTS = 0;
    }
  } else {
  }
  return;
}
}
void ldv_check_return_value_probe(int retval )
{
  {
  if (retval != 0) {
    {
    ldv_assert_linux_usb_dev__probe_failed(LDV_USB_DEV_REF_COUNTS == 0);
    }
  } else {
  }
  return;
}
}
void ldv_initialize(void)
{
  {
  LDV_USB_DEV_REF_COUNTS = 0;
  return;
}
}
void ldv_check_final_state(void)
{
  {
  {
  ldv_assert_linux_usb_dev__more_initial_at_exit(LDV_USB_DEV_REF_COUNTS == 0);
  }
  return;
}
}
extern void ldv_assert(char const * , int ) ;
void ldv__builtin_trap(void) ;
void ldv_assume(int expression )
{
  {
  if (expression == 0) {
    ldv_assume_label: ;
    goto ldv_assume_label;
  } else {
  }
  return;
}
}
void ldv_stop(void)
{
  {
  ldv_stop_label: ;
  goto ldv_stop_label;
}
}
long ldv__builtin_expect(long exp , long c )
{
  {
  return (exp);
}
}
void ldv__builtin_trap(void)
{
  {
  {
  ldv_assert("", 0);
  }
  return;
}
}
extern void *malloc(size_t ) ;
extern void *calloc(size_t , size_t ) ;
extern void free(void * ) ;
void *ldv_malloc(size_t size )
{
  void *res ;
  void *tmp ;
  long tmp___0 ;
  int tmp___1 ;
  {
  {
  tmp___1 = ldv_undef_int();
  }
  if (tmp___1 != 0) {
    {
    tmp = malloc(size);
    res = tmp;
    ldv_assume((unsigned long )res != (unsigned long )((void *)0));
    tmp___0 = ldv_is_err((void const *)res);
    ldv_assume(tmp___0 == 0L);
    }
    return (res);
  } else {
    return ((void *)0);
  }
}
}
void *ldv_calloc(size_t nmemb , size_t size )
{
  void *res ;
  void *tmp ;
  long tmp___0 ;
  int tmp___1 ;
  {
  {
  tmp___1 = ldv_undef_int();
  }
  if (tmp___1 != 0) {
    {
    tmp = calloc(nmemb, size);
    res = tmp;
    ldv_assume((unsigned long )res != (unsigned long )((void *)0));
    tmp___0 = ldv_is_err((void const *)res);
    ldv_assume(tmp___0 == 0L);
    }
    return (res);
  } else {
    return ((void *)0);
  }
}
}
void *ldv_zalloc(size_t size )
{
  void *tmp ;
  {
  {
  tmp = ldv_calloc(1UL, size);
  }
  return (tmp);
}
}
void ldv_free(void *s )
{
  {
  {
  free(s);
  }
  return;
}
}
void *ldv_xmalloc(size_t size )
{
  void *res ;
  void *tmp ;
  long tmp___0 ;
  {
  {
  tmp = malloc(size);
  res = tmp;
  ldv_assume((unsigned long )res != (unsigned long )((void *)0));
  tmp___0 = ldv_is_err((void const *)res);
  ldv_assume(tmp___0 == 0L);
  }
  return (res);
}
}
void *ldv_xzalloc(size_t size )
{
  void *res ;
  void *tmp ;
  long tmp___0 ;
  {
  {
  tmp = calloc(1UL, size);
  res = tmp;
  ldv_assume((unsigned long )res != (unsigned long )((void *)0));
  tmp___0 = ldv_is_err((void const *)res);
  ldv_assume(tmp___0 == 0L);
  }
  return (res);
}
}
unsigned long ldv_undef_ulong(void) ;
int ldv_undef_int_negative(void) ;
int ldv_undef_int_nonpositive(void) ;
extern int __VERIFIER_nondet_int(void) ;
extern unsigned long __VERIFIER_nondet_ulong(void) ;
int ldv_undef_int(void)
{
  int tmp ;
  {
  {
  tmp = __VERIFIER_nondet_int();
  }
  return (tmp);
}
}
unsigned long ldv_undef_ulong(void)
{
  unsigned long tmp ;
  {
  {
  tmp = __VERIFIER_nondet_ulong();
  }
  return (tmp);
}
}
int ldv_undef_int_negative(void)
{
  int ret ;
  int tmp ;
  {
  {
  tmp = ldv_undef_int();
  ret = tmp;
  ldv_assume(ret < 0);
  }
  return (ret);
}
}
int ldv_undef_int_nonpositive(void)
{
  int ret ;
  int tmp ;
  {
  {
  tmp = ldv_undef_int();
  ret = tmp;
  ldv_assume(ret <= 0);
  }
  return (ret);
}
}
int ldv_thread_create(struct ldv_thread *ldv_thread , void (*function)(void * ) ,
                      void *data ) ;
int ldv_thread_create_N(struct ldv_thread_set *ldv_thread_set , void (*function)(void * ) ,
                        void *data ) ;
int ldv_thread_join(struct ldv_thread *ldv_thread , void (*function)(void * ) ) ;
int ldv_thread_join_N(struct ldv_thread_set *ldv_thread_set , void (*function)(void * ) ) ;
int ldv_thread_create(struct ldv_thread *ldv_thread , void (*function)(void * ) ,
                      void *data )
{
  {
  if ((unsigned long )function != (unsigned long )((void (*)(void * ))0)) {
    {
    (*function)(data);
    }
  } else {
  }
  return (0);
}
}
int ldv_thread_create_N(struct ldv_thread_set *ldv_thread_set , void (*function)(void * ) ,
                        void *data )
{
  int i ;
  {
  if ((unsigned long )function != (unsigned long )((void (*)(void * ))0)) {
    i = 0;
    goto ldv_1179;
    ldv_1178:
    {
    (*function)(data);
    i = i + 1;
    }
    ldv_1179: ;
    if (i < ldv_thread_set->number) {
      goto ldv_1178;
    } else {
    }
  } else {
  }
  return (0);
}
}
int ldv_thread_join(struct ldv_thread *ldv_thread , void (*function)(void * ) )
{
  {
  return (0);
}
}
int ldv_thread_join_N(struct ldv_thread_set *ldv_thread_set , void (*function)(void * ) )
{
  {
  return (0);
}
}
extern void abort(void); 
void reach_error(){}
void ldv_assert_linux_usb_dev__less_initial_decrement(int expr )
{
  {
  if (! expr) {
    {
    {reach_error();abort();}
    }
  } else {
  }
  return;
}
}
void ldv_assert_linux_usb_dev__more_initial_at_exit(int expr )
{
  {
  if (! expr) {
    {
    {reach_error();abort();}
    }
  } else {
  }
  return;
}
}
void ldv_assert_linux_usb_dev__probe_failed(int expr )
{
  {
  if (! expr) {
    {
    {reach_error();abort();}
    }
  } else {
  }
  return;
}
}
void ldv_assert_linux_usb_dev__unincremented_counter_decrement(int expr )
{
  {
  if (! expr) {
    {
    {reach_error();abort();}
    }
  } else {
  }
  return;
}
}
unsigned long __VERIFIER_nondet_ulong(void);
unsigned long int __clear_user(void *arg0, unsigned long arg1) {
  return __VERIFIER_nondet_ulong();
}
void __copy_from_user_overflow() {
  return;
}
void __copy_to_user_overflow() {
  return;
}
void __list_add(struct list_head *arg0, struct list_head *arg1, struct list_head *arg2) {
  return;
}
void __mutex_init(struct mutex *arg0, const char *arg1, struct lock_class_key *arg2) {
  return;
}
void __raw_spin_lock_init(raw_spinlock_t *arg0, const char *arg1, struct lock_class_key *arg2) {
  return;
}
int __VERIFIER_nondet_int(void);
int __video_register_device(struct video_device *arg0, int arg1, int arg2, int arg3, struct module *arg4) {
  return __VERIFIER_nondet_int();
}
unsigned long __VERIFIER_nondet_ulong(void);
unsigned long int _copy_from_user(void *arg0, const void *arg1, unsigned int arg2) {
  return __VERIFIER_nondet_ulong();
}
unsigned long __VERIFIER_nondet_ulong(void);
unsigned long int _copy_to_user(void *arg0, const void *arg1, unsigned int arg2) {
  return __VERIFIER_nondet_ulong();
}
unsigned long __VERIFIER_nondet_ulong(void);
unsigned long int _raw_spin_lock_irqsave(raw_spinlock_t *arg0) {
  return __VERIFIER_nondet_ulong();
}
void _raw_spin_unlock_irqrestore(raw_spinlock_t *arg0, unsigned long arg1) {
  return;
}
void *compat_alloc_user_space(unsigned long arg0) {
  return ldv_malloc(0UL);
}
unsigned long __VERIFIER_nondet_ulong(void);
unsigned long int copy_in_user(void *arg0, const void *arg1, unsigned int arg2) {
  return __VERIFIER_nondet_ulong();
}
struct dentry *debugfs_create_dir(const char *arg0, struct dentry *arg1) {
  return ldv_malloc(sizeof(struct dentry));
}
struct dentry *debugfs_create_file(const char *arg0, umode_t arg1, struct dentry *arg2, void *arg3, const struct file_operations *arg4) {
  return ldv_malloc(sizeof(struct dentry));
}
void debugfs_remove_recursive(struct dentry *arg0) {
  return;
}
void getnstimeofday(struct timespec *arg0) {
  return;
}
void input_event(struct input_dev *arg0, unsigned int arg1, unsigned int arg2, int arg3) {
  return;
}
void input_free_device(struct input_dev *arg0) {
  return;
}
int __VERIFIER_nondet_int(void);
int input_register_device(struct input_dev *arg0) {
  return __VERIFIER_nondet_int();
}
void input_unregister_device(struct input_dev *arg0) {
  return;
}
void *kmemdup(const void *arg0, size_t arg1, gfp_t arg2) {
  return ldv_malloc(0UL);
}
void ktime_get_ts(struct timespec *arg0) {
  return;
}
void ldv_after_alloc(void *arg0) {
  return;
}
void ldv_assert(const char *arg0, int arg1) {
  return;
}
void ldv_check_alloc_flags(gfp_t arg0) {
  return;
}
void ldv_pre_probe() {
  return;
}
int __VERIFIER_nondet_int(void);
int ldv_pre_usb_register_driver() {
  return __VERIFIER_nondet_int();
}
void list_del(struct list_head *arg0) {
  return;
}
int __VERIFIER_nondet_int(void);
int media_device_register(struct media_device *arg0) {
  return __VERIFIER_nondet_int();
}
void media_device_unregister(struct media_device *arg0) {
  return;
}
void media_entity_cleanup(struct media_entity *arg0) {
  return;
}
int __VERIFIER_nondet_int(void);
int media_entity_create_link(struct media_entity *arg0, u16 arg1, struct media_entity *arg2, u16 arg3, u32 arg4) {
  return __VERIFIER_nondet_int();
}
int __VERIFIER_nondet_int(void);
int media_entity_init(struct media_entity *arg0, u16 arg1, struct media_pad *arg2, u16 arg3) {
  return __VERIFIER_nondet_int();
}
unsigned long __VERIFIER_nondet_ulong(void);
size_t memweight(const void *arg0, size_t arg1) {
  return __VERIFIER_nondet_ulong();
}
void might_fault() {
  return;
}
int __VERIFIER_nondet_int(void);
int mutex_lock_interruptible_nested(struct mutex *arg0, unsigned int arg1) {
  return __VERIFIER_nondet_int();
}
void mutex_lock_nested(struct mutex *arg0, unsigned int arg1) {
  return;
}
void mutex_unlock(struct mutex *arg0) {
  return;
}
long __VERIFIER_nondet_long(void);
loff_t no_llseek(struct file *arg0, loff_t arg1, int arg2) {
  return __VERIFIER_nondet_long();
}
int __VERIFIER_nondet_int(void);
int printk(const char *arg0, ...) {
  return __VERIFIER_nondet_int();
}
int __VERIFIER_nondet_int(void);
int scnprintf(char *arg0, size_t arg1, const char *arg2, ...) {
  return __VERIFIER_nondet_int();
}
void set_normalized_timespec(struct timespec *arg0, time_t arg1, s64 arg2) {
  return;
}
long __VERIFIER_nondet_long(void);
ssize_t simple_read_from_buffer(void *arg0, size_t arg1, loff_t *arg2, const void *arg3, size_t arg4) {
  return __VERIFIER_nondet_long();
}
int __VERIFIER_nondet_int(void);
int strcasecmp(const char *arg0, const char *arg1) {
  return __VERIFIER_nondet_int();
}
unsigned long __VERIFIER_nondet_ulong(void);
size_t strlcat(char *arg0, const char *arg1, __kernel_size_t arg2) {
  return __VERIFIER_nondet_ulong();
}
unsigned long __VERIFIER_nondet_ulong(void);
size_t strlcpy(char *arg0, const char *arg1, size_t arg2) {
  return __VERIFIER_nondet_ulong();
}
int __VERIFIER_nondet_int(void);
int strncasecmp(const char *arg0, const char *arg1, size_t arg2) {
  return __VERIFIER_nondet_int();
}
void *usb_alloc_coherent(struct usb_device *arg0, size_t arg1, gfp_t arg2, dma_addr_t *arg3) {
  return ldv_malloc(0UL);
}
struct urb *usb_alloc_urb(int arg0, gfp_t arg1) {
  return ldv_malloc(sizeof(struct urb));
}
int __VERIFIER_nondet_int(void);
int usb_autopm_get_interface(struct usb_interface *arg0) {
  return __VERIFIER_nondet_int();
}
void usb_autopm_put_interface(struct usb_interface *arg0) {
  return;
}
int __VERIFIER_nondet_int(void);
int usb_control_msg(struct usb_device *arg0, unsigned int arg1, __u8 arg2, __u8 arg3, __u16 arg4, __u16 arg5, void *arg6, __u16 arg7, int arg8) {
  return __VERIFIER_nondet_int();
}
void usb_deregister(struct usb_driver *arg0) {
  return;
}
int __VERIFIER_nondet_int(void);
int usb_driver_claim_interface(struct usb_driver *arg0, struct usb_interface *arg1, void *arg2) {
  return __VERIFIER_nondet_int();
}
void usb_driver_release_interface(struct usb_driver *arg0, struct usb_interface *arg1) {
  return;
}
void usb_enable_autosuspend(struct usb_device *arg0) {
  return;
}
void usb_free_coherent(struct usb_device *arg0, size_t arg1, void *arg2, dma_addr_t arg3) {
  return;
}
void usb_free_urb(struct urb *arg0) {
  return;
}
int __VERIFIER_nondet_int(void);
int usb_get_current_frame_number(struct usb_device *arg0) {
  return __VERIFIER_nondet_int();
}
struct usb_interface *usb_get_intf(struct usb_interface *arg0) {
  return ldv_malloc(sizeof(struct usb_interface));
}
struct usb_interface *usb_ifnum_to_if(const struct usb_device *arg0, unsigned int arg1) {
  return ldv_malloc(sizeof(struct usb_interface));
}
void usb_kill_urb(struct urb *arg0) {
  return;
}
int __VERIFIER_nondet_int(void);
int usb_match_one_id(struct usb_interface *arg0, const struct usb_device_id *arg1) {
  return __VERIFIER_nondet_int();
}
void usb_put_intf(struct usb_interface *arg0) {
  return;
}
int __VERIFIER_nondet_int(void);
int usb_register_driver(struct usb_driver *arg0, struct module *arg1, const char *arg2) {
  return __VERIFIER_nondet_int();
}
int __VERIFIER_nondet_int(void);
int usb_set_interface(struct usb_device *arg0, int arg1, int arg2) {
  return __VERIFIER_nondet_int();
}
int __VERIFIER_nondet_int(void);
int usb_string(struct usb_device *arg0, int arg1, char *arg2, size_t arg3) {
  return __VERIFIER_nondet_int();
}
int __VERIFIER_nondet_int(void);
int usb_submit_urb(struct urb *arg0, gfp_t arg1) {
  return __VERIFIER_nondet_int();
}
void v4l2_ctrl_merge(const struct v4l2_event *arg0, struct v4l2_event *arg1) {
  return;
}
void v4l2_ctrl_replace(struct v4l2_event *arg0, const struct v4l2_event *arg1) {
  return;
}
int __VERIFIER_nondet_int(void);
int v4l2_device_register(struct device *arg0, struct v4l2_device *arg1) {
  return __VERIFIER_nondet_int();
}
int __VERIFIER_nondet_int(void);
int v4l2_device_register_subdev(struct v4l2_device *arg0, struct v4l2_subdev *arg1) {
  return __VERIFIER_nondet_int();
}
void v4l2_device_unregister(struct v4l2_device *arg0) {
  return;
}
int __VERIFIER_nondet_int(void);
int v4l2_event_dequeue(struct v4l2_fh *arg0, struct v4l2_event *arg1, int arg2) {
  return __VERIFIER_nondet_int();
}
void v4l2_event_queue_fh(struct v4l2_fh *arg0, const struct v4l2_event *arg1) {
  return;
}
int __VERIFIER_nondet_int(void);
int v4l2_event_subscribe(struct v4l2_fh *arg0, const struct v4l2_event_subscription *arg1, unsigned int arg2, const struct v4l2_subscribed_event_ops *arg3) {
  return __VERIFIER_nondet_int();
}
int __VERIFIER_nondet_int(void);
int v4l2_event_unsubscribe(struct v4l2_fh *arg0, const struct v4l2_event_subscription *arg1) {
  return __VERIFIER_nondet_int();
}
void v4l2_fh_add(struct v4l2_fh *arg0) {
  return;
}
void v4l2_fh_del(struct v4l2_fh *arg0) {
  return;
}
void v4l2_fh_exit(struct v4l2_fh *arg0) {
  return;
}
void v4l2_fh_init(struct v4l2_fh *arg0, struct video_device *arg1) {
  return;
}
int __VERIFIER_nondet_int(void);
int v4l2_prio_change(struct v4l2_prio_state *arg0, enum v4l2_priority *arg1, enum v4l2_priority arg2) {
  return __VERIFIER_nondet_int();
}
int __VERIFIER_nondet_int(void);
int v4l2_prio_check(struct v4l2_prio_state *arg0, enum v4l2_priority arg1) {
  return __VERIFIER_nondet_int();
}
void v4l2_prio_init(struct v4l2_prio_state *arg0) {
  return;
}
int __VERIFIER_nondet_int(void);
enum v4l2_priority v4l2_prio_max(struct v4l2_prio_state *arg0) {
  return __VERIFIER_nondet_int();
}
void v4l2_subdev_init(struct v4l2_subdev *arg0, const struct v4l2_subdev_ops *arg1) {
  return;
}
void v4l_printk_ioctl(const char *arg0, unsigned int arg1) {
  return;
}
void vb2_buffer_done(struct vb2_buffer *arg0, enum vb2_buffer_state arg1) {
  return;
}
int __VERIFIER_nondet_int(void);
int vb2_dqbuf(struct vb2_queue *arg0, struct v4l2_buffer *arg1, bool arg2) {
  return __VERIFIER_nondet_int();
}
int __VERIFIER_nondet_int(void);
int vb2_mmap(struct vb2_queue *arg0, struct vm_area_struct *arg1) {
  return __VERIFIER_nondet_int();
}
void *vb2_plane_vaddr(struct vb2_buffer *arg0, unsigned int arg1) {
  return ldv_malloc(0UL);
}
unsigned int __VERIFIER_nondet_uint(void);
unsigned int vb2_poll(struct vb2_queue *arg0, struct file *arg1, poll_table *arg2) {
  return __VERIFIER_nondet_uint();
}
int __VERIFIER_nondet_int(void);
int vb2_qbuf(struct vb2_queue *arg0, struct v4l2_buffer *arg1) {
  return __VERIFIER_nondet_int();
}
int __VERIFIER_nondet_int(void);
int vb2_querybuf(struct vb2_queue *arg0, struct v4l2_buffer *arg1) {
  return __VERIFIER_nondet_int();
}
int __VERIFIER_nondet_int(void);
int vb2_queue_init(struct vb2_queue *arg0) {
  return __VERIFIER_nondet_int();
}
void vb2_queue_release(struct vb2_queue *arg0) {
  return;
}
int __VERIFIER_nondet_int(void);
int vb2_reqbufs(struct vb2_queue *arg0, struct v4l2_requestbuffers *arg1) {
  return __VERIFIER_nondet_int();
}
int __VERIFIER_nondet_int(void);
int vb2_streamoff(struct vb2_queue *arg0, enum v4l2_buf_type arg1) {
  return __VERIFIER_nondet_int();
}
int __VERIFIER_nondet_int(void);
int vb2_streamon(struct vb2_queue *arg0, enum v4l2_buf_type arg1) {
  return __VERIFIER_nondet_int();
}
struct video_device *video_devdata(struct file *arg0) {
  return ldv_malloc(sizeof(struct video_device));
}
struct video_device *video_device_alloc() {
  return ldv_malloc(sizeof(struct video_device));
}
void video_device_release(struct video_device *arg0) {
  return;
}
void video_unregister_device(struct video_device *arg0) {
  return;
}
long __VERIFIER_nondet_long(void);
long int video_usercopy(struct file *arg0, unsigned int arg1, unsigned long arg2, long int (*arg3)(struct file *, unsigned int, void *)) {
  return __VERIFIER_nondet_long();
}
void free(void *);
void kfree(void const *p) {
  free((void *)p);
}
