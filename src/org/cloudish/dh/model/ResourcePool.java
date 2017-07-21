package org.cloudish.dh.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.ResourceAttribute;
import org.cloudish.borg.model.Task;
import org.cloudish.borg.model.TaskConstraint;
import org.cloudish.dh.Utils;

public class ResourcePool {

	public static final String CPU_TYPE = "cpu";
	public static final String MEMORY_TYPE = "memory";

	private String poolType;
	private String id;
	private double capacity;
	private double freeCapacity;
	private Map<String, ResourceAttribute> attributes = new HashMap<>();
	private boolean isConstraintOn;
	
	@SuppressWarnings("serial")
	public static final List<String> CPU_ATTRIBUTES = new ArrayList<String>() {{
		add("9e");
		add("By");
		add("nZ");
		add("St");
		add("o/");
		add("P8");
		add("wN");
		add("rs");
		add("w2");
		add("w5");
	}};
	
	// 5,Host_5,0.5,0.2493,[5d,2;9e,2;By,4;GK,Ap;Ju,1;Ql,3;UX,2;ma,2;nU,2;nZ,2;rs,Fh;w3,4;wN,2;o/,0;P8,0]
	public ResourcePool(String poolType, Map<String, ResourceAttribute> attributes) {
		this(poolType, 0, attributes, true);
	}
	
	public ResourcePool(String poolType, Map<String, ResourceAttribute> attributes, boolean isConstraintOn) {
		this(poolType, 0, attributes, isConstraintOn);
	}

	public ResourcePool(String poolType, double capacity, Map<String, ResourceAttribute> attributes, boolean isConstraintOn) {
		this.poolType = poolType;
		this.capacity = capacity;
		this.freeCapacity = capacity;

		this.attributes = attributes;
		this.isConstraintOn = isConstraintOn;
		
		if (poolType.equals(CPU_TYPE)) {
			String cpuAttr = "";
			for (ResourceAttribute attr : attributes.values()) {
				cpuAttr += attr.toString() + ";"; 
			}
			if (attributes.isEmpty()) {
				this.id = "cpu-pool:[]";
			} else {
				this.id = "cpu-pool:[" + cpuAttr.substring(0, cpuAttr.length() -1) + "]";
			}
		} else {
			this.id = "mem-pool:[]";
		}
	}

	public boolean isFeasible(Task task) {
		if (task == null) {
			return true;
		}
		
		if (isConstraintOn) {
			// constraints are related only with CPU pools
			if (poolType.equals(CPU_TYPE)) {
				for (TaskConstraint constraint : task.getConstraints()) {
					
					/*
					 * Ignoring GK and Ql attribute, these are treated by logical server
					 */
					if (!"GK".equals(constraint.getAttName()) && !"Ql".equals(constraint.getAttName())) {
						ResourceAttribute resourceAtt = attributes.get(constraint.getAttName());
						
						if (resourceAtt == null || !resourceAtt.match(constraint)) {
							return false;
						}
					}
				}			
			}
		}

		return true;
	}

	public double getScore() {
		return freeCapacity;
	}

	public boolean hasMoreResource(double requested) {
		return freeCapacity >= requested;
	}

	public void allocate(double resourceRequest) {
		freeCapacity = Utils.format(freeCapacity - resourceRequest);

		System.out.println(getId() + " - freeCapacity?" + freeCapacity);
		
		if (freeCapacity < 0) {
			throw new RuntimeException(
					"The resource pool allocated more than it could. Free capacity is lower than zero.");
		}
	}

	public String getPoolType() {
		return poolType;
	}

	public double getCapacity() {
		return capacity;
	}

	public double getFreeCapacity() {
		return freeCapacity;
	}
	
	public boolean isConstraintOn() {
		return isConstraintOn;
	}

	public Map<String, ResourceAttribute> getAttributes() {
		return attributes;
	}
	
	public String getId() {
		return id;
	}

	public void incorporateHost(Host host) {
		if (poolType.equals(MEMORY_TYPE)) {
			capacity += host.getMemCapacity();
			freeCapacity += host.getMemCapacity();
		} else if (poolType.equals(CPU_TYPE)){
			capacity += host.getCpuCapacity();
			freeCapacity += host.getCpuCapacity();
		} else {
			throw new RuntimeException("The resource pool has a unkown pool type.");
		}
	}

	public boolean match(Host host) {
		if (poolType.equals(MEMORY_TYPE)) {
			return true;
		} else if (poolType.equals(CPU_TYPE)){
			
			for (String cpuAttribute : CPU_ATTRIBUTES) {
				// check if attribute is configured only in one of them
				if (host.getAttributes().get(cpuAttribute) == null && getAttributes().get(cpuAttribute) != null) {
					return false;
				} else if (host.getAttributes().get(cpuAttribute) != null
						&& getAttributes().get(cpuAttribute) == null) {
					return false;
				
				// check if attribute is configured in both and has different values
				} else if (host.getAttributes().get(cpuAttribute) != null
						&& getAttributes().get(cpuAttribute) != null) {

					if (!host.getAttributes().get(cpuAttribute).equals(getAttributes().get(cpuAttribute))) {
						return false;
					}
				}
			}
			
			return true;
		} else {
			throw new RuntimeException("The resource pool has a unkown pool type.");
		}
	}

	public boolean match(Map<String, ResourceAttribute> attributes) {
		if (poolType.equals(MEMORY_TYPE)) {
			return true;
		} else if (poolType.equals(CPU_TYPE)) {

			for (String cpuAttribute : CPU_ATTRIBUTES) {
				if (attributes.get(cpuAttribute) == null && getAttributes().get(cpuAttribute) != null) {
					return false;
				} else if (attributes.get(cpuAttribute) != null && getAttributes().get(cpuAttribute) == null) {
					return false;

					// check if attribute is configured in both and has
					// different values
				} else if (attributes.get(cpuAttribute) != null && getAttributes().get(cpuAttribute) != null) {

					if (!attributes.get(cpuAttribute).equals(getAttributes().get(cpuAttribute))) {
						return false;
					}
				}
			}
			return true;
		} else {
			throw new RuntimeException("The resource pool has a unkown pool type.");
		}
	}
}
