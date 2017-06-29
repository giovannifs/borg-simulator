package org.cloudish.dh.model;

import java.util.ArrayList;
import java.util.List;

import org.cloudish.borg.model.Task;
import org.cloudish.score.RankingScore;

public class LogicalServer extends Server {

	private double maxCpuCapacity;
	private double maxMemCapacity;
	private double resourceGrain;
	private ResourcePool cpuPool;
	private ResourcePool memPool;

	public LogicalServer(ResourcePool cpuPool, ResourcePool memPool, double maxCPUCapacity,
			double maxMemCapacity, double resourceGrain) {
		// TODO Auto-generated constructor stub
	}

	public double getScore(Task task) {
		if (!isFeasible(task)) {
			return -1;
		}

		// TODO calculate score needs to consider the logical server scaled up
		return rankingScore.calculateScore(task, this);
	}

	private boolean isFeasible(Task task) {
		// check if the resource pools are feasible for task
		if (!cpuPool.isFeasible(task) || !memPool.isFeasible(task)) {
			return false;
		}
		
		// check if logical server needs to be scaled up
		if (needsCPUScaleUp(task)) {
			double cpuToBeRequested = calcCpuToBeRequested(task);
			
			if (!cpuPool.hasMoreResource(cpuToBeRequested) || exceedCpuMaxCapacity(cpuToBeRequested)) {
				return false;
			}
		}
		
		if (needsMemScaleUp(task)) {
			double memToBeRequested = calcMemToBeRequested(task);
			
			if (!memPool.hasMoreResource(memToBeRequested) || exceedMemMaxCapacity(memToBeRequested)) {
				return false;
			} 
		}

		return true;
	}

	private double calcCpuToBeRequested(Task task) {
		double cpuToBeScaled = task.getCpuReq() - freeCPU;
		int numberOfGrains = (int) Math.round(cpuToBeScaled/getResourceGrain() + 0.5d); 
		
		double cpuToBeRequested = numberOfGrains * getResourceGrain();
		return cpuToBeRequested;
	}

	private double calcMemToBeRequested(Task task) {
		double memToBeScaled = task.getMemReq() - freeMem;
		int numberOfGrains = (int) Math.round(memToBeScaled/getResourceGrain() + 0.5d); 
		
		double memToBeRequested = numberOfGrains * getResourceGrain();
		return memToBeRequested;
	}

	private boolean exceedCpuMaxCapacity(double cpuToBeRequested) {
		return (cpuCapacity + cpuToBeRequested) > maxCpuCapacity;
	}

	private boolean exceedMemMaxCapacity(double memToBeRequested) {
		return (memCapacity + memToBeRequested) > maxMemCapacity;
	}

	public void allocate(Task task) {
		
		// scaling up if needed
		if (needsCPUScaleUp(task)) {			
			double cpuToBeRequested = calcCpuToBeRequested(task);
			getCpuPool().allocate(cpuToBeRequested);
			
			cpuCapacity += cpuCapacity + cpuToBeRequested;
			freeCPU +=  cpuToBeRequested;
		}
		
		if (needsMemScaleUp(task)) {
			double memToBeRequested = calcCpuToBeRequested(task);
			
			getMemPool().allocate(memToBeRequested);
			
			memCapacity += memToBeRequested;
			freeMem += memToBeRequested;
		}
		
		// allocating the task
		freeCPU = freeCPU - task.getCpuReq();
		freeMem = freeMem - task.getMemReq();
		
		if (freeCPU < 0 || freeMem < 0) {
			throw new RuntimeException("The free memory or cpu never must be a negative value.");
		}

		if (task.isAntiAffinity()) {
			jidAllocated.add(task.getJid());		
		}
	}

	public boolean needsCPUScaleUp(Task task) {
		return freeCPU < task.getCpuReq();
	}

	public boolean needsMemScaleUp(Task task) {
		return freeMem < task.getMemReq();
	}

	public double getMaxCpuCapacity() {
		return maxCpuCapacity;
	}

	public double getMaxMemCapacity() {
		return maxMemCapacity;
	}

	public double getResourceGrain() {
		return resourceGrain;
	}

	public ResourcePool getCpuPool() {
		return cpuPool;
	}

	public ResourcePool getMemPool() {
		return memPool;
	}
}
