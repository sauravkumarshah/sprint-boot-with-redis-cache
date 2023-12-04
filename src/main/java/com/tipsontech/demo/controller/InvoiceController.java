package com.tipsontech.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tipsontech.demo.entity.Invoice;
import com.tipsontech.demo.service.IInvoice;

@RestController
@RequestMapping("/api/invoice")
public class InvoiceController {

	@Autowired
	IInvoice invoiceService;

	@PostMapping("/")
	public ResponseEntity<Invoice> saveInvoice(@RequestBody Invoice invoice) {
		return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.saveInvoice(invoice));
	}

	@GetMapping("/")
	public ResponseEntity<List<Invoice>> getAllInvoices() {
		return ResponseEntity.status(HttpStatus.OK).body(invoiceService.getAllInvoices());
	}

	@GetMapping("/{id}")
	public ResponseEntity<Invoice> getOneInvoice(@PathVariable Integer id) {
		return ResponseEntity.status(HttpStatus.OK).body(invoiceService.getInvoiceById(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Invoice> updateInvoice(@RequestBody Invoice invoice, @PathVariable Integer id) {
		return ResponseEntity.status(HttpStatus.OK).body(invoiceService.updateInvoice(invoice, id));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteInvoice(@PathVariable Integer id) {
		invoiceService.deleteInvoice(id);
		return ResponseEntity.status(HttpStatus.OK).body("Invoice with id: " + id + " Deleted !");
	}
}
