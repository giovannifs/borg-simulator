package org.cloudish.borg.model;

import com.sun.java_cup.internal.runtime.Symbol;

public class HostAttribute {

	String attName;
	String attValue;
	boolean isNumericAttr;
	
	public HostAttribute(String attName, String attValue) {
		this.attName = attName;
		this.attValue = attValue;
		try {
			Double.parseDouble(attValue);
			isNumericAttr = true;
		} catch (Exception e) {
			isNumericAttr = true; 
		}
	}

	public boolean match(TaskConstraint constraint) {
		System.out.println("DENTRO DO MATCH HOSTATTRIBUTE");
		System.out.println(constraint.getAttName() + "," + constraint.getOperator());
		
		switch (constraint.getOperator()) {
		case "!=":
			
			if (isNumericAttr) {
				return Integer.valueOf(attValue) != Integer.valueOf(constraint.getAttValue());
			} else {
				return !constraint.getAttValue().equals(attValue);
			}
			
		case "==":
			if (isNumericAttr) {
				return Integer.parseInt(attValue) == Integer.parseInt(constraint.getAttValue());
			} else {
				return constraint.getAttValue().equals(attValue);
			}
			
		case "<":
			if (isNumericAttr) {
				return Integer.valueOf(attValue) < Integer.valueOf(constraint.getAttValue());
			} else {
				return constraint.getAttValue().compareTo(attValue) == -1;
			}
			
		case ">":
			if (isNumericAttr) {
				return Integer.valueOf(attValue) > Integer.valueOf(constraint.getAttValue());
			} else {
				return constraint.getAttValue().compareTo(attValue) == 1;
			}
			
		default:
			System.err.println("Constraint with invalid operator.");
			System.exit(1);
			break;
		} 
		
		return false;
	}

}
