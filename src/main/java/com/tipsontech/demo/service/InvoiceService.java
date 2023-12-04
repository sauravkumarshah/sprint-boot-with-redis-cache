package com.tipsontech.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.tipsontech.demo.entity.Invoice;
import com.tipsontech.demo.exception.InvoiceNotFoundException;
import com.tipsontech.demo.repository.InvoiceRepository;

@Service
public class InvoiceService implements IInvoice {

	@Autowired
	private InvoiceRepository invoiceRepo;

	@Override
	public Invoice saveInvoice(Invoice invoice) {

		return invoiceRepo.save(invoice);
	}

	@Override
	@CachePut(value = "Invoice", key = "#invoiceid")
	public Invoice updateInvoice(Invoice inv, Integer invoiceid) {
		Invoice invoice = invoiceRepo.findById(invoiceid)
				.orElseThrow(() -> new InvoiceNotFoundException("Invoice Not Found"));
		invoice.setAmount(inv.getAmount());
		invoice.setName(inv.getName());
		return invoiceRepo.save(invoice);
	}

	@Override
	@CacheEvict(value = "Invoice", key = "#invoiceid")
	// @CacheEvict(value="Invoice", allEntries=true) //in case there are multiple
	// records to delete
	public void deleteInvoice(Integer invoiceid) {
		Invoice invoice = invoiceRepo.findById(invoiceid)
				.orElseThrow(() -> new InvoiceNotFoundException("Invoice Not Found"));
		invoiceRepo.delete(invoice);
	}

	@Override
	@Cacheable(value = "Invoice", key = "#invoiceid")
	public Invoice getInvoiceById(Integer invoiceid) {
		return invoiceRepo.findById(invoiceid).orElseThrow(() -> new InvoiceNotFoundException("Invoice Not Found"));
	}

	@Override
	@Cacheable(value = "Invoice")
	public List<Invoice> getAllInvoices() {
		return invoiceRepo.findAll();
	}
}
