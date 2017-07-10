library(dplyr)
library(foreach)
library(ggplot2)
library("Rmisc")

setwd("/local/giovanni/git/borg-simulator/")
setwd("C:/Users/giovanni/Documents/cloudish/git/borg-simulator/")

total.cloud.cpu=6603.25
total.cloud.mem=5862.751

total.tasks=136585
total.prod.tasks=56048 

CollectAllocationInfo <- function(csvPath) {
  allocation <- read.csv("server-based-results/all-constraints-on-server-based/allocation-12477-hosts.csv")
  head(allocation)
  
  allocation <- read.csv(csvPath)
  allcoSummary <- allocation %>% summarise(servers=n(), infra.cpu=sum(cpuCapacity), infra.freeCpu=sum(freeCpu), cpu.fragmentation=infra.freeCpu/infra.cpu, cpu.remaing=total.cloud.cpu-infra.cpu,
                           infra.mem=sum(memCapacity), infra.freeMem=sum(freeMem), mem.fragmentation=infra.freeMem/infra.mem, mem.remaing=infra.mem-infra.mem)

  return(allocSummary)
}

CollectPendingInfo <- function(csvPath) {
  pendingQueue <- read.csv(csvPath)
  pendingQueue <- read.csv("server-based-results/all-constraints-on-server-based/pending-queue-12477-hosts.csv")

  pendSummary <- pendingQueue %>% mutate(prod=ifelse(priority>=9, 1, 0)) %>% mutate(nonprod=ifelse(priority<9, 1, 0)) %>% dplyr::summarise(tasks=n(), prod=sum(prod), nonprod.tasks=sum(nonprod), total.cpu=sum(cpuReq), total.mem=sum(memReq))
  return(pendSummary)
}


allocation %>% summary()
pendingQueueSize <- pendingQueueSB %>% summarise(n())
pendingQueueSB %>% group_by(priority) %>% summarise(n=n(), total.cpu=sum(cpuReq), total.mem=sum(memReq), perc.of.pending.queue=(n()/pendingQueueSize$`n()`))
pendingQueueFractionSB <- (pendingQueueSB %>% summarise(n()))/total.tasks



CI <- tasks %>% group_by(cpuReqNor) %>% dplyr::summarise(upper = CI(availability, ci = 0.95)[1], mean = CI(availability, ci = 0.95)[2], lower = CI(availability, ci = 0.95)[3]) 

head(allocation)
t <- data.frame()
t <- rbind(t, c(timestamp = 0, upper = CI(allocation$cpuCapacity, ci = 0.95)[1], mean = CI(allocation$cpuCapacity, ci = 0.95)[2], lower = CI(allocation$cpuCapacity, ci = 0.95)[3]))

colnames(t) <- c("time", "upper", "mean", "lower")
is.data.frame(t)
ggplot(t, aes(x=time, y=mean)) + geom_point() + geom_errorbar(aes(ymax = upper, ymin=lower))



# server-based
# all tasks constraint On
allocationSB <- read.csv("server-based-results/all-constraints-on-server-based/allocation-12477-hosts.csv")
pendingQueueSB <- read.csv("server-based-results/all-constraints-on-server-based/pending-queue-12477-hosts.csv")

pendingQueueSize <- pendingQueueSB %>% summarise(n())
allocationSB %>% summarise(total.cpu=sum(cpuCapacity), total.freeCpu=sum(freeCpu), cpu.fragmentation=total.freeCpu/total.cloud.cpu, total.mem=sum(memCapacity), total.freeMem=sum(freeMem), mem.fragmentation=total.freeMem/total.cloud.mem)
pendingQueueSB %>% summarise(n=n(), total.cpu=sum(cpuReq), total.mem=sum(memReq), perc.of.pending.queue=(n()/pendingQueueSize$`n()`))
pendingQueueSB %>% group_by(priority) %>% summarise(n=n(), total.cpu=sum(cpuReq), total.mem=sum(memReq), perc.of.pending.queue=(n()/pendingQueueSize$`n()`))
pendingQueueFractionSB <- (pendingQueueSB %>% summarise(n()))/total.tasks

# prod tasks constraint On
allocationProdSB <- read.csv("server-based-results/prod-constraints-on-server-based/allocation-12477-hosts.csv")
pendingQueueProdSB <- read.csv("server-based-results/prod-constraints-on-server-based/pending-queue-12477-hosts.csv")

pendingQueueSize <- pendingQueueProdSB %>% summarise(n())
allocationProdSB %>% summarise(total.cpu=sum(cpuCapacity), total.freeCpu=sum(freeCpu), cpu.fragmentation=total.freeCpu/total.cloud.cpu, total.mem=sum(memCapacity), total.freeMem=sum(freeMem), mem.fragmentation=total.freeMem/total.cloud.mem)
pendingQueueProdSB %>% summarise(n=n(), total.cpu=sum(cpuReq), total.mem=sum(memReq), perc.of.pending.queue=(n()/pendingQueueSize$`n()`))
(pendingQueueProdSB %>% summarise(n()))/total.prod.tasks

# all tasks constraint Off
allocationSBOff <- read.csv("server-based-results/all-constraints-off-server-based/allocation-12477-hosts.csv")
pendingQueueSBOff <- read.csv("server-based-results/all-constraints-off-server-based/pending-queue-12477-hosts.csv")

pendingQueueSize <- pendingQueueSBOff %>% summarise(n())
allocationSBOff %>% summarise(total.cpu=sum(cpuCapacity), total.freeCpu=sum(freeCpu), cpu.fragmentation=total.freeCpu/total.cloud.cpu, total.mem=sum(memCapacity), total.freeMem=sum(freeMem), mem.fragmentation=total.freeMem/total.cloud.mem)
pendingQueueSBOff %>% group_by(priority) %>% summarise(n=n(), total.cpu=sum(cpuReq), total.mem=sum(memReq), perc.of.pending.queue=(n()/pendingQueueSize$`n()`))
(pendingQueueSBOff %>% summarise(n()))/total.tasks

# prod tasks constraint Off
allocationSBProdOff <- read.csv("server-based-results/prod-constraints-off-server-based/allocation-12477-hosts.csv")
pendingQueueSBProdOff <- read.csv("server-based-results/prod-constraints-off-server-based/pending-queue-12477-hosts.csv")

pendingQueueSize <- pendingQueueSBProdOff %>% summarise(n())
allocationSBProdOff %>% summarise(total.cpu=sum(cpuCapacity), total.freeCpu=sum(freeCpu), cpu.fragmentation=total.freeCpu/total.cloud.cpu, total.mem=sum(memCapacity), total.freeMem=sum(freeMem), mem.fragmentation=total.freeMem/total.cloud.mem)
pendingQueueSBProdOff %>% group_by(priority) %>% summarise(n=n(), total.cpu=sum(cpuReq), total.mem=sum(memReq), perc.of.pending.queue=(n()/pendingQueueSize$`n()`))
(pendingQueueSBProdOff %>% summarise(n()))/total.tasks


# DH-based server-size
allocationDH <- read.csv("dh-based-results/all-constraint-on-dh-server-size-1-v2/allocation-11760-logicalservers.csv")
pendingQueueDH <- read.csv("dh-based-results/all-constraint-on-dh-server-size-1-v2/pending-queue-11760-logicalservers.csv")

allocationDH <- read.csv("dh-based-results/all-constraint-on-dh-server-size-1-v2/allocation-12479-logicalservers.csv")
pendingQueueDH <- read.csv("dh-based-results/all-constraint-on-dh-server-size-1-v2/pending-queue-12479-logicalservers.csv")

allocationDH <- read.csv("dh-based-results/all-constraint-on-dh-server-size-16-v2/allocation-9247-logicalservers.csv")
pendingQueueDH <- read.csv("dh-based-results/all-constraint-on-dh-server-size-16-v2/pending-queue-9247-logicalservers.csv")

allocationDH <- read.csv("dh-based-results/all-constraint-on-dh-server-size-16-v2/allocation-12483-logicalservers.csv")
pendingQueueDH <- read.csv("dh-based-results/all-constraint-on-dh-server-size-16-v2/pending-queue-12483-logicalservers.csv")

allocationDH <- read.csv("dh-based-results/all-constraint-off-dh-server-size-1/allocation-7210-logicalservers.csv")
pendingQueueDH <- read.csv("dh-based-results/all-constraint-off-dh-server-size-1/pending-queue-7210-logicalservers.csv")

allocationDH <- read.csv("dh-based-results/all-constraint-off-dh-server-size-1/allocation-12480-logicalservers.csv")
pendingQueueDH <- read.csv("dh-based-results/all-constraint-off-dh-server-size-1/pending-queue-12480-logicalservers.csv")



pendingQueueSize <- pendingQueueDH %>% summarise(n())
allocationDH %>% summarise(total.cpu=sum(cpuCapacity), total.freeCpu=sum(freeCpu), cpu.fragmentation=total.freeCpu/total.cloud.cpu, total.mem=sum(memCapacity), total.freeMem=sum(freeMem), mem.fragmentation=total.freeMem/total.cloud.mem)
pendingQueueDH %>% summarise(n=n(), total.cpu=sum(cpuReq), total.mem=sum(memReq), perc.of.pending.queue=(n()/pendingQueueSize$`n()`))
(pendingQueueDH %>% summarise(n()))/total.tasks

pendingQueueDH %>% group_by(priority) %>% summarise(n=n(), total.cpu=sum(cpuReq), total.mem=sum(memReq), perc.of.pending.queue=(n()/pendingQueueSize$`n()`))


allocationDH %>% summarise(total.cpu=sum(cpuCapacity), total.freeCpu=sum(freeCpu), cpu.fragmentation=total.freeCpu/total.cloud.cpu, total.mem=sum(memCapacity), total.freeMem=sum(freeMem), mem.fragmentation=total.freeMem/total.cloud.mem)
pendingQueueDH %>% group_by(priority) %>% summarise(n=n(), total.cpu=sum(cpuReq), cpu.pending.perct=total.cpu/total.cloud.cpu, total.mem=sum(memReq), mem.pending.perct=total.mem/total.cloud.mem)




allocation <- read.csv("results-prod-with-constraints-server-centric-0/allocation-12477-hosts.csv")
head(allocation) %>% mu
pendingQueue <- read.csv("results-prod-with-constraints-server-centric-0/pending-queue-12477-hosts.csv")
head(pendingQueue)

allocation <- read.csv("results-all-with-constraints-server-centric-1/allocation-12477-hosts.csv")

#infrastructure capacity
allocation %>% summarise(total.cpu=sum(cpuCapacity), mean.cpu=mean(cpuCapacity), total.freeCpu=sum(freeCpu), total.mem=sum(memCapacity), total.freeMem=sum(freeMem))
allocation %>% summary()


# allocation of prod tasks in DH-based with server-size scenario 1
allocationDH <- read.csv("results-prod-with-constraint-dh-server-size-scenario1/allocation-9822-logicalservers.csv")
#pending-queue empty

# allocation of prod tasks in DH-based with server-size scenario 2
allocationDH <- read.csv("results-prod-with-constraint-dh-server-size-scenario2/allocation-8040-logicalservers.csv")

allocationDH %>% summarise(total.cpu=sum(cpuCapacity), mean.cpu=mean(cpuCapacity), total.freeCpu=sum(freeCpu), total.mem=sum(memCapacity), total.freeMem=sum(freeMem))
allocationDH %>% select(cpuCapacity, freeCpu) %>% summary()
head(allocationDH)

# allocation of all tasks in DH-based with server-size scenario 1
allocationDH <- read.csv("results-all-with-constraint-dh-server-size-scenario1/allocation-12109-logicalservers.csv")
#pending-queue 43233 tasks

allocationTest <- read.csv("results-server-size-0407-prod/allocation-9057-logicalservers.csv")
allocationTest %>% select(cpuCapacity, freeCpu) %>% summary()
allocationTest %>% group_by(cpuPoolId) %>% summarise(total=n(), total.cpu=sum(cpuCapacity))

# allocation of all tasks in DH-based with server-size scenario 2
allocationDH <- read.csv("results-all-with-constraint-dh-server-size-scenario2/allocation-9166-logicalservers.csv")

# allocation of all tasks in DH-based with server-size scenario 2
allocationDH <- read.csv("results-all-with-constraint-dh-server-size-scenario3/allocation-9246-logicalservers.csv")
p <- allocationDH %>% group_by(cpuPoolId) %>% summarise(total=n(), total.cpu=sum(cpuCapacity), mean.cpu=mean(cpuCapacity))

allocationDH %>% summarise(total.cpu=sum(cpuCapacity), total.freeCpu=sum(freeCpu), total.mem=sum(memCapacity), total.freeMem=sum(freeMem))
allocationDH %>% summary()
head(allocationDH)
