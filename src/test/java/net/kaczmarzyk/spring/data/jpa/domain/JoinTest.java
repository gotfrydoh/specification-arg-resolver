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

import static jakarta.persistence.criteria.JoinType.INNER;
import static jakarta.persistence.criteria.JoinType.LEFT;
import static net.kaczmarzyk.spring.data.jpa.CustomerBuilder.customer;
import static net.kaczmarzyk.spring.data.jpa.ItemTagBuilder.itemTag;
import static net.kaczmarzyk.spring.data.jpa.OrderBuilder.order;
import static net.kaczmarzyk.spring.data.jpa.utils.ThrowableAssertions.assertThrows;
import static net.kaczmarzyk.utils.InterceptedStatementsAssert.assertThatInterceptedStatements;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.jparams.verifier.tostring.ToStringVerifier;

import net.kaczmarzyk.spring.data.jpa.Customer;
import net.kaczmarzyk.spring.data.jpa.IntegrationTestBase;
import net.kaczmarzyk.spring.data.jpa.ItemTag;
import net.kaczmarzyk.utils.interceptor.HibernateStatementInspector;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * @author Jakub Radlica
 * @author Tomasz Kaczmarzyk
 */
public class JoinTest extends IntegrationTestBase {

	Customer homerSimpson;
	Customer margeSimpson;
	Customer bartSimpson;

	@Before
	public void initData() {
		ItemTag books = itemTag("books").build(em);

		homerSimpson = customer("Homer", "Simpson")
				.orders(order("Duff Beer"), order("Donuts"))
				.build(em);

		margeSimpson = customer("Marge", "Simpson")
				.build(em);

		bartSimpson = customer("Bart", "Simpson")
				.orders(order("Comic Books").withTags(books))
				.build(em);

		HibernateStatementInspector.clearInterceptedStatements();
	}

	@Test
	public void joinsCollection() {
		Join<Customer> joinOrders = new Join<>(queryCtx, "orders", "o", LEFT, true);
		Equal<Customer> orderedItemName = new Equal<>(queryCtx, "o.itemName", new String[]{ "Duff Beer" }, defaultConverter);

		Conjunction<Customer> conjunction = new Conjunction<>(joinOrders, orderedItemName);

		List<Customer> customers = customerRepo.findAll(conjunction, Sort.by("id"));

		assertThat(customers)
				.extracting(Customer::getFirstName)
				.containsExactly("Homer");
	}

	@Test
	public void performsMultilevelJoinWithAttributeOfTypeSet() {
		Join<Customer> joinOrders = new Join<>(queryCtx, "orders", "o", LEFT, true);
		Join<Customer> joinTags = new Join<>(queryCtx, "o.tags", "t", LEFT, true);
		Equal<Customer> tagEqual = new Equal<>(queryCtx, "t.name", new String[]{ "books" }, defaultConverter);

		Conjunction<Customer> conjunction = new Conjunction<>(joinOrders, joinTags, tagEqual);

		List<Customer> customers = customerRepo.findAll(conjunction, Sort.by("id"));

		assertThat(customers)
				.extracting(Customer::getFirstName)
				.containsExactly("Bart");
	}

	@Test
	public void performsMultilevelJoinWithAttributeOfTypeList() {
		Join<Customer> joinOrders = new Join<>(queryCtx, "orders", "o", LEFT, true);
		Join<Customer> joinTags = new Join<>(queryCtx, "o.tagsList", "t", LEFT, true);
		Equal<Customer> tagEqual = new Equal<>(queryCtx, "t.name", new String[]{ "books" }, defaultConverter);

		Conjunction<Customer> conjunction = new Conjunction<>(joinOrders, joinTags, tagEqual);

		List<Customer> customers = customerRepo.findAll(conjunction, Sort.by("id"));

		assertThat(customers)
				.extracting(Customer::getFirstName)
				.containsExactly("Bart");
	}

	@Test
	public void performsMultilevelJoinWithAttributeOfTypeCollection() {
		Join<Customer> joinOrders = new Join<>(queryCtx, "orders", "o", LEFT, true);
		Join<Customer> joinTags = new Join<>(queryCtx, "o.tagsCollection", "t", LEFT, true);
		Equal<Customer> tagEqual = new Equal<>(queryCtx, "t.name", new String[]{ "books" }, defaultConverter);

		Conjunction<Customer> conjunction = new Conjunction<>(joinOrders, joinTags, tagEqual);

		List<Customer> customers = customerRepo.findAll(conjunction, Sort.by("id"));

		assertThat(customers)
				.extracting(Customer::getFirstName)
				.containsExactly("Bart");
	}

	@Test
	public void performsMultilevelJoinWithSimpleEntityAttribute() {
		Join<Customer> joinOrders = new Join<>(queryCtx, "orders", "o", LEFT, true);
		Join<Customer> joinTags = new Join<>(queryCtx, "o.note", "n", LEFT, true);
		Equal<Customer> tagEqual = new Equal<>(queryCtx, "n.title", new String[]{ "NoteDonuts" }, defaultConverter);

		Conjunction<Customer> conjunction = new Conjunction<>(joinOrders, joinTags, tagEqual);

		List<Customer> customers = customerRepo.findAll(conjunction, Sort.by("id"));

		assertThat(customers)
				.extracting(Customer::getFirstName)
				.containsExactly("Homer");
	}

	@Test
	public void throwsIllegalArgumentExceptionWhenJoinsAreDefinedInInvalidOrder() {
		Join<Customer> joinTags = new Join<>(queryCtx, "o.tags", "t", LEFT, true);
		Join<Customer> joinOrders = new Join<>(queryCtx, "orders", "o", LEFT, true);
		Equal<Customer> tagEqual = new Equal<>(queryCtx, "t.name", new String[]{ "books" }, defaultConverter);

		Conjunction<Customer> conjunction = new Conjunction<>(joinTags, joinOrders, tagEqual);

		assertThrows(
				InvalidDataAccessApiUsageException.class,
				() -> customerRepo.findAll(conjunction),
				"Join definition with alias: 'o' not found! " +
						"Make sure that join with the alias 'o' is defined before the join with path: 'o.tags'"
		);
	}
	
	@Test
	public void innerJoinIsEvaluatedEvenIfNoFilteringIsAppliedOnTheJoinedPart() {
		Join<Customer> innerJoinOrders = new Join<>(queryCtx, "orders", "", INNER, true);
		
		
		List<Customer> found = customerRepo.findAll(innerJoinOrders);
		
		assertThat(found)
			.hasSize(2)
			.extracting(Customer::getFirstName)
			.containsOnly("Bart", "Homer");
		
		assertThatInterceptedStatements()
			.hasSelects(1)
			.hasNumberOfJoins(1);
	}
	
	@Test
	public void innerJoinIsEvaluatedEvenIfNoFilteringIsAppliedOnTheJoinedPart_multiLevelJoin() {
		Join<Customer> leftJoinOrders = new Join<>(queryCtx, "orders", "o", LEFT, true);
		Join<Customer> innerJoinTags = new Join<>(queryCtx, "o.tags", "", INNER, true);
		
		List<Customer> found = customerRepo.findAll(Specification.where(leftJoinOrders).and(innerJoinTags));
		
		assertThat(found)
			.hasSize(1)
			.extracting(Customer::getFirstName)
			.containsOnly("Bart");
		
		assertThatInterceptedStatements()
			.hasSelects(1)
			.hasNumberOfJoins(2)
			.hasNumberOfTableJoins("orders", LEFT, 1)
			.hasNumberOfTableJoins("orders_tags", INNER, 1); // many-to-many join table
	}
	
	@Test
	public void leftJoinIsEvaluatedEvenIfNoFilteringIsAppliedOnTheJoinedPartButQueryIsNotDistinct() {
		Join<Customer> joinOrders = new Join<>(queryCtx, "orders", "", LEFT, false);
		
		
		List<Customer> found = customerRepo.findAll(joinOrders);
		
		assertThat(found)
			.hasSize(3)  // hibernate 6+ makes query distinct anyway
			.extracting(Customer::getFirstName)
			.containsOnly("Bart", "Homer", "Marge");
		
		assertThatInterceptedStatements()
			.hasSelects(1)
			.hasNumberOfJoins(1);
	}
	
	@Ignore // Hibernate 6+ makes all queries distinct, so this test fails
	@Test
	public void leftJoinIsEvaluatedEvenIfNoFilteringIsAppliedOnTheJoinedPartButQueryIsNotDistinct_multiLevelJoin() {
		Join<Customer> leftJoinOrders = new Join<>(queryCtx, "orders", "o", LEFT, false);
		Join<Customer> innerJoinTags = new Join<>(queryCtx, "o.tags", "", LEFT, false);
		
		List<Customer> found = customerRepo.findAll(Specification.where(leftJoinOrders).and(innerJoinTags));
		
		assertThat(found)
			.hasSize(3) // hibernate 6+ makes query distinct anyway
			.extracting(Customer::getFirstName)
			.containsOnly("Bart", "Homer", "Marge");
		
		assertThatInterceptedStatements()
			.hasSelects(1)
			.hasNumberOfJoins(2)
			.hasNumberOfTableJoins("orders", LEFT, 1)
			.hasNumberOfTableJoins("tags", LEFT, 1);
	}
	
	@Test
	public void leftJoinIsNotEvaluatedIfNoFilteringIsAppliedOnTheJoinedPartAndQueryIsDistinct() {
		Join<Customer> joinOrders = new Join<>(queryCtx, "orders", "", LEFT, true);
		
		
		List<Customer> found = customerRepo.findAll(joinOrders);
		
		assertThat(found)
			.hasSize(3)
			.extracting(Customer::getFirstName)
			.containsOnly("Bart", "Homer", "Marge");
		
		assertThatInterceptedStatements()
			.hasSelects(1)
			.hasNumberOfJoins(0);
	}
	
	@Test
	public void leftJoinIsNotEvaluatedIfNoFilteringIsAppliedOnTheJoinedPartAndQueryIsDistinct_multiLevelJoin() {
		Join<Customer> leftJoinOrders = new Join<>(queryCtx, "orders", "o", LEFT, true);
		Join<Customer> innerJoinTags = new Join<>(queryCtx, "o.tags", "", LEFT, true);
		
		List<Customer> found = customerRepo.findAll(Specification.where(leftJoinOrders).and(innerJoinTags));
		
		assertThat(found)
			.hasSize(3)
			.extracting(Customer::getFirstName)
			.containsOnly("Bart", "Homer", "Marge");
		
		assertThatInterceptedStatements()
			.hasSelects(1)
			.hasNumberOfJoins(0);
	}

	@Test
	public void equalsAndHashCodeContract() {
		EqualsVerifier.forClass(Join.class)
				.usingGetClass()
				.suppress(Warning.NONFINAL_FIELDS)
				.verify();
	}

	@Test
	public void toStringVerifier() {
		ToStringVerifier.forClass(Join.class)
				.verify();
	}

}
