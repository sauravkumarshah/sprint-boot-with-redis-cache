package com.tipsontech.demo;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import com.tipsontech.demo.entity.Invoice;
import com.tipsontech.demo.repository.InvoiceRepository;
import com.tipsontech.demo.service.IInvoice;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
class SpringBootRedisCacheApplicationTests {

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private InvoiceRepository repository;

	@Autowired
	private IInvoice service;

	@Autowired
	private ObjectMapper objectMapper;

	private static List<Invoice> invoices = new ArrayList<>();

	@Container
	private static final MSSQLServerContainer<?> SQLSERVER_CONTAINER = new MSSQLServerContainer<>(
			"mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

	@Container
	private static final RedisContainer REDIS_CONTAINER = new RedisContainer(
			RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG)).withExposedPorts(6379);

	@DynamicPropertySource
	static void setProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
		dynamicPropertyRegistry.add("spring.datasource.url", SQLSERVER_CONTAINER::getJdbcUrl);
		dynamicPropertyRegistry.add("spring.datasource.username", SQLSERVER_CONTAINER::getUsername);
		dynamicPropertyRegistry.add("spring.datasource.password", SQLSERVER_CONTAINER::getPassword);
		dynamicPropertyRegistry.add("spring.jpa.generate-ddl", () -> "true");
		dynamicPropertyRegistry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

		System.setProperty("spring.data.redis.host", REDIS_CONTAINER.getHost());
		System.setProperty("spring.data.redis.port", REDIS_CONTAINER.getMappedPort(6379).toString());
	}

	static {
		SQLSERVER_CONTAINER.start();
		REDIS_CONTAINER.start();
	}

	static {

		Invoice invoice1 = Invoice.builder().name("invoice 1").amount(123.45).build();
		Invoice invoice2 = Invoice.builder().name("invoice 2").amount(124.45).build();
		Invoice invoice3 = Invoice.builder().name("invoice 3").amount(126.45).build();
		Invoice invoice4 = Invoice.builder().name("invoice 4").amount(121.45).build();
		Invoice invoice5 = Invoice.builder().name("invoice 5").amount(128.45).build();

		invoices.add(invoice1);
		invoices.add(invoice2);
		invoices.add(invoice3);
		invoices.add(invoice4);
		invoices.add(invoice5);
	}

	@Test
	@Order(value = 1)
	void testConnectionToDatabase() {
		Assertions.assertNotNull(repository);
		Assertions.assertTrue(SQLSERVER_CONTAINER.isRunning());
		Assertions.assertTrue(REDIS_CONTAINER.isRunning());
	}

	@Test
	@Order(value = 2)
	void testSaveInvoices() throws Exception {
		for (Invoice invoice : invoices) {
			String inv = objectMapper.writeValueAsString(invoice);
			mockMvc.perform(
					MockMvcRequestBuilders.post("/api/invoice/").contentType(MediaType.APPLICATION_JSON).content(inv))
					.andExpect(status().isCreated());
		}
		Assertions.assertEquals(5, service.getAllInvoices().size());
	}

	@Test
	@Order(value = 3)
	void testGetAllInvoices() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/invoice/")).andExpect(status().isOk());
		Assertions.assertEquals(invoices.get(3).getName(), service.getAllInvoices().get(3).getName());
	}

	@Test
	@Order(value = 4)
	void testGetOneInvoice() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/invoice/2")).andExpect(status().isOk());
		Assertions.assertEquals(invoices.get(2).getName(), service.getInvoiceById(3).getName());
	}

	@Test
	@Order(value = 5)
	void testDeleteInvoiceById() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.delete("/api/invoice/2")).andExpect(status().isOk());
	}

	@Test
	@Order(value = 6)
	void testUpdateInvoice() throws Exception {
		Invoice invoice = Invoice.builder().invoiceid(3).name("invoice 33").amount(444.34).build();
		String inv = objectMapper.writeValueAsString(invoice);
		mockMvc.perform(
				MockMvcRequestBuilders.put("/api/invoice/3").contentType(MediaType.APPLICATION_JSON).content(inv))
				.andExpect(status().isOk());
		Assertions.assertEquals(invoice.getName(), service.getInvoiceById(3).getName());
		Assertions.assertEquals(invoice.getAmount(), service.getInvoiceById(3).getAmount());
	}

	@Test
	@Order(value = 7)
	void testCacheUsage() {
		// Assuming you have some invoice in the database
		int invoiceId = 1;

		// Perform an operation that should be cached (e.g., fetching invoice data)
		Invoice firstResult = service.getInvoiceById(invoiceId);

		// Check if the data is retrieved from the cache
		assertNull(redisTemplate.opsForValue().get("Invoice ::" + invoiceId));

		// Check if the cache manager is an instance of RedisCacheManager
		assertTrue(cacheManager instanceof RedisCacheManager, "Cache manager is not an instance of RedisCacheManager");

		// Add further assertions specific to RedisCacheManager
		RedisCacheManager redisCacheManager = (RedisCacheManager) cacheManager;

		// Check if the default cache configuration is applied
		assertNotNull(redisCacheManager.getCacheConfigurations().get("Invoice"),
				"Cache configuration for 'Invoice' is missing");
		
		// Perform the same operation again
		Invoice secondResult = service.getInvoiceById(invoiceId);

		// Check if the data is retrieved from the cache (no DB query)
		assertNull(redisTemplate.opsForValue().get("Invoice ::" + invoiceId));

	}
}
