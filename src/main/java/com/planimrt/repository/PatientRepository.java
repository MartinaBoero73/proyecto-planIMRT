package com.planimrt.repository;

import com.planimrt.model.Patient;
import com.planimrt.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientId(String patientId);

    List<Patient> findByResponsibleUser(User user);

    List<Patient> findByNameContainingIgnoreCase(String name);

    boolean existsByPatientId(String patientId);
}