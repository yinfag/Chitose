package ru.yinfag.chitose;

/**
 * Created with IntelliJ IDEA.
 * User: tahara
 * Date: 14.09.12
 * Time: 1:27
 * To change this template use File | Settings | File Templates.
 */
public final class Pair<A, B> {

	private final A myFirst;
	private final B mySecond;

	public Pair(final A first, final B second) {
		this.myFirst = first;
		this.mySecond = second;
	}

	public final A getFirst() {
		return myFirst;
	}

	public final B getSecond() {
		return mySecond;
	}
}
