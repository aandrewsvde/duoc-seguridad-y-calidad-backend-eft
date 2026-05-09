package com.duoc.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientController patientController;

    @Test
    void createPatientShouldReturnCreatedWhenSaveSucceeds() {
        Patient patient = new Patient("Luna", "Dog", "Labrador", 4, "Ana");

        ResponseEntity<?> response = patientController.createPatient(patient);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(patient, response.getBody());
        verify(patientRepository).save(patient);
    }

    @Test
    void createPatientShouldReturnBadRequestWithErrorMessageWhenSaveFails() {
        Patient patient = new Patient("Luna", "Dog", "Labrador", 4, "Ana");
        doThrow(new RuntimeException("DB down")).when(patientRepository).save(patient);

        ResponseEntity<?> response = patientController.createPatient(patient);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertInstanceOf(Map.class, response.getBody());

        @SuppressWarnings("unchecked")
        Map<String, String> error = (Map<String, String>) response.getBody();
        assertEquals("Error al registrar: DB down", error.get("message"));
    }

    @Test
    void getAllPatientsShouldReturnOkWithPatientsWhenRepositorySucceeds() {
        List<Patient> patients = List.of(
                new Patient("Luna", "Dog", "Labrador", 4, "Ana"),
                new Patient("Milo", "Cat", "Siamese", 2, "Pedro")
        );
        when(patientRepository.findAll()).thenReturn(patients);

        ResponseEntity<Iterable<Patient>> response = patientController.getAllPatients();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(patients, response.getBody());
    }

    @Test
    void getAllPatientsShouldReturnInternalServerErrorWhenRepositoryFails() {
        when(patientRepository.findAll()).thenThrow(new RuntimeException("DB down"));

        ResponseEntity<Iterable<Patient>> response = patientController.getAllPatients();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getPatientByIdShouldReturnOkWhenPatientExists() {
        int id = 10;
        Patient patient = new Patient("Luna", "Dog", "Labrador", 4, "Ana");
        patient.setId(id);
        when(patientRepository.findById(id)).thenReturn(Optional.of(patient));

        ResponseEntity<Patient> response = patientController.getPatientById(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(patient, response.getBody());
    }

    @Test
    void getPatientByIdShouldReturnNotFoundWhenPatientDoesNotExist() {
        int id = 99;
        when(patientRepository.findById(id)).thenReturn(Optional.empty());

        ResponseEntity<Patient> response = patientController.getPatientById(id);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getPatientByIdShouldReturnInternalServerErrorWhenRepositoryFails() {
        int id = 7;
        when(patientRepository.findById(id)).thenThrow(new RuntimeException("DB down"));

        ResponseEntity<Patient> response = patientController.getPatientById(id);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }
}
