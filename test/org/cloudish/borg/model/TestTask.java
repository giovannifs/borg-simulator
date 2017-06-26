package org.cloudish.borg.model;

import org.junit.Assert;
import org.junit.Test;

public class TestTask {

	private static final double ACCEPTABLE_DIFF = 0.000000000001;

	@Test
	public void testConstructor() {
		String taskLine = "0,0,4028922835,0.09375,0.03198,11,1,[Ql,<,14;wN,==,2;Ql,>,4]";
		Task t = new Task(taskLine);

		// checking
		Assert.assertEquals(0, t.getTid());
		Assert.assertEquals(4028922835l, t.getJid());
		Assert.assertEquals(0.09375, t.getCpuReq(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.03198, t.getMemReq(), ACCEPTABLE_DIFF);
		Assert.assertEquals(11, t.getPriority());
		Assert.assertTrue(t.isAntiAffinity());

		Assert.assertNotNull(t.getConstraints());
		Assert.assertEquals(3, t.getConstraints().size());

		Assert.assertTrue(t.getConstraints().contains(new TaskConstraint("Ql", "<", "14")));
		Assert.assertTrue(t.getConstraints().contains(new TaskConstraint("wN", "==", "2")));
		Assert.assertTrue(t.getConstraints().contains(new TaskConstraint("Ql", ">", "4")));
	}

	@Test
	public void testConstructor2() {
		String taskLine = "0,0,4028922835,0.09375,0.03198,11,1,[]";
		Task t = new Task(taskLine);

		// checking
		Assert.assertEquals(0, t.getTid());
		Assert.assertEquals(4028922835l, t.getJid());
		Assert.assertEquals(0.09375, t.getCpuReq(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.03198, t.getMemReq(), ACCEPTABLE_DIFF);
		Assert.assertEquals(11, t.getPriority());
		Assert.assertTrue(t.isAntiAffinity());

		Assert.assertNotNull(t.getConstraints());
		Assert.assertEquals(0, t.getConstraints().size());

		Assert.assertFalse(t.getConstraints().contains(new TaskConstraint("Ql", "<", "14")));
	}

	@Test(expected = Exception.class)
	public void testInvalidLine() {
		// without time
		String taskLine = "0,4028922835,0.09375,0.03198,11,1,[Ql,<,14;wN,==,2;Ql,>,4]";
		new Task(taskLine);
	}

	@Test(expected = Exception.class)
	public void testInvalidLine2() {
		// without tid
		String taskLine = "0,4028922835,0.09375,0.03198,11,1,[Ql,<,14;wN,==,2;Ql,>,4]";
		new Task(taskLine);
	}

	@Test(expected = Exception.class)
	public void testInvalidLine3() {
		// with invalid tid
		String taskLine = "0,NA,4028922835,0.09375,0.03198,11,1,[Ql,<,14;wN,==,2;Ql,>,4]";
		new Task(taskLine);
	}

	@Test(expected = Exception.class)
	public void testInvalidLine4() {
		// with invalid tid
		String taskLine = "0,-1,4028922835,0.09375,0.03198,11,1,[Ql,<,14;wN,==,2;Ql,>,4]";
		new Task(taskLine);
	}

	@Test(expected = Exception.class)
	public void testInvalidLine5() {
		// without jid
		String taskLine = "0,0,,0.09375,0.03198,11,1,[Ql,<,14;wN,==,2;Ql,>,4]";
		new Task(taskLine);
	}

	@Test(expected = Exception.class)
	public void testInvalidLine6() {
		// with invalid jid
		String taskLine = "0,0,-4,0.09375,0.03198,11,1,[Ql,<,14;wN,==,2;Ql,>,4]";
		new Task(taskLine);
	}

	@Test(expected = Exception.class)
	public void testInvalidLine7() {
		// with invalid jid
		String taskLine = "0,0,NA,0.09375,0.03198,11,1,[Ql,<,14;wN,==,2;Ql,>,4]";
		new Task(taskLine);
	}

	@Test(expected = Exception.class)
	public void testInvalidLine8() {
		// without cpu req
		String taskLine = "0,0,4028922835,0.03198,11,1,[Ql,<,14;wN,==,2;Ql,>,4]";
		new Task(taskLine);
	}

	@Test(expected = Exception.class)
	public void testInvalidLine9() {
		// with invalid cpu req
		String taskLine = "0,0,4028922835,NA,0.03198,11,1,[Ql,<,14;wN,==,2;Ql,>,4]";
		new Task(taskLine);
	}

	@Test(expected = Exception.class)
	public void testInvalidLine10() {
		// without mem req
		String taskLine = "0,0,4028922835,0.09375,,11,1,[Ql,<,14;wN,==,2;Ql,>,4]";
		new Task(taskLine);
	}

	@Test(expected = Exception.class)
	public void testInvalidLine11() {
		// with invalid mem req
		String taskLine = "0,0,4028922835,0.09375,-8,11,1,[Ql,<,14;wN,==,2;Ql,>,4]";
		new Task(taskLine);
	}

	@Test(expected = Exception.class)
	public void testInvalidLine12() {
		// without priority
		String taskLine = "0,0,4028922835,0.09375,0.03198,1,[Ql,<,14;wN,==,2;Ql,>,4]";
		new Task(taskLine);
	}

	@Test(expected = Exception.class)
	public void testInvalidLine13() {
		// with invalid priority
		String taskLine = "0,0,4028922835,0.09375,0.03198,-2,1,[Ql,<,14;wN,==,2;Ql,>,4]";
		new Task(taskLine);
	}

	@Test(expected = Exception.class)
	public void testInvalidLine14() {
		// without anti affinity
		String taskLine = "0,0,4028922835,0.09375,0.03198,11,[Ql,<,14;wN,==,2;Ql,>,4]";
		new Task(taskLine);
	}

	@Test(expected = Exception.class)
	public void testInvalidLine15() {
		// with invalid anti affinity
		String taskLine = "0,0,4028922835,0.09375,0.03198,11,NA,[Ql,<,14;wN,==,2;Ql,>,4]";
		new Task(taskLine);
	}

	@Test(expected = Exception.class)
	public void testInvalidLine16() {
		// without constraints
		String taskLine = "0,0,4028922835,0.09375,0.03198,11,1,";
		new Task(taskLine);
	}

	@Test(expected = Exception.class)
	public void testInvalidLine17() {
		// with invalid constraints
		String taskLine = "0,0,4028922835,0.09375,0.03198,11,1,[Ql<14]";
		new Task(taskLine);
	}

}
