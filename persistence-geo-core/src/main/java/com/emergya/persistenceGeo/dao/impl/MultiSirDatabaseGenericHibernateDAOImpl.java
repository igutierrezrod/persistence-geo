/*
 * GenericHibernateDAOImpl.java
 * 
 * Copyright (C) 2012
 * 
 * This file is part of Proyecto persistenceGeo
 * 
 * This software is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this library; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 * 
 * As a special exception, if you link this library with other files to produce
 * an executable, this library does not by itself cause the resulting executable
 * to be covered by the GNU General Public License. This exception does not
 * however invalidate any other reasons why the executable file might be covered
 * by the GNU General Public License.
 */
package com.emergya.persistenceGeo.dao.impl;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import com.emergya.persistenceGeo.dao.MultiSirDatabaseGenericDAO;

/**
 * Based on http://community.jboss.org/docs/DOC-13955
 * 
 * @param <T>
 *            entity type
 * @param <ID>
 *            primary key
 */
public abstract class MultiSirDatabaseGenericHibernateDAOImpl<T, ID extends Serializable>
		extends HibernateDaoSupport implements
		MultiSirDatabaseGenericDAO<T, ID> {

	protected Class<T> persistentClass;

	@Autowired
	@Qualifier("sessionFactoryMultiSIRDataSource")
	public void init(SessionFactory sessionFactory) {
		setSessionFactory(sessionFactory);
	}

	@SuppressWarnings({ "unchecked" })
	public MultiSirDatabaseGenericHibernateDAOImpl() {
		try {
			persistentClass = (Class<T>) ((ParameterizedType) getClass()
					.getGenericSuperclass()).getActualTypeArguments()[0];
		} catch (ClassCastException e) {
			// can be raised when DAO is inherited twice
			persistentClass = (Class<T>) ((ParameterizedType) getClass()
					.getSuperclass().getGenericSuperclass())
					.getActualTypeArguments()[0];
		}
	}

	public T findById(Long id, boolean lock) {
		T entity;
		if (lock) {
			entity = (T) getHibernateTemplate().get(persistentClass, id,
					LockMode.PESSIMISTIC_WRITE);
		} else {
			entity = (T) getHibernateTemplate().get(persistentClass, id);
		}

		return entity;
	}

	@Override
	public List<T> findAll() {
		return findByCriteria();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> findAllFromTo(Integer first, Integer last) {
		Criteria criteria = getSession().createCriteria(persistentClass);
		criteria.setFirstResult(first);
		criteria.setMaxResults(last - first);
		return criteria.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> findOrdered(Integer first, Integer last, String orderField,
			boolean ascending) {
		Criteria criteria = getSession().createCriteria(persistentClass);
		criteria.setFirstResult(first);
		criteria.setMaxResults(last - first);

		if (ascending) {
			criteria.addOrder(Order.asc(orderField));
		} else {
			criteria.addOrder(Order.desc(orderField));
		}
		return criteria.list();
	}

	@Override
	public List<T> findByExample(T exampleInstance, String[] excludedProperties) {
		return this.findByExample(exampleInstance, excludedProperties, false);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> findByExample(T exampleInstance,
			String[] excludedProperties, boolean ignoreCase) {
		DetachedCriteria crit = DetachedCriteria.forClass(persistentClass);
		Example example = Example.create(exampleInstance);

		if (excludedProperties != null) {
			for (String exclude : excludedProperties) {
				example.excludeProperty(exclude);
			}
		}

		if (ignoreCase) {
			crit.add(example.ignoreCase());
		} else {
			crit.add(example);
		}

		return getHibernateTemplate().findByCriteria(crit);
	}

	@Override
	public T makePersistent(T entity) {
		// Fix for #87143 and other "hibernate session contains the object"
		// problems
		// by checking that, and merging instead of updating in that case.
		if (getHibernateTemplate().contains(entity)) {
			return getHibernateTemplate().merge(entity);
		} else {
			getHibernateTemplate().saveOrUpdate(entity);
		}

		return entity;
	}

	@Override
	public void makeTransient(T entity) {
		getHibernateTemplate().delete(entity);
	}

	@Override
	public Long getResults() {
		return (Long) getSession().createCriteria(persistentClass)
				.setProjection(Projections.count("id")).uniqueResult();
	}

	/**
	 * Use this inside subclasses as a convenience method.
	 */
	@SuppressWarnings("unchecked")
	protected List<T> findByCriteria(Criterion... detachedCriterias) {
		DetachedCriteria crit = DetachedCriteria.forClass(persistentClass);
		for (Criterion c : detachedCriterias) {
			crit.add(c);
		}
		return getHibernateTemplate().findByCriteria(crit);
	}

	/**
	 * Método que se encarga de aplicar un mapa de parametros a una Query dada.
	 * 
	 * @param query
	 *            Query donde aplicar los parametros.
	 * @param parametros
	 *            Parametros que se deben aplicar.
	 */
	protected final void aplicarParametros(final Query query,
			final Map<String, Object> parametros) {
		if (query == null) {
			throw new IllegalArgumentException("query");
		}
		if (parametros == null) {
			throw new IllegalArgumentException("parametros");
		}
		for (final Map.Entry<String, Object> entry : parametros.entrySet()) {
			query.setParameter(entry.getKey(), entry.getValue());
		}
	}

}
