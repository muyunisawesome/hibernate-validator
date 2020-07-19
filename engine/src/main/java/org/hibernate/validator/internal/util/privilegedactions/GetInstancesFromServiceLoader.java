/*
 * Hibernate Validator, declare and validate application constraints
 *
 * License: Apache License, Version 2.0
 * See the license.txt file in the root directory or <http://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.validator.internal.util.privilegedactions;

import org.hibernate.validator.internal.util.logging.Log;
import org.hibernate.validator.internal.util.logging.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * @author Guillaume Smet
 */
public class GetInstancesFromServiceLoader<T> implements PrivilegedAction<List<T>> {

	//加载类的 classloader
	private final ClassLoader primaryClassLoader;

	//要加载的class
	private final Class<T> clazz;

	private static final Log LOG = LoggerFactory.make( MethodHandles.lookup() );

	private GetInstancesFromServiceLoader(ClassLoader primaryClassLoader, Class<T> clazz) {
		this.primaryClassLoader = primaryClassLoader;
		this.clazz = clazz;
	}

	public static <T> GetInstancesFromServiceLoader<T> action(ClassLoader primaryClassLoader, Class<T> serviceClass) {
		return new GetInstancesFromServiceLoader<T>( primaryClassLoader, serviceClass );
	}

	@Override
	public List<T> run() {
		// Option #1: try the primary class loader first (either the thread context class loader or the external class
		// loader that has been defined)
		// 加载类的实例
		List<T> instances = loadInstances( primaryClassLoader );

		// Option #2: if we cannot find any service files within the primary class loader, use the current class loader
		if ( instances.isEmpty() && GetInstancesFromServiceLoader.class.getClassLoader() != primaryClassLoader ) {
			instances = loadInstances( GetInstancesFromServiceLoader.class.getClassLoader() );
		}

		return instances;
	}

	private List<T> loadInstances(ClassLoader classloader) {
		//加载spi
		ServiceLoader<T> loader = ServiceLoader.load( clazz, classloader );
		Iterator<T> iterator = loader.iterator();
		List<T> instances = new ArrayList<T>();
		while ( iterator.hasNext() ) {
			try {
				instances.add( iterator.next() );
			}
			catch (ServiceConfigurationError e) {
				// ignore, because it can happen when multiple
				// services are present and some of them are not class loader
				// compatible with our API.
				// log an error still as it can hide a legitimate issue (see HV-1689)
				LOG.unableToLoadInstanceOfService( loader.getClass().getName(), e );
			}
		}
		return instances;
	}
}
