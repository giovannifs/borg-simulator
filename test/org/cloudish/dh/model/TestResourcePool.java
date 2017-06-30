package org.cloudish.dh.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.ResourceAttribute;
import org.cloudish.borg.model.Task;
import org.cloudish.borg.model.TaskConstraint;
import org.cloudish.score.KubernetesRankingScore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestResourcePool {

	Map<String, ResourceAttribute> poolAttributes;
	
	@Before
	public void setUp() {
		poolAttributes = createResourceAttributes();
		
	}
	
	@Test
	public void testMatchWithSameAttributes() {			
		ResourcePool pool = new ResourcePool(ResourcePool.CPU_TYPE, poolAttributes);
		
		Map<String, ResourceAttribute> hostAttributes = createResourceAttributes();
		
		Host host = new Host(0, 10, 10, new KubernetesRankingScore(), hostAttributes);
		
		Assert.assertTrue(pool.match(host));
	}
	
	@Test
	public void testMatchHostWithDiffValue() {			
		ResourcePool pool = new ResourcePool(ResourcePool.CPU_TYPE, poolAttributes);
		
		Map<String, ResourceAttribute> hostAttributes = createResourceAttributes();
		
		Host host = new Host(0, 10, 10, new KubernetesRankingScore(), hostAttributes);
		host.getAttributes().put("9e", new ResourceAttribute("9e", "1"));
		
		Assert.assertFalse(pool.match(host));
	}
	
	@Test
	public void testMatchHostHasMoreCpuAttribute() {			
		ResourcePool pool = new ResourcePool(ResourcePool.CPU_TYPE, poolAttributes);
		
		Map<String, ResourceAttribute> hostAttributes = createResourceAttributes();
		
		Host host = new Host(0, 10, 10, new KubernetesRankingScore(), hostAttributes);
		host.getAttributes().put("St", new ResourceAttribute("St", "1"));
		
		Assert.assertFalse(pool.match(host));
	}
	
	@Test
	public void testMatchHostHasMoreNonCpuAttribute() {			
		ResourcePool pool = new ResourcePool(ResourcePool.CPU_TYPE, poolAttributes);
		
		Map<String, ResourceAttribute> hostAttributes = createResourceAttributes();
		
		Host host = new Host(0, 10, 10, new KubernetesRankingScore(), hostAttributes);
		host.getAttributes().put("Ql", new ResourceAttribute("Ql", "5"));
		host.getAttributes().put("GK", new ResourceAttribute("GK", "Al"));
		
		Assert.assertTrue(pool.match(host));
	}
	
	@Test
	public void testMatchMemoryPool() {			
		ResourcePool pool = new ResourcePool(ResourcePool.MEMORY_TYPE, poolAttributes);
		
		Map<String, ResourceAttribute> hostAttributes = createResourceAttributes();
		
		Host host = new Host(0, 10, 10, new KubernetesRankingScore(), hostAttributes);
		Assert.assertTrue(pool.match(host));
		
		// different value
		host.getAttributes().put("9e", new ResourceAttribute("9e", "1"));
		Assert.assertTrue(pool.match(host));
		
		// host with more cpu attribute
		host.getAttributes().put("St", new ResourceAttribute("St", "1"));		
		Assert.assertTrue(pool.match(host));
		
		// host with more non cpu attribute
		host.getAttributes().put("Ql", new ResourceAttribute("Ql", "5"));
		host.getAttributes().put("GK", new ResourceAttribute("GK", "Al"));
		
		Assert.assertTrue(pool.match(host));		
	}

	private Map<String, ResourceAttribute> createResourceAttributes() {
		Map<String, ResourceAttribute> resourceAttributes = new HashMap<>();
		resourceAttributes.put("9e", new ResourceAttribute("9e", "2"));
		resourceAttributes.put("nZ", new ResourceAttribute("nZ", "2"));
		resourceAttributes.put("rs", new ResourceAttribute("rs", "0"));
		resourceAttributes.put("w2", new ResourceAttribute("w2", "1"));
		resourceAttributes.put("w5", new ResourceAttribute("w5", "1"));
		return resourceAttributes;
	}
	
	
	@Test
	public void testIsFeasibleWithNoConstraint() {
		ResourcePool pool = new ResourcePool(ResourcePool.CPU_TYPE, poolAttributes);

		Task task = new Task(0, 10, 0.5, 0.5, 11, true, new ArrayList<>());
		
		Assert.assertTrue(pool.isFeasible(task));
	}
	
	@Test
	public void testIsFeasibleWithConstraint() {
		ResourcePool pool = new ResourcePool(ResourcePool.CPU_TYPE, poolAttributes);

		// scenario 1
		ArrayList<TaskConstraint> constraints = new ArrayList<>();
		constraints.add(new TaskConstraint("9e", "==", "2"));
		constraints.add(new TaskConstraint("w5", ">", "0"));
		
		Task task = new Task(0, 10, 0.5, 0.5, 11, true, constraints);
		
		Assert.assertTrue(pool.isFeasible(task));
		
		// scenario 2
		constraints = new ArrayList<>();
		constraints.add(new TaskConstraint("9e", "!=", "1"));
		constraints.add(new TaskConstraint("w5", ">", "0"));
		constraints.add(new TaskConstraint("rs", "==", "0"));
		constraints.add(new TaskConstraint("w2", "!=", "0"));
		
		task = new Task(0, 10, 0.5, 0.5, 11, true, constraints);
		
		Assert.assertTrue(pool.isFeasible(task));

		// scenario 3
		constraints = new ArrayList<>();
		constraints.add(new TaskConstraint("9e", "==", "1"));
		
		task = new Task(0, 10, 0.5, 0.5, 11, true, constraints);
		
		Assert.assertFalse(pool.isFeasible(task));

		// scenario 4 (non existing attribute in the pool)
		constraints = new ArrayList<>();
		constraints.add(new TaskConstraint("St", "==", "1"));
		
		task = new Task(0, 10, 0.5, 0.5, 11, true, constraints);
		
		Assert.assertFalse(pool.isFeasible(task));
	}


}
