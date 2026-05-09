package com.duoc.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private InvoiceController invoiceController;

    private Invoice validInvoice;
    private InvoiceLineItem validItem;

    @BeforeEach
    void setUp() {
        validItem = new InvoiceLineItem(
                InvoiceLineItemType.SERVICE, "Consulta veterinaria", 1, new BigDecimal("25000"));

        validInvoice = new Invoice();
        validInvoice.getItems().add(validItem);
    }

    // ── createInvoiceForAppointment ───────────────────────────────────────────

    @Test
    void createInvoiceShouldReturnCreatedWhenValid() {
        when(appointmentRepository.existsById(10)).thenReturn(true);
        when(invoiceRepository.findByAppointmentId(10)).thenReturn(List.of());
        Invoice saved = new Invoice();
        saved.setId(1);
        when(invoiceRepository.save(any())).thenReturn(saved);

        ResponseEntity<?> response = invoiceController.createInvoiceForAppointment(10, validInvoice);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(saved, response.getBody());
        verify(invoiceRepository).save(validInvoice);
    }

    @Test
    void createInvoiceShouldSetIssueDateWhenNull() {
        validInvoice.setIssueDate(null);
        when(appointmentRepository.existsById(10)).thenReturn(true);
        when(invoiceRepository.findByAppointmentId(10)).thenReturn(List.of());
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        invoiceController.createInvoiceForAppointment(10, validInvoice);

        assertNotNull(validInvoice.getIssueDate());
    }

    @Test
    void createInvoiceShouldSetDefaultVatRateWhenNull() {
        validInvoice.setVatRate(null);
        when(appointmentRepository.existsById(10)).thenReturn(true);
        when(invoiceRepository.findByAppointmentId(10)).thenReturn(List.of());
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        invoiceController.createInvoiceForAppointment(10, validInvoice);

        assertEquals(new BigDecimal("0.19"), validInvoice.getVatRate());
    }

    @Test
    void createInvoiceShouldReturnBadRequestWhenAppointmentNotFound() {
        when(appointmentRepository.existsById(99)).thenReturn(false);

        ResponseEntity<?> response = invoiceController.createInvoiceForAppointment(99, validInvoice);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Appointment not found for appointmentId: 99", response.getBody());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoiceShouldReturnConflictWhenInvoiceAlreadyExists() {
        when(appointmentRepository.existsById(10)).thenReturn(true);
        when(invoiceRepository.findByAppointmentId(10)).thenReturn(List.of(new Invoice()));

        ResponseEntity<?> response = invoiceController.createInvoiceForAppointment(10, validInvoice);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("An invoice already exists for appointmentId: 10", response.getBody());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoiceShouldReturnBadRequestWhenItemsIsEmpty() {
        Invoice emptyItems = new Invoice();
        when(appointmentRepository.existsById(10)).thenReturn(true);
        when(invoiceRepository.findByAppointmentId(10)).thenReturn(List.of());

        ResponseEntity<?> response = invoiceController.createInvoiceForAppointment(10, emptyItems);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invoice must include at least one detail item", response.getBody());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoiceShouldReturnBadRequestWhenItemTypeMissing() {
        InvoiceLineItem item = new InvoiceLineItem(null, "Consulta", 1, new BigDecimal("10000"));
        Invoice invoice = new Invoice();
        invoice.getItems().add(item);
        when(appointmentRepository.existsById(10)).thenReturn(true);
        when(invoiceRepository.findByAppointmentId(10)).thenReturn(List.of());

        ResponseEntity<?> response = invoiceController.createInvoiceForAppointment(10, invoice);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Item 1 must define type (SERVICE, MEDICATION, ADDITIONAL_CHARGE)", response.getBody());
    }

    @Test
    void createInvoiceShouldReturnBadRequestWhenItemDescriptionBlank() {
        InvoiceLineItem item = new InvoiceLineItem(InvoiceLineItemType.SERVICE, "  ", 1, new BigDecimal("10000"));
        Invoice invoice = new Invoice();
        invoice.getItems().add(item);
        when(appointmentRepository.existsById(10)).thenReturn(true);
        when(invoiceRepository.findByAppointmentId(10)).thenReturn(List.of());

        ResponseEntity<?> response = invoiceController.createInvoiceForAppointment(10, invoice);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Item 1 must include a description", response.getBody());
    }

    @Test
    void createInvoiceShouldReturnBadRequestWhenItemQuantityZero() {
        InvoiceLineItem item = new InvoiceLineItem(InvoiceLineItemType.SERVICE, "Consulta", 0, new BigDecimal("10000"));
        Invoice invoice = new Invoice();
        invoice.getItems().add(item);
        when(appointmentRepository.existsById(10)).thenReturn(true);
        when(invoiceRepository.findByAppointmentId(10)).thenReturn(List.of());

        ResponseEntity<?> response = invoiceController.createInvoiceForAppointment(10, invoice);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Item 1 must include a quantity greater than zero", response.getBody());
    }

    @Test
    void createInvoiceShouldReturnBadRequestWhenItemUnitPriceNegative() {
        InvoiceLineItem item = new InvoiceLineItem(InvoiceLineItemType.SERVICE, "Consulta", 1, new BigDecimal("-1"));
        Invoice invoice = new Invoice();
        invoice.getItems().add(item);
        when(appointmentRepository.existsById(10)).thenReturn(true);
        when(invoiceRepository.findByAppointmentId(10)).thenReturn(List.of());

        ResponseEntity<?> response = invoiceController.createInvoiceForAppointment(10, invoice);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Item 1 must include a unitPrice greater or equal to zero", response.getBody());
    }

    @Test
    void createInvoiceShouldReturnBadRequestWhenVatRateNegative() {
        validInvoice.setVatRate(new BigDecimal("-0.05"));
        when(appointmentRepository.existsById(10)).thenReturn(true);
        when(invoiceRepository.findByAppointmentId(10)).thenReturn(List.of());

        ResponseEntity<?> response = invoiceController.createInvoiceForAppointment(10, validInvoice);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("vatRate cannot be negative", response.getBody());
    }

    @Test
    void createInvoiceShouldReturnBadRequestWhenRepositoryThrows() {
        when(appointmentRepository.existsById(10)).thenReturn(true);
        when(invoiceRepository.findByAppointmentId(10)).thenReturn(List.of());
        when(invoiceRepository.save(any())).thenThrow(new RuntimeException("DB down"));

        ResponseEntity<?> response = invoiceController.createInvoiceForAppointment(10, validInvoice);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
        @SuppressWarnings("unchecked")
        Map<String, String> error = (Map<String, String>) response.getBody();
        assertEquals("Error al generar factura: DB down", error.get("message"));
    }

    // ── getAllInvoices ────────────────────────────────────────────────────────

    @Test
    void getAllInvoicesShouldReturnOkWithInvoices() {
        List<Invoice> invoices = List.of(validInvoice);
        when(invoiceRepository.findAll()).thenReturn(invoices);

        ResponseEntity<Iterable<Invoice>> response = invoiceController.getAllInvoices();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(invoices, response.getBody());
    }

    @Test
    void getAllInvoicesShouldReturnInternalServerErrorWhenRepositoryFails() {
        when(invoiceRepository.findAll()).thenThrow(new RuntimeException("DB down"));

        ResponseEntity<Iterable<Invoice>> response = invoiceController.getAllInvoices();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    // ── getInvoiceById ────────────────────────────────────────────────────────

    @Test
    void getInvoiceByIdShouldReturnOkWhenFound() {
        when(invoiceRepository.findById(1)).thenReturn(Optional.of(validInvoice));

        ResponseEntity<Invoice> response = invoiceController.getInvoiceById(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(validInvoice, response.getBody());
    }

    @Test
    void getInvoiceByIdShouldReturnNotFoundWhenMissing() {
        when(invoiceRepository.findById(99)).thenReturn(Optional.empty());

        ResponseEntity<Invoice> response = invoiceController.getInvoiceById(99);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getInvoiceByIdShouldReturnInternalServerErrorWhenRepositoryFails() {
        when(invoiceRepository.findById(1)).thenThrow(new RuntimeException("DB down"));

        ResponseEntity<Invoice> response = invoiceController.getInvoiceById(1);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    // ── getInvoiceByAppointment ───────────────────────────────────────────────

    @Test
    void getInvoiceByAppointmentShouldReturnOkWhenFound() {
        when(appointmentRepository.existsById(10)).thenReturn(true);
        when(invoiceRepository.findByAppointmentId(10)).thenReturn(List.of(validInvoice));

        ResponseEntity<?> response = invoiceController.getInvoiceByAppointment(10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(validInvoice, response.getBody());
    }

    @Test
    void getInvoiceByAppointmentShouldReturnNotFoundWhenAppointmentMissing() {
        when(appointmentRepository.existsById(99)).thenReturn(false);

        ResponseEntity<?> response = invoiceController.getInvoiceByAppointment(99);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getInvoiceByAppointmentShouldReturnNotFoundWhenNoInvoicesExist() {
        when(appointmentRepository.existsById(10)).thenReturn(true);
        when(invoiceRepository.findByAppointmentId(10)).thenReturn(List.of());

        ResponseEntity<?> response = invoiceController.getInvoiceByAppointment(10);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getInvoiceByAppointmentShouldReturnInternalServerErrorWhenRepositoryFails() {
        when(appointmentRepository.existsById(10)).thenReturn(true);
        when(invoiceRepository.findByAppointmentId(10)).thenThrow(new RuntimeException("DB down"));

        ResponseEntity<?> response = invoiceController.getInvoiceByAppointment(10);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }
}
