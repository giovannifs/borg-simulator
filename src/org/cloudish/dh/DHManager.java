package org.cloudish.dh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.cloudish.borg.model.ResourceAttribute;
import org.cloudish.borg.model.Task;
import org.cloudish.dh.model.LogicalServer;
import org.cloudish.dh.model.ResourcePool;

public class DHManager {

	private List<LogicalServer> logicalServers = new ArrayList<>();
	private List<Task> pendingQueue = new ArrayList<>();
	private Map<String, List<ResourcePool>> resourcePools;
	private Map<String, LogicalServer> possibleGKValues = new HashMap<>();
	private double cpuResourceGrain;
	private double memResourceGrain;
	private double maxCpuServerCapacity;
	private double maxMemServerCapacity;
	
	private boolean constraintsOn = true;
	private boolean isAntiAffinityOn = true;
	
	public DHManager(Properties properties, Map<String, List<ResourcePool>> resourcePools, List<String> possibleGKValues) {
		if (resourcePools.isEmpty()) {
			throw new IllegalArgumentException("The resource pool must not be empty while creating a DH manager.");
		}

		this.cpuResourceGrain = Double.parseDouble(properties.getProperty("cpu_resource_grain"));		
		this.memResourceGrain = Double.parseDouble(properties.getProperty("mem_resource_grain"));
		
		System.out.println("cpu-resource-grain? " + cpuResourceGrain);
		System.out.println("mem-resource-grain? " + memResourceGrain);
		
		this.constraintsOn = properties.getProperty("placement_constraints_on") == null
				|| properties.getProperty("placement_constraints_on").equals("yes")
				|| properties.getProperty("placement_constraints_on").equals("on") ? true : false;
		
		this.isAntiAffinityOn = properties.getProperty("anti_affinity_constraint_on") == null
				|| properties.getProperty("anti_affinity_constraint_on").equals("yes")
				|| properties.getProperty("anti_affinity_constraint_on").equals("on") ? true : false;

		this.maxCpuServerCapacity = Double.parseDouble(properties.getProperty("max_cpu_logical_server_capacity"));
		this.maxMemServerCapacity = Double.parseDouble(properties.getProperty("max_memory_logical_server_capacity"));
		this.resourcePools = resourcePools;
		
		System.out.println("max_cpu_logical_server_capacity? " + maxCpuServerCapacity);
		System.out.println("max_cpu_logical_server_capacity? " + maxMemServerCapacity);
		
		
		for (String gkValue : possibleGKValues) {
			this.possibleGKValues.put(gkValue, null);
		}
	}

	public LogicalServer createLogicalServer(Map<String, ResourceAttribute> attributes) {		
		ResourcePool memPool = chooseResourcePool(ResourcePool.MEMORY_TYPE, null);
		ResourcePool cpuPool = null;
		
		for (ResourcePool pool : resourcePools.get(ResourcePool.CPU_TYPE)) {
			if (pool.match(attributes)) {
				cpuPool = pool;
				break;
			}
		}
		
		return new LogicalServer(cpuPool, memPool, getMaxCpuServerCapacity(), getMaxMemServerCapacity(),
				getCpuResourceGrain(), getMemResourceGrain(), this, isConstraintsOn(), isAntiAffinityOn());
	}
	
	public LogicalServer createLogicalServer(Task task) {		
		System.out.println("Creating new Logical Server for task " + task);
		
		ResourcePool cpuPool = null;
		ResourcePool memPool = null;
		
		if (task == null) {
			throw new RuntimeException("Trying to allocatie a null task.");
		} 

		// choosing cpu resource pool
		cpuPool = chooseResourcePool(ResourcePool.CPU_TYPE, task);
		memPool = chooseResourcePool(ResourcePool.MEMORY_TYPE, task);

		System.out.println("chosen CPU Pool?" + cpuPool.getId() );
		System.out.println("chosen CPU Pool free Capacity?" + cpuPool.getFreeCapacity());
		// there is not any cpu or mem pool feasible to the task
		if (cpuPool == null || memPool == null) {
			return null;
		}
		
		return new LogicalServer(cpuPool, memPool, getMaxCpuServerCapacity(), getMaxMemServerCapacity(),
				getCpuResourceGrain(), getMemResourceGrain(), this, isConstraintsOn(), isAntiAffinityOn());
	}

	private ResourcePool chooseResourcePool(String poolType, Task task) {
		ResourcePool bestPool = null;
		double bestPoolScore = -1;
		
//		List<ResourcePool> feasiblePools = new ArrayList<ResourcePool>();
		
		for (ResourcePool resourcePool : resourcePools.get(poolType)) {
			
			// checking if pool is feasible and has amount of resource available
			double resourceToBeRequested;
			if (poolType.equals(ResourcePool.CPU_TYPE)) {
				resourceToBeRequested = calcCpuToBeRequested(task);
			} else {
				resourceToBeRequested = calcMemToBeRequested(task);
			}
			
			if (resourcePool.isFeasible(task) && resourcePool.hasMoreResource(resourceToBeRequested)) {

//				feasiblePools.add(resourcePool);

				// calculating score
				double cpuScore = resourcePool.getScore();
				if (cpuScore > bestPoolScore) {
					bestPool = resourcePool;
					bestPoolScore = cpuScore;
				}
			}		
		}
		
//		if (!feasiblePools.isEmpty()) {
//			Collections.sort(feasiblePools, new Comparator<ResourcePool>() {
//
//				@Override
//				public int compare(ResourcePool o1, ResourcePool o2) {
//					return (-1) * new Double(o1.getScore()).compareTo(new Double(o2.getScore()));
//				}
//			});
//			
//			bestPool = feasiblePools.get(new Random().nextInt(Math.min(feasiblePools.size(), 3)));
//		}
		return bestPool;
	}
	
	private double calcCpuToBeRequested(Task task) {
		if (task == null || task.getCpuReq() == 0) {
			return getCpuResourceGrain();
		}
		double cpuToBeScaled = Utils.format(task.getCpuReq());
		int numberOfGrains = (int) Math.ceil(cpuToBeScaled / getCpuResourceGrain());
		double cpuToBeRequested = Utils.format(numberOfGrains * getCpuResourceGrain());
		return cpuToBeRequested;

	}

	private double calcMemToBeRequested(Task task) {
		if (task == null || task.getMemReq() == 0) {
			return getMemResourceGrain();
		}
		double memToBeScaled = Utils.format(task.getMemReq());
		int numberOfGrains = (int) Math.ceil(memToBeScaled / getMemResourceGrain());

		double memToBeRequested = Utils.format(numberOfGrains * getMemResourceGrain());
		return memToBeRequested;
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
			System.out.println("There is not logical server feasible.");

			// task is free
			if (task.getPriority() <= 1) {
				
				ResourcePool cpuPool = chooseResourcePool(ResourcePool.CPU_TYPE, task);
				ResourcePool memPool = chooseResourcePool(ResourcePool.MEMORY_TYPE, task);

				// the task could create a new server if it would not free one
				if (cpuPool != null && memPool != null) {
					task.setCouldCreateNewServer(true);
				}

				pendingQueue.add(task);
				return false;
				
			} else {
				// create new logicalServer
				LogicalServer newLServer = createLogicalServer(task);
				
				if (newLServer != null && task.getPriority() <= 1) {
					// a new server could be created but will not because the task is free
					task.setCouldCreateNewServer(true);
					
				}
				
				if (newLServer != null) {				
					logicalServers.add(newLServer);
					newLServer.allocate(task);
					return true;
					
				} else { // there was not resource to create a new logical server that fulfills the task
					
					pendingQueue.add(task);
					return false;
				}
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

	public Map<String, List<ResourcePool>> getResourcePools() {
		return resourcePools;
	}

	public double getCpuResourceGrain() {
		return cpuResourceGrain;
	}
	
	public double getMemResourceGrain() {
		return memResourceGrain;
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
	
	public boolean isConstraintsOn() {
		return constraintsOn;
	}	

	public boolean isAntiAffinityOn() {
		return isAntiAffinityOn;
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

//class RandomCollection<E> {
//    private final NavigableMap<Double, E> map = new TreeMap<Double, E>();
//    private final Random random;
//    private double total = 0;
//
//    public RandomCollection() {
//        this(new Random());
//    }
//
//    public RandomCollection(Random random) {
//        this.random = random;
//    }
//
//    public RandomCollection<E> add(double weight, E result) {
//        if (weight <= 0) return this;
//        total += weight;
//        map.put(total, result);
//        return this;
//    }
//
//    public E next() {
//        double value = random.nextDouble() * total;
//        return map.higherEntry(value).getValue();
//    }
//}
