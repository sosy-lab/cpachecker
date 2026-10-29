// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2011-2021 The SV-Benchmarks Community
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
//
// This is the SV-COMP/LDV benchmark
// "32_1_cilled_ok_nondet_linux-3.4-32_1-drivers--ata--pata_netcell.ko-ldv_main0_sequence_infinite_withcheck_stateful.cil.out.i"
// with every unused declaration removed and nothing else touched: every
// function, global variable, struct literal, and control-flow construct that
// the original file actually uses is preserved byte-for-byte, under its
// original name, with its original signature.
//
// What got removed is exactly what has zero references anywhere in the
// 4641-line original:
//  - ~800 Linux kernel typedef/struct/enum declarations that nothing in this
//    driver's reachable code touches (task_struct, signal_struct, the VFS,
//    the scheduler, ...). A struct that IS touched, but only through a
//    pointer (e.g. "struct pci_dev *pdev"), keeps a one-line forward
//    declaration instead of its full member list, since nothing here ever
//    dereferences a field of it; a struct that is embedded by value anywhere
//    live (e.g. inside the netcell_sht / netcell_ops / netcell_pci_driver
//    registration tables) keeps its full definition, unchanged.
//  - mutex_lock, mutex_unlock, mutex_trylock, mutex_lock_interruptible,
//    mutex_lock_killable and atomic_dec_and_mutex_lock: declared and defined,
//    but never called from anywhere in the original file either. The rest of
//    the LDV mutex-usage rule automaton (the "ldv_mutex" state variable,
//    ldv_blast_assert, and the final ldv_check_final_state check that main()
//    does call) is kept exactly as it was.
//  - init_module/cleanup_module (main() calls netcell_init()/netcell_exit()
//    directly), the four "__mod_*" module-metadata byte arrays, and
//    ldv__builtin_expect/kfree/free, none of which anything calls.
//
// This was produced mechanically: the original was split into its top-level
// declarations, a reachability sweep from main() determined which of those
// declarations are actually referenced (transitively), and only the
// unreferenced ones were dropped. Every kept declaration is an unmodified
// copy of the corresponding declaration in the original .i file. The result
// still verifies to the same TRUE verdict as the original, for the same
// reason: reach_error() is only reachable through a lock/unlock call that
// violates the protocol, and -- exactly as in the original -- no such call
// ever happens.

struct Scsi_Host;
struct ata_device;
struct ata_host;
struct ata_link;
struct ata_port;
struct ata_queued_cmd;
struct ata_taskfile;
struct attribute_group;
struct bin_attribute;
struct bio_integrity_payload;
enum blk_eh_timer_return;
struct block_device;
struct bug_entry;
struct bus_type;
struct class_attribute;
struct dev_pm_domain;
struct dev_pm_ops;
struct dev_pm_qos_request;
struct device_attribute;
struct device_dma_parameters;
struct device_node;
struct device_private;
struct device_type;
struct dma_coherent_mem;
struct dma_map_ops;
struct driver_private;
struct exception_table_entry;
struct ftrace_event_call;
struct gendisk;
struct hd_struct;
struct io_cq;
struct kernel_param;
struct kernel_symbol;
struct kobj_ns_type_operations;
struct kobj_type;
struct kobj_uevent_env;
struct kset_uevent_ops;
struct module_attribute;
struct module_notes_attrs;
struct module_param_attrs;
struct module_ref;
struct module_sect_attrs;
struct of_device_id;
struct page;
struct pci_dev;
struct pci_error_handlers;
struct pm_qos_constraints;
struct pm_subsys_data;
struct proc_dir_entry;
struct request_queue;
struct scsi_cmnd;
struct scsi_device;
struct scsi_target;
struct subsys_private;
struct sysfs_dirent;
struct task_struct;
struct tracepoint;
struct tvec_base;
struct wakeup_source;

extern void abort(void);

extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
void reach_error() { ((void) sizeof ((0) ? 1 : 0), __extension__ ({ if (0) ; else __assert_fail ("0", "32_1_cilled_ok_nondet_linux-3.4-32_1-drivers--ata--pata_netcell.ko-ldv_main0_sequence_infinite_withcheck_stateful.cil.out.c", 3, __extension__ __PRETTY_FUNCTION__); })); }
typedef unsigned short __u16;
typedef unsigned int __u32;
typedef unsigned long long __u64;
typedef unsigned char u8;
typedef unsigned short u16;
typedef unsigned int u32;
typedef long long s64;
typedef unsigned long long u64;
typedef long __kernel_long_t;
typedef unsigned long __kernel_ulong_t;
typedef __kernel_ulong_t __kernel_size_t;
typedef __kernel_long_t __kernel_ssize_t;
typedef __kernel_long_t __kernel_off_t;
typedef __u32 __kernel_dev_t;
typedef __kernel_dev_t dev_t;
typedef unsigned short umode_t;
typedef __kernel_off_t off_t;
typedef _Bool bool;
typedef __kernel_size_t size_t;
typedef __kernel_ssize_t ssize_t;
typedef unsigned long sector_t;
struct __anonstruct_atomic_t_7 {
   int counter ;
};
typedef struct __anonstruct_atomic_t_7 atomic_t;
struct __anonstruct_atomic64_t_8 {
   long counter ;
};
typedef struct __anonstruct_atomic64_t_8 atomic64_t;
struct list_head {
   struct list_head *next ;
   struct list_head *prev ;
};
struct hlist_node {
   struct hlist_node *next ;
   struct hlist_node **pprev ;
};
typedef void (*ctor_fn_t)(void);
typedef atomic64_t atomic_long_t;
typedef u16 __ticket_t;
typedef u32 __ticketpair_t;
struct __raw_tickets {
   __ticket_t head ;
   __ticket_t tail ;
};
union __anonunion____missing_field_name_36 {
   __ticketpair_t head_tail ;
   struct __raw_tickets tickets ;
};
struct arch_spinlock {
   union __anonunion____missing_field_name_36 __annonCompField17 ;
};
typedef struct arch_spinlock arch_spinlock_t;
struct raw_spinlock {
   arch_spinlock_t raw_lock ;
   unsigned int magic ;
   unsigned int owner_cpu ;
   void *owner ;
};
union __anonunion____missing_field_name_39 {
   struct raw_spinlock rlock ;
};
struct spinlock {
   union __anonunion____missing_field_name_39 __annonCompField19 ;
};
typedef struct spinlock spinlock_t;
struct __wait_queue_head {
   spinlock_t lock ;
   struct list_head task_list ;
};
typedef struct __wait_queue_head wait_queue_head_t;
struct mutex {
   atomic_t count ;
   spinlock_t wait_lock ;
   struct list_head wait_list ;
   struct task_struct *owner ;
   char const *name ;
   void *magic ;
};
struct completion {
   unsigned int done ;
   wait_queue_head_t wait ;
};
union ktime {
   s64 tv64 ;
};
typedef union ktime ktime_t;
struct timer_list {
   struct list_head entry ;
   unsigned long expires ;
   struct tvec_base *base ;
   void (*function)(unsigned long ) ;
   unsigned long data ;
   int slack ;
   int start_pid ;
   void *start_site ;
   char start_comm[16] ;
};
struct work_struct {
   atomic_long_t data ;
   struct list_head entry ;
   void (*func)(struct work_struct *work ) ;
};
struct pm_message {
   int event ;
};
typedef struct pm_message pm_message_t;
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
struct dev_pm_info {
   pm_message_t power_state ;
   unsigned int can_wakeup : 1 ;
   unsigned int async_suspend : 1 ;
   bool is_prepared : 1 ;
   bool is_suspended : 1 ;
   bool ignore_children : 1 ;
   spinlock_t lock ;
   struct list_head entry ;
   struct completion completion ;
   struct wakeup_source *wakeup ;
   bool wakeup_path : 1 ;
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
   enum rpm_request request ;
   enum rpm_status runtime_status ;
   int runtime_error ;
   int autosuspend_delay ;
   unsigned long last_busy ;
   unsigned long active_jiffies ;
   unsigned long suspended_jiffies ;
   unsigned long accounting_timestamp ;
   ktime_t suspend_time ;
   s64 max_time_suspended_ns ;
   struct dev_pm_qos_request *pq_req ;
   struct pm_subsys_data *subsys_data ;
   struct pm_qos_constraints *constraints ;
};
struct call_single_data {
   struct list_head list ;
   void (*func)(void *info ) ;
   void *info ;
   u16 flags ;
   u16 priv ;
};
struct rb_node {
   unsigned long rb_parent_color ;
   struct rb_node *rb_right ;
   struct rb_node *rb_left ;
} __attribute__((__aligned__(sizeof(long )))) ;
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
struct kref {
   atomic_t refcount ;
};
struct kobject {
   char const *name ;
   struct list_head entry ;
   struct kobject *parent ;
   struct kset *kset ;
   struct kobj_type *ktype ;
   struct sysfs_dirent *sd ;
   struct kref kref ;
   unsigned int state_initialized : 1 ;
   unsigned int state_in_sysfs : 1 ;
   unsigned int state_add_uevent_sent : 1 ;
   unsigned int state_remove_uevent_sent : 1 ;
   unsigned int uevent_suppress : 1 ;
};
struct kset {
   struct list_head list ;
   spinlock_t list_lock ;
   struct kobject kobj ;
   struct kset_uevent_ops const *uevent_ops ;
};
struct mod_arch_specific {
};
struct module_kobject {
   struct kobject kobj ;
   struct module *mod ;
   struct kobject *drivers_dir ;
   struct module_param_attrs *mp ;
};
enum module_state {
    MODULE_STATE_LIVE = 0,
    MODULE_STATE_COMING = 1,
    MODULE_STATE_GOING = 2
} ;
struct module {
   enum module_state state ;
   struct list_head list ;
   char name[64UL - sizeof(unsigned long )] ;
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
   ctor_fn_t *ctors ;
   unsigned int num_ctors ;
};
typedef unsigned long kernel_ulong_t;
struct pci_device_id {
   __u32 vendor ;
   __u32 device ;
   __u32 subvendor ;
   __u32 subdevice ;
   __u32 class ;
   __u32 class_mask ;
   kernel_ulong_t driver_data ;
};
struct klist_node {
   void *n_klist ;
   struct list_head n_node ;
   struct kref n_ref ;
};
struct dev_archdata {
   void *acpi_handle ;
   struct dma_map_ops *dma_ops ;
   void *iommu ;
};
struct device_driver {
   char const *name ;
   struct bus_type *bus ;
   struct module *owner ;
   char const *mod_name ;
   bool suppress_bind_attrs ;
   struct of_device_id const *of_match_table ;
   int (*probe)(struct device *dev ) ;
   int (*remove)(struct device *dev ) ;
   void (*shutdown)(struct device *dev ) ;
   int (*suspend)(struct device *dev , pm_message_t state ) ;
   int (*resume)(struct device *dev ) ;
   struct attribute_group const **groups ;
   struct dev_pm_ops const *pm ;
   struct driver_private *p ;
};
struct class {
   char const *name ;
   struct module *owner ;
   struct class_attribute *class_attrs ;
   struct device_attribute *dev_attrs ;
   struct bin_attribute *dev_bin_attrs ;
   struct kobject *dev_kobj ;
   int (*dev_uevent)(struct device *dev , struct kobj_uevent_env *env ) ;
   char *(*devnode)(struct device *dev , umode_t *mode ) ;
   void (*class_release)(struct class *class ) ;
   void (*dev_release)(struct device *dev ) ;
   int (*suspend)(struct device *dev , pm_message_t state ) ;
   int (*resume)(struct device *dev ) ;
   struct kobj_ns_type_operations const *ns_type ;
   void const *(*namespace)(struct device *dev ) ;
   struct dev_pm_ops const *pm ;
   struct subsys_private *p ;
};
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
   void (*release)(struct device *dev ) ;
};
struct pci_dynids {
   spinlock_t lock ;
   struct list_head list ;
};
struct pci_driver {
   struct list_head node ;
   char const *name ;
   struct pci_device_id const *id_table ;
   int (*probe)(struct pci_dev *dev , struct pci_device_id const *id ) ;
   void (*remove)(struct pci_dev *dev ) ;
   int (*suspend)(struct pci_dev *dev , pm_message_t state ) ;
   int (*suspend_late)(struct pci_dev *dev , pm_message_t state ) ;
   int (*resume_early)(struct pci_dev *dev ) ;
   int (*resume)(struct pci_dev *dev ) ;
   void (*shutdown)(struct pci_dev *dev ) ;
   struct pci_error_handlers *err_handler ;
   struct device_driver driver ;
   struct pci_dynids dynids ;
};
typedef void bio_end_io_t(struct bio * , int );
typedef void bio_destructor_t(struct bio * );
struct bio_vec {
   struct page *bv_page ;
   unsigned int bv_len ;
   unsigned int bv_offset ;
};
struct bio {
   sector_t bi_sector ;
   struct bio *bi_next ;
   struct block_device *bi_bdev ;
   unsigned long bi_flags ;
   unsigned long bi_rw ;
   unsigned short bi_vcnt ;
   unsigned short bi_idx ;
   unsigned int bi_phys_segments ;
   unsigned int bi_size ;
   unsigned int bi_seg_front_size ;
   unsigned int bi_seg_back_size ;
   unsigned int bi_max_vecs ;
   atomic_t bi_cnt ;
   struct bio_vec *bi_io_vec ;
   bio_end_io_t *bi_end_io ;
   void *bi_private ;
   struct bio_integrity_payload *bi_integrity ;
   bio_destructor_t *bi_destructor ;
   struct bio_vec bi_inline_vecs[0] ;
};
typedef void rq_end_io_fn(struct request * , int );
enum rq_cmd_type_bits {
    REQ_TYPE_FS = 1,
    REQ_TYPE_BLOCK_PC = 2,
    REQ_TYPE_SENSE = 3,
    REQ_TYPE_PM_SUSPEND = 4,
    REQ_TYPE_PM_RESUME = 5,
    REQ_TYPE_PM_SHUTDOWN = 6,
    REQ_TYPE_SPECIAL = 7,
    REQ_TYPE_ATA_TASKFILE = 8,
    REQ_TYPE_ATA_PC = 9
} ;
union __anonunion____missing_field_name_263 {
   struct rb_node rb_node ;
   void *completion_data ;
};
struct __anonstruct_elv_265 {
   struct io_cq *icq ;
   void *priv[2] ;
};
struct __anonstruct_flush_266 {
   unsigned int seq ;
   struct list_head list ;
   rq_end_io_fn *saved_end_io ;
};
union __anonunion____missing_field_name_264 {
   struct __anonstruct_elv_265 elv ;
   struct __anonstruct_flush_266 flush ;
};
struct request {
   struct list_head queuelist ;
   struct call_single_data csd ;
   struct request_queue *q ;
   unsigned int cmd_flags ;
   enum rq_cmd_type_bits cmd_type ;
   unsigned long atomic_flags ;
   int cpu ;
   unsigned int __data_len ;
   sector_t __sector ;
   struct bio *bio ;
   struct bio *biotail ;
   struct hlist_node hash ;
   union __anonunion____missing_field_name_263 __annonCompField50 ;
   union __anonunion____missing_field_name_264 __annonCompField51 ;
   struct gendisk *rq_disk ;
   struct hd_struct *part ;
   unsigned long start_time ;
   unsigned short nr_phys_segments ;
   unsigned short nr_integrity_segments ;
   unsigned short ioprio ;
   int ref_count ;
   void *special ;
   char *buffer ;
   int tag ;
   int errors ;
   unsigned char __cmd[16] ;
   unsigned char *cmd ;
   unsigned short cmd_len ;
   unsigned int extra_len ;
   unsigned int sense_len ;
   unsigned int resid_len ;
   void *sense ;
   unsigned long deadline ;
   struct list_head timeout_list ;
   unsigned int timeout ;
   int retries ;
   rq_end_io_fn *end_io ;
   void *end_io_data ;
   struct request *next_rq ;
};
struct scsi_host_template {
   struct module *module ;
   char const *name ;
   int (*detect)(struct scsi_host_template * ) ;
   int (*release)(struct Scsi_Host * ) ;
   char const *(*info)(struct Scsi_Host * ) ;
   int (*ioctl)(struct scsi_device *dev , int cmd , void *arg ) ;
   int (*compat_ioctl)(struct scsi_device *dev , int cmd , void *arg ) ;
   int (*queuecommand)(struct Scsi_Host * , struct scsi_cmnd * ) ;
   int (*transfer_response)(struct scsi_cmnd * , void (*done)(struct scsi_cmnd * ) ) ;
   int (*eh_abort_handler)(struct scsi_cmnd * ) ;
   int (*eh_device_reset_handler)(struct scsi_cmnd * ) ;
   int (*eh_target_reset_handler)(struct scsi_cmnd * ) ;
   int (*eh_bus_reset_handler)(struct scsi_cmnd * ) ;
   int (*eh_host_reset_handler)(struct scsi_cmnd * ) ;
   int (*slave_alloc)(struct scsi_device * ) ;
   int (*slave_configure)(struct scsi_device * ) ;
   void (*slave_destroy)(struct scsi_device * ) ;
   int (*target_alloc)(struct scsi_target * ) ;
   void (*target_destroy)(struct scsi_target * ) ;
   int (*scan_finished)(struct Scsi_Host * , unsigned long ) ;
   void (*scan_start)(struct Scsi_Host * ) ;
   int (*change_queue_depth)(struct scsi_device * , int , int ) ;
   int (*change_queue_type)(struct scsi_device * , int ) ;
   int (*bios_param)(struct scsi_device * , struct block_device * , sector_t , int * ) ;
   void (*unlock_native_capacity)(struct scsi_device * ) ;
   int (*proc_info)(struct Scsi_Host * , char * , char ** , off_t , int , int ) ;
   enum blk_eh_timer_return (*eh_timed_out)(struct scsi_cmnd * ) ;
   int (*host_reset)(struct Scsi_Host *shost , int reset_type ) ;
   char const *proc_name ;
   struct proc_dir_entry *proc_dir ;
   int can_queue ;
   int this_id ;
   unsigned short sg_tablesize ;
   unsigned short sg_prot_tablesize ;
   unsigned short max_sectors ;
   unsigned long dma_boundary ;
   short cmd_per_lun ;
   unsigned char present ;
   unsigned int supported_mode : 2 ;
   unsigned int unchecked_isa_dma : 1 ;
   unsigned int use_clustering : 1 ;
   unsigned int emulated : 1 ;
   unsigned int skip_settle_delay : 1 ;
   unsigned int ordered_tag : 1 ;
   unsigned int max_host_blocked ;
   struct device_attribute **shost_attrs ;
   struct device_attribute **sdev_attrs ;
   struct list_head legacy_hosts ;
   u64 vendor_id ;
};
typedef void *acpi_handle;
#pragma pack(1)
#pragma pack()
#pragma pack(1)
#pragma pack()
#pragma pack(1)
#pragma pack()
#pragma pack(1)
#pragma pack()
#pragma pack(1)
#pragma pack()
struct completion;
enum ata_lpm_policy {
    ATA_LPM_UNKNOWN = 0,
    ATA_LPM_MAX_POWER = 1,
    ATA_LPM_MED_POWER = 2,
    ATA_LPM_MIN_POWER = 3
} ;
enum sw_activity {
    OFF = 0,
    BLINK_ON = 1,
    BLINK_OFF = 2
} ;
struct ata_port_operations {
   int (*qc_defer)(struct ata_queued_cmd *qc ) ;
   int (*check_atapi_dma)(struct ata_queued_cmd *qc ) ;
   void (*qc_prep)(struct ata_queued_cmd *qc ) ;
   unsigned int (*qc_issue)(struct ata_queued_cmd *qc ) ;
   bool (*qc_fill_rtf)(struct ata_queued_cmd *qc ) ;
   int (*cable_detect)(struct ata_port *ap ) ;
   unsigned long (*mode_filter)(struct ata_device *dev , unsigned long xfer_mask ) ;
   void (*set_piomode)(struct ata_port *ap , struct ata_device *dev ) ;
   void (*set_dmamode)(struct ata_port *ap , struct ata_device *dev ) ;
   int (*set_mode)(struct ata_link *link , struct ata_device **r_failed_dev ) ;
   unsigned int (*read_id)(struct ata_device *dev , struct ata_taskfile *tf , u16 *id ) ;
   void (*dev_config)(struct ata_device *dev ) ;
   void (*freeze)(struct ata_port *ap ) ;
   void (*thaw)(struct ata_port *ap ) ;
   int (*prereset)(struct ata_link *link , unsigned long deadline ) ;
   int (*softreset)(struct ata_link *link , unsigned int *classes , unsigned long deadline ) ;
   int (*hardreset)(struct ata_link *link , unsigned int *classes , unsigned long deadline ) ;
   void (*postreset)(struct ata_link *link , unsigned int *classes ) ;
   int (*pmp_prereset)(struct ata_link *link , unsigned long deadline ) ;
   int (*pmp_softreset)(struct ata_link *link , unsigned int *classes , unsigned long deadline ) ;
   int (*pmp_hardreset)(struct ata_link *link , unsigned int *classes , unsigned long deadline ) ;
   void (*pmp_postreset)(struct ata_link *link , unsigned int *classes ) ;
   void (*error_handler)(struct ata_port *ap ) ;
   void (*lost_interrupt)(struct ata_port *ap ) ;
   void (*post_internal_cmd)(struct ata_queued_cmd *qc ) ;
   int (*scr_read)(struct ata_link *link , unsigned int sc_reg , u32 *val ) ;
   int (*scr_write)(struct ata_link *link , unsigned int sc_reg , u32 val ) ;
   void (*pmp_attach)(struct ata_port *ap ) ;
   void (*pmp_detach)(struct ata_port *ap ) ;
   int (*set_lpm)(struct ata_link *link , enum ata_lpm_policy policy , unsigned int hints ) ;
   int (*port_suspend)(struct ata_port *ap , pm_message_t mesg ) ;
   int (*port_resume)(struct ata_port *ap ) ;
   int (*port_start)(struct ata_port *ap ) ;
   void (*port_stop)(struct ata_port *ap ) ;
   void (*host_stop)(struct ata_host *host ) ;
   void (*sff_dev_select)(struct ata_port *ap , unsigned int device ) ;
   void (*sff_set_devctl)(struct ata_port *ap , u8 ctl ) ;
   u8 (*sff_check_status)(struct ata_port *ap ) ;
   u8 (*sff_check_altstatus)(struct ata_port *ap ) ;
   void (*sff_tf_load)(struct ata_port *ap , struct ata_taskfile const *tf ) ;
   void (*sff_tf_read)(struct ata_port *ap , struct ata_taskfile *tf ) ;
   void (*sff_exec_command)(struct ata_port *ap , struct ata_taskfile const *tf ) ;
   unsigned int (*sff_data_xfer)(struct ata_device *dev , unsigned char *buf , unsigned int buflen ,
                                 int rw ) ;
   void (*sff_irq_on)(struct ata_port * ) ;
   bool (*sff_irq_check)(struct ata_port * ) ;
   void (*sff_irq_clear)(struct ata_port * ) ;
   void (*sff_drain_fifo)(struct ata_queued_cmd *qc ) ;
   void (*bmdma_setup)(struct ata_queued_cmd *qc ) ;
   void (*bmdma_start)(struct ata_queued_cmd *qc ) ;
   void (*bmdma_stop)(struct ata_queued_cmd *qc ) ;
   u8 (*bmdma_status)(struct ata_port *ap ) ;
   ssize_t (*em_show)(struct ata_port *ap , char *buf ) ;
   ssize_t (*em_store)(struct ata_port *ap , char const *message , size_t size ) ;
   ssize_t (*sw_activity_show)(struct ata_device *dev , char *buf ) ;
   ssize_t (*sw_activity_store)(struct ata_device *dev , enum sw_activity val ) ;
   void (*phy_reset)(struct ata_port *ap ) ;
   void (*eng_timeout)(struct ata_port *ap ) ;
   struct ata_port_operations const *inherits ;
};
struct ata_port_info {
   unsigned long flags ;
   unsigned long link_flags ;
   unsigned long pio_mask ;
   unsigned long mwdma_mask ;
   unsigned long udma_mask ;
   struct ata_port_operations *port_ops ;
   void *private_data ;
};
extern struct module __this_module ;
extern int __attribute__((__warn_unused_result__)) pcim_enable_device(struct pci_dev *pdev ) ;
extern int __attribute__((__warn_unused_result__)) __pci_register_driver(struct pci_driver * ,
                                                                          struct module * ,
                                                                          char const *mod_name ) ;
extern void pci_unregister_driver(struct pci_driver *dev ) ;
extern int ata_scsi_ioctl(struct scsi_device *dev , int cmd , void *arg ) ;
extern int ata_scsi_queuecmd(struct Scsi_Host *h , struct scsi_cmnd *cmd ) ;
extern unsigned int ata_do_dev_read_id(struct ata_device *dev , struct ata_taskfile *tf ,
                                       u16 *id ) ;
extern int ata_std_bios_param(struct scsi_device *sdev , struct block_device *bdev ,
                              sector_t capacity , int *geom ) ;
extern void ata_scsi_unlock_native_capacity(struct scsi_device *sdev ) ;
extern int ata_scsi_slave_config(struct scsi_device *sdev ) ;
extern void ata_scsi_slave_destroy(struct scsi_device *sdev ) ;
extern int ata_cable_80wire(struct ata_port *ap ) ;
extern void ata_pci_remove_one(struct pci_dev *pdev ) ;
extern int ata_pci_device_suspend(struct pci_dev *pdev , int mesgevent ) ;
extern int ata_pci_device_resume(struct pci_dev *pdev ) ;
extern struct device_attribute *ata_common_sdev_attrs[] ;
extern void ata_print_version(struct device const *dev , char const *version ) ;
extern struct ata_port_operations const ata_bmdma_port_ops ;
extern int ata_pci_bmdma_clear_simplex(struct pci_dev *pdev ) ;
extern int ata_pci_bmdma_init_one(struct pci_dev *pdev , struct ata_port_info const * const *ppi ,
                                  struct scsi_host_template *sht , void *host_priv ,
                                  int hflags ) ;
static unsigned int netcell_read_id(struct ata_device *adev , struct ata_taskfile *tf ,
                                    u16 *id )
{ unsigned int err_mask ;
  unsigned int tmp___7 ;
  u16 *__cil_tmp6 ;
  u16 *__cil_tmp7 ;
  u16 __cil_tmp8 ;
  int __cil_tmp9 ;
  int __cil_tmp10 ;
  {
  {
  tmp___7 = ata_do_dev_read_id(adev, tf, id);
  err_mask = tmp___7;
  }
  if (err_mask == 0U) {
    __cil_tmp6 = id + 87;
    __cil_tmp7 = id + 87;
    __cil_tmp8 = *__cil_tmp7;
    __cil_tmp9 = (int )__cil_tmp8;
    __cil_tmp10 = __cil_tmp9 | 16384;
    *__cil_tmp6 = (u16 )__cil_tmp10;
  } else {
  }
  return (err_mask);
}
}
static struct scsi_host_template netcell_sht =
     {& __this_module, "pata_netcell", (int (*)(struct scsi_host_template * ))0, (int (*)(struct Scsi_Host * ))0,
    (char const *(*)(struct Scsi_Host * ))0, & ata_scsi_ioctl, (int (*)(struct scsi_device *dev ,
                                                                          int cmd ,
                                                                          void *arg ))0,
    & ata_scsi_queuecmd, (int (*)(struct scsi_cmnd * , void (*done)(struct scsi_cmnd * ) ))0,
    (int (*)(struct scsi_cmnd * ))0, (int (*)(struct scsi_cmnd * ))0, (int (*)(struct scsi_cmnd * ))0,
    (int (*)(struct scsi_cmnd * ))0, (int (*)(struct scsi_cmnd * ))0, (int (*)(struct scsi_device * ))0,
    & ata_scsi_slave_config, & ata_scsi_slave_destroy, (int (*)(struct scsi_target * ))0,
    (void (*)(struct scsi_target * ))0, (int (*)(struct Scsi_Host * , unsigned long ))0,
    (void (*)(struct Scsi_Host * ))0, (int (*)(struct scsi_device * , int , int ))0,
    (int (*)(struct scsi_device * , int ))0, & ata_std_bios_param, & ata_scsi_unlock_native_capacity,
    (int (*)(struct Scsi_Host * , char * , char ** , off_t , int , int ))0, (enum blk_eh_timer_return (*)(struct scsi_cmnd * ))0,
    (int (*)(struct Scsi_Host *shost , int reset_type ))0, "pata_netcell", (struct proc_dir_entry *)0,
    1, -1, (unsigned short)128, (unsigned short)0, (unsigned short)0, 65535UL, (short)1,
    (unsigned char)0, 0U, 0U, 1U, 1U, 0U, 0U, 0U, (struct device_attribute **)0, ata_common_sdev_attrs,
    {(struct list_head *)0, (struct list_head *)0}, 0ULL};
static struct ata_port_operations netcell_ops =
     {(int (*)(struct ata_queued_cmd *qc ))0, (int (*)(struct ata_queued_cmd *qc ))0,
    (void (*)(struct ata_queued_cmd *qc ))0, (unsigned int (*)(struct ata_queued_cmd *qc ))0,
    (bool (*)(struct ata_queued_cmd *qc ))0, & ata_cable_80wire, (unsigned long (*)(struct ata_device *dev ,
                                                                                    unsigned long xfer_mask ))0,
    (void (*)(struct ata_port *ap , struct ata_device *dev ))0, (void (*)(struct ata_port *ap ,
                                                                          struct ata_device *dev ))0,
    (int (*)(struct ata_link *link , struct ata_device **r_failed_dev ))0, & netcell_read_id,
    (void (*)(struct ata_device *dev ))0, (void (*)(struct ata_port *ap ))0, (void (*)(struct ata_port *ap ))0,
    (int (*)(struct ata_link *link , unsigned long deadline ))0, (int (*)(struct ata_link *link ,
                                                                          unsigned int *classes ,
                                                                          unsigned long deadline ))0,
    (int (*)(struct ata_link *link , unsigned int *classes , unsigned long deadline ))0,
    (void (*)(struct ata_link *link , unsigned int *classes ))0, (int (*)(struct ata_link *link ,
                                                                          unsigned long deadline ))0,
    (int (*)(struct ata_link *link , unsigned int *classes , unsigned long deadline ))0,
    (int (*)(struct ata_link *link , unsigned int *classes , unsigned long deadline ))0,
    (void (*)(struct ata_link *link , unsigned int *classes ))0, (void (*)(struct ata_port *ap ))0,
    (void (*)(struct ata_port *ap ))0, (void (*)(struct ata_queued_cmd *qc ))0, (int (*)(struct ata_link *link ,
                                                                                         unsigned int sc_reg ,
                                                                                         u32 *val ))0,
    (int (*)(struct ata_link *link , unsigned int sc_reg , u32 val ))0, (void (*)(struct ata_port *ap ))0,
    (void (*)(struct ata_port *ap ))0, (int (*)(struct ata_link *link , enum ata_lpm_policy policy ,
                                                unsigned int hints ))0, (int (*)(struct ata_port *ap ,
                                                                                 pm_message_t mesg ))0,
    (int (*)(struct ata_port *ap ))0, (int (*)(struct ata_port *ap ))0, (void (*)(struct ata_port *ap ))0,
    (void (*)(struct ata_host *host ))0, (void (*)(struct ata_port *ap , unsigned int device ))0,
    (void (*)(struct ata_port *ap , u8 ctl ))0, (u8 (*)(struct ata_port *ap ))0, (u8 (*)(struct ata_port *ap ))0,
    (void (*)(struct ata_port *ap , struct ata_taskfile const *tf ))0, (void (*)(struct ata_port *ap ,
                                                                                   struct ata_taskfile *tf ))0,
    (void (*)(struct ata_port *ap , struct ata_taskfile const *tf ))0, (unsigned int (*)(struct ata_device *dev ,
                                                                                           unsigned char *buf ,
                                                                                           unsigned int buflen ,
                                                                                           int rw ))0,
    (void (*)(struct ata_port * ))0, (bool (*)(struct ata_port * ))0, (void (*)(struct ata_port * ))0,
    (void (*)(struct ata_queued_cmd *qc ))0, (void (*)(struct ata_queued_cmd *qc ))0,
    (void (*)(struct ata_queued_cmd *qc ))0, (void (*)(struct ata_queued_cmd *qc ))0,
    (u8 (*)(struct ata_port *ap ))0, (ssize_t (*)(struct ata_port *ap , char *buf ))0,
    (ssize_t (*)(struct ata_port *ap , char const *message , size_t size ))0, (ssize_t (*)(struct ata_device *dev ,
                                                                                             char *buf ))0,
    (ssize_t (*)(struct ata_device *dev , enum sw_activity val ))0, (void (*)(struct ata_port *ap ))0,
    (void (*)(struct ata_port *ap ))0, & ata_bmdma_port_ops};
static int netcell_init_one(struct pci_dev *pdev , struct pci_device_id const *ent ) ;
static struct ata_port_info const info = {1UL, 0UL, 31UL, 7UL, 63UL, & netcell_ops, (void *)0};
static bool __print_once ;
static int netcell_init_one(struct pci_dev *pdev , struct pci_device_id const *ent )
{ struct ata_port_info const *port_info[2] ;
  int rc ;
  int tmp___7 ;
  unsigned long __cil_tmp6 ;
  unsigned long __cil_tmp7 ;
  unsigned long __cil_tmp8 ;
  unsigned long __cil_tmp9 ;
  void *__cil_tmp10 ;
  unsigned long __cil_tmp11 ;
  unsigned long __cil_tmp12 ;
  struct device *__cil_tmp13 ;
  struct device const *__cil_tmp14 ;
  unsigned long __cil_tmp15 ;
  unsigned long __cil_tmp16 ;
  struct ata_port_info const **__cil_tmp17 ;
  struct ata_port_info const * const *__cil_tmp18 ;
  void *__cil_tmp19 ;
  {
  __cil_tmp6 = 0 * 8UL;
  __cil_tmp7 = (unsigned long )(port_info) + __cil_tmp6;
  *((struct ata_port_info const **)__cil_tmp7) = & info;
  __cil_tmp8 = 1 * 8UL;
  __cil_tmp9 = (unsigned long )(port_info) + __cil_tmp8;
  __cil_tmp10 = (void *)0;
  *((struct ata_port_info const **)__cil_tmp9) = (struct ata_port_info const *)__cil_tmp10;
  if (! __print_once) {
    {
    __print_once = (bool )1;
    __cil_tmp11 = (unsigned long )pdev;
    __cil_tmp12 = __cil_tmp11 + 144;
    __cil_tmp13 = (struct device *)__cil_tmp12;
    __cil_tmp14 = (struct device const *)__cil_tmp13;
    ata_print_version(__cil_tmp14, "0.1.7");
    }
  } else {
  }
  {
  rc = (int )pcim_enable_device(pdev);
  }
  if (rc) {
    return (rc);
  } else {
  }
  {
  ata_pci_bmdma_clear_simplex(pdev);
  __cil_tmp15 = 0 * 8UL;
  __cil_tmp16 = (unsigned long )(port_info) + __cil_tmp15;
  __cil_tmp17 = (struct ata_port_info const **)__cil_tmp16;
  __cil_tmp18 = (struct ata_port_info const * const *)__cil_tmp17;
  __cil_tmp19 = (void *)0;
  tmp___7 = ata_pci_bmdma_init_one(pdev, __cil_tmp18, & netcell_sht, __cil_tmp19,
                                   0);
  }
  return (tmp___7);
}
}
static struct pci_device_id const netcell_pci_tbl[1] = { {(__u32 )5788, (__u32 )68, (__u32 )(~ 0), (__u32 )(~ 0), (__u32 )0, (__u32 )0,
      0UL}};
static struct pci_driver netcell_pci_driver =
     {{(struct list_head *)0, (struct list_head *)0}, "pata_netcell", netcell_pci_tbl,
    & netcell_init_one, & ata_pci_remove_one, & ata_pci_device_suspend, (int (*)(struct pci_dev *dev ,
                                                                                 pm_message_t state ))0,
    (int (*)(struct pci_dev *dev ))0, & ata_pci_device_resume, (void (*)(struct pci_dev *dev ))0,
    (struct pci_error_handlers *)0, {(char const *)0, (struct bus_type *)0, (struct module *)0,
                                     (char const *)0, (_Bool)0, (struct of_device_id const *)0,
                                     (int (*)(struct device *dev ))0, (int (*)(struct device *dev ))0,
                                     (void (*)(struct device *dev ))0, (int (*)(struct device *dev ,
                                                                                pm_message_t state ))0,
                                     (int (*)(struct device *dev ))0, (struct attribute_group const **)0,
                                     (struct dev_pm_ops const *)0, (struct driver_private *)0},
    {{{{{{0U}}, 0U, 0U, (void *)0}}}, {(struct list_head *)0, (struct list_head *)0}}};
static int netcell_init(void) __attribute__((__section__(".init.text"), __no_instrument_function__)) ;
static int netcell_init(void)
{ int tmp___7 ;
  {
  {
  tmp___7 = (int )__pci_register_driver(& netcell_pci_driver, & __this_module, "pata_netcell");
  }
  return (tmp___7);
}
}
static void netcell_exit(void) __attribute__((__section__(".exit.text"), __no_instrument_function__)) ;
static void netcell_exit(void)
{
  {
  {
  pci_unregister_driver(& netcell_pci_driver);
  }
  return;
}
}
void ldv_check_final_state(void) ;
extern void ldv_check_return_value(int res ) ;
extern void ldv_initialize(void) ;
extern int __VERIFIER_nondet_int(void) ;
int LDV_IN_INTERRUPT ;
static int res_netcell_init_one_1 ;
void main(void)
{ struct ata_device *var_group1 ;
  struct ata_taskfile *var_group2 ;
  u16 *var_netcell_read_id_0_p2 ;
  struct pci_dev *var_group3 ;
  struct pci_device_id const *var_netcell_init_one_1_p1 ;
  int tmp___7 ;
  int ldv_s_netcell_pci_driver_pci_driver ;
  int tmp___8 ;
  int tmp___9 ;
  int __cil_tmp10 ;
  {
  {
  LDV_IN_INTERRUPT = 1;
  ldv_initialize();
  tmp___7 = netcell_init();
  }
  if (tmp___7) {
    goto ldv_final;
  } else {
  }
  ldv_s_netcell_pci_driver_pci_driver = 0;
  {
  while (1) {
    while_continue: ;
    {
    tmp___9 = __VERIFIER_nondet_int();
    }
    if (tmp___9) {
    } else {
      {
      __cil_tmp10 = ldv_s_netcell_pci_driver_pci_driver == 0;
      if (! __cil_tmp10) {
      } else {
        goto while_break;
      }
      }
    }
    {
    tmp___8 = __VERIFIER_nondet_int();
    }
    if (tmp___8 == 0) {
      goto case_0;
    } else
    if (tmp___8 == 1) {
      goto case_1;
    } else {
      {
      goto switch_default;
      if (0) {
        case_0:
        {
        netcell_read_id(var_group1, var_group2, var_netcell_read_id_0_p2);
        }
        goto switch_break;
        case_1:
        if (ldv_s_netcell_pci_driver_pci_driver == 0) {
          {
          res_netcell_init_one_1 = netcell_init_one(var_group3, var_netcell_init_one_1_p1);
          ldv_check_return_value(res_netcell_init_one_1);
          }
          if (res_netcell_init_one_1) {
            goto ldv_module_exit;
          } else {
          }
          ldv_s_netcell_pci_driver_pci_driver = 0;
        } else {
        }
        goto switch_break;
        switch_default:
        goto switch_break;
      } else {
        switch_break: ;
      }
      }
    }
  }
  while_break: ;
  }
  ldv_module_exit:
  {
  netcell_exit();
  }
  ldv_final:
  {
  ldv_check_final_state();
  }
  return;
}
}
void ldv_blast_assert(void)
{
  {
  ERROR: {reach_error();abort();}
}
}
extern int __VERIFIER_nondet_int(void) ;
int ldv_mutex = 1;
void ldv_check_final_state(void)
{
  {
  if (ldv_mutex == 1) {
  } else {
    {
    ldv_blast_assert();
    }
  }
  return;
}
}
int __VERIFIER_nondet_int(void);
int __pci_register_driver(struct pci_driver *arg0, struct module *arg1, const char *arg2) {
  return __VERIFIER_nondet_int();
}
unsigned int __VERIFIER_nondet_uint(void);
unsigned int ata_do_dev_read_id(struct ata_device *arg0, struct ata_taskfile *arg1, u16 *arg2) {
  return __VERIFIER_nondet_uint();
}
int __VERIFIER_nondet_int(void);
int ata_pci_bmdma_clear_simplex(struct pci_dev *arg0) {
  return __VERIFIER_nondet_int();
}
int __VERIFIER_nondet_int(void);
int ata_pci_bmdma_init_one(struct pci_dev *arg0, const struct ata_port_info * const *arg1, struct scsi_host_template *arg2, void *arg3, int arg4) {
  return __VERIFIER_nondet_int();
}
void ata_print_version(const struct device *arg0, const char *arg1) {
  return;
}
void ldv_check_return_value(int arg0) {
  return;
}
void ldv_initialize() {
  return;
}
void pci_unregister_driver(struct pci_driver *arg0) {
  return;
}
int __VERIFIER_nondet_int(void);
int pcim_enable_device(struct pci_dev *arg0) {
  return __VERIFIER_nondet_int();
}
