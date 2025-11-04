package com.planimrt.repository;

import com.planimrt.model.DicomFile;
import com.planimrt.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DicomFileRepository extends JpaRepository<DicomFile, Long> {

    List<DicomFile> findByPatient(Patient patient);

    Optional<DicomFile> findBySopInstanceUID(String sopInstanceUID);

    List<DicomFile> findByModality(String modality);

    List<DicomFile> findByStudyInstanceUID(String studyInstanceUID);

    @Query("SELECT d FROM DicomFile d WHERE d.uploadedAt >= :startDate AND d.uploadedAt <= :endDate")
    List<DicomFile> findByUploadedAtBetween(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT d FROM DicomFile d WHERE d.patient.id = :patientId ORDER BY d.uploadedAt DESC")
    List<DicomFile> findByPatientIdOrderByUploadedAtDesc(@Param("patientId") Long patientId);
}