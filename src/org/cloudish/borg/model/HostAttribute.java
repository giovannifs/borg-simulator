package org.cloudish.borg.model;

public class HostAttribute {

	String attName;
	String attValue;
	boolean isNumericAttr;
	
	public HostAttribute(String attName2, String attValue2) {
		// TODO Auto-generated constructor stub
	}

	public boolean match(TaskConstraint constraint) {
		switch (constraint.operator) {
		case "/=":
			if (isNumericAttr) {
				return Double.valueOf(attValue) != Double.valueOf(constraint.getAttValue());
			} else {
				return !constraint.getAttValue().equals(attValue);
			}
			
		case "==":
			if (isNumericAttr) {
				return Double.valueOf(attValue) == Double.valueOf(constraint.getAttValue());
			} else {
				return constraint.getAttValue().equals(attValue);
			}
			
		case "<":
			if (isNumericAttr) {
				return Double.valueOf(attValue) < Double.valueOf(constraint.getAttValue());
			} else {
				return constraint.getAttValue().compareTo(attValue) == -1;
			}
			
		case ">":
			if (isNumericAttr) {
				return Double.valueOf(attValue) > Double.valueOf(constraint.getAttValue());
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
