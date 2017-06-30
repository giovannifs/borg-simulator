package org.cloudish.borg.model;

public class TaskConstraint {
	
	String attName;
	String operator;
	String attValue;
	
	public TaskConstraint(String attName, String op, String attValue) {
		this.attName = attName;
		this.operator = op;
		this.attValue = attValue;
	}

	public String getAttName() {
		return attName;
	}
	
	public String getOperator() {
		return operator;
	}
	
	public String getAttValue() {
		return attValue;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TaskConstraint) {
			TaskConstraint other = (TaskConstraint) obj;

			return getAttName().equals(other.getAttName()) && getOperator().equals(other.getOperator())
					&& getAttValue().equals(other.getAttValue());

		}
		return false;
	}
	
	@Override
	public String toString() {
		return getAttName() + getOperator()  + getAttValue();
	}
}
