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

CollectAllTimesSBAllocationInfo <- function(resultDir, constraintOn, allTasks) {
  constraint <- "on"
  
  if (!constraintOn)
    constraint <- "off"
  
  workload <- "all"
  if (!allTasks) 
    workload <- "prod"

  allAllocation <- data_frame()
  for (time in 0:29) {
    print(time)
    
    timeAllocation <- CollectAllocationInfo(paste(resultDir,"/time",time,"/", workload, "/allocation-", constraint,"-12477-hosts.csv", sep = ""))

    allAllocation <- rbind(allAllocation, timeAllocation)
  }
  return(allAllocation)
}

CollectAllTimesSBPendingInfo <- function(resultDir, constraintOn, allTasks, numberOfTasks) {
  constraint <- "on"
  
  if (!constraintOn)
    constraint <- "off"
  
  workload <- "all"
  if (!allTasks) 
    workload <- "prod"
  
  allPendingInfo <- data_frame()
  for (time in 0:29) {
    print(time)
    
    timePending <- CollectPendingInfo(paste(resultDir,"/time",time,"/", workload, "/pending-queue-", constraint,"-12477-hosts.csv", sep = ""))
    
    total.tasks <- numberOfTasks %>% filter(timestamp == time) %>% select(nTasks)
    timePending <- data.frame(timePending, nTasks = total.tasks)
    
    allPendingInfo <- rbind(allPendingInfo, timePending)
  }
  return(allPendingInfo)
}


CollectAllTimesDHAllocationInfo <- function(resultDir, constraintOn, allTasks = T, serverSize, minServers, resourceLabel = NULL) {
  constraint <- "on"
  if (!constraintOn)
    constraint <- "off"
  
  allAllocation <- data_frame()
  
  for (time in 0:29) {
    print(paste("Time ", time), sep= "")
    
    if (is.null(resourceLabel)) {
      timeResultDir <- paste(resultDir, "/time", time, "/", sep = "")
      
    } else {
      timeResultDir <- paste(resultDir, "/time", time, "/", resourceLabel, "/", sep = "")
    }
    
    prefixFileName <- paste("allocation-", constraint,"-", minServers, "-", serverSize, "-", serverSize, "-", sep = "")
    
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

CollectAllTimesDHPendingInfo <- function(resultDir, constraintOn, allTasks = T, serverSize, mimServers, numberOfTasks, resourceLabel = NULL) {
  constraint <- "on"
  if (!constraintOn)
    constraint <- "off"
  
  allPendingInfo <- data_frame()
  
  for (time in 0:29) {
    #timeResultDir <- paste(resultDir, "/time", time, "/", sep = "")
    
    if (is.null(resourceLabel)) {
      timeResultDir <- paste(resultDir, "/time", time, "/", sep = "")
      
    } else {
      timeResultDir <- paste(resultDir, "/time", time, "/", resourceLabel, "/", sep = "")
    }
    
    prefixFileName <- paste("pending-queue-", constraint,"-", minServers, "-", serverSize, "-", serverSize, "-", sep = "")
    
    if (!allTasks) {
      timeResultDir <- paste(timeResultDir, "prod/")
    } 
    
    for (fileName in list.files(path = timeResultDir)) {
      
      if (startsWith(fileName, prefixFileName)) {
        print(paste("Collecting allocation info from file:", fileName))
        
        timePending <- CollectPendingInfo(paste(timeResultDir,fileName, sep = ""))
        
        total.tasks <- numberOfTasks %>% filter(timestamp == time) %>% select(nTasks)
        timePending <- data.frame(timePending, nTasks = total.tasks)
        
        allPendingInfo <- rbind(allPendingInfo, timePending)
        break
      }
    }
  }
  
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

CalculatePendingFractionCI <- function(pendingInfo) {
  t <- data.frame(upper = CI(((pendingInfo$tasks)/pendingInfo$nTasks), ci = 0.95)[1], mean = CI(((pendingInfo$tasks)/pendingInfo$nTasks), ci = 0.95)[2], lower = CI(((pendingInfo$tasks)/pendingInfo$nTasks), ci = 0.95)[3])
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

CalculatePendingMemCI <- function(pendingInfo) {
  t <- data.frame(upper = CI(pendingInfo$total.mem, ci = 0.95)[1], mean = CI(pendingInfo$total.mem, ci = 0.95)[2], lower = CI(pendingInfo$total.mem, ci = 0.95)[3])
  return(t)
}

PlotCpuFragmentationCI <- function(fragmentations, constraintOn) {
  if (constraintOn) {
    ggplot(fragmentations, aes(x=infra, y=mean * 100)) + geom_point() + geom_errorbar(aes(ymax = upper*100, ymin=lower*100)) + ylab("% of CPU fragmentation") + xlab("Infrastructure") +  
      ggtitle(paste("CPU fragmentation considering placement constraint ", sep=""))  
  } else {
    ggplot(fragmentations, aes(x=infra, y=mean * 100)) + geom_point() + geom_errorbar(aes(ymax = upper*100, ymin=lower*100)) + ylab("% of CPU fragmentation") + xlab("Infrastructure") +  
      ggtitle(paste("CPU fragmentation not considering placement constraint ", sep=""))
  }
}

PlotMemFragmentationCI <- function(fragmentations, constraintOn) {
  if (constraintOn) {
    ggplot(fragmentations, aes(x=infra, y=mean * 100)) + geom_point() + geom_errorbar(aes(ymax = upper*100, ymin=lower*100)) + ylab("% of RAM fragmentation") + xlab("Infrastructure") +  
      ggtitle(paste("RAM fragmentation considering placement constraint ", sep=""))  
  } else {
    ggplot(fragmentations, aes(x=infra, y=mean * 100)) + geom_point() + geom_errorbar(aes(ymax = upper*100, ymin=lower*100)) + ylab("% of RAM fragmentation") + xlab("Infrastructure") +  
      ggtitle(paste("RAM fragmentation not considering placement constraint ", sep=""))
  }
}

PlotServersCI <- function(servers) {
  ggplot(servers, aes(x=infra, y=mean)) + geom_point() + geom_errorbar(aes(ymax = upper, ymin=lower)) + ylab("#servers") + xlab("Infrastructure") +  
    ggtitle(paste("Number of assembled logical servers", sep=""))  
}

PlotPendingFractionCI <- function(pendingFraction) {
  ggplot(pendingFraction, aes(x=infra, y=mean * 100)) + geom_point() + geom_errorbar(aes(ymax = upper * 100, ymin=lower * 100)) + ylab("% of tasks") + xlab("Infrastructure") +  
    ggtitle(paste("% of tasks in pending queue", sep=""))  
}

PlotProdPendingFractionCI <- function(pendingFraction) {
  ggplot(pendingFraction, aes(x=infra, y=mean * 100)) + geom_point() + geom_errorbar(aes(ymax = upper * 100, ymin=lower * 100)) + ylab("% of prod tasks") + xlab("Infrastructure") +  
    ggtitle(paste("% of pending queue that is prod task", sep=""))  
}

PlotPendingCpuCI <- function(pendingCpuCI) {
  ggplot(pendingCpuCI, aes(x=infra, y=mean)) + geom_point() + geom_errorbar(aes(ymax = upper, ymin=lower)) + ylab("requested cpu") + xlab("Infrastructure") +  
    ggtitle(paste("Amount of cpu requested by tasks in pending queue", sep=""))  
}

PlotPendingMemCI <- function(pendingMemCI) {
  ggplot(pendingMemCI, aes(x=infra, y=mean)) + geom_point() + geom_errorbar(aes(ymax = upper, ymin=lower)) + ylab("requested RAM") + xlab("Infrastructure") +  
    ggtitle(paste("Amount of RAM requested by tasks in pending queue", sep=""))  
}

CollectAllocationDiffBetweenSBAndDH <- function(allSBAllocationsOn, allDHAllocationsOn1_1){
  
  sbAllocation <- allSBAllocationsOn
  dhAllocation <- allDHAllocations1_1
  
  timePending <- 
  
  total.tasks <- numberOfTasks %>% filter(timestamp == time) %>% select(nTasks)
  timePending <- data.frame(timePending, nTasks = total.tasks)
  
  allPendingInfo <- rbind(allPendingInfo, timePending)
  
}


nAllTasks <- read.csv("number_of_tasks.txt")
nProdTasks <- read.csv("number_of_prod_tasks.txt")

allSBAllocationsOn <- CollectAllTimesSBAllocationInfo("experiment-results/sb-based-results", constraintOn = T, allTasks = T)
allSBPendingInfoOn <- CollectAllTimesSBPendingInfo("experiment-results/sb-based-results", constraintOn = T, allTasks = T, nAllTasks)

allSBAllocationsOff <- CollectAllTimesSBAllocationInfo("experiment-results/sb-based-results", constraintOn = F, allTasks = T)
allSBPendingInfoOff <- CollectAllTimesSBPendingInfo("experiment-results/sb-based-results", constraintOn = F, allTasks = T, nAllTasks)


allSBProdAllocations <- CollectAllTimesSBAllocationInfo("experiment-results/sb-based-results", constraintOn = T, allTasks = F)
allSBProdPendingInfo <- CollectAllTimesSBPendingInfo("experiment-results/sb-based-results", constraintOn = T, allTasks = F, nProdAllTasks)

allSBProdAllocationsOff <- CollectAllTimesSBAllocationInfo("experiment-results/sb-based-results", constraintOn = F, allTasks = F)
allSBProdPendingInfoOff <- CollectAllTimesSBPendingInfo("experiment-results/sb-based-results", constraintOn = F, allTasks = F, nProdTasks)


resultDir <- "experiment-results/dh-based-results"

allocation <- read.csv("experiment-results/dh-based-results/time0/allocation-on-1-16-16-9245-servers.csv")

allocation <- read.csv("experiment-results/dh-based-results/time1/allocation-on-1-16-16-9358-servers.csv")
allocation <- read.csv("experiment-results/dh-based-results/time18/allocation-on-1-16-16-11457-servers.csv")

head(allocation)
allocation %>% dplyr::summarise(pool.cpu=sum(cpuCapacity), pool.mem=sum(memCapacity)) 

allocation %>% dplyr::summarise(servers=n(), infra.cpu=sum(cpuCapacity), infra.freeCpu=sum(freeCpu), cpu.fragmentation=infra.freeCpu/infra.cpu, cpu.remaing=total.cloud.cpu-infra.cpu,
                                infra.mem=sum(memCapacity), infra.freeMem=sum(freeMem), mem.fragmentation=infra.freeMem/infra.mem, mem.remaing=total.cloud.mem-infra.mem)

allocation %>% group_by(cpuPoolId) %>% dplyr::summarise(pool.cpu=sum(cpuCapacity), pool.mem=sum(memCapacity)) %>% dplyr::summarise(total.cpu=sum(pool.cpu), total.mem=sum(pool.mem))

constraintOn = T
serverSize = 16

diffOn1_1 <- CollectAllocationDiffBetweenSBAndDH(allSBAllocationsOn, allDHAllocationsOn1_1)

# collecting allocation info constraint on
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