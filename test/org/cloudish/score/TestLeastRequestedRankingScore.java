package org.cloudish.score;

import org.junit.Test;

public class TestLeastRequestedRankingScore {

	@Test(expected=IllegalArgumentException.class)
	public void testCalculateScoreInvalidHost() {
		LeastRequestedRankingScore score = new LeastRequestedRankingScore();

		score.calculateScore(null, null);
	}

}
