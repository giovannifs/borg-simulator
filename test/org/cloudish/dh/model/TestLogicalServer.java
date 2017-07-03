package org.cloudish.dh.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.ResourceAttribute;
import org.cloudish.borg.model.Task;
import org.cloudish.borg.model.TaskConstraint;
import org.cloudish.dh.DHManager;
import org.cloudish.score.KubernetesRankingScore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class TestLogicalServer {

	private static final int MEM_CAPACITY = 10;
	private static final int CPU_CAPACITY = 30;

	private static final double ACCEPTABLE_DIFF = 0.00000001;

	private LogicalServer logicalServer;
	
	@SuppressWarnings("serial")
	@Before
	public void setUp() {
		Map<String, ResourceAttribute> poolAttributes = new HashMap<>();
		poolAttributes.put("9e", new ResourceAttribute("9e", "2"));
		poolAttributes.put("nZ", new ResourceAttribute("nZ", "2"));
		poolAttributes.put("rs", new ResourceAttribute("rs", "0"));
		poolAttributes.put("w2", new ResourceAttribute("w2", "1"));
		poolAttributes.put("w5", new ResourceAttribute("w5", "1"));
		
		ResourcePool cpuPool = new ResourcePool(ResourcePool.CPU_TYPE, poolAttributes);
		ResourcePool memPool = new ResourcePool(ResourcePool.MEMORY_TYPE, new HashMap<>());
		
		Host host = new Host(0, CPU_CAPACITY, MEM_CAPACITY, new KubernetesRankingScore(), poolAttributes);
		
		cpuPool.incorporateHost(host);
		memPool.incorporateHost(host);
		
		Properties properties = new Properties();

		properties.put("cpu_resource_grain", "0.1");
		properties.put("mem_resource_grain", "0.1");
		properties.put("min_logical_servers", "1");
		properties.put("max_cpu_logical_server_capacity", "1");
		properties.put("max_memory_logical_server_capacity","1");

		Map<String, List<ResourcePool>> resourcePools = new HashMap<>();
		resourcePools.put(ResourcePool.CPU_TYPE, new ArrayList<ResourcePool>() {{
			add(cpuPool);
		}});
		resourcePools.put(ResourcePool.MEMORY_TYPE, new ArrayList<ResourcePool>() {{
			add(memPool);
			}});
		
		ArrayList<String> possibleGKValues = new ArrayList<>();
		possibleGKValues.add("Ul");
		possibleGKValues.add("Ai");
		possibleGKValues.add("Oi");
		possibleGKValues.add("Sp");
		
		DHManager dhManager = new DHManager(properties, resourcePools, possibleGKValues);
		
		Assert.assertEquals(CPU_CAPACITY, cpuPool.getCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(CPU_CAPACITY, cpuPool.getFreeCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(MEM_CAPACITY, memPool.getCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(MEM_CAPACITY, memPool.getFreeCapacity(), ACCEPTABLE_DIFF);
		
		logicalServer = new LogicalServer(cpuPool, memPool, 1, 1, 0.1, 0.1, dhManager);
		
		// checking logical server initial configuration
		Assert.assertEquals(0.1, logicalServer.getCpuCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getFreeCPU(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getMemCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getFreeMem(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getCpuResourceGrain(), ACCEPTABLE_DIFF);
		Assert.assertEquals(cpuPool, logicalServer.getCpuPool());
		Assert.assertEquals(memPool, logicalServer.getMemPool());
		Assert.assertEquals(1, logicalServer.getMaxCpuCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(1, logicalServer.getMaxMemCapacity(), ACCEPTABLE_DIFF);
		
		Assert.assertEquals("", logicalServer.getGKAttr());
		Assert.assertEquals(1, logicalServer.getQlAttr());
		Assert.assertEquals(Integer.MAX_VALUE, logicalServer.getMaxAllowedQlAtt());
		Assert.assertEquals(1, logicalServer.getMinAllowedQlAtt());
		
		// checking if free capacity of pools were decreased
		Assert.assertEquals(30, cpuPool.getCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(30 - 0.1, cpuPool.getFreeCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(10, memPool.getCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(10 - 0.1, memPool.getFreeCapacity(), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testIsFeasible() {		
		// task requiring lower capacity
		Task task = new Task(0, 10, 0.05, 0.05, 11, false, new ArrayList<>());
		Assert.assertTrue(logicalServer.isFeasible(task));
		
		// task requiring same capacity
		task = new Task(0, 10, 0.1, 0.05, 11, false, new ArrayList<>());
		Assert.assertTrue(logicalServer.isFeasible(task));
		
		// task requiring more than capacity but lower than max
		task = new Task(0, 10, 0.2, 0.05, 11, false, new ArrayList<>());
		Assert.assertTrue(logicalServer.isFeasible(task));
		
		// task requiring more than capacity but lower than max
		task = new Task(0, 10, 0.9, 0.05, 11, false, new ArrayList<>());
		Assert.assertTrue(logicalServer.isFeasible(task));
		
		// task requiring more than capacity but equal to max server capacity
		task = new Task(0, 10, 1, 0.05, 11, false, new ArrayList<>());
		Assert.assertTrue(logicalServer.isFeasible(task));

		// task requiring more than capacity and max server capacity
		task = new Task(0, 10, 1.1, 0.05, 11, false, new ArrayList<>());
		Assert.assertFalse(logicalServer.isFeasible(task));
		
		// checking that logical server does not change its configuration
		Assert.assertEquals(0.1, logicalServer.getCpuCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getFreeCPU(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getMemCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getFreeMem(), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testIsFeasibleWithConstraint() {
		// constraints that match with cpu pool
		List<TaskConstraint> constraints = new ArrayList<>();
		constraints.add(new TaskConstraint("9e", "==", "2"));
		constraints.add(new TaskConstraint("rs", "!=", "1"));
		constraints.add(new TaskConstraint("w5", ">", "0"));
		
		Task task = new Task(0, 10, 0.05, 0.05, 11, false, constraints);
		Assert.assertTrue(logicalServer.isFeasible(task));

		// constraints that do not match with cpu pool
		constraints = new ArrayList<>();
		constraints.add(new TaskConstraint("9e", "!=", "2"));
		constraints.add(new TaskConstraint("rs", "==", "1"));

		task = new Task(0, 10, 0.05, 0.05, 11, false, constraints);
		Assert.assertFalse(logicalServer.isFeasible(task));
		
		// constraints that do not match with cpu pool
		constraints = new ArrayList<>();
		constraints.add(new TaskConstraint("St", "==", "1"));

		task = new Task(0, 10, 0.05, 0.05, 11, false, constraints);
		Assert.assertFalse(logicalServer.isFeasible(task));

		// constraints with GK attribute
		constraints = new ArrayList<>();
		constraints.add(new TaskConstraint("GK", "!=", "Al"));

		task = new Task(0, 10, 0.05, 0.05, 11, false, constraints);
		Assert.assertTrue(logicalServer.isFeasible(task));
		
		// constraints with Ql attribute
		constraints = new ArrayList<>();
		constraints.add(new TaskConstraint("Ql", "<", "5"));
		constraints.add(new TaskConstraint("Ql", ">", "0"));

		task = new Task(0, 10, 0.05, 0.05, 11, false, constraints);
		Assert.assertTrue(logicalServer.isFeasible(task));
				
		// checking that logical server does not change its configuration
		Assert.assertEquals(0.1, logicalServer.getCpuCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getFreeCPU(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getMemCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getFreeMem(), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testIsFeasibleBasedOnPoolCapacity() {
		logicalServer.setMaxCpuCapacity(Integer.MAX_VALUE);
		
		Task task = new Task(0, 10, 1, 0.05, 11, false, new ArrayList<>());
		Assert.assertTrue(logicalServer.isFeasible(task));

		task = new Task(0, 10, 5, 0.05, 11, false, new ArrayList<>());
		Assert.assertTrue(logicalServer.isFeasible(task));
		
		task = new Task(0, 10, 15, 0.05, 11, false, new ArrayList<>());
		Assert.assertTrue(logicalServer.isFeasible(task));

		task = new Task(0, 10, CPU_CAPACITY, 0.05, 11, false, new ArrayList<>());
		Assert.assertTrue(logicalServer.isFeasible(task));
		
		task = new Task(0, 10, CPU_CAPACITY + 0.01, 0.05, 11, false, new ArrayList<>());
		Assert.assertFalse(logicalServer.isFeasible(task));
		
		logicalServer.setMaxMemCapacity(Integer.MAX_VALUE);
		
		task = new Task(0, 10, 0.05, 1, 11, false, new ArrayList<>());
		Assert.assertTrue(logicalServer.isFeasible(task));

		task = new Task(0, 10, 0.05, 5, 11, false, new ArrayList<>());
		Assert.assertTrue(logicalServer.isFeasible(task));
		
		task = new Task(0, 10, 0.05, 9, 11, false, new ArrayList<>());
		Assert.assertTrue(logicalServer.isFeasible(task));

		task = new Task(0, 10, 0.05, MEM_CAPACITY, 11, false, new ArrayList<>());
		Assert.assertTrue(logicalServer.isFeasible(task));
		
		task = new Task(0, 10, 0.05, MEM_CAPACITY + 0.1, 11, false, new ArrayList<>());
		Assert.assertFalse(logicalServer.isFeasible(task));
				
		// checking that logical server does not change its configuration
		Assert.assertEquals(0.1, logicalServer.getCpuCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getFreeCPU(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getMemCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getFreeMem(), ACCEPTABLE_DIFF);
	}

	@Test
	public void testCalCpuToBeRequested() {		
		// resource grain is 0.1
		Task task = new Task(0, 10, 1, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(0.9, logicalServer.calcCpuToBeRequested(task), ACCEPTABLE_DIFF);

		task = new Task(0, 10, 0.09, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(0, logicalServer.calcCpuToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.1, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(0, logicalServer.calcCpuToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.15, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(0.1, logicalServer.calcCpuToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.5, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(0.4, logicalServer.calcCpuToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.55, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(0.5, logicalServer.calcCpuToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.21, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(0.2, logicalServer.calcCpuToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.10001, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(0.1, logicalServer.calcCpuToBeRequested(task), ACCEPTABLE_DIFF);
	}	
	
	@Test
	public void testCalCpuToBeRequested2() {		
		// resource grain is 0.001
		logicalServer.setCpuResourceGrain(0.001);
		logicalServer.setMemResourceGrain(0.001);
		
		Task task = new Task(0, 10, 1, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(0.9, logicalServer.calcCpuToBeRequested(task), ACCEPTABLE_DIFF);

		task = new Task(0, 10, 0.09, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(0, logicalServer.calcCpuToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.1, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(0, logicalServer.calcCpuToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.15, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(0.05, logicalServer.calcCpuToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.5, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(0.4, logicalServer.calcCpuToBeRequested(task), ACCEPTABLE_DIFF);
		task = new Task(0, 10, 0.21, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(0.11, logicalServer.calcCpuToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.10001, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(0.001, logicalServer.calcCpuToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.555, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(0.455, logicalServer.calcCpuToBeRequested(task), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testCalMemToBeRequested() {		
		// resource grain is 0.1
		Task task = new Task(0, 10, 0.05, 1, 11, false, new ArrayList<>());
		Assert.assertEquals(0.9, logicalServer.calcMemToBeRequested(task), ACCEPTABLE_DIFF);

		task = new Task(0, 10, 0.05, 0.09, 11, false, new ArrayList<>());
		Assert.assertEquals(0, logicalServer.calcMemToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.05, 0.1, 11, false, new ArrayList<>());
		Assert.assertEquals(0, logicalServer.calcMemToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.05, 0.15, 11, false, new ArrayList<>());
		Assert.assertEquals(0.1, logicalServer.calcMemToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.05, 0.5, 11, false, new ArrayList<>());
		Assert.assertEquals(0.4, logicalServer.calcMemToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.05, 0.55, 11, false, new ArrayList<>());
		Assert.assertEquals(0.5, logicalServer.calcMemToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.05, 0.21, 11, false, new ArrayList<>());
		Assert.assertEquals(0.2, logicalServer.calcMemToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.05, 0.10001, 11, false, new ArrayList<>());
		Assert.assertEquals(0.1, logicalServer.calcMemToBeRequested(task), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testCalMemToBeRequested2() {		
		// resource grain is 0.001
		logicalServer.setCpuResourceGrain(0.001);
		logicalServer.setMemResourceGrain(0.001);

		Task task = new Task(0, 10, 0.05, 1, 11, false, new ArrayList<>());
		Assert.assertEquals(0.9, logicalServer.calcMemToBeRequested(task), ACCEPTABLE_DIFF);

		task = new Task(0, 10, 0.05, 0.09, 11, false, new ArrayList<>());
		Assert.assertEquals(0, logicalServer.calcMemToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.05, 0.1, 11, false, new ArrayList<>());
		Assert.assertEquals(0, logicalServer.calcMemToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.05, 0.15, 11, false, new ArrayList<>());
		Assert.assertEquals(0.05, logicalServer.calcMemToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.05, 0.5, 11, false, new ArrayList<>());
		Assert.assertEquals(0.4, logicalServer.calcMemToBeRequested(task), ACCEPTABLE_DIFF);
		task = new Task(0, 10, 0.05, 0.21, 11, false, new ArrayList<>());
		Assert.assertEquals(0.11, logicalServer.calcMemToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.05, 0.10001, 11, false, new ArrayList<>());
		Assert.assertEquals(0.001, logicalServer.calcMemToBeRequested(task), ACCEPTABLE_DIFF);
		
		task = new Task(0, 10, 0.05, 0.555, 11, false, new ArrayList<>());
		Assert.assertEquals(0.455, logicalServer.calcMemToBeRequested(task), ACCEPTABLE_DIFF);
	}

	@Test
	public void testGetScore() {		
		// checking if score is calculated or logical server is not feasible
		Task task = new Task(0, 10, 0.05, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(20, logicalServer.getScore(task), ACCEPTABLE_DIFF);

		// task requiring less than logical server capacity
		task = new Task(0, 10, 0.09, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(20, logicalServer.getScore(task), ACCEPTABLE_DIFF);
		
		// task requiring equal to logical server capacity
		task = new Task(0, 10, 0.1, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(20, logicalServer.getScore(task), ACCEPTABLE_DIFF);
		
		// task requiring greater than logical server capacity but less than max capacity
		task = new Task(0, 10, 0.2, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(20, logicalServer.getScore(task), ACCEPTABLE_DIFF);
		
		// task requiring greater than logical server capacity but equal to max capacity
		task = new Task(0, 10, 1, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(20, logicalServer.getScore(task), ACCEPTABLE_DIFF);
		
		// task requiring greater than logical server capacity and max capacity
		task = new Task(0, 10, 1.1, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(-1, logicalServer.getScore(task), ACCEPTABLE_DIFF);
		
		// increasing the max capacity for cpu
		logicalServer.setMaxCpuCapacity(Integer.MAX_VALUE);
		
		// task requiring greater than logical server capacity and equal to pool capacity
		task = new Task(0, 10, CPU_CAPACITY, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(20, logicalServer.getScore(task), ACCEPTABLE_DIFF);

		// task requiring greater than logical server capacity and more than pool capacity
		task = new Task(0, 10, CPU_CAPACITY + 1, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(-1, logicalServer.getScore(task), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testGetScore2() {		
		// checking if score is calculated or logical server is not feasible
		Task task = new Task(0, 10, 0.05, 0.05, 11, false, new ArrayList<>());
		Assert.assertEquals(20, logicalServer.getScore(task), ACCEPTABLE_DIFF);

		// task requiring less than logical server capacity
		task = new Task(0, 10, 0.05, 0.09, 11, false, new ArrayList<>());
		Assert.assertEquals(20, logicalServer.getScore(task), ACCEPTABLE_DIFF);
		
		// task requiring equal to logical server capacity
		task = new Task(0, 10, 0.05, 0.1, 11, false, new ArrayList<>());
		Assert.assertEquals(20, logicalServer.getScore(task), ACCEPTABLE_DIFF);
		
		// task requiring greater than logical server capacity but less than max capacity
		task = new Task(0, 10, 0.05, 0.2, 11, false, new ArrayList<>());
		Assert.assertEquals(20, logicalServer.getScore(task), ACCEPTABLE_DIFF);
		
		// task requiring greater than logical server capacity but equal to max capacity
		task = new Task(0, 10, 0.05, 1, 11, false, new ArrayList<>());
		Assert.assertEquals(20, logicalServer.getScore(task), ACCEPTABLE_DIFF);
		
		// task requiring greater than logical server capacity and max capacity
		task = new Task(0, 10, 0.05, 1.1, 11, false, new ArrayList<>());
		Assert.assertEquals(-1, logicalServer.getScore(task), ACCEPTABLE_DIFF);
		
		// increasing the max capacity for mem
		logicalServer.setMaxMemCapacity(Integer.MAX_VALUE);
		
		// task requiring greater than logical server capacity and equal to pool capacity
		task = new Task(0, 10, 0.05, MEM_CAPACITY, 11, false, new ArrayList<>());
		Assert.assertEquals(20, logicalServer.getScore(task), ACCEPTABLE_DIFF);

		// task requiring greater than logical server capacity and more than pool capacity
		task = new Task(0, 10, 0.05, MEM_CAPACITY + 1, 11, false, new ArrayList<>());
		Assert.assertEquals(-1, logicalServer.getScore(task), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testAllocateWithoutConstraint() {
		Task task = new Task(0, 10, 0.05, 0.05, 11, false, new ArrayList<>());
		logicalServer.allocate(task);
		
		// checking that logical server does not change its configuration
		Assert.assertEquals(0.1, logicalServer.getCpuCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.05, logicalServer.getFreeCPU(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getMemCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.05, logicalServer.getFreeMem(), ACCEPTABLE_DIFF);
		
		Assert.assertTrue(logicalServer.getJidAllocated().isEmpty());

		Assert.assertEquals("", logicalServer.getGKAttr());
		Assert.assertEquals(1, logicalServer.getQlAttr());
		Assert.assertEquals(Integer.MAX_VALUE, logicalServer.getMaxAllowedQlAtt());
		Assert.assertEquals(1, logicalServer.getMinAllowedQlAtt());
	}
	
	
	@Test
	public void testAllocateScalingUpCPU() {
		Task task = new Task(0, 10, 0.49, 0.05, 11, false, new ArrayList<>());
		logicalServer.allocate(task);
		
		// checking that logical server does not change its configuration
		Assert.assertEquals(0.5, logicalServer.getCpuCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.01, logicalServer.getFreeCPU(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getMemCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.05, logicalServer.getFreeMem(), ACCEPTABLE_DIFF);
		
		Assert.assertTrue(logicalServer.getJidAllocated().isEmpty());

		Assert.assertEquals("", logicalServer.getGKAttr());
		Assert.assertEquals(1, logicalServer.getQlAttr());
		Assert.assertEquals(Integer.MAX_VALUE, logicalServer.getMaxAllowedQlAtt());
		Assert.assertEquals(1, logicalServer.getMinAllowedQlAtt());
	}
	
	@Test(expected=Exception.class)
	public void testAllocateScalingUpCpu2() {
		
		// checking that logical server does not change its configuration
		Assert.assertEquals(0.1, logicalServer.getCpuCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getFreeCPU(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getMemCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getFreeMem(), ACCEPTABLE_DIFF);
		Assert.assertEquals(1, logicalServer.getMaxCpuCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(1, logicalServer.getMaxMemCapacity(), ACCEPTABLE_DIFF);
		
		Task task = new Task(0, 10, 1.1, 0.05, 11, false, new ArrayList<>());
		logicalServer.allocate(task);
	}
	
	@Test
	public void testAllocateScalingUpMem() {
		Task task = new Task(0, 10, 0.05, 0.85, 11, false, new ArrayList<>());
		logicalServer.allocate(task);
		
		// checking that logical server does not change its configuration
		Assert.assertEquals(0.1, logicalServer.getCpuCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.05, logicalServer.getFreeCPU(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.9, logicalServer.getMemCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.05, logicalServer.getFreeMem(), ACCEPTABLE_DIFF);
		
		Assert.assertTrue(logicalServer.getJidAllocated().isEmpty());

		Assert.assertEquals("", logicalServer.getGKAttr());
		Assert.assertEquals(1, logicalServer.getQlAttr());
		Assert.assertEquals(Integer.MAX_VALUE, logicalServer.getMaxAllowedQlAtt());
		Assert.assertEquals(1, logicalServer.getMinAllowedQlAtt());
	}
	
	@Test(expected=Exception.class)
	public void testAllocateScalingUpMem2() {
		
		// checking that logical server does not change its configuration
		Assert.assertEquals(0.1, logicalServer.getCpuCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getFreeCPU(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getMemCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getFreeMem(), ACCEPTABLE_DIFF);
		Assert.assertEquals(1, logicalServer.getMaxCpuCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(1, logicalServer.getMaxMemCapacity(), ACCEPTABLE_DIFF);
		
		Task task = new Task(0, 10, 0.05, 1.1, 11, false, new ArrayList<>());
		logicalServer.allocate(task);
	}
	
	@Test
	public void testAllocateWithConstraint() {
		List<TaskConstraint> constraints = new ArrayList<>();
		constraints.add(new TaskConstraint("9e", "==", "2"));
		constraints.add(new TaskConstraint("w2", ">", "0"));
		
		Task task = new Task(0, 10, 0.05, 0.05, 11, false, constraints);
		logicalServer.allocate(task);
		
		// checking that logical server does not change its configuration
		Assert.assertEquals(0.1, logicalServer.getCpuCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.05, logicalServer.getFreeCPU(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getMemCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.05, logicalServer.getFreeMem(), ACCEPTABLE_DIFF);
		
		Assert.assertTrue(logicalServer.getJidAllocated().isEmpty());

		Assert.assertEquals("", logicalServer.getGKAttr());
		Assert.assertEquals(1, logicalServer.getQlAttr());
		Assert.assertEquals(Integer.MAX_VALUE, logicalServer.getMaxAllowedQlAtt());
		Assert.assertEquals(1, logicalServer.getMinAllowedQlAtt());
	}
	
	@Test
	public void testAllocateWithAntiaffinity() {		
		Task task = new Task(0, 10, 0.05, 0.05, 11, true, new ArrayList<>());
		logicalServer.allocate(task);
		
		// checking that logical server does not change its configuration
		Assert.assertEquals(0.1, logicalServer.getCpuCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.05, logicalServer.getFreeCPU(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getMemCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.05, logicalServer.getFreeMem(), ACCEPTABLE_DIFF);
		
		Assert.assertEquals(1, logicalServer.getJidAllocated().size());
		Assert.assertEquals(new Long(10), logicalServer.getJidAllocated().get(0));

		Assert.assertEquals("", logicalServer.getGKAttr());
		Assert.assertEquals(1, logicalServer.getQlAttr());
		Assert.assertEquals(Integer.MAX_VALUE, logicalServer.getMaxAllowedQlAtt());
		Assert.assertEquals(1, logicalServer.getMinAllowedQlAtt());
	}
	
	@Test
	public void testAllocateWithGKConstraint() {
		List<TaskConstraint> constraints = new ArrayList<>();
		constraints.add(new TaskConstraint("GK", "==", "Ul"));
		
		Assert.assertTrue(logicalServer.getDhManager().isGKValueAvailable("Ul"));
		
		Task task = new Task(0, 10, 0.05, 0.05, 11, false, constraints);
		logicalServer.allocate(task);
		
		// checking that logical server does not change its configuration
		Assert.assertEquals(0.1, logicalServer.getCpuCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.05, logicalServer.getFreeCPU(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getMemCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.05, logicalServer.getFreeMem(), ACCEPTABLE_DIFF);
		
		Assert.assertTrue(logicalServer.getJidAllocated().isEmpty());
		Assert.assertFalse(logicalServer.getDhManager().isGKValueAvailable("Ul"));

		Assert.assertEquals("Ul", logicalServer.getGKAttr());
		Assert.assertEquals(1, logicalServer.getQlAttr());
		Assert.assertEquals(Integer.MAX_VALUE, logicalServer.getMaxAllowedQlAtt());
		Assert.assertEquals(1, logicalServer.getMinAllowedQlAtt());
		
		// checking if other task with GK constraint could be allocate in this host
		constraints = new ArrayList<>();
		constraints.add(new TaskConstraint("GK", "==", "Oi"));
		
		Assert.assertTrue(logicalServer.getDhManager().isGKValueAvailable("Oi"));
		
		task = new Task(0, 10, 0.05, 0.05, 11, false, constraints);
		Assert.assertFalse(logicalServer.isFeasible(task));		
	}
	
	@Test
	public void testAllocateWithGKDiffConstraint() {
		List<TaskConstraint> constraints = new ArrayList<>();
		constraints.add(new TaskConstraint("GK", "!=", "Ul"));
		
		Assert.assertTrue(logicalServer.getDhManager().isGKValueAvailable("Ul"));
		
		Task task = new Task(0, 10, 0.05, 0.05, 11, false, constraints);
		logicalServer.allocate(task);
		
		// checking that logical server does not change its configuration
		Assert.assertEquals(0.1, logicalServer.getCpuCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.05, logicalServer.getFreeCPU(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getMemCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.05, logicalServer.getFreeMem(), ACCEPTABLE_DIFF);
		
		Assert.assertTrue(logicalServer.getJidAllocated().isEmpty());
		Assert.assertTrue(logicalServer.getDhManager().isGKValueAvailable("Ul"));

		Assert.assertEquals("", logicalServer.getGKAttr());
		Assert.assertEquals(1, logicalServer.getQlAttr());
		Assert.assertEquals(Integer.MAX_VALUE, logicalServer.getMaxAllowedQlAtt());
		Assert.assertEquals(1, logicalServer.getMinAllowedQlAtt());
		
		// checking if other task with GK constraint could be allocate in this host
		constraints = new ArrayList<>();
		constraints.add(new TaskConstraint("GK", "==", "Ul"));
		
		Assert.assertTrue(logicalServer.getDhManager().isGKValueAvailable("Ul"));
		
		task = new Task(0, 10, 0.05, 0.05, 11, false, constraints);
		Assert.assertFalse(logicalServer.isFeasible(task));		
	}
	
	@Test
	public void testAllocateWithQlConstraint() {
		List<TaskConstraint> constraints = new ArrayList<>();
		constraints.add(new TaskConstraint("Ql", ">", "2"));
		
		Task task = new Task(0, 10, 0.05, 0.05, 11, false, constraints);
		logicalServer.allocate(task);
		
		// checking that logical server does not change its configuration
		Assert.assertEquals(0.1, logicalServer.getCpuCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.05, logicalServer.getFreeCPU(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getMemCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.05, logicalServer.getFreeMem(), ACCEPTABLE_DIFF);
		
		Assert.assertTrue(logicalServer.getJidAllocated().isEmpty());

		Assert.assertEquals("", logicalServer.getGKAttr());
		Assert.assertEquals(3, logicalServer.getQlAttr());
		Assert.assertEquals(Integer.MAX_VALUE, logicalServer.getMaxAllowedQlAtt());
		Assert.assertEquals(3, logicalServer.getMinAllowedQlAtt());
		
		// checking if other task with Ql constraint could be allocate in this host
		constraints = new ArrayList<>();
		constraints.add(new TaskConstraint("Ql", "<", "3"));
		
		task = new Task(0, 10, 0.05, 0.05, 11, false, constraints);
		Assert.assertFalse(logicalServer.isFeasible(task));		
	}
	
	@Test
	public void testAllocateWithQlConstraint2() {
		List<TaskConstraint> constraints = new ArrayList<>();
		constraints.add(new TaskConstraint("Ql", ">", "5"));
		constraints.add(new TaskConstraint("Ql", "<", "13"));
		
		Task task = new Task(0, 10, 0.05, 0.05, 11, false, constraints);
		logicalServer.allocate(task);
		
		// checking that logical server does not change its configuration
		Assert.assertEquals(0.1, logicalServer.getCpuCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.05, logicalServer.getFreeCPU(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.1, logicalServer.getMemCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.05, logicalServer.getFreeMem(), ACCEPTABLE_DIFF);
		
		Assert.assertTrue(logicalServer.getJidAllocated().isEmpty());

		Assert.assertEquals("", logicalServer.getGKAttr());
		Assert.assertEquals(12, logicalServer.getQlAttr());
		Assert.assertEquals(12, logicalServer.getMaxAllowedQlAtt());
		Assert.assertEquals(6, logicalServer.getMinAllowedQlAtt());
		
		// checking if other task with Ql constraint could be allocate in this host
		constraints = new ArrayList<>();
		constraints.add(new TaskConstraint("Ql", "<", "3"));
		
		task = new Task(0, 10, 0.05, 0.05, 11, false, constraints);
		Assert.assertFalse(logicalServer.isFeasible(task));		
		
		constraints = new ArrayList<>();
		constraints.add(new TaskConstraint("Ql", ">", "12"));
		
		task = new Task(0, 10, 0.05, 0.05, 11, false, constraints);
		Assert.assertFalse(logicalServer.isFeasible(task));
	}
	
}
