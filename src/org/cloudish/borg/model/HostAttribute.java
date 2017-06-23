package org.cloudish.borg.model;

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
		switch (constraint.getOperator()) {
		case "!=":
			
			try {
				return Integer.valueOf(attValue) != Integer.valueOf(constraint.getAttValue());
			} catch (Exception e) {
				return !constraint.getAttValue().equals(attValue);
			}
			
		case "==":
			try {
				return Integer.parseInt(attValue) == Integer.parseInt(constraint.getAttValue());
			} catch (Exception e) {
				return constraint.getAttValue().equals(attValue);
			}
			
		case "<":
			try {
				return Integer.valueOf(attValue) < Integer.valueOf(constraint.getAttValue());
			} catch (Exception e) {
				return constraint.getAttValue().compareTo(attValue) == -1;
			}
			
		case ">":
			try {
				return Integer.valueOf(attValue) > Integer.valueOf(constraint.getAttValue());
			} catch (Exception e) {
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
