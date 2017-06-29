package org.cloudish.dh.model;

import java.util.HashMap;
import java.util.Map;

import org.cloudish.borg.model.ResourceAttribute;
import org.cloudish.borg.model.Task;

public class ResourcePool {

	public static final String CPU_TYPE = "cpu";
	public static final String MEMORY_TYPE = "memory";
	
	private String poolType;
	private double capacity;
	private double freeCapacity;
	private Map<String, ResourceAttribute> attributes = new HashMap<>();
	
	//	5,Host_5,0.5,0.2493,[5d,2;9e,2;By,4;GK,Ap;Ju,1;Ql,3;UX,2;ma,2;nU,2;nZ,2;rs,Fh;w3,4;wN,2;o/,0;P8,0]
	public ResourcePool(String line) {
		
	}

	public boolean isFeasible(Task task) {
		// TODO Auto-generated method stub
		return false;
	}

	public double getScore(Task task) {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean hasMoreResource(double requested) {
		// TODO Auto-generated method stub
		return false;
	}

	public double allocate(double resourceRequest) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
}
