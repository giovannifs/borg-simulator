package org.cloudish.score;

import org.cloudish.borg.model.Task;
import org.cloudish.dh.model.Server;

public class SimpleRankingScore implements RankingScore {

	@Override
	public double calculateScore(Task task, Server host) {
		if (host == null) {
			throw new IllegalArgumentException("Host must not be null.");
		}
		return host.getFreeCPU() + host.getFreeMem();
	}

}
