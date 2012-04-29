package ru.yinfag.chitose;

import java.util.*;

public class Utils {
	public static String join(final Iterable<?> items, final CharSequence separator) {
		final Iterator<?> it = items.iterator();
		if (!it.hasNext()) {
			return "";
		}
		final StringBuilder sb = new StringBuilder(it.next().toString());
		while (it.hasNext()) {
			sb.append(separator).append(it.next());
		}
		return sb.toString();
	}
}
