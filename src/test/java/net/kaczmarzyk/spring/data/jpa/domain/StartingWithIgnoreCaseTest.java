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

import net.kaczmarzyk.spring.data.jpa.Customer;
import net.kaczmarzyk.spring.data.jpa.IntegrationTestBase;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Locale;

import static net.kaczmarzyk.spring.data.jpa.CustomerBuilder.customer;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 
 * @author Matt S.Y. Ho
 *
 */
public class StartingWithIgnoreCaseTest extends IntegrationTestBase {

	Customer homerSimpson;
	Customer margeSimpson;
	Customer moeSzyslak;

	@Before
	public void initData() {
		homerSimpson = customer("Homer", "Simpson").street("Evergreen Terrace").build(em);
		margeSimpson = customer("Marge", "Simpson").street("Evergreen Terrace").build(em);
		moeSzyslak = customer("Moe", "Szyslak").street("Unknown").build(em);
	}

	@Test
	public void filtersByFirstLevelProperty() {
		StartingWithIgnoreCase<Customer> lastNameSimpson = new StartingWithIgnoreCase<>(queryCtx, "lastName",
				"SIMPSON");
		lastNameSimpson.setLocale(Locale.getDefault());
		List<Customer> result = customerRepo.findAll(lastNameSimpson);
		assertThat(result).hasSize(2).containsOnly(homerSimpson, margeSimpson);

		StartingWithIgnoreCase<Customer> firstNameWithO = new StartingWithIgnoreCase<>(queryCtx, "firstName", "HO");
		firstNameWithO.setLocale(Locale.getDefault());
		result = customerRepo.findAll(firstNameWithO);
		assertThat(result).hasSize(1).containsOnly(homerSimpson);
	}

	@Test
	public void filtersByNestedProperty() {
		StartingWithIgnoreCase<Customer> streetWithEvergreen = new StartingWithIgnoreCase<>(queryCtx, "address.street",
				"EVERGREEN");
		streetWithEvergreen.setLocale(Locale.getDefault());
		List<Customer> result = customerRepo.findAll(streetWithEvergreen);
		assertThat(result).hasSize(2).containsOnly(homerSimpson, margeSimpson);

		StartingWithIgnoreCase<Customer> streetWithTerrace = new StartingWithIgnoreCase<>(queryCtx, "address.street",
				"TERRACE");
		streetWithTerrace.setLocale(Locale.getDefault());
		result = customerRepo.findAll(streetWithTerrace);
		assertThat(result).hasSize(0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsMissingArgument() {
		new StartingWithIgnoreCase<>(queryCtx, "path", new String[] {});
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsInvalidNumberOfArguments() {
		new StartingWithIgnoreCase<>(queryCtx, "path", new String[] { "a", "b" });
	}

	@Test
	public void equalsAndHashCodeContract() {
		EqualsVerifier.forClass(StartingWithIgnoreCase.class)
				.usingGetClass()
				.suppress(Warning.NONFINAL_FIELDS)
				.verify();
	}
}
