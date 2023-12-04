package com.tipsontech.demo.service;

import java.util.List;

import com.tipsontech.demo.entity.Invoice;

public interface IInvoice {

	public Invoice saveInvoice(Invoice invoice);

	public Invoice updateInvoice(Invoice invoice, Integer invoiceid);

	public void deleteInvoice(Integer invoiceid);

	public Invoice getInvoiceById(Integer invoiceid);

	public List<Invoice> getAllInvoices();
}
