package org.cloudish.dh;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.ResourceAttribute;
import org.cloudish.dh.model.ResourcePool;

public class Utils {

	public static Map<String, List<ResourcePool>> createResourcePoolsFromHosts(List<Host> hosts, boolean isConstraintOn) {
		Map<String, List<ResourcePool>> pools = new HashMap<>();
		
		pools.put(ResourcePool.CPU_TYPE, new ArrayList<>());
		pools.put(ResourcePool.MEMORY_TYPE, new ArrayList<>());
		
		ResourcePool memPool = new ResourcePool(ResourcePool.MEMORY_TYPE, new HashMap<>(), isConstraintOn);
		
		for (Host host : hosts) {
			memPool.incorporateHost(host);
			
			boolean hostIncorporated = false;
			List<ResourcePool> cpuPools = pools.get(ResourcePool.CPU_TYPE);
			for (ResourcePool cpuPool : cpuPools) {
				if (cpuPool.match(host)) {
					hostIncorporated = true;
					cpuPool.incorporateHost(host);
					break;
				}
			}
			
			if (!hostIncorporated) {
				Map<String, ResourceAttribute> resourceAttributes = filterCpuAttributes(host.getAttributes());
				ResourcePool cpuPool = new ResourcePool(ResourcePool.CPU_TYPE, resourceAttributes, isConstraintOn);
				cpuPool.incorporateHost(host);
				pools.get(ResourcePool.CPU_TYPE).add(cpuPool);
			}
		}
		
		// adding memory pools (only one)
		pools.get(ResourcePool.MEMORY_TYPE).add(memPool);
		
		return pools;
	}

	protected static Map<String, ResourceAttribute> filterCpuAttributes(Map<String, ResourceAttribute> attributes) {
		Map<String, ResourceAttribute> cpuAttributes = new HashMap<>();
		
		for (String cpuAttrName : ResourcePool.CPU_ATTRIBUTES) {
			if (attributes.containsKey(cpuAttrName)) {
				cpuAttributes.put(cpuAttrName, attributes.get(cpuAttrName));
			}
		}
		return cpuAttributes;
	}

	public static List<String> getPossibleGKValues(List<Host> hosts) {
		List<String> GKValues = new ArrayList<>();

		for (Host host : hosts) {
			if (host.getAttributes().containsKey("GK") && host.getAttributes().get("GK") != null) {
				GKValues.add(host.getAttributes().get("GK").getAttValue());
			}
		}

		return GKValues;
	}
	
	static {Locale.setDefault(Locale.ROOT);}

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##########");

    public static double format(double value){

        String truncatedValue = DECIMAL_FORMAT.format(value);
        double doubleValue = Double.parseDouble(truncatedValue);

        return doubleValue;
    }

	public static int getInitialNumberOfServers(ResourcePool pool, double factor) {
		return (int) Math.ceil(pool.getCapacity() / factor);
	}
}
