package org.cloudish.dh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.ResourceAttribute;
import org.cloudish.borg.model.Task;
import org.cloudish.borg.model.TaskConstraint;
import org.cloudish.dh.model.ResourcePool;
import org.cloudish.score.KubernetesRankingScore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestUtils {

	private static final double ACCEPTABLE_DIFF = 0.00000000001;

	Map<String, ResourceAttribute> attributes1;
	Map<String, ResourceAttribute> attributes2; 
	Map<String, ResourceAttribute> attributes3;
	
	@Before
	public void setUp() {
		attributes1 = new HashMap<>();
		attributes1.put("9e", new ResourceAttribute("9e", "value1"));
		attributes1.put("rs", new ResourceAttribute("rs", "value1"));
		attributes1.put("nZ", new ResourceAttribute("nZ", "value1"));
		
		attributes2 = new HashMap<>();
		attributes2.put("9e", new ResourceAttribute("9e", "value2"));
		attributes2.put("rs", new ResourceAttribute("rs", "value2"));
		attributes2.put("nZ", new ResourceAttribute("nZ", "value2"));
		
		attributes3 = new HashMap<>();
		attributes3.put("9e", new ResourceAttribute("9e", "value3"));
		attributes3.put("rs", new ResourceAttribute("rs", "value3"));
		attributes3.put("nZ", new ResourceAttribute("nZ", "value3"));
		
	}
	
	@Test
	public void testCreateResourcePoolsHostsWithoutAttr() {
		Host host1 = new Host(1, 10, 10, new KubernetesRankingScore(), new HashMap<>(), true);
		
		Host host2 = new Host(2, 20, 20, new KubernetesRankingScore(), new HashMap<>(), true);
		
		Host host3 = new Host(3, 30, 30, new KubernetesRankingScore(), new HashMap<>(), true);
		
		List<Host> hosts = new ArrayList<>();
		hosts.add(host1);
		hosts.add(host2);
		hosts.add(host3);

		// checking if pools are created correctly
		Map<String, List<ResourcePool>> pools = Utils.createResourcePoolsFromHosts(hosts, true);
		
		// memory pool
		List<ResourcePool> memPools = pools.get(ResourcePool.MEMORY_TYPE);
		Assert.assertEquals(1, memPools.size());
		Assert.assertEquals(60, memPools.get(0).getCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(60, memPools.get(0).getFreeCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0, memPools.get(0).getAttributes().size());
				
		// cpu pools
		List<ResourcePool> cpuPools = pools.get(ResourcePool.CPU_TYPE);
		Assert.assertEquals(1, cpuPools.size());
		Assert.assertEquals(60, cpuPools.get(0).getCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(60, cpuPools.get(0).getFreeCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0, cpuPools.get(0).getAttributes().size());
	}
	
	@Test
	public void testCreateResourcePoolsHostsWithSameAttrAndDiffValues() {
		Host host1 = new Host(1, 10, 10, new KubernetesRankingScore(), attributes1, true);
		
		Host host2 = new Host(2, 20, 20, new KubernetesRankingScore(), attributes2, true);

		Host host3 = new Host(3, 30, 30, new KubernetesRankingScore(), attributes3, true);
		
		List<Host> hosts = new ArrayList<>();
		hosts.add(host1);
		hosts.add(host2);
		hosts.add(host3);

		// checking if pools are created correctly
		Map<String, List<ResourcePool>> pools = Utils.createResourcePoolsFromHosts(hosts, true);
		
		// memory pool
		List<ResourcePool> memPools = pools.get(ResourcePool.MEMORY_TYPE);
		Assert.assertEquals(1, memPools.size());
		Assert.assertEquals(60, memPools.get(0).getCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(60, memPools.get(0).getFreeCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0, memPools.get(0).getAttributes().size());
				
		// cpu pools
		List<ResourcePool> cpuPools = pools.get(ResourcePool.CPU_TYPE);
		Assert.assertEquals(3, cpuPools.size());
		
		boolean capacity10 = false;
		boolean capacity20 = false;
		boolean capacity30 = false;
		
		for (ResourcePool cpuPool : cpuPools) {
			if (cpuPool.getCapacity() == 10) {
				Assert.assertEquals(10, cpuPool.getCapacity(), ACCEPTABLE_DIFF);
				Assert.assertEquals(10, cpuPool.getFreeCapacity(), ACCEPTABLE_DIFF);
				Assert.assertEquals(attributes1, cpuPool.getAttributes());
					
				capacity10 = true;
			} else if (cpuPool.getCapacity() == 20) {
				Assert.assertEquals(20, cpuPool.getCapacity(), ACCEPTABLE_DIFF);
				Assert.assertEquals(20, cpuPool.getFreeCapacity(), ACCEPTABLE_DIFF);
				Assert.assertEquals(attributes2, cpuPool.getAttributes());
				
				capacity20 = true;
			} else if (cpuPool.getCapacity() == 30) {
				Assert.assertEquals(30, cpuPool.getCapacity(), ACCEPTABLE_DIFF);
				Assert.assertEquals(30, cpuPool.getFreeCapacity(), ACCEPTABLE_DIFF);
				Assert.assertEquals(attributes3, cpuPool.getAttributes());
				
				capacity30 = true;
			} else {
				Assert.fail("CPU pool was created with wrong capacity.");
			}
		}
		
		Assert.assertTrue(capacity10);
		Assert.assertTrue(capacity20);
		Assert.assertTrue(capacity30);
	}
	
	
	@Test
	public void testCreateResourcePoolsHostsWithSameAttrAndValues() {
		Host host1 = new Host(1, 10, 10, new KubernetesRankingScore(), attributes1, true);
		
		attributes2.get("9e").setAttValue("value1");
		attributes2.get("rs").setAttValue("value1");
		attributes2.get("nZ").setAttValue("value1");
		
		Host host2 = new Host(2, 20, 20, new KubernetesRankingScore(), attributes2, true);

		attributes3.get("9e").setAttValue("value1");
		attributes3.get("rs").setAttValue("value1");
		attributes3.get("nZ").setAttValue("value1");
		
		Host host3 = new Host(3, 30, 30, new KubernetesRankingScore(), attributes3, true);
		
		List<Host> hosts = new ArrayList<>();
		hosts.add(host1);
		hosts.add(host2);
		hosts.add(host3);

		// checking if pools are created correctly
		Map<String, List<ResourcePool>> pools = Utils.createResourcePoolsFromHosts(hosts, true);
		
		// memory pool
		List<ResourcePool> memPools = pools.get(ResourcePool.MEMORY_TYPE);
		Assert.assertEquals(1, memPools.size());
		Assert.assertEquals(60, memPools.get(0).getCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(60, memPools.get(0).getFreeCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0, memPools.get(0).getAttributes().size());
				
		// cpu pools
		List<ResourcePool> cpuPools = pools.get(ResourcePool.CPU_TYPE);
		Assert.assertEquals(1, cpuPools.size());
		Assert.assertEquals(60, cpuPools.get(0).getCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(60, cpuPools.get(0).getFreeCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(3, cpuPools.get(0).getAttributes().size());
		Assert.assertEquals(attributes1, cpuPools.get(0).getAttributes());
	}
	
	@Test
	public void testCreateResourcePoolsHostsWithExtraNonCpuAttrAttrAndSameValues() {
		Host host1 = new Host(1, 10, 10, new KubernetesRankingScore(), attributes1, true);
		
		attributes2.get("9e").setAttValue("value1");
		attributes2.get("rs").setAttValue("value1");
		attributes2.get("nZ").setAttValue("value1");
		attributes2.put("GK", new ResourceAttribute("GK","Al"));

		Host host2 = new Host(2, 20, 20, new KubernetesRankingScore(), attributes2, true);

		attributes3.get("9e").setAttValue("value1");
		attributes3.get("rs").setAttValue("value1");
		attributes3.get("nZ").setAttValue("value1");
		attributes2.put("Qh", new ResourceAttribute("Qh","2"));

		
		Host host3 = new Host(3, 30, 30, new KubernetesRankingScore(), attributes3, true);
		
		List<Host> hosts = new ArrayList<>();
		hosts.add(host1);
		hosts.add(host2);
		hosts.add(host3);

		// checking if pools are created correctly
		Map<String, List<ResourcePool>> pools = Utils.createResourcePoolsFromHosts(hosts, true);
		
		// memory pool
		List<ResourcePool> memPools = pools.get(ResourcePool.MEMORY_TYPE);
		Assert.assertEquals(1, memPools.size());
		Assert.assertEquals(60, memPools.get(0).getCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(60, memPools.get(0).getFreeCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0, memPools.get(0).getAttributes().size());
				
		// cpu pools
		List<ResourcePool> cpuPools = pools.get(ResourcePool.CPU_TYPE);
		Assert.assertEquals(1, cpuPools.size());
		Assert.assertEquals(60, cpuPools.get(0).getCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(60, cpuPools.get(0).getFreeCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(3, cpuPools.get(0).getAttributes().size());
		Assert.assertEquals(attributes1, cpuPools.get(0).getAttributes());
	}
	
	@Test
	public void testCreateResourcePoolsHostsWithDiffAttr() {
		Host host1 = new Host(1, 10, 10, new KubernetesRankingScore(), attributes1, true);
		
		Map<String, ResourceAttribute> otherAttributes = new HashMap<>();
		otherAttributes.put("St", new ResourceAttribute("St", "value1"));
		otherAttributes.put("By", new ResourceAttribute("By", "value1"));
		otherAttributes.put("P8", new ResourceAttribute("P8", "value1"));
		
		Host host2 = new Host(2, 20, 20, new KubernetesRankingScore(), otherAttributes, true);

		Map<String, ResourceAttribute> otherAttributes2 = new HashMap<>();
		otherAttributes2.put("o/", new ResourceAttribute("o/", "value1"));
		otherAttributes2.put("w2", new ResourceAttribute("w2", "value1"));
		otherAttributes2.put("wN", new ResourceAttribute("wN", "value1"));
		
		Host host3 = new Host(3, 30, 30, new KubernetesRankingScore(), otherAttributes2, true);
		
		List<Host> hosts = new ArrayList<>();
		hosts.add(host1);
		hosts.add(host2);
		hosts.add(host3);

		// checking if pools are created correctly
		Map<String, List<ResourcePool>> pools = Utils.createResourcePoolsFromHosts(hosts, true);
		
		// memory pool
		List<ResourcePool> memPools = pools.get(ResourcePool.MEMORY_TYPE);
		Assert.assertEquals(1, memPools.size());
		Assert.assertEquals(60, memPools.get(0).getCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(60, memPools.get(0).getFreeCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0, memPools.get(0).getAttributes().size());
				
		// cpu pools
		List<ResourcePool> cpuPools = pools.get(ResourcePool.CPU_TYPE);
		Assert.assertEquals(3, cpuPools.size());
		
		boolean capacity10 = false;
		boolean capacity20 = false;
		boolean capacity30 = false;
		
		for (ResourcePool cpuPool : cpuPools) {
			if (cpuPool.getCapacity() == 10) {
				Assert.assertEquals(10, cpuPool.getCapacity(), ACCEPTABLE_DIFF);
				Assert.assertEquals(10, cpuPool.getFreeCapacity(), ACCEPTABLE_DIFF);
				Assert.assertEquals(attributes1, cpuPool.getAttributes());
					
				capacity10 = true;
			} else if (cpuPool.getCapacity() == 20) {
				Assert.assertEquals(20, cpuPool.getCapacity(), ACCEPTABLE_DIFF);
				Assert.assertEquals(20, cpuPool.getFreeCapacity(), ACCEPTABLE_DIFF);
				Assert.assertEquals(otherAttributes, cpuPool.getAttributes());
				
				capacity20 = true;
			} else if (cpuPool.getCapacity() == 30) {
				Assert.assertEquals(30, cpuPool.getCapacity(), ACCEPTABLE_DIFF);
				Assert.assertEquals(30, cpuPool.getFreeCapacity(), ACCEPTABLE_DIFF);
				Assert.assertEquals(otherAttributes2, cpuPool.getAttributes());
				
				capacity30 = true;
			} else {
				Assert.fail("CPU pool was created with wrong capacity.");
			}
		}
		
		Assert.assertTrue(capacity10);
		Assert.assertTrue(capacity20);
		Assert.assertTrue(capacity30);
	}
	
	@Test
	public void testCreateResourcePoolsHostsWithExtraNonCpuAttrAndDiffValues() {
		
		attributes1.put("Ql", new ResourceAttribute("Ql","15"));				
		Host host1 = new Host(1, 10, 10, new KubernetesRankingScore(), attributes1, true);
		
		attributes2.put("GK", new ResourceAttribute("GK","Al"));
		Host host2 = new Host(2, 20, 20, new KubernetesRankingScore(), attributes2, true);

		attributes3.put("Qh", new ResourceAttribute("Qh","3"));
		Host host3 = new Host(3, 30, 30, new KubernetesRankingScore(), attributes3, true);
		
		List<Host> hosts = new ArrayList<>();
		hosts.add(host1);
		hosts.add(host2);
		hosts.add(host3);

		// checking if pools are created correctly
		Map<String, List<ResourcePool>> pools = Utils.createResourcePoolsFromHosts(hosts, true);
		
		// memory pool
		List<ResourcePool> memPools = pools.get(ResourcePool.MEMORY_TYPE);
		Assert.assertEquals(1, memPools.size());
		Assert.assertEquals(60, memPools.get(0).getCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(60, memPools.get(0).getFreeCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0, memPools.get(0).getAttributes().size());
				
		// cpu pools
		List<ResourcePool> cpuPools = pools.get(ResourcePool.CPU_TYPE);
		Assert.assertEquals(3, cpuPools.size());
		
		boolean capacity10 = false;
		boolean capacity20 = false;
		boolean capacity30 = false;
		
		for (ResourcePool cpuPool : cpuPools) {
			if (cpuPool.getCapacity() == 10) {
				Assert.assertEquals(10, cpuPool.getCapacity(), ACCEPTABLE_DIFF);
				Assert.assertEquals(10, cpuPool.getFreeCapacity(), ACCEPTABLE_DIFF);
				Assert.assertEquals(Utils.filterCpuAttributes(attributes1), cpuPool.getAttributes());
					
				capacity10 = true;
			} else if (cpuPool.getCapacity() == 20) {
				Assert.assertEquals(20, cpuPool.getCapacity(), ACCEPTABLE_DIFF);
				Assert.assertEquals(20, cpuPool.getFreeCapacity(), ACCEPTABLE_DIFF);
				Assert.assertEquals(Utils.filterCpuAttributes(attributes2), cpuPool.getAttributes());
				
				capacity20 = true;
			} else if (cpuPool.getCapacity() == 30) {
				Assert.assertEquals(30, cpuPool.getCapacity(), ACCEPTABLE_DIFF);
				Assert.assertEquals(30, cpuPool.getFreeCapacity(), ACCEPTABLE_DIFF);
				Assert.assertEquals(Utils.filterCpuAttributes(attributes3), cpuPool.getAttributes());
				
				capacity30 = true;
			} else {
				Assert.fail("CPU pool was created with wrong capacity.");
			}
		}
		
		Assert.assertTrue(capacity10);
		Assert.assertTrue(capacity20);
		Assert.assertTrue(capacity30);
	}
	
	@Test
	public void testGetGKValues() {
		attributes1.put("GK", new ResourceAttribute("GK","Tu"));				
		Host host1 = new Host(1, 10, 10, new KubernetesRankingScore(), attributes1, true);
		
		attributes2.put("GK", new ResourceAttribute("GK","Al"));
		Host host2 = new Host(2, 20, 20, new KubernetesRankingScore(), attributes2, true);

		Host host3 = new Host(3, 30, 30, new KubernetesRankingScore(), attributes3, true);
		
		List<Host> hosts = new ArrayList<>();
		hosts.add(host1);
		hosts.add(host2);
		hosts.add(host3);

		List<String> GKValues = Utils.getPossibleGKValues(hosts);
		
		Assert.assertEquals(2, GKValues.size());
		Assert.assertTrue(GKValues.contains("Tu"));
		Assert.assertTrue(GKValues.contains("Al"));
	}

	@Test
	public void testGetGKValuesWithoutGK() {
		Host host1 = new Host(1, 10, 10, new KubernetesRankingScore(), attributes1, true);
		
		Host host2 = new Host(2, 20, 20, new KubernetesRankingScore(), attributes2, true);

		Host host3 = new Host(3, 30, 30, new KubernetesRankingScore(), attributes3, true);
		
		List<Host> hosts = new ArrayList<>();
		hosts.add(host1);
		hosts.add(host2);
		hosts.add(host3);

		List<String> GKValues = Utils.getPossibleGKValues(hosts);
		
		Assert.assertEquals(0, GKValues.size());
	}
	
}
