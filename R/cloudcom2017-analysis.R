library(dplyr)
library(foreach)
library(ggplot2)
library("Rmisc")

theme_set(theme_bw())

setwd("/local/giovanni/git/borg-simulator/")
setwd("C:/Users/giovanni/Documents/cloudish/git/borg-simulator/")

total.cloud.cpu=6603.25
total.cloud.mem=5862.75133

CollectAllocationInfo <- function(csvPath) {
  #allocation <- read.csv("server-based-results/all-constraints-on-server-based/allocation-12477-hosts.csv")
  #head(allocation)
  
  allocation <- read.csv(csvPath)
  allocSummary <- allocation %>% dplyr::summarise(servers=n(), infra.cpu=sum(cpuCapacity), infra.freeCpu=sum(freeCpu), cpu.fragmentation=infra.freeCpu/infra.cpu, cpu.remaing=total.cloud.cpu-infra.cpu,
                                                  infra.mem=sum(memCapacity), infra.freeMem=sum(freeMem), mem.fragmentation=infra.freeMem/infra.mem, mem.remaing=total.cloud.mem-infra.mem)
  
  return(allocSummary)
}

CollectPendingInfo <- function(csvPath) {
  pendingQueue <- read.csv(csvPath)
  # pendingQueue <- read.csv("server-based-results/all-constraints-on-server-based/pending-queue-12477-hosts.csv")
  
  #head(pendingQueue)
  #  pendSummary <- pendingQueue %>% mutate(prod=ifelse(priority>=9, 1, 0)) %>% mutate(nonprod=ifelse(priority<9, 1, 0)) %>% dplyr::summarise(tasks=n(), prod=sum(prod), nonprod.tasks=sum(nonprod), total.cpu=sum(cpuReq), total.mem=sum(memReq))
  pendSummary <- pendingQueue %>% mutate(prod.cpu=ifelse(priority>=9, cpuReq, 0)) %>% mutate(prod.mem=ifelse(priority>=9, memReq, 0)) %>% mutate(nonprod.cpu=ifelse(priority<9 & priority >1, cpuReq, 0)) %>% mutate(nonprod.mem=ifelse(priority<9 & priority > 1, memReq, 0)) %>% mutate(free.cpu=ifelse(priority<2, cpuReq, 0)) %>% mutate(free.mem=ifelse(priority< 2, memReq, 0)) %>% dplyr::summarise(tasks=n(), total.prod.cpu=sum(prod.cpu), total.prod.mem=sum(prod.mem), total.nonprod.cpu=sum(nonprod.cpu), total.nonprod.mem=sum(nonprod.mem), total.free.cpu=sum(free.cpu), total.free.mem=sum(free.mem), total.cpu=sum(cpuReq), total.mem=sum(memReq))
  pendSummary <- pendSummary %>% mutate(prodCpuQueue=total.prod.cpu/total.cpu * 100, prodMemQueue=total.prod.mem/total.mem *100, nonprodCpuOnQueue=total.nonprod.cpu/total.cpu, nonprodMemOnQueue=total.nonprod.mem/total.mem, freeCpuOnQueue=total.free.cpu/total.cpu, freeMemOnQueue=total.free.mem/total.mem)
  return(pendSummary)
}

CollectAllTimesSBAllocationInfo <- function(resultDir, constraintOn, allTasks) {
  constraint <- "on"
  
  if (!constraintOn)
    constraint <- "off"
  
  workload <- "all"
  if (!allTasks) 
    workload <- "prod"
  
  allAllocation <- data_frame()
  for (time in 1:28) {
    print(time)
    
    timeAllocation <- CollectAllocationInfo(paste(resultDir,"/time",time,"/", workload, "/allocation-", constraint,"-12477-hosts.csv", sep = ""))
    
    allAllocation <- rbind(allAllocation, timeAllocation)
  }
  return(allAllocation)
}

CalculateCpuFragmentationCI <- function(allocation) {
  t <- data.frame(upper = CI(allocation$cpu.fragmentation, ci = 0.95)[1], mean = CI(allocation$cpu.fragmentation, ci = 0.95)[2], lower = CI(allocation$cpu.fragmentation, ci = 0.95)[3])
  return(t)
}

CalculateMemFragmentationCI <- function(allocation) {
  t <- data.frame(upper = CI(allocation$mem.fragmentation, ci = 0.95)[1], mean = CI(allocation$mem.fragmentation, ci = 0.95)[2], lower = CI(allocation$mem.fragmentation, ci = 0.95)[3])
  return(t)
}

CalculateInfraCPUCI <- function(allocation) {
  t <- data.frame(upper = CI(allocation$infra.cpu, ci = 0.95)[1]/6603.25 * 100, mean = CI(allocation$infra.cpu, ci = 0.95)[2]/6603.25* 100, lower = CI(allocation$infra.cpu, ci = 0.95)[3]/6603.25* 100)
  return(t)
}

CalculateInfraMemCI <- function(allocation) {
  t <- data.frame(upper = CI(allocation$infra.mem, ci = 0.95)[1]/5862.751 * 100, mean = CI(allocation$infra.mem, ci = 0.95)[2]/5862.751 * 100, lower = CI(allocation$infra.mem, ci = 0.95)[3]/5862.751 * 100)
  return(t)
}

CollectAllocationDiffBetweenSBAndDH <- function(sbAllocation, dhAllocation){
  
  diff <- sbAllocation - dhAllocation
  return(diff)
}

CollectAllTimesDHAllocationInfo <- function(resultDir, constraintOn, allTasks = T, serverSize, dhModel, resourceLabel = NULL) {
  constraint <- "on"
  if (!constraintOn)
    constraint <- "off"
  
  allAllocation <- data_frame()
  
  for (time in 1:28) {
    print(paste("Time ", time), sep= "")
    
    if (is.null(resourceLabel)) {
      timeResultDir <- paste(resultDir, "/time", time, "/", sep = "")
      
    } else {
      timeResultDir <- paste(resultDir, "/time", time, "/", resourceLabel, "/", sep = "")
    }
    
    prefixFileName <- paste("allocation-", constraint,"-", dhModel, "-", serverSize, "-", serverSize, "-", sep = "")
    
    if (!allTasks) {
      timeResultDir <- paste(timeResultDir, "prod/")
    }
    
    for (fileName in list.files(path = timeResultDir)) {
      
      if (startsWith(fileName, prefixFileName)) {
        print(paste("Collecting allocation info from file:", fileName))
        
        timeAllocation <- CollectAllocationInfo(paste(timeResultDir,fileName, sep = ""))
        
        allAllocation <- rbind(allAllocation, timeAllocation)
        break
      }
    }
  }
  
  return(allAllocation)
}

CollectAllTimesDHPendingInfo <- function(resultDir, constraintOn, allTasks = T, serverSize, dhModel, numberOfTasks, resourceLabel = NULL, resourceDemand) {
  constraint <- "on"
  if (!constraintOn)
    constraint <- "off"
  
  allPendingInfo <- data_frame()
  
  for (time in 1:28) {
    #timeResultDir <- paste(resultDir, "/time", time, "/", sep = "")
    
    if (is.null(resourceLabel)) {
      timeResultDir <- paste(resultDir, "/time", time, "/", sep = "")
      
    } else {
      timeResultDir <- paste(resultDir, "/time", time, "/", resourceLabel, "/", sep = "")
    }
    
    prefixFileName <- paste("pending-queue-", constraint,"-", dhModel, "-", serverSize, "-", serverSize, "-", sep = "")
    
    if (!allTasks) {
      timeResultDir <- paste(timeResultDir, "prod/")
    } 
    
    for (fileName in list.files(path = timeResultDir)) {
      
      if (startsWith(fileName, prefixFileName)) {
        print(paste("Collecting allocation info from file:", fileName))
        
        timePending <- CollectPendingInfo(paste(timeResultDir,fileName, sep = ""))
        
        total.tasks <- numberOfTasks %>% filter(timestamp == time) %>% select(nTasks)
        cpu.demand <- resourceDemand %>% filter(timestamp == time) %>% select(cpu.request)
        mem.demand <- resourceDemand %>% filter(timestamp == time) %>% select(mem.request)
        
        #prod.cpu.demand <- resourceDemand %>% filter(timestamp == time) %>% select(prod.cpu.demand)
        #prod.mem.demand <- resourceDemand %>% filter(timestamp == time) %>% select(prod.mem.demand)
        #nonprod.cpu.demand <- resourceDemand %>% filter(timestamp == time) %>% select(nonprod.cpu.demand)
        #nonprod.mem.demand <- resourceDemand %>% filter(timestamp == time) %>% select(nonprod.mem.demand)
        #free.cpu.demand <- resourceDemand %>% filter(timestamp == time) %>% select(free.cpu.demand)
        #free.mem.demand<- resourceDemand %>% filter(timestamp == time) %>% select(free.mem.demand)
        
        #timePending <- data.frame(timePending, nTasks = total.tasks, prodCpuDemand=prod.cpu.demand, prodMemDemand=prod.mem.demand, nonProdCpuDemand=nonprod.cpu.demand, nonProdMemDemand=nonprod.mem.demand, freeCpuDemand=free.cpu.demand, freeMemDemand=free.mem.demand, cpuDemand=cpu.demand, memDemand=mem.demand)
        
        timePending <- data.frame(timePending, nTasks = total.tasks, cpuDemand=cpu.demand, memDemand=mem.demand)
        
        allPendingInfo <- rbind(allPendingInfo, timePending)
        break
      }
    }
    
  }
  #  allPendingInfo <- allPendingInfo %>% mutate(pendingFraction=tasks/nTasks, prodCpuOnQueue=total.prod.cpu/prod.cpu.demand, prodMemOnQueue=total.prod.mem/prod.mem.demand, nonprodCpuOnQueue=total.nonprod.cpu/nonprod.cpu.demand, nonprodMemOnQueue=total.nonprod.mem/nonprod.mem.demand, freeCpuOnQueue=total.free.cpu/free.cpu.demand, freeMemOnQueue=total.free.mem/free.mem.demand, cpuOnQueue=total.cpu/cpu.request, memOnQueue=total.mem/mem.request)
  allPendingInfo <- allPendingInfo %>% mutate(pendingFraction=tasks/nTasks, cpuOnQueue=total.cpu/cpu.request, memOnQueue=total.mem/mem.request)
  
  return(allPendingInfo)
}

CollectAllTimesSBPendingInfo <- function(resultDir, constraintOn, allTasks, numberOfTasks, resourceDemand) {
  constraint <- "on"
  
  if (!constraintOn)
    constraint <- "off"
  
  workload <- "all"
  if (!allTasks) 
    workload <- "prod"
  
  allPendingInfo <- data_frame()
  for (time in 1:28) {
    #time <- 1
    #resultDir <- "experiment-results-more-grains/sb-based-results"
    print(time)
    
    timePending <- CollectPendingInfo(paste(resultDir,"/time",time,"/", workload, "/pending-queue-", constraint,"-12477-hosts.csv", sep = ""))
    
    total.tasks <- numberOfTasks %>% filter(timestamp == time) %>% select(nTasks)
    cpu.demand <- resourceDemand %>% filter(timestamp == time) %>% select(cpu.request)
    mem.demand <- resourceDemand %>% filter(timestamp == time) %>% select(mem.request)
    
    #prod.cpu.demand <- resourceDemand %>% filter(timestamp == time) %>% select(prod.cpu.demand)
    #prod.mem.demand <- resourceDemand %>% filter(timestamp == time) %>% select(prod.mem.demand)
    #nonprod.cpu.demand <- resourceDemand %>% filter(timestamp == time) %>% select(nonprod.cpu.demand)
    #nonprod.mem.demand <- resourceDemand %>% filter(timestamp == time) %>% select(nonprod.mem.demand)
    #free.cpu.demand <- resourceDemand %>% filter(timestamp == time) %>% select(free.cpu.demand)
    #free.mem.demand<- resourceDemand %>% filter(timestamp == time) %>% select(free.mem.demand)
    
    #timePending <- data.frame(timePending, nTasks = total.tasks, prodCpuDemand=prod.cpu.demand, prodMemDemand=prod.mem.demand, nonProdCpuDemand=nonprod.cpu.demand, nonProdMemDemand=nonprod.mem.demand, freeCpuDemand=free.cpu.demand, freeMemDemand=free.mem.demand, cpuDemand=cpu.demand, memDemand=mem.demand)
    
    timePending <- data.frame(timePending, nTasks = total.tasks, cpuDemand=cpu.demand, memDemand=mem.demand)
    
    allPendingInfo <- rbind(allPendingInfo, timePending)
  }
  
  #allPendingInfo <- allPendingInfo %>% mutate(pendingFraction=tasks/nTasks, prodCpuOnQueue=total.prod.cpu/prod.cpu.demand, prodMemOnQueue=total.prod.mem/prod.mem.demand, nonprodCpuOnQueue=total.nonprod.cpu/nonprod.cpu.demand, nonprodMemOnQueue=total.nonprod.mem/nonprod.mem.demand, freeCpuOnQueue=total.free.cpu/free.cpu.demand, freeMemOnQueue=total.free.mem/free.mem.demand, cpuOnQueue=total.cpu/cpu.request, memOnQueue=total.mem/mem.request)
  allPendingInfo <- allPendingInfo %>% mutate(pendingFraction=tasks/nTasks, cpuOnQueue=total.cpu/cpu.request, memOnQueue=total.mem/mem.request)
  #allPendingInfo <- allPendingInfo %>% mutate(pendingFraction=tasks/nTasks)
  
  return(allPendingInfo)
}

allSBAllocationsOn <- CollectAllTimesSBAllocationInfo("experiment-results-more-grains/sb-based-results", constraintOn = T, allTasks = T)

#blade
resultDir <- "experiment-results-more-grains/dh-based-results"

allDHAllocBladeCPUMiniOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", resourceLabel = "cpu-mini")
allDHAllocBladeCPUMediumOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", resourceLabel = "cpu-medium")
allDHAllocBladeCPUXLargeOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", resourceLabel = "cpu-xlarge")

allDHAllocBladeCPUMiniOnRam <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", resourceLabel = "ram-mini")
allDHAllocBladeCPUMediumOnRam <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", resourceLabel = "ram-medium")
allDHAllocBladeCPUXLargeOnRam <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", resourceLabel = "ram-xlarge")

# cpu fragmentation
cpuFragmentationsDiff <- data.frame()
cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(grain = "cpu", model = "blade-on", infra = paste("small, medium", sep=""), CalculateCpuFragmentationCI(allDHAllocBladeCPUMiniOn)))
cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(grain = "cpu", model = "blade-on", infra = paste("medium, medium", sep=""), CalculateCpuFragmentationCI(allDHAllocBladeCPUMediumOn)))
cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(grain = "cpu", model = "blade-on", infra = paste("large, medium", sep=""), CalculateCpuFragmentationCI(allDHAllocBladeCPUXLargeOn)))
cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(grain = "ram", model = "blade-on", infra = paste("medium, small", sep=""), CalculateCpuFragmentationCI(allDHAllocBladeCPUMiniOnRam)))
cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(grain = "ram", model = "blade-on", infra = paste("medium, medium", sep=""), CalculateCpuFragmentationCI(allDHAllocBladeCPUMediumOnRam)))
cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(grain = "ram", model = "blade-on", infra = paste("medium, large", sep=""), CalculateCpuFragmentationCI(allDHAllocBladeCPUXLargeOnRam)))

wilcox.test(allDHAllocBladeCPUMiniOn$cpu.fragmentation, allDHAllocBladeCPUMediumOn$cpu.fragmentation, alternative = "less", paired = T)
wilcox.test(allDHAllocBladeCPUMediumOn$cpu.fragmentation, allDHAllocBladeCPUXLargeOn$cpu.fragmentation, alternative = "less", paired = T)

wilcox.test(pendingCpuCIDiff$cpu.fragmentation, allDHAllocBladeCPUMediumOn$cpu.fragmentation, alternative = "less", paired = T)

# mem fragmentation
memFragmentationsDiff <- data.frame()
memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(grain = "cpu", model = "blade-on", infra = paste("small, medium", sep=""), CalculateMemFragmentationCI(allDHAllocBladeCPUMiniOn)))
memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(grain = "cpu", model = "blade-on", infra = paste("medium, medium", sep=""), CalculateMemFragmentationCI(allDHAllocBladeCPUMediumOn)))
memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(grain = "cpu", model = "blade-on", infra = paste("large, medium", sep=""), CalculateMemFragmentationCI(allDHAllocBladeCPUXLargeOn)))
memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(grain = "ram", model = "blade-on", infra = paste("medium, small", sep=""), CalculateMemFragmentationCI(allDHAllocBladeCPUMiniOnRam)))
memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(grain = "ram", model = "blade-on", infra = paste("medium, medium", sep=""), CalculateMemFragmentationCI(allDHAllocBladeCPUMediumOnRam)))
memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(grain = "ram", model = "blade-on", infra = paste("medium, large", sep=""), CalculateMemFragmentationCI(allDHAllocBladeCPUXLargeOnRam)))


diffBladeCPUMiniOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocBladeCPUMiniOn)
diffBladeCPUMediumOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocBladeCPUMediumOn)
diffBladeCPUXLargeOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocBladeCPUXLargeOn)

diffBladeCPUMiniOnRam <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocBladeCPUMiniOnRam)
diffBladeCPUMediumOnRam <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocBladeCPUMediumOnRam)
diffBladeCPUXLargeOnRam <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocBladeCPUXLargeOnRam)


cpuInfraDiff <- data.frame()
cpuInfraDiff <- rbind(cpuInfraDiff, data.frame(grain = "cpu", model = "blade-on", infra = paste("small, medium", sep=""), CalculateInfraCPUCI(diffBladeCPUMiniOn)))
cpuInfraDiff <- rbind(cpuInfraDiff, data.frame(grain = "cpu", model = "blade-on", infra = paste("medium, medium", sep=""), CalculateInfraCPUCI(diffBladeCPUMediumOn)))
cpuInfraDiff <- rbind(cpuInfraDiff, data.frame(grain = "cpu", model = "blade-on", infra = paste("large, medium", sep=""), CalculateInfraCPUCI(diffBladeCPUXLargeOn)))
cpuInfraDiff <- rbind(cpuInfraDiff, data.frame(grain = "ram", model = "blade-on", infra = paste("medium, small", sep=""), CalculateInfraCPUCI(diffBladeCPUMiniOnRam)))
cpuInfraDiff <- rbind(cpuInfraDiff, data.frame(grain = "ram", model = "blade-on", infra = paste("medium, medium", sep=""), CalculateInfraCPUCI(diffBladeCPUMediumOnRam)))
cpuInfraDiff <- rbind(cpuInfraDiff, data.frame(grain = "ram", model = "blade-on", infra = paste("medium, large", sep=""), CalculateInfraCPUCI(diffBladeCPUXLargeOnRam)))


memInfraDiff <- data.frame()
memInfraDiff <- rbind(memInfraDiff, data.frame(grain = "cpu", model = "blade-on", infra = paste("small, medium", sep=""), CalculateInfraMemCI(diffBladeCPUMiniOn)))
memInfraDiff <- rbind(memInfraDiff, data.frame(grain = "cpu", model = "blade-on", infra = paste("medium, medium", sep=""), CalculateInfraMemCI(diffBladeCPUMediumOn)))
memInfraDiff <- rbind(memInfraDiff, data.frame(grain = "cpu", model = "blade-on", infra = paste("large, medium", sep=""), CalculateInfraMemCI(diffBladeCPUXLargeOn)))
memInfraDiff <- rbind(memInfraDiff, data.frame(grain = "ram", model = "blade-on", infra = paste("medium, small", sep=""), CalculateInfraMemCI(diffBladeCPUMiniOnRam)))
memInfraDiff <- rbind(memInfraDiff, data.frame(grain = "ram", model = "blade-on", infra = paste("medium, medium", sep=""), CalculateInfraMemCI(diffBladeCPUMediumOnRam)))
memInfraDiff <- rbind(memInfraDiff, data.frame(grain = "ram", model = "blade-on", infra = paste("medium, large", sep=""), CalculateInfraMemCI(diffBladeCPUXLargeOnRam)))

GenerateWorkloadInfo <- function() {
  workloadInfo <- data.frame()
  
  for (time in 1:28) {
    #time <-1 
    submittedTasks <- read.table(paste("timestamps/all-tasks-info-time-", time, "-for-java.csv", sep =""), sep = ",")
    colnames(submittedTasks) <- c("submitTime", "tid", "jid", "cpuReq", "memReq", "priority", "diffMachine")
    
    resourceDemand <- submittedTasks %>% mutate(prod.cpu=ifelse(priority>=9, cpuReq, 0)) %>% mutate(prod.mem=ifelse(priority>=9, memReq, 0)) %>% mutate(nonprod.cpu=ifelse(priority<9 & priority >1, cpuReq, 0)) %>% mutate(nonprod.mem=ifelse(priority<9 & priority > 1, memReq, 0)) %>% mutate(free.cpu=ifelse(priority<2, cpuReq, 0)) %>% mutate(free.mem=ifelse(priority< 2, memReq, 0)) %>% summarise(timestamp = time, prod.cpu.demand=sum(prod.cpu), prod.mem.demand=sum(prod.mem), nonprod.cpu.demand=sum(nonprod.cpu), nonprod.mem.demand=sum(nonprod.mem), free.cpu.demand=sum(free.cpu), free.mem.demand=sum(free.mem), cpu.request=sum(cpuReq), mem.request=sum(memReq))
    
    workloadInfo <- rbind(workloadInfo, resourceDemand)
  }
  
  return(workloadInfo)
}

nAllTasks <- read.csv("number_of_tasks.txt")
nProdTasks <- read.csv("number_of_prod_tasks.txt")
resourceDemand <- GenerateWorkloadInfo()

allSBPendingInfoOn <- CollectAllTimesSBPendingInfo("experiment-results-more-grains/sb-based-results", constraintOn = T, allTasks = T, nAllTasks, resourceDemand)

resultDir <- "experiment-results-more-grains/dh-based-results"

allDHPendingBladeMiniOn <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", nAllTasks, resourceLabel = "cpu-mini", resourceDemand)
allDHPendingBladeMediumOn <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", nAllTasks, resourceLabel = "cpu-medium", resourceDemand)
allDHPendingBladeXLargeOn <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", nAllTasks, resourceLabel = "cpu-xlarge", resourceDemand)

allDHPendingBladeMiniOnRam <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", nAllTasks, resourceLabel = "ram-mini", resourceDemand)
allDHPendingBladeMediumOnRam <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", nAllTasks, resourceLabel = "ram-medium", resourceDemand)
allDHPendingBladeXLargeOnRam <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", nAllTasks, resourceLabel = "ram-xlarge", resourceDemand)

CollectPendingDiffBetweenSBAndDH <- function(sbPending, dhPending){
  diff <- sbPending - dhPending
  return(diff)
}

CalculateCpuOnQueueCI <- function(pendingInfo) {
  t <- data.frame(upper = CI(pendingInfo$cpuOnQueue * 100, ci = 0.95)[1], mean = CI(pendingInfo$cpuOnQueue * 100, ci = 0.95)[2], lower = CI(pendingInfo$cpuOnQueue * 100, ci = 0.95)[3])
  return(t)
}

CalculateMemOnQueueCI <- function(pendingInfo) {
  t <- data.frame(upper = CI(pendingInfo$memOnQueue * 100, ci = 0.95)[1], mean = CI(pendingInfo$memOnQueue * 100, ci = 0.95)[2], lower = CI(pendingInfo$memOnQueue * 100, ci = 0.95)[3])
  return(t)
}


diffPendBladeMiniOn <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingBladeMiniOn)
diffPendBladeMediumOn <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingBladeMediumOn)
diffPendBladeXLargeOn <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingBladeXLargeOn)

diffPendBladeMiniOnRam <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingBladeMiniOnRam)
diffPendBladeMediumOnRam <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingBladeMediumOnRam)
diffPendBladeXLargeOnRam <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingBladeXLargeOnRam)

pendingCpuCIDiff <- data.frame()
pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(grain = "cpu", model = "blade-on", infra = paste("small, medium", sep=""), CalculateCpuOnQueueCI(diffPendBladeMiniOn)))
pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(grain = "cpu", model = "blade-on", infra = paste("medium, medium", sep=""), CalculateCpuOnQueueCI(diffPendBladeMediumOn)))
pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(grain = "cpu", model = "blade-on", infra = paste("large, medium", sep=""), CalculateCpuOnQueueCI(diffPendBladeXLargeOn)))

pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(grain = "ram", model = "blade-on", infra = paste("medium, small", sep=""), CalculateCpuOnQueueCI(diffPendBladeMiniOnRam)))
pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(grain = "ram", model = "blade-on", infra = paste("medium, medium", sep=""), CalculateCpuOnQueueCI(diffPendBladeMediumOnRam)))
pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(grain = "ram", model = "blade-on", infra = paste("medium, large", sep=""), CalculateCpuOnQueueCI(diffPendBladeXLargeOnRam)))

pendingMemCIDiff <- data.frame()
pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(grain = "cpu", model = "blade-on", infra = paste("small, medium", sep=""), CalculateMemOnQueueCI(diffPendBladeMiniOn)))
pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(grain = "cpu", model = "blade-on", infra = paste("medium, medium", sep=""), CalculateMemOnQueueCI(diffPendBladeMediumOn)))
pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(grain = "cpu", model = "blade-on", infra = paste("large, medium", sep=""), CalculateMemOnQueueCI(diffPendBladeXLargeOn)))

pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(grain = "ram", model = "blade-on", infra = paste("medium, small", sep=""), CalculateMemOnQueueCI(diffPendBladeMiniOnRam)))
pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(grain = "ram", model = "blade-on", infra = paste("medium, medium", sep=""), CalculateMemOnQueueCI(diffPendBladeMediumOnRam)))
pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(grain = "ram", model = "blade-on", infra = paste("medium, large", sep=""), CalculateMemOnQueueCI(diffPendBladeXLargeOnRam)))


wilcox.test(diffPendBladeMiniOn$cpuOnQueue, diffPendBladeMediumOn$cpuOnQueue, alternative = "less", paired = T)
wilcox.test(diffPendBladeMediumOn$cpuOnQueue, diffPendBladeXLargeOn$cpuOnQueue, alternative = "less", paired = T)

cpuFragmentationsDiff <- cpuFragmentationsDiff %>% mutate(upper=upper * 100, mean= mean * 100, lower=lower * 100)
memFragmentationsDiff <- memFragmentationsDiff %>% mutate(upper=upper * 100, mean= mean * 100, lower=lower * 100)

cpuFragmentation <- cpuFragmentationsDiff %>% mutate(type = "Idleness", metric = "CPU")
memFragmentation <- memFragmentationsDiff %>% mutate(type = "Idleness", metric = "RAM")

cpuPending <- pendingCpuCIDiff%>% mutate(type = "Pending", metric = "CPU")
memPending <- pendingMemCIDiff%>% mutate(type = "Pending", metric = "RAM")

cpuInfraCIinfo <- cpuInfraDiff %>% mutate(type = "Unused", metric = "CPU")
memInfraCIinfo <- memInfraDiff %>% mutate(type = "Unused", metric = "RAM")

memInfraCIinfo %>% summarise(mean(mean))

ciInfo <- rbind(cpuInfraCIinfo, memInfraCIinfo, cpuPending, memPending, cpuFragmentation, memFragmentation)

library(scales)
library(reshape2)

ciInfo <- read.table("resource_grain_ic.csv", header = T)
ciInfo$type <- factor(ciInfo$type, c("Difference left\npending", "Unused\nresources", "Aggregate\nidleness" ))
# ciInfo$type <- factor(ciInfo$type, c("Pending", "Unused", "Idleness" ))
ciInfo = ciInfo %>% select(-grain)

df_aux = data.frame(metric = toupper(c("cpu", "cpu", "cpu", "ram", "ram", "ram")), type = rep(c("Difference left\npending", "Unused\nresources", "Aggregate\nidleness" ), 2), 
                    infra = "", mean = NA, lower = NA, upper = NA,
                    model = "blade-on", aux = "") %>% select(model, infra, upper, mean, lower, type, metric, aux)

ciInfo = rbind(ciInfo, df_aux)

ciInfo$infra = factor(ciInfo$aux, c("cpu small, medium", "cpu medium, medium", "cpu large, medium", "", "ram medium, small", "ram medium, medium", "ram medium, large"))
colors <- c("#8dd3c7", "#ffffb3", "#bebada", "#FFFFFF", "#fb8072", "#ffffb3", "#80b1d3")
labels <- c("small, medium", "medium, medium", "large, medium", "", "medium, small", "medium, medium", "medium, large")

p = ggplot(ciInfo, aes(x=type, y=mean / 100, fill=infra)) + #, group = interaction(infra, grain))) + 
  geom_bar(stat="identity", position=position_dodge(0.8), width = 0.7) + 
  geom_errorbar(aes(ymin=lower / 100, ymax=upper / 100),  width=.2, position=position_dodge(.8), size = 0.7) + 
  facet_grid(metric ~ ., scales = "free") + 
  xlab(NULL) + 
  ylab(NULL) +
  scale_fill_manual("Size of grain (CPU, RAM):", 
                    breaks=c("cpu small, medium", "cpu medium, medium", "cpu large, medium", "", "ram medium, small", "ram medium, medium", "ram medium, large"),
                    values = colors,
                    labels = labels) + 
  scale_x_discrete() +
  scale_y_continuous(labels = percent, limits = c(-0.0005, 0.17)) + 
  theme_bw(base_size = 16) + guides(fill = guide_legend(nrow = 2, byrow = TRUE)) + 
  theme(legend.position = "top")
p
png(filename = "resource_grain_ic.png", width = 550, height = 350)
print(p)
dev.off()

allSBAllocationsOn <- CollectAllTimesSBAllocationInfo("experiment-results-more-grains/sb-based-results", constraintOn = T, allTasks = T)

resultDir <- "experiment-results-more-grains/dh-based-results"

#blade X drawer
allDHAllocBladeCPUMediumOff <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 1, dhModel = "blade", resourceLabel = "cpu-medium-off")
allDHAllocDrawerCPUMediumOff <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 16, dhModel = "drawer", resourceLabel = "cpu-medium-off")

cpuFragmentationsDiff2 <- data.frame()
cpuFragmentationsDiff2 <- rbind(cpuFragmentationsDiff2, data.frame(model = "blade-limited", infra = paste("medium, medium", sep=""), CalculateCpuFragmentationCI(allDHAllocBladeCPUMediumOff)))
cpuFragmentationsDiff2 <- rbind(cpuFragmentationsDiff2, data.frame(model = "drawer-limited", infra = paste("medium, medium", sep=""), CalculateCpuFragmentationCI(allDHAllocDrawerCPUMediumOff)))

memFragmentationsDiff2 <- data.frame()
memFragmentationsDiff2 <- rbind(memFragmentationsDiff2, data.frame(model = "blade-limited", infra = paste("medium, medium", sep=""), CalculateMemFragmentationCI(allDHAllocBladeCPUMediumOff)))
memFragmentationsDiff2 <- rbind(memFragmentationsDiff2, data.frame(model = "drawer-limited", infra = paste("medium, medium", sep=""), CalculateMemFragmentationCI(allDHAllocDrawerCPUMediumOff)))

cpuInfraDiff2 <- data.frame()
cpuInfraDiff2 <- rbind(cpuInfraDiff2, data.frame(model = "blade-limited", infra = paste("medium, medium", sep=""), CalculateInfraCPUCI(CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocBladeCPUMediumOff))))
cpuInfraDiff2 <- rbind(cpuInfraDiff2, data.frame(model = "drawer-limited", infra = paste("medium, medium", sep=""), CalculateInfraCPUCI(CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocDrawerCPUMediumOff))))

memInfraDiff2 <- data.frame()
memInfraDiff2 <- rbind(memInfraDiff2, data.frame(model = "blade-limited", infra = paste("medium, medium", sep=""), CalculateInfraMemCI(CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocBladeCPUMediumOff))))
memInfraDiff2 <- rbind(memInfraDiff2, data.frame(model = "drawer-limited", infra = paste("medium, medium", sep=""), CalculateInfraMemCI(CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocDrawerCPUMediumOff))))

allDHPendingBladeMediumOff <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 1, dhModel = "blade", nAllTasks, resourceLabel = "cpu-medium-off", resourceDemand)
allDHPendingDrawerMediumOff <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 16, dhModel = "drawer", nAllTasks, resourceLabel = "cpu-medium-off", resourceDemand)

diffPendBladeMediumOff <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingBladeMediumOff)
diffPendDrawerMediumOff <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingDrawerMediumOff)

pendingCpuCIDiff2 <- data.frame()
pendingCpuCIDiff2<- rbind(pendingCpuCIDiff2, data.frame(model = "blade-limited", infra = paste("medium, medium", sep=""), CalculateCpuOnQueueCI(diffPendBladeMediumOff)))
pendingCpuCIDiff2<- rbind(pendingCpuCIDiff2, data.frame(model = "drawer-limited", infra = paste("medium, medium", sep=""), CalculateCpuOnQueueCI(diffPendDrawerMediumOff)))

pendingMemCIDiff2 <- data.frame()
pendingMemCIDiff2<- rbind(pendingMemCIDiff2, data.frame(model = "blade-limited", infra = paste("medium, medium", sep=""), CalculateMemOnQueueCI(diffPendBladeMediumOff)))
pendingMemCIDiff2<- rbind(pendingMemCIDiff2, data.frame(model = "drawer-limited", infra = paste("medium, medium", sep=""), CalculateMemOnQueueCI(diffPendDrawerMediumOff)))

cpuFragmentationsDiff2 <- cpuFragmentationsDiff2 %>% mutate(upper=upper * 100, mean= mean * 100, lower=lower * 100)
memFragmentationsDiff2 <- memFragmentationsDiff2 %>% mutate(upper=upper * 100, mean= mean * 100, lower=lower * 100)

cpuFragmentation2 <- cpuFragmentationsDiff2 %>% mutate(type = "Idleness", metric = "CPU")
memFragmentation2 <- memFragmentationsDiff2 %>% mutate(type = "Idleness", metric = "RAM")

cpuPending2 <- pendingCpuCIDiff2%>% mutate(type = "Pending", metric = "CPU")
memPending2 <- pendingMemCIDiff2%>% mutate(type = "Pending", metric = "RAM")

cpuInfraCIinfo2 <- cpuInfraDiff2 %>% mutate(type = "Unused", metric = "CPU")
memInfraCIinfo2 <- memInfraDiff2 %>% mutate(type = "Unused", metric = "RAM")

#cpuFragmentation2 <- cpuFragmentation2 %>% mutate(model=ifelse(model=="blade", "blade-limited", "drawer-limited"))
#cpuInfraCIinfo2 <- cpuInfraCIinfo2 %>% mutate(model=ifelse(model=="blade", "blade-limited", "drawer-limited"))
#cpuPending2 <- cpuPending2 %>% mutate(model=ifelse(model=="blade", "blade-limited", "drawer-limited"))

#memFragmentation2 <- memFragmentation2 %>% mutate(model=ifelse(model=="blade", "blade-limited", "drawer-limited"))
#memInfraCIinfo2 <- memInfraCIinfo2 %>% mutate(model=ifelse(model=="blade", "blade-limited", "drawer-limited"))
#memPending2 <- memPending2 %>% mutate(model=ifelse(model=="blade", "blade-limited", "drawer-limited"))


ciInfo2 <- rbind(cpuInfraCIinfo2, memInfraCIinfo2, cpuPending2, memPending2, cpuFragmentation2, memFragmentation2)

ciInfo2 <- read.table(file = "blade_vs_drawer_ic.csv", header = T)

ciInfo2$type = factor(ciInfo2$type, c("Difference left\npending", "Unused\nresources", "Aggregate\nidleness" ))
p = ggplot(ciInfo2, aes(x=type, y=mean / 100, fill=model)) +
     geom_bar(stat="identity", position=position_dodge(0.8), width = 0.7) +
     geom_errorbar(aes(ymin=lower / 100, ymax=upper / 100),  width=.2, position=position_dodge(0.8), size = 0.7) +
     facet_grid(metric ~ ., scales = "free") +
     xlab(NULL) +
     ylab(NULL) +
     scale_fill_brewer("Maximum size:\n", palette = "Set3") +
     scale_y_continuous(labels = percent, limits = c(-0.005, 0.17)) + theme_bw(base_size = 16)

p
png(filename = "blade_vs_drawer_ic.png", width = 550, height = 250)
print(p)
dev.off()
