package org.cloudish.dh.model;

import org.cloudish.borg.model.Task;

public class LogicalServer {

	public LogicalServer(ResourcePool bestCpuPool, ResourcePool bestMemPool, double maxCPUCapacity,
			double maxMemCapacity, double resourceGrain) {
		// TODO Auto-generated constructor stub
	}

	public double getScore(Task task) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void allocate(Task task) {
		// TODO Auto-generated method stub
		
	}

	public boolean needsCPUScaleUp(Task task) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean needsMemScaleUp(Task task) {
		// TODO Auto-generated method stub
		return false;
	}

}
