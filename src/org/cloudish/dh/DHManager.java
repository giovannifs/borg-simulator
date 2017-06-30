package org.cloudish.dh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.Random;
import java.util.TreeMap;

import org.cloudish.borg.model.Task;
import org.cloudish.dh.model.LogicalServer;
import org.cloudish.dh.model.ResourcePool;

public class DHManager {

	private List<LogicalServer> logicalServers = new ArrayList<>();
	private List<Task> pendingQueue = new ArrayList<>();
	private Map<String, List<ResourcePool>> resourcePools;
	private Map<String, LogicalServer> possibleGKValues = new HashMap<>();
	private double resourceGrain;
	private int minLogicalServer;
	private double maxCpuServerCapacity;
	private double maxMemServerCapacity;
	
	public DHManager(Properties properties, Map<String, List<ResourcePool>> resourcePools, List<String> possibleGKValues) {
		if (resourcePools.isEmpty()) {
			throw new IllegalArgumentException("The resource pool must not be empty while creating a DH manager.");
		}

		this.resourceGrain = Double.parseDouble(properties.getProperty("resource_grain"));
		this.minLogicalServer = Integer.parseInt(properties.getProperty("min_logical_servers"));
		this.maxCpuServerCapacity = Double.parseDouble(properties.getProperty("max_cpu_logical_server_capacity"));
		this.maxMemServerCapacity = Double.parseDouble(properties.getProperty("max_memory_logical_server_capacity"));
		this.resourcePools = resourcePools;
		
		for (String gkValue : possibleGKValues) {
			this.possibleGKValues.put(gkValue, null);
		}
	}

	public LogicalServer createLogicalServer(Task task) {		
		ResourcePool cpuPool = null;
		ResourcePool memPool = null;
		
		// creating initial logical server, then it must choose cpuPool according to its size
		if (task == null) {			
			cpuPool = chooseCpuPoolRandomly();
			memPool = chooseResourcePool(ResourcePool.MEMORY_TYPE, task);
			
		} else {
			// choosing cpu resource pool
			cpuPool = chooseResourcePool(ResourcePool.CPU_TYPE, task);
			memPool = chooseResourcePool(ResourcePool.MEMORY_TYPE, task);
		}

		// there is not any cpu or mem pool feasible to the task
		if (cpuPool == null || memPool == null) {
			return null;
		}
		
		return new LogicalServer(cpuPool, memPool, getMaxCpuServerCapacity(), getMaxMemServerCapacity(),
				getResourceGrain(), this);
	}

	private ResourcePool chooseCpuPoolRandomly() {
		ResourcePool cpuPool;
		RandomCollection<ResourcePool> rc = new RandomCollection<ResourcePool>();
		
		for (ResourcePool pool : resourcePools.get(ResourcePool.CPU_TYPE)) {
			rc.add(pool.getFreeCapacity(), pool);				
		}
		
		cpuPool = rc.next();
		
		System.out.println(cpuPool.getId() + " - size: " + cpuPool.getCapacity());
		return cpuPool;
	}

	private ResourcePool chooseResourcePool(String poolType, Task task) {
		ResourcePool bestPool = null;
		double bestPoolScore = -1;
		
		for (ResourcePool resourcePool : resourcePools.get(poolType)) {
			
			// checking if pool is feasible
			if (resourcePool.isFeasible(task)){
				
				// calculating score
				double cpuScore = resourcePool.getScore();
				if (cpuScore > bestPoolScore) {
					bestPool = resourcePool;
					bestPoolScore = cpuScore;
				}
			}
		}
		return bestPool;
	}

	public boolean allocate(Task task) {
		
		System.out.println("Allocating " + task);
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
			System.out.println("There is not logical server feasible.");
			
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
	
	public Map<String, LogicalServer> getPossibleGKValues() {
		return possibleGKValues;
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

	public boolean isGKValueAvailable(String GKValue) {
		return getPossibleGKValues().containsKey(GKValue) && getPossibleGKValues().get(GKValue) == null;
	}

	public void allocateGKValue(String attValue, LogicalServer logicalServer) {
		if (!getPossibleGKValues().containsKey(attValue) || getPossibleGKValues().get(attValue) != null) {
			throw new RuntimeException("Trying to allocate a GK value that is not available.");
		}

		getPossibleGKValues().put(attValue, logicalServer);
	}
}

class RandomCollection<E> {
    private final NavigableMap<Double, E> map = new TreeMap<Double, E>();
    private final Random random;
    private double total = 0;

    public RandomCollection() {
        this(new Random());
    }

    public RandomCollection(Random random) {
        this.random = random;
    }

    public RandomCollection<E> add(double weight, E result) {
        if (weight <= 0) return this;
        total += weight;
        map.put(total, result);
        return this;
    }

    public E next() {
        double value = random.nextDouble() * total;
        return map.higherEntry(value).getValue();
    }
}
