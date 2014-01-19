/**
 *  Catroid: An on-device visual programming system for Android devices
 *  Copyright (C) 2010-2013 The Catrobat Team
 *  (<http://developer.catrobat.org/credits>)
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *  
 *  An additional term exception under section 7 of the GNU Affero
 *  General Public License, version 3, is available at
 *  http://developer.catrobat.org/license_additional_term
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.formulaeditor;

import android.util.Log;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Sprite;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class FormulaElement implements Serializable {

	private static final long serialVersionUID = 1L;

	public static enum ElementType {
		OPERATOR, FUNCTION, NUMBER, SENSOR, USER_VARIABLE, BRACKET, STRING
	}

	public static final Double NOT_EXISTING_USER_VARIABLE_INTERPRETATION_VALUE = 0d;

	private ElementType type;
	private String value;
	private FormulaElement leftChild = null;
	private FormulaElement rightChild = null;
	private transient FormulaElement parent = null;

	public FormulaElement(ElementType type, String value, FormulaElement parent) {
		this.type = type;
		this.value = value;
		this.parent = parent;
	}

	public FormulaElement(ElementType type, String value, FormulaElement parent, FormulaElement leftChild,
			FormulaElement rightChild) {
		this.type = type;
		this.value = value;
		this.parent = parent;
		this.leftChild = leftChild;
		this.rightChild = rightChild;

		if (leftChild != null) {
			this.leftChild.parent = this;
		}
		if (rightChild != null) {
			this.rightChild.parent = this;
		}

	}

	public ElementType getElementType() {
		return type;
	}

	public String getValue() {
		return value;
	}

	public List<InternToken> getInternTokenList() {
		List<InternToken> internTokenList = new LinkedList<InternToken>();

		switch (type) {
			case BRACKET:
				internTokenList.add(new InternToken(InternTokenType.BRACKET_OPEN));
				if (rightChild != null) {
					internTokenList.addAll(rightChild.getInternTokenList());
				}
				internTokenList.add(new InternToken(InternTokenType.BRACKET_CLOSE));
				break;
			case OPERATOR:
				if (leftChild != null) {
					internTokenList.addAll(leftChild.getInternTokenList());
				}
				internTokenList.add(new InternToken(InternTokenType.OPERATOR, this.value));
				if (rightChild != null) {
					internTokenList.addAll(rightChild.getInternTokenList());
				}
				break;
			case FUNCTION:
				internTokenList.add(new InternToken(InternTokenType.FUNCTION_NAME, value));
				boolean functionHasParameters = false;
				if (leftChild != null) {
					internTokenList.add(new InternToken(InternTokenType.FUNCTION_PARAMETERS_BRACKET_OPEN));
					functionHasParameters = true;
					internTokenList.addAll(leftChild.getInternTokenList());
				}
				if (rightChild != null) {
					internTokenList.add(new InternToken(InternTokenType.FUNCTION_PARAMETER_DELIMITER));
					internTokenList.addAll(rightChild.getInternTokenList());
				}
				if (functionHasParameters) {
					internTokenList.add(new InternToken(InternTokenType.FUNCTION_PARAMETERS_BRACKET_CLOSE));
				}
				break;
			case USER_VARIABLE:
				internTokenList.add(new InternToken(InternTokenType.USER_VARIABLE, this.value));
				break;
			case NUMBER:
				internTokenList.add(new InternToken(InternTokenType.NUMBER, this.value));
				break;
			case SENSOR:
				internTokenList.add(new InternToken(InternTokenType.SENSOR, this.value));
				break;
			case STRING:
				internTokenList.add(new InternToken(InternTokenType.STRING, value));
				break;
		}
		return internTokenList;
	}

	public FormulaElement getRoot() {
		FormulaElement root = this;
		while (root.getParent() != null) {
			root = root.getParent();
		}
		return root;
	}

	public Object interpretRecursive(Sprite sprite) throws NumberFormatException {

		Object returnValue = 0d;

		switch (type) {
			case BRACKET:
				returnValue = rightChild.interpretRecursive(sprite);
				break;
			case NUMBER:
				returnValue = Double.parseDouble(value);
				break;
			case OPERATOR:
				Operators operator = Operators.getOperatorByValue(value);
				returnValue = interpretOperator(operator, sprite);
				break;
			case FUNCTION:
				Functions function = Functions.getFunctionByValue(value);
				returnValue = interpretFunction(function, sprite);
				break;
			case SENSOR:
				Sensors sensor = Sensors.getSensorByValue(value);
				if (sensor.isObjectSensor) {
					returnValue = interpretObjectSensor(sensor, sprite);
				} else {
					returnValue = SensorHandler.getSensorValue(sensor);
				}
				break;
			case USER_VARIABLE:
				UserVariablesContainer userVariables = ProjectManager.getInstance().getCurrentProject()
						.getUserVariables();
				UserVariable userVariable = userVariables.getUserVariable(value, sprite);
				if (userVariable == null) {
					returnValue = NOT_EXISTING_USER_VARIABLE_INTERPRETATION_VALUE;
					break;
				}

				Object userVariableValue = userVariable.getValue();
				if (userVariableValue instanceof String) {
					try {
						returnValue = interpretValueAsString((String) userVariableValue);
					} catch (NumberFormatException numberFormatException) {
						returnValue = userVariableValue;
					}
				} else {
					returnValue = userVariableValue;
				}
				break;

			case STRING:
				returnValue = interpretValueAsString(value);
				break;

		}

		returnValue = checkDegeneratedDoubleValues(returnValue);
		return returnValue;
	}

	private Object interpretFunction(Functions function, Sprite sprite) throws NumberFormatException {
		Object left = null;
		Object right = null;

		if (leftChild != null) {
			left = leftChild.interpretRecursive(sprite);
		}

		switch (function) {
			case SIN:
				return java.lang.Math.sin(Math.toRadians((Double) left));

			case COS:
				return java.lang.Math.cos(Math.toRadians((Double) left));

			case TAN:
				return java.lang.Math.tan(Math.toRadians((Double) left));

			case LN:
				return java.lang.Math.log((Double) left);

			case LOG:
				return java.lang.Math.log10((Double) left);

			case SQRT:
				return java.lang.Math.sqrt((Double) left);

			case RAND:
				right = rightChild.interpretRecursive(sprite);
				Double minimum = java.lang.Math.min((Double) left, (Double) right);
				Double maximum = java.lang.Math.max((Double) left, (Double) right);

				Double randomDouble = minimum + (java.lang.Math.random() * (maximum - minimum));

				if (isInteger(minimum) && isInteger(maximum)
						&& !(rightChild.type == ElementType.NUMBER && rightChild.value.contains("."))
						&& !(leftChild.type == ElementType.NUMBER && leftChild.value.contains("."))) {
					Log.i("info", "randomDouble: " + randomDouble);

					if ((Math.abs(randomDouble) - (int) Math.abs(randomDouble)) >= 0.5) {
						return Double.valueOf(randomDouble.intValue()) + 1;
					} else {
						return Double.valueOf(randomDouble.intValue());
					}

				} else {
					return randomDouble;
				}

			case ABS:
				return java.lang.Math.abs((Double) left);

			case ROUND:
				return (double) java.lang.Math.round((Double) left);

			case PI:
				return java.lang.Math.PI;

			case MOD:
				double dividend = (Double) left;
				double divisor = (Double) rightChild.interpretRecursive(sprite);

				if (dividend == 0 || divisor == 0) {
					return dividend;
				}

				if (divisor > 0) {
					while (dividend < 0) {
						dividend += java.lang.Math.abs(divisor);
					}
				} else {
					if (dividend > 0) {
						return (dividend % divisor) + divisor;
					}
				}

				return dividend % divisor;

			case ARCSIN:
				return java.lang.Math.toDegrees(Math.asin((Double) left));
			case ARCCOS:
				return java.lang.Math.toDegrees(Math.acos((Double) left));
			case ARCTAN:
				return java.lang.Math.toDegrees(Math.atan((Double) left));
			case EXP:
				return java.lang.Math.exp((Double) left);
			case MAX:
				right = rightChild.interpretRecursive(sprite);
				return java.lang.Math.max((Double) left, (Double) right);
			case MIN:
				right = rightChild.interpretRecursive(sprite);
				return java.lang.Math.min((Double) left, (Double) right);
			case TRUE:
				return 1d;
			case FALSE:
				return 0d;
			case LETTER:
				int index = ((Double) left).intValue() - 1;
				right = rightChild.interpretRecursive(sprite);

				//				if (rightChild.type == ElementType.USER_VARIABLE) {
				//					value = handleLetterUserVariableParameter(sprite);
				//				}

				if (index < 0) {
					return "";
				} else if (index >= String.valueOf(right).length()) {
					return "";
				}
				return String.valueOf(right).charAt(index);
			case LENGTH:
				if (leftChild == null) {
					return 0d;
				}
				if (leftChild.type == ElementType.NUMBER) {
					return (double) leftChild.value.length();
				}
				if (leftChild.type == ElementType.STRING) {
					return (double) leftChild.value.length();
				}
				if (leftChild.type == ElementType.USER_VARIABLE) {
					return (double) handleLengthUserVariableParameter(sprite);
				}
				return (double) (String.valueOf(left)).length();
			case JOIN:
				String firstPart = "";
				if (leftChild != null) {
					if (leftChild.getElementType() == ElementType.NUMBER) {
						Double number = ((Double) leftChild.interpretRecursive(sprite));
						if (isInteger(number)) {
							firstPart += number.intValue();
						} else {
							firstPart += number;
						}
					} else if (leftChild.getElementType() == ElementType.STRING) {
						firstPart = leftChild.value;
					} else if (leftChild.getElementType() != ElementType.STRING) {
						firstPart += leftChild.interpretRecursive(sprite);
					}
				}
				String secondPart = "";
				if (rightChild != null) {
					if (rightChild.getElementType() == ElementType.NUMBER) {
						Double number = ((Double) rightChild.interpretRecursive(sprite));
						if (isInteger(number)) {
							secondPart += number.intValue();
						} else {
							secondPart += number;
						}
					} else if (rightChild.getElementType() == ElementType.STRING) {
						secondPart = rightChild.value;
					} else if (rightChild.getElementType() != ElementType.STRING) {
						secondPart += rightChild.interpretRecursive(sprite);
					}
				}
				return firstPart + secondPart;
		}
		return 0d;
	}

	private Object interpretOperator(Operators operator, Sprite sprite) throws NumberFormatException {

		if (leftChild != null) {// binary operator
			Object leftObject;
			Object rightObject;
			try {
				leftObject = leftChild.interpretRecursive(sprite);
			} catch (NumberFormatException numberFormatException) {
				leftObject = new Double(0);
			}

			try {
				rightObject = rightChild.interpretRecursive(sprite);
			} catch (NumberFormatException numberFormatException) {
				rightObject = new Double(0);
			}

			Double left;
			Double right;

			switch (operator) {
				case PLUS:
					left = (Double) interpretOperatorDefault(leftObject);
					right = (Double) interpretOperatorDefault(rightObject);
					return left + right;
				case MINUS:
					left = (Double) interpretOperatorDefault(leftObject);
					right = (Double) interpretOperatorDefault(rightObject);
					return left - right;
				case MULT:
					left = (Double) interpretOperatorDefault(leftObject);
					right = (Double) interpretOperatorDefault(rightObject);
					return left * right;
				case DIVIDE:
					left = (Double) interpretOperatorDefault(leftObject);
					right = (Double) interpretOperatorDefault(rightObject);
					return left / right;
				case POW:
					left = (Double) interpretOperatorDefault(leftObject);
					right = (Double) interpretOperatorDefault(rightObject);
					return java.lang.Math.pow(left, right);
				case EQUAL:
					return interpretOperatorEqual(leftObject, rightObject);
				case NOT_EQUAL:
					return interpretOperatorEqual(leftObject, rightObject) == 1d ? 0d : 1d;
				case GREATER_THAN:
					left = (Double) interpretOperatorDefault(leftObject);
					right = (Double) interpretOperatorDefault(rightObject);
					return left.compareTo(right) > 0 ? 1d : 0d;
				case GREATER_OR_EQUAL:
					left = (Double) interpretOperatorDefault(leftObject);
					right = (Double) interpretOperatorDefault(rightObject);
					return left.compareTo(right) >= 0 ? 1d : 0d;
				case SMALLER_THAN:
					left = (Double) interpretOperatorDefault(leftObject);
					right = (Double) interpretOperatorDefault(rightObject);
					return left.compareTo(right) < 0 ? 1d : 0d;
				case SMALLER_OR_EQUAL:
					left = (Double) interpretOperatorDefault(leftObject);
					right = (Double) interpretOperatorDefault(rightObject);
					return left.compareTo(right) <= 0 ? 1d : 0d;
				case LOGICAL_AND:
					left = (Double) interpretOperatorDefault(leftObject);
					right = (Double) interpretOperatorDefault(rightObject);
					return (left * right) != 0d ? 1d : 0d;
				case LOGICAL_OR:
					left = (Double) interpretOperatorDefault(leftObject);
					right = (Double) interpretOperatorDefault(rightObject);
					return left != 0d || right != 0d ? 1d : 0d;
			}

		} else {//unary operators
			Object right = rightChild.interpretRecursive(sprite);

			switch (operator) {
				case MINUS:
					Double result = (Double) right;
					return Double.valueOf(-result.doubleValue());
				case LOGICAL_NOT:
					return (Double) right == 0d ? 1d : 0d;
			}

		}

		return 0d;
	}

	private Object interpretObjectSensor(Sensors sensor, Sprite sprite) throws NumberFormatException {
		Object returnValue = 0d;
		switch (sensor) {
			case OBJECT_BRIGHTNESS:
				returnValue = (double) sprite.look.getBrightnessInUserInterfaceDimensionUnit();
				break;
			case OBJECT_GHOSTEFFECT:
				returnValue = (double) sprite.look.getTransparencyInUserInterfaceDimensionUnit();
				break;
			case OBJECT_LAYER:
				returnValue = (double) sprite.look.getZIndex();
				break;
			case OBJECT_ROTATION:
				returnValue = (double) sprite.look.getDirectionInUserInterfaceDimensionUnit();
				break;
			case OBJECT_SIZE:
				returnValue = (double) sprite.look.getSizeInUserInterfaceDimensionUnit();
				break;
			case OBJECT_X:
				returnValue = (double) sprite.look.getXInUserInterfaceDimensionUnit();
				break;
			case OBJECT_Y:
				returnValue = (double) sprite.look.getYInUserInterfaceDimensionUnit();
				break;
		}
		return returnValue;
	}

	private Object interpretValueAsString(String value) throws NumberFormatException {

		if (parent == null && type != ElementType.USER_VARIABLE) {
			return value;
		}

		if (parent != null) {
			boolean isParentAFunction = Functions.getFunctionByValue(parent.value) != null;
			if (isParentAFunction && Functions.getFunctionByValue(parent.value).returnType == ElementType.STRING) {
				if (Functions.getFunctionByValue(parent.value) == Functions.LETTER && parent.leftChild == this) {
					try {
						return Double.valueOf(value);
					} catch (NumberFormatException numberFormatexception) {
						return Double.valueOf(0);
					}
				}
				return value;
			}

			if (isParentAFunction) {
				return Double.valueOf(0.0);
			}

			boolean isParentAOperator = Operators.getOperatorByValue(parent.value) != null;
			if (isParentAOperator
					&& (Operators.getOperatorByValue(parent.value) == Operators.EQUAL || Operators
							.getOperatorByValue(parent.value) == Operators.NOT_EQUAL)) {
				return value;
			}
		}

		if (value.length() == 0) {
			return Double.valueOf(0.0);
		}

		return Double.valueOf(value);
	}

	private Double interpretOperatorEqual(Object left, Object right) {

		if (left instanceof String && right instanceof String) {
			int compareResult = ((String) left).compareTo((String) right);
			if (compareResult == 0) {
				return 1d;
			}
		}
		if (left instanceof Double && right instanceof String) {
			try {
				int compareResult = ((Double) left).compareTo(Double.valueOf((String) right));
				if (compareResult == 0) {
					return 1d;
				}
			} catch (NumberFormatException numberFormatException) {
				return 0d;
			}
		}
		if (left instanceof String && right instanceof Double) {
			try {
				int compareResult = Double.valueOf((String) left).compareTo((Double) right);
				if (compareResult == 0) {
					return 1d;
				}
			} catch (NumberFormatException numberFormatException) {
				return 0d;
			}
		}
		if (left instanceof Double && right instanceof Double) {
			return (((Double) left).compareTo((Double) right) == 0) ? 1d : 0d;
		}
		return 0d;
	}

	private Object interpretOperatorDefault(Object object) throws NumberFormatException {
		if (object instanceof String) {
			return Double.valueOf((String) object);
		} else {
			return object;
		}
	}

	private Object checkDegeneratedDoubleValues(Object valueToCheck) throws NumberFormatException {

		if (valueToCheck instanceof String || valueToCheck instanceof Character) {
			return valueToCheck;
		}

		if (valueToCheck == null) {
			return 0.0;
		}

		if (((Double) valueToCheck).doubleValue() == Double.NEGATIVE_INFINITY) {
			return -Double.MAX_VALUE;
		}
		if (((Double) valueToCheck).doubleValue() == Double.POSITIVE_INFINITY) {
			return Double.MAX_VALUE;
		}
		if (((Double) valueToCheck).isNaN()) {
			throw new NumberFormatException("NaN");
		}

		return valueToCheck;
	}

	public FormulaElement getParent() {
		return parent;
	}

	public void setRightChild(FormulaElement rightChild) {
		this.rightChild = rightChild;
		this.rightChild.parent = this;
	}

	public void setLeftChild(FormulaElement leftChild) {
		this.leftChild = leftChild;
		this.leftChild.parent = this;
	}

	public void replaceElement(FormulaElement current) {
		parent = current.parent;
		leftChild = current.leftChild;
		rightChild = current.rightChild;
		value = current.value;
		type = current.type;

		if (leftChild != null) {
			leftChild.parent = this;
		}
		if (rightChild != null) {
			rightChild.parent = this;
		}
	}

	public void replaceElement(ElementType type, String value) {
		this.value = value;
		this.type = type;
	}

	public void replaceWithSubElement(String operator, FormulaElement rightChild) {

		FormulaElement cloneThis = new FormulaElement(ElementType.OPERATOR, operator, this.getParent(), this,
				rightChild);

		cloneThis.parent.rightChild = cloneThis;
	}

	private boolean isInteger(double value) {

		if ((Math.abs(value) - (int) Math.abs(value) < Double.MIN_VALUE)) {
			return true;
		}

		return false;
	}

	public boolean isLogicalOperator() {
		if (type == ElementType.OPERATOR) {
			return Operators.getOperatorByValue(value).isLogicalOperator;
		}
		return false;
	}

	public boolean hasFunctionStringReturnType() {
		Functions function = Functions.getFunctionByValue(value);
		if (function == null) {
			return false;
		}
		return function.returnType == ElementType.STRING;
	}

	public boolean containsElement(ElementType elementType) {
		if (type.equals(elementType) || (leftChild != null && leftChild.containsElement(elementType))
				|| (rightChild != null && rightChild.containsElement(elementType))) {
			return true;
		}
		return false;
	}

	public boolean isUserVariableWithTypeString(Sprite sprite) {
		if (type == ElementType.USER_VARIABLE) {
			UserVariablesContainer userVariableContainer = ProjectManager.getInstance().getCurrentProject()
					.getUserVariables();
			UserVariable userVariable = userVariableContainer.getUserVariable(value, sprite);

			Object userVariableValue = userVariable.getValue();
			if (userVariableValue instanceof String) {
				return true;
			} else {
				return false;
			}
		}
		return false;
	}

	private int handleLengthUserVariableParameter(Sprite sprite) {
		UserVariablesContainer userVariableContainer = ProjectManager.getInstance().getCurrentProject()
				.getUserVariables();
		UserVariable userVariable = userVariableContainer.getUserVariable(leftChild.value, sprite);

		Object userVariableValue = userVariable.getValue();
		if (userVariableValue instanceof String) {
			return (String.valueOf(userVariableValue)).length();
		} else {
			if (isInteger((Double) userVariableValue)) {
				return Integer.toString(((Double) userVariableValue).intValue()).length();
			} else {
				return Double.toString(((Double) userVariableValue)).length();
			}
		}

	}

	private String handleLetterUserVariableParameter(Sprite sprite) {
		UserVariablesContainer userVariableContainer = ProjectManager.getInstance().getCurrentProject()
				.getUserVariables();
		UserVariable userVariable = userVariableContainer.getUserVariable(rightChild.value, sprite);

		Object userVariableValue = userVariable.getValue();
		if (userVariableValue instanceof String) {
			return String.valueOf(userVariableValue);
		} else {
			if (isInteger((Double) userVariableValue)) {
				return Integer.toString(((Double) userVariableValue).intValue());
			} else {
				return Double.toString(((Double) userVariableValue));
			}
		}
	}

	public boolean isSingleNumberFormula() {
		if (type == ElementType.OPERATOR) {
			Operators operator = Operators.getOperatorByValue(value);
			if (operator == Operators.MINUS && leftChild == null) {
				return rightChild.isSingleNumberFormula();
			}
			return false;
		} else if (type == ElementType.NUMBER) {
			return true;
		}
		return false;
	}

	@Override
	public FormulaElement clone() {
		FormulaElement leftChildClone = leftChild == null ? null : leftChild.clone();
		FormulaElement rightChildClone = rightChild == null ? null : rightChild.clone();
		return new FormulaElement(type, new String(value == null ? "" : value), null, leftChildClone, rightChildClone);
	}

}
