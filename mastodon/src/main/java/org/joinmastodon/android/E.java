package org.joinmastodon.android;

import com.squareup.otto.AsyncBus;

/**
 * Created by grishka on 24.08.15.
 */
public class E{
	private static AsyncBus bus=new AsyncBus();

	public static void post(Object event){
		bus.post(event);
	}

	public static void register(Object listener){
		bus.register(listener);
	}

	public static void unregister(Object listener){
		bus.unregister(listener);
	}
}
