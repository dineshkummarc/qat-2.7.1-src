package qat.parser.qashparser;

import java.lang.Object;
import qat.parser.qashparser.QASHToken;

public class QASHTree extends Object {
	
	public QASHTree parent, left, right;
	public QASHToken value;
  
	public QASHTree() {
		this.parent = null;
		this.value = null;
		left = null;
		right = null;
	}
	
	public QASHTree(QASHTree parent) {
		this.parent = parent;
	}
	
	public QASHTree(QASHTree parent, QASHToken value) {
		this(value);
		this.parent = parent;
	}
	
	public QASHTree(QASHToken value) {
		this.parent = null;
		this.value = value;
		left = null;
		right = null;
	}
	
	public boolean isLeaf() {
		return ((left==null)&&(right==null));
	}
}
