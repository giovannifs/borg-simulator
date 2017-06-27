package org.cloudish.score;

import java.util.ArrayList;
import java.util.HashMap;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.Task;
import org.junit.Assert;
import org.junit.Test;

public class TestSimpleRankingScore {

	private static final double ACCEPTABLE_DIFF = 0.0000001;

	@Test(expected=IllegalArgumentException.class)
	public void testCalculateScoreInvalidHost() {
		SimpleRankingScore score = new SimpleRankingScore();

		score.calculateScore(null, null);
	}

	@Test
	public void testCalculateScore() {
		SimpleRankingScore score = new SimpleRankingScore();

		Host h = new Host(0, 0.4, 0.5, new HashMap<>());
		Assert.assertEquals(0.9, score.calculateScore(null, h), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testCalculateScore2() {
		SimpleRankingScore score = new SimpleRankingScore();

		Host h = new Host(0, 0.4, 0.5, new HashMap<>());
		Task t = new Task(0, 10, 0.2, 0.2, 11, true, new ArrayList<>());
		h.allocate(t);
		
		Assert.assertEquals(0.5, score.calculateScore(null, h), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testCalculateScore3() {
		SimpleRankingScore score = new SimpleRankingScore();

		Host h = new Host(0, 1, 0.1, new HashMap<>());
		Task t = new Task(0, 10, 0.2, 0.1, 11, true, new ArrayList<>());
		h.allocate(t);
		
		Assert.assertEquals(0.8, score.calculateScore(null, h), ACCEPTABLE_DIFF);
	}
}
