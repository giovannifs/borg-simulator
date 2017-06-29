package org.cloudish.dh.model;

import java.util.ArrayList;
import java.util.List;

import org.cloudish.borg.model.Task;
import org.cloudish.score.RankingScore;

public abstract class Server {

	protected double cpuCapacity;
	protected double memCapacity;
	protected double freeCPU;
	protected double freeMem;
	protected List<Long> jidAllocated = new ArrayList<>();
	protected RankingScore rankingScore;
	
	public abstract double getScore(Task task);
	
	public abstract void allocate(Task task);
	
	public double getCpuCapacity() {
		return cpuCapacity;
	}

	public double getMemCapacity() {
		return memCapacity;
	}

	public double getFreeCPU() {
		return freeCPU;
	}

	public double getFreeMem() {
		return freeMem;
	}
	
	public RankingScore getRankingScore() {
		return rankingScore;
	}
}
