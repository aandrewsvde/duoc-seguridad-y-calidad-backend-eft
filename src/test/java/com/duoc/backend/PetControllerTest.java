package com.duoc.backend;

import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetControllerTest {

    @Mock
    private PetRepository petRepository;

    @InjectMocks
    private PetController petController;

    private Pet pet;

    @BeforeEach
    void setUp() {
        pet = new Pet("Fido", "Dog", "Labrador", 3, "Male", "Santiago", List.of());
        pet.setId(1);
    }

    // ── createPet ─────────────────────────────────────────────────────────────

    @Test
    void createPetShouldReturnCreatedWhenSaveSucceeds() {
        ResponseEntity<?> response = petController.createPet(pet);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(pet, response.getBody());
        verify(petRepository).save(pet);
    }

    @Test
    void createPetShouldReturnBadRequestWhenSaveFails() {
        doThrow(new RuntimeException("DB down")).when(petRepository).save(pet);

        ResponseEntity<?> response = petController.createPet(pet);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
        @SuppressWarnings("unchecked")
        Map<String, String> error = (Map<String, String>) response.getBody();
        assertEquals("Error al registrar mascota: DB down", error.get("message"));
    }

    // ── getAllPets ─────────────────────────────────────────────────────────────

    @Test
    void getAllPetsShouldReturnOkWithPets() {
        List<Pet> pets = List.of(pet);
        when(petRepository.findAll()).thenReturn(pets);

        ResponseEntity<Iterable<Pet>> response = petController.getAllPets();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pets, response.getBody());
    }

    @Test
    void getAllPetsShouldReturnInternalServerErrorWhenRepositoryFails() {
        when(petRepository.findAll()).thenThrow(new RuntimeException("DB down"));

        ResponseEntity<Iterable<Pet>> response = petController.getAllPets();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    // ── getAvailablePets ───────────────────────────────────────────────────────

    @Test
    void getAvailablePetsShouldReturnOkWithPets() {
        List<Pet> pets = List.of(pet);
        when(petRepository.findByStatus("available")).thenReturn(pets);

        ResponseEntity<List<Pet>> response = petController.getAvailablePets();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pets, response.getBody());
    }

    @Test
    void getAvailablePetsShouldReturnInternalServerErrorWhenRepositoryFails() {
        when(petRepository.findByStatus("available")).thenThrow(new RuntimeException("DB down"));

        ResponseEntity<List<Pet>> response = petController.getAvailablePets();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    // ── getPetById ────────────────────────────────────────────────────────────

    @Test
    void getPetByIdShouldReturnOkWhenPetExists() {
        when(petRepository.findById(1)).thenReturn(Optional.of(pet));

        ResponseEntity<Pet> response = petController.getPetById(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pet, response.getBody());
    }

    @Test
    void getPetByIdShouldReturnNotFoundWhenPetDoesNotExist() {
        when(petRepository.findById(99)).thenReturn(Optional.empty());

        ResponseEntity<Pet> response = petController.getPetById(99);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getPetByIdShouldReturnInternalServerErrorWhenRepositoryFails() {
        when(petRepository.findById(1)).thenThrow(new RuntimeException("DB down"));

        ResponseEntity<Pet> response = petController.getPetById(1);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    // ── updatePet ─────────────────────────────────────────────────────────────

    @Test
    void updatePetShouldReturnOkWithUpdatedPetWhenExists() {
        Pet details = new Pet();
        details.setName("Rex");
        details.setSpecies("Dog");
        when(petRepository.findById(1)).thenReturn(Optional.of(pet));

        ResponseEntity<?> response = petController.updatePet(1, details);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Pet updated = (Pet) response.getBody();
        assertNotNull(updated);
        assertEquals("Rex", updated.getName());
        assertEquals("Dog", updated.getSpecies());
        verify(petRepository).save(pet);
    }

    @Test
    void updatePetShouldReturnNotFoundWhenPetDoesNotExist() {
        when(petRepository.findById(99)).thenReturn(Optional.empty());

        ResponseEntity<?> response = petController.updatePet(99, new Pet());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(petRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updatePetShouldReturnBadRequestWhenRepositoryFails() {
        Pet details = new Pet();
        details.setName("Rex");
        when(petRepository.findById(1)).thenReturn(Optional.of(pet));
        doThrow(new RuntimeException("DB down")).when(petRepository).save(pet);

        ResponseEntity<?> response = petController.updatePet(1, details);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
        @SuppressWarnings("unchecked")
        Map<String, String> error = (Map<String, String>) response.getBody();
        assertEquals("Error al actualizar mascota: DB down", error.get("message"));
    }

    // ── deletePet ─────────────────────────────────────────────────────────────

    @Test
    void deletePetShouldReturnOkWithMessageWhenPetExists() {
        when(petRepository.findById(1)).thenReturn(Optional.of(pet));

        ResponseEntity<?> response = petController.deletePet(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Mascota eliminada exitosamente", body.get("message"));
        verify(petRepository).deleteById(1);
    }

    @Test
    void deletePetShouldReturnNotFoundWhenPetDoesNotExist() {
        when(petRepository.findById(99)).thenReturn(Optional.empty());

        ResponseEntity<?> response = petController.deletePet(99);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(petRepository, never()).deleteById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deletePetShouldReturnBadRequestWhenRepositoryFails() {
        when(petRepository.findById(1)).thenReturn(Optional.of(pet));
        doThrow(new RuntimeException("DB down")).when(petRepository).deleteById(1);

        ResponseEntity<?> response = petController.deletePet(1);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
        @SuppressWarnings("unchecked")
        Map<String, String> error = (Map<String, String>) response.getBody();
        assertEquals("Error al eliminar mascota: DB down", error.get("message"));
    }

    // ── searchPets ────────────────────────────────────────────────────────────

    @Test
    void searchPetsShouldReturnOkWithAllCriteria() {
        List<Pet> pets = List.of(pet);
        when(petRepository.findBySpeciesAndGenderAndLocationAndAgeAndStatus("Dog", "Male", "Santiago", 3, "available"))
                .thenReturn(pets);

        ResponseEntity<List<Pet>> response = petController.searchPets("Dog", "Male", "Santiago", 3, "available");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pets, response.getBody());
    }

    @Test
    void searchPetsShouldReturnOkWithOnlySpecies() {
        List<Pet> pets = List.of(pet);
        when(petRepository.findBySpeciesAndStatus("Dog", "available")).thenReturn(pets);

        ResponseEntity<List<Pet>> response = petController.searchPets("Dog", null, null, null, "available");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pets, response.getBody());
    }

    @Test
    void searchPetsShouldReturnOkWithOnlyGender() {
        List<Pet> pets = List.of(pet);
        when(petRepository.findByGenderAndStatus("Male", "available")).thenReturn(pets);

        ResponseEntity<List<Pet>> response = petController.searchPets(null, "Male", null, null, "available");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pets, response.getBody());
    }

    @Test
    void searchPetsShouldReturnOkWithOnlyLocation() {
        List<Pet> pets = List.of(pet);
        when(petRepository.findByLocationAndStatus("Santiago", "available")).thenReturn(pets);

        ResponseEntity<List<Pet>> response = petController.searchPets(null, null, "Santiago", null, "available");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pets, response.getBody());
    }

    @Test
    void searchPetsShouldReturnOkWithOnlyAge() {
        List<Pet> pets = List.of(pet);
        when(petRepository.findByAgeAndStatus(3, "available")).thenReturn(pets);

        ResponseEntity<List<Pet>> response = petController.searchPets(null, null, null, 3, "available");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pets, response.getBody());
    }

    @Test
    void searchPetsShouldReturnOkWithNoFilters() {
        List<Pet> pets = List.of(pet);
        when(petRepository.findByStatus("available")).thenReturn(pets);

        ResponseEntity<List<Pet>> response = petController.searchPets(null, null, null, null, "available");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pets, response.getBody());
    }

    @Test
    void searchPetsShouldReturnInternalServerErrorWhenRepositoryFails() {
        when(petRepository.findByStatus("available")).thenThrow(new RuntimeException("DB down"));

        ResponseEntity<List<Pet>> response = petController.searchPets(null, null, null, null, "available");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }
}
