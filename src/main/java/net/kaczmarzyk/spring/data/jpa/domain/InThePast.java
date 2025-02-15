/**
 * Copyright 2014-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kaczmarzyk.spring.data.jpa.domain;

import net.kaczmarzyk.spring.data.jpa.utils.QueryContext;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Expression;

/**
 * <p>Filters with {@code <} where-clause (e.g. {@code where dateOfBirth < localtimestamp()}).</p>
 * <p>Given date type value in path is compared to current timestamp of the database.</p>
 *
 * <p>Supports date type fields.</p>
 */
public class InThePast<T, TimeType extends Comparable<TimeType>> extends PathSpecification<T> {

	private static final long serialVersionUID = 1L;

	public InThePast(QueryContext queryContext, String path) {
		super(queryContext, path);
	}

	@Override
	public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
		Expression<TimeType> rootPath = path(root);
		Expression<TimeType> currentTimestamp = (Expression<TimeType>) cb.currentTimestamp();

		return cb.lessThan(rootPath, currentTimestamp);
	}
}
