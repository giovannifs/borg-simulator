package org.cloudish.score;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.Task;

public class LeastRequestedRankingScore implements RankingScore {

	/*
	 * cpu((capacity - sum(requested)) * 10 / capacity) + memory((capacity -
	 * sum(requested)) * 10 / capacity) / 2(non-Javadoc)
	 * 
	 */
	
	@Override
	public double calculateScore(Task task, Host host) {
		if (host == null) {
			throw new IllegalArgumentException("Host must not be null.");
		}
		return (host.getFreeCPU() * 10 / host.getCpuCapacity() + host.getFreeMem() * 10 / host.getMemCapacity())/2;
	}
}
