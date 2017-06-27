package org.cloudish.borg.model;

public class ResourceAttribute {

	private String attName;
	private String attValue;
	
	public ResourceAttribute(String attName, String attValue) {
		this.attName = attName;
		this.attValue = attValue;
	}

	public boolean match(TaskConstraint constraint) {
		if (!attName.equals(constraint.getAttName())) {
			return false;
		}
		
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
				return attValue.compareTo(constraint.getAttValue()) == -1;
			}
			
		case ">":
			try {
				return Integer.valueOf(attValue) > Integer.valueOf(constraint.getAttValue());
			} catch (Exception e) {
				return attValue.compareTo(constraint.getAttValue()) == 1;
			}
			
		default:
			throw new IllegalArgumentException("Constraint with invalid operator.");
		} 
	}

	public String getAttName() {
		return attName;
	}

	public String getAttValue() {
		return attValue;
	}
}
