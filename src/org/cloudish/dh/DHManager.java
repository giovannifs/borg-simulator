package org.cloudish.dh;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.cloudish.borg.model.Task;
import org.cloudish.dh.model.LogicalServer;
import org.cloudish.dh.model.ResourcePool;

public class DHManager {

	private List<LogicalServer> logicalServers = new ArrayList<>();
	private List<Task> pendingQueue = new ArrayList<>();
	private Map<String, List<ResourcePool>> resourcePools;
	private double resourceGrain;
	private int minLogicalServer;
	private double maxCpuServerCapacity;
	private double maxMemServerCapacity;
	
	public DHManager(Properties properties, Map<String, List<ResourcePool>> resourcePools) {
		if (resourcePools.isEmpty()) {
			throw new IllegalArgumentException("The resource pool must not be empty while creating a DH manager.");
		}

		this.resourceGrain = Double.parseDouble(properties.getProperty("resource_grain"));
		this.minLogicalServer = Integer.parseInt(properties.getProperty("min_logical_servers"));
		this.maxCpuServerCapacity = Double.parseDouble(properties.getProperty("max_cpu_logical_server_capacity"));
		this.maxMemServerCapacity = Double.parseDouble(properties.getProperty("max_memory_logical_server_capacity"));
		this.resourcePools = resourcePools;
	}

	public LogicalServer createLogicalServer(Task task) {
		// choosing cpu resource pool
		ResourcePool bestCpuPool = chooseResourcePool(ResourcePool.CPU_TYPE, task);
		
		// there is not any cpu pool feasible to the task
		if (bestCpuPool == null) {
			return null;
		}
		
		ResourcePool bestMemPool = chooseResourcePool(ResourcePool.MEMORY_TYPE, task);
		
		// there is not any memory pool feasible to the task
		if (bestMemPool == null) {
			return null;
		}
		
		return new LogicalServer(bestCpuPool, bestMemPool, getMaxCpuServerCapacity(), getMaxMemServerCapacity(),
				getResourceGrain());
	}

	private ResourcePool chooseResourcePool(String poolType, Task task) {
		ResourcePool bestPool = null;
		double bestPoolScore = -1;
		for (ResourcePool resourcePool : resourcePools.get(poolType)) {

			double cpuScore = resourcePool.getScore(task);
			if (resourcePool.isFeasible(task) && cpuScore > bestPoolScore) {
				bestPool = resourcePool;
				bestPoolScore = cpuScore;
			}
		}
		return bestPool;
	}

	public boolean allocate(Task task) {
		
		double bestScore = -1;
		List<LogicalServer> bestLogicalServers = new ArrayList<>();
		
		for (LogicalServer lServer : logicalServers) {
			double score = lServer.getScore(task);
			
			if (score > bestScore) {
				bestScore = score;
				bestLogicalServers.clear();
				bestLogicalServers.add(lServer);
			} else if (score == bestScore && bestScore >= 0) {
				bestLogicalServers.add(lServer);
			}
		}

		// There is not feasible logical server 
		if (bestScore < 0 && bestLogicalServers.isEmpty()) {
			
			// create new logicalServer					
			LogicalServer newLServer = createLogicalServer(task);
			
			if (newLServer != null) {				
				logicalServers.add(newLServer);
				newLServer.allocate(task);
				return true;
			
			} else { // there was not resource to create a new logical server that fulfills the task
				
				pendingQueue.add(task);
				return false;
			}
		} else {
			LogicalServer bestLServer = chooseBestLogicalServer(bestLogicalServers, task);
			
			// this can do scale_up
			bestLServer.allocate(task);
			return true;
		}		
	}

	private LogicalServer chooseBestLogicalServer(List<LogicalServer> bestLogicalServers, Task task) {
		List<LogicalServer> filteredLServers = new ArrayList<>();

		// check if any logical server does not need scale up
		double minScaleup = Integer.MAX_VALUE;
		for (LogicalServer lServer : bestLogicalServers) {

			int scaleUp = lServer.needsCPUScaleUp(task) ? (lServer.needsMemScaleUp(task) ? 2 : 1)
					: (lServer.needsMemScaleUp(task) ? 1 : 0);

			if (scaleUp < minScaleup) {
				filteredLServers.clear();
				filteredLServers.add(lServer);
				minScaleup = scaleUp;
			} else if (scaleUp == minScaleup) {
				filteredLServers.add(lServer);
			}
		}

		// choosing a random host inside of filtered logical server list
		return filteredLServers.get(new Random().nextInt(filteredLServers.size()));
	}

	public void addLogicalServer(LogicalServer logicalServer) {
		logicalServers.add(logicalServer);		
	}

	public List<LogicalServer> getLogicalServers() {
		return logicalServers;
	}

	public List<Task> getPendingQueue() {
		return pendingQueue;
	}
	
	public int getMinLogicalServer() {
		return minLogicalServer;
	}

	public Map<String, List<ResourcePool>> getResourcePools() {
		return resourcePools;
	}

	public double getResourceGrain() {
		return resourceGrain;
	}

	public boolean hasMinimumLogicalServer() {
		return getLogicalServers().size() >= getMinLogicalServer();
	}

	public double getMaxCpuServerCapacity() {
		return maxCpuServerCapacity;
	}

	public double getMaxMemServerCapacity() {
		return maxMemServerCapacity;
	}

	public void createMinimumLogicalServer() {
		// TODO Auto-generated method stub
		
//		There is also an assembling constraint on the whole DH-based infrastructure that defines a minimum number of logical servers. This is treated a posteriori. When all tasks have been scheduled, the following algorithm is executed.
//
//		While the minimum number of logical servers is not reached, do:
//
//		--- Select the logical server with the worse performance (regarding the evaluation metric)
//
//		--- Divide this logical server in two, each with the minimal capacity possible, and using CPU components from the CPU pool originally used by the logical server that has been divided
//
//		--- Return all the leftover components to their respective pools
//
//		--- Use the same algorithm in the main loop to reschedule the tasks that were allocated in the logical server that was divided but considering only the two logical servers that have been created
		
	}
}
