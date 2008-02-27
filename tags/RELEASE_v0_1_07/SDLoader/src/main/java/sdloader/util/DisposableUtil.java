/*
 * Copyright 2005-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sdloader.util;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author shot
 */
public final class DisposableUtil {

	protected static final LinkedList<Disposable> disposables = CollectionsUtil
			.newLinkedList();

	protected static boolean DEBUG = false;

	public static synchronized void add(final Disposable disposable) {
		disposables.add(disposable);
	}

	public static synchronized void remove(final Disposable disposable) {
		disposables.remove(disposable);
	}

	public static synchronized void dispose() {
		listDisposables();
		while (!disposables.isEmpty()) {
			final Disposable disposable = disposables.removeLast();
			try {
				disposable.dispose();
			} catch (final Throwable t) {
				t.printStackTrace();
			}
		}
		disposables.clear();
	}

	private static void listDisposables() {
		if (!DEBUG) {
			return;
		}
		for (Iterator<Disposable> iterator = disposables.iterator(); iterator
				.hasNext();) {
			Disposable disposable = iterator.next();
			System.out
					.println("[Disposable] dipose : " + disposable.toString());
		}
	}

	public static void setDebugMode(final boolean debug) {
		DEBUG = debug;
	}

	public static interface Disposable {

		void dispose();
	}

}
