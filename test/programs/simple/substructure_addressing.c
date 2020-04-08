
typedef unsigned long size_t;

struct middma_device {
   void *dma_base ;
   unsigned int pci_id ;
   void *mask_reg ;
};

struct device {
   void *driver_data ;
};

struct pci_dev {
   void *sysdata ;
   int cfg_size ;
   struct device dev ;
};
extern void *kmalloc(size_t size ) ;
extern void *kzalloc(size_t size ) ;
extern void *ioremap_nocache(resource_size_t  , unsigned long  ) ;
static void dmac1_mask_periphral_intr(struct middma_device *mid ) 
{ 
  unsigned int pimr ;
  writel(pimr, (void volatile   *)mid->mask_reg + 8U);
}

__inline static struct middma_device *dev_get_drvdata(struct device  const  *dev ) 
{ 
  return (dev->driver_data);
}

__inline static struct middma_device *pci_get_drvdata(struct pci_dev *pdev ) 
{ 
  return dev_get_drvdata(& pdev->dev);
}

__inline static void dev_set_drvdata(struct device *dev , struct middma_device *data ) 
{ 
  dev->driver_data = data;
}

__inline static void pci_set_drvdata(struct pci_dev *pdev , struct middma_device *data ) 
{ 
  dev_set_drvdata(& pdev->dev, data);
}

static int mid_setup_dma(struct pci_dev *pdev ) 
{ 
  struct middma_device *dma ;

  {
  dma = pci_get_drvdata(pdev);
  dma->mask_reg = ioremap_nocache(4289626120ULL, 16UL);
  if (pdev->dev.driver_data == 0) {
    ERROR: goto ERROR;
  }
}
}

int main(void)
{ 
  struct middma_device *device ;
  struct device *tmp_dev;
  unsigned int base_addr;
  
  struct pci_dev *pdev = kmalloc(2976UL);
  if (pdev == 0) {
    return 0;
  }
  device = (struct middma_device *)kzalloc(1328UL);
  if (device == 0) {
    return 0;
  }
  pci_set_drvdata(pdev, device);
  mid_setup_dma(pdev);
  return 0;
}
