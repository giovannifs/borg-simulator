library(dplyr)
library(foreach)
library(ggplot2)
library("Rmisc")

theme_set(theme_bw())

setwd("/local/giovanni/git/borg-simulator/")
setwd("C:/Users/giovanni/Documents/cloudish/git/borg-simulator/")

total.cloud.cpu=6603.25
total.cloud.mem=5862.75133

total.tasks=136585
total.prod.tasks=56048 

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
  #pendingQueue <- read.csv("server-based-results/all-constraints-on-server-based/pending-queue-12477-hosts.csv")

  #head(pendingQueue)
  pendSummary <- pendingQueue %>% mutate(prod=ifelse(priority>=9, 1, 0)) %>% mutate(nonprod=ifelse(priority<9, 1, 0)) %>% dplyr::summarise(tasks=n(), prod=sum(prod), nonprod.tasks=sum(nonprod), total.cpu=sum(cpuReq), total.mem=sum(memReq))
  return(pendSummary)
}

CollectPendingRequestPerPriority <- function(csvPath) {
  pendingQueue <- read.csv(csvPath)
  #pendingQueue <- read.csv("server-based-results/all-constraints-on-server-based/pending-queue-12477-hosts.csv")
  
  #head(pendingQueue)
  pendingRequestPerPriority <- pendingQueue %>% group_by(priority) %>% dplyr::summarise(tasks=n(), cpu.request=sum(cpuReq), mem.request=sum(memReq))
  #pendSummary <- pendingQueue %>% mutate(prod=ifelse(priority>=9, 1, 0)) %>% mutate(nonprod=ifelse(priority<9, 1, 0)) %>% dplyr::summarise(tasks=n(), prod=sum(prod), nonprod.tasks=sum(nonprod), total.cpu=sum(cpuReq), total.mem=sum(memReq))

  pendingRequestPerPriority <- pendingRequestPerPriority %>% mutate(all.pend.tasks=sum(tasks), all.pend.cpu.req=sum(cpu.request), all.pend.mem.req=sum(mem.request))  
  return(pendingRequestPerPriority)
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

CollectAllTimesSBPendingInfo <- function(resultDir, constraintOn, allTasks, numberOfTasks, resourceDemand) {
  constraint <- "on"
  
  if (!constraintOn)
    constraint <- "off"
  
  workload <- "all"
  if (!allTasks) 
    workload <- "prod"
  
  allPendingInfo <- data_frame()
  for (time in 1:28) {
    print(time)
    
    timePending <- CollectPendingInfo(paste(resultDir,"/time",time,"/", workload, "/pending-queue-", constraint,"-12477-hosts.csv", sep = ""))
    
    total.tasks <- numberOfTasks %>% filter(timestamp == time) %>% select(nTasks)
    cpu.demand <- resourceDemand %>% filter(timestamp == time) %>% select(cpu.request)
    mem.demand <- resourceDemand %>% filter(timestamp == time) %>% select(mem.request)
    
    timePending <- data.frame(timePending, nTasks = total.tasks, cpuDemand=cpu.demand, memDemand=mem.demand)
    
    allPendingInfo <- rbind(allPendingInfo, timePending)
  }
  
  allPendingInfo <- allPendingInfo %>% mutate(pendingFraction=tasks/nTasks, cpuOnQueue=total.cpu/cpu.request, memOnQueue=total.mem/mem.request)
  #allPendingInfo <- allPendingInfo %>% mutate(pendingFraction=tasks/nTasks)
  
  return(allPendingInfo)
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
        
        timePending <- data.frame(timePending, nTasks = total.tasks, cpuDemand=cpu.demand, memDemand=mem.demand)
        
        allPendingInfo <- rbind(allPendingInfo, timePending)
        break
      }
    }
    
  }
  allPendingInfo <- allPendingInfo %>% mutate(pendingFraction=tasks/nTasks, cpuOnQueue=total.cpu/cpu.request, memOnQueue=total.mem/mem.request)

  return(allPendingInfo)
}

CalculateCpuFragmentationCI <- function(allocation) {
  t <- data.frame(upper = CI(allocation$cpu.fragmentation, ci = 0.95)[1], mean = CI(allocation$cpu.fragmentation, ci = 0.95)[2], lower = CI(allocation$cpu.fragmentation, ci = 0.95)[3])
  return(t)
}

CalculateMemFragmentationCI <- function(allocation) {
  t <- data.frame(upper = CI(allocation$mem.fragmentation, ci = 0.95)[1], mean = CI(allocation$mem.fragmentation, ci = 0.95)[2], lower = CI(allocation$mem.fragmentation, ci = 0.95)[3])
  return(t)
}

CalculateServersCI <- function(allocation) {
  t <- data.frame(upper = CI(allocation$servers, ci = 0.95)[1], mean = CI(allocation$servers, ci = 0.95)[2], lower = CI(allocation$servers, ci = 0.95)[3])
  return(t)
}

CalculateAdittionalServersCI <- function(allocation, initialServers) {
  t <- data.frame(upper = CI(allocation$servers - initialServers, ci = 0.95)[1], mean = CI(allocation$servers  - initialServers, ci = 0.95)[2], lower = CI(allocation$servers - initialServers, ci = 0.95)[3])
  return(t)
}


CalculateInfraCPUCI <- function(allocation) {
  t <- data.frame(upper = CI(allocation$infra.cpu, ci = 0.95)[1], mean = CI(allocation$infra.cpu, ci = 0.95)[2], lower = CI(allocation$infra.cpu, ci = 0.95)[3])
  return(t)
}

CalculateInfraMemCI <- function(allocation) {
  t <- data.frame(upper = CI(allocation$infra.mem, ci = 0.95)[1], mean = CI(allocation$infra.mem, ci = 0.95)[2], lower = CI(allocation$infra.mem, ci = 0.95)[3])
  return(t)
}

CalculatePendingFractionCI <- function(pendingInfo) {
  t <- data.frame(upper = CI((pendingInfo$pendingFraction), ci = 0.95)[1], mean = CI((pendingInfo$pendingFraction), ci = 0.95)[2], lower = CI((pendingInfo$pendingFraction), ci = 0.95)[3])
  #t <- data.frame(upper = CI(((pendingInfo$tasks)/pendingInfo$nTasks), ci = 0.95)[1], mean = CI(((pendingInfo$tasks)/pendingInfo$nTasks), ci = 0.95)[2], lower = CI(((pendingInfo$tasks)/pendingInfo$nTasks), ci = 0.95)[3])
  return(t)
}

CalculateProdPendingFractionCI <- function(pendingInfo) {
  t <- data.frame(upper = CI(((pendingInfo$prod)/pendingInfo$tasks), ci = 0.95)[1], mean = CI(((pendingInfo$prod)/pendingInfo$tasks), ci = 0.95)[2], lower = CI(((pendingInfo$prod)/pendingInfo$tasks), ci = 0.95)[3])
  return(t)
}

CalculatePendingCpuCI <- function(pendingInfo) {
  t <- data.frame(upper = CI(pendingInfo$total.cpu, ci = 0.95)[1], mean = CI(pendingInfo$total.cpu, ci = 0.95)[2], lower = CI(pendingInfo$total.cpu, ci = 0.95)[3])
  return(t)
}

CalculateCpuOnQueueCI <- function(pendingInfo) {
  t <- data.frame(upper = CI(pendingInfo$cpuOnQueue * 100, ci = 0.95)[1], mean = CI(pendingInfo$cpuOnQueue * 100, ci = 0.95)[2], lower = CI(pendingInfo$cpuOnQueue * 100, ci = 0.95)[3])
  return(t)
}

CalculateMemOnQueueCI <- function(pendingInfo) {
  t <- data.frame(upper = CI(pendingInfo$memOnQueue * 100, ci = 0.95)[1], mean = CI(pendingInfo$memOnQueue * 100, ci = 0.95)[2], lower = CI(pendingInfo$memOnQueue * 100, ci = 0.95)[3])
  return(t)
}

CalculatePendingMemCI <- function(pendingInfo) {
  t <- data.frame(upper = CI(pendingInfo$total.mem, ci = 0.95)[1], mean = CI(pendingInfo$total.mem, ci = 0.95)[2], lower = CI(pendingInfo$total.mem, ci = 0.95)[3]) 
  return(t)
}

PlotCpuFragmentationCI <- function(fragmentations) {
  ggplot(fragmentations, aes(x=infra, y=mean * 100)) + geom_point() + geom_errorbar(aes(ymax = upper*100, ymin=lower*100)) + ylab("% of CPU idleness") + xlab("Infrastructure") +  
    ggtitle(paste("CI of difference in CPU idleness", sep="")) + expand_limits(y=0) + theme(axis.text.x = element_text(angle = 90, hjust = 1))
  
}

PlotCpuInfraCI <- function(fragmentations) {
  ggplot(fragmentations, aes(x=infra, y=mean/6603.25*100)) + geom_point() + geom_errorbar(aes(ymax = upper/6603.25*100, ymin=lower/6603.25*100)) + ylab("CPU (%)") + xlab("Infrastructure") +  
    ggtitle(paste("CI of difference in CPU infra", sep="")) + expand_limits(y=0)+ theme(axis.text.x = element_text(angle = 90, hjust = 1))
  
}

PlotMemInfraCI <- function(fragmentations) {
  ggplot(fragmentations, aes(x=infra, y=mean/5862.751 * 100)) + geom_point() + geom_errorbar(aes(ymax = upper/5862.751* 100, ymin=lower/5862.751* 100)) + ylab("RAM (%)") + xlab("Infrastructure") +  
    ggtitle(paste("CI of difference in RAM infra", sep="")) + expand_limits(y=0) + expand_limits(y=0)+ theme(axis.text.x = element_text(angle = 90, hjust = 1))
  
}

PlotMemFragmentationCI <- function(fragmentations) { #}, constraintOn) {
  #if (constraintOn) {
    ggplot(fragmentations, aes(x=infra, y=mean * 100)) + geom_point() + geom_errorbar(aes(ymax = upper*100, ymin=lower*100)) + ylab("% of RAM idleness") + xlab("Infrastructure") +  
      ggtitle(paste("CI of difference in RAM idleness", sep=""))  + expand_limits(y=0)+ theme(axis.text.x = element_text(angle = 90, hjust = 1))
  #} else {
  #  ggplot(fragmentations, aes(x=infra, y=mean * 100)) + geom_point() + geom_errorbar(aes(ymax = upper*100, ymin=lower*100)) + ylab("% of RAM fragmentation") + xlab("Infrastructure") +  
  #    ggtitle(paste("RAM fragmentation not considering placement constraint ", sep=""))
  #}
}

PlotServersCI <- function(servers) {
  ggplot(servers, aes(x=infra, y=mean)) + geom_point() + geom_errorbar(aes(ymax = upper, ymin=lower)) + ylab("#servers") + xlab("Infrastructure") +  
    ggtitle(paste("CI of additional logical servers assembled", sep="")) + theme(axis.text.x = element_text(angle = 90, hjust = 1))
  
}

PlotPendingFractionCI <- function(pendingFraction) {
  ggplot(pendingFraction, aes(x=infra, y=mean * 100)) + geom_point() + geom_errorbar(aes(ymax = upper * 100, ymin=lower * 100)) + ylab("% of tasks") + xlab("Infrastructure") +  
    ggtitle(paste("CI of difference in % of tasks in pending queue", sep=""))  + expand_limits(y=0)
}

PlotPendingTasksCI <- function(pendingFraction) {
  ggplot(pendingFraction, aes(x=infra, y=mean)) + geom_point() + geom_errorbar(aes(ymax = upper, ymin=lower)) + ylab("#tasks") + xlab("Infrastructure") +  
    ggtitle(paste("% of tasks in pending queue", sep=""))  
}

PlotProdPendingFractionCI <- function(pendingFraction) {
  ggplot(pendingFraction, aes(x=infra, y=mean * 100)) + geom_point() + geom_errorbar(aes(ymax = upper * 100, ymin=lower * 100)) + ylab("% of prod tasks") + xlab("Infrastructure") +  
    ggtitle(paste("% of pending queue that is prod task", sep=""))  
}

PlotPendingCpuCI <- function(pendingCpuCI) {
  ggplot(pendingCpuCI, aes(x=infra, y=mean)) + geom_point() + geom_errorbar(aes(ymax = upper, ymin=lower)) + ylab("requested cpu") + xlab("Infrastructure") +  
    ggtitle(paste("CI of difference of cpu requested by tasks in pending queue", sep="")) + expand_limits(y=0) + theme(axis.text.x = element_text(angle = 90, hjust = 1))
}

PlotCpuOnQueueCI <- function(pendingCpuCI) {
  ggplot(pendingCpuCI, aes(x=infra, y=mean)) + geom_point() + geom_errorbar(aes(ymax = upper, ymin=lower)) + ylab("requested cpu (%)") + xlab("Infrastructure") +  
    ggtitle(paste("CI of difference of % of total cpu requested that are in pending queue", sep="")) + expand_limits(y=0) + theme(axis.text.x = element_text(angle = 90, hjust = 1))
}

PlotPendingMemCI <- function(pendingMemCI) {
  ggplot(pendingMemCI, aes(x=infra, y=mean)) + geom_point() + geom_errorbar(aes(ymax = upper, ymin=lower)) + ylab("requested RAM") + xlab("Infrastructure") +  
    ggtitle(paste("CI of difference of RAM requested by tasks in pending queue", sep="")) + expand_limits(y=0) + theme(axis.text.x = element_text(angle = 90, hjust = 1))
}

CollectAllocationDiffBetweenSBAndDH <- function(sbAllocation, dhAllocation){

  diff <- sbAllocation - dhAllocation
  return(diff)
}

CollectPendingDiffBetweenSBAndDH <- function(sbPending, dhPending){
  diff <- sbPending - dhPending
  return(diff)
}

GenerateCIInfo <- function(ConfidenceInterval) {
  error <- abs(ConfidenceInterval$upper) - abs(ConfidenceInterval$mean)
  return(paste(ConfidenceInterval$mean, " +- ", error, " -- error is ", error/ConfidenceInterval$mean, " of mean. ",  sep = ""))
}

GenerateWorkloadInfo <- function() {
  workloadInfo <- data.frame()
  
  for (time in 1:28) {
    #time <-1 
    submittedTasks <- read.table(paste("/local/giovanni/git/borg-simulator/timestamps/all-tasks-info-time-", time, "-for-java.csv", sep =""), sep = ",")
    colnames(submittedTasks) <- c("submitTime", "tid", "jid", "cpuReq", "memReq", "priority", "diffMachine")
    
    resourceDemand <- submittedTasks %>% summarise(timestamp = time, cpu.request=sum(cpuReq), mem.request=sum(memReq))
    
    workloadInfo <- rbind(workloadInfo, resourceDemand)
  }
  
  return(workloadInfo)
}

ProcessCPUData <- function() {
  allSBAllocationsOn <- CollectAllTimesSBAllocationInfo("experiment-results-more-grains/sb-based-results", constraintOn = T, allTasks = T)
  
  resultDir <- "experiment-results-more-grains/dh-based-results"
  grainVarying <- "cpu"
  grainVarying <- "ram"
  
  #blade
  allDHAllocBladeCPUMiniOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", resourceLabel = paste(grainVarying, "-mini", sep=""))
  allDHAllocBladeCPUSmallOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", resourceLabel = paste(grainVarying, "-small", sep=""))
  allDHAllocBladeCPUMediumOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", resourceLabel = paste(grainVarying, "-medium", sep=""))
  allDHAllocBladeCPULargeOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", resourceLabel = paste(grainVarying, "-large", sep=""))
  allDHAllocBladeCPUXLargeOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", resourceLabel = paste(grainVarying, "-xlarge", sep=""))
  
  
  #drawer
  allDHAllocDrawerCPUMiniOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", resourceLabel = paste(grainVarying, "-mini", sep=""))
  allDHAllocDrawerCPUSmallOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", resourceLabel = paste(grainVarying, "-small", sep=""))
  allDHAllocDrawerCPUMediumOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", resourceLabel = paste(grainVarying, "-medium", sep=""))
  allDHAllocDrawerCPULargeOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", resourceLabel = paste(grainVarying, "-large", sep=""))
  allDHAllocDrawerCPUXLargeOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", resourceLabel = paste(grainVarying, "-xlarge", sep=""))
  
  allDHAllocDrawerCPUMiniPreOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", resourceLabel = paste(grainVarying, "-mini-pre", sep=""))
  allDHAllocDrawerCPUMediumPreOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", resourceLabel = paste(grainVarying, "-medium-pre", sep=""))
  allDHAllocDrawerCPUXLargePreOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", resourceLabel = paste(grainVarying, "-xlarge-pre", sep=""))
  
  allDHAllocDrawerCPUMiniOff <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 16, dhModel = "drawer", resourceLabel = paste(grainVarying, "-mini-off", sep=""))
  allDHAllocDrawerCPUMediumOff <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 16, dhModel = "drawer", resourceLabel = paste(grainVarying, "-medium-off", sep=""))
  allDHAllocDrawerCPUXLargeOff <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 16, dhModel = "drawer", resourceLabel = paste(grainVarying, "-xlarge-off", sep=""))
  
  diffBladeCPUMiniOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocBladeCPUMiniOn)
  diffBladeCPUSmallOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocBladeCPUSmallOn)
  diffBladeCPUMediumOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocBladeCPUMediumOn)
  diffBladeCPULargeOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocBladeCPULargeOn)
  diffBladeCPUXLargeOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocBladeCPUXLargeOn)
  
  diffDrawerCPUMiniOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocDrawerCPUMiniOn)
  diffDrawerCPUSmallOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocDrawerCPUSmallOn)
  diffDrawerCPUMediumOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocDrawerCPUMediumOn)
  diffDrawerCPULargeOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocDrawerCPULargeOn)
  diffDrawerCPUXLargeOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocDrawerCPUXLargeOn)
  
  diffDrawerCPUMiniPreOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocDrawerCPUMiniPreOn)
  diffDrawerCPUMediumPreOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocDrawerCPUMediumPreOn)
  diffDrawerCPUXLargePreOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocDrawerCPUXLargePreOn)
  
  diffDrawerCPUMiniOff <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocDrawerCPUMiniOff)
  diffDrawerCPUMediumOff <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocDrawerCPUMediumOff)
  diffDrawerCPUXLargeOff <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocDrawerCPUXLargeOff)

  # cpu fragmentation
  cpuFragmentationsDiff <- data.frame()
  cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = paste("blade-micro", sep=""), CalculateCpuFragmentationCI(diffBladeCPUMiniOn)))
  cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = paste("blade-small", sep=""), CalculateCpuFragmentationCI(diffBladeCPUSmallOn)))
  cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = paste("blade-medium", sep=""), CalculateCpuFragmentationCI(diffBladeCPUMediumOn)))
  cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = paste("blade-large", sep=""), CalculateCpuFragmentationCI(diffBladeCPULargeOn)))
  cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = paste("blade-xlarge", sep=""), CalculateCpuFragmentationCI(diffBladeCPUXLargeOn)))
  
  cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = paste("drawer-micro", sep=""), CalculateCpuFragmentationCI(diffDrawerCPUMiniOn)))
  cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = paste("drawer-small", sep=""), CalculateCpuFragmentationCI(diffDrawerCPUSmallOn)))
  cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = paste("drawer-medium", sep=""), CalculateCpuFragmentationCI(diffDrawerCPUMediumOn)))
  cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = paste("drawer-large", sep=""), CalculateCpuFragmentationCI(diffDrawerCPULargeOn)))
  cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = paste("drawer-xlarge", sep=""), CalculateCpuFragmentationCI(diffDrawerCPUXLargeOn)))
  
  cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = paste("drawer-micro-pre", sep=""), CalculateCpuFragmentationCI(diffDrawerCPUMiniPreOn)))
  cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = paste("drawer-medium-pre", sep=""), CalculateCpuFragmentationCI(diffDrawerCPUMediumPreOn)))
  cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = paste("drawer-xlarge-pre", sep=""), CalculateCpuFragmentationCI(diffDrawerCPUXLargePreOn)))
  
  cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = paste("drawer-micro-off", sep=""), CalculateCpuFragmentationCI(diffDrawerCPUMiniOff)))
  cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = paste("drawer-medium-off", sep=""), CalculateCpuFragmentationCI(diffDrawerCPUMediumOff)))
  cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = paste("drawer-xlarge-off", sep=""), CalculateCpuFragmentationCI(diffDrawerCPUXLargeOff)))
  
  
  PlotCpuFragmentationCI(cpuFragmentationsDiff)
  
  #mem fragmentation
  memFragmentationsDiff <- data.frame()
  memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(infra = paste("blade-micro", sep=""), CalculateMemFragmentationCI(diffBladeCPUMiniOn)))
  memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(infra = paste("blade-small", sep=""), CalculateMemFragmentationCI(diffBladeCPUSmallOn)))
  memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(infra = paste("blade-medium", sep=""), CalculateMemFragmentationCI(diffBladeCPUMediumOn)))
  memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(infra = paste("blade-large", sep=""), CalculateMemFragmentationCI(diffBladeCPULargeOn)))
  memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(infra = paste("blade-xlarge", sep=""), CalculateMemFragmentationCI(diffBladeCPUXLargeOn)))

  memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(infra = paste("drawer-micro", sep=""), CalculateMemFragmentationCI(diffDrawerCPUMiniOn)))
  memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(infra = paste("drawer-small", sep=""), CalculateMemFragmentationCI(diffDrawerCPUSmallOn)))
  memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(infra = paste("drawer-medium", sep=""), CalculateMemFragmentationCI(diffDrawerCPUMediumOn)))
  memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(infra = paste("drawer-large", sep=""), CalculateMemFragmentationCI(diffDrawerCPULargeOn)))
  memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(infra = paste("drawer-xlarge", sep=""), CalculateMemFragmentationCI(diffDrawerCPUXLargeOn)))
  
  PlotMemFragmentationCI(memFragmentationsDiff)
  
  cpuInfraDiff <- data.frame()
  cpuInfraDiff <- rbind(cpuInfraDiff, data.frame(infra = paste("blade-micro", sep=""), CalculateInfraCPUCI(diffBladeCPUMiniOn)))
  cpuInfraDiff <- rbind(cpuInfraDiff, data.frame(infra = paste("blade-small", sep=""), CalculateInfraCPUCI(diffBladeCPUSmallOn)))
  cpuInfraDiff <- rbind(cpuInfraDiff, data.frame(infra = paste("blade-medium", sep=""), CalculateInfraCPUCI(diffBladeCPUMediumOn)))
  cpuInfraDiff <- rbind(cpuInfraDiff, data.frame(infra = paste("blade-large", sep=""), CalculateInfraCPUCI(diffBladeCPULargeOn)))
  cpuInfraDiff <- rbind(cpuInfraDiff, data.frame(infra = paste("blade-xlarge", sep=""), CalculateInfraCPUCI(diffBladeCPUXLargeOn)))
  
  cpuInfraDiff <- rbind(cpuInfraDiff, data.frame(infra = paste("drawer-micro", sep=""), CalculateInfraCPUCI(diffDrawerCPUMiniOn)))
  cpuInfraDiff <- rbind(cpuInfraDiff, data.frame(infra = paste("drawer-small", sep=""), CalculateInfraCPUCI(diffDrawerCPUSmallOn)))
  cpuInfraDiff <- rbind(cpuInfraDiff, data.frame(infra = paste("drawer-medium", sep=""), CalculateInfraCPUCI(diffDrawerCPUMediumOn)))
  cpuInfraDiff <- rbind(cpuInfraDiff, data.frame(infra = paste("drawer-large", sep=""), CalculateInfraCPUCI(diffDrawerCPULargeOn)))
  cpuInfraDiff <- rbind(cpuInfraDiff, data.frame(infra = paste("drawer-xlarge", sep=""), CalculateInfraCPUCI(diffDrawerCPUXLargeOn)))
  
  PlotCpuInfraCI(cpuInfraDiff)
  
  GenerateCIInfo(CalculateInfraCPUCI(diffBladeSmallOn))
  
  memInfraDiff <- data.frame()
  memInfraDiff <- rbind(memInfraDiff, data.frame(infra = paste("blade-micro", sep=""), CalculateInfraMemCI(diffBladeCPUMiniOn)))
  memInfraDiff <- rbind(memInfraDiff, data.frame(infra = paste("blade-small", sep=""), CalculateInfraMemCI(diffBladeCPUSmallOn)))
  memInfraDiff <- rbind(memInfraDiff, data.frame(infra = paste("blade-medium", sep=""), CalculateInfraMemCI(diffBladeCPUMediumOn)))
  memInfraDiff <- rbind(memInfraDiff, data.frame(infra = paste("blade-large", sep=""), CalculateInfraMemCI(diffBladeCPULargeOn)))
  memInfraDiff <- rbind(memInfraDiff, data.frame(infra = paste("blade-xlarge", sep=""), CalculateInfraMemCI(diffBladeCPUXLargeOn)))
  
  memInfraDiff <- rbind(memInfraDiff, data.frame(infra = paste("drawer-micro", sep=""), CalculateInfraMemCI(diffDrawerCPUMiniOn)))
  memInfraDiff <- rbind(memInfraDiff, data.frame(infra = paste("drawer-small", sep=""), CalculateInfraMemCI(diffDrawerCPUSmallOn)))
  memInfraDiff <- rbind(memInfraDiff, data.frame(infra = paste("drawer-medium", sep=""), CalculateInfraMemCI(diffDrawerCPUMediumOn)))
  memInfraDiff <- rbind(memInfraDiff, data.frame(infra = paste("drawer-large", sep=""), CalculateInfraMemCI(diffDrawerCPULargeOn)))
  memInfraDiff <- rbind(memInfraDiff, data.frame(infra = paste("drawer-xlarge", sep=""), CalculateInfraMemCI(diffDrawerCPUXLargeOn)))
  
  PlotMemInfraCI(memInfraDiff)
  
  # instead of diffe between SB and SH, we should plot the additional number of hosts
  adittionalServers <- data.frame()
  adittionalServers<- rbind(adittionalServers, data.frame(infra = paste("blade-micro", sep=""), CalculateAdittionalServersCI(allDHAllocBladeCPUMiniOn, 12477)))
  adittionalServers<- rbind(adittionalServers, data.frame(infra = paste("blade-small", sep=""), CalculateAdittionalServersCI(allDHAllocBladeCPUSmallOn, 12477)))
  adittionalServers<- rbind(adittionalServers, data.frame(infra = paste("blade-medium", sep=""), CalculateAdittionalServersCI(allDHAllocBladeCPUMediumOn, 12477)))
  adittionalServers<- rbind(adittionalServers, data.frame(infra = paste("blade-large", sep=""), CalculateAdittionalServersCI(allDHAllocBladeCPULargeOn, 12477)))
  adittionalServers<- rbind(adittionalServers, data.frame(infra = paste("blade-xlarge", sep=""), CalculateAdittionalServersCI(allDHAllocBladeCPUXLargeOn, 12477)))
  
  adittionalServers<- rbind(adittionalServers, data.frame(infra = paste("drawer-micro", sep=""), CalculateAdittionalServersCI(allDHAllocDrawerCPUMiniOn, 434)))
  adittionalServers<- rbind(adittionalServers, data.frame(infra = paste("drawer-small", sep=""), CalculateAdittionalServersCI(allDHAllocDrawerCPUSmallOn, 434)))
  adittionalServers<- rbind(adittionalServers, data.frame(infra = paste("drawer-medium", sep=""), CalculateAdittionalServersCI(allDHAllocDrawerCPUMediumOn, 434)))
  adittionalServers<- rbind(adittionalServers, data.frame(infra = paste("drawer-large", sep=""), CalculateAdittionalServersCI(allDHAllocDrawerCPULargeOn, 434)))
  adittionalServers<- rbind(adittionalServers, data.frame(infra = paste("drawer-xlarge", sep=""), CalculateAdittionalServersCI(allDHAllocDrawerCPUXLargeOn, 434)))
  
  adittionalServers<- rbind(adittionalServers, data.frame(infra = paste("drawer-micro-pre", sep=""), CalculateAdittionalServersCI(allDHAllocDrawerCPUMiniPreOn, 434)))
  adittionalServers<- rbind(adittionalServers, data.frame(infra = paste("drawer-medium-pre", sep=""), CalculateAdittionalServersCI(allDHAllocDrawerCPUMediumPreOn, 434)))
  adittionalServers<- rbind(adittionalServers, data.frame(infra = paste("drawer-xlarge-pre", sep=""), CalculateAdittionalServersCI(allDHAllocDrawerCPUXLargePreOn, 434)))
  
  adittionalServers<- rbind(adittionalServers, data.frame(infra = paste("drawer-micro-off", sep=""), CalculateAdittionalServersCI(allDHAllocDrawerCPUMiniOff, 434)))
  adittionalServers<- rbind(adittionalServers, data.frame(infra = paste("drawer-medium-off", sep=""), CalculateAdittionalServersCI(allDHAllocDrawerCPUMediumOff, 434)))
  adittionalServers<- rbind(adittionalServers, data.frame(infra = paste("drawer-xlarge-off", sep=""), CalculateAdittionalServersCI(allDHAllocDrawerCPUXLargeOff, 434)))
  
  
  PlotServersCI(adittionalServers)
}


ProcessPendingCPUData <- function() {
  nAllTasks <- read.csv("number_of_tasks.txt")
  nProdTasks <- read.csv("number_of_prod_tasks.txt")
  resourceDemand <- GenerateWorkloadInfo()
  
  allSBPendingInfoOn <- CollectAllTimesSBPendingInfo("experiment-results-more-grains/sb-based-results", constraintOn = T, allTasks = T, nAllTasks, resourceDemand)
  
  resultDir <- "experiment-results-more-grains/dh-based-results"
  grainVarying <- "cpu"
  grainVarying <- "ram"
  
  allDHPendingBladeMiniOn <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", nAllTasks, resourceLabel = paste(grainVarying, "-mini", sep=""), resourceDemand)
  allDHPendingBladeSmallOn <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", nAllTasks, resourceLabel = paste(grainVarying, "-small", sep=""), resourceDemand)
  allDHPendingBladeMediumOn <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", nAllTasks, resourceLabel = paste(grainVarying, "-medium", sep=""), resourceDemand)
  allDHPendingBladeLargeOn <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", nAllTasks, resourceLabel = paste(grainVarying, "-large", sep=""), resourceDemand)
  allDHPendingBladeXLargeOn <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", nAllTasks, resourceLabel = paste(grainVarying, "-xlarge", sep=""), resourceDemand)
  
  workloadInfo <- data.frame()
  workloadInfo <- allDHAllocBladeCPUMiniOn$infra.cpu - allDHAllocBladeCPUMiniOn$infra.freeCpu + allDHPendingBladeMiniOn$total.cpu

  allDHPendingDrawerMiniOn <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", nAllTasks, resourceLabel = paste(grainVarying, "-mini", sep=""), resourceDemand)
  allDHPendingDrawerSmallOn <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", nAllTasks, resourceLabel = paste(grainVarying, "-small", sep=""), resourceDemand)
  allDHPendingDrawerMediumOn <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", nAllTasks, resourceLabel = paste(grainVarying, "-medium", sep=""), resourceDemand)
  allDHPendingDrawerLargeOn <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", nAllTasks, resourceLabel = paste(grainVarying, "-large", sep=""), resourceDemand)
  allDHPendingDrawerXLargeOn <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", nAllTasks, resourceLabel = paste(grainVarying, "-xlarge", sep=""), resourceDemand)
  
  diffPendBladeMiniOn <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingBladeMiniOn)
  diffPendBladeSmallOn <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingBladeSmallOn)
  diffPendBladeMediumOn <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingBladeMediumOn)
  diffPendBladeLargeOn <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingBladeLargeOn)
  diffPendBladeXLargeOn <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingBladeXLargeOn)
  
  diffPendDrawerMiniOn <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingDrawerMiniOn)
  diffPendDrawerSmallOn <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingDrawerSmallOn)
  diffPendDrawerMediumOn <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingDrawerMediumOn)
  diffPendDrawerLargeOn <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingDrawerLargeOn)
  diffPendDrawerXLargeOn <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingDrawerXLargeOn)

  # pending queue fraction
  pendingFractionDiff <- data.frame()
  pendingFractionDiff<- rbind(pendingFractionDiff, data.frame(infra = "blade-on-small", CalculatePendingFractionCI(diffPendBladeSmallOn)))
  
  
  PlotPendingFractionCI(pendingFractionDiff)
  
  pendingCpuCIDiff <- data.frame()
  pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = paste("blade-micro", sep=""), CalculatePendingCpuCI(diffPendBladeMiniOn)))
  pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = paste("blade-small", sep=""), CalculatePendingCpuCI(diffPendBladeSmallOn)))
  pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = paste("blade-medium", sep=""), CalculatePendingCpuCI(diffPendBladeMediumOn)))
  pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = paste("blade-large", sep=""), CalculatePendingCpuCI(diffPendBladeLargeOn)))
  pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = paste("blade-xlarge", sep=""), CalculatePendingCpuCI(diffPendBladeXLargeOn)))
  
  pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = paste("drawer-micro", sep=""), CalculatePendingCpuCI(diffPendDrawerMiniOn)))
  pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = paste("drawer-small", sep=""), CalculatePendingCpuCI(diffPendDrawerSmallOn)))
  pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = paste("drawer-medium", sep=""), CalculatePendingCpuCI(diffPendDrawerMediumOn)))
  pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = paste("drawer-large", sep=""), CalculatePendingCpuCI(diffPendDrawerLargeOn)))
  pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = paste("drawer-xlarge", sep=""), CalculatePendingCpuCI(diffPendDrawerXLargeOn)))

  pendingCpuCIDiff <- data.frame()
  pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = paste("blade-micro", sep=""), CalculateCpuOnQueueCI(diffPendBladeMiniOn)))
  pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = paste("blade-small", sep=""), CalculateCpuOnQueueCI(diffPendBladeSmallOn)))
  pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = paste("blade-medium", sep=""), CalculateCpuOnQueueCI(diffPendBladeMediumOn)))
  pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = paste("blade-large", sep=""), CalculateCpuOnQueueCI(diffPendBladeLargeOn)))
  pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = paste("blade-xlarge", sep=""), CalculateCpuOnQueueCI(diffPendBladeXLargeOn)))
  
  pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = paste("drawer-micro", sep=""), CalculateCpuOnQueueCI(diffPendDrawerMiniOn)))
  pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = paste("drawer-small", sep=""), CalculateCpuOnQueueCI(diffPendDrawerSmallOn)))
  pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = paste("drawer-medium", sep=""), CalculateCpuOnQueueCI(diffPendDrawerMediumOn)))
  pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = paste("drawer-large", sep=""), CalculateCpuOnQueueCI(diffPendDrawerLargeOn)))
  pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = paste("drawer-xlarge", sep=""), CalculateCpuOnQueueCI(diffPendDrawerXLargeOn)))
  
  
  PlotPendingCpuCI(pendingCpuCIDiff)
  
  pendingMemCIDiff <- data.frame()
  pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(infra = paste("blade-micro", sep=""), CalculatePendingMemCI(diffPendBladeMiniOn)))
  pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(infra = paste("blade-small", sep=""), CalculatePendingMemCI(diffPendBladeSmallOn)))
  pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(infra = paste("blade-medium", sep=""), CalculatePendingMemCI(diffPendBladeMediumOn)))
  pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(infra = paste("blade-large", sep=""), CalculatePendingMemCI(diffPendBladeLargeOn)))
  pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(infra = paste("blade-xlarge", sep=""), CalculatePendingMemCI(diffPendBladeXLargeOn)))
  
  pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(infra = paste("drawer-micro", sep=""), CalculatePendingMemCI(diffPendDrawerMiniOn)))
  pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(infra = paste("drawer-small", sep=""), CalculatePendingMemCI(diffPendDrawerSmallOn)))
  pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(infra = paste("drawer-medium", sep=""), CalculatePendingMemCI(diffPendDrawerMediumOn)))
  pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(infra = paste("drawer-large", sep=""), CalculatePendingMemCI(diffPendDrawerLargeOn)))
  pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(infra = paste("drawer-xlarge", sep=""), CalculatePendingMemCI(diffPendDrawerXLargeOn)))

  PlotPendingMemCI(pendingMemCIDiff)
  
}

nAllTasks <- read.csv("number_of_tasks.txt")
nProdTasks <- read.csv("number_of_prod_tasks.txt")

allSBAllocationsOn <- CollectAllTimesSBAllocationInfo("experiment-results-free-not-create-LS/sb-based-results", constraintOn = T, allTasks = T)
allSBPendingInfoOn <- CollectAllTimesSBPendingInfo("experiment-results-free-not-create-LS/sb-based-results", constraintOn = T, allTasks = T, nAllTasks)

allSBAllocationsOff <- CollectAllTimesSBAllocationInfo("experiment-results/sb-based-results", constraintOn = F, allTasks = T)
allSBPendingInfoOff <- CollectAllTimesSBPendingInfo("experiment-results/sb-based-results", constraintOn = F, allTasks = T, nAllTasks)


allSBProdAllocations <- CollectAllTimesSBAllocationInfo("experiment-results/sb-based-results", constraintOn = T, allTasks = F)
allSBProdPendingInfo <- CollectAllTimesSBPendingInfo("experiment-results/sb-based-results", constraintOn = T, allTasks = F, nProdAllTasks)

allSBProdAllocationsOff <- CollectAllTimesSBAllocationInfo("experiment-results/sb-based-results", constraintOn = F, allTasks = F)
allSBProdPendingInfoOff <- CollectAllTimesSBPendingInfo("experiment-results/sb-based-results", constraintOn = F, allTasks = F, nProdTasks)


resultDir <- "experiment-results-free-not-create-LS/dh-based-results"
resultDir <- "experiment-results/dh-based-results"
resultDir <- "experiment-results-more-grains/dh-based-results"

allocation <- CollectAllocationInfo("experiment-results-more-grains/dh-based-results/time3/cpu-small/allocation-on-blade-1-1-12713-servers.csv")
allocation <- CollectAllocationInfo("experiment-results-free-not-create-LS/dh-based-results/time2/small-grain/allocation-on-blade-1-1-12713-servers.csv")
allocation <- CollectAllocationInfo("test-results-server-size-1907/allocation-on-blade-1-1-12761-servers.csv")
allocation <- read.csv("test-results-server-size-1907/allocation-on-unlimited-6605-6605-9497-servers.csv")
allocation <- read.csv("test-results-server-size-1907anti-off/allocation-on-unlimited-6605-6605-34-servers.csv")
allocation <- read.csv("test-results-server-size-1907anti-on-const-off/allocation-off-unlimited-6605-6605-6068-servers.csv")

allocation <- read.csv("experiment-results-free-not-create-LS/dh-based-results/time2/small-grain/allocation-on-drawer-16-16-8996-servers.csv")
allocation2 <- read.csv("experiment-results-free-not-create-LS/dh-based-results/time2/small-grain/allocation-on-blade-1-1-12713-servers.csv")

allocation %>% filter(cpuCapacity > 10) 
allocation %>% filter(cpuPoolId == "cpu-pool:[o/=0;rs=0;By=4;wN=2;P8=0;w2=1;9e=2;nZ=2;w5=1]") %>% summary()
                                    
summary(allocation)

cp <-allocation %>% group_by(cpuPoolId) %>% dplyr::summarise(servers=n(), total.cpu=sum(cpuCapacity)) %>% dplyr::summarise(total=sum(total.cpu))
data.frame(cp)


allDHAllocBladeCPUMediumOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", resourceLabel = "cpu-medium")
allDHAllocBladeCPULargeOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", resourceLabel = "cpu-large")
allDHAllocBladeCPUXLargeOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", resourceLabel = "cpu-xlarge")

# collecting and ploting the CI od the difference
allSBAllocationsOn <- CollectAllTimesSBAllocationInfo("experiment-results-free-not-create-LS/sb-based-results", constraintOn = T, allTasks = T)
allSBAllocationsOff <- CollectAllTimesSBAllocationInfo("experiment-results-free-not-create-LS/sb-based-results", constraintOn = F, allTasks = T)

#small on
allDHAllocBladeSmallOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade")
allDHAllocBladeSmallOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", resourceLabel = "small-grain")
allDHAllocDrawerSmallOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", resourceLabel = "small-grain")
allDHAllocDrawerPreSmallOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", resourceLabel = "small-grain-pre")
allDHAllocDrawerPreSmallOff <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 16, dhModel = "drawer", resourceLabel = "small-grain-pre")

allDHAllocBladeSmallOff <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 1, dhModel = "blade", resourceLabel = "small-grain")

#large on
allDHAllocBladeLargeOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", resourceLabel = "large-grain")
allDHAllocDrawerLargeOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", resourceLabel = "large-grain")
allDHAllocDrawerPreLargeOn <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", resourceLabel = "large-grain-pre")
allDHAllocDrawerPreLargeOff <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 16, dhModel = "drawer", resourceLabel = "large-grain-pre")

allDHAllocBladeLargeOff <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 1, dhModel = "blade", resourceLabel = "large-grain")

diffBladeSmallOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocBladeSmallOn)
diffBladeLargeOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocBladeLargeOn)
diffDrawerSmallOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocDrawerSmallOn)
diffDrawerPreSmallOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocDrawerPreSmallOn)
diffDrawerLargeOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocDrawerLargeOn)
diffDrawerPreLargeOn <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocDrawerPreLargeOn)


diffBladeSmallOff <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOff, allDHAllocBladeSmallOff)
diffBladeLargeOff <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOff, allDHAllocBladeLargeOff)

diffDrawerPreSmallOff <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocDrawerPreSmallOff)
diffDrawerPreLargeOff <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocDrawerPreLargeOff)

# cpu fragmentation
cpuFragmentationsDiff <- data.frame()
cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = "blade-on-small", CalculateCpuFragmentationCI(diffBladeSmallOn)))
cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = "blade-on-large", CalculateCpuFragmentationCI(diffBladeLargeOn)))
cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = "drawer-on-small", CalculateCpuFragmentationCI(diffDrawerSmallOn)))
cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = "drawer-on-large", CalculateCpuFragmentationCI(diffDrawerLargeOn)))
cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = "drawer-pre-on-small", CalculateCpuFragmentationCI(diffDrawerPreSmallOn)))
cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = "drawer-pre-on-large", CalculateCpuFragmentationCI(diffDrawerPreLargeOn)))

PlotCpuFragmentationCI(cpuFragmentationsDiff)

GenerateCIInfo(CalculateInfraCPUCI(diffBladeSmallOn))
GenerateCIInfo(CalculateInfraMemCI(diffBladeSmallOn))

GenerateCIInfo(CalculateInfraCPUCI(diffDrawerSmallOn))
GenerateCIInfo(CalculateInfraMemCI(diffDrawerSmallOn))

GenerateCIInfo(CalculateInfraCPUCI(diffBladeLargeOn))
GenerateCIInfo(CalculateInfraMemCI(diffBladeLargeOn))


GenerateCIInfo(CalculateInfraCPUCI(diffDrawerLargeOn))
GenerateCIInfo(CalculateInfraMemCI(diffDrawerLargeOn))


GenerateCIInfo(CalculateCpuFragmentationCI(diffBladeSmallOn))
GenerateCIInfo(CalculateMemFragmentationCI(diffBladeSmallOn))
GenerateCIInfo(CalculateServersCI(diffBladeSmallOn))
GenerateCIInfo(CalculatePendingFractionCI(diffPendBladeSmallOn))
GenerateCIInfo(CalculatePendingCpuCI(diffPendBladeSmallOn))
GenerateCIInfo(CalculatePendingMemCI(diffPendBladeSmallOn))

GenerateCIInfo(CalculateCpuFragmentationCI(diffDrawerSmallOn))
GenerateCIInfo(CalculateMemFragmentationCI(diffDrawerSmallOn))
GenerateCIInfo(CalculatePendingFractionCI(diffPendDrawerSmallOn))
GenerateCIInfo(CalculatePendingCpuCI(diffPendDrawerSmallOn))
GenerateCIInfo(CalculatePendingMemCI(diffPendDrawerSmallOn))
GenerateCIInfo(CalculateServersCI(diffDrawerSmallOn))

GenerateCIInfo(CalculateCpuFragmentationCI(diffBladeLargeOn))
GenerateCIInfo(CalculateMemFragmentationCI(diffBladeLargeOn))
GenerateCIInfo(CalculatePendingFractionCI(diffPendBladeLargeOn))
GenerateCIInfo(CalculatePendingCpuCI(diffPendBladeLargeOn))
GenerateCIInfo(CalculatePendingMemCI(diffPendBladeLargeOn))
GenerateCIInfo(CalculateServersCI(diffBladeLargeOn))


GenerateCIInfo(CalculateCpuFragmentationCI(diffDrawerLargeOn))
GenerateCIInfo(CalculateMemFragmentationCI(diffDrawerLargeOn))
GenerateCIInfo(CalculatePendingFractionCI(diffPendDrawerLargeOn))
GenerateCIInfo(CalculatePendingCpuCI(diffPendDrawerLargeOn))
GenerateCIInfo(CalculatePendingMemCI(diffPendDrawerLargeOn))
GenerateCIInfo(CalculateServersCI(diffDrawerLargeOn))


GenerateCIInfo(CalculateCpuFragmentationCI(diffDrawerPreSmallOn))
GenerateCIInfo(CalculateMemFragmentationCI(diffDrawerPreSmallOn))
GenerateCIInfo(CalculatePendingFractionCI(diffPendDrawerPreSmallOn))
GenerateCIInfo(CalculatePendingCpuCI(diffPendDrawerPreSmallOn))
GenerateCIInfo(CalculatePendingMemCI(diffPendDrawerPreSmallOn))
GenerateCIInfo(CalculateServersCI(diffDrawerPreSmallOn))

GenerateCIInfo(CalculateCpuFragmentationCI(diffDrawerPreLargeOn))
GenerateCIInfo(CalculateMemFragmentationCI(diffDrawerPreLargeOn))
GenerateCIInfo(CalculatePendingFractionCI(diffPendDrawerPreLargeOn))
GenerateCIInfo(CalculatePendingCpuCI(diffPendDrawerPreLargeOn))
GenerateCIInfo(CalculatePendingMemCI(diffPendDrawerPreLargeOn))
GenerateCIInfo(CalculateServersCI(diffDrawerPreLargeOn))


GenerateCIInfo(CalculateCpuFragmentationCI(diffDrawerPreSmallOff))
GenerateCIInfo(CalculateMemFragmentationCI(diffDrawerPreSmallOff))
GenerateCIInfo(CalculatePendingFractionCI(diffPendDrawerPreSmallOff))
GenerateCIInfo(CalculatePendingCpuCI(diffPendDrawerPreSmallOff))
GenerateCIInfo(CalculatePendingMemCI(diffPendDrawerPreSmallOff))
GenerateCIInfo(CalculateServersCI(diffDrawerPreSmallOff))


GenerateCIInfo(CalculateCpuFragmentationCI(diffDrawerPreLargeOff))
GenerateCIInfo(CalculateMemFragmentationCI(diffDrawerPreLargeOff))
GenerateCIInfo(CalculatePendingFractionCI(diffPendDrawerPreLargeOff))
GenerateCIInfo(CalculatePendingCpuCI(diffPendDrawerPreLargeOff))
GenerateCIInfo(CalculatePendingMemCI(diffPendDrawerPreLargeOff))
GenerateCIInfo(CalculateServersCI(diffDrawerPreLargeOff))



cpuFragmentationsDiff <- data.frame()
cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = "blade-off-small", CalculateCpuFragmentationCI(diffBladeSmallOff)))
cpuFragmentationsDiff <- rbind(cpuFragmentationsDiff, data.frame(infra = "blade-off-large", CalculateCpuFragmentationCI(diffBladeLargeOff)))

PlotCpuFragmentationCI(cpuFragmentationsDiff)

#mem fragmentation
memFragmentationsDiff <- data.frame()
memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(infra = "blade-on-small", CalculateMemFragmentationCI(diffBladeSmallOn)))
memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(infra = "blade-on-large", CalculateMemFragmentationCI(diffBladeLargeOn)))
memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(infra = "drawer-on-small", CalculateMemFragmentationCI(diffDrawerSmallOn)))
memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(infra = "drawer-on-large", CalculateMemFragmentationCI(diffDrawerLargeOn)))
memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(infra = "drawer-pre-on-small", CalculateMemFragmentationCI(diffDrawerPreSmallOn)))
memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(infra = "drawer-pre-on-large", CalculateMemFragmentationCI(diffDrawerPreLargeOn)))

PlotMemFragmentationCI(memFragmentationsDiff)

memFragmentationsDiff <- data.frame()
memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(infra = "blade-off-small", CalculateMemFragmentationCI(diffBladeSmallOff)))
memFragmentationsDiff <- rbind(memFragmentationsDiff, data.frame(infra = "blade-off-large", CalculateMemFragmentationCI(diffBladeLargeOff)))

serversDiff <- data.frame()
serversDiff<- rbind(serversDiff, data.frame(infra = "blade-on-small", CalculateServersCI(diffBladeSmallOn)))
serversDiff<- rbind(serversDiff, data.frame(infra = "blade-on-large", CalculateServersCI(diffBladeLargeOn)))
serversDiff<- rbind(serversDiff, data.frame(infra = "drawer-on-small", CalculateServersCI(diffDrawerSmallOn)))
serversDiff<- rbind(serversDiff, data.frame(infra = "drawer-on-large", CalculateServersCI(diffDrawerLargeOn)))
serversDiff<- rbind(serversDiff, data.frame(infra = "drawer-pre-on-small", CalculateServersCI(diffDrawerPreSmallOn)))
serversDiff<- rbind(serversDiff, data.frame(infra = "drawer-pre-on-large", CalculateServersCI(diffDrawerPreLargeOn)))

PlotServersCI(serversDiff)


#Pending queue
allSBPendingInfoOn <- CollectAllTimesSBPendingInfo("experiment-results-free-not-create-LS/sb-based-results", constraintOn = T, allTasks = T, nAllTasks)


GenerateCIInfo(CalculateProdPendingFractionCI(allSBPendingInfoOn))

allDHPendingBladeSmallOn <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", nAllTasks, resourceLabel = "small-grain")
allDHPendingBladeLargeOn <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade", nAllTasks, resourceLabel = "large-grain")
allDHPendingDrawerSmallOn <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", nAllTasks, resourceLabel = "small-grain")
allDHPendingDrawerLargeOn <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", nAllTasks, resourceLabel = "large-grain")
allDHPendingDrawerPreSmallOn <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", nAllTasks, resourceLabel = "small-grain-pre")
allDHPendingDrawerPreLargeOn <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, dhModel = "drawer", nAllTasks, resourceLabel = "large-grain-pre")
allDHPendingDrawerPreSmallOff <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 16, dhModel = "drawer", nAllTasks, resourceLabel = "small-grain-pre")
allDHPendingDrawerPreLargeOff <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 16, dhModel = "drawer", nAllTasks, resourceLabel = "large-grain-pre")



allDHPendingBladeSmallOff <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 1, dhModel = "blade", nAllTasks)
allDHPendingBladeLargeOff <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 1, dhModel = "blade", nAllTasks, resourceLabel = "large-grain")

diffPendBladeSmallOn <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingBladeSmallOn)
diffPendBladeLargeOn <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingBladeLargeOn)
diffPendDrawerSmallOn <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingDrawerSmallOn)
diffPendDrawerLargeOn <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingDrawerLargeOn)
diffPendDrawerPreSmallOn <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingDrawerPreSmallOn)
diffPendDrawerPreLargeOn <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingDrawerPreLargeOn)

diffPendBladeSmallOff <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingBladeSmallOff)
diffPendBladeLargeOff <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingBladeLargeOff)

diffPendDrawerPreSmallOff <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingDrawerPreSmallOff)
diffPendDrawerPreLargeOff <- CollectPendingDiffBetweenSBAndDH(allSBPendingInfoOn, allDHPendingDrawerPreLargeOff)


# pending queue fraction
pendingFractionDiff <- data.frame()
pendingFractionDiff<- rbind(pendingFractionDiff, data.frame(infra = "blade-on-small", CalculatePendingFractionCI(diffPendBladeSmallOn)))
pendingFractionDiff<- rbind(pendingFractionDiff, data.frame(infra = "blade-on-large", CalculatePendingFractionCI(diffPendBladeLargeOn)))
pendingFractionDiff<- rbind(pendingFractionDiff, data.frame(infra = "drawer-on-small", CalculatePendingFractionCI(diffPendDrawerSmallOn)))
pendingFractionDiff<- rbind(pendingFractionDiff, data.frame(infra = "drawer-on-large", CalculatePendingFractionCI(diffPendDrawerLargeOn)))
pendingFractionDiff<- rbind(pendingFractionDiff, data.frame(infra = "drawer-pre-on-small", CalculatePendingFractionCI(diffPendDrawerPreSmallOn)))
pendingFractionDiff<- rbind(pendingFractionDiff, data.frame(infra = "drawer-pre-on-large", CalculatePendingFractionCI(diffPendDrawerPreLargeOn)))

PlotPendingFractionCI(pendingFractionDiff)

pendingCpuCIDiff <- data.frame()
pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = "blade-on-small", CalculatePendingCpuCI(diffPendBladeSmallOn)))
pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = "blade-on-large", CalculatePendingCpuCI(diffPendBladeLargeOn)))
pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = "drawer-on-small", CalculatePendingCpuCI(diffPendDrawerSmallOn)))
pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = "drawer-on-large", CalculatePendingCpuCI(diffPendDrawerLargeOn)))
pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = "drawer-pre-on-small", CalculatePendingCpuCI(diffPendDrawerPreSmallOn)))
pendingCpuCIDiff<- rbind(pendingCpuCIDiff, data.frame(infra = "drawer-pre-on-large", CalculatePendingCpuCI(diffPendDrawerPreLargeOn)))

PlotPendingCpuCI(pendingCpuCIDiff)

pendingMemCIDiff <- data.frame()
pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(infra = "blade-on-small", CalculatePendingMemCI(diffPendBladeSmallOn)))
pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(infra = "blade-on-large", CalculatePendingMemCI(diffPendBladeLargeOn)))
pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(infra = "drawer-on-small", CalculatePendingMemCI(diffPendDrawerSmallOn)))
pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(infra = "drawer-on-large", CalculatePendingMemCI(diffPendDrawerLargeOn)))
pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(infra = "drawer-pre-on-small", CalculatePendingMemCI(diffPendDrawerPreSmallOn)))
pendingMemCIDiff<- rbind(pendingMemCIDiff, data.frame(infra = "drawer-pre-on-large", CalculatePendingMemCI(diffPendDrawerPreLargeOn)))

PlotPendingMemCI(pendingMemCIDiff)

IC <- CalculatePendingMemCI(diffPendBladeSmallOn)

error <- abs(IC$upper) - abs(IC$mean)

paste(IC$mean, " +- ", error, " -- error is ", error/IC$mean, " of mean. ",  sep = "")

# collecting allocation info constraint on
allDHAllocationsOn1_1 <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, dhModel = "blade")
allDHAllocationsOff1_1 <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 1, dhModel = "blade")

allDHAllocationsOn1_1 <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, 1)
allDHAllocationsOff1_1 <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 1, 1)
allDHAllocations1_12477 <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, 12477)
allDHAllocationsOn16_1 <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, 1)
allDHAllocationsOff16_1 <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 16, 1)
allDHAllocations16_12477 <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, 12477)

cpuFragmentations <- data.frame()
cpuFragmentations <- rbind(cpuFragmentations, data.frame(infra = "sb-on", CalculateCpuFragmentationCI(allSBAllocationsOn)))
#cpuFragmentations <- rbind(cpuFragmentations, data.frame(infra = "sb-off", CalculateCpuFragmentationCI(allSBAllocationsOff)))
cpuFragmentations <- rbind(cpuFragmentations, data.frame(infra = "dh-blade-on", CalculateCpuFragmentationCI(allDHAllocationsOn1_1)))
#cpuFragmentations <- rbind(cpuFragmentations, data.frame(infra = "dh-blade-off", CalculateCpuFragmentationCI(allDHAllocationsOff1_1)))
#cpuFragmentations <- rbind(cpuFragmentations, data.frame(infra = "dh-blade-12477", CalculateCpuFragmentationCI(allDHAllocations1_12477)))
cpuFragmentations <- rbind(cpuFragmentations, data.frame(infra = "dh-drawer-on", CalculateCpuFragmentationCI(allDHAllocationsOn16_1)))
#cpuFragmentations <- rbind(cpuFragmentations, data.frame(infra = "dh-drawer-off", CalculateCpuFragmentationCI(allDHAllocationsOff16_1)))
#cpuFragmentations <- rbind(cpuFragmentations, data.frame(infra = "dh-drawer-12477", CalculateCpuFragmentationCI(allDHAllocations16_12477)))

PlotCpuFragmentationCI(cpuFragmentations, T)



memFragmentations <- data.frame()
memFragmentations <- rbind(memFragmentations, data.frame(infra = "sb-on", CalculateMemFragmentationCI(allAllocations)))
memFragmentations <- rbind(memFragmentations, data.frame(infra = "dh-blade-on", CalculateMemFragmentationCI(allDHAllocations1_1)))
#memFragmentations <- rbind(memFragmentations, data.frame(infra = "dh-blade-12477", CalculateMemFragmentationCI(allDHAllocations1_12477)))
memFragmentations <- rbind(memFragmentations, data.frame(infra = "dh-drawer-on", CalculateMemFragmentationCI(allDHAllocations16_1)))
#memFragmentations <- rbind(memFragmentations, data.frame(infra = "dh-drawer-12477", CalculateMemFragmentationCI(allDHAllocations16_12477)))

PlotMemFragmentationCI(memFragmentations, T)


# evaluating number of servers
allDHAllocationsOn1_1 <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, 1)
allDHAllocationsOff1_1 <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 1, 1)

allDHAllocationsOn16_1 <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, 1)
allDHAllocationsOff16_1 <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 16, 1)

servers <- data.frame()
servers<- rbind(servers, data.frame(infra = "dh-blade-on", CalculateServersCI(allDHAllocationsOn1_1)))
servers<- rbind(servers, data.frame(infra = "dh-blade-off", CalculateServersCI(allDHAllocationsOff1_1)))
servers<- rbind(servers, data.frame(infra = "dh-drawer-on", CalculateServersCI(allDHAllocationsOn16_1)))
servers<- rbind(servers, data.frame(infra = "dh-drawer-off", CalculateServersCI(allDHAllocationsOff16_1)))

PlotServersCI(servers)

# pending queue
allDHPendingInfoOn1_1 <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, 1, nAllTasks)
allDHPendingInfoOff1_1 <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 1, 1, nAllTasks)
allDHPendingInfoOn16_1 <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 16, 1, nAllTasks)
allDHPendingInfoOff16_1 <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = F, serverSize = 16, 1, nAllTasks)

pendingFraction <- data.frame()
pendingFraction<- rbind(pendingFraction, data.frame(infra = "sb-on", CalculatePendingFractionCI(allSBPendingInfoOn)))
pendingFraction<- rbind(pendingFraction, data.frame(infra = "sb-off", CalculatePendingFractionCI(allSBPendingInfoOff)))
pendingFraction<- rbind(pendingFraction, data.frame(infra = "dh-blade-on", CalculatePendingFractionCI(allDHPendingInfoOn1_1)))
pendingFraction<- rbind(pendingFraction, data.frame(infra = "dh-blade-off", CalculatePendingFractionCI(allDHPendingInfoOff1_1)))
pendingFraction<- rbind(pendingFraction, data.frame(infra = "dh-drawer-on", CalculatePendingFractionCI(allDHPendingInfoOn16_1)))
pendingFraction<- rbind(pendingFraction, data.frame(infra = "dh-drawer-off", CalculatePendingFractionCI(allDHPendingInfoOff16_1)))

PlotPendingFractionCI(pendingFraction)

prodPendingFraction <- data.frame()
prodPendingFraction<- rbind(prodPendingFraction, data.frame(infra = "sb", CalculateProdPendingFractionCI(allSBPendingInfo)))
prodPendingFraction<- rbind(prodPendingFraction, data.frame(infra = "dh-blade-on", CalculateProdPendingFractionCI(allDHPendingInfoOn1_1)))
prodPendingFraction<- rbind(prodPendingFraction, data.frame(infra = "dh-blade-off", CalculateProdPendingFractionCI(allDHPendingInfoOff1_1)))
prodPendingFraction<- rbind(prodPendingFraction, data.frame(infra = "dh-drawer-on", CalculateProdPendingFractionCI(allDHPendingInfoOn16_1)))
prodPendingFraction<- rbind(prodPendingFraction, data.frame(infra = "dh-drawer-off", CalculateProdPendingFractionCI(allDHPendingInfoOff16_1)))

PlotProdPendingFractionCI(prodPendingFraction)

pendingCpuCI <- data.frame()
pendingCpuCI<- rbind(pendingCpuCI, data.frame(infra = "sb-on", CalculatePendingCpuCI(allSBPendingInfoOn)))
pendingCpuCI<- rbind(pendingCpuCI, data.frame(infra = "sb-off", CalculatePendingCpuCI(allSBPendingInfoOff)))
pendingCpuCI<- rbind(pendingCpuCI, data.frame(infra = "dh-blade-on", CalculatePendingCpuCI(allDHPendingInfoOn1_1)))
pendingCpuCI<- rbind(pendingCpuCI, data.frame(infra = "dh-blade-off", CalculatePendingCpuCI(allDHPendingInfoOff1_1)))
pendingCpuCI<- rbind(pendingCpuCI, data.frame(infra = "dh-drawer-on", CalculatePendingCpuCI(allDHPendingInfoOn16_1)))
pendingCpuCI<- rbind(pendingCpuCI, data.frame(infra = "dh-drawer-off", CalculatePendingCpuCI(allDHPendingInfoOff16_1)))

PlotPendingCpuCI(pendingCpuCI)

pendingMemCI <- data.frame()
pendingMemCI<- rbind(pendingMemCI, data.frame(infra = "sb-on", CalculatePendingMemCI(allSBPendingInfoOn)))
pendingMemCI<- rbind(pendingMemCI, data.frame(infra = "sb-off", CalculatePendingMemCI(allSBPendingInfoOff)))
pendingMemCI<- rbind(pendingMemCI, data.frame(infra = "dh-blade-on", CalculatePendingMemCI(allDHPendingInfoOn1_1)))
pendingMemCI<- rbind(pendingMemCI, data.frame(infra = "dh-blade-off", CalculatePendingMemCI(allDHPendingInfoOff1_1)))
pendingMemCI<- rbind(pendingMemCI, data.frame(infra = "dh-drawer-on", CalculatePendingMemCI(allDHPendingInfoOn16_1)))
pendingMemCI<- rbind(pendingMemCI, data.frame(infra = "dh-drawer-off", CalculatePendingMemCI(allDHPendingInfoOff16_1)))

PlotPendingMemCI(pendingMemCI)


mem <- allDHAllocations16_1 %>% filter(mem.remaing < 1) %>% dplyr::summarise(n())
nopend <- allDHAllocations16_1 %>% filter(mem.remaing > 1 & cpu.remaing > 1) %>% dplyr::summarise(n())
cpu <- allDHAllocations16_1 %>% filter(cpu.remaing < 1) %>% dplyr::summarise(n())
5/30
2/30
23/30
# resource grain
allDHAllocationsOn1_1_small <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, 1, resourceLabel = "small-grain")
allDHAllocationsOn1_1_medium <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, 1, resourceLabel = "medium-grain")
allDHAllocationsOn1_1_big <- CollectAllTimesDHAllocationInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, 1, resourceLabel = "big-grain")

cpuFragmentationsRG <- data.frame()
cpuFragmentationsRG <- rbind(cpuFragmentationsRG, data.frame(infra = "sb", CalculateCpuFragmentationCI(allSBAllocationsOn)))
cpuFragmentationsRG <- rbind(cpuFragmentationsRG, data.frame(infra = "dh-blade-large-grain", CalculateCpuFragmentationCI(allDHAllocationsOn1_1_big)))
cpuFragmentationsRG <- rbind(cpuFragmentationsRG, data.frame(infra = "dh-blade-small-grain", CalculateCpuFragmentationCI(allDHAllocationsOn1_1_medium)))
#cpuFragmentationsRG <- rbind(cpuFragmentationsRG, data.frame(infra = "dh-blade-small-grain", CalculateCpuFragmentationCI(allDHAllocationsOn1_1_small)))

PlotCpuFragmentationCI(cpuFragmentationsRG, T)

memFragmentationsRG <- data.frame()
memFragmentationsRG <- rbind(memFragmentationsRG, data.frame(infra = "sb", CalculateMemFragmentationCI(allSBAllocationsOn)))
memFragmentationsRG <- rbind(memFragmentationsRG, data.frame(infra = "dh-blade-large-grain", CalculateMemFragmentationCI(allDHAllocationsOn1_1_big)))
memFragmentationsRG <- rbind(memFragmentationsRG, data.frame(infra = "dh-blade-small-grain", CalculateMemFragmentationCI(allDHAllocationsOn1_1_medium)))
#memFragmentationsRG <- rbind(memFragmentationsRG, data.frame(infra = "dh-blade-small-grain", CalculateMemFragmentationCI(allDHAllocationsOn1_1_small)))

PlotMemFragmentationCI(memFragmentationsRG, T)

serversRG <- data.frame()
serversRG<- rbind(serversRG, data.frame(infra = "dh-blade-large-grain", CalculateServersCI(allDHAllocationsOn1_1_big)))
serversRG<- rbind(serversRG, data.frame(infra = "dh-blade-mid-grain", CalculateServersCI(allDHAllocationsOn1_1_medium)))
serversRG<- rbind(serversRG, data.frame(infra = "dh-blade-small-grain", CalculateServersCI(allDHAllocationsOn1_1_small)))

PlotServersCI(serversRG)

allDHPendingInfoOn1_1_small <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, 1, nAllTasks, "small-grain")
allDHPendingInfoOn1_1_medium <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, 1, nAllTasks, "medium-grain")
allDHPendingInfoOn1_1_big <- CollectAllTimesDHPendingInfo(resultDir, allTasks = T, constraintOn = T, serverSize = 1, 1, nAllTasks, "big-grain")


pendingFractionRG <- data.frame()
pendingFractionRG<- rbind(pendingFractionRG, data.frame(infra = "dh-blade-large-grain", CalculatePendingFractionCI(allDHPendingInfoOn1_1_big)))
pendingFractionRG<- rbind(pendingFractionRG, data.frame(infra = "dh-blade-mid-grain", CalculatePendingFractionCI(allDHPendingInfoOn1_1_medium)))
pendingFractionRG<- rbind(pendingFractionRG, data.frame(infra = "dh-blade-small-grain", CalculatePendingFractionCI(allDHPendingInfoOn1_1_small)))

PlotPendingFractionCI(pendingFractionRG)