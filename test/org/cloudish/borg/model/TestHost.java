package org.cloudish.borg.model;

import org.junit.Assert;
import org.junit.Test;

public class TestHost {

	private static final double ACCEPTABLE_DIFF = 0.0000000001;

	@Test
	public void testConstructor() {
		String hostLine = "5,Host_5,0.5,0.2493,[5d,2;9e,2;By,4;GK,Ap;Ju,1;Ql,3;UX,2;ma,2;nU,2;nZ,2;rs,Fh;w3,4;wN,2;o/,0;P8,0]";
		Host h = new Host(hostLine);

		// checking
		Assert.assertEquals(5, h.getId());
		Assert.assertEquals(0.5, h.getCpuCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.5, h.getFreeCPU(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.2493, h.getMemCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.2493, h.getFreeMem(), ACCEPTABLE_DIFF);

		Assert.assertNotNull(h.getAttributes());
		Assert.assertEquals(15, h.getAttributes().size());

		Assert.assertEquals("2", h.getAttributes().get("5d").getAttValue());
		Assert.assertEquals("2", h.getAttributes().get("9e").getAttValue());
		Assert.assertEquals("4", h.getAttributes().get("By").getAttValue());
		Assert.assertEquals("Ap", h.getAttributes().get("GK").getAttValue());
		Assert.assertEquals("1", h.getAttributes().get("Ju").getAttValue());
		Assert.assertEquals("3", h.getAttributes().get("Ql").getAttValue());
		Assert.assertEquals("2", h.getAttributes().get("UX").getAttValue());
		Assert.assertEquals("2", h.getAttributes().get("ma").getAttValue());
		Assert.assertEquals("2", h.getAttributes().get("nU").getAttValue());
		Assert.assertEquals("2", h.getAttributes().get("nZ").getAttValue());
		Assert.assertEquals("Fh", h.getAttributes().get("rs").getAttValue());
		Assert.assertEquals("4", h.getAttributes().get("w3").getAttValue());
		Assert.assertEquals("2", h.getAttributes().get("wN").getAttValue());
		Assert.assertEquals("0", h.getAttributes().get("o/").getAttValue());
		Assert.assertEquals("0", h.getAttributes().get("P8").getAttValue());

		Assert.assertNull(h.getAttributes().get("notexist"));
	}

	@Test
	public void testConstructorWithoutAttr() {
		String hostLine = "5,Host_5,0.5,0.2493,[]";
		Host h = new Host(hostLine);

		// checking
		Assert.assertEquals(5, h.getId());
		Assert.assertEquals(0.5, h.getCpuCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.5, h.getFreeCPU(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.2493, h.getMemCapacity(), ACCEPTABLE_DIFF);
		Assert.assertEquals(0.2493, h.getFreeMem(), ACCEPTABLE_DIFF);

		Assert.assertNotNull(h.getAttributes());
		Assert.assertEquals(0, h.getAttributes().size());

		Assert.assertNull(h.getAttributes().get("5d"));
		Assert.assertNull(h.getAttributes().get("notexist"));
	}

	@Test(expected = Exception.class)
	public void testConstructorInvalidLine() {
		// without host name
		String hostLine = "5,0.5,0.2493,[5d,2;9e,2;By,4;GK,Ap;Ju,1;Ql,3;UX,2;ma,2;nU,2;nZ,2;rs,Fh;w3,4;wN,2;o/,0;P8,0]";
		new Host(hostLine);
	}

	@Test(expected = Exception.class)
	public void testConstructorInvalidLine2() {
		// without host id
		String hostLine = "Host_5,0.5,0.2493,[5d,2;9e,2;By,4;GK,Ap;Ju,1;Ql,3;UX,2;ma,2;nU,2;nZ,2;rs,Fh;w3,4;wN,2;o/,0;P8,0]";
		new Host(hostLine);
	}

	@Test(expected = Exception.class)
	public void testConstructorInvalidLine3() {
		// without constraints
		String hostLine = "5,Host_5,0.5,0.2493";
		new Host(hostLine);
	}

	@Test(expected = Exception.class)
	public void testConstructorInvalidLine4() {
		// without cpuCapacity
		String hostLine = "5,Host_5,0.2493,[5d,2;9e,2;By,4;GK,Ap;Ju,1;Ql,3;UX,2;ma,2;nU,2;nZ,2;rs,Fh;w3,4;wN,2;o/,0;P8,0]";
		new Host(hostLine);
	}

	@Test(expected = Exception.class)
	public void testConstructorInvalidLine5() {
		// with empty cpuCapacity
		String hostLine = "5,Host_5,,0.2493,[5d,2;9e,2;By,4;GK,Ap;Ju,1;Ql,3;UX,2;ma,2;nU,2;nZ,2;rs,Fh;w3,4;wN,2;o/,0;P8,0]";
		new Host(hostLine);
	}

	@Test(expected = Exception.class)
	public void testConstructorInvalidLine6() {
		// with invalid cpuCapacity
		String hostLine = "5,Host_5,NA,0.2493,[5d,2;9e,2;By,4;GK,Ap;Ju,1;Ql,3;UX,2;ma,2;nU,2;nZ,2;rs,Fh;w3,4;wN,2;o/,0;P8,0]";
		new Host(hostLine);
	}

	@Test(expected = Exception.class)
	public void testConstructorInvalidLine7() {
		// without mem capacity
		String hostLine = "5,Host_5,0.5,[5d,2;9e,2;By,4;GK,Ap;Ju,1;Ql,3;UX,2;ma,2;nU,2;nZ,2;rs,Fh;w3,4;wN,2;o/,0;P8,0]";
		new Host(hostLine);
	}

	@Test(expected = Exception.class)
	public void testConstructorInvalidLine8() {
		// with empty mem capacity
		String hostLine = "5,Host_5,0.5,,[5d,2;9e,2;By,4;GK,Ap;Ju,1;Ql,3;UX,2;ma,2;nU,2;nZ,2;rs,Fh;w3,4;wN,2;o/,0;P8,0]";
		new Host(hostLine);
	}

	@Test(expected = Exception.class)
	public void testConstructorInvalidLine9() {
		// with invalid mem capacity
		String hostLine = "5,Host_5,0.5,NA,[5d,2;9e,2;By,4;GK,Ap;Ju,1;Ql,3;UX,2;ma,2;nU,2;nZ,2;rs,Fh;w3,4;wN,2;o/,0;P8,0]";
		new Host(hostLine);
	}

	@Test(expected = Exception.class)
	public void testConstructorInvalidLine10() {
		// with constraints in wrong format
		String hostLine = "5,Host_5,0.5,0.2493,[5d;2]";
		new Host(hostLine);
	}

	@Test(expected = Exception.class)
	public void testConstructorInvalidLine11() {
		// with constraints in wrong format
		String hostLine = "5,Host_5,0.5,0.2493,[5d=2]";
		new Host(hostLine);
	}

	public void testMatch() {
		String hostLine = "5,Host_5,0.5,0.2493,[5d,2;9e,2;By,4;GK,Ap;Ju,1;Ql,3;UX,2;ma,2;nU,2;nZ,2;rs,Fh;w3,4;wN,2;o/,0;P8,0]";
		Host h = new Host(hostLine);

		Assert.assertTrue(h.match(new Task("0,0,1,0.1,0.1,11,1,[5d,==,2]")));
		Assert.assertTrue(h.match(new Task("0,0,1,0.1,0.1,11,1,[5d,>,1]")));
		Assert.assertTrue(h.match(new Task("0,0,1,0.1,0.1,11,1,[5d,<,3]")));
		Assert.assertTrue(h.match(new Task("0,0,1,0.1,0.1,11,1,[5d,>,0;5d,<,3]")));

		Assert.assertTrue(h.match(new Task("0,0,1,0.1,0.1,11,1,[GK,!=,Ln;By,>,1;w3,<,9;w3,>,2]")));
		Assert.assertTrue(h.match(new Task("0,0,1,0.1,0.1,11,1,[nU,==,2;w3,<,9;w3,>,2;Ql,>,0;Ql,<,5]")));
		Assert.assertTrue(h.match(new Task("0,0,1,0.1,0.1,11,1,[Ql,<,14;wN,==,2;Ql,>,2;rs,!=,bf]")));
	}

	public void testNotMatch() {
		String hostLine = "5,Host_5,0.5,0.2493,[5d,2;9e,2;By,4;GK,Ap;Ju,1;Ql,3;UX,2;ma,2;nU,2;nZ,2;rs,Fh;w3,4;wN,2;o/,0;P8,0]";
		Host h = new Host(hostLine);

		Assert.assertTrue(h.match(new Task("0,0,1,0.1,0.1,11,1,[5d,!=,2]")));
		Assert.assertTrue(h.match(new Task("0,0,1,0.1,0.1,11,1,[5d,<,1]")));
		Assert.assertTrue(h.match(new Task("0,0,1,0.1,0.1,11,1,[5d,>,3]")));
		Assert.assertTrue(h.match(new Task("0,0,1,0.1,0.1,11,1,[5d,!=,0;5d,>,3]")));

		Assert.assertTrue(h.match(new Task("0,0,1,0.1,0.1,11,1,[GK,==,Ln;By,>,1;w3,<,9;w3,>,7]")));
		Assert.assertTrue(h.match(new Task("0,0,1,0.1,0.1,11,1,[nU,==,2;w3,<,9;w3,>,2;Ql,>,0;Ql,<,2]")));
		Assert.assertTrue(h.match(new Task("0,0,1,0.1,0.1,11,1,[Ql,<,14;wN,!=,2;Ql,>,2;rs,!=,bf]")));
	}
}
