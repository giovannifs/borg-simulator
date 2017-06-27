package org.cloudish.score;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.Task;

public interface RankingScore {
	
	public double calculateScore(Task task, Host host);

}
