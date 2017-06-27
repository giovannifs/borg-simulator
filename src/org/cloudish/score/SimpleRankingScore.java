package org.cloudish.score;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.Task;

public class SimpleRankingScore implements RankingScore {

	@Override
	public double calculateScore(Task task, Host host) {
		if (host == null) {
			throw new IllegalArgumentException("Host must not be null.");
		}
		return host.getFreeCPU() + host.getFreeMem();
	}

}
