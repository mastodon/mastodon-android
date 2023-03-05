package org.joinmastodon.android.utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Function;

public class TypedObjectPool<K, V>{
	private final Function<K, V> producer;
	private final HashMap<K, LinkedList<V>> pool=new HashMap<>();

	public TypedObjectPool(Function<K, V> producer){
		this.producer=producer;
	}

	public V obtain(K type){
		LinkedList<V> tp=pool.get(type);
		if(tp==null)
			pool.put(type, tp=new LinkedList<>());

		V value=tp.poll();
		if(value==null)
			value=producer.apply(type);
		return value;
	}

	public void reuse(K type, V obj){
		Objects.requireNonNull(obj);
		Objects.requireNonNull(type);

		LinkedList<V> tp=pool.get(type);
		if(tp==null)
			pool.put(type, tp=new LinkedList<>());
		tp.add(obj);
	}
}
