package org.cloudish.dh.model;

import java.util.HashMap;
import java.util.Map;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.ResourceAttribute;
import org.cloudish.borg.model.Task;
import org.cloudish.borg.model.TaskConstraint;

public class ResourcePool {

	public static final String CPU_TYPE = "cpu";
	public static final String MEMORY_TYPE = "memory";

	private String poolType;
	private double capacity;
	private double freeCapacity;
	private Map<String, ResourceAttribute> attributes = new HashMap<>();

	// 5,Host_5,0.5,0.2493,[5d,2;9e,2;By,4;GK,Ap;Ju,1;Ql,3;UX,2;ma,2;nU,2;nZ,2;rs,Fh;w3,4;wN,2;o/,0;P8,0]
	public ResourcePool(String poolType, Map<String, ResourceAttribute> attributes) {
		this(poolType, 0, attributes);
	}

	public ResourcePool(String poolType, double capacity, Map<String, ResourceAttribute> attributes) {
		this.poolType = poolType;
		this.capacity = capacity;
		this.freeCapacity = capacity;

		// TODO verify if this is cpupool and filter the interested attributes
		// the attributes can be configured
		this.attributes = attributes;
	}

	public boolean isFeasible(Task task) {
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

		if (CPU_TYPE.equals(getPoolType())) {
			if (freeCapacity >= task.getCpuReq()) {
				return true;
			}
		} else if (MEMORY_TYPE.equals(getPoolType())) {
			if (freeCapacity >= task.getMemReq()) {
				return true;
			}
		} else {
			throw new RuntimeException("The resource pool is not of a known type.");
		}

		return false;
	}

	public double getScore() {
		return freeCapacity;
	}

	public boolean hasMoreResource(double requested) {
		return freeCapacity >= requested;
	}

	public void allocate(double resourceRequest) {
		freeCapacity = freeCapacity - resourceRequest;

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

	public Map<String, ResourceAttribute> getAttributes() {
		return attributes;
	}

	public void incorporateHost(Host host) {
		// TODO Auto-generated method stub

	}

	public boolean match(Host host) {
		// TODO Auto-generated method stub
		return false;
	}
}
