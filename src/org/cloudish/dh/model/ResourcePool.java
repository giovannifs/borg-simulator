package org.cloudish.dh.model;

import java.util.HashMap;
import java.util.Map;

import org.cloudish.borg.model.ResourceAttribute;

public class ResourcePool {

	private double capacity;
	private double freeCapacity;
	private Map<String, ResourceAttribute> attributes = new HashMap<>();
	
	//	5,Host_5,0.5,0.2493,[5d,2;9e,2;By,4;GK,Ap;Ju,1;Ql,3;UX,2;ma,2;nU,2;nZ,2;rs,Fh;w3,4;wN,2;o/,0;P8,0]
	public ResourcePool(String line) {
		
	}
	
	
}
