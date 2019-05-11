package com.github.jikoo.enchantableblocks.util;

public class Triple<L, M, R> {

	private L left;
	private M middle;
	private R right;

	public Triple(L left, M middle, R right) {
		this.left = left;
		this.middle = middle;
		this.right = right;
	}

	public L getLeft() {
		return left;
	}

	public M getMiddle() {
		return middle;
	}

	public R getRight() {
		return right;
	}

	public void setLeft(L left) {
		this.left = left;
	}

	public void setMiddle(M middle) {
		this.middle = middle;
	}

	public void setRight(R right) {
		this.right = right;
	}

	@Override
	public int hashCode() {
		int hash = 1;
		hash = hash * 17 * left.hashCode();
		hash = hash * 17 * middle.hashCode();
		hash = hash * 17 * right.hashCode();
		return hash;
	}

}
