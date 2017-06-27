package org.cloudish.borg.model;

import org.junit.Assert;
import org.junit.Test;

public class TestResourceAttribute {

	@Test
	public void testMatch() {		
		ResourceAttribute hAttr = new ResourceAttribute("attName", "4");
		
		Assert.assertTrue(hAttr.match(new TaskConstraint("attName", "==", "4")));
		Assert.assertTrue(hAttr.match(new TaskConstraint("attName", ">", "3")));
		Assert.assertTrue(hAttr.match(new TaskConstraint("attName", "<", "5")));
		Assert.assertTrue(hAttr.match(new TaskConstraint("attName", "!=", "3")));
		
		Assert.assertFalse(hAttr.match(new TaskConstraint("otherAttr", "==", "4")));
		Assert.assertFalse(hAttr.match(new TaskConstraint("otherAttr", ">", "3")));
		Assert.assertFalse(hAttr.match(new TaskConstraint("otherAttr", "<", "5")));
		Assert.assertFalse(hAttr.match(new TaskConstraint("otherAttr", "!=", "5")));
		
		Assert.assertFalse(hAttr.match(new TaskConstraint("attName", "==", "5")));
		Assert.assertFalse(hAttr.match(new TaskConstraint("attName", ">", "4")));
		Assert.assertFalse(hAttr.match(new TaskConstraint("attName", "<", "4")));
		Assert.assertFalse(hAttr.match(new TaskConstraint("attName", "!=", "4")));
	}
	
	@Test
	public void testMatch2() {		
		ResourceAttribute hAttr = new ResourceAttribute("attName", "bf");
				
		Assert.assertTrue(hAttr.match(new TaskConstraint("attName", "==", "bf")));
		Assert.assertTrue(hAttr.match(new TaskConstraint("attName", ">", "af")));
		Assert.assertTrue(hAttr.match(new TaskConstraint("attName", "<", "cf")));
		Assert.assertTrue(hAttr.match(new TaskConstraint("attName", "!=", "ff")));
		
		Assert.assertFalse(hAttr.match(new TaskConstraint("otherAttr", "==", "bf")));
		Assert.assertFalse(hAttr.match(new TaskConstraint("otherAttr", ">", "af")));
		Assert.assertFalse(hAttr.match(new TaskConstraint("otherAttr", "<", "cf")));
		Assert.assertFalse(hAttr.match(new TaskConstraint("otherAttr", "!=", "ff")));
		
		Assert.assertFalse(hAttr.match(new TaskConstraint("attName", "==", "aa")));
		Assert.assertFalse(hAttr.match(new TaskConstraint("attName", ">", "cf")));
		Assert.assertFalse(hAttr.match(new TaskConstraint("attName", "<", "af")));
		Assert.assertFalse(hAttr.match(new TaskConstraint("attName", "!=", "bf")));
	}

	
	@Test(expected=IllegalArgumentException.class)
	public void testMatchInvalidOp() {		
		ResourceAttribute hAttr = new ResourceAttribute("attName", "4");
		
		Assert.assertTrue(hAttr.match(new TaskConstraint("attName", ">=", "4")));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testMatchInvalidOp2() {		
		ResourceAttribute hAttr = new ResourceAttribute("attName", "4");
		
		Assert.assertTrue(hAttr.match(new TaskConstraint("attName", "<=", "4")));
	}

}
